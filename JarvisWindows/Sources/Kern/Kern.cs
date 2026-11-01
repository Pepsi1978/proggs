global using JSONObjekt = System.Collections.Generic.Dictionary<string, object?>;
global using static JarvisWindows.Globale;

using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Windows;
using System.Windows.Threading;

namespace JarvisWindows;

/// <summary>JSON wie JSONSerialization in der Mac-Fassung: JSONObjekt = Dictionary, Arrays = List&lt;object?&gt;, Zahlen long oder double. Schlüssel immer sortiert, damit der Prompt-Cache trägt.</summary>
public static class JSON
{
    private static readonly JsonWriterOptions Knapp = new() { Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping };
    private static readonly JsonWriterOptions Schoen = new() { Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping, Indented = true };

    private static void Schreibe(Utf8JsonWriter w, object? o)
    {
        switch (o)
        {
            case null: w.WriteNullValue(); break;
            case string s: w.WriteStringValue(s); break;
            case bool b: w.WriteBooleanValue(b); break;
            case int i: w.WriteNumberValue(i); break;
            case long l: w.WriteNumberValue(l); break;
            case double d: w.WriteNumberValue(d); break;
            case float f: w.WriteNumberValue(f); break;
            case decimal m: w.WriteNumberValue(m); break;
            case JsonElement e: e.WriteTo(w); break;
            case IDictionary<string, object?> dict:
                w.WriteStartObject();
                foreach (var k in dict.Keys.OrderBy(x => x, StringComparer.Ordinal)) { w.WritePropertyName(k); Schreibe(w, dict[k]); }
                w.WriteEndObject();
                break;
            case System.Collections.IDictionary dict2:
                w.WriteStartObject();
                foreach (var k in dict2.Keys.Cast<object>().Select(x => x.ToString()!).OrderBy(x => x, StringComparer.Ordinal)) { w.WritePropertyName(k); Schreibe(w, dict2[k]); }
                w.WriteEndObject();
                break;
            case System.Collections.IEnumerable liste:
                w.WriteStartArray();
                foreach (var x in liste) Schreibe(w, x);
                w.WriteEndArray();
                break;
            default: w.WriteStringValue(o.ToString()); break;
        }
    }

    public static byte[] Daten(object? wert)
    {
        using var m = new MemoryStream();
        using (var w = new Utf8JsonWriter(m, Knapp)) Schreibe(w, wert);
        return m.ToArray();
    }

    public static string Text(object? wert, bool schoen = false)
    {
        using var m = new MemoryStream();
        using (var w = new Utf8JsonWriter(m, schoen ? Schoen : Knapp)) Schreibe(w, wert);
        return Encoding.UTF8.GetString(m.ToArray());
    }

    private static object? Wandle(JsonElement e) => e.ValueKind switch
    {
        JsonValueKind.String => e.GetString(),
        JsonValueKind.True => true,
        JsonValueKind.False => false,
        JsonValueKind.Null or JsonValueKind.Undefined => null,
        JsonValueKind.Number => e.TryGetInt64(out var l) ? l : e.GetDouble(),
        JsonValueKind.Array => e.EnumerateArray().Select(Wandle).ToList(),
        _ => e.EnumerateObject().ToDictionary(p => p.Name, p => Wandle(p.Value)),
    };

    public static object? Wert(ReadOnlySpan<byte> daten)
    {
        try
        {
            if (daten.Length >= 3 && daten[0] == 0xEF && daten[1] == 0xBB && daten[2] == 0xBF) daten = daten[3..];
            var r = new Utf8JsonReader(daten, new JsonReaderOptions { AllowTrailingCommas = true, CommentHandling = JsonCommentHandling.Skip });
            return Wandle(JsonElement.ParseValue(ref r));
        }
        catch { return null; }
    }

    public static JSONObjekt? Objekt(byte[] daten) => Wert(daten) as JSONObjekt;
    public static JSONObjekt? Objekt(string text) => Wert(Encoding.UTF8.GetBytes(text)) as JSONObjekt;
}

/// <summary>Bequemes Lesen, entspricht `o["k"] as? String` usw. in Swift.</summary>
public static class JsonLesen
{
    public static string? Text(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v as string : null;
    public static long? Ganz(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v switch { long l => l, int i => i, double d => (long)d, _ => null } : null;
    public static double? Zahl(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v switch { long l => l, int i => i, double d => d, float f => f, _ => null } : null;
    public static bool? Wahr(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v as bool? : null;
    public static JSONObjekt? Obj(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v as JSONObjekt : null;
    public static List<object?>? Liste(this JSONObjekt o, string k) => o.TryGetValue(k, out var v) ? v as List<object?> : null;
    /// <summary>`o["k"] as? [JSONObjekt]`: fehlend = leer.</summary>
    public static List<JSONObjekt> Objekte(this JSONObjekt o, string k) => o.Liste(k)?.OfType<JSONObjekt>().ToList() ?? (o.TryGetValue(k, out var v) && v is List<JSONObjekt> l ? l : new());
    public static List<string> Texte(this JSONObjekt o, string k) => o.Liste(k)?.OfType<string>().ToList() ?? (o.TryGetValue(k, out var v) && v is List<string> l ? l : new());
}

public static class Pfade
{
    public static readonly string Heim = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
    public static readonly string Proggs = Path.Combine(Heim, "proggs");
    private static string Ordner(string p) { Directory.CreateDirectory(p); return p; }
    /// <summary>%APPDATA%\JarvisWindows, entspricht Application Support/JarvisMac.</summary>
    public static readonly string Unterstuetzung = Ordner(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "JarvisWindows"));
    public static string Gespraeche => Ordner(Path.Combine(Unterstuetzung, "Gespraeche"));
    public static string Temp => Ordner(Path.Combine(Path.GetTempPath(), "JarvisWindows"));
    public static string Sk(string teil) => Path.Combine(Heim, "SK", teil.Replace('/', Path.DirectorySeparatorChar));
    public static string Ressource(string name) => Path.Combine(AppContext.BaseDirectory, "Resources", name);
    /// <summary>Eine mitgelieferte Markdown-Datei (Bundle.main.mitgeliefert).</summary>
    public static string Mitgeliefert(string name)
    {
        try { return File.ReadAllText(Ressource(name + ".md")); } catch { return name + ".md fehlt im Programmordner."; }
    }
}

public static class AppInfo
{
    /// <summary>Kennung beim Relay und Name des Rechners. Ein weiterer Windows-Rechner braucht nur andere Werte.</summary>
    public const string Geraet = "hp";
    public const string Geraetename = "HP";
    public const string Name = "Jarvis Windows " + Geraetename;
    public static string Version => (Assembly.GetEntryAssembly()?.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion ?? "dev").Split('+')[0];
    public static string Stand => Assembly.GetEntryAssembly()?.GetCustomAttributes<AssemblyMetadataAttribute>().FirstOrDefault(a => a.Key == "BuildTimestamp")?.Value ?? "";
}

/// <summary>Hauptfaden. Swift @MainActor entspricht: Aufruf und Fortsetzung auf dem WPF-Dispatcher.</summary>
public static class Haupt
{
    public static Dispatcher? D => Application.Current?.Dispatcher;
    public static void Post(Action a)
    {
        var d = D;
        if (d == null || d.CheckAccess()) a(); else d.BeginInvoke(a);
    }
    public static Task Auf(Func<Task> f)
    {
        var d = D;
        return d == null || d.CheckAccess() ? f() : d.InvokeAsync(f).Task.Unwrap();
    }
    public static Task<T> Auf<T>(Func<Task<T>> f)
    {
        var d = D;
        return d == null || d.CheckAccess() ? f() : d.InvokeAsync(f).Task.Unwrap();
    }
    public static T Wert<T>(Func<T> f)
    {
        var d = D;
        return d == null || d.CheckAccess() ? f() : d.Invoke(f);
    }
}

/// <summary>Entspricht @Observable: Eigenschaften melden über PropertyChanged; WPF-Bindungen und Ansichten hängen sich daran.</summary>
public abstract class Beobachtbar : INotifyPropertyChanged
{
    public event PropertyChangedEventHandler? PropertyChanged;
    protected void Melde([CallerMemberName] string? name = null) => Haupt.Post(() => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name)));
    protected bool Setze<T>(ref T feld, T wert, [CallerMemberName] string? name = null)
    {
        if (EqualityComparer<T>.Default.Equals(feld, wert)) return false;
        feld = wert;
        Melde(name);
        return true;
    }
}

public sealed class WeiteresKonto
{
    public string Titel { get; set; } = "";
    public string Beschreibung { get; set; } = "";
    public string Wert { get; set; } = "";
    public string Dienst { get; set; } = "";
    public bool Gesperrt { get; set; }
}

/// <summary>Zugänge (Schlüssel). Liegen verschlüsselt im Tresor (Tresor.cs), nie im Klartext, nie im Git.</summary>
public sealed class Zugang
{
    public string Anthropic { get; set; } = "";
    public string Openai { get; set; } = "";
    public string Gemini { get; set; } = "";
    public string Google { get; set; } = "";   // Google Cloud TTS (Chirp 3 HD)
    public string Groq { get; set; } = "";
    public string Relay { get; set; } = "";    // MCP-Relay des Handy-Jarvis (https://…/j/<token>/mcp)
    /// <summary>Zusätzliche Schlüssel für Helfer und neue Anwendungen, Kennung → Eintrag.</summary>
    public Dictionary<string, WeiteresKonto> Weitere { get; set; } = new();
    /// <summary>Feste Einträge, die Helfer nicht benutzen dürfen (eingebaute Funktionen arbeiten weiter).</summary>
    public List<string> GesperrtFuerHelfer { get; set; } = new();

    public static Zugang Laden()
    {
        var z = Tresor.Lade<Zugang>("zugang") ?? new Zugang();
        var vorher = JsonSerializer.Serialize(z);
        z.ErgaenzeAusSK();
        if (JsonSerializer.Serialize(z) != vorher) z.Speichern();
        z.GeheimnisseMelden();
        return z;
    }

    /// <summary>Erstbefüllung aus ~/SK (Regel 1: Zugangsdaten liegen dort). Vorhandene Werte bleiben.</summary>
    public void ErgaenzeAusSK()
    {
        try
        {
            var o = JSON.Objekt(File.ReadAllBytes(Pfade.Sk("VoiceAgent/keys.json")));
            if (o != null)
            {
                string S(string k) => (o.Text(k) ?? "").Trim();
                if (Anthropic.Length == 0) Anthropic = S("claude");
                if (Openai.Length == 0) Openai = S("openai");
                if (Gemini.Length == 0) Gemini = S("gemini");
                if (Google.Length == 0) Google = S("google");
                if (Groq.Length == 0) Groq = S("groq");
            }
        }
        catch { }
        if (Relay.Length == 0)
        {
            try { Relay = File.ReadAllText(Pfade.Sk("Jarvis/plugin-url.txt")).Trim(); } catch { }
        }
    }

    public void Speichern()
    {
        Tresor.Sichere(this, "zugang");
        GeheimnisseMelden();
    }

    private void GeheimnisseMelden()
    {
        var token = Relay.Split('/', StringSplitOptions.RemoveEmptyEntries).LastOrDefault(t => t != "mcp") ?? "";
        Geheimnisse.Setzen("zugang", new[] { Anthropic, Openai, Gemini, Google, Groq, Relay, token }.Concat(Weitere.Values.Select(w => w.Wert)));
    }
}

public sealed class LogEintrag
{
    public Guid Id { get; } = Guid.NewGuid();
    public DateTime Zeit { get; set; } = DateTime.Now;
    public string Art { get; set; } = "";     // fehler, werkzeug, gehirn, sprache, live, system, oberflaeche, gespraech, einstellung, tresor, ablage, gedaechtnis
    public string Text { get; set; } = "";
    public string? Wo, Werkzeug, Eingabe, Ergebnis, Fehler;
    public int? DauerMs;
    public string Anzeige
    {
        get
        {
            var t = Text;
            if (Wo != null) t += " · " + Wo;
            if (DauerMs != null) t += $" · {DauerMs} ms";
            if (Fehler != null) t += "\nFehler: " + Fehler;
            else if (Ergebnis != null) t += "\n" + (Ergebnis.Length > 300 ? Ergebnis[..300] : Ergebnis);
            return t;
        }
    }
}

/// <summary>Ausführliches Logbuch: je Tag eine Datei mit einer JSON-Zeile je Ereignis. Höchstens 30 Tage, Schlüssel werden vor dem Schreiben geschwärzt.</summary>
public sealed class Logbuch : Beobachtbar
{
    public const int AufbewahrungTage = 30;
    public static readonly string Ordner = Path.Combine(Pfade.Unterstuetzung, "Logbuch");
    // Nach Ordner: Statische Felder entstehen in Textreihenfolge, und der Konstruktor braucht ihn.
    public static Logbuch Shared { get; } = new();
    public ObservableCollection<LogEintrag> Eintraege { get; } = new();
    private string aufgeraeumtAm = "";
    private readonly object schloss = new();
    private readonly Dictionary<string, CancellationTokenSource> offene = new();

    private Logbuch() { Directory.CreateDirectory(Ordner); }

    private static string? Sauber(string? s, int grenze)
    {
        if (s == null) return null;
        var t = Geheimnisse.Schwaerzen(s);
        return t.Length > grenze ? t[..grenze] : t;
    }

    public void Schreib(string art, string text, string? wo = null, string? werkzeug = null, string? eingabe = null, string? ergebnis = null, string? fehler = null, int? dauerMs = null)
    {
        var e = new LogEintrag
        {
            Art = art, Text = Sauber(text, 4000) ?? "", Wo = wo, Werkzeug = werkzeug, Eingabe = Sauber(eingabe, 2000),
            Ergebnis = Sauber(ergebnis, 4000), Fehler = Sauber(fehler, 2000), DauerMs = dauerMs,
        };
        Haupt.Post(() =>
        {
            Eintraege.Add(e);
            while (Eintraege.Count > 800) Eintraege.RemoveAt(0);
        });
        var o = new JSONObjekt { ["zeit"] = e.Zeit.ToString("o"), ["art"] = e.Art, ["was"] = e.Text, ["version"] = AppInfo.Version, ["wo"] = e.Wo ?? Ort(art) };
        if (e.Werkzeug != null) o["werkzeug"] = e.Werkzeug;
        if (e.Eingabe != null) o["eingabe"] = e.Eingabe;
        if (e.Ergebnis != null) o["ergebnis"] = e.Ergebnis;
        if (e.Fehler != null || art == "fehler") o["fehler"] = e.Fehler ?? e.Text;
        if (e.DauerMs != null) o["dauer_ms"] = e.DauerMs;
        var datei = Path.Combine(Ordner, e.Zeit.ToString("yyyy-MM-dd") + ".jsonl");
        // Auf die Platte im Hintergrund: Der Hauptfaden wartet sonst bei jedem Ereignis.
        Task.Run(() =>
        {
            lock (schloss)
            {
                Aufraeumen();
                try { File.AppendAllText(datei, JSON.Text(o) + "\n"); } catch { }
            }
        });
    }

    /// <summary>Einstellungen ändern sich beim Ziehen und Tippen im Takt: Eingetragen wird nur der Endwert nach einer Sekunde Ruhe.</summary>
    public void Einstellung(string name, string wert, string wo)
    {
        Haupt.Post(async () =>
        {
            if (offene.TryGetValue(name, out var alt)) alt.Cancel();
            var neu = new CancellationTokenSource();
            offene[name] = neu;
            try { await Task.Delay(1000, neu.Token); Schreib("einstellung", $"{name} = {wert}", wo); } catch (OperationCanceledException) { }
        });
    }

    private static string Ort(string art) => art switch
    {
        "gehirn" => "Gehirn", "sprache" => "Sprache", "live" => "Live-Gespräch", "werkzeug" => "Werkzeuge", _ => "App",
    };

    private void Aufraeumen()
    {
        var heute = DateTime.Now.ToString("yyyy-MM-dd");
        if (heute == aufgeraeumtAm) return;
        aufgeraeumtAm = heute;
        var grenze = DateTime.Now.AddDays(-AufbewahrungTage);
        foreach (var d in Directory.EnumerateFiles(Ordner, "*.jsonl"))
            if (DateTime.TryParse(Path.GetFileNameWithoutExtension(d), out var tag) && tag < grenze)
                try { File.Delete(d); } catch { }
    }
}

public static partial class Globale
{
    /// <summary>log(...) der Mac-Fassung.</summary>
    public static void Log(string art, string text, string? wo = null, string? werkzeug = null, string? eingabe = null, string? ergebnis = null, string? fehler = null, int? dauerMs = null)
        => Logbuch.Shared.Schreib(art, text, wo, werkzeug, eingabe, ergebnis, fehler, dauerMs);

    public static string Schnitt(this string s, int n) => s.Length <= n ? s : s[..n];
}

public enum Anbieter { Anthropic, Openai }

public sealed record ModellWahl(string Id, string Name, Anbieter Anbieter)
{
    public static readonly IReadOnlyList<ModellWahl> Alle = new ModellWahl[]
    {
        new("claude-opus-5-5", "Claude Opus 5.5", Anbieter.Anthropic),
        new("claude-sonnet-5-5", "Claude Sonnet 5.5", Anbieter.Anthropic),
        new("claude-haiku-5-5", "Claude Haiku 5.5", Anbieter.Anthropic),
        new("claude-fable-5-1", "Claude Fable 5.1", Anbieter.Anthropic),
        new("gpt-6-astra", "GPT-6 Astra", Anbieter.Openai),
        new("gpt-6.1-sol", "GPT-6.1 Sol", Anbieter.Openai),
        new("gpt-6-luna", "GPT-6 Luna", Anbieter.Openai),
        new("gpt-6.1-terra", "GPT-6.1 Terra", Anbieter.Openai),
    };
    public static IEnumerable<ModellWahl> FuerRecherche => Alle.Where(m => !m.Id.Contains("luna") && !m.Id.Contains("haiku"));
    public static ModellWahl Zu(string id) => Alle.FirstOrDefault(m => m.Id == id) ?? Alle[0];
    public static string Aktuell(string id) => id == "gpt-5.6-terra" ? "gpt-6.1-terra" : id;
}

/// <summary>Einfacher Ersatz für UserDefaults: eine JSON-Datei, schreibt verzögert. Auch für die Vorlese-Einstellungen und alles andere, was UserDefaults nutzte.</summary>
public static class Vorgaben
{
    private static readonly string Datei = Path.Combine(Pfade.Unterstuetzung, "einstellungen.json");
    private static readonly object Schloss = new();
    private static readonly JSONObjekt Werte = Laden();
    private static CancellationTokenSource? uhr;

    private static JSONObjekt Laden()
    {
        try { return JSON.Objekt(File.ReadAllBytes(Datei)) ?? new(); } catch { return new(); }
    }

    public static object? Objekt(string k) { lock (Schloss) return Werte.TryGetValue(k, out var v) ? v : null; }
    public static string? Text(string k) => Objekt(k) as string;
    public static bool Hat(string k) => Objekt(k) != null;
    public static bool Wahr(string k, bool vorgabe) => Objekt(k) as bool? ?? vorgabe;
    public static int Ganz(string k, int vorgabe) => Objekt(k) switch { long l => (int)l, int i => i, double d => (int)d, _ => vorgabe };
    public static double Zahl(string k, double vorgabe) => Objekt(k) switch { long l => l, int i => i, double d => d, _ => vorgabe };
    public static void Setze(string k, object? v)
    {
        lock (Schloss)
        {
            if (v == null) Werte.Remove(k); else Werte[k] = v;
            uhr?.Cancel();
            uhr = new CancellationTokenSource();
            var t = uhr.Token;
            Task.Delay(400, t).ContinueWith(_ => Sichern(), t, TaskContinuationOptions.OnlyOnRanToCompletion, TaskScheduler.Default);
        }
    }
    public static void Sichern()
    {
        lock (Schloss)
        {
            try
            {
                var teil = Datei + ".teil";
                File.WriteAllText(teil, JSON.Text(Werte, schoen: true));
                File.Move(teil, Datei, true);
            }
            catch { }
        }
    }
}

public sealed class Einstellungen : Beobachtbar
{
    public static Einstellungen Shared { get; } = new();

    private Zugang _zugang = Zugang.Laden();
    private CancellationTokenSource? speicherUhr;
    public Zugang Zugang { get => _zugang; set { _zugang = value; Melde(); ZugangGeaendert(); } }
    /// <summary>Nach Änderungen an Zugang-Feldern aufrufen: Speichert nach kurzer Tipp-Pause, nicht bei jedem Tastendruck.</summary>
    public void ZugangGeaendert()
    {
        speicherUhr?.Cancel();
        speicherUhr = new CancellationTokenSource();
        var t = speicherUhr.Token;
        Task.Delay(700, t).ContinueWith(_ => ZugangSichern(), t, TaskContinuationOptions.OnlyOnRanToCompletion, TaskScheduler.Default);
    }
    public void ZugangSichern() { speicherUhr?.Cancel(); speicherUhr = null; _zugang.Speichern(); }

    private T Merke<T>(ref T feld, T wert, [CallerMemberName] string? name = null) where T : notnull
    {
        if (Setze(ref feld, wert, name))
        {
            var schluessel = char.ToLowerInvariant(name![0]) + name[1..];
            Vorgaben.Setze(schluessel, wert);
            Logbuch.Shared.Einstellung(schluessel, wert.ToString() ?? "", "Einstellungen");
        }
        return wert;
    }

    private bool _helferAutomatik = Vorgaben.Wahr("helferAutomatik", true);
    /// <summary>Eigene Tools: darf Jarvis fehlende Helfer selbst bauen lassen? Aus = Pause, vorhandene Helfer laufen weiter.</summary>
    public bool HelferAutomatik { get => _helferAutomatik; set => Merke(ref _helferAutomatik, value); }
    private string _rechercheModell = Vorgaben.Text("rechercheModell") ?? "gpt-6.1-sol";
    public string RechercheModell { get => _rechercheModell; set => Merke(ref _rechercheModell, value); }
    private string _rechercheEffort = Vorgaben.Text("rechercheEffort") ?? "high";
    public string RechercheEffort { get => _rechercheEffort; set => Merke(ref _rechercheEffort, value); }
    private string _modell = ModellWahl.Aktuell(Vorgaben.Text("modell") ?? "claude-opus-5-5");
    public string Modell { get => _modell; set { Merke(ref _modell, value); Melde(nameof(ModellWahlAktuell)); } }
    private string _effort = Vorgaben.Text("effort") ?? "medium";
    public string Effort { get => _effort; set => Merke(ref _effort, value); }
    private int _schrittLimit = Vorgaben.Ganz("schrittLimit", 40);
    public int SchrittLimit { get => _schrittLimit; set => Merke(ref _schrittLimit, value); }
    private bool _vorlesen = Vorgaben.Wahr("vorlesen", true);
    public bool Vorlesen { get => _vorlesen; set => Merke(ref _vorlesen, value); }
    private string _stimme = Vorgaben.Text("stimme") ?? "de-DE-Chirp3-HD-Charon";
    public string Stimme { get => _stimme; set => Merke(ref _stimme, value); }
    private double _tempo = Vorgaben.Zahl("tempo", 1.05);
    public double Tempo { get => _tempo; set => Merke(ref _tempo, value); }
    private bool _weckwortAn = Vorgaben.Wahr("weckwortAn", true);
    public bool WeckwortAn { get => _weckwortAn; set => Merke(ref _weckwortAn, value); }
    private string _weckwort = Vorgaben.Text("weckwort") ?? "Hey Jarvis";
    public string Weckwort { get => _weckwort; set => Merke(ref _weckwort, value); }
    // Ohne das 640-MB-Modell wäre das Diktat tot, bis es geladen ist: dann Groq, bis Frank Parakeet wählt.
    private string _transkription = Vorgaben.Text("transkription") ?? (ParakeetLocalClient.IsInstalled ? "parakeet" : "groq");
    /// <summary>parakeet | groq | windows (die Mac-Fassung hat statt windows apple).</summary>
    public string Transkription { get => _transkription; set => Merke(ref _transkription, value); }
    private string _liveAnbieter = Vorgaben.Text("liveAnbieter") ?? "openai";
    public string LiveAnbieter { get => _liveAnbieter; set => Merke(ref _liveAnbieter, value); }
    private string _liveStimmeOpenAI = Vorgaben.Text("liveStimmeOpenAI") ?? "flitz";
    public string LiveStimmeOpenAI { get => _liveStimmeOpenAI; set => Merke(ref _liveStimmeOpenAI, value); }
    private string _liveStimmeGemini = Vorgaben.Text("liveStimmeGemini") ?? "Charon";
    public string LiveStimmeGemini { get => _liveStimmeGemini; set => Merke(ref _liveStimmeGemini, value); }
    private string _liveModellOpenAI = Vorgaben.Text("liveModellOpenAI") ?? "gpt-live-1";
    public string LiveModellOpenAI { get => _liveModellOpenAI; set => Merke(ref _liveModellOpenAI, value); }
    private string _liveModellGemini = Vorgaben.Text("liveModellGemini") ?? "gemini-3.8-live";
    public string LiveModellGemini { get => _liveModellGemini; set => Merke(ref _liveModellGemini, value); }
    private int _liveStilleMinuten = Vorgaben.Ganz("liveStilleMinuten", 5);
    /// <summary>Live-Gespräch endet nach so vielen Minuten Stille, 0 = nie.</summary>
    public int LiveStilleMinuten { get => _liveStilleMinuten; set => Merke(ref _liveStilleMinuten, value); }

    public const double StilleMin = 1.0, StilleMax = 15.0, StilleStandard = 4.5;
    private static double StilleGeladen(string k) => Math.Clamp(Vorgaben.Zahl(k, StilleStandard), StilleMin, StilleMax);
    private double _stilleInAppSek = StilleGeladen("stilleInAppSek");
    /// <summary>Stille nach dem letzten Wort in Sekunden, bis das Mikrofon schließt, bei Jarvis vorn.</summary>
    public double StilleInAppSek { get => _stilleInAppSek; set => Merke(ref _stilleInAppSek, value); }
    private double _stilleAussenSek = StilleGeladen("stilleAussenSek");
    /// <summary>Dasselbe im Hintergrund, per Weckwort oder Kürzel (nicht Live).</summary>
    public double StilleAussenSek { get => _stilleAussenSek; set => Merke(ref _stilleAussenSek, value); }

    /// <summary>Sekunden aus Text („2,5“, „2.5 Sekunden“, „Maximum“). null bei allem anderen, auch anderer Einheit: „2 Minuten“ darf nie still 2 Sekunden werden.</summary>
    public static double? StilleAus(string text)
    {
        var t = text.Trim().ToLowerInvariant();
        if (new[] { "maximal", "maximum", "max", "voll" }.Contains(t)) return StilleMax;
        if (new[] { "minimal", "minimum", "min" }.Contains(t)) return StilleMin;
        var m = System.Text.RegularExpressions.Regex.Match(t, @"^(\d+([.,]\d+)?)\s*(s|sek\.?|sekunden?)?$");
        if (!m.Success || !double.TryParse(m.Groups[1].Value.Replace(',', '.'), System.Globalization.CultureInfo.InvariantCulture, out var x)) return null;
        var g = Math.Round(x * 10) / 10;
        return g >= StilleMin && g <= StilleMax ? g : null;
    }
    public static string StilleZahl(double s) => s == Math.Round(s) ? ((int)s).ToString() : s.ToString("0.0", System.Globalization.CultureInfo.InvariantCulture).Replace('.', ',');
    public static string StilleText(double s) => StilleZahl(s) + (s == 1 ? " Sekunde" : " Sekunden");

    private bool _vollzugriff = Vorgaben.Wahr("vollzugriff", true);
    public bool Vollzugriff { get => _vollzugriff; set => Merke(ref _vollzugriff, value); }
    private bool _lernenAn = Vorgaben.Wahr("lernenAn", true);
    public bool LernenAn { get => _lernenAn; set => Merke(ref _lernenAn, value); }
    private bool _globalesKuerzel = Vorgaben.Wahr("globalesKuerzel", true);
    public bool GlobalesKuerzel { get => _globalesKuerzel; set => Merke(ref _globalesKuerzel, value); }

    private Einstellungen()
    {
        if (!Vorgaben.Hat("liveAnbieter") && _zugang.Openai.Length == 0 && _zugang.Gemini.Length > 0) _liveAnbieter = "gemini";
    }

    /// <summary>Hat Jarvis überhaupt einen Kopf? (Claude-Schlüssel, ChatGPT-Anmeldung oder OpenAI-Schlüssel)</summary>
    public async Task<bool> GehirnZugang()
    {
        if (_zugang.Anthropic.Length > 0 || _zugang.Openai.Length > 0) return true;
        return await CodexAnmeldung.Shared.Angemeldet();
    }

    public ModellWahl ModellWahlAktuell => ModellWahl.Zu(Modell);
}
