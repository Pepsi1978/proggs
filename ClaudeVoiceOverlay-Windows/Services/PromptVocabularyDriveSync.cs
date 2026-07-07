using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
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
/// Synchronisiert das persoenliche Vokabular-Woerterbuch
/// (<c>personal-vocabulary.txt</c> im SK-Ordner) ueber Google Drive zwischen
/// allen Geraeten und beiden Overlays. Eigene Datei im <c>appDataFolder</c>,
/// gleiche OAuth-Mechanik wie <see cref="PromptSlotDriveSync"/>.
///
/// WICHTIG (Frank-Regel gegen destruktiven Sync, siehe
/// feedback_strava_sync_destructive_replace / feedback_no_panic_restore_in_parallel_sessions):
/// Der Merge ist eine reine VEREINIGUNG der Zeilen — ein einmal eingespeichertes
/// Wort geht NIE verloren, egal von welchem Geraet. Geloeschte Woerter koennen
/// daher von einem anderen Geraet zurueckkehren; das ist der bewusste Preis fuer
/// Verlustfreiheit (Frank-Wunsch: "diese Woerter immer mitgespeichert").
/// </summary>
public sealed class PromptVocabularyDriveSync
{
    public const string VocabularyFileName = "personal-vocabulary.txt";
    private const string AppDataFolderSpace = "appDataFolder";

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
    /// Lokaler Pfad der Vokabular-Datei — gleicher SK-Ordner wie die Gemini-
    /// Korrektur-Prompts, sodass sich auf EINEM Rechner TVO und CVO dieselbe
    /// Datei teilen.
    /// </summary>
    public static string LocalPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "SK", "VoiceOverlays", VocabularyFileName);

    /// <summary>
    /// Laedt die lokale Vokabular-Datei SOFORT zu Drive hoch und raeumt
    /// Duplikate weg. Wird nach jedem Speichern im Settings-Dialog aufgerufen.
    /// </summary>
    public async Task UploadAsync(CancellationToken ct = default)
    {
        var localPath = LocalPath;
        if (!File.Exists(localPath)) return;
        var bytes = await File.ReadAllBytesAsync(localPath, ct).ConfigureAwait(false);

        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, VocabularyFileName, ct).ConfigureAwait(false);

        using var content = new MemoryStream(bytes);
        if (ids.Count == 0)
        {
            var meta = new DriveFile
            {
                Name = VocabularyFileName,
                Parents = new[] { AppDataFolderSpace },
            };
            var create = drive.Files.Create(meta, content, "text/plain");
            create.Fields = "id";
            await create.UploadAsync(ct).ConfigureAwait(false);
        }
        else
        {
            var keep = ids[0];
            var update = drive.Files.Update(new DriveFile(), keep, content, "text/plain");
            update.Fields = "id";
            await update.UploadAsync(ct).ConfigureAwait(false);

            for (int i = 1; i < ids.Count; i++)
            {
                try { await drive.Files.Delete(ids[i]).ExecuteAsync(ct).ConfigureAwait(false); }
                catch (Exception ex)
                {
                    _logger?.LogWarning(ex, "Vocab dup-cleanup failed for {Id}", ids[i]);
                }
            }
        }
    }

    /// <summary>Holt die aktuellste Vokabular-Datei aus Drive. Null wenn keine vorhanden ist.</summary>
    public async Task<string?> DownloadAsync(CancellationToken ct = default)
    {
        var drive = await BuildDriveAsync(ct).ConfigureAwait(false);
        var ids = await FindAllAsync(drive, VocabularyFileName, ct).ConfigureAwait(false);
        if (ids.Count == 0) return null;

        using var buffer = new MemoryStream();
        await drive.Files.Get(ids[0]).DownloadAsync(buffer, ct).ConfigureAwait(false);
        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    /// <summary>
    /// Reine VEREINIGUNG zweier Vokabular-Texte: alle nicht-leeren Zeilen aus
    /// lokal UND cloud, dedupliziert (case-insensitiv, getrimmt), lokale
    /// Reihenfolge zuerst. Verlustfrei — kein Wort wird je entfernt.
    /// </summary>
    public static string MergeVocabularies(string? localText, string? cloudText)
    {
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var outLines = new List<string>();

        void Add(string? text)
        {
            if (string.IsNullOrEmpty(text)) return;
            foreach (var raw in text.Replace("\r\n", "\n").Split('\n'))
            {
                var line = raw.Trim();
                if (line.Length == 0) continue;
                if (seen.Add(line.ToLowerInvariant())) outLines.Add(line);
            }
        }

        Add(localText);
        Add(cloudText);
        return string.Join("\n", outLines);
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
