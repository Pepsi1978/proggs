using System;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Der "Helfer-Baumeister": ein Unteragent, der ZUR LAUFZEIT neue Helfer anlegt
    /// (Manifest Baustein 2 / Vision: "Der Hauptagent kann jederzeit eigene Unteragenten bauen").
    ///
    /// Frank sagt z.B. "bau mir einen Helfer, der mir Kochrezepte vorschlaegt" — der Baumeister
    /// leitet per LLM eine <see cref="SubAgentDefinition"/> ab, persistiert sie (CustomAgentStore)
    /// und registriert sofort einen <see cref="PromptSubAgent"/> in der laufenden Registry. Ab dann
    /// ist der neue Helfer ansprechbar. Rein sprachlich — KEIN Computer Use.
    /// </summary>
    public sealed class AgentBuilderSubAgent : ISubAgent
    {
        private readonly ILlmProvider _llm;                 // leitet die Definition aus dem Wunsch ab
        private readonly Func<ILlmProvider> _newAgentLlm;   // Provider fuer den neu gebauten Helfer
        private readonly SubAgentRegistry _registry;
        private readonly CustomAgentStore _store;

        public AgentBuilderSubAgent(ILlmProvider llm, Func<ILlmProvider> newAgentLlm,
            SubAgentRegistry registry, CustomAgentStore store)
        {
            _llm = llm;
            _newAgentLlm = newAgentLlm;
            _registry = registry;
            _store = store;
        }

        public string Name => "Helfer-Baumeister";
        public string Description => "Legt auf Wunsch neue, eigene Helfer an (z. B. einen Uebersetzer oder Koch-Berater).";

        public string HowTo => "Sag z. B. 'bau mir einen Uebersetzer-Helfer' oder 'erstelle einen Helfer fuer Kochrezepte' — der neue Helfer wird sofort angelegt, ist ab dann ansprechbar und bleibt ueber Neustarts erhalten.";

        private static readonly string[] BuildVerbs =
            { "bau", "erstell", "leg ", "erzeug", "richte", "mach mir ein", "neuen helfer", "neuen assistent" };
        private static readonly string[] HelperWords =
            { "helfer", "assistent", "agent", "experte", "spezialist", "berater" };

        // Namen, die der Baumeister nicht ueberschreiben darf (Kern-Helfer + sich selbst).
        private static readonly string[] Reserved =
            { "helfer-baumeister", "erinnerung", "notiz" };

        public bool CanHandle(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return false;
            var t = task.ToLowerInvariant();
            bool verb = BuildVerbs.Any(v => t.Contains(v));
            bool helper = HelperWords.Any(h => t.Contains(h));
            return verb && helper;
        }

        public string ConfirmationQuestion(string task)
            => "Soll ich dir dafuer einen neuen Helfer anlegen?";

        public async Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            SubAgentDefinition? def;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, ExtractPrompt),
                    new LlmMessage(LlmRole.User, task)
                };
                var reply = await _llm.ChatAsync(msgs, ct).ConfigureAwait(false);
                def = ParseDefinition(reply);
            }
            catch (Exception ex)
            {
                Log.Error("AgentBuilder: Definition-Extraktion fehlgeschlagen", ex);
                def = null;
            }

            if (def == null || string.IsNullOrWhiteSpace(def.Name))
                return "Ich konnte daraus keinen Helfer ableiten. Sag zum Beispiel: Bau mir einen Helfer, der mir Kochrezepte vorschlaegt.";

            if (Reserved.Contains(def.Name.Trim().ToLowerInvariant()))
                return $"Den Namen {def.Name} kann ich nicht verwenden, der ist schon vergeben. Nenn ihn bitte anders.";

            _store.Add(def);
            try { _registry.Register(new PromptSubAgent(def, _newAgentLlm())); }
            catch (Exception ex) { Log.Error("AgentBuilder: Registrierung des neuen Helfers fehlgeschlagen", ex); }

            Probe.Decision("Helfer angelegt", def.Name, new { def.Description, triggers = def.Triggers?.Count ?? 0 });
            Log.Info("AgentBuilder: neuer Helfer angelegt", new { def.Name, def.Description });
            return $"Erledigt. Ich habe den Helfer „{def.Name}“ angelegt. Er kuemmert sich um: {def.Description}";
        }

        private const string ExtractPrompt =
@"Du baust aus dem Wunsch des Nutzers die Definition eines neuen Sprach-Helfers.
Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, sonst nichts:
{""name"":""..."",""description"":""..."",""triggers"":[""..."",""...""],""systemPrompt"":""...""}

- name: kurzer, eindeutiger Name (1-2 Woerter), z. B. ""Koch-Berater"".
- description: EIN Satz, wofuer der Helfer zustaendig ist.
- triggers: 2 bis 5 kurze Stichwoerter/Phrasen (kleingeschrieben), bei denen der Helfer gemeint ist.
- systemPrompt: Anweisung an den Helfer, wie er antwortet — sein Charakter und Stil. Kurz, natuerlich, gut vorlesbar, Deutsch, keine Sonderzeichen.

Gib NUR das JSON aus, keinen weiteren Text, keine Code-Bloecke.";

        /// <summary>
        /// Liest die Definition aus der LLM-Antwort. Tolerant gegen umgebenden Text/Code-Fences:
        /// schneidet vom ersten '{' bis zum letzten '}'. null = nichts Brauchbares. Public static = testbar.
        /// </summary>
        public static SubAgentDefinition? ParseDefinition(string? reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return null;

            int start = reply.IndexOf('{');
            int end = reply.LastIndexOf('}');
            if (start < 0 || end <= start) return null;
            var json = reply.Substring(start, end - start + 1);

            try
            {
                var opts = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                var def = JsonSerializer.Deserialize<SubAgentDefinition>(json, opts);
                if (def == null || string.IsNullOrWhiteSpace(def.Name)) return null;

                def.Name = def.Name.Trim();
                def.Description = (def.Description ?? string.Empty).Trim();
                def.SystemPrompt = (def.SystemPrompt ?? string.Empty).Trim();
                def.Triggers = (def.Triggers ?? new())
                    .Where(t => !string.IsNullOrWhiteSpace(t))
                    .Select(t => t.Trim().ToLowerInvariant())
                    .Distinct()
                    .ToList();
                return def;
            }
            catch (JsonException)
            {
                return null;
            }
        }
    }
}
