using System;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Ein benutzerdefinierter Unteragent, dessen Verhalten KOMPLETT aus einer
    /// <see cref="SubAgentDefinition"/> kommt — kein eigener Code pro Helfer. Bei einer Aufgabe
    /// ruft er das LLM mit seinem eigenen System-Prompt und liefert eine kurze, vorlesbare Antwort.
    ///
    /// So kann Frank (per Sprache oder spaeter GUI) beliebig viele Spezialisten anlegen
    /// ("Uebersetzer", "Koch-Berater", …) — Manifest Baustein 2 (eigene Unteragenten definieren).
    /// Rein sprachlich/wissensbasiert, KEIN Computer Use (das bleibt eine eigene, abgesicherte Stufe).
    /// </summary>
    public sealed class PromptSubAgent : ISubAgent
    {
        private readonly SubAgentDefinition _def;
        private readonly ILlmProvider _llm;

        public PromptSubAgent(SubAgentDefinition def, ILlmProvider llm)
        {
            _def = def;
            _llm = llm;
        }

        public string Name => _def.Name;
        public string Description => _def.Description;

        // Anwendungs-Beispiele aus der Definition: die Trigger-Stichwoerter sind genau die
        // Woerter, mit denen der Nutzer diesen selbstgebauten Helfer anspricht.
        public string HowTo => (_def.Triggers != null && _def.Triggers.Count > 0)
            ? $"Sprich ihn ueber Stichwoerter wie {string.Join(", ", _def.Triggers)} an — die Aufgabe geht dann direkt an diesen Helfer."
            : "Nenne sein Fachgebiet in deiner Aufgabe — sie geht dann direkt an diesen Helfer.";

        public bool CanHandle(string task)
        {
            if (string.IsNullOrWhiteSpace(task) || _def.Triggers == null) return false;
            var t = task.ToLowerInvariant();
            foreach (var tr in _def.Triggers)
                if (!string.IsNullOrWhiteSpace(tr) && t.Contains(tr.ToLowerInvariant())) return true;
            return false;
        }

        public string ConfirmationQuestion(string task)
            => $"Soll ich das an deinen Helfer „{Name}“ geben?";

        public async Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            var sys = string.IsNullOrWhiteSpace(_def.SystemPrompt)
                ? $"Du bist ein spezialisierter Helfer namens {_def.Name}. Deine Aufgabe: {_def.Description}. " +
                  "Antworte kurz, natuerlich und gut vorlesbar auf Deutsch — keine Sonderzeichen, keine Aufzaehlungen."
                : _def.SystemPrompt;

            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, sys),
                    new LlmMessage(LlmRole.User, task)
                };
                var reply = await _llm.ChatAsync(msgs, ct).ConfigureAwait(false);
                Probe.That(!string.IsNullOrWhiteSpace(reply),
                    "PromptSubAgent: LLM lieferte leere Antwort", new { agent = _def.Name });
                Log.Info($"PromptSubAgent '{_def.Name}': Antwort erzeugt", new { chars = reply?.Length ?? 0 });
                return string.IsNullOrWhiteSpace(reply) ? "Dazu faellt mir gerade nichts ein." : reply!.Trim();
            }
            catch (Exception ex)
            {
                Log.Error($"PromptSubAgent '{_def.Name}': Antwort fehlgeschlagen", ex);
                return $"Dein Helfer {_def.Name} hat gerade ein Problem. Versuch es nochmal.";
            }
        }
    }
}
