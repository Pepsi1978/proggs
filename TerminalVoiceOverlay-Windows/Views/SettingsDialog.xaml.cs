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
    string? GoogleClientId,
    string? GoogleClientSecret);

public partial class SettingsDialog : Window
{
    public SettingsEditResult? Result { get; private set; }
    private readonly AppSettings _settingsSnapshot;

    public SettingsDialog(AppSettings current)
    {
        InitializeComponent();
        _settingsSnapshot = current;

        GroqKeyBox.Text = current.GroqApiKey ?? string.Empty;
        GeminiKeyBox.Text = current.GeminiApiKey ?? string.Empty;
        SeparatorBox.Text = current.SeparatorTemplate;
        GoogleClientIdBox.Text = current.GoogleClientId ?? string.Empty;
        GoogleClientSecretBox.Text = current.GoogleClientSecret ?? string.Empty;
        UpdateGoogleStatus(current.GoogleOAuthRefreshToken, current.GoogleAccountEmail);

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click += (_, _) =>
        {
            Result = new SettingsEditResult(
                NullIfBlank(GroqKeyBox.Text),
                NullIfBlank(GeminiKeyBox.Text),
                SeparatorBox.Text ?? " ; ",
                NullIfBlank(GoogleClientIdBox.Text),
                NullIfBlank(GoogleClientSecretBox.Text));
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
        // Persist the typed-in client credentials first — the flow needs them.
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
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            _settingsSnapshot.GoogleClientId = id;
            _settingsSnapshot.GoogleClientSecret = secret;
            await repo.UpdateAsync(_settingsSnapshot);

            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();
            BtnGoogleConnect.IsEnabled = false;
            BtnGoogleConnect.Content = "Oeffne Browser...";
            await drive.ConnectAsync();

            // Refetch to get the email.
            var refreshed = await repo.GetAsync();
            UpdateGoogleStatus(refreshed.GoogleOAuthRefreshToken, refreshed.GoogleAccountEmail);
            _settingsSnapshot.GoogleOAuthRefreshToken = refreshed.GoogleOAuthRefreshToken;
            _settingsSnapshot.GoogleAccountEmail = refreshed.GoogleAccountEmail;
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
            _settingsSnapshot.GoogleOAuthRefreshToken = null;
            _settingsSnapshot.GoogleAccountEmail = null;
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
