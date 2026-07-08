# session-start.ps1
#
# PowerShell-Pendant zu session-start.sh.
# SessionStart-Hook des finale-Plugins. Prueft NICHT-BLOCKIEREND ob die vier
# Skill-Symlinks aufloesbar sind und gibt bei Defekt konkrete Reparatur-Befehle
# nach stderr aus (Post-Install-Wizard).
#
# Wird auf Windows-Systemen ohne Git-Bash benoetigt damit der Schutz aktiv bleibt.
# In hooks.json optional aktivieren — siehe README "Cross-Platform-Hooks".

# Idempotency-Schutz: wenn Git Bash verfuegbar ist, laeuft die .sh-Variante als
# primaerer Hook. Override: FINALE_FORCE_PS1=1 fuer Testing (Wave 8 2026-05-21).
if (-not $env:FINALE_FORCE_PS1) {
    try {
        $null = Get-Command bash -ErrorAction Stop
        exit 0   # bash verfuegbar — .sh-Hook ist der primaere
    } catch {
        # bash nicht verfuegbar — wir sind der einzige Hook, weiter
    }
}

# Stop statt SilentlyContinue: Fehler vom try/catch gefangen.
$ErrorActionPreference = "Stop"

try {
    $pluginRoot = $env:CLAUDE_PLUGIN_ROOT
    if ([string]::IsNullOrWhiteSpace($pluginRoot) -or -not (Test-Path $pluginRoot -PathType Container)) {
        exit 0
    }

    $verify = Join-Path $pluginRoot "scripts/verify-skills.sh"
    if (-not (Test-Path $verify -PathType Leaf)) {
        [Console]::Error.WriteLine("[finale] Hinweis: verify-skills.sh fehlt — Plugin koennte beschaedigt sein.")
        exit 0
    }

    # Temp-Dateien fuer JSON und Stderr
    $tmpDir = if ($env:TEMP) { $env:TEMP } else { "/tmp" }
    $pid_str = [System.Diagnostics.Process]::GetCurrentProcess().Id
    $jsonFile = Join-Path $tmpDir ".finale-verify-$pid_str.json"
    $errFile  = Join-Path $tmpDir ".finale-verify-$pid_str.err"

    try {
        # Bash-Aufruf — auf Windows ohne Git Bash schlaegt das fehl und der catch greift.
        # KEIN $pluginRoot als zweites Argument: verify-skills.sh leitet PLUGIN_ROOT
        # selbst aus SCRIPT_DIR/.. ab (FIN-001). $1 von verify-skills.sh ist optionaler
        # App-Root, den SessionStart nicht kennt.
        $proc = Start-Process -FilePath "bash" `
            -ArgumentList @($verify) `
            -RedirectStandardOutput $jsonFile `
            -RedirectStandardError $errFile `
            -NoNewWindow -Wait -PassThru -ErrorAction Stop

        if ($proc.ExitCode -eq 0) {
            [Console]::Error.WriteLine("[finale] Alle vier Skill-Symlinks OK.")
            exit 0
        }

        # Verifikation fehlgeschlagen — Post-Install-Wizard
        [Console]::Error.WriteLine("[finale] WARNUNG: mindestens ein Skill-Symlink ist defekt.")

        if (Test-Path $jsonFile -PathType Leaf) {
            try {
                $data = Get-Content $jsonFile -Raw -Encoding UTF8 | ConvertFrom-Json
                $broken = @($data.skills | Where-Object { $_.status -eq 'error' })

                if ($broken.Count -gt 0) {
                    [Console]::Error.WriteLine("[finale] Reparatur-Befehle (direkt copy-pasten):")
                    [Console]::Error.WriteLine("[finale]")
                    [Console]::Error.WriteLine("[finale]   # Auf Windows ohne Git-Bash: PowerShell-Symlinks brauchen Admin oder Developer Mode")
                    [Console]::Error.WriteLine("[finale]   # Empfehlung: Developer Mode aktivieren (Settings -> Privacy -> For Developers)")
                    [Console]::Error.WriteLine("[finale]")
                    foreach ($s in $broken) {
                        [Console]::Error.WriteLine("[finale]   # $($s.name): $($s.problem)")
                        [Console]::Error.WriteLine("[finale]   New-Item -ItemType SymbolicLink -Path `"$($s.linkPath)`" -Target `"$($s.expectedTarget)`" -Force")
                    }
                    [Console]::Error.WriteLine("[finale]")
                    [Console]::Error.WriteLine("[finale] Voraussetzung: die echten Skills muessen unter ~/.claude/skills/ liegen.")
                }
            } catch {
                # JSON-Parser-Fehler — Fallback auf Stderr
                if (Test-Path $errFile -PathType Leaf) {
                    [Console]::Error.WriteLine("[finale] Details aus verify-skills.sh:")
                    Get-Content $errFile -Encoding UTF8 -TotalCount 10 | ForEach-Object {
                        [Console]::Error.WriteLine("[finale] $_")
                    }
                }
            }
        }
    }
    finally {
        Remove-Item $jsonFile, $errFile -ErrorAction SilentlyContinue
    }
}
catch {
    # Bei jedem Fehler (z. B. bash nicht verfuegbar) still durchwinken
    exit 0
}

exit 0
