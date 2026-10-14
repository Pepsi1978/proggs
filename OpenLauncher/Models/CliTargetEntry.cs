namespace OpenLauncher.Models;

/// <summary>
/// Ziel-CLI einer Sitzung. Nur fuer OpenAI-, Moonshot-AI- und Google-Modelle waehlbar: die Modelle
/// laufen entweder in OpenCode oder im eigenen CLI des Anbieters (Codex CLI, Kimi Code CLI bzw.
/// Gemini CLI).
/// Alle lesen ihre Regeln
/// aus derselben Profil-AGENTS.md, damit Profil und Arbeitsmodus in beiden CLIs identisch gelten.
/// </summary>
public sealed class CliTargetEntry
{
    /// <summary>"opencode", "codex", "kimi" oder "gemini".</summary>
    public required string Id { get; init; }
    public required string DisplayName { get; init; }
    public required string Description { get; init; }
}
