using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace PromptBoard.Services;

/// <summary>
/// Google Gemini generateContent client. Reads the API key from
/// <see cref="IAppSettingsRepository"/> on every call so the user can
/// change it without restarting. Uses the v1beta endpoint with
/// systemInstruction + contents. Manual retry loop, 3 attempts,
/// exponential backoff, retryable on 429 and 5xx.
/// </summary>
public sealed class GeminiService : IGeminiService
{
    private const string BaseEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/";
    private const int MaxRetries = 3;

    private readonly HttpClient _http;
    private readonly IAppSettingsRepository _settings;
    private readonly ILogger<GeminiService> _logger;

    public GeminiService(
        HttpClient http,
        IAppSettingsRepository settings,
        ILogger<GeminiService> logger)
    {
        _http = http;
        _settings = settings;
        _logger = logger;
    }

    public async Task<string> ImproveAsync(
        string userPrompt,
        string metaPrompt,
        string model,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(userPrompt))
        {
            throw new ArgumentException("userPrompt must not be empty.", nameof(userPrompt));
        }
        if (string.IsNullOrWhiteSpace(metaPrompt))
        {
            throw new ArgumentException("metaPrompt must not be empty.", nameof(metaPrompt));
        }
        if (string.IsNullOrWhiteSpace(model))
        {
            model = "gemini-2.5-flash";
        }

        AppSettings settings = await _settings.GetAsync(ct);
        if (string.IsNullOrWhiteSpace(settings.GeminiApiKey))
        {
            throw new GeminiApiKeyMissingException();
        }

        Exception? lastError = null;
        for (int attempt = 1; attempt <= MaxRetries; attempt++)
        {
            try
            {
                return await SendOnceAsync(userPrompt, metaPrompt, model, settings.GeminiApiKey!, ct);
            }
            catch (GeminiApiKeyMissingException)
            {
                throw;
            }
            catch (GeminiServiceException ex) when (IsRetryable(ex.StatusCode) && attempt < MaxRetries)
            {
                lastError = ex;
                int delayMs = 400 * (int)Math.Pow(2, attempt - 1);
                _logger.LogWarning(
                    "Gemini retry {Attempt}/{Max} after {Delay}ms (status={Status}).",
                    attempt, MaxRetries, delayMs, ex.StatusCode);
                await Task.Delay(delayMs, ct);
            }
            catch (HttpRequestException ex) when (attempt < MaxRetries)
            {
                lastError = ex;
                int delayMs = 400 * (int)Math.Pow(2, attempt - 1);
                _logger.LogWarning(ex, "Gemini transport error on attempt {Attempt}, retrying in {Delay}ms.", attempt, delayMs);
                await Task.Delay(delayMs, ct);
            }
        }

        throw new GeminiServiceException(
            "Gemini-Aufruf nach mehrfachen Versuchen fehlgeschlagen.",
            inner: lastError);
    }

    private async Task<string> SendOnceAsync(
        string userPrompt,
        string metaPrompt,
        string model,
        string apiKey,
        CancellationToken ct)
    {
        string url = $"{BaseEndpoint}{Uri.EscapeDataString(model)}:generateContent?key={Uri.EscapeDataString(apiKey)}";

        var payload = new GenerateContentRequest
        {
            SystemInstruction = new ContentPart
            {
                Parts = [new TextPart { Text = metaPrompt }],
            },
            Contents =
            [
                new ContentPart { Parts = [new TextPart { Text = userPrompt }] },
            ],
        };

        using HttpResponseMessage response = await _http.PostAsJsonAsync(url, payload, ct);
        string body = await response.Content.ReadAsStringAsync(ct);

        if (!response.IsSuccessStatusCode)
        {
            int status = (int)response.StatusCode;
            string message = response.StatusCode switch
            {
                HttpStatusCode.Unauthorized =>
                    "Gemini lehnt den API-Key ab (HTTP 401). Bitte im Settings-Dialog pruefen.",
                HttpStatusCode.Forbidden =>
                    "Gemini verweigert Zugriff (HTTP 403). API-Key pruefen.",
                HttpStatusCode.TooManyRequests =>
                    "Gemini meldet zu viele Anfragen (HTTP 429). Erneuter Versuch...",
                _ => $"Gemini-API meldet HTTP {status}: {TruncateForLog(body)}",
            };
            throw new GeminiServiceException(message, status);
        }

        try
        {
            using var doc = JsonDocument.Parse(body);
            if (!doc.RootElement.TryGetProperty("candidates", out JsonElement candidates)
                || candidates.ValueKind != JsonValueKind.Array
                || candidates.GetArrayLength() == 0)
            {
                throw new GeminiServiceException("Gemini-Antwort enthielt keine Kandidaten.");
            }

            JsonElement firstCandidate = candidates[0];
            if (!firstCandidate.TryGetProperty("content", out JsonElement content))
            {
                throw new GeminiServiceException("Gemini-Antwort: Kandidat ohne 'content'.");
            }
            if (!content.TryGetProperty("parts", out JsonElement parts) || parts.ValueKind != JsonValueKind.Array)
            {
                throw new GeminiServiceException("Gemini-Antwort: Kandidat ohne 'parts'.");
            }

            var sb = new System.Text.StringBuilder();
            foreach (JsonElement part in parts.EnumerateArray())
            {
                if (part.TryGetProperty("text", out JsonElement text) && text.ValueKind == JsonValueKind.String)
                {
                    sb.Append(text.GetString());
                }
            }

            string result = sb.ToString();
            if (string.IsNullOrWhiteSpace(result))
            {
                throw new GeminiServiceException("Gemini-Antwort enthielt keinen Text.");
            }

            _logger.LogInformation("Gemini response OK: {Chars} chars from model {Model}.", result.Length, model);
            return result;
        }
        catch (JsonException ex)
        {
            throw new GeminiServiceException("Gemini-Antwort war kein gueltiges JSON.", inner: ex);
        }
    }

    private static bool IsRetryable(int? statusCode) => statusCode switch
    {
        (int)HttpStatusCode.TooManyRequests => true,
        >= 500 and < 600 => true,
        _ => false,
    };

    private static string TruncateForLog(string s, int max = 300)
        => s.Length <= max ? s : string.Concat(s.AsSpan(0, max), "...");

    // -------- serialization dto --------

    private sealed class GenerateContentRequest
    {
        [System.Text.Json.Serialization.JsonPropertyName("systemInstruction")]
        public ContentPart? SystemInstruction { get; set; }

        [System.Text.Json.Serialization.JsonPropertyName("contents")]
        public ContentPart[] Contents { get; set; } = [];
    }

    private sealed class ContentPart
    {
        [System.Text.Json.Serialization.JsonPropertyName("parts")]
        public TextPart[] Parts { get; set; } = [];
    }

    private sealed class TextPart
    {
        [System.Text.Json.Serialization.JsonPropertyName("text")]
        public string Text { get; set; } = string.Empty;
    }
}
