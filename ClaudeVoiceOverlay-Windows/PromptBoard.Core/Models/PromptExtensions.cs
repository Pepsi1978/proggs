using PromptBoard.Core.Enums;

namespace PromptBoard.Core.Models;

/// <summary>
/// Helpers around <see cref="Prompt"/> that do not need to live on the entity
/// (keeps the entity purely data-shaped for EF Core).
/// </summary>
public static class PromptExtensions
{
    /// <summary>
    /// Returns the text that should be inserted into the CLI, honoring
    /// <see cref="Prompt.ActiveVersion"/>. Falls back to OriginalText when
    /// Improved is selected but empty.
    /// </summary>
    public static string EffectiveText(this Prompt prompt)
        => prompt.ActiveVersion == PromptVersion.Improved && !string.IsNullOrEmpty(prompt.ImprovedText)
            ? prompt.ImprovedText!
            : prompt.OriginalText;
}
