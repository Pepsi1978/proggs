using System.Collections.Specialized;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using static JarvisWindows.Ui;

namespace JarvisWindows;

// MARK: - Live

public sealed class LiveAnsicht : Decorator
{
    private readonly LiveGespraech live = LiveGespraech.Shared;
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly AppSteuerung steuerung = AppSteuerung.Shared;

    /// <summary>Kugel des Live-Gesprächs: liest Zustand und Pegel je Bild, nicht über den Neubau der Seite.</summary>
    private sealed class LiveKugel : JarvisKugel
    {
        public LiveKugel(double groesse) : base(0, KugelZustand.Ruhe, groesse) { }
        protected override void Aktualisieren()
        {
            var l = LiveGespraech.Shared;
            Zustand = l.Zustand switch { LiveZustand.Hoert => KugelZustand.Hoert, LiveZustand.Denkt => KugelZustand.Denkt, LiveZustand.Spricht => KugelZustand.Spricht, _ => KugelZustand.Ruhe };
            Pegel = l.Zustand == LiveZustand.Spricht ? l.PegelAus : l.PegelEin;
        }
    }

    private string StatusText => live.Zustand switch
    {
        LiveZustand.Aus => "Live-Gespräch starten",
        LiveZustand.Verbindet => "Verbinde …",
        LiveZustand.Hoert => "Ich höre dir zu",
        LiveZustand.Denkt => "Jarvis arbeitet daran …",
        _ => "Jarvis spricht",
    };

    public LiveAnsicht()
    {
        var kugel = new LiveKugel(260).Mittig().Klick(steuerung.Live);
        kugel.Margin = new Thickness(0, 30, 0, 30);
        var texte = new Reaktiv(() =>
        {
            var a = Tx(StatusText, 26, FontWeights.Bold);
            var b = Tx(live.Aktiv ? "Sprich einfach – du kannst mich jederzeit unterbrechen." : $"Gleichzeitig zuhören und sprechen über {(einst.LiveAnbieter == "gemini" ? "Gemini Live" : "OpenAI GPT-Live")}.", 14, null, Farbe.Text2);
            a.TextAlignment = b.TextAlignment = TextAlignment.Center;
            TextBlock? f = null;
            if (live.Fehler is { } t) { f = Tx(t, 13, FontWeights.Medium, Farbe.Rot); f.TextAlignment = TextAlignment.Center; }
            return V(8, a, b, f);
        }, new[] { nameof(LiveGespraech.Zustand), nameof(LiveGespraech.Fehler), nameof(Einstellungen.LiveAnbieter) }, live, einst);
        var knoepfe = new Reaktiv(() =>
        {
            if (live.Aktiv) return new KapselKnopf("stop.fill", "Gespräch beenden", live.Beenden, hervorgehoben: true).Mittig();
            return H(12, new KapselKnopf("waveform", "Live starten", steuerung.Live, hervorgehoben: true),
                new SegmentWahl(new[] { ("openai", "OpenAI GPT-Live"), ("gemini", "Gemini Live") }, einst.LiveAnbieter, w => einst.LiveAnbieter = w).Breite(260)).Mittig();
        }, new[] { nameof(LiveGespraech.Zustand) }, live);

        var mitschrift = new StackPanel { Margin = new Thickness(20) };
        var scroller = new Scroller(mitschrift);
        var mitschriftRahmen = new Border { Child = scroller, MaxWidth = 760, MaxHeight = 260, CornerRadius = new CornerRadius(20), Background = Farbe.Glas(0.04), Visibility = Visibility.Collapsed };
        void Zeile(LiveZeile z)
        {
            var n = Tx(z.VonFrank ? "Du" : "Jarvis", 12, FontWeights.Bold, z.VonFrank ? Farbe.Gruen : Farbe.Haupt).Breite(50);
            n.TextAlignment = TextAlignment.Right;
            var text = new WaehlbarerText { FontSize = 14, FontFamily = Ui.Schrift, Foreground = Farbe.Text, TextWrapping = TextWrapping.Wrap, Text = z.Text };
            mitschrift.Children.Add(HOben(10, n, text.Fuell()).Rand(0, mitschrift.Children.Count > 0 ? 10 : 0, 0, 0));
        }
        void MitschriftGeaendert(object? s, NotifyCollectionChangedEventArgs e)
        {
            if (e.Action == NotifyCollectionChangedAction.Add) foreach (LiveZeile z in e.NewItems!) Zeile(z);
            else { mitschrift.Children.Clear(); foreach (var z in live.Mitschrift) Zeile(z); }
            mitschriftRahmen.Visibility = live.Mitschrift.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
            scroller.Sv.ScrollToEnd();
        }
        var v = V(26, Spacer(), kugel, texte, knoepfe, mitschriftRahmen, Spacer());
        Child = new Border { Child = v, Padding = new Thickness(30) };
        Loaded += (_, _) =>
        {
            live.Mitschrift.CollectionChanged += MitschriftGeaendert;
            MitschriftGeaendert(null, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        };
        Unloaded += (_, _) => live.Mitschrift.CollectionChanged -= MitschriftGeaendert;
    }
}

// MARK: - Werkzeug-Text-Bereich (Ablage, Gedächtnis, Aktivität am Handy)

public sealed class BereichsLader : Beobachtbar
{
    private string? _text;
    public string? Text { get => _text; set => Setze(ref _text, value); }
    private bool _laedt;
    public bool Laedt { get => _laedt; set => Setze(ref _laedt, value); }
    /// <summary>Gelingt das Laden nicht, bleibt der letzte gute Stand stehen.</summary>
    public async Task<bool> Laden(string werkzeug, JSONObjekt? args = null)
    {
        Laedt = true;
        try
        {
            var t = await McpKlient.Shared.Text(werkzeug, args ?? new JSONObjekt());
            if (t == null)
            {
                Text ??= "Das Handy-Jarvis ist gerade nicht erreichbar.";
                return false;
            }
            Text = t;
            return true;
        }
        finally { Laedt = false; }
    }
}

public sealed class BereichsKopf : Stapel2
{
    public BereichsKopf(string symbol, string titel, string untertitel)
    {
        Inhalt = H(14, new Border { Width = 46, Height = 46, CornerRadius = new CornerRadius(14), Background = Farbe.Glas(0.06), Child = Sym.Bild(symbol, 20, Farbe.Akzent) },
            V(3, Tx(titel, 26, FontWeights.Bold), Tx(untertitel, 13, null, Farbe.Text2)).Fuell());
    }
}

/// <summary>Eine einklappbare Karte: klein, solange sie zu ist; lädt erst beim ersten Aufklappen.</summary>
public sealed class KlappKarte : Stapel2
{
    private readonly Func<UIElement?> inhalt;
    private readonly Action<bool>? geaendert;
    private readonly TextBlock pfeil, unter;
    private readonly RotateTransform pfeilDreh = new();
    private readonly Decorator platz = new();
    private bool offen;

    public KlappKarte(string symbol, string titel, object farbe, Func<UIElement?> inhalt, string untertitel = "", bool offen = false, Action<bool>? offenGeaendert = null)
    {
        this.inhalt = inhalt;
        geaendert = offenGeaendert;
        pfeil = Sym.Bild("chevron.down", 12, Farbe.Text3);
        pfeil.RenderTransformOrigin = new Point(0.5, 0.5);
        pfeil.RenderTransform = pfeilDreh;
        unter = Tx(untertitel, 12, null, Farbe.Text3, zeilen: 1);
        unter.Visibility = untertitel.Length > 0 ? Visibility.Visible : Visibility.Collapsed;
        platz.Visibility = Visibility.Collapsed;
        var kopf = H(10, new KartenTitel(symbol, titel, farbe), unter, Spacer(), pfeil).Klick(() => Offen = !Offen);
        Inhalt = new GlasKarte(V(12, kopf, platz), abstand: 14);
        Offen = offen;
    }

    public string Untertitel { set { unter.Text = value; unter.Visibility = value.Length > 0 ? Visibility.Visible : Visibility.Collapsed; } }

    public bool Offen
    {
        get => offen;
        set
        {
            if (offen == value && (platz.Child != null) == value) return;
            offen = value;
            pfeilDreh.Angle = value ? 180 : 0;
            platz.Child = value ? inhalt() : null;
            platz.Visibility = value ? Visibility.Visible : Visibility.Collapsed;
            geaendert?.Invoke(value);
        }
    }
}

public sealed class GedaechtnisAnsicht : Decorator
{
    private readonly BereichsLader gemerkt = new(), live = new(), handyFunktionen = new();
    private string art = "info", antwort = "";
    private bool gemerktOffen, liveOffen, liveVoll, liveWeg;
    private DateTime? liveStand;
    private string regeln = "";
    private readonly TextBlock antwortText = Tx("", 12, null, Farbe.Text2);
    private readonly HilfsMelder melder = new();
    private CancellationTokenSource? liveUhr;
    private sealed class HilfsMelder : Beobachtbar { public void Sag() => Melde("Stand"); }

    private string LiveUntertitel => liveWeg
        ? "Handy nicht erreichbar, letzter Stand" + (liveStand is { } s ? " " + s.ToString("HH:mm") : "")
        : liveStand is { } t ? $"vom Handy, Stand {t:HH:mm}" : "";

    public GedaechtnisAnsicht()
    {
        TextFeld merkFeld = null!;
        merkFeld = new TextFeld("z. B. „Ich arbeite am PC meistens mit Opus 5.5.“", 13);
        var merkKarte = new GlasKarte(V(10,
            new KartenTitel("plus.circle.fill", "Etwas merken", Farbe.Gruen),
            H(8,
                Bau.Auswahl(new (string Wert, string Name)[] { ("info", "Über Frank"), ("regel", "Regel"), ("ziel", "Ziel"), ("entscheidung", "Entscheidung"), ("stil", "Stil") }, () => art, w => art = w, 150),
                Box(merkFeld, Farbe.Glas(0.05), 10, 10, 10).Fuell(),
                new KapselKnopf("checkmark", "Merken", () =>
                {
                    var t = merkFeld.Text;
                    var a = art;
                    Starte(async () =>
                    {
                        var r = await McpKlient.Shared.Rufe("jarvis_merken", new JSONObjekt { ["art"] = a, ["text"] = t });
                        antwort = r.Text;
                        antwortText.Text = antwort;
                        antwortText.Visibility = Visibility.Visible;
                        merkFeld.Text = "";
                        if (gemerktOffen) await gemerkt.Laden("jarvis_gemerktes_lesen");
                    });
                }, hervorgehoben: true)),
            antwortText.Sichtbar(false)));

        var liveKarte = new KlappKarte("bolt.heart.fill", "Live-Gedächtnis", Farbe.Haupt, () => LaderInhalt(live, "Alles neu laden", () => { liveVoll = true; Starte(LiveLaden); }),
            offenGeaendert: an => { liveOffen = an; LiveTakt(); });
        void LiveTitel() => liveKarte.Untertitel = LiveUntertitel;
        melder.PropertyChanged += (_, _) => LiveTitel();

        var handyKarte = new KlappKarte("iphone", "Funktionsübersicht am Handy", Farbe.Gold,
            () => new Reaktiv(() => new MarkdownText(McpKlient.OhneKontextHinweis(handyFunktionen.Text ?? ""), 13), handyFunktionen), untertitel: "Was die Handy-App kann",
            offenGeaendert: an =>
            {
                if (!an) return;
                // Ohne Handy bleibt der Stand aus dem letzten Build.
                Starte(async () => { if (!await handyFunktionen.Laden("funktionen_lesen")) handyFunktionen.Text = Pfade.Mitgeliefert("handy-funktionen"); });
            });
        handyFunktionen.PropertyChanged += (_, e) => { if (e.PropertyName == nameof(BereichsLader.Laedt)) handyKarte.Untertitel = handyFunktionen.Laedt ? "lädt …" : "Was die Handy-App kann"; };
        var v = V(16,
            new BereichsKopf("brain.head.profile.fill", "Gedächtnis", "Ein Gedächtnis für alle Geräte: Was du hier sagst oder merkst, weiß Jarvis auch auf dem Handy."),
            merkKarte,
            new KlappKarte("doc.plaintext.fill", "Langzeitkontext", Farbe.Warnung, () => new LangzeitkontextEditor(), untertitel: "Deine Textdatei für Jarvis, auf PC und Handy gleich"),
            new KlappKarte("bookmark.fill", "Gemerktes", Farbe.Gold, () => LaderInhalt(gemerkt, "Neu laden", () => Starte(() => gemerkt.Laden("jarvis_gemerktes_lesen"))),
                offenGeaendert: an => { gemerktOffen = an; if (an) Starte(() => gemerkt.Laden("jarvis_gemerktes_lesen")); }),
            liveKarte,
            new KlappKarte("macbook", "Funktionsübersicht am PC", Farbe.Haupt, () => new MarkdownText(Pfade.Mitgeliefert("funktionen"), 13), untertitel: "Was Jarvis am Rechner kann, aus dem Repo"),
            handyKarte,
            new KlappKarte("square.grid.2x2.fill", "Handy-Reiter und CLI", Farbe.Kupfer, () => new MarkdownText(Pfade.Mitgeliefert("jarvis-wissen"), 13), untertitel: "Reiter der Handy-App, Zusammenarbeit mit dem CLI"),
            RegelnKarte());
        v.Margin = new Thickness(36);
        Child = new Scroller(v);
        Unloaded += (_, _) => { liveUhr?.Cancel(); liveUhr = null; };
    }

    private UIElement RegelnKarte()
    {
        var text = new MarkdownText("", 13);
        return new KlappKarte("waveform.circle.fill", "Regeln für ChatGPT Live", Farbe.Gruen, () => { text.Text = regeln.Length == 0 ? "Das Handy-Jarvis ist gerade nicht erreichbar." : regeln; return text; }, untertitel: "Vorgaben des Handy-Jarvis",
            offenGeaendert: an =>
            {
                if (!an) return;
                Starte(async () =>
                {
                    // Die Anleitung steht nur nach dem Verbinden frisch da; ist das Handy aus, bleibt der letzte Stand.
                    try { await McpKlient.Shared.Verbinden(); } catch { }
                    regeln = SymbioseBlock(await McpKlient.Shared.Anweisungen());
                    text.Text = regeln.Length == 0 ? "Das Handy-Jarvis ist gerade nicht erreichbar." : regeln;
                });
            });
    }

    private static UIElement LaderInhalt(BereichsLader l, string hilfe, Action neu)
    {
        var markdown = new MarkdownText(l.Text ?? "", 13);
        var kopf = new Reaktiv(() => H(8, l.Laedt ? new Spinner(16) : null, Spacer(), new RundKnopf("arrow.clockwise", neu, hilfe: hilfe, groesse: 28)), new[] { nameof(BereichsLader.Laedt) }, l);
        var inhalt = new Reaktiv(() => { markdown.Text = l.Text ?? ""; return null; }, new[] { nameof(BereichsLader.Text) }, l);
        return V(12, kopf, markdown, inhalt);
    }

    /// <summary>Aus der Anleitung des Handys nur Überschrift und Regeln [S…]; dahinter folgen Langzeitkontext und Werkzeughinweise, die gehören nicht in diese Karte.</summary>
    private static string SymbioseBlock(string t)
    {
        var start = t.IndexOf("SYMBIOSE CHATGPT LIVE ⇄ JARVIS · Stand", StringComparison.Ordinal);
        if (start < 0) return "";
        var zeilen = t[start..].Split('\n');
        return string.Join("\n\n", new[] { zeilen[0] }.Concat(zeilen.Skip(1).TakeWhile(z => z.StartsWith("[S", StringComparison.Ordinal))));
    }

    /// <summary>Der PC hat kein eigenes Gedächtnis: Solange die Karte offen ist, kommt jede Minute der aktuelle Stand vom Handy.</summary>
    private void LiveTakt()
    {
        liveUhr?.Cancel();
        liveUhr = null;
        if (!liveOffen) return;
        var q = liveUhr = new CancellationTokenSource();
        Starte(async () =>
        {
            while (!q.IsCancellationRequested)
            {
                await LiveLaden();
                await Task.Delay(TimeSpan.FromSeconds(60), q.Token);
            }
        });
    }

    /// <summary>„leise“: Die Anzeige verbraucht keine Meldungen, die für Gespräche in ChatGPT oder am Handy gedacht sind.</summary>
    private async Task LiveLaden()
    {
        var a = new JSONObjekt { ["leise"] = true };
        if (liveVoll) a["umfang"] = "voll";
        var ok = await live.Laden("jarvis_kontext", a);
        liveWeg = !ok;
        if (ok) liveStand = DateTime.Now;
        melder.Sag();
    }
}

/// <summary>Der Entwurf lebt außerhalb der Ansicht: Zuklappen oder ein Bereichswechsel dürfen ihn nicht verwerfen.</summary>
public sealed class LangzeitEntwurf : Beobachtbar
{
    public static LangzeitEntwurf Shared { get; } = new();
    private string _text = "", _gelesen = "", _meldung = "";
    public string Text { get => _text; set { if (Setze(ref _text, value)) Melde(nameof(Geaendert)); } }
    public string Gelesen { get => _gelesen; set { if (Setze(ref _gelesen, value)) Melde(nameof(Geaendert)); } }
    public long Stand { get; set; } = -1;
    private bool _laedt, _fehlerMeldung, _konflikt;
    public bool Laedt { get => _laedt; set => Setze(ref _laedt, value); }
    public string Meldung { get => _meldung; set => Setze(ref _meldung, value); }
    public bool FehlerMeldung { get => _fehlerMeldung; set => Setze(ref _fehlerMeldung, value); }
    public bool Konflikt { get => _konflikt; set => Setze(ref _konflikt, value); }
    public bool Geaendert => Text != Gelesen;

    public async Task Laden(bool erzwingen = false)
    {
        if (!erzwingen && Geaendert) return;
        Laedt = true;
        try
        {
            var r = await McpKlient.Shared.Rufe("jarvis_langzeitkontext_lesen", new JSONObjekt());
            if (r.Fehler || !r.Text.StartsWith("STAND ", StringComparison.Ordinal))
            {
                Meldung = r.Fehler ? r.Text : "Die Antwort des Handys war nicht lesbar. Läuft dort schon die neueste Jarvis-Version?";
                FehlerMeldung = true;
                return;
            }
            var (s, t) = Zerlege(r.Text);
            Stand = s; Text = t; Gelesen = t; Meldung = ""; Konflikt = false;
        }
        finally { Laedt = false; }
    }

    /// <summary>Aufbau: erste Zeile „STAND &lt;zahl&gt;“, danach der Text. Das Handy kann Hinweiszeilen nur ans Ende hängen, hier hängt es keine an.</summary>
    private static (long, string) Zerlege(string antwort)
    {
        var ende = antwort.IndexOf('\n');
        var kopf = ende < 0 ? antwort : antwort[..ende];
        long.TryParse(kopf.Length >= 6 ? kopf[6..].Trim() : "", out var stand);
        return (stand, ende < 0 ? "" : antwort[(ende + 1)..]);
    }

    /// <summary>„überschreiben“: Frank hat im Konflikt entschieden, dass seine Fassung gilt. Die vorige bleibt am Handy als Vorfassung erhalten.</summary>
    public async Task Speichern(bool ueberschreiben = false)
    {
        var basis = Stand;
        if (ueberschreiben)
        {
            var q = await McpKlient.Shared.Rufe("jarvis_langzeitkontext_lesen", new JSONObjekt());
            if (q.Fehler || !q.Text.StartsWith("STAND ", StringComparison.Ordinal)) { Meldung = q.Text; FehlerMeldung = true; return; }
            basis = Zerlege(q.Text).Item1;
        }
        var t = Text;
        var r = await McpKlient.Shared.Rufe("jarvis_langzeitkontext_ersetzen", new JSONObjekt { ["text"] = t, ["basis_stand"] = basis.ToString() });
        FehlerMeldung = r.Fehler;
        Konflikt = r.Fehler && r.Text.StartsWith("KONFLIKT", StringComparison.Ordinal);
        Meldung = Konflikt ? "Am Handy gibt es eine neuere Fassung. Entscheide, welche gilt." : (r.Fehler ? r.Text : "Gespeichert, auf PC und Handy gleich.");
        Log("gedaechtnis", $"Langzeitkontext gespeichert ({t.Length} Zeichen)", wo: "Gedächtnis", fehler: r.Fehler ? r.Text : null);
        if (!r.Fehler) { Stand = Zerlege(r.Text).Item1; Gelesen = t; }
    }
}

/// <summary>Die Textdatei liegt auf dem Handy; der PC liest und schreibt sie über das Relay, dadurch gibt es nur einen Stand.</summary>
public sealed class LangzeitkontextEditor : Stapel2
{
    private readonly LangzeitEntwurf e = LangzeitEntwurf.Shared;
    private void FrageVerwerfen() => Dialog.Zeige("Deine ungespeicherten Änderungen verwerfen?", null, new DialogKnopf("Abbrechen", null, Abbruch: true), new DialogKnopf("Verwerfen und laden", () => Starte(() => e.Laden(erzwingen: true)), Destruktiv: true));

    public LangzeitkontextEditor()
    {
        var feld = new TextFeld("", 13, mehrzeilig: true, text: e.Text, schreiben: t => { if (e.Text != t) e.Text = t; });
        feld.Box.MinHeight = 280;
        feld.Box.VerticalScrollBarVisibility = ScrollBarVisibility.Auto;
        void Abgleich(object? s, System.ComponentModel.PropertyChangedEventArgs a)
        {
            if (a.PropertyName == nameof(LangzeitEntwurf.Text) && feld.Text != e.Text) feld.Text = e.Text;
            if (a.PropertyName == nameof(LangzeitEntwurf.Laedt)) feld.IsEnabled = !e.Laedt;
        }
        var rahmen = new Border { Child = feld, Padding = new Thickness(6), CornerRadius = new CornerRadius(10), Background = Farbe.Tiefe(0.25) };
        var zeile = new Reaktiv(() => H(10,
            new KapselKnopf("square.and.arrow.down.fill", "Speichern", () =>
            {
                if (e.Text.Trim().Length == 0 && e.Gelesen.Length > 0)
                    Dialog.Zeige("Den ganzen Langzeitkontext leeren?", null, new DialogKnopf("Abbrechen", null, Abbruch: true), new DialogKnopf("Leeren", () => Starte(() => e.Speichern()), Destruktiv: true));
                else Starte(() => e.Speichern());
            }, hervorgehoben: e.Geaendert),
            new KapselKnopf("arrow.clockwise", "Vom Handy laden", () => { if (e.Geaendert) FrageVerwerfen(); else Starte(() => e.Laden(erzwingen: true)); }),
            e.Laedt ? new Spinner(16) : null,
            Spacer(),
            Tx($"{e.Text.Length} Zeichen{(e.Geaendert ? ", nicht gespeichert" : "")}", 11, null, e.Geaendert ? Farbe.Warnung : Farbe.Text3)), e);
        var konflikt = new Reaktiv(() =>
        {
            if (!e.Konflikt) return null;
            return H(10, new KapselKnopf("iphone", "Fassung vom Handy", FrageVerwerfen),
                new KapselKnopf("macbook", "Meine Fassung speichern", () => Starte(() => e.Speichern(ueberschreiben: true)), hervorgehoben: true));
        }, new[] { nameof(LangzeitEntwurf.Konflikt) }, e);
        var meldung = new Reaktiv(() => e.Meldung.Length == 0 ? null : Tx(e.Meldung, 12, null, e.FehlerMeldung ? Farbe.Warnung : Farbe.Gruen), new[] { nameof(LangzeitEntwurf.Meldung), nameof(LangzeitEntwurf.FehlerMeldung) }, e);
        Inhalt = V(10, Tx("Jarvis liest diesen Text bei jeder Frage mit. Füg zum Beispiel ein, was ChatGPT sich über dich gemerkt hat.", 12, null, Farbe.Text2), rahmen, zeile, konflikt, meldung);
        Loaded += (_, _) => { e.PropertyChanged += Abgleich; Starte(() => e.Laden()); };
        Unloaded += (_, _) => e.PropertyChanged -= Abgleich;
    }
}

public sealed class AktivitaetAnsicht : Decorator
{
    private readonly BereichsLader protokoll = new();
    private static int reiter;
    private readonly Decorator platz = new();

    public AktivitaetAnsicht()
    {
        var wahl = new SegmentWahl(new[] { ("0", "Am PC"), ("1", "Am Handy") }, reiter.ToString(), w => { reiter = int.Parse(w); Bauen(); }).Breite(260).Links();
        var raster = new Grid { Margin = new Thickness(36) };
        raster.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        raster.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        raster.RowDefinitions.Add(new RowDefinition());
        var kopf = new BereichsKopf("list.bullet.rectangle.portrait.fill", "Aktivität", "Was Jarvis am PC und am Handy zuletzt getan hat.");
        kopf.Margin = new Thickness(0, 0, 0, 18);
        wahl.Margin = new Thickness(0, 0, 0, 18);
        Grid.SetRow(wahl, 1);
        Grid.SetRow(platz, 2);
        raster.Children.Add(kopf);
        raster.Children.Add(wahl);
        raster.Children.Add(platz);
        Child = raster;
        Bauen();
    }

    private void Bauen()
    {
        if (reiter == 0) { platz.Child = new LogbuchListe(); return; }
        var m = new MarkdownText(protokoll.Text ?? "Lade …", 13);
        var nach = new Reaktiv(() => { m.Text = protokoll.Text ?? "Lade …"; return null; }, new[] { nameof(BereichsLader.Text) }, protokoll);
        platz.Child = new Border { Child = new Scroller(V(0, m, nach).Rand(18)), CornerRadius = new CornerRadius(18), Background = Farbe.Glas(0.03) };
        Starte(() => protokoll.Laden("protokoll_lesen"));
    }

    private sealed class LogbuchListe : Grid
    {
        private readonly StackPanel liste = new() { Margin = new Thickness(18) };
        public LogbuchListe()
        {
            RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            RowDefinitions.Add(new RowDefinition());
            var kopf = H(8, Tx($"Ausführliches Logbuch der letzten {Logbuch.AufbewahrungTage} Tage, für jede KI lesbar (JSON je Zeile).", 12, null, Farbe.Text2).Fuell(),
                new KapselKnopf("folder.fill", "Im Explorer zeigen", () => Bau.Oeffne(Logbuch.Ordner)));
            kopf.Margin = new Thickness(0, 0, 0, 18);
            Children.Add(kopf);
            var r = new Border { Child = new Scroller(liste), CornerRadius = new CornerRadius(18), Background = Farbe.Glas(0.03) };
            SetRow(r, 1);
            Children.Add(r);
            Neu();
            Loaded += (_, _) => { Logbuch.Shared.Eintraege.CollectionChanged += Geaendert; Neu(); };
            Unloaded += (_, _) => Logbuch.Shared.Eintraege.CollectionChanged -= Geaendert;
        }

        private void Neu()
        {
            liste.Children.Clear();
            foreach (var e in Logbuch.Shared.Eintraege) liste.Children.Insert(0, Zeile(e));
        }

        // Nur das Neue: Sonst baute jedes Ereignis bis zu 800 Zeilen neu.
        private void Geaendert(object? s, NotifyCollectionChangedEventArgs a)
        {
            if (a.Action == NotifyCollectionChangedAction.Add) foreach (LogEintrag e in a.NewItems!) liste.Children.Insert(0, Zeile(e));
            else if (a.Action == NotifyCollectionChangedAction.Remove && liste.Children.Count > 0) liste.Children.RemoveAt(liste.Children.Count - 1);
            else Neu();
        }

        private static UIElement Zeile(LogEintrag e)
        {
            var t = new WaehlbarerText { Text = e.Anzeige, FontSize = 12, FontFamily = Ui.Schrift, Foreground = Farbe.Text2, TextWrapping = TextWrapping.Wrap };
            return HOben(10, Tx(e.Zeit.ToString("HH:mm:ss"), 11, null, Farbe.Text3, mono: true), Tx(e.Art, 11, FontWeights.Bold, FarbeZu(e.Art), zeilen: 1).Breite(84), t.Fuell()).Rand(0, 0, 0, 6);
        }

        private static Brush FarbeZu(string art) => art switch
        {
            "fehler" => Farbe.Rot,
            "werkzeug" => Farbe.Gold,
            "gehirn" => Farbe.Haupt,
            "sprache" or "live" => Farbe.Gruen,
            "tresor" => Farbe.Warnung,
            "ablage" or "gedaechtnis" or "gespraech" => Farbe.Kupfer,
            _ => Farbe.Text3,
        };
    }
}

// MARK: - Menüleiste

/// <summary>Was bei der Mac-Fassung am Menüleisten-Symbol hängt: ein kleines Fenster über dem Infobereich, schließt sich beim Verlassen.</summary>
public sealed class MenueLeiste : Window
{
    private bool zu;
    public MenueLeiste()
    {
        Closing += (_, _) => zu = true;
        Activated += (_, _) => AppSteuerung.Shared.FensterZustandGeaendert();
        Closed += (_, _) => AppSteuerung.Shared.FensterZustandGeaendert();
        var steuerung = AppSteuerung.Shared;
        var schleife = SprachSchleife.Shared;
        Title = AppInfo.Name;
        WindowStyle = WindowStyle.None;
        ResizeMode = ResizeMode.NoResize;
        AllowsTransparency = true;
        Background = Brushes.Transparent;
        ShowInTaskbar = false;
        Topmost = true;
        Width = 300;
        SizeToContent = SizeToContent.Height;
        FontFamily = Ui.Schrift;

        var kopf = H(10, new Reaktiv(() => new JarvisKugel(0, schleife.Zustand == SprachZustand.Aus ? JarvisKugel.KugelZustand.Ruhe : JarvisKugel.KugelZustand.Hoert, 34), new[] { nameof(SprachSchleife.Zustand) }, schleife),
            V(1, Gesperrt("JARVIS", 13, FontWeights.ExtraBold, 3),
                new Reaktiv(() => Tx(steuerung.HandyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar", 10, null, Farbe.Text2), new[] { nameof(AppSteuerung.HandyVerbunden) }, steuerung)), Spacer());
        TextFeld feld = null!;
        feld = new TextFeld("Frag Jarvis …", 13, absenden: () =>
        {
            var t = feld.Text;
            feld.Text = "";
            steuerung.Gespraech = null;
            steuerung.Sende(t);
            steuerung.FensterZeigen();
            Close();
        });
        // Die Antwort läge sonst unsichtbar unter dem Prompt-Editor.
        feld.IsEnabled = steuerung.PromptBearbeiten == null;
        var feldRand = Box(feld, Farbe.Tiefe(0.25), 8, 8, 6, Farbe.Glas(0.14));
        var knoepfe = H(8, new KapselKnopf("mic.fill", "Sprechen", () => { steuerung.KuerzelSprechen(); Close(); }, hervorgehoben: true),
            new KapselKnopf("waveform", "Live", () => { steuerung.FensterZeigen(); steuerung.Live(); Close(); }));
        var offen = Tx("Jarvis öffnen  Strg+Alt+K", 13).Klick(() => { steuerung.FensterZeigen(); Close(); });
        offen.Background = Farbe.Klar;
        var ende = Tx("Beenden", 13, null, Farbe.Text2).Klick(JarvisApp.Beenden);
        ende.Background = Farbe.Klar;
        var v = V(12, kopf, feldRand, knoepfe, new Border { Height = 1, Background = Farbe.Linie }, offen, ende);
        Content = new Border { Child = v, Padding = new Thickness(16), CornerRadius = new CornerRadius(14), Background = Farbe.Grund2, BorderBrush = Farbe.Glas(0.16), BorderThickness = new Thickness(1) };
        Deactivated += (_, _) => { if (!zu) Close(); };
        Loaded += (_, _) =>
        {
            var a = SystemParameters.WorkArea;
            Left = a.Right - ActualWidth - 12;
            Top = a.Bottom - ActualHeight - 12;
            feld.Box.Focus();
        };
        KeyDown += (_, e) => { if (e.Key == Key.Escape) Close(); };
    }
}
