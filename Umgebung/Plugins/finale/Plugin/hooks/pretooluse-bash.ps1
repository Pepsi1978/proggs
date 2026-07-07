# pretooluse-bash.ps1
#
# PowerShell-Pendant zu pretooluse-bash.sh.
# Blockiert destruktive Bash-Commands die geschuetzte Dateien gefaehrden wuerden.
# Wird auf Windows-Systemen ohne Git-Bash benoetigt damit der Schutz aktiv bleibt.
# Pattern-Liste muss IDENTISCH zur .sh-Variante bleiben.

# Idempotency-Schutz: wenn Git Bash verfuegbar ist, laeuft die .sh-Variante.
# Dann beenden wir hier still, um doppelte Block-Meldungen zu vermeiden.
# Nur wenn bash NICHT verfuegbar ist der .ps1-Hook der einzige aktive Guard.
# Override fuer Testing: FINALE_FORCE_PS1=1 erzwingt Ausfuehrung der PS-Logik
# auch wenn bash verfuegbar ist (Wave 8 Test-Hook 2026-05-21).
if (-not $env:FINALE_FORCE_PS1) {
    try {
        $null = Get-Command bash -ErrorAction Stop
        exit 0
    } catch {
        # bash nicht verfuegbar — wir sind der einzige Guard, weiter
    }
}

# Stop statt SilentlyContinue: Fehler werden vom try/catch gefangen und enden
# in exit 0. Mit SilentlyContinue wuerde Code bei Exception still weiterlaufen
# und der Guard wirkungslos werden (z.B. malformiertes JSON → $cmd bleibt null
# → Bash-Befehl durchgewunken).
$ErrorActionPreference = "Stop"

try {
    # Input-Guard
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) { exit 0 }
    # DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin
    if ($stdin.Length -gt 524288) { exit 0 }

    # JSON parsen
    $parsed = $stdin | ConvertFrom-Json
    $cmd = $null
    if ($parsed.tool_input -and $parsed.tool_input.command) {
        $cmd = $parsed.tool_input.command
    }
    if ([string]::IsNullOrWhiteSpace($cmd)) { exit 0 }

    # Geschuetzte Dateien — IDENTISCH zur .sh-Variante.
    $protected = '(strings\.xml|AndroidManifest\.xml|build\.gradle\.kts|build\.gradle)'
    # Wave 8 Fix (2026-05-21): PATH_PREFIX + RBOUND statt \b — vorher
    # blockierte das Pattern `> strings.xml.bak` und `> xstrings.xml` faelschlich.
    $pathPrefix = '([^\s|&;]*/)?'
    $rbound = '(\s|;|\||&|$)'

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

    # Pattern 2 — Shell-Umleitung in geschuetzte Datei (Wave 8 mit strikten Boundaries)
    if (-not $blockReason) {
        if ($cmd -match ">>?\s+${pathPrefix}${protected}${rbound}") {
            $blockReason = "Shell-Umleitung in eine geschuetzte Datei — destructiver Komplett-Overwrite. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 3 — tee (mit oder ohne -a) in geschuetzte Datei
    if (-not $blockReason) {
        if ($cmd -match "tee\s+(-a\s+)?${pathPrefix}${protected}${rbound}") {
            $blockReason = "tee in eine geschuetzte Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 4 — cp / mv mit geschuetzter Ziel-Datei
    if (-not $blockReason) {
        if ($cmd -match "(^|[\s;|&])(cp|mv)\s+[^|&;]+\s${pathPrefix}${protected}${rbound}") {
            $blockReason = "cp/mv mit geschuetzter Ziel-Datei — destructiver Overwrite. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 5 — dd of= mit geschuetzter Datei
    if (-not $blockReason) {
        if ($cmd -match "dd\s.*of=${pathPrefix}${protected}${rbound}") {
            $blockReason = "dd of= mit geschuetzter Ziel-Datei — destructiv. Nutze Edit/Write ueber den fix-applier."
        }
    }

    # Pattern 6 — find ... -delete im Plugin-Output
    if (-not $blockReason) {
        if ($cmd -match 'find\s+.*\.android-shield.*-delete') {
            $blockReason = "find -delete auf .android-shield/ — destructiv. Audit-Log gehoert append-only."
        }
    }

    # Pattern 7 — Scripting-Sprache mit Code-Argument UND geschuetztem Dateinamen.
    # Hardening C1 2026-05-21 (Direktive #3) — schliesst Bypass via Python/Node/Perl etc.
    # Pattern muss IDENTISCH zur .sh-Variante sein.
    if (-not $blockReason) {
        if (($cmd -match '(^|[\s;|&])(python3?|node|nodejs|perl|ruby|sh|pwsh|bash)\s+(-[cei]|--eval|--exec|--command)') -and
            ($cmd -match $protected)) {
            $blockReason = "Scripting-Sprache (python/node/perl/etc.) mit Code-Argument das eine geschuetzte Datei referenziert. Auch indirekte Schreibversuche via Scripting werden blockiert. Nutze Edit/Write ueber den fix-applier."
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
