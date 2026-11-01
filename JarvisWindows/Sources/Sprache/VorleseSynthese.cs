using System.Globalization;
using System.Net;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using NAudio.Wave;

namespace JarvisWindows;

/// <summary>Fertig erzeugter Ton eines Absatzes (mp3 oder wav), noch nicht abgespielt.</summary>
public sealed record VorleseTon(byte[] Daten, string Endung);

public enum VorleseFehlerArt
{
    /// <summary>Betrifft die ganze Stimme (Schlüssel fehlt/abgelehnt, Kontingent): weiter zur nächsten Rückfallstufe.</summary>
    Abbruch,
    /// <summary>Fehler an genau diesem Absatz.</summary>
    Fehler,
    /// <summary>Die Stimme hat nicht rechtzeitig geantwortet.</summary>
    Zeitlimit,
}

public sealed class VorleseFehler : Exception
{
    public VorleseFehlerArt Art { get; }
    public VorleseFehler(VorleseFehlerArt art, string text) : base(text) { Art = art; }
    public static VorleseFehler Abbruch(string t) => new(VorleseFehlerArt.Abbruch, t);
    public static VorleseFehler Fehler(string t) => new(VorleseFehlerArt.Fehler, t);
    public static VorleseFehler Zeitlimit(string t) => new(VorleseFehlerArt.Zeitlimit, t);
}

/// <summary>Gemini-Stimmen: nacheinander statt parallel (gemeinsames Minutenlimit), kostenloser Schlüssel nach einem Fehler
/// 10 Minuten übersprungen, wenn es einen Ausweichweg gibt (wie Synthese.kt).</summary>
public sealed class GeminiTor
{
    public static readonly GeminiTor Shared = new();
    private readonly SemaphoreSlim tor = new(1, 1);
    private readonly object schloss = new();
    private readonly Dictionary<string, DateTime> freiGesperrtBis = new();

    public Task Betreten(CancellationToken ct = default) => tor.WaitAsync(ct);
    public void Verlassen() => tor.Release();
    public bool FreiGesperrt(string modell) { lock (schloss) return freiGesperrtBis.TryGetValue(modell, out var bis) && bis > DateTime.Now; }
    public void SperreFrei(string modell) { lock (schloss) freiGesperrtBis[modell] = DateTime.Now.AddSeconds(600); }
}

/// <summary>Erzeugt Vorleseton mit allen Anbietern der Android-App: Endpunkte, Modelle und Anfrageformate aus Synthese.kt,
/// EdgeTtsPlayer.kt und GoogleCloudTtsPlayer.kt. Scheitert die Hauptstimme, geht es die Rückfallkette durch, danach Edge.</summary>
public static class VorleseSynthese
{
    public const string GoogleURL = "https://texttospeech.googleapis.com/v1/text:synthesize";
    public const string GeminiURL = "https://generativelanguage.googleapis.com/v1beta/models/";
    public const string QwenURL = "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private static string OhneLeer(string s) => new(s.Where(c => !char.IsWhiteSpace(c)).ToArray());

    /// <summary>Ton für einen Absatz samt dem Anbieter, der ihn tatsächlich erzeugt hat (für das Tempo-Strecken bei Gemini).</summary>
    public static async Task<(VorleseTon, VorleseAnbieter)> Synthetisiere(string text, VorleseStand s, CancellationToken ct = default)
    {
        var haupt = s.Anbieter;
        Exception hauptFehler;
        try
        {
            return (await Erzeuge(haupt, text, s, null, s.Kette.Count > 0, ct), haupt);
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
        catch (Exception ex) { hauptFehler = ex; }
        var versucht = new HashSet<VorleseAnbieter> { haupt };
        for (int i = 0; i < s.Kette.Count; i++)
        {
            ct.ThrowIfCancellationRequested();
            var stufe = s.Kette[i];
            try
            {
                var ton = await Erzeuge(stufe.Anbieter, text, s, stufe.Schluessel, i < s.Kette.Count - 1, ct);
                Log("sprache", $"Vorlese-Rückfall: {stufe.Titel} statt {haupt.Titel} ({hauptFehler.Message.Schnitt(80)})");
                return (ton, stufe.Anbieter);
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch (Exception ex)
            {
                versucht.Add(stufe.Anbieter);
                Log("fehler", $"Vorlese-Rückfall {stufe.Titel} gescheitert: {ex.Message.Schnitt(120)}");
            }
        }
        // Edge braucht keinen Schlüssel und ist der garantierte Rückfall.
        if (!versucht.Contains(VorleseAnbieter.Edge))
        {
            try
            {
                var ton = await Erzeuge(VorleseAnbieter.Edge, text, s, null, false, ct);
                Log("sprache", $"Vorlese-Rückfall: Microsoft Edge statt {haupt.Titel}");
                return (ton, VorleseAnbieter.Edge);
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch { }
        }
        throw hauptFehler;
    }

    public static Task<VorleseTon> Erzeuge(VorleseAnbieter a, string text, VorleseStand s, string? schluesselStufe, bool mitAusweg, CancellationToken ct = default) => a switch
    {
        VorleseAnbieter.Google => Google(text, s.Stimme(VorleseAnbieter.Google), s, ct),
        VorleseAnbieter.GeminiFlash => Gemini(text, VorleseKatalog.GeminiFlashModell, s.Stimme(VorleseAnbieter.GeminiFlash), s, schluesselStufe, mitAusweg, ct),
        VorleseAnbieter.GeminiLite => Gemini(text, VorleseKatalog.GeminiLiteModell, s.Stimme(VorleseAnbieter.GeminiLite), s, schluesselStufe, mitAusweg, ct),
        VorleseAnbieter.Edge => Edge(text, s.Stimme(VorleseAnbieter.Edge), s.Tempo(VorleseAnbieter.Edge), s.ImmerDeutsch, ct),
        VorleseAnbieter.Azure => Azure(text, s.Stimme(VorleseAnbieter.Azure), s, ct),
        VorleseAnbieter.Qwen => Qwen(text, false, s, ct),
        VorleseAnbieter.QwenKlon => Qwen(text, true, s, ct),
        _ => Task.FromException<VorleseTon>(VorleseFehler.Abbruch("Systemstimme wird direkt gesprochen.")),
    };

    // MARK: HTTP mit Wiederholung

    private static readonly double[] Wartezeiten = { 1, 3, 8 };

    private static string Anfang(byte[] d, int n) => Encoding.UTF8.GetString(d, 0, Math.Min(d.Length, n));

    /// <summary>Führt die Anfrage aus. 5xx und Netzfehler werden wiederholt; 429 wartet nur mit `warteBei429` (retryDelay bis 65 s).</summary>
    public static async Task<byte[]> FuehreAus(Func<HttpRequestMessage> anfrage, double zeitlimit, bool warteBei429 = false, string name = "", CancellationToken ct = default)
    {
        var versuch = 0;
        while (true)
        {
            ct.ThrowIfCancellationRequested();
            try
            {
                using var req = anfrage();
                using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
                frist.CancelAfter(TimeSpan.FromSeconds(zeitlimit));
                using var antwort = await SprachHttp.Client.SendAsync(req, frist.Token).ConfigureAwait(false);
                var daten = await antwort.Content.ReadAsByteArrayAsync(frist.Token).ConfigureAwait(false);
                var code = (int)antwort.StatusCode;
                var text = Anfang(daten, 400);
                if (code >= 200 && code < 300) return daten;
                if (code == 401 || code == 403) throw VorleseFehler.Abbruch($"{name} hat den Schlüssel abgelehnt (Fehler {code}).");
                if (code == 429)
                {
                    double? warte = null;
                    var m = Regex.Match(text, @"""retryDelay""\s*:\s*""(\d+(?:\.\d+)?)s""");
                    if (m.Success && double.TryParse(m.Groups[1].Value, NumberStyles.Float, CultureInfo.InvariantCulture, out var w)) warte = w;
                    if (warteBei429 && versuch < 3 && (warte ?? 5) <= 65)
                    {
                        await Task.Delay(TimeSpan.FromSeconds(warte ?? 5), ct).ConfigureAwait(false);
                        versuch++;
                        continue;
                    }
                    throw VorleseFehler.Abbruch($"{name}: Kontingent oder Ratenlimit erschöpft (Fehler 429).");
                }
                if (code >= 500)
                {
                    if (versuch < Wartezeiten.Length)
                    {
                        await Task.Delay(TimeSpan.FromSeconds(Wartezeiten[versuch]), ct).ConfigureAwait(false);
                        versuch++;
                        continue;
                    }
                    throw VorleseFehler.Fehler($"{name} antwortete mit Fehler {code}.");
                }
                throw VorleseFehler.Fehler($"{name} antwortete mit Fehler {code}: {text.Schnitt(200)}");
            }
            catch (VorleseFehler) { throw; }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch (Exception ex)
            {
                if (versuch < Wartezeiten.Length - 1)
                {
                    await Task.Delay(TimeSpan.FromSeconds(Wartezeiten[versuch]), ct).ConfigureAwait(false);
                    versuch++;
                    continue;
                }
                throw VorleseFehler.Fehler($"{name} nicht erreichbar: {ex.Message}");
            }
        }
    }

    private static HttpRequestMessage JsonAnfrage(string url, object rumpf, params (string, string)[] kopf)
    {
        var req = new HttpRequestMessage(HttpMethod.Post, url);
        foreach (var (k, v) in kopf) req.Headers.TryAddWithoutValidation(k, v);
        req.Content = new ByteArrayContent(JSON.Daten(rumpf));
        req.Content.Headers.TryAddWithoutValidation("Content-Type", "application/json; charset=utf-8");
        return req;
    }

    public static string SsmlEscape(string t) => t.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;");

    public static string OhneSteuerzeichen(string t) => new(t.Select(c => c < 0x20 && c != '\n' && c != '\r' && c != '\t' ? ' ' : c).ToArray());

    // MARK: Google Chirp 3 HD

    /// <summary>Feste deutsche Aussprachen (IPA) für Wörter, die Chirp sonst englisch spricht (DeutscheAussprache.kt).</summary>
    private static readonly (string, string)[] Aussprachen = { ("Frank", "fʁaŋk"), ("Franks", "fʁaŋks") };

    public static async Task<VorleseTon> Google(string text, string stimme, VorleseStand s, CancellationToken ct = default)
    {
        var schluessel = s.Zugang.Google.Trim();
        if (schluessel.Length == 0) throw VorleseFehler.Abbruch("Es ist kein Google-Schlüssel hinterlegt.");
        var liste = Aussprachen.Where(a => Regex.IsMatch(text, $@"\b{a.Item1}\b"))
            .Select(a => (object?)new JSONObjekt { ["phrase"] = a.Item1, ["phoneticEncoding"] = "PHONETIC_ENCODING_IPA", ["pronunciation"] = a.Item2 }).ToList();
        // Chirp versteht kein SSML: Aussprachen als customPronunciations; lehnt Google sie ab (400), ohne sie erneut.
        if (liste.Count > 0)
        {
            try { return await GoogleAnfrage(text, stimme, schluessel, liste, s.Tempo(VorleseAnbieter.Google), ct).ConfigureAwait(false); }
            catch (VorleseFehler f) when (f.Art == VorleseFehlerArt.Fehler && f.Message.Contains("Fehler 400")) { }
        }
        return await GoogleAnfrage(text, stimme, schluessel, null, s.Tempo(VorleseAnbieter.Google), ct).ConfigureAwait(false);
    }

    private static async Task<VorleseTon> GoogleAnfrage(string text, string stimme, string schluessel, List<object?>? aussprachen, double tempo, CancellationToken ct)
    {
        var url = $"{GoogleURL}?key={Uri.EscapeDataString(schluessel)}";
        var eingabe = new JSONObjekt { ["text"] = text };
        if (aussprachen != null) eingabe["customPronunciations"] = new JSONObjekt { ["pronunciations"] = aussprachen };
        var rumpf = new JSONObjekt
        {
            ["input"] = eingabe,
            ["voice"] = new JSONObjekt { ["languageCode"] = "de-DE", ["name"] = stimme },
            // Chirp 3 HD kennt keinen pitch-Parameter: er wird gar nicht erst gesendet (sonst 400).
            ["audioConfig"] = new JSONObjekt { ["audioEncoding"] = "MP3", ["speakingRate"] = Math.Min(Math.Max(tempo, 0.25), 4) },
        };
        var daten = await FuehreAus(() => JsonAnfrage(url, rumpf), 60, name: "Google", ct: ct).ConfigureAwait(false);
        byte[]? mp3 = null;
        try { if (JSON.Objekt(daten)?.Text("audioContent") is string b64) mp3 = Convert.FromBase64String(b64); } catch (FormatException) { }
        if (mp3 == null || mp3.Length == 0) throw VorleseFehler.Fehler("Google hat kein Audio geliefert.");
        return new VorleseTon(mp3, "mp3");
    }

    // MARK: Gemini 3.8 Flash / Flash-Lite TTS

    public static async Task<VorleseTon> Gemini(string text, string modell, string stimme, VorleseStand s, string? nur, bool mitAusweg, CancellationToken ct = default)
    {
        var wahl = nur switch
        {
            RueckfallStufe.Frei => VorleseEinstellungen.GeminiNurFrei,
            RueckfallStufe.Bezahlt => VorleseEinstellungen.GeminiNurBezahlt,
            _ => s.GeminiWahl,
        };
        var frei = OhneLeer(s.Zugang.GeminiFrei);
        var bezahlt = OhneLeer(s.Zugang.GeminiBezahlt);
        Exception? fehlerFrei = null;
        if (wahl != VorleseEinstellungen.GeminiNurBezahlt)
        {
            var gesperrt = GeminiTor.Shared.FreiGesperrt(modell);
            // Mit Ausweichweg nicht erst bei 429 warten: der nächste Weg ist schneller als jede Wartezeit.
            var ausweg = mitAusweg || (wahl == VorleseEinstellungen.GeminiFreiDannBezahlt && bezahlt.Length > 0);
            if (frei.Length > 0 && !gesperrt)
            {
                try
                {
                    return await GeminiAnfrage(text, modell, stimme, frei, !ausweg, ct).ConfigureAwait(false);
                }
                catch (VorleseFehler f)
                {
                    fehlerFrei = f;
                    if (ausweg) GeminiTor.Shared.SperreFrei(modell);
                    Log("fehler", $"Gemini-Stimme: kostenloser Schlüssel gescheitert ({modell}): {f.Message.Schnitt(120)}");
                }
            }
            else if (frei.Length > 0 && wahl == VorleseEinstellungen.GeminiNurFrei)
            {
                fehlerFrei = VorleseFehler.Abbruch("Der kostenlose Gemini-Schlüssel pausiert nach einem Fehler.");
            }
            if (wahl == VorleseEinstellungen.GeminiNurFrei)
                throw fehlerFrei ?? VorleseFehler.Abbruch("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt.");
        }
        if (bezahlt.Length == 0)
            throw fehlerFrei ?? VorleseFehler.Abbruch("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt.");
        return await GeminiAnfrage(text, modell, stimme, bezahlt, true, ct).ConfigureAwait(false);
    }

    private static async Task<VorleseTon> GeminiAnfrage(string text, string modell, string stimme, string schluessel, bool warteBei429, CancellationToken ct)
    {
        // Gemini liest den Text wörtlich vor; eine Anweisung im Text würde mitgesprochen.
        var rumpf = new JSONObjekt
        {
            ["contents"] = new List<object?> { new JSONObjekt { ["parts"] = new List<object?> { new JSONObjekt { ["text"] = text } } } },
            ["generationConfig"] = new JSONObjekt
            {
                ["responseModalities"] = new List<object?> { "AUDIO" },
                ["speechConfig"] = new JSONObjekt
                {
                    ["voiceConfig"] = new JSONObjekt { ["prebuiltVoiceConfig"] = new JSONObjekt { ["voiceName"] = VorleseKatalog.GeminiName(stimme) } },
                },
            },
        };
        await GeminiTor.Shared.Betreten(ct).ConfigureAwait(false);
        byte[] daten;
        try
        {
            daten = await FuehreAus(() => JsonAnfrage($"{GeminiURL}{modell}:generateContent", rumpf, ("x-goog-api-key", schluessel)), 90, warteBei429, "Gemini", ct).ConfigureAwait(false);
        }
        finally { GeminiTor.Shared.Verlassen(); }
        var kandidat = JSON.Objekt(daten)?.Objekte("candidates").FirstOrDefault();
        var ende = kandidat?.Text("finishReason") ?? "";
        var teil = kandidat?.Obj("content")?.Objekte("parts").FirstOrDefault()?.Obj("inlineData");
        byte[]? roh = null;
        try { if (teil?.Text("data") is string b64) roh = Convert.FromBase64String(b64); } catch (FormatException) { }
        if (roh == null || roh.Length == 0) throw VorleseFehler.Fehler($"Gemini hat kein Audio geliefert ({ende}).");
        if (ende.Length > 0 && ende != "STOP") throw VorleseFehler.Fehler($"Gemini hat die Stimme abgebrochen ({ende}).");
        var mime = teil?.Text("mimeType") ?? "";
        var rate = 24_000;
        var m = Regex.Match(mime, @"rate=(\d+)");
        if (m.Success && int.TryParse(m.Groups[1].Value, out var r)) rate = r;
        var istWav = roh.Length >= 12 && roh[0] == 'R' && roh[1] == 'I' && roh[2] == 'F' && roh[3] == 'F';
        var wav = istWav ? roh : WavKopf.Mit(roh, rate);
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        if (!istWav)
        {
            var sekunden = (double)roh.Length / (rate * 2);
            if (text.Length >= 60 && sekunden < text.Length / 25.0)
                throw VorleseFehler.Fehler(string.Create(CultureInfo.InvariantCulture, $"Gemini-Audio zu kurz ({sekunden:0.0} s für {text.Length} Zeichen)."));
        }
        return new VorleseTon(wav, "wav");
    }

    // MARK: Azure Neural

    public static async Task<VorleseTon> Azure(string text, string stimmeId, VorleseStand s, CancellationToken ct = default)
    {
        var schluessel = OhneLeer(s.Zugang.Azure);
        if (schluessel.Length == 0) throw VorleseFehler.Abbruch("Es ist kein Azure-Schlüssel hinterlegt.");
        var region = VorleseKatalog.AzureRegion(s.Zugang.AzureRegion);
        if (region == null) throw VorleseFehler.Abbruch($"Die Azure-Region „{s.Zugang.AzureRegion.Schnitt(40)}“ verstehe ich nicht (etwa westeurope).");
        var stimme = VorleseKatalog.AzureName(stimmeId);
        var hd = VorleseKatalog.IstAzureHd(stimmeId);
        var escaped = SsmlEscape(OhneSteuerzeichen(text));
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen.
        var inhalt = s.ImmerDeutsch && stimme.Contains("Multilingual") ? $"<lang xml:lang='de-DE'>{escaped}</lang>" : escaped;
        var prozent = (int)Math.Round((Math.Min(Math.Max(s.Tempo(VorleseAnbieter.Azure), 0.7), 1.3) - 1) * 100, MidpointRounding.AwayFromZero);
        // HD-Stimmen bestimmen Betonung und Tempo selbst.
        var mitTempo = hd || prozent == 0 ? inhalt : $"<prosody rate='{(prozent > 0 ? "+" : "")}{prozent}%'>{inhalt}</prosody>";
        var ssml = $"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'><voice name='{stimme}'>{mitTempo}</voice></speak>";
        // F0 erlaubt rund 20 Anfragen je Minute: Ein 429 ist meist das Ratenlimit, nach Retry-After warten.
        double[] wartezeiten = { 3, 10, 20, 30 };
        var versuch = 0;
        while (true)
        {
            ct.ThrowIfCancellationRequested();
            using var req = new HttpRequestMessage(HttpMethod.Post, $"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1");
            req.Headers.TryAddWithoutValidation("Ocp-Apim-Subscription-Key", schluessel);
            req.Headers.TryAddWithoutValidation("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3");
            req.Headers.Remove("User-Agent");
            req.Headers.TryAddWithoutValidation("User-Agent", "Jarvis");
            req.Content = new ByteArrayContent(Encoding.UTF8.GetBytes(ssml));
            req.Content.Headers.TryAddWithoutValidation("Content-Type", "application/ssml+xml");
            byte[] daten;
            int code;
            double? nachricht;
            try
            {
                using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
                frist.CancelAfter(TimeSpan.FromSeconds(60));
                using var antwort = await SprachHttp.Client.SendAsync(req, frist.Token).ConfigureAwait(false);
                daten = await antwort.Content.ReadAsByteArrayAsync(frist.Token).ConfigureAwait(false);
                code = (int)antwort.StatusCode;
                nachricht = antwort.Headers.RetryAfter?.Delta?.TotalSeconds;
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
            catch (Exception ex)
            {
                throw VorleseFehler.Fehler($"Azure nicht erreichbar: {ex.Message}");
            }
            if (code >= 200 && code < 300)
            {
                if (daten.Length == 0) throw VorleseFehler.Fehler("Azure hat kein Audio geliefert.");
                return new VorleseTon(daten, "mp3");
            }
            if (code == 429 && versuch < wartezeiten.Length)
            {
                var sek = nachricht is double n ? Math.Min(Math.Max(n, 1), 60) : wartezeiten[versuch];
                await Task.Delay(TimeSpan.FromSeconds(sek), ct).ConfigureAwait(false);
                versuch++;
                continue;
            }
            if (code == 401 || code == 403) throw VorleseFehler.Abbruch($"Azure hat Schlüssel oder Region abgelehnt (Fehler {code}).");
            if (code == 429) throw VorleseFehler.Abbruch("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.");
            if (code == 400 && hd) throw VorleseFehler.Abbruch("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit Stufe S0 und nicht in jeder Region.");
            throw VorleseFehler.Fehler($"Azure antwortete mit Fehler {code}: {Anfang(daten, 200)}");
        }
    }

    // MARK: Alibaba Qwen (Standardstimmen und geklonte Stimme)

    public static async Task<VorleseTon> Qwen(string text, bool geklont, VorleseStand s, CancellationToken ct = default)
    {
        var schluessel = OhneLeer(s.Zugang.Qwen);
        var stimme = OhneLeer(geklont ? s.Stimme(VorleseAnbieter.QwenKlon) : s.Stimme(VorleseAnbieter.Qwen));
        if (schluessel.Length == 0) throw VorleseFehler.Abbruch("Der Alibaba-Schlüssel fehlt.");
        if (stimme.Length == 0) throw VorleseFehler.Abbruch("Die eigene Stimme ist nicht eingerichtet (Stimmen aus dem Alibaba-Konto laden).");
        var rumpf = new JSONObjekt
        {
            ["model"] = geklont ? VorleseKatalog.QwenKlonModell : VorleseKatalog.QwenModell,
            ["input"] = new JSONObjekt { ["text"] = text, ["voice"] = stimme, ["language_type"] = "German" },
        };
        var daten = await FuehreAus(() => JsonAnfrage(QwenURL, rumpf, ("Authorization", $"Bearer {schluessel}")), 90, name: "Alibaba", ct: ct).ConfigureAwait(false);
        var adresse = JSON.Objekt(daten)?.Obj("output")?.Obj("audio")?.Text("url");
        if (string.IsNullOrEmpty(adresse)) throw VorleseFehler.Fehler("Alibaba hat kein Audio geliefert.");
        if (adresse.StartsWith("http://", StringComparison.Ordinal)) adresse = "https://" + adresse[7..];
        if (!Uri.TryCreate(adresse, UriKind.Absolute, out var url)) throw VorleseFehler.Fehler("Alibaba lieferte eine unlesbare Adresse.");
        var ton = await FuehreAus(() => new HttpRequestMessage(HttpMethod.Get, url), 60, name: "Alibaba-Download", ct: ct).ConfigureAwait(false);
        return new VorleseTon(ton, "wav");
    }

    // MARK: Microsoft Edge (WebSocket wie EdgeTtsPlayer.kt)

    public const string EdgeToken = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";  // öffentlicher Edge-Vorlese-Token (wie edge-tts), kein Geheimnis gitleaks:allow
    public const string ChromiumVoll = "143.0.3650.75";
    public const string ChromiumHaupt = "143";
    public const string EdgeURL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1";
    public const string EdgeKonfig = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
        """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}""";

    public static string SecMsGec()
    {
        var jetzt = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var gerundet = jetzt - (jetzt % 300);
        var ticks = (gerundet + 11_644_473_600) * 10_000_000;
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes($"{ticks}{EdgeToken}")));
    }

    public static string EdgeSsml(string text, string stimme, double tempo, bool deutsch)
    {
        var anfrageId = Guid.NewGuid().ToString("N").ToUpperInvariant();
        var escaped = SsmlEscape(text);
        var prozent = (int)Math.Round((Math.Min(Math.Max(tempo, 0.7), 1.3) - 1) * 100, MidpointRounding.AwayFromZero);
        var rate = prozent >= 0 ? $"+{prozent}%" : $"{prozent}%";
        // Der <lang>-Rahmen hält mehrsprachige Stimmen bei Deutsch.
        var inhalt = deutsch ? $"<lang xml:lang='de-DE'>{escaped}</lang>" : escaped;
        return $"X-RequestId:{anfrageId}\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
            $"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'><voice name='{stimme}'>" +
            $"<prosody rate='{rate}' pitch='+0%'>{inhalt}</prosody></voice></speak>";
    }

    private sealed class EdgeZustand
    {
        public readonly object Schloss = new();
        public DateTime Beginn = DateTime.Now;
        public DateTime? LetzterRahmen;
        public bool Abgelaufen;
        public readonly MemoryStream Daten = new();
    }

    public static async Task<VorleseTon> Edge(string text, string stimme, double tempo, bool deutsch, CancellationToken ct = default)
    {
        var verbindung = Guid.NewGuid().ToString("N");
        var adresse = $"{EdgeURL}?TrustedClientToken={EdgeToken}&Sec-MS-GEC={SecMsGec()}&Sec-MS-GEC-Version=1-{ChromiumVoll}&ConnectionId={verbindung}";
        using var socket = new ClientWebSocket();
        socket.Options.SetRequestHeader("User-Agent", $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{ChromiumHaupt}.0.0.0 Safari/537.36 Edg/{ChromiumHaupt}.0.0.0");
        socket.Options.SetRequestHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold");
        socket.Options.SetRequestHeader("Pragma", "no-cache");
        socket.Options.SetRequestHeader("Cache-Control", "no-cache");
        socket.Options.SetRequestHeader("Cookie", $"muid={Guid.NewGuid().ToString("N").ToUpperInvariant()};");
        var z = new EdgeZustand();
        using var abbruch = CancellationTokenSource.CreateLinkedTokenSource(ct);
        // Fristen wie Synthese.kt: erste Antwort, Leerlauf zwischen Rahmen, insgesamt.
        using var waechterEnde = new CancellationTokenSource();
        var waechter = Task.Run(async () =>
        {
            try
            {
                while (!waechterEnde.IsCancellationRequested)
                {
                    await Task.Delay(500, waechterEnde.Token).ConfigureAwait(false);
                    bool ab;
                    lock (z.Schloss)
                    {
                        var jetzt = DateTime.Now;
                        var leer = z.LetzterRahmen is DateTime l ? (jetzt - l).TotalSeconds > 15 : (jetzt - z.Beginn).TotalSeconds > 20;
                        if (leer || (jetzt - z.Beginn).TotalSeconds > 60) z.Abgelaufen = true;
                        ab = z.Abgelaufen;
                    }
                    if (ab) { abbruch.Cancel(); return; }
                }
            }
            catch (OperationCanceledException) { }
        });
        try
        {
            await socket.ConnectAsync(new Uri(adresse), SprachHttp.Invoker, abbruch.Token).ConfigureAwait(false);
            await socket.SendAsync(Encoding.UTF8.GetBytes(EdgeKonfig), WebSocketMessageType.Text, true, abbruch.Token).ConfigureAwait(false);
            await socket.SendAsync(Encoding.UTF8.GetBytes(EdgeSsml(text, stimme, tempo, deutsch)), WebSocketMessageType.Text, true, abbruch.Token).ConfigureAwait(false);
            var puffer = new byte[32 * 1024];
            using var nachricht = new MemoryStream();
            while (true)
            {
                abbruch.Token.ThrowIfCancellationRequested();
                var r = await socket.ReceiveAsync(puffer, abbruch.Token).ConfigureAwait(false);
                if (r.MessageType == WebSocketMessageType.Close) throw new WebSocketException("Verbindung geschlossen");
                nachricht.Write(puffer, 0, r.Count);
                if (!r.EndOfMessage) continue;
                var d = nachricht.ToArray();
                nachricht.SetLength(0);
                if (r.MessageType == WebSocketMessageType.Binary)
                {
                    if (d.Length <= 2) continue;
                    var kopf = (d[0] << 8) | d[1];
                    var start = kopf + 2;
                    if (start >= d.Length) continue;
                    lock (z.Schloss) { z.Daten.Write(d, start, d.Length - start); z.LetzterRahmen = DateTime.Now; }
                }
                else if (Encoding.UTF8.GetString(d).Contains("Path:turn.end"))
                {
                    byte[] fertig;
                    lock (z.Schloss) fertig = z.Daten.ToArray();
                    if (fertig.Length == 0) throw VorleseFehler.Fehler("Edge hat kein Audio geliefert.");
                    return new VorleseTon(fertig, "mp3");
                }
            }
        }
        catch (VorleseFehler) { throw; }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
        catch (Exception ex)
        {
            bool ab;
            byte[] d;
            lock (z.Schloss) { ab = z.Abgelaufen; d = z.Daten.ToArray(); }
            // Kam nach Ton ein Zeitlimit, gilt der Absatz als fertig (wie der Edge-Wächter am Handy).
            if (ab && d.Length > 0) return new VorleseTon(d, "mp3");
            if (ab) throw VorleseFehler.Zeitlimit("Edge hat nicht rechtzeitig geantwortet.");
            throw VorleseFehler.Fehler($"Edge-Verbindung fehlgeschlagen: {ex.Message}");
        }
        finally
        {
            waechterEnde.Cancel();
            try { socket.Abort(); } catch { }
            try { await waechter.ConfigureAwait(false); } catch { }
        }
    }

    // MARK: Ton → abspielbarer Puffer

    /// <summary>Rate des Puffers, den die Stimme abspielt.</summary>
    public const int ZielRate = 24000;

    /// <summary>Dekodiert mp3/wav in einen Puffer im Zielformat (24 kHz mono float); `strecken` ≠ 1 ändert das Tempo
    /// bei gleicher Tonhöhe (für Gemini, das kein Sprechtempo kennt, wie am Handy per WSOLA).</summary>
    public static float[]? Puffer(VorleseTon ton, double strecken)
    {
        var quelle = Dekodiere(ton);
        if (quelle == null || quelle.Value.Samples.Length == 0) return null;
        var aus = Wandle(quelle.Value.Samples, quelle.Value.Rate);
        if (aus == null) return null;
        if (Math.Abs(strecken - 1) >= 0.02) aus = Strecke(aus, ZielRate, strecken) ?? aus;
        return aus;
    }

    private static (float[] Samples, int Rate)? Dekodiere(VorleseTon ton)
    {
        try
        {
            using var m = new MemoryStream(ton.Daten);
            using WaveStream leser = ton.Endung == "wav" ? new WaveFileReader(m) : new Mp3FileReader(m);
            return LiesMono(leser);
        }
        catch (Exception ex) when (ton.Endung != "wav")
        {
            // ACM-Decoder fehlt (etwa N-Ausgaben von Windows): über Media Foundation.
            Log("sprache", $"Vorlesen: MP3-Decoder nicht nutzbar ({ex.Message}), nehme Media Foundation");
        }
        catch (Exception ex)
        {
            Log("fehler", $"Vorlesen: Ton nicht lesbar ({ex.Message})");
            return null;
        }
        var datei = Path.Combine(Pfade.Temp, $"jarvis-vorlesen-{Guid.NewGuid():N}.{ton.Endung}");
        try
        {
            File.WriteAllBytes(datei, ton.Daten);
            using var leser = new MediaFoundationReader(datei);
            return LiesMono(leser);
        }
        catch (Exception ex)
        {
            Log("fehler", $"Vorlesen: Ton nicht lesbar ({ex.Message})");
            return null;
        }
        finally
        {
            try { File.Delete(datei); } catch { }
        }
    }

    private static (float[] Samples, int Rate) LiesMono(WaveStream leser)
    {
        var quelle = leser.ToSampleProvider();
        int kanaele = quelle.WaveFormat.Channels, rate = quelle.WaveFormat.SampleRate;
        var alle = new List<float>();
        var buf = new float[rate * kanaele];
        int n;
        while ((n = quelle.Read(buf, 0, buf.Length)) > 0)
        {
            for (int i = 0; i + kanaele <= n; i += kanaele)
            {
                float s = 0;
                for (int k = 0; k < kanaele; k++) s += buf[i + k];
                alle.Add(s / kanaele);
            }
        }
        return (alle.ToArray(), rate);
    }

    private static float[]? Wandle(float[] q, int rate)
    {
        if (rate == ZielRate) return q;
        // Etwas Stille hinterher: Der Filter hält die letzten Samples sonst zurück.
        var mitNachlauf = new float[q.Length + 256];
        Array.Copy(q, mitNachlauf, q.Length);
        return new Umwandler(ZielRate).Wandle(new MikroPuffer(mitNachlauf, rate));
    }

    /// <summary>Tempo ändern bei gleicher Tonhöhe: WSOLA wie Synthese.kt (Hann-Fenster, Suche nach dem bestpassenden Ausschnitt).
    /// Anfang und Ende bleiben ungeblendet erhalten.</summary>
    private static float[]? Strecke(float[] ein, int rate, double tempoWert)
    {
        var tempo = (float)Math.Min(Math.Max(tempoWert, 0.5), 2);
        int w = rate * 30 / 1000, hs = w / 2, such = rate * 10 / 1000;
        if (ein.Length < w * 4) return null;
        // Hann-Fenster: Bei halber Überlappung ergänzen sich die Fenster genau zu 1.
        var fenster = new float[w];
        for (int i = 0; i < w; i++) fenster[i] = (float)(0.5 - 0.5 * Math.Cos(2 * Math.PI * i / w));
        var aus = new float[(int)Math.Max(ein.Length, ein.Length / tempo) + 4 * w + 2 * such];
        int vorher = 0, k = 0;
        while (true)
        {
            var ziel = (int)Math.Round(k * hs * (double)tempo);
            var o = k * hs;
            if (ziel + w + such >= ein.Length || o + w > aus.Length) break;
            var pos = ziel;
            var natuerlich = vorher + hs;
            // Den Ausschnitt um das Ziel suchen, der am besten an den vorigen anschließt (grob gerastert, reicht für Sprache);
            // nach Energie normiert, sonst gewönne ein lauter, schlecht passender Ausschnitt.
            if (k > 0 && natuerlich + w < ein.Length)
            {
                var bester = double.NegativeInfinity;
                for (int d = -such; d <= such; d += 2)
                {
                    var c = ziel + d;
                    if (c < 0) continue;
                    double summe = 0, energie = 0;
                    for (int i = 0; i < w; i += 4)
                    {
                        double x = ein[c + i];
                        summe += ein[natuerlich + i] * x;
                        energie += x * x;
                    }
                    var guete = summe / Math.Sqrt(energie + 1e-9);
                    if (guete > bester) { bester = guete; pos = c; }
                }
            }
            // Der erste Ausschnitt hat keinen Vorgänger: seine erste Hälfte ungeblendet, sonst finge jeder Absatz mit einer Blende an.
            for (int i = 0; i < w; i++) aus[o + i] += ein[pos + i] * (k == 0 && i < hs ? 1f : fenster[i]);
            vorher = pos;
            k++;
        }
        if (k == 0) return null;
        // Ab der Mitte des letzten Ausschnitts den restlichen Ton unverändert anhängen: So fehlt am Ende nichts.
        var start = (k - 1) * hs + hs;
        var rest = Math.Min(ein.Length - (vorher + hs), aus.Length - start);
        for (int i = 0; i < rest; i++) aus[start + i] = ein[vorher + hs + i];
        var laenge = start + rest;
        var ergebnis = new float[laenge];
        for (int i = 0; i < laenge; i++) ergebnis[i] = Math.Clamp(aus[i], -1f, 1f);
        return ergebnis;
    }
}

public static class WavKopf
{
    /// <summary>Rohes PCM 16 Bit mono mit WAV-Kopf versehen.</summary>
    public static byte[] Mit(byte[] pcm, int rate)
    {
        var k = new byte[44 + pcm.Length];
        void S(int i, string t) => Encoding.ASCII.GetBytes(t).CopyTo(k, i);
        void U32(int i, int v) => BitConverter.TryWriteBytes(k.AsSpan(i), (uint)v);
        void U16(int i, int v) => BitConverter.TryWriteBytes(k.AsSpan(i), (ushort)v);
        S(0, "RIFF"); U32(4, 36 + pcm.Length); S(8, "WAVE"); S(12, "fmt "); U32(16, 16); U16(20, 1); U16(22, 1); U32(24, rate); U32(28, rate * 2); U16(32, 2); U16(34, 16);
        S(36, "data"); U32(40, pcm.Length);
        pcm.CopyTo(k, 44);
        return k;
    }
}
