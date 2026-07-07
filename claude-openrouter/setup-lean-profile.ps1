<#
================================================================================
  Legt das schlanke Claude-Profil "~/.claude-openrouter" an (idempotent).
================================================================================
  Zweck: Eine zweite, schlanke Claude-Code-Umgebung NUR fuer OpenRouter-Sessions.
  Sie laedt absichtlich WENIG, damit die Modelle guenstig + schnell laufen
  (kein Caching bei OpenRouter -> jeder Token Kontext kostet bei jeder Frage):

    - rules/   : NUR Direktive #3 (resilient-bugfixing.md), nicht die 40 user-Regeln
    - skills/  : Junction auf deine eigenen Skills (~/.claude/skills) — bleiben synchron
    - settings : bypassPermissions, KEINE Plugins (kein claude-mem), Auto-Memory AUS,
                 KEIN model "opus[1m]" (sonst haengt Claude Code das "[1m]" an OpenRouter-Slugs)

  Dein normales ~/.claude (voller Harness + Opus-Abo) bleibt voellig unberuehrt.
  Aufruf:  pwsh -File setup-lean-profile.ps1
================================================================================
#>
$ErrorActionPreference = 'Stop'
$lean    = Join-Path $HOME '.claude-openrouter'
# KOMPAKT-Fassung von Direktive #3 (~2K statt ~8K) — liegt versioniert im Repo:
$srcRule = Join-Path $HOME 'proggs\claude-openrouter\resilient-bugfixing-kompakt.md'
$srcSkil = Join-Path $HOME '.claude\skills'

New-Item -ItemType Directory -Force -Path (Join-Path $lean 'rules') | Out-Null

if (Test-Path $srcRule) {
    Copy-Item $srcRule (Join-Path $lean 'rules\resilient-bugfixing.md') -Force
    Write-Host "rules/: Direktive #3 (Kompaktfassung) kopiert."
} else { Write-Host "WARNUNG: $srcRule fehlt — Direktive #3 nicht kopiert." -ForegroundColor Yellow }

# Skills: Junction (kein Admin noetig) auf die eigenen user-Skills
$skillsLink = Join-Path $lean 'skills'
if (Test-Path $skillsLink) { (Get-Item $skillsLink).Delete() }
if (Test-Path $srcSkil) {
    New-Item -ItemType Junction -Path $skillsLink -Target $srcSkil | Out-Null
    Write-Host "skills/: Junction auf $srcSkil ($((Get-ChildItem $skillsLink -Directory).Count) Skills)."
} else { Write-Host "WARNUNG: $srcSkil fehlt — keine Skills verlinkt." -ForegroundColor Yellow }

# Schlanke settings.json (UTF-8 OHNE BOM — sonst bricht der JSON-Parse auf Windows)
$json = @'
{
  "permissions": { "defaultMode": "bypassPermissions" },
  "autoMemoryEnabled": false,
  "enabledPlugins": {},
  "env": {
    "CLAUDE_CODE_DISABLE_AUTO_MEMORY": "1",
    "DISABLE_TELEMETRY": "1"
  }
}
'@
[IO.File]::WriteAllText((Join-Path $lean 'settings.json'), $json, (New-Object Text.UTF8Encoding $false))
Write-Host "settings.json: geschrieben (BOM-frei)."
Write-Host "`nSchlankes Profil bereit: $lean" -ForegroundColor Green
