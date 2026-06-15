using System;
using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Placeholder for Google Drive upload/download. Real implementation
/// lands in Phase 7 together with the Settings dialog that carries the
/// OAuth client credentials and the browser-based sign-in flow.
/// </summary>
public interface IGoogleDriveBackupService
{
    Task<bool> IsAuthenticatedAsync(CancellationToken ct = default);

    /// <summary>
    /// Runs the OAuth 2.0 loopback flow. Opens the system browser,
    /// the user signs in, we receive the code on a local HTTP listener
    /// and exchange it for a refresh token that we persist in AppSettings.
    /// </summary>
    Task ConnectAsync(CancellationToken ct = default);

    Task SignOutAsync(CancellationToken ct = default);

    Task UploadAsync(string json, CancellationToken ct = default);

    Task<string?> DownloadLatestAsync(CancellationToken ct = default);

    /// <summary>
    /// Returns the primary e-mail of the currently connected Google account,
    /// or null if no account is connected.
    /// </summary>
    Task<string?> GetAccountEmailAsync(CancellationToken ct = default);
}

/// <summary>Signals that the Drive flow is not yet available.</summary>
public sealed class GoogleDriveNotConfiguredException : Exception
{
    public GoogleDriveNotConfiguredException()
        : base("Google-Drive-Sync folgt in Phase 7 zusammen mit dem Settings-Dialog und dem OAuth-Flow.") { }
}
