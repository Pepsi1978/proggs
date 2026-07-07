using System.Threading.Tasks;
using PromptBoard.Core.Enums;

namespace PromptBoard.Core.Services;

/// <summary>
/// Abstraction over XAML dialogs so ViewModels stay UI-free and testable.
/// The concrete implementation lives in PromptBoard.App.
/// </summary>
public interface IDialogService
{
    /// <summary>Ask for a new category's name and type.</summary>
    Task<NewCategoryResult?> ShowAddCategoryAsync();

    /// <summary>Ask the user for a new name for an existing item.</summary>
    Task<string?> ShowRenameAsync(string title, string currentValue);

    /// <summary>Generic confirmation dialog ("Delete X? Yes/No").</summary>
    Task<bool> ShowConfirmAsync(string title, string message, string confirmText = "Loeschen", string cancelText = "Abbrechen");

    /// <summary>Open the prompt editor and return the updated fields (or null on Cancel).</summary>
    Task<PromptEditorResult?> ShowPromptEditorAsync(PromptEditorRequest request);

    /// <summary>
    /// Show the backup / restore dialog. Returns true when a restore was
    /// applied (so the caller can refresh its views).
    /// </summary>
    Task<bool> ShowBackupAsync();

    /// <summary>
    /// Show the settings dialog. Returns true when the user saved changes
    /// (so the host can re-apply things like bar height on the window).
    /// </summary>
    Task<bool> ShowSettingsAsync();

    /// <summary>Show the About-this-app dialog.</summary>
    Task ShowAboutAsync();
}

public sealed record NewCategoryResult(string Name, CategoryType Type);

/// <summary>
/// Data handed to the editor dialog. The <see cref="IsAiImprovementPrompt"/>
/// flag tells the dialog whether it is editing a Gemini meta-prompt
/// (in which case the "Mit KI verbessern" button is hidden).
/// </summary>
public sealed record PromptEditorRequest(
    string ShortLabel,
    string OriginalText,
    string? ImprovedText,
    PromptVersion ActiveVersion,
    bool IsAiImprovementPrompt = false);

public sealed record PromptEditorResult(
    string ShortLabel,
    string OriginalText,
    string? ImprovedText,
    PromptVersion ActiveVersion);
