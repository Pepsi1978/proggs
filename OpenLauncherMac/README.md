# OpenLauncher (macOS)

Die macOS-Fassung des OpenLaunchers — **1:1 dieselbe Anwendung** wie `~/proggs/OpenLauncher`
(Windows, C#/WPF), nur in Swift/AppKit statt in WPF. Gleiche Oberfläche, gleiche Modelle, gleiche
Profile, gleiche Arbeitsmodi, gleiches Startverhalten.

## Bauen und starten

```bash
bash ~/proggs/OpenLauncherMac/build.sh
open ~/proggs/OpenLauncherMac/build/OpenLauncher.app
```

Gebaut wird per `swiftc` als `.app`-Bundle (kein Xcode-Projekt, kein SwiftPM — wie bei den anderen
macOS-Apps im Repo). `build.sh` setzt den Build-Zeitstempel selbst in die `Info.plist`, damit in der
Titelleiste nie eine von Hand getippte Uhrzeit steht.

## Tests

```bash
bash ~/proggs/OpenLauncherMac/tests/run-tests.sh
```

Prüft, dass die erzeugten Startskripte gültiges zsh sind und das eingebettete Python (LM-Studio-
Vorbereitung) übersetzbar ist — Gegenstück zu den PowerShell-Quellwächtern unter Windows.

## Was mit Windows geteilt wird

Diese Dateien liegen im Repo und werden von **beiden** Launchern gelesen und geschrieben:

| Datei | Inhalt |
|-------|--------|
| `OpenLauncher/models.json` | Modell-Liste, Reihenfolge, ausgeblendete Modelle |
| `OpenLauncher/model-defaults.json` | Gespeicherte Standards je Modell (Profil · Modus · Effort) |
| `OpenLauncher/Profiles/WorkModes/*.md` | Die vier Arbeitsmodus-Prompts (plattformneutral) |

Ein auf Windows gespeicherter Modell-Standard greift nach `git pull` also sofort auch auf dem Mac.

## Was getrennt ist — und warum

| Bereich | Windows | macOS |
|---------|---------|-------|
| Claude-Profile | `Profiles/ClaudeCode/` | `Profiles/ClaudeCodeMac/` |
| OpenCode-Profile | `Profiles/OpenCode/` | `Profiles/OpenCodeMac/` |

Die Profil-Ordner sind die `CLAUDE_CONFIG_DIR` der jeweiligen Sitzung. Die Windows-Fassung trägt
PowerShell-Hooks (`pwsh … .ps1`) und `C:\Users\barwa\…`-Pfade — beides funktioniert auf macOS nicht.
Die macOS-Profile enthalten dieselben Inhalte mit `bash … .sh`-Hooks und `/Users/frank/…`-Pfaden.

Der Inhalt (`rules/ agents/ commands/ skills/`, Profiltexte, Modus-Prompts) ist ansonsten wortgleich
übernommen, einschließlich der Verweise auf `~/SK`, `~/proggs/best-practices` und `~/proggs/bugs`.

## Terminal

Gestartet wird im **Standard-Terminal von macOS** (Terminal.app). Ob dabei ein neues Fenster oder
ein neuer Tab aufgeht, entscheidet die Systemeinstellung „Tabs bevorzugen beim Öffnen von
Dokumenten" — der Launcher überlässt das bewusst dem System.

Die rotierende Tab-Farbe aus der Windows-Fassung hat auf macOS kein direktes Gegenstück:
Terminal.app färbt nicht einzelne Tabs, sondern nimmt die Farben aus dem gewählten Profil. Der
Farbname bleibt aber sichtbar — er steht im Tab-Titel (`Claude-green-xhigh`, `OpenCode-blue-high`),
und bei Claude Code färbt zusätzlich `/color <name>` die Oberfläche selbst, genau wie unter Windows.

## Plattform-Abbildung im Detail

| Windows | macOS |
|---------|-------|
| Windows Terminal (`wt new-tab --tabColor`) | **Terminal.app** (`open -a Terminal <skript>`) |
| PowerShell-Startskript (`.ps1`) | zsh-Startskript (`.sh`) |
| `%APPDATA%\OpenLauncher` | `~/Library/Application Support/OpenLauncher` |
| `%LOCALAPPDATA%\OpenLauncher\sessions` | `~/Library/Application Support/OpenLauncher/sessions` |
| `mklink /J` (Skills im Minimal-Profil) | Symlink (`ln -s`) |
| `ProcessPriorityClass.AboveNormal` | `renice -n -5` |
| `lms.exe` | `~/.lmstudio/bin/lms` |
| Einzelinstanz per Mutex | Einzelinstanz per `NSRunningApplication` |
| Registry-PATH nachladen | `path_helper` + `.zprofile`/`.zshrc` |

## Eine bewusste Abweichung in der Oberfläche

Unter Windows ersetzt die App die Fensterknöpfe durch eigene (`WindowStyle="None"`), weil die
Windows-Standardleiste nicht ins Design passt. Auf macOS sind die **Ampel-Knöpfe links der
Systemstandard** — sie bleiben erhalten und werden nicht nachgebaut. Zwei Sätze Fensterknöpfe wären
verwirrend und würden Fensterverwaltung, Vollbild und Mission Control stören.

Alle Funktionen bleiben erhalten: Minimieren, Zoomen und Schließen über die Ampel, der
Design-Umschalter sitzt an derselben Stelle rechts wie unter Windows.

## Aufbau

```
OpenLauncherMac/
  build.sh                     Build als .app (setzt den Build-Zeitstempel)
  tests/run-tests.sh           Startskript-Syntaxtest
  OpenLauncher/
    main.swift                 Einstiegspunkt (von Hand verdrahtet, ohne NIB)
    AppDelegate.swift          Composition-Root, Einzelinstanz, Menüleiste
    Models/Models.swift        Modelle, Gruppen, Provider, Thinking-Stufen, Modell-Standards
    Services/                  Registry, Profile, OpenRouter, LM Studio, Terminal-Start, Logging
    ViewModels/                Gesamter Zustand (Port von MainViewModel.cs)
    Views/                     Fenster, Modell-Liste, Provider-Tabelle, Profil-/Thinking-Bereich, Dialoge
```

## Voraussetzungen

- macOS 13 oder neuer, Apple Silicon
- Xcode Command Line Tools (`swiftc`)
- `claude` und/oder `opencode` im PATH
- Optional: LM Studio für lokale Modelle
