using System;
using System.Windows;
using System.Windows.Controls;
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
        private readonly string _originalTheme;   // fuer "Abbrechen": Live-Vorschau zuruecksetzen

        public SettingsWindow(AppSettings settings)
        {
            InitializeComponent();
            _settings = settings;
            _originalTheme = settings.Theme;
            Loaded += (_, __) => Populate();
            // Titelleiste passend zum aktiven Theme (Almanach §2.7 — erst bei vorhandenem HWND).
            SourceInitialized += (_, __) => ThemeManager.ApplyTitleBar(this);
        }

        private void Populate()
        {
            try
            {
                // Erscheinungsbild: Anzeige-Text + interner Wert (Tag) je Eintrag.
                ThemeBox.Items.Clear();
                ThemeBox.Items.Add(new ComboBoxItem { Content = "Hell", Tag = "light" });
                ThemeBox.Items.Add(new ComboBoxItem { Content = "Dunkel", Tag = "dark" });
                ThemeBox.Items.Add(new ComboBoxItem { Content = "Automatisch (Windows)", Tag = "system" });
                string curTheme = string.IsNullOrWhiteSpace(_settings.Theme) ? "light" : _settings.Theme.Trim().ToLowerInvariant();
                foreach (ComboBoxItem it in ThemeBox.Items)
                    if ((string)it.Tag! == curTheme) { ThemeBox.SelectedItem = it; break; }
                if (ThemeBox.SelectedItem == null) ThemeBox.SelectedIndex = 0;

                MinimizeToTrayBox.IsChecked = _settings.MinimizeToTray;
                // Autostart: Wahrheit ist die Registry (HKCU\...\Run), nicht settings.json — direkt lesen.
                AutostartBox.IsChecked = AutostartManager.IsEnabled();

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

                WakeWordEnabledBox.IsChecked = _settings.WakeWordEnabled;
                WakeWordBox.Text = _settings.WakeWord;
                WakeChimeBox.IsChecked = _settings.WakeChimeEnabled;
                WakeSleepChimeBox.IsChecked = _settings.WakeSleepChimeEnabled;
                WakeTimeoutSlider.Value = _settings.WakeTimeoutMs;

                TimeZoneBox.ItemsSource = TimeZoneInfo.GetSystemTimeZones();
                TimeZoneBox.DisplayMemberPath = nameof(TimeZoneInfo.DisplayName);
                TimeZoneBox.SelectedValuePath = nameof(TimeZoneInfo.Id);
                bool autoTz = string.IsNullOrWhiteSpace(_settings.TimeZoneId);
                AutoTimeZoneBox.IsChecked = autoTz;
                TimeZoneBox.SelectedValue = autoTz ? TimeZoneInfo.Local.Id : _settings.TimeZoneId;
                TimeZoneBox.IsEnabled = !autoTz;

                UserColorBox.ItemsSource = ColorPalette.All;
                UserColorBox.DisplayMemberPath = nameof(ColorChoice.Name);
                UserColorBox.SelectedValuePath = nameof(ColorChoice.Hex);
                UserColorBox.SelectedValue = _settings.UserColor;
                if (UserColorBox.SelectedValue == null) UserColorBox.SelectedIndex = 0;
                AgentColorBox.ItemsSource = ColorPalette.All;
                AgentColorBox.DisplayMemberPath = nameof(ColorChoice.Name);
                AgentColorBox.SelectedValuePath = nameof(ColorChoice.Hex);
                AgentColorBox.SelectedValue = _settings.AgentColor;
                if (AgentColorBox.SelectedValue == null) AgentColorBox.SelectedIndex = 0;

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

                _settings.WakeWordEnabled = WakeWordEnabledBox.IsChecked == true;
                _settings.WakeWord = string.IsNullOrWhiteSpace(WakeWordBox.Text) ? "Okay Computer" : WakeWordBox.Text.Trim();
                _settings.WakeChimeEnabled = WakeChimeBox.IsChecked == true;
                _settings.WakeSleepChimeEnabled = WakeSleepChimeBox.IsChecked == true;
                _settings.WakeTimeoutMs = (int)WakeTimeoutSlider.Value;
                _settings.TimeZoneId = AutoTimeZoneBox.IsChecked == true
                    ? string.Empty
                    : (TimeZoneBox.SelectedValue as string ?? string.Empty);
                _settings.UserColor = UserColorBox.SelectedValue as string ?? "#4FC3F7";
                _settings.AgentColor = AgentColorBox.SelectedValue as string ?? "#F97316";
                _settings.Theme = (ThemeBox.SelectedItem as ComboBoxItem)?.Tag as string ?? "light";
                _settings.MinimizeToTray = MinimizeToTrayBox.IsChecked == true;

                Config.Save(_settings);
                // Windows-Autostart direkt in die Registry schreiben (Single Source of Truth).
                AutostartManager.SetEnabled(AutostartBox.IsChecked == true);
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
            // Live-Vorschau des Themes verwerfen — auf den Stand beim Oeffnen zuruecksetzen.
            try { ThemeManager.Apply(_originalTheme); } catch (Exception ex) { Log.Error("Theme zuruecksetzen fehlgeschlagen", ex); }
            DialogResult = false;
            Close();
        }

        private void Slider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e) => UpdateSliderLabels();

        /// <summary>Live-Vorschau: das gewaehlte Profil sofort anwenden (Guard gegen XAML-Load-Feuern, §2.10).</summary>
        private void ThemeBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (!_ready) return;
            var tag = (ThemeBox.SelectedItem as ComboBoxItem)?.Tag as string;
            if (!string.IsNullOrEmpty(tag))
            {
                try { ThemeManager.Apply(tag); } catch (Exception ex) { Log.Error("Theme-Vorschau anwenden fehlgeschlagen", ex); }
            }
        }

        // Auto-Zeitzone an/aus: die manuelle Auswahl ist nur bei abgeschaltetem "Automatisch" aktiv.
        private void AutoTimeZone_Click(object sender, RoutedEventArgs e)
            => TimeZoneBox.IsEnabled = AutoTimeZoneBox.IsChecked != true;

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
            WakeTimeoutLabel.Text = $"Wachfenster nach dem Wecken: {WakeTimeoutSlider.Value / 1000:F0} s";
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
