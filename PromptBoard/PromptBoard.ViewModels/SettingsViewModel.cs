using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace PromptBoard.ViewModels;

/// <summary>
/// ViewModel for the Settings dialog. Loads the singleton AppSettings
/// row, lets the UI edit its fields, and writes the row back on save.
/// </summary>
public partial class SettingsViewModel : ObservableObject
{
    private readonly IAppSettingsRepository _settings;
    private readonly IGoogleDriveBackupService _drive;
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

    // --- Google Drive properties ---

    [ObservableProperty]
    private string? _googleClientId;

    [ObservableProperty]
    private string? _googleClientSecret;

    [ObservableProperty]
    private string? _googleAccountEmail;

    [ObservableProperty]
    private bool _isGoogleConnected;

    [ObservableProperty]
    private string _googleStatusMessage = string.Empty;

    [ObservableProperty]
    private bool _isGoogleBusy;

    public SettingsViewModel(
        IAppSettingsRepository settings,
        IGoogleDriveBackupService drive,
        ILogger<SettingsViewModel> logger)
    {
        _settings = settings;
        _drive = drive;
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

        GoogleClientId = s.GoogleClientId;
        GoogleClientSecret = s.GoogleClientSecret;
        GoogleAccountEmail = s.GoogleAccountEmail;
        IsGoogleConnected = !string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken);
        GoogleStatusMessage = IsGoogleConnected
            ? $"Verbunden als {GoogleAccountEmail ?? "(E-Mail unbekannt)"}"
            : "Noch nicht verbunden.";
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

        // Persist Google Client credentials (but not refresh token — the
        // service manages that through its own code path).
        s.GoogleClientId = string.IsNullOrWhiteSpace(GoogleClientId) ? null : GoogleClientId.Trim();
        s.GoogleClientSecret = string.IsNullOrWhiteSpace(GoogleClientSecret) ? null : GoogleClientSecret.Trim();

        await _settings.UpdateAsync(s);
        _logger.LogInformation("Settings saved. GroqModel={GroqModel}, BarHeight={H}, AlwaysOnTop={Top}",
            s.GroqModel, s.BarHeight, s.AlwaysOnTop);
    }

    /// <summary>
    /// Runs the OAuth flow. Saves ClientId/Secret first so the service can pick them up.
    /// </summary>
    [RelayCommand]
    private async Task ConnectGoogleAsync(CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(GoogleClientId) || string.IsNullOrWhiteSpace(GoogleClientSecret))
        {
            GoogleStatusMessage = "Bitte zuerst Client-ID und Client-Secret eintragen.";
            return;
        }

        IsGoogleBusy = true;
        GoogleStatusMessage = "Oeffne Browser fuer die Google-Anmeldung...";
        try
        {
            // Persist credentials so GoogleDriveBackupService can read them.
            AppSettings s = await _settings.GetAsync(ct);
            s.GoogleClientId = GoogleClientId.Trim();
            s.GoogleClientSecret = GoogleClientSecret.Trim();
            await _settings.UpdateAsync(s, ct);

            await _drive.ConnectAsync(ct);

            // Reload visible fields.
            s = await _settings.GetAsync(ct);
            GoogleAccountEmail = s.GoogleAccountEmail;
            IsGoogleConnected = !string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken);
            GoogleStatusMessage = IsGoogleConnected
                ? $"Verbunden als {GoogleAccountEmail ?? "(E-Mail unbekannt)"}"
                : "Verbindung fehlgeschlagen.";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Google Drive connect failed.");
            GoogleStatusMessage = $"Fehler: {ex.Message}";
            IsGoogleConnected = false;
        }
        finally
        {
            IsGoogleBusy = false;
        }
    }

    [RelayCommand]
    private async Task DisconnectGoogleAsync(CancellationToken ct)
    {
        IsGoogleBusy = true;
        try
        {
            await _drive.SignOutAsync(ct);
            GoogleAccountEmail = null;
            IsGoogleConnected = false;
            GoogleStatusMessage = "Verbindung getrennt.";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Google Drive disconnect failed.");
            GoogleStatusMessage = $"Fehler beim Trennen: {ex.Message}";
        }
        finally
        {
            IsGoogleBusy = false;
        }
    }
}
