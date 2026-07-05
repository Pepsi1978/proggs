using System.Collections.ObjectModel;
using System.Text.Json.Serialization;
using CommunityToolkit.Mvvm.ComponentModel;

namespace OpenCodeLauncher.Models;

public sealed partial class ModelGroupEntry : ObservableObject
{
    public string Id { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string ProviderId { get; set; } = string.Empty;
    public string ProviderName { get; set; } = string.Empty;
    public ObservableCollection<ModelEntry> Models { get; set; } = new();

    [ObservableProperty]
    private bool _isExpanded = true;

    [JsonIgnore]
    public string HeaderText => $"{Title} ({Models.Count})";

    public void RefreshHeaderText() => OnPropertyChanged(nameof(HeaderText));
}
