# session-start-audit.ps1
$RepoRoot = "C:\Users\barwa\GeminiCLI"
cd $RepoRoot

Write-Host "=== Cross-CLI Intelligenz-Audit (Start/PS7) ==="

# Codex Audit
Write-Host "Prüfe Codex CLI..."
$codexChanges = (node Gemini-Setup/scripts/audit-codex-delta.mjs | Select-String "\- A ").Count
if ($codexChanges -gt 0) {
    Write-Host "💡 Intelligenz-Vorschlag (Gemini): Es gibt $codexChanges neue Erweiterungen bei Codex. -> Starte bitte die Bruecke zu Codex für Details." -ForegroundColor Cyan
} else {
    Write-Host "✅ Codex ist synchron."
}

# Claude Audit
Write-Host "Prüfe Claude Code..."
$claudeChanges = (node Gemini-Setup/scripts/audit-claude-delta.mjs | Select-String "\- A ").Count
if ($claudeChanges -gt 0) {
    Write-Host "💡 Intelligenz-Vorschlag (Gemini): Es gibt $claudeChanges neue Erweiterungen bei Claude Code. -> Starte bitte die Bruecke zu Claude Code für Details." -ForegroundColor Cyan
} else {
    Write-Host "✅ Claude Code ist synchron."
}
