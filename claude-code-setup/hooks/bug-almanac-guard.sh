#!/usr/bin/env bash
# bug-almanac-guard: Stufe 2 (ERZWINGUNG) des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Bei Edit/Write an bereichstypischen Dateien: BLOCKIERT (permissionDecision=deny), wenn fuer den
#   Bereich ein Almanach existiert, dieser aber in DIESER Session noch nicht per Read geoeffnet wurde.
# Bei Read einer bugs/<X>.md: setzt den "gelesen"-Marker (NIE blockierend).
# Kein Almanach fuer den Bereich? -> nur erinnern (Stufe 1), NICHT blockieren (Recherche braucht Franks OK).
# Notaus: existiert $TMPDIR/bug-almanac-disable.flag -> nie blockieren (nur erinnern). Sicherheitsventil.
# FAIL-OPEN: jeder interne Fehler -> exit 0 OHNE deny (durchlassen).
# WICHTIG (Almanach 1.6): NICHT exit 2 zum Blocken (blockt Write/Edit nicht) -> permissionDecision=deny + exit 0.
# Runs as PreToolUse hook (matcher: Read|Edit|Write). Platform: macOS/Linux.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
trap 'hook_log_warn "bug-almanac-guard: Error at line $LINENO"; exit 0' ERR

TMP="${TMPDIR:-/tmp}"
input=$(cat)
[ -n "$input" ] || exit 0
# tool_name + file_path via python3 statt jq (jq kann fehlen -> stummes Versagen; siehe claude-hooks.md 13.2).
tool=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('tool_name','') or '')" 2>/dev/null || echo "")
fp=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print((d.get('tool_input') or {}).get('file_path','') or '')" 2>/dev/null || echo "")
[ -n "$fp" ] || exit 0
fpl=$(echo "$fp" | tr '[:upper:]' '[:lower:]' | tr '\\' '/')

# -- Read-Zweig: "gelesen"-Marker setzen, NIE blockieren --
# Marker-Key = Almanach-Dateiname ohne .md (z.B. bugs/kotlin.md -> "kotlin").
if [ "$tool" = "Read" ]; then
    almName=$(echo "$fpl" | sed -n 's#.*/bugs/\([^/]*\)\.md$#\1#p')
    if [ -n "$almName" ] && [ "$almName" != "readme" ] && [ "$almName" != "system" ]; then
        touch "$TMP/bug-almanac-read-$almName.flag" 2>/dev/null || true
    fi
    exit 0
fi

# -- Edit/Write-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). --
slug=""; file=""; name=""
case "$fpl" in
    *manifest.json|*/overlays/*|*background.js|*service-worker.js|*vorlese-overlay*)
        slug="chrome"; file="chrome-extensions.md"; name="Browser-Erweiterungen (Chrome/Edge MV3)";;
    *build.gradle|*build.gradle.kts|*settings.gradle|*settings.gradle.kts|*/gradle/*|*gradle.properties|*gradle-wrapper*)
        slug="gradle"; file="gradle.md"; name="Build - Gradle (AGP/R8)";;
    *.kt|*.kts|*androidmanifest.xml)
        slug="kotlin"; file="kotlin.md"; name="Kotlin (Sprache/K2/Coroutines/Compose-Kontext)";;
    *.swift)
        slug="swift"; file="swift-appkit.md"; name="macOS-Desktop (Swift/AppKit)";;
    *.ts|*.tsx|*tsconfig.json)
        slug="typescript"; file="typescript.md"; name="TypeScript / Node";;
    *.user.js)
        slug="tampermonkey"; file="tampermonkey.md"; name="Tampermonkey/Userscripts";;
    *.xaml|*.csproj|*.cs)
        slug="dotnet"; file="dotnet-csharp.md"; name="C#/.NET (WPF, WinUI, Konsole, Backend)";;
    */hooks/*.ps1|*/hooks/*.sh)
        slug="claudehooks"; file="claude-hooks.md"; name="Claude-Harness Hooks (PowerShell/Bash)";;
esac
[ -n "$slug" ] || exit 0

almanachPath="$HOME/proggs/bugs/$file"
almKey=$(echo "$file" | sed 's/\.md$//' | tr '[:upper:]' '[:lower:]')
readMarker="$TMP/bug-almanac-read-$almKey.flag"
seenMarker="$TMP/bug-almanac-seen-$slug.flag"
disabled=0; [ -f "$TMP/bug-almanac-disable.flag" ] && disabled=1

# -- ERZWINGUNG: Almanach existiert, Notaus aus, aber noch nicht gelesen -> BLOCKIEREN --
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ ! -f "$readMarker" ]; then
    reason="STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '$name', aber bugs/$file wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/bugs/$file mit dem Read-Tool (komplett + Versions-Abgleich), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; das Lesen kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
    python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"
    exit 0
fi

# -- Almanach existiert + bereits gelesen (oder Notaus): einmalige sanfte Bestaetigung, dann frei --
if [ -f "$almanachPath" ]; then
    if [ ! -f "$seenMarker" ]; then
        touch "$seenMarker" 2>/dev/null || true
        if [ "$disabled" -eq 1 ]; then
            msg="BUG-ALMANACH-HINWEIS: Bereich '$name' - Notaus aktiv (bug-almanac-disable.flag), kein Lese-Zwang. Lies bugs/$file freiwillig."
        else
            msg="BUG-ALMANACH: bugs/$file wurde gelesen - Bereich '$name' ist fuer diese Session freigegeben."
        fi
        python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':sys.argv[1]}}))" "$msg"
    fi
    exit 0
fi

# -- Kein Almanach: nur erinnern (Stufe 1), NICHT blockieren - Recherche braucht Franks OK --
[ -f "$seenMarker" ] && exit 0
touch "$seenMarker" 2>/dev/null || true
msg="BUG-ALMANACH-HINWEIS: Du arbeitest an $name, aber es gibt noch KEINEN Almanach (bugs/$file). Hol Franks kurzes OK und STARTE dann den Skill 'bug-almanach-recherche' - das ist der vorgeschriebene, vollstaendige Weg; NICHT selbst ad hoc recherchieren."
python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':sys.argv[1]}}))" "$msg"

exit 0
