import AppKit

/// Die eingebaute Kommandozeile. 1:1-Port von Services/Terminal.cs.
///
/// Unter Windows lief jeder Befehl durch PowerShell; auf dem Mac ist das Gegenstueck die
/// Anmelde-Shell `/bin/zsh -lc`. Das `-l` ist der Punkt: nur eine Anmelde-Shell liest .zprofile
/// und hat damit Homebrew im PATH -- eine aus dem Finder gestartete App hat ihn nicht.
enum Terminal {
    @MainActor
    static var arbeitsverzeichnis: String = Pfade.repoWurzel

    static func ausfuehren(_ befehl: String, arbeitsverzeichnis: String) async -> String {
        guard !befehl.istLeer else { return "" }

        let lauf = await Kommandozeile.ausfuehren("/bin/zsh", ["-lc", befehl],
                                                  zeitlimit: 600,
                                                  arbeitsverzeichnis: arbeitsverzeichnis)

        var ausgabe = lauf.ausgabe.istLeer ? "(keine Ausgabe)" : lauf.ausgabe
        if lauf.abgelaufen {
            ausgabe += "\n[Zeitlimit von 10 Minuten überschritten]"
        } else if lauf.exitCode != 0 {
            ausgabe += "\n[Beendet mit Code \(lauf.exitCode)]"
        }
        return ausgabe
    }

    /// Oeffnet ein echtes Terminalfenster im Repo-Ordner.
    /// iTerm, wenn vorhanden -- sonst Terminal.app, das auf jedem Mac da ist.
    @MainActor
    @discardableResult
    static func fensterOeffnen(_ verzeichnis: String? = nil) -> Bool {
        let ordner = (verzeichnis?.istLeer ?? true) ? arbeitsverzeichnis : verzeichnis!

        for app in ["/Applications/iTerm.app", "/System/Applications/Utilities/Terminal.app"] {
            guard FileManager.default.fileExists(atPath: app) else { continue }
            let einstellung = NSWorkspace.OpenConfiguration()
            einstellung.activates = true
            NSWorkspace.shared.open([URL(fileURLWithPath: ordner)],
                                    withApplicationAt: URL(fileURLWithPath: app),
                                    configuration: einstellung,
                                    completionHandler: nil)
            return true
        }
        return false
    }
}
