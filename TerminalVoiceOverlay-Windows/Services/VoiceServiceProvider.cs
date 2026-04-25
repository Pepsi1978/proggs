namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Process-wide service locator for the three voice-stack pieces (audio
/// recorder, Groq Whisper transcription, optional Gemini text correction).
/// The OverlayWindow constructs them once at startup and registers them
/// here; secondary surfaces like the PromptEditDialog grab the same
/// instances instead of constructing parallel ones — important because
/// only one process at a time can hold the microphone.
/// </summary>
public static class VoiceServiceProvider
{
    public static AudioRecorder? Recorder { get; private set; }
    public static GroqWhisperClient? Groq { get; private set; }
    public static GeminiClient? Gemini { get; private set; }

    public static bool RecorderAvailable => Recorder is not null && Groq is not null;
    public static bool GeminiAvailable => Gemini is not null;

    public static void Initialize(AudioRecorder recorder, GroqWhisperClient groq, GeminiClient? gemini)
    {
        Recorder = recorder;
        Groq = groq;
        Gemini = gemini;
    }
}
