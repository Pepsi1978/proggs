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

        // ----- Schicht 3: Segment-Audio-Abgleich (Almanach groq-transkription §2.3, "zweite Luecke") -----
        // Trailing-/Pausen-Halluzination ("Ja" am Ende, nach echtem Satz) kommt mit HOHER Confidence —
        // Schicht 2 faengt sie nicht. Loesung: jedes Segment gegen die echte Audio-Lautstaerke pruefen.
        // Ein Segment, dessen Zeitfenster im PCM praktisch still war, ist eine Halluzination.
        private const int    FrameMs         = 20;     // RMS-Fenster
        private const double SegVoicedRatio  = 0.10;   // < 10% laute Frames im Segment-Fenster = still
        private const int    SampleRate      = 16000;  // Aufnahme ist 16 kHz mono 16-bit PCM

        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _language;
        private readonly string _url;
        private readonly double _voicedRmsThreshold;   // gleiche Schwelle wie die Stille-Erkennung (Schicht 1)

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(180) };

        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 3;
        private static readonly int[] DelaysMs = { 2000, 4000, 8000 };

        public GroqWhisperClient(string apiKey, string model, string language, string? url = null,
                                 double voicedRmsThreshold = 0.015)
        {
            _apiKey = apiKey;
            _model = model;
            _language = language;
            _url = string.IsNullOrWhiteSpace(url) ? DefaultUrl : url!;
            _voicedRmsThreshold = voicedRmsThreshold > 0 ? voicedRmsThreshold : 0.015;
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
                // Schicht 3: Voiced-Timeline aus dem aufgenommenen PCM bauen (16 kHz mono 16-bit WAV),
                // damit FilterTranscription jedes Segment gegen die echte Lautstaerke abgleichen kann.
                bool[]? voiced = BuildVoicedTimeline(fileBytes);
                // Confidence-Gate + Audio-Abgleich anwenden. Ergebnis kann leer sein (alles halluziniert/
                // Stille) — im Always-On-Betrieb NORMAL und KEIN Fehler (Almanach §4.4): leerer String.
                return FilterTranscription(json, voiced, FrameMs);
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
        private static string FilterTranscription(string json, bool[]? voiced, int frameMs)
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

            // Schicht 2: Confidence-Gate. Halluzinationen mit hoher "keine Sprache"-Wahrscheinlichkeit raus.
            var afterConfidence = new List<GroqSegment>();
            int droppedConfidence = 0;
            foreach (var seg in parsed.Segments)
            {
                if (IsHallucination(seg))
                {
                    droppedConfidence++;
                    Probe.Decision("groq-segment", "verworfen (Confidence)", new
                    {
                        text = seg.Text?.Trim(),
                        no_speech_prob = seg.NoSpeechProb,
                        avg_logprob = seg.AvgLogProb,
                        compression_ratio = seg.CompressionRatio,
                        dur = seg.End - seg.Start
                    });
                    continue;
                }
                afterConfidence.Add(seg);
            }

            // Schicht 3: Audio-Abgleich. F: Trailing-/Pausen-Halluzination ("Ja") mit HOHER Confidence,
            // die Schicht 2 ueberlebt — verwerfen, wenn ihr Zeitfenster im echten Audio still war.
            var kept = new List<GroqSegment>();
            int droppedAudio = 0;
            if (voiced != null)
            {
                foreach (var seg in afterConfidence)
                {
                    if (!SegmentHasSpeech(seg, voiced, frameMs))
                    {
                        droppedAudio++;
                        Probe.Decision("groq-segment", "verworfen (Audio still)", new
                        {
                            text = seg.Text?.Trim(),
                            start = seg.Start,
                            end = seg.End
                        });
                        continue;
                    }
                    kept.Add(seg);
                }
            }
            else
            {
                kept.AddRange(afterConfidence);
            }

            // Drift-Sicherung (funktionserhaltend): Wuerde der Audio-Abgleich ALLE Segmente verwerfen,
            // obwohl das Confidence-Gate noch welche liess, ist vermutlich das Whisper-Zeitstempel-
            // Alignment verschoben — dann lieber die Confidence-gefilterten behalten als den Satz verlieren.
            var finalSegs = kept;
            if (kept.Count == 0 && afterConfidence.Count > 0 && voiced != null)
            {
                Log.Warn("Groq: Audio-Abgleich (Schicht 3) verwarf alle Segmente -> Fallback auf Confidence-gefilterte (Zeitstempel-Drift?)");
                finalSegs = afterConfidence;
            }

            var sb = new StringBuilder();
            foreach (var seg in finalSegs)
            {
                var t = seg.Text?.Trim();
                if (string.IsNullOrEmpty(t)) continue;
                if (sb.Length > 0) sb.Append(' ');
                sb.Append(t);
            }

            string result = sb.ToString().Trim();
            // Observability (§8): Qualitaets-Ueberblick pro Transkription strukturiert als JSON-Lines loggen.
            Log.Info("Groq verbose_json gefiltert", ctx: new
            {
                segments = parsed.Segments.Count,
                kept = finalSegs.Count,
                droppedConfidence,
                droppedAudio,
                resultEmpty = result.Length == 0
            });
            return result;
        }

        /// <summary>
        /// Baut aus den WAV-Bytes (16 kHz mono 16-bit PCM) eine Voiced-Timeline: pro 20-ms-Frame
        /// true, wenn der RMS-Pegel ueber der Stille-Schwelle liegt. Robust: findet den data-Chunk,
        /// faellt sonst auf 44-Byte-Header zurueck; bei jedem Fehler null (dann kein Audio-Abgleich).
        /// </summary>
        private bool[]? BuildVoicedTimeline(byte[] wav)
        {
            try
            {
                if (wav == null || wav.Length <= 44) return null;
                // data-Chunk suchen (RIFF/WAVE; es koennen Chunks vor "data" stehen).
                int pos = 12, dataOffset = -1, dataLen = 0;
                while (pos + 8 <= wav.Length)
                {
                    string id = Encoding.ASCII.GetString(wav, pos, 4);
                    int sz = BitConverter.ToInt32(wav, pos + 4);
                    if (id == "data") { dataOffset = pos + 8; dataLen = sz; break; }
                    pos += 8 + sz + (sz & 1);
                }
                if (dataOffset < 0) { dataOffset = 44; dataLen = wav.Length - 44; }
                if (dataLen <= 0 || dataOffset + dataLen > wav.Length) dataLen = wav.Length - dataOffset;
                if (dataLen <= 0) return null;

                int frameSamples = SampleRate * FrameMs / 1000;   // 320
                int frameBytes = frameSamples * 2;
                int frameCount = dataLen / frameBytes;
                if (frameCount <= 0) return null;

                var voiced = new bool[frameCount];
                for (int f = 0; f < frameCount; f++)
                {
                    int baseB = dataOffset + f * frameBytes;
                    double sum = 0;
                    for (int i = 0; i < frameBytes; i += 2)
                    {
                        short s = (short)(wav[baseB + i] | (wav[baseB + i + 1] << 8));
                        double v = s / 32768.0;
                        sum += v * v;
                    }
                    voiced[f] = Math.Sqrt(sum / frameSamples) >= _voicedRmsThreshold;
                }
                return voiced;
            }
            catch (Exception ex)
            {
                Log.Warn("Groq: Voiced-Timeline (Schicht 3) nicht gebaut — Audio-Abgleich uebersprungen", ctx: new { error = ex.Message });
                return null;
            }
        }

        /// <summary>
        /// True, wenn das Zeitfenster [Start,End] eines Segments im echten Audio genug laute Frames
        /// hat (>= SegVoicedRatio). Ungueltige/leere Fenster werden NICHT verworfen (funktionserhaltend).
        /// </summary>
        private static bool SegmentHasSpeech(GroqSegment seg, bool[] voiced, int frameMs)
        {
            if (!(seg.End > seg.Start)) return true;
            int startF = Math.Max(0, (int)(seg.Start * 1000.0 / frameMs));
            int endF = Math.Min(voiced.Length, (int)Math.Ceiling(seg.End * 1000.0 / frameMs));
            if (endF <= startF) return true;
            int v = 0;
            for (int i = startF; i < endF; i++) if (voiced[i]) v++;
            return (double)v / (endF - startF) >= SegVoicedRatio;
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
