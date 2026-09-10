# Status Line Claude Code

Version 0.1.0 – 10.09.2026 12:00 Uhr.

Gesicherte Statuszeile aus dem aktuell verwendeten OpenLauncher-Claude-Code-Profil, damit sie auf jedem anderen Windows-Rechner genauso aussieht. Gegenstück zu [Statusline-Codex](../Statusline-Codex/README.md).

| Datei | Zweck |
| --- | --- |
| [statusline.sh](statusline.sh) | Das Statuszeilen-Skript (Stand von `claude-code-setup/hooks/statusline.sh` am 10.09.2026) |
| [statusline-settings.json](statusline-settings.json) | Der `statusLine`-Block für `settings.json` |
| [install-statusline.ps1](install-statusline.ps1) | Richtet alles auf einem Windows-Rechner ein |
| `.gitattributes` | Hält das Bash-Skript bei Unix-Zeilenenden, sonst bricht es unter Windows ab |

## Anzeige

**Zeile 1**, von links nach rechts:

| Bereich | Anzeige |
| --- | --- |
| 🧠 ctx | Kontextverbrauch als Balken und Prozent. Ab 80 % blinkt der Bereich rot. |
| 🤖 Modell | Modellname, „(1M context)“ gekürzt zu „(1M)“ |
| ⚡ Effort | Aktueller Effort (LOW, MEDIUM, HIGH, XHIGH), farbig je Stufe |
| ⏱ 5h | 5-Stunden-Limit als Balken, Prozent und Restzeit bis zum Reset |
| slow ─●┃── fast | 5h-Tempo: Liegt der Punkt links vom Strich, ist Reserve da. Liegt er rechts, läuft das Limit vor Fensterende voll. |
| 📅 7d | Wochenlimit als Balken, Prozent und Restzeit (Tage und Stunden) |
| slow ─●┃── fast | 7d-Tempo, gleiche Logik |
| 🕐 | Uhrzeit |

**Zeile 2:** 📁 Arbeitsordner, 💰 Sitzungskosten, ✏️ geänderte Zeilen (+/−), ⏳ Sitzungsdauer, 🏷️ Claude-Code-Version.

Farben: grün unter 50 %, gelb 50–79 %, rot ab 80 %. Die Limits werden über alle parallel laufenden Sitzungen desselben Kontos zusammengeführt (höchster Wert gewinnt). Die Daten dazu liegen in `~/.claude/state/`.

## Auf einem anderen Windows-Rechner einrichten

**Voraussetzungen:** Claude Code, Git for Windows (Git Bash) und Windows Terminal oder ein anderes Terminal, das Emojis anzeigt. Fehlt `jq`, installiert das Skript es selbst per winget.

1. Repo holen bzw. aktualisieren (`git pull`).
2. PowerShell im Ordner `Statusline-ClaudeCode` öffnen und ausführen:

   ```powershell
   pwsh -NoProfile -ExecutionPolicy Bypass -File .\install-statusline.ps1
   ```

   Das Skript prüft Git Bash, installiert bei Bedarf `jq` und kopiert `statusline.sh` nach `~/.claude/statusline-claudecode.sh`. Dann trägt es den `statusLine`-Block in die `settings.json` ein, legt vorher eine Sicherungskopie an und lässt alle anderen Einstellungen stehen. Zum Schluss zeigt es die Statuszeile einmal mit Beispieldaten an.
3. Claude Code neu starten.

Ziel ist standardmäßig `CLAUDE_CONFIG_DIR` (falls gesetzt), sonst `~/.claude`. Ein anderer Konfigurationsordner lässt sich mit `-ConfigDir <Pfad>` angeben.

**Mit OpenLauncher:** Die Claude-Code-Profile im Repo (`OpenLauncher/Profiles/ClaudeCode/*/settings.json`) enthalten die Statuszeile bereits. Liegt das Repo unter `~/proggs`, reicht dort `jq`. Das Installationsskript ist für Claude Code ohne OpenLauncher gedacht. Starte es nicht aus einer OpenLauncher-Sitzung heraus, sonst schreibt es in die Profil-`settings.json` des Repos.

**macOS:** `statusline.sh` läuft auch unter macOS (bash 3.2). Das Installationsskript ist nur für Windows. Von Hand: `brew install jq`, `statusline.sh` nach `~/.claude/statusline-claudecode.sh` kopieren und den Block aus `statusline-settings.json` in `~/.claude/settings.json` übernehmen.

## Auftrag zum schnellen Nachrüsten

> Richte meine Claude-Code-Statuszeile aus dem Repo-Ordner `Statusline-ClaudeCode` ein. Lies dort die README und führe unter Windows `install-statusline.ps1` aus, auf dem Mac die Handschritte. Ermittle vorher den tatsächlich aktiven Konfigurationsordner (`CLAUDE_CONFIG_DIR` bzw. `~/.claude`). Sichere die `settings.json` und erhalte alle anderen Einstellungen. Melde ausdrücklich, falls Git Bash oder jq fehlen und nicht installiert werden konnten.

## Aktualisieren

Die Quelle der laufenden Statuszeile ist `claude-code-setup/hooks/statusline.sh`. Wird sie geändert, die neue Fassung hierher kopieren, die Version in `package.json` und hier oben erhöhen und auf den anderen Rechnern `install-statusline.ps1` erneut ausführen.

## Umfang der Sicherung

Gespeichert werden nur das Statuszeilen-Skript und der `statusLine`-Block. Die vollständige persönliche `settings.json`, Zugangsdaten und die Limit-Zwischenstände aus `~/.claude/state/` gehören nicht dazu.
