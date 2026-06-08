using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Zerlegt einen Strom von Text-Deltas (aus dem LLM-Stream) in vollstaendige Saetze,
    /// sobald sie erkennbar sind. Das ist die Grundlage fuers satzweise Vorlesen: ein Satz
    /// kann gesprochen werden, waehrend der naechste noch generiert wird.
    ///
    /// Schneidet an Satzzeichen (. ! ? …), aber nur wenn der Satz lang genug ist (vermeidet
    /// Fehlschnitte bei Abkuerzungen wie "z.B.") UND ein Folge-Whitespace die Vollstaendigkeit
    /// bestaetigt. Zeilenumbruch ist ein harter Schnitt (Absatz/Aufzaehlung). Der Rest am
    /// Stream-Ende wird immer geflusht — es geht nie Text verloren (verlustfrei).
    /// </summary>
    public static class SentenceSegmenter
    {
        /// <summary>Mindestlaenge, ab der an einem Satzzeichen geschnitten wird.</summary>
        public const int MinChars = 18;

        private static bool IsEnder(char c) => c == '.' || c == '!' || c == '?' || c == '…';
        private static bool IsTrailing(char c) => IsEnder(c) || c == '"' || c == '\'' || c == ')' || c == '”' || c == '«' || c == '»';

        /// <summary>
        /// Streamt vollstaendige Saetze aus einem Delta-Strom. Verlustfrei: der Rest am Ende
        /// wird immer als letzter Satz ausgegeben.
        /// </summary>
        public static async IAsyncEnumerable<string> Segment(
            IAsyncEnumerable<string> deltas,
            [EnumeratorCancellation] CancellationToken ct = default)
        {
            var buf = new StringBuilder();
            await foreach (var delta in deltas.WithCancellation(ct).ConfigureAwait(false))
            {
                if (string.IsNullOrEmpty(delta)) continue;
                buf.Append(delta);
                while (TryExtractSentence(buf, out var sentence))
                    yield return sentence;
            }

            var rest = buf.ToString().Trim();
            if (rest.Length > 0) yield return rest;
        }

        /// <summary>
        /// Zerlegt einen FERTIGEN Text in Saetze nach denselben Regeln (fuer Tests und den
        /// Fallback-Fall, in dem die ganze Antwort als ein Chunk kommt). Public static = testbar.
        /// </summary>
        public static List<string> SplitSentences(string? text)
        {
            var result = new List<string>();
            if (string.IsNullOrWhiteSpace(text)) return result;

            var buf = new StringBuilder(text);
            while (TryExtractSentence(buf, out var sentence))
                result.Add(sentence);

            var rest = buf.ToString().Trim();
            if (rest.Length > 0) result.Add(rest);
            return result;
        }

        /// <summary>
        /// Schneidet den ERSTEN vollstaendigen Satz aus dem Puffer (mutiert ihn) oder gibt false
        /// zurueck, wenn noch kein Satz sicher abgeschlossen ist.
        /// </summary>
        private static bool TryExtractSentence(StringBuilder buf, out string sentence)
        {
            sentence = string.Empty;
            string s = buf.ToString();

            for (int i = 0; i < s.Length; i++)
            {
                char c = s[i];

                // Harter Schnitt am Zeilenumbruch (Absatz/Aufzaehlung).
                if (c == '\n')
                {
                    var cand = s.Substring(0, i).Trim();
                    buf.Clear();
                    buf.Append(s.Substring(i + 1).TrimStart());
                    if (cand.Length > 0) { sentence = cand; return true; }
                    return TryExtractSentence(buf, out sentence);   // leere Zeile uebersprungen
                }

                if (!IsEnder(c)) continue;

                // Nachfolgende Satz-/Schlusszeichen mitnehmen ("?!", "...", schliessende Quotes).
                int j = i;
                while (j + 1 < s.Length && IsTrailing(s[j + 1])) j++;

                // Vollstaendig nur, wenn ein Whitespace folgt (sonst koennte der Satz weitergehen).
                if (j + 1 < s.Length && char.IsWhiteSpace(s[j + 1]))
                {
                    var cand = s.Substring(0, j + 1).Trim();
                    if (cand.Length >= MinChars)
                    {
                        sentence = cand;
                        buf.Clear();
                        buf.Append(s.Substring(j + 1).TrimStart());
                        return true;
                    }
                    i = j;   // zu kurz -> weiter hinter dieser Stelle suchen
                }
            }
            return false;
        }
    }
}
