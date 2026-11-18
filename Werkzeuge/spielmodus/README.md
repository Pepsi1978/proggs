# Spielmodus (Windows)

Version 1.4.1 · Stand 18.11.2026, 17:04 Uhr

Hält beim Online-Spielen über die schmale 5G-Leitung (Tenda-Router, O2) alles von der Leitung fern, was Ping,
Latenz und Paketverluste verursacht. Hintergrund: Gaming Services (Xbox-App) luden mit 12 parallelen Verbindungen
Shader und trieben den Ping von 30 ms auf über 1500 ms (Almanach `bugs/gaming/netzwerk-latenz-5g-router.md`, B1–B3).

## Was es tut
- **Wächter** (`waechter.ps1`, geplante Aufgabe „Spielmodus-Waechter“, bei Anmeldung, höchste Rechte, unsichtbar, niedrige Priorität):
  Läuft `steam` oder `cod` (auch ältere `codNN-cod`/`spNN-cod`), wird `InstallService` (Store-Installationen)
  gestoppt und sein Starttyp gesperrt. `GamingServices`/`GamingServicesNet` sind geschützte Paket-Dienste:
  Sie werden sauber gestoppt (nie hart beendet: das wertet Windows als Absturz und startet sie sofort neu)
  und bei jedem Wiederanlaufen erneut gestoppt – aber **nur solange CoD nicht läuft**: CoD startet sie im Spiel
  selbst neu (gemessen ~30 Runden je Sitzung), Bekämpfen bringt dann keine Bandbreite, nur Ruckelrisiko.
  `WinStore.App` und die Xbox-App werden beendet.
  Stoppen wartet insgesamt höchstens 3 s, Starten wartet nie (`sc.exe start`). Ein Prozess-Schnappschuss alle 4 s,
  Protokoll nur bei Zustandswechseln. Aufgehoben wird die Sperre 8 s nach dem letzten Auslöser (Steam-Selbstupdate
  löst sie nicht kurz auf), dann meldet ein Statusfenster „Spielmodus pausiert“. Danach: Starttyp zurück, Gaming
  Services nur dann neu gestartet, wenn sie vorher liefen.
- **Google Drive** (Sperre aktiv = Steam oder CoD läuft): Firewall-Sperrregel für `GoogleDriveFS.exe`
  (Gruppe `Spielmodus`, jede Minute auch für eine neu aktualisierte Drive-Version). Drive bleibt offen und die Dateien
  bleiben lokal nutzbar, nur der Abgleich pausiert. Danach läuft er von selbst weiter.
- **Spiel-Firewall** (nur solange ein echtes Steam-Spiel läuft: Steam-Prozess da und `RunningAppID` ≠ 0, nicht bei Steam
  allein und nicht bei Steam-Werkzeugen wie Wallpaper Engine, SteamVR, Soundpad, Lossless Scaling; startet ein solches
  Werkzeug während des Spiels, bleibt die Firewall an, solange eine Programmdatei des Spiels läuft):
  - Windows-Firewall „ausgehend standardmäßig blockieren“. Vorhandene fremde ausgehende Freigaben (Claude Desktop, Teams,
    Store-Apps, Telemetrie, Sicherung …) werden vorübergehend abgeschaltet, nur das Kernnetzwerk (DNS, DHCP) bleibt.
    Eingehend blockt Windows ohnehin, eine Anfrage von außen kommt also auch nicht durch.
  - **WLAN-Hintergrundsuche aus** (`netsh wlan set autoconfig enabled=no`): beseitigt die AX201-Ping-Spitzen etwa alle
    10 Minuten (B3). Die Verbindung bleibt, nur Suchen/Roaming pausieren. Bricht das WLAN im Spiel ab, verbindet es
    sich erst nach dem Spiel neu.
  - Erlaubt (Gruppe `Spielmodus-Erlaubt`, bleibt dauerhaft, ist ohne Block wirkungslos; Regeln deinstallierter Programme
    werden aufgeräumt): alle .exe im Spielordner (aus `appmanifest_<id>.acf`, neu gescannt nur nach einem Spiel-Update),
    Steam, Anti-Cheat (EAC, BattlEye, EA AC), Launcher (EA app, Ubisoft Connect, Rockstar), Google Chrome, das Heimnetz
    (`LocalSubnet`, z. B. Tenda-Router-Seite), Ping (IPv4 und IPv6), verschlüsseltes DNS und die Windows-Internetprüfung,
    die Programmier-CLIs samt Werkzeugen (Claude Code, Codex, OpenCode, OpenLauncher, node, git mit Anmeldehelfer, gh,
    curl, pwsh/PowerShell, python).
  - Beim Sperren trennt er die schon offenen IPv4-Verbindungen gesperrter Programme (auch Google Drive und
    Windows Update unter svchost; DNS und Internetprüfung bleiben), denn eine Firewall-Sperre stoppt nur neue Verbindungen.
  - Git wird in allen Varianten erkannt (`Git\ucrt64` und `Git\mingw64`).
  - Findet der Wächter keine Programmdatei des Spiels, sperrt er nicht (sonst wäre das Spiel selbst offline).
  - Kein Statusfenster beim Spielstart; alle Fenster nehmen nie den Fokus (`WS_EX_NOACTIVATE`).
  - 8 s nach Spielende ist alles wieder offen („Spiel beendet“-Fenster).
- **Absturzsicher:** Der Zustand steht in `firewall.json`, **bevor** etwas geändert wird (atomar geschrieben).
  Scheitert das Zurückstellen, versucht der Wächter es alle 30 s erneut. Startet der Wächter mitten im Spiel neu, bleibt
  die Firewall an; sonst stellt er zurück. Zusätzlich stellt die Aufgabe **„Spielmodus-Notfall“** (`notfall.ps1`, SYSTEM,
  bei jedem Systemstart, vor der Anmeldung) alles zurück. So bleibt der PC nach Stromausfall oder Absturz nie offline.
- **Desktop „Spielmodus an-aus“** (`umschalten.ps1`): Doppelklick schaltet den Wächter aus/an (Datei `aus.flag`)
  und zeigt unten rechts ein eigenes Statusfenster (grün = AN, grau = AUS, 5 s, Klick schließt; `meldung.ps1`).
  Die Windows-Sprechblase wurde unter Windows 11 verschluckt. Ein zweiter Klick innerhalb von 3 s schaltet nicht zurück.
- **Steam-Start:** Der Wächter meldet beim Start von Steam per Statusfenster, ob der Spielmodus AN oder AUS ist.
- **Einrichtung** (`einrichten.ps1 [-TreiberZip <zip>]`, hebt sich selbst auf Admin):
  Kopiert die Skripte nach `%ProgramFiles%\Spielmodus` (nur Admins dürfen dort schreiben); beide Aufgaben und das
  Desktop-Symbol laufen nur von dort, nie aus dem Git-Ordner (sonst könnte jedes Programm die Admin-Skripte ändern).
  Baut dort `meldung.dll` (Statusfenster ohne Kompilieren je Meldung).
  Store-Automatikupdates aus (`AutoDownload=2`, Wirkung unter Windows 11 Home unbelegt), Übermittlungsoptimierung
  gedeckelt (Hintergrund 10 %, Vordergrund 50 %) und **ohne Hochladen an andere PCs** (`DODownloadMode=0`),
  Game-Bar-Aufnahme aus (verhindert das „ms-gamingoverlay“-Fenster bei fehlender Game Bar), alten Wächter stoppen,
  Aufgaben + Verknüpfung anlegen. Optional Intel-WLAN-Treiber (Sicherung per `pnputil /export-driver` mit Prüfung,
  nur `netwtw*.inf`, deren eigener Katalog gültig von Microsoft oder Intel Corporation signiert ist).
  Danach „Bevorzugtes Band“ = 5 GHz (Wert 2) und „Roaming-Dynamik“ = am niedrigsten (Wert 0); der Adapter startet nur
  neu, wenn sich ein Wert wirklich geändert hat. Der WLAN-Adapter wird am Medientyp erkannt, nicht am Namen.
- **Entfernen** nur über die CLI: `zurueck.ps1` (bewusst kein Desktop-Symbol). Stellt die in `vorher.json`
  gesicherten Ursprungswerte und eine noch aktive Spiel-Firewall wieder her, löscht beide Aufgaben, alle Firewall-Regeln,
  `%ProgramFiles%\Spielmodus`, Symbol und Zustandsdateien. Eine Firewall, die der Spielmodus nie verändert hat, bleibt unberührt.

## Dateien und Protokolle
`%ProgramFiles%\Spielmodus\` → laufende Kopie aller Skripte + `meldung.dll`.
`%LOCALAPPDATA%\Spielmodus\` → `waechter.log` (+ `.alt`), `einrichten.log`, `notfall.log`, `starttypen.json`,
`vorher.json`, `aus.flag`, `umschalten.zeit`, `spiele.json` (Programmdateien je Spiel und Build),
**`firewall.json`** (nur während eines Spiels: Ursprungszustand der Firewall; ist der PC offline, stellt
`zurueck.ps1` oder ein Neustart alles zurück), `treiber-sicherung-*`.

## Hinweise
- Änderungen im Repo wirken erst nach erneutem `einrichten.ps1` (kopiert nach Program Files).
- Solange Steam läuft, bekommen auch Store-Apps (z. B. Codex, VS Code als Store-Paket) keine Updates.
- Braucht ein Spiel die Gaming Services beim Start, den Wächter per Doppelklick ausschalten.
- Die Spiel-Firewall gilt nur für Steam-Spiele. CoD über Battle.net oder Game Pass bekommt nur die Sperre (Store, Xbox, Drive).
- Rest-Bandbreite im Spiel selbst: In CoD „On-Demand-Texturstreaming“ ausschalten (lädt sonst während der Runde nach, B2a).
  Steam lädt im Spiel standardmäßig nicht herunter (Einstellung „Downloads während des Spielens“ bleibt aus).
- Die erlaubten CLIs (Claude Code) aktualisieren sich selbst. Eine während des Spiels offene Sitzung kann dabei ein
  Update laden. Für sehr wichtige Runden CLI-Sitzungen schließen.
- Treiber zurück: Geräte-Manager → Intel Wi-Fi 6 AX201 → Treiber → „Vorheriger Treiber“, oder
  `pnputil /add-driver "%LOCALAPPDATA%\Spielmodus\treiber-sicherung-23.170.0.1\*.inf" /install`.
- Nur Windows (Microsoft Store/Gaming Services gibt es auf macOS nicht).
