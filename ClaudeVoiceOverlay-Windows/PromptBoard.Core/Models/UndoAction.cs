using System;
using System.Collections.Generic;

namespace PromptBoard.Core.Models;

/// <summary>
/// A freshly deleted item that can still be restored within the 10-second
/// Undo window. The MainViewModel publishes one of these, the MainPage shows
/// an InfoBar with an "Undo"-button, and the timer expires the item silently.
/// </summary>
public sealed class UndoAction
{
    public UndoActionKind Kind { get; init; }

    public string Message { get; init; } = string.Empty;

    /// <summary>Filled when <see cref="Kind"/> is <see cref="UndoActionKind.DeletedCategory"/>.</summary>
    public Category? Category { get; init; }

    /// <summary>
    /// The prompts that belonged to the deleted category, or the single deleted
    /// prompt. Keeps the full entities (including TPH subtype) so the Restore
    /// flow can call AddAsync with the exact state that was removed.
    /// </summary>
    public IReadOnlyList<Prompt> Prompts { get; init; } = Array.Empty<Prompt>();

    public DateTime CreatedAt { get; } = DateTime.UtcNow;
}

public enum UndoActionKind
{
    DeletedCategory,
    DeletedPrompt,
}
