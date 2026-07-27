#!/usr/bin/env bash
# docker-cache-cleanup.sh — woechentlicher "Papierkorb" fuer den Docker-Build-Cache.
#
# WARUM ES DIESES SKRIPT GIBT (Vorfall 27.07.2026):
#   Die Root-Disk des VPS war zu 62 % voll (60 GB von 96 GB). Die Diagnose ergab:
#   54 GB davon waren reiner BUILD-CACHE in /var/lib/containerd (2983 Snapshot-Schichten,
#   aelteste vom 23.06.2026). Die eigentlichen Anwendungen belegten nur ~6 GB.
#   Ursache: Jeder Deploy laeuft per "docker compose up -d --build" (siehe DEPLOY.md).
#   BuildKit legt dabei Zwischenschichten als Cache ab — und raeumte sie NIE wieder weg,
#   weil weder eine GC-Policy in daemon.json noch ein Aufraeum-Job existierte.
#   Bei zwei Stacks (second-brain + werft-studio) mit zusammen 12 selbst gebauten Diensten
#   waechst das schnell: ~54 GB in gut vier Wochen.
#
# WAS ES TUT:
#   Begrenzt den Build-Cache auf MAX_CACHE. Alles darueber (aeltester Cache zuerst) faellt weg.
#   Der Cache ist reiner Beschleuniger — nichts davon wird zur Laufzeit gebraucht. Ein
#   geloeschter Cache macht nur den NAECHSTEN Bau eines Dienstes einmalig langsamer.
#
# WAS ES BEWUSST NICHT TUT (Funktionalitaets-Erhaltung, bugs/server/docker.md §6.4):
#   KEIN "docker system prune", KEIN "-a", KEIN "--volumes". Images, Container, Netzwerke und
#   vor allem VOLUMES (Postgres, Qdrant, MinIO = echte Daten!) werden NICHT angefasst.
#   Nur "docker builder prune" — der raeumt ausschliesslich Bau-Zwischenschichten.
#
# ZWEITE SCHICHT (Defense in Depth):
#   Zusaetzlich ist in /etc/docker/daemon.json eine BuildKit-GC-Policy hinterlegt, die schon
#   waehrend des Bauens begrenzt. Dieses Skript ist das Netz darunter, falls die Policy
#   nicht greift (sie wird erst mit einem Docker-Daemon-Neustart aktiv).
#
# Cron (Server, sonntags 3:30 Uhr — eine halbe Stunde vor dem Gehirn-Backup um 4:00):
#   30 3 * * 0 /opt/second-brain/scripts/docker-cache-cleanup.sh
set -euo pipefail

MAX_CACHE="10GB"                                          # so viel Build-Cache darf bleiben
LOG="/opt/second-brain/brain-logs/docker-cache-cleanup.jsonl"
WARN_PCT=80                                               # ab dieser Root-Belegung: Warnung ins Log

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >> "$LOG" 2>/dev/null || true; }

mkdir -p "$(dirname "$LOG")" 2>/dev/null || true

# --- Sonde 1: Zustand VOR dem Aufraeumen festhalten (nichts stirbt still) ---------------
vorher_pct="$(df --output=pcent / | tail -1 | tr -dc '0-9')"
vorher_used="$(df -h --output=used / | tail -1 | tr -d ' ')"
cache_vorher="$(docker system df --format '{{.Type}} {{.Size}}' 2>/dev/null | awk '/Build Cache/{print $3}')"
log "START  root=${vorher_used} (${vorher_pct}%)  build-cache=${cache_vorher:-unbekannt}"

# --- Aufraeumen ------------------------------------------------------------------------
# --max-used-space ist die Docker-28+/29-Option (das aeltere --keep-storage gibt es nicht mehr).
# Ohne -a: nur ungenutzter Cache faellt weg, aktiv referenzierter bleibt.
if ! ausgabe="$(docker builder prune -f --max-used-space="$MAX_CACHE" 2>&1)"; then
    log "FEHLER: docker builder prune fehlgeschlagen: ${ausgabe}"
    echo "docker-cache-cleanup FEHLER: ${ausgabe}" >&2
    exit 1
fi
freed="$(printf '%s' "$ausgabe" | awk '/Total:/{print $2}')"
log "PRUNE  freigegeben=${freed:-0B}"

# --- Sonde 2: Zustand NACH dem Aufraeumen + Sanity-Check -------------------------------
nachher_pct="$(df --output=pcent / | tail -1 | tr -dc '0-9')"
nachher_used="$(df -h --output=used / | tail -1 | tr -d ' ')"
log "ENDE   root=${nachher_used} (${nachher_pct}%)"

# Invariante: alle Container muessen weiterlaufen. Ein Cache-Prune darf NIE einen Dienst
# umbringen — wenn doch, muss das laut und sofort im Log stehen.
laufend="$(docker ps -q | wc -l)"
gesamt="$(docker ps -aq | wc -l)"
if [ "$laufend" -lt "$gesamt" ]; then
    log "WARNUNG: nur ${laufend} von ${gesamt} Containern laufen — bitte pruefen!"
fi

if [ "$nachher_pct" -ge "$WARN_PCT" ]; then
    log "WARNUNG: Root-Disk trotz Aufraeumen bei ${nachher_pct}% — andere Ursache pruefen (du -xh --max-depth=1 /var)"
fi

exit 0
