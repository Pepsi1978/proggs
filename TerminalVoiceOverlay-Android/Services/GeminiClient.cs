using System.Text;
using System.Text.Json;

namespace TerminalVoiceOverlay.Services;

public sealed class GeminiClient
{
    private readonly string _apiKey;
    private readonly string _model;
    private readonly string _thinkingLevel;
    private readonly HttpClient _http;
    private static readonly int[] RetryableStatusCodes = [429, 500, 503];
    private const int MaxRetries = 5;
    private static readonly int[] DelaysMs = [2000, 4000, 8000, 16000, 32000];

    private const string PromptTemplate = @"ROLLE:
Du bist ein technischer Redakteur, spezialisiert auf die Aufbereitung von Spracheingaben für KI-Coding-Tools. Du verstehst Programmierkonzepte und bewahrst technische Präzision, während du gesprochene Sprache in klare schriftliche Anweisungen umwandelst.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Der Sprecher spricht Deutsch, verwendet aber regelmäßig englische Fachbegriffe aus der Programmierung (Funktionsnamen, Frameworks, CLI-Befehle etc.). Die Spracherkennung kann diese englischen Begriffe falsch transkribieren – erkenne und korrigiere solche Fehler anhand des technischen Kontexts. Der Sprecher gibt Programmier-Anweisungen an ein KI-Coding-Tool (z.B. Claude Code). Bereite den Text so auf, dass er als klare, präzise Eingabe funktioniert.

VORGEHEN:
1) Entferne Diktat-Artefakte: Fülllaute (""äh"", ""ähm""), Stotterer, Wortwiederholungen, sinnlose Fragmente.
2) Erkenne und korrigiere falsch transkribierte englische Fachbegriffe (z.B. ""use Tate"" → ""useState"", ""Fötch"" → ""fetch"").
3) Erkenne die Absicht und formuliere den Text als klare Anweisung um. Sätze dürfen umstrukturiert, Wortwahl verbessert und Satzgrenzen neu gesetzt werden. Zusammengehörige Anweisungen als einen Befehl belassen.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.
5) Bewahre technische Begriffe EXAKT: Dateinamen, Funktionsnamen, Variablen, Programmiersprachen, Frameworks, CLI-Befehle, API-Namen – NICHT übersetzen oder verändern.

GRENZEN:
- Keine neuen Informationen oder Vermutungen hinzufügen.
- Intention des Originals vollständig erhalten.
- Englische Fachbegriffe und Code-Referenzen NIEMALS eindeutschen.
- Sprache: Deutsch (außer technische Begriffe).

AUSGABEFORMAT:
- Ausschließlich den überarbeiteten Text. Keine Kommentare, keine Erklärungen, kein Präfix.
- Ausführliche, vollständige Sätze, sodass jede Intention des Sprechers klar und unmissverständlich beim Leser ankommt.
- Natürlicher, verständlicher Ton – so wie man einem Kollegen etwas erklärt. Kein Behördendeutsch, keine Geschäftsbrief-Floskeln, keine gestelzte Sprache.

TEXT_START
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
        return await SendWithRetry(PromptTemplate + text + "\nTEXT_END", 0);
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
            Android.Util.Log.Warn("VoiceOverlay", $"Gemini {statusCode} - retry {attempt + 1}/{MaxRetries}, waiting {DelaysMs[attempt]}ms...");
            await Task.Delay(DelaysMs[attempt]);
            return await SendWithRetry(prompt, attempt + 1);
        }

        var errorBody = await response.Content.ReadAsStringAsync();
        throw new Exception($"Gemini API error {statusCode}: {errorBody}");
    }

    private static string ExtractText(string responseJson)
    {
        using var doc = JsonDocument.Parse(responseJson);
        var root = doc.RootElement;

        if (!root.TryGetProperty("candidates", out var candidates) ||
            candidates.GetArrayLength() == 0)
            throw new Exception("Unexpected Gemini response: no candidates");

        var contentElem = candidates[0].GetProperty("content");
        var parts = contentElem.GetProperty("parts");

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

        throw new Exception("No text in Gemini response");
    }
}
