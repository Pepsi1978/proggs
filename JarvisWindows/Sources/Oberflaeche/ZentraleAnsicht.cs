using System.Diagnostics;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using static JarvisWindows.Ui;

namespace JarvisWindows;

public sealed class ZentraleDaten : Beobachtbar
{
    public static ZentraleDaten Shared { get; } = new();
    private string? _auswertung, _aufgaben, _termine, _wetter;
    public string? Auswertung { get => _auswertung; set => Setze(ref _auswertung, value); }
    public string? Aufgaben { get => _aufgaben; set => Setze(ref _aufgaben, value); }
    public string? Termine { get => _termine; set => Setze(ref _termine, value); }
    public string? Wetter { get => _wetter; set => Setze(ref _wetter, value); }
    private bool _laedt;
    public bool Laedt { get => _laedt; set => Setze(ref _laedt, value); }
    private DateTime? _zuletzt;
    public DateTime? Zuletzt { get => _zuletzt; set => Setze(ref _zuletzt, value); }
    private DateTime? _letzterFehlschlag;
    /// <summary>Gesetzt, wenn der letzte Abruf gar nichts brachte; ein Erfolg löscht es.</summary>
    public DateTime? LetzterFehlschlag { get => _letzterFehlschlag; set => Setze(ref _letzterFehlschlag, value); }

    /// <summary>Lädt unabhängig von der Ansicht, damit ein Wechsel den Abruf nicht abbricht. Nach einem Fehlschlag frühestens nach einer Minute wieder.</summary>
    public void Vorladen()
    {
        if (LetzterFehlschlag is { } f && (DateTime.Now - f).TotalSeconds < 60) return;
        Starte(() => Laden());
    }

    public async Task Laden(bool erzwingen = false)
    {
        if (Laedt) return;
        if (!erzwingen && Zuletzt is { } z && (DateTime.Now - z).TotalSeconds < 300) return;
        Laedt = true;
        try
        {
            var bis = DateTime.Now.AddDays(2).ToString("yyyy-MM-dd");
            var a = McpKlient.Shared.Text("tagesauswertung_lesen");
            var b = McpKlient.Shared.Text("aufgaben_lesen", new JSONObjekt { ["bereich"] = "heute" });
            var c = McpKlient.Shared.Text("kalender_lesen", new JSONObjekt { ["von"] = "heute", ["bis"] = bis });
            var d = McpKlient.Shared.Text("wetter_lesen", new JSONObjekt { ["datum"] = "heute", ["tage"] = 2 });
            await Task.WhenAll(a, b, c, d);
            string? ra = a.Result, rb = b.Result, rc = c.Result, rd = d.Result;
            // Was gerade nicht kam, bleibt stehen; nur ein Erfolg sperrt das erneute Laden für fünf Minuten.
            Auswertung = ra != null ? KuerzeAuswertung(ra) : Auswertung;
            Aufgaben = rb ?? Aufgaben; Termine = rc ?? Termine; Wetter = rd ?? Wetter;
            if (ra != null || rb != null || rc != null || rd != null)
            {
                Zuletzt = DateTime.Now;
                LetzterFehlschlag = null;
            }
            else LetzterFehlschlag = DateTime.Now;
        }
        finally { Laedt = false; }
    }

    /// <summary>Nur der Teil für Frank (ohne den angehängten Regel- und Gedächtnisblock).</summary>
    public static string KuerzeAuswertung(string t)
    {
        var s = t;
        foreach (var marke in new[] { "\nFRANKS REGELN", "\n--- SEIT DIESER", "\nGESAMTBILD FEHLT" })
        {
            var i = s.IndexOf(marke, StringComparison.Ordinal);
            if (i >= 0) s = s[..i];
        }
        return s.Trim();
    }
}

public sealed class ZentraleAnsicht : Decorator
{
    private readonly ZentraleDaten daten = ZentraleDaten.Shared;
    private readonly AppSteuerung steuerung = AppSteuerung.Shared;
    private bool auswertungOffen, neuLaeuft, neuFehler;
    private readonly HilfsMelder melder = new();
    private sealed class HilfsMelder : Beobachtbar { public void Sag() => Melde("Stand"); }

    public static string Gruss
    {
        get
        {
            var h = DateTime.Now.Hour;
            return h is >= 5 and < 11 ? "Guten Morgen, Frank." : h is >= 11 and < 17 ? "Hallo Frank." : h is >= 17 and < 22 ? "Guten Abend, Frank." : "Noch wach, Frank?";
        }
    }

    public static string DatumZeile => DateTime.Now.ToString("dddd, d. MMMM yyyy", Deutsch);

    public ZentraleAnsicht()
    {
        var karten = new Dictionary<string, Func<UIElement>>
        {
            ["pc"] = PcKarte,
            ["auswertung"] = () => new Reaktiv(AuswertungsKarte, new[] { nameof(ZentraleDaten.Auswertung), nameof(ZentraleDaten.Laedt), "Stand" }, daten, melder),
            ["biomarker"] = BiomarkerKarte.Bauen,
            ["solar"] = SolarKarte.Bauen,
            ["aufgaben"] = () => ListenKarte("checklist", "Aufgaben heute", Farbe.Gruen, () => daten.Aufgaben, nameof(ZentraleDaten.Aufgaben)),
            ["termine"] = () => ListenKarte("calendar", "Termine", Farbe.Gold, () => daten.Termine, nameof(ZentraleDaten.Termine)),
            ["wetter"] = () => ListenKarte("cloud.sun.fill", "Wetter", Farbe.Warnung, () => daten.Wetter, nameof(ZentraleDaten.Wetter)),
        };
        // Wie die Startbereiche am Handy (Modul M1.2): jede Karte an der ganzen Fläche greifen und verschieben; die Reihenfolge bleibt gespeichert.
        var stapel = V(20);
        var aus = Einstellungen.Shared.ZentraleAusgeblendet.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        foreach (var id in Reihenfolge(karten.Keys).Where(k => !aus.Contains(k))) stapel.Children.Add(new Decorator { Child = karten[id](), Tag = id });
        Ziehen.Ordnen(stapel, ids =>
        {
            Einstellungen.Shared.ZentraleReihenfolge = string.Join(",", ids);
            Log("oberflaeche", "Zentrale umsortiert: " + string.Join(", ", ids), wo: "Zentrale");
        });
        var v = V(20, new ZugangsHinweis(), stapel, Tx("Karten lassen sich per Drag & Drop verschieben.", 11, null, Farbe.Text3).Mittig());
        v.Margin = new Thickness(28, 26, 28, 26);
        Child = new Scroller(v);
        Loaded += (_, _) =>
        {
            daten.Vorladen();
            if (Einstellungen.Shared.BiomarkerBeimStart && !aus.Contains("biomarker")) Starte(() => BioDaten.Shared.Laden());
            // Wie am Handy einmal je App-Start von selbst, danach per Knopf oder im eingestellten Takt.
            if (Einstellungen.Shared.SolarBeimStart && !aus.Contains("solar") && !SolarDaten.Shared.AutomatischGelesen) { SolarDaten.Shared.AutomatischGelesen = true; Starte(SolarDaten.Shared.Lesen); }
        };
    }

    /// <summary>Die Werks-Reihenfolge wie am Handy oben die Lage, darunter Biomarker und Solarbatterie; neue Karten kommen hinten dazu.</summary>
    public static readonly string[] Werk = { "pc", "auswertung", "biomarker", "solar", "aufgaben", "termine", "wetter" };

    public static List<string> Reihenfolge(IEnumerable<string> vorhanden)
    {
        var da = vorhanden.ToList();
        var gespeichert = Einstellungen.Shared.ZentraleReihenfolge.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).Where(da.Contains).Distinct().ToList();
        return gespeichert.Concat(Werk.Where(da.Contains).Where(x => !gespeichert.Contains(x))).Concat(da.Where(x => !gespeichert.Contains(x) && !Werk.Contains(x))).ToList();
    }

    private UIElement AuswertungsKarte()
    {
        var kopf = H(8, new KartenTitel("sun.horizon.fill", "Tagesauswertung", Farbe.Haupt).Fuell(),
            daten.Laedt ? new Spinner(16) : null,
            new RundKnopf("speaker.wave.2.fill", () => { if (daten.Auswertung is { } a) Starte(() => Stimme.Shared.Sprich(a)); }, hilfe: "Vorlesen", groesse: 30),
            new RundKnopf("arrow.clockwise", () => Starte(() => daten.Laden(erzwingen: true)), hilfe: "Neu laden", groesse: 30),
            new RundKnopf("sparkles", () =>
            {
                if (neuLaeuft) return;
                neuLaeuft = true;
                melder.Sag();
                Starte(async () =>
                {
                    var t = await McpKlient.Shared.Text("tagesauswertung_lesen", new JSONObjekt { ["neu_erstellen"] = true }, sekunden: 240);
                    if (t != null) daten.Auswertung = ZentraleDaten.KuerzeAuswertung(t); else neuFehler = true;
                    neuLaeuft = false;
                    melder.Sag();
                });
            }, hilfe: "Neue Auswertung erstellen lassen (1–3 Minuten)", aktiv: neuLaeuft, groesse: 30));
        var s = V(14, kopf);
        if (neuLaeuft) s.Children.Add(Tx("Jarvis schreibt eine neue Auswertung – das dauert ein bis drei Minuten …", 12, null, Farbe.Haupt));
        if (neuFehler) s.Children.Add(Tx("Die neue Auswertung kam nicht an – das Handy war nicht erreichbar oder brauchte zu lange.", 12, null, Farbe.Warnung));
        if (daten.Auswertung is { } a)
        {
            s.Children.Add(new MarkdownText(auswertungOffen ? a : Anfang(a), 14));
            s.Children.Add(Tx(auswertungOffen ? "Weniger anzeigen" : "Ganze Auswertung lesen", 13, FontWeights.SemiBold, Farbe.Haupt).Links().Klick(() => { auswertungOffen = !auswertungOffen; melder.Sag(); }));
        }
        else if (!daten.Laedt)
            s.Children.Add(Tx("Das Handy-Jarvis ist gerade nicht erreichbar – sobald es online ist, steht hier deine Tagesauswertung.", 14, null, Farbe.Text2));
        else
            s.Children.Add(Tx("Lade deine Tagesauswertung …", 14, null, Farbe.Text2));
        return new GlasKarte(s);
    }

    public static string Anfang(string t) => string.Join("\n\n", t.Split("\n\n").Take(3));

    private UIElement ListenKarte(string symbol, string titel, Brush farbe, Func<string?> text, string eigenschaft) => new Reaktiv(() =>
    {
        var s = V(12, new KartenTitel(symbol, titel, farbe));
        if (text() is { } t) s.Children.Add(new MarkdownText(Aufbereiten(t), 13));
        else s.Children.Add(Tx(daten.Laedt ? "Lade …" : "Nicht verfügbar", 13, null, Farbe.Text3));
        return new GlasKarte(s);
    }, new[] { eigenschaft, nameof(ZentraleDaten.Laedt) }, daten);

    /// <summary>Werkzeugtexte etwas lesbarer: interne Kennungen raus.</summary>
    public static string Aufbereiten(string t, int grenze = 1600)
    {
        var s = Regex.Replace(t, @"\[id \d+\] ", "");
        s = McpKlient.OhneKontextHinweis(s);
        if (s.Length > grenze) s = s[..grenze] + " …";
        return s;
    }

    private UIElement PcKarte()
    {
        var gitter = new AdaptivGitter { Minimum = 190, Abstand = 10 };
        void Kachel(string symbol, string titel, Brush farbe, Action a) => gitter.Children.Add(new WinKachel(symbol, titel, farbe, a));
        Kachel("terminal.fill", "Programmier-Session", Farbe.Haupt, () => { steuerung.NeuesGespraech(); steuerung.Entwurf = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: "; });
        Kachel("arrow.triangle.2.circlepath", "Updates prüfen", Farbe.Gruen, () => { steuerung.Gespraech = null; steuerung.Sende("Prüfe in der Updater-Zentrale, welche Programme auf dem PC ein Update brauchen, und sag mir das Ergebnis."); });
        Kachel("eye.fill", "Bildschirm ansehen", Farbe.Gold, () => { steuerung.Gespraech = null; steuerung.Sende("Schau dir meinen Bildschirm an und sag mir kurz, was du siehst."); });
        Kachel("gauge.with.dots.needle.67percent", "PC-Status", Farbe.Warnung, () => { steuerung.Gespraech = null; steuerung.Sende("Wie geht es meinem PC? Kurzer Status."); });
        Kachel("bubble.left.and.bubble.right.fill", "ChatGPT öffnen", Farbe.Gruen, () => Starte(() => Shell.Pwsh("$a = Get-StartApps | Where-Object Name -like 'ChatGPT*' | Select-Object -First 1; if ($a) { Start-Process \"shell:AppsFolder\\$($a.AppID)\" } else { Start-Process 'https://chatgpt.com' }")));
        Kachel("square.stack.3d.up.fill", "OpenLauncher", Farbe.Haupt, OpenLauncherStarten);
        return new GlasKarte(V(14, new KartenTitel("macbook", "Am PC", Farbe.Kupfer), gitter));
    }

    private static void OpenLauncherStarten()
    {
        Task.Run(() =>
        {
            try
            {
                var wurzel = System.IO.Path.Combine(Pfade.Proggs, "OpenLauncher", "bin", "Release");
                var exe = new DirectoryInfo(wurzel).EnumerateFiles("OpenLauncher.exe", SearchOption.AllDirectories)
                    .Where(f => f.Directory?.Name == "win-x64").OrderByDescending(f => f.LastWriteTime).FirstOrDefault();
                if (exe == null) { Log("fehler", "OpenLauncher nicht gefunden", wo: "Zentrale/Am PC"); return; }
                using var _ = Process.Start(new ProcessStartInfo(exe.FullName) { UseShellExecute = true, WorkingDirectory = exe.DirectoryName });
            }
            catch (Exception ex) { Log("fehler", "OpenLauncher nicht gestartet", wo: "Zentrale/Am PC", fehler: ex.Message); }
        });
    }
}

/// <summary>Fehlt Jarvis jeder Zugang zu einem Modell, sagt das der erste Bildschirm.</summary>
public sealed class ZugangsHinweis : Decorator
{
    public ZugangsHinweis()
    {
        Visibility = Visibility.Collapsed;
        Loaded += (_, _) => Starte(async () =>
        {
            var da = await Einstellungen.Shared.GehirnZugang();
            Visibility = da ? Visibility.Collapsed : Visibility.Visible;
            if (da) { Child = null; return; }
            Child = new GlasKarte(H(14, Sym.Bild("key.horizontal.fill", 20, Farbe.Warnung),
                V(3, Tx("Noch ein Schritt: Jarvis braucht einen Kopf", 15, FontWeights.SemiBold), Tx("Melde dich einmal mit deinem ChatGPT-Konto an (läuft über dein Abo) oder trage einen Anthropic-Schlüssel ein.", 12, null, Farbe.Text2)).Fuell(),
                new KapselKnopf("arrow.right", "Zu den Konten", () =>
                {
                    EinstellungenAnsicht.StartReiter = "konten";
                    AppSteuerung.Shared.Bereich = Bereich.Einstellungen;
                }, hervorgehoben: true)));
        });
    }
}

public sealed class WinKachel : Border
{
    private readonly ScaleTransform skala = new();
    public WinKachel(string symbol, string titel, Brush farbe, Action aktion)
    {
        Child = H(10, new Border { Width = 34, Height = 34, CornerRadius = new CornerRadius(10), Background = Farbe.Mit(farbe, 0.13), Child = Sym.Bild(symbol, 15, farbe) },
            Tx(titel, 13, FontWeights.SemiBold, Farbe.Text, zeilen: 1).Fuell());
        Padding = new Thickness(10);
        CornerRadius = new CornerRadius(14);
        BorderThickness = new Thickness(1);
        BorderBrush = Farbe.Glas(0.06);
        this.SchwebeFlaeche(Farbe.Glas(0.04), Farbe.Glas(0.09));
        RenderTransformOrigin = new Point(0.5, 0.5);
        RenderTransform = skala;
        this.Schwebe(an =>
        {
            BorderBrush = Farbe.Glas(an ? 0.14 : 0.06);
            var a = new DoubleAnimation(an ? 1.015 : 1, new Duration(TimeSpan.FromMilliseconds(250))) { EasingFunction = new BackEase { EasingMode = EasingMode.EaseOut, Amplitude = 0.3 } };
            skala.BeginAnimation(ScaleTransform.ScaleXProperty, a);
            skala.BeginAnimation(ScaleTransform.ScaleYProperty, a);
        });
        this.Klick(() => { Log("oberflaeche", $"Funktionsboard: {titel} gestartet", wo: "Zentrale/Am PC"); aktion(); });
    }
}
