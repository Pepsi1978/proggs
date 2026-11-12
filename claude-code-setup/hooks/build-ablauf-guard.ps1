# build-ablauf-guard: PreToolUse-Blocker: Android-Bau vor Versions-Bump und Kill-Befehle, die die eigene Shell treffen.
# Logik in build-ablauf-guard.py (gemeinsam mit der .sh-Version). Blockt via permissionDecision=deny.
# Fail-open: faellt Python aus, wird nicht blockiert (kein Crash, kein Hook-Fehler).
$ErrorActionPreference = "SilentlyContinue"
try {
    # UTF-8 auf beiden Seiten: Sonst liest pwsh die Hook-Eingabe als OEM-Codepage und Python als cp1252 (Umlaute im
    # Pfad oder Befehl werden zu Mojibake, einzelne Bytes lassen json.load scheitern und den Hook still durchlassen).
    [Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $stdin = [Console]::In.ReadToEnd()
    if ($stdin) {
        $py = Join-Path $PSScriptRoot "build-ablauf-guard.py"
        $stdin | python -X utf8 $py 2>$null
    }
} catch { }
exit 0
