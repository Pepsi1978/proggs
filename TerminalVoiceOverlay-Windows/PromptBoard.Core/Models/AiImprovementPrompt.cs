namespace PromptBoard.Core.Models;

/// <summary>
/// A Gemini meta-prompt that lives in an AiLibrary category. When
/// IsActiveForImprovement is true, Gemini uses its OriginalText as the
/// system instruction to rewrite other prompts. Only one AiImprovementPrompt
/// may be active at a time — enforced in AiImprovementPromptRepository.
/// </summary>
public class AiImprovementPrompt : Prompt
{
    /// <summary>
    /// Gemini model id. The Voice Terminal Overlay app standardizes on
    /// "gemini-3.1-flash-lite" across every Gemini surface
    /// (dictation cleanup, prompt improvement, history-title generation).
    /// User-selectable per meta-prompt for the rare case someone wants
    /// a different model for a specific improvement chain.
    /// </summary>
    public string GeminiModel { get; set; } = "gemini-3.1-flash-lite";

    /// <summary>True for exactly one AiImprovementPrompt across the whole app.</summary>
    public bool IsActiveForImprovement { get; set; }
}
