# Verbindet das per USB angeschlossene Android-Handy zusätzlich per WLAN mit adb.
# Danach kann das Kabel ab. Ohne Kabel: versucht die zuletzt bekannte IP erneut.
$Port = 5555
$State = Join-Path $env:USERPROFILE ".adb-wlan-last-ip"

$Usb = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "^\S+\s+device$" -and $_ -notmatch ":" } |
    ForEach-Object { ($_ -split "\s+")[0] } | Select-Object -First 1

if ($Usb) {
    adb -s $Usb shell svc wifi enable *> $null
    $Ip = $null
    for ($i = 0; $i -lt 15 -and -not $Ip; $i++) {
        $line = adb -s $Usb shell ip -f inet addr show wlan0 2>$null | Select-String "inet (\d+\.\d+\.\d+\.\d+)"
        if ($line) { $Ip = $line.Matches[0].Groups[1].Value } else { Start-Sleep 2 }
    }
    if (-not $Ip) { Write-Host "Handy hat keine WLAN-IP."; exit 1 }
    Set-Content $State $Ip
    adb -s $Usb tcpip $Port | Out-Null
    Start-Sleep 3
} else {
    $Ip = if (Test-Path $State) { (Get-Content $State).Trim() }
    if (-not $Ip) { Write-Host "Kein USB-Gerät und keine bekannte IP. Einmal per Kabel anschließen."; exit 1 }
}

if (-not ((adb connect "${Ip}:$Port") -match "connected")) {
    adb kill-server; adb start-server | Out-Null; Start-Sleep 1
    adb connect "${Ip}:$Port"
}
adb devices -l
