#!/usr/bin/env bash
# bug-almanac-guard: Stufe 2 (ERZWINGUNG) des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Bei Edit/Write/MultiEdit an bereichstypischen Dateien: BLOCKIERT (permissionDecision=deny), wenn fuer
#   den Bereich ein Almanach existiert, dieser aber in DIESER Session noch nicht gelesen wurde.
# Bei Read einer bugs/<X>.md ODER Bash-cat/bat/less auf bugs/<X>.md: setzt den "gelesen"-Marker (NIE blockierend).
# Kein Almanach fuer den Bereich? -> nur erinnern (Stufe 1), NICHT blockieren (Recherche braucht Franks OK).
# Notaus: existiert $TMPDIR/bug-almanac-disable.flag -> nie blockieren (nur erinnern). Sicherheitsventil.
# Block-Logging: jeder Block wird (Datum + slug) nach ~/.claude/state/bug-almanac-blocks.log geschrieben (persistent).
# FAIL-OPEN: jeder interne Fehler -> exit 0 OHNE deny (durchlassen).
# WICHTIG (claude-hooks.md 1.6): NICHT exit 2 zum Blocken (blockt Write/Edit nicht) -> permissionDecision=deny + exit 0.
# Runs as PreToolUse hook (matcher: Read|Edit|Write|MultiEdit|Bash). Platform: macOS/Linux.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
trap 'hook_log_warn "bug-almanac-guard: Error at line $LINENO"; exit 0' ERR

TMP="${TMPDIR:-/tmp}"
input=$(cat)
[ -n "$input" ] || exit 0
tool=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('tool_name','') or '')" 2>/dev/null || echo "")

# -- Bash-Zweig: cat/bat/less etc. auf bugs/<X>.md -> "gelesen"-Marker, NIE blockieren --
if [ "$tool" = "Bash" ]; then
    cmd=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print((d.get('tool_input') or {}).get('command','') or '')" 2>/dev/null || echo "")
    if [ -n "$cmd" ]; then
        cl=$(echo "$cmd" | tr '[:upper:]' '[:lower:]' | tr '\\' '/')
        # grep ohne Treffer = exit 1 -> mit pipefail/set -e abfangen (|| true), sonst Log-Spam pro Bash-Call.
        matches=$(echo "$cl" | grep -oE 'bugs/[a-z0-9._-]+\.md' || true)
        if [ -n "$matches" ]; then
            echo "$matches" | sed -E 's#bugs/([a-z0-9._-]+)\.md#\1#' | while read -r an; do
                if [ -n "$an" ] && [ "$an" != "readme" ] && [ "$an" != "system" ]; then
                    touch "$TMP/bug-almanac-read-$an.flag" 2>/dev/null || true
                fi
            done
        fi
    fi
    exit 0
fi

fp=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print((d.get('tool_input') or {}).get('file_path','') or '')" 2>/dev/null || echo "")
[ -n "$fp" ] || exit 0
fpl=$(echo "$fp" | tr '[:upper:]' '[:lower:]' | tr '\\' '/')

# -- Read-Zweig: "gelesen"-Marker setzen, NIE blockieren (relativ + absolut) --
if [ "$tool" = "Read" ]; then
    almName=$(echo "$fpl" | sed -nE 's#(^|.*/)bugs/([^/]+)\.md$#\2#p')
    if [ -n "$almName" ] && [ "$almName" != "readme" ] && [ "$almName" != "system" ]; then
        touch "$TMP/bug-almanac-read-$almName.flag" 2>/dev/null || true
    fi
    exit 0
fi

# -- Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). --
slug=""; file=""; name=""
case "$fpl" in
    *manifest.json|*/overlays/*|*background.js|*service-worker.js|*vorlese-overlay*)
        slug="chrome"; file="chrome-extensions.md"; name="Browser-Erweiterungen (Chrome/Edge MV3)";;
    *google-services*.json|*billing*.kt|*subscription*.kt|*purchase*.kt)
        # Firebase-/Billing-Backend: google-services.json + Billing/Subscription/Purchase-Klassen.
        # MUSS vor dem androidplatform- und dem generischen .kt-Zweig stehen.
        slug="firebasebilling"; file="firebase-billing.md"; name="Firebase / Crashlytics / Play Billing (Google-Backend-Dienste)";;
    *build.gradle|*build.gradle.kts|*settings.gradle|*settings.gradle.kts|*/gradle/*|*gradle.properties|*gradle-wrapper*)
        slug="gradle"; file="gradle.md"; name="Build - Gradle (AGP/R8)";;
    *androidmanifest.xml|*service.kt|*receiver.kt|*worker.kt|*migration.kt|*migrations.kt|*database.kt)
        # Framework/Runtime-Dateien: Manifest (Permissions/Services/Receiver) + Service/Receiver/Worker/Room-DB/Migration.
        # Diese enthalten kein @Composable -> kein Konflikt mit dem Compose/Kotlin-Zweig (muss VORHER stehen).
        slug="androidplatform"; file="android-platform.md"; name="Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager/Room-Runtime)";;
    *.kt|*.kts)
        # .kt/.kts: Compose-UI-Datei (@Composable/setContent)? -> jetpack-compose.md, sonst kotlin.md.
        # Inhalt aus existierender Datei UND aus dem Tool-Input pruefen. FAIL-OPEN (trap faengt Fehler).
        composeSignal=0
        case "$fpl" in
          *.kt|*.kts)
            probe=""
            [ -f "$fp" ] && probe=$(cat "$fp" 2>/dev/null || true)
            ti_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string','') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
            probe="$probe
$ti_extra"
            case "$probe" in *@Composable*|*setContent*) composeSignal=1;; esac
            ;;
        esac
        if [ "$composeSignal" -eq 1 ]; then
            slug="compose"; file="jetpack-compose.md"; name="Jetpack Compose (Android-UI)"
        else
            slug="kotlin"; file="kotlin.md"; name="Kotlin (Sprache/K2/Coroutines/Compose-Kontext)"
        fi
        ;;
    *.swift|*.xcodeproj*|*/info.plist|info.plist|*.entitlements)
        slug="swift"; file="swift-appkit.md"; name="macOS-Desktop (Swift/AppKit)";;
    *.ts|*.tsx|*tsconfig.json)
        slug="typescript"; file="typescript.md"; name="TypeScript / Node";;
    *.user.js)
        slug="tampermonkey"; file="tampermonkey.md"; name="Tampermonkey/Userscripts";;
    *.xaml|*.csproj|*.cs)
        slug="dotnet"; file="dotnet-csharp.md"; name="C#/.NET (WPF, WinUI, Konsole, Backend)";;
    *.py)
        slug="python"; file="python-windows.md"; name="Python (Windows-Encoding/Cross-Platform-Scripting)";;
    */hooks/*.ps1|*/hooks/*.sh)
        slug="claudehooks"; file="claude-hooks.md"; name="Claude-Harness Hooks (PowerShell/Bash)";;
esac
[ -n "$slug" ] || exit 0

almanachPath="$HOME/proggs/bugs/$file"
almKey=$(echo "$file" | sed 's/\.md$//' | tr '[:upper:]' '[:lower:]')
readMarker="$TMP/bug-almanac-read-$almKey.flag"
seenMarker="$TMP/bug-almanac-seen-$slug.flag"
disabled=0; [ -f "$TMP/bug-almanac-disable.flag" ] && disabled=1

# -- Robustheits-Fallback (Fix 2026-06-02): Read-Marker via Transkript nachziehen --
# Read-Hook kann das Read verpasst haben: Matcher-Cache in der Hook-Aenderungs-Session
# (Hook-Config gecacht — claude-hooks.md TL;DR Punkt 3) ODER Race bei Read+Edit im selben Block.
# Bevor blockiert wird: fehlt der Read-Marker, im Transkript nach einem Tool-Call mit file_path
# auf bugs/<almanach>.md suchen (unabhaengig vom Read-Hook). Block-Reasons haben den Pfad NICHT
# als file_path -> kein Self-Unblock. Laeuft nur im Fehlalarm-Fall (Marker fehlt), also selten.
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ ! -f "$readMarker" ]; then
    tpath=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('transcript_path','') or '')" 2>/dev/null || echo "")
    if [ -n "$tpath" ] && [ -f "$tpath" ]; then
        if grep -qE 'file_path"[[:space:]]*:[[:space:]]*"[^"]*bugs[/\\]+'"$almKey"'\.md' "$tpath" 2>/dev/null; then
            touch "$readMarker" 2>/dev/null || true
        fi
    fi
fi

# -- ERZWINGUNG: Almanach existiert, Notaus aus, aber noch nicht gelesen -> BLOCKIEREN --
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ ! -f "$readMarker" ]; then
    # Block-Logging (persistent) — nur Beobachtung, beeinflusst nie die Entscheidung.
    stateDir="$HOME/.claude/state"
    mkdir -p "$stateDir" 2>/dev/null || true
    echo "$(date '+%Y-%m-%d %H:%M') $slug" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
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
