# Bekannte Bugs/Risiken: „Drittsoftware nutzt KI-Abo statt API-Key" (CLI-Impersonation)

> PFLICHT-LESEN vor dem Versuch, in eigener Software ein KI-ABO (statt API-Key) zu nutzen, indem man
> sich als die offizielle CLI/App des Anbieters ausgibt. Stand: zuletzt recherchiert am 2026-06-08.
> Verwandt: OAuth-Mechanik siehe `oauth-device-code.md`. Zweite Seite: `best-practices/apis/cli-impersonation-subscription-auth.md`.

> **Grauzone — ehrlich:** Der offizielle Weg (eigene CLI loggt sich per OAuth ins eigene Abo ein) ist
> erlaubt. Das NACHBAUEN als Drittsoftware (gleiche Client-ID/Header/Endpunkte wie die offizielle CLI),
> um fremde Abo-Backends anzuzapfen, bewegt sich zwischen Grauzone und klarem ToS-Verstoss. Anbieter
> haben 2026 aktiv mit Client-Identity-Checks, Token-Revocation und Bans reagiert. Diese Datei
> dokumentiert die Mechanik UND die realen Bruchstellen/Risiken — nicht als Empfehlung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Abo statt API-Key in Drittsoftware geplant | Erst Legalitaet pruefen — oft ToS-Verstoss | TL;DR, Checkliste |
| 2 | ⭐ Claude Pro/Max OAuth in Drittsoftware | Seit 09.01.2026 geblockt + Ban — NICHT bauen | §C1 |
| 3 | "only authorized for use with Claude Code" (403) | Harter Client-Block, kein Retry — auf API-Key | §C1, §E2 |
| 4 | Codex „Sign in with ChatGPT" (offiziell) | Erlaubt; Port 1455, `~/.codex/auth.json` | §A1 |
| 5 | Headless/SSH-Login schlaegt fehl | `codex login --device-auth` (Admin-Freigabe) | §A2 |
| 6 | Ploetzlich 401/403 nach Anbieter-Update | Client-ID/Originator/Endpoint hat sich geaendert | §E1 |
| 7 | `auth.json`/Tokens ablegen | Wie Passwort: nie committen, nie loggen | §A1, Checkliste |
| 8 | Legaler, stabiler Ausweg | API-Key ODER offizielle CLI (auch per SSH) | §E3 |

---

## A) OpenAI Codex CLI — der offizielle Abo-OAuth-Flow (die Referenz)

### A1. Codex CLI „Sign in with ChatGPT" — OAuth statt API-Key
- **Was:** offizielle Codex CLI loggt sich per OAuth in den ChatGPT-Account (Plus/Pro/Business) ein; Nutzung zählt gegen das Abo, nicht gegen API-Billing.
- **Mechanik (verifiziert):** Client-ID `app_EMoamEEZ73f0CkXaXp7hrann`; `https://auth.openai.com/oauth/authorize` + `/oauth/token`; PKCE; lokaler Callback-Server **Port 1455**; `originator` signalisiert den Client (`codex_cli_rs` / `codex_vscode`); Token-Response `id_token`/`access_token`/`refresh_token`/`account_id`; Access-Token ~8–10 Tage, Refresh erneuert; Speicher `~/.codex/auth.json` (Klartext) oder OS-Keyring (`cli_auth_credentials_store: file|keyring|auto`).
- **Risiko/ToS:** offizieller, erlaubter Weg — KEIN Verstoss, solange die offizielle CLI genutzt wird.
- **FIX/Pflege:** `~/.codex/auth.json` wie ein Passwort behandeln (nicht committen); bei Refresh-Problemen `codex login`; Headless/SSH/WSL2 → `codex login --device-auth`.
- **Quelle:** https://developers.openai.com/codex/auth · https://help.openai.com/en/articles/11369540-using-codex-with-your-chatgpt-plan

### A2. Device-Code-Auth braucht Workspace-Admin-Freigabe
- **Symptom:** `codex login --device-auth` schlägt in Headless/SSH/Docker fehl.
- **Ursache:** Device-Code muss explizit aktiviert werden — Personal: ChatGPT Settings → Security; Business: nur durch Workspace-Admin.
- **FIX:** Freigabe setzen/Admin bitten; alternativ Browser-Flow auf einer Maschine + `auth.json` kopieren (Token-Bindung beachten).
- **Quelle:** https://github.com/openai/codex/issues/9253

---

## B) Der „Hermes"-Trick — Drittsoftware gibt sich als Codex aus

### B1. Hermes-Proxy nutzt das ChatGPT-Abo als LLM-Backend
- **Was:** Hermes Agent stellt einen lokalen OpenAI-kompatiblen Endpunkt bereit, der intern den OAuth-Provider nutzt, in dem Hermes eingeloggt ist (ChatGPT Pro/Plus, auch Claude Pro, SuperGrok). Beliebige Tools sprechen „normale" OpenAI-API gegen `localhost`, ohne eigenen API-Key.
- **Mechanik:** „wrappt" die Codex App-Server-Runtime und routet über DENSELBEN Auth-Flow wie die Codex CLI (Client-ID/PKCE/Originator wie A1). Ohne aktives Abo liefert das Codex-OAuth-Backend HTTP 400.
- **Bruchstelle/Bug:** bricht sofort, wenn OpenAI Client-ID, `originator`, Endpunkte oder die Runtime-Schnittstelle ändert — der nachgebaute Client muss EXAKT der offiziellen Codex-Signatur entsprechen.
- **Risiko/ToS (ehrlich):** Grauzone bis Verstoss — Drittsoftware maskiert sich als offizieller Codex-Client. Ban-Risiko analog Anthropic.
- **FIX/Workaround:** Proxy an die jeweils aktuelle Codex-Version koppeln (dauerhafter Wartungsaufwand).
- **Quelle:** https://hermesagents.net/blog/hermes-proxy-claude-pro-aider-cline-codex · https://hermes-agent.nousresearch.com/docs/user-guide/features/codex-app-server-runtime
- **Code-belegte Details (Repo `NousResearch/hermes-agent`, analysiert 2026-06-28 — vollständiger Weg in `best-practices/apis/cli-impersonation-subscription-auth.md` §9):**
  - **Cloudflare-403 ohne `originator` ⭐:** Vor `https://chatgpt.com/backend-api/codex` sitzt ein Cloudflare-Layer, der NUR first-party Originatoren (`codex_cli_rs`, `codex_vscode`, `codex_sdk_ts`, Prefix `Codex`) durchlässt. Von Nicht-Residential-IP (VPS/Server) ohne erlaubten `originator` → **403 mit `cf-mitigated: challenge`, egal ob die Auth korrekt ist**. Hermes pinnt daher `originator: codex_cli_rs` + codex-förmigen `User-Agent`. Symptom „403 trotz gültigem Token" → originator/User-Agent prüfen.
  - **`refresh_token_reused` (One-Time-Use-Rotation):** OpenAI rotiert den Refresh-Token bei jedem Refresh. Refreshen Nachbau UND echte Codex-CLI/VS-Code denselben Token, invalidiert der eine den anderen → Fehler `refresh_token_reused` → `codex` neu ausführen. FIX: getrennte Token-Stores (Hermes `~/.hermes/auth.json` ≠ `~/.codex/auth.json`), nie zurückschreiben.
  - **`ChatGPT-Account-ID` muss aus dem JWT:** Der Header-Wert kommt aus dem Access-Token selbst (JWT-Payload-Claim `["https://api.openai.com/auth"]["chatgpt_account_id"]`), nicht aus separater Config. Fehlt er → 401.
  - **Endpoint ist das ChatGPT-Backend, nicht die API:** `https://chatgpt.com/backend-api/codex` (zählt gegen Abo), NICHT `api.openai.com` (API-Billing). Client-ID `app_EMoamEEZ73f0CkXaXp7hrann`, Token-URL `https://auth.openai.com/oauth/token`, public Client (kein `client_secret`).
  - **Modell-Allow-List driftet:** kein hartes Default-Modell pinnen — die akzeptierten Modelle am ChatGPT-Endpoint sind eine undokumentierte, wandernde Liste (in 6 Wochen `gpt-5.3-codex`→`gpt-5.2-codex`→`gpt-5.4`). Modell explizit übergeben, sonst stiller Bruch.
  - **429 ≠ Auth-Fehler:** 429 vom Token-/Inferenz-Endpoint = Abo-Quota erschöpft, Credentials noch gültig → „retry later", nicht relogin-Schleife.

### B2. OpenCode-Plugins — ChatGPT-Abo-OAuth als Plugin
- **Was:** `numman-ali/opencode-openai-codex-auth`, `tumf/opencode-openai-device-auth` bringen den Codex-OAuth-Flow in OpenCode/headless — „same method as OpenAI's official Codex CLI".
- **Mechanik:** dieselbe öffentliche Client-ID + PKCE + Token-Refresh wie A1; 15-Min-Caching gegen Rate-Limits.
- **Risiko/ToS:** Plugin-Doku schränkt selbst ein: „personal development use only. Not for commercial services, API resale, multi-user."
- **Quelle:** https://github.com/numman-ali/opencode-openai-codex-auth

---

## C) Claude Code / Claude per Abo-OAuth — und die Anthropic-Durchsetzung

### C1. Claude Pro/Max OAuth in Drittsoftware — seit 2026 server-seitig geblockt ⭐
- **Was:** Tools loggten sich per OAuth mit Claude-Pro/Max-Abo ein und sendeten Header, die sie als offizielle Claude-Code-CLI ausgaben.
- **Bruchstelle/Lektion:**
  - **09.01.2026:** Anthropic deployte server-seitige Client-Identity-Checks. Fehler: „This credential is only authorized for use with Claude Code and cannot be used for other API requests." → Token gültig, Anfrage abgelehnt, weil nicht vom echten Binary.
  - **Feb 2026:** ToS verschärft — OAuth-Token aus Free/Pro/Max in jeder anderen Software = Verstoss gegen Consumer-ToS.
  - **Token-Revocation** bei Drittnutzung; **Bans** teils binnen 20 Min (einige Fehl-Bans revidiert).
- **Risiko/ToS:** eindeutiger ToS-Verstoss + reales Ban-Risiko. Nicht mehr Grauzone.
- **FIX (legal):** (1) offizielle Claude-Code-CLI (läuft auch remote per SSH); (2) API-Key statt OAuth in Drittsoftware; (3) Anbieter wechseln. „Header weiter nachbauen" ist KEINE dauerhafte Lösung mehr.
- **Quelle:** https://www.theregister.com/2026/02/20/anthropic_clarifies_ban_third_party_claude_access/

---

## D) GitHub Copilot-Token als LLM-Backend

### D1. copilot-api — Copilot-Abo als OpenAI/Anthropic-kompatibler Server
- **Was:** `ericc-ch/copilot-api` verwandelt ein Copilot-Abo in einen lokalen OpenAI-/Anthropic-kompatiblen Server.
- **Mechanik:** authentifiziert mit GitHub-Token (Account mit Copilot-Abo), tauscht ihn gegen kurzlebigen Copilot-Bearer, ruft `api.githubcopilot.com`, sendet Editor-Imitations-Header (`editor-version`, `copilot-integration-id`, `user-agent`). *(Exakte Header-Werte aus Quellen nicht 1:1 bestätigt — Repo-Quelltext prüfen.)*
- **Bruchstelle:** bricht bei Änderung der Token-Exchange-Endpunkte / erwarteten Editor-Header / Integration-ID-Prüfung.
- **Risiko/ToS:** Zweckentfremdung des Copilot-Abos → Grauzone bis Verstoss, GitHub-Account-Sperr-Risiko.
- **Quelle:** https://github.com/ericc-ch/copilot-api

---

## E) Querschnitt: Bruchstellen, Erkennung, Abwehr (gilt für alle Muster)

### E1. Die wiederkehrenden Bruchstellen
- **Client-ID/User-Agent/Originator ändert sich** → Auth bricht (Signatur muss EXAKT stimmen).
- **Endpoint-Rotation** → 401/403 nach Anbieter-Update.
- **Token-Refresh-Eigenheiten** → Access-Token kurzlebig (Codex ~8–10 Tage); Refresh kann widerrufen werden.
- **Abo-Rate-Limits ≠ API-Limits** → bei Codex zählt Nutzung gegen „agentic usage" (geteilt mit anderen ChatGPT-Agents); April 2026 von Message- auf Token-Basis umgestellt.
- **Plötzliche 401/403 nach Update** → häufigstes Symptom (Client-Identity-Check oder Endpoint-Wechsel).

### E2. Erkennung/Abwehr der Anbieter
- **Server-seitige Client-Identity-Verifikation** (Anthropic 09.01.2026): prüft, ob die Anfrage wirklich vom offiziellen Binary stammt — Header-Spoofing reicht nicht.
- **Token-Scope-Bindung** („only authorized for use with Claude Code").
- **Automatisierte Ban-Heuristik** auf ungewöhnliche Nutzungsmuster.
- **Refresh-Token-Revocation** als gezielte Gegenmaßnahme.

### E3. Funktionserhaltender, legaler Ausweg
- **API-Key statt Abo-OAuth in Drittsoftware** (stabile Abrechnung, kein Ban-Risiko) ODER ausschließlich die offizielle CLI verwenden.

---

## Fix-Status (Stand 2026-06-08)

| Muster | Status |
|---|---|
| Codex CLI Abo-OAuth (offiziell) | **erlaubt** — kein Verstoss bei offizieller CLI |
| Hermes/OpenCode-Codex-Nachbau | **funktioniert** (Grauzone), bricht bei jeder Codex-Signatur-Änderung |
| Claude Pro/Max OAuth in Drittsoftware | **server-seitig geblockt** seit 09.01.2026 + ToS-Verstoss + Ban-Risiko |
| Copilot-Token als Backend | **funktioniert** (Grauzone), Account-Sperr-Risiko |

**Ehrlichkeits-Hinweis:** Codex-OAuth-Werte (Client-ID, Port 1455, `~/.codex/auth.json`) + Anthropic-Durchsetzungs-Chronologie (09.01./Feb 2026) sind mehrfach belegt. NICHT final: exakte Copilot-Imitations-Header; Anthropic-Spoofing-Interna werden in den Quellen bewusst nicht offengelegt.

---

## Pflicht-Checkliste (vor so einem Versuch)

- [ ] Geht es überhaupt legal? (Offizielle CLI / API-Key sind risikofrei — Drittsoftware-Abo-OAuth ist es bei Anthropic NICHT mehr.)
- [ ] Bewusst, dass jede Client-ID/Originator/Endpoint-Änderung des Anbieters den Login bricht?
- [ ] Bewusst, dass Anthropic server-seitig blockt + bannt (kein Header-Spoofing-Workaround mehr)?
- [ ] Tokens (`auth.json`) sicher gespeichert, nicht committet?
- [ ] Im Zweifel: API-Key oder offizielle CLI (auch remote per SSH) statt Abo-Nachbau.

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/apis/cli-impersonation-subscription-auth.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [anthropic-api](anthropic-api.md)
- [api-integration-general](api-integration-general.md)
- [deepseek-api](deepseek-api.md)
- [google-gemini-api](google-gemini-api.md)
- [groq-api](groq-api.md)
- [local-openai-compatible](local-openai-compatible.md)
- [mistral-api](mistral-api.md)
- [oauth-device-code](oauth-device-code.md)
- [openai-api](openai-api.md)
- [openrouter-api](openrouter-api.md)
- [other-llm-apis](other-llm-apis.md)
- [xai-grok-api](xai-grok-api.md)
