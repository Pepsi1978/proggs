# pretooluse-bash.ps1
#
# PowerShell-Pendant zu pretooluse-bash.sh.
# Blockiert destruktive Bash-Commands die geschuetzte Dateien gefaehrden wuerden.
# Wird auf Windows-Systemen ohne Git-Bash benoetigt damit der Schutz aktiv bleibt.
# Pattern-Liste muss IDENTISCH zur .sh-Variante bleiben.

$ErrorActionPreference = "SilentlyContinue"

try {
    # Input-Guard
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) { exit 0 }

    # JSON parsen
    $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
    $cmd = $null
    if ($parsed.tool_input -and $parsed.tool_input.command) {
        $cmd = $parsed.tool_input.command
    }
    if ([string]::IsNullOrWhiteSpace($cmd)) { exit 0 }

    # Geschuetzte Dateien — IDENTISCH zur .sh-Variante
    $protected = '(strings\.xml|AndroidManifest\.xml|build\.gradle\.kts|build\.gradle)'

    $blockReason = $null

    # Pattern 1 — rm -rf auf .android-shield/ oder res/
    if ($cmd -match 'rm\s+-r[fF]\s+.*\.android-shield(/|\b)') {
        $blockReason = "rm -rf auf .android-shield/ — das wuerde alle Plugin-Reports und das audit-log loeschen"
    }
    elseif ($cmd -match 'rm\s+-r[fF]\s+.*\bres/values') {
        $blockReason = "rm -rf auf res/values — das wuerde Lokalisierungs-Dateien zerstoeren"
    }
    elseif ($cmd -match 'rm\s+-r[fF]\s+.*\bres(/|\b)') {
        $blockReason = "rm -rf auf res/ — das wuerde alle App-Ressourcen zerstoeren"
    }

    # Pattern 2 — Shell-Umleitung in geschuetzte Datei
    if (-not $blockReason) {
        if ($cmd -match ">>?\s*[^|&]*$protected(\b|`$)") {
            $blockReason = "Shell-Umleitung in eine geschuetzte Datei — destructiver Komplett-Overwrite. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 3 — tee (mit oder ohne -a) in geschuetzte Datei
    if (-not $blockReason) {
        if ($cmd -match "tee\s+(-a\s+)?[^|&]*$protected(\b|`$)") {
            $blockReason = "tee in eine geschuetzte Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 4 — cp / mv mit geschuetzter Ziel-Datei
    if (-not $blockReason) {
        if ($cmd -match "(^|[\s;|&])(cp|mv)\s+[^|&;]+\s[^|&;\s]*$protected([\s;|&]|`$)") {
            $blockReason = "cp/mv mit geschuetzter Ziel-Datei — destructiver Overwrite. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 5 — dd of= mit geschuetzter Datei
    if (-not $blockReason) {
        if ($cmd -match "dd\s.*of=[^\s]*$protected(\b|`$)") {
            $blockReason = "dd of= mit geschuetzter Ziel-Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 6 — find ... -delete im Plugin-Output
    if (-not $blockReason) {
        if ($cmd -match 'find\s+.*\.android-shield.*-delete') {
            $blockReason = "find -delete auf .android-shield/ — destructiv. Audit-Log gehoert append-only."
        }
    }

    if ($blockReason) {
        [Console]::Error.WriteLine("[finale] BLOCKIERT: $blockReason")
        [Console]::Error.WriteLine("[finale] Befehl: $cmd")
        [Console]::Error.WriteLine("[finale] Wenn du das wirklich willst, fuehre den Befehl ausserhalb des Plugin-Scopes aus.")
        exit 2
    }
}
catch {
    exit 0
}

exit 0
