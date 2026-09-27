#!/usr/bin/env bash
# Wartet auf das Codex-Review eines Pull Requests und kehrt SOFORT zurück, sobald es fertig ist.
# Fragt alle 15 s nach, höchstens MAX Sekunden (Standard 480 = 8 min).
# Aufruf: bash warte-auf-codex.sh <PR-Nummer> [max_sekunden]
# Ausgabe (letzte Zeile): CODEX=ok | CODEX=befunde (n) | CODEX=timeout
set -u
PR="$1"; MAX="${2:-480}"; REPO="${REPO:-Pepsi1978/proggs}"; BOT='chatgpt-codex-connector[bot]'
start=$(date +%s)
while :; do
  daumen=$(gh api "repos/$REPO/issues/$PR/reactions" --jq "[.[]|select(.user.login==\"$BOT\" and .content==\"+1\")]|length" 2>/dev/null || echo 0)
  reviews=$(gh api "repos/$REPO/pulls/$PR/reviews" --jq "[.[]|select(.user.login==\"$BOT\")]|length" 2>/dev/null || echo 0)
  zeilen=$(gh api "repos/$REPO/pulls/$PR/comments" --jq "[.[]|select(.user.login==\"$BOT\")]|length" 2>/dev/null || echo 0)
  fertig=$(gh api "repos/$REPO/issues/$PR/comments" --jq "[.[]|select(.user.login==\"$BOT\" and (.body|test(\"Completed|find any major issues|keine.*Befunde\";\"i\")))]|length" 2>/dev/null || echo 0)
  if [ "${zeilen:-0}" -gt 0 ] || [ "${reviews:-0}" -gt 0 ]; then
    echo "CODEX=befunde ($zeilen Zeilenkommentare, $reviews Reviews)"; exit 0
  fi
  if [ "${daumen:-0}" -gt 0 ] || [ "${fertig:-0}" -gt 0 ]; then
    echo "CODEX=ok"; exit 0
  fi
  jetzt=$(( $(date +%s) - start ))
  [ "$jetzt" -ge "$MAX" ] && { echo "CODEX=timeout (${jetzt}s)"; exit 0; }
  sleep 15
done
