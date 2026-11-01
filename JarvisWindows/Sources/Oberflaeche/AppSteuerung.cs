using System.ComponentModel;
using System.Collections.Specialized;
using System.Windows;
using System.Windows.Interop;

namespace JarvisWindows;

public enum Bereich { Start, Zentrale, Ablage, Gespraech, Live, Einstellungen }

public static class BereichInfo
{
    extension(Bereich b)
    {
        public string Titel => b switch
        {
            Bereich.Start => "Start", Bereich.Zentrale => "Zentrale", Bereich.Ablage => "Ablage", Bereich.Gespraech => "Gespräch", Bereich.Live => "Live", _ => "Einstellungen",
        };
        public string Symbol => b switch
        {
            Bereich.Start => "house.fill", Bereich.Zentrale => "sparkles", Bereich.Ablage => "tray.full.fill", Bereich.Gespraech => "bubble.left.and.text.bubble.right.fill", Bereich.Live => "waveform", _ => "gearshape.fill",
        };
    }
}

/// <summary>Zentrale Steuerung: Navigation, Weckwort, globales Tastenkürzel, Sprachpille.</summary>
public sealed class AppSteuerung : Beobachtbar
{
    public static AppSteuerung Shared { get; } = new();

    private Bereich _bereich = Bereich.Start;
    public Bereich Bereich
    {
        get => _bereich;
        set
        {
            if (!Setze(ref _bereich, value)) return;
            Log("oberflaeche", $"Bereich geöffnet: {value.Titel}", wo: "Seitenleiste");
            Anhang = null;
        }
    }

    private Gespraech? _gespraech;
    public Gespraech? Gespraech
    {
        get => _gespraech;
        // Ein „Nachtrag:“ gehört zur Sitzung, in der er begonnen wurde: Im nächsten Gespräch bleibt nur der Text.
        set
        {
            if (ReferenceEquals(_gespraech, value)) return;
            if (_gespraech != null) { _gespraech.PropertyChanged -= GespraechGeaendert; _gespraech.Nachrichten.CollectionChanged -= GespraechGeaendert; }
            _gespraech = value;
            if (value != null) { value.PropertyChanged += GespraechGeaendert; value.Nachrichten.CollectionChanged += GespraechGeaendert; }
            Melde();
            Melde(nameof(GespraechInhalt));
            Melde(nameof(NachtragMoeglich));
            Anhang = null;
            ProgrammierVorwahlSchliessen();
            if (NachtragAktiv) EingabeText = ProgrammierVorspann.OhneNachtrag(EingabeText);
        }
    }
    private void GespraechGeaendert(object? s, EventArgs e)
    {
        if (e is PropertyChangedEventArgs p && p.PropertyName is not (nameof(JarvisWindows.Gespraech.AuftragId) or nameof(JarvisWindows.Gespraech.Arbeitet))) return;
        Melde(nameof(GespraechInhalt));
        Melde(nameof(NachtragMoeglich));
    }
    /// <summary>Nur zum Beobachten: meldet, wenn sich im offenen Gespräch Nachrichtenzahl, Arbeitet oder AuftragId ändert.</summary>
    public int GespraechInhalt => _gespraech?.Nachrichten.Count ?? 0;

    private string _eingabeText = "";
    /// <summary>Der Text der Sprechblase, wie am Handy einer für alle Ansichten: Er überlebt jeden Ansichtswechsel, und die schwebenden Knöpfe können ihn bearbeiten.</summary>
    public string EingabeText
    {
        get => _eingabeText;
        set
        {
            if (!Setze(ref _eingabeText, value)) return;
            if (value.Length > 0) { GeleerterText = null; BlaseKompakt = false; }
            Melde(nameof(ProgrammiererAktiv));
            Melde(nameof(NachtragAktiv));
        }
    }
    private double _blaseHoehe = 96;
    /// <summary>Höhe der Blase samt Rand: Die schwebenden Knöpfe stehen direkt darüber.</summary>
    public double BlaseHoehe { get => _blaseHoehe; set => Setze(ref _blaseHoehe, value); }
    private bool _blaseKompakt = true;
    /// <summary>Eingeklappt zeigt die Blase nur ihre untere Zeile; mit Text bleibt sie immer offen.</summary>
    public bool BlaseKompakt { get => _blaseKompakt; set => Setze(ref _blaseKompakt, value); }
    private int _blaseFokus;
    /// <summary>Zählt hoch, wenn die Blase den Fokus bekommen soll.</summary>
    public int BlaseFokus { get => _blaseFokus; set => Setze(ref _blaseFokus, value); }
    private string? _geleerterText;
    /// <summary>Was der Papierkorb der Blase zuletzt geleert hat, für „Rückgängig“.</summary>
    public string? GeleerterText { get => _geleerterText; set => Setze(ref _geleerterText, value); }
    private byte[]? _anhang;
    /// <summary>Das Bild für die nächste Frage (`@State anhang` von Start und Chat). Liegt hier, damit es den Neubau beim Wechsel Hell/Dunkel überlebt; ein Wechsel von Bereich oder Gespräch verwirft es wie die Mac-Ansichten.</summary>
    public byte[]? Anhang { get => _anhang; set => Setze(ref _anhang, value); }
    /// <summary>Vorformulierter Text für die Blase (Prompt senden, in neuem Gespräch fortsetzen). Schon Getipptes bleibt dahinter stehen.</summary>
    public string Entwurf
    {
        get => EingabeText;
        set { EingabeText = AufgabenText.Verbinde(value, EingabeText); if (value.Length > 0) BlaseFokus += 1; }
    }
    private PromptEintrag? _promptBearbeiten;
    /// <summary>Der Prompt, der gerade im Vollbild bearbeitet wird.</summary>
    public PromptEintrag? PromptBearbeiten { get => _promptBearbeiten; set => Setze(ref _promptBearbeiten, value); }
    private string _promptEntwurf = "";
    /// <summary>Der noch nicht gespeicherte neue Prompt: Er überlebt das Vollbild und den Wechsel des Bereichs.</summary>
    public string PromptEntwurf { get => _promptEntwurf; set => Setze(ref _promptEntwurf, value); }
    private PromptEintrag? _promptLoeschenFrage;
    /// <summary>Der eben gesendete Prompt, solange die Frage offen ist, ob er aus der Ablage weg soll.</summary>
    public PromptEintrag? PromptLoeschenFrage { get => _promptLoeschenFrage; set => Setze(ref _promptLoeschenFrage, value); }
    private string? _promptLoeschenFehler;
    public string? PromptLoeschenFehler { get => _promptLoeschenFehler; set => Setze(ref _promptLoeschenFehler, value); }
    private bool _handyVerbunden;
    public bool HandyVerbunden { get => _handyVerbunden; set => Setze(ref _handyVerbunden, value); }
    private string _handyVersion = "";
    public string HandyVersion { get => _handyVersion; set => Setze(ref _handyVersion, value); }
    private bool kuerzelAn;
    private bool _appVorn = true;
    /// <summary>Jarvis ist die vordere App. Danach richten sich die Animationen: Im Hintergrund rechnen sie nicht.</summary>
    public bool AppVorn { get => _appVorn; private set { if (Setze(ref _appVorn, value)) Takt.Abgleichen(); } }
    private readonly SprachPille pille = new();
    /// <summary>Laufende Fragen aus der Eingabeblase, zum Abbrechen.</summary>
    private readonly Dictionary<int, (Guid Gespraech, CancellationTokenSource Abbruch)> fragen = new();
    private int fragenNummer;

    public void Hochfahren()
    {
        DesignStand.Shared.Starten();
        VorderApp.Beobachten();
        TresorSperre.Shared.Beobachten();
        SprachSchleife.Shared.PropertyChanged += (_, e) => { if (e.PropertyName is nameof(SprachSchleife.Zustand) or null) Melde(nameof(Fragen)); };
        Weckwort.Shared.BeiErkennung = () => Haupt.Post(WeckwortErkannt);
        if (Einstellungen.Shared.WeckwortAn) Weckwort.Shared.Starten();
        if (Einstellungen.Shared.GlobalesKuerzel) KuerzelRegistrieren();
        AgentenKanal.Shared.Starten();
        Ui.Starte(async () =>
        {
            while (true)
            {
                // Frisch im Hintergrund: Die Liste ist warm und der Stand stimmt, wenn Frank fragt.
                var liste = await McpKlient.Shared.Werkzeuge(frisch: true);
                HandyVerbunden = liste.Count > 0;
                HandyVersion = await McpKlient.Shared.ServerVersion();
                var unsicher = await McpKlient.Shared.Unsicher();
                await Task.Delay(TimeSpan.FromSeconds(HandyVerbunden && !unsicher ? 300 : 30));
            }
        });
        // Guthaben wie am Handy alle 15 Minuten, nicht während Live und nicht, solange weder Handy noch eigene Console-Anmeldung es liefern können.
        Ui.Starte(async () => { if (await ConsoleSitzung.AltbestandUebernehmen()) await Guthaben.Shared.Laden(frisch: true); });
        Ui.Starte(async () =>
        {
            while (true)
            {
                if (!(HandyVerbunden || ConsoleSitzung.Angemeldet) || LiveGespraech.Shared.Aktiv) { await Task.Delay(TimeSpan.FromSeconds(30)); continue; }
                await Guthaben.Shared.Laden();
                await Task.Delay(TimeSpan.FromSeconds(900));
            }
        });
    }

    private string? _projekt;
    /// <summary>Offenes Projekt in der Leiste.</summary>
    public string? Projekt { get => _projekt; set => Setze(ref _projekt, value); }

    /// <summary>imProjekt = aus der Projektleiste („Neues Gespräch im Projekt“); alles andere beginnt auf dem Startbildschirm.</summary>
    public void NeuesGespraech(bool imProjekt = false)
    {
        if (!imProjekt) { StartOeffnen(spracheBeenden: false); return; }
        // Ein offener Entwurf bleibt: Mehrmaliges Drücken legt nichts an.
        var g = EntwurfOderNeu();
        g.Projekt = Projekt;
        Gespraech = g;
        Bereich = Bereich.Gespraech;
    }

    /// <summary>„Start“: neues Gespräch mit offener Blase. Angelegt wird es erst mit der ersten Nachricht, leere Gespräche gibt es also nicht.</summary>
    public void StartOeffnen(bool spracheBeenden = true)
    {
        _ = McpKlient.Shared.KontextVorladen();
        if (spracheBeenden && SprachSchleife.Shared.Zustand != SprachZustand.Aus) SprachSchleife.Shared.Beenden();
        Gespraech = null;
        Bereich = Bereich.Start;
        BlaseKompakt = false;
        BlaseFokus += 1;
    }

    /// <summary>„Neue Session“ der schwebenden Knöpfe, wie am Handy: leeres Gespräch, die Blase klappt ohne Text ein.</summary>
    public void NeueSession()
    {
        ProgrammierVorwahlSchliessen();
        StartOeffnen();
        BlaseKompakt = true;
    }

    // MARK: Programmierung und Nachtrag (Bedienlogik wie ProgrammierSchweber und AppViewModel am Handy)

    public bool ProgrammiererAktiv => ProgrammierVorspann.HatAuftrag(EingabeText);
    private bool _programmierVorwahlOffen;
    public bool ProgrammierVorwahlOffen { get => _programmierVorwahlOffen; set => Setze(ref _programmierVorwahlOffen, value); }
    private CancellationTokenSource? vorwahlFrist;
    public bool NachtragAktiv => ProgrammierVorspann.HatNachtrag(EingabeText);
    public bool NachtragMoeglich => Gespraech?.AuftragId != null;

    private void ZurBlase()
    {
        BlaseKompakt = false;
        BlaseFokus += 1;
    }

    /// <summary>Setzt den Vorspann vor den Text; ein zweiter Klick nimmt genau ihn wieder heraus. Ein „Nachtrag:“ vorn weicht ihm.</summary>
    public void ProgrammierVorwahlSchliessen()
    {
        vorwahlFrist?.Cancel();
        ProgrammierVorwahlOffen = false;
    }

    public void ProgrammiererUmschalten()
    {
        vorwahlFrist?.Cancel();
        if (ProgrammiererAktiv) { ProgrammierVorwahlOffen = false; EingabeText = ProgrammierVorspann.OhneAuftrag(EingabeText); return; }
        ProgrammierVorwahlOffen = !ProgrammierVorwahlOffen;
        // Vergessene Modellwahl schließt sich wie am Handy nach einer Minute.
        if (ProgrammierVorwahlOffen)
        {
            var frist = vorwahlFrist = new CancellationTokenSource();
            Ui.Starte(async () =>
            {
                await Task.Delay(TimeSpan.FromSeconds(60), frist.Token);
                ProgrammierVorwahlOffen = false;
            });
        }
    }

    /// <summary>Knopf der Modellwahl: setzt den Einstieg mit diesem Modell vor den Text; gesendet wird erst von Frank.</summary>
    public void ProgrammierVorwahl(ProgrammierVorspann.Modell modell)
    {
        ProgrammierVorwahlSchliessen();
        EingabeText = ProgrammierVorspann.MitAuftrag(ProgrammierVorspann.OhneAuftrag(ProgrammierVorspann.OhneNachtrag(EingabeText)), vorspann: modell.Vorspann);
        ZurBlase();
    }

    public void NachtragUmschalten()
    {
        if (!NachtragMoeglich) return;
        ProgrammierVorwahlSchliessen();
        if (NachtragAktiv) { EingabeText = ProgrammierVorspann.OhneNachtrag(EingabeText); return; }
        EingabeText = ProgrammierVorspann.MitNachtrag(ProgrammierVorspann.OhneAuftrag(EingabeText));
        ZurBlase();
    }

    /// <summary>„Nachtrag:“ vorn: Der Text geht ohne Modell in genau die Sitzung dieses Gesprächs. Geht er nicht raus und das Gespräch ist noch offen, kommt er in die Blase zurück.</summary>
    public void NachtragSenden(Gespraech g, string id, string text)
    {
        var roh = EingabeText;
        EingabeText = "";
        BlaseKompakt = true;
        var gesendet = new Nachricht(Rolle.Frank, $"{ProgrammierVorspann.Nachtrag} {text}");
        var antwortFeld = new Nachricht(Rolle.Jarvis, "", laeuft: true);
        g.Nachrichten.Add(gesendet);
        g.Nachrichten.Add(antwortFeld);
        g.Speichern();
        Ui.Starte(async () =>
        {
            var r = await McpKlient.Shared.Rufe("programmier_nachtrag", new JSONObjekt { ["auftrag_id"] = id, ["text"] = text, ["franks_worte"] = $"Nachtrag in die gleiche Session {id}" });
            Log("gespraech", "Nachtrag per Knopf", wo: "Schwebeknöpfe", fehler: r.Fehler ? r.Text : null);
            var antwort = McpKlient.OhneKontextHinweis(r.Text).Trim();
            if (g.Nachrichten.Contains(antwortFeld))
            {
                antwortFeld.Text = antwort.Length == 0 ? (r.Fehler ? "Der Nachtrag ging nicht raus." : "Nachtrag übergeben.") : antwort;
                antwortFeld.Laeuft = false;
            }
            if (r.Fehler)
            {
                // Noch im selben Gespräch und die Blase ist frei: zum erneuten Senden zurück; sonst bleibt der Text im Gespräch stehen.
                if (ReferenceEquals(Gespraech, g) && EingabeText.Length == 0)
                {
                    g.Nachrichten.Remove(gesendet);
                    EingabeText = roh;
                }
            }
            else
            {
                // Der Verlauf ans Modell darf hier nicht wachsen, er könnte gerade zwischen tool_use und tool_result stehen: Die nächste Frage bringt den Nachtrag mit.
                g.GesendeteNachtraege.Add(text);
            }
            g.Speichern();
        });
    }

    private Gespraech EntwurfOderNeu()
    {
        _ = McpKlient.Shared.KontextVorladen();
        if (Gespraech is { } g && g.Nachrichten.Count == 0 && !g.Arbeitet) return g;
        return GespraechsSpeicher.Shared.Neu();
    }

    /// <summary>Ja auf die Frage nach dem Senden: löscht nur den Eintrag der Ablage, der Text in der Eingabe bleibt.</summary>
    public void GesendetenPromptLoeschen(PromptEintrag p)
    {
        Ui.Starte(async () =>
        {
            // Über die feste Kennung und nur bei unverändertem Text: Eine inzwischen geänderte Fassung bleibt stehen.
            var r = await AblageAbgleich.Shared.Werkzeug("prompt_speichern", new JSONObjekt { ["aktion"] = "loeschen", ["nummer"] = p.Nummer, ["erstellt"] = p.Erstellt, ["text"] = p.Text });
            if (r is { Fehler: true }) { PromptLoeschenFehler = r.Text; return; }
            Log("ablage", "Gesendeten Prompt gelöscht", wo: "Ablage/Prompts");
        });
    }

    public void Oeffne(Gespraech g)
    {
        Gespraech = g;
        Bereich = Bereich.Gespraech;
    }

    /// <summary>Nur zum Beobachten: meldet, wenn eine Frage beginnt oder endet oder sich das Sprachgespräch ändert (Abbrechbar neu prüfen).</summary>
    public int Fragen => fragen.Count;

    /// <summary>Der Agentenkanal hat einen Lauf begonnen oder beendet: Der Stopp-Knopf prüft neu.</summary>
    public void AbbruchStandGeaendert() => Melde(nameof(Fragen));

    /// <summary>Text an Jarvis schicken (aus Zentrale, Menüleiste oder Chat).</summary>
    public void Sende(string text, bool gesprochen = false, byte[]? bild = null)
    {
        ProgrammierVorwahlSchliessen();
        var t = text.Trim();
        if (t.Length == 0 && bild == null) return;
        Log("gespraech", "Frage an Jarvis: " + t.Schnitt(500), wo: gesprochen ? "Sprache" : "Texteingabe");
        var g = Gespraech ?? GespraechsSpeicher.Shared.Neu();
        Gespraech = g;
        Bereich = Bereich.Gespraech;
        fragenNummer += 1;
        var nr = fragenNummer;
        var abbruch = new CancellationTokenSource();
        fragen[nr] = (g.Id, abbruch);
        Melde(nameof(Fragen));
        Ui.Starte(async () =>
        {
            try
            {
                var antwort = await JarvisAgent.Shared.Frage(t.Length == 0 ? "Schau dir das Bild an." : t, g, gesprochen: gesprochen, bild: bild, ct: abbruch.Token);
                fragen.Remove(nr);
                Melde(nameof(Fragen));
                if (gesprochen && Einstellungen.Shared.Vorlesen && !abbruch.IsCancellationRequested) await Stimme.Shared.Sprich(antwort);
            }
            finally
            {
                if (fragen.Remove(nr)) Melde(nameof(Fragen));
            }
        });
    }

    /// <summary>Stopp-Knopf wie am Handy: solange Jarvis in diesem Gespräch an einer Frage aus der Blase arbeitet oder das Sprachgespräch antwortet oder spricht.</summary>
    public bool Abbrechbar(Gespraech g)
    {
        var s = SprachSchleife.Shared;
        if (ReferenceEquals(s.Gespraech, g) && (s.Zustand == SprachZustand.Denkt || s.Zustand == SprachZustand.Spricht)) return true;
        return g.Arbeitet && (fragen.Values.Any(f => f.Gespraech == g.Id) || AgentenKanal.Shared.LaeuftIn(g));
    }

    public void Abbrechen(Gespraech g)
    {
        GespraechBeendet(g);
        Log("gespraech", "Antwort abgebrochen", wo: "Eingabeblase");
    }

    /// <summary>Laufende Fragen und das Sprachgespräch dieses Gesprächs enden, etwa beim Löschen.</summary>
    public void GespraechBeendet(Gespraech g)
    {
        var betroffen = fragen.Values.Where(f => f.Gespraech == g.Id).ToList();
        foreach (var f in betroffen) f.Abbruch.Cancel();
        AgentenKanal.Shared.LaufAbbrechen(g);
        var sprache = ReferenceEquals(SprachSchleife.Shared.Gespraech, g) && SprachSchleife.Shared.Zustand != SprachZustand.Aus;
        // Die Vormerkung gehört zum gestoppten Auftrag: Ein unbeteiligter Lauf, der endet oder gelöscht wird, nimmt sie nicht mit.
        if (betroffen.Count > 0 || sprache) WinDialoge.VormerkungLoeschen();
        if (sprache) SprachSchleife.Shared.Beenden();
    }

    public void Sprechen() => KuerzelSprechen();

    public void Live()
    {
        if (LiveGespraech.Shared.Aktiv) { LiveGespraech.Shared.Beenden(); return; }
        var g = Gespraech ?? GespraechsSpeicher.Shared.Neu();
        Gespraech = g;
        Bereich = Bereich.Live;
        LiveGespraech.Shared.Starten(g);
    }

    /// <summary>Knopf „Live“ in der Blase: öffnet nur den Live-Bildschirm, gestartet wird dort. Vom Start aus ein neues Gespräch, aus einem Gespräch dieses.</summary>
    public void LiveOeffnen(Gespraech? g)
    {
        if (!LiveGespraech.Shared.Aktiv) Gespraech = g;
        Bereich = Bereich.Live;
    }

    public void FensterZeigen() => Haupt.Post(() =>
    {
        var w = HauptFenster.Anlegen();
        if (!w.IsVisible) w.Show();
        if (w.WindowState == WindowState.Minimized) w.WindowState = w.VorherGross ? WindowState.Maximized : WindowState.Normal;
        var griff = new WindowInteropHelper(w).Handle;
        // Der Rückgabewert von Activate sagt nichts: Zählt nur, ob das Fenster wirklich offen und vorn ist.
        if (Nativ.IsIconic(griff)) Nativ.ShowWindow(griff, Nativ.SW_RESTORE);
        w.Activate();
        Nativ.SetForegroundWindow(griff);
        if (Nativ.GetForegroundWindow() != griff)
        {
            w.Topmost = true;
            w.Topmost = false;
            w.Activate();
        }
        FensterZustandGeaendert();
    });

    /// <summary>Ist Jarvis wirklich vorn? Dann ist die Blase unten die Anzeige, sonst die Pille.</summary>
    public bool FensterVorn => Haupt.Wert(() =>
    {
        var w = HauptFenster.Aktuell;
        return w != null && w.IsVisible && w.IsActive && w.WindowState != WindowState.Minimized;
    });

    /// <summary>Bildschirm gesperrt oder im Ruhezustand.</summary>
    public static bool BildschirmAus => JarvisApp.BildschirmAus;

    /// <summary>„Hey Jarvis“: wie am Handy. Ist Jarvis vorn, geht das offene Gespräch weiter, sonst beginnt ein neues im Hintergrund mit der Pille.</summary>
    public void WeckwortErkannt()
    {
        if (LiveGespraech.Shared.Aktiv || SprachSchleife.Shared.Zustand != SprachZustand.Aus) return;
        Log("sprache", "Weckwort erkannt");
        SprachgespraechStarten();
    }

    private void SprachgespraechStarten()
    {
        _ = McpKlient.Shared.KontextVorladen();
        if (!FensterVorn)
        {
            // Im Hintergrund bleibt die Ansicht, wie sie ist: Ein Fehlalarm darf nichts Offenes verdrängen. Klick auf die Pille oder die Rückkehr zu Jarvis zeigt das Gespräch.
            SprachSchleife.Shared.Starten(GespraechsSpeicher.Shared.Neu());
            return;
        }
        var g = Bereich == Bereich.Gespraech ? (Gespraech ?? GespraechsSpeicher.Shared.Neu()) : EntwurfOderNeu();
        Gespraech = g;
        Bereich = Bereich.Gespraech;
        SprachSchleife.Shared.Starten(g, inApp: true);
    }

    // MARK: Sprachpille

    /// <summary>Sichtbar, solange Live läuft, und im Gesprächsmodus, wenn Jarvis nicht vorn ist.</summary>
    public void PilleAbgleichen() => Haupt.Post(() => pille.Setze(LiveGespraech.Shared.Aktiv || (SprachSchleife.Shared.Zustand != SprachZustand.Aus && !FensterVorn)));

    /// <summary>Damit die Pille nicht auf dem Bildschirmfoto landet, das sie gerade angestoßen hat.</summary>
    public Task PilleFuerAufnahme(bool ausblenden) => Haupt.Auf(() => pille.FuerAufnahme(ausblenden));

    /// <summary>Klick auf die Pille: öffnet Jarvis mit dem laufenden Gespräch.</summary>
    public void PilleOeffnen()
    {
        if (LiveGespraech.Shared.Aktiv) Bereich = Bereich.Live;
        else if (SprachSchleife.Shared.Gespraech is { } g) { Gespraech = g; Bereich = Bereich.Gespraech; }
        FensterZeigen();
    }

    /// <summary>Vom Hauptfenster bei Aktivieren, Verlassen, Minimieren, Schließen. Einen Schritt später: Beim Schließen zählt das Fenster in diesem Moment noch als sichtbar.</summary>
    public void FensterZustandGeaendert() => Ui.Spaeter(() =>
    {
        var aktiv = HauptFenster.Aktuell is { IsActive: true, IsVisible: true };
        // NSApp.isActive: auch Menüleiste und eigene Blätter zählen. Die Pille nimmt nie den Fokus.
        AppVorn = Application.Current.Windows.OfType<Window>().Any(w => w.IsActive && w.IsVisible);
        PilleAbgleichen();
        // Zurück in Jarvis, während das Gespräch im Hintergrund schon Inhalt hat: Es erscheint.
        var s = SprachSchleife.Shared;
        if (aktiv && s.Gespraech is { } g && s.Zustand != SprachZustand.Aus && g.Nachrichten.Count > 0 && !ReferenceEquals(Gespraech, g))
        {
            Gespraech = g;
            Bereich = Bereich.Gespraech;
        }
    });

    // MARK: Globales Tastenkürzel Strg+Alt+J = sprechen, Strg+Alt+K = Fenster

    public void KuerzelRegistrieren()
    {
        if (kuerzelAn) return;
        kuerzelAn = true;
        var griff = JarvisApp.Griff;
        foreach (var (nr, taste) in new[] { (1, 'J'), (2, 'K') })
            if (!Nativ.RegisterHotKey(griff, nr, Nativ.MOD_CONTROL | Nativ.MOD_ALT | Nativ.MOD_NOREPEAT, taste))
                Log("fehler", $"Globales Kürzel Strg+Alt+{taste} ist schon vergeben", wo: "AppSteuerung");
    }

    public void KuerzelAbmelden()
    {
        kuerzelAn = false;
        var griff = JarvisApp.Griff;
        Nativ.UnregisterHotKey(griff, 1);
        Nativ.UnregisterHotKey(griff, 2);
    }

    public void KuerzelSprechen()
    {
        if (SprachSchleife.Shared.Zustand != SprachZustand.Aus) { SprachSchleife.Shared.Beenden(); return; }
        if (LiveGespraech.Shared.Aktiv) return;
        SprachgespraechStarten();
    }
}
