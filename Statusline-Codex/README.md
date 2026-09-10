# Status Line Codex

Version 0.1.4 – 10.09.2026 11:56 Uhr.

Gesicherte Statuszeilen-Einstellungen aus dem aktuell verwendeten OpenLauncher-Codex-Profil. Die maßgebliche Vorlage ist **[status-line.toml](status-line.toml)**. Dieses Verzeichnis ersetzt die frühere selbst gebaute Kostenanzeige samt Rust-Patch, Preisdateien und Build-/Update-Skripten.

## Anzeige und Reihenfolge

| Einstellung | Anzeige |
| --- | --- |
| `model-with-reasoning` | Modell mit Reasoning-Effort |
| `current-dir` | Aktuelles Verzeichnis |
| `permissions` | Berechtigungsmodus, beispielsweise Full Access |
| `context-used` | Kontextverbrauch, beispielsweise Context 0% Used |
| `weekly-limit` | Wochenkontingent, beispielsweise Weekly 88% Left |
| `run-state` | Laufzustand, beispielsweise Ready |
| `used-tokens` | Verbrauchte Tokens; im Quellprofil zusätzlich konfiguriert |
| `codex-version` | Version der laufenden Codex CLI |
| `estimated-thread-cost` | Kostenschätzung; im Quellprofil zusätzlich konfiguriert |
| `fast-mode` | Fast Off oder Fast On |

Die Vorlage übernimmt alle zehn Einträge unverändert aus der aktiven Konfiguration. Welche Felder tatsächlich erscheinen, hängt von der CLI-Version, verfügbaren Daten und dem Terminal ab. Die Prozentwerte, das Modell, der Effort, das Verzeichnis, Ready und Fast On/Off sind dynamisch und werden nicht als feste Texte gespeichert. `run-state` bezeichnet den Laufzustand, keinen dauerhaft eingestellten Arbeitsmodus. Die Anzeige `permissions` erteilt keine Berechtigungen; `fast-mode` schaltet Fast nicht ein oder aus.

## Auf einem anderen Rechner einrichten

1. Das tatsächlich von der Ziel-CLI verwendete Konfigurationsverzeichnis ermitteln: gesetztes `CODEX_HOME` verwenden, sonst `~/.codex`. Bei OpenLauncher dessen Codex-Profil verwenden. Nicht versehentlich nur das Standardprofil bearbeiten.
2. Die dortige `config.toml` vor der Änderung sichern. Falls noch keine vorhanden ist, das Verzeichnis und die Datei anlegen.
3. Den Inhalt aus `status-line.toml` übernehmen: In einer vorhandenen `[tui]`-Tabelle ausschließlich `status_line` ersetzen beziehungsweise ergänzen. Fehlt `[tui]`, die Tabelle hinzufügen. Keine zweite `[tui]`-Tabelle anlegen und keine gesamte bestehende Konfiguration überschreiben. Andere TUI-Einstellungen, Modelle, Berechtigungen, MCP-Server und Zugangsdaten erhalten.
4. Codex CLI vollständig neu starten. Auf der Zielinstallation lässt sich über `/statusline` feststellen, welche Felder diese CLI anbietet. Unterstützt sie nicht alle gespeicherten Schlüssel, ist für dieselbe Anzeige eine dazu passende CLI-Version erforderlich. Die Vorlage allein installiert keine zusätzlichen CLI-Funktionen und keinen alten Kosten-Patch.

Die TOML-Einstellungen sind unabhängig vom Betriebssystem; dieselbe Vorlage kann unter Windows, macOS und Linux in das jeweils aktive Profil übernommen werden. Auf dem Quellrechner ist diese Konfiguration bereits aktiv; dort ist keine erneute Installation nötig.

## Auftrag zum schnellen Nachrüsten

> Richte meine Codex-Statuszeile aus dem Repo-Ordner `Statusline-Codex` (Status Line Codex) ein. Lies dort die README und übernimm die vollständige Reihenfolge aus `status-line.toml` in `tui.status_line` der tatsächlich aktiven Codex-Konfiguration. Ermittle dazu zuerst `CODEX_HOME` beziehungsweise das vom Launcher verwendete Profil. Sichere die Konfigurationsdatei und erhalte alle anderen Einstellungen. Stelle fest, ob die Ziel-CLI die Einträge unterstützt, und melde fehlende Unterstützung ausdrücklich. Die angezeigten Live-Werte bleiben dynamisch. Starte keine alten Kostenanzeige-Builds und ändere weder Berechtigungen noch Fast-Modus nur wegen ihrer Anzeige.

## Umfang der Sicherung

Gespeichert wird ausschließlich die Statuszeilen-Konfiguration. Die vollständige persönliche `config.toml`, Zugangsdaten, absolute Benutzerpfade und CLI-Binärdateien gehören nicht zu dieser Sicherung. Die alte Kostenanzeige-Implementierung bleibt bei Bedarf über die Git-Historie auffindbar; im aktuellen Ordner wurde sie entfernt. Bereits lokal installierte CLI-Binärdateien werden durch diese Repository-Änderung nicht verändert.
