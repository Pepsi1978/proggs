# experience-logger — SessionEnd Hook (Windows Wrapper)
# Reicht den SessionEnd-stdin (transcript_path) an experience-logger.py weiter,
# das die ECHTEN Session-Signale aus dem Transcript zieht (gemeinsame Cross-Platform-Logik).
# Ersetzt die alte parasitaere Logik (las nur die letzte session-scores-Zeile -> Platzhalter,
# Feldnamen-Mismatch, datentot seit 2026-04-12).
# stdin dual-read (Almanach claude-hooks 12.4: Stop/SessionEnd-Hooks bekommen auf Windows/pwsh
# gelegentlich kein stdin). Faellt stdin aus, findet experience-logger.py das neueste Transcript selbst.
# Direktive #3: Graceful Degradation — IMMER exit 0, nie die Session blockieren.
try {
    $stdin = ""
    try { $stdin = [Console]::In.ReadToEnd() } catch { }
    if ([string]::IsNullOrEmpty($stdin)) {
        try { $stdin = ($input | Out-String) } catch { }
    }
    $py = Join-Path $PSScriptRoot "experience-logger.py"
    if (Test-Path $py) {
        $stdin | python $py 2>$null
    }
} catch { }
exit 0
