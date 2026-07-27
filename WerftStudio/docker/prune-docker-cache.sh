#!/bin/sh
set -u

maintenance=/var/lib/werft-studio-maintenance
mkdir -p "$maintenance"
before="$(df -B1 --output=used / | tail -n 1 | tr -d ' ')"
if /usr/bin/docker builder prune --all --force; then
  ok=true
else
  ok=false
fi
after="$(df -B1 --output=used / | tail -n 1 | tr -d ' ')"
freed=$((before > after ? before - after : 0))

if [ -f "$maintenance/prune.request" ]; then
  request_id="$(tr -cd '0-9a-f-' < "$maintenance/prune.request")"
  printf '{"requestId":"%s","freedBytes":%s,"usedBytes":%s,"ok":%s}\n' "$request_id" "$freed" "$after" "$ok" > "$maintenance/prune.result.tmp"
  mv "$maintenance/prune.result.tmp" "$maintenance/prune.result"
  rm -f "$maintenance/prune.request"
fi

[ "$ok" = true ]
