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
        private readonly AgentMemory? _memory;
        private readonly SubAgentRegistry? _subAgents;
        private readonly string? _timeZoneId;
        private readonly List<LlmMessage> _history = new();

        public BossAgent(ILlmProvider provider, string? systemPrompt, AgentMemory? memory = null, SubAgentRegistry? subAgents = null, string? timeZoneId = null)
        {
            _provider = provider;
            _systemPrompt = string.IsNullOrWhiteSpace(systemPrompt) ? BossAgentPrompt.Default : systemPrompt!;
            _memory = memory;
            _subAgents = subAgents;
            _timeZoneId = timeZoneId;
        }

        public IReadOnlyList<LlmMessage> History => _history;

        /// <summary>Name des aktiven LLM-Providers (fuer die Turn-Trace: welches Gehirn hat geantwortet).</summary>
        public string ProviderName => _provider.Name;

        /// <summary>System-Prompt + bisheriger Verlauf — die vollstaendige Nachrichtenliste fuers LLM.</summary>
        public IReadOnlyList<LlmMessage> BuildMessages()
        {
            // System-Prompt + aktuelle Echtzeit (LLM kennt die Uhrzeit sonst nicht) + Gedaechtnis-Block.
            var systemText = _systemPrompt
                + TimeContext.BuildBlock(_timeZoneId)
                + (_memory?.ContextBlock() ?? string.Empty);

            // Faehigkeiten-Liste NUR anhaengen, wenn der Nutzer gerade danach fragt — sonst Kontext sparen
            // (Frank-Wunsch 2026-06-07: nicht bei jedem Turn, nur bei "kannst du ...?"-Fragen).
            string? lastUser = null;
            for (int i = _history.Count - 1; i >= 0; i--)
                if (_history[i].Role == LlmRole.User) { lastUser = _history[i].Text; break; }
            if (AgentCapabilities.IsCapabilityQuestion(lastUser))
                systemText += AgentCapabilities.BuildBlock();
            var list = new List<LlmMessage>(_history.Count + 1)
            {
                new LlmMessage(LlmRole.System, systemText)
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
                _memory?.RecordTurn(userText ?? string.Empty, reply);   // ueber Sessions hinweg merken
                Log.Info($"BossAgent: Antwort erzeugt ({reply.Length} Zeichen) via {_provider.Name}");
                return reply;
            }
            catch (Exception ex)
            {
                Log.Error("BossAgent: Fehler beim Erzeugen der Antwort", ex);
                throw;
            }
        }

        /// <summary>
        /// Hauptagent dirigiert: Ist eine AUFGABE erkannt UND ein Unteragent zustaendig, wird an
        /// ihn DELEGIERT; sonst antwortet der Hauptagent selbst (LLM). Liefert die Antwort und —
        /// falls delegiert — den Namen des Unteragenten (fuer die Turn-Trace). Faellt bei einem
        /// Fehler des Unteragenten sicher auf den Hauptagenten zurueck (kein Funktionsverlust).
        /// </summary>
        public async Task<AgentReply> HandleAsync(string userText, IntentKind intent, CancellationToken ct = default)
        {
            if (intent == IntentKind.Task && _subAgents != null)
            {
                var sub = _subAgents.Find(userText);
                if (sub != null)
                {
                    Log.Info($"BossAgent: delegiere an Unteragent '{sub.Name}'");
                    try
                    {
                        var subReply = await sub.HandleAsync(userText, ct).ConfigureAwait(false);
                        _history.Add(new LlmMessage(LlmRole.User, userText ?? string.Empty));
                        _history.Add(new LlmMessage(LlmRole.Assistant, subReply));
                        _memory?.RecordTurn(userText ?? string.Empty, subReply);
                        return new AgentReply(subReply, sub.Name);
                    }
                    catch (Exception ex)
                    {
                        Log.Error($"BossAgent: Unteragent '{sub.Name}' schlug fehl — Hauptagent uebernimmt", ex);
                        // Fallback: nichts geht verloren, der Hauptagent antwortet selbst.
                    }
                }
            }
            return new AgentReply(await RespondAsync(userText, ct).ConfigureAwait(false), null);
        }

        /// <summary>Setzt den Gespraechsverlauf zurueck (neues Gespraech).</summary>
        public void Reset()
        {
            _history.Clear();
            Log.Info("BossAgent: Gespraechsverlauf zurueckgesetzt");
        }
    }
}
