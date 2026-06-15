#!/usr/bin/env bash
#
# cowork-git.sh — zuverlaessiger git commit/push aus der Claude-Cowork-Linux-VM (Windows).
#
# GRUNDPROBLEM:
#   Cowork arbeitet nicht direkt auf dem Windows-Ordner, sondern ueber eine gemountete Bruecke
#   (virtiofs/CBFS) zu einer Linux-VM. Diese Bruecke verzerrt, wie Dateien aussehen UND ist beim
#   Lesen/Schreiben kuerzlich geaenderter Dateien unzuverlaessig (Truncation, Padding, Versions-
#   Flackern, Linter-Interferenz). Deshalb liegt der git-Maschinenraum (Index/Locks/Objekte) auf
#   der VM-ext4-Platte (~/.cowork-gitdir/proggs), nur der Arbeitsbaum bleibt auf der Bruecke.
#   Quelle der Wahrheit ist origin/main.
#
# MOUNT-FALLEN, die dieses Skript abfaengt (sonst wird der Commit unbrauchbar ODER zerstoert Daten):
#   1) .lock nicht loeschbar              -> git-dir auf VM-ext4.
#   2) Datei-Modus immer 0755             -> core.fileMode=false.
#   3) Symlinks nicht lesbar (I/O)        -> guard_unreadable_symlinks (skip-worktree).
#   4) Git-LFS-Dateien als Vollinhalt     -> guard_lfs_pointers (skip-worktree); sonst >100 MB -> GitHub lehnt ab.
#   5) Build-/Abhaengigkeits-Berge        -> **/build/ **/.gradle/ **/node_modules/ in .gitignore.
#   6) DATENVERLUST durch Mount-Lesefehler-> guard_data_loss: bricht VOR dem Commit ab, wenn eine
#      getrackte Datei verdaechtig stark schrumpft (Truncation) oder faelschlich als geloescht
#      gestaged wird (Phantom-Loeschung). DAS ist der wichtigste Schutz: aus stillem Datenverlust
#      wird ein lauter Stopp. Bewusst ueberschreibbar mit COWORK_ALLOW_SHRINK=1.
#
# NUTZUNG (in Cowork):
#   bash cowork-git.sh setup                          # Stand holen, Push-Zugang pruefen
#   bash cowork-git.sh push "#NNN - Text"             # add -A + Waechter + commit + push
#   bash cowork-git.sh push-files "#NNN - Text" a b   # NUR Dateien a,b (gezielt) + Waechter + push
#   bash cowork-git.sh <git-befehl>                   # beliebiger git-Befehl (status, log, diff ...)
#
#   COWORK_WORKTREE         -> Arbeitsbaum-Pfad ueberschreiben (Default: Ordner des Skripts).
#                             Sinnvoll, wenn das Skript wegen Mount-Flackern aus einer stabilen
#                             VM-Kopie ausgefuehrt wird, aber auf den proggs-Mount zeigen soll.
#   COWORK_ALLOW_SHRINK=1   -> Waechter erlaubt absichtliches Schrumpfen/Loeschen.
#   COWORK_SHRINK_MAX_PCT   -> ab wie viel % Schrumpfung verdaechtig (Default 30).
#   COWORK_SHRINK_MIN_BYTES -> absolute Mindestschrumpfung, sonst egal (Default 200).
#
# VORAUSSETZUNG: Push-Token persistent in <repo>/.git/credentials (siehe COWORK-GIT-PUSH-SETUP.md).
#
set -euo pipefail

WORKTREE="${COWORK_WORKTREE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
GITDIR="${COWORK_GITDIR:-$HOME/.cowork-gitdir/proggs}"
REMOTE_URL="${COWORK_REMOTE:-https://github.com/Pepsi1978/proggs.git}"
BRANCH="${COWORK_BRANCH:-main}"
MOUNT_CREDS="$WORKTREE/.git/credentials"
VM_CREDS="$GITDIR/cowork-credentials"
GIT_USER_NAME="${COWORK_GIT_NAME:-Pepsi1978}"
GIT_USER_EMAIL="${COWORK_GIT_EMAIL:-barwandt@gmail.com}"

log() { printf '[cowork-git] %s\n' "$1"; }
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
  g config core.fileMode false
  g config core.quotePath false

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
  g reset --mixed -q "$BRANCH"
  log "Bereit: Arbeitsbaum unveraendert, git-dir auf VM-Platte (Locks loeschbar)."
}

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

# Falle 6 (WICHTIGSTER SCHUTZ): Datenverlust verhindern. Laeuft NACH dem Staging, VOR dem Commit.
# Vergleicht je gestaged + in origin existierender Datei die Byte-Groesse origin vs. Index.
guard_data_loss() {
  local max_pct="${COWORK_SHRINK_MAX_PCT:-30}"
  local min_bytes="${COWORK_SHRINK_MIN_BYTES:-200}"
  local suspicious=0 report="" status path old new drop pct
  while IFS="$(printf '\t')" read -r status path; do
    case "$status" in
      M*)
        old=$(g cat-file -s "$BRANCH:$path" 2>/dev/null) || continue
        new=$(g cat-file -s ":0:$path" 2>/dev/null) || continue
        [ -z "$old" ] && continue
        [ -z "$new" ] && continue
        if [ "$new" -lt "$old" ]; then
          drop=$(( old - new )); pct=$(( drop * 100 / old ))
          if [ "$drop" -ge "$min_bytes" ] && [ "$pct" -ge "$max_pct" ]; then
            suspicious=$((suspicious+1))
            report="${report}
  - ${path}: ${old} -> ${new} Bytes (-${pct}%, -${drop} Bytes)"
          fi
        fi
        ;;
      D*)
        if [ -e "$WORKTREE/$path" ] || readlink "$WORKTREE/$path" >/dev/null 2>&1; then
          suspicious=$((suspicious+1))
          report="${report}
  - ${path}: als GELOESCHT gestaged, existiert aber noch im Worktree (Phantom-Loeschung)"
        fi
        ;;
    esac
  done < <(g diff --cached --name-status "$BRANCH")
  if [ "$suspicious" -gt 0 ]; then
    log "ABBRUCH (Datenverlust-Waechter): $suspicious verdaechtige Aenderung(en) — moegliche Mount-Truncation / Phantom-Loeschung:"
    printf '%b\n' "$report"
    log "NICHTS wurde committet/gepusht. Datei(en) pruefen (DATEIENDE!), ggf. per Shell neu schreiben, dann erneut."
    log "Falls Verkleinerung/Loeschung WIRKLICH gewollt: 'COWORK_ALLOW_SHRINK=1 bash cowork-git.sh ...' voranstellen."
    return 1
  fi
  log "Datenverlust-Waechter: keine verdaechtigen Schrumpfungen/Loeschungen."
  return 0
}

do_commit_and_push() {
  local msg="$1"
  if [ "${COWORK_ALLOW_SHRINK:-0}" = "1" ]; then
    log "WARNUNG: COWORK_ALLOW_SHRINK=1 — Datenverlust-Waechter uebersprungen (Schrumpfen/Loeschen erlaubt)."
  else
    guard_data_loss || exit 1
  fi
  if g diff --cached --quiet; then
    log "Nichts zu committen (Arbeitsbaum = origin/$BRANCH)."
    exit 0
  fi
  g commit -m "$msg"
  g push origin "HEAD:$BRANCH"
  log "Erfolgreich nach origin/$BRANCH gepusht."
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
    do_commit_and_push "$msg"
    ;;
  push-files)
    msg="${2:-update from cowork}"
    shift 2 2>/dev/null || true
    if [ "$#" -eq 0 ]; then
      log "FEHLER: push-files braucht mindestens eine Datei. Bsp: push-files \"#NNN - Text\" datei1 datei2"
      exit 2
    fi
    ensure_setup
    g read-tree "$BRANCH"
    guard_unreadable_symlinks
    guard_lfs_pointers
    g add -- "$@"
    do_commit_and_push "$msg"
    ;;
  "")
    log "Nutzung: bash cowork-git.sh {setup | push \"Nachricht\" | push-files \"Nachricht\" datei... | <git-befehl>}"
    exit 2
    ;;
  *)
    g "$@"
    ;;
esac
