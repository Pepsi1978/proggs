#!/usr/bin/env bash
# Gemeinsamer GitHub-Zugang für warte-auf-codex.sh und warte-auf-bau.sh — mit oder ohne `gh`.
# Cloud-Sitzungen haben oft kein `gh` (Bug B15 in bugs/claude-tooling/claude-code-cloud.md), aber
# curl, jq und meist GH_TOKEN/GITHUB_TOKEN. Wird mit `source` eingebunden.
#
#   api <pfad>        gibt das JSON von https://api.github.com/<pfad> aus, Filtern danach mit jq
#   pruefe_zugang     bricht sofort mit ZUGANG=fehlt ab, statt minutenlang still zu warten

GITHUB_API_TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"

api() {
  if command -v gh >/dev/null 2>&1; then
    gh api "$1"
  else
    local kopf=(-H "Accept: application/vnd.github+json")
    # Ohne Token nur 60 Anfragen pro Stunde — beim Nachfragen alle 15 s zu wenig.
    [ -n "$GITHUB_API_TOKEN" ] && kopf+=(-H "Authorization: Bearer $GITHUB_API_TOKEN")
    curl -fsS "${kopf[@]}" "https://api.github.com/$1"
  fi
}

pruefe_zugang() {
  command -v jq >/dev/null 2>&1 || { echo "ZUGANG=fehlt (jq nicht installiert)"; exit 2; }
  if ! command -v gh >/dev/null 2>&1 && ! command -v curl >/dev/null 2>&1; then
    echo "ZUGANG=fehlt (weder gh noch curl)"; exit 2
  fi
  api "repos/$1" >/dev/null 2>&1 || {
    echo "ZUGANG=fehlt (GitHub-API für $1 nicht erreichbar — GitHub-MCP-Werkzeuge nutzen)"; exit 2
  }
}
