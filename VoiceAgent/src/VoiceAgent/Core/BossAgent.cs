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
        private readonly ChatSession _session;

        /// <summary>
        /// Offene Verstaendnis-Rueckfrage: ein Unteragent wartet auf das "Ja" des Nutzers,
        /// bevor er die Aufgabe ausfuehrt (Manifest Abschnitt 6: erst zurueckfragen, dann tun).
        /// null = keine offene Rueckfrage.
        /// </summary>
        private PendingAction? _pending;

        /// <summary>Eine vom Nutzer noch zu bestaetigende Aufgabe (Unteragent + originaler Auftragstext).</summary>
        private sealed record PendingAction(ISubAgent Sub, string Task);

        /// <summary>True, solange eine Verstaendnis-Rueckfrage auf Antwort wartet (fuer UI/Tests).</summary>
        public bool HasPendingConfirmation => _pending != null;

        public BossAgent(ILlmProvider provider, string? systemPrompt, AgentMemory? memory = null, SubAgentRegistry? subAgents = null, string? timeZoneId = null, ChatSession? session = null)
        {
            _provider = provider;
            _systemPrompt = string.IsNullOrWhiteSpace(systemPrompt) ? BossAgentPrompt.Default : systemPrompt!;
            _memory = memory;
            _subAgents = subAgents;
            _timeZoneId = timeZoneId;
            _session = session ?? new ChatSession();   // entkoppelt: aktive Session wird hereingereicht
        }

        public IReadOnlyList<LlmMessage> History => _session.History;

        /// <summary>Name des aktiven LLM-Providers (fuer die Turn-Trace: welches Gehirn hat geantwortet).</summary>
        public string ProviderName => _provider.Name;

        /// <summary>System-Prompt + bisheriger Verlauf — die vollstaendige Nachrichtenliste fuers LLM.</summary>
        public IReadOnlyList<LlmMessage> BuildMessages()
        {
            // System-Prompt + Faehigkeiten (damit der Agent NIE falsch verneint) + aktuelle Echtzeit
            // (LLM kennt die Uhrzeit sonst nicht) + Gedaechtnis-Block ueber Sessions.
            // Faehigkeiten IMMER mitgeben: die fruehere "nur bei Frage"-Heuristik war zu fragil
            // (verpasste Formulierungen wie "...erinnern kannst" -> Agent verneinte faelschlich).
            // capabilities.md ist kompakt genug, um verlaesslich bei jedem Turn dabei zu sein.
            var systemText = _systemPrompt
                + AgentCapabilities.BuildBlock()
                + TimeContext.BuildBlock(_timeZoneId)
                + (_memory?.ContextBlock() ?? string.Empty);
            var list = new List<LlmMessage>(_session.History.Count + 2)
            {
                new LlmMessage(LlmRole.System, systemText)
            };
            if (!string.IsNullOrWhiteSpace(_session.Summary))
                list.Add(new LlmMessage(LlmRole.System,
                    "ZUSAMMENFASSUNG DES BISHERIGEN GESPRAECHS (aeltere Teile dieser Session):\n" + _session.Summary));
            list.AddRange(_session.History);
            // Live-Sonde: jeder Turn protokolliert, wie viel Kontext mitgeht
            // (haette den Settings-Bug sofort sichtbar gemacht — messages waere auf 0 gefallen).
            Log.Info("CHECKPOINT Kontext-Block gebaut",
                new { messages = _session.History.Count, hasSummary = !string.IsNullOrWhiteSpace(_session.Summary) });
            return list;
        }

        /// <summary>Verarbeitet eine Nutzer-Eingabe und liefert die Antwort des Hauptagenten.</summary>
        public async Task<string> RespondAsync(string userText, CancellationToken ct = default)
        {
            Log.Info($"BossAgent: Eingabe empfangen ({userText?.Length ?? 0} Zeichen)");
            Probe.That(!string.IsNullOrWhiteSpace(userText), "BossAgent: leere Eingabe an den Hauptagenten");
            _session.History.Add(new LlmMessage(LlmRole.User, userText ?? string.Empty));
            try
            {
                var reply = await _provider.ChatAsync(BuildMessages(), ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(reply), "BossAgent: LLM lieferte eine leere Antwort", new { provider = _provider.Name });
                _session.History.Add(new LlmMessage(LlmRole.Assistant, reply));
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
            // ---- 1) Steht eine Verstaendnis-Rueckfrage offen? Dann zuerst Ja/Nein deuten. ----
            if (_pending != null)
            {
                var decision = ConfirmationDetector.Interpret(userText);
                Probe.Decision("Rueckfrage-Antwort", decision.ToString(), new { sub = _pending.Sub.Name });
                switch (decision)
                {
                    case ConfirmationDetector.Decision.Yes:
                        return await ExecutePendingAsync(userText, ct).ConfigureAwait(false);

                    case ConfirmationDetector.Decision.No:
                    {
                        var declinedSub = _pending.Sub.Name;
                        _pending = null;
                        const string declined = "Alles klar, dann lasse ich das.";
                        RecordTurn(userText, declined);
                        Log.Info("BossAgent: Aufgabe nach Rueckfrage abgelehnt", new { sub = declinedSub });
                        return new AgentReply(declined, null);
                    }

                    default: // Unclear -> Rueckfrage fallen lassen, Eingabe als NEUES Thema behandeln (kein Haengenbleiben)
                        Log.Info("BossAgent: Rueckfrage unbeantwortet -> als neues Thema behandelt",
                            new { sub = _pending.Sub.Name });
                        _pending = null;
                        break;
                }
            }

            // ---- 2) Neue Aufgabe erkannt + zustaendiger Unteragent -> ZUERST zurueckfragen (NICHT ausfuehren). ----
            if (intent == IntentKind.Task && _subAgents != null)
            {
                var sub = _subAgents.Find(userText);
                if (sub != null)
                {
                    string question;
                    try { question = sub.ConfirmationQuestion(userText); }
                    catch (Exception ex)
                    {
                        Log.Error($"BossAgent: ConfirmationQuestion von '{sub.Name}' warf — generische Rueckfrage", ex);
                        question = "Soll ich das fuer dich erledigen?";
                    }
                    _pending = new PendingAction(sub, userText);
                    Probe.Decision("Aufgabe", "Rueckfrage gestellt (noch nicht ausgefuehrt)", new { sub = sub.Name });
                    RecordTurn(userText, question);
                    Log.Info($"BossAgent: Rueckfrage zu Unteragent '{sub.Name}' gestellt");
                    return new AgentReply(question, null);
                }
            }

            // ---- 3) Sonst: Hauptagent antwortet selbst. ----
            return new AgentReply(await RespondAsync(userText, ct).ConfigureAwait(false), null);
        }

        /// <summary>
        /// Fuehrt die zuvor zurueckgefragte Aufgabe aus, nachdem der Nutzer bestaetigt hat.
        /// Faellt bei einem Fehler des Unteragenten sicher auf den Hauptagenten zurueck
        /// (funktionserhaltend — nichts geht verloren).
        /// </summary>
        private async Task<AgentReply> ExecutePendingAsync(string userText, CancellationToken ct)
        {
            var pending = _pending!;
            _pending = null;   // Rueckfrage ist beantwortet — egal wie es ausgeht
            Log.Info($"BossAgent: Aufgabe bestaetigt -> delegiere an '{pending.Sub.Name}'");
            try
            {
                var subReply = await pending.Sub.HandleAsync(pending.Task, ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(subReply),
                    "BossAgent: Unteragent lieferte leere Antwort nach Bestaetigung", new { sub = pending.Sub.Name });
                RecordTurn(userText, subReply);   // Verlauf: Nutzer-"Ja" + Ergebnis
                return new AgentReply(subReply, pending.Sub.Name);
            }
            catch (Exception ex)
            {
                Log.Error($"BossAgent: bestaetigter Unteragent '{pending.Sub.Name}' schlug fehl — Hauptagent uebernimmt", ex);
                return new AgentReply(await RespondAsync(userText, ct).ConfigureAwait(false), null);
            }
        }

        /// <summary>Schreibt einen Nutzer/Antwort-Turn in Verlauf UND Langzeitgedaechtnis (eine Stelle, konsistent).</summary>
        private void RecordTurn(string userText, string reply)
        {
            _session.History.Add(new LlmMessage(LlmRole.User, userText ?? string.Empty));
            _session.History.Add(new LlmMessage(LlmRole.Assistant, reply));
            _memory?.RecordTurn(userText ?? string.Empty, reply);
        }

        /// <summary>Setzt den Gespraechsverlauf zurueck (neues Gespraech).</summary>
        public void Reset()
        {
            _session.History.Clear();
            _session.Summary = "";
            _pending = null;   // offene Rueckfrage gehoert nicht in ein neues Gespraech
            Log.Info("BossAgent: Verlauf der aktiven Session zurueckgesetzt");
        }
    }
}
