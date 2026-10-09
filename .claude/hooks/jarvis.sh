#!/usr/bin/env bash
# Jarvis aus einer Cloud-Sitzung erreichen (Claude Code in der Cloud, Codex Cloud), ohne eingerichteten MCP-Server.
#
#   bash .claude/hooks/jarvis.sh                          -> die letzte Tagesauswertung von Jarvis
#   bash .claude/hooks/jarvis.sh <werkzeug> '<json>'      -> ein beliebiges Jarvis-Werkzeug, z. B.
#   bash .claude/hooks/jarvis.sh aufgaben_lesen '{"bereich":"heute"}'
#   bash .claude/hooks/jarvis.sh start                    -> wie ohne Angabe, aber kurz wartend (für den Start-Hook)
#
# Die Adresse steht in der Umgebungsvariablen JARVIS_MCP_URL der Cloud-Umgebung (die Plugin-Adresse aus Jarvis,
# Einstellungen -> ChatGPT-Plugin). Sie ist ein Schlüssel: nie ausgeben, nie ins Repo schreiben.
# Der Host muss in der Netzwerk-Freigabe der Cloud-Umgebung stehen. Endet immer mit 0, damit ein Start-Hook nie hängt.

werkzeug="${1:-tagesauswertung_lesen}"
argumente="${2:-}"
[ -z "$argumente" ] && argumente='{}'
wartezeit=90
if [ "$werkzeug" = "start" ]; then werkzeug="tagesauswertung_lesen"; wartezeit=25; fi

if [ -z "${JARVIS_MCP_URL:-}" ]; then
  echo "JARVIS NICHT EINGERICHTET: In dieser Umgebung fehlt die Variable JARVIS_MCP_URL. Sag das Frank in einem Satz und arbeite normal weiter."
  exit 0
fi

anfrage=$(printf '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"%s","arguments":%s}}' "$werkzeug" "$argumente")
antwort=$(curl -sS --max-time "$wartezeit" -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -d "$anfrage" "$JARVIS_MCP_URL" 2>&1)
if [ $? -ne 0 ] || [ -z "$antwort" ]; then
  # Die Fehlermeldung von curl kann die Adresse enthalten: nur die Art des Fehlers weitergeben.
  grund=$(printf '%s' "$antwort" | grep -oiE 'host_not_allowed|could not resolve host|timed out|connection refused|403|407' | head -1)
  echo "JARVIS NICHT ERREICHBAR (${grund:-keine Antwort}): Der Host von Jarvis ist in der Netzwerk-Freigabe dieser Umgebung nicht erlaubt, oder Franks Handy ist gerade offline. Sag das Frank in einem Satz und arbeite normal weiter."
  exit 0
fi

if command -v jq >/dev/null 2>&1; then
  text=$(printf '%s' "$antwort" | jq -r '.result.content[0].text // .error.message // empty' 2>/dev/null)
else
  text=$(printf '%s' "$antwort" | python3 -c 'import json,sys
d=json.load(sys.stdin)
print(((d.get("result") or {}).get("content") or [{}])[0].get("text") or (d.get("error") or {}).get("message") or "")' 2>/dev/null)
fi
if [ -z "$text" ]; then
  echo "JARVIS HAT NICHT VERWERTBAR GEANTWORTET. Sag das Frank in einem Satz und arbeite normal weiter."
  exit 0
fi

if [ "$werkzeug" = "tagesauswertung_lesen" ]; then
  echo "JARVIS-TAGESAUSWERTUNG (Hintergrund zu Franks Tag: Dienst, Schlaf und Erholung, Termine, Aufgaben. Nicht ungefragt wiedergeben; nur einbeziehen, wenn es für die Aufgabe zählt):"
fi
printf '%s\n' "$text"
exit 0
