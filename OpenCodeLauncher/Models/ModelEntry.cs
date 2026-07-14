using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;

namespace OpenCodeLauncher.Models;

/// <summary>
/// Ein vom Nutzer gepflegter Modell-Eintrag in einer Launcher-Gruppe.
/// Slug = provider-lokale ID (z.B. "z-ai/glm-5.2" oder "gpt-5.5").
/// </summary>
public sealed partial class ModelEntry : ObservableObject
{
    public string Slug { get; set; } = string.Empty;

    // INotifyPropertyChanged: DisplayName wird zur Laufzeit aus dem API-Namen aktualisiert
    // (LoadProvidersAsync). Ohne Change-Notification blieben die gebundene Modell-Liste und der
    // Provider-Header auf dem alten Namen stehen, bis die Liste neu aufgebaut wird.
    [ObservableProperty]
    private string _displayName = string.Empty;

    public string ProviderId { get; set; } = "openrouter";
    public string ProviderName { get; set; } = "OpenRouter";

    [JsonIgnore]
    public string ModelString => Slug.StartsWith($"{ProviderId}/", StringComparison.OrdinalIgnoreCase)
        ? Slug
        : $"{ProviderId}/{Slug}";
}
