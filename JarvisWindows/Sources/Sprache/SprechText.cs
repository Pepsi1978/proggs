using System.Text;
using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Macht Text vorlesbar, bevor er an eine Stimme geht (Gegenstück zu SprechText.kt der Android-App).
/// Der angezeigte Text ändert sich nicht, nur der Weg zur Synthese wird aufbereitet.</summary>
public static class SprechText
{
    private static readonly (string, string)[] Abkuerzungen =
    {
        ("z. B.", "zum Beispiel"), ("z.B.", "zum Beispiel"), ("u. a.", "unter anderem"), ("u.a.", "unter anderem"),
        ("d. h.", "das heißt"), ("d.h.", "das heißt"), ("bzw.", "beziehungsweise"), ("ca.", "circa"),
        ("ggf.", "gegebenenfalls"), ("evtl.", "eventuell"), ("inkl.", "inklusive"), ("exkl.", "exklusive"),
        ("usw.", "und so weiter"), ("etc.", "und so weiter"), ("vgl.", "vergleiche"), ("Nr.", "Nummer"),
        ("Abb.", "Abbildung"), ("Kap.", "Kapitel"), ("Mio.", "Millionen"), ("Mrd.", "Milliarden"),
        ("Std.", "Stunden"), ("Sek.", "Sekunden"), ("Tsd.", "Tausend"), ("max.", "maximal"),
        ("s. o.", "siehe oben"), ("s. u.", "siehe unten"),
    };

    private static readonly (string, string)[] Ersatz =
    {
        ("&", " und "), ("%", " Prozent "), ("€", " Euro "), ("$", " Dollar "), ("£", " Pfund "),
        ("+", " plus "), ("=", " gleich "), ("<", " kleiner als "), (">", " größer als "),
        ("→", " führt zu "), ("←", " kommt von "), ("@", " at "), ("/", " oder "), ("|", ". "),
        ("•", ". "), ("·", ". "), ("…", ". "), ("„", ""), ("“", ""), ("”", ""), ("\"", ""), ("«", ""), ("»", ""),
        ("–", " - "), ("—", " - "), ("\\", " "), ("^", " "), ("~", " "), ("©", ""), ("®", ""), ("™", ""),
    };

    private static readonly Dictionary<string, string> Einheiten = new()
    {
        ["km"] = "Kilometer", ["cm"] = "Zentimeter", ["mm"] = "Millimeter", ["m"] = "Meter", ["l"] = "Liter", ["ml"] = "Milliliter",
        ["h"] = "Stunden", ["min"] = "Minuten", ["kWh"] = "Kilowattstunden", ["°C"] = "Grad", ["kg"] = "Kilogramm", ["g"] = "Gramm",
        ["ms"] = "Millisekunden", ["GB"] = "Gigabyte", ["MB"] = "Megabyte", ["W"] = "Watt",
    };

    private static readonly string[] Monate = { "Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember" };

    private static readonly string[] Ordnung =
    {
        "erster", "zweiter", "dritter", "vierter", "fünfter", "sechster", "siebter", "achter",
        "neunter", "zehnter", "elfter", "zwölfter", "dreizehnter", "vierzehnter", "fünfzehnter",
        "sechzehnter", "siebzehnter", "achtzehnter", "neunzehnter", "zwanzigster",
        "einundzwanzigster", "zweiundzwanzigster", "dreiundzwanzigster", "vierundzwanzigster",
        "fünfundzwanzigster", "sechsundzwanzigster", "siebenundzwanzigster", "achtundzwanzigster",
        "neunundzwanzigster", "dreißigster", "einunddreißigster",
    };

    private static string Ersetze(string text, string muster, Func<string[], string?> f) =>
        Regex.Replace(text, muster, m =>
        {
            var gruppen = m.Groups.Cast<Group>().Select(g => g.Success ? g.Value : "").ToArray();
            return f(gruppen) ?? gruppen[0];
        });

    private static string Re(string text, string muster, string vorlage, RegexOptions opt = RegexOptions.None) => Regex.Replace(text, muster, vorlage, opt);

    /// <summary>Emoji und Verwandtes, entspricht isEmojiPresentation bzw. isEmoji oberhalb von U+238C.</summary>
    private static bool IstEmoji(Rune r)
    {
        var v = r.Value;
        return v == 0xFE0F || v == 0x200D
            || (v >= 0x238D && v <= 0x23FF) || v == 0x24C2 || (v >= 0x25AA && v <= 0x25FE)
            || (v >= 0x2600 && v <= 0x27BF) || (v >= 0x2934 && v <= 0x2935) || (v >= 0x2B00 && v <= 0x2BFF)
            || v == 0x3030 || v == 0x303D || v == 0x3297 || v == 0x3299
            || (v >= 0x1F000 && v <= 0x1FAFF);
    }

    /// <summary>Bereinigt Text für die Sprachausgabe: Markdown, Code, Webadressen, Quellen, Emojis raus, Zahlen lesbar.</summary>
    public static string FuerStimme(string roh)
    {
        var t = roh;
        if (t.Trim().Length == 0) return "";
        t = Re(t, @"```[\s\S]*?```", " Codebeispiel. ");
        t = Re(t, @"`([^`]*)`", "$1");
        t = Re(t, @"\[([^\]]+)\]\([^)]*\)", "$1");
        t = Re(t, @"(?i)\b(?:https?://|www\.)\S+", " Link ");
        t = Re(t, @"\b[\w.-]+@[\w.-]+\.\w{2,}\b", " E-Mail-Adresse ");
        t = Re(t, @"(?<![\w])(?:~|/)[\w.-]*[/][\w./-]+", " Dateipfad ");
        t = Re(t, @"\((?:vgl\.|siehe|vergleiche|Quelle:?|S\.|Seite|Abb\.|Kap\.)[^)]*\)|\[[^\]]{0,80}\]", " ", RegexOptions.IgnoreCase);
        t = Re(t, @"(?m)^\s{0,6}(?:[-*•·+]|\d{1,2}[.)](?!\s*(?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\b))\s+", "");
        t = Re(t, @"(?m)^\|?[\s:|-]+\|[\s:|-]*$", "");          // Tabellen-Trennzeilen
        t = Re(t, @"[*_#]|~~|(?m:^[ \t]*>+)", "");
        t = Re(t, @"[(){}\[\]]", ", ");
        foreach (var (kurz, lang) in Abkuerzungen)
            t = Regex.Replace(t, @"(?<!\p{L})" + Regex.Escape(kurz), lang.Replace("$", "$$"));
        t = Zahlen(t);
        foreach (var (z, w) in Ersatz) t = t.Replace(z, w);
        // Emojis und sonstige Symbole entfernen
        var ohne = new StringBuilder(t.Length);
        foreach (var r in t.EnumerateRunes()) if (!IstEmoji(r)) ohne.Append(r.ToString());
        t = ohne.ToString();
        t = Re(t, @"[^\p{L}\p{N} .,!?;:\-'\n]", " ");
        t = Re(t, @"[ \t]+", " ");
        t = Re(t, @"([!?.])\1+", "$1");
        t = Re(t, @" ?([.,!?;:]) ?", "$1 ");
        t = Re(t, @"([.,!?;:])(?:\s*\1)+", "$1");
        t = Re(t, @",\s*\.", ".");
        t = Re(t, @"\n{3,}", "\n\n");
        return string.Join("\n", t.Split('\n').Select(z => z.Trim(' ', '\t'))).Trim();
    }

    private static string Zahlen(string roh)
    {
        var t = roh;
        t = Ersetze(t, @"\b(\d{1,2})\.(\d{1,2})\.(\d{4})?", g =>
        {
            if (!int.TryParse(g[1], out var tag) || !int.TryParse(g[2], out var monat) || tag < 1 || tag > 31 || monat < 1 || monat > 12) return null;
            return Ordnung[tag - 1] + " " + Monate[monat - 1] + (g[3].Length == 0 ? "" : " " + g[3]);
        });
        t = Ersetze(t, @"\b(\d{1,2}):(\d{2})\b", g =>
        {
            if (!int.TryParse(g[1], out var h) || !int.TryParse(g[2], out var m) || h < 0 || h > 23 || m < 0 || m > 59) return null;
            return m == 0 ? $"{h} Uhr" : $"{h} Uhr {m}";
        });
        t = Ersetze(t, @"\b(\d+),(\d+)\s*([a-zA-Z°]{1,4})?", g =>
        {
            var zahl = $"{g[1]} Komma {g[2]}";
            if (Einheiten.TryGetValue(g[3], out var e)) return zahl + " " + e;
            return zahl + (g[3].Length == 0 ? "" : " " + g[3]);
        });
        t = Ersetze(t, @"\b(\d+)\s*([a-zA-Z°]{1,4})\b", g => Einheiten.TryGetValue(g[2], out var e) ? $"{g[1]} {e}" : null);
        return t;
    }

    /// <summary>Zerlegt den Text in Stücke für die Synthese: erstes Stück kurz (schneller Start), danach bis ~350 Zeichen.</summary>
    public static List<string> Stuecke(string text)
    {
        var saetze = new List<string>();
        var aktuell = new StringBuilder();
        for (int i = 0; i < text.Length; i++)
        {
            var c = text[i];
            aktuell.Append(c);
            var ende = c == '\n' || ((c == '.' || c == '!' || c == '?') && (i + 1 >= text.Length || text[i + 1] == ' ' || text[i + 1] == '\n'));
            if (ende)
            {
                var s = aktuell.ToString().Trim();
                if (s.Length > 0) saetze.Add(s);
                aktuell.Clear();
            }
        }
        var rest = aktuell.ToString().Trim();
        if (rest.Length > 0) saetze.Add(rest);

        var stuecke = new List<string>();
        var puffer = "";
        foreach (var s in saetze)
        {
            var grenze = stuecke.Count == 0 ? 140 : 350;
            if (puffer.Length > 0 && puffer.Length + s.Length + 1 > grenze)
            {
                stuecke.Add(puffer);
                puffer = "";
            }
            puffer += (puffer.Length == 0 ? "" : " ") + s;
            if (stuecke.Count == 0 && puffer.Length >= 60)
            {
                stuecke.Add(puffer);
                puffer = "";
            }
        }
        if (puffer.Length > 0) stuecke.Add(puffer);
        // Überlange Einzelsätze (ohne Satzzeichen) hart an Wortgrenzen teilen.
        return stuecke.SelectMany(s =>
        {
            if (s.Length <= 800) return new List<string> { s };
            var teile = new List<string>();
            var p = "";
            foreach (var w in s.Split(' ', StringSplitOptions.RemoveEmptyEntries))
            {
                if (p.Length + w.Length > 600) { teile.Add(p); p = ""; }
                p += (p.Length == 0 ? "" : " ") + w;
            }
            if (p.Length > 0) teile.Add(p);
            return teile;
        }).ToList();
    }
}
