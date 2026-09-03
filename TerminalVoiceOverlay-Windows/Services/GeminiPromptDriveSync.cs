using System;
using System.Collections.Generic;
using System.IO;
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
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Synchronisiert die Gemini-Korrektur-Prompts (10 Profil-Dateien + Legacy) und
/// den Woerterbuch-Schalter (vocabulary-enabled.txt) ueber Google Drive
/// (appDataFolder). Anders als der Vokabular-Sync (verlustfreie Vereinigung)
/// gilt hier LWW: die Fassung mit dem neueren savedAt-Zeitstempel gewinnt
/// (Frank-Wunsch 2026-06-22). Alle Dateien wandern als EIN Bundle
/// (gemini-prompts-bundle.json); ein lokaler Marker (.gemini-prompts-synced)
/// haelt den zuletzt angewendeten/hochgeladenen Stand fest, damit beim Start nur
/// ein WIRKLICH neueres Cloud-Bundle den lokalen Stand ueberschreibt.
///
/// personal-vocabulary.txt ist hier bewusst NICHT dabei — das Woerterbuch hat
/// seinen eigenen, verlustfreien Vereinigungs-Sync (PromptVocabularyDriveSync).
/// </summary>
public sealed class GeminiPromptDriveSync
{
    private const string BundleFileName = "gemini-prompts-bundle.json";
    private const string AppDataFolderSpace = "appDataFolder";
    private const string SyncMarkerFileName = ".gemini-prompts-synced";
    private const string DirtyMarkerFileName = ".gemini-prompts-dirty";

    private static readonly SemaphoreSlim SyncGate = new(1, 1);
    private static readonly object DirtyMarkerGate = new();
    private readonly PromptBoardSecretStore _secrets;

    public GeminiPromptDriveSync(PromptBoardSecretStore secrets)
    {
        _secrets = secrets;
    }

    private static string SkDir => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "SK", "VoiceOverlays");

    // Die vom Sync erfassten Dateinamen (Prompts + Schalter).
    private static IEnumerable<string> SyncedFileNames()
    {
        yield return "gemini-correction-prompt-standard.txt";
        yield return "gemini-correction-prompt-programmierung.txt";
        yield return "gemini-correction-prompt-meta.txt";
        for (int i = 4; i <= 10; i++) yield return $"gemini-correction-prompt-{i:D2}.txt";
        yield return "gemini-correction-prompt.txt"; // Legacy-Sammeldatei
        yield return "vocabulary-enabled.txt";       // Woerterbuch-Schalter
        yield return "vocabulary-preamble.txt";      // Woerterbuch-Einleitungstext
    }

    private sealed class Bundle
    {
        public string savedAt { get; set; } = "";
        public Dictionary<string, string> files { get; set; } = new();
    }

    private static string MarkerPath => Path.Combine(SkDir, SyncMarkerFileName);
    private static string DirtyMarkerPath => Path.Combine(SkDir, DirtyMarkerFileName);

    private static string ReadMarker()
    {
        try { return File.Exists(MarkerPath) ? File.ReadAllText(MarkerPath).Trim() : ""; }
        catch { return ""; }
    }

    private static void WriteMarker(string savedAt)
    {
        Directory.CreateDirectory(SkDir);
        File.WriteAllText(MarkerPath, savedAt);
    }

    private static void MarkDirty()
    {
        lock (DirtyMarkerGate)
        {
            Directory.CreateDirectory(SkDir);
            File.WriteAllText(DirtyMarkerPath, Guid.NewGuid().ToString("N"));
        }
    }

    private static string ReadDirtyMarker()
    {
        lock (DirtyMarkerGate)
        {
            return File.Exists(DirtyMarkerPath) ? File.ReadAllText(DirtyMarkerPath).Trim() : "";
        }
    }

    private static void ClearDirtyMarker(string uploadedGeneration)
    {
        if (string.IsNullOrEmpty(uploadedGeneration)) return;
        lock (DirtyMarkerGate)
        {
            if (string.Equals(ReadDirtyMarker(), uploadedGeneration, StringComparison.Ordinal))
                File.Delete(DirtyMarkerPath);
        }
    }

    private static Bundle BuildLocalBundle(string savedAt)
    {
        var b = new Bundle { savedAt = savedAt };
        foreach (var name in SyncedFileNames())
        {
            var p = Path.Combine(SkDir, name);
            try { b.files[name] = File.ReadAllText(p); }
            catch (FileNotFoundException) { }
            catch (DirectoryNotFoundException) { }
        }
        return b;
    }

    /// <summary>Laedt den aktuellen lokalen Stand SOFORT zu Drive hoch (savedAt = jetzt).</summary>
    public async Task UploadAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            await UploadCoreAsync(ct).ConfigureAwait(false);
        }
        finally
        {
            SyncGate.Release();
        }
    }

    private async Task UploadCoreAsync(CancellationToken ct)
    {
        var savedAt = DateTimeOffset.UtcNow.ToString("O");
        var dirtyGeneration = ReadDirtyMarker();
        var bundle = BuildLocalBundle(savedAt);
        if (bundle.files.Count == 0) return; // lokal nichts vorhanden — nichts zu sichern

        var json = JsonSerializer.SerializeToUtf8Bytes(bundle);
        using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, BundleFileName, ct).ConfigureAwait(false);

        using var content = new MemoryStream(json);
        if (ids.Count == 0)
        {
            var meta = new DriveFile { Name = BundleFileName, Parents = new[] { AppDataFolderSpace } };
            var create = drive.Files.Create(meta, content, "application/json");
            create.Fields = "id";
            EnsureUploadCompleted(await create.UploadAsync(ct).ConfigureAwait(false));
        }
        else
        {
            var update = drive.Files.Update(new DriveFile(), ids[0], content, "application/json");
            update.Fields = "id";
            EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));
            for (int i = 1; i < ids.Count; i++)
            {
                try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
                catch { /* dup cleanup best-effort */ }
            }
        }
        WriteMarker(savedAt);
        ClearDirtyMarker(dirtyGeneration);
        UploadSucceeded?.Invoke();
    }

    /// <summary>Wird nach erfolgreichem Backup-Upload gefeuert (vom Background-Thread)
    /// — der Abonnent setzt damit die sichtbare Sync-Bestaetigung im PromptBoard.</summary>
    public static event Action? UploadSucceeded;

    /// <summary>
    /// Holt das Cloud-Bundle und wendet es lokal an, WENN sein savedAt neuer ist
    /// als der lokale Marker (LWW). Kein Cloud-Bundle vorhanden -> lokalen Stand
    /// als Saat hochladen. Einmal beim Start.
    /// </summary>
    public async Task SyncFromCloudAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            await SyncFromCloudCoreAsync(ct).ConfigureAwait(false);
        }
        finally
        {
            SyncGate.Release();
        }
    }

    private async Task SyncFromCloudCoreAsync(CancellationToken ct)
    {
        if (!string.IsNullOrEmpty(ReadDirtyMarker()))
        {
            await UploadCoreAsync(ct).ConfigureAwait(false);
            return;
        }

        using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, BundleFileName, ct).ConfigureAwait(false);
        if (ids.Count == 0)
        {
            await UploadCoreAsync(ct).ConfigureAwait(false); // Saat
            return;
        }

        using var buffer = new MemoryStream();
        EnsureDownloadCompleted(
            await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false));
        Bundle? cloud;
        try { cloud = JsonSerializer.Deserialize<Bundle>(buffer.ToArray()); }
        catch (JsonException ex) { throw new InvalidDataException("Google Drive bundle is not valid JSON.", ex); }
        if (cloud is null || string.IsNullOrEmpty(cloud.savedAt)) return;

        if (!IsNewerThanMarker(cloud.savedAt, ReadMarker())) return;

        ApplyCloudFiles(cloud.files);
        WriteMarker(cloud.savedAt);
    }

    private static void ApplyCloudFiles(Dictionary<string, string> files)
    {
        Directory.CreateDirectory(SkDir);
        var allowed = new HashSet<string>(SyncedFileNames(), StringComparer.OrdinalIgnoreCase);
        var desired = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var kv in files)
        {
            if (!allowed.Contains(kv.Key) || !string.Equals(Path.GetFileName(kv.Key), kv.Key, StringComparison.Ordinal))
                throw new InvalidDataException($"Bundle contains an unsupported file name: {kv.Key}");
            if (!desired.TryAdd(kv.Key, kv.Value))
                throw new InvalidDataException($"Duplicate bundle target: {kv.Key}");
        }

        var temps = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        var backups = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        var existed = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        try
        {
            foreach (var kv in desired)
            {
                var target = Path.Combine(SkDir, kv.Key);
                var temp = target + $".{Guid.NewGuid():N}.tmp";
                File.WriteAllText(temp, kv.Value);
                temps[target] = temp;
            }

            foreach (var name in allowed)
            {
                var target = Path.Combine(SkDir, name);
                if (!File.Exists(target)) continue;
                var backup = target + $".{Guid.NewGuid():N}.bak";
                File.Copy(target, backup);
                backups[target] = backup;
                existed.Add(target);
            }

            foreach (var name in allowed)
            {
                var target = Path.Combine(SkDir, name);
                if (temps.TryGetValue(target, out var temp))
                {
                    if (File.Exists(target)) File.Replace(temp, target, destinationBackupFileName: null);
                    else File.Move(temp, target);
                }
                else
                {
                    File.Delete(target);
                }
            }
        }
        catch (Exception applyError)
        {
            var rollbackErrors = new List<Exception>();
            foreach (var name in allowed)
            {
                var target = Path.Combine(SkDir, name);
                try
                {
                    if (existed.Contains(target)) File.Copy(backups[target], target, overwrite: true);
                    else File.Delete(target);
                }
                catch (Exception ex) { rollbackErrors.Add(ex); }
            }
            if (rollbackErrors.Count > 0)
            {
                rollbackErrors.Insert(0, applyError);
                throw new AggregateException("Bundle apply and rollback failed.", rollbackErrors);
            }
            throw;
        }
        finally
        {
            foreach (var path in temps.Values)
                TryDeleteTempFile(path);
            foreach (var path in backups.Values)
                TryDeleteTempFile(path);
        }
    }

    private static bool IsNewerThanMarker(string cloudSavedAt, string marker)
    {
        if (!DateTimeOffset.TryParse(cloudSavedAt, out var cloudTime))
            throw new InvalidDataException($"Bundle has an invalid savedAt value: {cloudSavedAt}");
        if (string.IsNullOrWhiteSpace(marker)) return true;
        return !DateTimeOffset.TryParse(marker, out var markerTime) || cloudTime > markerTime;
    }

    private static void TryDeleteTempFile(string path)
    {
        try
        {
            File.Delete(path);
        }
        catch (Exception ex)
        {
            DiagLog.Write("GeminiPromptSync", "temp cleanup failed", ("path", path), ("err", ex.Message));
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

    // ── Fire-and-forget statische Wrapper (Editor-/Schalter-Save, App-Start) ──

    /// <summary>Nach jedem Speichern eines Prompts/Schalters aufrufen.</summary>
    public static void TryUpload()
    {
        try { MarkDirty(); }
        catch (Exception ex)
        {
            DiagLog.Write("GeminiPromptSync", "dirty marker failed", ("err", ex.Message));
            return;
        }
        _ = Task.Run(async () =>
        {
            try
            {
                var store = PromptBoardHost.Get<PromptBoardSecretStore>();
                await new GeminiPromptDriveSync(store).UploadAsync().ConfigureAwait(false);
            }
            catch (Exception ex) { DiagLog.Write("GeminiPromptSync", "backup upload failed", ("err", ex.Message), ("type", ex.GetType().Name)); }
        });
    }

    /// <summary>Einmal beim App-Start aufrufen.</summary>
    public static void TrySyncFromCloud()
    {
        _ = Task.Run(async () =>
        {
            try
            {
                var store = PromptBoardHost.Get<PromptBoardSecretStore>();
                await new GeminiPromptDriveSync(store).SyncFromCloudAsync().ConfigureAwait(false);
            }
            catch (Exception ex) { DiagLog.Write("GeminiPromptSync", "cloud sync failed", ("err", ex.Message), ("type", ex.GetType().Name)); }
        });
    }

    // ── Drive-Hilfsfunktionen (gleiche OAuth-Mechanik wie PromptVocabularyDriveSync) ──

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
}
