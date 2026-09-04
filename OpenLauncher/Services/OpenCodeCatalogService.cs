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
