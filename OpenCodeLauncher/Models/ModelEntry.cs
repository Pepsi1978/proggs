using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;

namespace OpenCodeLauncher.Models;

/// <summary>
/// Ein vom Nutzer gepflegter Modell-Eintrag in einer Launcher-Gruppe.
/// Slug = provider-lokale ID (z.B. "z-ai/glm-5.2" oder "gpt-5.5").
/// </summary>
public sealed partial class ModelEntry : ObservableObject
{
    // INotifyPropertyChanged: der Slug ist per "Bearbeiten" aenderbar; ohne Change-Notification
    // (inkl. ModelString) bliebe die Modell-Liste auf der alten ID stehen.
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(ModelString))]
    private string _slug = string.Empty;

    // INotifyPropertyChanged: DisplayName wird zur Laufzeit aus dem API-Namen aktualisiert
    // (LoadProvidersAsync). Ohne Change-Notification blieben die gebundene Modell-Liste und der
    // Provider-Header auf dem alten Namen stehen, bis die Liste neu aufgebaut wird.
    [ObservableProperty]
    private string _displayName = string.Empty;

    /// <summary>
    /// Anzeigename wurde von Hand gesetzt (Dialog "Modell bearbeiten"). Dann darf ihn weder der
    /// OpenRouter-API-Name noch der OpenRouterFree-Sync noch die Default-Liste ueberschreiben.
    /// </summary>
    public bool HasCustomDisplayName { get; set; }

    /// <summary>
    /// Eintrag stammt vom Nutzer (hinzugefuegt oder bearbeitet) und darf vom OpenRouterFree-Sync
    /// nicht entfernt werden, nur weil der Slug in der Remote-Liste fehlt.
    /// </summary>
    public bool IsUserDefined { get; set; }

    public string ProviderId { get; set; } = "openrouter";
    public string ProviderName { get; set; } = "OpenRouter";

    [JsonIgnore]
    public string ModelString => Slug.StartsWith($"{ProviderId}/", StringComparison.OrdinalIgnoreCase)
        ? Slug
        : $"{ProviderId}/{Slug}";
}
