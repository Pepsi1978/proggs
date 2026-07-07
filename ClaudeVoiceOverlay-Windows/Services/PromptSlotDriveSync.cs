using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
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
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Synchronisiert die 15 Prompt-Zwischenspeicher-Slots zwischen Mac, Windows
/// und Cloud (Google Drive appDataFolder). Eigenstaendiger Service neben
/// <see cref="PromptHistoryDriveSync"/> — gleicher Secret-Store und OAuth-
/// Refresh-Token, aber eine eigene Datei <c>prompt-slots.json</c>, damit die
/// Slots unabhaengig von Historie und Promptboard-Backup gespiegelt werden.
/// </summary>
public sealed class PromptSlotDriveSync
{
    // Gleicher Dateiname wie das macOS-Claude-Overlay → geteilte Slots mit Mac-Claude.
    private const string SlotsFileName = "prompt-slots-claudecodex.json";
    private const string AppDataFolderSpace = "appDataFolder";

    private readonly PromptBoardSecretStore _secrets;
    private readonly ILogger<PromptSlotDriveSync>? _logger;

    public PromptSlotDriveSync(
        PromptBoardSecretStore secrets,
        ILogger<PromptSlotDriveSync>? logger = null)
    {
        _secrets = secrets;
        _logger = logger;
    }

    /// <summary>
    /// Laedt die lokale prompt-slots.json zu Drive hoch und raeumt Duplikate
    /// weg. Wird SOFORT nach jedem Speichern/Loeschen eines Slots aufgerufen.
    /// </summary>
    public async Task UploadSlotsAsync(string localPath, CancellationToken ct = default)
    {
        if (!File.Exists(localPath)) return;
        var bytes = await File.ReadAllBytesAsync(localPath, ct).ConfigureAwait(false);

        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, SlotsFileName, ct).ConfigureAwait(false);

        using var content = new MemoryStream(bytes);
        if (ids.Count == 0)
        {
            var meta = new DriveFile
            {
                Name = SlotsFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(meta, content, "application/json");
            create.Fields = "id";
            await create.UploadAsync(ct).ConfigureAwait(false);
        }
        else
        {
            var keep = ids[0];
            var update = drive.Files.Update(new DriveFile(), keep, content, "application/json");
            update.Fields = "id";
            await update.UploadAsync(ct).ConfigureAwait(false);

            for (int i = 1; i < ids.Count; i++)
            {
                try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
                catch (Exception ex)
                {
                    _logger?.LogWarning(ex, "Slot dup-cleanup failed for {Id}", ids[i]);
                }
            }
        }
    }

    /// <summary>Holt die aktuellste prompt-slots.json aus Drive. Null wenn keine vorhanden ist.</summary>
    public async Task<string?> DownloadSlotsAsync(CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, SlotsFileName, ct).ConfigureAwait(false);
        if (ids.Count == 0) return null;

        using var buffer = new MemoryStream();
        await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false);
        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    // ── Drive-Hilfsfunktionen (eigenstaendig, wie PromptHistoryDriveSync) ──

    private GoogleAuthorizationCodeFlow BuildFlow(string id, string secret) =>
        new(new GoogleAuthorizationCodeFlow.Initializer
        {
            ClientSecrets = new ClientSecrets { ClientId = id, ClientSecret = secret },
            Scopes = new[] { DriveService.Scope.DriveAppdata },
            DataStore = new NullDataStore(),
        });

    private async Task<DriveService> BuildDriveAsync(CancellationToken ct)
    {
        var s = _secrets.Load();
        if (string.IsNullOrWhiteSpace(s.GoogleClientId) ||
            string.IsNullOrWhiteSpace(s.GoogleClientSecret) ||
            string.IsNullOrWhiteSpace(s.GoogleOAuthRefreshToken))
        {
            throw new InvalidOperationException(
                "Google Drive ist nicht verbunden. Verbindung im Settings-Dialog herstellen.");
        }

        var flow = BuildFlow(s.GoogleClientId!.Trim(), s.GoogleClientSecret!.Trim());
        var token = new TokenResponse { RefreshToken = s.GoogleOAuthRefreshToken!.Trim() };
        var cred = new UserCredential(flow, "promptboard-user", token);
        await cred.RefreshTokenAsync(ct).ConfigureAwait(false);

        return new DriveService(new BaseClientService.Initializer
        {
            HttpClientInitializer = cred,
            ApplicationName = "PromptBoard",
        });
    }

    private static async Task<List<string>> FindAllAsync(
        DriveService drive, string fileName, CancellationToken ct)
    {
        var list = drive.Files.List();
        list.Spaces = AppDataFolderSpace;
        list.Q = $"name = '{fileName}' and trashed = false";
        list.Fields = "files(id, modifiedTime)";
        list.OrderBy = "modifiedTime desc";
        list.PageSize = 100;
        var result = await list.ExecuteAsync(ct).ConfigureAwait(false);
        var ids = new List<string>();
        if (result.Files != null)
        {
            foreach (var f in result.Files)
            {
                if (!string.IsNullOrEmpty(f.Id)) ids.Add(f.Id);
            }
        }
        return ids;
    }

    /// <summary>
    /// Mergt eine Cloud-Slots-JSON in die lokale Liste: pro Slot-Nummer
    /// gewinnt der Eintrag mit dem juengsten <c>UpdatedAt</c>. So setzt sich
    /// sowohl ein neuer Text als auch ein neues Loeschen (Tombstone) gegen
    /// einen aelteren Stand durch. Kaputtes Cloud-JSON laesst die lokale
    /// Liste unveraendert.
    /// </summary>
    public static List<PromptSlotEntry> MergeEntries(
        IEnumerable<PromptSlotEntry> local,
        string cloudJson)
    {
        List<PromptSlotEntry>? cloud = null;
        try
        {
            cloud = JsonSerializer.Deserialize<List<PromptSlotEntry>>(
                cloudJson,
                new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                    PropertyNameCaseInsensitive = true,
                });
        }
        catch
        {
            cloud = null;
        }

        var byNumber = new Dictionary<int, PromptSlotEntry>();
        foreach (var e in local)
        {
            if (e is not null && e.Number is >= 1 and <= PromptSlotService.SlotCount)
                byNumber[e.Number] = e;
        }
        if (cloud is not null)
        {
            foreach (var e in cloud)
            {
                if (e is null || e.Number is < 1 or > PromptSlotService.SlotCount) continue;
                if (byNumber.TryGetValue(e.Number, out var existing))
                {
                    if (e.UpdatedAt > existing.UpdatedAt) byNumber[e.Number] = e;
                }
                else
                {
                    byNumber[e.Number] = e;
                }
            }
        }
        return byNumber.Values.OrderBy(e => e.Number).ToList();
    }
}
