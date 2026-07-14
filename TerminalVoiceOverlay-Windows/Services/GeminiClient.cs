using System;
using System.Diagnostics;
using System.IO;
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
        // Geteilter HttpClient ueber alle GeminiClient-Instanzen. Pro Voice-
        // Submit baut OverlayWindow eine neue GeminiClient-Instanz (weil der
        // API-Key zentral aus dem PromptBoard-Settings-Dialog frisch gelesen
        // wird). Mit einem privaten HttpClient pro Instanz wuerde jeder
        // Voice-Submit eine neue Socket-Pool-Sitzung anlegen — bei vielen
        // schnellen Aufnahmen drohen ausgehende TCP-Ports im TIME_WAIT-Zustand
        // zu erschoepfen. Das HttpClient-Objekt selbst haelt keine Auth — die
        // steckt in der URL pro Aufruf — also ist Sharing sicher.
        private static readonly HttpClient SharedHttp = new HttpClient(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        })
        {
            Timeout = TimeSpan.FromSeconds(15)
        };
        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 0;
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
            // HttpClient wird statisch geteilt (siehe SharedHttp).
        }

        /// Basis-Verzeichnis fuer alle Gemini-Korrektur-Prompts. Wird bei JEDEM
        /// CorrectTextAsync-Aufruf neu gelesen — Aenderungen wirken sofort
        /// ohne App-Neustart.
        private static string GeminiPromptDir
            => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                            "SK", "VoiceOverlays");

        /// <summary>
        /// Mappt eine Profil-Nummer auf den Dateinamen der zugehoerigen
        /// Prompt-Datei. Profile 1-3 haben semantische Namen (Standard,
        /// Programmierung, Meta), Profile 4-10 haben numerische Slots, die
        /// der Benutzer spaeter mit eigenen Prompt-Dateien fuellen kann.
        /// Falls die profilspezifische Datei fehlt, faellt der Aufrufer auf
        /// die alte Sammeldatei zurueck (Backward Compat) und am Ende auf
        /// den eingebauten PromptTemplate-Konstanten.
        /// </summary>
        private static string ProfilePromptFileName(int profile) => profile switch
        {
            1 => "gemini-correction-prompt-standard.txt",
            2 => "gemini-correction-prompt-programmierung.txt",
            3 => "gemini-correction-prompt-meta.txt",
            >= 4 and <= 10 => $"gemini-correction-prompt-{profile:D2}.txt",
            _ => "gemini-correction-prompt-standard.txt"
        };

        // ── Public API fuer den Prompt-Editor (Etappe 2b, Frank-Wunsch 2026-06-22) ──

        /// <summary>Anzeige-Name eines Profils fuer die Editor-Liste.</summary>
        public static string ProfileLabel(int profile) => profile switch
        {
            1 => "Profil 1 — Standard (alltaegliche Texte)",
            2 => "Profil 2 — Programmierung (Code, CLI, Frameworks)",
            3 => "Profil 3 — Meta-Intelligenz (strukturiertes Denken)",
            _ => $"Profil {profile} — Slot {profile}"
        };

        /// <summary>Voller Pfad der Prompt-Datei eines Profils (zum Speichern).</summary>
        public static string ProfilePromptPath(int profile) =>
            Path.Combine(GeminiPromptDir, ProfilePromptFileName(profile));

        /// <summary>
        /// Die aktuell WIRKSAME Vorlage eines Profils: profilspezifische Datei,
        /// sonst Legacy-Sammeldatei, sonst eingebauter Default. So sieht der Editor
        /// immer genau das, was Gemini fuer dieses Profil gerade bekommt.
        /// </summary>
        public static string EffectivePrompt(int profile) =>
            LoadGeminiCorrectionPrompt(profile) ?? PromptTemplate;

        /// <summary>
        /// Speichert die bearbeitete Vorlage eines Profils in seine SK-Datei.
        /// Aenderung wirkt sofort (GeminiClient liest per mtime-Cache neu).
        /// </summary>
        public static void SaveProfilePrompt(int profile, string text)
        {
            var path = ProfilePromptPath(profile);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, text ?? string.Empty);
        }

        /// <summary>Der aktuell WIRKSAME Woerterbuch-Einleitungstext: editierbare Datei oder Default.</summary>
        public static string EffectiveVocabularyPreamble()
        {
            var p = Path.Combine(GeminiPromptDir, VocabularyPreambleFileName);
            var text = ReadPromptFileCached(p)?.Trim();
            return string.IsNullOrWhiteSpace(text) ? DefaultVocabularyPreamble : text;
        }

        /// <summary>Speichert den Woerterbuch-Einleitungstext in seine SK-Datei (wirkt sofort).</summary>
        public static void SaveVocabularyPreamble(string text)
        {
            var p = Path.Combine(GeminiPromptDir, VocabularyPreambleFileName);
            Directory.CreateDirectory(Path.GetDirectoryName(p)!);
            File.WriteAllText(p, text ?? string.Empty);
        }

        // Mtime-getriebener Cache fuer die Korrektur-Prompt-Dateien. Frueher
        // las jeder Voice-Submit die Datei neu von Disk — bei OneDrive-
        // Materialisierung oder Antivirus-Hooks kostete das messbar 5-50 ms
        // Latenz vor dem eigentlichen Gemini-Call. Jetzt: erste Anfrage liest
        // einmal, folgende Anfragen vergleichen nur den File.GetLastWriteTimeUtc-
        // Zeitstempel (in-process, kein Disk-Read). Aenderungen an der Datei
        // wirken weiter sofort — wir lesen neu sobald die mtime sich verschiebt.
        // Direktive-3-Resilienz: thread-safe per ConcurrentDictionary, jeder
        // Path hat seinen eigenen Eintrag; bei Datei-loescht-und-neu-erstellt
        // ist die mtime des neuen Files anders, also wird automatisch geladen.
        private sealed class CachedPrompt
        {
            public DateTime MtimeUtc;
            public string? Text;
        }
        private static readonly System.Collections.Concurrent.ConcurrentDictionary<string, CachedPrompt> _promptCache
            = new(StringComparer.OrdinalIgnoreCase);

        private static string? ReadPromptFileCached(string path)
        {
            try
            {
                if (!File.Exists(path)) return null;
                var mtime = File.GetLastWriteTimeUtc(path);
                if (_promptCache.TryGetValue(path, out var cached) && cached.MtimeUtc == mtime)
                    return cached.Text;
                var text = File.ReadAllText(path);
                _promptCache[path] = new CachedPrompt { MtimeUtc = mtime, Text = text };
                return text;
            }
            catch
            {
                return null;
            }
        }

        private static string? LoadGeminiCorrectionPrompt(int profile)
        {
            try
            {
                // 1) Profil-spezifische Datei
                var profilePath = Path.Combine(GeminiPromptDir, ProfilePromptFileName(profile));
                var text = ReadPromptFileCached(profilePath);
                if (!string.IsNullOrWhiteSpace(text)) return text;

                // 2) Legacy-Fallback: alte Sammeldatei (vor Profil-Trennung)
                var legacyPath = Path.Combine(GeminiPromptDir, "gemini-correction-prompt.txt");
                text = ReadPromptFileCached(legacyPath);
                if (!string.IsNullOrWhiteSpace(text)) return text;

                return null;
            }
            catch { return null; }
        }

        // Dateiname der persoenlichen Vokabular-Liste. Liegt im selben SK-Ordner
        // wie die Korrektur-Prompts, sodass sich auf EINEM Rechner TVO und CVO
        // automatisch dieselbe Liste teilen. Wird ueber denselben mtime-Cache
        // gelesen wie die Prompt-Dateien — Aenderungen wirken sofort ohne Neustart.
        private const string PersonalVocabularyFileName = "personal-vocabulary.txt";

        // Woerterbuch-Schalter (Frank-Wunsch 2026-06-22): geteilte Datei im SK-Ordner.
        // Inhalt "true" = Woerterbuch wird an Gemini mitgeschickt; alles andere
        // (auch fehlende Datei) = aus. Default aus.
        private const string VocabularyEnabledFileName = "vocabulary-enabled.txt";

        // Editierbarer Einleitungstext fuer den Woerterbuch-Block (Frank-Wunsch
        // 2026-06-22): liegt als Datei vocabulary-preamble.txt im SK-Ordner vor,
        // faellt sonst auf DefaultVocabularyPreamble zurueck.
        private const string VocabularyPreambleFileName = "vocabulary-preamble.txt";
        public const string DefaultVocabularyPreamble =
            "PERSÖNLICHES VOKABULAR (Begriffe, die die Spracherkennung oft falsch schreibt):\n" +
            "Die folgenden Wörter/Eigennamen benutzt der Sprecher regelmäßig. Die Groq/Whisper-" +
            "Spracherkennung transkribiert sie oft falsch, weil sie nur ÄHNLICH KLINGEN. Wenn ein " +
            "transkribiertes Wort phonetisch einem dieser Begriffe ähnelt UND es im Satzkontext " +
            "Sinn ergibt, ersetze es durch die hier angegebene korrekte Schreibweise. Erzwinge die " +
            "Ersetzung NICHT, wenn der Kontext eindeutig eine andere, normale Bedeutung hat " +
            "(z.B. bleibt \"Backen\" beim Thema Kuchen \"Backen\", wird aber im Programmier-" +
            "Kontext zu \"Backend\").";

        /// <summary>
        /// Laedt die persoenliche Vokabular-Liste (haeufige Begriffe/Eigennamen
        /// des Sprechers) und baut daraus einen Praeambel-Block, der VOR den
        /// Korrektur-Prompt gesetzt wird. Bewusst kontextsensitiv formuliert:
        /// KEIN stures Suchen-und-Ersetzen — Gemini bringt einen Begriff nur dann
        /// in die korrekte Schreibweise, wenn er phonetisch passt UND der
        /// Satzkontext es hergibt ("Backen" bleibt "Backen", wird nur im
        /// Code-Kontext zu "Backend"). Leere/fehlende Datei → leerer String,
        /// also exakt das bisherige Verhalten.
        /// </summary>
        private static string LoadPersonalVocabularyBlock()
        {
            try
            {
                // Woerterbuch-Schalter zuerst pruefen: nur laden wenn ausdruecklich
                // aktiviert ("true"). Fehlt die Datei oder steht etwas anderes drin,
                // bleibt das Woerterbuch aus (Frank-Wunsch 2026-06-22, Default aus).
                var togglePath = Path.Combine(GeminiPromptDir, VocabularyEnabledFileName);
                var toggle = ReadPromptFileCached(togglePath)?.Trim();
                if (!string.Equals(toggle, "true", StringComparison.OrdinalIgnoreCase))
                    return string.Empty;

                var path = Path.Combine(GeminiPromptDir, PersonalVocabularyFileName);
                var vocab = ReadPromptFileCached(path)?.Trim();
                if (string.IsNullOrWhiteSpace(vocab)) return string.Empty;

                // Einleitungstext (Praeambel) aus der editierbaren Datei, sonst Default.
                return EffectiveVocabularyPreamble() + "\n" + vocab + "\n\n";
            }
            catch
            {
                return string.Empty;
            }
        }

        public async Task<string> CorrectTextAsync(string text, int profile = 1)
        {
            // Profil-spezifische Korrektur-Prompt-Datei hat Vorrang — Frank
            // kann sie jederzeit pflegen ohne Rebuild. Fallbacks: Legacy-
            // Sammeldatei, dann eingebauter Template.
            var template = LoadGeminiCorrectionPrompt(profile) ?? PromptTemplate;
            // Persoenliches Vokabular als Praeambel VORANSTELLEN (Gemini-BP §7:
            // wiederkehrende Inhalte an den Prompt-Anfang → bessere Cache-Trefferquote).
            var vocab = LoadPersonalVocabularyBlock();
            return await SendWithRetry(BuildPrompt(template, vocab, text) + "\nTEXT_END", 0);
        }

        // Baut den finalen Prompt aus Vorlage + Woerterbuch-Block + gesprochenem
        // Text. Die Platzhalter {{TEXT}} und {{WOERTERBUCH}} koennen frei in der
        // Vorlage positioniert werden (Frank-Wunsch 2026-06-22). Abwaertskompatibel:
        // fehlt {{WOERTERBUCH}} -> Woerterbuch-Block an den Anfang (wie bisher);
        // fehlt {{TEXT}} -> gesprochener Text ans Ende (wie bisher). vocab ist
        // bereits leer, wenn der Woerterbuch-Schalter aus ist (LoadPersonalVocabularyBlock).
        private static string BuildPrompt(string template, string vocab, string text)
        {
            const string vocabMarker = "{{WOERTERBUCH}}";
            const string textMarker = "{{TEXT}}";
            var result = template;

            result = result.Contains(vocabMarker)
                ? result.Replace(vocabMarker, vocab)
                : vocab + result;

            result = result.Contains(textMarker)
                ? result.Replace(textMarker, text)
                : result + text;

            return result;
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
        /// Erzeugt eine kompakte Zusammenfassung von 6-8 deutschen Woertern,
        /// die beschreibt WOFUER ein gespeicherter Prompt da ist bzw. was er
        /// bewirkt. Wird als Hover-Tooltip ueber dem belegten Prompt-
        /// Zwischenspeicher-Slot angezeigt. Leerer Rueckgabewert bei Fehler
        /// oder leerem Input — der Tooltip faellt dann auf den Standardtext
        /// zurueck, die Slot-Funktion bleibt davon unberuehrt (best-effort).
        /// </summary>
        public async Task<string> GenerateSlotSummaryAsync(string text)
        {
            string trimmed = (text ?? string.Empty).Trim();
            if (trimmed.Length == 0) return string.Empty;

            const string summaryPrompt =
                "Fasse in 6 bis 8 deutschen Woertern zusammen, WOFUER der folgende " +
                "Prompt da ist bzw. was er bewirkt. STRENGE REGELN: 6 bis 8 Woerter. " +
                "Keine Anfuehrungszeichen. Kein Punkt am Ende. Kein Praefix wie " +
                "'Zusammenfassung:'. Nur die nackte Wortgruppe zurueckgeben.\n\nPROMPT:\n";
            try
            {
                string raw = await SendWithRetry(summaryPrompt + trimmed, 0);
                return SanitizeSummary(raw);
            }
            catch
            {
                // Best-effort: bei jedem Fehler leere Summary — der Slot bleibt
                // nutzbar, der Tooltip faellt auf den Standardtext zurueck.
                return string.Empty;
            }
        }

        /// <summary>
        /// Saeubert die Gemini-Antwort fuer die Slot-Summary: trimmt
        /// Anfuehrungszeichen und Schlusspunkt, klemmt auf maximal 8 Woerter.
        /// Liefert leer wenn nichts Brauchbares uebrig bleibt.
        /// </summary>
        private static string SanitizeSummary(string raw)
        {
            string s = (raw ?? string.Empty).Trim().Trim('"', '\'', '“', '”', '‚', '‘');
            if (s.EndsWith(".")) s = s.Substring(0, s.Length - 1).Trim();
            var words = s.Split(new[] { ' ', '\t', '\n', '\r' },
                                StringSplitOptions.RemoveEmptyEntries);
            if (words.Length == 0) return string.Empty;
            if (words.Length > 8) words = words[..8];
            return string.Join(" ", words);
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

        /// <summary>
        /// Baut die generationConfig — thinkingConfig wird NUR mitgesendet
        /// wenn ein ThinkingLevel gesetzt ist. Modelle wie gemini-2.5-flash
        /// rejecten den Parameter mit "Thinking level is not supported for
        /// this model" wenn er drin ist; Pro- und Flash-Thinking-Modelle
        /// brauchen ihn dagegen. Mit einem Dictionary statt anonymem Typ
        /// koennen wir den Schluessel bedingt weglassen.
        /// </summary>
        private System.Collections.Generic.Dictionary<string, object> BuildGenerationConfig()
        {
            var cfg = new System.Collections.Generic.Dictionary<string, object>
            {
                ["maxOutputTokens"] = 8192,
            };
            if (!string.IsNullOrWhiteSpace(_thinkingLevel))
            {
                cfg["thinkingConfig"] = new { thinkingLevel = _thinkingLevel };
            }
            return cfg;
        }

        private async Task<string> SendWithRetry(string prompt, int attempt)
        {
            var totalSw = Stopwatch.StartNew();
            var url = $"https://generativelanguage.googleapis.com/v1beta/models/{_model}:generateContent";

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
                generationConfig = BuildGenerationConfig()
            };

            var json = JsonSerializer.Serialize(payload);
            using var request = new HttpRequestMessage(HttpMethod.Post, url)
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            };
            request.Headers.Add("x-goog-api-key", _apiKey);
            DiagLog.Write("Gemini", "http_start", ("model", _model), ("attempt", attempt), ("promptChars", prompt.Length), ("payloadBytes", Encoding.UTF8.GetByteCount(json)));

            int statusCode;
            bool isSuccessStatusCode;
            string body;
            string retryAfter;
            using (var response = await SharedHttp.SendAsync(request).ConfigureAwait(false))
            {
                statusCode = (int)response.StatusCode;
                isSuccessStatusCode = response.IsSuccessStatusCode;
                retryAfter = response.Headers.RetryAfter?.ToString() ?? "";
                body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
            }
            DiagLog.Perf("Gemini", "http_response", totalSw, ("status", statusCode), ("attempt", attempt), ("retryAfter", retryAfter));

            if (isSuccessStatusCode)
            {
                var text = ExtractText(body);
                DiagLog.Perf("Gemini", "extract_done", totalSw, ("bodyChars", body.Length), ("textChars", text.Length));
                return text;
            }

            if (Array.IndexOf(RetryableStatusCodes, statusCode) >= 0 && attempt < MaxRetries)
            {
                Console.WriteLine($"Gemini {statusCode} - Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms...");
                DiagLog.Warn("Gemini", "retry_scheduled", ("status", statusCode), ("attempt", attempt), ("delayMs", DelaysMs[attempt]));
                await Task.Delay(DelaysMs[attempt]).ConfigureAwait(false);
                return await SendWithRetry(prompt, attempt + 1).ConfigureAwait(false);
            }

            DiagLog.Warn("Gemini", "http_error", ("status", statusCode), ("body", body.Length > 500 ? body.Substring(0, 500) + "…" : body));
            throw new Exception($"Gemini API Fehler {statusCode}: {body}");
        }

        private static string ExtractText(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            var root = doc.RootElement;

            if (root.TryGetProperty("promptFeedback", out var promptFeedback) &&
                promptFeedback.TryGetProperty("blockReason", out var blockReasonElement) &&
                blockReasonElement.ValueKind == JsonValueKind.String)
            {
                var blockReason = blockReasonElement.GetString();
                if (!string.IsNullOrWhiteSpace(blockReason) &&
                    !string.Equals(blockReason, "BLOCK_REASON_UNSPECIFIED", StringComparison.Ordinal))
                {
                    var message = promptFeedback.TryGetProperty("blockReasonMessage", out var messageElement) &&
                                  messageElement.ValueKind == JsonValueKind.String
                        ? $": {messageElement.GetString()}"
                        : string.Empty;
                    throw new Exception($"Gemini blockierte den Prompt ({blockReason}){message}");
                }
            }

            if (!root.TryGetProperty("candidates", out var candidates) ||
                candidates.GetArrayLength() == 0)
                throw new Exception("Unerwartete Gemini-Antwort: keine Kandidaten");

            var candidate = candidates[0];
            if (!candidate.TryGetProperty("finishReason", out var finishReasonElement) ||
                finishReasonElement.ValueKind != JsonValueKind.String)
                throw new Exception("Unerwartete Gemini-Antwort: finishReason fehlt");

            var finishReason = finishReasonElement.GetString();
            if (!string.Equals(finishReason, "STOP", StringComparison.Ordinal))
                throw new Exception($"Unvollständige Gemini-Antwort (finishReason: {finishReason})");

            var content = candidate.GetProperty("content");
            var parts = content.GetProperty("parts");
            var result = new StringBuilder();

            foreach (var part in parts.EnumerateArray())
            {
                // Skip thinking parts
                if (part.TryGetProperty("thought", out var thought) &&
                    thought.ValueKind == JsonValueKind.True)
                    continue;

                if (part.TryGetProperty("text", out var textElem) &&
                    textElem.ValueKind == JsonValueKind.String)
                    result.Append(textElem.GetString());
            }

            var text = result.ToString().Trim();
            if (!string.IsNullOrEmpty(text))
                return text;

            throw new Exception("Kein Text in Gemini-Antwort");
        }
    }
}
