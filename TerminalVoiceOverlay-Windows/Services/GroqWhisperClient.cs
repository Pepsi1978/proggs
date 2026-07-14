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
        private static readonly bool PreferCurlTransport = OperatingSystem.IsWindows();
        private const int TransportTimeoutSeconds = 75;

        private sealed class CurlStartException : Exception
        {
            public CurlStartException(string message, Exception? innerException = null)
                : base(message, innerException) { }
        }

        // Statischer geteilter HttpClient. In dieser App wird der GroqWhisperClient
        // aktuell pro Process nur einmal gebaut, daher ist Socket-Exhaustion
        // KEIN konkretes Problem — aber: kuenftiges Settings-Reload (z.B. Wechsel
        // des Whisper-Endpoints) wuerde mit per-instance HttpClient pro Reload
        // einen neuen Socket-Pool aufmachen und alte Verbindungen erst nach
        // TIME_WAIT freigeben. Statisch geteilt wie bei GeminiClient.SharedHttp
        // ist die kanonische .NET-Loesung. Authorization-Header wird pro Request
        // gesetzt (nicht am Client), daher kein Konflikt bei Sharing.
        private static readonly HttpClient SharedHttp = new HttpClient(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        })
        {
            // Funktionserhaltend: Groq ist heute zeitweise 40-50 s langsam.
            // Ein 20-s-Timeout machte die App zwar schnell wieder gelb, verlor
            // aber jede Aufnahme. 75 s laesst den Text noch ankommen; die
            // Latenz-Sonden zeigen trotzdem exakt, ob Groq der Flaschenhals ist.
            Timeout = TimeSpan.FromSeconds(TransportTimeoutSeconds)
        };
        private static readonly int[] RetryableStatusCodes = { 429, 500, 503 };
        private const int MaxRetries = 0;
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

        // ----- Sprachinhalt-Vorfilter (Schicht 1): reine Stille gar nicht erst senden -----
        // Whisper halluziniert bei Stille Floskeln ("Vielen Dank") MIT hoher Confidence -> das
        // Confidence-Gate (Schicht 2) greift dann nicht, und bei ultrakurzen Clips fehlen oft die
        // Segmente ganz. Deshalb VOR dem Senden pruefen, ob ueberhaupt genug LAUTE (Sprach-)Zeit
        // im Clip ist. Konservativ: trennt reine Stille (~0 ms laut) von echter kurzer Sprache wie
        // "ja"/"stop" (>150 ms laut). Nur ABSOLUTE laute Zeit (keine Ratio) -> echte Aufnahmen mit
        // langen Denkpausen bleiben erhalten (sie haben echte Sprache irgendwo drin).
        private const double SpeechRmsThreshold = 0.015;  // RMS ueber diesem Wert gilt als "laut"
        private const int    MinSpeechMs        = 150;    // Mindest-Summe lauter Zeit, sonst nicht senden

        // ----- Segment-Audio-Abgleich (Schicht 3, Almanach §2.3 "zweite Luecke") -----
        // Trailing-/Pausen-Halluzination ("Ja" am Ende nach echtem Satz) kommt mit HOHER Confidence
        // -> das Confidence-Gate (Schicht 2) faengt sie nicht. Loesung: jedes Segment gegen die echte
        // Audio-Lautstaerke pruefen — ist sein Zeitfenster praktisch still, ist es eine Halluzination.
        private const int    FrameMs        = 20;    // RMS-Fenster (gleich wie HasSpeechContent)
        private const double SegVoicedRatio = 0.10;  // < 10% laute Frames im Segment-Fenster = still

        // ----- Floskel-Blocklist (Schicht 4, letzter Filter, Almanach §2.4) -----
        // Bei kurzem Knopfdruck ("nichts gesagt", nur Klick/Atem) halluziniert Whisper "Vielen Dank"
        // MIT hoher Confidence (Schicht 2 greift nicht) und der Klick liegt oft IM Segment-Zeitfenster
        // oder die Drift-Sicherung haelt das eine Segment (Schicht 3 greift nicht). GOLDENE REGEL:
        // eine Floskel NIE allein wegen des Wortlauts verwerfen — nur wenn ALLE drei Signale zugleich
        // zutreffen: (1) Ausgabe kurz, (2) normalisierter EXAKTER Match gegen die Liste, (3) Stille-
        // Kontext (der ganze Clip war sprach-arm). So bleibt ein bewusst gesprochenes "Vielen Dank"
        // (laute Zeit > Schwelle) erhalten.
        private const int    FloskelMaxWords          = 6;
        private const int    FloskelMaxChars          = 64;
        private const double SilenceContextMaxVoicedMs = 600;  // Clip mit weniger lauter Zeit = Stille-Kontext

        // Normalisierte (lowercase, ohne Satzzeichen/Ziffern, Umlaute behalten) Whisper-Outro-Floskeln.
        // DE-Kern + EN-Kern aus dem HF-Dataset "whisper-hallucinations" (Almanach §2.4).
        private static readonly HashSet<string> FloskelBlocklist = new(StringComparer.Ordinal)
        {
            "vielen dank",
            "vielen dank fürs zuschauen",
            "vielen dank fuers zuschauen",
            "vielen dank für eure aufmerksamkeit",
            "vielen dank für ihre aufmerksamkeit",
            "vielen dank für die aufmerksamkeit",
            "bis zum nächsten mal",
            "bis zum nächsten video",
            "untertitel",
            "untertitel des zdf",
            "untertitelung des zdf für funk",
            "untertitel im auftrag des zdf für funk",
            "untertitel von stephanie geiges",
            "untertitel der amara org community",
            "der text ist nicht auf deutsch",
            "thank you",
            "thank you for watching",
            "thanks for watching",
            "please subscribe",
            "subtitles by the amara org community",
        };

        public GroqWhisperClient(string apiKey, string model, string language, string url)
        {
            _apiKey = apiKey;
            _model = model;
            _language = language;
            _url = url;
        }

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            var totalSw = Stopwatch.StartNew();
            DiagLog.Write("Groq", "transcribe_start", ("file", wavFilePath), ("model", _model), ("lang", _language));
            // Datei einmal in den Speicher laden und ueber alle Retry-Versuche
            // wiederverwenden. Vorher las jeder Retry die WAV-Datei neu von
            // Disk — bei 3 Retries und ~1 MB Audio entstehen 3 MB unnoetige
            // Disk-I/O. Die Bytes werden im Aufruf nicht mutiert, also ist
            // Sharing zwischen Versuchen sicher.
            var readSw = Stopwatch.StartNew();
            byte[] fileBytes = await File.ReadAllBytesAsync(wavFilePath).ConfigureAwait(false);
            DiagLog.Perf("Groq", "read_wav", readSw, ("bytes", fileBytes.Length));
            // Schicht 1 (Sprachinhalt-Vorfilter): Aufnahme ohne erkennbaren Sprachinhalt gar nicht
            // erst senden. Faengt "Knopf gedrueckt, nichts gesagt" -> Whisper haette sonst Stille als
            // Floskel halluziniert. Werfen statt leer zurueckgeben -> der Aufrufer-catch behandelt es
            // wie die bisherige "leere Antwort" und tippt NICHTS (kein einsames " ; ").
            var prefilterSw = Stopwatch.StartNew();
            if (!HasSpeechContent(fileBytes))
            {
                DiagLog.Warn("Groq", "prefilter_rejected", ("ms", prefilterSw.ElapsedMilliseconds), ("bytes", fileBytes.Length));
                throw new Exception("Aufnahme ohne erkennbaren Sprachinhalt — nicht an Groq gesendet (Stille-Schutz)");
            }
            DiagLog.Perf("Groq", "prefilter_ok", prefilterSw, ("bytes", fileBytes.Length));
            try
            {
                var text = await TranscribeWithRetry(fileBytes, 0).ConfigureAwait(false);
                DiagLog.Perf("Groq", "transcribe_total", totalSw, ("chars", text.Length));
                return text;
            }
            catch (Exception ex)
            {
                DiagLog.Error("Groq", "transcribe_failed", ex, ("ms", totalSw.ElapsedMilliseconds), ("bytes", fileBytes.Length));
                throw;
            }
        }

        private async Task<string> TranscribeWithRetry(byte[] fileBytes, int attempt)
        {
            if (PreferCurlTransport)
            {
                try
                {
                    var json = await SendWithCurlAsync(fileBytes).ConfigureAwait(false);
                    bool[]? voiced = BuildVoicedTimeline(fileBytes);
                    var text = FilterTranscription(json, voiced);
                    DiagLog.Write("Groq", "filter_done", ("jsonChars", json.Length), ("textChars", text.Length), ("transport", "curl"));
                    if (!string.IsNullOrEmpty(text))
                        return text;
                    throw new Exception("Leere Antwort von Groq");
                }
                catch (CurlStartException ex)
                {
                    // Funktionserhaltend: curl.exe ist auf Franks Windows-Rechner
                    // der schnelle Standardtransport. Wenn curl fehlt oder lokal
                    // blockiert ist, bleibt der .NET-Client als Fallback erhalten.
                    DiagLog.Warn("Groq", "curl_transport_failed_fallback_dotnet", ("err", ex.Message), ("type", ex.GetType().Name));
                }
            }

            return await TranscribeWithHttpClientAsync(fileBytes, attempt).ConfigureAwait(false);
        }

        private async Task<string> SendWithCurlAsync(byte[] fileBytes)
        {
            string wavPath = Path.Combine(Path.GetTempPath(), $"tvo_groq_upload_{Guid.NewGuid():N}.wav");
            try
            {
                await File.WriteAllBytesAsync(wavPath, fileBytes).ConfigureAwait(false);

                using var process = new Process();
                process.StartInfo = new ProcessStartInfo
                {
                    FileName = "curl.exe",
                    Arguments = "--config -",
                    UseShellExecute = false,
                    RedirectStandardInput = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                    StandardInputEncoding = Encoding.UTF8,
                    StandardOutputEncoding = Encoding.UTF8,
                    StandardErrorEncoding = Encoding.UTF8,
                };

                var curlSw = Stopwatch.StartNew();
                DiagLog.Write("Groq", "http_start", ("attempt", 0), ("bytes", fileBytes.Length), ("url", _url), ("transport", "curl"));
                try
                {
                    if (!process.Start())
                        throw new CurlStartException("curl.exe konnte nicht gestartet werden");
                }
                catch (System.ComponentModel.Win32Exception ex)
                {
                    throw new CurlStartException("curl.exe konnte nicht gestartet oder gefunden werden", ex);
                }
                await process.StandardInput.WriteAsync(BuildCurlConfig(wavPath)).ConfigureAwait(false);
                process.StandardInput.Close();
                string stdout = await process.StandardOutput.ReadToEndAsync().ConfigureAwait(false);
                string stderr = await process.StandardError.ReadToEndAsync().ConfigureAwait(false);
                await process.WaitForExitAsync().ConfigureAwait(false);
                DiagLog.Perf("Groq", "http_response", curlSw,
                    ("attempt", 0),
                    ("status", process.ExitCode == 0 ? 200 : 0),
                    ("transport", "curl"),
                    ("exit", process.ExitCode));

                if (process.ExitCode != 0)
                {
                    var details = string.IsNullOrWhiteSpace(stdout)
                        ? Truncate(stderr, 500)
                        : $"{Truncate(stderr, 500)} Response: {Truncate(stdout, 500)}";
                    throw new Exception($"curl.exe Groq request failed (exit {process.ExitCode}): {details}");
                }
                if (string.IsNullOrWhiteSpace(stdout))
                    throw new Exception("curl.exe Groq request returned an empty body");
                return stdout;
            }
            finally
            {
                TryDelete(wavPath);
            }
        }

        private string BuildCurlConfig(string wavPath)
        {
            var sb = new StringBuilder();
            sb.Append("url = ").Append(CurlQuote(_url)).Append('\n');
            sb.Append("request = \"POST\"\n");
            sb.Append("header = ").Append(CurlQuote("Authorization: Bearer " + _apiKey)).Append('\n');
            sb.Append("form = ").Append(CurlQuote("file=@" + wavPath + ";type=audio/wav")).Append('\n');
            sb.Append("form = ").Append(CurlQuote("model=" + _model)).Append('\n');
            sb.Append("form = ").Append(CurlQuote("language=" + _language)).Append('\n');
            sb.Append("form = \"response_format=verbose_json\"\n");
            sb.Append("form = \"temperature=0\"\n");
            sb.Append("silent\n");
            sb.Append("show-error\n");
            sb.Append("fail-with-body\n");
            sb.Append("max-time = ").Append(TransportTimeoutSeconds).Append('\n');
            return sb.ToString();
        }

        private static string CurlQuote(string value)
        {
            return "\"" + value.Replace("\\", "\\\\").Replace("\"", "\\\"") + "\"";
        }

        private static void TryDelete(string path)
        {
            try
            {
                if (!string.IsNullOrEmpty(path) && File.Exists(path))
                    File.Delete(path);
            }
            catch (Exception ex)
            {
                DiagLog.Warn("Groq", "temp_delete_failed", ("path", path), ("error", ex.Message));
            }
        }

        private async Task<string> TranscribeWithHttpClientAsync(byte[] fileBytes, int attempt)
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

            var httpSw = Stopwatch.StartNew();
            DiagLog.Write("Groq", "http_start", ("attempt", attempt), ("bytes", fileBytes.Length), ("url", _url));
            // ResponseHeadersRead ist hier der Root-Fix: Der Standardmodus
            // ResponseContentRead puffert die komplette Antwort vor Rueckgabe.
            // Auf Franks Windows-Rechner hing genau dieser Pfad bei Groq/Cloudflare
            // konstant ca. 42,5 s, obwohl Header + Body sofort verfuegbar waren.
            using var timeoutCts = new System.Threading.CancellationTokenSource(TimeSpan.FromSeconds(TransportTimeoutSeconds));
            int statusCode;
            bool isSuccessStatusCode;
            string responseBody;
            string retryAfter;
            using (var response = await SharedHttp.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeoutCts.Token).ConfigureAwait(false))
            {
                statusCode = (int)response.StatusCode;
                isSuccessStatusCode = response.IsSuccessStatusCode;
                retryAfter = response.Headers.RetryAfter?.ToString() ?? "";
                responseBody = await response.Content.ReadAsStringAsync(timeoutCts.Token).ConfigureAwait(false);
            }
            DiagLog.Perf("Groq", "http_response", httpSw,
                ("attempt", attempt),
                ("status", statusCode),
                ("retryAfter", retryAfter));

            if (isSuccessStatusCode)
            {
                // Schicht 3: Voiced-Timeline aus dem aufgenommenen PCM bauen (16-bit mono WAV), damit
                // FilterTranscription jedes Segment gegen die echte Lautstaerke abgleichen kann.
                bool[]? voiced = BuildVoicedTimeline(fileBytes);
                // Confidence-Gate (Schicht 2) + Audio-Abgleich (Schicht 3) anwenden. Bleibt Text uebrig
                // -> zurueck. Alles Stille/halluziniert -> leer: wie bisher die "leere Antwort"
                // (gleiche Exception, gleicher Aufrufer-catch) — funktionserhaltend.
                var text = FilterTranscription(responseBody, voiced);
                DiagLog.Write("Groq", "filter_done", ("jsonChars", responseBody.Length), ("textChars", text.Length));
                if (!string.IsNullOrEmpty(text))
                    return text;
                throw new Exception("Leere Antwort von Groq");
            }

            if (Array.IndexOf(RetryableStatusCodes, statusCode) >= 0 && attempt < MaxRetries)
            {
                Console.WriteLine($"Groq {statusCode} - Versuch {attempt + 1}/{MaxRetries}, warte {DelaysMs[attempt]}ms...");
                DiagLog.Warn("Groq", "retry_scheduled", ("status", statusCode), ("attempt", attempt), ("delayMs", DelaysMs[attempt]));
                await Task.Delay(DelaysMs[attempt]).ConfigureAwait(false);
                return await TranscribeWithRetry(fileBytes, attempt + 1).ConfigureAwait(false);
            }

            DiagLog.Warn("Groq", "http_error", ("status", statusCode), ("body", Truncate(responseBody, 500)));
            throw new Exception($"Groq API Fehler {statusCode}: {responseBody}");
        }

        private static string Truncate(string value, int maxChars)
        {
            if (string.IsNullOrEmpty(value) || value.Length <= maxChars) return value;
            return value.Substring(0, maxChars) + "…";
        }

        /// <summary>
        /// Wendet das Confidence-Gate (Almanach §2.3) auf die verbose_json-Antwort an und gibt die
        /// verbleibenden Segment-Texte zusammengefuegt zurueck. Funktionserhaltend: faellt bei
        /// unerwartetem Format auf den Roh-Text zurueck (nie verlieren); ohne Segment-Metadaten wird
        /// der top-level Text ungefiltert durchgelassen.
        /// </summary>
        private static string FilterTranscription(string json, bool[]? voiced)
        {
            GroqVerboseResponse? parsed;
            try
            {
                parsed = JsonSerializer.Deserialize(json, GroqJsonContext.Default.GroqVerboseResponse);
            }
            catch (JsonException)
            {
                // Unerwartetes Format -> NICHT verlieren: top-level "text" als Fallback ziehen.
                // Schicht 4 trotzdem anwenden (auch hier kann eine reine Floskel stehen).
                return BlockIfFloskel(ExtractFallbackText(json), voiced);
            }

            if (parsed?.Segments == null || parsed.Segments.Count == 0)
            {
                // Keine Segment-Metadaten -> nicht filterbar (ultrakurze Clips beim schnellen Druecken
                // liefern oft NUR top-level Text, KEINE Segmente — Almanach §2.3). Funktionserhalt:
                // top-level Text durchlassen, aber ZUERST durch die Floskel-Blocklist (Schicht 4) — sonst
                // umgeht "Vielen Dank" bei Kurzclips den ganzen Stille-Schutz.
                return BlockIfFloskel((parsed?.Text ?? string.Empty).Trim(), voiced);
            }

            // Schicht 2: Confidence-Gate. Halluzinationen mit hoher "keine Sprache"-Wahrscheinlichkeit raus.
            var afterConfidence = new List<GroqSegment>();
            int droppedConfidence = 0;
            foreach (var seg in parsed.Segments)
            {
                if (IsHallucination(seg)) { droppedConfidence++; continue; }
                afterConfidence.Add(seg);
            }

            // Schicht 3: Audio-Abgleich. Trailing-/Pausen-Halluzination mit HOHER Confidence (die
            // Schicht 2 ueberlebt) verwerfen, wenn ihr Zeitfenster im echten Audio still war.
            var kept = new List<GroqSegment>();
            int droppedAudio = 0;
            if (voiced != null)
            {
                foreach (var seg in afterConfidence)
                {
                    if (!SegmentHasSpeech(seg, voiced)) { droppedAudio++; continue; }
                    kept.Add(seg);
                }
            }
            else
            {
                kept.AddRange(afterConfidence);
            }

            // Drift-Sicherung (funktionserhaltend): Wuerde der Audio-Abgleich ALLE Segmente verwerfen,
            // obwohl das Confidence-Gate noch welche liess, ist vermutlich das Whisper-Zeitstempel-
            // Alignment verschoben -> lieber die Confidence-gefilterten behalten als den Satz verlieren.
            var finalSegs = kept;
            if (kept.Count == 0 && afterConfidence.Count > 0 && voiced != null)
            {
                Console.WriteLine("Groq: Audio-Abgleich (Schicht 3) verwarf alle Segmente -> Fallback auf Confidence-gefilterte (Zeitstempel-Drift?).");
                DiagLog.Warn("Groq", "audio_filter_drift_fallback", ("segments", afterConfidence.Count));
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

            if (droppedConfidence > 0 || droppedAudio > 0)
            {
                Console.WriteLine($"Groq: {droppedConfidence} Confidence- + {droppedAudio} Audio-Segment(e) verworfen, {finalSegs.Count} behalten (Stille-Schutz).");
                DiagLog.Write("Groq", "segments_filtered", ("confidence", droppedConfidence), ("audio", droppedAudio), ("kept", finalSegs.Count));
            }

            // Schicht 4: Floskel-Blocklist (letzter Filter, an ALLEN Ausgaengen — siehe oben). Faengt
            // "Vielen Dank", das Schicht 2+3 ueberlebt. Funktionserhaltend (kurz + exakt + Stille-Kontext).
            return BlockIfFloskel(sb.ToString().Trim(), voiced);
        }

        /// <summary>
        /// Schicht 4 zentral: gibt den Text zurueck — oder leer, wenn er eine Stille-Floskel ist
        /// (<see cref="IsBlocklistedFloskel"/>). An JEDEM Rueckgabepfad von FilterTranscription aufgerufen,
        /// damit auch der "keine Segmente"-Kurzclip-Pfad (Almanach §2.3) gefiltert wird.
        /// </summary>
        private static string BlockIfFloskel(string text, bool[]? voiced)
        {
            if (IsBlocklistedFloskel(text, voiced))
            {
                Console.WriteLine($"Groq: Floskel-Blocklist (Schicht 4) verwarf \"{text}\" (kurz + exakter Match + Stille-Kontext).");
                DiagLog.Warn("Groq", "floskel_blocked", ("chars", text.Length));
                return string.Empty;
            }
            return text;
        }

        /// <summary>
        /// Schicht 4 (Almanach §2.4): true, wenn der Gesamttext eine Whisper-Outro-Floskel ist. Verwirft
        /// NUR bei drei gleichzeitigen Signalen — (1) kurz, (2) normalisierter EXAKTER Blocklist-Match,
        /// (3) Stille-Kontext (gesamte laute Zeit im Clip &lt; Schwelle). Ohne Voiced-Timeline (voiced==null)
        /// wird NICHT verworfen (Stille-Kontext nicht messbar -> funktionserhaltend durchlassen).
        /// </summary>
        private static bool IsBlocklistedFloskel(string text, bool[]? voiced)
        {
            if (string.IsNullOrEmpty(text) || text.Length > FloskelMaxChars) return false;
            if (voiced == null) return false;  // Stille-Kontext nicht messbar -> echte Sprache nie verlieren

            string norm = NormalizeFloskel(text);
            if (norm.Length == 0) return false;
            int words = norm.Split(' ', StringSplitOptions.RemoveEmptyEntries).Length;
            if (words > FloskelMaxWords) return false;
            if (!FloskelBlocklist.Contains(norm)) return false;   // (2) exakter Match (== nicht contains)

            // (3) Stille-Kontext: war der ganze Clip sprach-arm? Bewusst gesprochene Floskel hat mehr laute Zeit.
            double voicedMs = 0;
            foreach (bool v in voiced) if (v) voicedMs += FrameMs;
            return voicedMs < SilenceContextMaxVoicedMs;
        }

        /// <summary>lowercase, Satzzeichen/Ziffern entfernt (Umlaute bleiben), Whitespace kollabiert.</summary>
        private static string NormalizeFloskel(string s)
        {
            var sb = new StringBuilder(s.Length);
            foreach (char c in s.ToLowerInvariant())
            {
                if (char.IsLetter(c)) sb.Append(c);
                else if (char.IsWhiteSpace(c)) sb.Append(' ');
                // Satzzeichen, Ziffern, Symbole weglassen
            }
            return string.Join(' ', sb.ToString().Split(' ', StringSplitOptions.RemoveEmptyEntries));
        }

        /// <summary>
        /// Baut aus den WAV-Bytes (16-bit mono PCM) eine Voiced-Timeline: pro 20-ms-Frame true, wenn
        /// der RMS-Pegel ueber der Stille-Schwelle liegt. Liest die Sample-Rate aus dem Header (Bytes
        /// 24-27). Bei jedem Problem null -> dann kein Audio-Abgleich (funktionserhaltend).
        /// </summary>
        private static bool[]? BuildVoicedTimeline(byte[] wav)
        {
            const int headerSize = 44;
            if (wav.Length <= headerSize + 4) return null;
            int sampleRate = wav[24] | (wav[25] << 8) | (wav[26] << 16) | (wav[27] << 24);
            if (sampleRate <= 0) sampleRate = 16000;
            int frameSamples = Math.Max(1, sampleRate * FrameMs / 1000);
            int frameBytes = frameSamples * 2;
            int frameCount = (wav.Length - headerSize) / frameBytes;
            if (frameCount <= 0) return null;
            var voiced = new bool[frameCount];
            for (int f = 0; f < frameCount; f++)
            {
                int baseB = headerSize + f * frameBytes;
                double sumSq = 0;
                for (int s = 0; s < frameSamples; s++)
                {
                    int idx = baseB + s * 2;
                    short sample = (short)(wav[idx] | (wav[idx + 1] << 8));
                    double v = sample / 32768.0;
                    sumSq += v * v;
                }
                voiced[f] = Math.Sqrt(sumSq / frameSamples) > SpeechRmsThreshold;
            }
            return voiced;
        }

        /// <summary>
        /// True, wenn das Zeitfenster [Start,End] eines Segments genug laute Frames hat (>= SegVoicedRatio).
        /// Ungueltige/leere Fenster werden NICHT verworfen (funktionserhaltend).
        /// </summary>
        private static bool SegmentHasSpeech(GroqSegment seg, bool[] voiced)
        {
            if (!(seg.End > seg.Start)) return true;
            int startF = Math.Max(0, (int)(seg.Start * 1000.0 / FrameMs));
            int endF = Math.Min(voiced.Length, (int)Math.Ceiling(seg.End * 1000.0 / FrameMs));
            if (endF <= startF) return true;
            int v = 0;
            for (int i = startF; i < endF; i++) if (voiced[i]) v++;
            return (double)v / (endF - startF) >= SegVoicedRatio;
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

        /// <summary>
        /// Prueft, ob eine 16-bit-PCM-mono-WAV genug echten Sprachinhalt enthaelt, um an Groq gesendet
        /// zu werden (Schicht 1). Misst die aufsummierte LAUTE Zeit in 20-ms-Frames (RMS &gt; Schwelle):
        /// echte kurze Aussagen ueberschreiten <see cref="MinSpeechMs"/>, reine Stille nicht. Liest die
        /// Sample-Rate aus dem WAV-Header (Bytes 24-27); nimmt mono 16-bit an (so nimmt der AudioRecorder auf).
        /// </summary>
        private static bool HasSpeechContent(byte[] wav)
        {
            const int headerSize = 44;
            if (wav.Length <= headerSize + 4) return false;   // kein/zu wenig Audio
            int sampleRate = wav[24] | (wav[25] << 8) | (wav[26] << 16) | (wav[27] << 24);
            if (sampleRate <= 0) sampleRate = 16000;
            int frameSamples = Math.Max(1, sampleRate * 20 / 1000);   // 20-ms-Frames
            int frameBytes = frameSamples * 2;
            double voicedMs = 0;
            for (int i = headerSize; i + frameBytes <= wav.Length; i += frameBytes)
            {
                double sumSq = 0;
                for (int s = 0; s < frameSamples; s++)
                {
                    int idx = i + s * 2;
                    short sample = (short)(wav[idx] | (wav[idx + 1] << 8));
                    double f = sample / 32768.0;
                    sumSq += f * f;
                }
                double rms = Math.Sqrt(sumSq / frameSamples);
                if (rms > SpeechRmsThreshold) voicedMs += 20;
            }
            Console.WriteLine($"Groq-Vorfilter: laute Zeit {voicedMs:F0} ms (Schwelle {MinSpeechMs} ms) -> {(voicedMs >= MinSpeechMs ? "senden" : "verworfen")}.");
            DiagLog.Write("Groq", "prefilter_measure", ("voicedMs", voicedMs.ToString("F0")), ("thresholdMs", MinSpeechMs), ("ok", voicedMs >= MinSpeechMs));
            return voicedMs >= MinSpeechMs;
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
