import Foundation

/// Laufzeit-Konfiguration aus der `.env`. Vollstaendige Portierung von
/// TerminalVoiceOverlay-Windows/Services/Config.cs: dort sind Whisper-Modell,
/// Sprache, Endpunkt, Gemini-Modell, Denkstufe, Abtastrate, Kanalzahl und die
/// Liste der Ziel-Terminals allesamt ueber die .env einstellbar. Auf macOS
/// standen diese Werte bisher fest im Code verteilt (Modellname im
/// GroqWhisperClient, Denkstufe im GeminiClient, Abtastrate im AudioRecorder,
/// Ziel-Apps im AppWatcher) und liessen sich ohne Neubau nicht aendern.
struct Config {
    // Groq
    let groqApiKey: String
    let whisperModel: String
    let whisperLang: String
    let whisperUrl: String

    // Gemini
    let geminiApiKey: String?
    let geminiModel: String
    let geminiThinkingLevel: String

    // Gemini Transcribe (Sprache-zu-Text, Alternative zu Groq Whisper).
    // Eigener Schluessel, weil der Free-Tier-Key fuer die Transkription ein
    // anderer sein darf als der Key fuer die Textkorrektur. Fehlt er, faellt
    // geminiTranscribeApiKey auf GEMINI_API_KEY zurueck.
    let geminiTranscribeApiKey: String?
    let geminiTranscribeBatchModel: String

    // Audio
    let audioSampleRate: Int
    let audioChannels: Int

    /// Zusaetzliche Ziel-Apps (Bundle-IDs) aus der .env. Windows-Pendant:
    /// TERMINAL_PROCESS_NAMES. Ergaenzt die eingebaute Liste in AppWatcher,
    /// ersetzt sie nicht — so bleibt eine unvollstaendige .env harmlos.
    let extraTerminalBundleIDs: [String]

    var geminiAvailable: Bool { !(geminiApiKey?.isEmpty ?? true) }
    var geminiTranscribeAvailable: Bool { !(geminiTranscribeApiKey?.isEmpty ?? true) }

    /// Die zuletzt geladene Konfiguration. Damit kommen auch Stellen an die
    /// Werte, die keine Instanz durchgereicht bekommen (AudioRecorder,
    /// AppWatcher) — 1:1 zur Windows-Fassung, wo `Config` ebenfalls einmal
    /// geladen und dann verteilt wird.
    private(set) static var current: Config?

    enum ConfigError: Error, LocalizedError {
        case missingApiKey(String)

        var errorDescription: String? {
            switch self {
            case .missingApiKey(let key):
                return "\(key) nicht gefunden. Bitte .env Datei anlegen."
            }
        }
    }

    static func load() throws -> Config {
        let env = parseEnvFile()
        guard let groqKey = env["GROQ_API_KEY"], !groqKey.isEmpty else {
            throw ConfigError.missingApiKey("GROQ_API_KEY")
        }
        let geminiKey = env["GEMINI_API_KEY"]
        let processNames = get(env, "TERMINAL_BUNDLE_IDS", "")
        let config = Config(
            groqApiKey: groqKey,
            whisperModel: get(env, "WHISPER_MODEL", "whisper-large-v3-turbo"),
            whisperLang: get(env, "WHISPER_LANG", "de"),
            whisperUrl: get(env, "WHISPER_URL", "https://api.groq.com/openai/v1/audio/transcriptions"),
            geminiApiKey: (geminiKey?.isEmpty ?? true) ? nil : geminiKey,
            geminiModel: get(env, "GEMINI_MODEL", "gemini-3.1-flash-lite"),
            geminiThinkingLevel: get(env, "GEMINI_THINKING_LEVEL", "MEDIUM"),
            geminiTranscribeApiKey: {
                let k = env["GEMINI_TRANSCRIBE_API_KEY"]
                return (k?.isEmpty ?? true) ? ((geminiKey?.isEmpty ?? true) ? nil : geminiKey) : k
            }(),
            geminiTranscribeBatchModel: get(env, "GEMINI_TRANSCRIBE_BATCH_MODEL", "gemini-3.5-transcribe"),
            audioSampleRate: getInt(env, "AUDIO_SAMPLE_RATE", 16000),
            audioChannels: getInt(env, "AUDIO_CHANNELS", 1),
            extraTerminalBundleIDs: processNames
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }
        )
        Config.current = config
        return config
    }

    private static func get(_ env: [String: String], _ key: String, _ fallback: String) -> String {
        guard let value = env[key], !value.isEmpty else { return fallback }
        return value
    }

    private static func getInt(_ env: [String: String], _ key: String, _ fallback: Int) -> Int {
        guard let raw = env[key], let value = Int(raw) else { return fallback }
        return value
    }

    private static func parseEnvFile() -> [String: String] {
        let bundleParent = Bundle.main.bundleURL.deletingLastPathComponent()
        let home = FileManager.default.homeDirectoryForCurrentUser
        // SK — Secret Keys Zentrale (cross-platform: $HOME/SK/VoiceOverlays/.env).
        // Alle API-Keys leben dort. Projekt-lokale Fallback-Pfade bleiben als Legacy.
        let searchPaths = [
            home.appendingPathComponent("SK/VoiceOverlays/.env"),
            bundleParent.appendingPathComponent(".env"),
            bundleParent.deletingLastPathComponent().appendingPathComponent(".env"),
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent(".env"),
            home.appendingPathComponent(".config/ClaudeCodexVoiceOverlay/.env"),
        ]

        for path in searchPaths {
            if let contents = try? String(contentsOf: path, encoding: .utf8) {
                return parseEnvContents(contents)
            }
        }
        return [:]
    }

    private static func parseEnvContents(_ contents: String) -> [String: String] {
        var result: [String: String] = [:]
        for line in contents.components(separatedBy: .newlines) {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty || trimmed.hasPrefix("#") { continue }
            let parts = trimmed.split(separator: "=", maxSplits: 1)
            if parts.count == 2 {
                let key = String(parts[0]).trimmingCharacters(in: .whitespaces)
                var value = String(parts[1]).trimmingCharacters(in: .whitespaces)
                // Remove surrounding quotes
                if (value.hasPrefix("\"") && value.hasSuffix("\"")) ||
                   (value.hasPrefix("'") && value.hasSuffix("'")) {
                    value = String(value.dropFirst().dropLast())
                }
                result[key] = value
            }
        }
        return result
    }
}
