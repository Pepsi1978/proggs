# Installs the unofficial DLSS 5 neural-rendering mod (OptiScaler-DLSSNR fork + NVIDIA's nvngx_dlssnr.dll)
# into single-player games that already ship DLSS. Nothing is committed to the repo: all binaries are
# downloaded into %LOCALAPPDATA%\DLSS5-Mod\downloads, pinned by SHA-256, and the model DLL must carry a
# valid NVIDIA Authenticode signature or nothing is installed.
# Usage: .\installieren.ps1                      (default game list below)
#        .\installieren.ps1 -Spiel 'Cyberpunk 2077'
param([string[]]$Spiel)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$Pakete = @{
    Opti  = @{ Url = 'https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/download/v0.2.0-dlssnr/OptiScaler-DLSSNR-v0.2.0.zip'
               Sha = '8eece7a4d7de6de5917f0c99ac60540b2d77022e7699bba717b0a6d9e1829bce' }
    Model = @{ Url = 'https://github.com/RankFTW/rhi-repo/releases/download/dlssnr-310.8.0/nvngx_dlssnr_310.8.0.zip'
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
# Kernel anti-cheat: a ReShade/OptiScaler hook there risks a permanent account ban. Never install.
$AntiCheat ='Randgrid.sys','randgrid.sys','skuld.sys','EasyAntiCheat','BattlEye','BEService*','EAAntiCheat*','ACE-*','AntiCheatExpert'

$cache = "$env:LOCALAPPDATA\DLSS5-Mod\downloads"
New-Item -ItemType Directory -Force $cache | Out-Null
foreach ($k in $Pakete.Keys) {
    $zip = Join-Path $cache (Split-Path $Pakete[$k].Url -Leaf)
    if (-not (Test-Path $zip)) { "Lade $(Split-Path $zip -Leaf) ..."; Invoke-WebRequest $Pakete[$k].Url -OutFile $zip }
    if ((Get-FileHash $zip -Algorithm SHA256).Hash.ToLower() -ne $Pakete[$k].Sha) { Remove-Item $zip; throw "SHA-256 von $(Split-Path $zip -Leaf) stimmt nicht - abgebrochen." }
    $Pakete[$k].Dir = Join-Path $cache $k
    if (-not (Test-Path $Pakete[$k].Dir)) { Expand-Archive $zip $Pakete[$k].Dir }
}
$model = Get-ChildItem $Pakete.Model.Dir -Recurse -Filter 'nvngx_dlssnr.dll' | Select-Object -First 1
$sig = Get-AuthenticodeSignature $model.FullName
if ($sig.Status -ne 'Valid' -or $sig.SignerCertificate.Subject -notmatch 'O=NVIDIA Corporation') { throw "nvngx_dlssnr.dll ist nicht gültig von NVIDIA signiert ($($sig.Status)) - abgebrochen." }
"Modell: nvngx_dlssnr.dll $($model.VersionInfo.FileVersion), NVIDIA-Signatur gültig"

$optiFiles = Get-ChildItem $Pakete.Opti.Dir -Recurse -File | Where-Object { $_.Name -notmatch '^(setup_windows\.bat|setup_linux\.sh|!! .*|READ ME.*)$' }
$ziel = if ($Spiel) { $Spiel } else { @($Spiele.Keys) }
foreach ($name in $ziel) {
    $exe = $Spiele[$name]
    if (-not $exe -or -not (Test-Path $exe)) { "[$name] übersprungen: Exe nicht gefunden"; continue }
    $dir = Split-Path $exe
    $root = ($exe -split '\\steamapps\\common\\')[0] + '\steamapps\common\' + (($exe -split '\\steamapps\\common\\')[1] -split '\\')[0]
    # CoD MW2 was tried on 21.09.2026 on explicit request: the game crashes ~1 s after OptiScaler init (bugs M12).
    if (Get-ChildItem $root -Recurse -Depth 4 -Include $AntiCheat -ErrorAction SilentlyContinue | Select-Object -First 1) { "[$name] BLOCKIERT: Anti-Cheat gefunden, Bann-Risiko"; continue }
    if (Get-Process -Name ([IO.Path]::GetFileNameWithoutExtension($exe)) -ErrorAction SilentlyContinue) { "[$name] übersprungen: Spiel läuft gerade"; continue }
    $dxgi = Join-Path $dir 'dxgi.dll'
    if ((Test-Path $dxgi) -and (Get-Item $dxgi).VersionInfo.OriginalFilename -ne 'OptiScaler.dll') { "[$name] übersprungen: fremde dxgi.dll vorhanden (z. B. ReShade)"; continue }

    $manifest = @()
    foreach ($f in $optiFiles) {
        $rel = $f.FullName.Substring($Pakete.Opti.Dir.Length + 1)
        if ($rel -eq 'OptiScaler.dll') { $rel = 'dxgi.dll' }
        $dst = Join-Path $dir $rel
        New-Item -ItemType Directory -Force (Split-Path $dst) | Out-Null
        Copy-Item $f.FullName $dst -Force
        $manifest += $rel
    }
    Copy-Item $model.FullName (Join-Path $dir 'nvngx_dlssnr.dll') -Force; $manifest += 'nvngx_dlssnr.dll'

    # Turn the neural pass on so no overlay click is needed (only the key inside [DlssNr]).
    $iniPath = Join-Path $dir 'OptiScaler.ini'
    $ini = Get-Content $iniPath; $inNr = $false
    for ($i = 0; $i -lt $ini.Count; $i++) {
        if ($ini[$i] -match '^\[') { $inNr = $ini[$i] -eq '[DlssNr]' }
        elseif ($ini[$i] -eq 'Dx12Upscaler=auto') { $ini[$i] = 'Dx12Upscaler=dlss' }  # auto = DLSS on RTX anyway, pin it
        elseif ($inNr -and $ini[$i] -eq 'Enabled=auto') { $ini[$i] = 'Enabled=true' }
    }
    Set-Content $iniPath $ini -Encoding UTF8
    $manifest += 'OptiScaler.log'
    Set-Content (Join-Path $dir 'dlss5-mod-dateien.txt') $manifest -Encoding UTF8
    "[$name] installiert in $dir ($($manifest.Count - 1) Dateien, DLSS 5 an)"
}
