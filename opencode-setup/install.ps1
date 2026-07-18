# install.ps1 — OpenCode-Umgebung aus dem plattformuebergreifenden Speicher installieren (Windows).
# Installer v1.1.0 - 18.07.2026, 12:54 Uhr
#
# Kopiert ALLES aus opencode-setup\ an seinen Platz unter %USERPROFILE%\.config\opencode\, sodass
# OpenCode auf einem frischen Rechner 1:1 dieselbe Umgebung hat: globale Config, globale Regeln
# (AGENTS.md), globale Agents, lokale Plugins, TUI-Plugin-Dependencies, Notifier-Sounds. Danach ein Voraussetzungs-Check
# (SK-Keys, OPENROUTER_API_KEY, WireGuard/Second-Brain, opencode-Auth) mit klarer TODO-Liste.
#
# Voraussetzung: OpenCode-CLI ist bereits installiert (siehe README, Schritt 1).
# Aufruf:        pwsh ~/proggs/opencode-setup/install.ps1
#
# Idempotent: vorhandene Dateien werden vor dem Ueberschreiben nach .backup-<zeit>\ gesichert.
# Die "shell"-Zeile bleibt "pwsh" (Windows). opencode-notifier.json wird mit den korrekten lokalen
# Windows-Sound-Pfaden NEU erzeugt (BOM-frei).

param(
  [switch]$SkipWindowsFix,
  [switch]$SkipLauncherBuild
)

$ErrorActionPreference = 'Stop'

$Src = $PSScriptRoot                                  # opencode-setup\
# Zielort (per Env OPENCODE_SETUP_DST ueberschreibbar, z.B. zum Testen):
$Dst = if ($env:OPENCODE_SETUP_DST) { $env:OPENCODE_SETUP_DST } else { Join-Path $env:USERPROFILE '.config\opencode' }

function Ok($m)   { Write-Host "OK  $m" -ForegroundColor Green }
function Warn($m) { Write-Host "--  $m" -ForegroundColor Yellow }
function Err($m)  { Write-Host "!!  $m" -ForegroundColor Red }

function Write-Utf8NoBom([string]$Path, [string]$Content) {
  # BOM-frei schreiben (Claude-Config-Almanach 3.2: BOM bricht JSON-Parse auf Windows).
  [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding $false))
}

Write-Host "== OpenCode-Setup -> $Dst =="

# --- 0) OpenCode installiert? (nur Hinweis, kein Abbruch) ---
if (Get-Command opencode -ErrorAction SilentlyContinue) {
  $ver = (& opencode --version 2>$null) -join ''
  Ok "OpenCode-CLI gefunden: $ver"
} else {
  Err "OpenCode-CLI NICHT gefunden. Zuerst installieren (siehe README, Schritt 1), z.B.:"
  Write-Host "      scoop install opencode        # Windows (empfohlen, setzt PATH)"
  Write-Host "      choco install opencode        # Chocolatey"
}

# --- 1) Zielverzeichnisse + Backup ---
New-Item -ItemType Directory -Force -Path $Dst, (Join-Path $Dst 'agents'), (Join-Path $Dst 'plugins'), (Join-Path $Dst 'sounds'), (Join-Path $Dst 'skill') | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
function Backup([string]$Path) {
  if (Test-Path $Path) {
    $bdir = Join-Path $Dst ".backup-$stamp"
    New-Item -ItemType Directory -Force -Path $bdir | Out-Null
    Copy-Item $Path $bdir -Recurse -Force -ErrorAction SilentlyContinue
  }
}

# --- 2) Globale Config (shell bleibt pwsh auf Windows -> 1:1 kopieren) ---
Backup (Join-Path $Dst 'opencode.jsonc')
Copy-Item (Join-Path $Src 'opencode.jsonc') (Join-Path $Dst 'opencode.jsonc') -Force
Ok 'opencode.jsonc (shell=pwsh)'

Backup (Join-Path $Dst 'tui.json')
Copy-Item (Join-Path $Src 'tui.json') (Join-Path $Dst 'tui.json') -Force
Ok 'tui.json (TUI-Plugins)'

# --- 3) Globale Regeln, Agents, Plugins ---
$agentsPath = Join-Path $Dst 'AGENTS.md'
if (Test-Path $agentsPath) {
  Backup $agentsPath
  Warn 'vorhandene AGENTS.md beibehalten (wird vom Launcher verlustfrei migriert)'
} else {
  Copy-Item (Join-Path $Src 'AGENTS-global.md') $agentsPath
  Ok 'AGENTS.md (Standardvorlage fuer die erste Profilmigration)'
}

$agents = Get-ChildItem (Join-Path $Src 'agents') -Filter *.md -ErrorAction SilentlyContinue
if ($agents) { $agents | Copy-Item -Destination (Join-Path $Dst 'agents') -Force; Ok 'agents/' } else { Warn 'keine agents/*.md' }

$plugins = Get-ChildItem (Join-Path $Src 'plugins') -File -ErrorAction SilentlyContinue | Where-Object {
  $_.Extension -in '.js', '.mjs' -and $_.Name -notlike '*.test.mjs'
}
if ($plugins) { $plugins | Copy-Item -Destination (Join-Path $Dst 'plugins') -Force; Ok 'plugins/ (inkl. Notifier-Vertrag)' } else { Warn 'keine plugins/*.{js,mjs}' }

$pluginDirs = Get-ChildItem (Join-Path $Src 'plugins') -Directory -ErrorAction SilentlyContinue
if ($pluginDirs) { $pluginDirs | Copy-Item -Destination (Join-Path $Dst 'plugins') -Recurse -Force; Ok 'plugins/*/ (TUI-Plugin-Pakete)' } else { Warn 'keine plugins/*/' }

$terminalTitle = Join-Path $Src 'plugins\windows\terminal-task-title.js'
if (Test-Path -LiteralPath $terminalTitle) {
  Copy-Item -LiteralPath $terminalTitle -Destination (Join-Path $Dst 'plugins\terminal-task-title.js') -Force
  Remove-Item -LiteralPath (Join-Path $Dst 'plugins\terminal-task-title.ts') -Force -ErrorAction SilentlyContinue
  Ok 'terminal-task-title.js (Windows, headless deaktiviert)'
}

# Entfernt die mit Plugin 1.1.0 ausgelieferten Theme-Duplikate. Seit 1.2.0 nutzt das Dropdown
# ausschliesslich die eingebauten OpenCode-Themes.
$obsoleteThemes = @(
  (Join-Path $Dst 'themes\frank-dark.json'),
  (Join-Path $Dst 'themes\frank-light.json'),
  (Join-Path $Dst 'plugins\token-cost-sidebar\themes\frank-dark.json'),
  (Join-Path $Dst 'plugins\token-cost-sidebar\themes\frank-light.json')
)
$obsoleteThemes | Remove-Item -Force -ErrorAction SilentlyContinue
$obsoletePluginThemeDir = Join-Path $Dst 'plugins\token-cost-sidebar\themes'
if ((Test-Path $obsoletePluginThemeDir) -and -not (Get-ChildItem $obsoletePluginThemeDir -Force)) {
  Remove-Item $obsoletePluginThemeDir -Force
}

$npm = Get-Command npm -ErrorAction SilentlyContinue
if ($npm) {
  $notifierVersion = '0.2.8'
  $installedNotifierPackage = Join-Path $Dst 'node_modules\@mohak34\opencode-notifier\package.json'
  if (Test-Path -LiteralPath $installedNotifierPackage) {
    try {
      $installedNotifierVersion = ([IO.File]::ReadAllText($installedNotifierPackage) | ConvertFrom-Json).version
      if ($installedNotifierVersion -match '^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$') { $notifierVersion = $installedNotifierVersion }
    } catch { Warn 'Installierte Notifier-Version unlesbar -> sichere Basis 0.2.8 wird verwendet' }
  }
  $dependencies = @(
    '@opencode-ai/plugin@1.17.15',
    '@opentui/core@0.4.3',
    '@opentui/solid@0.4.3',
    'solid-js@1.9.12',
    "@mohak34/opencode-notifier@$notifierVersion"
  )
  Push-Location $Dst
  try {
    & npm install --silent --save-exact @dependencies | Out-Null
    if ($LASTEXITCODE -eq 0) { Ok 'TUI-Plugin-Dependencies (npm)' } else { Warn 'TUI-Plugin-Dependencies konnten nicht installiert werden' }
  } finally {
    Pop-Location
  }
} else {
  Warn 'npm nicht gefunden -> TUI-Plugin-Dependencies manuell in ~/.config/opencode installieren'
}

$skills = Get-ChildItem (Join-Path $Src 'skill') -Directory -ErrorAction SilentlyContinue
if ($skills) { $skills | Copy-Item -Destination (Join-Path $Dst 'skill') -Recurse -Force; Ok 'skill/ (OpenCode-Skills, z.B. session-opencode)' } else { Warn 'keine skill/*' }

# --- 4) Notifier-Sounds + Config (Windows-Pfade lokal erzeugen, BOM-frei) ---
$wavs = Get-ChildItem (Join-Path $Src 'sounds') -Filter *.wav -ErrorAction SilentlyContinue
if ($wavs) { $wavs | Copy-Item -Destination (Join-Path $Dst 'sounds') -Force; Ok 'sounds/' } else { Warn 'keine sounds/*.wav' }

Backup (Join-Path $Dst 'opencode-notifier.json')
$notifier = [ordered]@{
  sound = $true
  notification = $true
  showSessionTitle = $true
  suppressWhenFocused = $false
  minDuration = 0
  events = [ordered]@{
    permission = $false
    complete = $true
    subagent_complete = $false
    error = $false
    question = $true
    interrupted = $false
    user_cancelled = $false
    plan_exit = $false
    session_started = $false
    user_message = $false
    client_connected = $false
  }
  messages = [ordered]@{
    permission = 'OpenCode benötigt eine Freigabe: {sessionTitle}'
    complete = 'Arbeit abgeschlossen: {sessionTitle}'
    subagent_complete = 'Unteraufgabe abgeschlossen: {sessionTitle}'
    error = 'Fehler in der Session: {sessionTitle}'
    question = 'OpenCode hat eine Frage: {sessionTitle}'
    interrupted = 'Session wurde unterbrochen: {sessionTitle}'
    user_cancelled = 'Session wurde abgebrochen: {sessionTitle}'
    plan_exit = 'Plan ist zur Prüfung bereit: {sessionTitle}'
  }
  sounds = [ordered]@{
    permission = (Join-Path $Dst 'sounds\permission.wav')
    complete   = (Join-Path $Dst 'sounds\complete.wav')
    subagent_complete = (Join-Path $Dst 'sounds\complete.wav')
    error      = (Join-Path $Dst 'sounds\error.wav')
    question   = (Join-Path $Dst 'sounds\permission.wav')
    interrupted = (Join-Path $Dst 'sounds\error.wav')
    user_cancelled = (Join-Path $Dst 'sounds\error.wav')
    plan_exit  = (Join-Path $Dst 'sounds\permission.wav')
  }
}
Write-Utf8NoBom (Join-Path $Dst 'opencode-notifier.json') ($notifier | ConvertTo-Json -Depth 5)
Ok 'opencode-notifier.json (lokale Sound-Pfade, BOM-frei)'

$notifierUpdater = Join-Path $Dst 'plugins\notifier-auto-updater.mjs'
$node = Get-Command node -ErrorAction SilentlyContinue
if ($node -and (Test-Path -LiteralPath $notifierUpdater)) {
  & $node.Source $notifierUpdater --config-dir $Dst --force --verbose
  if ($LASTEXITCODE -eq 0) { Ok 'Notifier täglich geprüft und Vertragsregeln verifiziert' } else { Warn 'Notifier-Update verworfen -> letzte funktionierende Version bleibt aktiv' }
} else {
  Warn 'Node oder Notifier-Updater fehlt -> tägliche Prüfung startet beim nächsten OpenCode-Start erneut'
}

# --- 5) Windows-Stabilitätsbuild + Launcher (auf Testzielen überspringbar) ---
if (-not $SkipWindowsFix) {
  $windowsFix = Join-Path $Src 'build-install-windows-mousefix.ps1'
  & $windowsFix -Force
  if ($LASTEXITCODE -ne 0) { throw 'Windows-Stabilitätsbuild fehlgeschlagen; bestehende Version bleibt aktiv.' }
  Ok 'OpenCode-Windows-Fix (Maus, Full-Repaint, TUI-Variant, Auto-Update)'
}

if (-not $SkipLauncherBuild) {
  $launcherDir = Join-Path (Split-Path $Src -Parent) 'OpenCodeLauncher'
  $dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
  if ($dotnet -and (Test-Path -LiteralPath $launcherDir)) {
    & dotnet build (Join-Path $launcherDir 'OpenCodeLauncher.csproj') -c Release
    if ($LASTEXITCODE -ne 0) { throw 'OpenCode Launcher konnte nicht gebaut werden.' }
    & (Join-Path $launcherDir 'create_shortcut.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Launcher-Verknüpfung konnte nicht erstellt werden.' }
    Ok 'OpenCode Launcher + Desktop-Verknüpfung'
  } else {
    Warn 'dotnet 8 oder OpenCodeLauncher fehlt -> Launcher nicht gebaut'
  }
}

# --- 6) Voraussetzungs-Check (nur Hinweise; das Setup selbst ist fertig) ---
Write-Host ''
Write-Host '== Voraussetzungs-Check (was noch fehlt, manuell erledigen) =='
$todo = 0
function CheckFile([string]$Path, [string]$Desc) {
  if ((Test-Path $Path) -and ((Get-Item $Path).Length -gt 0)) { Ok $Desc } else { Err "FEHLT: $Desc ($Path)"; $script:todo++ }
}
CheckFile (Join-Path $env:USERPROFILE 'SK\OpenCode\firecrawl-api-key.txt') 'Firecrawl-Key (Recherche Engine A)'
CheckFile (Join-Path $env:USERPROFILE 'SK\OpenCode\go-api-key.txt') 'OpenCode-Go-Key (MiniMax-Recherche)'
CheckFile (Join-Path $env:USERPROFILE 'SK\ClaudeCodeOpenRouter\openrouter.key') 'OpenRouter-Key (Engine B)'

if ($env:OPENROUTER_API_KEY) {
  Ok 'OPENROUTER_API_KEY gesetzt (Owl-Provider)'
} else {
  Err 'OPENROUTER_API_KEY NICHT gesetzt -> Owl-Provider unbenutzbar. Als User-Umgebungsvariable setzen.'
  $todo++
}

if (Test-Connection -ComputerName 10.8.0.1 -Count 1 -Quiet -ErrorAction SilentlyContinue) {
  Ok 'WireGuard-Tunnel aktiv (10.8.0.1 erreichbar) -> Second-Brain-MCP nutzbar'
} else {
  Warn '10.8.0.1 nicht erreichbar -> WireGuard-Tunnel starten, sonst kein Second-Brain (bugs/server/wireguard.md)'
  $todo++
}

Write-Host ''
Warn 'Noch manuell (interaktiv, nicht skriptbar):'
Write-Host '  - opencode auth login   (bzw. /connect in der TUI) fuer das Go-Abo (opencode-go/MiniMax + Plan)'
Write-Host "  - Beim ERSTEN Prompt muss OpenCode melden: 'N Regeln aus dem zweiten Gehirn eingelesen.'"
Write-Host '  - @plannotator/opencode installiert OpenCode beim Start; der Notifier wurde lokal als Guard-Abhängigkeit installiert.'

Write-Host ''
if ($todo -eq 0) {
  Ok '== Fertig. Alle Voraussetzungen erfuellt -> "opencode" starten. =='
} else {
  Warn "== Dateien installiert. Noch $todo Voraussetzung(en) offen (siehe oben). =="
}
