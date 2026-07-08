# Hoechstes Modell + groesstes Kontextfenster ueberall (KRITISCH)

## Grundregel
Jeder Subagent/Worker/Researcher laeuft IMMER auf dem hoechsten Opus mit groesstem Kontextfenster
(`opus[1m]`, Opus 4.8, 1M Token). Erscheint ein hoeheres, gilt automatisch das. NIEMALS Sonnet/Haiku
erzwingen — Massstab ist Stabilitaet+Intelligenz, nicht Kosten/Tempo (Frank nimmt Mehrkosten in Kauf).

> **Eine Ausnahme (seit 2026-07-01):** Web-Research-Eskalationsstufe C (Sonnet-5-Schwarm,
> `research-strategy.md` §4a) laeuft bewusst auf **Sonnet 5** (`model:"sonnet"`, natives 1M-Kontext).
> Gilt AUSSCHLIESSLICH fuer Engine-C-Spawns — kein anderer Subagent leitet daraus "Sonnet ist ok" ab.

## Warum
Groesseres Fenster (1M statt 200k) verhindert "Prompt is too long"-Crashes; das 1M-Fenster kostet keinen
Aufpreis pro Token jenseits 200k. Mit Output-Disziplin (`subagent-crash-proofing.md`) bleiben Kosten moderat.

## Mechanismus (seit 2026-07-01)
`settings.json` → `env.CLAUDE_CODE_SUBAGENT_MODEL = "inherit"` (KEINE globale Zwangs-Ueberschreibung
mehr). Jeder der 32 eigenen Agents pinnt `model: opus[1m]` im Frontmatter. Ad-hoc-Spawns
(`general-purpose`) erben `inherit` → **MUESSEN `model:"opus[1m]"` explizit mitgeben** (Ausnahme Engine
C = `model:"sonnet"`). Session-Modell = `opus[1m]`. `config-guard` Allowlist `{sonnet, opus, opus[1m], inherit}`.

## Workflows
Das Workflow-Tool nutzt das Session-Modell (`opus[1m]`). `agent(prompt)` ohne `opts.model` erbt opus[1m]
(richtig). `opts.model` NIE auf sonnet/haiku (ausser bewusst fuer triviale, anfrage-dichte Stage).

## Was NIEMALS
- Subagenten auf Sonnet/Haiku erzwingen (ausser Engine-C-Ausnahme) · bei ad-hoc-Spawn `model` weglassen
  (kein Auto-Opus-Fallback mehr) · kleineres Fenster als das groesste · `inherit` als Fehler melden oder
  auf opus[1m] "zuruecksetzen" (zerstoert die Sonnet-5-Ausnahme) · die Sonnet-5-Ausnahme ohne Franks OK ausweiten.
