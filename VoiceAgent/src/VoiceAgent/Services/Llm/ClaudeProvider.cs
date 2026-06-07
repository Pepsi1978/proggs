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
    /// Anthropic Claude (api.anthropic.com/v1/messages). In Baustein 1 vorbereitet
    /// und voll funktionsfaehig — Standard bleibt Gemini, in der UI umschaltbar.
    /// </summary>
    public sealed class ClaudeProvider : ILlmProvider
    {
        private readonly string _apiKey;
        private readonly string _model;

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(120) };

        public ClaudeProvider(string apiKey, string model)
        {
            _apiKey = apiKey;
            _model = model;
        }

        public string Name => "Claude";

        public async Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein Claude-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            var system = string.Join("\n\n",
                messages.Where(m => m.Role == LlmRole.System).Select(m => m.Text));

            var msgs = messages
                .Where(m => m.Role != LlmRole.System)
                .Select(m => new
                {
                    role = m.Role == LlmRole.Assistant ? "assistant" : "user",
                    content = m.Text
                })
                .ToArray();

            var payload = new Dictionary<string, object>
            {
                ["model"] = _model,
                ["max_tokens"] = 2048,
                ["messages"] = msgs
            };
            if (!string.IsNullOrWhiteSpace(system))
                payload["system"] = system;

            using var req = new HttpRequestMessage(HttpMethod.Post, "https://api.anthropic.com/v1/messages");
            req.Headers.Add("x-api-key", _apiKey);
            req.Headers.Add("anthropic-version", "2023-06-01");
            req.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            var resp = await SharedHttp.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Claude API Fehler {(int)resp.StatusCode}: {body}");
                throw new Exception($"Claude API Fehler {(int)resp.StatusCode}: {body}");
            }
            return ExtractText(body);
        }

        /// <summary>Extrahiert den Text aus der Claude-Antwort (content[].text).</summary>
        public static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            if (!doc.RootElement.TryGetProperty("content", out var content))
                throw new Exception("Unerwartete Claude-Antwort: kein content");
            foreach (var part in content.EnumerateArray())
            {
                if (part.TryGetProperty("text", out var t))
                {
                    var s = t.GetString()?.Trim();
                    if (!string.IsNullOrEmpty(s)) return s!;
                }
            }
            throw new Exception("Kein Text in Claude-Antwort");
        }
    }
}
