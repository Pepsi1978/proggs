#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
skip_validate=0

for arg in "$@"; do
  case "$arg" in
    --skip-validate) skip_validate=1 ;;
    *) echo "Unknown argument: $arg" >&2; echo "Usage: bootstrap-codex-setup.sh [--skip-validate]" >&2; exit 1 ;;
  esac
done

if [[ ! -d "$REPO_ROOT/codex-setup" ]]; then
  echo "bootstrap-codex-setup.sh must run inside a workspace that contains codex-setup/." >&2
  exit 1
fi

cd "$REPO_ROOT"
echo "Codex bootstrap startet im Workspace $REPO_ROOT"

if [[ "$skip_validate" -eq 0 ]]; then
  echo "[1/2] Synchronisiere Codex-Session-Start ..."
  if [[ -f "$SCRIPT_DIR/session-start-sync.sh" ]]; then
    bash "$SCRIPT_DIR/session-start-sync.sh"
  else
    echo "  session-start-sync.sh nicht gefunden, übersprungen."
  fi
  echo "[2/2] Validiere codex-setup ..."
  node "$SCRIPT_DIR/validate-codex-setup.mjs"
else
  echo "[1/1] Validierung übersprungen (--skip-validate)."
fi

echo "Bootstrap abgeschlossen."
echo "Nächste direkte Brücken-Trigger:"
echo "- Starte die Claude-Delta-Prüfung"
echo "- Starte die Gemini-Delta-Prüfung"
echo "Optionaler Gesamtüberblick:"
echo "- bash codex-setup/scripts/bootstrap-report.sh"
