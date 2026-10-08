# Explorer und Desktop-Symbole blinken im Sekundentakt (SHCNE_ASSOCCHANGED-Sturm)

**Stand:** 08.10.2026 · Windows 11 26H2 (Build 26300) · WebView2 154 · Lively Wallpaper 1.0.164

## Symptom
Alle Symbole auf dem Desktop und in jedem Explorer-Fenster blinken etwa ein- bis zweimal pro
Sekunde. `explorer.exe` verbraucht dauerhaft ein bis zwei CPU-Kerne. Ein Neustart des Explorers
hilft nur wenige Sekunden.

## Ursache
Ein Prozess meldet dem System im 500-ms-Takt „Dateizuordnung geändert" (`SHCNE_ASSOCCHANGED`).
Der Explorer lädt daraufhin jedes Mal alle Symbole neu.

Am 08.10.2026 war es der `msedgewebview2.exe` von **Lively Wallpaper**
(`Lively.Player.WebView2.exe`). Die Standardzuordnung für `.pdf` war ungültig
(`FileExts\.pdf\UserChoice` = `MSEdgePDF`, `UserChoiceLatest` = `Acrobat.Document.DC`). Der
WebView fragte die Zuordnung ab, Windows wollte sie auf `MSEdgePDF` zurücksetzen, der Schreibzugriff
scheiterte mit `0x80070005` (UCPD-Schutz), die Änderungsmeldung ging trotzdem raus, der WebView
fragte wegen der Meldung erneut ab. Endlosschleife.

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

## Fix
- Sofort: das Wirtsprogramm beenden (bei Lively zuerst `Lively.Watchdog`, sonst startet er neu).
- Dauerhaft: die Standard-App für den Dateityp einmal von Hand setzen, Einstellungen → Apps →
  Standard-Apps → `.pdf`. Erst danach das Wirtsprogramm wieder starten.

## Fallen
- `UserChoice` lässt sich per Skript weder löschen noch schreiben. UCPD meldet dabei irreführend
  „subkey does not exist". Nicht dagegen ankämpfen.
- Der Explorer-Neustart startet auch Lively neu, der Sturm ist sofort wieder da.
- Registry-Zeitstempel helfen nicht: die Schreibversuche scheitern, es ändert sich nichts.
- Prozesslisten zeigen keinen jede Sekunde neu startenden Prozess; der Absender läuft dauerhaft.
