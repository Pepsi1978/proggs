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
    private bool _isBusy; // recording or transcribing

    public PromptEditorDialog(
        PromptEditorRequest request,
        IAudioRecorder recorder,
        IGroqTranscriptionService transcription)
    {
        InitializeComponent();
        _recorder = recorder;
        _transcription = transcription;

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

    private async void OnDictatePointerPressed(object sender, PointerRoutedEventArgs e)
    {
        if (_isBusy)
        {
            return;
        }

        try
        {
            _isBusy = true;
            ClearStatus();
            await _recorder.StartAsync();
            RecordingHint.Visibility = Visibility.Visible;
            IsPrimaryButtonEnabled = false;
            Log.Information("Dictation: recording started.");
        }
        catch (Exception ex)
        {
            ShowError($"Mikrofon konnte nicht gestartet werden: {ex.Message}");
            _isBusy = false;
            IsPrimaryButtonEnabled = true;
            Log.Error(ex, "Failed to start dictation.");
        }
    }

    private async void OnDictatePointerReleased(object sender, PointerRoutedEventArgs e)
    {
        await StopAndTranscribeAsync();
    }

    private async void OnDictatePointerCanceled(object sender, PointerRoutedEventArgs e)
    {
        await StopAndTranscribeAsync();
    }

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
                _isBusy = false;
                IsPrimaryButtonEnabled = true;
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
            ShowError("Kein Groq-API-Key gesetzt. Bitte im Settings-Dialog eintragen.");
        }
        catch (GroqTranscriptionException ex)
        {
            ShowError(ex.Message);
            Log.Warning(ex, "Groq transcription failed.");
        }
        catch (Exception ex)
        {
            ShowError($"Diktat-Fehler: {ex.Message}");
            Log.Error(ex, "Unexpected dictation error.");
        }
        finally
        {
            TranscribingHint.Visibility = Visibility.Collapsed;
            _isBusy = false;
            IsPrimaryButtonEnabled = true;
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

    private void ShowError(string message)
    {
        DictateErrorHint.Text = message;
        DictateErrorHint.Visibility = Visibility.Visible;
    }

    private void ClearStatus()
    {
        DictateErrorHint.Visibility = Visibility.Collapsed;
        DictateErrorHint.Text = string.Empty;
    }
}
