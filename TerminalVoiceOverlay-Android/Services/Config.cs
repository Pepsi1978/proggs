using Android.Content;

namespace TerminalVoiceOverlay.Services;

public sealed class Config
{
    private const string PrefsName = "VoiceOverlayPrefs";

    // Groq
    public string GroqApiKey { get; }
    public string WhisperModel { get; }
    public string WhisperLang { get; }
    public string WhisperUrl { get; }

    // Gemini
    public string? GeminiApiKey { get; }
    public string GeminiModel { get; }
    public string GeminiThinkingLevel { get; }
    public bool GeminiAvailable => !string.IsNullOrEmpty(GeminiApiKey);

    private Config(string groqApiKey, string whisperModel, string whisperLang, string whisperUrl,
                   string? geminiApiKey, string geminiModel, string geminiThinkingLevel)
    {
        GroqApiKey = groqApiKey;
        WhisperModel = whisperModel;
        WhisperLang = whisperLang;
        WhisperUrl = whisperUrl;
        GeminiApiKey = geminiApiKey;
        GeminiModel = geminiModel;
        GeminiThinkingLevel = geminiThinkingLevel;
    }

    public static Config Load(Context context)
    {
        var prefs = context.GetSharedPreferences(PrefsName, FileCreationMode.Private);

        var groqKey = prefs?.GetString("groq_api_key", "") ?? "";
        if (string.IsNullOrEmpty(groqKey))
            throw new InvalidOperationException("Groq API Key is required. Please set it in the app settings.");

        return new Config(
            groqApiKey: groqKey,
            whisperModel: prefs?.GetString("whisper_model", "whisper-large-v3") ?? "whisper-large-v3",
            whisperLang: prefs?.GetString("whisper_lang", "de") ?? "de",
            whisperUrl: prefs?.GetString("whisper_url", "https://api.groq.com/openai/v1/audio/transcriptions")
                        ?? "https://api.groq.com/openai/v1/audio/transcriptions",
            geminiApiKey: prefs?.GetString("gemini_api_key", null),
            geminiModel: prefs?.GetString("gemini_model", "gemini-3.1-flash-lite-preview")
                         ?? "gemini-3.1-flash-lite-preview",
            geminiThinkingLevel: prefs?.GetString("gemini_thinking_level", "MEDIUM") ?? "MEDIUM"
        );
    }

    public static void Save(Context context, string groqApiKey, string? geminiApiKey,
                            string whisperModel, string whisperLang, string geminiModel)
    {
        var prefs = context.GetSharedPreferences(PrefsName, FileCreationMode.Private);
        var editor = prefs?.Edit();
        if (editor == null) return;

        editor.PutString("groq_api_key", groqApiKey);
        editor.PutString("gemini_api_key", geminiApiKey);
        editor.PutString("whisper_model", whisperModel);
        editor.PutString("whisper_lang", whisperLang);
        editor.PutString("gemini_model", geminiModel);
        editor.Apply();
    }

    public static bool HasGroqKey(Context context)
    {
        var prefs = context.GetSharedPreferences(PrefsName, FileCreationMode.Private);
        var key = prefs?.GetString("groq_api_key", "") ?? "";
        return !string.IsNullOrEmpty(key);
    }
}
