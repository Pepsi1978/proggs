import Foundation

/// OpenCode, wie der OpenLauncher es startet: nicht das npm-Paket, sondern der eigene Build mit dem
/// Mac-Fix unter ~/.local/share/opencode-mousefix (Zeigerdatei current.json, gebaut von
/// opencode-setup/build-install-macos-tuifix.sh). Port von Providers/OpenCodeSitzungAktualisierer.cs
/// samt der Pruefung aus OpenCodeFixAktualisierer.cs.
///
/// Das Bauskript bricht ab, sobald die Patches nicht mehr auf den neuen Quellstand passen -- sie
/// anzupassen ist Arbeit fuer ein CLI. Ein offenes Update startet deshalb eine OpenLauncher-Sitzung
/// (Opus 5.5, Profil minimal, Schnellmodus, Effort high) ueber den fensterlosen Einstieg des
/// Launchers (--jarvis-auftrag), mit dem Auftrag als erster Nachricht.
///
/// Die Karte steht danach auf "Sitzung baut" (ausstehend); die naechste Pruefung bestaetigt das
/// Update, sobald current.json auf die neue Version zeigt (UpdateKette.stagedUrteil).
struct OpenCodeSitzungAktualisierer: Aktualisierer {
    let art = "opencodesitzung"

    static let profil = "minimal"
    static let modus = "schnell"
    static let modell = "claude-opus-5-5[1m]"
    static let effort = "high"
    static let sitzungsName = "Updater-Zentrale: OpenCode-Update"

    /// Eine Sitzung fuer dieselbe Version startet hoechstens alle drei Stunden.
    static let sperrzeit: TimeInterval = 3 * 60 * 60

    static var wurzel: String {
        (NSHomeDirectory() as NSString).appendingPathComponent(".local/share/opencode-mousefix")
    }

    private static var zeigerDatei: String { (wurzel as NSString).appendingPathComponent("current.json") }
    private static var markerDatei: String {
        (Pfade.benutzerOrdner as NSString).appendingPathComponent("opencode-sitzung.json")
    }

    static let launcherExe = "/Applications/OpenLauncher.app/Contents/MacOS/OpenLauncher"

    /// Was der Protokollkopf der Karte zeigt.
    static var befehlsbeschreibung: String {
        "OpenLauncher --jarvis-auftrag <auftrag> --profil \(profil) --modus \(modus) --modell \(modell) --effort \(effort)"
    }

    // MARK: - Pruefen

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        guard let aktiv = Self.aktiveVersion() else {
            return PruefErgebnis(zustand: .nichtInstalliert, meldung: "Kein Mac-Fix-Build aktiv: " + Self.zeigerDatei)
        }
        protokoll.berichte("Aktiv: " + aktiv.custom)
        let npm = await CliAktualisierer.netzVersion("https://registry.npmjs.org/\(eintrag.npmPaket ?? "opencode-ai")/latest",
                                                     json: true, protokoll)
        guard let neueste = npm.version else {
            return PruefErgebnis(zustand: .fehler, installierteVersion: aktiv.custom,
                                 meldung: npm.problem ?? "Registry-Abfrage fehlgeschlagen.")
        }
        if !Versionen.istNeuer(neueste, als: aktiv.upstream) {
            return PruefErgebnis(zustand: .aktuell, installierteVersion: aktiv.custom, verfuegbareVersion: neueste,
                                 meldung: "Auf dem neuesten Stand (mit Mac-Fix).")
        }
        return PruefErgebnis(zustand: .updateVerfuegbar, installierteVersion: aktiv.custom, verfuegbareVersion: neueste,
                             meldung: "Neue Version \(neueste) – eine OpenLauncher-Sitzung baut sie mit dem Mac-Fix.")
    }

    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String { Self.aktiveVersion()?.custom ?? "" }

    // MARK: - Aktualisieren

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let npm = await CliAktualisierer.netzVersion("https://registry.npmjs.org/\(eintrag.npmPaket ?? "opencode-ai")/latest",
                                                     json: true, protokoll)
        guard let version = npm.version else {
            return PruefErgebnis(zustand: .fehler, meldung: npm.problem ?? "Registry-Abfrage fehlgeschlagen.")
        }

        let aktiv = Self.aktiveVersion()
        if let aktiv, !Versionen.istNeuer(version, als: aktiv.upstream) {
            return PruefErgebnis(zustand: .aktuell, installierteVersion: aktiv.custom, verfuegbareVersion: version,
                                 meldung: "Bereits aktuell (mit Mac-Fix).")
        }

        // Eine Sitzung zur Zeit, egal fuer welche Version: eine neuere Version, die erscheint,
        // waehrend die erste Sitzung noch den Fix einarbeitet, darf keine zweite auf denselben
        // Patches starten. Die Sperre endet, wenn die gestartete Version aktiv ist, oder nach der Sperrzeit.
        if let frueher = Self.letzterStart(), Date().timeIntervalSince(frueher.zeit) < Self.sperrzeit,
           !(aktiv.map { Versionen.vergleiche($0.upstream, frueher.version) >= 0 } ?? false) {
            protokoll.berichte("Für \(frueher.version) läuft schon eine Sitzung seit \(Formate.uhrzeit(frueher.zeit)) Uhr – keine zweite.")
            return PruefErgebnis(zustand: .fertig,
                                 meldung: "Die OpenLauncher-Sitzung für \(frueher.version) läuft seit \(Formate.uhrzeit(frueher.zeit)) Uhr – "
                                        + "keine zweite. Die Karte bestätigt das Update, sobald die neue Version aktiv ist.",
                                 erstNachNeustart: true)
        }

        guard FileManager.default.isExecutableFile(atPath: Self.launcherExe) else {
            return PruefErgebnis(zustand: .fehler, meldung: "OpenLauncher nicht gefunden: " + Self.launcherExe)
        }

        let ordner = (Pfade.benutzerOrdner as NSString).appendingPathComponent("auftraege")
        Pfade.ordnerAnlegen(ordner)
        let stempel = DateFormatter()
        stempel.dateFormat = "yyyyMMdd-HHmmss"
        let datei = (ordner as NSString).appendingPathComponent("opencode-\(version)-\(stempel.string(from: Date())).md")
        do {
            try Self.auftrag(version, aktiv?.custom).write(toFile: datei, atomically: true, encoding: .utf8)
        } catch {
            return PruefErgebnis(zustand: .fehler, meldung: "Auftragsdatei ließ sich nicht schreiben: \(error.localizedDescription)")
        }

        let argumente = Self.argumente(datei)
        protokoll.berichte("OpenLauncher " + argumente.joined(separator: " "))
        guard let code = await Self.launcherStarten(argumente) else {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.")
        }
        if code != 0 {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Der OpenLauncher konnte die Sitzung nicht starten (Code \(code)) – Einzelheiten im OpenLauncher-Log.")
        }

        Self.startMerken(version)
        protokoll.berichte("Sitzung gestartet, Auftrag: " + datei)
        return PruefErgebnis(zustand: .fertig,
                             meldung: "OpenLauncher-Sitzung gestartet (Opus 5.5, minimal, Schnellmodus, high): sie baut \(version) "
                                    + "mit dem Mac-Fix. Die Karte bestätigt das Update, sobald die neue Version aktiv ist.",
                             erstNachNeustart: true)
    }

    // MARK: - Intern

    static func argumente(_ auftragsDatei: String, name: String = sitzungsName) -> [String] {
        ["--jarvis-auftrag", auftragsDatei, "--name", name, "--profil", profil, "--modus", modus,
         "--modell", modell, "--effort", effort, "--ordner", Pfade.repoWurzel]
    }

    /// Startet den Launcher und liefert seinen Exit-Code (nil = keine Antwort binnen zwei Minuten).
    /// Bewusst OHNE umgeleitete Pipes: der Launcher oeffnet ein Terminal, das sie erben wuerde, und
    /// das Warten dauerte dann, bis diese Sitzung endet.
    static func launcherStarten(_ argumente: [String]) async -> Int32? {
        let umwelt = Kommandozeile.umgebung
        return await withCheckedContinuation { fortsetzung in
            DispatchQueue.global(qos: .userInitiated).async {
                let prozess = Process()
                prozess.executableURL = URL(fileURLWithPath: launcherExe)
                prozess.arguments = argumente
                prozess.environment = umwelt
                prozess.standardInput = FileHandle.nullDevice
                prozess.standardOutput = FileHandle.nullDevice
                prozess.standardError = FileHandle.nullDevice
                do { try prozess.run() } catch { fortsetzung.resume(returning: -1); return }
                let ende = Date().addingTimeInterval(120)
                while prozess.isRunning && Date() < ende { Thread.sleep(forTimeInterval: 0.25) }
                fortsetzung.resume(returning: prozess.isRunning ? nil : prozess.terminationStatus)
            }
        }
    }

    /// Die erste Nachricht der Sitzung: Ziel, der eine Bauweg, und wann es fertig ist.
    static func auftrag(_ version: String, _ aktiv: String?) -> String {
        "Auftrag von der Updater-Zentrale macOS: OpenCode CLI aktualisieren\n\n"
        + "Ziel: Die neueste OpenCode-Version \(version) läuft mit unserem Mac-Fix "
        + "(aktiv ist bisher \(aktiv ?? "keine Fix-Version")).\n\n"
        + "1. Baue sie mit dem vorhandenen Skript: `bash ~/proggs/opencode-setup/build-install-macos-tuifix.sh --version \(version)`.\n"
        + "2. Bricht das Skript ab, weil die Patches unter `opencode-setup/patches` nicht mehr zur neuen Version passen, "
        + "verschmilz den Mac-Fix mit der neuen Version: Patches an den neuen Quellstand anpassen, die Patch-Revision im Skript erhöhen, erneut bauen. "
        + "Keine Fix-Funktion weglassen (alle `capabilities` in current.json müssen wieder `present` sein).\n"
        + "3. Meldet das Skript, dass schon ein Bau läuft (meist der OpenLauncher): kurz warten und erneut starten.\n"
        + "4. Fertig ist es erst, wenn `~/.local/share/opencode-mousefix/current.json` unter `active.upstreamVersion` \(version) zeigt.\n\n"
        + "Laufende OpenCode-Sitzungen nicht beenden, die bisherige Version bleibt bis zur Aktivierung nutzbar. "
        + "Danach den Pflicht-Abschluss nach den Profilregeln (Version, Commit, Push), falls am Skript oder an den Patches etwas geändert wurde.\n"
    }

    /// Der aktive Build aus current.json -- nur, wenn seine Programmdatei wirklich existiert.
    static func aktiveVersion() -> (upstream: String, custom: String)? {
        guard let daten = FileManager.default.contents(atPath: zeigerDatei),
              let objekt = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any],
              let aktiv = objekt["active"] as? [String: Any],
              let relativ = aktiv["relativeExe"] as? String else { return nil }
        let exe = ((wurzel as NSString).appendingPathComponent(relativ) as NSString).standardizingPath
        guard exe.hasPrefix(wurzel), FileManager.default.fileExists(atPath: exe) else { return nil }
        return ((aktiv["upstreamVersion"] as? String) ?? "", (aktiv["customVersion"] as? String) ?? "")
    }

    private static func letzterStart() -> (version: String, zeit: Date)? {
        guard let daten = FileManager.default.contents(atPath: markerDatei),
              let objekt = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any],
              let version = objekt["version"] as? String,
              let zeitText = objekt["zeit"] as? String,
              let zeit = ISO8601DateFormatter().date(from: zeitText) else { return nil }
        return (version, zeit)
    }

    private static func startMerken(_ version: String) {
        Pfade.ordnerAnlegen(Pfade.benutzerOrdner)
        let objekt: [String: Any] = ["version": version, "zeit": ISO8601DateFormatter().string(from: Date())]
        if let daten = try? JSONSerialization.data(withJSONObject: objekt) {
            try? daten.write(to: URL(fileURLWithPath: markerDatei), options: .atomic)
        }
    }
}
