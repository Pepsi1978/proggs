# Genialer Wecker

Android-Wecker für das Fold, direkt aus der gewünschten Gestaltung von **Geniale Ideen**
entwickelt. Kotlin / Jetpack Compose, Android 8 oder neuer, Ziel-SDK 36.

## Bedienung

1. **＋ Wecker** öffnen, Uhrzeit und Namen wählen.
2. Unter **Wann soll er wecken?** einmalig, Wochentage, einen Kalendertag oder
   **Schichtwecker · alle X Tage** auswählen.
3. Die optionalen Karten aufklappen: Weckablauf, Musik, Lautstärke/Schlummern, Foto-Aufgabe.
4. Über den feststehenden Knopf **Wecker speichern** sichern. Der Wecker wird dabei
   **immer aktiviert**. Die Audio-Vorbereitung läuft anschließend unabhängig weiter;
    währenddessen lassen sich weitere Wecker anlegen.
5. Gespeicherte Wecker erscheinen **standardmäßig zugeklappt**: Aktivierungsschalter links,
   Uhrzeit und Titel daneben, Aufklapppfeil rechts. Tippen auf die kompakte Karte oder die
   Uhrzeit/Titel-Zeile öffnet direkt den Editor, auch bei ausgeschalteten Weckern. Nur der Pfeil
   öffnet die Details und Aktionen. Der Schalter schaltet ausschließlich den jeweiligen Wecker.

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
- Bei einem Ablauf mit offenen Ideen oder eigenem Text zeigt der Editor die Stimmauswahl und
  direkt darunter den Sprechgeschwindigkeitsregler. Beide Werte können unabhängig pro Wecker
  festgelegt oder auf dem globalen Standard belassen werden. Die Auswahl bleibt im Entwurf und
  im gespeicherten Wecker erhalten und gilt auch für die sechs offline vorbereiteten Varianten.
- Schlüssellose Edge-Stimmen, Google Chirp 3 HD und **Meine Stimmen** aus Alibaba samt
  Aufnahme, Erstellung, Auswahl, Favoriten und Löschung. Die hochgeladenen Stimmen werden
  beim Start und beim Öffnen der Einstellungen automatisch geladen und kontogebunden
  zwischengespeichert. Alibaba-Standardstimmen sind nicht mehr in der Auswahl.
- Groq-Diktat mit Whisper Large V3 Turbo und den vier Filterschichten aus Geniale Ideen.
  Textverbesserung über dessen ChatGPT-Anmeldung und Modellauswahl; Originaltext zurückholbar.
- Foto zum Stoppen: Referenzmotiv, Mindesthelligkeit und/oder vorherrschende Farbe mit
  Mindestanteil. Prüfung ausschließlich lokal. Alle ausgewählten Bedingungen müssen stimmen.
- Gold-/Glasgestaltung, echte plastische Knöpfe, animierte Lichtreflexe, schwebender Hintergrund,
  Hell-/Dunkelmodus. Breite Displays zeigen die Weckkarten zweispaltig.
- Persistenter Bearbeitungsentwurf. Ein App-Neustart verliert keinen begonnenen Wecker.
- Die Berechtigungskarte erscheint auf der Startseite nur bei fehlenden Freigaben.
  Oben in den Einstellungen steht der vollständige Status mit grünem Schutzsymbol.

### Bedienkomfort (1.1.45)

- Die Kopfkarte nennt neben Termin und Restzeit auch den **Namen** des nächsten Weckers
  beziehungsweise des laufenden Schlummerns. Lange Namen brechen auf zwei Zeilen um.
- Aufgeklappte Weckerkarten bleiben beim Falten oder Drehen des Geräts aufgeklappt.
  Eine neu geöffnete Liste startet weiterhin zugeklappt.
- Die Stimmenauswahl im Wecker nennt wie die Einstellungen auch weiblich/männlich, öffnet
  bei der aktuellen Auswahl und bietet ab mehr als zwölf Einträgen ein Suchfeld mit
  verständlichem Hinweis, wenn nichts passt. Kurze Auswahllisten bleiben unverändert.
- In den Einstellungen sind Auswahlpunkt und Stimmenname eine gemeinsame, mindestens 48 dp
  hohe Fläche; der Favoritenstern liegt daneben, hat einen sprechenden Namen für TalkBack
  und wählt die Stimme nicht aus.

### Bedienkomfort (1.1.46)

- Im Wecker-Editor lässt sich die **weckereigene Stimme mit dem weckereigenen Tempo** anhören.
  Die Vorschau nimmt beim Start einen festen Stimm-Schnappschuss, damit Ton und Abspieltempo
  zusammenpassen, und verändert die globalen Einstellungen nicht. Während einer Aufnahme oder
  eines laufenden Vorgangs ist das Anhören gesperrt; Stoppen bleibt möglich.
- Die Vorschau im festen Speicherbereich kündigt keinen Termin mehr an, wenn der Wecker die
  Prüfung beim Speichern gar nicht bestehen würde. Sie zeigt stattdessen genau die Meldung, die
  sonst erst nach dem Tippen käme. Das Speichern selbst ist unverändert.
- Bei den Sprachschlüsseln ist „Speichern“ nur bei einer echten Änderung aktiv, daneben steht
  dann „Noch nicht gespeichert“; die Eingabetaste speichert ausdrücklich. Ein Schlüssel wird
  weiterhin nur auf ausdrückliches Speichern abgelegt. **Hinweis:** Wer die Karte zuklappt oder
  die Einstellungen verlässt, verliert eine ungespeicherte Eingabe weiterhin – der Zustand ist
  jetzt nur sichtbar, bevor es passiert.

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

- Beim Speichern werden **sechs Varianten derselben Stimme** erzeugt und dauerhaft gespeichert.
  **Jede Idee wird einzeln vollständig in sechs Varianten vorbereitet, erst dann die nächste.**
  Kein Request enthält die ganze Ideenliste. Sehr lange Ideen werden zusätzlich an Absatz-
  und bei Bedarf Satz-/Wortgrenzen geteilt (höchstens 900 Zeichen je Request).
  Vollständige Absätze sind die Einheiten nach dem Cortex-Prinzip.
- Die Varianten verwenden getrennte TTS-Requests und behutsam unterschiedliche Sprechtempi.
  Es wird keine abweichende Stimme gewählt. Die tatsächliche Betonung entscheidet der Anbieter.
  Eine neue Synthese garantiert nicht bei jedem Anbieter automatisch eine neue Betonung.
- Beim ersten Durchlauf werden alle Ideen/Texte in Variante 1 abgespielt, dann in Variante 2,
  bis Variante 6 und wieder 1. Der nächste lokale Absatz wird während des laufenden Absatzes
  vorbereitet und bei direkten Absatzübergängen über `MediaPlayer.setNextMediaPlayer` übergeben.
- Nach jedem vollständigen eigenen Text folgen **2 Sekunden Pause**, nach jeder vollständigen
  Idee **1,5 Sekunden** – auch beim Übergang in die nächste Variante und von Variante 6 zurück
  zu 1. Innerhalb einer langen Idee entstehen keine zusätzlichen Absatzpausen. Die Pausen
  behalten ihre Dauer unabhängig vom Sprechtempo; Stoppen/Schlummern bricht auch die Pause ab.
  Alte Ideen-Audios erhalten die neuen Ideengrenzen bei der nächsten Audio-Vorbereitung;
  vorhandene passende Sprachdateien werden dabei weiterverwendet.
- Erst ein vollständig erzeugter Satz von Dateien ersetzt die vorherige Fassung. Die Oberfläche
  zeigt Idee/Text, Variante und Absatz. Vorhandene Ein-Varianten-Wecker bleiben abspielbar.
- Änderungen an offenen Ideen stoßen eine Hintergrundvorbereitung an; zusätzlich läuft
  ein netzgebundener WorkManager-Abgleich. Beim Auslösen selbst gibt es keinen Netzaufruf.
- Ohne fertiges Sprach-Audio erklingt ein lokaler Ersatzweckton. Eine fehlgeschlagene
  Vorbereitung darf niemals einen stummen Wecker ergeben.
- Schlägt Google oder die eigene Alibaba-Stimme beim Vorbereiten fehl, wird nur für diesen
  Fehlerfall Edge verwendet und der abweichende Anbieter sichtbar markiert. Pro Vorbereitungs-
  lauf wird ein ausgefallener Anbieter nicht für jeden einzelnen Absatz erneut belastet.
- Beim Wecken wird **kein Internet eingeschaltet und keine neue Cloud-Synthese gestartet**.
  Die Sechs-Varianten-Vorbereitung ersetzt die zwischenzeitlich erwogene Zwei-Minuten-Vorbereitung.
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

### Nachbesserung: mehrere Wecker und sechs Varianten

- 21 JVM-Prüfungen bestanden (12 Termin-/Persistenzprüfungen plus 9 Varianten-/Absatzprüfungen).
- 13 Geräteprüfungen bestanden, einschließlich der zuvor fehlgeschlagenen Editor-Regressionen,
  automatischem Laden eigener Alibaba-Stimmen, zwei unabhängigen gleichzeitigen Alarmen,
  sechs real erzeugten Audiodateien und Wiedergabe 1 → 2 → 3 → 4 → 5 → 6 → 1.
- Die vollständige MP3-Wiedergabe wurde zusätzlich nach Wechsel zum Android-Startbildschirm
  geprüft. Zwei zunächst unterbrochene Bedienungstests wurden nach Abstimmung mit dem Nutzer
  gezielt wiederholt und bestanden.
- Auf dem Gerät geprüft: Bei vollständigen Freigaben keine Berechtigungskarte auf der
  Startseite; in den Einstellungen steht sie ganz oben mit dem Bereitschaftsstatus.
- Reproduzierbare Fehlerklasse: Ein verspätetes Ereignis eines alten Editors ersetzte den
  neuen Entwurf. Jetzt gelten getrennte Entwurfs-IDs, veraltete Ereignisse werden verworfen,
  und das Anlegen besitzt einen eigenen kollisionsgeschützten Schreibpfad.
- Auch Änderungen während eines laufenden Speichervorgangs bleiben als neuerer Entwurf
  erhalten. Wird ein Text während seiner Audio-Vorbereitung erneut gespeichert, wird die
  vorherige Vorbereitung sauber beendet und die aktuelle Fassung vorbereitet.

### Nachbesserung: Vorlesepausen und kompakte Weckerkarten

- 23 JVM-Prüfungen bestanden, einschließlich Textlauf-Pausen, Ideengrenzen über mehrere
  Absätze und deren Speicherung für alle sechs Varianten.
- Drei gezielte Geräteprüfungen bestanden: 2-Sekunden- und 1,5-Sekunden-Pause bei doppeltem
  Sprechtempo sowie Stoppen während der Pause ohne verspäteten Neustart.
- Der vollständige Variantenzyklus 1 → 2 → 3 → 4 → 5 → 6 → 1 läuft auch mit Pausen durch.
- Weckerkarten auf Außen- und Innendisplay in Hell/Dunkel geprüft: links schalten, rechts
  unabhängig aufklappen, alle bisherigen Aktionen im aufgeklappten Bereich. Ein über die
  Oberfläche neu angelegter Testwecker war sofort aktiviert und zugeklappt. Ausschalten öffnet
  keine Details. Der Testwecker wurde anschließend wieder gelöscht.

### Zuverlässige Weckannahme (1.1.11)

- Ein ausgelöster Termin wird geprüft (`at` muss zum gespeicherten Vorkommen passen) und dann
  **in einem einzigen Speichervorgang** angenommen: Folgezustand und Klingelauftrag samt Zeitmetadaten
  (`ringingMeta`: Vorkommen und Annahmezeitpunkt). Ältere Einträge ohne Metadaten bleiben gültig.
- Der Weckdienst wird **vor** der Folgeplanung gestartet. Scheitert die Planung, klingelt der aktuelle
  Wecker trotzdem; die Karte zeigt den Hinweis, ein neuer Versuch folgt nach dem Klingeln bzw. beim App-Start.
- Doppelte Broadcasts klingeln nicht doppelt. Wiederherstellung holt ein unterbrochenes Klingeln genau
  einmal über den Schlummer-Slot nach und verschiebt `nextAt` dabei nicht; nur ein **bekannt** über
  12 Stunden altes Klingeln wird verworfen und sichtbar gemeldet.
- Schlummern speichert zuerst und nimmt den Zustand zurück, wenn die Planung scheitert.
- Stoppen bleibt im laufenden Prozess wirksam, auch wenn das Entfernen des Auftrags nicht gespeichert werden kann.
- Lehnt Android den Dienststart ab, folgt ein begrenztes Ersatzsignal (System-Weckton, max. 10 Minuten,
  endet mit dem echten Dienst). Best effort: verweigerte Benachrichtigungen, gesperrter Kanal, Nicht stören,
  erzwungenes Beenden oder ein ausgeschaltetes Gerät kann die App nicht umgehen.
- Tests: `AlarmClaimTest` (11 JVM) und `AlarmClaimIntegrationTest` (6 Geräteprüfungen, nicht invasiv,
  Fehlerinjektion nur für die eigene Test-ID).
