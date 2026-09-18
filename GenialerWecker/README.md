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

### Bedienkomfort (1.1.47)

- Die Schnellwahl „in X Tagen“ erscheint nur noch, wenn das Startdatum tatsächlich zwischen heute
  und 60 Tagen liegt. Weiter entfernte Anker – auch ein in der Vergangenheit liegender Schichtanker –
  nennen stattdessen sachlich ihre Entfernung und verweisen auf den Kalender. Damit behauptet die
  Schnellwahl keinen geklemmten Wert mehr. Weil sie außerhalb dieses Bereichs gar nicht erscheint,
  kann ein weit entfernter oder zurückliegender Anker dort auch nicht versehentlich neu gesetzt
  werden; innerhalb von 0 bis 60 Tagen bleibt der Regler unverändert bedienbar.
- Die Stimmenliste in den Einstellungen sagt jetzt, warum sie leer ist: kein Treffer zur Suche, keine
  Favoriten für diesen Anbieter, oder beides. Der angebotene Knopf setzt genau die Filter zurück,
  die im Weg stehen. Eine noch nicht geladene eigene Stimmenliste wird weiterhin durch die
  vorhandenen Lade- und Fehlerhinweise erklärt, nicht als Filterproblem ausgegeben.

### Bedienkomfort (1.1.48)

- Ein noch nicht gespeicherter Sprachschlüssel überlebt jetzt das **Zuklappen der Karte
  „Sprachschlüssel"**; die zugeklappte Karte nennt das sachlich als „ungespeicherte Änderung"
  und stellt einen Entwurf nie als gespeicherten Schlüssel dar. Ausdrücklich **nicht** behoben
  sind Verluste beim Verlassen der Einstellungen, beim Drehen oder Falten und beim Prozessende –
  dort gilt weiterhin das bisherige Verhalten. Der Entwurf bleibt nur im Arbeitsspeicher der
  Einstellungsseite: nichts davon geht in den SavedState, auf die Platte oder ins Log. Beim
  Zuklappen ist ein sichtbar gemachter Schlüssel wieder maskiert.
- Bestätigungsdialoge benennen die Folge auf dem Knopf selbst: „Wecker löschen", „Stimme löschen"
  und „Übernehmen" statt eines allgemeinen „Bestätigen". Titel, Warntexte und „Abbrechen" sind
  unverändert, es kam kein zusätzlicher Dialog hinzu.

### Darstellung (1.1.49)

- **Weckbildschirm folgt der Hell-/Dunkel-Wahl.** Behoben wurden vier belegte Fehlerwege – welcher
  davon im beobachteten Fall überwog, ist **nicht** am Gerät nachgewiesen:
  1. Bei nicht lesbarem verschlüsseltem Speicher lieferte jeder Lesezugriff stillschweigend den
     Vorgabewert `"light"`. Der Weckbildschirm unterscheidet das jetzt über `SecureSettings.verfuegbar`
     und nimmt in diesem Fehlerfall dunkel. Eine ausdrücklich gewählte helle Oberfläche bleibt hell.
  2. Die Wahl wurde einmalig beim Aufbau gelesen. Da die Activity `singleTask` ist und ein weiteres
     Klingeln sie über `onNewIntent` wiederverwendet, gilt sie jetzt ab jedem `ON_RESUME` neu.
  3. Das Startfenster erbte die helle Material-Vorlage. `Theme.Wecker.Alarm` setzt dafür einen dunklen
     `windowBackground`, bis Compose zeichnet; die Oberfläche selbst folgt weiter der Einstellung.
  4. `enableEdgeToEdge()` ohne Argumente setzt Systemleisten-Scrims nach der Systemkonfiguration.
     Sie werden jetzt ausdrücklich passend zur Wahl und durchsichtig gesetzt.
  Vor der ersten Entsperrung wird weiterhin nichts Verschlüsseltes geöffnet; dort gilt dunkel.
- **Neue Einstellung „Darstellung → Ausrichtung"** mit Hochformat, Querformat und Automatisch.
  Automatisch ist der Vorgabewert und entspricht dem bisherigen Verhalten. Sie gilt für die
  Weckerliste und den Weckbildschirm und nutzt die bereits vorhandene Speicherung. **Grenze:**
  Android kann die Ausrichtung in geteilten Fenstern oder auf großen Displays vorgeben – ab einer
  kleinsten Fensterbreite von 600 dp wird `setRequestedOrientation()` ignoriert. Das befristete
  App-Opt-out `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` steht im Manifest und läuft mit
  targetSdk 37 aus; die Layouts bleiben adaptiv. Eine Garantie ist das nicht.
- **Nächster Termin oben:** heute nur die Uhrzeit, morgen „07:00 · morgen", ab übermorgen zuerst das
  Datum („So, 20.09. · 07:00"), bei einem anderen Jahr mit Jahreszahl. Entschieden wird nach
  lokalen Kalendertagen, nicht nach Stundenabstand. Der Weckername bleibt darunter stehen.
- **Vorlauf der Schlafenszeit-Erinnerung frei wählbar:** Regler von 0 bis 60 Minuten in ganzen
  Minuten, sichtbar nur bei eingeschaltetem Schalter. 15 Minuten bleiben die Vorgabe, auch für
  vorhandene Daten. **0 bedeutet „genau zur Schlafenszeit", nicht „aus"** – ausgeschaltet wird
  allein über den Schalter. Beim Ziehen wandert nur die Anzeige mit; gespeichert und umgeplant wird
  am Ende der Geste über denselben bestätigten Speicherpfad wie beim Schalter. Scheitert das
  Speichern, gilt der bisherige Wert weiter und die App sagt es.
  Ein bereits zugestellter Hinweis ertönt nach einer Vorlaufänderung nicht erneut: Fingerabdruck und
  Gruppe der Erinnerung hängen bewusst nicht vom Vorlauf ab. Jede Auslieferung wird gegen das mit dem
  aktuellen Vorlauf gültige Zeitfenster geprüft; was nicht mehr hineinfällt, wird verworfen. Eine
  Vorlaufänderung allein macht eine Auslieferung nicht ungültig. Bei 0 Minuten gilt eine Karenz von bis
  zu 60 Sekunden nach der Schlafenszeit, nie über die Weckzeit hinaus – ohne sie würde jede noch so
  kleine Zustellverzögerung von Android die Erinnerung stumm verwerfen. Bei einem Vorlauf über 0
  bleiben die bisherigen Verfallsregeln unverändert. Weck- und Schlummerplanung sind nicht berührt.

### Designentwürfe (1.1.50)

- Unter `design/` liegen **vier Designkonzepte** als lokale HTML-Vorschau, jeweils hell und dunkel,
  mit Startseite, Editor, Einstellungen und Alarmbildschirm: A Nachtatelier (redaktionell),
  B Morgenruhe (Tagesablauf an einer Achse), C Orbit (Instrumententafel), D Traumraum (skulptural).
  Vergleichsgalerie: `design/index.html`, Breiten 360 / 412 / 840. Einzelheiten in `design/README.md`.
- **Entschieden und beauftragt:** Einstellungen → Darstellung bekommt vier dauerhaft wählbare
  Designs — **Schlicht** (genau das heutige Erscheinungsbild, Vorgabe für bestehende und neue
  Nutzer), **Morgenruhe** (B), **Traumraum** (D) und **Orbit** (C). **Nachtatelier (A) wird nicht
  übernommen** und bleibt nur als Entwurf erhalten. Die Wahl gilt über Startseite, Editor,
  Einstellungen und Alarmbildschirm, nativ in Compose, unabhängig von Hell/Dunkel und Ausrichtung.
- **In 1.1.51 ist die native Optik noch unverändert.** Die Umsetzung folgt in einer eigenen Runde;
  bis dahin sieht die App aus wie bisher.

### Bedienkomfort der Designs (1.1.54)

- **Der Designwechsel lässt aufgeklappte Karten offen.** `DesignBlatt` hatte zwei getrennte
  `Column`-Aufrufstellen; ein Wechsel zu oder von Traumraum verwarf damit den ganzen Unterbaum, und
  jede Karte fiel auf ihren Ausgangszustand zurück. Jetzt gibt es genau eine Aufrufstelle, nur
  Modifier und Griffbalken hängen am Design. Die vier Abschnittsstile und das erneute Maskieren der
  Sprachschlüssel bleiben unverändert; die Schlüsselentwürfe liegen weiterhin ausschließlich im
  Arbeitsspeicher der Einstellungsseite, nie im SavedState und nie auf der Platte.
- **Die große Uhr auf dem Weckbildschirm passt sich der Breite an.** Bei Morgenruhe, Traumraum und
  Orbit wird die Schriftgröße **gemessen** statt geraten. Messung und Darstellung benutzen denselben
  Textstil — aus dem geerbten Stil plus Schrift, Gewicht und Farbe —, sodass auch Zeichenabstand und
  Systemskalierung eingehen. Die breiteste Ziffer wird ermittelt, nicht angenommen: alle zehn werden
  einmal gemessen und bilden das Muster „XX:XX". Von der Höchstgröße wird linear auf die verfügbare
  Breite gesucht: zwischen einer **nachweislich passenden** Unterseite und der zu großen Oberseite
  wird begrenzt halbiert, und zurückgegeben wird immer die zuletzt bestätigte Unterseite. Ein
  Abbruch der Suche gilt ausdrücklich **nicht** als Treffer.
  Bei genügend Platz bleibt es bei 76, 80 beziehungsweise 88 sp; bei Traumraum zählt die Breite
  abzüglich des Kuppelrands. Gerechnet wird einmal je Konfiguration, nicht je Minute.
  **Was zugesichert ist:** Die gewählte Größe wurde gemessen und passte für das Muster „XX:XX" in die
  berechnete Breite; die Uhr bricht nicht um und wird nicht mit Auslassungszeichen gekürzt.
  **Dokumentierter Sonderfall:** Ist die Breite unbekannt oder so klein, dass selbst 8 sp nicht
  hineinpassen, wird diese Größe ohne Zusicherung gesetzt — dann ist schlicht kein Platz vorhanden.
  Die Systemschriftgröße bleibt für alles andere wirksam. **Schlicht rechnet unverändert wie bisher.**
  Eine Abnahme auf dem Gerät oder über Gerätekombinationen hinweg ist damit nicht behauptet.
- Der Titel der Orbit-Kopfzeile darf zwei Zeilen belegen, damit „Wecker bearbeiten" in Versalien
  nicht früh gekürzt wird; Zurück, Hell/Dunkel und Zahnrad bleiben erreichbar.

### Vier Designs (1.1.52)

Unter **Einstellungen → Darstellung → Design** stehen vier dauerhaft wählbare Erscheinungsbilder.
Die Wahl ist **unabhängig** von Hell/Dunkel und von der Ausrichtung — jedes Design hat beide Modi —
und gilt für Weckerliste, Editor, Einstellungen und Weckbildschirm.

- **Schlicht** (Vorgabe) — genau das bisherige Gold auf Glas. `paletteFuer(SCHLICHT, …)` gibt die
  vorhandenen Objekte `DunkleGoldPalette` und `HelleGoldPalette` unverändert zurück, und
  `SchlichtGestalt` ruft weiterhin `SichtbarerHintergrund` und `GoldKarte` auf. Es wurde nichts
  nachgebaut; ohne eigene Wahl sieht die App aus wie zuvor.
- **Morgenruhe** — Salbei und Creme am Tag, Tannengrün in der Nacht, Terrakotta als Akzent. Die
  Weckerliste wird zur **Tagesachse**: jede Karte hängt als Station an einer durchgehenden Linie.
  Flache Flächen ohne Glanz, Pillenknöpfe, gestapelte Alarmtasten. Das **Bettmotiv** steht im Kopf.
- **Traumraum** — Pflaume, Rosé und Perlmutt. **Kuppel** mit zentriertem Kissenmotiv über dem Kopf,
  Termin und Restzeit in einer runden **Perle**, stark gerundete Flächen, runde Alarmtasten.
- **Orbit** — Fast-Schwarz mit Eisblau und Limette. **Instrumententafel**: kantige Module, eigene
  Kopfzeile mit fester Schrift, alle großen Zahlen in Monospace, Wecker als dichte Zeilen mit
  Termin auf einer Achse, zwei gleich große Alarmmodule. Im Kopfmodul steht die **freigestellte
  Sternbahn** an der Stelle des Zwölfstundenrings; Uhrzeit und nächster Termin bleiben daneben
  unverändert. Das Bild ist durchsichtig hinterlegt und trägt sich auf heller wie dunkler Fläche.

Nachtatelier (A) ist bewusst **nicht** übernommen und bleibt nur als Entwurf unter `design/`.

Technisch: `de/frank/wecker/design/` enthält Enum, Tokens, Paletten und vier Gestalten. Der geteilte
Code berechnet Zustand und Rückrufe einmal; die Gestalten zeichnen nur. **Alle Funktionen bleiben in
jedem Design vollständig**: Editor, Diktat, Stimmen, Schlüssel, Berechtigungen, Foto-Aufgabe,
Schlummern, Bereitschaft und Meldungen. Die Zustandsmaschine des Weckbildschirms — `PendingAction`,
Ring-Absicherung, Foto-Auslöser, `BackHandler`, Dienstkommunikation, Schlummern und Stoppen — ist
unangetastet; Designs ändern dort nur Anordnung, Form und Farbe. Die Mindestmaße der Alarmtasten
gelten in allen vier.

### Termin-Anzeige auf den Weckerkarten (1.1.51)

- Behoben: Die **große Überschrift einer Weckerkarte** zeigte immer die konfigurierte Weckzeit. Bei
  einem Monats-, Jahres- oder Intervallplan — etwa monatlich am 23., Start 23.09.2026, 15:45 — stand
  dort nur „15:45", obwohl der Termin Tage entfernt liegt. 1.1.49 hatte die Regel nur im globalen
  Kopfbereich eingeführt, die Karte blieb ausgelassen.
- Ein **aktiver** Wecker zeigt jetzt den tatsächlich geplanten nächsten Termin aus `nextAt`: heute nur
  die Uhrzeit, morgen die Uhrzeit mit dem Zusatz „morgen", ab übermorgen das lokale Kalenderdatum
  zuerst und die Uhrzeit danach, bei einem anderen Jahr mit Jahreszahl. Datum und Uhrzeit dürfen
  zweizeilig stehen, statt verkleinert oder abgeschnitten zu werden.
- Ein **ausgeschalteter** Wecker zeigt weiterhin seine konfigurierte Weckzeit — genau die bearbeitet
  der Editor. Schlummerhinweis, Weckername und das Uhrzeitfeld im Editor sind unverändert.
- Kopfbereich, zugeklappte und aufgeklappte Karte sowie die Speichervorschau nutzen jetzt **denselben
  Anzeigehelfer**, damit dieselbe Zeit überall gleich geschrieben wird. Planung, `AlarmTime` und
  Scheduler sind unangetastet; die Berechnung des Termins wurde nicht verändert.

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
