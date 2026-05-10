using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using ClaudeVoiceOverlay.Models;
using ClaudeVoiceOverlay.NativeMethods;
using ClaudeVoiceOverlay.Services;

namespace ClaudeVoiceOverlay.Views
{
    public partial class OverlayWindow : Window
    {
        // ── Color constants (matches macOS OverlayPanel.swift) ──

        // Idle/base
        private static readonly SolidColorBrush BtnIdle       = Brush("#2D2D2D");
        private static readonly SolidColorBrush BtnRecording  = Brush("#E53935");
        private static readonly SolidColorBrush BtnProcessing = Brush("#FF9800");
        private static readonly SolidColorBrush BtnSuccess    = Brush("#43A047");
        // Toggles
        private static readonly SolidColorBrush ToggleOn      = Brush("#16a34a");
        private static readonly SolidColorBrush ToggleOff     = Brush("#2D2D2D");
        // BTW mic
        private static readonly SolidColorBrush BtnBtwIdle      = Brush("#64B5F6");
        private static readonly SolidColorBrush BtnBtwRecording = Brush("#1E88E5");
        private static readonly SolidColorBrush BtnBtwPulse     = Brush("#90CAF9");
        // Special
        private static readonly SolidColorBrush BtnX         = Brush("#E53935");
        private static readonly SolidColorBrush BtnXPressed  = Brush("#FF6666");
        private static readonly SolidColorBrush BtnMicIdle   = Brush("#2A5DA8");
        // Copy/Paste buttons
        private static readonly SolidColorBrush BtnCopy      = Brush("#29B6F6");
        private static readonly SolidColorBrush BtnPaste     = Brush("#AB47BC");
        // Ultrathink star
        private static readonly SolidColorBrush BtnUltrathinkOn  = Brush("#B8860B");
        private static readonly SolidColorBrush StarGold         = Brush("#FFD700");
        private static readonly SolidColorBrush StarMuted        = Brush("#8B7355");

        // Pulse colours for main mic
        private static readonly SolidColorBrush BtnRecordingBright = Brush("#FF6666");

        // ── Services ──
        private readonly AudioRecorder     _audioRecorder;
        private readonly GroqWhisperClient _groqClient;
        private readonly GeminiClient?     _geminiClient;
        private readonly AppWatcher        _appWatcher;

        // ── State ──
        private RecordingState _micState    = RecordingState.Idle;
        private bool _isProcessing          = false;
        private bool isBtwRecording         = false;
        private bool geminiEnabled          = true;  // G-button on by default (falls back to false if no Gemini key)
        private bool autoEnterEnabled       = true;  // macOS default (was false in Windows)
        private bool hasPastedText          = false;
        private bool ultrathinkEnabled      = false;
        private string? lastRawTranscript   = null;

        // ── Timers ──
        private readonly DispatcherTimer _pulseTimer;
        private readonly DispatcherTimer _btwPulseTimer;
        private readonly DispatcherTimer _resetTimer;
        // Drueckt die Pille periodisch zurueck nach ganz oben im Z-Order —
        // sonst verdraengen Desktop-Widgets sie bei langen Aufnahmen mit
        // Fensterwechsel (Bugfix 2026-05-10, parallel zum Terminal-Projekt).
        private readonly DispatcherTimer _topmostAssertTimer;
        private bool _pulseBright    = false;
        private bool _btwPulseBright = false;

        // ── Waveform-Visualizer (Pegel-Anzeige im Mic-Button) ──
        // 14 Striche, je 2px breit mit 1px Spacing → Gesamtbreite 41px,
        // zentriert im 48px-Canvas. Buffer haelt die letzten 14 Pegelwerte
        // (0..1); neue Werte kommen rechts rein, alte fallen links raus —
        // die Welle "fliesst" optisch nach links.
        private const int  WaveformBarCount  = 14;
        private const double WaveformBarWidth   = 2.0;
        private const double WaveformBarSpacing = 1.0;
        private const double WaveformCanvasH    = 48.0;
        private const double WaveformMinH       = 3.0;
        private const double WaveformMaxH       = 40.0;
        private readonly float[] _waveformBuffer = new float[WaveformBarCount];
        private readonly System.Windows.Shapes.Rectangle[] _waveformBars =
            new System.Windows.Shapes.Rectangle[WaveformBarCount];

        // ── Constructor ──

        public OverlayWindow(Config config)
        {
            InitializeComponent();

            _audioRecorder = new AudioRecorder(config.AudioSampleRate, config.AudioChannels);
            _groqClient    = new GroqWhisperClient(config.GroqApiKey, config.WhisperModel, config.WhisperLang, config.WhisperUrl);
            _appWatcher    = new AppWatcher(config.TargetProcessNames);

            if (config.GeminiAvailable)
                _geminiClient = new GeminiClient(config.GeminiApiKey!, config.GeminiModel, config.GeminiThinkingLevel);

            // ── Pulse timer: main mic (500 ms, #FF6666 ↔ #E53935) ──
            _pulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _pulseTimer.Tick += (_, _) =>
            {
                _pulseBright = !_pulseBright;
                MicButton.Background = _pulseBright ? BtnRecordingBright : BtnRecording;
            };

            // ── Pulse timer: BTW mic (500 ms, #90CAF9 ↔ #1E88E5) ──
            _btwPulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _btwPulseTimer.Tick += (_, _) =>
            {
                _btwPulseBright = !_btwPulseBright;
                BtwButton.Background = _btwPulseBright ? BtnBtwPulse : BtnBtwRecording;
            };

            // ── Reset timer: 3 s back to idle after success/error ──
            _resetTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
            _resetTimer.Tick += (_, _) =>
            {
                _resetTimer.Stop();
                SetMicState(RecordingState.Idle);
            };

            // ── Topmost-Reassert-Timer (Bugfix 2026-05-10) ──
            // WPF Topmost="True" verliert bei langen Aufnahmen + haeufigem
            // Fensterwechsel gegen andere Topmost-Fenster (Desktop-Widgets,
            // Pop-ups). Alle 2,5 s die Pille per SetWindowPos(HWND_TOPMOST,
            // NOACTIVATE) zurueck nach ganz oben kicken — ohne Fokus zu
            // klauen, ohne Position zu aendern.
            _topmostAssertTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(2500) };
            _topmostAssertTimer.Tick += (_, _) => ReassertTopmostIfVisible();
            _topmostAssertTimer.Start();

            // ── Initial button colours ──
            // G-button is on by default — falls back to Whisper-raw if no Gemini API key.
            if (_geminiClient == null) geminiEnabled = false;
            XButton.Background      = BtnX;           // red
            WButton.Background      = geminiEnabled ? ToggleOff : ToggleOn;  // green when Gemini off (Whisper-raw)
            MicButton.Background       = BtnMicIdle;      // dark blue
            BtwButton.Background    = BtnBtwIdle;      // light blue
            GButton.Background    = geminiEnabled ? ToggleOn : ToggleOff;  // green when Gemini on
            EnterButton.Background = BtnProcessing;   // orange (autoEnter starts true)
            CopyButton.Background  = BtnCopy;         // light blue
            PasteButton.Background = BtnPaste;        // purple
            UltrathinkButton.Background = ToggleOff;   // dark (ultrathink starts disabled)

            // ── Waveform-Striche einmalig im Canvas anlegen ──
            // 14 weisse Rectangles mit voller Deckkraft auf dem roten
            // Recording-Hintergrund — klassischer VU-Meter-Look. Sie
            // werden hier nur erzeugt; die Hoehen-Animation passiert in
            // OnAudioLevelChanged.
            BuildWaveformBars();

            // ── Pegel-Listener: speist die Welle waehrend der Aufnahme ──
            _audioRecorder.LevelChanged += OnAudioLevelChanged;

            // ── Hover animations ──
            AttachHover(XButton);
            AttachHover(WButton);
            AttachHover(BtwButton);
            AttachHover(MicButton);
            AttachHover(GButton);
            AttachHover(UltrathinkButton);
            AttachHover(EnterButton);
            AttachHover(CopyButton);
            AttachHover(PasteButton);

            // ── App watcher ──
            _appWatcher.AppActivated   += OnAppActivated;
            _appWatcher.AppDeactivated += OnAppDeactivated;
            _appWatcher.Start();
        }

        // ── Hover animation helper ──

        private static void AttachHover(System.Windows.Controls.Button btn)
        {
            btn.RenderTransformOrigin = new Point(0.5, 0.5);
            btn.RenderTransform       = new ScaleTransform(1.0, 1.0);

            btn.MouseEnter += (_, _) => AnimateScale(btn, 1.15, TimeSpan.FromMilliseconds(150));
            btn.MouseLeave += (_, _) => AnimateScale(btn, 1.0,  TimeSpan.FromMilliseconds(150));
        }

        private static void AnimateScale(System.Windows.Controls.Button btn, double to, TimeSpan duration)
        {
            var ease  = new QuadraticEase { EasingMode = EasingMode.EaseOut };
            var animX = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = ease };
            var animY = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = ease };

            if (btn.RenderTransform is ScaleTransform st)
            {
                st.BeginAnimation(ScaleTransform.ScaleXProperty, animX);
                st.BeginAnimation(ScaleTransform.ScaleYProperty, animY);
            }
        }

        // ── Non-activating window setup ──

        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);

            var hwnd = new WindowInteropHelper(this).Handle;

            // Add WS_EX_NOACTIVATE + WS_EX_TOOLWINDOW
            var exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
            Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE,
                exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);

            // Hook WndProc for WM_MOUSEACTIVATE
            var source = HwndSource.FromHwnd(hwnd);
            source?.AddHook(WndProc);
        }

        private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
        {
            if (msg == Win32.WM_MOUSEACTIVATE)
            {
                handled = true;
                return (IntPtr)Win32.MA_NOACTIVATE;
            }
            return IntPtr.Zero;
        }

        // ── App watcher callbacks ──

        private void OnAppActivated(IntPtr appHwnd)
        {
            var workArea = AppWatcher.GetMonitorWorkArea(appHwnd);
            Left = workArea.X + workArea.Width - Width - 23;
            Top  = workArea.Y + (workArea.Height - Height) / 2;

            if (!IsVisible)
            {
                Show();
                Console.WriteLine("Overlay: visible (app active)");
            }

            // KRITISCH (Bugfix 2026-05-10): Auch wenn die Pille bereits sichtbar
            // war (z.B. waehrend laufender Aufnahme), muss sie aktiv zurueck
            // nach ganz oben — sonst bleibt sie hinter Desktop-Widgets.
            ReassertTopmostIfVisible();
        }

        /// <summary>
        /// Kickt die Pille per SetWindowPos(HWND_TOPMOST, NOACTIVATE) auf die
        /// absolute Topmost-Position im Z-Order zurueck. WPF Topmost=True
        /// allein reicht nicht: bei langen Aufnahmen mit Fensterwechsel
        /// verdraengen andere Topmost-Fenster die Pille sonst.
        /// </summary>
        private void ReassertTopmostIfVisible()
        {
            try
            {
                if (!IsVisible) return;
                var hwnd = new WindowInteropHelper(this).Handle;
                if (hwnd == IntPtr.Zero) return;
                Win32.SetWindowPos(
                    hwnd,
                    Win32.HWND_TOPMOST,
                    0, 0, 0, 0,
                    Win32.SWP_NOMOVE | Win32.SWP_NOSIZE | Win32.SWP_NOACTIVATE);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ReassertTopmost: {ex.Message}");
            }
        }

        private void OnAppDeactivated()
        {
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
                return;

            if (IsVisible)
            {
                Hide();
                Console.WriteLine("Overlay: hidden (app inactive)");
            }
        }

        // ── Button handlers ──

        /// <summary>X button — clear current input field.</summary>
        private async void BtnClear_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _appWatcher.ActiveAppHwnd;
            hasPastedText = false;

            // Flash X button: gray for 2 s then back to red
            XButton.Background = BtnIdle;

            // Run ClearInput on background thread (Thread.Sleep blocks UI thread)
            await Task.Run(() => AppController.ClearInput(hwnd));

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => XButton.Background = BtnX);
            });
        }

        /// <summary>Main mic button — start / stop recording.</summary>
        private async void BtnMic_Click(object sender, RoutedEventArgs e)
        {
            // Ignore if BTW mic is active
            if (isBtwRecording) return;
            if (_isProcessing)  return;

            if (_micState == RecordingState.Recording)
            {
                // ── Stop recording ──
                var wavFile = _audioRecorder.Stop();
                _ = Task.Run(() => { Console.Beep(660, 120); Console.Beep(440, 120); });
                _pulseTimer.Stop();
                _pulseBright = false;

                if (wavFile == null)
                {
                    SetMicState(RecordingState.Idle);
                    return;
                }

                _isProcessing = true;
                SetMicState(RecordingState.Processing);
                Console.WriteLine("Recording stopped, transcribing...");

                try
                {
                    var transcript = await _groqClient.TranscribeAsync(wavFile);
                    Console.WriteLine($"Transcript: {transcript}");
                    lastRawTranscript = transcript;

                    string finalText;
                    if (geminiEnabled && _geminiClient != null)
                    {
                        Console.WriteLine("Gemini correction...");
                        try
                        {
                            finalText = await _geminiClient.CorrectTextAsync(transcript);
                            Console.WriteLine($"Corrected: {finalText}");
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Gemini error: {ex.Message}, using raw text");
                            finalText = transcript;
                        }
                    }
                    else
                    {
                        finalText = transcript;
                    }

                    // Prepend ultrathink prefix if enabled
                    if (ultrathinkEnabled && !hasPastedText)
                        finalText = "ultrathink - " + finalText;

                    // Prepend space if text was already pasted on this line
                    if (hasPastedText)
                        finalText = " " + finalText;

                    AppController.PasteText(finalText, _appWatcher.ActiveAppHwnd, autoEnterEnabled);
                    SetMicState(RecordingState.Success);
                    ResetUltrathink();
                    Console.WriteLine("Text inserted");

                    // Track paste state
                    hasPastedText = !autoEnterEnabled;
                    if (autoEnterEnabled)
                        hasPastedText = false;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Transcription error: {ex.Message}");
                    SetMicState(RecordingState.Error);
                }
                finally
                {
                    _isProcessing = false;
                    ScheduleReset();

                    try { if (wavFile != null) File.Delete(wavFile); }
                    catch { /* ignore */ }
                }
            }
            else
            {
                // ── Start recording ──
                // KRITISCH: Reset-Timer aus der vorherigen Aufnahme stoppen.
                // Sonst feuert er ggf. mitten in der NEUEN Aufnahme und setzt
                // _micState auf Idle zurueck — UI sieht aus als waere die
                // Aufnahme aus, _audioRecorder laeuft aber weiter (State-Drift
                // bei schnellen aufeinanderfolgenden Aufnahmen).
                _resetTimer.Stop();
                try
                {
                    _audioRecorder.Start();
                    SetMicState(RecordingState.Recording);
                    _ = Task.Run(() => Console.Beep(880, 150));
                    Console.WriteLine("Recording started");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Microphone error: {ex.Message}");
                    SetMicState(RecordingState.Error);
                    ScheduleReset();
                }
            }
        }

        /// <summary>BTW mic button — record and prepend "/btw " to the text.</summary>
        private async void BtnBtw_Click(object sender, RoutedEventArgs e)
        {
            // Ignore if main mic is active
            if (_micState == RecordingState.Recording) return;
            if (_isProcessing) return;

            if (isBtwRecording)
            {
                // ── Stop BTW recording ──
                var wavFile = _audioRecorder.Stop();
                _ = Task.Run(() => { Console.Beep(660, 120); Console.Beep(440, 120); });
                _btwPulseTimer.Stop();
                _btwPulseBright = false;
                isBtwRecording = false;

                if (wavFile == null)
                {
                    SetBtwMicState(RecordingState.Idle);
                    return;
                }

                _isProcessing = true;
                SetBtwMicState(RecordingState.Processing);
                Console.WriteLine("BTW recording stopped, transcribing...");

                try
                {
                    var transcript = await _groqClient.TranscribeAsync(wavFile);
                    Console.WriteLine($"BTW transcript: {transcript}");

                    string finalText;
                    if (geminiEnabled && _geminiClient != null)
                    {
                        Console.WriteLine("BTW Gemini correction...");
                        try
                        {
                            finalText = await _geminiClient.CorrectTextAsync(transcript);
                            Console.WriteLine($"BTW corrected: {finalText}");
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"BTW Gemini error: {ex.Message}, using raw text");
                            finalText = transcript;
                        }
                    }
                    else
                    {
                        finalText = transcript;
                    }

                    // Build prefix: ultrathink + /btw
                    var prefix = ultrathinkEnabled ? "ultrathink - /btw " : "/btw ";

                    // Prepend space if text was already pasted on this line, then prefix
                    if (hasPastedText)
                        finalText = " " + prefix + finalText;
                    else
                        finalText = prefix + finalText;

                    AppController.PasteText(finalText, _appWatcher.ActiveAppHwnd, autoEnterEnabled);
                    SetBtwMicState(RecordingState.Success);
                    ResetUltrathink();
                    Console.WriteLine("BTW text inserted");

                    hasPastedText = !autoEnterEnabled;
                    if (autoEnterEnabled)
                        hasPastedText = false;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"BTW transcription error: {ex.Message}");
                    SetBtwMicState(RecordingState.Error);
                }
                finally
                {
                    _isProcessing = false;

                    // Reset BTW button to idle after 3 s
                    await Task.Delay(3000);
                    if (!isBtwRecording)
                        SetBtwMicState(RecordingState.Idle);

                    try { if (wavFile != null) File.Delete(wavFile); }
                    catch { /* ignore */ }
                }
            }
            else
            {
                // ── Start BTW recording ──
                try
                {
                    isBtwRecording = true;
                    _audioRecorder.Start();
                    SetBtwMicState(RecordingState.Recording);
                    _ = Task.Run(() => Console.Beep(880, 150));
                    Console.WriteLine("BTW recording started");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"BTW microphone error: {ex.Message}");
                    isBtwRecording = false;
                    SetBtwMicState(RecordingState.Error);

                    await Task.Delay(3000);
                    SetBtwMicState(RecordingState.Idle);
                }
            }
        }

        /// <summary>W button — undo Gemini correction, paste raw Whisper text.</summary>
        private void BtnWhisperUndo_Click(object sender, RoutedEventArgs e)
        {
            if (lastRawTranscript == null) return;

            AppController.ClearInput(_appWatcher.ActiveAppHwnd);
            System.Threading.Thread.Sleep(100);
            AppController.PasteText(lastRawTranscript, _appWatcher.ActiveAppHwnd);
            hasPastedText = true;
            Console.WriteLine($"Whisper raw text inserted: {lastRawTranscript}");

            lastRawTranscript = null;
        }

        /// <summary>G button — toggle Gemini on/off.
        /// G=on → GButton green, WButton dark.
        /// G=off → GButton dark, WButton green (Whisper-raw active).</summary>
        private void BtnGemini_Click(object sender, RoutedEventArgs e)
        {
            if (_geminiClient == null) return;
            geminiEnabled = !geminiEnabled;

            if (geminiEnabled)
            {
                GButton.Background = ToggleOn;
                WButton.Background   = ToggleOff;
            }
            else
            {
                GButton.Background = ToggleOff;
                WButton.Background   = ToggleOn;
            }

            Console.WriteLine($"Gemini {(geminiEnabled ? "ON" : "OFF")}");
        }

        /// <summary>Enter button — toggle auto-enter.
        /// ON→OFF: button goes dark.
        /// OFF→ON: button goes orange AND fires Return immediately.</summary>
        private void BtnAutoEnter_Click(object sender, RoutedEventArgs e)
        {
            if (autoEnterEnabled)
            {
                // Turn OFF
                autoEnterEnabled = false;
                EnterButton.Background = ToggleOff;
                hasPastedText = false;
                Console.WriteLine("Auto-enter OFF");
            }
            else
            {
                // Turn ON → fire Return immediately
                autoEnterEnabled = true;
                EnterButton.Background = BtnProcessing;
                hasPastedText = false;
                Console.WriteLine("Auto-enter ON — firing Return");

                // Fire a Return key press into the active app
                AppController.PressReturn(_appWatcher.ActiveAppHwnd);
            }
        }

        /// <summary>Star button — one-shot ultrathink toggle.
        /// When ON, "ultrathink - " is prepended to the NEXT voice input only,
        /// then automatically turns off.</summary>
        private void BtnUltrathink_Click(object sender, RoutedEventArgs e)
        {
            ultrathinkEnabled = !ultrathinkEnabled;

            if (ultrathinkEnabled)
            {
                UltrathinkButton.Background = BtnUltrathinkOn;
                UltrathinkStar.Fill = StarGold;
            }
            else
            {
                UltrathinkButton.Background = ToggleOff;
                UltrathinkStar.Fill = StarMuted;
            }

            Console.WriteLine($"Ultrathink {(ultrathinkEnabled ? "ON (one-shot)" : "OFF")}");
        }

        /// <summary>Reset ultrathink after one-shot use (auto-disable after text insertion).</summary>
        private void ResetUltrathink()
        {
            if (!ultrathinkEnabled) return;
            ultrathinkEnabled = false;
            Dispatcher.Invoke(() =>
            {
                UltrathinkButton.Background = ToggleOff;
                UltrathinkStar.Fill = StarMuted;
            });
            Console.WriteLine("Ultrathink auto-OFF (one-shot used)");
        }

        /// <summary>C button — copy selected text via Ctrl+C.</summary>
        private void BtnCopy_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _appWatcher.ActiveAppHwnd;

            // Flash: gray for 2 s then back to blue
            CopyButton.Background = BtnIdle;

            AppController.CopySelection(hwnd);

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => CopyButton.Background = BtnCopy);
            });

            Console.WriteLine("Copy: Ctrl+C sent to app");
        }

        /// <summary>P button — paste clipboard content into input field via Ctrl+V.
        /// If auto-enter is enabled, sends Enter after paste.</summary>
        private void BtnPaste_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _appWatcher.ActiveAppHwnd;

            // Flash: gray for 2 s then back to purple
            PasteButton.Background = BtnIdle;

            AppController.PasteClipboard(hwnd);
            hasPastedText = true;

            if (autoEnterEnabled)
            {
                // Small delay then send Enter
                _ = Task.Run(() =>
                {
                    System.Threading.Thread.Sleep(300);
                    AppController.PressReturn(hwnd);
                });
                hasPastedText = false;
                Console.WriteLine("Paste+Enter: Ctrl+V → Return sent to app");
            }
            else
            {
                Console.WriteLine("Paste: Ctrl+V sent to app");
            }

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => PasteButton.Background = BtnPaste);
            });
        }

        // ── Mic state helpers ──

        private void SetMicState(RecordingState state)
        {
            _micState = state;
            _pulseTimer.Stop();
            _pulseBright = false;

            // Welle nur waehrend der Recording-Phase sichtbar.
            SetWaveformVisible(state == RecordingState.Recording);

            switch (state)
            {
                case RecordingState.Idle:
                    MicButton.Background = BtnMicIdle;
                    break;
                case RecordingState.Recording:
                    MicButton.Background = BtnRecording;
                    _pulseTimer.Start();
                    break;
                case RecordingState.Processing:
                    MicButton.Background = BtnProcessing;
                    break;
                case RecordingState.Success:
                    MicButton.Background = BtnSuccess;
                    break;
                case RecordingState.Error:
                    MicButton.Background = BtnX;
                    break;
            }
        }

        private void SetBtwMicState(RecordingState state)
        {
            _btwPulseTimer.Stop();
            _btwPulseBright = false;

            switch (state)
            {
                case RecordingState.Idle:
                    BtwButton.Background = BtnBtwIdle;
                    break;
                case RecordingState.Recording:
                    BtwButton.Background = BtnBtwRecording;
                    _btwPulseTimer.Start();
                    break;
                case RecordingState.Processing:
                    BtwButton.Background = BtnProcessing;
                    break;
                case RecordingState.Success:
                    BtwButton.Background = BtnSuccess;
                    break;
                case RecordingState.Error:
                    BtwButton.Background = BtnX;
                    break;
            }
        }

        private void ScheduleReset()
        {
            _resetTimer.Stop();
            _resetTimer.Start();
        }

        // ── Brush factory ──

        private static SolidColorBrush Brush(string hex)
        {
            var color = (Color)ColorConverter.ConvertFromString(hex);
            var brush  = new SolidColorBrush(color);
            brush.Freeze();
            return brush;
        }

        // ── Waveform-Visualizer ──

        /// <summary>
        /// Erzeugt die 14 weissen Strich-Rectangles und legt sie im
        /// WaveformCanvas ab. Wird einmal beim Konstruktor aufgerufen.
        /// </summary>
        private void BuildWaveformBars()
        {
            if (WaveformCanvas == null) return;

            const double startOffset =
                (48.0 - (WaveformBarCount * WaveformBarWidth
                         + (WaveformBarCount - 1) * WaveformBarSpacing)) / 2.0;

            for (int i = 0; i < WaveformBarCount; i++)
            {
                var bar = new System.Windows.Shapes.Rectangle
                {
                    Width = WaveformBarWidth,
                    Height = WaveformMinH,
                    RadiusX = 1.0,
                    RadiusY = 1.0,
                    Fill = System.Windows.Media.Brushes.White,
                };
                double x = startOffset + i * (WaveformBarWidth + WaveformBarSpacing);
                System.Windows.Controls.Canvas.SetLeft(bar, x);
                System.Windows.Controls.Canvas.SetTop(bar, (WaveformCanvasH - WaveformMinH) / 2.0);
                WaveformCanvas.Children.Add(bar);
                _waveformBars[i] = bar;
            }
        }

        /// <summary>
        /// Wird vom AudioRecorder pro Buffer (~100ms) aufgerufen. Pegel
        /// wird leicht verstaerkt (Wurzel + Faktor) damit normale Sprech-
        /// lautstaerke ausgepraegte Striche ergibt. Buffer rotiert: neuer
        /// Wert kommt rechts rein, alte fallen links raus.
        /// </summary>
        private void OnAudioLevelChanged(float level)
        {
            Dispatcher.BeginInvoke(new Action(() =>
            {
                if (WaveformCanvas == null || WaveformCanvas.Visibility != Visibility.Visible)
                    return;

                float boosted = MathF.Min(1f, MathF.Sqrt(level) * 1.6f);

                for (int i = 0; i < WaveformBarCount - 1; i++)
                    _waveformBuffer[i] = _waveformBuffer[i + 1];
                _waveformBuffer[WaveformBarCount - 1] = boosted;

                for (int i = 0; i < WaveformBarCount; i++)
                {
                    if (_waveformBars[i] == null) continue;
                    double h = WaveformMinH + _waveformBuffer[i] * (WaveformMaxH - WaveformMinH);
                    _waveformBars[i].Height = h;
                    System.Windows.Controls.Canvas.SetTop(
                        _waveformBars[i], (WaveformCanvasH - h) / 2.0);
                }
            }));
        }

        /// <summary>
        /// Schaltet zwischen Mikrofon-Emoji (Idle) und Wellenanzeige
        /// (Recording) um. Beim Wechsel auf "Welle" wird der Buffer
        /// auf null gesetzt.
        /// </summary>
        private void SetWaveformVisible(bool visible)
        {
            if (WaveformCanvas == null || MicIcon == null) return;
            if (visible)
            {
                Array.Clear(_waveformBuffer, 0, _waveformBuffer.Length);
                for (int i = 0; i < WaveformBarCount; i++)
                {
                    if (_waveformBars[i] == null) continue;
                    _waveformBars[i].Height = WaveformMinH;
                    System.Windows.Controls.Canvas.SetTop(
                        _waveformBars[i], (WaveformCanvasH - WaveformMinH) / 2.0);
                }
                MicIcon.Visibility = Visibility.Collapsed;
                WaveformCanvas.Visibility = Visibility.Visible;
            }
            else
            {
                WaveformCanvas.Visibility = Visibility.Collapsed;
                MicIcon.Visibility = Visibility.Visible;
            }
        }

        // ── Cleanup ──

        protected override void OnClosed(EventArgs e)
        {
            _pulseTimer.Stop();
            _btwPulseTimer.Stop();
            _resetTimer.Stop();
            _topmostAssertTimer.Stop();
            _audioRecorder.LevelChanged -= OnAudioLevelChanged;
            _appWatcher.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
