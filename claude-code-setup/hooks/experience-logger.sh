#!/usr/bin/env bash
# experience-logger — SessionEnd Hook (macOS/Linux Wrapper)
# Reicht den SessionEnd-stdin (transcript_path) an experience-logger.py weiter,
# das die ECHTEN Session-Signale aus dem Transcript zieht (gemeinsame Cross-Platform-Logik).
# Ersetzt die alte parasitaere Logik (las nur die letzte session-scores-Zeile -> Platzhalter,
# Feldnamen-Mismatch, datentot seit 2026-04-12).
# Faellt stdin aus, findet experience-logger.py das neueste Transcript selbst.
# Direktive #3: Graceful Degradation — IMMER exit 0, nie die Session blockieren.
set +e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PY="$SCRIPT_DIR/experience-logger.py"
if [ -f "$PY" ]; then
    cat | python3 "$PY" 2>/dev/null || true
fi
exit 0
