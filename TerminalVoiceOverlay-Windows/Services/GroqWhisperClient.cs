using System;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    public sealed class GroqWhisperClient
    {
        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _language;
        private readonly string _url;
        private readonly HttpClient _http;
        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 3;
        private static readonly int[] DelaysMs = { 2000, 4000, 8000 };

        public GroqWhisperClient(string apiKey, string model, string language, string url)
        {
            _apiKey = apiKey;
            _model = model;
            _language = language;
            _url = url;
            _http = new HttpClient { Timeout = TimeSpan.FromSeconds(180) };
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            return await TranscribeWithRetry(wavFilePath, 0);
        }

        private async Task<string> TranscribeWithRetry(string wavFilePath, int attempt)
        {
            using var content = new MultipartFormDataContent();

            // Add fields
            content.Add(new StringContent(_model), "model");
            content.Add(new StringContent(_language), "language");
            content.Add(new StringContent("text"), "response_format");
            content.Add(new StringContent("0"), "temperature");

            // Add file
            var fileBytes = await File.ReadAllBytesAsync(wavFilePath);
            var fileContent = new ByteArrayContent(fileBytes);
            fileContent.Headers.ContentType = new MediaTypeHeaderValue("audio/wav");
            content.Add(fileContent, "file", "recording.wav");

            using var request = new HttpRequestMessage(HttpMethod.Post, _url)
            {
                Content = content
            };
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

            var response = await _http.SendAsync(request);
            var statusCode = (int)response.StatusCode;

            if (response.IsSuccessStatusCode)
            {
                var text = (await response.Content.ReadAsStringAsync()).Trim();
                if (!string.IsNullOrEmpty(text))
                    return text;
                throw new Exception("Leere Antwort von Groq");
            }

            if (Array.IndexOf(RetryableStatusCodes, statusCode) >= 0 && attempt < MaxRetries)
            {
                Console.WriteLine($"Groq {statusCode} - Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms...");
                await Task.Delay(DelaysMs[attempt]);
                return await TranscribeWithRetry(wavFilePath, attempt + 1);
            }

            var errorBody = await response.Content.ReadAsStringAsync();
            throw new Exception($"Groq API Fehler {statusCode}: {errorBody}");
        }
    }
}
