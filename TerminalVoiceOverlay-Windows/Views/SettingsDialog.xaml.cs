using System;
using System.IO;
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
    bool AutoHide,
    string Orientation,
    bool PersistOverlayPosition);

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
        VocabularyBox.Text = LoadPersonalVocabulary();
        UseVocabularyCheck.IsChecked = LoadVocabularyEnabled();
        PreambleBox.Text = GeminiClient.EffectiveVocabularyPreamble();
        SeparatorBox.Text = current.SeparatorTemplate;
        AutoHideCheck.IsChecked = current.AutoHide;
        HorizontalCheck.IsChecked = string.Equals(current.Orientation, "horizontal",
            StringComparison.OrdinalIgnoreCase);
        PersistPositionCheck.IsChecked = current.PersistOverlayPosition;

        // Google OAuth credentials live in $HOME/SK/PromptBoard/.env per
        // secrets-in-sk-folder.md — they never enter the Drive backup JSON.
        var secrets = _secretStore.Load();
        GoogleClientIdBox.Text = secrets.GoogleClientId ?? string.Empty;
        GoogleClientSecretBox.Text = secrets.GoogleClientSecret ?? string.Empty;
        UpdateGoogleStatus(secrets.GoogleOAuthRefreshToken, secrets.GoogleAccountEmail);

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click += (_, _) =>
        {
            // Persoenliches Vokabular in die SK-Datei schreiben — unabhaengig vom
            // SettingsEditResult, weil GeminiClient die Datei direkt liest.
            SavePersonalVocabulary(VocabularyBox.Text);
            SaveVocabularyEnabled(UseVocabularyCheck.IsChecked == true);
            GeminiClient.SaveVocabularyPreamble(PreambleBox.Text);
            GeminiPromptDriveSync.TryUpload();   // Schalter/Praeambel-Aenderung sofort ins Backup

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
                AutoHideCheck.IsChecked == true,
                HorizontalCheck.IsChecked == true ? "horizontal" : "vertical",
                PersistPositionCheck.IsChecked == true);
            Close();
        };
        BtnEditPrompts.Click += (_, _) => GeminiPromptListDialog.Show(this);
        BtnGoogleConnect.Click += async (_, _) => await ConnectGoogleAsync();
        BtnGoogleDisconnect.Click += async (_, _) => await DisconnectGoogleAsync();
    }

    private static string? NullIfBlank(string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s.Trim();

    // Persoenliches Vokabular fuer die Gemini-Korrektur lebt als Datei im
    // SK-Ordner (gleicher Ort wie die Korrektur-Prompts). Dieses Feld ist nur
    // die bequeme Editier-Oberflaeche; GeminiClient liest die Datei direkt (mit
    // mtime-Cache), darum wirkt eine Aenderung sofort und teilt sich auf einem
    // Rechner automatisch zwischen TVO und CVO.
    private static string PersonalVocabularyPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "SK", "VoiceOverlays", "personal-vocabulary.txt");

    private static string LoadPersonalVocabulary()
    {
        try
        {
            var path = PersonalVocabularyPath;
            return File.Exists(path) ? File.ReadAllText(path) : string.Empty;
        }
        catch { return string.Empty; }
    }

    private static void SavePersonalVocabulary(string? text)
    {
        try
        {
            var path = PersonalVocabularyPath;
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, (text ?? string.Empty).Trim() + "\n");
        }
        catch (Exception ex)
        {
            // Best-effort: das Speichern der uebrigen Settings darf nicht an der
            // Vokabel-Datei scheitern. Fehler zeigen, aber nicht werfen.
            MessageBox.Show($"Woerterbuch konnte nicht gespeichert werden: {ex.Message}",
                "Hinweis", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    // Woerterbuch-Schalter (Frank-Wunsch 2026-06-22): geteilte SK-Datei
    // vocabulary-enabled.txt, gleiche Stelle wie das Woerterbuch selbst. "true"
    // = Woerterbuch wird an Gemini mitgeschickt; alles andere (auch fehlende
    // Datei) = aus. GeminiClient liest dieselbe Datei direkt (mtime-Cache),
    // darum wirkt das Umschalten sofort und teilt sich TVO/CVO auf einem Rechner.
    private static string VocabularyEnabledPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "SK", "VoiceOverlays", "vocabulary-enabled.txt");

    private static bool LoadVocabularyEnabled()
    {
        try
        {
            var path = VocabularyEnabledPath;
            return File.Exists(path) &&
                   string.Equals(File.ReadAllText(path).Trim(), "true", StringComparison.OrdinalIgnoreCase);
        }
        catch { return false; }
    }

    private static void SaveVocabularyEnabled(bool enabled)
    {
        try
        {
            var path = VocabularyEnabledPath;
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, enabled ? "true\n" : "false\n");
        }
        catch { /* best-effort: Schalter darf das Speichern nicht blockieren */ }
    }

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
