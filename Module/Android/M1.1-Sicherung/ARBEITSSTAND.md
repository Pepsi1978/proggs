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

## Fertig

| Datei | Zeilen | Entkopplung |
|---|---|---|
| `Vertrag.kt` | ~120 | neu: `SicherungsTeil`, `SicherungsInhalt`, `SicherungsRuecknahme`, `SicherungsProtokoll`, `SicherungsNamen`, `Einspielspur` |
| `BackupStatus.kt` | 98 | keine nötig — war schon frei |
| `AutoSicherung.kt` | 138 | `KompassLog` → `protokoll` (Vorgabe: still) |
| `DateiSicherung.kt` | 379 | `AppProfil` → `namen`; Muster aus `companion` in die Instanz |

## Offen

| Schritt | Umfang |
|---|---|
| `Sicherungsrahmen.kt` — Kopf/Fußzeile/Prüfsumme aus `Sicherung.kt` | ~250 Zeilen |
| `SicherungsDienst.kt` — `KompassRepository` → `SicherungsInhalt` | 192 Zeilen |
| `SicherungsOberflaeche.kt` — aus `EinstellungenScreen.kt` herauslösen | ~170 Zeilen |
| Anbindung in `KompassKern` — Satz-Serialisierung, Teile, Einspielen | ~400 Zeilen |
| Alten Code entfernen, Aufrufstellen ziehen | — |
| **Abnahme:** drei Apps bauen **und** eine alte Sicherungsdatei einspielen | — |

## Zustand des Repos

**Sicher.** Bisher sind nur neue Dateien unter `Module/` entstanden.
`KompassKern` ist unberührt, alle drei Kompass-Apps bauen unverändert.
