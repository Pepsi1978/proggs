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
using Google.Apis.Drive.v3;
using Google.Apis.Services;
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
/// OAuth client. Refresh token is persisted in AppSettings.
/// </summary>
public sealed class GoogleDriveBackupService : IGoogleDriveBackupService
{
    private const string BackupFileName = "promptboard-backup.json";
    private const string AppDataFolderSpace = "appDataFolder";

    private readonly IAppSettingsRepository _settings;
    private readonly ILogger<GoogleDriveBackupService> _logger;
    private readonly HttpClient _http = new();

    public GoogleDriveBackupService(
        IAppSettingsRepository settings,
        ILogger<GoogleDriveBackupService> logger)
    {
        _settings = settings;
        _logger = logger;
    }

    public async Task<bool> IsAuthenticatedAsync(CancellationToken ct = default)
    {
        var s = await _settings.GetAsync(ct);
        return !string.IsNullOrWhiteSpace(s.GoogleClientId)
            && !string.IsNullOrWhiteSpace(s.GoogleClientSecret)
            && !string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken);
    }

    public async Task ConnectAsync(CancellationToken ct = default)
    {
        var s = await _settings.GetAsync(ct);
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

        s.GoogleOAuthRefreshToken = credential.Token.RefreshToken;
        s.GoogleAccountEmail = await FetchEmailAsync(credential.Token.AccessToken, ct);
        await _settings.UpdateAsync(s, ct);

        _logger.LogInformation("Google Drive connected for {Email}", s.GoogleAccountEmail);
    }

    public async Task SignOutAsync(CancellationToken ct = default)
    {
        var s = await _settings.GetAsync(ct);
        s.GoogleOAuthRefreshToken = null;
        s.GoogleAccountEmail = null;
        await _settings.UpdateAsync(s, ct);
    }

    public async Task UploadAsync(string json, CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct);
        var existingId = await FindFileIdAsync(drive, ct);
        using var content = new MemoryStream(Encoding.UTF8.GetBytes(json));

        if (existingId is null)
        {
            var metadata = new DriveFile
            {
                Name = BackupFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(metadata, content, "application/json");
            create.Fields = "id";
            await create.UploadAsync(ct);
        }
        else
        {
            var update = drive.Files.Update(new DriveFile(), existingId, content, "application/json");
            update.Fields = "id";
            await update.UploadAsync(ct);
        }
    }

    public async Task<string?> DownloadLatestAsync(CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct);
        var fileId = await FindFileIdAsync(drive, ct);
        if (fileId is null) return null;

        using var buffer = new MemoryStream();
        await drive.Files.Get(fileId).DownloadAsync(buffer, ct);
        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    public async Task<string?> GetAccountEmailAsync(CancellationToken ct = default)
    {
        var s = await _settings.GetAsync(ct);
        return s.GoogleAccountEmail;
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
        var s = await _settings.GetAsync(ct);
        var (id, secret) = RequireCreds(s);
        if (string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken))
            throw new GoogleDriveNotConfiguredException();

        var flow = BuildFlow(id, secret);
        var token = new TokenResponse { RefreshToken = s.GoogleOAuthRefreshToken };
        var cred = new UserCredential(flow, "promptboard-user", token);
        await cred.RefreshTokenAsync(ct);

        return new DriveService(new BaseClientService.Initializer
        {
            HttpClientInitializer = cred,
            ApplicationName = "PromptBoard",
        });
    }

    private static async Task<string?> FindFileIdAsync(DriveService drive, CancellationToken ct)
    {
        var list = drive.Files.List();
        list.Spaces = AppDataFolderSpace;
        list.Q = $"name = '{BackupFileName}' and trashed = false";
        list.Fields = "files(id)";
        list.PageSize = 1;
        var result = await list.ExecuteAsync(ct);
        return result.Files?.Count > 0 ? result.Files[0].Id : null;
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

    private static (string id, string secret) RequireCreds(AppSettings s)
    {
        if (string.IsNullOrWhiteSpace(s.GoogleClientId) || string.IsNullOrWhiteSpace(s.GoogleClientSecret))
            throw new GoogleDriveNotConfiguredException();
        return (s.GoogleClientId!.Trim(), s.GoogleClientSecret!.Trim());
    }
}
