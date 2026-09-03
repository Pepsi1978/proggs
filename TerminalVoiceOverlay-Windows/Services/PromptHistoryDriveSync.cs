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
using Google.Apis.Download;
using Google.Apis.Drive.v3;
using Google.Apis.Services;
using Google.Apis.Upload;
using Google.Apis.Util.Store;
using Microsoft.Extensions.Logging;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Synchronisiert die Prompt-Historie zwischen Mac, Windows und Cloud
/// (Google Drive appDataFolder). Eigenstaendiger Service neben dem
/// bestehenden GoogleDriveBackupService — beide nutzen denselben
/// Secret-Store und denselben OAuth-Refresh-Token, schreiben aber in
/// getrennte Dateien:
/// <list type="bullet">
///   <item>Backup: <c>promptboard-backup.json</c></item>
///   <item>Diese Klasse: <c>prompt-history.json</c> + <c>archive-NNN.md</c></item>
/// </list>
/// Wir teilen den OAuth-Code nicht ueber eine gemeinsame Basisklasse,
/// damit das Promptboard-Core-Interface unveraendert bleibt — der
/// Standalone-Promptboard-App-Build zieht die alten Methoden weiter,
/// ohne dass wir dort ein neues Interface implementieren muessen.
/// </summary>
public sealed class PromptHistoryDriveSync
{
    private const string HistoryFileName = "prompt-history.json";
    private const string AppDataFolderSpace = "appDataFolder";

    private static readonly SemaphoreSlim SyncGate = new(1, 1);
    private readonly PromptBoardSecretStore _secrets;
    private readonly ILogger<PromptHistoryDriveSync>? _logger;
    private static readonly JsonSerializerOptions HistoryJsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = true,
    };

    public PromptHistoryDriveSync(
        PromptBoardSecretStore secrets,
        ILogger<PromptHistoryDriveSync>? logger = null)
    {
        _secrets = secrets;
        _logger = logger;
    }

    /// <summary>
    /// Laedt den Inhalt der lokalen prompt-history.json zu Drive hoch und
    /// raeumt etwaige Duplikate weg. Identisches Cleanup-Muster wie der
    /// bestehende Backup-Service — in Drive bleibt immer genau eine
    /// aktuelle Datei.
    /// </summary>
    public async Task UploadHistoryAsync(string localPath, CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            if (!File.Exists(localPath)) return;
            var bytes = await File.ReadAllBytesAsync(localPath, ct).ConfigureAwait(false);
            var local = JsonSerializer.Deserialize<List<PromptHistoryEntry>>(bytes, HistoryJsonOptions)
                ?? throw new InvalidDataException("Local history JSON is empty.");

            using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
            var ids = await FindAllAsync(drive, HistoryFileName, ct).ConfigureAwait(false);

            if (ids.Count > 0)
            {
                using var cloudBuffer = new MemoryStream();
                EnsureDownloadCompleted(
                    await drive.Files.Get(ids[0]).DownloadAsync(cloudBuffer, ct).ConfigureAwait(false));
                var cloudJson = Encoding.UTF8.GetString(cloudBuffer.ToArray());
                bytes = JsonSerializer.SerializeToUtf8Bytes(MergeEntries(local, cloudJson), HistoryJsonOptions);
            }

            using var content = new MemoryStream(bytes);
            if (ids.Count == 0)
            {
                var meta = new DriveFile
                {
                    Name = HistoryFileName,
                    Parents = new[] { AppDataFolderSpace },
                };
                var create = drive.Files.Create(meta, content, "application/json");
                create.Fields = "id";
                EnsureUploadCompleted(await create.UploadAsync(ct).ConfigureAwait(false));
            }
            else
            {
                var keep = ids[0];
                var update = drive.Files.Update(new DriveFile(), keep, content, "application/json");
                update.Fields = "id";
                EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));

                for (int i = 1; i < ids.Count; i++)
                {
                    try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
                    catch (Exception ex)
                    {
                        _logger?.LogWarning(ex, "History dup-cleanup failed for {Id}", ids[i]);
                    }
                }
            }
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>
    /// Holt die aktuellste prompt-history.json aus Drive. Liefert null
    /// wenn keine vorhanden ist (z.B. erster Start auf einem neuen Geraet
    /// und der andere Rechner hat noch nichts hochgeladen).
    /// </summary>
    public async Task<string?> DownloadHistoryAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
            var ids = await FindAllAsync(drive, HistoryFileName, ct).ConfigureAwait(false);
            if (ids.Count == 0) return null;

            using var buffer = new MemoryStream();
            EnsureDownloadCompleted(
                await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false));
            return Encoding.UTF8.GetString(buffer.ToArray());
        }
        finally
        {
            SyncGate.Release();
        }
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

    // ── Drive-Hilfsfunktionen (eigenstaendig, kein Re-Use aus dem Backup-Service) ──

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

        // Gehaerteter Client (Verbindungs-Timeout, Keepalive, 30 s Gesamt) fuer
        // alle Drive-Dienste — siehe DriveHttp (Vorfall 03.09.2026, 100-s-Haenger).
        return DriveHttp.CreateService(cred);
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
    /// Mergt eine Cloud-Historie-JSON in die lokale Liste: Eintraege per
    /// ID-Dedup zusammenfuehren, nach Timestamp absteigend sortieren. Das
    /// Resultat ist die neue lokale Liste — der Aufrufer (PromptHistoryService)
    /// kann sie schreiben und ggf. ueberzaehlige Eintraege archivieren.
    /// </summary>
    public static List<PromptHistoryEntry> MergeEntries(
        IEnumerable<PromptHistoryEntry> local,
        string cloudJson)
    {
        List<PromptHistoryEntry>? cloud = null;
        try
        {
            cloud = JsonSerializer.Deserialize<List<PromptHistoryEntry>>(
                cloudJson,
                HistoryJsonOptions);
        }
        catch
        {
            // Kaputte Cloud-JSON darf den lokalen Stand nicht zerstoeren.
            cloud = null;
        }

        var merged = new Dictionary<string, PromptHistoryEntry>(StringComparer.OrdinalIgnoreCase);
        foreach (var e in local) if (e is not null && !string.IsNullOrEmpty(e.Id)) merged[e.Id] = e;
        if (cloud is not null)
        {
            foreach (var e in cloud)
            {
                if (e is null || string.IsNullOrEmpty(e.Id)) continue;
                if (!merged.TryGetValue(e.Id, out var existing))
                {
                    merged[e.Id] = e;
                    continue;
                }

                var winner = EffectiveUpdatedAt(e) > EffectiveUpdatedAt(existing) ? e : existing;
                var archivedAt = Max(existing.ArchivedAt, e.ArchivedAt);
                if (archivedAt is not null)
                {
                    winner.ArchivedAt = archivedAt;
                    if (winner.UpdatedAt < archivedAt.Value) winner.UpdatedAt = archivedAt.Value;
                }
                merged[e.Id] = winner;
            }
        }
        return merged.Values
            .OrderByDescending(e => e.Timestamp)
            .ToList();
    }

    private static DateTime EffectiveUpdatedAt(PromptHistoryEntry entry)
    {
        var updatedAt = entry.UpdatedAt == default ? entry.Timestamp : entry.UpdatedAt;
        return entry.ArchivedAt is { } archivedAt && archivedAt > updatedAt
            ? archivedAt
            : updatedAt;
    }

    private static DateTime? Max(DateTime? left, DateTime? right) =>
        left is null ? right : right is null || left >= right ? left : right;
}
