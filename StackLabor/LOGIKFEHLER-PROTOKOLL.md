# LOGIKFEHLER-PROTOKOLL — StackLabor

- Software: StackLabor (`C:\Users\barwa\proggs\StackLabor`), Android/Kotlin/Compose
- Datum: 11.09.2026 — Zweig: main (direkt, per Profil kein Feature-Branch)
- Build: `./gradlew :app:assembleDebug` — Tests: Schnellmodus, keine Testläufe (User prüft selbst)
- Basislinie: ungebaut vor Fix-Charge; Review rein lesend, 77 Kotlin-Dateien kartiert

## Karte (Kurzform)

| Bereich | Ebene | Risiko | Zweck |
|---|---|---|---|
| Ampel/Prüfsumme (`domain/`) | Geschäftslogik | hoch | Ampeln, Prüfsumme, Anzeigen |
| Repository/Transfer (`data/repository`, `data/transfer`) | Daten/Persistenz | hoch | CRUD, Import/Export, Merge |
| DAOs/Entities (`data/local`) | Daten/Persistenz | mittel | Room, Kaskaden |
| Codex-Dienste (`service/codex`, `service/auth`, `service/network`) | Netzwerk | hoch | Auswertung, JSON, Retry, Konkurrenz |
| TTS/Speech (`service/tts`, `service/speech`) | Plattform | niedrig | Vorlesen, Transkription |
| ViewModel/App (`StackLaborViewModel`, `AppContainer`) | Zustandsverwaltung/Oberfläche | hoch | UI-State, Tagesdosen, Historie |
| UI-Screens/Navigation (`ui/`) | Oberfläche | mittel | 15 Screens, Routen, Sortierung |

## Loop-Zustand

- Aktuelle Runde: 3 (Stufe 3, Grenzen/Zustand/Zeit) — abgeschlossen
- Konvergenzzähler: 0 (Runde 3 brachte neue bestätigte Funde)
- Nächster Schritt per Auftrag: Runde 4 (Stufe 4, Invarianten und Gegenbeweis)
- Offene Fixe: keine — alle bestätigten Funde behoben und gebaut

## Funde

| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurzinfo |
|---|---|---|---|---|---|---|---|
| F1 | 1 | Ampel | `StackLaborRepository.kt: berechneAmpeln` | Zustand | hoch | verifiziert | Leere Zellen → Mittel GRÜN/Sammel GELB statt GRAU |
| F2 | 1 | ViewModel | `StackLaborViewModel.kt: dailyDoses` | Arithmetik/Einheiten | hoch | verifiziert | Stück-Summe ohne Alle-N-Tage-Durchschnitt, falsche Einheit bei Mix |
| F3 | 1 | ViewModel | `StackLaborViewModel.kt: toHistoryUi` | Oberflächenlogik | mittel | verifiziert | Historie kennt kein GELB, widerspricht historySignal |
| F4 | 1 | Repository | `StackLaborRepository.kt: vereinige` | Daten/Persistenz | hoch | verifiziert | DAZU dupliziert Wirkstoffkomponenten vorhandener Mittel |
| F5 | 1 | Repository | `StackLaborRepository.kt: vereinige` | Schnittstellenvertrag | mittel | verifiziert | getValue-Absturz bei verwaisten Referenzen statt klarer Meldung |
| F6 | 1 | Persistenz | `BewertungDao.kt`, `StackLaborRepository.kt: loescheEintrag/uebertrageMittel` | Daten/Persistenz | hoch | verifiziert | Alte Konkurrenzen bleiben nach Eintrag-Löschung/Verschieben |
| F7 | 1 | ViewModel | `StackLaborViewModel.kt: DeleteMedicine` | Berechtigungen/Datenverlust | hoch | verifiziert | Katalog-Löschen per Kaskade ohne Undo |
| F8 | 1 | Anzeige | `DeutscheAnzeigen.kt: toEinheitOrNull` | Randfall Unicode | niedrig | verifiziert | Griechisches μ (U+03BC) nicht erkannt |
| F9 | 1 | Prüfsumme | `Pruefsumme.kt: berechnen` | Standardwerte | niedrig | verifiziert | Blank-einheitText ohne takeIf, inkonsistent zur Anzeige |
| F10 | 2 | Codex | `CodexJson.kt: decodeStringFieldPrefix` | Schnittstellenvertrag | mittel | verifiziert | `"gesamt"` im Fließtext wird als Schlüssel missdeutet |
| F11 | 3 | ViewModel | `StackLaborViewModel.kt: applyGlobal` | Zustand/Lebenszyklus | hoch | verifiziert | Gelöschter Stack bleibt ausgewählt (Geister-Anzeige) |
| F12 | 3 | ViewModel | `StackLaborViewModel.kt: scheduleCompetitionCheck` | Fehlerbehandlung | mittel | verifiziert | Catch-Block stürzt bei gelöschtem Stack ab |
| F13 | 3 | Codex | `CompetitionCheckCoordinator.kt` | — | — | abgelehnt | Klasse ist ungenutzter Code (kein Aufrufer); kein Ausführungspfad, kein Fehler |
| F14 | 3 | Persistenz | `StackDao.kt`, Repository, ViewModel `ToggleMedicine` | Nebenläufigkeit | mittel | verifiziert | Doppel-Tap liest zweimal denselben Stand, Toggle wirkt nur einmal |
| F15 | 3 | ViewModel | `StackLaborViewModel.kt: startCodexLogin` | Nebenläufigkeit | niedrig | verifiziert | Doppel-Tap startet parallele Device-Auth-Flows |

### F1 — Beweis/Fix
- Eingabe: Auswertung schlägt nach Repair fehl → Bewertung mit leeren Zellen gespeichert. Ist: Mittel GRÜN, Sammel GELB. Soll (SPEC §10): Ampeln bleiben grau. Fix: `keineDaten = bewertung == null || zellen.isEmpty()` → überall GRAU, `sammelAmpel(..., !keineDaten)`.

### F2 — Beweis/Fix
- Eingabe: Eintrag 2 Stück alle 2 Tage ohne Mengenangabe. Ist: `doses.sumOf{stueckzahl}` = 2 Stück. Soll (SPEC §14.2): Durchschnitt = 1 Stück. Zusätzlich: bei gemischten Einheiten fiel Stück-Summe mit Mengeneinheit (z. B. „2 mg“) heraus. Fix: `stueckProTag` mit Division durch N (Fallback 2 wie Anzeige), Else-Zweig immer „Stück“, `?: 1` → `?: 2`.

### F3 — Beweis/Fix
- Eingabe: Ziel Saldo 1 ohne Störung. Ist Historie: GRÜN; Hauptansicht: GELB. Soll: gleiche Logik. Fix: `toHistoryUi` nutzt ROT ab Stärke 3 / GELB bei Störung / GRÜN sonst — wie `historySignal`.

### F4 — Beweis/Fix
- Eingabe: DAZU-Import enthält Mittel M1 (bekannt) mit Komponente K1 (bekannt). Ist: neue UUID-Komponente dupliziert K1. Soll: Bestand unverändert. Fix: nur Komponenten importieren, deren `mittelId` zu den *neuen* Mitteln gehört.

### F5 — Beweis/Fix
- Eingabe: manipulierter Import, Dosis verweist auf unbekannten Eintrag. Ist: `getValue` wirft `NoSuchElementException`. Soll: klare `require`-Meldung. Fix: `require(... in ...)` für Einträge/Dosen/Partner.

### F6 — Beweis/Fix
- Eingabe: Eintrag löschen oder Mittel verschieben. Ist: `loescheZellenFuerStackMittel` löscht Zellen, Konkurrenzen mit dem Mittel bleiben. Soll: keine verwaisten Konkurrenzhinweise. Fix: neue DAO-Methode `loescheKonkurrenzenFuerStackMittel`, Aufruf in beiden Transaktionen.

### F7 — Beweis/Fix
- Eingabe: Mittel im Katalog löschen, das in Stacks steckt (CASCADE löscht Einträge mit). Ist: kein Undo, Einträge still weg. Soll: rückholbar wie Zusammenführen. Fix: `deleteMedicine()` mit Exportsicherung + `undoAction` (Muster von `mergeMedicines`).

### F8 — Beweis/Fix
- Eingabe: Einheit „μg“ mit griechischem Mu. Ist: kein Treffer → Freitext. Soll: UG. Fix: zusätzlich `.replace("μ", "U")`.

### F9 — Beweis/Fix
- Eingabe: `einheitText = " "` (blank). Ist Prüfsumme: `""` statt Einheitscode. Soll: wie Anzeige auf Code fallen. Fix: `takeIf(String::isNotBlank)`.

### F10 — Beweis/Fix
- Eingabe: Rohtext, in dem `"gesamt"` im Fließtext vorkommt, bevor der echte Schlüssel steht. Ist: Prefix-Decoder nimmt ersten Treffer. Soll: nur Treffer mit nachfolgendem Doppelpunkt. Fix: Schleife mit Doppelpunkt-Prüfung, sonst weitersuchen.

### F11 — Beweis/Fix
- Eingabe: gewählten Stack löschen. Ist: `selectedStackId` zeigt weiter auf die gelöschte ID, `applySelected` bricht mit `?: return` ab — Kopf, Mittel und Ziele bleiben als Geist stehen. Soll: auf ersten übrigen (oder keinen) wechseln. Fix: in `applyGlobal` fehlende Auswahl zurücksetzen.

### F12 — Beweis/Fix
- Eingabe: Mittel hinzufügen, Stack innerhalb von 3 s löschen. Ist: Catch-Block ruft `ladeStackInhalt` auf, das erneut wirft — uncaught im Scope. Soll: still aufgeben. Fix: `runCatching` um den Hinweis-Fallback.

### F13 — abgelehnt
- Vermuteter Catch-Absturz im `CompetitionCheckCoordinator` — bei Prüfung per Suche (grep): die Klasse wird nirgends instanziiert (Aufrufer: keine), die lebende Prüfung steht in `StackLaborViewModel.scheduleCompetitionCheck` (siehe F12). Ohne Ausführungspfad kein Logikfehler.

### F14 — Beweis/Fix
- Eingabe: Häkchen doppelt tippen, bevor der Flow neu emittiert. Ist: beide Taps lesen `aktiv=true`, beide schreiben `false` — Ende `false` statt `true`. Soll: zwei Taps heben sich auf. Fix: neue DAO-Anweisung `UPDATE … SET aktiv = NOT aktiv`, Repository `schalteEintragAktivUm`, Toggle nutzt sie.

### F15 — Beweis/Fix
- Eingabe: Codex-Anmelden doppelt tippen. Ist: zwei parallele Device-Auth-Flows (doppelte Polls/Meldungen). Soll: zweiter Tap wirkungslos. Fix: `codexLoginJob`-Guard mit Rücksetzung im `finally`.

## Rundenübersicht

| Runde | Stufe | Alle Bereiche geprüft | gemeldet/bestätigt/behoben/verifiziert | Build | Zähler danach |
|---|---|---|---|---|---|
| 1 | 1 Überblick | ja (77 Dateien, Kern gelesen, Rest per Suche) | 9/9/9/9 | assembleDebug OK | 0 |
| 2 | 2 Funktion-für-Funktion | ja (Änderungsstellen + Codex/TTS/UI-Rest) | 1/1/1/1 | assembleDebug OK | 1 |
| 3 | 3 Grenzen/Zustand/Zeit | ja (Aufrufer, Lebenszyklus, Wettläufe, Doppel-Tap, Netzverlust) | 5/4/1 abgelehnt/4/4 | assembleDebug OK | 0 |

## Klärungsbedarf

- K1: `saveMedicine` kopiert Frei-Werte in die Dienst-Dosis (keine getrennt editierbaren Dienst-Werte im Draft). SPEC F-72 verlangt „aktiviert und bearbeitet die zweite Dosisvariante“. Empfehlung: Draft um Dienst-Felder erweitern oder bestätigen, dass Kopie Absicht ist. Kein Fix vorgenommen.
