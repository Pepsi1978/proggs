# Quellcode-Guard fuer die Fenster-Aktivierung des OpenCode Launchers.
#
# Hintergrund: Der Defekt "Klick auf den Taskleisten-Button holt das Fenster nicht nach vorn, nur
# ein Neustart hilft" entsteht dadurch, dass die App bei JEDEM Fensterwechsel eine verzoegerte
# Win32-Vordergrundsequenz nachschiebt und dabei per AttachThreadInput die Eingabewarteschlange an
# den Shell-Thread (explorer.exe) koppelt. Loest sich diese Kopplung durch ein Race nicht, ist der
# Prozess bis zum Neustart nicht mehr aktivierbar.
#
# Warum ein Quellcode-Guard und nicht nur ein Laufzeittest: Das Leck ist ein Timing-Race und laesst
# sich programmgesteuert nicht zuverlaessig ausloesen — der Laufzeittest (window-activation-smoke.ps1)
# besteht deshalb auch auf dem defekten Stand. Die eigentliche Regression ist strukturell und in
# diesem Code bereits zweimal aufgetreten (12.07. und 18.07.2026: Fix war dokumentiert, aber nicht
# im Quellstand). Genau dagegen sichert dieser Guard ab. Beide Tests gehoeren zusammen.

[CmdletBinding()]
param(
    [string]$SourcePath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'MainWindow.xaml.cs')
)

$ErrorActionPreference = 'Stop'
$SourcePath = (Resolve-Path -LiteralPath $SourcePath).Path
$source = Get-Content -LiteralPath $SourcePath -Raw
$failures = New-Object System.Collections.Generic.List[string]

function Get-BlockAfter {
    # Liefert den Code-Block ab dem ersten Treffer von $Anchor bis zum Ende seines geschweiften
    # Blocks. Klammern werden gezaehlt statt per Regex geraten: ein nicht-gieriges .*? wuerde ueber
    # das Blockende hinauslaufen und irgendeinen spaeteren Treffer in der Datei melden (False Positive).
    param([string]$Text, [string]$Anchor)

    $start = [regex]::Match($Text, $Anchor)
    if (-not $start.Success) { return '' }

    $open = $Text.IndexOf('{', $start.Index)
    if ($open -lt 0) { return '' }

    $depth = 0
    for ($i = $open; $i -lt $Text.Length; $i++) {
        switch ($Text[$i]) {
            '{' { $depth++ }
            '}' {
                $depth--
                if ($depth -eq 0) { return $Text.Substring($open, $i - $open + 1) }
            }
        }
    }
    # Unbalancierte Klammern: Rest zurueckgeben statt still leer — sonst wuerde ein kaputter
    # Quellstand den Guard versehentlich bestehen lassen.
    return $Text.Substring($open)
}

# 1. Kein Vordergrund-Push aus Activated. Beim Klick auf den Taskleisten-Button aktiviert Windows
#    das Fenster selbst; ein nachgeschobener Callback funkt in diese laufende Sequenz hinein.
#    Einzeiler (Activated += (_, _) => ...;) haben keinen Block und werden separat geprueft.
# Kein '$'-Anker: bei CRLF-Zeilenenden steht nach [^\r\n]* noch das \r, und .NET setzt $ erst
# unmittelbar vor das \n — der Ausdruck wuerde nie matchen und die Regel still durchfallen.
$activatedInline = [regex]::Match($source, '(?m)^[ \t]*Activated\s*\+=[^\r\n]*')
$activatedIsSingleLine = $activatedInline.Success -and $activatedInline.Value.TrimEnd().EndsWith(';')
$activatedBlock = if ($activatedInline.Success -and -not $activatedIsSingleLine) {
    Get-BlockAfter -Text $source -Anchor '(?m)^[ \t]*Activated\s*\+='
} else { '' }
if (($activatedInline.Success -and $activatedInline.Value -match 'QueueBringToTaskbarForeground') -or
    ($activatedBlock -match 'QueueBringToTaskbarForeground')) {
    $failures.Add('Activated-Handler ruft QueueBringToTaskbarForeground auf. Windows besitzt diesen Pfad; der Push erzeugt genau den Aktivierungsdefekt.')
}

# 2. Kein Vordergrund-Push aus StateChanged, aus demselben Grund.
$stateChangedBlock = Get-BlockAfter -Text $source -Anchor '(?m)^\s*StateChanged\s*\+='
if ($stateChangedBlock -match 'QueueBringToTaskbarForeground') {
    $failures.Add('StateChanged-Handler ruft QueueBringToTaskbarForeground auf. Zustandswechsel sind keine Aktivierungsanforderung.')
}

# 3. AttachThreadInput muss sein Detach pruefen. Ein still fehlgeschlagenes Detach laesst die
#    Eingabekopplung bestehen und ist damit die eigentliche Ursache des "nur Neustart hilft"-Zustands.
if ($source -match 'AttachThreadInput') {
    if ($source -notmatch '(?ms)finally\s*\{[^}]*?AttachThreadInput\([^)]*?,\s*false\)') {
        $failures.Add('AttachThreadInput wird nicht in einem finally-Block geloest.')
    }
    if ($source -notmatch '(?ms)var\s+detached\s*=\s*AttachThreadInput\([^)]*?,\s*false\)') {
        $failures.Add('Das Ergebnis des AttachThreadInput-Detach wird nicht ausgewertet. Ein stiller Fehlschlag bleibt sonst unbemerkt.')
    }
}

# 4. Keine booleschen Reentranz-Flags fuer die Aktivierung. Ein Flag, dessen Reset ausfaellt,
#    blockiert die Fensteraktivierung dauerhaft — dieselbe Fehlerklasse wie beim Hotkey-Debounce
#    (Bug-Case 2026-06-21). Zeitstempel laufen immer von selbst ab.
if ($source -match '(?m)^\s*private\s+bool\s+_(isBringingToForeground|bringToForegroundQueued)\b') {
    $failures.Add('Boolesche Reentranz-Flags fuer die Vordergrundaktivierung gefunden. Zeitbasierte Sperren verwenden, die nicht haengen bleiben koennen.')
}

# 5. Restore ausschliesslich nativ: WindowState.Normal wuerde einen maximierten Launcher
#    auf Normalgroesse zuruecksetzen.
$bringBlock = Get-BlockAfter -Text $source -Anchor 'private\s+void\s+BringToTaskbarForeground\(string\s+reason\)'
if ($bringBlock -match 'WindowState\s*=\s*WindowState\.Normal') {
    $failures.Add('BringToTaskbarForeground setzt WindowState.Normal statt nativ per SW_RESTORE zu restaurieren.')
}
if ($bringBlock -notmatch 'ShowWindow\([^)]*SW_RESTORE\)') {
    $failures.Add('BringToTaskbarForeground restauriert nicht nativ per SW_RESTORE.')
}

if ($failures.Count -gt 0) {
    Write-Host "Quellcode-Guard FEHLGESCHLAGEN ($SourcePath):" -ForegroundColor Red
    foreach ($f in $failures) { Write-Host "  - $f" -ForegroundColor Red }
    throw "Window-Activation-Quellcode-Guard: $($failures.Count) Verstoss/Verstoesse."
}

[pscustomobject]@{
    Source = $SourcePath
    Checks = 5
    Result = 'passed'
} | Format-List
