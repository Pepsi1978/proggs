import Foundation

/// Was die App sich pro Programm merkt. 1:1-Port von Services/Einstellungen.cs.
///
/// Die beiden Windows-Felder rund um den Administrator-Autostart (`GesicherterRunName`,
/// `GesicherterRunWert`) entfallen: auf macOS gibt es weder den HKCU-Run-Schluessel noch eine
/// geplante Aufgabe mit hoechsten Rechten, die ihn ersetzen muesste. Siehe PORTING.md.
final class ProgrammEinstellung: Codable {
    var ausgeblendet: Bool = false

    init() {}

    enum CodingKeys: String, CodingKey { case ausgeblendet = "Ausgeblendet" }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        ausgeblendet = (try? c.decode(Bool.self, forKey: .ausgeblendet)) ?? false
    }
}

/// Der Benutzer-Zustand liegt ausserhalb des Repos
/// (~/Library/Application Support/UpdaterZentrale/settings.json), damit programs.json auch nach
/// dem Umlegen von Schaltern eine saubere, einchequierbare Datei bleibt.
final class Einstellungen: Codable {
    var programme: [String: ProgrammEinstellung] = [:]

    /// Heller Modus; dunkel bleibt die Vorgabe, weil die App meist nur kurz geoeffnet wird.
    var hellModus: Bool = false

    init() {}

    enum CodingKeys: String, CodingKey {
        case programme = "Programme"
        case hellModus = "HellModus"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        programme = (try? c.decode([String: ProgrammEinstellung].self, forKey: .programme)) ?? [:]
        hellModus = (try? c.decode(Bool.self, forKey: .hellModus)) ?? false
    }

    static func laden() -> Einstellungen {
        guard FileManager.default.fileExists(atPath: Pfade.einstellungsDatei),
              let daten = FileManager.default.contents(atPath: Pfade.einstellungsDatei),
              let geladen = try? JSONDecoder().decode(Einstellungen.self, from: daten) else {
            // Eine kaputte Einstellungsdatei darf die App nie blockieren; die Vorgaben taugen immer.
            return Einstellungen()
        }
        return geladen
    }

    func speichern() {
        Pfade.ordnerAnlegen(Pfade.benutzerOrdner)
        let kodierer = JSONEncoder()
        kodierer.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let daten = try? kodierer.encode(self) else { return }
        try? daten.write(to: URL(fileURLWithPath: Pfade.einstellungsDatei), options: .atomic)
    }

    func fuer(_ id: String) -> ProgrammEinstellung {
        if let vorhanden = programme[id] { return vorhanden }
        let neu = ProgrammEinstellung()
        programme[id] = neu
        return neu
    }
}
