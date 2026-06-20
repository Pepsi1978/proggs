#!/usr/bin/env bash
# mm-research.sh — dünner Wrapper um mm-research.py (die eigentliche Logik liegt dort, robuster +
# plattformübergreifend, nur Python-Standardbibliothek). Siehe Kopf von mm-research.py.
#
# Verwendung:  bash mm-research.sh "deine Recherche-Frage" [anzahl_quellen]
#   MM_THINK_BUDGET=24000  (env, optional)
exec python3 "$(dirname "$0")/mm-research.py" "$@"
