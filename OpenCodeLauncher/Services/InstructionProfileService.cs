using System.IO;
using System.Text;
using System.Text.Json;

namespace OpenCodeLauncher.Services;

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
    /// Isolierter Claude-Config-Ordner (CLAUDE_CONFIG_DIR) NUR fuer das Minimal-Profil. Liegt im Repo
    /// (~/proggs/OpenCodeLauncher/Profiles/ClaudeCode/minimal) und traegt Login-Token, settings.json
    /// usw. Die dortige .gitignore haelt Laufzeit/Secrets vom Repo fern; die aktive CLAUDE.md ist
    /// bewusst untracked und wird aus der Minimal-Profilquelle befuellt. Standard und Strikt setzen
    /// KEIN CLAUDE_CONFIG_DIR und nutzen direkt das echte ~/.claude (volle Skills + Regeln + Login).
    /// </summary>
    public static string ResolveClaudeConfigDir()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenCodeLauncher", "Profiles", "ClaudeCode", "minimal");
    }

    /// <summary>Versionierte Profilquelle (Regeltext) je Claude-Profil.</summary>
    public static string ResolveClaudeProfileSourcePath(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenCodeLauncher", "Profiles", "ClaudeCode", "sources", profileId + ".md");
    }

    private static string EnsureClaudeProfileSource(string profileId)
    {
        var path = ResolveClaudeProfileSourcePath(profileId);
        CreateIfMissing(path, DefaultSource("ClaudeCode", profileId));
        return path;
    }

    /// <summary>
    /// Bereitet den Claude-Start vor: setzt die aktive CLAUDE.md im gemeinsamen Config-Ordner auf den
    /// Inhalt der gewaehlten Profilquelle und gibt den Ordner zurueck (als CLAUDE_CONFIG_DIR).
    /// </summary>
    public string? EnsureClaudeConfigDir(string profileId)
    {
        ValidateProfileId(profileId);
        // Nur Minimal wird isoliert: eigener, leerer Repo-Config-Ordner via CLAUDE_CONFIG_DIR
        // -> keine Skills/Rules/Hooks/Memory aus ~/.claude, nur die profilspezifische CLAUDE.md.
        // Standard und Strikt laden bewusst das echte ~/.claude (volle Skills + globale Regeln +
        // Login): dafuer KEIN Redirect (null zurueckgeben) und ~/.claude bleibt unangetastet.
        if (!string.Equals(profileId, "minimal", StringComparison.Ordinal))
            return null;

        var dir = ResolveClaudeConfigDir();
        Directory.CreateDirectory(dir);
        WriteText(Path.Combine(dir, "CLAUDE.md"), ReadText(EnsureClaudeProfileSource(profileId)));
        return dir;
    }

    // ===================== OpenCode =====================

    /// <summary>Versionierte Profilquelle (Regeltext) je OpenCode-Profil: Profiles/OpenCode/&lt;id&gt;/AGENTS.md.</summary>
    public static string ResolveOpenCodeProfileSourcePath(string profileId)
    {
        ValidateProfileId(profileId);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, "proggs", "OpenCodeLauncher", "Profiles", "OpenCode", profileId, "AGENTS.md");
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

    public OpenCodeProfileSession PrepareOpenCodeSession(string profileId, string workDir)
    {
        // Globale ~/.config/opencode/AGENTS.md leer halten: der Profil-Kontext kommt ausschliesslich
        // ueber die Projekt-AGENTS.md (ActivateProjectAgents). So laedt OpenCode (und ein evtl.
        // `instructions`-Verweis in der globalen opencode.jsonc) hier nichts hinzu.
        WriteIfChanged(GetOpenCodeGlobalAgentsPath(), string.Empty);
        var source = EnsureOpenCodeProfileSource(profileId);

        var sessionRoot = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "OpenCodeLauncher", "sessions", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(sessionRoot);
        var configPath = Path.Combine(sessionRoot, "opencode-profile.json");

        // Der Regeltext kommt ueber die Projekt-AGENTS.md; die Session-Config braucht KEINE
        // instructions. Sie existiert nur, weil OPENCODE_CONFIG auf eine gueltige Datei zeigen muss.
        var config = new Dictionary<string, object> { ["$schema"] = "https://opencode.ai/config.json" };
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
