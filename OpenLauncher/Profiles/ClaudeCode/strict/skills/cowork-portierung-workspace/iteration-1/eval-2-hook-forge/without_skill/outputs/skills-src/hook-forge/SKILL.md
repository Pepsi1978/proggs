---
name: hook-forge
description: "Erstellt resiliente Claude-Code-Hooks (.ps1 + .sh) nach den 3 Direktiven: exit 0 Pflicht, try/catch, Hook-Log, beide Plattformen. Trigger: Hook erstellen, neuer Hook, Hook bauen, Hook fixen."
---

# Hook-Forge (Cowork-Fassung) — resiliente Hook-Erstellung nach den 3 Direktiven

Diese Cowork-Fassung stellt sicher, dass JEDER neue oder grundlegend umgeschriebene Claude-Code-Hook
resilient ist: nie Hook-Fehler (exit 0 Pflicht), auf BEIDEN Plattformen (.ps1 + .sh), den 3 Direktiven
folgend (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing). Läuft in der **Claude-Cowork-Desktop-App**.

---

## 0. ZUERST LESEN — Ablage-Ort & Hook-Dateien (Cowork)

Hooks leben außerhalb des gemounteten Arbeitsordners im echten Claude-Verzeichnis. **Schreibe die
Hook-Dateien ins reale Hook-Verzeichnis und spiegle sie RELATIV ins Setup-Repo des Arbeitsordners** —
NICHT in einen fest verdrahteten `~/proggs`-Pfad. Struktur:

```
~/.claude/hooks/<name>.ps1     ← echte Windows-Version (greift wirklich)
~/.claude/hooks/<name>.sh      ← echte macOS/Linux-Version (greift wirklich)
claude-code-setup/hooks/<name>.ps1   ← Spiegel im Arbeitsordner (relativ)
claude-code-setup/hooks/<name>.sh    ← Spiegel im Arbeitsordner (relativ)
```

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt `claude-code-setup/hooks/` → ERST anlegen
(Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen, weil ein
Ordner fehlt. Nennt der Benutzer einen anderen Basis-Ordner, dort hinein (gleiche Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM
  Schreiben einer Hook-Datei das Dateiende prüfen (`tail -1`, `wc -l`) — gerade das abschließende
  `exit 0` MUSS wirklich in der Datei stehen, sonst gibt der Hook non-zero zurück.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse überleben
  den Aufruf-Wechsel nicht. Schreiben + Syntax-Check + Git müssen in jeweils EINEM Aufruf laufen.
- **Git NIEMALS nackt** aus der VM (Locks, Mount-Fallen). Abschluss IMMER über
  `bash ~/proggs/cowork-git.sh push-files …` (siehe „Sichern").
- **CLI-Hooks/Guards fehlen in Cowork:** `bug-almanac-guard`, `subagent-context` etc. gibt es hier
  nicht — verlasse dich nicht auf sie, befolge die Regeln dieses Skills direkt.

---

## Pflicht-Ablauf (Checkliste)

### Phase 1: Design

1. **Event bestimmen**: Welches Hook-Event? (SessionStart, UserPromptSubmit, PreToolUse, PostToolUse,
   Stop, SubagentStop, SessionEnd, etc.)
2. **Typ bestimmen**: `command` (Skript ausführen) oder `prompt` (Text injizieren)?
   - ACHTUNG: `prompt`-Typ funktioniert NICHT bei SessionStart und SessionEnd! (kein ToolUseContext)
   - Bei SessionStart IMMER `command` verwenden.
3. **Timeout bestimmen**: Standard 60000ms. SessionStart-Hooks: max 30000ms (blockieren den Start).
4. **Name wählen**: Beschreibender Kebab-Case-Name (z.B. `disk-guard`, `intent-anker`, `auto-format`).

### Phase 2: Template anwenden

Lies die passenden Templates:
- **PowerShell**: `references/ps1-template.md` — für Windows
- **Bash**: `references/sh-template.md` — für macOS/Linux

BEIDE Varianten MÜSSEN erstellt werden. Keine Ausnahme.

Jeder Hook MUSS diese Elemente enthalten:

| Element | Warum | Direktive |
|---------|-------|-----------|
| `$ErrorActionPreference = "Stop"` (PS1) / `set -euo pipefail` (SH) | Fehler früh erkennen | #3 Resilient |
| `try { ... } catch { ... }` (PS1) / `trap ... ERR` (SH) | Fehler abfangen statt propagieren | #3 Resilient |
| `exit 0` am ENDE (IMMER) | Hook darf NIEMALS non-zero returnen | #3 Resilient |
| `exit 0` in JEDEM catch/ERR-trap-Block | Auch bei Fehlern graceful beenden (mit `set -e` sonst non-zero) | #3 Resilient |
| `. "$PSScriptRoot/hook-log.ps1"` / `. "$SCRIPT_DIR/hook-log.sh"` | Zentrale Fehler-Protokollierung | #2 Selbstbeobachtung |
| Hook-Log bei JEDEM Fehler | Fehler werden ins Log geschrieben, nicht verschluckt | #2 Selbstbeobachtung |
| Whiteboard-Eintrag bei SCHWEREN Fehlern | Sichtbar für /self-improve | #1 Superintelligenz |

### Phase 3: Implementierung

1. **PowerShell-Version erstellen**: `~/.claude/hooks/<name>.ps1`
2. **Bash-Version erstellen**: Die SH-Version muss FUNKTIONAL IDENTISCH sein.
   - Gleiche Logik, gleiche Checks, gleiche Log-Messages
   - Plattform-spezifische Unterschiede berücksichtigen (Pfade, Tools, Syntax)
3. **stdin per `python3` parsen, nicht per `jq`** (PreToolUse/PostToolUse) — jq kann auf frischen
   Systemen fehlen und versagt dann stumm.
4. **Testen**: Beide Versionen syntax-prüfen (in EINEM Shell-Aufruf, ~45s-Limit beachten):
   - PS1: `pwsh -NoProfile -Command "& { $null = [System.Management.Automation.Language.Parser]::ParseFile('PFAD', [ref]$null, [ref]$null) }"`
   - SH: `bash -n PFAD`
   - Nach dem Schreiben Dateiende prüfen (`tail -1`) — das `exit 0` MUSS da sein (Mount-Falle).

### Phase 4: Integration

1. **Setup-Repo spiegeln**: Beide Versionen relativ nach `claude-code-setup/hooks/` im Arbeitsordner kopieren.
2. **settings.json registrieren** (wenn nötig):
   - Hook in `~/.claude/settings.json` unter dem richtigen Event eintragen
   - Format: `{"hooks": {"[Event]": [{"type": "command", "command": "pwsh -NoProfile -File ..."}]}}`
   - Auch die macOS-Version in `claude-code-setup/settings.json` eintragen.
   - Und `settings-reference.json` aktualisieren (3-Dateien-Regel)!
3. **Whiteboard-Eintrag**: Neuen Hook unter „Systemzustand" im Whiteboard erwähnen (Hook-Zähler aktualisieren).

### Phase 5: Verifikation (PFLICHT — kein Hook ohne diese Prüfung)

- [ ] PS1-Version hat `exit 0` am Ende?
- [ ] SH-Version hat `exit 0` am Ende UND im ERR-trap?
- [ ] PS1-Version hat `try/catch` und `$ErrorActionPreference = "Stop"`?
- [ ] SH-Version hat `set -euo pipefail` und `trap ... ERR`?
- [ ] Kein `exit 1` in SessionStart-Hooks?
- [ ] Hook-Log wird bei Fehlern aufgerufen?
- [ ] Beide Versionen syntax-geprüft?
- [ ] Dateiende jeder Datei geprüft (Mount-Truncation, `tail -1`)?
- [ ] Ins Setup-Repo (relativ) gespiegelt?
- [ ] In settings.json registriert (wenn nötig)?
- [ ] 3-Dateien-Regel für Settings eingehalten?

## Verbotene Muster

| Muster | Warum verboten | Stattdessen |
|--------|---------------|-------------|
| `exit 1` in SessionStart | Erzeugt sichtbaren Hook-Fehler | `exit 0` + Fehler ins Whiteboard loggen |
| Kein `exit 0` am Ende | PowerShell gibt Exit-Code des letzten Befehls zurück | Explizites `exit 0` am Skript-Ende |
| Kein `exit 0` im SH-ERR-trap | `set -e` beendet bei Fehler non-zero, `exit 0` am Ende wird nie erreicht | `exit 0` IN den ERR-trap setzen |
| `type: "prompt"` bei SessionStart | Kein ToolUseContext verfügbar, crasht | `type: "command"` verwenden |
| Nur .ps1 ODER nur .sh | Andere Plattform hat keinen Hook | IMMER beide erstellen |
| stdin per `jq` parsen | jq fehlt oft, versagt stumm | stdin per `python3` parsen |
| Fehler still verschlucken | Verstößt gegen Direktive #2 | Hook-Log + ggf. Whiteboard-Eintrag |
| Endlosschleife / langer Timeout | Blockiert Session-Start oder Tool-Nutzung | Timeout setzen, async ausführen |
| Hardcoded absolute Pfade | Bricht auf anderer Plattform | `$PSScriptRoot`, `$HOME`, `$SCRIPT_DIR` |

## Spezialfall: Async-Hooks

Hooks die lange dauern (Reindexierung, Downloads, Builds) MÜSSEN async sein:
- PS1: `Start-Process pwsh -ArgumentList "-NoProfile", "-File", $scriptPath -WindowStyle Hidden`
- SH: `nohup bash "$script" &>/dev/null &` + `disown`

## Sichern (Cowork-Git)

Git NIEMALS nackt aus Cowork — immer über das Wrapper-Skript:

```bash
bash ~/proggs/cowork-git.sh setup
# auf "Push-Zugang OK" warten, dann nur die eigenen relativen Pfade nennen:
bash ~/proggs/cowork-git.sh push-files "#NNN - new hook <name> (.ps1 + .sh)" claude-code-setup/hooks/<name>.ps1 claude-code-setup/hooks/<name>.sh
```

Ist kein Git-Repo verbunden → nur speichern und dem Benutzer den Ablage-Pfad nennen.

## Was NIEMALS passieren darf

- Einen Hook nur für eine Plattform erstellen (.ps1 ODER .sh) — IMMER beide.
- Kein `exit 0` am Skript-Ende oder im SH-ERR-trap — der Hook würde non-zero zurückgeben.
- `exit 1` in einem SessionStart-Hook — blockiert den Session-Start sichtbar.
- `type: "prompt"` bei SessionStart/SessionEnd — kein ToolUseContext, crasht.
- Fehler still verschlucken statt sie ins Hook-Log zu schreiben (Direktive #2).
- Nacktes `git commit`/`git push` aus Cowork — immer `cowork-git.sh`.
- Eine Hook-Datei schreiben, ohne das Dateiende zu prüfen (Mount-Truncation schneidet das `exit 0` ab).
- Git/Schreiben über mehrere Aufrufe verteilen (~45s-Limit) statt in EINEM Aufruf.

## Referenzen

- `references/ps1-template.md` — vollständiges PowerShell-Hook-Template.
- `references/sh-template.md` — vollständiges Bash-Hook-Template.
- Cowork-Regeln: `bugs/claude-tooling/cowork.md`, `bugs/claude-tooling/cowork-git-push.md` (im Arbeitsordner).
