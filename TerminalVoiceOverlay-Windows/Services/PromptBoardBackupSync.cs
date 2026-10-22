using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Merge logic for the PromptBoard Drive backup (promptboard-backup*.json). Replaces the
/// former "upload local unchecked / restore with Replace" pair that lost changes made on
/// other devices (and, because TVO and CVO share ONE local DB but sync two different
/// backup files, deleted the sibling app's prompts).
///
/// Rules (Frank's principle: a deleted prompt that comes back is annoying, a lost prompt
/// is damage):
/// - every machine remembers the last state it agreed on with the cloud (the "base",
///   one file PER BACKUP FILE, so TVO and CVO never share it);
/// - cloud unchanged since the base -> local is authoritative (local deletions propagate);
/// - cloud changed -> three-way merge per row: one-sided changes win, two-sided changes
///   go to the newer timestamp, rows new on either side are kept, a local row is never
///   deleted because the cloud lacks it; a row the user deleted locally stays deleted
///   unless it was edited elsewhere after the base;
/// - JSON shape stays exactly as macOS and PromptBoardPanel write it; unknown fields of
///   cloud rows (e.g. macOS "PromptKind") are passed through.
/// </summary>
internal static class PromptBoardBackupSync
{
    private static readonly string[] CategoryFields = { "Name", "SortOrder", "BackgroundColorHex", "Type" };
    private static readonly string[] PromptFields =
    {
        "CategoryId", "ShortLabel", "OriginalText", "ImprovedText", "ActiveVersion", "IsAlwaysOn",
        "IsPrePrompt", "IsPostPrompt", "SortOrder", "HotkeyNumber", "HotkeyLetter",
        "ImprovedByAiPromptId", "GeminiModel", "IsActiveForImprovement",
    };

    // Same defaults as the Windows/macOS parsers, so "field missing" equals "default".
    private static readonly Dictionary<string, string> FieldDefaults = new(StringComparer.Ordinal)
    {
        ["IsAlwaysOn"] = "false", ["IsPrePrompt"] = "true", ["IsPostPrompt"] = "false",
        ["IsActiveForImprovement"] = "false", ["ActiveVersion"] = "0", ["SortOrder"] = "0", ["Type"] = "0",
    };

    // ── base file (machine-local, one per backup file name) ──

    public static string BasePath(string backupFileName) => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PromptBoard",
        Path.GetFileNameWithoutExtension(backupFileName) + ".synced-base.json");

    public static string? ReadBase(string backupFileName)
    {
        try { return AtomicFile.ReadAllTextShared(BasePath(backupFileName)); }
        catch { return null; }
    }

    public static void WriteBase(string backupFileName, string json) =>
        AtomicFile.WriteAllText(BasePath(backupFileName), json);

    // ── local snapshot ──

    /// <summary>
    /// Builds the local backup JSON in exactly the shape PromptBoardPanel.BuildBackupJsonAsync
    /// writes. Returns the moment taken BEFORE reading, used as the "edited meanwhile" guard.
    /// </summary>
    public static async Task<(string Json, DateTime SnapshotUtc)> BuildLocalJsonAsync(CancellationToken ct)
    {
        var snapshotUtc = DateTime.UtcNow;
        using var scope = PromptBoardHost.Services.CreateScope();
        var backup = scope.ServiceProvider.GetRequiredService<IBackupService>();
        var doc = await backup.CreateAsync(ct).ConfigureAwait(false);

        var cats = new JsonArray();
        foreach (var c in doc.Categories)
        {
            cats.Add(new JsonObject
            {
                ["Id"] = c.Id,
                ["Name"] = c.Name,
                ["SortOrder"] = c.SortOrder,
                ["BackgroundColorHex"] = c.BackgroundColorHex,
                ["Type"] = (int)c.Type,
                ["CreatedAtUtc"] = c.CreatedAtUtc,
                ["UpdatedAtUtc"] = c.UpdatedAtUtc,
            });
        }
        var categoryIds = doc.Categories.Select(c => c.Id).ToHashSet();
        var prompts = new JsonArray();
        foreach (var p in doc.Prompts.Where(p => categoryIds.Contains(p.CategoryId)))
        {
            prompts.Add(new JsonObject
            {
                ["Id"] = p.Id,
                ["CategoryId"] = p.CategoryId,
                ["ShortLabel"] = p.ShortLabel,
                ["OriginalText"] = p.OriginalText,
                ["ImprovedText"] = p.ImprovedText,
                ["ActiveVersion"] = (int)p.ActiveVersion,
                ["IsAlwaysOn"] = p.IsAlwaysOn,
                ["IsPrePrompt"] = p.IsPrePrompt,
                ["IsPostPrompt"] = p.IsPostPrompt,
                ["SortOrder"] = p.SortOrder,
                ["HotkeyNumber"] = p.HotkeyNumber,
                ["HotkeyLetter"] = p.HotkeyLetter is char l ? l.ToString() : null,
                ["ImprovedByAiPromptId"] = p.ImprovedByAiPromptId,
                ["IsAiImprovementPrompt"] = p.IsAiImprovementPrompt,
                ["GeminiModel"] = p.GeminiModel,
                ["IsActiveForImprovement"] = p.IsActiveForImprovement,
                ["CreatedAtUtc"] = p.CreatedAtUtc,
                ["UpdatedAtUtc"] = p.UpdatedAtUtc,
            });
        }
        var root = new JsonObject
        {
            ["SchemaVersion"] = 2,
            ["ExportedAt"] = snapshotUtc,
            ["Categories"] = cats,
            ["Prompts"] = prompts,
            ["SeparatorTemplate"] = doc.SeparatorTemplate ?? " ; ",
        };
        return (root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }), snapshotUtc);
    }

    /// <summary>Applies the rows the merge took from the cloud, never deleting anything.</summary>
    public static async Task ApplyLocalAsync(BackupDocument changes, DateTime snapshotUtc, CancellationToken ct)
    {
        using var scope = PromptBoardHost.Services.CreateScope();
        var backup = scope.ServiceProvider.GetRequiredService<IBackupService>();
        await backup.MergeChangedAsync(changes, snapshotUtc, ct).ConfigureAwait(false);
    }

    // ── merge ──

    public sealed class Outcome
    {
        public string MergedJson { get; init; } = "";
        /// <summary>Rows (and separator) the local store must take over; null = nothing.</summary>
        public BackupDocument? LocalChanges { get; init; }
        /// <summary>True when the cloud does not yet hold the merged state.</summary>
        public bool NeedsUpload { get; init; }
    }

    /// <summary>Content fingerprint ignoring ExportedAt and row timestamps.</summary>
    public static string Fingerprint(string json)
    {
        var root = ParseRoot(json);
        var sb = new StringBuilder();
        foreach (var (id, row) in Rows(root, "Categories").OrderBy(kv => kv.Key))
            sb.Append('C').Append(id).Append(':').Append(Content(row, CategoryFields)).Append('\n');
        foreach (var (id, row) in Rows(root, "Prompts").OrderBy(kv => kv.Key))
            sb.Append('P').Append(id).Append(':').Append(Content(row, PromptFields)).Append('\n');
        sb.Append('S').Append(Str(root["SeparatorTemplate"]) ?? " ; ");
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(sb.ToString())));
    }

    public static Outcome Merge(string localJson, string? cloudJson, string? baseJson)
    {
        if (cloudJson is null)
            return new Outcome { MergedJson = localJson, NeedsUpload = true };

        string localFp = Fingerprint(localJson);
        string cloudFp = Fingerprint(cloudJson);
        if (baseJson is not null && string.Equals(SafeFingerprint(baseJson), cloudFp, StringComparison.Ordinal))
        {
            // Nobody else changed the cloud since we last agreed: local is the truth
            // (this is how local deletions reach the cloud).
            return new Outcome { MergedJson = localJson, NeedsUpload = localFp != cloudFp };
        }
        if (localFp == cloudFp)
            return new Outcome { MergedJson = localJson, NeedsUpload = false };

        var local = ParseRoot(localJson);
        var cloud = ParseRoot(cloudJson);
        var @base = baseJson is null ? null : TryParseRoot(baseJson);

        DateTime localExport = DocTime(local), cloudExport = DocTime(cloud), baseExport = @base is null ? DateTime.MinValue : DocTime(@base);

        // Categories first (prompts need their category types for the AI flag).
        var changedCats = new List<JsonObject>();
        var mergedCats = MergeRows(
            Rows(local, "Categories"), Rows(cloud, "Categories"), @base is null ? null : Rows(@base, "Categories"),
            CategoryFields, localExport, cloudExport, baseExport, changedCats);

        var catTypes = new Dictionary<Guid, int>();
        foreach (var (id, row) in mergedCats) catTypes[id] = IntOf(row["Type"]);

        var changedPrompts = new List<JsonObject>();
        var localPrompts = Rows(local, "Prompts");
        var mergedPrompts = MergeRows(
            localPrompts, Rows(cloud, "Prompts"), @base is null ? null : Rows(@base, "Prompts"),
            PromptFields, localExport, cloudExport, baseExport, changedPrompts);

        // Normalize prompt rows that came from the cloud (macOS/legacy shapes).
        foreach (var (id, row) in mergedPrompts)
        {
            if (localPrompts.TryGetValue(id, out var l) && l["IsAiImprovementPrompt"] is JsonNode localAi)
                row["IsAiImprovementPrompt"] = localAi.DeepClone(); // the type of an existing prompt never flips
            else if (row["IsAiImprovementPrompt"] is null)
                row["IsAiImprovementPrompt"] = IsAiLegacy(row, catTypes);
        }

        // A kept prompt needs its category (e.g. category deleted locally, prompt edited
        // elsewhere): restore it from cloud or base instead of dropping the prompt.
        var mergedCatIds = mergedCats.Select(kv => kv.Id).ToHashSet();
        foreach (var (id, row) in mergedPrompts.ToList())
        {
            var catId = GuidOf(row["CategoryId"]);
            if (catId is null) { mergedPrompts.RemoveAll(kv => kv.Id == id); continue; }
            if (mergedCatIds.Contains(catId.Value)) continue;
            var source = Rows(cloud, "Categories").GetValueOrDefault(catId.Value)
                      ?? (@base is null ? null : Rows(@base, "Categories").GetValueOrDefault(catId.Value));
            if (source is null)
            {
                DiagLog.Write("PromptBoardSync", "orphan prompt dropped from merge (category unknown everywhere)", ("id", id));
                mergedPrompts.RemoveAll(kv => kv.Id == id);
                continue;
            }
            var restored = Normalized(source, cloudExport);
            mergedCats.Add((catId.Value, restored));
            mergedCatIds.Add(catId.Value);
            changedCats.Add(restored);
        }

        // Exactly one active AI improvement prompt at most (unique index + validation).
        var active = mergedPrompts.Where(kv => BoolOf(kv.Row["IsActiveForImprovement"]) && BoolOf(kv.Row["IsAiImprovementPrompt"])).ToList();
        if (active.Count > 1)
        {
            var keep = active.OrderByDescending(kv => RowTime(kv.Row, DateTime.MinValue)).First().Id;
            foreach (var (id, row) in active)
            {
                if (id == keep) continue;
                row["IsActiveForImprovement"] = false;
                if (!changedPrompts.Contains(row)) changedPrompts.Add(row);
            }
        }

        // Separator: the side that changed since the base wins; without base local wins.
        string? localSep = Str(local["SeparatorTemplate"]), cloudSep = Str(cloud["SeparatorTemplate"]);
        string? baseSep = @base is null ? null : Str(@base["SeparatorTemplate"]);
        string? mergedSep = localSep;
        string? sepChange = null;
        if (cloudSep is not null && cloudSep != localSep && @base is not null && localSep == baseSep)
        {
            mergedSep = cloudSep;
            sepChange = cloudSep;
        }

        var root = new JsonObject
        {
            ["SchemaVersion"] = 2,
            ["ExportedAt"] = DateTime.UtcNow,
            ["Categories"] = new JsonArray(mergedCats.Select(kv => (JsonNode)kv.Row.DeepClone()).ToArray()),
            ["Prompts"] = new JsonArray(mergedPrompts.Select(kv => (JsonNode)kv.Row.DeepClone()).ToArray()),
            ["SeparatorTemplate"] = mergedSep ?? " ; ",
        };
        string mergedJson = root.ToJsonString(new JsonSerializerOptions { WriteIndented = true });

        BackupDocument? changes = null;
        if (changedCats.Count > 0 || changedPrompts.Count > 0 || sepChange is not null)
        {
            var changeRoot = new JsonObject
            {
                ["SchemaVersion"] = 2,
                ["ExportedAt"] = DateTime.UtcNow,
                ["Categories"] = new JsonArray(changedCats.Select(r => (JsonNode)r.DeepClone()).ToArray()),
                ["Prompts"] = new JsonArray(changedPrompts.Select(r => (JsonNode)r.DeepClone()).ToArray()),
            };
            changes = ToDocument(changeRoot, sepChange);
        }

        return new Outcome
        {
            MergedJson = mergedJson,
            LocalChanges = changes,
            NeedsUpload = Fingerprint(mergedJson) != cloudFp,
        };
    }

    /// <summary>
    /// Three-way row merge. Returns the merged rows in local order (cloud-only rows after);
    /// every row the local store must take over is added to <paramref name="toLocal"/>.
    /// </summary>
    private static List<(Guid Id, JsonObject Row)> MergeRows(
        Dictionary<Guid, JsonObject> local, Dictionary<Guid, JsonObject> cloud, Dictionary<Guid, JsonObject>? @base,
        string[] fields, DateTime localExport, DateTime cloudExport, DateTime baseExport, List<JsonObject> toLocal)
    {
        var result = new List<(Guid, JsonObject)>();
        foreach (var id in local.Keys.Concat(cloud.Keys.Where(k => !local.ContainsKey(k))))
        {
            local.TryGetValue(id, out var l);
            cloud.TryGetValue(id, out var c);
            JsonObject? b = null;
            @base?.TryGetValue(id, out b);

            if (l is not null && c is not null)
            {
                bool lc = Same(l, c, fields);
                bool cloudUnchanged = b is not null && Same(c, b, fields);
                bool localUnchanged = b is not null && Same(l, b, fields);
                if (lc || cloudUnchanged || (!localUnchanged && RowTime(l, localExport) >= RowTime(c, cloudExport)))
                {
                    result.Add((id, (JsonObject)l.DeepClone()));
                }
                else
                {
                    var taken = Normalized(c, cloudExport);
                    result.Add((id, taken));
                    toLocal.Add(taken);
                }
            }
            else if (l is not null)
            {
                // New locally, or missing in the cloud: never delete a local row.
                result.Add((id, (JsonObject)l.DeepClone()));
            }
            else if (c is not null)
            {
                if (b is not null && Same(c, b, fields))
                    continue; // deleted here since the last sync, untouched elsewhere: stays deleted
                // New elsewhere, or deleted here but edited elsewhere afterwards: keep it.
                var taken = Normalized(c, cloudExport);
                result.Add((id, taken));
                toLocal.Add(taken);
            }
        }
        return result;
    }

    /// <summary>Clone of a cloud row with an explicit UpdatedAtUtc (macOS rows carry none).</summary>
    private static JsonObject Normalized(JsonObject row, DateTime docTime)
    {
        var copy = (JsonObject)row.DeepClone();
        if (copy["UpdatedAtUtc"] is null) copy["UpdatedAtUtc"] = RowTime(row, docTime);
        if (copy["CreatedAtUtc"] is null) copy["CreatedAtUtc"] = RowTime(row, docTime);
        return copy;
    }

    /// <summary>
    /// AI flag for rows without IsAiImprovementPrompt — same rule the Windows parser applies
    /// to such (macOS/legacy) backups: AI library category, or an explicit macOS PromptKind.
    /// </summary>
    private static bool IsAiLegacy(JsonObject row, Dictionary<Guid, int> catTypes)
    {
        var catId = GuidOf(row["CategoryId"]);
        if (catId is { } id && catTypes.TryGetValue(id, out var t) && t == (int)CategoryType.AiLibrary) return true;
        return string.Equals(Str(row["PromptKind"]), "AiImprovementPrompt", StringComparison.Ordinal);
    }

    // ── JSON -> BackupDocument (mirrors PromptBoardPanel.ApplyBackupJsonAsync) ──

    private static BackupDocument ToDocument(JsonObject root, string? separator)
    {
        DateTime docTime = DocTime(root);
        var cats = new List<CategoryDto>();
        foreach (var (id, c) in Rows(root, "Categories"))
        {
            cats.Add(new CategoryDto(
                id,
                Str(c["Name"]) ?? string.Empty,
                Str(c["BackgroundColorHex"]) ?? "#DCEDEC",
                IntOf(c["SortOrder"]),
                (CategoryType)IntOf(c["Type"]),
                TimeOf(c["CreatedAtUtc"]) ?? docTime,
                TimeOf(c["UpdatedAtUtc"]) ?? docTime));
        }
        var prompts = new List<PromptDto>();
        foreach (var (id, p) in Rows(root, "Prompts"))
        {
            var catId = GuidOf(p["CategoryId"]);
            if (catId is null) continue;
            bool isAi = BoolOf(p["IsAiImprovementPrompt"]);
            string? letter = Str(p["HotkeyLetter"]);
            prompts.Add(new PromptDto(
                id, catId.Value,
                Str(p["ShortLabel"]) ?? string.Empty,
                Str(p["OriginalText"]) ?? string.Empty,
                Str(p["ImprovedText"]),
                (PromptVersion)IntOf(p["ActiveVersion"]),
                BoolOf(p["IsAlwaysOn"]),
                IntOf(p["SortOrder"]),
                GuidOf(p["ImprovedByAiPromptId"]),
                isAi,
                Str(p["GeminiModel"]),
                isAi && BoolOf(p["IsActiveForImprovement"]),
                TimeOf(p["CreatedAtUtc"]) ?? docTime,
                TimeOf(p["UpdatedAtUtc"]) ?? docTime,
                p["HotkeyNumber"] is null ? null : IntOf(p["HotkeyNumber"]),
                string.IsNullOrEmpty(letter) ? null : letter[0],
                p["IsPrePrompt"] is null || BoolOf(p["IsPrePrompt"]),
                BoolOf(p["IsPostPrompt"])));
        }
        return new BackupDocument(2, docTime, null, cats, prompts, separator);
    }

    // ── JSON helpers ──

    private static JsonObject ParseRoot(string json) =>
        JsonNode.Parse(json) as JsonObject ?? throw new InvalidDataException("Backup JSON is not an object.");

    private static JsonObject? TryParseRoot(string json)
    {
        try { return ParseRoot(json); }
        catch { return null; }
    }

    private static string? SafeFingerprint(string json)
    {
        try { return Fingerprint(json); }
        catch { return null; }
    }

    private static Dictionary<Guid, JsonObject> Rows(JsonObject root, string key)
    {
        var map = new Dictionary<Guid, JsonObject>();
        if (root[key] is JsonArray arr)
        {
            foreach (var n in arr)
            {
                if (n is JsonObject o && GuidOf(o["Id"]) is { } id && id != Guid.Empty)
                    map.TryAdd(id, o);
            }
        }
        return map;
    }

    private static bool Same(JsonObject a, JsonObject b, string[] fields) =>
        string.Equals(Content(a, fields), Content(b, fields), StringComparison.Ordinal);

    private static string Content(JsonObject row, string[] fields)
    {
        var sb = new StringBuilder();
        foreach (var f in fields) sb.Append(Norm(f, row[f])).Append('\u001f');
        return sb.ToString();
    }

    private static string Norm(string field, JsonNode? node)
    {
        if (node is null) return FieldDefaults.TryGetValue(field, out var d) ? d : "∅";
        if (node is JsonValue v && v.TryGetValue<string>(out var s))
            return Guid.TryParse(s, out var g) ? g.ToString("D") : s;
        return node.ToJsonString();
    }

    private static DateTime DocTime(JsonObject root) => TimeOf(root["ExportedAt"]) ?? DateTime.MinValue;

    private static DateTime RowTime(JsonObject row, DateTime fallback) => TimeOf(row["UpdatedAtUtc"]) ?? fallback;

    private static DateTime? TimeOf(JsonNode? node)
    {
        // Values set in code (Normalized) hold a DateTime, parsed values a string.
        if (node is JsonValue dv && dv.TryGetValue<DateTime>(out var direct))
            return direct.Kind == DateTimeKind.Local ? direct.ToUniversalTime() : DateTime.SpecifyKind(direct, DateTimeKind.Utc);
        if (node is JsonValue v && v.TryGetValue<string>(out var s) &&
            DateTime.TryParse(s, CultureInfo.InvariantCulture,
                DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal, out var t))
            return DateTime.SpecifyKind(t, DateTimeKind.Utc);
        return null;
    }

    private static Guid? GuidOf(JsonNode? node) =>
        node is JsonValue v && v.TryGetValue<string>(out var s) && Guid.TryParse(s, out var g) ? g : null;

    private static string? Str(JsonNode? node) =>
        node is JsonValue v && v.TryGetValue<string>(out var s) ? s : null;

    private static int IntOf(JsonNode? node)
    {
        if (node is JsonValue v)
        {
            if (v.TryGetValue<int>(out var i)) return i;
            if (v.TryGetValue<long>(out var l)) return (int)l;
            if (v.TryGetValue<double>(out var d)) return (int)d;
            if (v.TryGetValue<string>(out var s) && int.TryParse(s, out var p)) return p;
        }
        return 0;
    }

    private static bool BoolOf(JsonNode? node)
    {
        if (node is JsonValue v)
        {
            if (v.TryGetValue<bool>(out var b)) return b;
            if (v.TryGetValue<int>(out var i)) return i != 0;
        }
        return false;
    }
}
