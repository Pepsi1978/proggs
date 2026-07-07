using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Runtime.CompilerServices;
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
    public sealed class GeminiProvider : ILlmProvider, IStreamingLlmProvider
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

            return await SendWithRetry(BuildPayloadJson(messages), 0, ct).ConfigureAwait(false);
        }

        /// <summary>Baut den Request-Body (contents + system_instruction). Von ChatAsync UND StreamAsync genutzt.</summary>
        private static string BuildPayloadJson(IReadOnlyList<LlmMessage> messages)
        {
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

            return JsonSerializer.Serialize(payload);
        }

        /// <summary>
        /// Streamt die Antwort tokenweise via Server-Sent-Events (streamGenerateContent?alt=sse).
        /// Jede "data:"-Zeile ist ein GenerateContentResponse-Chunk; Thinking-Parts werden
        /// uebersprungen. Bewusst OHNE Retry (Streaming-Retry waere zustandsbehaftet) — bei einem
        /// harten Fehler wirft die Methode, der Aufrufer faengt das ab. Fuer nicht-gestreamte
        /// Aufrufe (IntentDetector, Komprimierung) bleibt ChatAsync mit Retry erhalten.
        /// </summary>
        public async IAsyncEnumerable<string> StreamAsync(
            IReadOnlyList<LlmMessage> messages,
            [EnumeratorCancellation] CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein Gemini-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            var url = $"https://generativelanguage.googleapis.com/v1beta/models/{_model}:streamGenerateContent?alt=sse&key={_apiKey}";
            using var req = new HttpRequestMessage(HttpMethod.Post, url)
            {
                Content = new StringContent(BuildPayloadJson(messages), Encoding.UTF8, "application/json")
            };

            using var resp = await SharedHttp
                .SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct)
                .ConfigureAwait(false);

            if (!resp.IsSuccessStatusCode)
            {
                var err = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                Log.Error($"Gemini Stream Fehler {(int)resp.StatusCode}: {err}");
                throw new Exception($"Gemini Stream Fehler {(int)resp.StatusCode}: {err}");
            }

            using var stream = await resp.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
            using var reader = new StreamReader(stream, Encoding.UTF8);

            string? line;
            while ((line = await reader.ReadLineAsync(ct).ConfigureAwait(false)) != null)
            {
                if (line.Length == 0 || !line.StartsWith("data:", StringComparison.Ordinal)) continue;
                var data = line.Substring(5).Trim();
                if (data.Length == 0 || data == "[DONE]") continue;
                var delta = TryExtractDelta(data);
                if (!string.IsNullOrEmpty(delta)) yield return delta!;
            }
        }

        /// <summary>
        /// Extrahiert den Text-Delta aus EINER SSE-data-Zeile. Thinking-Parts (thought=true)
        /// werden uebersprungen. null = kein Text in diesem Chunk (z.B. nur usageMetadata oder
        /// unvollstaendige Zeile). Public static = isoliert testbar.
        /// </summary>
        public static string? TryExtractDelta(string chunkJson)
        {
            try
            {
                using var doc = JsonDocument.Parse(chunkJson);
                var root = doc.RootElement;
                if (!root.TryGetProperty("candidates", out var candidates) || candidates.GetArrayLength() == 0)
                    return null;
                if (!candidates[0].TryGetProperty("content", out var content)) return null;
                if (!content.TryGetProperty("parts", out var parts)) return null;

                var sb = new StringBuilder();
                foreach (var part in parts.EnumerateArray())
                {
                    if (part.TryGetProperty("thought", out var thought) && thought.ValueKind == JsonValueKind.True)
                        continue;
                    if (part.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                        sb.Append(t.GetString());
                }
                return sb.Length > 0 ? sb.ToString() : null;
            }
            catch (JsonException)
            {
                return null;   // unvollstaendige/leere Zeile -> ignorieren (verlustfrei: naechster Chunk folgt)
            }
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
