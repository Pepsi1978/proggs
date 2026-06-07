using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Standard-Gehirn: Google Gemini ueber generativelanguage.googleapis.com.
    /// HTTP-/Retry-Mechanik und das Thinking-Part-Skipping sind aus dem bewaehrten
    /// TerminalVoiceOverlay-GeminiClient uebernommen und auf Multi-Turn erweitert.
    /// </summary>
    public sealed class GeminiProvider : ILlmProvider
    {
        private readonly string _apiKey;
        private readonly string _model;

        // Ein statischer Client (BP 2 / Almanach §8.1) mit PooledConnectionLifetime
        // gegen stale DNS (§8.2).
        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(120) };

        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 4;
        private static readonly int[] DelaysMs = { 1500, 3000, 6000, 12000 };

        public GeminiProvider(string apiKey, string model)
        {
            _apiKey = apiKey;
            _model = model;
        }

        public string Name => "Gemini";

        public async Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein Gemini-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            var system = string.Join("\n\n",
                messages.Where(m => m.Role == LlmRole.System).Select(m => m.Text));

            var contents = messages
                .Where(m => m.Role != LlmRole.System)
                .Select(m => new
                {
                    role = m.Role == LlmRole.Assistant ? "model" : "user",
                    parts = new[] { new { text = m.Text } }
                })
                .ToArray();

            var payload = new Dictionary<string, object>
            {
                ["contents"] = contents,
                ["generationConfig"] = new { maxOutputTokens = 2048 }
            };
            if (!string.IsNullOrWhiteSpace(system))
                payload["system_instruction"] = new { parts = new[] { new { text = system } } };

            return await SendWithRetry(JsonSerializer.Serialize(payload), 0, ct).ConfigureAwait(false);
        }

        private async Task<string> SendWithRetry(string json, int attempt, CancellationToken ct)
        {
            var url = $"https://generativelanguage.googleapis.com/v1beta/models/{_model}:generateContent?key={_apiKey}";
            using var content = new StringContent(json, Encoding.UTF8, "application/json");

            HttpResponseMessage response;
            try
            {
                response = await SharedHttp.PostAsync(url, content, ct).ConfigureAwait(false);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                Log.Error($"Gemini-Netzwerkfehler (Versuch {attempt + 1})", ex);
                if (attempt < MaxRetries)
                {
                    await Task.Delay(DelaysMs[attempt], ct).ConfigureAwait(false);
                    return await SendWithRetry(json, attempt + 1, ct).ConfigureAwait(false);
                }
                throw;
            }

            var status = (int)response.StatusCode;
            if (response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                return ExtractText(body);
            }

            if (Array.IndexOf(RetryableStatusCodes, status) >= 0 && attempt < MaxRetries)
            {
                Log.Warn($"Gemini {status} — Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms");
                await Task.Delay(DelaysMs[attempt], ct).ConfigureAwait(false);
                return await SendWithRetry(json, attempt + 1, ct).ConfigureAwait(false);
            }

            var err = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            Log.Error($"Gemini API Fehler {status}: {err}");
            throw new Exception($"Gemini API Fehler {status}: {err}");
        }

        /// <summary>
        /// Extrahiert den Antworttext aus der Gemini-JSON-Antwort und ueberspringt
        /// reine Thinking-Parts. Public static, damit der Parser isoliert testbar ist.
        /// </summary>
        public static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            var root = doc.RootElement;

            if (!root.TryGetProperty("candidates", out var candidates) || candidates.GetArrayLength() == 0)
                throw new Exception("Unerwartete Gemini-Antwort: keine Kandidaten");

            var content = candidates[0].GetProperty("content");
            var parts = content.GetProperty("parts");
            foreach (var part in parts.EnumerateArray())
            {
                if (part.TryGetProperty("thought", out var thought) && thought.GetBoolean())
                    continue;
                if (part.TryGetProperty("text", out var textElem))
                {
                    var text = textElem.GetString()?.Trim();
                    if (!string.IsNullOrEmpty(text)) return text!;
                }
            }
            throw new Exception("Kein Text in Gemini-Antwort");
        }
    }
}
