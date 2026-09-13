# Genialer Wecker

Android-Wecker für das Fold, direkt aus der gewünschten Gestaltung von **Geniale Ideen**
entwickelt. Kotlin / Jetpack Compose, Android 8 oder neuer, Ziel-SDK 36.

## Bedienung

1. **＋ Wecker** öffnen, Uhrzeit und Namen wählen.
2. Unter **Wann soll er wecken?** einmalig, Wochentage, einen Kalendertag oder
   **Schichtwecker · alle X Tage** auswählen.
3. Die optionalen Karten aufklappen: Weckablauf, Musik, Lautstärke/Schlummern, Foto-Aufgabe.
4. Über den feststehenden Knopf **Wecker speichern** sichern.

Für einen 35-Tage-Schichtplan: **alle X Tage → 35 Tage**, den ersten tatsächlichen
Nachtschichttag und die gewünschte Uhrzeit einstellen. Die Folgetermine bleiben an diesem
Datum verankert. Schlummern und das Auslassen eines Termins verschieben den Rhythmus nicht.
Die Schnellwahl bietet bis zu 60 Tage; im Kalender lassen sich auch weiter entfernte
Starttermine wählen. Datums- und Intervallpläne werden in lokaler Kalenderzeit berechnet.

## Funktionen

- Beliebig viele eigenständige Wecker, Wochentage, einmalige Termine, Tagesintervalle,
  Duplizieren, nächstes Vorkommen auslassen, Schlummerdauer und Schlummerlimit.
- Frei anordenbare Folge aus Klingelzeichen, offenen Ideen, eigenem Text und Musik.
  Der Ablauf wiederholt sich bis zum Stoppen. Musikdateien werden vollständig abgespielt.
- MP3 und andere Android-Audioformate werden aus dem Dateiwähler in den privaten
  Gerätespeicher kopiert. Geräte-Wecktöne und vier eigens erzeugte Signale sind auswählbar.
- Pro Wecker eigene Lautstärke, optionales Anschwellen und Vibration. Wiedergabe über
  `USAGE_ALARM` / `STREAM_ALARM`; die frühere Lautstärke wird anschließend wiederhergestellt.
- Schlüssellose Edge-Stimmen, Google Chirp 3 HD, Alibaba-Standardstimmen und eigene
  Alibaba-Stimmen samt Aufnahme, Erstellung, Auswahl, Favoriten und Löschung.
- Groq-Diktat mit Whisper Large V3 Turbo und den vier Filterschichten aus Geniale Ideen.
  Textverbesserung über dessen ChatGPT-Anmeldung und Modellauswahl; Originaltext zurückholbar.
- Foto zum Stoppen: Referenzmotiv, Mindesthelligkeit und/oder vorherrschende Farbe mit
  Mindestanteil. Prüfung ausschließlich lokal. Alle ausgewählten Bedingungen müssen stimmen.
- Gold-/Glasgestaltung, echte plastische Knöpfe, animierte Lichtreflexe, schwebender Hintergrund,
  Hell-/Dunkelmodus. Breite Displays zeigen die Weckkarten zweispaltig.
- Persistenter Bearbeitungsentwurf. Ein App-Neustart verliert keinen begonnenen Wecker.

## Ideenbrücke

Geniale Ideen benötigt das mitgelieferte Update mit `bridge/WeckerProvider.kt`.
`content://de.frank.genialeideen.wecker/offen` liefert ausschließlich offene Ideen, nach
`reihenfolge`, `angelegtAm DESC`, `id` sortiert. Eine Signaturberechtigung plus konkrete
Prüfung des aufrufenden Pakets schützen die Schnittstelle. Schreibzugriff ist ausgeschlossen.

In den Einstellungen kann der Benutzer ausdrücklich **Spracheinstellungen übernehmen**
wählen. Dies kopiert Stimme, Tempo und die Google-/Alibaba-/Groq-Schlüssel über Binder in
den verschlüsselten Speicher des Weckers. Schlüssel werden nicht in die Ideenkopie geschrieben.
Anschließend ist die Stimmenauswahl im Wecker unabhängig von Geniale Ideen.

## Offline und Android-Freigaben

- Beim Speichern werden Cloud-Stimmen in kleine, Unicode-sichere Abschnitte zerlegt,
  vorab erzeugt und dauerhaft gespeichert. Erst ein vollständig erzeugter Satz von
  Dateien ersetzt die vorherige Fassung. Die Oberfläche zeigt den Vorbereitungsstand.
- Änderungen an offenen Ideen stoßen eine Hintergrundvorbereitung an; zusätzlich läuft
  ein netzgebundener WorkManager-Abgleich. Beim Auslösen selbst gibt es keinen Netzaufruf.
- Ohne fertiges Sprach-Audio erklingt ein lokaler Ersatzweckton. Eine fehlgeschlagene
  Vorbereitung darf niemals einen stummen Wecker ergeben.
- Weckdaten und Audiodateien liegen im Device-Protected Storage. Die Receiver planen
  nach Neustart, App-Update und Uhrzeitänderung erneut. Vor der ersten Entsperrung werden
  weder verschlüsselte API-Schlüssel noch WorkManager geöffnet.
- **Nicht stören:** Android muss Alarme im verwendeten Modus zulassen. Die App erklärt die
  Freigaben und verlinkt zu den passenden Systemseiten. Vollständige Systemsperren,
  erzwungenes Beenden und ein ausgeschaltetes Gerät können nicht umgangen werden.
- Google Chirp 3 HD benötigt einen für **Cloud Text-to-Speech** freigeschalteten Schlüssel.
  Ein ausschließlich für Gemini freigeschalteter Schlüssel ist nicht automatisch ausreichend.
- Ein Fotoabgleich ist ein lokaler Struktur-/Farbvergleich, keine semantische KI-Erkennung.
  Das Referenzmotiv sollte aus ähnlicher Perspektive aufgenommen werden.

## Übernommene Bausteine

Die Dateien unter `de/frank/genialeideen/` und die drei Schriftdateien stammen aus
`GenialeIdeen/app/src/main/`. Ihre Namensräume bleiben erhalten; die neue App hat die eigene
Application-ID `de.frank.genialerwecker`. Es besteht keine Quellpfad-Verknüpfung zur anderen App.

Weckerspezifische Anpassungen der Kopien:

- `EdgeTtsPlayer`: Ausgabe vollständiger Audiodateien ohne sofortige Wiedergabe, kompletter
  Tempobereich, zulässiges Edge-SSML ohne `<lang>` und Pitch in Hz.
- `Theme`: Standard-Textfarbe auch für die neuen Weckerbildschirme im Dunkelmodus.
- `Meldung`: kleiner UI-Vertrag für die wiederverwendeten Gestaltungselemente.

## Bauen und prüfen

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class de.frank.wecker.AlarmIntegrationTest de.frank.genialerwecker.test/androidx.test.runner.AndroidJUnitRunner
```

Die Signierung nutzt den gemeinsamen Android-Debug-Key. Geniale Ideen zuerst aktualisieren,
dann den Wecker installieren. Bestehende Apps niemals für ein Signaturproblem deinstallieren.

### Verifikation auf dem angeschlossenen Fold (SM-F971B)

- **12 JVM-Tests bestanden:** Wochentage, einmalige Termine, 35-Tage-Rhythmus über die
  Zeitumstellung, übersprungene Zyklen, Startdatum, 60 Tage Vorlauf, Sommerzeitlücke,
  kein doppelter Alarm in der zurückgestellten Stunde, vollständige Speicherung,
  Foto-Pflichtbedingungen und Unicode-Textteilung.
- **5 Geräte-Integrationstests bestanden:** echter AlarmManager bei ausgeschaltetem WLAN
  und Mobilfunkdaten sowie Nicht-stören-Modus „nur Alarme“, hörbarer Alarmkanal und
  Lautstärke-Wiederherstellung; komplette 30-Sekunden-MP3 und anschließender Schrittwechsel;
  Foto-Stopp-Sperre und Schlummern ohne Schichtverschiebung; reale Ideenbrücke und Sortierung;
  lokale Farb-/Helligkeitsprüfung.
- **Zusätzlicher Live-Edge-Test bestanden:** Der Dienst liefert eine vollständige,
  lokal abspielbare Audiodatei. Der zunächst gefundene SSML-Fehler wurde korrigiert.
- **Fold-Oberfläche geprüft:** Außendisplay 1248 × 1972 und Innendisplay 2448 × 1848
  über den Geräte-Zustandswechsel; ein- und zweispaltige Darstellung, Hell-/Dunkelansicht.
  Anschließend wurde der ursprüngliche Gerätezustand wiederhergestellt.
- Build und Android-Lint erfolgreich. Warnungen stammen überwiegend aus den übernommenen
  Versionspins und älteren, weiterhin verwendeten Android-APIs.

Nicht als live verifiziert ausgegeben: kostenpflichtige Google-/Alibaba-Aufrufe,
Groq-Diktat und ChatGPT-Anmeldung ohne eingerichteten Nutzerzugang sowie ein tatsächlicher
Geräteneustart mit anschließendem Alarm vor der ersten Entsperrung.
