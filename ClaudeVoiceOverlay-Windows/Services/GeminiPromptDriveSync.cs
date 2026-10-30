using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Drive.v3;
using Google.Apis.Upload;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Synchronisiert die Gemini-Korrektur-Prompts (10 Profil-Dateien + Legacy), den
/// Woerterbuch-Schalter (vocabulary-enabled.txt), die Praeambel und die Schnell-Prompts
/// ueber Google Drive (appDataFolder). Alle Dateien wandern als EIN Bundle
/// (gemini-prompts-bundle.json, Format {savedAt, files} — identisch zu macOS).
///
/// PER-FILE MERGE (replaces the former whole-bundle overwrite): the bundle carries only
/// one savedAt and macOS rebuilds it on every upload, so per-file timestamps cannot
/// survive a round trip. Instead every machine remembers the fingerprints of the state it
/// last agreed on with the cloud (<see cref="BaseFingerprintFileName"/>, machine-local,
/// shared by TVO and CVO). Per file: local unchanged since that state -> the cloud version
/// is taken; local changed -> the local version wins and is uploaded. Files missing in the
/// cloud are never deleted locally, they are uploaded instead. Unknown cloud entries
/// (newer macOS builds) are passed through untouched.
///
/// personal-vocabulary.txt ist hier bewusst NICHT dabei — das Woerterbuch hat
/// seinen eigenen Drei-Wege-Sync (PromptVocabularyDriveSync).
/// </summary>
public sealed class GeminiPromptDriveSync
{
    private const string BundleFileName = "gemini-prompts-bundle.json";
    private const string AppDataFolderSpace = "appDataFolder";
    private const string SyncMarkerFileName = ".gemini-prompts-synced";
    private const string DirtyMarkerFileName = ".gemini-prompts-dirty";
    private const string BaseFingerprintFileName = "gemini-prompts.synced-files.json";
    private const int MaxSyncAttempts = 3;

    private static readonly TimeSpan LockTimeout = TimeSpan.FromSeconds(10);
    private static readonly SemaphoreSlim SyncGate = new(1, 1);
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
        yield return "parakeet-vocabulary-enabled.txt"; // Schalter des Parakeet-Woerterbuchs
        yield return "parakeet-vocabulary.txt";      // Begriffe des Parakeet-Woerterbuchs
        foreach (var name in QuickPromptStore.FileNames()) yield return name; // Schnell-Prompts 1-10 + Kurzbeschreibungen
    }

    private sealed class Bundle
    {
        public string savedAt { get; set; } = "";
        public Dictionary<string, string> files { get; set; } = new();
    }

    private static string MarkerPath => Path.Combine(SkDir, SyncMarkerFileName);
    private static string DirtyMarkerPath => Path.Combine(SkDir, DirtyMarkerFileName);
    // Machine-local on purpose (like the vocabulary base): the SK folder may travel
    // between machines, the "last agreed state" must not.
    private static string BaseFingerprintPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "VoiceOverlays", BaseFingerprintFileName);

    // ── markers (all access under the cross-process prompt-files lock) ──

    private static string ReadMarkerUnlocked()
    {
        try { return (AtomicFile.ReadAllTextShared(MarkerPath) ?? "").Trim(); }
        catch { return ""; }
    }

    private static void MarkDirty()
    {
        using var held = CrossProcessLock.Acquire(SharedLockNames.GeminiPromptFiles,
            TimeSpan.FromSeconds(2), throwOnTimeout: false);
        AtomicFile.WriteAllText(DirtyMarkerPath, Guid.NewGuid().ToString("N"));
    }

    private static string ReadDirtyMarkerUnlocked() =>
        (AtomicFile.ReadAllTextShared(DirtyMarkerPath) ?? "").Trim();

    private static void ClearDirtyMarkerUnlocked(string uploadedGeneration)
    {
        if (string.IsNullOrEmpty(uploadedGeneration)) return;
        try
        {
            if (string.Equals(ReadDirtyMarkerUnlocked(), uploadedGeneration, StringComparison.Ordinal))
                File.Delete(DirtyMarkerPath);
        }
        catch (Exception ex)
        {
            // Best effort: a leftover dirty marker only causes one extra (merging) upload.
            DiagLog.Write("GeminiPromptSync", "dirty marker clear failed", ("err", ex.Message));
        }
    }

    private static Dictionary<string, string>? ReadBaseFingerprintsUnlocked()
    {
        try
        {
            var json = AtomicFile.ReadAllTextShared(BaseFingerprintPath);
            if (string.IsNullOrWhiteSpace(json)) return null;
            var map = JsonSerializer.Deserialize<Dictionary<string, string>>(json);
            return map is null ? null : new Dictionary<string, string>(map, StringComparer.OrdinalIgnoreCase);
        }
        catch (Exception ex)
        {
            DiagLog.Write("GeminiPromptSync", "base fingerprints unreadable, falling back", ("err", ex.Message));
            return null;
        }
    }

    private static string Fingerprint(string content) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(content)));

    // ── local snapshot ──

    private sealed class LocalSnapshot
    {
        public Dictionary<string, string?> Files { get; } = new(StringComparer.OrdinalIgnoreCase);
        public Dictionary<string, DateTime> WriteTimesUtc { get; } = new(StringComparer.OrdinalIgnoreCase);
        public Dictionary<string, string>? BaseFingerprints { get; set; }
        public string Marker { get; set; } = "";
        public string DirtyGeneration { get; set; } = "";
    }

    private static LocalSnapshot ReadLocalSnapshot()
    {
        using var held = CrossProcessLock.Acquire(SharedLockNames.GeminiPromptFiles, LockTimeout);
        var snap = new LocalSnapshot
        {
            BaseFingerprints = ReadBaseFingerprintsUnlocked(),
            Marker = ReadMarkerUnlocked(),
            DirtyGeneration = ReadDirtyMarkerUnlocked(),
        };
        foreach (var name in SyncedFileNames())
        {
            var path = Path.Combine(SkDir, name);
            snap.Files[name] = AtomicFile.ReadAllTextShared(path);
            try { if (File.Exists(path)) snap.WriteTimesUtc[name] = File.GetLastWriteTimeUtc(path); }
            catch { /* optional heuristic input */ }
        }
        return snap;
    }

    // ── merge ──

    private sealed class MergePlan
    {
        /// <summary>The agreed state (what the cloud holds after this sync), allowed names only.</summary>
        public Dictionary<string, string> Result { get; } = new(StringComparer.OrdinalIgnoreCase);

        /// <summary>Cloud entries this build does not know — passed through on upload, never written locally.</summary>
        public Dictionary<string, string> PassThrough { get; } = new(StringComparer.OrdinalIgnoreCase);

        /// <summary>Files whose cloud version must be written locally.</summary>
        public List<string> FromCloud { get; } = new();
    }

    private static MergePlan Merge(LocalSnapshot local, Bundle? cloud, bool localChangeRequested)
    {
        var plan = new MergePlan();
        var allowed = new HashSet<string>(SyncedFileNames(), StringComparer.OrdinalIgnoreCase);
        var cloudFiles = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        if (cloud is not null)
        {
            foreach (var kv in cloud.files)
            {
                if (kv.Value is null) continue;
                if (allowed.Contains(kv.Key) && string.Equals(Path.GetFileName(kv.Key), kv.Key, StringComparison.Ordinal))
                    cloudFiles.TryAdd(kv.Key, kv.Value);
                else
                    plan.PassThrough.TryAdd(kv.Key, kv.Value);
            }
        }

        // Fallback without remembered fingerprints (first run after this update):
        // "unchanged" = file not written since the last sync mark.
        DateTimeOffset? markerTime = DateTimeOffset.TryParse(local.Marker, out var m) ? m : null;
        DateTimeOffset? cloudTime = cloud is not null && DateTimeOffset.TryParse(cloud.savedAt, out var c) ? c : null;
        bool dirty = !string.IsNullOrEmpty(local.DirtyGeneration);

        foreach (var name in SyncedFileNames())
        {
            local.Files.TryGetValue(name, out var localContent);
            cloudFiles.TryGetValue(name, out var cloudContent);

            if (cloudContent is null)
            {
                // Missing in the cloud: never delete locally, upload it instead.
                if (localContent is not null) plan.Result[name] = localContent;
                continue;
            }
            if (localContent is null)
            {
                plan.Result[name] = cloudContent;
                plan.FromCloud.Add(name);
                continue;
            }
            if (string.Equals(localContent, cloudContent, StringComparison.Ordinal))
            {
                plan.Result[name] = localContent;
                continue;
            }

            bool localUnchanged;
            if (local.BaseFingerprints is { } baseFps)
            {
                localUnchanged = baseFps.TryGetValue(name, out var fp) &&
                                 string.Equals(fp, Fingerprint(localContent), StringComparison.Ordinal);
            }
            else if (markerTime is { } mt)
            {
                // One-time transition (marker from the old build, no fingerprints yet):
                // exactly the old semantics — a newer cloud bundle wins unless a local
                // save is pending. (File mtimes are useless here: the old apply wrote the
                // files at apply time, later than the cloud savedAt stored as marker, so
                // they would always look "changed".)
                localUnchanged = cloudTime is { } ct && ct > mt && !(localChangeRequested || dirty);
            }
            else
            {
                // Never synced on this machine: old behaviour — a pending local save wins,
                // otherwise the cloud is the reference.
                localUnchanged = !(localChangeRequested || dirty);
            }

            if (localUnchanged)
            {
                plan.Result[name] = cloudContent;
                plan.FromCloud.Add(name);
            }
            else
            {
                plan.Result[name] = localContent;
            }
        }
        return plan;
    }

    private static bool SameFiles(Dictionary<string, string> result, Bundle cloud)
    {
        var cloudAllowed = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        var allowed = new HashSet<string>(SyncedFileNames(), StringComparer.OrdinalIgnoreCase);
        foreach (var kv in cloud.files)
            if (kv.Value is not null && allowed.Contains(kv.Key)) cloudAllowed.TryAdd(kv.Key, kv.Value);
        if (cloudAllowed.Count != result.Count) return false;
        foreach (var kv in result)
            if (!cloudAllowed.TryGetValue(kv.Key, out var v) || !string.Equals(v, kv.Value, StringComparison.Ordinal))
                return false;
        return true;
    }

    // ── public entry points ──

    /// <summary>Merges the local state with the cloud and uploads it (after every local save).</summary>
    public async Task UploadAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            await SyncCoreAsync(localChangeRequested: true, ct).ConfigureAwait(false);
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>Wird nach erfolgreichem Backup-Upload gefeuert (vom Background-Thread)
    /// — der Abonnent setzt damit die sichtbare Sync-Bestaetigung im PromptBoard.</summary>
    public static event Action? UploadSucceeded;

    /// <summary>
    /// Holt das Cloud-Bundle, uebernimmt pro Datei die Cloud-Fassung, wo lokal seit dem
    /// letzten Abgleich nichts geaendert wurde, und laedt lokale Aenderungen bzw. in der
    /// Cloud fehlende Dateien hoch. Einmal beim Start.
    /// </summary>
    public async Task SyncFromCloudAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            await SyncCoreAsync(localChangeRequested: false, ct).ConfigureAwait(false);
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>Wird nach dem Anwenden neuerer Cloud-Dateien gefeuert (vom
    /// Background-Thread) — das Overlay frischt damit die Schnell-Prompt-Tooltips auf.</summary>
    public static event Action? CloudApplied;

    private async Task SyncCoreAsync(bool localChangeRequested, CancellationToken ct)
    {
        using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        for (int attempt = 1; attempt <= MaxSyncAttempts; attempt++)
        {
            ct.ThrowIfCancellationRequested();
            var listing = await DriveHttp.ListAppDataFilesAsync(drive, BundleFileName, ct).ConfigureAwait(false);
            var cloud = await DownloadMergedBundleAsync(drive, listing, ct).ConfigureAwait(false);
            var snapshot = ReadLocalSnapshot();
            var plan = Merge(snapshot, cloud, localChangeRequested);

            bool needsUpload = (cloud is null ? plan.Result.Count > 0 : !SameFiles(plan.Result, cloud)) || listing.Count > 1;
            string savedAt = cloud?.savedAt ?? "";
            if (needsUpload)
            {
                var current = await DriveHttp.ListAppDataFilesAsync(drive, BundleFileName, ct).ConfigureAwait(false);
                if (!string.Equals(DriveHttp.VersionOf(current), DriveHttp.VersionOf(listing), StringComparison.Ordinal))
                {
                    DiagLog.Write("GeminiPromptSync", "cloud bundle changed during merge, merging again", ("attempt", attempt));
                    continue;
                }
                savedAt = DateTimeOffset.UtcNow.ToString("O");
                var upload = new Bundle { savedAt = savedAt };
                foreach (var kv in plan.PassThrough) upload.files[kv.Key] = kv.Value;
                foreach (var kv in plan.Result) upload.files[kv.Key] = kv.Value;
                await UploadBundleAsync(drive, current, upload, ct).ConfigureAwait(false);
            }

            if (!CommitLocal(snapshot, plan, savedAt))
            {
                DiagLog.Write("GeminiPromptSync", "local prompt files changed during sync, merging again", ("attempt", attempt));
                continue;
            }

            if (plan.FromCloud.Count > 0) CloudApplied?.Invoke();
            if (needsUpload || localChangeRequested) UploadSucceeded?.Invoke();
            return;
        }
        DiagLog.Write("GeminiPromptSync", "sync gave up after concurrent changes; dirty marker kept for next run");
    }

    /// <summary>
    /// Applies the cloud-won files, then records the agreed state (fingerprints, marker)
    /// and clears the dirty marker — all under the cross-process lock, so TVO and CVO
    /// never interleave. Returns false (nothing written) when a local file changed since
    /// the snapshot. On a write error every touched file is restored and neither marker
    /// nor fingerprints are updated.
    /// </summary>
    private static bool CommitLocal(LocalSnapshot snapshot, MergePlan plan, string savedAt)
    {
        using var held = CrossProcessLock.Acquire(SharedLockNames.GeminiPromptFiles, LockTimeout);

        foreach (var name in SyncedFileNames())
        {
            var now = AtomicFile.ReadAllTextShared(Path.Combine(SkDir, name));
            snapshot.Files.TryGetValue(name, out var before);
            if (!string.Equals(now, before, StringComparison.Ordinal)) return false;
        }

        var written = new List<string>();
        try
        {
            foreach (var name in plan.FromCloud)
            {
                AtomicFile.WriteAllText(Path.Combine(SkDir, name), plan.Result[name]);
                written.Add(name);
            }

            if (!string.IsNullOrEmpty(savedAt)) AtomicFile.WriteAllText(MarkerPath, savedAt);
            ClearDirtyMarkerUnlocked(snapshot.DirtyGeneration);

            // LAST step: the fingerprints declare "this content is agreed with the cloud".
            // Written after everything else, so any earlier failure leaves old fingerprints
            // next to rolled-back files — consistent, never "stale local looks changed".
            var fps = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            foreach (var kv in plan.Result) fps[kv.Key] = Fingerprint(kv.Value);
            AtomicFile.WriteAllText(BaseFingerprintPath, JsonSerializer.Serialize(fps));
            return true;
        }
        catch (Exception applyError)
        {
            var rollbackErrors = new List<Exception>();
            foreach (var name in written)
            {
                try
                {
                    var path = Path.Combine(SkDir, name);
                    snapshot.Files.TryGetValue(name, out var before);
                    if (before is null) File.Delete(path);
                    else AtomicFile.WriteAllText(path, before);
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
    }

    /// <summary>
    /// Downloads the bundle. Several copies (two devices created it at once) are combined:
    /// the newest savedAt is the primary, files only present in older copies are added,
    /// so nothing is deleted unseen during the duplicate cleanup.
    /// </summary>
    private static async Task<Bundle?> DownloadMergedBundleAsync(
        DriveService drive, IReadOnlyList<DriveFile> listing, CancellationToken ct)
    {
        var bundles = new List<Bundle>();
        foreach (var f in listing.Take(10))
        {
            var json = await DriveHttp.DownloadTextAsync(drive, f.Id, ct).ConfigureAwait(false);
            try
            {
                var b = JsonSerializer.Deserialize<Bundle>(json);
                if (b is not null) { b.files ??= new(); bundles.Add(b); }
            }
            catch (JsonException ex)
            {
                if (bundles.Count == 0 && f == listing[0])
                    throw new InvalidDataException("Google Drive bundle is not valid JSON.", ex);
                DiagLog.Write("GeminiPromptSync", "duplicate bundle unreadable, ignored", ("id", f.Id), ("err", ex.Message));
            }
        }
        if (bundles.Count == 0) return null;

        static DateTimeOffset When(Bundle b) =>
            DateTimeOffset.TryParse(b.savedAt, out var t) ? t : DateTimeOffset.MinValue;
        var ordered = bundles.OrderByDescending(When).ToList();
        var merged = new Bundle { savedAt = ordered[0].savedAt };
        foreach (var b in ordered)
            foreach (var kv in b.files)
                merged.files.TryAdd(kv.Key, kv.Value);
        return merged;
    }

    private static async Task UploadBundleAsync(DriveService drive, IReadOnlyList<DriveFile> listing, Bundle bundle, CancellationToken ct)
    {
        using var content = new MemoryStream(JsonSerializer.SerializeToUtf8Bytes(bundle));
        if (listing.Count == 0)
        {
            var meta = new DriveFile { Name = BundleFileName, Parents = new[] { AppDataFolderSpace } };
            var create = drive.Files.Create(meta, content, "application/json");
            create.Fields = "id";
            EnsureUploadCompleted(await create.UploadAsync(ct).ConfigureAwait(false));
            return;
        }

        var update = drive.Files.Update(new DriveFile(), listing[0].Id, content, "application/json");
        update.Fields = "id";
        EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));
        // Safe now: the duplicates' files are part of the uploaded bundle.
        for (int i = 1; i < listing.Count; i++)
        {
            try { await drive.Files.Delete(listing[i].Id).ExecuteAsync(ct).ConfigureAwait(false); }
            catch { /* dup cleanup best-effort */ }
        }
    }

    private static void EnsureUploadCompleted(IUploadProgress progress)
    {
        progress.ThrowOnFailure();
        if (progress.Status != UploadStatus.Completed)
            throw new IOException($"Google Drive upload did not complete: {progress.Status}");
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

    private static GoogleAuthorizationCodeFlow BuildFlow(string id, string secret) =>
        DriveHttp.CreateFlow(id, secret, DriveService.Scope.DriveAppdata);

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
}
