# Store-PowerShell im geschützten Paketordner darf nie als WSL-Interop-Programm im tmux-Wrapper landen.
$ErrorActionPreference = 'Stop'
Add-Type -Path (Join-Path $PSScriptRoot '../Services/TmuxLauncher.cs')
$T = [OpenLauncher.Services.TmuxLauncher]
$pf = 'C:\Program Files'
$local = 'C:\Users\test\AppData\Local'
$store = 'C:\Program Files\WindowsApps\Microsoft.PowerShell_7.6.6.0_x64__8wekyb3d8bbwe\pwsh.exe'

if (-not $T::IsProtectedPackagePath($store, $pf)) { throw 'Store-Paketpfad nicht als geschützt erkannt.' }
if ($T::IsProtectedPackagePath("$local\Microsoft\WindowsApps\pwsh.exe", $pf)) { throw 'App-Ausführungsverknüpfung fälschlich gesperrt.' }
$c = @($T::PowerShellCandidates($store, $pf, $local))
if ($c -contains $store) { throw 'Store-Paketpfad bleibt Kandidat.' }
if ($c[0] -ne "$local\Microsoft\WindowsApps\pwsh.exe") { throw "Verknüpfung nicht erster Kandidat: $($c -join ', ')" }
$c = @($T::PowerShellCandidates("$pf\PowerShell\7\pwsh.exe", $pf, $local))
if ($c[0] -ne "$pf\PowerShell\7\pwsh.exe") { throw 'Normale pwsh-Installation verliert Vorrang.' }
$ps5 = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
$c = @($T::PowerShellCandidates($ps5, $pf, $local))
if ($c.Count -ne 1 -or $c[0] -ne $ps5) { throw 'Windows PowerShell erhält fremde pwsh-Kandidaten.' }

# Echter Wrapper mit dem realen Store-Pfad dieses Rechners, falls vorhanden; keine Sitzung wird gestartet.
$realStore = (where.exe pwsh.exe 2>$null | Where-Object { $T::IsProtectedPackagePath($_, $env:ProgramFiles) } | Select-Object -First 1)
if ($realStore) {
    $dir = Join-Path $env:TEMP ('tmux-pwsh-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $dir | Out-Null
    try {
        $script = Join-Path $dir 'probe.ps1'
        Set-Content -LiteralPath $script -Value 'exit 0' -Encoding utf8
        $wrapper = $T::BuildStartScript($script, $dir, $realStore)
        $content = Get-Content -LiteralPath $wrapper -Raw
        if ($content -match 'Program Files/WindowsApps') { throw 'Wrapper nutzt weiterhin den gesperrten Paketpfad.' }
        if ($content -notmatch "'openlauncher-bootstrap'" -or $content -notmatch "WSL_INTEROP=/run/WSL/1_interop" -or $content -notmatch "'attach-session'") { throw 'Wrapper prüft die Sitzung nicht vor dem Anhängen.' }
        Remove-Item -LiteralPath $wrapper
        # Sofort endende CLI: klarer Fehler nach allen Startversuchen statt stilles "[exited]".
        Set-Content -LiteralPath $script -Value 'exit 1' -Encoding utf8
        $wrapper = $T::BuildStartScript($script, $dir, $realStore)
        $failed = $false
        try { & $wrapper } catch { $failed = $_.Exception.Message -match 'nach 3 tmux-Startversuchen' }
        Remove-Item -LiteralPath $wrapper
        if (-not $failed) { throw 'Sofort sterbende CLI wurde nicht als Fehler gemeldet.' }
        # Anhängefehler: ohne Terminal scheitert attach-session real (Exit != 0) und muss klar gemeldet werden.
        Set-Content -LiteralPath $script -Value 'Start-Sleep -Seconds 20' -Encoding utf8
        $wrapper = $T::BuildStartScript($script, $dir, $realStore)
        $content = Get-Content -LiteralPath $wrapper -Raw
        if ($content -notmatch "(?m)'attach-session'.*\r?\nif \(\`$LASTEXITCODE -ne 0\) \{ throw 'Anhängen") { throw 'attach-session ohne Exitcode-Prüfung.' }
        $session = [regex]::Match($content, 'tmux-Sitzung: (\S+) \| WSL: (\S+)')
        $failed = $false
        try { & $wrapper } catch { $failed = $_.Exception.Message -match 'Anhängen an die tmux-Sitzung' }
        finally { wsl -d $session.Groups[2].Value --exec tmux -L openlauncher kill-session -t ('=' + $session.Groups[1].Value) 2>$null }
        Remove-Item -LiteralPath $wrapper
        if (-not $failed) { throw 'Gescheitertes Anhängen wurde nicht als Fehler gemeldet.' }
    } finally {
        Get-ChildItem -LiteralPath $dir -File | Remove-Item
        Remove-Item -LiteralPath $dir
    }
    Write-Output 'OK: Store-PowerShell ersetzt, Wrapper legt Sitzung geprüft an und hängt dann an.'
} else {
    Write-Output 'OK: Kandidatenlogik; kein Store-pwsh auf diesem Rechner für den Wrapper-Test.'
}
