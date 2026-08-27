<#
.SYNOPSIS
    Macht die Cortex-Root-CA auf diesem Windows-Rechner vertrauenswuerdig (Chrome zeigt dann ein Schloss).

.DESCRIPTION
    Das Cockpit laeuft hinter Caddy per HTTPS auf https://10.8.0.1, das Zertifikat kommt aber von
    Caddys EIGENER interner CA (bewusst kein Let's Encrypt — kein oeffentlicher DNS-Name, keine
    offenen Ports; siehe bugs/server/reverse-proxy-tls.md §1). Diese CA kennt kein Browser ab Werk
    -> Chrome schreibt "Nicht sicher". Nach diesem Skript kennt Windows sie.

    Idempotent: beliebig oft ausfuehrbar. Holt die CA frisch vom Server, wenn SSH klappt, sonst
    nimmt es die Sicherungskopie aus ~\SK\second-brain\.

    Der Import geht in den Speicher des BENUTZERS (Cert:\CurrentUser\Root) — kein Admin noetig.
    Chrome liest diesen Speicher mit.

.EXAMPLE
    pwsh -NoProfile -File .\windows\install-cortex-ca.ps1
#>
[CmdletBinding()]
param(
    [string]$VpsIp  = '168.231.83.205',
    [string]$WgIp   = '10.8.0.1',
    [string]$SshKey = "$HOME\SK\second-brain\id_ed25519",
    [string]$CaFile = "$HOME\SK\second-brain\caddy-root-ca.crt"
)

$ErrorActionPreference = 'Stop'
Write-Host '== Cortex Root-CA einrichten ==' -ForegroundColor Cyan

# 1) CA frisch holen (nur wenn SSH klappt) — sonst mit der Sicherungskopie weiterarbeiten.
$sshOk = $false
try {
    & ssh -o BatchMode=yes -o ConnectTimeout=8 -i $SshKey "root@$VpsIp" 'true' 2>$null | Out-Null
    $sshOk = ($LASTEXITCODE -eq 0)
} catch { $sshOk = $false }

if ($sshOk) {
    $tmp = [System.IO.Path]::GetTempFileName()
    & ssh -o BatchMode=yes -i $SshKey "root@$VpsIp" `
        'docker exec sb-caddy cat /data/caddy/pki/authorities/local/root.crt' 2>$null |
        Set-Content -Path $tmp -Encoding ascii
    if ((Get-Content $tmp -Raw) -match 'BEGIN CERTIFICATE') {
        Copy-Item $tmp $CaFile -Force
        Write-Host "  CA frisch vom Server geholt -> $CaFile"
    } else {
        Write-Warning '  Abholen fehlgeschlagen, nehme vorhandene Kopie.'
    }
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
} else {
    Write-Host '  Server per SSH nicht erreichbar (Full-Tunnel-VPN an?) — nehme vorhandene Kopie.'
}

if (-not (Test-Path $CaFile)) { throw "Keine CA-Datei unter $CaFile." }

$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2 $CaFile
Write-Host "  Fingerabdruck: $($cert.Thumbprint)"
Write-Host "  Gueltig bis:   $($cert.NotAfter)"

# 2) Alte Kopien derselben CA entfernen (sonst sammeln sich Duplikate an).
Get-ChildItem Cert:\CurrentUser\Root |
    Where-Object { $_.Subject -like '*Caddy Local Authority*' -and $_.Thumbprint -ne $cert.Thumbprint } |
    ForEach-Object {
        Write-Host "  alte CA-Kopie entfernt ($($_.Thumbprint))"
        Remove-Item "Cert:\CurrentUser\Root\$($_.Thumbprint)" -Force
    }

# 3) Importieren (idempotent: gleicher Thumbprint = kein Duplikat).
if (-not (Test-Path "Cert:\CurrentUser\Root\$($cert.Thumbprint)")) {
    Import-Certificate -FilePath $CaFile -CertStoreLocation Cert:\CurrentUser\Root | Out-Null
    Write-Host '  CA importiert und als vertrauenswuerdig markiert.'
} else {
    Write-Host '  CA war bereits vertrauenswuerdig — nichts zu tun.'
}

# 4) Beweis antreten: echte TLS-Pruefung OHNE Zertifikats-Ausnahme.
try {
    $r = Invoke-WebRequest -Uri "https://$WgIp/" -UseBasicParsing -TimeoutSec 10
    Write-Host "  PRUEFUNG OK: https://$WgIp antwortet mit HTTP $($r.StatusCode), Zertifikat wird akzeptiert." -ForegroundColor Green
    Write-Host 'CORTEX_CA_OK'
} catch {
    Write-Host "  PRUEFUNG: https://$WgIp nicht erreichbar (WireGuard-Tunnel aus?) — Import ist trotzdem gesetzt."
}

Write-Host ''
Write-Host "Chrome benutzt ab sofort: https://$WgIp  (nicht mehr http://$WgIp`:8003)"
Write-Host 'Chrome muss einmal komplett neu gestartet werden, damit er den Zertifikatspeicher neu liest.'
