# posttooluse-strings-xml.ps1
#
# PowerShell-Pendant zu posttooluse-strings-xml.sh.
# Nach jedem Edit/Write auf eine strings.xml: leichter XML-Lint der die haeufigsten
# Build-Killer-Fehler in Android-Lokalisierungs-Dateien detektiert. Schreibt
# Warnungen nach stderr, blockiert NICHT (Aenderung ist schon passiert).
#
# Geprueft (identisch zur .sh-Variante):
# 1. Nackte Apostrophe in <string>-Werten
# 2. Nackte doppelte Anfuehrungszeichen in <string>-Werten
# 3. Unmatched &-Symbole (& muss &amp; sein)

# Idempotency-Schutz: wenn Git Bash verfuegbar ist, laeuft die .sh-Variante.
# Override: FINALE_FORCE_PS1=1 fuer Testing (Wave 8 2026-05-21).
if (-not $env:FINALE_FORCE_PS1) {
    try {
        $null = Get-Command bash -ErrorAction Stop
        exit 0
    } catch {
        # bash nicht verfuegbar — weiter
    }
}

# Stop statt SilentlyContinue: Fehler werden vom try/catch gefangen.
# Lint ist non-blocking — Exit 0 in jedem Fall, auch wenn der Lint selbst crasht.
$ErrorActionPreference = "Stop"

try {
    $stdin = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($stdin)) { exit 0 }
    # DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin
    if ($stdin.Length -gt 524288) { exit 0 }

    $parsed = $stdin | ConvertFrom-Json
    $filePath = $null
    if ($parsed.tool_input) {
        if ($parsed.tool_input.file_path) {
            $filePath = $parsed.tool_input.file_path
        } elseif ($parsed.tool_input.path) {
            $filePath = $parsed.tool_input.path
        }
    }
    if ([string]::IsNullOrWhiteSpace($filePath)) { exit 0 }
    if (-not (Test-Path $filePath -PathType Leaf)) { exit 0 }

    # Nur strings.xml in res/values*
    if ($filePath -notmatch 'res[/\\]values[^/\\]*[/\\]strings\.xml$') {
        exit 0
    }

    # Datei lesen
    $content = $null
    try {
        $content = Get-Content $filePath -Raw -Encoding UTF8 -ErrorAction Stop
    } catch {
        exit 0
    }

    if ([string]::IsNullOrWhiteSpace($content)) { exit 0 }

    # Alle <string>-Bloecke finden
    $problems = New-Object System.Collections.ArrayList
    $stringRegex = [regex]'(?s)<string\b[^>]*>(.*?)</string>'
    $matches = $stringRegex.Matches($content)

    foreach ($m in $matches) {
        $val = $m.Groups[1].Value
        # Zeilennummer berechnen
        $lineNum = ($content.Substring(0, $m.Index) -split "`n").Count

        # Skip CDATA
        if ($val -match 'CDATA') { continue }

        # Nackte Apostrophe (nicht escaped, nicht &apos;)
        $valNoApos = $val -replace '&apos;', ''
        if ($valNoApos -match "(?<!\\)'") {
            [void]$problems.Add(@{ line = $lineNum; msg = "nackter Apostroph im <string>-Wert (Build-Killer in vielen Locales)" })
        }

        # Nackte doppelte Anfuehrungszeichen
        $valNoQuot = $val -replace '&quot;', ''
        if ($valNoQuot -match '(?<!\\)"') {
            [void]$problems.Add(@{ line = $lineNum; msg = "nacktes Anfuehrungszeichen im <string>-Wert" })
        }

        # & ohne gueltige Entity
        if ($val -match '&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)') {
            [void]$problems.Add(@{ line = $lineNum; msg = "nacktes '&' ohne Entity-Schreibweise" })
        }

        if ($problems.Count -ge 20) { break }
    }

    if ($problems.Count -gt 0) {
        [Console]::Error.WriteLine("[finale] strings.xml-Lint-Warnungen fuer ${filePath}:")
        foreach ($p in $problems) {
            [Console]::Error.WriteLine("[finale]   line:$($p.line)|$($p.msg)")
        }
        [Console]::Error.WriteLine("[finale] Diese Probleme koennen den Android-Build brechen. fix-applier sollte sie eigentlich vermeiden — bitte pruefen.")
    }
}
catch {
    exit 0
}

exit 0
