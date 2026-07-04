using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using OpenCodeLauncher.Models;
using OpenCodeLauncher.Services;

namespace OpenCodeLauncher.ViewModels;

public sealed partial class MainViewModel : ObservableObject
{
    private readonly ModelRegistry _registry;
    private readonly OpenRouterService _router = new();
    private readonly OpenCodeLauncherService _launcher = new();
    private CancellationTokenSource? _loadCts;

    public MainViewModel()
    {
        _registry = ModelRegistry.Load();
        foreach (var m in _registry.Models) Models.Add(m);
        if (Models.Count > 0) SelectedModel = Models[0];
        WorkDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");

        Version = $"Version 1.0.0 ({DateTime.Now:dd.MM.yyyy}, {DateTime.Now:HH:mm} Uhr)";
    }

    public ObservableCollection<ModelEntry> Models { get; } = new();
    public ObservableCollection<ProviderEntry> Providers { get; } = new();

    [ObservableProperty] private ModelEntry? _selectedModel;
    [ObservableProperty] private ProviderEntry? _selectedProvider;
    [ObservableProperty] private string _workDir = string.Empty;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string _statusText = "Bereit.";
    [ObservableProperty] private string _version = string.Empty;

    partial void OnSelectedModelChanged(ModelEntry? value)
    {
        SelectedProvider = null;
        Providers.Clear();
        if (value != null) _ = LoadProvidersAsync(value);
    }

    private async Task LoadProvidersAsync(ModelEntry model)
    {
        _loadCts?.Cancel();
        _loadCts = new CancellationTokenSource();
        var ct = _loadCts.Token;
        IsLoading = true;
        StatusText = $"Lade Provider für {model.DisplayName} …";
        try
        {
            var (displayName, providers) = await _router.GetProvidersAsync(model.Slug, ct);
            if (!string.IsNullOrWhiteSpace(displayName) && displayName != model.DisplayName)
            {
                // Anzeigename aus API übernehmen, falls Liste ihn noch als Slug zeigt.
                model.DisplayName = displayName.Replace($"{model.Slug.Split('/')[0]}: ", "");
            }
            Providers.Clear();
            foreach (var p in providers) Providers.Add(p);
            SelectedProvider = Providers.FirstOrDefault();
            StatusText = providers.Count == 0
                ? $"Keine Provider für {model.DisplayName} gefunden (Slug korrekt?)."
                : $"{providers.Count} Provider für {model.DisplayName} geladen.";
        }
        catch (OperationCanceledException) { /* ok */ }
        catch (Exception ex)
        {
            StatusText = $"Fehler: {ex.Message}";
            Logger.Instance.Error("MainViewModel", "LoadProvidersAsync", ex.Message);
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private async Task RefreshAsync()
    {
        if (SelectedModel != null) await LoadProvidersAsync(SelectedModel);
    }

    [RelayCommand]
    private void AddModel()
    {
        var slug = SimplePrompt("Neues Modell hinzufügen",
            "OpenRouter-Slug eingeben (z.B. 'z-ai/glm-5.2'):", "");
        if (string.IsNullOrWhiteSpace(slug)) return;
        var display = SimplePrompt("Anzeigename",
            "Anzeigename (frei lassen für Slug):", slug);
        if (_registry.AddModel(slug, display))
        {
            var entry = _registry.Models.Last();
            Models.Add(entry);
            SelectedModel = entry;
        }
        else
        {
            StatusText = $"Modell '{slug}' existiert bereits.";
        }
    }

    [RelayCommand]
    private void RemoveModel()
    {
        if (SelectedModel == null) return;
        var idx = Models.IndexOf(SelectedModel);
        if (idx < 0) return;
        _registry.RemoveAt(idx);
        Models.RemoveAt(idx);
        if (Models.Count > 0)
            SelectedModel = Models[Math.Clamp(idx, 0, Models.Count - 1)];
        else
            SelectedModel = null;
    }

    [RelayCommand]
    private void BrowseWorkDir()
    {
        var dlg = new Microsoft.Win32.OpenFolderDialog
        {
            InitialDirectory = Directory.Exists(WorkDir) ? WorkDir : Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            Title = "Arbeitsverzeichnis für OpenCode wählen"
        };
        if (dlg.ShowDialog() == true) WorkDir = dlg.FolderName;
    }

    [RelayCommand]
    private void Start()
    {
        if (SelectedModel == null || SelectedProvider == null)
        {
            StatusText = "Bitte Modell und Provider wählen.";
            return;
        }
        if (!Directory.Exists(WorkDir))
        {
            StatusText = "Arbeitsverzeichnis existiert nicht.";
            return;
        }
        try
        {
            var modelString = _launcher.ConfigureProvider(SelectedModel.Slug, SelectedProvider, Providers);
            _launcher.Launch(modelString, WorkDir);
            StatusText = $"OpenCode gestartet: {SelectedModel.DisplayName} via {SelectedProvider.ProviderName}";
        }
        catch (Exception ex)
        {
            StatusText = $"Start fehlgeschlagen: {ex.Message}";
            Logger.Instance.Error("MainViewModel", "Start", ex.Message);
        }
    }

    // Drag&Drop-Unterstützung: Reihenfolge ändern.
    public void MoveModel(int from, int to)
    {
        if (from < 0 || from >= Models.Count || to < 0 || to >= Models.Count || from == to) return;
        _registry.Move(from, to);
        var item = Models[from];
        Models.RemoveAt(from);
        Models.Insert(to, item);
        SelectedModel = item;
    }

    // Minimaler Input-Dialog ohne eigene Window-XAML (System.Windows.MessageBox kann kein Text-Eingabefeld).
    private static string SimplePrompt(string title, string label, string defaultValue)
    {
        var w = new Window
        {
            Title = title,
            Width = 460,
            Height = 200,
            WindowStartupLocation = WindowStartupLocation.CenterScreen,
            ResizeMode = ResizeMode.NoResize,
            Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(32, 32, 40)),
        };
        var sp = new System.Windows.Controls.StackPanel { Margin = new Thickness(16) };
        var tb = new System.Windows.Controls.TextBlock { Text = label, Foreground = System.Windows.Media.Brushes.White, Margin = new Thickness(0, 0, 0, 8) };
        var input = new System.Windows.Controls.TextBox { Text = defaultValue, Padding = new Thickness(8) };
        var btn = new System.Windows.Controls.Button { Content = "OK", Padding = new Thickness(20, 6, 20, 6), HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(0, 12, 0, 0) };
        btn.Click += (_, _) => { w.DialogResult = true; w.Close(); };
        sp.Children.Add(tb); sp.Children.Add(input); sp.Children.Add(btn);
        w.Content = sp;
        input.Focus();
        input.SelectAll();
        return w.ShowDialog() == true ? input.Text.Trim() : string.Empty;
    }
}
