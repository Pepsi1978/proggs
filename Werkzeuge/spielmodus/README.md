# Spielmodus (Windows)

Version 1.0.0 · Stand 17.11.2026, 18:20 Uhr

Hält beim Online-Spielen über die schmale 5G-Leitung alle Microsoft-Store- und Xbox-Downloads fern.
Hintergrund: Gaming Services (Xbox-App) luden mit 12 parallelen Verbindungen Shader und trieben den
Ping von 30 ms auf über 1500 ms (Almanach `bugs/gaming/netzwerk-latenz-5g-router.md`, B1/B2).

## Was es tut
- **Wächter** (`waechter.ps1`, geplante Aufgabe „Spielmodus-Waechter“, bei Anmeldung, höchste Rechte, unsichtbar):
  Läuft `steam`, `cod` oder ein CoD-Bootstrapper, werden die Dienste `GamingServices`, `GamingServicesNet`
  und `InstallService` (Store-Installationen) gestoppt und gesperrt, `WinStore.App` und die Xbox-App beendet.
  Startet etwas davon neu, wird es sofort wieder beendet. Ist kein Spiel mehr offen, stellt er die
  ursprünglichen Starttypen (`starttypen.json`) wieder her.
  In den ersten 90 Sekunden nach CoD-Start protokolliert er alle neuen Fenster (Diagnose des Absturzfensters).
- **Desktop „Spielmodus an-aus“** (`umschalten.ps1`): Doppelklick schaltet den Wächter aus/an (Datei `aus.flag`).
- **Desktop „Spielmodus zurueck“** (`zurueck.ps1`): entfernt alles wieder (UAC), inkl. Store-Richtlinie und Game-Bar-Aufnahme.
- **Einrichtung** (`einrichten.ps1 -TreiberZip <zip>`, einmal als Admin): Store-Automatikupdates aus
  (`HKLM\SOFTWARE\Policies\Microsoft\WindowsStore\AutoDownload=2`, Wirkung unter Windows 11 Home unbelegt),
  Aufgabe + Verknüpfungen anlegen, Intel-WLAN-Treiber aktualisieren (alter Treiber per `pnputil /export-driver`
  gesichert, Installation nur bei gültig signierten Katalogen), danach „Bevorzugtes Band“ = 5 GHz (Wert 2)
  und „Roaming-Dynamik“ = am niedrigsten (Wert 0).

## Dateien und Protokolle
`%LOCALAPPDATA%\Spielmodus\` → `waechter.log`, `einrichten.log`, `starttypen.json`, `aus.flag`, `treiber-sicherung-*`.

## Hinweise
- Solange Steam läuft, bekommen auch Store-Apps (z. B. Codex, VS Code als Store-Paket) keine Updates.
- Braucht ein Spiel die Gaming Services beim Start, den Wächter per Doppelklick ausschalten.
- Treiber zurück: Geräte-Manager → Intel Wi-Fi 6 AX201 → Treiber → „Vorheriger Treiber“, oder
  `pnputil /add-driver "%LOCALAPPDATA%\Spielmodus\treiber-sicherung-23.170.0.1\*.inf" /install`.
- Nur Windows (Microsoft Store/Gaming Services gibt es auf macOS nicht).
