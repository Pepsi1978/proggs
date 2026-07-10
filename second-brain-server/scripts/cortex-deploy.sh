#!/usr/bin/env bash
# cortex-deploy.sh — Deploy-Guard fuer den second-brain-Stack (Cortex).
#
# Zweck: Der Cortex-Stack wird per scp auf den VPS deployed (KEIN git auf dem Server), waehrend
# mehrere Claude-Sessions denselben ~/proggs-Arbeitsbaum teilen. Dadurch kann der LAUFENDE VPS-Stand
# einer Datei neuer sein als git, wenn eine andere Session dort etwas deployed, aber noch nicht
# committed hat. Ein blinder scp wuerde diese fremde, nur-deployte Arbeit ueberschreiben.
# Dieses Skript vergleicht pro Datei DREI Staende — VPS (laeuft) / git HEAD (committed) / lokal —
# und warnt, bevor etwas Fremdes zerstoert wird.
#
# Nutzung:
#   scripts/cortex-deploy.sh check  <datei> [<datei> ...]   # nur pruefen (Exit 0=sicher, 1=Warnung)
#   scripts/cortex-deploy.sh deploy <datei> [<datei> ...]   # pruefen; nur bei OK scp + Service-Rebuild
#   Dateien RELATIV zu second-brain-server/ angeben, z.B.: dashboard/static/index.html brain-api/app.py
#
# Env-Overrides:
#   CORTEX_VPS         Ziel-Host          (default root@168.231.83.205)
#   CORTEX_REMOTE_DIR  Ziel-Verzeichnis   (default /opt/second-brain)
#   CORTEX_FORCE=1     deploy trotz Warnung erzwingen (bewusst fremdes ueberschreiben)
#   CORTEX_NO_BUILD=1  deploy ohne Container-Rebuild (nur scp)
#
# Braucht: bash, git, ssh, scp, diff, mktemp (Git Bash auf Windows / macOS / Linux).

set -euo pipefail
# Globaler Fehler-Faenger (Observability-First): nichts stirbt still.
trap 'rc=$?; [ $rc -ne 0 ] && echo "[FEHLER] Abbruch in Zeile $LINENO (exit $rc): $BASH_COMMAND" >&2' EXIT

VPS="${CORTEX_VPS:-root@168.231.83.205}"
REMOTE_DIR="${CORTEX_REMOTE_DIR:-/opt/second-brain}"
SSH_OPTS=(-o ConnectTimeout=10 -o BatchMode=yes)
# Cortex-SSH-Key automatisch nutzen (DEPLOY.md: ~/SK/second-brain/id_ed25519) — ohne ihn
# scheiterte jedes ssh/scp mit 'Permission denied' (Windows hat keinen ~/.ssh/config-Eintrag).
CORTEX_KEY="${CORTEX_KEY:-$HOME/SK/second-brain/id_ed25519}"
[ -f "$CORTEX_KEY" ] && SSH_OPTS+=(-i "$CORTEX_KEY")

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SBROOT="$(cd "$SCRIPT_DIR/.." && pwd)"                 # second-brain-server/
PREFIX="$(git -C "$SBROOT" rev-parse --show-prefix)"   # z.B. "second-brain-server/" (repo-root-relativ)

c_red=$'\033[31m'; c_grn=$'\033[32m'; c_yel=$'\033[33m'; c_dim=$'\033[2m'; c_0=$'\033[0m'
log()  { echo "${c_dim}[cortex-deploy]${c_0} $*"; }
ok()   { echo "  ${c_grn}[OK]${c_0}   $*"; }
warn() { echo "  ${c_yel}[WARN]${c_0} $*"; }
err()  { echo "  ${c_red}[FEHL]${c_0} $*"; }

usage() { grep -E '^#( |$)' "${BASH_SOURCE[0]}" | sed -E 's/^# ?//'; exit 2; }

# CRLF-normalisierter Vergleich zweier Dateien (0 = identisch)
same() { diff -q <(tr -d '\r' < "$1") <(tr -d '\r' < "$2") >/dev/null 2>&1; }

# Prueft eine Datei; setzt globale Arrays SAFE[] / BLOCKED[]. Rueckgabe 0=deploybar, 1=blockiert.
check_file() {
  local rel="$1" repopath="${PREFIX}$1" remote="$REMOTE_DIR/$1" local_f="$SBROOT/$1"
  local tvps thead
  echo "── $rel"
  if [ ! -f "$local_f" ]; then err "lokale Datei fehlt: $local_f"; BLOCKED+=("$rel"); return 1; fi
  if ! git -C "$SBROOT" cat-file -e "HEAD:$repopath" 2>/dev/null; then
    warn "in git HEAD nicht getrackt — kann VPS-Stand nicht vergleichen (neue Datei?)"; SAFE+=("$rel"); return 0
  fi
  tvps="$(mktemp)"; thead="$(mktemp)"
  git -C "$SBROOT" show "HEAD:$repopath" > "$thead" 2>/dev/null || : > "$thead"
  # SSH-VERBINDUNGSFEHLER (rc 255) STRIKT von 'Datei fehlt' (cat rc 1) unterscheiden:
  # sonst wird ein kaputter SSH-Zugang als 'Neuanlage, kein Risiko' fehlinterpretiert
  # und der Guard laeuft ins Leere (Vorfall 2026-07-10, fehlender -i Key auf Windows).
  ssh_rc=0
  ssh "${SSH_OPTS[@]}" "$VPS" "cat '$remote' 2>/dev/null" > "$tvps" || ssh_rc=$?
  if [ "$ssh_rc" -ge 255 ]; then
    err "SSH-Verbindung fehlgeschlagen (rc=$ssh_rc) — VPS-Stand unpruefbar, NICHT deploybar"
    BLOCKED+=("$rel"); rm -f "$tvps" "$thead"; return 1
  fi
  if [ "$ssh_rc" -ne 0 ]; then
    warn "auf dem VPS (noch) nicht vorhanden -> Neuanlage, kein Ueberschreib-Risiko"; SAFE+=("$rel")
    rm -f "$tvps" "$thead"; return 0
  fi
  if same "$tvps" "$thead"; then
    if same "$local_f" "$thead"; then ok "VPS == git == lokal (nichts zu deployen)"
    else                              ok "VPS == git; lokal hat deine committeten/uncommitteten Aenderungen -> sicher deploybar"; fi
    SAFE+=("$rel"); rm -f "$tvps" "$thead"; return 0
  fi
  # VPS weicht von git HEAD ab: eine ANDERE Session hat dort deployed ohne Commit.
  warn "VPS weicht von git HEAD ab — FREMDE nur-deployte Aenderung! Ueberschreiben zerstoert sie."
  echo "${c_dim}     Diff VPS(<) vs git HEAD(>), erste Zeilen:${c_0}"
  diff <(tr -d '\r' < "$tvps") <(tr -d '\r' < "$thead") | head -12 | sed 's/^/       /'
  BLOCKED+=("$rel"); rm -f "$tvps" "$thead"; return 1
}

# Leitet den Compose-Service aus dem Dateipfad ab (fuer den Rebuild).
service_of() {
  case "$1" in
    brain-api/*)  echo brain-api ;;  dashboard/*) echo dashboard ;;
    agent/*)      echo agent ;;      librarian/*) echo librarian ;;
    mcp/*|mcp-server/*) echo mcp ;;  *) echo "" ;;
  esac
}

cmd="${1:-}"; shift || true
[ -z "$cmd" ] && usage
[ "$cmd" != "check" ] && [ "$cmd" != "deploy" ] && usage
[ $# -eq 0 ] && { err "keine Dateien angegeben"; usage; }

log "VPS=$VPS  REMOTE_DIR=$REMOTE_DIR"
# Vorpruefung: ist der lokale Branch mit origin synchron? (sonst erst rebasen/pushen)
git -C "$SBROOT" fetch origin --quiet 2>/dev/null || warn "git fetch fehlgeschlagen (offline?) — Vergleich nutzt lokalen Stand"
if [ -n "$(git -C "$SBROOT" rev-list HEAD..origin/main 2>/dev/null)" ]; then
  warn "HEAD ist HINTER origin/main — erst 'git rebase origin/main' + push, sonst deployst du veralteten Code."
fi

declare -a SAFE=() BLOCKED=()
overall=0
for f in "$@"; do check_file "$f" || overall=1; done

echo
log "Ergebnis: ${#SAFE[@]} sicher, ${#BLOCKED[@]} blockiert."

if [ "$cmd" = "check" ]; then exit $overall; fi

# deploy-Modus
if [ ${#BLOCKED[@]} -gt 0 ] && [ "${CORTEX_FORCE:-0}" != "1" ]; then
  err "Deploy ABGEBROCHEN — ${#BLOCKED[@]} Datei(en) wuerden fremde Arbeit ueberschreiben: ${BLOCKED[*]}"
  err "Erst mit der anderen Session klaeren (deren Aenderung committen lassen) ODER CORTEX_FORCE=1 setzen (bewusst)."
  exit 1
fi
[ ${#BLOCKED[@]} -gt 0 ] && warn "CORTEX_FORCE=1 — ueberschreibe bewusst: ${BLOCKED[*]}"

declare -a services=()
for f in "$@"; do
  log "scp $f -> $VPS:$REMOTE_DIR/$f"
  scp "${SSH_OPTS[@]}" -q "$SBROOT/$f" "$VPS:$REMOTE_DIR/$f"
  svc="$(service_of "$f")"; [ -n "$svc" ] && services+=("$svc")
done

if [ "${CORTEX_NO_BUILD:-0}" = "1" ]; then log "CORTEX_NO_BUILD=1 — kein Rebuild."; trap - EXIT; exit 0; fi
# betroffene Services dedupliziert rebuilden
uniq_services="$(printf '%s\n' "${services[@]:-}" | sort -u | grep -v '^$' || true)"
if [ -n "$uniq_services" ]; then
  for svc in $uniq_services; do
    log "rebuild + restart: $svc"
    ssh "${SSH_OPTS[@]}" "$VPS" "cd '$REMOTE_DIR' && docker compose up -d --build --no-deps $svc" 2>&1 | tail -4
  done
else
  warn "kein Compose-Service aus den Pfaden ableitbar — bitte manuell rebuilden."
fi
log "Deploy fertig."
trap - EXIT
