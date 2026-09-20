# Erstellt die Desktop-Verknuepfung fuer die UpdateZentrale.
# Das Icon steckt bereits in der .exe (ApplicationIcon im csproj), darum reicht die exe als
# Icon-Quelle.

$projektDir = $PSScriptRoot
$exe = Join-Path $projektDir "bin\Release\net8.0-windows10.0.19041.0\win-x64\UpdateZentrale.exe"
$desktopDir = [Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)
$lnkPath = Join-Path $desktopDir "UpdateZentrale.lnk"

if (-not (Test-Path $exe)) {
    Write-Error "EXE nicht gefunden: $exe  --  zuerst 'dotnet build -c Release' ausfuehren."
    exit 1
}

$shell = New-Object -ComObject WScript.Shell
$lnk = $shell.CreateShortcut($lnkPath)
$lnk.TargetPath = $exe
$lnk.WorkingDirectory = $projektDir
$lnk.IconLocation = "$exe,0"
$lnk.Description = "UpdateZentrale - alle Werkzeuge pruefen und aktualisieren"
$lnk.WindowStyle = 1
$lnk.Save()

Write-Output "Verknuepfung erstellt: $lnkPath"
Write-Output "  -> Ziel: $exe"
