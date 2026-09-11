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
3. Loop-Zustand: aktuelle Runde 1, nächste Stufe 1 (Überblick), Konvergenzzähler 0, nächster Blickwinkel Stufe 2 Funktion-für-Funktion, offene Fixe 8 (L-01–L-08), ausstehend Phase-0-Commit.
4. Fundtabelle:
| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurzbeschreibung |
|---|---|---|---|---|---|---|---|
| L-01 | 1 | B2 | Ablage.kt:695 | Daten/Persistenz | hoch | gemeldet | nichtUmgesetzt übernimmt nur Tag-1-Aufgaben auf Merkliste |
| L-02 | 1 | B1 | AppViewModel.kt:201-205 | Zustand | mittel | gemeldet | Rückweg räumt Kreise nicht ab (Kommentar vs. Code) |
| L-03 | 1 | B1 | AppViewModel.kt:1187-1189 | Zustand | mittel | gemeldet | letzterTagErreicht reagiert nicht auf Tageswechsel |
| L-04 | 1 | B1 | AppViewModel.kt:1142-1145 | Zustand/Oberfläche | mittel | gemeldet | waehleAuswertung lässt alte Einschätzung stehen |
| L-05 | 1 | B1 | AppViewModel.kt:1256-1274 | Fehlerbehandlung | hoch | gemeldet | schliesseAb meldet Erfolg auch bei Fehler/null |
| L-06 | 1 | B1 | AppViewModel.kt:1292-1303 | Fehlerbehandlung | mittel | gemeldet | nichtUmgesetzt ohne try/catch, stiller Abbruch |
| L-07 | 1 | B1 | AppViewModel.kt:1791-1799 | Lebenszyklus | mittel | gemeldet | beimVerlassen lässt Uhr/Zustand weiterlaufen |
| L-08 | 1 | B1 | AppViewModel.kt:1365-1371 | Oberfläche | niedrig | gemeldet | lies() startet Einschätzung nie bei Fremdwiedergabe |
5. Rundenübersicht:
| Runde | Stufe | alle Bereiche geprüft | gemeldet/bestätigt/abgelehnt/behoben/verifiziert | Build/Tests | Zähler danach |
6. Klärungsbedarf: (leer)
