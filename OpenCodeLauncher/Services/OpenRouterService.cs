using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.RegularExpressions;
using OpenCodeLauncher.Models;

namespace OpenCodeLauncher.Services;

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
    private static readonly HttpClient Http = new()
    {
        DefaultRequestHeaders =
        {
            { "HTTP-Referer", "https://github.com/Pepsi1978/proggs" },
            { "X-Title", "OpenCode Launcher" }
        }
    };

    private readonly string? _apiKey;

    public OpenRouterService()
    {
        _apiKey = ResolveApiKey();
    }

    private static string? ResolveApiKey()
    {
        try
        {
            var env = Environment.GetEnvironmentVariable("OPENROUTER_API_KEY");
            if (!string.IsNullOrWhiteSpace(env)) return env;

            var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var keyFile = Path.Combine(home, "SK", "ClaudeCodeOpenRouter", "openrouter.key");
            if (File.Exists(keyFile))
            {
                var k = File.ReadAllText(keyFile).Trim();
                if (!string.IsNullOrEmpty(k)) return k;
            }
        }
        catch { /* ohne Key läuft /endpoints auch (public read) */ }
        return null;
    }

    /// <summary>Liefert den Modell-Anzeigenamen (data.name) oder null.</summary>
    public async Task<(string? displayName, List<ProviderEntry> providers)> GetProvidersAsync(string slug, CancellationToken ct = default)
    {
        var log = Logger.Instance;
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/models/{slug}/endpoints");
            if (_apiKey != null) req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            resp.EnsureSuccessStatusCode();

            await using var stream = await resp.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
            var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct).ConfigureAwait(false);

            string? displayName = null;
            var providers = new List<ProviderEntry>();

            if (doc.RootElement.TryGetProperty("data", out var data))
            {
                if (data.TryGetProperty("name", out var nameEl) && nameEl.ValueKind == JsonValueKind.String)
                    displayName = nameEl.GetString();

                if (data.TryGetProperty("endpoints", out var eps) && eps.ValueKind == JsonValueKind.Array)
                {
                    foreach (var ep in eps.EnumerateArray())
                    {
                        var p = ParseEndpoint(ep);
                        if (p != null) providers.Add(p);
                    }
                }
            }

            await EnrichProviderMetricsFromWebAsync(slug, providers, ct).ConfigureAwait(false);
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
                ThroughputLast30m = ep.TryGetProperty("throughput_last_30m", out var tp) && tp.ValueKind == JsonValueKind.Number ? tp.GetDouble() : null,
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

    private static async Task EnrichProviderMetricsFromWebAsync(string slug, List<ProviderEntry> providers, CancellationToken ct)
    {
        if (providers.Count == 0 || providers.All(p => p.ThroughputLast30m.HasValue)) return;

        try
        {
            var html = await Http.GetStringAsync($"https://openrouter.ai/{slug}/providers", ct).ConfigureAwait(false);
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
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenRouterService", "EnrichProviderMetricsFromWebAsync", $"TPS-Web-Fallback fehlgeschlagen: {ex.Message}", new { slug });
        }
    }

    private static double? ReadMetric(string body, string name)
    {
        var match = Regex.Match(body, $"{Regex.Escape(name)}\\\\\":(?<value>[0-9.]+)");
        return match.Success && double.TryParse(match.Groups["value"].Value, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out var value)
            ? value
            : null;
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
