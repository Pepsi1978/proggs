# Logikfehler-Jagd — Experimente

1. Kopf: Software Experimente (Android, Kotlin/Compose, `Experimente/`), Datum 11.09.2026, Arbeitsbranch `main`, Remote-Branch `origin/main`, Ausgangscommit 761b7649e, Build `./gradlew :app:assembleDebug --offline`, Tests keine vorhanden (kein `src/test`, kein `*Test*.kt`). Basislinie: Build grün (EXIT 0, nur SDK-XML-Warnung), Tests 0/0.
2. Karte:
| Bereich | Dateien/Module | Ebene | Risiko | Zweck |
|---|---|---|---|---|
| B1 Zustand/ViewModel | ui/AppViewModel.kt | Zustandsverwaltung | hoch | führt Navigation, Aufnahme, Auswertung, Vorlesen zusammen |
| B2 Daten/Persistenz | data/local/*, data/repo/Ablage.kt, data/settings/Einstellungen.kt, data/backup/* | Daten/Persistenz | hoch | Room-Entitäten, Abläufe F-01–F-21, Einstellungen, Backup |
| B3 Audio/Transkription | audio/* | Netzwerk/Nebenläufigkeit | mittel | Aufnahme, Groq-Transkription, Halluzinationsfilter |
| B4 TTS/Vorlesen | tts/* | Geschäftslogik/Plattform | mittel | Absatzteilung, Pipeline, vier Stimmen + Rückfall |
| B5 KI/Auth | ai/*, auth/* | Netzwerk/Geschäftslogik | mittel | Prompts, Schemata, Codex-Zugang |
| B6 Benachrichtigung/Lebenszyklus | notify/*, ExperimenteApp.kt, MainActivity.kt, network/* | Plattform/Lebenszyklus | mittel-hoch | Wecker, Boot, Lifecycle, OkHttp |
| B7 Oberfläche | ui/Navigation.kt, ui/screens/*, ui/components/*, ui/theme/* | Oberfläche | mittel | sechs Hauptbildschirme, Bausteine, Themen |
| nicht prüfrelevant | build/*, .gradle/, res/font, generierter Code, Fremdbibliotheken | — | — | — |
3. Loop-Zustand: aktuelle Runde 3, nächste Stufe 3 (Grenzen, Zustand, Zeit), Konvergenzzähler 0, nächster Blickwinkel Stufe 4 Invarianten/Gegenbeweis, offene Fixe 0, ausstehend nichts.
4. Fundtabelle:
| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurzbeschreibung |
|---|---|---|---|---|---|---|---|
| L-01 | 1 | B2 | Ablage.kt:695 | Daten/Persistenz | hoch | verifiziert | nichtUmgesetzt übernimmt nur Tag-1-Aufgaben auf Merkliste |
| L-02 | 1 | B1 | AppViewModel.kt:201-205 | Zustand | mittel | verifiziert | Rückweg räumt Kreise nicht ab (Kommentar vs. Code) |
| L-03 | 1 | B1 | AppViewModel.kt:1187-1189 | Zustand | mittel | verifiziert | letzterTagErreicht reagiert nicht auf Tageswechsel |
| L-04 | 1 | B1 | AppViewModel.kt:1142-1145 | Zustand/Oberfläche | mittel | verifiziert | waehleAuswertung lässt alte Einschätzung stehen |
| L-05 | 1 | B1 | AppViewModel.kt:1256-1274 | Fehlerbehandlung | hoch | verifiziert | schliesseAb meldet Erfolg auch bei Fehler/null |
| L-06 | 1 | B1 | AppViewModel.kt:1292-1303 | Fehlerbehandlung | mittel | verifiziert | nichtUmgesetzt ohne try/catch, stiller Abbruch |
| L-07 | 1 | B1 | AppViewModel.kt:1791-1799 | Lebenszyklus | mittel | verifiziert | beimVerlassen lässt Uhr/Zustand weiterlaufen |
| L-08 | 1 | B1 | AppViewModel.kt:1365-1371 | Oberfläche | niedrig | verifiziert | lies() startet Einschätzung nie bei Fremdwiedergabe |

Beweise/Fixe/Verifikation Runde 1:
- L-01: mehrtägiges laufendes Experiment (Tage 1–3 mit Aufgaben) → „Nicht umgesetzt“ → Ist: Merkliste enthält nur Tag 1 (`tagesaufgaben(id,1)`), Tage 2–3 verloren; Soll: alle Tage wie bei `nimmAusMonitor`. Fix: `alleZu` gruppiert nach `dayIndex` (Commit Runde 1). Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Beweis hinfällig, volle Liste drin; (2) einziger Aufrufer VM-`nichtUmgesetzt` zeigt jetzt Vollständiges; (3) leere Aufgaben → `"[]"` wie zuvor.
- L-02: A→B→A → Ist: `(_rueckweg+jetzt).takeLast(8)` behält Ziel doppelt, Kreis möglich; Soll laut Kommentar: enthaltenes Ziel bis Stelle abräumen. Fix: `take(indexOf(ziel))` vor dem Drauflegen. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Kreis A→B→A ergibt `[B]`; (2) `zurueck()`/`wische()` unverändert nutzbar; (3) Tiefe 8 und Leeren bei Hauptbildschirm bleiben.
- L-03: B-03 offen über Mitternacht → Ist: `.map{..._heute.value}` feuert nur bei Experimentwechsel; Soll: Tageswechsel dreht Abschlussfrage. Fix: `combine(ausgewertetes,_heute)`. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) reagiert auf beide Quellen; (2) einziger Leser Auswertung.kt:85 zeigt Korrektes; (3) null → false.
- L-04: Antwort liegt vor, dann Auswahlwechsel → Ist: alte `_einschaetzung`/ANTWORT bleibt; Soll wie `oeffneAuswertung` räumen. Fix: Liste leeren, Zustand AUFNAHME. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) alte Antwort weg; (2) Aufrufer Monitor-Auswahl zeigt Neues; (3) leere Liste/zulässiger Zustand.
- L-05: `_wertetAus==null` oder `schliesseAb` wirft → Ist: trotzdem Blüte + MONITOR + „Abgeschlossen“; Soll: nur bei Erfolg. Fix: null→Störung+Return, Fehler→Störung+Return, Blüte/Navigation nur bei Erfolg. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Erfolgsmeldung nur bei Erfolg; (2) B-03-Knopf bleibt bei Fehler stehen; (3) null und Exception abgedeckt.
- L-06: `ablage.nichtUmgesetzt` wirft → Ist: unbehandelter Abbruch ohne Störung; Soll: catch+Störung wie `werteAus`. Fix: try/catch mit `freundlich()`. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Fehler sichtbar; (2) kein neues Fehlverhalten, Feld bleibt; (3) Erfolgs-/Fehlerpfad getrennt.
- L-07: Aufnahme läuft + `onStop` → Ist: `uhr`-Job zählt weiter, Zustand AUFNAHME; Soll wie `beendeAufnahme` stoppen+neu bestimmen. Fix: `uhr?.cancel()`, `stop()`, `bestimmeZustand()` bei AUFNAHME. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) kein Timer-Leak; (2) Aufrufer MainActivity.onStop korrekt; (3) Nicht-Aufnahme-Fall unverändert.
- L-08: Fremdtext läuft + `lies(einschaetzung)` → Ist: `umschalten()` stoppt Fremdes, Return, Einschätzung startet nie; Soll: wechseln via `liesVor`. Fix: gleiche Kennung→toggeln, fremde→`halteAn()`+durchstarten. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Einschätzung startet; (2) B-03-Lautsprecher konsistent mit `liesVor`; (3) blanker Text weiter via `liesVor`-Meldung.
5. Rundenübersicht:
| Runde | Stufe | alle Bereiche geprüft | gemeldet/bestätigt/abgelehnt/behoben/verifiziert | Build/Tests | Zähler danach |
| 1 | 1 Überblick | ja (B1 per Helfer+Selbst-Triage, B2–B7 selbst gelesen/gesucht) | 8/8/0/8/8 | Build grün, keine Tests | 0 |
| 2 | 2 Funktion-für-Funktion | ja (alle Bereiche: Verträge gegen Code gehalten, Randfälle leer/null/0/negativ/Max/Duplikat) | 3/3/0/3/3 | Build grün, keine Tests | 0 |

Runde-2-Funde (alle bestätigt, behoben, verifiziert):
| L-09 | 2 | B1 | AppViewModel.kt:606 | Schnittstellenvertrag | mittel | verifiziert | starteSofort meldet „Drei laufen“ auch bei „schon übernommen“ |
| L-10 | 2 | B2 | Ablage.kt:348 | Kontrollfluss | niedrig | verifiziert | Doppelklick auf Starten → falsche Drei-Meldung statt No-op |
| L-11 | 2 | B1 | AppViewModel.kt:554 | Fehlerbehandlung | niedrig | verifiziert | legeEigenesImMonitorAn: Fehler schließt Fläche still, ohne Meldung |

Beweise/Fixe/Verifikation Runde 2:
- L-09: Vorschlag per Wisch übernommen, dann „Jetzt starten“ auf derselben Karte → Ist: `starteSofort`→`uebernimm`→null→VM meldet DREI_LAUFEN (Heute.kt:412), obwohl z. B. erst eines läuft; Soll: „steht schon im Monitor“. Fix: null-Fall unterscheidet per `stehtImMonitor` zwischen „schon übernommen“ und „voll“. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Fallunterscheidung deckt beide null-Ursachen ab; (2) einziger Aufrufer Heute-Karte zeigt Korrektes; (3) Titel stets gesetzt (KI filtert blanks, Spalte NOT NULL).
- L-10: Doppelklick auf „Starten“ vor UI-Aktualisierung → Ist: zweiter `starte`-Aufruf sieht LAEUFT→false→DREI_LAUFEN; Soll: No-op-Erfolg. Fix: LAEUFT→true ohne Änderung, MAX-Prüfung bleibt davor. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Zweitdruck kein Fehlschlag mehr; (2) Aufrufer `starteAnstehendes` funkelt statt Falschmeldung; (3) ANSTEHEND/ABGESCHLOSSEN-Pfade unverändert.
- L-11: DB-Fehler bei F-35-Speichern → Ist: `try/finally` ohne `catch`, Fläche bleibt zu, kein Wort; Soll: Meldung + Text erhalten. Fix: `catch` zeigt Störung und öffnet Fläche mit erhaltenem Text wieder. Test: kein Test, weil keine Testinfrastruktur. Verifikation: (1) Fehler sichtbar, Text nicht verloren; (2) Aufrufer Anlegeflaeche zeigt Feld wieder; (3) `_wartet` via `finally` weiter garantiert null.
6. Klärungsbedarf: (leer)
