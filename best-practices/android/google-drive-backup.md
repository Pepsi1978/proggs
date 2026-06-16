# Google-Drive-Backup & Cloud-Sync — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man in BestJournal (Android, Kotlin) die Room-DB + Fotos robust nach Google Drive
> (appDataFolder) sichert — ohne verwaiste Dateien, ohne Datenverlust bei mehreren Geräten, mit
> sauberem Restore. Lokale DB ist **Source of Truth**.
> **Versions-Anker:** `com.google.apis:google-api-services-drive:v3-rev20241206-2.0.0` · Drive REST
> **API v3** (neues **Quota-Unit-Modell seit 01.05.2026**) · `play-services-auth 21.3.0`
> (`GoogleAuthUtil`/`GoogleSignIn` **deprecated**, Entfernung ab **Mai 2026**) ·
> `androidx.credentials 1.5.0-alpha06` + `googleid 1.1.1` · compileSdk 35, minSdk 26.
> **Gegenstück (was schiefgeht):** [`bugs/android/google-drive-backup.md`](../../bugs/android/google-drive-backup.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Backup wächst endlos (Waisen) | Reconciliation-Sweep: `remote − lokal = Waisen` → trashen; Manifest als Gültig-Liste | §2 |
| 2 | Geänderte Datei wird nicht erkannt | Change-Detection über `md5Checksum`/`sha256Checksum`, NICHT Name+Size | §3 |
| 3 | Sweep zu aggressiv | Max-Delete-Schwelle (z. B. max 20 %); Dry-Run-Log; Abbruch wenn lokale DB verdächtig leer | §2 |
| 4 | 2. Gerät überschreibt neueres Backup | Read-before-write: `headRevisionId`/eigener `dataVersion` prüfen, nur wenn lokal neuer | §4 |
| 5 | Doppelte Backup-Datei | Eine kanonische File-ID merken + `appProperties`-Marker; immer `update`, nie blind `create` | §4 |
| 6 | Versehentliches Überschreiben rückgängig | Drive-Revisionen; kritische Stände `keepForever=true` (vor Auto-Purge nach 30 T/100 Rev.) | §4 |
| 7 | Scope/Verifizierung | `drive.appdata` ist **non-sensitive** → kein CASA; niemals auf vollen `drive` erweitern | §5 |
| 8 | Auth-Code veraltet | `GoogleAuthUtil.getToken` → `Identity.getAuthorizationClient().authorize()` (Frist Mai 2026) | §5 |
| 9 | 403/429 Rate-Limit | Truncated Exponential Backoff **mit Jitter**; Semaphore 5 → 3–4; ~3 Writes/s | §6 |
| 10 | „Speicher voll" 403 | `storageQuotaExceeded` NICHT retryen; appDataFolder zählt gegen 15 GB; vorab `about.get` | §6 |
| 11 | Upload-Duplikate nach Retry | Resumable + **Pre-Generated File-ID** (`files.generateIds`) → Retry gibt 409, kein Duplikat | §7 |
| 12 | Upload bricht bei App-Wechsel ab | Backup in `CoroutineWorker` (WorkManager) statt ViewModel-Scope; FGS-Typ `dataSync` | §8 |
| 13 | Restore korrumpiert DB | Checksum prüfen; `-wal`/`-shm` mit löschen; Room `close()`; atomar tauschen; App-Neustart | §9 |
| 14 | Restore „malformed"/Schema | `quick_check` + Schema-Version auf Temp-Datei VOR dem Swap; kein `fallbackToDestructiveMigration` | §9 |

---

## 1) appDataFolder — Grundlagen & Backup-Layout

`offiziell`

- **Was:** Versteckter Ordner **pro App und Nutzer**; nur deine App sieht den Inhalt, der Nutzer
  sieht ihn nicht in der Drive-UI (nur unter „Apps verwalten" löschbar). Scope `drive.appdata`.
- **CRUD:** `parents=["appDataFolder"]` beim `create`; bei `list`/`get` **immer `setSpaces("appDataFolder")`**
  (sonst leere Treffer) **und `setFields(...)`** (sonst sind `md5Checksum`/`size`/`modifiedTime`/
  `appProperties` null). Paginierung über `nextPageToken`, `pageSize` max 1000.
- **Löschen:** Im appDataFolder ist **kein Papierkorb** möglich (`files.update trashed=true` →
  `notSupportedForAppDataFolderFiles`). `files.delete` löscht **permanent** und gibt Speicher sofort frei.
- **Layout-Empfehlung:** Room-DB als **eine** Datei (`files.update` auf feste ID → neue Revision);
  Fotos einzeln (für echtes Delta) **oder** alles als ZIP-Generation (eliminiert Waisen, siehe §2).
  Stabile **Namens-Präfixe** wegen der `name contains`-Prefix-Falle. Eine **Manifest-Datei**
  (`bestjournal_manifest.json`) mit gültigen Keys + Hashes + Schema-/App-Version + Konto-ID.
- **Wichtig:** Das Drive-Backup ist **kein Ersatz** für die lokale DB — es wird bei App-Deinstallation
  oder Konto-Wechsel gelöscht und ist konto-gebunden. Lokale DB bleibt Source of Truth.
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/appdata · https://developers.google.com/workspace/drive/api/guides/delete

## 2) Orphan-Cleanup / Reconciliation (Kernthema — gegen endloses Wachstum)

`offiziell` (Drive) · `extern` (Sync-Praxis: rsync/Iceberg/Borg)

- **Das Problem:** Ein **upsert-only**-Sync (lädt nur neue/geänderte lokale Dateien hoch) hat keine
  Gegenrichtung — lokal gelöschte Fotos bleiben als Waisen auf Drive → Backup wächst monoton.
- **Lösung A — Reconciliation-Sweep („Mark-and-Sweep"):** Nach jedem Upload-Lauf:
  1. `remote_set` = alle `photo_*` im appDataFolder (`files.list`).
  2. `local_set` = alle Foto-Keys, die laut **lokaler DB** noch existieren (Source of Truth).
  3. `orphans = remote_set − local_set` → diese Drive-Dateien entfernen.
- **Lösung B — Manifest-getrieben:** Eine Manifest-Datei listet alle gültigen Keys; Sweep löscht
  remote alles, was **nicht** im Manifest steht. **Reihenfolge strikt:** neue Dateien hochladen →
  Manifest committen → **erst danach** Waisen löschen (nie umgekehrt).
- **Lösung C — ZIP-Generation:** DB + Fotos als eine ZIP pro Backup; neue ZIP hoch, alte löschen →
  Waisen entstehen by design nicht. Trade-off: kein Delta (immer Voll-Upload).
- **Sicherheitsnetze (Pflicht):**
  - **Max-Delete-Schwelle** (rsync `--max-delete`): Sweep abbrechen, wenn er mehr als X % löschen
    würde — schützt vor Totallöschung bei leerer/kaputter lokaler DB.
  - **Auto-Disable bei I/O-Fehler:** Wenn DB-Lesen/Listing fehlschlägt oder verdächtig leer ist,
    Sweep **nicht** ausführen.
  - **Dry-Run-Log:** Orphan-Liste vor dem Löschen ins Log schreiben.
  - **Soft-Delete statt Hard-Delete:** Waisen erst trashen/per `appProperties.deletedAt` markieren,
    erst beim übernächsten sauberen Lauf hart löschen (Schutz vor einmaligem Sync-Glitch).
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/manage-changes · https://linux.die.net/man/1/rsync · https://iomete.com/resources/blog/iceberg-maintenance-runbook

## 3) Change-Detection — md5/sha256 statt Name+Size

`offiziell`

- **Falle:** Name+Size erkennt geänderte Dateien nicht zuverlässig (gleiche Größe ≠ gleicher
  Inhalt) → stiller Datenverlust im Backup.
- **Best Practice:** Drive berechnet automatisch `md5Checksum` **und** `sha256Checksum` für
  Blob-Dateien. Lokal Hash berechnen, mit dem Drive-Feld vergleichen (per `fields=files(id,name,
  md5Checksum,sha256Checksum,size,modifiedTime)`); nur bei Hash-Differenz hochladen. SHA-256
  bevorzugen (kollisionssicher). Foto-fileId + Hash lokal in der DB cachen, um nicht jedes Mal
  den ganzen appDataFolder durchlisten zu müssen.
- **Hinweis:** `md5Checksum`/`sha256Checksum` sind **nicht** in Drive-Suchen (`q`) verwendbar —
  wer danach filtern will, legt den Hash zusätzlich in `appProperties` ab.
- **Quelle:** https://developers.google.com/workspace/drive/api/reference/rest/v3/files · https://docs.cloud.google.com/storage/docs/data-validation

## 4) Multi-Device-Konfliktauflösung

`offiziell`

- **Harte Wahrheit:** Drive bietet **kein** serverseitiges Locking, **kein** If-Match/ETag-Concurrency-Token
  für `files.update`, **keine** transaktionale Erzeugung. Jeder Schutz ist clientseitig, Best-Effort.
- **Eine kanonische File-ID:** Erst-Backup `create`, ID lokal persistieren (+ `appProperties.backupSlot="primary"`
  als Wiederfinde-Marker); danach **immer `files.update`**, nie wieder `create`. Beim App-Start
  Dedupe-Sweep (mehrere Treffer → neuesten behalten, Rest löschen/mergen).
- **Read-before-write:** Vor jedem Upload `files.get?fields=headRevisionId,modifiedTime,size,appProperties`.
  Nur überschreiben, wenn lokal nachweislich neuer. `headRevisionId` als selbstgebautes
  Concurrency-Token (ändert sich bei jeder Inhalts-Revision). Achtung: Drive-`modifiedTime` ist ein
  Operations-Zeitstempel (ändert sich auch bei Rename) — fachlichen Stand zusätzlich als eigenen
  `dataVersion`/`lastEntryTs` in `appProperties` führen und den vergleichen.
- **Konflikt = warnen, nicht still überschreiben:** Bei Divergenz Remote-Stand zuerst sichern
  (Revision bleibt erhalten), nach fachlichem Zeitstempel entscheiden, Nutzer informieren.
- **Revisionen als Notbremse:** `revisions.list`/`get` für alte Stände; kritische mit
  `keepForever=true` markieren (Drive purged sonst auto nach ~30 T bzw. ab 100 Nicht-keepForever-Revisionen;
  `revisions.list` kann bei langer Historie unvollständig sein → nicht als alleinige Konflikt-Logik).
- **Changes API** (`changes.getStartPageToken` einmal, dann `changes.list(pageToken, spaces=appDataFolder)`,
  Token **pro Gerät**) erkennt Fremd-Änderungen günstig; bei ungültigem Token Full-Resync.
- **Wenn „kein Eintrag darf je verloren gehen":** Pro-Gerät-Datei (`entropy_journal_<deviceId>.db`)
  + Record-Level-Merge in Room (UUID-PK, per-Row-Timestamp, Tombstones). Datei-Merge in Drive ist
  prinzipiell unmöglich (binärer Blob).
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/manage-revisions · https://developers.google.com/workspace/drive/api/reference/rest/v3/changes/getStartPageToken

## 5) OAuth-Scopes, Consent & Auth-Migration

`offiziell`

- **Scope `drive.appdata` ist NON-SENSITIVE** (zusammen mit `drive.file`) → nur Basis-/Brand-Verifizierung,
  **kein CASA-Security-Assessment**, keine jährliche Prüfung. **Niemals** auf vollen `drive`-Scope
  erweitern (restricted → teures jährliches CASA).
- **Consent-Screen:** Im **„Testing"**-Status laufen Refresh-Tokens nach **7 Tagen** ab, max. **100
  Testnutzer**, „unverified app"-Warnscreen. Vor Release auf **„In Production"** + Brand-Verifizierung
  (für non-sensitive ~2–3 Werktage). Getrennte Cloud-Projekte für Test und Prod.
- **Auth-Migration (DRINGEND):** `GoogleAuthUtil.getToken` + komplettes `GoogleSignIn`-Paket sind
  **deprecated**, Entfernung aus dem SDK **ab Mai 2026**. Migration:
  - Autorisierung (Drive-Scope) → `Identity.getAuthorizationClient(activity).authorize(AuthorizationRequest)`.
    Bei `result.hasResolution()` den `PendingIntent` über `ActivityResultLauncher` starten (ersetzt
    `UserRecoverableAuthException`). Access-Token aus `authorizationResult.accessToken`.
  - Ungültigen Token: `clearToken(...)` statt blind weiterverwenden (Access-Token lebt ~1 h).
  - Identität (E-Mail/Name) → Credential Manager (`androidx.credentials`, schon in den Deps).
  - SDK von `play-services-auth 21.3.0` auf aktuell (21.6.0) heben.
- **Anti-Pattern vermeiden:** Access-Token NICHT als Identitätsnachweis ans Backend schicken;
  für Server-Zugriff `getServerAuthCode()` (Offline-Access), Refresh-Token nur serverseitig.
- **`revokeAccess()` entzieht ALLE Scopes** — nur für „Konto trennen", nicht zum Token-Aufräumen.
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/api-specific-auth · https://developer.android.com/identity/authorization · https://developer.android.com/identity/sign-in/legacy-gsi-migration

## 6) Quota & Rate-Limits

`offiziell`

- **NEU seit 01.05.2026 — Quota-Unit-Modell** (ersetzt „queries/100s" für neue Projekte; Projekte
  mit Drive-Calls Nov 2025–Apr 2026 behalten altes Modell): 1.000.000 Units/Min/Projekt, **325.000
  Units/Min/Nutzer**, 1 TB/Tag Egress. Kosten: Read 5, **List 100**, Download 200, **Write/Upload 50**.
  → Das Per-User-Quota ist bei 5 parallelen Uploads praktisch nie der Engpass.
- **Echter Engpass = Drive-Backend-Rate-Limit:** `403 userRateLimitExceeded` auch bei freier
  Console-Quota. Praxis: **3–4 parallele Transfers** robust, ab ~8 häufen sich Fehler; nicht mehr
  als **~3 sustained Writes/s**. → Semaphore von 5 auf 3–4 senken + Token-Bucket.
- **Backoff (Pflicht):** Truncated Exponential Backoff **mit Jitter**:
  `wait = min((2^n) + random_ms, max_backoff)`, `random_ms ≤ 1000` (pro Retry neu würfeln —
  verhindert Retry-Storm bei parallelen Uploads), `max_backoff` 32–64 s, begrenzte Retry-Zahl.
  `Retry-After`-Header hat Vorrang. Retry nur bei 429 / 403-rate/userRate / 5xx.
- **NICHT retryen:** `403 storageQuotaExceeded` (Nutzer-Speicher voll — **appDataFolder zählt gegen
  die 15 GB!**; vorab `about.get?fields=storageQuota` prüfen, Nutzer informieren, alte Generationen
  rotieren), `400 badRequest`, `404`. `403 dailyLimitExceeded` ist oft ein selbst gesetztes
  Console-Cap → entfernen, nicht retryen.
- **Kein Batch für Media:** Drive unterstützt keine Batch-Uploads/-Downloads. `/batch` nur für
  Metadaten (max 100, zählt als n Requests, Reihenfolge nicht garantiert).
- **`fields=` überall setzen** (Upload `fields=id`), gzip aktivieren — spart Bandbreite/RAM. Pro
  Nutzer max **750 GB Upload/Tag**.
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/limits · https://developers.google.com/workspace/drive/api/guides/handle-errors

## 7) Resumable Upload & Zuverlässigkeit

`offiziell`

- **Resumable als Default** für mobiles Backup (nicht nur > 5 MB) — übersteht Verbindungsabbrüche.
  Im Java-Client per Default aktiv; **nie** `setDirectUploadEnabled(true)`.
- **Chunk-Größe = Vielfaches von 256 KB** (`n * MediaHttpUploader.MINIMUM_CHUNK_SIZE`), praktisch
  5–10 MB; krumme Werte brechen den Upload.
- **Session-URI persistieren** (Room/DataStore), gültig **1 Woche**. Status nach Abbruch: leerer
  `PUT` mit `Content-Range: */<total>` → `308 Resume Incomplete` (Range-Header lesen, ab dort weiter),
  `200/201` = fertig, `404` = Session tot → neu starten. **5xx → resume, jeder 4xx → komplett neu.**
- **Idempotenz (KRITISCH gegen Duplikate):** `files.generateIds` → Pre-Generated File-ID beim
  `create` mitgeben. Retry nach unklarem Serverfehler gibt dann `409 Conflict` statt Duplikat.
  Zusätzlich Gürtel-und-Hosenträger: vor Upload `files.list` → existiert? → `files.update` statt `create`.
- **Content-Länge immer setzen:** `FileContent(mimeType, file)` bevorzugen (Länge bekannt);
  bei `InputStreamContent` `setLength()` — sonst puffert der Uploader Chunks im RAM → OOM.
- **Timeouts hochsetzen:** read-Timeout ≥ 3 min via `HttpRequestInitializer` (Default 20 s zu kurz)
  — sonst `SocketTimeoutException` bei großen Chunks; beim Wrappen den Original-Initializer mitaufrufen
  (sonst Auth weg).
- **Thread-Sicherheit:** EIN `Drive`-Service + EIN `NetHttpTransport` app-weit teilen (Connection-Reuse),
  aber **pro Coroutine ein eigenes Request-/`MediaHttpUploader`-Objekt** — `AbstractGoogleClientRequest`
  ist NICHT thread-safe. Credentials sind thread-safe.
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/manage-uploads · https://developers.google.com/api-client-library/java/google-api-java-client/media-upload

## 8) WorkManager statt ViewModel-Scope

`offiziell`

- **Falle:** Upload im `viewModelScope` wird bei App-Wechsel/Activity-Zerstörung gecancelt → Backup
  bricht ab.
- **Best Practice:** Backup in einen **`CoroutineWorker`** (WorkManager) — übersteht App-Exit/Prozess-Tod/Neustart.
  ViewModel beobachtet nur den `WorkInfo`-Status für die UI. Constraints: `NetworkType.UNMETERED`
  (nur WLAN), `setRequiresBatteryNotLow(true)`. Backoff `EXPONENTIAL`; bei behebbaren Fehlern `Result.retry()`.
- **Android 14+ Foreground-Service:** Typ deklarieren — `FOREGROUND_SERVICE_TYPE_DATA_SYNC`
  (`<service android:foregroundServiceType="dataSync">` + Permission). **Android 14/15: `dataSync`
  hat ein 6-Stunden-Budget/24 h** (ab 15 teilen sich alle FGS dieses Budget) → für sehr lange/große
  Backups **User-Initiated Data Transfer (UIDT) Jobs** statt `dataSync`; ohnehin in Chunks/Delta arbeiten.
- **Quellen:** https://developer.android.com/develop/background-work/background-tasks/persistent · https://developer.android.com/develop/background-work/services/fgs/timeout

## 9) Restore & Integritätsprüfung

`offiziell` (Drive/Android) · `extern` (SQLite-Forum)

- **Download:** `files.get` mit `alt=media` (Java: `executeMediaAndDownloadTo`), resumable/chunked
  gegen Abbruch. `drive.appdata` reicht; reiner Metadaten-Scope darf nicht herunterladen.
- **Integrität (Pflicht, VOR dem Ersetzen):** Metadaten mit `fields=md5Checksum,sha256Checksum,size,
  appProperties` holen; lokal Hash über die empfangenen Bytes berechnen, vergleichen → Mismatch =
  Abbruch (intakte Live-DB nicht durch kaputte ersetzen). Zusätzlich eigenen SHA-256 beim Backup in
  `appProperties` ablegen (durchsuchbar, unabhängig von Drives Server-Hash).
- **WAL/SHM (häufigste Korruptionsfalle):** Dein `wal_checkpoint(TRUNCATE)` vor dem Backup ist
  korrekt. Beim **Restore** die alten `…-wal` UND `…-shm` neben der Ziel-DB **mit löschen** — sonst
  spielt SQLite die alte WAL auf die neue DB → Korruption/„malformed".
- **Atomarer Swap:** Download in **Temp-Datei** → Checksum prüfen → `PRAGMA quick_check` (`ok`?) +
  `PRAGMA user_version`/Schema-Version prüfen → Room `close()` + Singleton verwerfen (keine offene
  Verbindung!) → alte `.db`/`-wal`/`-shm` löschen (alte `.db` optional als `.bak`) → Temp atomar
  umbenennen → **App-Neustart** (neue Room-Instanz). Bei jedem Fehler: Temp verwerfen, Original bleibt.
- **Schema-Version:** Restaurierte DB **neuer** als Code → Restore ablehnen (Update nötig); **älter**
  → registrierte Room-`Migration`s greifen. **Kein** `fallbackToDestructiveMigration()` als Default
  (löscht die gerade restaurierte DB).
- **Fotos + DB:** Fotos zuerst vollständig laden+verifizieren, **DB-Swap als letzten atomaren Schritt**;
  gemeinsamen Snapshot-Token in beiden Manifesten prüfen; nach Restore Foto-Referenzen reconcilen
  (fehlende markieren statt crashen).
- **SQLCipher (falls):** verschlüsselte DB als Datei tauschen ist sicher; nach Restore mit demselben
  Key öffnen + `PRAGMA cipher_integrity_check`. Falscher Key wirkt wie Korruption — Key nie ins Backup legen.
- **Quellen:** https://developers.google.com/workspace/drive/api/guides/manage-downloads · https://sqlite.org/wal.html · https://sqlite.org/pragma.html

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/android/google-drive-backup.md`) |
|-------------------------|----------------------------------------------------------------|
| §1 (appDataFolder/Layout) | A1–A8 (setSpaces/setFields/trashed/name-contains/Quota/Properties/Lebenszyklus) |
| §2 (Orphan-Cleanup) | O1–O5 (Orphan-Wachstum/Sweep-Sicherheit/Atomarität/Multi-Device-Sweep) |
| §3 (Change-Detection) | O2 (Name+Size unzuverlässig) |
| §4 (Multi-Device) | M1–M6 (kein If-Match/LWW/modifiedTime/Duplikate/Revisions-Purge) |
| §5 (Scopes/Auth) | AU1–AU6 (GoogleAuthUtil-Deprecation/Testmodus/Token/revokeAccess/Anti-Pattern) |
| §6 (Quota/Rate) | Q1–Q6 (Quota-Modell/Semaphore/storageQuota/dailyLimit/Batch/Backoff) |
| §7 (Resumable/Reliability) | U1–U8 (Chunk-256K/Session-URI/Idempotenz/OOM/Timeout/Thread-Safety) |
| §8 (WorkManager) | U9–U11 (ViewModel-Scope-Cancel/FGS-Typ/6h-Limit) |
| §9 (Restore/Integrität) | R1–R9 (alt=media/Checksum/WAL-SHM/Swap-bei-offener-DB/Schema/SQLCipher) |

---

## Quellen (Auswahl, mit Flag)

- `offiziell` Google — appDataFolder, Uploads, Downloads, Revisions, Changes, Limits, Errors, Auth:
  https://developers.google.com/workspace/drive/api/guides/appdata · …/manage-uploads · …/manage-downloads ·
  …/manage-revisions · …/manage-changes · …/limits · …/handle-errors · …/api-specific-auth
- `offiziell` Android — Authorization/Credential-Manager, WorkManager, FGS-Timeout, Room:
  https://developer.android.com/identity/authorization · …/develop/background-work/services/fgs/timeout
- `offiziell` SQLite — WAL, PRAGMA integrity/quick_check: https://sqlite.org/wal.html · https://sqlite.org/pragma.html
- `extern` Sync-Praxis — rsync `--max-delete`, Iceberg-Orphan-Cleanup, Borg/Restic-Retention (Muster
  für Reconciliation/Sicherheitsnetze).

> **Checkpoint:** Vollständig recherchiert (7 Researcher). Kern: Waisen entstehen zwangsläufig bei
> upsert-only Einzeldatei-Sync → Reconciliation-Sweep (Manifest + Max-Delete-Schutz) ODER
> ZIP-Generationen. Zwei akute Wartungspunkte: Quota-Unit-Modell (seit 01.05.2026) und
> GoogleAuthUtil-Entfernung (ab Mai 2026).
