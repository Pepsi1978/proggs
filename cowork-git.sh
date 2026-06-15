#!/usr/bin/env bash
#
# cowork-git.sh — zuverlaessiger git commit/push aus der Claude-Cowork-Linux-VM (Windows).
#
# PROBLEM (siehe bugs/claude-tooling/cowork.md Paragraf 10a):
#   Auf dem von Windows in die Cowork-VM gemounteten Ordner kann git seine
#   .lock-Hilfsdateien nicht wieder loeschen (virtiofs/CBFS: "Operation not permitted").
#   Dadurch haengt jeder commit/push aus der VM an liegengebliebenen Locks.
#
# LOESUNG:
#   Das GIT-VERZEICHNIS (wo ALLE .lock/refs/index/objects entstehen) liegt auf der
#   VM-EIGENEN Festplatte (ext4 — loeschen funktioniert). Der Arbeitsbaum (deine echten
#   Dateien) bleibt im gemounteten Ordner. Das im Ordner liegende .git (das dein normales
#   Windows-Terminal nutzt) bleibt voellig unberuehrt — beide Welten stoeren sich nicht.
#   Quelle der Wahrheit ist origin/main; das VM-git-dir ist nur ein Vehikel zum Pushen
#   und wird bei jeder Session frisch aus GitHub aufgebaut (es ueberlebt VM-Neustarts nicht,
#   das ist gewollt und unproblematisch).
#
# NUTZUNG (in Cowork):
#   bash cowork-git.sh setup                 # holt aktuellen Stand, prueft Push-Zugang
#   bash cowork-git.sh push "#NNN - Text"    # add -A + commit + push origin main
#   bash cowork-git.sh status                # beliebiger git-Befehl (status, log, diff ...)
#
# VORAUSSETZUNG: Der Push-Token liegt persistent in <repo>/.git/credentials
#   (Format: https://Pepsi1978:<TOKEN>@github.com) — siehe COWORK-GIT-PUSH-SETUP.md.
#
set -euo pipefail

WORKTREE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GITDIR="${COWORK_GITDIR:-$HOME/.cowork-gitdir/proggs}"
REMOTE_URL="${COWORK_REMOTE:-https://github.com/Pepsi1978/proggs.git}"
BRANCH="${COWORK_BRANCH:-main}"
MOUNT_CREDS="$WORKTREE/.git/credentials"
VM_CREDS="$GITDIR/cowork-credentials"
GIT_USER_NAME="${COWORK_GIT_NAME:-Pepsi1978}"
GIT_USER_EMAIL="${COWORK_GIT_EMAIL:-barwandt@gmail.com}"

log() { printf '[cowork-git] %s\n' "$1"; }
# git-Wrapper: erzwingt das VM-git-dir + den gemounteten Arbeitsbaum
g() { git --git-dir="$GITDIR" --work-tree="$WORKTREE" "$@"; }

ensure_setup() {
  mkdir -p "$(dirname "$GITDIR")"
  if [ ! -d "$GITDIR" ]; then
    log "Lege internes git-Verzeichnis auf der VM-Platte an: $GITDIR"
    git --git-dir="$GITDIR" --work-tree="$WORKTREE" init -q -b "$BRANCH"
    g remote add origin "$REMOTE_URL"
  fi
  g config user.name  "$GIT_USER_NAME"
  g config user.email "$GIT_USER_EMAIL"

  # Token aus der persistenten Mount-Datei in eine VM-lokale Kopie uebernehmen
  # (Mount nur LESEN; alle Schreib-/Loesch-Vorgaenge bleiben auf der VM-Platte).
  if [ -f "$MOUNT_CREDS" ]; then
    cp -f "$MOUNT_CREDS" "$VM_CREDS"
    chmod 600 "$VM_CREDS" 2>/dev/null || true
    g config credential.helper "store --file=$VM_CREDS"
  else
    log "WARNUNG: $MOUNT_CREDS fehlt. Token zuerst dort hinterlegen (siehe COWORK-GIT-PUSH-SETUP.md)."
  fi

  log "Hole aktuellen Stand von origin/$BRANCH ..."
  g fetch -q origin "$BRANCH"
  g update-ref "refs/heads/$BRANCH" FETCH_HEAD
  g symbolic-ref HEAD "refs/heads/$BRANCH"
  # Index auf den origin-Stand setzen, Arbeitsdateien NICHT anfassen:
  g reset --mixed -q "$BRANCH"
  log "Bereit: Arbeitsbaum unveraendert, git-dir auf VM-Platte (Locks loeschbar)."
}

cmd="${1:-}"
case "$cmd" in
  setup)
    ensure_setup
    if g ls-remote --exit-code origin -h "refs/heads/$BRANCH" >/dev/null 2>&1; then
      log "Push-Zugang OK (Auth-/Lese-Test bestanden). Bereit fuer 'push'."
    else
      log "FEHLER: Kein Zugang zu origin. Token in $MOUNT_CREDS pruefen."
      exit 1
    fi
    ;;
  push)
    msg="${2:-update from cowork}"
    ensure_setup
    g add -A
    if g diff --cached --quiet; then
      log "Nichts zu committen (Arbeitsbaum = origin/$BRANCH)."
      exit 0
    fi
    g commit -m "$msg"
    g push origin "HEAD:$BRANCH"
    log "Erfolgreich nach origin/$BRANCH gepusht."
    ;;
  "")
    log "Nutzung: bash cowork-git.sh {setup | push \"Nachricht\" | <git-befehl>}"
    exit 2
    ;;
  *)
    # Jeder andere git-Befehl mit korrektem git-dir/work-tree:
    g "$@"
    ;;
esac
