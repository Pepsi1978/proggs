using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Drive.v3;
using Google.Apis.Upload;
using Microsoft.Extensions.Logging;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Syncs the personal vocabulary (<c>personal-vocabulary.txt</c> in the SK folder)
/// through Google Drive between all devices and both overlays. Own file in the
/// <c>appDataFolder</c>, same OAuth mechanics as <see cref="PromptSlotDriveSync"/>.
///
/// THREE-WAY MERGE (Frank's decision 2026-10-22, replaces the former pure union):
/// every machine remembers the state it last agreed on with the cloud
/// (<see cref="BasePath"/>). New terms from any device are kept; a term deleted on
/// ONE side since that last sync disappears everywhere, and a changed spelling
/// (github -> GitHub) wins over the old one. Without a remembered state (first run)
/// it is a plain union, so nothing is lost that was not explicitly deleted.
/// </summary>
public sealed class PromptVocabularyDriveSync
{
    public const string VocabularyFileName = "personal-vocabulary.txt";
    private const string AppDataFolderSpace = "appDataFolder";

    /// <summary>At most this many merge rounds when the cloud or the local file moves underneath us.</summary>
    private const int MaxSyncAttempts = 3;

    /// <summary>
    /// A local file without any term while the base still has more than this many terms
    /// is treated as "unknown" (truncated/emptied by a crash), never as "user deleted all".
    /// </summary>
    private const int SuspiciousEmptyBaseTerms = 5;

    private static readonly TimeSpan LockTimeout = TimeSpan.FromSeconds(10);

    private static readonly SemaphoreSlim SyncGate = new(1, 1);
    private readonly PromptBoardSecretStore _secrets;
    private readonly ILogger<PromptVocabularyDriveSync>? _logger;

    public PromptVocabularyDriveSync(
        PromptBoardSecretStore secrets,
        ILogger<PromptVocabularyDriveSync>? logger = null)
    {
        _secrets = secrets;
        _logger = logger;
    }

    /// <summary>
    /// Local path of the vocabulary file -- same SK folder as the Gemini correction
    /// prompts, so TVO and CVO on ONE machine share the same file.
    /// </summary>
    public static string LocalPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "SK", "VoiceOverlays", VocabularyFileName);

    /// <summary>
    /// The state this machine last agreed on with the cloud (base of the three-way
    /// merge). Machine-local on purpose: shared by TVO and CVO, never synced.
    /// </summary>
    public static string BasePath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "VoiceOverlays", "personal-vocabulary.synced.txt");

    /// <summary>
    /// Three-way sync with Drive: merges base, local and cloud, rewrites the local file
    /// only if its term set changed (keeps the user's layout otherwise), uploads only if the
    /// cloud's terms differ, then remembers the result as the new base. Runs at startup
    /// and after every save in the settings dialog.
    ///
    /// Safety (TVO and CVO share the file and the base on one machine):
    /// - local file and base are read and committed under a cross-process lock and written
    ///   atomically; the commit is skipped and the round repeated when the local file was
    ///   changed by someone else during the network part;
    /// - right before the upload the cloud listing is re-read; if another device uploaded
    ///   meanwhile, the round is repeated with the new cloud state (max 3 rounds);
    /// - duplicate cloud copies are merged (union) before they are removed;
    /// - an empty local file next to a big base is treated as unknown, not as mass delete.
    /// </summary>
    public async Task SyncAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
            for (int attempt = 1; attempt <= MaxSyncAttempts; attempt++)
            {
                ct.ThrowIfCancellationRequested();
                var (localText, baseText) = ReadLocalAndBase();

                var files = await DriveHttp.ListAppDataFilesAsync(drive, VocabularyFileName, ct).ConfigureAwait(false);
                string? cloudText = await DownloadUnionAsync(drive, files, ct).ConfigureAwait(false);

                // A truncated or emptied local file must not wipe every term on every device.
                string effectiveLocal = localText;
                string? structure = localText;
                int baseCount = Terms(baseText).Count;
                if (Terms(localText).Count == 0 && baseCount > SuspiciousEmptyBaseTerms)
                {
                    DiagLog.Write("VocabSync", "local vocabulary empty while base has terms - treated as unknown, cloud/base wins",
                        ("baseTerms", baseCount), ("localBytes", localText.Length));
                    effectiveLocal = baseText ?? string.Empty;
                    structure = cloudText ?? baseText;
                }

                var mergedTerms = MergeTerms(baseText, effectiveLocal, cloudText);
                var merged = Render(structure, mergedTerms);

                bool cloudDiffers = cloudText is null
                    ? mergedTerms.Count > 0
                    : !SameTermSet(Terms(cloudText), mergedTerms);
                if (cloudDiffers || files.Count > 1)
                {
                    // Optimistic concurrency: upload only if the cloud is still what we merged.
                    var current = await DriveHttp.ListAppDataFilesAsync(drive, VocabularyFileName, ct).ConfigureAwait(false);
                    if (!string.Equals(DriveHttp.VersionOf(current), DriveHttp.VersionOf(files), StringComparison.Ordinal))
                    {
                        DiagLog.Write("VocabSync", "cloud changed during merge, merging again", ("attempt", attempt));
                        continue;
                    }
                    await UploadTextAsync(drive, current, merged, ct).ConfigureAwait(false);
                }

                if (CommitLocal(localText, mergedTerms, merged)) return;
                DiagLog.Write("VocabSync", "local file changed during sync, merging again", ("attempt", attempt));
            }
            DiagLog.Write("VocabSync", "sync gave up after concurrent changes; next run retries", ("attempts", MaxSyncAttempts));
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>Snapshot of the local file and the base, consistent with each other.</summary>
    private static (string Local, string? Base) ReadLocalAndBase()
    {
        using var held = CrossProcessLock.Acquire(SharedLockNames.Vocabulary, LockTimeout);
        return (AtomicFile.ReadAllTextShared(LocalPath) ?? string.Empty,
                AtomicFile.ReadAllTextShared(BasePath));
    }

    /// <summary>
    /// Writes the merge result locally — but only if the local file is still exactly the
    /// snapshot the merge was based on. Returns false when someone changed it meanwhile.
    /// </summary>
    private static bool CommitLocal(string localSnapshot, List<string> mergedTerms, string merged)
    {
        using var held = CrossProcessLock.Acquire(SharedLockNames.Vocabulary, LockTimeout);
        var current = AtomicFile.ReadAllTextShared(LocalPath) ?? string.Empty;
        if (!string.Equals(current, localSnapshot, StringComparison.Ordinal)) return false;
        if (!SameTermSet(Terms(localSnapshot), mergedTerms))
            AtomicFile.WriteAllText(LocalPath, merged + "\n");
        AtomicFile.WriteAllText(BasePath, merged + "\n");
        return true;
    }

    /// <summary>
    /// Cloud state = union of ALL copies (normally one). Duplicates appear when two devices
    /// created the file at the same time; their terms are kept instead of being deleted unseen.
    /// </summary>
    private static async Task<string?> DownloadUnionAsync(DriveService drive, IReadOnlyList<DriveFile> files, CancellationToken ct)
    {
        if (files.Count == 0) return null;
        var parts = new List<string>();
        foreach (var f in files.Take(10))
            parts.Add(await DriveHttp.DownloadTextAsync(drive, f.Id, ct).ConfigureAwait(false));
        return string.Join("\n", parts);
    }

    /// <summary>Writes <paramref name="text"/> as the single cloud copy and removes duplicates.</summary>
    private async Task UploadTextAsync(DriveService drive, IReadOnlyList<DriveFile> files, string text, CancellationToken ct)
    {
        using var content = new MemoryStream(Encoding.UTF8.GetBytes(text));
        if (files.Count == 0)
        {
            var meta = new DriveFile
            {
                Name = VocabularyFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(meta, content, "text/plain");
            create.Fields = "id";
            EnsureUploadCompleted(await create.UploadAsync(ct).ConfigureAwait(false));
            return;
        }

        var update = drive.Files.Update(new DriveFile(), files[0].Id, content, "text/plain");
        update.Fields = "id";
        EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));

        // Safe now: the duplicates' terms are part of the merged text just uploaded.
        for (int i = 1; i < files.Count; i++)
        {
            try { await drive.Files.Delete(files[i].Id).ExecuteAsync(ct).ConfigureAwait(false); }
            catch (Exception ex)
            {
                _logger?.LogWarning(ex, "Vocab dup-cleanup failed for {Id}", files[i].Id);
            }
        }
    }

    private static void EnsureUploadCompleted(IUploadProgress progress)
    {
        progress.ThrowOnFailure();
        if (progress.Status != UploadStatus.Completed)
            throw new IOException($"Google Drive upload did not complete: {progress.Status}");
    }

    /// <summary>
    /// The terms of a vocabulary text. Delegates to the shared tokenizer
    /// (<see cref="PersonalVocabulary.Tokenize"/>) so sync and transcription always agree.
    /// </summary>
    public static List<string> Terms(string? text) => PersonalVocabulary.Tokenize(text);

    private static bool SameTermSet(List<string> a, List<string> b) =>
        new HashSet<string>(a, StringComparer.Ordinal).SetEquals(b);

    /// <summary>
    /// Three-way merge of the term lists, local order first. A term in the base that is
    /// missing on one side was deleted there since the last sync and is dropped. A term
    /// not in the base is new and kept. The spelling that changed since the base wins.
    /// No cloud copy yet: local is the truth (seeds the cloud). No base: plain union.
    /// </summary>
    public static List<string> MergeTerms(string? baseText, string? localText, string? cloudText)
    {
        var local = Terms(localText);
        if (cloudText is null) return local;
        var cloud = Terms(cloudText);
        var baseMap = ToMap(Terms(baseText));
        var localMap = ToMap(local);
        var cloudMap = ToMap(cloud);

        var result = new List<string>();
        var done = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var term in local.Concat(cloud))
        {
            if (!done.Add(term)) continue;
            bool inBase = baseMap.TryGetValue(term, out var b);
            bool inLocal = localMap.TryGetValue(term, out var l);
            bool inCloud = cloudMap.TryGetValue(term, out var c);
            if (inBase && (!inLocal || !inCloud)) continue;
            string spelling = inLocal && (!inBase || l != b) ? l!
                : inCloud && (!inBase || c != b) ? c!
                : (l ?? c)!;
            result.Add(spelling);
        }
        return result;
    }

    private static Dictionary<string, string> ToMap(List<string> terms)
    {
        var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var t in terms) map.TryAdd(t, t);
        return map;
    }

    /// <summary>
    /// Renders <paramref name="terms"/> while keeping the layout of
    /// <paramref name="structureText"/> (normally the local file): comment lines, blank
    /// lines, section order and line grouping stay where they are. A line whose terms all
    /// survived unchanged is kept byte for byte (so "Backen -> Backend, nur im Code" stays
    /// exactly as written as long as none of its parts was deleted); deleted terms are
    /// removed from their line (an emptied line disappears), a changed spelling is
    /// substituted in place, and new terms (from the cloud) are appended, one per line.
    /// </summary>
    internal static string Render(string? structureText, List<string> terms)
    {
        var wanted = ToMap(terms);
        var placed = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var lines = new List<string>();
        if (!string.IsNullOrEmpty(structureText))
        {
            foreach (var rawLine in structureText.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n'))
            {
                var line = rawLine.TrimEnd();
                var trimmed = line.Trim();
                if (trimmed.Length == 0 || trimmed.StartsWith('#'))
                {
                    lines.Add(line);
                    continue;
                }

                var parts = PersonalVocabulary.SplitLine(trimmed).ToList();
                bool unchanged = parts.All(p =>
                    wanted.TryGetValue(p, out var spelling) && string.Equals(spelling, p, StringComparison.Ordinal));
                if (unchanged)
                {
                    lines.Add(line);
                    foreach (var p in parts) placed.Add(p);
                    continue;
                }

                var kept = new List<string>();
                foreach (var p in parts)
                {
                    if (wanted.TryGetValue(p, out var spelling) && placed.Add(spelling)) kept.Add(spelling);
                }
                if (kept.Count > 0) lines.Add(string.Join(", ", kept));
            }
        }

        while (lines.Count > 0 && lines[^1].Trim().Length == 0) lines.RemoveAt(lines.Count - 1);
        foreach (var t in terms)
        {
            if (placed.Add(t)) lines.Add(t);
        }
        return string.Join("\n", lines);
    }

    // ── Drive-Hilfsfunktionen (eigenstaendig, wie PromptSlotDriveSync) ──

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
