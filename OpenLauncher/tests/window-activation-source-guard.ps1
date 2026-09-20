# Quellcode-Guard fuer die Fenster-Aktivierung des OpenLaunchers.
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

function Remove-CodeComments {
    # Entfernt Zeilen- und Blockkommentare. Reiner Textfilter, bewusst simpel: im geprueften
    # Fenster-Code gibt es keine String-Literale, die "//" enthalten.
    param([string]$Text)
    $withoutBlocks = [regex]::Replace($Text, '(?s)/\*.*?\*/', '')
    return [regex]::Replace($withoutBlocks, '(?m)//.*$', '')
}

$code = Remove-CodeComments -Text $source

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

# 6. Der SC_RESTORE-Zweig darf die Vordergrund-Sequenz nicht anwerfen. Belegt am 20.09.2026: auf
#    Windows 11 26200 kommt SC_RESTORE auch beim gewoehnlichen Taskleisten-Klick an, wo Windows die
#    Aktivierung selbst besitzt. Die frueheren Guard-Regeln 1 und 2 deckten nur Activated und
#    StateChanged ab — derselbe verbotene Selbst-Push lief ueber WndProc ungehindert weiter.
$wndProcBlock = Get-BlockAfter -Text $code -Anchor 'private\s+IntPtr\s+WndProc\('
if ($wndProcBlock -match 'QueueBringToTaskbarForeground') {
    $failures.Add('WndProc ruft QueueBringToTaskbarForeground auf (SC_RESTORE-Zweig). Dieser Pfad laeuft auch beim Taskleisten-Klick; Windows besitzt ihn.')
}
if ($wndProcBlock -notmatch 'BeginShellRestoreWatch') {
    $failures.Add('WndProc beobachtet die Restore-Anforderung nicht (BeginShellRestoreWatch fehlt). Ohne Beobachtung ist ein fehlgeschlagener Restore wieder unsichtbar.')
}

# 7. Vordergrund allein ist kein Erfolg. Ein minimiertes Fenster kann den Vordergrund-Status halten;
#    eine Pruefung ohne IsIconic meldete am 20.09.2026 acht Mal activated=true, waehrend das Fenster
#    nativ minimiert bei -16000/-16000 hing, und uebersprang dadurch jeden Fallback.
if ($code -match '(?m)^\s*return\s+GetForegroundWindow\(\)\s*==\s*hwnd\s*;') {
    $failures.Add('Eine Aktivierung gilt als erfolgreich, sobald das HWND den Vordergrund haelt. IsIconic muss mitgeprueft werden (IsForegroundAndVisible).')
}
if ($code -notmatch 'IsForegroundAndVisible') {
    $failures.Add('Es gibt kein Erfolgsmass, das Vordergrund UND Sichtbarkeit prueft (IsForegroundAndVisible fehlt).')
}

# 8. Die Eingabewarteschlange darf nie an einen Shell-Thread geheftet werden — dokumentierte Ursache
#    des Zustands "laesst sich bis zum Neustart nicht mehr aktivieren".
if ($code -match 'AttachThreadInput' -and $code -notmatch 'BelongsToShell') {
    $failures.Add('AttachThreadInput wird ohne Shell-Pruefung aufgerufen (BelongsToShell fehlt).')
}

# 9. Die Selbstheilung darf ein bewusst minimiertes Fenster nicht hochreissen. Sie greift nur, wenn
#    seit der Restore-Anforderung KEIN Minimieren kam. Ein Heilen nach einem Minimieren waere vom
#    Doppelklick des Benutzers nicht unterscheidbar (Fix-Induced-Failure vom 24.07.2026).
if ($code -match 'ShouldHealMissingRestore') {
    $healExpr = [regex]::Match($code, 'ShouldHealMissingRestore\(\)\s*=>([^;]*);')
    $healSource = if ($healExpr.Success) {
        $healExpr.Groups[1].Value
    } else {
        Get-BlockAfter -Text $code -Anchor 'bool\s+ShouldHealMissingRestore'
    }
    if ($healSource -notmatch '_lastNativeMinimizeUtc\s*<\s*_restoreRequestedUtc') {
        $failures.Add('Die Selbstheilung prueft nicht mehr, dass seit der Restore-Anforderung kein Minimieren kam. Sie wuerde absichtlich minimierte Fenster hochreissen.')
    }
} else {
    $failures.Add('ShouldHealMissingRestore fehlt. Ohne diese Bedingung heilt die Selbstheilung ungeprueft.')
}

if ($failures.Count -gt 0) {
    Write-Host "Quellcode-Guard FEHLGESCHLAGEN ($SourcePath):" -ForegroundColor Red
    foreach ($f in $failures) { Write-Host "  - $f" -ForegroundColor Red }
    throw "Window-Activation-Quellcode-Guard: $($failures.Count) Verstoss/Verstoesse."
}

[pscustomobject]@{
    Source = $SourcePath
    Checks = 9
    Result = 'passed'
} | Format-List
