# session-start-audit.ps1 (Gemini) ÔÇö Automatisches Cross-CLI Audit bei Session-Start
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }
cd $RepoRoot

# Lade Whiteboard-Funktionalit├ñt
. "$RepoRoot\Gemini-Setup\scripts\whiteboard-insert.ps1"

Write-Host "=== ­ƒÆí Gemini Cross-CLI Intelligence Audit (Start/PS7) ===" -ForegroundColor Cyan

function Process-DeltaAudit($CliName, $ScriptPath) {
    Write-Host "Pruefe $CliName..."
    $AuditOutput = node "$RepoRoot\$ScriptPath"
    $NewCommits = $AuditOutput | Select-String "\[([a-f0-9]{7})\]"
    
    if ($NewCommits.Count -gt 0) {
        $Count = $NewCommits.Count
        Write-Host "­ƒÆí $CliName: $Count neue Commits gefunden." -ForegroundColor Yellow
        
        $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
        $Entry = "### $Timestamp ÔÇö Br├╝cken-Audit ($CliName): $Count neue Commits seit dem letzten Checkpoint gefunden. Empfehlung: Br├╝cke zu $CliName starten (`audit-$($CliName.ToLower().Replace(' ', '-'))-delta.mjs`)."
        
        # In Whiteboard unter "Offene Fehler & Probleme" oder eine neue Sektion schreiben
        Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $Entry
    } else {
        Write-Host "Ô£à $CliName ist synchron." -ForegroundColor Green
    }
}

# Codex Audit
Process-DeltaAudit "Codex" "Gemini-Setup/scripts/audit-codex-delta.mjs"

# Claude Audit
Process-DeltaAudit "Claude Code" "Gemini-Setup/scripts/audit-claude-delta.mjs"

# NEU: Proaktiver Briefkasten-Check (PORTING-LIST.md)
Write-Host "`n--- Starte Briefkasten-Check (PORTING-LIST.md) ---" -ForegroundColor Yellow
$PortingPaths = @(
    @{ name = "Codex"; path = "$RepoRoot\codex-setup\PORTING-LIST.md" },
    @{ name = "Claude Code"; path = "$RepoRoot\claude-code-setup\PORTING-LIST.md" }
)

foreach ($cp in $PortingPaths) {
    if (Test-Path $cp.path) {
        Write-Host "­ƒô¼ Neue Nachricht von $($cp.name) gefunden!" -ForegroundColor Green
        # Starte automatische Extraktion der Details ins Whiteboard
        powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$RepoRoot\Gemini-Setup\scripts\read-and-extract-porting-list.ps1"
    } else {
        Write-Host "­ƒô¡ Keine direkte Nachricht von $($cp.name)." -ForegroundColor DarkGray
    }
}

Write-Host "=== Audit abgeschlossen ==="
