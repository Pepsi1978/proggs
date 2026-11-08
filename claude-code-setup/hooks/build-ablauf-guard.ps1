# build-ablauf-guard: PreToolUse-Blocker: Android-Bau vor Versions-Bump und Kill-Befehle, die die eigene Shell treffen.
# Logik in build-ablauf-guard.py (gemeinsam mit der .sh-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
$ErrorActionPreference = "SilentlyContinue"
try {
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin) {
        $py = Join-Path $PSScriptRoot "build-ablauf-guard.py"
        $stdin | python $py 2>$null
    }
} catch { }
exit 0
