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
}
