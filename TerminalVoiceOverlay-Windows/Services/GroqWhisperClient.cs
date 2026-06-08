using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
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

        // ----- Confidence-Gate-Schwellen gegen Whisper-Stille-Halluzination -----
        // (Bug-Almanach bugs/desktop/groq-transkription.md §2.3). Bei einer laengeren Pause
        // mitten in der Aufnahme erfindet Whisper Floskeln ("Vielen Dank"); ueber verbose_json
        // liefert Groq pro Segment Qualitaetsfelder, mit denen sich genau diese Segmente
        // verwerfen lassen — echte Sprache bleibt. UND-Logik (NoSpeechProb UND AvgLogProb)
        // schuetzt leise Sprache. Zu aggressiv? ZUERST NoSpeechProbMax auf 0.7, NIE AvgLogProbMin lockern.
        private const double NoSpeechProbMax     = 0.6;
        private const double AvgLogProbMin       = -1.0;
        private const double CompressionRatioMax = 2.4;   // Wiederholungs-Halluzination ("danke danke danke")
        private const double MiniNoiseDurSec     = 0.4;   // sehr kurzes Segment + Stille-Verdacht

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
            // verbose_json statt text: liefert die Confidence-Felder fuers Halluzinations-Gate,
            // bei Groq ohne Mehrlatenz/-kosten (nur word-Timestamps kosten extra — die fordern wir NICHT an).
            content.Add(new StringContent("verbose_json"), "response_format");
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
                var json = await response.Content.ReadAsStringAsync();
                // Confidence-Gate anwenden (Schicht 2). Bleibt nach dem Filtern Text uebrig -> zurueck.
                // Ist alles Stille/halluziniert -> leer: verhaelt sich wie bisher die "leere Antwort"
                // (gleiche Exception, gleicher Aufrufer-catch) — funktionserhaltend.
                var text = FilterTranscription(json);
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

        /// <summary>
        /// Wendet das Confidence-Gate (Almanach §2.3) auf die verbose_json-Antwort an und gibt die
        /// verbleibenden Segment-Texte zusammengefuegt zurueck. Funktionserhaltend: faellt bei
        /// unerwartetem Format auf den Roh-Text zurueck (nie verlieren); ohne Segment-Metadaten wird
        /// der top-level Text ungefiltert durchgelassen.
        /// </summary>
        private static string FilterTranscription(string json)
        {
            GroqVerboseResponse? parsed;
            try
            {
                parsed = JsonSerializer.Deserialize(json, GroqJsonContext.Default.GroqVerboseResponse);
            }
            catch (JsonException)
            {
                // Unerwartetes Format -> NICHT verlieren: top-level "text" als Fallback ziehen.
                return ExtractFallbackText(json);
            }

            if (parsed?.Segments == null || parsed.Segments.Count == 0)
            {
                // Keine Segment-Metadaten -> nicht filterbar. Funktionserhalt: top-level Text durchlassen.
                return (parsed?.Text ?? string.Empty).Trim();
            }

            var sb = new StringBuilder();
            int kept = 0, dropped = 0;
            foreach (var seg in parsed.Segments)
            {
                if (IsHallucination(seg))
                {
                    dropped++;
                    continue;
                }
                var t = seg.Text?.Trim();
                if (!string.IsNullOrEmpty(t))
                {
                    if (sb.Length > 0) sb.Append(' ');
                    sb.Append(t);
                }
                kept++;
            }

            if (dropped > 0)
                Console.WriteLine($"Groq: {dropped} Halluzinations-Segment(e) verworfen, {kept} behalten (Stille-Schutz).");
            return sb.ToString().Trim();
        }

        /// <summary>
        /// Halluzinations-Heuristik fuer EIN Segment (Almanach §2.3). Alle Regeln ODER-verknuepft;
        /// die Stille-Regel selbst ist intern UND-verknuepft (schuetzt echte leise Sprache).
        /// </summary>
        private static bool IsHallucination(GroqSegment seg)
        {
            if (seg.NoSpeechProb > NoSpeechProbMax && seg.AvgLogProb < AvgLogProbMin) return true; // Stille (UND!)
            if (seg.CompressionRatio > CompressionRatioMax) return true;                            // Repetition
            double dur = seg.End - seg.Start;
            if (dur > 0 && dur < MiniNoiseDurSec && seg.NoSpeechProb > NoSpeechProbMax) return true; // Mini-Noise
            return false;
        }

        /// <summary>Minimaler, abhaengigkeitsfreier Fallback: top-level "text" aus dem JSON ziehen.</summary>
        private static string ExtractFallbackText(string json)
        {
            try
            {
                using var doc = JsonDocument.Parse(json);
                if (doc.RootElement.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                    return (t.GetString() ?? string.Empty).Trim();
            }
            catch (JsonException)
            {
                // wirklich unbrauchbar -> nichts Verwertbares
            }
            return string.Empty;
        }
    }

    // ---- verbose_json-DTOs (System.Text.Json Source-Generator: reflection-frei, trim-/AOT-fest fuer
    // self-contained publish). Groq liefert snake_case -> explizite [JsonPropertyName]. Unbekannte
    // Felder (id, seek, tokens, x_groq ...) ignoriert STJ automatisch. ----
    internal sealed record GroqVerboseResponse(
        [property: JsonPropertyName("text")]     string? Text,
        [property: JsonPropertyName("segments")] IReadOnlyList<GroqSegment>? Segments);

    internal sealed record GroqSegment(
        [property: JsonPropertyName("text")]              string? Text,
        [property: JsonPropertyName("start")]             double Start,
        [property: JsonPropertyName("end")]               double End,
        [property: JsonPropertyName("no_speech_prob")]    double NoSpeechProb,
        [property: JsonPropertyName("avg_logprob")]       double AvgLogProb,
        [property: JsonPropertyName("compression_ratio")] double CompressionRatio);

    [JsonSourceGenerationOptions(PropertyNameCaseInsensitive = true)]
    [JsonSerializable(typeof(GroqVerboseResponse))]
    internal partial class GroqJsonContext : JsonSerializerContext
    {
    }
}
