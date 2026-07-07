using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Audio;
using VoiceAgent.Services.Llm;

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
        private readonly Chime _chimePreview = new();   // spielt den ausgewaehlten Start-/Stop-Ton zum Probehoeren
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
            // Vorhoer-Wiedergabe beim Schliessen stoppen (WaveOut-Handle freigeben, Almanach §9.4).
            Closed += (_, __) => { try { _chimePreview.Stop(); _previewPlayer.Stop(); } catch { /* unkritisch */ } };
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
                ProviderBox.Items.Add("codex");
                ProviderBox.SelectedItem = _settings.LlmProvider;
                if (ProviderBox.SelectedItem == null) ProviderBox.SelectedIndex = 0;

                ModelBox.Text = _settings.LlmModel;

                // Modelle pro Rolle: zeigt die EFFEKTIV aufgeloesten Werte (gesetzte Rolle oder Vorgabe),
                // damit Frank immer sieht, welches Gehirn wo arbeitet — und es pro Rolle umstellen kann.
                DeepUnderstandingBox.IsChecked = _settings.DeepUnderstanding;
                PopulateRoleRow(BrainProviderBox, BrainModelBox, ModelRole.Brain);
                PopulateRoleRow(BuilderProviderBox, BuilderModelBox, ModelRole.Builder);
                PopulateRoleRow(CuProviderBox, CuModelBox, ModelRole.ComputerUse);
                PopulateRoleRow(EndpointProviderBox, EndpointModelBox, ModelRole.Endpoint);
                PopulateRoleRow(IntentProviderBox, IntentModelBox, ModelRole.Intent);

                // Codex: Effort-Auswahl (volle Stufen) + Anmeldestatus.
                CodexEffortBox.Items.Clear();
                CodexEffortBox.Items.Add("low");
                CodexEffortBox.Items.Add("medium");
                CodexEffortBox.Items.Add("high");
                CodexEffortBox.SelectedItem = CodexProvider.NormalizeEffort(_settings.CodexEffort);
                if (CodexEffortBox.SelectedItem == null) CodexEffortBox.SelectedItem = "medium";
                RefreshCodexStatus();

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
                // Editierbare Auswahlliste: kuratierte Vorschlaege im Dropdown, eigener Text moeglich.
                WakeWordBox.ItemsSource = WakeWords.Names;
                WakeWordBox.Text = WakeWords.Canonical(_settings.WakeWord);   // Vorschlag ODER frei eingetippt
                WakeChimeBox.IsChecked = _settings.WakeChimeEnabled;
                WakeSleepChimeBox.IsChecked = _settings.WakeSleepChimeEnabled;

                // Ton-Auswahl: je 30 kurze Toene aus assets/sounds/* + Standard voran (ChimeLibrary).
                StartSoundBox.ItemsSource = ChimeLibrary.StartSounds();
                StartSoundBox.DisplayMemberPath = nameof(ChimeSound.DisplayName);
                StartSoundBox.SelectedValuePath = nameof(ChimeSound.RelativePath);
                StartSoundBox.SelectedValue = _settings.WakeChimeSound;
                if (StartSoundBox.SelectedValue == null) StartSoundBox.SelectedIndex = 0;
                StopSoundBox.ItemsSource = ChimeLibrary.StopSounds();
                StopSoundBox.DisplayMemberPath = nameof(ChimeSound.DisplayName);
                StopSoundBox.SelectedValuePath = nameof(ChimeSound.RelativePath);
                StopSoundBox.SelectedValue = _settings.WakeSleepChimeSound;
                if (StopSoundBox.SelectedValue == null) StopSoundBox.SelectedIndex = 0;
                MessageSoundBox.ItemsSource = ChimeLibrary.MessageSounds();
                MessageSoundBox.DisplayMemberPath = nameof(ChimeSound.DisplayName);
                MessageSoundBox.SelectedValuePath = nameof(ChimeSound.RelativePath);
                MessageSoundBox.SelectedValue = _settings.ChimeSound;
                if (MessageSoundBox.SelectedValue == null) MessageSoundBox.SelectedIndex = 0;

                WakeTimeoutSlider.Value = _settings.WakeTimeoutMs;

                // Computer Use: Stufe Aus/Sicher/Vollzugriff. Tag = Roh-Wert fuer CommandGuard.ParseMode.
                ComputerUseModeBox.Items.Clear();
                ComputerUseModeBox.Items.Add(new ComboBoxItem { Content = "Aus — steuert den Rechner nicht", Tag = "off" });
                ComputerUseModeBox.Items.Add(new ComboBoxItem { Content = "Sicher — heikle Befehle erst nach „Ja“", Tag = "safe" });
                ComputerUseModeBox.Items.Add(new ComboBoxItem { Content = "Vollzugriff — direkt (außer Sperrliste)", Tag = "full" });
                string curCu = CommandGuard.ParseMode(_settings.ComputerUseMode) switch
                {
                    Core.ComputerUseMode.Safe => "safe",
                    Core.ComputerUseMode.Full => "full",
                    _ => "off",
                };
                foreach (ComboBoxItem it in ComputerUseModeBox.Items)
                    if ((string)it.Tag! == curCu) { ComputerUseModeBox.SelectedItem = it; break; }
                if (ComputerUseModeBox.SelectedItem == null) ComputerUseModeBox.SelectedIndex = 0;

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
                _ = InitModelDropdownsAsync();   // Modell-Dropdowns live befuellen (behaelt gespeicherte Auswahl)
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
                // Weckwort (editierbar: Vorschlag ODER eigener Text) ZUERST pruefen, bevor etwas
                // gespeichert wird: bei aktivem Weckwort-Modus muss das (englische) Modell es erkennen
                // koennen — sonst klare Meldung statt stillem Ausfall.
                var wakeWordInput = (WakeWordBox.Text ?? string.Empty).Trim();
                if (string.IsNullOrWhiteSpace(wakeWordInput)) wakeWordInput = WakeWords.Default;
                if (WakeWordEnabledBox.IsChecked == true)
                {
                    string wakeModelDir = System.IO.Path.Combine(AppContext.BaseDirectory, "assets", "wakeword-model");
                    if (!WakeWords.CanRecognize(wakeWordInput, wakeModelDir))
                    {
                        MessageBox.Show(this,
                            $"Das Weckwort „{wakeWordInput}“ kann das (englische) Modell nicht erkennen.\n\n" +
                            "Bitte ein englisch klingendes Wort ohne Umlaute/Sonderzeichen wählen (z. B. „Hey Buddy“).",
                            "Weckwort nicht erkennbar", MessageBoxButton.OK, MessageBoxImage.Warning);
                        return;   // nicht speichern, Fenster offen lassen
                    }
                }

                // Prompt: wenn unveraendert == Standard, leer speichern, damit kuenftige
                // Standard-Verbesserungen automatisch greifen.
                var prompt = PromptBox.Text ?? string.Empty;
                _settings.SystemPrompt = prompt.Trim() == BossAgentPrompt.Default.Trim() ? string.Empty : prompt;

                _settings.LlmProvider = ProviderBox.SelectedItem as string ?? "gemini";
                _settings.LlmModel = string.IsNullOrWhiteSpace(ModelBox.Text)
                    ? AppSettings.DefaultGeminiModel
                    : ModelBox.Text.Trim();
                _settings.CodexEffort = CodexProvider.NormalizeEffort(CodexEffortBox.SelectedItem as string);

                // Modelle pro Rolle speichern. Die Legacy-Felder (EndpointModel/IntentModel) bleiben
                // synchron, damit alte Fallback-Pfade konsistent sind (funktionserhaltend).
                _settings.DeepUnderstanding = DeepUnderstandingBox.IsChecked == true;
                _settings.RoleBrain = ReadRole(BrainProviderBox, BrainModelBox);
                _settings.RoleBuilder = ReadRole(BuilderProviderBox, BuilderModelBox);
                _settings.RoleComputerUse = ReadRole(CuProviderBox, CuModelBox);
                _settings.RoleEndpoint = ReadRole(EndpointProviderBox, EndpointModelBox);
                _settings.RoleIntent = ReadRole(IntentProviderBox, IntentModelBox);
                if (_settings.RoleEndpoint.Provider == "gemini" && _settings.RoleEndpoint.IsSet)
                    _settings.EndpointModel = _settings.RoleEndpoint.Model;
                if (_settings.RoleIntent.Provider == "gemini" && _settings.RoleIntent.IsSet)
                    _settings.IntentModel = _settings.RoleIntent.Model;
                Log.Info("CHECKPOINT Modelle pro Rolle gespeichert", new
                {
                    step = "Rollen-Modelle speichern",
                    intent = "jede Rolle bekommt das von Frank gewaehlte Gehirn",
                    brain = $"{_settings.RoleBrain.Provider}/{_settings.RoleBrain.Model}",
                    builder = $"{_settings.RoleBuilder.Provider}/{_settings.RoleBuilder.Model}",
                    computerUse = $"{_settings.RoleComputerUse.Provider}/{_settings.RoleComputerUse.Model}",
                    endpoint = $"{_settings.RoleEndpoint.Provider}/{_settings.RoleEndpoint.Model}",
                    intentRole = $"{_settings.RoleIntent.Provider}/{_settings.RoleIntent.Model}",
                    deepUnderstanding = _settings.DeepUnderstanding,
                    ok = true
                });
                _settings.TtsVoiceName = VoiceBox.SelectedValue as string ?? GoogleTtsVoices.DefaultVoiceName;

                _settings.SemanticEndpointing = SemanticEndpointBox.IsChecked == true;
                _settings.IntentDetection = IntentDetectionBox.IsChecked == true;
                _settings.SilenceThreshold = Math.Round(SilenceThresholdSlider.Value, 3);
                _settings.SilenceMs = (int)SilenceMsSlider.Value;
                _settings.MinUtteranceMs = (int)MinUtteranceSlider.Value;
                _settings.EndpointMaxWaitMs = (int)EndpointWaitSlider.Value;

                _settings.WakeWordEnabled = WakeWordEnabledBox.IsChecked == true;
                _settings.WakeWord = WakeWords.Canonical(wakeWordInput);   // kuratiert kanonisch, frei getrimmt durchgereicht
                _settings.WakeChimeEnabled = WakeChimeBox.IsChecked == true;
                _settings.WakeSleepChimeEnabled = WakeSleepChimeBox.IsChecked == true;
                _settings.WakeChimeSound = StartSoundBox.SelectedValue as string ?? "wakeword.wav";
                _settings.WakeSleepChimeSound = StopSoundBox.SelectedValue as string ?? "sleep.wav";
                _settings.ChimeSound = MessageSoundBox.SelectedValue as string ?? "message1.wav";
                _settings.WakeTimeoutMs = (int)WakeTimeoutSlider.Value;
                _settings.ComputerUseMode = (ComputerUseModeBox.SelectedItem as ComboBoxItem)?.Tag as string ?? "off";
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

        // Start-Ton probehoeren. Event-Handler: Body komplett in try/catch (Almanach §7.2).
        private void PreviewStartSound_Click(object sender, RoutedEventArgs e)
            => PreviewChime(StartSoundBox.SelectedValue as string, "Start");

        // Stop-Ton probehoeren.
        private void PreviewStopSound_Click(object sender, RoutedEventArgs e)
            => PreviewChime(StopSoundBox.SelectedValue as string, "Stop");

        // Melde-Ton (proaktiver Funkspruch) probehoeren.
        private void PreviewMessageSound_Click(object sender, RoutedEventArgs e)
            => PreviewChime(MessageSoundBox.SelectedValue as string, "Melde");

        private void PreviewChime(string? relativePath, string which)
        {
            if (string.IsNullOrWhiteSpace(relativePath)) return;
            try
            {
                _chimePreview.FileName = relativePath;
                _chimePreview.Play();
                // Observability-Checkpoint (Intent-Verifikation): was wurde vorgehoert?
                Log.Info("CHECKPOINT: Ton-Vorhoeren", new { step = "Ton vorhoeren", which, file = relativePath, ok = true });
            }
            catch (Exception ex)
            {
                Log.Error("SettingsWindow: Ton-Vorhoeren fehlgeschlagen", ex);
            }
        }

        /// <summary>
        /// Bei Anbieter-Wechsel die Modell-Liste des neuen Anbieters laden und ein GUELTIGES Modell
        /// waehlen — verhindert Modell/Anbieter-Mismatch (war Ursache des Codex-400).
        /// Guard gegen XAML-Load-Feuern (§2.10).
        /// </summary>
        private void ProviderBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (!_ready) return;
            _ = FillMainModelsAsync(forceValid: true);
        }

        /// <summary>Beim Oeffnen: alle Modell-Dropdowns live befuellen, gespeicherte Auswahl behalten.</summary>
        private async System.Threading.Tasks.Task InitModelDropdownsAsync()
        {
            await FillMainModelsAsync(forceValid: false).ConfigureAwait(true);
            // Rollen-Zeilen nacheinander (schont die Anbieter-APIs; Fehler je Zeile unkritisch).
            await FillRoleModelsAsync(BrainProviderBox, BrainModelBox, forceValid: false).ConfigureAwait(true);
            await FillRoleModelsAsync(BuilderProviderBox, BuilderModelBox, forceValid: false).ConfigureAwait(true);
            await FillRoleModelsAsync(CuProviderBox, CuModelBox, forceValid: false).ConfigureAwait(true);
            await FillRoleModelsAsync(EndpointProviderBox, EndpointModelBox, forceValid: false).ConfigureAwait(true);
            await FillRoleModelsAsync(IntentProviderBox, IntentModelBox, forceValid: false).ConfigureAwait(true);
        }

        // ---------- Modelle pro Rolle (Boss-Agent-Ueberarbeitung Phase 1) ----------

        private static readonly string[] Providers = { "gemini", "claude", "openai", "codex" };

        /// <summary>
        /// Befuellt eine Rollen-Zeile (Anbieter + Modell) mit dem EFFEKTIV aufgeloesten Wert
        /// (gesetzte Rolle oder eingebaute Vorgabe) und verdrahtet den Anbieter-Wechsel.
        /// </summary>
        private void PopulateRoleRow(ComboBox providerBox, ComboBox modelBox, ModelRole role)
        {
            providerBox.Items.Clear();
            foreach (var p in Providers) providerBox.Items.Add(p);
            var (prov, model) = LlmProviderFactory.ResolveRole(_settings, role);
            providerBox.SelectedItem = prov;
            if (providerBox.SelectedItem == null) providerBox.SelectedIndex = 0;
            modelBox.Text = model;
            // Anbieter-Wechsel: Modell-Liste neu laden + gueltiges Modell waehlen (Guard gegen XAML-Load, §2.10).
            providerBox.SelectionChanged += (_, __) =>
            {
                if (_ready) _ = FillRoleModelsAsync(providerBox, modelBox, forceValid: true);
            };
        }

        /// <summary>Modell-Liste einer Rollen-Zeile zum gewaehlten Anbieter laden (live + Fallback).</summary>
        private async System.Threading.Tasks.Task FillRoleModelsAsync(ComboBox providerBox, ComboBox modelBox, bool forceValid)
        {
            try
            {
                var provider = providerBox.SelectedItem as string ?? "gemini";
                var current = (modelBox.Text ?? string.Empty).Trim();
                var list = await ModelCatalog.GetModelsAsync(provider).ConfigureAwait(true);
                modelBox.ItemsSource = list;
                if (ListContains(list, current)) modelBox.Text = current;          // gueltig -> behalten
                else if (forceValid && list.Count > 0) modelBox.Text = list[0];    // Wechsel -> gueltiges Default
                else modelBox.Text = current;                                      // Oeffnen -> eigenes behalten (editierbar)
            }
            catch (Exception ex) { Log.Error("Rollen-Modell-Dropdown laden fehlgeschlagen", ex); }
        }

        /// <summary>Liest eine Rollen-Zeile als persistente Einstellung aus.</summary>
        private static ModelRoleSetting ReadRole(ComboBox providerBox, ComboBox modelBox) => new()
        {
            Provider = providerBox.SelectedItem as string ?? "gemini",
            Model = (modelBox.Text ?? string.Empty).Trim(),
        };

        /// <summary>
        /// Fuellt das Haupt-Modell-Dropdown mit den Modellen des aktuellen Anbieters (live + Fallback).
        /// forceValid=true (Anbieter-Wechsel): passt das aktuelle Modell nicht zur Liste, wird das erste
        /// gueltige genommen. forceValid=false (Oeffnen): die gespeicherte Auswahl bleibt erhalten.
        /// </summary>
        private async System.Threading.Tasks.Task FillMainModelsAsync(bool forceValid)
        {
            try
            {
                var provider = (ProviderBox.SelectedItem as string) ?? "gemini";
                var current = (ModelBox.Text ?? string.Empty).Trim();
                var list = await ModelCatalog.GetModelsAsync(provider).ConfigureAwait(true);
                ModelBox.ItemsSource = list;
                if (ListContains(list, current)) ModelBox.Text = current;          // gueltig -> behalten
                else if (forceValid && list.Count > 0) ModelBox.Text = list[0];    // Wechsel -> gueltiges Default
                else ModelBox.Text = current;                                      // Oeffnen -> eigenes behalten (editierbar)
            }
            catch (Exception ex) { Log.Error("Modell-Dropdown laden fehlgeschlagen", ex); }
        }

        private static bool ListContains(IReadOnlyList<string> list, string value)
        {
            foreach (var s in list) if (string.Equals(s, value, StringComparison.OrdinalIgnoreCase)) return true;
            return false;
        }

        /// <summary>Zeigt den Codex-Anmeldestatus und schaltet den Abmelden-Button entsprechend.</summary>
        private void RefreshCodexStatus()
        {
            bool loggedIn = CodexAuth.IsLoggedIn();
            CodexStatusText.Text = loggedIn
                ? "✓ Bei Codex angemeldet (läuft über dein ChatGPT-Abo)."
                : "Nicht angemeldet. Per Gerätecode anmelden, dann oben als Anbieter „codex“ wählen.";
            CodexLogoutButton.IsEnabled = loggedIn;
        }

        // Geraetecode-Anmeldung. Event-Handler: async void ist hier ok, Body komplett in try/catch (Almanach §7.2).
        private async void CodexLogin_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                CodexLoginButton.IsEnabled = false;
                CodexStatusText.Text = "Fordere Gerätecode an…";

                var info = await CodexAuth.RequestDeviceCodeAsync();

                // Browser auf die Bestätigungs-Seite öffnen (Standard-Browser via Shell).
                try
                {
                    System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(info.VerificationUrl)
                    { UseShellExecute = true });
                }
                catch (Exception ex) { Log.Warn($"Codex: Browser öffnen fehlgeschlagen ({ex.Message})"); }

                CodexStatusText.Text = $"Im Browser ({info.VerificationUrl}) diesen Code eingeben: {info.UserCode}\nWarte auf Bestätigung…";
                MessageBox.Show(this,
                    $"Im Browser diese Seite öffnen:\n{info.VerificationUrl}\n\nUnd diesen Code eingeben:\n\n    {info.UserCode}\n\nDanach hier warten — die Anmeldung wird automatisch erkannt.",
                    "Codex-Anmeldung per Gerätecode", MessageBoxButton.OK, MessageBoxImage.Information);

                await CodexAuth.PollForTokensAsync(info);

                RefreshCodexStatus();
                CodexStatusText.Text = "✓ Codex-Anmeldung erfolgreich. Anbieter „codex“ wählen und Modell (z. B. gpt-5.5) eintragen.";
            }
            catch (Exception ex)
            {
                Log.Error("Codex-Anmeldung fehlgeschlagen", ex);
                CodexStatusText.Text = "Anmeldung fehlgeschlagen: " + ex.Message;
            }
            finally
            {
                CodexLoginButton.IsEnabled = true;
            }
        }

        private void CodexLogout_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                CodexAuth.Logout();
                RefreshCodexStatus();
            }
            catch (Exception ex)
            {
                Log.Error("Codex-Abmelden fehlgeschlagen", ex);
            }
        }
    }
}
