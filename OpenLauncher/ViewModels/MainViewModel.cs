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
    private readonly OpenCodeCatalogService _openCodeCatalog = new();
    private readonly LmStudioService _lmStudio = new();
    private readonly OpenLauncherService _launcher = new();
    private readonly OpenCodeUpdateService _updater = new();
    private readonly InstructionProfileService _profiles = new();
    private readonly ModelDefaultsService _modelDefaults = new();
    private CancellationTokenSource? _loadCts;
    private CancellationTokenSource? _thinkingCts;

    // Standard des gerade gewaehlten Modells, solange er noch greift. Er ueberstimmt die
    // profilabhaengige Effort-Vorauswahl - aber nur bis der Nutzer das Profil selbst umstellt;
    // danach gilt wieder die normale Regel (Strikt -> X High, sonst High).
    private ModelDefaultEntry? _pendingModelDefault;
    // True, solange OnSelectedModelChanged den gespeicherten Standard setzt: die dabei ausgeloesten
    // Profil-Wechsel sind programmatisch und duerfen _pendingModelDefault nicht verwerfen.
    private bool _applyingModelDefault;

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
        // Ziel-CLI: nur bei OpenAI-Modellen sichtbar. Beide CLIs lesen dieselbe Profil-AGENTS.md,
        // deshalb gelten Minimal/Standard/Strikt und der Arbeitsmodus in beiden gleich.
        CliTargets.Add(new CliTargetEntry
        {
            Id = "opencode",
            DisplayName = "OpenCode",
            Description = "Wie bisher: OpenCode-TUI mit Provider-Wahl"
        });
        CliTargets.Add(new CliTargetEntry
        {
            Id = "codex",
            DisplayName = "Codex CLI",
            Description = "Eigenes OpenAI-CLI, Profil aus der AGENTS.md"
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
        SelectedCliTarget = CliTargets.Single(target => target.Id == "opencode");
        // Freimodus ist die Vorauswahl fuer JEDES Profil und JEDES Modell: der Modellwechsel setzt
        // das Minimalprofil, der Profilwechsel setzt wieder diesen Modus -> ohne aktives Umschalten
        // laeuft jede Session ohne zusaetzlichen Modus-Prompt.
        SelectedWorkMode = WorkModes.Single(mode => mode.Id == "frei");
        SelectedModel = ModelGroups.SelectMany(group => group.Models).FirstOrDefault(model => !model.IsHidden);
        _ = RefreshOpenRouterFreeModelsAsync();
        _ = RefreshLmStudioModelsAsync();
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
    public ObservableCollection<CliTargetEntry> CliTargets { get; } = new();
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
    [ObservableProperty] private CliTargetEntry? _selectedCliTarget;
    /// <summary>Nur OpenAI-Modelle laufen wahlweise in OpenCode oder im Codex CLI -- sonst ist die
    /// CLI-Zeile ausgeblendet und es bleibt beim jeweils einzigen Weg.</summary>
    [ObservableProperty] private bool _hasCliChoice;
    [ObservableProperty] private string _profileContextText = "OpenCode · AGENTS.md";
    [ObservableProperty] private bool _canEditSelectedProfile = true;
    [ObservableProperty] private bool _hasHiddenModels;
    [ObservableProperty] private string _modelDefaultButtonText = "Standard speichern";
    [ObservableProperty] private string _modelDefaultSummary = string.Empty;
    [ObservableProperty] private bool _hasModelDefault;
    // Gegenstueck zu HasModelDefault: die Kontextzeile ("Claude Code · Minimal + …") und die
    // Standard-Zeile teilen sich denselben Platz. Ohne dieses Gegenstueck stuenden beide
    // untereinander und der Profil-Bereich saesse mit gespeichertem Standard eine Zeile tiefer.
    [ObservableProperty] private bool _hasNoModelDefault = true;
    [ObservableProperty] private bool _canSaveModelDefault;

    partial void OnSelectedModelChanged(ModelEntry? value)
    {
        // Gespeicherter Standard des Modells zuerst holen: er bestimmt Profil, Modus und (spaeter,
        // sobald die Stufen geladen sind) den Effort. Ohne Standard bleibt es beim Minimalprofil.
        _pendingModelDefault = value == null ? null : _modelDefaults.Find(value.ModelString);
        _applyingModelDefault = true;
        try
        {
            SelectedProfile = Profiles.FirstOrDefault(profile => profile.Id == "minimal");
            // Ohne gespeicherten Standard startet jedes Modell wieder auf OpenCode -- das bisherige
            // Verhalten bleibt damit die Vorauswahl.
            SelectedCliTarget = CliTargets.FirstOrDefault(target => target.Id == "opencode");
            if (_pendingModelDefault != null)
            {
                SelectedProfile = Profiles.FirstOrDefault(profile =>
                    profile.Id == _pendingModelDefault.ProfileId && profile.IsEnabled) ?? SelectedProfile;
                SelectedWorkMode = WorkModes.FirstOrDefault(mode => mode.Id == _pendingModelDefault.WorkModeId)
                    ?? SelectedWorkMode;
                if (IsOpenAiModel(value))
                {
                    SelectedCliTarget = CliTargets.FirstOrDefault(target => target.Id == _pendingModelDefault.CliTargetId)
                        ?? SelectedCliTarget;
                }
            }
        }
        finally
        {
            _applyingModelDefault = false;
        }
        HasCliChoice = IsOpenAiModel(value);
        SelectedProvider = null;
        Providers.Clear();
        SelectedThinkingOption = null;
        ThinkingOptions.Clear();
        ThinkingTitle = IsClaudeCodeModel(value) ? "EFFORT" : "THINKING";
        ThinkingSubtitle = IsClaudeCodeModel(value) ? "Claude-Code-Level" : "Reasoning-Level";
        UpdateProfileContextText();
        UpdateProfileAvailability();
        RefreshModelDefaultState();
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
        RefreshModelDefaultState();
        if (SelectedModel == null || value == null) return;
        var label = IsClaudeCodeModel(SelectedModel) ? "Effort" : "Thinking";
        StatusText = $"{label} für {SelectedModel.DisplayName}: {value.DisplayName}";
    }

    partial void OnSelectedWorkModeChanged(WorkModeEntry? value) => RefreshModelDefaultState();

    partial void OnSelectedCliTargetChanged(CliTargetEntry? value)
    {
        UpdateProfileContextText();
        RefreshModelDefaultState();
        if (_applyingModelDefault || value == null || !HasCliChoice) return;
        StatusText = $"Ziel-CLI für {SelectedModel?.DisplayName}: {value.DisplayName}";
    }

    /// <summary>Zeigt unter der Ueberschrift, aus welcher Datei das gewaehlte Werkzeug seine Regeln liest.</summary>
    private void UpdateProfileContextText() => ProfileContextText = IsClaudeCodeModel(SelectedModel)
        ? "Claude Code · Minimal + Standard + Strikt"
        : IsCodexCliSelected
            ? "Codex CLI · Profil + Modus in der AGENTS.md"
            : "OpenCode · Profil-Snapshots";

    private async Task LoadThinkingOptionsAsync(ModelEntry model, bool forceRefresh = false)
    {
        _thinkingCts?.Cancel();
        _thinkingCts?.Dispose();
        _thinkingCts = new CancellationTokenSource();
        var ct = _thinkingCts.Token;
        var previousValue = forceRefresh ? SelectedThinkingOption?.Value : null;
        ThinkingSubtitle = "Prüfe aktuelle Modellfähigkeiten …";

        try
        {
            var levels = GetStaticThinkingLevels(model).ToList();
            var currentLevels = await _openCodeCatalog.GetThinkingLevelsAsync(model, ct, forceRefresh);
            var source = currentLevels != null ? "Aktuell aus models.dev" : "Katalog ohne Stufenangabe · lokale Vorgabe";
            if (currentLevels != null) levels = currentLevels;
            else if (string.Equals(model.ProviderId, "openrouter", StringComparison.OrdinalIgnoreCase))
            {
                var apiLevels = await _router.GetThinkingLevelsAsync(model.Slug, ct, forceRefresh);
                levels = apiLevels;
                source = "Aus OpenRouter-Fähigkeiten abgeleitet";
            }

            if (ct.IsCancellationRequested) return;
            SelectedThinkingOption = null;
            ThinkingOptions.Clear();
            foreach (var option in levels.Select(ToThinkingOption)) ThinkingOptions.Add(option);
            SelectedThinkingOption = ThinkingOptions.FirstOrDefault(option => option.Value == previousValue);
            if (SelectedThinkingOption == null) SelectProfileThinkingOption();
            ThinkingSubtitle = source;
            var empty = IsClaudeCodeModel(model) ? "Kein Effort für dieses Modell erkannt." : "Kein Thinking für dieses Modell erkannt.";
            var prompt = IsClaudeCodeModel(model) ? "Effort-Wert wählen." : "Thinking-Wert wählen.";
            UpdateThinkingState(levels.Count == 0 ? empty : prompt);
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { /* ok */ }
        catch (Exception ex)
        {
            if (ct.IsCancellationRequested) return;
            Logger.Instance.Warn("MainViewModel", "LoadThinkingOptionsAsync", $"Thinking-/Effort-Level nicht geladen: {ex.Message}", new { model.Slug });
            if (ThinkingOptions.Count == 0)
            {
                foreach (var level in GetStaticThinkingLevels(model)) ThinkingOptions.Add(ToThinkingOption(level));
                SelectProfileThinkingOption();
            }
            ThinkingSubtitle = "Aktualisierung fehlgeschlagen · bisherige/lokale Stufen";
            UpdateThinkingState("Thinking/Effort konnte nicht geprüft werden.");
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

    [RelayCommand]
    private async Task RefreshModelCatalogAsync()
    {
        StatusText = "Aktualisiere Modellkataloge …";
        var updated = new List<string>();
        var failed = new List<string>();

        try
        {
            var catalog = await _router.GetModelCatalogAsync(forceRefresh: true);
            _registry.SyncOpenRouterModels(catalog.Models);
            _registry.SyncOpenRouterFreeModels(catalog.FreeModels);
            updated.Add($"OpenRouter {catalog.Models.Count}");
            updated.Add($"OpenRouterFree {catalog.FreeModels.Count}");
        }
        catch (Exception ex)
        {
            failed.Add("OpenRouter");
            Logger.Instance.Warn("MainViewModel", "RefreshModelCatalogAsync", $"OpenRouter-Katalog nicht aktualisiert: {ex.Message}");
        }

        try
        {
            var freeZenModels = await _openCodeCatalog.GetFreeZenModelsAsync();
            if (freeZenModels.Count > 0)
            {
                _registry.SyncOpenCodeZenFreeModels(freeZenModels);
                updated.Add($"OpenCode Zen Free {freeZenModels.Count}");
            }
            else
            {
                failed.Add("OpenCode Zen Free");
            }
        }
        catch (Exception ex)
        {
            failed.Add("OpenCode Zen Free");
            Logger.Instance.Warn("MainViewModel", "RefreshModelCatalogAsync", $"OpenCode-Zen-Katalog nicht aktualisiert: {ex.Message}");
        }

        RefreshHiddenModels();
        if (SelectedModel != null && FindGroupForModel(SelectedModel) == null)
            SelectedModel = ModelGroups.SelectMany(group => group.Models).FirstOrDefault(model => !model.IsHidden);

        StatusText = updated.Count == 0
            ? $"Modellaktualisierung fehlgeschlagen: {string.Join(", ", failed)}. Bestehende Listen bleiben erhalten."
            : failed.Count == 0
                ? $"Modelle aktualisiert: {string.Join(" · ", updated)}."
                : $"Modelle teilweise aktualisiert: {string.Join(" · ", updated)}; nicht erreichbar: {string.Join(", ", failed)}.";
    }

    private void RefreshHiddenModels()
    {
        HiddenModels.Clear();
        foreach (var model in ModelGroups.SelectMany(group => group.Models).Where(model => model.IsHidden))
            HiddenModels.Add(model);
        HasHiddenModels = HiddenModels.Count > 0;
    }

    /// <summary>
    /// Liest beim Start die lokal in LM Studio geladenen/verfuegbaren Modelle und fuellt damit
    /// den Reiter "LM Studio". Ist LM Studio nicht installiert oder der Server aus, bleibt die
    /// zuletzt bekannte Liste stehen — der Launcher startet trotzdem normal.
    /// </summary>
    private async Task RefreshLmStudioModelsAsync()
    {
        try
        {
            if (!LmStudioService.IsInstalled) return;

            var localModels = await _lmStudio.GetLocalModelsWithServerAsync();
            if (localModels.Count == 0) return;

            _registry.SyncLmStudioModels(localModels);
            var group = ModelGroups.FirstOrDefault(g => string.Equals(g.Id, "lmstudio", StringComparison.OrdinalIgnoreCase));
            group?.RefreshHeaderText();
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("MainViewModel", "RefreshLmStudioModelsAsync", $"LM-Studio-Modelle nicht abrufbar: {ex.Message}");
        }
    }

    partial void OnSelectedProfileChanged(InstructionProfileEntry? value)
    {
        // Stellt der Nutzer das Profil selbst um, ist der gespeicherte Standard verlassen: ab hier
        // gilt wieder die profilabhaengige Effort-Vorauswahl.
        if (!_applyingModelDefault) _pendingModelDefault = null;
        UpdateProfileAvailability();
        RefreshModelDefaultState();
        if (value == null) return;

        SelectedWorkMode = WorkModes.Single(mode => mode.Id == "frei");
        SelectProfileThinkingOption();
        StatusText = $"Profil {value.DisplayName} ausgewählt.";
    }

    private void SelectProfileThinkingOption()
    {
        if (SelectedProfile == null || ThinkingOptions.Count == 0) return;

        // Ein von Hand gespeicherter Modell-Standard schlaegt die profilabhaengige Vorauswahl.
        // Er greift erst hier, weil die Effort-Stufen erst nach dem Modellwechsel geladen sind.
        var storedOption = string.IsNullOrWhiteSpace(_pendingModelDefault?.ThinkingValue)
            ? null
            : ThinkingOptions.FirstOrDefault(option => option.Value == _pendingModelDefault!.ThinkingValue);
        if (storedOption != null)
        {
            SelectedThinkingOption = storedOption;
            return;
        }

        var preferredValue = SelectedProfile.Id == "strict" ? "xhigh" : "high";
        SelectedThinkingOption = ThinkingOptions.FirstOrDefault(option => option.Value == preferredValue)
            ?? ThinkingOptions.Last();
    }

    /// <summary>
    /// Haelt Schalterbeschriftung und Standard-Anzeige zum aktuell gewaehlten Modell aktuell.
    /// Entspricht die Auswahl genau dem gespeicherten Standard, wird der Schalter zum Entfernen —
    /// derselbe Knopf schaltet den Standard also an und wieder aus.
    /// </summary>
    private void RefreshModelDefaultState()
    {
        var stored = SelectedModel == null ? null : _modelDefaults.Find(SelectedModel.ModelString);
        HasModelDefault = stored != null;
        HasNoModelDefault = stored == null;
        CanSaveModelDefault = SelectedModel != null && SelectedProfile != null && SelectedWorkMode != null;
        ModelDefaultSummary = stored == null
            ? string.Empty
            : $"★ Standard: {DescribeProfile(stored.ProfileId)} · {DescribeWorkMode(stored.WorkModeId)} · {DescribeThinking(stored.ThinkingValue)}{DescribeCliSuffix(stored.CliTargetId)}";
        ModelDefaultButtonText = MatchesStoredDefault(stored) ? "★ Standard entfernen" : "☆ Standard speichern";
    }

    private bool MatchesStoredDefault(ModelDefaultEntry? stored) =>
        stored != null &&
        stored.ProfileId == SelectedProfile?.Id &&
        stored.WorkModeId == SelectedWorkMode?.Id &&
        string.Equals(stored.ThinkingValue, SelectedThinkingOption?.Value ?? string.Empty, StringComparison.OrdinalIgnoreCase) &&
        // Bei Modellen ohne CLI-Wahl bleibt das Feld leer: dann darf ein alter Eintrag ohne
        // CliTargetId nicht als "abweichend" gelten.
        string.Equals(stored.CliTargetId, CurrentCliTargetId, StringComparison.OrdinalIgnoreCase);

    /// <summary>Zu speichernde Ziel-CLI: leer, wenn das Modell gar keine Wahl anbietet.</summary>
    private string CurrentCliTargetId => HasCliChoice ? SelectedCliTarget?.Id ?? string.Empty : string.Empty;

    private string DescribeCliSuffix(string cliTargetId) =>
        string.IsNullOrWhiteSpace(cliTargetId)
            ? string.Empty
            : " · " + (CliTargets.FirstOrDefault(target => target.Id == cliTargetId)?.DisplayName ?? cliTargetId);

    private string DescribeProfile(string profileId) =>
        Profiles.FirstOrDefault(profile => profile.Id == profileId)?.DisplayName ?? profileId;

    private string DescribeWorkMode(string workModeId) =>
        WorkModes.FirstOrDefault(mode => mode.Id == workModeId)?.DisplayName ?? workModeId;

    private static string DescribeThinking(string thinkingValue) =>
        string.IsNullOrWhiteSpace(thinkingValue) ? "ohne Stufe" : ToThinkingOption(thinkingValue).DisplayName;

    /// <summary>
    /// Speichert die aktuelle Auswahl (Profil, Modus, Effort) als Standard des gewaehlten Modells —
    /// oder entfernt ihn wieder, wenn genau dieser Standard schon gilt. Jedes Modell hat seinen
    /// eigenen Standard; er wird bei jedem Wechsel auf dieses Modell und damit auch beim App-Start
    /// vorausgewaehlt.
    /// </summary>
    [RelayCommand]
    private void ToggleModelDefault()
    {
        var model = SelectedModel;
        if (model == null || SelectedProfile == null || SelectedWorkMode == null)
        {
            StatusText = "Bitte Modell, Profil und Modus wählen.";
            return;
        }

        var key = model.ModelString;
        var stored = _modelDefaults.Find(key);
        if (MatchesStoredDefault(stored))
        {
            _modelDefaults.Remove(key);
            _pendingModelDefault = null;
            StatusText = $"Standard für {model.DisplayName} entfernt.";
            Logger.Instance.Info("MainViewModel", "ToggleModelDefault", "Modell-Standard entfernt", new { key });
        }
        else
        {
            var entry = new ModelDefaultEntry
            {
                ProfileId = SelectedProfile.Id,
                WorkModeId = SelectedWorkMode.Id,
                ThinkingValue = SelectedThinkingOption?.Value ?? string.Empty,
                CliTargetId = CurrentCliTargetId
            };
            _modelDefaults.Save(key, entry);
            _pendingModelDefault = entry;
            StatusText = $"Standard für {model.DisplayName} gespeichert: {SelectedProfile.DisplayName} · {SelectedWorkMode.DisplayName} · {DescribeThinking(entry.ThinkingValue)}{DescribeCliSuffix(entry.CliTargetId)}";
            Logger.Instance.Info("MainViewModel", "ToggleModelDefault", "Modell-Standard gespeichert", new
            {
                key,
                entry.ProfileId,
                entry.WorkModeId,
                entry.ThinkingValue,
                entry.CliTargetId
            });
        }

        RefreshModelDefaultState();
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
        // Das Codex CLI spricht immer direkt mit OpenAI -- dort gibt es keine Provider-Wahl, die
        // Auswahl darf den Start also nicht blockieren.
        if (SelectedModel == null || (SelectedProvider == null && !IsCodexCliSelected))
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
                provider = SelectedProvider?.ProviderName,
                providerSlug = SelectedProvider?.ProviderSlug,
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

            if (IsCodexCliSelected)
            {
                // Codex CLI liest keine Plugin-Modi: Profil UND Modus-Prompt wandern zusammen in die
                // AGENTS.md des Arbeitsverzeichnisses -- dieselbe Profilquelle wie bei OpenCode.
                var agentsPath = _profiles.ActivateCodexProjectAgents(SelectedProfile.Id, SelectedWorkMode.Id, WorkDir);
                _launcher.LaunchCodexCli(SelectedModel, WorkDir, thinkingLevel);
                Logger.Instance.Info("MainViewModel", "Start", "Codex-CLI-Kontext geschrieben", new
                {
                    profile = SelectedProfile.Id,
                    workMode = SelectedWorkMode.Id,
                    agentsPath
                });
                StatusText = string.IsNullOrWhiteSpace(thinkingLevel)
                    ? $"Codex CLI gestartet: {SelectedModel.DisplayName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}"
                    : $"Codex CLI gestartet: {SelectedModel.DisplayName} · Effort {SelectedThinkingOption?.DisplayName} · Profil {SelectedProfile.DisplayName} · Modus {SelectedWorkMode.DisplayName}";
                return;
            }

            // Projekt-AGENTS.md passend zum Profil setzen (Minimal -> nur minimal.md), BEVOR die
            // Session vorbereitet und OpenCode gestartet wird.
            _profiles.ActivateProjectAgents(SelectedProfile.Id, WorkDir);
            var isLmStudio = string.Equals(
                SelectedModel.ProviderId,
                LmStudioService.ProviderId,
                StringComparison.OrdinalIgnoreCase);
            var profileSession = _profiles.PrepareOpenCodeSession(SelectedProfile.Id, WorkDir, isLmStudio);
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
                provider = SelectedProvider?.ProviderName,
                providerSlug = SelectedProvider?.ProviderSlug,
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

    /// <summary>Nur direkte OpenAI-Modelle koennen im Codex CLI laufen (OpenRouter-GPTs nicht:
    /// Codex spricht ausschliesslich mit OpenAI selbst).</summary>
    private static bool IsOpenAiModel(ModelEntry? model) =>
        string.Equals(model?.ProviderId, "openai", StringComparison.OrdinalIgnoreCase);

    private bool IsCodexCliSelected =>
        HasCliChoice && string.Equals(SelectedCliTarget?.Id, "codex", StringComparison.Ordinal);

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
