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
    string Orientation);

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

        // API keys may still live exclusively in the central SK .env. Show the
        // effective values instead of making a working configuration look empty.
        // A value prefilled from .env is only a display hint: it is saved to the DB
        // (which takes precedence) ONLY if the user changes it — otherwise a later
        // key swap in the .env would silently stop working.
        Config? fileConfig = null;
        try { fileConfig = Config.Load(); } catch { }
        _groqEnvPrefill = PrefillFromEnv(GroqKeyBox, current.GroqApiKey, fileConfig?.GroqApiKey);
        _geminiEnvPrefill = PrefillFromEnv(GeminiKeyBox, current.GeminiApiKey, fileConfig?.GeminiApiKey);
        // Transkriptions-Modell: Groq (0), Gemini Transcribe (1), Gemini Live (2).
        // Lebt als SK-Datei, deshalb ausserhalb des SettingsEditResult.
        TranscriptionEngineBox.SelectedIndex = TranscriptionEngineSetting.Current switch
        {
            TranscriptionEngineSetting.Gemini => 1,
            TranscriptionEngineSetting.GeminiLive => 2,
            TranscriptionEngineSetting.Parakeet => 3,
            _ => 0,
        };
        UpdateParakeetButton();
        ParakeetDownloadButton.Click += async (_, _) =>
        {
            if (_parakeetDownloading || ParakeetLocalClient.IsInstalled) return;
            _parakeetDownloading = true;
            ParakeetDownloadButton.IsEnabled = false;
            ParakeetDownloadButton.Content = "Lädt … 0 %";
            var progress = new Progress<(long Done, long Total)>(p =>
                ParakeetDownloadButton.Content = $"Lädt … {p.Done * 100 / p.Total} %");
            try
            {
                await ParakeetLocalClient.DownloadAsync(progress);
                _parakeetDownloading = false;
                UpdateParakeetButton();
            }
            catch (Exception ex)
            {
                _parakeetDownloading = false;
                DiagLog.Warn("Settings", "parakeet_download_failed", ("error", ex.Message));
                ParakeetDownloadButton.IsEnabled = true;
                ParakeetDownloadButton.Content = "Fehler — erneut laden";
            }
        };
        var groqModelInitial = (GroqModelSetting.Current ?? fileConfig?.WhisperModel) == GroqModelSetting.LargeV3 ? 1 : 0;
        GroqModelBox.SelectedIndex = groqModelInitial;
        TranscriptionModeBox.SelectedIndex = TranscriptionModeSetting.UseSmart ? 1 : 0;
        VocabularyBox.Text = LoadPersonalVocabulary();
        UseVocabularyCheck.IsChecked = LoadVocabularyEnabled();
        PreambleBox.Text = GeminiClient.EffectiveVocabularyPreamble();
        SeparatorBox.Text = current.SeparatorTemplate;
        AutoHideCheck.IsChecked = current.AutoHide;
        HorizontalCheck.IsChecked = string.Equals(current.Orientation, "horizontal",
            StringComparison.OrdinalIgnoreCase);

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
            TranscriptionEngineSetting.Save(TranscriptionEngineBox.SelectedIndex switch
            {
                1 => TranscriptionEngineSetting.Gemini,
                2 => TranscriptionEngineSetting.GeminiLive,
                3 => TranscriptionEngineSetting.Parakeet,
                _ => TranscriptionEngineSetting.Groq,
            });
            if (TranscriptionEngineBox.SelectedIndex == 3) ParakeetLocalClient.Shared.Preload();
            // Nur bei echter Aenderung schreiben, sonst ueberstimmt die Datei ein WHISPER_MODEL aus der .env.
            if (GroqModelBox.SelectedIndex != groqModelInitial)
                GroqModelSetting.Save(GroqModelBox.SelectedIndex == 1
                    ? GroqModelSetting.LargeV3
                    : GroqModelSetting.Turbo);
            TranscriptionModeSetting.Save(TranscriptionModeBox.SelectedIndex == 1
                ? TranscriptionModeSetting.Smart
                : TranscriptionModeSetting.Verbatim);
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
                KeyToPersist(GroqKeyBox.Text, _groqEnvPrefill),
                KeyToPersist(GeminiKeyBox.Text, _geminiEnvPrefill),
                SeparatorBox.Text ?? " ; ",
                AutoHideCheck.IsChecked == true,
                HorizontalCheck.IsChecked == true ? "horizontal" : "vertical");
            Close();
        };
        BtnGoogleConnect.Click += async (_, _) => await ConnectGoogleAsync();
        BtnGoogleDisconnect.Click += async (_, _) => await DisconnectGoogleAsync();
    }

    private static string? NullIfBlank(string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s.Trim();

    /// <summary>The .env value shown in a key box because the DB had none
    /// (null when the box shows the DB value).</summary>
    /// <summary>Laeuft der Download des Offline-Modells gerade (gilt ueber das Schliessen des Dialogs hinaus)?</summary>
    private static bool _parakeetDownloading;

    private void UpdateParakeetButton()
    {
        if (_parakeetDownloading) { ParakeetDownloadButton.IsEnabled = false; ParakeetDownloadButton.Content = "Lädt …"; return; }
        var installed = ParakeetLocalClient.IsInstalled;
        ParakeetDownloadButton.IsEnabled = !installed;
        ParakeetDownloadButton.Content = installed
            ? "Offline-Modell bereit"
            : $"Offline-Modell laden ({ParakeetLocalClient.TotalBytes / 1_048_576} MB)";
    }

    private readonly string? _groqEnvPrefill;
    private readonly string? _geminiEnvPrefill;

    /// <summary>
    /// Fills <paramref name="box"/> with the DB key, or — when the DB has none —
    /// with the .env key as a marked hint. Returns the .env value used as prefill.
    /// </summary>
    private static string? PrefillFromEnv(System.Windows.Controls.TextBox box, string? dbValue, string? envValue)
    {
        if (!string.IsNullOrWhiteSpace(dbValue))
        {
            box.Text = dbValue;
            return null;
        }
        var env = NullIfBlank(envValue);
        box.Text = env ?? string.Empty;
        if (env is not null)
        {
            box.ToolTip = "Wert aus der .env (SK\\VoiceOverlays) — wird nur gespeichert, wenn du ihn aenderst. " +
                          "Unveraendert gilt weiter die .env.";
            var normalForeground = box.Foreground;
            box.Foreground = new SolidColorBrush(Color.FromRgb(0x9E, 0x9E, 0x9E));
            box.TextChanged += (_, _) => box.Foreground = normalForeground;
        }
        return env;
    }

    /// <summary>Unchanged .env prefill → null (DB stays empty, .env keeps winning).</summary>
    private static string? KeyToPersist(string? boxText, string? envPrefill)
    {
        var value = NullIfBlank(boxText);
        if (envPrefill is not null && string.Equals(value, envPrefill, StringComparison.Ordinal))
            return null;
        return value;
    }

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
            using var _ = CrossProcessLock.Acquire(SharedLockNames.Vocabulary, TimeSpan.FromSeconds(5), throwOnTimeout: false);
            AtomicFile.WriteAllText(path, (text ?? string.Empty).Trim() + "\n");
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
