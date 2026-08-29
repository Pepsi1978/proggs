import Foundation

/// Welches Sprache-zu-Text-Modell benutzt wird. Die Auswahl lebt — wie der
/// Woerterbuch-Schalter — als winzige Datei im geteilten SK-Ordner, damit sie
/// sofort wirkt, keinen Neustart braucht und sich die Overlays auf einem
/// Rechner teilen.
///
/// Inhalt: "groq" oder "gemini". Fehlende/kaputte Datei = groq.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/TranscriptionEngineSetting.cs.
/// Der dortige dritte Wert "gemini-live" (Streaming ueber WebSocket) wird hier
/// bewusst wie "gemini" behandelt: die Live-Variante ist nachweislich langsamer
/// (15,1 s gegen 4,4 s an derselben Aufnahme) und ungenauer (4,0 % gegen 2,6 %
/// Wortfehlerrate), sie existiert unter Windows nur zum Vergleich.
enum TranscriptionEngineSetting {

    static let groq   = "groq"
    static let gemini = "gemini"

    private static var path: String {
        (NSHomeDirectory() as NSString)
            .appendingPathComponent("SK/VoiceOverlays/transcription-engine.txt")
    }

    // mtime-Cache wie bei den Gemini-Prompts: die Datei wird pro Aufnahme
    // gelesen, ein Datei-Zugriff je Voice-Turn ist unnoetig.
    private static var cached = groq
    private static var cachedStamp: Date?
    private static let lock = NSLock()

    static var current: String {
        let p = path
        guard let attrs = try? FileManager.default.attributesOfItem(atPath: p),
              let stamp = attrs[.modificationDate] as? Date else {
            return groq
        }
        lock.lock()
        defer { lock.unlock() }
        if stamp != cachedStamp {
            let raw = (try? String(contentsOfFile: p, encoding: .utf8))?
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased() ?? ""
            // "gemini-live" faellt hier bewusst auf den Batch-Weg (siehe oben).
            cached = raw.hasPrefix(gemini) ? gemini : groq
            cachedStamp = stamp
        }
        return cached
    }

    static var useGemini: Bool { current == gemini }

    static func save(_ engine: String) {
        let value = (engine == gemini) ? gemini : groq
        let p = path
        let dir = (p as NSString).deletingLastPathComponent
        try? FileManager.default.createDirectory(atPath: dir,
                                                 withIntermediateDirectories: true)
        try? (value + "\n").write(toFile: p, atomically: true, encoding: .utf8)
        lock.lock()
        cachedStamp = nil
        lock.unlock()
    }
}
