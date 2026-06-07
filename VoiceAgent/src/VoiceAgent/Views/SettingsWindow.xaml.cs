using System;
using System.Windows;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Audio;

namespace VoiceAgent.Views
{
    /// <summary>
    /// Einstellungs-Fenster: System-Prompt, API-Schluessel (→ SK-Ordner), Modell-Auswahl
    /// und Stimmenauswahl mit Probehoeren. Speichert in AppSettings + keys.json.
    /// </summary>
    public partial class SettingsWindow : Window
    {
        private readonly AppSettings _settings;
        private readonly AudioPlayer _previewPlayer = new();

        public SettingsWindow(AppSettings settings)
        {
            InitializeComponent();
            _settings = settings;
            Loaded += (_, __) => Populate();
        }

        private void Populate()
        {
            try
            {
                PromptBox.Text = string.IsNullOrWhiteSpace(_settings.SystemPrompt)
                    ? BossAgentPrompt.Default
                    : _settings.SystemPrompt;

                GroqKeyBox.Text = Config.ReadApiKey("groq");
                GoogleKeyBox.Text = Config.ReadApiKey("google");
                GeminiKeyBox.Text = Config.ReadApiKey("gemini");
                ClaudeKeyBox.Text = Config.ReadApiKey("claude");
                OpenAiKeyBox.Text = Config.ReadApiKey("openai");

                ProviderBox.Items.Add("gemini");
                ProviderBox.Items.Add("claude");
                ProviderBox.Items.Add("openai");
                ProviderBox.SelectedItem = _settings.LlmProvider;
                if (ProviderBox.SelectedItem == null) ProviderBox.SelectedIndex = 0;

                ModelBox.Text = _settings.LlmModel;
                EndpointModelBox.Text = _settings.EndpointModel;

                VoiceBox.ItemsSource = GoogleTtsVoices.All;
                VoiceBox.DisplayMemberPath = nameof(GoogleTtsVoice.DisplayName);
                VoiceBox.SelectedValuePath = nameof(GoogleTtsVoice.Name);
                VoiceBox.SelectedValue = _settings.TtsVoiceName;
                if (VoiceBox.SelectedValue == null) VoiceBox.SelectedValue = GoogleTtsVoices.DefaultVoiceName;
            }
            catch (Exception ex)
            {
                Log.Error("SettingsWindow: Befuellen fehlgeschlagen", ex);
            }
        }

        private void Save_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                // Prompt: wenn unveraendert == Standard, leer speichern, damit kuenftige
                // Standard-Verbesserungen automatisch greifen.
                var prompt = PromptBox.Text ?? string.Empty;
                _settings.SystemPrompt = prompt.Trim() == BossAgentPrompt.Default.Trim() ? string.Empty : prompt;

                _settings.LlmProvider = ProviderBox.SelectedItem as string ?? "gemini";
                _settings.LlmModel = string.IsNullOrWhiteSpace(ModelBox.Text)
                    ? AppSettings.DefaultGeminiModel
                    : ModelBox.Text.Trim();
                _settings.EndpointModel = string.IsNullOrWhiteSpace(EndpointModelBox.Text)
                    ? AppSettings.DefaultGeminiModel
                    : EndpointModelBox.Text.Trim();
                _settings.TtsVoiceName = VoiceBox.SelectedValue as string ?? GoogleTtsVoices.DefaultVoiceName;

                Config.Save(_settings);
                Config.SaveApiKey("groq", GroqKeyBox.Text?.Trim() ?? "");
                Config.SaveApiKey("google", GoogleKeyBox.Text?.Trim() ?? "");
                Config.SaveApiKey("gemini", GeminiKeyBox.Text?.Trim() ?? "");
                Config.SaveApiKey("claude", ClaudeKeyBox.Text?.Trim() ?? "");
                Config.SaveApiKey("openai", OpenAiKeyBox.Text?.Trim() ?? "");

                Log.Info("SettingsWindow: Einstellungen gespeichert");
                DialogResult = true;
                Close();
            }
            catch (Exception ex)
            {
                Log.Error("SettingsWindow: Speichern fehlgeschlagen", ex);
                MessageBox.Show(this, "Speichern fehlgeschlagen: " + ex.Message, "Fehler",
                    MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void Cancel_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }

        // Event-Handler: async void ist hier ok, Body komplett in try/catch (Almanach §7.2).
        private async void Preview_Click(object sender, RoutedEventArgs e)
        {
            var voice = VoiceBox.SelectedValue as string ?? GoogleTtsVoices.DefaultVoiceName;
            var key = GoogleKeyBox.Text?.Trim() ?? string.Empty;
            try
            {
                PreviewButton.IsEnabled = false;
                var tts = new GoogleTtsClient(key);
                var audio = await tts.SynthesizeAsync("Hallo, so klinge ich.", _settings.TtsLanguageCode, voice);
                await _previewPlayer.PlayAsync(audio);
            }
            catch (Exception ex)
            {
                Log.Error("SettingsWindow: Probehoeren fehlgeschlagen", ex);
                MessageBox.Show(this, "Probehoeren fehlgeschlagen: " + ex.Message, "Fehler",
                    MessageBoxButton.OK, MessageBoxImage.Warning);
            }
            finally
            {
                PreviewButton.IsEnabled = true;
            }
        }
    }
}
