# bug-case-auto-writer.ps1 — Automatischer Bug-Cases-Writer + Pattern-Matcher
# Event: PostToolUseFailure
# Zweck: Luecke 2 (automatisches Schreiben) + Luecke 6 (ausfuehrbares System)
#
# Was dieses Skript tut:
# 1. Liest die Fehlermeldung aus dem PostToolUseFailure-Event
# 2. Prueft ob ein aehnlicher Fall in bug-cases.jsonl existiert (Pattern-Match)
# 3. Wenn JA: Gibt den alten Fix als Kontext an Claude zurueck (RAG-Effekt)
# 4. Wenn NEIN: Schreibt einen neuen Roh-Eintrag in bug-cases.jsonl
#
# Poka-Yoke-Stufe: 3 (Eliminierung) — Vergessen ist strukturell unmoeglich
# Defense in Depth: Schicht 1 (praeventiv: Pattern-Match) + Schicht 2 (reaktiv: Auto-Write)
#
# WICHTIG: Dieses Skript ist KEIN dot-sourced Library. Es ist ein Standalone-Hook.
#          exit 0 am Ende ist PFLICHT.

# --- Fehlerresilienz: Niemals die Session blockieren ---
$ErrorActionPreference = "SilentlyContinue"
try {

# --- Pfade ---
$bugCasesPath = Join-Path $env:USERPROFILE "proggs" ".claude" "agent-memory" "shared" "bug-cases.jsonl"
$logDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
$logFile = Join-Path $logDir "bug-case-auto-writer-$(Get-Date -Format 'yyyy-MM-dd').log"

# Sicherstellen dass Log-Verzeichnis existiert
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

function Write-Log($msg) {
    $ts = Get-Date -Format "HH:mm:ss"
    Add-Content -Path $logFile -Value "[$ts] $msg" -Encoding UTF8 -ErrorAction SilentlyContinue
}

# --- stdin lesen (JSON vom Hook-Event) ---
$inputJson = ""
try {
    $inputJson = $input | Out-String
} catch {
    Write-Log "WARN: stdin konnte nicht gelesen werden"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($inputJson)) {
    exit 0
}

$eventData = $null
try {
    $eventData = $inputJson | ConvertFrom-Json
} catch {
    Write-Log "WARN: stdin ist kein gueltiges JSON"
    exit 0
}

# --- Fehlerdaten extrahieren ---
$toolName = ""
$errorText = ""
$commandText = ""

# PostToolUseFailure liefert tool_name, tool_input und error
if ($eventData.tool_name) { $toolName = $eventData.tool_name }
if ($eventData.error) { $errorText = $eventData.error }
if ($eventData.tool_input) {
    if ($eventData.tool_input.command) { $commandText = $eventData.tool_input.command }
    elseif ($eventData.tool_input.file_path) { $commandText = $eventData.tool_input.file_path }
}

# Triviale Fehler ignorieren (unter 20 Zeichen, meist Timeouts oder leere Fehler)
if ($errorText.Length -lt 20) {
    Write-Log "SKIP: Fehlermeldung zu kurz ($($errorText.Length) Zeichen)"
    exit 0
}

# --- LUECKE 6: Pattern-Matching gegen bestehende Bug-Cases ---
$matchedCase = $null
$matchedScore = 0

if (Test-Path $bugCasesPath) {
    $existingCases = Get-Content $bugCasesPath -Encoding UTF8 -ErrorAction SilentlyContinue

    foreach ($line in $existingCases) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }

        $case = $null
        try { $case = $line | ConvertFrom-Json } catch { continue }

        if (-not $case.symptom) { continue }

        # Symptom-Pattern-Match: Pruefe ob die Fehlermeldung das Symptom enthaelt
        $symptomWords = $case.symptom -split '\s+' | Where-Object { $_.Length -gt 3 }
        $matchCount = 0
        foreach ($word in $symptomWords) {
            if ($errorText -match [regex]::Escape($word)) {
                $matchCount++
            }
        }

        # Score: Anteil der Symptom-Woerter die im Fehler vorkommen
        if ($symptomWords.Count -gt 0) {
            $score = [math]::Round($matchCount / $symptomWords.Count, 2)
            if ($score -gt $matchedScore) {
                $matchedScore = $score
                $matchedCase = $case
            }
        }

        # Zusaetzlich: Exaktes Substring-Match auf symptom_pattern (wenn vorhanden)
        if ($case.symptom_pattern) {
            try {
                if ($errorText -match $case.symptom_pattern) {
                    $matchedScore = 1.0
                    $matchedCase = $case
                    break  # Exakter Match gefunden, nicht weiter suchen
                }
            } catch {
                # Ungueltige Regex ignorieren
            }
        }
    }
}

# --- Entscheidung: Match gefunden oder neuer Fall? ---
$outputJson = $null

if ($matchedCase -and $matchedScore -ge 0.5) {
    # LUECKE 6: Bekannten Fix als Kontext zurueckgeben (RAG-Effekt)
    $fixAdvice = "BEKANNTER FEHLER (Aehnlichkeit: $([math]::Round($matchedScore * 100))%)`n"
    $fixAdvice += "Symptom: $($matchedCase.symptom)`n"
    $fixAdvice += "Root Cause: $($matchedCase.root_cause)`n"
    $fixAdvice += "Fix: $($matchedCase.fix)`n"
    if ($matchedCase.prevention) {
        $fixAdvice += "Praevention: $($matchedCase.prevention)`n"
    }
    if ($matchedCase.prevention_rule) {
        $fixAdvice += "Praevention: $($matchedCase.prevention_rule)`n"
    }
    $fixAdvice += "WENDE DIESEN FIX ZUERST AN bevor du eine neue Hypothese versuchst."

    # Ausgabe als additionalContext fuer Claude
    $output = @{
        hookSpecificOutput = @{
            hookEventName = "PostToolUseFailure"
            additionalContext = $fixAdvice
        }
    }
    $outputJson = $output | ConvertTo-Json -Depth 5 -Compress

    Write-Log "MATCH: Score=$matchedScore, Symptom='$($matchedCase.symptom)', Tool=$toolName"

} else {
    # LUECKE 2: Neuen Roh-Eintrag automatisch in bug-cases.jsonl schreiben

    # Duplikat-Check: Gleicher Fehler in den letzten 24h schon geschrieben?
    $isDuplicate = $false
    $errorHash = ($errorText.Substring(0, [math]::Min(100, $errorText.Length))).GetHashCode()

    if (Test-Path $bugCasesPath) {
        $recentLines = Get-Content $bugCasesPath -Tail 10 -Encoding UTF8 -ErrorAction SilentlyContinue
        foreach ($line in $recentLines) {
            if ($line -match [regex]::Escape($errorHash.ToString())) {
                $isDuplicate = $true
                break
            }
            # Auch auf aehnlichen Fehlertext pruefen (erste 80 Zeichen)
            $shortError = $errorText.Substring(0, [math]::Min(80, $errorText.Length))
            if ($line -match [regex]::Escape($shortError)) {
                $isDuplicate = $true
                break
            }
        }
    }

    if (-not $isDuplicate) {
        # Neuen Eintrag erstellen
        $newCase = @{
            date = Get-Date -Format "yyyy-MM-dd"
            symptom = $errorText.Substring(0, [math]::Min(200, $errorText.Length))
            root_cause = "AUTO-ERFASST — Root Cause noch nicht analysiert"
            fix = "AUTO-ERFASST — Fix noch nicht dokumentiert"
            files = @($commandText)
            tags = @($toolName.ToLower(), "auto-captured")
            severity = "unbekannt"
            error_hash = $errorHash.ToString()
            auto_captured = $true
        }

        # Atomar schreiben (Temp-File + Rename-Append)
        $tempFile = Join-Path (Split-Path $bugCasesPath) "bug-cases-temp-$([guid]::NewGuid().ToString('N').Substring(0,8)).jsonl"
        $jsonLine = $newCase | ConvertTo-Json -Compress

        try {
            # An Original appenden (atomar genug fuer JSONL)
            Add-Content -Path $bugCasesPath -Value $jsonLine -Encoding UTF8 -NoNewline
            Add-Content -Path $bugCasesPath -Value "`n" -Encoding UTF8 -NoNewline
            Write-Log "WRITE: Neuer Bug-Case auto-erfasst, Tool=$toolName, Hash=$errorHash"
        } catch {
            Write-Log "ERROR: Konnte nicht in bug-cases.jsonl schreiben: $_"
        }

        # Claude informieren dass ein neuer Fall erfasst wurde
        $infoText = "Bug-Case automatisch erfasst (Tool: $toolName). "
        $infoText += "Nach dem Fix: Bitte Root Cause und Fix in bug-cases.jsonl nachtragen (Feld 'auto_captured: true' suchen)."

        $output = @{
            hookSpecificOutput = @{
                hookEventName = "PostToolUseFailure"
                additionalContext = $infoText
            }
        }
        $outputJson = $output | ConvertTo-Json -Depth 5 -Compress
    } else {
        Write-Log "SKIP: Duplikat erkannt (Hash=$errorHash)"
    }
}

# --- Ausgabe an Claude Code (stdout) ---
if ($outputJson) {
    Write-Output $outputJson
}

} catch {
    # Failsafe: Hook darf NIEMALS die Session blockieren
    # Fehler loggen aber exit 0
    try {
        $errMsg = "FATAL: $($_.Exception.Message)"
        $logDir2 = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
        $logFile2 = Join-Path $logDir2 "bug-case-auto-writer-$(Get-Date -Format 'yyyy-MM-dd').log"
        if (-not (Test-Path $logDir2)) { New-Item -ItemType Directory -Path $logDir2 -Force | Out-Null }
        Add-Content -Path $logFile2 -Value "[$(Get-Date -Format 'HH:mm:ss')] $errMsg" -Encoding UTF8
    } catch {
        # Selbst das Logging ist fehlgeschlagen — still weitermachen
    }
}

# PFLICHT: Standalone-Hook muss mit exit 0 enden
exit 0
