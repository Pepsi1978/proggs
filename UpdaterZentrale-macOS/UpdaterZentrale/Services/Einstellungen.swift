import Foundation

/// Was die App sich pro Programm merkt. 1:1-Port von Services/Einstellungen.cs.
///
/// Die beiden Windows-Felder rund um den Administrator-Autostart (`GesicherterRunName`,
/// `GesicherterRunWert`) entfallen: auf macOS gibt es weder den HKCU-Run-Schluessel noch eine
/// geplante Aufgabe mit hoechsten Rechten, die ihn ersetzen muesste. Siehe PORTING.md.
final class ProgrammEinstellung: Codable {
    var ausgeblendet: Bool = false

    /// Bis hierher wurden Fehlermeldungen weggeklickt. Das rote Band einer Karte bleibt so lange
    /// stehen, bis es quittiert wird -- und ein spaeterer Fehler taucht wieder auf, weil sein
    /// Zeitstempel jenseits dieser Marke liegt. Das Protokoll bleibt davon unberuehrt.
    var fehlerQuittiertBis: Date?

    init() {}

    enum CodingKeys: String, CodingKey {
        case ausgeblendet = "Ausgeblendet"
        case fehlerQuittiertBis = "FehlerQuittiertBis"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        ausgeblendet = (try? c.decode(Bool.self, forKey: .ausgeblendet)) ?? false
        fehlerQuittiertBis = try? c.decode(Date.self, forKey: .fehlerQuittiertBis)
    }
}

/// Der Rest eines Sammellaufs, den das Selbst-Update unterbrochen hat. Die neue Fassung setzt ihn
/// nach ihrer Startpruefung fort.
final class SammelFortsetzung: Codable {
    var sammelId: String = ""
    var zeit: Date = Date()
    var programmIds: [String] = []
    /// Schon erledigte Schritte des urspruenglichen Plans -- die Positionen zaehlen dort weiter.
    var versatz: Int = 0
    var planGroesse: Int = 0
    /// Der Plan kam vom CLI-Knopf -- dessen Rueckfrage galt schon.
    var cliBestaetigt: Bool = false

    init() {}

    enum CodingKeys: String, CodingKey {
        case sammelId = "SammelId", zeit = "Zeit", programmIds = "ProgrammIds", versatz = "Versatz",
             planGroesse = "PlanGroesse", cliBestaetigt = "CliBestaetigt"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        sammelId = (try? c.decode(String.self, forKey: .sammelId)) ?? ""
        zeit = (try? c.decode(Date.self, forKey: .zeit)) ?? .distantPast
        programmIds = (try? c.decode([String].self, forKey: .programmIds)) ?? []
        versatz = (try? c.decode(Int.self, forKey: .versatz)) ?? 0
        planGroesse = (try? c.decode(Int.self, forKey: .planGroesse)) ?? 0
        cliBestaetigt = (try? c.decode(Bool.self, forKey: .cliBestaetigt)) ?? false
    }
}

/// Der Benutzer-Zustand liegt ausserhalb des Repos
/// (~/Library/Application Support/UpdaterZentrale/settings.json), damit programs.json auch nach
/// dem Umlegen von Schaltern eine saubere, einchequierbare Datei bleibt.
final class Einstellungen: Codable {
    var programme: [String: ProgrammEinstellung] = [:]

    /// Heller Modus; dunkel bleibt die Vorgabe, weil die App meist nur kurz geoeffnet wird.
    var hellModus: Bool = false

    /// Karten innerhalb jeder Gruppe nach erwarteter Update-Dauer sortiert (kuerzeste oben).
    var nachDauerSortieren: Bool = false

    /// Ein Sammellauf hat zuerst die Updater-Zentrale selbst aktualisiert, die sich dafuer beendet
    /// und neu startet. Der Rest liegt hier und wird von der neuen Fassung fortgesetzt.
    var sammelFortsetzen: SammelFortsetzung?

    init() {}

    enum CodingKeys: String, CodingKey {
        case programme = "Programme"
        case hellModus = "HellModus"
        case nachDauerSortieren = "NachDauerSortieren"
        case sammelFortsetzen = "SammelFortsetzen"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        programme = (try? c.decode([String: ProgrammEinstellung].self, forKey: .programme)) ?? [:]
        hellModus = (try? c.decode(Bool.self, forKey: .hellModus)) ?? false
        nachDauerSortieren = (try? c.decode(Bool.self, forKey: .nachDauerSortieren)) ?? false
        sammelFortsetzen = try? c.decode(SammelFortsetzung.self, forKey: .sammelFortsetzen)
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
