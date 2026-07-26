#!/usr/bin/env bash
# scan-strings.sh — Master-Skript fuer Phase 1 des string-extraktor Skills
#
# Findet ALLE hardcodierten Strings in einer Android-App (Compose + XML)
# und erzeugt einen strukturierten Initial-Bericht.
#
# Aufruf:  bash scan-strings.sh <APP_DIR>
# Output:  <APP_DIR>/string-extraktor-scan.md (Markdown-Bericht)
#
# Findet:
#  - Compose Text() mit hardcodierten Strings (Pattern 1)
#  - String-Templates und Konkatenation (Pattern 2)
#  - Toasts und Snackbars (Pattern 3)
#  - Content Descriptions (Pattern 4)
#  - Placeholder/Hint/Label (Pattern 5)
#  - TopAppBar und Navigation (Pattern 6)
#  - Benutzersichtbare Fehler (Pattern 7)
#  - Enum-Labels (Pattern 8)
#  - XML-Layouts mit android:text (Pattern 9)
#  - Unicode-Escapes Ü fuer deutsche Umlaute (Pattern U1-U7, PFLICHT)
#  - HTML-Entities &Uuml; etc.
#
# Filtert AUS:
#  - Log/Timber-Statements
#  - Test-Verzeichnisse
#  - @Preview-Composables
#  - URLs und technische Konstanten

set -euo pipefail

APP_DIR="${1:-}"
if [ -z "$APP_DIR" ] || [ ! -d "$APP_DIR" ]; then
    echo "Usage: $0 <APP_DIR>" >&2
    echo "  <APP_DIR>: Pfad zur Android-App (mit app/src/main/)" >&2
    exit 1
fi

SRC="$APP_DIR/app/src/main"
if [ ! -d "$SRC" ]; then
    # Multi-Module: jedes Modul mit src/main/ scannen
    SRC_DIRS=$(find "$APP_DIR" -type d -path '*/src/main' -not -path '*/build/*' 2>/dev/null || true)
    if [ -z "$SRC_DIRS" ]; then
        echo "FEHLER: Kein src/main/ in $APP_DIR gefunden." >&2
        exit 1
    fi
else
    SRC_DIRS="$SRC"
fi

OUT="$APP_DIR/string-extraktor-scan.md"
DATE=$(date +%Y-%m-%d)
TOTAL=0

cat > "$OUT" <<HEADER
# String-Extraktor: Initial-Scan-Bericht

**Datum:** $DATE
**App:** $(basename "$APP_DIR")
**Geprueft:** $SRC_DIRS

> Dieser Bericht ist die Eingabe fuer Phase 2 (AUDIT) und Phase 3 (CREATE) des
> string-extraktor Skills. Treffer werden hier roh aufgelistet — die Bewertung
> "echter hardcoded String vs. ignorierbar" macht der Skill in Phase 2.

---

HEADER

# Vollstaendiger Filter — passt zu Phase 1.6 in SKILL.md:
# Log/Timber/println/@Preview (nie sichtbar), test-Verzeichnisse (nicht in Produktion),
# analytics.log (Bezeichner), getString("...")-Aufrufe bei SharedPreferences (interne Keys),
# jsonObject.getString (Daten-Keys), Regex(...) (technisch), const val TAG (Logging).
FILTER_OUT='/test/\|/androidTest/\|Log\.\|Timber\.\|println\|@Preview\|analytics\.\|getString("[^"]*")\|jsonObject\.\|Regex(\|const val TAG'

count_hits() {
    local pattern="$1"
    local glob="$2"
    local count=0
    for dir in $SRC_DIRS; do
        if [ -d "$dir" ]; then
            local c
            c=$(grep -rEn "$pattern" "$dir" --include="$glob" 2>/dev/null \
                | grep -v "$FILTER_OUT" \
                | wc -l || true)
            count=$((count + c))
        fi
    done
    echo "$count"
}

dump_hits() {
    local pattern="$1"
    local glob="$2"
    local title="$3"
    local count=$(count_hits "$pattern" "$glob")
    TOTAL=$((TOTAL + count))
    {
        echo "## $title"
        echo ""
        echo "**Treffer:** $count"
        echo ""
        if [ "$count" -gt 0 ]; then
            echo '```'
            for dir in $SRC_DIRS; do
                [ -d "$dir" ] && grep -rEn "$pattern" "$dir" --include="$glob" 2>/dev/null \
                    | grep -v "$FILTER_OUT" \
                    | head -50 || true
            done
            echo '```'
            echo ""
            [ "$count" -gt 50 ] && echo "(+ $((count - 50)) weitere — siehe Vollausgabe)" && echo ""
        fi
    } >> "$OUT"
}

dump_hits 'Text\(\s*"[^"]*[a-zA-ZäöüÄÖÜß]'                          '*.kt'  'Pattern 1 — Compose Text() mit Literal'
dump_hits 'Text\(\s*"[^"]*\$|"[^"]*" \+'                             '*.kt'  'Pattern 2 — String-Templates und Konkatenation'
dump_hits 'Toast\.makeText\([^,]+,\s*"|showSnackbar\(\s*"'           '*.kt'  'Pattern 3 — Toast und Snackbar'
dump_hits 'contentDescription\s*=\s*"[^"]*[a-zA-ZäöüÄÖÜß]'          '*.kt'  'Pattern 4 — Content Descriptions (Accessibility)'
dump_hits 'placeholder\s*=.*Text\(\s*"|label\s*=.*Text\(\s*"'        '*.kt'  'Pattern 5 — Placeholder/Hint/Label'
dump_hits 'TopAppBar.*title|NavigationBarItem.*label'                '*.kt'  'Pattern 6 — TopAppBar/Navigation (manuell pruefen)'
dump_hits 'error\(\s*"[^"]*[a-zA-Z]'                                 '*.kt'  'Pattern 7 — Benutzersichtbare Fehler'
dump_hits 'enum class.*\{|sealed.*\{'                                '*.kt'  'Pattern 8 — Enum/Sealed Classes (manuell pruefen)'
dump_hits 'android:text="[^@][^"]*[a-zA-ZäöüÄÖÜß]'                   '*.xml' 'Pattern 9 — XML-Layouts mit android:text'
dump_hits '\\u00(dc|fc|d6|f6|c4|e4|df)'                              '*.kt'  'Pattern U — Unicode-Escapes fuer deutsche Umlaute (PFLICHT)'
dump_hits '&(Uuml|uuml|Ouml|ouml|Auml|auml|szlig);'                  '*'     'Pattern H — HTML-Entities (selten)'

{
    echo "---"
    echo ""
    echo "## Zusammenfassung"
    echo ""
    echo "- **Gesamt-Treffer:** $TOTAL"
    echo "- **Geprueft am:** $DATE"
    echo "- **Quellverzeichnisse:** $SRC_DIRS"
    echo ""
    echo "**Naechste Schritte:**"
    echo ""
    echo "1. Treffer in Pattern 1-9 als hardcodierte Strings extrahieren (Phase 3 des Skills)"
    echo "2. Pattern U-Treffer SOFORT als Strings extrahieren (deutsche Umlaute im Code)"
    echo "3. Pattern 6 und 8 manuell pruefen — false positives moeglich"
    echo "4. Filterregeln pruefen: Log, Timber, @Preview, /test/, /androidTest/ wurden ausgeschlossen"
} >> "$OUT"

echo "OK: $TOTAL Treffer gefunden — Bericht in $OUT"
