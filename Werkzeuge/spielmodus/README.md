# Spielmodus (Windows)

Version 1.0.2 · Stand 17.11.2026, 18:21 Uhr

Hält beim Online-Spielen über die schmale 5G-Leitung alle Microsoft-Store- und Xbox-Downloads fern.
Hintergrund: Gaming Services (Xbox-App) luden mit 12 parallelen Verbindungen Shader und trieben den
Ping von 30 ms auf über 1500 ms (Almanach `bugs/gaming/netzwerk-latenz-5g-router.md`, B1/B2).

## Was es tut
- **Wächter** (`waechter.ps1`, geplante Aufgabe „Spielmodus-Waechter“, bei Anmeldung, höchste Rechte, unsichtbar):
  Läuft `steam`, `cod` oder ein CoD-Bootstrapper, werden die Dienste `GamingServices`, `GamingServicesNet`
  und `InstallService` (Store-Installationen) gestoppt (Starttyp sperrbar nur bei `InstallService`, die
  Gaming-Dienste sind geschützte Paket-Dienste und werden stattdessen bei jedem Neustart sofort wieder beendet), `WinStore.App` und die Xbox-App beendet.
  Startet etwas davon neu, wird es sofort wieder beendet. Ist kein Spiel mehr offen, stellt er die
  ursprünglichen Starttypen (`starttypen.json`) wieder her.
  In den ersten 90 Sekunden nach CoD-Start protokolliert er alle neuen Fenster (Diagnose des Absturzfensters).
- **Desktop „Spielmodus an-aus“** (`umschalten.ps1`): Doppelklick schaltet den Wächter aus/an (Datei `aus.flag`)
  und meldet per Windows-Mitteilung, ob der Spielmodus jetzt AN oder AUS ist (`meldung.ps1`).
- **Steam-Start:** Der Wächter meldet beim Start von Steam per Mitteilung, ob der Spielmodus AN oder AUS ist.
- **Entfernen** nur über die CLI: `zurueck.ps1` (bewusst kein Desktop-Symbol), entfernt Aufgabe, Store-Richtlinie,
  Game-Bar-Sperre und das Symbol.
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
