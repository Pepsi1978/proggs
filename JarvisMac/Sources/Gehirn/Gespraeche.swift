import Foundation
import Observation

struct Schritt: Codable, Identifiable, Hashable {
    var id = UUID()
    var werkzeug: String
    var kurz: String
    var ok: Bool? = nil     // nil = läuft noch
    var amHandy: Bool
}

struct Nachricht: Codable, Identifiable, Hashable {
    enum Rolle: String, Codable { case frank, jarvis, hinweis }
    var id = UUID()
    var rolle: Rolle
    var text: String
    var zeit = Date()
    var schritte: [Schritt] = []
    var gesprochen = false
    var bild: Data? = nil
    var laeuft = false
}

private struct GespraechsDatei: Codable {
    var id: UUID; var titel: String; var erstellt: Date; var geaendert: Date
    var nachrichten: [Nachricht]; var verlauf: Data; var kontext: String; var gelerntBis: Int?; var projekt: String?; var auftragId: String?; var gesendeteNachtraege: [String]?
}

/// Eine eingelesene Datei, fertig entschlüsselt: Das teure JSON läuft im Hintergrund, angelegt wird das Gespräch auf dem Hauptfaden.
private struct GeleseneDatei: @unchecked Sendable {
    let datei: GespraechsDatei
    let verlauf: [JSONObjekt]
}

@MainActor @Observable
final class Gespraech: Identifiable {
    let id: UUID
    var titel: String
    var erstellt: Date
    var geaendert: Date
    var nachrichten: [Nachricht]
    /// Verlauf im Messages-Format (kanonisch), wie er an das Modell geht.
    var verlauf: [JSONObjekt]
    /// Live-Gedächtnis des Handys zum Gesprächsbeginn (bleibt im Gespräch fest, damit der Prompt-Cache trägt).
    var kontext: String
    var arbeitet = false
    /// Bis zu dieser Nachrichtenzahl hat „Lernen am Mac“ das Gespräch schon ausgewertet.
    var gelerntBis = 0
    /// Projekt, zu dem das Gespräch gehört (nil = normales Gespräch).
    var projekt: String?
    /// Nummer des Programmierauftrags, der aus diesem Gespräch gestartet wurde: das Ziel für „Nachtrag“.
    var auftragId: String?
    /// Per Knopf gesendete Nachträge, die das Modell noch nicht kennt; sie gehen mit der nächsten Frage mit.
    var gesendeteNachtraege: [String] = []
    /// Gespräch mit Programmierauftrag – steht im Leistenreiter „Programmierung“. Gemerkt statt gerechnet: Die Seitenleiste
    /// liest es für alle Gespräche und würde sonst bei jedem gestreamten Textstück neu zeichnen.
    private(set) var istProgrammierung = false
    /// Einmal gelöscht, schreibt auch eine noch laufende Antwort die Datei nicht wieder.
    @ObservationIgnored var geloescht = false

    init(id: UUID = UUID(), titel: String = "Neues Gespräch") {
        self.id = id; self.titel = titel; erstellt = Date(); geaendert = Date()
        nachrichten = []; verlauf = []; kontext = ""
    }

    func programmierungPruefen() {
        let neu = nachrichten.contains { $0.schritte.contains { ["programmieren", "openlauncher_sitzung", "programmier_nachtrag", "programmierer_sprechen"].contains($0.werkzeug) } }
        if neu != istProgrammierung { istProgrammierung = neu }
    }

    var dateiURL: URL { Pfade.gespraeche.appendingPathComponent("\(id.uuidString).json") }

    /// Alle Schreibzugriffe der Reihe nach, damit ein späteres Löschen nie von einem älteren Speichern überholt wird.
    nonisolated static let schreiber = DispatchQueue(label: "jarvis.gespraeche", qos: .utility)

    /// Zählt je Gespräch die Speicherwünsche: Von mehreren wartenden schreibt nur der jüngste, alles davor wäre überholt.
    private final class Stand: @unchecked Sendable {
        let lock = NSLock()
        var je: [UUID: Int] = [:]
        func neu(_ id: UUID) -> Int { lock.withLock { je[id, default: 0] += 1; return je[id]! } }
        func aktuell(_ id: UUID, _ nr: Int) -> Bool { lock.withLock { je[id] == nr } }
    }
    private nonisolated static let stand = Stand()

    /// Ein Gespräch ohne Inhalt bleibt ein Entwurf: keine Datei und kein Eintrag in der Leiste.
    func speichern() {
        guard !nachrichten.isEmpty, !geloescht else { return }
        geaendert = Date()
        GespraechsSpeicher.shared.aufnehmen(self)
        let (id, titel, erstellt, geaendert, nachrichten, verlauf, kontext, gelerntBis, projekt, auftragId, nachtraege, url) =
            (self.id, self.titel, self.erstellt, self.geaendert, self.nachrichten, self.verlauf, self.kontext, self.gelerntBis, self.projekt, self.auftragId, self.gesendeteNachtraege, dateiURL)
        // Das Gespräch samt Bildern kodieren und schreiben dauert: nicht auf dem Hauptfaden, sonst stocken Oberfläche und Pegel.
        let nr = Self.stand.neu(id)
        Self.schreiber.async {
            guard Self.stand.aktuell(id, nr) else { return }
            let d = GespraechsDatei(id: id, titel: titel, erstellt: erstellt, geaendert: geaendert, nachrichten: nachrichten,
                                    verlauf: JSON.daten(verlauf), kontext: kontext, gelerntBis: gelerntBis, projekt: projekt, auftragId: auftragId, gesendeteNachtraege: nachtraege)
            if let daten = try? JSONEncoder().encode(d) { try? daten.write(to: url, options: .atomic) }
        }
    }

    fileprivate init(gelesen: GeleseneDatei) {
        let d = gelesen.datei
        id = d.id; titel = d.titel; erstellt = d.erstellt; geaendert = d.geaendert
        nachrichten = d.nachrichten; verlauf = gelesen.verlauf
        kontext = d.kontext; gelerntBis = d.gelerntBis ?? 0; projekt = d.projekt; auftragId = d.auftragId; gesendeteNachtraege = d.gesendeteNachtraege ?? []
        programmierungPruefen()
    }

    /// Leere Altbestände verschwinden dabei samt Datei.
    fileprivate nonisolated static func lesen(_ url: URL) -> GeleseneDatei? {
        guard let daten = try? Data(contentsOf: url), var d = try? JSONDecoder().decode(GespraechsDatei.self, from: daten) else { return nil }
        guard !d.nachrichten.isEmpty else { try? FileManager.default.removeItem(at: url); return nil }
        d.nachrichten = d.nachrichten.map { var n = $0; n.laeuft = false; return n }
        return GeleseneDatei(datei: d, verlauf: (JSON.wert(d.verlauf) as? [JSONObjekt]) ?? [])
    }
}

@MainActor @Observable
final class GespraechsSpeicher {
    static let shared = GespraechsSpeicher()
    var alle: [Gespraech] = []
    /// Die gespeicherten Gespräche sind von der Platte da (der Agentenkanal sucht sein Gespräch darin).
    private(set) var geladen = false

    private final class Sammler: @unchecked Sendable {
        let lock = NSLock()
        var liste: [GeleseneDatei] = []
    }

    init() {
        // Im Hintergrund und parallel: Gespräche mit Bildern würden den Start sonst sichtbar aufhalten.
        Task.detached(priority: .userInitiated) { [weak self] in
            let dateien = ((try? FileManager.default.contentsOfDirectory(at: Pfade.gespraeche, includingPropertiesForKeys: nil)) ?? [])
                .filter { $0.pathExtension == "json" }
            let sammler = Sammler()
            DispatchQueue.concurrentPerform(iterations: dateien.count) { i in
                guard let g = Gespraech.lesen(dateien[i]) else { return }
                sammler.lock.withLock { sammler.liste.append(g) }
            }
            let gelesen = sammler.liste
            await self?.uebernehmen(gelesen)
        }
    }

    private func uebernehmen(_ gelesen: [GeleseneDatei]) {
        geladen = true
        let schonDa = Set(alle.map(\.id))
        alle = (alle + gelesen.filter { !schonDa.contains($0.datei.id) }.map(Gespraech.init(gelesen:))).sorted { $0.geaendert > $1.geaendert }
    }

    /// Ein Entwurf: In die Liste kommt das Gespräch erst mit seiner ersten Nachricht.
    func neu() -> Gespraech { Gespraech() }

    func aufnehmen(_ g: Gespraech) {
        if !alle.contains(where: { $0.id == g.id }) { alle.insert(g, at: 0) }
    }

    func loeschen(_ g: Gespraech) {
        g.geloescht = true
        AppSteuerung.shared.gespraechBeendet(g)
        alle.removeAll { $0.id == g.id }
        let url = g.dateiURL
        Gespraech.schreiber.async { try? FileManager.default.removeItem(at: url) }
    }

    func nachOben(_ g: Gespraech) {
        guard !g.geloescht else { return }
        alle.removeAll { $0.id == g.id }
        alle.insert(g, at: 0)
    }

    /// Beim Beenden der App: Was noch in der Warteschlange steht, muss auf die Platte.
    nonisolated static func abschliessen() { Gespraech.schreiber.sync {} }
}
