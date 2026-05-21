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
# primaerer Hook. Wir beenden hier still um doppelte Block-Meldungen zu vermeiden.
# Nur wenn bash NICHT verfuegbar (native Windows ohne Git Bash) ist der .ps1-Hook
# der einzige aktive Guard. Hardening 2026-05-21 (Direktive #3).
try {
    $null = Get-Command bash -ErrorAction Stop
    exit 0
} catch {
    # bash nicht verfuegbar — wir sind der einzige Guard, weiter
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

    # Stale-Lock-Check (Wave 3 Hardening 2026-05-21 — Direktive #3 Loop 2 K5):
    # Lock-Datei enthaelt PID des Orchestrators (Format: "orchestratorPid: <number>").
    # Wenn der Prozess nicht mehr laeuft UND der Lock > 5 Min alt: stale -> Lock ignorieren
    # und Schreiben durchlassen mit klarer Warnung. Verhindert Stunden-langes Blockieren
    # nach Orchestrator-Crash (vorher: Lock blockierte bis 24h Stale-Detection griff).
    try {
        $lockContent = Get-Content $lockFound -Raw -ErrorAction Stop
        if ($lockContent -match 'orchestratorPid:\s*(\d+)') {
            $lockPid = [int]$matches[1]
            $lockAge = (Get-Date) - (Get-Item $lockFound -ErrorAction Stop).LastWriteTime

            $procAlive = $false
            try {
                $null = Get-Process -Id $lockPid -ErrorAction Stop
                $procAlive = $true
            } catch {
                $procAlive = $false
            }

            if (-not $procAlive -and $lockAge.TotalMinutes -gt 5) {
                [Console]::Error.WriteLine("[finale] WARNUNG: Stale-Lock erkannt (PID $lockPid nicht mehr aktiv, $([math]::Round($lockAge.TotalMinutes,1)) Min alt).")
                [Console]::Error.WriteLine("[finale] Lock wird ignoriert (Stale). Manuell loeschen: Remove-Item '$lockFound'")
                exit 0
            }
        }
    } catch {
        # Lock-Inhalt unlesbar — defensiv blockieren (besser als false-positive durchlassen)
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
