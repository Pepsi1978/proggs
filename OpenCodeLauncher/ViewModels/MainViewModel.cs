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
        foreach (var g in _registry.Groups) ModelGroups.Add(g);
        SelectedModel = ModelGroups.FirstOrDefault(g => g.Models.Count > 0)?.Models.FirstOrDefault();
        _ = RefreshOpenRouterFreeModelsAsync();
        WorkDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");

        var version = Assembly.GetExecutingAssembly().GetName().Version?.ToString(3) ?? "1.5.7";
        Version = $"Version {version} (05.07.2026, 15:14 Uhr)";
    }

    public ObservableCollection<ModelGroupEntry> ModelGroups { get; } = new();
    public ObservableCollection<ProviderEntry> Providers { get; } = new();

    [ObservableProperty] private ModelEntry? _selectedModel;
    [ObservableProperty] private ProviderEntry? _selectedProvider;
    [ObservableProperty] private string _workDir = string.Empty;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string _statusText = "Bereit.";
    [ObservableProperty] private string _version = string.Empty;
    [ObservableProperty] private string _lastErrorDetails = "Noch kein Fehler protokolliert.";
    [ObservableProperty] private string _lastErrorPath = string.Empty;
    [ObservableProperty] private bool _hasLastError;

    partial void OnSelectedModelChanged(ModelEntry? value)
    {
        SelectedProvider = null;
        Providers.Clear();
        if (value != null) _ = LoadProvidersAsync(value);
    }

    private async Task RefreshOpenRouterFreeModelsAsync()
    {
        try
        {
            var freeModels = await _router.GetFreeModelsAsync();
            if (freeModels.Count == 0) return;

            _registry.SyncOpenRouterFreeModels(freeModels);
            var group = ModelGroups.FirstOrDefault(g => string.Equals(g.Id, "openrouter-free", StringComparison.OrdinalIgnoreCase));
            if (group != null)
            {
                group.RefreshHeaderText();
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("MainViewModel", "RefreshOpenRouterFreeModelsAsync", $"OpenRouterFree bleibt bei Fallback-Liste: {ex.Message}");
        }
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
            if (!string.Equals(model.ProviderId, "openrouter", StringComparison.OrdinalIgnoreCase))
            {
                Providers.Clear();
                var direct = new ProviderEntry
                {
                    ProviderName = model.ProviderName,
                    ProviderSlug = model.ProviderId,
                    Tag = model.ProviderId,
                    Status = 0
                };
                Providers.Add(direct);
                SelectedProvider = direct;
                StatusText = $"{model.DisplayName} über {model.ProviderName} bereit.";
                return;
            }

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
            var details = BuildErrorDetails("Provider laden", ex, model, null, null);
            LastErrorPath = Logger.Instance.WriteErrorReport("provider_load", details);
            LastErrorDetails = details + $"{Environment.NewLine}{Environment.NewLine}Gespeichert unter: {LastErrorPath}";
            HasLastError = true;
            Logger.Instance.Error("MainViewModel", "LoadProvidersAsync", ex, new { model.Slug, LastErrorPath });
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
        var targetGroup = FindGroupForModel(SelectedModel) ?? ModelGroups.FirstOrDefault(g => g.Id == "openrouter") ?? ModelGroups.FirstOrDefault();
        if (targetGroup == null) return;
        var slug = SimplePrompt("Neues Modell hinzufügen",
            $"Modell-ID für '{targetGroup.Title}' eingeben (ohne Provider-Präfix):", "");
        if (string.IsNullOrWhiteSpace(slug)) return;
        var display = SimplePrompt("Anzeigename",
            "Anzeigename (frei lassen für Slug):", slug);
        if (_registry.AddModel(targetGroup, slug, display))
        {
            var entry = targetGroup.Models.Last();
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
        var group = FindGroupForModel(SelectedModel);
        if (group == null) return;
        var idx = group.Models.IndexOf(SelectedModel);
        if (idx < 0) return;
        if (!ConfirmRemoveModel(SelectedModel.DisplayName, SelectedModel.Slug)) return;
        _registry.RemoveAt(group, idx);
        if (group.Models.Count > 0)
            SelectedModel = group.Models[Math.Clamp(idx, 0, group.Models.Count - 1)];
        else
            SelectedModel = ModelGroups.SelectMany(g => g.Models).FirstOrDefault();
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
            var modelString = _launcher.ConfigureProvider(SelectedModel, SelectedProvider, Providers);
            _launcher.Launch(modelString, WorkDir);
            StatusText = $"OpenCode gestartet: {SelectedModel.DisplayName} via {SelectedProvider.ProviderName}";
        }
        catch (Exception ex)
        {
            var details = BuildErrorDetails("OpenCode starten", ex, SelectedModel, SelectedProvider, WorkDir);
            LastErrorPath = Logger.Instance.WriteErrorReport("start", details);
            LastErrorDetails = details + $"{Environment.NewLine}{Environment.NewLine}Gespeichert unter: {LastErrorPath}";
            HasLastError = true;
            StatusText = $"Start fehlgeschlagen. Details gespeichert: {Path.GetFileName(LastErrorPath)}";
            Logger.Instance.Error("MainViewModel", "Start", ex, new
            {
                model = SelectedModel.ModelString,
                provider = SelectedProvider.ProviderName,
                providerSlug = SelectedProvider.ProviderSlug,
                WorkDir,
                LastErrorPath
            });
        }
    }

    [RelayCommand]
    private void ShowLastError()
    {
        var w = new Window
        {
            Title = "OpenCode Launcher - Fehlerdetails",
            Width = 980,
            Height = 720,
            WindowStartupLocation = WindowStartupLocation.CenterScreen,
            Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(32, 32, 40)),
        };
        var grid = new System.Windows.Controls.Grid { Margin = new Thickness(14) };
        grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = GridLength.Auto });
        var text = new System.Windows.Controls.TextBox
        {
            Text = LastErrorDetails,
            IsReadOnly = true,
            AcceptsReturn = true,
            AcceptsTab = true,
            TextWrapping = TextWrapping.NoWrap,
            VerticalScrollBarVisibility = System.Windows.Controls.ScrollBarVisibility.Auto,
            HorizontalScrollBarVisibility = System.Windows.Controls.ScrollBarVisibility.Auto,
            Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(24, 24, 30)),
            Foreground = System.Windows.Media.Brushes.White,
            BorderBrush = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(72, 72, 86)),
            Padding = new Thickness(10)
        };
        var close = new System.Windows.Controls.Button
        {
            Content = "Schließen",
            Padding = new Thickness(20, 7, 20, 7),
            HorizontalAlignment = HorizontalAlignment.Right,
            Margin = new Thickness(0, 12, 0, 0)
        };
        close.Click += (_, _) => w.Close();
        System.Windows.Controls.Grid.SetRow(text, 0);
        System.Windows.Controls.Grid.SetRow(close, 1);
        grid.Children.Add(text);
        grid.Children.Add(close);
        w.Content = grid;
        w.ShowDialog();
    }

    [RelayCommand]
    private void OpenLogFolder()
    {
        var folder = string.IsNullOrWhiteSpace(LastErrorPath)
            ? Path.GetDirectoryName(Logger.Instance.LogPath)
            : Path.GetDirectoryName(LastErrorPath);
        if (!string.IsNullOrWhiteSpace(folder) && Directory.Exists(folder))
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(folder) { UseShellExecute = true });
    }

    private static string BuildErrorDetails(string action, Exception ex, ModelEntry? model, ProviderEntry? provider, string? workDir)
    {
        var sb = new System.Text.StringBuilder();
        sb.AppendLine($"Aktion: {action}");
        sb.AppendLine($"Zeit: {DateTime.Now:yyyy-MM-dd HH:mm:ss.fff}");
        sb.AppendLine($"App-Version: {Assembly.GetExecutingAssembly().GetName().Version?.ToString(3)}");
        sb.AppendLine($"Arbeitsverzeichnis: {workDir ?? "(nicht gesetzt)"}");
        sb.AppendLine($"Modell: {model?.DisplayName ?? "(nicht gesetzt)"} [{model?.Slug ?? "-"}]");
        sb.AppendLine($"Provider: {provider?.ProviderName ?? "(nicht gesetzt)"} [{provider?.ProviderSlug ?? "-"} / {provider?.Tag ?? "-"}]");
        sb.AppendLine($"Logdatei: {Logger.Instance.LogPath}");
        sb.AppendLine();
        AppendException(sb, ex, 0);
        return sb.ToString();
    }

    private static void AppendException(System.Text.StringBuilder sb, Exception ex, int depth)
    {
        var prefix = depth == 0 ? "Exception" : $"Inner Exception {depth}";
        sb.AppendLine($"{prefix}: {ex.GetType().FullName}");
        sb.AppendLine($"Message: {ex.Message}");
        sb.AppendLine("StackTrace:");
        sb.AppendLine(ex.StackTrace ?? "(keine StackTrace verfügbar)");
        sb.AppendLine();
        if (ex.InnerException != null) AppendException(sb, ex.InnerException, depth + 1);
    }

    // Drag&Drop-Unterstützung: Reihenfolge ändern.
    public void MoveModel(ModelGroupEntry group, int from, int to)
    {
        if (from < 0 || from >= group.Models.Count) return;
        var item = group.Models[from];
        _registry.MoveModel(group, from, to);
        SelectedModel = item;
    }

    public void MoveGroup(int from, int to)
    {
        if (from < 0 || from >= ModelGroups.Count || to < 0 || to >= ModelGroups.Count || from == to) return;
        var item = ModelGroups[from];
        _registry.MoveGroup(from, to);
        ModelGroups.RemoveAt(from);
        ModelGroups.Insert(to, item);
    }

    [RelayCommand]
    private void ToggleGroup(ModelGroupEntry group)
    {
        group.IsExpanded = !group.IsExpanded;
        _registry.Save();
    }

    private ModelGroupEntry? FindGroupForModel(ModelEntry? model)
    {
        if (model == null) return null;
        return ModelGroups.FirstOrDefault(g => g.Models.Contains(model));
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

    private static bool ConfirmRemoveModel(string displayName, string slug)
    {
        var w = new Window
        {
            Title = "Modell entfernen",
            Width = 520,
            Height = 230,
            WindowStartupLocation = WindowStartupLocation.CenterScreen,
            ResizeMode = ResizeMode.NoResize,
            WindowStyle = WindowStyle.None,
            AllowsTransparency = true,
            Background = System.Windows.Media.Brushes.Transparent,
        };

        var root = new System.Windows.Controls.Border
        {
            Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(36, 36, 52)),
            BorderBrush = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(58, 58, 82)),
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(14),
            Padding = new Thickness(22)
        };
        var sp = new System.Windows.Controls.StackPanel();
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "Modell entfernen?",
            Foreground = System.Windows.Media.Brushes.White,
            FontSize = 18,
            FontWeight = FontWeights.SemiBold,
            Margin = new Thickness(0, 0, 0, 12)
        });
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = $"{displayName}\n{slug}",
            Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(184, 184, 196)),
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(0, 0, 0, 18)
        });
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "Das Modell wird nur aus der Launcher-Liste entfernt. OpenCode selbst bleibt unverändert.",
            Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(136, 136, 148)),
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(0, 0, 0, 22)
        });

        var buttons = new System.Windows.Controls.StackPanel
        {
            Orientation = System.Windows.Controls.Orientation.Horizontal,
            HorizontalAlignment = HorizontalAlignment.Right
        };
        var cancel = DialogButton("Abbrechen", false);
        var remove = DialogButton("Entfernen", true);
        cancel.Click += (_, _) => { w.DialogResult = false; w.Close(); };
        remove.Click += (_, _) => { w.DialogResult = true; w.Close(); };
        buttons.Children.Add(cancel);
        buttons.Children.Add(remove);
        sp.Children.Add(buttons);
        root.Child = sp;
        w.Content = root;
        return w.ShowDialog() == true;
    }

    private static System.Windows.Controls.Button DialogButton(string text, bool accent)
    {
        var button = new System.Windows.Controls.Button
        {
            Content = text,
            Padding = new Thickness(18, 8, 18, 8),
            Margin = new Thickness(8, 0, 0, 0),
            Foreground = System.Windows.Media.Brushes.White,
            Background = new System.Windows.Media.SolidColorBrush(accent
                ? System.Windows.Media.Color.FromRgb(91, 91, 214)
                : System.Windows.Media.Color.FromRgb(43, 43, 61)),
            BorderThickness = new Thickness(0),
            Cursor = System.Windows.Input.Cursors.Hand
        };
        var template = new System.Windows.Controls.ControlTemplate(typeof(System.Windows.Controls.Button));
        var border = new System.Windows.FrameworkElementFactory(typeof(System.Windows.Controls.Border));
        border.SetValue(System.Windows.Controls.Border.BackgroundProperty, new System.Windows.TemplateBindingExtension(System.Windows.Controls.Button.BackgroundProperty));
        border.SetValue(System.Windows.Controls.Border.CornerRadiusProperty, new CornerRadius(8));
        border.SetValue(System.Windows.Controls.Border.PaddingProperty, new System.Windows.TemplateBindingExtension(System.Windows.Controls.Button.PaddingProperty));
        var presenter = new System.Windows.FrameworkElementFactory(typeof(System.Windows.Controls.ContentPresenter));
        presenter.SetValue(System.Windows.Controls.ContentPresenter.HorizontalAlignmentProperty, HorizontalAlignment.Center);
        presenter.SetValue(System.Windows.Controls.ContentPresenter.VerticalAlignmentProperty, VerticalAlignment.Center);
        border.AppendChild(presenter);
        template.VisualTree = border;
        button.Template = template;
        return button;
    }
}
