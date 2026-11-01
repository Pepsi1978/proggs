using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using Windows.ApplicationModel.DataTransfer;
using static JarvisWindows.Bau;

namespace JarvisWindows;

// MARK: - Prompt-Speicher (wie Reiter „Prompts“ in der Handy-Ablage)

public sealed record PromptEintrag
{
    public PromptEintrag() { }
    public PromptEintrag(int nummer, string titel, string text, long erstellt = 0, string quelle = "")
    {
        Nummer = nummer; Titel = titel; Text = text; Erstellt = erstellt; Quelle = quelle;
    }
    public int Nummer { get; set; }
    public string Titel { get; init; } = "";
    public string Text { get; init; } = "";
    /// <summary>Feste Kennung des Handys (Anlagezeitpunkt); 0 = unbekannt.</summary>
    public long Erstellt { get; set; }
    /// <summary>Die Jarvis-Version, von der der Prompt stammt.</summary>
    public string Quelle { get; set; } = "";
    public long Id => Erstellt != 0 ? Erstellt : Nummer;
}

/// <summary>Eigene Ansicht, damit Tippen im Entwurf nicht jedes Mal die ganze Prompt-Liste neu auswertet.</summary>
public sealed class NeuerPromptKarte : UserControl
{
    private string fehler = "";
    private readonly TextBlock fehlerText = T("", 12, null, Farbe.Warnung);

    public NeuerPromptKarte()
    {
        var steuerung = AppSteuerung.Shared;
        var entwurf = new TextBindung(() => steuerung.PromptEntwurf ?? "", v => steuerung.PromptEntwurf = v);
        System.ComponentModel.PropertyChangedEventHandler nach = (_, e) => { if (e.PropertyName == nameof(steuerung.PromptEntwurf)) entwurf.Melde(); };
        Loaded += (_, _) => steuerung.PropertyChanged += nach;
        Unloaded += (_, _) => steuerung.PropertyChanged -= nach;

        var editor = Flaeche(Feld(entwurf, "", 13, mehrzeilig: true), Farbe.Tiefe(0.25), 10, 6, 6);
        editor.MinHeight = 70;
        fehlerText.Visibility = Visibility.Collapsed;
        Content = Karte(V(10,
            Titel("plus.circle.fill", "Neuer Prompt", Farbe.Gruen),
            editor,
            new PromptWerkzeuge(entwurf),
            H(10,
                Kapsel("arrow.up.left.and.arrow.down.right", "Im Vollbild schreiben", () => steuerung.PromptBearbeiten = new PromptEintrag(0, "", steuerung.PromptEntwurf ?? "")),
                Kapsel("square.and.arrow.down.fill", "Speichern", async () =>
                {
                    var t = steuerung.PromptEntwurf ?? "";
                    // Nur Gespeichertes verschwindet aus dem Feld.
                    try { await AblageAbgleich.Shared.PromptSpeichern(t); }
                    catch (Exception ex) { Zeige(ex.Message); return; }
                    Zeige("");
                    if (steuerung.PromptEntwurf == t) steuerung.PromptEntwurf = "";
                }, hervorgehoben: true),
                Spacer()),
            fehlerText));
    }

    private void Zeige(string t)
    {
        fehler = t;
        fehlerText.Text = fehler;
        fehlerText.Visibility = fehler.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
    }
}

public sealed class PromptsAnsicht : LebendeAnsicht
{
    private readonly AblageAbgleich abgleich = AblageAbgleich.Shared;
    private string fehler = "";
    private PromptEintrag? offen;
    private readonly NeuerPromptKarte neu = new();
    private readonly StackPanel liste = new();
    private readonly ContentControl kopf = new();
    private readonly TextBlock fehlerText = T("", 12, null, Farbe.Warnung);
    private readonly StackPanel gesamt;

    private bool NachQuelle { get => Vorgaben.Wahr("prompts.nachQuelle", false); set => Vorgaben.Setze("prompts.nachQuelle", value); }

    public PromptsAnsicht()
    {
        Beobachte(abgleich, nameof(abgleich.Prompts), nameof(abgleich.AufbauStand));
        Beobachte(abgleich.Prompts);
        fehlerText.Visibility = Visibility.Collapsed;
        gesamt = V(16, neu, kopf, fehlerText, liste);
    }

    protected override void Erschienen() => QuellenOrdnung.Geaendert += Neu;
    protected override void Verschwunden() => QuellenOrdnung.Geaendert -= Neu;

    protected override UIElement Baue()
    {
        // Karte und Feld bleiben stehen: Nur Kopf und Liste entstehen neu, sonst verlöre das Eingabefeld beim Abgleich den Fokus.
        kopf.Content = BaueKopf();
        fehlerText.Text = fehler;
        fehlerText.Visibility = fehler.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
        liste.Children.Clear();
        foreach (var p in Angezeigt()) VAnfuegen(liste, Karte_(p), 16);
        return gesamt;
    }

    private UIElement BaueKopf()
    {
        var beschriftung = H(6, Sy("arrow.up.arrow.down", 12, Farbe.Text), T(NachQuelle ? "Nach Quelle" : "Eigene Reihenfolge", 12));
        Button? k = null;
        k = Klick(beschriftung, () => OeffneMenue(k!, new[]
        {
            new MenueZeile("Eigene Reihenfolge", () => { NachQuelle = false; Neu(); }, false, !NachQuelle),
            new MenueZeile("Nach Quelle", () => { NachQuelle = true; Neu(); }, false, NachQuelle),
            MenueZeile.Trenner,
            new MenueZeile("Reihenfolge der Quellen …", () => OeffnePopup(k!, new QuellenListe())),
        }));
        return H(0, Spacer(), k);
    }

    private UIElement Karte_(PromptEintrag p)
    {
        var istOffen = offen?.Id == p.Id;
        var text = T(p.Text, 12, null, Farbe.Text2, istOffen ? 0 : 2);
        var nummer = T(p.Nummer.ToString(), 18, FontWeights.Black, Farbe.Akzent);
        nummer.Width = 30;
        nummer.TextAlignment = TextAlignment.Center;
        var oben = Klick(HOben(8, nummer, Fuell(V(4,
            T(p.Titel.Length == 0 ? $"Prompt {p.Nummer}" : p.Titel, 14, FontWeights.SemiBold),
            T($"Von {p.Quelle}", 11, null, Farbe.Text3),
            text))), () => { offen = offen?.Id == p.Id ? null : p; Neu(); });
        return Karte(V(8, oben, H(8,
            Spacer(),
            Klein("trash", "Löschen", async () =>
            {
                if (!Bestaetigen($"Prompt {p.Nummer} auf allen Jarvis-Versionen löschen?", "", "Löschen")) return;
                try { await abgleich.PromptLoeschen(p.Erstellt); fehler = ""; } catch (Exception ex) { fehler = ex.Message; }
                Neu();
            }),
            Klein("doc.on.doc", "Kopieren", () => Kopiere(p.Text)),
            Klein("square.and.pencil", "Bearbeiten", () => AppSteuerung.Shared.PromptBearbeiten = p),
            Kapsel("paperplane.fill", "Senden", () =>
            {
                AppSteuerung.Shared.NeuesGespraech();
                AppSteuerung.Shared.Entwurf = "Übergib bitte folgende Aufgabe an den Programmierer: " + p.Text;
                AppSteuerung.Shared.PromptLoeschenFrage = p;
            }, hervorgehoben: true))), 14);
    }

    /// <summary>Nach Quelle sortiert ändert nur die Anzeige: Innerhalb einer Quelle und bei den Nummern bleibt es bei der eigenen Reihenfolge.</summary>
    private List<PromptEintrag> Angezeigt()
    {
        var alle = abgleich.Prompts.ToList();
        if (!NachQuelle) return alle;
        var o = QuellenOrdnung.Liste(QuellenOrdnung.Wert);
        return alle.OrderBy(a => QuellenOrdnung.Rang(o, a.Quelle)).ThenBy(a => a.Nummer).ToList();
    }
}

// MARK: - Stil lernen aus Daumen hoch/runter (wie am Handy: daraus lernt die Stil-Datei)

public static class StilLernen
{
    public static void Bewerte(string antwort, string frage, bool gut)
    {
        Log("system", $"Antwort bewertet: {(gut ? "👍" : "👎")}");
        _ = Haupt.Auf(async () =>
        {
            var prompt = $"Frank hat diese Antwort von Jarvis mit {(gut ? "Daumen hoch (gefällt ihm)" : "Daumen runter (gefällt ihm nicht)")} bewertet.\n"
                + $"Frage: {frage.Schnitt(1500)}\n"
                + $"Antwort: {antwort.Schnitt(4000)}\n\n"
                + "Leite daraus höchstens EINE allgemeine, dauerhaft gültige Regel für Form, Länge, Gliederung oder Ton von Jarvis' Antworten ab "
                + "(nicht zum Inhalt), als vollständigen Satz an Jarvis. Ist nichts Verallgemeinerbares erkennbar, antworte genau: KEINE";
            var regel = await Kurzauftrag.Frage(prompt);
            if (regel == null || regel.ToUpperInvariant().StartsWith("KEINE", StringComparison.Ordinal) || regel.Length <= 15) return;
            var r = await McpKlient.Shared.Rufe("jarvis_merken", new JSONObjekt { ["art"] = "stil", ["text"] = regel.Schnitt(400) });
            Log("system", $"Stil gelernt: {regel} → {(r.Fehler ? "Fehler" : "gemerkt")}");
        });
    }
}

// MARK: - Teilen

/// <summary>Windows-Teilen-Dialog; geht er nicht, landet der Text in der Zwischenablage.</summary>
public sealed class TeilenKnopf : UserControl
{
    public string Text { get; set; }

    public TeilenKnopf() : this("") { }
    public TeilenKnopf(string text)
    {
        Text = text;
        Content = Klick(Sy("square.and.arrow.up", 13, Farbe.Text2), Teilen, "Teilen");
    }

    private void Teilen()
    {
        try
        {
            var fenster = Window.GetWindow(this);
            var hwnd = fenster == null ? IntPtr.Zero : new WindowInteropHelper(fenster).Handle;
            if (hwnd == IntPtr.Zero) throw new InvalidOperationException("Kein Fenster");
            var manager = DataTransferManagerInterop.GetForWindow(hwnd);
            var text = Text;
            Windows.Foundation.TypedEventHandler<DataTransferManager, DataRequestedEventArgs>? behandler = null;
            behandler = (m, a) =>
            {
                m.DataRequested -= behandler;
                a.Request.Data.Properties.Title = AppInfo.Name;
                a.Request.Data.SetText(text);
            };
            manager.DataRequested += behandler;
            DataTransferManagerInterop.ShowShareUIForWindow(hwnd);
        }
        catch (Exception ex)
        {
            Kopiere(Text);
            Log("fehler", "Teilen-Dialog nicht verfügbar, Text in die Zwischenablage gelegt", wo: "Teilen", fehler: ex.Message);
        }
    }
}

// MARK: - Projekte (benannte Gruppen von Gesprächen, wie „Projekte“ in der Handy-Leiste)

public sealed class Projekte : Beobachtbar
{
    public static Projekte Shared { get; } = new();
    public ObservableCollection<string> Namen { get; }

    public Projekte()
    {
        var gespeichert = Vorgaben.Objekt("projekte") as List<object?>;
        Namen = new ObservableCollection<string>(gespeichert?.OfType<string>() ?? Enumerable.Empty<string>());
        Namen.CollectionChanged += (_, _) => { Vorgaben.Setze("projekte", Namen.Cast<object?>().ToList()); Melde(nameof(Namen)); };
    }

    public void Neu(string name)
    {
        var n = name.Trim();
        if (n.Length == 0 || Namen.Contains(n)) return;
        Namen.Insert(0, n);
    }
}
