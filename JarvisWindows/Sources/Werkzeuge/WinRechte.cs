using System.Security.Principal;
using System.Windows;
using System.Windows.Media;
using Microsoft.Win32;
using Windows.Security.Credentials.UI;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Der echte Stand der Rechte am PC, für Einstellungen → Windows & Rechte und das Werkzeug windows_rechte. Stellt nichts um; nur „Fehlende anfragen“ öffnet die passenden Seiten der Windows-Einstellungen.</summary>
public static class WinRechte
{
    /// <summary>Entfaellt = am Mac ein Recht, unter Windows nicht nötig.</summary>
    public enum ZustandArt { Ja, Nein, Offen, Unbekannt, Entfaellt }

    public static string Text(this ZustandArt z) => z switch
    {
        ZustandArt.Ja => "vorhanden",
        ZustandArt.Nein => "fehlt",
        ZustandArt.Offen => "noch nicht gefragt",
        ZustandArt.Entfaellt => "entfällt unter Windows",
        _ => "von Windows nicht abfragbar",
    };

    /// <param name="Bereich">Seite der Windows-Einstellungen; null = nichts zu öffnen.</param>
    public sealed record Zeile(string Titel, ZustandArt Zustand, string Text, string? Bereich = null);

    private const string Einwilligung = @"Software\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone";

    private static object? Wert(RegistryKey wurzel, string pfad, string name)
    {
        try { using var k = wurzel.OpenSubKey(pfad); return k?.GetValue(name); } catch { return null; }
    }

    private static ZustandArt Mikrofon()
    {
        var werte = new[] { Wert(Registry.LocalMachine, Einwilligung, "Value"), Wert(Registry.CurrentUser, Einwilligung, "Value"), Wert(Registry.CurrentUser, Einwilligung + @"\NonPackaged", "Value") }.OfType<string>().ToList();
        if (werte.Any(w => w.Equals("Deny", StringComparison.OrdinalIgnoreCase))) return ZustandArt.Nein;
        return werte.Count == 0 ? ZustandArt.Unbekannt : ZustandArt.Ja;
    }

    private static async Task<(ZustandArt, string)> Autostart()
    {
        foreach (var name in new[] { "JarvisWindowsWecker", AppInfo.Name })
        {
            var r = await Shell.Prozess("schtasks", new[] { "/query", "/tn", name }, sekunden: 10);
            if (r.Code == 0) return (ZustandArt.Ja, $"Aufgabe „{name}“ ist eingerichtet: Jarvis ist nach der Anmeldung erreichbar.");
        }
        foreach (var name in new[] { AppInfo.Name, "JarvisWindows" })
            if (Wert(Registry.CurrentUser, @"Software\Microsoft\Windows\CurrentVersion\Run", name) != null) return (ZustandArt.Ja, $"Autostart-Eintrag „{name}“ ist gesetzt.");
        return (ZustandArt.Nein, "Keine Autostart-Aufgabe gefunden; publish.ps1 richtet JarvisWindowsWecker ein.");
    }

    private static async Task<ZustandArt> WindowsHello()
    {
        try
        {
            var lauf = UserConsentVerifier.CheckAvailabilityAsync().AsTask();
            if (await Task.WhenAny(lauf, Task.Delay(5000)) != lauf) return ZustandArt.Unbekannt;
            return lauf.Result == UserConsentVerifierAvailability.Available ? ZustandArt.Ja : ZustandArt.Nein;
        }
        catch { return ZustandArt.Unbekannt; }
    }

    private static async Task<ZustandArt> UeberwachterOrdnerzugriff()
    {
        var r = await Shell.Pwsh("(Get-MpPreference).EnableControlledFolderAccess", sekunden: 10);
        return r.Code != 0 ? ZustandArt.Unbekannt : r.Ausgabe.Trim() == "1" ? ZustandArt.Nein : ZustandArt.Ja;
    }

    public static async Task<List<Zeile>> Stand()
    {
        var mikrofon = Mikrofon();
        var sprache = Wert(Registry.CurrentUser, @"Software\Microsoft\Speech_OneCore\Settings\OnlineSpeechPrivacy", "HasAccepted") switch { 1 => ZustandArt.Ja, int => ZustandArt.Nein, _ => ZustandArt.Offen };
        var mitteilung = Wert(Registry.CurrentUser, @"Software\Microsoft\Windows\CurrentVersion\PushNotifications", "ToastEnabled") is 0 ? ZustandArt.Nein : ZustandArt.Ja;
        var uacAus = Wert(Registry.LocalMachine, @"Software\Microsoft\Windows\CurrentVersion\Policies\System", "EnableLUA") is 0;
        var uacStill = uacAus || Wert(Registry.LocalMachine, @"Software\Microsoft\Windows\CurrentVersion\Policies\System", "ConsentPromptBehaviorAdmin") is 0;
        var sudo = Wert(Registry.LocalMachine, @"Software\Microsoft\Windows\CurrentVersion\Sudo", "Enabled") is int and not 0;
        using var ich = WindowsIdentity.GetCurrent();
        var erhoeht = new WindowsPrincipal(ich).IsInRole(WindowsBuiltInRole.Administrator);
        var autostartLauf = Autostart();
        var helloLauf = WindowsHello();
        var ordnerLauf = UeberwachterOrdnerzugriff();
        var (autostart, autostartText) = await autostartLauf;
        var hello = await helloLauf;
        var ordner = await ordnerLauf;
        return new List<Zeile>
        {
            new("Vollzugriff", Einstellungen.Shared.Vollzugriff ? ZustandArt.Ja : ZustandArt.Nein, "Jarvis darf am PC alles: Befehle, Programme, Dateien, Tastatur, Bildschirm, Terminal. Schaltet nur Frank um."),
            new("Mikrofon", mikrofon, "Weckwort, Diktat und Live. Unter Windows muss „Desktop-Apps den Zugriff erlauben“ an sein.", "ms-settings:privacy-microphone"),
            new("Autostart-Aufgabe", autostart, autostartText, "ms-settings:startupapps"),
            new("Spracherkennung", sprache, "Online-Spracherkennung für die Windows-Erkennung; Parakeet und Groq brauchen sie nicht.", "ms-settings:privacy-speech"),
            new("Mitteilungen", mitteilung, "Frank Bescheid sagen, wenn eine Freigabe nötig ist.", "ms-settings:notifications"),
            new("Windows Hello", hello, "Schlüssel im Tresor anzeigen.", "ms-settings:signinoptions"),
            new("Überwachter Ordnerzugriff", ordner, ordner == ZustandArt.Nein ? "Der Virenschutz sperrt das Schreiben in geschützte Ordner; Jarvis muss dort zugelassen werden." : "Geschützte Ordner lassen sich beschreiben. Kann nur Frank in der Windows-Sicherheit ändern."),
            new("Als Administrator", erhoeht ? ZustandArt.Ja : ZustandArt.Nein, erhoeht ? "Jarvis läuft erhöht und bedient auch erhöhte Fenster." : "Jarvis läuft als normaler Benutzer. Fenster, die als Administrator laufen, nehmen Klicks und Tasten nicht an, das meldet Windows auch nicht; am Foto prüfen."),
            new("sudo ohne Passwort", sudo ? ZustandArt.Ja : ZustandArt.Nein, sudo ? "Windows-sudo ist eingeschaltet; ob es fragt, hängt an den UAC-Einstellungen." : "Windows-sudo ist aus. Erhöhte Befehle zeigen eine UAC-Abfrage."),
            new("Admin-Fenster ohne Passwort", uacStill ? ZustandArt.Ja : ZustandArt.Nein, uacStill ? "Admin-Abfragen kommen nicht." : "UAC-Abfragen kommen und bleiben bei Frank: Sie laufen auf dem sicheren Desktop, Jarvis sieht und bedient sie nicht."),
            new("Bedienungshilfen", ZustandArt.Entfaellt, "Klicken, tippen, Dialoge lesen und bestätigen braucht unter Windows keine eigene Freigabe (SendInput, UI Automation)."),
            new("Bildschirmaufnahme", ZustandArt.Entfaellt, "Den Bildschirm ansehen braucht unter Windows keine eigene Freigabe."),
            new("Automation", ZustandArt.Entfaellt, "PowerShell und UI Automation steuern Programme ohne Einzelfreigabe."),
            new("Festplattenvollzugriff", ZustandArt.Entfaellt, "Jarvis liest alles, was Frank als Benutzer lesen darf; Grenze ist der überwachte Ordnerzugriff."),
            new("Programme verwalten", ZustandArt.Entfaellt, "Eine eigene Freigabe gibt es nicht; was in Program Files liegt, verlangt UAC."),
            new("Softwareinstallation ohne Passwort", ZustandArt.Entfaellt, "winget und Installer zeigen bei Bedarf eine UAC-Abfrage. Das bestätigt Frank selbst, danach arbeitet Jarvis weiter."),
        };
    }

    public static void Oeffnen(string bereich)
    {
        try { Shell.Starte(bereich); }
        catch (Exception ex) { Log("fehler", $"Einstellungen nicht geöffnet: {bereich}", wo: "Windows & Rechte", fehler: ex.Message); }
    }

    /// <summary>Windows kennt keine Abfrage, die ein Desktop-Programm auslösen dürfte: Für alles, was fehlt, geht die passende Seite der Einstellungen auf.</summary>
    public static async Task FehlendeAnfragen()
    {
        foreach (var z in await Stand())
            if (z.Zustand is ZustandArt.Nein or ZustandArt.Offen && z.Bereich != null) Oeffnen(z.Bereich);
    }
}

/// <summary>Einstellungen → Windows & Rechte: was da ist, was fehlt, was Frank selbst freigeben muss.</summary>
public sealed class WinRechteAnsicht : LebendeAnsicht
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private List<WinRechte.Zeile> zeilen = new();
    private bool fragt;

    public WinRechteAnsicht() { Beobachte(einst, nameof(Einstellungen.Vollzugriff)); }

    protected override void Erschienen() => _ = Lade();

    private async Task Lade()
    {
        zeilen = await WinRechte.Stand();
        Content = Baue();
    }

    private async Task Fragen()
    {
        if (fragt) return;
        fragt = true;
        Content = Baue();
        await WinRechte.FehlendeAnfragen();
        zeilen = await WinRechte.Stand();
        fragt = false;
        Content = Baue();
    }

    protected override UIElement Baue()
    {
        var liste = Ui.V(10, Ui.H(8,
            new KartenTitel("checkmark.shield.fill", "Rechte von Windows", Farbe.Gruen), Ui.Spacer(),
            new KapselKnopf("hand.raised.fill", fragt ? "Fragt …" : "Fehlende anfragen", () => _ = Fragen(), hervorgehoben: true),
            new RundKnopf("arrow.clockwise", () => _ = Lade(), hilfe: "Neu prüfen", groesse: 30)));
        if (zeilen.Count == 0) liste.Children.Add(new Spinner().Links());
        foreach (var z in zeilen)
        {
            var gut = z.Zustand is WinRechte.ZustandArt.Ja or WinRechte.ZustandArt.Entfaellt;
            var symbol = gut ? "checkmark.circle.fill" : z.Zustand == WinRechte.ZustandArt.Unbekannt ? "questionmark.circle" : "exclamationmark.circle.fill";
            Brush farbe = gut ? Farbe.Gruen : z.Zustand == WinRechte.ZustandArt.Unbekannt ? Farbe.Text3 : Farbe.Warnung;
            liste.Children.Add(Ui.H(12,
                Sy(symbol, 14, farbe),
                Ui.V(2, Ui.Tx(z.Titel, 13, FontWeights.SemiBold), Ui.Tx(z.Text, 11, null, Farbe.Text2)).Fuell(),
                Ui.Tx(z.Zustand.Text(), 11, FontWeights.Medium, Farbe.Text3, zeilen: 1),
                z.Bereich is { } b ? TextKnopf("Öffnen", 11, Farbe.Haupt, () => WinRechte.Oeffnen(b)) : null).Rand(0, 3));
        }
        return Ui.V(18,
            new GlasKarte(Ui.V(8,
                new KartenTitel("bolt.shield.fill", "Vollzugriff", Farbe.Warnung),
                Ui.H(16,
                    Ui.V(3, Ui.Tx("Jarvis darf am PC alles", 14, FontWeights.SemiBold),
                        Ui.Tx("Befehle, Programme, Dateien, Tastatur, Bildschirm, Terminal und normale Bestätigungsdialoge eines Auftrags. Aus = nur Programme und Adressen öffnen, Mitteilungen, Status.", 12, null, Farbe.Text2)).Fuell(),
                    new Schalter(() => einst.Vollzugriff, an => einst.Vollzugriff = an)))),
            new GlasKarte(liste),
            new GlasKarte(Ui.V(8,
                new KartenTitel("person.badge.shield.checkmark.fill", "Das bleibt bei dir", Farbe.Gold),
                Ui.Tx("Passwörter und Zugangsdaten gibt Jarvis nie ein und speichert sie nie. Anmeldung, Windows Hello, UAC und Datenschutz-Freigaben von Windows bestätigst du selbst: Jarvis sagt dir dann, was fehlt, und arbeitet danach weiter. Normale Dialoge eines Auftrags, etwa das Ja zu einem beauftragten OpenLauncher-Update, bestätigt Jarvis selbst. Das Update der Overlays bleibt dein Klick.", 12, null, Farbe.Text2))));
    }
}
