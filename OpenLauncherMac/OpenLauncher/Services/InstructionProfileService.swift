import Foundation

struct InstructionProfileDocuments {
    let globalPath: String
    let globalText: String
    let projectPath: String
    let projectText: String
}

struct OpenCodeProfileSession {
    let profileId: String
    let sourceGlobalPath: String
    let sourceProjectPath: String
    let globalSnapshotPath: String
    let projectSnapshotPath: String
    let configPath: String
}

/// Einheitliches Profilmodell: JEDES Profil (Claude wie OpenCode) ist genau EINE bearbeitbare
/// Repo-Datei. Der Launcher schreibt deren Inhalt vor jedem Start in die Datei, die das jeweilige
/// Werkzeug tatsaechlich liest:
///   Claude   -> aktive CLAUDE.md im gemeinsamen Config-Ordner (CLAUDE_CONFIG_DIR).
///   OpenCode -> Projekt-AGENTS.md im Arbeitsverzeichnis (activateProjectAgents).
/// Kein Verstecken, keine Snapshots, keine "Global+Projekt"-Zweiteilung.
///
/// 1:1-Port von Services/InstructionProfileService.cs. Plattform-Unterschiede:
///   - Profile liegen unter Profiles/ClaudeCodeMac bzw. Profiles/OpenCodeMac (die Windows-Profile
///     tragen PowerShell-Hooks und C:\-Pfade und funktionieren auf macOS nicht).
///   - Die Skills werden im Minimal-Profil per Symlink eingeblendet (statt mklink /J).
///   - Die Arbeitsmodi (Profiles/WorkModes) sind plattformneutral und werden GETEILT.
final class InstructionProfileService {
    private static let profileIds: Set<String> = ["minimal", "standard", "strict"]
    private static let workModeIds: Set<String> = ["frei", "schnell", "normal", "gruendlich"]

    // ===================== Laden / Speichern (eine Datei je Profil) =====================

    func loadProfile(isClaudeCode: Bool, profileId: String, workDir: String) throws -> InstructionProfileDocuments {
        let source = isClaudeCode
            ? try Self.ensureClaudeProfileSource(profileId)
            : try Self.ensureOpenCodeProfileSource(profileId)
        // Nur EIN Dokument: das Projekt-Dokument bleibt bewusst leer (der Editor zeigt eine Datei).
        return InstructionProfileDocuments(globalPath: source, globalText: Paths.readText(source),
                                           projectPath: "", projectText: "")
    }

    func saveProfile(isClaudeCode: Bool, profileId: String, workDir: String, globalText: String, projectText: String) throws {
        let source = isClaudeCode
            ? try Self.resolveClaudeProfileSourcePath(profileId)
            : try Self.resolveOpenCodeProfileSourcePath(profileId)
        // Immer schreiben (auch leer) - so kann der Nutzer den Kontext bewusst leeren.
        Self.writeText(globalText, to: source)
    }

    /// Dateiname, den das Werkzeug tatsaechlich einliest (fuer die Editor-Anzeige).
    static func activeFileName(isClaudeCode: Bool) -> String {
        isClaudeCode ? "CLAUDE.md" : "AGENTS.md"
    }

    // ===================== Claude Code =====================

    /// Eigener Claude-Config-Ordner (CLAUDE_CONFIG_DIR) je Profil im Repo
    /// (~/proggs/OpenLauncher/Profiles/ClaudeCodeMac/<id>). Jedes Profil traegt seine eigenen,
    /// versionierten Inhalte (settings.json und - bei Standard/Strikt - skills/rules/agents/commands),
    /// sodass sie auf jedem Mac identisch verfuegbar sind und frei bearbeitet werden koennen. Die
    /// .gitignore jedes Ordners haelt Laufzeit/Secrets (Login-Token, sessions/, cache/) vom Repo fern;
    /// die aktive CLAUDE.md ist bewusst untracked und wird pro Start aus der Profilquelle befuellt.
    /// Minimal bleibt bewusst regelfrei (Skills nur per Symlink, siehe ensureSkillsSymlink).
    static func resolveClaudeConfigDir(_ profileId: String) throws -> String {
        try validateProfileId(profileId)
        return (Paths.macProfilesRoot as NSString).appendingPathComponent("ClaudeCodeMac/\(profileId)")
    }

    /// Versionierte Profilquelle (Regeltext) je Claude-Profil.
    static func resolveClaudeProfileSourcePath(_ profileId: String) throws -> String {
        try validateProfileId(profileId)
        return (Paths.macProfilesRoot as NSString).appendingPathComponent("ClaudeCodeMac/sources/\(profileId).md")
    }

    private static func ensureClaudeProfileSource(_ profileId: String) throws -> String {
        let path = try resolveClaudeProfileSourcePath(profileId)
        createIfMissing(path, text: defaultSource(tool: "ClaudeCode", profileId: profileId))
        return path
    }

    /// Bereitet den Claude-Start vor: setzt die aktive CLAUDE.md im profil-eigenen Config-Ordner auf
    /// den Inhalt der gewaehlten Profilquelle und gibt den Ordner zurueck (als CLAUDE_CONFIG_DIR).
    /// Der Modus-Prompt haengt hinter dem Profil -> er gilt fuer die ganze Session, genau wie bei
    /// OpenCode. Der Login-Token wird bei Bedarf lokal aus ~/.claude uebernommen.
    func ensureClaudeConfigDir(profileId: String, workModeId: String) throws -> String {
        try Self.validateProfileId(profileId)
        let dir = try Self.resolveClaudeConfigDir(profileId)
        Paths.ensureDirectory(dir)
        Self.writeText(try composeClaudeContext(profileId: profileId, workModeId: workModeId),
                       to: (dir as NSString).appendingPathComponent("CLAUDE.md"))
        Self.ensureLoginToken(configDir: dir)

        // Minimal bleibt bewusst regelfrei: es traegt KEINE versionierten Skills, sondern blendet die
        // echten ~/.claude/skills per Symlink ein. Standard und Strikt haben ihre Skills als echte,
        // versionierte Kopien im Repo -> dort wird nichts verlinkt.
        if profileId == "minimal" { Self.ensureSkillsSymlink(configDir: dir) }

        return dir
    }

    /// Inhalt der aktiven CLAUDE.md: erst der Profiltext, dahinter der Prompt des gewaehlten
    /// Arbeitsmodus (Profiles/WorkModes/<id>.md) - genau so, wie er im Launcher bearbeitet wurde.
    /// Leerer Modus-Prompt (Standard beim Freimodus) haengt nichts an.
    private func composeClaudeContext(profileId: String, workModeId: String) throws -> String {
        let profileText = Paths.readText(try Self.ensureClaudeProfileSource(profileId))
        let modeText = try loadWorkMode(workModeId).trimmingCharacters(in: .whitespacesAndNewlines)
        if modeText.isEmpty { return profileText }
        if profileText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return modeText + "\n" }
        var trimmed = profileText
        while trimmed.hasSuffix("\n") { trimmed.removeLast() }
        return trimmed + "\n\n" + modeText + "\n"
    }

    // ===================== Arbeitsmodi (Modus-Prompts) =====================

    /// Versionierte, frei bearbeitbare Prompt-Datei je Arbeitsmodus: Profiles/WorkModes/<id>.md.
    /// Ihr Inhalt ist die EINZIGE Quelle des Modus-Prompts - OpenCode liest dieselbe Datei ueber das
    /// work-mode-Plugin (auch beim Umschalten in der TUI), Claude Code bekommt sie beim Start hinter
    /// das Profil in die aktive CLAUDE.md geschrieben. Plattformneutral -> GETEILT mit Windows.
    static func resolveWorkModeSourcePath(_ workModeId: String) throws -> String {
        try validateWorkModeId(workModeId)
        return (Paths.macProfilesRoot as NSString).appendingPathComponent("WorkModes/\(workModeId).md")
    }

    /// Prompt des Modus lesen (legt die Datei beim ersten Mal mit dem Standardtext an).
    func loadWorkMode(_ workModeId: String) throws -> String {
        let path = try Self.resolveWorkModeSourcePath(workModeId)
        Self.createIfMissing(path, text: Self.defaultWorkModePrompt(workModeId))
        return Paths.readText(path)
    }

    /// Prompt des Modus speichern (auch leer - dann ergaenzt der Modus nichts).
    func saveWorkMode(_ workModeId: String, text: String) throws {
        Self.writeText(text, to: try Self.resolveWorkModeSourcePath(workModeId))
    }

    /// Startinhalt, falls die Datei fehlt. Wortgleich mit dem eingebauten Notnagel des
    /// OpenCode-Plugins (opencode-setup/plugins/token-cost-sidebar/dist/work-mode.ts) - beide Seiten
    /// sollen ohne Datei denselben Text ergeben. Der Freimodus bleibt bewusst leer.
    private static func defaultWorkModePrompt(_ workModeId: String) -> String {
        switch workModeId {
        case "schnell":
            return "AKTIVER ARBEITSMODUS: Schnellmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Bearbeite nur die ausdrücklich verlangte Änderung und wähle dafür den kleinsten korrekten Eingriff. Prüfe die direkt betroffenen Aufrufer und führe fokussierte Tests für das geänderte Verhalten aus. Vermeide allgemeine Refactorings, zusätzliche Härtung und themenfremde Verbesserungen. Starte kein zusätzliches Quality Gate, außer der Auftrag oder das aktive AGENTS.md-Profil verlangt es.\n"
        case "normal":
            return "AKTIVER ARBEITSMODUS: Normalmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Löse den Auftrag vollständig mit einem zum Risiko und Umfang passenden Eingriff. Prüfe betroffene Aufrufer, naheliegende Regressionen und relevante Randfälle und führe die passenden Tests oder Builds aus. Kleine, direkt auftragsbezogene Härtungen sind erlaubt; vermeide themenfremde Refactorings. Für durch diesen Modus zusätzlich veranlasste Quality Gates gelten höchstens zwei Durchläufe, sofern der Auftrag oder das aktive AGENTS.md-Profil nicht mehr verlangt.\n"
        case "gruendlich":
            return "AKTIVER ARBEITSMODUS: Gründlichkeitsmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Untersuche neben der konkreten Änderung auch betroffene Aufrufer, Abhängigkeiten, relevante Randfälle und verwandte Fehlerklassen. Nimm sinnvolle, auftragsnahe Härtungen vor und verifiziere das Ergebnis mit den vollständigen relevanten Tests oder Builds. Wiederhole erforderliche Quality Gates ohne feste Obergrenze, bis alle Befunde behoben und alle Prüfungen grün sind. Melde verbleibende Unsicherheiten ausdrücklich.\n"
        default:
            return ""
        }
    }

    @discardableResult
    private static func validateWorkModeId(_ workModeId: String) throws -> String {
        guard workModeIds.contains(workModeId) else {
            throw LauncherError.message("Unbekannter Modus: \(workModeId)")
        }
        return workModeId
    }

    /// Uebernimmt den Login-Token (.credentials.json) einmalig lokal aus ~/.claude in den
    /// Profil-Config-Ordner, falls dort noch keiner liegt - so muss man sich pro Profil/Rechner nicht
    /// neu anmelden. Der Token ist ein Secret: er wird per .gitignore garantiert nie versioniert.
    ///
    /// macOS-Besonderheit: Claude Code legt den Token hier bevorzugt im Schluesselbund ab. Fehlt die
    /// Datei, ist das KEIN Fehler - der Schluesselbund traegt die Anmeldung dann ohnehin.
    private static func ensureLoginToken(configDir: String) {
        let target = (configDir as NSString).appendingPathComponent(".credentials.json")
        if Paths.fileExists(target) { return }

        let source = (Paths.claudeHome as NSString).appendingPathComponent(".credentials.json")
        guard Paths.fileExists(source) else { return }

        do {
            try FileManager.default.copyItem(atPath: source, toPath: target)
            Logger.shared.info("InstructionProfileService", "ensureLoginToken", "Login-Token lokal uebernommen",
                               ["configDir": configDir])
        } catch {
            Logger.shared.warn("InstructionProfileService", "ensureLoginToken",
                               "Login-Token nicht uebernommen: \(error.localizedDescription)", ["configDir": configDir])
        }
    }

    /// Blendet die echten ~/.claude/skills als Symlink in den Minimal-Config-Ordner ein, damit im
    /// sonst isolierten Minimal-Profil ALLE Skills verfuegbar sind - OHNE die uebrige
    /// ~/.claude-Umgebung (Rules/Hooks/Memory/Agents) hereinzuholen. Auf macOS genuegt ein
    /// gewoehnlicher Symlink (das Windows-Gegenstueck braucht mklink /J, weil Symlinks dort
    /// Admin-Rechte verlangen). Idempotent: korrekter Symlink -> nichts tun; falsches Ziel ->
    /// ersetzen; ein echtes Verzeichnis wird aus Sicherheit nie angefasst.
    private static func ensureSkillsSymlink(configDir: String) {
        let realSkills = (Paths.claudeHome as NSString).appendingPathComponent("skills")
        // Kein echtes Skills-Verzeichnis -> nichts einzublenden (keinen toten Link anlegen).
        guard Paths.directoryExists(realSkills) else { return }

        let link = (configDir as NSString).appendingPathComponent("skills")
        let fm = FileManager.default

        if let destination = try? fm.destinationOfSymbolicLink(atPath: link) {
            let resolved = destination.hasPrefix("/") ? destination
                : ((configDir as NSString).appendingPathComponent(destination) as NSString).standardizingPath
            if (resolved as NSString).standardizingPath == (realSkills as NSString).standardizingPath { return }
            // Falsches Ziel: nur den Link entfernen (folgt ihm NICHT -> Zielinhalt bleibt).
            do {
                try fm.removeItem(atPath: link)
            } catch {
                Logger.shared.warn("InstructionProfileService", "ensureSkillsSymlink",
                                   "Alter Skills-Symlink nicht entfernbar: \(error.localizedDescription)", ["link": link])
                return
            }
        } else if fm.fileExists(atPath: link) {
            // Echtes Verzeichnis: nicht anfassen (koennte bewusst versionierte Skills sein).
            return
        }

        do {
            try fm.createSymbolicLink(atPath: link, withDestinationPath: realSkills)
            Logger.shared.info("InstructionProfileService", "ensureSkillsSymlink", "Skills-Symlink eingerichtet",
                               ["link": link, "target": realSkills])
        } catch {
            Logger.shared.warn("InstructionProfileService", "ensureSkillsSymlink",
                               "Skills-Symlink fehlgeschlagen: \(error.localizedDescription)",
                               ["link": link, "target": realSkills])
        }
    }

    // ===================== OpenCode =====================

    /// Versionierte Profilquelle (Regeltext) je OpenCode-Profil: Profiles/OpenCodeMac/<id>/AGENTS.md.
    static func resolveOpenCodeProfileSourcePath(_ profileId: String) throws -> String {
        try validateProfileId(profileId)
        return (Paths.macProfilesRoot as NSString).appendingPathComponent("OpenCodeMac/\(profileId)/AGENTS.md")
    }

    private static func ensureOpenCodeProfileSource(_ profileId: String) throws -> String {
        let path = try resolveOpenCodeProfileSourcePath(profileId)
        createIfMissing(path, text: defaultSource(tool: "OpenCode", profileId: profileId))
        return path
    }

    /// Setzt die Projekt-AGENTS.md im Arbeitsverzeichnis = Inhalt der Profilquelle, BEVOR OpenCode
    /// startet. OpenCode liest die AGENTS.md im Arbeitsverzeichnis immer ein (kein Abschalt-Flag);
    /// statt sie zu verstecken, kontrollieren wir ihren Inhalt. Deterministisch bei jedem Start,
    /// ohne Umbenennen/Restore - die Datei existiert immer mit gueltigem Inhalt.
    func activateProjectAgents(profileId: String, workDir: String) throws {
        guard Paths.directoryExists(workDir) else {
            throw LauncherError.message("Arbeitsverzeichnis nicht gefunden: \(workDir)")
        }
        let source = try Self.ensureOpenCodeProfileSource(profileId)
        Self.writeText(Paths.readText(source), to: (workDir as NSString).appendingPathComponent("AGENTS.md"))
    }

    func prepareOpenCodeSession(profileId: String, workDir: String) throws -> OpenCodeProfileSession {
        // Globale ~/.config/opencode/AGENTS.md leer halten: der Profil-Kontext kommt ausschliesslich
        // ueber die Projekt-AGENTS.md (activateProjectAgents). So laedt OpenCode (und ein evtl.
        // `instructions`-Verweis in der globalen opencode.jsonc) hier nichts hinzu.
        Self.writeIfChanged("", to: Self.openCodeGlobalAgentsPath)
        let source = try Self.ensureOpenCodeProfileSource(profileId)

        let sessionRoot = (Paths.sessionsRoot as NSString)
            .appendingPathComponent(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased())
        Paths.ensureDirectory(sessionRoot)
        let configPath = (sessionRoot as NSString).appendingPathComponent("opencode-profile.json")

        // Der Regeltext kommt ueber die Projekt-AGENTS.md; die Session-Config braucht KEINE
        // instructions. Sie existiert nur, weil OPENCODE_CONFIG auf eine gueltige Datei zeigen muss.
        Self.writeText("{\n  \"$schema\": \"https://opencode.ai/config.json\"\n}", to: configPath)
        Self.deleteOldSessions(Paths.sessionsRoot)

        return OpenCodeProfileSession(profileId: profileId,
                                      sourceGlobalPath: source,
                                      sourceProjectPath: "",
                                      globalSnapshotPath: (workDir as NSString).appendingPathComponent("AGENTS.md"),
                                      projectSnapshotPath: "",
                                      configPath: configPath)
    }

    // ===================== Defaults / Helfer =====================

    private static func defaultSource(tool: String, profileId: String) -> String {
        switch profileId {
        case "minimal":
            return "# \(tool)-Profil: Minimal\n\nArbeite selbstständig am konkreten Benutzerauftrag. Prüfe Dateien und Projektzustand mit Werkzeugen, statt zu raten. Beschränke Änderungen auf den Auftrag und erhalte bestehende Funktionalität.\n"
        case "standard":
            return "# \(tool)-Profil: Standard\n\nBewährte Arbeits- und Projektregeln. Betroffene Aufrufer und Regressionen prüfen; Änderungen vor dem Commit mit den relevanten Tests/Builds verifizieren.\n"
        case "strict":
            return "# \(tool)-Profil: Strikt\n\nMaximale Absicherung: Annahmen vor jeder Änderung am tatsächlichen Zustand prüfen, jede Änderung mit Tests/Builds verifizieren, verbleibende Unsicherheiten ausdrücklich melden.\n"
        default:
            return "# \(tool)-Profil: \(profileId)\n"
        }
    }

    @discardableResult
    private static func validateProfileId(_ profileId: String) throws -> String {
        guard profileIds.contains(profileId) else {
            throw LauncherError.message("Unbekanntes Profil: \(profileId)")
        }
        return profileId
    }

    private static var openCodeGlobalAgentsPath: String {
        (Paths.openCodeConfigDir as NSString).appendingPathComponent("AGENTS.md")
    }

    private static func deleteOldSessions(_ sessionsRoot: String) {
        guard Paths.directoryExists(sessionsRoot) else { return }
        let cutoff = Date().addingTimeInterval(-14 * 24 * 60 * 60)
        let fm = FileManager.default
        guard let entries = try? fm.contentsOfDirectory(atPath: sessionsRoot) else { return }
        for entry in entries {
            let path = (sessionsRoot as NSString).appendingPathComponent(entry)
            guard Paths.directoryExists(path),
                  let attributes = try? fm.attributesOfItem(atPath: path),
                  let created = attributes[.creationDate] as? Date, created < cutoff else { continue }
            try? fm.removeItem(atPath: path)
        }
    }

    private static func createIfMissing(_ path: String, text: String) {
        if !Paths.fileExists(path) { writeText(text, to: path) }
    }

    private static func writeIfChanged(_ text: String, to path: String) {
        let normalized = normalize(text)
        if Paths.fileExists(path), normalize(Paths.readText(path)) == normalized { return }
        writeText(normalized, to: path)
    }

    private static func writeText(_ text: String, to path: String) {
        Paths.writeAtomic(normalize(text), to: path)
    }

    private static func normalize(_ text: String) -> String {
        text.replacingOccurrences(of: "\r\n", with: "\n").replacingOccurrences(of: "\r", with: "\n")
    }
}

/// Einfacher Fehlertyp mit sprechender deutscher Meldung (Ersatz fuer die C#-Exceptions).
enum LauncherError: LocalizedError {
    case message(String)

    var errorDescription: String? {
        switch self {
        case .message(let text): return text
        }
    }
}
