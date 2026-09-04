using System.Net.Http;
using System.Text.Json;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

/// <summary>Liest den von OpenCode selbst verwendeten models.dev-Katalog und liefert die aktiven,
/// kostenlosen Modelle des Providers OpenCode Zen.</summary>
public sealed class OpenCodeCatalogService
{
    private const string CatalogUrl = "https://models.dev/api.json";
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(30) };
    private string? _thinkingCatalog;
    private DateTime _thinkingCatalogAt;

    // null means unknown; an empty list means the catalog explicitly offers no effort choice.
    public async Task<List<string>?> GetThinkingLevelsAsync(ModelEntry model, CancellationToken ct, bool forceRefresh = false)
    {
        if (forceRefresh || _thinkingCatalog == null || DateTime.UtcNow - _thinkingCatalogAt > TimeSpan.FromMinutes(5))
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, CatalogUrl);
            request.Headers.CacheControl = new() { NoCache = true };
            using var response = await Http.SendAsync(request, ct).ConfigureAwait(false);
            response.EnsureSuccessStatusCode();
            var json = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            ct.ThrowIfCancellationRequested();
            _thinkingCatalog = json;
            _thinkingCatalogAt = DateTime.UtcNow;
        }

        using var document = JsonDocument.Parse(_thinkingCatalog);
        var slug = model.Slug;
        if (model.ProviderId == "anthropic" && slug.EndsWith("[1m]", StringComparison.Ordinal)) slug = slug[..^4];
        if (!document.RootElement.TryGetProperty(model.ProviderId, out var provider) ||
            !provider.TryGetProperty("models", out var models) ||
            !models.TryGetProperty(slug, out var item)) return null;
        if (!item.TryGetProperty("reasoning_options", out var options))
            return item.TryGetProperty("reasoning", out var reasoning) && reasoning.ValueKind == JsonValueKind.False ? [] : null;
        if (options.ValueKind != JsonValueKind.Array) return null;

        var levels = new List<string>();
        var toggle = false;
        foreach (var option in options.EnumerateArray())
        {
            var type = ReadString(option, "type");
            if (type == "toggle") toggle = true;
            if (type != "effort" || !option.TryGetProperty("values", out var values) || values.ValueKind != JsonValueKind.Array) continue;
            foreach (var value in values.EnumerateArray())
            {
                if (value.ValueKind != JsonValueKind.String) continue;
                var level = value.GetString()?.Trim().ToLowerInvariant();
                if (!string.IsNullOrWhiteSpace(level) && !levels.Contains(level)) levels.Add(level);
            }
        }
        return levels.Count == 0 && toggle ? ["none", "thinking"] : levels;
    }

    public async Task<List<ModelEntry>> GetFreeZenModelsAsync(CancellationToken ct = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, CatalogUrl);
        using var response = await Http.SendAsync(request, ct).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        await using var stream = await response.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
        using var document = await JsonDocument.ParseAsync(stream, cancellationToken: ct).ConfigureAwait(false);

        if (!document.RootElement.TryGetProperty("opencode", out var provider) ||
            !provider.TryGetProperty("models", out var modelsElement) ||
            modelsElement.ValueKind != JsonValueKind.Object)
            return [];

        var models = new List<ModelEntry>();
        foreach (var property in modelsElement.EnumerateObject())
        {
            var item = property.Value;
            if (!IsVisible(item) || !IsFree(item)) continue;

            var id = ReadString(item, "id") ?? property.Name;
            if (id.StartsWith("opencode/", StringComparison.OrdinalIgnoreCase))
                id = id["opencode/".Length..];
            if (string.IsNullOrWhiteSpace(id)) continue;

            models.Add(new ModelEntry
            {
                Slug = id,
                DisplayName = ReadString(item, "name") ?? id,
                ProviderId = "opencode",
                ProviderName = "OpenCode Zen"
            });
        }

        models.Sort((left, right) => string.Compare(left.DisplayName, right.DisplayName, StringComparison.OrdinalIgnoreCase));
        Logger.Instance.Info("OpenCodeCatalogService", "GetFreeZenModelsAsync", $"{models.Count} kostenlose OpenCode-Zen-Modelle geladen");
        return models;
    }

    private static bool IsVisible(JsonElement item)
    {
        var status = ReadString(item, "status");
        return !string.Equals(status, "alpha", StringComparison.OrdinalIgnoreCase) &&
               !string.Equals(status, "deprecated", StringComparison.OrdinalIgnoreCase);
    }

    private static bool IsFree(JsonElement item)
    {
        if (!item.TryGetProperty("cost", out var cost) || cost.ValueKind != JsonValueKind.Object) return false;
        return ReadNumber(cost, "input") == 0 && ReadNumber(cost, "output") == 0;
    }

    private static string? ReadString(JsonElement item, string propertyName) =>
        item.TryGetProperty(propertyName, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;

    private static double? ReadNumber(JsonElement item, string propertyName)
    {
        if (!item.TryGetProperty(propertyName, out var value)) return null;
        if (value.ValueKind == JsonValueKind.Number && value.TryGetDouble(out var number)) return number;
        return value.ValueKind == JsonValueKind.String &&
               double.TryParse(value.GetString(), System.Globalization.NumberStyles.Float,
                   System.Globalization.CultureInfo.InvariantCulture, out number)
            ? number
            : null;
    }
}
