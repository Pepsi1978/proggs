# Baut scripts/handy-helper.dex neu. Nur noetig, wenn HandyHelper.java geaendert wurde; das Ergebnis liegt im Repo.
$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$plattform = Get-ChildItem "$sdk\platforms" | Sort-Object Name | Select-Object -Last 1
$bt = Get-ChildItem "$sdk\build-tools" | Sort-Object Name | Select-Object -Last 1
$tmp = Join-Path $env:TEMP 'handy-helper-bau'
Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory $tmp | Out-Null
javac -encoding UTF-8 --release 11 -nowarn -cp "$($plattform.FullName)\android.jar" -d $tmp "$PSScriptRoot\HandyHelper.java"
$klassen = @((Get-ChildItem $tmp -Filter *.class).FullName)
& "$($bt.FullName)\d8.bat" --min-api 26 --lib "$($plattform.FullName)\android.jar" --output $tmp $klassen
Copy-Item "$tmp\classes.dex" "$PSScriptRoot\..\scripts\handy-helper.dex" -Force
(Get-Item "$PSScriptRoot\..\scripts\handy-helper.dex").Length
