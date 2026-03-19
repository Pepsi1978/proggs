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
        private bool geminiEnabled          = false; // macOS default
        private bool autoEnterEnabled       = true;  // macOS default (was false in Windows)
        private bool hasPastedText          = false;
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

            // ── Initial button colours ──
            XButton.Background      = BtnX;           // red
            WButton.Background      = ToggleOn;        // green  (Whisper-raw active, Gemini off)
            MicButton.Background       = BtnMicIdle;      // dark blue
            BtwButton.Background    = BtnBtwIdle;      // light blue
            GButton.Background    = ToggleOff;       // dark   (Gemini starts disabled)
            EnterButton.Background = BtnProcessing;   // orange (autoEnter starts true)

            // ── Hover animations ──
            AttachHover(XButton);
            AttachHover(WButton);
            AttachHover(BtwButton);
            AttachHover(MicButton);
            AttachHover(GButton);
            AttachHover(EnterButton);

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

        // ── App watcher callbacks ──

        private void OnAppActivated(IntPtr appHwnd)
        {
            if (!_manuallyPositioned)
            {
                var workArea = AppWatcher.GetMonitorWorkArea(appHwnd);
                Left = workArea.X + workArea.Width - Width - 23;
                Top  = workArea.Y + (workArea.Height - Height) / 4;
            }

            if (!IsVisible)
            {
                Show();
                Console.WriteLine("Overlay: visible (app active)");
            }
        }

        private void OnAppDeactivated()
        {
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
                return;

            if (IsVisible)
            {
                _manuallyPositioned = false;
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

            // Clear input with proper async/await
            await AppController.ClearInputAsync(hwnd);

            // Reset X button color after 2 seconds
            await Task.Delay(2000);
            XButton.Background = BtnX;
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

                    // Prepend space if text was already pasted on this line
                    if (hasPastedText)
                        finalText = " " + finalText;

                    await AppController.PasteTextAsync(finalText, _appWatcher.ActiveAppHwnd, autoEnterEnabled);
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

                    // Prepend space if text was already pasted on this line, then /btw prefix
                    if (hasPastedText)
                        finalText = " /btw " + finalText;
                    else
                        finalText = "/btw " + finalText;

                    await AppController.PasteTextAsync(finalText, _appWatcher.ActiveAppHwnd, autoEnterEnabled);
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
        private async void BtnWhisperUndo_Click(object sender, RoutedEventArgs e)
        {
            if (lastRawTranscript == null) return;

            await AppController.ClearInputAsync(_appWatcher.ActiveAppHwnd);
            await Task.Delay(100);
            await AppController.PasteTextAsync(lastRawTranscript, _appWatcher.ActiveAppHwnd);
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
        private async void BtnAutoEnter_Click(object sender, RoutedEventArgs e)
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
                await AppController.PressReturnAsync(_appWatcher.ActiveAppHwnd);
            }
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
            _appWatcher.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
