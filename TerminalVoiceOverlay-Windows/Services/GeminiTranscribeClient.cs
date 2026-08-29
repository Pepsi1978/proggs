using System;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Sprache-zu-Text ueber Gemini (Modell gemini-3.5-transcribe-live).
    /// Alternative zu <see cref="GroqWhisperClient"/>, umschaltbar in den
    /// Einstellungen.
    ///
    /// WICHTIG — bewusst OHNE Halluzinations-Gate: die dreischichtige
    /// Stille-Halluzinations-Abwehr (Floskel-Liste, Confidence-Gate,
    /// Audio-Abgleich) in GroqWhisperClient haengt an Whispers
    /// verbose_json-Feldern (avg_logprob, no_speech_prob, compression_ratio),
    /// die Gemini gar nicht liefert. Gemini bekommt stattdessen per Prompt
    /// gesagt, dass es bei Stille nichts ausgeben soll; nur die leere Antwort
    /// wird abgefangen.
    /// </summary>
    public sealed class GeminiTranscribeClient
    {
        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _language;

        // Geteilter HttpClient (gleicher Grund wie bei GeminiClient.SharedHttp):
        // pro Voice-Submit wird eine neue Client-Instanz gebaut, ein privater
        // Socket-Pool je Instanz wuerde Ports im TIME_WAIT anhaeufen.
        private static readonly HttpClient SharedHttp = new HttpClient(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        })
        {
            Timeout = TimeSpan.FromSeconds(75)
        };

        private const string TranscribePrompt =
            "Transkribiere die Audioaufnahme wortgetreu. " +
            "Die Sprache ist {LANG}; behalte englische Fachbegriffe aus der Programmierung im Original bei. " +
            "Gib AUSSCHLIESSLICH den gesprochenen Text aus — keine Einleitung, keine Anfuehrungszeichen, " +
            "keine Zeitmarken, keine Sprecherkennzeichnung, keine Kommentare. " +
            "Enthaelt die Aufnahme keine Sprache (Stille, nur Rauschen oder Atem), gib eine leere Antwort aus " +
            "und erfinde auf keinen Fall Floskeln wie \"Vielen Dank\" oder \"Untertitel\".";

        public GeminiTranscribeClient(string apiKey, string model, string language)
        {
            _apiKey = apiKey;
            _model = string.IsNullOrWhiteSpace(model) ? "gemini-3.5-transcribe-live" : model;
            _language = string.IsNullOrWhiteSpace(language) ? "de" : language;
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            var sw = Stopwatch.StartNew();
            byte[] wav = await File.ReadAllBytesAsync(wavFilePath).ConfigureAwait(false);
            DiagLog.Write("GeminiSTT", "http_start", ("model", _model), ("wavBytes", wav.Length));

            var url = $"https://generativelanguage.googleapis.com/v1beta/models/{_model}:generateContent";
            var payload = new
            {
                contents = new[]
                {
                    new
                    {
                        role = "user",
                        parts = new object[]
                        {
                            new { text = TranscribePrompt.Replace("{LANG}", LanguageName(_language)) },
                            new { inline_data = new { mime_type = "audio/wav", data = Convert.ToBase64String(wav) } }
                        }
                    }
                },
                generationConfig = new { maxOutputTokens = 4096, temperature = 0.0 }
            };

            var json = JsonSerializer.Serialize(payload);
            using var request = new HttpRequestMessage(HttpMethod.Post, url)
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
            DiagLog.Perf("GeminiSTT", "http_response", sw, ("status", status));

            if (status != 200)
            {
                DiagLog.Warn("GeminiSTT", "http_error", ("status", status),
                    ("body", body.Length > 500 ? body.Substring(0, 500) + "…" : body));
                if (status == 404)
                    throw new Exception(
                        $"Gemini-Transkription: Modell \"{_model}\" antwortet nicht auf generateContent " +
                        "(404). In den Einstellungen zurueck auf Groq Whisper stellen oder " +
                        "GEMINI_TRANSCRIBE_MODEL in der .env auf ein Modell mit Audio-Eingang aendern.");
                throw new Exception($"Gemini-Transkription Fehler {status}: {body}");
            }

            var text = ExtractText(body);
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

        private static string LanguageName(string code) => code.ToLowerInvariant() switch
        {
            "de" => "Deutsch",
            "en" => "Englisch",
            _ => code,
        };

        /// <summary>
        /// Text aus der generateContent-Antwort ziehen. Anders als bei der
        /// Textkorrektur ist eine leere Antwort hier KEIN Fehler, sondern das
        /// gewuenschte Ergebnis fuer eine stille Aufnahme — der Aufrufer
        /// behandelt den leeren String wie bisher bei Groq.
        /// </summary>
        private static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            var root = doc.RootElement;

            if (root.TryGetProperty("promptFeedback", out var feedback) &&
                feedback.TryGetProperty("blockReason", out var reason) &&
                reason.ValueKind == JsonValueKind.String &&
                !string.Equals(reason.GetString(), "BLOCK_REASON_UNSPECIFIED", StringComparison.Ordinal))
            {
                throw new Exception($"Gemini blockierte die Aufnahme ({reason.GetString()})");
            }

            if (!root.TryGetProperty("candidates", out var candidates) || candidates.GetArrayLength() == 0)
                return string.Empty;

            var candidate = candidates[0];
            if (!candidate.TryGetProperty("content", out var content) ||
                !content.TryGetProperty("parts", out var parts))
                return string.Empty;

            var result = new StringBuilder();
            foreach (var part in parts.EnumerateArray())
            {
                if (part.TryGetProperty("thought", out var thought) && thought.ValueKind == JsonValueKind.True)
                    continue;
                if (part.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                    result.Append(t.GetString());
            }
            return result.ToString().Trim();
        }
    }
}
