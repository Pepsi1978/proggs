#!/usr/bin/env bash
#
# cowork-git.sh — zuverlaessiger git commit/push aus der Claude-Cowork-Linux-VM (Windows).
#
# GRUNDPROBLEM:
#   Cowork arbeitet ueber eine gemountete virtiofs/FUSE-Bruecke zu einer Linux-VM. Die Bruecke ist
#   beim Lesen/Schreiben kuerzlich geaenderter Dateien unzuverlaessig (Truncation, Padding, Flackern)
#   und erlaubt kein Loeschen aus der VM. Deshalb liegt der git-Maschinenraum (Index/Locks/Objekte)
#   auf der VM-ext4-Platte (~/.cowork-gitdir/proggs); nur der Arbeitsbaum bleibt auf der Bruecke.
#   Wahrheit ist origin/main.
#
# MOUNT-FALLEN, die dieses Skript abfaengt:
#   1) .lock nicht loeschbar              -> git-dir auf VM-ext4.
#   2) Datei-Modus immer 0755             -> core.fileMode=false.
#   3) Symlinks nicht lesbar (I/O)        -> guard_unreadable_symlinks (skip-worktree).
#   4) Git-LFS-Dateien als Vollinhalt     -> guard_lfs_pointers (skip-worktree); sonst >100 MB -> Reject.
#   5) Build-/Abhaengigkeits-Berge        -> **/build/ **/.gradle/ **/node_modules/ in .gitignore.
#   6) DATENVERLUST durch Mount-Lesefehler-> guard_data_loss: ABBRUCH vor Commit bei verdaechtiger
#      Schrumpfung/Phantom-Loeschung. Override: COWORK_ALLOW_SHRINK=1.
#   7) Mount-Schreib-/Lese-Flackern       -> Commits git-intern bauen; Skript aus stabiler VM-Kopie
#      via COWORK_WORKTREE ausfuehrbar.
#   8) origin/main wandert (Non-Fast-Forward) -> do_push_with_retry setzt den eigenen Commit per
#      git-internem 3-Wege-Merge (KEIN Worktree, KEIN Force) sauber auf den neuen Stand auf und pusht
#      erneut, bis COWORK_PUSH_RETRIES (Default 3). Fremde Aenderungen bleiben erhalten; bei echtem
#      Konflikt (gleiche Datei beidseitig geaendert) sauberer Abbruch statt Raten.
#
# ROBUSTHEIT/PERFORMANCE (ensure_setup):
#   core.fileMode/quotePath false; core.preloadIndex true; index.version 4; index.skipHash true
#   (wirkt ab Git 2.40); gc.auto 0 (kein blockierendes Auto-GC ueber die Bruecke);
#   safe.directory fuer git-dir UND work-tree (gegen "dubious ownership"); GIT_TERMINAL_PROMPT=0
#   (kein Haengen an Auth-Prompts). fsmonitor/untrackedCache bewusst AUS (auf FUSE unzuverlaessig).
#
# NUTZUNG (in Cowork):
#   bash cowork-git.sh setup                          # Stand holen, Push-Zugang pruefen
#   bash cowork-git.sh push "#NNN - Text"             # add -A + Waechter + commit + push (+ Retry)
#   bash cowork-git.sh push-files "#NNN - Text" a b   # NUR Dateien a,b (gezielt) + Waechter (+ Retry)
#   bash cowork-git.sh <git-befehl>                   # beliebiger git-Befehl (status, log, diff ...)
#
#   COWORK_WORKTREE / COWORK_GITDIR / COWORK_REMOTE / COWORK_BRANCH -> Pfade/Ziele ueberschreiben.
#   COWORK_ALLOW_SHRINK=1   -> Waechter erlaubt absichtliches Schrumpfen/Loeschen.
#   COWORK_SHRINK_MAX_PCT (30) / COWORK_SHRINK_MIN_BYTES (200) -> Empfindlichkeit des Waechters.
#   COWORK_PUSH_RETRIES (3) -> max. Versuche bei Non-Fast-Forward.
#
# VORAUSSETZUNG: Push-Token persistent in <repo>/.git/credentials (siehe COWORK-GIT-PUSH-SETUP.md).
#
set -euo pipefail
export GIT_TERMINAL_PROMPT=0   # nicht-interaktiv: bei fehlendem Token sofort abbrechen statt haengen

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
  # Performance ueber die langsame Bruecke (KEIN untrackedCache/fsmonitor — auf FUSE unzuverlaessig):
  g config core.preloadIndex true
  g config index.version 4
  g config index.skipHash true     # wirkt ab Git 2.40; auf aelteren harmlos ignoriert
  g config gc.auto 0               # kein blockierendes Auto-GC ueber die Bruecke
  # "dubious ownership" vermeiden (Mount-Owner != VM-Nutzer) — dedupliziert eintragen:
  local d
  for d in "$WORKTREE" "$GITDIR"; do
    git config --global --get-all safe.directory 2>/dev/null | grep -qxF "$d" \
      || git config --global --add safe.directory "$d"
  done

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

# Falle 6 (WICHTIGSTER SCHUTZ): Datenverlust verhindern. NACH dem Staging, VOR dem Commit.
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

# 3-Wege-Merge OHNE Worktree: setzt Commit <theirs> per merge-base <base> auf <ours> auf und gibt
# die neue Commit-OID aus. Erhaelt fremde Aenderungen in <ours>; bei echtem Konflikt Exit != 0.
reparent_onto() {
  local base="$1" ours="$2" theirs="$3" msg="$4" tmpd tidx mt new
  tmpd="$(mktemp -d "${TMPDIR:-/tmp}/cowork-midx.XXXXXX")"
  tidx="$tmpd/index"   # darf NICHT existieren -> git legt frischen Index an (sonst "index file smaller than expected")
  if GIT_INDEX_FILE="$tidx" g read-tree -i -m --aggressive "$base" "$ours" "$theirs" 2>/dev/null \
     && mt=$(GIT_INDEX_FILE="$tidx" g write-tree 2>/dev/null); then
    rm -rf "$tmpd"
    new=$(g commit-tree "$mt" -p "$ours" -m "$msg") || return 1
    printf '%s\n' "$new"; return 0
  fi
  rm -rf "$tmpd"; return 1
}

# Stagen (modusabhaengig) -> Waechter -> commit -> push. Bei Non-Fast-Forward wird der eigene Commit
# git-intern (3-Wege-Merge, kein Worktree, kein Force) auf den neuen origin-Stand gesetzt und erneut
# gepusht, bis COWORK_PUSH_RETRIES Versuche.
do_push_with_retry() {
  local msg="$1" mode="$2"; shift 2
  local files=("$@")
  local max="${COWORK_PUSH_RETRIES:-3}" attempt=1 out base0 c1 t1 c2
  guard_unreadable_symlinks
  guard_lfs_pointers
  if [ "$mode" = "all" ]; then g add -A; else g add -- "${files[@]}"; fi
  if [ "${COWORK_ALLOW_SHRINK:-0}" = "1" ]; then
    log "WARNUNG: COWORK_ALLOW_SHRINK=1 — Datenverlust-Waechter uebersprungen (Schrumpfen/Loeschen erlaubt)."
  else
    guard_data_loss || exit 1
  fi
  if g diff --cached --quiet; then
    log "Nichts zu committen (Arbeitsbaum = origin/$BRANCH)."
    exit 0
  fi
  base0="$(g rev-parse "$BRANCH")"
  g commit -q -m "$msg"
  c1="$(g rev-parse HEAD)"
  while : ; do
    if out=$(g push origin "HEAD:$BRANCH" 2>&1); then
      printf '%s\n' "$out"
      log "Erfolgreich nach origin/$BRANCH gepusht (Versuch $attempt)."
      return 0
    fi
    printf '%s\n' "$out"
    if printf '%s' "$out" | grep -qiE 'fetch first|non-fast-forward|rejected|cannot lock ref|failed to update ref|updates were rejected|stale info'; then
      if [ "$attempt" -ge "$max" ]; then
        log "FEHLER: Push nach $attempt Versuchen weiter abgelehnt — origin wandert zu schnell. Spaeter erneut."
        return 1
      fi
      log "Non-Fast-Forward (Versuch $attempt/$max): origin weitergewandert — Commit git-intern (3-Wege-Merge) oben aufsetzen."
      attempt=$((attempt+1))
      g fetch -q origin "$BRANCH"
      t1="$(g rev-parse FETCH_HEAD)"
      if c2=$(reparent_onto "$base0" "$t1" "$c1" "$msg"); then
        g update-ref "refs/heads/$BRANCH" "$c2"
        g symbolic-ref HEAD "refs/heads/$BRANCH"
        base0="$t1"; c1="$c2"
        continue
      else
        log "FEHLER: echter Merge-Konflikt mit dem neuen origin/$BRANCH (gleiche Datei beidseitig geaendert). Bitte manuell aufloesen."
        return 1
      fi
    fi
    log "FEHLER beim Push (kein Non-Fast-Forward) — bitte Ausgabe oben pruefen."
    return 1
  done
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
    do_push_with_retry "$msg" all
    ;;
  push-files)
    msg="${2:-update from cowork}"
    shift 2 2>/dev/null || true
    if [ "$#" -eq 0 ]; then
      log "FEHLER: push-files braucht mindestens eine Datei. Bsp: push-files \"#NNN - Text\" datei1 datei2"
      exit 2
    fi
    ensure_setup
    do_push_with_retry "$msg" files "$@"
    ;;
  "")
    log "Nutzung: bash cowork-git.sh {setup | push \"Nachricht\" | push-files \"Nachricht\" datei... | <git-befehl>}"
    exit 2
    ;;
  *)
    g "$@"
    ;;
esac
