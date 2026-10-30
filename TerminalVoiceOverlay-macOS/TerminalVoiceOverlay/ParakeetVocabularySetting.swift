import Foundation

/// Das Woerterbuch der Offline-Transkription (Parakeet primeline). Eigener Schalter und eigene Liste,
/// getrennt von vocabulary-enabled.txt / personal-vocabulary.txt, die zur Gemini-Korrektur gehoeren.
///
/// Beides lebt als winzige Datei im geteilten SK-Ordner, wirkt also ab der naechsten Aufnahme und gilt
/// fuer TVO und CVO zugleich:
///   parakeet-vocabulary-enabled.txt  "false" = aus, sonst an; fehlt die Datei: an
///   parakeet-vocabulary.txt          Begriffe durch Komma oder Zeilenumbruch getrennt; fehlt die Datei: Startliste
///
/// Gleiche Datei in TerminalVoiceOverlay-macOS und ClaudeCodexVoiceOverlay-macOS,
/// Gegenstueck in den Windows-Overlays: Services/ParakeetVocabularySetting.cs.
enum ParakeetVocabularySetting {

    /// Startliste, gleicher Stand wie ParakeetUeberJarvis.STANDARD_WOERTERBUCH (Modul M1.4).
    static let starterList = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic"

    /// Wie am Handy: mehr als 100 Begriffe gehen nicht in die Beam-Suche.
    private static let maxWords = 100

    private static var dir: String {
        (NSHomeDirectory() as NSString).appendingPathComponent("SK/VoiceOverlays")
    }
    private static var enabledPath: String { (dir as NSString).appendingPathComponent("parakeet-vocabulary-enabled.txt") }
    private static var vocabularyPath: String { (dir as NSString).appendingPathComponent("parakeet-vocabulary.txt") }

    static var isEnabled: Bool {
        guard let raw = try? String(contentsOfFile: enabledPath, encoding: .utf8) else { return true }
        return raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() != "false"
    }

    /// Der Text fuers Einstellungsfeld. Eine leere Datei bleibt leer: Das ist eine bewusst geleerte Liste.
    static func loadText() -> String {
        (try? String(contentsOfFile: vocabularyPath, encoding: .utf8)) ?? starterList
    }

    /// Schreibt nur bei echter Aenderung: Solange nichts geaendert wird, bleibt es bei fehlender Datei
    /// und damit bei der jeweils aktuellen Startliste.
    static func saveEnabled(_ enabled: Bool) {
        guard enabled != isEnabled else { return }
        write(enabled ? "true" : "false", to: enabledPath)
    }

    static func saveText(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed != loadText().trimmingCharacters(in: .whitespacesAndNewlines) else { return }
        write(trimmed, to: vocabularyPath)
    }

    private static func write(_ text: String, to path: String) {
        do {
            try FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
            try (text + "\n").write(toFile: path, atomically: true, encoding: .utf8)
        } catch {
            DiagLog.warn("Settings", "parakeet_vocabulary_save_failed", [("error", error.localizedDescription)])
        }
    }

    /// Die Wortliste fuer die naechste Aufnahme; leer, wenn der Schalter aus ist oder keine Begriffe da sind.
    static func hotwords() -> String {
        isEnabled ? format(loadText()) : ""
    }

    /// Format von sherpa-onnx: Begriffe durch "/" getrennt. "/", ":" und "#" haben dort eine eigene Bedeutung
    /// und werden zu Leerzeichen. Doppelte (ohne Gross/Klein) fallen weg, hoechstens 100 Begriffe.
    static func format(_ text: String) -> String {
        var seen = Set<String>()
        var words: [String] = []
        for token in text.components(separatedBy: CharacterSet(charactersIn: "\n\r,;")) {
            let word = token.replacingOccurrences(of: "[/:#]", with: " ", options: .regularExpression)
                .trimmingCharacters(in: .whitespaces)
            guard !word.isEmpty, seen.insert(word.lowercased()).inserted else { continue }
            words.append(word)
            if words.count >= maxWords { break }
        }
        return words.joined(separator: "/")
    }
}
