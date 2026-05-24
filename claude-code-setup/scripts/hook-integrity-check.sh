#!/usr/bin/env bash
# hook-integrity-check.sh
# Prueft kontext-injizierende Hooks auf zwei Fehlerklassen, die am 2026-05-24 real auftraten:
#   (1) Hook gibt INVALIDES JSON aus (Encoding/Steuerzeichen) -> Claude Code verwirft den Kontext still.
#   (2) .ps1-Hook ohne UTF-8-OutputEncoding -> Sonderzeichen (Pfeil/Blitz/Gluehbirne) werden verfaelscht.
#   (3) .ps1/.sh-Hook-Paar mit Lock aber asymmetrischem Cleanup -> verwaister Lock auf einer Plattform.
#
# Aufruf (z.B. aus /self-improve Stufe 6):
#   bash ~/.claude/scripts/hook-integrity-check.sh
#
# Nicht-blockierend: exit 0 immer. FAIL/WARN nur informativ im Bericht.

HOOKS_DIR="$HOME/.claude/hooks"
FAIL=0
WARN=0
ok()   { echo "  OK   $*"; }
warn() { echo "  WARN $*"; WARN=$((WARN + 1)); }
fail() { echo "  FAIL $*"; FAIL=$((FAIL + 1)); }

echo "==> Hook-Integrity-Check (JSON + Encoding + Cross-Platform-Lifecycle)"

# --- 1. Kontext-injizierende Hooks: geben sie striktes, valides UTF-8-JSON aus? ---
echo "--- 1. JSON-Integritaet (strikter Parser, wie Claude Code) ---"
for f in "$HOOKS_DIR"/*.ps1 "$HOOKS_DIR"/*.sh; do
    [ -f "$f" ] || continue
    grep -q "additionalContext" "$f" 2>/dev/null || continue
    base=$(basename "$f")
    out=""
    case "$f" in
        *.ps1)
            if ! command -v pwsh > /dev/null 2>&1; then
                warn "$base: pwsh nicht verfuegbar — .ps1 nicht dynamisch testbar"
                continue
            fi
            out=$(pwsh -NoProfile -File "$f" 2>/dev/null)
            ;;
        *.sh)
            out=$(bash "$f" 2>/dev/null)
            ;;
    esac
    if [ -z "$out" ]; then
        # Bedingte Hooks (PostToolUse/PrePush) geben ohne echtes Event KEINEN Output aus.
        # Leer ist daher KORREKT (kein JSON-Bruch) — nicht als FAIL werten.
        ok "$base: kein Output ohne Event (bedingter Hook) — korrekt"
    elif printf '%s' "$out" | python3 -c "import sys,json; json.loads(sys.stdin.buffer.read().decode('utf-8','strict'))" 2>/dev/null; then
        ok "$base: striktes UTF-8-JSON valide"
    else
        fail "$base: INVALIDES JSON (Encoding/Steuerzeichen) — Kontext wuerde verworfen"
    fi
done

# --- 2. .ps1-Hooks die JSON ausgeben: UTF-8-OutputEncoding gesetzt? ---
echo "--- 2. UTF-8-OutputEncoding in JSON-ausgebenden .ps1-Hooks ---"
for f in "$HOOKS_DIR"/*.ps1; do
    [ -f "$f" ] || continue
    grep -q "additionalContext" "$f" 2>/dev/null || continue
    base=$(basename "$f")
    if grep -q "OutputEncoding" "$f" 2>/dev/null; then
        ok "$base: setzt OutputEncoding (Sonderzeichen sicher)"
    else
        warn "$base: KEIN [Console]::OutputEncoding=UTF8 — Sonderzeichen koennten zu ? verfaelschen"
    fi
done

# --- 3. Lock-Lifecycle: erstellt ein Hook eine .lock-Datei, ohne sie je freizugeben? ---
# Das ist die ECHTE Bug-Klasse hinter dem verwaisten Reindex-Lock (2026-05-24): explizit eine
# Lock-Datei anlegen, aber nie entfernen. Pro-Datei-Pruefung (kein fragiler Cross-Platform-
# Zaehlvergleich). flock/FD-Locks geben automatisch frei -> kein Leak, gelten als freigegeben.
echo "--- 3. Lock-Lifecycle: erstellt-aber-nicht-freigegeben? ---"
LOCK_PAT='\.lock|[Ll]ock[Ff]ile|LOCK_FILE'
for f in "$HOOKS_DIR"/*.ps1 "$HOOKS_DIR"/*.sh; do
    [ -f "$f" ] || continue
    grep -qE "$LOCK_PAT" "$f" 2>/dev/null || continue
    base=$(basename "$f")
    # Erstellt der Hook explizit eine Lock-Datei?
    creates=$(grep -ciE "CreateNew|New-Item.*($LOCK_PAT)|Out-File.*($LOCK_PAT)|(echo|printf).*>.*($LOCK_PAT)" "$f" 2>/dev/null || true)
    # Gibt er sie frei — explizit (Remove/rm/unlink) ODER selbst-freigebend (flock/FD-Redirect)?
    releases=$(grep -ciE "Remove-Item.*($LOCK_PAT)|(rm |unlink).*($LOCK_PAT)|flock|[0-9]+>.*($LOCK_PAT)" "$f" 2>/dev/null || true)
    creates=${creates:-0}; releases=${releases:-0}
    if [ "$creates" -gt 0 ] && [ "$releases" -eq 0 ]; then
        warn "$base: erstellt Lock-Datei ($creates) aber kein Cleanup/flock erkennbar -> evtl. verwaister Lock (wie Bug 4)"
    elif [ "$creates" -gt 0 ]; then
        ok "$base: Lock-Datei wird erstellt UND freigegeben ($releases)"
    fi
done

echo ""
echo "Hook-Integrity: $FAIL FAIL, $WARN WARN"
exit 0
