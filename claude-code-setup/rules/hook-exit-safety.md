# Hook Exit-Code-Sicherheit: exit 0 darf Hooks NIEMALS kaputt machen (KRITISCH)

> **Direktive #3 (Resilient Bugfixing): Dieser Fehler wurde am 2026-04-04 gemacht und
> resistent gefixt. Er darf NIEMALS wieder auftreten.**

## Problem (was passiert ist)

Am 2026-04-04 wurde `exit 0` blindlings zu 5 Hook-Dateien hinzugefuegt. 3 davon waren
**Dot-Sourced-Bibliotheken** — Dateien die von anderen Hooks per `. "$PSScriptRoot/datei.ps1"`
geladen werden. In einer Dot-Sourced-Datei beendet `exit 0` nicht die Bibliothek,
sondern den **aufrufenden Hook**. Das hat 15 Hooks still abgeschaltet.

## Die 3 Hook-Kategorien (PFLICHT-Wissen vor jedem Hook-Edit)

### Kategorie 1: Dot-Sourced-Bibliotheken
**Erkennung**: Andere Hooks laden sie mit `. "$PSScriptRoot/dateiname.ps1"`
**Exit-Code**: **NIEMALS `exit` verwenden** — weder `exit 0` noch `exit 1`
**Warum**: `exit` in einer Dot-Sourced-Datei beendet den AUFRUFENDEN Prozess
**Aktuelle Bibliotheken**: `hook-log.ps1`, `whiteboard-insert.ps1`
**Pruefung**: `grep -rn '\. "\$PSScriptRoot/' ~/.claude/hooks/*.ps1` zeigt alle Bibliotheken

### Kategorie 2: Blocker/Guard-Hooks
**Erkennung**: Enthalten `exit 1` oder `exit 2` fuer Fehlerfaelle (blockieren Aktionen)
**Exit-Code**: `exit 1` oder `exit 2` bei Verstoss, `exit 0` nur am Ende als Default-Erfolg
**Warum**: Wenn sie IMMER `exit 0` zurueckgeben, koennen sie nichts mehr blockieren
**Aktuelle Blocker**: `config-guard.ps1`, `safety-gate.ps1`, `agent-resource-lock.ps1`

### Kategorie 3: Standalone Non-Blocking Hooks
**Erkennung**: Werden direkt von Claude Code aufgerufen, blockieren nichts
**Exit-Code**: **MUSS `exit 0` am Ende haben**
**Warum**: Ohne `exit 0` kann ein interner Fehler die Session blockieren
**Beispiele**: `session-guard.ps1`, `auto-sync.ps1`, `session-score.ps1`, alle Logging-Hooks

## Pflicht-Ablauf VOR jedem Hook-Edit

Bevor `exit 0` zu einem Hook hinzugefuegt oder entfernt wird:

1. **Ist es eine Bibliothek?** → `grep -rn 'dateiname.ps1' ~/.claude/hooks/` ausfuehren
   - Wenn andere Hooks sie per `. "$PSScriptRoot/..."` laden: **KEIN exit**
2. **Hat der Hook `exit 1` oder `exit 2`?** → Er ist ein Blocker
   - `exit 0` nur als Default-Erfolg am Ende, NICHT als Ersatz fuer die Fehler-Exits
3. **Keins von beidem?** → Standalone-Hook, `exit 0` am Ende ist PFLICHT

## Poka-Yoke: Automatische Pruefung (Stufe 2 — Erzwingung)

Der `hook-exit0-guard.ps1` Pre-Commit-Hook prueft bei jedem Commit von `.ps1`-Dateien:
- Standalone-Hooks MUESSEN `exit 0` haben
- Bibliotheken DUERFEN KEIN `exit` haben
- Blocker-Hooks werden nicht geaendert

## Was NIEMALS passieren darf

- NIEMALS blind `exit 0` zu allen Hook-Dateien hinzufuegen
- NIEMALS `exit` (egal welcher Code) in eine Dot-Sourced-Bibliothek schreiben
- NIEMALS einen Blocker-Hook so aendern, dass er immer `exit 0` zurueckgibt
- NIEMALS Hook-Dateien aendern ohne vorher die Kategorie zu bestimmen
