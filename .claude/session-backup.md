# Session Handoff — 23.09.2026 20:25

## Ziel (1-3 Saetze)
adb über WLAN zum Handy (Samsung Galaxy Fold SM-F971B, Android 17, Seriennummer R3GL7073MLM, Heim-IP 192.168.0.155) soll dauerhaft ohne Kabel funktionieren: nach PC-Neustart, Handy-Neustart, beidem, in jedem WLAN. Fix nach Direktive 3, danach Logik-Review in Schleife + Recherche (Engine C, Sonnet-Schwarm).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
- **Keine laufende Aufgabe**, alles committet und gepusht.
- **Offene Frage an den Benutzer (letzte Nachricht):** Angebot, einen ECHTEN Handy-Neustart-Test zu machen: `adb -s 192.168.0.155:5555 reboot`, Benutzer entsperrt das Handy danach einmal, dann prüfen ob die Verbindung ohne Kabel binnen ~2-3 Min von selbst zurückkommt (Wachhund-Log `%LOCALAPPDATA%\adb-wlan\adb-wlan.log`, `adb devices`, `adb logcat -d -s WlanDebug`). Nur nach "ja" ausführen — Handy-Neustart nicht ungefragt.

## Aktueller Status
- Erledigt (alle gepusht): 5ede9b53a (Grundlösung), ec0b9fc47 (Lebenszeichen, Serial-Bindung, feste SDK-adb), dfac236c5 (UpdateStation WorkManager statt PendingIntent), 646556a0b (Wachhund nach Netzbeitritt), a0b7329eb (UpdateStation 1.0.11 Nachprüfen+Retry), 894e24801 (Pflege über WLAN, Doze-Ausnahme, Wachhund 2 Min, Almanach, Hint-Hook), 3ec9a38bc (Android-Studio-Einstellung dokumentiert).
- Handy: UpdateStation 1.0.11 per WLAN installiert, WRITE_SECURE_SETTINGS granted, deviceidle-Whitelist gesetzt. (In dem Build steckte eine uncommittete Quellen.kt-Änderung einer parallelen UpdateStation-Sitzung; die Sitzung hat inzwischen weiter committet.)
- PC: Aufgabe `adb-wlan-wachhund` (Anmeldung +1 Min, Ereignis 10000 NetworkProfile/Operational +20 s, alle 2 Min; conhost --headless powershell adb-wlan.ps1 -Leise). Benutzer-PATH: SDK platform-tools zuerst; Benutzervariable ADB = %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe. Android Studio: other.xml PropertyService keyToString "AdbOptionsService.use.user.managed.adb": "true", Port 5037 (Backup other.xml.vor-adb-server.bak).
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- `Werkzeuge/adb-wlan/adb-wlan.ps1` — Hauptskript (BOM nötig für powershell 5.1): mDNS, Serial-Bindung (~/.adb-wlan-serial), Lebenszeichen mit Zeitlimit (AdbZeit), TLS→tcpip 5555, Pflege (pm grant + deviceidle, stündlich), Mutex, Log.
- `Werkzeuge/adb-wlan/adb-wlan.sh` — macOS-Spiegel (adbzeit ohne timeout-Befehl).
- `Werkzeuge/adb-wlan/wachhund-einrichten.ps1` — registriert die Aufgabe (idempotent).
- `UpdateStation/app/src/main/java/de/frank/updatestation/WlanDebugReceiver.kt` — Boot/Replaced → planen(); WorkManager-Aufgabe `Aufgabe` (UNMETERED, KEEP) setzt adb_wifi_enabled=1 bei WLAN mit IPv4, liest nach 3 s nach, retry max 5, einmal pro networkHandle.
- `UpdateStation/.../UpdateStationApp.kt` — onCreate ruft WlanDebugReceiver.planen(this).
- `best-practices/android/adb-wlan-debugging.md` (+ -kurzcheck), `bugs/android/adb-wlan-debugging.md` (+ -kurzcheck, 11 Einträge), `bugs/README.md` Index-Zeile, `claude-code-setup/hooks/bug-almanac-hint.py` + Spiegel `Umgebung/Hooks/` (AREAS-Eintrag android/adb-wlan-debugging).
- Recherche-Rohdaten: `~/.research-swarm/adb-wlan/r1.md … r6.md`.
- Memory: `OpenLauncher/Profiles/ClaudeCode/minimal/projects/C--Users-barwa-proggs/memory/adb-wlan-selbstheilend.md` (gitignored, lokal).

## Getroffene Entscheidungen
- Stabile Serial bleibt IP:5555 (Regel 18), TLS nur als Rückweg — TLS-Port nie speichern.
- Wachhund nie kill-server (trennt andere Sitzungen/Emulatoren); nur nicht-leiser Aufruf darf einmal neu starten.
- Guard-Hook bewusst NICHT erweitert (selten geänderte Skripte; Hint + Index reichen).
- Einmal pro WLAN-Verbindung einschalten (keine Nachfrage-Spam in fremden Netzen, manuelles Aus bleibt).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- `registerNetworkCallback(request, PendingIntent)` als Trigger: Android 17 RELEASEd die Anmeldung ~5 s nach Prozessstart — NICHT verwenden.
- Neu-Anmelden im ausgelösten Empfänger → Endlosschleife (760 Anmeldungen/s, 57 % CPU) — nie aus Empfänger heraus neu anmelden/planen.
- `unregisterNetworkCallback(pi)` direkt vor `register` → löscht die neue Anmeldung (async).
- ADB_MDNS_AUTO_CONNECT=adb-tls-connect → verbindet nach Serverstart NICHT automatisch.
- Tests mit `run-as rm shared_prefs` wirken nur nach Prozess-Kill (Prefs im RAM gecacht); `am force-stop` widerruft PendingIntents verzögert → Testartefakte. Für Tests `am kill` + `am broadcast -f 0x20 -n de.frank.updatestation/.WlanDebugReceiver`.
- Remove-Item in einem Befehl mit "C:\Program Files\..." wird vom Schutzfilter blockiert → getrennt ausführen.

## Wichtige Recherche-Ergebnisse
- Android 17 "adb Wi-Fi 2.0": vertraute Netze nach SSID+BSSID, System schaltet dort selbst ein — ergänzt unsere App, kollidiert nicht.
- Auto-Connect des adb-Servers ist ereignisgesteuert → aktiver Wachhund ist Best Practice. adb 37.0.0: mDNS-Backend libadbmdns.
- Samsung: Telefon-MAC für DHCP-Reservierung; "Zu mobilen Daten wechseln" aus; Auto Blocker sperrt nur USB.

## Naechste Schritte (priorisiert)
1. Antwort des Benutzers auf den Handy-Neustart-Test abwarten; bei "ja" Test fahren und Ergebnis melden.
2. Benutzer erinnern (falls noch nicht erledigt): Entwickleroption "ADB-Autorisierungstimeout deaktivieren" einschalten.
3. Optional: DHCP-Reservierung im Tenda-Router + Telefon-MAC; Tailscale für Firmennetz.

## Offene Fragen
- Soll der echte Handy-Neustart-Test laufen? (Benutzer muss danach entsperren.)

## Anker
- Branch: main
- Letzte Commits:
da8ecb29c härte APK-Updates: Versionslog-Notizen robust maskieren; UpdateStation prüft nie doppelt gleichzeitig
9643cb1dc härte APK-Updates: Wiederanlauf und Wiederholung ohne unnötigen Bump, Build-Ausgabe über Metadaten; UpdateStation liest Manifest und Dateiliste stimmig
3ec9a38bc best-practices: Android Studio nutzt den extern verwalteten adb-Server, dokumentiert
894e24801 härte WLAN-adb nach Recherche: Pflege über WLAN mit Doze-Ausnahme, Wachhund alle 2 Minuten, Almanach und Best Practices, Hint-Hook registriert
a0b7329eb UpdateStation: prüfe WLAN-Debugging nach dem Einschalten nach und wiederhole begrenzt
