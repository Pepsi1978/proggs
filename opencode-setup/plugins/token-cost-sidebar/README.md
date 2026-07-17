# Token-Cost-Sidebar für OpenCode

> **Installationsregel für Agenten:** Diese Datei vor jeder Installation oder Aktualisierung
> vollständig lesen. Nicht nur diesen Ordner kopieren, weil Arbeitsmodus-Auswahl und
> Prompt-Injektion aus mehreren gemeinsam benötigten Bestandteilen bestehen.

Stand: v1.5.3 - 17.07.2026, 14:28 Uhr

## Funktionen

- Klickbare Auswahl zwischen Schnellmodus, Normalmodus und Gründlichkeitsmodus.
- Klickbare Effort-Auswahl für Low, Medium, High und X-High, soweit vom aktuellen Modell unterstützt.
- Anzeige des live ausgewählten Modells direkt oberhalb der Effort-Auswahl.
- Orange, fette und unterstrichene Sidebar-Überschriften für Session, Modell, Context, Theme, MCP und LSP.
- Effort-Klicks ändern unmittelbar OpenCodes aktive Model-Variante für den nächsten Modellaufruf.
- Sitzungsbezogene Speicherung des gewählten Arbeitsmodus.
- Injektion der passenden Arbeitsanweisung in jeden neuen Modellaufruf.
- Anzeige von Modell, Tokenverbrauch, Preisen und geschätzten Sitzungskosten.
- Linksbündige, orange und fette Theme-Auswahl mit direkt folgendem Theme-Namen sowie nebeneinanderliegender Dunkel-/Hell-Umschaltung; der aktive Modus ist fett.
- Das aktive `AGENTS.md`-Profil bleibt vollständig gültig und hat bei Widersprüchen Vorrang.

## Vollständiger Installationsumfang

Für eine funktionsfähige Installation werden immer alle folgenden Bestandteile benötigt:

| Bestandteil | Repo-Quelle | Installationsziel | Zweck |
|---|---|---|---|
| TUI-Plugin | `opencode-setup/plugins/token-cost-sidebar/` | `~/.config/opencode/plugins/token-cost-sidebar/` | Sidebar und Modusauswahl |
| System-Plugin | `opencode-setup/plugins/work-mode.js` | `~/.config/opencode/plugins/work-mode.js` | Prompt-Injektion pro Modellaufruf |
| TUI-Konfiguration | `opencode-setup/tui.json` | `~/.config/opencode/tui.json` | Registriert die Sidebar |
| Abhängigkeiten | `package.json` und Installer | `~/.config/opencode/node_modules/` | OpenCode-, OpenTUI- und Solid-Laufzeit |

Die maßgebliche Quelle der Modusbezeichnungen und Prompts ist
`dist/work-mode.ts`. Eine zweite Kopie der Prompttexte soll nicht gepflegt werden.

## Empfohlene Installation

Auf einem neuen Rechner zuerst das Repository nach `~/proggs` klonen oder aktualisieren.
Danach den plattformgerechten Setup-Installer ausführen; er installiert alle oben genannten
Bestandteile gemeinsam.

### Windows

```powershell
pwsh ~/proggs/opencode-setup/install.ps1
```

### macOS oder Linux

```bash
bash ~/proggs/opencode-setup/install.sh
```

Die Installer richten zusätzlich die übrige versionierte OpenCode-Umgebung ein. Soll nur dieses
Plugin installiert werden, müssen trotzdem alle vier Bestandteile aus der Tabelle kopiert und die
Abhängigkeiten aus `package.json` installiert werden.

## Pflicht nach Installation

OpenCode vollständig beenden und neu starten. Plugin- und Konfigurationsdateien werden beim Start
geladen und in einer bereits laufenden Session nicht automatisch aktualisiert.

## Verifikation

1. Unter dem Session-Titel stehen nach einer Leerzeile das aktive Modell und direkt darunter die unterstützten Effort-Stufen; ein Klick markiert die aktive Stufe.
2. Die rechte Sidebar zeigt darunter `Schnellmodus`, `Normalmodus` und `Gründlichkeitsmodus`.
3. Ein Effort-Klick ändert denselben validierten Model-Variant-State wie OpenCodes eigener Varianten-Picker.
4. Ein Modus-Klick markiert den gewählten Modus und die Auswahl bleibt in derselben Session erhalten.
5. Beim nächsten Modellaufruf beginnt die zusätzliche Systemanweisung mit
   `AKTIVER ARBEITSMODUS: <gewählter Modus>`.
6. Die Anweisung bestätigt, dass das aktive `AGENTS.md`-Profil vollständig und unverändert gilt.
7. Die Plugin-Version in `package.json` entspricht der Version dieser README.

Tests im Repo:

```bash
cd ~/proggs/opencode-setup/plugins/token-cost-sidebar
bun test
```

## Zustände und Fehlerverhalten

- Der Modus wird pro Session unter `~/.local/state/opencode/work-modes/` gespeichert.
- Neue Sessions starten standardmäßig im Schnellmodus.
- Kann der gespeicherte Modus nicht gelesen werden, verwendet das System-Plugin den Schnellmodus
  und schreibt den Fehler in das OpenCode-App-Log.
