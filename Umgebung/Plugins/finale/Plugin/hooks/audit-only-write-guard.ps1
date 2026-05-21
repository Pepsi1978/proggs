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

# Idempotency-Schutz: wenn Git Bash verfuegbar ist, laeuft die .sh-Variante als
# primaerer Hook. Override: FINALE_FORCE_PS1=1 fuer Testing (Wave 8 2026-05-21).
if (-not $env:FINALE_FORCE_PS1) {
    try {
        $null = Get-Command bash -ErrorAction Stop
        exit 0
    } catch {
        # bash nicht verfuegbar — wir sind der einzige Guard, weiter
    }
}

# Stop statt SilentlyContinue: Fehler in der Hook-Logik werden vom try/catch
# unten gefangen und enden in exit 0.
$ErrorActionPreference = "Stop"

try {
    # Input-Guard: leerer stdin -> still durchwinken
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) {
        exit 0
    }
    # DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin
    if ($stdin.Length -gt 524288) { exit 0 }

    # JSON parsen
    $parsed = $stdin | ConvertFrom-Json
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

    # Path-Traversal-Schutz (C3 2026-05-21 Hardening): bei ../ im Pfad
    # kanonisieren BEVOR der Whitelist-Check. Verhindert Bypass wie
    # C:\app\..\.android-shield\..\res\values\strings.xml.
    $realPath = $filePath
    if ($filePath -match '\.\.[/\\]') {
        try {
            $realPath = [System.IO.Path]::GetFullPath($filePath)
        } catch {
            # Bei Fehler beim Kanonisieren: Original verwenden (Whitelist greift dann nicht — sicher).
            $realPath = $filePath
        }
    }

    # Schreiben innerhalb .android-shield/ ist immer erlaubt (Plugin-Output-Domain).
    # Match auf KANONISIERTEM Pfad — sonst Traversal-Bypass.
    if ($realPath -match '[/\\]\.android-shield[/\\]') {
        exit 0
    }

    # Suche aufwaerts nach Audit-Lock (auf kanonisiertem Pfad)
    $dir = Split-Path -Parent $realPath
    $lockFound = $null
    $depth = 0
    $maxDepth = 20

    while ($dir -and ($dir -ne (Split-Path -Parent $dir)) -and ($depth -lt $maxDepth)) {
        # Nested Join-Path mit nur einem Separator pro Aufruf — robuster bei
        # strikten Pfad-Operationen (Test-Path -LiteralPath) als
        # Forward-Slash in einem Argument-String.
        $lockPath = Join-Path (Join-Path $dir ".android-shield") ".audit-only.lock"
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

    # Stale-Lock-Check (Wave 6 Hardening 2026-05-21 — Lock-Format-Mismatch behoben):
    # Wave 5 stellte das Lock-Format auf sessionToken um (LLM-Agenten haben keine
    # stabile OS-PID), aber dieser Hook las weiter orchestratorPid -> Stale-Check
    # war tot -> Locks blieben nach Crash fuer immer aktiv.
    # Wave 6: nur Timestamp-basierter Stale-Check. Plus: Math.Max(0, age) gegen
    # negative TotalMinutes durch NTP-Korrektur/VM-Resume.
    try {
        $lockAge = (Get-Date) - (Get-Item $lockFound -ErrorAction Stop).LastWriteTime
        # Negative lock_age (NTP-Korrektur) auf 0 clippen
        $ageMinutes = [math]::Max(0, $lockAge.TotalMinutes)
        # Stale-Schwelle 30 Min
        if ($ageMinutes -gt 30) {
            [Console]::Error.WriteLine("[finale] WARNUNG: Stale-Lock erkannt ($([math]::Round($ageMinutes,1)) Min alt, ueber 30-Min-Schwelle).")
            [Console]::Error.WriteLine("[finale] Vorheriger Orchestrator-Lauf wahrscheinlich gecrasht.")
            [Console]::Error.WriteLine("[finale] Lock wird ignoriert. Manuell loeschen: Remove-Item '$lockFound'")
            exit 0
        }
    } catch {
        # Lock-Datei nicht lesbar/zugaenglich — defensiv blockieren
        # (besser false-positive Block als false-positive Durchlassen)
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
