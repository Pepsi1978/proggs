# Jarvis-MCP fehlt in OpenCode

Stand: 22.10.2026, 12:45 Uhr.

## Symptom und Ursache

`jarvis_tagesauswertung_lesen` und die übrigen Jarvis-Werkzeuge fehlen vollständig.
Die aktive globale `~/.config/opencode/opencode.jsonc` und die Installer-Vorlage
`opencode-setup/opencode.jsonc` hatten keinen `mcp.jarvis`-Eintrag. Die Startregel
zum Abruf einer Tagesauswertung registriert selbst keinen Server. Auf dem betroffenen
Rechner fehlte außerdem `~/SK/Jarvis/plugin-url.txt`.

## Behebung

- Den Server in beiden Konfigurationen als `remote` mit `enabled: true`, `oauth: false`
  und `timeout: 120000` eintragen.
- `url` ausschließlich als `{file:~/SK/Jarvis/plugin-url.txt}` referenzieren. Die aktuelle
  Adresse aus dem vorhandenen Relay-Zustand sicher in SK übernehmen; nie ausgeben oder committen.
- OpenCode vollständig neu starten, da laufende Sitzungen ihre geladene Konfiguration behalten.

## Verwandte Fehlerquellen und Vorbeugung

- **Gleiche Klasse:** Aktive Konfiguration und Installer-Vorlage gemeinsam ergänzen, damit
  eine Neuinstallation den Eintrag nicht wieder entfernt (Poka-Yoke: Vorlage).
- **Gleiche Komponente:** Jarvis verwendet den öffentlichen HTTPS-Relay und einen zustandslosen
  MCP-Server. Der STDIO-Workaround des Second-Brain-Servers ist dafür nicht vorausgesetzt.
- **Gleiche Abhängigkeit:** Der OpenLauncher-Sitzungssnapshot überschreibt nur Chrome und Elgato;
  Jarvis wird aus der globalen Konfiguration geerbt. Claude-/Codex-Konfigurationen sind unabhängig.
- **Zweite Absicherung:** Die README nennt die notwendige SK-Datei, den Adresswechsel und den
  erforderlichen Neustart ausdrücklich. Die Datei muss vor einem frischen Setup vorhanden sein.
- Keine anderen MCP-Einträge oder Modellkonfigurationen ersetzen. Der Fix ergänzt ausschließlich
  Jarvis; laufende Sitzungen werden nicht beendet.

## Erkennung

Fehlender Servereintrag ist von Verbindungsfehlern zu unterscheiden: zunächst `mcp.jarvis`
in der aktiven globalen Konfiguration und anschließend die SK-Datei prüfen. Bei Diagnoseausgaben
den geheimen Abschnitt `/j/<Geheimnis>/mcp` immer redigieren.

## Ergebnis auf dem Windows-Rechner

`opencode mcp list` meldet nach der Konfigurationsänderung `jarvis connected`.
Chrome und Elgato verbinden ebenfalls. Der unabhängige Second-Brain-Eintrag meldet
`Connection closed`; dieser Befund gehört nicht zur Jarvis-Ursache.
