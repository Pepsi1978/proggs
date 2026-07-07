# OpenRouter (Custom-API-Provider) in der Claude Code CLI — Best Practices

> **Stand 2026-06-17, Claude Code 2.1.177, OpenRouter „Anthropic Skin".** Gegenstueck zum Almanach
> `bugs/claude-tooling/openrouter-claude-code.md` (dort: was schiefgeht ↔ hier: wie man es von Anfang
> an richtig macht). Adressat ist Claude Code selbst beim Einrichten von Franks OpenRouter-Anbindung.
> **Anker:** claude-code=2.1.177

---

## 1. Grundprinzip (einmal verstehen, dann fallen 80 % der Fehler weg)

Claude Code spricht **nur** die Anthropic-Messages-API. OpenRouter hat dafuer eine **„Anthropic Skin"**
unter `https://openrouter.ai/api`. Zwei Variablen sind **orthogonal**: `ANTHROPIC_BASE_URL` = WOHIN,
`ANTHROPIC_MODEL`/`ANTHROPIC_DEFAULT_*_MODEL` = WELCHES Modell. Beide muessen gesetzt sein.

Drei innere Modell-Tiers existieren immer: **Haiku** (Hintergrund), **Sonnet** (Coding), **Opus**
(Reasoning). Wer nicht alle drei mappt, bekommt zur Laufzeit 404 im Hintergrund.

---

## 2. Das robuste Standard-Setup (Reihenfolge einhalten)

1. OpenRouter-Key erstellen (openrouter.ai/settings/keys), Konto mit Guthaben.
2. Config an EINER Stelle fuehren — empfohlen: `~/.claude/settings.json` `env`-Block (gilt projektweit),
   Token in `settings.local.json` (gitignored). **UTF-8 ohne BOM** speichern.
3. Diese Variablen setzen:
   ```json
   {
     "env": {
       "ANTHROPIC_BASE_URL": "https://openrouter.ai/api",
       "ANTHROPIC_AUTH_TOKEN": "sk-or-v1-…",
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
4. Bei vorherigem Anthropic-Login: einmal `/logout`. Terminal neu starten.
5. **`/status`** als Wahrheits-Check: `Base URL: https://openrouter.ai/api`, Auth via `ANTHROPIC_AUTH_TOKEN`.

---

## 3. Die fuenf goldenen Regeln (Prävention)

1. **Base-URL ohne `/v1`** (`https://openrouter.ai/api`). `/v1` ist die OpenAI-URL → falsch fuer die CLI.
2. **`ANTHROPIC_API_KEY` explizit leeren** (`""`), nicht nur unset — sonst Auth-Konflikt/Fallback zu Anthropic.
3. **Alle drei `DEFAULT_*_MODEL` + `SUBAGENT_MODEL` setzen** — sonst 404 bei Hintergrund-/Subagent-Tasks.
4. **`DISABLE_EXPERIMENTAL_BETAS=1`** — verhindert 400er durch Anthropic-exklusive Beta-Header am Gateway.
5. **Caching-Kosten kennen:** Prompt-Caching wirkt nur am nativen Anthropic-Endpoint. Fuer reines Claude
   ist Anthropic-direkt oft einfacher und billiger; OpenRouter lohnt fuer Modell-Breite/Experimente.

---

## 4. Mehrere Modelle nutzen & wechseln

- **Mehrere parallel:** je Terminal `claude --model <slug>` starten (Modell gilt pro Session).
- **Laufzeit-Wechsel:** nur `/model <slug>` (Env-Var greift erst nach Neustart). Slug direkt eintippbar; fuer
  einen festen Picker-Eintrag `ANTHROPIC_CUSTOM_MODEL_OPTION="<slug>"`.
- **Slug-Format:** `provider/model`, optional `~…-latest` (neueste Version), `:free` (kostenlos/rate-limitiert).

---

## 5. Auto-Routing pro Aufgabe (Franks spaeteres Ziel) — der saubere Weg

Bordmittel koennen **kein** task-basiertes Routing (nur Alias→Modell + `opusplan`). Fuer „bestimmte Aufgaben →
bestimmte Modelle automatisch" ist **claude-code-router (CCR)** der direkte Weg. Saubere CCR-Config:

- Felder **lowercase**: `name`, `api_base_url` (voller `…/api/v1/chat/completions`-Pfad!), `api_key`, `models`.
- `transformer: { "use": ["openrouter"] }` (nicht `anthropic`).
- `Router`-Rollen: `default`, `background` (billig), `think` (Reasoning), `longContext` (ab `longContextThreshold`,
  Default 60000), `webSearch` (Modell mit `:online`/web-search-Support), `image`.
- Pro Rolle eine `fallback`-Liste; Pflicht-Reasoning-Modelle **nur** auf `think` (sonst 400).
- Beim Aendern von `default` alle abhaengigen Rollen mit-aendern (kein `same_as_default`).
- Routing-Entscheidungen loggen (`LOG_LEVEL: "debug"`) — Observability gegen stille Fehlrouten.

Alternativen: LiteLLM (≥ 1.75.7 wegen Tool-Use-Streaming; Versionen 1.82.7/1.82.8 wegen Malware meiden),
cc-switch (nur manuelles Profil-Umschalten, kein Auto-Routing; Forks vor Installation pruefen).

---

## 6. Zurueck zu Anthropic (sauber)

`ANTHROPIC_BASE_URL` und `ANTHROPIC_AUTH_TOKEN` **wirklich entfernen** (Registry/`env`-Block), nicht nur
`/logout`. Dann neue Konsole + `/status`.

---

## Best-Practice ↔ Bug — Abschnitts-Bezugstabelle

| Best-Practice (hier) | Bug-Gegenstueck (`bugs/claude-tooling/openrouter-claude-code.md`) |
|----------------------|------------------------------------------------------------------|
| §2 Setup / §3 Regel 1 | §2.1 Base-URL `/v1`-Falle |
| §3 Regel 2 | §3.1 `ANTHROPIC_API_KEY` leeren |
| §3 Regel 3 / §4 | §4.1 Background-404, §5 Modellwechsel |
| §3 Regel 4 | §6.2 Beta-Header-400 |
| §3 Regel 5 | §6.4 Prompt-Caching-Verlust |
| §5 Auto-Routing | §11 (CCR/LiteLLM/cc-switch) |
| §2 BOM/Settings | §10.3, §10.6 · `claude-config.md §3.2` |
