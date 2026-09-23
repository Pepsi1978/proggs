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
   - liest den **Versionslog** `<Projekt>/app/src/main/assets/versionslog.json` (siehe unten) —
     sein letzter Eintrag ist die Version, die gebaut wird,
   - vergleicht dessen `versionCode` mit dem zuletzt veröffentlichten (`update.json` im
     Zielordner) und — falls das Handy per adb erreichbar ist — mit dem am Handy installierten.
     Ist er nicht höher, **hängt das Skript selbst einen Eintrag an** (versionCode + 1,
     versionName + 1 in der letzten Stelle, Stand = jetzt, Notiz = Commit-Betreffe am Projekt seit
     der letzten Änderung am Versionslog). Das Handy ist bei Fernwartung normalerweise NICHT
     verbunden: Beim ersten Update eines Projekts (noch keine `update.json`) hebt das Skript
     deshalb immer um 1 an, danach zählt die `update.json`,
   - baut, signiert unsignierte APKs mit `~/SK/Android/debug-shared.keystore`,
   - liest Paket, versionCode, versionName und Signatur **aus der fertigen APK** (aapt2, apksigner)
     und bricht ab, wenn Paket oder Version nicht passen,
   - kopiert die APK als `<Projekt>-<versionName>-vc<versionCode>.apk`, **behält die 5 neuesten
     APKs** (nach versionCode) im Ordner, löscht ältere und schreibt zuletzt `update.json`
     (inkl. der letzten 15 Versionslog-Einträge für „Neu in dieser Version" in UpdateStation).
4. **Ausgabe auswerten** (Zeilen mit `APK_UPDATE_`):
   - `APK_UPDATE_STATUS=ok` → fertig.
   - `APK_UPDATE_VERSIONSLOG_ERGAENZT=a->b (<pfad>)` → der Versionslog wurde ergänzt. Sofort
     committen und pushen (nur mit Pfad, siehe Regel 10):
     `git -C ~/proggs commit -m "<Projekt>: Version für APK-Update anheben" -- <Projekt>/app/src/main/assets/versionslog.json`,
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
  "erstelltAmIso": "2026-09-23T14:10:00+02:00",
  "commit": "a1b2c3d4e",
  "versionslog": [ { "versionCode": 30, "versionName": "1.0.29", "stand": "23.09.2026, 14:05 Uhr", "notiz": "…" } ]
}
```

## Versionslog (in jeder Android-App)

`app/src/main/assets/versionslog.json`, neuester Eintrag **unten**, ein Eintrag pro Zeile:

```json
{
  "format": 1,
  "app": "FisetinBegleiter",
  "eintraege": [
    { "versionCode": 29, "versionName": "1.0.28", "stand": "07.09.2026, 14:18 Uhr", "notiz": "Stand vor Einführung des Versionslogs" },
    { "versionCode": 30, "versionName": "1.0.29", "stand": "23.09.2026, 15:10 Uhr", "notiz": "Versionslog eingeführt" }
  ]
}
```

- `build.gradle.kts` liest `versionCode`, `versionName` und `VERSION_BUMPED_AT` aus dem letzten
  Eintrag (Block `versionslogAktuell` vor `android {`). Dort nie Versionen von Hand eintragen.
- Die Datei liegt als Asset in der APK: UpdateStation liest sie direkt aus der installierten App
  (`createPackageContext(paket, 0).assets`) und zeigt den Versionsverlauf.
- Regel 6: Jeder Commit an einer App hängt einen Eintrag an (versionCode + 1).
- Neues Projekt: Datei anlegen und den Block aus `UpdateStation/app/build.gradle.kts` übernehmen.

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
