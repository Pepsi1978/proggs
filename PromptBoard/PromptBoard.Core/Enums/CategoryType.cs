namespace PromptBoard.Core.Enums;

/// <summary>
/// Kind of category, controls rendering and behavior in the bar.
/// </summary>
public enum CategoryType
{
    /// <summary>Normal user-defined category. Prompts render inline.</summary>
    Standard = 0,

    /// <summary>Project category. Prompts render inside a popup flyout (Phase 3).</summary>
    Project = 1,

    /// <summary>AI-library category. Holds Gemini meta-prompts used to improve other prompts.</summary>
    AiLibrary = 2,
}
