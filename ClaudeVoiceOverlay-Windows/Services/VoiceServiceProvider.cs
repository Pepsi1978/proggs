namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Process-wide service locator for the three voice-stack pieces (audio
/// recorder, Groq Whisper transcription, optional Gemini text correction)
/// PLUS die geteilte PromptHistoryService-Instanz. Frueher instanziierten
/// OverlayWindow und PromptBoardPanel jeweils ihren eigenen
/// PromptHistoryService — beide schrieben in dieselbe prompt-history.json
/// mit getrennten SemaphoreSlim-Locks, was bei parallelem Submit-Append +
/// Panel-ReloadHistory zu File-Race-Bedingungen fuehren konnte (zwei
/// File.Move-Aufrufe gleichzeitig auf derselben temp-Datei). Mit der
/// einzigen geteilten Instanz greifen alle Pfade durch denselben Lock.
/// </summary>
public static class VoiceServiceProvider
{
    public static AudioRecorder? Recorder { get; private set; }
    public static GroqWhisperClient? Groq { get; private set; }
    public static GeminiClient? Gemini { get; private set; }

    /// <summary>
    /// Single PromptHistoryService instance per process. Lazy-initialisiert
    /// damit ein Code-Pfad der den HistoryService braucht ohne dass das
    /// OverlayWindow je gestartet wurde (z.B. zukuenftige CLI-Tools die nur
    /// die Historie lesen) trotzdem einen funktionierenden Service bekommt.
    /// Lock fuer den ersten Aufruf — DoubleCheckedLocking-Pattern.
    /// </summary>
    private static PromptHistoryService? _history;
    private static readonly object _historyLock = new();
    public static PromptHistoryService History
    {
        get
        {
            if (_history is not null) return _history;
            lock (_historyLock)
            {
                _history ??= new PromptHistoryService();
                return _history;
            }
        }
    }

    /// <summary>
    /// Single PromptSlotService instance per process — gleiche Lazy-/Lock-
    /// Logik wie <see cref="History"/>. Haelt die 15 Prompt-Zwischenspeicher-
    /// Slots, die OverlayWindow (Cloud-Sync) und PromptBoardPanel (UI) teilen.
    /// </summary>
    private static PromptSlotService? _slots;
    private static readonly object _slotsLock = new();
    public static PromptSlotService Slots
    {
        get
        {
            if (_slots is not null) return _slots;
            lock (_slotsLock)
            {
                _slots ??= new PromptSlotService();
                return _slots;
            }
        }
    }

    public static bool RecorderAvailable => Recorder is not null && Groq is not null;
    public static bool GeminiAvailable => Gemini is not null;

    public static void Initialize(AudioRecorder recorder, GroqWhisperClient groq, GeminiClient? gemini)
    {
        Recorder = recorder;
        Groq = groq;
        Gemini = gemini;
    }
}
