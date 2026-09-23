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
   - prüft den Projektnamen (nur Buchstaben, Ziffern, `_ . -`, Ordner direkt unter `~/proggs`)
     und übernimmt die Schreibweise vom Datenträger,
   - prüft **vor jeder Versionsentscheidung** den Git-Stand: keine offenen Änderungen unter
     `<Projekt>` (Index und Arbeitsbaum), `fetch` erfolgreich, HEAD nicht hinter origin, keine
     ungepushten Commits am Projekt — sonst `fehler`,
   - entscheidet mit N = Versionslog, I = am Handy installiert (0 ohne adb), P = zuletzt
     veröffentlicht:
     - P kommt aus `update.json`. **Unlesbares** Manifest: P = höchste `-vcX.apk` im Ordner
       (konservativ, nie dieselbe Nummer erneut). **Fehlendes** Manifest oder älteres Manifest:
       eine APK mit genau vcN ist ein abgebrochener eigener Lauf und wird ersetzt
       (`APK_UPDATE_WARNUNG=verwaiste … wird ersetzt`), sofern Name, Paket und vc passen —
       sonst `fehler` („fremde Datei“); alle anderen verwaisten APKs zählen weiter.
     - **Veröffentlicht, wenn N > P und N ≥ I** (N = I ist erlaubt, z. B. nach adb-Installation).
     - Nennt `update.json` schon N und ist alles unverändert (APK-Hash, Manifest-Commit auflösbar,
       kein Projekt-Diff seit diesem Commit, I ≤ N) → `already-current`, ohne Build.
     - Sonst **hängt das Skript selbst einen Eintrag an** (versionCode = max(P, I) + 1,
       versionName + 1 in der letzten Stelle, Stand = jetzt, Notiz = Commit-Betreffe seit der
       letzten Änderung am Versionslog). Die erste Veröffentlichung braucht keinen Extra-Bump,
   - **veröffentlicht nie, was es selbst geändert hat:** Hat es den Versionslog eingerichtet oder
     ergänzt, stoppt es mit `APK_UPDATE_STATUS=vorbereitet` (Exit 2), ohne zu bauen,
   - läuft pro Projekt exklusiv (Sperrdatei, eindeutige Temp-Dateien pro Lauf); ein zweiter Lauf
     für dasselbe Projekt bricht mit Fehler ab,
   - baut, nimmt die APK **eindeutig aus `output-metadata.json`** des Ausgabeordners (genau ein
     Element, Paket und versionCode müssen passen; APK-Splits werden nicht unterstützt) und
     signiert unsignierte APKs mit `~/SK/Android/debug-shared.keystore`,
   - fragt das Handy nur lesend über dieselbe adb wie das WLAN-Werkzeug ab (`$env:ADB`, sonst
     SDK-adb); `-OhneGeraet` (nur für Tests) überspringt das,
   - liest Paket, versionCode, versionName und Signatur **aus der fertigen APK** (aapt2, apksigner)
     und bricht ab, wenn Paket oder Version nicht passen,
   - legt die APK als `<Projekt>-<versionName>-vc<versionCode>.apk` ab, danach `update.json`
     (inkl. der letzten 15 Versionslog-Einträge für „Neu in dieser Version" in UpdateStation).
     Beide entstehen außerhalb des Drive-Ordners und kommen per Umbenennen hinein: Lokal ist so
     nie eine halbe Datei sichtbar. Wann und in welcher Reihenfolge Google Drive für Desktop
     hochlädt, sichert das **nicht** — Zwischenstände fängt UpdateStation ab (Nachprüfung).
     Hash und Manifest werden zurückgelesen, erst dann meldet es `ok` und räumt auf: **die 5
     neuesten APKs** bleiben, dazu immer die bisher referenzierte.
4. **Ausgabe auswerten** (Zeilen mit `APK_UPDATE_`):
   - `APK_UPDATE_STATUS=already-current` (Exit 0) → schon veröffentlicht und unverändert;
     nichts zu tun, **kein Commit** (typisch bei einer Wiederholung per Fernwartung).
   - `APK_UPDATE_STATUS=ok` → **lokal bereit** (`APK_UPDATE_BEREIT=lokal`): Die Dateien liegen im
     Drive-Ordner, den Upload übernimmt Google Drive für Desktop. Nicht als „in der Cloud“ melden.
   - `APK_UPDATE_STATUS=vorbereitet` (Exit 2) → **nichts veröffentlicht.** Das Skript hat
     Projektdateien vorbereitet, `APK_UPDATE_COMMIT_NOETIG` nennt sie:
     - `APK_UPDATE_VERSIONSLOG_EINGERICHTET=vcN (<log>, <gradle>)`: neues Projekt auf den
       Versionslog umgestellt (bestehende Nummer, kein Bump),
     - `APK_UPDATE_VERSIONSLOG_ERGAENZT=a->b (<pfad>)`: Versionslog um einen Eintrag ergänzt.

     Diese Pfade zusammen mit der eigentlichen App-Änderung committen (genau ein Versionsbump,
     **nur mit Pfaden** — im Monorepo liegen fremde Änderungen, nie pauschal stagen), dann
     `git -C ~/proggs pull --rebase --autostash`, `git -C ~/proggs push` und das Skript erneut
     starten. Z. B.:
     `git -C ~/proggs commit -m "<Projekt>: Version für APK-Update anheben" -- <Pfade aus APK_UPDATE_COMMIT_NOETIG>`.
   - `APK_UPDATE_NEUES_PROJEKT=…` → Projekt fehlt in `projekte.json`, läuft mit den Standards.
   - `APK_UPDATE_STATUS=fehler` → `APK_UPDATE_FEHLER` lesen, Ursache beheben (z. B. falsche
     Variante in `projekte.json`, roter Build, offene Änderungen, nicht gepusht, anderer Lauf
     aktiv), erneut starten. Nie eine APK von Hand in den Ordner kopieren — ohne passende
     `update.json` sieht die Handy-App sie nicht.
5. **Neues Projekt?** Das Skript veröffentlicht es auch ohne Eintrag in `projekte.json`
   (Standard `assembleRelease`, Paket = die eine `applicationId`) in einen eigenen Unterordner.
   Fehlt der Versionslog, stellt es nur das eindeutige Standardmuster um (genau eine Zeile
   `versionCode = <Zahl>` und `versionName = "x.y.z"`, genau ein `android {`); alles andere bricht
   mit Anleitung ab — dann den Block `versionslogAktuell` aus `UpdateStation/app/build.gradle.kts`
   von Hand übernehmen. Ein Eintrag in `projekte.json` ist nur nötig, wenn Gradle-Task,
   APK-Ordner oder Paket vom Standard abweichen (z. B. Variante `debug`, mehrere
   `applicationId`); dann nachtragen, den Skill committen und pushen.

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
committet wurde. Die Handy-App findet das Update bei der nächsten Prüfung (Standard alle 30 Minuten oder
über „Jetzt prüfen"); das Prüfintervall stellt man in UpdateStation per Regler ein. Installiert wird
am Handy nur nach eigenem Tippen und Android-Bestätigung.
