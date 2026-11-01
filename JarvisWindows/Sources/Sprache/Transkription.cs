using System.Globalization;
using System.Runtime.CompilerServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace JarvisWindows;

/// <summary>Transkription wie im Handy-Jarvis (Modul M1.4): Groq Whisper Large V3 Turbo oder Large V3 mit Wörterbuch und
/// vier Filtern gegen erfundene Texte, Parakeet primeline offline, dazu am PC die Spracherkennung von Windows.
/// Auswahl in `Einstellungen.Transkription`: "parakeet" | "groq" | "windows". Nie ein stiller Wechsel auf ein anderes
/// Verfahren; nur bei Groq nimmt Jarvis wie am Handy Turbo, solange Large V3 für die Organisation gesperrt ist.</summary>
public sealed class TranskriptionsEinstellungen : Beobachtbar
{
    public static TranskriptionsEinstellungen Shared { get; } = new();

    public const string ModellTurbo = "whisper-large-v3-turbo";
    public const string ModellLargeV3 = "whisper-large-v3";
    public const string StandardWoerterbuch = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic";
    /// <summary>Startliste des Parakeet-Wörterbuchs, gleicher Stand wie ParakeetUeberJarvis.STANDARD_WOERTERBUCH (Modul M1.4).</summary>
    public const string StandardParakeetWoerterbuch = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic";

    private bool Merke<T>(ref T feld, T wert, string schluessel, [CallerMemberName] string? name = null)
    {
        if (!Setze(ref feld, wert, name)) return false;
        Vorgaben.Setze(schluessel, wert);
        return true;
    }

    private string _whisperModell = Vorgaben.Text("tk.whisperModell") ?? ModellTurbo;
    public string WhisperModell { get => _whisperModell; set => Merke(ref _whisperModell, value, "tk.whisperModell"); }
    private bool _filterStilleVorab = Vorgaben.Wahr("tk.filterStille", true);
    public bool FilterStilleVorab { get => _filterStilleVorab; set => Merke(ref _filterStilleVorab, value, "tk.filterStille"); }
    private bool _filterSegmente = Vorgaben.Wahr("tk.filterSegmente", true);
    public bool FilterSegmente { get => _filterSegmente; set => Merke(ref _filterSegmente, value, "tk.filterSegmente"); }
    private bool _filterZeitstempel = Vorgaben.Wahr("tk.filterZeitstempel", true);
    public bool FilterZeitstempel { get => _filterZeitstempel; set => Merke(ref _filterZeitstempel, value, "tk.filterZeitstempel"); }
    private bool _filterFloskeln = Vorgaben.Wahr("tk.filterFloskeln", true);
    public bool FilterFloskeln { get => _filterFloskeln; set => Merke(ref _filterFloskeln, value, "tk.filterFloskeln"); }
    private bool _woerterbuchAn = Vorgaben.Wahr("tk.woerterbuchAn", true);
    public bool WoerterbuchAn { get => _woerterbuchAn; set => Merke(ref _woerterbuchAn, value, "tk.woerterbuchAn"); }
    private string _woerterbuchBegriffe = Vorgaben.Text("tk.woerterbuch") ?? StandardWoerterbuch;
    public string WoerterbuchBegriffe { get => _woerterbuchBegriffe; set => Merke(ref _woerterbuchBegriffe, value, "tk.woerterbuch"); }
    // Eigener Schalter und eigene Liste für Parakeet, getrennt vom Whisper-Wörterbuch.
    private bool _parakeetWoerterbuchAn = Vorgaben.Wahr("tk.parakeetWoerterbuchAn", true);
    public bool ParakeetWoerterbuchAn { get => _parakeetWoerterbuchAn; set => Merke(ref _parakeetWoerterbuchAn, value, "tk.parakeetWoerterbuchAn"); }
    private string _parakeetWoerterbuch = Vorgaben.Text("tk.parakeetWoerterbuch") ?? StandardParakeetWoerterbuch;
    public string ParakeetWoerterbuch { get => _parakeetWoerterbuch; set => Merke(ref _parakeetWoerterbuch, value, "tk.parakeetWoerterbuch"); }

    private double? _download;
    /// <summary>Fortschritt des Parakeet-Downloads (0…1), null = kein Download.</summary>
    public double? Download { get => _download; set => Setze(ref _download, value); }
    private string? _downloadFehler;
    public string? DownloadFehler { get => _downloadFehler; set => Setze(ref _downloadFehler, value); }

    private TranskriptionsEinstellungen()
    {
        // Vorgabe wie gewünscht: Parakeet, wenn das geteilte Modell (von TVO/CVO) schon auf dem Rechner liegt.
        if (!Vorgaben.Hat("transkription") && ParakeetLocalClient.IsInstalled) Einstellungen.Shared.Transkription = "parakeet";
    }

    /// <summary>Wörterbuch als Whisper-Prompt („Begriffe: A, B.“), höchstens 350 Zeichen und 100 Begriffe, wie am Handy.</summary>
    public string? Prompt
    {
        get
        {
            if (!WoerterbuchAn) return null;
            var gesehen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var begriffe = WoerterbuchBegriffe.Split('\n', '\r', ',', ';')
                .Select(t => t.Trim(' ', '\t'))
                .Where(t => t.Length > 0 && !t.StartsWith('#') && gesehen.Add(t))
                .Take(100).ToList();
            if (begriffe.Count == 0) return null;
            var s = "Begriffe: ";
            var n = 0;
            foreach (var w in begriffe)
            {
                if (s.Length + w.Length + 2 > 350) continue;
                if (n > 0) s += ", ";
                s += w;
                n++;
            }
            return n == 0 ? null : s + ".";
        }
    }

    /// <summary>Wortliste für Parakeet je Aufnahme; leer = ohne Wörterbuch.</summary>
    public string ParakeetHotwords => ParakeetWoerterbuchAn ? ParakeetLocalClient.Hotwords(ParakeetWoerterbuch) : "";

    public string AnzeigeName => Einstellungen.Shared.Transkription switch
    {
        "parakeet" => "Parakeet primeline (offline)",
        "groq" => WhisperModell == ModellLargeV3 ? "Groq Whisper Large V3" : "Groq Whisper Large V3 Turbo",
        _ => "Windows-Spracherkennung",
    };

    public void ParakeetLaden()
    {
        if (Download != null) return;
        Download = 0;
        DownloadFehler = null;
        ParakeetLocalClient.Download(
            (n, gesamt) => Download = (double)n / Math.Max(gesamt, 1),
            fehler =>
            {
                Download = null;
                DownloadFehler = fehler?.Message;
                if (fehler == null) ParakeetLocalClient.Shared.Preload();
            });
    }
}

/// <summary>Einheitlicher Einstieg fürs Diktat: 16-kHz-Mono-Samples → Text mit dem gewählten Verfahren.</summary>
public static class Transkription
{
    public sealed class Fehler : Exception
    {
        public Fehler(string text) : base(text) { }
    }

    public static Task<string> Erkenne(float[] samples) => Haupt.Auf(async () =>
    {
        var e = TranskriptionsEinstellungen.Shared;
        // Die Zeitachse über die ganze Aufnahme rechnen: nicht auf dem Hauptfaden.
        var analyse = await Task.Run(() => new SprachAnalyse(samples));
        switch (Einstellungen.Shared.Transkription)
        {
            case "parakeet":
                if (!ParakeetLocalClient.IsInstalled)
                    throw new Fehler("Offline-Modell Parakeet fehlt – Einstellungen → Sprache → Transkription → „Modell laden“. Kein Rückfall auf Groq.");
                // „Stille vorab“ gilt wie am Handy auch offline.
                if (e.FilterStilleVorab && analyse.StimmMs < SprachAnalyse.MinSprachMs) return "";
                var zuschnitt = e.FilterStilleVorab ? analyse.Zugeschnitten(samples) : samples;
                return await ParakeetLocalClient.Shared.Transkribiere(zuschnitt, e.ParakeetHotwords);
            default:
                return await GroqWhisper.Transkribiere(samples, analyse, Einstellungen.Shared.Zugang.Groq, e.WhisperModell, e.Prompt,
                    new GroqWhisper.Filter(e.FilterStilleVorab, e.FilterSegmente, e.FilterZeitstempel, e.FilterFloskeln));
        }
    });
}

/// <summary>Stimm-Zeitachse einer Aufnahme (20-ms-Rahmen): Grundlage für „Stille vorab“, Zuschnitt und Zeitstempel-Filter.</summary>
public sealed class SprachAnalyse
{
    public const int RahmenMs = 20;
    // Unter der Diktat-Schwelle (mindestens 0,012), sonst verwirft „Stille vorab“ leises Sprechen, das das Diktat als begonnen zählt.
    public const float RmsSchwelle = 0.009f;
    public const int MinSprachMs = 150;
    public bool[] Stimmhaft { get; }
    public int StimmMs => Stimmhaft.Count(x => x) * RahmenMs;

    public SprachAnalyse(float[] samples, int rate = 16000)
    {
        var n = rate * RahmenMs / 1000;
        var r = new List<bool>(samples.Length / n + 1);
        var i = 0;
        while (i + n <= samples.Length)
        {
            float s = 0;
            for (int j = i; j < i + n; j++) s += samples[j] * samples[j];
            r.Add(MathF.Sqrt(s / n) > RmsSchwelle);
            i += n;
        }
        Stimmhaft = r.ToArray();
    }

    private SprachAnalyse(bool[] stimmhaft) { Stimmhaft = stimmhaft; }

    /// <summary>Stille vorn und hinten abschneiden (mit 300 ms Rand), damit nichts Leeres hochgeht.</summary>
    public float[] Zugeschnitten(float[] samples, int rate = 16000)
    {
        int a = Array.IndexOf(Stimmhaft, true), b = Array.LastIndexOf(Stimmhaft, true);
        if (a < 0 || b < 0) return samples;
        var n = rate * RahmenMs / 1000;
        const int rand = 15;
        int von = Math.Max(0, (a - rand) * n), bis = Math.Min(samples.Length, (b + 1 + rand) * n);
        return von < bis ? samples[von..bis] : samples;
    }

    /// <summary>Zeitachse passend zur zugeschnittenen Aufnahme.</summary>
    public SprachAnalyse NachZuschnitt()
    {
        int a = Array.IndexOf(Stimmhaft, true), b = Array.LastIndexOf(Stimmhaft, true);
        if (a < 0 || b < 0) return this;
        int von = Math.Max(0, a - 15), bis = Math.Min(Stimmhaft.Length, b + 1 + 15);
        return new SprachAnalyse(Stimmhaft[von..bis]);
    }

    public bool SegmentHatSprache(double start, double ende, double minAnteil = 0.10)
    {
        if (Stimmhaft.Length == 0 || ende <= start) return true;
        var erst = Math.Min(Math.Max((int)(start * 1000 / RahmenMs), 0), Stimmhaft.Length - 1);
        var letzt = Math.Min(Math.Max((int)(ende * 1000 / RahmenMs), erst), Stimmhaft.Length - 1);
        var n = 0;
        for (int i = erst; i <= letzt; i++) if (Stimmhaft[i]) n++;
        return (double)n / (letzt - erst + 1) >= minAnteil;
    }
}

// MARK: - Einstellungsansicht

public sealed class TranskriptionsEinstellungenAnsicht : UserControl
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly TranskriptionsEinstellungen tk = TranskriptionsEinstellungen.Shared;
    private readonly Merker auffrischen = new();
    // Textfelder überleben den Neubau: Sonst ginge beim Umschalten das Getippte und der Fokus verloren.
    private readonly TextFeld begriffe;
    private readonly TextFeld parakeetBegriffe;
    private readonly FrameworkElement begriffeSpeichern;
    private readonly FrameworkElement parakeetSpeichern;

    private sealed class Merker : Beobachtbar
    {
        public void Melden() => Melde("Stand");
    }

    public TranskriptionsEinstellungenAnsicht()
    {
        begriffe = new TextFeld("", 12, mehrzeilig: true, text: tk.WoerterbuchBegriffe);
        parakeetBegriffe = new TextFeld("", 12, mehrzeilig: true, text: tk.ParakeetWoerterbuch);
        begriffeSpeichern = new KapselKnopf("checkmark", "Begriffe speichern", () => { tk.WoerterbuchBegriffe = begriffe.Text; }, hervorgehoben: true).Links();
        parakeetSpeichern = new KapselKnopf("checkmark", "Speichern", () => { tk.ParakeetWoerterbuch = parakeetBegriffe.Text; }, hervorgehoben: true).Links();
        void Abgleich()
        {
            begriffeSpeichern.Sichtbar(begriffe.Text != tk.WoerterbuchBegriffe);
            parakeetSpeichern.Sichtbar(parakeetBegriffe.Text != tk.ParakeetWoerterbuch);
        }
        begriffe.Geaendert += _ => Abgleich();
        parakeetBegriffe.Geaendert += _ => Abgleich();
        Abgleich();
        var nur = new[]
        {
            nameof(Einstellungen.Transkription), nameof(TranskriptionsEinstellungen.Download), nameof(TranskriptionsEinstellungen.DownloadFehler),
            nameof(TranskriptionsEinstellungen.WhisperModell), nameof(TranskriptionsEinstellungen.FilterStilleVorab), nameof(TranskriptionsEinstellungen.FilterSegmente),
            nameof(TranskriptionsEinstellungen.FilterZeitstempel), nameof(TranskriptionsEinstellungen.FilterFloskeln), nameof(TranskriptionsEinstellungen.WoerterbuchAn),
            nameof(TranskriptionsEinstellungen.WoerterbuchBegriffe), nameof(TranskriptionsEinstellungen.ParakeetWoerterbuchAn), nameof(TranskriptionsEinstellungen.ParakeetWoerterbuch),
            "Stand",
        };
        void BeiAenderung(object? _, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if (e.PropertyName is nameof(TranskriptionsEinstellungen.WoerterbuchBegriffe) or nameof(TranskriptionsEinstellungen.ParakeetWoerterbuch)) Abgleich();
        }
        Loaded += (_, _) => tk.PropertyChanged += BeiAenderung;
        Unloaded += (_, _) => tk.PropertyChanged -= BeiAenderung;
        Content = new Reaktiv(Bauen, nur, einst, tk, auffrischen);
    }

    private static T Loese<T>(T e) where T : FrameworkElement
    {
        if (e.Parent is Panel p) p.Children.Remove(e);
        else if (e.Parent is Decorator d) d.Child = null;
        return e;
    }

    private UIElement Bauen()
    {
        var t = einst.Transkription;
        Stapel? filter = null;
        if (t != "windows")
        {
            filter = Ui.V(8,
                Ui.Tx("Filter gegen erfundene Texte bei Stille", 13, FontWeights.SemiBold).Rand(0, 4, 0, 0),
                Ui.H(8,
                    Chip("Stille vorab", () => tk.FilterStilleVorab, v => tk.FilterStilleVorab = v),
                    t == "groq" ? Chip("Segment-Werte", () => tk.FilterSegmente, v => tk.FilterSegmente = v) : null,
                    t == "groq" ? Chip("Zeitstempel", () => tk.FilterZeitstempel, v => tk.FilterZeitstempel = v) : null,
                    t == "groq" ? Chip("Floskeln", () => tk.FilterFloskeln, v => tk.FilterFloskeln = v) : null),
                t == "parakeet" ? Ui.Tx("Die Whisper-Filter gelten nur für Groq; „Stille vorab“ wirkt auch hier.", 11, null, Farbe.Text3) : null);
        }
        return Ui.V(14,
            new KartenTitel("text.bubble.fill", "Transkription (Spracheingabe)", Farbe.Gruen),
            Ui.Tx("Gilt für Diktat, Sprachgespräch und „Hey Jarvis“. Jarvis wechselt nie still auf ein anderes Verfahren; fehlt etwas, sagt er es.", 12, null, Farbe.Text2),
            Ui.V(8,
                Wahl("parakeet", "Parakeet primeline – offline", "Deutscher Fine-Tune von Parakeet TDT 0.6B v3, int8 über sherpa-onnx, nur CPU. Kein Internet, kein Schlüssel."),
                Wahl("groq", "Groq Whisper – online", "Whisper Large V3 Turbo oder Large V3 bei Groq, mit Wörterbuch und Filtern. Braucht Netz und Groq-Schlüssel."),
                Wahl("windows", "Windows – online", "Spracherkennung von Windows, mit Live-Vorschau beim Sprechen. Braucht die eingeschaltete Online-Spracherkennung in den Windows-Einstellungen.")),
            t == "parakeet" ? Parakeet() : null,
            t == "groq" ? Groq() : null,
            filter);
    }

    private FrameworkElement Wahl(string id, string titel, string text)
    {
        var aktiv = einst.Transkription == id;
        return Ui.Box(
            Ui.HOben(12,
                Sym.Bild(aktiv ? "largecircle.fill.circle" : "circle", 16, aktiv ? Farbe.Gruen : Farbe.Text3),
                Ui.V(3, Ui.Tx(titel, 14, FontWeights.SemiBold), Ui.Tx(text, 12, null, Farbe.Text2)).Fuell()),
            Farbe.Glas(aktiv ? 0.08 : 0.03), 14, 12, 12, aktiv ? Farbe.Mit(Farbe.Gruen, 0.5) : Farbe.Glas(0.06))
            .Klick(() => einst.Transkription = id);
    }

    private static FrameworkElement Chip(string titel, Func<bool> lies, Action<bool> setze)
    {
        var an = lies();
        var farbe = an ? Farbe.Schwarz : Farbe.Text2;
        return Ui.Kapsel(Ui.H(6, Sym.Bild(an ? "checkmark" : "xmark", 10, farbe), Ui.Tx(titel, 12, FontWeights.SemiBold, farbe, zeilen: 1)),
            an ? Farbe.Gruen : Farbe.Glas(0.07), 12, 7).Klick(() => setze(!lies())).Links();
    }

    private static FrameworkElement ChipWahl(string titel, bool aktiv, Action aktion) =>
        Ui.Kapsel(Ui.Tx(titel, 12, FontWeights.SemiBold, aktiv ? Farbe.Schwarz : Farbe.Text2, zeilen: 1), aktiv ? Farbe.Haupt : Farbe.Glas(0.07), 12, 7).Klick(aktion).Links();

    private static string Fest(double wert, string format) => wert.ToString(format, CultureInfo.InvariantCulture);

    private UIElement Parakeet()
    {
        static string Mb(long b) => $"{b / 1_048_576} MB";
        var da = ParakeetLocalClient.IsInstalled;
        TextBlock zustand = tk.Download is double p
            ? Ui.Tx($"Lädt … {(int)(p * 100)} %", 12, null, Farbe.Text2)
            : da ? Ui.Tx($"Bereit, {Mb(ParakeetLocalClient.BelegteBytes)} belegt", 12, FontWeights.Medium, Farbe.Gruen)
                 : Ui.Tx($"Nicht geladen ({Mb(ParakeetLocalClient.TotalBytes)})", 12, null, Farbe.Text3);
        TextBlock? messung = null;
        if (ParakeetLocalClient.Shared.Messung is { } m)
        {
            messung = Ui.Tx($"Gemessen auf diesem PC: Laden und Aufwärmen {Fest(m.LadeMs / 1000.0, "0.0")} s, Arbeitsspeicher etwa {m.RamMb} MB"
                + (m.AudioMs > 0 ? $"; letzte Mitschrift {Fest(m.AudioMs / 1000.0, "0.0")} s Aufnahme in {Fest(m.RechenMs / 1000.0, "0.00")} s gerechnet." : "."), 11, null, Farbe.Text2);
        }
        FrameworkElement? knopf = null;
        if (tk.Download == null && !da)
            knopf = new KapselKnopf("arrow.down.circle.fill", $"Modell laden ({Mb(ParakeetLocalClient.TotalBytes)}, einmalig)", tk.ParakeetLaden, hervorgehoben: true).Links();
        else if (da)
            knopf = new KapselKnopf("bolt.fill", "Vorladen und messen", () =>
            {
                ParakeetLocalClient.Shared.Preload();
                Ui.Starte(async () => { await Task.Delay(8000); auffrischen.Melden(); });
            }).Links();
        return Ui.Box(Ui.V(8,
            Ui.H(0, Ui.Tx("Modell", 13, FontWeights.SemiBold), Ui.Spacer(), zustand),
            tk.Download is double q ? Bau.Balken(q, Farbe.Gruen) : null,
            messung,
            Ui.Tx(@"Das Modell liegt geteilt unter %LOCALAPPDATA%\VoiceOverlays\ – dasselbe wie in TerminalVoiceOverlay und ClaudeVoiceOverlay.", 11, null, Farbe.Text3),
            knopf,
            tk.DownloadFehler is string f ? Ui.Tx(f, 12, null, Farbe.Rot) : null,
            Ui.Tx("Wörterbuch", 13, FontWeights.SemiBold).Rand(0, 4, 0, 0),
            Chip("Wörterbuch nutzen", () => tk.ParakeetWoerterbuchAn, v => tk.ParakeetWoerterbuchAn = v),
            tk.ParakeetWoerterbuchAn ? Ui.Tx("Begriffe, die Parakeet bevorzugt erkennen soll (durch Komma oder Zeilenumbruch getrennt, höchstens 100). Die dafür nötige Beam-Suche rechnet rund 30 % langsamer.", 11, null, Farbe.Text2) : null,
            tk.ParakeetWoerterbuchAn ? Editor(parakeetBegriffe) : null,
            tk.ParakeetWoerterbuchAn ? Loese(parakeetSpeichern) : null),
            Farbe.Tiefe(0.2), 14, 12, 12);
    }

    private static FrameworkElement Editor(TextFeld feld)
    {
        Loese(feld);
        feld.MinHeight = 70;
        return Ui.Box(feld, Farbe.Tiefe(0.25), 10, 6, 6);
    }

    private UIElement Groq() => Ui.Box(Ui.V(10,
        einst.Zugang.Groq.Length == 0 ? Ui.Tx("Kein Groq-Schlüssel eingetragen (Einstellungen → Konten).", 12, FontWeights.Medium, Farbe.Warnung) : null,
        Ui.Tx("Whisper-Modell", 13, FontWeights.SemiBold),
        Ui.H(8,
            ChipWahl("Large V3 Turbo (schnell)", tk.WhisperModell != TranskriptionsEinstellungen.ModellLargeV3, () =>
            {
                tk.WhisperModell = TranskriptionsEinstellungen.ModellTurbo;
                GroqWhisper.SperreAufheben();
            }),
            ChipWahl("Large V3 (genauer)", tk.WhisperModell == TranskriptionsEinstellungen.ModellLargeV3, () =>
            {
                tk.WhisperModell = TranskriptionsEinstellungen.ModellLargeV3;
                GroqWhisper.SperreAufheben();
            })),
        Ui.Tx("Large V3 muss in der Groq-Konsole für deine Organisation freigeschaltet sein; sonst nimmt Jarvis wie am Handy Turbo"
              + (GroqWhisper.LargeV3Gesperrt ? " (gerade gesperrt, derzeit Turbo)." : "."), 11, null, Farbe.Text3),
        Ui.Tx("Persönliches Wörterbuch", 13, FontWeights.SemiBold).Rand(0, 4, 0, 0),
        Chip("Wörterbuch mitsenden", () => tk.WoerterbuchAn, v => tk.WoerterbuchAn = v),
        tk.WoerterbuchAn ? Ui.Tx("Begriffe (durch Komma oder Zeilenumbruch getrennt)", 11, null, Farbe.Text2) : null,
        tk.WoerterbuchAn ? Editor(begriffe) : null,
        tk.WoerterbuchAn ? Loese(begriffeSpeichern) : null),
        Farbe.Tiefe(0.2), 14, 12, 12);
}
