namespace OpenLauncher.Models;

/// <summary>
/// Ziel-CLI einer Sitzung. Nur fuer OpenAI- und Moonshot-AI-Modelle waehlbar: die Modelle laufen
/// entweder in OpenCode (Standard) oder im eigenen CLI des Anbieters (Codex CLI bzw. Kimi Code CLI).
/// Alle lesen ihre Regeln
/// aus derselben Profil-AGENTS.md, damit Profil und Arbeitsmodus in beiden CLIs identisch gelten.
/// </summary>
public sealed class CliTargetEntry
{
    /// <summary>"opencode", "codex" oder "kimi".</summary>
    public required string Id { get; init; }
    public required string DisplayName { get; init; }
    public required string Description { get; init; }
}
