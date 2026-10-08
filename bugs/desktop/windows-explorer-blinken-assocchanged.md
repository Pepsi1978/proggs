# Explorer und Desktop-Symbole blinken im Sekundentakt (SHCNE_ASSOCCHANGED-Sturm)

**Stand:** 08.10.2026, 22:27 Uhr · Windows 11 26H2 (Build 26300) · Edge/WebView2 154.0.4258.62 · Lively Wallpaper 1.0.164 (Store-App)

## Symptom
Alle Symbole auf dem Desktop und in jedem Explorer-Fenster blinken etwa ein- bis zweimal pro
Sekunde. „Dieser PC", Systemsteuerung und Papierkorb blinken nicht. `explorer.exe` verbraucht
dauerhaft ein bis zwei CPU-Kerne. Ein Neustart des Explorers hilft nur wenige Sekunden.

## Ursache
Ein Prozess meldet dem System im 500-ms-Takt „Dateizuordnung geändert" (`SHCNE_ASSOCCHANGED`).
Der Explorer lädt daraufhin jedes Mal alle Symbole neu.

**Die eigentliche Ursache ist ein leerer Schattenschlüssel in der privaten Registry einer Store-App.**
Store-Apps (MSIX) schreiben `HKCU` nicht in die echte Registry, sondern in eine private Datei:
`%LOCALAPPDATA%\Packages\<Paket>\SystemAppData\Helium\User.dat`. Dort lag bei Lively ein **leerer**
Schlüssel `FileExts\.pdf\UserChoice`. Er verdeckt für alle Prozesse der App die echte Zuordnung:

| Sicht | `FileExts\.pdf\UserChoice` |
|---|---|
| echte Registry | `ProgId=Acrobat.Document.DC`, `Hash=…` (gültig) |
| aus dem Lively-Paket | Schlüssel vorhanden, **keine Werte** |

Livelys `msedgewebview2.exe` sieht also „keine Standard-App" (im Protokoll: `Currentdefaultprogid: .`),
will auf `MSEdgePDF` zurücksetzen, der Schreibzugriff scheitert mit `0x80070005` (UCPD-Schutz), die
Änderungsmeldung geht trotzdem raus, der WebView fragt wegen der Meldung erneut ab. Endlosschleife.

Entstanden ist der Schattenschlüssel vermutlich beim Edge-Update am 08.10.2026 um 12:50 Uhr: Um
13:00:41 Uhr wurde die echte Zuordnung auf `MSEdgePDF` geschrieben, ein Reset innerhalb des Pakets
blieb auf halbem Weg stecken (Schlüssel angelegt, Werte verweigert). Belegt ist der Zeitpunkt, nicht
der genaue Ablauf.

## Zwei Irrwege am 08.10.2026
1. **Mittags:** nur Lively beendet. Beim nächsten Explorer-Neustart startete Lively mit, der Sturm
   war wieder da.
2. **Abends:** die echte Zuordnung galt als ungültig (`UserChoice=MSEdgePDF`,
   `UserChoiceLatest=Acrobat.Document.DC`). Frank stellte in den Einstellungen auf Acrobat um, beide
   Einträge waren danach gleich und gültig. **Der Sturm lief trotzdem weiter**, weil Lively die echte
   Zuordnung gar nicht sieht.

Lehre: Bei einer Store-App immer die Sicht **aus dem Paket** prüfen, nicht nur die echte Registry.

## Fix
Die App beenden (bei Lively zuerst `Lively.Watchdog`), dann die private Registry-Datei beiseitelegen:

```powershell
$h = "$env:LOCALAPPDATA\Packages\<Paketfamilie>\SystemAppData\Helium"
Move-Item "$h\User.dat" "$h\User.dat.blinkschutz-sicherung" -Force
```

Windows legt beim nächsten Start eine frische Datei an, die App sieht wieder die echte Zuordnung.
In der Datei liegen nur Zwischenstände von WebView2/Edge (Update-Status, Absturzzähler, Zonen),
keine Einstellungen von Lively. Die liegen als JSON unter `LocalCache\Local\Lively Wallpaper`.
Geprüft 08.10.2026, 22:24 Uhr: Lively läuft, 0 Rücksetzversuche in 55 Sekunden.

## Dauerhafte Absicherung: Explorer-Blinkschutz
`Werkzeuge/explorer-blinkschutz/` (Aufgabe „Explorer-Blinkschutz", läuft ohne Admin-Rechte). Neuer
Rechner: einmal `einrichten.ps1` ausführen.

- Auslöser: Ereignis 62441 im Protokoll `Microsoft-Windows-Shell-Core/AppDefaults`, dazu Anmeldung
  und alle 5 Minuten.
- Ab 6 Rücksetzversuchen in 10 Sekunden gilt es als Sturm. Der Wächter löst den Absender auf
  (bei `msedgewebview2.exe` das Wirtsprogramm hinter `--webview-exe-name=`) und beendet das
  Wirtsprogramm.
- **Store-App:** Er legt die private Registry-Datei beiseite und startet die App sofort wieder.
  Höchstens ein Versuch pro Paket in 30 Minuten.
- **Kein Paket oder Sturm kommt wieder:** Programm bleibt gestoppt, die Standard-Apps-Seite öffnet
  sich, eine Meldung nennt den nötigen Klick. Sobald sich `UserChoice` ändert, startet das Programm
  wieder.
- Geschützte Prozesse (Explorer, Edge, Chrome, Einstellungen, Systemprozesse) beendet er nie, dort
  kommt nur die Meldung.
- Log: `%LOCALAPPDATA%\explorer-blinkschutz\blinkschutz.log`.
- Echttest 08.10.2026, 22:26 Uhr: defekte Datei absichtlich zurückgespielt, Lively gestartet. Sturm
  nach 8 Rücksetzversuchen erkannt, Datei beiseitegelegt, Lively neu gestartet, danach 0 Versuche.
  Rund 7 Sekunden vom ersten Blinken bis zur Heilung, ohne Klick.

## Diagnose
1. **Windows-Protokoll lesen** (nennt Absender-PID und Dateityp direkt):
   `Get-WinEvent -LogName 'Microsoft-Windows-Shell-Core/AppDefaults' -MaxEvents 20`
   Ereignis 62441 „Benutzerauswahl für .pdf zurückgesetzt", 62443 `SetDefault-Error … 0X80070005`.
   Hunderte Einträge pro Minute = Sturm.
2. **Absender auflösen:** PID aus dem Ereignis, bei `msedgewebview2.exe` steht das Wirtsprogramm in
   der Kommandozeile hinter `--webview-exe-name=`.
3. **Sicht aus dem Paket prüfen** (Store-App, App muss nicht laufen):
   `Invoke-CommandInDesktopPackage -PackageFamilyName <Familie> -AppId App -Command cmd.exe -Args '/c <skript.cmd>' -PreventBreakaway`
   mit `reg query "HKCU\…\FileExts\.pdf" /s > datei.txt` im Skript. Zeigt `UserChoice` ohne Werte,
   ist es der Schattenschlüssel.
4. **Gegenprobe:** Wirtsprogramm beenden, Explorer-CPU fällt auf nahe null.

## Fallen
- `UserChoice` lässt sich per Skript weder löschen noch schreiben, auch nicht aus dem Paket heraus
  (`reg delete` → „Zugriff verweigert"). Nie per Skript an `UserChoice` schreiben.
- Die private `User.dat` lässt sich nicht offline bearbeiten: `RegLoadAppKey` meldet „Die Datenbank
  der Konfigurationsregistrierung ist beschädigt" (Fehler 1009). Es bleibt nur das Beiseitelegen.
- Ein Wächter darf kein Meldungsfenster im eigenen Prozess zeigen. Am 08.10.2026 um 22:19 Uhr hing
  er am offenen Fenster, hielt seine Sperre und ließ den nächsten Sturm 30 Sekunden laufen. Meldungen
  gehören in einen eigenen Prozess.
- Der Explorer-Neustart startet auch Lively neu, der Sturm ist sofort wieder da.
- Lively hat einen Wachhund (`Lively.Watchdog`), der es sofort neu startet. Immer zuerst den beenden.
- Das Protokoll ist ringförmig und fasst im Sturm nur rund 4,5 Minuten. Den ersten Auslöser findet
  man dort nicht mehr, wohl aber über den Zeitstempel des Registry-Schlüssels (`RegQueryInfoKey`) und
  das Änderungsdatum des Edge-Programmordners.
- Nur der eine WebView2 der Store-App stürmt. Die WebView2-Prozesse anderer Programme laufen
  daneben unauffällig, das ist der Hinweis auf die paketeigene Registry.
