import Foundation

/// Startbefehl ohne Fenster für Programmieraufträge von Jarvis (Franks Sprachassistent), Gegenstück zu JarvisAuftrag.cs:
/// `OpenLauncher --jarvis-auftrag <auftrag.txt> [--name "Jarvis: App"] [--profil minimal] [--modus schnell]
/// [--modell claude-opus-5-5[1m]] [--effort high] [--ordner ~/proggs] [--auftrag-id NUMMER]
/// [--cli claude|codex|opencode|kimi|antigravity] [--anbieter openai]`.
/// Mit --auftrag-id bekommt die Sitzung die Jarvis-Hooks (Fortschritt und Rückfragen in den Jarvis-Chat).
/// Ab 1.24.45 jedes Modell aus models.json mit seinen CLIs (Franks Wahl in Jarvis per Sprache): --cli wählt das Werkzeug
/// wie die CLI-Kacheln, --anbieter die Modellgruppe (ProviderId), falls ein Slug mehrfach vorkommt. Codex, Kimi und
/// Antigravity kennen keine Hooks: Die Sitzung meldet über jarvis_hook.py melde; OpenCode über das Plugin jarvis-auftrag.js.
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
            let cli = wert("--cli", "claude").lowercased()
            let anbieter = wert("--anbieter", "")
            let id = auftragId.isEmpty ? nil : auftragId
            if cli == "claude" {
                let configDir = try InstructionProfileService().ensureClaudeConfigDir(profileId: profil, workModeId: modus)
                try OpenLauncherService().launchClaudeCode(modelId: modell, workDir: ordner, effortLevel: effort,
                                                           claudeConfigDir: configDir, useTmux: false,
                                                           remoteControlName: name, initialPromptFile: datei,
                                                           jarvisAuftragId: id)
            } else {
                try starteAndereCli(cli: cli, modell: modell, anbieter: anbieter, profil: profil, modus: modus, effort: effort,
                                    ordner: ordner, datei: datei, auftragId: id, name: name)
            }
            Logger.shared.info("JarvisAuftrag", "verarbeite", "Jarvis-Sitzung gestartet",
                               ["cli": cli, "name": name, "profil": profil, "modus": modus, "modell": modell, "anbieter": anbieter,
                                "effort": effort, "ordner": ordner, "auftragId": auftragId])
            return 0
        } catch {
            Logger.shared.error("JarvisAuftrag", "verarbeite", error.localizedDescription)
            return 1
        }
    }

    /// Wie startCore im MainViewModel für Codex CLI, Kimi Code CLI, Antigravity CLI und OpenCode, nur ohne Fenster und mit
    /// dem Auftrag als erster Nachricht.
    private static func starteAndereCli(cli: String, modell: String, anbieter: String, profil: String, modus: String, effort: String,
                                        ordner: String, datei: String, auftragId: String?, name: String) throws {
        let alle = ModelRegistry.load().groups.flatMap { $0.models }
        guard let model = alle.first(where: { $0.slug == modell && (anbieter.isEmpty || $0.providerId.caseInsensitiveCompare(anbieter) == .orderedSame) })
        else { throw LauncherError.message("Modell \(modell) (\(anbieter)) steht nicht in models.json") }
        let profiles = InstructionProfileService()
        let launcher = OpenLauncherService()
        switch cli {
        case "codex":
            _ = try profiles.activateCodexProjectAgents(profileId: profil, workModeId: modus, workDir: ordner)
            let codexHome = try profiles.prepareCodexHome(profileId: profil)
            try launcher.launchCodexCli(model: model, workDir: ordner, effortLevel: effort, codexHome: codexHome, useTmux: false,
                                        initialPromptFile: datei, jarvisAuftragId: auftragId, title: name)
        case "kimi":
            _ = try profiles.activateCodexProjectAgents(profileId: profil, workModeId: modus, workDir: ordner)
            try launcher.launchKimiCli(model: model, workDir: ordner, effortLevel: effort, useTmux: false,
                                       initialPromptFile: datei, jarvisAuftragId: auftragId, title: name)
        case "antigravity":
            _ = try profiles.activateCodexProjectAgents(profileId: profil, workModeId: modus, workDir: ordner)
            try launcher.launchAntigravityCli(model: model, workDir: ordner, effortLevel: effort, useTmux: false,
                                              initialPromptFile: datei, jarvisAuftragId: auftragId, title: name)
        case "opencode":
            try profiles.activateProjectAgents(profileId: profil, workDir: ordner)
            let sitzung = try profiles.prepareOpenCodeSession(profileId: profil, workDir: ordner,
                                                              isLmStudio: model.providerId.caseInsensitiveCompare(LmStudioService.providerId) == .orderedSame)
            // OpenRouter braucht in der Oberfläche eine Provider-Wahl; ohne sie routet OpenRouter selbst (kein Patch).
            let modelString = model.providerId.caseInsensitiveCompare("openrouter") == .orderedSame ? model.modelString
                : try launcher.configureProvider(model: model, chosen: ProviderEntry(), allProviders: [], thinkingLevel: effort)
            if auftragId != nil { installiereJarvisPlugin() }
            try launcher.launch(modelString: modelString, workDir: ordner, thinkingLevel: effort, profileConfigPath: sitzung.configPath,
                                workMode: modus, useTmux: false, initialPromptFile: datei, jarvisAuftragId: auftragId, title: name)
        default:
            throw LauncherError.message("Unbekanntes CLI: \(cli)")
        }
    }

    /// Das OpenCode-Plugin jarvis-auftrag.js (Rückfragen, Abschluss, Nachträge ans Handy) liegt im Repo; OpenCode lädt Plugins
    /// aus ~/.config/opencode/plugins. Ohne JARVIS_AUFTRAG_ID tut es nichts, darf also immer dort liegen.
    private static func installiereJarvisPlugin() {
        let quelle = (Paths.home as NSString).appendingPathComponent("proggs/opencode-setup/plugins/jarvis-auftrag.js")
        let ziel = (Paths.home as NSString).appendingPathComponent(".config/opencode/plugins/jarvis-auftrag.js")
        guard let neu = FileManager.default.contents(atPath: quelle) else { return }
        if FileManager.default.contents(atPath: ziel) == neu { return }
        Paths.ensureDirectory((ziel as NSString).deletingLastPathComponent)
        FileManager.default.createFile(atPath: ziel, contents: neu)
    }
}
