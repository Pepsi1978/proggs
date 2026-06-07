using System;
using System.IO;
using System.Threading;
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
    /// Mikrofon (AlwaysOnListener) → Transkription (Groq) → semantische Endpunkt-Erkennung
    /// (sammelt Sprech-Haeppchen, wartet Gedankenpausen ab) → Hauptagent (LLM) →
    /// Sprachausgabe (Google Chirp3-HD). Text-Eingabe bleibt zusaetzlich moeglich.
    /// </summary>
    public partial class MainWindow : Window
    {
        private AppSettings _settings = new();
        private BossAgent? _agent;
        private EndpointDetector? _endpoint;
        private GoogleTtsClient? _tts;
        private GroqWhisperClient? _stt;
        private AlwaysOnListener? _listener;
        private readonly AudioPlayer _player = new();

        private bool _busy;                          // verhindert ueberlappende Verarbeitung (nur UI-Thread)
        private string _pending = string.Empty;      // gesammelte Sprech-Haeppchen einer noch offenen Aussage
        private CancellationTokenSource? _safetyCts;  // Sicherheitsnetz-Timer nach "WEITER"

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
                BuildAgents();

                _listener = new AlwaysOnListener(_settings.SilenceThreshold, _settings.SilenceMs, _settings.MinUtteranceMs);
                _listener.OnUtterance += OnUtterance;
                _listener.Start();
                _listener.SetEnabled(_settings.MicEnabled);

                MicToggle.IsChecked = _settings.MicEnabled;
                UpdateMicLabel();
                Log.Info("MainWindow geladen — Voice-Loop bereit.");
                Append("System", "Bereit. Sprich einfach los — Gedankenpausen sind erlaubt. Oder tippe unten.");
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Initialisierung fehlgeschlagen", ex);
                Append("System", "Initialisierung fehlgeschlagen: " + ex.Message);
            }
        }

        private void BuildAgents()
        {
            _agent = new BossAgent(LlmProviderFactory.Create(_settings), _settings.SystemPrompt);
            // Endpunkt-Check laeuft immer auf einem guenstigen, schnellen Gemini-Modell
            // (unabhaengig vom Haupt-Gehirn) — separat in den Einstellungen waehlbar.
            _endpoint = new EndpointDetector(new GeminiProvider(Config.ReadApiKey("gemini"), _settings.EndpointModel));
            _tts = new GoogleTtsClient(Config.ReadApiKey("google"));
            _stt = new GroqWhisperClient(Config.ReadApiKey("groq"), _settings.SttModel, _settings.SttLanguage);
        }

        private void OnClosed(object? sender, EventArgs e)
        {
            try { CancelSafetyTimer(); _listener?.Dispose(); _player.Stop(); }
            catch (Exception ex) { Log.Error("MainWindow: Aufraeumen-Fehler", ex); }
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
            var dlg = new Views.SettingsWindow(_settings) { Owner = this };
            if (dlg.ShowDialog() == true)
            {
                try
                {
                    _settings = Config.Load();
                    BuildAgents();
                    _listener?.SetEnabled(_settings.MicEnabled);
                    MicToggle.IsChecked = _settings.MicEnabled;
                    UpdateMicLabel();
                    Append("System", "Einstellungen gespeichert und uebernommen.");
                }
                catch (Exception ex)
                {
                    Log.Error("MainWindow: Uebernehmen der Einstellungen fehlgeschlagen", ex);
                    Append("Fehler", ex.Message);
                }
            }
        }

        // ---------- Sprach-Eingang (vom Listener-Thread) ----------

        private void OnUtterance(string wavPath) => Dispatcher.InvokeAsync(() => _ = HandleUtteranceAsync(wavPath));

        private async Task HandleUtteranceAsync(string wavPath)
        {
            CancelSafetyTimer();   // neues Haeppchen → Sicherheitsnetz zuruecksetzen
            if (_busy || _stt == null) { TryDelete(wavPath); return; }

            _busy = true;
            PauseMic();   // waehrend Transkription/Check/Sprechen nicht mithoeren
            try
            {
                SetStatus("Hoere zu …");
                string segment = await _stt.TranscribeAsync(wavPath);
                TryDelete(wavPath);

                if (string.IsNullOrWhiteSpace(segment))
                {
                    if (!string.IsNullOrEmpty(_pending)) StartSafetyTimer();   // Aussage offen lassen
                    else SetStatus("Bereit");
                    return;
                }

                _pending = string.IsNullOrEmpty(_pending) ? segment.Trim() : (_pending + " " + segment.Trim());

                bool complete = true;
                if (_settings.SemanticEndpointing && _endpoint != null)
                {
                    SetStatus("Pruefe, ob du fertig bist …");
                    complete = await _endpoint.IsCompleteAsync(_pending);
                }

                if (complete)
                {
                    await FinalizeAsync();
                }
                else
                {
                    SetStatus("… ich hoere weiter zu");
                    StartSafetyTimer();   // falls nichts mehr kommt, doch senden
                }
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprach-Verarbeitung fehlgeschlagen", ex);
                Append("Fehler", ex.Message);
                SetStatus("Fehler — siehe Log");
                _pending = string.Empty;
            }
            finally
            {
                _busy = false;
                ResumeMic();
            }
        }

        /// <summary>Sendet die gesammelte Aussage an den Hauptagenten und leert den Puffer.</summary>
        private async Task FinalizeAsync()
        {
            var text = _pending;
            _pending = string.Empty;
            if (string.IsNullOrWhiteSpace(text)) return;
            Append("Du", text);
            await RespondAndSpeakAsync(text);
        }

        // ---------- Sicherheitsnetz: nach langer Stille trotzdem senden ----------

        private void StartSafetyTimer()
        {
            CancelSafetyTimer();
            _safetyCts = new CancellationTokenSource();
            var token = _safetyCts.Token;
            int waitMs = _settings.EndpointMaxWaitMs;
            _ = Task.Delay(waitMs, token).ContinueWith(t =>
            {
                if (t.IsCanceled) return;
                Dispatcher.InvokeAsync(() => _ = FlushPendingAsync());
            }, TaskScheduler.Default);
        }

        private void CancelSafetyTimer()
        {
            try { _safetyCts?.Cancel(); _safetyCts?.Dispose(); } catch { /* egal */ }
            _safetyCts = null;
        }

        private async Task FlushPendingAsync()
        {
            if (_busy || string.IsNullOrEmpty(_pending)) return;
            _busy = true;
            PauseMic();
            try
            {
                Log.Info("Endpoint: Sicherheitsnetz greift — sende gesammelte Aussage");
                await FinalizeAsync();
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sicherheitsnetz-Senden fehlgeschlagen", ex);
                _pending = string.Empty;
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

            CancelSafetyTimer();
            _pending = string.Empty;
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
