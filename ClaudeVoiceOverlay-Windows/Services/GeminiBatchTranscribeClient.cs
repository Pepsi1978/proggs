using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Sprache-zu-Text ueber Gemini 3.5 Transcribe (Modell
    /// <c>gemini-3.5-transcribe</c>) — der von Google fuer FERTIGE Aufnahmen
    /// vorgesehene Weg, im Gegensatz zur Streaming-Schwester
    /// <see cref="GeminiTranscribeClient"/>.
    ///
    /// WARUM DIESER WEG DER BESSERE IST (recherchiert und nachgemessen):
    ///   - Google verweist im Live-Guide ausdruecklich hierher: "Read the
    ///     Gemini Transcribe documentation for non-streaming audio files."
    ///     Das Overlay hat beim Transkribieren immer eine fertige Datei.
    ///   - Genauer: 2,6 % Wortfehlerrate gegenueber 4,0 % bei der
    ///     Live-Variante (und 4,6 % bei Groq Whisper large-v3-turbo).
    ///   - Schneller: an derselben 64,5-s-Aufnahme gemessen 4,4 s statt 15,1 s.
    ///   - Einfacher: ein HTTPS-Aufruf statt WebSocket, damit entfallen
    ///     Sprechpausen-Erkennung, Aktivitaets-Markierungen und das
    ///     Zusammensetzen von Teilstuecken — genau die Stellen, an denen die
    ///     Live-Variante Text verschluckt hat.
    ///
    /// VERBATIM ODER SMART: umschaltbar in den Einstellungen, siehe
    /// <see cref="TranscriptionModeSetting"/>. Voreinstellung ist verbatim,
    /// weil smart zwar Fuellwoerter raeumt und Absaetze setzt, dabei aber
    /// umformuliert und WOERTER WEGLAESST — in jedem Messdurchgang wurde aus
    /// "Ich frage mich" ein "Frage mich". Schneller ist smart nicht (Median
    /// 4,6 s gegen 6,1 s, im Rauschen der Serverlast). Fuellwoerter raeumt
    /// anschliessend ohnehin die Gemini-Textkorrektur weg.
    ///
    /// AUDIO GEHT INLINE, NICHT UEBER DIE FILES-API: beides funktioniert, aber
    /// inline spart den zweiten Roundtrip (gemessen 4,4 s statt 5,7 s) und vor
    /// allem landen die Diktate so NICHT als Dateien auf Google-Servern, wo sie
    /// sonst 48 h liegen blieben.
    ///
    /// BEWUSST OHNE HALLUZINATIONS-GATE: die dreischichtige Abwehr gegen
    /// Whisper-Stille-Halluzinationen in <see cref="GroqWhisperClient"/> haengt
    /// an Whispers verbose_json-Feldern, die Gemini nicht liefert. Bleibt die
    /// Antwort leer, wird wie beim Groq-Stille-Schutz geworfen, damit nichts
    /// getippt wird.
    /// </summary>
    public sealed class GeminiBatchTranscribeClient
    {
        private readonly string _apiKey;
        private readonly string _model;

        private const string Endpoint = "https://generativelanguage.googleapis.com/v1beta/interactions";

        // Geteilter HttpClient (gleicher Grund wie bei GeminiClient.SharedHttp):
        // pro Aufnahme wird eine neue Client-Instanz gebaut, ein privater
        // Socket-Pool je Instanz wuerde Ports im TIME_WAIT anhaeufen.
        private static readonly HttpClient SharedHttp = new HttpClient(
            ResilientHttp.CreateHandler(TimeSpan.FromMinutes(5)))
        {
            // WARUM 30 s UND NICHT MEHR (Vorfall 30.08.2026): gemessen
            // braucht dieser Aufruf 3,5-4,7 s, auch bei 1,75 MB Audio — die
            // Antwortzeit haengt am Modell, kaum an der Laenge der Aufnahme.
            // Die frueheren 120 s waren daher kein Puffer, sondern eine
            // Wartehalle: bei einer haengenden Verbindung stand der Knopf zwei
            // Minuten auf orange, bevor der Groq-Weg ueberhaupt anlief. 30 s
            // sind das Sechsfache der Messung und lassen echten Serverstau
            // durch, brechen aber eine tote Verbindung fruehzeitig ab —
            // zusammen mit dem Groq-Fallback (~1 s) ist der schlimmste Fall
            // damit halbe Minute statt zwei Minuten.
            Timeout = TimeSpan.FromSeconds(30)
        };

        public GeminiBatchTranscribeClient(string apiKey, string model)
        {
            _apiKey = apiKey;
            _model = string.IsNullOrWhiteSpace(model) ? "gemini-3.5-transcribe" : model;
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            var sw = Stopwatch.StartNew();
            var wav = await File.ReadAllBytesAsync(wavFilePath).ConfigureAwait(false);
            var vocabulary = PersonalVocabulary.Load();
            DiagLog.Write("GeminiBatch", "start", ("model", _model), ("wavBytes", wav.Length),
                ("vocabWords", vocabulary.Length), ("mode", TranscriptionModeSetting.Current));

            var mode = TranscriptionModeSetting.Current;
            var transcriptionConfig = new Dictionary<string, object>
            {
                // Verbatim (Standard) = wortgetreu; smart = aufgeraeumt, aber
                // laesst Woerter weg. Umschaltbar in den Einstellungen.
                ["mode"] = new { type = mode },
            };
            if (vocabulary.Length > 0)
                transcriptionConfig["custom_vocabulary"] = vocabulary;

            var payload = new
            {
                model = _model,
                input = new object[]
                {
                    new
                    {
                        type = "audio",
                        mime_type = "audio/wav",
                        data = Convert.ToBase64String(wav),
                    }
                },
                generation_config = new { transcription_config = transcriptionConfig },
            };

            var json = JsonSerializer.Serialize(payload);
            using var request = new HttpRequestMessage(HttpMethod.Post, Endpoint)
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            };
            request.Headers.Add("x-goog-api-key", _apiKey);

            int status;
            string body;
            using (var response = await SharedHttp.SendAsync(request).ConfigureAwait(false))
            {
                status = (int)response.StatusCode;
                body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
            }
            DiagLog.Perf("GeminiBatch", "http_response", sw, ("status", status));

            if (status != 200)
            {
                DiagLog.Warn("GeminiBatch", "http_error", ("status", status),
                    ("body", body.Length > 500 ? body.Substring(0, 500) + "…" : body));
                if (status == 429)
                    throw new Exception(
                        "Gemini-Kontingent erschoepft (429) — das Overlay weicht auf Groq Whisper aus.");
                throw new Exception($"Gemini-Transkription Fehler {status}: {body}");
            }

            // ExtractText throws a plain Exception for an unexpected response shape
            // (-> Groq fallback in SpeechToTextRouter). NoSpeech only when the
            // structure is valid but carries no text.
            var text = ExtractText(body);
            if (string.IsNullOrWhiteSpace(text))
            {
                DiagLog.Warn("GeminiBatch", "empty_result");
                throw new NoSpeechException("Aufnahme ohne erkennbaren Sprachinhalt (Gemini gab keinen Text zurueck)");
            }

            DiagLog.Perf("GeminiBatch", "done", sw, ("chars", text.Length));
            return text;
        }

        /// <summary>
        /// Der Text steht in <c>steps[].content[].text</c>. Ein
        /// <c>output_text</c>-Feld gibt es nur in den SDKs, nicht in der
        /// REST-Antwort — dort muss der Baum selbst durchlaufen werden.
        /// Tolerant also accepts an <c>outputs[]</c> array (older/alternative
        /// Interactions response shape). Throws a plain Exception (technical
        /// failure -> Groq fallback) when the interaction did not complete or
        /// neither <c>steps</c> nor <c>outputs</c> is present; returns an empty
        /// string only for a valid but text-less response (= no speech).
        /// </summary>
        private static string ExtractText(string responseJson)
        {
            JsonDocument doc;
            try { doc = JsonDocument.Parse(responseJson); }
            catch (JsonException ex)
            {
                throw new Exception("Gemini-Transkription: Antwort ist kein gueltiges JSON — " + ex.Message);
            }

            using (doc)
            {
                var root = doc.RootElement;
                if (root.ValueKind != JsonValueKind.Object)
                    throw new Exception("Gemini-Transkription: unerwartete Antwortstruktur (kein JSON-Objekt)");

                if (root.TryGetProperty("error", out var error))
                    throw new Exception("Gemini-Transkription: " + error);

                if (root.TryGetProperty("status", out var status) &&
                    status.ValueKind == JsonValueKind.String &&
                    !string.Equals(status.GetString(), "completed", StringComparison.OrdinalIgnoreCase))
                {
                    DiagLog.Warn("GeminiBatch", "unexpected_status", ("status", status.GetString() ?? ""));
                    throw new Exception($"Gemini-Transkription nicht abgeschlossen (status: {status.GetString()})");
                }

                bool hasSteps = root.TryGetProperty("steps", out var steps) && steps.ValueKind == JsonValueKind.Array;
                bool hasOutputs = root.TryGetProperty("outputs", out var outputs) && outputs.ValueKind == JsonValueKind.Array;
                if (!hasSteps && !hasOutputs)
                {
                    DiagLog.Warn("GeminiBatch", "unexpected_structure",
                        ("preview", responseJson.Length > 300 ? responseJson.Substring(0, 300) + "…" : responseJson));
                    throw new Exception("Gemini-Transkription: unerwartete Antwortstruktur (weder steps noch outputs)");
                }

                var result = new StringBuilder();
                if (hasSteps) AppendTexts(steps, result);
                if (hasOutputs) AppendTexts(outputs, result);
                return result.ToString().Trim();
            }
        }

        /// <summary>
        /// Collects text parts from <c>steps[]</c>/<c>outputs[]</c>: each element is
        /// either a text part itself (<c>{type:"text", text}</c>) or carries a
        /// <c>content[]</c> array of such parts. Non-text and thought parts are skipped.
        /// </summary>
        private static void AppendTexts(JsonElement items, StringBuilder result)
        {
            foreach (var item in items.EnumerateArray())
            {
                if (item.ValueKind != JsonValueKind.Object) continue;

                if (item.TryGetProperty("content", out var content) && content.ValueKind == JsonValueKind.Array)
                {
                    foreach (var part in content.EnumerateArray())
                        AppendPart(part, result);
                }
                else
                {
                    AppendPart(item, result);
                }
            }
        }

        private static void AppendPart(JsonElement part, StringBuilder result)
        {
            if (part.ValueKind != JsonValueKind.Object) return;
            if (part.TryGetProperty("type", out var type) &&
                type.ValueKind == JsonValueKind.String &&
                type.GetString() != "text")
                return;
            if (part.TryGetProperty("thought", out var thought) && thought.ValueKind == JsonValueKind.True)
                return;

            if (part.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
            {
                if (result.Length > 0) result.Append(' ');
                result.Append(t.GetString());
            }
        }
    }
}
