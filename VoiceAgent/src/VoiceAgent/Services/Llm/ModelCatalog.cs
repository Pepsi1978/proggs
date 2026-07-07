using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Liefert pro Anbieter die verfuegbaren Modell-IDs fuer die Auswahl-Dropdowns in den
    /// Einstellungen. Holt die Liste LIVE vom jeweiligen Anbieter (immer aktuell) und faellt
    /// auf eine recherchierte Liste zurueck, wenn kein Schluessel/keine Verbindung da ist.
    ///
    /// So kann nie wieder ein Modell ausgewaehlt werden, das nicht zum Anbieter passt
    /// (das war die Ursache des Codex-400 "model not supported").
    ///
    /// Fallback-Listen Stand 2026-06-08 (3-Researcher-Recherche, offizielle Doku).
    /// </summary>
    public static class ModelCatalog
    {
        private static readonly HttpClient Http = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(10) };

        // ----- Fallback-Listen (recherchiert) -----
        private static readonly string[] GeminiFlashFallback =
        {
            "gemini-flash-latest", "gemini-3.5-flash", "gemini-3.1-flash",
            "gemini-3.1-flash-lite", "gemini-2.5-flash", "gemini-2.5-flash-lite",
            "gemini-flash-lite-latest",
        };
        private static readonly string[] OpenAiFallback =
        {
            "gpt-5.5", "gpt-5.4", "gpt-5.4-mini", "gpt-4.1", "gpt-4.1-mini", "gpt-4o-mini", "o3",
        };
        private static readonly string[] ClaudeFallback =
        {
            "claude-opus-4-8", "claude-sonnet-4-6", "claude-haiku-4-5-20251001", "claude-opus-4-7",
        };

        /// <summary>Die kuratierte Fallback-Liste eines Anbieters (nie leer).</summary>
        public static IReadOnlyList<string> Fallback(string? provider) => (provider ?? "gemini").ToLowerInvariant() switch
        {
            "codex" => CodexModels.Fallback,
            "openai" => OpenAiFallback,
            "claude" => ClaudeFallback,
            _ => GeminiFlashFallback,
        };

        /// <summary>
        /// Verfuegbare Modelle des Anbieters — live, sonst Fallback. Wirft nie (Fehler -> Fallback).
        /// </summary>
        public static async Task<IReadOnlyList<string>> GetModelsAsync(string? provider, CancellationToken ct = default)
        {
            var p = (provider ?? "gemini").ToLowerInvariant();
            try
            {
                return p switch
                {
                    "codex" => await CodexModels.GetModelIdsAsync(ct).ConfigureAwait(false),
                    "openai" => await OpenAiAsync(ct).ConfigureAwait(false),
                    "claude" => await ClaudeAsync(ct).ConfigureAwait(false),
                    _ => await GeminiFlashAsync(ct).ConfigureAwait(false),
                };
            }
            catch (Exception ex)
            {
                Log.Warn($"ModelCatalog: Live-Liste fuer '{p}' fehlgeschlagen ({ex.Message}) — nutze Fallback");
                return Fallback(p);
            }
        }

        /// <summary>Gemini: NUR Flash-Modelle, die generateContent unterstuetzen (keine Bild-Flash-Modelle).</summary>
        public static async Task<IReadOnlyList<string>> GeminiFlashAsync(CancellationToken ct = default)
        {
            var key = Config.ReadApiKey("gemini");
            if (string.IsNullOrWhiteSpace(key)) return GeminiFlashFallback;

            using var resp = await Http.GetAsync(
                $"https://generativelanguage.googleapis.com/v1beta/models?key={key}", ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode) return GeminiFlashFallback;

            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(body);
            var list = new List<string>();
            if (doc.RootElement.TryGetProperty("models", out var models) && models.ValueKind == JsonValueKind.Array)
            {
                foreach (var m in models.EnumerateArray())
                {
                    var name = m.TryGetProperty("name", out var n) ? n.GetString() : null;
                    if (string.IsNullOrEmpty(name)) continue;
                    var id = name.StartsWith("models/", StringComparison.Ordinal) ? name.Substring("models/".Length) : name;
                    var lo = id.ToLowerInvariant();
                    if (!lo.Contains("flash") || lo.Contains("image")) continue;   // nur Text-Flash

                    bool genContent = false;
                    if (m.TryGetProperty("supportedGenerationMethods", out var meth) && meth.ValueKind == JsonValueKind.Array)
                        foreach (var x in meth.EnumerateArray())
                            if (x.GetString() == "generateContent") { genContent = true; break; }
                    if (!genContent) continue;

                    if (!list.Contains(id)) list.Add(id);
                }
            }
            return list.Count > 0 ? list : GeminiFlashFallback;
        }

        /// <summary>OpenAI: Chat-/Text-Modelle (gpt-*/o<Ziffer>), ohne Embeddings/Audio/Bild/Codex.</summary>
        private static async Task<IReadOnlyList<string>> OpenAiAsync(CancellationToken ct)
        {
            var key = Config.ReadApiKey("openai");
            if (string.IsNullOrWhiteSpace(key)) return OpenAiFallback;

            using var req = new HttpRequestMessage(HttpMethod.Get, "https://api.openai.com/v1/models");
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", key);
            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode) return OpenAiFallback;

            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(body);
            var list = new List<string>();
            if (doc.RootElement.TryGetProperty("data", out var data) && data.ValueKind == JsonValueKind.Array)
                foreach (var m in data.EnumerateArray())
                {
                    var id = m.TryGetProperty("id", out var i) ? i.GetString() : null;
                    if (IsOpenAiChatModel(id) && !list.Contains(id!)) list.Add(id!);
                }
            list.Sort(StringComparer.OrdinalIgnoreCase);
            return list.Count > 0 ? list : OpenAiFallback;
        }

        private static readonly string[] OpenAiExclude =
            { "embedding", "whisper", "transcribe", "tts", "dall-e", "image", "audio", "realtime", "moderation", "codex", "search", "computer-use" };

        private static bool IsOpenAiChatModel(string? id)
        {
            if (string.IsNullOrEmpty(id)) return false;
            var lo = id.ToLowerInvariant();
            if (lo == "chat-latest") return true;
            bool prefix = lo.StartsWith("gpt-", StringComparison.Ordinal)
                          || (lo.Length >= 2 && lo[0] == 'o' && char.IsDigit(lo[1]));
            if (!prefix) return false;
            foreach (var ex in OpenAiExclude) if (lo.Contains(ex)) return false;
            return true;
        }

        /// <summary>Claude: alle Modelle aus der Models-API (neueste zuerst).</summary>
        private static async Task<IReadOnlyList<string>> ClaudeAsync(CancellationToken ct)
        {
            var key = Config.ReadApiKey("claude");
            if (string.IsNullOrWhiteSpace(key)) return ClaudeFallback;

            using var req = new HttpRequestMessage(HttpMethod.Get, "https://api.anthropic.com/v1/models?limit=100");
            req.Headers.TryAddWithoutValidation("x-api-key", key);
            req.Headers.TryAddWithoutValidation("anthropic-version", "2023-06-01");
            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode) return ClaudeFallback;

            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(body);
            var list = new List<string>();
            if (doc.RootElement.TryGetProperty("data", out var data) && data.ValueKind == JsonValueKind.Array)
                foreach (var m in data.EnumerateArray())
                    if (m.TryGetProperty("id", out var i) && i.GetString() is string id && id.Length > 0 && !list.Contains(id))
                        list.Add(id);
            return list.Count > 0 ? list : ClaudeFallback;
        }
    }
}
