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
    var gelernt = false

    init(id: UUID = UUID(), titel: String = "Neues Gespräch") {
        self.id = id; self.titel = titel; erstellt = Date(); geaendert = Date()
        nachrichten = []; verlauf = []; kontext = ""
    }

    private struct Datei: Codable {
        var id: UUID; var titel: String; var erstellt: Date; var geaendert: Date
        var nachrichten: [Nachricht]; var verlauf: Data; var kontext: String; var gelernt: Bool?
    }

    var dateiURL: URL { Pfade.gespraeche.appendingPathComponent("\(id.uuidString).json") }

    func speichern() {
        geaendert = Date()
        let d = Datei(id: id, titel: titel, erstellt: erstellt, geaendert: geaendert, nachrichten: nachrichten,
                      verlauf: JSON.daten(verlauf), kontext: kontext, gelernt: gelernt)
        if let daten = try? JSONEncoder().encode(d) { try? daten.write(to: dateiURL, options: .atomic) }
    }

    static func laden(_ url: URL) -> Gespraech? {
        guard let daten = try? Data(contentsOf: url), let d = try? JSONDecoder().decode(Datei.self, from: daten) else { return nil }
        let g = Gespraech(id: d.id, titel: d.titel)
        g.erstellt = d.erstellt; g.geaendert = d.geaendert
        g.nachrichten = d.nachrichten.map { var n = $0; n.laeuft = false; return n }
        g.verlauf = (JSON.wert(d.verlauf) as? [JSONObjekt]) ?? []
        g.kontext = d.kontext; g.gelernt = d.gelernt ?? false
        return g
    }
}

@MainActor @Observable
final class GespraechsSpeicher {
    static let shared = GespraechsSpeicher()
    var alle: [Gespraech] = []

    init() {
        let dateien = (try? FileManager.default.contentsOfDirectory(at: Pfade.gespraeche, includingPropertiesForKeys: nil)) ?? []
        alle = dateien.filter { $0.pathExtension == "json" }.compactMap(Gespraech.laden).sorted { $0.geaendert > $1.geaendert }
    }

    func neu() -> Gespraech {
        let g = Gespraech()
        alle.insert(g, at: 0)
        return g
    }

    func loeschen(_ g: Gespraech) {
        alle.removeAll { $0.id == g.id }
        try? FileManager.default.removeItem(at: g.dateiURL)
    }

    func nachOben(_ g: Gespraech) {
        alle.removeAll { $0.id == g.id }
        alle.insert(g, at: 0)
    }
}
