#!/usr/bin/env bash
# cortex-copy.sh — Schnelles Kopieren zum/vom Cortex-Server (10.8.0.1) ueber WireGuard.
#
# WARUM DIESES SKRIPT (gemessen 27.08.2026, siehe bugs/server/samba-wireguard.md §12):
#   Finder/SMB kopiert SERIELL. Bei ~50 ms Latenz zum VPS (Paris) wartet jeder einzelne
#   SMB-Round-Trip die volle Laufzeit ab -> nur ~5 Mbit/s von ~60 Mbit/s Uplink (9 %).
#   rclone kopiert mit mehreren Streams GLEICHZEITIG -> gemessen 42,6 Mbit/s (71 %).
#   Das ist Faktor 8 — ohne dass sich an Leitung, Tunnel oder Server irgendetwas aendert.
#
# BENUTZUNG:
#   cortex-copy.sh push <lokaler-pfad> <ziel-auf-server>     # hochladen (kopieren)
#   cortex-copy.sh pull <quelle-auf-server> <lokaler-pfad>   # herunterladen
#   cortex-copy.sh sync <lokaler-pfad> <ziel-auf-server>     # abgleichen (Ziel = Quelle!)
#   cortex-copy.sh ls   <pfad-auf-server>                    # auflisten
#   cortex-copy.sh bench                                     # Durchsatz messen
#
#   Server-Pfade beginnen mit dem Laufwerk: daten:... oder gedanken:...
#   Beispiele:
#     cortex-copy.sh push ~/Filme daten:Filme
#     cortex-copy.sh pull gedanken:Notizen ~/Notizen
#     cortex-copy.sh sync ~/Projekte daten:Backup/Projekte
#
# ACHTUNG bei "sync": macht das Ziel zur exakten Kopie der Quelle — loescht dort also
#   Dateien, die es in der Quelle nicht (mehr) gibt. Das Skript fragt vorher nach.

set -uo pipefail

LOG_DIR="$HOME/Library/Logs/cortex-copy"
LOG="$LOG_DIR/cortex-copy.log"
mkdir -p "$LOG_DIR"

# --- Beobachtungsschicht (observability-first.md): jeder Lauf wird protokolliert -------------
log() {
  local lvl="$1"; shift
  printf '{"ts":"%s","level":"%s","module":"cortex-copy","msg":%s}\n' \
    "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$lvl" "$(printf '%s' "$*" | sed 's/\\/\\\\/g; s/"/\\"/g; s/^/"/; s/$/"/')" >> "$LOG"
}
die() { printf '\n❌ %s\n' "$*" >&2; log error "$*"; exit 1; }

echo "Protokoll: $LOG"

# --- Vorbedingungen pruefen (Sonden statt stiller Fehlschlaege) ------------------------------
command -v rclone >/dev/null 2>&1 || die "rclone fehlt. Installieren mit:  brew install rclone"

RC_CONF="$HOME/.config/rclone/rclone.conf"

# Selbstheilung (Direktive #3): fehlen die Remotes, werden sie aus dem SK-Ordner angelegt —
# statt nur zu meckern. Zugangsdaten kommen NUR aus ~/SK (secrets-in-sk-folder.md), nie aus dem Repo.
if ! grep -q '^\[cortex-daten\]' "$RC_CONF" 2>/dev/null; then
  SK_ENV="$HOME/SK/second-brain/samba.env"
  [ -f "$SK_ENV" ] || die "rclone-Remotes fehlen UND $SK_ENV ist nicht da. Ohne Zugangsdaten geht nichts."
  SAMBA_USER="$(grep '^SAMBA_USER=' "$SK_ENV" | cut -d= -f2-)"
  SAMBA_PASS="$(grep '^SAMBA_PASS=' "$SK_ENV" | cut -d= -f2-)"
  [ -n "$SAMBA_USER" ] && [ -n "$SAMBA_PASS" ] || die "SAMBA_USER/SAMBA_PASS fehlen in $SK_ENV"
  echo "ℹ️  rclone-Remotes fehlen — lege sie aus $SK_ENV an..."
  mkdir -p "$(dirname "$RC_CONF")"
  [ -f "$RC_CONF" ] && cp "$RC_CONF" "$RC_CONF.bak-$(date +%Y%m%d-%H%M%S)"
  OBS="$(rclone obscure "$SAMBA_PASS")"
  for SHARE in daten gedanken; do
    cat >> "$RC_CONF" <<EOF

[cortex-$SHARE]
type = smb
host = 10.8.0.1
user = $SAMBA_USER
pass = $OBS
share = $SHARE
EOF
  done
  chmod 600 "$RC_CONF"
  unset SAMBA_PASS OBS
  echo "✅ Remotes cortex-daten und cortex-gedanken angelegt."
  log info "rclone-Remotes automatisch aus SK angelegt"
fi

# Tunnel lebendig? Ohne WireGuard ist alles andere sinnlos — lieber klar melden als lange haengen.
if ! ping -c 2 -W 2000 10.8.0.1 >/dev/null 2>&1; then
  die "Server 10.8.0.1 antwortet nicht. Laeuft der WireGuard-Tunnel?  (macos/wireguard-up.sh)"
fi

# --- Optimale Parameter (gemessen, nicht geraten) --------------------------------------------
# transfers 8:            8 Dateien gleichzeitig. 16 brachte nichts mehr (39 statt 43 Mbit/s)
#                         -> die Leitung ist ab ~8 Streams gesaettigt.
# multi-thread-streams 8: teilt EINE grosse Datei auf 8 Streams (sonst nur ~25 Mbit/s).
# buffer-size 32M:        deckt das Bandbreiten-Verzoegerungs-Produkt ab (60 Mbit/s x 50 ms).
# --local-no-set-modtime: spart einen SMB-Round-Trip pro Datei (bei ~50 ms spuerbar).
RCLONE_OPTS=(
  --transfers 8
  --checkers 8
  --multi-thread-streams 8
  --multi-thread-cutoff 16M
  --buffer-size 32M
  --smb-idle-timeout 5m
  --retries 5
  --low-level-retries 20
  --stats 5s
  --progress
  --log-file "$LOG"
  --log-level INFO
)

# --- Server-Pfad (daten:Unterordner) in ein rclone-Remote uebersetzen ------------------------
zu_remote() {
  local p="$1"
  case "$p" in
    daten:*)    printf 'cortex-daten:daten/%s'       "${p#daten:}" ;;
    gedanken:*) printf 'cortex-gedanken:gedanken/%s' "${p#gedanken:}" ;;
    *) die "Server-Pfad muss mit 'daten:' oder 'gedanken:' beginnen — bekommen: '$p'" ;;
  esac
}

BEFEHL="${1:-}"; shift || true

case "$BEFEHL" in
  push)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh push <lokaler-pfad> <daten:ziel>"
    QUELLE="$1"; ZIEL="$(zu_remote "$2")"
    [ -e "$QUELLE" ] || die "Lokaler Pfad existiert nicht: $QUELLE"
    log info "push start: $QUELLE -> $ZIEL"
    echo "⬆️  Hochladen: $QUELLE  ->  $2"
    rclone copy "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}" --local-no-set-modtime
    RC=$?; log info "push ende rc=$RC"
    [ $RC -eq 0 ] && echo "✅ Fertig." || die "Fehlgeschlagen (Code $RC) — Details im Protokoll."
    ;;

  pull)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh pull <daten:quelle> <lokaler-pfad>"
    QUELLE="$(zu_remote "$1")"; ZIEL="$2"
    log info "pull start: $QUELLE -> $ZIEL"
    echo "⬇️  Herunterladen: $1  ->  $ZIEL"
    rclone copy "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}"
    RC=$?; log info "pull ende rc=$RC"
    [ $RC -eq 0 ] && echo "✅ Fertig." || die "Fehlgeschlagen (Code $RC) — Details im Protokoll."
    ;;

  sync)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh sync <lokaler-pfad> <daten:ziel>"
    QUELLE="$1"; ZIEL="$(zu_remote "$2")"
    [ -e "$QUELLE" ] || die "Lokaler Pfad existiert nicht: $QUELLE"
    echo
    echo "⚠️  ABGLEICH macht '$2' zur exakten Kopie von '$QUELLE'."
    echo "    Dateien, die dort liegen aber nicht in der Quelle, werden GELOESCHT."
    echo
    echo "    Zuerst der Trockenlauf — es wird noch nichts veraendert:"
    rclone sync "$QUELLE" "$ZIEL" --dry-run --transfers 8 --checkers 8 2>&1 | tail -40
    echo
    read -r -p "Wirklich so ausfuehren? (tippe JA) " ANTWORT
    [ "$ANTWORT" = "JA" ] || { echo "Abgebrochen — nichts veraendert."; log info "sync abgebrochen"; exit 0; }
    log info "sync start: $QUELLE -> $ZIEL"
    rclone sync "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}" --local-no-set-modtime
    RC=$?; log info "sync ende rc=$RC"
    [ $RC -eq 0 ] && echo "✅ Abgleich fertig." || die "Fehlgeschlagen (Code $RC)"
    ;;

  ls)
    [ $# -eq 1 ] || die "Aufruf: cortex-copy.sh ls <daten:pfad>"
    rclone lsl "$(zu_remote "$1")" --max-depth 1
    ;;

  bench)
    echo "=== Durchsatzmessung zum Cortex-Server ==="
    TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
    echo "Erzeuge 6 x 8 MB Testdaten..."
    for i in 1 2 3 4 5 6; do dd if=/dev/urandom of="$TMP/t$i.bin" bs=1m count=8 2>/dev/null; done
    echo "Lade 48 MB hoch (8 parallele Streams)..."
    S=$(date +%s)
    rclone copy "$TMP" cortex-daten:daten/__bench --transfers 8 --checkers 8 \
      --multi-thread-streams 8 --buffer-size 32M --stats-one-line --stats 5s
    E=$(date +%s); D=$((E-S)); [ "$D" -lt 1 ] && D=1
    echo
    echo "  Durchsatz: $(echo "scale=2; 48/$D" | bc) MB/s  =  $(echo "scale=1; 384/$D" | bc) Mbit/s"
    echo "  (Zum Vergleich: Finder/SMB schafft hier typisch ~0,7 MB/s = 5 Mbit/s)"
    rclone purge cortex-daten:daten/__bench 2>/dev/null
    log info "bench: ${D}s fuer 48 MB"
    ;;

  *)
    sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
