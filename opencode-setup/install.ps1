# install.ps1 — OpenCode-Umgebung aus dem plattformuebergreifenden Speicher installieren (Windows).
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
Backup (Join-Path $Dst 'AGENTS.md')
Copy-Item (Join-Path $Src 'AGENTS-global.md') (Join-Path $Dst 'AGENTS.md') -Force
Ok 'AGENTS.md (globale Regeln)'

$agents = Get-ChildItem (Join-Path $Src 'agents') -Filter *.md -ErrorAction SilentlyContinue
if ($agents) { $agents | Copy-Item -Destination (Join-Path $Dst 'agents') -Force; Ok 'agents/' } else { Warn 'keine agents/*.md' }

$plugins = Get-ChildItem (Join-Path $Src 'plugins') -Filter *.js -ErrorAction SilentlyContinue
if ($plugins) { $plugins | Copy-Item -Destination (Join-Path $Dst 'plugins') -Force; Ok 'plugins/ (inkl. tool-first-guard)' } else { Warn 'keine plugins/*.js' }

$pluginDirs = Get-ChildItem (Join-Path $Src 'plugins') -Directory -ErrorAction SilentlyContinue
if ($pluginDirs) { $pluginDirs | Copy-Item -Destination (Join-Path $Dst 'plugins') -Recurse -Force; Ok 'plugins/*/ (TUI-Plugin-Pakete)' } else { Warn 'keine plugins/*/' }

$npm = Get-Command npm -ErrorAction SilentlyContinue
if ($npm) {
  Push-Location $Dst
  try {
    & npm install --silent '@opencode-ai/plugin@1.17.7' '@opentui/core@0.3.4' '@opentui/solid@0.4.0' 'solid-js@1.9.12' | Out-Null
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
  sounds = [ordered]@{
    permission = (Join-Path $Dst 'sounds\permission.wav')
    complete   = (Join-Path $Dst 'sounds\complete.wav')
    error      = (Join-Path $Dst 'sounds\error.wav')
  }
}
Write-Utf8NoBom (Join-Path $Dst 'opencode-notifier.json') ($notifier | ConvertTo-Json -Depth 5)
Ok 'opencode-notifier.json (lokale Sound-Pfade, BOM-frei)'

# --- 5) Voraussetzungs-Check (nur Hinweise; das Setup selbst ist fertig) ---
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
Write-Host '  - npm-Plugins (@mohak34/opencode-notifier, @plannotator/opencode) installiert OpenCode selbst beim Start.'

Write-Host ''
if ($todo -eq 0) {
  Ok '== Fertig. Alle Voraussetzungen erfuellt -> "opencode" starten. =='
} else {
  Warn "== Dateien installiert. Noch $todo Voraussetzung(en) offen (siehe oben). =="
}
