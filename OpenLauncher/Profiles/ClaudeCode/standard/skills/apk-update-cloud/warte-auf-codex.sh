#!/usr/bin/env bash
# Wartet auf das Codex-Review eines Pull Requests und kehrt SOFORT zurück, sobald es fertig ist.
# Fragt alle 15 s nach, höchstens MAX Sekunden (Standard 480 = 8 min). Braucht kein `gh`:
# ohne gh geht es per curl + jq (siehe github-api.sh).
# Aufruf: bash warte-auf-codex.sh <PR-Nummer> [max_sekunden]
# Ausgabe (letzte Zeile): CODEX=ok | CODEX=befunde (n) | CODEX=timeout | ZUGANG=fehlt (…)
set -u
PR="$1"; MAX="${2:-480}"; REPO="${REPO:-Pepsi1978/proggs}"; BOT='chatgpt-codex-connector[bot]'
source "$(dirname "$0")/github-api.sh"
pruefe_zugang "$REPO"

# Zählt die Einträge von <pfad>, auf die der jq-Filter <bedingung> passt; bei Fehler „x“.
zaehle() {
  local json
  json=$(api "$1" 2>/dev/null) && printf '%s' "$json" | jq "[.[]|select($2)]|length" 2>/dev/null || echo x
}

start=$(date +%s)
while :; do
  daumen=$(zaehle "repos/$REPO/issues/$PR/reactions" ".user.login==\"$BOT\" and .content==\"+1\"")
  reviews=$(zaehle "repos/$REPO/pulls/$PR/reviews" ".user.login==\"$BOT\"")
  zeilen=$(zaehle "repos/$REPO/pulls/$PR/comments" ".user.login==\"$BOT\"")
  fertig=$(zaehle "repos/$REPO/issues/$PR/comments" \
    ".user.login==\"$BOT\" and (.body|test(\"Completed|find any major issues|keine.*Befunde\";\"i\"))")
  case "$daumen$reviews$zeilen$fertig" in *x*) zaehle_fehler 1; sleep 15; continue ;; *) zaehle_fehler 0 ;; esac
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
