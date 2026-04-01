#!/usr/bin/env bash
# check-uistate-coverage.sh
#
# Prüft ob alle Felder in *UiState data classes tatsächlich im
# zugehörigen *Screen.kt konsumiert werden.
#
# Verwendung:
#   ./scripts/check-uistate-coverage.sh
#
# Exit-Code:
#   0 = alle Felder werden konsumiert
#   1 = mindestens ein Feld ist ungenutzt (potentieller Dead State)
#
# Hinweis: Das Script sucht anhand des Namens — "JournalUiState" → JournalScreen.kt
# Funktioniert für alle Screens die der Konvention *UiState / *Screen.kt folgen.

set -euo pipefail

SRC="app/src/main/java/com/bestjournal/app"
VIEWMODEL_DIR="$SRC/ui/screens"
FOUND_ISSUES=0

# Wechsle ins Projektverzeichnis (Script kann von überall aufgerufen werden)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo "=== UiState Coverage Check ==="
echo "Projekt: $PROJECT_DIR"
echo ""

# Finde alle *ViewModel.kt die eine *UiState data class enthalten
while IFS= read -r viewmodel_file; do
    screen_name=$(basename "$viewmodel_file" ViewModel.kt)   # z.B. "Journal"
    screen_file=$(find "$VIEWMODEL_DIR" -name "${screen_name}Screen.kt" 2>/dev/null | head -1)

    if [[ -z "$screen_file" ]]; then
        echo "⚠  Kein Screen gefunden für: $viewmodel_file"
        continue
    fi

    # Extrahiere alle Felder aus der UiState data class
    # Sucht nach: val fieldName: Type (in einer data class *UiState)
    state_class="${screen_name}UiState"
    in_class=false
    brace_depth=0

    while IFS= read -r line; do
        # Erkennt Beginn der UiState class
        if echo "$line" | grep -qE "data class ${state_class}\s*[(\{]"; then
            in_class=true
        fi

        if $in_class; then
            brace_depth=$(echo "$line" | awk '{
                for(i=1; i<=length($0); i++) {
                    c = substr($0,i,1)
                    if(c == "(" || c == "{") d++
                    if(c == ")" || c == "}") d--
                }
                print d
            }' | tail -1)

            # Extrahiere Feldnamen: val xyz: Type oder val xyz =
            while IFS= read -r field; do
                field=$(echo "$field" | sed 's/^ *//' | sed 's/:.*//' | sed 's/=.*//' | tr -d ' ')
                [[ -z "$field" ]] && continue
                [[ "$field" == "val" ]] && continue

                # Prüfe ob das Feld im Screen konsumiert wird
                if ! grep -q "\b${field}\b" "$screen_file" 2>/dev/null; then
                    echo "❌  UNGENUTZT: $state_class.$field"
                    echo "     Definiert in: $viewmodel_file"
                    echo "     Nicht gefunden in: $screen_file"
                    echo ""
                    FOUND_ISSUES=1
                fi
            done < <(echo "$line" | grep -oE 'val [a-zA-Z][a-zA-Z0-9]*' | awk '{print $2}')

            # Ende der Klasse (brace_depth kehrt zu 0 oder negativ zurück)
            if $in_class && [[ "$brace_depth" -le 0 ]] && [[ "$brace_depth" != "" ]]; then
                # Nur beenden wenn wir schon in der Klasse waren und depth <= 0
                in_class=false
            fi
        fi
    done < "$viewmodel_file"

done < <(find "$VIEWMODEL_DIR" -name "*ViewModel.kt" 2>/dev/null)

echo "=== Ergebnis ==="
if [[ $FOUND_ISSUES -eq 0 ]]; then
    echo "✅  Alle UiState-Felder werden in ihren Screens konsumiert."
    exit 0
else
    echo "❌  Es wurden ungenuzte UiState-Felder gefunden (siehe oben)."
    echo "     Tipp: Entweder das Feld im Screen verwenden oder aus UiState entfernen."
    exit 1
fi
