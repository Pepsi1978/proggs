# ACE — Geschuetzte Zonen und Selbstverbessernde Regeln (KRITISCH)

> **Basierend auf ACE (Agentic Context Engineering), Stanford/UC Berkeley, arXiv 2510.04618**
> **Direktive #1 (Superintelligenz): CLAUDE.md wird zum lebenden Dokument**
> **Direktive #3 (Resilient Bugfixing): Defense in Depth fuer Regelaenderungen**

## Was ist ACE?

ACE ist ein System das die Regeln der Programmierumgebung automatisch verbessert —
basierend auf echten Session-Daten (Fehler, Korrekturen, Scores), nicht auf Vermutungen.
Drei Rollen arbeiten zusammen:
- **Generator**: Analysiert was in Sessions passiert ist
- **Reflector**: Prueft ob Muster wiederholt auftraten (kein Einzelfall)
- **Curator**: Formuliert konkrete Regelverbesserungen

## Geschuetzte Zonen (ABSOLUT UNANTASTBAR)

Diese Bereiche duerfen von KEINEM Agenten, Skill, Hook oder Prozess automatisch
geaendert werden — auch nicht vom ACE-Curator:

### In CLAUDE.md
| Zone | Markierung | Inhalt |
|------|-----------|--------|
| Franks Begruessung | `ACE-PROTECTED-ZONE-START/END: Franks Begruessung` | Zeilen 1-23 |
| Die 3 Direktiven | `ACE-PROTECTED-ZONE-START/END: Die 3 Direktiven` | Superintelligenz, Selbstbeobachtung, Resilient Bugfixing |

### In ~/.claude/rules/
| Datei | Warum geschuetzt |
|-------|-----------------|
| `superintelligence.md` | Direktive #1 — hoechste Prioritaet |
| `self-observation.md` | Direktive #2 — Selbstbeobachtung |
| `resilient-bugfixing.md` | Direktive #3 — Resilient Bugfixing |
| `bypass-permissions-permanent.md` | Sicherheitskritisch — bypassPermissions |

## Evolvable Zonen (duerfen verbessert werden)

| Bereich | Markierung | Wer darf aendern |
|---------|-----------|-----------------|
| Operative Regeln in CLAUDE.md | `ACE-EVOLVABLE-ZONE-START/END` | ACE-Curator (mit Benutzer-Bestaetigung) |
| Regeln in `~/.claude/rules/*.md` | Keine Markierung noetig | ACE-Curator (ausser gesperrte Dateien) |
| Agenten in `~/.claude/agents/*.md` | Keine Markierung noetig | ACE-Curator (mit Benutzer-Bestaetigung) |

## Defense in Depth (4 Schichten)

| Schicht | Mechanismus | Was es schuetzt |
|---------|-------------|----------------|
| 1 | HTML-Kommentar-Marker in CLAUDE.md | Definiert sichtbar wo die Grenzen sind |
| 2 | Sperrliste im ACE-Curator-Agent | Agent weiss welche Dateien er nicht anfassen darf |
| 3 | Benutzer-Bestaetigung fuer JEDEN Vorschlag | Kein automatisches Anwenden ohne Frank |
| 4 | Git-Commit fuer jede Aenderung | Jederzeit rueckgaengig machbar |

## Wann ACE laeuft

- **Manuell**: "ACE starten", "Regeln verbessern"
- **Via /self-improve**: Als Stufe 7 ("ACE-Regeloptimierung")
- **Empfohlen**: Am Ende von Sessions mit >20 Tool-Calls

## Was NIEMALS passieren darf

- ❌ ACE aendert eine geschuetzte Zone
- ❌ ACE aendert Regeln ohne Benutzer-Bestaetigung
- ❌ ACE erstellt Regeln basierend auf Einzelfaellen (min. 2x aufgetreten)
- ❌ ACE macht bestehende Regeln komplizierter ohne messbaren Mehrwert
- ❌ ACE loescht bestehende Regeln (nur erweitern oder neue erstellen)
- ❌ Die ACE-PROTECTED-ZONE-Marker werden entfernt oder verschoben
