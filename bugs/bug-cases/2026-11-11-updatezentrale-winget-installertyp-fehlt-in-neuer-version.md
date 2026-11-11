# UpdateZentrale: festgelegter winget-Installer-Typ fehlt in der neuen Version (11.11.2026)

> Behoben in UpdateZentrale 1.3.82. macOS ist nicht betroffen, denn brew kennt keinen festgelegten Installer-Typ.

## Symptom

PowerShell 7 (`wingetInstallerTyp: wix`) endete im Sammellauf mit „Nicht verifiziert“: „Dasselbe Update (7.7.0.0) wird weiter angeboten, obwohl sich der Stand geändert hat (7.6.9.0 → 7.6.10.0)“. `winget install --id Microsoft.PowerShell --exact --force --installer-type wix` meldete „Gefunden … Version 7.6.10.0“ und „Erfolgreich installiert“. `winget list` zeigte danach weiter `7.6.10.0  Verfügbar 7.7.0.0`.

## Ursache

- Das GitHub-Release v7.7.0 (10.11.2026) und sein winget-Manifest enthalten **kein MSI** mehr, nur `PowerShell-7.7.0.msixbundle` und ZIP-Dateien.
- `winget install --installer-type wix` nimmt dann die neueste Version **mit** wix-Installer, hier 7.6.10.0. `winget list` bietet aber die neueste Version **unabhängig vom Typ** an, also 7.7.0.0.
- Die Prüfung las das Angebot nur aus `winget list`. Deshalb wäre das Update nie verifiziert worden, und jeder Sammellauf hätte 7.6.10 per `--force` neu installiert.
- `winget show --version V --installer-type T` endet in beiden Fällen mit Exit-Code 0. Ohne passenden Installer steht dort nur der übersetzte Text „Es wurde kein anwendbarer Installer gefunden“. Mit passendem Installer steht dort eine SHA256-Zeile (64 Hex-Zeichen), und genau daran lässt sich der Fall sprachunabhängig erkennen.
- Ohne Versionsangabe filtert `winget show --installer-type` nicht nach Version: Es zeigt 7.7.0.0 ohne Installer.

## Behebung

- `WingetAktualisierer.AngebotMitInstallerTypAsync`: Ist `wingetInstallerTyp` gesetzt und meldet die Liste ein Update, prüft die UpdateZentrale das angebotene Paket per `winget show … --version <Angebot> --installer-type <Typ>` (über den Hash, `HatInstaller`).
  - Fehlt der Typ, geht sie `winget show --versions` von oben nach unten durch, höchstens 4 Versionen zwischen der installierten Fassung und dem Angebot. Die neueste Version mit diesem Typ wird das Angebot.
  - Ist keine neuere Version mit diesem Typ zu finden, steht die Karte auf „Aktuell“ mit dem Hinweis „7.7.0.0 gibt es nur ohne wix-Installer“.
  - Ist eine Antwort unlesbar (Zeitlimit, Exit ≠ 0, kein `[Id]`), bleibt das Angebot der Liste. Ein Fehler wird nie zu „aktuell“.
  - Die Ergebnisse werden je Id, Typ und Version im Prozess zwischengespeichert. Die Aufrufe laufen in derselben winget-Reihe wie die Listen.
- Katalog: `wingetName` von `PowerShell` auf `PowerShell 7-x64` geändert. Neben dem MSI liegt ein MSIX-Paket `Microsoft.PowerShell 7.6.9.0`, und der alte Name traf beide Zeilen. Kommt das MSIX auf 7.7, hätte die MSI-Karte sonst 7.7 als installiert gelesen.

## Erkennen

Bei „Nicht verifiziert“ mit „dasselbe Update wird weiter angeboten, obwohl sich der Stand geändert hat“ an einer Karte mit `wingetInstallerTyp`: Prüfen, ob die angebotene Version diesen Typ überhaupt hat (`winget show --id X --exact --version V --installer-type T`). Das Update ist dann angekommen; nur das Angebot war falsch.

## Offen

Die MSI-Reihe von PowerShell endet mit 7.6.x (LTS). Ein Wechsel auf 7.7 geht nur über MSIX oder ZIP. Das ist eine Entscheidung, kein Fehler des Update-Wegs: Mehrere Katalogeinträge rufen `C:/Program Files/PowerShell/7/pwsh.exe` fest auf.

## Dateien

- `UpdateZentrale/Providers/WingetAktualisierer.cs`
- `UpdateZentrale/programs.json`
- `UpdateZentrale/README.md`
