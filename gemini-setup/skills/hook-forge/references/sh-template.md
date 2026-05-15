# Bash Hook Template

Dieses Template ist die Pflicht-Grundlage fuer JEDEN neuen Bash-Hook.
Kopiere es, passe die markierten Stellen an, und entferne die Kommentar-Markierungen.

```bash
#!/usr/bin/env bash
# [HOOK-NAME]: [Kurzbeschreibung was der Hook tut]
# Runs as [EVENT] hook (z.B. SessionStart, PostToolUse, Stop)
# stdout -> AI context (system-reminder), stderr -> user terminal
# Platform: macOS/Linux

set -euo pipefail

# --- Standard-Imports (PFLICHT) ---
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
# Falls Whiteboard-Zugriff noetig:
# . "$SCRIPT_DIR/whiteboard-insert.sh"

# --- Error Handling (PFLICHT) ---
cleanup() {
    # Aufraeum-Logik hier (optional)
    :
}
trap 'hook_log_warn "[HOOK-NAME]: Unexpected error at line $LINENO"; cleanup' ERR

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
# Ohne explizites exit 0 gibt Bash den Exit-Code des letzten
# Befehls zurueck. Bei set -e und einem trap kann das non-zero sein.
exit 0
```

## Varianten

### Minimal (fuer einfache Hooks ohne Whiteboard)

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "[HOOK-NAME]: Error at line $LINENO"' ERR

# Logik hier

exit 0
```

### Mit stdin-Parsing (fuer PreToolUse/PostToolUse)

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"

trap 'hook_log_warn "[HOOK-NAME]: Error at line $LINENO"' ERR

input=$(cat)
tool_name=$(echo "$input" | jq -r '.tool_name // empty')
tool_input=$(echo "$input" | jq -r '.tool_input // empty')

# Logik basierend auf $tool_name und $tool_input

exit 0
```

### Async (fuer lang-laufende Operationen)

```bash
#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
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

1. **IMMER `exit 0`** — am absoluten Ende des Skripts
2. **NIEMALS `exit 1`** — Fehler werden geloggt, nicht propagiert
3. **IMMER `hook-log.sh` importieren** — zentrale Protokollierung
4. **IMMER `set -euo pipefail`** — strikte Fehlerbehandlung
5. **IMMER `trap` fuer ERR** — Fehler abfangen
6. **NIEMALS interaktive Befehle** — kein read, kein Warten auf User-Input
7. **Pfade mit `$SCRIPT_DIR` oder `$HOME`** — nie hardcoded
8. **Shebang `#!/usr/bin/env bash`** — portabel zwischen macOS und Linux
