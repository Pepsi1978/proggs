#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

skip_install=0
skip_validate=0

for arg in "$@"; do
  case "$arg" in
    --skip-install)
      skip_install=1
      ;;
    --skip-validate)
      skip_validate=1
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: bootstrap-codex-setup.sh [--skip-install] [--skip-validate]" >&2
      exit 1
      ;;
  esac
done

if [[ ! -d "$REPO_ROOT/codex-setup" ]]; then
  echo "bootstrap-codex-setup.sh must run inside a Codex workspace that contains codex-setup/." >&2
  exit 1
fi

cd "$REPO_ROOT"

echo "Codex bootstrap startet im Workspace $REPO_ROOT"

if [[ "$skip_install" -eq 0 ]]; then
  echo "[1/3] Installiere self-improve lokal nach ~/.codex/skills ..."
  bash "$SCRIPT_DIR/install-self-improve.sh"
  echo "[2/3] Konfiguriere Git-Hooks fuer den lokalen Pre-Commit-Scanner ..."
  bash "$SCRIPT_DIR/install-git-hooks.sh"
else
  echo "[1/3] self-improve-Installation uebersprungen (--skip-install)."
  echo "[2/3] Git-Hook-Installation uebersprungen (--skip-install)."
fi

if [[ "$skip_validate" -eq 0 ]]; then
  echo "[3/3] Validiere codex-setup ..."
  bash "$SCRIPT_DIR/validate-codex-setup.sh"
else
  echo "[3/3] Validierung uebersprungen (--skip-validate)."
fi

echo "Bootstrap abgeschlossen."
echo "Naechste direkte Bruecken-Trigger:"
echo "- Starte bitte die Bruecke zu Claude Code"
echo "- Starte bitte die Bruecke zu Gemini CLI"
echo "Optionaler Gesamtueberblick:"
echo "- bash codex-setup/scripts/bootstrap-report.sh"
