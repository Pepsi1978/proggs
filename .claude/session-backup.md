# Session Handoff — 2026-06-19, ~21:45

## Ziel (1-3 Saetze)
EntropieReductor (private Android-App, Kotlin/Compose/Room/Hilt/Gemini): Das Feature
"Prioritaets-Gedaechtnis" ist fertig gebaut und installiert. Jetzt offen, in DIESER Reihenfolge:
(1) Bug-1-Fix am Handy bestaetigen, (2) NEUE AUFGABE: Prioritaets-Gedaechtnis-Liste nach Prioritaet
sortieren (hoechste oben), (3) DANACH der grosse app-weite ID-Architektur-Umbau. WICHTIG: ERST die
kleine Sortier-Aufgabe, DANN das grosse ID-Projekt.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Code-Aufgabe unterbrochen — der letzte Code-Stand (Bug-1-Fix) ist sauber committed,
gepusht UND auf dem Handy installiert. Die Session wartet auf eine RICHTUNGS-ENTSCHEIDUNG des
Benutzers zum naechsten grossen Vorhaben (ID-Architektur, siehe "Offene Fragen").

ALLERERSTE Aktion der neuen Session: NICHT blind drauflos. Zuerst die "Offene Fragen" unten lesen
und mit dem Benutzer klaeren, ob (a) er Bug 1 am Handy schon getestet hat und ob er (b) jetzt das
grosse ID-Architektur-Projekt geplant haben will (er hatte es bereits gross spezifiziert — siehe
Abschnitt "ID-Architektur-Vorhaben"). Wenn er "leg los mit dem ID-Projekt" sagt: mit der
Bestandsaufnahme + Design-Spec starten (brainstorming-Skill, dann writing-plans), NICHT ad hoc das
Kern-System umbauen.

## NEUE AUFGABE 1 (ZUERST umsetzen, VOR der ID-Architektur) — Prioritaets-Gedaechtnis sortieren
Im Prioritaets-Gedaechtnis (Einstellungen -> "Prioritaets-Gedaechtnis", PriorityMemoryScreen) die
Liste nach PRIORITAET sortieren: hoechste Prioritaet GANZ OBEN, niedrigste ganz unten — egal wie
viele Eintraege. Bei gleicher Prioritaet ist die Reihenfolge der gleichwertigen Eintraege
untereinander egal (z.B. zwei 60%-Eintraege stehen beieinander, 61% darueber, 59% darunter).
Zweck: Benutzer sieht auf einen Blick wichtige vs. unwichtige Aufgaben und kann Prioritaeten
nachtraeglich anpassen.
UMSETZUNG (klein): Aktuell sortiert die Liste nach `updatedAt DESC` (PriorityMemoryDao.getAll()).
Auf "ORDER BY priority DESC, updatedAt DESC" umstellen (zweites Kriterium nur fuer stabile Anzeige
bei Gleichstand). DAO-Weg bevorzugt (statt im ViewModel). Danach: ./gradlew :app:compileDebugKotlin,
commit+push, installieren (RFCX70KTDFX), am Geraet kurz pruefen. KEIN grosser Umbau noetig.

## Aktueller Status
- Erledigt: Prioritaets-Gedaechtnis KOMPLETT (9 Tasks, Commits #46946-#46954). Datenbank
  (priority_memory, Migration 29->30), Lernen beim Schieberegler (auch Loop), Gemini-Abgleich beim
  Einsprechen mit strenger Match-Regel, Einstellungs-UI (Liste + An/Aus + Limit 300 + blinkende
  Warnung + Detail-Editor), Drive-Backup Schema v17 + Tombstone, Version 0.17.0. Unit-Tests gruen,
  alle Builds gruen, APK auf Geraet RFCX70KTDFX installiert, DB-Migration lief sauber (App startet).
- Erledigt: Bug 1 (Loop-Prio wird nicht gespeichert) GEFIXT + installiert — Commit #46955. Root
  Cause: der neue learnFromManualPriority-Hook in TasksViewModel.setManualPriority lag OHNE try/catch
  zwischen dem Instanz-Update und der Loop-Propagierung (isRec-Block); eine Exception im Hook brach
  die Coroutine ab -> Loop-Block (Template + Geschwister) lief nie. Fix: Hook in runCatching{} +
  .onFailure-Diag-Log gekapselt, sodass ein Fehler im optionalen Lernen NIE die Kern-Funktion bricht.
- In Arbeit / OFFEN: Bug-1-Funktionstest am Handy (nur der Benutzer kann einsprechen/Regler bedienen).
- OFFEN (NICHT begonnen): grosser ID-Architektur-Umbau (siehe eigener Abschnitt unten).
- Bug 2 (doppelter Aufgabenvorschlag) ist NICHT separat gefixt — wird durch den ID-Umbau strukturell
  geloest. Benutzer hat den doppelten Vorschlag bereits manuell geloescht.

## Relevante Dateien (Prioritaets-Gedaechtnis, fertig)
- data/local/entities/PriorityMemoryEntity.kt, data/local/dao/PriorityMemoryDao.kt (neu)
- data/repository/PriorityMemoryRepository.kt (Lernen + Restore-Methoden + Tombstone bei delete)
- domain/usecase/PriorityMemoryLogic.kt (+ Test) — reine Funktionen formatPriorityMemoriesForPrompt,
  selectMemoryToUpdate (Dedup per sourceEntryId, dann Titel-Fallback)
- domain/usecase/ProcessEntryUseCase.kt — Gedaechtnis laden + an SystemPromptBuilder.build uebergeben
  + Match-Checkpoint (Diag "PrioMemory"); PRIORITY_DOCTRINE enthaelt strenge Match-Regel
- domain/usecase/SystemPromptBuilder.kt — neuer Kontext-Block "Prioritaets-Gedaechtnis"
- presentation/dashboard1/TasksViewModel.kt — setManualPriority mit runCatching-gekapseltem Lern-Hook
- presentation/settings/prioritymemory/ (PriorityMemoryScreen, PriorityMemoryDetailScreen,
  PriorityMemoryViewModel mit List- + DetailViewModel)
- data/remote/drive/BackupPayload.kt (BackupPriorityMemory + Mappings, Feld priorityMemories),
  SyncCoordinator.kt (Upload, version=17), SyncEntriesUseCase.kt (Restore LWW+Tombstone),
  data/TombstoneStore.kt (TombstoneType.PRIORITY_MEMORY)
- docs/specs/2026-06-19-prioritaets-gedaechtnis-design.md + docs/plans/2026-06-19-...-plan.md

## Wichtige Befunde zum geplanten ID-Umbau (aus 2 Explore-Agenten dieser Session)
Identitaetskette ist HEUTE DREIFACH GEBROCHEN: Idee hat feste UUID -> Vorschlag bekommt NEUE UUID
(kein sourceIdeaId) -> Aufgabe bekommt NOCHMAL neue UUID. Einziger Doppelvorschlag-Schutz: ein
processed_idea_ids-Set, das (a) von "Zuruecksetzen"-Buttons geleert wird und (b) in ZWEI getrennten
Sets (Tasks vs Habits) auseinanderlaeuft. KEIN Existenzcheck "hat diese Idee schon eine Aufgabe?".
Belegte Stellen:
- Ideen: IdeenEntry (presentation/ideen/IdeenScreen.kt, DataStore "ideen_entries"), id=UUID, stabil.
- Vorschlaege: KiTaskSuggestion/AutoTaskSuggestion (UUID, NICHT verknuepft); Stores
  data/SuggestionDataStores.kt (kiTaskSuggestionStore, gewohnheitSuggestionStore).
- Generierung+Dedup: domain/usecase/GenerateSuggestionsUseCase.kt (~Z.87 filter id !in processedIds).
- Uebernahme Vorschlag->Aufgabe verliert ID: KiTaskSuggestViewModel.acceptSuggestion ruft
  process(text) -> ProcessEntryUseCase erzeugt neue UUID (~Z.140).
- Aufgabe/Entropie: EntropyEntryEntity.id (UUID, PK). Thesen, Journal/Tagebuch, Mental-Saetze:
  haben teils eigene IDs, aber Identitaet wird beim Uebergang nicht durchgereicht.

## Getroffene Entscheidungen
- Prioritaets-Gedaechtnis: Gemini-Abgleich im bestehenden Aufruf, Treffer als KI-Prio, schlanke Karte
  + Detail-Editor, Loop lernt mit, nur neue Aufgaben (kein Rescore), Limit Default 300, An/Aus an.
- Bug-1-Fix bewusst via runCatching (Defense-in-Depth: optionales Feature darf Kern nie brechen),
  NICHT durch Entfernen des Hooks (Direktive #3 funktionserhaltend).
- ID-Umbau wird NICHT blind gemacht — erst Bestandsaufnahme + Design-Spec + Plan (wie beim
  Prioritaets-Gedaechtnis), dann schrittweise mit Build-Pruefung + Daten-Migration.

## Fehlgeschlagene Ansaetze
- Keine echten Sackgassen. Hinweis: bug-almanac-guard blockiert Edits, bis Almanach+Best-Practices-
  Kurzcheck (Read limit=80) des Bereichs gelesen sind — in dieser Session bereits gelesen/freigegeben:
  Room, Hilt, Kotlin, Jetpack Compose, Google-Drive-Backup, Gradle. Bei NEUER Session erneut noetig.
- git commit -- <pfad> scheitert bei NOCH-UNTRACKTEN neuen Dateien ("did not match any file") —
  bei neuen Dateien zuerst `git add`, dann commit.
- Session-Backup: single-quoted Heredoc (<<'EOF') ueber den Bash-Tool-Wrapper scheitert am Quoting
  ("unexpected EOF matching '") — stattdessen Write-Tool (Datei einmal lesen) + cp ins Repo nutzen.

## ID-Architektur-Vorhaben (Benutzer-Spezifikation sinngemaess — NAECHSTES GROSSES PROJEKT)
Der Benutzer will eine app-weite Identitaets-Architektur:
- JEDER Eintrag/Input bekommt eine feste, eindeutige, kollisionssichere ID: Ideen,
  Aufgabenvorschlaege, Aufgaben, GEWOHNHEITEN, Entropie-Eintraege, Thesen, Journal-Eintraege,
  Mental-Board-Saetze. Jeder einzelne Mental-Satz eine eigene feste ID.
- Die ID wird IMMER zum naechsten Schritt MITGESCHICKT — egal ob manuell oder agentisch.
- Eine Idee kann zu Vorschlag, Aufgabe UND Gewohnheit werden — alle teilen dieselbe ID wie die Idee.
- Dedup/"existiert schon?" laeuft nur noch ueber die ID (nicht ueber Titel/Text). Damit ist auch
  Umbenennen/Editieren ohne Identitaetsverlust moeglich und Doppelvorschlaege sind ausgeschlossen.
- Zukuenftige Ketten (z.B. aus Entropie automatisch Ideen bauen, aus Thesen Ideen) reichen die ID
  ebenfalls durch. Forscher-Bereich: Entropie, Thesen, Journal — jeder Eintrag feste ID.
- Kern-Erkenntnis: Viele Entities HABEN schon UUIDs; sie werden nur beim Uebergang weggeworfen.
  Der Umbau = Herkunfts-/originId-Feld ueberall + ID an jedem Uebergang durchreichen + Dedup auf ID +
  Migration fuer Bestandsdaten.
- HARTE REGEL: KEINE doppelten IDs im gesamten System. Zwei unterschiedliche Ideen / zwei
  unterschiedliche Aufgaben duerfen NIE dieselbe ID haben. Gleiche ID gibt es NUR entlang einer KETTE
  (Idee -> Vorschlag -> Aufgabe, oder Idee -> Vorschlag -> Gewohnheit). ID-Generierung kollisionssicher (UUID).
- PRIORITAETS-GEDAECHTNIS, ID-Sonderregel (Benutzer-Entscheidung 2026-06-19): Gedaechtnis-Eintraege
  bekommen eine EIGENE, eigenstaendige Identitaet, damit ein Agent NIE einen Gedaechtnis-Eintrag mit
  einer echten Aufgabe verwechselt (sie teilen NICHT dieselbe Primaer-ID). Die Herkunft (Aufgabe, aus
  der der Eintrag stammt) wird per Referenz gehalten. Sobald der Nutzer einen Gedaechtnis-Eintrag
  editiert (Titel/Beschreibung), ist er nicht mehr deckungsgleich mit der Ursprungsaufgabe -> die
  Verbindung wird geloest. Das ist die EINZIGE Stelle im System, wo sich eine Zuordnung aendert.
  CLAUDE-EMPFEHLUNG + AKTUELLER STAND: PriorityMemoryEntity hat HEUTE schon das Richtige — eigene
  eindeutige `id` (UUID) PLUS `sourceEntryId` (= ID der Ursprungsaufgabe als Referenz/Kette). Damit:
  keine doppelten Primaer-IDs, volle Rueckverfolgbarkeit, keine Agent-Verwechslung. Umzusetzen bleibt
  nur: beim Editieren eines Gedaechtnis-Eintrags `sourceEntryId` auf null setzen (Verbindung geloest).
  Deckt sich exakt mit Franks finaler Intuition ("Gedaechtnis-Aufgaben eigene ID").

## Naechste Schritte (priorisiert)
1. Bug 1 am Handy bestaetigen (Loop-Prio bewegt + bleibt nach Reload?). Bei Problem:
   "starte den Live-Logik-Check" -> adb logcat -s PrioMemory mitlesen.
2. NEUE AUFGABE 1 (ZUERST): Prioritaets-Gedaechtnis-Liste nach Prioritaet sortieren — siehe eigener
   Abschnitt oben. Klein/schnell (DAO ORDER BY priority DESC).
3. DANACH das grosse ID-Architektur-Projekt: brainstorming-Skill -> Design-Spec (welche Entities,
   ID durchreichen via originId/sourceId, Migration, Dedup-Umstellung, Gedaechtnis-ID-Sonderregel +
   keine-doppelten-IDs-Regel siehe ID-Vorhaben), dann writing-plans -> schrittweise, Build pro Schritt.
4. Geraete-ID: RFCX70KTDFX. Debug-Package: de.frank.entropyreducer.debug
   (Start: adb -s RFCX70KTDFX shell monkey -p de.frank.entropyreducer.debug -c android.intent.category.LAUNCHER 1).

## Offene Fragen (worauf der Benutzer antworten muss)
- Soll JETZT mit der Bestandsaufnahme + Planung des grossen ID-Architektur-Projekts gestartet werden,
  oder zuerst Bug 1 am Handy bestaetigen? (Letzte gestellte Frage der Session — Benutzer hat
  stattdessen "session backup" gesagt.)

## Anker
- Branch: main
- Geraet: RFCX70KTDFX (Debug-Package de.frank.entropyreducer.debug), App-Version 0.17.0
- Letzte Commits:
9cadbf8ab #46955 - fix(EntropieReductor): isolate priority-memory learn hook (runCatching)
6455fb44f #46954 - chore(EntropieReductor): version bump 0.17.0 for priority memory feature
5dfe1d197 #46953 - feat(EntropieReductor): drive backup/sync for priority memory (schema v17 + tombstone)
f96d28284 #46952 - feat(EntropieReductor): priority-memory detail editor
7f899788b #46951 - feat(EntropieReductor): priority-memory settings list (toggle, limit, blinking warning)
