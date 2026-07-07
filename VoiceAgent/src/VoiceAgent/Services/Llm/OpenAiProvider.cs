using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// OpenAI / ChatGPT (api.openai.com/v1/chat/completions). In Baustein 1 vorbereitet
    /// und voll funktionsfaehig — Standard bleibt Gemini, in der UI umschaltbar.
    /// </summary>
    public sealed class OpenAiProvider : ILlmProvider
    {
        private readonly string _apiKey;
        private readonly string _model;

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(120) };

        public OpenAiProvider(string apiKey, string model)
        {
            _apiKey = apiKey;
            _model = model;
        }

        public string Name => "OpenAI";

        public async Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein OpenAI-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            var msgs = messages
                .Select(m => new
                {
                    role = m.Role switch
                    {
                        LlmRole.System => "system",
                        LlmRole.Assistant => "assistant",
                        _ => "user"
                    },
                    content = m.Text
                })
                .ToArray();

            var payload = new { model = _model, messages = msgs };

            using var req = new HttpRequestMessage(HttpMethod.Post, "https://api.openai.com/v1/chat/completions");
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);
            req.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            var resp = await SharedHttp.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"OpenAI API Fehler {(int)resp.StatusCode}: {body}");
                throw new Exception($"OpenAI API Fehler {(int)resp.StatusCode}: {body}");
            }
            return ExtractText(body);
        }

        /// <summary>Extrahiert den Text aus der OpenAI-Antwort (choices[0].message.content).</summary>
        public static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            if (!doc.RootElement.TryGetProperty("choices", out var choices) || choices.GetArrayLength() == 0)
                throw new Exception("Unerwartete OpenAI-Antwort: keine choices");
            var text = choices[0].GetProperty("message").GetProperty("content").GetString()?.Trim();
            if (string.IsNullOrEmpty(text)) throw new Exception("Kein Text in OpenAI-Antwort");
            return text!;
        }
    }
}
