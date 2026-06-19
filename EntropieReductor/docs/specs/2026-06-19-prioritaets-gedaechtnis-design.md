# Design-Spec: Prioritäts-Gedächtnis (Priority Memory)

**Datum:** 2026-06-19
**App:** Entropie Reductor (Kotlin/Compose, Room, Hilt, Gemini-API)
**Status:** Vom Benutzer freigegebene Designrichtung — bereit für Implementierungsplan

---

## 1. Ziel in einem Satz

Jedes Mal, wenn der Nutzer einer Aufgabe per Schieberegler eine **manuelle Priorität**
gibt, merkt sich die App das als eigenständigen Erinnerungs-Eintrag. Beim Einsprechen
einer **neuen** Aufgabe bekommt Gemini diese Erinnerungen mit und übernimmt — bei
**spezifischer** inhaltlicher Ähnlichkeit — die damalige Priorität als KI-Vorschlag.

## 2. Getroffene Entscheidungen (vom Benutzer bestätigt)

| Thema | Entscheidung |
|-------|-------------|
| Abgleich-Mechanismus | Gemini bekommt das Gedächtnis als Kontext im **bestehenden** Aufruf mit und entscheidet selbst (keine Embeddings, kein zweiter Aufruf). |
| Duplikate | Sehr ähnlichen Eintrag **aktualisieren** statt neu anlegen — Gedächtnis bleibt schlank. |
| Treffer-Verhalten | Übernommene Priorität wird als **KI-Priorität** (`priorityScore`) gesetzt, mit Begründung; bleibt manuell überschreibbar. |
| Karten-Optik | **Schlanke Karte** (Titel + verkürzte Beschreibung + Farb-Optik + Schieberegler). Antippen öffnet eine **Detail-Seite** mit voll editierbarem Titel **und** Beschreibung. |
| Abgleich-Text | An Gemini gehen **Titel + vollständige Beschreibung**. Match muss **spezifisch** sein (laufen ≈ laufen gehen), nicht nur thematisch verwandt (Laufen ≠ Federball, Sport ≠ Sport). |
| Loop-Aufgaben | Lernen **mit** — jede manuelle Prio-Setzung wird gemerkt, auch bei wiederkehrenden Aufgaben. |
| An/Aus-Schalter | **Vorhanden, Standard AN.** |
| Geltungsbereich | Abgleich greift **nur beim Einsprechen/Anlegen neuer Aufgaben**, NICHT beim automatischen Rescore bestehender Aufgaben. |
| Limit | KI berücksichtigt beim Abgleich nur die **neuesten N Einträge**. N ist **einstellbar** (Eingabefeld + Speichern), **Standard 300**. Bei Erreichen des Limits **blinkende Warnung** oben in der Liste. |

## 3. Bestehende Architektur (Ist-Zustand, Bezugspunkte)

- Aufgaben sind **keine** eigene Entity, sondern `EntropyEntryEntity`
  (`data/local/entities/EntropyEntryEntity.kt`, Tabelle `entropy_entries`).
  Relevante Felder: `title`, `description`, `priorityScore` (KI, Double 0–100),
  `priorityReason`, `manualPriorityScore` (Double?, manuell, null = KI bestimmt),
  `manualPriorityScoreSetAt`.
- Manuelle Prio-Setzung: `TasksViewModel.setManualPriority(entryId, score)`
  (`presentation/dashboard1/TasksViewModel.kt`, ~Z.983; Loop-Sonderfall ~Z.1002, der
  die Prio rückwärts ins `RecurringTemplateEntity` und Geschwister-Instanzen schreibt).
- Schieberegler: `EntropyEntryCard` in `presentation/dashboard1/TasksScreen.kt`
  (~Z.1577; Material3 `Slider` ~Z.1714, `0f..100f`, `steps = 19`,
  `onValueChangeFinished` → `onSetManualPriority`).
- Gemini beim Einsprechen: `domain/usecase/ProcessEntryUseCase.kt`
  (`invoke(...)` ~Z.41, Gemini-Call ~Z.91, `PRIORITY_DOCTRINE` ~Z.481–641,
  `StructuredEntryDto`, `rescoreExisting(...)` ~Z.309).
- DAO: `data/local/dao/EntropyEntryDao.kt`. Repository: `EntryRepository`.
- DB: `data/local/AppDatabase.kt` (Version **29**, Migrationen als
  `object : Migration(x, x+1)`, registriert via `addMigrations(...)`;
  Beispiel `MIGRATION_28_29` ~Z.859 fügt `manualPriorityScoreSetAt` hinzu).
- Backup/Drive-Sync: `data/remote/drive/BackupPayload.kt` (`@Serializable`,
  jede Entity hat ein `List<BackupXxx>`-Feld mit `= emptyList()`-Default;
  Mappings `toBackup()`/`toEntity()`); Orchestrierung im `SyncCoordinator`.
- Settings: `presentation/settings/SettingsHomeScreen.kt` (LazyColumn aus
  `SectionDef`/`SectionCard`), Routen in `presentation/navigation/Routes.kt`,
  Graph in `presentation/navigation/AppNavGraph.kt`. Referenz-Muster für ein
  scrollbares Unter-Screen: `presentation/settings/prompts/PromptsScreen.kt`.

## 4. Neue Datenkomponenten

### 4.1 Entity `PriorityMemoryEntity` (Tabelle `priority_memory`)
Getrennt von den Aufgaben, damit das Gedächtnis erhalten bleibt, auch wenn die
Ursprungsaufgabe erledigt/archiviert/gelöscht ist.

| Feld | Typ | Zweck |
|------|-----|-------|
| `id` | `String` (PK, UUID) | |
| `title` | `String` | Kurztitel (Karten-Zusammenfassung, editierbar) |
| `description` | `String` | Vollständiger Text (Detail-Seite editierbar, geht an Gemini) |
| `priority` | `Double` (0–100) | gemerkte Priorität |
| `createdAt` | `Long` | Anlage-Zeitpunkt |
| `updatedAt` | `Long` | letzte Änderung |
| `sourceEntryId` | `String?` | Ursprungsaufgabe (Nachvollziehbarkeit, optional) |

### 4.2 DAO `PriorityMemoryDao`
`getAll()` (Flow, sortiert nach `updatedAt DESC`), `getById`, `getNewest(limit: Int)`
(neueste N für den Gemini-Kontext), `observeCount()` (Flow<Int> für die Limit-Warnung),
`upsert`, `update`, `deleteById`, `getAllForBackup()`.

### 4.3 Repository `PriorityMemoryRepository`
Kapselt DAO + die Lern-Logik:
- `learnFromManualPriority(entry: EntropyEntryEntity, score: Double)` — legt an oder
  aktualisiert (siehe 5).
- `getMemoriesForPrompt(limit: Int): List<PriorityMemoryEntity>` — liefert die
  **neuesten `limit`** Einträge (sortiert `updatedAt DESC`) für den Gemini-Kontext (siehe 6).
- `observeCount(): Flow<Int>` — Gesamtzahl der Einträge, für die Limit-Warnung in der UI.
- CRUD für die Einstellungs-UI.

## 5. Lern-Logik (automatisch, im Hintergrund)

Auslöser: jeder Aufruf von `TasksViewModel.setManualPriority(...)` (normale **und**
Loop-Aufgaben). Nach dem bestehenden Speichern der manuellen Prio wird zusätzlich
`PriorityMemoryRepository.learnFromManualPriority(entry, score)` aufgerufen.

Ablauf in `learnFromManualPriority`:
1. Prüfen, ob bereits ein **sehr ähnlicher** Gedächtnis-Eintrag existiert.
   - Primär per `sourceEntryId`-Gleichheit (dieselbe Aufgabe erneut justiert → sicher Update).
   - Zusätzlich eine schlanke lokale Vor-Dedup (z. B. exakt/sehr nah gleicher Titel),
     um Duplikate aus fast identischen Aufgaben zu vermeiden. (Die *semantische*
     Tiefe macht Gemini beim Abgleich, nicht die Lern-Seite.)
2. Treffer → `priority`, `description`, `title`, `updatedAt` aktualisieren.
   Kein Treffer → neuen Eintrag anlegen.

Hinweis: Das Lernen ist nur aktiv, wenn der An/Aus-Schalter (siehe 7) auf AN steht.

## 6. Abgleich beim Einsprechen (Gemini)

In `ProcessEntryUseCase.invoke(...)` (Pfad für neue Aufgaben, **nicht** `rescoreExisting`):
- Wenn das Gedächtnis AN ist und Einträge existiert, wird vor dem Gemini-Call die
  Gedächtnis-Liste geladen (`getMemoriesForPrompt()`) und kompakt formatiert
  (`Titel | vollständige Beschreibung | Priorität`).
- Diese Liste wird als zusätzlicher Kontextblock an den bestehenden Aufruf angehängt;
  `PRIORITY_DOCTRINE` wird um eine **strenge** Match-Anweisung ergänzt:
  > „Unten steht ein Prioritäts-Gedächtnis aus früher manuell gesetzten Prioritäten.
  > Wenn die neue Aufgabe inhaltlich quasi **dasselbe Vorhaben** beschreibt wie ein
  > Eintrag (z. B. ‚laufen gehen' ≈ ‚Lauftraining im Wald'), dann übernimm dessen
  > Priorität als `priorityScore` und schreibe in `priorityReason` ‚aus früherer
  > ähnlicher Aufgabe übernommen'. **Nur thematisch verwandt reicht NICHT**
  > (Laufen ≠ Federball; ‚Sport' ist KEIN gemeinsamer Nenner). Im Zweifel: kein
  > Treffer, normal bewerten."
- Treffer → Priorität landet als **KI-Priorität** (`priorityScore`), nicht als
  `manualPriorityScore`. Der Nutzer kann sie weiterhin per Regler überschreiben (was
  dann wieder ins Gedächtnis lernt).
- Kein Treffer → unveränderte bestehende Berechnung.

**Skalierung/Limit (einstellbar):** Die KI berücksichtigt beim Abgleich nur die
**neuesten N Einträge** (`updatedAt DESC`). N ist ein in den Einstellungen **frei
einstellbares Limit**, **Standard 300**. Sind mehr Einträge gespeichert als N, fallen
die ältesten aus dem Abgleich — **verlustfrei**: sie bleiben gespeichert und in der
Liste sichtbar, nur der an Gemini geschickte Ausschnitt ist begrenzt. Erreicht oder
überschreitet die tatsächliche Eintragszahl das Limit, erscheint oben in der Liste eine
**blinkende Warnung** (siehe 7).

## 7. Einstellungs-Bereich „Prioritäts-Gedächtnis"

- **Verdrahtung:** neue Route in `Routes.kt`, Section in `SettingsHomeScreen`,
  `composable(...)` in `AppNavGraph.kt` (Muster wie `PromptsScreen`).
- **Listen-Screen** (`PriorityMemoryScreen` + `PriorityMemoryViewModel`):
  - Oben: **An/Aus-Schalter** „Prioritäts-Gedächtnis nutzen" (Standard AN; persistiert,
    z. B. in den bestehenden Settings/Preferences). Steuert Lernen **und** Anwenden.
  - **Limit-Eingabefeld:** kleines Zahlenfeld „Von der KI berücksichtigte Einträge"
    + Speichern (Standard 300). Persistiert wie der Schalter. Bestimmt N aus Abschnitt 6.
  - **Blinkende Warnung (oben, nur wenn `Eintragszahl >= Limit`):** auffälliger,
    blinkender Hinweis (Compose `rememberInfiniteTransition`, pulsierende Farbe/Alpha)
    ganz oben in der Liste, sofort sichtbar beim Öffnen. Text sinngemäß: „Limit erreicht
    (X von N). Ältere Einträge werden von der KI nicht mehr berücksichtigt — lösche
    Einträge oder erhöhe das Limit." Verschwindet automatisch, sobald wieder unter dem Limit.
  - Scrollbare `LazyColumn` schlanker Karten: Titel + verkürzte Beschreibung +
    Prio-Farb-Optik + Schieberegler (gleiche Optik wie `EntropyEntryCard`, aber ohne
    Status/Zeitfenster/Erledigt). Regler ändert `priority` direkt.
  - Karte antippen → Detail-Seite.
  - Eintrag entfernen (Swipe oder Button) → `deleteById`.
- **Detail-Seite** (`PriorityMemoryDetailScreen`):
  - Titel: editierbares Textfeld.
  - Beschreibung: großes, mehrzeiliges editierbares Textfeld (darf lang sein).
  - Prio-Schieberegler.
  - Speichern + Löschen.

## 8. Observability (Pflicht laut Observability-First-Direktive)

Die App hat bereits ein Diagnose-Log (`DiagnosticLogDatabase`). Neue Sonden:
- **Lern-Sonde:** beim Anlegen/Aktualisieren eines Gedächtnis-Eintrags
  (`gelernt: '<title>' → Prio <x>`, mit Angabe Anlage vs. Update).
- **Abgleich-Checkpoint (Intent-Verifikation):** beim Einsprechen schreibt der Code
  einen Checkpoint „erwartet vs. tatsächlich":
  - Treffer: `CHECKPOINT prio-memory-match: '<neueTitel>' ≈ '<gemerkterTitel>' → Prio <x> übernommen (ok)`.
  - Kein Treffer: `CHECKPOINT prio-memory-match: kein Treffer für '<neueTitel>' (ok)`.
- So lässt sich live prüfen, ob der Abgleich greift und ob er **spezifisch genug**
  ist (keine falschen Treffer wie Laufen↔Federball).

## 9. Persistenz-Querschnitt (mitziehen nicht vergessen)

- **Migration:** `AppDatabase` Version 29 → **30**, `object : Migration(29,30)` mit
  `CREATE TABLE priority_memory (...)` + Registrierung in `addMigrations(...)`;
  neue Entity in die `@Database(entities = [...])`-Liste.
- **Backup/Drive-Sync:** neues `List<BackupPriorityMemory>`-Feld in `BackupPayload`
  (Default `emptyList()`), `BackupPriorityMemory`-Klasse + Mappings, Sammeln/Einspielen
  im `SyncCoordinator` (inkl. Tombstone-Konsistenz wie bei anderen Entities).
- **Version-Bump + sichtbar:** App-`versionName`/`versionCode` erhöhen; sichtbare
  Versionsanzeige zieht automatisch mit.

## 10. Neue/geänderte Dateien (Überblick)

**Neu**
- `data/local/entities/PriorityMemoryEntity.kt`
- `data/local/dao/PriorityMemoryDao.kt`
- `data/repository/PriorityMemoryRepository.kt`
- `presentation/settings/prioritymemory/PriorityMemoryScreen.kt`
- `presentation/settings/prioritymemory/PriorityMemoryDetailScreen.kt`
- `presentation/settings/prioritymemory/PriorityMemoryViewModel.kt`

**Geändert**
- `data/local/AppDatabase.kt` (v30 + Migration + Entity-Liste)
- `presentation/dashboard1/TasksViewModel.kt` (`setManualPriority` ruft Lern-Logik)
- `domain/usecase/ProcessEntryUseCase.kt` (Gedächtnis-Kontext + Match-Anweisung)
- `presentation/navigation/Routes.kt`, `presentation/navigation/AppNavGraph.kt`,
  `presentation/settings/SettingsHomeScreen.kt` (Navigation + Section)
- `data/remote/drive/BackupPayload.kt` (+ `SyncCoordinator`) (Backup-Schema)
- DI-Modul (Bereitstellung von `PriorityMemoryDao`/`Repository`, Migration registrieren)
- App-`build.gradle.kts` (Version-Bump)
- Settings/Preferences-Store (An/Aus-Schalter **und** einstellbares Limit persistieren)

## 11. Bewusst NICHT enthalten (YAGNI)

- Keine Embeddings/Vektordatenbank (Gemini-Kontext reicht).
- Kein Abgleich beim Rescore bestehender Aufgaben (nur neue).
- Keine Verlaufs-Historie pro Eintrag (Update statt Duplikat).
- Kein eigener Gemini-Call nur für den Abgleich (in den bestehenden integriert).
