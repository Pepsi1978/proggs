#!/usr/bin/env bash
# blunder-scan.sh — Optional Pre-Commit Blunder Check (L3, bash)
#
# Schach-Engine Refutation-Pattern: Vor jedem Commit den challenger fragen:
# "Was koennten die naechsten 3 Fehler aus diesem Diff sein?"
#
# ACHTUNG: Standalone-Tool, NICHT als auto-Hook registriert!
# Aufruf: bash ~/.Gemini/hooks/blunder-scan.sh
# Oder als pre-commit-hook wenn explizit aktiviert.
#
# Direktive #3: Non-blocking, Timeout-Schutz, Graceful Degradation.

set +e
trap 'echo "[blunder-scan] Fehler abgefangen"; exit 0' ERR

TIMEOUT_SEC=${TIMEOUT_SEC:-60}

# 1. Git-Repo pruefen
repo_root=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$repo_root" ]; then
    echo "[blunder-scan] Kein Git-Repository — uebersprungen."
    exit 0
fi
cd "$repo_root" || { echo "[blunder-scan] cd zum Repo-Root fehlgeschlagen"; exit 0; }

# 2. Staged Diff holen
diff_stat=$(git diff --cached --stat 2>/dev/null)
diff_full=$(git diff --cached 2>/dev/null)

if [ -z "$diff_full" ]; then
    echo "[blunder-scan] Keine staged Aenderungen — nichts zu pruefen."
    exit 0
fi

# 3. Truncate bei zu grossem Diff
diff_text=$(echo "$diff_full" | head -c 5000)
if [ ${#diff_full} -gt 5000 ]; then
    diff_text="${diff_text}

[... truncated for speed ...]"
fi

echo "[blunder-scan] Starte Refutation-Scan (max ${TIMEOUT_SEC}s)..."
echo "Geaenderte Dateien:"
echo "$diff_stat"

# 4. Prompt-Datei schreiben
temp_prompt="${TMPDIR:-/tmp}/blunder-scan-$$.txt"
cat > "$temp_prompt" <<EOF
Refutation-Scan fuer geplanten Commit. Beantworte in max 300 Woerter auf Deutsch:

**Was sind die 3 wahrscheinlichsten Blunders die aus diesem Diff entstehen koennten?**

Konkrete Fragen:
1. Bricht diese Aenderung andere Hooks/Skills/Agents die NICHT im Diff sind?
2. Fehlt das Cross-Platform-Gegenstueck (.sh wenn .ps1 geaendert, umgekehrt)?
3. Wird bestehende Funktionalitaet ungewollt entfernt? (Funktionalitaets-Diff!)
4. Gibt es Race Conditions oder Update-Resistenz-Probleme?
5. Koennte der Benutzer etwas sehen das er nicht sehen sollte (Secrets)?

Nur WAHRSCHEINLICHE Blunders nennen (>30%).
Bei KEINEM erwarteten Blunder: "Diff sauber, keine Refutation noetig."

Diff:
$diff_text
EOF

echo ""
echo "[blunder-scan] Rufe challenger-Agent auf..."
echo "[blunder-scan] HINWEIS: Dieser Hook ist standalone — zur Ausfuehrung aus einer gemini-setup-Session:"
echo "[blunder-scan]   Nutze den challenger-Agent mit dem Inhalt von: $temp_prompt"
echo "[blunder-scan] ODER aktiviere diesen Hook als echten pre-commit in git config."
echo "[blunder-scan] Output bleibt in $temp_prompt zur manuellen Pruefung."

exit 0

