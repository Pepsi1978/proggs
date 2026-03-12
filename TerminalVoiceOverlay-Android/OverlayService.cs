using Android.App;
using Android.Content;
using Android.Graphics;
using Android.Graphics.Drawables;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay;

// Recording lifecycle states — mirrors the Windows version's RecordingState enum.
public enum RecordingState { Idle, Recording, Processing, Success, Error }

// Foreground service that hosts the floating overlay window.
// Requires SYSTEM_ALERT_WINDOW + FOREGROUND_SERVICE_MICROPHONE permissions.
[Service(Name = "com.pepsi1978.terminalvoiceoverlay.OverlayService",
         Exported = false,
         ForegroundServiceType = Android.Content.PM.ForegroundService.TypeMicrophone)]
public class OverlayService : Service
{
    // ── Notification ──────────────────────────────────────────────────────────
    private const string ChannelId   = "voice_overlay_channel";
    private const int    NotifId     = 1;

    // ── Colour palette (mirrors Windows version exactly) ─────────────────────
    private static readonly Color ColBtnIdle          = Color.ParseColor("#2D2D2D");
    private static readonly Color ColBtnMicIdle       = Color.ParseColor("#2A5DA8");
    private static readonly Color ColBtnBtwIdle       = Color.ParseColor("#64B5F6");
    private static readonly Color ColBtnX             = Color.ParseColor("#E53935");
    private static readonly Color ColBtnRecording     = Color.ParseColor("#E53935");
    private static readonly Color ColBtnRecordingBrt  = Color.ParseColor("#FF6666");
    private static readonly Color ColBtnBtwRecording  = Color.ParseColor("#1E88E5");
    private static readonly Color ColBtnBtwPulse      = Color.ParseColor("#90CAF9");
    private static readonly Color ColBtnProcessing    = Color.ParseColor("#FF9800");
    private static readonly Color ColBtnSuccess       = Color.ParseColor("#43A047");
    private static readonly Color ColToggleOn         = Color.ParseColor("#16a34a");
    private static readonly Color ColToggleOff        = Color.ParseColor("#2D2D2D");

    // ── State ────────────────────────────────────────────────────────────────
    private RecordingState _micState        = RecordingState.Idle;
    private bool           _isBtwRecording  = false;
    private bool           _geminiEnabled   = false;
    private bool           _autoEnterEnabled = true;
    private bool           _hasPastedText   = false;
    private string?        _lastRawTranscript = null;

    // Pulse phase flags
    private bool _micPulsePhase  = false;
    private bool _btwPulsePhase  = false;

    // ── Services ─────────────────────────────────────────────────────────────
    private Config?             _config;
    private AudioRecorder?      _recorder;
    private AudioRecorder?      _btwRecorder;
    private GroqWhisperClient?  _whisper;
    private GeminiClient?       _gemini;

    // ── Android system services ───────────────────────────────────────────────
    private IWindowManager?     _windowManager;
    private ClipboardManager?   _clipboard;
    private Vibrator?           _vibrator;
    private Handler             _handler = new Handler(Looper.MainLooper!);

    // ── Timers (Handler-based, main thread) ──────────────────────────────────
    private Java.Lang.Runnable? _micPulseRunnable;
    private Java.Lang.Runnable? _btwPulseRunnable;
    private Java.Lang.Runnable? _micResetRunnable;
    private Java.Lang.Runnable? _btwResetRunnable;
    private Java.Lang.Runnable? _xFlashRunnable;

    // ── Views ─────────────────────────────────────────────────────────────────
    private View?   _overlayRoot;
    private View? _btnX;
    private View? _btnW;
    private View? _btnBtw;
    private View? _btnMic;
    private View? _btnG;
    private View? _btnEnter;

    // ── Drag tracking ────────────────────────────────────────────────────────
    private int  _dragInitialY;
    private int  _dragTouchStartY;
    private bool _isDragging;

    // ─────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    public override void OnCreate()
    {
        base.OnCreate();

        _windowManager = GetSystemService(WindowService)?.JavaCast<IWindowManager>();
        _clipboard     = GetSystemService(ClipboardService)?.JavaCast<ClipboardManager>();
        _vibrator      = GetSystemService(VibratorService)?.JavaCast<Vibrator>();

        try
        {
            _config      = Config.Load(this);
            _recorder    = new AudioRecorder();
            _btwRecorder = new AudioRecorder();
            _whisper     = new GroqWhisperClient(
                _config.GroqApiKey,
                _config.WhisperModel,
                _config.WhisperLang,
                _config.WhisperUrl);

            if (_config.GeminiAvailable)
                _gemini = new GeminiClient(
                    _config.GeminiApiKey!,
                    _config.GeminiModel,
                    _config.GeminiThinkingLevel);

            CreateNotificationChannel();
            StartForeground(NotifId, BuildNotification(),
                Android.Content.PM.ForegroundService.TypeMicrophone);

            CreateOverlay();
        }
        catch (Exception ex)
        {
            // Show error to user and stop the service gracefully instead of crashing silently.
            Toast.MakeText(this, $"Voice Overlay failed to start: {ex.Message}", ToastLength.Long)?.Show();
            StopSelf();
        }
    }

    public override StartCommandResult OnStartCommand(Intent? intent, StartCommandFlags flags, int startId)
        => StartCommandResult.Sticky;

    public override IBinder? OnBind(Intent? intent) => null;

    public override void OnDestroy()
    {
        StopAllTimers();
        _recorder?.Dispose();
        _btwRecorder?.Dispose();

        if (_overlayRoot != null && _windowManager != null)
        {
            try { _windowManager.RemoveView(_overlayRoot); }
            catch { /* view may already be gone */ }
        }

        base.OnDestroy();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private void CreateNotificationChannel()
    {
        if (Build.VERSION.SdkInt < BuildVersionCodes.O) return;

        var nm = GetSystemService(NotificationService)?.JavaCast<NotificationManager>()!;
        if (nm.GetNotificationChannel(ChannelId) != null) return;

        var ch = new NotificationChannel(
            ChannelId,
            "Voice Overlay",
            NotificationImportance.Low)
        {
            Description = "Persistent notification for the floating voice overlay"
        };
        nm.CreateNotificationChannel(ch);
    }

    private Notification BuildNotification()
    {
        var builder = new Notification.Builder(this, ChannelId)
            .SetSmallIcon(Android.Resource.Drawable.IcDialogInfo)
            .SetContentTitle("Voice Overlay")
            .SetContentText("Floating overlay is active")
            .SetOngoing(true);

        return builder.Build()!;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlay window
    // ─────────────────────────────────────────────────────────────────────────

    private void CreateOverlay()
    {
        var inflater = LayoutInflater.From(this)!;
        _overlayRoot = inflater.Inflate(Resource.Layout.overlay_layout, null)!;

        // Resolve buttons
        _btnX     = _overlayRoot.FindViewById(Resource.Id.btn_x)!;
        _btnW     = _overlayRoot.FindViewById(Resource.Id.btn_w)!;
        _btnBtw   = _overlayRoot.FindViewById(Resource.Id.btn_btw)!;
        _btnMic   = _overlayRoot.FindViewById(Resource.Id.btn_mic)!;
        _btnG     = _overlayRoot.FindViewById(Resource.Id.btn_g)!;
        _btnEnter = _overlayRoot.FindViewById(Resource.Id.btn_enter)!;

        // Apply initial colours
        SetButtonColor(_btnX,     ColBtnX);           // red  — always
        SetButtonColor(_btnW,     ColToggleOn);        // green — Gemini off → Whisper active
        SetButtonColor(_btnBtw,   ColBtnBtwIdle);      // light blue
        SetButtonColor(_btnMic,   ColBtnMicIdle);      // dark blue
        SetButtonColor(_btnG,     ColToggleOff);       // dark  — Gemini starts disabled
        SetButtonColor(_btnEnter, ColBtnProcessing);   // orange — autoEnter starts true

        // Wire button events
        _btnX.Click     += (_, _) => OnXClick();
        _btnW.Click     += (_, _) => OnWClick();
        _btnBtw.Click   += (_, _) => OnBtwClick();
        _btnMic.Click   += (_, _) => OnMicClick();
        _btnG.Click     += (_, _) => OnGClick();
        _btnEnter.Click += (_, _) => OnEnterClick();

        // Enable vertical dragging on the root panel
        _overlayRoot.Touch += OnRootTouch;

        // Build WindowManager layout params
        var wmType = Build.VERSION.SdkInt >= BuildVersionCodes.O
            ? WindowManagerTypes.ApplicationOverlay
            : WindowManagerTypes.Phone;

        var lp = new WindowManagerLayoutParams(
            ViewGroup.LayoutParams.WrapContent,
            ViewGroup.LayoutParams.WrapContent,
            wmType,
            WindowManagerFlags.NotFocusable,
            Format.Translucent)
        {
            Gravity = GravityFlags.Right | GravityFlags.CenterVertical,
            X = 0,
            Y = 0
        };

        _windowManager!.AddView(_overlayRoot, lp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drag handling
    // ─────────────────────────────────────────────────────────────────────────

    private void OnRootTouch(object? sender, View.TouchEventArgs e)
    {
        var ev = e.Event!;
        var lp = (WindowManagerLayoutParams)_overlayRoot!.LayoutParameters!;

        switch (ev.Action)
        {
            case MotionEventActions.Down:
                _dragInitialY   = lp.Y;
                _dragTouchStartY = (int)ev.RawY;
                _isDragging     = false;
                break;

            case MotionEventActions.Move:
                int deltaY = (int)ev.RawY - _dragTouchStartY;
                if (Math.Abs(deltaY) > 8) _isDragging = true;
                if (_isDragging)
                {
                    lp.Y = _dragInitialY + deltaY;
                    _windowManager!.UpdateViewLayout(_overlayRoot, lp);
                }
                break;

            case MotionEventActions.Up:
            case MotionEventActions.Cancel:
                // Consume tap events that were actually drags
                if (_isDragging) e.Handled = true;
                _isDragging = false;
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button handlers
    // ─────────────────────────────────────────────────────────────────────────

    // X — delete pasted text from terminal, clear clipboard, flash gray then back to red
    private void OnXClick()
    {
        // If text was pasted into the terminal, send Ctrl+U to clear the line
        if (_hasPastedText)
            DeletePastedText();
        else
            ClearClipboard();

        _hasPastedText = false;

        SetButtonColor(_btnX!, ColBtnIdle);
        CancelRunnable(ref _xFlashRunnable);

        _xFlashRunnable = new Java.Lang.Runnable(() =>
        {
            SetButtonColor(_btnX!, ColBtnX);
            _xFlashRunnable = null;
        });
        _handler.PostDelayed(_xFlashRunnable, 2000);
    }

    // W — whisper undo: paste raw transcript back to clipboard
    private void OnWClick()
    {
        if (_lastRawTranscript == null) return;

        CopyToClipboard(_lastRawTranscript);
        _lastRawTranscript = null;
    }

    // BTW — record and prepend "/btw "
    private void OnBtwClick()
    {
        if (_micState == RecordingState.Recording ||
            _micState == RecordingState.Processing) return;

        if (_isBtwRecording)
        {
            StopBtwRecording();
        }
        else
        {
            StartBtwRecording();
        }
    }

    // Mic — main voice recording
    private void OnMicClick()
    {
        if (_isBtwRecording) return;

        if (_micState == RecordingState.Recording)
        {
            StopMainRecording();
        }
        else if (_micState == RecordingState.Idle)
        {
            StartMainRecording();
        }
        // Ignore clicks while Processing / Success / Error
    }

    // G — toggle Gemini correction
    private void OnGClick()
    {
        _geminiEnabled = !_geminiEnabled;

        if (_geminiEnabled)
        {
            SetButtonColor(_btnG!, ColToggleOn);
            SetButtonColor(_btnW!, ColToggleOff);   // W goes dark when Gemini active
        }
        else
        {
            SetButtonColor(_btnG!, ColToggleOff);
            SetButtonColor(_btnW!, ColToggleOn);    // W goes green when Gemini off (Whisper undo active)
        }
    }

    // Enter — toggle auto-enter (appends newline after paste)
    private void OnEnterClick()
    {
        _autoEnterEnabled = !_autoEnterEnabled;
        SetButtonColor(_btnEnter!, _autoEnterEnabled ? ColBtnProcessing : ColToggleOff);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recording — main mic
    // ─────────────────────────────────────────────────────────────────────────

    private void StartMainRecording()
    {
        VibrateShort(100);
        _recorder!.Start();
        SetMicState(RecordingState.Recording);
        StartMicPulse();
    }

    private void StopMainRecording()
    {
        StopMicPulse();
        VibrateDouble();
        SetMicState(RecordingState.Processing);
        SetButtonColor(_btnMic!, ColBtnProcessing);

        var wavFile = _recorder!.Stop();

        _ = Task.Run(async () =>
        {
            try
            {
                string raw = wavFile != null
                    ? await _whisper!.TranscribeAsync(wavFile)
                    : string.Empty;

                _lastRawTranscript = raw;

                string final = (_geminiEnabled && _gemini != null)
                    ? await _gemini.CorrectTextAsync(raw)
                    : raw;

                if (_hasPastedText)
                    final = " " + final;

                if (_autoEnterEnabled)
                    final += "\n";

                _handler.Post(() =>
                {
                    CopyAndPaste(final);
                    _hasPastedText = true;
                    SetMicState(RecordingState.Success);
                    SetButtonColor(_btnMic!, ColBtnSuccess);
                    ScheduleMicReset(3000);
                });
            }
            catch
            {
                _handler.Post(() =>
                {
                    SetMicState(RecordingState.Error);
                    SetButtonColor(_btnMic!, ColBtnX);
                    ScheduleMicReset(3000);
                });
            }
            finally
            {
                // Clean up temp WAV file after transcription is complete.
                if (wavFile != null)
                {
                    try { File.Delete(wavFile); }
                    catch { /* ignore — file may already be gone */ }
                }
            }
        });
    }

    private void SetMicState(RecordingState state) => _micState = state;

    private void ScheduleMicReset(int delayMs)
    {
        CancelRunnable(ref _micResetRunnable);
        _micResetRunnable = new Java.Lang.Runnable(() =>
        {
            SetMicState(RecordingState.Idle);
            SetButtonColor(_btnMic!, ColBtnMicIdle);
            _micResetRunnable = null;
        });
        _handler.PostDelayed(_micResetRunnable, delayMs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recording — BTW mic
    // ─────────────────────────────────────────────────────────────────────────

    private void StartBtwRecording()
    {
        VibrateShort(100);
        _btwRecorder!.Start();
        _isBtwRecording = true;
        SetButtonColor(_btnBtw!, ColBtnBtwRecording);
        StartBtwPulse();
    }

    private void StopBtwRecording()
    {
        StopBtwPulse();
        VibrateDouble();
        _isBtwRecording = false;
        SetButtonColor(_btnBtw!, ColBtnProcessing);

        var wavFile = _btwRecorder!.Stop();

        _ = Task.Run(async () =>
        {
            try
            {
                string raw = wavFile != null
                    ? await _whisper!.TranscribeAsync(wavFile)
                    : string.Empty;

                string final = (_geminiEnabled && _gemini != null)
                    ? await _gemini.CorrectTextAsync(raw)
                    : raw;

                final = "/btw " + final;

                if (_autoEnterEnabled)
                    final += "\n";

                _handler.Post(() =>
                {
                    CopyAndPaste(final);
                    // Track whether text was pasted without auto-enter (for X-button delete).
                    _hasPastedText = !_autoEnterEnabled;
                    SetButtonColor(_btnBtw!, ColBtnSuccess);
                    ScheduleBtwReset(3000);
                });
            }
            catch
            {
                _handler.Post(() =>
                {
                    SetButtonColor(_btnBtw!, ColBtnX);
                    ScheduleBtwReset(3000);
                });
            }
            finally
            {
                // Clean up temp WAV file after transcription is complete.
                if (wavFile != null)
                {
                    try { File.Delete(wavFile); }
                    catch { /* ignore — file may already be gone */ }
                }
            }
        });
    }

    private void ScheduleBtwReset(int delayMs)
    {
        CancelRunnable(ref _btwResetRunnable);
        _btwResetRunnable = new Java.Lang.Runnable(() =>
        {
            SetButtonColor(_btnBtw!, ColBtnBtwIdle);
            _btwResetRunnable = null;
        });
        _handler.PostDelayed(_btwResetRunnable, delayMs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pulse animations (500 ms, alternating colours)
    // ─────────────────────────────────────────────────────────────────────────

    private void StartMicPulse()
    {
        _micPulsePhase = false;
        ScheduleMicPulseTick();
    }

    private void StopMicPulse() => CancelRunnable(ref _micPulseRunnable);

    private void ScheduleMicPulseTick()
    {
        _micPulseRunnable = new Java.Lang.Runnable(() =>
        {
            if (_micState != RecordingState.Recording) return;

            _micPulsePhase = !_micPulsePhase;
            SetButtonColor(_btnMic!, _micPulsePhase ? ColBtnRecordingBrt : ColBtnRecording);
            ScheduleMicPulseTick();
        });
        _handler.PostDelayed(_micPulseRunnable, 500);
    }

    private void StartBtwPulse()
    {
        _btwPulsePhase = false;
        ScheduleBtwPulseTick();
    }

    private void StopBtwPulse() => CancelRunnable(ref _btwPulseRunnable);

    private void ScheduleBtwPulseTick()
    {
        _btwPulseRunnable = new Java.Lang.Runnable(() =>
        {
            if (!_isBtwRecording) return;

            _btwPulsePhase = !_btwPulsePhase;
            SetButtonColor(_btnBtw!, _btwPulsePhase ? ColBtnBtwPulse : ColBtnBtwRecording);
            ScheduleBtwPulseTick();
        });
        _handler.PostDelayed(_btwPulseRunnable, 500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — button colour via GradientDrawable (runtime-changeable)
    // ─────────────────────────────────────────────────────────────────────────

    private static void SetButtonColor(View btn, Color color)
    {
        var drawable = btn.Background as GradientDrawable
                    ?? new GradientDrawable();

        drawable.SetColor(color);
        btn.Background = drawable;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — clipboard + auto-paste
    // ─────────────────────────────────────────────────────────────────────────

    private void CopyToClipboard(string text)
    {
        var clip = ClipData.NewPlainText("voice_overlay", text);
        _clipboard!.PrimaryClip = clip;
        VibrateShort(50);
    }

    /// <summary>
    /// Copies text to clipboard, then triggers paste into the focused app via AccessibilityService.
    /// </summary>
    private void CopyAndPaste(string text)
    {
        CopyToClipboard(text);
        TriggerPaste();
    }

    /// <summary>
    /// Triggers paste in the focused app via the AccessibilityService.
    /// Small delay (150ms) to ensure clipboard content is ready.
    /// </summary>
    private void TriggerPaste()
    {
        if (!PasteAccessibilityService.IsConnected) return;

        _handler.PostDelayed(new Java.Lang.Runnable(() =>
        {
            PasteAccessibilityService.Instance?.PerformPaste();
        }), 150);
    }

    /// <summary>
    /// Deletes previously pasted text from the terminal by sending Ctrl+U (line kill).
    /// Only effective when auto-enter was OFF (text still on the input line).
    /// </summary>
    private void DeletePastedText()
    {
        if (!PasteAccessibilityService.IsConnected) return;

        // Ctrl+U (\x15) clears the entire input line in bash/zsh
        CopyAndPaste("\x15");

        // Clear clipboard after the delete so Ctrl+U doesn't linger
        _handler.PostDelayed(new Java.Lang.Runnable(() =>
        {
            ClearClipboard();
        }), 400);
    }

    private void ClearClipboard()
    {
        if (Build.VERSION.SdkInt >= BuildVersionCodes.P)
        {
            _clipboard?.ClearPrimaryClip();
        }
        else
        {
            // Pre-P: overwrite with empty string
            var clip = ClipData.NewPlainText("voice_overlay", string.Empty);
            _clipboard!.PrimaryClip = clip;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — haptic feedback
    // ─────────────────────────────────────────────────────────────────────────

    private void VibrateShort(int ms)
    {
        if (_vibrator == null || !_vibrator.HasVibrator) return;

        if (Build.VERSION.SdkInt >= BuildVersionCodes.O)
            _vibrator.Vibrate(VibrationEffect.CreateOneShot(ms, VibrationEffect.DefaultAmplitude));
        else
#pragma warning disable CA1422 // Obsolete API, but required for API < 26 fallback
            _vibrator.Vibrate(ms);
#pragma warning restore CA1422
    }

    private void VibrateDouble()
    {
        if (_vibrator == null || !_vibrator.HasVibrator) return;

        // Pattern: [delay, vibrate, pause, vibrate]
        long[] pattern = [0, 80, 100, 80];

        if (Build.VERSION.SdkInt >= BuildVersionCodes.O)
            _vibrator.Vibrate(VibrationEffect.CreateWaveform(pattern, -1));
        else
#pragma warning disable CA1422
            _vibrator.Vibrate(pattern, -1);
#pragma warning restore CA1422
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — timer management
    // ─────────────────────────────────────────────────────────────────────────

    private void CancelRunnable(ref Java.Lang.Runnable? runnable)
    {
        if (runnable != null)
        {
            _handler.RemoveCallbacks(runnable);
            runnable = null;
        }
    }

    private void StopAllTimers()
    {
        CancelRunnable(ref _micPulseRunnable);
        CancelRunnable(ref _btwPulseRunnable);
        CancelRunnable(ref _micResetRunnable);
        CancelRunnable(ref _btwResetRunnable);
        CancelRunnable(ref _xFlashRunnable);
    }
}
