import Foundation

/// Jarvis auf HP-Laptop, Power-PC und Mac: duenne Karte ueber der gemeinsamen Verteil-Logik
/// `Werkzeuge/jarvis-verteilung/jarvis_verteilung.py` (dieselbe Datei nutzt die Windows-Zentrale, Port von
/// Providers/JarvisVerteilungAktualisierer.cs). Pruefen = Repo holen, Portierungsbedarf bestimmen und noetige
/// Portierungssitzungen im Hintergrund starten (nie bei einem gewoehnlichen Update ohne Portierungsbedarf),
/// Aktualisieren = nur gepruefte, freigegebene Staende mit Sicherung und Wiederherstellung installieren. Beim ersten
/// Pruefen richtet sie den LaunchAgent `de.frank.jarvis-verteilung` ein (alle 30 Minuten und bei der Anmeldung).
struct JarvisVerteilungAktualisierer: Aktualisierer {
    let art = "jarvisverteilung"

    private final class Merker: @unchecked Sendable { var eingerichtet = false }
    private static let merker = Merker()

    static var skript: String {
        (Pfade.repoWurzel as NSString).appendingPathComponent("Werkzeuge/jarvis-verteilung/jarvis_verteilung.py")
    }

    private static func lauf(_ befehl: [String], zeit: TimeInterval) async -> (BefehlErgebnis, [String: Any]?) {
        let json = (NSTemporaryDirectory() as NSString).appendingPathComponent("jarvis-verteilung-\(UUID().uuidString).json")
        defer { try? FileManager.default.removeItem(atPath: json) }
        let erg = await Kommandozeile.ausfuehren("/usr/bin/python3", [skript] + befehl, zeitlimit: zeit,
                                                 arbeitsverzeichnis: Pfade.repoWurzel,
                                                 zusatzUmgebung: ["JARVIS_VERTEILUNG_JSON": json])
        let daten = (try? Data(contentsOf: URL(fileURLWithPath: json)))
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
        return (erg, daten)
    }

    private static func lokal(_ d: [String: Any]?) -> [String: Any]? {
        (d?["ziele"] as? [[String: Any]])?.first { ($0["lokal"] as? Bool) == true }
    }

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        guard FileManager.default.fileExists(atPath: Self.skript) else {
            return PruefErgebnis(zustand: .fehler, meldung: "Verteil-Skript fehlt: " + Self.skript)
        }
        if !Self.merker.eingerichtet {
            let (e, _) = await Self.lauf(["einrichten"], zeit: 60)
            protokoll.berichte(e.ausgabe)
            Self.merker.eingerichtet = e.exitCode == 0
        }
        let (erg, daten) = await Self.lauf(["pruefen"], zeit: 360)
        protokoll.berichte(erg.ausgabe)
        return Self.einordnen(daten, erg, nachInstallation: false)
    }

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        protokoll.berichte("Installiert nur geprüfte, freigegebene Stände; vorher Sicherung, bei Fehlschlag wird die letzte Version wiederhergestellt.")
        let (erg, daten) = await Self.lauf(["installieren"], zeit: 35 * 60)
        protokoll.berichte(erg.ausgabe)
        return Self.einordnen(daten, erg, nachInstallation: true)
    }

    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        let (_, daten) = await Self.lauf(["status", "--ohne-holen"], zeit: 120)
        return Self.lokal(daten)?["installiert"] as? String ?? ""
    }

    static func einordnen(_ daten: [String: Any]?, _ erg: BefehlErgebnis, nachInstallation: Bool) -> PruefErgebnis {
        guard let z = lokal(daten) else {
            let letzte = erg.ausgabe.split(separator: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.last { !$0.isEmpty } ?? ""
            return PruefErgebnis(zustand: .fehler, meldung: "Jarvis-Verteilung ohne Ergebnis: " + letzte, protokoll: erg.ausgabe)
        }
        let text = daten?["text"] as? String ?? ""
        let kurz = daten?["kurz"] as? String ?? ""
        let installiert = z["installiert"] as? String ?? ""
        let bereit = z["bereitgestellt"] as? String ?? ""
        if nachInstallation && kurz == "fehlgeschlagen" {
            return PruefErgebnis(zustand: .fehler, installierteVersion: installiert, verfuegbareVersion: bereit, meldung: text, protokoll: erg.ausgabe)
        }
        var zustand: UpdateZustand
        switch z["zustand"] as? String ?? "" {
        case "aktuell": zustand = .aktuell
        case "bereitgestellt", "wartet": zustand = .updateVerfuegbar
        case "nicht_installiert": zustand = .nichtInstalliert
        // Blockiert ist kein Fehler der Zentrale (die letzte funktionierende Version bleibt), aber auch nicht "aktuell":
        // ein neuerer Stand wartet, der Grund steht auf der Karte.
        case "blockiert": zustand = .updateVerfuegbar
        default: zustand = .unbekannt
        }
        if nachInstallation && kurz == "installiert" {
            zustand = .fertig
        } else if nachInstallation {
            // Nichts installiert (wartet, blockiert, schon aktuell): kein Fehler, keine Uebergabe an Claude Code; der Grund steht auf der Karte.
            zustand = .abgebrochen
        }
        return PruefErgebnis(zustand: zustand, installierteVersion: installiert, verfuegbareVersion: bereit, meldung: text, protokoll: erg.ausgabe)
    }
}
