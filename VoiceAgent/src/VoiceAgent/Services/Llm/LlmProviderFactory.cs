using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Erzeugt den passenden LLM-Provider anhand der Einstellungen. Der API-Schluessel
    /// wird frisch aus dem SK-Ordner gelesen, damit Aenderungen ohne Neustart greifen.
    /// </summary>
    public static class LlmProviderFactory
    {
        public static ILlmProvider Create(AppSettings settings)
        {
            var provider = (settings.LlmProvider ?? "gemini").ToLowerInvariant();
            Log.Info($"LlmProviderFactory: erzeuge Provider '{provider}', Modell '{settings.LlmModel}'");
            return provider switch
            {
                "claude" => new ClaudeProvider(Config.ReadApiKey("claude"), settings.LlmModel),
                "openai" => new OpenAiProvider(Config.ReadApiKey("openai"), settings.LlmModel),
                // Codex nutzt das ChatGPT-Abo (Geraetecode-Login) statt eines API-Schluessels,
                // daher kein Config.ReadApiKey — die Tokens liegen in ~/SK/VoiceAgent/codex-auth.json.
                "codex" => new CodexProvider(settings.LlmModel, settings.CodexEffort),
                _ => new GeminiProvider(Config.ReadApiKey("gemini"), settings.LlmModel),
            };
        }
    }
}
