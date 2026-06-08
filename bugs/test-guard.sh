#!/usr/bin/env bash
# test-guard.sh — Regressionstest fuer den bug-almanac-guard Hook (Stufe-2-Erzwingung).
# Ausfuehren: bash bugs/test-guard.sh   (Exit 0 = alle Tests gruen, sonst Anzahl Fehler)
# Ruft den AKTIVEN Hook (~/.claude/hooks/bug-almanac-guard.sh) mit simulierten PreToolUse-Events auf.
# Nutzt ein ISOLIERTES TMPDIR -> stoert die echte Session NICHT. PS1-Pendant: bugs/test-guard.ps1.

set -uo pipefail
HOOK="$HOME/.claude/hooks/bug-almanac-guard.sh"
[ -f "$HOOK" ] || { echo "FEHLER: Hook nicht gefunden: $HOOK"; exit 99; }

export TMPDIR="$(mktemp -d)"
BUGS="$HOME/proggs/bugs"
fails=0

run()      { printf '%s' "$1" | bash "$HOOK" 2>/dev/null; }
decision() { printf '%s' "$1" | python3 -c "import json,sys; d=sys.stdin.read().strip(); print((json.loads(d).get('hookSpecificOutput',{}).get('permissionDecision','(none)')) if d else '(none)')" 2>/dev/null || echo "(none)"; }
check()    { if [ "$2" = "1" ]; then echo "[PASS] $1"; else echo "[FAIL] $1"; fails=$((fails+1)); fi; }
clearm()   { rm -f "$TMPDIR"/bug-almanac-*.flag 2>/dev/null || true; }
eq()       { [ "$1" = "$2" ] && echo 1 || echo 0; }
ne()       { [ "$1" != "$2" ] && echo 1 || echo 0; }
isempty()  { [ -z "$1" ] && echo 1 || echo 0; }
exists()   { [ -f "$1" ] && echo 1 || echo 0; }

# Kategorie-robust: Almanach rekursiv suchen (liegt jetzt in bugs/<kategorie>/<datei>.md).
alm_path() { find "$BUGS" -name "$1" -type f 2>/dev/null | head -1; }
if [ -z "$(alm_path kotlin.md)" ]; then
    echo "WARN: kotlin.md fehlt — Block-Tests uebersprungen"
else
    clearm
    check "leeres stdin -> kein Output"           "$(isempty "$(run '')")"
    check "Edit .kt ohne Lesen -> deny"           "$(eq "$(decision "$(run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')")" deny)"
    check "Write .kt ohne Lesen -> deny"          "$(eq "$(decision "$(run '{"tool_name":"Write","tool_input":{"file_path":"x/Foo.kt"}}')")" deny)"
    check "MultiEdit .kt ohne Lesen -> deny"      "$(eq "$(decision "$(run '{"tool_name":"MultiEdit","tool_input":{"file_path":"x/Foo.kt"}}')")" deny)"

    clearm
    run '{"tool_name":"Read","tool_input":{"file_path":"/p/proggs/bugs/android/kotlin.md"}}' >/dev/null
    check "Read absolut (Kategorie-Pfad) -> Marker gesetzt" "$(exists "$TMPDIR/bug-almanac-read-kotlin.flag")"
    check "Edit .kt nach Read -> kein deny"       "$(ne "$(decision "$(run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')")" deny)"

    clearm
    run '{"tool_name":"Read","tool_input":{"file_path":"bugs/android/kotlin.md"}}' >/dev/null
    check "Read relativ (Kategorie-Pfad) -> Marker gesetzt" "$(exists "$TMPDIR/bug-almanac-read-kotlin.flag")"

    clearm
    run '{"tool_name":"Bash","tool_input":{"command":"cat ~/proggs/bugs/android/kotlin.md"}}' >/dev/null
    check "Bash-cat Almanach -> Marker gesetzt"   "$(exists "$TMPDIR/bug-almanac-read-kotlin.flag")"
    check "Bash harmlos -> kein Output"           "$(isempty "$(run '{"tool_name":"Bash","tool_input":{"command":"echo hallo"}}')")"

    clearm
    touch "$TMPDIR/bug-almanac-disable.flag"
    check "Notaus aktiv -> kein deny"             "$(ne "$(decision "$(run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')")" deny)"
    clearm
fi

# Bereich ohne Almanach dynamisch waehlen (fuer Hinweis-Test)
missing=""
for pair in "Foo.swift:swift-appkit.md" "app.ts:typescript.md" "app.rs:rust.md"; do
    f="${pair%%:*}"; md="${pair##*:}"
    if [ -z "$(alm_path "$md")" ]; then missing="$f"; break; fi
done
if [ -n "$missing" ]; then
    check "Kein Almanach -> kein deny (nur Hinweis)" "$(ne "$(decision "$(run "{\"tool_name\":\"Edit\",\"tool_input\":{\"file_path\":\"x/$missing\"}}")")" deny)"
else
    echo "INFO: alle Test-Bereiche haben Almanache — 'kein Almanach'-Test uebersprungen"
fi

rm -rf "$TMPDIR"
echo ""
if [ "$fails" -eq 0 ]; then echo "ALLE TESTS GRUEN"; exit 0; else echo "$fails TEST(S) FEHLGESCHLAGEN"; exit "$fails"; fi
