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
using Google.Apis.Download;
using Google.Apis.Drive.v3;
using Google.Apis.Services;
using Google.Apis.Upload;
using Google.Apis.Util.Store;
using Microsoft.Extensions.Logging;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services;

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
    /// only if its terms changed (keeps the user's layout otherwise), uploads only if the
    /// cloud's terms differ, then remembers the result as the new base. Runs at startup
    /// and after every save in the settings dialog.
    /// </summary>
    public async Task SyncAsync(CancellationToken ct = default)
    {
        await SyncGate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var localPath = LocalPath;
            string localText = File.Exists(localPath)
                ? await File.ReadAllTextAsync(localPath, ct).ConfigureAwait(false)
                : string.Empty;
            string? baseText = File.Exists(BasePath)
                ? await File.ReadAllTextAsync(BasePath, ct).ConfigureAwait(false)
                : null;

            using var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
            var ids = await FindAllAsync(drive, VocabularyFileName, ct).ConfigureAwait(false);
            string? cloudText = null;
            if (ids.Count > 0)
            {
                using var buffer = new MemoryStream();
                EnsureDownloadCompleted(
                    await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false));
                cloudText = Encoding.UTF8.GetString(buffer.ToArray());
            }

            var mergedTerms = MergeTerms(baseText, localText, cloudText);
            var merged = Render(localText, mergedTerms);

            if (!Terms(localText).SequenceEqual(mergedTerms, StringComparer.Ordinal))
            {
                Directory.CreateDirectory(Path.GetDirectoryName(localPath)!);
                await File.WriteAllTextAsync(localPath, merged + "\n", ct).ConfigureAwait(false);
            }

            bool cloudDiffers = cloudText is null
                ? mergedTerms.Count > 0
                : !new HashSet<string>(Terms(cloudText), StringComparer.Ordinal).SetEquals(mergedTerms);
            if (cloudDiffers || ids.Count > 1)
                await UploadTextAsync(drive, ids, merged, ct).ConfigureAwait(false);

            Directory.CreateDirectory(Path.GetDirectoryName(BasePath)!);
            await File.WriteAllTextAsync(BasePath, merged + "\n", ct).ConfigureAwait(false);
        }
        finally
        {
            SyncGate.Release();
        }
    }

    /// <summary>Writes <paramref name="text"/> as the single cloud copy and removes duplicates.</summary>
    private async Task UploadTextAsync(DriveService drive, IList<string> ids, string text, CancellationToken ct)
    {
        using var content = new MemoryStream(Encoding.UTF8.GetBytes(text));
        if (ids.Count == 0)
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

        var update = drive.Files.Update(new DriveFile(), ids[0], content, "text/plain");
        update.Fields = "id";
        EnsureUploadCompleted(await update.UploadAsync(ct).ConfigureAwait(false));

        for (int i = 1; i < ids.Count; i++)
        {
            try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
            catch (Exception ex)
            {
                _logger?.LogWarning(ex, "Vocab dup-cleanup failed for {Id}", ids[i]);
            }
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

    /// <summary>
    /// The terms of a vocabulary text: split on line breaks, comma and semicolon,
    /// trimmed, comment lines (#) skipped, deduplicated case-insensitively.
    /// </summary>
    public static List<string> Terms(string? text)
    {
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var terms = new List<string>();
        if (string.IsNullOrEmpty(text)) return terms;
        foreach (var raw in text.Split('\r', '\n'))
        {
            var line = raw.Trim();
            if (line.Length == 0 || line.StartsWith('#')) continue;
            foreach (var part in line.Split(',', ';'))
            {
                var term = part.Trim();
                if (term.Length > 0 && seen.Add(term)) terms.Add(term);
            }
        }
        return terms;
    }

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

    /// <summary>Comment lines of the local file first, then all terms as one comma line.</summary>
    private static string Render(string? localText, List<string> terms)
    {
        var lines = new List<string>();
        if (!string.IsNullOrEmpty(localText))
            foreach (var raw in localText.Split('\r', '\n'))
            {
                var line = raw.Trim();
                if (line.StartsWith('#')) lines.Add(line);
            }
        if (terms.Count > 0) lines.Add(string.Join(", ", terms));
        return string.Join("\n", lines);
    }

    // ── Drive-Hilfsfunktionen (eigenstaendig, wie PromptSlotDriveSync) ──

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
