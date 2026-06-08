using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Minimaler SentencePiece-UNIGRAM-Encoder fuer die Weckwort-Tokenisierung zur Laufzeit.
    ///
    /// Hintergrund (Bug-Almanach wake-word #10/#34): Das KWS-Modell erkennt TOKENS aus keywords.txt,
    /// nicht Klartext. Damit ein FREI eingetipptes Weckwort wirkt, muss es zur Laufzeit in genau die
    /// Tokens zerlegt werden, die das mitgelieferte sentencepiece-Modell (bpe.model, in Wahrheit
    /// model_type=UNIGRAM, 500 Pieces) erzeugt. Eine schwergewichtige Tokenizer-Lib ist dafuer NICHT
    /// noetig — der Viterbi-Decode ueber 500 Pieces ist trivial und ohne neue NuGet-Abhaengigkeit.
    ///
    /// Das Vokabular (Piece + Log-Score) liegt als <c>unigram-vocab.txt</c> neben den Modelldateien;
    /// es wurde EINMAL aus <c>bpe.model</c> extrahiert (die Scores kommen damit garantiert von
    /// sentencepiece selbst, nicht von einem eigenen Protobuf-Parser). Verifiziert: dieser Encoder
    /// liefert fuer die kuratierten Weckwoerter UND beliebige englische Woerter exakt dieselben Tokens
    /// wie Python-sentencepiece (siehe <c>SentencePieceUnigramTests</c>).
    ///
    /// WICHTIG: Das Modell ist auf GROSSSCHREIBUNG trainiert ("COMPUTER" → "▁COMP U TER", aber
    /// "Computer" → "▁C omputer"). Der Input wird deshalb zu Grossbuchstaben normalisiert. Zeichen
    /// ausserhalb des (englischen) Vokabulars (Umlaute, viele Sonderzeichen) machen das Wort
    /// un-erkennbar → <see cref="Encode"/> gibt null zurueck, der Aufrufer lehnt es ab.
    /// </summary>
    public sealed class SentencePieceUnigram
    {
        private const char Sep = '▁';   // ▁  SentencePiece-Wortanfangsmarke

        private readonly Dictionary<string, float> _scores;   // nur NORMAL-Pieces (type 1)
        private readonly int _maxLen;

        // Pro Modell-Verzeichnis EINMAL laden (das Vokabular ist statisch). null = nicht ladbar.
        private static readonly ConcurrentDictionary<string, SentencePieceUnigram?> _cache =
            new(StringComparer.OrdinalIgnoreCase);

        private SentencePieceUnigram(Dictionary<string, float> scores, int maxLen)
        {
            _scores = scores;
            _maxLen = maxLen;
        }

        /// <summary>
        /// Laedt (gecacht) den Encoder fuer ein Modell-Verzeichnis. Gibt null zurueck, wenn das
        /// Vokabular fehlt oder leer ist — dann bleibt nur die kuratierte Auswahlliste.
        /// </summary>
        public static SentencePieceUnigram? ForModel(string modelDir)
        {
            return _cache.GetOrAdd(modelDir ?? string.Empty, static dir =>
            {
                try
                {
                    string path = Path.Combine(dir, "unigram-vocab.txt");
                    if (!File.Exists(path))
                    {
                        Log.Warn("SentencePieceUnigram: Vokabular fehlt — freie Weckwort-Eingabe nicht moeglich",
                                 new { path });
                        return null;
                    }

                    var scores = new Dictionary<string, float>(StringComparer.Ordinal);
                    int maxLen = 1;
                    foreach (var raw in File.ReadAllLines(path, Encoding.UTF8))
                    {
                        if (raw.Length == 0 || raw[0] == '#') continue;
                        // Zeilenformat: piece <TAB> score <TAB> type
                        var parts = raw.Split('\t');
                        if (parts.Length < 3 || parts[2] != "1") continue;   // nur NORMAL-Pieces zum Matchen
                        string piece = parts[0];
                        if (piece.Length == 0) continue;
                        if (!float.TryParse(parts[1], NumberStyles.Float, CultureInfo.InvariantCulture, out float score))
                            continue;
                        scores[piece] = score;
                        if (piece.Length > maxLen) maxLen = piece.Length;
                    }

                    Probe.That(scores.Count > 0, "SentencePieceUnigram: Vokabular leer geladen", new { path });
                    if (scores.Count == 0) return null;
                    Log.Info("SentencePieceUnigram geladen", new { pieces = scores.Count, maxLen });
                    return new SentencePieceUnigram(scores, maxLen);
                }
                catch (Exception ex)
                {
                    Log.Error("SentencePieceUnigram: Laden fehlgeschlagen", ex);
                    return null;
                }
            });
        }

        /// <summary>
        /// Tokenisiert einen freien Weckwort-Text in die SentencePiece-Tokens (durch Leerzeichen
        /// getrennt, z.B. "▁COMP U TER"). Gibt null zurueck, wenn der Text Zeichen enthaelt, die das
        /// (englische) Modell nicht kennt — solche Weckwoerter koennen nicht erkannt werden.
        /// Best-Path-Viterbi: maximiert die Summe der Piece-Log-Scores (= sentencepiece-Standard).
        /// </summary>
        public string? Encode(string? text)
        {
            string norm = Normalize(text);
            if (norm.Length == 0) return null;

            int n = norm.Length;
            var dp = new double[n + 1];
            var backPos = new int[n + 1];
            var backPiece = new string?[n + 1];
            for (int i = 1; i <= n; i++) { dp[i] = double.NegativeInfinity; backPos[i] = -1; }
            dp[0] = 0.0;

            for (int i = 0; i < n; i++)
            {
                if (double.IsNegativeInfinity(dp[i])) continue;
                int max = Math.Min(_maxLen, n - i);
                for (int len = 1; len <= max; len++)
                {
                    if (_scores.TryGetValue(norm.Substring(i, len), out float sc))
                    {
                        double cand = dp[i] + sc;
                        if (cand > dp[i + len])
                        {
                            dp[i + len] = cand;
                            backPos[i + len] = i;
                            backPiece[i + len] = norm.Substring(i, len);
                        }
                    }
                }
            }

            if (double.IsNegativeInfinity(dp[n]))
                return null;   // ein Zeichen war nicht zerlegbar (OOV) → nicht erkennbar

            var tokens = new List<string>();
            for (int i = n; i > 0; i = backPos[i])
            {
                if (backPos[i] < 0 || backPiece[i] is null) return null;   // Sicherheitsnetz
                tokens.Add(backPiece[i]!);
            }
            tokens.Reverse();
            return string.Join(' ', tokens);
        }

        /// <summary>Kann dieses Weckwort erkannt werden (alle Zeichen im Modell-Vokabular)?</summary>
        public bool CanEncode(string? text) => Encode(text) != null;

        /// <summary>
        /// sentencepiece-Normalisierung, vereinfacht fuer dieses englische Uppercase-Modell:
        /// trimmen, Mehrfach-Leerzeichen zusammenfassen, GROSSSCHREIBUNG, dann add_dummy_prefix=True
        /// (fuehrendes ▁) und jedes Leerzeichen → ▁.
        /// </summary>
        private static string Normalize(string? text)
        {
            if (string.IsNullOrWhiteSpace(text)) return string.Empty;
            var sb = new StringBuilder(text.Length + 1);
            sb.Append(Sep);
            bool lastSep = true;
            foreach (char c in text.Trim().ToUpperInvariant())
            {
                if (c == ' ' || c == '\t' || c == Sep)
                {
                    if (!lastSep) { sb.Append(Sep); lastSep = true; }
                }
                else { sb.Append(c); lastSep = false; }
            }
            if (sb.Length > 1 && sb[sb.Length - 1] == Sep) sb.Length -= 1;   // abschliessendes ▁ weg
            return sb.ToString();
        }
    }
}
