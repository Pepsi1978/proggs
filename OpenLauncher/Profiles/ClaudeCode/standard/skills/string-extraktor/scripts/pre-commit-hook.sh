#!/usr/bin/env bash
# pre-commit-hook.sh — Erweiterter Pre-Commit-Hook fuer den string-extraktor Skill
#
# Sucht nach hardcodierten Strings in staged Kotlin-Dateien mit dem vollen
# 9-Pattern-Set aus Phase 1 (statt nur Basic-Regex aus dem Skill).
# Verhindert dass neue hardcodierte Strings im Commit landen.
#
# Installation:
#   cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit
#
# Aufruf-Modi:
#   .git/hooks/pre-commit                  (von Git aufgerufen, scannt staged Dateien)
#   bash pre-commit-hook.sh --files "f1 f2" (manuell mit Datei-Liste, fuer CI)
#   bash pre-commit-hook.sh --all          (alle .kt-Dateien im Projekt)
#
# Exit-Code:
#   0 = keine hardcodierten Strings gefunden
#   1 = hardcodierte Strings gefunden (Commit blockiert)

set -euo pipefail

# === Konfiguration: Welche Dateien scannen? ===
MODE="staged"
FILES=""

while [ $# -gt 0 ]; do
    case "$1" in
        --files)
            MODE="manual"
            FILES="$2"
            shift 2
            ;;
        --all)
            MODE="all"
            shift
            ;;
        *)
            shift
            ;;
    esac
done

# === Dateien sammeln ===
if [ "$MODE" = "staged" ]; then
    # Pattern 9 deckt XML-Layouts ab (android:text in res/values/), nicht nur Kotlin
    FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(kt|xml)$' || true)
elif [ "$MODE" = "all" ]; then
    FILES=$(find . \( -name "*.kt" -o -name "*.xml" \) -not -path "*/build/*" -not -path "*/test/*" -not -path "*/androidTest/*")
fi

if [ -z "$FILES" ]; then
    exit 0
fi

# === Die 9 Patterns aus Phase 1.3 ===
# Jedes Pattern findet einen Typ von hardcodiertem String
# Format: "PATTERN_NAME|GREP_PATTERN"

declare -a PATTERNS=(
    "Compose-Text|Text\(\s*\"[^\"]*[a-zA-ZäöüÄÖÜß][^\"]*\""
    "Compose-Button|Button\([^)]*\)\s*\{[^}]*Text\(\s*\""
    "Toast|Toast\.makeText\([^,]+,\s*\""
    "Snackbar|showSnackbar\(\s*\""
    "Snackbar-Show|snackbarHostState\.showSnackbar\(\s*\""
    "ContentDescription|contentDescription\s*=\s*\""
    "Placeholder-Hint|(placeholder|hint|label)\s*=.*Text\(\s*\""
    "TopAppBar|TopAppBar.*title\s*=.*Text\(\s*\""
    "Error-Call|error\(\s*\"[^\"]*[a-zA-Z]"
    "Enum-Label|enum class[^{]*\{[^}]*\"[^\"]*[a-zA-Z]"
    "XML-android-text|android:(text|hint|contentDescription|title)=\"[^@][^\"]+\""
)

# === Unicode-Escape-Pattern (PFLICHT) ===
UNICODE_ESCAPE_PATTERN='\\u00(dc|fc|d6|f6|c4|e4|df)'

# === Ausschluesse: Was NICHT als hardcoded String gezaehlt wird ===
# Erweitert: analytics, getString (interne Keys), jsonObject (Daten-Keys)
EXCLUDE_PATTERN='stringResource|@Preview|@Suppress|// |Log\.|Timber\.|println\(|Regex\(|const val TAG|analytics\.|getString\(\"|jsonObject\.|"\\u'

# === Scan ===
ERRORS=0
TMP=$(mktemp)
trap "rm -f $TMP" EXIT

for F in $FILES; do
    # Datei ueberspringen wenn nicht existiert (kann bei staged deletions passieren)
    [ -f "$F" ] || continue

    # Test-Verzeichnisse ueberspringen
    case "$F" in
        */test/*|*/androidTest/*|*Preview*)
            continue
            ;;
    esac

    FILE_HITS=""

    # Diff-Modus: nur neue Zeilen (mit +) scannen wenn staged
    if [ "$MODE" = "staged" ]; then
        CONTENT=$(git diff --cached "$F" | grep "^+" | sed 's/^+//' || true)
    else
        CONTENT=$(cat "$F")
    fi

    if [ -z "$CONTENT" ]; then
        continue
    fi

    echo "$CONTENT" > "$TMP"

    # Pattern fuer Pattern durchgehen
    for ENTRY in "${PATTERNS[@]}"; do
        NAME="${ENTRY%%|*}"
        PATTERN="${ENTRY#*|}"

        HITS=$(grep -nE "$PATTERN" "$TMP" 2>/dev/null | grep -vE "$EXCLUDE_PATTERN" || true)
        if [ -n "$HITS" ]; then
            FILE_HITS="${FILE_HITS}\n  [$NAME]\n$(echo "$HITS" | sed 's/^/    /')"
        fi
    done

    # Unicode-Escape-Check (PFLICHT)
    UNICODE_HITS=$(grep -nE "$UNICODE_ESCAPE_PATTERN" "$TMP" 2>/dev/null | grep -vE "$EXCLUDE_PATTERN" || true)
    if [ -n "$UNICODE_HITS" ]; then
        FILE_HITS="${FILE_HITS}\n  [Unicode-Escape Umlaut]\n$(echo "$UNICODE_HITS" | sed 's/^/    /')"
    fi

    if [ -n "$FILE_HITS" ]; then
        echo "=== Hardcoded strings in: $F ===" >&2
        printf "%b\n" "$FILE_HITS" >&2
        ERRORS=$((ERRORS+1))
    fi
done

# === Auswertung ===
if [ "$ERRORS" -gt 0 ]; then
    echo "" >&2
    echo "============================================" >&2
    echo "FEHLER: $ERRORS Datei(en) mit hardcodierten Strings." >&2
    echo "" >&2
    echo "Bitte stringResource() verwenden:" >&2
    echo "  Text(\"Speichern\")" >&2
    echo "  -> Text(stringResource(R.string.action_save))" >&2
    echo "" >&2
    echo "Bei Notwendigkeit (z.B. echte technische Konstanten):" >&2
    echo "  @Suppress(\"HardcodedText\")" >&2
    echo "============================================" >&2
    exit 1
fi

exit 0
