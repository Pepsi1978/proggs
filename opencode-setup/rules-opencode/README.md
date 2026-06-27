# rules-opencode — verkleinerte Regeln fuer OpenCode (Qdrant / zweites Gehirn)

Dieser Ordner enthaelt **kompakte Fassungen** der Arbeitsregeln aus der Gehirn-Kategorie
`Programmierung/Rules` (aktuell 15). Zweck: OpenCode (auf dem Qdrant-Server) laedt diese kleinen
Versionen statt der grossen Originale — das spart Token. **Claude Code nutzt weiterhin die VOLLEN
Regeln aus `~/.claude/rules/`** (diese hier ersetzen die nicht). Ausnahme: `anti-halluzination.md`
ist eine NEUE, OpenCode-spezifische Regel ohne `~/.claude/rules/`-Original — ihre Vollfassung/Belege
liegen in `best-practices/agents/anti-halluzination-regeln.md`.

## Wie zurueckspeichern
Jede Datei beginnt in der **ersten Zeile mit dem exakten Gehirn-Titel** (reiner Text, ohne `#`,
ohne "(KRITISCH)"-Zusatz). Diese Zeile einfach markieren, kopieren und beim Speichern als Titel
einfuegen — dann landet die kompakte Fassung unter demselben Titel + Kategorie
`Programmierung/Rules` im Gehirn (ueberschreibt die grosse Version).

## Mapping (Datei -> exakter Titel zum Kopieren)
| Datei | Gehirn-Titel (1:1, erste Zeile der Datei) |
|-------|--------------------------------------------|
| observability-first.md | Observability-First: Sonden-, Logging- & Live-Monitoring-Standard |
| known-bugs-before-coding.md | Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird |
| debugging-and-verification.md | Debugging & Verifikation: Systematisch statt Trial-and-Error |
| parallel-sessions-git.md | Parallele Sessions — Commit & Push am geteilten main-Branch |
| git-workflow.md | Git-Workflow: Alle Regeln fuer Git-Operationen |
| communication-and-language.md | Kommunikation, Sprache & Benutzer-Interaktion |
| lossless-context-principle.md | Verlustfrei-Prinzip: Kontext reduzieren OHNE Funktionalitaet zu verlieren |
| commit-before-build.md | Commit+Push VOR jedem Build |
| german-umlauts-always.md | Deutsche Umlaute immer verwenden |
| research-persistence.md | Research-Persistenz: Recherchen in Best Practices & Bug-Almanache einarbeiten |
| secrets-in-sk-folder.md | Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3) |
| semicolon-task-separator.md | Semikolon-Trenner für mehrere Aufgaben in einem Prompt |
| task-completion-summary.md | Pflicht-Schema am Ende jeder Aufgabe |
| version-bump-visible-always.md | Versionszähler immer hochzählen — und SICHTBAR |
| anti-halluzination.md | Anti-Halluzination: erst pruefen, dann behaupten — nichts erfinden |
| projekt-wissen-aus-gehirn.md | Projekt-Wissen aus dem Gehirn zuerst lesen (Kategorie Projekte) |

Kategorie fuer alle 16: `Programmierung/Rules`.

## Grundsatz der Verkleinerung
Verkleinert heisst: alle Handlungsregeln, MUSS-/NIEMALS-Punkte und konkreten Befehle/Formate
bleiben erhalten — nur Vorfall-Anekdoten, Quellenangaben (arXiv etc.), "Zusammenspiel mit anderen
Regeln"-Tabellen, ausfuehrliche Code-Beispiele und Wiederholungen sind entfernt. Reicht eine Regel
im Einzelfall inhaltlich nicht aus, gilt die Vollfassung in `~/.claude/rules/<gleicher-dateiname>`.
