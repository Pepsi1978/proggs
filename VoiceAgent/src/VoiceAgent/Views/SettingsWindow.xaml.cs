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
        private bool _ready;   // true erst NACH Populate — verhindert NullRef in ValueChanged waehrend XAML-Load

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

                SemanticEndpointBox.IsChecked = _settings.SemanticEndpointing;
                IntentDetectionBox.IsChecked = _settings.IntentDetection;
                SilenceThresholdSlider.Value = _settings.SilenceThreshold;
                SilenceMsSlider.Value = _settings.SilenceMs;
                MinUtteranceSlider.Value = _settings.MinUtteranceMs;
                EndpointWaitSlider.Value = _settings.EndpointMaxWaitMs;
                _ready = true;            // ab jetzt existieren ALLE Controls -> Label-Updates erlaubt
                UpdateSliderLabels();
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

                _settings.SemanticEndpointing = SemanticEndpointBox.IsChecked == true;
                _settings.IntentDetection = IntentDetectionBox.IsChecked == true;
                _settings.SilenceThreshold = Math.Round(SilenceThresholdSlider.Value, 3);
                _settings.SilenceMs = (int)SilenceMsSlider.Value;
                _settings.MinUtteranceMs = (int)MinUtteranceSlider.Value;
                _settings.EndpointMaxWaitMs = (int)EndpointWaitSlider.Value;

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

        private void Slider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e) => UpdateSliderLabels();

        /// <summary>Aktualisiert die Live-Wertanzeige neben den Empfindlichkeits-Schiebereglern.</summary>
        private void UpdateSliderLabels()
        {
            // ValueChanged feuert schon beim XAML-Laden (Slider.Minimum -> Wert-Coercion), wenn die
            // weiter unten im XAML stehenden Label/Slider-Controls noch gar nicht existieren.
            // Erst nach Populate (alle Controls da) ausfuehren — sonst NullReferenceException.
            if (!_ready) return;
            SilenceThresholdLabel.Text = $"Stille-Schwelle (Empfindlichkeit): {SilenceThresholdSlider.Value:F3} — kleiner = empfindlicher";
            SilenceMsLabel.Text = $"Pause bis eine Aussage endet: {SilenceMsSlider.Value:F0} ms";
            MinUtteranceLabel.Text = $"Kuerzeste erkannte Aussage: {MinUtteranceSlider.Value:F0} ms";
            EndpointWaitLabel.Text = $"Sicherheitsnetz nach langer Pause: {EndpointWaitSlider.Value:F0} ms";
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
