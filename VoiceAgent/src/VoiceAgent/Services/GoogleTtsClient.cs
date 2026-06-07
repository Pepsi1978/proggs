using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Google Cloud Text-to-Speech (texttospeech.googleapis.com) mit API-Key-Auth.
    /// Synthetisiert Text zu MP3-Audio in einer Chirp-3-HD-Stimme. Nach der
    /// EntropieReductor-Kotlin-Vorlage in C# neu gebaut.
    /// </summary>
    public sealed class GoogleTtsClient
    {
        private readonly string _apiKey;

        private static readonly HttpClient SharedHttp = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(60) };

        public GoogleTtsClient(string apiKey) => _apiKey = apiKey;

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

        /// <summary>Synthetisiert Text zu MP3-Bytes in der gewaehlten Stimme.</summary>
        public async Task<byte[]> SynthesizeAsync(string text, string languageCode, string voiceName, CancellationToken ct = default)
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
                throw new Exception($"Google TTS Fehler {(int)resp.StatusCode}: {body}");
            }

            var audio = ParseAudio(body);
            Log.Info($"TTS: {audio.Length} Bytes Audio erzeugt (Stimme {voiceName}, {text.Length} Zeichen)");
            return audio;
        }
    }
}
