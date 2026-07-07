# Codified Context: Dreigliedrige Kontext-Infrastruktur (KRITISCH)

> Basierend auf Codified Context (arXiv 2602.20478): Ein Team baute ein 108.000-Zeilen-System
> mit Gemini CLI und entdeckte, dass eine einzige Gemini.md bei grossen Projekten nicht skaliert.
> Direktive: #1 Superintelligenz

## Die 3 Schichten unserer Kontext-Infrastruktur

Unser System hat bereits eine dreigliedrige Architektur — diese Regel formalisiert sie
und stellt sicher, dass sie bewusst gepflegt wird statt zufaellig zu wachsen.

### Schicht 1: Verfassung (Gemini.md)
**Datei**: `~/proggs/Gemini.md` (einzige Kopie — Duplikat ~/Gemini.md wurde am 2026-04-04 entfernt)
**Zweck**: Kern-Regeln die IMMER geladen werden — die "Verfassung" des Systems.
**Inhalt**: Die 3 Direktiven, Franks Begruessung, grundlegende Arbeitsregeln.
**Groesse**: So kompakt wie moeglich. Jede Zeile muss ihren Platz rechtfertigen.

**Abruf-Hooks**: Gemini.md enthaelt Verweise auf spezialisierte Regeln:
- "Vollstaendige Regel: `~/.Gemini/rules/self-observation.md`"
- "Fuer Android-spezifische Details siehe `android-phase-trigger.md` → laedt `references/development-phases.md`"
Diese Verweise sind wie ein Bibliothekskatalog — sie sagen Gemini WO das
Detailwissen liegt, ohne es direkt in Gemini.md zu kopieren.

### Schicht 2: Domain-Regeln (~/.Gemini/rules/)
**Pfad**: `~/.Gemini/rules/*.md`
**Zweck**: Spezialisiertes Wissen pro Themengebiet — wird automatisch geladen.
**Inhalt**: 30+ Regeln zu Git, Android, Hooks, Sicherheit, Qualitaet, Workflows.
**Vorteil**: Jede Regel ist eigenstaendig und kann unabhaengig verbessert werden
(durch ACE-Curator oder manuell).

**Namenskonvention**: `[thema]-[spezifik].md`
- `fetch-rebase-before-push.md` — Git-Workflow
- `android-phase-trigger.md` — Android-Trigger (laedt Reference on-demand)
- `experience-and-trajectory.md` — Erfahrungsspeicher + Trajectories
- `ace-protected-zones.md` — ACE-Schutz

### Schicht 3: On-Demand-Wissen (Agents + Speicher)
**Pfade**:
- `~/.Gemini/agents/*.md` — Spezialisierte Agenten (werden NUR bei Bedarf geladen)
- `.Gemini/agent-memory/shared/MEMORY.md` — Zentrales Whiteboard
- `.Gemini/agent-memory/shared/*.jsonl` — Strukturierte Daten (Experience Store, Bug-Cases, Trajectories)
- `~/proggs/LEARNINGS.md` — Projektuebergreifende technische Learnings

**Zweck**: Wissen das nur geladen wird wenn es gebraucht wird.
Ein Agent wird nur gestartet wenn seine Expertise relevant ist.
JSONL-Dateien werden nur per grep durchsucht wenn ein Fehler oder eine Aufgabe es erfordert.

## Warum 3 Schichten statt 1?

| Problem mit 1 Datei | Loesung mit 3 Schichten |
|---------------------|------------------------|
| Gemini.md waechst endlos | Kern-Regeln bleiben kompakt, Details in Rules |
| Alles wird immer geladen | On-Demand-Wissen wird NUR bei Bedarf gelesen |
| Aenderungen betreffen alles | Jede Rule-Datei kann isoliert geaendert werden |
| Kontext-Fenster voll | Rules werden zwar automatisch geladen, aber Agents nur bei Bedarf |
| Schwer zu warten | Jede Datei hat einen klaren Zweck |

## Regeln fuer die Pflege der 3 Schichten

### Wann gehoert etwas in Gemini.md (Schicht 1)?
- Wenn es in JEDER Session relevant ist (Direktiven, Grundregeln)
- Wenn es NICHT in eine eigenstaendige Rule-Datei passt
- Wenn es Gemini.md-Verweise auf andere Dateien braucht (Abruf-Hooks)
- **NIEMALS**: Detaillierte technische Anleitungen (die gehoeren in Rules)

### Wann gehoert etwas in ~/.Gemini/rules/ (Schicht 2)?
- Wenn es ein spezifisches Thema betrifft (Git, Android, Hooks, etc.)
- Wenn es als eigenstaendige Datei verstaendlich ist
- Wenn es durch ACE-Curator verbessert werden koennte
- **Die meisten neuen Regeln gehoeren hierhin**

### Wann gehoert etwas in Agents/Speicher (Schicht 3)?
- Wenn es NUR bei Bedarf gebraucht wird (Agenten)
- Wenn es strukturierte Daten sind (JSONL-Dateien)
- Wenn es projektspezifisches Wissen ist (LEARNINGS.md)
- Wenn es fluechtiges Wissen ist (Whiteboard, Session-Scores)

## Skalierbarkeits-Warnsignal

Wenn Gemini.md ueber 500 Zeilen waechst, ist es Zeit fuer eine Extraktion:
1. Identifiziere Bloecke die als eigenstaendige Rule-Datei funktionieren wuerden
2. Erstelle die Rule-Datei in `~/.Gemini/rules/`
3. Ersetze den Block in Gemini.md durch einen Abruf-Hook:
   "Vollstaendige Regel: `~/.Gemini/rules/[name].md`"
4. **ACE-Curator respektiert die geschuetzten Zonen** — nur die Evolvable Zone anpassen

Aktueller Stand: Gemini.md hat ~420 Zeilen (davon ~110 geschuetzt).
Das ist noch im gruenen Bereich, aber nahe am Warnwert.

## Defense in Depth (Direktive 3)

| Schicht | Mechanismus | Was es schuetzt |
|---------|-------------|----------------|
| 1 | ACE-PROTECTED-ZONE Marker | Kern-Inhalte koennen nicht automatisch geaendert werden |
| 2 | Git-Versionierung | Gemini.md im Repo, jede Aenderung rueckgaengig machbar |
| 3 | gemini-setup/ Backup | Rules und Agents werden ins Repo kopiert |
| 4 | Diese Regel (on-demand) | Gemini weiss WIE die Architektur funktioniert |

## Was NIEMALS passieren darf

- ❌ Detaillierte Anleitungen direkt in Gemini.md schreiben statt in Rules
- ❌ Rules erstellen die nur in Gemini.md Sinn ergeben (keine Eigenstaendigkeit)
- ❌ On-Demand-Wissen (Agents, JSONL) immer laden statt nur bei Bedarf
- ❌ Gemini.md ueber 600 Zeilen wachsen lassen ohne Extraktion
- ❌ Die 3-Schichten-Architektur ignorieren und alles in eine Datei packen

