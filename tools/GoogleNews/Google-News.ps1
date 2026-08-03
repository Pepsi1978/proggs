# Google News als eigenes App-Fenster starten - Discover-Ersatz für Windows.
# Startet news.google.com in einem Chrome-App-Fenster (ohne Adressleiste und Tabs)
# und legt es auf den zweiten Monitor, falls einer angeschlossen ist.

$Version = 'Version 1.0.0 (03.08.2026, 14.31 Uhr)'
$Url     = 'https://news.google.com/'

Write-Host "Google News App-Fenster - $Version"

# --- Chrome finden ------------------------------------------------------------
$chromeKandidaten = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)
$chrome = $chromeKandidaten | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $chrome) {
    Write-Host 'Google Chrome wurde nicht gefunden. Bitte Chrome installieren.' -ForegroundColor Red
    Start-Sleep -Seconds 5
    exit 1
}

# --- Win32-Funktionen fürs Fensterschieben ------------------------------------
if (-not ('GNewsWin32' -as [type])) {
    Add-Type @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public class GNewsWin32 {
    public delegate bool EnumProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool IsZoomed(IntPtr hWnd);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowTextW(IntPtr hWnd, StringBuilder s, int max);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassNameW(IntPtr hWnd, StringBuilder s, int max);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
'@
}

function Get-NewsFenster {
    $global:GNewsHwnd = [IntPtr]::Zero
    $rueckruf = [GNewsWin32+EnumProc] {
        param($hWnd, $lParam)
        if (-not [GNewsWin32]::IsWindowVisible($hWnd)) { return $true }
        $klasse = New-Object System.Text.StringBuilder 256
        [void][GNewsWin32]::GetClassNameW($hWnd, $klasse, 256)
        if ($klasse.ToString() -ne 'Chrome_WidgetWin_1') { return $true }
        $titel = New-Object System.Text.StringBuilder 512
        [void][GNewsWin32]::GetWindowTextW($hWnd, $titel, 512)
        $t = $titel.ToString()
        # App-Fenster tragen kein " - Google Chrome" im Titel, normale Tabs schon.
        if ($t -match 'Google News' -and $t -notmatch 'Google Chrome$') {
            $global:GNewsHwnd = $hWnd
            return $false
        }
        return $true
    }
    [void][GNewsWin32]::EnumWindows($rueckruf, [IntPtr]::Zero)
    return $global:GNewsHwnd
}

# --- Bereits offenes Fenster wiederverwenden, sonst starten --------------------
$hwnd = Get-NewsFenster
if ($hwnd -eq [IntPtr]::Zero) {
    Start-Process -FilePath $chrome -ArgumentList "--app=$Url"
    $wartenBis = (Get-Date).AddSeconds(25)
    while ((Get-Date) -lt $wartenBis) {
        Start-Sleep -Milliseconds 400
        $hwnd = Get-NewsFenster
        if ($hwnd -ne [IntPtr]::Zero) { break }
    }
}

if ($hwnd -eq [IntPtr]::Zero) {
    Write-Host 'Das Google-News-Fenster wurde nicht gefunden - es läuft aber trotzdem.' -ForegroundColor Yellow
    exit 0
}

# --- Auf zweiten Monitor legen und maximieren ----------------------------------
Add-Type -AssemblyName System.Windows.Forms
$bildschirme = @([System.Windows.Forms.Screen]::AllScreens)
$zweiter = $bildschirme | Where-Object { -not $_.Primary } | Select-Object -First 1

$SW_RESTORE  = 9
$SW_MAXIMIZE = 3
$SWP_NOZORDER = 0x0004

# Chrome setzt den Fensterzustand nach dem Laden noch einmal selbst - deshalb
# kurz warten und das Fenster in mehreren Anläufen aus dem minimierten Zustand holen.
Start-Sleep -Milliseconds 1200
for ($i = 0; $i -lt 6; $i++) {
    if (-not [GNewsWin32]::IsIconic($hwnd)) { break }
    [void][GNewsWin32]::ShowWindow($hwnd, $SW_RESTORE)
    Start-Sleep -Milliseconds 400
}

if ($zweiter) {
    $flaeche = $zweiter.WorkingArea
    [void][GNewsWin32]::SetWindowPos($hwnd, [IntPtr]::Zero, $flaeche.X, $flaeche.Y, $flaeche.Width, $flaeche.Height, $SWP_NOZORDER)
    Write-Host "Auf den zweiten Monitor gelegt ($($zweiter.DeviceName))."
} else {
    Write-Host 'Nur ein Monitor aktiv - Fenster wird auf diesem maximiert.'
}

# Chrome stellt nach dem Laden seine gespeicherte Fenstergröße wieder her und
# überschreibt dabei das Maximieren - deshalb mehrfach nachhalten, bis es sitzt.
for ($i = 0; $i -lt 8; $i++) {
    [void][GNewsWin32]::ShowWindow($hwnd, $SW_MAXIMIZE)
    Start-Sleep -Milliseconds 400
    if ([GNewsWin32]::IsZoomed($hwnd)) { break }
}
[void][GNewsWin32]::SetForegroundWindow($hwnd)

if ([GNewsWin32]::IsIconic($hwnd)) {
    Write-Host 'Hinweis: Das Fenster ist noch minimiert - einmal in der Taskleiste anklicken.' -ForegroundColor Yellow
} elseif ([GNewsWin32]::IsZoomed($hwnd)) {
    Write-Host 'Fertig - Google News ist offen und bildschirmfüllend.'
} else {
    Write-Host 'Fertig - Google News ist offen (Fenstergröße von Chrome vorgegeben).'
}
