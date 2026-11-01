using System.Globalization;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Threading;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Mehrere Aufgaben in einem Text: Trenner ist genau „ ; “ (Leerzeichen, Semikolon, Leerzeichen). Andere Semikolons gehören zum Text.</summary>
public static class AufgabenText
{
    public const string Trenner = " ; ";

    /// <summary>Zwei Texte hintereinander, ohne Trenner hinzuzufügen, wo der erste schon damit endet.</summary>
    public static string Verbinde(string a, string b)
    {
        if (a.Length == 0) return b;
        if (b.Length == 0) return a;
        return a.EndsWith(' ') ? a + b : a + " " + b;
    }

    /// <summary>Hängt eine neue Einsprechung an: Zwischen den Einsprechungen und am Ende steht immer der Trenner.</summary>
    public static string Anhaengen(string basis, string neu)
    {
        var n = OhneEndTrenner(neu);
        if (n.Length == 0) return basis;
        var b = OhneEndTrenner(basis);
        // Ein bloßer Vorspann ist keine Aufgabe: Das Gesagte gehört direkt dahinter.
        if (ProgrammierVorspann.IstBlosserEinstieg(b)) return b + " " + n + Trenner;
        return (b.Length == 0 ? "" : b + Trenner) + n + Trenner;
    }

    private static string OhneEndTrenner(string t)
    {
        var s = t.Trim();
        while (s == ";" || s.EndsWith(" ;", StringComparison.Ordinal)) s = s[..^1].Trim();
        return s;
    }

    /// <summary>Verbessert jede Aufgabe einzeln und setzt die Trenner wieder dazwischen, auch am Ende, wenn er dort stand.
    /// Misslingt eine Aufgabe, bleibt ihr Text unverändert; zurück kommt, wie viele das waren.</summary>
    public static async Task<(string Text, int Fehlgeschlagen)> Verbessern(string text)
    {
        var getrimmt = text.Trim();
        var hatEndTrenner = getrimmt == ";" || getrimmt.EndsWith(" ;", StringComparison.Ordinal);
        var teile = OhneEndTrenner(getrimmt).Split(Trenner).Select(x => x.Trim()).Where(x => x.Length > 0).ToList();
        if (teile.Count == 0) return (text, 0);
        var fertig = teile.ToList();
        var fehlgeschlagen = 0;
        // Höchstens vier gleichzeitig: Ein Prompt mit dreißig Aufgaben liefe sonst in die Ratenbegrenzung.
        for (var start = 0; start < teile.Count; start += 4)
        {
            var aufgaben = Enumerable.Range(start, Math.Min(4, teile.Count - start)).Select(async i => (i, await Kurzauftrag.Frage(teile[i], system: Anweisung))).ToList();
            foreach (var (i, antwort) in await Task.WhenAll(aufgaben))
            {
                // Das Modell darf aus einer Aufgabe nie zwei machen: ein versehentlicher Trenner im Ergebnis wird zum gewöhnlichen Semikolon.
                var a = OhneEndTrenner(antwort ?? "").Replace(Trenner, "; ");
                if (a.Length == 0) fehlgeschlagen++; else fertig[i] = a;
            }
        }
        return (string.Join(Trenner, fertig) + (hatEndTrenner ? Trenner : ""), fehlgeschlagen);
    }

    private const string Anweisung =
        "Du bringst genau eine Aufgabe aus einem Prompt von Frank in gutes Deutsch. Ein Prompt ist ein vorformulierter Auftrag, den Frank später an seinen Assistenten Jarvis "
        + "(oft weiter an einen Programmierer) schickt. Frank hat ihn eingesprochen oder eingetippt. Erfasse den Inhalt sinngemäß und schreibe ihn als klaren, vollständigen, "
        + "für sich allein verständlichen Auftrag. Behalte jeden inhaltlichen Punkt, jeden Namen, jede Zahl und jede Anforderung, füge nichts hinzu und deute nichts um. "
        + "Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg und berichtige Hörfehler der Spracherkennung. Absätze oder Aufzählungen nur, wenn sie den Auftrag klarer machen. "
        + "Führe den Auftrag nicht aus. Die übrigen Aufgaben des Prompts werden getrennt bearbeitet: Gib genau diese eine Aufgabe zurück und verwende nie die Zeichenfolge Leerzeichen, Semikolon, Leerzeichen. "
        + "Antworte nur mit dem fertigen Text, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung.";
}

/// <summary>Die festen Vorspänne der schwebenden Knöpfe, wörtlich wie am Handy (ProgrammiererVorlage.kt, NACHTRAG_VORSPANN).</summary>
public static class ProgrammierVorspann
{
    public const string Auftrag = "Übergib bitte folgende Aufgabe an den Programmierer:";
    public const string Nachtrag = "Nachtrag:";

    /// <summary>Die vier Modelle des Programmierknopfs, wie am Handy (ProgrammiererVorlage.kt). Handy geht an `programmieren`, Mac (dieser PC) an `openlauncher_sitzung`, Clis sind die wählbaren CLIs.</summary>
    public sealed class Modell
    {
        public string Label { get; }
        public string Vorspann { get; }
        public IReadOnlyDictionary<string, string> Handy { get; }
        public IReadOnlyDictionary<string, string> Mac { get; }
        public IReadOnlyList<string> Clis { get; }
        public string Id => Label;
        public Modell(string label, Dictionary<string, string> handy, Dictionary<string, string> mac, string[] clis)
        {
            Label = label;
            Vorspann = $"Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell {label}:";
            Handy = handy; Mac = mac; Clis = clis;
        }
    }

    private static readonly Dictionary<string, string> Standard = new() { ["effort"] = "high", ["profil"] = "minimal", ["modus"] = "schnell" };
    private static Dictionary<string, string> MitStandard(Dictionary<string, string> d)
    {
        var r = new Dictionary<string, string>(d);
        foreach (var (k, v) in Standard) r.TryAdd(k, v);
        return r;
    }

    public static readonly IReadOnlyList<Modell> Modelle = new[]
    {
        new Modell("Opus 5.5", new() { ["modell"] = "opus" }, MitStandard(new() { ["modell"] = "claude-opus-5-5[1m]", ["cli"] = "claude" }), new[] { "claude" }),
        new Modell("Sol 6.1", new() { ["modell"] = "sol" }, MitStandard(new() { ["modell"] = "gpt-6.1-sol", ["anbieter"] = "openai", ["cli"] = "codex" }), new[] { "codex", "opencode" }),
        new Modell("Kimi K3.1 Pro", MitStandard(new() { ["modell"] = "Kimi K3.1 Pro", ["cli"] = "opencode" }),
            MitStandard(new() { ["modell"] = "k3.1-pro", ["anbieter"] = "kimi-code-plan-global", ["cli"] = "opencode" }), new[] { "opencode", "kimi" }),
        new Modell("Gemini 4 Argon", MitStandard(new() { ["modell"] = "Gemini 4 Argon", ["cli"] = "antigravity" }),
            MitStandard(new() { ["modell"] = "gemini-4-argon", ["anbieter"] = "google", ["cli"] = "antigravity" }), new[] { "antigravity", "opencode" }),
    };
    private static readonly List<string> AlleAuftraege = new[] { Auftrag }.Concat(Modelle.Select(m => m.Vorspann)).ToList();

    /// <summary>Das per Knopf im Einstieg gewählte Modell; null ohne Modell im Einstieg.</summary>
    public static Modell? ModellIn(string t) { var v = Vorn(t); return Modelle.FirstOrDefault(m => v.StartsWith(m.Vorspann, StringComparison.Ordinal)); }
    public static bool IstBlosserEinstieg(string t) => AlleAuftraege.Contains(t) || t == Nachtrag;

    /// <summary>Wie am Handy: Musste Jarvis erst nachfragen, gilt das Modell der vorigen Nachricht weiter, höchstens 10 Minuten und nur, solange nichts gestartet ist.</summary>
    public static string? OffeneWahl(IEnumerable<Nachricht> nachrichten)
    {
        var n = nachrichten.ToList();
        var i = n.FindLastIndex(x => x.Rolle == Rolle.Frank);
        if (i < 0 || ModellIn(n[i].Text) == null || (DateTime.Now - n[i].Zeit).TotalSeconds >= 600) return null;
        if (n.Skip(i + 1).Any(x => x.Schritte.Any(s => s.Werkzeug is "programmieren" or "openlauncher_sitzung"))) return null;
        return n[i].Text;
    }

    private static bool CliGenannt(string cli, string t)
    {
        var muster = cli switch
        {
            "kimi" => @"kimi\s*(code\s*)?cli", "opencode" => @"open\s*code", "antigravity" => @"antigravity|\bagy\b", "codex" => "codex", "claude" => @"claude\s*code", _ => null,
        };
        return muster != null && Regex.IsMatch(t, muster, RegexOptions.IgnoreCase);
    }

    /// <summary>Der Knopf gilt verbindlich: Modell immer; CLI, Effort, Profil und Modus nur, solange Frank sie nicht selbst nennt.</summary>
    public static JSONObjekt Verbindlich(string werkzeug, JSONObjekt args, string frage, string? vorige)
    {
        var m = ModellIn(frage) ?? (HatAuftrag(frage) || vorige == null ? null : ModellIn(vorige));
        if (m == null) return args;
        IReadOnlyDictionary<string, string> fest;
        switch (werkzeug)
        {
            case "programmieren": fest = m.Handy; break;
            case "openlauncher_sitzung": fest = m.Mac; break;
            default: return args;
        }
        var neu = new JSONObjekt(args);
        var worte = frage + "\n" + (vorige ?? "");
        foreach (var (k, v) in fest)
        {
            var alt = (args.Text(k) ?? "").Trim();
            if (Standard.ContainsKey(k) && alt.Length > 0) continue;
            if (k == "cli" && m.Clis.Contains(alt) && CliGenannt(alt, worte)) continue;
            neu[k] = v;
        }
        return neu;
    }

    private static string Vorn(string t) => t.TrimStart();
    private static string Ohne(string vorspann, string t)
    {
        var v = Vorn(t);
        return v.StartsWith(vorspann, StringComparison.Ordinal) ? Vorn(v[vorspann.Length..]) : t;
    }

    public static bool HatAuftrag(string t) { var v = Vorn(t); return AlleAuftraege.Any(x => v.StartsWith(x, StringComparison.Ordinal)); }
    public static bool HatNachtrag(string t) => Vorn(t).StartsWith(Nachtrag, StringComparison.Ordinal);
    public static string OhneAuftrag(string t)
    {
        var v = Vorn(t);
        var a = AlleAuftraege.FirstOrDefault(x => v.StartsWith(x, StringComparison.Ordinal));
        return a != null ? Ohne(a, t) : t;
    }
    public static string OhneNachtrag(string t) => Ohne(Nachtrag, t);
    public static string MitAuftrag(string t, string vorspann = Auftrag) => $"{vorspann} {t.Trim()}";
    public static string MitNachtrag(string t) => $"{Nachtrag} {Vorn(t)}";
}

/// <summary>„Ich höre zu …“ mit Pegel, danach „Schreibe mit …“; für die Blase und jedes Textfeld mit Diktat.</summary>
public sealed class DiktatStatus : LebendeAnsicht
{
    private readonly double groesse;
    private readonly int zeilen;
    private readonly Diktat diktat = Diktat.Shared;

    public DiktatStatus() : this(12, 1) { }
    public DiktatStatus(double groesse, int zeilen)
    {
        this.groesse = groesse;
        this.zeilen = zeilen;
        // Den Pegel holt sich der Balken selbst: Sonst entstünde die Zeile bei jeder Messung neu.
        Beobachte(diktat, nameof(Diktat.SchreibtMit), nameof(Diktat.Vorschau));
    }

    protected override UIElement Baue()
    {
        if (diktat.SchreibtMit) return T("Schreibe mit …", groesse, null, Farbe.Text2, zeilen);
        return H(8, new PegelBalken(() => diktat.Pegel), Fuell(T(diktat.Vorschau.Length == 0 ? "Ich höre zu …" : diktat.Vorschau, groesse, null, Farbe.Text2, zeilen)));
    }
}

/// <summary>Mikrofon für ein Textfeld: Jede Einsprechung endet mit dem Trenner, damit die nächste folgen kann.</summary>
public sealed class DiktatKnopf : LebendeAnsicht
{
    private readonly TextBindung text;
    /// <summary>Pegel und erkannter Text neben dem Knopf; die Eingabeblase zeigt sie selbst darüber.</summary>
    private readonly bool vorschau;
    private readonly Action<bool> meldeLaeuft;
    private readonly Action<string> meldeFehler;
    private readonly Diktat diktat = Diktat.Shared;
    private readonly SprachSchleife schleife = SprachSchleife.Shared;
    private bool laeuft;
    private string meldung = "";
    private bool ansichtFort;
    private DiktatStatus? status;

    public DiktatKnopf(TextBindung text, bool vorschau = true, Action<bool>? meldeLaeuft = null, Action<string>? meldeFehler = null)
    {
        this.text = text;
        this.vorschau = vorschau;
        this.meldeLaeuft = meldeLaeuft ?? (_ => { });
        this.meldeFehler = meldeFehler ?? (_ => { });
        Beobachte(diktat, nameof(Diktat.Aktiv), nameof(Diktat.SchreibtMit), nameof(Diktat.OffeneAufnahme));
        Beobachte(schleife, nameof(SprachSchleife.Zustand));
    }

    /// <summary>Wie am Handy: rot mit Stopp, solange aufgenommen wird, auch die laufende Runde des Gesprächsmodus.</summary>
    private bool NimmtAuf => diktat.Aktiv && !diktat.SchreibtMit && (laeuft || schleife.Zustand == SprachZustand.Hoert);

    protected override void Erschienen() => ansichtFort = false;
    protected override void Verschwunden() { ansichtFort = true; if (laeuft) diktat.Stoppen(); }

    protected override UIElement Baue()
    {
        var teile = new List<UIElement?>();
        if (laeuft && diktat.SchreibtMit)
        {
            var d = Drehkreis();
            d.Margin = new Thickness(9);
            d.ToolTip = "Schreibe mit …";
            teile.Add(d);
        }
        else
            teile.Add(Rund(NimmtAuf ? "stop.fill" : "mic.fill", Druecken, NimmtAuf ? "Aufnahme beenden" : "Einsprechen", NimmtAuf, Farbe.Rot));
        if (diktat.OffeneAufnahme != null && !diktat.Aktiv)
            teile.Add(Rund("arrow.clockwise", ErneutUmwandeln, "Aufnahme erneut in Text umwandeln", false, Farbe.Warnung));
        if (vorschau && laeuft && diktat.Aktiv) teile.Add(status ??= new DiktatStatus(12, 1));
        if (vorschau && meldung.Length > 0) teile.Add(T(meldung, 12, null, Farbe.Warnung, 2));
        return H(8, teile.ToArray());
    }

    private async void Druecken()
    {
        if (laeuft) { diktat.Stoppen(); return; }
        // Im Gesprächsmodus gehört das Mikrofon der Schleife: Tippen beendet die Aufnahme oder unterbricht Jarvis, und es geht in der nächsten Runde weiter.
        switch (schleife.Zustand)
        {
            case SprachZustand.Hoert: if (diktat.Aktiv) diktat.Stoppen(); return;
            case SprachZustand.Spricht: Stimme.Shared.Stopp(); return;
            case SprachZustand.Denkt: Melde_("Jarvis antwortet gerade."); return;
        }
        // Ein Diktat, das jemand anderes gestartet hat (Sprachgespräch, Chat), bleibt unangetastet.
        if (diktat.Aktiv) { Melde_("Das Mikrofon wird gerade schon benutzt."); return; }
        laeuft = true; Melde_(""); meldeLaeuft(true); Neu();
        var erkannt = await diktat.HoereBisStille();
        if (erkannt != null) Uebernehmen(erkannt);
        else if (diktat.Fehler is { } f) Melde_(f);
        laeuft = false; meldeLaeuft(false); Neu();
    }

    private async void ErneutUmwandeln()
    {
        laeuft = true; Melde_(""); meldeLaeuft(true); Neu();
        var erkannt = await diktat.ErneutUmwandeln();
        if (erkannt != null) Uebernehmen(erkannt);
        else if (diktat.Fehler is { } f) Melde_(f);
        laeuft = false; meldeLaeuft(false); Neu();
    }

    private void Uebernehmen(string erkannt)
    {
        if (ansichtFort)
        {
            var s = AppSteuerung.Shared;
            s.EingabeText = AufgabenText.Anhaengen(s.EingabeText, erkannt);
        }
        else text.Wert = AufgabenText.Anhaengen(text.Wert, erkannt);
    }

    private void Melde_(string t) { meldung = t; meldeFehler(t); Neu(); }
}

/// <summary>Mikrofon, KI-Verbesserung je Aufgabe und Rückgängig für einen Prompt.</summary>
public sealed class PromptWerkzeuge : UserControl
{
    private readonly TextBindung text;
    private string? vorher, eigenerStand;
    private bool arbeitet;
    private string meldung = "";
    private readonly ContentControl rest = new();

    public PromptWerkzeuge(TextBindung text)
    {
        this.text = text;
        Action geaendert = () =>
        {
            // Rückgängig gilt nur für den Stand direkt nach der KI; danach würde es spätere Änderungen löschen.
            if (vorher != null && text.Wert != eigenerStand) { vorher = null; Zeichne(); }
        };
        Loaded += (_, _) => { text.Geaendert -= geaendert; text.Geaendert += geaendert; };
        Unloaded += (_, _) => text.Geaendert -= geaendert;
        Content = H(10, new DiktatKnopf(text), rest, Spacer());
        Zeichne();
    }

    private void Zeichne()
    {
        rest.Content = H(10,
            Kapsel("sparkles", arbeitet ? "Jarvis verbessert …" : "Mit KI verbessern", Verbessern),
            arbeitet ? Drehkreis() : null,
            vorher != null ? Kapsel("arrow.uturn.backward", "Rückgängig", () => { var v = vorher!; vorher = null; text.Wert = v; Zeichne(); }) : null,
            meldung.Length > 0 ? T(meldung, 12, null, Farbe.Warnung) : null);
    }

    private async void Verbessern()
    {
        if (arbeitet || text.Wert.Trim().Length == 0) return;
        var alt = text.Wert;
        arbeitet = true; meldung = ""; Zeichne();
        var (neu, fehlgeschlagen) = await AufgabenText.Verbessern(alt);
        arbeitet = false;
        if (fehlgeschlagen > 0) meldung = $"{fehlgeschlagen} Aufgabe(n) konnten nicht verbessert werden und blieben unverändert.";
        // Hat Frank währenddessen weitergeschrieben oder diktiert, bleibt sein Text stehen.
        if (text.Wert != alt)
        {
            meldung = "Du hast währenddessen weitergeschrieben, deshalb bleibt dein Text unverändert.";
            Log("ablage", "Prompt-Verbesserung verworfen, Text wurde währenddessen geändert", wo: "Ablage/Prompts");
        }
        else if (neu != alt)
        {
            vorher = alt; eigenerStand = neu; text.Wert = neu;
            vorher = alt;   // Die Änderungsmeldung eben darf das Rückgängig nicht löschen.
            Log("ablage", $"Prompt verbessert ({fehlgeschlagen} Aufgaben unverändert)", wo: "Ablage/Prompts");
        }
        Zeichne();
    }
}

/// <summary>Einen Prompt bearbeiten: deckt das ganze Fenster ab und schaltet es für diese Zeit auf maximiert ohne Rahmen.</summary>
public sealed class PromptEditorVollbild : UserControl
{
    private readonly PromptEintrag prompt;
    private readonly AppSteuerung steuerung = AppSteuerung.Shared;
    private string text;
    private string fehler = "";
    private bool speichert;
    /// <summary>Der Prompt wurde inzwischen geändert oder gelöscht: Der nächste Klick auf Speichern legt ihn neu an.</summary>
    private bool alsNeu;
    private readonly TextBlock zaehler = T("", 12, null, Farbe.Text3);
    private readonly TextBlock fehlerText = T("", 13, null, Farbe.Warnung);
    private readonly ContentControl speicherPlatz = new();
    private (WindowStyle Stil, WindowState Zustand, ResizeMode Groesse, Window Fenster)? vorher;
    private TextBox? feld;

    public PromptEditorVollbild() : this(AppSteuerung.Shared.PromptBearbeiten ?? new PromptEintrag(0, "", "")) { }

    public PromptEditorVollbild(PromptEintrag prompt)
    {
        this.prompt = prompt;
        text = prompt.Text;
        Background = B(Farbe.Grund);
        var bindung = new TextBindung(() => text, v => { text = v; Zaehle(); });

        fehlerText.Visibility = Visibility.Collapsed;
        BaueSpeichern();
        Zaehle();

        var editorFeld = Feld(bindung, "", 17, mehrzeilig: true, fertig: b => feld = b);
        var rahmen = Flaeche(editorFeld, Farbe.Glas(0.05), 16, 14, 14);

        var g = new Grid { Margin = new Thickness(36, 40, 36, 28) };
        foreach (var _ in Enumerable.Range(0, 3)) g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        var kopf = H(12,
            Fuell(T(prompt.Nummer > 0 ? $"Prompt {prompt.Nummer} bearbeiten" : "Neuer Prompt", 24, FontWeights.Bold)),
            zaehler,
            Kapsel("xmark", "Schließen", () => { if (Geaendert) { if (Bestaetigen("Deine Änderungen an diesem Prompt verwerfen?", "", "Verwerfen")) Schliessen(); } else Schliessen(); }),
            speicherPlatz);
        var werkzeuge = Rand(new PromptWerkzeuge(bindung), o: 14);
        Grid.SetRow(werkzeuge, 1);
        Rand(fehlerText, o: 14);
        Grid.SetRow(fehlerText, 2);
        Rand(rahmen, o: 14);
        Grid.SetRow(rahmen, 3);
        g.Children.Add(kopf); g.Children.Add(werkzeuge); g.Children.Add(fehlerText); g.Children.Add(rahmen);
        Content = g;

        PreviewKeyDown += (_, e) =>
        {
            if (e.Key == Key.S && Keyboard.Modifiers == ModifierKeys.Control) { Speichern(); e.Handled = true; }
        };
        Loaded += (_, _) => { Vollbild(true); Dispatcher.BeginInvoke(DispatcherPriority.Input, () => feld?.Focus()); };
        Unloaded += (_, _) => Vollbild(false);
    }

    private bool Geaendert => prompt.Nummer > 0 && text != prompt.Text;

    private void Zaehle() => zaehler.Text = $"{new StringInfo(text).LengthInTextElements} Zeichen";
    private void BaueSpeichern() => speicherPlatz.Content = Kapsel("square.and.arrow.down.fill", speichert ? "Speichert …" : "Speichern", Speichern, hervorgehoben: true);

    private void ZeigeFehler(string f)
    {
        fehler = f;
        fehlerText.Text = f;
        fehlerText.Visibility = f.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    /// <summary>Ein neuer Prompt geht ohne Speichern zurück ins Feld unter „Neuer Prompt“, nichts davon ist verloren.</summary>
    private void Schliessen(bool gespeichert = false)
    {
        if (prompt.Nummer == 0) steuerung.PromptEntwurf = gespeichert ? "" : text;
        steuerung.PromptBearbeiten = null;
    }

    private async void Speichern()
    {
        if (speichert) return;
        speichert = true; ZeigeFehler(""); BaueSpeichern();
        try
        {
            var abgleich = AblageAbgleich.Shared;
            if (prompt.Nummer > 0 && !alsNeu)
            {
                if (abgleich.Prompts.FirstOrDefault(p => p.Erstellt == prompt.Erstellt)?.Text != prompt.Text)
                {
                    ZeigeFehler("Dieser Prompt wurde inzwischen geändert oder gelöscht. Speichern legt deine Fassung als neuen Prompt an.");
                    alsNeu = true;
                    return;
                }
                await abgleich.PromptAendern(prompt.Erstellt, text);
            }
            else await abgleich.PromptSpeichern(text);
            Schliessen(gespeichert: true);
        }
        catch (Exception ex) { ZeigeFehler(ex.Message); }
        finally { speichert = false; BaueSpeichern(); }
    }

    /// <summary>Fenster maximiert ohne Rahmen; beim Schließen zurück auf den vorherigen Zustand.</summary>
    private void Vollbild(bool an)
    {
        if (an)
        {
            var w = Window.GetWindow(this);
            if (w == null || vorher != null) return;
            vorher = (w.WindowStyle, w.WindowState, w.ResizeMode, w);
            // Nur aus „Normal“ heraus deckt ein rahmenloses Fenster auch die Taskleiste ab.
            w.WindowState = WindowState.Normal;
            w.WindowStyle = WindowStyle.None;
            w.ResizeMode = ResizeMode.NoResize;
            w.WindowState = WindowState.Maximized;
        }
        else if (vorher is { } v)
        {
            vorher = null;
            v.Fenster.WindowState = WindowState.Normal;
            v.Fenster.WindowStyle = v.Stil;
            v.Fenster.ResizeMode = v.Groesse;
            v.Fenster.WindowState = v.Zustand;
        }
    }
}
