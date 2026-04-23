namespace PromptBoard.Core.Models;

/// <summary>
/// Singleton row of global user settings. The repository ensures exactly
/// one row exists and returns it for reads.
/// </summary>
/// <remarks>
/// NOTE: API keys are stored in this entity per spec, but production code
/// should write them into Windows PasswordVault (DPAPI) via the settings
/// service (Phase 7). For the MVP they live here, encrypted-at-rest only
/// by the OS file ACL on the SQLite file.
/// </remarks>
public class AppSettings : BaseEntity
{
    public string? GroqApiKey { get; set; }

    public string? GeminiApiKey { get; set; }

    public string? GoogleOAuthRefreshToken { get; set; }

    public string GroqModel { get; set; } = "whisper-large-v3-turbo";

    public bool AlwaysOnTop { get; set; } = true;

    public double BarHeight { get; set; } = 140;

    /// <summary>
    /// Separator between chained prompts when IsAlwaysOn prompts are
    /// concatenated with the clicked prompt. Default: blank-line, semicolon,
    /// blank-line — chosen so target AIs treat each segment as a separate task.
    /// </summary>
    public string SeparatorTemplate { get; set; } = "\n\n;\n\n";
}
