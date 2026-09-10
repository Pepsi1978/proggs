using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;

namespace OpenLauncher.Services;

/// <summary>
/// Einheitliches Profilmodell: JEDES Profil (Claude wie OpenCode) ist genau EINE bearbeitbare
/// Repo-Datei. Der Launcher schreibt deren Inhalt vor jedem Start in die Datei, die das jeweilige
/// Werkzeug tatsaechlich liest:
///   Claude   -> aktive CLAUDE.md im gemeinsamen Config-Ordner (CLAUDE_CONFIG_DIR).
///   OpenCode -> Projekt-AGENTS.md im Arbeitsverzeichnis (ActivateProjectAgents).
/// Kein Verstecken, keine Snapshots, keine "Global+Projekt"-Zweiteilung mehr.
/// </summary>
public sealed class InstructionProfileService
{
    private static readonly HashSet<string> ProfileIds = new(StringComparer.Ordinal) { "minimal", "standard", "strict" };
    private static readonly HashSet<string> WorkModeIds = new(StringComparer.Ordinal) { "frei", "schnell", "normal", "gruendlich" };

    // ===================== Laden / Speichern (eine Datei je Profil) =====================

    public InstructionProfileDocuments LoadProfile(bool isClaudeCode, string profileId, string workDir)
    {
        var source = isClaudeCode ? EnsureClaudeProfileSource(profileId) : EnsureOpenCodeProfileSource(profileId);
        // Nur EIN Dokument: das Projekt-Dokument bleibt bewusst leer (der Editor zeigt eine Datei).
        return new InstructionProfileDocuments(source, ReadText(source), string.Empty, string.Empty);
    }

    public void SaveProfile(bool isClaudeCode, string profileId, string workDir, string globalText, string projectText)
    {
        var source = isClaudeCode ? ResolveClaudeProfileSourcePath(profileId) : ResolveOpenCodeProfileSourcePath(profileId);
        // Immer schreiben (auch leer) -- so kann der Nutzer den Kontext bewusst leeren.
        WriteText(source, globalText);
    }

    /// <summary>Dateiname, den das Werkzeug tatsaechlich einliest (fuer die Editor-Anzeige).</summary>
    public static string ActiveFileName(bool isClaudeCode) => isClaudeCode ? "CLAUDE.md" : "AGENTS.md";

    // ===================== Claude Code =====================

    /// <summary>
    /// Eigener Claude-Config-Ordner (CLAUDE_CONFIG_DIR) je Profil im Repo
    /// (~/proggs/OpenLauncher/Profiles/ClaudeCode/&lt;id&gt;). Jedes Profil traegt seine eigenen,
    /// versionierten Inhalte (settings.json und -- bei Standard/Strikt -- skills/rules/agents/commands),
    /// sodass sie auf jedem Rechner identisch verfuegbar sind und frei bearbeitet werden koennen. Die
    /// .gitignore jedes Ordners haelt Laufzeit/Secrets (Login-Token, sessions/, cache/) vom Repo fern;
    /// die aktive CLAUDE.md ist bewusst untracked und wird pro Start aus der Profilquelle befuellt.
    /// Minimal bleibt bewusst regelfrei (Skills nur per Junction, siehe EnsureSkillsLink).
    /// </summary>
    public static string ResolveClaudeConfigDir(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCode", profileId);
    }

    /// <summary>Versionierte Profilquelle (Regeltext) je Claude-Profil.</summary>
    public static string ResolveClaudeProfileSourcePath(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenLauncher", "Profiles", "ClaudeCode", "sources", profileId + ".md");
    }

    private static string EnsureClaudeProfileSource(string profileId)
    {
        var path = ResolveClaudeProfileSourcePath(profileId);
        CreateIfMissing(path, DefaultSource("ClaudeCode", profileId));
        return path;
    }

    /// <summary>
    /// Bereitet den Claude-Start vor: setzt die aktive CLAUDE.md im profil-eigenen Config-Ordner auf
    /// den Inhalt der gewaehlten Profilquelle und gibt den Ordner zurueck (als CLAUDE_CONFIG_DIR).
    /// JEDES Profil hat seinen eigenen Repo-Ordner (Profiles/ClaudeCode/&lt;id&gt;) -> der Kontext ist
    /// versioniert und auf jedem Rechner gleich. Standard/Strikt tragen ihre eigenen, frei
    /// bearbeitbaren skills/rules/agents/commands im Repo; Minimal bleibt regelfrei und blendet die
    /// Skills nur per Junction ein. Der Login-Token wird bei Bedarf lokal aus ~/.claude uebernommen.
    /// </summary>
    public string? EnsureClaudeConfigDir(string profileId, string workModeId)
    {
        ValidateProfileId(profileId);
        var dir = ResolveClaudeConfigDir(profileId);
        Directory.CreateDirectory(dir);
        WriteText(Path.Combine(dir, "CLAUDE.md"), ComposeClaudeContext(profileId, workModeId));
        EnsureLoginToken(dir);

        // Skills: Standard IST die Repo-Quelle; Minimal und Strikt verlinken per Junction darauf.
        // Dazu die globalen Skill-Orte (~/.claude/skills, ~/.agents/skills) -- siehe RepoSkillsDir.
        if (profileId is "minimal" or "strict")
            EnsureSkillsLink(Path.Combine(dir, "skills"));
        EnsureGlobalSkillLinks();

        return dir;
    }

    /// <summary>
    /// Inhalt der aktiven CLAUDE.md: erst der Profiltext, dahinter der Prompt des gewaehlten
    /// Arbeitsmodus (Profiles/WorkModes/&lt;id&gt;.md) -- genau so, wie er im Launcher bearbeitet wurde.
    /// Leerer Modus-Prompt (Standard beim Freimodus) haengt nichts an.
    /// </summary>
    private string ComposeClaudeContext(string profileId, string workModeId)
    {
        var profileText = ReadText(EnsureClaudeProfileSource(profileId));
        var modeText = LoadWorkMode(workModeId).Trim();
        if (modeText.Length == 0) return profileText;
        if (profileText.Trim().Length == 0) return modeText + "\n";
        return profileText.TrimEnd('\n') + "\n\n" + modeText + "\n";
    }

    // ===================== Arbeitsmodi (Modus-Prompts) =====================

    /// <summary>
    /// Versionierte, frei bearbeitbare Prompt-Datei je Arbeitsmodus:
    /// Profiles/WorkModes/&lt;id&gt;.md. Ihr Inhalt ist die EINZIGE Quelle des Modus-Prompts --
    /// OpenCode liest dieselbe Datei ueber das work-mode-Plugin (auch beim Umschalten in der TUI),
    /// Claude Code bekommt sie beim Start hinter das Profil in die aktive CLAUDE.md geschrieben.
    /// </summary>
    public static string ResolveWorkModeSourcePath(string workModeId)
    {
        ValidateWorkModeId(workModeId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenLauncher", "Profiles", "WorkModes", workModeId + ".md");
    }

    /// <summary>Prompt des Modus lesen (legt die Datei beim ersten Mal mit dem Standardtext an).</summary>
    public string LoadWorkMode(string workModeId)
    {
        var path = ResolveWorkModeSourcePath(workModeId);
        CreateIfMissing(path, DefaultWorkModePrompt(workModeId));
        return ReadText(path);
    }

    /// <summary>Prompt des Modus speichern (auch leer -- dann ergaenzt der Modus nichts).</summary>
    public void SaveWorkMode(string workModeId, string text) =>
        WriteText(ResolveWorkModeSourcePath(workModeId), text);

    /// <summary>
    /// Startinhalt, falls die Datei fehlt. Wortgleich mit dem eingebauten Notnagel des
    /// OpenCode-Plugins (opencode-setup/plugins/token-cost-sidebar/dist/work-mode.ts) -- beide
    /// Seiten sollen ohne Datei denselben Text ergeben. Der Freimodus bleibt bewusst leer.
    /// </summary>
    private static string DefaultWorkModePrompt(string workModeId) => workModeId switch
    {
        "schnell" => "AKTIVER ARBEITSMODUS: Schnellmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Bearbeite nur die ausdrücklich verlangte Änderung und wähle dafür den kleinsten korrekten Eingriff. Prüfe die direkt betroffenen Aufrufer und führe fokussierte Tests für das geänderte Verhalten aus. Vermeide allgemeine Refactorings, zusätzliche Härtung und themenfremde Verbesserungen. Starte kein zusätzliches Quality Gate, außer der Auftrag oder das aktive AGENTS.md-Profil verlangt es.\n",
        "normal" => "AKTIVER ARBEITSMODUS: Normalmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Löse den Auftrag vollständig mit einem zum Risiko und Umfang passenden Eingriff. Prüfe betroffene Aufrufer, naheliegende Regressionen und relevante Randfälle und führe die passenden Tests oder Builds aus. Kleine, direkt auftragsbezogene Härtungen sind erlaubt; vermeide themenfremde Refactorings. Für durch diesen Modus zusätzlich veranlasste Quality Gates gelten höchstens zwei Durchläufe, sofern der Auftrag oder das aktive AGENTS.md-Profil nicht mehr verlangt.\n",
        "gruendlich" => "AKTIVER ARBEITSMODUS: Gründlichkeitsmodus. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. Untersuche neben der konkreten Änderung auch betroffene Aufrufer, Abhängigkeiten, relevante Randfälle und verwandte Fehlerklassen. Nimm sinnvolle, auftragsnahe Härtungen vor und verifiziere das Ergebnis mit den vollständigen relevanten Tests oder Builds. Wiederhole erforderliche Quality Gates ohne feste Obergrenze, bis alle Befunde behoben und alle Prüfungen grün sind. Melde verbleibende Unsicherheiten ausdrücklich.\n",
        _ => string.Empty,
    };

    private static string ValidateWorkModeId(string workModeId)
    {
        if (!WorkModeIds.Contains(workModeId))
            throw new ArgumentException($"Unbekannter Modus: {workModeId}", nameof(workModeId));
        return workModeId;
    }

    /// <summary>
    /// Uebernimmt den Login-Token (.credentials.json) einmalig lokal aus ~/.claude in den Profil-
    /// Config-Ordner, falls dort noch keiner liegt -- so muss man sich pro Profil/Rechner nicht neu
    /// anmelden. Der Token ist ein Secret: er wird per .gitignore garantiert nie versioniert. Ist im
    /// Ziel bereits ein (evtl. im Profil neu erzeugter) Token vorhanden, bleibt er unangetastet.
    /// </summary>
    private static void EnsureLoginToken(string configDir)
    {
        var target = Path.Combine(configDir, ".credentials.json");
        if (File.Exists(target)) return;

        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var source = Path.Combine(home, ".claude", ".credentials.json");
        if (!File.Exists(source)) return;

        try
        {
            File.Copy(source, target);
            Logger.Instance.Info("InstructionProfileService", "EnsureLoginToken", "Login-Token lokal uebernommen", new { configDir });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "EnsureLoginToken", $"Login-Token nicht uebernommen: {ex.Message}", new { configDir });
        }
    }

    /// <summary>
    /// Einzige Skill-Quelle fuer ALLE Werkzeuge (Claude Code, Codex, OpenCode), Profile, Modi und
    /// Rechner: die versionierten Repo-Skills. Jeder andere Skill-Ort ist nur eine Junction hierauf,
    /// damit eine KI, die einen Skill "an Ort und Stelle" verbessert, immer die Repo-Datei aendert.
    /// </summary>
    public static string RepoSkillsDir => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "proggs", "OpenLauncher", "Profiles", "ClaudeCode", "standard", "skills");

    /// <summary>
    /// Stellt die globalen Skill-Orte auf das Repo um: ~/.claude/skills (Claude Code ohne Profil,
    /// OpenCode) und ~/.agents/skills (Codex und OpenCode scannen ihn immer). Laeuft bei jedem Start
    /// jedes Werkzeugs, damit auch ein frisch eingerichteter Rechner ohne Handgriff umgestellt wird.
    /// </summary>
    public static void EnsureGlobalSkillLinks()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        EnsureSkillsLink(Path.Combine(home, ".claude", "skills"));
        EnsureSkillsLink(Path.Combine(home, ".agents", "skills"));
        // OpenCodes eigener globaler Skill-Ordner: alte Kopien dort wuerden die Repo-Skills verdecken.
        foreach (var name in new[] { "skill", "skills" })
            BackupStaleSkillDir(Path.Combine(home, ".config", "opencode", name));
    }

    /// <summary>
    /// Sichert einen echten, nicht leeren Skill-Ordner als &lt;name&gt;.bak-&lt;Zeitstempel&gt; (nie loeschen),
    /// damit dort liegende Zweitkopien die Repo-Skills nicht mehr verdecken.
    /// </summary>
    private static void BackupStaleSkillDir(string dir)
    {
        try
        {
            var info = new DirectoryInfo(dir);
            if (!info.Exists || info.Attributes.HasFlag(FileAttributes.ReparsePoint) || !info.EnumerateFileSystemInfos().Any()) return;
            var backup = $"{dir}.bak-{DateTime.Now:yyyyMMdd-HHmmss}";
            Directory.Move(dir, backup);
            Logger.Instance.Info("InstructionProfileService", "BackupStaleSkillDir", "Alte Skill-Kopien gesichert", new { dir, backup });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "BackupStaleSkillDir", $"Alte Skill-Kopien nicht gesichert: {ex.Message}", new { dir });
        }
    }

    /// <summary>
    /// Macht <paramref name="link"/> zur Verzeichnis-Junction auf RepoSkillsDir. Junction statt
    /// Symlink: braucht KEINE Admin-Rechte und keinen Developer-Mode. Idempotent: korrekte Junction ->
    /// nichts tun; falsches Ziel -> nur den Reparse-Point ersetzen; ein echtes Verzeichnis (alte
    /// Skill-Kopien) wird NIE geloescht, sondern als &lt;name&gt;.bak-&lt;Zeitstempel&gt; daneben gesichert.
    /// </summary>
    private static void EnsureSkillsLink(string link)
    {
        var realSkills = RepoSkillsDir;
        // Kein Repo-Skills-Verzeichnis -> keinen toten Link anlegen.
        if (!Directory.Exists(realSkills)) return;

        var info = new DirectoryInfo(link);
        try
        {
            if (info.Exists && info.Attributes.HasFlag(FileAttributes.ReparsePoint))
            {
                var current = Path.TrimEndingDirectorySeparator(info.LinkTarget ?? string.Empty);
                if (string.Equals(current, Path.TrimEndingDirectorySeparator(realSkills), StringComparison.OrdinalIgnoreCase))
                    return; // Junction zeigt bereits korrekt.
                // Falsches Ziel: nur den Reparse-Point entfernen (folgt der Junction NICHT -> Zielinhalt bleibt).
                Directory.Delete(link, recursive: false);
            }
            else if (info.Exists)
            {
                var backup = $"{link}.bak-{DateTime.Now:yyyyMMdd-HHmmss}";
                Directory.Move(link, backup);
                Logger.Instance.Info("InstructionProfileService", "EnsureSkillsLink", "Alte Skill-Kopie gesichert", new { link, backup });
            }
            Directory.CreateDirectory(Path.GetDirectoryName(link)!);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsLink", $"Skill-Ordner nicht umstellbar: {ex.Message}", new { link });
            return;
        }

        try
        {
            // mklink /J erzeugt eine Junction ohne Admin/Developer-Mode (Directory.CreateSymbolicLink braucht beides).
            var psi = new ProcessStartInfo("cmd.exe", $"/c mklink /J \"{link}\" \"{realSkills}\"")
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            using var p = Process.Start(psi);
            if (p != null && p.WaitForExit(5000) && p.ExitCode == 0)
                Logger.Instance.Info("InstructionProfileService", "EnsureSkillsLink", "Skills-Junction eingerichtet", new { link, target = realSkills });
            else
                Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsLink", "mklink /J nicht erfolgreich", new { link, target = realSkills, exit = p?.ExitCode });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsLink", $"Skills-Junction fehlgeschlagen: {ex.Message}", new { link, target = realSkills });
        }
    }

    // ===================== OpenCode =====================

    /// <summary>Versionierte Profilquelle (Regeltext) je OpenCode-Profil: Profiles/OpenCode/&lt;id&gt;/AGENTS.md.</summary>
    public static string ResolveOpenCodeProfileSourcePath(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenLauncher", "Profiles", "OpenCode", profileId, "AGENTS.md");
    }

    private static string EnsureOpenCodeProfileSource(string profileId)
    {
        var path = ResolveOpenCodeProfileSourcePath(profileId);
        CreateIfMissing(path, DefaultSource("OpenCode", profileId));
        return path;
    }

    /// <summary>
    /// Setzt die Projekt-AGENTS.md im Arbeitsverzeichnis = Inhalt der Profilquelle, BEVOR OpenCode
    /// startet. OpenCode liest die AGENTS.md im Arbeitsverzeichnis immer ein (kein Abschalt-Flag);
    /// statt sie zu verstecken, kontrollieren wir ihren Inhalt. Deterministisch bei jedem Start,
    /// ohne Umbenennen/Restore -- die Datei existiert immer mit gueltigem Inhalt.
    /// </summary>
    public void ActivateProjectAgents(string profileId, string workDir)
    {
        if (!Directory.Exists(workDir))
            throw new DirectoryNotFoundException($"Arbeitsverzeichnis nicht gefunden: {workDir}");
        WriteText(Path.Combine(workDir, "AGENTS.md"), ReadText(EnsureOpenCodeProfileSource(profileId)));
    }

    // ===================== Codex CLI =====================

    /// <summary>
    /// Bereitet den Codex-CLI-Start vor: schreibt Profiltext + Modus-Prompt in die AGENTS.md des
    /// Arbeitsverzeichnisses. Codex liest diese Datei garantiert ein (Projekt-Dokument im
    /// Arbeitsverzeichnis bzw. Git-Wurzel) -- damit gilt im Codex CLI exakt dasselbe Profil wie in
    /// OpenCode, statt der AGENTS.md, die Codex bei der lokalen Installation selbst anlegen wuerde.
    ///
    /// Anders als bei OpenCode wird der Modus-Prompt hier MIT in die Datei geschrieben: OpenCode holt
    /// ihn ueber sein work-mode-Plugin aus Profiles/WorkModes/&lt;id&gt;.md, Codex kennt kein solches
    /// Plugin. Quelle ist trotzdem dieselbe Datei, damit beide CLIs denselben Text sehen.
    /// </summary>
    public string ActivateCodexProjectAgents(string profileId, string workModeId, string workDir)
    {
        if (!Directory.Exists(workDir))
            throw new DirectoryNotFoundException($"Arbeitsverzeichnis nicht gefunden: {workDir}");
        var text = ComposeCodexContext(profileId, workModeId);
        var target = Path.Combine(workDir, "AGENTS.md");
        WriteText(target, text);
        AlignAncestorCodexAgents(workDir, text);
        return target;
    }

    /// <summary>
    /// Codex liest die AGENTS.md-Kette von der Git-Wurzel bis zum Arbeitsverzeichnis und haengt sie
    /// aneinander. Liegt das Arbeitsverzeichnis in einem Unterordner, wuerde also eine hoeher
    /// liegende AGENTS.md VOR dem gewaehlten Profil gelten -- moeglicherweise mit einem anderen
    /// Profil vom letzten Start. Deshalb werden alle Vorfahren-Dateien, die erkennbar vom Launcher
    /// stammen, auf denselben Text gezogen. Fremde AGENTS.md bleiben unangetastet.
    /// </summary>
    private static void AlignAncestorCodexAgents(string workDir, string text)
    {
        try
        {
            var dir = Directory.GetParent(Path.GetFullPath(workDir));
            while (dir != null)
            {
                var candidate = Path.Combine(dir.FullName, "AGENTS.md");
                if (File.Exists(candidate) && IsLauncherProfileText(ReadText(candidate)))
                    WriteIfChanged(candidate, text);
                if (Directory.Exists(Path.Combine(dir.FullName, ".git"))) break;
                dir = dir.Parent;
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "AlignAncestorCodexAgents", $"Vorfahren-AGENTS.md nicht angeglichen: {ex.Message}", new { workDir });
        }
    }

    /// <summary>Erkennt eine vom Launcher geschriebene Profildatei an ihrer Kopfzeile.</summary>
    private static bool IsLauncherProfileText(string text) =>
        text.TrimStart().StartsWith("# Open-Code-Profil:", StringComparison.Ordinal);

    /// <summary>
    /// Inhalt der Codex-AGENTS.md: erst der Profiltext (dieselbe Quelle wie OpenCode:
    /// Profiles/OpenCode/&lt;id&gt;/AGENTS.md), dahinter der Prompt des gewaehlten Arbeitsmodus.
    /// Leerer Modus-Prompt (Freimodus) haengt nichts an.
    /// </summary>
    public string ComposeCodexContext(string profileId, string workModeId)
    {
        var profileText = ReadText(EnsureOpenCodeProfileSource(profileId));
        var modeText = LoadWorkMode(workModeId).Trim();
        if (modeText.Length == 0) return profileText;
        if (profileText.Trim().Length == 0) return modeText + "\n";
        return profileText.TrimEnd('\n') + "\n\n" + modeText + "\n";
    }

    /// <summary>
    /// Legt das EIGENE Codex-Zuhause des Launchers an und gibt seinen Pfad zurueck. Es wird beim
    /// Start ueber die Umgebungsvariable CODEX_HOME gesetzt, damit Codex NICHT das persoenliche
    /// ~/.codex benutzt. Grund: dort liegen eine globale AGENTS.md mit fremden Regeln, rund 40
    /// Plugins, mehrere MCP-Server, Hooks und eine angepasste Statuszeile -- all das wuerde
    /// zusaetzlich zum Launcher-Profil gelten. Im eigenen Zuhause gilt ausschliesslich die
    /// Profil-AGENTS.md des Arbeitsverzeichnisses.
    ///
    /// Das Zuhause ist bewusst PERSISTENT (nicht pro Sitzung): sonst laeuft bei jedem Start das
    /// Onboarding und der Vertrauensdialog erneut, und die Sitzungshistorie waere jedes Mal weg.
    /// </summary>
    public string PrepareCodexHome(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "OpenLauncher", "codex-home");
        Directory.CreateDirectory(home);

        // Globale Codex-AGENTS.md leer halten -- dieselbe Logik wie bei OpenCode: der Profiltext
        // kommt ausschliesslich ueber die Projekt-AGENTS.md. Codex legt sich hier sonst beim
        // ersten Start selbst eine an.
        WriteIfChanged(Path.Combine(home, "AGENTS.md"), string.Empty);

        // Minimale config.toml: keine Plugins, keine MCP-Server, keine Hooks, kein notify, keine
        // eigene Statuszeile. Nur anlegen wenn sie fehlt -- Codex traegt hier selbst seine
        // [projects.*]-Vertrauensstufen ein, die bei jedem Neuschreiben verloren gingen (dann kaeme
        // der Vertrauensdialog bei jedem Start zurueck).
        CreateIfMissing(Path.Combine(home, "config.toml"), CodexBaseConfig);

        // Anmeldung aus dem persoenlichen ~/.codex uebernehmen, damit kein zweiter Login noetig ist.
        // Nur wenn sie hier fehlt oder die Quelle neuer ist: ein im eigenen Zuhause erneuerter
        // Token darf nicht durch einen aelteren ueberschrieben werden.
        MirrorCodexAuth(home);

        // Skills ausschliesslich aus dem Repo: Codex scannt ~/.agents/skills immer mit, und das ist eine
        // Junction auf die Repo-Skills (live geprueft: Codex folgt ihr und meldet die Repo-Pfade, Edits
        // landen also im Repo). CODEX_HOME/skills bleibt ein echter Ordner, weil Codex dort seine
        // .system-Skills ablegt; frueher gespiegelte Kopien und die alte Sperrliste werden entfernt.
        EnsureGlobalSkillLinks();
        RemoveCodexSkillCopies(home);
        RemoveCodexSkillBlocklist(home);
        return home;
    }

    private const string CodexBaseConfig = """
# Von OpenLauncher angelegt. Bewusst minimal: kein Plugin, kein MCP-Server, kein Hook,
# keine eigene Statuszeile. Die Regeln kommen ausschliesslich aus der Profil-AGENTS.md
# des Arbeitsverzeichnisses. Codex ergaenzt hier selbst nur seine Vertrauensstufen.
""";

    /// <summary>
    /// Entfernt die frueher nach CODEX_HOME/skills gespiegelten Skill-Kopien. Codex liest die
    /// Repo-Skills jetzt ueber die Junction ~/.agents/skills; Kopien hier waeren doppelt, veralteten,
    /// und eine KI wuerde beim Verbessern die Kopie statt der Repo-Datei aendern. Der Codex-eigene
    /// Ordner .system bleibt unangetastet.
    /// </summary>
    private static void RemoveCodexSkillCopies(string home)
    {
        var target = Path.Combine(home, "skills");
        try
        {
            if (!Directory.Exists(target)) return;
            foreach (var entry in Directory.EnumerateFileSystemEntries(target))
            {
                if (string.Equals(Path.GetFileName(entry), ".system", StringComparison.OrdinalIgnoreCase)) continue;
                var attributes = File.GetAttributes(entry);
                if (attributes.HasFlag(FileAttributes.ReparsePoint)) Directory.Delete(entry);
                else if (attributes.HasFlag(FileAttributes.Directory)) Directory.Delete(entry, recursive: true);
                else File.Delete(entry);
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "RemoveCodexSkillCopies", $"Alte Skill-Kopien nicht entfernt: {ex.Message}", new { target });
        }
    }

    /// <summary>
    /// Entfernt die fruehere Sperrliste ([[skills.config]] enabled = false fuer ~/.agents/skills) aus
    /// der config.toml. Sie waere jetzt schaedlich: ~/.agents/skills zeigt auf die Repo-Skills und
    /// Codex loest die Pfade auf -- die Eintraege wuerden die Repo-Skills abschalten. Alle
    /// [[skills.config]]-Tabellen stammen ausschliesslich vom Launcher; alles andere bleibt erhalten.
    /// </summary>
    private static void RemoveCodexSkillBlocklist(string home)
    {
        var configPath = Path.Combine(home, "config.toml");
        try
        {
            var kept = new List<string>();
            var inSkillTable = false;
            foreach (var line in ReadText(configPath).Replace("\r\n", "\n").Split('\n'))
            {
                var trimmed = line.Trim();
                if (trimmed == "[[skills.config]]") { inSkillTable = true; continue; }
                if (inSkillTable && trimmed.StartsWith('[')) inSkillTable = false;
                if (inSkillTable) continue;
                if (trimmed.StartsWith(CodexSkillBlockMarker, StringComparison.Ordinal)) continue;
                kept.Add(line);
            }
            WriteIfChanged(configPath, string.Join("\n", kept).TrimEnd('\n') + "\n");
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "RemoveCodexSkillBlocklist", $"Skill-Sperrliste nicht entfernt: {ex.Message}", new { configPath });
        }
    }

    private const string CodexSkillBlockMarker = "# OpenLauncher-Skills:";

    private static void MirrorCodexAuth(string home)
    {
        try
        {
            var source = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".codex", "auth.json");
            if (!File.Exists(source)) return;
            var target = Path.Combine(home, "auth.json");
            if (File.Exists(target) && File.GetLastWriteTimeUtc(target) >= File.GetLastWriteTimeUtc(source)) return;
            File.Copy(source, target, overwrite: true);
            Logger.Instance.Info("InstructionProfileService", "MirrorCodexAuth", "Codex-Anmeldung uebernommen", new { home });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "MirrorCodexAuth", $"Codex-Anmeldung nicht uebernommen: {ex.Message}", new { home });
        }
    }

    public OpenCodeProfileSession PrepareOpenCodeSession(string profileId, string workDir, bool isLmStudio)
    {
        // Globale ~/.config/opencode/AGENTS.md leer halten: der Profil-Kontext kommt ausschliesslich
        // ueber die Projekt-AGENTS.md (ActivateProjectAgents). So laedt OpenCode (und ein evtl.
        // `instructions`-Verweis in der globalen opencode.jsonc) hier nichts hinzu.
        WriteIfChanged(GetOpenCodeGlobalAgentsPath(), string.Empty);
        // OpenCode liest ~/.claude/skills und ~/.agents/skills -> beide auf die Repo-Skills.
        EnsureGlobalSkillLinks();
        var source = EnsureOpenCodeProfileSource(profileId);

        var sessionRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "OpenLauncher", "sessions", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(sessionRoot);
        var configPath = Path.Combine(sessionRoot, "opencode-profile.json");

        // Chrome-MCPs sind nur fuer lokale LM-Studio-Modelle abgeschaltet. Die explizite
        // Gegenrichtung verhindert, dass ein alter globaler false-Wert Cloud-Sitzungen lahmlegt.
        var chromeEnabled = !isLmStudio;
        var config = new Dictionary<string, object>
        {
            ["$schema"] = "https://opencode.ai/config.json",
            ["mcp"] = new Dictionary<string, object>
            {
                ["chrome-devtools"] = new Dictionary<string, bool> { ["enabled"] = chromeEnabled },
                ["chrome-personal"] = new Dictionary<string, bool> { ["enabled"] = chromeEnabled }
            }
        };
        WriteText(configPath, JsonSerializer.Serialize(config, new JsonSerializerOptions { WriteIndented = true }));
        DeleteOldSessions(Path.GetDirectoryName(sessionRoot)!);

        return new OpenCodeProfileSession(
            profileId,
            source,
            string.Empty,
            Path.Combine(workDir, "AGENTS.md"),
            string.Empty,
            configPath);
    }

    // ===================== Defaults / Helfer =====================

    private static string DefaultSource(string tool, string profileId) => profileId switch
    {
        "minimal" => $"# {tool}-Profil: Minimal\n\nArbeite selbstständig am konkreten Benutzerauftrag. Prüfe Dateien und Projektzustand mit Werkzeugen, statt zu raten. Beschränke Änderungen auf den Auftrag und erhalte bestehende Funktionalität.\n",
        "standard" => $"# {tool}-Profil: Standard\n\nBewährte Arbeits- und Projektregeln. Betroffene Aufrufer und Regressionen prüfen; Änderungen vor dem Commit mit den relevanten Tests/Builds verifizieren.\n",
        "strict" => $"# {tool}-Profil: Strikt\n\nMaximale Absicherung: Annahmen vor jeder Änderung am tatsächlichen Zustand prüfen, jede Änderung mit Tests/Builds verifizieren, verbleibende Unsicherheiten ausdrücklich melden.\n",
        _ => $"# {tool}-Profil: {profileId}\n",
    };

    private static string ValidateProfileId(string profileId)
    {
        if (!ProfileIds.Contains(profileId))
            throw new ArgumentException($"Unbekanntes Profil: {profileId}", nameof(profileId));
        return profileId;
    }

    private static string GetOpenCodeGlobalAgentsPath()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, ".config", "opencode", "AGENTS.md");
    }

    private static void DeleteOldSessions(string sessionsRoot)
    {
        if (!Directory.Exists(sessionsRoot)) return;
        var cutoff = DateTime.UtcNow.AddDays(-14);
        foreach (var directory in Directory.EnumerateDirectories(sessionsRoot))
        {
            try
            {
                if (Directory.GetCreationTimeUtc(directory) < cutoff) Directory.Delete(directory, recursive: true);
            }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }
    }

    private static string ReadText(string path) => File.Exists(path)
        ? File.ReadAllText(path, Encoding.UTF8)
        : string.Empty;

    private static void CreateIfMissing(string path, string text)
    {
        if (!File.Exists(path)) WriteText(path, text);
    }

    private static void WriteIfChanged(string path, string text)
    {
        var normalized = Normalize(text);
        if (File.Exists(path) && string.Equals(Normalize(ReadText(path)), normalized, StringComparison.Ordinal)) return;
        WriteText(path, normalized);
    }

    private static void WriteText(string path, string text)
    {
        var directory = Path.GetDirectoryName(path)!;
        Directory.CreateDirectory(directory);
        var tempPath = $"{path}.{Guid.NewGuid():N}.tmp";
        try
        {
            File.WriteAllText(tempPath, Normalize(text), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
            File.Move(tempPath, path, overwrite: true);
        }
        finally
        {
            if (File.Exists(tempPath)) File.Delete(tempPath);
        }
    }

    private static string Normalize(string text) =>
        text.Replace("\r\n", "\n", StringComparison.Ordinal).Replace('\r', '\n');
}

public sealed record InstructionProfileDocuments(
    string GlobalPath,
    string GlobalText,
    string ProjectPath,
    string ProjectText);

public sealed record OpenCodeProfileSession(
    string ProfileId,
    string SourceGlobalPath,
    string SourceProjectPath,
    string GlobalSnapshotPath,
    string ProjectSnapshotPath,
    string ConfigPath);
