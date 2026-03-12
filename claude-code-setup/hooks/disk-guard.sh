#!/data/data/com.termux/files/usr/bin/bash
# Disk space guardian - warns when storage is critically low
# Runs as a SessionStart hook

USAGE=$(df /data/data/com.termux/files/home 2>/dev/null | awk 'NR==2{gsub(/%/,"",$5); print $5}')
AVAIL=$(df -h /data/data/com.termux/files/home 2>/dev/null | awk 'NR==2{print $4}')

if [ -n "$USAGE" ] && [ "$USAGE" -ge 95 ]; then
  termux-notification \
    --title "Speicherplatz kritisch!" \
    --content "Nur noch $AVAIL frei (${USAGE}% belegt). Fuehre ~/scripts/cleanup.sh aus!" \
    --priority max \
    --vibrate 500,200,500 \
    --id disk-warn \
    --button1 "Cleanup" \
    --button1-action "bash ~/scripts/cleanup.sh" 2>/dev/null

  echo "WARNUNG: Speicherplatz bei ${USAGE}% — nur $AVAIL frei!" >&2
fi

exit 0
