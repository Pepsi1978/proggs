# Boot-time safety net, runs as SYSTEM (task "Spielmodus-Notfall", at startup, before any logon).
# No game can be running at boot, so any leftover game-mode state is always wrong here:
# outbound block, disabled foreign rules, Wi-Fi autoconfig off, Drive block, InstallService disabled.
# Restores all of it, so the PC is never left offline - even if the watcher never starts again.
param([Parameter(Mandatory)][string]$Daten)
. (Join-Path $PSScriptRoot 'gemeinsam.ps1')
$log = Join-Path $Daten 'notfall.log'
function L([string]$t) { try { Add-Content -LiteralPath $log ('{0:dd.MM.yyyy HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8 } catch { } }

$datei = Join-Path $Daten 'firewall.json'
if (Test-Path -LiteralPath $datei) {
    # The firewall service may still be starting right after boot: retry for up to 2 minutes.
    for ($i = 0; $i -lt 12; $i++) {
        $r = Restore-SpielFirewall $datei
        if ($r.Ok) { L "Spiel-Firewall nach Neustart zurückgestellt $($r.Meldung)"; break }
        L "Zurückstellen fehlgeschlagen, neuer Versuch: $($r.Meldung)"
        Start-Sleep -Seconds 10
    }
}
Remove-NetFirewallRule -DisplayName 'Spielmodus: Google Drive gesperrt' -ErrorAction SilentlyContinue

if ((Get-Service InstallService -ErrorAction SilentlyContinue).StartType -eq 'Disabled') {
    $typ = 'Manual'
    try { $t = (Get-Content -LiteralPath (Join-Path $Daten 'starttypen.json') -Raw | ConvertFrom-Json).InstallService; if ($t -and $t -ne 'Disabled') { $typ = $t } } catch { }
    try { Set-Service InstallService -StartupType $typ -ErrorAction Stop; L "InstallService zurück auf $typ" } catch { L "InstallService: $($_.Exception.Message)" }
}
