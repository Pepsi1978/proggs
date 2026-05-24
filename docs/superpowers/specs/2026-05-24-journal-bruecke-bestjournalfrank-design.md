# Design: Tagebuch-Brücke BestJournal Frank → Entropie Reductor

**Datum:** 2026-05-24
**Status:** Freigegeben (Frank, 2026-05-24)
**Betrifft zwei Apps:** `BestJournalFrank` (Quelle) und `EntropieReductor` (Ziel)

---

## 1. Ziel

Der bisher leere **Journal-Reiter** in Entropie Reductor (Aufgaben-Tab, Sub-Bereich Slot 1,
früher „Loop") wird zum echten **Tagebuch**, gefüttert aus der App **BestJournal Frank**.
Die Tagebucheinträge aus BestJournal Frank werden in Entropie Reductor angezeigt, im gleichen
Timeline-Look wie der bestehende Entropie-Reiter, mit Vorlesen-Funktion. Read-only: ansehen und
vorlesen, kein Bearbeiten. Bei jedem App-Start wird abgeglichen (volles Abbild).

### Begriffsklärung (wichtig)
Der Code-Bereich `presentation/tagebuch` (Slot 2, „Entropie") ist **kein** echtes Tagebuch,
sondern der **Entropie-Reiter** (erfasste innere/mentale/emotionale Entropie). Er bleibt
unverändert und wird nur als **optische Vorlage** (Timeline-Stil) für den neuen Journal-Reiter
genutzt. Die beiden Bereiche bleiben inhaltlich vollständig getrennt.

---

## 2. Entscheidungen (mit Frank abgestimmt)

| Punkt | Entscheidung |
|-------|--------------|
| Zugriffsweg | **ContentProvider** (Daten-Durchreiche) in BestJournal Frank, Sync beim Start in Entropie Reductor |
| Bearbeiten | **Nur lesen + vorlesen.** Kein Editieren/Löschen in Entropie Reductor |
| Sync-Art | **Volles Abbild.** Neue einfügen, geänderte aktualisieren, in der Quelle gelöschte entfernen |
| Inhalte | Titel, Text, KI-Zusammenfassung, Original/KI-verbessert (Umschalter), **Nachträge** |
| Vorlesen | Liest Eintrag **und** alle Nachträge am Stück hintereinander vor (ein Knopf) |
| Fotos/Videos | **Nicht** dabei (keine Mediendatei-Durchreiche nötig) |
| Optik | Timeline wie der Entropie-Reiter (Buch-Badge, Zeit-Sektionen) |
| Sync-Status-Kopf | Ganz oben im Journal-Reiter, **immer sichtbar** (auch bei leerer Liste): Zeitpunkt der letzten Synchronisierung + Anzahl der beim letzten Sync neu hinzugekommenen Einträge |

---

## 3. Architektur

### 3.1 BestJournal Frank: read-only ContentProvider („Durchreiche")

**Neue Datei:** `app/src/main/java/com/entropyjournal/data/provider/JournalExportProvider.kt`

- ContentProvider, der nur lesend Daten aus der bestehenden Room-DB `entropy_journal_db` herausgibt.
- Zugriff auf die DB über das vorhandene Singleton `AppDatabase.getDatabase(context)` und dessen
  `openHelper.readableDatabase` (gibt einen echten `android.database.Cursor` zurück, ideal als
  ContentProvider-Rückgabe). Es wird **nichts** geschrieben oder verändert.
- Authority: `${applicationId}.journalexport` (Manifest-Platzhalter). Damit ergibt sich
  `com.entropyjournal.debug.journalexport` (Debug) bzw. `com.entropyjournal.journalexport` (Release).
- Exponierte URIs:
  - `content://<authority>/entries` → Tabelle `journal_entries`
  - `content://<authority>/followups` → Tabelle `entry_follow_ups`
- `query()` liefert für `entries` die Spalten: `id, timestamp, title, displayText, rawText,
  improvedText, isImproved, summary`. Für `followups`: `id, entryId, createdAt, text, rawText,
  improvedText, isImproved`. `insert/update/delete` werfen `UnsupportedOperationException`
  (read-only). `getType()` liefert passende MIME-Typen.

**Manifest (`BestJournalFrank/app/src/main/AndroidManifest.xml`):**
```xml
<permission
    android:name="com.entropyjournal.permission.READ_JOURNAL"
    android:protectionLevel="normal"
    android:label="Tagebuch lesen" />

<provider
    android:name=".data.provider.JournalExportProvider"
    android:authorities="${applicationId}.journalexport"
    android:exported="true"
    android:readPermission="com.entropyjournal.permission.READ_JOURNAL" />
```

> Sicherheits-Hinweis: Da beide Apps **unterschiedliche Signatur-Schlüssel** haben
> (`debug-shared.keystore` vs. `entropiereductor.debug.keystore`), ist kein `signature`-Schutz
> möglich, ohne einen der Keystores anzugleichen (würde Neuinstallation + Datenverlust erzwingen).
> Deshalb `protectionLevel="normal"`. Für zwei private Apps auf Franks eigenem Gerät unkritisch.

### 3.2 Entropie Reductor: Abgleich + lokale Kopie + Anzeige

**Lokale Kopie (neue, eigenständige Room-DB `journal_mirror_db`):**
- `JournalMirrorEntryEntity`: `sourceId: Long` (PrimaryKey, = id aus BestJournal Frank),
  `timestamp, title, displayText, rawText, improvedText, isImproved, summary`.
- `JournalMirrorFollowupEntity`: `sourceId: Long` (PrimaryKey), `entryId: Long` (Index),
  `createdAt, text, rawText, improvedText, isImproved`.
- DAO: `upsertEntries`, `upsertFollowups`, `deleteEntriesNotIn(ids)`, `deleteFollowupsNotIn(ids)`,
  `observeAllEntries(): Flow<...>`, `observeFollowups(entryId)`, `getEntry(sourceId)`.
- **Bewusst getrennte DB**, die **nicht** ins Drive-Backup aufgenommen wird (die Daten sind durch
  erneuten Sync jederzeit reproduzierbar). Damit greift die Backup-Schema-Pflicht
  (`BackupPayload.kt`) hier **nicht**, weil diese DB kein Teil des App-Backups ist.

**Abgleich-Logik (`JournalMirrorRepository`):**
1. Authority auflösen: zuerst `com.entropyjournal.debug.journalexport`, sonst
   `com.entropyjournal.journalexport` (funktioniert für Debug- und Release-Variante von Frank).
2. `contentResolver.query(entries)` + `query(followups)` lesen.
3. Upsert aller gelesenen Einträge/Nachträge in die lokale DB.
4. Volles Abbild: lokale Zeilen löschen, deren `sourceId` nicht mehr in der Quelle vorkommt.
5. **Neue Einträge zählen** (nur echte Inserts, keine Updates) für den Sync-Status-Kopf.
6. Sync-Metadaten schreiben: `lastSyncMs` (jetzt) + `lastNewCount` (Anzahl Inserts).
7. Robuste Fehlerbehandlung: Provider nicht gefunden / nicht installiert / SecurityException
   → abfangen, loggen, vorhandene lokale Kopie unangetastet lassen, kein Crash.

**Sync-Metadaten:** kleiner DataStore `journal_sync_meta` mit `lastSyncMs: Long` und
`lastNewCount: Int`. Wird als Flow beobachtet und im Sync-Status-Kopf angezeigt.

**Start-Auslöser:** Sync läuft beim App-Start und beim Zurückkehren in den Vordergrund
(ProcessLifecycle ON_START), im IO-Coroutine-Scope, nicht blockierend. Verankert im vorhandenen
App-Startup-Pfad (`lifecycle.process` ist bereits als Dependency vorhanden).

**UI — Journal-Reiter (`presentation/journal/JournalScreen.kt`):**
- Sync-Status-Kopf ganz oben (immer sichtbar):
  `Zuletzt synchronisiert: dd.MM.yyyy · HH:mm  ·  N neue Einträge`
  (bei N=0: „0 Einträge neu"; wenn noch nie synchronisiert: „Noch nicht synchronisiert").
- Darunter die Timeline-Liste: Buch-Badge + Zeit-Sektionen (Heute/Gestern/Diese Woche/...),
  visueller Stil aus dem Entropie-Reiter wiederverwendet (gemeinsame Komponenten extrahieren
  bzw. nachbauen). Tippen auf einen Eintrag öffnet die Detail-Ansicht.
- Leer-Zustand: Sync-Status-Kopf bleibt sichtbar, darunter freundlicher Hinweis
  (z. B. „Noch keine Tagebucheinträge aus BestJournal Frank").

**UI — Detail (`presentation/journal/JournalEntryDetailScreen.kt`):**
- Titel, Zeitstempel, KI-Zusammenfassung (falls vorhanden).
- Umschalter **Original / KI-verbessert** (zeigt `rawText` bzw. `improvedText`).
- Nachträge darunter als eigene Karten (read-only).
- **Vorlesen-Knopf**: liest Eintragstext + alle Nachträge am Stück hintereinander vor,
  über den vorhandenen `TtsPlayer` (Google TTS). Keine Eingabefelder, kein Speichern.

**Navigation (`AppNavGraph.kt`, `Routes.kt`):**
- Aufgaben-Tab Slot 1 (`parent == TASKS && index == 1`) zeigt statt des leeren Platzhalters
  den neuen `JournalScreen`.
- Neue Detail-Route `journal/entry/{sourceId}`.

**Manifest (`EntropieReductor/app/src/main/AndroidManifest.xml`):**
```xml
<uses-permission android:name="com.entropyjournal.permission.READ_JOURNAL" />

<queries>
    <provider android:authorities="com.entropyjournal.journalexport" />
    <provider android:authorities="com.entropyjournal.debug.journalexport" />
</queries>
```

---

## 4. Datenfluss

```
App-Start / Vordergrund
   → JournalMirrorRepository.sync()  (IO-Thread)
   → ContentResolver.query(entries) + query(followups)  (durch die Durchreiche)
   → upsert + prune in journal_mirror_db
   → Sync-Metadaten (lastSyncMs, lastNewCount) schreiben
JournalScreen beobachtet:
   → journal_mirror_db (Flow)  → Timeline-Liste
   → journal_sync_meta (Flow)  → Sync-Status-Kopf
Tippen → JournalEntryDetailScreen → Vorlesen (TtsPlayer: Eintrag + Nachträge)
```

---

## 5. Fehlerbehandlung

| Fall | Verhalten |
|------|-----------|
| BestJournal Frank nicht installiert / Provider fehlt | `query` liefert `null` / `IllegalArgumentException` → abfangen, lokale Kopie bleibt, Sync-Status zeigt letzten erfolgreichen Stand |
| Keine Berechtigung (SecurityException) | abfangen, loggen, freundlicher Hinweis statt Crash |
| Quelle leer | Leer-Zustand, Sync-Status-Kopf mit „0 Einträge neu" |
| Teil-Lesefehler (z. B. followups schlägt fehl) | Einträge trotzdem übernehmen (Graceful Degradation), Fehler loggen |

---

## 6. Testing

- **Unit-Test** der Abgleich-Logik (Diff: einfügen / aktualisieren / löschen) gegen einen
  Fake-Cursor / Fake-ContentResolver, inkl. korrekter `lastNewCount`-Berechnung.
- **Manuell:** beide Apps bauen + installieren →
  1. Eintrag in BestJournal Frank anlegen → Entropie Reductor neu starten → erscheint im Journal,
     Sync-Kopf zeigt „1 neue Einträge".
  2. Eintrag in Frank ändern → nach Neustart aktualisiert.
  3. Eintrag in Frank löschen → nach Neustart verschwunden.
  4. Nachtrag in Frank → erscheint im Detail; Vorlesen liest Eintrag + Nachtrag.
  5. BestJournal Frank deinstalliert → Entropie Reductor crasht nicht, alte Kopie bleibt.

---

## 7. Betroffene Dateien (Übersicht)

**BestJournal Frank:**
- NEU `data/provider/JournalExportProvider.kt`
- ÄNDERN `AndroidManifest.xml` (Permission + Provider)
- Version-Bump (`app/build.gradle.kts`)

**Entropie Reductor:**
- NEU `data/local/journalmirror/JournalMirrorEntities.kt` (2 Entities)
- NEU `data/local/journalmirror/JournalMirrorDao.kt`
- NEU `data/local/journalmirror/JournalMirrorDatabase.kt`
- NEU `data/repository/JournalMirrorRepository.kt`
- NEU `data/prefs/JournalSyncMeta.kt` (DataStore für lastSyncMs/lastNewCount)
- NEU `presentation/journal/JournalScreen.kt`
- NEU `presentation/journal/JournalEntryDetailScreen.kt`
- NEU/ÄNDERN Start-Auslöser (ProcessLifecycle-Observer im App-Startup-Pfad)
- ÄNDERN `di/...` (Hilt: Bereitstellung der neuen DB + Repository)
- ÄNDERN `presentation/navigation/AppNavGraph.kt`, `Routes.kt` (Slot 1 + Detail-Route)
- ÄNDERN `AndroidManifest.xml` (`<queries>` + `<uses-permission>`)
- Wiederverwendung: vorhandener `TtsPlayer`, Timeline-Komponenten aus dem Entropie-Reiter
- Version-Bump (`app/build.gradle.kts`)

**Beide Apps** werden einmal neu gebaut und auf dem Gerät installiert.

---

## 8. Bewusste Nicht-Ziele (YAGNI)

- Kein Schreiben/Bearbeiten/Löschen zurück in BestJournal Frank.
- Keine Fotos/Videos.
- Kein Live-Sync während die App offen ist (nur bei Start/Vordergrund, wie gewünscht).
- Kein geräteübergreifender Sync (gleiches Gerät genügt).
