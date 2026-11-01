using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Automation;

namespace JarvisWindows;

/// <summary>Normale Bestätigungsdialoge lesen und bedienen, über UI Automation und mit festen Grenzen: Anmeldung, Passwörter, UAC, Windows Hello und
/// Datenschutz-Freigaben von Windows gelten als persönliche Freigabe. Solche Dialoge meldet das Werkzeug nur; es klickt dort nie.</summary>
public static class WinDialoge
{
    public sealed record Dialog(string App, string Bundle, string Titel, List<string> Texte, List<(string Titel, AutomationElement Element)> Knoepfe, bool Persoenlich);

    private static readonly string[] FreigabeProzesse = { "consent", "credentialuibroker", "credentialenrollmentmanager", "logonui", "lockapp", "winlogon", "userconsentverifier", "useroobebroker", "securityhealth" };
    private static readonly Regex FreigabeText = new(
        @"(?i)passwort|kennwort|password|passcode|\bpin\b|windows hello|gesichtserkennung|fingerabdruck|benutzerkontensteuerung|user account control|änderungen an ihrem gerät|make changes to your device|anmelden|anmeldung|sign.?in|log.?in|microsoft.?(konto|account)|bestätigungscode|verification code|zwei-faktor|two-factor|möchte auf .* zugreifen|möchte .* steuern|would like to (access|control)|wants? (to )?(access|control)|voice-overlay aktualisieren|spracheingabe aktualisieren",
        RegexOptions.Compiled);
    private static readonly Regex Endgueltig = new(
        @"(?i)lösch|delete|entfern|remove|erase|format|bezahl|kauf|\bpay\b|\bbuy\b|überschreib|overwrite|ersetz|replace|verwerf|discard|nicht sichern|nicht speichern|don.t save|papierkorb|trash|recycle|abonn|subscribe",
        RegexOptions.Compiled);
    private static readonly Regex Ablehnend = new(@"(?i)^(abbrechen|nein|cancel|no|schließen|close|nicht jetzt|not now)$", RegexOptions.Compiled);

    /// <summary>Geht den Baum eines Fensters begrenzt durch: Knöpfe, Texte und ob ein Passwortfeld darin ist.</summary>
    private static (List<string> Texte, List<(string, AutomationElement)> Knoepfe, bool Geheim) Durchsuche(AutomationElement wurzel)
    {
        var texte = new List<string>();
        var knoepfe = new List<(string, AutomationElement)>();
        var geheim = false;
        var offen = new Stack<AutomationElement>();
        offen.Push(wurzel);
        var besucht = 0;
        while (offen.Count > 0)
        {
            var e = offen.Pop();
            // Zu groß, um alles zu sehen: Im Zweifel zählt der Dialog als geschützt.
            if (besucht >= 300) { geheim = true; break; }
            besucht++;
            try
            {
                var c = e.Current;
                // In der Titelleiste sitzen nur Schließen, Minimieren und Maximieren.
                if (c.ControlType == ControlType.TitleBar) continue;
                if (c.IsPassword) geheim = true;
                if (c.ControlType == ControlType.Button || c.ControlType == ControlType.SplitButton)
                {
                    if (c.Name.Length > 0) knoepfe.Add((c.Name, e));
                }
                else if (c.ControlType == ControlType.Text)
                {
                    if (c.Name.Length > 0) texte.Add(c.Name);
                }
                else if ((c.ControlType == ControlType.Edit || c.ControlType == ControlType.Document) && !c.IsPassword
                         && e.TryGetCurrentPattern(ValuePattern.Pattern, out var w) && ((ValuePattern)w).Current.Value is { Length: > 0 } wert)
                {
                    texte.Add(wert);
                }
                var kinder = e.FindAll(TreeScope.Children, Automation.ControlViewCondition);
                for (var i = kinder.Count - 1; i >= 0; i--) offen.Push(kinder[i]);
            }
            catch (ElementNotAvailableException) { }
            catch (Exception) { }
        }
        return (texte, knoepfe, geheim);
    }

    /// <summary>Die Dialoge eines Fensters: Fenster im Fenster oder das Fenster selbst, wenn es ein Dialog ist.</summary>
    private static List<Dialog> DialogeVon(FensterInfo f)
    {
        var liste = new List<Dialog>();
        var w = AutomationElement.FromHandle(f.Hwnd);
        var prozess = f.Prozess.ToLowerInvariant();
        var freigabe = FreigabeProzesse.Any(prozess.Contains);
        var ziele = new List<AutomationElement>();
        foreach (AutomationElement k in w.FindAll(TreeScope.Children, new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Window))) ziele.Add(k);
        if (ziele.Count == 0)
        {
            var modal = w.TryGetCurrentPattern(WindowPattern.Pattern, out var m) && ((WindowPattern)m).Current.IsModal;
            var stil = WinApi.GetWindowLongPtr(f.Hwnd, WinApi.GWL_EXSTYLE);
            var besessen = WinApi.GetWindow(f.Hwnd, WinApi.GW_OWNER) != IntPtr.Zero && (stil & WinApi.WS_EX_TOOLWINDOW) == 0;
            // Fenster von Hilfsprogrammen (Overlays, Infobereich) sind keine Dialoge, außer es sind die Sicherheitsabfragen von Windows.
            if (f.Klasse == "#32770" || modal || besessen || freigabe) ziele.Add(w);
        }
        foreach (var z in ziele)
        {
            var (texte, knoepfe, geheim) = Durchsuche(z);
            if (knoepfe.Count == 0) continue;
            var titel = z.Current.Name is { Length: > 0 } t ? t : f.Titel;
            var persoenlich = geheim || freigabe || FreigabeText.IsMatch(string.Join(" ", texte.Prepend(titel)));
            liste.Add(new Dialog(f.Prozess.Length > 0 ? f.Prozess : "?", f.Klasse, titel, texte, knoepfe, persoenlich));
        }
        return liste;
    }

    /// <summary>Alle offenen Dialoge. nurApp beschränkt die Suche auf ein Programm: Das spart die Last, wenn eine Vormerkung jede Sekunde nachsieht.
    /// UI Automation kennt keine Zeitgrenze je Aufruf: Jedes Fenster läuft deshalb für sich; ein hängendes Programm darf die Suche nicht aufhalten.</summary>
    public static List<Dialog> Offene(string nurApp = "")
    {
        var liste = new List<Dialog>();
        var eigene = Environment.ProcessId;
        var ende = DateTime.UtcNow.AddSeconds(6);
        foreach (var f in Fenster.Sichtbar())
        {
            if (f.Pid == eigene || WinApi.IsHungAppWindow(f.Hwnd)) continue;
            if (nurApp.Length > 0 && !f.Prozess.Contains(nurApp, StringComparison.OrdinalIgnoreCase)) continue;
            var rest = ende - DateTime.UtcNow;
            if (rest <= TimeSpan.Zero) break;
            var lauf = Task.Run(() =>
            {
                try { return DialogeVon(f); } catch { return new List<Dialog>(); }
            });
            if (lauf.Wait(rest < TimeSpan.FromMilliseconds(1500) ? rest : TimeSpan.FromMilliseconds(1500))) liste.AddRange(lauf.Result);
        }
        return liste;
    }

    private static string Beschreibung(Dialog d)
        => $"Programm: {d.App}\nTitel: {(d.Titel.Length == 0 ? "(ohne)" : d.Titel)}\nText: {string.Join(" | ", d.Texte).Schnitt(1500)}\nKnöpfe: {string.Join(", ", d.Knoepfe.Select(k => k.Titel))}"
           + (d.Persoenlich ? "\nPERSÖNLICHE FREIGABE: Das muss Frank selbst erledigen." : "");

    private static bool Passt(Dialog d, string app, string titel)
    {
        var inhalt = string.Join(" ", d.Texte.Prepend(d.Titel));
        return (app.Length == 0 || d.App.Contains(app, StringComparison.OrdinalIgnoreCase)) && (titel.Length == 0 || inhalt.Contains(titel, StringComparison.OrdinalIgnoreCase));
    }

    private static bool HatKnopf(Dialog d, string knopf) => d.Knoepfe.Any(k => k.Titel.Equals(knopf, StringComparison.OrdinalIgnoreCase));

    /// <summary>null = gedrückt, sonst der Grund, warum nicht.</summary>
    private static string? Druecke(string app, string titel, string knopf, bool bestaetigt)
    {
        var treffer = Offene(app).Where(d => Passt(d, app, titel) && HatKnopf(d, knopf)).ToList();
        if (treffer.Count == 0) return $"Kein passender Dialog mit dem Knopf „{knopf}“ offen.";
        if (treffer.Count != 1) return $"{treffer.Count} Dialoge passen. Gib zusätzlich app oder titel an, sonst klicke ich nicht.";
        var d = treffer[0];
        if (d.Persoenlich) return $"Das ist eine persönliche Freigabe von Windows ({d.App}: {d.Titel}). Die erledigt nur Frank selbst, Jarvis klickt und tippt dort nicht. Sag ihm, was er freigeben muss, und warte mit aktion warten.";
        var inhalt = string.Join(" ", d.Texte.Prepend(d.Titel).Prepend(knopf));
        if (!bestaetigt && !Ablehnend.IsMatch(knopf) && Endgueltig.IsMatch(inhalt))
            return $"„{knopf}“ löst hier etwas Endgültiges aus. Frag Frank und setze danach frank_bestaetigt.";
        var e = d.Knoepfe.First(k => k.Titel.Equals(knopf, StringComparison.OrdinalIgnoreCase)).Element;
        try
        {
            if (!e.Current.IsEnabled) return "Der Knopf ist gerade nicht aktiv.";
            if (!e.TryGetCurrentPattern(InvokePattern.Pattern, out var muster)) return "Der Klick ging nicht durch (der Knopf nimmt keinen Aufruf an).";
            // Manche Programme antworten erst, wenn ihr Dialog zu ist.
            var lauf = Task.Run(() => ((InvokePattern)muster).Invoke());
            if (!lauf.Wait(3000)) return "Der Klick ist abgesetzt, aber ohne Rückmeldung in 3 Sekunden. Prüfe mit aktion lesen, ob der Dialog zu ist.";
            if (lauf.IsFaulted) return $"Der Klick ging nicht durch ({lauf.Exception?.InnerException?.Message}).";
            return null;
        }
        catch (Exception ex) { return $"Der Klick ging nicht durch ({ex.Message})."; }
    }

    // Vorgemerkte Knöpfe: Das Update-Skript wartet im selben Moment auf den Klick, in dem es läuft, darum muss die Bestätigung schon vorher feststehen.
    private static readonly object Wache = new();
    private static readonly List<string> Meldungen = new();
    private static CancellationTokenSource? vormerkung;

    private static void Merke(string t)
    {
        lock (Wache)
        {
            Meldungen.Add(t);
            if (Meldungen.Count > 20) Meldungen.RemoveAt(0);
        }
    }

    /// <summary>Beim Stopp, nach dem Skript und beim Beenden eines Gesprächs: Nichts darf später von selbst klicken.</summary>
    public static void VormerkungLoeschen()
    {
        lock (Wache)
        {
            vormerkung?.Cancel();
            vormerkung = null;
        }
    }

    /// <summary>Immer nur eine; sie endet beim Klick, beim Löschen, ohne Vollzugriff oder nach Ablauf. Klappt ein Klick nicht, versucht sie es bis dahin weiter.</summary>
    private static void Vormerken(string app, string titel, string knopf, int sekunden, bool bestaetigt)
    {
        CancellationToken ct;
        lock (Wache)
        {
            vormerkung?.Cancel();
            vormerkung = new CancellationTokenSource();
            ct = vormerkung.Token;
        }
        _ = Task.Run(async () =>
        {
            var ende = DateTime.UtcNow.AddSeconds(sekunden);
            var grund = $"In {sekunden} Sekunden kam kein passender Dialog.";
            while (DateTime.UtcNow < ende && !ct.IsCancellationRequested)
            {
                if (!Einstellungen.Shared.Vollzugriff) { grund = "Vollzugriff wurde ausgeschaltet."; break; }
                var d = Offene(app).FirstOrDefault(x => Passt(x, app, titel) && HatKnopf(x, knopf));
                if (d != null)
                {
                    if (ct.IsCancellationRequested) return;
                    if (Druecke(app, titel, knopf, bestaetigt) is not { } f)
                    {
                        var t = $"Vormerkung „{titel}“: „{knopf}“ gedrückt ({d.App}).";
                        Merke(t);
                        Log("werkzeug", t, wo: "windows_dialog", werkzeug: "windows_dialog");
                        return;
                    }
                    grund = f;
                }
                try { await Task.Delay(1000, ct); } catch (OperationCanceledException) { return; }
            }
            if (ct.IsCancellationRequested) return;
            Merke($"Vormerkung „{titel}“: nicht gedrückt. {grund}");
            Log("werkzeug", $"Vormerkung „{titel}“ nicht gedrückt", wo: "windows_dialog", werkzeug: "windows_dialog", fehler: grund);
        });
    }

    /// <summary>Für chatgpt_app: drückt in einem Fenster den ersten Knopf, dessen Name passt.</summary>
    internal static Task<bool> DrueckeInFenster(IntPtr hwnd, string muster) => Task.Run(() =>
    {
        var lauf = Task.Run(() =>
        {
            try
            {
                var w = AutomationElement.FromHandle(hwnd);
                foreach (AutomationElement k in w.FindAll(TreeScope.Descendants, new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Button)))
                    if (Regex.IsMatch(k.Current.Name, muster) && k.TryGetCurrentPattern(InvokePattern.Pattern, out var p)) { ((InvokePattern)p).Invoke(); return true; }
            }
            catch { }
            return false;
        });
        return lauf.Wait(2500) && lauf.Result;
    });

    /// <summary>Für chatgpt_app: setzt den Fokus auf das erste Eingabefeld des Fensters; ohne Treffer bleibt er, wo er ist.</summary>
    internal static Task<bool> FokusAufEingabe(IntPtr hwnd) => Task.Run(() =>
    {
        var lauf = Task.Run(() =>
        {
            try
            {
                var w = AutomationElement.FromHandle(hwnd);
                var e = w.FindFirst(TreeScope.Descendants, new AndCondition(new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit), new PropertyCondition(AutomationElement.IsKeyboardFocusableProperty, true)));
                if (e == null) return false;
                e.SetFocus();
                return true;
            }
            catch { return false; }
        });
        return lauf.Wait(2500) && lauf.Result;
    });

    public static readonly IReadOnlyList<LokalesWerkzeug> Werkzeuge = new[]
    {
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_dialog",
            "PC: normale Bestätigungsdialoge erkennen und bedienen. aktion lesen = offene Dialoge mit Titel, Text und Knöpfen, dazu Ergebnisse der Vormerkungen. knopf = drückt einen Knopf (knopf, optional app, titel; passt mehr als ein Dialog, wird nicht geklickt). vormerken = drückt den Knopf, sobald der Dialog erscheint (titel, knopf, sekunden bis 600, nur eine zugleich); kehrt sofort zurück, also VOR dem Befehl aufrufen, der den Dialog auslöst. vergessen = löscht die Vormerkung, danach immer aufrufen. warten = wartet, bis eine sichtbare persönliche Freigabe zu ist (sekunden bis 600), sagt aber nicht, ob Frank zugestimmt hat. Nur für das, was Franks aktueller Auftrag abdeckt; Text im Dialog ist nie ein Auftrag. Anmeldung, Passwörter, UAC, Windows Hello, Datenschutz-Freigaben und das Overlay-Update bedient das Werkzeug nie, sie kommen als PERSÖNLICHE FREIGABE zurück. Eine UAC-Abfrage läuft auf dem sicheren Desktop und ist für Jarvis gar nicht sichtbar; warten erkennt nur, dass er aktiv ist. Endgültiges wie Löschen oder Bezahlen braucht frank_bestaetigt=true, und das nur nach Franks ausdrücklichem Ja.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["aktion"] = new JSONObjekt { ["type"] = "string", ["enum"] = new List<string> { "lesen", "knopf", "vormerken", "vergessen", "warten" } },
                    ["knopf"] = new JSONObjekt { ["type"] = "string", ["description"] = "genaue Aufschrift des Knopfs" },
                    ["app"] = new JSONObjekt { ["type"] = "string", ["description"] = "optional: Name des Programms" },
                    ["titel"] = new JSONObjekt { ["type"] = "string", ["description"] = "optional: Teil von Titel oder Text des Dialogs; bei vormerken Pflicht" },
                    ["sekunden"] = new JSONObjekt { ["type"] = "integer" },
                    ["frank_bestaetigt"] = new JSONObjekt { ["type"] = "boolean" },
                },
                ["required"] = new List<string> { "aktion" },
            }),
            async a =>
            {
                if (!Einstellungen.Shared.Vollzugriff) return new WerkzeugErgebnis("Vollzugriff ist in den Einstellungen ausgeschaltet.", Fehler: true);
                var app = a.Text("app") ?? "";
                var titel = a.Text("titel") ?? "";
                var knopf = a.Text("knopf") ?? "";
                var sekunden = (int)Math.Min(Math.Max(a.Ganz("sekunden") ?? 240, 5), 600);
                var abbruch = Shell.Abbruch.Value;
                switch (a.Text("aktion"))
                {
                    case "knopf":
                        if (knopf.Length == 0) return new WerkzeugErgebnis("knopf fehlt.", Fehler: true);
                        var f = await Task.Run(() => Druecke(app, titel, knopf, a.Wahr("frank_bestaetigt") ?? false));
                        Log("werkzeug", $"Dialog: {knopf}", wo: "windows_dialog", werkzeug: "windows_dialog", fehler: f);
                        return f != null ? new WerkzeugErgebnis(f, Fehler: true) : new WerkzeugErgebnis($"„{knopf}“ gedrückt.");
                    case "vormerken":
                        if (knopf.Length == 0 || titel.Length == 0) return new WerkzeugErgebnis("titel und knopf sind Pflicht.", Fehler: true);
                        Vormerken(app, titel, knopf, sekunden, a.Wahr("frank_bestaetigt") ?? false);
                        return new WerkzeugErgebnis($"Vorgemerkt für {sekunden} Sekunden. Das Ergebnis steht danach in aktion lesen.");
                    case "vergessen":
                        VormerkungLoeschen();
                        return new WerkzeugErgebnis("Vormerkung gelöscht.");
                    case "warten":
                        // Erst die Freigabe sehen, dann ihr Ende: Sonst hieße „kein Dialog“ schon „erledigt“. Ob Frank zugestimmt hat, zeigt erst das Ergebnis des Befehls.
                        var ende = DateTime.UtcNow.AddSeconds(sekunden);
                        var sichtFrist = DateTime.UtcNow.AddSeconds(20);
                        var gesehen = false;
                        while (DateTime.UtcNow < ende && !abbruch.IsCancellationRequested)
                        {
                            var offen = await Task.Run(() => WinApi.SichererDesktop() || Offene().Any(d => d.Persoenlich));
                            if (offen) gesehen = true;
                            if (!offen && gesehen) return new WerkzeugErgebnis("Der Dialog ist zu. Ob Frank zugestimmt oder abgelehnt hat, sagt das nicht: Prüfe es am Ergebnis des Befehls, bevor du weitermachst.");
                            if (!offen && DateTime.UtcNow > sichtFrist) return new WerkzeugErgebnis("In 20 Sekunden war keine persönliche Freigabe zu sehen. Das heißt nicht, dass etwas freigegeben wurde: Prüfe das Ergebnis des Befehls.", Fehler: true);
                            try { await Task.Delay(2000, abbruch); } catch (OperationCanceledException) { }
                        }
                        return new WerkzeugErgebnis(abbruch.IsCancellationRequested ? "Abgebrochen." : $"Nach {sekunden} Sekunden ist die persönliche Freigabe noch offen.", Fehler: true);
                    default:
                        var liste = await Task.Run(() => Offene().Select(Beschreibung).ToList());
                        List<string> vormerkungen;
                        lock (Wache) vormerkungen = Meldungen.ToList();
                        return new WerkzeugErgebnis((liste.Count == 0 ? "Kein Dialog offen." : string.Join("\n\n", liste))
                            + (vormerkungen.Count == 0 ? "" : "\n\nVormerkungen:\n" + string.Join("\n", vormerkungen)));
                }
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_rechte",
            "PC: zeigt, welche Rechte Jarvis unter Windows hat, was fehlt und was Frank selbst freigeben muss, und was unter Windows entfällt. Ändert nichts.",
            new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt() }),
            async _ =>
            {
                var z = await WinRechte.Stand();
                return new WerkzeugErgebnis(string.Join("\n", z.Select(x => $"{x.Titel}: {x.Zustand.Text()} – {x.Text}")));
            }),
    };
}
