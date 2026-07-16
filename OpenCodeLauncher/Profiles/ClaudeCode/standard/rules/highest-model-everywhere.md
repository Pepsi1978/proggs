# Hoechstes Modell + groesstes Kontextfenster ueberall (KRITISCH)

## Grundregel
Jeder Subagent/Worker/Researcher laeuft IMMER auf `opus[1m]` (Opus 4.8, 1M Token); Hoeheres erscheint ->
gilt automatisch. NIEMALS Sonnet/Haiku erzwingen (1M verhindert "Prompt is too long"-Crashes, kein
Aufpreis jenseits 200k); Massstab: Stabilitaet, nicht Kosten.

**Ausnahme (2026-07-01):** Web-Research-Eskalationsstufe C (Sonnet-5-Schwarm, `research-strategy.md`
Par.4a) laeuft bewusst auf Sonnet 5 (`model:"sonnet"`, natives 1M). NUR Engine-C-Spawns.

## Mechanismus (2026-07-01)
`settings.json` -> `env.CLAUDE_CODE_SUBAGENT_MODEL = "inherit"` (keine globale Ueberschreibung). Jeder der
32 eigenen Agents pinnt `model: opus[1m]` im Frontmatter. Ad-hoc-Spawns (`general-purpose`) erben
`inherit` -> **MUESSEN `model:"opus[1m]"` explizit mitgeben** (Ausnahme Engine C = `model:"sonnet"`).
Session-Modell = `opus[1m]`. `config-guard` Allowlist `{sonnet, opus, opus[1m], inherit}`.

## Workflows
Workflow-Tool nutzt das Session-Modell `opus[1m]`; `agent()` ohne `opts.model` erbt es; `opts.model` NIE
sonnet/haiku (ausser triviale Stage).

## Was NIEMALS
- Subagenten auf Sonnet/Haiku erzwingen (ausser Engine C); bei ad-hoc-Spawn `model` weglassen (kein
  Auto-Opus-Fallback); kleineres Fenster als das groesste; `inherit` als Fehler melden oder auf opus[1m]
  "zuruecksetzen" (zerstoert die Sonnet-5-Ausnahme); die Sonnet-5-Ausnahme ohne Franks OK ausweiten.
