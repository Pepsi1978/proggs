using System.IO;
using System.Net.Http;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using System.Text;

namespace JarvisWindows;

/// <summary>HTTP des Gehirns. Dieser PC hat totes IPv6: Verbunden wird immer zuerst über IPv4, je Adresse mit eigener kurzer Frist.
/// HttpClient.Timeout ist aus: Die Fristen je Anfrage setzt `Anfrage` und bei Datenströmen `Zeilen` (wie timeoutInterval am Mac: Zeit ohne Daten, nicht Gesamtdauer).</summary>
public static class GehirnNetz
{
    private static SocketsHttpHandler Handler(int? jeServer, DecompressionMethods entpacken)
    {
        var h = new SocketsHttpHandler
        {
            ConnectCallback = VerbindeIPv4Zuerst,
            ConnectTimeout = TimeSpan.FromSeconds(30),
            PooledConnectionLifetime = TimeSpan.FromMinutes(5),
            PooledConnectionIdleTimeout = TimeSpan.FromSeconds(60),
            AutomaticDecompression = entpacken,
        };
        if (jeServer != null) h.MaxConnectionsPerServer = jeServer.Value;
        return h;
    }

    public static readonly HttpClient Client = new(Handler(null, DecompressionMethods.All)) { Timeout = Timeout.InfiniteTimeSpan };
    /// <summary>Dateien auf eigenen Verbindungen und höchstens zwei zugleich (dateiSession am Mac). Ohne Entpacken: Sonst stimmen Fortsetzen und Länge nicht.</summary>
    public static readonly HttpClient DateiClient = new(Handler(2, DecompressionMethods.None)) { Timeout = Timeout.InfiniteTimeSpan };

    private static async ValueTask<Stream> VerbindeIPv4Zuerst(SocketsHttpConnectionContext kontext, CancellationToken ct)
    {
        var ziel = kontext.DnsEndPoint;
        var adressen = (await Dns.GetHostAddressesAsync(ziel.Host, ct))
            .OrderBy(a => a.AddressFamily == AddressFamily.InterNetwork ? 0 : 1).ToList();
        Exception? letzter = null;
        foreach (var a in adressen)
        {
            var s = new Socket(a.AddressFamily, SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
            try
            {
                s.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, true);
                using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
                frist.CancelAfter(TimeSpan.FromSeconds(10));
                await s.ConnectAsync(a, ziel.Port, frist.Token);
                return new NetworkStream(s, ownsSocket: true);
            }
            catch (Exception ex)
            {
                s.Dispose();
                ct.ThrowIfCancellationRequested();
                letzter = ex;
            }
        }
        throw letzter ?? new SocketException((int)SocketError.HostNotFound);
    }

    /// <summary>Schickt die Anfrage und liest die ganze Antwort. Überschreitet sie die Frist: TimeoutException; Abbruch von außen: OperationCanceledException.</summary>
    public static async Task<(int code, byte[] daten)> Anfrage(HttpRequestMessage req, double sekunden, CancellationToken ct = default, HttpClient? client = null)
    {
        using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
        frist.CancelAfter(TimeSpan.FromSeconds(sekunden));
        try
        {
            using var antwort = await (client ?? Client).SendAsync(req, HttpCompletionOption.ResponseContentRead, frist.Token);
            return ((int)antwort.StatusCode, await antwort.Content.ReadAsByteArrayAsync(frist.Token));
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested)
        {
            throw new TimeoutException("Zeitüberschreitung der Anfrage.");
        }
    }

    /// <summary>Für Datenströme: kehrt zurück, sobald die Kopfzeilen da sind. Der Aufrufer gibt die Antwort frei.</summary>
    public static async Task<HttpResponseMessage> Strom(HttpRequestMessage req, double sekunden, CancellationToken ct = default, HttpClient? client = null)
    {
        using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
        frist.CancelAfter(TimeSpan.FromSeconds(sekunden));
        try
        {
            return await (client ?? Client).SendAsync(req, HttpCompletionOption.ResponseHeadersRead, frist.Token);
        }
        catch (OperationCanceledException) when (!ct.IsCancellationRequested)
        {
            throw new TimeoutException("Zeitüberschreitung der Anfrage.");
        }
    }

    /// <summary>Zeilen einer Antwort; die Frist gilt je Zeile (SSE). Entspricht bytes.lines.</summary>
    public static async IAsyncEnumerable<string> Zeilen(HttpResponseMessage antwort, double sekunden, [EnumeratorCancellation] CancellationToken ct = default)
    {
        using var strom = await antwort.Content.ReadAsStreamAsync(ct);
        using var leser = new StreamReader(strom, new UTF8Encoding(false));
        using var uhr = CancellationTokenSource.CreateLinkedTokenSource(ct);
        while (true)
        {
            uhr.CancelAfter(TimeSpan.FromSeconds(sekunden));
            string? zeile;
            try
            {
                zeile = await leser.ReadLineAsync(uhr.Token);
            }
            catch (OperationCanceledException) when (!ct.IsCancellationRequested)
            {
                throw new TimeoutException("Zeitüberschreitung beim Lesen der Antwort.");
            }
            if (zeile == null) yield break;
            yield return zeile;
        }
    }

    /// <summary>Text einer Fehlerantwort, höchstens gut 4000 Zeichen.</summary>
    public static async Task<string> FehlerText(HttpResponseMessage antwort, CancellationToken ct = default)
    {
        var t = new StringBuilder();
        try
        {
            await foreach (var z in Zeilen(antwort, 30, ct))
            {
                t.Append(z);
                if (t.Length > 4000) break;
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException) { }
        return t.ToString();
    }

    public static StringContent JsonInhalt(object? wert, string typ = "application/json")
    {
        var c = new StringContent(JSON.Text(wert), new UTF8Encoding(false));
        c.Headers.ContentType = new MediaTypeHeaderValue(typ);
        return c;
    }

    /// <summary>Lesbarer Grund für eine gescheiterte Anfrage.</summary>
    public static string Beschreibung(Exception ex) => ex switch
    {
        OperationCanceledException => "Abgebrochen.",
        _ => ex.Message,
    };
}

public static class GehirnHilfe
{
    /// <summary>Wie String.prefix: schneidet nie mitten in ein Ersatzzeichenpaar. Ein halbes Emoji ließe sonst das Schreiben des ganzen JSON scheitern.</summary>
    public static string Kappe(this string s, int n) => s.Schnitt(n);

    /// <summary>Wartet höchstens `sekunden` auf eine Arbeit (mitFrist am Mac). true = rechtzeitig fertig. Die Arbeit läuft danach weiter, bis ihr Besitzer sie abbricht.</summary>
    public static async Task<bool> Frist(double sekunden, Task arbeit)
    {
        var uhr = Task.Delay(TimeSpan.FromSeconds(sekunden));
        return await Task.WhenAny(arbeit, uhr) == arbeit;
    }

    /// <summary>Datei unversehrt ersetzen (options: .atomic).</summary>
    public static void SchreibeAtomar(string pfad, byte[] daten)
    {
        var teil = pfad + "." + Guid.NewGuid().ToString("N") + ".tmp";
        File.WriteAllBytes(teil, daten);
        try { File.Move(teil, pfad, true); }
        catch { try { File.Delete(teil); } catch { } throw; }
    }

    public static double JetztSekunden() => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / 1000.0;
}

/// <summary>Antwort eines Modellschritts – immer im Format der Claude-Messages-API (kanonisches Verlaufsformat von JarvisWindows).</summary>
public sealed class ModellAntwort
{
    /// <summary>Inhaltsblöcke (text, tool_use, thinking, server_tool_use …), unverändert zurückzugeben.</summary>
    public List<JSONObjekt> Inhalt { get; set; } = new();
    /// <summary>end_turn, tool_use, pause_turn, max_tokens, refusal.</summary>
    public string StopGrund { get; set; } = "end_turn";
    public int EingabeTokens { get; set; }
    public int AusgabeTokens { get; set; }
    public int CacheTokens { get; set; }
    public int CacheSchreibTokens { get; set; }
}

/// <summary>Listenpreise der Claude-API in Dollar je Million Tokens (Stand 06.10.2026). Cache schreiben = 1,25 × Eingabe. Websuche ist nicht enthalten.</summary>
public static class ClaudePreis
{
    private static readonly Dictionary<string, (double ein, double aus, double cacheLesen)> Tabelle = new()
    {
        ["claude-opus-5-5"] = (4, 20, 0.20),
        ["claude-sonnet-5-5"] = (2, 10, 0.20),
        ["claude-fable-5-1"] = (10, 50, 0.25),
        ["claude-haiku-5-5"] = (0.10, 0.50, 0.01),
    };

    public static double Dollar(string modell, ModellAntwort a)
    {
        if (!Tabelle.TryGetValue(modell, out var p)) return 0;
        // Haiku kostet oberhalb von 100 000 Tokens Eingabe das Fünffache.
        if (modell == "claude-haiku-5-5" && a.EingabeTokens + a.CacheTokens + a.CacheSchreibTokens > 100_000) p = (0.50, 2.50, 0.05);
        var ein = a.EingabeTokens * p.ein + a.CacheSchreibTokens * p.ein * 1.25 + a.CacheTokens * p.cacheLesen;
        return (ein + a.AusgabeTokens * p.aus) / 1_000_000;
    }
}

public interface ModellAnbieter
{
    /// <summary>Ein Schritt: Verlauf (Messages-Format) rein, Inhaltsblöcke raus. `onText` bekommt Textstücke beim Streamen, auf einem Hintergrundfaden.</summary>
    Task<ModellAntwort> Antworte(IReadOnlyList<string> system, IReadOnlyList<WerkzeugDefinition> werkzeuge, bool websuche, IReadOnlyList<JSONObjekt> verlauf,
                                 string modell, string effort, Action<string> onText, CancellationToken ct = default);
}

public enum AnbieterFehlerArt { KeinSchluessel, Http, Abgelehnt, Netz }

public sealed class AnbieterFehler : Exception
{
    public AnbieterFehlerArt Art { get; }
    public int Code { get; }
    public string Text { get; }

    private AnbieterFehler(AnbieterFehlerArt art, int code, string text, string meldung) : base(meldung)
    {
        Art = art;
        Code = code;
        Text = text;
    }

    public static AnbieterFehler KeinSchluessel(string a) => new(AnbieterFehlerArt.KeinSchluessel, 0, a, $"Kein API-Schlüssel für {a} eingetragen (Einstellungen → Konten).");
    public static AnbieterFehler Http(int c, string t) => new(AnbieterFehlerArt.Http, c, t, $"Modell-Fehler {c}: {t.Kappe(400)}");
    public static AnbieterFehler Abgelehnt(string t) => new(AnbieterFehlerArt.Abgelehnt, 0, t, $"Das Modell hat die Anfrage abgelehnt. {t}");
    public static AnbieterFehler Netz(string t) => new(AnbieterFehlerArt.Netz, 0, t, $"Netzwerkfehler: {t}");
}

// MARK: - Claude (Messages API, Streaming, Prompt-Caching)

public sealed class AnthropicKlient : ModellAnbieter
{
    private readonly string schluessel;
    public AnthropicKlient(string schluessel) { this.schluessel = schluessel; }

    private static readonly JSONObjekt Fluechtig = new() { ["type"] = "ephemeral" };

    // Immer im Hintergrund: Das Lesen des Stroms zeilenweise auf dem Hauptfaden ließe die Oberfläche stocken.
    public Task<ModellAntwort> Antworte(IReadOnlyList<string> system, IReadOnlyList<WerkzeugDefinition> werkzeuge, bool websuche, IReadOnlyList<JSONObjekt> verlauf,
                                        string modell, string effort, Action<string> onText, CancellationToken ct = default)
        => Task.Run(() => Lauf(system, werkzeuge, websuche, verlauf, modell, effort, onText, ct));

    private async Task<ModellAntwort> Lauf(IReadOnlyList<string> system, IReadOnlyList<WerkzeugDefinition> werkzeuge, bool websuche, IReadOnlyList<JSONObjekt> verlauf,
                                           string modell, string effort, Action<string> onText, CancellationToken ct)
    {
        if (schluessel.Length == 0) throw AnbieterFehler.KeinSchluessel("Anthropic");
        using var req = new HttpRequestMessage(HttpMethod.Post, "https://api.anthropic.com/v1/messages");
        req.Headers.TryAddWithoutValidation("x-api-key", schluessel);
        req.Headers.TryAddWithoutValidation("anthropic-version", "2026-10-21");

        var mitFallback = modell is "claude-opus-5-5" or "claude-sonnet-5-5" or "claude-fable-5-1";
        if (mitFallback) req.Headers.TryAddWithoutValidation("anthropic-beta", "server-side-fallback-2026-07-01");

        // Prompt-Cache: tools → system → messages. Stabiles zuerst, Haltepunkte am letzten Werkzeug und an den Systemteilen.
        var tools = werkzeuge.Select(w => new JSONObjekt
        {
            ["name"] = w.Name, ["description"] = w.Beschreibung.Kappe(4000), ["input_schema"] = w.Schema,
        }).ToList();
        if (websuche)
        {
            var neu = modell is "claude-opus-5-5" or "claude-sonnet-5-5";
            tools.Add(new JSONObjekt { ["type"] = neu ? "web_search_20260209" : "web_search_20250305", ["name"] = "web_search", ["max_uses"] = 3 });
        }
        if (tools.Count > 0) tools[^1]["cache_control"] = Fluechtig;
        var systemBloecke = system.Where(t => !string.IsNullOrEmpty(t)).Select((t, i) =>
        {
            var b = new JSONObjekt { ["type"] = "text", ["text"] = t };
            if (i < 2) b["cache_control"] = Fluechtig;
            return b;
        }).ToList();
        // Nur die Kopie bekommt den Haltepunkt: Der gespeicherte Verlauf bleibt unberührt.
        var verlaufMitCache = new List<JSONObjekt>(verlauf);
        if (verlaufMitCache.Count > 0 && verlaufMitCache[^1].TryGetValue("content", out var c) && c is not string && c != null)
        {
            var inhalt = verlaufMitCache[^1].Objekte("content").ToList();
            if (inhalt.Count > 0)
            {
                inhalt[^1] = new JSONObjekt(inhalt[^1]) { ["cache_control"] = Fluechtig };
                verlaufMitCache[^1] = new JSONObjekt(verlaufMitCache[^1]) { ["content"] = inhalt };
            }
        }
        var rumpf = new JSONObjekt
        {
            ["model"] = modell, ["max_tokens"] = 32000, ["stream"] = true,
            ["system"] = systemBloecke, ["messages"] = verlaufMitCache,
            ["output_config"] = new JSONObjekt { ["effort"] = effort },
        };
        if (tools.Count > 0) rumpf["tools"] = tools;
        if (mitFallback) rumpf["fallbacks"] = "default";
        req.Content = GehirnNetz.JsonInhalt(rumpf);

        try
        {
            using var antwort = await GehirnNetz.Strom(req, 600, ct);
            var code = (int)antwort.StatusCode;
            if (code != 200) throw AnbieterFehler.Http(code, await GehirnNetz.FehlerText(antwort, ct));
            return await Lies(antwort, onText, ct);
        }
        catch (Exception ex) when (ex is HttpRequestException or IOException or TimeoutException)
        {
            throw AnbieterFehler.Netz(GehirnNetz.Beschreibung(ex));
        }
    }

    private static async Task<ModellAntwort> Lies(HttpResponseMessage antwort, Action<string> onText, CancellationToken ct)
    {
        var bloecke = new Dictionary<int, JSONObjekt>();
        var teilJSON = new Dictionary<int, string>();
        var ergebnis = new ModellAntwort();
        var fertig = false;
        await foreach (var zeile in GehirnNetz.Zeilen(antwort, 600, ct))
        {
            if (!zeile.StartsWith("data:", StringComparison.Ordinal)) continue;
            var e = JSON.Objekt(zeile[5..].Trim());
            if (e == null) continue;
            switch (e.Text("type") ?? "")
            {
                case "message_stop":
                    fertig = true;
                    break;
                case "message_start":
                {
                    var u = e.Obj("message")?.Obj("usage");
                    if (u != null)
                    {
                        ergebnis.EingabeTokens = (int)(u.Ganz("input_tokens") ?? 0);
                        ergebnis.CacheTokens = (int)(u.Ganz("cache_read_input_tokens") ?? 0);
                        ergebnis.CacheSchreibTokens = (int)(u.Ganz("cache_creation_input_tokens") ?? 0);
                    }
                    break;
                }
                case "content_block_start":
                    bloecke[(int)(e.Ganz("index") ?? 0)] = e.Obj("content_block") ?? new JSONObjekt();
                    break;
                case "content_block_delta":
                {
                    var i = (int)(e.Ganz("index") ?? 0);
                    var d = e.Obj("delta");
                    if (d == null || !bloecke.TryGetValue(i, out var b)) continue;
                    switch (d.Text("type") ?? "")
                    {
                        case "text_delta":
                            var s = d.Text("text") ?? "";
                            b["text"] = (b.Text("text") ?? "") + s;
                            onText(s);
                            break;
                        case "thinking_delta": b["thinking"] = (b.Text("thinking") ?? "") + (d.Text("thinking") ?? ""); break;
                        case "signature_delta": b["signature"] = d.Text("signature") ?? ""; break;
                        case "input_json_delta": teilJSON[i] = teilJSON.GetValueOrDefault(i, "") + (d.Text("partial_json") ?? ""); break;
                        case "citations_delta":
                            var l = b.Liste("citations") ?? new List<object?>();
                            if (d.TryGetValue("citation", out var x) && x != null) l.Add(x);
                            b["citations"] = l;
                            break;
                    }
                    break;
                }
                case "content_block_stop":
                {
                    var i = (int)(e.Ganz("index") ?? 0);
                    if (teilJSON.TryGetValue(i, out var roh) && bloecke.TryGetValue(i, out var b))
                        b["input"] = roh.Length == 0 ? new JSONObjekt() : (JSON.Objekt(roh) ?? new JSONObjekt { ["_unlesbar"] = roh });
                    break;
                }
                case "message_delta":
                {
                    var s = e.Obj("delta")?.Text("stop_reason");
                    if (s != null) ergebnis.StopGrund = s;
                    var u = e.Obj("usage");
                    if (u != null) ergebnis.AusgabeTokens = (int)(u.Ganz("output_tokens") ?? 0);
                    break;
                }
                case "error":
                    throw AnbieterFehler.Http(500, e.Obj("error")?.Text("message") ?? "Unbekannter Fehler");
            }
        }
        // Ein abgerissener Strom ist kein Erfolg: Ein halber Werkzeugaufruf liefe sonst mit leeren Argumenten.
        if (!fertig) throw AnbieterFehler.Netz("Die Antwort des Modells brach vorzeitig ab.");
        ergebnis.Inhalt = bloecke.Keys.OrderBy(k => k).Select(k => bloecke[k]).ToList();
        if (ergebnis.StopGrund == "refusal" && ergebnis.Inhalt.Count == 0) throw AnbieterFehler.Abgelehnt("");
        return ergebnis;
    }
}

/// <summary>Einfacher Einzelaufruf ohne Werkzeuge (Titel, Lernen, Umformulieren).</summary>
public static class Kurzauftrag
{
    public static async Task<string?> Frage(string prompt, string system = "", string modell = "claude-haiku-5-5", CancellationToken ct = default)
    {
        var z = Einstellungen.Shared.Zugang;
        // Ohne Claude-Schlüssel über das ChatGPT-Konto (GPT-6 Luna) – Titel und Lernen gehen dann trotzdem.
        ModellAnbieter anbieter = z.Anthropic.Length == 0 ? new OpenAIKlient(z.Openai) : new AnthropicKlient(z.Anthropic);
        var m = z.Anthropic.Length == 0 ? "gpt-6-luna" : modell;
        var verlauf = new List<JSONObjekt>
        {
            new() { ["role"] = "user", ["content"] = new List<JSONObjekt> { new() { ["type"] = "text", ["text"] = prompt } } },
        };
        try
        {
            var a = await anbieter.Antworte(new[] { system }, Array.Empty<WerkzeugDefinition>(), false, verlauf, m, "low", _ => { }, ct);
            return string.Concat(a.Inhalt.Where(b => b.Text("type") == "text").Select(b => b.Text("text") ?? "")).Trim();
        }
        catch (Exception ex) when (ex is not OutOfMemoryException)
        {
            return null;
        }
    }
}
