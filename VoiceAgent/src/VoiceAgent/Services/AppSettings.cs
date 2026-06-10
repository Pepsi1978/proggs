namespace VoiceAgent.Services
{
    /// <summary>
    /// Anbieter + Modell fuer EINE Rolle des Hauptagenten (z. B. Verstehen, Agenten-Bau).
    /// Leere Felder = die Rolle nutzt ihre eingebaute, sinnvolle Vorgabe
    /// (siehe LlmProviderFactory.ResolveRole) — so bleiben alte settings.json gueltig.
    /// </summary>
    public sealed class ModelRoleSetting
    {
        public string Provider { get; set; } = string.Empty;   // "gemini" | "claude" | "openai" | "codex"
        public string Model { get; set; } = string.Empty;

        /// <summary>True, wenn beide Felder bewusst gesetzt sind (sonst greift die Vorgabe).</summary>
        public bool IsSet => !string.IsNullOrWhiteSpace(Provider) && !string.IsNullOrWhiteSpace(Model);
    }

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
        public string LlmProvider { get; set; } = "gemini";        // "gemini" | "claude" | "openai" | "codex"
        public string LlmModel { get; set; } = DefaultGeminiModel;

        /// <summary>
        /// Denk-Aufwand fuer Codex (OpenAI ueber ChatGPT-Abo): "low" | "medium" | "high".
        /// Wird als reasoning.effort an chatgpt.com/backend-api/codex/responses geschickt.
        /// Gilt nur, wenn LlmProvider == "codex".
        /// </summary>
        public string CodexEffort { get; set; } = "medium";

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
        // Stille-Dauer bis ein Sprech-Haeppchen endet. 1,2 s statt frueher 3 s (Voice-Pipeline-
        // Recherche 2026-06-10: Konversation 300-550 ms, Diktat 1000-2000 ms). Gedankenpausen
        // bleiben sicher: der semantische Endpoint-Check sagt bei halben Saetzen "WEITER" und
        // sammelt weiter, das Sicherheitsnetz (EndpointMaxWaitMs) flusht notfalls. 3 s machten
        // jede Antwort traege UND liessen Hintergrundgeraeusche die Aufnahme endlos offen halten.
        public int SilenceMs { get; set; } = 1200;
        public int MinUtteranceMs { get; set; } = 350;         // kuerzere Schnipsel ignorieren (Huster etc.)
        // Harter Deckel pro Aussage (Voice-Pipeline-Almanach: Azure segmentiert intern ~15 s,
        // Rhasspy nutzt timeout 30 s). Findet die Stille-Erkennung KEIN Ende (Tastatur/Atmen
        // ueber der Schwelle), wird die Aussage hier finalisiert und VERARBEITET — nie verworfen.
        // Verhindert die beobachteten 41-s-/131-s-Endlos-Aufnahmen (Log 2026-06-10). 0 = aus.
        public int MaxUtteranceMs { get; set; } = 30000;

        // ----- Sprachgehalt-Vorfilter (gegen Whisper-Stille-Halluzination, Almanach groq-transkription §2.1/2.2) -----
        // Ein kurzer Geraeusch-Peak (Tastatur, Tuerklicken) startet die Aufnahme; danach folgt fast
        // nur Stille. MinUtteranceMs misst nur die DAUER, nicht den Sprachgehalt — so ein fast-stiller
        // Clip rutscht durch und Whisper halluziniert dann "Vielen Dank". Diese beiden Schwellen messen,
        // wie viel ECHTE Stimme in der fertigen Aussage steckt, und verwerfen sprach-arme Aussagen VOR
        // dem Senden an Groq. Konservativ eingestellt (echte kurze Befehle wie "ja"/"stop" bleiben erhalten).
        // WICHTIG: betrifft NUR die an Groq gesendete Aussage — NICHT den kontinuierlichen Wake-Word-Stream
        // (Wake-Almanach Bug #33: kein Frame-Filtering vor dem Streaming-KWS). 0 = Filter aus.
        public int MinVoicedMs { get; set; } = 200;            // Mindest-Summe an lauter (Sprach-)Zeit in der Aussage
        public double MinVoicedRatio { get; set; } = 0.15;     // Mindest-Anteil lauter Zeit an der Gesamtdauer (15%)

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

        // Helfer-Auswahl per LLM: der Hauptagent waehlt den passenden Unteragenten anhand seiner
        // Beschreibung (versteht freie Formulierungen), statt nur nach Stichwort. Faellt bei Fehler
        // oder fehlender Auswahl auf das bisherige Stichwort-Matching zurueck. Nutzt das IntentModel.
        public bool SubAgentRouting { get; set; } = true;

        // ----- Tiefes Verstehen + Modelle pro Rolle (Boss-Agent-Ueberarbeitung Phase 1) -----
        // Tiefes Verstehen: ein STARKES Modell (Brain-Rolle) versteht jede Aussage strukturiert
        // (Intent, Ziel-Helfer, normalisierter Auftrag, Zeit, Rueckfrage) BEVOR geroutet wird.
        // Ersetzt bei Erfolg den 3-Topf-IntentDetector; bei Fehler greift verlustfrei die alte Pipeline.
        public bool DeepUnderstanding { get; set; } = true;

        // Jede Rolle des Hauptagenten kann ein eigenes Gehirn bekommen (Frank stellt final ein).
        // Leer = sinnvolle Vorgabe (LlmProviderFactory.ResolveRole): Builder->Codex (wenn angemeldet),
        // Brain->starkes Modell (Codex/Claude, sonst Haupt-Gehirn), Endpoint/Intent->bisherige Werte.
        // Die Sprechen-Rolle (gesprochene Antwort) bleibt LlmProvider/LlmModel oben.
        public ModelRoleSetting RoleBrain { get; set; } = new();        // Verstehen/Orchestrieren
        public ModelRoleSetting RoleBuilder { get; set; } = new();      // Agenten-Bau
        public ModelRoleSetting RoleComputerUse { get; set; } = new();  // Befehls-Ableitung Computer Use
        public ModelRoleSetting RoleEndpoint { get; set; } = new();     // FERTIG/WEITER-Endpunkt-Check
        public ModelRoleSetting RoleIntent { get; set; } = new();       // Intent-Klassifikation + Helfer-Router

        // Computer Use: ob/wie der Agent den Rechner steuern darf (3 Stufen nach Hermes-Vorbild).
        // "off"  = aus (Default, sicher), "safe" = gefaehrliche Befehle erst nach gesprochenem "Ja",
        // "full" = Vollzugriff (alles ausser der Hardline-Blocklist). Wird zu ComputerUseMode geparst.
        public string ComputerUseMode { get; set; } = "off";

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
        // Welcher Start-Ton (ich-hoere-zu) gespielt wird — Pfad relativ zu assets/ (Default: bisheriger Ton).
        // Auswaehlbar in den Einstellungen aus assets/sounds/start (siehe ChimeLibrary).
        public string WakeChimeSound { get; set; } = "wakeword.wav";
        // Abfallender "Einschlaf"-Ton, wenn das Wachfenster nach Stille automatisch ablaeuft (WakeTimeoutMs)
        // und der Agent vom Zuhoeren zurueck in den Ruhezustand wechselt. Signalisiert hoerbar: "ab jetzt
        // hoere ich nicht mehr zu — du musst mich erst wieder wecken." NUR beim automatischen Timeout, NICHT
        // beim manuellen Abschalten (da hat es Frank ja selbst getan). Default an.
        public bool WakeSleepChimeEnabled { get; set; } = true;
        // Welcher Stop-Ton (ich-hoere-nicht-mehr-zu) gespielt wird — Pfad relativ zu assets/ (Default: bisheriger Ton).
        // Auswaehlbar in den Einstellungen aus assets/sounds/stop (siehe ChimeLibrary).
        public string WakeSleepChimeSound { get; set; } = "sleep.wav";

        // Melde-Ton: wird gespielt, wenn der Agent PROAKTIV etwas sagen will (Erinnerung/Funkspruch).
        // Pfad relativ zu assets/ (Default: bisheriger Ton). Auswaehlbar aus assets/sounds/message.
        public string ChimeSound { get; set; } = "message1.wav";

        // ----- Zeit -----
        // Damit der Agent die echte Uhrzeit kennt (LLMs raten sie sonst). Leer = automatisch
        // die System-Zeitzone dieses PCs; sonst eine TimeZoneInfo-Id (z.B. "W. Europe Standard Time").
        public string TimeZoneId { get; set; } = string.Empty;

        // ----- Farben im Gespraech (in den Einstellungen waehlbar) -----
        public string UserColor { get; set; } = "#4FC3F7";   // was Frank sagt (Cyan)
        public string AgentColor { get; set; } = "#F97316";  // was der Agent antwortet (Orange)

        // ----- Fenster-Verhalten -----
        // true  = beim Minimieren NUR ins Infobereich-Symbol (Tray) verstecken (aus der Taskleiste),
        //         per Doppelklick aufs Tray-Symbol oder Kontextmenue wieder oeffnen.
        // false = normales Minimieren in die Taskleiste (klassisches Verhalten).
        public bool MinimizeToTray { get; set; } = true;

        // ----- Erscheinungsbild (Hell/Dunkel-Profil) -----
        // "light" = warmes helles Profil (Standard, bisheriges Verhalten),
        // "dark"  = dunkles Profil, "system" = folgt dem Windows-Theme.
        // Wird vom ThemeManager beim Start angewendet und ueber den Topbar-Schalter
        // bzw. die Einstellungen gesetzt.
        public string Theme { get; set; } = "light";
    }
}
