# Altlasten — teuer erkaufte Operativ-Lektionen (verlustfrei aus v5.x gerettet)

> Der neue self-improve nimmt den alten Skill NICHT als Vorlage. Aber jede dieser Lektionen
> entstand aus einem echten Vorfall. Sie zu vergessen hieße, den Fehler erneut zu machen
> (Direktive #3). Darum bleiben sie hier — kurz, abrufbar, nicht im Hot-Context.
> Der Kreislauf verweist gezielt hierauf (A1, A2, A3, A6).

---

## A1 — Merge-Konflikt-Pre-Check (ZUERST, vor allem anderen)
Vor dem Lesen von MEMORY.md auf Konflikt-Marker prüfen — ein 17 Tage ungelöster Konflikt verfälschte einmal das ganze System.
```bash
grep -lE '<<<<<<< (Updated upstream|HEAD)|>>>>>>> Stashed changes|\|\|\|\|\|\|\| Stash base' \
  ~/proggs/.claude/agent-memory/shared/MEMORY.md ~/proggs/CLAUDE.md ~/proggs/.mcp.json 2>/dev/null
```
Bei Treffer: SOFORT abbrechen, laut warnen. Marker müssen manuell aufgelöst werden, bevor der Lauf weitergeht — sonst arbeitet alles auf verfälschten Daten.

## A2 — Cross-Platform-Sync (jede eigene Änderung wird plattformübergreifend wirksam)
Alle Änderungen, die der Skill macht, nach `~/proggs/claude-code-setup/` spiegeln, damit Windows + macOS beim nächsten Start dieselbe Verbesserung erhalten:
- Hooks (`.ps1` + `.sh` + ggf. `.ts`) → `claude-code-setup/hooks/` — **beide** Plattform-Varianten ändern, nie nur eine.
- Agents/Skills/Commands (inkl. `self-improve.md` + `self-improve-ref/`) → `claude-code-setup/commands/` bzw. `agents/`.
- Rules → `claude-code-setup/rules/`; CLAUDE.md → `~/proggs/CLAUDE.md`.
- settings: die 3-Dateien-Regel beachten (`settings-reference.json`, macOS-`settings.json`, `settings.local.json`) — nie nur eine aktualisieren.
- Whiteboard (MEMORY.md) → `claude-code-setup/agent-memory/shared/`.
**Funktionale Parität (nicht nur Existenz):** Für jedes Hook-Paar `.sh`/`.ps1` prüfen, dass beide *dasselbe* tun (gleiche Writes, gleiche Outputs) — ein Paar lief monatelang unterschiedlich, ohne dass es auffiel.
NICHT syncen (maschinenspezifisch): `settings.json` direkt, Plugin-Caches, Session-Transcripts.

## A3 — Shell/Terminal-Updates IMMER ZULETZT
Updates von PowerShell, Git, Git Bash, Node, npm, Bun, Deno, Python, Claude-Code-CLI zerstören ALLE offenen Terminal-Fenster und killen laufende Prozesse. Reihenfolge ist Pflicht:
1. ALLE anderen Aufgaben abschließen → 2. committen+pushen → 3. Frank warnen ("offene Terminal-Fenster werden geschlossen") → 4. explizite Bestätigung abwarten (AskUserQuestion) → 5. erst dann updaten, einzeln, mit Statusmeldung → 6. danach PATH verifizieren (`path-verify.sh/.ps1 --fix`).
Lehnt Frank ab: in MEMORY.md mit Status DEFERRED notieren. Nie ohne Bestätigung, nie während anderer Arbeit. (Vorfall 2026-03-19: PowerShell-Update mitten im Lauf zerstörte stundenlange Arbeit.)

## A4 — Arbeitsverzeichnis & Single-Repo
Nur in `~/proggs` arbeiten (alles geht nach `Pepsi1978/proggs`). `~/Codex` ist GESPERRT — dort niemals lesen/schreiben/cd (gehört dem Codex-CLI; Überschneidung = Datenverlust). Keine neuen Repos erstellen.

## A5 — Plattform-Detection & Befehle
`uname -s` (Darwin=macOS, MINGW*=Windows). Nie `brew` auf Windows, nie `winget` auf macOS. Windows: komplexe Befehle als temporäre `.ps1`. Python immer mit `encoding='utf-8'` (Windows-cp1252-Falle). JSON nie mit `sed`/`awk` bearbeiten — Edit-Tool oder Python `json`, danach validieren.

## A6 — Commit-Disziplin (parallele Sessions)
Frank arbeitet mit 4–5 gleichzeitigen Sessions am selben Repo. Darum: nur eigene Dateien namentlich stagen (nie `git add -A`/`.`). Vor jedem Push `git fetch origin && git rebase origin/main`. Bei Rejection einfach `fetch+rebase+push` wiederholen — niemals `--force` oder `reset --hard`. Commit-Format `#NNN - Beschreibung`. Vor Build: erst committen+pushen.

## A7 — Hook-Sicherheit
Vor jedem Hook-Edit klären, ob die Datei dot-sourced ist (dann NIE `exit` — beendet den Aufrufer), ein Blocker/Guard (gezielt `exit 1/2`, sonst `exit 0` am Ende) oder Standalone (`exit 0` am Ende Pflicht). Event-spezifische Hooks (SubagentStop/PostToolUse) brauchen einen Input-Guard (Pflichtfeld leer → `exit 0` ohne Side-Effects), AUSSER reine Kontext-Injection-Hooks ohne Side-Effects. Keine `type: "prompt"`-Hooks bei SessionStart.

## A8 — Subagenten absturzsicher
Subagenten haben KEIN Auto-Compact. Große Dateien nie komplett ins LLM laden (Python-IO/Grep/Ranges), enger Scope, Ergebnisse in Dateien auslagern, schlanke tools-Whitelist. Bei Worker-Crash ("Prompt is too long"): Orchestrator-Resume — Checkpoint lesen, kleiner + diszipliniert neu spawnen, nie die Aufgabe aufgeben.

## A9 — Secrets nie ins Repo
API-Keys/Tokens/Keystores leben in `$HOME/SK/<projekt>/`, nie im Repo. Beim Kopieren von Settings ins Repo sofort redaktieren, bevor staged wird. Keine `.gitignore`-Ausnahmen wie `!debug/google-services.json`.

## A10 — Geschützte Zonen respektieren
Die 3 Direktiven (`superintelligence.md`, `self-observation.md`, `resilient-bugfixing.md`), `bypass-permissions-permanent.md`, `highest-model-everywhere.md` und Franks Begrüßung sind ABSOLUT unantastbar. Modell-Policy: `model`/`SUBAGENT_MODEL` = `opus[1m]` — self-improve darf das NIE auf Sonnet/Haiku oder ein kleineres Fenster ändern (das wäre ein Bug). `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` bleibt 100, nie unter 85. `defaultMode: bypassPermissions` bleibt.
