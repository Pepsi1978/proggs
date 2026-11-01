using System.Globalization;
using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Erkennt, ob ein gesprochener Satz die Sprachsession beenden soll, wie am Handy (SprachBefehle.kt). Läuft vor dem Modell:
/// Was hier sicher erkannt wird, beendet sofort. Frei Formuliertes geht ans Modell, das dafür `sprachsession_beenden` hat.</summary>
public static class SprachBefehle
{
    private static readonly CultureInfo Deutsch = new("de-DE");

    /// <summary>„Ende“, „Stopp“, „Danke, das war's“ …: nur wenn der ganze Satz ein solcher Befehl ist.</summary>
    public static bool IstAusbefehl(string text) => Ausbefehle.Contains(Satz(text));

    /// <summary>Kleingeschrieben, ohne Satzzeichen und Füllwörter, als ein Satz.</summary>
    private static string Satz(string text)
    {
        var s = Regex.Replace(text.ToLower(Deutsch).Replace("’", "'"), "[^a-zäöüß' ]", " ");
        return string.Join(" ", s.Split(' ', StringSplitOptions.RemoveEmptyEntries).Where(w => !Fuellworte.Contains(w))).Replace("'", "");
    }

    /// <summary>Für die laufende Sprachschleife: Es genügt auch ein bloßes „Beenden“ oder „Session beenden“.</summary>
    public static bool IstSessionEnde(string text)
    {
        if (IstAusbefehl(text)) return true;
        var worte = Normiert(text);
        if (worte.Length == 0 || worte.Length > 8) return false;
        // Fragen und Verneinungen beenden nie: „Wie beende ich die Sprachsession?“, „Beende die Sprachsession nicht“.
        if (worte.Any(Sperrworte.Contains)) return false;
        var rest = worte.Where(w => !Fuellworte.Contains(w) && !Artikel.Contains(w) && !Helfer.Contains(w)).ToList();
        if (rest.Count == 0 || rest.Any(w => !EndeVerben.Contains(w) && !NomenImmer.Contains(w) && !NomenImModus.Contains(w))) return false;
        if (!rest.Any(EndeVerben.Contains)) return false;
        if (rest.Any(NomenImmer.Contains)) return true;
        // In der Sprachschleife: „Session beenden“ oder ein bloßes „Bitte beenden jetzt“, aber nie ein einzelnes „zu“ oder „ab“.
        return rest.Any(NomenImModus.Contains) || rest.Any(KernVerben.Contains) || (rest.Contains("hör") && rest.Contains("auf"));
    }

    /// <summary>Kleinschreibung, ohne Satzzeichen; „Sprach-Session“, „Sprach Session“ und „Voice Session“ werden zu „sprachsession“.</summary>
    private static string[] Normiert(string text)
    {
        var s = " " + Regex.Replace(Regex.Replace(text.ToLower(Deutsch), "[^a-zäöüß ]", " "), " +", " ").Trim(' ') + " ";
        foreach (var (alt, neu) in Zusammen) s = s.Replace($" {alt} ", $" {neu} ");
        return s.Split(' ', StringSplitOptions.RemoveEmptyEntries);
    }

    private static readonly HashSet<string> Fuellworte = new() { "jarvis", "okay", "ok", "bitte", "hey", "also", "gut", "dann", "ja", "nein", "nee", "so" };

    private static readonly HashSet<string> Helfer = new()
    {
        "jetzt", "mal", "doch", "einfach", "endlich", "sofort", "gleich", "nun", "erstmal", "danke", "vielen", "dank", "gerne", "gern",
        "kannst", "könntest", "würdest", "du", "wir", "können", "kann", "ich", "will", "möchte", "möchtest", "sollst", "lass", "lasst",
        "uns", "mach", "mache", "machen", "schalt", "schalte", "hiermit", "wieder", "komplett", "ganz", "für", "heute", "hier",
    };

    private static readonly HashSet<string> Artikel = new()
    {
        "die", "den", "das", "der", "dem", "diese", "diesen", "dieses", "unsere", "meine", "deine", "dein", "mein",
        "aktuelle", "aktive", "laufende", "aktuellen", "aktiven", "laufenden",
    };

    private static readonly HashSet<string> EndeVerben = new()
    {
        "beenden", "beende", "beendet", "beend", "stoppen", "stoppe", "stopp", "stop", "aus", "ausschalten", "ausmachen", "abschalten",
        "abstellen", "schließen", "schließe", "schließ", "zu", "zumachen", "ende", "schluss", "aufhören", "hör", "höre", "auf",
        "abbrechen", "brich", "ab", "zuzuhören",
    };

    /// <summary>Ohne Nomen zählen nur diese eindeutigen Verben.</summary>
    private static readonly HashSet<string> KernVerben = new() { "beenden", "beende", "beendet", "stoppen", "stoppe", "stopp", "stop", "aufhören", "abbrechen", "schluss", "ende" };

    private static readonly HashSet<string> NomenImmer = new()
    {
        "sprachsession", "sprachsitzung", "sprachmodus", "sprachschleife", "gesprächsmodus", "sprachdialog", "sprachsteuerung", "mikrofon", "mikro",
    };

    private static readonly HashSet<string> NomenImModus = new() { "session", "sitzung", "gespräch", "unterhaltung" };

    private static readonly HashSet<string> Sperrworte = new()
    {
        "nicht", "kein", "keine", "nie", "wie", "warum", "wieso", "weshalb", "was", "wann", "wo", "ob", "erklär", "erkläre",
        "programmier", "programmiersession", "programmierauftrag", "auftrag", "app",
    };

    private static readonly (string, string)[] Zusammen =
    {
        ("sprach session", "sprachsession"), ("voice session", "sprachsession"), ("sprach sitzung", "sprachsitzung"),
        ("sprach modus", "sprachmodus"), ("gesprächs modus", "gesprächsmodus"), ("sprach schleife", "sprachschleife"),
    };

    private static readonly HashSet<string> Hoeflich = new()
    {
        "danke", "danke schön", "dankeschön", "vielen dank", "tschüss", "tschüs", "ciao", "fertig", "das wars", "danke das wars", "das wars danke",
        "das ist alles", "danke das ist alles", "das reicht", "mehr nicht", "nichts mehr", "nichts weiter", "sonst nichts", "danke sonst nichts",
    };

    private static readonly HashSet<string> Ausbefehle = new(Hoeflich.Concat(new[]
    {
        "ende", "stopp", "stop", "schluss", "aus",
        "sprachsitzung beenden", "sprachsitzung beendet", "sitzung beenden", "gespräch beenden", "beenden", "beende", "aufhören", "hör auf",
        "ende der sitzung", "session beenden", "schluss für jetzt", "erstmal nicht", "erst mal nicht",
        // Am Laptop schon vor der Angleichung ans Handy gültig, deshalb weiter dabei.
        "das war es", "passt danke", "danke dir", "höre auf", "brich ab", "abbrechen",
    }));
}
