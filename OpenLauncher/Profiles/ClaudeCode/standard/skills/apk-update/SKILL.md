---
name: apk-update
description: >
  Baut nach einem Update einer bestehenden Android-App die Update-APK (Release-Build, signiert mit
  dem gemeinsamen Debug-Key bzw. der passenden Variante), prüft Paket, versionCode und Signatur an
  der fertigen APK und legt sie zusammen mit einer update.json in den Google-Drive-Ordner
  "C:\Users\barwa\Meine Ablage\Dokumente\Updates\<Projekt>". Von dort holt die Handy-App
  UpdateStation die APK, vergleicht die Version mit der installierten und installiert nur Neueres.
  Nutze diesen Skill IMMER wenn der Benutzer sagt: "apk update", "starte den skill apk update",
  "starte apk update", "APK-Update", "leg die APK in den Update-Ordner", "Update-APK bereitstellen",
  "APK fürs Handy bereitstellen", "schieb das Update in den Updates-Ordner". Typisch per
  Fernwartung direkt nach einem Update-Auftrag an eine App.
invocation: user
---

# APK Update — Update-APK in den Drive-Ordner legen

## Zweck

Frank gibt per Fernwartung Update-Aufträge an die CLI und sagt danach „starte den Skill apk update".
Die fertige APK landet in `Meine Ablage\Dokumente\Updates\<Projekt>\`, Google Drive synchronisiert
sie, und die Handy-App **UpdateStation** (`~/proggs/UpdateStation`) meldet „Update für App X
gefunden" und installiert nur, wenn der `versionCode` wirklich höher ist als der installierte.

## Ablauf

1. **Projekt bestimmen.** Das Projekt, das in dieser Sitzung gerade aktualisiert wurde, oder das
   im Aufruf genannte (z. B. „apk update GenialerWecker"). Ist es unklar: Unterordner von
   `Updates` auflisten und fragen.
2. **Update muss fertig sein.** `git -C ~/proggs status -sb -- <Projekt>` prüfen. Offene
   Änderungen am Projekt zuerst nach Regel 9 abschließen (bauen, Version bumpen, committen,
   pushen). Nie halbfertigen Code als Update ausliefern.
3. **Skript starten** (Gradle braucht länger als das Standard-Zeitlimit → `timeout: 600000`):
   ```powershell
   pwsh -NoProfile -File "C:\Users\barwa\proggs\OpenLauncher\Profiles\ClaudeCode\standard\skills\apk-update\scripts\apk-update.ps1" -Projekt <Projekt>
   ```
   Das Skript
   - liest `projekte.json` (Gradle-Task, APK-Ordner, erwartetes Paket),
   - vergleicht den `versionCode` aus `app/build.gradle.kts` mit dem zuletzt veröffentlichten
     (`update.json` im Zielordner) und **hebt ihn an, falls er nicht höher ist**,
   - baut, signiert unsignierte APKs mit `~/SK/Android/debug-shared.keystore`,
   - liest Paket, versionCode, versionName und Signatur **aus der fertigen APK** (aapt2, apksigner)
     und bricht ab, wenn Paket oder Version nicht passen,
   - kopiert die APK als `<Projekt>-<versionName>-vc<versionCode>.apk`, löscht ältere APKs im
     Ordner und schreibt zuletzt `update.json`.
4. **Ausgabe auswerten** (Zeilen mit `APK_UPDATE_`):
   - `APK_UPDATE_STATUS=ok` → fertig.
   - `APK_UPDATE_VERSIONCODE_ANGEHOBEN=a->b` → `app/build.gradle.kts` wurde geändert. Sofort
     committen und pushen (nur mit Pfad, siehe Regel 10):
     `git -C ~/proggs commit -m "<Projekt>: versionCode für APK-Update anheben" -- <Projekt>/app/build.gradle.kts`,
     dann `git -C ~/proggs pull --rebase --autostash` und `git -C ~/proggs push`.
   - `APK_UPDATE_STATUS=fehler` → `APK_UPDATE_FEHLER` lesen, Ursache beheben (z. B. falsche
     Variante in `projekte.json`, roter Build), erneut starten. Nie eine APK von Hand in den
     Ordner kopieren — ohne passende `update.json` sieht die Handy-App sie nicht.
5. **Neues Projekt?** Fehlt es in `projekte.json`, dort mit `paket` (applicationId der
   ausgelieferten Variante) nachtragen, den Skill committen und pushen.

## Format von update.json (Vertrag mit der Handy-App — nur erweitern, nie umbenennen)

```json
{
  "format": 1,
  "projekt": "FisetinBegleiter",
  "paket": "de.frank.fisetinbegleiter",
  "versionCode": 30,
  "versionName": "1.0.29",
  "versionStand": "23.09.2026, 14:05 Uhr",
  "apk": "FisetinBegleiter-1.0.29-vc30.apk",
  "groesse": 8123456,
  "sha256": "…",
  "signaturSha256": "f782131c…",
  "variante": "release",
  "erstelltAm": "23.09.2026 14:10",
  "erstelltAmIso": "2026-09-23T14:10:00+02:00"
}
```

Die Handy-App vergleicht **nur `versionCode`** (streng größer) — `versionName` ist nicht sortierbar
(drei Kompass-Apps haben `0.6.11`, QwenTtsBench `1.04.0`). Zusätzlich prüft sie SHA-256 der Datei
und dass die Signatur der APK zur installierten App passt.

## Sonderfälle

- **BestJournalAndroid**: Release ist mit dem Play-Store-Key signiert. Passt nur, wenn am Handy die
  Release-Variante installiert ist; sonst meldet die Handy-App „Signatur weicht ab".
- **EntropieReductor**: Variante `benchmark` (Paket mit `.debug`), ersetzt die Debug-App ohne
  Datenverlust.
- **GenialeIdeen**: Variante `schnell`.
- **BestJournalFrank, VoiceKey**: Am Handy ist nur die Debug-Variante (`.debug`) installiert, daher
  liefert der Skill hier `assembleDebug` aus.
- Welche Variante am Handy liegt, zeigt `adb shell pm list packages`. Passt das Paket nicht, zeigt
  die Handy-App die APK als „nicht auf diesem Handy".
- Meldet die Handy-App „Signatur weicht ab": nie deinstallieren (Regel 15), sondern Variante in
  `projekte.json` korrigieren.

## Abschluss melden

Projekt, Version alt → neu (versionCode), Pfad der APK, ob `build.gradle.kts` angehoben und
committet wurde. Die Handy-App findet das Update bei der nächsten Prüfung (alle 30 Minuten oder
über „Jetzt prüfen").
