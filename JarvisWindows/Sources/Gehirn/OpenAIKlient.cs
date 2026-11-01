using System.IO;
using System.Net.Http;
namespace JarvisWindows;

/// <summary>OpenAI Responses API. Zwei Wege:
/// 1. Mit ChatGPT-Anmeldung (CodexAnmeldung, eigener Login am Laptop) über chatgpt.com/backend-api/codex/responses –
///    wie die Handy-App, läuft über Franks ChatGPT-Abo. Hat Vorrang, sobald angemeldet.
/// 2. Sonst mit eigenem API-Schlüssel über api.openai.com/v1/responses.
/// Der kanonische Verlauf (Claude-Messages-Format) wird pro Aufruf in Responses-`input` übersetzt und die Antwort
/// zurück in Claude-Blöcke (text, tool_use).</summary>
public sealed class OpenAIKlient : ModellAnbieter
{
    private readonly string schluessel;
    public OpenAIKlient(string schluessel) { this.schluessel = schluessel; }

    public const string CodexURL = "https://chatgpt.com/backend-api/codex/responses";
    public const string ApiURL = "https://api.openai.com/v1/responses";
    /// <summary>Neue Modelle (z. B. GPT-6.1 Sol) schaltet das Codex-Backend erst ab dieser Client-Kennung frei, sonst HTTP 400.</summary>
    public const string CodexVersion = "0.159.0";

    public Task<ModellAntwort> Antworte(IReadOnlyList<string> system, IReadOnlyList<WerkzeugDefinition> werkzeuge, bool websuche, IReadOnlyList<JSONObjekt> verlauf,
                                        string modell, string effort, Action<string> onText, CancellationToken ct = default)
        => Task.Run(() => Lauf(system, werkzeuge, websuche, verlauf, modell, effort, onText, ct));

    private async Task<ModellAntwort> Lauf(IReadOnlyList<string> system, IReadOnlyList<WerkzeugDefinition> werkzeuge, bool websuche, IReadOnlyList<JSONObjekt> verlauf,
                                           string modell, string effort, Action<string> onText, CancellationToken ct)
    {
        var codex = await CodexAnmeldung.Shared.Angemeldet();
        if (!codex && schluessel.Length == 0) throw AnbieterFehler.KeinSchluessel("OpenAI (API-Schlüssel oder ChatGPT-Anmeldung)");

        var tools = werkzeuge.Select(w => new JSONObjekt
        {
            ["type"] = "function", ["name"] = w.Name, ["description"] = w.Beschreibung.Kappe(4000),
            ["parameters"] = w.Schema, ["strict"] = false,
        }).ToList();
        if (websuche) tools.Add(new JSONObjekt { ["type"] = "web_search" });

        var rumpf = new JSONObjekt
        {
            ["model"] = modell,
            ["stream"] = true,
            ["store"] = false,
            ["instructions"] = string.Join("\n\n", system.Where(t => !string.IsNullOrEmpty(t))),
            ["input"] = Eingabe(verlauf),
            ["reasoning"] = new JSONObjekt { ["effort"] = Effort(effort, modell, codex) },
        };
        if (tools.Count > 0)
        {
            rumpf["tools"] = tools;
            rumpf["tool_choice"] = "auto";
            rumpf["parallel_tool_calls"] = true;
        }

        var versuch = 0;
        while (true)
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, codex ? CodexURL : ApiURL);
            req.Headers.TryAddWithoutValidation("Accept", "text/event-stream");
            var r = new JSONObjekt(rumpf);
            if (codex)
            {
                var (token, konto) = await CodexAnmeldung.Shared.GueltigerToken(ct);
                req.Headers.TryAddWithoutValidation("Authorization", $"Bearer {token}");
                req.Headers.TryAddWithoutValidation("originator", "codex_cli_rs");
                req.Headers.TryAddWithoutValidation("version", CodexVersion);
                req.Headers.TryAddWithoutValidation("User-Agent", $"codex_cli_rs/{CodexVersion} (JarvisWindows)");
                req.Headers.TryAddWithoutValidation("ChatGPT-Account-ID", konto);
                // Erster Versuch über den Priority-Pool wie am Handy; bei Serverfehlern ohne.
                if (versuch == 0) r["service_tier"] = "priority";
            }
            else
            {
                req.Headers.TryAddWithoutValidation("Authorization", $"Bearer {schluessel}");
            }
            req.Content = GehirnNetz.JsonInhalt(r);
            try
            {
                return await Stream(req, onText, ct);
            }
            catch (AnbieterFehler f) when (f.Art == AnbieterFehlerArt.Http && (f.Code == 408 || f.Code >= 500) && versuch < 2)
            {
                versuch += 1;
                Log("fehler", $"OpenAI {f.Code}, neuer Versuch {versuch}: {f.Text.Kappe(120)}");
                await Task.Delay(TimeSpan.FromSeconds(versuch == 1 ? 1 : 3), ct);
            }
        }
    }

    // MARK: - Effort

    public static string Effort(string e, string modell, bool codex)
    {
        var s = e.ToLowerInvariant();
        if (codex)
        {
            // Codex-Backend: Astra, Sol, Terra bis ultra; Luna bis max.
            var erlaubt = modell.Contains("luna")
                ? new[] { "low", "medium", "high", "xhigh", "max" }
                : new[] { "low", "medium", "high", "xhigh", "max", "ultra" };
            return erlaubt.Contains(s) ? s : "medium";
        }
        return s switch
        {
            "low" or "medium" or "high" => s,
            "xhigh" or "max" or "ultra" => "high",
            _ => "medium",
        };
    }

    // MARK: - Verlauf (Claude-Format) → Responses-input

    public static List<JSONObjekt> Eingabe(IEnumerable<JSONObjekt> verlauf)
    {
        var input = new List<JSONObjekt>();
        foreach (var nachricht in verlauf)
        {
            var rolle = nachricht.Text("role") ?? "user";
            var bloecke = nachricht.Text("content") is { } s
                ? new List<JSONObjekt> { new() { ["type"] = "text", ["text"] = s } }
                : nachricht.Objekte("content");

            if (rolle == "assistant")
            {
                var texte = new List<string>();
                void TextAbschliessen()
                {
                    var t = string.Concat(texte);
                    if (t.Length > 0)
                        input.Add(new JSONObjekt
                        {
                            ["type"] = "message", ["role"] = "assistant",
                            ["content"] = new List<JSONObjekt> { new() { ["type"] = "output_text", ["text"] = t } },
                        });
                    texte.Clear();
                }
                foreach (var b in bloecke)
                {
                    switch (b.Text("type") ?? "")
                    {
                        case "text": texte.Add(b.Text("text") ?? ""); break;
                        case "tool_use":
                            TextAbschliessen();
                            var args = b.TryGetValue("input", out var ein) && ein != null ? JSON.Text(ein) : "{}";
                            input.Add(new JSONObjekt
                            {
                                ["type"] = "function_call", ["call_id"] = b.Text("id") ?? "",
                                ["name"] = b.Text("name") ?? "", ["arguments"] = args,
                            });
                            break;
                        // thinking, server_tool_use, web_search_tool_result … überspringen
                    }
                }
                TextAbschliessen();
            }
            else
            {
                var teile = new List<JSONObjekt>();
                var nachBildern = new List<JSONObjekt>();
                void TeileAbschliessen()
                {
                    if (teile.Count > 0)
                    {
                        input.Add(new JSONObjekt { ["type"] = "message", ["role"] = "user", ["content"] = teile });
                        teile = new List<JSONObjekt>();
                    }
                }
                foreach (var b in bloecke)
                {
                    switch (b.Text("type") ?? "")
                    {
                        case "text": teile.Add(new JSONObjekt { ["type"] = "input_text", ["text"] = b.Text("text") ?? "" }); break;
                        case "image":
                            if (BildURL(b) is { } u) teile.Add(new JSONObjekt { ["type"] = "input_image", ["image_url"] = u });
                            break;
                        case "tool_result":
                            TeileAbschliessen();
                            var text = "";
                            if (b.Text("content") is { } inhaltText) text = inhaltText;
                            else
                            {
                                foreach (var c in b.Objekte("content"))
                                {
                                    if (c.Text("type") == "text") text += (text.Length == 0 ? "" : "\n") + (c.Text("text") ?? "");
                                    else if (c.Text("type") == "image" && BildURL(c) is { } bu)
                                        nachBildern.Add(new JSONObjekt { ["type"] = "input_image", ["image_url"] = bu });
                                }
                            }
                            if (b.Wahr("is_error") == true) text = "FEHLER: " + text;
                            input.Add(new JSONObjekt
                            {
                                ["type"] = "function_call_output", ["call_id"] = b.Text("tool_use_id") ?? "",
                                ["output"] = text.Length == 0 ? "(leer)" : text,
                            });
                            break;
                    }
                }
                TeileAbschliessen();
                // Bilder aus Werkzeugergebnissen (z. B. Bildschirmfoto) als eigene Nutzernachricht nach den Ausgaben.
                if (nachBildern.Count > 0)
                {
                    var inhalt = new List<JSONObjekt> { new() { ["type"] = "input_text", ["text"] = "Bild aus dem Werkzeugergebnis:" } };
                    inhalt.AddRange(nachBildern);
                    input.Add(new JSONObjekt { ["type"] = "message", ["role"] = "user", ["content"] = inhalt });
                }
            }
        }
        return input;
    }

    private static string? BildURL(JSONObjekt b)
    {
        var q = b.Obj("source");
        if (q == null) return null;
        if (q.Text("data") is { } d) return $"data:{q.Text("media_type") ?? "image/png"};base64,{d}";
        return q.Text("url");
    }

    // MARK: - Streaming

    private static async Task<ModellAntwort> Stream(HttpRequestMessage req, Action<string> onText, CancellationToken ct)
    {
        try
        {
            using var antwort = await GehirnNetz.Strom(req, 900, ct);
            var code = (int)antwort.StatusCode;
            if (code != 200)
            {
                var t = await GehirnNetz.FehlerText(antwort, ct);
                if (code == 401 || code == 403) await CodexAnmeldung.Shared.UngueltigMelden(code);
                throw AnbieterFehler.Http(code, t);
            }
            return await Lies(antwort, onText, ct);
        }
        catch (Exception ex) when (ex is HttpRequestException or IOException or TimeoutException)
        {
            throw AnbieterFehler.Netz(GehirnNetz.Beschreibung(ex));
        }
    }

    private static async Task<ModellAntwort> Lies(HttpResponseMessage antwort, Action<string> onText, CancellationToken ct)
    {
        var deltas = new System.Text.StringBuilder();
        var items = new List<JSONObjekt>();
        var gesehen = new HashSet<string>();
        var ergebnis = new ModellAntwort();
        var fertig = false;
        void Sammle(JSONObjekt item)
        {
            var kennung = item.Text("id") ?? item.Text("call_id") ?? JSON.Text(item);
            if (gesehen.Add(kennung)) items.Add(item);
        }
        await foreach (var zeile in GehirnNetz.Zeilen(antwort, 900, ct))
        {
            if (!zeile.StartsWith("data:", StringComparison.Ordinal)) continue;
            var daten = zeile[5..].Trim();
            if (daten == "[DONE]") break;
            var e = JSON.Objekt(daten);
            if (e == null) continue;
            switch (e.Text("type") ?? "")
            {
                case "response.output_text.delta":
                    if (e.Text("delta") is { Length: > 0 } s) { deltas.Append(s); onText(s); }
                    break;
                case "response.output_item.done":
                    if (e.Obj("item") is { } i) Sammle(i);
                    break;
                case "response.completed":
                case "response.incomplete":
                    var r = e.Obj("response") ?? new JSONObjekt();
                    foreach (var o in r.Objekte("output")) Sammle(o);
                    if (r.Obj("usage") is { } u)
                    {
                        ergebnis.EingabeTokens = (int)(u.Ganz("input_tokens") ?? 0);
                        ergebnis.AusgabeTokens = (int)(u.Ganz("output_tokens") ?? 0);
                        ergebnis.CacheTokens = (int)(u.Obj("input_tokens_details")?.Ganz("cached_tokens") ?? 0);
                    }
                    if (e.Text("type") == "response.incomplete") ergebnis.StopGrund = "max_tokens";
                    fertig = true;
                    break;
                case "response.failed":
                case "error":
                    var f = e.Obj("response")?.Obj("error") ?? e.Obj("error") ?? e;
                    throw AnbieterFehler.Http(500, f.Text("message") ?? JSON.Text(f));
            }
            if (fertig) break;
        }

        var inhalt = new List<JSONObjekt>();
        var textAusItems = "";
        var aufrufe = new List<JSONObjekt>();
        foreach (var i in items)
        {
            switch (i.Text("type") ?? "")
            {
                case "message":
                    foreach (var c in i.Objekte("content").Where(c => c.Text("type") == "output_text"))
                        textAusItems += c.Text("text") ?? "";
                    break;
                case "function_call":
                    var args = i.Text("arguments") ?? "";
                    var eingabe = args.Length == 0 ? new JSONObjekt() : (JSON.Objekt(args) ?? new JSONObjekt { ["_unlesbar"] = args });
                    aufrufe.Add(new JSONObjekt
                    {
                        ["type"] = "tool_use", ["id"] = i.Text("call_id") ?? i.Text("id") ?? Guid.NewGuid().ToString().ToUpperInvariant(),
                        ["name"] = i.Text("name") ?? "", ["input"] = eingabe,
                    });
                    break;
            }
        }
        var text = textAusItems.Length == 0 ? deltas.ToString() : textAusItems;
        if (text.Length > 0) inhalt.Add(new JSONObjekt { ["type"] = "text", ["text"] = text });
        inhalt.AddRange(aufrufe);
        if (aufrufe.Count > 0) ergebnis.StopGrund = "tool_use";
        ergebnis.Inhalt = inhalt;
        return ergebnis;
    }
}
