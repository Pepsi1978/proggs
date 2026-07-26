#!/usr/bin/env bash
# check-uk-data-processing.sh — Prueft ob die App personenbezogene Daten von UK-Nutzern verarbeitet.
#
# Wird vom Rechtssicherheits-Skill in Schritt 5h-UK (UK-Vertreter-Pflicht) aufgerufen.
# Wenn KEINE Datenverarbeitung gefunden wird, darf UK in der Country-Availability bleiben.
# Wenn EINE Datenverarbeitung nachgewiesen wird (Crashlytics, Analytics, Push-FCM, etc.),
# greift UK-GDPR Art. 27 — Standard-Empfehlung Option B (UK ausschliessen).
#
# Exit-Codes:
#   0 = Datenverarbeitung nachgewiesen (UK-Vertreter-Pflicht relevant)
#   1 = Keine Datenverarbeitung — UK darf bleiben
#   2 = Ungueltiger Aufruf
#
# Verwendung: bash check-uk-data-processing.sh <APP_DIR>

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
    SEARCH="rg -n -i --no-heading"
else
    SEARCH="grep -E -rn -i --color=never --binary-files=without-match --exclude-dir=.gradle --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=.idea --exclude-dir=captures --exclude-dir=.claude --exclude-dir=.kotlin --exclude-dir=.gemini"
fi

DATA_PROCESSING_FOUND=0
HITS=()

# 1. SDKs die automatisch personenbezogene Daten verarbeiten
RESULT=$($SEARCH "firebase|crashlytics|analytics|sentry|amplitude|mixpanel|fcm|admob|adsense|advertising[-_ ]id|ad_id|ads-sdk" "$APP_DIR" 2>/dev/null | head -5)
if [ -n "$RESULT" ]; then
    HITS+=("SDK-Tracking: $(echo "$RESULT" | head -1)")
    DATA_PROCESSING_FOUND=1
fi

# 2. Netzwerk-/Backend-Aktivitaet
RESULT=$($SEARCH "INTERNET|retrofit|okhttp|ktor|api[-_ ]key|bearer" "$APP_DIR" 2>/dev/null | head -5)
if [ -n "$RESULT" ]; then
    HITS+=("Backend-Aktivitaet: $(echo "$RESULT" | head -1)")
    DATA_PROCESSING_FOUND=1
fi

# 3. Account-/Login-Indikatoren
RESULT=$($SEARCH "firebase[-_ ]auth|google[-_ ]signin|signup|signin|login.*screen|account.*creation" "$APP_DIR" 2>/dev/null | head -5)
if [ -n "$RESULT" ]; then
    HITS+=("Account-System: $(echo "$RESULT" | head -1)")
    DATA_PROCESSING_FOUND=1
fi

# 4. Cloud-Sync / Backup
RESULT=$($SEARCH "drive|dropbox|s3.amazonaws|gcs.googleapis|cloud[-_ ]sync|backup[-_ ]server|sync[-_ ]server" "$APP_DIR" 2>/dev/null | head -5)
if [ -n "$RESULT" ]; then
    HITS+=("Cloud-Sync: $(echo "$RESULT" | head -1)")
    DATA_PROCESSING_FOUND=1
fi

# 5. Privacy Policy enthaelt bereits UK-Vertreter-Hinweis?
UK_REP_FOUND=$($SEARCH "uk[ -]representative|uk[ -]vertreter|article[ ]27|art\\.[ ]27|gdprlocal|verasafe|captaincompliance" "$APP_DIR" 2>/dev/null | head -3)

# 6. Country availability
COUNTRY_LIST=$($SEARCH "country[ -]availability|distribution|target[ -]markets|united kingdom" "$APP_DIR" 2>/dev/null | head -3)

# Output als Pseudo-JSON
echo "{"
echo "  \"data_processing_found\": $([ $DATA_PROCESSING_FOUND -eq 1 ] && echo true || echo false),"
echo "  \"hits\": ["
for ((i=0; i<${#HITS[@]}; i++)); do
    if [ $i -lt $((${#HITS[@]} - 1)) ]; then
        printf "    %q,\n" "${HITS[$i]}"
    else
        printf "    %q\n" "${HITS[$i]}"
    fi
done
echo "  ],"
echo "  \"uk_representative_mentioned\": $([ -n "$UK_REP_FOUND" ] && echo true || echo false),"
echo "  \"country_list_found\": $([ -n "$COUNTRY_LIST" ] && echo true || echo false)"
echo "}"

if [ $DATA_PROCESSING_FOUND -eq 1 ]; then
    echo ""
    echo "EMPFEHLUNG: UK-Vertreter-Pflicht relevant. Standard-Empfehlung Option B: UK aus" >&2
    echo "Country-Availability entfernen (Play Console + alle anderen Distributionskanaele)." >&2
    exit 0
else
    echo ""
    echo "EMPFEHLUNG: KEINE Datenverarbeitung von UK-Nutzern nachweisbar. UK darf in" >&2
    echo "Country-Availability bleiben (kein Befund im UK-Vertreter-Block)." >&2
    exit 1
fi
