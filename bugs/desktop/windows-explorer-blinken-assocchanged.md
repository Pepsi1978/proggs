# Explorer und Desktop-Symbole blinken im Sekundentakt (SHCNE_ASSOCCHANGED-Sturm)

**Stand:** 08.10.2026, 22:20 Uhr · Windows 11 26H2 (Build 26300) · Edge/WebView2 154.0.4258.62 · Lively Wallpaper 1.0.164

## Symptom
Alle Symbole auf dem Desktop und in jedem Explorer-Fenster blinken etwa ein- bis zweimal pro
Sekunde. „Dieser PC", Systemsteuerung und Papierkorb blinken nicht. `explorer.exe` verbraucht
dauerhaft ein bis zwei CPU-Kerne. Ein Neustart des Explorers hilft nur wenige Sekunden.

## Ursache
Ein Prozess meldet dem System im 500-ms-Takt „Dateizuordnung geändert" (`SHCNE_ASSOCCHANGED`).
Der Explorer lädt daraufhin jedes Mal alle Symbole neu.

Die Kette am 08.10.2026 (zweimal am selben Tag aufgetreten):
1. **12:50 Uhr: Edge aktualisiert sich** auf 154.0.4258.62.
2. **13:00:41 Uhr:** `FileExts\.pdf\UserChoice` wird auf `MSEdgePDF` geschrieben. `UserChoiceLatest`
   steht seit 09.09.2026 unverändert auf `Acrobat.Document.DC`. Die beiden Einträge widersprechen
   sich, Windows hält die Zuordnung für ungültig (im Protokoll: `Currentdefaultprogid: .`, also leer).
3. Der `msedgewebview2.exe` von **Lively Wallpaper** (`Lively.Player.WebView2.exe`) fragt die
   Zuordnung ab. Windows will sie auf `MSEdgePDF` zurücksetzen, der Schreibzugriff scheitert mit
   `0x80070005` (UCPD-Schutz), die Änderungsmeldung geht trotzdem raus, der WebView fragt wegen der
   Meldung erneut ab. Endlosschleife.

Der Auslöser ist also das Edge-Update, Lively ist nur das Echo. **Mit jedem Edge-Update kann der
Fehler wiederkommen**, und jeder andere WebView2-Wirt kann ihn genauso auslösen.

## Dauerhafte Absicherung: Explorer-Blinkschutz
`Werkzeuge/explorer-blinkschutz/` (eingerichtet 08.10.2026, Aufgabe „Explorer-Blinkschutz", läuft ohne
Admin-Rechte). Neuer Rechner: einmal `einrichten.ps1` ausführen.

- Auslöser: Ereignis 62441 im Protokoll `Microsoft-Windows-Shell-Core/AppDefaults`, dazu Anmeldung
  und alle 5 Minuten.
- Ab 6 Rücksetzversuchen in 10 Sekunden gilt es als Sturm. Der Wächter löst den Absender auf
  (bei `msedgewebview2.exe` das Wirtsprogramm hinter `--webview-exe-name=`), beendet das
  Wirtsprogramm, öffnet die Standard-Apps-Seite und zeigt eine Meldung mit dem nötigen Klick.
- Geschützte Prozesse (Explorer, Edge, Chrome, Einstellungen, Systemprozesse) beendet er nie, dort
  kommt nur die Meldung.
- Er merkt sich das gestoppte Programm in `%LOCALAPPDATA%\explorer-blinkschutz\gestoppt.json` und
  startet es wieder, sobald sich `UserChoice` oder `UserChoiceLatest` geändert hat. Kommt der Sturm
  danach zurück, stoppt er erneut und merkt sich den neuen Stand (keine Endlosschleife).
- Log: `%LOCALAPPDATA%\explorer-blinkschutz\blinkschutz.log`.
- Echttest 08.10.2026, 22:17 Uhr: Lively gestartet, Sturm nach 8 Rücksetzversuchen (rund 4 Sekunden)
  erkannt und gestoppt.

## Die eigentliche Reparatur (ein Klick, nur von Hand möglich)
Einstellungen → Apps → Standard-Apps → oben nach `.pdf` suchen → App neu auswählen. Steht dort schon
die gewünschte App, einmal eine andere wählen und zurückstellen, damit Windows beide Einträge neu
schreibt. Prüfen: `UserChoice` und `UserChoiceLatest\ProgId` nennen danach dieselbe ProgId.

Am 08.10.2026 stand dieser Schritt nach dem ersten Vorfall im Almanach, wurde aber nicht ausgeführt
(Zeitstempel von `UserChoice` blieb 13:00:41). Beim nächsten Explorer-Neustart startete Lively mit
und der Sturm war wieder da. Deshalb gibt es jetzt den Wächter.

## Diagnose in drei Schritten
1. **Windows-Protokoll lesen** (nennt Absender-PID und Dateityp direkt):
   `Get-WinEvent -LogName 'Microsoft-Windows-Shell-Core/AppDefaults' -MaxEvents 20`
   Ereignis 62441 „Benutzerauswahl für .pdf zurückgesetzt", 62443 `SetDefault-Error … 0X80070005`.
   Hunderte Einträge pro Minute = Sturm.
2. **Absender auflösen:** PID aus dem Ereignis, bei `msedgewebview2.exe` steht das Wirtsprogramm in
   der Kommandozeile hinter `--webview-exe-name=`.
3. **Gegenprobe:** Wirtsprogramm beenden, Explorer-CPU fällt auf nahe null.

Ohne das Protokoll: ein verstecktes Fenster per `SHChangeNotifyRegister` anmelden und die
Ereignisse mitzählen (Ereignis `0x08000000`, Pfad `*.pdf`).

## Fallen
- `UserChoice` lässt sich per Skript weder löschen noch schreiben. UCPD meldet dabei irreführend
  „subkey does not exist". Nicht dagegen ankämpfen, nie per Skript an `UserChoice` schreiben.
- Der Explorer-Neustart startet auch Lively neu, der Sturm ist sofort wieder da.
- Lively hat einen Wachhund (`Lively.Watchdog`), der es sofort neu startet. Immer zuerst den beenden.
- Das Protokoll ist ringförmig und fasst im Sturm nur rund 4,5 Minuten. Den ersten Auslöser findet
  man dort nicht mehr, wohl aber über den Zeitstempel des Registry-Schlüssels `UserChoice`
  (`RegQueryInfoKey`) und das Änderungsdatum des Edge-Programmordners.
- Prozesslisten zeigen keinen jede Sekunde neu startenden Prozess; der Absender läuft dauerhaft.
- „Fix steht im Almanach" heißt nicht „Fix wurde ausgeführt": bei einem Schritt, den nur Frank
  machen kann, am Ende den Zeitstempel des Schlüssels prüfen und den offenen Schritt ausdrücklich melden.
