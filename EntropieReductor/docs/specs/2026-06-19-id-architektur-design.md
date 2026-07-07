# Design-Spec: App-weite ID-Architektur (Identitaets- & Herkunfts-System)

- **Datum:** 2026-06-19
- **App:** EntropieReductor (privat, Kotlin/Compose/Room/Hilt/Gemini, Multi-Device: S23 Ultra, Fold 6, Tablet)
- **Status:** Design freigegeben (Frank, 2026-06-19) — bereit fuer Implementierungsplan
- **Vorgaenger-Kontext:** Prioritaets-Gedaechtnis-Feature (v0.17.x) ist fertig und nutzt bereits das Zielmuster (eigene `id` + `sourceEntryId`).

---

## 1. Ziel & Motivation

Heute hat zwar fast jeder Eintrag eine eigene ID, aber **an jedem Uebergang wird die ID weggeworfen** und eine neue erzeugt (Idee → Vorschlag → Aufgabe). Folgen:

- **Doppelvorschlag-Bug:** Ein bereits angenommener (und ggf. umbenannter) Vorschlag taucht erneut auf, weil die Verbindung „dieser Vorschlag ist schon eine Aufgabe geworden" verloren geht. Dedup laeuft heute ueber zwei getrennte „verarbeitete Ideen"-Listen (`processed_idea_ids` fuer Aufgaben, `habit_processed_idea_ids` fuer Gewohnheiten), die auseinanderlaufen.
- **Keine Rueckverfolgbarkeit:** Man kann nicht nachvollziehen, aus welcher Idee eine Aufgabe entstanden ist.
- **Heterogenes Datenmodell:** Manche Daten liegen als JSON in DataStore (Ideen, Vorschlaege, Gewohnheiten, Mental-Saetze), manche als Room-Tabellen (Aufgaben/Entropie, Prioritaets-Gedaechtnis). Das erschwert Sicherung und Sync.

**Ziel:** Eine einheitliche, app-weite Identitaets-Architektur, in der jeder Eintrag eine feste, eindeutige ID hat, seine Herkunft kennt (Kette rueckverfolgbar), alle Daten in **einer** Datenbank leben (saubere Sicherung + Sync), und der Doppelvorschlag-Bug strukturell verschwindet — **ohne** den Fortschritt zu blockieren.

---

## 2. Grundprinzipien (die freigegebenen Entscheidungen)

| # | Entscheidung | Begruendung |
|---|--------------|-------------|
| P1 | **Eigene ID + Herkunfts-Verweis** pro Eintrag (nicht eine geteilte ID entlang der Kette) | Jeder Eintrag eindeutig ansprechbar UND Kette rueckverfolgbar; erfuellt „KEINE doppelten IDs". Identisch zum bestehenden Prioritaets-Gedaechtnis. |
| P2 | **Alle Daten in EINE Room-Datenbank** (DataStore-JSON-Listen ziehen um) | Ein Ort fuer den Agenten; „ganze DB sichern = alles gesichert" (Backup-Wunsch); kein Doppel-System; DataStore ist fuer wachsende Eintrags-Listen ohnehin das falsche Werkzeug. |
| P3 | **Nie blockieren.** ID-Architektur dedupliziert nur *exakte* Ketten-Duplikate (derselbe konkrete Eintrag); *semantische* Aehnlichkeit bleibt Sache des Prioritaets-Gedaechtnisses. | Neu eingesprochene Ideen sind nie 1:1 gleich (Nuancen mit Gruenden); eine harte Blockade wuerde echten Fortschritt verhindern (halb erledigte Aufgaben, vergessene Unterpunkte). |
| P4 | **Schrittweise**: ein Datentyp / eine Kette pro Etappe, dazwischen commit+push + Verifikation per Live-Logik-Sonden („Log-Kette"). | Grosser Umbau bleibt kontrolliert und rueckbaubar. |
| P5 | **Feingranularer Multi-Device-Sync bleibt** (Last-Write-Wins + Tombstones), jetzt einheitlich ueber Room. Zusaetzlich kompletter DB-Snapshot als Disaster-Recovery. | „Ganze DB ersetzen" wuerde Multi-Device-Datenverlust zurueckbringen (ein Geraet ueberschreibt das andere). |

---

## 3. Datenmodell

### 3.1 Identitaets-Bausteine (an JEDEM Eintrag)

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `id` | `String` (UUID), Primary Key | Eigene, eindeutige ID. Haben fast alle Eintraege heute schon. |
| `originId` | `String?` | Direkter Vorgaenger („von welchem Eintrag stamme ich?"). `null`, wenn der Eintrag ein Ursprung ist (z. B. eine frisch eingesprochene Idee) oder ein Bestandsdatum vor dem Umbau. |
| `originType` | `String?` (Enum-Code) | Art des Vorgaengers: `IDEA`, `TASK_SUGGESTION`, `HABIT_SUGGESTION`, `TASK`, `HABIT`, `ENTROPY`, `THESE`, `JOURNAL`, … |
| `rootId` | `String?` | Wurzel der Kette (der allererste Ursprungs-Eintrag). Erbt sich entlang der Kette: ist `originId` gesetzt, uebernimmt das Kind die `rootId` des Vorgaengers (bzw. dessen `id`, falls der Vorgaenger selbst die Wurzel ist). Ermoeglicht „ganze Kette auf einen Blick". |

### 3.2 Tabellen

**Bereits in Room (bekommen nur die Identitaets-Bausteine + Typ-Kennzeichen ergaenzt):**

| Tabelle | Inhalt | Aenderung |
|---------|--------|-----------|
| `entropy_entries` | Aufgaben/Entropie **sowie** Thesen **und** app-eigenes Journal (heute ueber `priorityScore`/`category`/`bucket` differenziert) | `originId`/`originType`/`rootId` ergaenzen; sauberes Typ-Kennzeichen pruefen: bestehendes `category`-Feld nutzen ODER explizites `type`-Feld (`ENTROPIE`/`THESE`/`JOURNAL`) einfuehren — Entscheidung im Plan nach Blick auf das echte `category`-Feld. **Bleibt eine gemeinsame Tabelle** (gleiche Struktur, gleiche Operationen). |
| `entropy_entry_followups` | Aufgaben-Nachtraege | `originId`/`originType`/`rootId` ergaenzen (FK auf `entropy_entries` bleibt). |
| `priority_memory` | Prioritaets-Gedaechtnis | Nutzt bereits `id` + `sourceEntryId`. Vereinheitlichung der Feldnamen pruefen (siehe §6). |
| `journal_mirror_entries` | **Externer** Journal-Spiegel (read-only Fremd-App-Daten) | **Bleibt unveraendert, eigene Tabelle** — anderer Natur (gespiegelte Fremddaten mit eigener `sourceId`). Nicht Teil der Kette. |

**Neu in Room (Migration aus DataStore-JSON):**

| Neue Tabelle | Loest ab (DataStore-Key) | Inhalt |
|--------------|--------------------------|--------|
| `ideas` | `ideen_entries` / `entries_json` | Ideen |
| `idea_followups` | (im selben JSON verschachtelt) | Ideen-Nachtraege |
| `task_suggestions` | `ki_task_suggestions` / `suggestions_json` | Aufgaben-Vorschlaege |
| `habit_suggestions` | `gewohnheit_suggestions` / `suggestions_json` | Gewohnheits-Vorschlaege |
| `habits` | `gewohnheit_board` / `gewohnheiten_json` | Gewohnheiten |
| `mental_sentences` | `mental_board` / `mentals_json` | Mental-Board-Saetze (jeder Satz ein Eintrag) |

**Abgeloest (ersatzlos, durch Ketten-Logik ersetzt):** die DataStore-Keys `processed_idea_ids` und `habit_processed_idea_ids`.

---

## 4. Die Ketten & ID-Durchreichung

An jedem Uebergang wird `originId`/`originType`/`rootId` gesetzt statt eine beziehungslose neue UUID zu erzeugen.

```
Idee            id=A1   origin=—            root=A1
  → Vorschlag   id=V7   origin=A1 (IDEA)    root=A1
  → Aufgabe     id=G3   origin=V7 (TASK_SUGGESTION)  root=A1
```

**Kern-Ketten (Stufe 1):**
- Idee → Aufgaben-Vorschlag → Aufgabe
- Idee → Gewohnheits-Vorschlag → Gewohnheit

**Konkrete Code-Uebergaenge, die die Herkunft setzen muessen** (aus der Inventur):
- `GenerateSuggestionsUseCase` (Vorschlags-Erzeugung): Quell-Ideen-`id` an den Vorschlag binden.
- `KiTaskSuggestViewModel.acceptSuggestion`: heute wird der Vorschlag zu Text konkateniert und neu verarbeitet → ID-Verlust. Kuenftig: `originId` = Vorschlag-`id` an die entstehende Aufgabe.
- `GewohnheitBoardScreen.addGewohnheit` / Drag-Promotion: `originId` = Gewohnheits-Vorschlag-`id` an die Gewohnheit.
- `ProcessEntryUseCase` (Entropy-Erzeugung): Herkunft mitschreiben statt nur neue UUID.

**Spaetere Ketten (Stufe 5, soweit gewuenscht):** Entropie → Idee, These → Idee usw. — gleiches Muster.

---

## 5. Dedup-Philosophie (zwei Werkzeuge, sauber getrennt)

| Werkzeug | Zustaendig fuer | Verhalten |
|----------|-----------------|-----------|
| **ID-Architektur** | *Exakte* Ketten-Identitaet | Verhindert nur das technische Doppel-Weiterverarbeiten **ein und desselben** konkreten Eintrags (z. B. ein schon angenommener Vorschlag wird nicht erneut als offener Vorschlag gefuehrt — erkennbar daran, dass aus ihm bereits ein Eintrag mit `originId` = seine `id` hervorging). **Blockiert nie** eine neue oder aehnliche Idee. |
| **Prioritaets-Gedaechtnis** | *Semantische* Aehnlichkeit | Erkennt ueber Gemini „so etwas hatten wir schon" und vergibt die gelernte Prioritaet. **Blockiert nichts.** |

**Abgeschafft:** die zwei getrennten `processed_idea_ids`-Listen. Ersetzt durch die robuste, einheitliche Kette ueber `originId`/`rootId`.

---

## 6. Prioritaets-Gedaechtnis-Sonderregel

- Behaelt **eigene `id`** + **Herkunfts-Referenz** (`sourceEntryId` = die Ursprungs-Aufgabe), damit ein Agent einen Gedaechtnis-Eintrag **nie** mit einer echten Aufgabe verwechselt (sie teilen NICHT dieselbe Primaer-ID).
- **Beim Editieren** eines Gedaechtnis-Eintrags (Titel/Beschreibung) wird `sourceEntryId` auf `null` gesetzt — die Verbindung loest sich, weil der Eintrag dann nicht mehr deckungsgleich mit der Ursprungs-Aufgabe ist. **Einzige** Stelle im System, wo sich eine Zuordnung nachtraeglich aendert.
- Im Plan pruefen, ob `sourceEntryId` zugunsten der einheitlichen `originId`/`originType`-Felder umbenannt/vereinheitlicht wird (funktional identisch) — ohne Verhaltensaenderung.

---

## 7. Migration der Bestandsdaten (das Heikelste — mehrfach abgesichert)

Die fuenf DataStore-JSON-Typen ziehen nach Room. Pflicht-Absicherung pro Migrations-Etappe:

1. **Vor dem Umzug** automatisch ein frisches Drive-Backup anstossen.
2. **Einmalige, automatische Migration** beim ersten Start der neuen Version: JSON-Liste → Room-Tabelle, bestehende `id`s uebernehmen.
3. **Alte JSON-Listen werden NICHT geloescht**, sondern bleiben als Fallback liegen, bis die Migration verifiziert ist (Anzahl-Abgleich pro Typ: JSON-Eintraege == Room-Zeilen).
4. **Bestandsdaten haben keine Herkunft** (`originId`/`originType`/`rootId` = `null`) — korrekt, sie sind vor dem Umbau entstanden.
5. Entropie/Thesen/Journal: **kein** Umzug (schon in Room), nur Room-Schema-Migration (Felder ergaenzen).
6. Room-Schema: bei JEDER strukturellen Aenderung `version` erhoehen + echte `Migration(N,M)` (nie destruktiv); Schema-JSON exportieren/einchecken (Room-Best-Practice).

---

## 8. Backup & Sync

- **Feingranularer Sync bleibt** (Last-Write-Wins pro Eintrag + Tombstones bei Loeschung), laeuft jetzt einheitlich ueber Room statt ueber die JSON+Room-Mischung.
- **Jede neue Tabelle** wird in das Drive-Backup-Schema (`BackupPayload`) UND in die Sync-/Restore-Logik (`SyncCoordinator`, `SyncEntriesUseCase`, `TombstoneStore`) aufgenommen — sonst Datenverlust beim Geraete-Wechsel. (Memory-Regel `entropie_reductor_backup_schema_pflicht`.)
- **Backup-Schema-Version** pro Schritt korrekt hochzaehlen.
- **DB-Snapshot** (ganze Datenbank, mit `wal_checkpoint(TRUNCATE)` vor dem Kopieren) zusaetzlich als Disaster-Recovery — getrennt vom feingranularen Sync, ersetzt ihn NICHT.

---

## 9. Observability (Sonden ueberall — Frank-Vorgabe)

Gemaess Observability-First + Live-Logik-Sonden:

- **Live-Logik-Checkpoints** an jedem Ketten-Uebergang und jeder Migration, eigener Kanal (`kind:CHECKPOINT`, TAG z. B. `LOGIC`):
  - z. B. `CHECKPOINT step="Idee→Vorschlag" expected="origin=A1" actual="origin=A1" ok=true`
  - Migration: `CHECKPOINT step="Migration ideas" expected="json_count=N" actual="room_count=N" ok=true`
- **Strukturiertes Logging** (JSON-Lines) + globaler Fehler-Faenger bleiben/werden genutzt.
- Verifikation pro Etappe: „starte den Live-Logik-Check" → `adb logcat -s LOGIC` mitlesen, waehrend Frank die App bedient.
- **Unit-Tests** fuer: Herkunfts-Durchreichung an jedem Uebergang, `rootId`-Vererbung, Migration verlustfrei (Anzahl + Stichproben), Prio-Gedaechtnis `sourceEntryId`-Loesung beim Editieren.

---

## 10. Etappen-Reihenfolge (Claude entscheidet; schrittweise, commit+push+verify dazwischen)

1. **Fundament:** Identitaets-Bausteine (`originId`/`originType`/`rootId`) + Typ-Kennzeichen in die **bestehenden** Room-Tabellen (`entropy_entries`, Followups, `priority_memory`). Room-Migration. Risikoarm (kein Daten-Umzug). Sonden-Grundgeruest.
2. **Kern-Kette Aufgaben:** `ideas` + `task_suggestions` nach Room migrieren; Herkunft Idee→Vorschlag→Aufgabe durchreichen; `processed_idea_ids` abloesen; Backup-Schema + Sync mitziehen.
3. **Kern-Kette Gewohnheiten:** `habit_suggestions` + `habits` nach Room migrieren; Herkunft Idee→Gewohnheits-Vorschlag→Gewohnheit; `habit_processed_idea_ids` abloesen; Backup/Sync mitziehen.
4. **Mental-Saetze:** `mental_sentences` nach Room; Backup/Sync mitziehen.
5. **Weitere Ketten:** Entropie→Idee, These→Idee u. a. (soweit gewuenscht), gleiches Muster.

Jede Etappe endet mit: Build gruen → Version-Bump (sichtbar) → commit+push → Live-Logik-Check.

---

## 11. Nicht-Ziele / YAGNI

- **Keine** zentrale separate „Lineage-Tabelle" (Ansatz 2) — Herkunft lebt als Feld am Eintrag.
- **Keine** geteilte ID entlang der Kette (verletzt „keine doppelten IDs").
- **Keine** harte Ideen-Blockade beim Dedup.
- **Kein** „ganze DB ersetzen"-Sync (bricht Multi-Device).
- Kein Umbau des externen Journal-Spiegels.

---

## 12. Offene Punkte (im Plan zu entscheiden)

- `entropy_entries`: bestehendes `category`-Feld als Typ-Kennzeichen nutzen vs. explizites `type`-Feld einfuehren (nach Blick auf den echten Feldstand).
- `priority_memory`: `sourceEntryId` umbenennen/vereinheitlichen zu `originId`/`originType` (rein kosmetisch, funktionserhaltend) — ja/nein.
- Genauer Trigger, wann eine Idee (erstmalig) einen Vorschlag erzeugt, sodass kein Vorschlags-Spam entsteht, aber neue/aehnliche Ideen nie blockiert werden (Detail-Logik in Etappe 2).
