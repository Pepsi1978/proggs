# Erstellt die Desktop-Verknuepfung fuer OpenCodeLauncher mit dem eingebetteten Icon.
# Die .exe enthaelt das Icon bereits (ApplicationIcon im csproj), darum reicht es,
# die Verknuepfung direkt auf die exe zeigen zu lassen (IconLocation = exe).

$exe = "C:\Users\barwa\proggs\OpenCodeLauncher\bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenCodeLauncher.exe"
$desktopDir = [Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)
$lnkPath = Join-Path $desktopDir "OpenCode Launcher.lnk"

if (-not (Test-Path $exe)) {
    Write-Error "EXE nicht gefunden: $exe"
    exit 1
}

$shell = New-Object -ComObject WScript.Shell
$lnk = $shell.CreateShortcut($lnkPath)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = "C:\Users\barwa\proggs\OpenCodeLauncher"
$lnk.IconLocation = "$exe,0"
$lnk.Description = "OpenCode Launcher — Modell + Provider fuer OpenCode auswaehlen und starten"
$lnk.WindowStyle = 1
$lnk.Save()

Write-Output "Verknuepfung erstellt: $lnkPath"
Write-Output "  -> Ziel: $exe"
Write-Output "  -> Icon: $exe,0"
