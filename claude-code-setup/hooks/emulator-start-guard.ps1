# emulator-start-guard: PreToolUse-Blocker fuer nackte Android-Emulator-Starts.
# Logik in emulator-start-guard.py (gemeinsam mit der .sh-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
$ErrorActionPreference = "SilentlyContinue"
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin) {
        $py = Join-Path $PSScriptRoot "emulator-start-guard.py"
        $stdin | python $py 2>$null
    }
} catch { }
exit 0
