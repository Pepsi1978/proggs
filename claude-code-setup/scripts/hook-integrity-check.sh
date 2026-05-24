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

# --- 3. Cross-Platform-Lock-Lifecycle: .ps1/.sh-Paare mit Lock symmetrisch? ---
echo "--- 3. Cross-Platform-Lock-Lifecycle (.ps1 vs .sh) ---"
for ps in "$HOOKS_DIR"/*.ps1; do
    [ -f "$ps" ] || continue
    sh="${ps%.ps1}.sh"
    [ -f "$sh" ] || continue
    base=$(basename "${ps%.ps1}")
    # Nur pruefen wenn ueberhaupt mit einem Lock gearbeitet wird
    if grep -qi "lock" "$ps" 2>/dev/null || grep -qi "lock" "$sh" 2>/dev/null; then
        ps_clean=$(grep -ciE "(Remove-Item|del).*[Ll]ock" "$ps" 2>/dev/null || true)
        sh_clean=$(grep -ciE "rm .*[Ll]ock|unlink.*[Ll]ock" "$sh" 2>/dev/null || true)
        ps_clean=${ps_clean:-0}; sh_clean=${sh_clean:-0}
        if { [ "$ps_clean" -gt 0 ] && [ "$sh_clean" -gt 0 ]; } || { [ "$ps_clean" -eq 0 ] && [ "$sh_clean" -eq 0 ]; }; then
            ok "$base: Lock-Cleanup symmetrisch (ps1=$ps_clean, sh=$sh_clean; 0/0 = ggf. extern via TS)"
        else
            warn "$base: ASYMMETRIE im Lock-Cleanup (ps1=$ps_clean, sh=$sh_clean) — eine Plattform koennte Lock verwaisen lassen"
        fi
    fi
done

echo ""
echo "Hook-Integrity: $FAIL FAIL, $WARN WARN"
exit 0
