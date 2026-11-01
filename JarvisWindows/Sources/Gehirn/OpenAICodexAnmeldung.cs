using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;

namespace JarvisWindows;

/// <summary>Was im Tresor unter "codex" liegt.</summary>
public sealed class CodexSpeicher
{
    public string AccessToken { get; set; } = "";
    public string? RefreshToken { get; set; }
    public DateTime Ablauf { get; set; }
    public string? Konto { get; set; }
    public string? Email { get; set; }
}

/// <summary>Anmeldung mit Franks ChatGPT-Konto per Codex-Gerätecode – eigener Login nur für diesen Laptop.
/// Tokens liegen verschlüsselt im Tresor (Tresor.cs). NIE die Tokens des Handys oder aus
/// ~/SK/VoiceAgent mitbenutzen: OpenAI rotiert den Refresh-Token, zwei Geräte mit demselben Token beenden sich
/// gegenseitig (refresh_token_reused).
///
/// Ablauf für die Oberfläche:
///   var (code, url) = await CodexAnmeldung.Shared.Start();   // Code anzeigen + in Zwischenablage, url öffnen
///   var email = await CodexAnmeldung.Shared.WarteAufFreigabe(); // wartet, bis Frank im Browser bestätigt hat
///
/// Actor am Mac: Eigenschaften sind hier Methoden mit Task (wie Kern.cs `Angemeldet()`), der Zustand steht unter einem Schloss.</summary>
public sealed class CodexAnmeldung
{
    public static CodexAnmeldung Shared { get; } = new();

    public sealed class Fehler : Exception
    {
        private Fehler(string text) : base(text) { }
        public static Fehler NichtAngemeldet => new("Nicht mit ChatGPT angemeldet (Einstellungen → Konten → Mit ChatGPT anmelden).");
        public static Fehler Abgelaufen => new("Der Anmeldecode ist abgelaufen. Bitte die Anmeldung neu starten.");
        public static Fehler Antwort(string t) => new(t);
    }

    private const string ClientId = "app_EMoamEEZ73f0CkXaXp7hrann";
    private const string TokenURL = "https://auth.openai.com/oauth/token";
    private const string CodeURL = "https://auth.openai.com/api/accounts/deviceauth/usercode";
    private const string PruefURL = "https://auth.openai.com/api/accounts/deviceauth/token";
    public const string FreigabeURL = "https://auth.openai.com/codex/device";
    private const string Redirect = "https://auth.openai.com/deviceauth/callback";

    private readonly object schloss = new();
    private CodexSpeicher? speicher;
    private (string benutzerCode, string kennung, int intervall, DateTime bis)? offen;
    private Task<CodexSpeicher>? erneuerung;

    private CodexAnmeldung()
    {
        speicher = Tresor.Lade<CodexSpeicher>("codex");
        Geheimnisse.Setzen("codex", new[] { speicher?.AccessToken ?? "", speicher?.RefreshToken ?? "" });
    }

    public Task<bool> Angemeldet() { lock (schloss) return Task.FromResult(speicher != null); }
    public Task<string?> Email() { lock (schloss) return Task.FromResult(speicher?.Email); }

    public Task Abmelden()
    {
        lock (schloss) speicher = null;
        Tresor.Loesche("codex");
        Geheimnisse.Setzen("codex", Array.Empty<string>());
        return Task.CompletedTask;
    }

    /// <summary>Nach 401/403 vom Backend: Token sofort als abgelaufen behandeln, damit der nächste Aufruf erneuert.</summary>
    public Task UngueltigMelden(int code)
    {
        lock (schloss)
            // Nicht MinValue: GueltigerToken rechnet davon noch 120 Sekunden ab und würde sonst bei jedem weiteren Aufruf werfen.
            if (speicher != null) speicher = Kopie(speicher, DateTime.Now.AddDays(-1));
        return Task.CompletedTask;
    }

    private static CodexSpeicher Kopie(CodexSpeicher s, DateTime ablauf) => new()
    {
        AccessToken = s.AccessToken, RefreshToken = s.RefreshToken, Ablauf = ablauf, Konto = s.Konto, Email = s.Email,
    };

    // MARK: Gerätecode

    public async Task<(string benutzerCode, string url)> Start(CancellationToken ct = default)
    {
        var (code, text) = await Post(CodeURL, new JSONObjekt { ["client_id"] = ClientId }, ct);
        var o = code == 200 ? JSON.Objekt(text) : null;
        var benutzer = o?.Text("user_code")?.Trim();
        var kennung = o?.Text("device_auth_id")?.Trim();
        if (string.IsNullOrEmpty(benutzer) || string.IsNullOrEmpty(kennung))
            throw Fehler.Antwort($"OpenAI hat keinen Gerätecode geliefert ({code}): {text.Kappe(200)}");
        var iv = (int?)o!.Ganz("interval") ?? (int.TryParse(o.Text("interval"), out var z) ? z : 5);
        lock (schloss) offen = (benutzer, kennung, Math.Min(Math.Max(iv, 3), 30), DateTime.Now.AddMinutes(15));
        return (benutzer, FreigabeURL);
    }

    /// <summary>Fragt alle paar Sekunden nach, bis Frank den Code im Browser bestätigt hat. Gibt die E-Mail des Kontos zurück.</summary>
    public async Task<string?> WarteAufFreigabe(CancellationToken ct = default)
    {
        (string benutzerCode, string kennung, int intervall, DateTime bis) o;
        lock (schloss)
        {
            if (offen == null) throw Fehler.Antwort("Keine Anmeldung gestartet.");
            o = offen.Value;
        }
        while (DateTime.Now < o.bis)
        {
            await Task.Delay(TimeSpan.FromSeconds(o.intervall), ct);
            int code;
            string text;
            try
            {
                (code, text) = await Post(PruefURL, new JSONObjekt { ["device_auth_id"] = o.kennung, ["user_code"] = o.benutzerCode }, ct);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                o.intervall = Math.Min(o.intervall + 2, 30);
                continue;
            }
            if (code == 403 || code == 404 || code == 429 || code >= 500)
            {
                if (code == 429 || text.Contains("slow_down")) o.intervall = Math.Min(o.intervall + 5, 30);
                continue;
            }
            var j = code == 200 ? JSON.Objekt(text) : null;
            var ac = j?.Text("authorization_code");
            var verifier = j?.Text("code_verifier");
            if (string.IsNullOrEmpty(ac) || string.IsNullOrEmpty(verifier))
                throw Fehler.Antwort($"Geräteanmeldung abgelehnt ({code}): {text.Kappe(200)}");
            lock (schloss) offen = null;
            var (c2, t2) = await PostForm(TokenURL, new Dictionary<string, string>
            {
                ["grant_type"] = "authorization_code", ["client_id"] = ClientId, ["code"] = ac,
                ["redirect_uri"] = Redirect, ["code_verifier"] = verifier,
            }, ct);
            var s = SpeicherAus(c2, t2, null);
            Sichern(s);
            Log("system", "ChatGPT-Anmeldung am Laptop erfolgreich" + (s.Email != null ? $" ({s.Email})" : ""));
            return s.Email;
        }
        lock (schloss) offen = null;
        throw Fehler.Abgelaufen;
    }

    // MARK: Token

    /// <summary>Gültiger Zugriffstoken und ChatGPT-Konto. Erneuerung serialisiert: parallele Aufrufer warten auf dieselbe.</summary>
    public async Task<(string token, string konto)> GueltigerToken(CancellationToken ct = default)
    {
        CodexSpeicher s;
        Task<CodexSpeicher>? laufend;
        Task<CodexSpeicher> t;
        lock (schloss)
        {
            if (speicher == null) throw Fehler.NichtAngemeldet;
            s = speicher;
            if (DateTime.Now < s.Ablauf.AddSeconds(-120) && s.Konto != null) return (s.AccessToken, s.Konto);
            laufend = erneuerung;
            if (laufend == null)
            {
                if (s.RefreshToken == null) t = null!;
                else
                {
                    var refresh = s.RefreshToken;
                    var alt = s;
                    // Eigene Aufgabe ohne ct: Ein Stopp darf einen schon rotierten Refresh-Token nicht verlieren.
                    t = Task.Run(async () =>
                    {
                        var (c, text) = await PostForm(TokenURL, new Dictionary<string, string>
                        {
                            ["grant_type"] = "refresh_token", ["refresh_token"] = refresh, ["client_id"] = ClientId,
                        }, default);
                        return SpeicherAus(c, text, alt);
                    });
                    erneuerung = t;
                }
            }
            else t = laufend;
        }
        if (laufend != null)
        {
            var n = await laufend.WaitAsync(ct);
            return (n.AccessToken, n.Konto ?? "");
        }
        if (t == null)
        {
            await Abmelden();
            throw Fehler.NichtAngemeldet;
        }
        try
        {
            var n = await t;
            Sichern(n);
            if (n.Konto == null) throw Fehler.Antwort("Im Codex-Token fehlt die ChatGPT-Account-ID.");
            return (n.AccessToken, n.Konto);
        }
        catch (Exception ex)
        {
            var txt = ex.Message.ToLowerInvariant();
            if (txt.Contains("invalid_grant") || txt.Contains("refresh_token_reused") || txt.Contains("(401)"))
            {
                await Abmelden();
                Log("fehler", "ChatGPT-Anmeldung am Laptop ungültig, bitte neu anmelden");
            }
            throw;
        }
        finally
        {
            lock (schloss) erneuerung = null;
        }
    }

    private void Sichern(CodexSpeicher s)
    {
        lock (schloss) speicher = s;
        Tresor.Sichere(s, "codex");
        Geheimnisse.Setzen("codex", new[] { s.AccessToken, s.RefreshToken ?? "" });
    }

    private static CodexSpeicher SpeicherAus(int code, string text, CodexSpeicher? alt)
    {
        var j = code == 200 ? JSON.Objekt(text) : null;
        var access = j?.Text("access_token");
        if (string.IsNullOrEmpty(access)) throw Fehler.Antwort($"OpenAI-Anmeldung fehlgeschlagen ({code}): {text.Kappe(200)}");
        var id = j!.Text("id_token");
        var laufzeit = j.Zahl("expires_in") ?? 3600;
        var neuerRefresh = j.Text("refresh_token");
        return new CodexSpeicher
        {
            AccessToken = access,
            RefreshToken = string.IsNullOrEmpty(neuerRefresh) ? alt?.RefreshToken : neuerRefresh,
            Ablauf = DateTime.Now.AddSeconds(laufzeit),
            Konto = AuthWert(access, "chatgpt_account_id") ?? AuthWert(access, "account_id")
                ?? (id != null ? AuthWert(id, "chatgpt_account_id") : null) ?? alt?.Konto,
            Email = (id != null ? Jwt(id)?.Text("email") : null) ?? Jwt(access)?.Text("email")
                ?? AuthWert(access, "email") ?? alt?.Email,
        };
    }

    private static JSONObjekt? Jwt(string token)
    {
        var teile = token.Split('.', StringSplitOptions.RemoveEmptyEntries);
        if (teile.Length <= 1) return null;
        var b = teile[1].Replace('-', '+').Replace('_', '/');
        while (b.Length % 4 != 0) b += "=";
        try { return JSON.Objekt(Convert.FromBase64String(b)); } catch (FormatException) { return null; }
    }

    private static string? AuthWert(string token, string name)
    {
        var v = Jwt(token)?.Obj("https://api.openai.com/auth")?.Text(name);
        return string.IsNullOrEmpty(v) ? null : v;
    }

    // MARK: HTTP

    private static async Task<(int, string)> Post(string url, JSONObjekt json, CancellationToken ct)
    {
        using var r = new HttpRequestMessage(HttpMethod.Post, url) { Content = GehirnNetz.JsonInhalt(json) };
        r.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        var (code, d) = await GehirnNetz.Anfrage(r, 25, ct);
        return (code, Encoding.UTF8.GetString(d));
    }

    private static async Task<(int, string)> PostForm(string url, Dictionary<string, string> felder, CancellationToken ct)
    {
        var rumpf = string.Join("&", felder.Select(f => $"{f.Key}={Uri.EscapeDataString(f.Value)}"));
        using var r = new HttpRequestMessage(HttpMethod.Post, url);
        r.Content = new StringContent(rumpf, new UTF8Encoding(false));
        r.Content.Headers.ContentType = new MediaTypeHeaderValue("application/x-www-form-urlencoded");
        r.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        var (code, d) = await GehirnNetz.Anfrage(r, 25, ct);
        return (code, Encoding.UTF8.GetString(d));
    }
}
