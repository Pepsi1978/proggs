using System;
using System.Collections.Generic;
using PromptBoard.Core.Enums;

namespace PromptBoard.Core.Models;

/// <summary>
/// Serializable snapshot of the user's categories and prompts. This is
/// the JSON schema written to disk and uploaded to Google Drive.
/// API keys are deliberately NOT included — they live in PasswordVault
/// and in the local AppSettings row only.
/// </summary>
public sealed record BackupDocument(
    int SchemaVersion,
    DateTime CreatedAtUtc,
    string? AppVersion,
    IReadOnlyList<CategoryDto> Categories,
    IReadOnlyList<PromptDto> Prompts);

public sealed record CategoryDto(
    Guid Id,
    string Name,
    string BackgroundColorHex,
    int SortOrder,
    CategoryType Type,
    DateTime CreatedAtUtc,
    DateTime UpdatedAtUtc);

public sealed record PromptDto(
    Guid Id,
    Guid CategoryId,
    string ShortLabel,
    string OriginalText,
    string? ImprovedText,
    PromptVersion ActiveVersion,
    bool IsAlwaysOn,
    int SortOrder,
    Guid? ImprovedByAiPromptId,
    bool IsAiImprovementPrompt,
    string? GeminiModel,
    bool IsActiveForImprovement,
    DateTime CreatedAtUtc,
    DateTime UpdatedAtUtc);

/// <summary>How a restore merges the incoming document with the existing data.</summary>
public enum RestoreMode
{
    /// <summary>Wipe everything and repopulate from the backup.</summary>
    Replace = 0,

    /// <summary>
    /// Upsert by Id: entries present in both are overwritten with the backup's
    /// version; entries only in the backup are added; entries only in the DB are kept.
    /// </summary>
    Merge = 1,
}

public sealed record RestoreResult(
    int CategoriesAdded,
    int CategoriesUpdated,
    int CategoriesRemoved,
    int PromptsAdded,
    int PromptsUpdated,
    int PromptsRemoved);
