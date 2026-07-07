using System;
using System.Linq;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Deutet eine gesprochene Antwort auf eine Rueckfrage des Hauptagenten als
    /// Zustimmung (Yes), Ablehnung (No) oder Unklar. Heuristisch und isoliert testbar
    /// (public static Interpret) — bewusst kein LLM-Call, damit die Bestaetigung sofort
    /// und ohne Netzwerk-Latenz erkannt wird (Manifest: natuerliches, zeitechtes Gespraech).
    ///
    /// Konservativ ausgelegt: Bei kurzen Antworten hat ein Nein-Signal IMMER Vorrang
    /// ("sicher nicht" -> No, nicht Yes). Nur klare Signale werden gewertet; alles andere
    /// ist Unclear — der Aufrufer (BossAgent) verwirft dann die offene Rueckfrage und
    /// behandelt die Eingabe als neues Thema (kein Haengenbleiben, keine Fehlausfuehrung).
    /// </summary>
    public static class ConfirmationDetector
    {
        public enum Decision { Yes, No, Unclear }

        // Klare Zustimmungs-Woerter (gesprochen, deutsch). Mehrdeutige Einzelwoerter
        // ("auf", "doch") bewusst NICHT enthalten — die werden nur als Phrase gewertet.
        private static readonly string[] YesWords =
        {
            "ja", "jap", "jep", "joa", "jo", "jawohl", "jawoll", "klar", "klaro",
            "ok", "okay", "oki", "okey", "gerne", "gern", "genau", "passt", "stimmt",
            "korrekt", "richtig", "machs", "los", "go", "yes", "yep",
            "definitiv", "absolut", "unbedingt", "jawull"
        };

        // Klare Ablehnungs-Woerter.
        private static readonly string[] NoWords =
        {
            "nein", "ne", "nee", "noe", "nope", "nichts", "stopp", "stop", "halt",
            "abbrechen", "abbruch", "lass", "vergiss", "quatsch", "nicht", "niemals"
        };

        // Eindeutige Mehrwort-Phrasen (gewinnen vor der Token-Heuristik).
        private static readonly string[] NoPhrases =
        {
            "lieber nicht", "doch nicht", "lass es", "lass mal", "lass stecken",
            "vergiss es", "nicht noetig", "brauchst nicht", "nein danke", "ne lass",
            "sicher nicht", "auf keinen"
        };

        private static readonly string[] YesPhrases =
        {
            "ja bitte", "ja gerne", "ja mach", "mach das", "mach mal", "mach es",
            "ja genau", "auf jeden", "tu das", "leg los", "na klar", "ja klar",
            "ja sicher", "passt so", "ja unbedingt", "mach ruhig", "ja klaro"
        };

        /// <summary>Interpretiert die Antwort. Null/leer => Unclear.</summary>
        public static Decision Interpret(string? text)
        {
            if (string.IsNullOrWhiteSpace(text)) return Decision.Unclear;

            string norm = Normalize(text);

            // 1) Eindeutige Phrasen zuerst (Nein-Phrasen vor Ja-Phrasen).
            foreach (var p in NoPhrases) if (norm.Contains(p)) return Decision.No;
            foreach (var p in YesPhrases) if (norm.Contains(p)) return Decision.Yes;

            var tokens = norm.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (tokens.Length == 0) return Decision.Unclear;

            // 2) Kurze Antwort (1-3 Woerter): Nein-Signal hat VORRANG (konservativ).
            //    So wird "sicher nicht" / "ja aber nicht" sicher als Ablehnung gewertet.
            if (tokens.Length <= 3 && tokens.Any(t => NoWords.Contains(t)))
                return Decision.No;

            // 3) Erstes signifikantes Token entscheidet (so antwortet man: "Ja ...", "Nein ...", "Klar ...").
            string first = tokens[0];
            if (NoWords.Contains(first)) return Decision.No;
            if (YesWords.Contains(first)) return Decision.Yes;

            // 4) Kurze Antwort: irgendein klares Ja-Wort darin zaehlt.
            if (tokens.Length <= 3 && tokens.Any(t => YesWords.Contains(t)))
                return Decision.Yes;

            return Decision.Unclear;
        }

        // Fuellwoerter, die ein reines Nein begleiten duerfen, ohne dass es eine neue Anweisung wird.
        private static readonly string[] FillerWords =
        {
            "danke", "schon", "mal", "jetzt", "bitte", "das", "es", "doch", "erstmal",
            "lieber", "dann", "ach", "ok", "okay", "ne", "noe", "wirklich", "echt"
        };

        /// <summary>
        /// True, wenn die Antwort ein REINES Nein ist (nur Ablehnungs- und Fuellwoerter, keine
        /// neue Anweisung). Nur dann darf eine offene Aufgabe abgebrochen werden — "nein, mach es
        /// um 10 Uhr" ist KEIN Abbruch, sondern eine Korrektur (Almanach orchestrator-agent 5.5).
        /// Public static = isoliert testbar.
        /// </summary>
        public static bool IsPureDecline(string? text)
        {
            if (string.IsNullOrWhiteSpace(text)) return true;
            var norm = Normalize(text);
            foreach (var p in NoPhrases) norm = norm.Replace(p, " ");
            foreach (var t in norm.Split(' ', StringSplitOptions.RemoveEmptyEntries))
                if (!NoWords.Contains(t) && !FillerWords.Contains(t))
                    return false;   // es steckt mehr drin als ein Nein -> moegliche Korrektur
            return true;
        }

        /// <summary>Kleinschreibung, Satzzeichen weg (Umlaute bleiben), Mehrfach-Leerzeichen zusammen.</summary>
        private static string Normalize(string text)
        {
            var s = text.ToLowerInvariant().Trim();
            var chars = s.Select(c => char.IsLetterOrDigit(c) || c == ' ' ? c : ' ').ToArray();
            s = new string(chars);
            return string.Join(' ', s.Split(' ', StringSplitOptions.RemoveEmptyEntries));
        }
    }
}
