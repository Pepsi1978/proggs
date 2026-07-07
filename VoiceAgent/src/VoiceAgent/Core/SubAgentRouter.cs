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
    /// Waehlt per LLM den passenden Unteragenten fuer eine erkannte Aufgabe — anhand der
    /// Helfer-BESCHREIBUNGEN, nicht nur nach Stichwort. So versteht der Hauptagent auch freie
    /// Formulierungen ("kannst du dir das aufschreiben?" -> Notiz-Helfer), die ein reines
    /// Keyword-Matching verpasst.
    ///
    /// Laeuft wie IntentDetector/EndpointDetector auf einem eigenen, billigen Modell. Gibt bei
    /// Unsicherheit oder Fehler null zurueck — der Aufrufer (BossAgent) faellt dann verlustfrei
    /// auf das bisherige Stichwort-Matching zurueck (funktionserhaltend).
    /// </summary>
    public sealed class SubAgentRouter
    {
        private readonly ILlmProvider _provider;
        public SubAgentRouter(ILlmProvider provider) => _provider = provider;

        /// <summary>
        /// Liefert den Namen des zustaendigen Unteragenten — oder null (kein passender Helfer /
        /// Fehler / Unsicherheit). Der BossAgent uebernimmt dann selbst bzw. nutzt das Keyword-Matching.
        /// </summary>
        public async Task<string?> RouteAsync(string task, IReadOnlyList<ISubAgent> agents, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(task) || agents == null || agents.Count == 0) return null;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, BuildSystemPrompt(agents)),
                    new LlmMessage(LlmRole.User, task)
                };
                var reply = (await _provider.ChatAsync(msgs, ct).ConfigureAwait(false) ?? string.Empty).Trim();
                var picked = MatchName(reply, agents);
                Probe.Decision("Helfer-Router", picked ?? "KEINER", new { task, reply });
                Log.Info($"SubAgent-Router: \"{task}\" -> {(picked ?? "KEINER")} (LLM: {reply})");
                return picked;
            }
            catch (Exception ex)
            {
                Log.Error("SubAgent-Router: Routing fehlgeschlagen — Stichwort-Fallback uebernimmt", ex);
                return null;
            }
        }

        /// <summary>Baut den System-Prompt mit der Helfer-Liste. Public static = isoliert testbar.</summary>
        public static string BuildSystemPrompt(IReadOnlyList<ISubAgent> agents)
        {
            var sb = new StringBuilder();
            sb.AppendLine("Du bist der Vermittler eines Sprachassistenten. Der Hauptagent hat eine Aufgabe des Nutzers erkannt. Waehle den EINEN Helfer, der am besten dafuer zustaendig ist — oder KEINER, wenn keiner wirklich passt.");
            sb.AppendLine();
            sb.AppendLine("Verfuegbare Helfer (Name — Zustaendigkeit):");
            foreach (var a in agents)
                sb.AppendLine($"- {a.Name}: {a.Description}");
            sb.AppendLine();
            sb.Append("Antworte mit GENAU dem Namen des passenden Helfers (exakt wie oben geschrieben) oder mit dem Wort KEINER. Sonst nichts.");
            return sb.ToString();
        }

        /// <summary>
        /// Ordnet die LLM-Antwort einem Helfer-Namen zu (tolerant, case-insensitiv). "KEINER" oder
        /// kein Treffer => null. Public static = isoliert testbar.
        /// </summary>
        public static string? MatchName(string? reply, IReadOnlyList<ISubAgent> agents)
        {
            if (string.IsNullOrWhiteSpace(reply) || agents == null) return null;
            var u = reply.ToLowerInvariant();
            if (u.Contains("keiner")) return null;

            // Bevorzugt exakter Treffer; sonst der laengste enthaltene Name (robust gegen Teil-Strings).
            string? best = null;
            foreach (var a in agents)
            {
                var name = a.Name.ToLowerInvariant();
                if (u == name) return a.Name;
                if (u.Contains(name) && (best == null || a.Name.Length > best.Length))
                    best = a.Name;
            }
            return best;
        }
    }
}
