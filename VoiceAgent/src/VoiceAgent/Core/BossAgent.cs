using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Text;
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
        // ---- Routing-Entscheidung (gemeinsam fuer den synchronen UND den gestreamten Pfad) ----

        private enum RouteKind { ExecuteConfirmed, Declined, AskBack, MainAgent }
        private sealed record Route(RouteKind Kind, ISubAgent? Sub, string? Task, string? Text);

        /// <summary>
        /// Entscheidet, was mit der Eingabe geschehen soll (und mutiert dabei die offene Rueckfrage
        /// _pending). KEINE Ausfuehrung, KEIN LLM-Call — nur die Entscheidung. So nutzen HandleAsync
        /// (synchron) und HandleStreaming (gestreamt) GENAU dieselbe Gate-Logik (Manifest Abschnitt 6).
        /// </summary>
        private Route DecideRoute(string userText, IntentKind intent)
        {
            // 1) Offene Verstaendnis-Rueckfrage? Zuerst Ja/Nein deuten.
            if (_pending != null)
            {
                var decision = ConfirmationDetector.Interpret(userText);
                Probe.Decision("Rueckfrage-Antwort", decision.ToString(), new { sub = _pending.Sub.Name });
                switch (decision)
                {
                    case ConfirmationDetector.Decision.Yes:
                    {
                        var p = _pending; _pending = null;
                        return new Route(RouteKind.ExecuteConfirmed, p.Sub, p.Task, null);
                    }
                    case ConfirmationDetector.Decision.No:
                    {
                        var name = _pending.Sub.Name; _pending = null;
                        Log.Info("BossAgent: Aufgabe nach Rueckfrage abgelehnt", new { sub = name });
                        return new Route(RouteKind.Declined, null, null, "Alles klar, dann lasse ich das.");
                    }
                    default: // Unclear -> Rueckfrage fallen lassen, Eingabe als NEUES Thema behandeln
                        Log.Info("BossAgent: Rueckfrage unbeantwortet -> als neues Thema behandelt",
                            new { sub = _pending.Sub.Name });
                        _pending = null;
                        break;
                }
            }

            // 2) Neue Aufgabe erkannt + zustaendiger Unteragent -> ZUERST zurueckfragen.
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
                    Log.Info($"BossAgent: Rueckfrage zu Unteragent '{sub.Name}' gestellt");
                    return new Route(RouteKind.AskBack, sub, userText, question);
                }
            }

            // 3) Sonst: Hauptagent antwortet selbst.
            return new Route(RouteKind.MainAgent, null, userText, null);
        }

        /// <summary>
        /// Verarbeitet eine Eingabe und liefert die KOMPLETTE Antwort auf einmal (nicht gestreamt).
        /// Gleiche Gate-Logik wie HandleStreaming — fuer Aufrufer/Tests, die den Volltext brauchen.
        /// </summary>
        public async Task<AgentReply> HandleAsync(string userText, IntentKind intent, CancellationToken ct = default)
        {
            var route = DecideRoute(userText, intent);
            switch (route.Kind)
            {
                case RouteKind.ExecuteConfirmed:
                    return await ExecuteConfirmedAsync(route.Sub!, route.Task!, userText, ct).ConfigureAwait(false);
                case RouteKind.Declined:
                case RouteKind.AskBack:
                    RecordTurn(userText, route.Text!);
                    return new AgentReply(route.Text!, null);
                default:
                    return new AgentReply(await RespondAsync(userText, ct).ConfigureAwait(false), null);
            }
        }

        /// <summary>
        /// Wie HandleAsync, aber liefert die Antwort als STROM (fuers zeitechte Vorlesen). Die
        /// Routing-Entscheidung (Rueckfrage/Delegation) ist synchron und steht sofort fest; nur die
        /// Selbstantwort des Hauptagenten wird — wenn der Provider es kann — tokenweise gestreamt.
        /// </summary>
        public AgentStream HandleStreaming(string userText, IntentKind intent, CancellationToken ct = default)
        {
            var route = DecideRoute(userText, intent);
            return route.Kind switch
            {
                RouteKind.ExecuteConfirmed =>
                    new AgentStream(route.Sub!.Name, ExecuteConfirmedStream(route.Sub!, route.Task!, userText, ct)),
                RouteKind.Declined or RouteKind.AskBack =>
                    new AgentStream(null, SingleChunkRecorded(userText, route.Text!, ct)),
                _ =>
                    new AgentStream(null, MainAgentStream(userText, ct)),
            };
        }

        /// <summary>Fuehrt die bestaetigte Aufgabe aus (komplette Antwort). Fehler-Fallback auf den Hauptagenten.</summary>
        private async Task<AgentReply> ExecuteConfirmedAsync(ISubAgent sub, string task, string userText, CancellationToken ct)
        {
            Log.Info($"BossAgent: Aufgabe bestaetigt -> delegiere an '{sub.Name}'");
            try
            {
                var subReply = await sub.HandleAsync(task, ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(subReply),
                    "BossAgent: Unteragent lieferte leere Antwort nach Bestaetigung", new { sub = sub.Name });
                RecordTurn(userText, subReply);
                return new AgentReply(subReply, sub.Name);
            }
            catch (Exception ex)
            {
                Log.Error($"BossAgent: bestaetigter Unteragent '{sub.Name}' schlug fehl — Hauptagent uebernimmt", ex);
                return new AgentReply(await RespondAsync(userText, ct).ConfigureAwait(false), null);
            }
        }

        // ---- Stream-Varianten (yield) ----

        /// <summary>Gibt einen fertigen Text als EINEN Chunk aus und schreibt den Turn in den Verlauf.</summary>
        private async IAsyncEnumerable<string> SingleChunkRecorded(string userText, string text,
            [EnumeratorCancellation] CancellationToken ct = default)
        {
            yield return text;
            RecordTurn(userText, text);
            await Task.CompletedTask.ConfigureAwait(false);   // haelt die Methode async (kein CS1998)
        }

        /// <summary>Fuehrt die bestaetigte Aufgabe aus und gibt das Ergebnis als EINEN Chunk aus (Fehler-Fallback auf Hauptagent).</summary>
        private async IAsyncEnumerable<string> ExecuteConfirmedStream(ISubAgent sub, string task, string userText,
            [EnumeratorCancellation] CancellationToken ct = default)
        {
            Log.Info($"BossAgent: Aufgabe bestaetigt -> delegiere an '{sub.Name}'");
            string reply;
            bool viaFallback = false;
            try
            {
                reply = await sub.HandleAsync(task, ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(reply),
                    "BossAgent: Unteragent lieferte leere Antwort nach Bestaetigung", new { sub = sub.Name });
            }
            catch (Exception ex)
            {
                Log.Error($"BossAgent: bestaetigter Unteragent '{sub.Name}' schlug fehl — Hauptagent uebernimmt", ex);
                reply = await RespondAsync(userText, ct).ConfigureAwait(false);   // RespondAsync schreibt History selbst
                viaFallback = true;
            }
            if (!viaFallback) RecordTurn(userText, reply);
            yield return reply;
        }

        /// <summary>
        /// Der Hauptagent antwortet selbst. Streamt tokenweise, wenn der Provider IStreamingLlmProvider
        /// implementiert; sonst Fallback auf ChatAsync (ein Chunk). Schreibt den vollen Turn am Ende in
        /// Verlauf + Gedaechtnis (der Iterator laeuft, waehrend der Consumer liest).
        /// </summary>
        private async IAsyncEnumerable<string> MainAgentStream(string userText,
            [EnumeratorCancellation] CancellationToken ct = default)
        {
            Probe.That(!string.IsNullOrWhiteSpace(userText), "BossAgent: leere Eingabe an den Hauptagenten (Stream)");
            _session.History.Add(new LlmMessage(LlmRole.User, userText ?? string.Empty));
            var messages = BuildMessages();
            var sb = new StringBuilder();

            if (_provider is IStreamingLlmProvider streaming)
            {
                await foreach (var delta in streaming.StreamAsync(messages, ct).WithCancellation(ct).ConfigureAwait(false))
                {
                    if (string.IsNullOrEmpty(delta)) continue;
                    sb.Append(delta);
                    yield return delta;
                }
            }
            else
            {
                var full = await _provider.ChatAsync(messages, ct).ConfigureAwait(false);
                sb.Append(full);
                yield return full;
            }

            var reply = sb.ToString();
            Probe.That(!string.IsNullOrWhiteSpace(reply),
                "BossAgent: LLM lieferte eine leere Antwort (Stream)", new { provider = _provider.Name });
            _session.History.Add(new LlmMessage(LlmRole.Assistant, reply));
            _memory?.RecordTurn(userText ?? string.Empty, reply);
            Log.Info($"BossAgent: Stream-Antwort erzeugt ({reply.Length} Zeichen) via {_provider.Name}");
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
