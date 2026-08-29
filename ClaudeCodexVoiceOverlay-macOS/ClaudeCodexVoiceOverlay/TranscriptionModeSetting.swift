import Foundation

/// Wie woertlich Gemini transkribiert — Verbatim oder Smart. Liegt wie der
/// Modell-Schalter als winzige Datei im geteilten SK-Ordner, wirkt damit
/// sofort, braucht keinen Neustart und gilt fuer alle vier Overlays
/// (TVO/CVO, Windows/macOS) gemeinsam.
///
/// Inhalt: "verbatim" oder "smart". Fehlende/kaputte Datei = verbatim.
///
/// UNTERSCHIED (an derselben 64,5-s-Aufnahme mehrfach gemessen):
///   Tempo — praktisch gleich, Median 4,6 s (smart) gegen 6,1 s (verbatim);
///           die Schwankung durch Serverlast ist groesser als der Unterschied.
///   Text  — smart setzt Absaetze und wirft Fuellwoerter raus, formuliert
///           dabei aber um und LAESST WOERTER WEG: in jedem einzelnen
///           Durchgang wurde aus "Ich frage mich" ein "Frage mich"
///           (418 gegen 415 Zeichen, 66 gegen 65 Woerter).
/// Deshalb ist verbatim die Voreinstellung — bei einer Zahl, einem Dateinamen
/// oder einem "nicht" waere ein verschlucktes Wort teuer.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/TranscriptionModeSetting.cs.
enum TranscriptionModeSetting {

    static let verbatim = "verbatim"
    static let smart    = "smart"

    private static var path: String {
        (NSHomeDirectory() as NSString)
            .appendingPathComponent("SK/VoiceOverlays/transcription-mode.txt")
    }

    // mtime-Cache: die Datei wird pro Aufnahme gelesen, ein Datei-Zugriff je
    // Voice-Turn ist unnoetig.
    private static var cached = verbatim
    private static var cachedStamp: Date?
    private static let lock = NSLock()

    static var current: String {
        let p = path
        guard let attrs = try? FileManager.default.attributesOfItem(atPath: p),
              let stamp = attrs[.modificationDate] as? Date else {
            return verbatim
        }
        lock.lock()
        defer { lock.unlock() }
        if stamp != cachedStamp {
            let raw = (try? String(contentsOfFile: p, encoding: .utf8))?
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased() ?? ""
            cached = (raw == smart) ? smart : verbatim
            cachedStamp = stamp
        }
        return cached
    }

    static var useSmart: Bool { current == smart }

    static func save(_ mode: String) {
        let value = (mode == smart) ? smart : verbatim
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
