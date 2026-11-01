using System.Globalization;

namespace JarvisWindows;

/// <summary>Modelle, Stimmen und Anweisungen des Live-Gesprächs – Werte wie in der Handy-App (Jarvis/live/LiveOptionen.kt, Stand 28.10.2026).</summary>
public static class LiveOptionen
{
    public sealed record Wahl(string Id, string Name);

    /// <summary>„Bei Stille beenden“ wie am Handy: Minuten, 0 = nie.</summary>
    public static readonly IReadOnlyList<(int Minuten, string Name)> StilleEnde = new (int, string)[] { (2, "Nach 2 Minuten"), (5, "Nach 5 Minuten"), (10, "Nach 10 Minuten"), (0, "Nie") };

    public static readonly IReadOnlyList<Wahl> OpenAIModelle = new Wahl[]
    {
        new("gpt-live-1", "GPT-Live 1 (neuestes in der API)"),
        new("gpt-realtime-2.1", "Realtime 2.1"),
        new("gpt-realtime-2.1-mini", "Realtime 2.1 mini"),
    };
    public static readonly IReadOnlyList<Wahl> GeminiModelle = new Wahl[]
    {
        new("gemini-3.8-live", "Gemini 3.8 Live"),
        new("gemini-3.8-live-extended-thinking", "Gemini 3.8 Live Extended Thinking"),
        new("gemini-3.1-flash-live-preview", "Gemini 3.1 Flash Live"),
    };

    /// <summary>GPT-Live-Stimmen (API-Referenz, 28.10.2026); Nuri, Juni und Flitz sind die deutschen.</summary>
    public static readonly IReadOnlyList<Wahl> LiveStimmen = new Wahl[]
    {
        new("nuri", "Nuri (Deutsch, weiblich)"), new("juni", "Juni (Deutsch, weiblich)"), new("flitz", "Flitz (Deutsch, männlich)"),
        new("marin", "Marin (Standard)"), new("cedar", "Cedar"), new("alloy", "Alloy"), new("ash", "Ash"), new("ballad", "Ballad"), new("coral", "Coral"),
        new("echo", "Echo"), new("sage", "Sage"), new("shimmer", "Shimmer"), new("verse", "Verse"),
    };
    /// <summary>Realtime-Stimmen.</summary>
    public static readonly IReadOnlyList<Wahl> RealtimeStimmen = new Wahl[]
    {
        new("marin", "Marin"), new("cedar", "Cedar"), new("alloy", "Alloy"), new("ash", "Ash"), new("ballad", "Ballad"),
        new("coral", "Coral"), new("echo", "Echo"), new("sage", "Sage"), new("shimmer", "Shimmer"), new("verse", "Verse"),
    };
    /// <summary>Die 30 Stimmen der Gemini Live API.</summary>
    public static readonly IReadOnlyList<Wahl> GeminiStimmen = new (string, string)[]
    {
        ("Charon", "sachlich, männlich"), ("Puck", "munter, männlich"), ("Kore", "bestimmt, weiblich"), ("Zephyr", "hell, weiblich"),
        ("Fenrir", "aufgeweckt, männlich"), ("Leda", "jugendlich, weiblich"), ("Orus", "bestimmt, männlich"), ("Aoede", "locker, weiblich"),
        ("Callirrhoe", "gelassen, weiblich"), ("Autonoe", "klar, weiblich"), ("Enceladus", "hauchig, männlich"), ("Iapetus", "deutlich, männlich"),
        ("Umbriel", "gelassen, männlich"), ("Algieba", "weich, männlich"), ("Despina", "weich, weiblich"), ("Erinome", "deutlich, weiblich"),
        ("Algenib", "rau, männlich"), ("Rasalgethi", "informativ, männlich"), ("Laomedeia", "beschwingt, weiblich"), ("Achernar", "sanft, weiblich"),
        ("Alnilam", "fest, männlich"), ("Schedar", "ausgeglichen, männlich"), ("Gacrux", "reif, weiblich"), ("Pulcherrima", "direkt, weiblich"),
        ("Achird", "freundlich, männlich"), ("Zubenelgenubi", "ungezwungen, männlich"), ("Vindemiatrix", "behutsam, weiblich"), ("Sadachbia", "lebhaft, männlich"),
        ("Sadaltager", "kenntnisreich, männlich"), ("Sulafat", "warm, weiblich"),
    }.Select(x => new Wahl(x.Item1, $"{x.Item1} · {x.Item2}")).ToList();

    /// <summary>Gültiges OpenAI-Modell (unbekannte Werte → GPT-Live 1).</summary>
    public static string OpenAIModell(string wert) => OpenAIModelle.Any(m => m.Id == wert) ? wert : "gpt-live-1";
    /// <summary>Gültiges Gemini-Modell (unbekannte Werte → Gemini 3.8 Live, wie das Handy-Profil „Gemini 3.8 Live“).</summary>
    public static string GeminiModell(string wert) => GeminiModelle.Any(m => m.Id == wert) ? wert : "gemini-3.8-live";

    public static string OpenAIStimme(string wert, string modell)
    {
        var live = modell.StartsWith("gpt-live", StringComparison.Ordinal);
        var liste = live ? LiveStimmen : RealtimeStimmen;
        return liste.Any(s => s.Id == wert) ? wert : (live ? "flitz" : "cedar");
    }
    public static string GeminiStimme(string wert) => GeminiStimmen.Any(s => s.Id == wert) ? wert : "Charon";

    /// <summary>Gemini: Denkstufen je Modell (Handy: geminiStufen).</summary>
    public static string? GeminiDenkstufe(string modell) => modell switch
    {
        "gemini-3.8-live-extended-thinking" => "LOW",
        "gemini-3.1-flash-live-preview" => "MINIMAL",
        _ => null,
    };

    public const string Begruessung = "Begrüße Frank kurz auf Deutsch, höchstens ein Satz, dann hör zu.";
    public const string DauertNoch = "Das dauert noch etwas. Ich sage dir die Antwort, sobald sie da ist, auch wenn unser Gespräch bis dahin endet. Sie steht dann auch im Chat.";

    /// <summary>Neutrale Signale, die Gemini sagen darf, während Jarvis arbeitet.</summary>
    public static readonly IReadOnlyList<string> Signale = new[] { "Mhm", "Okay", "Alles klar", "Moment", "Einen Moment", "Augenblick", "Ich schau nach", "Lass mich kurz nachsehen" };

    public static string Jetzt() => DateTime.Now.ToString("dddd, d. MMMM yyyy, HH:mm", CultureInfo.GetCultureInfo("de-DE")) + " Uhr.";

    private const string Bereiche = "Termine, Dienstplan, Aufgaben, Wecker, Timer, Biodaten, Schlaf, Ideen, Tagebuch, Mails, Wetter, Fahrzeit, Gedächtnis, " +
        "Ablage, Programmieraufträge, Recherchen und alles, was am PC passieren soll (Programme öffnen, Befehle, Dateien, Terminal, OpenLauncher, Updates, Bildschirm)";

    /// <summary>Anweisung fürs Sprachmodell. gptLive: delegiert (Client-Delegation), sonst Funktion jarvis_fragen. nurStimme: Gemini „Jarvis direkt“.</summary>
    public static string Anweisung(bool gptLive, bool nurStimme)
    {
        if (nurStimme)
        {
            return "# Rolle\nDu bist die Stimme von Jarvis, Franks persönlichem Assistenten auf seinem Laptop, im Live-Sprachgespräch. " +
                "Inhaltlich antwortet immer Jarvis: Die App gibt jeden Beitrag von Frank an Jarvis weiter. Hörbar sind Jarvis' Antworten in deinen Worten " +
                "und kurze neutrale Signale; eigene Sachantworten bleiben stumm.\n" +
                "# Sprache\nSprich immer Deutsch. Natürlich, warm und flüssig: kurze Sätze, keine Aufzählungszeichen, keine Formatierung, keine Links und keine Kennungen vorlesen.\n" +
                "# Franks Beiträge\nAuf alles, was Frank sagt, auch Smalltalk, ein Danke oder eine bloße Mitteilung, rufst du sofort jarvis_fragen mit seinen Worten auf. " +
                "Antworte nie selbst inhaltlich, auch nicht, wenn du die Antwort zu wissen glaubst. Davor darfst du höchstens ein kurzes neutrales Signal sagen, nur eines von: " +
                string.Join(", ", Signale.Select(s => $"„{s}“")) + ". Oft passt gar keins. Nie zweimal dasselbe hintereinander, " +
                "kein Ja oder Nein, keine Wartefloskeln wiederholen und nichts von der Antwort vorwegnehmen.\n" +
                "# Antworten von Jarvis\nDas Ergebnis von jarvis_fragen und jede Nachricht, die mit „" + JarvisWeiche.Marke.Trim() + "“ beginnt, ist Jarvis' Antwort an Frank. " +
                "Sprich sie sofort, ohne die Markierung und ohne Einleitung wie „Jarvis sagt“. Du musst sie nicht wörtlich vorlesen: Formuliere ihren Kern natürlich " +
                "wie im Gespräch und verbinde ihn gern mit einem kurzen Übergang wie „Also“ oder „Schau mal“. Alle Fakten, Zahlen, Namen, Zeiten, Einschränkungen, Unsicherheiten, " +
                "Rückfragen und Bestätigungen bleiben dabei genau erhalten. Füge keine eigenen Fakten, Ratschläge oder Bewertungen hinzu, und bestätige nur, was Jarvis bestätigt. " +
                "Auf eine solche Nachricht rufst du nie jarvis_fragen auf.\n" +
                "# Ausnahme\nNur die kurze Begrüßung zu Beginn sprichst du selbst.\n" +
                "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf. Eine unterbrochene Antwort setzt du nicht von dir aus fort; will Frank den Rest hören, kommt er von Jarvis.\n" +
                "# Regeln\nErfinde nie Daten. Nenne nie Quellen oder Webadressen. Behaupte nie selbst, etwas erledigt oder das Gespräch beendet zu haben; das sagt nur Jarvis.\n" +
                "Jetzt ist " + Jetzt();
        }
        return "# Rolle\nDu bist die Stimme von Jarvis, Franks persönlichem Assistenten, hier auf seinem Laptop im Live-Sprachgespräch. " +
            "Du selbst weißt nichts über Franks Daten und kannst am PC nichts tun; das macht Jarvis.\n" +
            "# Sprache\nSprich immer Deutsch, auch wenn Frank kurz Englisch spricht. Natürlich, warm und knapp wie ein guter Gesprächspartner: kurze Sätze, " +
            "keine Aufzählungszeichen, keine Formatierung, keine Links, keine Quellen und keine Kennungen vorlesen.\n" +
            "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf und gehst auf ihn ein. Lass ihm Denkpausen.\n" +
            (gptLive
                ? $"# Nachschlagen\nFür alles, was Franks Daten, Apps oder Aktionen betrifft ({Bereiche}) und für aktuelle Fragen aus dem Internet delegierst du, bevor du antwortest. " +
                  "Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“. Gib danach den Kern von Jarvis' Antwort natürlich wieder, ohne Fakten zu verändern.\n"
                : $"# Werkzeug\nFür alles, was Franks Daten, Apps oder Aktionen betrifft ({Bereiche}) und für aktuelle Fragen aus dem Internet rufst du jarvis_fragen mit dem vollständigen " +
                  "Auftrag in Franks Worten auf, bevor du antwortest. Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“.\n") +
            "# Beenden\nWill Frank das Gespräch beenden, etwa „Gespräch beenden“ oder „Tschüss Jarvis“, sag höchstens „Bis später“; die App schließt die Verbindung.\n" +
            "# Regeln\nErfinde nie Daten. Melde etwas erst als erledigt, wenn das Ergebnis von Jarvis da ist.\n" +
            "Jetzt ist " + Jetzt();
    }

    public static string JarvisWerkzeugBeschreibung() =>
        "Fragt Jarvis mit allen Werkzeugen und Franks Daten (Handy-Apps und PC): " + Bereiche +
        ". Gib den Auftrag vollständig in Franks Worten weiter.";

    /// <summary>Sätze, mit denen Frank das Live-Gespräch beendet.</summary>
    public static bool IstBeenden(string text)
    {
        var t = text.ToLowerInvariant();
        int a = 0, b = t.Length;
        while (a < b && (char.IsWhiteSpace(t[a]) || char.IsPunctuation(t[a]))) a++;
        while (b > a && (char.IsWhiteSpace(t[b - 1]) || char.IsPunctuation(t[b - 1]))) b--;
        t = t[a..b];
        string[] muster = { "gespräch beenden", "beende das gespräch", "beende das live-gespräch", "live-gespräch beenden", "leg auf",
            "tschüss jarvis", "tschüs jarvis", "ciao jarvis", "bis später jarvis", "auf wiedersehen jarvis", "sprachsession beenden" };
        return muster.Any(m => t.Contains(m, StringComparison.Ordinal)) || t == "tschüss" || t == "tschüs";
    }
}
