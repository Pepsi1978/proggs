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
        public int SilenceMs { get; set; } = 3000;             // Stille-Dauer bis ein Sprech-Haeppchen endet (3s: erlaubt Gedankenpausen)
        public int MinUtteranceMs { get; set; } = 350;         // kuerzere Schnipsel ignorieren (Huster etc.)

        // Semantische Endpunkt-Erkennung: nach einer Pause prueft das LLM, ob der Gedanke
        // abgeschlossen ist (FERTIG) oder ob nur eine Denkpause vorliegt (WEITER → weiter zuhoeren).
        public bool SemanticEndpointing { get; set; } = true;
        public int EndpointMaxWaitMs { get; set; } = 4000;     // Sicherheitsnetz: nach so langer Stille trotzdem senden

        // FERTIG/WEITER ist eine triviale Aufgabe -> eigenes, guenstiges + schnelles Gemini-Modell,
        // unabhaengig vom (evtl. teureren) Haupt-Gehirn. In den Einstellungen aenderbar.
        public string EndpointModel { get; set; } = "gemini-3.1-flash-lite";

        // Intent-Erkennung: ein eigenes billiges Gemini-Modell ordnet die Aussage VOR der Antwort
        // in AUFGABE/FRAGE/PLAUDEREI ein (statt es aus der Antwort zu raten). Macht die Live-Sonde exakt.
        public bool IntentDetection { get; set; } = true;
        public string IntentModel { get; set; } = "gemini-3.1-flash-lite";

        // ----- Wake-Word ("Okay Computer") -----
        // Wenn an, hoert der Agent im Ruhezustand nur auf das Weckwort und verarbeitet erst
        // danach (fuer WakeTimeoutMs) normal. Wenn aus, wird wie bisher JEDE Aussage verarbeitet.
        public bool WakeWordEnabled { get; set; } = false;     // Opt-in: bestehendes Verhalten bleibt Default
        // Anzeige-/Greeting-Text des aktiven Weckworts. Die ERKENNUNG nutzt die gebundelte,
        // vor-tokenisierte keywords.txt (englische Phonetik, gigaspeech). Anderes Wort:
        // keywords.txt neu erzeugen (siehe assets/wakeword-model/README.md).
        public string WakeWord { get; set; } = "Okay Computer";
        public int WakeTimeoutMs { get; set; } = 60000;        // aktives Fenster nach dem Wecken (60 s)
        public bool WakeChimeEnabled { get; set; } = true;     // Erkennungston beim Wecken (danach sofort sprechen)

        // ----- Zeit -----
        // Damit der Agent die echte Uhrzeit kennt (LLMs raten sie sonst). Leer = automatisch
        // die System-Zeitzone dieses PCs; sonst eine TimeZoneInfo-Id (z.B. "W. Europe Standard Time").
        public string TimeZoneId { get; set; } = string.Empty;

        // ----- Farben im Gespraech (in den Einstellungen waehlbar) -----
        public string UserColor { get; set; } = "#4FC3F7";   // was Frank sagt (Cyan)
        public string AgentColor { get; set; } = "#F97316";  // was der Agent antwortet (Orange)
    }
}
