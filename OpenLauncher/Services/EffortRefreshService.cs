using System.IO;
using System.Text.Json;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

public sealed record EffortSnapshot(List<string> Levels, string Source, DateTimeOffset CheckedAt, bool CanRemove);
public sealed record EffortUpdateReport(string Model, string Status, string Source, DateTimeOffset? LastSuccess, DateTimeOffset AttemptedAt);

public sealed class EffortRefreshService
{
    private readonly OpenCodeCatalogService _catalog = new();
    private readonly OpenRouterService _router = new();
    private static readonly object ReportGate = new();
    private static readonly string ReportPath = Path.Combine(ResearchSettingsService.DataDirectory, "effort-reports.json");
    private static Dictionary<string, EffortUpdateReport>? _reports;
    public static string Key(ModelEntry model, string access) => $"{model.ModelString} [{access}]";
    public static bool ValidLevel(string value) => value is "none" or "minimal" or "low" or "medium" or "high" or "xhigh" or "max" or "ultra" or "thinking";

    public async Task<EffortSnapshot?> RefreshAsync(ModelEntry model, string access, CancellationToken ct, bool force = false)
    {
        var problems = new List<string>();
        EffortSnapshot? current = null;
        EffortSnapshot? local = null;
        var conflict = false;
        if (access == "codex" && model.ProviderId == "openai")
        {
            local = ReadCodexCache(model);
            try
            {
                if (await CodexResearchService.Instance.IsConnectedAsync(ct).ConfigureAwait(false))
                {
                    var models = await CodexResearchService.Instance.GetModelsAsync(ct).ConfigureAwait(false);
                    var item = models.FirstOrDefault(x => x.Id == model.Slug);
                    if (item != null && item.Efforts.Count > 0)
                        current = new(item.Efforts, "OpenAI-Codex-Kontokatalog", DateTimeOffset.UtcNow, true);
                }
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch (Exception ex) { problems.Add("Codex-Kontokatalog: " + ex.Message); }
        }
        else
        {
            try
            {
                var levels = await _catalog.GetThinkingLevelsAsync(model, ct, force).ConfigureAwait(false);
                if (levels != null) current = new(levels, "https://models.dev/api.json", DateTimeOffset.UtcNow, true);
                else problems.Add("models.dev: keine eindeutigen Stufen für diese Modellkennung");
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch (Exception ex) { problems.Add("models.dev: " + ex.Message); }

            if (model.ProviderId == "openrouter")
            {
                try
                {
                    var levels = await _router.GetThinkingLevelsAsync(model.Slug, ct, force).ConfigureAwait(false);
                    if (levels != null)
                    {
                        // OpenRouter lists supported parameters, not an exact effort enumeration.
                        // Derived options may add choices, but never remove established choices.
                        if (current == null) current = new(levels, "https://openrouter.ai/api/v1/models (abgeleitete Stufen)", DateTimeOffset.UtcNow, false);
                        else if (!current.Levels.ToHashSet().SetEquals(levels))
                        {
                            conflict = true;
                            problems.Add("Quellen unterscheiden sich: models.dev-Stufen und OpenRouter-Parameter");
                        }
                    }
                    else problems.Add("OpenRouter: keine eindeutigen Fähigkeiten");
                }
                catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
                catch (Exception ex) { problems.Add("OpenRouter: " + ex.Message); }
            }
        }
        if (current != null && current.Levels.Any(level => !ValidLevel(level)))
        {
            problems.Add("Katalog enthält noch nicht unterstützte Effort-Werte; keine automatische Übernahme");
            current = null;
        }
        if (conflict) current = null;
        if (current == null || force)
        {
            var research = await CodexResearchService.Instance.ResearchAsync(model, access, ct, manual: force).ConfigureAwait(false);
            if (research != null)
            {
                if (current != null && !current.Levels.ToHashSet().SetEquals(research.Levels))
                    problems.Add("Webrecherche weicht vom Katalog ab; keine ungeprüfte Zusammenführung");
                else current = new(research.Levels, research.Source, research.CheckedAt, current?.CanRemove ?? research.ExplicitRemoval);
            }
            else if (force)
            {
                var report = CodexResearchService.Instance.GetReports().FirstOrDefault(x => x.Model == Key(model, access));
                problems.Add("Webrecherche: " + (report?.Status.Split('\n')[0] ?? "Kein neues Ergebnis"));
            }
        }
        ct.ThrowIfCancellationRequested();
        if (current != null)
        {
            Record(model, access, current, problems.Count == 0 ? "Aktualisiert" : "Aktualisiert mit Quellenhinweis: " + string.Join(" · ", problems));
            return current;
        }
        Record(model, access, null, "Bisheriger Stand bleibt erhalten. " + string.Join(" · ", problems) +
            " · Kein neuer belegter KI-Nachweis; Details unter Einstellungen.");
        return local;
    }

    public static EffortSnapshot? ReadCodexCache(ModelEntry model)
    {
        try
        {
            var home = Environment.GetEnvironmentVariable("CODEX_HOME");
            if (string.IsNullOrWhiteSpace(home)) home = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".codex");
            var path = Path.Combine(home, "models_cache.json");
            if (!File.Exists(path)) return null;
            using var doc = JsonDocument.Parse(File.ReadAllText(path));
            var root = doc.RootElement;
            if (!root.TryGetProperty("models", out var models) || models.ValueKind != JsonValueKind.Array) return null;
            foreach (var item in models.EnumerateArray())
            {
                if (!item.TryGetProperty("slug", out var slug) || slug.GetString() != model.Slug ||
                    !item.TryGetProperty("supported_reasoning_levels", out var raw) || raw.ValueKind != JsonValueKind.Array) continue;
                var levels = raw.EnumerateArray().Select(x => x.ValueKind == JsonValueKind.String ? x.GetString() :
                    x.TryGetProperty("effort", out var level) ? level.GetString() : null).OfType<string>().Distinct().ToList();
                if (levels.Count == 0 || levels.Any(x => !ValidLevel(x))) return null;
                var at = root.TryGetProperty("fetched_at", out var fetched) && fetched.TryGetDateTimeOffset(out var date)
                    ? date : new DateTimeOffset(File.GetLastWriteTimeUtc(path));
                return new(levels, "Lokaler Codex-Kontokatalog", at, false);
            }
        }
        catch (Exception ex) { Logger.Instance.Warn(nameof(EffortRefreshService), nameof(ReadCodexCache), ex.Message); }
        return null;
    }

    public static IReadOnlyList<EffortUpdateReport> GetReports()
    {
        lock (ReportGate)
        {
            if (_reports == null)
            {
                try { _reports = File.Exists(ReportPath) ? JsonSerializer.Deserialize<Dictionary<string, EffortUpdateReport>>(File.ReadAllText(ReportPath)) : null; }
                catch (Exception ex) { Logger.Instance.Warn(nameof(EffortRefreshService), "LoadReports", ex.Message); }
                _reports ??= new();
            }
            return _reports.Values.OrderByDescending(x => x.AttemptedAt).ToList();
        }
    }

    public static void Record(ModelEntry model, string access, EffortSnapshot? snapshot, string status)
    {
        lock (ReportGate)
        {
            _ = GetReports();
            var key = Key(model, access);
            _reports!.TryGetValue(key, out var previous);
            _reports[key] = new(key, status, snapshot?.Source ?? previous?.Source ?? "Noch keine bestätigte Quelle",
                snapshot?.CheckedAt ?? previous?.LastSuccess, DateTimeOffset.UtcNow);
            try
            {
                Directory.CreateDirectory(ResearchSettingsService.DataDirectory);
                File.WriteAllText(ReportPath + ".tmp", JsonSerializer.Serialize(_reports));
                File.Move(ReportPath + ".tmp", ReportPath, true);
            }
            catch (Exception ex) { Logger.Instance.Warn(nameof(EffortRefreshService), "SaveReports", ex.Message); }
        }
    }
}
