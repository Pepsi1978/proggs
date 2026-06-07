using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Verlustarme Hintergrund-Komprimierung pro Session: ab einer Schwelle werden die
    /// aeltesten Nachrichten zu einer kompakten Zusammenfassung verdichtet (per LLM) und aus
    /// dem wortwoertlichen Verlauf entfernt; die letzten K Nachrichten bleiben im Original.
    /// </summary>
    public sealed class ContextCompressor
    {
        private readonly int _budgetTokens;
        private readonly double _threshold;
        private readonly int _keepRecent;

        public ContextCompressor(int budgetTokens = 12000, double threshold = 0.75, int keepRecent = 6)
        {
            _budgetTokens = budgetTokens;
            _threshold = threshold;
            _keepRecent = keepRecent;
        }

        /// <summary>Grobe Token-Schaetzung (Zeichen/4) ueber Summary + Verlauf — kein Tokenizer noetig.</summary>
        public static int EstimateTokens(ChatSession s)
        {
            long chars = s.Summary?.Length ?? 0;
            foreach (var m in s.History) chars += m.Text?.Length ?? 0;
            return (int)(chars / 4);
        }

        /// <summary>Fuellstand 0..1 fuer die Kontext-Anzeige.</summary>
        public double Fill(ChatSession s) => Math.Min(1.0, (double)EstimateTokens(s) / _budgetTokens);

        /// <summary>Komprimiert, falls noetig. Liefert true, wenn etwas verdichtet wurde.</summary>
        public async Task<bool> MaybeCompressAsync(ChatSession s, ILlmProvider provider, CancellationToken ct = default)
        {
            if (EstimateTokens(s) < _budgetTokens * _threshold) return false;
            if (s.History.Count <= _keepRecent) return false;

            int cut = s.History.Count - _keepRecent;
            var older = s.History.GetRange(0, cut);

            var sb = new StringBuilder();
            if (!string.IsNullOrWhiteSpace(s.Summary))
                sb.Append("Bisherige Zusammenfassung:\n").Append(s.Summary).Append("\n\n");
            sb.AppendLine("Fasse den folgenden Gespraechsausschnitt KNAPP zusammen. Behalte Fakten, Entscheidungen, offene Aufgaben und Namen. Schreibe in Stichpunkten, kein Vorwort:");
            foreach (var m in older)
            {
                var who = m.Role == LlmRole.User ? "Frank" : "Agent";
                sb.Append(who).Append(": ").AppendLine(m.Text);
            }

            try
            {
                Log.Info("Komprimierung gestartet", new { before = EstimateTokens(s), cut, keep = _keepRecent });
                var summary = await provider.ChatAsync(
                    new List<LlmMessage> { new LlmMessage(LlmRole.User, sb.ToString()) }, ct).ConfigureAwait(false);

                if (string.IsNullOrWhiteSpace(summary))
                {
                    Log.Warn("Komprimierung lieferte leere Zusammenfassung — Verlauf bleibt unveraendert");
                    return false;
                }

                s.Summary = summary.Trim();
                s.History.RemoveRange(0, cut);
                Log.Info("Komprimierung fertig", new { after = EstimateTokens(s), remaining = s.History.Count });
                Probe.That(s.History.Count == _keepRecent, "ContextCompressor: unerwartete Verlaufslaenge nach Komprimierung",
                    new { count = s.History.Count, keep = _keepRecent });
                return true;
            }
            catch (Exception ex)
            {
                Log.Error("Komprimierung fehlgeschlagen — Verlauf bleibt vollstaendig erhalten", ex);
                return false;   // Funktionserhalt: lieber nicht komprimieren als Wissen verlieren
            }
        }
    }
}
