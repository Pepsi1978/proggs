import Foundation

/// Ein einzelner Prompt-Zwischenspeicher-Slot (1…10). `text == ""` ist ein
/// Tombstone: der Slot wurde geloescht, der Zeitstempel bleibt aber erhalten,
/// damit das Loeschen beim Cloud-Merge geraeteuebergreifend gewinnt (ein
/// frisch geloeschter Slot mit neuem `updatedAt` ueberschreibt einen aelteren
/// belegten Stand auf dem anderen Geraet). Feldnamen sind camelCase und damit
/// 1:1 mit dem Windows-Pendant (PromptSlotService.cs) JSON-kompatibel.
struct PBSlotEntry: Codable, Equatable {
    var number: Int       // 1…10
    var text: String      // "" == geloescht (Tombstone)
    var updatedAt: Date
}

/// Liest und schreibt die 10 Prompt-Zwischenspeicher-Slots aus einer JSON-
/// Datei. Voellig analog zum `PromptHistoryStore`: serielle Queue, atomares
/// Schreiben via Temp-Datei + Replace, ISO-8601-Datum kompatibel zu EF Core /
/// Windows. Die Datei wird nach jedem Speichern/Loeschen sofort zu Google
/// Drive gespiegelt (`prompt-slots.json` im appDataFolder).
///
/// Speicherort macOS:
///   ~/Library/Application Support/TerminalVoiceOverlay/slots/prompt-slots.json
final class PromptSlotStore {

    static let shared = PromptSlotStore()

    /// Anzahl der Slots (1…10). Zentral, damit UI und Validierung dieselbe
    /// Quelle nutzen.
    static let slotCount = 10

    private let baseDir: URL
    private let slotsFileURL: URL
    private let queue = DispatchQueue(label: "tvo.PromptSlotStore.queue")

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private init() {
        let support = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        ).first ?? URL(fileURLWithPath: NSHomeDirectory())
        baseDir = support
            .appendingPathComponent("TerminalVoiceOverlay", isDirectory: true)
            .appendingPathComponent("slots", isDirectory: true)
        slotsFileURL = baseDir.appendingPathComponent("prompt-slots.json")
        try? FileManager.default.createDirectory(at: baseDir, withIntermediateDirectories: true)
    }

    /// Pfad zur lokalen JSON-Datei (Diagnose / Cloud-Sync).
    var slotsFilePath: String { slotsFileURL.path }

    // MARK: - Lesen

    /// Liefert die belegten Slots als Dictionary `nummer → text` (Tombstones
    /// mit leerem Text werden ausgefiltert). Wird vom UI genutzt um die
    /// Zahlen-Leiste einzufaerben und beim Klick den Text zu laden.
    func loadMap(completion: @escaping ([Int: String]) -> Void) {
        queue.async { [weak self] in
            guard let self = self else { completion([:]); return }
            let entries = self.loadUnlocked()
            var map: [Int: String] = [:]
            for e in entries where !e.text.isEmpty && (1...Self.slotCount).contains(e.number) {
                map[e.number] = e.text
            }
            DispatchQueue.main.async { completion(map) }
        }
    }

    /// Wie `loadMap`, liefert aber zusaetzlich die Speicher-Zeitstempel pro
    /// belegtem Slot — fuer die Anzeige „wann gespeichert" neben dem X.
    func loadMapAndTimes(completion: @escaping ([Int: String], [Int: Date]) -> Void) {
        queue.async { [weak self] in
            guard let self = self else { completion([:], [:]); return }
            let entries = self.loadUnlocked()
            var map: [Int: String] = [:]
            var times: [Int: Date] = [:]
            for e in entries where !e.text.isEmpty && (1...Self.slotCount).contains(e.number) {
                map[e.number] = e.text
                times[e.number] = e.updatedAt
            }
            DispatchQueue.main.async { completion(map, times) }
        }
    }

    /// Liefert ALLE Eintraege roh (inkl. Tombstones mit leerem Text) — wird
    /// fuer den Cloud-Merge gebraucht, der die Zeitstempel vergleicht.
    func loadEntries(completion: @escaping ([PBSlotEntry]) -> Void) {
        queue.async { [weak self] in
            guard let self = self else { completion([]); return }
            let entries = self.loadUnlocked()
            DispatchQueue.main.async { completion(entries) }
        }
    }

    // MARK: - Schreiben

    /// Speichert (oder ueberschreibt) den Text in einem Slot. Aktualisiert
    /// `updatedAt` auf jetzt, schreibt atomar zurueck und ruft `completion`
    /// auf dem Main-Thread auf (dort stoesst der Aufrufer den Cloud-Sync an).
    func save(number: Int, text: String, completion: @escaping () -> Void) {
        guard (1...Self.slotCount).contains(number) else {
            DispatchQueue.main.async { completion() }
            return
        }
        queue.async { [weak self] in
            guard let self = self else { DispatchQueue.main.async { completion() }; return }
            var entries = self.loadUnlocked()
            let entry = PBSlotEntry(number: number, text: text, updatedAt: Date())
            if let idx = entries.firstIndex(where: { $0.number == number }) {
                entries[idx] = entry
            } else {
                entries.append(entry)
            }
            self.saveUnlocked(entries)
            DispatchQueue.main.async { completion() }
        }
    }

    /// Loescht den Slot dauerhaft. Hinterlaesst einen Tombstone (leerer Text +
    /// neuer Zeitstempel), damit das Loeschen beim naechsten Cloud-Merge
    /// gewinnt. `completion` laeuft auf dem Main-Thread fuer den Sofort-Sync.
    func delete(number: Int, completion: @escaping () -> Void) {
        guard (1...Self.slotCount).contains(number) else {
            DispatchQueue.main.async { completion() }
            return
        }
        queue.async { [weak self] in
            guard let self = self else { DispatchQueue.main.async { completion() }; return }
            var entries = self.loadUnlocked()
            let tombstone = PBSlotEntry(number: number, text: "", updatedAt: Date())
            if let idx = entries.firstIndex(where: { $0.number == number }) {
                entries[idx] = tombstone
            } else {
                entries.append(tombstone)
            }
            self.saveUnlocked(entries)
            DispatchQueue.main.async { completion() }
        }
    }

    // MARK: - Cloud-Sync

    /// Roher JSON-Inhalt der lokalen Datei fuer den Cloud-Upload (1:1 hoch,
    /// damit Mac und Windows exakt dieselben Bytes austauschen). Leeres
    /// Array wenn noch nichts existiert.
    func rawJsonFromDisk() -> String {
        guard FileManager.default.fileExists(atPath: slotsFileURL.path),
              let s = try? String(contentsOf: slotsFileURL, encoding: .utf8)
        else { return "[]" }
        return s
    }

    /// Ersetzt den gesamten lokalen Stand durch eine gemergte Liste — wird
    /// vom Cloud-Sync nach dem Merge genutzt.
    func replaceAll(entries: [PBSlotEntry], completion: @escaping () -> Void) {
        queue.async { [weak self] in
            guard let self = self else { DispatchQueue.main.async { completion() }; return }
            self.saveUnlocked(entries)
            DispatchQueue.main.async { completion() }
        }
    }

    /// Mergt eine Cloud-JSON in die lokale Liste: pro Slot-Nummer gewinnt der
    /// Eintrag mit dem juengsten `updatedAt`. So setzt sich sowohl ein neuer
    /// Text als auch ein neues Loeschen (Tombstone) gegen einen aelteren Stand
    /// durch. Kaputtes Cloud-JSON laesst die lokale Liste unveraendert.
    static func merge(local: [PBSlotEntry], cloudJson: String) -> [PBSlotEntry] {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let str = try decoder.singleValueContainer().decode(String.self)
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
        var cloud: [PBSlotEntry] = []
        if let data = cloudJson.data(using: .utf8),
           let parsed = try? decoder.decode([PBSlotEntry].self, from: data) {
            cloud = parsed
        }
        var byNumber: [Int: PBSlotEntry] = [:]
        for e in local where (1...slotCount).contains(e.number) { byNumber[e.number] = e }
        for e in cloud where (1...slotCount).contains(e.number) {
            if let existing = byNumber[e.number] {
                if e.updatedAt > existing.updatedAt { byNumber[e.number] = e }
            } else {
                byNumber[e.number] = e
            }
        }
        return byNumber.values.sorted { $0.number < $1.number }
    }

    // MARK: - Interne Helpers (laufen alle innerhalb der serial queue)

    private func loadUnlocked() -> [PBSlotEntry] {
        guard FileManager.default.fileExists(atPath: slotsFileURL.path) else { return [] }
        do {
            let data = try Data(contentsOf: slotsFileURL)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .custom { decoder in
                let str = try decoder.singleValueContainer().decode(String.self)
                if let d = Self.isoFormatter.date(from: str) { return d }
                let f = ISO8601DateFormatter()
                f.formatOptions = [.withInternetDateTime]
                if let d = f.date(from: str) { return d }
                throw DecodingError.dataCorruptedError(
                    in: try decoder.singleValueContainer(),
                    debugDescription: "Invalid date: \(str)")
            }
            return try decoder.decode([PBSlotEntry].self, from: data)
        } catch {
            NSLog("PromptSlot load failed: %@", String(describing: error))
            return []
        }
    }

    private func saveUnlocked(_ entries: [PBSlotEntry]) {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var c = encoder.singleValueContainer()
            try c.encode(Self.isoFormatter.string(from: date))
        }
        do {
            // Stabile Reihenfolge nach Slot-Nummer, damit das JSON
            // deterministisch ist (kleinere Drive-Diffs, leichteres Debuggen).
            let sorted = entries.sorted { $0.number < $1.number }
            let data = try encoder.encode(sorted)
            let tmp = slotsFileURL.appendingPathExtension("tmp")
            try data.write(to: tmp, options: .atomic)
            _ = try? FileManager.default.removeItem(at: slotsFileURL)
            try FileManager.default.moveItem(at: tmp, to: slotsFileURL)
        } catch {
            NSLog("PromptSlot save failed: %@", String(describing: error))
        }
    }
}
