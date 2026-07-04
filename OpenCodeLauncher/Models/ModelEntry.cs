using System.Text.Json.Serialization;

namespace OpenCodeLauncher.Models;

/// <summary>
/// Ein vom Nutzer gepflegter Modell-Eintrag in einer Launcher-Gruppe.
/// Slug = provider-lokale ID (z.B. "z-ai/glm-5.2" oder "gpt-5.5").
/// </summary>
public sealed class ModelEntry
{
    public string Slug { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string ProviderId { get; set; } = "openrouter";
    public string ProviderName { get; set; } = "OpenRouter";

    [JsonIgnore]
    public string ModelString => Slug.StartsWith($"{ProviderId}/", StringComparison.OrdinalIgnoreCase)
        ? Slug
        : $"{ProviderId}/{Slug}";
}
