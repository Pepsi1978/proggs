#!/usr/bin/env bash
# validate.sh — Cross-Platform-Wrapper fuer validate_extracted.py
#
# Findet automatisch den richtigen Python-Befehl (python3 oder python),
# damit der Validator auf macOS UND Windows funktioniert.
#
# Aufruf (gleich wie validate_extracted.py direkt):
#   bash scripts/validate.sh <strings.xml>
#   bash scripts/validate.sh <strings.xml> --code-dir <java>
#   bash scripts/validate.sh <strings.xml> --suggest-donottranslate
#   bash scripts/validate.sh <strings.xml> --strict
#
# Hintergrund:
#   - macOS: python ist oft Python 2 (oder fehlt), python3 ist Standard
#   - Windows: python ist Python 3, python3 fehlt typischerweise
#   - Linux: beides moeglich, python3 verlaesslicher

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALIDATOR="$SCRIPT_DIR/validate_extracted.py"

if [ ! -f "$VALIDATOR" ]; then
    echo "FEHLER: validate_extracted.py nicht gefunden in $SCRIPT_DIR" >&2
    exit 2
fi

# Python-Befehl finden (Reihenfolge: python3 > python falls Python 3)
PYTHON=""
if command -v python3 >/dev/null 2>&1; then
    PYTHON="python3"
elif command -v python >/dev/null 2>&1; then
    # Pruefen ob "python" wirklich Python 3 ist
    PY_VER=$(python -c 'import sys; print(sys.version_info[0])' 2>/dev/null || echo "0")
    if [ "$PY_VER" = "3" ]; then
        PYTHON="python"
    fi
fi

if [ -z "$PYTHON" ]; then
    echo "FEHLER: Weder 'python3' noch 'python' (Version 3) gefunden im PATH." >&2
    echo "Bitte Python 3 installieren: https://www.python.org/downloads/" >&2
    exit 2
fi

exec "$PYTHON" "$VALIDATOR" "$@"
