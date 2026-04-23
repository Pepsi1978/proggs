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
}

/// <summary>Result when the user creates a new category.</summary>
public sealed record NewCategoryResult(string Name, CategoryType Type);

/// <summary>Data handed to the editor dialog.</summary>
public sealed record PromptEditorRequest(
    string ShortLabel,
    string OriginalText,
    string? ImprovedText,
    PromptVersion ActiveVersion);

/// <summary>Updated fields returned from the editor.</summary>
public sealed record PromptEditorResult(
    string ShortLabel,
    string OriginalText,
    string? ImprovedText,
    PromptVersion ActiveVersion);
