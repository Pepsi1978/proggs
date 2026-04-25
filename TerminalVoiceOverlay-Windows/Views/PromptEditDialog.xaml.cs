using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Threading;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

public sealed record PromptEditResult(string ShortLabel, string OriginalText, bool IsAlwaysOn);

public partial class PromptEditDialog : Window
{
    public PromptEditResult? Result { get; private set; }

    /// <summary>
    /// Original text BEFORE Gemini improvement, captured the moment the
    /// user clicks "G". Lets us offer "Original speichern" alongside
    /// "Verbessert speichern" without losing what was originally typed
    /// or dictated.
    /// </summary>
    private string? _originalBeforeImprove;

    private bool _isImproving;

    private static readonly Brush MicIdle      = new SolidColorBrush(Color.FromRgb(0x3D, 0x3D, 0x3D));
    private static readonly Brush MicRecording = new SolidColorBrush(Color.FromRgb(0xE5, 0x39, 0x35));
    private static readonly Brush MicTranscribing = new SolidColorBrush(Color.FromRgb(0xB8, 0x86, 0x0B));

    private DispatcherTimer? _pulseTimer;
    private bool _pulseBright;

    public PromptEditDialog(
        string title,
        string initialLabel,
        string initialText,
        bool initialAlwaysOn)
    {
        InitializeComponent();
        TitleText.Text = title;
        ShortLabelBox.Text = initialLabel;
        OriginalTextBox.Text = initialText;
        AlwaysOnCheckbox.IsChecked = initialAlwaysOn;

        ShortLabelBox.Focus();
        ShortLabelBox.SelectAll();

        // Disable mic / G when their backing services aren't available
        // (e.g. no GROQ_API_KEY in .env, or no Gemini key).
        BtnMic.IsEnabled    = VoiceServiceProvider.RecorderAvailable;
        BtnGemini.IsEnabled = VoiceServiceProvider.GeminiAvailable;

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click      += (_, _) => SaveAs(OriginalTextBox.Text);
        BtnSaveOriginal.Click += (_, _) =>
        {
            // Persist what the user originally typed/dictated, even though
            // the textbox now shows the Gemini-improved version.
            SaveAs(_originalBeforeImprove ?? OriginalTextBox.Text);
        };
        BtnSaveImproved.Click += (_, _) => SaveAs(OriginalTextBox.Text);

        BtnMic.Click    += async (_, _) => await ToggleRecordingAsync();
        BtnGemini.Click += async (_, _) => await RunGeminiAsync();

        Closed += (_, _) =>
        {
            // Defensive: stop any in-progress recording so we never leak
            // the microphone if the dialog is closed mid-recording.
            try
            {
                if (VoiceServiceProvider.Recorder?.IsRecording == true)
                    VoiceServiceProvider.Recorder.Stop();
            }
            catch { /* ignored */ }
            _pulseTimer?.Stop();
        };
    }

    private void SaveAs(string text)
    {
        var label = ShortLabelBox.Text?.Trim() ?? string.Empty;
        if (string.IsNullOrEmpty(label)) return;
        Result = new PromptEditResult(label, text ?? string.Empty, AlwaysOnCheckbox.IsChecked == true);
        Close();
    }

    public static PromptEditResult? Ask(
        Window owner, string title, string label, string text, bool alwaysOn)
    {
        var dlg = new PromptEditDialog(title, label, text, alwaysOn) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Result;
    }

    // ──────────────── Mic-Recording ────────────────

    /// <summary>
    /// Toggles the dictation recorder. While recording the mic button
    /// pulses red; on stop the wav is sent to Groq Whisper and the
    /// transcribed text is appended to the prompt-text box. The button
    /// briefly shows an amber "transcribing" state during the API call.
    /// </summary>
    private async Task ToggleRecordingAsync()
    {
        var recorder = VoiceServiceProvider.Recorder;
        var groq     = VoiceServiceProvider.Groq;
        if (recorder is null || groq is null)
        {
            ShowStatus("Mic-Aufnahme nicht verfuegbar (kein GROQ_API_KEY in .env).");
            return;
        }

        if (recorder.IsRecording)
        {
            // ── Stop & transcribe ──
            StopPulse();
            BtnMic.Background = MicTranscribing;
            ShowStatus("Transkribiere...");
            string? wavPath = null;
            try
            {
                wavPath = recorder.Stop();
                if (string.IsNullOrEmpty(wavPath))
                {
                    ShowStatus("Aufnahme leer.");
                    BtnMic.Background = MicIdle;
                    return;
                }
                var text = await groq.TranscribeAsync(wavPath);
                AppendToPromptText(text);
                ShowStatus($"{text.Length} Zeichen eingefuegt.");
            }
            catch (Exception ex)
            {
                ShowStatus($"Transkription fehlgeschlagen: {ex.Message}");
            }
            finally
            {
                BtnMic.Background = MicIdle;
                if (!string.IsNullOrEmpty(wavPath))
                    TryDeleteFile(wavPath);
            }
            return;
        }

        // ── Start ──
        try
        {
            recorder.Start();
            StartPulse();
            ShowStatus("Aufnahme laeuft. Klick erneut auf Mic zum Stoppen.");
        }
        catch (Exception ex)
        {
            ShowStatus($"Aufnahme konnte nicht gestartet werden: {ex.Message}");
            BtnMic.Background = MicIdle;
        }
    }

    private void AppendToPromptText(string newText)
    {
        if (string.IsNullOrWhiteSpace(newText)) return;
        var current = OriginalTextBox.Text ?? string.Empty;
        if (current.Length > 0 && !current.EndsWith(' ') && !current.EndsWith('\n'))
            current += " ";
        OriginalTextBox.Text = current + newText.Trim();
        // Scroll caret into view at end so the user sees the new text land.
        OriginalTextBox.CaretIndex = OriginalTextBox.Text.Length;
        OriginalTextBox.ScrollToEnd();
    }

    private void StartPulse()
    {
        _pulseTimer ??= new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
        _pulseTimer.Tick -= OnPulseTick;
        _pulseTimer.Tick += OnPulseTick;
        _pulseBright = false;
        BtnMic.Background = MicRecording;
        _pulseTimer.Start();
    }

    private void StopPulse()
    {
        _pulseTimer?.Stop();
        _pulseBright = false;
    }

    private void OnPulseTick(object? sender, EventArgs e)
    {
        _pulseBright = !_pulseBright;
        BtnMic.Background = _pulseBright
            ? new SolidColorBrush(Color.FromRgb(0xFF, 0x66, 0x66))
            : MicRecording;
    }

    // ──────────────── Gemini-Verbesserung ────────────────

    /// <summary>
    /// Sends the current prompt text to Gemini for cleanup. On success the
    /// textbox shows the improved version, and the footer switches from a
    /// single "Speichern" button to two ("Original speichern" / "Verbessert
    /// speichern"), so the user can pick which version to persist. The
    /// original text is held in <see cref="_originalBeforeImprove"/>.
    /// </summary>
    private async Task RunGeminiAsync()
    {
        if (_isImproving) return;
        var gemini = VoiceServiceProvider.Gemini;
        if (gemini is null)
        {
            ShowStatus("Gemini nicht verfuegbar (kein GEMINI_API_KEY in .env).");
            return;
        }
        var current = OriginalTextBox.Text?.Trim();
        if (string.IsNullOrEmpty(current))
        {
            ShowStatus("Kein Text zum Verbessern.");
            return;
        }

        _isImproving = true;
        BtnGemini.IsEnabled = false;
        BtnMic.IsEnabled = false;
        ShowStatus("Verbessere mit Gemini...");
        try
        {
            var improved = await gemini.CorrectTextAsync(current);
            if (string.IsNullOrWhiteSpace(improved))
            {
                ShowStatus("Gemini lieferte leere Antwort.");
                return;
            }
            // Capture the user's original ONLY on the first improvement so
            // multiple back-to-back G clicks don't lose the very first text.
            _originalBeforeImprove ??= current;
            OriginalTextBox.Text = improved;
            ShowImprovedFooter();
            ShowStatus($"Verbessert ({improved.Length} Zeichen). Original im Hintergrund erhalten.");
        }
        catch (Exception ex)
        {
            ShowStatus($"Gemini-Verbesserung fehlgeschlagen: {ex.Message}");
        }
        finally
        {
            _isImproving = false;
            BtnGemini.IsEnabled = VoiceServiceProvider.GeminiAvailable;
            BtnMic.IsEnabled    = VoiceServiceProvider.RecorderAvailable;
        }
    }

    private void ShowImprovedFooter()
    {
        BtnOk.Visibility           = Visibility.Collapsed;
        DualSavePanel.Visibility   = Visibility.Visible;
    }

    // ──────────────── Misc helpers ────────────────

    private void ShowStatus(string text)
    {
        StatusText.Text = text;
    }

    private static void TryDeleteFile(string path)
    {
        try { if (File.Exists(path)) File.Delete(path); }
        catch { /* keep the temp file silently if delete fails */ }
    }
}
