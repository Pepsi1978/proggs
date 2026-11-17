# Spielmodus-Waechter (Version siehe README.md)
# Laeuft als geplante Aufgabe "Spielmodus-Waechter" im Benutzerkontext mit hoechsten Rechten.
# Solange Steam oder Call of Duty laeuft, werden Microsoft Store und Gaming Services (Shader-
# und Spiel-Downloads ueber die Xbox-App) gestoppt und gesperrt. Danach wird alles auf die
# gesicherten Starttypen zurueckgestellt.
# Ausschalten per Doppelklick auf "Spielmodus an-aus" (Flag-Datei aus.flag).

$ErrorActionPreference = 'Continue'
$daten  = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$log    = Join-Path $daten 'waechter.log'
$sicher = Join-Path $daten 'starttypen.json'
$flagAus = Join-Path $daten 'aus.flag'
New-Item -ItemType Directory -Force $daten | Out-Null

$ausloeser = '^(steam|cod|cod\d\d-cod|sp\d\d-cod|bootstrapper)$'
$dienste   = 'GamingServices', 'GamingServicesNet', 'InstallService'
# GamingServices/-Net sind Paket-Dienste: Starttyp nicht aenderbar (Zugriff verweigert), nur stoppbar
$sperrbar  = 'InstallService'
$stopProz  = 'WinStore.App', 'XboxPcApp', 'XboxPcAppFT', 'gamingservices', 'gamingservicesnet'

function Write-Log([string]$t) {
    $z = '{0:dd.MM.yyyy HH:mm:ss}  {1}' -f (Get-Date), $t
    Add-Content -Path $log -Value $z -Encoding utf8
    if ((Get-Item $log).Length -gt 2MB) { Move-Item $log "$log.alt" -Force }
}

Add-Type @"
using System; using System.Text; using System.Runtime.InteropServices;
public class SpielmodusFenster {
  public delegate bool EnumProc(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc p, IntPtr l);
  [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
}
"@

function Get-SichtbareFenster {
    $liste = New-Object System.Collections.Generic.List[string]
    [SpielmodusFenster]::EnumWindows({ param($h, $l)
        if ([SpielmodusFenster]::IsWindowVisible($h)) {
            $sb = New-Object System.Text.StringBuilder 300
            [void][SpielmodusFenster]::GetWindowText($h, $sb, 300)
            if ($sb.Length -gt 0) {
                $p = 0; [void][SpielmodusFenster]::GetWindowThreadProcessId($h, [ref]$p)
                $n = (Get-Process -Id $p -ErrorAction SilentlyContinue).ProcessName
                $liste.Add("$n | $($sb.ToString())")
            }
        }
        return $true }, [IntPtr]::Zero) | Out-Null
    return $liste
}

function Save-Starttypen {
    if (Test-Path $sicher) { return }
    $s = @{}
    foreach ($d in $dienste) {
        $svc = Get-Service $d -ErrorAction SilentlyContinue
        if ($svc) { $s[$d] = "$($svc.StartType)" }
    }
    $s | ConvertTo-Json | Set-Content $sicher -Encoding utf8
    Write-Log "Starttypen gesichert: $($s | ConvertTo-Json -Compress)"
}

function Enable-Sperre {
    foreach ($d in $dienste) {
        if ($d -in $sperrbar) { try { Set-Service $d -StartupType Disabled -ErrorAction Stop } catch { Write-Log "Starttyp $d nicht setzbar: $($_.Exception.Message)" } }
        try { Stop-Service $d -Force -ErrorAction Stop } catch { Write-Log "Stop ${d}: $($_.Exception.Message)" }
    }
    Stop-Process -Name $stopProz -Force -ErrorAction SilentlyContinue
}

function Disable-Sperre {
    $s = if (Test-Path $sicher) { Get-Content $sicher -Raw | ConvertFrom-Json } else { $null }
    foreach ($d in $sperrbar) {
        $typ = if ($s -and $s.$d) { $s.$d } else { 'Manual' }
        try { Set-Service $d -StartupType $typ -ErrorAction Stop } catch { Write-Log "Rueckstellen ${d}: $($_.Exception.Message)" }
    }
    Start-Service GamingServices, GamingServicesNet -ErrorAction SilentlyContinue
}

Write-Log "Waechter gestartet (PID $PID)"
Save-Starttypen
$gesperrt = $false
$steamLief = [bool](Get-Process steam -ErrorAction SilentlyContinue)
$codSeit = $null

function Show-Meldung([string]$t) {
    $pwsh = (Get-Command pwsh).Source
    $skript = Join-Path $PSScriptRoot 'meldung.ps1'
    Start-Process "$env:WINDIR\System32\conhost.exe" -ArgumentList "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`" -Text `"$t`"" -WindowStyle Hidden
}
$fensterBekannt = @{}

while ($true) {
    $aus = Test-Path $flagAus
    $laeuft = @(Get-Process | Where-Object { $_.ProcessName -match $ausloeser })
    $soll = (-not $aus) -and $laeuft.Count -gt 0

    $steamJetzt = [bool]($laeuft | Where-Object ProcessName -eq 'steam')
    if ($steamJetzt -and -not $steamLief) {
        if ($aus) { Show-Meldung 'Steam gestartet: Spielmodus ist AUS. Store und Xbox-Downloads sind nicht gesperrt.' }
        else      { Show-Meldung 'Steam gestartet: Spielmodus ist AN. Store und Xbox-Downloads sind gesperrt.' }
        Write-Log ("Steam-Start gemeldet (Spielmodus " + $(if ($aus) { 'AUS' } else { 'AN' }) + ')')
    }
    $steamLief = $steamJetzt

    if ($soll -and -not $gesperrt) {
        Write-Log "Sperre AN (Ausloeser: $(($laeuft.ProcessName | Sort-Object -Unique) -join ', '))"
        Enable-Sperre; $gesperrt = $true
    } elseif (-not $soll -and $gesperrt) {
        Write-Log ("Sperre AUS (" + $(if ($aus) { 'von Hand ausgeschaltet' } else { 'kein Spiel/Steam mehr' }) + ')')
        Disable-Sperre; $gesperrt = $false
    }

    if ($gesperrt) {
        $rest = @(Get-Process -Name $stopProz -ErrorAction SilentlyContinue)
        if ($rest.Count -gt 0) {
            Write-Log "Wieder gestartet und beendet: $(($rest.ProcessName | Sort-Object -Unique) -join ', ')"
            Enable-Sperre
        }
    }

    # Fenster-Protokoll in den ersten 90 Sekunden nach CoD-Start (Diagnose Absturz-/Store-Fenster)
    $cod = @($laeuft | Where-Object { $_.ProcessName -ne 'steam' })
    if ($cod.Count -gt 0 -and -not $codSeit) { $codSeit = Get-Date; $fensterBekannt = @{}; Write-Log 'CoD-Start erkannt, protokolliere neue Fenster 90 s' }
    if ($cod.Count -eq 0) { $codSeit = $null }
    if ($codSeit -and ((Get-Date) - $codSeit).TotalSeconds -lt 90) {
        foreach ($f in Get-SichtbareFenster) {
            if (-not $fensterBekannt.ContainsKey($f)) { $fensterBekannt[$f] = 1; Write-Log "Fenster: $f" }
        }
        Start-Sleep -Seconds 1
    } else {
        Start-Sleep -Seconds 4
    }
}
