using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Services;

/// <summary>
/// Export/import <see cref="BackupDocument"/>s as JSON files on disk.
/// </summary>
public interface IBackupFileService
{
    Task ExportAsync(string path, CancellationToken ct = default);

    /// <summary>Read a backup file from disk and return it (caller decides how to apply).</summary>
    Task<BackupDocument> ReadAsync(string path, CancellationToken ct = default);

    /// <summary>Build a fresh <see cref="BackupDocument"/> and serialize it to a JSON string.</summary>
    Task<string> SerializeAsync(CancellationToken ct = default);

    /// <summary>Parse a JSON string produced by <see cref="SerializeAsync"/>.</summary>
    BackupDocument Deserialize(string json);
}
