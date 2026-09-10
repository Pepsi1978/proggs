[CmdletBinding()]
param(
    # Claude-Code-Konfigurationsordner, dessen settings.json die Statuszeile bekommt.
    # Standard: CLAUDE_CONFIG_DIR (falls gesetzt), sonst ~/.claude.
    [string]$ConfigDir
)

# Richtet die Claude-Code-Statuszeile aus diesem Ordner auf einem Windows-Rechner ein:
#   1. prueft Git for Windows (Claude Code fuehrt die Statuszeile ueber Git Bash aus),
#   2. installiert jq per winget, falls es fehlt (die Statuszeile liest damit die Sitzungsdaten),
#   3. kopiert statusline.sh mit Unix-Zeilenenden nach ~/.claude/statusline-claudecode.sh,
#   4. traegt den statusLine-Block in settings.json ein (vorher Sicherungskopie, Rest bleibt),
#   5. rendert die Statuszeile einmal mit Beispieldaten zur Kontrolle.

$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
if (-not $ConfigDir) {
    $ConfigDir = if ($env:CLAUDE_CONFIG_DIR) { $env:CLAUDE_CONFIG_DIR } else { Join-Path $HOME '.claude' }
}

# --- 1. Git Bash -------------------------------------------------------------------------
# Bewusst NICHT "bash" aus dem PATH: das kann die WSL-bash aus System32 sein.
$gitBash = @(
    $env:CLAUDE_CODE_GIT_BASH_PATH,
    (Join-Path $env:ProgramFiles 'Git\bin\bash.exe'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Git\bin\bash.exe')
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
if (-not $gitBash) {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($git) {
        $candidate = Join-Path (Split-Path (Split-Path $git.Source)) 'bin\bash.exe'
        if (Test-Path -LiteralPath $candidate) { $gitBash = $candidate }
    }
}
if (-not $gitBash) {
    throw 'Git for Windows fehlt (Git Bash wird für die Statuszeile gebraucht). Installieren mit: winget install --id Git.Git -e'
}
Write-Host "Git Bash: $gitBash"

# --- 2. jq -------------------------------------------------------------------------------
$jqOrte = @((Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Links\jq.exe'), (Join-Path $HOME 'bin\jq.exe'))
$jqDa = (Get-Command jq -ErrorAction SilentlyContinue) -or ($jqOrte | Where-Object { Test-Path -LiteralPath $_ })
if (-not $jqDa) {
    Write-Host 'jq fehlt – wird per winget installiert ...'
    winget install --id jqlang.jq -e --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0) { throw "jq konnte nicht installiert werden (winget Exit-Code $LASTEXITCODE). Bitte manuell installieren: winget install --id jqlang.jq -e" }
}
else { Write-Host 'jq: vorhanden' }

# --- 3. Skript kopieren (immer LF) ---------------------------------------------------------
# Mit Windows-Zeilenenden (CRLF) bricht bash mit "$'\r': command not found" ab. Git kann beim
# Auschecken CRLF erzeugen; die .gitattributes verhindert das, hier wird es zusaetzlich erzwungen.
$zielSkript = Join-Path $HOME '.claude\statusline-claudecode.sh'
New-Item -ItemType Directory -Force -Path (Split-Path $zielSkript) | Out-Null
$text = [IO.File]::ReadAllText((Join-Path $here 'statusline.sh')) -replace "`r`n", "`n"
[IO.File]::WriteAllText($zielSkript, $text, [Text.UTF8Encoding]::new($false))
Write-Host "Skript kopiert: $zielSkript"

# --- 4. settings.json ----------------------------------------------------------------------
$settingsPath = Join-Path $ConfigDir 'settings.json'
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
$block = (Get-Content -LiteralPath (Join-Path $here 'statusline-settings.json') -Raw | ConvertFrom-Json).statusLine
if (Test-Path -LiteralPath $settingsPath) {
    $backup = "$settingsPath.bak-statusline-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Copy-Item -LiteralPath $settingsPath -Destination $backup
    Write-Host "Sicherung: $backup"
    $raw = Get-Content -LiteralPath $settingsPath -Raw
    $settings = if ([string]::IsNullOrWhiteSpace($raw)) { [pscustomobject]@{} } else { $raw | ConvertFrom-Json }
}
else {
    $settings = [pscustomobject]@{}
}
$settings | Add-Member -NotePropertyName statusLine -NotePropertyValue $block -Force
[IO.File]::WriteAllText($settingsPath, ($settings | ConvertTo-Json -Depth 100), [Text.UTF8Encoding]::new($false))
Write-Host "statusLine eingetragen: $settingsPath"

# --- 5. Probelauf -------------------------------------------------------------------------
$beispiel = '{"model":{"display_name":"Opus (1M context)"},"effort":{"level":"high"},"workspace":{"current_dir":"C:\\Users\\Beispiel\\proggs"},"context_window":{"used_percentage":12},"session_id":"installtest"}'
Write-Host ''
Write-Host 'Probelauf mit Beispieldaten:'
$beispiel | & $gitBash $zielSkript
Write-Host ''
Write-Host 'Fertig. Claude Code neu starten, dann erscheint die Statuszeile.'
exit 0
