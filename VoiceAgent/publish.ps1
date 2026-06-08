# publish.ps1 — Erzeugt eine einzelne VoiceAgent.exe (self-contained)
# Ausfuehren:  pwsh -File publish.ps1
#
# WICHTIG (Bug-Almanach dotnet-csharp §1.5): KEIN PublishTrimmed / PublishAot fuer WPF
# (WPF nutzt Reflection auf XAML -> Trimming wuerde die App zerstoeren).

$ErrorActionPreference = "Stop"

$proj = Join-Path $PSScriptRoot "src\VoiceAgent\VoiceAgent.csproj"
$out  = Join-Path $PSScriptRoot "publish"

Write-Host "Baue VoiceAgent (Release, self-contained single-file)..." -ForegroundColor Cyan

dotnet publish $proj -c Release -r win-x64 --self-contained true `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -o $out

if ($LASTEXITCODE -eq 0) {
    # WICHTIG (Bug 2026-06-08): Der Single-File-Publish legt Content-Ordner zwar an, kopiert die
    # Dateien aber NICHT zuverlaessig neben die EXE (bekannte single-file-Eigenheit). Folge sonst:
    # Wake-Word-Modell (encoder.onnx) fehlt -> SherpaWakeWordEngine wirft FileNotFound -> Weckwort
    # faellt aus und die App verarbeitet JEDE Aussage, obwohl sie "ruht". Deshalb hier explizit
    # alle Projekt-Assets (Modell + Sounds + Icon) und capabilities.md neben die EXE spiegeln.
    Write-Host "Stelle Content-Assets neben der EXE sicher (Wake-Word-Modell, Sounds)..." -ForegroundColor Cyan
    $srcAssets = Join-Path $PSScriptRoot "src\VoiceAgent\assets"
    $dstAssets = Join-Path $out "assets"
    New-Item -ItemType Directory -Force -Path $dstAssets | Out-Null
    Copy-Item (Join-Path $srcAssets "*") $dstAssets -Recurse -Force
    Copy-Item (Join-Path $PSScriptRoot "src\VoiceAgent\capabilities.md") $out -Force -ErrorAction SilentlyContinue
    $enc = Join-Path $dstAssets "wakeword-model\encoder.onnx"
    if (Test-Path $enc) {
        Write-Host "  OK: Wake-Word-Modell vorhanden ($([math]::Round((Get-Item $enc).Length/1MB,1)) MB)" -ForegroundColor Green
    } else {
        Write-Host "  WARNUNG: Wake-Word-Modell fehlt weiterhin — Weckwort wuerde ausfallen!" -ForegroundColor Red
    }

    $exe = Get-Item (Join-Path $out "VoiceAgent.exe")
    Write-Host "`nErfolgreich gebaut!" -ForegroundColor Green
    Write-Host "Datei:   $($exe.FullName)"
    Write-Host "Groesse: $([math]::Round($exe.Length / 1MB, 1)) MB"
    Write-Host "`nBeim ersten Start: Einstellungen oeffnen und API-Schluessel eintragen"
    Write-Host "(werden sicher in ~/SK/VoiceAgent/keys.json gespeichert)."
} else {
    Write-Host "`nBuild fehlgeschlagen!" -ForegroundColor Red
    exit 1
}
