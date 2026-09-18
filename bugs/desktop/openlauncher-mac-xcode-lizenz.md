# OpenLauncherMac: Build bricht mit unakzeptierter Xcode-Lizenz ab

Stand: 18.09.2026 — macOS 26 (Darwin 25.6), Xcode-Auswahl `/Applications/Xcode.app/Contents/Developer`

## Symptom
`bash ~/proggs/OpenLauncherMac/update-launcher.sh` schließt den laufenden Launcher, dann:

```
You have not agreed to the Xcode license agreements. Please run 'sudo xcodebuild -license' ...
Der Build ist fehlgeschlagen. Das Update wurde nicht durchgeführt.
```

Der Rechner bleibt ohne laufenden Launcher zurück, weil der Updater vor dem Build beendet.

## Ursache
`build.sh` ruft `swiftc` und `xcrun --show-sdk-path` auf. `xcrun` läuft über das aktive
Developer-Verzeichnis; zeigt das auf Xcode.app, prüft es die Lizenz und bricht ab. Die separat
installierten Command Line Tools haben diese Prüfung nicht.

## Abhilfe (ohne sudo, sofort)
```bash
DEVELOPER_DIR=/Library/Developer/CommandLineTools bash ~/proggs/OpenLauncherMac/update-launcher.sh
```

## Dauerhafte Lösung (nur vom Nutzer, braucht Passwort)
```bash
sudo xcodebuild -license accept
```

## Nicht tun
- Kein `build.sh` als Ersatz: der Updater meldet danach `already-current` und installiert nichts
  (Abhilfe dann `touch OpenLauncherMac/OpenLauncher/Info.plist`).
- Kein `OPENLAUNCHER_UPDATE_FORCE=1`.
