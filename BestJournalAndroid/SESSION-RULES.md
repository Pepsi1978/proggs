# Best Journal — Session-Regeln

Diese Regeln gelten fuer JEDE Arbeitssession an der Best Journal App.

## Entwicklungsphase (IMMER AKTUELL HALTEN)
- **Aktuelle Phase**: 2 (Debug & Entwicklung)
- **Phase gestartet am**: 2026-03-31
- **Naechster Meilenstein**: Alle Features implementiert und stabil
- **Version**: v1.0.2 (Build 3)
- **Offene Features**: [vom Benutzer zu definieren]
- **Bekannte Bugs**: [vom Benutzer zu definieren]
- **Phasen-Referenz**: `~/.claude/rules/android-development-phases.md`

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
- App stoppen und neu starten: `adb -s [DEVICE] shell am force-stop com.bestjournal.app.debug`
- Bei Hilt-Aenderungen: `./gradlew clean assembleDebug`

## Geraete
- Erstes Handy (Samsung, Windows): `RFCX70KTDFX`
- Zweites Handy (Samsung, Windows): `R5CW206F0ZM`
- Tablet (Windows): `3406105H803E8G`
- Emulator: `emulator-5554`
- Samsung (macOS): `R52W800205N`

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

## Firebase / Google Cloud Projekt
- Firebase-Projektname: "bestjurnal"
- Firebase-Projekt-ID: `bestjurnal-a15b9`
- Projektnummer: `314424175660`
- Web-Client-ID: `314424175660-12m82j2jgvmc0tcgl61igghp67nnpmn1.apps.googleusercontent.com`
- Android-Client (Release): Paketname `com.bestjournal.app`
- Android-Client (Debug): Paketname `com.bestjournal.app.debug`
- Google Drive API: aktiviert
- Gmail API: aktiviert (fuer Feedback-Funktion)
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

## Tagebuch-Timeline (festgelegt)
- Eintraege gruppiert nach: Heute → Gestern → Letzte Woche → Monate → Jahre
- Vertikale Timeline-Linie links mit farbigen Punkten (Cyan)
- KI-generierte Ueberschrift (3-4 Woerter, unterstrichen, Primary-Farbe) ueber jedem Eintrag
- KI-generierte Stichpunkt-Zusammenfassung in der Detailansicht
- Stift-Button (links) + Mikrofon-Button (rechts) unten zentriert

## Offline-Transkription (sherpa-onnx)
- Bibliothek: sherpa-onnx v1.12.34 (Apache 2.0, kostenlos)
- Modell: Whisper Base int8 (153 MB, in APK gebundelt)
- Sprache: Deutsch (multilingual)
- Geschwindigkeit: ~4 Sekunden fuer 30s Audio
- Fallback-Logik: Groq API (wenn Key vorhanden) → Lokales Whisper (immer verfuegbar)
- AAR: `app/libs/sherpa-onnx-1.12.34.aar`
- Modell-Dateien: `app/src/main/assets/whisper/` (NICHT in Git — zu gross)

## Sicherheit (festgelegt)
- Biometric Lock: Fingerabdruck / PIN / Muster
- 60 Sekunden Toleranz bei Hintergrund-Wechsel
- API-Keys nur sichtbar nach Biometric-Auth (wenn Lock aktiviert)
- Feedback-Senden: Via Gmail API aus dem Google-Konto des Users
- Sign-Out: Atomares `clear()` + `commit()` auf SharedPreferences

## Konto-Logik (festgelegt)
- Anmelden: Direkt aus Einstellungen, kein Zwischenschritt
- Auto-Restore: Nach Anmeldung automatisch Backup von Drive laden + App-Restart
- Abmelden: DB loeschen, Prefs atomar clearen, API-Keys/Theme/Biometric behalten
- Sichern: Manueller Button "Tagebucheintraege sichern"
- Feedback: dev.app.support@gmail.com + Bestaetigung an User

## Kostenkontrolle (implementiert 2026-03-31)
- Abo-Preise: €4,99/Monat, €39,99/Jahr (33% Ersparnis)
- Dashboard-Limit: 16/Tag (still) → Cooldown 30 Min. → ab 20: Tageslimit
- Entry-Limit fuer Analyse: 40 (Trial + Abo), 10 (Free)
- Spam-Schutz: 25 KI-Aufrufe/Stunde
- Eintraege: Unbegrenzt
- Modell: gemini-2.5-flash ueber Firebase AI Logic
- Token-Kosten: Input $0.30/1M, Output $2.50/1M
- Geschaetzte Kosten pro Abo-Nutzer: ~€1.50-5.00/Monat

## Google Play Developer Account
- Entwicklername: Barwandt Digital Labs
- Konto-ID: 6096203479331369923
- Status: Identitaetspruefung ausstehend
- Google Play Billing Produkt-IDs: `bestjournal_ai_monthly`, `bestjournal_ai_yearly`
