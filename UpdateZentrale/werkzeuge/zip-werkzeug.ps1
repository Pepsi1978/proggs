# zip-werkzeug.ps1 -- Update für Werkzeuge ohne winget-Paket und ohne Selbst-Update (Stand 23.10.2026, 11:40 Uhr)
#
# Gradle, Kotlin-Compiler und uv liegen als entpackte Archive unter %LOCALAPPDATA%\Programs\<Werkzeug>\<Version>.
# Dieses Skript lädt die neueste Fassung vom Hersteller, entpackt sie daneben und lässt die Verknüpfung
# "current" (Junction) darauf zeigen. Im Benutzer-PATH steht danach nur noch ...\current\..., ältere
# versionierte Einträge desselben Werkzeugs werden entfernt -- beim nächsten Update ändert sich am PATH nichts mehr.
# Die vorige Fassung bleibt liegen (Rückweg), noch ältere werden gelöscht.
#
# Aufruf: pwsh -NoProfile -File zip-werkzeug.ps1 -Werkzeug gradle|kotlin|uv
# Statuszeile: ZIPWERKZEUG_STATUS=installed <version> | current <version> | failed <grund>
param(
    [Parameter(Mandatory)][ValidateSet('gradle', 'kotlin', 'uv')][string]$Werkzeug
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$programme = Join-Path $env:LOCALAPPDATA 'Programs'
$kopf = @{ 'User-Agent' = 'UpdateZentrale' }

function Neueste {
    switch ($Werkzeug) {
        'gradle' {
            $j = Invoke-RestMethod 'https://services.gradle.org/versions/current' -Headers $kopf
            return @{ Version = $j.version; Url = $j.downloadUrl }
        }
        'kotlin' {
            $j = Invoke-RestMethod 'https://api.github.com/repos/JetBrains/kotlin/releases/latest' -Headers $kopf
            $v = $j.tag_name.TrimStart('v')
            $a = $j.assets | Where-Object { $_.name -eq "kotlin-compiler-$v.zip" } | Select-Object -First 1
            if (-not $a) { throw "Kein Archiv kotlin-compiler-$v.zip im Release." }
            return @{ Version = $v; Url = $a.browser_download_url }
        }
        'uv' {
            $j = Invoke-RestMethod 'https://api.github.com/repos/astral-sh/uv/releases/latest' -Headers $kopf
            $v = $j.tag_name.TrimStart('v')
            $a = $j.assets | Where-Object { $_.name -eq 'uv-x86_64-pc-windows-msvc.zip' } | Select-Object -First 1
            if (-not $a) { throw 'Kein Archiv uv-x86_64-pc-windows-msvc.zip im Release.' }
            return @{ Version = $v; Url = $a.browser_download_url }
        }
    }
}

# Pro Werkzeug: Wurzel, Ordnername einer Version, bin-Ordner darin, Programmdatei, Versionsabfrage.
$cfg = switch ($Werkzeug) {
    'gradle' { @{ Wurzel = Join-Path $programme 'Gradle'; Ordner = { param($v) "gradle-$v" }; Bin = 'bin'; Exe = 'gradle.bat'; Args = @('--version') } }
    'kotlin' { @{ Wurzel = Join-Path $programme 'Kotlin'; Ordner = { param($v) "kotlinc-$v" }; Bin = 'kotlinc\bin'; Exe = 'kotlinc.bat'; Args = @('-version') } }
    'uv'     { @{ Wurzel = Join-Path $programme 'uv'; Ordner = { param($v) "$v" }; Bin = '.'; Exe = 'uv.exe'; Args = @('--version') } }
}

function VersionVon([string]$binOrdner) {
    $exe = Join-Path $binOrdner $cfg.Exe
    if (-not (Test-Path -LiteralPath $exe)) { return '' }
    $text = (& $exe @($cfg.Args) 2>&1 | Out-String)
    if ($text -match '\d+\.\d+\.\d+') { return $Matches[0] }
    return ''
}

function PfadAufCurrent {
    $ziel = [IO.Path]::GetFullPath((Join-Path (Join-Path $cfg.Wurzel 'current') $cfg.Bin)).TrimEnd('\')
    $wurzel = [IO.Path]::GetFullPath($cfg.Wurzel).TrimEnd('\') + '\'
    $alt = @([Environment]::GetEnvironmentVariable('Path', 'User') -split ';' | Where-Object { $_ })
    $neu = @($alt | Where-Object { -not $_.TrimEnd('\').StartsWith($wurzel, [StringComparison]::OrdinalIgnoreCase) -or $_.TrimEnd('\') -ieq $ziel })
    if (-not ($neu | Where-Object { $_.TrimEnd('\') -ieq $ziel })) { $neu += $ziel }
    if (($neu -join ';') -ne ($alt -join ';')) {
        [Environment]::SetEnvironmentVariable('Path', ($neu -join ';'), 'User')
        Write-Output "Benutzer-PATH: $ziel (versionierte Einträge unter $wurzel entfernt)."
    }
}

try {
    $neu = Neueste
    $current = Join-Path $cfg.Wurzel 'current'
    $installiert = VersionVon (Join-Path $current $cfg.Bin)
    Write-Output "Neueste Version: $($neu.Version) | aktiv (current): $(if ($installiert) { $installiert } else { '(keine)' })"

    $zielOrdner = Join-Path $cfg.Wurzel (& $cfg.Ordner $neu.Version)
    if ($installiert -ne $neu.Version) {
        if (-not (Test-Path -LiteralPath (Join-Path (Join-Path $zielOrdner $cfg.Bin) $cfg.Exe))) {
            $tmp = Join-Path ([IO.Path]::GetTempPath()) "zipwerkzeug-$Werkzeug-$([Guid]::NewGuid().ToString('N'))"
            New-Item -ItemType Directory -Path $tmp | Out-Null
            try {
                $zip = Join-Path $tmp 'paket.zip'
                Write-Output "Lade $($neu.Url)"
                Invoke-WebRequest $neu.Url -OutFile $zip -Headers $kopf
                $entpackt = Join-Path $tmp 'x'
                Expand-Archive -LiteralPath $zip -DestinationPath $entpackt
                # Gradle packt in gradle-<v>\, Kotlin in kotlinc\, uv flach -- auf die Ordnerform von $cfg bringen.
                $quelle = switch ($Werkzeug) {
                    'gradle' { Join-Path $entpackt "gradle-$($neu.Version)" }
                    default  { $entpackt }
                }
                if (Test-Path -LiteralPath $zielOrdner) { Remove-Item -LiteralPath $zielOrdner -Recurse -Force }
                New-Item -ItemType Directory -Path $cfg.Wurzel -Force | Out-Null
                Move-Item -LiteralPath $quelle -Destination $zielOrdner
            }
            finally {
                Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
        $geprueft = VersionVon (Join-Path $zielOrdner $cfg.Bin)
        if ($geprueft -ne $neu.Version) { throw "Die entpackte Fassung meldet '$geprueft' statt $($neu.Version)." }

        $vorher = if (Test-Path -LiteralPath $current) { [string]((Get-Item -LiteralPath $current).Target | Select-Object -First 1) } else { '' }
        if (Test-Path -LiteralPath $current) { (Get-Item -LiteralPath $current).Delete() }
        New-Item -ItemType Junction -Path $current -Target $zielOrdner | Out-Null
        Write-Output "current -> $zielOrdner"

        # Aufräumen: die eben aktive und die vorige Fassung bleiben, ältere gehen. Beim ersten Umstieg
        # (noch kein current) bleibt alles liegen -- die bisher genutzte Fassung ist der Rückweg.
        if ($vorher) {
            $behalten = @([IO.Path]::GetFullPath($zielOrdner).TrimEnd('\'), [IO.Path]::GetFullPath($vorher).TrimEnd('\'))
            Get-ChildItem -LiteralPath $cfg.Wurzel -Directory |
                Where-Object { $_.Name -ne 'current' -and ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0 -and $behalten -notcontains $_.FullName.TrimEnd('\') } |
                ForEach-Object { Write-Output "Entferne alte Fassung $($_.Name)"; Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
        }
    }

    PfadAufCurrent
    $aktiv = VersionVon (Join-Path $current $cfg.Bin)
    if ($aktiv -ne $neu.Version) { throw "Nach dem Umschalten meldet current '$aktiv' statt $($neu.Version)." }
    if ($installiert -eq $neu.Version) { Write-Output "ZIPWERKZEUG_STATUS=current $aktiv" } else { Write-Output "ZIPWERKZEUG_STATUS=installed $aktiv" }
    Write-Output 'Neue Terminalfenster finden die Fassung über ...\current; offene Fenster behalten ihren alten PATH.'
    exit 0
}
catch {
    Write-Output "ZIPWERKZEUG_STATUS=failed $($_.Exception.Message)"
    exit 1
}
