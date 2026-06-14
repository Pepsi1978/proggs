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
                }
            }
            # Best-Practices-Datei per cat/bat/less gelesen -> "bp-gelesen"-Marker (Schluessel = Bereich ohne 'best-practices-'-Praefix).
            foreach ($m in [regex]::Matches($cl, 'best-practices-([a-z0-9._-]+)\.md')) {
                $bn = $m.Groups[1].Value
                New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-bp-read-" + $bn + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
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
            }
        }
        # Best-Practices-Datei gelesen -> "bp-gelesen"-Marker (Schluessel = Bereich ohne 'best-practices-'-Praefix).
        if ($fpl -match '/best-practices-([a-z0-9._-]+)\.md$') {
            $bpName = $Matches[1]
            $bpm = Join-Path $env:TEMP ("bug-almanac-bp-read-" + $bpName + ".flag")
            New-Item -ItemType File -Path $bpm -Force -ErrorAction SilentlyContinue | Out-Null
        }
        exit 0
    }

    # ── Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). ──
    $slug = $null; $file = $null; $name = $null
    if ($fpl -match '\.sdplugin/' -or $fpl -match 'propertyinspector') {
        # Stream-Deck-Plugin: Dateien im *.sdPlugin-Ordner ODER ein Property Inspector.
        # MUSS vor dem chrome-Zweig stehen, da ein Stream-Deck-manifest.json sonst vom
        # generischen 'manifest.json$'-Match faelschlich als Chrome-Erweiterung erkannt wuerde.
        $slug = 'streamdeck'; $file = 'stream-deck.md'; $name = 'Elgato Stream Deck Plugin-Entwicklung'
    } elseif ($fpl -match '\.mcp\.json$') {
        # MCP-Server-Registrierung (.mcp.json). Vor dem chrome-'manifest.json'-Zweig (kein
        # Suffix-Konflikt, aber explizit). MCP-Server-Quellcode wird im .ts/.py-Zweig per Content-Probe erkannt.
        $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
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
    } elseif ($fpl -match 'androidmanifest\.xml$' -or $fpl -match '(service|receiver|worker|migrations?|database)\.kt$') {
        # Framework/Runtime-Dateien: Manifest (Permissions/Services/Receiver) + Service/Receiver/Worker/Room-DB/Migration.
        # Diese enthalten kein @Composable -> kein Konflikt mit dem Compose/Kotlin-Zweig (muss VORHER stehen).
        $slug = 'androidplatform'; $file = 'android-platform.md'; $name = 'Android-Framework / Platform-SDK (Lifecycle/Permissions/Services/WorkManager/Room-Runtime)'
    } elseif ($fpl -match '\.kts?$') {
        # .kt/.kts: Compose-UI-Datei (@Composable/setContent)? -> jetpack-compose.md, sonst kotlin.md.
        # Inhalt aus der existierenden Datei UND aus dem Tool-Input (neue Datei/neuer Composable) pruefen. FAIL-OPEN.
        $composeSignal = $false
        $hiltSignal = $false
        $netSignal = $false
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
        }
        if ($hiltSignal) {
            $slug = 'hiltdagger'; $file = 'hilt-dagger.md'; $name = 'Hilt/Dagger Dependency Injection (KSP)'
        } elseif ($netSignal) {
            $slug = 'networking'; $file = 'retrofit-okhttp-moshi.md'; $name = 'Android-Networking (Retrofit/OkHttp/Moshi)'
        } elseif ($composeSignal) {
            $slug = 'compose'; $file = 'jetpack-compose.md'; $name = 'Jetpack Compose (Android-UI)'
        } else {
            $slug = 'kotlin'; $file = 'kotlin.md'; $name = 'Kotlin (Sprache/K2/Coroutines/Compose-Kontext)'
        }
    } elseif ($fpl -match '\.swift$' -or $fpl -match '\.xcodeproj' -or $fpl -match '(^|/)info\.plist$' -or $fpl -match '\.entitlements$') {
        $slug = 'swift'; $file = 'swift-appkit.md'; $name = 'macOS-Desktop (Swift/AppKit)'
    } elseif ($fpl -match '\.tsx?$' -or $fpl -match 'tsconfig\.json$') {
        # .ts/.tsx: MCP-Server-Quelle (@modelcontextprotocol/sdk etc.)? -> mcp-server.md, sonst typescript.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog zum Compose-Probe). FAIL-OPEN.
        $mcpSignal = $false
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
        }
        if ($mcpSignal) {
            $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
        } else {
            $slug = 'typescript'; $file = 'typescript.md'; $name = 'TypeScript / Node'
        }
    } elseif ($fpl -match '\.ic(o|ns)$') {
        # App-Icon-Asset-Datei (.ico Windows / .icns macOS). Eindeutige Endung, kein Konflikt. Icon-Build-Skripte werden zusaetzlich im .py-Zweig per Content-Probe erkannt.
        $slug = 'iconbuilding'; $file = 'icon-building.md'; $name = 'App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)'
    } elseif ($fpl -match '\.xaml$' -or $fpl -match '\.csproj$' -or $fpl -match '\.cs$') {
        # .cs: Content-Probe -> Groq-Whisper-Transkription (GroqWhisperClient/audio/transcriptions/api.groq.com)
        #      ODER Wake-Word/Keyword-Spotting (sherpa-onnx/Porcupine/KeywordSpotter/...) -> sonst dotnet-csharp.md.
        # Inhalt aus existierender Datei UND Tool-Input pruefen (analog zum MCP-/Compose-Probe). Nur .cs (XAML/csproj enthalten keinen STT-/KWS-Code). FAIL-OPEN.
        $groqSignal = $false; $wakeSignal = $false
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
        }
        if ($groqSignal) {
            $slug = 'groq'; $file = 'groq-transkription.md'; $name = 'Groq Whisper Transkription (Audio/STT)'
        } elseif ($wakeSignal) {
            $slug = 'wakeword'; $file = 'wake-word.md'; $name = 'Wake-Word / Keyword-Spotting (.NET/C#)'
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
        if ($mcpPy) {
            $slug = 'mcpserver'; $file = 'mcp-server.md'; $name = 'MCP-Server-Bau (Model Context Protocol)'
        } elseif ($iconPy) {
            $slug = 'iconbuilding'; $file = 'icon-building.md'; $name = 'App-Icon-Building (Windows/.ico, macOS/.icns, Android adaptive)'
        } else {
            $slug = 'python'; $file = 'python-windows.md'; $name = 'Python (Windows-Encoding/Cross-Platform-Scripting)'
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

    # ── ERZWINGUNG: Almanach existiert, Notaus aus, aber noch nicht gelesen -> BLOCKIEREN ──
    if ($almanachExists -and -not $disabled -and -not (Test-Path $readMarker)) {
        # Block-Logging (persistent ueber Sessions/Tage) — nur Beobachtung, beeinflusst nie die Entscheidung.
        try {
            $stateDir = Join-Path $env:USERPROFILE ".claude/state"
            if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force -ErrorAction SilentlyContinue | Out-Null }
            Add-Content -Path (Join-Path $stateDir "bug-almanac-blocks.log") -Value ("$(Get-Date -Format 'yyyy-MM-dd HH:mm') $slug") -Encoding UTF8 -ErrorAction SilentlyContinue
        } catch {}
        $reason = "STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '" + $name + "', aber " + $almRel + " wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/" + $almRel + " mit dem Read-Tool (komplett + Versions-Abgleich), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; das Lesen kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
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
            $bpRoot = Join-Path $env:USERPROFILE "proggs/best-practices/projekt-code"
            $bpItem = $null
            try { if (Test-Path $bpRoot) { $bpItem = Get-ChildItem -Path $bpRoot -Recurse -Filter ("best-practices-" + $almKey + ".md") -File -ErrorAction SilentlyContinue | Select-Object -First 1 } } catch {}
            if ($bpItem) {
                $bpRel = (($bpItem.FullName -replace '\\','/') -replace '.*?/best-practices/', 'best-practices/')

                # Transcript-Fallback (analog Almanach): Read evtl. vom Read-Hook verpasst (Matcher-Cache / Read+Edit-Race).
                try {
                    $tp = [string]$data.transcript_path
                    if (-not [string]::IsNullOrWhiteSpace($tp) -and (Test-Path $tp)) {
                        $pat = 'file_path"\s*:\s*"[^"]*best-practices-' + [regex]::Escape($almKey) + '\.md'
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
                $reason = "STOPP - Best-Practices-Pflicht (Regel: known-bugs-before-coding). Der Bug-Almanach fuer '" + $name + "' ist gelesen - aber die zugehoerige Best-Practices-Datei " + $bpRel + " in dieser Session noch NICHT. Reihenfolge: erst Almanach (erledigt), dann Best Practices, DANN editieren. Oeffne ZUERST ~/proggs/" + $bpRel + " mit dem Read-Tool (so macht man es von vornherein richtig, damit der Bug gar nicht erst entsteht), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
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
