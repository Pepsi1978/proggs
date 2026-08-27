# claude-login-sync: spiegelt den Claude-Code-Login auf alle Profil-Konfigurationsordner.
# Läuft als SessionStart- und SessionEnd-Hook; Logik in claude-login-sync.py (gemeinsam mit .sh).
# Ohne diesen Abgleich verlangt jedes Profil (OpenLauncher: minimal/standard/strict) eine
# eigene Anmeldung, weil Claude Code den Token je CLAUDE_CONFIG_DIR getrennt ablegt
# (Windows: <configdir>\.credentials.json). Siehe bugs/claude-tooling/claude-config.md.
#
# Die Ausgabe geht bewusst nicht in Claudes Kontext; das Protokoll steht in
# ~/.claude/logs/claude-login-sync.jsonl. Fail-open, endet immer mit 0.
$ErrorActionPreference = "SilentlyContinue"
try {
    $null = [Console]::In.ReadToEnd()
    $py = Join-Path $PSScriptRoot "claude-login-sync.py"
    $null = python $py 2>$null
} catch { }
exit 0
