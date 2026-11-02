# Befehle auf dem Handy (Shell ohne Root)

Stand 02.11.2026, Galaxy Fold 8 (SM-F971B), Android 17, One UI 9. Aufruf: `H sh "<befehl>"`. Mehrere in einem `sh` mit `;` sparen Zeit. „Ungeprüft“ heißt: nur aus der Hilfe oder einer Quelle, auf dem Gerät noch nicht ausgeführt, also Ergebnis nachlesen.

`cmd`, `dumpsys` und `settings` brauchen 45–75 ms, `content query` etwa 1 s. `svc wifi`, `svc data`, `svc bluetooth`, `media` und `cmd clipboard` gibt es hier nicht mehr.

## Einstellungen finden, prüfen, setzen

```
settings list system|secure|global | grep -i <wort>     Schlüssel suchen, schnellster Weg
settings get <namespace> <schluessel>
settings put <namespace> <schluessel> <wert>            danach neu lesen; vorher alten Wert notieren
am start -a android.settings.<SEITE>                    Seite direkt öffnen, dann tree
```

Seiten: `SETTINGS`, `WIFI_SETTINGS`, `BLUETOOTH_SETTINGS`, `DISPLAY_SETTINGS`, `SOUND_SETTINGS`, `LOCATION_SOURCE_SETTINGS`, `BATTERY_SAVER_SETTINGS`, `APPLICATION_SETTINGS`, `APP_NOTIFICATION_SETTINGS`, `DATA_ROAMING_SETTINGS`, `ACCESSIBILITY_SETTINGS`, `DATE_SETTINGS`, `LOCALE_SETTINGS`, `SECURITY_SETTINGS`. Einzelne App: `am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:<paket>`.

Einstellung ohne bekannten Schlüssel: Seite öffnen, in der App die Suche nutzen (`H tap`, `H text`), Treffer antippen, im `tree` den Schalter `[an]`/`[aus]` lesen. Samsung-eigene Schalter stehen oft nur in der Oberfläche.

## Geräte-Zustand

```
dumpsys power | grep mWakefulness=                      wach oder aus
dumpsys window | grep isKeyguardShowing                 Sperrbildschirm
dumpsys window | grep -E 'mCurrentFocus=Window'         App im Vordergrund
dumpsys battery                                         Akku
cmd power wakeup    cmd power set-mode <n>
cmd display get-brightness    cmd display set-brightness <wert>
```

## Funk und Ton

```
cmd wifi status                 cmd wifi list-scan-results
cmd bluetooth_manager enable    cmd bluetooth_manager disable
cmd location is-location-enabled      cmd location set-location-enabled true|false
cmd connectivity airplane-mode enable|disable           ungeprüft; trennt den eigenen WLAN-Zugang
cmd notification set_dnd off|priority|alarms|none       Nicht stören
cmd uimode night yes|no|auto                            Dunkelmodus
cmd media_session volume --stream 3 --get               0 Anruf, 1 System, 2 Klingeln, 3 Medien, 4 Wecker, 5 Hinweise
cmd media_session volume --show --stream 3 --set 9
cmd media_session dispatch play-pause|next|previous|stop
```

## Apps

```
cmd package list packages -3                   Dritt-Apps, ohne -3 alle
cmd package resolve-activity --brief -a android.intent.action.VIEW -d <url>      Ziel vor dem Start prüfen
monkey -p <paket> -c android.intent.category.LAUNCHER 1        Start ohne Activity-Namen (das macht H open)
am start -a android.intent.action.VIEW -d <url>
am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "Text"     Teilen, Auswahl per Tippen
am start -a android.intent.action.SET_ALARM --ei android.intent.extra.alarm.HOUR 7 --ei android.intent.extra.alarm.MINUTES 30      ungeprüft
pm grant <paket> <recht>    pm revoke <paket> <recht>     cmd appops get <paket>         Rechte: nur nach Franks Ja
cmd role get-role-holders android.app.role.SMS            Standard-Apps
```

Samsung-Pakete: Telefon `com.samsung.android.dialer`, Browser `com.sec.android.app.sbrowser`, Nachrichten `com.google.android.apps.messaging`, Einstellungen `com.android.settings`.

## Benachrichtigungen

```
cmd notification list                       Schlüssel
dumpsys notification --noredact             Klartext; ohne den Schalter ist der Text geschwärzt
cmd statusbar expand-notifications          aufklappen, danach tree und tap
cmd statusbar expand-settings               Schnelleinstellungen
cmd statusbar collapse
cmd notification snooze --for 60000 '<key>'
```

Eine Aktion ausführen oder wegwischen geht nur über die Oberfläche: aufklappen und tippen.

## Daten (privat)

Lesen geht, auf Franks Wunsch beschränken und nie vollständig in Berichte kopieren: `content query --uri content://com.android.calendar/events --projection title:dtstart`, ebenso `content://com.android.contacts/data/phones`, `content://sms/inbox`, `content://call_log/calls`. Für Termine, Aufgaben und Mails zuerst die Jarvis-Werkzeuge nehmen, wenn sie da sind.

Schreiben über `content insert|update|delete` ist nicht rückgängig zu machen: nur nach Franks Ja.

## Dateien, Aufnahme, Logs

```
adb push <datei> /sdcard/...     adb pull /sdcard/...             auf dem Rechner ausführen
screenrecord --time-limit 10 /sdcard/a.mp4                        danach pull und löschen; maximal 180 s, ohne Ton
logcat -d -t 200                 logcat -d -s <TAG>:V             Fehlersuche
dumpsys meminfo <paket>          dumpsys gfxinfo <paket>
```

## Finger weg

| Befehl | Folge |
|---|---|
| `pm clear`, `pm uninstall`, `content delete`, `settings delete` | Datenverlust |
| `am force-stop de.frank.jarvis`, `am kill-all` | beendet Jarvis, Wecker und Dienste |
| `cmd wifi set-wifi-enabled disabled`, `settings put global adb_enabled 0`, `adb_wifi_enabled 0` | kappt den eigenen Zugang |
| `cmd lock_settings ...` | ändert die Sperre; `verify` zählt Fehlversuche |
| `cmd bluetooth_manager factoryReset` | löscht alle Kopplungen |
| `cmd testharness enable` | Werksreset |
| `reboot`, `svc power reboot|shutdown` | Neustart; Port 5555 ist danach weg |
| `wm size`, `wm density` | bleibt nach Neustart, nur mit `reset` zurück |
| `dumpsys battery set`, `cmd statusbar send-disable-flag`, `cmd role remove-role-holder` | verfälscht oder sperrt, bis es zurückgenommen wird |
| `tel:` mit Notrufnummern | nie per Skript wählen |
