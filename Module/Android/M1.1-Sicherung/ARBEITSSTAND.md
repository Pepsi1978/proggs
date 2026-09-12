# M1.1 Sicherung — Arbeitsstand

**Stand: 12.09.2026** · Herausgelöst aus `KompassKern` (geteilt von ClaudeKompass,
CodexKompass, OpenCodeKompass).

> Diese Datei verschwindet, sobald das Modul fertig ist. Bis dahin hält sie fest,
> was entschieden wurde — damit die Arbeit an jeder Stelle fortsetzbar bleibt.

## Der Schnitt

> **Das Modul besitzt den Umschlag und den Ablauf. Die App besitzt den Inhalt.**

| Bereich | Wohin |
|---|---|
| Prüfsumme, Kopf, Fußzeile, satzweises Streamen | Modul |
| Ordnerverwaltung, Aufräumen, Namensmuster (SAF) | Modul |
| Autosicherung mit Ruhezeit und Hintergrund-Auslöser | Modul |
| Statusanzeige | Modul |
| Oberfläche: Auswahlmenü, Schalter, Ordner, Vorschau | Modul |
| **Satz-Serialisierung** (welche Felder ein Eintrag hat) | **App** |
| **Welche Teile es gibt** (Slash, Config, Praxis, Fragen, Gespräche) | **App** |
| **Einspielen und Zurücknehmen** (was ein Duplikat ist) | **App** |

## Befund zur Formattreue

Die Datei hat auf oberster Ebene **feste Feldnamen**, die das Kompass-Modell
abbilden:

```json
{ "schema": 3, "app": "…", "erstelltAm": "…", "roomVersion": N,
  "umfang": ["slash", …],
  "eintraege": [ … ], "fragen": [ … ], "sitzungen": [ { …, "nachrichten": [ … ] } ],
  "anzahl": { "eintraege": N, "fragen": N, "sitzungen": N, "nachrichten": N },
  "jeBereich": { … }, "pruefsumme": "…" }
```

**Folge:** Kopf und Fußzeile schreibt das Modul, die benannten Nutzlast-Felder
dazwischen schreibt die App. So bleibt die Datei **Byte für Byte** wie bisher —
alte Sicherungen bleiben einspielbar, was der Benutzer ausdrücklich verlangt hat.

`anzahl` wird im Modul zu `Map<String, Int>`, weil die vier Schlüssel
kompass-eigen sind. Den Satz „Nächste Sicherung: …" formt die Anbindung.

## Zwei Dinge, die nicht verändert werden dürfen

1. **`kompass_backup_status` / `sicherungs_ordner`** — die `SharedPreferences`,
   in denen der gewählte Sicherungsordner steht. Ein anderer Name heißt: Der
   Benutzer hat seinen Ordner verloren, ohne Meldung. Wird über
   `SicherungsNamen` unverändert hereingereicht.
2. **Die Dateinamensmuster** — auch die früheren Präfixe, sonst werden alte
   Sicherungen im Ordner nicht mehr gefunden.

## Fertig — das Modul ist vollständig

| Datei | Zeilen |
|---|---|
| `Vertrag.kt` | 198 |
| `Sicherungsrahmen.kt` | 285 |
| `DateiSicherung.kt` | 392 |
| `SicherungsDienst.kt` | 192 |
| `SicherungsSteuerung.kt` | 296 |
| `AutoSicherung.kt` | 146 |
| `BackupStatus.kt` | 109 |
| **gesamt** | **1.618** |

Gegenprobe: `grep` nach `de.frank.kompass`, `KompassLog`, `AppProfil`,
`KompassDatabase`, `Entity` im Modulordner — **kein Treffer**.

### Die Oberfläche: bewusst ohne Compose

Der UI-Block in `EinstellungenScreen.kt` benutzt Kompass-eigene Bausteine
(`Block`, `Mehrfachauswahl`, `Schalterzeile`, `LocalKompassFarben`). Sie ins
Modul zu ziehen hätte das ganze Gestaltungssystem mitgeschleppt.

Stattdessen liefert `SicherungsSteuerung` den Zustand und die Aktionen, und
**jede App zeichnet mit ihren eigenen Bausteinen**. Das erfüllt die Vorgabe
unmittelbar: Funktionen 1:1, Aussehen passt sich an.

## Zwischenfall: parallele Änderung am Quellcode

Während der Herauslösung hat eine andere Sitzung `5a67dc8e5` committet
(„acht Leistungsbremsen im Sicherungsmodul beseitigt") und dabei
`BackupStatus.kt`, `DateiSicherung.kt` und `SicherungsDienst.kt` geändert.
Außerdem wurde `OpenCodeKompass` zu `OCodeKompass` umbenannt.

**Geprüft:** Die Modulkopien stammen vom Stand *nach* diesem Commit. Ein
normalisierter Vergleich gegen den heutigen `KompassKern` zeigt 39 abweichende
Zeilen — alle davon eigene Entkopplungen (Konstruktorparameter, aus dem
`companion` gezogene Muster, Protokoll). Es fehlt nichts.

## Offen — Phase 5 und 6

| Schritt | Umfang |
|---|---|
| Anbindung in `KompassKern`: `SicherungsInhalt` mit der Satz-Serialisierung | ~350 Zeilen |
| `EinstellungenViewModel` auf `SicherungsSteuerung` umstellen | ~127 Zeilen |
| `EinstellungenScreen` auf den neuen Zustand ziehen | ~170 Zeilen |
| `KompassRepository`: `EinspielSenke` → `SicherungsRuecknahme` | — |
| Alten Code entfernen | 5 Dateien |
| **Abnahme:** drei Apps bauen **und** eine alte Sicherungsdatei einspielen | — |
| `BEISPIEL.md`, `INDEX.md`, Konsumententabelle, Regel 9 | — |

## Zustand des Repos

**Sicher.** Bisher sind nur neue Dateien unter `Module/` entstanden.
`KompassKern` ist unberührt, alle drei Kompass-Apps bauen unverändert.
