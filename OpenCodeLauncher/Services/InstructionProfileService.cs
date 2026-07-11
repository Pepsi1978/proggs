using System.IO;
using System.Text;

namespace OpenCodeLauncher.Services;

public sealed class InstructionProfileService
{
    public InstructionProfileDocuments LoadStandard(bool isClaudeCode, string workDir)
    {
        var globalPath = GetGlobalPath(isClaudeCode);
        var projectPath = Path.Combine(workDir, isClaudeCode ? "CLAUDE.md" : "AGENTS.md");
        return new InstructionProfileDocuments(
            globalPath,
            ReadText(globalPath),
            projectPath,
            ReadText(projectPath));
    }

    public void SaveStandard(bool isClaudeCode, string workDir, string globalText, string projectText)
    {
        if (!Directory.Exists(workDir))
            throw new DirectoryNotFoundException($"Arbeitsverzeichnis nicht gefunden: {workDir}");

        var globalPath = GetGlobalPath(isClaudeCode);
        var projectPath = Path.Combine(workDir, isClaudeCode ? "CLAUDE.md" : "AGENTS.md");
        WriteIfNeeded(globalPath, globalText);
        WriteIfNeeded(projectPath, projectText);
    }

    private static string GetGlobalPath(bool isClaudeCode)
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return isClaudeCode
            ? Path.Combine(home, ".claude", "CLAUDE.md")
            : Path.Combine(home, ".config", "opencode", "AGENTS.md");
    }

    private static string ReadText(string path) => File.Exists(path)
        ? File.ReadAllText(path, Encoding.UTF8)
        : string.Empty;

    private static void WriteIfNeeded(string path, string text)
    {
        if (!File.Exists(path) && string.IsNullOrWhiteSpace(text)) return;

        var directory = Path.GetDirectoryName(path)!;
        Directory.CreateDirectory(directory);
        var normalized = text.Replace("\r\n", "\n", StringComparison.Ordinal).Replace('\r', '\n');
        var tempPath = path + ".tmp";
        File.WriteAllText(tempPath, normalized, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        File.Move(tempPath, path, overwrite: true);
    }
}

public sealed record InstructionProfileDocuments(
    string GlobalPath,
    string GlobalText,
    string ProjectPath,
    string ProjectText);
