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
        private IntentDetector? _intentDetector;
        private GoogleTtsClient? _tts;
        private GroqWhisperClient? _stt;
        private AlwaysOnListener? _listener;
        private readonly AudioPlayer _player = new();
        private AgentMemory? _memory;                 // persistentes Langzeit-Gedaechtnis (ueber Sessions)
        private SubAgentRegistry? _subAgents;         // Unteragenten, die der Hauptagent dirigiert

        private bool _busy;                          // verhindert ueberlappende Verarbeitung (nur UI-Thread)
        private string _pending = string.Empty;      // gesammelte Sprech-Haeppchen einer noch offenen Aussage
        private CancellationTokenSource? _safetyCts;  // Sicherheitsnetz-Timer nach "WEITER"
        private TurnTrace? _turn;                     // aktueller Sprach-Turn (Live-Logik-Sonde)

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
                _memory = new AgentMemory();   // laedt Fakten + letzte Gespraeche aus frueheren Sessions
                _subAgents = new SubAgentRegistry();
                _subAgents.Register(new NoteSubAgent(_memory));   // erster Demo-Unteragent (merkt sich Notizen)
                BuildAgents();

                RebuildListener();

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
            _agent = new BossAgent(LlmProviderFactory.Create(_settings), _settings.SystemPrompt, _memory, _subAgents);
            // Endpunkt-Check laeuft immer auf einem guenstigen, schnellen Gemini-Modell
            // (unabhaengig vom Haupt-Gehirn) — separat in den Einstellungen waehlbar.
            _endpoint = new EndpointDetector(new GeminiProvider(Config.ReadApiKey("gemini"), _settings.EndpointModel));
            _intentDetector = new IntentDetector(new GeminiProvider(Config.ReadApiKey("gemini"), _settings.IntentModel));
            _tts = new GoogleTtsClient(Config.ReadApiKey("google"));
            _stt = new GroqWhisperClient(Config.ReadApiKey("groq"), _settings.SttModel, _settings.SttLanguage);
        }

        /// <summary>
        /// Erstellt den Mikrofon-Listener mit den aktuellen Empfindlichkeits-Werten neu.
        /// Wird beim Laden UND nach dem Speichern der Einstellungen aufgerufen, damit geaenderte
        /// Schieberegler-Werte (Stille-Schwelle/Pausendauer) sofort greifen — nicht erst beim Neustart.
        /// </summary>
        private void RebuildListener()
        {
            try { _listener?.Dispose(); }
            catch (Exception ex) { Log.Error("MainWindow: alten Listener schliessen fehlgeschlagen", ex); }
            _listener = new AlwaysOnListener(_settings.SilenceThreshold, _settings.SilenceMs, _settings.MinUtteranceMs);
            _listener.OnUtterance += OnUtterance;
            _listener.Start();
            _listener.SetEnabled(_settings.MicEnabled);
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

        private void LogButton_Click(object sender, RoutedEventArgs e)
        {
            // Nicht-modal (Show, nicht ShowDialog): Frank kann gleichzeitig sprechen und mitlesen.
            try { new Views.LogViewerWindow { Owner = this }.Show(); }
            catch (Exception ex) { Log.Error("MainWindow: Log-Viewer oeffnen fehlgeschlagen", ex); }
        }

        private void SettingsButton_Click(object sender, RoutedEventArgs e)
        {
            // Auch das OEFFNEN in try/catch: ein Fehler im Settings-Fenster darf die App nie killen.
            try
            {
                var dlg = new Views.SettingsWindow(_settings) { Owner = this };
                if (dlg.ShowDialog() == true)
                {
                    _settings = Config.Load();
                    BuildAgents();
                    RebuildListener();   // geaenderte Empfindlichkeit sofort uebernehmen
                    MicToggle.IsChecked = _settings.MicEnabled;
                    UpdateMicLabel();
                    Append("System", "Einstellungen gespeichert und uebernommen.");
                }
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Einstellungen oeffnen/uebernehmen fehlgeschlagen", ex);
                Append("Fehler", "Einstellungen konnten nicht geoeffnet werden: " + ex.Message);
                SetStatus("Fehler — siehe Log");
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

                // Live-Sonde: neuer Turn, sobald das erste Haeppchen einer frischen Aussage eintrifft.
                if (string.IsNullOrEmpty(_pending)) _turn = TurnTrace.Begin("voice");
                _turn?.Heard(segment);

                _pending = string.IsNullOrEmpty(_pending) ? segment.Trim() : (_pending + " " + segment.Trim());

                bool complete = true;
                if (_settings.SemanticEndpointing && _endpoint != null)
                {
                    SetStatus("Pruefe, ob du fertig bist …");
                    complete = await _endpoint.IsCompleteAsync(_pending);
                    _turn?.Endpoint(complete);
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
                _turn?.Failed("voice", ex);
                _turn?.Complete();
                _turn = null;
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
            if (string.IsNullOrWhiteSpace(text)) { _turn?.Abort("leere Aussage"); _turn = null; return; }
            _turn?.Accumulated(text);
            Append("Du", text);
            await RespondAndSpeakAsync(text);
            _turn?.Complete();
            _turn = null;
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
                _turn?.Failed("safety-flush", ex);
                _turn?.Complete();
                _turn = null;
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
            _turn = TurnTrace.Begin("text");
            _turn.Accumulated(text);
            try
            {
                await RespondAndSpeakAsync(text);
                _turn?.Complete();
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Text-Verarbeitung fehlgeschlagen", ex);
                _turn?.Failed("text", ex);
                _turn?.Complete();
                Append("Fehler", ex.Message);
                SetStatus("Fehler — siehe Log");
            }
            finally
            {
                _turn = null;
                SendButton.IsEnabled = true;
                _busy = false;
                ResumeMic();
            }
        }

        // ---------- Gemeinsamer Kern: antworten + vorlesen ----------

        private async Task RespondAndSpeakAsync(string text)
        {
            if (_agent == null) return;
            // Erst VERSTEHEN: Intent exakt per LLM-Detektor klassifizieren (wenn aktiviert),
            // sonst spaeter heuristisch aus der Antwort ableiten.
            bool exactIntent = _settings.IntentDetection && _intentDetector != null;
            IntentKind intent = IntentKind.Unknown;
            if (exactIntent)
            {
                SetStatus("Verstehe, was du willst …");
                intent = await _intentDetector!.ClassifyAsync(text);
                _turn?.UnderstoodExact(intent);
            }
            SetStatus("Denke nach …");
            var sw = System.Diagnostics.Stopwatch.StartNew();
            var result = await _agent.HandleAsync(text, intent);   // delegiert ggf. an einen Unteragenten
            sw.Stop();
            var reply = result.Text;
            if (result.DelegatedTo != null) _turn?.Delegated(result.DelegatedTo);
            if (!exactIntent) _turn?.Understood(reply);                                  // Fallback: heuristisch aus der Antwort
            _turn?.Responded(reply, _agent.ProviderName, sw.Elapsed.TotalMilliseconds);  // welches Gehirn, wie schnell, Rueckfrage?
            Append("Agent", reply);
            SetStatus("Spreche …");
            bool spoke = await SpeakAsync(reply);
            _turn?.Spoke(_settings.TtsVoiceName, spoke);
            SetStatus("Bereit");
        }

        private async Task<bool> SpeakAsync(string text)
        {
            if (_tts == null || string.IsNullOrWhiteSpace(text)) return false;
            try
            {
                var audio = await _tts.SynthesizeAsync(text, _settings.TtsLanguageCode, _settings.TtsVoiceName);
                await _player.PlayAsync(audio);
                return true;
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprachausgabe fehlgeschlagen (App laeuft weiter)", ex);
                return false;
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
