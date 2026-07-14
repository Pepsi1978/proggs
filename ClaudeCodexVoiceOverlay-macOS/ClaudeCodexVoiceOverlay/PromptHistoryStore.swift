import Foundation

/// Ein einzelner Eintrag in der Prompt-Historie. Plain-Swift-Pendant zum
/// Windows `PromptHistoryEntry` (PromptHistoryService.cs). Feldnamen sind
/// bewusst camelCase und identisch zum C#-Pendant nach dessen JSON-Naming-
/// Policy — so kann die JSON-Datei zwischen Mac und Windows direkt
/// gespiegelt werden (Phase 5: Google Drive Sync).
struct PBHistoryEntry: Codable, Equatable {
    var id: String
    var text: String
    var title: String
    var timestamp: Date
    var updatedAt: Date? = nil
    var archivedAt: Date? = nil
}

/// Liest und schreibt die Prompt-Historie aus einer JSON-Datei. Bei mehr
/// als 100 aktiven Eintraegen wandert der aelteste in eine Markdown-
/// Archivdatei im Unterordner `Terminal Archiv` — eine MD-Datei haelt
/// maximal 500 Eintraege, danach wird eine neue Datei mit fortlaufender
/// Nummer angelegt. Innerhalb jeder MD-Datei steht der aktuellste Eintrag
/// oben, damit der Benutzer beim Oeffnen sofort die neuesten archivierten
/// Prompts sieht.
///
/// Thread-sicherheit: Alle Lese-/Schreibpfade gehen ueber dieselbe
/// `DispatchQueue` (serial). Atomares Schreiben via Temp-Datei + Replace,
/// damit ein Crash mitten im Schreiben niemals eine korrupte history.json
/// hinterlaesst.
final class PromptHistoryStore {

    static let shared = PromptHistoryStore()

    private static let maxActiveEntries = 100
    private static let maxEntriesPerArchive = 500
    private static let archiveFolderName = "Claude Archiv"

    private let baseDir: URL
    private let historyFileURL: URL
    private let archiveDirURL: URL
    private let queue = DispatchQueue(label: "tvo.PromptHistoryStore.queue")

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f
    }()

    private init() {
        let support = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        ).first ?? URL(fileURLWithPath: NSHomeDirectory())
        baseDir = support
            .appendingPathComponent("ClaudeCodexVoiceOverlay", isDirectory: true)
            .appendingPathComponent("history", isDirectory: true)
        historyFileURL = baseDir.appendingPathComponent("prompt-history.json")
        archiveDirURL = baseDir.appendingPathComponent(Self.archiveFolderName, isDirectory: true)
        try? FileManager.default.createDirectory(at: baseDir, withIntermediateDirectories: true)
        try? FileManager.default.createDirectory(at: archiveDirURL, withIntermediateDirectories: true)
    }

    /// Pfad zum lokalen JSON, fuer Diagnose und Cloud-Sync (Phase 5).
    var historyFilePath: String { historyFileURL.path }

    /// Pfad zum Archiv-Ordner, fuer Cloud-Sync (Phase 5).
    var archiveDirectoryPath: String { archiveDirURL.path }

    /// Liest die aktive Historie. Neuester Eintrag steht an Index 0.
    /// Ein fehlendes oder kaputtes JSON liefert eine leere Liste — die
    /// Historie ist Add-Only-Wissen, korrupte Eintraege werden nicht in
    /// das laufende Programm geschleppt.
    func load(completion: @escaping ([PBHistoryEntry]) -> Void) {
        queue.async { [weak self] in
            guard let self = self else { completion([]); return }
            let entries = Array(self.loadUnlocked()
                .filter { $0.archivedAt == nil }
                .sorted { $0.timestamp > $1.timestamp }
                .prefix(Self.maxActiveEntries))
            DispatchQueue.main.async { completion(entries) }
        }
    }

    /// Liest aktive Einträge und Archiv-Tombstones für einen verlustfreien Cloud-Merge.
    func loadAll(completion: @escaping ([PBHistoryEntry]) -> Void) {
        queue.async { [weak self] in
            guard let self = self else { completion([]); return }
            let entries = self.loadUnlocked()
            DispatchQueue.main.async { completion(entries) }
        }
    }

    /// Fuegt einen neuen Eintrag oben in der Historie hinzu, schreibt die
    /// Datei atomar zurueck und schiebt ueberzaehlige Eintraege ins MD-
    /// Archiv. Der Aufrufer ist dafuer verantwortlich, den Titel vorab zu
    /// erzeugen (z.B. via `GeminiClient.generateTitle`).
    func append(text: String, title: String, completion: @escaping (PBHistoryEntry) -> Void) {
        let safeTitle = title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? GeminiClient.fallbackTitle(from: text)
            : title.trimmingCharacters(in: .whitespacesAndNewlines)
        let now = Date()
        let entry = PBHistoryEntry(
            id: UUID().uuidString,
            text: text,
            title: safeTitle,
            timestamp: now,
            updatedAt: now
        )

        queue.async { [weak self] in
            guard let self = self else { return }
            var entries = self.loadUnlocked()
            entries.insert(entry, at: 0)

            self.archiveOverflowUnlocked(&entries)
            self.saveUnlocked(entries)
            DispatchQueue.main.async { completion(entry) }
        }
    }

    /// Ersetzt die gesamte aktive Historie durch eine neue Liste — wird
    /// vom Cloud-Sync genutzt nachdem die Cloud-Eintraege mit der
    /// lokalen Liste gemergt wurden. Wendet dieselben Schwellen an wie
    /// `append`: aelteste Eintraege wandern automatisch ins Archiv,
    /// sobald die aktive Liste die 100-Marke ueberschreitet.
    func replaceAll(entries: [PBHistoryEntry], completion: @escaping () -> Void) {
        queue.async { [weak self] in
            guard let self = self else { return }
            var sorted = entries.sorted { $0.timestamp > $1.timestamp }
            self.archiveOverflowUnlocked(&sorted)
            self.saveUnlocked(sorted)
            DispatchQueue.main.async { completion() }
        }
    }

    /// Liest die rohe JSON-Datei der lokalen Historie (oder leerer String
    /// wenn nicht vorhanden). Wird fuer den Cloud-Upload genutzt — wir
    /// laden den Datei-Inhalt 1:1 hoch, ohne ihn nochmal zu serialisieren,
    /// damit Mac und Windows wirklich die exakt gleichen Bytes
    /// austauschen.
    func rawJsonFromDisk() -> String {
        guard FileManager.default.fileExists(atPath: historyFileURL.path),
              let s = try? String(contentsOf: historyFileURL, encoding: .utf8)
        else { return "[]" }
        return s
    }

    /// Mergt eine Cloud-Historie-JSON in die lokale Liste: Eintraege per
    /// ID-Dedup zusammenfuehren. Lokal gewinnt bei Doppel-IDs (kann
    /// frischen KI-Titel enthalten), unbekannte Cloud-IDs werden
    /// uebernommen.
    static func merge(local: [PBHistoryEntry], cloudJson: String) -> [PBHistoryEntry] {
        // Cloud-JSON entschluesseln. Bei kaputtem JSON: lokale Liste
        // unveraendert zurueckgeben — Cloud darf den lokalen Stand nie
        // zerstoeren.
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let str = try decoder.singleValueContainer().decode(String.self)
            if str.hasPrefix("0001-01-01") { return Date(timeIntervalSince1970: 0) }
            let f1 = ISO8601DateFormatter()
            f1.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let d = f1.date(from: str) { return d }
            let f2 = ISO8601DateFormatter()
            f2.formatOptions = [.withInternetDateTime]
            if let d = f2.date(from: str) { return d }
            throw DecodingError.dataCorruptedError(
                in: try decoder.singleValueContainer(),
                debugDescription: "Invalid date: \(str)")
        }
        var cloud: [PBHistoryEntry] = []
        if let data = cloudJson.data(using: .utf8),
           let parsed = try? decoder.decode([PBHistoryEntry].self, from: data) {
            cloud = parsed
        }
        var byId: [String: PBHistoryEntry] = [:]
        for e in local where !e.id.isEmpty { byId[e.id.lowercased()] = e }
        for e in cloud where !e.id.isEmpty {
            let key = e.id.lowercased()
            if let existing = byId[key] {
                byId[key] = mergeRevision(existing, e)
            } else {
                byId[key] = e
            }
        }
        return byId.values.sorted { $0.timestamp > $1.timestamp }
    }

    private static func mergeRevision(_ left: PBHistoryEntry, _ right: PBHistoryEntry) -> PBHistoryEntry {
        var winner = effectiveUpdatedAt(right) > effectiveUpdatedAt(left) ? right : left
        let archivedAt = [left.archivedAt, right.archivedAt].compactMap { $0 }.max()
        if let archivedAt {
            winner.archivedAt = archivedAt
            if (winner.updatedAt ?? winner.timestamp) < archivedAt {
                winner.updatedAt = archivedAt
            }
        }
        return winner
    }

    private static func effectiveUpdatedAt(_ entry: PBHistoryEntry) -> Date {
        max(entry.updatedAt ?? entry.timestamp, entry.archivedAt ?? entry.timestamp)
    }

    /// Aktualisiert den Titel eines bestehenden Eintrags. Wird vom
    /// AppDelegate aufgerufen sobald der KI-Titel von Gemini eingetroffen
    /// ist — der Eintrag wurde vorher schon mit einem Fallback angelegt.
    func updateTitle(entryId: String, newTitle: String) {
        guard !entryId.isEmpty else { return }
        queue.async { [weak self] in
            guard let self = self else { return }
            var entries = self.loadUnlocked()
            guard let idx = entries.firstIndex(where: { $0.id == entryId }) else { return }
            let cleaned = newTitle.trimmingCharacters(in: .whitespacesAndNewlines)
            entries[idx].title = cleaned.isEmpty
                ? GeminiClient.fallbackTitle(from: entries[idx].text)
                : cleaned
            entries[idx].updatedAt = Date()
            self.saveUnlocked(entries)
        }
    }

    /// Aktualisiert den Volltext eines bestehenden Eintrags. Wird vom
    /// PromptHistoryPanel aufgerufen nachdem der Benutzer einen Eintrag
    /// im Editor-Sheet veraendert und mit Speichern bestaetigt hat.
    /// Titel und Zeitstempel bleiben unveraendert — Titel ist KI-generiert,
    /// Zeitstempel ist der urspruengliche Sortier-Schluessel.
    func updateText(entryId: String, newText: String, completion: @escaping () -> Void) {
        guard !entryId.isEmpty else {
            DispatchQueue.main.async { completion() }
            return
        }
        queue.async { [weak self] in
            guard let self = self else {
                DispatchQueue.main.async { completion() }
                return
            }
            var entries = self.loadUnlocked()
            if let idx = entries.firstIndex(where: { $0.id == entryId }) {
                entries[idx].text = newText
                entries[idx].updatedAt = Date()
                self.saveUnlocked(entries)
            }
            DispatchQueue.main.async { completion() }
        }
    }

    // MARK: - Interne Helpers (laufen alle innerhalb der serial queue)

    private func loadUnlocked() -> [PBHistoryEntry] {
        guard FileManager.default.fileExists(atPath: historyFileURL.path) else {
            return []
        }
        do {
            let data = try Data(contentsOf: historyFileURL)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .custom { decoder in
                let str = try decoder.singleValueContainer().decode(String.self)
                if str.hasPrefix("0001-01-01") { return Date(timeIntervalSince1970: 0) }
                if let d = Self.isoFormatter.date(from: str) { return d }
                // Fallback ohne Bruchteilssekunden
                let f = ISO8601DateFormatter()
                f.formatOptions = [.withInternetDateTime]
                if let d = f.date(from: str) { return d }
                throw DecodingError.dataCorruptedError(
                    in: try decoder.singleValueContainer(),
                    debugDescription: "Invalid date: \(str)")
            }
            return try decoder.decode([PBHistoryEntry].self, from: data)
        } catch {
            NSLog("PromptHistory load failed: %@", String(describing: error))
            return []
        }
    }

    private func saveUnlocked(_ entries: [PBHistoryEntry]) {
        // Atomares Schreiben: Erst in eine Temp-Datei, dann atomar
        // ueber das Original. Wenn das Programm mitten im Schreiben crasht,
        // bleibt entweder das alte oder das neue JSON bestehen — niemals
        // etwas Halbgares dazwischen.
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var c = encoder.singleValueContainer()
            try c.encode(Self.isoFormatter.string(from: date))
        }
        do {
            let data = try encoder.encode(entries)
            let tmp = historyFileURL.appendingPathExtension("tmp")
            try data.write(to: tmp, options: .atomic)
            // Atomares Verschieben — ersetzt das Ziel ohne Zwischenzustand.
            _ = try? FileManager.default.removeItem(at: historyFileURL)
            try FileManager.default.moveItem(at: tmp, to: historyFileURL)
        } catch {
            NSLog("PromptHistory save failed: %@", String(describing: error))
        }
    }

    private func appendToArchiveUnlocked(_ entry: PBHistoryEntry) {
        try? FileManager.default.createDirectory(
            at: archiveDirURL, withIntermediateDirectories: true)
        let existing: [URL]
        do {
            existing = try FileManager.default.contentsOfDirectory(
                at: archiveDirURL, includingPropertiesForKeys: nil
            ).filter { $0.lastPathComponent.hasPrefix("archive-") &&
                       $0.pathExtension == "md" }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }
        } catch {
            existing = []
        }

        let idMarker = "<!-- prompt-history-id: \(entry.id) -->"
        let legacyBlock = formatEntryAsMarkdown(entry, includeId: false)
        if existing.contains(where: { url in
            guard let content = try? String(contentsOf: url, encoding: .utf8) else { return false }
            return content.localizedCaseInsensitiveContains(idMarker) || content.contains(legacyBlock)
        }) {
            return
        }

        let target: URL
        if existing.isEmpty {
            target = archiveDirURL.appendingPathComponent("archive-001.md")
        } else {
            let latest = existing.last!
            let count = countEntries(in: latest)
            if count >= Self.maxEntriesPerArchive {
                let nextNum = parseArchiveNumber(latest) + 1
                target = archiveDirURL.appendingPathComponent(
                    String(format: "archive-%03d.md", nextNum))
            } else {
                target = latest
            }
        }

        let newBlock = formatEntryAsMarkdown(entry, includeId: true)
        let oldContent: String
        if FileManager.default.fileExists(atPath: target.path),
           let existing = try? String(contentsOf: target, encoding: .utf8) {
            oldContent = existing
        } else {
            oldContent = ""
        }
        let combined = newBlock + oldContent
        try? combined.write(to: target, atomically: true, encoding: .utf8)
    }

    private func countEntries(in url: URL) -> Int {
        guard let content = try? String(contentsOf: url, encoding: .utf8) else { return 0 }
        return content
            .components(separatedBy: "\n")
            .reduce(into: 0) { acc, line in
                if line.hasPrefix("## ") { acc += 1 }
            }
    }

    private func parseArchiveNumber(_ url: URL) -> Int {
        let name = url.deletingPathExtension().lastPathComponent  // archive-NNN
        guard let dash = name.lastIndex(of: "-") else { return 0 }
        let suffix = name[name.index(after: dash)...]
        return Int(suffix) ?? 0
    }

    private func archiveOverflowUnlocked(_ entries: inout [PBHistoryEntry]) {
        let overflowIds = Set(entries
            .filter { $0.archivedAt == nil }
            .sorted { $0.timestamp > $1.timestamp }
            .dropFirst(Self.maxActiveEntries)
            .map { $0.id.lowercased() })

        for index in entries.indices where overflowIds.contains(entries[index].id.lowercased()) {
            let archivedAt = Date()
            entries[index].archivedAt = archivedAt
            if (entries[index].updatedAt ?? entries[index].timestamp) < archivedAt {
                entries[index].updatedAt = archivedAt
            }
            appendToArchiveUnlocked(entries[index])
        }
        entries.sort { $0.timestamp > $1.timestamp }
    }

    private func formatEntryAsMarkdown(_ e: PBHistoryEntry, includeId: Bool) -> String {
        let when = Self.displayFormatter.string(from: e.timestamp)
        let title = e.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? "Ohne Titel" : e.title
        let marker = includeId ? "<!-- prompt-history-id: \(e.id) -->\n" : ""
        return "## \(when) — \(title)\n\(marker)\n\(e.text)\n\n---\n\n"
    }
}
