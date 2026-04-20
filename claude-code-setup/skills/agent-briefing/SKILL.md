---
name: agent-briefing
description: Generiere ein kompaktes Situational-Awareness-Briefing (max 10 Zeilen) das als erster Block fuer jeden Agent-Prompt injiziert werden soll. Nutze DIESEN Skill IMMER wenn du einen Subagent mit projektspezifischer Aufgabe spawnen willst — er liefert dir dynamischen Kontext (Branch, letzte Commits, offene Fehler, Datei-Ownership) den du in den Agent-Prompt kopieren kannst. Inspiriert vom Luftfahrt-Crew-Resource-Management: Gemeinsames mentales Modell fuer alle Beteiligten.
---

# Agent-Briefing Skill — Luftfahrt-CRM fuer Subagents

> Quelle: R6 Creative Research (2026-04-20) — Crew Resource Management hat Flugunfaelle
> um ~70% reduziert, nicht durch bessere Piloten, sondern durch strukturierte Information.

Agents erben NICHT die Konversations-Historie. Sie starten "blind" in das Projekt hinein.
Dieser Skill liefert einen standardisierten Briefing-Block den du als ERSTEN Abschnitt
in jeden Subagent-Prompt kopierst.

## Wann verwenden

**IMMER** bei:
- Spawning von architect, debugger, coder, tester, reviewer, optimizer Agents
- Komplexen Aufgaben (>2 Dateien) die Projekt-Wissen brauchen
- Nach laengeren Sessions wenn du unsicher bist welcher Kontext dem Agent hilft

**NICHT noetig** bei:
- Reinen Recherche-Agents (WebSearch) ohne Code-Bezug
- Nemo-Agent (nutzt nur Allgemeinwissen)
- env-checker (scannt die Umgebung selbst)

## Die Ausfuehrung

Fuehre diese Befehle aus und nutze die Outputs um das Briefing-Template zu fuellen:

```bash
# 1. Aktueller Branch
git branch --show-current

# 2. Letzte 3 Commits (oneline)
git log --oneline -3

# 3. Geaenderte Dateien in dieser Session (uncommitted)
git status --short

# 4. Anzahl offener Fehler im Whiteboard
grep -c "^### .*OFFEN" ~/proggs/.claude/agent-memory/shared/MEMORY.md 2>/dev/null

# 5. Aktuelle Aufgabe des Haupt-Agenten (aus current-spec.md falls vorhanden)
test -f /tmp/current-spec.md && head -5 /tmp/current-spec.md
test -f "$env:TEMP/current-spec.md" && head -5 "$env:TEMP/current-spec.md"
```

## Das Briefing-Template (10 Zeilen max)

Formatiere das Ergebnis EXAKT so, und kopiere es als ERSTEN Block in den Agent-Prompt:

```
## CRM-Briefing (Situational Awareness)
- Repository: Pepsi1978/proggs (~/proggs/)
- Branch: [Output von git branch --show-current]
- Letzte 3 Commits: [Output von git log --oneline -3, komma-getrennt]
- Unstaged Dateien: [Anzahl uncommitted] ([kritische Dateinamen falls wenige])
- Offene Fehler im Whiteboard: [Zahl]
- Datei-Ownership fuer DIESE Aufgabe: [Liste der Dateien die du (der Subagent) bearbeiten darfst]
- Aus dem Scope: [Dateien die du NICHT anfassen darfst]
- Plattform: [Windows / macOS — via uname -s]
- Zentrales Whiteboard lesen: ~/proggs/.claude/agent-memory/shared/MEMORY.md
- Sentinel schreiben am Ende: $env:TEMP/agent-writeback-[agent-name].json (Windows) oder /tmp/agent-writeback-[agent-name].json (Unix)
```

## Regeln

### Dynamisch, nicht statisch
**NIEMALS** hardcodieren. Jeder Briefing-Block muss frisch generiert werden — Commits,
Branch-Name und offene Fehler aendern sich staendig.

### Kurz, nicht verbose
Max 10 Zeilen. Wenn das Briefing laenger wird: kuerze Commit-Messages oder Dateinamen.
Der Agent muss die Info in <10 Sekunden erfassen koennen.

### Datei-Ownership ist heilig
Bei parallelen Agents MUSS jeder Agent explizit wissen welche Dateien SEINE sind und
welche TABU sind. Ohne das gibt es Merge-Konflikte und Edit-Kollisionen.

### Sprache: Deutsch
Das Briefing wird auf Deutsch geschrieben (laut CLAUDE.md-Konvention). Tool-Namen und
Code-Identifier bleiben Englisch.

## Beispiel (vollstaendig)

Angenommen der Haupt-Agent will einen `coder` spawnen der `DashboardScreen.kt` erweitert.
Dann fuehrt er erst diesen Skill aus, generiert den Briefing-Block, und haengt ihn
an seinen Agent-Prompt:

```
## CRM-Briefing (Situational Awareness)
- Repository: Pepsi1978/proggs (~/proggs/)
- Branch: main
- Letzte 3 Commits: #1559 Self-improve thorough fix, #1558 Debug session hooks, #1557 Pin effortLevel
- Unstaged Dateien: 3 (CLAUDE.md, DashboardScreen.kt, strings.xml)
- Offene Fehler im Whiteboard: 2
- Datei-Ownership fuer DIESE Aufgabe: BestJournalAndroid/app/src/main/java/.../DashboardScreen.kt
- Aus dem Scope: NICHT strings.xml aendern (das macht ein paralleler Agent)
- Plattform: Windows (MINGW64)
- Zentrales Whiteboard lesen: ~/proggs/.claude/agent-memory/shared/MEMORY.md
- Sentinel schreiben: $env:TEMP/agent-writeback-coder.json

[danach der eigentliche Task-Prompt]
```

## Warum das funktioniert

**Ohne Briefing:** Der Agent muss 3-5 Tool-Calls machen nur um sich zu orientieren
(Git-State, Whiteboard, Projektstruktur). Das sind 10.000+ Token Overhead pro Agent.

**Mit Briefing:** Der Agent hat die wichtigsten 10 Fakten in <200 Tokens — und kann
direkt mit der echten Aufgabe beginnen.

**Compound-Effekt:** Bei 5 parallelen Agents werden 50.000+ Token gespart — der Benutzer
bezahlt fuer Ergebnisse, nicht fuer Orientierung.

## Was NIEMALS passieren darf

- ❌ Briefing statisch hardcodieren (wird sofort veraltet)
- ❌ Briefing ueberspringen "weil der Agent das eh lesen kann" (doppelte Arbeit)
- ❌ Sensitive Daten im Briefing (Tokens, Secrets) — wenn gefunden: ERROR, nicht einbetten
- ❌ Briefing laenger als 10 Zeilen (dann wird's ignoriert)
- ❌ Ownership-Zeile weglassen bei parallelen Agents (Datei-Konflikt-Garantie)
