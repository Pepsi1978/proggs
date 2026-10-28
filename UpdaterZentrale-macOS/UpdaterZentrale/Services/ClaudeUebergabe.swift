import Foundation

/// Uebergibt einen gescheiterten Lauf an eine neue Claude-Code-Sitzung im Terminal: Opus 5.5,
/// Profil minimal, Schnellmodus, Effort high. Nutzt denselben OpenLauncher-Start wie die
/// OpenCode-Sitzungskarte (--jarvis-auftrag, Exit-Code geprueft), mit dem Problem als erster
/// Nachricht. Port von Services/ClaudeUebergabe.cs (UpdateZentrale 1.3.41/1.3.42).
enum ClaudeUebergabe {

    /// Der Launcher reicht den ganzen Auftrag als ein Argument an claude weiter. Das Ende des
    /// Protokolls bleibt deshalb kurz; das volle Protokoll steht per Pfad im Auftrag.
    private static let protokollZeichen = 14000

    /// Automatische Uebergaben je Programm hoechstens einmal in dieser Frist (Klick geht immer).
    static let sperrfrist: TimeInterval = 6 * 3600

    /// Nie zwei automatische Sitzungen zugleich: sie wuerden dieselben Dateien aendern und auf main pushen.
    /// Wer spaeter kommt, wartet und wird danach gegen die Sperrfrist geprueft -- verloren geht keiner.
    @MainActor private static var automatischLaeuft = false

    /// Nur echte Fehlschlaege; "nicht verifiziert" heisst oft nur "Version nicht feststellbar".
    static func automatischRelevant(_ bericht: UpdateBericht) -> Bool { bericht.ergebnis == .fehlgeschlagen }

    /// `gestartet`: eine Sitzung laeuft (vermutlich). `problem`: warum nicht, oder was unklar blieb.
    struct AutoErgebnis { var gestartet = false; var problem: String? }

    /// Startet EINE Sitzung fuer alle Fehler ausserhalb der Sperrfrist und merkt sie nach dem Start vor --
    /// auch wenn der Launcher nur nicht rechtzeitig antwortete, denn dann laeuft die Sitzung meist doch.
    @MainActor static func automatischStarten(_ faelle: [(ProgrammEintrag, UpdateBericht)],
                                              _ einstellungen: Einstellungen) async -> AutoErgebnis {
        guard faelle.contains(where: { automatischRelevant($0.1) }) else { return AutoErgebnis() }
        while automatischLaeuft {
            // Abgebrochen: aussteigen statt ohne Pause auf dem MainActor zu kreisen.
            do { try await Task.sleep(nanoseconds: 500_000_000) } catch { return AutoErgebnis() }
        }
        automatischLaeuft = true
        defer { automatischLaeuft = false }

        let offen = faelle.filter { eintrag, bericht in
            guard automatischRelevant(bericht) else { return false }
            guard let zuletzt = einstellungen.fuer(eintrag.id).automatischUebergebenAm else { return true }
            return Date().timeIntervalSince(zuletzt) >= sperrfrist
        }
        guard !offen.isEmpty else {
            Diagnose.ereignis(.info, "uebergabe", "auto.gesperrt", "Sperrfrist läuft",
                              ["programme": faelle.filter { automatischRelevant($0.1) }.map(\.0.id).joined(separator: ",")])
            return AutoErgebnis()
        }
        // VOR dem Start vorgemerkt: endet die App waehrend des Launcher-Starts (Selbst-Update), gilt
        // die Sperrfrist trotzdem. Scheitert der Start sicher, wird die Marke zurueckgenommen.
        let bisher = offen.map { einstellungen.fuer($0.0.id).automatischUebergebenAm }
        for (eintrag, _) in offen { einstellungen.fuer(eintrag.id).automatischUebergebenAm = Date() }
        einstellungen.speichern()
        let (problem, vielleicht) = await startenKern(offen)
        if problem != nil {
            for ((eintrag, _), alt) in zip(offen, bisher) {
                // Unklar (Launcher antwortete nicht): nur eine Stunde sperren statt sechs -- haengt er
                // an einem Dialog, kommt der Fehler bald noch einmal an.
                einstellungen.fuer(eintrag.id).automatischUebergebenAm =
                    vielleicht ? Date().addingTimeInterval(3600 - sperrfrist) : alt
            }
            einstellungen.speichern()
        }
        return AutoErgebnis(gestartet: problem == nil || vielleicht, problem: problem)
    }

    static func starten(_ eintrag: ProgrammEintrag, _ bericht: UpdateBericht) async -> String? {
        await starten([(eintrag, bericht)])
    }

    /// Nil bei Erfolg, sonst warum keine Sitzung gestartet wurde.
    static func starten(_ faelle: [(ProgrammEintrag, UpdateBericht)]) async -> String? {
        await startenKern(faelle).problem
    }

    /// `vielleicht`: der Launcher antwortete nicht rechtzeitig -- die Sitzung laeuft womoeglich doch.
    private static func startenKern(_ faelle: [(ProgrammEintrag, UpdateBericht)]) async -> (problem: String?, vielleicht: Bool) {
        guard let erster = faelle.first else { return (nil, false) }
        let launcher = OpenCodeSitzungAktualisierer.launcherExe
        guard FileManager.default.isExecutableFile(atPath: launcher) else {
            return ("OpenLauncher nicht gefunden: " + launcher, false)
        }

        let ordner = (Pfade.benutzerOrdner as NSString).appendingPathComponent("auftraege")
        Pfade.ordnerAnlegen(ordner)
        let stempel = DateFormatter()
        stempel.dateFormat = "yyyyMMdd-HHmmss"
        let kennung = faelle.count == 1 ? erster.0.id : "sammel-\(faelle.count)"
        let datei = (ordner as NSString).appendingPathComponent("fehler-\(kennung)-\(stempel.string(from: Date())).md")
        do {
            try auftragText(faelle).write(toFile: datei, atomically: true, encoding: .utf8)
        } catch {
            Diagnose.ereignis(.warnung, "uebergabe", "auftrag.schreiben", error.localizedDescription, ["programm": kennung])
            return ("Übergabe an Claude Code fehlgeschlagen: \(error.localizedDescription)", false)
        }

        let argumente = OpenCodeSitzungAktualisierer.argumente(datei, name: "Updater-Zentrale: Fehler \(kennung)")
        guard let code = await OpenCodeSitzungAktualisierer.launcherStarten(argumente) else {
            return ("Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.", true)
        }
        return code == 0 ? (nil, false)
            : ("Der OpenLauncher konnte die Sitzung nicht starten (Code \(code)) – Einzelheiten im OpenLauncher-Log.", false)
    }

    private static func auftragText(_ faelle: [(ProgrammEintrag, UpdateBericht)]) -> String {
        let zeit = DateFormatter()
        zeit.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        var t = "Auftrag von der Updater-Zentrale (macOS): " + (faelle.count == 1 ? "Ein Update ist" : "\(faelle.count) Updates sind")
           + " fehlgeschlagen. Analysiere die Ursache und behebe das Problem.\n"
        t += "Das kann am Update-Weg der Updater-Zentrale liegen (" + Pfade.projektOrdner
           + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.\n\n"
        t += "Gehe so vor:\n"
        t += "1. Finde die Ursache im Protokoll unten (bei mehreren Fehlern: jeden einzeln) (bei Bedarf in den Protokolldateien).\n"
        t += "2. Bring das Update zu Ende. Nutze schon geladene Downloads im Homebrew- bzw. Paket-Cache und lade große Dateien nicht unnötig erneut.\n"
        t += "3. Sorge dafür, dass dieser Fehler nicht wieder auftritt: Bau die Vorbeugung dauerhaft in die Updater-Zentrale ein "
           + "(programs.json, Update-Weg im Code oder das Update-Skript des Programms), möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall.\n"
        t += "4. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter ~/proggs/bugs fest, damit die Updater-Zentrale aus jedem Fehler lernt.\n\n"
        for (eintrag, bericht) in faelle {
            t += faelle.count > 1 ? "\n=== \(bericht.name) ===\n" : ""
            t += "Programm: \(bericht.name) (Id \(eintrag.id), Art \(eintrag.art))\n"
            t += "Zeitpunkt: \(zeit.string(from: bericht.zeit))\n"
            t += "Ergebnis: \(bericht.ergebnisText), Exit-Code \(bericht.exitCode)\n"
            if !bericht.versionVorher.istLeer || !bericht.versionNachher.istLeer {
                t += "Version vorher/nachher: \(bericht.versionVorher) → \(bericht.versionNachher)\n"
            }
            if !bericht.befehl.istLeer { t += "Befehl: \(bericht.befehl)\n" }
            t += "Meldung: \(bericht.meldung)\n"
            t += "Protokolldatei (vollständig): \(bericht.protokollDatei)\n"
            t += "Weitere Protokolle (Tagesprotokolle, Diagnose-JSONL, verlauf.jsonl): \(Protokollierung.ordner)\n"

            // Das Tagesprotokoll teilen sich alle Programme; nur die Zeilen dieses Programms zaehlen.
            // \r-Fortschrittsbalken werden zu eigenen Zeilen, damit keine Riesenzeile den Auszug sprengt.
            if let inhalt = try? String(contentsOfFile: bericht.protokollDatei, encoding: .utf8) {
                let marke = "[\(eintrag.id)]"
                let zeilen = inhalt.components(separatedBy: CharacterSet(charactersIn: "\r\n"))
                    .filter { $0.contains(marke) }
                var ende: [String] = []
                var laenge = 0
                let budget = protokollZeichen / faelle.count
                for zeile in zeilen.reversed() {
                    var kurz = zeile.count > 2000 ? String(zeile.prefix(2000)) + " …" : zeile
                    if laenge + kurz.count >= budget {
                        // Die letzte Zeile ist meist die entscheidende Fehlermeldung: gekuerzt mitnehmen.
                        guard ende.isEmpty else { break }
                        kurz = String(kurz.prefix(max(200, budget - 2))) + " …"
                    }
                    ende.insert(kurz, at: 0)
                    laenge += kurz.count + 1
                    if laenge >= budget { break }
                }
                t += "\n" + (ende.count < zeilen.count
                    ? "Protokoll von \(eintrag.id) (letzte \(ende.count) von \(zeilen.count) Zeilen, den Rest in der Datei oben lesen):\n"
                    : "Protokoll von \(eintrag.id):\n")
                t += "----\n" + ende.joined(separator: "\n") + "\n----\n"
            }
        }
        return t
    }
}
