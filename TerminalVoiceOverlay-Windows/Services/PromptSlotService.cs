using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Ein einzelner Prompt-Zwischenspeicher-Slot (1…30). <c>Text == ""</c> ist ein
/// Tombstone: der Slot wurde geloescht, der Zeitstempel bleibt erhalten, damit
/// das Loeschen beim Cloud-Merge geraeteuebergreifend gewinnt. Feldnamen sind
/// bewusst flach + camelCase und damit bytegenau zur macOS-Variante
/// (PBSlotEntry in PromptSlotStore.swift) kompatibel.
/// </summary>
public sealed class PromptSlotEntry
{
    public int Number { get; set; }
    public string Text { get; set; } = string.Empty;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>
    /// KI-generierte Kurz-Zusammenfassung (6-8 Woerter) WOFUER dieser Prompt
    /// da ist — wird als Hover-Tooltip ueber dem belegten Slot angezeigt.
    /// Leer bei Tombstones und bei Eintraegen, die vor diesem Feature
    /// gespeichert wurden (Fallback: Standard-Tooltip). Reist im JSON
    /// (camelCase <c>summary</c>) mit ins Drive-Backup und ist bytegenau
    /// kompatibel zur macOS-Variante (PBSlotEntry.summary).
    /// </summary>
    public string Summary { get; set; } = string.Empty;

    /// <summary>
    /// Prioritaet dieses Slots fuer die farbige Einfaerbung der Zahlen-Leiste:
    /// 0 = keine (Standard, auch fuer alte Eintraege ohne dieses Feld),
    /// 1 = niedrig (gruen), 2 = mittel (gelb), 3 = hoch (rot). Reist im JSON
    /// (camelCase <c>priority</c>) mit ins Drive-Backup und ist bytegenau
    /// kompatibel zur macOS-Variante (PBSlotEntry.priority). Bei Tombstones
    /// (leerer Text) bedeutungslos.
    /// </summary>
    public int Priority { get; set; }
}

/// <summary>
/// Liest und schreibt die 30 Prompt-Zwischenspeicher-Slots aus einer JSON-Datei.
/// Voellig analog zum <see cref="PromptHistoryService"/>: SemaphoreSlim-Gate,
/// atomares Schreiben (Temp-Datei + File.Move overwrite), camelCase-JSON
/// kompatibel zur macOS-Seite. Die Datei wird nach jedem Speichern/Loeschen
/// sofort zu Google Drive gespiegelt (<c>prompt-slots.json</c> im appDataFolder).
/// </summary>
public sealed class PromptSlotService
{
    // 30 Slots, in der UI als zwei Reihen dargestellt (1…15 oben, 16…30 unten).
    // Zentrale Quelle fuer UI-Aufbau, Validierung und Cloud-Merge — bytegenau
    // synchron zur macOS-Variante (PromptSlotStore.slotCount).
    public const int SlotCount = 30;

    private readonly string _baseDir;
    private readonly string _slotsFilePath;
    private readonly SemaphoreSlim _gate = new(1, 1);

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        PropertyNameCaseInsensitive = true,
    };

    public PromptSlotService()
    {
        _baseDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PromptBoard",
            "slots");
        _slotsFilePath = Path.Combine(_baseDir, "prompt-slots.json");
        Directory.CreateDirectory(_baseDir);
    }

    /// <summary>Pfad zur lokalen JSON-Datei (Diagnose / Cloud-Sync).</summary>
    public string SlotsFilePath => _slotsFilePath;

    /// <summary>
    /// Liefert die belegten Slots als Dictionary <c>Nummer → Text</c>
    /// (Tombstones mit leerem Text werden ausgefiltert). Quelle fuer die
    /// Einfaerbung der Zahlen-Leiste und das Laden beim Klick.
    /// </summary>
    public async Task<Dictionary<int, string>> LoadMapAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var map = new Dictionary<int, string>();
            foreach (var e in entries)
            {
                if (!string.IsNullOrEmpty(e.Text) && e.Number is >= 1 and <= SlotCount)
                    map[e.Number] = e.Text;
            }
            return map;
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Wie <see cref="LoadMapAsync"/>, liefert aber zusaetzlich die Speicher-
    /// Zeitstempel pro belegtem Slot — fuer die Anzeige „wann gespeichert".
    /// </summary>
    public async Task<(Dictionary<int, string> Map, Dictionary<int, DateTime> Times)>
        LoadMapAndTimesAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var map = new Dictionary<int, string>();
            var times = new Dictionary<int, DateTime>();
            foreach (var e in entries)
            {
                if (!string.IsNullOrEmpty(e.Text) && e.Number is >= 1 and <= SlotCount)
                {
                    map[e.Number] = e.Text;
                    times[e.Number] = e.UpdatedAt;
                }
            }
            return (map, times);
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Wie <see cref="LoadMapAndTimesAsync"/>, liefert zusaetzlich die
    /// KI-Zusammenfassungen pro belegtem Slot — fuer die Hover-Tooltips der
    /// Zahlen-Leiste. Nur Slots mit nicht-leerer Summary erscheinen im
    /// Summary-Dictionary.
    /// </summary>
    public async Task<(Dictionary<int, string> Map, Dictionary<int, DateTime> Times, Dictionary<int, string> Summaries, Dictionary<int, int> Priorities)>
        LoadMapTimesSummariesAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var map = new Dictionary<int, string>();
            var times = new Dictionary<int, DateTime>();
            var summaries = new Dictionary<int, string>();
            var priorities = new Dictionary<int, int>();
            foreach (var e in entries)
            {
                if (!string.IsNullOrEmpty(e.Text) && e.Number is >= 1 and <= SlotCount)
                {
                    map[e.Number] = e.Text;
                    times[e.Number] = e.UpdatedAt;
                    if (!string.IsNullOrWhiteSpace(e.Summary)) summaries[e.Number] = e.Summary;
                    if (e.Priority != 0) priorities[e.Number] = e.Priority;
                }
            }
            return (map, times, summaries, priorities);
        }
        finally { _gate.Release(); }
    }

    /// <summary>Liefert ALLE Eintraege roh (inkl. Tombstones) — fuer den Cloud-Merge.</summary>
    public async Task<List<PromptSlotEntry>> LoadEntriesAsync(CancellationToken ct = default)
    {
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try { return await LoadUnlockedAsync(ct).ConfigureAwait(false); }
        finally { _gate.Release(); }
    }

    /// <summary>Speichert (oder ueberschreibt) den Text in einem Slot mit neuem Zeitstempel.</summary>
    public async Task SaveAsync(int number, string text, CancellationToken ct = default)
    {
        if (number is < 1 or > SlotCount) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            int idx = entries.FindIndex(e => e.Number == number);
            // Neuer Text -> alte Summary ist ungueltig und wird geleert. Die
            // frische 6-8-Wort-Zusammenfassung holt der Aufrufer gleich danach
            // per Gemini und schreibt sie ueber SetSummaryAsync nach. Die
            // Prioritaet (farbige Einfaerbung) gehoert zum SLOT, nicht zum Text,
            // und bleibt beim Ueberschreiben erhalten (Frank-Wunsch 2026-06-16).
            int priority = idx >= 0 ? entries[idx].Priority : 0;
            var entry = new PromptSlotEntry { Number = number, Text = text ?? string.Empty, UpdatedAt = DateTime.UtcNow, Summary = string.Empty, Priority = priority };
            if (idx >= 0) entries[idx] = entry; else entries.Add(entry);
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Aktualisiert NUR die KI-Zusammenfassung eines belegten Slots — aber nur,
    /// wenn dessen gespeicherter Text noch exakt <paramref name="forText"/>
    /// entspricht. So landet eine Summary, die fuer einen alten Text generiert
    /// wurde, nie auf einem inzwischen geaenderten Slot (der Gemini-Call ist
    /// asynchron und kann den schnelleren Re-Save ueberholen). Bumpt
    /// <c>UpdatedAt</c>, damit die Summary per Cloud-Merge auf andere Geraete
    /// wandert. No-op bei leerem/geaendertem Slot.
    /// </summary>
    public async Task SetSummaryAsync(int number, string forText, string summary, CancellationToken ct = default)
    {
        if (number is < 1 or > SlotCount) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            int idx = entries.FindIndex(e => e.Number == number);
            if (idx < 0) return;
            var e = entries[idx];
            if (string.IsNullOrEmpty(e.Text)) return;                    // Tombstone
            if (!string.Equals(e.Text, forText, StringComparison.Ordinal)) return; // Text inzwischen geaendert
            e.Summary = summary ?? string.Empty;
            e.UpdatedAt = DateTime.UtcNow;
            entries[idx] = e;
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Setzt die Prioritaet eines belegten Slots (0 = keine, 1 = niedrig,
    /// 2 = mittel, 3 = hoch) — Quelle fuer die farbige Einfaerbung der
    /// Zahlen-Leiste. Bumpt <c>UpdatedAt</c>, damit die Aenderung per Cloud-Merge
    /// (Last-Write-Wins) auf andere Geraete wandert UND ins Google-Drive-Backup
    /// (prompt-slots*.json) kommt. No-op bei leerem/Tombstone-Slot (eine
    /// Prioritaet ohne Prompt ergibt keinen Sinn).
    /// </summary>
    public async Task SetPriorityAsync(int number, int priority, CancellationToken ct = default)
    {
        if (number is < 1 or > SlotCount) return;
        if (priority is < 0 or > 3) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            int idx = entries.FindIndex(e => e.Number == number);
            if (idx < 0) return;
            var e = entries[idx];
            if (string.IsNullOrEmpty(e.Text)) return;   // Tombstone -> keine Prioritaet
            e.Priority = priority;
            e.UpdatedAt = DateTime.UtcNow;
            entries[idx] = e;
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Loescht den Slot dauerhaft. Hinterlaesst einen Tombstone (leerer Text +
    /// neuer Zeitstempel), damit das Loeschen beim Cloud-Merge gewinnt.
    /// </summary>
    public async Task DeleteAsync(int number, CancellationToken ct = default)
    {
        if (number is < 1 or > SlotCount) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);
            var tombstone = new PromptSlotEntry { Number = number, Text = string.Empty, UpdatedAt = DateTime.UtcNow };
            int idx = entries.FindIndex(e => e.Number == number);
            if (idx >= 0) entries[idx] = tombstone; else entries.Add(tombstone);
            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    /// <summary>
    /// Verschiebt den Prompt von Slot <paramref name="from"/> nach Slot
    /// <paramref name="to"/> (Drag&amp;Drop in der Zahlen-Leiste, Frank-Wunsch 2026-06-08).
    /// <para>
    /// Ist <paramref name="to"/> LEER → reines Verschieben: <paramref name="from"/>
    /// wird zum Tombstone (leerer Text), damit das Leeren beim Cloud-Merge
    /// geraeteuebergreifend gewinnt — genau wie bei <see cref="DeleteAsync"/>.
    /// </para>
    /// <para>
    /// Ist <paramref name="to"/> BELEGT → Tauschen: <paramref name="from"/> erhaelt
    /// den bisherigen Text von <paramref name="to"/>. Kein Prompt geht verloren.
    /// </para>
    /// Beide Slots bekommen einen frischen Zeitstempel, damit die Aenderung beim
    /// Last-Write-Wins-Merge auf anderen PCs uebernommen wird. Alles atomar im
    /// selben Semaphor-Gate, damit kein halber Zustand persistiert/gesynct wird.
    /// </summary>
    public async Task MoveAsync(int from, int to, CancellationToken ct = default)
    {
        if (from == to) return;
        if (from is < 1 or > SlotCount || to is < 1 or > SlotCount) return;
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            var entries = await LoadUnlockedAsync(ct).ConfigureAwait(false);

            string TextOf(int n)
            {
                var e = entries.FirstOrDefault(x => x.Number == n);
                return e?.Text ?? string.Empty;
            }
            string SummaryOf(int n)
            {
                var e = entries.FirstOrDefault(x => x.Number == n);
                return e?.Summary ?? string.Empty;
            }
            int PriorityOf(int n)
            {
                var e = entries.FirstOrDefault(x => x.Number == n);
                return e?.Priority ?? 0;
            }

            string fromText = TextOf(from);
            // Nichts zu verschieben, wenn die Quelle leer ist (Tombstone/unbelegt).
            if (string.IsNullOrEmpty(fromText)) return;
            string toText = TextOf(to); // leer => Move, belegt => Swap

            // Summaries reisen mit dem Text mit, damit der Hover-Tooltip nach
            // Verschieben/Tauschen weiter zum richtigen Prompt passt.
            string fromSummary = SummaryOf(from);
            string toSummary = SummaryOf(to);

            // Prioritaet reist mit dem Prompt mit (wie die Summary), damit die
            // farbige Einfaerbung nach Verschieben/Tauschen am richtigen Slot bleibt.
            int fromPriority = PriorityOf(from);
            int toPriority = PriorityOf(to);

            var now = DateTime.UtcNow;

            void Upsert(int number, string text, string summary, int priority)
            {
                var entry = new PromptSlotEntry { Number = number, Text = text ?? string.Empty, UpdatedAt = now, Summary = summary ?? string.Empty, Priority = priority };
                int idx = entries.FindIndex(x => x.Number == number);
                if (idx >= 0) entries[idx] = entry; else entries.Add(entry);
            }

            Upsert(to, fromText, fromSummary, fromPriority);   // Ziel bekommt den gezogenen Prompt + Summary + Prioritaet
            Upsert(from, toText, toSummary, toPriority);       // Quelle bekommt den alten Ziel-Text + Summary + Prioritaet (leer = Tombstone = Move)

            await SaveUnlockedAsync(entries, ct).ConfigureAwait(false);
            Console.WriteLine(
                $"PromptSlot move {from}->{to} ({(string.IsNullOrEmpty(toText) ? "move" : "swap")})");
        }
        finally { _gate.Release(); }
    }

    /// <summary>Ersetzt den gesamten lokalen Stand durch eine gemergte Liste (Cloud-Sync).</summary>
    public async Task ReplaceAllAsync(IEnumerable<PromptSlotEntry> entries, CancellationToken ct = default)
    {
        var list = entries.Where(e => e is not null).ToList();
        await _gate.WaitAsync(ct).ConfigureAwait(false);
        try { await SaveUnlockedAsync(list, ct).ConfigureAwait(false); }
        finally { _gate.Release(); }
    }

    // ── Interne Helpers (laufen alle innerhalb des Semaphors) ──

    private async Task<List<PromptSlotEntry>> LoadUnlockedAsync(CancellationToken ct)
    {
        if (!File.Exists(_slotsFilePath)) return new List<PromptSlotEntry>();
        try
        {
            await using var stream = File.OpenRead(_slotsFilePath);
            var list = await JsonSerializer.DeserializeAsync<List<PromptSlotEntry>>(
                stream, JsonOptions, ct).ConfigureAwait(false);
            return list ?? new List<PromptSlotEntry>();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptSlot load failed: {ex.Message}");
            return new List<PromptSlotEntry>();
        }
    }

    private async Task SaveUnlockedAsync(List<PromptSlotEntry> entries, CancellationToken ct)
    {
        // Stabile Reihenfolge nach Slot-Nummer fuer deterministisches JSON
        // (kleinere Drive-Diffs, leichteres Debuggen).
        var sorted = entries.OrderBy(e => e.Number).ToList();
        string tmp = _slotsFilePath + ".tmp";
        await using (var stream = File.Create(tmp))
        {
            await JsonSerializer.SerializeAsync(stream, sorted, JsonOptions, ct).ConfigureAwait(false);
        }
        File.Move(tmp, _slotsFilePath, overwrite: true);
    }
}
