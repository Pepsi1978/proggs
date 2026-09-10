# Android-Debug-Signierung: ein Key für alle Apps und Rechner

Stand: 10.09.2026

## Regel

- Alle eigenen Android-Apps signieren Debug- (und private Release-)Builds mit **einem** Key:
  `~/SK/Android/debug-shared.keystore`, SHA-256 `F7:82:13:1C:43:62:7B:CA:C4:87:27:97:7D:FF:7E:F7:F1:37:E4:FF:9A:21:C1:7F:84:0B:83:E8:64:B7:FD:B2`,
  Alias `androiddebugkey`, Store- und Key-Passwort `android`, gültig bis 2056.
- OpenLauncher (Windows `Services/AndroidDebugKeystoreSync.cs`, Mac `AndroidDebugKeystoreSync.swift`) legt ihn
  beim Start nach `~/.android/debug.keystore`. AGP nimmt diese Datei für jeden Debug-Build ohne eigene
  `signingConfig`, also brauchen neue Projekte **nichts** zu konfigurieren.
- Neue Projekte: keinen eigenen Keystore erzeugen, keine Debug-`signingConfig` erfinden.
- Ausnahme: BestJournalAndroid-Release (`~/SK/BestJournalAndroid/release.keystore`, Play Store). Nie anfassen.
- Sync zwischen Rechnern: `Y:\Keystores\Android\` (Kopie von `~/SK/Android/`).

## `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: nie deinstallieren

Deinstallieren löscht die App-Daten. Stattdessen den Signierschlüssel per APK-Signature-Scheme-v3-Rotation
umziehen (Android 9+, auf dem Fold getestet am 10.09.2026). Man braucht den **alten** Key, mit dem die App auf
dem Handy signiert ist. OpenLauncher sichert abweichende Keys automatisch als
`~/.android/debug.keystore.<Rechner>.<Zeit>.bak` (Passwort `android`).

```powershell
$as  = "$env:LOCALAPPDATA\Android\Sdk\build-tools\37.0.0\apksigner.bat"
$old = "$HOME\.android\debug.keystore.<Rechner>.<Zeit>.bak"
$new = "$HOME\SK\Android\debug-shared.keystore"
# 1. Lineage alt -> neu (einmal pro altem Key)
& $as rotate --out lineage.bin --old-signer --ks $old --ks-pass pass:android --new-signer --ks $new --ks-pass pass:android
# 2. Installiertes APK ziehen (nur Einzel-APK; bei Splits alle Teile signieren)
adb pull ((adb shell pm path <paket>) -replace 'package:','') app.apk
# 3. Mit Lineage neu signieren und drüberinstallieren - Daten bleiben
& $as sign --ks $old --ks-pass pass:android --next-signer --ks $new --ks-pass pass:android --lineage lineage.bin --rotation-min-sdk-version 28 --out app-rot.apk app.apk
adb install -r app-rot.apk
```

Danach akzeptiert das Gerät Updates, die nur mit dem gemeinsamen Key signiert sind (normaler Gradle-Build).
Welcher Key auf dem Gerät gilt: APK ziehen, `apksigner verify --print-certs`.

## Umgezogen am 10.09.2026 (alter CODI-Key `17:10:34:C5…` → gemeinsamer Key)

CodexKompass, StackLaborWerftStudio, GenialeIdeen, KarteikartenLernen, Denknotiz, Experimente, PerfectMoment,
QwenTtsBench, FisetinBegleiter. Alter Key + Lineage liegen nur noch lokal auf CODI in
`~/SK-Archiv-20260910/Android/alt/` (nicht in SK, nicht auf Y:).
Schon vorher auf dem gemeinsamen Key: CortexAndroid, Gedankenspeicher, ClaudeKompass, VoiceKey,
EntropieReductor, BestJournalFrank, BestJournalAndroid-Debug, NEMS. Seit 10.09.2026 lesen alle acht ihn aus
`~/SK/Android/` statt aus eigenen Kopien in `~/SK/<App>/`.
