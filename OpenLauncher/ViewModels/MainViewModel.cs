using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using OpenLauncher.Models;
using OpenLauncher.Services;

namespace OpenLauncher.ViewModels;

public sealed partial class MainViewModel : ObservableObject
{
    private readonly ModelRegistry _registry;
    private readonly OpenRouterService _router = new();
    private readonly OpenLauncherService _launcher = new();
    private readonly OpenCodeUpdateService _updater = new();
    private readonly InstructionProfileService _profiles = new();
    private CancellationTokenSource? _loadCts;
    private CancellationTokenSource? _thinkingCts;

    public MainViewModel()
    {
        _registry = ModelRegistry.Load();
        foreach (var g in _registry.Groups) ModelGroups.Add(g);
        foreach (var model in ModelGroups.SelectMany(group => group.Models).Where(model => model.IsHidden))
            HiddenModels.Add(model);
        HasHiddenModels = HiddenModels.Count > 0;
        Profiles.Add(new InstructionProfileEntry
        {
            Id = "minimal",
            DisplayName = "Minimal",
            Description = "Frisches, leeres Profil zum Ausbauen",
            IsEnabled = true
        });
        Profiles.Add(new InstructionProfileEntry
        {
            Id = "standard",
            DisplayName = "Standard",
            Description = "Bewährte globale und Projektregeln",
            IsEnabled = true
        });
        Profiles.Add(new InstructionProfileEntry
        {
            Id = "strict",
            DisplayName = "Strikt",
            Description = "Mehr Kontrolle und Absicherung",
            IsEnabled = true
        });
        WorkModes.Add(new WorkModeEntry
        {
            Id = "frei",
            DisplayName = "Freimodus",
            Description = "Kein zusätzlicher Modus-Prompt"
        });
        WorkModes.Add(new WorkModeEntry
        {
            Id = "schnell",
            DisplayName = "Schnellmodus",
            Description = "Kleinster korrekter Eingriff"
        });
        WorkModes.Add(new WorkModeEntry
        {
            Id = "normal",
            DisplayName = "Normalmodus",
            Description = "Passend zu Risiko und Umfang"
        });
        WorkModes.Add(new WorkModeEntry
        {
            Id = "gruendlich",
            DisplayName = "Gründlichkeitsmodus",
            Description = "Randfälle und Härtung mitprüfen"
        });
        SelectedProfile = Profiles.Single(profile => profile.Id == "minimal");
        // Freimodus ist die Vorauswahl fuer JEDES Profil und JEDES Modell: der Modellwechsel setzt
        // das Minimalprofil, der Profilwechsel setzt wieder diesen Modus -> ohne aktives Umschalten
        // laeuft jede Session ohne zusaetzlichen Modus-Prompt.
        SelectedWorkMode = WorkModes.Single(mode => mode.Id == "frei");
        SelectedModel = ModelGroups.SelectMany(group => group.Models).FirstOrDefault(model => !model.IsHidden);
        _ = RefreshOpenRouterFreeModelsAsync();
        WorkDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        _ = CheckOpenCodeUpdateAsync();

        // Version UND Zeitstempel kommen aus der Assembly: die Uhrzeit setzt MSBuild beim Compile
        // (Target "SetBuildTimestamp"), damit hier nie wieder ein von Hand getippter - und damit
        // moeglicherweise falscher - Zeitpunkt steht. Fallback nur, falls das Attribut fehlt.
        var assembly = Assembly.GetExecutingAssembly();
        var version = assembly.GetName().Version?.ToString(3) ?? "?";
        var buildTimestamp = assembly.GetCustomAttributes<AssemblyMetadataAttribute>()
            .FirstOrDefault(attribute => attribute.Key == "BuildTimestamp")?.Value;
        Version = string.IsNullOrWhiteSpace(buildTimestamp)
            ? $"Version {version}"
            : $"Version {version} ({buildTimestamp} Uhr)";
    }

    public ObservableCollection<ModelGroupEntry> ModelGroups { get; } = new();
    public ObservableCollection<ProviderEntry> Providers { get; } = new();
    public ObservableCollection<ThinkingOptionEntry> ThinkingOptions { get; } = new();
    public ObservableCollection<InstructionProfileEntry> Profiles { get; } = new();
    public ObservableCollection<WorkModeEntry> WorkModes { get; } = new();
    public ObservableCollection<ModelEntry> HiddenModels { get; } = new();

    [ObservableProperty] private ModelEntry? _selectedModel;
    [ObservableProperty] private ProviderEntry? _selectedProvider;
    [ObservableProperty] private ThinkingOptionEntry? _selectedThinkingOption;
    [ObservableProperty] private bool _hasThinkingOptions;
    [ObservableProperty] private bool _hasNoThinkingOptions = true;
    [ObservableProperty] private string _thinkingEmptyText = "Modell wählen.";
    [ObservableProperty] private string _thinkingTitle = "THINKING";
    [ObservableProperty] private string _thinkingSubtitle = "Reasoning-Level";
    [ObservableProperty] private string _workDir = string.Empty;
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string _statusText = "Bereit.";
    [ObservableProperty] private string _version = string.Empty;
    [ObservableProperty] private string _lastErrorDetails = "Noch kein Fehler protokolliert.";
    [ObservableProperty] private string _lastErrorPath = string.Empty;
    [ObservableProperty] private bool _hasLastError;
    [ObservableProperty] private InstructionProfileEntry? _selectedProfile;
    [ObservableProperty] private WorkModeEntry? _selectedWorkMode;
    [ObservableProperty] private string _profileContextText = "OpenCode · AGENTS.md";
    [ObservableProperty] private bool _canEditSelectedProfile = true;
    [ObservableProperty] private bool _hasHiddenModels;

    partial void OnSelectedModelChanged(ModelEntry? value)
    {
        SelectedProfile = Profiles.FirstOrDefault(profile => profile.Id == "minimal");
        SelectedProvider = null;
        Providers.Clear();
        SelectedThinkingOption = null;
        ThinkingOptions.Clear();
        ThinkingTitle = IsClaudeCodeModel(value) ? "EFFORT" : "THINKING";
        ThinkingSubtitle = IsClaudeCodeModel(value) ? "Claude-Code-Level" : "Reasoning-Level";
        ProfileContextText = IsClaudeCodeModel(value) ? "Claude Code · Minimal + Standard + Strikt" : "OpenCode · Profil-Snapshots";
        UpdateProfileAvailability();
        if (value == null)
        {
            _loadCts?.Cancel();
            _thinkingCts?.Cancel();
            UpdateThinkingState("Modell wählen.");
            return;
        }

        UpdateThinkingState("Lade Thinking …");
        _ = LoadThinkingOptionsAsync(value);
        _ = LoadProvidersAsync(value);
    }

    partial void OnSelectedThinkingOptionChanged(ThinkingOptionEntry? value)
    {
        if (SelectedModel == null || value == null) return;
        var label = IsClaudeCodeModel(SelectedModel) ? "Effort" : "Thinking";
        StatusText = $"{label} für {SelectedModel.DisplayName}: {value.DisplayName}";
    }

    private async Task LoadThinkingOptionsAsync(ModelEntry model, bool forceRefresh = false)
    {
        _thinkingCts?.Cancel();
        _thinkingCts?.Dispose();
        _thinkingCts = new CancellationTokenSource();
        var ct = _thinkingCts.Token;

        try
        {
            var levels = GetStaticThinkingLevels(model).ToList();
            if (string.Equals(model.ProviderId, "openrouter", StringComparison.OrdinalIgnoreCase))
            {
                var apiLevels = await _router.GetThinkingLevelsAsync(model.Slug, ct, forceRefresh);
                if (apiLevels.Count > 0) levels = apiLevels;
            }

            if (ct.IsCancellationRequested) return;
            ThinkingOptions.Clear();
            foreach (var option in levels.Select(ToThinkingOption)) ThinkingOptions.Add(option);
            SelectProfileThinkingOption();
            var empty = IsClaudeCodeModel(model) ? "Kein Effort für dieses Modell erkannt." : "Kein Thinking für dieses Modell erkannt.";
            var prompt = IsClaudeCodeModel(model) ? "Effort-Wert wählen." : "Thinking-Wert wählen.";
            UpdateThinkingState(levels.Count == 0 ? empty : prompt);
        }
        catch (OperationCanceledException) { /* ok */ }
        catch (Exception ex)
        {
            Logger.Instance.Warn("MainViewModel", "LoadThinkingOptionsAsync", $"Thinking-/Effort-Level nicht geladen: {ex.Message}", new { model.Slug });
            ThinkingOptions.Clear();
            UpdateThinkingState(IsClaudeCodeModel(model) ? "Effort konnte nicht geladen werden." : "Thinking konnte nicht geladen werden.");
        }
    }

    private void UpdateThinkingState(string emptyText)
    {
        HasThinkingOptions = ThinkingOptions.Count > 0;
        HasNoThinkingOptions = !HasThinkingOptions;
        ThinkingEmptyText = HasThinkingOptions ? "" : emptyText;
    }

    private static IEnumerable<string> GetStaticThinkingLevels(ModelEntry model)
    {
        return OpenCodeVariantCatalog.GetLauncherLevels(model);
    }

    private static ThinkingOptionEntry ToThinkingOption(string value)
    {
        var normalized = value.Trim().ToLowerInvariant();
        return new ThinkingOptionEntry
        {
            Value = normalized,
            DisplayName = normalized switch
            {
                "xhigh" => "X High",
                "max" => "Max",
                "thinking" => "Thinking",
                "none" => "None",
                "minimal" => "Minimal",
                "low" => "Low",
                "medium" => "Medium",
                "high" => "High",
                _ => normalized
            },
            Description = normalized switch
            {
                "none" => "aus",
                "minimal" => "sehr knapp",
                "low" => "leicht",
                "medium" => "empfohlen",
                "high" => "gründlich",
                "xhigh" => "maximal hoch",
                "max" => "Maximum",
                "thinking" => "aktiviert",
                _ => "Thinking"
            }
        };
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

    partial void OnSelectedProfileChanged(InstructionProfileEntry? value)
    {
        UpdateProfileAvailability();
        if (value == null) return;

        SelectedWorkMode = WorkModes.Single(mode => mode.Id == "frei");
        SelectProfileThinkingOption();
        StatusText = $"Profil {value.DisplayName} ausgewählt.";
    }

    private void SelectProfileThinkingOption()
    {
        if (SelectedProfile == null || ThinkingOptions.Count == 0) return;

        var preferredValue = SelectedProfile.Id == "strict" ? "xhigh" : "high";
        SelectedThinkingOption = ThinkingOptions.FirstOrDefault(option => option.Value == preferredValue)
            ?? ThinkingOptions.Last();
    }

    private void UpdateProfileAvailability()
    {
        CanEditSelectedProfile = SelectedProfile != null &&
            (!IsClaudeCodeModel(SelectedModel) || IsClaudeCodeProfileSupported(SelectedProfile.Id));
    }

    // Claude Code unterstuetzt alle drei Profile, jedes mit eigenem Repo-Config-Ordner
    // (CLAUDE_CONFIG_DIR): Minimal (regelfrei, Skills nur per Junction), Standard und Strikt
    // (versionierte skills/rules/agents/commands im Repo, frei bearbeitbar, auf jedem Rechner gleich).
    private static bool IsClaudeCodeProfileSupported(string profileId) =>
        profileId is "standard" or "minimal" or "strict";

    private async Task CheckOpenCodeUpdateAsync()
    {
        var result = await _updater.CheckAsync();
        if (result.Status == "installed")
            StatusText = result.Message;
        else if (result.Status == "failed")
            StatusText = $"OpenCode-Update zurückgestellt; letzter Windows-Fix bleibt aktiv.";
    }

    private async Task LoadProvidersAsync(ModelEntry model)
    {
        _loadCts?.Cancel();
        _loadCts?.Dispose();
        _loadCts = new CancellationTokenSource();
        var ct = _loadCts.Token;
        IsLoading = true;
        StatusText = $"Lade Provider für {model.DisplayName} …";
        try
        {
            if (!string.Equals(model.ProviderId, "openrouter", StringComparison.OrdinalIgnoreCase))
            {
                var direct = new ProviderEntry
                {
                    ProviderName = model.ProviderName,
                    ProviderSlug = model.ProviderId,
                    Tag = model.ProviderId,
                    Status = 0
                };
                await EnrichDirectProviderAsync(model, direct, ct);
                // Nach dem await prüfen, ob dieser Ladevorgang schon abgelöst wurde (schneller
                // Modellwechsel): sonst schreibt eine späte Antwort die Provider des falschen
                // Modells und Start würde mit falschem Provider konfigurieren.
                if (ct.IsCancellationRequested) return;
                Providers.Clear();
                Providers.Add(direct);
                SelectedProvider = direct;
                StatusText = $"{model.DisplayName} über {model.ProviderName} bereit.";
                return;
            }

            var (displayName, providers) = await _router.GetProvidersAsync(model.Slug, ct);
            if (ct.IsCancellationRequested) return;
            if (!string.IsNullOrWhiteSpace(displayName) && displayName != model.DisplayName)
            {
                // Anzeigename aus API übernehmen, falls Liste ihn noch als Slug zeigt.
                // Author-Präfix ("Z-AI: ") case-insensitiv entfernen: der Slug-Author ist klein-,
                // der API-Name großgeschrieben — ein case-sensitives Replace ließe den Präfix stehen.
                var authorPrefix = $"{model.Slug.Split('/')[0]}: ";
                model.DisplayName = displayName.StartsWith(authorPrefix, StringComparison.OrdinalIgnoreCase)
                    ? displayName[authorPrefix.Length..]
                    : displayName;
            }
            Providers.Clear();
            foreach (var p in providers) Providers.Add(p);
            SelectedProvider = Providers.FirstOrDefault();
            StatusText = providers.Count == 0
                ? $"Keine Provider für {model.DisplayName} gefunden (Slug korrekt?)."
                : $"{providers.Count} Provider für {model.DisplayName} geladen.";
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { /* durch Modellwechsel abgelöst */ }
        catch (Exception ex)
        {
            // Ein HttpClient.Timeout wirft ebenfalls eine (Task)OperationCanceledException, aber mit
            // NICHT abgebrochenem ct. Diese darf NICHT als stiller Abbruch verschluckt werden, sonst
            // bliebe der Status auf "Lade Provider …" hängen. Der when-Filter oben lässt sie hierher
            // durchfallen, damit ein echter Fehlerbericht entsteht.
            StatusText = $"Fehler: {ex.Message}";
            var details = BuildErrorDetails("Provider laden", ex, model, null, null);
            LastErrorPath = Logger.Instance.WriteErrorReport("provider_load", details);
            LastErrorDetails = details + $"{Environment.NewLine}{Environment.NewLine}Gespeichert unter: {LastErrorPath}";
            HasLastError = true;
            Logger.Instance.Error("MainViewModel", "LoadProvidersAsync", ex, new { model.Slug, LastErrorPath });
        }
        finally
        {
            // Nur der aktuelle (nicht abgelöste) Ladevorgang darf die Ladeanzeige zurücksetzen —
            // sonst löscht ein per Modellwechsel abgebrochener Vorgang sie, während der nachfolgende
            // Ladevorgang noch läuft (Ladeanzeige würde vorzeitig verschwinden).
            if (!ct.IsCancellationRequested) IsLoading = false;
        }
    }

    private async Task EnrichDirectProviderAsync(ModelEntry model, ProviderEntry direct, CancellationToken ct)
    {
        var metadata = OpenCodeModelMetadataCatalog.Find(model.ProviderId, model.Slug);
        if (metadata == null) return;

        direct.ContextLength = metadata.ContextLength;
        try
        {
            var (_, providers) = await _router.GetProvidersAsync(metadata.OpenRouterSlug, ct);
            if (providers.Count == 0) return;

            var fastest = providers
                .Where(p => p.ThroughputLast30m.HasValue)
                .OrderByDescending(p => p.ThroughputLast30m!.Value)
                .FirstOrDefault();
            if (fastest != null)
            {
                direct.ThroughputLast30m = fastest.ThroughputLast30m;
                direct.ContextLength = Math.Max(direct.ContextLength, fastest.ContextLength);
            }
            else
            {
                direct.ContextLength = Math.Max(direct.ContextLength, providers.Max(p => p.ContextLength));
            }
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            // Echter Abbruch (Modellwechsel) -> Load abbrechen.
            throw;
        }
        catch (Exception ex)
        {
            // Auch ein Metadaten-Timeout landet hier (ct nicht abgebrochen): das Enrichment ist nur
            // Best-Effort, der Direkt-Provider wird trotzdem ohne Zusatz-Metadaten angezeigt.
            Logger.Instance.Warn("MainViewModel", "EnrichDirectProviderAsync", $"OpenCode-Metadaten-Fallback für {model.Slug}: {ex.Message}", new { model.ProviderId, metadata.OpenRouterSlug });
        }
    }

    [RelayCommand]
    private async Task RefreshAsync()
    {
        var model = SelectedModel;
        if (model == null) return;

        UpdateThinkingState("Lade Thinking …");
        await Task.WhenAll(
            LoadProvidersAsync(model),
            LoadThinkingOptionsAsync(model, forceRefresh: true));
    }

    [RelayCommand]
    private void AddModel()
    {
        var defaultGroup = FindGroupForModel(SelectedModel) ?? ModelGroups.FirstOrDefault(g => g.Id == "openrouter") ?? ModelGroups.FirstOrDefault();
        if (defaultGroup == null) return;
        var result = ShowAddModelDialog(ModelGroups, defaultGroup);
        if (result == null) return;
        var (targetGroup, slug, display) = result.Value;
        if (_registry.AddModel(targetGroup, slug, display))
        {
            var entry = targetGroup.Models.Last();
            SelectedModel = entry;
            targetGroup.IsExpanded = true;
            targetGroup.RefreshHeaderText();
            StatusText = $"Modell '{entry.DisplayName}' zu '{targetGroup.Title}' hinzugefügt.";
        }
        else
        {
            StatusText = $"Modell '{slug}' existiert in '{targetGroup.Title}' bereits.";
        }
    }

    [RelayCommand]
    private void EditModel()
    {
        var model = SelectedModel;
        if (model == null)
        {
            StatusText = "Kein Modell ausgewählt.";
            return;
        }
        var group = FindGroupForModel(model);
        if (group == null) return;

        var result = ShowModelDialog(ModelGroups, group, "Modell bearbeiten", "Speichern", model.Slug, model.DisplayName);
        if (result == null) return;
        var (targetGroup, slug, display) = result.Value;

        if (!_registry.UpdateModel(group, model, targetGroup, slug, display))
        {
            StatusText = $"Modell '{slug}' existiert in '{targetGroup.Title}' bereits.";
            return;
        }

        targetGroup.IsExpanded = true;
        // Auswahl neu setzen: Provider/Thinking haengen am Slug und muessen nach der
        // Bearbeitung neu geladen werden.
        SelectedModel = null;
        SelectedModel = model;
        StatusText = ReferenceEquals(group, targetGroup)
            ? $"Modell '{model.DisplayName}' bearbeitet."
            : $"Modell '{model.DisplayName}' bearbeitet und nach '{targetGroup.Title}' verschoben.";
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
        SelectedModel = group.Models.Skip(Math.Min(idx, group.Models.Count)).FirstOrDefault(model => !model.IsHidden)
            ?? group.Models.Take(Math.Min(idx, group.Models.Count)).LastOrDefault(model => !model.IsHidden)
            ?? ModelGroups.SelectMany(candidateGroup => candidateGroup.Models).FirstOrDefault(model => !model.IsHidden);
    }

    [RelayCommand]
    private void HideModel(ModelEntry model)
    {
        if (model.IsHidden) return;
        var group = FindGroupForModel(model);
        if (group == null) return;

        var index = group.Models.IndexOf(model);
        model.IsHidden = true;
        HiddenModels.Add(model);
        HasHiddenModels = true;
        group.RefreshHeaderText();
        _registry.Save();

        if (ReferenceEquals(SelectedModel, model))
        {
            SelectedModel = group.Models.Skip(index + 1).FirstOrDefault(candidate => !candidate.IsHidden)
                ?? group.Models.Take(index).LastOrDefault(candidate => !candidate.IsHidden)
                ?? ModelGroups.SelectMany(candidateGroup => candidateGroup.Models).FirstOrDefault(candidate => !candidate.IsHidden);
        }

        StatusText = $"Modell '{model.DisplayName}' ausgeblendet.";
    }

    [RelayCommand]
    private void RestoreModel(ModelEntry model)
    {
        if (!model.IsHidden) return;
        var group = FindGroupForModel(model);
        if (group == null) return;

        model.IsHidden = false;
        HiddenModels.Remove(model);
        HasHiddenModels = HiddenModels.Count > 0;
        group.RefreshHeaderText();
        _registry.Save();
        SelectedModel ??= model;
        StatusText = $"Modell '{model.DisplayName}' wieder eingeblendet.";
    }

    [RelayCommand]
    private void ShowHiddenModels()
    {
        var window = new HiddenModelsWindow(this)
        {
            Owner = Application.Current.MainWindow
        };
        window.ShowDialog();
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
        if (SelectedProfile == null)
        {
            StatusText = "Bitte ein Profil wählen.";
            return;
        }
        if (SelectedWorkMode == null)
        {
            StatusText = "Bitte einen Modus wählen.";
            return;
        }
        var isClaudeCode = IsClaudeCodeModel(SelectedModel);
        if (isClaudeCode && !IsClaudeCodeProfileSupported(SelectedProfile.Id))
        {
            StatusText = $"Profil {SelectedProfile.DisplayName} ist für Claude Code noch nicht eingerichtet.";
            return;
        }
        if (!Directory.Exists(WorkDir))
        {
            StatusText = "Arbeitsverzeichnis existiert nicht.";
            return;
        }
        try
        {
            var thinkingLevel = SelectedThinkingOption?.CommandValue;
            var profileDocuments = _profiles.LoadProfile(isClaudeCode, SelectedProfile.Id, WorkDir);
            Logger.Instance.Info("MainViewModel", "Start", "Vollständige Startauswahl geprüft", new
            {
                model = SelectedModel.ModelString,
                provider = SelectedProvider.ProviderName,
                providerSlug = SelectedProvider.ProviderSlug,
                thinkingLevel,
                profile = SelectedProfile.Id,
                workMode = SelectedWorkMode.Id,
                profileGlobal = profileDocuments.GlobalPath,
                profileProject = profileDocuments.ProjectPath,
                workDir = WorkDir
            });
            if (isClaudeCode)
            {
                // Jedes Profil hat seinen eigenen Repo-Config-Ordner (CLAUDE_CONFIG_DIR): Standard/Strikt mit
                // versionierten skills/rules/agents/commands, Minimal regelfrei (Skills per Junction).
                // Der Modus-Prompt haengt hinter dem Profil in der aktiven CLAUDE.md -> er gilt fuer
                // die ganze Session, genau wie bei OpenCode.
                var claudeConfigDir = _profiles.EnsureClaudeConfigDir(SelectedProfile.Id, SelectedWorkMode.Id);
                _launcher.LaunchClaudeCode(SelectedModel.Slug, WorkDir, thinkingLevel, claudeConfigDir);
                StatusText = string.IsNullOrWhiteSpace(thinkingLevel)
                    ? $"Claude Code gestartet: {SelectedModel.DisplayName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}"
                    : $"Claude Code gestartet: {SelectedModel.DisplayName} · Effort {SelectedThinkingOption?.DisplayName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}";
                return;
            }

            // Projekt-AGENTS.md passend zum Profil setzen (Minimal -> nur minimal.md), BEVOR die
            // Session vorbereitet und OpenCode gestartet wird.
            _profiles.ActivateProjectAgents(SelectedProfile.Id, WorkDir);
            var profileSession = _profiles.PrepareOpenCodeSession(SelectedProfile.Id, WorkDir);
            var modelString = _launcher.ConfigureProvider(SelectedModel, SelectedProvider, Providers, thinkingLevel);
            _launcher.Launch(modelString, WorkDir, thinkingLevel, profileSession.ConfigPath, SelectedWorkMode.Id);
            Logger.Instance.Info("MainViewModel", "Start", "OpenCode-Profilsnapshot erstellt", new
            {
                profileSession.ProfileId,
                profileSession.SourceGlobalPath,
                profileSession.SourceProjectPath,
                profileSession.GlobalSnapshotPath,
                profileSession.ProjectSnapshotPath,
                profileSession.ConfigPath
            });
            StatusText = string.IsNullOrWhiteSpace(thinkingLevel)
                ? $"OpenCode gestartet: {SelectedModel.DisplayName} via {SelectedProvider.ProviderName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}"
                : $"OpenCode gestartet: {SelectedModel.DisplayName} via {SelectedProvider.ProviderName} · Thinking {SelectedThinkingOption?.DisplayName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}";
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
            Title = "OpenLauncher - Fehlerdetails",
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
    private void EditProfile()
    {
        if (SelectedProfile is not { IsEnabled: true } || SelectedModel == null) return;

        var isClaudeCode = IsClaudeCodeModel(SelectedModel);
        if (isClaudeCode && !IsClaudeCodeProfileSupported(SelectedProfile.Id)) return;
        try
        {
            var documents = _profiles.LoadProfile(isClaudeCode, SelectedProfile.Id, WorkDir);
            // Jedes Profil ist genau EINE Datei -> Editor zeigt Dateiname + Pfad und ein Textfeld.
            var editor = new ProfileEditorWindow(documents, isClaudeCode, SelectedProfile.DisplayName)
            {
                Owner = Application.Current.MainWindow
            };
            if (editor.ShowDialog() != true) return;

            _profiles.SaveProfile(isClaudeCode, SelectedProfile.Id, WorkDir, editor.GlobalText, editor.ProjectText);
            StatusText = $"Profil {SelectedProfile.DisplayName} für {(isClaudeCode ? "Claude Code" : "OpenCode")} gespeichert.";
            Logger.Instance.Info("MainViewModel", "EditProfile", "Profil gespeichert", new
            {
                cli = isClaudeCode ? "claude" : "opencode",
                profile = SelectedProfile.Id,
                documents.GlobalPath,
                documents.ProjectPath
            });
        }
        catch (Exception ex)
        {
            StatusText = $"Profil konnte nicht gespeichert werden: {ex.Message}";
            Logger.Instance.Error("MainViewModel", "EditProfile", ex, new { WorkDir, model = SelectedModel.ModelString });
        }
    }

    /// <summary>
    /// Bearbeitet den Prompt des gewaehlten Arbeitsmodus. Der gespeicherte Text ist die einzige
    /// Quelle: OpenCode liest dieselbe Datei bei jedem Modellaufruf (auch nach dem Umschalten in der
    /// TUI), Claude Code bekommt sie beim Start hinter das Profil geschrieben.
    /// </summary>
    [RelayCommand]
    private void EditWorkMode()
    {
        if (SelectedWorkMode == null) return;

        try
        {
            var path = InstructionProfileService.ResolveWorkModeSourcePath(SelectedWorkMode.Id);
            var editor = new ProfileEditorWindow(SelectedWorkMode.DisplayName, path, _profiles.LoadWorkMode(SelectedWorkMode.Id))
            {
                Owner = Application.Current.MainWindow
            };
            if (editor.ShowDialog() != true) return;

            _profiles.SaveWorkMode(SelectedWorkMode.Id, editor.GlobalText);
            StatusText = $"Modus {SelectedWorkMode.DisplayName} gespeichert.";
            Logger.Instance.Info("MainViewModel", "EditWorkMode", "Modus-Prompt gespeichert", new { mode = SelectedWorkMode.Id, path });
        }
        catch (Exception ex)
        {
            StatusText = $"Modus konnte nicht gespeichert werden: {ex.Message}";
            Logger.Instance.Error("MainViewModel", "EditWorkMode", ex, new { mode = SelectedWorkMode?.Id });
        }
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

    public void MoveModel(ModelGroupEntry sourceGroup, int from, ModelGroupEntry targetGroup, int to)
    {
        if (from < 0 || from >= sourceGroup.Models.Count) return;
        var item = sourceGroup.Models[from];
        if (!_registry.MoveModel(sourceGroup, from, targetGroup, to))
        {
            StatusText = $"Modell '{item.DisplayName}' existiert in '{targetGroup.Title}' bereits.";
            return;
        }
        targetGroup.IsExpanded = true;
        SelectedModel = item;
        StatusText = ReferenceEquals(sourceGroup, targetGroup)
            ? $"Modell in '{targetGroup.Title}' verschoben."
            : $"Modell nach '{targetGroup.Title}' verschoben.";
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

    private static bool IsClaudeCodeModel(ModelEntry? model) =>
        string.Equals(model?.ProviderId, "anthropic", StringComparison.OrdinalIgnoreCase);

    private static (ModelGroupEntry Group, string Slug, string DisplayName)? ShowAddModelDialog(IEnumerable<ModelGroupEntry> groups, ModelGroupEntry defaultGroup) =>
        ShowModelDialog(groups, defaultGroup, "Neues Modell hinzufügen", "Hinzufügen", string.Empty, string.Empty);

    private static (ModelGroupEntry Group, string Slug, string DisplayName)? ShowModelDialog(
        IEnumerable<ModelGroupEntry> groups,
        ModelGroupEntry defaultGroup,
        string title,
        string confirmText,
        string initialSlug,
        string initialDisplayName)
    {
        var groupList = groups.ToList();
        if (groupList.Count == 0) return null;

        var w = new Window
        {
            Title = title,
            Width = 560,
            Height = 330,
            WindowStartupLocation = WindowStartupLocation.CenterScreen,
            ResizeMode = ResizeMode.NoResize,
            Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(32, 32, 40)),
        };
        var sp = new System.Windows.Controls.StackPanel { Margin = new Thickness(18) };
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "In welche Kategorie soll das Modell?",
            Foreground = System.Windows.Media.Brushes.White,
            Margin = new Thickness(0, 0, 0, 8)
        });
        var category = new System.Windows.Controls.ComboBox
        {
            ItemsSource = groupList,
            DisplayMemberPath = nameof(ModelGroupEntry.Title),
            SelectedItem = defaultGroup,
            Padding = new Thickness(8),
            Margin = new Thickness(0, 0, 0, 14)
        };
        sp.Children.Add(category);
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "Modell-ID",
            Foreground = System.Windows.Media.Brushes.White,
            Margin = new Thickness(0, 0, 0, 8)
        });
        var slugInput = new System.Windows.Controls.TextBox { Text = initialSlug, Padding = new Thickness(8), Margin = new Thickness(0, 0, 0, 12) };
        sp.Children.Add(slugInput);
        sp.Children.Add(new System.Windows.Controls.TextBlock
        {
            Text = "Anzeigename (optional)",
            Foreground = System.Windows.Media.Brushes.White,
            Margin = new Thickness(0, 0, 0, 8)
        });
        var displayInput = new System.Windows.Controls.TextBox { Text = initialDisplayName, Padding = new Thickness(8) };
        sp.Children.Add(displayInput);

        var buttons = new System.Windows.Controls.StackPanel
        {
            Orientation = System.Windows.Controls.Orientation.Horizontal,
            HorizontalAlignment = HorizontalAlignment.Right,
            Margin = new Thickness(0, 18, 0, 0)
        };
        var cancel = DialogButton("Abbrechen", false);
        var confirm = DialogButton(confirmText, true);
        cancel.Click += (_, _) => { w.DialogResult = false; w.Close(); };
        confirm.Click += (_, _) =>
        {
            if (category.SelectedItem is not ModelGroupEntry || string.IsNullOrWhiteSpace(slugInput.Text)) return;
            w.DialogResult = true;
            w.Close();
        };
        buttons.Children.Add(cancel);
        buttons.Children.Add(confirm);
        sp.Children.Add(buttons);
        w.Content = sp;
        slugInput.Focus();
        slugInput.CaretIndex = slugInput.Text.Length;

        return w.ShowDialog() == true && category.SelectedItem is ModelGroupEntry selectedGroup
            ? (selectedGroup, slugInput.Text.Trim(), displayInput.Text.Trim())
            : null;
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
