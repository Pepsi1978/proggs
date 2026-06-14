#!/usr/bin/env bash
# bug-almanac-guard: Stufe 2 (ERZWINGUNG) des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Bei Edit/Write/MultiEdit an bereichstypischen Dateien: BLOCKIERT (permissionDecision=deny), wenn fuer
#   den Bereich ein Almanach existiert, dieser aber in DIESER Session noch nicht gelesen wurde.
# DANACH (zweite Stufe): existiert eine best-practices-<bereich>.md, BLOCKIERT der Hook weiter, bis auch sie
#   gelesen ist. Reihenfolge automatisch erzwungen: erst Almanach (was schiefgeht), dann Best Practices
#   (wie man es richtig macht), DANN editieren. Keine BP-Datei fuer den Bereich -> nur Almanach zaehlt.
# Bei Read einer bugs/<X>.md / best-practices-<X>.md ODER Bash-cat/bat/less darauf: setzt den "gelesen"-Marker (NIE blockierend).
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
        matches=$(echo "$cl" | grep -oE 'bugs/([a-z0-9._-]+/)*[a-z0-9._-]+\.md' || true)
        if [ -n "$matches" ]; then
            echo "$matches" | sed -E 's#.*/([a-z0-9._-]+)\.md#\1#' | while read -r an; do
                if [ -n "$an" ] && [ "$an" != "readme" ] && [ "$an" != "system" ]; then
                    touch "$TMP/bug-almanac-read-$an.flag" 2>/dev/null || true
                fi
            done
        fi
        # Best-Practices-Datei per cat/bat/less gelesen -> "bp-gelesen"-Marker (Schluessel = Bereich ohne 'best-practices-'-Praefix).
        bpmatches=$(echo "$cl" | grep -oE 'best-practices-[a-z0-9._-]+\.md' || true)
        if [ -n "$bpmatches" ]; then
            echo "$bpmatches" | sed -E 's#best-practices-([a-z0-9._-]+)\.md#\1#' | while read -r bn; do
                [ -n "$bn" ] && touch "$TMP/bug-almanac-bp-read-$bn.flag" 2>/dev/null || true
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
    almName=$(echo "$fpl" | sed -nE 's#(^|.*/)bugs/(.*/)?([^/]+)\.md$#\3#p')
    if [ -n "$almName" ] && [ "$almName" != "readme" ] && [ "$almName" != "system" ]; then
        touch "$TMP/bug-almanac-read-$almName.flag" 2>/dev/null || true
    fi
    # Best-Practices-Datei gelesen -> "bp-gelesen"-Marker (Schluessel = Bereich ohne 'best-practices-'-Praefix).
    bpName=$(echo "$fpl" | sed -nE 's#.*/best-practices-([a-z0-9._-]+)\.md$#\1#p')
    if [ -n "$bpName" ]; then
        touch "$TMP/bug-almanac-bp-read-$bpName.flag" 2>/dev/null || true
    fi
    exit 0
fi

# -- Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). --
slug=""; file=""; name=""
case "$fpl" in
    *.sdplugin/*|*propertyinspector*)
        # Stream-Deck-Plugin: Dateien im *.sdPlugin-Ordner ODER ein Property Inspector.
        # MUSS vor dem chrome-Zweig stehen, da ein Stream-Deck-manifest.json sonst vom
        # generischen '*manifest.json'-Match faelschlich als Chrome-Erweiterung erkannt wuerde.
        slug="streamdeck"; file="stream-deck.md"; name="Elgato Stream Deck Plugin-Entwicklung";;
    *.mcp.json)
        # MCP-Server-Registrierung (.mcp.json). Vor dem chrome-'manifest.json'-Zweig (kein
        # Suffix-Konflikt, aber explizit). MCP-Server-Quellcode wird im .ts/.py-Zweig per Content-Probe erkannt.
        slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)";;
    *manifest.json|*/overlays/*|*background.js|*service-worker.js|*vorlese-overlay*)
        slug="chrome"; file="chrome-extensions.md"; name="Browser-Erweiterungen (Chrome/Edge MV3)";;
    *google-services*.json|*billing*.kt|*subscription*.kt|*purchase*.kt)
        # Firebase-/Billing-Backend: google-services.json + Billing/Subscription/Purchase-Klassen.
        # MUSS vor dem androidplatform- und dem generischen .kt-Zweig stehen.
        slug="firebasebilling"; file="firebase-billing.md"; name="Firebase / Crashlytics / Play Billing (Google-Backend-Dienste)";;
    *proguard*.pro|*consumer*.pro|*.keep.xml)
        # R8/ProGuard-Regeln + Resource-keep (*.keep.xml). MUSS vor dem gradle-Zweig stehen:
        # build.gradle* bleibt gradle.md, R8-Regeldateien -> r8.md.
        slug="r8"; file="r8.md"; name="R8 (Code-Shrinker/Optimizer/Obfuscator)";;
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
        hiltSignal=0
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
            # Hilt/Dagger-DI-Signale (Annotationen im Datei-/Tool-Input) -> hilt-dagger.md (hat Vorrang).
            case "$probe" in
              *@HiltAndroidApp*|*@AndroidEntryPoint*|*@HiltViewModel*|*@HiltWorker*|*@InstallIn*|*@Module*|*@AssistedInject*|*@Provides*|*@Binds*) hiltSignal=1;;
            esac
            ;;
        esac
        # *Module.kt (Dateiname) ist ein starkes Hilt/Dagger-DI-Signal, auch ohne Annotation im Probe.
        case "$fpl" in *module.kt) hiltSignal=1;; esac
        if [ "$hiltSignal" -eq 1 ]; then
            slug="hiltdagger"; file="hilt-dagger.md"; name="Hilt/Dagger Dependency Injection (KSP)"
        elif [ "$composeSignal" -eq 1 ]; then
            slug="compose"; file="jetpack-compose.md"; name="Jetpack Compose (Android-UI)"
        else
            slug="kotlin"; file="kotlin.md"; name="Kotlin (Sprache/K2/Coroutines/Compose-Kontext)"
        fi
        ;;
    *.swift|*.xcodeproj*|*/info.plist|info.plist|*.entitlements)
        slug="swift"; file="swift-appkit.md"; name="macOS-Desktop (Swift/AppKit)";;
    *.ts|*.tsx|*tsconfig.json)
        # .ts/.tsx: MCP-Server-Quelle (@modelcontextprotocol/sdk etc.)? -> mcp-server.md, sonst typescript.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog Compose-Probe). FAIL-OPEN (trap).
        mcpSignal=0
        case "$fpl" in
          *.ts|*.tsx)
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
            case "$probe" in
              *@modelcontextprotocol/sdk*|*McpServer*|*StdioServerTransport*|*StreamableHTTPServerTransport*|*setRequestHandler*) mcpSignal=1;;
            esac
            ;;
        esac
        if [ "$mcpSignal" -eq 1 ]; then
            slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)"
        else
            slug="typescript"; file="typescript.md"; name="TypeScript / Node"
        fi
        ;;
    *.ico|*.icns)
        # App-Icon-Asset-Datei (.ico Windows / .icns macOS). Eindeutige Endung. Icon-Build-Skripte zusaetzlich per .py-Content-Probe.
        slug="iconbuilding"; file="icon-building.md"; name="App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)";;
    *.xaml|*.csproj|*.cs)
        # .cs: Content-Probe -> Groq-Transkription (GroqWhisperClient/audio/transcriptions/api.groq.com)
        #      ODER Wake-Word/Keyword-Spotting (sherpa-onnx/Porcupine/KeywordSpotter/...) -> sonst dotnet-csharp.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog MCP-/Compose-Probe). Nur .cs (XAML/csproj enthalten keinen STT-/KWS-Code). FAIL-OPEN (trap).
        groqSignal=0; wakeSignal=0
        case "$fpl" in
          *.cs)
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
            case "$probe" in
              *GroqWhisperClient*|*audio/transcriptions*|*api.groq.com*) groqSignal=1;;
            esac
            case "$probe" in
              *KeywordSpotter*|*sherpa-onnx*|*sherpa.onnx*|*sherpa_onnx*|*SherpaOnnx*|*Porcupine*|*NanoWakeWord*|*OpenWakeWord*|*WakeWord*|*wakeword*|*wake-word*|*wake_word*) wakeSignal=1;;
            esac
            ;;
        esac
        if [ "$groqSignal" -eq 1 ]; then
            slug="groq"; file="groq-transkription.md"; name="Groq Whisper Transkription (Audio/STT)"
        elif [ "$wakeSignal" -eq 1 ]; then
            slug="wakeword"; file="wake-word.md"; name="Wake-Word / Keyword-Spotting (.NET/C#)"
        else
            slug="dotnet"; file="dotnet-csharp.md"; name="C#/.NET (WPF, WinUI, Konsole, Backend)"
        fi
        ;;
    *.py)
        # .py: MCP-Server-Quelle (mcp/FastMCP)? -> mcp-server.md, sonst python-windows.md. FAIL-OPEN (trap).
        mcpPy=0
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
        case "$probe" in
          *FastMCP*|*mcp.server*|*"from mcp"*|*"import mcp"*|*stdio_server*) mcpPy=1;;
        esac
        # Icon-Build-Skript (Pillow-Alpha-Check/iconutil/Android-adaptive/WPF-ApplicationIcon)? -> icon-building.md.
        iconPy=0
        case "$probe" in
          *icns*|*iconutil*|*getchannel*|*ic_launcher*|*ApplicationIcon*) iconPy=1;;
        esac
        if [ "$mcpPy" -eq 1 ]; then
            slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)"
        elif [ "$iconPy" -eq 1 ]; then
            slug="iconbuilding"; file="icon-building.md"; name="App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)"
        else
            slug="python"; file="python-windows.md"; name="Python (Windows-Encoding/Cross-Platform-Scripting)"
        fi
        ;;
    */hooks/*.ps1|*/hooks/*.sh)
        slug="claudehooks"; file="claude-hooks.md"; name="Claude-Harness Hooks (PowerShell/Bash)";;
    */claude.md|claude.md|*/rules/*.md|*/settings.json|settings.json|*/settings.local.json|*/settings-reference.json|*/skill.md|*/commands/*.md|*/agents/*.md)
        # Claude-Code-Konfiguration: CLAUDE.md, rules/*.md, settings(.local/-reference).json, SKILL.md, commands/*.md, agents/*.md.
        # Hooks (.ps1/.sh) faengt der claudehooks-Case oben ab; MEMORY.md bewusst NICHT (zu haeufig automatisch beschrieben).
        slug="claudeconfig"; file="claude-config.md"; name="Claude-Code Konfiguration und Regeln (CLAUDE.md/Rules/Settings/Skills/Commands/Agents)";;
esac
# Selbst-Ausschluss: Almanach-/Best-Practices-.md duerfen claudeconfig NIE triggern (sind selbst .md).
if [ "$slug" = "claudeconfig" ]; then
    case "$fpl" in */bugs/*|*/best-practices/*) slug=""; file=""; name="";; esac
fi

# -- Generische Code-Erkennung (Luecke B, 2026-06-07): bekannte Programmiersprachen-Endung OHNE eigenes Mapping. --
# Greift NUR, wenn oben kein spezifischer Bereich erkannt wurde. So bekommt auch eine erste Rust-/Go-/Ruby-/
# Java-Datei (= neuer Bereich ohne Almanach) Zaehne, statt still durchzurutschen. Nur echte Programmiersprachen-
# Endungen. Der abgeleitete file-Name (z.B. rust.md) existiert noch nicht -> faellt unten in den
# "kein Almanach"-Quittungszweig. Legt Frank spaeter bugs/<kat>/rust.md an, findet ihn der find-Filter
# automatisch -> dann greift die normale Almanach-Erzwingung. FAIL-OPEN bleibt (trap).
if [ -z "$slug" ]; then
    case "$fpl" in
        *.rs)     slug="rust";    file="rust.md";    name="neuer Code-Bereich (Rust)";;
        *.go)     slug="go";      file="go.md";      name="neuer Code-Bereich (Go)";;
        *.rb)     slug="ruby";    file="ruby.md";    name="neuer Code-Bereich (Ruby)";;
        *.java)   slug="java";    file="java.md";    name="neuer Code-Bereich (Java)";;
        *.php)    slug="php";     file="php.md";     name="neuer Code-Bereich (PHP)";;
        *.lua)    slug="lua";     file="lua.md";     name="neuer Code-Bereich (Lua)";;
        *.c|*.cc|*.cpp|*.cxx|*.h|*.hpp) slug="cpp"; file="cpp.md"; name="neuer Code-Bereich (C/C++)";;
        *.dart)   slug="dart";    file="dart.md";    name="neuer Code-Bereich (Dart/Flutter)";;
        *.vue)    slug="vue";     file="vue.md";     name="neuer Code-Bereich (Vue)";;
        *.svelte) slug="svelte";  file="svelte.md";  name="neuer Code-Bereich (Svelte)";;
        *.ex|*.exs) slug="elixir"; file="elixir.md"; name="neuer Code-Bereich (Elixir)";;
        *.clj)    slug="clojure"; file="clojure.md"; name="neuer Code-Bereich (Clojure)";;
        *.scala)  slug="scala";   file="scala.md";   name="neuer Code-Bereich (Scala)";;
        *.hs)     slug="haskell"; file="haskell.md"; name="neuer Code-Bereich (Haskell)";;
        *.zig)    slug="zig";     file="zig.md";     name="neuer Code-Bereich (Zig)";;
        *.nim)    slug="nim";     file="nim.md";     name="neuer Code-Bereich (Nim)";;
        *.pl)     slug="perl";    file="perl.md";    name="neuer Code-Bereich (Perl)";;
        *.groovy) slug="groovy";  file="groovy.md";  name="neuer Code-Bereich (Groovy)";;
    esac
fi
[ -n "$slug" ] || exit 0

# Kategorie-robust (2026-06-03): Almanache liegen in Kategorie-Unterordnern (bugs/<kategorie>/<file>).
# Den Almanach per Suche nach dem Dateinamen finden, statt einen festen Pfad zu raten — so muss dieser
# Hook NICHT angefasst werden, wenn eine Datei die Kategorie wechselt. '|| true' faengt SIGPIPE (head) ab.
bugsRoot="$HOME/proggs/bugs"
almanachPath="$(find "$bugsRoot" -name "$file" -type f 2>/dev/null | head -1 || true)"
if [ -n "$almanachPath" ]; then almRel="bugs/${almanachPath#"$bugsRoot"/}"; else almRel="bugs/$file"; fi
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
        if grep -qE 'file_path"[[:space:]]*:[[:space:]]*"[^"]*bugs[/\\]+([^"/\\]+[/\\]+)*'"$almKey"'\.md' "$tpath" 2>/dev/null; then
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
    reason="STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '$name', aber $almRel wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/$almRel mit dem Read-Tool (komplett + Versions-Abgleich), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; das Lesen kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
    python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"
    exit 0
fi

# -- BP-ERZWINGUNG (zweite Seite der Medaille): Almanach gelesen, aber best-practices-<bereich>.md noch nicht --
# Reihenfolge automatisch erzwungen: dieser Block wird nur erreicht, wenn der Almanach-Block oben durchfiel
# (= Almanach gelesen ODER Notaus). Greift NUR wenn Almanach existiert+gelesen, Notaus aus und eine
# best-practices-<almKey>.md unter best-practices/projekt-code/ existiert. Sonst (keine BP-Datei): durchlassen.
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ -f "$readMarker" ]; then
    bpReadMarker="$TMP/bug-almanac-bp-read-$almKey.flag"
    # Lazy (Perf): den find-Verzeichnis-Scan NUR ausfuehren, wenn die BP-Datei in dieser Session noch
    # nicht als gelesen markiert ist. Marker schon da (haeufigster Fall nach einmaligem BP-Lesen +
    # vielen Folge-Edits) -> find entfaellt komplett. Verhaltensneutral: das Ergebnis wuerde dann
    # ohnehin nur zu "kein Block" fuehren.
    if [ ! -f "$bpReadMarker" ]; then
        bpRoot="$HOME/proggs/best-practices/projekt-code"
        bpPath="$(find "$bpRoot" -name "best-practices-$almKey.md" -type f 2>/dev/null | head -1 || true)"
        if [ -n "$bpPath" ]; then
            bpRel="best-practices/${bpPath#*/best-practices/}"
            # Transcript-Fallback (analog Almanach): Read evtl. vom Read-Hook verpasst (Matcher-Cache / Read+Edit-Race).
            tpath=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('transcript_path','') or '')" 2>/dev/null || echo "")
            if [ -n "$tpath" ] && [ -f "$tpath" ]; then
                if grep -qE 'file_path"[[:space:]]*:[[:space:]]*"[^"]*best-practices-'"$almKey"'\.md' "$tpath" 2>/dev/null; then
                    touch "$bpReadMarker" 2>/dev/null || true
                fi
            fi
            if [ ! -f "$bpReadMarker" ]; then
                stateDir="$HOME/.claude/state"
                mkdir -p "$stateDir" 2>/dev/null || true
                echo "$(date '+%Y-%m-%d %H:%M') $slug (best-practices)" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
                reason="STOPP - Best-Practices-Pflicht (Regel: known-bugs-before-coding). Der Bug-Almanach fuer '$name' ist gelesen - aber die zugehoerige Best-Practices-Datei $bpRel in dieser Session noch NICHT. Reihenfolge: erst Almanach (erledigt), dann Best Practices, DANN editieren. Oeffne ZUERST ~/proggs/$bpRel mit dem Read-Tool (so macht man es von vornherein richtig, damit der Bug gar nicht erst entsteht), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
                python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"
                exit 0
            fi
        fi
    fi
fi

# -- Almanach existiert + bereits gelesen (oder Notaus): einmalige sanfte Bestaetigung, dann frei --
if [ -f "$almanachPath" ]; then
    if [ ! -f "$seenMarker" ]; then
        touch "$seenMarker" 2>/dev/null || true
        if [ "$disabled" -eq 1 ]; then
            msg="BUG-ALMANACH-HINWEIS: Bereich '$name' - Notaus aktiv (bug-almanac-disable.flag), kein Lese-Zwang. Lies $almRel (+ Best Practices) freiwillig."
        else
            msg="BUG-ALMANACH: $almRel gelesen, Best-Practices-Pflicht erfuellt - Bereich '$name' ist fuer diese Session freigegeben."
        fi
        python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':sys.argv[1]}}))" "$msg"
    fi
    exit 0
fi

# -- Kein Almanach: BLOCKIEREN mit Quittung (Stufe 2, 2026-06-07) - Recherche/Entscheidung braucht Franks OK --
# Frueher nur zahnloser additionalContext-Hinweis (wurde uebersehen; im Block-Log nie ein "kein-almanach"-Eintrag).
# Jetzt deny, bis eine bewusste Geste ihn aufhebt: Quittung 'bug-almanac-ack-<slug>.flag' (nach Franks OK / bei
# Kleinkram angelegt) ODER globaler Notaus. So bekommt der "neuer Bereich"-Trigger echte Zaehne.
ackMarker="$TMP/bug-almanac-ack-$slug.flag"
if [ "$disabled" -eq 1 ] || [ -f "$ackMarker" ]; then
    # Quittung gesetzt oder Notaus aktiv -> frei. Einmalige sanfte Bestaetigung (seenMarker gegen Spam).
    if [ ! -f "$seenMarker" ]; then
        touch "$seenMarker" 2>/dev/null || true
        if [ "$disabled" -eq 1 ]; then
            msg="BUG-ALMANACH-HINWEIS: Bereich '$name' ohne Almanach (bugs/$file) - Notaus aktiv, freigegeben."
        else
            msg="BUG-ALMANACH-HINWEIS: Bereich '$name' ohne Almanach (bugs/$file) - Quittung gesetzt, fuer diese Session freigegeben."
        fi
        python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':sys.argv[1]}}))" "$msg"
    fi
    exit 0
fi
# Block-Logging (persistent) - nur Beobachtung, beeinflusst nie die Entscheidung.
stateDir="$HOME/.claude/state"
mkdir -p "$stateDir" 2>/dev/null || true
echo "$(date '+%Y-%m-%d %H:%M') $slug (kein-almanach)" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
reason="STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du arbeitest an '$name', aber es gibt noch KEINEN Almanach (bugs/$file). Zwei Wege: (1) Frank kurz um OK bitten und dann den Skill 'bug-almanach-recherche' STARTEN (der vorgeschriebene, vollstaendige Weg - NICHT selbst ad hoc recherchieren); ODER (2) wenn das nur trivialer Kleinkram ist (String/Doku/Versions-Bump) bzw. Frank gegen eine Recherche entscheidet: die Quittung anlegen - leere Datei '$ackMarker' (touch) - danach ist der Bereich fuer diese Session frei. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen."
python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"

exit 0
