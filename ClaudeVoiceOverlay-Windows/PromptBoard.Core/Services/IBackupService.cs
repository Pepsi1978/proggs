using System;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Services;

/// <summary>
/// Produces and applies <see cref="BackupDocument"/>s against the local database.
/// </summary>
public interface IBackupService
{
    /// <summary>Gather all categories and prompts into a snapshot document.</summary>
    Task<BackupDocument> CreateAsync(CancellationToken ct = default);

    /// <summary>
    /// Apply <paramref name="document"/> to the database using the chosen
    /// <paramref name="mode"/>. Returns counts describing what changed so
    /// the UI can show a summary.
    /// </summary>
    Task<RestoreResult> ApplyAsync(BackupDocument document, RestoreMode mode, CancellationToken ct = default);

    /// <summary>
    /// Non-destructive merge used by the Drive sync: upserts the given rows, NEVER deletes
    /// a local row, and skips every local row whose UpdatedAt is newer than
    /// <paramref name="skipIfLocalChangedAfterUtc"/> (the moment the sync took its local
    /// snapshot), so an edit made while the sync ran is never overwritten. Prompts whose
    /// category exists neither locally nor in the document, and rows that would change
    /// their type, are skipped instead of aborting the whole merge.
    /// </summary>
    Task<RestoreResult> MergeChangedAsync(BackupDocument changes, DateTime skipIfLocalChangedAfterUtc, CancellationToken ct = default);
}
