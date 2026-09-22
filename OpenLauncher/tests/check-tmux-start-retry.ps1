# Kontrollierte tmux-Starts über echtes WSL: Bootstrap-Fenster, CIM-Bestätigung, bis zu 3 Versuche mit Backoff,
# CLI unabhängig vom wsl.exe-Halteprozess. Eigener frischer tmux-Socket: ein laufender Launcher-Server würde
# mit seinem alten Interop-Socket den Fehler verdecken. Keine Modellaufrufe.
$ErrorActionPreference = 'Stop'
Add-Type -Path (Join-Path $PSScriptRoot '../Services/TmuxLauncher.cs')
$T = [OpenLauncher.Services.TmuxLauncher]
$pwsh = (Get-Command pwsh.exe).Source
$dir = Join-Path $env:TEMP ('tmux-retry ' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $dir | Out-Null
$sessions = @()
$socket = 'olretry-' + [guid]::NewGuid().ToString('N').Substring(0, 12)

# Muster trifft nur den exakten inneren Skriptnamen, nie den Wrapper oder längere Namen.
$p = $T::CliPattern('C:\x\claude.ps1')
if ('pwsh.exe -File C:\x\claude.ps1' -notmatch $p) { throw 'Muster verfehlt das Startskript.' }
if ('pwsh.exe -File "C:\x\claude.ps1"' -notmatch $p) { throw 'Muster verfehlt das gequotete Startskript.' }
foreach ($other in 'pwsh.exe -File C:\x\claude-tmux.ps1', 'pwsh.exe -File C:\x\myclaude.ps1', 'pwsh.exe -File C:\x\claude.ps1x') {
    if ($other -match $p) { throw "Muster trifft fremdes Skript: $other" }
}
if ($T::IsPlainToken('Ubuntu 22') -or $T::IsPlainToken("a'b") -or -not $T::IsPlainToken('/usr/bin/tmux')) { throw 'Token-Prüfung falsch.' }

function New-Probe([string]$name, [int]$okFrom) {
    $counter = Join-Path $dir "$name.count"
    $script = Join-Path $dir "$name.ps1"
    $text = "`$n = [int](Get-Content -LiteralPath '$counter' -ErrorAction SilentlyContinue) + 1`n"
    $text += "Set-Content -LiteralPath '$counter' -Value `$n`n"
    $text += "if (`$n -lt $okFrom) { exit 1 }`nStart-Sleep -Seconds 60`n"
    [IO.File]::WriteAllText($script, $text, [Text.UTF8Encoding]::new($false))
    $wrapper = $T::BuildStartScript($script, $dir, $pwsh, $socket)
    $content = Get-Content -LiteralPath $wrapper -Raw
    if ($content -notmatch 'tmux-Sitzung: (\S+) \| WSL: (\S+)') { throw 'Sitzungsmetadaten fehlen.' }
    $info = @{ wrapper = $wrapper; content = $content; counter = $counter; script = $script; session = $Matches[1]; distro = $Matches[2] }
    $script:sessions += $info
    $info
}
function Tmux($info) { & wsl.exe -d $info.distro --exec tmux -L $socket @args 2>$null }
function Run-Detached($info) {
    # Ohne Terminal kann attach-session nicht laufen; alles davor ist produktiver Code.
    $detached = $info.content -replace "(?m)^.*'attach-session'.*\r?\n.*\r?\n?", ''
    & ([scriptblock]::Create($detached)) 6>&1 | Out-String
}
function Holders($info) {
    @(Get-CimInstance Win32_Process -Filter "Name='wsl.exe'" | Where-Object { $_.CommandLine -match [regex]::Escape($info.session) })
}
function CliPid($info) {
    @(Get-CimInstance Win32_Process -Filter "Name='pwsh.exe'" | Where-Object { $_.CommandLine -match $T::CliPattern($info.script) }).ProcessId
}

try {
    $any = New-Probe 'statisch' 1
    foreach ($forbidden in "'sh'", "'bash'", "'-lc'", 'Invoke-Expression') {
        if ($any.content.Contains($forbidden)) { throw "Wrapper setzt Shellbefehle zusammen: $forbidden" }
    }
    if ($any.content -notmatch 'WSL_INTEROP=/run/WSL/1_interop') { throw 'CLI hängt weiter am Interop eines kurzlebigen wsl.exe.' }

    # 1. Erster Start stirbt, zweiter läuft: Neustart nach Backoff, danach nur noch das CLI-Fenster.
    $flaky = New-Probe 'wackelig' 2
    $out = Run-Detached $flaky
    if ($out -notmatch 'Neustart 2/3') { throw "Kein sichtbarer zweiter Versuch: $out" }
    if ((Get-Content -LiteralPath $flaky.counter) -ne '2') { throw 'Startskript nicht genau zweimal gelaufen.' }
    $windows = @(Tmux $flaky list-windows -t ('=' + $flaky.session) -F '#{window_name}')
    if ($windows.Count -ne 1 -or $windows[0] -ne $T::CliWindow) { throw "Bootstrap-Fenster blieb stehen: $($windows -join ', ')" }
    # 2. Halteprozess ist freigegeben und beendet; die CLI überlebt das Ende jedes wsl.exe.
    Start-Sleep -Seconds 3
    if ((Holders $flaky).Count) { throw 'wsl.exe-Halteprozess läuft nach dem Start weiter.' }
    if (-not (CliPid $flaky)) { throw 'CLI-PowerShell starb mit dem Halteprozess.' }
    Write-Output 'OK: Zweiter Versuch bestätigt, Bootstrap entfernt, CLI unabhängig vom Halteprozess.'

    # 3. Dauerhaft sterbende CLI: genau 3 Versuche, Backoff 2 s + 4 s, klarer Fehler, keine Reste.
    $dead = New-Probe 'tot' 99
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $failed = $false
    try { Run-Detached $dead | Out-Null } catch { $failed = $_.Exception.Message -match 'nach 3 tmux-Startversuchen' }
    if (-not $failed) { throw 'Dauerhaft sterbende CLI nicht als Fehler gemeldet.' }
    if ((Get-Content -LiteralPath $dead.counter) -ne '3') { throw 'Nicht genau 3 Startversuche.' }
    if ($watch.Elapsed.TotalSeconds -lt 6) { throw 'Backoff zwischen den Versuchen fehlt.' }
    Tmux $dead has-session -t ('=' + $dead.session) | Out-Null
    if ($LASTEXITCODE -eq 0) { throw 'Gescheiterte Zielsitzung blieb bestehen.' }
    if ((Holders $dead).Count) { throw 'Halteprozess nach Fehlschlag nicht beendet.' }
    Write-Output 'OK: 3 kontrollierte Versuche mit Backoff, danach Fehler ohne Restsitzung.'

    # 4. Erneuter Aufruf desselben Wrappers bei laufender CLI startet nichts neu.
    $before = CliPid $flaky
    $out = Run-Detached $flaky
    if ((Get-Content -LiteralPath $flaky.counter) -ne '2' -or (CliPid $flaky) -ne $before) { throw 'Wiederanhängen startete die CLI neu.' }
    Write-Output 'OK: Wiederanhängen lässt die laufende CLI unverändert.'
} finally {
    foreach ($info in $sessions) { Remove-Item -LiteralPath $info.wrapper -ErrorAction SilentlyContinue }
    if ($sessions) { Tmux $sessions[0] kill-server | Out-Null }
    Get-ChildItem -LiteralPath $dir -File | Remove-Item
    Remove-Item -LiteralPath $dir
}
