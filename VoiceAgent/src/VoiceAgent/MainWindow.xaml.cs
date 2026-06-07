using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Audio;
using VoiceAgent.Services.Llm;

namespace VoiceAgent
{
    /// <summary>
    /// Hauptfenster und Orchestrierung des Voice-Loops:
    /// Mikrofon (AlwaysOnListener) → Transkription (Groq) → Hauptagent (LLM) →
    /// Sprachausgabe (Google Chirp3-HD). Text-Eingabe bleibt zusaetzlich moeglich.
    /// </summary>
    public partial class MainWindow : Window
    {
        private AppSettings _settings = new();
        private BossAgent? _agent;
        private GoogleTtsClient? _tts;
        private GroqWhisperClient? _stt;
        private AlwaysOnListener? _listener;
        private readonly AudioPlayer _player = new();
        private bool _busy;   // verhindert ueberlappende Verarbeitung (nur UI-Thread)

        public MainWindow()
        {
            InitializeComponent();
            Loaded += OnLoaded;
            Closed += OnClosed;
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            try
            {
                _settings = Config.Load();
                _agent = new BossAgent(LlmProviderFactory.Create(_settings), _settings.SystemPrompt);
                _tts = new GoogleTtsClient(Config.ReadApiKey("google"));
                _stt = new GroqWhisperClient(Config.ReadApiKey("groq"), _settings.SttModel, _settings.SttLanguage);

                _listener = new AlwaysOnListener(_settings.SilenceThreshold, _settings.SilenceMs, _settings.MinUtteranceMs);
                _listener.OnUtterance += OnUtterance;
                _listener.Start();
                _listener.SetEnabled(_settings.MicEnabled);

                MicToggle.IsChecked = _settings.MicEnabled;
                UpdateMicLabel();
                Log.Info("MainWindow geladen — Voice-Loop bereit.");
                Append("System", "Bereit. Sprich einfach los — oder tippe unten. (API-Schluessel in ~/SK/VoiceAgent/keys.json)");
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Initialisierung fehlgeschlagen", ex);
                Append("System", "Initialisierung fehlgeschlagen: " + ex.Message);
            }
        }

        private void OnClosed(object? sender, EventArgs e)
        {
            try { _listener?.Dispose(); _player.Stop(); } catch (Exception ex) { Log.Error("MainWindow: Aufraeumen-Fehler", ex); }
        }

        // ---------- Mikrofon-Schalter ----------

        private void MicToggle_Click(object sender, RoutedEventArgs e)
        {
            bool on = MicToggle.IsChecked == true;
            _settings.MicEnabled = on;
            _listener?.SetEnabled(on);
            Config.Save(_settings);
            UpdateMicLabel();
        }

        private void UpdateMicLabel() => MicToggle.Content = _settings.MicEnabled ? "Mikrofon: an" : "Mikrofon: aus";

        private void SettingsButton_Click(object sender, RoutedEventArgs e)
        {
            // Vollwertiges Einstellungs-Fenster folgt in Etappe 5.
            Append("System", "Einstellungen folgen in Etappe 5. API-Schluessel bis dahin in ~/SK/VoiceAgent/keys.json.");
        }

        // ---------- Sprach-Eingang (vom Listener-Thread) ----------

        private void OnUtterance(string wavPath)
        {
            // Auf den UI-Thread marshallen; Verarbeitung dort starten.
            Dispatcher.InvokeAsync(() => _ = HandleUtteranceAsync(wavPath));
        }

        private async Task HandleUtteranceAsync(string wavPath)
        {
            if (_busy || _stt == null)
            {
                TryDelete(wavPath);
                return;
            }
            _busy = true;
            PauseMic();   // waehrend Verarbeitung + Sprechen nicht zuhoeren (kein Selbst-Mithoeren)
            try
            {
                SetStatus("Hoere zu …");
                string text = await _stt.TranscribeAsync(wavPath);
                TryDelete(wavPath);

                if (string.IsNullOrWhiteSpace(text))
                {
                    SetStatus("Bereit");
                    return;
                }

                Append("Du", text);
                await RespondAndSpeakAsync(text);
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprach-Verarbeitung fehlgeschlagen", ex);
                Append("Fehler", ex.Message);
                SetStatus("Fehler — siehe Log");
            }
            finally
            {
                _busy = false;
                ResumeMic();
            }
        }

        // ---------- Text-Eingabe ----------

        private void InputBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter) { e.Handled = true; _ = SendTextAsync(); }
        }

        private void SendButton_Click(object sender, RoutedEventArgs e) => _ = SendTextAsync();

        private async Task SendTextAsync()
        {
            var text = InputBox.Text?.Trim();
            if (string.IsNullOrEmpty(text) || _agent == null || _busy) return;

            InputBox.Clear();
            _busy = true;
            PauseMic();
            Append("Du", text);
            SendButton.IsEnabled = false;
            try
            {
                await RespondAndSpeakAsync(text);
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Text-Verarbeitung fehlgeschlagen", ex);
                Append("Fehler", ex.Message);
                SetStatus("Fehler — siehe Log");
            }
            finally
            {
                SendButton.IsEnabled = true;
                _busy = false;
                ResumeMic();
            }
        }

        // ---------- Gemeinsamer Kern: antworten + vorlesen ----------

        private async Task RespondAndSpeakAsync(string text)
        {
            if (_agent == null) return;
            SetStatus("Denke nach …");
            var reply = await _agent.RespondAsync(text);
            Append("Agent", reply);
            SetStatus("Spreche …");
            await SpeakAsync(reply);
            SetStatus("Bereit");
        }

        private async Task SpeakAsync(string text)
        {
            if (_tts == null || string.IsNullOrWhiteSpace(text)) return;
            try
            {
                var audio = await _tts.SynthesizeAsync(text, _settings.TtsLanguageCode, _settings.TtsVoiceName);
                await _player.PlayAsync(audio);
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprachausgabe fehlgeschlagen (App laeuft weiter)", ex);
            }
        }

        // ---------- Helfer ----------

        private void PauseMic() => _listener?.SetEnabled(false);
        private void ResumeMic() { if (_settings.MicEnabled) _listener?.SetEnabled(true); }

        private static void TryDelete(string path)
        {
            try { if (File.Exists(path)) File.Delete(path); } catch { /* egal */ }
        }

        private void Append(string who, string text)
        {
            ConversationBox.AppendText($"{who}: {text}\n\n");
            ConversationBox.ScrollToEnd();
        }

        private void SetStatus(string text) => StatusText.Text = text;
    }
}
