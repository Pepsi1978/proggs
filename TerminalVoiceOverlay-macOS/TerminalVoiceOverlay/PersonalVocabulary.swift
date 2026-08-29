import Foundation

/// Das persoenliche Woerterbuch aus dem SK-Ordner, als Wortliste.
///
/// Dieselbe Datei und derselbe Ein/Aus-Schalter, die auch die
/// Gemini-Textkorrektur benutzt (GeminiClient liest sie dort als Prompt-Block).
/// Der Gemini-Transkriptionsweg gibt die Liste dagegen als customVocabulary
/// direkt an die Erkennung — die Begriffe werden damit schon beim Zuhoeren
/// richtig geschrieben, statt erst in der Nachkorrektur repariert zu werden.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/PersonalVocabulary.cs.
enum PersonalVocabulary {

    /// Die APIs nehmen bis zu 1000 Begriffe; laut Google sind die Ergebnisse
    /// bis etwa 100 am besten. Deckel daher bei 100.
    private static let maxWords = 100

    private static var dir: String {
        (NSHomeDirectory() as NSString).appendingPathComponent("SK/VoiceOverlays")
    }

    /// Die Begriffe, oder ein leeres Feld, wenn der Woerterbuch-Schalter aus
    /// ist oder die Datei fehlt. Wirft nie — ohne Woerterbuch zu transkribieren
    /// ist besser als gar nicht.
    static func load() -> [String] {
        let togglePath = (dir as NSString).appendingPathComponent("vocabulary-enabled.txt")
        let toggle = (try? String(contentsOfFile: togglePath, encoding: .utf8))?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        guard toggle == "true" else { return [] }

        let vocabPath = (dir as NSString).appendingPathComponent("personal-vocabulary.txt")
        guard let raw = try? String(contentsOfFile: vocabPath, encoding: .utf8) else { return [] }

        var seen = Set<String>()
        var words: [String] = []
        for token in raw.components(separatedBy: CharacterSet(charactersIn: "\n\r,;")) {
            let word = token.trimmingCharacters(in: .whitespaces)
            guard !word.isEmpty, !word.hasPrefix("#") else { continue }
            let key = word.lowercased()
            guard !seen.contains(key) else { continue }
            seen.insert(key)
            words.append(word)
            if words.count >= maxWords { break }
        }
        return words
    }
}
