# rule-size-guard: PreToolUse-Blocker fuer Regel-Dateien > 2 KB (rule-size-limit.md).
# Logik in rule-size-guard.py (gemeinsam mit der .sh-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
$ErrorActionPreference = "SilentlyContinue"
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin) {
        $py = Join-Path $PSScriptRoot "rule-size-guard.py"
        $stdin | python $py 2>$null
    }
} catch { }
exit 0
