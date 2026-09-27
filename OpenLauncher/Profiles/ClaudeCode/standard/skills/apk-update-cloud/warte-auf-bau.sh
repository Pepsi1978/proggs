#!/usr/bin/env bash
# Wartet nach dem Merge auf den GitHub-Bau (android-cloud-build.yml) und kehrt SOFORT zurück,
# sobald er fertig ist. Fragt alle 15 s nach, höchstens MAX Sekunden (Standard 570, passt unter
# das 10-min-Limit des Bash-Werkzeugs; bei BAU=laeuft einfach erneut aufrufen).
# Aufruf: bash warte-auf-bau.sh <PR-Nummer> [max_sekunden]
# Ausgabe (letzte Zeile): BAU=gruen <url> | BAU=rot <url> | BAU=laeuft <url> | BAU=kein-lauf
set -u
PR="$1"; MAX="${2:-570}"; REPO="${REPO:-Pepsi1978/proggs}"
SHA=$(gh api "repos/$REPO/pulls/$PR" --jq .merge_commit_sha)
start=$(date +%s)
while :; do
  lauf=$(gh api "repos/$REPO/actions/workflows/android-cloud-build.yml/runs?head_sha=$SHA&per_page=1" \
    --jq '.workflow_runs[0]|select(.)|"\(.status) \(.conclusion) \(.html_url)"' 2>/dev/null)
  jetzt=$(( $(date +%s) - start ))
  if [ -n "$lauf" ]; then
    set -- $lauf
    if [ "$1" = completed ]; then
      [ "$2" = success ] && echo "BAU=gruen $3" || echo "BAU=rot ($2) $3"; exit 0
    fi
    [ "$jetzt" -ge "$MAX" ] && { echo "BAU=laeuft $3"; exit 0; }
  else
    # Kein Lauf nach 3 min: Merge hat keine App-Dateien berührt oder Auslöser greift nicht.
    [ "$jetzt" -ge 180 ] && { echo "BAU=kein-lauf (Commit $SHA)"; exit 0; }
  fi
  sleep 15
done
