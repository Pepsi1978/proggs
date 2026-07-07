# bug-almanac-guard: Stufe 2 (ERZWINGUNG) des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Bei Edit/Write/MultiEdit an bereichstypischen Dateien: BLOCKIERT (permissionDecision=deny), wenn fuer
#   den Bereich ein Almanach existiert, dieser aber in DIESER Session noch nicht gelesen wurde.
# DANACH (zweite Stufe): existiert eine best-practices-<bereich>.md, BLOCKIERT der Hook weiter, bis auch sie
#   gelesen ist. Reihenfolge automatisch erzwungen: erst Almanach (was schiefgeht), dann Best Practices
#   (wie man es richtig macht), DANN editieren. Keine BP-Datei fuer den Bereich -> nur Almanach zaehlt.
# Bei Read einer bugs/<X>.md / best-practices-<X>.md ODER Bash-cat/bat/less darauf: setzt den "gelesen"-Marker (NIE blockierend).
# Kein Almanach fuer den Bereich? -> nur erinnern (Stufe 1), NICHT blockieren (Recherche braucht Franks OK).
# Notaus: existiert $TEMP/bug-almanac-disable.flag -> nie blockieren (nur erinnern). Sicherheitsventil.
# Block-Logging: jeder Block wird (Datum + slug) nach ~/.claude/state/bug-almanac-blocks.log geschrieben (persistent).
# FAIL-OPEN: jeder interne Fehler -> exit 0 OHNE deny (durchlassen).
# WICHTIG (claude-hooks.md 1.6): NICHT exit 2 zum Blocken (blockt Write/Edit nicht) -> permissionDecision=deny + exit 0.
# Runs as PreToolUse hook (matcher: Read|Edit|Write|MultiEdit|Bash).
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

# ── Sonde (Direktive #2 Selbstbeobachtung): schreibt EINE JSON-Zeile pro Block/Pass nach
# ~/.claude/state/bug-almanac-triggers.jsonl. Rein beobachtend — beeinflusst NIE die
# Block-Entscheidung. Schreibt NUR in die Datei, NIE auf stdout (sonst Session-Crash, claude-hooks §16.1).
# FAIL-OPEN: jeder Fehler wird verschluckt. Liest $data/$tool/$fp aus dem Hook-Scope.
function Add-AlmanacTrigger {
    param(
        [string]$EventType,
        [string]$BlockType,
        [string]$Slug,
        [string]$Area,
        [bool]$HighRisk
    )
    try {
        $stateDir = Join-Path $env:USERPROFILE ".claude/state"
        if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
        $jsonl = Join-Path $stateDir "bug-almanac-triggers.jsonl"
        # Rotation: bei >5 MB einmalig nach .1 wegrollen (genau eine Vorgaengerdatei).
        try { if ((Test-Path $jsonl) -and ((Get-Item $jsonl).Length -gt 5MB)) { Move-Item -Path $jsonl -Destination "$jsonl.1" -Force -ErrorAction SilentlyContinue } } catch {}
        # change_excerpt aus dem Tool-Input (content / new_string / edits[].new_string), ~300 Zeichen.
        $excerpt = ""
        try {
            $ti = $data.tool_input
            if ($ti.content)        { $excerpt = [string]$ti.content }
            elseif ($ti.new_string) { $excerpt = [string]$ti.new_string }
            elseif ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $excerpt += [string]$e.new_string + "`n" } } }
        } catch {}
        if ($excerpt.Length -gt 300) { $excerpt = $excerpt.Substring(0, 300) }
        # Secret-Maskierung (observability-first §8).
        $excerpt = $excerpt -replace 'gho_[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'ghp_[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'sk-[A-Za-z0-9]{20,}','[REDACTED]' `
                            -replace 'AIza[A-Za-z0-9_\-]{20,}','[REDACTED]'
        $excerpt = [regex]::Replace($excerpt, '(?i)(token|key|secret|password)(["'']?\s*[:=]\s*["''])[^"'']+', '$1$2[REDACTED]')
        $sid = ""
        try { $sid = [string]$data.session_id } catch {}
        $obj = [ordered]@{
            ts             = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss")
            event          = $EventType
            block_type     = $BlockType
            slug           = $Slug
            area           = $Area
            tool           = $tool
            file           = $fp
            change_excerpt = $excerpt
            high_risk      = $HighRisk
            session        = $sid
        }
        Add-Content -Path $jsonl -Value ($obj | ConvertTo-Json -Compress -Depth 5) -Encoding UTF8 -ErrorAction SilentlyContinue
    } catch {}
}

try {
    # stdin robust lesen (mal Console.In, mal $input je nach Invokation).
    $raw = ""
    try { $raw = [Console]::In.ReadToEnd() } catch {}
    if ([string]::IsNullOrWhiteSpace($raw)) {
        try { $raw = $input | Out-String } catch {}
    }
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }

    $data = $raw | ConvertFrom-Json
    $tool = [string]$data.tool_name

    # ── Bash-Zweig: cat/bat/less etc. auf bugs/<X>.md -> "gelesen"-Marker, NIE blockieren ──
    # (deckt den Fall ab, dass ein Almanach nicht ueber das Read-Tool geoeffnet wird.)
    if ($tool -eq 'Bash') {
        $cmd = [string]$data.tool_input.command
        if (-not [string]::IsNullOrWhiteSpace($cmd)) {
            $cl = ($cmd.ToLower()) -replace '\\', '/'
            foreach ($m in [regex]::Matches($cl, 'bugs/(?:[a-z0-9._-]+/)*([a-z0-9._-]+)\.md')) {
                $an = $m.Groups[1].Value
                if ($an -ne 'readme' -and $an -ne 'system') {
                    New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-read-" + $an + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
                    # cat/bat/less liest immer den Volltext -> auch den full-Marker setzen (Digest-Modell Stufe C).
                    New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-full-" + $an + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
            # Best-Practices-Datei per cat/bat/less gelesen -> "bp-gelesen"-Marker. Abwaertskompatibel:
            # NEU best-practices/<kat>/<bereich>.md | ALT .../best-practices-<bereich>.md. Schluessel = <bereich>.
            foreach ($m in [regex]::Matches($cl, 'best-practices/(?:[a-z0-9._-]+/)*([a-z0-9._-]+)\.md')) {
                $bn = $m.Groups[1].Value -replace '^best-practices-', ''
                if ($bn -ne 'readme' -and $bn -ne 'system' -and $bn -notmatch '^_') {
                    New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-bp-read-" + $bn + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
        }
        exit 0
    }

    $fp = $data.tool_input.file_path
    if ([string]::IsNullOrWhiteSpace($fp)) { exit 0 }
    $fpl = ($fp.ToLower()) -replace '\\', '/'

    # ── Read-Zweig: "gelesen"-Marker setzen, NIE blockieren ──
    # Marker-Key = Almanach-Dateiname ohne .md (z.B. bugs/android/kotlin.md -> "kotlin", kategorie-unabhaengig). Relativ + absolut.
    if ($tool -eq 'Read') {
        if ($fpl -match '(?:^|/)bugs/(?:[^/]+/)*([^/]+)\.md$') {
            $almName = $Matches[1]
            if ($almName -ne 'readme' -and $almName -ne 'system') {
                $rm = Join-Path $env:TEMP ("bug-almanac-read-" + $almName + ".flag")
                New-Item -ItemType File -Path $rm -Force -ErrorAction SilentlyContinue | Out-Null
                # Digest-Modell: Volltext-Read (kein limit ODER limit >= 500) setzt zusaetzlich den
                # full-Marker (Stufe C). Kurzcheck-Read (z.B. limit=80) setzt nur den read-Marker.
                $lim = 0
                try { if ($data.tool_input.limit) { $lim = [int]$data.tool_input.limit } } catch {}
                if ($lim -le 0 -or $lim -ge 500) {
                    New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-full-" + $almName + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
        }
        # Best-Practices-Datei gelesen -> "bp-gelesen"-Marker. Abwaertskompatibel (Migration 2026-06-16):
        #   NEU best-practices/<kat>/<bereich>.md  |  ALT .../best-practices-<bereich>.md
        # Schluessel = <bereich> (best-practices--Praefix gestrippt); README/SYSTEM/_-Dateien ausgenommen.
        if ($fpl -match '/best-practices/(?:[^/]+/)*([^/]+)\.md$') {
            $bpName = $Matches[1] -replace '^best-practices-', ''
            if ($bpName -ne 'readme' -and $bpName -ne 'system' -and $bpName -notmatch '^_') {
                $bpm = Join-Path $env:TEMP ("bug-almanac-bp-read-" + $bpName + ".flag")
                New-Item -ItemType File -Path $bpm -Force -ErrorAction SilentlyContinue | Out-Null
            }
        }
        exit 0
    }

    # ── Test-Artefakt-Ausschluss (Frank-Entscheidung 2026-06-22 via almanach-trigger-auswertung):
    # Selbsttest-Dateien des Almanach-/Guard-Systems (Dateiname enthaelt 'almtest', z.B.
    # almtest-manifest.json) sollen den ECHTEN Guard NICHT ausloesen und die Sonde nicht verschmutzen.
    # ENG gefasst (nur 'almtest'), damit echte Unit-Tests (FooTest.kt/*.test.ts/_test.py) mit echter
    # Logik NICHT durchrutschen (Direktive #3, funktionserhaltend). Betrifft nur Edit/Write/MultiEdit
    # (Read/Bash sind oben schon mit exit 0 raus).
    if ($fpl -match 'almtest') { exit 0 }

    # ── Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). ──
    $slug = $null; $file = $null; $name = $null
    if ($fpl -match '\.sdplugin/' -or $fpl -match 'propertyinspector') {
        # Stream-Deck-Plugin: Dateien im *.sdPlugin-Ordner ODER ein Property Inspector.
        # MUSS vor dem chrome-Zweig stehen, da ein Stream-Deck-manifest.json sonst vom
        # generischen 'manifest.json$'-Match faelschlich als Chrome-Erweiterung erkannt wuerde.
        $slug = 'streamdeck'; $file = 'stream-deck.md'; $name = 'Elgato Stream Deck Plugin-Entwicklung'
    } elseif ($fpl -match '(^|/)(docker-)?compose\.ya?ml$' -or $fpl -match '(^|/)dockerfile(\.[^/]+)?$' -or $fpl -match '\.dockerfile$') {
        # Docker & Docker-Compose (Self-Hosting-Betrieb): compose.yaml/.yml, docker-compose.*,
        # Dockerfile (auch Dockerfile.prod / app.dockerfile). Dateiname-basiert, eindeutig.
        $slug = 'docker'; $file = 'docker.md'; $name = 'Docker & Docker-Compose (Self-Hosting-Betrieb)'
    } elseif ($fpl -match '(^|/)caddyfile$' -or $fpl -match '\.caddyfile$' -or $fpl -match '\.service$' -or $fpl -match '(^|/)nginx\.conf$' -or $fpl -match '/sites-(available|enabled)/') {
        # Reverse-Proxy/TLS (Caddy) + systemd-Units + nginx-Configs -> reverse-proxy-tls.md.
        # Caddyfile (kein Suffix), *.service (systemd-Unit), nginx.conf/sites-*. Eindeutige Dateimuster.
        $slug = 'reverseproxytls'; $file = 'reverse-proxy-tls.md'; $name = 'Reverse-Proxy/TLS (Caddy) & Linux-VPS-Betrieb'
    } elseif ($fpl -match '\.mcp\.json$') {
        # MCP-Server-Registrierung (.mcp.json). Vor dem chrome-'manifest.json'-Zweig (kein
        # Suffix-Konflikt, aber explizit). MCP-Server-Quellcode wird im .ts/.py-Zweig per Content-Probe erkannt.
        $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
    } elseif ($fpl -match '(^|/)opencode\.json$' -or $fpl -match '/\.opencode/' -or $fpl -match '/\.config/opencode/') {
        # OpenCode CLI: zentrale Config (opencode.json, strict) ODER projekt-/user-lokaler opencode-Ordner.
        # Eindeutige Muster, kein Konflikt mit .mcp.json/settings.json. (Coverage-Luecke geschlossen 2026-06-20.)
        $slug = 'opencode'; $file = 'opencode-cli.md'; $name = 'OpenCode CLI (Config/Agents/Plugins)'
    } elseif ($fpl -match 'manifest\.json$' -or $fpl -match '/overlays/' -or $fpl -match 'background\.js$' -or $fpl -match 'service-worker\.js$' -or $fpl -match 'vorlese-overlay') {
        $slug = 'chrome'; $file = 'chrome-extensions.md'; $name = 'Browser-Erweiterungen (Chrome/Edge MV3)'
    } elseif ($fpl -match 'google-services.*\.json$' -or $fpl -match '(billing|subscription|purchase).*\.kt$') {
        # Firebase-/Billing-Backend: google-services.json + Billing/Subscription/Purchase-Klassen.
        # MUSS vor dem androidplatform- und dem generischen .kt-Zweig stehen (sonst faengt z.B. database.kt/.kt das ab).
        $slug = 'firebasebilling'; $file = 'firebase-billing.md'; $name = 'Firebase / Crashlytics / Play Billing (Google-Backend-Dienste)'
    } elseif ($fpl -match 'proguard.*\.pro$' -or $fpl -match 'consumer.*\.pro$' -or $fpl -match '\.keep\.xml$') {
        # R8/ProGuard-Regeln (proguard-rules.pro, consumer-rules.pro) + Resource-keep (*.keep.xml).
        # MUSS vor dem gradle-Zweig stehen: build.gradle* bleibt gradle.md, R8-Regeldateien -> r8.md.
        $slug = 'r8'; $file = 'r8.md'; $name = 'R8 (Code-Shrinker/Optimizer/Obfuscator)'
    } elseif ($fpl -match 'build\.gradle(\.kts)?$' -or $fpl -match 'settings\.gradle(\.kts)?$' -or $fpl -match '/gradle/' -or $fpl -match 'gradle\.properties$' -or $fpl -match 'gradle-wrapper') {
        $slug = 'gradle'; $file = 'gradle.md'; $name = 'Build - Gradle (AGP/R8)'
    } elseif ($fpl -match '(database|dao)\.kt$' -or $fpl -match 'migration[^/]*\.kt$') {
        # Room-Persistenz (Dateiname): Database/DAO/Migration. MUSS vor dem androidplatform- und dem
        # generischen .kt-Zweig stehen. Room hat seit 2026-06-15 einen eigenen Almanach (room.md);
        # frueher liefen *Database.kt/*Migration(s).kt faelschlich nach android-platform.md.
        # (@Entity/@Dao/@Database OHNE diese Dateinamen werden im .kt-Zweig per Content-Probe erkannt.)
        $slug = 'room'; $file = 'room.md'; $name = 'Room-Persistenz (Entity/DAO/Database/Migration/TypeConverter/@Relation)'
    } elseif ($fpl -match 'androidmanifest\.xml$') {
        # Manifest separat (keine .kt): Platform-SDK (Permissions/Services/Receiver/Edge-to-Edge/targetSdk).
        # Der frueher hier mitgefangene (service|receiver|worker).kt-Teil wandert in den .kt-Block unten
        # als Dateiname-FALLBACK *nach* den spezifischen Inhalts-Signalen (voice/workmanager/drive) —
        # so verdeckt 'service.kt'/'worker.kt' nicht mehr die feature-spezifischen Android-Almanache.
        $slug = 'androidplatform'; $file = 'android-platform.md'; $name = 'Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager)'
    } elseif ($fpl -match '\.xml$') {
        # App-Widget-Provider-XML (res/xml/*.xml mit <appwidget-provider ...>) -> app-widgets.md.
        # AndroidManifest ist oben schon abgefangen; sonstiges XML (layout/strings/...) faellt durch
        # (kein Fehlalarm) — nur per appwidget-provider-Inhalt erkennen. Coverage-Erweiterung 2026-06-20.
        $xmlProbe = ""
        try { if (Test-Path -LiteralPath $fp) { $xmlProbe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $xmlProbe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $xmlProbe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $xmlProbe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($xmlProbe -match 'appwidget-provider') {
            $slug = 'appwidgets'; $file = 'app-widgets.md'; $name = 'App-Widgets (Glance/RemoteViews/AppWidgetProvider)'
        } elseif ($fpl -match 'network_security_config' -or $xmlProbe -match 'network-security-config' -or $xmlProbe -match 'cleartextTrafficPermitted') {
            # Network Security Config (Cleartext fuer private IP / interne-CA-trust-anchors) -> client-anbindung.md.
            $slug = 'clientanbindung'; $file = 'client-anbindung.md'; $name = 'Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST)'
        }
    } elseif ($fpl -match '\.kts?$') {
        # .kt/.kts: ZENTRALE Android/Kotlin-Erkennung. Inhalt EINMAL proben (existierende Datei + Tool-Input),
        # dann nach Prioritaet routen. FAIL-OPEN (try/catch). Reihenfolge strikt funktionserhaltend: die
        # spezifischen Feature-Signale (voice/workmanager/drive) stehen VOR dem service/worker-Dateiname-
        # Fallback, alle bestehenden Signale (hilt/net/media3/coil/room/compose) bleiben unveraendert dahinter.
        $composeSignal = $false
        $hiltSignal = $false
        $netSignal = $false
        $media3Signal = $false
        $coilSignal = $false
        $roomSignal = $false
        $voiceSignal = $false
        $workmanagerSignal = $false
        $driveSignal = $false
        $filamentSignal = $false
        $widgetSignal = $false
        # Dateiname-Fallback: Service/Receiver/Worker -> android-platform (wie bisher, NUR wenn kein
        # spezifischeres Feature-Signal davor zieht).
        $androidPlatformName = ($fpl -match '(service|receiver|worker)\.kt$')
        if ($fpl -match 'module\.kt$') { $hiltSignal = $true }
        if ($fpl -match '\.kts?$') {
            $probe = ""
            try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
            try {
                $ti = $data.tool_input
                if ($ti.content)    { $probe += "`n" + [string]$ti.content }
                if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
                if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
            } catch {}
            if ($probe -match '@Composable' -or $probe -match 'setContent') { $composeSignal = $true }
            # Hilt/Dagger-DI-Signale (Annotationen im Datei-/Tool-Input) -> hilt-dagger.md (hat Vorrang).
            if ($probe -match '@HiltAndroidApp' -or $probe -match '@AndroidEntryPoint' -or $probe -match '@HiltViewModel' -or $probe -match '@HiltWorker' -or $probe -match '@InstallIn' -or $probe -match '@Module' -or $probe -match '@AssistedInject' -or $probe -match '@Provides' -or $probe -match '@Binds') { $hiltSignal = $true }
            # Retrofit/OkHttp/Moshi-Networking-Signale -> retrofit-okhttp-moshi.md (nach Hilt, vor Compose/Kotlin).
            if ($probe -match 'retrofit2' -or $probe -match 'Retrofit\.Builder' -or $probe -match 'okhttp3' -or $probe -match 'OkHttpClient' -or $probe -match 'HttpLoggingInterceptor' -or $probe -match 'CertificatePinner' -or $probe -match '@JsonClass' -or $probe -match 'com\.squareup\.moshi' -or $probe -match 'Moshi\.Builder' -or $probe -match '@GET' -or $probe -match '@POST' -or $probe -match '@PUT' -or $probe -match '@DELETE' -or $probe -match '@PATCH' -or $probe -match '@FormUrlEncoded' -or $probe -match '@Multipart') { $netSignal = $true }
            # Media3/ExoPlayer-Audio-Wiedergabe-Signale -> media3-exoplayer.md (nach Hilt/Net, vor Compose/Kotlin).
            if ($probe -match 'media3' -or $probe -match 'ExoPlayer' -or $probe -match 'MediaSession' -or $probe -match 'MediaController' -or $probe -match 'MediaItem' -or $probe -match 'PlayerView' -or $probe -match 'DefaultLoadControl' -or $probe -match 'setMediaItem') { $media3Signal = $true }
            # Coil-3-Bild-/Video-Laden-Signale -> coil3.md (nach Hilt/Net/Media3, vor Compose/Kotlin).
            if ($probe -match 'coil3' -or $probe -match 'io\.coil-kt' -or $probe -match 'AsyncImage' -or $probe -match 'rememberAsyncImagePainter' -or $probe -match 'SubcomposeAsyncImage' -or $probe -match 'ImageLoader' -or $probe -match 'ImageRequest' -or $probe -match 'SingletonImageLoader') { $coilSignal = $true }
            # Room-Persistenz-Signale (Entity/DAO/Database/TypeConverter ohne *Dao.kt/*Database.kt-Dateinamen)
            # -> room.md (nach Hilt/Net/Media3/Coil, vor Compose/Kotlin). @Query bewusst NICHT (Retrofit-Konflikt).
            if ($probe -match '@Entity' -or $probe -match '@Dao' -or $probe -match '@Database' -or $probe -match '@TypeConverter' -or $probe -match '@ProvidedTypeConverter' -or $probe -match 'RoomDatabase' -or $probe -match 'androidx\.room' -or $probe -match '@PrimaryKey' -or $probe -match '@ColumnInfo' -or $probe -match '@Embedded' -or $probe -match '@AutoMigration' -or $probe -match '@Upsert') { $roomSignal = $true }
            # Voice-Assistant-Ausloesung / Wake-Word / Mic -> voice-assistant-trigger.md (spezifisch, VOR android-platform).
            if ($probe -match 'VoiceInteractionService' -or $probe -match 'AccessibilityService' -or $probe -match 'WakeWord' -or $probe -match 'Hotword' -or $probe -match 'KEYCODE_ASSIST' -or $probe -match 'ACTION_ASSIST' -or $probe -match 'sherpa[-_.]?onnx' -or $probe -match 'Porcupine' -or $probe -match 'OpenWakeWord' -or $probe -match 'NanoWakeWord' -or $probe -match 'Shizuku') { $voiceSignal = $true }
            # WorkManager & Notifications -> workmanager-notifications.md (spezifisch, VOR android-platform).
            if ($probe -match 'WorkManager' -or $probe -match 'PeriodicWorkRequest' -or $probe -match 'OneTimeWorkRequest' -or $probe -match 'CoroutineWorker' -or $probe -match 'enqueueUnique' -or $probe -match 'setExactAndAllowWhileIdle' -or $probe -match 'setAndAllowWhileIdle' -or $probe -match 'AlarmManager' -or $probe -match 'NotificationChannel' -or $probe -match 'NotificationCompat' -or $probe -match 'NotificationManagerCompat' -or $probe -match 'SCHEDULE_EXACT_ALARM') { $workmanagerSignal = $true }
            # Google-Drive-Backup & Cloud-Sync -> google-drive-backup.md (spezifisch, VOR android-platform).
            if ($probe -match 'appDataFolder' -or $probe -match 'DriveScopes' -or $probe -match 'DRIVE_APPDATA' -or $probe -match 'com\.google\.api\.services\.drive' -or $probe -match 'google-api-services-drive' -or $probe -match 'AuthorizationClient' -or $probe -match 'GoogleAuthUtil') { $driveSignal = $true }
            # 3D auf Android (Filament/SceneView) -> 3d-filament-android.md (vor Compose/Kotlin).
            if ($probe -match 'com\.google\.android\.filament' -or $probe -match 'Filament' -or $probe -match 'ModelViewer' -or $probe -match 'io\.github\.sceneview' -or $probe -match 'SceneView' -or $probe -match 'rememberEngine') { $filamentSignal = $true }
            # App-Widgets (Glance + klassisch) -> app-widgets.md (Coverage-Luecke geschlossen 2026-06-20).
            # MUSS vor compose stehen: Glance nutzt @Composable -> wuerde sonst als compose erkannt.
            if ($probe -match 'GlanceAppWidget' -or $probe -match 'GlanceAppWidgetReceiver' -or $probe -match 'provideGlance' -or $probe -match 'AppWidgetProvider' -or $probe -match 'RemoteViewsService' -or $probe -match 'RemoteViewsFactory' -or $probe -match 'AppWidgetManager' -or $probe -match 'updateAppWidget' -or $probe -match 'appWidgetId') { $widgetSignal = $true }
        }
        if ($widgetSignal) {
            $slug = 'appwidgets'; $file = 'app-widgets.md'; $name = 'App-Widgets (Glance/RemoteViews/AppWidgetProvider)'
        } elseif ($voiceSignal) {
            $slug = 'voiceassistant'; $file = 'voice-assistant-trigger.md'; $name = 'Voice-Assistant-Ausloesung / Wake-Word / Mic (Android)'
        } elseif ($workmanagerSignal) {
            $slug = 'workmanager'; $file = 'workmanager-notifications.md'; $name = 'WorkManager & Notifications (Reminder/Hintergrund)'
        } elseif ($driveSignal) {
            $slug = 'drivebackup'; $file = 'google-drive-backup.md'; $name = 'Google-Drive-Backup & Cloud-Sync (Android)'
        } elseif ($androidPlatformName) {
            $slug = 'androidplatform'; $file = 'android-platform.md'; $name = 'Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager)'
        } elseif ($hiltSignal) {
            $slug = 'hiltdagger'; $file = 'hilt-dagger.md'; $name = 'Hilt/Dagger Dependency Injection (KSP)'
        } elseif ($netSignal) {
            $slug = 'networking'; $file = 'retrofit-okhttp-moshi.md'; $name = 'Android-Networking (Retrofit/OkHttp/Moshi)'
        } elseif ($media3Signal) {
            $slug = 'media3'; $file = 'media3-exoplayer.md'; $name = 'Audio-Wiedergabe (Media3/ExoPlayer)'
        } elseif ($coilSignal) {
            $slug = 'coil3'; $file = 'coil3.md'; $name = 'Bild-/Video-Laden (Coil 3)'
        } elseif ($roomSignal) {
            $slug = 'room'; $file = 'room.md'; $name = 'Room-Persistenz (Entity/DAO/Database/Migration/TypeConverter/@Relation)'
        } elseif ($filamentSignal) {
            $slug = 'filament'; $file = '3d-filament-android.md'; $name = '3D auf Android (Filament/SceneView)'
        } elseif ($composeSignal) {
            $slug = 'compose'; $file = 'jetpack-compose.md'; $name = 'Jetpack Compose (Android-UI)'
        } else {
            $slug = 'kotlin'; $file = 'kotlin.md'; $name = 'Kotlin (Sprache/K2/Coroutines/Compose-Kontext)'
        }
    } elseif ($fpl -match '\.swift$' -or $fpl -match '\.xcodeproj' -or $fpl -match '(^|/)info\.plist$' -or $fpl -match '\.entitlements$') {
        # .swift: Content-Probe -> macOS-Overlay (NSPanel/...) / 3D-Metal-SceneKit (MTKView/SceneKit/RealityKit)
        #         / On-Device-Whisper (whisper.cpp/WhisperKit) -> sonst swift-appkit.md. Probe nur bei .swift
        #         (xcodeproj/plist/entitlements enthalten keinen Swift-Code). FAIL-OPEN.
        $macOverlaySignal = $false; $metalSignal = $false; $whisperSignal = $false
        if ($fpl -match '\.swift$') {
            $probe = ""
            try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
            try {
                $ti = $data.tool_input
                if ($ti.content)    { $probe += "`n" + [string]$ti.content }
                if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
                if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
            } catch {}
            # On-Device-Whisper zuerst (eindeutigste Signale).
            if ($probe -match 'whisper\.cpp' -or $probe -match 'WhisperKit' -or $probe -match 'whisperkit' -or $probe -match 'ggml' -or $probe -match 'CoreML.*[Ww]hisper') { $whisperSignal = $true }
            # macOS-Overlay-Fenster (NSPanel/nonactivating/collectionBehavior/click-through).
            if ($probe -match 'nonactivatingPanel' -or $probe -match 'NSPanel' -or $probe -match 'collectionBehavior' -or $probe -match 'canJoinAllSpaces' -or $probe -match 'orderFrontRegardless' -or $probe -match 'ignoresMouseEvents' -or $probe -match 'LSUIElement' -or $probe -match 'CGEventTap') { $macOverlaySignal = $true }
            # 3D auf macOS (Metal/SceneKit/RealityKit).
            if ($probe -match 'MTKView' -or $probe -match 'MTLDevice' -or $probe -match 'MTLCreateSystemDefaultDevice' -or $probe -match 'SceneKit' -or $probe -match 'SCNView' -or $probe -match 'RealityKit' -or $probe -match 'RealityView' -or $probe -match 'MetalFX' -or $probe -match 'MetalKit') { $metalSignal = $true }
        }
        # Info.plist/entitlements mit ATS/Local-Network/network.client -> Client-Anbindung (VPN/REST ans eigene Backend).
        $clientAnbindungSignal = $false
        if ($fpl -match '(^|/)info\.plist$' -or $fpl -match '\.entitlements$') {
            $plistProbe = ""
            try { if (Test-Path -LiteralPath $fp) { $plistProbe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
            try {
                $ti = $data.tool_input
                if ($ti.content)    { $plistProbe += "`n" + [string]$ti.content }
                if ($ti.new_string) { $plistProbe += "`n" + [string]$ti.new_string }
                if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $plistProbe += "`n" + [string]$e.new_string } } }
            } catch {}
            if ($plistProbe -match 'NSAppTransportSecurity' -or $plistProbe -match 'NSAllowsLocalNetworking' -or $plistProbe -match 'NSLocalNetworkUsageDescription' -or $plistProbe -match 'NSExceptionAllowsInsecureHTTPLoads' -or $plistProbe -match 'com\.apple\.security\.network\.client') { $clientAnbindungSignal = $true }
        }
        if ($clientAnbindungSignal) {
            $slug = 'clientanbindung'; $file = 'client-anbindung.md'; $name = 'Endgeraet-zu-self-hosted-Server-Anbindung (VPN/REST)'
        } elseif ($whisperSignal) {
            $slug = 'whisperlokal'; $file = 'whisper-stt-lokal.md'; $name = 'On-Device-Whisper / lokale Transkription'
        } elseif ($macOverlaySignal) {
            $slug = 'macosoverlay'; $file = 'macos-overlay.md'; $name = 'macOS-Overlay-Fenster (Swift/AppKit)'
        } elseif ($metalSignal) {
            $slug = 'metalmacos'; $file = '3d-metal-scenekit-macos.md'; $name = '3D auf macOS (Metal/SceneKit/RealityKit)'
        } else {
            $slug = 'swift'; $file = 'swift-appkit.md'; $name = 'macOS-Desktop (Swift/AppKit)'
        }
    } elseif ($fpl -match '\.tsx?$' -or $fpl -match 'tsconfig\.json$') {
        # .ts/.tsx: MCP-Server-Quelle (@modelcontextprotocol/sdk etc.)? -> mcp-server.md, sonst typescript.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog zum Compose-Probe). FAIL-OPEN.
        $mcpSignal = $false
        $threejsSignal = $false
        $motionAssetSignal = $false
        if ($fpl -match '\.tsx?$') {
            $probe = ""
            try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
            try {
                $ti = $data.tool_input
                if ($ti.content)    { $probe += "`n" + [string]$ti.content }
                if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
                if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
            } catch {}
            if ($probe -match '@modelcontextprotocol/sdk' -or $probe -match 'McpServer' -or $probe -match 'StdioServerTransport' -or $probe -match 'StreamableHTTPServerTransport' -or $probe -match 'setRequestHandler') { $mcpSignal = $true }
            # 3D im Web (Three.js/Babylon/WebGPU/R3F) -> 3d-threejs-webgpu.md (nach MCP, vor typescript).
            if ($probe -match 'THREE\.' -or $probe -match 'three/examples' -or $probe -match '@react-three/fiber' -or $probe -match '@react-three/drei' -or $probe -match '@babylonjs' -or $probe -match 'WebGPURenderer' -or $probe -match 'GLTFLoader' -or $probe -match 'KTX2Loader' -or $probe -match 'PMREMGenerator') { $threejsSignal = $true }
            # Lottie/Rive/SVG-Animationen -> lottie-rive-svg-animationen.md (nach MCP, vor generischem TypeScript).
            if ($probe -match 'lottie|bodymovin|@lottiefiles|@rive-app|useRive|RiveComponent|rive\.wasm|RuntimeLoader|loadAnimation') { $motionAssetSignal = $true }
        }
        if ($mcpSignal) {
            $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
        } elseif ($motionAssetSignal) {
            $slug = 'lottierivesvg'; $file = 'lottie-rive-svg-animationen.md'; $name = 'Lottie / Rive / SVG-Animationen im Web'
        } elseif ($threejsSignal) {
            $slug = 'threejs'; $file = '3d-threejs-webgpu.md'; $name = '3D im Web (Three.js/Babylon/WebGPU)'
        } else {
            $slug = 'typescript'; $file = 'typescript.md'; $name = 'TypeScript / Node'
        }
    } elseif ($fpl -match '\.(riv|lottie)$' -or $fpl -match '(lottie|bodymovin|rive)[^/]*\.json$') {
        $slug = 'lottierivesvg'; $file = 'lottie-rive-svg-animationen.md'; $name = 'Lottie / Rive / SVG-Animationen im Web'
    } elseif ($fpl -match '\.json$') {
        # Lottie-JSON ohne sprechenden Dateinamen nur per Inhalt erkennen, damit package.json/Configs nicht gekapert werden.
        $jsonProbe = ""
        try { if (Test-Path -LiteralPath $fp) { $jsonProbe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $jsonProbe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $jsonProbe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $jsonProbe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($jsonProbe -match '"fr"\s*:' -and $jsonProbe -match '"layers"\s*:') {
            $slug = 'lottierivesvg'; $file = 'lottie-rive-svg-animationen.md'; $name = 'Lottie / Rive / SVG-Animationen im Web'
        }
    } elseif ($fpl -match '\.svg$') {
        $svgProbe = ""
        try { if (Test-Path -LiteralPath $fp) { $svgProbe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $svgProbe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $svgProbe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $svgProbe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($svgProbe -match '<animate|<animateTransform|<animateMotion|<set|<mask|<clipPath|pathLength|stroke-dasharray') {
            $slug = 'lottierivesvg'; $file = 'lottie-rive-svg-animationen.md'; $name = 'Lottie / Rive / SVG-Animationen im Web'
        }
    } elseif ($fpl -match '\.(html|css|scss|sass)$') {
        $slug = 'webdesign'; $file = 'webseitenbau-webdesign.md'; $name = 'Webseitenbau / Webdesign / visuelle Effekte'
    } elseif ($fpl -match '\.ic(o|ns)$') {
        # App-Icon-Asset-Datei (.ico Windows / .icns macOS). Eindeutige Endung, kein Konflikt. Icon-Build-Skripte werden zusaetzlich im .py-Zweig per Content-Probe erkannt.
        $slug = 'iconbuilding'; $file = 'icon-building.md'; $name = 'App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)'
    } elseif ($fpl -match '\.xaml$' -or $fpl -match '\.csproj$' -or $fpl -match '\.cs$') {
        # .cs: Content-Probe -> Groq-Whisper-Transkription (GroqWhisperClient/audio/transcriptions/api.groq.com)
        #      ODER Wake-Word/Keyword-Spotting (sherpa-onnx/Porcupine/KeywordSpotter/...) -> sonst dotnet-csharp.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog zum MCP-/Compose-Probe). Nur .cs (XAML/csproj enthalten keinen STT-/KWS-Code). FAIL-OPEN.
        $groqSignal = $false; $wakeSignal = $false; $whisperSignal = $false; $dxSignal = $false; $winOverlaySignal = $false; $textInjectSignal = $false
        if ($fpl -match '\.cs$') {
            $probe = ""
            try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
            try {
                $ti = $data.tool_input
                if ($ti.content)    { $probe += "`n" + [string]$ti.content }
                if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
                if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
            } catch {}
            if ($probe -match 'GroqWhisperClient' -or $probe -match 'audio/transcriptions' -or $probe -match 'api\.groq\.com') { $groqSignal = $true }
            if ($probe -match 'KeywordSpotter|sherpa[-_.]?onnx|Porcupine|NanoWakeWord|OpenWakeWord|WakeWord|wake[-_]word') { $wakeSignal = $true }
            # On-Device-Whisper / lokale Transkription -> whisper-stt-lokal.md (nach groq/wake; lokal, NICHT Cloud).
            if ($probe -match 'Whisper\.net' -or $probe -match 'WhisperFactory' -or $probe -match 'whisper\.cpp' -or $probe -match 'GgmlType' -or $probe -match 'CTranslate2' -or $probe -match 'faster[-_]whisper') { $whisperSignal = $true }
            # 3D auf Windows (.NET DirectX/Stride/Silk.NET) -> 3d-dotnet-directx-windows.md.
            if ($probe -match 'Stride\.' -or $probe -match 'Silk\.NET' -or $probe -match 'Vortice' -or $probe -match 'Veldrid' -or $probe -match 'SharpDX' -or $probe -match 'Direct3D' -or $probe -match 'D3D12' -or $probe -match 'MonoGame') { $dxSignal = $true }
            # Windows-Overlay-Fenster (WPF: always-on-top/click-through/global Hotkey) -> windows-overlay.md.
            if ($probe -match 'WS_EX_NOACTIVATE' -or $probe -match 'WS_EX_TOOLWINDOW' -or $probe -match 'WS_EX_TRANSPARENT' -or $probe -match 'WS_EX_LAYERED' -or $probe -match 'RegisterHotKey' -or $probe -match 'WH_KEYBOARD_LL' -or $probe -match 'SetWindowPos' -or $probe -match 'WindowChrome' -or $probe -match 'AllowsTransparency') { $winOverlaySignal = $true }
            # Text-Injection in fremde Electron/Chromium-Felder (Claude Desktop Chat/Code/Cowork) -> windows-electron-text-injection.md.
            # Spezifischer als der Overlay-Zweig: Chromium-Render-HWND / UIA-Befuellen / Scancode-Paste / A11y-Aktivierung.
            if ($probe -match 'Chrome_RenderWidgetHostHWND' -or $probe -match 'force-renderer-accessibility' -or $probe -match 'KEYEVENTF_SCANCODE' -or $probe -match 'EnumChildWindows' -or $probe -match 'ValuePattern' -or $probe -match 'TextPattern' -or $probe -match 'FlaUI' -or $probe -match 'DWMWA_EXTENDED_FRAME_BOUNDS') { $textInjectSignal = $true }
        }
        if ($groqSignal) {
            $slug = 'groq'; $file = 'groq-transkription.md'; $name = 'Groq Whisper Transkription (Audio/STT)'
        } elseif ($wakeSignal) {
            $slug = 'wakeword'; $file = 'wake-word.md'; $name = 'Wake-Word / Keyword-Spotting (.NET/C#)'
        } elseif ($whisperSignal) {
            $slug = 'whisperlokal'; $file = 'whisper-stt-lokal.md'; $name = 'On-Device-Whisper / lokale Transkription'
        } elseif ($dxSignal) {
            $slug = 'dxwindows'; $file = '3d-dotnet-directx-windows.md'; $name = '3D auf Windows (.NET DirectX/Stride/Silk.NET)'
        } elseif ($textInjectSignal) {
            $slug = 'wineltextinject'; $file = 'windows-electron-text-injection.md'; $name = 'Text-Injection in Electron/Chromium-Felder (Windows, C#/WPF)'
        } elseif ($winOverlaySignal) {
            $slug = 'winoverlay'; $file = 'windows-overlay.md'; $name = 'Windows-Overlay-Fenster (C#/WPF)'
        } else {
            $slug = 'dotnet'; $file = 'dotnet-csharp.md'; $name = 'C#/.NET (WPF, WinUI, Konsole, Backend)'
        }
    } elseif ($fpl -match '\.py$') {
        # .py: MCP-Server-Quelle (mcp/FastMCP)? -> mcp-server.md, sonst python-windows.md. FAIL-OPEN.
        $mcpPy = $false
        $probe = ""
        try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $probe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($probe -match 'FastMCP' -or $probe -match 'mcp\.server' -or $probe -match 'from mcp' -or $probe -match 'import mcp' -or $probe -match 'stdio_server') { $mcpPy = $true }
        # Icon-Build-Skript (Pillow-Alpha-Check/iconutil/Android-adaptive/WPF-ApplicationIcon)? -> icon-building.md. Eindeutige Icon-Signale.
        $iconPy = $false
        if ($probe -match 'icns|iconutil|getchannel|ic_launcher|ApplicationIcon') { $iconPy = $true }
        # On-Device-Whisper / lokale Transkription (faster-whisper/whisper.cpp/CTranslate2) -> whisper-stt-lokal.md.
        $whisperPy = $false
        if ($probe -match 'faster_whisper' -or $probe -match 'faster-whisper' -or $probe -match 'whisper\.cpp' -or $probe -match 'WhisperModel' -or $probe -match 'ctranslate2' -or $probe -match 'pywhispercpp') { $whisperPy = $true }
        # Serverseitiger autonomer KI-Agent: Framework (pydantic-ai/langgraph) ODER eigene Tool-Loop im agent/-Ordner -> ai-agent-frameworks.md.
        $agentPy = $false
        if ($probe -match 'pydantic_ai' -or $probe -match 'pydantic-ai' -or $probe -match 'from langgraph' -or $probe -match 'import langgraph' -or $probe -match 'langchain\.agents' -or $probe -match 'create_react_agent' -or $probe -match 'StateGraph') { $agentPy = $true }
        if (-not $agentPy -and $fpl -match '(^|/)agent/[^/]+\.py$' -and ($probe -match 'generate_content' -or $probe -match 'tool_use' -or $probe -match 'system_instruction' -or $probe -match 'llm_decide' -or $probe -match 'brain_store' -or $probe -match 'brain_search')) { $agentPy = $true }
        # FastAPI/uvicorn-Web-Schicht (Endpoints/Middleware/lifespan/Body-Handling) -> fastapi.md. NACH mcp/agent
        # (agent/*.py bleibt ai-agent-frameworks), faengt brain-api/dashboard und andere FastAPI-Services.
        $fastapiPy = $false
        if ($probe -match 'from fastapi' -or $probe -match 'import fastapi' -or $probe -match 'FastAPI\(' -or $probe -match 'APIRouter' -or $probe -match '@app\.(get|post|put|delete|patch)' -or $probe -match 'uvicorn') { $fastapiPy = $true }
        if ($mcpPy) {
            $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
        } elseif ($agentPy) {
            $slug = 'aiagentframeworks'; $file = 'ai-agent-frameworks.md'; $name = 'Serverseitige autonome KI-Agenten (Loop/Tools/State/Kosten)'
        } elseif ($fastapiPy) {
            $slug = 'fastapi'; $file = 'fastapi.md'; $name = 'FastAPI & async-Python-Server (uvicorn-Web-Schicht)'
        } elseif ($iconPy) {
            $slug = 'iconbuilding'; $file = 'icon-building.md'; $name = 'App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)'
        } elseif ($whisperPy) {
            $slug = 'whisperlokal'; $file = 'whisper-stt-lokal.md'; $name = 'On-Device-Whisper / lokale Transkription'
        } else {
            $slug = 'python'; $file = 'python-windows.md'; $name = 'Python (Windows-Encoding/Cross-Platform-Scripting)'
        }
    } elseif ($fpl -match '\.(gd|tscn|tres|gdshader)$' -or $fpl -match '(^|/)project\.godot$') {
        # Godot 4 (GDScript/Szenen/Shader/Projekt) -> 3d-godot.md. Eindeutige Endungen, kein Konflikt.
        $slug = 'godot'; $file = '3d-godot.md'; $name = '3D mit Godot 4'
    } elseif ($fpl -match '\.conf$') {
        # WireGuard-Config (wg0.conf etc.): .conf ist generisch -> NUR per Inhalt erkennen (kein Fehlalarm
        # bei anderen .conf). Coverage-Luecke geschlossen 2026-06-20. Kein WG-Inhalt -> slug bleibt leer -> durch.
        $wgProbe = ""
        try { if (Test-Path -LiteralPath $fp) { $wgProbe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $wgProbe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $wgProbe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $wgProbe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($wgProbe -match '\[Interface\]' -and ($wgProbe -match '\[Peer\]' -or $wgProbe -match 'PrivateKey' -or $wgProbe -match 'ListenPort' -or $wgProbe -match 'AllowedIPs' -or $wgProbe -match 'PersistentKeepalive' -or $wgProbe -match 'wg-quick')) {
            $slug = 'wireguard'; $file = 'wireguard.md'; $name = 'WireGuard VPN-Konfiguration'
        }
    } elseif ($fpl -match '/hooks/[^/]*\.(ps1|sh)$') {
        $slug = 'claudehooks'; $file = 'claude-hooks.md'; $name = 'Claude-Harness Hooks (PowerShell/Bash)'
    } elseif (($fpl -notmatch '/(bugs|best-practices)/') -and (
                $fpl -match '(^|/)claude\.md$' -or
                $fpl -match '/rules/[^/]+\.md$' -or
                $fpl -match '(^|/)settings\.json$' -or
                $fpl -match '(^|/)settings\.local\.json$' -or
                $fpl -match '(^|/)settings-reference\.json$' -or
                $fpl -match '/skill\.md$' -or
                $fpl -match '/(commands|agents)/[^/]+\.md$')) {
        # Claude-Code-Konfiguration: CLAUDE.md, rules/*.md (~/.claude + claude-code-setup), settings(.local/-reference).json,
        # SKILL.md, commands/*.md, agents/*.md. AUSGESCHLOSSEN: bugs/** und best-practices/** (sind selbst .md -> kein Selbst-Trigger).
        # Hooks (.ps1/.sh) faengt der claudehooks-Zweig oben ab; MEMORY.md bewusst NICHT (zu haeufig automatisch beschrieben).
        $slug = 'claudeconfig'; $file = 'claude-config.md'; $name = 'Claude-Code Konfiguration und Regeln (CLAUDE.md/Rules/Settings/Skills/Commands/Agents)'
    }

    # ── Rust-3D (Bevy/wgpu) VOR der generischen Endungs-Erkennung ──
    # 3d-rust-wgpu-bevy.md existiert, rust.md NICHT. Ohne diesen Check liefe eine .rs-Datei in den
    # generischen rust.md-Platzhalter -> Fehlalarm "kein Almanach", obwohl 3d-rust-wgpu-bevy.md da ist.
    # Greift nur bei 3D-Signal; reines Rust faellt unveraendert in den generischen rust.md-Platzhalter.
    if (-not $slug -and ($fpl -match '\.rs$' -or $fpl -match '(^|/)cargo\.toml$')) {
        $probe = ""
        try { if (Test-Path -LiteralPath $fp) { $probe = Get-Content -LiteralPath $fp -Raw -ErrorAction SilentlyContinue } } catch {}
        try {
            $ti = $data.tool_input
            if ($ti.content)    { $probe += "`n" + [string]$ti.content }
            if ($ti.new_string) { $probe += "`n" + [string]$ti.new_string }
            if ($ti.edits)      { foreach ($e in $ti.edits) { if ($e.new_string) { $probe += "`n" + [string]$e.new_string } } }
        } catch {}
        if ($probe -match '\bbevy\b' -or $probe -match '\bwgpu\b' -or $probe -match 'Camera3d' -or $probe -match 'Mesh3d' -or $probe -match 'MeshMaterial3d' -or $probe -match 'PbrBundle' -or $probe -match 'StandardMaterial') {
            $slug = '3drust'; $file = '3d-rust-wgpu-bevy.md'; $name = '3D mit Rust (wgpu/Bevy)'
        }
    }

    # ── Generische Code-Erkennung (Luecke B, 2026-06-07): bekannte Programmiersprachen-Endung OHNE eigenes Mapping. ──
    # Greift NUR, wenn oben kein spezifischer Bereich erkannt wurde. So bekommt auch eine erste Rust-/Go-/Ruby-/
    # Java-Datei (= neuer Bereich, fuer den noch kein Almanach existiert) Zaehne, statt still durchzurutschen
    # ('if (-not $slug) { exit 0 }' liess das frueher komplett still verstummen). Nur echte Programmiersprachen-
    # Endungen (keine Daten/Doku/Config). Der abgeleitete file-Name (z.B. 'rust.md') existiert noch nicht ->
    # faellt unten korrekt in den "kein Almanach"-Quittungszweig. Legt Frank spaeter bugs/<kat>/rust.md an,
    # findet der rekursive Filter ihn automatisch -> dann greift die normale Almanach-Erzwingung. FAIL-OPEN bleibt.
    if (-not $slug) {
        $genMap = [ordered]@{
            '\.rs$'              = 'rust|Rust'
            '\.go$'             = 'go|Go'
            '\.rb$'             = 'ruby|Ruby'
            '\.java$'           = 'java|Java'
            '\.php$'            = 'php|PHP'
            '\.lua$'            = 'lua|Lua'
            '\.(c|cc|cpp|cxx|h|hpp)$' = 'cpp|C/C++'
            '\.dart$'           = 'dart|Dart/Flutter'
            '\.vue$'            = 'vue|Vue'
            '\.svelte$'         = 'svelte|Svelte'
            '\.exs?$'           = 'elixir|Elixir'
            '\.clj$'            = 'clojure|Clojure'
            '\.scala$'          = 'scala|Scala'
            '\.hs$'             = 'haskell|Haskell'
            '\.zig$'            = 'zig|Zig'
            '\.nim$'            = 'nim|Nim'
            '\.pl$'             = 'perl|Perl'
            '\.groovy$'         = 'groovy|Groovy'
        }
        foreach ($pat in $genMap.Keys) {
            if ($fpl -match $pat) {
                $parts = $genMap[$pat] -split '\|'
                $slug = $parts[0]; $file = $parts[0] + '.md'; $name = 'neuer Code-Bereich (' + $parts[1] + ')'
                break
            }
        }
    }
    if (-not $slug) { exit 0 }

    # Kategorie-robust (2026-06-03): Almanache liegen in Kategorie-Unterordnern (bugs/<kategorie>/<file>).
    # Den Almanach per rekursiver Suche nach dem Dateinamen finden, statt einen festen Pfad zu raten —
    # so muss dieser Hook NICHT angefasst werden, wenn eine Datei die Kategorie wechselt.
    $bugsRoot = Join-Path $env:USERPROFILE "proggs/bugs"
    $almanachItem = $null
    try { $almanachItem = Get-ChildItem -Path $bugsRoot -Recurse -Filter $file -File -ErrorAction SilentlyContinue | Select-Object -First 1 } catch {}
    $almanachExists = [bool]$almanachItem
    $almRel = if ($almanachItem) { (($almanachItem.FullName -replace '\\','/') -replace '.*?/bugs/', 'bugs/') } else { "bugs/$file" }
    $disabled = Test-Path (Join-Path $env:TEMP "bug-almanac-disable.flag")
    $almKey = ($file -replace '\.md$', '').ToLower()
    $readMarker = Join-Path $env:TEMP ("bug-almanac-read-" + $almKey + ".flag")
    $seenMarker = Join-Path $env:TEMP ("bug-almanac-seen-" + $slug + ".flag")
    # Digest-Modell Stufe C: Hochrisiko-Bereiche (tickende/teure Fehlerklassen: Release, Geld, Harness)
    # verlangen den VOLLTEXT (full-Marker), nicht nur den Kurzcheck. Liste = almKey (Dateiname ohne .md).
    $highRiskKeys = @('r8', 'firebase-billing', 'claude-hooks', 'claude-config')
    $isHighRisk = $highRiskKeys -contains $almKey
    $fullMarker = Join-Path $env:TEMP ("bug-almanac-full-" + $almKey + ".flag")

    # ── Robustheits-Fallback (Fix 2026-06-02): Read-Marker via Transkript nachziehen ──
    # Der Read-Zweig dieses Hooks setzt den Marker nur, wenn der Read-Hook tatsaechlich feuert.
    # Das kann ausbleiben: (a) Matcher-Cache in der Session, in der dieser Hook geaendert wurde
    # (Hook-Config ist gecacht — claude-hooks.md TL;DR Punkt 3), (b) Race wenn Read+Edit im selben
    # Antwortblock laufen. Dann blockt der Guard faelschlich, obwohl der Almanach laengst gelesen
    # wurde. Fallback: fehlt der Read-Marker, im Session-Transkript nach einem Tool-Call mit
    # file_path auf bugs/<almanach>.md suchen (unabhaengig vom Read-Hook). Block-Reasons enthalten
    # den Pfad NICHT als "file_path" -> kein Self-Unblock durch fruehere Blocks. Laeuft nur wenn
    # der Marker fehlt (Fehlalarm-Fall), also selten -> kein Performance-Problem.
    if ($almanachExists -and -not $disabled -and -not (Test-Path $readMarker)) {
        try {
            $tp = [string]$data.transcript_path
            if (-not [string]::IsNullOrWhiteSpace($tp) -and (Test-Path $tp)) {
                $pat = 'file_path"\s*:\s*"[^"]*bugs[\\/]+(?:[^"\\/]+[\\/]+)*' + [regex]::Escape($almKey) + '\.md'
                if (Select-String -LiteralPath $tp -Pattern $pat -Quiet -ErrorAction SilentlyContinue) {
                    New-Item -ItemType File -Path $readMarker -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
        } catch {}
    }

    # ── Trivial-Filter: reiner Version-Bump in JEDEM Bereich (Frank-Entscheidung 2026-06-20 +
    # bereichsuebergreifend erweitert 2026-06-22, beide via almanach-trigger-auswertung). Ein
    # Version-Bump (nur versionCode/versionName, "version":, version=, <Version>... bei .csproj)
    # aendert KEINE Logik -> kein Bug-Wissen noetig (known-bugs-before-coding nennt Versions-Bump als
    # Kleinkram). Die Definition kommt aus der GEMEINSAMEN Quelle (trivial_classify.py) -> gilt fuer
    # gradle/npm/Cargo/.csproj gleichermassen. NUR wenn JEDE nicht-leere Edit-Zeile eine Versionszeile
    # ist, NUR wenn sonst geblockt wuerde (read-Marker fehlt). Funktionserhaltend: jede andere Zeile ->
    # normaler Block unten. Transparenz: als pass/trivial-uebersprungen in die Sonde (nicht still).
    if (-not $disabled -and -not (Test-Path $readMarker)) {
        # Trivial-Entscheidung aus der GEMEINSAMEN Quelle (bugs/trivial_classify.py is-version-bump-json)
        # -> Guard und Auswertung (aggregate.py) laufen nie auseinander (DRY, Direktive #1). FAIL-SAFE:
        # faellt der Aufruf aus (python/Modul fehlt), wird NICHT durchgewunken (normaler Block) -> kein
        # Funktionsverlust, nur keine Trivial-Optimierung.
        $tcScript = Join-Path $env:USERPROFILE "proggs/bugs/trivial_classify.py"
        $isVb = $false
        try {
            if (Test-Path $tcScript) {
                $raw | & python3 $tcScript "is-version-bump-json" 2>$null | Out-Null
                if ($LASTEXITCODE -eq 0) { $isVb = $true }
            }
        } catch {}
        if ($isVb) {
            Add-AlmanacTrigger -EventType "pass" -BlockType "trivial-uebersprungen" -Slug $slug -Area $name -HighRisk $isHighRisk
            exit 0
        }
    }

    # ── ERZWINGUNG: Almanach existiert, Notaus aus, aber noch nicht gelesen -> BLOCKIEREN ──
    if ($almanachExists -and -not $disabled -and -not (Test-Path $readMarker)) {
        # Block-Logging (persistent ueber Sessions/Tage) — nur Beobachtung, beeinflusst nie die Entscheidung.
        try {
            $stateDir = Join-Path $env:USERPROFILE ".claude/state"
            if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
            Add-Content -Path (Join-Path $stateDir "bug-almanac-blocks.log") -Value ("$(Get-Date -Format 'yyyy-MM-dd HH:mm') $slug") -Encoding UTF8 -ErrorAction SilentlyContinue
        } catch {}
        Add-AlmanacTrigger -EventType "block" -BlockType "almanach-ungelesen" -Slug $slug -Area $name -HighRisk $isHighRisk
        $reason = if ($isHighRisk) {
            "STOPP - Bug-Almanach-Pflicht (Digest-Modell Stufe C, Regel: known-bugs-before-coding). Du editierst eine Datei aus dem HOCHRISIKO-Bereich '" + $name + "', aber " + $almRel + " wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/" + $almRel + " KOMPLETT mit dem Read-Tool (OHNE limit - Hochrisiko verlangt den Volltext, der Kurzcheck reicht hier nicht) + Versions-Abgleich, DANN editiere erneut. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
        } else {
            "STOPP - Bug-Almanach-Pflicht (Digest-Modell Stufe A, Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '" + $name + "', aber der Kurzcheck von " + $almRel + " wurde in dieser Session noch NICHT gelesen. Lies JETZT NUR den Kurzcheck: Read auf ~/proggs/" + $almRel + " mit limit=80 (die Kurzcheck-Sektion oben in der Datei; der Volltext ist NICHT noetig - er wird erst beim ERSTEN FEHLER im Bereich Pflicht, Stufe B). DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
        }
        $out = @{
            hookSpecificOutput = @{
                hookEventName            = "PreToolUse"
                permissionDecision       = "deny"
                permissionDecisionReason = $reason
            }
        }
        Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
        exit 0
    }

    # ── Stufe C (Digest-Modell): Hochrisiko-Bereich verlangt den VOLLTEXT, nicht nur den Kurzcheck ──
    # Erreicht nur, wenn der read-Marker existiert (Kurzcheck oder mehr gelesen). Fehlt der full-Marker,
    # wird bei Hochrisiko-Bereichen blockiert, bis ein Volltext-Read (Read ohne limit) erkannt wurde.
    if ($almanachExists -and -not $disabled -and $isHighRisk -and (Test-Path $readMarker) -and -not (Test-Path $fullMarker)) {
        # Transcript-Fallback (analog Almanach): ein Volltext-Read (Read OHNE limit-Feld im input-Objekt)
        # kann vom Read-Zweig verpasst worden sein (Matcher-Cache / Read+Edit-Race).
        try {
            $tp = [string]$data.transcript_path
            if (-not [string]::IsNullOrWhiteSpace($tp) -and (Test-Path $tp)) {
                $pat = 'file_path"\s*:\s*"[^"]*bugs[\\/]+(?:[^"\\/]+[\\/]+)*' + [regex]::Escape($almKey) + '\.md"(?![^}]*"limit"\s*:)'
                if (Select-String -LiteralPath $tp -Pattern $pat -Quiet -ErrorAction SilentlyContinue) {
                    New-Item -ItemType File -Path $fullMarker -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
        } catch {}
        if (-not (Test-Path $fullMarker)) {
            try {
                $stateDir = Join-Path $env:USERPROFILE ".claude/state"
                if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
                Add-Content -Path (Join-Path $stateDir "bug-almanac-blocks.log") -Value ("$(Get-Date -Format 'yyyy-MM-dd HH:mm') $slug (stufe-c-volltext)") -Encoding UTF8 -ErrorAction SilentlyContinue
            } catch {}
            Add-AlmanacTrigger -EventType "block" -BlockType "volltext-c" -Slug $slug -Area $name -HighRisk $true
            $reason = "STOPP - Volltext-Pflicht (Digest-Modell Stufe C, Regel: known-bugs-before-coding). '" + $name + "' ist ein HOCHRISIKO-Bereich (tickende/teure Fehlerklasse: Release, Geld, Harness). Der Kurzcheck von " + $almRel + " ist gelesen, aber der VOLLTEXT noch nicht. Oeffne ~/proggs/" + $almRel + " KOMPLETT mit dem Read-Tool (OHNE limit), DANN editiere erneut. (Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
            $out = @{
                hookSpecificOutput = @{
                    hookEventName            = "PreToolUse"
                    permissionDecision       = "deny"
                    permissionDecisionReason = $reason
                }
            }
            Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
            exit 0
        }
    }

    # ── BP-ERZWINGUNG (zweite Seite der Medaille): Almanach gelesen, aber die Best-Practices-Datei noch nicht ──
    # Reihenfolge automatisch erzwungen: dieser Block wird nur erreicht, wenn der Almanach-Block oben durchfiel
    # (= Almanach gelesen ODER Notaus). Greift NUR wenn Almanach existiert+gelesen, Notaus aus und eine
    # best-practices-<almKey>.md unter best-practices/projekt-code/ existiert. Sonst (keine BP-Datei): durchlassen.
    if ($almanachExists -and -not $disabled -and (Test-Path $readMarker)) {
        $bpReadMarker = Join-Path $env:TEMP ("bug-almanac-bp-read-" + $almKey + ".flag")
        # Lazy (Perf): den rekursiven BP-Verzeichnis-Scan NUR ausfuehren, wenn die BP-Datei in dieser
        # Session noch nicht als gelesen markiert ist. Ist der Marker schon da (haeufigster Fall nach
        # einmaligem BP-Lesen + vielen Folge-Edits), entfaellt der Scan komplett — verhaltensneutral,
        # weil sein Ergebnis dann ohnehin nur zu "kein Block" fuehren wuerde.
        if (-not (Test-Path $bpReadMarker)) {
            $bpRoot = Join-Path $env:USERPROFILE "proggs/best-practices"
            $bpItem = $null
            # Abwaertskompatibel: NEU <bereich>.md (best-practices/<kat>/) ODER ALT best-practices-<bereich>.md.
            try { if (Test-Path $bpRoot) { $bpItem = Get-ChildItem -Path $bpRoot -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -eq ($almKey + ".md") -or $_.Name -eq ("best-practices-" + $almKey + ".md") } | Select-Object -First 1 } } catch {}
            if ($bpItem) {
                $bpRel = (($bpItem.FullName -replace '\\','/') -replace '.*?/best-practices/', 'best-practices/')

                # Transcript-Fallback (analog Almanach): Read evtl. vom Read-Hook verpasst (Matcher-Cache / Read+Edit-Race).
                try {
                    $tp = [string]$data.transcript_path
                    if (-not [string]::IsNullOrWhiteSpace($tp) -and (Test-Path $tp)) {
                        $pat = 'file_path"\s*:\s*"[^"]*best-practices[/-][^"]*' + [regex]::Escape($almKey) + '\.md'
                        if (Select-String -LiteralPath $tp -Pattern $pat -Quiet -ErrorAction SilentlyContinue) {
                            New-Item -ItemType File -Path $bpReadMarker -Force -ErrorAction SilentlyContinue | Out-Null
                        }
                    }
                } catch {}

                if (-not (Test-Path $bpReadMarker)) {
                try {
                    $stateDir = Join-Path $env:USERPROFILE ".claude/state"
                    if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
                    Add-Content -Path (Join-Path $stateDir "bug-almanac-blocks.log") -Value ("$(Get-Date -Format 'yyyy-MM-dd HH:mm') $slug (best-practices)") -Encoding UTF8 -ErrorAction SilentlyContinue
                } catch {}
                Add-AlmanacTrigger -EventType "block" -BlockType "best-practices" -Slug $slug -Area $name -HighRisk $isHighRisk
                $reason = "STOPP - Best-Practices-Pflicht (Regel: known-bugs-before-coding). Der Bug-Almanach fuer '" + $name + "' ist gelesen - aber die zugehoerige Best-Practices-Datei " + $bpRel + " in dieser Session noch NICHT. Reihenfolge: erst Almanach (erledigt), dann Best Practices, DANN editieren. Oeffne ZUERST ~/proggs/" + $bpRel + " mit dem Read-Tool (Kurzcheck mit limit=80 reicht - Digest-Modell Stufe A; so macht man es von vornherein richtig, damit der Bug gar nicht erst entsteht), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
                $out = @{
                    hookSpecificOutput = @{
                        hookEventName            = "PreToolUse"
                        permissionDecision       = "deny"
                        permissionDecisionReason = $reason
                    }
                }
                    Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
                    exit 0
                }
            }
        }
    }

    # ── Almanach existiert + bereits gelesen (oder Notaus): einmalige sanfte Bestaetigung, dann frei ──
    if ($almanachExists) {
        if (-not (Test-Path $seenMarker)) {
            New-Item -ItemType File -Path $seenMarker -Force -ErrorAction SilentlyContinue | Out-Null
            Add-AlmanacTrigger -EventType "pass" -BlockType ($(if ($disabled) {"disabled"} else {"already-read"})) -Slug $slug -Area $name -HighRisk $isHighRisk
            $msg = if ($disabled) {
                "BUG-ALMANACH-HINWEIS: Bereich '" + $name + "' - Notaus aktiv (bug-almanac-disable.flag), kein Lese-Zwang. Lies " + $almRel + " (+ Best Practices) freiwillig."
            } else {
                "BUG-ALMANACH: " + $almRel + " gelesen, Best-Practices-Pflicht erfuellt - Bereich '" + $name + "' ist fuer diese Session freigegeben."
            }
            $out = @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; additionalContext = $msg } }
            Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
        }
        exit 0
    }

    # ── Kein Almanach: BLOCKIEREN mit Quittung (Stufe 2, 2026-06-07) — Recherche/Entscheidung braucht Franks OK ──
    # Frueher war dies nur ein zahnloser additionalContext-Hinweis (= wurde uebersehen; im Block-Log gab es nie
    # einen "kein-almanach"-Eintrag). Jetzt BLOCKT der Hook (permissionDecision=deny), bis eine bewusste Geste
    # ihn aufhebt: die Quittung 'bug-almanac-ack-<slug>.flag' (von mir nach Franks OK / bei Kleinkram angelegt)
    # ODER der globale Notaus. So bekommt der "neuer Bereich"-Trigger echte Zaehne (deterministisch statt advisory).
    $ackMarker = Join-Path $env:TEMP ("bug-almanac-ack-" + $slug + ".flag")
    if ($disabled -or (Test-Path $ackMarker)) {
        # Quittung gesetzt oder Notaus aktiv -> frei. Einmalige sanfte Bestaetigung (seenMarker gegen Spam).
        if (-not (Test-Path $seenMarker)) {
            New-Item -ItemType File -Path $seenMarker -Force -ErrorAction SilentlyContinue | Out-Null
            Add-AlmanacTrigger -EventType "pass" -BlockType ($(if ($disabled) {"disabled"} else {"ack"})) -Slug $slug -Area $name -HighRisk $false
            $msg = if ($disabled) {
                "BUG-ALMANACH-HINWEIS: Bereich '" + $name + "' ohne Almanach (bugs/" + $file + ") - Notaus aktiv, freigegeben."
            } else {
                "BUG-ALMANACH-HINWEIS: Bereich '" + $name + "' ohne Almanach (bugs/" + $file + ") - Quittung gesetzt, fuer diese Session freigegeben."
            }
            $out = @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; additionalContext = $msg } }
            Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
        }
        exit 0
    }
    # Block-Logging (persistent) — nur Beobachtung, beeinflusst nie die Entscheidung.
    try {
        $stateDir = Join-Path $env:USERPROFILE ".claude/state"
        if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
        Add-Content -Path (Join-Path $stateDir "bug-almanac-blocks.log") -Value ("$(Get-Date -Format 'yyyy-MM-dd HH:mm') $slug (kein-almanach)") -Encoding UTF8 -ErrorAction SilentlyContinue
    } catch {}
    Add-AlmanacTrigger -EventType "block" -BlockType "kein-almanach" -Slug $slug -Area $name -HighRisk $false
    $reason = "STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du arbeitest an '" + $name + "', aber es gibt noch KEINEN Almanach (bugs/" + $file + "). Zwei Wege: (1) Frank kurz um OK bitten und dann den Skill 'bug-almanach-recherche' STARTEN (der vorgeschriebene, vollstaendige Weg - NICHT selbst ad hoc recherchieren); ODER (2) wenn das nur trivialer Kleinkram ist (String/Doku/Versions-Bump) bzw. Frank gegen eine Recherche entscheidet: die Quittung anlegen - leere Datei '" + $ackMarker + "' (z.B. Bash: touch ... / PowerShell: New-Item) - danach ist der Bereich fuer diese Session frei. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen."
    $out = @{
        hookSpecificOutput = @{
            hookEventName            = "PreToolUse"
            permissionDecision       = "deny"
            permissionDecisionReason = $reason
        }
    }
    Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
} catch {
    Hook-LogWarn "bug-almanac-guard: $_"
}

exit 0
