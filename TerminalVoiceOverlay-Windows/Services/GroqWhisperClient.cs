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

        // Statischer geteilter HttpClient. In dieser App wird der GroqWhisperClient
        // aktuell pro Process nur einmal gebaut, daher ist Socket-Exhaustion
        // KEIN konkretes Problem — aber: kuenftiges Settings-Reload (z.B. Wechsel
        // des Whisper-Endpoints) wuerde mit per-instance HttpClient pro Reload
        // einen neuen Socket-Pool aufmachen und alte Verbindungen erst nach
        // TIME_WAIT freigeben. Statisch geteilt wie bei GeminiClient.SharedHttp
        // ist die kanonische .NET-Loesung. Authorization-Header wird pro Request
        // gesetzt (nicht am Client), daher kein Konflikt bei Sharing.
        private static readonly HttpClient SharedHttp = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(180)
        };
        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 3;
        private static readonly int[] DelaysMs = { 2000, 4000, 8000 };

        public GroqWhisperClient(string apiKey, string model, string language, string url)
        {
            _apiKey = apiKey;
            _model = model;
            _language = language;
            _url = url;
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            // Datei einmal in den Speicher laden und ueber alle Retry-Versuche
            // wiederverwenden. Vorher las jeder Retry die WAV-Datei neu von
            // Disk — bei 3 Retries und ~1 MB Audio entstehen 3 MB unnoetige
            // Disk-I/O. Die Bytes werden im Aufruf nicht mutiert, also ist
            // Sharing zwischen Versuchen sicher.
            byte[] fileBytes = await File.ReadAllBytesAsync(wavFilePath);
            return await TranscribeWithRetry(fileBytes, 0);
        }

        private async Task<string> TranscribeWithRetry(byte[] fileBytes, int attempt)
        {
            using var content = new MultipartFormDataContent();

            // Add fields. Kein prompt-Parameter mehr — der Original-Whisper-Output
            // wird unveraendert weitergegeben (z. B. an Gemini), wo themenspezifische
            // Profile die Nachbearbeitung uebernehmen.
            content.Add(new StringContent(_model), "model");
            content.Add(new StringContent(_language), "language");
            content.Add(new StringContent("text"), "response_format");
            content.Add(new StringContent("0"), "temperature");

            // Add file (Bytes wurden vom Aufrufer einmalig geladen)
            var fileContent = new ByteArrayContent(fileBytes);
            fileContent.Headers.ContentType = new MediaTypeHeaderValue("audio/wav");
            content.Add(fileContent, "file", "recording.wav");

            using var request = new HttpRequestMessage(HttpMethod.Post, _url)
            {
                Content = content
            };
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

            var response = await SharedHttp.SendAsync(request);
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
                return await TranscribeWithRetry(fileBytes, attempt + 1);
            }

            var errorBody = await response.Content.ReadAsStringAsync();
            throw new Exception($"Groq API Fehler {statusCode}: {errorBody}");
        }
    }
}
