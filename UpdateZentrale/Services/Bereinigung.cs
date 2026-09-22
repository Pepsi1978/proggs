using System.Text.RegularExpressions;

namespace UpdateZentrale.Services;

/// <summary>
/// The single place that makes text safe to persist or export: secrets are masked, sizes are
/// bounded. Everything that reaches a log file, the history or the diagnostics package goes
/// through here -- never write raw command lines or tool output.
/// </summary>
public static class Bereinigung
{
    public const string Maske = "***";

    /// <summary>Key names whose value is a secret, wherever they appear (env, CLI, JSON, query).</summary>
    private const string Schluessel =
        @"(?:pass(?:word|wd|phrase)?|pwd|kennwort|passwort|secret|client[_-]?secret|token|access[_-]?token|refresh[_-]?token"
        + @"|id[_-]?token|auth[_-]?token|bearer[_-]?token|api[_-]?key|apikey|x-api-key|access[_-]?key|secret[_-]?key"
        + @"|private[_-]?key|session[_-]?(?:id|token|key)|cookie|set-cookie|credentials?|signature|sig)";

    private static readonly RegexOptions Optionen = RegexOptions.IgnoreCase | RegexOptions.Compiled | RegexOptions.CultureInvariant;

    private static readonly (Regex Muster, string Ersatz)[] Regeln =
    {
        // Authorization headers, any scheme.
        (new Regex(@"\b(proxy-authorization|authorization)(\s*[:=]\s*)(?:(bearer|basic|digest|token|negotiate)\s+)?[^\s""',;]+", Optionen),
            "$1$2$3 " + Maske),
        // Bare "Bearer xyz" / "Basic xyz".
        (new Regex(@"\b(bearer|basic)\s+[A-Za-z0-9\-._~+/]{8,}=*", Optionen), "$1 " + Maske),
        // Credentials inside URIs: scheme://user:pass@host
        (new Regex(@"\b([a-z][a-z0-9+.\-]*://)[^/\s:@""']+:[^/\s@""']+@", Optionen), "$1" + Maske + ":" + Maske + "@"),
        // JSON: "apiKey": "value"
        (new Regex(@"(""[A-Za-z0-9_\-]*" + Schluessel + @"""\s*:\s*"")([^""]*)("")", Optionen), "$1" + Maske + "$3"),
        // CLI flags: --token value, --api-key=value, -Password 'x', /password:x
        (new Regex(@"((?:--?|/)[A-Za-z0-9_\-]*" + Schluessel + @")(\s*[=: ]\s*)(['""]?)[^\s'""]+", Optionen), "$1$2$3" + Maske),
        // Assignments: GITHUB_TOKEN=..., password: x, api_key = "x", ?token=x&
        (new Regex(@"(\b[A-Za-z0-9_\-.]*?" + Schluessel + @")(\s*[=:]\s*)(['""]?)[^\s'""&;,]+", Optionen), "$1$2$3" + Maske),
        // Well-known token shapes, even without a key next to them.
        (new Regex(@"\bsk-(?:ant-|proj-)?[A-Za-z0-9_\-]{16,}", Optionen), Maske),
        (new Regex(@"\bgh[pousr]_[A-Za-z0-9]{20,}", Optionen), Maske),
        (new Regex(@"\bgithub_pat_[A-Za-z0-9_]{20,}", Optionen), Maske),
        (new Regex(@"\bxox[abprs]-[A-Za-z0-9\-]{10,}", Optionen), Maske),
        (new Regex(@"\bAKIA[0-9A-Z]{16}\b", Optionen), Maske),
        (new Regex(@"\bAIza[0-9A-Za-z_\-]{30,}", Optionen), Maske),
        (new Regex(@"\beyJ[A-Za-z0-9_\-]{8,}\.[A-Za-z0-9_\-]{8,}\.[A-Za-z0-9_\-]{8,}", Optionen), Maske),
    };

    /// <summary>Masks every secret pattern. Null becomes "".</summary>
    public static string Bereinigen(string? text)
    {
        if (string.IsNullOrEmpty(text)) return "";
        var ergebnis = text;
        foreach (var (muster, ersatz) in Regeln) ergebnis = muster.Replace(ergebnis, ersatz);
        return ergebnis;
    }

    /// <summary>
    /// Bounds a text: head and tail stay, the middle is replaced by a visible marker that names
    /// how much was left out and how long the original was.
    /// </summary>
    public static string Kuerzen(string? text, int maxZeichen)
    {
        if (string.IsNullOrEmpty(text)) return "";
        if (text.Length <= maxZeichen || maxZeichen < 40) return text.Length <= maxZeichen ? text : text[..maxZeichen];

        var kopf = maxZeichen * 3 / 4;
        var schwanz = maxZeichen - kopf;
        return text[..kopf]
               + "\n…[gekürzt: " + (text.Length - kopf - schwanz) + " von " + text.Length + " Zeichen ausgelassen]…\n"
               + text[^schwanz..];
    }

    /// <summary>Both in the right order: mask first (a secret must not survive in the kept part).</summary>
    public static string Sicher(string? text, int maxZeichen) => Kuerzen(Bereinigen(text), maxZeichen);
}
