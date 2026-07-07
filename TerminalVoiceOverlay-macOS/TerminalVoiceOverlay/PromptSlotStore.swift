import Foundation

/// Ein einzelner Prompt-Zwischenspeicher-Slot (1…15). `text == ""` ist ein
/// Tombstone: der Slot wurde geloescht, der Zeitstempel bleibt aber erhalten,
/// damit das Loeschen beim Cloud-Merge geraeteuebergreifend gewinnt (ein
/// frisch geloeschter Slot mit neuem `updatedAt` ueberschreibt einen aelteren
/// belegten Stand auf dem anderen Geraet). Feldnamen sind camelCase und damit
/// 1:1 mit dem Windows-Pendant (PromptSlotService.cs) JSON-kompatibel.
struct PBSlotEntry: Codable, Equatable {
    var number: Int       // 1…15
    var text: String      // "" == geloescht (Tombstone)
    var updatedAt: Date
    /// KI-Zusammenfassung (6-8 Woerter) WOFUER dieser Prompt da ist — als
    /// Hover-Tooltip ueber dem belegten Slot. Leer bei Tombstones und bei
    /// Eintraegen aus der Zeit vor diesem Feature. Reist im JSON (Key
    /// `summary`) mit ins Drive-Backup; 1:1 zur Windows-Variante (Summary).
    var summary: String = ""
    /// Prioritaet fuer die farbige Einfaerbung der Zahlen-Leiste: 0 = keine
    /// (Standard, auch fuer alte Eintraege ohne dieses Feld), 1 = niedrig
    /// (gruen), 2 = mittel (gelb), 3 = hoch (rot). Reist im JSON (Key
    /// `priority`) mit ins Drive-Backup; 1:1 zur Windows-Variante (Priority).
    var priority: Int = 0

    enum CodingKeys: String, CodingKey { case number, text, updatedAt, summary, priority }

    init(number: Int, text: String, updatedAt: Date, summary: String = "", priority: Int = 0) {
        self.number = number
        self.text = text
        self.updatedAt = updatedAt
        self.summary = summary
        self.priority = priority
    }

    /// Toleranter Decoder: aeltere prompt-slots.json OHNE `summary`-Key (vor
    /// diesem Feature) bleibt lesbar — fehlt der Key, ist die Summary leer,
    /// statt dass das Decoding der ganzen Datei fehlschlaegt.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        number = try c.decode(Int.self, forKey: .number)
        text = try c.decode(String.self, forKey: .text)
        updatedAt = try c.decode(Date.self, forKey: .updatedAt)
        summary = try c.decodeIfPresent(String.self, forKey: .summary) ?? ""
        priority = try c.decodeIfPresent(Int.self, forKey: .priority) ?? 0
    }
}

/// Liest und schreibt die 15 Prompt-Zwischenspeicher-Slots aus einer JSON-
/// Datei. Voellig analog zum `PromptHistoryStore`: serielle Queue, atomares
/// Schreiben via Temp-Datei + Replace, ISO-8601-Datum kompatibel zu EF Core /
/// Windows. Die Datei wird nach jedem Speichern/Loeschen sofort zu Google
/// Drive gespiegelt (`prompt-slots.json` im appDataFolder).
///
/// Speicherort macOS:
///   ~/Library/Application Support/TerminalVoiceOverlay/slots/prompt-slots.json
final class PromptSlotStore {

    static let shared = PromptSlotStore()

    /// Anzahl der Slots (1…30, in der UI als zwei Reihen 1…15 / 16…30).
    /// Zentral, damit UI und Validierung dieselbe Quelle nutzen — bytegenau
    /// synchron zur Windows-Variante (PromptSlotService.SlotCount).
    static let slotCount = 30

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

    /// Wie `loadMapAndTimes`, liefert zusaetzlich die KI-Zusammenfassungen pro
    /// belegtem Slot — fuer die Hover-Tooltips der Zahlen-Leiste. Nur Slots mit
    /// nicht-leerer Summary erscheinen im Summary-Dictionary.
    func loadMapTimesSummaries(
        completion: @escaping ([Int: String], [Int: Date], [Int: String], [Int: Int]) -> Void
    ) {
        queue.async { [weak self] in
            guard let self = self else { completion([:], [:], [:], [:]); return }
            let entries = self.loadUnlocked()
            var map: [Int: String] = [:]
            var times: [Int: Date] = [:]
            var summaries: [Int: String] = [:]
            var priorities: [Int: Int] = [:]
            for e in entries where !e.text.isEmpty && (1...Self.slotCount).contains(e.number) {
                map[e.number] = e.text
                times[e.number] = e.updatedAt
                if !e.summary.isEmpty { summaries[e.number] = e.summary }
                if e.priority != 0 { priorities[e.number] = e.priority }
            }
            DispatchQueue.main.async { completion(map, times, summaries, priorities) }
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
            // Prioritaet (farbige Einfaerbung) gehoert zum SLOT, nicht zum Text,
            // und bleibt beim Ueberschreiben erhalten (Frank-Wunsch 2026-06-16).
            let keepPriority = entries.first(where: { $0.number == number })?.priority ?? 0
            let entry = PBSlotEntry(number: number, text: text, updatedAt: Date(), summary: "", priority: keepPriority)
            if let idx = entries.firstIndex(where: { $0.number == number }) {
                entries[idx] = entry
            } else {
                entries.append(entry)
            }
            self.saveUnlocked(entries)
            DispatchQueue.main.async { completion() }
        }
    }

    /// Aktualisiert NUR die KI-Summary eines belegten Slots — aber nur, wenn
    /// dessen gespeicherter Text noch exakt `forText` entspricht (der Gemini-
    /// Call ist asynchron und kann einen schnelleren Re-Save ueberholen).
    /// Bumpt `updatedAt`, damit die Summary per Cloud-Merge auf andere Geraete
    /// wandert. No-op bei leerem/geaendertem Slot.
    func setSummary(number: Int, forText: String, summary: String,
                    completion: @escaping () -> Void) {
        guard (1...Self.slotCount).contains(number) else {
            DispatchQueue.main.async { completion() }
            return
        }
        queue.async { [weak self] in
            guard let self = self else { DispatchQueue.main.async { completion() }; return }
            var entries = self.loadUnlocked()
            guard let idx = entries.firstIndex(where: { $0.number == number }),
                  !entries[idx].text.isEmpty,
                  entries[idx].text == forText else {
                DispatchQueue.main.async { completion() }
                return
            }
            entries[idx].summary = summary
            entries[idx].updatedAt = Date()
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

    /// Setzt die Prioritaet (0=keine,1=niedrig,2=mittel,3=hoch) eines belegten
    /// Slots. Bumpt `updatedAt`, damit die Aenderung per Cloud-Merge auf andere
    /// Geraete wandert UND ins Google-Drive-Backup (prompt-slots.json) kommt.
    /// No-op bei leerem/Tombstone-Slot.
    func setPriority(number: Int, priority: Int, completion: @escaping () -> Void) {
        guard (1...Self.slotCount).contains(number), (0...3).contains(priority) else {
            DispatchQueue.main.async { completion() }
            return
        }
        queue.async { [weak self] in
            guard let self = self else { DispatchQueue.main.async { completion() }; return }
            var entries = self.loadUnlocked()
            guard let idx = entries.firstIndex(where: { $0.number == number }),
                  !entries[idx].text.isEmpty else {
                DispatchQueue.main.async { completion() }
                return
            }
            entries[idx].priority = priority
            entries[idx].updatedAt = Date()
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
