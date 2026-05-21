# audit-only-write-guard.ps1
#
# PowerShell-Pendant zu audit-only-write-guard.sh.
# PreToolUse-Hook fuer Edit|Write|MultiEdit. Blockiert Schreibversuche an
# App-Dateien wenn ein Audit-Only-Lauf aktiv ist. Aktiv = Lock-Datei
# `<app-root>/.android-shield/.audit-only.lock` existiert in einem
# Vorfahren-Verzeichnis des betroffenen Pfads.
#
# Verhalten identisch zur .sh-Variante. Wird auf Windows-Systemen ohne
# Git-Bash benoetigt damit der Schutz aktiv bleibt.

$ErrorActionPreference = "SilentlyContinue"

try {
    # Input-Guard: leerer stdin -> still durchwinken
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) {
        exit 0
    }

    # JSON parsen
    $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
    $filePath = $null
    if ($parsed.tool_input) {
        if ($parsed.tool_input.file_path) {
            $filePath = $parsed.tool_input.file_path
        } elseif ($parsed.tool_input.path) {
            $filePath = $parsed.tool_input.path
        }
    }
    if ([string]::IsNullOrWhiteSpace($filePath)) {
        exit 0
    }

    # Schreiben innerhalb .android-shield/ ist immer erlaubt (Plugin-Output-Domain)
    if ($filePath -match '[/\\]\.android-shield[/\\]') {
        exit 0
    }

    # Suche aufwaerts nach Audit-Lock
    $dir = Split-Path -Parent $filePath
    $lockFound = $null
    $depth = 0
    $maxDepth = 20

    while ($dir -and ($dir -ne (Split-Path -Parent $dir)) -and ($depth -lt $maxDepth)) {
        $lockPath = Join-Path $dir ".android-shield/.audit-only.lock"
        if (Test-Path $lockPath -PathType Leaf) {
            $lockFound = $lockPath
            break
        }
        $dir = Split-Path -Parent $dir
        $depth++
    }

    if (-not $lockFound) {
        exit 0
    }

    # Lock aktiv UND Datei nicht in .android-shield/ -> blockieren
    [Console]::Error.WriteLine("[finale] BLOCKIERT: Audit-Only-Modus aktiv (Lock: $lockFound)")
    [Console]::Error.WriteLine("[finale] Datei: $filePath")
    [Console]::Error.WriteLine("[finale] Im Audit-Only-Modus duerfen nur Dateien unter .android-shield/ beschrieben werden.")
    [Console]::Error.WriteLine("[finale] Fuer Fixes: /finale:fix-only oder /finale:run starten — dort wird der Lock nicht gesetzt.")
    exit 2
}
catch {
    # Bei Fehler im Hook still durchlassen — Session brechen ist schlimmer als Schutz aus
    exit 0
}
