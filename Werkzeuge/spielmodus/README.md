# Spielmodus (Windows)

Version 1.3.1 · Stand 18.11.2026, 14:43 Uhr

Hält beim Online-Spielen über die schmale 5G-Leitung alle Microsoft-Store- und Xbox-Downloads fern.
Hintergrund: Gaming Services (Xbox-App) luden mit 12 parallelen Verbindungen Shader und trieben den
Ping von 30 ms auf über 1500 ms (Almanach `bugs/gaming/netzwerk-latenz-5g-router.md`, B1/B2).

## Was es tut
- **Wächter** (`waechter.ps1`, geplante Aufgabe „Spielmodus-Waechter“, bei Anmeldung, höchste Rechte, unsichtbar, niedrige Priorität):
  Läuft `steam` oder `cod` (auch ältere `codNN-cod`/`spNN-cod`), wird `InstallService` (Store-Installationen)
  gestoppt und sein Starttyp gesperrt. `GamingServices`/`GamingServicesNet` sind geschützte Paket-Dienste:
  Sie werden sauber gestoppt (nie hart beendet: das wertet Windows als Absturz und startet sie sofort neu)
  und bei jedem Wiederanlaufen erneut gestoppt – aber **nur solange CoD nicht läuft**: CoD startet sie im Spiel
  selbst neu (gemessen ~30 Runden je Sitzung), Bekämpfen bringt dann keine Bandbreite, nur Ruckelrisiko.
  `WinStore.App` und die Xbox-App werden beendet.
  Stoppen wartet höchstens 15 s (`-NoWait` + `WaitForStatus`), Prüfung alle 4 s über einen einzigen Prozess-Schnappschuss,
  Protokoll nur bei Zustandswechseln. Aufgehoben wird die Sperre erst, wenn 20 s kein Auslöser mehr läuft
  (Steam-Selbstupdate löst sie nicht kurz auf). Danach: Starttyp zurück, Gaming Services nur dann neu
  gestartet, wenn sie vorher liefen. Nach Absturz/Neustart während einer Sperre stellt er beim Start zurück.
- **Google Drive** (Sperre aktiv = Steam oder CoD läuft): Firewall-Sperrregel für `GoogleDriveFS.exe`
  (Gruppe `Spielmodus`). Drive bleibt offen und die Dateien bleiben lokal nutzbar, nur der Abgleich pausiert. Danach läuft er von selbst weiter.
- **Spiel-Firewall** (nur solange ein Spiel läuft: Steam `RunningAppID` ≠ 0 oder CoD, nicht bei Steam allein):
  Windows-Firewall „ausgehend standardmäßig blockieren“ (Ursprungswerte in `firewall.json`). Erlaubt sind
  (Gruppe `Spielmodus-Erlaubt`, bleibt dauerhaft, ist ohne Block wirkungslos): alle .exe im Spielordner
  (aus `appmanifest_<id>.acf`), Steam, Anti-Cheat (EAC/BattlEye), Google Chrome und das Heimnetz (`LocalSubnet`, z. B. Tenda-Router-Seite).
  Alles andere hat im Spiel kein Internet, auch Claude Code, Jarvis-PC-Brücke, WireGuard und Discord.
  20 s nach Spielende ist alles wieder offen. Nach einem Absturz stellt der Wächter beim Start zurück.
- **Desktop „Spielmodus an-aus“** (`umschalten.ps1`): Doppelklick schaltet den Wächter aus/an (Datei `aus.flag`)
  und zeigt unten rechts ein eigenes Statusfenster (grün = AN, grau = AUS, 5 s, Klick schließt; `meldung.ps1`).
  Die Windows-Sprechblase wurde unter Windows 11 verschluckt. Ein zweiter Klick innerhalb von 3 s schaltet nicht zurück.
- **Steam-Start:** Der Wächter meldet beim Start von Steam per Mitteilung, ob der Spielmodus AN oder AUS ist.
- **Einrichtung** (`einrichten.ps1 [-TreiberZip <zip>]`, hebt sich selbst auf Admin):
  Store-Automatikupdates aus (`AutoDownload=2`, Wirkung unter Windows 11 Home unbelegt), Übermittlungsoptimierung
  gedeckelt (Hintergrund 10 %, Vordergrund 50 %), Game-Bar-Aufnahme aus (verhindert das „ms-gamingoverlay“-Fenster
  bei fehlender Game Bar), alten Wächter stoppen, Aufgabe + Verknüpfung anlegen. Optional Intel-WLAN-Treiber
  (Sicherung per `pnputil /export-driver` mit Prüfung, nur `netwtw*.inf` mit gültiger Microsoft/Intel-Signatur).
  Danach „Bevorzugtes Band“ = 5 GHz (Wert 2) und „Roaming-Dynamik“ = am niedrigsten (Wert 0) mit einem Adapter-Neustart.
- **Entfernen** nur über die CLI: `zurueck.ps1` (bewusst kein Desktop-Symbol). Stellt die in `vorher.json`
  gesicherten Ursprungswerte wieder her und löscht Aufgabe, Symbol und Zustandsdateien.

## Dateien und Protokolle
`%LOCALAPPDATA%\Spielmodus\` → `waechter.log`, `einrichten.log`, `starttypen.json`, `vorher.json`, `aus.flag`, `treiber-sicherung-*`.

## Hinweise
- Solange Steam läuft, bekommen auch Store-Apps (z. B. Codex, VS Code als Store-Paket) keine Updates.
- Braucht ein Spiel die Gaming Services beim Start, den Wächter per Doppelklick ausschalten.
- Treiber zurück: Geräte-Manager → Intel Wi-Fi 6 AX201 → Treiber → „Vorheriger Treiber“, oder
  `pnputil /add-driver "%LOCALAPPDATA%\Spielmodus\treiber-sicherung-23.170.0.1\*.inf" /install`.
- Nur Windows (Microsoft Store/Gaming Services gibt es auf macOS nicht).
