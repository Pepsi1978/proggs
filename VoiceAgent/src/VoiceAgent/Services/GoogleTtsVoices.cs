using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

namespace VoiceAgent.Services
{
    public enum VoiceGender { Female, Male }

    /// <summary>Eine auswaehlbare Stimme. Name = Kennung (bei Gemini/Azure mit Anbieter-Praefix).</summary>
    public sealed record GoogleTtsVoice(string Name, string DisplayName, VoiceGender Gender);

    /// <summary>Ein Sprachausgabe-Anbieter (Port aus Jarvis TtsCatalog.kt).</summary>
    public sealed record TtsProviderInfo(string Id, string Label);

    /// <summary>
    /// Stimmkatalog: die 30 deutschen Google "Chirp 3 HD"-Stimmen (Franks "3D-Stimmen"), dazu
    /// Gemini 3.8 Flash TTS, Gemini 3.8 Flash-Lite TTS (dieselben 30 Namen, Kennung "provider:Name")
    /// und Azure Neural ("azure:"-Praefix, inkl. DragonHD). Portiert aus Jarvis TtsCatalog.kt.
    /// Default: de-DE-Chirp3-HD-Kore (weiblich, warm).
    /// </summary>
    public static class GoogleTtsVoices
    {
        public const string DefaultVoiceName = "de-DE-Chirp3-HD-Kore";
        public const string DefaultLanguageCode = "de-DE";

        public const string ProviderGoogle = "google_cloud";
        public const string ProviderGeminiFlash = "gemini_flash";
        public const string ProviderGeminiLite = "gemini_lite";
        public const string ProviderAzure = "azure_neural";

        public const string DefaultGeminiFlashVoice = ProviderGeminiFlash + ":Leda";
        public const string DefaultGeminiLiteVoice = ProviderGeminiLite + ":Leda";
        public const string DefaultAzureVoice = "azure:de-DE-SeraphinaMultilingualNeural";
        public const string DefaultAzureRegion = "westeurope";

        public static readonly IReadOnlyList<TtsProviderInfo> Providers = new List<TtsProviderInfo>
        {
            new(ProviderGoogle, "Google Chirp 3 HD"),
            new(ProviderGeminiFlash, "Gemini 3.8 Flash TTS"),
            new(ProviderGeminiLite, "Gemini 3.8 Flash-Lite TTS"),
            new(ProviderAzure, "Azure Neural"),
        };

        public static readonly IReadOnlyList<GoogleTtsVoice> All = new List<GoogleTtsVoice>
        {
            // Weibliche Stimmen
            new("de-DE-Chirp3-HD-Achernar",     "Achernar — weiblich",        VoiceGender.Female),
            new("de-DE-Chirp3-HD-Aoede",        "Aoede — weiblich",           VoiceGender.Female),
            new("de-DE-Chirp3-HD-Autonoe",      "Autonoe — weiblich",         VoiceGender.Female),
            new("de-DE-Chirp3-HD-Callirrhoe",   "Callirrhoe — weiblich",      VoiceGender.Female),
            new("de-DE-Chirp3-HD-Despina",      "Despina — weiblich",         VoiceGender.Female),
            new("de-DE-Chirp3-HD-Erinome",      "Erinome — weiblich",         VoiceGender.Female),
            new("de-DE-Chirp3-HD-Gacrux",       "Gacrux — weiblich",          VoiceGender.Female),
            new("de-DE-Chirp3-HD-Kore",         "Kore — weiblich (Standard)", VoiceGender.Female),
            new("de-DE-Chirp3-HD-Laomedeia",    "Laomedeia — weiblich",       VoiceGender.Female),
            new("de-DE-Chirp3-HD-Leda",         "Leda — weiblich",            VoiceGender.Female),
            new("de-DE-Chirp3-HD-Pulcherrima",  "Pulcherrima — weiblich",     VoiceGender.Female),
            new("de-DE-Chirp3-HD-Sulafat",      "Sulafat — weiblich",         VoiceGender.Female),
            new("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix — weiblich",    VoiceGender.Female),
            new("de-DE-Chirp3-HD-Zephyr",       "Zephyr — weiblich",          VoiceGender.Female),
            // Maennliche Stimmen
            new("de-DE-Chirp3-HD-Achird",        "Achird — maennlich",        VoiceGender.Male),
            new("de-DE-Chirp3-HD-Algenib",       "Algenib — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Algieba",       "Algieba — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Alnilam",       "Alnilam — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Charon",        "Charon — maennlich",        VoiceGender.Male),
            new("de-DE-Chirp3-HD-Enceladus",     "Enceladus — maennlich",     VoiceGender.Male),
            new("de-DE-Chirp3-HD-Fenrir",        "Fenrir — maennlich",        VoiceGender.Male),
            new("de-DE-Chirp3-HD-Iapetus",       "Iapetus — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Orus",          "Orus — maennlich",          VoiceGender.Male),
            new("de-DE-Chirp3-HD-Puck",          "Puck — maennlich",          VoiceGender.Male),
            new("de-DE-Chirp3-HD-Rasalgethi",    "Rasalgethi — maennlich",    VoiceGender.Male),
            new("de-DE-Chirp3-HD-Sadachbia",     "Sadachbia — maennlich",     VoiceGender.Male),
            new("de-DE-Chirp3-HD-Sadaltager",    "Sadaltager — maennlich",    VoiceGender.Male),
            new("de-DE-Chirp3-HD-Schedar",       "Schedar — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Umbriel",       "Umbriel — maennlich",       VoiceGender.Male),
            new("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi — maennlich", VoiceGender.Male),
        };

        /// <summary>Gemini 3.8 TTS: dieselben 30 Stimmnamen wie Chirp 3 HD, Kennung "gemini_flash:Leda".</summary>
        public static readonly IReadOnlyList<GoogleTtsVoice> GeminiFlash = GeminiVoices(ProviderGeminiFlash);
        public static readonly IReadOnlyList<GoogleTtsVoice> GeminiLite = GeminiVoices(ProviderGeminiLite);

        private static IReadOnlyList<GoogleTtsVoice> GeminiVoices(string provider) =>
            All.Select(v =>
            {
                var shortName = v.Name.Substring("de-DE-Chirp3-HD-".Length);
                var label = v.DisplayName.Replace(" (Standard)", "");
                return new GoogleTtsVoice($"{provider}:{shortName}", label, v.Gender);
            }).ToList();

        /// <summary>
        /// Azure AI Speech (eigenes Konto, Schluessel + Region): offizielle Neural-Stimmen plus HD (DragonHD).
        /// HD zaehlt nicht zum kostenlosen F0-Kontingent.
        /// </summary>
        public static readonly IReadOnlyList<GoogleTtsVoice> Azure = new List<GoogleTtsVoice>
        {
            new("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD — weiblich", VoiceGender.Female),
            new("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD — maennlich", VoiceGender.Male),
            new("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina — weiblich", VoiceGender.Female),
            new("azure:de-DE-KatjaNeural", "Katja — weiblich", VoiceGender.Female),
            new("azure:de-DE-AmalaNeural", "Amala — weiblich", VoiceGender.Female),
            new("azure:de-DE-ElkeNeural", "Elke — weiblich", VoiceGender.Female),
            new("azure:de-DE-GiselaNeural", "Gisela (Kind) — weiblich", VoiceGender.Female),
            new("azure:de-DE-KlarissaNeural", "Klarissa — weiblich", VoiceGender.Female),
            new("azure:de-DE-LouisaNeural", "Louisa — weiblich", VoiceGender.Female),
            new("azure:de-DE-MajaNeural", "Maja — weiblich", VoiceGender.Female),
            new("azure:de-DE-TanjaNeural", "Tanja — weiblich", VoiceGender.Female),
            new("azure:de-DE-FlorianMultilingualNeural", "Florian — maennlich", VoiceGender.Male),
            new("azure:de-DE-ConradNeural", "Conrad — maennlich", VoiceGender.Male),
            new("azure:de-DE-KillianNeural", "Killian — maennlich", VoiceGender.Male),
            new("azure:de-DE-BerndNeural", "Bernd — maennlich", VoiceGender.Male),
            new("azure:de-DE-ChristophNeural", "Christoph — maennlich", VoiceGender.Male),
            new("azure:de-DE-KasperNeural", "Kasper — maennlich", VoiceGender.Male),
            new("azure:de-DE-KlausNeural", "Klaus — maennlich", VoiceGender.Male),
            new("azure:de-DE-RalfNeural", "Ralf — maennlich", VoiceGender.Male),
        };

        public static IReadOnlyList<GoogleTtsVoice> VoicesFor(string providerId) => providerId switch
        {
            ProviderGeminiFlash => GeminiFlash,
            ProviderGeminiLite => GeminiLite,
            ProviderAzure => Azure,
            _ => All,
        };

        public static string DefaultVoiceFor(string providerId) => providerId switch
        {
            ProviderGeminiFlash => DefaultGeminiFlashVoice,
            ProviderGeminiLite => DefaultGeminiLiteVoice,
            ProviderAzure => DefaultAzureVoice,
            _ => DefaultVoiceName,
        };

        /// <summary>Findet den Anbieter zu einer Stimm-Kennung (Chirp, wenn nichts passt).</summary>
        public static string ProviderOf(string? voiceId)
        {
            var id = voiceId ?? "";
            if (id.StartsWith("azure:")) return ProviderAzure;
            if (id.StartsWith(ProviderGeminiFlash + ":")) return ProviderGeminiFlash;
            if (id.StartsWith(ProviderGeminiLite + ":")) return ProviderGeminiLite;
            return ProviderGoogle;
        }

        /// <summary>Der Stimmname, den Gemini erwartet ("Leda").</summary>
        public static string GeminiName(string voiceId)
        {
            var i = voiceId.IndexOf(':');
            return i >= 0 ? voiceId[(i + 1)..] : voiceId;
        }

        /// <summary>Dieselbe Stimme bei Chirp 3 HD — fuer den Rueckfall, wenn der kostenlose Gemini-Schluessel scheitert.</summary>
        public static string ChirpFor(string voiceId) => "de-DE-Chirp3-HD-" + GeminiName(voiceId);

        /// <summary>Der Stimmname, den Azure erwartet ("de-DE-KatjaNeural").</summary>
        public static string AzureName(string voiceId) => voiceId.StartsWith("azure:") ? voiceId["azure:".Length..] : voiceId;

        public static bool IsAzureHd(string voiceId) => voiceId.Contains("DragonHD");

        /// <summary>
        /// Regionsname fuer die Adresse ("westeurope"). Nimmt auch den eingefuegten Endpunkt aus dem Portal;
        /// null, wenn nichts Brauchbares darin steht.
        /// </summary>
        public static string? AzureRegion(string? input)
        {
            var raw = (input ?? "").Trim().ToLowerInvariant();
            if (raw.Length == 0) raw = DefaultAzureRegion;
            var m = Regex.Match(raw, @"^(?:https?://)?([a-z0-9]+)\.(?:api\.cognitive|tts\.speech)\.");
            var region = m.Success ? m.Groups[1].Value : raw;
            return Regex.IsMatch(region, "^[a-z0-9]{2,40}$") ? region : null;
        }

        public static GoogleTtsVoice? FindByName(string name) =>
            All.Concat(GeminiFlash).Concat(GeminiLite).Concat(Azure).FirstOrDefault(v => v.Name == name);

        public static string DisplayNameFor(string name) => FindByName(name)?.DisplayName ?? name;
    }
}
