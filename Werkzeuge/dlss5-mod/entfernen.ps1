# Removes the DLSS 5 mod from every game folder that has a dlss5-mod-dateien.txt (written by installieren.ps1).
# Usage: .\entfernen.ps1                   (all games)
#        .\entfernen.ps1 -Spiel 'Starfield'  (folder name part match)
param([string]$Spiel)
$roots = 'C:\Program Files (x86)\Steam\steamapps\common','D:\SteamLibrary\steamapps\common','G:\SteamLibrary\steamapps\common'
$lists = Get-ChildItem $roots -Recurse -Depth 5 -Filter 'dlss5-mod-dateien.txt' -ErrorAction SilentlyContinue | Where-Object { -not $Spiel -or $_.FullName -like "*$Spiel*" }
if (-not $lists) { 'Keine Mod-Installation gefunden.'; return }
foreach ($l in $lists) {
    $dir = $l.DirectoryName
    foreach ($rel in Get-Content $l.FullName) { $p = Join-Path $dir $rel; if (Test-Path $p) { Remove-Item $p -Force } }
    $cap = Join-Path $dir 'dlssnr-capture'; if (Test-Path $cap) { Remove-Item $cap -Recurse -Force }
    foreach ($sub in 'OptiScaler\D3D12_OptiScaler','OptiScaler','Licenses') { $p = Join-Path $dir $sub; if ((Test-Path $p) -and -not (Get-ChildItem $p -Recurse -File)) { Remove-Item $p -Recurse -Force } }
    Remove-Item $l.FullName -Force
    "Entfernt: $dir"
}
