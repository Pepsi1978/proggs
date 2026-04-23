using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.ViewModels;

/// <summary>
/// ViewModel for the Settings dialog. Loads the singleton AppSettings
/// row, lets the UI edit its fields, and writes the row back on save.
/// </summary>
public partial class SettingsViewModel : ObservableObject
{
    private readonly IAppSettingsRepository _settings;
    private readonly ILogger<SettingsViewModel> _logger;

    public static IReadOnlyList<string> GroqModels { get; } =
    [
        "whisper-large-v3-turbo",
        "whisper-large-v3",
    ];

    /// <summary>Suggested Gemini model ids. User can still edit freely.</summary>
    public ObservableCollection<string> GeminiModels { get; } =
    [
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-pro",
    ];

    [ObservableProperty]
    private string? _groqApiKey;

    [ObservableProperty]
    private string? _geminiApiKey;

    [ObservableProperty]
    private string _groqModel = "whisper-large-v3-turbo";

    [ObservableProperty]
    private double _barHeight = 200;

    [ObservableProperty]
    private string _separatorTemplate = "\n\n;\n\n";

    [ObservableProperty]
    private bool _alwaysOnTop = true;

    public SettingsViewModel(
        IAppSettingsRepository settings,
        ILogger<SettingsViewModel> logger)
    {
        _settings = settings;
        _logger = logger;
    }

    public async Task LoadAsync()
    {
        AppSettings s = await _settings.GetAsync();
        GroqApiKey = s.GroqApiKey;
        GeminiApiKey = s.GeminiApiKey;
        GroqModel = s.GroqModel;
        BarHeight = s.BarHeight;
        SeparatorTemplate = s.SeparatorTemplate;
        AlwaysOnTop = s.AlwaysOnTop;
    }

    public async Task SaveAsync()
    {
        AppSettings s = await _settings.GetAsync();
        s.GroqApiKey = string.IsNullOrWhiteSpace(GroqApiKey) ? null : GroqApiKey.Trim();
        s.GeminiApiKey = string.IsNullOrWhiteSpace(GeminiApiKey) ? null : GeminiApiKey.Trim();
        s.GroqModel = string.IsNullOrWhiteSpace(GroqModel) ? "whisper-large-v3-turbo" : GroqModel.Trim();
        s.BarHeight = BarHeight <= 0 ? 200 : BarHeight;
        s.SeparatorTemplate = string.IsNullOrEmpty(SeparatorTemplate) ? "\n\n;\n\n" : SeparatorTemplate;
        s.AlwaysOnTop = AlwaysOnTop;

        await _settings.UpdateAsync(s);
        _logger.LogInformation("Settings saved. GroqModel={GroqModel}, BarHeight={H}, AlwaysOnTop={Top}",
            s.GroqModel, s.BarHeight, s.AlwaysOnTop);
    }
}
