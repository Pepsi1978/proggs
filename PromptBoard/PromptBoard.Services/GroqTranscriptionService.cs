using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace PromptBoard.Services;

/// <summary>
/// Groq Whisper transcription via multipart POST. Reads the API key and
/// model from <see cref="IAppSettingsRepository"/> each call so the user
/// can swap them at runtime without restarting the app.
/// </summary>
public sealed class GroqTranscriptionService : IGroqTranscriptionService
{
    private const string Endpoint = "https://api.groq.com/openai/v1/audio/transcriptions";
    private const int MaxRetries = 3;

    private readonly HttpClient _http;
    private readonly IAppSettingsRepository _settings;
    private readonly ILogger<GroqTranscriptionService> _logger;

    public GroqTranscriptionService(
        HttpClient http,
        IAppSettingsRepository settings,
        ILogger<GroqTranscriptionService> logger)
    {
        _http = http;
        _settings = settings;
        _logger = logger;
    }

    public async Task<string> TranscribeAsync(byte[] wavBytes, CancellationToken ct = default)
    {
        if (wavBytes is null || wavBytes.Length == 0)
        {
            throw new ArgumentException("Audio stream is empty.", nameof(wavBytes));
        }

        AppSettings settings = await _settings.GetAsync(ct);
        if (string.IsNullOrWhiteSpace(settings.GroqApiKey))
        {
            throw new GroqApiKeyMissingException();
        }

        string model = string.IsNullOrWhiteSpace(settings.GroqModel)
            ? "whisper-large-v3-turbo"
            : settings.GroqModel;

        Exception? lastError = null;
        for (int attempt = 1; attempt <= MaxRetries; attempt++)
        {
            try
            {
                return await SendOnceAsync(wavBytes, settings.GroqApiKey!, model, ct);
            }
            catch (GroqApiKeyMissingException)
            {
                throw;
            }
            catch (GroqTranscriptionException ex) when (IsRetryable(ex.StatusCode) && attempt < MaxRetries)
            {
                lastError = ex;
                int delayMs = 400 * (int)Math.Pow(2, attempt - 1);
                _logger.LogWarning(
                    "Groq transcription retry {Attempt}/{Max} after {Delay}ms (status={Status}).",
                    attempt, MaxRetries, delayMs, ex.StatusCode);
                await Task.Delay(delayMs, ct);
            }
            catch (HttpRequestException ex) when (attempt < MaxRetries)
            {
                lastError = ex;
                int delayMs = 400 * (int)Math.Pow(2, attempt - 1);
                _logger.LogWarning(ex, "Groq transport error on attempt {Attempt}, retrying in {Delay}ms.", attempt, delayMs);
                await Task.Delay(delayMs, ct);
            }
        }

        throw new GroqTranscriptionException(
            "Groq-Transkription nach mehrfachen Versuchen fehlgeschlagen.",
            inner: lastError);
    }

    private async Task<string> SendOnceAsync(byte[] wavBytes, string apiKey, string model, CancellationToken ct)
    {
        using var content = new MultipartFormDataContent();

        var audio = new ByteArrayContent(wavBytes);
        audio.Headers.ContentType = new MediaTypeHeaderValue("audio/wav");
        content.Add(audio, name: "file", fileName: "audio.wav");
        content.Add(new StringContent(model), name: "model");
        content.Add(new StringContent("json"), name: "response_format");

        using var request = new HttpRequestMessage(HttpMethod.Post, Endpoint) { Content = content };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);

        using HttpResponseMessage response = await _http.SendAsync(request, HttpCompletionOption.ResponseContentRead, ct);
        string body = await response.Content.ReadAsStringAsync(ct);

        if (!response.IsSuccessStatusCode)
        {
            int status = (int)response.StatusCode;
            string message = response.StatusCode switch
            {
                HttpStatusCode.Unauthorized =>
                    "Groq lehnt den API-Key ab (HTTP 401). Bitte im Settings-Dialog pruefen.",
                HttpStatusCode.TooManyRequests =>
                    "Groq meldet zu viele Anfragen (HTTP 429). Erneuter Versuch...",
                _ => $"Groq-API meldet HTTP {status}: {TruncateForLog(body)}",
            };
            throw new GroqTranscriptionException(message, status);
        }

        try
        {
            using var doc = JsonDocument.Parse(body);
            if (doc.RootElement.TryGetProperty("text", out JsonElement text) && text.ValueKind == JsonValueKind.String)
            {
                string transcript = text.GetString() ?? string.Empty;
                _logger.LogInformation("Groq transcription OK: {Chars} chars.", transcript.Length);
                return transcript;
            }
        }
        catch (JsonException ex)
        {
            throw new GroqTranscriptionException("Groq-Antwort war kein gueltiges JSON.", inner: ex);
        }

        throw new GroqTranscriptionException("Groq-Antwort enthielt kein 'text'-Feld.");
    }

    private static bool IsRetryable(int? statusCode) => statusCode switch
    {
        (int)HttpStatusCode.TooManyRequests => true,
        >= 500 and < 600 => true,
        _ => false,
    };

    private static string TruncateForLog(string s, int max = 300)
        => s.Length <= max ? s : string.Concat(s.AsSpan(0, max), "...");
}
