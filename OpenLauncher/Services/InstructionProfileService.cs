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
    /// Minimal bleibt bewusst regelfrei (Skills nur per Junction, siehe EnsureSkillsJunction).
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

        // Minimal bleibt bewusst regelfrei: es traegt KEINE versionierten Skills, sondern blendet
        // die echten ~/.claude/skills per Junction ein. Standard und Strikt haben ihre Skills als
        // echte, versionierte Kopien im Repo -> dort wird nichts verlinkt.
        if (string.Equals(profileId, "minimal", StringComparison.Ordinal))
            EnsureSkillsJunction(dir);

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
    /// Blendet die echten ~/.claude/skills als Verzeichnis-Junction in den Minimal-Config-Ordner ein,
    /// damit im sonst isolierten Minimal-Profil ALLE Skills verfuegbar sind -- OHNE die uebrige
    /// ~/.claude-Umgebung (Rules/Hooks/Memory/Agents) hereinzuholen. Junction statt Symlink: braucht
    /// KEINE Admin-Rechte und keinen Developer-Mode. Idempotent: korrekte Junction -> nichts tun;
    /// falsches Ziel -> ersetzen; ein echtes Verzeichnis wird aus Sicherheit nie angefasst. Die
    /// Junction bleibt lokal (die .gitignore des Ordners schliesst skills/ NICHT wieder ein).
    /// </summary>
    private static void EnsureSkillsJunction(string configDir)
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var realSkills = Path.Combine(home, ".claude", "skills");
        // Kein echtes Skills-Verzeichnis -> nichts einzublenden (keinen toten Link anlegen).
        if (!Directory.Exists(realSkills)) return;

        var link = Path.Combine(configDir, "skills");
        var info = new DirectoryInfo(link);
        if (info.Exists)
        {
            if (!info.Attributes.HasFlag(FileAttributes.ReparsePoint))
                return; // echtes Verzeichnis: nicht anfassen (koennte bewusst versionierte Skills sein).

            var current = Path.TrimEndingDirectorySeparator(info.LinkTarget ?? string.Empty);
            if (string.Equals(current, Path.TrimEndingDirectorySeparator(realSkills), StringComparison.OrdinalIgnoreCase))
                return; // Junction zeigt bereits korrekt.

            // Falsches Ziel: nur den Reparse-Point entfernen (folgt der Junction NICHT -> Zielinhalt bleibt).
            try { Directory.Delete(link, recursive: false); }
            catch (Exception ex)
            {
                Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsJunction", $"Alte Skills-Junction nicht entfernbar: {ex.Message}", new { link });
                return;
            }
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
                Logger.Instance.Info("InstructionProfileService", "EnsureSkillsJunction", "Skills-Junction eingerichtet", new { link, target = realSkills });
            else
                Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsJunction", "mklink /J nicht erfolgreich", new { link, target = realSkills, exit = p?.ExitCode });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("InstructionProfileService", "EnsureSkillsJunction", $"Skills-Junction fehlgeschlagen: {ex.Message}", new { link, target = realSkills });
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
        var target = Path.Combine(workDir, "AGENTS.md");
        WriteText(target, ComposeCodexContext(profileId, workModeId));
        return target;
    }

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

    public OpenCodeProfileSession PrepareOpenCodeSession(string profileId, string workDir, bool isLmStudio)
    {
        // Globale ~/.config/opencode/AGENTS.md leer halten: der Profil-Kontext kommt ausschliesslich
        // ueber die Projekt-AGENTS.md (ActivateProjectAgents). So laedt OpenCode (und ein evtl.
        // `instructions`-Verweis in der globalen opencode.jsonc) hier nichts hinzu.
        WriteIfChanged(GetOpenCodeGlobalAgentsPath(), string.Empty);
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
