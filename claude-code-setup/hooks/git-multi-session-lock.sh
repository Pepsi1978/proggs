#!/usr/bin/env bash
# git-multi-session-lock.sh - DEAKTIVIERT (No-Op) seit 2026-06-26
#
# Frueher: Lock liess parallele Sessions bei git add/commit/push bis zu 120s warten;
# es gab KEIN PostToolUse-Release (Lock nur per TTL/PID-Tod frei). Bash-Tool-Timeout
# bei 120s -> "Commit nach zwei Minuten ausgetimt" (Frank-Vorfall 2026-06-26).
#
# Frank-Entscheidung 2026-06-26: Multi-Session = nur HINWEISE, keine Sperren
# (analog session-presence-guard, schon No-Op). Schutz bleibt ueber
# parallel-sessions-git.md (nur eigene Dateien stagen, fetch+rebase vor Push).
#
# Aus settings.json deregistriert; dieser No-Op faengt laufende/gecachte Sessions ab.
exit 0
