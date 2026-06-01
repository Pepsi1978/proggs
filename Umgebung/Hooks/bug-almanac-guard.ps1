# bug-almanac-guard: Erinnert bei Edit/Write an bereichstypischen Dateien an den passenden Bug-Almanach.
# Schicht 2 ("Sicherheitsnetz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as PreToolUse hook (matcher: Edit|Write). NICHT blockierend - injiziert nur additionalContext.
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
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
    $fp = $data.tool_input.file_path
    if ([string]::IsNullOrWhiteSpace($fp)) { exit 0 }
    $fpl = ($fp.ToLower()) -replace '\\', '/'

    # Bereich anhand des Dateipfads erkennen (Pfad-Mapping - bei neuem Almanach hier ergaenzen).
    $slug = $null; $file = $null; $name = $null
    if ($fpl -match 'manifest\.json$' -or $fpl -match '/overlays/' -or $fpl -match 'background\.js$' -or $fpl -match 'service-worker\.js$' -or $fpl -match 'vorlese-overlay') {
        $slug = 'chrome'; $file = 'chrome-extensions.md'; $name = 'Browser-Erweiterungen (Chrome/Edge MV3)'
    } elseif ($fpl -match '\.kts?$' -or $fpl -match 'androidmanifest\.xml$') {
        $slug = 'android'; $file = 'android-compose.md'; $name = 'Android (Kotlin/Compose)'
    } elseif ($fpl -match '\.xaml$' -or $fpl -match '\.csproj$' -or $fpl -match '\.cs$') {
        $slug = 'wpf'; $file = 'wpf-csharp.md'; $name = 'Windows-Desktop (C#/WPF/WinUI)'
    } elseif ($fpl -match '\.swift$') {
        $slug = 'swift'; $file = 'swift-appkit.md'; $name = 'macOS-Desktop (Swift/AppKit)'
    } elseif ($fpl -match '\.user\.js$') {
        $slug = 'tampermonkey'; $file = 'tampermonkey.md'; $name = 'Tampermonkey/Userscripts'
    } elseif ($fpl -match '/hooks/[^/]*\.(ps1|sh)$') {
        $slug = 'claudehooks'; $file = 'claude-hooks.md'; $name = 'Claude-Harness Hooks (PowerShell/Bash)'
    }
    if (-not $slug) { exit 0 }

    # Spam-Schutz: nur einmal pro Bereich pro Session (Marker leert der Index-Hook bei SessionStart).
    $marker = Join-Path $env:TEMP ("bug-almanac-seen-" + $slug + ".flag")
    if (Test-Path $marker) { exit 0 }
    New-Item -ItemType File -Path $marker -Force -ErrorAction SilentlyContinue | Out-Null

    $almanachPath = Join-Path (Join-Path $env:USERPROFILE "proggs/bugs") $file
    if (Test-Path $almanachPath) {
        $msg = "BUG-ALMANACH-HINWEIS: Du arbeitest an " + $name + ". Lies ZUERST bugs/" + $file + " (falls in dieser Session noch nicht geschehen), bevor du bekannte Bugs einbaust."
    } else {
        $msg = "BUG-ALMANACH-HINWEIS: Du arbeitest an " + $name + ", aber es gibt noch KEINEN Almanach (bugs/" + $file + "). Hol Franks kurzes OK und STARTE dann den Skill 'bug-almanach-recherche' - das ist der vorgeschriebene, vollstaendige Weg; NICHT selbst ad hoc recherchieren."
    }

    $out = @{
        hookSpecificOutput = @{
            hookEventName = "PreToolUse"
            additionalContext = $msg
        }
    }
    Write-Output ($out | ConvertTo-Json -Depth 5 -Compress)
} catch {
    Hook-LogWarn "bug-almanac-guard: $_"
}

exit 0
