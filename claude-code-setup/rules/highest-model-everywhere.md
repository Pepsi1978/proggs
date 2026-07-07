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

> **⚡ EINE eng begrenzte Ausnahme (seit 2026-07-01):** Die Web-Research-Eskalationsstufe C
> (der bisherige "Opus-Schwarm", jetzt **Sonnet-5-Schwarm** in `~/.claude/rules/research-strategy.md`
> §4a) laeuft bewusst auf **Sonnet 5** (`model:"sonnet"`) statt Opus — Sonnet 5 hat seit seinem
> Erscheinen (2026-06-30) ebenfalls **natives 1M-Kontext**, die urspruengliche Motivation dieser
> Regel (Kontextfenster) ist fuer diesen einen Anwendungsfall also gleichermassen erfuellt. Diese
> Ausnahme gilt **AUSSCHLIESSLICH** fuer die Engine-C-Research-Spawns — kein anderer Subagent,
> Worker oder Researcher darf daraus "Sonnet ist jetzt auch ok" ableiten. Siehe Mechanismus unten.

---

## Warum (Anti-Absturz + Context Rot)

Der Benutzer hatte staendig Subagent-Abstuerze ("Prompt is too long"). Ein groesseres
Kontextfenster (1M statt 200k bei Sonnet/Haiku) verhindert den Crash. Das 1M-Fenster kostet
laut Doku KEINEN Aufpreis pro Token jenseits 200k — nur Opus-pro-Token statt Sonnet-pro-Token.
Kombiniert mit der Output-Disziplin aus [[subagent-crash-proofing]] (kleine Prompts) bleiben
die Kosten moderat und die Abstuerze verschwinden.

---

## Der Mechanismus (Defense in Depth) — UMGEBAUT am 2026-07-01

**Verifizierter Doku-Fakt:** `CLAUDE_CODE_SUBAGENT_MODEL` ueberschreibt — solange es NICHT auf
`inherit` steht — den per-invocation `model`-Parameter UND das Frontmatter JEDES Subagents,
ausnahmslos (code.claude.com/docs/en/model-config). Genau das machte die Ausnahme fuer die
Research-Eskalation C (Sonnet 5) technisch unmoeglich, solange die Variable global auf `opus[1m]`
stand — ein `model:"sonnet"`-Parameter am Agent-Tool-Aufruf waere wirkungslos gewesen. Deshalb
wurde der Mechanismus umgebaut: von "eine globale Zwangs-Variable" zu "explizites Pinning pro
Agent-Datei" — das Ergebnis ist fuer alle bisherigen Agents IDENTISCH, ermoeglicht aber die eine
gezielte Ausnahme.

| Schicht | Wo | Wirkung |
|---------|-----|---------|
| 1 (primaer) | `~/.claude/settings.json` → `env.CLAUDE_CODE_SUBAGENT_MODEL = "inherit"` | Normale Modell-Aufloesung — KEINE globale Zwangs-Ueberschreibung mehr. Jeder Subagent nutzt jetzt sein eigenes Frontmatter bzw. den explizit uebergebenen `model`-Parameter. |
| 2 | Agent-Frontmatter `model: opus[1m]` (alle 32 eigenen Agents in `~/.claude/agents/*.md`) | Explizit gepinnt — TRAEGT jetzt die Last, die vorher die env-Variable trug. Fuer diese Agents aendert sich dadurch NICHTS am Verhalten. |
| 3 | Ad-hoc Agent-Tool-Aufrufe ohne eigenes Agent-File (`subagent_type:general-purpose` + Prompt) | Erben `inherit` → fallen auf das Session-Modell zurueck, WENN kein `model`-Parameter explizit gesetzt wird. **Deshalb MUSS jeder ad-hoc Subagent, der auf Opus[1m] laufen soll, `model:"opus[1m]"` explizit im Aufruf mitgeben** (z.B. `quality-gate`-Unteraufrufe, spontane Recherche-/Analyse-Subagents). Ausnahme: Engine-C-Research-Spawns bekommen bewusst `model:"sonnet"` (siehe oben). |
| 4 | `~/.claude/settings.json` → `model = "opus[1m]"` bzw. der vom Benutzer gewaehlte Wert | Hauptagent-Modell — unabhaengig von dieser Regel, siehe `session-guard.ps1`. |
| 5 | `config-guard(.ps1/.sh)` + `config-guard-preemptive(.ps1/.sh)` | Allowlist `{sonnet, opus, opus[1m], inherit}` — blockiert Muell/Injection, erlaubt bewussten manuellen Rollback UND den Umbau auf `inherit`. |

**Fuer bereits bestehende Agents aendert sich nichts** — sie liefen vorher via env-Zwang auf
opus[1m], jetzt via eigenes Frontmatter auf opus[1m]. **Neu ist nur:** Ad-hoc-Subagent-Spawns
(ohne eigene Agent-Datei) MUESSEN ab jetzt ihr Modell explizit angeben — es gibt keinen
automatischen Opus-Fallback mehr fuer sie. Bei Zweifel: `model:"opus[1m]"` explizit setzen.

---

## Selbstverbesserungs-Prozesse duerfen das NICHT rueckgaengig machen (KRITISCH)

`self-improve`, der ACE-Curator, `env-checker` und jeder andere Selbstbeobachtungs-/
Optimierungs-Prozess behandeln folgende Werte als GESCHUETZTEN Soll-Zustand:
- `CLAUDE_CODE_SUBAGENT_MODEL = "inherit"` (NICHT mehr `opus[1m]` — das ist der bewusste
  Umbau vom 2026-07-01, siehe Mechanismus oben; ein "Zuruecksetzen" auf `opus[1m]` wuerde die
  Sonnet-5-Research-Ausnahme wieder kaputt machen)
- `model: opus[1m]` in JEDEM der 32 eigenen Agent-Frontmatter (`~/.claude/agents/*.md`)
- `model:"sonnet"` explizit bei den Engine-C-Research-Ad-hoc-Spawns (siehe `research-strategy.md` §4a)

Sie duerfen KEINEN dieser Werte auf Sonnet/Haiku (ausserhalb der einen dokumentierten Ausnahme)
oder ein kleineres Kontextfenster aendern — auch nicht "aus Kostengruenden". Eine Aenderung weg
von einem der drei Soll-Werte ist ein BUG, keine Optimierung. Insbesondere darf `env-checker`
NICHT `CLAUDE_CODE_SUBAGENT_MODEL="inherit"` als "Abweichung von opus[1m]" melden — das ist seit
2026-07-01 der korrekte Zustand.

---

## Was NIEMALS passieren darf

- Subagenten/Researcher/Worker auf Sonnet oder Haiku erzwingen — **ausser** der einen dokumentierten
  Ausnahme (Research-Eskalation C, Sonnet-5-Schwarm)
- Bei einem ad-hoc Agent-Tool-Aufruf (kein eigenes Agent-File) das `model`-Argument weglassen und
  sich auf einen automatischen Opus-Fallback verlassen — seit dem Umbau auf `inherit` gibt es den
  nicht mehr; explizit `model:"opus[1m]"` (oder bei Engine C `model:"sonnet"`) mitgeben
- Ein kleineres Kontextfenster als das groesste verfuegbare verwenden
- `self-improve`/ACE/Hooks `CLAUDE_CODE_SUBAGENT_MODEL="inherit"` als Fehler melden oder auf
  `opus[1m]` "zuruecksetzen" — das wuerde die Sonnet-5-Research-Ausnahme wieder zerstoeren
- Eines der 32 Agent-Frontmatter von `model: opus[1m]` wegaendern, ohne dass der Benutzer das
  explizit fuer genau diesen einen Agent verlangt hat
- Die Sonnet-5-Ausnahme auf andere Subagents/Worker/Researcher ausweiten, ohne dass der Benutzer
  das explizit fuer den jeweiligen Anwendungsfall entscheidet
- Fremde Plugin-Agents editieren, um Sonnet zu entfernen (unnoetig — sie erben nun `model:sonnet`/
  `haiku` aus ihrem eigenen Frontmatter, da `inherit` nicht mehr zwingend ueberschreibt; falls ein
  fremder Plugin-Agent auf Opus[1m] laufen soll, `CLAUDE_CODE_SUBAGENT_MODEL` NICHT global zurueck
  auf `opus[1m]` stellen — stattdessen den Einzelfall pruefen)

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
| `~/.claude/rules/research-strategy.md` §4a | Die eine dokumentierte Ausnahme (Sonnet-5-Schwarm) im Detail: Modell-Mechanik, Effort, Geltungsbereich |
| Memory `feedback_subagent_model_opus_1m` | Die operative Notiz zu dieser Policy (Update 2026-07-01 noetig: Mechanismus jetzt inherit+Pinning statt globaler Zwang) |
