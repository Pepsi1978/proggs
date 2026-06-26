# git-multi-session-lock.ps1 - DEAKTIVIERT (No-Op) seit 2026-06-26
#
# Frueher: Lock-Mechanismus, der parallele Sessions bei git add/commit/push bis zu
# 120-130s warten liess. Es gab KEIN PostToolUse-Release -> der Lock wurde nur per
# TTL (180s) oder PID-Tod frei. Das Bash-Tool timet bei 120s aus
# -> "Commit-Befehl nach zwei Minuten ausgetimt" (Frank-Vorfall 2026-06-26).
#
# Frank-Entscheidung 2026-06-26: Das Multi-Session-System arbeitet nur noch mit
# HINWEISEN (session-presence-warn), nicht mit Sperren - analog session-presence-guard,
# der aus demselben Grund schon zum No-Op wurde (Deadlock-Gefahr). Der Schutz gegen
# versehentliches Ueberschreiben bleibt vollstaendig ueber parallel-sessions-git.md
# erhalten (nur eigene Dateien stagen, fetch+rebase vor jedem Push).
#
# Dieser Hook ist aus settings.json deregistriert. Der No-Op faengt zusaetzlich
# laufende/gecachte Sessions ab, die den Befehl noch aufrufen (settings sind gecacht).
exit 0
