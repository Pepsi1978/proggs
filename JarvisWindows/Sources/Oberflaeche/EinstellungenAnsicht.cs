using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using static JarvisWindows.Bau;

namespace JarvisWindows;

public sealed class EinstellungenAnsicht : UserControl
{
    /// <summary>Der zuletzt offene Reiter bleibt beim Wechsel zu Chat oder Zentrale erhalten.</summary>
    public static string StartReiter { get; set; } = "gehirn";

    private readonly Einstellungen einst = Einstellungen.Shared;
    private string reiter = StartReiter;
    private readonly StackPanel leiste = new();
    private readonly ContentControl rechts = new() { HorizontalContentAlignment = HorizontalAlignment.Stretch, VerticalContentAlignment = VerticalAlignment.Stretch };

    private static readonly (string Id, string Titel, string Symbol)[] ReiterListe =
    {
        ("gehirn", "Jarvis' Kopf", "brain"), ("guthaben", "Guthaben", "creditcard.fill"), ("kontingente", "Kontingente", "gauge.with.dots.needle.67percent"), ("stimme", "Stimme", "speaker.wave.3.fill"), ("sprache", "Sprache & Weckwort", "mic.fill"),
        ("live", "Live-Gespräch", "waveform"), ("windows", "Windows & Rechte", "macbook"), ("tools", "Eigene Tools", "wrench.and.screwdriver.fill"), ("konten", "Konten und APIs", "key.fill"),
        ("gedaechtnis", "Gedächtnis", "brain.head.profile.fill"), ("aktivitaet", "Aktivität", "list.bullet.rectangle.portrait.fill"),
        ("ueber", "Über", "info.circle.fill"),
    };

    public EinstellungenAnsicht()
    {
        leiste.Width = 230;
        leiste.Margin = new Thickness(18, 40, 18, 0);
        var g = new Grid();
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        // Star: Auch ein zu breites Kind darf die Spalte nie aufweiten, sonst rutscht die Seitenleiste nach links.
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star), MinWidth = 0 });
        g.ClipToBounds = true;
        g.Children.Add(leiste);
        Grid.SetColumn(rechts, 1);
        g.Children.Add(rechts);
        Content = g;
        Loaded += (_, _) => { reiter = StartReiter; Zeichne(); };
    }

    private void Waehle(string r)
    {
        if (reiter == r) return;
        reiter = r;
        StartReiter = r;
        Zeichne();
    }

    private void Zeichne()
    {
        leiste.Children.Clear();
        var titel = T("Einstellungen", 24, FontWeights.Bold);
        titel.Margin = new Thickness(8, 0, 0, 14);
        leiste.Children.Add(titel);
        foreach (var r in ReiterListe)
            VAnfuegen(leiste, new LeistenEintrag(r.Symbol, r.Titel, reiter == r.Id, () => Waehle(r.Id)), leiste.Children.Count > 1 ? 4 : 0);
        rechts.Content = Seite();
    }

    private UIElement Seite()
    {
        // Diese beiden bringen Kopf und Scrollen selbst mit.
        if (reiter == "gedaechtnis") return new GedaechtnisAnsicht();
        if (reiter == "aktivitaet") return new AktivitaetAnsicht();
        var inhalt = reiter switch
        {
            "gehirn" => Gehirn(),
            "guthaben" => new GuthabenAnsicht(),
            "kontingente" => new KontingenteAnsicht(),
            "stimme" => Stimme_(),
            "sprache" => V(18, Sprache(), new TranskriptionsEinstellungenAnsicht()),
            "live" => new LiveKarte(),
            "windows" => new WinRechteAnsicht(),
            "tools" => new EigeneToolsAnsicht(),
            "konten" => new KontenAnsicht(),
            _ => Ueber(),
        };
        var rand = new ContentControl { Content = inhalt, HorizontalContentAlignment = HorizontalAlignment.Stretch, Margin = new Thickness(30, 44, 30, 30), MaxWidth = 780, HorizontalAlignment = HorizontalAlignment.Left };
        return new ScrollViewer { Content = rand, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };
    }

    /// <summary>Beschriftung links, Bedienelement rechts.</summary>
    internal static UIElement Zeile(string titel, string beschreibung, UIElement inhalt)
    {
        var text = V(3, T(titel, 14, FontWeights.SemiBold), beschreibung.Length > 0 ? T(beschreibung, 12, null, Farbe.Text2) : null);
        return Rand(H(16, Fuell(Rand(text, r: 20)), inhalt), o: 6, u: 6);
    }

    private UIElement Gehirn() => Karte(V(8,
        Titel("brain", "Jarvis' Kopf"),
        Zeile("Modell", "Claude über deinen Anthropic-Schlüssel oder GPT über dein ChatGPT-Konto.",
            Auswahl(ModellWahl.Alle.Select(m => (m.Id, m.Name)), () => einst.Modell, v => einst.Modell = v, 220)),
        Rand(V(8,
            V(3, T("Denkstufe", 14, FontWeights.SemiBold), T("Höher = gründlicher, aber langsamer und teurer.", 12, null, Farbe.Text2)),
            new StufenWahl(new[] { "low", "medium", "high", "xhigh", "max" }, () => einst.Effort, v => einst.Effort = v)), o: 6, u: 6),
        Zeile("Schrittlimit je Anfrage", "Wie viele Werkzeugschritte Jarvis höchstens am Stück macht (0 = unbegrenzt).",
            Stepper(() => einst.SchrittLimit, v => einst.SchrittLimit = v, 0, 200, 10)),
        Zeile("Lernen am PC", "Ruht ein Gespräch 10 Minuten, gibt Jarvis neue dauerhafte Angaben von dir ins gemeinsame Gedächtnis.",
            new Schalter(() => einst.LernenAn, v => einst.LernenAn = v))));

    private UIElement Stimme_() => V(18,
        Karte(V(8,
            Titel("speaker.wave.3.fill", "Vorlesen"),
            Zeile("Antworten vorlesen", "Im Sprachgespräch liest Jarvis seine Antworten vor.", new Schalter(() => einst.Vorlesen, v => einst.Vorlesen = v)))),
        new VorleseEinstellungenAnsicht());

    private UIElement Sprache() => new SpracheKarte();

    private UIElement Ueber()
    {
        var s = AppSteuerung.Shared;
        return Karte(V(10,
            H(16,
                new JarvisKugel(groesse: 70),
                Fuell(V(4,
                    T(AppInfo.Name, 20, FontWeights.Bold),
                    T($"Version {AppInfo.Version} · Build {AppInfo.Stand} Uhr", 13, null, Farbe.Text2),
                    T($"Handy-Jarvis: {(s.HandyVerbunden ? $"verbunden ({s.HandyVersion})" : "nicht erreichbar")}", 13, null, Farbe.Text2)))),
            T("Dasselbe Jarvis wie auf dem Handy – gleiches Live-Gedächtnis, gleiche Apps über das Relay – plus volle Steuerung des PCs.", 13, null, Farbe.Text2)));
    }

    /// <summary>Öffnet eine Seite der Windows-Einstellungen, z. B. „privacy-microphone“.</summary>
    public static void Oeffne(string bereich) => Bau.Oeffne("ms-settings:" + bereich);
}

/// <summary>Karte „Sprache &amp; Weckwort“; nur die Statuszeile folgt dem Weckwort, die Felder behalten den Fokus.</summary>
internal sealed class SpracheKarte : UserControl
{
    public SpracheKarte()
    {
        var einst = Einstellungen.Shared;
        var titelZeile = T($"„{einst.Weckwort}“", 14, FontWeights.SemiBold);
        var erste = V(3, titelZeile, T("Jarvis lauscht am PC auf das Weckwort, auch wenn das Fenster zu ist.", 12, null, Farbe.Text2));
        var weckwort = new TextBindung(() => einst.Weckwort, v => { einst.Weckwort = v; titelZeile.Text = $"„{v}“"; });
        var wortFeld = Feld(weckwort, "", 13, breite: 200, fertig: b =>
        {
            void Anwenden() { Weckwort.Shared.Stoppen(); if (einst.WeckwortAn) Weckwort.Shared.Starten(); }
            b.KeyDown += (_, e) => { if (e.Key == Key.Enter) Anwenden(); };
        });
        var punkt = Kreis(Farbe.Text3);
        var lauscht = T("", 12, null, Farbe.Text2);
        void Status()
        {
            var an = Weckwort.Shared.Lauscht;
            punkt.Fill = B(an ? Farbe.Gruen : Farbe.Text3);
            lauscht.Text = an ? "Weckwort lauscht gerade" : "Weckwort lauscht nicht";
        }
        Status();
        System.ComponentModel.PropertyChangedEventHandler h = (_, e) => { if (e.PropertyName == nameof(Weckwort.Lauscht)) Status(); };
        Loaded += (_, _) => { Weckwort.Shared.PropertyChanged -= h; Weckwort.Shared.PropertyChanged += h; Status(); };
        Unloaded += (_, _) => Weckwort.Shared.PropertyChanged -= h;

        Content = Karte(V(8,
            Titel("mic.fill", "Sprache & Weckwort"),
            Rand(H(16, Fuell(Rand(erste, r: 20)), new Schalter(() => einst.WeckwortAn, an =>
            {
                einst.WeckwortAn = an;
                if (an) Weckwort.Shared.Starten(); else Weckwort.Shared.Stoppen();
            })), o: 6, u: 6),
            EinstellungenAnsicht.Zeile("Weckwort", "z. B. „Hey Jarvis“ oder „Okay Computer“.", wortFeld),
            EinstellungenAnsicht.Zeile("Stillezeit in der App", "Sekunden Stille nach deinem letzten Wort, bis das Mikrofon schließt, wenn das Gespräch bei Jarvis vorn begann und er noch vorn ist: 1 bis 15, auch mit Komma. Gilt auch für Strg+Alt+J, nicht im Live-Modus.",
                new StillezeitFeld(() => einst.StilleInAppSek, v => einst.StilleInAppSek = v)),
            EinstellungenAnsicht.Zeile("Stillezeit außerhalb der App", "Dasselbe in allen anderen Fällen: Das Gespräch begann im Hintergrund (Fenster zu oder andere App vorn) oder Jarvis ist nicht mehr vorn.",
                new StillezeitFeld(() => einst.StilleAussenSek, v => einst.StilleAussenSek = v)),
            EinstellungenAnsicht.Zeile("Tastenkürzel", "Strg+Alt+J überall: sprechen · Strg+Alt+K: Jarvis öffnen · im Fenster Strg+D sprechen, Strg+L Live, Strg+N neues Gespräch.",
                new Schalter(() => einst.GlobalesKuerzel, an =>
                {
                    einst.GlobalesKuerzel = an;
                    if (an) AppSteuerung.Shared.KuerzelRegistrieren(); else AppSteuerung.Shared.KuerzelAbmelden();
                })),
            H(8, punkt, lauscht, Spacer())));
    }
}

/// <summary>Karte „Live-Gespräch“: Anbieter und Modell schalten die Listen darunter um.</summary>
internal sealed class LiveKarte : LebendeAnsicht
{
    private readonly Einstellungen einst = Einstellungen.Shared;

    public LiveKarte() { Beobachte(einst, nameof(Einstellungen.LiveAnbieter), nameof(Einstellungen.LiveModellOpenAI)); }

    protected override UIElement Baue()
    {
        var stilleEnde = new List<(int, string)>();
        foreach (var (minuten, name) in LiveOptionen.StilleEnde) stilleEnde.Add((minuten, name));
        var s = V(8,
            Titel("waveform", "Live-Gespräch"),
            EinstellungenAnsicht.Zeile("Anbieter", "Gleichzeitig zuhören und sprechen, mit deinen eigenen API-Schlüsseln.",
                Segment(new[] { ("openai", "OpenAI"), ("gemini", "Gemini") }, () => einst.LiveAnbieter, v => einst.LiveAnbieter = v)),
            EinstellungenAnsicht.Zeile("Bei Stille beenden", "Beendet das Live-Gespräch, wenn so lange niemand spricht. GPT-Live kostet auch bei Stille weiter.",
                Auswahl(stilleEnde, () => einst.LiveStilleMinuten, v => einst.LiveStilleMinuten = v, 260)));
        if (einst.LiveAnbieter == "gemini")
        {
            VAnfuegen(s, EinstellungenAnsicht.Zeile("Gemini-Modell", "", Auswahl(LiveOptionen.GeminiModelle.Select(m => (m.Id, m.Name)), () => einst.LiveModellGemini, v => einst.LiveModellGemini = v, 260)), 8);
            VAnfuegen(s, EinstellungenAnsicht.Zeile("Gemini-Stimme", "", Auswahl(LiveOptionen.GeminiStimmen.Select(m => (m.Id, $"{m.Id} – {m.Name}")), () => einst.LiveStimmeGemini, v => einst.LiveStimmeGemini = v, 260)), 8);
        }
        else
        {
            VAnfuegen(s, EinstellungenAnsicht.Zeile("OpenAI-Modell", "", Auswahl(LiveOptionen.OpenAIModelle.Select(m => (m.Id, m.Name)), () => einst.LiveModellOpenAI, v => einst.LiveModellOpenAI = v, 260)), 8);
            var stimmen = einst.LiveModellOpenAI == "gpt-live-1" ? LiveOptionen.LiveStimmen : LiveOptionen.RealtimeStimmen;
            VAnfuegen(s, EinstellungenAnsicht.Zeile("OpenAI-Stimme", "", Auswahl(stimmen.Select(m => (m.Id, m.Name)), () => einst.LiveStimmeOpenAI, v => einst.LiveStimmeOpenAI = v, 260)), 8);
        }
        return Karte(s);
    }
}

/// <summary>Gleich breite Stufen, die immer in ihre Karte passen.</summary>
public sealed class StufenWahl : UserControl
{
    private readonly List<(string Wert, string Name)> stufen;
    private readonly Func<string> lies;
    private readonly Action<string> setze;

    public StufenWahl(IEnumerable<string> stufen, Func<string> lies, Action<string> setze) : this(stufen.Select(x => (x, x)), lies, setze) { }
    public StufenWahl(IEnumerable<(string Wert, string Name)> stufen, Func<string> lies, Action<string> setze)
    {
        this.stufen = stufen.ToList();
        this.lies = lies;
        this.setze = setze;
        Zeichne();
    }

    private void Zeichne()
    {
        var g = new Grid();
        var i = 0;
        foreach (var (wert, name) in stufen)
        {
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            var aktiv = lies() == wert;
            var t = T(name, 12, aktiv ? FontWeights.Bold : FontWeights.Medium, aktiv ? System.Windows.Media.Brushes.Black : Farbe.Text, 1);
            t.TextAlignment = TextAlignment.Center;
            var f = Flaeche(t, aktiv ? Farbe.Akzent : Farbe.Glas(0.07), 9, 2, 7);
            var k = Klick(f, () => { setze(wert); Zeichne(); });
            k.Margin = new Thickness(i == 0 ? 0 : 2, 0, i == stufen.Count - 1 ? 0 : 2, 0);
            Grid.SetColumn(k, i++);
            g.Children.Add(k);
        }
        Content = g;
    }
}

public sealed class KontenAnsicht : UserControl
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly TextBlock relayTest = T("", 12, null, Farbe.Text2);
    private readonly TextBlock codexCode = new() { FontFamily = Mono, FontSize = 28, FontWeight = FontWeights.Bold, Foreground = B(Farbe.Haupt), Visibility = Visibility.Collapsed };
    private readonly TextBlock codexStatus = T("", 12, null, Farbe.Text2);

    public KontenAnsicht()
    {
        codexStatus.Visibility = Visibility.Collapsed;
        var relay = new TextBindung(() => einst.Zugang.Relay, v => { einst.Zugang.Relay = v; einst.ZugangGeaendert(); });
        Content = V(18,
            Karte(V(10,
                Titel("iphone.radiowaves.left.and.right", "Verbindung zum Handy-Jarvis", Farbe.Gold),
                T("Über diese Relay-Adresse teilt der PC Gedächtnis, Apps und Ablage mit deinem Handy (dieselbe wie im ChatGPT-Plugin).", 12, null, Farbe.Text2),
                new TresorFeld("Relay-Adresse", relay),
                H(8,
                    Kapsel("antenna.radiowaves.left.and.right", "Verbindung testen", async () =>
                    {
                        var n = (await McpKlient.Shared.Werkzeuge(frisch: true)).Count;
                        relayTest.Text = n > 0 ? $"Verbunden – {n} Werkzeuge vom Handy." : "Keine Verbindung (Handy aus oder Adresse falsch).";
                        AppSteuerung.Shared.HandyVerbunden = n > 0;
                    }, hervorgehoben: true),
                    Fuell(relayTest)))),
            Karte(V(10,
                Titel("person.badge.key.fill", "ChatGPT-Konto (für GPT-Modelle)", Farbe.Gruen),
                T("Eigene Anmeldung nur für diesen PC – das Handy bleibt angemeldet.", 12, null, Farbe.Text2),
                H(10,
                    Kapsel("link", "Mit ChatGPT anmelden", Anmelden, hervorgehoben: true),
                    Kapsel("rectangle.portrait.and.arrow.right", "Abmelden", async () =>
                    {
                        await CodexAnmeldung.Shared.Abmelden();
                        Status("Abgemeldet.");
                    }),
                    Spacer()),
                codexCode,
                codexStatus)),
            Karte(V(10,
                Titel("key.fill", "Konten und APIs", Farbe.Warnung),
                T("Verschlüsselt im Tresor, gebunden an deinen Windows-Benutzer auf diesem PC. Anzeigen und Ändern nur nach Windows Hello. Das schützt den Speicher, nicht vor Missbrauch durch ein bereits laufendes Programm unter deinem Benutzer.", 12, null, Farbe.Text2),
                new TresorKopf(),
                new KontenListe())));
        Loaded += async (_, _) =>
        {
            if (await CodexAnmeldung.Shared.Angemeldet())
            {
                var mail = await CodexAnmeldung.Shared.Email();
                Status("Angemeldet" + (mail != null ? $" als {mail}" : "") + ".");
            }
        };
    }

    private void Status(string t)
    {
        codexStatus.Text = t;
        codexStatus.Visibility = t.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private void Code(string t)
    {
        codexCode.Text = t;
        codexCode.Visibility = t.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private async void Anmelden()
    {
        try
        {
            var (code, url) = await CodexAnmeldung.Shared.Start();
            Code(code);
            Kopiere(code);
            Status("Code kopiert – im Browser einfügen und freigeben.");
            Oeffne(url);
            var email = await CodexAnmeldung.Shared.WarteAufFreigabe();
            Code("");
            Status($"Angemeldet als {email ?? "ChatGPT-Konto"}.");
        }
        catch (Exception ex)
        {
            Status($"Anmeldung fehlgeschlagen: {ex.Message}");
        }
    }
}

/// <summary>Eine Stillezeit in Sekunden; übernimmt mit Eingabetaste oder beim Verlassen, Ungültiges zeigt kurz einen Hinweis und fällt auf den alten Wert zurück.</summary>
internal sealed class StillezeitFeld : UserControl
{
    private readonly Func<double> lies;
    private readonly Action<double> setze;
    private readonly TextBox feld;
    private readonly TextBlock ungueltig = T("1 bis 15", 12, null, System.Windows.Media.Brushes.Red);

    public StillezeitFeld(Func<double> lies, Action<double> setze)
    {
        this.lies = lies;
        this.setze = setze;
        feld = Eingabe(Einstellungen.StilleZahl(lies()));
        feld.Width = 60;
        ungueltig.Visibility = Visibility.Collapsed;
        feld.KeyDown += (_, e) => { if (e.Key == Key.Enter) Uebernehmen(); };
        feld.LostKeyboardFocus += (_, _) => Uebernehmen();
        // Ein Klick ins Leere nimmt dem Feld nicht sicher den Fokus: Auch beim Verlassen der Seite übernehmen.
        Unloaded += (_, _) => Uebernehmen();
        Content = H(6, ungueltig, feld, T("Sekunden", 12, null, Farbe.Text3));
    }

    private void Uebernehmen()
    {
        var s = Einstellungen.StilleAus(feld.Text);
        ungueltig.Visibility = s == null ? Visibility.Visible : Visibility.Collapsed;
        if (s is { } x) setze(x);
        feld.Text = Einstellungen.StilleZahl(lies());
    }
}
