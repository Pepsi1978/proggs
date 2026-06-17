# Bekannte Bugs & Fallen: OpenRouter (Custom-API-Provider) in der Claude Code CLI

> **PFLICHT-LESEN vor JEDER Arbeit am OpenRouter-/Custom-Provider-Setup der Claude-Code-CLI:**
> `~/.claude/settings.json` (`env`-Block) bzw. `settings.local.json`, die Provider-
> Umgebungsvariablen (`ANTHROPIC_BASE_URL`, `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_API_KEY`,
> `ANTHROPIC_MODEL`, `ANTHROPIC_DEFAULT_*_MODEL`, `CLAUDE_CODE_SUBAGENT_MODEL`), sowie ggf.
> die claude-code-router-Config (`~/.claude-code-router/config.json`).
>
> **Abgrenzung (NICHT hier — eigene Almanache):** Allgemeine Settings/Permissions/Rules →
> `claude-config.md` · MCP-Server-Bau → `mcp-server.md` · Hooks → `claude-hooks.md` ·
> Python-Scripting/Encoding → `python-windows.md`. Das **UTF-8-BOM-Problem** der settings.json
> ist in `claude-config.md §3.2` dokumentiert (hier nur kurz referenziert).
>
> **Stand:** recherchiert am **2026-06-17** fuer **Claude Code 2.1.177** (Windows/MINGW64 +
> macOS) und **OpenRouter „Anthropic Skin"** (`https://openrouter.ai/api`).
> **Anker:** claude-code=2.1.177  <!-- maschinenlesbar fuer check-version-anchor.py -->
> Versions-Fixes sind **changelog-belegt** (offizielles `anthropics/claude-code` CHANGELOG).
> Issue-OPEN/CLOSED nur dort hart, wo per direktem Seiten-Fetch verifiziert — sonst „Status unklar".
> **Cowork-Einschraenkung dieser Recherche:** `gh` war im Cowork-Sandbox nicht verfuegbar und die
> GitHub-REST-API lieferte ueber WebFetch leere Bodies → kein maschineller `gh`-Statuscheck moeglich
> (Ehrlichkeits-Hinweis am Ende). Loesungen sind funktionserhaltend (nie „Feature weglassen").

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter Arbeit
> hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der Schnell-
> Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Base-URL setzen | `https://openrouter.ai/api` — **OHNE `/v1`**, ohne trailing slash | §2.1 |
| 2 | „model not found" trotz korrektem Key | `ANTHROPIC_API_KEY=""` **explizit leeren** (nicht nur unset) | §3.1 |
| 3 | „model may not exist" mitten in Session | Background-Modell vergessen → alle 3 `ANTHROPIC_DEFAULT_*_MODEL` setzen | §4.1 |
| 4 | 400 beim Start mit MCP/Tool-Search | `tool_reference`-Bug → gefixt ab 2.1.70; sonst `ENABLE_TOOL_SEARCH` aus | §6.1 |
| 5 | 400 „context-management/prompt-caching-scope" Beta | `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1` | §6.2 |
| 6 | Kosten explodieren in langen Sessions | Prompt-Cache geht ueber OpenAI-Wire verloren → nativer Anthropic-Endpoint | §6.4 |
| 7 | Abbruch nach 5 Min bei langsamem Modell | `API_TIMEOUT_MS` hoch (gefixt 2.1.106) + `API_FORCE_IDLE_TIMEOUT=0` (2.1.169) | §7 |
| 8 | settings.json wird ignoriert (Windows) | UTF-8-**BOM** bricht Parse → BOM-frei speichern (`claude-config.md §3.2`) | §10.3 |
| 9 | OpenRouter-Slug nicht im `/model`-Picker | `ANTHROPIC_CUSTOM_MODEL_OPTION="<slug>"` setzen | §5.2 |
| 10 | Modellwechsel zur Laufzeit wirkt nicht | Env-Var greift erst nach Neustart; live nur per `/model` | §5.3 |
| 11 | Subagent laeuft auf falschem Modell | `CLAUDE_CODE_SUBAGENT_MODEL="<slug>"` setzen | §5.4 |
| 12 | Auto-Routing pro Aufgabe gewuenscht | Geht NUR mit Proxy (claude-code-router) — Bordmittel nur grob | §11 |
| 13 | Git Bash macht aus `/api` einen Pfad | `MSYS_NO_PATHCONV=1` ODER Vars als Windows-User-Env setzen | §10.7 |
| 14 | Zurueck zu Anthropic, aber Calls falsch | `/logout` reicht NICHT — `ANTHROPIC_BASE_URL` wirklich entfernen | §10.4 |
| 15 | `/fast` blockiert ueber OpenRouter | `CLAUDE_CODE_SKIP_FAST_MODE_ORG_CHECK=1` (ab 2.1.96), nur Opus 4.6+ | §5.7 |

---

## TL;DR — kopierbereites Minimal-Setup fuer Frank (OpenRouter, ohne Proxy)

Setzen in `~/.claude/settings.json` (`env`-Block, **UTF-8 ohne BOM**) ODER persistent als Windows-User-Env:

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "https://openrouter.ai/api",
    "ANTHROPIC_AUTH_TOKEN": "sk-or-v1-DEIN-KEY",
    "ANTHROPIC_API_KEY": "",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "~anthropic/claude-opus-latest",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "~anthropic/claude-sonnet-latest",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "~anthropic/claude-haiku-latest",
    "CLAUDE_CODE_SUBAGENT_MODEL": "~anthropic/claude-opus-latest",
    "CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS": "1",
    "CLAUDE_CODE_ATTRIBUTION_HEADER": "0",
    "API_TIMEOUT_MS": "1200000",
    "API_FORCE_IDLE_TIMEOUT": "0"
  }
}
```

Danach: bei vorherigem Anthropic-Login einmal `/logout`, Terminal neu starten, mit **`/status`** verifizieren
(muss `Base URL: https://openrouter.ai/api` + Auth-Quelle `ANTHROPIC_AUTH_TOKEN` zeigen). Die 5 wichtigsten
Regeln: (1) Base-URL ohne `/v1`, (2) `ANTHROPIC_API_KEY` explizit leer, (3) alle drei Default-Modelle setzen
(sonst 404 im Hintergrund), (4) `DISABLE_EXPERIMENTAL_BETAS=1` gegen 400er, (5) Prompt-Caching nur ueber den
nativen Anthropic-Endpoint — sonst teuer.

---

## 1. Architektur-Grundlagen (Voraussetzung — kein Bug, aber Wurzel fast aller Fallen)

Claude Code spricht intern **ausschliesslich** die **Anthropic-Messages-API** (`/v1/messages` +
`/v1/messages/count_tokens`), alternativ Bedrock/Vertex/Foundry. OpenRouters **Standard-Endpoint ist
OpenAI-Format** und funktioniert mit der CLI **NICHT** direkt. OpenRouter bietet aber eine
**„Anthropic Skin"** (`POST https://openrouter.ai/api/v1/messages`), die genau das Anthropic-Protokoll
spricht — **das** ist der Weg ohne Proxy. Ein Gateway MUSS `anthropic-beta`- und `anthropic-version`-Header
weiterleiten; fehlt das, drohen reduzierte Funktion oder 400er.

`ANTHROPIC_BASE_URL` aendert **nur, WOHIN** die Requests gehen — **nicht, WELCHES Modell** antwortet. Das
Modell wird getrennt ueber `/model`, `--model`, `ANTHROPIC_MODEL` oder die `ANTHROPIC_DEFAULT_*_MODEL`-Vars
gesetzt. Quelle: code.claude.com/docs/en/llm-gateway · /en/model-config.

---

## 2. Verbindung & Base-URL

### 2.1 ⭐ HAEUFIG — Falsche Base-URL mit `/v1` → 404 / „model not found"
**Symptom:** 404, „model not found" oder Endpoint unerreichbar, obwohl der Key korrekt ist — auch mid-session.
**Ursache:** Es wurde die **OpenAI**-kompatible URL `https://openrouter.ai/api/v1` gesetzt. Claude Code haengt
`/v1/messages` **selbst** an → `…/api/v1/v1/messages` (Doppel-`/v1`), falsches Routing.
**Versionen:** versionsunabhaengig (Konfig-Falle).
**FIX:** `ANTHROPIC_BASE_URL="https://openrouter.ai/api"` — exakt so, **ohne `/v1`**, **ohne** trailing slash.
Mit `/status` pruefen (muss `Anthropic base URL: https://openrouter.ai/api` zeigen). **Achtung Sonderfall:** Wer
**claude-code-router** nutzt, setzt dort den vollen `…/api/v1/chat/completions`-Pfad (CCR spricht OpenAI-Format) —
die `/v1`-Regel gilt nur fuer den **direkten** CLI-Weg.
**Quelle:** openrouter.ai/docs/cookbook/coding-agents/claude-code-integration · mykolaaleksandrov.dev (2026-05).

### 2.2 `count_tokens`-Endpoint von OpenRouter (Status unklar)
**Symptom/Sachverhalt:** Claude Code ruft regelmaessig `/v1/messages/count_tokens` (Token-Anzeige, Auto-Compact).
Im Anthropic-Modus MUSS der Gateway diesen Endpoint exponieren.
**Ursache:** Die OpenRouter-API-Referenz listet in „Anthropic Messages" nur `create-messages` — **kein**
dokumentierter `count_tokens`. Ob die CLI das Fehlen sauber abfaengt (Heuristik-Fallback) oder Fehler wirft, ist
aus OpenRouter-Quellen **nicht eindeutig belegt** (verwandte dokumentierte Faelle stammen von LiteLLM/Ollama).
**Versionen:** per Design OpenRouter; kein CLI-Fix.
**FIX/Workaround:** Wenn `count_tokens`-bezogene Fehler auftreten, alle drei Default-Modelle sauber setzen (§4.1),
damit die CLI nicht in unerwartete Pfade laeuft. Kernfunktion (Messages) bleibt unberuehrt.
**Quelle:** openrouter.ai/docs/api/api-reference/anthropic-messages/create-messages · code.claude.com/docs/en/llm-gateway.

---

## 3. Authentifizierung

### 3.1 ⭐ HAEUFIG — `ANTHROPIC_API_KEY` muss EXPLIZIT leer sein (nicht nur unset)
**Symptom:** Auth-Fehler, „Both a token (ANTHROPIC_AUTH_TOKEN) and an API key (ANTHROPIC_API_KEY) are set", oder
Claude Code faellt still auf direkten Anthropic-Connect zurueck statt OpenRouter.
**Ursache:** Ein noch in der Shell/Registry gesetzter realer `ANTHROPIC_API_KEY` (auch ein **alter** aus
frueherem Setup) kollidiert mit `ANTHROPIC_AUTH_TOKEN`. Bloss „nicht setzen" reicht nicht, wenn die Variable
global existiert.
**Versionen:** versionsunabhaengig.
**FIX:** `ANTHROPIC_API_KEY=""` als **leeren String** setzen (nicht nur unset/null), OpenRouter-Key in
`ANTHROPIC_AUTH_TOKEN`. Beim Zurueck zur Anthropic-Subscription die Variable ganz entfernen.
**Quelle:** openrouter.ai/docs/cookbook/coding-agents/claude-code-integration (Step 2) · dan1t0.com (2026-01).

### 3.2 Offizielle Auth-Praezedenz (gegen Verwirrung „welche Variable gewinnt?")
**Fakten (code.claude.com/docs/en/authentication):** Bei mehreren gesetzten Credentials waehlt Claude Code in
dieser Reihenfolge: (1) Cloud-Provider (`CLAUDE_CODE_USE_BEDROCK/_VERTEX/_FOUNDRY`) → (2) **`ANTHROPIC_AUTH_TOKEN`**
(Header `Authorization: Bearer`) ← das nutzt OpenRouter → (3) `ANTHROPIC_API_KEY` (Header `x-api-key`) →
(4) `apiKeyHelper`-Script → (5) `CLAUDE_CODE_OAUTH_TOKEN` → (6) Subscription-OAuth aus `/login`.
**Wichtig:** `ANTHROPIC_AUTH_TOKEN` (Rang 2) schlaegt `ANTHROPIC_API_KEY` (Rang 3) — die kursierende Foren-Aussage
„API-Key gewinnt immer" ist **falsch** fuer die reine Env-Var-Auswahl. Diese Auth-Achse ist getrennt von der
allgemeinen Settings-Praezedenz (§10.6).
**Quelle:** code.claude.com/docs/en/authentication.

### 3.3 Gecachte Anthropic-OAuth-Session blockiert OpenRouter-Modelle
**Symptom:** „model-not-found" speziell fuer OpenRouter-eigene Modelle (`openrouter/auto` u.a.) + Auth-Conflict-
Warnung beim Start.
**Ursache:** Eine aeltere gecachte Anthropic-OAuth-Anmeldung (vor dem Umstieg) liegt noch vor und kollidiert mit
`ANTHROPIC_AUTH_TOKEN`.
**Versionen:** aktuell (2026).
**FIX:** Einmal `/logout` in Claude Code, dann `claude` beenden + neu starten. **Hilft NUR** gegen die gecachte
OAuth-Session — **nicht** gegen einen real im Profil gesetzten `ANTHROPIC_API_KEY` (das ist §3.1).
**Quelle:** openrouter.ai/docs/cookbook/coding-agents/claude-code-integration (Step 3) · rushis.com.

### 3.4 `apiKeyHelper` erbt `ANTHROPIC_BASE_URL` aus settings.json NICHT (gemeldeter Bug)
**Symptom:** Wer Token-Rotation per `apiKeyHelper` macht und `ANTHROPIC_BASE_URL` nur in settings.json setzt: das
Helper-Script laeuft in einer Umgebung OHNE diese Variable → testet evtl. gegen den falschen Endpoint.
**Ursache:** Die Ausfuehrungsumgebung des Key-Helpers enthaelt den `ANTHROPIC_BASE_URL`-Wert aus settings.json nicht
(relevant auch bei `managed-settings.json`). Praezedenz: `apiKeyHelper` ist **niedriger** als `ANTHROPIC_AUTH_TOKEN`/
`ANTHROPIC_API_KEY` — sind die gesetzt, greift der Helper gar nicht.
**Versionen:** offen gemeldet (#26999, Feature-Request #29146), Fix-Status unklar.
**FIX:** Base-URL dem Helper explizit als eigene Prozess-Env-Var mitgeben oder hartkodieren — nicht auf Vererbung
aus settings.json verlassen. Refresh-Intervall via `CLAUDE_CODE_API_KEY_HELPER_TTL_MS`.
**Quelle:** github.com/anthropics/claude-code/issues/26999 · /issues/29146 · code.claude.com/docs/en/llm-gateway.

### 3.5 `Authorization`-Header-Kollision mit `ANTHROPIC_CUSTOM_HEADERS`
**Symptom:** 403/401, wenn gleichzeitig `ANTHROPIC_AUTH_TOKEN` **und** ein eigener `Authorization`-Header via
`ANTHROPIC_CUSTOM_HEADERS` (oder `apiKeyHelper`) gesetzt sind. Beim Bedrock-Pfad fuehrte das zu SigV4-403.
**Ursache:** Zwei Quellen schreiben denselben Header.
**Versionen:** Bedrock-403-Kollision gefixt ab **2.1.106**; das generelle Muster bleibt eine Konfig-Falle.
**FIX:** NICHT gleichzeitig `ANTHROPIC_AUTH_TOKEN` und einen eigenen `Authorization`-Header setzen. Fuer OpenRouter
genuegt `ANTHROPIC_AUTH_TOKEN`.
**Quelle:** CHANGELOG 2.1.106 · code.claude.com/docs/en/env-vars (`ANTHROPIC_CUSTOM_HEADERS`).

---

## 4. Modelle & die drei Tiers — die zentrale 404-Falle

### 4.1 ⭐ HAEUFIG — Background/Haiku-Modell vergessen → „model may not exist" / 404 mitten in der Session
**Symptom:** Es laeuft scheinbar, dann ploetzlich (oft beim ersten Hintergrund-Task: Terminal-Titel, Konversations-
Zusammenfassung, Git-Diff-Summary): `There's an issue with the selected model (...). It may not exist or you may not
have access to it.` bzw. im Log `model 'claude-haiku-4-5-...' not found` / HTTP-404. Das Hauptmodell funktioniert,
nur Background-Requests scheitern. Bei bezahlten Providern entstehen zusaetzlich Kosten/Fehler.
**Ursache:** Claude Code mappt Tasks auf **drei Tiers** — **Haiku** (Hintergrund: Titel, Summaries, Auto-Compaction),
**Sonnet** (Standard-Coding), **Opus** (komplexes Reasoning). `ANTHROPIC_MODEL`/`--model` setzt nur das Hauptmodell;
fuer Background faellt die CLI auf eingebaute Anthropic-Default-IDs zurueck, die ein OpenRouter-only-Setup nicht
aufloest → 404, von der CLI als „Modell existiert nicht" fehlinterpretiert.
**Versionen:** strukturell alle 2.1.x bei Custom-Providern.
**FIX:** Alle Tier-Variablen explizit auf gueltige OpenRouter-Slugs setzen — vor allem das **Background**-Modell:
```bash
export ANTHROPIC_DEFAULT_HAIKU_MODEL="~anthropic/claude-haiku-latest"    # Background/Titel/Summaries
export ANTHROPIC_DEFAULT_SONNET_MODEL="~anthropic/claude-sonnet-latest"
export ANTHROPIC_DEFAULT_OPUS_MODEL="~anthropic/claude-opus-latest"
```
Man darf fuer alle Tiers dasselbe Modell setzen. **`ANTHROPIC_SMALL_FAST_MODEL` ist deprecated** → durch
`ANTHROPIC_DEFAULT_HAIKU_MODEL` ersetzt (wer noch der alten Var folgt, vergisst evtl. das Background-Modell).
**Quelle:** rushis.com (Ollama, gleiche Klasse) · openrouter.ai/docs/.../claude-code-integration · code.claude.com/docs/en/model-config.

### 4.2 Background-Side-Query schickt nicht verfuegbares Haiku (Teil-Fix ab 2.1.141)
**Symptom (historisch):** z.B. `API Error: 400 ... 'claude-3-5-haiku-20241022' does not support thinking` — Sonnet
wurde im Hintergrund still auf ein altes Haiku umgeschaltet; auf Gateway/Bedrock/Vertex/Foundry schickten Background-
Side-Queries eine Haiku-ID, die der Provider nicht kennt.
**Ursache:** Background-Tasks nutzten ein fest verdrahtetes Haiku-Tier ohne Verfuegbarkeits-Check.
**Versionen:** sichtbar ab v1.0.110; **Teil-Fix in 2.1.141** (Background-Side-Queries fallen jetzt auf das Main-Loop-
Modell zurueck, wenn auf Bedrock/Vertex/Foundry/Gateway kein `ANTHROPIC_SMALL_FAST_MODEL`-Override gesetzt ist).
**FIX:** Trotz Fallback `ANTHROPIC_DEFAULT_HAIKU_MODEL` explizit setzen (§4.1) statt sich auf den Fallback zu verlassen.
**Quelle:** github.com/anthropics/claude-code/issues/7409 · Release v2.1.141.

### 4.3 Slug-Format-Fallen: `provider/model`, `~latest`-Prefix, `:free`-Suffix
**Symptom:** 402 (Insufficient credits) oder 404, obwohl der Name „stimmt"; oder es laeuft eine veraltete Modellversion.
**Ursache/FIX (funktionserhaltend):**
- OpenRouter-Slugs sind **`provider/model`** (z.B. `anthropic/claude-opus-4-8`, `deepseek/deepseek-chat`), **nicht**
  der nackte Anthropic-Stil `claude-opus-4-8`.
- **`~`-Prefix** (z.B. `~anthropic/claude-opus-latest`) → OpenRouter loest automatisch auf die **neueste** Version
  der Familie auf (gegen veraltete Pins). OpenRouters eigene Doku nutzt genau diese `~…-latest`-Form.
- **`:free`-Suffix** kennzeichnet kostenlose/rate-limitierte Varianten; ohne Suffix wird ggf. das bezahlte Modell
  adressiert. (Eine Einzelquelle nannte `:fre` — vermutlich Tippfehler; Standard ist `:free`. Vor Uebernahme pruefen.)
- Exakte Slugs von openrouter.ai/models kopieren; bei ploetzlichem „model not found" zuerst die aktuelle Slug verifizieren.
**Versionen:** provider-seitig (OpenRouter), versionsunabhaengig.
**Quelle:** mykolaaleksandrov.dev · openrouter.ai/docs/.../claude-code-integration.

### 4.4 Nicht-Anthropic-Modelle: Feature-Inkompatibilitaeten (Tool-Use, Thinking, Fast)
**Symptom:** Tool-Use/Function-Calling unzuverlaessig, `... does not support thinking`, fehlende Reasoning-Ausgabe,
abweichende Kontextfenster, wenn man auf `openai/...`, `google/...`, `meta-llama/...`, `deepseek/...`, `qwen/...` routet.
**Ursache:** Claude Code ist auf Anthropic-Modelle optimiert („may not work correctly with other providers"). Extended
Thinking + Fast Mode sind Anthropic-spezifisch.
**Versionen:** 2.1.x.
**FIX (funktionserhaltend):** OpenRouter empfiehlt **Anthropic First-Party (1P) als Top-Priority-Provider** bei
Claude-Code-Nutzung. Fuer andere Modelle: tool-faehige waehlen (DeepSeek, Qwen-Coder gelten als brauchbar); ggf.
`:exacto`-Variante fuer zuverlaessigere Tool-Calls; Feature-Erkennung per
`ANTHROPIC_DEFAULT_*_MODEL_SUPPORTED_CAPABILITIES="effort,thinking,adaptive_thinking,interleaved_thinking"` deklarieren
(sonst deaktiviert die CLI Features per ID-Pattern-Matching bei fremden IDs); Thinking gezielt aus mit `MAX_THINKING_TOKENS=0`;
bei brechendem Tool-Streaming `CLAUDE_CODE_ENABLE_FINE_GRAINED_TOOL_STREAMING=0`.
**Quelle:** mykolaaleksandrov.dev · code.claude.com/docs/en/model-config · openrouter.ai/docs/.../claude-code-integration.

### 4.5 Effort-Parameter wird vom Custom-Modell abgelehnt (400) — gefixt 2.1.70
**Symptom:** `API Error: 400 This model does not support the effort parameter` bei Custom-IDs, die nicht dem Claude-
Naming entsprechen.
**Ursache:** Claude Code erkennt Effort/Thinking ueber ID-Pattern-Matching; Custom-IDs matchen nicht.
**Versionen:** bis 2.1.69; **gefixt ab 2.1.70** (sendet Effort nicht mehr bei nicht-erkannten IDs).
**FIX:** Auf 2.1.177 erledigt. Effort bewusst erzwingen (wenn das Modell es kann): `CLAUDE_CODE_ALWAYS_ENABLE_EFFORT=1`
(ab 2.1.84) bzw. granular per `ANTHROPIC_DEFAULT_*_MODEL_SUPPORTED_CAPABILITIES`.
**Quelle:** CHANGELOG 2.1.70 / 2.1.84 · code.claude.com/docs/en/env-vars.

### 4.6 `availableModels`-Allowlist verhindert Alias-Umbiegen (ab 2.1.176)
**Symptom:** Ein gesetzter `ANTHROPIC_DEFAULT_*_MODEL`-Alias wird still ignoriert.
**Ursache:** Ab **v2.1.176** kann die Alias-Aufloesung keinen erlaubten Alias auf ein Modell **ausserhalb** von
`availableModels` umbiegen — bei gesetzter Allowlist schlaegt das Mapping still fehl.
**Versionen:** ab 2.1.176 (also auf 2.1.177 aktiv).
**FIX:** Ziel-Slugs in die `availableModels`-Allowlist aufnehmen, falls man eine solche pflegt; sonst keine restriktive
Allowlist setzen, wenn man Custom-Slugs mappt.
**Quelle:** code.claude.com/docs/en/model-config.

---

## 5. Modellwechsel & mehrere Modelle nutzen (Franks Ziel: „mehrere Modelle wechseln")

### 5.1 Mehrere Modelle parallel = mehrere Sessions, nicht `/model`-Hopping
**Fakt:** `--model` und `ANTHROPIC_MODEL` gelten **nur fuer die jeweils gestartete Session**. Wer mehrere Modelle
gleichzeitig laufen lassen will, startet **je Terminal** ein eigenes `claude --model <slug>` — nicht per `/model`
zwischen ihnen umschalten.
**Quelle:** code.claude.com/docs/en/model-config.

### 5.2 OpenRouter-Slug erscheint nicht im `/model`-Picker
**Symptom:** Der `/model`-Picker zeigt nur Anthropic-Aliase (Opus/Sonnet/Haiku/Default/Fable); ein OpenRouter-Slug
wie `deepseek/deepseek-chat` ist nicht waehlbar.
**Ursache:** Der Picker zeigt nur Built-in-Aliase; Gateway-Model-Discovery (`/v1/models`) fuegt nur Modelle mit
ID-Praefix `claude`/`anthropic` hinzu — reine Fremdmodelle werden gefiltert.
**Versionen:** 2.1.x.
**FIX (drei Wege):** (1) `/model <slug>` direkt eintippen (wird wie voller Modellname behandelt). (2) Eigenen Picker-
Eintrag haengen:
```bash
export ANTHROPIC_CUSTOM_MODEL_OPTION="anthropic/claude-3.5-sonnet"
export ANTHROPIC_CUSTOM_MODEL_OPTION_NAME="Sonnet via OpenRouter"        # optional (ab 2.1.73)
export ANTHROPIC_CUSTOM_MODEL_OPTION_DESCRIPTION="Routed through OpenRouter"
```
(Validierung wird fuer diese ID uebersprungen — jeder vom Endpoint akzeptierte String ist erlaubt.) (3) Gateway-
Discovery `CLAUDE_CODE_ENABLE_GATEWAY_MODEL_DISCOVERY=1` (ab **2.1.129**; nur Anthropic-Messages-Format, nur
`claude`/`anthropic`-Praefix; Cache `~/.claude/cache/gateway-models.json`). Ob OpenRouters `/api` den Discovery-Pfad
bedient, ist nicht bestaetigt — Weg 1/2 ist sicher.
**Quelle:** code.claude.com/docs/en/model-config · /en/llm-gateway · CHANGELOG 2.1.73 / 2.1.129.

### 5.3 Modellwechsel ohne Neustart: Env-Var greift NICHT live, `/model` schon
**Symptom:** Geaenderte `ANTHROPIC_MODEL`/`ANTHROPIC_DEFAULT_*`-Env-Var wirkt waehrend laufender Session nicht.
**Ursache:** Env-Vars werden beim Prozessstart gelesen.
**Versionen:** Verhalten generell; Speicher-Cutover bei **v2.1.153**.
**FIX:** Laufzeit-Wechsel per `/model <slug>`. Persistente Defaults per Env-Var/settings.json + **Neustart**.
Praezedenz (hoechste zuerst): `/model` (Session) → `--model` (Start) → `ANTHROPIC_MODEL` → `model`-Feld in Settings.
**Seit 2.1.153** speichert `/model` die Wahl als Default fuer neue Sessions (`Enter` = wechseln+speichern, `s` = nur
diese Session); in **2.1.144–2.1.152** galt `/model` nur fuer die laufende Session und `d` speicherte den Default —
Verhaltensaenderung mitten in der 2.1-Reihe, leicht zu verwechseln.
**Quelle:** code.claude.com/docs/en/model-config.

### 5.4 Subagenten/Background laufen auf anderem Modell als gesetzt
**Symptom:** Hauptmodell via OpenRouter gesetzt, aber Subagent-/Agent-Team-Aufrufe scheitern oder laufen auf einer
eingebauten Anthropic-Default-ID, die OpenRouter nicht kennt.
**Ursache:** Subagenten loesen ihr Modell ueber eine eigene Kette: per-Invocation-`model` → `model`-Frontmatter →
Default-Resolution.
**Versionen:** 2.1.x.
**FIX:** `CLAUDE_CODE_SUBAGENT_MODEL="<gueltiger-slug>"` setzen — **ueberschreibt** per-Invocation-`model` und
Frontmatter fuer ALLE Subagenten/Agent-Teams (`inherit` = normale Resolution). Zusaetzlich `ANTHROPIC_DEFAULT_HAIKU_MODEL`
setzen (§4.1).
**Quelle:** code.claude.com/docs/en/model-config · openrouter.ai/docs/.../claude-code-integration.

### 5.5 `/model` zeigt Custom-Modell, aber Folge-Requests fallen still auf Anthropic-ID (offen)
**Symptom:** Ueber `/model` ist ein Custom-Modell gewaehlt; bei Tool-/Folge-/Retry-/Background-Requests traegt ein Teil
**nicht** den Slug, sondern eine `claude-…`-ID → bei OpenRouter 404 oder Fehlrouting.
**Ursache:** Bestimmte interne Requests erben den gewaehlten Custom-Slug nicht (eng verwandt mit §4.1, hier aber TROTZ
korrekter `/model`-Wahl).
**Versionen:** gemeldet ab v2.1.6, **Status offen** (#18025, per Snippet — hart nicht final verifizierbar in Cowork).
**FIX (Workaround):** Defense in Depth — nicht nur `/model`, sondern alle `ANTHROPIC_DEFAULT_*_MODEL` +
`CLAUDE_CODE_SUBAGENT_MODEL` auf gueltige Slugs pinnen, damit auch „durchrutschende" Requests auf einem existierenden
Modell landen. Vollstaendiger Fix liegt bei Anthropic.
**Quelle:** github.com/anthropics/claude-code/issues/18025.

### 5.6 Resume-Sessions ignorieren das aktuelle Modell-Setting (per Design)
**Symptom:** `--resume`/`--continue`/`/resume` laeuft auf dem alten (evtl. nicht mehr existierenden) Slug weiter.
**Ursache:** Bewusst: resumte Sessions behalten das beim Speichern aktive Modell, unabhaengig vom aktuellen Setting.
**FIX:** Nach dem Resume bei Bedarf per `/model <slug>` explizit umsetzen.
**Quelle:** code.claude.com/docs/en/model-config.

### 5.7 `/fast`-Mode bricht ueber OpenRouter ohne Skip-Flag
**Symptom:** `/fast` toggelt nicht / Org-Pruefung blockiert.
**Ursache:** `/fast` sendet `speed:"fast"` (+ Opus). Fast-Mode gibt es nur auf **Opus 4.6/4.7/4.8** und nur ueber den
Anthropic-1P-Provider; die Org-Pruefung scheitert bei OpenRouter-Routing.
**Versionen:** ab **2.1.96** steuerbar.
**FIX:** `CLAUDE_CODE_SKIP_FAST_MODE_ORG_CHECK=1`. An nicht-unterstuetzte Modelle gesendetes `speed:"fast"` wird still
verworfen (Standard-Speed).
**Quelle:** openrouter.ai/docs/.../claude-code-integration.

---

## 6. Tool-Use, Beta-Header & Prompt-Caching (die kritischsten Fallen)

### 6.1 `tool_reference`-Bloecke (MCP Tool Search) → 400 bei custom BASE_URL — gefixt 2.1.70
**Symptom:** Sofortige `API Error: 400` beim Start mit Gateway, sobald MCP-Tools / Tool Search aktiv sind.
**Ursache:** Bei aktiver MCP Tool Search sendet die CLI `tool_reference`-Bloecke, die nur die echte Anthropic-API
versteht; ein Drittanbieter-Gateway lehnt sie ab.
**Versionen:** bis 2.1.69; **gefixt ab 2.1.70** (Tool Search erkennt Proxy-Endpunkte und deaktiviert `tool_reference`
automatisch); zudem ist MCP Tool Search bei nicht-erstanbieterischem Host **default AUS**. Reaktivierung zuverlaessig ab 2.1.72.
**FIX:** Auf 2.1.177 erledigt. Wenn dein Proxy `tool_reference` korrekt weiterleitet, wieder an mit `ENABLE_TOOL_SEARCH=true`.
**Querverweis:** `best-practices/claude-tooling/mcp.md` (Tool Search deaktiviert bei Custom `ANTHROPIC_BASE_URL`).
**Quelle:** CHANGELOG 2.1.70 / 2.1.72 · code.claude.com/docs/en/env-vars (`ANTHROPIC_BASE_URL`).

### 6.2 ⭐ HAEUFIG — Proxy/Gateway lehnt experimentelle Beta-Header/Felder ab → 400
**Symptom:** 400er auf OpenRouter/Bedrock/Vertex, u.a. `Unexpected value(s) 'prompt-caching-scope-2026-01-05' for the
'anthropic-beta' header`, `No endpoints available that support ... context-management-2025-06-27`, oder Ablehnung wegen
structured-outputs-Feldern im Tool-Schema.
**Ursache:** Claude Code sendet Anthropic-exklusive Beta-Header/Felder (`prompt-caching-scope-*`, `context-management-*`,
structured-outputs), die das Ziel nicht kennt; Proxies reichen sie ungefiltert weiter.
**Versionen:** structured-outputs-Header-Bug bis 2.1.80, **gefixt ab 2.1.81** (Flag unterdrueckt ihn jetzt). Header-
Klasse selbst bleibt providerabhaengig.
**FIX:** `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1` setzen (offiziell in der llm-gateway-Doku empfohlen). Alternativ
Gateway-seitig die nicht unterstuetzten Beta-Werte filtern.
**Quelle:** CHANGELOG 2.1.81 · code.claude.com/docs/en/llm-gateway · github.com/BerriAI/litellm/issues/19984 ·
github.com/anthropics/claude-code-action/issues/1089 · github.com/farion1231/cc-switch/issues/1888.

### 6.3 Schlechte Cache-Hit-Rate durch Attribution-Block hinter dem Gateway
**Symptom:** Hohe Kosten / niedrige Cache-Trefferquote, wenn der Gateway seinen Cache auf den vollen Request-Body legt.
**Ursache:** Die CLI stellt dem System-Prompt einen kurzen Attribution-Block voran (Client-Version + Konversations-
Fingerprint). Die echte Anthropic-API strippt ihn vor dem Caching — ein Custom-Gateway nicht, der wechselnde Block
bricht den Cache-Key.
**FIX:** `CLAUDE_CODE_ATTRIBUTION_HEADER=0` (laesst den Block weg; Anthropic-Caching bleibt unbeeinflusst).
**Quelle:** code.claude.com/docs/en/llm-gateway · /en/env-vars.

### 6.4 ⭐ HAEUFIG — Prompt-Caching geht ueber OpenAI-Wire-Format verloren → Kosten explodieren
**Symptom:** Lange Coding-Sessions deutlich teurer als Anthropic-direkt; auf der OpenRouter-Activity-Seite ist
`cached_tokens` immer `0`. Jeder Turn re-sendet den vollen Kontext zum vollen Preis.
**Ursache:** Anthropic-Caching ehrt `cache_control` nur auf Top-Level-Content-Bloecken im **nativen** Anthropic-Messages-
Format. Geht der Pfad ueber das **OpenAI-kompatible `chat_completions`-Format** (typisch bei BYOK/Router-Setups), werden
die Cache-Marker nicht durchgereicht. Quantifiziert: Cache-Read kostet ~0,1× Input (≈90 % Ersparnis); ohne Caching kann
ein langer Agent-Run drastisch teurer werden.
**Versionen:** strukturell / per Design.
**FIX (funktionserhaltend):** Den **nativen Anthropic-Endpoint** nutzen (direkter `ANTHROPIC_BASE_URL=https://openrouter.ai/api`
statt chat_completions-Mode); bei OpenRouter Caching nur am nativen Anthropic-Provider erwarten. Bedingungen: automatisches
Top-Level-Caching nur bei direktem Anthropic-Provider (Bedrock/Vertex unterstuetzen Top-Level-`cache_control` nicht →
OpenRouter schliesst sie dann aus); explizite Per-Block-Breakpoints **max. 4**; Mindest-Token 4096 (Opus 4.5–4.8/Haiku 4.5)
bzw. 1024 (Sonnet 4/4.5/4.6); TTL 5 Min default, `"ttl":"1h"` teurer (Write 2× statt 1.25×). **Achtung:** manuelles
`provider.order` deaktiviert Sticky-Routing → senkt Cache-Hits. Wer Claude-Qualitaet **und** Caching will, ist mit
Anthropic-direkt oft einfacher/billiger; OpenRouter lohnt fuer Modell-Experimente/Breite.
**Quelle:** openrouter.ai/docs/guides/best-practices/prompt-caching · github.com/microsoft/vscode/issues/312939.

### 6.5 Prompt-Caching gezielt steuern/abschalten (Konfig)
**Hinweis:** Falls OpenRouter-Modelle Anthropic-Caching-Header nicht sauber verarbeiten: `DISABLE_PROMPT_CACHING=1`
(global) oder pro Tier `DISABLE_PROMPT_CACHING_OPUS/SONNET/HAIKU/FABLE=1`. Seit **2.1.108** warnt die CLI beim Start,
wenn Caching aus ist; TTL-Steuerung `ENABLE_PROMPT_CACHING_1H` / `FORCE_PROMPT_CACHING_5M`.
**Quelle:** code.claude.com/docs/en/model-config · CHANGELOG 2.1.108.

### 6.6 `cache_control` auf leerem Text-Block → 400, Session dauerhaft kaputt (offen)
**Symptom:** Nach einem Bild **ohne** Begleittext entsteht ein leerer Text-Block mit `cache_control`; die API/ein strikter
Gateway antwortet `400 cache_control cannot be set for empty text blocks`. Danach scheitert **jeder** Folge-Turn → Session
unbrauchbar.
**Ursache:** Der leere Block bleibt in der History und wird bei jedem Request neu mitgeschickt.
**Versionen:** **offen** (#52689, per direktem Seiten-Fetch als OFFEN verifiziert).
**FIX/Workaround:** Beim Anhaengen eines Bildes IMMER mindestens ein Zeichen Begleittext tippen. Recovery nur durch
Editieren einer Nachricht VOR dem fehlerhaften Block.
**Quelle:** github.com/anthropics/claude-code/issues/52689.

---

## 7. Timeouts (besonders relevant bei langsamen/streamenden OpenRouter-Backends)

### 7.1 Harter 5-Min-Request-Timeout ignorierte `API_TIMEOUT_MS` — gefixt 2.1.106
**Symptom:** Slow Backends (lokale LLMs, langsame Gateways, Extended Thinking) brachen nach 5 Min ab, egal welcher
`API_TIMEOUT_MS`-Wert.
**Versionen:** bis 2.1.105; **gefixt ab 2.1.106**.
**FIX:** Auf 2.1.177 erledigt. Zusaetzlich `API_TIMEOUT_MS` grosszuegig (Default 600000 ms = 10 Min; Max 2147483647 —
hoehere Werte ueberlaufen den Timer → sofortiger Fehlschlag).
**Quelle:** CHANGELOG 2.1.106 · code.claude.com/docs/en/env-vars.

### 7.2 5-Min-Idle-Timeout bricht langsam streamende Gateways ab — steuerbar ab 2.1.169
**Symptom:** Stream bricht ab, wenn das Gateway zwischen Chunks > 5 Min pausiert.
**Ursache:** Auf allen Nicht-Anthropic-Providern ist ein 5-Min-Idle-Timeout aktiv.
**Versionen:** steuerbar ab **2.1.169**.
**FIX:** `API_FORCE_IDLE_TIMEOUT=0` (deaktiviert den Idle-Timeout; `=1` erzwingt ihn ueberall). Verwandt:
`CLAUDE_STREAM_IDLE_TIMEOUT_MS` (Streaming-Watchdog, Default 90s, ab 2.1.84).
**Quelle:** code.claude.com/docs/en/env-vars.

### 7.3 Timeout bei langen Tasks — Tokens werden trotzdem berechnet
**Symptom:** Lange Generierungen (ganze Dateien, Test-Suites) brechen mit Timeout ab; Tokens sind verbraucht.
**Ursache:** Client-Timeout kuerzer als Generierungszeit. Anderes Fehlerbild als Rate-Limit.
**FIX:** Aufgaben in kleinere Einheiten zerlegen, Output begrenzen, Timeout-Toleranz erhoehen — nicht mit Rate-Limit-
Retries vermischen.
**Quelle:** evolink.ai/blog (2026-05).

---

## 8. Interactive-Mode / Onboarding ruft `api.anthropic.com` trotz BASE_URL

### 8.1 Interactive-Mode-Startup umgeht `ANTHROPIC_BASE_URL` (#36998, geschlossen)
**Symptom:** `claude` (interaktiv) verbindet beim Start direkt zu `api.anthropic.com` (`ECONNREFUSED` /
`Unable to connect to Anthropic services`), wenn direkter Egress geblockt ist. `claude -p` (Print-Mode) respektiert
`ANTHROPIC_BASE_URL` korrekt.
**Ursache:** Org-/Auth-/Telemetrie-Lookup beim Start umgeht den Proxy.
**Versionen:** v2.1.80 (Linux); **Issue #36998 GESCHLOSSEN** (per Seiten-Fetch verifiziert).
**FIX/Workaround:** In reinen Proxy-Umgebungen `claude -p` nutzen; alternativ `api.anthropic.com` im Proxy/Firewall
durchlassen statt hart blocken; Telemetrie reduzieren mit `DISABLE_TELEMETRY=1`.
**Quelle:** github.com/anthropics/claude-code/issues/36998.

### 8.2 Erststart ohne `hasCompletedOnboarding` prueft gegen `api.anthropic.com`
**Symptom:** Neue User (z.B. LiteLLM-Setup) bekommen beim allerersten Start `Failed to connect to api.anthropic.com`.
Erst nach `"hasCompletedOnboarding": true` in `~/.claude.json` geht es.
**Ursache:** Der Onboarding-API-Key-Check prueft gegen `api.anthropic.com` statt gegen `ANTHROPIC_BASE_URL`.
**Versionen:** 2.1.22 / 2.1.47 (macOS); als Duplikat von #15274 markiert; Status unklar (Snippet).
**FIX/Workaround:** `"hasCompletedOnboarding": true` manuell in `~/.claude.json` setzen, bevor die CLI zum ersten Mal startet.
**Quelle:** github.com/anthropics/claude-code/issues/26935 · /issues/15274.

---

## 9. Rate-Limits, Kosten & OpenRouter-Plattform

### 9.1 Rate-Limit-Stacking: zwei unabhaengige 429-Quellen
**Symptom:** 429 mal als „OpenRouter Rate limit exceeded", mal als durchgereichtes Anthropic-Limit — schwer zu
diagnostizieren.
**Ursache:** ZWEI Systeme: (1) OpenRouter-Plattform-Limits — `:free` z.B. **20 RPM**, 50–1000 Requests/Tag (abhaengig von
≥10 gekauften Credits); Paid kein hartes OpenRouter-Limit. (2) Upstream-Anthropic-Limits (RPM/ITPM/OTPM) auf Basis der
OpenRouter-Org-Allokation. Limits sind **global** — Zusatz-Accounts/Keys umgehen sie nicht.
**FIX:** 429 zuordnen: „Rate limit exceeded" ohne Provider-Detail = OpenRouter-Schicht → Rate senken/Plan upgraden/strecken.
Fuer agentische Bursts paid statt free, Requests serialisieren. Negativer Credit-Saldo → `402` (auch fuer Free-Modelle) →
Credits aufladen. Pruefbar via `GET https://openrouter.ai/api/v1/key`.
**Quelle:** evolink.ai/blog · openrouter.ai/docs/api/reference/limits.

### 9.2 „Provider returned error" (502) ≠ echtes Rate-Limit
**Symptom:** `{"error":{"code":502,"message":"Provider returned error: [upstream details]"}}`.
**Ursache:** OpenRouter hat weitergeleitet, der Upstream (Anthropic o.a.) hat abgelehnt — Ursache UPstream (Quota,
Kontextlaenge, transient).
**FIX:** Upstream-Detail auswerten und dort fixen (nicht blind OpenRouter-Plan upgraden). Bei Kontextlaenge → Aufgabe
splitten / neue Session.
**Quelle:** evolink.ai/blog.

### 9.3 Kontextlimit (400) — oft kleiner als beim nativen Claude
**Symptom:** `400 input length and max_tokens exceed context limit`, oft als durchgereichter Upstream-Fehler.
**Ursache:** Kombinierte Eingabe + `max_tokens` uebersteigt das Kontextfenster des konkreten Modells. Ueber OpenRouter
ist das Fenster modell-/upstream-spezifisch und oft kleiner (manche Routen nur 200K statt 1M; viele Free-Modelle 32–40K).
Auto-Compact greift bei Nicht-Anthropic-Modellen nicht zwingend gleich.
**FIX:** Aufgabe chunken / neue Session; Modell mit groesserem Kontext waehlen (Limit auf der OpenRouter-Modellseite pruefen).
**Quelle:** evolink.ai/blog · github.com/anthropics/claude-code/issues/3012.

### 9.4 Telemetrie spart sinnlose Requests
**Hinweis:** `DISABLE_TELEMETRY=1` (mehrfach in Praxis-Aliassen empfohlen) reduziert ueberfluessige Hintergrund-Calls,
die bei Custom-Providern teils ins Leere/404 laufen.
**Quelle:** Community-Praxis (mehrere Blogs).

---

## 10. Plattform-Fallen (Windows / macOS) beim Setzen der Provider-Konfig

### 10.1 `setx` macht Variablen nur in NEUEN Sessions sichtbar
**Symptom:** Nach `setx ANTHROPIC_BASE_URL ...` startet `claude` im selben Fenster trotzdem gegen Anthropic;
`echo $env:ANTHROPIC_BASE_URL` ist leer.
**Ursache:** `setx` schreibt in die Registry (persistent), aktualisiert aber **nicht** die laufende Prozess-Umgebung.
**FIX:** Persistent UND laufende Session fuettern, dann neues Fenster:
```powershell
[Environment]::SetEnvironmentVariable("ANTHROPIC_BASE_URL","https://openrouter.ai/api","User")
$env:ANTHROPIC_BASE_URL = "https://openrouter.ai/api"   # nur aktuelle Session
```
**Plattform:** Windows. **Quelle:** learn.microsoft.com (setx) · mindwiredai.com.

### 10.2 `setx` 1024-Zeichen-Limit zerstoert PATH / lange Tokens
**Symptom:** „WARNING: The data being saved is truncated to 1024 characters" trotz „SUCCESS"; lange Tokens oder
`setx PATH "%PATH%;..."` werden abgeschnitten → PATH kaputt, danach fehlen python/git/node.
**Ursache:** Hartes 1024-Zeichen-Limit von `setx`, meldet trotzdem Erfolg.
**FIX:** NIE `setx` fuer PATH/lange Werte. `[Environment]::SetEnvironmentVariable(...,"User")` (kein 1024-Limit).
**Plattform:** Windows. **Quelle:** wiert.me · learn.microsoft.com · CloudBees KBEC-00068.

### 10.3 UTF-8-BOM bricht den JSON-Parse von settings.json (Windows) — per Design
**Symptom:** `/doctor` meldet settings.json als malformed, obwohl sie valide aussieht (`Unrecognized token '﻿'`);
der `env`-Block wird ignoriert → Claude geht weiter gegen Anthropic.
**Ursache:** Windows-Editoren/PowerShell `Out-File`/`>`/`Set-Content` schreiben UTF-8 **mit BOM**.
**Versionen:** **per Design / NOT_PLANNED** (#9906) — bleibt bestehen, BOM-frei speichern ist Pflicht.
**FIX:** BOM-frei als „UTF-8" speichern. PowerShell:
`[IO.File]::WriteAllText("$env:USERPROFILE\.claude\settings.json",$json,(New-Object Text.UTF8Encoding $false))`
(PS7+: `Set-Content -Encoding utf8NoBOM`). **Volltext:** `bugs/claude-tooling/claude-config.md §3.2` (eigener Vorfall,
2× getroffen). Parser ist zudem strikt gegen Kommentare/trailing commas.
**Quelle:** github.com/anthropics/claude-code/issues/9906 · /issues/14442 · `claude-config.md §3.2`.

### 10.4 Vergessene `ANTHROPIC_BASE_URL` schickt dich beim Zurueckwechseln an den falschen Endpoint
**Symptom:** Direkt-Login mit Anthropic-Subscription scheitert mit verwirrenden Auth-/„model-not-found"-Fehlern; oder
`/login` wirkt, aber Calls landen am alten Endpoint.
**Ursache:** Eine persistent gesetzte `ANTHROPIC_BASE_URL` ueberlebt Session, Reboot und `/logout` (das loescht nur die
OAuth-Session, NICHT die Shell-/Registry-Variable).
**FIX:** Erst `/status` (welcher Endpoint+Auth aktiv?). Zum sauberen Zurueck:
```powershell
[Environment]::SetEnvironmentVariable("ANTHROPIC_BASE_URL",$null,"User")
[Environment]::SetEnvironmentVariable("ANTHROPIC_AUTH_TOKEN",$null,"User")
Remove-Item Env:\ANTHROPIC_BASE_URL,Env:\ANTHROPIC_AUTH_TOKEN -ErrorAction SilentlyContinue
```
Bei settings.json: den `env`-Block entfernen. Neue Konsole, `/status` gegenchecken.
**Quelle:** openrouter.ai/docs/.../claude-code-integration · code.claude.com/docs/en/authentication.

### 10.5 Beide Auth-Variablen gleichzeitig → verwirrende Fehler
Siehe §3.1 (FIX: `ANTHROPIC_API_KEY=""`) + §3.2 (Praezedenz). Auf Windows besonders zaeh, weil persistente User-Env
+ gecachter Login zusammenkommen — `/status` ist der Wahrheits-Check.

### 10.6 Praezedenz settings.json `env` vs. Shell-Env vs. CLI-Flag richtig verstehen
**Symptom:** Eine Variable wird „mal so, mal so" wirksam.
**Ursache:** Zwei Achsen werden verwechselt: (a) **Settings-Achse** (Wert mit direktem Aequivalent): System-Env →
Shell-rc → inline `export` → projekt-`.env` → VS-Code-`environmentVariables` → **settings.json-Key** → **CLI-Flag**
(hoechste). (b) **Auth-Auswahl-Achse** (§3.2). Settings-Datei-Merge: global → project → **local gewinnt**.
**FIX:** Provider-Config an EINER Stelle fuehren — Empfehlung: global `~/.claude/settings.json` `env`-Block, Token in
`settings.local.json` (gitignored). Nach jeder Aenderung `/status`.
**Quelle:** claude-codex.fr/en/reference/environment · `best-practices/claude-tooling/settings.md`.

### 10.7 Git Bash / MINGW64 verstuemmelt die Base-URL (POSIX-Pfad-Konvertierung)
**Symptom:** In Git Bash wird ein Wert mit `/` zu einem Windows-Pfad (`/api` → `C:/Program Files/Git/api`) → Endpoint kaputt.
**Ursache:** MSYS2/MINGW64 konvertiert pfad-aehnliche Argumente automatisch.
**FIX:** `export MSYS_NO_PATHCONV=1` voranstellen, ODER (sauberer) Provider-Vars NICHT in Git Bash exportieren, sondern
als Windows-User-Env (`[Environment]::SetEnvironmentVariable`) — Git Bash erbt sie dann ohne Mangling.
**Plattform:** Windows (Git Bash). **Quelle:** CloudBees KBEC-00420 · todzhang.com.

### 10.8 PowerShell-Quoting bei Tokens mit Sonderzeichen
**Symptom:** Token wird veraendert/gekuerzt gesetzt → 401, obwohl korrekt kopiert.
**Ursache:** Doppelte Quotes interpolieren `$` und behandeln Backtick als Escape.
**FIX:** In PowerShell **einfache** Quotes nutzen: `[Environment]::SetEnvironmentVariable("ANTHROPIC_AUTH_TOKEN",'sk-or-v1-...','User')`.
Am robustesten: Token als reinen JSON-String in settings.json (umgeht Shell-Quoting komplett).
**Plattform:** Windows. **Quelle:** mindwiredai.com.

### 10.9 Token im Klartext im Shell-Profil (Sicherheit, macOS/Linux)
**Symptom:** OpenRouter-Key liegt ungeschuetzt in `.zshrc`/`.bashrc`; Leak-Risiko bei dotfiles-Repos/Screen-Sharing.
**FIX:** Key nicht roh ins Profil — `settings.local.json` (gitignored) oder `apiKeyHelper` (Secret-Store/Keychain;
Refresh per `CLAUDE_CODE_API_KEY_HELPER_TTL_MS`). Entspricht Franks CLAUDE.md-Regel „keine Secrets roh; .gitignore".
**Plattform:** macOS/Linux (analog Windows-dotfiles). **Quelle:** openrouter.ai/docs/.../claude-code-integration · code.claude.com/docs/en/authentication.

### 10.10 `.env`-Datei im Projekt wird vom nativen Installer NICHT gelesen
**Symptom:** Provider-Config in projekt-lokaler `.env` bleibt wirkungslos.
**Ursache:** Der native Claude-Code-Installer liest Standard-`.env` nicht automatisch.
**FIX:** Config in `~/.claude/settings.json` (`env`-Block) oder als echte exportierte Shell-/User-Env-Var. Wenn `.env`
gewuenscht: vorher `set -a; source .env; set +a`, dann `claude`.
**Quelle:** openrouter.ai/docs/.../claude-code-integration · claude-codex.fr.

---

## 11. Auto-Routing pro Aufgabe — verschiedene Modelle automatisch (Franks Ziel: spaeter)

> **Grenzziehung (wichtig fuer die Entscheidung):** **Echtes aufgabenbasiertes Auto-Routing** (verschiedene Modelle fuer
> `default`/`background`/`think`/`longContext`/`webSearch`) gibt es **NUR mit externem Router/Proxy**. **Bordmittel allein**
> (Env-Vars) erlauben nur eine **grobe 3-Rollen-Verteilung** (Haupt = Opus/Sonnet, Hintergrund = Haiku) + `opusplan`
> (Opus in Plan-Mode, Sonnet in Execution, nur Anthropic) — **kein** Routing nach Kontextlaenge oder Reasoning-Bedarf.

### 11.1 Was OHNE Proxy geht (Bordmittel)
`ANTHROPIC_DEFAULT_OPUS/SONNET/HAIKU/FABLE_MODEL` mappen nur **Alias → Modell-ID**. `ANTHROPIC_DEFAULT_HAIKU_MODEL` ist
zugleich das „background functionality"-Modell (Auto-Compaction, Titel) — die einzige eingebaute Background-Rolle. Mehr
„Auto" geht ohne Proxy nicht. (`ANTHROPIC_BASE_URL` aendert nur das Ziel, nicht das Routing — §1.)

### 11.2 claude-code-router (CCR) — das eigentliche Auto-Routing-Tool
`Router`-Rollen: `default`, `background`, `think`, `longContext` (ab `longContextThreshold`, **Default 60000** Token via
tiktoken `cl100k_base`), `webSearch`, `image`. Format je Rolle: **`provider,model`**. Beispiel mit OpenRouter:
```json
{
  "Providers": [{
    "name": "openrouter",
    "api_base_url": "https://openrouter.ai/api/v1/chat/completions",
    "api_key": "sk-or-v1-…",
    "models": ["anthropic/claude-sonnet-4","google/gemini-2.5-pro","deepseek/deepseek-chat"],
    "transformer": { "use": ["openrouter"] }
  }],
  "Router": {
    "default": "openrouter,anthropic/claude-sonnet-4",
    "background": "openrouter,deepseek/deepseek-chat",
    "think": "openrouter,anthropic/claude-3.7-sonnet:thinking",
    "longContext": "openrouter,google/gemini-2.5-pro",
    "longContextThreshold": 60000,
    "webSearch": "openrouter,perplexity/sonar"
  }
}
```
Config: `~/.claude-code-router/config.json`. Custom-Logik pro Aufgabe: `CUSTOM_ROUTER_PATH` (JS-Modul). Subagent-Routing
per Inline-Tag `<CCR-SUBAGENT-MODEL>provider,model</CCR-SUBAGENT-MODEL>` (muss am Prompt-Anfang stehen, sonst ignoriert).
Project-Level-Override: `~/.claude/projects/<id>/claude-code-router.json` (Vorrang vor global).
**Quelle:** musistudio.github.io/claude-code-router/docs/server/config/routing.

### 11.3 ⭐ HAEUFIG — CCR Feldnamen-Chaos (Doku widerspricht sich)
**Symptom:** Provider „registriert erfolgreich", aber Requests scheitern/werden nicht geroutet; teils HTML-500 (§11.4).
**Ursache:** Die Provider-Doku zeigt `NAME/HOST/APIKEY/MODELS` mit `transformers:["anthropic"]`; reale funktionierende
Configs (+README) nutzen **`name/api_base_url/api_key/models`** mit **`transformer:{use:["openrouter"]}`**. Fuer OpenRouter
ist der richtige Transformer **`openrouter`** (nicht `anthropic`).
**FIX:** lowercase-Felder verwenden, `transformer:{use:["openrouter"]}`, `api_base_url` mit vollem `…/api/v1/chat/completions`-Pfad.
**Quelle:** github.com/musistudio/claude-code-router/issues/1159 · /issues/1020 · Doku /config/providers.

### 11.4 CCR: OpenRouter liefert HTML statt JSON → 500 „Unexpected token '<'"
**Symptom:** `500 ... Unexpected token '<'` bei `transformResponseIn`; direkter `curl` geht, CCR nicht.
**Ursache:** OpenRouter gibt bei Fehlern (Rate-Limit, falscher Pfad, Auth-Edgecase) eine **HTML**-Fehlerseite; der
Transformer erwartet JSON. Haeufiger Ausloeser: falsche Feldnamen (§11.3) oder fehlender `/chat/completions`-Pfad.
**FIX:** Feldnamen korrigieren, `LOG_LEVEL:"debug"`, `api_base_url` pruefen, Konto auf Guthaben/Rate-Limit checken.
**Quelle:** github.com/musistudio/claude-code-router/issues/1159.

### 11.5 ⭐ CCR: `think`/Background trifft Modell mit ERZWUNGENEM Reasoning → 400 (offen)
**Symptom:** `API Error 400 "Reasoning is mandatory for this endpoint and cannot be disabled."` (Background-/Subagent-Tasks).
**Ursache:** Die CLI sendet `thinking:{type:"disabled"}`; der `openrouter`-Transformer macht daraus `reasoning:{enabled:false}`.
Modelle mit Pflicht-Reasoning (z.B. `stepfun/step-3.5-flash:free`) lehnen das ab. Root Cause in `dist/cli.js`
(`transformRequestIn` sendet `reasoning` auch bei `disabled`).
**Versionen:** CCR 2.0.0; **Issue #1238 OFFEN** (per Seiten-Fetch verifiziert).
**FIX/Workaround:** Modelle mit Pflicht-Reasoning **nur** auf die `think`-Rolle legen, nicht auf `default`/`background`;
oder den Transformer patchen (reasoning nur bei `thinking.type==="enabled"` weiterleiten).
**Quelle:** github.com/musistudio/claude-code-router/issues/1238.

### 11.6 CCR: Gemini ueber OpenRouter bricht in langen Sessions ab (400)
**Symptom:** Nach einigen Minuten `API Error 400 ... INVALID_ARGUMENT` (z.B. Gemini 3 Pro auf der `think`-Rolle).
**Ursache:** Manche Modelle (v.a. Gemini ueber OpenRouter) lehnen Request-Strukturen ab, die die CLI/CCR ueber mehrere
Turns aufbaut (Tool-Use-Historie, Block-Reihenfolge, leere Argumente).
**FIX:** Auf kritischen Rollen robustere (Claude-)Modelle ueber OpenRouter; Gemini nur fuer `longContext`/`webSearch` mit
kurzen Historien; `fallback`-Liste konfigurieren (§11.8), damit die Session bei 400 nicht hart abbricht.
**Quelle:** github.com/musistudio/claude-code-router/issues/1020.

### 11.7 CCR: kein `same_as_default` (DRY-Falle, offen)
**Symptom:** Aendert man `default`, laufen `background`/`think`/`webSearch` still auf dem alten Modell weiter.
**Ursache:** CCR hat keine `same_as_default`-Option (Feature-Request #1007); jede Rolle muss explizit gesetzt werden.
**FIX:** Beim Aendern von `default` alle abhaengigen Rollen manuell mit-aendern.
**Quelle:** github.com/musistudio/claude-code-router/issues/1007.

### 11.8 CCR: Fallback nur bei HTTP-Fehler + doppelte Instanz = 401
**Fakten/Fallen:** `fallback`-Listen triggern **nur** bei fehlgeschlagener HTTP-Response, sequenziell; Backup-Modelle
**muessen** im `Providers`-Array existieren. Eine syntaktisch gueltige, aber unbrauchbare Antwort loest **keinen** Fallback aus.
**Doppelte-Instanz-Falle (#227):** Eine zweite, unbemerkte CCR-Instanz faengt Requests ab → `401 No auth credentials found`
in Retry-Schleife, obwohl die Config unveraendert ist. **FIX:** alte Prozesse killen (`ps aux | grep router` → `kill`) bzw.
`ccr restart`.
**Quelle:** Doku /config/routing · github.com/musistudio/claude-code-router/issues/227.

### 11.9 CCR: `custom_router_path` — zwei widerspruechliche Signaturen
**Falle:** README/npm zeigt `module.exports = async function(req, config) {…}` (`null` = Fallback auf Default-Router);
die Routing-Doku zeigt `module.exports = function(config, context)` mit `context={scenario,projectId,tokenCount}`. Falsche
Annahme → `undefined`-Zugriffe + stiller Fallback.
**FIX:** Installierte CCR-Version gegen `dist/cli.js` pruefen, defensiv beide Argumente loggen, Routing-Entscheidung loggen
(Observability), `return null`/Default-String fuer sichere Rueckfalllogik.
**Quelle:** Doku /config/routing (Custom Router) · github.com/musistudio/claude-code-router.

### 11.10 CCR: `webSearch`/`:online`-Suffix erforderlich
**Falle:** Fuer die `webSearch`-Rolle ueber OpenRouter muss dem Modellnamen der **`:online`-Suffix** angehaengt werden,
sonst kann das Modell nicht websuchen. (OpenRouters `:online` ist teils deprecated zugunsten `openrouter:web_search`-Server-Tool —
Stand pruefen.)
**Quelle:** Doku /config/routing · openrouter.ai/docs.

### 11.11 LiteLLM-Proxy: Streaming bricht bei Tool-Use (gefixt LiteLLM 1.75.7)
**Symptom:** Mit Nicht-Anthropic-Modell ueber LiteLLM auf `/v1/messages`: bei Tool-Calls
`[ERROR] Error streaming, falling back to non-streaming mode: Content block not found`.
**Ursache:** LiteLLMs Uebersetzung OpenAI-Streaming → Anthropic-Content-Blocks erzeugt bei Tool-Use-Chunks keine korrekte
Block-Reihenfolge.
**Versionen:** reproduzierbar < 1.75.7; **gefixt ab LiteLLM 1.75.7** (extern). Alternative: claude-code-router hat die
noetigen Transforms.
**FIX:** LiteLLM ≥ 1.75.7. **Sicherheits-Warnung:** LiteLLM PyPI **1.82.7/1.82.8 waren mit Credential-stehlender Malware
kompromittiert** (Anthropic warnt explizit) — Version pruefen, Third-Party-Proxies generell mit Vorsicht.
**Quelle:** github.com/BerriAI/litellm/issues/13373 · code.claude.com/docs/en/llm-gateway (LiteLLM-Warnung).

### 11.12 LiteLLM: 403 / Auth-Header-Praezedenz + `drop_params`
**Symptom:** 403 trotz gueltigem Virtual-Key; oder `400 Unknown parameter: 'output_config'` / `'... does not support ...'`.
**Ursache:** Der von der CLI gesendete Anthropic-OAuth-`Authorization`-Header uebersteuert den `x-api-key`/Virtual-Key;
zudem reicht LiteLLM Anthropic-spezifische Parameter (`output_config`) ungefiltert an OpenAI weiter.
**FIX:** `ANTHROPIC_API_KEY=""` leeren, sauber `ANTHROPIC_AUTH_TOKEN`/Virtual-Key setzen (kein OAuth-Token mitsenden);
`drop_params: true` in `litellm_settings`; `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1`.
**Quelle:** github.com/BerriAI/litellm/issues/29190 · /issues/22963 · /issues/24436.

### 11.13 cc-switch / ccswitch — Profil-Switcher (kein Auto-Routing)
**Sachverhalt:** cc-switch (mehrere gleichnamige Forks) schaltet **manuell** zwischen Konfig-Profilen (Anthropic-Account ↔
OpenRouter-Setup), verwaltet MCP/System-Prompts; manche Varianten betreiben einen lokalen Routing-Service. Fuer Franks
„automatisch bestimmte Modelle fuer bestimmte Aufgaben" ist das **nur ergaenzend** (schneller Wechsel), nicht die Kernloesung.
**Sicherheit:** Mehrere konkurrierende Forks mit aehnlichem Namen — vor Installation Repo/Maintainer/Commits pruefen
(Supply-Chain-/Prompt-Injection-Risiko bei Tools, die API-Keys verwalten). Bekannter Bug #1888: `context-management-*`-Beta
wird nicht gestrippt → 400 (betrifft SDK-Clients, nicht die CLI).
**Quelle:** github.com/venkycs/cc-switch · github.com/farion1231/cc-switch/issues/1888.

---

## Fix-Status (Schritt 3) — was ist schon behoben, was nicht?

> **Methodik & Ehrlichkeit:** Versions-Fixes unten sind **changelog-belegt** (offizielles `anthropics/claude-code`
> CHANGELOG, von Researcher 1 extrahiert). Da `gh` im Cowork-Sandbox fehlte und die GitHub-REST-API ueber WebFetch leere
> Bodies lieferte, sind Issue-**OPEN/CLOSED**-Status nur dort hart, wo Researcher die Seite direkt gefetcht haben
> (markiert); die uebrigen sind als „Status unklar" gefuehrt. Im Zweifel gilt ein Bug als **noch offen**.

### Belegt GEFIXT (≤ 2.1.177 → auf Franks Version bereits aktiv)

| Frueherer Bug | gefixt ab | Bezug |
|---------------|-----------|-------|
| Effort-Param 400 bei Custom-IDs | **2.1.70** | §4.5 |
| `tool_reference` 400 bei custom BASE_URL | **2.1.70** | §6.1 |
| MCP-Tool-Search-Leerantwort | **2.1.70** | §6.1 |
| Tool Search trotz BASE_URL reaktivierbar (`ENABLE_TOOL_SEARCH`) | **2.1.72** | §6.1 |
| `ANTHROPIC_CUSTOM_MODEL_OPTION` (+`_NAME`/`_DESCRIPTION`) eingefuehrt | **2.1.73** | §5.2 |
| `DISABLE_EXPERIMENTAL_BETAS` unterdrueckt structured-outputs-Header | **2.1.81** | §6.2 |
| `_SUPPORTED_CAPABILITIES` / `ALWAYS_ENABLE_EFFORT` (3p-Feature-Deklaration) | **2.1.84** | §4.4/§4.5 |
| `/fast` Skip-Org-Check (`CLAUDE_CODE_SKIP_FAST_MODE_ORG_CHECK`) | **2.1.96** | §5.7 |
| Harter 5-Min-Timeout ignorierte `API_TIMEOUT_MS` | **2.1.106** | §7.1 |
| Bedrock-403 Auth-Header-Kollision | **2.1.106** | §3.5 |
| Custom-Keybindings auf 3p-Providern | **2.1.106** | — |
| Prompt-Caching-TTL-Steuerung + Start-Warnung | **2.1.108** | §6.5 |
| Gateway-Model-Discovery (`/v1/models`) | **2.1.129** (Feature) | §5.2 |
| Background-Side-Query Fallback auf Main-Loop-Modell (Gateway/Bedrock/Vertex/Foundry) | **2.1.141** | §4.2 |
| `/model` speichert Default fuer neue Sessions | **2.1.153** (Verhalten) | §5.3 |
| `API_FORCE_IDLE_TIMEOUT` (Idle-Timeout steuerbar) | **2.1.169** (Feature) | §7.2 |
| LiteLLM Streaming-Tool-Use (extern) | **LiteLLM 1.75.7** | §11.11 |

### Noch NICHT gefixt / per Design / Status offen (Workaround bleibt aktiv)

| Bug | Status | Workaround |
|-----|--------|------------|
| UTF-8-BOM bricht settings.json | **per Design** (#9906 NOT_PLANNED) | BOM-frei speichern (§10.3) |
| `count_tokens` fehlt bei OpenRouter | per Design OpenRouter; CLI-Toleranz unklar | Default-Modelle sauber setzen (§2.2/§4.1) |
| `ANTHROPIC_BASE_URL` aendert nicht das Modell | per Design | Modell separat setzen (§1) |
| Background/Haiku-404 ohne Override | per Design (Teil-Fallback ab 2.1.141) | alle `DEFAULT_*_MODEL` setzen (§4.1) |
| Prompt-Caching-Verlust ueber OpenAI-Wire | strukturell/per Design | nativen Anthropic-Endpoint (§6.4) |
| `/model`-Custom-Modell rutscht intermittierend auf Anthropic-ID | **offen** #18025 (Snippet, unklar) | alle Tier-Vars pinnen (§5.5) |
| `cache_control` auf leerem Text-Block → 400 | **offen** #52689 (verifiziert OFFEN) | Bild immer mit ≥1 Zeichen Text (§6.6) |
| `apiKeyHelper` erbt `ANTHROPIC_BASE_URL` nicht | gemeldet #26999, Fix unklar | Base-URL dem Helper explizit geben (§3.4) |
| Interactive/Onboarding ruft api.anthropic.com | #36998 **GESCHLOSSEN**; #26935/#15274 unklar | `claude -p` / `hasCompletedOnboarding` (§8) |
| CCR Pflicht-Reasoning 400 | **offen** #1238 (verifiziert OFFEN) | Pflicht-Reasoning-Modelle nur auf `think` (§11.5) |
| CCR kein `same_as_default` | **offen** #1007 (Feature-Request) | Rollen manuell synchron halten (§11.7) |

---

## Bug ↔ Best-Practices — Abschnitts-Bezugstabelle

| Bug-Abschnitt (dieser Almanach) | Best-Practice-Gegenstueck |
|---------------------------------|---------------------------|
| §2/§3/§4 Setup-Grundlagen, Env-Vars | `best-practices/claude-tooling/openrouter-claude-code.md` (Setup-Rezept, „richtig von Anfang an") |
| §10.3 BOM, §10.6 Praezedenz | `best-practices/claude-tooling/settings.md` · `bugs/claude-tooling/claude-config.md §3.2` |
| §6.1 tool_reference / Tool Search | `best-practices/claude-tooling/mcp.md` |
| §1 Drittanbieter-Architektur | `best-practices/claude-tooling/claude-code-desktop-vs-cli.md` |
| §5 Modelle/Subagent-Modell | `best-practices/claude-tooling/settings.md` |

---

## Pflicht-Checkliste (vor + waehrend der OpenRouter-Arbeit)

- [ ] `ANTHROPIC_BASE_URL=https://openrouter.ai/api` — **ohne `/v1`**, ohne trailing slash (§2.1).
- [ ] `ANTHROPIC_AUTH_TOKEN=<OpenRouter-Key>`, `ANTHROPIC_API_KEY=""` **explizit leer** (§3.1).
- [ ] **Alle drei** `ANTHROPIC_DEFAULT_OPUS/SONNET/HAIKU_MODEL` gesetzt (sonst Background-404, §4.1) + `CLAUDE_CODE_SUBAGENT_MODEL` (§5.4).
- [ ] `CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS=1` gegen Beta-Header-400er (§6.2).
- [ ] Lange/langsame Backends: `API_TIMEOUT_MS` hoch + `API_FORCE_IDLE_TIMEOUT=0` (§7).
- [ ] settings.json **UTF-8 ohne BOM** speichern (§10.3), Token in `settings.local.json` (gitignored, §10.9).
- [ ] Windows: Vars per `[Environment]::SetEnvironmentVariable(...,"User")`, **nicht** `setx` (§10.1/§10.2); nicht in Git Bash exportieren (§10.7).
- [ ] Nach jeder Aenderung **`/status`** als Wahrheits-Check (Endpoint + Auth-Quelle).
- [ ] Kosten im Blick: Prompt-Caching nur am nativen Anthropic-Endpoint (§6.4); fuer reines Claude oft Anthropic-direkt einfacher/billiger.
- [ ] Auto-Routing pro Aufgabe braucht claude-code-router — dort Feldnamen `name/api_base_url/api_key/models` + `transformer:{use:["openrouter"]}` (§11.3), `/chat/completions`-Pfad, `fallback`-Listen.
- [ ] Zurueck zu Anthropic: `ANTHROPIC_BASE_URL`/`ANTHROPIC_AUTH_TOKEN` wirklich **entfernen**, nicht nur `/logout` (§10.4).
