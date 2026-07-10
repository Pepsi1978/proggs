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

# Sonde (Direktive #2 Selbstbeobachtung): schreibt EINE JSON-Zeile pro Block/Pass nach
# ~/.claude/state/bug-almanac-triggers.jsonl. Rein beobachtend — beeinflusst NIE die Entscheidung.
# JSON-Schreiben per python3 (kein jq — claude-hooks §13.2/§16.2). Schreibt NUR in die Datei, NIE
# auf stdout (sonst Session-Crash, §16.1). FAIL-OPEN via || true (set -e harmlos). Liest $input/$tool/$fp.
add_almanac_trigger() {
    # $1=event $2=block_type $3=slug $4=area $5=high_risk(0/1)
    stateDir="$HOME/.claude/state"
    mkdir -p "$stateDir" 2>/dev/null || true
    jsonl="$stateDir/bug-almanac-triggers.jsonl"
    if [ -f "$jsonl" ]; then
        sz=$(wc -c < "$jsonl" 2>/dev/null || echo 0)
        [ "${sz:-0}" -gt 5242880 ] && mv -f "$jsonl" "$jsonl.1" 2>/dev/null || true
    fi
    ALM_INPUT="$input" ALM_TOOL="$tool" ALM_FP="$fp" ALM_JSONL="$jsonl" \
    python3 - "$1" "$2" "$3" "$4" "$5" <<'PYEOF' 2>/dev/null || true
import json, sys, re, datetime, os
ev, bt, slug, area, hr = sys.argv[1:6]
try:
    d = json.loads(os.environ.get('ALM_INPUT', '') or '{}')
except Exception:
    d = {}
ti = d.get('tool_input') or {}
ex = ti.get('content') or ti.get('new_string') or ''
if not ex and ti.get('edits'):
    ex = '\n'.join((e.get('new_string') or '') for e in ti['edits'])
ex = ex[:300]
for p in [r'gho_[A-Za-z0-9]{20,}', r'ghp_[A-Za-z0-9]{20,}', r'sk-[A-Za-z0-9]{20,}', r'AIza[A-Za-z0-9_\-]{20,}']:
    ex = re.sub(p, '[REDACTED]', ex)
ex = re.sub(r'''(?i)(token|key|secret|password)(["']?\s*[:=]\s*["'])[^"']+''', r'\1\2[REDACTED]', ex)
o = {
    'ts': datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S'),
    'event': ev, 'block_type': bt, 'slug': slug, 'area': area,
    'tool': os.environ.get('ALM_TOOL', ''), 'file': os.environ.get('ALM_FP', ''),
    'change_excerpt': ex, 'high_risk': (hr == '1'),
    'session': d.get('session_id', '') or '',
}
try:
    with open(os.environ['ALM_JSONL'], 'a', encoding='utf-8') as f:
        f.write(json.dumps(o, ensure_ascii=False) + '\n')
except Exception:
    pass
PYEOF
}

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
                    # cat/bat/less liest immer den Volltext -> auch den full-Marker setzen (Digest-Modell Stufe C).
                    touch "$TMP/bug-almanac-full-$an.flag" 2>/dev/null || true
                fi
            done
        fi
        # Best-Practices-Datei per cat/bat/less gelesen -> "bp-gelesen"-Marker. Abwaertskompatibel:
        # NEU best-practices/<kat>/<bereich>.md | ALT .../best-practices-<bereich>.md. Schluessel = <bereich>.
        bpmatches=$(echo "$cl" | grep -oE 'best-practices/([a-z0-9._-]+/)*[a-z0-9._-]+\.md' || true)
        if [ -n "$bpmatches" ]; then
            echo "$bpmatches" | sed -E 's#.*/##; s#^best-practices-##; s#\.md$##' | while read -r bn; do
                case "$bn" in readme|system|_*) bn="";; esac
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
        # Digest-Modell: Volltext-Read (kein limit ODER limit >= 500) setzt zusaetzlich den full-Marker
        # (Stufe C). Kurzcheck-Read (z.B. limit=80) setzt nur den read-Marker.
        lim=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); print(int((d.get('tool_input') or {}).get('limit') or 0))
except Exception:
    print(0)
" 2>/dev/null || echo 0)
        if [ "${lim:-0}" -le 0 ] || [ "${lim:-0}" -ge 500 ]; then
            touch "$TMP/bug-almanac-full-$almName.flag" 2>/dev/null || true
        fi
    fi
    # Best-Practices-Datei gelesen -> "bp-gelesen"-Marker. Abwaertskompatibel (Migration 2026-06-16):
    # NEU best-practices/<kat>/<bereich>.md | ALT .../best-practices-<bereich>.md. Schluessel = <bereich>.
    bpName=$(echo "$fpl" | sed -nE 's#.*/best-practices/(.*/)?([^/]+)\.md$#\2#p' | sed -E 's#^best-practices-##')
    case "$bpName" in readme|system|_*) bpName="";; esac
    if [ -n "$bpName" ]; then
        touch "$TMP/bug-almanac-bp-read-$bpName.flag" 2>/dev/null || true
    fi
    exit 0
fi

# -- Test-Artefakt-Ausschluss (Frank-Entscheidung 2026-06-22 via almanach-trigger-auswertung):
# Selbsttest-Dateien des Almanach-/Guard-Systems (Dateiname enthaelt 'almtest', z.B. almtest-manifest.json)
# loesen den ECHTEN Guard NICHT aus und verschmutzen die Sonde nicht. ENG (nur 'almtest'), damit echte
# Unit-Tests (FooTest.kt/*.test.ts/_test.py) mit Logik nicht durchrutschen (Direktive #3). Nur Edit/Write/
# MultiEdit (Read/Bash sind oben schon mit exit 0 raus).
case "$fpl" in *almtest*) exit 0 ;; esac

# -- Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). --
slug=""; file=""; name=""
case "$fpl" in
    *compose.yaml|*compose.yml|*docker-compose.yml|*docker-compose.yaml|*dockerfile|*dockerfile.*|*.dockerfile)
        # Docker & Docker-Compose (Self-Hosting-Betrieb): compose.yaml/.yml, docker-compose.*,
        # Dockerfile (auch Dockerfile.prod / app.dockerfile). Dateiname-basiert, eindeutig.
        # Abgrenzung: Qdrant-/Agent-spezifisches faengt der jeweilige Almanach separat ab.
        slug="docker"; file="docker.md"; name="Docker & Docker-Compose (Self-Hosting-Betrieb)";;
    *caddyfile|*.caddyfile|*.service|*nginx.conf|*/sites-available/*|*/sites-enabled/*)
        # Reverse-Proxy/TLS (Caddy) + systemd-Units + nginx-Configs -> reverse-proxy-tls.md.
        # Caddyfile (kein Suffix), *.service (systemd-Unit), nginx.conf/sites-*. Eindeutige Dateimuster.
        # Abgrenzung: VPN -> wireguard.md, Docker-Betrieb -> docker.md, Security-Architektur -> self-hosted-ai-agent-server.md.
        slug="reverseproxytls"; file="reverse-proxy-tls.md"; name="Reverse-Proxy/TLS (Caddy) & Linux-VPS-Betrieb";;
    *.sdplugin/*|*propertyinspector*)
        # Stream-Deck-Plugin: Dateien im *.sdPlugin-Ordner ODER ein Property Inspector.
        # MUSS vor dem chrome-Zweig stehen, da ein Stream-Deck-manifest.json sonst vom
        # generischen '*manifest.json'-Match faelschlich als Chrome-Erweiterung erkannt wuerde.
        slug="streamdeck"; file="stream-deck.md"; name="Elgato Stream Deck Plugin-Entwicklung";;
    *.mcp.json)
        # MCP-Server-Registrierung (.mcp.json). Vor dem chrome-'manifest.json'-Zweig (kein
        # Suffix-Konflikt, aber explizit). MCP-Server-Quellcode wird im .ts/.py-Zweig per Content-Probe erkannt.
        slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)";;
    *opencode.json|*opencode.jsonc|*tui.json|*tui.jsonc|*/.opencode/*|*/.config/opencode/*)
        # OpenCode CLI: zentrale Config (opencode.json/tui.json) ODER projekt-/user-lokaler opencode-Ordner.
        # Eindeutige Muster, kein Konflikt mit .mcp.json/settings.json. (Coverage-Luecke geschlossen 2026-06-20.)
        slug="opencode"; file="opencode-cli.md"; name="OpenCode CLI (Config/Agents/Plugins)";;
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
    *database.kt|*dao.kt|*migration*.kt)
        # Room-Persistenz (Dateiname): Database/DAO/Migration. MUSS vor dem androidplatform- und dem
        # generischen .kt-Zweig stehen. Room hat seit 2026-06-15 einen eigenen Almanach (room.md);
        # frueher liefen *Database.kt/*Migration(s).kt faelschlich nach android-platform.md.
        # (@Entity/@Dao/@Database OHNE diese Dateinamen werden im .kt-Zweig per Content-Probe erkannt.)
        slug="room"; file="room.md"; name="Room-Persistenz (Entity/DAO/Database/Migration/TypeConverter/@Relation)";;
    *androidmanifest.xml)
        # Manifest separat (keine .kt): Platform-SDK (Permissions/Services/Receiver/Edge-to-Edge/targetSdk).
        # Der frueher hier mitgefangene service/receiver/worker.kt-Teil wandert in den .kt-case unten als
        # Dateiname-FALLBACK *nach* den spezifischen Inhalts-Signalen (voice/workmanager/drive) — so
        # verdeckt service.kt/worker.kt nicht mehr die feature-spezifischen Android-Almanache.
        slug="androidplatform"; file="android-platform.md"; name="Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager)";;
    *.xml)
        # App-Widget-Provider-XML (res/xml/*.xml mit <appwidget-provider ...>) -> app-widgets.md.
        # AndroidManifest ist oben schon abgefangen; sonstiges XML faellt durch (kein Fehlalarm) -
        # nur per appwidget-provider-Inhalt erkennen. Coverage-Erweiterung 2026-06-20.
        xmlProbe=""
        [ -f "$fp" ] && xmlProbe=$(cat "$fp" 2>/dev/null || true)
        xml_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string','') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
        xmlProbe="$xmlProbe
$xml_extra"
        case "$xmlProbe" in
          *appwidget-provider*) slug="appwidgets"; file="app-widgets.md"; name="App-Widgets (Glance/RemoteViews/AppWidgetProvider)";;
        esac
        # Network Security Config (Cleartext fuer private IP / interne-CA-trust-anchors) -> client-anbindung.md.
        if [ -z "$file" ]; then
          case "$fpl" in *network_security_config*) slug="clientanbindung"; file="client-anbindung.md"; name="Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST)";; esac
        fi
        if [ -z "$file" ]; then
          case "$xmlProbe" in *network-security-config*|*cleartextTrafficPermitted*) slug="clientanbindung"; file="client-anbindung.md"; name="Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST)";; esac
        fi
        ;;
    *.kt|*.kts)
        # .kt/.kts: ZENTRALE Android/Kotlin-Erkennung. Inhalt EINMAL proben, dann nach Prioritaet routen.
        # FAIL-OPEN (trap). Reihenfolge strikt funktionserhaltend: die spezifischen Feature-Signale
        # (voice/workmanager/drive) stehen VOR dem service/worker-Dateiname-Fallback, alle bestehenden
        # Signale (hilt/net/media3/coil/room/compose) bleiben unveraendert dahinter.
        composeSignal=0
        hiltSignal=0
        netSignal=0
        media3Signal=0
        coilSignal=0
        roomSignal=0
        voiceSignal=0
        workmanagerSignal=0
        driveSignal=0
        filamentSignal=0
        widgetSignal=0
        # Dateiname-Fallback: Service/Receiver/Worker -> android-platform (wie bisher, NUR wenn kein
        # spezifischeres Feature-Signal davor zieht).
        androidPlatformName=0
        case "$fpl" in *service.kt|*receiver.kt|*worker.kt) androidPlatformName=1;; esac
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
            # Retrofit/OkHttp/Moshi-Networking-Signale -> retrofit-okhttp-moshi.md (nach Hilt, vor Compose/Kotlin).
            case "$probe" in
              *retrofit2*|*Retrofit.Builder*|*okhttp3*|*OkHttpClient*|*HttpLoggingInterceptor*|*CertificatePinner*|*@JsonClass*|*com.squareup.moshi*|*Moshi.Builder*|*@GET*|*@POST*|*@PUT*|*@DELETE*|*@PATCH*|*@FormUrlEncoded*|*@Multipart*) netSignal=1;;
            esac
            # Media3/ExoPlayer-Audio-Wiedergabe-Signale -> media3-exoplayer.md (nach Hilt/Net, vor Compose/Kotlin).
            case "$probe" in
              *media3*|*ExoPlayer*|*MediaSession*|*MediaController*|*MediaItem*|*PlayerView*|*DefaultLoadControl*|*setMediaItem*) media3Signal=1;;
            esac
            # Coil-3-Bild-/Video-Laden-Signale -> coil3.md (nach Hilt/Net/Media3, vor Compose/Kotlin).
            case "$probe" in
              *coil3*|*io.coil-kt*|*AsyncImage*|*rememberAsyncImagePainter*|*SubcomposeAsyncImage*|*ImageLoader*|*ImageRequest*|*SingletonImageLoader*) coilSignal=1;;
            esac
            # Room-Persistenz-Signale (Entity/DAO/Database/TypeConverter ohne *Dao.kt/*Database.kt-Dateinamen)
            # -> room.md (nach Hilt/Net/Media3/Coil, vor Compose/Kotlin). @Query bewusst NICHT (Retrofit-Konflikt).
            case "$probe" in
              *@Entity*|*@Dao*|*@Database*|*@TypeConverter*|*@ProvidedTypeConverter*|*RoomDatabase*|*androidx.room*|*@PrimaryKey*|*@ColumnInfo*|*@Embedded*|*@AutoMigration*|*@Upsert*) roomSignal=1;;
            esac
            # Voice-Assistant-Ausloesung / Wake-Word / Mic -> voice-assistant-trigger.md (spezifisch, VOR android-platform).
            case "$probe" in
              *VoiceInteractionService*|*AccessibilityService*|*WakeWord*|*Hotword*|*KEYCODE_ASSIST*|*ACTION_ASSIST*|*sherpa-onnx*|*sherpa.onnx*|*sherpa_onnx*|*Porcupine*|*OpenWakeWord*|*NanoWakeWord*|*Shizuku*) voiceSignal=1;;
            esac
            # WorkManager & Notifications -> workmanager-notifications.md (spezifisch, VOR android-platform).
            case "$probe" in
              *WorkManager*|*PeriodicWorkRequest*|*OneTimeWorkRequest*|*CoroutineWorker*|*enqueueUnique*|*setExactAndAllowWhileIdle*|*setAndAllowWhileIdle*|*AlarmManager*|*NotificationChannel*|*NotificationCompat*|*NotificationManagerCompat*|*SCHEDULE_EXACT_ALARM*) workmanagerSignal=1;;
            esac
            # Google-Drive-Backup & Cloud-Sync -> google-drive-backup.md (spezifisch, VOR android-platform).
            case "$probe" in
              *appDataFolder*|*DriveScopes*|*DRIVE_APPDATA*|*com.google.api.services.drive*|*google-api-services-drive*|*AuthorizationClient*|*GoogleAuthUtil*) driveSignal=1;;
            esac
            # 3D auf Android (Filament/SceneView) -> 3d-filament-android.md (vor Compose/Kotlin).
            case "$probe" in
              *com.google.android.filament*|*Filament*|*ModelViewer*|*io.github.sceneview*|*SceneView*|*rememberEngine*) filamentSignal=1;;
            esac
            # App-Widgets (Glance + klassisch) -> app-widgets.md (Coverage-Luecke 2026-06-20). VOR compose
            # (Glance nutzt @Composable -> wuerde sonst als compose erkannt).
            case "$probe" in
              *GlanceAppWidget*|*GlanceAppWidgetReceiver*|*provideGlance*|*AppWidgetProvider*|*RemoteViewsService*|*RemoteViewsFactory*|*AppWidgetManager*|*updateAppWidget*|*appWidgetId*) widgetSignal=1;;
            esac
            ;;
        esac
        # *Module.kt (Dateiname) ist ein starkes Hilt/Dagger-DI-Signal, auch ohne Annotation im Probe.
        case "$fpl" in *module.kt) hiltSignal=1;; esac
        if [ "$widgetSignal" -eq 1 ]; then
            slug="appwidgets"; file="app-widgets.md"; name="App-Widgets (Glance/RemoteViews/AppWidgetProvider)"
        elif [ "$voiceSignal" -eq 1 ]; then
            slug="voiceassistant"; file="voice-assistant-trigger.md"; name="Voice-Assistant-Ausloesung / Wake-Word / Mic (Android)"
        elif [ "$workmanagerSignal" -eq 1 ]; then
            slug="workmanager"; file="workmanager-notifications.md"; name="WorkManager & Notifications (Reminder/Hintergrund)"
        elif [ "$driveSignal" -eq 1 ]; then
            slug="drivebackup"; file="google-drive-backup.md"; name="Google-Drive-Backup & Cloud-Sync (Android)"
        elif [ "$androidPlatformName" -eq 1 ]; then
            slug="androidplatform"; file="android-platform.md"; name="Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager)"
        elif [ "$hiltSignal" -eq 1 ]; then
            slug="hiltdagger"; file="hilt-dagger.md"; name="Hilt/Dagger Dependency Injection (KSP)"
        elif [ "$netSignal" -eq 1 ]; then
            slug="networking"; file="retrofit-okhttp-moshi.md"; name="Android-Networking (Retrofit/OkHttp/Moshi)"
        elif [ "$media3Signal" -eq 1 ]; then
            slug="media3"; file="media3-exoplayer.md"; name="Audio-Wiedergabe (Media3/ExoPlayer)"
        elif [ "$coilSignal" -eq 1 ]; then
            slug="coil3"; file="coil3.md"; name="Bild-/Video-Laden (Coil 3)"
        elif [ "$roomSignal" -eq 1 ]; then
            slug="room"; file="room.md"; name="Room-Persistenz (Entity/DAO/Database/Migration/TypeConverter/@Relation)"
        elif [ "$filamentSignal" -eq 1 ]; then
            slug="filament"; file="3d-filament-android.md"; name="3D auf Android (Filament/SceneView)"
        elif [ "$composeSignal" -eq 1 ]; then
            slug="compose"; file="jetpack-compose.md"; name="Jetpack Compose (Android-UI)"
        else
            slug="kotlin"; file="kotlin.md"; name="Kotlin (Sprache/K2/Coroutines/Compose-Kontext)"
        fi
        ;;
    *.swift|*.xcodeproj*|*/info.plist|info.plist|*.entitlements)
        # .swift: Content-Probe -> macOS-Overlay (NSPanel/...) / 3D-Metal-SceneKit (MTKView/SceneKit/RealityKit)
        #         / On-Device-Whisper (whisper.cpp/WhisperKit) -> sonst swift-appkit.md. Probe nur bei .swift
        #         (xcodeproj/plist/entitlements enthalten keinen Swift-Code). FAIL-OPEN (trap).
        macOverlaySignal=0; metalSignal=0; whisperSignal=0
        case "$fpl" in
          *.swift)
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
            case "$probe" in *whisper.cpp*|*WhisperKit*|*whisperkit*|*ggml*) whisperSignal=1;; esac
            case "$probe" in *nonactivatingPanel*|*NSPanel*|*collectionBehavior*|*canJoinAllSpaces*|*orderFrontRegardless*|*ignoresMouseEvents*|*LSUIElement*|*CGEventTap*) macOverlaySignal=1;; esac
            case "$probe" in *MTKView*|*MTLDevice*|*MTLCreateSystemDefaultDevice*|*SceneKit*|*SCNView*|*RealityKit*|*RealityView*|*MetalFX*|*MetalKit*) metalSignal=1;; esac
            ;;
        esac
        # Info.plist/entitlements mit ATS/Local-Network/network.client -> Client-Anbindung (VPN/REST ans eigene Backend).
        clientAnbindungSignal=0
        case "$fpl" in
          */info.plist|info.plist|*.entitlements)
            plistProbe=""
            [ -f "$fp" ] && plistProbe=$(cat "$fp" 2>/dev/null || true)
            plist_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string','') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
            plistProbe="$plistProbe
$plist_extra"
            case "$plistProbe" in *NSAppTransportSecurity*|*NSAllowsLocalNetworking*|*NSLocalNetworkUsageDescription*|*NSExceptionAllowsInsecureHTTPLoads*|*com.apple.security.network.client*) clientAnbindungSignal=1;; esac
            ;;
        esac
        if [ "$clientAnbindungSignal" -eq 1 ]; then
            slug="clientanbindung"; file="client-anbindung.md"; name="Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST)"
        elif [ "$whisperSignal" -eq 1 ]; then
            slug="whisperlokal"; file="whisper-stt-lokal.md"; name="On-Device-Whisper / lokale Transkription"
        elif [ "$macOverlaySignal" -eq 1 ]; then
            slug="macosoverlay"; file="macos-overlay.md"; name="macOS-Overlay-Fenster (Swift/AppKit)"
        elif [ "$metalSignal" -eq 1 ]; then
            slug="metalmacos"; file="3d-metal-scenekit-macos.md"; name="3D auf macOS (Metal/SceneKit/RealityKit)"
        else
            slug="swift"; file="swift-appkit.md"; name="macOS-Desktop (Swift/AppKit)"
        fi
        ;;
    *.ts|*.tsx|*tsconfig.json)
        # .ts/.tsx: MCP-Server-Quelle (@modelcontextprotocol/sdk etc.)? -> mcp-server.md, sonst typescript.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog Compose-Probe). FAIL-OPEN (trap).
        mcpSignal=0
        threejsSignal=0
        motionAssetSignal=0
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
            # 3D im Web (Three.js/Babylon/WebGPU/R3F) -> 3d-threejs-webgpu.md (nach MCP, vor typescript).
            case "$probe" in
              *THREE.*|*three/examples*|*@react-three/fiber*|*@react-three/drei*|*@babylonjs*|*WebGPURenderer*|*GLTFLoader*|*KTX2Loader*|*PMREMGenerator*) threejsSignal=1;;
            esac
            # Lottie/Rive/SVG-Animationen -> lottie-rive-svg-animationen.md (nach MCP, vor generischem TypeScript).
            case "$probe" in
              *lottie*|*bodymovin*|*@lottiefiles*|*@rive-app*|*useRive*|*RiveComponent*|*rive.wasm*|*RuntimeLoader*|*loadAnimation*) motionAssetSignal=1;;
            esac
            ;;
        esac
        if [ "$mcpSignal" -eq 1 ]; then
            slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)"
        elif [ "$motionAssetSignal" -eq 1 ]; then
            slug="lottierivesvg"; file="lottie-rive-svg-animationen.md"; name="Lottie / Rive / SVG-Animationen im Web"
        elif [ "$threejsSignal" -eq 1 ]; then
            slug="threejs"; file="3d-threejs-webgpu.md"; name="3D im Web (Three.js/Babylon/WebGPU)"
        else
            slug="typescript"; file="typescript.md"; name="TypeScript / Node"
        fi
        ;;
    *.riv|*.lottie|*lottie*.json|*bodymovin*.json|*rive*.json)
        slug="lottierivesvg"; file="lottie-rive-svg-animationen.md"; name="Lottie / Rive / SVG-Animationen im Web";;
    *.json)
        # Lottie-JSON ohne sprechenden Dateinamen nur per Inhalt erkennen, damit package.json/Configs nicht gekapert werden.
        jsonProbe=""
        [ -f "$fp" ] && jsonProbe=$(cat "$fp" 2>/dev/null || true)
        json_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
        jsonProbe="$jsonProbe
$json_extra"
        case "$jsonProbe" in
          *\"fr\"*:*\"layers\"*:*) slug="lottierivesvg"; file="lottie-rive-svg-animationen.md"; name="Lottie / Rive / SVG-Animationen im Web";;
        esac
        ;;
    *.svg)
        svgProbe=""
        [ -f "$fp" ] && svgProbe=$(cat "$fp" 2>/dev/null || true)
        svg_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
        svgProbe="$svgProbe
$svg_extra"
        case "$svgProbe" in
          *'<animate'*|*'<animateTransform'*|*'<animateMotion'*|*'<set'*|*'<mask'*|*'<clipPath'*|*pathLength*|*stroke-dasharray*) slug="lottierivesvg"; file="lottie-rive-svg-animationen.md"; name="Lottie / Rive / SVG-Animationen im Web";;
        esac
        ;;
    *.html|*.css|*.scss|*.sass)
        slug="webdesign"; file="webseitenbau-webdesign.md"; name="Webseitenbau / Webdesign / visuelle Effekte";;
    *.ico|*.icns)
        # App-Icon-Asset-Datei (.ico Windows / .icns macOS). Eindeutige Endung. Icon-Build-Skripte zusaetzlich per .py-Content-Probe.
        slug="iconbuilding"; file="icon-building.md"; name="App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)";;
    *.xaml|*.csproj|*.cs)
        # .cs: Content-Probe -> Groq-Transkription (GroqWhisperClient/audio/transcriptions/api.groq.com)
        #      ODER Wake-Word/Keyword-Spotting (sherpa-onnx/Porcupine/KeywordSpotter/...) -> sonst dotnet-csharp.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog MCP-/Compose-Probe). Nur .cs (XAML/csproj enthalten keinen STT-/KWS-Code). FAIL-OPEN (trap).
        groqSignal=0; wakeSignal=0; whisperSignal=0; dxSignal=0; winOverlaySignal=0; textInjectSignal=0
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
            # On-Device-Whisper / lokale Transkription -> whisper-stt-lokal.md (nach groq/wake; lokal, NICHT Cloud).
            case "$probe" in
              *Whisper.net*|*WhisperFactory*|*whisper.cpp*|*GgmlType*|*CTranslate2*|*faster-whisper*|*faster_whisper*) whisperSignal=1;;
            esac
            # 3D auf Windows (.NET DirectX/Stride/Silk.NET) -> 3d-dotnet-directx-windows.md.
            case "$probe" in
              *Stride.*|*Silk.NET*|*Vortice*|*Veldrid*|*SharpDX*|*Direct3D*|*D3D12*|*MonoGame*) dxSignal=1;;
            esac
            # Windows-Overlay-Fenster (WPF: always-on-top/click-through/global Hotkey) -> windows-overlay.md.
            case "$probe" in
              *WS_EX_NOACTIVATE*|*WS_EX_TOOLWINDOW*|*WS_EX_TRANSPARENT*|*WS_EX_LAYERED*|*RegisterHotKey*|*WH_KEYBOARD_LL*|*SetWindowPos*|*WindowChrome*|*AllowsTransparency*) winOverlaySignal=1;;
            esac
            # Text-Injection in fremde Electron/Chromium-Felder (Claude Desktop Chat/Code/Cowork) -> windows-electron-text-injection.md.
            # Spezifischer als der Overlay-Zweig: Chromium-Render-HWND / UIA-Befuellen / Scancode-Paste / A11y-Aktivierung.
            case "$probe" in
              *Chrome_RenderWidgetHostHWND*|*force-renderer-accessibility*|*KEYEVENTF_SCANCODE*|*EnumChildWindows*|*ValuePattern*|*TextPattern*|*FlaUI*|*DWMWA_EXTENDED_FRAME_BOUNDS*) textInjectSignal=1;;
            esac
            ;;
        esac
        if [ "$groqSignal" -eq 1 ]; then
            slug="groq"; file="groq-transkription.md"; name="Groq Whisper Transkription (Audio/STT)"
        elif [ "$wakeSignal" -eq 1 ]; then
            slug="wakeword"; file="wake-word.md"; name="Wake-Word / Keyword-Spotting (.NET/C#)"
        elif [ "$whisperSignal" -eq 1 ]; then
            slug="whisperlokal"; file="whisper-stt-lokal.md"; name="On-Device-Whisper / lokale Transkription"
        elif [ "$dxSignal" -eq 1 ]; then
            slug="dxwindows"; file="3d-dotnet-directx-windows.md"; name="3D auf Windows (.NET DirectX/Stride/Silk.NET)"
        elif [ "$textInjectSignal" -eq 1 ]; then
            slug="wineltextinject"; file="windows-electron-text-injection.md"; name="Text-Injection in Electron/Chromium-Felder (Windows, C#/WPF)"
        elif [ "$winOverlaySignal" -eq 1 ]; then
            slug="winoverlay"; file="windows-overlay.md"; name="Windows-Overlay-Fenster (C#/WPF)"
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
        # On-Device-Whisper / lokale Transkription (faster-whisper/whisper.cpp/CTranslate2) -> whisper-stt-lokal.md.
        whisperPy=0
        case "$probe" in
          *faster_whisper*|*faster-whisper*|*whisper.cpp*|*WhisperModel*|*ctranslate2*|*pywhispercpp*) whisperPy=1;;
        esac
        # Serverseitiger autonomer KI-Agent: Framework (pydantic-ai/langgraph) ODER eigene Tool-Loop im agent/-Ordner -> ai-agent-frameworks.md.
        agentPy=0
        case "$probe" in
          *pydantic_ai*|*pydantic-ai*|*"from langgraph"*|*"import langgraph"*|*langchain.agents*|*create_react_agent*|*StateGraph*) agentPy=1;;
        esac
        if [ "$agentPy" -eq 0 ]; then
          case "$fpl" in
            */agent/*.py|agent/*.py)
              case "$probe" in
                *generate_content*|*tool_use*|*system_instruction*|*llm_decide*|*brain_store*|*brain_search*) agentPy=1;;
              esac
              ;;
          esac
        fi
        # FastAPI/uvicorn-Web-Schicht (Endpoints/Middleware/lifespan/Body-Handling) -> fastapi.md. NACH mcp/agent
        # (agent/*.py bleibt ai-agent-frameworks), faengt brain-api/dashboard und andere FastAPI-Services.
        fastapiPy=0
        case "$probe" in
          *"from fastapi"*|*"import fastapi"*|*"FastAPI("*|*APIRouter*|*"@app.get"*|*"@app.post"*|*"@app.put"*|*"@app.delete"*|*"@app.patch"*|*uvicorn*) fastapiPy=1;;
        esac
        if [ "$mcpPy" -eq 1 ]; then
            slug="mcpserver"; file="mcp-server.md"; name="MCP-Server-Bau (Model Context Protocol)"
        elif [ "$agentPy" -eq 1 ]; then
            slug="aiagentframeworks"; file="ai-agent-frameworks.md"; name="Serverseitige autonome KI-Agenten (Loop/Tools/State/Kosten)"
        elif [ "$fastapiPy" -eq 1 ]; then
            slug="fastapi"; file="fastapi.md"; name="FastAPI & async-Python-Server (uvicorn-Web-Schicht)"
        elif [ "$iconPy" -eq 1 ]; then
            slug="iconbuilding"; file="icon-building.md"; name="App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)"
        elif [ "$whisperPy" -eq 1 ]; then
            slug="whisperlokal"; file="whisper-stt-lokal.md"; name="On-Device-Whisper / lokale Transkription"
        else
            slug="python"; file="python-windows.md"; name="Python (Windows-Encoding/Cross-Platform-Scripting)"
        fi
        ;;
    *.gd|*.tscn|*.tres|*.gdshader|*project.godot)
        # Godot 4 (GDScript/Szenen/Shader/Projekt) -> 3d-godot.md. Eindeutige Endungen, kein Konflikt.
        slug="godot"; file="3d-godot.md"; name="3D mit Godot 4";;
    *.conf)
        # WireGuard-Config (wg0.conf etc.): .conf ist generisch -> NUR per Inhalt erkennen (kein Fehlalarm
        # bei anderen .conf). Coverage-Luecke geschlossen 2026-06-20. Kein WG-Inhalt -> slug bleibt leer -> durch.
        wgProbe=""
        [ -f "$fp" ] && wgProbe=$(cat "$fp" 2>/dev/null || true)
        wg_extra=$(printf '%s' "$input" | python3 -c "import json,sys
try:
    d=json.load(sys.stdin); ti=d.get('tool_input') or {}
    parts=[ti.get('content','') or '', ti.get('new_string','') or '']
    for e in (ti.get('edits') or []): parts.append(e.get('new_string','') or '')
    print('\n'.join(parts))
except Exception:
    print('')
" 2>/dev/null || true)
        wgProbe="$wgProbe
$wg_extra"
        case "$wgProbe" in
          *'[Interface]'*)
            case "$wgProbe" in
              *'[Peer]'*|*PrivateKey*|*ListenPort*|*AllowedIPs*|*PersistentKeepalive*|*wg-quick*)
                slug="wireguard"; file="wireguard.md"; name="WireGuard VPN-Konfiguration";;
            esac
            ;;
        esac
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

# -- Rust-3D (Bevy/wgpu) VOR der generischen Endungs-Erkennung --
# 3d-rust-wgpu-bevy.md existiert, rust.md NICHT. Ohne diesen Check liefe eine .rs-Datei in den generischen
# rust.md-Platzhalter -> Fehlalarm "kein Almanach", obwohl 3d-rust-wgpu-bevy.md da ist. Greift nur bei
# 3D-Signal; reines Rust faellt unveraendert in den generischen rust.md-Platzhalter.
if [ -z "$slug" ]; then
    case "$fpl" in
        *.rs|*cargo.toml)
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
              *bevy*|*wgpu*|*Camera3d*|*Mesh3d*|*MeshMaterial3d*|*PbrBundle*|*StandardMaterial*) slug="3drust"; file="3d-rust-wgpu-bevy.md"; name="3D mit Rust (wgpu/Bevy)";;
            esac
            ;;
    esac
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
# Digest-Modell Stufe C: Hochrisiko-Bereiche (tickende/teure Fehlerklassen: Release, Geld, Harness)
# verlangen den VOLLTEXT (full-Marker), nicht nur den Kurzcheck. Liste = almKey (Dateiname ohne .md).
isHighRisk=0
case "$almKey" in r8|firebase-billing|claude-hooks|claude-config) isHighRisk=1;; esac
fullMarker="$TMP/bug-almanac-full-$almKey.flag"
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

# -- Trivial-Filter: reiner Version-Bump in JEDEM Bereich (Frank-Entscheidung 2026-06-20 +
# bereichsuebergreifend erweitert 2026-06-22, beide via almanach-trigger-auswertung). Version-Bump
# (nur versionCode/versionName, "version":, version=, <Version>... bei .csproj) aendert KEINE Logik ->
# kein Bug-Wissen noetig (known-bugs-before-coding nennt Versions-Bump als Kleinkram). Definition aus
# der GEMEINSAMEN Quelle (trivial_classify.py) -> gilt fuer gradle/npm/Cargo/.csproj gleichermassen.
# NUR wenn JEDE nicht-leere Edit-Zeile eine Versionszeile ist, NUR wenn sonst geblockt wuerde.
# Funktionserhaltend: jede andere Zeile -> normaler Block unten. Transparenz: pass/trivial-uebersprungen.
# JSON per python3 (kein jq - claude-hooks §16.2). FAIL-OPEN via || echo 0.
if [ "$disabled" -eq 0 ] && [ ! -f "$readMarker" ]; then
    # Trivial-Entscheidung aus der GEMEINSAMEN Quelle (bugs/trivial_classify.py is-version-bump-json)
    # -> Guard und Auswertung (aggregate.py) laufen nie auseinander (DRY, Direktive #1). FAIL-SAFE:
    # scheitert der Aufruf (python/Modul fehlt), greift der normale Block -> kein Funktionsverlust.
    tcScript="$HOME/proggs/bugs/trivial_classify.py"
    if [ -f "$tcScript" ] && printf '%s' "$input" | python3 "$tcScript" is-version-bump-json >/dev/null 2>&1; then
        add_almanac_trigger pass trivial-uebersprungen "$slug" "$name" "$isHighRisk"
        exit 0
    fi
fi

# -- ERZWINGUNG: Almanach existiert, Notaus aus, aber noch nicht gelesen -> BLOCKIEREN --
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ ! -f "$readMarker" ]; then
    # Block-Logging (persistent) — nur Beobachtung, beeinflusst nie die Entscheidung.
    stateDir="$HOME/.claude/state"
    mkdir -p "$stateDir" 2>/dev/null || true
    echo "$(date '+%Y-%m-%d %H:%M') $slug" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
    add_almanac_trigger block almanach-ungelesen "$slug" "$name" "$isHighRisk"
    if [ "$isHighRisk" -eq 1 ]; then
        reason="STOPP - Bug-Almanach-Pflicht (Digest-Modell Stufe C, Regel: known-bugs-before-coding). Du editierst eine Datei aus dem HOCHRISIKO-Bereich '$name', aber $almRel wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/$almRel KOMPLETT mit dem Read-Tool (OHNE limit - Hochrisiko verlangt den Volltext, der Kurzcheck reicht hier nicht) + Versions-Abgleich, DANN editiere erneut. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
    else
        reason="STOPP - Bug-Almanach-Pflicht (Digest-Modell Stufe A, Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '$name', aber der Kurzcheck von $almRel wurde in dieser Session noch NICHT gelesen. Lies JETZT NUR den Kurzcheck: Read auf ~/proggs/$almRel mit limit=80 (die Kurzcheck-Sektion oben in der Datei; der Volltext ist NICHT noetig - er wird erst beim ERSTEN FEHLER im Bereich Pflicht, Stufe B). DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
    fi
    python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"
    exit 0
fi

# -- Stufe C (Digest-Modell): Hochrisiko-Bereich verlangt den VOLLTEXT, nicht nur den Kurzcheck --
# Erreicht nur, wenn der read-Marker existiert (Kurzcheck oder mehr gelesen). Fehlt der full-Marker,
# wird bei Hochrisiko-Bereichen blockiert, bis ein Volltext-Read (Read ohne limit) erkannt wurde.
if [ -f "$almanachPath" ] && [ "$disabled" -eq 0 ] && [ "$isHighRisk" -eq 1 ] && [ -f "$readMarker" ] && [ ! -f "$fullMarker" ]; then
    # Transcript-Fallback (analog Almanach): ein Volltext-Read (Read OHNE limit-Feld im input-Objekt)
    # kann vom Read-Zweig verpasst worden sein (Matcher-Cache / Read+Edit-Race).
    tpath=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('transcript_path','') or '')" 2>/dev/null || echo "")
    if [ -n "$tpath" ] && [ -f "$tpath" ]; then
        if python3 - "$tpath" "$almKey" <<'PYEOF' 2>/dev/null
import re, sys
path, key = sys.argv[1], sys.argv[2]
pat = re.compile('file_path"\\s*:\\s*"[^"]*bugs[/\\\\]+(?:[^"/\\\\]+[/\\\\]+)*' + re.escape(key) + '\\.md"(?![^}]*"limit"\\s*:)')
try:
    with open(path, encoding='utf-8', errors='ignore') as f:
        sys.exit(0 if any(pat.search(line) for line in f) else 1)
except Exception:
    sys.exit(1)
PYEOF
        then
            touch "$fullMarker" 2>/dev/null || true
        fi
    fi
    if [ ! -f "$fullMarker" ]; then
        stateDir="$HOME/.claude/state"
        mkdir -p "$stateDir" 2>/dev/null || true
        echo "$(date '+%Y-%m-%d %H:%M') $slug (stufe-c-volltext)" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
        add_almanac_trigger block volltext-c "$slug" "$name" 1
        reason="STOPP - Volltext-Pflicht (Digest-Modell Stufe C, Regel: known-bugs-before-coding). '$name' ist ein HOCHRISIKO-Bereich (tickende/teure Fehlerklasse: Release, Geld, Harness). Der Kurzcheck von $almRel ist gelesen, aber der VOLLTEXT noch nicht. Oeffne ~/proggs/$almRel KOMPLETT mit dem Read-Tool (OHNE limit), DANN editiere erneut. (Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
        python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"
        exit 0
    fi
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
        bpRoot="$HOME/proggs/best-practices"
        # Abwaertskompatibel: NEU <bereich>.md ODER ALT best-practices-<bereich>.md.
        bpPath="$(find "$bpRoot" \( -name "$almKey.md" -o -name "best-practices-$almKey.md" \) -type f 2>/dev/null | head -1 || true)"
        if [ -n "$bpPath" ]; then
            bpRel="best-practices/${bpPath#*/best-practices/}"
            # Transcript-Fallback (analog Almanach): Read evtl. vom Read-Hook verpasst (Matcher-Cache / Read+Edit-Race).
            tpath=$(printf '%s' "$input" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('transcript_path','') or '')" 2>/dev/null || echo "")
            if [ -n "$tpath" ] && [ -f "$tpath" ]; then
                if grep -qE 'file_path"[[:space:]]*:[[:space:]]*"[^"]*best-practices[/-][^"]*'"$almKey"'\.md' "$tpath" 2>/dev/null; then
                    touch "$bpReadMarker" 2>/dev/null || true
                fi
            fi
            if [ ! -f "$bpReadMarker" ]; then
                stateDir="$HOME/.claude/state"
                mkdir -p "$stateDir" 2>/dev/null || true
                echo "$(date '+%Y-%m-%d %H:%M') $slug (best-practices)" >> "$stateDir/bug-almanac-blocks.log" 2>/dev/null || true
                add_almanac_trigger block best-practices "$slug" "$name" "$isHighRisk"
                reason="STOPP - Best-Practices-Pflicht (Regel: known-bugs-before-coding). Der Bug-Almanach fuer '$name' ist gelesen - aber die zugehoerige Best-Practices-Datei $bpRel in dieser Session noch NICHT. Reihenfolge: erst Almanach (erledigt), dann Best Practices, DANN editieren. Oeffne ZUERST ~/proggs/$bpRel mit dem Read-Tool (Kurzcheck mit limit=80 reicht - Digest-Modell Stufe A; so macht man es von vornherein richtig, damit der Bug gar nicht erst entsteht), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen.)"
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
        bt="already-read"; [ "$disabled" -eq 1 ] && bt="disabled"
        add_almanac_trigger pass "$bt" "$slug" "$name" "$isHighRisk"
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
        bt="ack"; [ "$disabled" -eq 1 ] && bt="disabled"
        add_almanac_trigger pass "$bt" "$slug" "$name" 0
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
add_almanac_trigger block kein-almanach "$slug" "$name" 0
reason="STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du arbeitest an '$name', aber es gibt noch KEINEN Almanach (bugs/$file). Zwei Wege: (1) Frank kurz um OK bitten und dann den Skill 'bug-almanach-recherche' STARTEN (der vorgeschriebene, vollstaendige Weg - NICHT selbst ad hoc recherchieren); ODER (2) wenn das nur trivialer Kleinkram ist (String/Doku/Versions-Bump) bzw. Frank gegen eine Recherche entscheidet: die Quittung anlegen - leere Datei '$ackMarker' (touch) - danach ist der Bereich fuer diese Session frei. Notaus bei Fehlalarm: leere Datei $TMP/bug-almanac-disable.flag anlegen."
python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput':{'hookEventName':'PreToolUse','permissionDecision':'deny','permissionDecisionReason':sys.argv[1]}}))" "$reason"

exit 0
