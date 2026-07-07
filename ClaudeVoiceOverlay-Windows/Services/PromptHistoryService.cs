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
/// Thread-sicherheit: Alle Lese-/Schreibpfade gehen durch denselben
/// SemaphoreSlim. Atomares Schreiben mit Temp-Datei + Replace, damit ein
/// Crash mitten im Schreiben niemals eine korrupte history.json hinterlaesst.
/// </summary>
public sealed class PromptHistoryService
{
    private const int MaxActiveEntries = 100;
    private const int MaxEntriesPerArchive = 500;
    private const string ArchiveFolderName = "Terminal Archiv";

    private readonly string _baseDir;
    private readonly string _historyFilePath;
    private readonly string _archiveDir;
    private readonly SemaphoreSlim _gate = new(1, 1);

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
    }

    /// <summary>Pfad zum lokalen JSON, fuer Diagnose und Cloud-Sync (Phase 5).</summary>
    public string HistoryFilePath => _historyFilePath;

    /// <summary>Pfad zum Archiv-Ordner, fuer Cloud-Sync (Phase 5).</summary>
    public string ArchiveDirectory => _archiveDir;

    /// <summary>
    /// Liest die aktive Historie. Neuester Eintrag steht an Index 0.
    /// Ein fehlendes oder kaputtes JSON liefert eine leere Liste — die
    /// Historie ist Add-Only-Wissen, korrupte Eintraege werden nicht in
    /// das laufende Programm geschleppt.
    /// </summary>
    public async Task<List<PromptHistoryEntry>> LoadAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            return await LoadUnlockedAsync(ct).ConfigureAwait(false);
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
        var entry = new PromptHistoryEntry
        {
            Id = Guid.NewGuid().ToString(),
            Text = text ?? string.Empty,
            Title = string.IsNullOrWhiteSpace(title)
                ? GeminiClient.FallbackTitleFromText(text ?? string.Empty)
                : title.Trim(),
            Timestamp = DateTime.UtcNow,
        };

        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            entries.Insert(0, entry);

            // Wenn die aktive Liste die Schwelle ueberschreitet, wandert
            // der jeweils aelteste Eintrag ins Archiv. Schleife fuer den
            // unwahrscheinlichen Fall, dass mehrere Eintraege auf einmal
            // aufgelaufen sind (z.B. nach einem Crash-Recovery).
            while (entries.Count > MaxActiveEntries)
            {
                var oldest = entries[^1];
                entries.RemoveAt(entries.Count - 1);
                AppendToArchiveUnlocked(oldest);
            }

            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
            return entry;
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
        var sorted = entries
            .Where(e => e is not null)
            .OrderByDescending(e => e.Timestamp)
            .ToList();

        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            while (sorted.Count > MaxActiveEntries)
            {
                var oldest = sorted[^1];
                sorted.RemoveAt(sorted.Count - 1);
                AppendToArchiveUnlocked(oldest);
            }
            await SaveUnlockedAsync(sorted, ct).ConfigureAwait(false);
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
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var match = entries.FirstOrDefault(e => e.Id == entryId);
            if (match is null) return;
            match.Title = string.IsNullOrWhiteSpace(newTitle)
                ? GeminiClient.FallbackTitleFromText(match.Text)
                : newTitle.Trim();
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
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
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var match = entries.FirstOrDefault(e => e.Id == entryId);
            if (match is null) return;
            match.Text = newText ?? string.Empty;
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Interne Helpers (laufen alle innerhalb des Semaphors)
    // ─────────────────────────────────────────────────────────────────────

    private async Task<List<PromptHistoryEntry>> LoadUnlockedAsync(CancellationToken ct)
    {
        if (!File.Exists(_historyFilePath))
        {
            return new List<PromptHistoryEntry>();
        }
        try
        {
            await using var stream = File.OpenRead(_historyFilePath);
            var list = await JsonSerializer.DeserializeAsync<List<PromptHistoryEntry>>(
                stream, JsonOptions, ct).ConfigureAwait(false);
            return list ?? new List<PromptHistoryEntry>();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptHistory load failed: {ex.Message}");
            return new List<PromptHistoryEntry>();
        }
    }

    private async Task SaveUnlockedAsync(
        List<PromptHistoryEntry> entries, CancellationToken ct)
    {
        // Atomares Schreiben: Erst in eine Temp-Datei im selben Ordner,
        // dann mit File.Move(overwrite=true) ueber das Original. Wenn das
        // Programm mitten im Schreiben crasht, bleibt entweder das alte
        // oder das neue JSON bestehen — niemals etwas Halbgares dazwischen.
        string tmp = _historyFilePath + ".tmp";
        await using (var stream = File.Create(tmp))
        {
            await JsonSerializer.SerializeAsync(stream, entries, JsonOptions, ct)
                .ConfigureAwait(false);
        }
        File.Move(tmp, _historyFilePath, overwrite: true);
    }

    private void AppendToArchiveUnlocked(PromptHistoryEntry entry)
    {
        // Aktuellste Archiv-Datei bestimmen: hoechste Nummer ODER neue
        // anlegen wenn die letzte voll ist (>= 500 Eintraege).
        Directory.CreateDirectory(_archiveDir);
        var existing = Directory.GetFiles(_archiveDir, "archive-*.md")
            .OrderBy(p => p, StringComparer.OrdinalIgnoreCase)
            .ToList();

        string targetFile;
        if (existing.Count == 0)
        {
            targetFile = Path.Combine(_archiveDir, "archive-001.md");
        }
        else
        {
            string latest = existing[^1];
            int currentCount = CountEntriesInArchive(latest);
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
        string newBlock = FormatEntryAsMarkdown(entry);
        string oldContent = File.Exists(targetFile)
            ? File.ReadAllText(targetFile, Encoding.UTF8)
            : string.Empty;
        File.WriteAllText(targetFile, newBlock + oldContent, Encoding.UTF8);
    }

    private static int CountEntriesInArchive(string path)
    {
        if (!File.Exists(path)) return 0;
        // Jeder Eintrag beginnt mit "## " an Zeilenanfang.
        int count = 0;
        foreach (var line in File.ReadLines(path, Encoding.UTF8))
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

    private static string FormatEntryAsMarkdown(PromptHistoryEntry e)
    {
        var local = e.Timestamp.ToLocalTime();
        string when = local.ToString("yyyy-MM-dd HH:mm");
        string title = string.IsNullOrWhiteSpace(e.Title) ? "Ohne Titel" : e.Title;
        var sb = new StringBuilder();
        sb.Append("## ").Append(when).Append(" — ").Append(title).Append('\n');
        sb.Append('\n');
        sb.Append(e.Text ?? string.Empty).Append('\n');
        sb.Append('\n');
        sb.Append("---").Append('\n');
        sb.Append('\n');
        return sb.ToString();
    }
}
