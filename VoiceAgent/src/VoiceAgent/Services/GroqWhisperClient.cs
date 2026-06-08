using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Groq Whisper Speech-to-Text. Uebernommen aus dem bewaehrten TerminalVoiceOverlay-
    /// GroqWhisperClient (statischer HttpClient, Retry mit Backoff), ergaenzt um
    /// ConfigureAwait(false), CancellationToken und Diagnose-Logging.
    ///
    /// Schicht 2 gegen die Whisper-Stille-Halluzination (Almanach groq-transkription §2.3):
    /// fordert <c>response_format=verbose_json</c> an (bei Groq OHNE Mehrlatenz/-kosten vs. text)
    /// und filtert pro Segment ueber die Qualitaetsfelder <c>no_speech_prob</c>,
    /// <c>avg_logprob</c>, <c>compression_ratio</c>. Verwerfen mit UND-Logik (schuetzt echte
    /// leise Sprache). Bleibt nach dem Filtern nichts uebrig, wird ein LEERER String geliefert —
    /// im Always-On-Betrieb normal; der Aufrufer (MainWindow) behandelt das bereits korrekt.
    /// </summary>
    public sealed class GroqWhisperClient
    {
        public const string DefaultUrl = "https://api.groq.com/openai/v1/audio/transcriptions";

        // ----- Confidence-Gate-Schwellen (Almanach groq-transkription §2.3) -----
        // UND-Logik (NoSpeechProb UND AvgLogProb) schuetzt leise Sprache: ein leiser, aber echter
        // Satz hat zwar evtl. erhoehtes NoSpeechProb, aber ein nicht zu negatives AvgLogProb -> bleibt.
        // Zu aggressiv? ZUERST NoSpeechProbMax auf 0.7 anheben, NIE AvgLogProbMin lockern.
        private const double NoSpeechProbMax     = 0.6;   // darueber: hohe "keine Sprache"-Wahrscheinlichkeit
        private const double AvgLogProbMin       = -1.0;  // darunter: sehr unsicheres Segment
        private const double CompressionRatioMax = 2.4;   // darueber: Wiederholungs-Halluzination ("danke danke danke")
        private const double MiniNoiseDurSec     = 0.4;   // sehr kurzes Segment + Stille-Verdacht = Mini-Noise

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
                // verbose_json statt text: liefert die Confidence-Felder fuers Gate, bei Groq ohne
                // Mehrlatenz/-kosten (nur word-Timestamps kosten extra — die fordern wir NICHT an).
                { new StringContent("verbose_json"), "response_format" },
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
                var json = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                // Confidence-Gate anwenden. Ergebnis kann leer sein (alles halluziniert/Stille) — das ist
                // im Always-On-Betrieb NORMAL und KEIN Fehler (Almanach §4.4): leerer String statt Exception.
                return FilterTranscription(json);
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

        /// <summary>
        /// Wendet das Confidence-Gate (Almanach §2.3) auf die verbose_json-Antwort an und gibt die
        /// verbleibenden Segment-Texte zusammengefuegt zurueck. Funktionserhaltend: faellt bei
        /// unerwartetem Format auf den Roh-Text zurueck (nie verlieren), und ohne Segment-Metadaten
        /// wird der top-level Text ungefiltert durchgelassen.
        /// </summary>
        private static string FilterTranscription(string json)
        {
            GroqVerboseResponse? parsed;
            try
            {
                parsed = JsonSerializer.Deserialize(json, GroqJsonContext.Default.GroqVerboseResponse);
            }
            catch (JsonException ex)
            {
                // Unerwartetes Format -> NICHT verlieren: top-level "text" als Fallback ziehen.
                Log.Warn("Groq: verbose_json nicht parsebar — Rohtext-Fallback", ctx: new { error = ex.Message });
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
                    // Observability (§8): jede Verwerfung mit ihren Qualitaetswerten nachvollziehbar machen.
                    Probe.Decision("groq-segment", "verworfen", new
                    {
                        text = seg.Text?.Trim(),
                        no_speech_prob = seg.NoSpeechProb,
                        avg_logprob = seg.AvgLogProb,
                        compression_ratio = seg.CompressionRatio,
                        dur = seg.End - seg.Start
                    });
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

            string result = sb.ToString().Trim();
            // Observability (§8): Qualitaets-Ueberblick pro Transkription strukturiert als JSON-Lines loggen.
            Log.Info("Groq verbose_json gefiltert", ctx: new
            {
                segments = parsed.Segments.Count,
                kept,
                dropped,
                resultEmpty = result.Length == 0
            });
            return result;
        }

        /// <summary>
        /// Halluzinations-Heuristik fuer EIN Segment (Almanach §2.3). Reihenfolge der Regeln egal,
        /// alle sind ODER-verknuepft; die Stille-Regel selbst ist intern UND-verknuepft.
        /// </summary>
        private static bool IsHallucination(GroqSegment seg)
        {
            // Stille-Halluzination: UND-Logik — schuetzt echte leise Sprache (siehe Schwellen-Doku oben).
            if (seg.NoSpeechProb > NoSpeechProbMax && seg.AvgLogProb < AvgLogProbMin) return true;
            // Wiederholungs-Halluzination ("danke danke danke ...").
            if (seg.CompressionRatio > CompressionRatioMax) return true;
            // Mini-Noise: sehr kurzes Segment mit hohem Stille-Verdacht.
            double dur = seg.End - seg.Start;
            if (dur > 0 && dur < MiniNoiseDurSec && seg.NoSpeechProb > NoSpeechProbMax) return true;
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
                // wirklich unbrauchbar -> nichts Verwertbares (still verwerfen, kein Fehler im Always-On)
            }
            return string.Empty;
        }
    }

    // ---- verbose_json-DTOs ----
    // System.Text.Json Source-Generator: reflection-frei, trim-/AOT-fest. Wichtig, weil publish.ps1
    // self-contained baut — reflexionsbasiertes JSON koennte beim Trimming still brechen.
    // Groq liefert snake_case -> pro Property explizites [JsonPropertyName]. Unbekannte Felder
    // (id, seek, tokens, x_groq ...) ignoriert STJ automatisch.
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
