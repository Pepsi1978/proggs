#!/usr/bin/env bash
# cortex-copy.sh — Schnelles Kopieren zum/vom Cortex-Server (10.8.0.1) ueber WireGuard.
#
# Version 2.0.0 (27.08.2026, 21:25 Uhr) — wird bei jedem Lauf ins Protokoll geschrieben.
#
# WARUM DIESES SKRIPT (gemessen 27.08.2026, siehe bugs/server/samba-wireguard.md §14):
#   Der Engpass zum VPS in Paris ist die LATENZ (rund 48 ms), nicht die Bandbreite.
#   Finder/SMB arbeitet pro Datei seriell und wartet jeden Round-Trip voll ab:
#     - VIELE KLEINE Dateien (200 x 50 KB): Finder 1,5 Dateien/s und brach mit Timeouts ab;
#       rclone mit 64 gleichzeitigen Uebertragungen 29,8 Dateien/s -> rund Faktor 20.
#     - WENIGE GROSSE Dateien: hier hilft Parallelitaet ueber DATEIEN, nicht innerhalb einer.
#   Das Skript waehlt das passende Profil automatisch anhand der mittleren Dateigroesse.
#
# ZWEI TRANSPORTWEGE (neu 27.08.2026, alles serverseitig gegengeprueft):
#   Nachmessung mit freier Leitung (Uplink 54,8 Mbit/s, roher SSH-Durchsatz durch den Tunnel
#   44 Mbit/s = das erreichbare Maximum) zeigte, dass SMB und SFTP genau gegenlaeufig sind:
#
#     | Fall            | SMB              | SFTP             |
#     |-----------------|------------------|------------------|
#     | 60 x 50 KB      | 18,7 Dateien/s   |  4,4 Dateien/s   |
#     | 1 x 48 MB       | 10,2 Mbit/s      | 37,1 Mbit/s      |
#
#   Eine EINZELNE grosse Datei bleibt ueber SMB bei rund 10 Mbit/s — `--multi-thread-streams`
#   aendert daran nichts, weil der SMB-Backend die Streams nicht wirklich parallel fuehrt.
#   Ueber SFTP laeuft dieselbe Datei mit 37 Mbit/s, also nahe am Tunnel-Maximum (Faktor 3,6).
#   Umgekehrt kostet SFTP pro Datei viel mehr Vorlauf -> bei vielen kleinen Dateien ist SMB
#   klar vorn. Deshalb waehlt das Skript jetzt AUCH den Transportweg automatisch:
#     Schnitt < 2 MB  -> SMB  (64 gleichzeitige Uebertragungen)
#     Schnitt >= 2 MB -> SFTP (ueber den SSH-Schluessel aus ~/SK/second-brain/id_ed25519)
#   Nach jedem SFTP-Transfer wird der Besitzer auf dem Server wieder auf frank:frank gesetzt —
#   SSH laeuft als root, und ohne das koennte man die Dateien ueber den SMB-Mount nicht mehr
#   aendern. Fehlt der SSH-Schluessel oder antwortet SSH nicht, faellt alles auf SMB zurueck
#   (nichts geht verloren, es wird nur langsamer).
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
log info "cortex-copy 2.0.0 gestartet"

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

# --- SFTP-Transportweg: verfuegbar? ---------------------------------------------------------
# Schluessel und Server-Basispfad kommen aus ~/SK (secrets-in-sk-folder.md), nie aus dem Repo.
SSH_KEY="$HOME/SK/second-brain/id_ed25519"
SSH_USER="root"
SFTP_BASIS="/srv/samba"          # dort liegen die Freigaben daten/ und gedanken/
SHARE_BESITZER="frank:frank"     # uid 1000 — so gehoeren die Dateien auch ueber SMB wieder Frank
# Die Pruefung laeuft ERST, wenn der SFTP-Weg wirklich gebraucht wird (bei vielen kleinen
# Dateien kostet ein SSH-Handshake bei 48 ms Latenz mehr, als er dort nutzen wuerde).
# Das Ergebnis wird gemerkt, damit pro Lauf hoechstens ein Handshake anfaellt.
SFTP_MOEGLICH=-1   # -1 = noch nicht geprueft, 0 = nein, 1 = ja
sftp_verfuegbar() {
  [ "$SFTP_MOEGLICH" -ge 0 ] && return $(( 1 - SFTP_MOEGLICH ))
  if [ -f "$SSH_KEY" ] && ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=6 \
       -o StrictHostKeyChecking=accept-new "$SSH_USER@10.8.0.1" true >/dev/null 2>&1; then
    SFTP_MOEGLICH=1; return 0
  fi
  SFTP_MOEGLICH=0
  log warn "SFTP-Weg nicht verfuegbar (Schluessel fehlt oder SSH antwortet nicht) — nutze SMB."
  return 1
}

# rclone-Parameter fuer den SFTP-Weg (inline, damit nichts in der rclone-Konfig stehen muss).
# Als ARRAY, nicht als Funktion mit Wortaufspaltung — der Schluesselpfad darf Leerzeichen haben.
SFTP_FLAGS=(--sftp-host 10.8.0.1 --sftp-user "$SSH_USER" --sftp-key-file "$SSH_KEY"
            --sftp-concurrency 64 --sftp-set-modtime=false)

# Nach einem SFTP-Transfer den Besitzer richtigstellen (SSH laeuft als root).
sftp_besitzer_richten() {
  local serverpfad="$1"
  ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=6 "$SSH_USER@10.8.0.1" \
    "chown -R $SHARE_BESITZER $(printf '%q' "$serverpfad") 2>/dev/null" >/dev/null 2>&1 \
    && log info "Besitzer auf $SHARE_BESITZER gesetzt: $serverpfad" \
    || log warn "Besitzer konnte nicht gesetzt werden: $serverpfad (Dateien gehoeren dann root)"
}

# --- Parameter nach Dateigroesse waehlen (alles gemessen, nicht geraten) ---------------------
#
# Der Engpass ist die LATENZ (rund 48 ms), nicht die Bandbreite. Daraus folgen zwei voellig
# verschiedene Faelle — deshalb waehlt das Skript das Profil automatisch:
#
#   VIELE KLEINE Dateien: jede Datei kostet mehrere Round-Trips. Hier hilft nur, sehr viele
#     Dateien GLEICHZEITIG zu uebertragen. Gemessen (200 Dateien x 50 KB):
#       Finder/SMB 1,5 Dateien/s (und brach mit Timeouts ab)  |  rclone  8 parallel:  6,2/s
#       rclone 32 parallel: 21,0/s  |  48: 26,2/s  |  **64: 29,8/s**  -> Faktor 20 zum Finder.
#   WENIGE GROSSE Dateien: hier ist die Leitung schon mit wenigen Streams voll. Gemessen
#     (24 MB, leere Leitung): Finder/SMB 33,1 Mbit/s vs. rclone 36,4 Mbit/s — kaum Unterschied.
#     Wichtig ist hier `--multi-thread-streams`, sonst bleibt EINE grosse Datei bei rund 25 Mbit/s.
#
# buffer-size 32M: deckt das Bandbreiten-Verzoegerungs-Produkt ab (60 Mbit/s x 48 ms).
# --local-no-set-modtime: spart einen SMB-Round-Trip pro Datei (bei 48 ms spuerbar).

# Durchschnittliche Dateigroesse eines lokalen Pfades ermitteln (in KB; 0 = unbekannt).
mittlere_groesse_kb() {
  local pfad="$1"
  [ -e "$pfad" ] || { echo 0; return; }
  if [ -f "$pfad" ]; then echo $(( $(stat -f%z "$pfad") / 1024 )); return; fi
  # Ordner: Gesamtgroesse und Anzahl in EINEM Durchlauf (spart Zeit bei vielen Dateien)
  local summe anzahl
  read -r summe anzahl < <(find "$pfad" -type f -exec stat -f%z {} + 2>/dev/null \
    | awk '{s+=$1; n++} END {print (n?s:0), (n?n:0)}')
  [ "${anzahl:-0}" -gt 0 ] 2>/dev/null || { echo 0; return; }
  echo $(( summe / anzahl / 1024 ))
}

# Setzt RCLONE_OPTS passend zur mittleren Dateigroesse.
waehle_profil() {
  local kb="$1" quelle="${2:-}"
  local basis=(
    --buffer-size 32M
    --smb-idle-timeout 5m
    --retries 5
    --low-level-retries 20
    --stats 5s
    --progress
    --log-file "$LOG"
    --log-level INFO
  )
  if [ "$kb" -gt 0 ] && [ "$kb" -lt 2048 ]; then
    # unter 2 MB im Schnitt -> latenzgebunden -> massiv parallel, und SMB ist hier klar vorn
    # (gemessen 60 x 50 KB: SMB 18,7 Dateien/s vs. SFTP 4,4 Dateien/s)
    TRANSPORT="smb"
    PROFIL="viele kleine Dateien (Schnitt ${kb} KB) -> SMB, 64 gleichzeitig"
    RCLONE_OPTS=(--transfers 64 --checkers 64 "${basis[@]}")
  else
    # grosse Dateien -> SFTP, sonst haengt EINE Datei ueber SMB bei rund 10 Mbit/s fest
    # (gemessen 1 x 48 MB: SMB 10,2 Mbit/s vs. SFTP 37,1 Mbit/s bei 44 Mbit/s Tunnel-Maximum)
    if sftp_verfuegbar; then
      TRANSPORT="sftp"
      if [ "$kb" -gt 0 ]; then PROFIL="grosse Dateien (Schnitt $((kb/1024)) MB) -> SFTP, 4 gleichzeitig"
      else PROFIL="Standard -> SFTP, 4 gleichzeitig"; fi
      RCLONE_OPTS=(--transfers 4 --checkers 8 "${SFTP_FLAGS[@]}" "${basis[@]}")
    else
      TRANSPORT="smb"
      if [ "$kb" -gt 0 ]; then PROFIL="grosse Dateien (Schnitt $((kb/1024)) MB) -> SMB (SFTP nicht verfuegbar)"
      else PROFIL="Standard -> SMB (SFTP nicht verfuegbar)"; fi
      RCLONE_OPTS=(--transfers 8 --checkers 8 --multi-thread-streams 8 --multi-thread-cutoff 16M "${basis[@]}")
    fi
  fi
  echo "⚙️  Profil: $PROFIL"
  log info "profil: $PROFIL"
}

RCLONE_OPTS=()   # wird pro Befehl von waehle_profil gesetzt
TRANSPORT="smb"  # ebenso: "smb" oder "sftp"

# --- Server-Pfad (daten:Unterordner) in ein rclone-Remote uebersetzen ------------------------
zu_remote() {
  local p="$1"
  case "$p" in
    daten:*)    printf 'cortex-daten:daten/%s'       "${p#daten:}" ;;
    gedanken:*) printf 'cortex-gedanken:gedanken/%s' "${p#gedanken:}" ;;
    *) die "Server-Pfad muss mit 'daten:' oder 'gedanken:' beginnen — bekommen: '$p'" ;;
  esac
}

# Derselbe Ort, aber als echter Pfad auf dem Server (fuer den SFTP-Weg).
zu_serverpfad() {
  local p="$1"
  case "$p" in
    daten:*)    printf '%s/daten/%s'    "$SFTP_BASIS" "${p#daten:}" ;;
    gedanken:*) printf '%s/gedanken/%s' "$SFTP_BASIS" "${p#gedanken:}" ;;
    *) die "Server-Pfad muss mit 'daten:' oder 'gedanken:' beginnen — bekommen: '$p'" ;;
  esac
}

# Liefert das Ziel/die Quelle passend zum gewaehlten Transportweg.
# Muss NACH waehle_profil aufgerufen werden (dort wird TRANSPORT gesetzt).
fuer_transport() {
  local p="$1"
  if [ "$TRANSPORT" = "sftp" ]; then printf ':sftp:%s' "$(zu_serverpfad "$p")"
  else zu_remote "$p"; fi
}

BEFEHL="${1:-}"; shift || true

case "$BEFEHL" in
  push)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh push <lokaler-pfad> <daten:ziel>"
    QUELLE="$1"
    [ -e "$QUELLE" ] || die "Lokaler Pfad existiert nicht: $QUELLE"
    echo "⬆️  Hochladen: $QUELLE  ->  $2"
    waehle_profil "$(mittlere_groesse_kb "$QUELLE")"   # setzt RCLONE_OPTS UND TRANSPORT
    ZIEL="$(fuer_transport "$2")"
    log info "push start: $QUELLE -> $ZIEL (transport=$TRANSPORT)"
    rclone copy "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}" --local-no-set-modtime
    RC=$?
    # Ueber SFTP schreibt root — Besitzer zurueckdrehen, sonst gehoeren die Dateien am
    # SMB-Mount niemandem, den Frank aendern darf.
    [ $RC -eq 0 ] && [ "$TRANSPORT" = "sftp" ] && sftp_besitzer_richten "$(zu_serverpfad "$2")"
    log info "push ende rc=$RC"
    [ $RC -eq 0 ] && echo "✅ Fertig." || die "Fehlgeschlagen (Code $RC) — Details im Protokoll."
    ;;

  pull)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh pull <daten:quelle> <lokaler-pfad>"
    ZIEL="$2"
    echo "⬇️  Herunterladen: $1  ->  $ZIEL"
    # Groesse der Gegenseite erfragen, um dasselbe Profil wie beim Hochladen zu waehlen.
    # Die Abfrage laeuft immer ueber SMB — sie ist winzig, und TRANSPORT steht hier noch nicht fest.
    GR_KB=$(rclone size "$(zu_remote "$1")" --json 2>/dev/null \
      | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); print(d['bytes']//d['count']//1024 if d.get('count') else 0)
except Exception: print(0)" 2>/dev/null || echo 0)
    waehle_profil "${GR_KB:-0}"
    QUELLE="$(fuer_transport "$1")"
    log info "pull start: $QUELLE -> $ZIEL (transport=$TRANSPORT)"
    rclone copy "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}"
    RC=$?; log info "pull ende rc=$RC"
    [ $RC -eq 0 ] && echo "✅ Fertig." || die "Fehlgeschlagen (Code $RC) — Details im Protokoll."
    ;;

  sync)
    [ $# -eq 2 ] || die "Aufruf: cortex-copy.sh sync <lokaler-pfad> <daten:ziel>"
    QUELLE="$1"
    [ -e "$QUELLE" ] || die "Lokaler Pfad existiert nicht: $QUELLE"
    waehle_profil "$(mittlere_groesse_kb "$QUELLE")"   # setzt RCLONE_OPTS UND TRANSPORT
    ZIEL="$(fuer_transport "$2")"
    echo
    echo "⚠️  ABGLEICH macht '$2' zur exakten Kopie von '$QUELLE'."
    echo "    Dateien, die dort liegen aber nicht in der Quelle, werden GELOESCHT."
    echo
    TROCKEN_EXTRA=(); [ "$TRANSPORT" = "sftp" ] && TROCKEN_EXTRA=("${SFTP_FLAGS[@]}")
    echo "    Zuerst der Trockenlauf — es wird noch nichts veraendert:"
    rclone sync "$QUELLE" "$ZIEL" --dry-run --transfers 8 --checkers 8 "${TROCKEN_EXTRA[@]}" 2>&1 | tail -40
    echo
    read -r -p "Wirklich so ausfuehren? (tippe JA) " ANTWORT
    [ "$ANTWORT" = "JA" ] || { echo "Abgebrochen — nichts veraendert."; log info "sync abgebrochen"; exit 0; }
    log info "sync start: $QUELLE -> $ZIEL (transport=$TRANSPORT)"
    rclone sync "$QUELLE" "$ZIEL" "${RCLONE_OPTS[@]}" --local-no-set-modtime
    RC=$?
    [ $RC -eq 0 ] && [ "$TRANSPORT" = "sftp" ] && sftp_besitzer_richten "$(zu_serverpfad "$2")"
    log info "sync ende rc=$RC"
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

    messe() {   # messe <name> <ziel-rclone-pfad> <extra-flags...>
      local name="$1" ziel="$2"; shift 2
      local s e
      s=$(python3 -c 'import time;print(time.time())')
      rclone copy "$TMP" "$ziel" --transfers 8 --checkers 8 --buffer-size 32M "$@" >/dev/null 2>&1
      e=$(python3 -c 'import time;print(time.time())')
      python3 -c "print(f'  {\"$name\":<28} {48*8/($e-$s):5.1f} Mbit/s')"
    }

    echo "Lade 48 MB ueber SMB hoch..."
    messe "SMB (6 x 8 MB)" "cortex-daten:daten/_bench_smb"
    if sftp_verfuegbar; then
      echo "Lade dieselben 48 MB ueber SFTP hoch..."
      messe "SFTP (6 x 8 MB)" ":sftp:$SFTP_BASIS/daten/_bench_sftp" "${SFTP_FLAGS[@]}"
    else
      echo "  (SFTP-Weg nicht verfuegbar — nur SMB gemessen)"
    fi

    # Serverseitige Gegenprobe: hat der Server die Daten WIRKLICH? Ohne das misst man
    # nur den lokalen Schreibpuffer (Messfalle, live erlebt am 27.08.2026).
    echo "Gegenprobe auf dem Server:"
    if [ "$SFTP_MOEGLICH" -eq 1 ]; then
      ssh -i "$SSH_KEY" -o BatchMode=yes "$SSH_USER@10.8.0.1" \
        "du -sh $SFTP_BASIS/daten/_bench_smb $SFTP_BASIS/daten/_bench_sftp 2>/dev/null;
         rm -rf $SFTP_BASIS/daten/_bench_smb $SFTP_BASIS/daten/_bench_sftp" 2>/dev/null \
        | sed 's/^/  /'
    else
      rclone purge cortex-daten:daten/_bench_smb >/dev/null 2>&1
      echo "  (keine SSH-Verbindung — Gegenprobe uebersprungen)"
    fi
    echo
    echo "  Zum Vergleich: Finder/SMB schafft bei vielen kleinen Dateien rund 1,3 Dateien/s."
    log info "bench gelaufen"
    ;;

  *)
    sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
