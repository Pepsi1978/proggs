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
    /// Gemini model id, e.g. "gemini-2.5-flash", "gemini-2.5-pro".
    /// User-selectable per meta-prompt.
    /// </summary>
    public string GeminiModel { get; set; } = "gemini-2.5-flash";

    /// <summary>True for exactly one AiImprovementPrompt across the whole app.</summary>
    public bool IsActiveForImprovement { get; set; }
}
