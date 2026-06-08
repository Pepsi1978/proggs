using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// OpenAI-Codex-Anmeldung per Geraetecode (OAuth 2.0 Device Authorization Grant) — 1:1
    /// nachgebaut aus Hermes (hermes_cli/auth.py::_codex_device_code_login + _refresh_codex_auth_tokens).
    ///
    /// Damit nutzt der VoiceAgent das vorhandene ChatGPT-Abo ueber die interne Codex-Backend-API
    /// (chatgpt.com/backend-api/codex) statt ueber API-Credits. Es ist KEIN offizieller API-Weg:
    /// wir verwenden dieselbe Client-ID wie das echte Codex-CLI und geben uns spaeter beim
    /// Inferenz-Call als codex_cli_rs aus (siehe CodexProvider). OpenAI kann das jederzeit aendern.
    ///
    /// Tokens liegen — wie alle Geheimnisse — im SK-Ordner (~/SK/VoiceAgent/codex-auth.json),
    /// NIE im Repo und NIE in settings.json (Regel: secrets-in-sk-folder).
    /// </summary>
    public static class CodexAuth
    {
        // Konstanten 1:1 aus Hermes (hermes_cli/auth.py).
        public const string ClientId = "app_EMoamEEZ73f0CkXaXp7hrann";
        private const string Issuer = "https://auth.openai.com";
        private const string TokenUrl = Issuer + "/oauth/token";
        private const string DeviceUserCodeUrl = Issuer + "/api/accounts/deviceauth/usercode";
        private const string DevicePollUrl = Issuer + "/api/accounts/deviceauth/token";
        private const string DeviceVerifyUrl = Issuer + "/codex/device";        // Anzeige-URL fuer den Nutzer
        private const string RedirectUri = Issuer + "/deviceauth/callback";

        // Vor Ablauf so viele Sekunden vorher erneuern (Hermes: CODEX_ACCESS_TOKEN_REFRESH_SKEW_SECONDS).
        private const int RefreshSkewSeconds = 120;

        // Statischer, langlebiger Client gegen Socket-Exhaustion + stale DNS (Almanach §8.1/8.2).
        private static readonly HttpClient Http = new(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(3)
        })
        { Timeout = TimeSpan.FromSeconds(20) };

        private static readonly JsonSerializerOptions JsonOpts = new() { WriteIndented = true };

        private static readonly object FileLock = new();

        /// <summary>Pfad der Codex-Token-Datei im SK-Ordner.</summary>
        private static string AuthPath => Path.Combine(Config.SecretsDir, "codex-auth.json");

        /// <summary>Gespeicherte Codex-OAuth-Tokens (im SK-Ordner abgelegt).</summary>
        public sealed class CodexTokens
        {
            public string AccessToken { get; set; } = string.Empty;
            public string RefreshToken { get; set; } = string.Empty;
            public string AccountId { get; set; } = string.Empty;       // aus dem JWT extrahiert (ChatGPT-Account-ID)
            public string LastRefresh { get; set; } = string.Empty;     // ISO-8601 UTC
        }

        /// <summary>True, wenn bereits ein Refresh-Token gespeichert ist (also angemeldet).</summary>
        public static bool IsLoggedIn()
        {
            var t = Load();
            return t != null && !string.IsNullOrWhiteSpace(t.RefreshToken);
        }

        /// <summary>Meldet ab — loescht die gespeicherten Tokens.</summary>
        public static void Logout()
        {
            try
            {
                lock (FileLock)
                {
                    if (File.Exists(AuthPath)) File.Delete(AuthPath);
                }
                Log.Info("Codex: abgemeldet (Tokens geloescht)");
            }
            catch (Exception ex) { Log.Error("Codex: Abmelden fehlgeschlagen", ex); }
        }

        // ---------- Geraetecode-Login ----------

        /// <summary>Antwort von Schritt 1 (Geraetecode anfordern): Code + URL fuer den Nutzer.</summary>
        public sealed record DeviceCodeInfo(string UserCode, string VerificationUrl, string DeviceAuthId, int IntervalSeconds);

        /// <summary>
        /// Schritt 1: Fordert einen Geraetecode an. Der Aufrufer zeigt UserCode + VerificationUrl an
        /// und ruft danach <see cref="PollForTokensAsync"/>, bis der Nutzer im Browser bestaetigt hat.
        /// </summary>
        public static async Task<DeviceCodeInfo> RequestDeviceCodeAsync(CancellationToken ct = default)
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, DeviceUserCodeUrl)
            {
                Content = new StringContent(
                    JsonSerializer.Serialize(new { client_id = ClientId }), Encoding.UTF8, "application/json")
            };
            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Codex-Geraetecode anfordern fehlgeschlagen {(int)resp.StatusCode}: {body}");
                throw new Exception($"Codex-Geraetecode anfordern fehlgeschlagen ({(int)resp.StatusCode}).");
            }

            using var doc = JsonDocument.Parse(body);
            var root = doc.RootElement;
            var userCode = root.TryGetProperty("user_code", out var uc) ? uc.GetString() ?? "" : "";
            var deviceAuthId = root.TryGetProperty("device_auth_id", out var da) ? da.GetString() ?? "" : "";
            var interval = root.TryGetProperty("interval", out var iv) && iv.ValueKind == JsonValueKind.Number
                ? iv.GetInt32()
                : (root.TryGetProperty("interval", out var ivs) && int.TryParse(ivs.GetString(), out var p) ? p : 5);
            interval = Math.Max(3, interval);

            if (string.IsNullOrWhiteSpace(userCode) || string.IsNullOrWhiteSpace(deviceAuthId))
                throw new Exception("Codex-Geraetecode-Antwort unvollstaendig (kein user_code/device_auth_id).");

            Log.Info("Codex: Geraetecode angefordert");
            return new DeviceCodeInfo(userCode, DeviceVerifyUrl, deviceAuthId, interval);
        }

        /// <summary>
        /// Schritt 2+3: Pollt bis der Nutzer im Browser bestaetigt hat, tauscht dann den
        /// Authorization-Code gegen Tokens und SPEICHERT sie. Liefert die gespeicherten Tokens.
        /// 403/404 = Nutzer hat noch nicht bestaetigt (weiter pollen). Bricht nach maxWait ab.
        /// </summary>
        public static async Task<CodexTokens> PollForTokensAsync(
            DeviceCodeInfo info, CancellationToken ct = default)
        {
            var deadline = DateTime.UtcNow.AddMinutes(15);
            while (DateTime.UtcNow < deadline)
            {
                ct.ThrowIfCancellationRequested();
                await Task.Delay(info.IntervalSeconds * 1000, ct).ConfigureAwait(false);

                using var req = new HttpRequestMessage(HttpMethod.Post, DevicePollUrl)
                {
                    Content = new StringContent(
                        JsonSerializer.Serialize(new { device_auth_id = info.DeviceAuthId, user_code = info.UserCode }),
                        Encoding.UTF8, "application/json")
                };
                using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);

                if (resp.StatusCode == System.Net.HttpStatusCode.Forbidden ||
                    resp.StatusCode == System.Net.HttpStatusCode.NotFound)
                    continue;   // Nutzer hat noch nicht bestaetigt

                var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                if (!resp.IsSuccessStatusCode)
                {
                    Log.Error($"Codex-Polling Fehler {(int)resp.StatusCode}: {body}");
                    throw new Exception($"Codex-Anmeldung fehlgeschlagen beim Polling ({(int)resp.StatusCode}).");
                }

                using var doc = JsonDocument.Parse(body);
                var root = doc.RootElement;
                var authCode = root.TryGetProperty("authorization_code", out var ac) ? ac.GetString() ?? "" : "";
                var verifier = root.TryGetProperty("code_verifier", out var cv) ? cv.GetString() ?? "" : "";
                if (string.IsNullOrWhiteSpace(authCode) || string.IsNullOrWhiteSpace(verifier))
                    throw new Exception("Codex-Polling lieferte keinen authorization_code/code_verifier.");

                Log.Info("Codex: Browser-Bestaetigung erhalten, tausche Code gegen Tokens");
                return await ExchangeCodeAsync(authCode, verifier, ct).ConfigureAwait(false);
            }
            throw new TimeoutException("Codex-Anmeldung abgebrochen (15 Minuten ohne Bestaetigung).");
        }

        /// <summary>Schritt 4: Tauscht authorization_code gegen access/refresh-Token und speichert sie.</summary>
        private static async Task<CodexTokens> ExchangeCodeAsync(string authCode, string verifier, CancellationToken ct)
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, TokenUrl)
            {
                Content = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["grant_type"] = "authorization_code",
                    ["code"] = authCode,
                    ["redirect_uri"] = RedirectUri,
                    ["client_id"] = ClientId,
                    ["code_verifier"] = verifier,
                })
            };
            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Codex Token-Tausch Fehler {(int)resp.StatusCode}: {body}");
                throw new Exception($"Codex Token-Tausch fehlgeschlagen ({(int)resp.StatusCode}).");
            }

            var (access, refresh) = ParseTokenResponse(body);
            var tokens = new CodexTokens
            {
                AccessToken = access,
                RefreshToken = refresh,
                AccountId = ExtractAccountId(access),
                LastRefresh = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ"),
            };
            Save(tokens);
            Log.Info("Codex: Anmeldung erfolgreich, Tokens gespeichert");
            return tokens;
        }

        // ---------- Gueltigen Access-Token liefern (mit Auto-Refresh) ----------

        /// <summary>
        /// Liefert einen gueltigen Access-Token. Erneuert ihn automatisch ueber den Refresh-Token,
        /// wenn er (bald) ablaeuft. Wirft, wenn nicht angemeldet.
        /// </summary>
        public static async Task<string> GetValidAccessTokenAsync(CancellationToken ct = default)
        {
            var tokens = Load() ?? throw new InvalidOperationException(
                "Nicht bei Codex angemeldet (Einstellungen → Codex-Anmeldung per Geraetecode).");

            if (string.IsNullOrWhiteSpace(tokens.AccessToken) || IsExpiring(tokens.AccessToken, RefreshSkewSeconds))
            {
                Log.Info("Codex: Access-Token laeuft ab — erneuere ueber Refresh-Token");
                tokens = await RefreshAsync(tokens, ct).ConfigureAwait(false);
            }
            return tokens.AccessToken;
        }

        /// <summary>Die im JWT enthaltene ChatGPT-Account-ID (fuer den ChatGPT-Account-ID-Header).</summary>
        public static string GetAccountId()
        {
            var t = Load();
            if (t == null) return string.Empty;
            if (!string.IsNullOrWhiteSpace(t.AccountId)) return t.AccountId;
            return ExtractAccountId(t.AccessToken);
        }

        /// <summary>Erneuert den Access-Token ueber den Refresh-Token (Hermes: grant_type=refresh_token).</summary>
        private static async Task<CodexTokens> RefreshAsync(CodexTokens tokens, CancellationToken ct)
        {
            if (string.IsNullOrWhiteSpace(tokens.RefreshToken))
                throw new InvalidOperationException("Codex: kein Refresh-Token — bitte neu anmelden.");

            using var req = new HttpRequestMessage(HttpMethod.Post, TokenUrl)
            {
                Content = new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["grant_type"] = "refresh_token",
                    ["refresh_token"] = tokens.RefreshToken,
                    ["client_id"] = ClientId,
                })
            };
            req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            using var resp = await Http.SendAsync(req, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode)
            {
                Log.Error($"Codex Token-Refresh Fehler {(int)resp.StatusCode}: {body}");
                throw new Exception($"Codex Token-Refresh fehlgeschlagen ({(int)resp.StatusCode}). Ggf. neu anmelden.");
            }

            var (access, refresh) = ParseTokenResponse(body);
            // OpenAI gibt nicht immer einen neuen Refresh-Token zurueck — alten behalten (Hermes-Verhalten).
            tokens.AccessToken = access;
            if (!string.IsNullOrWhiteSpace(refresh)) tokens.RefreshToken = refresh;
            if (string.IsNullOrWhiteSpace(tokens.AccountId)) tokens.AccountId = ExtractAccountId(access);
            tokens.LastRefresh = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ");
            Save(tokens);
            Log.Info("Codex: Access-Token erneuert");
            return tokens;
        }

        // ---------- Hilfsfunktionen ----------

        private static (string access, string refresh) ParseTokenResponse(string json)
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            var access = root.TryGetProperty("access_token", out var a) ? a.GetString() ?? "" : "";
            var refresh = root.TryGetProperty("refresh_token", out var r) ? r.GetString() ?? "" : "";
            if (string.IsNullOrWhiteSpace(access))
                throw new Exception("Codex Token-Antwort enthielt keinen access_token.");
            return (access, refresh);
        }

        /// <summary>
        /// Liest die ChatGPT-Account-ID aus dem JWT (Claim "https://api.openai.com/auth".chatgpt_account_id).
        /// 1:1 aus Hermes (auxiliary_client.py::_codex_cloudflare_headers). Bei Fehler: leer (tolerant).
        /// </summary>
        public static string ExtractAccountId(string accessToken)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(accessToken)) return string.Empty;
                var parts = accessToken.Split('.');
                if (parts.Length < 2) return string.Empty;
                using var doc = JsonDocument.Parse(Encoding.UTF8.GetString(Base64UrlDecode(parts[1])));
                if (doc.RootElement.TryGetProperty("https://api.openai.com/auth", out var authClaim) &&
                    authClaim.TryGetProperty("chatgpt_account_id", out var acct) &&
                    acct.ValueKind == JsonValueKind.String)
                {
                    return acct.GetString() ?? string.Empty;
                }
            }
            catch (Exception ex) { Log.Warn($"Codex: Account-ID aus JWT lesen fehlgeschlagen: {ex.Message}"); }
            return string.Empty;
        }

        /// <summary>Prueft anhand des JWT-"exp"-Claims, ob der Token in skewSeconds ablaeuft.</summary>
        private static bool IsExpiring(string accessToken, int skewSeconds)
        {
            try
            {
                var parts = accessToken.Split('.');
                if (parts.Length < 2) return true;   // unlesbar -> sicherheitshalber erneuern
                using var doc = JsonDocument.Parse(Encoding.UTF8.GetString(Base64UrlDecode(parts[1])));
                if (doc.RootElement.TryGetProperty("exp", out var exp) && exp.ValueKind == JsonValueKind.Number)
                {
                    var expiresAt = DateTimeOffset.FromUnixTimeSeconds(exp.GetInt64());
                    return DateTimeOffset.UtcNow.AddSeconds(skewSeconds) >= expiresAt;
                }
            }
            catch { /* unlesbar -> erneuern */ }
            return true;
        }

        private static byte[] Base64UrlDecode(string input)
        {
            var s = input.Replace('-', '+').Replace('_', '/');
            switch (s.Length % 4) { case 2: s += "=="; break; case 3: s += "="; break; }
            return Convert.FromBase64String(s);
        }

        private static CodexTokens? Load()
        {
            try
            {
                lock (FileLock)
                {
                    if (!File.Exists(AuthPath)) return null;
                    return JsonSerializer.Deserialize<CodexTokens>(File.ReadAllText(AuthPath));
                }
            }
            catch (Exception ex) { Log.Error("Codex: Tokens lesen fehlgeschlagen", ex); return null; }
        }

        private static void Save(CodexTokens tokens)
        {
            try
            {
                lock (FileLock)
                {
                    Directory.CreateDirectory(Config.SecretsDir);
                    File.WriteAllText(AuthPath, JsonSerializer.Serialize(tokens, JsonOpts), Encoding.UTF8);
                }
            }
            catch (Exception ex) { Log.Error("Codex: Tokens speichern fehlgeschlagen", ex); }
        }
    }
}
