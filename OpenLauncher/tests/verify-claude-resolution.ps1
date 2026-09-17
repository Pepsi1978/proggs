# Führt die produktive Auflösung mit simulierten Installationen aus, ohne Claude-Sitzungen zu öffnen.
$ErrorActionPreference = 'Stop'
$source = Get-Content (Join-Path $PSScriptRoot '../Services/OpenLauncherService.cs') -Raw
$start = $source.IndexOf('    # Für beide Terminalarten dieselbe Installation auflösen:')
$end = $source.IndexOf('    & $claudeExecutable @claudeArgs', $start)
if ($start -lt 0 -or $end -lt 0) { throw 'Gemeinsamer Claude-Start fehlt.' }
$resolve = [scriptblock]::Create($source.Substring($start, $end - $start))
$native = Join-Path $env:USERPROFILE '.local/bin/claude.exe'
$npm = Join-Path $env:APPDATA 'npm/node_modules/@anthropic-ai/claude-code/bin/claude.exe'
$testShim = Join-Path $env:TEMP 'fnm-test/claude.ps1'
$shimTarget = Join-Path (Split-Path $testShim -Parent) 'node_modules/@anthropic-ai/claude-code/bin/claude.exe'
$pathExe = Join-Path $env:TEMP 'custom-claude/claude.exe'

function Get-Command {
    param($Name, [switch]$All, $ErrorAction)
    if ($Name -eq 'claude.exe') {
        if ($script:offerPath) { [pscustomobject]@{ Source = $pathExe } }
    } else { [pscustomobject]@{ Source = $testShim } }
}
function Test-Path {
    param($LiteralPath, $PathType)
    return $LiteralPath -in $script:existing
}

$cases = @(
    @{ Name = 'Defekter fnm-Wrapper'; Files = @($testShim, $npm); Expected = $npm; Path = $false },
    @{ Name = 'Native Installation'; Files = @($native, $npm); Expected = $native; Path = $false },
    @{ Name = 'Intakte fnm-Installation'; Files = @($testShim, $shimTarget); Expected = $shimTarget; Path = $false },
    @{ Name = 'EXE im PATH'; Files = @($pathExe); Expected = $pathExe; Path = $true },
    @{ Name = 'Keine Installation'; Files = @($testShim); Expected = $null; Path = $false }
)
foreach ($case in $cases) {
    foreach ($embeddedTerminal in @($false, $true)) {
        foreach ($colorName in @('blue', '')) {
            $script:existing = $case.Files
            $script:offerPath = $case.Path
            $claudeArgs = @('--model', 'test-model', '--effort', 'high', '--settings', 'path with spaces.json')
            $expectedArgs = @($claudeArgs)
            if (-not $embeddedTerminal) { $expectedArgs += $(if ($colorName) { "/color $colorName" } else { '/color' }) }
            $failed = $false
            try { . $resolve } catch {
                if ($case.Expected -or $_ -notlike '*Keine funktionsfähige Claude-Installation*') { throw }
                $failed = $true
            }
            if (-not $case.Expected) {
                if (-not $failed) { throw 'Fehlende Installation wurde nicht gemeldet.' }
            } else {
                if ($claudeExecutable -ne $case.Expected) { throw "Falsche EXE: $claudeExecutable" }
                if (($claudeArgs | ConvertTo-Json -Compress) -cne ($expectedArgs | ConvertTo-Json -Compress)) { throw 'Startargumente verändert.' }
            }
        }
    }
    Write-Output "PASS: $($case.Name), normal/eingebettet, mit/ohne Farbe."
}
