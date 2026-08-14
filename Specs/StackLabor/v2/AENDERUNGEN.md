# Änderungen durch den Designer — StackLabor
Stand: 14.08.2026, 14:25 · Design-Paket: `Designs/Outbox/StackLabor/` (aus `Stacklabor Claude Designs.zip`, 161.395 Byte) · Verglichen mit: `Specs/StackLabor/v1/`

## 0. Art des Rücklaufs

Kein Werft-Paket, sondern ein **Claude-Design**: zwei byteidentische `.dc.html` („StackLabor
Entwurf" und „StackLabor Prototyp", je 140.225 Byte), `support.js` als Laufzeit, ein
`.thumbnail`, und in `uploads/` die sieben hochgeladenen Spec-Dateien **byteweise unverändert
zurück**. Der Designer hat die Specs also nicht fortgeschrieben, sondern einen
**durchklickbaren Prototyp** gebaut.

Der Entwurf enthält nicht nur Gestaltung, sondern **funktionierende Logik**: den vollständigen
Datenbestand (6 Stacks, 72 Einträge mit Kürzeln für Durchfallrisiko, Dosis-Variante,
Kombi-Gruppe und Alternierung), zwölf Ziele mit stackweiser Priorisierung, die Ampel-Rechnung
aus F-14, Störungs- und Konkurrenz-Beispiele, Drag & Drop, Wischen mit Rückgängig, Zustände für
Auswertung, „veraltet", offline und nicht angemeldet.

**Vermessung:** Da der Prototyp durchklickbar ist, wurde er nicht statisch, sondern **klickend**
vermessen — 15 Bildschirme × 2 Erscheinungen = **30 Messungen** in
`v2/messung/<erscheinung>/<B-xx>.json`, dazu 30 Bilder in `v2/bilder/`. Der erste Versuch mit
`messe-design.ps1` lieferte nur unaufgelöste Platzhalter (`{{ b.name }}`), weil dessen
Wartezeit von 900 ms für den React-Aufbau nicht reicht; die Messung wurde mit 9 s Ladezeit und
1,4 s je Bildschirmwechsel wiederholt.

## 1. Bildschirme

| Kennung | v1 | im Rücklauf | Bewertung |
|---|---|---|---|
| B-01 | Hauptbildschirm | Hauptbildschirm | unverändert |
| B-02 | Stack-Detail | Stack-Detail | unverändert |
| B-03 | Ziel-Katalog | Ziel-Katalog | unverändert |
| B-04 | Ziele dieses Stacks | Ziele dieses Stacks | unverändert |
| B-05 | Mittel bearbeiten | Mittel bearbeiten | unverändert |
| B-06 | Aufschlüsselung | Aufschlüsselung | unverändert |
| B-07 | Auswertung im Vollbild | Auswertung im Vollbild | unverändert |
| B-08 | Eigene Fragen | Eigene Fragen | unverändert |
| B-09 | Alle Stacks zusammen | Alle Stacks zusammen | unverändert |
| B-10 | Einstellungen | Einstellungen | unverändert |
| B-11 | Codex-Anmeldung | Codex-Anmeldung | unverändert |
| B-12 | Ziele ordnen | Ziele ordnen | unverändert |
| B-13 | Stack bearbeiten | Stack bearbeiten | unverändert |
| B-14 | Mittel-Katalog | Mittel-Katalog | unverändert |
| B-15 | Auswertungs-Historie | Auswertungs-Historie | unverändert |

**Kein Bildschirm entfallen, keiner neu.** Der Entwurf folgt der Bildschirmliste aus v1 genau.

## 2. Neue Bedienelemente ohne Funktion in v1

| Bildschirm | Element | Was es tun soll | Kennung |
|---|---|---|---|
| B-02 | Kontextmenü bei langem Drücken in der Ansicht „Löslichkeit" | Öffnet **Bearbeiten** (→ B-05), **Gruppe bilden** (→ F-28), **Entfernen** (→ F-04). Verhindert den toten Ziehversuch in der Ansicht, in der nicht gezogen werden kann | **F-31** (neu) |

Der Designer hat dieses Element in seiner Anmerkung 3 selbst erklärt — es musste nicht
nachgefragt werden.

## 3. Geänderte Gestaltung

Der Entwurf hat die v1-Absicht **bestätigt statt überschrieben**. Nachgemessen:

| Punkt | v1-Absicht | Gemessen | Bewertung |
|---|---|---|---|
| Mittel-Karte | 273 × 56 dp, Radius 12 dp | 273 × 56 dp, Radius 12 dp | exakt getroffen |
| Mittel-Name | 15 sp, Gewicht 500 | 15 px, Gewicht 500 | exakt getroffen |
| Kartenschatten | „2 dp" | `0 2px 6px rgba(15,23,42,.10)` | präzisiert |
| Textfarbe stark / schwach | `#0F172A` / `#64748B` | `rgb(15,23,42)` / `rgb(100,116,139)` | exakt getroffen |
| Akzent | `#4F46E5` | `rgb(79,70,229)` | exakt getroffen |
| Ampelfarben hell | `#047857` / `#D97706` / `#DC2626` / `#94A3B8` | identisch | exakt getroffen |
| Ampel als Kantenbalken | 3 dp links | 3 dp links, Radius `2px 0 0 2px` | präzisiert |
| Löslichkeitspunkt | 8 dp, fettlöslich mit Rand | 8 dp, Rand 1,5 dp `#64748B` | präzisiert |
| Schriftfamilie | „Inter oder Systemschrift" | **Inter** + Material Symbols Rounded | festgelegt |
| Kopfbereich B-01 | 96 dp | **97 dp** | geringfügig geändert |
| Leiste „Alle Stacks" | 48 dp | **49 dp** | geringfügig geändert |
| Stack-Karte | 76 dp | **78 dp** | geringfügig geändert |
| Plus-Knopf | 56 dp | **57 dp**, Radius 28 dp | geringfügig geändert |
| Gerät-Eckenradius | nicht festgelegt | 24 dp | ergänzt |
| Kopfverlauf | „animierter Farbverlauf" | `linear-gradient(110deg, #4F46E5, #0EA5E9, #4F46E5)` | präzisiert |

**Keine v1-Gestaltungsaussage wurde vom Entwurf widerlegt.** Es musste daher auch kein Satz aus
v1 gestrichen werden — der Fall, der in einem früheren Durchlauf die Tiefen-Schicht gekostet hat,
tritt hier nicht ein.

## 4. Geänderte Bewegung

v1 beschrieb 24 Bewegungen als Absicht. Der Entwurf setzt **15 Keyframes** um und ergänzt
gemessene Werte:

| Bewegung | v1 | Gemessen |
|---|---|---|
| Ampel-Überblendung | 320 ms `cubic-bezier(0.4,0,0.2,1)` | identisch (`transition: background-color .32s`) |
| Erscheinungswechsel | 420 ms | identisch (`transition` auf `background-color`, `color`, `border-color`) |
| Blatt von unten | 300 ms `cubic-bezier(0.05,0.7,0.1,1)` | identisch (`@keyframes blatt`) |
| Gestaffeltes Einblenden | 40 ms Versatz | **30 ms** Versatz (`rein`, gemessen 0,03 … 0,2 s) |
| Aura an roter Ampel | 2400 ms | identisch (`@keyframes atem`), zusätzlich Skalierung 1 → 1,9 |
| Plus-Knopf atmet | 3200 ms, 1,0 → 1,02 | identisch (`@keyframes fabatem`) |
| Kopfverlauf | 30 s | identisch (`@keyframes wandern`) |
| Glanzkante | 8 s | identisch (`@keyframes glanz`) |
| Vollbild von unten | nicht beziffert | **320 ms**, Y +48 dp (`@keyframes hoch`) |
| Bildschirmwechsel vor / zurück | 300 / 260 ms | **300 ms** beide; Rücklauf über kürzere Strecke (24 statt 32 dp) |
| Puls an geänderter Ampel | 520 ms, Radius +6 dp | **520 ms**, Skalierung 1 → 2,6 (`@keyframes puls`) |
| Warte-Schimmer | 1400 ms | identisch (`@keyframes schimmer`), X −140 % → 240 % |
| Entsättigtes Pulsieren | 1600 ms | identisch (`@keyframes m12`) |
| Pegelbalken | „drei Balken" | `@keyframes pegel`, Höhe 4 → 14 → 5 px |
| Pulsring Anmeldung | nicht vorgesehen | **neu**: `@keyframes ring`, 1,6 s |

**Neu gegenüber v1:** `ring` (Wartezeile B-11).
**Im Entwurf nicht gebaut:** M-17 (Faltvorgang) — der Prototyp zeigt nur die zugeklappte
Leitgröße. Bleibt als Absicht aus v1 gültig und ist als solche gekennzeichnet.
**Gehört nicht zur App:** `sc-shine` — ein Effekt der Designer-Bühne.

## 5. Entfallenes

**Nichts.** Der Designer hat keinen Bildschirm, kein Bedienelement und keine Funktion
weggelassen. Alle `B-`, `F-` und `M-`-Kennungen aus v1 haben im Entwurf eine Entsprechung.

## 6. Was der Rücklauf zusätzlich mitbringt

| Zugewinn | Bedeutung für den Bau |
|---|---|
| **12 Ziele** samt Zuordnung und Reihenfolge je Stack | Von Frank am 14.08.2026 als Startbestand bestätigt → `00-PROJEKT.md` §6, F-21 |
| **Kürzel im Datenbestand** (`R` Durchfallrisiko, `V75` zweite Dosis, `k1` Kombi-Gruppe, `alt:` Alternierung) | Bestätigt das Datenmodell aus v1 und liefert das Format für `startbestand.json` |
| **Ampel-Rechnung als lauffähiger Code** (Zielgewicht 3/2/1, Schwellen für rot/gelb/grün/grau) | Bestätigt F-14 eins zu eins; der Bau kann sich daran ausrichten |
| **Fortschrittserzählung** in sechs Schritten | Wörtlich übernommen in `03-MOTION-SPEC.md` M-13 |
| **Störungs- und Konkurrenz-Beispiele** (Kaffee ↔ Eisen, Zink ↔ Kupfer u. a.) | Nur Vorführdaten des Prototyps — in der App liefert Codex diese Werte |
| **Erläuterung je Bildschirm** (Zweck + drei Punkte) | Diente der Prüfung, dass jeder Bildschirm seinen Zweck behalten hat |

## 7. Anmerkungen des Designers im Wortlaut

| Marke | Anmerkung | Behandlung |
|---|---|---|
| NEU | „Bühne links und rechts gehört nicht zur App: Bildschirm-Index und Zustands-Schalter sind nur zum Durchklicken da." | Übernommen: `00-PROJEKT.md` §4 schließt die Bühne ausdrücklich aus. Gemessen wurde nur der Geräteinhalt |
| NEU | „Die zwölf Ziele im Katalog sind Beispieldaten — der Spec nennt keinen Ziel-Bestand, nur „Senolytika" und „Sport" als Beispiele." | Frank gefragt am 14.08.2026 → **als Startbestand übernommen** |
| NEU | „B-02: langes Drücken in der Ansicht „Löslichkeit" öffnet ein Kontextmenü (Bearbeiten · Gruppe bilden · Entfernen) statt eines toten Ziehversuchs." | Übernommen als **F-31** |
| OFFEN | „O-02: „alterniert mit" nimmt hier mehrere Partner auf — der Dreier-Zyklus Citicolin ↔ Uridin + PS ist so abgebildet." | **Offene Frage O-02 aus v1 damit gelöst**; das Feld nimmt eine Liste auf |
