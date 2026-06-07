namespace VoiceAgent.Services
{
    /// <summary>
    /// Persistente Benutzer-Einstellungen des Hauptagenten. Wird als JSON unter
    /// %LOCALAPPDATA%\VoiceAgent\settings.json gespeichert. API-Schluessel liegen
    /// NICHT hier, sondern in ~/SK/VoiceAgent/keys.json (siehe Config).
    /// </summary>
    public sealed class AppSettings
    {
        // ----- Defaults (an EINER Stelle aenderbar) -----
        // Verifiziert 2026-06-07: stabile GA-Model-ID (nicht die -preview-Variante).
        public const string DefaultGeminiModel = "gemini-3.1-flash-lite";
        public const string DefaultTtsVoice = "de-DE-Chirp3-HD-Kore";
        public const string DefaultTtsLanguage = "de-DE";
        public const string DefaultSttModel = "whisper-large-v3-turbo";
        public const string DefaultSttLanguage = "de";

        // ----- Gehirn (LLM) -----
        public string LlmProvider { get; set; } = "gemini";        // "gemini" | "claude" | "openai"
        public string LlmModel { get; set; } = DefaultGeminiModel;

        /// <summary>
        /// System-Prompt des Hauptagenten. Leer = der eingebaute Standard-Prompt
        /// (BossAgentPrompt.Default) wird zur Laufzeit verwendet.
        /// </summary>
        public string SystemPrompt { get; set; } = string.Empty;

        // ----- Sprachausgabe (TTS) -----
        public string TtsVoiceName { get; set; } = DefaultTtsVoice;
        public string TtsLanguageCode { get; set; } = DefaultTtsLanguage;

        // ----- Spracheingabe (STT) -----
        public string SttModel { get; set; } = DefaultSttModel;
        public string SttLanguage { get; set; } = DefaultSttLanguage;

        // ----- Mikrofon / Voice-Loop -----
        public bool MicEnabled { get; set; } = true;
        public double SilenceThreshold { get; set; } = 0.012;  // RMS-Schwelle: darunter = Stille
        public int SilenceMs { get; set; } = 900;              // Stille-Dauer bis "Aussage zu Ende"
        public int MinUtteranceMs { get; set; } = 350;         // kuerzere Schnipsel ignorieren (Huster etc.)
    }
}
