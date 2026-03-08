using System;
using System.Diagnostics;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    public sealed class GeminiClient
    {
        private readonly string _apiKey;
        private readonly string _model;
        private readonly string _thinkingLevel;
        private readonly HttpClient _http;
        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 5;
        private static readonly int[] DelaysMs = { 2000, 4000, 8000, 16000, 32000 };

        private const string PromptTemplate = @"Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute (""äh"", ""ähm"", ""öhm""), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
   - Sätze dürfen umstrukturiert werden.
   - Wortwahl darf verbessert werden.
   - Satzgrenzen dürfen neu gesetzt werden.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
- Keine Vermutungen über nicht Gesagtes.
- Die Intention des Originals muss vollständig erhalten bleiben.
- Sprache: Deutsch.

REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

TEXT:
";

        public GeminiClient(string apiKey, string model, string thinkingLevel)
        {
            _apiKey = apiKey;
            _model = model;
            _thinkingLevel = thinkingLevel;
            _http = new HttpClient { Timeout = TimeSpan.FromSeconds(120) };
        }

        public async Task<string> CorrectTextAsync(string text)
        {
            return await SendWithRetry(PromptTemplate + text, 0);
        }

        private async Task<string> SendWithRetry(string prompt, int attempt)
        {
            var url = $"https://generativelanguage.googleapis.com/v1beta/models/{_model}:generateContent?key={_apiKey}";

            var payload = new
            {
                contents = new[]
                {
                    new
                    {
                        role = "user",
                        parts = new[] { new { text = prompt } }
                    }
                },
                generationConfig = new
                {
                    maxOutputTokens = 8192,
                    thinkingConfig = new { thinkingLevel = _thinkingLevel }
                }
            };

            var json = JsonSerializer.Serialize(payload);
            using var content = new StringContent(json, Encoding.UTF8, "application/json");
            var response = await _http.PostAsync(url, content);
            var statusCode = (int)response.StatusCode;

            if (response.IsSuccessStatusCode)
            {
                var body = await response.Content.ReadAsStringAsync();
                return ExtractText(body);
            }

            if (Array.IndexOf(RetryableStatusCodes, statusCode) >= 0 && attempt < MaxRetries)
            {
                Debug.WriteLine($"Gemini {statusCode} - Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms...");
                await Task.Delay(DelaysMs[attempt]);
                return await SendWithRetry(prompt, attempt + 1);
            }

            var errorBody = await response.Content.ReadAsStringAsync();
            throw new Exception($"Gemini API Fehler {statusCode}: {errorBody}");
        }

        private static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            var root = doc.RootElement;

            if (!root.TryGetProperty("candidates", out var candidates) ||
                candidates.GetArrayLength() == 0)
                throw new Exception("Unerwartete Gemini-Antwort: keine Kandidaten");

            var content = candidates[0].GetProperty("content");
            var parts = content.GetProperty("parts");

            foreach (var part in parts.EnumerateArray())
            {
                // Skip thinking parts
                if (part.TryGetProperty("thought", out var thought) && thought.GetBoolean())
                    continue;

                if (part.TryGetProperty("text", out var textElem))
                {
                    var text = textElem.GetString()?.Trim();
                    if (!string.IsNullOrEmpty(text))
                        return text;
                }
            }

            throw new Exception("Kein Text in Gemini-Antwort");
        }
    }
}
