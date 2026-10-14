# Installs ShortFuse's "RenoDX DLSS" ReShade add-on (renodx-dlss.addon64) + ReShade 6.8.0 (add-on edition)
# + NVIDIA's nvngx_dlssnr.dll into single-player games that ship DLSS. Replaces the OptiScaler-DLSSNR route:
# both need the dxgi.dll slot and only one neural consumer may be loaded per game (bugs M6).
# Nothing is committed to the repo: binaries go to %LOCALAPPDATA%\DLSS5-Mod\downloads, pinned by SHA-256,
# and the model DLL must carry a valid NVIDIA Authenticode signature. Writes dlss5-mod-dateien.txt, so
# entfernen.ps1 removes this route as well.
# Usage: .\renodx-installieren.ps1                       (default game list below)
#        .\renodx-installieren.ps1 -Spiel 'Cyberpunk 2077'
param([string[]]$Spiel)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$Pakete = @{
    ReShade = @{ Url = 'https://reshade.me/downloads/ReShade_Setup_6.8.0_Addon.exe'
                 Sha = 'afe4c8f13048306307983b8b3d41d5bf00a86820440b0e57dea10950e1176445' }
    RenoDX  = @{ Url = 'https://github.com/RankFTW/rhi-repo/releases/download/renodx-dlss-SF-26.1003.2350/renodx-dlss_SF_26.1003.2350.zip'
                 Sha = '34ef92d6742bf6d05df42ff81c04b70f5138b0a0a970a93f1a04295ac83f7f0f' }
    Model   = @{ Url = 'https://github.com/RankFTW/rhi-repo/releases/download/dlssnr-310.8.0/nvngx_dlssnr_310.8.0.zip'
                 Sha = '388c0a7912e15ec911b9c9e11a692142b11fe387ddf2b637d8c358138fffb3ac' }
}

# Folder = where the game's exe lives (not always the install root).
$Spiele = [ordered]@{
    'Cyberpunk 2077'    = 'G:\SteamLibrary\steamapps\common\Cyberpunk 2077\bin\x64\Cyberpunk2077.exe'
    'Starfield'         = 'D:\SteamLibrary\steamapps\common\Starfield\Starfield.exe'
    'Manor Lords'       = 'D:\SteamLibrary\steamapps\common\Manor Lords\ManorLords\Binaries\Win64\ManorLords-Win64-Shipping.exe'
    'Star Trek Voyager' = 'G:\SteamLibrary\steamapps\common\Star Trek Voyager - Across the Unknown\STVoyager\Binaries\Win64\STVoyagerSteam-Win64-Shipping.exe'
    'AC Shadows'        = 'D:\SteamLibrary\steamapps\common\Assassin''s Creed Shadows\ACShadows.exe'
}
# Kernel anti-cheat: a ReShade hook there risks a permanent account ban. Never install.
$AntiCheat = 'Randgrid.sys','randgrid.sys','skuld.sys','EasyAntiCheat','BattlEye','BEService*','EAAntiCheat*','ACE-*','AntiCheatExpert'

$cache = "$env:LOCALAPPDATA\DLSS5-Mod\downloads"
New-Item -ItemType Directory -Force $cache | Out-Null
foreach ($k in $Pakete.Keys) {
    $file = Join-Path $cache (Split-Path $Pakete[$k].Url -Leaf)
    if (-not (Test-Path $file)) { "Lade $(Split-Path $file -Leaf) ..."; Invoke-WebRequest $Pakete[$k].Url -OutFile $file -UserAgent 'Mozilla/5.0' }
    if ((Get-FileHash $file -Algorithm SHA256).Hash.ToLower() -ne $Pakete[$k].Sha) { Remove-Item $file; throw "SHA-256 von $(Split-Path $file -Leaf) stimmt nicht - abgebrochen." }
    $Pakete[$k].Dir = Join-Path $cache "renodx-route\$k"
    if (Test-Path $Pakete[$k].Dir) { continue }
    if ($file -like '*.exe') {
        # The ReShade setup is an exe with an appended zip; Expand-Archive can't read it, Windows' bsdtar can.
        New-Item -ItemType Directory $Pakete[$k].Dir | Out-Null
        tar -xf $file -C $Pakete[$k].Dir ReShade64.dll
        if ($LASTEXITCODE) { Remove-Item $Pakete[$k].Dir -Recurse; throw 'ReShade64.dll ließ sich nicht aus dem Setup entpacken - abgebrochen.' }
    } else { Expand-Archive $file $Pakete[$k].Dir }
}
$reshade = Join-Path $Pakete.ReShade.Dir 'ReShade64.dll'
$addon = Get-ChildItem $Pakete.RenoDX.Dir -Recurse -Filter 'renodx-dlss.addon64' | Select-Object -First 1
$model = Get-ChildItem $Pakete.Model.Dir -Recurse -Filter 'nvngx_dlssnr.dll' | Select-Object -First 1
if (-not (Test-Path $reshade) -or -not $addon) { throw 'ReShade64.dll oder renodx-dlss.addon64 fehlt im Paket - abgebrochen.' }
$sig = Get-AuthenticodeSignature $model.FullName
if ($sig.Status -ne 'Valid' -or $sig.SignerCertificate.Subject -notmatch 'O=NVIDIA Corporation') { throw "nvngx_dlssnr.dll ist nicht gültig von NVIDIA signiert ($($sig.Status)) - abgebrochen." }
"ReShade $((Get-Item $reshade).VersionInfo.FileVersion) · RenoDX DLSS SF-26.1003.2350 · Modell nvngx_dlssnr.dll $($model.VersionInfo.FileVersion) (NVIDIA-Signatur gültig)"

$ziel = if ($Spiel) { $Spiel } else { @($Spiele.Keys) }
foreach ($name in $ziel) {
    $exe = $Spiele[$name]
    if (-not $exe -or -not (Test-Path $exe)) { "[$name] übersprungen: Exe nicht gefunden"; continue }
    $dir = Split-Path $exe
    $root = ($exe -split '\\steamapps\\common\\')[0] + '\steamapps\common\' + (($exe -split '\\steamapps\\common\\')[1] -split '\\')[0]
    if (Get-ChildItem $root -Recurse -Depth 4 -Include $AntiCheat -ErrorAction SilentlyContinue | Select-Object -First 1) { "[$name] BLOCKIERT: Anti-Cheat gefunden, Bann-Risiko"; continue }
    if (Get-Process -Name ([IO.Path]::GetFileNameWithoutExtension($exe)) -ErrorAction SilentlyContinue) { "[$name] übersprungen: Spiel läuft gerade"; continue }

    # Remove a previous install of either route (OptiScaler-DLSSNR or this one) via its file list.
    $liste = Join-Path $dir 'dlss5-mod-dateien.txt'
    if (Test-Path $liste) {
        foreach ($rel in Get-Content $liste) { $p = Join-Path $dir $rel; if (Test-Path $p) { Remove-Item $p -Force } }
        $cap = Join-Path $dir 'dlssnr-capture'; if (Test-Path $cap) { Remove-Item $cap -Recurse -Force }
        foreach ($sub in 'OptiScaler\D3D12_OptiScaler','OptiScaler','Licenses') { $p = Join-Path $dir $sub; if ((Test-Path $p) -and -not (Get-ChildItem $p -Recurse -File)) { Remove-Item $p -Recurse -Force } }
        Remove-Item $liste -Force
    }
    if (Test-Path (Join-Path $dir 'dxgi.dll')) { "[$name] übersprungen: fremde dxgi.dll vorhanden"; continue }
    $fremd = Get-ChildItem $dir -Filter '*.addon64' | Where-Object Name -ne 'renodx-dlss.addon64'
    if ($fremd) { "[$name] übersprungen: anderes Add-on vorhanden ($($fremd.Name -join ', ')) - nur ein neurales Add-on pro Spiel"; continue }

    Copy-Item $reshade (Join-Path $dir 'dxgi.dll') -Force
    Copy-Item $addon.FullName (Join-Path $dir 'renodx-dlss.addon64') -Force
    Copy-Item $model.FullName (Join-Path $dir 'nvngx_dlssnr.dll') -Force
    $ini = Join-Path $dir 'ReShade.ini'
    if (-not (Test-Path $ini)) { Set-Content $ini "[OVERLAY]`r`nTutorialProgress=4`r`n" -Encoding ASCII }
    Set-Content $liste 'dxgi.dll','renodx-dlss.addon64','nvngx_dlssnr.dll','ReShade.ini','ReShade.log' -Encoding UTF8
    "[$name] installiert in $dir (ReShade als dxgi.dll + RenoDX DLSS + Modell)"
}
