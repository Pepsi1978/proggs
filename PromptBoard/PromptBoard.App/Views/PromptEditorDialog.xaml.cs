using System;
using System.Threading.Tasks;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Services;
using Serilog;

namespace PromptBoard.App.Views;

public sealed partial class PromptEditorDialog : ContentDialog
{
    private readonly IAudioRecorder _recorder;
    private readonly IGroqTranscriptionService _transcription;
    private readonly IPromptImprovementService _improvement;
    private bool _isDictating;
    private bool _isImproving;

    public PromptEditorDialog(
        PromptEditorRequest request,
        IAudioRecorder recorder,
        IGroqTranscriptionService transcription,
        IPromptImprovementService improvement)
    {
        InitializeComponent();
        _recorder = recorder;
        _transcription = transcription;
        _improvement = improvement;

        ShortLabelTextBox.Text = request.ShortLabel;
        OriginalTextBox.Text = request.OriginalText;
        ImprovedTextBox.Text = request.ImprovedText ?? string.Empty;

        bool hasImproved = !string.IsNullOrWhiteSpace(request.ImprovedText);
        ImprovedHint.Visibility = hasImproved ? Visibility.Collapsed : Visibility.Visible;
        UseImprovedRadio.IsEnabled = hasImproved;

        if (request.ActiveVersion == PromptVersion.Improved && hasImproved)
        {
            UseImprovedRadio.IsChecked = true;
        }
        else
        {
            UseOriginalRadio.IsChecked = true;
        }

        // Hide the Improve button when editing a meta-prompt itself.
        if (request.IsAiImprovementPrompt)
        {
            ImproveButton.Visibility = Visibility.Collapsed;
        }
        else
        {
            ImproveButton.Click += OnImproveClicked;
        }

        Opened += (_, _) => ShortLabelTextBox.Focus(FocusState.Programmatic);

        // Push-to-talk: press to start, release (or pointer exits) to stop.
        DictateButton.AddHandler(PointerPressedEvent, new PointerEventHandler(OnDictatePointerPressed), true);
        DictateButton.AddHandler(PointerReleasedEvent, new PointerEventHandler(OnDictatePointerReleased), true);
        DictateButton.AddHandler(PointerCanceledEvent, new PointerEventHandler(OnDictatePointerCanceled), true);
        DictateButton.AddHandler(PointerCaptureLostEvent, new PointerEventHandler(OnDictatePointerCanceled), true);
    }

    public PromptEditorResult Result
    {
        get
        {
            string? improved = string.IsNullOrWhiteSpace(ImprovedTextBox.Text) ? null : ImprovedTextBox.Text;
            PromptVersion version = UseImprovedRadio.IsChecked == true && improved is not null
                ? PromptVersion.Improved
                : PromptVersion.Original;
            return new PromptEditorResult(
                ShortLabel: ShortLabelTextBox.Text?.Trim() ?? string.Empty,
                OriginalText: OriginalTextBox.Text ?? string.Empty,
                ImprovedText: improved,
                ActiveVersion: version);
        }
    }

    // -------- Dictation (Phase 4) --------

    private async void OnDictatePointerPressed(object sender, PointerRoutedEventArgs e)
    {
        if (_isDictating || _isImproving)
        {
            return;
        }

        try
        {
            _isDictating = true;
            ClearDictateError();
            await _recorder.StartAsync();
            RecordingHint.Visibility = Visibility.Visible;
            IsPrimaryButtonEnabled = false;
            ImproveButton.IsEnabled = false;
            Log.Information("Dictation: recording started.");
        }
        catch (Exception ex)
        {
            ShowDictateError($"Mikrofon konnte nicht gestartet werden: {ex.Message}");
            _isDictating = false;
            IsPrimaryButtonEnabled = true;
            ImproveButton.IsEnabled = true;
            Log.Error(ex, "Failed to start dictation.");
        }
    }

    private async void OnDictatePointerReleased(object sender, PointerRoutedEventArgs e)
        => await StopAndTranscribeAsync();

    private async void OnDictatePointerCanceled(object sender, PointerRoutedEventArgs e)
        => await StopAndTranscribeAsync();

    private async Task StopAndTranscribeAsync()
    {
        if (!_recorder.IsRecording)
        {
            return;
        }

        try
        {
            byte[] wav = await _recorder.StopAsync();
            RecordingHint.Visibility = Visibility.Collapsed;

            if (wav.Length == 0)
            {
                _isDictating = false;
                IsPrimaryButtonEnabled = true;
                ImproveButton.IsEnabled = true;
                return;
            }

            TranscribingHint.Visibility = Visibility.Visible;
            string transcript = await _transcription.TranscribeAsync(wav);
            if (!string.IsNullOrWhiteSpace(transcript))
            {
                AppendTranscript(transcript.Trim());
            }
        }
        catch (GroqApiKeyMissingException)
        {
            ShowDictateError("Kein Groq-API-Key gesetzt. Bitte im Settings-Dialog eintragen.");
        }
        catch (GroqTranscriptionException ex)
        {
            ShowDictateError(ex.Message);
            Log.Warning(ex, "Groq transcription failed.");
        }
        catch (Exception ex)
        {
            ShowDictateError($"Diktat-Fehler: {ex.Message}");
            Log.Error(ex, "Unexpected dictation error.");
        }
        finally
        {
            TranscribingHint.Visibility = Visibility.Collapsed;
            _isDictating = false;
            IsPrimaryButtonEnabled = true;
            ImproveButton.IsEnabled = true;
        }
    }

    private void AppendTranscript(string transcript)
    {
        string existing = OriginalTextBox.Text ?? string.Empty;
        OriginalTextBox.Text = string.IsNullOrWhiteSpace(existing)
            ? transcript
            : existing + " " + transcript;
        OriginalTextBox.SelectionStart = OriginalTextBox.Text.Length;
    }

    private void ShowDictateError(string message)
    {
        DictateErrorHint.Text = message;
        DictateErrorHint.Visibility = Visibility.Visible;
    }

    private void ClearDictateError()
    {
        DictateErrorHint.Visibility = Visibility.Collapsed;
        DictateErrorHint.Text = string.Empty;
    }

    // -------- Improvement (Phase 5) --------

    private async void OnImproveClicked(object sender, RoutedEventArgs e)
    {
        if (_isDictating || _isImproving)
        {
            return;
        }

        string source = OriginalTextBox.Text?.Trim() ?? string.Empty;
        if (string.IsNullOrEmpty(source))
        {
            ShowImproveError("Bitte zuerst einen Originaltext eingeben oder diktieren.");
            return;
        }

        try
        {
            _isImproving = true;
            ClearImproveError();
            ImprovingHint.Visibility = Visibility.Visible;
            IsPrimaryButtonEnabled = false;
            DictateButton.IsEnabled = false;
            ImproveButton.IsEnabled = false;

            string improved = await _improvement.ImproveAsync(source);

            if (!string.IsNullOrWhiteSpace(improved))
            {
                ImprovedTextBox.Text = improved.Trim();
                ImprovedHint.Visibility = Visibility.Collapsed;
                UseImprovedRadio.IsEnabled = true;
                // Per spec: do NOT auto-switch ActiveVersion; user decides.
                Log.Information("Gemini improvement applied: {Chars} chars.", improved.Length);
            }
            else
            {
                ShowImproveError("Gemini lieferte keine Antwort.");
            }
        }
        catch (NoActiveMetaPromptException ex)
        {
            ShowImproveError(ex.Message);
        }
        catch (GeminiApiKeyMissingException)
        {
            ShowImproveError("Kein Gemini-API-Key gesetzt. Bitte im Settings-Dialog eintragen.");
        }
        catch (GeminiServiceException ex)
        {
            ShowImproveError(ex.Message);
            Log.Warning(ex, "Gemini improvement failed.");
        }
        catch (Exception ex)
        {
            ShowImproveError($"Verbesserungs-Fehler: {ex.Message}");
            Log.Error(ex, "Unexpected improvement error.");
        }
        finally
        {
            ImprovingHint.Visibility = Visibility.Collapsed;
            _isImproving = false;
            IsPrimaryButtonEnabled = true;
            DictateButton.IsEnabled = true;
            ImproveButton.IsEnabled = true;
        }
    }

    private void ShowImproveError(string message)
    {
        ImproveErrorHint.Text = message;
        ImproveErrorHint.Visibility = Visibility.Visible;
    }

    private void ClearImproveError()
    {
        ImproveErrorHint.Visibility = Visibility.Collapsed;
        ImproveErrorHint.Text = string.Empty;
    }
}
