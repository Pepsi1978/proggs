using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Drive.v3;
using Google.Apis.Services;
using Google.Apis.Util.Store;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services;

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

    private static string ReadMarker()
    {
        try { return File.Exists(MarkerPath) ? File.ReadAllText(MarkerPath).Trim() : ""; }
        catch { return ""; }
    }

    private static void WriteMarker(string savedAt)
    {
        try { Directory.CreateDirectory(SkDir); File.WriteAllText(MarkerPath, savedAt); }
        catch { /* best-effort */ }
    }

    private static Bundle BuildLocalBundle(string savedAt)
    {
        var b = new Bundle { savedAt = savedAt };
        foreach (var name in SyncedFileNames())
        {
            var p = Path.Combine(SkDir, name);
            if (File.Exists(p))
            {
                try { b.files[name] = File.ReadAllText(p); } catch { /* skip unreadable */ }
            }
        }
        return b;
    }

    /// <summary>Laedt den aktuellen lokalen Stand SOFORT zu Drive hoch (savedAt = jetzt).</summary>
    public async Task UploadAsync(CancellationToken ct = default)
    {
        // Sekunden-genaues ISO-8601-UTC — IDENTISCHES Format auf Windows UND Mac,
        // damit der lexikographische LWW-Vergleich cross-platform korrekt ist.
        var savedAt = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss'Z'");
        var bundle = BuildLocalBundle(savedAt);
        if (bundle.files.Count == 0) return; // lokal nichts vorhanden — nichts zu sichern

        var json = JsonSerializer.SerializeToUtf8Bytes(bundle);
        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, BundleFileName, ct).ConfigureAwait(false);

        using var content = new MemoryStream(json);
        if (ids.Count == 0)
        {
            var meta = new DriveFile { Name = BundleFileName, Parents = new[] { AppDataFolderSpace } };
            var create = drive.Files.Create(meta, content, "application/json");
            create.Fields = "id";
            await create.UploadAsync(ct).ConfigureAwait(false);
        }
        else
        {
            var update = drive.Files.Update(new DriveFile(), ids[0], content, "application/json");
            update.Fields = "id";
            await update.UploadAsync(ct).ConfigureAwait(false);
            for (int i = 1; i < ids.Count; i++)
            {
                try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
                catch { /* dup cleanup best-effort */ }
            }
        }
        WriteMarker(savedAt);
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
        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, BundleFileName, ct).ConfigureAwait(false);
        if (ids.Count == 0)
        {
            await UploadAsync(ct).ConfigureAwait(false); // Saat
            return;
        }

        using var buffer = new MemoryStream();
        await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false);
        Bundle? cloud;
        try { cloud = JsonSerializer.Deserialize<Bundle>(buffer.ToArray()); }
        catch { return; }
        if (cloud is null || string.IsNullOrEmpty(cloud.savedAt)) return;

        // LWW: nur ein WIRKLICH neueres Cloud-Bundle ueberschreibt lokal.
        // ISO-8601-UTC ("o") ist lexikographisch == chronologisch sortierbar.
        if (string.CompareOrdinal(cloud.savedAt, ReadMarker()) <= 0) return;

        Directory.CreateDirectory(SkDir);
        foreach (var kv in cloud.files)
        {
            var safe = Path.GetFileName(kv.Key); // kein Pfad-Trick
            if (string.IsNullOrEmpty(safe)) continue;
            try { File.WriteAllText(Path.Combine(SkDir, safe), kv.Value); }
            catch { /* skip unwritable */ }
        }
        WriteMarker(cloud.savedAt);
    }

    // ── Fire-and-forget statische Wrapper (Editor-/Schalter-Save, App-Start) ──

    /// <summary>Nach jedem Speichern eines Prompts/Schalters aufrufen.</summary>
    public static void TryUpload()
    {
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
}
