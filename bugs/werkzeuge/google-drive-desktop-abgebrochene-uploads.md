# Google Drive für Desktop (macOS): abgebrochene APK-Uploads

Stand: 30.10.2026, Google Drive für Desktop 133.0.2.0 (File Provider), macOS 27.2.

**Symptom:** UpdateStation meldet „Update-Ordner … nicht stimmig“. `update.json` nennt eine APK, die im Ordner fehlt. Betroffen sind wechselnd viele Apps: Am 29. und 30.10.2026 stehen 27 abgebrochene APKs im Protokoll (unter anderem Jarvis, BestJournal, Gedankenspeicher, EntropieReductor, die Kompass-Apps, PerfectMoment).

**Ablauf laut `Logs/drive_fs.txt`:**
1. Der APK-Upload läuft in Zeitüberschreitungen: `UNAVAILABLE_WHILE_OFFLINE`, „Network error Cleared request“, `is_client_timeout_error: true`.
2. Nach Wiederholungen bricht der File Provider die Anfrage ab: `RequestCancellation` → `CancelPendingUpload`.
3. Drive kopiert die APK nach `~/Library/Application Support/Google/DriveFS/canceled_uploads/<konto>/` (teils `lost_and_found/<konto>/`), Protokoll in `cancel_upload_data.txt`, und **löscht sie aus dem Drive-Ordner**.
4. Die kleine `update.json` kommt trotzdem an, das Manifest zeigt auf eine fehlende APK.

Warum die Anfragen in Zeitüberschreitungen laufen, ist offen. Das Netz war sonst erreichbar.

**Falsches Muster:** Datei in den Drive-Ordner legen und danach `ok` melden. Der lokale Ordner belegt keinen Upload.

**Upload-Beleg:** `NSURLUbiquitousItemIsUploadedKey` und `...IsUploadingKey` wechseln auch beim File Provider verlässlich von „lädt“ auf „hochgeladen“ (am 30.10.2026 an einer 65-MB-APK beobachtet). Die Abfrage lädt nichts herunter.

**Lösung:** `apk-update.ps1` schreibt `update.json` erst nach dem Upload-Beleg der APK, legt abgebrochene Dateien bis zu dreimal neu ab und lässt ohne Beleg den alten Stand gültig. Bestehende Fälle: `apk-update.ps1 -Projekt Alle -Reparieren`. Es stellt eine APK nur mit exakt der Prüfsumme aus `update.json` wieder her.

**Grenze:** Unter Windows und im Cloud-Bau greift der Beleg nicht. Der Cloud-Bau lädt per `rclone copyto` synchron erst die APK, dann `update.json`. Windows meldet `APK_UPDATE_WARNUNG=Upload hier nicht prüfbar`; dort bleibt UpdateStation die Absicherung (keine Installation ohne APK und passende Prüfsumme).
