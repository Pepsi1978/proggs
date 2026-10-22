using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Ein einzelner Eintrag in der Prompt-Historie. Bewusst flach gehalten
/// damit Cross-Platform-JSON-Sync mit der macOS-Variante (PBHistoryEntry
/// in PromptHistoryStore.swift) bytegenau passt — beide Seiten
/// serialisieren die gleichen Feldnamen mit denselben Casing-Regeln.
/// </summary>
public sealed class PromptHistoryEntry
{
    public string Id { get; set; } = Guid.NewGuid().ToString();

    /// <summary>Vollstaendiger Prompt-Text wie er an die CLI gegangen ist.</summary>
    public string Text { get; set; } = string.Empty;

    /// <summary>KI-generierter Kurztitel (max 4 Woerter).</summary>
    public string Title { get; set; } = string.Empty;

    /// <summary>UTC-Zeitpunkt des Sendens — fuer plattformuebergreifende Sortierung.</summary>
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    /// <summary>UTC-Revision für konfliktfreie geräteübergreifende Bearbeitungen.</summary>
    public DateTime UpdatedAt { get; set; }

    /// <summary>Monotone Archiv-Tombstone; archivierte IDs dürfen nicht wieder aktiv werden.</summary>
    public DateTime? ArchivedAt { get; set; }
}

/// <summary>
/// Liest und schreibt die Prompt-Historie aus einer JSON-Datei. Bei mehr
/// als 100 aktiven Eintraegen wandert der aelteste in eine Markdown-
/// Archivdatei im Unterordner <c>Terminal Archiv</c> — eine MD-Datei
/// haelt maximal 500 Eintraege, danach wird eine neue Datei mit
/// fortlaufender Nummer angelegt. Innerhalb jeder MD-Datei steht der
/// aktuellste Eintrag oben, damit der Benutzer beim Oeffnen sofort die
/// neuesten archivierten Prompts sieht.
///
/// Thread- und Prozesssicherheit: Alle Lese-/Schreibpfade gehen durch denselben
/// SemaphoreSlim und einen app-uebergreifenden Named Mutex. Atomares Schreiben
/// mit Temp-Datei + Replace verhindert eine korrupte history.json bei Abstuerzen.
/// </summary>
public sealed class PromptHistoryService
{
    private const int MaxActiveEntries = 100;
    private const int MaxEntriesPerArchive = 500;
    private const string ArchiveFolderName = "Terminal Archiv";
    private const string StoreMutexName = @"Local\PromptBoard.PromptHistoryStore";

    private readonly string _baseDir;
    private readonly string _historyFilePath;
    private readonly string _archiveDir;
    private readonly SemaphoreSlim _gate = new(1, 1);
    // Created in the constructor (not as a field initializer) so a failure can be
    // handled: an elevated creator used to give a non-elevated sibling an
    // UnauthorizedAccessException in the constructor -> crash loop. Null = no lock.
    private readonly Mutex? _storeMutex;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    };

    public PromptHistoryService()
    {
        // Eine Schiene tiefer als die SQLite-DB des PromptBoards, damit die
        // beiden Subsysteme in benachbarten Ordnern liegen aber sich nichts
        // gegenseitig ueberschreiben koennen.
        _baseDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PromptBoard",
            "history");
        _historyFilePath = Path.Combine(_baseDir, "prompt-history.json");
        _archiveDir = Path.Combine(_baseDir, ArchiveFolderName);
        Directory.CreateDirectory(_baseDir);
        Directory.CreateDirectory(_archiveDir);
        _storeMutex = CrossProcessLock.CreateSharedMutex(StoreMutexName);
    }

    /// <summary>Pfad zum lokalen JSON, fuer Diagnose und Cloud-Sync (Phase 5).</summary>
    public string HistoryFilePath => _historyFilePath;

    /// <summary>Pfad zum Archiv-Ordner, fuer Cloud-Sync (Phase 5).</summary>
    public string ArchiveDirectory => _archiveDir;

    /// <summary>
    /// Liest die aktive Historie. Neuester Eintrag steht an Index 0.
    /// Eine fehlende Datei liefert eine leere Liste. I/O- und JSON-Fehler
    /// werden kontrolliert an den Aufrufer gemeldet, damit eine nachfolgende
    /// Mutation niemals versehentlich einen leeren Bestand speichert.
    /// </summary>
    public async Task<List<PromptHistoryEntry>> LoadAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            return WithStoreMutex(ct, () => LoadUnlocked(ct)
                .Where(e => e.ArchivedAt is null)
                .OrderByDescending(e => e.Timestamp)
                .Take(MaxActiveEntries)
                .ToList());
        }
        finally { _gate.Release(); }
    }

    /// <summary>Lädt aktive Einträge und Archiv-Tombstones für einen verlustfreien Cloud-Merge.</summary>
    public async Task<List<PromptHistoryEntry>> LoadAllAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            return WithStoreMutex(ct, () => LoadUnlocked(ct));
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Fuegt einen neuen Eintrag oben in der Historie hinzu, schreibt die
    /// Datei atomar zurueck und schiebt ueberzaehlige Eintraege ins MD-
    /// Archiv. Der Aufrufer ist dafuer verantwortlich, den Titel vorab zu
    /// erzeugen (z.B. via <see cref="GeminiClient.GenerateTitleAsync"/>).
    /// </summary>
    public async Task<PromptHistoryEntry> AppendAsync(
        string text, string title, CancellationToken ct = default)
    {
        var now = DateTime.UtcNow;
        var entry = new PromptHistoryEntry
        {
            Id = Guid.NewGuid().ToString(),
            Text = text ?? string.Empty,
            Title = string.IsNullOrWhiteSpace(title)
                ? GeminiClient.FallbackTitleFromText(text ?? string.Empty)
                : title.Trim(),
            Timestamp = now,
            UpdatedAt = now,
        };

        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            return WithStoreMutex(ct, () =>
            {
                var entries = LoadUnlocked(ct);
                entries.Insert(0, entry);

                ArchiveOverflowUnlocked(entries);
                SaveUnlocked(entries, ct);
                return entry;
            });
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Ersetzt die gesamte aktive Historie durch eine neue Liste — wird
    /// vom Cloud-Sync genutzt nachdem die Cloud-Eintraege mit der
    /// lokalen Liste gemergt wurden. Wendet dieselben Schwellen an wie
    /// AppendAsync: aelteste Eintraege wandern automatisch ins Archiv,
    /// sobald die aktive Liste die 100-Marke ueberschreitet.
    /// </summary>
    public async Task ReplaceAllAsync(
        IEnumerable<PromptHistoryEntry> entries, CancellationToken ct = default)
    {
        var allEntries = entries
            .Where(e => e is not null)
            .OrderByDescending(e => e.Timestamp)
            .ToList();

        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            WithStoreMutex(ct, () =>
            {
                ArchiveOverflowUnlocked(allEntries);
                SaveUnlocked(allEntries, ct);
            });
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Mergt die Cloud-JSON ATOMAR in den lokalen Stand (innerhalb von Gate +
    /// Mutex — ein gleichzeitiges AppendAsync geht dadurch nicht verloren).
    /// Gleiche Archiv-Schwellen wie <see cref="ReplaceAllAsync"/>. Liefert den
    /// gespeicherten Stand und ob sich lokal etwas geaendert hat.
    /// </summary>
    public async Task<(List<PromptHistoryEntry> Merged, bool Changed)> MergeFromCloudAsync(
        string cloudJson, CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            return WithStoreMutex(ct, () =>
            {
                var local = LoadUnlocked(ct);
                // Eigene Vergleichskopie: MergeEntries setzt ArchivedAt direkt am
                // lokalen Objekt — ein Vergleich gegen `local` saehe die Aenderung nie.
                var before = LoadUnlocked(ct);
                var merged = PromptHistoryDriveSync.MergeEntries(local, cloudJson);
                if (PromptHistoryDriveSync.SameContent(merged, before)) return (before, false);
                ArchiveOverflowUnlocked(merged);
                SaveUnlocked(merged, ct);
                return (merged, true);
            });
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Aktualisiert den Titel eines bestehenden Eintrags und schreibt die
    /// Historie atomar zurueck. Wird vom OverlayWindow aufgerufen sobald
    /// der KI-Titel von Gemini eingetroffen ist — der Eintrag wurde
    /// vorher schon mit einem Fallback-Titel angelegt, damit der Benutzer
    /// ihn sofort in der Historie sieht.
    /// </summary>
    public async Task UpdateTitleAsync(string entryId, string newTitle, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(entryId)) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            WithStoreMutex(ct, () =>
            {
                var entries = LoadUnlocked(ct);
                var match = entries.FirstOrDefault(e => e.Id == entryId);
                if (match is null) return;
                match.Title = string.IsNullOrWhiteSpace(newTitle)
                    ? GeminiClient.FallbackTitleFromText(match.Text)
                    : newTitle.Trim();
                match.UpdatedAt = DateTime.UtcNow;
                SaveUnlocked(entries, ct);
            });
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Aktualisiert den Volltext eines bestehenden Eintrags und schreibt
    /// die Historie atomar zurueck. Wird vom PromptBoardPanel aufgerufen
    /// nachdem der Benutzer einen Eintrag im Editor-Dialog veraendert
    /// und mit Speichern bestaetigt hat. Titel und Zeitstempel bleiben
    /// unveraendert — Titel ist KI-generiert, Zeitstempel ist der
    /// urspruengliche Sortier-Schluessel.
    /// </summary>
    public async Task UpdateTextAsync(string entryId, string newText, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(entryId)) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            WithStoreMutex(ct, () =>
            {
                var entries = LoadUnlocked(ct);
                var match = entries.FirstOrDefault(e => e.Id == entryId);
                if (match is null) return;
                match.Text = newText ?? string.Empty;
                match.UpdatedAt = DateTime.UtcNow;
                SaveUnlocked(entries, ct);
            });
        }
        finally { _gate.Release(); }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Interne Helpers (laufen alle innerhalb des Semaphors und Named Mutex)
    // ─────────────────────────────────────────────────────────────────────

    private List<PromptHistoryEntry> LoadUnlocked(CancellationToken ct)
    {
        if (!File.Exists(_historyFilePath))
        {
            return new List<PromptHistoryEntry>();
        }
        try
        {
            ct.ThrowIfCancellationRequested();
            using var stream = new FileStream(_historyFilePath, FileMode.Open, FileAccess.Read,
                FileShare.ReadWrite | FileShare.Delete);
            var list = JsonSerializer.Deserialize<List<PromptHistoryEntry>>(stream, JsonOptions);
            return list ?? throw new JsonException("PromptHistory JSON contained null.");
        }
        catch (Exception ex) when (ex is IOException or JsonException or UnauthorizedAccessException)
        {
            Console.WriteLine($"PromptHistory load failed: {ex.Message}");
            throw;
        }
    }

    private void SaveUnlocked(List<PromptHistoryEntry> entries, CancellationToken ct)
    {
        string tmp = $"{_historyFilePath}.{Guid.NewGuid():N}.tmp";
        try
        {
            ct.ThrowIfCancellationRequested();
            using (var stream = new FileStream(tmp, FileMode.CreateNew, FileAccess.Write, FileShare.None))
            {
                JsonSerializer.Serialize(stream, entries, JsonOptions);
                stream.Flush(flushToDisk: true);
            }
            ct.ThrowIfCancellationRequested();

            // Short retry: the Drive upload path or the sibling overlay may hold the
            // file open for a moment (File.Replace then fails with a sharing violation).
            AtomicFile.ReplaceWithRetry(tmp, _historyFilePath);
        }
        finally
        {
            TryDeleteTemporaryFile(tmp);
        }
    }

    private T WithStoreMutex<T>(CancellationToken ct, Func<T> action)
    {
        bool ownsMutex = false;
        try
        {
            if (_storeMutex is null)
            {
                // Mutex unavailable (see constructor): run without the cross-process
                // lock rather than crash; the SemaphoreSlim still serializes this process.
                ct.ThrowIfCancellationRequested();
                return action();
            }
            try
            {
                if (ct.CanBeCanceled)
                {
                    int signaled = WaitHandle.WaitAny(new WaitHandle[] { _storeMutex, ct.WaitHandle });
                    if (signaled == 1) ct.ThrowIfCancellationRequested();
                }
                else
                {
                    _storeMutex.WaitOne();
                }
            }
            catch (AbandonedMutexException ex)
            {
                Console.WriteLine($"PromptHistory recovered abandoned store mutex: {ex.Message}");
            }

            ownsMutex = true;
            ct.ThrowIfCancellationRequested();
            return action();
        }
        finally
        {
            if (ownsMutex) _storeMutex?.ReleaseMutex();
        }
    }

    private void WithStoreMutex(CancellationToken ct, Action action)
    {
        WithStoreMutex<object?>(ct, () =>
        {
            action();
            return null;
        });
    }

    private static void TryDeleteTemporaryFile(string path)
    {
        try
        {
            File.Delete(path);
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
            Console.WriteLine($"PromptHistory temp cleanup failed: {ex.Message}");
        }
    }

    /// <summary>
    /// Archive contents read ONCE per overflow pass (before: every archived entry re-read
    /// every archive-*.md). Holds the ids already archived plus the raw contents for the
    /// legacy (pre-id-marker) duplicate check; kept current while entries are appended.
    /// </summary>
    private sealed class ArchiveIndex
    {
        private const string IdPrefix = "<!-- prompt-history-id: ";
        public HashSet<string> Ids { get; } = new(StringComparer.OrdinalIgnoreCase);
        public Dictionary<string, string> Contents { get; } = new(StringComparer.OrdinalIgnoreCase);

        public static ArchiveIndex Build(IEnumerable<string> files)
        {
            var index = new ArchiveIndex();
            foreach (var path in files) index.Set(path, File.ReadAllText(path, Encoding.UTF8));
            return index;
        }

        public void Set(string path, string content)
        {
            Contents[path] = content;
            int at = 0;
            while ((at = content.IndexOf(IdPrefix, at, StringComparison.Ordinal)) >= 0)
            {
                at += IdPrefix.Length;
                int end = content.IndexOf(" -->", at, StringComparison.Ordinal);
                if (end < 0) break;
                Ids.Add(content[at..end].Trim());
                at = end;
            }
        }

        public bool ContainsLegacy(string block)
        {
            foreach (var c in Contents.Values)
                if (c.Contains(block, StringComparison.Ordinal)) return true;
            return false;
        }
    }

    private void AppendToArchiveUnlocked(PromptHistoryEntry entry, ArchiveIndex index)
    {
        // Aktuellste Archiv-Datei bestimmen: hoechste Nummer ODER neue
        // anlegen wenn die letzte voll ist (>= 500 Eintraege).
        Directory.CreateDirectory(_archiveDir);
        var existing = index.Contents.Keys
            .OrderBy(p => p, StringComparer.OrdinalIgnoreCase)
            .ToList();

        if (index.Ids.Contains(entry.Id) ||
            index.ContainsLegacy(FormatEntryAsMarkdown(entry, includeId: false)))
        {
            return;
        }

        string targetFile;
        if (existing.Count == 0)
        {
            targetFile = Path.Combine(_archiveDir, "archive-001.md");
        }
        else
        {
            string latest = existing[^1];
            int currentCount = CountEntries(index.Contents[latest]);
            if (currentCount >= MaxEntriesPerArchive)
            {
                int nextNum = ParseArchiveNumber(latest) + 1;
                targetFile = Path.Combine(_archiveDir, $"archive-{nextNum:D3}.md");
            }
            else
            {
                targetFile = latest;
            }
        }

        // Neueste oben in der Datei: vorhandenen Inhalt lesen, neuen Block
        // davor einfuegen, alles zurueckschreiben. Bei einer 500-Eintraege-
        // Datei sind das im Worst Case ein paar hundert KB — vernachlaessigbar.
        string newBlock = FormatEntryAsMarkdown(entry, includeId: true);
        string oldContent = index.Contents.TryGetValue(targetFile, out var cached)
            ? cached
            : File.Exists(targetFile) ? File.ReadAllText(targetFile, Encoding.UTF8) : string.Empty;
        string tmp = $"{targetFile}.{Guid.NewGuid():N}.tmp";
        try
        {
            using (var stream = new FileStream(tmp, FileMode.CreateNew, FileAccess.Write, FileShare.None))
            using (var writer = new StreamWriter(stream, Encoding.UTF8))
            {
                writer.Write(newBlock);
                writer.Write(oldContent);
                writer.Flush();
                stream.Flush(flushToDisk: true);
            }

            AtomicFile.ReplaceWithRetry(tmp, targetFile);
            index.Set(targetFile, newBlock + oldContent);
        }
        finally
        {
            TryDeleteTemporaryFile(tmp);
        }
    }

    private static int CountEntries(string content)
    {
        // Jeder Eintrag beginnt mit "## " an Zeilenanfang.
        int count = 0;
        foreach (var line in content.Split('\n'))
        {
            if (line.StartsWith("## ", StringComparison.Ordinal)) count++;
        }
        return count;
    }

    private static int ParseArchiveNumber(string path)
    {
        string name = Path.GetFileNameWithoutExtension(path); // archive-NNN
        int dash = name.LastIndexOf('-');
        if (dash < 0 || dash + 1 >= name.Length) return 0;
        return int.TryParse(name[(dash + 1)..], out int n) ? n : 0;
    }

    private void ArchiveOverflowUnlocked(List<PromptHistoryEntry> entries)
    {
        var overflow = entries
            .Where(e => e.ArchivedAt is null)
            .OrderByDescending(e => e.Timestamp)
            .Skip(MaxActiveEntries)
            .ToList();

        if (overflow.Count > 0)
        {
            Directory.CreateDirectory(_archiveDir);
            var index = ArchiveIndex.Build(Directory.GetFiles(_archiveDir, "archive-*.md"));
            foreach (var entry in overflow)
            {
                var archivedAt = DateTime.UtcNow;
                entry.ArchivedAt = archivedAt;
                if (entry.UpdatedAt < archivedAt) entry.UpdatedAt = archivedAt;
                AppendToArchiveUnlocked(entry, index);
            }
        }

        entries.Sort((a, b) => b.Timestamp.CompareTo(a.Timestamp));
    }

    private static string FormatEntryAsMarkdown(PromptHistoryEntry e, bool includeId)
    {
        var local = e.Timestamp.ToLocalTime();
        string when = local.ToString("yyyy-MM-dd HH:mm");
        string title = string.IsNullOrWhiteSpace(e.Title) ? "Ohne Titel" : e.Title;
        var sb = new StringBuilder();
        sb.Append("## ").Append(when).Append(" — ").Append(title).Append('\n');
        if (includeId)
            sb.Append("<!-- prompt-history-id: ").Append(e.Id).Append(" -->").Append('\n');
        sb.Append('\n');
        sb.Append(e.Text ?? string.Empty).Append('\n');
        sb.Append('\n');
        sb.Append("---").Append('\n');
        sb.Append('\n');
        return sb.ToString();
    }
}
