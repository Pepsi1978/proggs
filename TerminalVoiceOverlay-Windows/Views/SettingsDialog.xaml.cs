using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using Microsoft.Extensions.DependencyInjection;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

public sealed record SettingsEditResult(
    string? GroqApiKey,
    string? GeminiApiKey,
    string SeparatorTemplate,
    bool AutoHide);

public partial class SettingsDialog : Window
{
    public SettingsEditResult? Result { get; private set; }
    private readonly AppSettings _settingsSnapshot;
    private readonly PromptBoardSecretStore _secretStore;

    public SettingsDialog(AppSettings current)
    {
        InitializeComponent();
        _settingsSnapshot = current;
        _secretStore = PromptBoardHost.Get<PromptBoardSecretStore>();

        // Non-secret settings stay in the SQLite DB (they sync via Drive backup).
        GroqKeyBox.Text = current.GroqApiKey ?? string.Empty;
        GeminiKeyBox.Text = current.GeminiApiKey ?? string.Empty;
        SeparatorBox.Text = current.SeparatorTemplate;
        AutoHideCheck.IsChecked = current.AutoHide;

        // Google OAuth credentials live in $HOME/SK/PromptBoard/.env per
        // secrets-in-sk-folder.md — they never enter the Drive backup JSON.
        var secrets = _secretStore.Load();
        GoogleClientIdBox.Text = secrets.GoogleClientId ?? string.Empty;
        GoogleClientSecretBox.Text = secrets.GoogleClientSecret ?? string.Empty;
        UpdateGoogleStatus(secrets.GoogleOAuthRefreshToken, secrets.GoogleAccountEmail);

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click += (_, _) =>
        {
            // Persist Google secrets to the SK file. Caller (PromptBoardPanel)
            // only handles the non-secret half via SettingsEditResult.
            _secretStore.Save(_secretStore.Load() with
            {
                GoogleClientId = NullIfBlank(GoogleClientIdBox.Text),
                GoogleClientSecret = NullIfBlank(GoogleClientSecretBox.Text),
            });

            Result = new SettingsEditResult(
                NullIfBlank(GroqKeyBox.Text),
                NullIfBlank(GeminiKeyBox.Text),
                SeparatorBox.Text ?? " ; ",
                AutoHideCheck.IsChecked == true);
            Close();
        };
        BtnGoogleConnect.Click += async (_, _) => await ConnectGoogleAsync();
        BtnGoogleDisconnect.Click += async (_, _) => await DisconnectGoogleAsync();
    }

    private static string? NullIfBlank(string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s.Trim();

    private void UpdateGoogleStatus(string? refreshToken, string? email)
    {
        if (string.IsNullOrEmpty(refreshToken))
        {
            GoogleStatus.Text = "nicht verbunden";
            GoogleStatus.Foreground = new SolidColorBrush(Color.FromRgb(0xE5, 0x39, 0x35));
        }
        else
        {
            GoogleStatus.Text = string.IsNullOrEmpty(email) ? "verbunden" : $"verbunden ({email})";
            GoogleStatus.Foreground = new SolidColorBrush(Color.FromRgb(0x16, 0xA3, 0x4A));
        }
    }

    private async Task ConnectGoogleAsync()
    {
        var id = NullIfBlank(GoogleClientIdBox.Text);
        var secret = NullIfBlank(GoogleClientSecretBox.Text);
        if (id is null || secret is null)
        {
            MessageBox.Show("Bitte erst Google Client ID und Client Secret eintragen.", "Google Drive",
                MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        try
        {
            // Persist the typed-in client credentials to the SK file first — the
            // OAuth flow reads them from there, not from the DB anymore.
            _secretStore.Save(_secretStore.Load() with
            {
                GoogleClientId = id,
                GoogleClientSecret = secret,
            });

            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();
            BtnGoogleConnect.IsEnabled = false;
            BtnGoogleConnect.Content = "Oeffne Browser...";
            await drive.ConnectAsync();

            // Refresh status from the SK file — ConnectAsync wrote the
            // new refresh token + email there.
            var refreshed = _secretStore.Load();
            UpdateGoogleStatus(refreshed.GoogleOAuthRefreshToken, refreshed.GoogleAccountEmail);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Google-Login fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            BtnGoogleConnect.IsEnabled = true;
            BtnGoogleConnect.Content = "Verbinden";
        }
    }

    private async Task DisconnectGoogleAsync()
    {
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();
            await drive.SignOutAsync();
            UpdateGoogleStatus(null, null);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    public static SettingsEditResult? Ask(Window owner, AppSettings current)
    {
        var dlg = new SettingsDialog(current) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Result;
    }
}
