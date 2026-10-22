using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Schluessel und Optionen der Zusatz-Anbieter (Gemini 3.8 TTS, Azure Neural).
    /// Port der Jarvis-SecureSettings-Felder; die Schluessel liegen in ~/SK/VoiceAgent/keys.json.
    /// </summary>
    public sealed record TtsOptions(
        string GeminiKeyFree,
        string GeminiKeyPaid,
        string GeminiKeyChoice,
        bool GeminiChirpFallback,
        string AzureKey,
        string AzureRegion)
    {
        public const string KeyFreeThenPaid = "frei_dann_bezahlt";
        public const string KeyOnlyFree = "nur_frei";
        public const string KeyOnlyPaid = "nur_bezahlt";

        /// <summary>Optionen der Schluesselwahl mit den Labels aus Jarvis.</summary>
        public static readonly (string Id, string Label)[] KeyChoices =
        {
            (KeyFreeThenPaid, "Erst kostenlos, dann bezahlt"),
            (KeyOnlyFree, "Nur kostenlos"),
            (KeyOnlyPaid, "Nur bezahlt"),
        };

        public static TtsOptions Empty => new("", "", KeyFreeThenPaid, false, "", GoogleTtsVoices.DefaultAzureRegion);

        /// <summary>Liest Schluessel aus dem SK-Ordner, Wahl/Rueckfall/Region aus den Einstellungen.</summary>
        public static TtsOptions FromConfig(AppSettings s) => new(
            Config.ReadApiKey("gemini_tts_frei"),
            Config.ReadApiKey("gemini_tts_bezahlt"),
            string.IsNullOrWhiteSpace(s.GeminiTtsKeyChoice) ? KeyFreeThenPaid : s.GeminiTtsKeyChoice,
            s.GeminiTtsChirpFallback,
            Config.ReadApiKey("azure_tts"),
            string.IsNullOrWhiteSpace(s.AzureTtsRegion) ? GoogleTtsVoices.DefaultAzureRegion : s.AzureTtsRegion);
    }

    /// <summary>HTTP-/Inhaltsfehler einer Stimme (Netzfehler laufen als HttpRequestException durch).</summary>
    public sealed class TtsException : Exception
    {
        public TtsException(string message) : base(message) { }
    }

    /// <summary>
    /// Sprachausgabe: Google Cloud Text-to-Speech (Chirp 3 HD) mit API-Key-Auth, dazu als Weiche nach
    /// Stimm-Kennung Gemini 3.8 Flash / Flash-Lite TTS und Azure Neural (Port aus Jarvis Synthese.kt).
    /// Liefert MP3 (Google, Azure) oder WAV (Gemini) — der AudioPlayer spielt beides (Media Foundation).
    /// </summary>
    public sealed class GoogleTtsClient
    {
        private readonly string _apiKey;
        private readonly TtsOptions _opts;

        private const string GeminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/";
        public const string GeminiFlashModel = "gemini-3.8-flash-tts";
        public const string GeminiLiteModel = "gemini-3.8-flash-lite-tts";

        /// <summary>So lange bleibt der kostenlose Gemini-Schluessel nach einem Fehler uebersprungen, wenn es einen Ausweichweg gibt.</summary>
        private const long FreePauseMs = 10 * 60_000L;

        /// <summary>Gilt fuer alle Instanzen: sonst probierte jeder neue Client den gescheiterten Schluessel erneut.</summary>
        private static long _freeBlockedUntil;

        private static readonly int[] WaitMs = { 1_000, 3_000, 8_000 };
        /// <summary>Reicht zusammen ueber das Minutenfenster von Azure F0.</summary>
        private static readonly int[] AzureWaitMs = { 3_000, 10_000, 20_000, 30_000 };

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(60) };

        public GoogleTtsClient(string apiKey, TtsOptions? options = null)
        {
            _apiKey = apiKey;
            _opts = options ?? TtsOptions.Empty;
        }

        /// <summary>Baut den Synthesize-Request-Body. Public static = isoliert testbar.</summary>
        public static string BuildRequestJson(string text, string languageCode, string voiceName)
        {
            var payload = new
            {
                input = new { text },
                voice = new { languageCode, name = voiceName },
                audioConfig = new { audioEncoding = "MP3" }
            };
            return JsonSerializer.Serialize(payload);
        }

        /// <summary>Dekodiert das Base64-Audio aus der TTS-Antwort. Public static = testbar.</summary>
        public static byte[] ParseAudio(string responseJson)
        {
            using var doc = JsonDocument.Parse(responseJson);
            if (!doc.RootElement.TryGetProperty("audioContent", out var ac))
                throw new Exception("Keine Audiodaten in TTS-Antwort (audioContent fehlt)");
            var b64 = ac.GetString();
            if (string.IsNullOrEmpty(b64))
                throw new Exception("Leere Audiodaten in TTS-Antwort");
            return Convert.FromBase64String(b64);
        }

        /// <summary>Synthetisiert Text in der gewaehlten Stimme; der Anbieter ergibt sich aus der Kennung.</summary>
        public Task<byte[]> SynthesizeAsync(string text, string languageCode, string voiceName, CancellationToken ct = default)
        {
            return GoogleTtsVoices.ProviderOf(voiceName) switch
            {
                GoogleTtsVoices.ProviderGeminiFlash => GeminiAsync(text, GeminiFlashModel, voiceName, languageCode, ct),
                GoogleTtsVoices.ProviderGeminiLite => GeminiAsync(text, GeminiLiteModel, voiceName, languageCode, ct),
                GoogleTtsVoices.ProviderAzure => AzureAsync(text, voiceName, ct),
                _ => GoogleAsync(text, languageCode, voiceName, ct),
            };
        }

        // ---------- Google Chirp 3 HD ----------

        private async Task<byte[]> GoogleAsync(string text, string languageCode, string voiceName, CancellationToken ct)
        {
            if (string.IsNullOrWhiteSpace(_apiKey))
                throw new InvalidOperationException("Kein Google-TTS-API-Schluessel hinterlegt (Einstellungen → API-Schluessel).");

            // Sanity: Google TTS lehnt Requests ueber 5000 Byte ab — lange Antworten wuerden still scheitern.
            Probe.That(text.Length <= 5000, "TTS: Text ueber Google-5000-Zeichen-Limit — Ausgabe koennte abgelehnt werden", new { chars = text.Length });

            var url = $"https://texttospeech.googleapis.com/v1/text:synthesize?key={_apiKey}";
            using var content = new StringContent(BuildRequestJson(text, languageCode, voiceName), Encoding.UTF8, "application/json");

            var resp = await SharedHttp.PostAsync(url, content, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Google TTS Fehler {(int)resp.StatusCode}: {body}");
                throw new TtsException($"Google TTS Fehler {(int)resp.StatusCode}: {body}");
            }

            var audio = ParseAudio(body);
            Log.Info($"TTS: {audio.Length} Bytes Audio erzeugt (Stimme {voiceName}, {text.Length} Zeichen)");
            return audio;
        }

        // ---------- Gemini 3.8 Flash / Flash-Lite TTS ----------

        /// <summary>
        /// Reihenfolge laut Schluesselwahl: erst der kostenlose, dann der bezahlte Schluessel. Ist der
        /// Chirp-Rueckfall an, geht es nach einem Fehler des kostenlosen Schluessels nicht zum bezahlten,
        /// sondern mit derselben Stimme zu Chirp 3 HD. Netzfehler laufen unveraendert durch.
        /// </summary>
        private async Task<byte[]> GeminiAsync(string text, string model, string voiceId, string languageCode, CancellationToken ct)
        {
            var choice = _opts.GeminiKeyChoice;
            var free = Regex.Replace(_opts.GeminiKeyFree ?? "", @"\s", "");
            var paid = Regex.Replace(_opts.GeminiKeyPaid ?? "", @"\s", "");
            var chirp = _opts.GeminiChirpFallback;
            Exception? freeError = null;
            if (choice != TtsOptions.KeyOnlyPaid)
            {
                var blocked = Environment.TickCount64 < Interlocked.Read(ref _freeBlockedUntil);
                if (free.Length > 0 && !blocked)
                {
                    // Mit Ausweichweg nicht erst bei 429 warten — der naechste Weg ist schneller als jede Wartezeit.
                    var fallback = chirp || (choice == TtsOptions.KeyFreeThenPaid && paid.Length > 0);
                    try
                    {
                        return await GeminiRequestAsync(text, model, voiceId, free, "kostenlos", waitOn429: !fallback, ct).ConfigureAwait(false);
                    }
                    catch (TtsException ex)
                    {
                        freeError = ex;
                        if (fallback) Interlocked.Exchange(ref _freeBlockedUntil, Environment.TickCount64 + FreePauseMs);
                        Log.Info($"Gemini-Stimme: kostenloser Schluessel gescheitert (modell={model}): {Trim(ex.Message, 120)}");
                    }
                }
                if (chirp)
                {
                    Log.Info($"Gemini-Stimme: Rueckfall auf Chirp 3 HD (stimme={GoogleTtsVoices.GeminiName(voiceId)}, gesperrt={blocked})");
                    return await GoogleAsync(text, languageCode, GoogleTtsVoices.ChirpFor(voiceId), ct).ConfigureAwait(false);
                }
                if (choice == TtsOptions.KeyOnlyFree)
                    throw freeError ?? new TtsException("Vorlesen fehlgeschlagen: Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.");
            }
            if (paid.Length == 0)
                throw freeError ?? new TtsException("Vorlesen fehlgeschlagen: Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.");
            return await GeminiRequestAsync(text, model, voiceId, paid, "bezahlt", waitOn429: true, ct).ConfigureAwait(false);
        }

        private async Task<byte[]> GeminiRequestAsync(string text, string model, string voiceId, string key, string kind, bool waitOn429, CancellationToken ct)
        {
            var started = Environment.TickCount64;
            // Gemini liest den Text woertlich vor; eine Anweisung im Text wuerde mitgesprochen.
            var payload = new
            {
                contents = new[] { new { parts = new[] { new { text } } } },
                generationConfig = new
                {
                    responseModalities = new[] { "AUDIO" },
                    speechConfig = new
                    {
                        voiceConfig = new { prebuiltVoiceConfig = new { voiceName = GoogleTtsVoices.GeminiName(voiceId) } }
                    }
                }
            };
            var json = JsonSerializer.Serialize(payload);
            var body = await SendWithRetryAsync(() =>
            {
                var req = new HttpRequestMessage(HttpMethod.Post, $"{GeminiUrl}{model}:generateContent")
                {
                    Content = new StringContent(json, Encoding.UTF8, "application/json")
                };
                req.Headers.Add("x-goog-api-key", key);
                return req;
            }, waitOn429, ct).ConfigureAwait(false);

            string finish = "", data = "", mime = "";
            using (var doc = JsonDocument.Parse(body))
            {
                if (doc.RootElement.TryGetProperty("candidates", out var cands) && cands.ValueKind == JsonValueKind.Array && cands.GetArrayLength() > 0)
                {
                    var cand = cands[0];
                    if (cand.TryGetProperty("finishReason", out var fr)) finish = fr.GetString() ?? "";
                    if (cand.TryGetProperty("content", out var c) && c.TryGetProperty("parts", out var parts)
                        && parts.ValueKind == JsonValueKind.Array && parts.GetArrayLength() > 0
                        && parts[0].TryGetProperty("inlineData", out var inline))
                    {
                        if (inline.TryGetProperty("data", out var d)) data = d.GetString() ?? "";
                        if (inline.TryGetProperty("mimeType", out var mt)) mime = mt.GetString() ?? "";
                    }
                }
            }
            if (data.Length == 0) throw new TtsException($"Gemini hat kein Audio geliefert ({finish}).");
            if (finish.Length > 0 && finish != "STOP") throw new TtsException($"Gemini hat die Stimme abgebrochen ({finish}).");

            var raw = Convert.FromBase64String(data);
            var rm = Regex.Match(mime, @"rate=(\d+)");
            var rate = rm.Success && int.TryParse(rm.Groups[1].Value, out var r) ? r : 24_000;
            var wav = raw.Length > 44 && Encoding.ASCII.GetString(raw, 0, 4) == "RIFF" ? raw : WithWavHeader(raw, rate);
            // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
            var seconds = (wav.Length - 44) / (WavRate(wav) * 2.0);
            if (text.Length >= 60 && seconds < text.Length / 25.0)
                throw new TtsException($"Gemini-Audio zu kurz ({seconds:F1} s für {text.Length} Zeichen).");
            Log.Info($"TTS: Gemini-Absatz erzeugt (modell={model} schluessel={kind} zeichen={text.Length}, {Environment.TickCount64 - started} ms)");
            return wav;
        }

        /// <summary>Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der Player es abspielt.</summary>
        public static byte[] WithWavHeader(byte[] pcm, int rate)
        {
            var wav = new byte[44 + pcm.Length];
            void Ascii(int at, string s) => Encoding.ASCII.GetBytes(s).CopyTo(wav, at);
            void I32(int at, int v) => BitConverter.GetBytes(v).CopyTo(wav, at);
            void I16(int at, short v) => BitConverter.GetBytes(v).CopyTo(wav, at);
            Ascii(0, "RIFF"); I32(4, 36 + pcm.Length); Ascii(8, "WAVE");
            Ascii(12, "fmt "); I32(16, 16); I16(20, 1); I16(22, 1); I32(24, rate); I32(28, rate * 2); I16(32, 2); I16(34, 16);
            Ascii(36, "data"); I32(40, pcm.Length);
            pcm.CopyTo(wav, 44);
            return wav;
        }

        private static int WavRate(byte[] wav)
        {
            var r = BitConverter.ToInt32(wav, 24);
            return r > 0 ? r : 24_000;
        }

        /// <summary>Schickt die Anfrage; 429 je nach Wunsch mit Wartezeiten wiederholen (Jarvis fuehreAus).</summary>
        private static async Task<string> SendWithRetryAsync(Func<HttpRequestMessage> build, bool waitOn429, CancellationToken ct)
        {
            var attempt = 0;
            while (true)
            {
                using var req = build();
                using var resp = await SharedHttp.SendAsync(req, ct).ConfigureAwait(false);
                var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                var code = (int)resp.StatusCode;
                if (code is >= 200 and <= 299) return body;
                if (code == 429 && waitOn429 && attempt < WaitMs.Length)
                {
                    Log.Info($"TTS: Rate-Limit, warte {WaitMs[attempt]} ms (Versuch {attempt})");
                    await Task.Delay(WaitMs[attempt], ct).ConfigureAwait(false);
                    attempt++;
                    continue;
                }
                if (code is 401 or 403)
                    throw new TtsException($"Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler {code}). Prüf ihn in den Einstellungen.");
                if (code == 429)
                    throw new TtsException("Vorlesen fehlgeschlagen: Das Kontingent der Stimme ist erschöpft (Fehler 429).");
                throw new TtsException($"Die Stimme antwortete mit Fehler {code}: {Trim(body, 200)}");
            }
        }

        // ---------- Azure Neural ----------

        /// <summary>
        /// Azure AI Speech ueber REST: Schluessel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
        /// HD-Stimmen (DragonHD) bekommen keinen Tempo-Rahmen: sie bestimmen Betonung und Tempo selbst.
        /// </summary>
        private async Task<byte[]> AzureAsync(string text, string voiceId, CancellationToken ct)
        {
            var key = Regex.Replace(_opts.AzureKey ?? "", @"\s", "");
            if (key.Length == 0)
                throw new TtsException("Vorlesen fehlgeschlagen: Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.");
            var region = GoogleTtsVoices.AzureRegion(_opts.AzureRegion)
                ?? throw new TtsException($"Vorlesen fehlgeschlagen: Die Azure-Region „{Trim(_opts.AzureRegion ?? "", 40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.");
            var voice = GoogleTtsVoices.AzureName(voiceId);
            var hd = GoogleTtsVoices.IsAzureHd(voiceId);
            // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
            var clean = Regex.Replace(text, "[\u0000-\u0008\u000B\u000C\u000E-\u001F]", " ");
            var escaped = System.Security.SecurityElement.Escape(clean) ?? "";
            // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die uebrigen sprechen ohnehin nur Deutsch.
            var inner = voice.Contains("Multilingual") ? $"<lang xml:lang='de-DE'>{escaped}</lang>" : escaped;
            var ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
                       $"<voice name='{voice}'>{inner}</voice></speak>";
            var started = Environment.TickCount64;

            var attempt = 0;
            while (true)
            {
                using var req = new HttpRequestMessage(HttpMethod.Post, $"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1")
                {
                    Content = new StringContent(ssml, Encoding.UTF8, "application/ssml+xml")
                };
                req.Headers.Add("Ocp-Apim-Subscription-Key", key);
                req.Headers.Add("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3");
                req.Headers.TryAddWithoutValidation("User-Agent", "VoiceAgent");
                using var resp = await SharedHttp.SendAsync(req, ct).ConfigureAwait(false);
                var code = (int)resp.StatusCode;
                if (code is >= 200 and <= 299)
                {
                    var bytes = await resp.Content.ReadAsByteArrayAsync(ct).ConfigureAwait(false);
                    if (bytes.Length == 0) throw new TtsException("Azure hat kein Audio geliefert.");
                    Log.Info($"TTS: Azure-Absatz erzeugt (stimme={voice} zeichen={text.Length}, {Environment.TickCount64 - started} ms)");
                    return bytes;
                }
                var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                // F0 erlaubt nur rund 20 Anfragen je Minute: ein 429 ist meist das Ratenlimit -> nach Retry-After warten.
                if (code == 429 && attempt < AzureWaitMs.Length)
                {
                    var retry = resp.Headers.RetryAfter?.Delta;
                    var ms = retry.HasValue ? (int)Math.Clamp(retry.Value.TotalMilliseconds, 1_000, 60_000) : AzureWaitMs[attempt];
                    Log.Info($"TTS: Azure-Rate-Limit, warte {ms} ms (Versuch {attempt})");
                    await Task.Delay(ms, ct).ConfigureAwait(false);
                    attempt++;
                    continue;
                }
                if (code is 401 or 403)
                    throw new TtsException($"Vorlesen fehlgeschlagen: Azure hat Schlüssel oder Region abgelehnt (Fehler {code}). Prüf beides in den Einstellungen.");
                if (code == 429)
                    throw new TtsException("Vorlesen fehlgeschlagen: Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.");
                if (code == 400 && hd)
                    throw new TtsException("Vorlesen fehlgeschlagen: Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.");
                throw new TtsException($"Azure antwortete mit Fehler {code}: {Trim(body, 200)}");
            }
        }

        private static string Trim(string s, int max) => s.Length <= max ? s : s[..max];
    }
}
