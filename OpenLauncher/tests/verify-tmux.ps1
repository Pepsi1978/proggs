# Echter WSL/Windows-Transport, nur eigene kurzlebige Testsitzungen; keine Modellaufrufe.
$ErrorActionPreference = 'Stop'
Add-Type -Path (Join-Path $PSScriptRoot '../Services/TmuxLauncher.cs')
$dir = Join-Path $env:TEMP ('tmux Grüße $test '' ' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $dir | Out-Null
$pwsh = (Get-Command pwsh.exe).Source
function Literal([string]$text) { "'" + $text.Replace("'", "''") + "'" }
$sessions = @()
try {
    foreach ($cli in @('claude', 'codex', 'opencode')) {
        $exe = switch ($cli) {
            'claude' { Join-Path $env:APPDATA 'npm/node_modules/@anthropic-ai/claude-code/bin/claude.exe' }
            'codex' { (Get-Command codex.exe).Source }
            'opencode' {
                $root = Join-Path $env:USERPROFILE '.local/share/opencode-mousefix'
                $pointer = Get-Content (Join-Path $root 'current.json') -Raw | ConvertFrom-Json
                Join-Path $root $pointer.active.relativeExe
            }
        }
        if (-not (Test-Path -LiteralPath $exe)) { throw "CLI fehlt: $exe" }
        $output = Join-Path $dir ($cli + '.json')
        $script = Join-Path $dir ($cli + '.ps1')
        $text = "Set-Location -LiteralPath $(Literal $dir)`n"
        $text += "@{ cwd = (Get-Location).Path; marker = 'Grüße ''Zitat'' `$dollar'; version = (& $(Literal $exe) --version | Out-String).Trim() } | ConvertTo-Json | Set-Content -LiteralPath $(Literal $output) -Encoding utf8`n"
        $text += "Read-Host 'Test fertig' | Out-Null`n"
        [IO.File]::WriteAllText($script, $text, [Text.UTF8Encoding]::new($false))
        $wrapper = [OpenLauncher.Services.TmuxLauncher]::BuildStartScript($script, $dir, $pwsh)
        $content = Get-Content -LiteralPath $wrapper -Raw
        if ($content -notmatch 'tmux-Sitzung: (\S+) \| WSL: (\S+)') { throw 'Sitzungsmetadaten fehlen.' }
        $session = $Matches[1]; $distro = $Matches[2]
        $sessions += @{ session = $session; distro = $distro }
        # Nur der Client-Modus wird für den Test geändert; produktive Argumente bleiben erhalten.
        $detached = $content.Replace("'new-session' '-A'", "'new-session' '-d'")
        & ([scriptblock]::Create($detached))
        $deadline = [DateTime]::UtcNow.AddSeconds(25)
        while (-not (Test-Path -LiteralPath $output)) {
            if ([DateTime]::UtcNow -gt $deadline) { throw "$cli antwortet im tmux-Test nicht." }
            Start-Sleep -Milliseconds 100
        }
        $result = Get-Content -LiteralPath $output -Raw | ConvertFrom-Json
        if ($result.cwd -cne $dir -or $result.marker -cne "Grüße 'Zitat' `$dollar" -or -not $result.version) {
            throw "Pfad, Unicode oder CLI-Version verändert: $cli $($result | ConvertTo-Json -Compress)"
        }
        Write-Output "$cli über produktiven tmux-Wrapper: $($result.version); Pfad und Sonderzeichen korrekt."
    }
    $failed = $false
    try { [OpenLauncher.Services.TmuxLauncher]::BuildStartScript((Join-Path $dir 'fehlt.ps1'), $dir, $pwsh) }
    catch { $failed = $true }
    if (-not $failed) { throw 'Fehlendes Startskript wurde akzeptiert.' }
} finally {
    foreach ($item in $sessions) {
        wsl -d $item.distro --exec tmux -L openlauncher kill-session -t $item.session
    }
    # Nur einzelne Dateien im selbst erzeugten, verifizierten Testverzeichnis entfernen.
    Get-ChildItem -LiteralPath $dir -File | Remove-Item
    Remove-Item -LiteralPath $dir
}
