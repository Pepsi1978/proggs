namespace OpenLauncher.Models;

/// <summary>
/// Ziel-CLI einer Sitzung. Nur fuer OpenAI-Modelle waehlbar: dieselben GPT-Modelle laufen
/// entweder in OpenCode (Standard) oder im eigenstaendigen Codex CLI. Beide lesen ihre Regeln
/// aus derselben Profil-AGENTS.md, damit Profil und Arbeitsmodus in beiden CLIs identisch gelten.
/// </summary>
public sealed class CliTargetEntry
{
    /// <summary>"opencode" oder "codex".</summary>
    public required string Id { get; init; }
    public required string DisplayName { get; init; }
    public required string Description { get; init; }
}
