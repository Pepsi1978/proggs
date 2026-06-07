using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Groq Whisper Speech-to-Text. Uebernommen aus dem bewaehrten TerminalVoiceOverlay-
    /// GroqWhisperClient (statischer HttpClient, Retry mit Backoff), ergaenzt um
    /// ConfigureAwait(false), CancellationToken und Diagnose-Logging.
    /// </summary>
    public sealed class GroqWhisperClient
    {
        public const string DefaultUrl = "https://api.groq.com/openai/v1/audio/transcriptions";

        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _language;
        private readonly string _url;

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(180) };

        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 3;
        private static readonly int[] DelaysMs = { 2000, 4000, 8000 };

        public GroqWhisperClient(string apiKey, string model, string language, string? url = null)
        {
            _apiKey = apiKey;
            _model = model;
            _language = language;
            _url = string.IsNullOrWhiteSpace(url) ? DefaultUrl : url!;
        }

        public async Task<string> TranscribeAsync(string wavFilePath, CancellationToken ct = default)
        {
            // Datei einmal laden und ueber alle Retries wiederverwenden.
            byte[] fileBytes = await File.ReadAllBytesAsync(wavFilePath, ct).ConfigureAwait(false);
            // Sanity: ein gueltiges WAV ist groesser als der 44-Byte-Header — sonst kam kein Audio an.
            Probe.That(fileBytes.Length > 44, "Whisper: WAV verdaechtig klein (evtl. kein Audio aufgenommen)", new { bytes = fileBytes.Length });
            return await TranscribeWithRetry(fileBytes, 0, ct).ConfigureAwait(false);
        }

        private async Task<string> TranscribeWithRetry(byte[] fileBytes, int attempt, CancellationToken ct)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein Groq-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            using var content = new MultipartFormDataContent
            {
                { new StringContent(_model), "model" },
                { new StringContent(_language), "language" },
                { new StringContent("text"), "response_format" },
                { new StringContent("0"), "temperature" }
            };
            var fileContent = new ByteArrayContent(fileBytes);
            fileContent.Headers.ContentType = new MediaTypeHeaderValue("audio/wav");
            content.Add(fileContent, "file", "recording.wav");

            using var request = new HttpRequestMessage(HttpMethod.Post, _url) { Content = content };
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

            var response = await SharedHttp.SendAsync(request, ct).ConfigureAwait(false);
            var statusCode = (int)response.StatusCode;

            if (response.IsSuccessStatusCode)
            {
                var text = (await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false)).Trim();
                if (!string.IsNullOrEmpty(text)) return text;
                throw new Exception("Leere Antwort von Groq");
            }

            if (Array.IndexOf(RetryableStatusCodes, statusCode) >= 0 && attempt < MaxRetries)
            {
                Log.Warn($"Groq {statusCode} — Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms");
                await Task.Delay(DelaysMs[attempt], ct).ConfigureAwait(false);
                return await TranscribeWithRetry(fileBytes, attempt + 1, ct).ConfigureAwait(false);
            }

            var errorBody = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            Log.Error($"Groq API Fehler {statusCode}: {errorBody}");
            throw new Exception($"Groq API Fehler {statusCode}: {errorBody}");
        }
    }
}
