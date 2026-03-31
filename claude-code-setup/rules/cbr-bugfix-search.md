# Case-Based Reasoning: Bug-Datenbank durchsuchen VOR dem Debuggen (KRITISCH)

> Quelle: Superintelligenz Finding 6 — Case-Based Reasoning fuer LLM-Agenten
> (arXiv 2504.06943)
> Direktive: #2 Selbstbeobachtung + #3 Resilient Bugfixing

## Regel

Bevor ein neuer Fehler debuggt wird, MUSS zuerst die Bug-Datenbank
(`~/proggs/.claude/agent-memory/shared/bug-cases.jsonl`) nach aehnlichen
Faellen durchsucht werden.

## Warum (Arzt-Analogie)

Ein erfahrener Arzt denkt bei Symptomen: "Das hatte ich schon mal — da war
die Ursache X." Statt jedes Mal von Null zu diagnostizieren, greift er auf
seine Erfahrung zurueck. Das nennt sich Case-Based Reasoning (CBR).

Genauso hier: Jeder gefixte Bug wird als "Fall" gespeichert. Beim naechsten
aehnlichen Fehler durchsucht Claude die Datenbank und findet den alten Fall
— mit Root Cause und Loesung. Das spart Debugging-Zeit und verhindert dass
der gleiche Fehler mehrmals von Grund auf analysiert werden muss.

## Die 4 CBR-Phasen

1. **Retrieve**: Bug-Datenbank durchsuchen (Grep nach Symptom/Fehlermeldung)
2. **Reuse**: Den alten Fix als ersten Loesungsansatz verwenden
3. **Revise**: Falls der alte Fix nicht 1:1 passt, anpassen
4. **Retain**: Den NEUEN Fall (mit Anpassungen) in die Datenbank eintragen

## Datenbank-Format (bug-cases.jsonl)

Jede Zeile ist ein JSON-Objekt:
```json
{"date":"2026-03-31","symptom":"Push rejected: non-fast-forward","root_cause":"Kein fetch+rebase vor push","fix":"git fetch origin && git rebase origin/main vor jedem push","files":["beliebig"],"tags":["git","push","rebase"],"severity":"hoch"}
```

## Wann die Datenbank durchsuchen

- Bei JEDEM Build-Fehler
- Bei JEDEM fehlgeschlagenen Befehl
- Bei JEDER Fehlermeldung die nicht sofort klar ist
- Suchstrategie: `grep` nach Schluesselwoertern aus der Fehlermeldung

## Wann in die Datenbank schreiben

- Nach JEDEM Bugfix der laenger als 5 Minuten gedauert hat
- Nach JEDEM Fehler der zum zweiten Mal aufgetreten ist (ALARM!)
- Mindestens: symptom, root_cause, fix, tags
- Optional: files, severity, prevention_rule

## Zusammenspiel mit anderen Systemen

- **Resilient Bugfixing** (`resilient-bugfixing.md`): CBR formalisiert den
  "Verwandte Fehlerquellen suchen"-Schritt — die Datenbank IST die Suche.
- **Error-Antigens** (`error-antigens.jsonl`): Die Antigen-Datei nutzt die
  Bug-Cases als Grundlage fuer automatische Praevention.
- **Whiteboard**: Schwere/wiederkehrende Bugs werden ZUSAETZLICH im Whiteboard
  unter "Offene Fehler" eingetragen.
