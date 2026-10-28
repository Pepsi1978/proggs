# Startet Fernsteuerungs-Dienst und Weiterleitung unsichtbar, falls sie nicht schon laufen. Die Aufgabe
# „JarvisFernsteuerung“ (einrichten.ps1) ruft das bei der Anmeldung und danach jede Minute auf; beide Teile
# erkennen einen zweiten Start selbst am belegten Port und beenden sich dann sofort.
$ErrorActionPreference = 'SilentlyContinue'
$ordner = $PSScriptRoot
$logdir = Join-Path $env:LOCALAPPDATA 'JarvisFernsteuerung'
New-Item -ItemType Directory -Force $logdir | Out-Null

function Lauscht([int]$port) { [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) }

if (-not (Lauscht 47801)) {
    $py = (Get-Command pythonw.exe).Source
    Start-Process -FilePath $py -ArgumentList "`"$ordner\fernsteuerung.py`"" -WindowStyle Hidden `
        -RedirectStandardError (Join-Path $logdir 'dienst.log') -RedirectStandardOutput (Join-Path $logdir 'dienst.out.log')
}
if (-not (Lauscht 47800)) {
    $node = Join-Path $env:ProgramFiles 'nodejs\node.exe'
    Start-Process -FilePath $node -ArgumentList "`"$ordner\weiterleitung.js`"" -WindowStyle Hidden `
        -RedirectStandardError (Join-Path $logdir 'weiterleitung.log') -RedirectStandardOutput (Join-Path $logdir 'weiterleitung.out.log')
}
