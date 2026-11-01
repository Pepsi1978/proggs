using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Vorlese-Anbieter wie in der Android-App (tts/TtsCatalog.kt): gleiche Kennungen, Stimmen und Rückfallstufen.</summary>
public enum VorleseAnbieter
{
    QwenKlon,
    Qwen,
    Google,
    GeminiFlash,
    GeminiLite,
    Edge,
    Azure,
    System, // nur am PC: beste deutsche Systemstimme, offline
}

public static class VorleseAnbieterMerkmale
{
    extension(VorleseAnbieter a)
    {
        public string RawValue => a switch
        {
            VorleseAnbieter.QwenKlon => "qwen_clone",
            VorleseAnbieter.Qwen => "qwen",
            VorleseAnbieter.Google => "google_cloud",
            VorleseAnbieter.GeminiFlash => "gemini_flash",
            VorleseAnbieter.GeminiLite => "gemini_lite",
            VorleseAnbieter.Edge => "edge_tts",
            VorleseAnbieter.Azure => "azure_neural",
            _ => "system",
        };

        public string Id => a.RawValue;

        public string Titel => a switch
        {
            VorleseAnbieter.QwenKlon => "Meine Stimme",
            VorleseAnbieter.Qwen => "Alibaba",
            VorleseAnbieter.Google => "Google Chirp 3 HD",
            VorleseAnbieter.GeminiFlash => "Gemini 3.8 Flash TTS",
            VorleseAnbieter.GeminiLite => "Gemini 3.8 Flash-Lite TTS",
            VorleseAnbieter.Edge => "Microsoft Edge",
            VorleseAnbieter.Azure => "Azure Neural",
            _ => "Systemstimme (offline)",
        };

        public string Kurz => a switch
        {
            VorleseAnbieter.QwenKlon => "Meine",
            VorleseAnbieter.Qwen => "Alibaba",
            VorleseAnbieter.Google => "Google",
            VorleseAnbieter.GeminiFlash => "Gemini 3.8",
            VorleseAnbieter.GeminiLite => "Gemini 3.8 Lite",
            VorleseAnbieter.Edge => "Edge",
            VorleseAnbieter.Azure => "Azure",
            _ => "System",
        };

        public bool IstGemini => a == VorleseAnbieter.GeminiFlash || a == VorleseAnbieter.GeminiLite;

        /// <summary>Wertet das Sprechtempo aus (Gemini über das Strecken des fertigen Tons); Alibaba spricht im eigenen Tempo.</summary>
        public bool KenntTempo => a == VorleseAnbieter.Google || a == VorleseAnbieter.Edge || a == VorleseAnbieter.Azure || a == VorleseAnbieter.System || a.IstGemini;

        public string Beschreibung => a switch
        {
            VorleseAnbieter.Edge => "Kostenlos, ohne Schlüssel – der garantierte Rückfall.",
            VorleseAnbieter.Google => "Sehr natürliche deutsche Stimmen, eigener Google-Cloud-Schlüssel.",
            VorleseAnbieter.GeminiFlash => "Gemini 3.8 Flash TTS, 30 Stimmen, kostenloser und/oder bezahlter AI-Studio-Schlüssel.",
            VorleseAnbieter.GeminiLite => "Gemini 3.8 Flash-Lite TTS, schneller und günstiger, gleiche 30 Stimmen.",
            VorleseAnbieter.Azure => "Azure AI Speech mit eigenem Schlüssel und Region, 17 Neural-Stimmen plus HD (HD nur Stufe S0).",
            VorleseAnbieter.Qwen => "Alibaba Qwen TTS Standardstimmen (DashScope-Schlüssel).",
            VorleseAnbieter.QwenKlon => "Deine geklonte Stimme aus dem Alibaba-Konto (DashScope-Schlüssel).",
            _ => "Die beste deutsche Windows-Stimme, ohne Netz.",
        };
    }

    extension(VorleseAnbieter)
    {
        public static IReadOnlyList<VorleseAnbieter> AllCases => Alle;
        /// <summary>VorleseAnbieter(rawValue:).</summary>
        public static VorleseAnbieter? Aus(string? rawValue) => Alle.Cast<VorleseAnbieter?>().FirstOrDefault(x => x!.Value.RawValue == rawValue);
    }

    private static readonly VorleseAnbieter[] Alle =
    {
        VorleseAnbieter.QwenKlon, VorleseAnbieter.Qwen, VorleseAnbieter.Google, VorleseAnbieter.GeminiFlash,
        VorleseAnbieter.GeminiLite, VorleseAnbieter.Edge, VorleseAnbieter.Azure, VorleseAnbieter.System,
    };
}

public enum Geschlecht { Weiblich, Maennlich }

public sealed record VorleseStimme(string Id, string Name, Geschlecht Geschlecht);

/// <summary>Eine Stufe der Vorlese-Rückfallkette; `Schluessel` nur bei Gemini: „frei“ oder „bezahlt“.</summary>
public sealed record RueckfallStufe(string Id, VorleseAnbieter Anbieter, string? Schluessel, string Titel)
{
    public const string Frei = "frei";
    public const string Bezahlt = "bezahlt";
}

public static class VorleseKatalog
{
    public const string StandardEdge = "de-DE-SeraphinaMultilingualNeural";
    public const string StandardGoogle = "de-DE-Chirp3-HD-Kore";
    public const string StandardQwen = "Cherry";
    public const string StandardGeminiFlash = "gemini_flash:Leda";
    public const string StandardGeminiLite = "gemini_lite:Leda";
    public const string StandardAzure = "azure:de-DE-SeraphinaMultilingualNeural";
    public const string StandardAzureRegion = "westeurope";

    public const string GeminiFlashModell = "gemini-3.8-flash-tts";
    public const string GeminiLiteModell = "gemini-3.8-flash-lite-tts";
    public const string QwenModell = "qwen3-tts-flash";
    public const string QwenKlonModell = "qwen3-tts-vc-2026-01-22";

    private static VorleseStimme W(string id, string name) => new(id, name, Geschlecht.Weiblich);
    private static VorleseStimme M(string id, string name) => new(id, name, Geschlecht.Maennlich);

    public static readonly IReadOnlyList<VorleseStimme> EdgeStimmen = new[]
    {
        W("de-DE-SeraphinaMultilingualNeural", "Seraphina"),
        W("de-DE-KatjaNeural", "Katja"),
        W("de-DE-AmalaNeural", "Amala"),
        M("de-DE-FlorianMultilingualNeural", "Florian"),
        M("de-DE-KillianNeural", "Killian"),
        M("de-DE-ConradNeural", "Conrad"),
    };

    public static readonly IReadOnlyList<VorleseStimme> QwenStimmen = new[]
    {
        W("Cherry", "Cherry"), W("Chelsie", "Chelsie"), W("Serena", "Serena"), W("Jada", "Jada"), W("Sunny", "Sunny"),
        M("Ethan", "Ethan"), M("Dylan", "Dylan"),
    };

    private static readonly string[] ChirpWeiblich =
    {
        "Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Kore",
        "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr",
    };
    private static readonly string[] ChirpMaennlich =
    {
        "Achird", "Algenib", "Algieba", "Alnilam", "Charon", "Enceladus", "Fenrir", "Iapetus",
        "Orus", "Puck", "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi",
    };

    public static readonly IReadOnlyList<VorleseStimme> GoogleStimmen =
        ChirpWeiblich.Select(n => W($"de-DE-Chirp3-HD-{n}", n)).Concat(ChirpMaennlich.Select(n => M($"de-DE-Chirp3-HD-{n}", n))).ToList();

    /// <summary>Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD; die Kennung trägt den Anbieter vorn („gemini_flash:Leda“).</summary>
    public static readonly IReadOnlyList<VorleseStimme> GeminiFlashStimmen = GoogleStimmen.Select(s => new VorleseStimme($"gemini_flash:{s.Name}", s.Name, s.Geschlecht)).ToList();
    public static readonly IReadOnlyList<VorleseStimme> GeminiLiteStimmen = GoogleStimmen.Select(s => new VorleseStimme($"gemini_lite:{s.Name}", s.Name, s.Geschlecht)).ToList();

    public static string GeminiName(string id) => id.Split(':').Last();
    /// <summary>Dieselbe Stimme bei Chirp 3 HD (Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert).</summary>
    public static string ChirpZu(string id) => $"de-DE-Chirp3-HD-{GeminiName(id)}";

    public static readonly IReadOnlyList<VorleseStimme> AzureStimmen = new[]
    {
        W("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD"),
        M("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD"),
        W("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina"),
        W("azure:de-DE-KatjaNeural", "Katja"),
        W("azure:de-DE-AmalaNeural", "Amala"),
        W("azure:de-DE-ElkeNeural", "Elke"),
        W("azure:de-DE-GiselaNeural", "Gisela (Kind)"),
        W("azure:de-DE-KlarissaNeural", "Klarissa"),
        W("azure:de-DE-LouisaNeural", "Louisa"),
        W("azure:de-DE-MajaNeural", "Maja"),
        W("azure:de-DE-TanjaNeural", "Tanja"),
        M("azure:de-DE-FlorianMultilingualNeural", "Florian"),
        M("azure:de-DE-ConradNeural", "Conrad"),
        M("azure:de-DE-KillianNeural", "Killian"),
        M("azure:de-DE-BerndNeural", "Bernd"),
        M("azure:de-DE-ChristophNeural", "Christoph"),
        M("azure:de-DE-KasperNeural", "Kasper"),
        M("azure:de-DE-KlausNeural", "Klaus"),
        M("azure:de-DE-RalfNeural", "Ralf"),
    };

    public static string AzureName(string id) => id.StartsWith("azure:", StringComparison.Ordinal) ? id[6..] : id;
    public static bool IstAzureHd(string id) => id.Contains("DragonHD");

    /// <summary>Regionsname („westeurope“); nimmt auch einen eingefügten Endpunkt aus dem Azure-Portal. null = unbrauchbar.</summary>
    public static string? AzureRegion(string eingabe)
    {
        var roh = eingabe.Trim().ToLowerInvariant();
        if (roh.Length == 0) roh = StandardAzureRegion;
        var m = Regex.Match(roh, @"^(?:https?://)?([a-z0-9]+)\.(?:api\.cognitive|tts\.speech)\.");
        if (m.Success) roh = m.Groups[1].Value;
        return Regex.IsMatch(roh, @"^[a-z0-9]{2,40}$") ? roh : null;
    }

    public static readonly IReadOnlyList<RueckfallStufe> RueckfallStufen = new[]
    {
        new RueckfallStufe("gemini_flash:frei", VorleseAnbieter.GeminiFlash, RueckfallStufe.Frei, "Gemini 3.8 Flash TTS (kostenlos)"),
        new RueckfallStufe("gemini_flash:bezahlt", VorleseAnbieter.GeminiFlash, RueckfallStufe.Bezahlt, "Gemini 3.8 Flash TTS (bezahlt)"),
        new RueckfallStufe("gemini_lite:frei", VorleseAnbieter.GeminiLite, RueckfallStufe.Frei, "Gemini 3.8 Flash-Lite TTS (kostenlos)"),
        new RueckfallStufe("gemini_lite:bezahlt", VorleseAnbieter.GeminiLite, RueckfallStufe.Bezahlt, "Gemini 3.8 Flash-Lite TTS (bezahlt)"),
        new RueckfallStufe(VorleseAnbieter.Google.Id, VorleseAnbieter.Google, null, VorleseAnbieter.Google.Titel),
        new RueckfallStufe(VorleseAnbieter.Azure.Id, VorleseAnbieter.Azure, null, VorleseAnbieter.Azure.Titel),
        new RueckfallStufe(VorleseAnbieter.Edge.Id, VorleseAnbieter.Edge, null, VorleseAnbieter.Edge.Titel),
        new RueckfallStufe(VorleseAnbieter.Qwen.Id, VorleseAnbieter.Qwen, null, VorleseAnbieter.Qwen.Titel),
        new RueckfallStufe(VorleseAnbieter.QwenKlon.Id, VorleseAnbieter.QwenKlon, null, VorleseAnbieter.QwenKlon.Titel),
    };

    /// <summary>rueckfallStufe(_:) in Swift; der Name stößt in C# nicht mit dem Typ zusammen.</summary>
    public static RueckfallStufe? RueckfallStufeZu(string id) => RueckfallStufen.FirstOrDefault(s => s.Id == id);

    /// <summary>Feste Stimmen eines Anbieters (die geklonten kommen aus dem Alibaba-Konto, siehe VorleseEinstellungen.KlonStimmen).</summary>
    public static IReadOnlyList<VorleseStimme> Stimmen(VorleseAnbieter a) => a switch
    {
        VorleseAnbieter.Edge => EdgeStimmen,
        VorleseAnbieter.Google => GoogleStimmen,
        VorleseAnbieter.GeminiFlash => GeminiFlashStimmen,
        VorleseAnbieter.GeminiLite => GeminiLiteStimmen,
        VorleseAnbieter.Azure => AzureStimmen,
        VorleseAnbieter.Qwen => QwenStimmen,
        VorleseAnbieter.QwenKlon => Array.Empty<VorleseStimme>(),
        _ => new[] { new VorleseStimme("system", "Beste deutsche Systemstimme", Geschlecht.Weiblich) },
    };

    /// <summary>Anbieter zu einer Stimm-Kennung aus dem festen Katalog.</summary>
    public static VorleseAnbieter? AnbieterZu(string id)
    {
        if (id == "system") return VorleseAnbieter.System;
        foreach (var a in VorleseAnbieter.AllCases)
            if (Stimmen(a).Any(s => s.Id == id)) return a;
        if (id.StartsWith("qwen-tts-vc-", StringComparison.Ordinal)) return VorleseAnbieter.QwenKlon;
        return null;
    }

    /// <summary>Anzeigename einer geklonten Stimme („qwen-tts-vc-FrankHD-voice-2026…“ → „Frank HD“).</summary>
    public static string KlonName(string id)
    {
        var m = Regex.Match(id, @"^qwen-tts-vc-(.+?)-voice-");
        if (!m.Success) return id;
        var name = m.Groups[1].Value;
        // Alibaba erlaubt keine Leerzeichen: vor einem Großbuchstaben nach einem Kleinbuchstaben trennen.
        var aus = new System.Text.StringBuilder();
        char? vorher = null;
        foreach (var z in name)
        {
            if (vorher is char v && char.IsLower(v) && char.IsUpper(z)) aus.Append(' ');
            aus.Append(z);
            vorher = z;
        }
        return aus.ToString();
    }
}
