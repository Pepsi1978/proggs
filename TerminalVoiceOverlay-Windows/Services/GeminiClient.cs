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

        // Prompt-Engineer-Template fuer den PromptBoard-Edit-Dialog (G-Button).
        // Wandelt einen roh-transkribierten Whisper-Text in einen kopierfertigen
        // Claude-Code-CLI-Prompt um. Bewusst getrennt vom Diktat-Cleanup-Template
        // oben — das Overlay-Diktat soll weiter den lockeren Cleanup nutzen,
        // der PromptBoard-Editor will dagegen einen strukturierten Prompt.
        private const string ClaudeCodePromptEngineerTemplate = @"Du bist ein international anerkannter Prompt-Engineer mit Spezialisierung auf Programmier-Prompts für Claude Code in der CLI-Umgebung. Deine einzige Aufgabe ist es, einen per Whisper eingesprochenen, roh transkribierten Text in einen präzisen, sofort im Claude-Code-CLI-Fenster einsetzbaren Prompt umzuwandeln.

Vorgehen:

1. Erfasse die programmiertechnische Absicht des Textes: Was soll gebaut, geändert, debuggt oder refaktoriert werden? Welche Dateien, Sprachen, Frameworks, Pfade, Tools sind gemeint?

2. Korrigiere Whisper-Transkriptionsfehler nicht nur nach allgemeiner Sprachlogik, sondern explizit nach Programmier-Logik. Wenn ein Wort im Code-Kontext sinnlos wirkt, prüfe phonetisch, ob ein Programmier-Begriff gemeint ist. Typische Whisper-Verwechslungen, an denen du dich orientierst:
   - ""Brunch"" → branch
   - ""Mörsch"" / ""März"" → merge
   - ""Komitee"" → commit
   - ""Reposi-Tory"" / ""Repo-Story"" → repository / repo
   - ""Busch"" → push
   - ""Pull-Anforderung"" / ""Pullrikwest"" → pull request
   - ""A-Sync"" / ""Async-hron"" → async
   - ""Funkschon"" → function
   - ""Wert"" / ""Lett"" → var / let / const
   - ""Klosure"" → closure
   - ""Daiwa"" / ""Daiver"" → diff
   - ""Cäsch"" → cache
   - ""Kju"" → queue
   - ""Schell"" → shell
   - ""Endpoint"" / ""End-Punkt"" → endpoint
   - Tool-/Library-Namen wie npm, pnpm, Vite, Docker, Tampermonkey, ESLint, Prettier, Jest, Vitest, FastAPI, Tailwind etc. phonetisch erkennen.
   Bei mehrdeutigen Stellen wähle die im Kontext plausibelste Programmier-Bedeutung.

3. Strukturiere den fertigen Prompt programmierfreundlich für Claude Code:
   - Klare Aufgabe im Imperativ (""Implementiere…"", ""Refaktoriere…"", ""Debugge…"", ""Erstelle…"")
   - Sofern erkennbar: Dateipfade, Funktions-/Modulnamen, Sprache, Framework, Versionen
   - Akzeptanzkriterien: Was muss am Ende funktionieren oder getestet sein?
   - Wenn sinnvoll: Hinweis auf bestehende Konventionen / Code-Stil beibehalten
   - Wenn die Aufgabe komplex ist: explizit ""Erstelle erst einen Plan, dann implementiere"" oder ""Schreibe Tests"" ergänzen
   - Edge-Cases nennen, wenn sie aus dem Original ableitbar sind

4. Sprache des Output-Prompts: Deutsch. Technische Fachbegriffe (function, branch, commit, async, hook, endpoint, …) bleiben Englisch.

5. Länge: so kurz wie möglich, so lang wie nötig. Keine Floskeln, kein Vorgeplänkel, keine Meta-Kommentare über deine Arbeit.

Output-Format (exakt so):

[Der fertige, kopierfertige Claude-Code-CLI-Prompt – direkt loslegen, kein ""Hier ist…""]

Annahmen (nur falls vorhanden, sonst diesen Block komplett weglassen):
- [phonetisch oder inhaltlich getroffene Annahme 1]
- [phonetisch oder inhaltlich getroffene Annahme 2]

Gib ausschließlich den umgewandelten Prompt (plus optional den Annahmen-Block) zurück. Keine Einleitung, keine Bestätigung, keine Erklärung deiner Arbeit.

Der zu verarbeitende Whisper-Text folgt nun:
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

        /// <summary>
        /// Wandelt einen roh-transkribierten Whisper-Text in einen
        /// kopierfertigen Claude-Code-CLI-Prompt um. Wird vom PromptBoard-
        /// Edit-Dialog beim Klick auf "G" verwendet — getrennt vom Overlay-
        /// Diktat-Cleanup, damit beide Pfade unabhaengig optimierbar bleiben.
        /// </summary>
        public async Task<string> BuildClaudeCodePromptAsync(string rawWhisperText)
        {
            return await SendWithRetry(ClaudeCodePromptEngineerTemplate + rawWhisperText, 0);
        }

        /// <summary>
        /// Erzeugt einen kompakten Titel von maximal 4 Woertern auf Deutsch
        /// fuer einen gegebenen Prompt. Wird fuer die Prompt-Historie
        /// verwendet, damit der Benutzer die Eintraege auf einen Blick
        /// wiedererkennen kann. Fallback auf die ersten 4 Woerter des Texts
        /// wenn die Gemini-Antwort leer oder ungueltig ist — die Historie
        /// darf nie wegen einer fehlgeschlagenen Titel-Generierung blockieren.
        /// </summary>
        public async Task<string> GenerateTitleAsync(string text)
        {
            string trimmed = (text ?? string.Empty).Trim();
            if (trimmed.Length == 0) return string.Empty;

            const string titlePrompt =
                "Erstelle einen sehr kompakten Titel auf Deutsch fuer den folgenden " +
                "Prompt. STRENGE REGELN: Maximal 4 Woerter. Keine Anfuehrungszeichen. " +
                "Kein Punkt am Ende. Keine Aufzaehlungen, kein Praefix wie 'Titel:'. " +
                "Nur die nackten 1-4 Woerter zurueckgeben.\n\nPROMPT:\n";
            try
            {
                string raw = await SendWithRetry(titlePrompt + trimmed, 0);
                string sanitized = SanitizeTitle(raw, trimmed);
                LogTitle($"OK raw=[{raw.Replace('\n', ' ')}] → [{sanitized}]");
                return sanitized;
            }
            catch (Exception ex)
            {
                // Bei jedem Fehler still auf den lokalen Fallback ausweichen,
                // ABER den Fehler in eine Log-Datei schreiben damit der
                // Benutzer (und ich beim Debuggen) den Grund sehen kann.
                LogTitle($"FAIL {ex.GetType().Name}: {ex.Message}");
                return FallbackTitleFromText(trimmed);
            }
        }

        /// <summary>
        /// Schreibt eine Diagnose-Zeile in title-debug.log neben der
        /// Promptboard-Datenbank. Hilft beim Debuggen warum die Historie-
        /// Titel manchmal nicht von Gemini kommen — Append-only, atomar
        /// pro Zeile, schluckt selbst alle Fehler still.
        /// </summary>
        private static void LogTitle(string line)
        {
            try
            {
                string dir = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "PromptBoard", "history");
                System.IO.Directory.CreateDirectory(dir);
                string path = System.IO.Path.Combine(dir, "title-debug.log");
                string ts = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                System.IO.File.AppendAllText(path, $"{ts}  {line}\n",
                    System.Text.Encoding.UTF8);
            }
            catch { /* Diagnostics must never break the main flow. */ }
        }

        private static string SanitizeTitle(string raw, string fallbackSource)
        {
            string s = (raw ?? string.Empty).Trim().Trim('"', '\'', '“', '”', '‚', '‘');
            if (s.EndsWith(".")) s = s.Substring(0, s.Length - 1).Trim();
            // Auf maximal 4 Woerter clampen.
            var words = s.Split(new[] { ' ', '\t', '\n', '\r' },
                                StringSplitOptions.RemoveEmptyEntries);
            if (words.Length == 0) return FallbackTitleFromText(fallbackSource);
            if (words.Length > 4) words = words[..4];
            return string.Join(" ", words);
        }

        internal static string FallbackTitleFromText(string text)
        {
            var words = (text ?? string.Empty)
                .Split(new[] { ' ', '\t', '\n', '\r' },
                       StringSplitOptions.RemoveEmptyEntries);
            if (words.Length == 0) return "Ohne Titel";
            if (words.Length > 4) words = words[..4];
            return string.Join(" ", words);
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
                Console.WriteLine($"Gemini {statusCode} - Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms...");
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
