using System;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Drive.v3;
using Google.Apis.Drive.v3.Data;
using Google.Apis.Services;
using Google.Apis.Util.Store;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace PromptBoard.Services;

/// <summary>
/// Real Google Drive backup service using OAuth 2.0 loopback flow.
/// Stores <c>promptboard-backup.json</c> in the hidden <c>appDataFolder</c>
/// so the file is not visible in the user's normal Drive and cannot be
/// accessed by other apps with different client credentials.
/// </summary>
public sealed class GoogleDriveBackupService : IGoogleDriveBackupService
{
    private const string BackupFileName = "promptboard-backup.json";
    private const string AppDataFolderSpace = "appDataFolder";

    private readonly IAppSettingsRepository _settingsRepository;
    private readonly ILogger<GoogleDriveBackupService> _logger;
    private readonly HttpClient _httpClient = new();

    public GoogleDriveBackupService(
        IAppSettingsRepository settingsRepository,
        ILogger<GoogleDriveBackupService> logger)
    {
        _settingsRepository = settingsRepository;
        _logger = logger;
    }

    public async Task<bool> IsAuthenticatedAsync(CancellationToken ct = default)
    {
        AppSettings s = await _settingsRepository.GetAsync(ct);
        return !string.IsNullOrWhiteSpace(s.GoogleClientId)
            && !string.IsNullOrWhiteSpace(s.GoogleClientSecret)
            && !string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken);
    }

    public async Task ConnectAsync(CancellationToken ct = default)
    {
        AppSettings s = await _settingsRepository.GetAsync(ct);
        (string clientId, string clientSecret) = RequireCredentials(s);

        GoogleAuthorizationCodeFlow flow = CreateFlow(clientId, clientSecret);
        var receiver = new Google.Apis.Auth.OAuth2.LocalServerCodeReceiver();

        UserCredential credential = await new AuthorizationCodeInstalledApp(flow, receiver)
            .AuthorizeAsync("promptboard-user", ct);

        if (string.IsNullOrEmpty(credential.Token.RefreshToken))
        {
            throw new InvalidOperationException(
                "Google hat keinen Refresh-Token zurueckgegeben. Melde dich mit einem anderen Konto an oder widerrufe die App-Berechtigung unter myaccount.google.com.");
        }

        s.GoogleOAuthRefreshToken = credential.Token.RefreshToken;
        s.GoogleAccountEmail = await FetchAccountEmailAsync(credential.Token.AccessToken, ct);
        await _settingsRepository.UpdateAsync(s, ct);

        _logger.LogInformation("Google Drive connected. Account={Email}", s.GoogleAccountEmail);
    }

    public async Task SignOutAsync(CancellationToken ct = default)
    {
        AppSettings s = await _settingsRepository.GetAsync(ct);
        s.GoogleOAuthRefreshToken = null;
        s.GoogleAccountEmail = null;
        await _settingsRepository.UpdateAsync(s, ct);
        _logger.LogInformation("Google Drive signed out.");
    }

    public async Task UploadAsync(string json, CancellationToken ct = default)
    {
        DriveService drive = await BuildDriveServiceAsync(ct);

        string? existingId = await FindExistingFileIdAsync(drive, ct);
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
            _logger.LogInformation("Uploaded fresh backup to Drive appDataFolder.");
        }
        else
        {
            var update = drive.Files.Update(new DriveFile(), existingId, content, "application/json");
            update.Fields = "id";
            await update.UploadAsync(ct);
            _logger.LogInformation("Replaced existing Drive backup (id={Id}).", existingId);
        }
    }

    public async Task<string?> DownloadLatestAsync(CancellationToken ct = default)
    {
        DriveService drive = await BuildDriveServiceAsync(ct);
        string? fileId = await FindExistingFileIdAsync(drive, ct);
        if (fileId is null)
        {
            _logger.LogInformation("No Drive backup found (appDataFolder empty).");
            return null;
        }

        using var buffer = new MemoryStream();
        await drive.Files.Get(fileId).DownloadAsync(buffer, ct);
        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    public async Task<string?> GetAccountEmailAsync(CancellationToken ct = default)
    {
        AppSettings s = await _settingsRepository.GetAsync(ct);
        return s.GoogleAccountEmail;
    }

    // --- Internals ---------------------------------------------------------

    private GoogleAuthorizationCodeFlow CreateFlow(string clientId, string clientSecret)
    {
        return new GoogleAuthorizationCodeFlow(new GoogleAuthorizationCodeFlow.Initializer
        {
            ClientSecrets = new ClientSecrets
            {
                ClientId = clientId,
                ClientSecret = clientSecret,
            },
            Scopes = new[]
            {
                DriveService.Scope.DriveAppdata,
                "openid",
                "email",
            },
            DataStore = new NullDataStore(),
        });
    }

    private async Task<DriveService> BuildDriveServiceAsync(CancellationToken ct)
    {
        AppSettings s = await _settingsRepository.GetAsync(ct);
        (string clientId, string clientSecret) = RequireCredentials(s);

        if (string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken))
        {
            throw new GoogleDriveNotConfiguredException();
        }

        GoogleAuthorizationCodeFlow flow = CreateFlow(clientId, clientSecret);
        var token = new TokenResponse { RefreshToken = s.GoogleOAuthRefreshToken };
        var credential = new UserCredential(flow, "promptboard-user", token);

        // Force a refresh so we have a fresh access token.
        await credential.RefreshTokenAsync(ct);

        return new DriveService(new BaseClientService.Initializer
        {
            HttpClientInitializer = credential,
            ApplicationName = "PromptBoard",
        });
    }

    private static async Task<string?> FindExistingFileIdAsync(DriveService drive, CancellationToken ct)
    {
        var list = drive.Files.List();
        list.Spaces = AppDataFolderSpace;
        list.Q = $"name = '{BackupFileName}' and trashed = false";
        list.Fields = "files(id, name, modifiedTime)";
        list.PageSize = 1;

        FileList result = await list.ExecuteAsync(ct);
        if (result.Files is null || result.Files.Count == 0)
        {
            return null;
        }
        return result.Files[0].Id;
    }

    private async Task<string?> FetchAccountEmailAsync(string accessToken, CancellationToken ct)
    {
        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, "https://www.googleapis.com/oauth2/v2/userinfo");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", accessToken);
            HttpResponseMessage response = await _httpClient.SendAsync(request, ct);
            if (!response.IsSuccessStatusCode)
            {
                return null;
            }
            string payload = await response.Content.ReadAsStringAsync(ct);
            using JsonDocument doc = JsonDocument.Parse(payload);
            return doc.RootElement.TryGetProperty("email", out JsonElement email) ? email.GetString() : null;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "UserInfo lookup failed; account email will stay empty.");
            return null;
        }
    }

    private static (string clientId, string clientSecret) RequireCredentials(AppSettings s)
    {
        if (string.IsNullOrWhiteSpace(s.GoogleClientId) || string.IsNullOrWhiteSpace(s.GoogleClientSecret))
        {
            throw new GoogleDriveNotConfiguredException();
        }
        return (s.GoogleClientId!.Trim(), s.GoogleClientSecret!.Trim());
    }
}
