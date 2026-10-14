import Foundation

struct PruefstandEintrag: Codable {
    var zustand: String
    var installiert: String
    var verfuegbar: String
    var meldung: String
    var zeit: Date
}

/// Das Ergebnis der letzten Pruefung je Programm, ueber Neustarts hinweg
/// (~/Library/Application Support/UpdaterZentrale/pruefstand.json). Das naechste Oeffnen zeigt es
/// sofort, statt alles erneut abzufragen. 1:1-Port von Services/Pruefstand.cs.
///
/// Nur klare Antworten werden gemerkt. Eine fehlgeschlagene oder unbekannte Pruefung nie -- sonst
/// stuende ein alter Fehler beim naechsten Start als aktueller Zustand da.
@MainActor
enum Pruefstand {
    private static var stand: [String: PruefstandEintrag]?

    private static var datei: String {
        (Pfade.benutzerOrdner as NSString).appendingPathComponent("pruefstand.json")
    }

    static func lesen(_ id: String) -> PruefstandEintrag? { laden()[id] }

    static func merken(_ id: String, _ ergebnis: PruefErgebnis) {
        guard let name = zustandsName(ergebnis.zustand) else {
            vergessen(id)
            return
        }
        var alle = laden()
        alle[id] = PruefstandEintrag(zustand: name, installiert: ergebnis.installierteVersion,
                                     verfuegbar: ergebnis.verfuegbareVersion, meldung: ergebnis.meldung, zeit: Date())
        stand = alle
        speichern()
    }

    /// Nach einem Update ist die gemerkte Antwort hinfaellig.
    static func vergessen(_ id: String) {
        var alle = laden()
        guard alle.removeValue(forKey: id) != nil else { return }
        stand = alle
        speichern()
    }

    static func zustandsName(_ zustand: UpdateZustand) -> String? {
        switch zustand {
        case .aktuell: return "aktuell"
        case .updateVerfuegbar: return "updateVerfuegbar"
        case .nichtInstalliert: return "nichtInstalliert"
        default: return nil
        }
    }

    static func zustand(_ name: String) -> UpdateZustand? {
        switch name {
        case "aktuell": return .aktuell
        case "updateVerfuegbar": return .updateVerfuegbar
        case "nichtInstalliert": return .nichtInstalliert
        default: return nil
        }
    }

    private static func laden() -> [String: PruefstandEintrag] {
        if let stand { return stand }
        var geladen: [String: PruefstandEintrag] = [:]
        if let daten = FileManager.default.contents(atPath: datei) {
            let dekodierer = JSONDecoder()
            dekodierer.dateDecodingStrategy = .iso8601
            // Eine kaputte Datei kostet nur eine frische Pruefung.
            geladen = (try? dekodierer.decode([String: PruefstandEintrag].self, from: daten)) ?? [:]
        }
        stand = geladen
        return geladen
    }

    private static func speichern() {
        guard let stand else { return }
        Pfade.ordnerAnlegen(Pfade.benutzerOrdner)
        let kodierer = JSONEncoder()
        kodierer.outputFormatting = [.prettyPrinted, .sortedKeys]
        kodierer.dateEncodingStrategy = .iso8601
        guard let daten = try? kodierer.encode(stand) else { return }
        try? daten.write(to: URL(fileURLWithPath: datei), options: .atomic)
    }
}
