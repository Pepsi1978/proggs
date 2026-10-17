import Foundation

/// Startbefehl ohne Fenster für Programmieraufträge von Jarvis (Franks Sprachassistent), Gegenstück zu JarvisAuftrag.cs:
/// `OpenLauncher --jarvis-auftrag <auftrag.txt> [--name "Jarvis: App"] [--profil minimal] [--modus schnell]
/// [--modell claude-opus-5-5[1m]] [--effort high] [--ordner ~/proggs] [--auftrag-id NUMMER]`.
/// Mit --auftrag-id bekommt die Sitzung die Jarvis-Hooks (Fortschritt und Rückfragen in den Jarvis-Chat).
/// Startet genau wie der Startknopf eine Claude-Code-Sitzung in Terminal.app (Profil-Ordner, aktive CLAUDE.md mit Modus,
/// Modell, Effort, Terminal-Farbe), dazu Remote Control mit Namen und den Auftrag als erste Nachricht. Aufrufer ist die
/// Mac-Brücke (Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py). Läuft neben einem offenen OpenLauncher, ohne dessen
/// Fenster zu berühren: main.swift ruft das vor dem Aufbau der App auf.
/// Rückgabe: 0 = gestartet, 1 = Fehler (Einzelheiten im OpenLauncher-Log).
enum JarvisAuftrag {
    static let schalter = "--jarvis-auftrag"

    /// nil = kein Jarvis-Auftrag in den Argumenten, sonst der Exit-Code.
    static func verarbeite(_ args: [String]) -> Int32? {
        guard let stelle = args.firstIndex(of: schalter) else { return nil }
        func wert(_ name: String, _ vorgabe: String) -> String {
            guard let i = args.firstIndex(of: name), i + 1 < args.count,
                  !args[i + 1].trimmingCharacters(in: .whitespaces).isEmpty else { return vorgabe }
            return args[i + 1]
        }
        do {
            let datei = stelle + 1 < args.count ? args[stelle + 1] : ""
            guard FileManager.default.fileExists(atPath: datei) else {
                throw LauncherError.message("Auftragsdatei fehlt: \(datei)")
            }
            let profil = wert("--profil", "minimal")
            let modus = wert("--modus", "schnell")
            let modell = wert("--modell", "claude-opus-5-5[1m]")
            let effort = wert("--effort", "high")
            let name = wert("--name", "Jarvis")
            let ordner = wert("--ordner", (Paths.home as NSString).appendingPathComponent("proggs"))
            let auftragId = wert("--auftrag-id", "")
            if !auftragId.isEmpty, auftragId.range(of: "^[A-Za-z0-9]{1,20}$", options: .regularExpression) == nil {
                throw LauncherError.message("Ungültige Auftragsnummer")
            }
            let configDir = try InstructionProfileService().ensureClaudeConfigDir(profileId: profil, workModeId: modus)
            try OpenLauncherService().launchClaudeCode(modelId: modell, workDir: ordner, effortLevel: effort,
                                                       claudeConfigDir: configDir, useTmux: false,
                                                       remoteControlName: name, initialPromptFile: datei,
                                                       jarvisAuftragId: auftragId.isEmpty ? nil : auftragId)
            Logger.shared.info("JarvisAuftrag", "verarbeite", "Jarvis-Sitzung gestartet",
                               ["name": name, "profil": profil, "modus": modus, "modell": modell, "effort": effort,
                                "ordner": ordner, "auftragId": auftragId])
            return 0
        } catch {
            Logger.shared.error("JarvisAuftrag", "verarbeite", error.localizedDescription)
            return 1
        }
    }
}
