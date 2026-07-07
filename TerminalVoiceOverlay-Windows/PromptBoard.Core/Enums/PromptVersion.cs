namespace PromptBoard.Core.Enums;

/// <summary>
/// Which version of a prompt is active when inserting into the CLI.
/// </summary>
public enum PromptVersion
{
    /// <summary>Use the originally authored or dictated text.</summary>
    Original = 0,

    /// <summary>Use the AI-improved text (must exist before activation).</summary>
    Improved = 1,
}
