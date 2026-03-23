# Environment Fixes — Cross-Tool Knowledge Base

**Zweck**: Zentrale Dokumentation aller Umgebungs-Fixes, die fuer ALLE AI-Tools gelten.
Wird gelesen von: Claude Code, Codex CLI, Gemini CLI.
Jeder Fix hier verhindert, dass ein anderes Tool den gleichen Fehler macht.

**Format pro Eintrag**:
- **Datum** und **Plattform** (Windows/macOS/beide)
- **Was war kaputt** (Symptom)
- **Warum** (Root Cause)
- **Wie gefixt** (konkreter Fix)
- **Vermeidungsregel** (was in Zukunft zu beachten ist)

---

## 2026-03-23 — Python cp1252 Encoding Crash (Windows)

**Plattform:** Windows
**Symptom:** Python-Skript crasht beim Schreiben von JSON-Dateien die Emojis enthalten mit `UnicodeEncodeError: 'charmap' codec can't encode character`.
**Root Cause:** Windows Python verwendet standardmaessig `cp1252` als Dateikodierung. Emojis und viele Unicode-Zeichen sind darin nicht darstellbar.
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
**Symptom:** `settings.json` war nach einem Python-Crash nur noch 277 statt 522 Zeilen lang. Die Datei war nicht mehr als JSON lesbar. Claude Code konnte die Konfiguration nicht mehr laden.
**Root Cause:** Python oeffnet die Datei mit `open(path, 'w')` was den Inhalt SOFORT loescht. Wenn der Schreibvorgang dann crasht (z.B. Encoding-Fehler), bleibt eine halbe Datei zurueck.
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
**Symptom:** Nach macOS-Sessions fehlten auf Windows 3 Hook-Events (ConfigChange, Stop, SubagentStart), 43 GitHub-Permissions und 2 Plugins. Windows war 2 Tage hinterher.
**Root Cause:** `~/.claude/settings.json` liegt AUSSERHALB des Git-Repos. Der auto-sync Hook synchronisiert nur Dateien innerhalb `~/.claude/hooks/`, `rules/`, `agents/`, aber NICHT die settings.json selbst. Aenderungen auf macOS (neue Hooks registrieren, Permissions hinzufuegen) kommen deshalb nicht automatisch auf Windows an.
**Fix:** Manueller Abgleich: settings-reference.json (im Repo, 1:1-Kopie der Windows-Settings) mit der macOS-Version vergleichen und fehlende Eintraege manuell uebertragen. Hooks auf Windows muessen `.ps1`-Varianten verwenden statt `.sh`.
**Vermeidungsregel:** Nach JEDER macOS-Session die Settings aendert: settings-reference.json committen. Beim naechsten Windows-Start: Diff pruefen und lokal uebernehmen. Langfristig: Auto-Diff-Hook der beim SessionStart warnt wenn Reference und lokal auseinanderlaufen.

---

## 2026-03-23 — Hook-Kommando-Format macOS vs Windows

**Plattform:** beide
**Symptom:** Neue Hook-Events (ConfigChange, Stop, SubagentStart) waren auf macOS mit `bash ~/.claude/hooks/*.sh` registriert, funktionierten aber auf Windows nicht weil dort `.ps1` ueber `pwsh` ausgefuehrt werden muss.
**Root Cause:** Zwei verschiedene Shell-Oekosysteme. macOS nutzt native Bash, Windows nutzt PowerShell (pwsh) fuer Hooks.
**Fix:** Jeder Hook existiert als Paar: `.sh` (macOS) + `.ps1` (Windows). In settings.json werden die plattform-spezifischen Kommandos registriert:
```
macOS:   "bash ~/.claude/hooks/config-guard.sh"
Windows: "pwsh -NoProfile -ExecutionPolicy Bypass -File \"$USERPROFILE/.claude/hooks/config-guard.ps1\""
```
**Vermeidungsregel:** Beim Anlegen neuer Hooks IMMER beide Varianten erstellen (.sh + .ps1) und in BEIDEN Settings-Dateien (settings.json macOS + settings-reference.json Windows) registrieren.

---

## Template fuer neue Eintraege

```markdown
## DATUM — Kurzbeschreibung (Plattform)

**Plattform:** Windows/macOS/beide
**Symptom:** Was sichtbar schiefging
**Root Cause:** WARUM es passiert ist (tiefste Ursache)
**Fix:** Was konkret geaendert wurde (mit Code-Beispiel wenn hilfreich)
**Vermeidungsregel:** Was in Zukunft zu beachten ist um den Fehler nie wieder zu machen
```
