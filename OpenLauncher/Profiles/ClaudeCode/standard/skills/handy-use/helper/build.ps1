# Baut scripts/handy-helper.dex neu (Windows). Nur noetig, wenn HandyHelper.java geaendert wurde; das Ergebnis liegt im Repo. macOS und Linux: build.sh
$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
function Neueste($ordner, $praefix) {
    Get-ChildItem $ordner | Where-Object { $_.Name -match "^$praefix(\d+(\.\d+)*)$" } |
        Sort-Object { [version](($_.Name -replace "^$praefix", '') + '.0') } | Select-Object -Last 1
}
$plattform = Neueste "$sdk\platforms" 'android-'
$bt = Neueste "$sdk\build-tools" ''
$tmp = Join-Path $env:TEMP 'handy-helper-bau'
Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory $tmp | Out-Null
javac -encoding UTF-8 --release 11 -nowarn -cp "$($plattform.FullName)\android.jar" -d $tmp "$PSScriptRoot\HandyHelper.java"
if ($LASTEXITCODE -ne 0) { throw 'javac fehlgeschlagen' }
$klassen = @((Get-ChildItem $tmp -Filter *.class).FullName)
& "$($bt.FullName)\d8.bat" --min-api 26 --lib "$($plattform.FullName)\android.jar" --output $tmp $klassen
if ($LASTEXITCODE -ne 0) { throw 'd8 fehlgeschlagen' }
Copy-Item "$tmp\classes.dex" "$PSScriptRoot\..\scripts\handy-helper.dex" -Force
(Get-Item "$PSScriptRoot\..\scripts\handy-helper.dex").Length
