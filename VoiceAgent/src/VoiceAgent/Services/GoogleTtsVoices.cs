using System.Collections.Generic;
using System.Linq;

namespace VoiceAgent.Services
{
    public enum VoiceGender { Female, Male }

    /// <summary>Eine auswaehlbare Google-TTS-Stimme.</summary>
    public sealed record GoogleTtsVoice(string Name, string DisplayName, VoiceGender Gender);

    /// <summary>
    /// Die 30 deutschen Google "Chirp 3 HD"-Stimmen (Franks "3D-Stimmen"), portiert aus
    /// dem EntropieReductor. Quelle: cloud.google.com/text-to-speech/docs/chirp3-hd.
    /// Default: de-DE-Chirp3-HD-Kore (weiblich, warm).
    /// </summary>
    public static class GoogleTtsVoices
    {
        public const string DefaultVoiceName = "de-DE-Chirp3-HD-Kore";
        public const string DefaultLanguageCode = "de-DE";

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

        public static GoogleTtsVoice? FindByName(string name) => All.FirstOrDefault(v => v.Name == name);

        public static string DisplayNameFor(string name) => FindByName(name)?.DisplayName ?? name;
    }
}
