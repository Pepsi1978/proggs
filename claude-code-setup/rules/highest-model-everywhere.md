# Hoechstes Modell + groesstes Kontextfenster ueberall (KRITISCH)

> Gilt in JEDER Session fuer JEDEN Subagent/Worker/Researcher. Ergaenzt [[subagent-crash-proofing]]
> und [[lossless-context-principle]].

## Grundregel

Jeder Subagent/Worker/Researcher laeuft IMMER auf dem hoechsten Opus mit groesstem Kontextfenster
(aktuell `opus[1m]`, Opus 4.8, 1M Token). Erscheint ein hoeheres Opus/groesseres Fenster, gilt
automatisch das. **NIEMALS Sonnet/Haiku erzwingen** — Massstab ist Stabilitaet+Intelligenz, nicht
Kosten/Tempo (Frank nimmt Mehrkosten bewusst in Kauf).

> **Eine Ausnahme (seit 2026-07-01):** Die Web-Research-Eskalationsstufe C (Sonnet-5-Schwarm,
> `research-strategy.md` §4a) laeuft bewusst auf **Sonnet 5** (`model:"sonnet"`, natives 1M-Kontext).
> Gilt AUSSCHLIESSLICH fuer Engine-C-Research-Spawns — kein anderer Subagent darf daraus "Sonnet ist
> ok" ableiten.

## Warum

Groesseres Fenster (1M statt 200k) verhindert "Prompt is too long"-Crashes; das 1M-Fenster kostet
laut Doku keinen Aufpreis pro Token jenseits 200k (nur Opus-pro-Token). Mit Output-Disziplin
([[subagent-crash-proofing]]) bleiben Kosten moderat.

## Mechanismus (seit 2026-07-01: inherit + Pro-Agent-Pinning)

| Schicht | Wo | Wirkung |
|---------|-----|---------|
| 1 | `settings.json` → `env.CLAUDE_CODE_SUBAGENT_MODEL = "inherit"` | KEINE globale Zwangs-Ueberschreibung mehr; jeder Subagent nutzt sein Frontmatter bzw. den `model`-Parameter. |
| 2 | Agent-Frontmatter `model: opus[1m]` (alle 32 eigenen Agents) | Explizit gepinnt — traegt jetzt die Last. Verhalten unveraendert. |
| 3 | Ad-hoc-Spawns (`general-purpose` ohne Agent-File) | Erben `inherit` → **MUESSEN `model:"opus[1m]"` explizit mitgeben** (Ausnahme: Engine-C = `model:"sonnet"`). Kein Auto-Opus-Fallback mehr. |
| 4 | `settings.json` → `model = "opus[1m]"` | Hauptagent-Modell (`session-guard`). |
| 5 | `config-guard` | Allowlist `{sonnet, opus, opus[1m], inherit}`. |

**Faustregel:** Bei jedem ad-hoc Agent-Tool-Aufruf `model:"opus[1m]"` explizit setzen (bei Engine C `model:"sonnet"`).

## Workflows (eigener Pfad)

Das Workflow-Tool nutzt NICHT `CLAUDE_CODE_SUBAGENT_MODEL`, sondern das **Session-Modell**
(`model = opus[1m]`). `agent(prompt)` ohne `opts.model` → erbt opus[1m] (richtig so). `opts.model` NIE
auf sonnet/haiku setzen (ausser bewusst fuer triviale, anfrage-dichte Stage).

## Selbstverbesserung darf das NICHT rueckgaengig machen

`self-improve`/ACE/`env-checker` behandeln als geschuetzten Soll-Zustand: `CLAUDE_CODE_SUBAGENT_MODEL
= "inherit"` (NICHT auf opus[1m] "zuruecksetzen" — zerstoert die Sonnet-5-Research-Ausnahme),
`model: opus[1m]` in jedem eigenen Agent-Frontmatter, `model:"sonnet"` bei Engine-C-Spawns. Abweichung
davon ist ein BUG, keine Optimierung. `env-checker` darf `inherit` NICHT als Fehler melden.

## Was NIEMALS passieren darf

- Subagenten auf Sonnet/Haiku erzwingen (ausser Engine-C-Ausnahme)
- Bei ad-hoc-Spawn `model` weglassen (kein Auto-Opus-Fallback mehr)
- Kleineres Fenster als das groesste verfuegbare
- `inherit` als Fehler melden oder auf opus[1m] "zuruecksetzen"
- Die Sonnet-5-Ausnahme ohne Franks explizite Einzelfall-Entscheidung ausweiten
