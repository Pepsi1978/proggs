# Second-Brain-Einstellungsbereich + Ideen-Sync — Design

**Stand:** 2026-07-04 · **Status Etappe 1:** umgesetzt (v0.20.0) · **Etappe 2:** offen

## Ziel (Frank-Wunsch 2026-07-04)

Ein eigener Einstellungsbereich „Second Brain" in EntropieReductor (nicht im Dashboard),
getrennt vom „API-Schlüssel"-Bereich. Darin Schalter pro Bereich (aktuell nur „Ideen",
erweiterbar). Bei aktivem „Ideen"-Schalter werden Ideen bidirektional mit dem Second Brain
synchron gehalten.

## Format eines Ideen-Eintrags im Brain

- **Titel** = der Ideen-Titel (z. B. „Frage Meditation ausprobieren"), NICHT die interne ID.
- **Text** (reiner, lesbarer Text, keine Rauten/Markdown):
  ```
  Erstellt am: <dd.MM.yyyy, HH:mm Uhr>
  Aktualisiert am: <dd.MM.yyyy, HH:mm Uhr>

  Idee: <verbesserte Fassung, sonst Original>
  Zusammenfassung: <summary, nur wenn vorhanden>
  ```
- Keine Nachträge (Followups) im Brain-Text. Kategorie im Brain: „Ideen".

## Architektur

- **Verbindung** (Bearer-Key + WireGuard-Config) bleibt im Bereich „API-Schlüssel".
- **Inhalts-Schalter** (welche Bereiche synchronisiert werden) im neuen Bereich „Second Brain".
- Der Titel ist im Brain der **eindeutige Schlüssel** (`/store` überschreibt per Titel).
  Konsequenz: gleichnamige Ideen kollidieren (neuere gewinnt) — bewusst akzeptiert.

## Etappe 1 (umgesetzt v0.20.0)

1. `SecondBrainApi.forget()` — DELETE /by-title (Client für das serverseitige Löschen).
2. `AppSettings` — merkt pro Idee-ID den zuletzt hochgeladenen Titel (`second_brain_idea_titles`,
   Format `id=title`); `readSecondBrainIdeaTitles` / `setSecondBrainIdeaTitle` / `clearSecondBrainIdeaSync`.
3. `SecondBrainIdeaConnector` — Titel = Ideen-Titel; neues Textformat; Löschungs-Erkennung
   (in App gelöschte Idee → per Titel aus Brain entfernen); Titel-Änderung → alten Eintrag entfernen.
4. Neuer `SecondBrainSettingsScreen` + `SecondBrainSettingsViewModel`, Route `SETTINGS_SECOND_BRAIN`,
   Karte in `SettingsHomeScreen`, im NavGraph verdrahtet.
5. Ideen-Schalter + Sync-Button aus `ApiKeysScreen` entfernt (dort bleibt nur die Verbindung).

Richtung App → Brain (Upload, Update, Löschung, Titel-Änderung) ist damit vollständig.
Der Server hat alles Nötige (`/store`, `DELETE /by-title`, `/by-category`) — kein Server-Umbau.

## Etappe 2 (umgesetzt, v0.20.6) — Brain → App

Im Brain (Kategorie „Ideen") neu angelegte Einträge werden in die App geholt (via `/by-category`),
mit Titel-Abgleich + Tombstones (uploadedTitles) gegen Wiederauferstehung. Import mit Brain-Zeitstempel,
nur Text. Löschung Brain→App: im Brain entfernte, in der App synchronisierte Ideen werden auch in der
App gelöscht — abgesichert durch `health.ready` (Ladefenster-Schutz gegen fälschliches Massenlöschen).
Trigger: beim App-Start (MainActivity) UND beim Betreten des Ideen-Reiters (IdeenScreen + IdeenBrainSyncViewModel),
je mit Retry für den Tunnel-Aufbau. Alle vier Richtungen live verifiziert.

## Etappe 3 (offen, To-Do 2026-07-04) — dasselbe für ALLE weiteren Listen

Frank-Wunsch: den kompletten Ideen-Sync **1:1 auf alle weiteren App-Listen** ausrollen, je mit
eigenem An/Aus-Schalter im Second-Brain-Einstellungsbereich:
- **Entropie / „Forscher"** (EntropyEntryEntity / EntropyEntryDao)
- **Thesen**
- **Journal** (Tagebuch / JournalMirror)
- **Gewohnheiten** (HabitEntity / HabitDao)
- **Mental** (MentalEntity / MentalSentenceDao)

Jeweils volle bidirektionale Synchronität (rein UND raus) wie bei Ideen — eigene Brain-Kategorie,
Import/Upload/Löschung beidseitig, Ladefenster- + Tombstone-Schutz, Resync-Knopf.

**Umsetzungs-Hinweis:** Den `SecondBrainIdeaConnector` **generisch parametrisieren** (Kategorie +
jeweilige DAO + **EIGENE** Settings-Marken-Keys pro Liste — sonst vermischen sich Sync-Stamps/Tombstones)
statt N Kopien. Schalter je Liste im `SecondBrainSettingsScreen`, Pull-Trigger je Reiter-Screen.
(Auch als Projekt-Memory `second-brain-all-lists-sync-todo` festgehalten.)
