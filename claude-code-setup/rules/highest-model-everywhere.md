# Hoechstes Modell + groesstes Kontextfenster ueberall (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-01. Gilt AUTOMATISCH in JEDER Session,
> fuer JEDEN Subagent, Worker, Researcher und Agent — ausnahmslos. Ergaenzt die Anti-Absturz-
> Strategie aus [[subagent-crash-proofing]] und das Verlustfrei-Prinzip aus [[lossless-context-principle]].

---

## Grundregel

**Jeder Subagent, Worker, Researcher und Agent laeuft IMMER auf dem hoechsten verfuegbaren
Opus-Modell mit dem groessten verfuegbaren Kontextfenster.** Aktuell: `opus[1m]` (Opus 4.8,
1 Million Token). Sobald ein neueres/hoeheres Opus erscheint (z.B. 4.9) oder ein groesseres
Kontextfenster verfuegbar wird, gilt automatisch DAS — `opus` zeigt laut Doku immer auf das
neueste Opus, und `[1m]` (bzw. ein groesseres Suffix) auf das groesste Kontextfenster.

**NIEMALS Sonnet oder Haiku fuer Subagenten erzwingen.** Geschwindigkeit/Kosten sind NICHT der
Massstab — Stabilitaet und Intelligenz sind es. Der Benutzer nimmt die Mehrkosten bewusst in Kauf.

---

## Warum (Anti-Absturz + Context Rot)

Der Benutzer hatte staendig Subagent-Abstuerze ("Prompt is too long"). Ein groesseres
Kontextfenster (1M statt 200k bei Sonnet/Haiku) verhindert den Crash. Das 1M-Fenster kostet
laut Doku KEINEN Aufpreis pro Token jenseits 200k — nur Opus-pro-Token statt Sonnet-pro-Token.
Kombiniert mit der Output-Disziplin aus [[subagent-crash-proofing]] (kleine Prompts) bleiben
die Kosten moderat und die Abstuerze verschwinden.

---

## Der Mechanismus (Defense in Depth)

| Schicht | Wo | Wirkung |
|---------|-----|---------|
| 1 (primaer) | `~/.claude/settings.json` → `env.CLAUDE_CODE_SUBAGENT_MODEL = "opus[1m]"` | Ueberschreibt das `model:`-Frontmatter JEDES Subagents — auch fremder Plugin-Agents. EINE Variable steuert alles. |
| 2 | `~/.claude/settings.json` → `model = "opus[1m]"` | Hauptagent ebenfalls auf hoechstem Modell + 1M. |
| 3 | Agent-Frontmatter `model: opus` (eigene Agents) | Fallback, falls die env-Variable je auf `inherit` faellt. |
| 4 | `config-guard(.ps1/.sh)` + `config-guard-preemptive(.ps1/.sh)` | Allowlist `{sonnet, opus, opus[1m]}` — blockiert Muell/Injection, erlaubt bewussten manuellen Rollback. |
| 5 | `session-guard.ps1` | Setzt das Hauptmodell `model` bei echtem Neustart auf `opus[1m]` zurueck. |

**Verifizierter Doku-Fakt:** `CLAUDE_CODE_SUBAGENT_MODEL` ueberschreibt den per-invocation
`model`-Parameter UND das Frontmatter jedes Subagents (code.claude.com/docs/en/model-config).
Deshalb laufen auch fremde Plugin-Agents (superpowers, context-engineering-kit etc.) mit
`model: sonnet`/`haiku` faktisch auf `opus[1m]` — fremder Code muss NICHT editiert werden.

---

## Selbstverbesserungs-Prozesse duerfen das NICHT rueckgaengig machen (KRITISCH)

`self-improve`, der ACE-Curator, `env-checker` und jeder andere Selbstbeobachtungs-/
Optimierungs-Prozess behandeln `CLAUDE_CODE_SUBAGENT_MODEL = opus[1m]` und `model = opus[1m]`
als GESCHUETZTEN Soll-Wert. Sie duerfen ihn NIEMALS auf Sonnet/Haiku oder ein kleineres
Kontextfenster aendern — auch nicht "aus Kostengruenden". Eine Aenderung weg von einem
Opus-Wert ist ein BUG, kein Optimierung.

---

## Was NIEMALS passieren darf

- Subagenten/Researcher/Worker auf Sonnet oder Haiku erzwingen
- `CLAUDE_CODE_SUBAGENT_MODEL` auf einen Nicht-Opus-Wert setzen (ausser bewusster, manueller Test durch den Benutzer)
- Ein kleineres Kontextfenster als das groesste verfuegbare verwenden
- `self-improve`/ACE/Hooks den Wert "korrigieren" lassen, weil ein veralteter Soll-Wert (z.B. `sonnet`) irgendwo steht
- Fremde Plugin-Agents editieren, um Sonnet zu entfernen (unnoetig — die env-Variable ueberschreibt sie; Edits werden bei Updates verworfen)

---

## Workflows (eigener Modell-Pfad — wichtig)

Das Workflow-Tool (dynamische Workflows, viele Agenten pro Run) verwendet NICHT
`CLAUDE_CODE_SUBAGENT_MODEL`, sondern das **Session-Modell** (`model` in settings.json).
Da `model = opus[1m]` ist (durch `session-guard` abgesichert), laufen ALLE Workflow-Agenten
automatisch auf Opus 4.8 mit 1M Kontext — OHNE extra Einstellung. Belegt durch die offizielle
Doku (code.claude.com/docs/en/workflows): "Every agent in a workflow uses your session's model
unless the script routes a stage to a different one."

Konsequenz fuer selbst geschriebene Workflows:
- `agent(prompt)` ohne `opts.model` → erbt das Session-Modell (opus[1m]). RICHTIG so.
- `opts.model` NIEMALS auf ein kleineres Modell (sonnet/haiku) setzen — ausser bewusst fuer eine
  triviale, anfrage-dichte Stage, und nur mit Begruendung.
- Concurrency-Fakt: max 16 Agenten GLEICHZEITIG (CPU-abhaengig), bis 1000 total pro Run. "Hunderte
  gleichzeitig" stimmt nicht — sie laufen in 16er-Slots ab. Wer auf opus[1m] laeuft, stuerzt dabei nicht ab.

## Zusammenspiel

| Regel | Zusammenspiel |
|-------|--------------|
| [[subagent-crash-proofing]] | Output-Disziplin haelt die Prompts klein — diese Regel gibt das grosse Fenster als Sicherheitsnetz |
| [[lossless-context-principle]] | Grosses Fenster ersetzt NICHT die verlustfreie Reduktion — beides zusammen |
| Memory `feedback_subagent_model_opus_1m` | Die operative Notiz zu dieser Policy |
