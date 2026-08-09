# Session Handoff — 09.08.2026, ca. 23:30 Uhr

## Ziel
Die Pipeline `neue-applikation` komplett durchfahren fuer die neue Android-App
**"Experimente"** (taegliche persoenliche Experiment-Vorschlaege, Sprachbedienung,
KI-gefuehrtes Logbuch). Stufe 1 (Grilling) und Stufe 2 (Rueckimport) sind FERTIG und
gepusht. Aktuell laeuft **Stufe 3: design-umsetzer** (Betriebsart P, Bau aus Spec-Paket).
Danach fehlt nur noch Build + Version-Bump + Installation aufs Geraet.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt

- **Welche Aufgabe lief gerade:** Stufe 3, **Block 5 von 6: die neun Bildschirme bauen**.
  Bloecke 1-4 (Fundament/Theme, Datenschicht, Dienste, KI-Aufgaben+Ablage) sind fertig,
  gruen gebaut, committet und gepusht.

- **Wo genau unterbrochen — der allerletzte Schritt:** Ich hatte gerade
  `ui/screens/Heute.kt` (Bildschirm B-01) fertig geschrieben (438 Zeilen) und wollte in
  EINEM Bash-Aufruf (a) drei versehentlich eingebaute Platzhalter am Dateiende entfernen
  (`platzhalterAufgabe`, `platzhalterRefresh`, `fun abstand()`) samt der drei zugehoerigen
  ungenutzten Imports (Spacer, Refresh, Aufgabe) und danach (b) `gradlew compileDebugKotlin`
  laufen lassen. Der Benutzer hat den Bash-Aufruf per ESC abgelehnt.

- **Schon erledigter Teil DIESES Schritts:** Die **Bereinigung ist bereits passiert** —
  grep auf `platzhalterAufgabe|platzhalterRefresh|fun abstand` in Heute.kt liefert 0
  Treffer, die drei Imports sind ebenfalls weg. Heute.kt ist also sauber (438 Zeilen).

- **Noch offener Teil DIESES Schritts:** Heute.kt wurde seit der Bereinigung **NICHT mehr
  uebersetzt**. Der Compile-Lauf steht noch aus.

- **So geht es EXAKT weiter (allererste Aktion der neuen Session):**
  `cd ~/proggs/Experimente && ./gradlew compileDebugKotlin --no-daemon -q 2>&1 | grep -E "^e: " | head -20`
  Fehler beheben, bis sauber. Danach die restlichen Bildschirme bauen (Reihenfolge unten
  unter "Naechste Schritte").

- **Was dafuer alles vorhanden sein muss:**
  - Projekt: `~/proggs/Experimente/` (Gradle-Aufbau von PerfectMoment uebernommen)
  - Build-Befehl: `cd ~/proggs/Experimente && ./gradlew assembleDebug --no-daemon -q`
    (dauert ca. 1-2 Min; `--no-daemon` ist wichtig)
  - Bisher IMMER gruen. 40 Kotlin-Dateien.
  - **KEIN Android-Geraet angeschlossen** (`adb devices` leer) — betrifft Block 6.

- **Uncommitteter Arbeitsstand:** 5 NEUE (untracked) Dateien, ~1584 Zeilen, ausgelagert
  nach `.claude/session-backup.diff` (lokal + Repo). Es sind:
  - `Experimente/.../tts/Vorleser.kt` — schlanker Vorleser ueber die 3 TTS-Abspieler
  - `Experimente/.../ui/AppViewModel.kt` — Zustandshalter (kompiliert bereits sauber)
  - `Experimente/.../ui/components/Bausteine.kt` — Karte(M-01), AufgabenZeile(M-06),
    MerkenSymbol(M-07), Wartezustand(M-09), Leisten, Knoepfe (kompiliert sauber)
  - `Experimente/.../ui/components/Sprechknopf.kt` — M-02 Atmen + M-03 Vibration
    (kompiliert sauber)
  - `Experimente/.../ui/screens/Heute.kt` — B-01, NOCH NICHT uebersetzt seit Bereinigung
  Die Dateien liegen real im Working Tree (gleicher PC) — der Diff ist nur Absicherung.

- **Danach:** weiter mit "Naechste Schritte".

## Aktueller Status
- **Erledigt und gepusht:**
  - `f80e53a70` Experimente: Spec v1 aus dem Grilling
  - `7938b7a8d` Experimente: Spec v2 aus dem Design
  - `2436508bc` Experimente: Fundament, Datenschicht und Dienste (Bloecke 1-3)
  - `1f4746b7a` Experimente: KI-Aufgaben und Ablage-Schicht (Block 4)
- **In Arbeit:** Block 5 (Oberflaeche). B-01 geschrieben, 8 Bildschirme fehlen noch.
- **Blockiert:** Block 6 (Installation) — kein Geraet an ADB.

## Relevante Dateien
- `~/proggs/Specs/Experimente/v2/BAU-AUFTRAG.md` — Bau-Auftrag mit der Abhakliste
  (9 Bildschirme B-01..B-09, 27 Funktionen F-01..F-27, 75 Bewegungen M-01..M-75,
  20 Abnahmekriterien A-01..A-20)
- `~/proggs/Specs/Experimente/v2/02-UI-SPEC.md` — Aufbau JEDES Bildschirms, alle Texte
  woertlich (Abschnitt 8), beide Erscheinungen
- `~/proggs/Specs/Experimente/v2/03-MOTION-SPEC.md` — 75 Bewegungen, je mit fertigem
  Jetpack-Compose-Ausdruck
- `~/proggs/Specs/Experimente/v2/01-FUNKTIONS-SPEC.md` — Verhalten je Funktion
- `~/proggs/Designs/Outbox/Experimente/WERFT-DESIGN/bildschirme/21dunkelstandard/*.html`
  und `.../22hell/*.html` — je 9 Bildschirme zum optischen Abgleich
- `~/proggs/Specs/_Pipeline-Logbuch/2026-08-09-lauf-01.md` — **Lauf-Logbuch**, muss nach
  jedem Schritt fortgeschrieben werden (ausdruecklicher Wunsch des Benutzers, sein
  allererster Auftrag dieser Session). Enthaelt bereits 6 Skill-Schwaechen B-01..B-06.

## Getroffene Entscheidungen
- KI-Weg: **Codex-OAuth wie PerfectMoment** (ChatGPT-Abo), NICHT Claude-API. Zwei
  getrennte Modellwahlen + Effort: eine fuer Experimente, eine fuers Logbuch.
- Drei Speicher: 15-Tage-Log (ausfuehrlich, 20-30 Zeilen/Tag) wandert ins Langzeit-Log
  (verdichtet, max 7 Zeilen/Tag, dauerhaft), dazu die Erkenntnisliste.
- Hoechstens **3** gleichzeitig offene Experimente; mehrtaegige moeglich.
- Fuenf Vorschlaege: 2 zur Lage passend + 2 voellig neu + 1 von der Merkliste.
- KEIN festes Raster von Lebensbereichen, KEINE Bereichsfarben.
- Namen: **Merkliste** = aufgehobene Experimente, **Wuensche & Ziele** = Lebensziele.
- EINE To-Do-Liste fuer den Tag, nach Experimenten gruppiert (nicht eine je Experiment).
- Nicht umgesetztes Experiment: wird TROTZDEM ausgewertet UND kommt zurueck auf die Merkliste.
- Design: warm im Grundton, sachlich im Aufbau. Dunkel ist Standard.
- Rangfolge bei Konflikten: 1. KI muss Frank kennen, 2. nichts geht verloren,
  3. Vorschlaege wirklich neu.

## Fehlgeschlagene Ansaetze — NICHT wiederholen
- **Kotlin-Bloecke per Klammerzaehlung ab der Signaturzeile entfernen:** scheitert an
  mehrzeiligen Parameterlisten mit Default `= {}` — der Zaehler haelt die geschweiften
  Klammern des Defaults fuer den Rumpf und loescht nur die Signatur, der Rumpf bleibt als
  Waise stehen. Korrekt: erst die Parameterliste ueber RUNDklammern zu Ende zaehlen, dann
  ab der ersten `{` die geschweiften. (Hat CodexAuthManager zweimal zerschossen, musste
  aus PerfectMoment neu kopiert werden.)
- **Deutsche Anfuehrungszeichen mit geradem Schlusszeichen** in Kotlin-Strings: beendet
  den String und erzeugt Syntaxfehler. Immer typografisch schliessen.
- **Heredoc mit einfachen Anfuehrungszeichen fuer Dateien mit doppeltem Backslash**
  (settings.gradle.kts): aus `com\\.android` wurde `com\.android` -> Kotlin-Fehler
  "Illegal escape". Solche Dateien mit dem Write-Tool schreiben, nicht per Heredoc.
- **Langen Markdown-Text per Heredoc schreiben** (dieses Backup): brach mit
  "unexpected EOF while looking for matching quote" ab. Fuer lange Notizen das
  Write-Tool nehmen.
- **PerfectMoments `TtsManager` uebernehmen:** liest aus PerfectMoments eigenem
  Preference-Store, passt nicht. Stattdessen eigener schlanker `tts/Vorleser.kt`.

## Wichtige Recherche-Ergebnisse
- `CodexModels.kt`: 3 Modelle (`gpt-5.6-sol`, `gpt-5.6-terra`, `gpt-5.6-luna`),
  5 Effort-Stufen (`low`, `medium`, `high`, `xhigh`, `max`).
- `GroqTranscriber`: `whisper-large-v3-turbo`, Sprache `de`, `temperature=0`, max 25 MB,
  plus SpeechAnalyzer-Vorfilter und WhisperHallucinationFilter.
- TTS: Google Chirp 3 HD (31 dt. Stimmen, Standard `de-DE-Chirp3-HD-Kore`),
  Qwen/DashScope fuer die eigene Stimme, Edge als dritter Weg.
- Design-Bezugsgroesse: **412 x 915 dp bei density 1** -> px aus dem Design sind 1:1 dp.
- Gemessene Farben sind EXAKT identisch mit der v1-Absicht (alle 26 Werte).

## Naechste Schritte (priorisiert)
1. **`compileDebugKotlin` fuer Heute.kt laufen lassen** und Fehler beheben (siehe oben).
2. **B-02 Gespraech** (`ui/screens/Gespraech.kt`): Faden mit Blasen (AppForm.blaseIch /
   blaseKi), Sprechknopf klein unten rechts, Antwort wird SOFORT vorgelesen.
3. **B-03 Auswertung** (`ui/screens/Auswertung.kt`): alle offenen Experimente der Reihe
   nach, je Sprechknopf + Textfeld + "Text mit KI verbessern" + KI-Antwort mit
   Lautsprecher, dazu "Ueberspringen" und "Fertig".
4. **B-04 Ziele / B-05 Merkliste / B-06 Erkenntnisse / B-07 Logbuch** (koennen zusammen
   in `ui/screens/Listen.kt`): B-07 mit zwei Reitern (Letzte 15 Tage / Langzeit),
   langer Druck = bearbeiten/loeschen.
5. **B-08 Einstellungen / B-09 Selbstbild** (`ui/screens/Einstellungen.kt`): zwei
   Modellwahlen + Effort, Stimme, Zugaenge (Codex-Anmeldung), Erinnerungen, Erscheinung.
6. **`ui/Navigation.kt`** + MainActivity: NavHost, untere Leiste
   (Heute / Ziele / Merkliste / Erkenntnisse / Logbuch), Bildschirmwechsel = reines
   Ueberblenden 200 ms, tiefer hinein = von rechts schieben 240 ms. Dazu F-27
   Wisch-Navigation zwischen den 5 Hauptbildschirmen.
7. Build gruen, dann **Version-Bump** in `app/build.gradle.kts`
   (`versionName` + `VERSION_BUMPED_AT`), committen und pushen.
8. **Screenshot-Abgleich je Bildschirm und je Erscheinung** gegen die WERFT-DESIGN-HTMLs
   (der Skill verlangt das ausdruecklich) — braucht ein Geraet.
9. `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
10. Lauf-Logbuch fortschreiben und Abschlussbericht ueber die ganze Kette geben.

## Offene Fragen
- **Kein Android-Geraet an ADB.** Ohne Geraet keine Installation und kein optischer
  Abgleich -> die Umsetzung gaelte als "optisch ungeprueft". Der Benutzer wurde bereits
  darauf hingewiesen, eine Antwort steht aus.
- Spec-Luecke, die ich selbst geschlossen habe: `01-FUNKTIONS-SPEC` Abschnitt 3 listet
  10 Einheiten, F-01 schreibt aber eine `SituationEntry`, die dort fehlt. Ich habe eine
  11. Einheit `Lage` angelegt. Muss noch ins Lauf-Logbuch.

## Anker
- Branch: main
- Letzte Commits:
1f4746b7a Experimente: KI-Aufgaben und Ablage-Schicht
2436508bc Experimente: Fundament, Datenschicht und Dienste
7938b7a8d Experimente: Spec v2 aus dem Design
f80e53a70 Experimente: Spec v1 aus dem Grilling
eb5c6fde8 Pipeline durchgepruefte: drei Luecken geschlossen
