# Bash Hook Template

Dieses Template ist die Pflicht-Grundlage fuer JEDEN neuen Bash-Hook.
Kopiere es, passe die markierten Stellen an, und entferne die Kommentar-Markierungen.

> **WARUM `exit 0` IM trap (KRITISCH):** Mit `set -e` beendet ein scheiternder Befehl das
> Skript SOFORT mit non-zero — der ERR-trap loggt zwar, aber danach bricht `set -e` ab und
> das `exit 0` am Dateiende wird NIE erreicht. Folge: Hook gibt non-zero zurueck → "hook error".
> Deshalb gehoert `exit 0` IN jeden ERR-trap (nicht nur ans Dateiende). So endet der Hook bei
> JEDEM unerwarteten Fehler graceful. (Eigener Vorfall 2026-06-01: bug-almanac-Hooks hatten
> genau diesen Bug; siehe bugs/claude-tooling/claude-hooks.md 13.4.)

```bash
#!/usr/bin/env bash
# [HOOK-NAME]: [Kurzbeschreibung was der Hook tut]
# Runs as [EVENT] hook (z.B. SessionStart, PostToolUse, Stop)
# stdout -> AI context (system-reminder), stderr -> user terminal
# Platform: macOS/Linux

set -euo pipefail

# --- Standard-Imports (PFLICHT) ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
# Falls Whiteboard-Zugriff noetig:
# . "$SCRIPT_DIR/whiteboard-insert.sh"

# --- Error Handling (PFLICHT) ---
cleanup() {
    # Aufraeum-Logik hier (optional)
    :
}
# exit 0 IM trap (PFLICHT) - sonst beendet set -e bei einem Fehler mit non-zero.
trap 'hook_log_warn "[HOOK-NAME]: Unexpected error at line $LINENO"; cleanup; exit 0' ERR

# ============================================================
# HOOK-LOGIK HIER EINFUEGEN
# ============================================================

# Beispiel: Status an AI-Kontext und User-Terminal schreiben
# write_status() { echo "$1"; echo "$1" >&2; }
# write_status "[HookName]: Alles OK."

# Beispiel: Fehler ins Log schreiben
# hook_log "[HookName]: Operation erfolgreich"

# ============================================================

# PFLICHT: Jeder Hook MUSS mit exit 0 enden — ausnahmslos.
exit 0
```

## Varianten

### Minimal (fuer einfache Hooks ohne Whiteboard)

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "[HOOK-NAME]: Error at line $LINENO"; exit 0' ERR

# Logik hier

exit 0
```

### Mit stdin-Parsing (fuer PreToolUse/PostToolUse)

> stdin per **python3** parsen, NICHT per `jq`: jq ist eine optionale Abhaengigkeit, die auf
> frischen macOS-Systemen und unter Git-Bash oft fehlt — dann liefert es leere Werte und der
> Hook versagt stumm (bugs/claude-tooling/claude-hooks.md 13.2). python3 ist praktisch ueberall vorhanden.

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "[HOOK-NAME]: Error at line $LINENO"; exit 0' ERR

input=$(cat)
[ -n "$input" ] || exit 0
tool_name=$(printf '%s' "$input" | python3 -c "import json,sys; print(json.load(sys.stdin).get('tool_name','') or '')" 2>/dev/null || echo "")
file_path=$(printf '%s' "$input" | python3 -c "import json,sys; print((json.load(sys.stdin).get('tool_input') or {}).get('file_path','') or '')" 2>/dev/null || echo "")

# Logik basierend auf $tool_name und $file_path

exit 0
```

### Async (fuer lang-laufende Operationen)

```bash
#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

worker="$SCRIPT_DIR/[WORKER-SCRIPT].sh"
if [ -f "$worker" ]; then
    nohup bash "$worker" &>/dev/null &
    disown $! 2>/dev/null || true
    hook_log "[HOOK-NAME]: Worker gestartet (async)"
fi

exit 0
```

## Wichtige Regeln

1. **IMMER `exit 0`** — am absoluten Ende des Skripts UND im ERR-trap
2. **NIEMALS `exit 1`** — Fehler werden geloggt, nicht propagiert
3. **IMMER `hook-log.sh` importieren** — zentrale Protokollierung
4. **IMMER `set -euo pipefail`** — strikte Fehlerbehandlung
5. **IMMER `trap` fuer ERR — MIT `exit 0` im trap** (sonst beendet `set -e` non-zero, siehe oben)
6. **stdin per `python3` parsen, nicht per `jq`** — jq kann fehlen und versagt dann stumm
7. **NIEMALS interaktive Befehle** — kein read, kein Warten auf User-Input
8. **Pfade mit `$SCRIPT_DIR` (`${BASH_SOURCE[0]}`) oder `$HOME`** — nie hardcoded
9. **Shebang `#!/usr/bin/env bash`** — portabel zwischen macOS und Linux
