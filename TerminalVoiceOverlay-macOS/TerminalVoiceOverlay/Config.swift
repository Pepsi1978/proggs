import Foundation

struct Config {
    let groqApiKey: String
    let geminiApiKey: String?

    var geminiAvailable: Bool { geminiApiKey != nil && !geminiApiKey!.isEmpty }

    static func load() -> Config {
        let env = parseEnvFile()
        guard let groqKey = env["GROQ_API_KEY"], !groqKey.isEmpty else {
            fatalError("GROQ_API_KEY nicht gefunden. Bitte .env Datei anlegen.")
        }
        return Config(
            groqApiKey: groqKey,
            geminiApiKey: env["GEMINI_API_KEY"]
        )
    }

    private static func parseEnvFile() -> [String: String] {
        let bundleParent = Bundle.main.bundleURL.deletingLastPathComponent()
        let searchPaths = [
            // Next to .app bundle (build/)
            bundleParent.appendingPathComponent(".env"),
            // One level up from build/ (project root)
            bundleParent.deletingLastPathComponent().appendingPathComponent(".env"),
            // Current working directory
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent(".env"),
            // ~/.config/TerminalVoiceOverlay/.env
            FileManager.default.homeDirectoryForCurrentUser
                .appendingPathComponent(".config/TerminalVoiceOverlay/.env"),
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
