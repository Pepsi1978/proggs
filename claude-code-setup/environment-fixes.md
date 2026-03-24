# Environment Fixes — Cross-Tool Knowledge Base

**Zweck**: Zentrale Dokumentation aller Umgebungs-Fixes, die fuer ALLE AI-Tools gelten.
Wird gelesen von: Claude Code, Codex CLI, Gemini CLI.
Jeder Fix hier verhindert, dass ein anderes Tool den gleichen Fehler macht.

**WICHTIG fuer alle lesenden Tools**: Jeder Eintrag ist so geschrieben, dass er OHNE
zusaetzlichen Kontext verstaendlich ist. Wenn du als CLI-Tool diesen Fix liest, hast du
alle Informationen die du brauchst um den gleichen Fehler zu vermeiden — inklusive
Hintergrund, Code-Beispielen und konkreten Regeln.

**Schwester-Datenbanken** (andere CLIs dokumentieren ihre Fixes hier):
- Codex CLI: `codex-setup/state/environment-fixes.json`
- Gemini CLI: `Gemini-Setup/agent-memory/shared/MEMORY.md`

**Format pro Eintrag**:
- **Datum** und **Plattform** (Windows/macOS/beide)
- **Kontext**: Was wurde gerade gemacht als der Fehler auftrat (damit andere CLIs die Situation verstehen)
- **Symptom**: Was sichtbar schiefging (exakte Fehlermeldung wenn moeglich)
- **Root Cause**: WARUM es passiert ist (tiefste Ursache, nicht nur das Symptom)
- **Fix**: Was konkret geaendert wurde (mit Code-Beispiel)
- **Vermeidungsregel**: Was in Zukunft zu beachten ist (als klare Wenn-Dann-Regel)

---

## 2026-03-24 — Missing standalone Kotlin/Gradle/pipx after PowerShell 7.6 upgrade (Windows)

**Plattform:** Windows
**Kontext:** User upgraded PowerShell manually from 7.5.5 to 7.6.0. Full environment audit
revealed that Kotlin standalone compiler, Gradle, pipx, and `.local/bin` were missing from
the system. The PowerShell upgrade itself was clean (no PATH corruption), but the audit
exposed pre-existing gaps that the CLAUDE.md PATH reference list demanded.
**Symptom:** `kotlinc: command not found`, `gradle: command not found`, `pipx: command not found`.
PATH entries `C:\Kotlin\kotlinc\bin`, `C:\Gradle\gradle-9.4.1\bin`, `%USERPROFILE%\.local\bin`
were listed in CLAUDE.md but the actual directories did not exist on the system.
JAVA_HOME was only set at Machine level, not User level.
**Root Cause:** Kotlin and Gradle were never installed as standalone tools — they were only
available bundled inside Android Studio. The CLAUDE.md reference list was aspirational but
the actual installations were missing. pipx was not installed because `uv tool install` was
used directly. `.local/bin` was never created.
**Fix:**
1. Kotlin: Downloaded `kotlin-compiler-2.3.20.zip` from JetBrains GitHub releases, extracted
   to `C:\Kotlin\` (creates `C:\Kotlin\kotlinc\bin\kotlinc.bat`)
2. Gradle: Downloaded `gradle-9.4.1-bin.zip` from services.gradle.org, extracted to
   `C:\Gradle\` (creates `C:\Gradle\gradle-9.4.1\bin\gradle.bat`)
3. pipx: `uv tool install pipx` (installs to `.local/bin`)
4. `.local/bin`: `mkdir -p "$USERPROFILE/.local/bin"`
5. PATH: Added all three missing entries to User PATH via `[Environment]::SetEnvironmentVariable`
6. JAVA_HOME: Copied Machine-level value to User level
7. Verification: `kotlinc -version` (2.3.20), `gradle --version` (9.4.1), `pipx --version` (1.11.0),
   Kotlin+Java integration test (compiled and ran Hello World), Gradle+Java integration (detected JVM 21)
**Vermeidungsregel:**
- After ANY shell/runtime upgrade, run the full PATH verification from CLAUDE.md (11 entries + 3 env vars)
- Standalone tools (Kotlin, Gradle) must be installed OUTSIDE of Android Studio for CLI use
- When updating Gradle version: change BOTH the directory name (`C:\Gradle\gradle-X.Y.Z\bin`)
  AND the CLAUDE.md reference list entry
- `gh release download` for GitHub assets, NOT `curl` to `api.github.com` (hook blocks raw curl)

---

## 2026-03-23 — Python cp1252 Encoding Crash (Windows)

**Plattform:** Windows
**Kontext:** Ein Python-Einzeiler wurde ueber die Bash-Shell ausgefuehrt, um die Claude Code
settings.json zu aktualisieren (neue Hook-Events und Permissions hinzufuegen). Die JSON-Datei
enthielt Prompt-Texte mit Emoji-Zeichen (z.B. das Gluehbirnen-Emoji in einem PreCompact-Hook).
Das Skript verwendete `json.dump(data, f, ensure_ascii=False)` um die Datei zu schreiben.
**Symptom:** Python crashte mit: `UnicodeEncodeError: 'charmap' codec can't encode character '\U0001f4a1' in position 234: character maps to <undefined>`. Die Datei wurde dabei beschaedigt (siehe naechster Eintrag).
**Root Cause:** Windows Python verwendet standardmaessig `cp1252` als Dateikodierung wenn man
`open(path, 'w')` ohne explizites Encoding aufruft. `cp1252` ist ein 8-Bit-Zeichensatz der
nur 256 Zeichen kennt — Emojis, CJK-Zeichen und viele Unicode-Zeichen sind darin nicht
darstellbar. Auf macOS ist der Standard UTF-8, daher tritt der Fehler dort nie auf.
**Fix:** Bei JEDEM `open()` Aufruf explizit `encoding='utf-8'` angeben:
```python
# Lesen:
with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)
# Schreiben:
with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
```
**Vermeidungsregel:** Auf Windows NIEMALS `open()` ohne `encoding='utf-8'` verwenden wenn die Datei Unicode enthalten koennte. Auf macOS ist UTF-8 der Standard, dort faellt der Fehler nicht auf.

---

## 2026-03-23 — Abgeschnittene settings.json durch Crash beim Schreiben (Windows)

**Plattform:** Windows (kann auch auf macOS passieren)
**Kontext:** Direkt nach dem vorherigen Encoding-Crash wurde der gleiche Python-Befehl nochmal
ausgefuehrt, diesmal mit `encoding='utf-8'`. Aber die Datei war bereits vom ersten Versuch
beschaedigt — der erste `open(path, 'w')` hatte den Original-Inhalt geloescht und nur 277 von
522 Zeilen geschrieben bevor der Crash kam. Der zweite Versuch konnte die beschaedigte Datei
nicht mehr als JSON parsen → `JSONDecodeError: Expecting value: line 278`.
Die Datei musste komplett neu geschrieben werden aus dem Gedaechtnis der Session.
**Symptom:** `settings.json` war nach dem Python-Crash nur noch 277 statt 522 Zeilen lang. Die Datei war nicht mehr als JSON lesbar. Kein Backup vorhanden weil die Datei ausserhalb des Git-Repos liegt.
**Root Cause:** Python oeffnet die Datei mit `open(path, 'w')` was den Inhalt SOFORT loescht
(truncate on open). Wenn der Schreibvorgang dann crasht (z.B. Encoding-Fehler), bleibt eine
halbe Datei zurueck. Es gibt keinen Rollback-Mechanismus — der alte Inhalt ist unwiederbringlich
verloren sobald `open('w')` aufgerufen wird.
**Fix:** Atomares Schreiben verwenden — erst in eine temporaere Datei schreiben, dann umbenennen:
```python
import tempfile, os, json

def safe_json_write(path, data):
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                      delete=False, encoding='utf-8') as tmp:
        json.dump(data, tmp, indent=2, ensure_ascii=False)
        tmp.write('\n')
        tmp_path = tmp.name
    os.replace(tmp_path, path)  # Atomic on all platforms
```
**Vermeidungsregel:** Kritische Konfigurationsdateien (settings.json, MEMORY.md, session-scores.jsonl) NIEMALS direkt mit `open('w')` ueberschreiben. Immer atomares Write-to-Temp-then-Rename verwenden.

---

## 2026-03-23 — Settings-Drift zwischen macOS und Windows (beide)

**Plattform:** beide
**Kontext:** Der Benutzer arbeitet auf zwei Rechnern (macOS = Hauptrechner, Windows = Zweitrechner)
mit der gleichen Claude Code Konfiguration. Auf macOS wurden in mehreren Sessions neue Features
hinzugefuegt: 3 neue Hook-Events (ConfigChange, Stop, SubagentStart), 43 GitHub-MCP-Permissions,
2 neue SessionStart-Hooks (claude-mem-worker-guard, plugin-health-check) und 2 neue Plugins.
Beim naechsten Start auf Windows fehlte all das — die Windows-Installation war 2 Tage hinterher.
**Symptom:** Nach macOS-Sessions fehlten auf Windows 3 Hook-Events (ConfigChange, Stop, SubagentStart), 43 GitHub-Permissions und 2 Plugins. Windows war 2 Tage hinterher.
**Root Cause:** `~/.claude/settings.json` liegt AUSSERHALB des Git-Repos. Der auto-sync Hook synchronisiert nur Dateien innerhalb `~/.claude/hooks/`, `rules/`, `agents/`, aber NICHT die settings.json selbst. Aenderungen auf macOS (neue Hooks registrieren, Permissions hinzufuegen) kommen deshalb nicht automatisch auf Windows an.
**Fix:** Manueller Abgleich: settings-reference.json (im Repo, 1:1-Kopie der Windows-Settings) mit der macOS-Version vergleichen und fehlende Eintraege manuell uebertragen. Hooks auf Windows muessen `.ps1`-Varianten verwenden statt `.sh`.
**Vermeidungsregel:** Nach JEDER macOS-Session die Settings aendert: settings-reference.json committen. Beim naechsten Windows-Start: Diff pruefen und lokal uebernehmen. Langfristig: Auto-Diff-Hook der beim SessionStart warnt wenn Reference und lokal auseinanderlaufen.

---

## 2026-03-23 — Hook-Kommando-Format macOS vs Windows

**Plattform:** beide
**Kontext:** Die Claude Code Programmierumgebung nutzt "Hooks" — Skripte die bei bestimmten
Ereignissen automatisch ausgefuehrt werden (z.B. SessionStart, SubagentStop, ConfigChange).
Jeder Hook wird in `~/.claude/settings.json` registriert mit einem Shell-Kommando.
Auf macOS wurden 3 neue Hook-Events hinzugefuegt mit Bash-Kommandos (`bash ~/.claude/hooks/config-guard.sh`).
Beim Uebertragen auf Windows mussten diese in PowerShell-Kommandos umgeschrieben werden.
**Symptom:** Neue Hook-Events (ConfigChange, Stop, SubagentStart) waren auf macOS mit `bash ~/.claude/hooks/*.sh` registriert, funktionierten aber auf Windows nicht weil dort `.ps1` ueber `pwsh` ausgefuehrt werden muss.
**Root Cause:** Zwei verschiedene Shell-Oekosysteme. macOS nutzt native Bash (vorinstalliert),
Windows nutzt PowerShell 7 (pwsh) fuer Hooks. Die Bash von Git Bash auf Windows KANN zwar
.sh-Skripte ausfuehren, aber die .ps1-Varianten sind zuverlaessiger weil sie native Windows-APIs
nutzen koennen (z.B. Windows-Benachrichtigungen, Registry-Zugriff, .NET-Integration).
**Fix:** Jeder Hook existiert als Paar: `.sh` (macOS) + `.ps1` (Windows). In settings.json werden die plattform-spezifischen Kommandos registriert:
```
macOS:   "bash ~/.claude/hooks/config-guard.sh"
Windows: "pwsh -NoProfile -ExecutionPolicy Bypass -File \"$USERPROFILE/.claude/hooks/config-guard.ps1\""
```
**Vermeidungsregel:** Beim Anlegen neuer Hooks IMMER beide Varianten erstellen (.sh + .ps1) und in BEIDEN Settings-Dateien (settings.json macOS + settings-reference.json Windows) registrieren.

---

## 2026-03-23 — grep -P (Perl-Regex) funktioniert nicht auf Windows (Windows)

**Plattform:** Windows
**Kontext:** In der Claude Code Umgebung werden regelmaessig Shell-Befehle ueber das Bash-Tool
ausgefuehrt. Auf Windows laeuft dabei Git Bash (eine Bash-Emulation die mit Git for Windows
mitgeliefert wird). Ein Befehl versuchte mit `grep -P '^\S+ #\K\d+'` eine Commit-Nummer
aus der Git-Log-Ausgabe zu extrahieren. Das `-P` Flag aktiviert Perl-kompatible regulaere
Ausdruecke (PCRE), die maechtigere Muster wie `\K` (Lookbehind-Reset) ermoeglichen.
**Symptom:** `grep: -P supports only unibyte and UTF-8 locales` — der Befehl schlaegt mit
Exit-Code 2 fehl. Kein Ergebnis wird zurueckgegeben.
**Root Cause:** Die `grep`-Version in Git Bash fuer Windows ist gegen eine Bibliothek kompiliert
die PCRE nur in Einbyte- und UTF-8-Locales unterstuetzt. Die Windows-Locale ist aber
typischerweise eine Multibyte-Locale (z.B. `German_Germany.1252`) die nicht als UTF-8 erkannt
wird. Auf macOS ist die Standard-Locale `en_US.UTF-8`, daher funktioniert `grep -P` dort
problemlos. Auch `ripgrep` (`rg`) auf Windows hat dieses Problem NICHT — es nutzt eine eigene
Regex-Engine. Aber im Claude Code Kontext wird oft das eingebaute `grep` ueber Bash aufgerufen.
**Fix:** Statt `grep -P` auf Windows alternative Werkzeuge verwenden:
```bash
# FALSCH — crasht auf Windows Git Bash:
git log --oneline -1 | grep -oP '^\S+ #\K\d+'

# RICHTIG — funktioniert ueberall:
# Variante 1: awk (in Git Bash enthalten)
git log --oneline -1 | awk -F'#' '{print $2}' | awk '{print $1}'

# Variante 2: sed mit einfachem Regex
git log --oneline -1 | sed 's/.*#\([0-9]*\).*/\1/'

# Variante 3: Bash Parameter Expansion (kein externes Tool noetig)
line=$(git log --oneline -1); num=${line#*#}; echo ${num%% *}
```
**Vermeidungsregel:** Auf Windows NIEMALS `grep -P` verwenden. Immer `awk`, `sed` mit
Basic-Regex, oder Bash-Builtins als Alternative nutzen. Wenn Perl-Regex zwingend noetig ist,
`perl -ne` oder `python3 -c` als Ersatz verwenden. Diese Regel gilt fuer ALLE Shell-Befehle
die ueber das Bash-Tool ausgefuehrt werden, nicht nur fuer Git-Befehle.

---

## Template fuer neue Eintraege

```markdown
## DATUM — Kurzbeschreibung (Plattform)

**Plattform:** Windows/macOS/beide
**Kontext:** Was wurde gerade gemacht? Welche Aufgabe, welcher Workflow, welches Tool?
  (Schreibe genug Kontext, dass ein anderes CLI-Tool das noch nie mit dieser Umgebung
  gearbeitet hat die Situation komplett verstehen kann.)
**Symptom:** Was sichtbar schiefging (exakte Fehlermeldung wenn moeglich)
**Root Cause:** WARUM es passiert ist (tiefste Ursache, technisch erklaert,
  inklusive WARUM es auf der anderen Plattform NICHT passiert)
**Fix:** Was konkret geaendert wurde (mit vollstaendigem Code-Beispiel —
  sowohl das falsche als auch das richtige Muster zeigen)
**Vermeidungsregel:** Als klare Wenn-Dann-Regel formulieren:
  "Wenn [Situation], dann IMMER [Aktion] statt [was man instinktiv tun wuerde]"
```
