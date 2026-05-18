#!/usr/bin/env bash
# check-compliance-artifacts.sh — Prueft ob interne Compliance-Artefakte im Repo nachweisbar sind.
#
# Prueft auf Hinweise zu: VVT/ROPA, DSFA/DPIA, TIA, TOMs, AVV-Liste, SCCs, Loeschkonzept,
# Datenpannen-Plan, AI-Risikoklassifizierung, DSA-Beschwerdestelle.
#
# Exit-Codes:
#   0 = Alle 10 Artefakt-Klassen mindestens 1x referenziert
#   1 = Mindestens 1 Artefakt-Klasse fehlt — 🟠 HOCH im Bericht
#   2 = Ungueltiger Aufruf
#
# Verwendung: bash check-compliance-artifacts.sh <APP_DIR>

set -u

APP_DIR="${1:-}"

if [ -z "$APP_DIR" ]; then
    echo "Verwendung: $0 <APP_DIR>" >&2
    exit 2
fi

if [ ! -d "$APP_DIR" ]; then
    echo "Fehler: '$APP_DIR' ist kein Verzeichnis." >&2
    exit 2
fi

# Suche-Strategie: bevorzugt rg (schneller), fallback auf grep -E (extended regex — Pflicht
# damit `|` als ODER funktioniert). `command -v rg` muss ein BINARY finden, nicht eine
# Bash-Funktion — Claude Code injiziert eine `rg`-Funktion in interaktive Shells, die in
# Subshells nicht verfuegbar ist. Direktive 3: ohne -E waeren alle Pipe-Patterns unwirksam.
if command -v rg >/dev/null 2>&1 && ! type -t rg 2>/dev/null | grep -q function; then
    SEARCH="rg -n -i --no-heading -l"
else
    SEARCH="grep -E -rl -i --color=never --binary-files=without-match --exclude-dir=.gradle --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=.idea --exclude-dir=captures --exclude-dir=.claude --exclude-dir=.kotlin --exclude-dir=.gemini"
fi

# Pruefung pro Artefakt-Klasse
declare -A artifacts=(
    ["VVT_ROPA"]="ropa|rope|verarbeitungstaetigkeit|art\\.?[ ]?30|verzeichnis.*verarbeitung"
    ["DSFA_DPIA"]="dsfa|dpia|datenschutz.*folgenabschaetzung|impact.*assessment|art\\.?[ ]?35"
    ["TIA"]="tia|transfer.*impact|art\\.?[ ]?44"
    ["TOMs"]="tom|toms|technisch.*organisatorisch|art\\.?[ ]?32"
    ["AVV_DPA_Liste"]="avv|dpa.template|auftragsverarbeitung|art\\.?[ ]?28"
    ["SCCs"]="standard.*contractual|scc|standardvertragsklausel|art\\.?[ ]?46|dpf|data.*privacy.*framework"
    ["Loeschkonzept"]="loeschkonzept|deletion.*concept|retention.*policy"
    ["Datenpannen_Plan"]="datenpanne|breach.*plan|breach.*notification|art\\.?[ ]?33|72.*stunde"
    ["AI_Risikoklassifizierung"]="ai.*risk|ki.*risiko|risikoklassifizierung|ai.*act.*art\\.?[ ]?6"
    ["DSA_Beschwerdestelle"]="dsa.*kontakt|dsa.*beschwerde|single.*point.*of.*contact|art\\.?[ ]?11.*dsa"
)

declare -A found

for key in "${!artifacts[@]}"; do
    pattern="${artifacts[$key]}"
    RESULT=$($SEARCH "$pattern" "$APP_DIR" 2>/dev/null | head -1)
    if [ -n "$RESULT" ]; then
        found[$key]="$RESULT"
    fi
done

# Auch Doku-Verzeichnisse pruefen
EXTRA_PATHS=""
for d in "$APP_DIR/docs/datenschutz" "$APP_DIR/docs/compliance" "$APP_DIR/docs/avv" "$APP_DIR/docs/legal"; do
    if [ -d "$d" ]; then
        EXTRA_PATHS="$EXTRA_PATHS $d"
    fi
done

MISSING_COUNT=0
echo "{"
echo "  \"artifacts\": {"
COUNT=0
for key in VVT_ROPA DSFA_DPIA TIA TOMs AVV_DPA_Liste SCCs Loeschkonzept Datenpannen_Plan AI_Risikoklassifizierung DSA_Beschwerdestelle; do
    if [ -n "${found[$key]+x}" ]; then
        status="true"
    else
        status="false"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
    COUNT=$((COUNT + 1))
    if [ $COUNT -lt 10 ]; then
        echo "    \"$key\": $status,"
    else
        echo "    \"$key\": $status"
    fi
done
echo "  },"
echo "  \"missing_count\": $MISSING_COUNT,"
# Doku-Verzeichnisse trimmen und sauber als JSON-Array ausgeben
EXTRA_TRIMMED=$(echo "$EXTRA_PATHS" | sed -e 's/^ *//' -e 's/ *$//')
if [ -n "$EXTRA_TRIMMED" ]; then
    # Pfade als komma-getrennte JSON-Liste — jeder Pfad in Anfuehrungszeichen
    JSON_DIRS=$(echo "$EXTRA_TRIMMED" | tr ' ' '\n' | awk 'NF{printf "\"%s\",", $0}' | sed 's/,$//')
    echo "  \"compliance_dirs_found\": [$JSON_DIRS]"
else
    echo "  \"compliance_dirs_found\": []"
fi
echo "}"

if [ $MISSING_COUNT -gt 0 ]; then
    echo ""
    echo "EMPFEHLUNG: $MISSING_COUNT von 10 internen Compliance-Artefakten nicht im Repo nachweisbar." >&2
    echo "Im Bericht als 🟠 HOCH markieren — durch Datenschutzbeauftragten verifizieren lassen." >&2
    exit 1
else
    exit 0
fi
