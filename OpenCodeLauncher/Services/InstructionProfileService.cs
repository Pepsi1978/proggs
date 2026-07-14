using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace OpenCodeLauncher.Services;

public sealed class InstructionProfileService
{
    private static readonly HashSet<string> OpenCodeProfileIds = new(StringComparer.Ordinal)
    {
        "minimal", "standard", "strict"
    };

    public InstructionProfileDocuments LoadProfile(bool isClaudeCode, string profileId, string workDir)
    {
        if (isClaudeCode)
        {
            return string.Equals(profileId, "minimal", StringComparison.Ordinal)
                ? LoadClaudeMinimal()
                : LoadClaudeStandard(workDir);
        }

        EnsureOpenCodeProfiles(workDir);
        if (string.Equals(profileId, "minimal", StringComparison.Ordinal))
        {
            // Minimal kennt nur EINE bearbeitbare Datei (im Repo, git-versioniert).
            var minimalSource = EnsureOpenCodeMinimalSource();
            return new InstructionProfileDocuments(minimalSource, ReadText(minimalSource), string.Empty, string.Empty);
        }
        var paths = GetOpenCodeProfilePaths(profileId, workDir);
        return new InstructionProfileDocuments(
            paths.GlobalPath,
            ReadText(paths.GlobalPath),
            paths.ProjectPath,
            ReadText(paths.ProjectPath));
    }

    public void SaveProfile(bool isClaudeCode, string profileId, string workDir, string globalText, string projectText)
    {
        if (!Directory.Exists(workDir))
            throw new DirectoryNotFoundException($"Arbeitsverzeichnis nicht gefunden: {workDir}");

        if (isClaudeCode)
        {
            if (string.Equals(profileId, "minimal", StringComparison.Ordinal))
            {
                // Minimal kennt nur EINE Datei (die globale CLAUDE.md im Repo-Config-Ordner).
                // Immer schreiben, auch wenn leer -- so kann der Nutzer den Kontext bewusst leeren.
                var minimal = LoadClaudeMinimal();
                WriteText(minimal.GlobalPath, globalText);
                return;
            }

            if (!string.Equals(profileId, "standard", StringComparison.Ordinal))
                throw new InvalidOperationException("Claude Code unterstützt derzeit nur Minimal und Standard.");

            var documents = LoadClaudeStandard(workDir);
            WriteIfNeeded(documents.GlobalPath, globalText);
            WriteIfNeeded(documents.ProjectPath, projectText);
            return;
        }

        EnsureOpenCodeProfiles(workDir);
        if (string.Equals(profileId, "minimal", StringComparison.Ordinal))
        {
            // Minimal: nur die eine Repo-Datei schreiben; kein projektspezifisches Dokument.
            WriteText(EnsureOpenCodeMinimalSource(), globalText);
            return;
        }
        var paths = GetOpenCodeProfilePaths(profileId, workDir);
        WriteDocumentsAtomically(paths.GlobalPath, globalText, paths.ProjectPath, projectText);
    }

    public OpenCodeProfileSession PrepareOpenCodeSession(string profileId, string workDir)
    {
        EnsureOpenCodeProfiles(workDir);
        var sessionRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "OpenCodeLauncher", "sessions", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(sessionRoot);
        var configPath = Path.Combine(sessionRoot, "opencode-profile.json");
        var jsonOptions = new JsonSerializerOptions { WriteIndented = true };

        if (string.Equals(profileId, "minimal", StringComparison.Ordinal))
        {
            // Minimal laedt AUSSCHLIESSLICH die eine Minimal-Datei. Kein zweites Dokument, keine
            // weiteren Regeln. Die zwangsweise von OpenCode gelesenen AGENTS.md (Projekt + global)
            // werden beim Start ueber das Launch-Script fuer die Sessiondauer ausgeblendet.
            var minimalSource = EnsureOpenCodeMinimalSource();
            var minimalSnapshot = Path.Combine(sessionRoot, "minimal.md");
            WriteText(minimalSnapshot, ReadText(minimalSource));

            var minimalConfig = new Dictionary<string, object>
            {
                ["$schema"] = "https://opencode.ai/config.json",
                ["instructions"] = new[] { minimalSnapshot }
            };
            WriteText(configPath, JsonSerializer.Serialize(minimalConfig, jsonOptions));
            DeleteOldSessions(Path.GetDirectoryName(sessionRoot)!);

            return new OpenCodeProfileSession(
                profileId,
                minimalSource,
                string.Empty,
                minimalSnapshot,
                string.Empty,
                configPath);
        }

        var source = GetOpenCodeProfilePaths(profileId, workDir);
        var globalSnapshot = Path.Combine(sessionRoot, "global.md");
        var projectSnapshot = Path.Combine(sessionRoot, "project.md");
        WriteText(globalSnapshot, ReadText(source.GlobalPath));
        WriteText(projectSnapshot, ReadText(source.ProjectPath));

        var config = new Dictionary<string, object>
        {
            ["$schema"] = "https://opencode.ai/config.json",
            ["instructions"] = new[] { globalSnapshot, projectSnapshot }
        };
        WriteText(configPath, JsonSerializer.Serialize(config, jsonOptions));
        DeleteOldSessions(Path.GetDirectoryName(sessionRoot)!);

        return new OpenCodeProfileSession(
            profileId,
            source.GlobalPath,
            source.ProjectPath,
            globalSnapshot,
            projectSnapshot,
            configPath);
    }

    private void EnsureOpenCodeProfiles(string workDir)
    {
        if (!Directory.Exists(workDir))
            throw new DirectoryNotFoundException($"Arbeitsverzeichnis nicht gefunden: {workDir}");

        var standard = GetOpenCodeProfilePaths("standard", workDir);
        var activeGlobal = GetOpenCodeGlobalAgentsPath();
        var activeProject = Path.Combine(workDir, "AGENTS.md");
        var standardGlobal = AddProfileHeading(ReadSeedText(activeGlobal, "standard", "global.md"), "Standard")
            .Replace(
                "> Wird bei jedem OpenCode-Start aus `~/.config/opencode/AGENTS.md` geladen. Diese kompakte Datei\n> enthält die immer geltenden Kernregeln; die ausführlichen Arbeitsregeln liegen im zweiten Gehirn.",
                "> Wird vom OpenCode Launcher als unveränderlicher Sitzungssnapshot geladen. Die ausführlichen Arbeitsregeln liegen im zweiten Gehirn.",
                StringComparison.Ordinal);
        var standardProject = AddProfileHeading(ReadProjectSeedText(activeProject, workDir), "Standard");

        CreateIfMissing(standard.GlobalPath, standardGlobal);
        CreateIfMissing(standard.ProjectPath, standardProject);

        // Minimal hat nur EINE Quelle (git-versioniert im Repo). Kein AppData-Snapshot,
        // damit nichts Zusaetzliches mitgeladen werden kann.
        EnsureOpenCodeMinimalSource();

        var strict = GetOpenCodeProfilePaths("strict", workDir);
        CreateIfMissing(strict.GlobalPath, standardGlobal
            .Replace("# OpenCode-Profil: Standard", "# OpenCode-Profil: Strikt", StringComparison.Ordinal)
            .TrimEnd() + "\n\n## Strikte zusätzliche Absicherung\n\n- Prüfe Annahmen vor Änderungen anhand des tatsächlichen Zustands.\n- Verifiziere jede Änderung mit den relevanten Tests oder Builds.\n- Melde verbleibende Unsicherheiten ausdrücklich.\n");
        CreateIfMissing(strict.ProjectPath, standardProject
            .Replace("# OpenCode-Profil: Standard", "# OpenCode-Profil: Strikt", StringComparison.Ordinal)
            .TrimEnd() + "\n\n## Strikte Projektprüfung\n\n- Prüfe bei Änderungen die direkt betroffenen Aufrufer und Regressionen.\n- Schließe die Aufgabe erst nach nachvollziehbarer Verifikation ab.\n");

        WriteIfChanged(activeGlobal,
            "# OpenCode Launcher\n\nDie ausführlichen globalen Regeln werden vom OpenCode Launcher als ausgewähltes Profil sitzungsbezogen geladen.\n");
    }

    private static InstructionProfileDocuments LoadClaudeStandard(string workDir)
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var globalPath = Path.Combine(home, ".claude", "CLAUDE.md");
        var projectPath = Path.Combine(workDir, "CLAUDE.md");
        return new InstructionProfileDocuments(
            globalPath,
            ReadText(globalPath),
            projectPath,
            ReadText(projectPath));
    }

    /// <summary>
    /// Config-Ordner des Claude-Minimal-Profils. Liegt bewusst IM Repo neben den OpenCode-Profilen
    /// (~/proggs/OpenCodeLauncher/Profiles/ClaudeCode/minimal), damit die CLAUDE.md (und spaeter selbst
    /// erstellte Hooks/Rules/settings) ueber git auf allen Rechnern identisch sind. Laufzeit-Dateien +
    /// Login-Token werden von der dortigen .gitignore vom Repo ferngehalten; der Build schliesst den
    /// Ordner ueber die .csproj aus.
    /// </summary>
    public static string ResolveClaudeMinimalConfigDir()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenCodeLauncher", "Profiles", "ClaudeCode", "minimal");
    }

    /// <summary>
    /// Einzige Quelldatei des OpenCode-Minimalprofils. Liegt bewusst IM Repo neben den anderen
    /// OpenCode-Profilen (~/proggs/OpenCodeLauncher/Profiles/OpenCode/minimal/minimal.md), damit
    /// sie git-versioniert und auf allen Rechnern identisch ist. Das Minimalprofil laedt
    /// AUSSCHLIESSLICH den Inhalt dieser Datei als Kontext.
    /// </summary>
    public static string ResolveOpenCodeMinimalSourcePath()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenCodeLauncher", "Profiles", "OpenCode", "minimal", "minimal.md");
    }

    private static string EnsureOpenCodeMinimalSource()
    {
        var path = ResolveOpenCodeMinimalSourcePath();
        CreateIfMissing(path,
            "# OpenCode-Profil: Minimal\n\nArbeite selbstständig am konkreten Benutzerauftrag. Prüfe Dateien und Projektzustand mit Werkzeugen, statt zu raten. Beschränke Änderungen auf den Auftrag und erhalte bestehende Funktionalität.\n");
        return path;
    }

    private static InstructionProfileDocuments LoadClaudeMinimal()
    {
        var claudeMd = Path.Combine(ResolveClaudeMinimalConfigDir(), "CLAUDE.md");
        // Minimal hat nur eine globale Datei; kein projektspezifisches Dokument.
        return new InstructionProfileDocuments(claudeMd, ReadText(claudeMd), string.Empty, string.Empty);
    }

    /// <summary>
    /// Stellt den Claude-Config-Ordner fuer das gewaehlte Profil bereit und gibt den Pfad zurueck,
    /// der als CLAUDE_CONFIG_DIR gesetzt werden soll. Fuer "standard" wird null zurueckgegeben
    /// (normales ~/.claude), fuer "minimal" der Repo-Config-Ordner (bei Bedarf inkl. leerer CLAUDE.md).
    /// </summary>
    public string? EnsureClaudeConfigDir(string profileId)
    {
        if (!string.Equals(profileId, "minimal", StringComparison.Ordinal)) return null;

        var dir = ResolveClaudeMinimalConfigDir();
        Directory.CreateDirectory(dir);
        var claudeMd = Path.Combine(dir, "CLAUDE.md");
        if (!File.Exists(claudeMd))
            WriteText(claudeMd, "<!-- Minimal-Profil fuer Claude Code. Trage hier eigene Anweisungen ein. -->\n");
        return dir;
    }

    private static (string GlobalPath, string ProjectPath) GetOpenCodeProfilePaths(string profileId, string workDir)
    {
        ValidateOpenCodeProfileId(profileId);
        var root = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "OpenCodeLauncher", "profiles", "opencode", profileId);
        var projectKey = BuildProjectKey(workDir);
        return (
            Path.Combine(root, "global.md"),
            Path.Combine(root, "projects", projectKey, "project.md"));
    }

    private static string BuildProjectKey(string workDir)
    {
        var normalized = Path.GetFullPath(workDir).TrimEnd(Path.DirectorySeparatorChar).ToUpperInvariant();
        var folder = Path.GetFileName(normalized).ToLowerInvariant();
        var safeFolder = new string(folder.Select(c => char.IsLetterOrDigit(c) || c is '-' or '_' ? c : '-').ToArray());
        var hash = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(normalized)))[..10].ToLowerInvariant();
        return $"{safeFolder}-{hash}";
    }

    private static string ReadSeedText(string activePath, string profileId, string fileName)
    {
        var active = ReadText(activePath);
        if (!string.IsNullOrWhiteSpace(active) && !IsLauncherBootstrap(active))
            return active;

        var assemblyDirectory = Path.GetDirectoryName(typeof(InstructionProfileService).Assembly.Location)!;
        var templatePath = Path.Combine(assemblyDirectory, "Profiles", "OpenCode", profileId, fileName);
        var template = ReadText(templatePath);
        if (string.IsNullOrWhiteSpace(template))
            throw new FileNotFoundException($"Standardprofil-Vorlage nicht gefunden: {templatePath}", templatePath);
        return template;
    }

    private static string ReadProjectSeedText(string activePath, string workDir)
    {
        var active = ReadText(activePath);
        if (!string.IsNullOrWhiteSpace(active) && !IsLauncherBootstrap(active)) return active;

        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var defaultWorkDir = Path.Combine(home, "proggs");
        if (string.Equals(Path.GetFullPath(workDir).TrimEnd(Path.DirectorySeparatorChar),
                Path.GetFullPath(defaultWorkDir).TrimEnd(Path.DirectorySeparatorChar),
                StringComparison.OrdinalIgnoreCase))
            return ReadSeedText(activePath, "standard", "project.md");

        return "# Projektprofil: Standard\n\nBeachte die automatisch geladene Projekt-AGENTS.md und die Anweisungen des Benutzers.\n";
    }

    private static bool IsLauncherBootstrap(string text) =>
        text.Contains("vom OpenCode Launcher als ausgewähltes Profil", StringComparison.Ordinal) ||
        text.Contains("OpenCode Launcher lädt die ausführlichen Regeln", StringComparison.Ordinal);

    private static string AddProfileHeading(string text, string profileName) =>
        text.StartsWith("# OpenCode-Profil:", StringComparison.Ordinal)
            ? text
            : $"# OpenCode-Profil: {profileName}\n\n{text}";

    private static string GetOpenCodeGlobalAgentsPath()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, ".config", "opencode", "AGENTS.md");
    }

    private static void ValidateOpenCodeProfileId(string profileId)
    {
        if (!OpenCodeProfileIds.Contains(profileId))
            throw new ArgumentException($"Unbekanntes OpenCode-Profil: {profileId}", nameof(profileId));
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
        // Bestehenden, abweichenden Inhalt vor dem Überschreiben sichern. Der Launcher verwaltet die
        // globale AGENTS.md als Bootstrap-Stub; hat sie extern anderen Inhalt bekommen, ginge dieser
        // sonst ohne jede Sicherung verloren.
        if (File.Exists(path))
        {
            try { File.Copy(path, path + ".bak", overwrite: true); }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }
        WriteText(path, normalized);
    }

    private static void WriteIfNeeded(string path, string text)
    {
        if (!File.Exists(path) && string.IsNullOrWhiteSpace(text)) return;
        WriteText(path, text);
    }

    private static void WriteDocumentsAtomically(string globalPath, string globalText, string projectPath, string projectText)
    {
        var globalExisted = File.Exists(globalPath);
        var projectExisted = File.Exists(projectPath);
        var previousGlobal = globalExisted ? ReadText(globalPath) : null;
        var previousProject = projectExisted ? ReadText(projectPath) : null;
        var stagedGlobal = StageText(globalPath, globalText);
        string? stagedProject = null;
        var globalReplaced = false;
        var projectReplaced = false;

        try
        {
            stagedProject = StageText(projectPath, projectText);
            File.Move(stagedGlobal, globalPath, overwrite: true);
            globalReplaced = true;
            File.Move(stagedProject, projectPath, overwrite: true);
            projectReplaced = true;
        }
        catch (Exception writeException)
        {
            var rollbackErrors = new List<Exception>();
            if (globalReplaced) TryRestore(globalPath, globalExisted, previousGlobal, rollbackErrors);
            if (projectReplaced) TryRestore(projectPath, projectExisted, previousProject, rollbackErrors);
            if (rollbackErrors.Count > 0)
                throw new AggregateException("Profil konnte nicht vollständig gespeichert oder zurückgesetzt werden.", new[] { writeException }.Concat(rollbackErrors));
            throw;
        }
        finally
        {
            DeleteStagedFile(stagedGlobal);
            if (stagedProject != null) DeleteStagedFile(stagedProject);
        }
    }

    private static string StageText(string path, string text)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var stagedPath = $"{path}.{Guid.NewGuid():N}.tmp";
        File.WriteAllText(stagedPath, Normalize(text), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        return stagedPath;
    }

    private static void TryRestore(string path, bool existed, string? previousText, ICollection<Exception> errors)
    {
        try
        {
            if (existed) WriteText(path, previousText!);
            else if (File.Exists(path)) File.Delete(path);
        }
        catch (Exception rollbackException)
        {
            errors.Add(rollbackException);
        }
    }

    private static void DeleteStagedFile(string path)
    {
        try
        {
            if (File.Exists(path)) File.Delete(path);
        }
        catch (IOException) { }
        catch (UnauthorizedAccessException) { }
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
