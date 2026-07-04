using System.Text.Json.Serialization;

namespace OpenCodeLauncher.Models;

/// <summary>
/// Ein vom Nutzer gepflegtes Modell-Eintrag in der Launcher-Liste.
/// Slug = OpenRouter-ID (z.B. "z-ai/glm-5.2"). Die Reihenfolge wird durch
/// die Liste in der ModelRegistry bestimmt (Drag&Drop).
/// </summary>
public sealed class ModelEntry
{
    public string Slug { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;

    [JsonIgnore]
    public string ModelString => $"openrouter/{Slug}";
}
