# CLI-Impersonation / Subscription-Auth — Best Practices (Stand 2026-06-09)

> Gegenstueck zu `bugs/apis/cli-impersonation-subscription-auth.md`. (Researcher-Recherche 2026-06-09.)
> Hinweis: ToS/Rechtslage je Anbieter beachten — dies ist technische Doku, keine Rechtsberatung.

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
