import Foundation

/// Uebergibt einen gescheiterten Lauf oder ein ganzes anstehendes Update an eine neue
/// Claude-Code-Sitzung im Terminal: Opus 5.5, Profil minimal, Schnellmodus, Effort high. Nutzt
/// denselben OpenLauncher-Start wie die OpenCode-Sitzungskarte (--jarvis-auftrag, Exit-Code
/// geprueft), mit dem Auftrag als erster Nachricht. Port von Services/ClaudeUebergabe.cs.
enum ClaudeUebergabe {

    /// Der Launcher reicht den ganzen Auftrag als ein Argument an claude weiter. Das Ende des
    /// Protokolls bleibt deshalb kurz; das volle Protokoll steht per Pfad im Auftrag.
    private static let protokollZeichen = 14000

    /// Only keeps a second session from working on the same failure while the first one still runs:
    /// a program that fails again after that repair must reach a new session right away. A click always goes through.
    static let sperrfrist: TimeInterval = 3600

    /// What a card or the batch line says when the lock kept a failure from a new session.
    static var sperrHinweis: String {
        "Dieser Fehler ging in den letzten \(Int(sperrfrist / 60)) Minuten schon an Claude Code – „An Claude Code übergeben“ startet eine neue Sitzung."
    }

    /// Serialises the launcher starts, so the lock is read after the previous start has marked its
    /// programs. Sessions for different programs can still run side by side; the task text therefore
    /// tells them how to share main. Who comes later waits and is then checked against the lock.
    @MainActor private static var automatischLaeuft = false

    /// Jeder Lauf, den die Karte als Fehler zeigt, auch "nicht verifiziert", was auch einen nicht lesbaren
    /// Stand meinen kann: der Auftrag laesst das zuerst pruefen. Ausgenommen sind ein vom Benutzer abgebrochener
    /// Lauf und ein Netz, das ueber alle Versuche gestoert blieb: das kann eine Sitzung nicht reparieren.
    static func automatischRelevant(_ bericht: UpdateBericht) -> Bool {
        bericht.istFehler && !bericht.ohneAutoUebergabe && !bericht.meldung.hasPrefix(BrewAktualisierer.netzGestoertMeldung)
    }

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
        Diagnose.ereignis(problem == nil ? .info : .warnung, "uebergabe", "auto.start", problem ?? "Sitzung gestartet",
                          ["programme": offen.map(\.0.id).joined(separator: ",")])
        // Unclear (the launcher did not answer): the session usually runs anyway, so the mark stays.
        if problem != nil && !vielleicht {
            for ((eintrag, _), alt) in zip(offen, bisher) {
                einstellungen.fuer(eintrag.id).automatischUebergebenAm = alt
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
        let kennung = faelle.count == 1 ? erster.0.id : "sammel-\(faelle.count)"
        return await sitzungStarten(dateiName: "fehler-\(kennung)", sitzungsName: "Updater-Zentrale: Fehler \(kennung)",
                                    auftrag: auftragText(faelle))
    }

    /// Uebergibt ein ganzes anstehendes Update an eine neue Sitzung.
    /// - Parameters:
    ///   - befehl: der Weg, auf dem diese Karte das Update selbst ausfuehren wuerde.
    ///   - beendenZugestimmt: das Programm laeuft, und der Benutzer hat dem Beenden zugestimmt.
    ///   - skriptZugestimmt: das Skript fragt nicht selbst, der Benutzer hat die Frage der App beantwortet.
    /// - Returns: `problem`: warum keine Sitzung startete; `vielleicht`: der Launcher antwortete nicht, sie laeuft womoeglich doch.
    static func updateStarten(_ eintrag: ProgrammEintrag, installiert: String, verfuegbar: String, befehl: String,
                              beendenZugestimmt: Bool, skriptZugestimmt: Bool) async -> (problem: String?, vielleicht: Bool) {
        await sitzungStarten(dateiName: "update-\(eintrag.id)", sitzungsName: "Updater-Zentrale: Update \(eintrag.id)",
                             auftrag: updateAuftragText(eintrag, installiert: installiert, verfuegbar: verfuegbar, befehl: befehl,
                                                        beendenZugestimmt: beendenZugestimmt, skriptZugestimmt: skriptZugestimmt))
    }

    private static func sitzungStarten(dateiName: String, sitzungsName: String,
                                       auftrag: @autoclosure () -> String) async -> (problem: String?, vielleicht: Bool) {
        let launcher = OpenCodeSitzungAktualisierer.launcherExe
        guard FileManager.default.isExecutableFile(atPath: launcher) else {
            return ("OpenLauncher nicht gefunden: " + launcher, false)
        }

        let ordner = (Pfade.benutzerOrdner as NSString).appendingPathComponent("auftraege")
        Pfade.ordnerAnlegen(ordner)
        let stempel = DateFormatter()
        stempel.dateFormat = "yyyyMMdd-HHmmss"
        let datei = (ordner as NSString).appendingPathComponent("\(dateiName)-\(stempel.string(from: Date())).md")
        do {
            try auftrag().write(toFile: datei, atomically: true, encoding: .utf8)
        } catch {
            Diagnose.ereignis(.warnung, "uebergabe", "auftrag.schreiben", error.localizedDescription, ["programm": dateiName])
            return ("Übergabe an Claude Code fehlgeschlagen: \(error.localizedDescription)", false)
        }

        let argumente = OpenCodeSitzungAktualisierer.argumente(datei, name: sitzungsName)
        guard let code = await OpenCodeSitzungAktualisierer.launcherStarten(argumente) else {
            return ("Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.", true)
        }
        return code == 0 ? (nil, false)
            : ("Der OpenLauncher konnte die Sitzung nicht starten (Code \(code)) – Einzelheiten im OpenLauncher-Log.", false)
    }

    private static func updateAuftragText(_ eintrag: ProgrammEintrag, installiert: String, verfuegbar: String, befehl: String,
                                          beendenZugestimmt: Bool, skriptZugestimmt: Bool) -> String {
        let von = installiert.istLeer ? "unbekannt" : installiert
        let nach = verfuegbar.istLeer ? "unbekannt" : verfuegbar
        var t = "Auftrag von der Updater-Zentrale (macOS): Führe das Update von \(eintrag.name) vollständig aus (\(von) → \(nach)). "
              + "Die Versionen stammen von der letzten Prüfung, lies den Stand zuerst frisch.\n\n"
        t += "Gehe so vor:\n"
        t += "1. Führe das Update über den Weg der Updater-Zentrale aus: \(befehl). Einzelheiten stehen im Katalogeintrag unten, die Umsetzung in "
           + Pfade.projektOrdner + "/UpdaterZentrale/Providers. Eigene Werkzeuge (OpenLauncher, TVO, CVO, Updater-Zentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.\n"
        t += "2. Klemmt dieser Weg: Finde die Ursache und bring das Update auf einem anderen sicheren Weg zu Ende. "
           + "Ein Nein oder keine Antwort in einem Rückfragefenster (cancelled, no-answer) ist kein Klemmen: nicht erneut starten, sondern melden.\n"
        t += "3. Prüfe am Ende die installierte Version nach und melde das Ergebnis.\n"
        t += "4. Hat der Weg der Updater-Zentrale versagt: Bau die Vorbeugung dauerhaft ein (" + Pfade.projektOrdner
           + ", Katalog programs.json) und halte Ursache und Vorbeugung im passenden Bug-Almanach unter ~/proggs/bugs fest.\n"
        t += "\nBeende andere laufende Programme nicht ohne Rückfrage.\n"
        if eintrag.beendenVorUpdate {
            t += beendenZugestimmt
                ? "\(eintrag.name) läuft und muss fürs Update beendet werden, dem habe ich beim Klick zugestimmt.\n"
                : "\(eintrag.name) muss fürs Update beendet sein. Beim Klick lief es nicht; läuft es inzwischen, frag vor dem Beenden nach.\n"
        }
        if skriptZugestimmt { t += "Dem Neubau über das Skript habe ich beim Klick zugestimmt, das Skript fragt nicht selbst.\n" }
        if eintrag.kannCliBeenden {
            t += "\nAchtung, dieses Update kann laufende CLI-Sitzungen beenden, auch diese hier: \(eintrag.cliRisiko ?? "")\n"
            t += "Starte es abgekoppelt, damit es auch dann zu Ende läuft, wenn diese Sitzung endet. Verlangt es geschlossene Terminalfenster, melde das, statt es zu erzwingen. "
               + "Endet die Sitzung vorher, bestätigt die nächste Prüfung der Updater-Zentrale das Ergebnis.\n"
        }
        t += "\nProtokolle der Updater-Zentrale: \(Protokollierung.ordner)\n\nKatalogeintrag:\n----\n"
        let kodierer = JSONEncoder()
        kodierer.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        if let daten = try? kodierer.encode(eintrag), let json = String(data: daten, encoding: .utf8) {
            t += json + "\n"
        } else {
            t += "(Id \(eintrag.id), Art \(eintrag.art); der Eintrag steht in programs.json)\n"
        }
        return t + "----\n"
    }

    private static func auftragText(_ faelle: [(ProgrammEintrag, UpdateBericht)]) -> String {
        let zeit = DateFormatter()
        zeit.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        var t = "Auftrag von der Updater-Zentrale (macOS): " + (faelle.count == 1 ? "Ein Update ist" : "\(faelle.count) Updates sind")
           + " fehlgeschlagen. Finde die Ursache, repariere sie dauerhaft und bring danach das Update zu Ende.\n"
        t += "Das kann am Update-Weg der Updater-Zentrale liegen (" + Pfade.projektOrdner
           + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.\n"
        t += "REVIEW: ja\n\n"
        t += "Arbeite diese Schritte automatisch und genau in dieser Reihenfolge ab:\n"
        t += "1. Finde die Ursache im Protokoll unten (bei mehreren Fehlern: jeden einzeln, bei Bedarf in den Protokolldateien). "
           + "Bei „Nicht verifiziert“ prüfe zuerst, ob das Update doch angekommen ist; dann ist nur die Erkennung zu reparieren.\n"
        t += "2. Repariere dauerhaft, damit dieser Fehler beim nächsten Mal nicht mehr auftritt: Bau die Vorbeugung in die Updater-Zentrale ein "
           + "(programs.json, Update-Weg im Code oder das Update-Skript des Programms), möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall. "
           + "Kann dieselbe Ursache auch unter Windows auftreten, zieh ~/proggs/UpdateZentrale mit.\n"
        t += "3. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter ~/proggs/bugs fest, damit die Updater-Zentrale aus jedem Fehler lernt. Committe und pushe die Änderungen.\n"
        t += "4. Führe danach das Update jeder gescheiterten Software zu Ende und prüfe die installierte Version nach. Nutze schon geladene Downloads im Homebrew- bzw. Paket-Cache und lade große Dateien nicht unnötig erneut. "
           + "Wirkt die Reparatur erst in der neu gebauten Updater-Zentrale, führe den reparierten Weg hier selbst aus.\n"
        t += "5. Führe danach ohne Rückfrage das Standard-Code-Review über die Änderungen dieser Sitzung aus (Skill /code-review), behebe die Befunde, committe und pushe. Ohne Codeänderung entfällt das Review.\n"
        t += "6. Baue als allerletzten Schritt die Updater-Zentrale neu, sobald sich in " + Pfade.projektOrdner + " etwas geändert hat: bash " + Pfade.projektOrdner
           + "/update-zentrale.sh, mit dem höchsten erlaubten Zeitlimit. Läuft die App, zeigt mir das Skript ein Ja/Nein-Fenster und schließt sie danach. "
           + "Prüfe danach in ~/Library/Logs/UpdaterZentrale-selbstupdate.log, ob die neue Fassung gestartet ist.\n\n"
        t += "Regeln für diese Sitzung:\n"
        t += "- Beende andere laufende Programme nicht ohne Rückfrage. Eigene Werkzeuge (OpenLauncher, TVO, CVO, Updater-Zentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.\n"
        t += "- Ein Nein oder keine Antwort in einem Rückfragefenster (cancelled, no-answer) ist kein Klemmen: nicht erneut starten, sondern melden.\n"
        t += "- Andere Sitzungen können parallel an diesem Repo arbeiten: Lies die Versionsnummer direkt vor dem Bump frisch und committe nur deine eigenen Pfade.\n"
        t += "- Die Updater-Zentrale kann währenddessen noch weitere Updates ausführen. Meldet dein Update eine Sperre (brew, npm), warte und versuche es erneut, statt fremde Prozesse zu beenden. "
           + "Schritt 6 schließt die App und bräche so einen Sammellauf ab: Trägt die letzte Zeile von " + Protokollierung.sammelDatei
           + " das Ereignis „plan“ oder „fortsetzung“, läuft er noch. Warte dann vor Schritt 6 auf sein Ende, höchstens zwei Stunden.\n\n"
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
            if eintrag.id.caseInsensitiveCompare(Sammelplan.selbstId) == .orderedSame {
                t += "Das ist die Updater-Zentrale selbst: Schritt 4 entfällt für sie, ihr Update erledigt Schritt 6, auch ohne Änderung in ihrem Ordner.\n"
            }
            if eintrag.beendenVorUpdate {
                t += "\(eintrag.name) muss fürs Update beendet sein. Läuft es gerade, frag vor dem Beenden nach.\n"
            }
            if eintrag.kannCliBeenden {
                t += "Achtung, dieses Update kann laufende CLI-Sitzungen beenden, auch diese hier: \(eintrag.cliRisiko ?? "") "
                   + "Führe es deshalb erst nach den Schritten 5 und 6 aus und starte es abgekoppelt, damit es auch dann zu Ende läuft, wenn diese Sitzung endet. "
                   + "Verlangt es geschlossene Terminalfenster, melde das, statt es zu erzwingen.\n"
            }

            // Das Tagesprotokoll teilen sich alle Programme; nur die Zeilen dieses Programms zaehlen.
            // \r-Fortschrittsbalken werden zu eigenen Zeilen, damit keine Riesenzeile den Auszug sprengt.
            // Tolerant dekodiert: ungueltige Bytes einer Werkzeug-Ausgabe duerfen den Auszug nicht kosten.
            if let daten = FileManager.default.contents(atPath: bericht.protokollDatei) {
                let inhalt = String(decoding: daten, as: UTF8.self)
                let marke = "[\(eintrag.id)]"
                let alleZeilen = inhalt.components(separatedBy: CharacterSet(charactersIn: "\r\n"))
                let eigene = alleZeilen.filter { $0.contains(marke) }
                let zeilen = eigene.isEmpty ? alleZeilen : eigene
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
