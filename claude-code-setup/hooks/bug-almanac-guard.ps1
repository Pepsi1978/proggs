# bug-almanac-guard: Stufe 2 (ERZWINGUNG) des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Bei Edit/Write/MultiEdit an bereichstypischen Dateien: BLOCKIERT (permissionDecision=deny), wenn fuer
#   den Bereich ein Almanach existiert, dieser aber in DIESER Session noch nicht gelesen wurde.
# Bei Read einer bugs/<X>.md ODER Bash-cat/bat/less auf bugs/<X>.md: setzt den "gelesen"-Marker (NIE blockierend).
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
            foreach ($m in [regex]::Matches($cl, 'bugs/([a-z0-9._-]+)\.md')) {
                $an = $m.Groups[1].Value
                if ($an -ne 'readme' -and $an -ne 'system') {
                    New-Item -ItemType File -Path (Join-Path $env:TEMP ("bug-almanac-read-" + $an + ".flag")) -Force -ErrorAction SilentlyContinue | Out-Null
                }
            }
        }
        exit 0
    }

    $fp = $data.tool_input.file_path
    if ([string]::IsNullOrWhiteSpace($fp)) { exit 0 }
    $fpl = ($fp.ToLower()) -replace '\\', '/'

    # ── Read-Zweig: "gelesen"-Marker setzen, NIE blockieren ──
    # Marker-Key = Almanach-Dateiname ohne .md (z.B. bugs/kotlin.md -> "kotlin"). Relativ + absolut.
    if ($tool -eq 'Read') {
        if ($fpl -match '(?:^|/)bugs/([^/]+)\.md$') {
            $almName = $Matches[1]
            if ($almName -ne 'readme' -and $almName -ne 'system') {
                $rm = Join-Path $env:TEMP ("bug-almanac-read-" + $almName + ".flag")
                New-Item -ItemType File -Path $rm -Force -ErrorAction SilentlyContinue | Out-Null
            }
        }
        exit 0
    }

    # ── Edit/Write/MultiEdit-Zweig: Bereich anhand des Dateipfads erkennen (bei neuem Almanach hier ergaenzen). ──
    $slug = $null; $file = $null; $name = $null
    if ($fpl -match 'manifest\.json$' -or $fpl -match '/overlays/' -or $fpl -match 'background\.js$' -or $fpl -match 'service-worker\.js$' -or $fpl -match 'vorlese-overlay') {
        $slug = 'chrome'; $file = 'chrome-extensions.md'; $name = 'Browser-Erweiterungen (Chrome/Edge MV3)'
    } elseif ($fpl -match 'google-services.*\.json$' -or $fpl -match '(billing|subscription|purchase).*\.kt$') {
        # Firebase-/Billing-Backend: google-services.json + Billing/Subscription/Purchase-Klassen.
        # MUSS vor dem androidplatform- und dem generischen .kt-Zweig stehen (sonst faengt z.B. database.kt/.kt das ab).
        $slug = 'firebasebilling'; $file = 'firebase-billing.md'; $name = 'Firebase / Crashlytics / Play Billing (Google-Backend-Dienste)'
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
        }
        if ($composeSignal) {
            $slug = 'compose'; $file = 'jetpack-compose.md'; $name = 'Jetpack Compose (Android-UI)'
        } else {
            $slug = 'kotlin'; $file = 'kotlin.md'; $name = 'Kotlin (Sprache/K2/Coroutines/Compose-Kontext)'
        }
    } elseif ($fpl -match '\.swift$' -or $fpl -match '\.xcodeproj' -or $fpl -match '(^|/)info\.plist$' -or $fpl -match '\.entitlements$') {
        $slug = 'swift'; $file = 'swift-appkit.md'; $name = 'macOS-Desktop (Swift/AppKit)'
    } elseif ($fpl -match '\.tsx?$' -or $fpl -match 'tsconfig\.json$') {
        $slug = 'typescript'; $file = 'typescript.md'; $name = 'TypeScript / Node'
    } elseif ($fpl -match '\.user\.js$') {
        $slug = 'tampermonkey'; $file = 'tampermonkey.md'; $name = 'Tampermonkey/Userscripts'
    } elseif ($fpl -match '\.xaml$' -or $fpl -match '\.csproj$' -or $fpl -match '\.cs$') {
        $slug = 'dotnet'; $file = 'dotnet-csharp.md'; $name = 'C#/.NET (WPF, WinUI, Konsole, Backend)'
    } elseif ($fpl -match '\.py$') {
        $slug = 'python'; $file = 'python-windows.md'; $name = 'Python (Windows-Encoding/Cross-Platform-Scripting)'
    } elseif ($fpl -match '/hooks/[^/]*\.(ps1|sh)$') {
        $slug = 'claudehooks'; $file = 'claude-hooks.md'; $name = 'Claude-Harness Hooks (PowerShell/Bash)'
    }
    if (-not $slug) { exit 0 }

    $almanachPath = Join-Path (Join-Path $env:USERPROFILE "proggs/bugs") $file
    $almanachExists = Test-Path $almanachPath
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
                $pat = 'file_path"\s*:\s*"[^"]*bugs[\\/]+' + [regex]::Escape($almKey) + '\.md'
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
        $reason = "STOPP - Bug-Almanach-Pflicht (Regel: known-bugs-before-coding). Du editierst eine Datei aus dem Bereich '" + $name + "', aber bugs/" + $file + " wurde in dieser Session noch NICHT gelesen. Oeffne ZUERST ~/proggs/bugs/" + $file + " mit dem Read-Tool (komplett + Versions-Abgleich), DANN editiere erneut - das Lesen wird automatisch erkannt und gibt den Bereich frei. (Trivialer Kleinkram wie String/Doku/Versions-Bump ist von der Regel ausgenommen; das Lesen kostet pro Bereich nur EINMAL pro Session. Notaus bei Fehlalarm: leere Datei " + (Join-Path $env:TEMP 'bug-almanac-disable.flag') + " anlegen.)"
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

    # ── Almanach existiert + bereits gelesen (oder Notaus): einmalige sanfte Bestaetigung, dann frei ──
    if ($almanachExists) {
        if (-not (Test-Path $seenMarker)) {
            New-Item -ItemType File -Path $seenMarker -Force -ErrorAction SilentlyContinue | Out-Null
            $msg = if ($disabled) {
                "BUG-ALMANACH-HINWEIS: Bereich '" + $name + "' - Notaus aktiv (bug-almanac-disable.flag), kein Lese-Zwang. Lies bugs/" + $file + " freiwillig."
            } else {
                "BUG-ALMANACH: bugs/" + $file + " wurde gelesen - Bereich '" + $name + "' ist fuer diese Session freigegeben."
            }
            $out = @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; additionalContext = $msg } }
            Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
        }
        exit 0
    }

    # ── Kein Almanach: nur erinnern (Stufe 1), NICHT blockieren - Recherche braucht Franks OK ──
    if (Test-Path $seenMarker) { exit 0 }
    New-Item -ItemType File -Path $seenMarker -Force -ErrorAction SilentlyContinue | Out-Null
    $msg = "BUG-ALMANACH-HINWEIS: Du arbeitest an " + $name + ", aber es gibt noch KEINEN Almanach (bugs/" + $file + "). Hol Franks kurzes OK und STARTE dann den Skill 'bug-almanach-recherche' - das ist der vorgeschriebene, vollstaendige Weg; NICHT selbst ad hoc recherchieren."
    $out = @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; additionalContext = $msg } }
    Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
} catch {
    Hook-LogWarn "bug-almanac-guard: $_"
}

exit 0
