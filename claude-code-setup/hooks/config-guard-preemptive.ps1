# config-guard-preemptive.ps1 — Poka-Yoke Stufe 3: Blockiert Config-Aenderungen BEVOR sie wirksam werden
# Event: ConfigChange (blockierbar mit exit 2)
# Zweck: Sofortmassnahme 1 — Upgrade von reaktiv (PostToolUse) zu praeventiv (ConfigChange)
#
# UNTERSCHIED zum bestehenden config-guard.ps1:
#   config-guard.ps1       = PostToolUse (NACH der Aenderung) = Poka-Yoke Stufe 2 (Erzwingung)
#   config-guard-preemptive.ps1 = ConfigChange (VOR der Aenderung) = Poka-Yoke Stufe 3 (Eliminierung)
#
# Beide laufen parallel — Defense in Depth:
#   Schicht 1 (praeventiv): Dieser Hook blockiert die Aenderung bevor sie gespeichert wird
#   Schicht 2 (reaktiv): config-guard.ps1 repariert falls Schicht 1 versagt hat
#
# WICHTIG: Standalone-Hook (kein dot-source). MUSS mit exit 0 enden (ausser bei Block → exit 2).

$ErrorActionPreference = "SilentlyContinue"

try {

# --- stdin lesen ---
$inputJson = ""
try {
    $inputJson = $input | Out-String
} catch {
    exit 0
}

if ([string]::IsNullOrWhiteSpace($inputJson)) { exit 0 }

$eventData = $null
try {
    $eventData = $inputJson | ConvertFrom-Json
} catch {
    exit 0
}

# --- Geschuetzte Einstellungen pruefen ---
# ConfigChange-Event liefert die GEPLANTE Aenderung — wir pruefen sie VOR dem Speichern

$violations = @()

# Methode 1: Direkt die settings.json lesen (nach der geplanten Aenderung)
# ConfigChange feuert BEVOR die Aenderung persistent wird, aber die Datei koennte
# bereits im Speicher geaendert sein. Sicherheitshalber beide Pfade pruefen.

$settingsFiles = @(
    (Join-Path $env:USERPROFILE ".claude" "settings.json"),
    (Join-Path $env:USERPROFILE ".claude" "settings.local.json")
)

foreach ($settingsPath in $settingsFiles) {
    if (-not (Test-Path $settingsPath)) { continue }

    try {
        $content = Get-Content $settingsPath -Raw -Encoding UTF8
        $data = $content | ConvertFrom-Json

        # KRITISCH: bypassPermissions darf NIEMALS geaendert werden
        if ($data.permissions) {
            $mode = $data.permissions.defaultMode
            if ($null -ne $mode -and $mode -ne "" -and $mode -ne "bypassPermissions") {
                $violations += "defaultMode='$mode' in $(Split-Path $settingsPath -Leaf) (MUSS 'bypassPermissions' sein)"
            }

            # allow-Liste darf nicht existieren (wirkt als Whitelist-Blocker)
            if ($null -ne $data.permissions.allow) {
                $violations += "allow-Liste in $(Split-Path $settingsPath -Leaf) erkannt (blockiert bypassPermissions)"
            }
        }

        # AUTOCOMPACT darf nicht unter 95 fallen
        if ($data.env) {
            $acp = $data.env.CLAUDE_AUTOCOMPACT_PCT_OVERRIDE
            if ($null -ne $acp -and $acp -ne "" -and [int]$acp -lt 95) {
                $violations += "AUTOCOMPACT=$acp (Minimum: 95)"
            }

            # SUBAGENT_MODEL muss sonnet bleiben
            $subModel = $data.env.CLAUDE_CODE_SUBAGENT_MODEL
            if ($null -ne $subModel -and $subModel -ne "" -and $subModel -ne "sonnet") {
                $violations += "SUBAGENT_MODEL='$subModel' (erwartet: sonnet)"
            }
        }

    } catch {
        # Datei konnte nicht gelesen werden — nicht blockieren
    }
}

# --- Entscheidung: Blockieren oder durchlassen ---
if ($violations.Count -gt 0) {
    $msg = $violations -join "; "

    # Logging
    $logDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    $logFile = Join-Path $logDir "config-guard-preemptive-$(Get-Date -Format 'yyyy-MM-dd').log"
    Add-Content -Path $logFile -Value "[$(Get-Date -Format 'HH:mm:ss')] BLOCKED: $msg" -Encoding UTF8

    # Ausgabe an Claude Code
    $output = @{
        error = "CONFIG-GUARD PRAEVENTIV: BLOCKIERT — $msg. Diese Aenderung wurde verhindert BEVOR sie wirksam wurde."
    } | ConvertTo-Json -Compress
    Write-Output $output

    exit 2
}

} catch {
    # Failsafe: Bei internem Fehler NICHT blockieren
    try {
        $logDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
        if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
        $logFile = Join-Path $logDir "config-guard-preemptive-$(Get-Date -Format 'yyyy-MM-dd').log"
        Add-Content -Path $logFile -Value "[$(Get-Date -Format 'HH:mm:ss')] FATAL: $($_.Exception.Message)" -Encoding UTF8
    } catch { }
}

# PFLICHT: Standalone-Hook muss mit exit 0 enden
exit 0
