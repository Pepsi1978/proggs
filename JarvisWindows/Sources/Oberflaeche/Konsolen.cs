using System.Globalization;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using Microsoft.Web.WebView2.Core;
using static JarvisWindows.Bau;

namespace JarvisWindows;

public sealed class WebKeks
{
    public string Name { get; set; } = "";
    public string Wert { get; set; } = "";
    public string Domain { get; set; } = "";
    public string Pfad { get; set; } = "/";
    public bool Sicher { get; set; }
    public bool NurHttp { get; set; }
    public DateTime? Ablauf { get; set; }
    public int SameSite { get; set; }
}

public sealed class WebSitzungDaten
{
    public List<WebKeks> Kekse { get; set; } = new();
    /// <summary>localStorage je Herkunft: Manche Seiten (Kimi) halten die Anmeldung dort statt im Cookie.</summary>
    public Dictionary<string, Dictionary<string, string>> Speicher { get; set; } = new();
}

/// <summary>Anmeldungen für Google, claude.ai und Kimi. Sie liegen nur im Tresor; die Browser arbeiten flüchtig und bekommen sie für den Abruf zurück.</summary>
public static class WebSitzungen
{
    private static readonly Dictionary<string, WebSitzungDaten?> Vorrat = new();
    private static readonly Dictionary<string, int> Nummern = new();
    private const string SpeicherLesen = "(function(){ try { var o={}; for(var i=0;i<localStorage.length;i++){ var k=localStorage.key(i); o[k]=localStorage.getItem(k); } return JSON.stringify({h:location.origin,w:o}); } catch(e){ return null; } })()";

    private static string Name(string id) => "konsole-" + id;

    public static bool GehoertDazu(string id, string domain)
    {
        var d = domain.TrimStart('.').ToLowerInvariant();
        bool Teil(string s) => d == s || d.EndsWith("." + s, StringComparison.Ordinal);
        return id switch
        {
            "google" => Teil("google.com"),
            "claude" => Teil("claude.ai"),
            "kimi" => Teil("kimi.ai") || Teil("kimi.com") || Teil("moonshot.cn") || Teil("moonshot.ai"),
            _ => false,
        };
    }

    public static WebSitzungDaten? Lade(string id)
    {
        lock (Vorrat)
        {
            if (Vorrat.TryGetValue(id, out var d)) return d;
            var neu = Tresor.Lade<WebSitzungDaten>(Name(id));
            Vorrat[id] = neu;
            Melden(id, neu);
            return neu;
        }
    }

    private static void Melden(string id, WebSitzungDaten? d)
        => Geheimnisse.Setzen(Name(id), (d?.Kekse.Select(k => k.Wert) ?? Enumerable.Empty<string>()).Concat(d?.Speicher.Values.SelectMany(v => v.Values) ?? Enumerable.Empty<string>()));

    public static bool Angemeldet(string id)
    {
        var d = Lade(id);
        if (d == null) return false;
        return id switch
        {
            "google" => d.Kekse.Any(k => k.Name == "SAPISID"),
            "claude" => d.Kekse.Any(k => k.Name == "sessionKey"),
            _ => d.Kekse.Count > 0 || d.Speicher.Count > 0,
        };
    }

    public static int Nummer(string id) { lock (Vorrat) return Nummern.GetValueOrDefault(id); }

    public static void Abmelden(string id)
    {
        lock (Vorrat)
        {
            Vorrat[id] = null;
            Nummern[id] = Nummern.GetValueOrDefault(id) + 1;
            Tresor.Loesche(Name(id));
            Melden(id, null);
        }
        Log("guthaben", "Am Laptop abgemeldet: " + id, wo: "Konsolen");
    }

    /// <summary>Zieht Cookies und localStorage aus einem Browser in den Tresor. neu = eine Anmeldung, die eine laufende Abfrage ungültig macht.</summary>
    public static async Task Erfassen(string id, CoreWebView2 kern, bool neu, int? nurBeiNummer = null)
    {
        var cookies = await kern.CookieManager.GetCookiesAsync("");
        var kekse = cookies.Where(c => GehoertDazu(id, c.Domain)).Select(c => new WebKeks
        {
            Name = c.Name, Wert = c.Value, Domain = c.Domain, Pfad = c.Path, Sicher = c.IsSecure, NurHttp = c.IsHttpOnly, SameSite = (int)c.SameSite,
            Ablauf = c.IsSession ? null : c.Expires,
        }).ToList();
        var roh = JSON.Wert(Encoding.UTF8.GetBytes(await kern.ExecuteScriptAsync(SpeicherLesen))) as string;
        lock (Vorrat)
        {
            if (nurBeiNummer != null && Nummern.GetValueOrDefault(id) != nurBeiNummer) return;
            var alt = Vorrat.GetValueOrDefault(id) ?? Tresor.Lade<WebSitzungDaten>(Name(id));
            var d = new WebSitzungDaten { Kekse = kekse, Speicher = neu || alt == null ? new() : alt.Speicher };
            if (roh != null && JSON.Objekt(roh) is { } o && o.Text("h") is { } h && Uri.TryCreate(h, UriKind.Absolute, out var u) && GehoertDazu(id, u.Host) && o.Obj("w") is { } w)
                d.Speicher[h] = w.Where(p => p.Value is string).ToDictionary(p => p.Key, p => (string)p.Value!);
            if (neu) Nummern[id] = Nummern.GetValueOrDefault(id) + 1;
            Vorrat[id] = d;
            Tresor.Sichere(d, Name(id));
            Melden(id, d);
        }
    }

    /// <summary>Cookie-Kopf für eine Adresse, wie ein Browser ihn schickt.</summary>
    public static string KeksKopf(string id, string adresse)
    {
        var u = new Uri(adresse);
        var d = Lade(id);
        if (d == null) return "";
        return string.Join("; ", d.Kekse.Where(k =>
        {
            var w = k.Domain.TrimStart('.');
            return (u.Host.Equals(w, StringComparison.OrdinalIgnoreCase) || (k.Domain.StartsWith('.') && u.Host.EndsWith("." + w, StringComparison.OrdinalIgnoreCase)))
                && u.AbsolutePath.StartsWith(k.Pfad, StringComparison.Ordinal) && (k.Ablauf == null || k.Ablauf > DateTime.Now);
        }).Select(k => $"{k.Name}={k.Wert}"));
    }

    public static string? Keks(string id, string name) => Lade(id)?.Kekse.FirstOrDefault(k => k.Name == name)?.Wert;
}

/// <summary>Liest eine Seite in einem unsichtbaren Browser mit Franks Anmeldung aus dem Tresor: Die Bot-Abwehr lässt nur einen echten Browser durch, und manche Seite baut ihren Inhalt erst per Skript auf.</summary>
public static class VersteckterLeser
{
    private const int WsPopup = unchecked((int)0x80000000);

    public static Task<string> Lies(string id, string url, string js, double sekunden, Func<string, bool> fertig, bool erzwungen)
        => Haupt.Auf(() => Lauf(id, url, js, sekunden, fertig, erzwungen));

    private static async Task<string> Lauf(string id, string url, string js, double sekunden, Func<string, bool> fertig, bool erzwungen)
    {
        var ordner = Path.Combine(Pfade.Temp, "KonsoleLeser-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(ordner);
        var nummer = WebSitzungen.Nummer(id);
        HwndSource? quelle = null;
        CoreWebView2Controller? steuerung = null;
        try
        {
            var umgebung = await CoreWebView2Environment.CreateAsync(null, ordner);
            quelle = new HwndSource(new HwndSourceParameters("JarvisKonsolenLeser") { WindowStyle = WsPopup, Width = 1080, Height = 1400, PositionX = -32000, PositionY = -32000 });
            steuerung = await umgebung.CreateCoreWebView2ControllerAsync(quelle.Handle);
            steuerung.Bounds = new System.Drawing.Rectangle(0, 0, 1080, 1400);
            steuerung.IsVisible = true;
            var kern = steuerung.CoreWebView2;
            kern.Settings.UserAgent = ConsoleSitzung.BrowserKennung;
            var daten = WebSitzungen.Lade(id);
            foreach (var k in daten?.Kekse ?? new List<WebKeks>())
            {
                var c = kern.CookieManager.CreateCookie(k.Name, k.Wert, k.Domain, k.Pfad);
                c.IsSecure = k.Sicher;
                c.IsHttpOnly = k.NurHttp;
                c.SameSite = (CoreWebView2CookieSameSiteKind)k.SameSite;
                if (k.Ablauf is { } a) c.Expires = a;
                kern.CookieManager.AddOrUpdateCookie(c);
            }
            if (daten?.Speicher.Count > 0)
                await kern.AddScriptToExecuteOnDocumentCreatedAsync($"(function(){{ var m={JSON.Text(daten.Speicher.ToDictionary(p => p.Key, p => (object?)p.Value.ToDictionary(x => x.Key, x => (object?)x.Value)))}; var s=m[location.origin]; if(!s) return; try {{ for(var k in s) if(localStorage.getItem(k)===null) localStorage.setItem(k,s[k]); }} catch(e){{}} }})();");
            kern.Navigate(url);
            var letzter = "";
            var ende = DateTime.Now.AddSeconds(sekunden);
            while (DateTime.Now < ende)
            {
                await Task.Delay(1000);
                // Die Stimme hat Vorrang: Ein Abruf aus dem Hintergrund weicht sofort einem Gespräch.
                if (!erzwungen && KontingentQuellen.GespraechLaeuft) throw new InvalidOperationException("Abruf wegen eines Gesprächs verschoben.");
                var s = JSON.Wert(Encoding.UTF8.GetBytes(await kern.ExecuteScriptAsync(js))) as string;
                if (string.IsNullOrEmpty(s)) continue;
                letzter = s;
                if (fertig(s)) break;
            }
            // Erneuerte Cookies und Token zurück in den Tresor, sonst läuft die Anmeldung trotz Abfragen ab.
            await WebSitzungen.Erfassen(id, kern, false, nummer);
            return letzter;
        }
        finally
        {
            try { steuerung?.Close(); } catch { }
            try { quelle?.Dispose(); } catch { }
            _ = Task.Run(async () =>
            {
                for (var i = 0; i < 20 && Directory.Exists(ordner); i++)
                {
                    await Task.Delay(1000);
                    try { Directory.Delete(ordner, true); } catch { }
                }
            });
        }
    }
}

/// <summary>Die Limits am Laptop selbst lesen, mit eigener Anmeldung. Gleiche Abfragen wie am Handy in KontingenteFaehigkeit.kt.</summary>
public static class KontingentQuellen
{
    public sealed class NichtAngemeldet : Exception { public NichtAngemeldet(string t) : base(t) { } }
    private sealed record Roh(string Name, double Genutzt, long Fenster, long Reset, long Gelesen);

    public static readonly (string Id, string Titel)[] Alle = { ("claude", "Claude (Anthropic)"), ("codex", "ChatGPT / Codex (OpenAI)"), ("kimi", "Kimi"), ("gemini", "Gemini / Antigravity (Google)") };
    public const string ClaudeLogin = "https://claude.ai/login";
    public const string KimiSeite = "https://www.kimi.ai/settings/subscription";
    private const string Speicher = "kontingente.laptop";
    private const long Stunden5 = 5 * 3600, Woche = 7 * 86400;
    private static readonly TimeZoneInfo Berlin = TimeZoneInfo.FindSystemTimeZoneById("W. Europe Standard Time");
    private static readonly CultureInfo De = CultureInfo.GetCultureInfo("de-DE");
    private static long Jetzt => DateTimeOffset.UtcNow.ToUnixTimeSeconds();

    public static bool GespraechLaeuft => SprachSchleife.Shared.Zustand != SprachZustand.Aus;

    private static DateTime ZeitIn(long s) => TimeZoneInfo.ConvertTime(DateTimeOffset.FromUnixTimeSeconds(s), Berlin).DateTime;

    private static string ZeitText(long s)
    {
        var z = ZeitIn(s);
        var heute = TimeZoneInfo.ConvertTime(DateTime.UtcNow, Berlin).Date;
        if (z.Date == heute) return "heute, " + z.ToString("HH:mm", De) + " Uhr";
        if (z.Date == heute.AddDays(1)) return "morgen, " + z.ToString("HH:mm", De) + " Uhr";
        return z.ToString("dddd, dd.MM., HH:mm", De) + " Uhr";
    }

    private static string DauerText(long s) => s >= 2 * 86400 ? $"{s / 86400} Tage" : s >= 2 * 3600 ? $"{s / 3600} Stunden" : $"{Math.Max(s / 60, 1)} Minuten";

    private static Kontingente.Limit Anzeige(Roh l)
    {
        var j = Jetzt;
        var abgelaufen = l.Reset > 0 && l.Reset <= j;
        var genutzt = abgelaufen ? 0 : l.Genutzt;
        var vergangen = l.Fenster - (l.Reset - (l.Gelesen > 0 ? l.Gelesen : j));
        var rechenbar = l.Reset > j && l.Fenster > 0 && l.Genutzt > 0 && vergangen >= l.Fenster / 10;
        double? hoch = rechenbar ? l.Genutzt * l.Fenster / vergangen : null;
        var schnell = l.Genutzt < 100 && (hoch ?? 0) > 100;
        var satz = l.Reset <= 0 ? "Das Zeitfenster beginnt mit der nächsten Nutzung" : abgelaufen ? "Zurückgesetzt, neuer Verbrauch erst beim nächsten Abruf" : "Zurückgesetzt: " + ZeitText(l.Reset);
        var tempo = "";
        if (l.Reset > 0 && !abgelaufen)
        {
            if (l.Genutzt >= 100) tempo = "Aufgebraucht bis zum Zurücksetzen.";
            else if (schnell)
            {
                var leer = l.Reset - l.Fenster + (long)(vergangen * 100 / l.Genutzt);
                tempo = $"Bei diesem Tempo wäre es {ZeitText(leer)} leer, {DauerText(l.Reset - leer)} vor dem Zurücksetzen.";
            }
            else tempo = hoch is { } h ? $"Tempo passt: hochgerechnet etwa {(int)h} % bis zum Zurücksetzen." : "Für eine Hochrechnung ist es noch zu früh.";
        }
        return new Kontingente.Limit(l.Name, genutzt, satz, l.Reset > 0 ? ZeitIn(l.Reset).ToString("dddd, dd.MM., HH:mm", De) + " Uhr" : "", l.Reset, tempo, schnell);
    }

    /// <summary>Der letzte Termin um 6 oder 18 Uhr deutscher Zeit: Bis zum nächsten bleibt der gespeicherte Stand, außer Frank fragt ausdrücklich neu.</summary>
    private static DateTimeOffset LetzterTermin()
    {
        var jetzt = TimeZoneInfo.ConvertTime(DateTime.UtcNow, Berlin);
        var t = jetzt.Date.AddHours(jetzt.Hour >= 18 ? 18 : jetzt.Hour >= 6 ? 6 : -6);
        return new DateTimeOffset(t, Berlin.GetUtcOffset(t));
    }

    private static string? agyPfad;
    public static string? AgyPfad()
    {
        if (agyPfad != null && File.Exists(agyPfad)) return agyPfad;
        var orte = new[] { Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "agy", "bin", "agy.exe"), Path.Combine(Pfade.Heim, ".agy", "bin", "agy.exe") }
            .Concat((Environment.GetEnvironmentVariable("PATH") ?? "").Split(';', StringSplitOptions.RemoveEmptyEntries).Select(p => Path.Combine(p, "agy.exe")));
        agyPfad = orte.FirstOrDefault(File.Exists);
        return agyPfad;
    }

    public static async Task<bool> HierMoeglich(string id) => id switch
    {
        "codex" => await CodexAnmeldung.Shared.Angemeldet(),
        "gemini" => AgyPfad() != null,
        _ => WebSitzungen.Angemeldet(id),
    };

    private static JSONObjekt Gespeichert() => JSON.Objekt(Vorgaben.Text(Speicher) ?? "{}") ?? new();

    /// <summary>Stand eines Dienstes am Laptop. Scheitert die Abfrage, bleibt der letzte Stand mit Hinweis.</summary>
    public static async Task<Kontingente.AnbieterStand> Lies(string id, string titel, bool frisch)
    {
        var alt = Gespeichert().Obj(id);
        var altLimits = alt?.Objekte("limits").Select(l => new Roh(l.Text("name") ?? "", l.Zahl("genutzt") ?? 0, l.Ganz("fenster") ?? 0, l.Ganz("reset") ?? 0, l.Ganz("gelesen") ?? 0)).ToList() ?? new();
        var altKonto = alt?.Text("konto") ?? "";
        var altGeholt = alt?.Ganz("geholt") ?? 0;
        string Stand(long g) => g <= 0 ? "" : "Stand " + ZeitIn(g).ToString("dd.MM., HH:mm", De) + " Uhr";
        Kontingente.AnbieterStand Aus(string konto, List<Roh> l, string hinweis, bool anmelden, long geholt)
            => new(id, titel, konto, hinweis, anmelden, Stand(geholt), l.Select(Anzeige).ToList());
        if (!frisch && altLimits.Count > 0 && (altGeholt >= LetzterTermin().ToUnixTimeSeconds() || GespraechLaeuft)) return Aus(altKonto, altLimits, "", false, altGeholt);
        try
        {
            var (konto, neu) = id switch
            {
                "claude" => await Claude(frisch),
                "codex" => await Codex(),
                "kimi" => await Kimi(frisch),
                "gemini" => await Gemini(),
                _ => throw new InvalidOperationException("Unbekannter Dienst."),
            };
            // Fehlt nur ein Limit, bleibt dessen letzter Stand. Gemini liefert alles auf einmal: Dort ist ein fehlender Name umbenannt oder entfallen.
            var fehlen = id == "gemini" ? new List<Roh>() : altLimits.Where(a => neu.All(n => n.Name != a.Name)).ToList();
            var alle = neu.Concat(fehlen).ToList();
            var geholt = Jetzt;
            var o = Gespeichert();
            o[id] = new JSONObjekt
            {
                ["konto"] = konto, ["geholt"] = geholt,
                ["limits"] = alle.Select(l => (object?)new JSONObjekt { ["name"] = l.Name, ["genutzt"] = l.Genutzt, ["fenster"] = l.Fenster, ["reset"] = l.Reset, ["gelesen"] = l.Gelesen }).ToList(),
            };
            Vorgaben.Setze(Speicher, JSON.Text(o));
            var erwartet = id == "kimi" ? 2 : 1;
            var hinweis = fehlen.Count > 0 || neu.Count < erwartet ? "Ein Limit ließ sich gerade nicht lesen" + (fehlen.Count > 0 ? ", dort steht der letzte Stand." : ".") : "";
            return Aus(konto, alle, hinweis, false, geholt);
        }
        catch (NichtAngemeldet f)
        {
            Log("guthaben", $"Kontingent {id}: {f.Message}", wo: "Konsolen");
            return Aus(altKonto, altLimits, altLimits.Count > 0 ? "Letzter Stand. " + f.Message : f.Message, true, altGeholt);
        }
        catch (Exception f)
        {
            Log("fehler", $"Kontingent {id} nicht abrufbar", wo: "Konsolen", fehler: f.Message);
            return Aus(altKonto, altLimits, altLimits.Count > 0 ? "Letzter Stand. " + f.Message : f.Message, false, altGeholt);
        }
    }

    public static void Vergessen(string id)
    {
        var o = Gespeichert();
        if (o.Remove(id)) Vorgaben.Setze(Speicher, JSON.Text(o));
    }

    private static bool ClaudeEndgueltig(string s)
    {
        var j = JSON.Objekt(s);
        if (j == null) return false;
        var c = j.Ganz("s");
        return c == 200 || c == 401 || (c == 403 && (j.Text("b") ?? "").TrimStart().StartsWith('{'));
    }

    private const string ClaudeSkript = """
        (function(){ if(location.host!=='claude.ai') return null;
            if(window.__jk===undefined){ window.__jk=null; (async()=>{
                const fertig=(s,b)=>{ window.__jk=JSON.stringify({s:s,b:b}); if(s!==200&&s!==401) setTimeout(()=>{window.__jk=undefined},3000); };
                try {
                    const a=await fetch('/api/organizations',{credentials:'include',headers:{Accept:'application/json'}});
                    if(!a.ok){ fertig(a.status,(await a.text()).slice(0,200)); return; }
                    const o=await a.json(); const org=o.find(x=>(x.capabilities||[]).includes('chat'))||o[0];
                    const r=await fetch('/api/organizations/'+org.uuid+'/usage',{credentials:'include',headers:{Accept:'application/json'}});
                    fertig(r.status,await r.text());
                } catch(e){ fertig(0,String(e)); } })(); }
            return window.__jk===undefined?null:window.__jk; })()
        """;

    private static async Task<(string, List<Roh>)> Claude(bool frisch)
    {
        if (!WebSitzungen.Angemeldet("claude")) throw new NichtAngemeldet("Noch nicht bei claude.ai angemeldet.");
        JSONObjekt? antwort = null;
        // Zuerst die leichte Datei; lässt die Bot-Abwehr das nicht zu, die echte Seite, die deren Prüfung löst.
        foreach (var (adresse, frist) in new[] { ("https://claude.ai/robots.txt", 8.0), ("https://claude.ai/new", 30.0) })
        {
            var s = await VersteckterLeser.Lies("claude", adresse, ClaudeSkript, frist, ClaudeEndgueltig, frisch);
            antwort = JSON.Objekt(s) ?? antwort;
            if (ClaudeEndgueltig(s)) break;
        }
        var code = antwort?.Ganz("s") ?? 0;
        var text = antwort?.Text("b") ?? "";
        if (code == 401 || (code == 403 && text.TrimStart().StartsWith('{'))) throw new NichtAngemeldet($"claude.ai lehnt die Anmeldung ab ({code}). Bitte neu anmelden.");
        if (code != 200) throw new InvalidOperationException(code == 0 ? "claude.ai antwortet gerade nicht." : $"claude.ai antwortet gerade nicht (Fehler {code}).");
        var j = JSON.Objekt(text) ?? throw new InvalidOperationException("claude.ai nennt keine Limits (Schnittstelle geändert?).");
        var limits = new List<Roh>();
        foreach (var (feld, name, fenster) in new[] { ("five_hour", "5-Stunden-Limit", Stunden5), ("seven_day", "Wochenlimit", Woche), ("seven_day_opus", "Wochenlimit Opus", Woche), ("seven_day_sonnet", "Wochenlimit Sonnet", Woche) })
        {
            if (j.Obj(feld) is not { } o) continue;
            var reset = DateTimeOffset.TryParse(o.Text("resets_at"), CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out var d) ? d.ToUnixTimeSeconds() : 0;
            limits.Add(new Roh(name, o.Zahl("utilization") ?? 0, fenster, reset, Jetzt));
        }
        if (limits.Count == 0) throw new InvalidOperationException("claude.ai nennt keine Limits (Schnittstelle geändert?).");
        return ("", limits);
    }

    private static async Task<(string, List<Roh>)> Codex()
    {
        if (!await CodexAnmeldung.Shared.Angemeldet()) throw new NichtAngemeldet("Noch nicht bei OpenAI angemeldet.");
        JSONObjekt? j = null;
        for (var versuch = 0; versuch < 2 && j == null; versuch++)
        {
            var (token, konto) = await CodexAnmeldung.Shared.GueltigerToken();
            using var req = new HttpRequestMessage(HttpMethod.Get, "https://chatgpt.com/backend-api/wham/usage");
            req.Headers.TryAddWithoutValidation("Accept", "application/json");
            req.Headers.TryAddWithoutValidation("Authorization", "Bearer " + token);
            req.Headers.TryAddWithoutValidation("originator", "codex_cli_rs");
            req.Headers.TryAddWithoutValidation("User-Agent", $"codex_cli_rs/{OpenAIKlient.CodexVersion} (JarvisWindows)");
            req.Headers.TryAddWithoutValidation("ChatGPT-Account-ID", konto);
            var (code, daten) = await GehirnNetz.Anfrage(req, 25);
            if ((code == 401 || code == 403) && versuch == 0) { await CodexAnmeldung.Shared.UngueltigMelden(code); continue; }
            if (code == 401 || code == 403) throw new NichtAngemeldet($"OpenAI lehnt die Anmeldung ab ({code}). Bitte neu anmelden.");
            if (code != 200) throw new InvalidOperationException($"OpenAI antwortet mit Fehler {code}.");
            j = JSON.Objekt(daten);
        }
        var rl = j?.Obj("rate_limit") ?? throw new InvalidOperationException("OpenAI nennt keine Limits (Schnittstelle geändert?).");
        var limits = new List<Roh>();
        foreach (var feld in new[] { "primary_window", "secondary_window" })
        {
            if (rl.Obj(feld) is not { } w) continue;
            var fenster = w.Ganz("limit_window_seconds") ?? Woche;
            var grund = fenster == Woche ? "Wochenlimit" : fenster == Stunden5 ? "5-Stunden-Limit" : $"Limit über {fenster / 3600} Stunden";
            limits.Add(new Roh(limits.Any(l => l.Name == grund) ? grund + " (2)" : grund, w.Zahl("used_percent") ?? 0, fenster, w.Ganz("reset_at") ?? 0, Jetzt));
        }
        if (limits.Count == 0) throw new InvalidOperationException("OpenAI nennt keine Limits (Schnittstelle geändert?).");
        return (string.Join(" · ", new[] { j!.Text("email"), j.Text("plan_type") }.Where(x => !string.IsNullOrWhiteSpace(x))), limits);
    }

    private const string Seitentext = "(function(){ return document.body ? (document.body.innerText || document.body.textContent || '') : null; })()";
    private static readonly Regex KimiGesamt = new("Gesamtnutzung|Total usage|Overall usage", RegexOptions.IgnoreCase);
    private static readonly Regex Kimi5h = new(@"Nutzung in 5 Stunden|(?<![\d.,])5[- ]?(?:hours?|h)\b", RegexOptions.IgnoreCase);
    private static readonly Regex KimiAnmelden = new(@"\b(Anmelden|Einloggen|Log in|Sign in)\b");
    private static readonly Regex Prozent = new(@"(\d+(?:[.,]\d+)?)\s*%");
    private static readonly Regex KimiZeit = new(@"(?:(\d{4})-)?(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})");

    /// <summary>„Gesamtnutzung 0.57% … Wird in 2026-11-28 19:08 zurückgesetzt“ und „Nutzung in 5 Stunden Code 6% Wird in 10-31 22:01 zurückgesetzt“.</summary>
    private static List<Roh> KimiLesen(string text)
    {
        var r = new List<Roh>();
        var g = KimiGesamt.Matches(text).LastOrDefault();
        if (g == null) return r;
        var s = Kimi5h.Matches(text).FirstOrDefault(m => m.Index > g.Index);
        var stunden = s?.Index ?? text.Length;
        var monat = KimiLimit(text[g.Index..stunden], "Monatliche Gesamtnutzung", 0);
        var fuenf = stunden < text.Length ? KimiLimit(text[stunden..], "5-Stunden-Limit", Stunden5) : null;
        if (fuenf != null) r.Add(fuenf);
        if (monat != null) r.Add(monat);
        return r;
    }

    public static bool KimiLesbar(string text) => KimiLesen(text).Count > 0;

    private static Roh? KimiLimit(string teil, string name, long fenster)
    {
        var p = Prozent.Match(teil);
        var z = KimiZeit.Match(teil);
        if (!p.Success || !z.Success || !double.TryParse(p.Groups[1].Value.Replace(',', '.'), CultureInfo.InvariantCulture, out var wert)) return null;
        var heute = DateTime.Today;
        DateTime d;
        try { d = new DateTime(z.Groups[1].Success ? int.Parse(z.Groups[1].Value) : heute.Year, int.Parse(z.Groups[2].Value), int.Parse(z.Groups[3].Value), int.Parse(z.Groups[4].Value), int.Parse(z.Groups[5].Value), 0, DateTimeKind.Local); }
        catch (ArgumentOutOfRangeException) { return null; }
        if (!z.Groups[1].Success && d.Date < heute.AddDays(-1)) d = d.AddYears(1);
        var ende = new DateTimeOffset(d).ToUnixTimeSeconds();
        // Der Monat ist unterschiedlich lang: Sein Fenster beginnt einen Kalendermonat vor dem Zurücksetzen.
        var f = fenster > 0 ? fenster : ende - new DateTimeOffset(d.AddMonths(-1)).ToUnixTimeSeconds();
        return new Roh(name, wert, f, ende, Jetzt);
    }

    private static async Task<(string, List<Roh>)> Kimi(bool frisch)
    {
        if (!WebSitzungen.Angemeldet("kimi")) throw new NichtAngemeldet("Noch nicht bei Kimi angemeldet.");
        var beginn = DateTime.Now;
        var text = await VersteckterLeser.Lies("kimi", KimiSeite, Seitentext, 30, s =>
            KimiLesen(s).Count == 2 || ((DateTime.Now - beginn).TotalSeconds > 12 && KimiLesen(s).Count == 0 && KimiAnmelden.IsMatch(s)), frisch);
        var limits = KimiLesen(text);
        if (limits.Count > 0) return ("", limits);
        if (text.Trim().Length == 0) throw new InvalidOperationException("Die Kimi-Seite hat nicht geladen.");
        if (KimiAnmelden.IsMatch(text)) throw new NichtAngemeldet("Kimi lehnt die Anmeldung ab. Bitte neu anmelden.");
        throw new InvalidOperationException("Die Kimi-Seite ließ sich nicht lesen (Seite geändert?).");
    }

    private static readonly Regex Ansi = new(@"\x1b\[[0-9;?]*[A-Za-z]");
    private static readonly Regex Fuenf = new(@"(five|5)[ -]?hour");
    private static readonly Dictionary<string, string> Gruppen = new() { ["Gemini Models"] = "Gemini-Modelle", ["Claude and GPT models"] = "Claude und GPT" };

    /// <summary>`agy -p /usage`: eine Zeile je Limit mit Tabulatoren: Gruppe, Art, Rest in Prozent, Zurücksetzen (UTC). Wie OpenLauncherServer/kern/agy_kontingent.py. Es zählt das Abo, nicht der Schlüssel.</summary>
    private static async Task<(string, List<Roh>)> Gemini()
    {
        var pfad = AgyPfad() ?? throw new InvalidOperationException("Die Antigravity-CLI ist auf diesem Laptop nicht installiert.");
        var r = await Shell.Prozess(pfad, new[] { "-p", "/usage" }, sekunden: 25, umgebung: new Dictionary<string, string> { ["GEMINI_API_KEY"] = "" });
        var limits = new List<Roh>();
        foreach (var zeile in Ansi.Replace(r.Ausgabe, "").Split('\n'))
        {
            var t = zeile.Split('\t').Select(x => x.Trim()).ToArray();
            if (t.Length != 4 || !t[2].EndsWith('%') || !double.TryParse(t[2].TrimEnd('%'), CultureInfo.InvariantCulture, out var rest)) continue;
            if (!DateTimeOffset.TryParse(t[3], CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out var reset)) continue;
            var art = t[1].ToLowerInvariant();
            var fuenf = Fuenf.IsMatch(art);
            // Eine unbekannte Art nicht als Woche ausgeben: Name und Fenster wären falsch.
            if (!fuenf && !art.Contains("week")) continue;
            limits.Add(new Roh($"{Gruppen.GetValueOrDefault(t[0], t[0])}: {(fuenf ? "5-Stunden-Limit" : "Wochenlimit")}", Math.Round(100 - rest, 1), fuenf ? Stunden5 : Woche, reset.ToUnixTimeSeconds(), Jetzt));
        }
        if (limits.Count > 0) return ("Antigravity-CLI", limits);
        var meldung = (r.Ausgabe + r.Fehler).Trim();
        if (meldung.Contains("login", StringComparison.OrdinalIgnoreCase) || meldung.Contains("auth", StringComparison.OrdinalIgnoreCase) || meldung.Contains("sign in", StringComparison.OrdinalIgnoreCase))
            throw new NichtAngemeldet("Die Antigravity-CLI ist nicht angemeldet.");
        throw new InvalidOperationException($"agy nennt keine Limits (Code {r.Code}): {meldung.Kappe(200)}");
    }
}

/// <summary>Das Google-Guthaben am Laptop, wie am Handy in GoogleGuthabenFaehigkeit.kt: über die Schnittstelle von AI Studio mit Franks Google-Sitzung.</summary>
public static class GoogleSitzung
{
    public sealed class Abgelaufen : Exception { public Abgelaufen() : base("Die Google-Anmeldung am Laptop ist abgelaufen. Bitte unter Einstellungen → Konsolen neu anmelden.") { } }

    public const string LoginURL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Faistudio.google.com%2Fbilling";
    private const string Studio = "https://aistudio.google.com";
    private const string Rpc = "https://alkalimakersuite-pa.clients6.google.com/$rpc/google.internal.alkali.applications.makersuite.v1.MakerSuiteService/";
    /// <summary>Öffentlicher Browser-Schlüssel der AI-Studio-Webseite (steht in deren Skript, kein Geheimnis von Frank).</summary>
    private const string StudioSchluessel = "AIzaSyDdP816MREB3SkjZO04QXbjsigfcI0GWOs";
    private const string Speicher = "google.laptop.stand";
    private static readonly CultureInfo De = CultureInfo.GetCultureInfo("de-DE");

    public static void Vergessen() => Vorgaben.Setze(Speicher, null);

    private static async Task<List<object?>?> Aufruf(string methode, string koerper)
    {
        var sap = WebSitzungen.Keks("google", "SAPISID") ?? throw new Abgelaufen();
        var ts = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var hash = Convert.ToHexString(SHA1.HashData(Encoding.UTF8.GetBytes($"{ts} {sap} {Studio}"))).ToLowerInvariant();
        using var req = new HttpRequestMessage(HttpMethod.Post, Rpc + methode) { Content = new StringContent(koerper, new UTF8Encoding(false)) };
        req.Content.Headers.TryAddWithoutValidation("Content-Type", "application/json+protobuf");
        req.Headers.TryAddWithoutValidation("Cookie", WebSitzungen.KeksKopf("google", Rpc));
        req.Headers.TryAddWithoutValidation("Authorization", $"SAPISIDHASH {ts}_{hash}");
        req.Headers.TryAddWithoutValidation("X-Goog-Api-Key", StudioSchluessel);
        req.Headers.TryAddWithoutValidation("X-Goog-AuthUser", "0");
        req.Headers.TryAddWithoutValidation("Origin", Studio);
        req.Headers.TryAddWithoutValidation("Referer", Studio + "/");
        req.Headers.TryAddWithoutValidation("User-Agent", ConsoleSitzung.BrowserKennung);
        var (code, daten) = await GehirnNetz.Anfrage(req, 25);
        if (code == 401 || code == 403) throw new Abgelaufen();
        if (code < 200 || code >= 300) throw new InvalidOperationException($"AI Studio antwortet mit Fehler {code}.");
        return JSON.Wert(daten) as List<object?> ?? throw new InvalidOperationException("AI Studio lieferte keine lesbare Antwort (Schnittstelle geändert?).");
    }

    private static List<object?>? L(List<object?>? a, int i) => a != null && i < a.Count ? a[i] as List<object?> : null;
    private static string S(List<object?>? a, int i) => a != null && i < a.Count ? a[i]?.ToString() ?? "" : "";
    private static double Geld(List<object?>? a)
    {
        if (a == null) return 0;
        var einheiten = long.TryParse(S(a, 1), out var e) ? e : 0;
        var nano = long.TryParse(S(a, 2), out var n) ? n : 0;
        return einheiten + nano / 1_000_000_000.0;
    }
    private static string Betrag(double w) => w.ToString("N2", De);
    private static string Datum(long s) => DateTimeOffset.FromUnixTimeSeconds(s).LocalDateTime.ToString("dd.MM.yyyy", De);
    private static string Tage(long t) => t <= 0 ? "heute" : t == 1 ? "morgen" : $"in {t} Tagen";
    private static long TageBis(DateTime d) => (long)(d.Date - DateTime.Today).TotalDays;

    private static async Task<double> Vorauszahlung(string kontoName)
    {
        var ebp = S(await Aufruf("GetEmbeddedPortalParameters", JSON.Text(new List<object?> { kontoName, 1L })), 0);
        if (ebp.Length == 0) throw new InvalidOperationException("AI Studio nennt keinen Zugang zur Zahlungsseite.");
        var url = "https://payments.google.com/payments/u/0/embedded_landing_page?hl=de&ebp=" + Uri.EscapeDataString(ebp);
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        req.Headers.TryAddWithoutValidation("Cookie", WebSitzungen.KeksKopf("google", url));
        req.Headers.TryAddWithoutValidation("User-Agent", ConsoleSitzung.BrowserKennung);
        req.Headers.TryAddWithoutValidation("Referer", Studio + "/");
        var (code, daten) = await GehirnNetz.Anfrage(req, 25);
        if (code < 200 || code >= 300) throw new InvalidOperationException($"Die Zahlungsseite von Google antwortet mit Fehler {code}.");
        var m = Regex.Match(Encoding.UTF8.GetString(daten), "class=\"b3-balance-card-headline\"[^>]*>([^<]+)<");
        if (!m.Success) throw new InvalidOperationException("Auf der Zahlungsseite steht keine Vorauszahlung (Seite geändert?).");
        var t = new string(WebUtility.HtmlDecode(m.Groups[1].Value).Where(c => char.IsDigit(c) || c is ',' or '.' or '-').ToArray()).Replace(".", "").Replace(',', '.');
        return double.TryParse(t, CultureInfo.InvariantCulture, out var w) ? w : throw new InvalidOperationException("Die Vorauszahlung ist nicht lesbar.");
    }

    private static async Task<(double Summe, double? Limit)?> Ausgaben()
    {
        var projekte = new List<string>();
        void Sammle(List<object?>? l)
        {
            foreach (var p in l?.OfType<List<object?>>() ?? Enumerable.Empty<List<object?>>())
                if (S(p, 0).StartsWith("projects/", StringComparison.Ordinal) && !projekte.Contains(S(p, 0))) projekte.Add(S(p, 0));
        }
        try { Sammle(L(await Aufruf("ListCloudProjects", "[]"), 0)); } catch (Abgelaufen) { throw; } catch { }
        try { Sammle(L(await Aufruf("ListImportedProjects", "[]"), 4)); } catch (Abgelaufen) { throw; } catch { }
        double summe = 0, limit = 0;
        var gefunden = false;
        foreach (var p in projekte)
        {
            // Projekte ohne Abrechnung antworten mit 403: Das sagt hier nichts über die Anmeldung.
            List<object?>? z;
            try { z = await Aufruf("GetProjectUsageLimit", JSON.Text(new List<object?> { p })); } catch { continue; }
            var ausgegeben = L(z, 2);
            if (ausgegeben == null) continue;
            gefunden = true;
            summe += Geld(ausgegeben);
            limit += Geld(L(z, 1));
        }
        return gefunden ? (summe, limit > 0.004 ? limit : null) : null;
    }

    /// <summary>Derselbe Satz wie das Werkzeug des Handys. 15 Minuten gilt der letzte Stand, außer frisch.</summary>
    public static async Task<string> Stand(bool frisch)
    {
        var alt = JSON.Objekt(Vorgaben.Text(Speicher) ?? "{}") ?? new();
        var jetztMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (!frisch && alt.Text("satz") is { } gemerkt && jetztMs - (alt.Ganz("geholt") ?? 0) < 15 * 60_000) return gemerkt;
        var konto = L(L(await Aufruf("ListBillingAccounts", "[]"), 0), 0) ?? throw new InvalidOperationException("Bei Google ist kein Rechnungskonto für AI Studio zu sehen.");
        var kontoName = S(konto, 0);
        var w = S(konto, 2);
        var waehrung = w == "EUR" || w.Length == 0 ? "€" : w;
        var zeilen = L(await Aufruf("BatchGetCredits", JSON.Text(new List<object?> { new List<object?> { kontoName } })), 0)?.OfType<List<object?>>().ToList() ?? new();
        var jetzt = jetztMs / 1000;
        var gutschriften = zeilen.Select(z => (Betrag: Geld(L(z, 2)), Verbraucht: Geld(L(z, 3)), Uebrig: Geld(L(z, 4)), Ende: long.TryParse(S(L(z, 8), 0), out var e) ? e : 0L))
            .Where(g => g.Ende == 0 || g.Ende >= jetzt).ToList();
        long letzte = 0;
        double? letzterBetrag = null;
        try
        {
            foreach (var b in L(await Aufruf("GetGdpProfileAndBenefits", "[]"), 2)?.OfType<List<object?>>() ?? Enumerable.Empty<List<object?>>())
                if (long.TryParse(S(L(b, 5), 0), out var beginn) && beginn > letzte) { letzte = beginn; letzterBetrag = Geld(L(b, 3)); }
        }
        catch (Abgelaufen) { throw; }
        catch (Exception ex) { Log("guthaben", "Gewährungen nicht lesbar", wo: "Konsolen", fehler: ex.Message); }
        double? betragAlt = alt.Zahl("vorauszahlung"), vorher = alt.Zahl("vorher");
        var seit = alt.Ganz("seit") ?? 0;
        var gelesen = alt.Ganz("gelesen") ?? 0;
        string? hinweis = null;
        var vz = betragAlt;
        try
        {
            var neu = await Vorauszahlung(kontoName);
            if (betragAlt == null) { seit = jetztMs; vorher = null; }
            else if (Math.Abs(neu - betragAlt.Value) >= 0.004) { seit = jetztMs; vorher = betragAlt; }
            else if (seit <= 0) seit = jetztMs;
            vz = neu;
            gelesen = jetztMs;
        }
        catch (Abgelaufen) { throw; }
        catch (Exception ex) { hinweis = "Vorauszahlung gerade nicht lesbar, das ist der letzte Betrag: " + ex.Message; }
        var monat = await Ausgaben();
        double? ausgaben = monat?.Summe ?? alt.Zahl("ausgaben");
        double? limitAusgaben = monat != null ? monat.Value.Limit : alt.Zahl("limit");

        var rest = gutschriften.Sum(g => g.Uebrig);
        var satz = new StringBuilder($"Google-Guthaben für die Gemini-Stimmen: übrig {Betrag(rest)} {waehrung} von {Betrag(gutschriften.Sum(g => g.Betrag))} {waehrung}, vom Guthaben abgebucht {Betrag(gutschriften.Sum(g => g.Verbraucht))} {waehrung}.");
        if (ausgaben is { } a)
        {
            satz.Append($" Ausgaben diesen Monat laut AI Studio: {Betrag(a)} {waehrung}");
            if (limitAusgaben is { } l) satz.Append($" von {Betrag(l)} {waehrung} Limit");
            satz.Append('.');
        }
        var offen = gutschriften.Where(g => g.Uebrig > 0.004).OrderBy(g => g.Ende).ToList();
        if (offen.Count > 0) satz.Append($" Als Nächstes verfallen {Betrag(offen[0].Uebrig)} {waehrung} am {Datum(offen[0].Ende)} ({Tage(TageBis(DateTimeOffset.FromUnixTimeSeconds(offen[0].Ende).LocalDateTime))}).");
        if (letzte > 0)
        {
            var d = DateTimeOffset.FromUnixTimeSeconds(letzte).LocalDateTime.Date.AddMonths(1);
            while (d < DateTime.Today) d = d.AddMonths(1);
            satz.Append($" Nächstes Guthaben voraussichtlich am {d.ToString("dd.MM.yyyy", De)} ({Tage(TageBis(d))})");
            if (letzterBetrag is { } lb) satz.Append($", etwa {Betrag(lb)} {waehrung}");
            satz.Append('.');
        }
        if (vz is { } v)
        {
            satz.Append($" Eingezahltes echtes Geld (Vorauszahlung): {Betrag(v)} {waehrung}");
            if (gelesen < jetztMs) satz.Append(", letzter lesbarer Betrag, gerade nicht geprüft.");
            else if (vorher is { } vo && v < vo - 0.004) satz.Append($", gesunken von {Betrag(vo)} {waehrung} am {Datum(seit / 1000)}.");
            else satz.Append(seit > 0 ? $", unverändert seit {Datum(seit / 1000)}." : ".");
        }
        satz.Append(" Die Vorauszahlung wird erst belastet, wenn das Guthaben aufgebraucht ist. Stand ").Append(DateTime.Now.ToString("dd.MM., HH:mm", De)).Append(" Uhr.");
        if (hinweis != null) satz.Append(' ').Append(hinweis);

        var speicher = new JSONObjekt { ["satz"] = satz.ToString(), ["geholt"] = jetztMs, ["seit"] = seit, ["gelesen"] = gelesen };
        if (vz != null) speicher["vorauszahlung"] = vz;
        if (vorher != null) speicher["vorher"] = vorher;
        if (ausgaben != null) speicher["ausgaben"] = ausgaben;
        if (limitAusgaben != null) speicher["limit"] = limitAusgaben;
        Vorgaben.Setze(Speicher, JSON.Text(speicher));
        return satz.ToString();
    }
}

/// <summary>Anmeldefenster für Google, claude.ai und Kimi. Schließt sich, sobald die Sitzung steht; danach holt der Aufrufer den Stand frisch.</summary>
public sealed class KonsolenAnmeldung : UserControl
{
    private static Window? offen;
    private readonly ConsoleBrowser browser;
    private readonly string id;

    private KonsolenAnmeldung(string id, string titel, string hinweis, string start, Action schliessen)
    {
        this.id = id;
        var g = new Grid { Width = 560, Height = 740 };
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        browser = new ConsoleBrowser(schliessen, new Uri(start), Erkannt);
        g.Children.Add(Rand(H(10,
            Fuell(V(2, T(titel, 15, FontWeights.SemiBold), T(hinweis, 12, null, Farbe.Text2))),
            Kapsel("checkmark", "Fertig", async () =>
            {
                if (browser.Kern is { } k) await WebSitzungen.Erfassen(id, k, true);
                schliessen();
            }, hervorgehoben: true),
            Kapsel("xmark", "Abbrechen", schliessen)), 14, 14, 14, 14));
        Grid.SetRow(browser, 1);
        g.Children.Add(browser);
        Background = B(Farbe.Grund2);
        Foreground = B(Farbe.Text);
        Content = g;
    }

    /// <summary>Fertig erkannt: gültige Sitzung gesichert, das Fenster schließt von selbst. Kimi nennt kein eindeutiges Cookie, dort zählen die lesbaren Limits.</summary>
    private async Task<bool> Erkannt(CoreWebView2 kern)
    {
        var host = Uri.TryCreate(kern.Source, UriKind.Absolute, out var u) ? u.Host : "";
        bool da;
        if (id == "kimi")
        {
            var text = JSON.Wert(Encoding.UTF8.GetBytes(await kern.ExecuteScriptAsync("(function(){ return document.body ? document.body.innerText : null; })()"))) as string;
            da = text != null && KontingentQuellen.KimiLesbar(text);
        }
        else
        {
            var name = id == "google" ? "SAPISID" : "sessionKey";
            var seite = id == "google" ? host.EndsWith("aistudio.google.com", StringComparison.Ordinal) : host == "claude.ai" && !u!.AbsolutePath.Contains("/login");
            var cookies = await kern.CookieManager.GetCookiesAsync("");
            da = seite && cookies.Any(c => c.Name == name && WebSitzungen.GehoertDazu(id, c.Domain));
        }
        if (da)
        {
            await WebSitzungen.Erfassen(id, kern, true);
            Log("guthaben", "Am Laptop angemeldet: " + id, wo: "Konsolen");
        }
        return da;
    }

    public static void Zeige(string id, string titel, string hinweis, string start, Func<Task> danach)
    {
        if (offen != null) { offen.Activate(); return; }
        var eigner = AktivesFenster();
        Window? w = null;
        var ansicht = new KonsolenAnmeldung(id, titel, hinweis, start, () => w?.Close());
        w = new Window
        {
            Title = titel, Content = ansicht, SizeToContent = SizeToContent.WidthAndHeight, ResizeMode = ResizeMode.NoResize, Owner = eigner, ShowInTaskbar = false,
            WindowStartupLocation = eigner == null ? WindowStartupLocation.CenterScreen : WindowStartupLocation.CenterOwner, Background = B(Farbe.Grund2),
        };
        offen = w;
        w.Closed += async (_, _) => { offen = null; await danach(); };
        w.Show();
    }
}

/// <summary>Einstellungen → Konsolen: Anmeldung am Laptop für alles, was unter Guthaben und Kontingente steht. Mit eigener Anmeldung liest Jarvis den Stand selbst, ohne sie kommt er vom Handy.</summary>
public sealed class KonsolenAnsicht : LebendeAnsicht
{
    private readonly Guthaben g = Guthaben.Shared;
    private readonly Kontingente k = Kontingente.Shared;
    private bool codexDa, agyDa;
    private string codexCode = "", codexStatus = "";

    public KonsolenAnsicht() { Beobachte(g); Beobachte(k); }

    protected override async void Erschienen()
    {
        agyDa = KontingentQuellen.AgyPfad() != null;
        codexDa = await CodexAnmeldung.Shared.Angemeldet();
        Neu();
        _ = g.Laden();
        _ = k.Laden();
    }

    private static (string Text, object Farbe) Stufe(bool hier, bool abgelaufen, bool handy)
    {
        if (hier && abgelaufen) return ("Abgelaufen, bitte neu anmelden", Farbe.Rot);
        if (hier) return ("Verbunden", Farbe.Gruen);
        if (handy) return ("Über das Handy", Farbe.Warnung);
        return ("Nicht verbunden", Farbe.Text3);
    }

    private UIElement Zeile(string symbol, string titel, string text, (string Text, object Farbe) stand, params UIElement?[] knoepfe)
    {
        var reihe = new List<UIElement?>(knoepfe) { Spacer() };
        return Karte(V(10,
            H(10, Fuell((FrameworkElement)Titel(symbol, titel)), Kreis(stand.Farbe), T(stand.Text, 12, FontWeights.SemiBold, stand.Farbe, 1)),
            T(text, 12, null, Farbe.Text2),
            H(10, reihe.ToArray())));
    }

    private bool VomHandy(string id) => k.VomHandy.Contains(id) && k.Anbieter.Any(a => a.Id == id && a.Limits.Count > 0);
    private bool Abgelehnt(string id) => k.Anbieter.Any(a => a.Id == id && a.Anmelden && !k.VomHandy.Contains(id));

    private UIElement Web(string id, string titel, string symbol, string text, string hinweis, string start)
    {
        var hier = WebSitzungen.Angemeldet(id);
        return Zeile(symbol, titel, text, Stufe(hier, Abgelehnt(id), VomHandy(id)),
            Kapsel("link", hier ? "Neu anmelden" : "Anmelden", () => KonsolenAnmeldung.Zeige(id, titel + " anmelden", hinweis, start, async () => { Neu(); await k.Laden(true); }), hervorgehoben: !hier),
            hier ? Kapsel("xmark", "Abmelden", async () =>
            {
                WebSitzungen.Abmelden(id);
                KontingentQuellen.Vergessen(id);
                Neu();
                await k.Laden(true);
            }) : null);
    }

    protected override UIElement Baue()
    {
        var s = V(18, T("Melde dich hier am Laptop an, dann liest Jarvis Guthaben und Kontingente direkt und schnell. Ohne eigene Anmeldung kommt der Stand vom Handy. Die Anmeldungen liegen verschlüsselt im Tresor.", 13, null, Farbe.Text2));

        VAnfuegen(s, T("Guthaben", 12, FontWeights.SemiBold, Farbe.Text3), 18);
        var anthropicHier = ConsoleSitzung.Angemeldet;
        VAnfuegen(s, Zeile("brain", "Anthropic-Konsole", "Guthaben und Kosten der Claude-Modelle.", Stufe(anthropicHier, g.PcAbgelaufen, g.AnthropicRest != null),
            Kapsel("link", anthropicHier ? "Neu anmelden" : "Anmelden", () => g.AnmeldungOffen = true, hervorgehoben: !anthropicHier),
            anthropicHier ? Kapsel("xmark", "Abmelden", async () =>
            {
                Log("guthaben", "Am Laptop bei der Console abgemeldet", wo: "Konsolen");
                g.Abmelden();
                await g.Laden(frisch: true);
            }) : null), 18);
        var googleHier = WebSitzungen.Angemeldet("google");
        VAnfuegen(s, Zeile("waveform", "Google AI Studio", "Guthaben der Gemini-Stimmen.", Stufe(googleHier, g.GoogleAbgelaufen, g.GoogleRest != null),
            Kapsel("link", googleHier ? "Neu anmelden" : "Anmelden", () => KonsolenAnmeldung.Zeige("google", "Bei Google anmelden", "Google lässt die Anmeldung in eingebetteten Browsern nicht immer zu. Dann bleibt der Stand vom Handy.", GoogleSitzung.LoginURL, async () => { Neu(); await g.Laden(frisch: true); }), hervorgehoben: !googleHier),
            googleHier ? Kapsel("xmark", "Abmelden", async () =>
            {
                WebSitzungen.Abmelden("google");
                GoogleSitzung.Vergessen();
                Neu();
                await g.Laden(frisch: true);
            }) : null), 18);

        VAnfuegen(s, T("Kontingente", 12, FontWeights.SemiBold, Farbe.Text3), 18);
        VAnfuegen(s, Web("claude", "Claude", "gauge.with.dots.needle.67percent", "Limits des Claude-Abos von claude.ai.", "Am besten per E-Mail-Code. Das Fenster schließt sich danach von selbst.", KontingentQuellen.ClaudeLogin), 18);

        var codexKnoepfe = new List<UIElement?>
        {
            Kapsel("link", codexDa ? "Neu anmelden" : "Anmelden", CodexAnmelden, hervorgehoben: !codexDa),
            codexDa ? Kapsel("xmark", "Abmelden", async () =>
            {
                if (!Bestaetigen("Bei ChatGPT abmelden?", "Das gilt auch für die GPT-Modelle von Jarvis auf diesem Laptop.", "Abmelden")) return;
                await CodexAnmeldung.Shared.Abmelden();
                KontingentQuellen.Vergessen("codex");
                codexDa = false;
                codexStatus = "Abgemeldet.";
                Neu();
                await k.Laden(true);
            }) : null,
        };
        VAnfuegen(s, Zeile("gauge.with.dots.needle.67percent", "ChatGPT / Codex", "Limits des ChatGPT-Abos. Dieselbe Anmeldung wie für die GPT-Modelle.", Stufe(codexDa, Abgelehnt("codex"), VomHandy("codex")), codexKnoepfe.ToArray()), 18);
        if (codexCode.Length > 0) VAnfuegen(s, T(codexCode, 24, FontWeights.Bold, Farbe.Haupt), 8);
        if (codexStatus.Length > 0) VAnfuegen(s, T(codexStatus, 12, null, Farbe.Text2), 8);

        VAnfuegen(s, Web("kimi", "Kimi", "gauge.with.dots.needle.67percent", "Limits des Kimi-Abos von kimi.ai.", "Anmelden, dann Fertig. Sobald die Limits sichtbar sind, schließt sich das Fenster von selbst.", KontingentQuellen.KimiSeite), 18);

        VAnfuegen(s, Zeile("gauge.with.dots.needle.67percent", "Gemini / Antigravity", agyDa
                ? "Limits der Antigravity-CLI. Sie liest diesen Laptop direkt, die Anmeldung liegt in der CLI selbst."
                : "Limits der Antigravity-CLI. Sie ist hier nicht installiert, darum kommt der Stand vom Handy.",
            Stufe(agyDa, Abgelehnt("gemini"), VomHandy("gemini")),
            agyDa ? Kapsel("link", "Im Terminal anmelden", () =>
            {
                var pfad = KontingentQuellen.AgyPfad()!;
                try { Shell.Starte("wt.exe", $"\"{pfad}\""); }
                catch { try { Shell.Starte("cmd.exe", $"/k \"\"{pfad}\"\""); } catch (Exception ex) { Log("fehler", "Terminal für agy nicht startbar", wo: "Konsolen", fehler: ex.Message); } }
            }) : null), 18);
        return s;
    }

    private async void CodexAnmelden()
    {
        try
        {
            var (code, url) = await CodexAnmeldung.Shared.Start();
            codexCode = code;
            Kopiere(code);
            codexStatus = "Code kopiert – im Browser einfügen und freigeben.";
            Neu();
            Oeffne(url);
            var email = await CodexAnmeldung.Shared.WarteAufFreigabe();
            codexCode = "";
            codexDa = true;
            codexStatus = $"Angemeldet als {email ?? "ChatGPT-Konto"}.";
            Neu();
            await k.Laden(true);
        }
        catch (Exception ex)
        {
            codexCode = "";
            codexStatus = $"Anmeldung fehlgeschlagen: {ex.Message}";
            Neu();
        }
    }
}
