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
    @MainActor private static var letzteAutomatische: [String: Date] = [:]

    /// Merkt die Uebergabe gleich vor, damit ein zweiter Fehler im selben Sammellauf keine zweite Sitzung startet.
    @MainActor static func automatischErlaubt(_ id: String) -> Bool {
        if let zuletzt = letzteAutomatische[id], Date().timeIntervalSince(zuletzt) < sperrfrist { return false }
        letzteAutomatische[id] = Date()
        return true
    }

    /// Nil bei Erfolg, sonst warum keine Sitzung gestartet wurde.
    static func starten(_ eintrag: ProgrammEintrag, _ bericht: UpdateBericht) async -> String? {
        let launcher = OpenCodeSitzungAktualisierer.launcherExe
        guard FileManager.default.isExecutableFile(atPath: launcher) else {
            return "OpenLauncher nicht gefunden: " + launcher
        }

        let ordner = (Pfade.benutzerOrdner as NSString).appendingPathComponent("auftraege")
        Pfade.ordnerAnlegen(ordner)
        let stempel = DateFormatter()
        stempel.dateFormat = "yyyyMMdd-HHmmss"
        let datei = (ordner as NSString).appendingPathComponent("fehler-\(eintrag.id)-\(stempel.string(from: Date())).md")
        do {
            try auftragText(eintrag, bericht).write(toFile: datei, atomically: true, encoding: .utf8)
        } catch {
            Diagnose.ereignis(.warnung, "uebergabe", "auftrag.schreiben", error.localizedDescription, ["programm": eintrag.id])
            return "Übergabe an Claude Code fehlgeschlagen: \(error.localizedDescription)"
        }

        let argumente = OpenCodeSitzungAktualisierer.argumente(datei, name: "Updater-Zentrale: Fehler \(eintrag.id)")
        guard let code = await OpenCodeSitzungAktualisierer.launcherStarten(argumente) else {
            return "Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar."
        }
        return code == 0 ? nil
            : "Der OpenLauncher konnte die Sitzung nicht starten (Code \(code)) – Einzelheiten im OpenLauncher-Log."
    }

    private static func auftragText(_ eintrag: ProgrammEintrag, _ bericht: UpdateBericht) -> String {
        let zeit = DateFormatter()
        zeit.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        var t = "Auftrag von der Updater-Zentrale (macOS): Ein Update ist fehlgeschlagen. Analysiere die Ursache und behebe das Problem.\n"
        t += "Das kann am Update-Weg der Updater-Zentrale liegen (" + Pfade.projektOrdner
           + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.\n\n"
        t += "Gehe so vor:\n"
        t += "1. Finde die Ursache im Protokoll unten (bei Bedarf in den Protokolldateien).\n"
        t += "2. Bring das Update zu Ende. Nutze schon geladene Downloads im Homebrew- bzw. Paket-Cache und lade große Dateien nicht unnötig erneut.\n"
        t += "3. Sorge dafür, dass dieser Fehler nicht wieder auftritt: Bau die Vorbeugung dauerhaft in die Updater-Zentrale ein "
           + "(programs.json, Update-Weg im Code oder das Update-Skript des Programms), möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall.\n"
        t += "4. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter ~/proggs/bugs fest, damit die Updater-Zentrale aus jedem Fehler lernt.\n\n"
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
            for zeile in zeilen.reversed() {
                let kurz = zeile.count > 2000 ? String(zeile.prefix(2000)) + " …" : zeile
                if laenge + kurz.count >= protokollZeichen { break }
                ende.insert(kurz, at: 0)
                laenge += kurz.count + 1
            }
            t += "\n" + (ende.count < zeilen.count
                ? "Protokoll von \(eintrag.id) (letzte \(ende.count) von \(zeilen.count) Zeilen, den Rest in der Datei oben lesen):\n"
                : "Protokoll von \(eintrag.id):\n")
            t += "----\n" + ende.joined(separator: "\n") + "\n----\n"
        }
        return t
    }
}
