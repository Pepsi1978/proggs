#!/usr/bin/env bash
# cowork-commit.sh — Ein-Befehl-Abschluss fuer Cowork-Sessions.
# Raeumt stale Git-Lockfiles, staged Aenderungen, committet mit fortlaufender
# #NNN-Nummer und pusht. Funktioniert in der Cowork-Sandbox UND am Host, weil
# immer ins Repo-Wurzelverzeichnis gewechselt wird (relative Credential-Datei
# .git/.cowork-credentials wird dadurch zuverlaessig gefunden).
#
# Nutzung:
#   bash scripts/cowork-commit.sh                # Auto-Beschreibung
#   bash scripts/cowork-commit.sh "Beschreibung" # eigene Beschreibung
#
# Voraussetzung fuer den Push aus der Sandbox: einmalig die Token-Datei anlegen
# (siehe scripts/COWORK-PUSH-SETUP.md). Am Host genuegen die dort gespeicherten
# GitHub-Credentials.

set -euo pipefail

# 1) Immer ins Repo-Wurzelverzeichnis (macht relative Credential-Pfade robust)
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

# 2) Stale Lockfiles raeumen (von abgebrochenen Git-Laeufen / Crashes)
for lock in .git/index.lock .git/HEAD.lock .git/objects/maintenance.lock; do
  [ -e "$lock" ] && rm -f "$lock" && echo "Lock entfernt: $lock"
done

# 3) Aenderungen pruefen
if git diff --quiet && git diff --cached --quiet && [ -z "$(git status --porcelain)" ]; then
  echo "Keine Aenderungen — nichts zu committen."
  exit 0
fi

# 4) Fortlaufende Commit-Nummer ermitteln (#NNN aus der Historie + 1)
LAST="$(git log --pretty=%s -3000 2>/dev/null | grep -oE '#[0-9]+' | tr -d '#' | sort -rn | head -1 || true)"
LAST="${LAST:-0}"
NUM=$((LAST + 1))

# 5) Beschreibung (Argument oder Auto)
DESC="${1:-Cowork-Session Aenderungen}"
MSG="#${NUM} - ${DESC}"

# 6) Stagen + committen
# Parallel-Session-sicher: NIEMALS 'git add -A' (zieht halbfertige Dateien anderer
# gleichzeitig laufender Sessions mit — verstoesst gegen parallel-sessions-git-Regel).
# Stattdessen: explizit genannte Pfade (ab dem 2. Argument), sonst nur die
# Cowork-Wissens-Ordner als sicherer Default — nie etwas ausserhalb davon.
if [ "$#" -gt 1 ]; then
  shift                                  # 1. Argument war die Beschreibung
  TARGETS=("$@")
else
  TARGETS=(bugs best-practices scripts)  # sicherer Default: nur diese Ordner
fi
# Nur existierende Ziele an git add geben (fehlende Ordner ueberspringen)
EXISTING=()
for t in "${TARGETS[@]}"; do [ -e "$t" ] && EXISTING+=("$t"); done
if [ "${#EXISTING[@]}" -eq 0 ]; then echo "Keine der Ziel-Pfade existiert: ${TARGETS[*]}"; exit 0; fi
git add -- "${EXISTING[@]}"
# Nach dem gezielten Stagen pruefen, ob ueberhaupt etwas im Index liegt
if git diff --cached --quiet; then
  echo "Nichts gestaged (in ${EXISTING[*]} keine Aenderungen). Eigene Pfade angeben:"
  echo "  bash scripts/cowork-commit.sh \"$DESC\" pfad/zur/datei ..."
  exit 0
fi
git commit -m "$MSG"
echo "Committet: $MSG  (gestaged: ${EXISTING[*]})"

# 7) Vor dem Push aktualisieren (Pflicht laut Projektregeln), dann pushen
git fetch origin
git rebase origin/main || { echo "Rebase-Konflikt — bitte manuell aufloesen, dann 'git rebase --continue' + 'git push'."; exit 1; }
git push origin HEAD:main
echo "Gepusht nach origin/main."
