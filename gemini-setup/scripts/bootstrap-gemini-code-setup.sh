#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

skip_validate=0

for arg in "$@"; do
  case "$arg" in
    --skip-validate)
      skip_validate=1
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: bootstrap-gemini-setup.sh [--skip-validate]" >&2
      exit 1
      ;;
  esac
done

if [[ ! -d "$REPO_ROOT/gemini-setup" ]]; then
  echo "bootstrap-gemini-setup.sh must run inside a workspace that contains gemini-setup/." >&2
  exit 1
fi

cd "$REPO_ROOT"

echo "Gemini CLI bootstrap startet im Workspace $REPO_ROOT"

if [[ "$skip_validate" -eq 0 ]]; then
  echo "[1/1] Validiere gemini-setup ..."
  if [[ -f "$SCRIPT_DIR/validate-gemini-setup.sh" ]]; then
    bash "$SCRIPT_DIR/validate-gemini-setup.sh"
  else
    echo "  validate-gemini-setup.sh nicht gefunden, uebersprungen."
  fi
else
  echo "[1/1] Validierung uebersprungen (--skip-validate)."
fi

echo "Bootstrap abgeschlossen."
echo "Naechste direkte Bruecken-Trigger:"
echo "- Starte bitte die Bruecke zu Codex"
echo "- Starte bitte die Bruecke zu Gemini CLI"
echo "Optionaler Gesamtueberblick:"
echo "- bash gemini-setup/scripts/bootstrap-report.sh"

