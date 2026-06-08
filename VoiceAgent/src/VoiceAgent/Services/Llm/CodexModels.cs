using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Verfuegbare Codex-Modelle. Holt die Liste live vom Codex-Backend
    /// (chatgpt.com/backend-api/codex/models) und faellt sonst auf die kuratierte Liste zurueck —
    /// 1:1 aus Hermes (hermes_cli/codex_models.py).
    /// </summary>
    public static class CodexModels
    {
        /// <summary>Kuratierte Fallback-Liste (Hermes DEFAULT_CODEX_MODELS, Stand 2026).</summary>
        public static readonly IReadOnlyList<string> Fallback = new[]
        {
            "gpt-5.5",
            "gpt-5.4-mini",
            "gpt-5.4",
            "gpt-5.3-codex",
            "gpt-5.3-codex-spark",
        };

        /// <summary>Standard-Modell, wenn nichts ausgewaehlt ist.</summary>
        public const string Default = "gpt-5.5";

        private static readonly HttpClient Http = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(10) };

        /// <summary>
        /// Liefert die verfuegbaren Modell-IDs. Versucht zuerst die Live-Abfrage (braucht Anmeldung),
        /// faellt bei Fehler/Nicht-Anmeldung auf <see cref="Fallback"/> zurueck (nie leer).
        /// </summary>
        public static async Task<IReadOnlyList<string>> GetModelIdsAsync(CancellationToken ct = default)
        {
            try
            {
                if (!CodexAuth.IsLoggedIn()) return Fallback;
                var token = await CodexAuth.GetValidAccessTokenAsync(ct).ConfigureAwait(false);
                var live = await FetchFromApiAsync(token, ct).ConfigureAwait(false);
                return live.Count > 0 ? live : Fallback;
            }
            catch (Exception ex)
            {
                Log.Warn($"Codex: Modell-Liste live abrufen fehlgeschlagen ({ex.Message}) — nutze Fallback");
                return Fallback;
            }
        }

        private static async Task<List<string>> FetchFromApiAsync(string accessToken, CancellationToken ct)
        {
            var result = new List<string>();
            using var req = new HttpRequestMessage(HttpMethod.Get,
                "https://chatgpt.com/backend-api/codex/models?client_version=1.0.0");
            req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", accessToken);

            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode) return result;

            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(body);
            if (!doc.RootElement.TryGetProperty("models", out var models) || models.ValueKind != JsonValueKind.Array)
                return result;

            // Nach "priority" sortieren, versteckte ueberspringen (wie Hermes _fetch_models_from_api).
            var sortable = new List<(int rank, string slug)>();
            foreach (var item in models.EnumerateArray())
            {
                if (item.ValueKind != JsonValueKind.Object) continue;
                if (!item.TryGetProperty("slug", out var slugEl) || slugEl.ValueKind != JsonValueKind.String) continue;
                var slug = slugEl.GetString()?.Trim();
                if (string.IsNullOrEmpty(slug)) continue;

                if (item.TryGetProperty("visibility", out var vis) && vis.ValueKind == JsonValueKind.String)
                {
                    var v = vis.GetString()?.Trim().ToLowerInvariant();
                    if (v == "hide" || v == "hidden") continue;
                }

                var rank = item.TryGetProperty("priority", out var pr) && pr.ValueKind == JsonValueKind.Number
                    ? pr.GetInt32() : 10_000;
                sortable.Add((rank, slug!));
            }

            sortable.Sort((x, y) => x.rank != y.rank ? x.rank.CompareTo(y.rank) : string.CompareOrdinal(x.slug, y.slug));
            foreach (var (_, slug) in sortable)
                if (!result.Contains(slug)) result.Add(slug);

            return result;
        }
    }
}
