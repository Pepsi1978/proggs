using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Der Hauptagent ("Boss"). Haelt den Gespraechsverlauf und den System-Prompt,
    /// schickt beides ans LLM (ueber ILlmProvider) und liefert die Antwort zurueck.
    /// Spaeter regelt er von hier aus auch die Unteragenten — fuer Baustein 1 fuehrt
    /// er das Gespraech und erkennt Aufgaben (per System-Prompt).
    /// </summary>
    public sealed class BossAgent
    {
        private readonly ILlmProvider _provider;
        private readonly string _systemPrompt;
        private readonly List<LlmMessage> _history = new();

        public BossAgent(ILlmProvider provider, string? systemPrompt)
        {
            _provider = provider;
            _systemPrompt = string.IsNullOrWhiteSpace(systemPrompt) ? BossAgentPrompt.Default : systemPrompt!;
        }

        public IReadOnlyList<LlmMessage> History => _history;

        /// <summary>Name des aktiven LLM-Providers (fuer die Turn-Trace: welches Gehirn hat geantwortet).</summary>
        public string ProviderName => _provider.Name;

        /// <summary>System-Prompt + bisheriger Verlauf — die vollstaendige Nachrichtenliste fuers LLM.</summary>
        public IReadOnlyList<LlmMessage> BuildMessages()
        {
            var list = new List<LlmMessage>(_history.Count + 1)
            {
                new LlmMessage(LlmRole.System, _systemPrompt)
            };
            list.AddRange(_history);
            return list;
        }

        /// <summary>Verarbeitet eine Nutzer-Eingabe und liefert die Antwort des Hauptagenten.</summary>
        public async Task<string> RespondAsync(string userText, CancellationToken ct = default)
        {
            Log.Info($"BossAgent: Eingabe empfangen ({userText?.Length ?? 0} Zeichen)");
            Probe.That(!string.IsNullOrWhiteSpace(userText), "BossAgent: leere Eingabe an den Hauptagenten");
            _history.Add(new LlmMessage(LlmRole.User, userText ?? string.Empty));
            try
            {
                var reply = await _provider.ChatAsync(BuildMessages(), ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(reply), "BossAgent: LLM lieferte eine leere Antwort", new { provider = _provider.Name });
                _history.Add(new LlmMessage(LlmRole.Assistant, reply));
                Log.Info($"BossAgent: Antwort erzeugt ({reply.Length} Zeichen) via {_provider.Name}");
                return reply;
            }
            catch (Exception ex)
            {
                Log.Error("BossAgent: Fehler beim Erzeugen der Antwort", ex);
                throw;
            }
        }

        /// <summary>Setzt den Gespraechsverlauf zurueck (neues Gespraech).</summary>
        public void Reset()
        {
            _history.Clear();
            Log.Info("BossAgent: Gespraechsverlauf zurueckgesetzt");
        }
    }
}
