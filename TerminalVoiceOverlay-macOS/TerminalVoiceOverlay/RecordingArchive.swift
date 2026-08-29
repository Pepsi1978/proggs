import Foundation

/// Bewahrt die zuletzt aufgenommenen Diktate auf, statt sie nach dem Turn zu loeschen.
///
/// Vorfall 29.08.2026 (Windows-Schwester-App): Ein 15,4-Minuten-Diktat ergab eine 29,5-MB-WAV,
/// die Groq mit HTTP 413 abwies; die Datei wurde danach geloescht — der gesprochene Text war
/// endgueltig weg und musste komplett neu gesprochen werden. Seitdem gilt: **eine Aufnahme wird
/// nie mehr direkt geloescht**. Jede Aufnahme wandert hierher, die letzten `keepCount` bleiben
/// erhalten und koennen ueber Rechtsklick auf das Mikrofon erneut transkribiert werden.
///
/// Bewusst klein gehalten: reines Dateisystem, kein Zustand im Speicher, damit das Archiv auch
/// nach einem Absturz der App vollstaendig ist. 1:1-Pendant zu `RecordingArchive.cs` unter Windows.
enum RecordingArchive {

    /// So viele Aufnahmen bleiben erhalten (die letzte und die vorletzte).
    static let keepCount = 2

    private static let filePrefix = "aufnahme_"

    /// ~/Library/Application Support/TerminalVoiceOverlay/aufnahmen
    static var directory: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent("Library/Application Support")
        let dir = base
            .appendingPathComponent("TerminalVoiceOverlay", isDirectory: true)
            .appendingPathComponent("aufnahmen", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    /// Verschiebt eine frisch aufgenommene WAV ins Archiv und raeumt alles ausser den letzten
    /// `keepCount` Aufnahmen ab. Gibt den neuen Pfad zurueck (nil, wenn nichts zu archivieren war).
    /// Wirft nie — das Archiv darf einen Turn niemals zum Scheitern bringen.
    @discardableResult
    static func archive(_ fileURL: URL?, success: Bool) -> URL? {
        guard let fileURL = fileURL else { return nil }
        let fm = FileManager.default
        guard fm.fileExists(atPath: fileURL.path) else { return nil }

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd_HH-mm-ss"
        let stamp = formatter.string(from: Date())
        let status = success ? "ok" : "fehler"

        var target = directory.appendingPathComponent("\(filePrefix)\(stamp)_\(status).wav")
        // Zwei Aufnahmen in derselben Sekunde: Zaehler anhaengen statt ueberschreiben.
        var counter = 2
        while fm.fileExists(atPath: target.path) && counter < 100 {
            target = directory.appendingPathComponent("\(filePrefix)\(stamp)_\(status)_\(counter).wav")
            counter += 1
        }

        do {
            try fm.moveItem(at: fileURL, to: target)
            let attrs = try? fm.attributesOfItem(atPath: target.path)
            let bytes = (attrs?[.size] as? NSNumber)?.intValue ?? 0
            DiagLog.write("Recording", "archived",
                          [("path", target.path), ("bytes", bytes), ("success", success)])
            prune()
            return target
        } catch {
            DiagLog.warn("Recording", "archive_failed", [("err", error.localizedDescription)])
            return nil
        }
    }

    /// Die aufbewahrten Aufnahmen, neueste zuerst (hoechstens `keepCount`).
    static func recent() -> [URL] {
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.contentModificationDateKey],
            options: [.skipsHiddenFiles]
        ) else { return [] }

        return files
            .filter { $0.lastPathComponent.hasPrefix(filePrefix) && $0.pathExtension == "wav" }
            .sorted { modified($0) > modified($1) }
            .prefix(keepCount)
            .map { $0 }
    }

    /// Die n-letzte Aufnahme (0 = die letzte, 1 = die vorletzte) oder nil, wenn es sie nicht gibt.
    static func at(_ index: Int) -> URL? {
        let all = recent()
        return index >= 0 && index < all.count ? all[index] : nil
    }

    /// Loescht alles ausser den letzten `keepCount` Aufnahmen.
    private static func prune() {
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.contentModificationDateKey],
            options: [.skipsHiddenFiles]
        ) else { return }

        let stale = files
            .filter { $0.lastPathComponent.hasPrefix(filePrefix) && $0.pathExtension == "wav" }
            .sorted { modified($0) > modified($1) }
            .dropFirst(keepCount)

        for old in stale {
            do {
                try fm.removeItem(at: old)
                DiagLog.write("Recording", "archive_pruned", [("path", old.path)])
            } catch {
                // Gesperrt -> die naechste Aufnahme raeumt sie ab.
            }
        }
    }

    private static func modified(_ url: URL) -> Date {
        (try? url.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
    }
}
