using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Services;

namespace PromptBoard.Services;

/// <summary>
/// Placeholder implementation. The real OAuth flow + Drive uploads land
/// in Phase 7 together with the Settings dialog. Until then every call
/// throws <see cref="GoogleDriveNotConfiguredException"/> so the UI can
/// show a clear "coming soon" hint.
/// </summary>
public sealed class StubGoogleDriveBackupService : IGoogleDriveBackupService
{
    public Task<bool> IsAuthenticatedAsync(CancellationToken ct = default) => Task.FromResult(false);

    public Task ConnectAsync(CancellationToken ct = default)
        => throw new GoogleDriveNotConfiguredException();

    public Task SignOutAsync(CancellationToken ct = default) => Task.CompletedTask;

    public Task UploadAsync(string json, CancellationToken ct = default)
        => throw new GoogleDriveNotConfiguredException();

    public Task<string?> DownloadLatestAsync(CancellationToken ct = default)
        => throw new GoogleDriveNotConfiguredException();

    public Task<string?> GetAccountEmailAsync(CancellationToken ct = default) => Task.FromResult<string?>(null);
}
