# Entropy Journal — Session-Regeln

Diese Regeln gelten fuer JEDE Arbeitssession an der Entropy Journal App.

## Versionierung (PFLICHT)
- Bei JEDEM Commit die Version erhoehen in BEIDEN Stellen:
  1. `app/build.gradle.kts` → `versionCode` + `versionName`
  2. `SettingsScreen.kt` → Text "Entropy Journal vX.X.X"
- Wir sind in der Alpha-Phase: Versionen starten bei 0.x.x
- Patch-Version erhoehen bei kleinen Fixes (0.4.1 → 0.4.2)
- Minor-Version erhoehen bei neuen Features (0.4.x → 0.5.0)

## Git-Workflow (PFLICHT)
- Nach jedem Zwischenschritt committen und pushen
- Vor jedem Push: `git stash && git fetch origin && git rebase origin/main && git stash pop && git push`
- Commit-Format: `#NNN - Beschreibung` (fortlaufende Nummer)
- Co-Authored-By: Claude Opus 4.6

## Build & Install (PFLICHT)
- Nach jeder Aenderung: `./gradlew assembleDebug`
- Auf dem Handy installieren: `adb -s [DEVICE] install -r app/build/outputs/apk/debug/app-debug.apk`
- App stoppen und neu starten: `adb -s [DEVICE] shell am force-stop com.entropyjournal.debug`
- Bei Hilt-Aenderungen: `./gradlew clean assembleDebug`

## Geraete
- Erstes Handy (Samsung): `RFCX70KTDFX`
- Zweites Handy (Samsung): `R5CW206F0ZM`
- Emulator: `emulator-5554`

## Dark Mode Farben (festgelegt)
- Hintergrund: `#2C3930` (dunkles Waldgruen)
- Oberflaeche: `#3C3D37` (warmes Dunkelgrau)
- Erhoeht: `#3F4F44` (gedaempftes Gruen-Grau)
- Container: `#A27B5C` (warmes Erdbraun)
- Primaer-Akzent: `#D36B00` (leuchtendes Orange)
- Text: `#DCD7C9` (sanftes Creme)

## Light Mode Farben (festgelegt — Original)
- Hintergrund: `#F8F8FC`, Oberflaeche: `#FFFFFF`
- Primaer-Akzent: Teal `#0097A7`

## Einstellungen-Reihenfolge (festgelegt)
1. Konto
2. Erscheinungsbild (Dunkelmodus / System folgen / Sonnenauf-untergang)
3. Sicherheit (Fingerabdruck / PIN — sperrt nach 60s im Hintergrund)
4. API-Schluessel
5. Gemini-Modell
6. Aufnahme (Dauer + Textverbesserung + Dashboard-Auto-Update)
7. Feedback (E-Mail an dev.app.support@gmail.com, CC an angemeldeten User)
8. Ueber die App (Version + Copyright Frank Barwandt)

## Navigation-Reihenfolge (festgelegt)
- Unten: Dashboard | Tagebuch | Einstellungen
- Tag/Nacht-Toggle: Gelbes Symbol (#FFD54F) rechts neben "Tagebuch" und "Dashboard"

## Google Cloud Projekt
- Projektname: "Tagebuch"
- Web-Client-ID: `674560807048-l6ktqsucjr4ld91srdc6assgfiks19mh.apps.googleusercontent.com`
- Android-Client: Paketname `com.entropyjournal.debug`, SHA-1 vom Debug-Keystore
- Google Drive API: aktiviert
- Testnutzer: barwandt@gmail.com

## Gemini Modelle (aktuelle Liste)
1. gemini-3-flash-preview ($0.50/$3.00)
2. gemini-3.1-flash-lite-preview ($0.25/$1.50)
3. gemini-2.5-flash ($0.30/$2.50)
4. gemini-2.5-flash-lite ($0.10/$0.40)
5. gemini-2.0-flash ($0.10/$0.40)

## API-Keys
- Gehoeren zum Geraet, NICHT zum Konto
- Werden bei Sign-Out gesichert und danach wiederhergestellt
- NICHT in der Cloud speichern (Sicherheitsrisiko, weckt Misstrauen)

## Dashboard-Regeln
- Gesamtanalyse: ALLE Tagebucheintraege gleichwertig beruecksichtigen
- Kategorien: Nur relevante, dynamisch erstellt, nach Entropie sortiert (hoechste links)
- Empfehlungen: Aus ALLEN Kategorien, sortiert nach Prioritaet (rot → orange → blau)
- Ratschlaege pro Kategorie: 3-8 Stueck
- Kategorie-Namen: Max 15 Zeichen, kurz und knapp
- Manuelle Aktualisierung: Frische Analyse OHNE alten Kontext
- Undo-Button: Vorherigen Zustand wiederherstellen
- Sprache: Einfach, klar, fuer Zehntklaessler verstaendlich
