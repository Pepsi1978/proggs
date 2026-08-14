# Bau-Auftrag — StackLabor
Stand: 14.08.2026, 14:25 · Stufe: v2

## 1. Was gebaut wird

Eine Android-App, mit der Frank seine Nahrungsergänzungsmittel-Stacks zusammenstellt und gegen
selbst gesetzte Ziele prüfen lässt. Sechs Stacks als Zeit-Slots, 72 Einträge, zwölf Ziele, die je
Stack angehakt und per Drag & Drop priorisiert werden. Eine Codex-Auswertung liefert eine
gespeicherte Bewertungstabelle Mittel × Ziel; daraus rechnet die App alle Ampeln **lokal** —
Häkchen und Umsortieren wirken deshalb sofort, kostenlos und offline. Dazu die Vorlesefunktion aus
`PerfectMoment`/`EntropieReductor` mit drei Anbietern und allen Stimmen.

**Gebaut wird ausschließlich der Geräteinhalt.** Die Bühne des Entwurfs — Bildschirm-Index links,
Zustands-Schalter oben, Erläuterungsspalte rechts — ist eine Vorführhilfe des Designers und
gehört nicht zur App.

## 2. Zielplattform und Technik-Weg

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht |
|---|---|---|---|
| Android | Galaxy Z Fold 8 (SM-F971B) **zugeklappt = Leitgröße**: 1248 × 1972 px @ 420 dpi = **297 × 469 dp** | Kotlin + Jetpack Compose | ja |
| Android | Galaxy Z Fold 8 **aufgeklappt**: 1848 × 2448 px @ 420 dpi, 120 Hz = **440 × 583 dp**, zweispaltig | Kotlin + Jetpack Compose | ja |

> **Umrechnung der Messung:** `stil`-Werte sind **direkt dp/sp** (`15px` → `15.sp`).
> `kasten`-Werte sind **doppelt** — für dp halbieren.

Weitere Technik: Room (Datenhaltung) · Hilt oder ein schlanker eigener Container (wie
`PerfectMoment/di/AppContainer.kt`) · OkHttp für Codex · Media3/ExoPlayer für die Sprachausgabe.

## 3. Verbindliche Quellen

| Datei | Wofür verbindlich |
|---|---|
| `01-FUNKTIONS-SPEC.md` | **Verhalten, Daten, Regeln.** 31 Funktionen mit Ablauf, Fehlerfall und Grenzen; Datenmodell; die Ampel-Rechenregel (F-14); das JSON-Format für Codex (F-12) |
| `02-UI-SPEC.md` | **Jede Farbe, jedes Maß, jeder Bildschirm** — lesbare Fassung der Messung |
| `03-MOTION-SPEC.md` | **Jede Bewegung**, je mit `@keyframes`-Namen als Quelle |
| `messung/<erscheinung>/<B-xx>.json` | **Die Wahrheit für alle Werte.** 30 Dateien. Steht ein Wert dort und nicht im Spec, gilt die Messung |
| `bilder/<erscheinung>/<B-xx>.png` | **Der Augenschein für die Abnahme.** 30 Bilder — je Bildschirm ein Screenshot der gebauten App danebenhalten |
| `00-PROJEKT.md` | App-Name, Zielplattform, Rahmen, Ziel-Startbestand (§6), Abnahmekriterien |
| `../v1/STARTBESTAND.md` | Die 72 Einträge in 6 Stacks — Grundlage für `startbestand.json` in den Assets |
| `AENDERUNGEN.md` | Was der Designer gegenüber v1 geändert hat, und was er ausdrücklich anmerkt |
| `Designs/Outbox/StackLabor/StackLabor Entwurf.dc.html` | Der lauffähige Prototyp. Sein Skriptblock enthält die Ampel-Rechnung, die Datenkürzel und die Fortschrittserzählung im Original |

**Vorbild-Projekte im Repo — Code übernehmen, nicht neu erfinden:**

| Baustein | Herkunft |
|---|---|
| Codex-Anbindung (OAuth-Geräte-Flow, Anfragen, Fehlerklassen, Modellwahl) | `~/proggs/PerfectMoment/app/src/main/java/de/frank/perfectmoment/auth/CodexAuthManager.kt` + `CodexModels.kt` |
| Sprachausgabe (drei Anbieter, Stimmen-Katalog, Pegel) | `~/proggs/PerfectMoment/.../tts/` — `EdgeTtsPlayer`, `GoogleCloudTtsPlayer`, `QwenTtsPlayer`, `TtsManager`, `TtsCatalog`, `SpeechLoudness` |
| Weiterlaufen im Hintergrund, Verbrauchszählung | `~/proggs/EntropieReductor/.../data/tts/` — `TtsPlaybackService`, `TtsUsageStore`, `TtsUsageBackup` |
| Aufbau eines Compose-Projekts mit Room | `~/proggs/NEMS/` |

> **Achtung:** Der TTS-**Einstellungsbildschirm** aus `EntropieReductor` ist auf dessen Domäne
> zugeschnitten (Mentals, Gewohnheiten) und wird **nicht** übernommen. StackLabor bekommt einen
> eigenen (B-10) aus denselben technischen Bausteinen.

## 4. Abhakliste

Fertig ist der Bau erst, wenn **jede** Kennung im Quellcode nachweisbar ist.

**Bildschirme (15):**
B-01 Hauptbildschirm · B-02 Stack-Detail · B-03 Ziel-Katalog · B-04 Ziele dieses Stacks ·
B-05 Mittel bearbeiten · B-06 Aufschlüsselung · B-07 Auswertung im Vollbild · B-08 Eigene Fragen ·
B-09 Alle Stacks zusammen · B-10 Einstellungen · B-11 Codex-Anmeldung · B-12 Ziele ordnen ·
B-13 Stack bearbeiten · B-14 Mittel-Katalog · B-15 Auswertungs-Historie

**Funktionen (29 im Kern, 2 später):**
F-01 Stack anlegen/bearbeiten/löschen · F-02 Mittel hinzufügen mit nachlaufender Prüfung ·
F-03 Mittel bearbeiten · F-04 Mittel entfernen · F-05 Häkchen · F-06 Sortierung umschalten ·
F-07 Einnahme-Reihenfolge ziehen · F-08 Ziel anlegen · F-09 Ziele für Stack wählen ·
F-10 Ziele priorisieren · F-11 Eigene Fragen · F-12 Stack auswerten · F-13 Alle Stacks prüfen ·
F-14 Ampeln lokal rechnen · F-15 Aufschlüsselung · F-16 Vorlesen · F-17 Codex anmelden ·
F-18 Einstellungen · F-19 Export · F-20 Import · F-21 Startbestand einlesen · F-22 Hell/Dunkel ·
F-23 „veraltet"-Markierung · F-24 Suchen · F-27 Dosis-Variante · F-28 Kombi-Gruppe ·
F-29 Auswertungs-Historie · F-30 Katalog verwalten · **F-31 Kontextmenü (neu aus dem Entwurf)**
*(später: F-25 Mittel verschieben · F-26 Stack duplizieren)*

**Bewegungen (24):**
M-01 Ziel aufnehmen · M-02 Ausweichen · M-03 Nummern live · M-04 Einrasten · M-05 Auto-Rollen ·
M-06 Abbruch · M-07 Ampel-Überblendung · M-08 gestaffelte Ampeln · M-09 Puls · M-10 Verbindungsfarbe ·
M-11 Schimmer · M-12 entsättigtes Pulsieren · M-13 streamender Text · M-14 Überlagerung aufklappen ·
M-15 Sprech-Markierung + Pegel · M-16 Dauerbewegung · M-17 Faltvorgang · M-18 Mittel ziehen ·
M-19 Erscheinungswechsel · M-20 Blatt · M-21 Aura · M-22 gestaffeltes Einblenden ·
M-23 Häkchen-Rückmeldung · M-24 Wischen

**Abnahme (22):** A-01 … A-22 — siehe `00-PROJEKT.md` §5.

## 5. Offene Fragen vor dem Bau

**Keine, die den Bau aufhalten.** Die drei verbliebenen Punkte sind entschieden oder unkritisch:

| Nr | Punkt | Festlegung für den Bau |
|---|---|---|
| O-01 | Wortlaut des Auftragstexts an Codex | Wird beim Bau nach bestem Wissen formuliert; die Fortschrittserzählung und das JSON-Schema stehen fest. In B-10 später nachjustierbar |
| O-03 | Rolle des Durchfallrisikos in der Auswertung | Wird als Feld geführt und Codex mitgeteilt; keine eigene Logik in der App |
| O-04 | Löschen eines verwendeten Ziels | Warnung mit Nennung der betroffenen Stacks, danach Löschen samt Bewertungszellen |

O-02 (Dreier-Zyklus) hat der Designer gelöst: „alterniert mit" nimmt mehrere Partner auf.

## 6. Reihenfolge des Baus — Vorschlag

1. **Gerüst und Daten:** Projekt, Room-Schema, `startbestand.json` samt Einlesen (F-21), Katalog (F-30).
2. **B-01 und B-02** — die beiden Bildschirme, an denen Frank die App erlebt. Danach die erste Abnahme gegen `bilder/hell/B-01.png` und `B-02.png`.
3. **Ziele:** B-03, B-04, B-12 samt F-08 bis F-10 und der Ampel-Rechnung F-14 — ab hier lebt die App bereits ohne Netz.
4. **Codex:** B-11, F-17, dann F-12 mit dem JSON-Schema, B-07 und die Wartezustände.
5. **Der Rest:** B-05, B-06, B-08, B-09, B-13, B-14, B-15.
6. **Vorlesen:** F-16 mit den drei Anbietern, B-10.
7. **Bewegung und Feinschliff:** M-01 … M-24, reduzierte Bewegung, zweispaltiges Layout.
8. **Abnahme:** jeder Bildschirm gegen sein Bild in beiden Erscheinungen; danach A-01 … A-22 auf dem Gerät durchgehen.
