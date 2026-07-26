using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Text.RegularExpressions;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

/// <summary>
/// Ruft die Provider-Liste für ein OpenRouter-Modell ab.
/// Endpunkt: GET https://openrouter.ai/api/v1/models/{author}/{slug}/endpoints
/// Liefert data.endpoints[] mit provider_name, tag, pricing.prompt/completion (USD/Token),
/// context_length, throughput_last_30m, status, uptime_*.
/// Provider-Slug für OpenCode-Config wird aus tag (vor "/") abgeleitet.
/// </summary>
public sealed class OpenRouterService
{
    private const string BaseUrl = "https://openrouter.ai/api/v1";
    private const string FrontendBaseUrl = "https://openrouter.ai/api/frontend";
    private static readonly HttpClient Http = new()
    {
        // Ohne Timeout blieben hängende Requests bis zum 100-Sekunden-Default stehen (Start-Load
        // ohne CancellationToken). 30 s sind großzügig für /models und den TPS-HTML-Fallback.
        Timeout = TimeSpan.FromSeconds(30),
        DefaultRequestHeaders =
        {
            { "HTTP-Referer", "https://github.com/Pepsi1978/proggs" },
            { "X-Title", "OpenLauncher" }
        }
    };

    // Die vollständige /models-Antwort ist groß und wird sowohl beim Start (Free-Modelle) als auch
    // bei jedem Modellwechsel (Thinking-Level) gebraucht. Ohne Cache lädt jeder Wechsel die komplette
    // Liste neu. Kurzlebiger String-Cache (TTL) + Semaphore gegen paralleles Doppel-Laden.
    private static readonly SemaphoreSlim ModelsCacheLock = new(1, 1);
    private static readonly TimeSpan ModelsCacheTtl = TimeSpan.FromMinutes(5);
    private static string? _modelsJson;
    private static DateTime _modelsJsonUtc = DateTime.MinValue;
    private static readonly object ThroughputCacheLock = new();
    private static readonly string ThroughputCachePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenLauncher",
        "provider-throughput.json");
    private static Dictionary<string, double>? _throughputCache;

    private static async Task<string> FetchModelsJsonAsync(CancellationToken ct, bool forceRefresh = false)
    {
        var cached = _modelsJson;
        if (!forceRefresh && cached != null && DateTime.UtcNow - _modelsJsonUtc < ModelsCacheTtl) return cached;

        await ModelsCacheLock.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            cached = _modelsJson;
            if (!forceRefresh && cached != null && DateTime.UtcNow - _modelsJsonUtc < ModelsCacheTtl) return cached;

            using var req = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/models");
            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            resp.EnsureSuccessStatusCode();
            var json = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            _modelsJson = json;
            _modelsJsonUtc = DateTime.UtcNow;
            return json;
        }
        finally
        {
            ModelsCacheLock.Release();
        }
    }

    /// <summary>Liefert den Modell-Anzeigenamen (data.name) oder null.</summary>
    public async Task<(string? displayName, List<ProviderEntry> providers)> GetProvidersAsync(string slug, CancellationToken ct = default)
    {
        var log = Logger.Instance;
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/models/{slug}/endpoints");

            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            resp.EnsureSuccessStatusCode();

            await using var stream = await resp.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
            // using: JsonDocument least ArrayPool-Puffer, die ohne Dispose nicht zurückgegeben werden.
            using var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct).ConfigureAwait(false);

            string? displayName = null;
            string? permaslug = null;
            var providers = new List<ProviderEntry>();

            if (doc.RootElement.TryGetProperty("data", out var data))
            {
                if (data.TryGetProperty("name", out var nameEl) && nameEl.ValueKind == JsonValueKind.String)
                    displayName = nameEl.GetString();

                if (data.TryGetProperty("endpoints", out var eps) && eps.ValueKind == JsonValueKind.Array)
                {
                    foreach (var ep in eps.EnumerateArray())
                    {
                        permaslug ??= ReadPermaslug(ep);
                        var p = ParseEndpoint(ep);
                        if (p != null) providers.Add(p);
                    }
                }
            }

            await EnrichProviderMetricsFromWebAsync(slug, permaslug ?? slug, providers, ct).ConfigureAwait(false);
            providers.Sort(CompareByPrice);
            log.Info("OpenRouterService", "GetProvidersAsync", $"slug={slug} -> {providers.Count} Provider");
            return (displayName, providers);
        }
        catch (Exception ex)
        {
            log.Error("OpenRouterService", "GetProvidersAsync", $"slug={slug} fehlgeschlagen: {ex.Message}", new { ex.GetType().Name });
            throw;
        }
    }

    public async Task<List<ModelEntry>> GetFreeModelsAsync(CancellationToken ct = default)
    {
        var log = Logger.Instance;
        try
        {
            var json = await FetchModelsJsonAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(json);
            var models = new List<ModelEntry>();

            if (doc.RootElement.TryGetProperty("data", out var data) && data.ValueKind == JsonValueKind.Array)
            {
                foreach (var item in data.EnumerateArray())
                {
                    var model = ParseFreeModel(item);
                    if (model != null) models.Add(model);
                }
            }

            models.Sort((a, b) => string.Compare(a.DisplayName, b.DisplayName, StringComparison.OrdinalIgnoreCase));
            log.Info("OpenRouterService", "GetFreeModelsAsync", $"{models.Count} kostenlose OpenRouter-Modelle geladen");
            return models;
        }
        catch (Exception ex)
        {
            log.Error("OpenRouterService", "GetFreeModelsAsync", $"Free-Modellliste fehlgeschlagen: {ex.Message}", new { ex.GetType().Name });
            throw;
        }
    }

    public async Task<List<string>> GetThinkingLevelsAsync(string slug, CancellationToken ct = default, bool forceRefresh = false)
    {
        var log = Logger.Instance;
        try
        {
            var json = await FetchModelsJsonAsync(ct, forceRefresh).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(json);
            if (!doc.RootElement.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Array)
                return [];

            foreach (var item in data.EnumerateArray())
            {
                if (!item.TryGetProperty("id", out var idEl) || idEl.ValueKind != JsonValueKind.String) continue;
                if (!string.Equals(idEl.GetString(), slug, StringComparison.OrdinalIgnoreCase)) continue;

                var levels = ParseThinkingLevels(item);
                log.Info("OpenRouterService", "GetThinkingLevelsAsync", $"slug={slug} -> {levels.Count} Thinking-Level");
                return levels;
            }
        }
        catch (Exception ex)
        {
            log.Warn("OpenRouterService", "GetThinkingLevelsAsync", $"Thinking-Level-Fallback für {slug}: {ex.Message}");
        }

        return [];
    }

    private static ModelEntry? ParseFreeModel(JsonElement item)
    {
        try
        {
            if (!item.TryGetProperty("id", out var idEl) || idEl.ValueKind != JsonValueKind.String) return null;
            var id = idEl.GetString() ?? string.Empty;
            if (!id.EndsWith(":free", StringComparison.OrdinalIgnoreCase)) return null;

            if (!item.TryGetProperty("pricing", out var pricing) || pricing.ValueKind != JsonValueKind.Object) return null;
            var prompt = ParseDouble(pricing, "prompt");
            var completion = ParseDouble(pricing, "completion");
            if (prompt != 0 || completion != 0) return null;

            var name = item.TryGetProperty("name", out var nameEl) && nameEl.ValueKind == JsonValueKind.String
                ? nameEl.GetString() ?? id
                : id;

            return new ModelEntry
            {
                Slug = id,
                DisplayName = NormalizeFreeModelName(name),
                ProviderId = "openrouter",
                ProviderName = "OpenRouter"
            };
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "ParseFreeModel", $"Modell übersprungen: {ex.Message}");
            return null;
        }
    }

    private static List<string> ParseThinkingLevels(JsonElement item)
    {
        if (!item.TryGetProperty("id", out var idEl) || idEl.ValueKind != JsonValueKind.String) return [];
        var id = idEl.GetString() ?? string.Empty;
        return OpenCodeVariantCatalog.GetOpenRouterLevels(
            id,
            ModelMentionsReasoning(item),
            ModelSupportsParameter(item, "reasoning_effort")).ToList();
    }

    private static bool ModelSupportsParameter(JsonElement item, string expected)
    {
        if (!item.TryGetProperty("supported_parameters", out var parameters) || parameters.ValueKind != JsonValueKind.Array)
            return false;

        return parameters.EnumerateArray().Any(parameter =>
            parameter.ValueKind == JsonValueKind.String &&
            string.Equals(parameter.GetString(), expected, StringComparison.OrdinalIgnoreCase));
    }

    private static bool ModelMentionsReasoning(JsonElement item)
    {
        if (item.TryGetProperty("supported_parameters", out var parameters) && parameters.ValueKind == JsonValueKind.Array)
        {
            foreach (var parameter in parameters.EnumerateArray())
            {
                if (parameter.ValueKind != JsonValueKind.String) continue;
                var value = parameter.GetString() ?? string.Empty;
                if (value.Contains("reasoning", StringComparison.OrdinalIgnoreCase)) return true;
            }
        }

        if (item.TryGetProperty("id", out var idEl) && idEl.ValueKind == JsonValueKind.String)
        {
            var id = idEl.GetString() ?? string.Empty;
            return id.Contains("gpt-5", StringComparison.OrdinalIgnoreCase)
                || id.Contains("reasoning", StringComparison.OrdinalIgnoreCase)
                || id.Contains("thinking", StringComparison.OrdinalIgnoreCase);
        }

        return false;
    }

    private static string NormalizeFreeModelName(string name)
    {
        var normalized = Regex.Replace(name, "\\s*\\(free\\)\\s*$", " Free", RegexOptions.IgnoreCase).Trim();
        return normalized.Replace(": ", " ");
    }

    private static ProviderEntry? ParseEndpoint(JsonElement ep)
    {
        try
        {
            var p = new ProviderEntry
            {
                ProviderName = ep.GetProperty("provider_name").GetString() ?? "",
                Tag = ep.TryGetProperty("tag", out var t) && t.ValueKind == JsonValueKind.String ? t.GetString() ?? "" : "",
                ContextLength = ep.TryGetProperty("context_length", out var cl) && cl.ValueKind == JsonValueKind.Number ? cl.GetInt32() : 0,
                Quantization = ep.TryGetProperty("quantization", out var q) && q.ValueKind == JsonValueKind.String ? q.GetString() ?? "" : "",
                Status = ep.TryGetProperty("status", out var st) && st.ValueKind == JsonValueKind.Number ? st.GetInt32() : 0,
                MaxCompletionTokens = ep.TryGetProperty("max_completion_tokens", out var mc) && mc.ValueKind == JsonValueKind.Number ? mc.GetInt32() : null,
                EndpointId = ep.TryGetProperty("id", out var endpointId) && endpointId.ValueKind == JsonValueKind.String ? endpointId.GetString() ?? "" : "",
                ThroughputLast30m = ReadThroughput(ep),
                UptimeLast5m = ep.TryGetProperty("uptime_last_5m", out var up) && up.ValueKind == JsonValueKind.Number ? up.GetDouble() : null,
            };

            if (ep.TryGetProperty("pricing", out var pr) && pr.ValueKind == JsonValueKind.Object)
            {
                p.PromptPerToken = ParseDouble(pr, "prompt");
                p.CompletionPerToken = ParseDouble(pr, "completion");
                p.CacheReadPerToken = ParseDouble(pr, "input_cache_read");
                p.Discount = pr.TryGetProperty("discount", out var d) && d.ValueKind == JsonValueKind.Number ? d.GetDouble() : null;
            }

            // Provider-Slug aus tag (vor "/") ableiten — zuverlässiger als provider_name-Normalization.
            var tag = p.Tag;
            if (!string.IsNullOrEmpty(tag))
                p.ProviderSlug = tag.Split('/')[0].ToLowerInvariant();
            else
                p.ProviderSlug = p.ProviderName.ToLowerInvariant().Replace(" ", "").Replace(".", "");

            return p;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "ParseEndpoint", $"Endpoint übersprungen: {ex.Message}");
            return null;
        }
    }

    private static string? ReadPermaslug(JsonElement endpoint)
    {
        if (!endpoint.TryGetProperty("name", out var nameEl) || nameEl.ValueKind != JsonValueKind.String) return null;
        var name = nameEl.GetString() ?? string.Empty;
        var separator = name.LastIndexOf('|');
        if (separator < 0) return null;
        var value = name[(separator + 1)..].Trim();
        if (value.EndsWith(":free", StringComparison.OrdinalIgnoreCase))
            value = value[..^":free".Length];
        return value.Contains('/') ? value : null;
    }

    private static double? ReadThroughput(JsonElement endpoint)
    {
        if (!endpoint.TryGetProperty("throughput_last_30m", out var throughput)) return null;
        if (throughput.ValueKind == JsonValueKind.Number) return throughput.GetDouble();
        if (throughput.ValueKind != JsonValueKind.Object) return null;

        return ReadJsonDouble(throughput, "p50")
            ?? ReadJsonDouble(throughput, "median")
            ?? ReadJsonDouble(throughput, "value");
    }

    private static async Task EnrichProviderMetricsFromWebAsync(string slug, string permaslug, List<ProviderEntry> providers, CancellationToken ct)
    {
        if (providers.Count == 0) return;
        if (providers.All(p => p.ThroughputLast30m.HasValue))
        {
            StoreThroughputCache(slug, providers);
            return;
        }

        string? html = null;
        try
        {
            html = await Http.GetStringAsync($"https://openrouter.ai/{slug}/providers", ct).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "EnrichProviderMetricsFromWebAsync", $"TPS-Providerseite fehlgeschlagen: {ex.Message}", new { slug });
        }

        if (html != null)
        {
            await EnrichFromThroughputChartAsync(permaslug, html, providers, ct).ConfigureAwait(false);
            EnrichFromLegacyHtml(html, providers);
        }

        StoreThroughputCache(slug, providers);
        ApplyThroughputCache(slug, providers);
    }

    private static async Task EnrichFromThroughputChartAsync(string permaslug, string html, List<ProviderEntry> providers, CancellationToken ct)
    {
        try
        {
            var endpointTags = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            var matches = Regex.Matches(
                html,
                "id\\\\\":\\\\\"(?<id>[0-9a-fA-F-]{36})\\\\\",\\\\\"name\\\\\":\\\\\"[^\\\\\"]+\\\\\".{0,40000}?provider_slug\\\\\":\\\\\"(?<tag>[^\\\\\"]+)\\\\\"",
                RegexOptions.Singleline);
            foreach (Match match in matches)
                endpointTags.TryAdd(match.Groups["id"].Value, match.Groups["tag"].Value);

            foreach (var provider in providers)
            {
                if (!string.IsNullOrEmpty(provider.EndpointId))
                    endpointTags.TryAdd(provider.EndpointId, provider.Tag);
            }
            if (endpointTags.Count == 0) return;

            var encodedPermaslug = Uri.EscapeDataString(permaslug);
            string? json = null;
            foreach (var url in new[]
                     {
                         $"{FrontendBaseUrl}/v1/stats/throughput-comparison?permaslug={encodedPermaslug}",
                         $"{FrontendBaseUrl}/stats/throughput-comparison?permaslug={encodedPermaslug}"
                     })
            {
                try
                {
                    json = await Http.GetStringAsync(url, ct).ConfigureAwait(false);
                    break;
                }
                catch (HttpRequestException)
                {
                    // OpenRouter hat diesen Website-Endpunkt bereits zwischen /stats und /v1/stats verschoben.
                }
            }
            if (json == null) return;

            using var doc = JsonDocument.Parse(json);
            if (!doc.RootElement.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Array) return;
            var latestByEndpoint = new Dictionary<string, double>(StringComparer.OrdinalIgnoreCase);
            for (var i = data.GetArrayLength() - 1; i >= 0; i--)
            {
                if (!data[i].TryGetProperty("y", out var values) || values.ValueKind != JsonValueKind.Object) continue;
                foreach (var metric in values.EnumerateObject())
                {
                    var endpointId = metric.Name.Split("::", 2, StringSplitOptions.None)[0];
                    if (!latestByEndpoint.ContainsKey(endpointId) && metric.Value.ValueKind == JsonValueKind.Number)
                        latestByEndpoint[endpointId] = metric.Value.GetDouble();
                }
            }

            var throughputByTag = new Dictionary<string, double>(StringComparer.OrdinalIgnoreCase);
            foreach (var (endpointId, throughput) in latestByEndpoint)
            {
                if (endpointTags.TryGetValue(endpointId, out var tag))
                    throughputByTag[tag] = throughput;
            }
            foreach (var provider in providers)
            {
                if (!provider.ThroughputLast30m.HasValue && throughputByTag.TryGetValue(provider.Tag, out var throughput))
                    provider.ThroughputLast30m = throughput;
            }
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "EnrichFromThroughputChartAsync", $"TPS-Chart-Fallback fehlgeschlagen: {ex.Message}", new { permaslug });
        }
    }

    private static void EnrichFromLegacyHtml(string html, List<ProviderEntry> providers)
    {
        var metrics = new Dictionary<string, double>(StringComparer.OrdinalIgnoreCase);
        var matches = Regex.Matches(
            html,
            "provider_name\\\\\":\\\\\"(?<name>[^\\\\\"]+)\\\\\".*?routing_heuristics\\\\\":\\{(?<body>.*?)\\}",
            RegexOptions.Singleline);

        foreach (Match match in matches)
        {
            var name = match.Groups["name"].Value;
            if (metrics.ContainsKey(name)) continue;
            var body = match.Groups["body"].Value;
            var throughput = ReadMetric(body, "p50_throughput_30_minutes") ?? ReadMetric(body, "p50_throughput");
            if (throughput.HasValue) metrics[name] = throughput.Value;
        }

        foreach (var provider in providers)
        {
            if (!provider.ThroughputLast30m.HasValue && metrics.TryGetValue(provider.ProviderName, out var throughput))
                provider.ThroughputLast30m = throughput;
        }
    }

    private static void StoreThroughputCache(string slug, IEnumerable<ProviderEntry> providers)
    {
        lock (ThroughputCacheLock)
        {
            EnsureThroughputCacheLoaded();
            var changed = false;
            foreach (var provider in providers.Where(p => p.ThroughputLast30m.HasValue))
            {
                var key = ThroughputCacheKey(slug, provider);
                var value = provider.ThroughputLast30m!.Value;
                if (!_throughputCache!.TryGetValue(key, out var existing) || Math.Abs(existing - value) > 0.001)
                {
                    _throughputCache[key] = value;
                    changed = true;
                }
            }
            if (!changed) return;

            try
            {
                var directory = Path.GetDirectoryName(ThroughputCachePath)!;
                Directory.CreateDirectory(directory);
                var tempPath = ThroughputCachePath + ".tmp";
                File.WriteAllText(tempPath, JsonSerializer.Serialize(_throughputCache));
                File.Move(tempPath, ThroughputCachePath, overwrite: true);
            }
            catch (Exception ex)
            {
                Logger.Instance.Warn("OpenRouterService", "StoreThroughputCache", $"TPS-Cache konnte nicht gespeichert werden: {ex.Message}");
            }
        }
    }

    private static void ApplyThroughputCache(string slug, IEnumerable<ProviderEntry> providers)
    {
        lock (ThroughputCacheLock)
        {
            EnsureThroughputCacheLoaded();
            foreach (var provider in providers.Where(p => !p.ThroughputLast30m.HasValue))
            {
                if (_throughputCache!.TryGetValue(ThroughputCacheKey(slug, provider), out var value))
                    provider.ThroughputLast30m = value;
            }
        }
    }

    private static void EnsureThroughputCacheLoaded()
    {
        if (_throughputCache != null) return;
        try
        {
            _throughputCache = File.Exists(ThroughputCachePath)
                ? JsonSerializer.Deserialize<Dictionary<string, double>>(File.ReadAllText(ThroughputCachePath))
                : null;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "EnsureThroughputCacheLoaded", $"TPS-Cache wird neu aufgebaut: {ex.Message}");
        }
        _throughputCache ??= new Dictionary<string, double>(StringComparer.OrdinalIgnoreCase);
    }

    private static string ThroughputCacheKey(string slug, ProviderEntry provider)
    {
        var endpoint = string.IsNullOrWhiteSpace(provider.Tag) ? provider.ProviderName : provider.Tag;
        return $"{slug.Trim().ToLowerInvariant()}|{endpoint.Trim().ToLowerInvariant()}";
    }

    private static double? ReadMetric(string body, string name)
    {
        var match = Regex.Match(body, $"{Regex.Escape(name)}\\\\\":(?<value>[0-9.]+)");
        return match.Success && double.TryParse(match.Groups["value"].Value, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out var value)
            ? value
            : null;
    }

    private static double? ReadJsonDouble(JsonElement parent, string name)
    {
        if (!parent.TryGetProperty(name, out var value)) return null;
        if (value.ValueKind == JsonValueKind.Number) return value.GetDouble();
        if (value.ValueKind == JsonValueKind.String && double.TryParse(value.GetString(), System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out var parsed))
            return parsed;
        return null;
    }

    private static double ParseDouble(JsonElement parent, string name)
    {
        if (parent.TryGetProperty(name, out var el) && el.ValueKind == JsonValueKind.String)
        {
            var s = el.GetString();
            if (double.TryParse(s, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out var d))
                return d;
        }
        else if (parent.TryGetProperty(name, out el) && el.ValueKind == JsonValueKind.Number)
        {
            return el.GetDouble();
        }
        return 0;
    }

    /// <summary>
    /// Sortiert günstigsten Input-Preis zuerst; bei Gleichstand Output-Preis,
    /// dann größtes Kontextfenster. — entspricht der OpenRouter-Web-Sortierung.
    /// </summary>
    private static int CompareByPrice(ProviderEntry a, ProviderEntry b)
    {
        int c = a.InputPerMillion.CompareTo(b.InputPerMillion);
        if (c != 0) return c;
        c = a.OutputPerMillion.CompareTo(b.OutputPerMillion);
        if (c != 0) return c;
        return b.ContextLength.CompareTo(a.ContextLength);
    }
}
