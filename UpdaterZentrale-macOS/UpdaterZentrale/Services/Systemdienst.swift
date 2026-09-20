import Foundation

/// Liest die Systemtatsachen, die die Oberflaeche pro Programm zeigt.
/// Port von Services/Systemdienst.cs -- reduziert auf das, was es auf macOS wirklich gibt.
///
/// Weggefallen gegenueber Windows (siehe PORTING.md, Abschnitt "Nicht portiert"):
///   * "Als Administrator ausfuehren" (AppCompatFlags\Layers, RUNASADMIN) -- macOS kennt keinen
///     solchen Schalter. Ein Programm laeuft mit den Rechten dessen, der es startet.
///   * Das Umlegen des Autostarts auf eine geplante Aufgabe mit hoechsten Rechten -- es gibt kein
///     Problem, das es loesen muesste: LaunchAgents starten auch Programme, die unter Windows von
///     der Benutzerkontensteuerung uebersprungen wuerden.
///
/// Geblieben ist das Autostart-Kennzeichen. Es wird ausschliesslich GELESEN, nie geschrieben:
/// die Overlays bringen ihre LaunchAgents selbst mit, und ein Update-Werkzeug hat darin nichts
/// zu aendern.
enum Systemdienst {

    private static var launchAgentsOrdner: String {
        (Pfade.heim as NSString).appendingPathComponent("Library/LaunchAgents")
    }

    /// True, wenn ein LaunchAgent oder ein Anmeldeobjekt auf dieses Programm zeigt.
    ///
    /// Verglichen wird wie unter Windows ueber den WERT, nie ueber den Namen: der Name eines
    /// Agents ist frei gewaehlt ("com.frank.terminalvoiceoverlay") und wuerde das Kennzeichen an
    /// die falsche Karte haengen. Auch Huellen zaehlen -- die Overlays starten ueber ihren
    /// Agent mit einem Pfad in ihren Repo-Ordner, nicht ueber das installierte Buendel.
    static func imAutostart(_ eintrag: ProgrammEintrag) -> Bool {
        let ziel = eintrag.zielPfad
        let ordner = eintrag.repoOrdner.map {
            (Pfade.repoWurzel as NSString).appendingPathComponent($0)
        }

        for befehlszeile in autostartBefehle() {
            if !ziel.isEmpty, befehlszeile.localizedCaseInsensitiveContains(ziel) { return true }
            if let ordner, befehlszeile.localizedCaseInsensitiveContains(ordner) { return true }
        }
        return false
    }

    /// Alle Startbefehle, die beim Anmelden laufen: LaunchAgents des Benutzers plus Anmeldeobjekte.
    private static func autostartBefehle() -> [String] {
        var befehle: [String] = []

        // 1. LaunchAgents -- der Weg, den die eigenen Werkzeuge nutzen.
        if let dateien = try? FileManager.default.contentsOfDirectory(atPath: launchAgentsOrdner) {
            for datei in dateien where datei.hasSuffix(".plist") {
                let pfad = (launchAgentsOrdner as NSString).appendingPathComponent(datei)
                guard let daten = FileManager.default.contents(atPath: pfad),
                      let plist = try? PropertyListSerialization.propertyList(from: daten, options: [], format: nil),
                      let woerterbuch = plist as? [String: Any] else { continue }

                if let programm = woerterbuch["Program"] as? String { befehle.append(programm) }
                if let argumente = woerterbuch["ProgramArguments"] as? [String] {
                    befehle.append(argumente.joined(separator: " "))
                }
            }
        }

        // 2. Anmeldeobjekte aus den Systemeinstellungen -- der Weg, den GUI-Programme meist nehmen.
        befehle.append(contentsOf: anmeldeObjekte())
        return befehle
    }

    /// Die Anmeldeobjekte des Benutzers. Kostet einen AppleScript-Aufruf, deshalb wird das
    /// Ergebnis fuer die Lebensdauer des Fensters gemerkt -- die Liste aendert sich nicht,
    /// waehrend die App laeuft, und beim "Zustaende auffrischen" wird sie verworfen.
    private nonisolated(unsafe) static var anmeldeObjekteZwischenspeicher: [String]?

    static func zwischenspeicherLeeren() { anmeldeObjekteZwischenspeicher = nil }

    private static func anmeldeObjekte() -> [String] {
        if let gemerkt = anmeldeObjekteZwischenspeicher { return gemerkt }

        let rohr = Pipe()
        let prozess = Process()
        prozess.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        prozess.arguments = ["-e", "tell application \"System Events\" to get the path of every login item"]
        prozess.standardOutput = rohr
        prozess.standardError = FileHandle.nullDevice

        guard (try? prozess.run()) != nil else {
            anmeldeObjekteZwischenspeicher = []
            return []
        }
        let daten = rohr.fileHandleForReading.readDataToEndOfFile()
        prozess.waitUntilExit()

        let text = String(data: daten, encoding: .utf8) ?? ""
        let liste = text.split(separator: ",").map { String($0).trimmed }.filter { !$0.isEmpty }
        anmeldeObjekteZwischenspeicher = liste
        return liste
    }
}
