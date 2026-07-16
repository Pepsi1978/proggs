---
name: hook-forge
description: "Erstellt resiliente Claude-Code-Hooks nach den 3 Direktiven (exit 0, ps1+sh, Logging) und legt sie im Arbeitsordner ab. Trigger: Hook erstellen, neuer Hook, Hook bauen, Hook fixen."
---

# Hook-Forge (Cowork-Fassung) — resiliente Hook-Erstellung nach den 3 Direktiven

Dieser Skill stellt sicher, dass jeder neue oder grundlegend umgeschriebene Claude-Code-Hook
resilient ist: niemals einen Hook-Fehler erzeugt (`exit 0` Pflicht), auf BEIDEN Plattformen
funktioniert (`.ps1` + `.sh`) und den 3 Hauptdirektiven folgt (Superintelligenz, Selbstbeobachtung,
Resilient Bugfixing). Laeuft in der Claude-Cowork-Desktop-App.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

Cowork mountet einen Arbeitsordner (ueblicherweise `proggs`), aber NICHT unter dem festen Pfad
`~/proggs`. Schreibe alle Ergebnisse **relativ** zum verbundenen Arbeitsordner. Die erzeugten
Hook-Dateien gehoeren in den Hook-Ordner des Arbeitsordners (das Cowork-Gegenstueck zu `~/.claude/hooks/`):

| Was | Pfad (relativ zum Arbeitsordner) |
|-----|----------------------------------|
| PowerShell-Hook | `claude-code-setup/hooks/[name].ps1` |
| Bash-Hook | `claude-code-setup/hooks/[name].sh` |

Beide Varianten sind Pflicht — keine Ausnahme. Ordner anlegen ist erlaubt und gewollt
(`mkdir -p claude-code-setup/hooks`, falls Shell verfuegbar) — nie abbrechen, weil ein Ordner fehlt.

> Hinweis: In Cowork existieren die CLI-Hooks/Guards (`bug-almanac-guard`, `subagent-context`,
> `config-guard` …) NICHT. Die Registrierung in `settings.json` und die 3-Dateien-Settings-Regel
> sind ein CLI-Schritt — in Cowork wird der Hook nur als Datei abgelegt; ein spaeterer CLI-Sync
> uebernimmt Registrierung und Verteilung. Im Cowork-Lauf also: Datei(en) sauber schreiben und
> sichern, KEINE `settings.json` in Cowork anfassen.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

- **Mount-Schreibfalle:** Die Bruecke schneidet Dateienden manchmal ab. Nach JEDEM Schreiben das
  Dateiende pruefen (`tail -1`, `wc -l`) — besonders kritisch hier, weil ein abgeschnittenes
  finales `exit 0` genau den Hook-Fehler erzeugt, den dieser Skill verhindern soll.
- **~45s-Shell-Limit:** Jeder Schreib-/Git-Schritt muss in EINEM Aufruf durchlaufen;
  Hintergrundprozesse ueberleben den Aufruf-Wechsel nicht. Researcher/Agenten sind unkritisch.
- **Git NIEMALS nackt** aus der Cowork-VM (Locks, Mount-Fallen) → Abschluss immer ueber
  `bash ~/proggs/cowork-git.sh` (siehe „Sichern").
- Nachschlagen bei Unsicherheit: `bugs/claude-tooling/cowork.md` (§6 Skills) und
  `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

---

## Pflicht-Ablauf (Checkliste)

### Phase 1: Design

1. **Event bestimmen**: Welches Hook-Event? (SessionStart, UserPromptSubmit, PreToolUse, PostToolUse, Stop, SubagentStop, SessionEnd, etc.)
2. **Typ bestimmen**: `command` (Skript ausfuehren) oder `prompt` (Text injizieren)?
   - ACHTUNG: `prompt`-Typ funktioniert NICHT bei SessionStart und SessionEnd! (kein ToolUseContext)
   - Bei SessionStart IMMER `command` verwenden.
3. **Timeout bestimmen**: Standard 60000ms. SessionStart-Hooks: max 30000ms (blockieren den Start).
4. **Name waehlen**: Beschreibender Kebab-Case-Name (z.B. `disk-guard`, `intent-anker`, `auto-format`).

### Phase 2: Template anwenden

Lies die passenden Templates:
- **PowerShell**: `references/ps1-template.md` — fuer Windows
- **Bash**: `references/sh-template.md` — fuer macOS/Linux

BEIDE Varianten MUESSEN erstellt werden. Keine Ausnahme.

Jeder Hook MUSS diese Elemente enthalten:

| Element | Warum | Direktive |
|---------|-------|-----------|
| `$ErrorActionPreference = "Stop"` (PS1) / `set -e` (SH) | Fehler frueh erkennen | #3 Resilient |
| `try { ... } catch { ... }` (PS1) / `trap` (SH) | Fehler abfangen statt propagieren | #3 Resilient |
| `exit 0` am ENDE (IMMER) | Hook darf NIEMALS non-zero returnen | #3 Resilient |
| `exit 0` in JEDEM catch/trap-Block | Auch bei Fehlern graceful beenden | #3 Resilient |
| `. "$PSScriptRoot/hook-log.ps1"` / `. "$(dirname "$0")/hook-log.sh"` | Zentrale Fehler-Protokollierung | #2 Selbstbeobachtung |
| Hook-Log bei JEDEM Fehler | Fehler werden ins Log geschrieben, nicht verschluckt | #2 Selbstbeobachtung |
| Whiteboard-Eintrag bei SCHWEREN Fehlern | Sichtbar fuer /self-improve | #1 Superintelligenz |

### Phase 3: Implementierung

1. **PowerShell-Version erstellen**: `claude-code-setup/hooks/[name].ps1`
2. **Bash-Version erstellen**: Die SH-Version muss FUNKTIONAL IDENTISCH sein.
   - Gleiche Logik, gleiche Checks, gleiche Log-Messages
   - Plattform-spezifische Unterschiede beruecksichtigen (Pfade, Tools, Syntax)
3. **Syntax pruefen** (falls Shell verfuegbar): beide Versionen syntax-pruefen:
   - PS1: `pwsh -NoProfile -Command "& { $null = [System.Management.Automation.Language.Parser]::ParseFile('PFAD', [ref]$null, [ref]$null) }"`
   - SH: `bash -n PFAD`

### Phase 4: Integration

In Cowork wird der Hook NUR als Datei im Arbeitsordner abgelegt (siehe §0). Die CLI-spezifischen
Schritte — Registrierung in `~/.claude/settings.json`, Spiegelung in `claude-code-setup/settings.json`
+ `settings-reference.json` (3-Dateien-Regel), Whiteboard-Hook-Zaehler — uebernimmt ein spaeterer
CLI-Sync. Im Cowork-Lauf KEINE `settings.json` anfassen; nur die beiden Hook-Dateien sicher schreiben.

### Phase 5: Verifikation (PFLICHT — kein Hook ohne diese Pruefung)

- [ ] PS1-Version hat `exit 0` am Ende?
- [ ] SH-Version hat `exit 0` am Ende?
- [ ] PS1-Version hat `try/catch` oder `$ErrorActionPreference`?
- [ ] SH-Version hat `set -e` oder `trap` (mit `exit 0` im trap)?
- [ ] Kein `exit 1` in SessionStart-Hooks?
- [ ] Hook-Log wird bei Fehlern aufgerufen?
- [ ] Beide Versionen syntax-geprueft (falls Shell verfuegbar)?
- [ ] Dateiende beider Dateien geprueft (Mount-Truncation, §0a)?

## Verbotene Muster

| Muster | Warum verboten | Stattdessen |
|--------|---------------|-------------|
| `exit 1` in SessionStart | Erzeugt sichtbaren Hook-Fehler | `exit 0` + Fehler ins Whiteboard loggen |
| Kein `exit 0` am Ende | PowerShell gibt Exit-Code des letzten Befehls zurueck | Explizites `exit 0` am Skript-Ende |
| `type: "prompt"` bei SessionStart | Kein ToolUseContext verfuegbar, crasht | `type: "command"` verwenden |
| Nur .ps1 ODER nur .sh | Andere Plattform hat keinen Hook | IMMER beide erstellen |
| Fehler still verschlucken | Verstoesst gegen Direktive #2 | Hook-Log + ggf. Whiteboard-Eintrag |
| Endlosschleife / langer Timeout | Blockiert Session-Start oder Tool-Nutzung | Timeout setzen, async ausfuehren |
| Hardcoded absolute Pfade | Bricht auf anderer Plattform | `$PSScriptRoot`, `$HOME`, `$(dirname "$0")` |

## Spezialfall: Async-Hooks

Hooks die lange dauern (Reindexierung, Downloads, Builds) MUESSEN async sein:
- PS1: `Start-Process pwsh -ArgumentList "-NoProfile", "-File", $scriptPath -WindowStyle Hidden`
- SH: `nohup bash "$script" &>/dev/null &` + `disown`

---

## Sichern (Cowork-Git)

Abschluss IMMER ueber das Wrapper-Skript (nie nacktes `git`):

```bash
bash ~/proggs/cowork-git.sh setup
# auf "Push-Zugang OK" warten, dann:
bash ~/proggs/cowork-git.sh push-files "#NNN - hook-forge: [name]-Hook erstellt (ps1+sh)" \
  claude-code-setup/hooks/[name].ps1 claude-code-setup/hooks/[name].sh
```

Nur die eigenen, gerade geschriebenen Pfade namentlich angeben. Kein Git-Repo verbunden →
nur speichern und den Ablage-Pfad nennen.

## Was NIEMALS passieren darf

- Einen Hook ohne `exit 0` am Ende ODER ohne `exit 0` in jedem catch/trap-Block erstellen.
- `exit 1` in einem SessionStart-Hook (erzeugt sichtbaren Hook-Fehler).
- `type: "prompt"` bei SessionStart/SessionEnd (kein ToolUseContext → crasht).
- Nur eine Plattform-Variante schreiben (.ps1 ODER .sh) — IMMER beide.
- Fehler still verschlucken statt ins Hook-Log zu schreiben (Direktive #2).
- Hardcoded absolute Pfade — `$PSScriptRoot` / `$HOME` / `$(dirname "$0")` verwenden.
- Das Dateiende nach dem Schreiben nicht pruefen (Mount-Truncation kann das finale `exit 0` abschneiden).
- Nacktes `git commit`/`git push` aus Cowork — immer `cowork-git.sh`.

## Referenzen

- `references/ps1-template.md` — vollstaendiges PowerShell-Hook-Template.
- `references/sh-template.md` — vollstaendiges Bash-Hook-Template.
- Cowork-Regeln: `bugs/claude-tooling/cowork.md` (§6 Skills), `bugs/claude-tooling/cowork-git-push.md`.
