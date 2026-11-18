# Shared helpers (dot-sourced by waechter.ps1, notfall.ps1 and zurueck.ps1).
# One place for "undo the game firewall", so the three callers can never drift apart.

function Get-WlanName {
    # Physical Wi-Fi adapter (NdisPhysicalMedium 9 = Native 802.11), independent of its display name.
    $a = Get-NetAdapter -Physical -ErrorAction SilentlyContinue | Where-Object { $_.NdisPhysicalMedium -eq 9 } | Select-Object -First 1
    if ($a) { return $a.Name }
    return $null
}

function Save-JsonAtomar($objekt, [string]$datei) {
    # Write to a temp file, then replace: a power loss never leaves a half-written restore file.
    $tmp = "$datei.tmp"
    $objekt | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $tmp -Encoding utf8 -ErrorAction Stop
    Move-Item -LiteralPath $tmp -Destination $datei -Force -ErrorAction Stop
}

function Restore-SpielFirewall([string]$datei) {
    # Undoes everything the game firewall changed, using the saved state in $datei (firewall.json).
    # Returns @{ Ok; Meldung }. Ok = $false only if outbound could not be set back to allow; then the
    # file is kept so the caller can retry. Without the file nothing is touched (not our change).
    if (-not (Test-Path -LiteralPath $datei)) { return [pscustomobject]@{ Ok = $true; Meldung = '' } }
    $v = $null
    try { $v = Get-Content -LiteralPath $datei -Raw | ConvertFrom-Json } catch { }
    $ok = $true; $meld = @()
    foreach ($pr in @('Domain', 'Private', 'Public')) {
        $w = if ($v -and $v.$pr -and "$($v.$pr)" -ne 'Block') { "$($v.$pr)" } else { 'NotConfigured' }
        try { Set-NetFirewallProfile -Name $pr -DefaultOutboundAction $w -ErrorAction Stop }
        catch { $ok = $false; $meld += "Profil ${pr}: $($_.Exception.Message)" }
    }
    # Foreign allow rules back on; rules an app removed meanwhile are skipped, the rest still runs.
    $namen = @($v.Deaktiviert | Where-Object { $_ })
    if ($namen.Count -gt 0) { Enable-NetFirewallRule -Name $namen -ErrorAction SilentlyContinue }
    # Wi-Fi background scans back on (needed for roaming and reconnecting after a drop).
    if ($v -and $v.WlanAuto) {
        $out = netsh wlan set autoconfig enabled=yes interface="$($v.WlanAuto)" 2>&1
        if ($LASTEXITCODE -ne 0) { $meld += "WLAN-Autoconfig nicht wieder an: $out" }
    }
    if ($ok) { Remove-Item -LiteralPath $datei -Force -ErrorAction SilentlyContinue }
    return [pscustomobject]@{ Ok = $ok; Meldung = ($meld -join ' | ') }
}
