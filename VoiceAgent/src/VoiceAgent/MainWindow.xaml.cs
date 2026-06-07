using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;
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
        private ReminderService? _reminderService;                // geplante Erinnerungen (zeitgesteuert)
        private readonly Chime _chime = new();                    // Enterprise-Sound fuer proaktive Meldungen
        private Reminder? _pendingReminder;                       // faellig, wartet auf Franks Reaktion
        private readonly Queue<Reminder> _reminderQueue = new();  // weitere faellige, noch nicht angesagt
        private DispatcherTimer? _reminderPingTimer;              // wiederholt den Sound jede Minute bis Reaktion

        private bool _busy;                          // verhindert ueberlappende Verarbeitung (nur UI-Thread)
        private string _pending = string.Empty;      // gesammelte Sprech-Haeppchen einer noch offenen Aussage
        private CancellationTokenSource? _safetyCts;  // Sicherheitsnetz-Timer nach "WEITER"
        private TurnTrace? _turn;                     // aktueller Sprach-Turn (Live-Logik-Sonde)
        private DispatcherTimer? _clockTimer;         // live Uhr oben im Fenster

        private SessionStore? _sessionStore;          // Persistenz der Sessions (JSON pro Session)
        private SessionManager? _sessions;            // aktive Session, entkoppelt vom Provider/Settings
        private ContextCompressor? _compressor;       // verlustarme Hintergrund-Komprimierung pro Session
        private ILlmProvider? _provider;              // aktiver LLM-Provider (auch fuer die Komprimierung)
        private bool _ready;                          // true erst NACH dem Laden — Guard gegen Handler beim XAML-Load (§2.10)
        private bool _suppressSelection;              // unterdrueckt SelectionChanged beim Neu-Befuellen der Liste

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
                _reminderService = new ReminderService();
                _subAgents.Register(new ReminderSubAgent(_reminderService));   // Erinnerungen (zeitgesteuert)
                _subAgents.Register(new NoteSubAgent(_memory));                // Notizen

                _sessionStore = new SessionStore();
                _sessions = new SessionManager(_sessionStore);
                _sessions.ActiveChanged += RefreshSessionList;
                _sessions.NewSession();                 // Start: frische, leere Session
                _compressor = new ContextCompressor();  // Default-Budget/-Schwelle

                BuildAgents();

                RebuildListener();

                MicToggle.IsChecked = _settings.MicEnabled;
                UpdateMicLabel();
                StartClock();
                Log.Info("MainWindow geladen — Voice-Loop bereit.");
                Append("System", "Bereit. Sprich einfach los — Gedankenpausen sind erlaubt. Oder tippe unten.");

                _ready = true;            // ab jetzt duerfen die Sidebar-Handler feuern
                RefreshSessionList();
                RefreshContextMeter();
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Initialisierung fehlgeschlagen", ex);
                Append("System", "Initialisierung fehlgeschlagen: " + ex.Message);
            }
        }

        private void BuildAgents()
        {
            _provider = LlmProviderFactory.Create(_settings);
            // WICHTIG (Settings-Fix): die AKTIVE Session wird hereingereicht — beim erneuten BuildAgents
            // (z.B. nach dem Speichern der Einstellungen) bleibt derselbe Verlauf erhalten.
            _agent = new BossAgent(_provider, _settings.SystemPrompt, _memory, _subAgents, _settings.TimeZoneId, _sessions?.Active);
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
            try { _clockTimer?.Stop(); _reminderPingTimer?.Stop(); CancelSafetyTimer(); _listener?.Dispose(); _player.Stop(); }
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

        private void StartClock()
        {
            _clockTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            _clockTimer.Tick += (_, __) => { UpdateClock(); CheckReminders(); };
            _clockTimer.Start();
            UpdateClock();
        }

        /// <summary>Zeigt die Uhr in derselben Zeitzone, die auch der Agent kennt (konsistent).</summary>
        private void UpdateClock()
        {
            try
            {
                var zone = TimeContext.ResolveZone(_settings.TimeZoneId);
                var now = TimeZoneInfo.ConvertTime(DateTimeOffset.Now, zone);
                ClockText.Text = now.ToString("ddd, dd.MM.yyyy  HH:mm:ss", new System.Globalization.CultureInfo("de-DE"));
            }
            catch { /* Uhr ist unkritisch — nie crashen */ }
        }

        /// <summary>Prueft jede Sekunde, ob eine Erinnerung faellig ist (nur wenn gerade nichts laeuft).</summary>
        /// <summary>Sammelt faellige Erinnerungen und stoesst den Ping-Vorgang an (jede Sekunde aufgerufen).</summary>
        private void CheckReminders()
        {
            if (_reminderService == null) return;
            try
            {
                foreach (var r in _reminderService.TakeDue(DateTimeOffset.Now)) _reminderQueue.Enqueue(r);
                TryStartReminderPing();
            }
            catch (Exception ex) { Log.Error("CheckReminders fehlgeschlagen", ex); }
        }

        /// <summary>
        /// Zwei-Schritt-Erinnerung Schritt 1: Sound ertoenen lassen und so lange jede Minute
        /// WIEDERHOLEN, bis Frank reagiert. KEINE Ansage, Mikrofon bleibt an (Frank soll antworten).
        /// Die Ansage (Schritt 2) kommt erst, wenn Frank etwas sagt (siehe AnnounceReminderAsync).
        /// </summary>
        private void TryStartReminderPing()
        {
            if (_pendingReminder != null || _reminderQueue.Count == 0 || _busy) return;
            _pendingReminder = _reminderQueue.Dequeue();
            Log.Info("Erinnerung faellig — Ping startet, warte auf Reaktion", new { _pendingReminder.Text });
            Append("System", "(Erinnerung faellig — sag kurz Bescheid, dann sage ich dir, worum es geht.)");
            StartPinging();
        }

        private void StartPinging()
        {
            _chime.Play();   // sofort einmal
            _reminderPingTimer ??= new DispatcherTimer { Interval = TimeSpan.FromMinutes(1) };
            _reminderPingTimer.Tick -= OnReminderPing;   // doppelte Registrierung vermeiden
            _reminderPingTimer.Tick += OnReminderPing;
            _reminderPingTimer.Start();
        }

        private void OnReminderPing(object? sender, EventArgs e)
        {
            if (_pendingReminder == null) { StopPinging(); return; }
            _chime.Play();   // alle 60 s wiederholen, bis Frank reagiert (falls er den Sound verpasst hat)
        }

        private void StopPinging() => _reminderPingTimer?.Stop();

        /// <summary>Schritt 2: Frank hat reagiert — jetzt erst die Erinnerung vorlesen, dann ggf. die naechste.</summary>
        private async Task AnnounceReminderAsync(Reminder r)
        {
            _busy = true;
            PauseMic();
            try
            {
                var msg = $"Du wolltest, dass ich dich erinnere: {r.Text}";
                Append("Agent", msg);
                Log.Info("Erinnerung bestaetigt — Ansage", new { r.Text });
                await SpeakAsync(msg);
            }
            catch (Exception ex) { Log.Error("Erinnerungs-Ansage fehlgeschlagen", ex); }
            finally
            {
                _busy = false;
                ResumeMic();
                TryStartReminderPing();   // falls weitere Erinnerungen warten
            }
        }

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

                // Wartet eine Erinnerung auf Bestaetigung? Dann ist DIESE Aeusserung Franks Reaktion
                // ("ja, was gibt es?") -> jetzt erst die Erinnerung ansagen, kein normaler Turn.
                if (_pendingReminder != null)
                {
                    var pend = _pendingReminder; _pendingReminder = null; StopPinging();
                    await AnnounceReminderAsync(pend);
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

            // Wartet eine Erinnerung auf Bestaetigung? Dann loest diese Eingabe die Ansage aus.
            if (_pendingReminder != null)
            {
                var pend = _pendingReminder; _pendingReminder = null; StopPinging();
                InputBox.Clear();
                Append("Du", text);
                await AnnounceReminderAsync(pend);
                return;
            }

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

            // Nach dem Sprechen: Session sichern, Titel ggf. setzen, bei Bedarf im Hintergrund komprimieren.
            _sessions?.EnsureTitleFromFirstMessage();
            if (_compressor != null && _provider != null && _sessions != null)
                await _compressor.MaybeCompressAsync(_sessions.Active, _provider);
            _sessions?.SaveActive();
            RefreshContextMeter();
            RefreshSessionList();

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
            // Farbliche Unterscheidung: Du/Agent aus den Einstellungen waehlbar; Fehler=Rot, System=Grau.
            var brush = new SolidColorBrush(who switch
            {
                "Du" => ParseColor(_settings.UserColor, Color.FromRgb(0x4F, 0xC3, 0xF7)),
                "Agent" => ParseColor(_settings.AgentColor, Color.FromRgb(0xF9, 0x73, 0x16)),
                "Fehler" => Color.FromRgb(0xEF, 0x53, 0x50),
                _ => Color.FromRgb(0x9A, 0xA0, 0xAA),
            });
            var para = new Paragraph();
            para.Inlines.Add(new Run(who + ": ") { FontWeight = FontWeights.Bold, Foreground = brush });
            para.Inlines.Add(new Run(text) { Foreground = brush });
            ConversationBox.Document.Blocks.Add(para);
            ConversationBox.ScrollToEnd();
        }

        /// <summary>Hex -> Color, mit Fallback bei ungueltigem Wert (kein Crash).</summary>
        private static Color ParseColor(string? hex, Color fallback)
        {
            if (string.IsNullOrWhiteSpace(hex)) return fallback;
            try { return (Color)ColorConverter.ConvertFromString(hex)!; }
            catch { return fallback; }
        }

        // ---------- Sessions-Sidebar ----------

        private void RefreshSessionList()
        {
            if (_sessions == null) return;
            try
            {
                var query = SearchBox.Text?.Trim() ?? "";
                IEnumerable<SessionInfo> items = _sessions.List();
                if (!string.IsNullOrEmpty(query))
                    items = items.Where(i => i.Title.Contains(query, StringComparison.OrdinalIgnoreCase));
                _suppressSelection = true;
                SessionList.ItemsSource = items.ToList();
                SessionList.SelectedItem = null;
                _suppressSelection = false;
            }
            catch (Exception ex) { Log.Error("Sidebar: Liste aktualisieren fehlgeschlagen", ex); }
        }

        private void NewSessionButton_Click(object sender, RoutedEventArgs e)
        {
            if (_sessions == null) return;
            _sessions.SaveActive();
            _sessions.NewSession();
            BuildAgents();          // Agent zeigt auf die neue (leere) Session
            ClearTranscript();
            RefreshContextMeter();
        }

        private void SessionList_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            if (!_ready || _suppressSelection || _sessions == null) return;
            if (SessionList.SelectedItem is SessionInfo info && info.Id != _sessions.Active.Id)
            {
                _sessions.SaveActive();
                _sessions.Switch(info.Id);
                BuildAgents();
                RenderTranscript();
                RefreshContextMeter();
            }
        }

        private void SearchBox_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e)
        {
            if (!_ready) return;
            RefreshSessionList();
        }

        private void RenameSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            var name = PromptText("Session umbenennen", info.Title);
            if (!string.IsNullOrWhiteSpace(name)) { _sessions.Rename(info.Id, name!); RefreshSessionList(); }
        }

        private void PinSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            _sessions.SetPinned(info.Id, !info.Pinned);
            RefreshSessionList();
        }

        private void DeleteSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            if (MessageBox.Show($"Session „{info.Title}“ löschen?", "Löschen",
                    MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
            bool wasActive = info.Id == _sessions.Active.Id;
            _sessions.Delete(info.Id);
            if (wasActive) { BuildAgents(); RenderTranscript(); }
            RefreshSessionList();
            RefreshContextMeter();
        }

        private static SessionInfo? InfoFromMenu(object sender)
            => (sender as System.Windows.Controls.MenuItem)?.DataContext as SessionInfo;

        private void RefreshContextMeter()
        {
            if (_compressor == null || _sessions == null) return;
            try
            {
                int pct = (int)Math.Round(_compressor.Fill(_sessions.Active) * 100);
                ContextPillText.Text = pct >= 75 ? $"Kontext {pct}% · wird komprimiert…" : $"Kontext {pct}%";
            }
            catch (Exception ex) { Log.Error("Kontext-Anzeige aktualisieren fehlgeschlagen", ex); }
        }

        private void ClearTranscript() => ConversationBox.Document.Blocks.Clear();

        private void RenderTranscript()
        {
            ClearTranscript();
            if (_sessions == null) return;
            foreach (var m in _sessions.Active.History)
                Append(m.Role == LlmRole.Assistant ? "Agent" : "Du", m.Text);
        }

        /// <summary>Kleiner modaler Text-Dialog (bewusst ohne Microsoft.VisualBasic-Dependency).</summary>
        private string? PromptText(string title, string initial)
        {
            var win = new Window
            {
                Title = title, Width = 380, Height = 150, Owner = this,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                ResizeMode = ResizeMode.NoResize,
                Background = new SolidColorBrush(Color.FromRgb(0x1E, 0x21, 0x28))
            };
            var grid = new System.Windows.Controls.Grid { Margin = new Thickness(12) };
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            var tb = new System.Windows.Controls.TextBox
            {
                Text = initial, FontSize = 14, Padding = new Thickness(6, 4, 6, 4),
                Background = new SolidColorBrush(Color.FromRgb(0x23, 0x26, 0x2E)),
                Foreground = new SolidColorBrush(Color.FromRgb(0xE4, 0xE6, 0xEB)),
                VerticalContentAlignment = VerticalAlignment.Center
            };
            System.Windows.Controls.Grid.SetRow(tb, 0);
            var ok = new System.Windows.Controls.Button
            {
                Content = "OK", Width = 90, Height = 30, IsDefault = true,
                HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Bottom
            };
            System.Windows.Controls.Grid.SetRow(ok, 1);
            string? result = null;
            ok.Click += (_, __) => { result = tb.Text; win.DialogResult = true; };
            grid.Children.Add(tb);
            grid.Children.Add(ok);
            win.Content = grid;
            tb.Loaded += (_, __) => { tb.Focus(); tb.SelectAll(); };
            return win.ShowDialog() == true ? result : null;
        }

        private void SetStatus(string text) => StatusText.Text = text;
    }
}
