#!/usr/bin/env bash
# claude-login-sync: spiegelt den Claude-Code-Login auf alle Profil-Konfigurationsordner.
# Läuft als SessionStart- und SessionEnd-Hook; Logik in claude-login-sync.py (gemeinsam mit .ps1).
# Ohne diesen Abgleich verlangt jedes Profil (OpenLauncher: minimal/standard/strict) eine
# eigene Anmeldung, weil Claude Code den Token je CLAUDE_CONFIG_DIR getrennt im Schlüsselbund
# ablegt. Siehe bugs/claude-tooling/claude-config.md.
#
# Die Ausgabe geht bewusst nicht in Claudes Kontext (SessionStart-stdout landet dort) —
# das Protokoll steht in ~/.claude/logs/claude-login-sync.jsonl.
# Fail-open: fällt Python aus, passiert nichts; der Hook endet immer mit 0.
# Event-JSON verwerfen (wird nicht gebraucht). Nur wenn stdin KEIN Terminal ist —
# sonst würde ein Aufruf von Hand ewig auf eine Eingabe warten.
[ -t 0 ] || cat >/dev/null 2>&1 || true
python3 "$(dirname "$0")/claude-login-sync.py" >/dev/null 2>&1 || true
exit 0
