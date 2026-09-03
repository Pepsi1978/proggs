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

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Port of the PromptBoard Google Drive backup service. Stores a single
/// <c>promptboard-backup.json</c> in the hidden <c>appDataFolder</c> so
/// the file is invisible in normal Drive and scoped to this app's
/// OAuth client. Client ID/Secret/Refresh token are persisted in
/// $HOME/SK/PromptBoard/.env via <see cref="PromptBoardSecretStore"/>.
/// </summary>
public sealed class GoogleDriveBackupService : IGoogleDriveBackupService
{
    private const string BackupFileName = "promptboard-backup.json";
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

    public async Task UploadAsync(string json, CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct);
        // Cleanup rule (ported from BestJournal): list ALL promptboard-backup.json
        // files in the appDataFolder, keep the newest, delete every duplicate.
        // Prevents the "95% stale data on restore" accumulation problem.
        var existingIds = await FindAllFileIdsAsync(drive, ct);
        using var content = new MemoryStream(Encoding.UTF8.GetBytes(json));

        if (existingIds.Count == 0)
        {
            var metadata = new DriveFile
            {
                Name = BackupFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(metadata, content, "application/json");
            create.Fields = "id";
            EnsureUploadCompleted(await create.UploadAsync(ct));
        }
        else
        {
            var keepId = existingIds[0];
            var update = drive.Files.Update(new DriveFile(), keepId, content, "application/json");
            update.Fields = "id";
            EnsureUploadCompleted(await update.UploadAsync(ct));

            // Fire-and-forget cleanup of duplicates. Errors are logged but
            // don't fail the upload — losing a stale duplicate is not fatal.
            for (int i = 1; i < existingIds.Count; i++)
            {
                var dupId = existingIds[i];
                try
                {
                    await drive.Files.Delete(dupId).ExecuteAsync(ct);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Duplicate-cleanup delete failed for {Id}", dupId);
                }
            }
        }
    }

    public async Task<string?> DownloadLatestAsync(CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct);
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

    private static GoogleAuthorizationCodeFlow BuildFlow(string id, string secret) =>
        new(new GoogleAuthorizationCodeFlow.Initializer
        {
            ClientSecrets = new ClientSecrets { ClientId = id, ClientSecret = secret },
            Scopes = new[] { DriveService.Scope.DriveAppdata, "openid", "email" },
            DataStore = new NullDataStore(),
        });

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
