using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Download;
using Google.Apis.Drive.v3;
using Google.Apis.Services;
using Google.Apis.Upload;
using Google.Apis.Util.Store;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Port of the PromptBoard Google Drive backup service. Stores a single
/// <c>promptboard-backup.json</c> in the hidden <c>appDataFolder</c> so
/// the file is invisible in normal Drive and scoped to this app's
/// OAuth client. Client ID/Secret/Refresh token are persisted in
/// $HOME/SK/PromptBoard/.env via <see cref="PromptBoardSecretStore"/>.
/// </summary>
public sealed class GoogleDriveBackupService : IGoogleDriveBackupService
{
    // Gleicher Dateiname wie das macOS-Claude-Overlay (ClaudeCodexVoiceOverlay-macOS),
    // damit Windows-CVO und Mac-Claude DASSELBE Drive-Backup teilen (Sync der Prompts).
    // Das Terminal-Overlay nutzt "promptboard-backup.json" (ohne Suffix) → bewusst getrennt.
    private const string BackupFileName = "promptboard-backup-claudecodex.json";
    private const string AppDataFolderSpace = "appDataFolder";

    private readonly PromptBoardSecretStore _secrets;
    private readonly ILogger<GoogleDriveBackupService> _logger;
    private readonly HttpClient _http = new();

    public GoogleDriveBackupService(
        PromptBoardSecretStore secrets,
        ILogger<GoogleDriveBackupService> logger)
    {
        _secrets = secrets;
        _logger = logger;
    }

    public Task<bool> IsAuthenticatedAsync(CancellationToken ct = default)
    {
        var s = _secrets.Load();
        var ok = !string.IsNullOrWhiteSpace(s.GoogleClientId)
              && !string.IsNullOrWhiteSpace(s.GoogleClientSecret)
              && !string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken);
        return Task.FromResult(ok);
    }

    public async Task ConnectAsync(CancellationToken ct = default)
    {
        var s = _secrets.Load();
        var (clientId, clientSecret) = RequireCreds(s);
        var flow = BuildFlow(clientId, clientSecret);
        var receiver = new LocalServerCodeReceiver();

        var credential = await new AuthorizationCodeInstalledApp(flow, receiver)
            .AuthorizeAsync("promptboard-user", ct);

        if (string.IsNullOrEmpty(credential.Token.RefreshToken))
        {
            throw new InvalidOperationException(
                "Google hat keinen Refresh-Token zurueckgegeben. Widerrufe die App-Berechtigung bei myaccount.google.com und versuche es erneut.");
        }

        var email = await FetchEmailAsync(credential.Token.AccessToken, ct);
        _secrets.Save(s with
        {
            GoogleOAuthRefreshToken = credential.Token.RefreshToken,
            GoogleAccountEmail = email,
        });

        _logger.LogInformation("Google Drive connected for {Email}", email);
    }

    public Task SignOutAsync(CancellationToken ct = default)
    {
        var s = _secrets.Load();
        _secrets.Save(s with
        {
            GoogleOAuthRefreshToken = null,
            GoogleAccountEmail = null,
        });
        return Task.CompletedTask;
    }

    private const int MaxSyncAttempts = 3;

    // One merge/upload at a time per process (the service itself is transient).
    private static readonly SemaphoreSlim SyncGate = new(1, 1);

    /// <summary>
    /// Fired (background thread) after a sync merged cloud rows into the local store —
    /// an open PromptBoard panel should reload its lists.
    /// </summary>
    public static event Action? LocalStoreMerged;

    /// <summary>
    /// Uploads the PromptBoard backup — merge-safe. The caller's <paramref name="json"/> is
    /// only a trigger: the service takes its own fresh snapshot (same shape) right before
    /// merging, reads the cloud first and, if another device changed it since our last
    /// sync, merges those changes into the local store BEFORE uploading. Before, the local
    /// DB was uploaded unchecked and silently overwrote other devices' changes.
    /// </summary>
    public Task UploadAsync(string json, CancellationToken ct = default) => SyncAsync(ct);

    /// <inheritdoc />
    public async Task<bool> SyncAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
            bool localChanged = false;
            for (int attempt = 1; attempt <= MaxSyncAttempts; attempt++)
            {
                ct.ThrowIfCancellationRequested();
                var listing = await DriveHttp.ListAppDataFilesAsync(drive, BackupFileName, ct).ConfigureAwait(false);
                string? cloudJson = await DownloadCombinedAsync(drive, listing, ct).ConfigureAwait(false);
                var (localJson, snapshotUtc) = await PromptBoardBackupSync.BuildLocalJsonAsync(ct).ConfigureAwait(false);
                string? baseJson = PromptBoardBackupSync.ReadBase(BackupFileName);

                var outcome = PromptBoardBackupSync.Merge(localJson, cloudJson, baseJson);
                bool applyFailed = false;
                if (outcome.LocalChanges is { } changes)
                {
                    try
                    {
                        await PromptBoardBackupSync.ApplyLocalAsync(changes, snapshotUtc, ct).ConfigureAwait(false);
                        localChanged = true;
                        _logger.LogInformation("Drive sync merged {C} categories / {P} prompts from the cloud.",
                            changes.Categories.Count, changes.Prompts.Count);
                    }
                    catch (Exception ex) when (ex is not OperationCanceledException)
                    {
                        // One odd cloud row must not stop local edits from leaving this PC:
                        // still upload the merged state (the cloud rows stay in it), but keep
                        // the old base so the next run retries the local apply honestly.
                        applyFailed = true;
                        _logger.LogWarning(ex, "Drive sync could not apply cloud rows locally; uploading anyway.");
                        DiagLog.Write("PromptBoardSync", "local apply failed, upload continues", ("err", ex.Message));
                    }
                }

                if (outcome.NeedsUpload || listing.Count > 1)
                {
                    // Optimistic concurrency: only overwrite the cloud state we merged with.
                    var current = await DriveHttp.ListAppDataFilesAsync(drive, BackupFileName, ct).ConfigureAwait(false);
                    if (!string.Equals(DriveHttp.VersionOf(current), DriveHttp.VersionOf(listing), StringComparison.Ordinal))
                    {
                        _logger.LogInformation("Drive backup changed during merge, merging again (attempt {A}).", attempt);
                        continue;
                    }
                    await UploadJsonAsync(drive, current, outcome.MergedJson, ct).ConfigureAwait(false);
                }

                if (!applyFailed) PromptBoardBackupSync.WriteBase(BackupFileName, outcome.MergedJson);
                if (localChanged) LocalStoreMerged?.Invoke();
                return localChanged;
            }
            _logger.LogWarning("Drive sync gave up after {N} concurrent cloud changes; next run retries.", MaxSyncAttempts);
            if (localChanged) LocalStoreMerged?.Invoke();
            return localChanged;
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>
    /// Cloud state = newest copy, combined with every duplicate (row-wise newer wins, nothing
    /// dropped), so the duplicate cleanup after the upload never deletes rows unseen.
    /// </summary>
    private static async Task<string?> DownloadCombinedAsync(
        DriveService drive, System.Collections.Generic.IReadOnlyList<DriveFile> listing, CancellationToken ct)
    {
        string? combined = null;
        for (int i = 0; i < listing.Count && i < 10; i++)
        {
            var json = await DriveHttp.DownloadTextAsync(drive, listing[i].Id, ct).ConfigureAwait(false);
            combined = combined is null ? json : PromptBoardBackupSync.Merge(combined, json, null).MergedJson;
        }
        return combined;
    }

    private static async Task UploadJsonAsync(
        DriveService drive, System.Collections.Generic.IReadOnlyList<DriveFile> listing, string json, CancellationToken ct)
    {
        using var content = new MemoryStream(Encoding.UTF8.GetBytes(json));
        if (listing.Count == 0)
        {
            var metadata = new DriveFile
            {
                Name = BackupFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(metadata, content, "application/json");
            create.Fields = "id";
            EnsureUploadCompleted(await create.UploadAsync(ct).ConfigureAwait(false));
            return;
        }

        var update = drive.Files.Update(new DriveFile(), listing[0].Id, content, "application/json");
        update.Fields = "id";
        EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));

        // Duplicates are merged into the uploaded JSON above — safe to remove now.
        for (int i = 1; i < listing.Count; i++)
        {
            try
            {
                await drive.Files.Delete(listing[i].Id).ExecuteAsync(ct).ConfigureAwait(false);
            }
            catch (Exception)
            {
                // Losing a stale duplicate is not fatal; it is merged again next time.
            }
        }
    }

    public async Task<string?> DownloadLatestAsync(CancellationToken ct = default)
    {
        using var drive = await BuildDriveAsync(ct);
        // Always grab the newest backup, skipping any stale duplicates.
        var ids = await FindAllFileIdsAsync(drive, ct);
        if (ids.Count == 0) return null;

        using var buffer = new MemoryStream();
        EnsureDownloadCompleted(await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct));
        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    public Task<string?> GetAccountEmailAsync(CancellationToken ct = default)
    {
        return Task.FromResult(_secrets.Load().GoogleAccountEmail);
    }

    private static void EnsureUploadCompleted(IUploadProgress progress)
    {
        progress.ThrowOnFailure();
        if (progress.Status != UploadStatus.Completed)
            throw new IOException($"Google Drive upload did not complete: {progress.Status}");
    }

    private static void EnsureDownloadCompleted(IDownloadProgress progress)
    {
        progress.ThrowOnFailure();
        if (progress.Status != DownloadStatus.Completed)
            throw new IOException($"Google Drive download did not complete: {progress.Status}");
    }

    // ── internals ─────────────────────────────────

    // Token refresh over the hardened client with a 20 s timeout - see DriveHttp.CreateFlow.
    private static GoogleAuthorizationCodeFlow BuildFlow(string id, string secret) =>
        DriveHttp.CreateFlow(id, secret, DriveService.Scope.DriveAppdata, "openid", "email");

    private async Task<DriveService> BuildDriveAsync(CancellationToken ct)
    {
        var s = _secrets.Load();
        var (id, secret) = RequireCreds(s);
        if (string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken))
            throw new GoogleDriveNotConfiguredException();

        var flow = BuildFlow(id, secret);
        var token = new TokenResponse { RefreshToken = s.GoogleOAuthRefreshToken };
        var cred = new UserCredential(flow, "promptboard-user", token);
        await cred.RefreshTokenAsync(ct);

        // Gehaerteter Client (Verbindungs-Timeout, Keepalive, 30 s Gesamt) fuer
        // alle Drive-Dienste — siehe DriveHttp (Vorfall 03.09.2026, 100-s-Haenger).
        return DriveHttp.CreateService(cred);
    }

    /// Lists every non-trashed promptboard-backup.json in the appDataFolder,
    /// newest first (modifiedTime desc). Used both for the upload+cleanup
    /// path and for finding the freshest file on download.
    private static async Task<System.Collections.Generic.List<string>> FindAllFileIdsAsync(
        DriveService drive, CancellationToken ct)
    {
        var list = drive.Files.List();
        list.Spaces = AppDataFolderSpace;
        list.Q = $"name = '{BackupFileName}' and trashed = false";
        list.Fields = "files(id, modifiedTime)";
        list.OrderBy = "modifiedTime desc";
        list.PageSize = 100;
        var result = await list.ExecuteAsync(ct);
        var ids = new System.Collections.Generic.List<string>();
        if (result.Files != null)
        {
            foreach (var f in result.Files)
            {
                if (!string.IsNullOrEmpty(f.Id)) ids.Add(f.Id);
            }
        }
        return ids;
    }

    private async Task<string?> FetchEmailAsync(string accessToken, CancellationToken ct)
    {
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, "https://www.googleapis.com/oauth2/v2/userinfo");
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
            var resp = await _http.SendAsync(req, ct);
            if (!resp.IsSuccessStatusCode) return null;
            var payload = await resp.Content.ReadAsStringAsync(ct);
            using var doc = JsonDocument.Parse(payload);
            return doc.RootElement.TryGetProperty("email", out var e) ? e.GetString() : null;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "UserInfo failed.");
            return null;
        }
    }

    private static (string id, string secret) RequireCreds(Secrets s)
    {
        if (string.IsNullOrWhiteSpace(s.GoogleClientId) || string.IsNullOrWhiteSpace(s.GoogleClientSecret))
            throw new GoogleDriveNotConfiguredException();
        return (s.GoogleClientId!.Trim(), s.GoogleClientSecret!.Trim());
    }
}
