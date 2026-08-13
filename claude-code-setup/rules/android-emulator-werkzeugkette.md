# Android-Emulator IMMER ueber die Werkzeugkette starten (KRITISCH)

> Frank: "Die gesamte Kette, die vorher an Werkzeugen eingebaut ist, muss auch immer genutzt
> werden. Immer wenn ich sage: starte den Emulator mit dieser App."

## Die eine Regel
Jeder Emulator-Start laeuft ueber `~/proggs/Werkzeuge/fold8-emulator/Start-Fold8.ps1` —
NIE ueber `emulator.exe` direkt:

`pwsh -File ~/proggs/Werkzeuge/fold8-emulator/Start-Fold8.ps1 -Projekt <Name>`

Baut die App, installiert + startet sie, setzt Originalgroesse (1:1 wie das echte Fold 8),
zentriert das Fenster und fuehrt es per Waechter beim Drehen/Klappen nach.
`-Innen` grosses Display (Standard: zugeklappt/Cover) · `-OhneBauen` · `-Zoom` · `-Apk` · `-Kaltstart`.

## Warum
Der Emulator kennt weder Startgroesse noch Startposition (`-scale` abgeschafft, `-window-size`
nur Fuchsia, `emulator-user.ini` wird beim Beenden ueberschrieben). Nackt gestartet landete das
Fenster 648 px OBERHALB des Bildschirms — Titelleiste nicht mehr greifbar (13.08.2026).
Details: `bugs/android/emulator-foldable.md` #18/#18b.

## Weitere Werkzeuge (gleicher Ordner, README dort)
`Klappen.ps1 -Auf/-Zu` · `Set-Originalgroesse.ps1 [-Ueberwachen]` · `Pruefe-Massstab.ps1` ·
`Zeig-Elemente.ps1` · `Zeig-Fehler.ps1`.

## Was NIEMALS
- `emulator.exe -avd ...` direkt aufrufen (der `emulator-start-guard`-Hook blockt es)
- Fenstergroesse/-position von Hand setzen statt ueber die Kette
- die Kette umgehen "weil es schneller geht"
