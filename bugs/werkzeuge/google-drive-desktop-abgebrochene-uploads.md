# Google Drive für Desktop (macOS): abgebrochene APK-Uploads

Stand: 30.10.2026, beobachtet auf macOS mit Drive für Desktop (File Provider).

**Symptom:** UpdateStation meldet „Update-Ordner … nicht stimmig“. `update.json` nennt eine APK, die im Ordner fehlt.

**Befund:** Die APK liegt unter `~/Library/Application Support/Google/DriveFS/canceled_uploads/<konto>/`, teils auch unter
`lost_and_found/<konto>/`, je mit einer `*_data.txt` samt Ursprungspfad. Im `Logs/drive_fs.txt` stehen zur selben Zeit
`UNAVAILABLE_WHILE_OFFLINE` („Network error Cleared request“) und `CancelPendingUpload`, obwohl das Netz sonst erreichbar ist.
Der genaue Auslöser des Abbruchs ist offen.

**Lösung:** Sha256 der Datei gegen `update.json` prüfen und sie erst dann unter ihrem Namen zurück in den Projektordner legen.
Das Skript `apk-update` meldet lokal `ok`, sobald die Datei im Ordner liegt, und prüft den Upload nicht.
