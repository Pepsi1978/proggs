using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using TerminalVoiceOverlay.Models;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views
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
        private readonly TerminalWatcher   _terminalWatcher;

        // ── State ──
        private RecordingState _micState    = RecordingState.Idle;
        private bool _isProcessing          = false;
        private bool isBtwRecording         = false;
        private bool geminiEnabled          = false; // macOS default
        private bool autoEnterEnabled       = true;  // macOS default (was false in Windows)
        private bool hasPastedText          = false;
        private bool alwaysOnActive         = false;

        // PromptBoard integration: on-demand prefix lookup + side panel.
        private IAlwaysOnPrefixService? _alwaysOnPrefix;
        private PromptBoardPanel? _promptPanel;
        private string? lastRawTranscript   = null;

        // ── Right-click drag state ──
        private bool _isDragging;
        private bool _manuallyPositioned;
        private int _dragStartCursorX, _dragStartCursorY;
        private double _dragStartLeft, _dragStartTop;
        private double _dragDpiX, _dragDpiY;

        // ── Timers ──
        private readonly DispatcherTimer _pulseTimer;
        private readonly DispatcherTimer _btwPulseTimer;
        private readonly DispatcherTimer _resetTimer;
        private bool _pulseBright    = false;
        private bool _btwPulseBright = false;

        // ── Constructor ──

        public OverlayWindow(Config config)
        {
            InitializeComponent();

            _audioRecorder   = new AudioRecorder(config.AudioSampleRate, config.AudioChannels);
            _groqClient      = new GroqWhisperClient(config.GroqApiKey, config.WhisperModel, config.WhisperLang, config.WhisperUrl);
            _terminalWatcher = new TerminalWatcher(config.TerminalProcessNames);

            if (config.GeminiAvailable)
                _geminiClient = new GeminiClient(config.GeminiApiKey!, config.GeminiModel, config.GeminiThinkingLevel);

            // Share the audio/STT/Gemini stack with secondary surfaces
            // (e.g. PromptEditDialog's mic + G buttons). Single AudioRecorder
            // instance is critical — only one process can hold the microphone.
            VoiceServiceProvider.Initialize(_audioRecorder, _groqClient, _geminiClient);

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

            // ── Initial button colours ──
            XButton.Background    = BtnX;           // red
            WButton.Background    = ToggleOn;        // green  (Whisper-raw active, Gemini off)
            MicButton.Background  = BtnMicIdle;      // dark blue
            BtwButton.Background  = BtnBtwIdle;      // light blue
            GButton.Background    = ToggleOff;       // dark   (Gemini starts disabled)
            EnterButton.Background = BtnProcessing;  // orange (autoEnter starts true)
            CopyButton.Background  = BtnCopy;        // light blue
            PasteButton.Background = BtnPaste;       // purple
            UltrathinkButton.Background = ToggleOff;  // dark (PromptBoard always-on prefix starts disabled)

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

            // ── Terminal watcher ──
            _terminalWatcher.TerminalActivated   += OnTerminalActivated;
            _terminalWatcher.TerminalDeactivated += OnTerminalDeactivated;
            _terminalWatcher.Start();

            // Resolve the PromptBoard prefix service if the DI host is up.
            // Silent fallback when PromptBoard is unavailable — the star
            // button simply toggles nothing in that case.
            try
            {
                _alwaysOnPrefix = PromptBoardHost.Get<IAlwaysOnPrefixService>();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AlwaysOnPrefixService not available: {ex.Message}");
                _alwaysOnPrefix = null;
            }
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
            switch (msg)
            {
                case Win32.WM_MOUSEACTIVATE:
                    handled = true;
                    return (IntPtr)Win32.MA_NOACTIVATE;

                case Win32.WM_RBUTTONDOWN:
                    if (Win32.GetCursorPos(out var startPt))
                    {
                        _isDragging = true;
                        _dragStartCursorX = startPt.X;
                        _dragStartCursorY = startPt.Y;
                        _dragStartLeft = Left;
                        _dragStartTop = Top;
                        var src = PresentationSource.FromVisual(this);
                        _dragDpiX = src?.CompositionTarget?.TransformToDevice.M11 ?? 1.0;
                        _dragDpiY = src?.CompositionTarget?.TransformToDevice.M22 ?? 1.0;
                        Win32.SetCapture(hwnd);
                    }
                    handled = true;
                    break;

                case Win32.WM_MOUSEMOVE:
                    if (_isDragging && Win32.GetCursorPos(out var movePt))
                    {
                        Left = _dragStartLeft + (movePt.X - _dragStartCursorX) / _dragDpiX;
                        Top  = _dragStartTop  + (movePt.Y - _dragStartCursorY) / _dragDpiY;
                        // Keep the PromptBoard side panel glued to the
                        // pillar's left edge during right-click drag.
                        if (_promptPanel is not null && _promptPanel.IsVisible)
                            PositionPromptPanel();
                    }
                    break;

                case Win32.WM_RBUTTONUP:
                    if (_isDragging)
                    {
                        _isDragging = false;
                        _manuallyPositioned = true;
                        Win32.ReleaseCapture();
                        handled = true;
                    }
                    break;
            }
            return IntPtr.Zero;
        }

        // ── Terminal watcher callbacks ──

        private void OnTerminalActivated(IntPtr terminalHwnd)
        {
            if (!_manuallyPositioned)
            {
                var workArea = TerminalWatcher.GetMonitorWorkArea(terminalHwnd);
                // 2 mm closer to the right edge: 23 - 8 = 15 px.
                Left = workArea.X + workArea.Width - Width - 15;
                // Anchor to the top-right corner of the work area instead
                // of the vertical center — Frank prefers the pillar high
                // so it doesn't cover terminal output. 20 px breathing
                // room from the very top edge. The saved-position path
                // above still wins once the user drags the pillar manually.
                Top  = workArea.Y + 20;
            }

            if (!IsVisible)
            {
                Show();
                Console.WriteLine("Overlay: visible (terminal active)");
            }

            // Star toggle is on but the panel was hidden during a previous
            // terminal-deactivation? Bring it back alongside the pillar so
            // both windows behave as a single unit per user expectation.
            if (alwaysOnActive && _promptPanel is not null && !_promptPanel.IsVisible)
            {
                PositionPromptPanel();
                _promptPanel.Show();
            }
        }

        private void OnTerminalDeactivated()
        {
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
                return;

            // Hide (not Close) the panel so its state — selected category,
            // edit-in-progress, scroll position — survives until the user
            // returns to the terminal. Closing would null _promptPanel and
            // also flip alwaysOnActive off via the Closed handler, losing
            // the user's intent.
            if (_promptPanel is not null && _promptPanel.IsVisible)
            {
                _promptPanel.Hide();
            }

            if (IsVisible)
            {
                _manuallyPositioned = false;
                Hide();
                Console.WriteLine("Overlay: hidden (terminal inactive)");
            }
        }

        // ── Button handlers ──

        /// <summary>X button — clear current terminal line.</summary>
        private async void BtnClear_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;
            hasPastedText = false;

            // Flash X button: gray for 2 s then back to red
            XButton.Background = BtnIdle;

            // Run ClearLine on background thread (Thread.Sleep blocks UI thread)
            await Task.Run(() => TerminalController.ClearLine(hwnd));

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

                    // Wrap the dictation with PromptBoard always-on prompts
                    // when the star toggle is active. Pre-prompts go before,
                    // post-prompts after; both are independent so a prompt
                    // can wrap the dictation on both sides if both flags
                    // are set. Only on the first paste per line — follow-ups
                    // are appended to the existing line without wrapping.
                    if (!hasPastedText)
                    {
                        var (preFix, postFix) = await BuildAlwaysOnWrappersAsync();
                        if (!string.IsNullOrEmpty(preFix))
                            finalText = preFix + finalText;
                        if (!string.IsNullOrEmpty(postFix))
                            finalText = finalText + postFix;
                    }

                    // Always append " ; " after the dictated text — inline
                    // space + semicolon + space marks every dictation as
                    // its own task without forcing line breaks in the
                    // terminal. Applies regardless of auto-enter.
                    finalText = finalText + " ; ";

                    TerminalController.PasteText(finalText, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                    SetMicState(RecordingState.Success);
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

                    // BTW prefix stays simple (no always-on chaining here —
                    // BTW lines are short asides, not full prompts).
                    const string btwMarker = "/btw ";

                    // Prepend space if text was already pasted on this line, then prefix
                    if (hasPastedText)
                        finalText = " " + btwMarker + finalText;
                    else
                        finalText = btwMarker + finalText;

                    TerminalController.PasteText(finalText, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                    SetBtwMicState(RecordingState.Success);
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

            TerminalController.ClearLine(_terminalWatcher.ActiveTerminalHwnd);
            System.Threading.Thread.Sleep(100);
            TerminalController.PasteText(lastRawTranscript, _terminalWatcher.ActiveTerminalHwnd);
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
                WButton.Background = ToggleOff;
            }
            else
            {
                GButton.Background = ToggleOff;
                WButton.Background = ToggleOn;
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

                // Fire a Return key press into the active terminal
                TerminalController.PressReturn(_terminalWatcher.ActiveTerminalHwnd);
            }
        }

        /// <summary>Star button — toggles the full PromptBoard integration:
        /// opens/closes the side panel AND enables/disables the always-on
        /// prefix. The panel docks to the right of the pillar so it looks
        /// like a single unit.</summary>
        private void BtnUltrathink_Click(object sender, RoutedEventArgs e)
        {
            alwaysOnActive = !alwaysOnActive;

            if (alwaysOnActive)
            {
                UltrathinkButton.Background = BtnUltrathinkOn;
                UltrathinkStar.Fill = StarGold;
                ShowPromptPanel();
            }
            else
            {
                UltrathinkButton.Background = ToggleOff;
                UltrathinkStar.Fill = StarMuted;
                HidePromptPanel();
            }

            Console.WriteLine($"PromptBoard panel {(alwaysOnActive ? "OPEN" : "CLOSED")}");
        }

        private void ShowPromptPanel()
        {
            if (_promptPanel is null)
            {
                _promptPanel = new PromptBoardPanel();
                _promptPanel.PromptInsertRequested += OnPromptPanelInsert;
                // Right-click drag on the panel itself moves both the
                // panel (handled inside) and this pillar window — slide
                // the pillar to stay glued to the panel's right edge.
                _promptPanel.PanelDragged += () =>
                {
                    if (_promptPanel is null) return;
                    Left = _promptPanel.Left + _promptPanel.Width + 4;
                    Top  = _promptPanel.Top;
                    _manuallyPositioned = true;
                };
                _promptPanel.Closed += (_, _) =>
                {
                    _promptPanel = null;
                    // If the panel was closed by something other than the
                    // star toggle, keep the toggle state in sync.
                    if (alwaysOnActive)
                    {
                        alwaysOnActive = false;
                        UltrathinkButton.Background = ToggleOff;
                        UltrathinkStar.Fill = StarMuted;
                    }
                };
            }

            PositionPromptPanel();
            _promptPanel.Show();
            _ = _promptPanel.RefreshAsync();
        }

        private void HidePromptPanel()
        {
            if (_promptPanel is null) return;
            var p = _promptPanel;
            _promptPanel = null;
            p.Close();
        }

        private void PositionPromptPanel()
        {
            if (_promptPanel is null) return;
            // Dock the panel directly to the LEFT of the pillar with a
            // 4-pixel seam. The VTO typically sits at the right screen edge,
            // so docking to the left keeps the panel on-screen. Match the
            // pillar's HEIGHT exactly so the two floating windows visually
            // line up — the user asked for a uniform vertical extent so
            // the panel doesn't look stubby next to the bar (or vice-versa).
            _promptPanel.Height = Height;
            _promptPanel.Left = Left - _promptPanel.Width - 4;
            _promptPanel.Top = Top;
        }

        private void OnPromptPanelInsert(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            try
            {
                TerminalController.PasteText(text, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                Console.WriteLine($"Panel prompt inserted: {text.Length} chars.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Panel insert failed: {ex.Message}");
            }
        }

        /// <summary>Build the PromptBoard always-on Pre AND Post wrappers
        /// when the star toggle is active. Returns (empty, empty) when
        /// the toggle is off, the service is unavailable, or no
        /// IsAlwaysOn prompts exist.</summary>
        private async Task<(string Pre, string Post)> BuildAlwaysOnWrappersAsync()
        {
            if (!alwaysOnActive || _alwaysOnPrefix is null) return (string.Empty, string.Empty);

            try
            {
                string pre = await _alwaysOnPrefix.BuildPreAsync();
                string post = await _alwaysOnPrefix.BuildPostAsync();
                return (pre ?? string.Empty, post ?? string.Empty);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AlwaysOn wrappers build failed: {ex.Message}");
                return (string.Empty, string.Empty);
            }
        }

        /// <summary>C button — copy selected text via Ctrl+C.</summary>
        private void BtnCopy_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;

            // Flash: gray for 2 s then back to blue
            CopyButton.Background = BtnIdle;

            TerminalController.CopySelection(hwnd);

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => CopyButton.Background = BtnCopy);
            });

            Console.WriteLine("Copy: Ctrl+C sent to terminal");
        }

        /// <summary>P button — paste clipboard content into command line via Ctrl+V.
        /// If auto-enter is enabled, sends Enter after paste.</summary>
        private void BtnPaste_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;

            // Flash: gray for 2 s then back to purple
            PasteButton.Background = BtnIdle;

            TerminalController.PasteClipboard(hwnd);
            hasPastedText = true;

            if (autoEnterEnabled)
            {
                // Small delay then send Enter
                _ = Task.Run(() =>
                {
                    System.Threading.Thread.Sleep(300);
                    TerminalController.PressReturn(hwnd);
                });
                hasPastedText = false;
                Console.WriteLine("Paste+Enter: Ctrl+V → Return sent to terminal");
            }
            else
            {
                Console.WriteLine("Paste: Ctrl+V sent to terminal");
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

        // ── Cleanup ──

        protected override void OnClosed(EventArgs e)
        {
            _pulseTimer.Stop();
            _btwPulseTimer.Stop();
            _resetTimer.Stop();
            _terminalWatcher.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
