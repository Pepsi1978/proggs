using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Die Rollen des Hauptagenten. Jede Rolle kann in den Einstellungen ein eigenes
    /// Modell (+Anbieter) bekommen — verschiedene Aufgaben brauchen verschieden starke Gehirne
    /// (Almanach orchestrator-agent 1.1/1.6: das Verstehens-Gehirn darf NICHT das billigste sein).
    /// </summary>
    public enum ModelRole
    {
        /// <summary>Die gesprochene Antwort des Hauptagenten (latenzkritisch).</summary>
        Speech,
        /// <summary>Verstehen + Orchestrieren: der strukturierte Verstehens-Schritt (stark!).</summary>
        Brain,
        /// <summary>Agenten-Bau: Definition neuer Helfer ableiten (Default: Codex).</summary>
        Builder,
        /// <summary>Befehls-Ableitung fuer Computer Use.</summary>
        ComputerUse,
        /// <summary>FERTIG/WEITER-Endpunkt-Check (latenzkritisch, billig).</summary>
        Endpoint,
        /// <summary>Intent-Klassifikation (Fallback-Pfad) + Helfer-Router.</summary>
        Intent,
    }

    /// <summary>
    /// Erzeugt den passenden LLM-Provider anhand der Einstellungen. Der API-Schluessel
    /// wird frisch aus dem SK-Ordner gelesen, damit Aenderungen ohne Neustart greifen.
    /// </summary>
    public static class LlmProviderFactory
    {
        /// <summary>Provider fuer die Haupt-Antwort (Sprechen-Rolle) — bisheriges Verhalten.</summary>
        public static ILlmProvider Create(AppSettings settings)
            => CreateForRole(settings, ModelRole.Speech);

        /// <summary>Provider fuer eine bestimmte Rolle des Hauptagenten (Modelle pro Rolle).</summary>
        public static ILlmProvider CreateForRole(AppSettings settings, ModelRole role)
        {
            var (provider, model) = ResolveRole(settings, role);
            Log.Info($"LlmProviderFactory: Rolle {role} -> Provider '{provider}', Modell '{model}'");
            return provider switch
            {
                "claude" => new ClaudeProvider(Config.ReadApiKey("claude"), model),
                "openai" => new OpenAiProvider(Config.ReadApiKey("openai"), model),
                // Codex nutzt das ChatGPT-Abo (Geraetecode-Login) statt eines API-Schluessels,
                // daher kein Config.ReadApiKey — die Tokens liegen in ~/SK/VoiceAgent/codex-auth.json.
                // Brain-Rolle (Verstehens-Schritt) laeuft latenzkritisch in JEDEM Voice-Turn und ist
                // ein JSON-Klassifikationsjob: Effort "low" statt des globalen CodexEffort (Frank:
                // "high") — gemessen 6,6 s pro Turn mit high (Log 2026-06-10), das Modell bleibt
                // gpt-5.5 (stark, Almanach 1.1). Builder/ComputerUse behalten den vollen Effort.
                "codex" => new CodexProvider(model, role == ModelRole.Brain ? "low" : settings.CodexEffort),
                _ => new GeminiProvider(Config.ReadApiKey("gemini"), model),
            };
        }

        /// <summary>
        /// Loest eine Rolle zu (Anbieter, Modell) auf. Eine in den Einstellungen GESETZTE Rolle
        /// gewinnt immer; leere Rollen bekommen eine sinnvolle Vorgabe — funktionserhaltend
        /// (Endpoint/Intent = bisherige Legacy-Felder) bzw. nach Franks Vorgabe (Builder = Codex,
        /// Brain = starkes Modell, NICHT Flash-lite). Public static = isoliert nachvollziehbar/testbar.
        /// </summary>
        public static (string Provider, string Model) ResolveRole(AppSettings settings, ModelRole role)
        {
            var set = role switch
            {
                ModelRole.Brain => settings.RoleBrain,
                ModelRole.Builder => settings.RoleBuilder,
                ModelRole.ComputerUse => settings.RoleComputerUse,
                ModelRole.Endpoint => settings.RoleEndpoint,
                ModelRole.Intent => settings.RoleIntent,
                _ => null,
            };
            if (set != null && set.IsSet)
                return (set.Provider.Trim().ToLowerInvariant(), set.Model.Trim());

            switch (role)
            {
                case ModelRole.Endpoint:   // bisheriges Verhalten: eigenes guenstiges Gemini-Modell
                    return ("gemini", FallbackModel(settings.EndpointModel));
                case ModelRole.Intent:     // bisheriges Verhalten: eigenes guenstiges Gemini-Modell
                    return ("gemini", FallbackModel(settings.IntentModel));
                case ModelRole.ComputerUse: // bisheriges Verhalten: Befehls-Ableitung auf dem Intent-Modell
                    return ("gemini", FallbackModel(settings.IntentModel));
                case ModelRole.Builder:    // Franks Vorgabe: Agenten-Bau ueber Codex (ChatGPT-Abo)
                    if (CodexAuth.IsLoggedIn()) return ("codex", CodexModels.Fallback[0]);
                    return ResolveStrong(settings);
                case ModelRole.Brain:      // Verstehen/Orchestrieren: NIE das billigste Modell (Almanach 1.1)
                    return ResolveStrong(settings);
                default:                   // Speech: das eingestellte Haupt-Gehirn (Legacy-Felder)
                    return ((settings.LlmProvider ?? "gemini").ToLowerInvariant(),
                            FallbackModel(settings.LlmModel));
            }
        }

        /// <summary>
        /// Staerkstes verfuegbares Gehirn: Codex (wenn angemeldet) -> Claude (wenn Schluessel da)
        /// -> sonst das Haupt-Gehirn. Graceful Degradation statt Hard-Fail, Frank stellt final ein.
        /// </summary>
        private static (string Provider, string Model) ResolveStrong(AppSettings settings)
        {
            if (CodexAuth.IsLoggedIn()) return ("codex", CodexModels.Fallback[0]);
            if (!string.IsNullOrWhiteSpace(Config.ReadApiKey("claude"))) return ("claude", "claude-sonnet-4-6");
            return ((settings.LlmProvider ?? "gemini").ToLowerInvariant(), FallbackModel(settings.LlmModel));
        }

        private static string FallbackModel(string? model)
            => string.IsNullOrWhiteSpace(model) ? AppSettings.DefaultGeminiModel : model.Trim();
    }
}
