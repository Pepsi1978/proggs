using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
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
    /// OpenAI Codex ueber die interne ChatGPT-Backend-API (chatgpt.com/backend-api/codex/responses) —
    /// 1:1 nachgebaut aus Hermes (agent/auxiliary_client.py + codex_responses_adapter.py).
    ///
    /// Nutzt das ChatGPT-Abo statt API-Credits (Anmeldung per Geraetecode, siehe <see cref="CodexAuth"/>).
    /// Gibt sich gegenueber dem Endpunkt als das echte Codex-CLI aus (originator/User-Agent codex_cli_rs +
    /// ChatGPT-Account-ID aus dem JWT) — das ist Pflicht, sonst antwortet die Cloudflare-Schicht mit 403.
    /// Der Denk-Aufwand ist ueber <see cref="_effort"/> (low/medium/high) frei waehlbar.
    /// </summary>
    public sealed class CodexProvider : ILlmProvider
    {
        private const string ResponsesUrl = "https://chatgpt.com/backend-api/codex/responses";

        private readonly string _model;
        private readonly string _effort;

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(120) };

        public CodexProvider(string model, string effort)
        {
            _model = NormalizeModel(model);
            // Sonde: Wenn ein fremdes Modell auf den Default gemappt wurde, sichtbar machen.
            if (!string.Equals(_model, (model ?? "").Trim(), StringComparison.Ordinal))
                Log.Warn($"Codex: '{model}' ist kein Codex-Modell — nutze '{_model}'. " +
                         "(In den Einstellungen als Modell z. B. gpt-5.5 eintragen.)");
            _effort = NormalizeEffort(effort);
        }

        public string Name => "Codex";

        /// <summary>
        /// Codex akzeptiert nur eigene Modelle (gpt-5.5, gpt-5.4, gpt-5.3-codex …). Ein fremdes Modell
        /// — etwa wenn von Gemini auf Codex umgestellt wird, ohne das Modell-Feld zu aendern — wuerde
        /// einen HTTP 400 ausloesen ("model is not supported"). Statt zu scheitern faellt ein Nicht-gpt-
        /// Modell (oder leeres) funktionserhaltend auf den Codex-Default zurueck.
        /// </summary>
        public static string NormalizeModel(string? model)
        {
            var m = (model ?? "").Trim();
            if (m.Length == 0 || !m.StartsWith("gpt", StringComparison.OrdinalIgnoreCase))
                return CodexModels.Default;
            return m;
        }

        /// <summary>
        /// Codex-Backend akzeptiert low/medium/high; "minimal" wird (wie in Hermes) auf "low"
        /// geklemmt, leere/unbekannte Werte fallen auf "medium" zurueck.
        /// </summary>
        public static string NormalizeEffort(string? effort)
        {
            var e = (effort ?? "").Trim().ToLowerInvariant();
            return e switch
            {
                "minimal" => "low",
                "low" => "low",
                "medium" => "medium",
                "high" => "high",
                _ => "medium",
            };
        }

        public async Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
        {
            var token = await CodexAuth.GetValidAccessTokenAsync(ct).ConfigureAwait(false);
            var accountId = CodexAuth.GetAccountId();
            var payload = BuildPayloadJson(messages, _model, _effort);

            // Live-Sonde (Observability): festhalten, mit welchem Modell + Effort der Call rausgeht.
            Log.Info($"Codex-Request: Modell '{_model}', Effort '{_effort}'");

            using var req = new HttpRequestMessage(HttpMethod.Post, ResponsesUrl)
            {
                Content = new StringContent(payload, Encoding.UTF8, "application/json")
            };
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
            // Tarn-Header 1:1 aus Hermes (_codex_cloudflare_headers) — ohne diese: 403.
            req.Headers.TryAddWithoutValidation("originator", "codex_cli_rs");
            req.Headers.UserAgent.ParseAdd("codex_cli_rs/0.0.0 (VoiceAgent)");
            if (!string.IsNullOrWhiteSpace(accountId))
                req.Headers.TryAddWithoutValidation("ChatGPT-Account-ID", accountId);

            using var resp = await SharedHttp.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);

            if (resp.StatusCode == HttpStatusCode.Unauthorized)
            {
                Log.Error($"Codex 401 (Token abgelehnt): {body}");
                throw new Exception("Codex hat den Zugang abgelehnt (401). Bitte in den Einstellungen neu per Geraetecode anmelden.");
            }
            if (resp.StatusCode == HttpStatusCode.Forbidden)
            {
                Log.Error($"Codex 403 (Cloudflare/originator): {body}");
                throw new Exception("Codex-Zugang blockiert (403). Der originator/Account-Header wurde abgelehnt.");
            }
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Codex API Fehler {(int)resp.StatusCode}: {body}");
                throw new Exception($"Codex API Fehler {(int)resp.StatusCode}: {body}");
            }

            return ExtractText(body);
        }

        /// <summary>
        /// Baut den /responses-Request. System-Nachrichten werden zu "instructions" zusammengefasst,
        /// der restliche Verlauf zu "input" (user → input_text, assistant → output_text). KEIN
        /// temperature/max_output_tokens — die lehnt das Codex-Backend mit 400 ab (Hermes-Hinweis).
        /// Public static, damit der Payload-Aufbau isoliert testbar ist.
        /// </summary>
        public static string BuildPayloadJson(IReadOnlyList<LlmMessage> messages, string model, string effort)
        {
            var instructions = string.Join("\n\n",
                messages.Where(m => m.Role == LlmRole.System).Select(m => m.Text));

            var input = messages
                .Where(m => m.Role != LlmRole.System)
                .Select(m => new
                {
                    role = m.Role == LlmRole.Assistant ? "assistant" : "user",
                    content = new[]
                    {
                        new
                        {
                            type = m.Role == LlmRole.Assistant ? "output_text" : "input_text",
                            text = m.Text
                        }
                    }
                })
                .ToArray();

            var payload = new Dictionary<string, object>
            {
                ["model"] = model,
                ["instructions"] = string.IsNullOrWhiteSpace(instructions) ? "Du bist ein hilfreicher Assistent." : instructions,
                ["input"] = input.Length > 0
                    ? (object)input
                    : new[] { new { role = "user", content = new[] { new { type = "input_text", text = "" } } } },
                ["store"] = false,
                ["reasoning"] = new { effort, summary = "auto" },
                ["include"] = new[] { "reasoning.encrypted_content" },
            };

            return JsonSerializer.Serialize(payload);
        }

        /// <summary>
        /// Extrahiert den Antworttext aus einer /responses-JSON-Antwort: aus dem output[]-Array die
        /// "message"-Items (role assistant), darin die Text-Parts (type "output_text"/"text").
        /// reasoning-Items werden uebersprungen. Public static = isoliert testbar.
        /// </summary>
        public static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            var root = doc.RootElement;

            if (!root.TryGetProperty("output", out var output) || output.ValueKind != JsonValueKind.Array)
                throw new Exception("Unerwartete Codex-Antwort: kein output-Array");

            var sb = new StringBuilder();
            foreach (var item in output.EnumerateArray())
            {
                if (item.ValueKind != JsonValueKind.Object) continue;
                if (!item.TryGetProperty("type", out var typeEl) || typeEl.GetString() != "message") continue;
                if (!item.TryGetProperty("content", out var content) || content.ValueKind != JsonValueKind.Array) continue;

                foreach (var part in content.EnumerateArray())
                {
                    if (part.ValueKind != JsonValueKind.Object) continue;
                    var ptype = part.TryGetProperty("type", out var pt) ? pt.GetString() : null;
                    if (ptype != "output_text" && ptype != "text") continue;
                    if (part.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                        sb.Append(t.GetString());
                }
            }

            var text = sb.ToString().Trim();
            if (string.IsNullOrEmpty(text)) throw new Exception("Kein Text in Codex-Antwort");
            return text;
        }
    }
}
