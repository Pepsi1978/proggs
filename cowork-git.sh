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
# MOUNT-ARTEFAKTE, die dieses Skript abfaengt (sonst wird der Commit unbrauchbar):
#   1) .lock-Dateien nicht loeschbar         -> git-dir liegt auf der VM-Platte (ext4).
#   2) Datei-Modus immer 0755 gemeldet       -> core.fileMode=false (keine 644->755-Diffs).
#   3) Manche Symlinks nicht lesbar (I/O)    -> guard_unreadable_symlinks (skip-worktree).
#   4) Git-LFS-Dateien als Vollinhalt sichtbar -> guard_lfs_pointers (skip-worktree),
#      sonst ersetzt 'add -A' die kleinen LFS-Zeiger durch echte Riesendateien und der
#      Push wird von GitHub (100-MB-Limit) abgelehnt.
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
  # Der Windows->VM-Mount meldet ALLE Dateien als ausfuehrbar (0755). Ohne diese Zeile
  # wuerde git bei JEDER getrackten Datei einen 644->755 Modus-Wechsel als Aenderung
  # stagen und den Commit mit Tausenden Schein-Aenderungen aufblaehen. false = ignorieren.
  g config core.fileMode false

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

# Schutz gegen Mount-Lesefehler: Manche getrackten Symlinks sind Windows-Symlinks,
# die ueber den gemounteten Ordner (virtiofs/CBFS) nicht lesbar sind ("readlink:
# Input/output error"). 'git add -A' bricht dann mit "unable to index file ...
# fatal: updating files failed" ab. Da diese Symlinks bereits unveraendert in
# origin/main liegen, nehmen wir genau die unlesbaren per skip-worktree von der
# Neu-Indizierung aus. (Muss bei JEDEM push laufen, weil 'git reset --mixed' in
# ensure_setup die skip-worktree-Flags wieder zuruecksetzt.)
guard_unreadable_symlinks() {
  local meta path
  g ls-files -s | while IFS="$(printf '\t')" read -r meta path; do
    case "$meta" in
      120000\ *)
        if ! readlink "$WORKTREE/$path" >/dev/null 2>&1; then
          g update-index --skip-worktree "$path" 2>/dev/null \
            && log "Unlesbarer Mount-Symlink ausgenommen (skip-worktree): $path"
        fi
        ;;
    esac
  done
  return 0
}

# Schutz gegen LFS-Aufblaehung: Git-LFS-Dateien (in .gitattributes mit filter=lfs)
# liegen im origin nur als kleine Zeiger (~130 Byte), erscheinen im gemounteten
# Arbeitsbaum aber als voller Inhalt (z. B. 262-MB-ONNX-Modelle). Ohne Schutz wuerde
# 'add -A' die Zeiger durch die echten Riesendateien ersetzen -> Push-Ablehnung durch
# GitHubs 100-MB-Limit und dauerhaftes Aufblaehen des Repos. Daher: alle getrackten
# LFS-Dateien per skip-worktree von 'add -A' ausnehmen. (Echte Pflege von LFS-Dateien
# erfolgt vom Windows-Rechner, nicht aus der Cowork-VM.)
guard_lfs_pointers() {
  [ -f "$WORKTREE/.gitattributes" ] || return 0
  local pat f
  grep -E 'filter=lfs' "$WORKTREE/.gitattributes" 2>/dev/null | awk '{print $1}' | while read -r pat; do
    [ -z "$pat" ] && continue
    g ls-files -z -- "$pat" 2>/dev/null | while IFS= read -r -d '' f; do
      g update-index --skip-worktree "$f" 2>/dev/null \
        && log "LFS-Datei vom add -A ausgenommen (skip-worktree): $f"
    done
  done
  return 0
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
    guard_unreadable_symlinks
    guard_lfs_pointers
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
