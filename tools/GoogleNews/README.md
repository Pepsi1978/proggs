# Google News als eigener Bildschirm (Discover-Ersatz für Windows)

Öffnet **news.google.com** in einem eigenständigen Fenster ohne Adressleiste und Tabs — wie ein
richtiges Programm. Gedacht als Ersatz für **Google Discover**, den Nachrichtenbildschirm links
neben dem Android-Startbildschirm.

## Warum nicht Discover selbst?

Google Discover gibt es auf Windows nicht. Google hatte den Feed eine Zeit lang auf der
Desktop-Startseite von google.com, hat ihn im **März 2026 aber wieder abgeschafft**. Die offizielle
Google-App für Windows (`Alt + Leertaste`) kann Suche, KI-Modus und Lens — **keinen Discover-Feed**.

Google News ist die nächstbeste offizielle Lösung: derselbe Google-Account, dieselben Interessen,
personalisierter Feed unter „Für dich".

## Benutzung

Doppelklick auf **Google News** — auf dem Desktop oder im Startmenü (auch über die Windows-Suche
zu finden und an die Taskleiste anheftbar).

Das Skript:

1. sucht ein bereits offenes Google-News-Fenster und verwendet es wieder (kein zweites Fenster),
2. startet sonst Chrome im App-Modus (`--app=`) mit dem Standardprofil, damit der Feed über das
   angemeldete Google-Konto personalisiert ist,
3. holt das Fenster aus dem minimierten Zustand,
4. legt es auf den **zweiten Monitor**, falls einer angeschlossen ist, und maximiert es.

## Auf einen eigenen „Bildschirm" legen

**Zweiter Monitor:** Ist einer angeschlossen, macht das Skript das automatisch.

**Zweiter virtueller Desktop** (wenn nur ein Monitor da ist — das kommt dem Wischen am Handy am
nächsten):

1. `Strg + Windows + D` erstellt einen neuen Desktop.
2. Dort Google News starten.
3. Mit `Strg + Windows + Pfeil rechts/links` zwischen den Desktops wechseln — oder mit vier
   Fingern auf dem Touchpad nach links/rechts wischen.

Windows merkt sich, auf welchem Desktop das Fenster liegt.

## Dateien

| Datei | Zweck |
|-------|-------|
| `Google-News.ps1` | Launcher: startet, findet und positioniert das Fenster |
| `google-news.ico` | Icon für die Verknüpfungen |

Die Verknüpfungen selbst liegen außerhalb des Repos:
`%USERPROFILE%\Desktop\Google News.lnk` und
`%APPDATA%\Microsoft\Windows\Start Menu\Programs\Google News.lnk`.

Neu anlegen lassen sie sich mit:

```powershell
$ws = New-Object -ComObject WScript.Shell
$lnk = $ws.CreateShortcut("$([Environment]::GetFolderPath('Desktop'))\Google News.lnk")
$lnk.TargetPath   = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"
$lnk.Arguments    = '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\Users\barwa\proggs\Tools\GoogleNews\Google-News.ps1"'
$lnk.IconLocation = 'C:\Users\barwa\proggs\Tools\GoogleNews\google-news.ico,0'
$lnk.Save()
```

## Voraussetzungen

Google Chrome. Ist Chrome nicht installiert, meldet das Skript das und bricht ab.

## Nur Windows

Dieses Werkzeug ist bewusst Windows-spezifisch (PowerShell, Win32-Fensterfunktionen,
`.lnk`-Verknüpfungen). Auf macOS ginge derselbe Trick mit
`open -na "Google Chrome" --args --app=https://news.google.com/` — bisher nicht gebaut, weil der
Bedarf an diesem Windows-Rechner bestand.
