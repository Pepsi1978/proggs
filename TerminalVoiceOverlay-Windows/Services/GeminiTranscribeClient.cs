using System;
using System.Diagnostics;
using System.IO;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Sprache-zu-Text ueber Gemini (Modell gemini-3.5-transcribe-live).
    /// Alternative zu <see cref="GroqWhisperClient"/>, umschaltbar in den
    /// Einstellungen.
    ///
    /// WARUM WEBSOCKET UND NICHT REST: das Transcribe-Live-Modell listet als
    /// einzige Methode "bidiGenerateContent" — der normale
    /// generateContent-Endpunkt antwortet mit 404. Also spricht dieser Client
    /// die Live-API: Setup schicken, die fertige Aufnahme als PCM-Bloecke
    /// hineinschieben, audioStreamEnd senden, die Transkript-Fragmente
    /// einsammeln bis der Server den Zug abschliesst.
    ///
    /// BEWUSST OHNE HALLUZINATIONS-GATE: die dreischichtige Abwehr gegen
    /// Whisper-Stille-Halluzinationen in GroqWhisperClient haengt an Whispers
    /// verbose_json-Feldern (avg_logprob, no_speech_prob, compression_ratio),
    /// die Gemini gar nicht liefert. Bleibt die Antwort leer, wird wie beim
    /// Groq-Stille-Schutz geworfen, damit nichts getippt wird.
    /// </summary>
    public sealed class GeminiTranscribeClient
    {
        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _language;

        // Die Live-API nimmt rohes PCM entgegen. Groesse eines Audio-Blocks:
        // 32 KB entspricht bei 16 kHz/16 bit/mono rund einer Sekunde. Kleine
        // Bloecke, weil einzelne Riesen-Frames die Verbindung sonst blockieren.
        private const int ChunkBytes = 32 * 1024;
        private static readonly TimeSpan Deadline = TimeSpan.FromSeconds(75);

        public GeminiTranscribeClient(string apiKey, string model, string language)
        {
            _apiKey = apiKey;
            _model = string.IsNullOrWhiteSpace(model) ? "gemini-3.5-transcribe-live" : model;
            _language = string.IsNullOrWhiteSpace(language) ? "de" : language;
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            var sw = Stopwatch.StartNew();
            var wav = await File.ReadAllBytesAsync(wavFilePath).ConfigureAwait(false);
            var (pcm, sampleRate) = ExtractPcm(wav);
            DiagLog.Write("GeminiSTT", "start", ("model", _model), ("wavBytes", wav.Length),
                ("pcmBytes", pcm.Length), ("rate", sampleRate), ("lang", _language));

            using var cts = new CancellationTokenSource(Deadline);
            var ct = cts.Token;
            using var ws = new ClientWebSocket();
            ws.Options.SetRequestHeader("x-goog-api-key", _apiKey);

            var uri = new Uri("wss://generativelanguage.googleapis.com/ws/" +
                              "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent");

            try
            {
                await ws.ConnectAsync(uri, ct).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                DiagLog.Warn("GeminiSTT", "connect_failed", ("error", ex.Message));
                throw new Exception("Gemini-Transkription: Verbindung fehlgeschlagen — " + ex.Message);
            }

            // 1) Setup. responseModalities TEXT + inputAudioTranscription: das
            //    Modell soll das Gesprochene verschriftlichen, nicht antworten.
            var setup = JsonSerializer.Serialize(new
            {
                setup = new
                {
                    model = "models/" + _model,
                    generationConfig = new { responseModalities = new[] { "TEXT" } },
                    inputAudioTranscription = new { },
                }
            });
            await SendAsync(ws, setup, ct).ConfigureAwait(false);

            // 2) Aufnahme hineinschieben. mimeType nach Live-API-Vorgabe:
            //    16-bit PCM, little endian, mono, Rate aus dem WAV-Header.
            for (int offset = 0; offset < pcm.Length; offset += ChunkBytes)
            {
                var len = Math.Min(ChunkBytes, pcm.Length - offset);
                var chunk = Convert.ToBase64String(pcm, offset, len);
                var msg = JsonSerializer.Serialize(new
                {
                    realtimeInput = new
                    {
                        audio = new { data = chunk, mimeType = "audio/pcm;rate=" + sampleRate }
                    }
                });
                await SendAsync(ws, msg, ct).ConfigureAwait(false);
            }

            // 3) Ende der Aufnahme melden — erst danach schliesst der Server den Zug ab.
            await SendAsync(ws, "{\"realtimeInput\":{\"audioStreamEnd\":true}}", ct).ConfigureAwait(false);

            // 4) Fragmente einsammeln.
            var transcript = new StringBuilder();
            var modelText = new StringBuilder();
            var buffer = new byte[16 * 1024];
            var frame = new StringBuilder();
            bool done = false;

            try
            {
                while (!done && ws.State == WebSocketState.Open)
                {
                    var result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct).ConfigureAwait(false);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        DiagLog.Warn("GeminiSTT", "server_closed",
                            ("status", result.CloseStatus?.ToString() ?? "?"),
                            ("reason", ws.CloseStatusDescription ?? ""));
                        break;
                    }

                    frame.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
                    if (!result.EndOfMessage) continue;

                    var payload = frame.ToString();
                    frame.Clear();
                    done = HandleServerMessage(payload, transcript, modelText);
                }
            }
            catch (OperationCanceledException)
            {
                DiagLog.Warn("GeminiSTT", "timeout", ("seconds", Deadline.TotalSeconds));
                throw new Exception("Gemini-Transkription: Zeitueberschreitung (75 s).");
            }
            finally
            {
                if (ws.State == WebSocketState.Open)
                {
                    try
                    {
                        await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "done", CancellationToken.None)
                            .ConfigureAwait(false);
                    }
                    catch { /* Schliessen darf das Ergebnis nicht kippen */ }
                }
            }

            // inputTranscription ist die Verschriftlichung des Gesprochenen und
            // hat Vorrang; modelTurn-Text ist der Ausweg, falls das Modell den
            // Text als normale Antwort liefert.
            var text = transcript.Length > 0 ? transcript.ToString().Trim() : modelText.ToString().Trim();
            if (string.IsNullOrWhiteSpace(text))
            {
                // Gleiches Verhalten wie der Groq-Stille-Schutz: werfen statt
                // leer zurueckgeben, damit der Aufrufer-catch greift und NICHTS
                // getippt wird (kein einsames " ; ").
                DiagLog.Warn("GeminiSTT", "empty_result");
                throw new Exception("Aufnahme ohne erkennbaren Sprachinhalt (Gemini gab keinen Text zurueck)");
            }

            DiagLog.Perf("GeminiSTT", "done", sw, ("chars", text.Length));
            return text;
        }

        /// <summary>Verarbeitet eine Server-Nachricht; true = Zug beendet.</summary>
        private static bool HandleServerMessage(string payload, StringBuilder transcript, StringBuilder modelText)
        {
            JsonDocument doc;
            try { doc = JsonDocument.Parse(payload); }
            catch
            {
                DiagLog.Warn("GeminiSTT", "unparsable_frame",
                    ("preview", payload.Length > 200 ? payload.Substring(0, 200) : payload));
                return false;
            }

            using (doc)
            {
                var root = doc.RootElement;

                if (root.TryGetProperty("error", out var error))
                    throw new Exception("Gemini-Transkription: " + error);

                if (root.TryGetProperty("setupComplete", out _))
                {
                    DiagLog.Write("GeminiSTT", "setup_complete");
                    return false;
                }

                if (!root.TryGetProperty("serverContent", out var server))
                    return false;

                if (server.TryGetProperty("inputTranscription", out var input) &&
                    input.TryGetProperty("text", out var inText) &&
                    inText.ValueKind == JsonValueKind.String)
                {
                    transcript.Append(inText.GetString());
                }

                if (server.TryGetProperty("modelTurn", out var turn) &&
                    turn.TryGetProperty("parts", out var parts) &&
                    parts.ValueKind == JsonValueKind.Array)
                {
                    foreach (var part in parts.EnumerateArray())
                    {
                        if (part.TryGetProperty("thought", out var thought) && thought.ValueKind == JsonValueKind.True)
                            continue;
                        if (part.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                            modelText.Append(t.GetString());
                    }
                }

                var finished =
                    (server.TryGetProperty("turnComplete", out var tc) && tc.ValueKind == JsonValueKind.True) ||
                    (server.TryGetProperty("generationComplete", out var gc) && gc.ValueKind == JsonValueKind.True);
                return finished;
            }
        }

        private static Task SendAsync(ClientWebSocket ws, string json, CancellationToken ct) =>
            ws.SendAsync(new ArraySegment<byte>(Encoding.UTF8.GetBytes(json)),
                WebSocketMessageType.Text, true, ct);

        /// <summary>
        /// Rohes PCM und die Abtastrate aus einer WAV-Datei ziehen. Der
        /// data-Chunk wird gesucht statt fest 44 Byte abzuschneiden — NAudio
        /// schreibt je nach Format zusaetzliche Chunks (LIST/fact) vor die
        /// Daten, ein fester Versatz wuerde dann Rauschen liefern.
        /// </summary>
        private static (byte[] Pcm, int SampleRate) ExtractPcm(byte[] wav)
        {
            const int fallbackRate = 16000;
            if (wav.Length < 44 ||
                Encoding.ASCII.GetString(wav, 0, 4) != "RIFF" ||
                Encoding.ASCII.GetString(wav, 8, 4) != "WAVE")
            {
                return (wav, fallbackRate);
            }

            int rate = fallbackRate;
            int pos = 12;
            while (pos + 8 <= wav.Length)
            {
                var id = Encoding.ASCII.GetString(wav, pos, 4);
                int size = BitConverter.ToInt32(wav, pos + 4);
                int body = pos + 8;
                if (size < 0 || body + size > wav.Length)
                    size = wav.Length - body;

                if (id == "fmt " && size >= 16)
                    rate = BitConverter.ToInt32(wav, body + 4);
                else if (id == "data")
                {
                    var pcm = new byte[size];
                    Array.Copy(wav, body, pcm, 0, size);
                    return (pcm, rate);
                }

                pos = body + size + (size % 2); // Chunks sind wortweise ausgerichtet
            }

            return (wav, rate);
        }
    }
}
