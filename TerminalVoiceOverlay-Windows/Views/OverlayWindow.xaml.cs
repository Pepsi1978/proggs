using System;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Threading;
using TerminalVoiceOverlay.Models;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views
{
    public partial class OverlayWindow : Window
    {
        // ── Colors ──
        private static readonly SolidColorBrush BrushIdle = new(Color.FromRgb(0x2D, 0x2D, 0x2D));
        private static readonly SolidColorBrush BrushRecording = new(Color.FromRgb(0xE5, 0x39, 0x35));
        private static readonly SolidColorBrush BrushRecordingBright = new(Color.FromRgb(0xFF, 0x66, 0x66));
        private static readonly SolidColorBrush BrushProcessing = new(Color.FromRgb(0xFF, 0x98, 0x00));
        private static readonly SolidColorBrush BrushSuccess = new(Color.FromRgb(0x43, 0xA0, 0x47));
        private static readonly SolidColorBrush BrushGeminiOn = new(Color.FromRgb(0x22, 0xC5, 0x5E));
        private static readonly SolidColorBrush BrushGeminiOff = new(Color.FromRgb(0xEF, 0x44, 0x44));

        // ── Services ──
        private readonly AudioRecorder _audioRecorder = new();
        private readonly GroqWhisperClient _groqClient;
        private readonly GeminiClient? _geminiClient;
        private readonly TerminalWatcher _terminalWatcher = new();

        // ── State ──
        private RecordingState _micState = RecordingState.Idle;
        private bool _isProcessing;
        private bool _geminiEnabled = true;
        private readonly DispatcherTimer _pulseTimer;
        private readonly DispatcherTimer _resetTimer;
        private bool _pulseBright;

        public OverlayWindow(Config config)
        {
            InitializeComponent();

            _groqClient = new GroqWhisperClient(config.GroqApiKey);
            if (config.GeminiAvailable)
            {
                _geminiClient = new GeminiClient(config.GeminiApiKey!);
            }
            else
            {
                _geminiEnabled = false;
            }

            // Pulse timer (0.5s toggle for recording animation)
            _pulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _pulseTimer.Tick += (_, _) =>
            {
                _pulseBright = !_pulseBright;
                BtnMic.Background = _pulseBright ? BrushRecordingBright : BrushRecording;
            };

            // Reset timer (3s back to idle after success/error)
            _resetTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
            _resetTimer.Tick += (_, _) =>
            {
                _resetTimer.Stop();
                SetMicState(RecordingState.Idle);
            };

            // Set initial Gemini button state
            UpdateGeminiButton();

            // Terminal watcher events
            _terminalWatcher.TerminalActivated += OnTerminalActivated;
            _terminalWatcher.TerminalDeactivated += OnTerminalDeactivated;
            _terminalWatcher.Start();
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

        // ── Terminal watcher callbacks ──

        private void OnTerminalActivated(IntPtr terminalHwnd)
        {
            // Position overlay on the correct monitor
            var workArea = TerminalWatcher.GetMonitorWorkArea(terminalHwnd);
            Left = workArea.X + workArea.Width - Width - 4;
            Top = workArea.Y + (workArea.Height - Height) / 2;

            if (!IsVisible)
            {
                Show();
                Debug.WriteLine("Overlay: sichtbar (Terminal aktiv)");
            }
        }

        private void OnTerminalDeactivated()
        {
            // Don't hide while recording or processing
            if (_micState == RecordingState.Recording || _isProcessing)
                return;

            if (IsVisible)
            {
                Hide();
                Debug.WriteLine("Overlay: versteckt (Terminal nicht aktiv)");
            }
        }

        // ── Button handlers ──

        private void BtnClear_Click(object sender, RoutedEventArgs e)
        {
            TerminalController.ClearLine();
        }

        private async void BtnMic_Click(object sender, RoutedEventArgs e)
        {
            if (_isProcessing) return;

            if (_micState == RecordingState.Recording)
            {
                // ── Stop recording ──
                var wavFile = _audioRecorder.Stop();
                if (wavFile == null)
                {
                    SetMicState(RecordingState.Idle);
                    return;
                }

                _isProcessing = true;
                SetMicState(RecordingState.Processing);
                Debug.WriteLine("Aufnahme gestoppt, transkribiere...");

                try
                {
                    var transcript = await _groqClient.TranscribeAsync(wavFile);
                    Debug.WriteLine($"Transkription: {transcript}");

                    string finalText;
                    if (_geminiEnabled && _geminiClient != null)
                    {
                        Debug.WriteLine("Gemini-Korrektur...");
                        try
                        {
                            finalText = await _geminiClient.CorrectTextAsync(transcript);
                            Debug.WriteLine($"Korrigiert: {finalText}");
                        }
                        catch (Exception ex)
                        {
                            Debug.WriteLine($"Gemini-Fehler: {ex.Message}, verwende Rohtext");
                            finalText = transcript;
                        }
                    }
                    else
                    {
                        finalText = transcript;
                    }

                    TerminalController.PasteText(finalText);
                    SetMicState(RecordingState.Success);
                    Debug.WriteLine("Text eingefügt");
                }
                catch (Exception ex)
                {
                    Debug.WriteLine($"Transkriptionsfehler: {ex.Message}");
                    SetMicState(RecordingState.Error);
                }
                finally
                {
                    _isProcessing = false;
                    ScheduleReset();

                    // Clean up temp file
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
                    Debug.WriteLine("Aufnahme gestartet");
                }
                catch (Exception ex)
                {
                    Debug.WriteLine($"Mikrofon-Fehler: {ex.Message}");
                    SetMicState(RecordingState.Error);
                    ScheduleReset();
                }
            }
        }

        private void BtnGemini_Click(object sender, RoutedEventArgs e)
        {
            if (_geminiClient == null) return;
            _geminiEnabled = !_geminiEnabled;
            UpdateGeminiButton();
            Debug.WriteLine($"Gemini {(_geminiEnabled ? "AN" : "AUS")}");
        }

        // ── State management ──

        private void SetMicState(RecordingState state)
        {
            _micState = state;
            _pulseTimer.Stop();
            _pulseBright = false;

            switch (state)
            {
                case RecordingState.Idle:
                    BtnMic.Background = BrushIdle;
                    break;
                case RecordingState.Recording:
                    BtnMic.Background = BrushRecording;
                    _pulseTimer.Start();
                    break;
                case RecordingState.Processing:
                    BtnMic.Background = BrushProcessing;
                    break;
                case RecordingState.Success:
                    BtnMic.Background = BrushSuccess;
                    break;
                case RecordingState.Error:
                    BtnMic.Background = BrushRecording;
                    break;
            }
        }

        private void UpdateGeminiButton()
        {
            BtnGemini.Background = _geminiEnabled ? BrushGeminiOn : BrushGeminiOff;
        }

        private void ScheduleReset()
        {
            _resetTimer.Stop();
            _resetTimer.Start();
        }

        // ── Cleanup ──

        protected override void OnClosed(EventArgs e)
        {
            _pulseTimer.Stop();
            _resetTimer.Stop();
            _terminalWatcher.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
