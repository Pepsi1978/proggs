# writeback-enforcer.ps1
$RepoRoot = "C:\Users\barwa\GeminiCLI"
$ScriptDir = "$RepoRoot/Gemini-Setup/scripts"

# Führe den Whiteboard-Merge aus
node "$ScriptDir/whiteboard-bridge.mjs" merge-sentinels --workspace "$RepoRoot"

Write-Host "✅ Whiteboard-Writeback abgeschlossen (PS7)." -ForegroundColor Green
