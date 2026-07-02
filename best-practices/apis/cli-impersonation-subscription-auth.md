# CLI-Impersonation / Subscription-Auth — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/cli-impersonation-subscription-auth.md`. (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
> Hinweis: ToS/Rechtslage je Anbieter beachten — dies ist technische Doku, keine Rechtsberatung.
> Update 2026-07-02: Abo-OAuth bleibt nur fuer offizielle/zulässige Clients vertretbar; zusaetzlich Kontingent-Kopplung beachten (Codex und ChatGPT teilen bei Plus/Pro ein rollierendes 5-Stunden-Fenster).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Abo-OAuth in eigener App | Legalitaet/Anwendungsfall zuerst klaeren | §1 |
| 2 | OAuth in Headless/CI | Device-Code-Flow bzw. `claude setup-token` nutzen | §2 |
| 3 | Token-Refresh speichern | Immer mergen, refresh_token nie ueberschreiben | §3 |
| 4 | Tokens ablegen | Wie Passwort; Keyring > Klartext, nie committen | §4 |
| 5 | Header/Client-Identitaet | Header-Imitation ist keine Dauerstrategie | §5 |
| 6 | Mehrere Credentials gesetzt | Praezedenz beachten — sonst stiller 401 | §6 |
| 7 | 401/403 behandeln | 401 = Refresh; 403-Client-Block = umstellen | §7 |
| 8 | Abo-Limits respektieren | Cachen statt hammern; API-Key-Fallback einbauen | §8 |
| 9 | Hermes→Codex konkret nachbauen | Voller Code-Weg: Client-ID `app_EMoam…`, Endpoint `chatgpt.com/backend-api/codex`, Header `originator: codex_cli_rs` + `ChatGPT-Account-ID` aus JWT | §9 |

## 1. Rechtlicher Rahmen ZUERST klaeren (defensiv)
- Offizielle CLI mit eigenem Abo-OAuth ist erlaubt. Abo-OAuth-Tokens (Free/Pro/Max) in Drittsoftware sind bei Anthropic ein ToS-Verstoss und werden server-seitig geblockt — fuer eigene Apps/Agents stattdessen API-Key oder die offizielle CLI nutzen. Quelle: https://platform.claude.com/docs/en/manage-claude/authentication · offiziell
- Codex-Plugin-Doku schraenkt selbst ein: nur „personal development use", nicht fuer kommerzielle Mehr-Nutzer-Dienste oder API-Resale. Vor jeder Nutzung pruefen, ob der Anwendungsfall (privat vs. kommerziell, ein Nutzer vs. viele) ueberhaupt zulaessig ist. Quelle: https://github.com/numman-ali/opencode-openai-codex-auth · community

## 2. OAuth/Geraetecode-Flow korrekt umsetzen
- Standard ist Browser-OAuth mit PKCE und lokalem Callback (Codex: `localhost:1455`). In Headless/SSH/Container den Device-Code-Flow nutzen (`codex login --device-auth`) — der braucht oft Personal- oder Workspace-Admin-Freigabe. Quelle: https://developers.openai.com/codex/auth · offiziell
- Wenn der lokale Callback nicht erreichbar ist (WSL2/SSH), den vom Browser angezeigten Login-Code manuell in die CLI einfuegen statt auf den Redirect zu warten. Quelle: https://code.claude.com/docs/en/authentication · offiziell
- Fuer CI/Skripte Claude Code: einmalig `claude setup-token` (Ein-Jahres-Token, nur Inference-Scope) erzeugen und als `CLAUDE_CODE_OAUTH_TOKEN` setzen; das Token wird nicht automatisch gespeichert. Quelle: https://code.claude.com/docs/en/authentication · offiziell

## 3. Token-Refresh: niemals den refresh_token ueberschreiben
- Die CLI refresht Access-Tokens automatisch vor Ablauf — keine eigene Refresh-Logik nachbauen, sondern die CLI/das auth-File die Tokens in place aktualisieren lassen. Quelle: https://developers.openai.com/codex/auth/ci-cd-auth · offiziell
- Klassischer Bug: Beim Refresh liefert das `tokens`-Event nur den neuen `access_token`, NICHT den `refresh_token` — wer das Credential-File komplett ueberschreibt, verliert den refresh_token („No refresh token is set" nach ~1 h). Beim Speichern immer mergen, nie blind ersetzen. Quelle: https://github.com/google-gemini/gemini-cli/issues/21691 · community
- Access-Tokens sind kurzlebig (Gemini ~1 h, Codex ~8-10 Tage); Refresh-Token kann anbieterseitig widerrufen werden. Auf Reuse-/Relogin-Pfad vorbereiten statt in generische Fehler zu kippen. Quelle: https://codex.danielvaughan.com/2026/04/01/codex-cli-authentication-flows-credential-management/ · community

## 4. Sichere Token-Ablage
- Auth-Dateien (`~/.codex/auth.json`, `~/.gemini/oauth_creds.json`, Claude `.credentials.json`) wie Passwoerter behandeln: nie committen, nie in Logs/Tickets, nicht zwischen parallelen Jobs teilen. Quelle: https://developers.openai.com/codex/auth/ci-cd-auth · offiziell
- Bevorzugt OS-Keyring statt Klartext-Datei (`cli_auth_credentials_store: keyring|auto`). Claude Code nutzt macOS-Keychain bzw. unter Linux/Windows `.credentials.json` mit Mode `0600` / Profil-ACLs. Quelle: https://code.claude.com/docs/en/authentication · offiziell
- In CI: `auth.json` NUR wenn fehlend seeden, danach das refreshte File zurueck in den Secret-Store schreiben (ephemere Runner) bzw. persistent auf Self-hosted-Runnern liegen lassen — nie das Original-Secret bei jedem Run drueberschreiben. Quelle: https://developers.openai.com/codex/auth/ci-cd-auth · offiziell

## 5. Notwendige Header / Client-Identitaet
- OAuth-Tokens gehen als `Authorization: Bearer`; bei Anthropic-Proxy/Gateway `ANTHROPIC_AUTH_TOKEN` (Bearer) statt `ANTHROPIC_API_KEY` (`X-Api-Key`). Quelle: https://code.claude.com/docs/en/authentication · offiziell
- Codex signalisiert den Client per `originator` (`codex_cli_rs` / `codex_vscode`) und fester Client-ID; Anthropic-OAuth braucht das `anthropic-beta`-oauth-Feature. WICHTIG: Anthropic prueft seit 09.01.2026 die echte Client-Identitaet server-seitig — Header allein machen aus Drittsoftware keine legitime CLI. Quelle: bugs/apis/cli-impersonation-subscription-auth.md (C1) · offiziell-belegt
- Header/Client-IDs sind anbieter-internes Vertragswerk und driften — auf eigene Header-Imitation NICHT als dauerhafte Strategie setzen. Quelle: https://www.theregister.com/2026/02/20/anthropic_clarifies_ban_third_party_claude_access/ · community

## 6. Endpunkt-Eigenheiten & Reasoning-Effort
- Endpunkte je CLI: Codex `auth.openai.com` (OAuth) + ChatGPT-Backend; Claude `Authorization: Bearer`; Gemini OAuth Google-Account. Endpunkte rotieren — 401/403 nach Anbieter-Update einplanen. Quelle: https://developers.openai.com/codex/auth · offiziell
- Reasoning-/Effort-Felder werden ueber die jeweilige CLI-Config gesteuert (Codex enforct Login/Modell per Managed-Config wie `forced_login_method`); fuer Konzern-Proxys `CODEX_CA_CERTIFICATE` setzen. Quelle: https://developers.openai.com/codex/auth · offiziell
- Claude Code: bei mehreren gesetzten Credentials gilt eine feste Praezedenz (Bedrock/Vertex/Foundry → `ANTHROPIC_AUTH_TOKEN` → `ANTHROPIC_API_KEY` → `apiKeyHelper` → `CLAUDE_CODE_OAUTH_TOKEN` → Abo-OAuth). Falsche Reihenfolge = stiller Credential-Tausch → erst recht 401. Quelle: https://code.claude.com/docs/en/authentication · offiziell

## 7. Robustes Fehler-Handling (401/403/Expiry)
- 401 als Refresh-Trigger behandeln (Claude `apiKeyHelper` wird nach 5 min ODER bei HTTP 401 erneut gerufen, TTL via `CLAUDE_CODE_API_KEY_HELPER_TTL_MS`). Bei 401/403 erst Refresh, dann definierter Relogin-Pfad, nicht endlos retryen. Quelle: https://code.claude.com/docs/en/authentication · offiziell
- 403 „credential only authorized for use with Claude Code" = harter Client-Identity-Block, KEIN transienter Fehler — nicht durch Retry/Header-Tweaks umgehen, sondern auf API-Key/offizielle CLI umstellen. Quelle: bugs/apis/cli-impersonation-subscription-auth.md (C1) · offiziell-belegt
- Bei persistenten 429 (Gemini) hilft Re-Auth der OAuth-Session; Refresh-Fehlschlag → `codex login` / `gemini login` auf einer Trusted-Maschine und Secret ersetzen. Quelle: https://github.com/google-gemini/gemini-cli/issues/24384 · community

## 8. Rate-Limit-Fairness & Versions-Drift
- Abo-Limits sind keine API-Limits: Codex zaehlt gegen geteilte „agentic usage" (April 2026 von Message- auf Token-Basis umgestellt); Drittsoftware soll diese Limits respektieren, nicht maximal ausreizen (Caching statt Hammern — Plugins cachen z. B. 15 min). Quelle: bugs/apis/cli-impersonation-subscription-auth.md (E1, B2) · belegt
- CLIs aendern Client-ID/Originator/Endpunkte/Runtime regelmaessig — wer eine Signatur nachbaut, hat dauerhaften Wartungsaufwand und ploetzliche Totalausfaelle. Robuster: API-Key-Pfad als Fallback fest einbauen. Quelle: https://hermesagents.net/blog/hermes-proxy-claude-pro-aider-cline-codex · community
- Auffaellige Nutzungsmuster loesen Ban-Heuristik + Refresh-Token-Revocation aus — defensives, abo-konformes Volumen einhalten und Account-Risiko ehrlich gegen den Nutzen abwaegen. Quelle: bugs/apis/cli-impersonation-subscription-auth.md (E2) · belegt

## 9. Fallstudie: Der komplette Weg, wie Hermes sich als Codex ausgibt (aus dem Quellcode)

> **Quelle:** lokal heruntergeladenes Repo `NousResearch/hermes-agent` (`~/hermes-agent`, Stand
> Repo-Dateien 2026-06-07; analysiert 2026-06-28). Hermes Agent 2026.5, Codex CLI ab 0.130.0.
> Belegende Dateien: `hermes_cli/auth.py`, `agent/auxiliary_client.py`, `agent/account_usage.py`,
> `agent/codex_runtime.py`, `website/docs/user-guide/features/codex-app-server-runtime.md`.
> **Ehrlich:** Dies ist die technische Mechanik, wie eine Drittsoftware das ChatGPT-Abo statt eines
> API-Keys nutzt, indem sie sich als die offizielle Codex-CLI ausgibt. ToS-Grauzone bis -Verstoss
> (siehe §1 + Almanach B1/E2) — hier als nachschlagbares Wissen dokumentiert, nicht als Empfehlung.

### 9.0 Zwei Betriebsmodi (Hermes kann beides)
| Modus | Was passiert | Wann |
|-------|--------------|------|
| **`codex_responses`** (die eigentliche Impersonation) | Hermes spricht mit dem Subscription-Token **direkt** gegen das ChatGPT-Codex-Backend und setzt die Codex-Client-Header selbst. Hermes besitzt die Agent-Schleife. | Default-Weg, um das Abo als LLM-Backend zu nutzen |
| **`codex_app_server`** (Codex wrappen) | Hermes startet die **echte** `codex`-CLI als Subprozess (JSON-RPC über stdio), nutzt deren `~/.codex/auth.json`, und reicht Tools per MCP-Callback rein. Codex besitzt die Tool-Schleife. | opt-in: `/codex-runtime codex_app_server` |

Der "vorgaukeln, Codex zu sein"-Kern steckt in **`codex_responses`** (§9.1–9.6). `codex_app_server`
gibt sich gar nicht selbst aus — es benutzt das echte Binary (§9.8b).

### 9.1 Die feste Codex-Identität (exakte Konstanten, `hermes_cli/auth.py`)
```
CODEX_OAUTH_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"   # die PUBLIC Client-ID der echten Codex-CLI
CODEX_OAUTH_TOKEN_URL = "https://auth.openai.com/oauth/token"
DEFAULT_CODEX_BASE_URL = "https://chatgpt.com/backend-api/codex"   # ChatGPT-Backend, NICHT api.openai.com
CODEX_ACCESS_TOKEN_REFRESH_SKEW_SECONDS = 120            # 2 Min vor Ablauf erneuern
```
Der entscheidende Trick steckt in der **Base-URL**: Anfragen gehen an `chatgpt.com/backend-api/codex`
(zählt gegen das **Abo**), nicht an `api.openai.com` (zählt gegen **API-Billing**). Die Client-ID ist
die öffentliche der echten Codex-CLI — sie ist kein Geheimnis, sondern die Identität, als die man auftritt.

### 9.2 Token-Beschaffung — aus `~/.codex/auth.json` importieren (nicht neu erfinden)
Hermes baut **keinen** eigenen Browser-OAuth nach, sondern liest die Tokens, die `codex login` bereits
erzeugt hat (`_import_codex_cli_tokens`):
- Liest `${CODEX_HOME:-~/.codex}/auth.json` → `tokens.access_token` + `tokens.refresh_token`.
- **Schreibt NIE in `~/.codex/auth.json` zurück** (würde die echte Codex-CLI stören). Hermes legt seine
  Arbeitskopie getrennt in `~/.hermes/auth.json` ab.
- Lehnt bereits abgelaufene Tokens beim Import ab (sonst „Login successful" ohne nutzbares Credential).
- **Warum getrennt halten (KRITISCH):** Der Refresh-Token ist **One-Time-Use** (OpenAI rotiert ihn).
  Würden Hermes UND die echte Codex-CLI denselben Token refreshen, „verbraucht" der eine den Token des
  anderen → Fehler `refresh_token_reused`. Deshalb zwei getrennte Stores.

### 9.3 Token-Refresh (`refresh_codex_oauth_pure`)
```
POST https://auth.openai.com/oauth/token
Content-Type: application/x-www-form-urlencoded
grant_type=refresh_token & refresh_token=<…> & client_id=app_EMoamEEZ73f0CkXaXp7hrann
```
- **Public Client:** KEIN `client_secret` (PKCE-Stil). Nur Client-ID + Refresh-Token.
- Antwort liefert neuen `access_token` (Refresh-Token bleibt i.d.R. gleich) → **gemerged** speichern,
  nie das ganze File überschreiben (sonst refresh_token-Verlust, siehe §3).
- Fehler-Klassifikation (wichtig fürs Handling): `429` = Abo-Quota erschöpft, Credentials noch gültig →
  „später retry", **nicht** relogin. `401/403` bzw. `invalid_grant`/`invalid_token` → relogin nötig.
  `refresh_token_reused` → echte Codex-CLI hat den Token konsumiert → `codex` neu ausführen.

### 9.4 Die Wire-Header — DAS Herzstück der Impersonation (`agent/auxiliary_client.py`, `_codex_cloudflare_headers`)
Jede Inferenz-Anfrage an `chatgpt.com/backend-api/codex` trägt:
```
Authorization:    Bearer <access_token>
originator:       codex_cli_rs                          # gibt sich als upstream codex-rs CLI aus
User-Agent:       codex_cli_rs/0.0.0 (Hermes Agent)     # codex-förmig (schlägt SDK-Fingerprinting)
ChatGPT-Account-ID: <chatgpt_account_id aus dem JWT>    # siehe unten
```
Der `ChatGPT-Account-ID`-Wert wird **aus dem Access-Token selbst** geholt: Das Token ist ein JWT; Hermes
base64-dekodiert den Payload (Teil 2) und liest den Claim `["https://api.openai.com/auth"]["chatgpt_account_id"]`.
Fehlt/kaputt → Header wird weggelassen (führt zu sauberem 401 statt Crash). `agent/account_usage.py` setzt
denselben Header (dort Schreibweise `ChatGPT-Account-Id`) für die Usage-Abfrage.

### 9.5 Die Cloudflare-Hürde — warum `originator` zwingend ist
Vor `chatgpt.com/backend-api/codex` sitzt ein Cloudflare-Layer, der **nur first-party Originatoren**
durchlässt: `codex_cli_rs`, `codex_vscode`, `codex_sdk_ts`, alles mit Prefix `Codex`. Anfragen von
**Nicht-Residential-IPs** (VPS, server-gehostete Agents) ohne erlaubten `originator` bekommen **403 mit
`cf-mitigated: challenge`** — **unabhängig davon, ob die Auth korrekt ist**. Deshalb pinnt Hermes
`originator: codex_cli_rs` + den codex-förmigen User-Agent. Ohne diese beiden Header: 403, selbst mit
gültigem Token.

### 9.6 Modell-Allow-List driftet (kein hardcoded Default)
Hermes pinnt bewusst **kein** Standard-Modell für diesen Endpoint. Die auf dem ChatGPT-Account-Endpoint
akzeptierten Modelle sind eine **undokumentierte, wandernde Allow-List** (Kommentar im Code: in 6 Wochen
Anfang 2026 `gpt-5.3-codex` → `gpt-5.2-codex` → `gpt-5.4`). Der Aufrufer MUSS das Modell explizit übergeben.
Lektion: ein fest verdrahtetes Modell bricht still, sobald OpenAI die Liste verschiebt.

### 9.7 Relevante Env-Variablen
| Variable | Wirkung |
|----------|---------|
| `CODEX_HOME` | Codex-State-Verzeichnis (Default `~/.codex`); für Profil-Isolation pro Profil setzen |
| `HERMES_CODEX_BASE_URL` | überschreibt die Backend-Base-URL |
| `HERMES_CODEX_REFRESH_TIMEOUT_SECONDS` | Timeout des Refresh-Calls (Default 20 s) |

### 9.8 Der komplette Weg als Rezept (zum Nachschlagen)
**a) Direkt-Impersonation (`codex_responses`-Stil) — Minimal-Nachbau:**
1. `codex login` ausführen (echte CLI) → erzeugt `~/.codex/auth.json` mit `tokens.{access_token,refresh_token,account_id}`.
2. Access-Token lesen; ist es <2 Min vor Ablauf, per §9.3 refreshen (`grant_type=refresh_token`, Client-ID `app_EMoam…`, kein Secret) und **gemerged** zurückschreiben (eigener Store, nicht `~/.codex/`).
3. `chatgpt_account_id` aus dem JWT-Payload des Access-Tokens ziehen.
4. Request an `POST https://chatgpt.com/backend-api/codex/...` mit den 4 Headern aus §9.4 + explizitem Modell (§9.6).
5. Fehler nach §9.3 klassifizieren (429 = warten, 401/403/invalid_grant = relogin, refresh_token_reused = `codex` neu).

**b) Codex wrappen (`codex_app_server`) — kein Selbst-Ausgeben nötig:**
1. `npm i -g @openai/codex`, `codex login`.
2. Hermes startet `codex app-server` als Subprozess (JSON-RPC über stdio: `thread/start`, `turn/start`, `item/*`-Notifications).
3. Eigene Tools per MCP-Server in `~/.codex/config.toml` registrieren (`[mcp_servers.<name>]`), damit Codex zurück-callt. Hermes umrahmt nur (Sessions, Slash-Commands, Memory), die LLM-/Tool-Arbeit macht das echte Codex-Binary mit dessen `~/.codex/auth.json`.
4. Vorteil: kein Cloudflare-/originator-Problem (es IST die echte CLI). Nachteil: an die jeweils installierte Codex-Version gekoppelt.

### 9.9 Bruchstellen & ehrliche Risiken (zusammengefasst)
- Jede Änderung von OpenAI an Client-ID, `originator`-Whitelist, Endpoint, JWT-Claim-Pfad oder
  App-Server-Protokoll bricht den Nachbau sofort (vgl. Anthropic 09.01.2026, Almanach C1).
- One-Time-Use-Refresh-Token: parallele Nutzung durch echte Codex-CLI ↔ Nachbau invalidiert Tokens.
- Abo-Quota (429) ist kein Auth-Fehler; nicht in Relogin-Schleifen laufen.
- ToS: Drittsoftware, die sich als offizieller Client maskiert, ist Grauzone bis Verstoss; Ban-/Revocation-Risiko real.
  Stabiler, legaler Weg bleibt: API-Key ODER die echte CLI nutzen (auch per SSH/`--device-auth`).

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/cli-impersonation-subscription-auth.md`) |
|---|---|
| 1 Rechtlicher Rahmen | TL;DR 1/3/5, A1, B2, C1, Fix-Status, Checkliste |
| 2 OAuth/Geraetecode-Flow | A1, A2, B1, C1 (setup-token) |
| 3 Token-Refresh ohne Ueberschreiben | A1, E1, E3 |
| 4 Sichere Token-Ablage | A1, A2, Checkliste (auth.json) |
| 5 Notwendige Header / Client-Identitaet | A1, B1, C1, E1, E2 |
| 6 Endpunkt-Eigenheiten & Effort | A1, B1, D1, E1 |
| 7 Fehler-Handling 401/403/Expiry | C1, E1, E2 |
| 8 Rate-Limit-Fairness & Versions-Drift | B1, B2, E1, E2 |
| 9 Fallstudie Hermes→Codex (Code-Weg) | B1 (Hermes-Trick), A1 (Codex-OAuth), E1/E2 (Bruchstellen/Risiken) |
