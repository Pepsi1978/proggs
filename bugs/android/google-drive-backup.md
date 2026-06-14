# Bekannte Bugs: Google-Drive-Backup & Cloud-Sync (Android, appDataFolder)

> PFLICHT-LESEN vor Arbeit am Drive-Backup/Restore (BestJournal: Room-DB + Fotos → appDataFolder).
> Stand: tief recherchiert am 2026-06-14 (7 Researcher parallel). Versions-Anker:
> `google-api-services-drive:v3-rev20241206-2.0.0` · Drive REST v3 (**Quota-Unit-Modell seit
> 01.05.2026**) · `play-services-auth 21.3.0` (`GoogleAuthUtil`/`GoogleSignIn` deprecated,
> **Entfernung ab Mai 2026**) · `androidx.credentials 1.5.0-alpha06` + `googleid 1.1.1` ·
> compileSdk 35, minSdk 26. Lokale DB ist Source of Truth.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/android/best-practices-google-drive-backup.md`](../../best-practices/projekt-code/android/best-practices-google-drive-backup.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **A** appDataFolder · **O** Orphan/Sync · **M** Multi-Device · **AU** Auth · **Q** Quota · **U** Upload · **R** Restore.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `list` findet nichts | `setSpaces("appDataFolder")` vergessen | A1 |
| 2 | `md5Checksum`/`size`/`appProperties` null | `setFields(...)` explizit angeben | A2 |
| 3 | `trashed=true` schlägt fehl | appDataFolder kann nicht trashen → `files.delete` (permanent) | A3 |
| 4 | `name contains` findet zu wenig | Beim `name`-Feld nur **Prefix**-Match → stabile Präfixe | A4 |
| 5 | Backup wächst endlos | Reconciliation-Sweep `remote − lokal`; Name+Size ist unzuverlässig | O1, O2 |
| 6 | Sweep löscht zu viel | Max-Delete-Schwelle; nie aus bloßer lokaler Abwesenheit löschen (Multi-Device) | O3, O4 |
| 7 | 2. Gerät überschreibt neueres Backup | Read-before-write (`headRevisionId`/eigener `dataVersion`); kein If-Match in Drive | M1, M2 |
| 8 | Doppelte Backup-Datei | Kanonische File-ID + `update`; list-then-update gegen create-Race | M4 |
| 9 | Alte Version weg | Drive purged Revisionen (30 T/100 Rev.) → `keepForever` für kritische | M5 |
| 10 | Build bricht / Auth tot ab Mai 2026 | `GoogleAuthUtil` entfernt → `AuthorizationClient.authorize()` migrieren | AU1 |
| 11 | Nach 7 Tagen Re-Auth nötig | Consent-Screen „Testing" → auf „Production" + Verifizierung | AU2 |
| 12 | `403`/`429` beim Massen-Upload | Backoff+Jitter; Semaphore 5→3–4; ~3 Writes/s | Q1, Q2 |
| 13 | `403 storageQuotaExceeded` | NICHT retryen; appDataFolder zählt gegen 15 GB; `about.get` vorab | Q3 |
| 14 | Upload-Duplikate nach Retry | Resumable + Pre-Generated File-ID (`generateIds`) → 409 statt Duplikat | U3 |
| 15 | Chunk-Upload bricht | Chunk = Vielfaches von 256 KB; Session-URI 1 Woche gültig | U1, U2 |
| 16 | OOM / `SocketTimeout` bei großen Dateien | Content-Länge setzen (`FileContent`); read-Timeout ≥ 3 min | U5, U6 |
| 17 | Sporadische Fehler bei 5 parallel | Pro Coroutine eigenes Request-Objekt (nicht thread-safe) | U7 |
| 18 | Upload stoppt bei App-Wechsel | WorkManager statt ViewModel-Scope; FGS-Typ `dataSync` (Android 14+) | U9, U10 |
| 19 | Restore → „malformed" | Alte `-wal`/`-shm` mit löschen; Room `close()` vor Swap; Checksum prüfen | R3, R4 |
| 20 | Restore-Crash Schema | Schema-Version vor Swap prüfen; kein `fallbackToDestructiveMigration` | R6 |

---

## A) appDataFolder-Grundlagen

### A1. `files.list` liefert leere Treffer — `setSpaces` vergessen ⭐ HAEUFIG
- **Symptom:** Backup-Dateien existieren, aber `files.list` gibt nichts zurück.
- **Ursache:** Ohne `setSpaces("appDataFolder")` sucht die API im normalen `drive`-Space, nicht im App-Ordner.
- **Versionen:** v3, alle.
- **FIX:** Bei JEDEM `list`/`get` im App-Ordner `setSpaces("appDataFolder")` setzen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/appdata

### A2. `md5Checksum`/`size`/`modifiedTime`/`appProperties` sind null ⭐ HAEUFIG
- **Symptom:** Felder in der `list`/`get`-Antwort null, obwohl gesetzt.
- **Ursache:** `files.list` liefert per Default nur `kind,id,name,mimeType,resourceKey`; alles andere nur per `fields`.
- **Versionen:** v3, alle.
- **FIX:** `setFields("nextPageToken, files(id,name,md5Checksum,sha256Checksum,size,modifiedTime,appProperties)")`.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/fields-parameter

### A3. `trashed=true` im appDataFolder → `notSupportedForAppDataFolderFiles`
- **Symptom:** `files.update trashed=true` (oder v2 `files.trash`) schlägt fehl: „Files within the Application Data folder cannot be trashed."
- **Ursache:** Im App-Ordner ist kein Papierkorb möglich; ebenso kein Teilen/Permissions, kein Verschieben zwischen Spaces.
- **Versionen:** v3, per Design.
- **FIX:** Immer `files.delete(fileId)` (permanent, gibt Speicher sofort frei). Für „Soft-Delete" eigenes `appProperties.deletedAt`-Tombstone nutzen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/appdata

### A4. `name contains` macht beim `name`-Feld nur Prefix-Matching ⭐ HAEUFIG
- **Symptom:** `q="name contains 'foto'"` findet `foto_001.jpg`, aber NICHT `backup_foto.jpg`.
- **Ursache:** `contains` ist beim `name`-Term nur Präfix-Abgleich, kein echtes „enthält irgendwo".
- **Versionen:** v3.
- **FIX:** Backup-Dateien mit eindeutigem, stabilem Präfix benennen (`bestjournal_db_`, `bestjournal_photo_`); danach `name contains 'bestjournal_'`. Besser noch über `appProperties` filtern. Apostroph/Backslash im `q` escapen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/ref-search-terms

### A5. appDataFolder zählt gegen die 15 GB des Nutzers ⭐ HAEUFIG
- **Symptom:** Foto-Backups füllen unbemerkt den Drive-Speicher; irgendwann `403 storageQuotaExceeded`, obwohl der Nutzer „nichts" in Drive sieht.
- **Ursache:** Der App-Ordner ist nur versteckt, nicht speicherfrei — er belegt das 15-GB-Konto (geteilt mit Gmail/Photos), gelistet unter „Hidden app data".
- **Versionen:** alle.
- **FIX:** Backup-Größe begrenzen/rotieren, Fotos komprimieren; vorab `about.get?fields=storageQuota` prüfen; `storageQuotaExceeded` nicht retryen (siehe Q3).
- **Quelle:** https://support.google.com/drive/answer/6374270

### A6. `appProperties`-Limits gesprengt
- **Symptom:** `files.create/update` schlägt fehl bei zu vielen/zu langen Properties.
- **Ursache:** Max. 100 Properties/Datei, max. 30 private (appProperties)/App/Datei, max. **124 Bytes** pro Property (Key+Value, UTF-8).
- **Versionen:** alle.
- **FIX:** Metadaten knapp halten; großes JSON-Manifest NICHT in `appProperties` zwängen, sondern in eine separate Manifest-Datei.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/properties

### A7. Backup verschwindet bei Deinstallation / Konto-Wechsel
- **Symptom:** Cloud-Backup plötzlich weg / im neuen Konto leer.
- **Ursache:** appDataFolder wird bei App-Deinstallation (Entfernen aus My Drive) gelöscht; er ist konto-gebunden (anderes Konto = anderer, leerer Ordner).
- **Versionen:** alle.
- **FIX:** Drive-Backup nie als alleinige Quelle behandeln — lokale DB bleibt Source of Truth. Konto-ID in `appProperties`/Manifest; beim Restore klar kommunizieren, welches Konto nötig ist.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/appdata

### A8. `md5Checksum`/`headRevisionId`/`sha256Checksum` nur für Blob-Dateien
- **Symptom:** Felder null.
- **Ursache:** Nur für echte Binär-/Blob-Inhalte gefüllt (nicht für Google-Docs-Typen/Ordner/Shortcuts). DB + JPG/PNG sind Blobs → gefüllt.
- **Versionen:** v3.
- **FIX:** Nur für Blob-Dateien darauf verlassen (trifft hier zu).
- **Quelle:** https://developers.google.com/workspace/drive/api/reference/rest/v3/files

---

## O) Orphan-Cleanup / Sync

### O1. Backup wächst endlos durch fehlende Reconciliation ⭐ HAEUFIG
- **Symptom:** Lokal gelöschtes Foto bleibt als `photo_<name>` auf Drive; Backup wird nie kleiner.
- **Ursache:** Upsert-only-Sync (nur neue/geänderte lokale Dateien hoch) ohne Gegenrichtung „welche Remote-Dateien haben lokal kein Gegenstück mehr?".
- **Versionen:** eigener Code (generisches Sync-Muster).
- **FIX:** Reconciliation-Sweep: `remote_set` (alle `photo_*`) − `local_set` (DB-Keys) = Waisen → entfernen (erst trashen/soft-delete, dann hart). Manifest-getrieben: neue Dateien hoch → Manifest committen → erst danach Waisen löschen.
- **Quelle:** https://www.makeuseof.com/what-are-orphaned-files-google-drive/ · https://iomete.com/resources/blog/iceberg-maintenance-runbook

### O2. Change-Detection über Name+Size übersieht geänderte Dateien ⭐ HAEUFIG
- **Symptom:** Lokal geändertes Foto (gleicher Name, zufällig gleiche Größe) wird nicht neu hochgeladen → Backup hat stille Alt-Version.
- **Ursache:** Größe/Timestamp sind keine Inhalts-Identität.
- **Versionen:** eigener Code.
- **FIX:** Auf `md5Checksum`/`sha256Checksum` umstellen (+ Größen-Sanity-Check). Drive berechnet beide automatisch für Blobs.
- **Quelle:** https://docs.cloud.google.com/storage/docs/data-validation

### O3. Sweep löscht das ganze Backup bei leerer/kaputter lokaler DB
- **Symptom:** Nach Migrationsfehler ist `local_set` leer → Sweep löscht alles.
- **Ursache:** Naiver Sweep nimmt „nicht lokal = Müll" ohne Sicherheitsnetz.
- **Versionen:** eigener Code.
- **FIX:** Max-Delete-Schwelle (rsync-Prinzip): Sweep abbrechen, wenn er > X % löschen würde; bei I/O-Fehler/verdächtig leerer DB Sweep gar nicht ausführen; Orphan-Liste vorher als Dry-Run loggen.
- **Quelle:** https://linux.die.net/man/1/rsync · https://www.computerhope.com/unix/rsync.htm

### O4. Multi-Device-Sweep löscht fremde aktuelle Dateien
- **Symptom:** Gerät A löscht beim Sweep Fotos, die Gerät B frisch hochgeladen hat (auf A nie lokal) → Datenverlust.
- **Ursache:** „Remote, aber nicht in meiner lokalen DB" ist bei Multi-Device oft „von einem anderen Gerät".
- **Versionen:** eigener Code, Multi-Device.
- **FIX:** Nur per explizitem Tombstone löschen (nicht aus bloßer Abwesenheit); Pro-Gerät-Namespace (`photo_<deviceId>_<name>`) → jedes Gerät sweept nur eigene; oder Changes API nutzen.
- **Quelle:** https://docs.syncthing.net/users/syncing.html

### O5. Nicht-atomares Backup: alte Generation gelöscht, neuer Upload bricht ab → gar kein Backup
- **Symptom:** Nach Abbruch kein gültiges Backup mehr.
- **Ursache:** „delete-then-upload" statt „upload-then-delete".
- **Versionen:** eigener Code.
- **FIX:** Strikt: neue Generation hochladen + verifizieren (Checksum) → Manifest/Pointer umschreiben → **erst danach** alte/Waisen löschen.
- **Quelle:** https://borgbackup.readthedocs.io/en/stable/usage/prune.html

---

## M) Multi-Device-Konfliktauflösung

### M1. Drive hat KEIN If-Match/ETag-Concurrency-Token für `files.update`
- **Symptom:** `If-Match` mit `headRevisionId` als Konflikt-Guard funktioniert nicht (kein 412).
- **Ursache:** Drive-`files` stellt keinen schreibbaren Versions-Vorbedingungs-Mechanismus bereit; `headRevisionId` ist read-only.
- **Versionen:** v3.
- **FIX:** Optimistic Concurrency selbst nachbauen: vor `update` `files.get?fields=headRevisionId,modifiedTime` lesen, nur überschreiben, wenn `headRevisionId` noch die zuletzt gesehene ist (Read-Compare-Write, Best-Effort).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-revisions

### M2. Blindes Überschreiben = Datenverlust (Last-Write-Wins) ⭐ HAEUFIG
- **Symptom:** Gerät B überschreibt das frischere Backup von Gerät A; A's letzte Einträge weg.
- **Ursache:** „Lade hoch, was lokal ist", ohne Remote-Zustand zu lesen.
- **Versionen:** eigener Code.
- **FIX:** Vor jedem Upload `files.get?fields=modifiedTime,headRevisionId,appProperties`; nur überschreiben, wenn lokal echt neuer; sonst Remote sichern/Nutzer warnen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/appdata

### M3. Drive-`modifiedTime` ≠ fachlicher Daten-Stand
- **Symptom:** `modifiedTime`-Vergleich trügt; schon ein Rename/Metadaten-Touch ändert sie.
- **Ursache:** `modifiedTime` ist ein Drive-Operations-Zeitstempel.
- **Versionen:** v3.
- **FIX:** Eigenen fachlichen Zeitstempel/`dataVersion` in `appProperties` führen und den vergleichen; `modifiedTime` nur als grober Sekundärindikator.
- **Quelle:** https://github.com/rclone/rclone/issues/4412

### M4. Duplikate durch `create`-Race (zwei Geräte) ⭐ HAEUFIG
- **Symptom:** Zwei Dateien gleichen Namens, verschiedene IDs; Restore mehrdeutig.
- **Ursache:** Keine transaktionale Erzeugung; Lücke zwischen `list` und `create`; Drive erlaubt Namensgleichheit.
- **Versionen:** v3.
- **FIX:** Erst-Backup `create`, File-ID lokal persistieren, danach immer `update`. Auf frischem Gerät vor `create` per `files.list`/`appProperties`-Marker suchen → vorhandene ID nutzen. Periodischer Dedupe-Sweep beim App-Start (neueste behalten, Rest löschen). Restwahrscheinlichkeit bei exakt gleichzeitigem Erst-Backup bleibt.
- **Quelle:** https://github.com/rclone/rclone/issues/4412

### M5. Alte Revisionen werden auto-gepurged
- **Symptom:** Vorletzte Revision zum Zurückholen schon gelöscht.
- **Ursache:** Drive löscht alte Revisionen automatisch (~30 Tage, früher wenn 100 Nicht-keepForever-Revisionen erreicht).
- **Versionen:** v3.
- **FIX:** Kritische Stände per `revisions.update keepForever=true` markieren (max 200, zählen auf Quota). Head-Revision wird nie auto-gepurged.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-revisions

### M6. `revisions.list` bei langer Historie unvollständig
- **Symptom:** Ältester Stand fehlt in der Liste.
- **Ursache:** Doku: bei großer Revisionshistorie kann die Liste unvollständig sein; erste ≠ älteste.
- **Versionen:** v3.
- **FIX:** Revisionen nur als Best-Effort-Recovery; Konflikt-Logik auf eigenem `dataVersion`/Zeitstempel beruhen lassen, nicht auf Revisions-Vollständigkeit.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-revisions

### M7. `contentRestrictions.readOnly` ist KEIN Lock
- **Symptom:** Als Concurrency-Guard gedacht, schützt aber nicht.
- **Ursache:** Von jedem Writer wieder abschaltbar, mutable, „not meant to create an immutable record".
- **Versionen:** v3.
- **FIX:** Nicht als Lock nutzen; stattdessen Best-Effort-Lease über `appProperties.lockedBy/lockedUntil` + Jitter, plus Read-before-write (M1/M2).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/content-restrictions

---

## AU) OAuth, Consent & Auth-Migration

### AU1. `GoogleAuthUtil`/`GoogleSignIn` deprecated — Entfernung ab Mai 2026 ⭐ HAEUFIG
- **Symptom (zukünftig):** Ab Mai 2026 entfernt → Build bricht beim SDK-Update; ab ~Juli 2028 serverseitiger Fehlschlag auch mit altem SDK.
- **Ursache:** `com.google.android.gms.auth.GoogleAuthUtil.getToken` + `UserRecoverableAuthException` + das ganze `GoogleSignIn`-Paket sind deprecated — genau der hier genutzte Pfad.
- **Versionen:** play-services-auth (aktuell 21.6.0; Projekt auf 21.3.0).
- **FIX:** Migrieren auf `Identity.getAuthorizationClient(activity).authorize(AuthorizationRequest mit Scope(DRIVE_APPDATA))`; bei `result.hasResolution()` `PendingIntent` über `ActivityResultLauncher` (ersetzt `UserRecoverableAuthException`); Access-Token aus `authorizationResult.accessToken`. Identität via Credential Manager (schon in Deps).
- **Quelle:** https://developer.android.com/identity/sign-in/legacy-gsi-migration · https://developer.android.com/identity/authorization

### AU2. Consent-Screen „Testing": Refresh-Token nach 7 Tagen abgelaufen
- **Symptom:** Nach Tagen plötzlich `invalid_grant`/Re-Auth, obwohl Nutzer nichts geändert hat.
- **Ursache:** Im Publishing-Status „Testing" verfallen Autorisierungen/Refresh-Tokens nach 7 Tagen; pro Testnutzer nur 1 gültiges Refresh-Token.
- **Versionen:** OAuth-Consent generell.
- **FIX:** Für echte Nutzung App auf „In Production" + Verifizierung (für `drive.appdata` non-sensitive nur Brand-Verifizierung). Getrennte Cloud-Projekte für Test/Prod.
- **Quelle:** https://support.google.com/cloud/answer/15549945

### AU3. „Testing": 100-Nutzer-Cap + „unverified app"-Warnscreen
- **Symptom:** Ab 101. Tester Login-Fehler; Tester sehen den Warnscreen.
- **Ursache:** Testmodus erlaubt max. 100 explizit gelistete Testnutzer.
- **Versionen:** OAuth-Consent.
- **FIX:** Vor breiter Verteilung veröffentlichen + Brand-Verifizierung; Privacy-Policy-URL + Homepage auf gleicher öffentlicher Domain.
- **Quelle:** https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification

### AU4. Access-Token (1 h) abgelaufen/ungültig, gecacht weiterverwendet
- **Symptom:** Sporadische API-Fehler trotz „gültigem" Token.
- **Ursache:** Access-Tokens leben ~1 h, werden lokal gecacht, können vorzeitig ungültig werden.
- **Versionen:** alle.
- **FIX:** Bei Fehler `Identity.getAuthorizationClient(activity).clearToken(...)`, dann erneut `authorize()`.
- **Quelle:** https://developer.android.com/identity/authorization

### AU5. `revokeAccess()` entzieht ALLE Scopes
- **Symptom:** Nach gezieltem Widerruf kompletter Drive-Zugriff weg.
- **Ursache:** `AuthorizationClient.revokeAccess()` entzieht alle gewährten Scopes + löscht gecachte Tokens.
- **Versionen:** alle.
- **FIX:** `revokeAccess()` nur für „Konto trennen"; zum Token-Aufräumen `clearToken()`.
- **Quelle:** https://developer.android.com/identity/authorization

### AU6. Access-Token als Identitätsnachweis ans Backend (Anti-Pattern)
- **Symptom:** Sicherheitslücke (fremdes Token einschleusbar).
- **Ursache:** `getToken("oauth2:...")`-Muster verleitet dazu, den Access-Token als Login-Beweis zu senden.
- **Versionen:** alle.
- **FIX:** Identität via ID-Token (Credential Manager); serverseitiger Drive-Zugriff via `getServerAuthCode()` (Offline-Access), Refresh-Token nur serverseitig.
- **Quelle:** https://developers.google.com/identity/sign-in/android/migration-guide

---

## Q) Quoten & Rate-Limits

### Q1. `403 userRateLimitExceeded`/`429` beim Massen-Upload ⭐ HAEUFIG
- **Symptom:** Beim parallelen Foto-Upload 403/429, auch wenn Console-Quota frei wirkt.
- **Ursache:** Drive-Backend-Rate-Limit (nicht das Console-Quota); zu viele Writes/s.
- **Versionen:** v3 (Quota-Unit-Modell seit 01.05.2026; Write/Upload = 50 Units, Per-User 325.000/Min — selten der Engpass).
- **FIX:** Truncated Exponential Backoff **mit Jitter** (`min((2^n)+random≤1000ms, 32–64s)`, begrenzte Retries, `Retry-After` hat Vorrang); Retry nur bei 429/403-rate/5xx.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/limits

### Q2. Semaphore(5) als „sicher" angenommen
- **Symptom:** Sporadische 403 trotz nur 5 Threads.
- **Ursache:** Nicht Thread-Zahl, sondern Writes/s triggern das Limit (5 schnelle = > 3 Writes/s); Google: „too many parallel threads".
- **Versionen:** v3.
- **FIX:** Semaphore 5 → 3–4 + clientseitig auf ~3 Writes/s drosseln (Token-Bucket); Backoff fängt Spitzen.
- **Quelle:** https://rcloneview.com/support/blog/fix-google-drive-403-rate-limit-errors-rcloneview

### Q3. `403 storageQuotaExceeded` — nicht retrybar ⭐ HAEUFIG
- **Symptom:** Upload schlägt fehl, Backoff hilft nicht.
- **Ursache:** Nutzer-Speicher (15 GB) voll; appDataFolder zählt mit (siehe A5).
- **Versionen:** v3.
- **FIX:** NICHT retryen; Nutzer klar informieren; vorab `about.get?fields=storageQuota` (limit/usage); alte Generationen rotieren (Nutzer kann App-Daten nicht selbst sehen).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/handle-errors

### Q4. `403 dailyLimitExceeded` blind retryen
- **Symptom:** 403 trotz niedriger Last; Backoff hilft nicht.
- **Ursache:** Meist ein selbst gesetztes „Queries per day"-Cap in der Cloud Console.
- **Versionen:** v3.
- **FIX:** Cap in der Console entfernen, nicht endlos retryen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/handle-errors

### Q5. Uploads batchen wollen (`/batch`)
- **Symptom:** Media-Upload-Batch schlägt fehl.
- **Ursache:** „Drive doesn't support batch operations for media" (Upload/Download/Export).
- **Versionen:** v3.
- **FIX:** Uploads einzeln parallel; `/batch` nur für Metadaten (max 100, zählt als n Requests, Reihenfolge nicht garantiert).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/performance#batch

### Q6. Quota-Modell-Wechsel (01.05.2026) übersehen
- **Symptom:** Limits anders als in älteren Anleitungen; Debugging verwirrend.
- **Ursache:** Neues **Quota-Unit-Modell** für Projekte ab 01.05.2026; Projekte mit Calls Nov 2025–Apr 2026 behalten altes „queries/100s"-Modell. Zusätzlich: 750 GB Upload/Tag/Nutzer.
- **Versionen:** v3, ab 01.05.2026.
- **FIX:** Projekt-Alter/Modell prüfen; `fields=` überall (Upload `fields=id`) + gzip zur Last-Reduktion; `quotaUser` nur relevant bei zentralem Server.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/limits

---

## U) Upload-Zuverlässigkeit

### U1. Resumable-Chunk-Größe nicht Vielfaches von 256 KB
- **Symptom:** Upload bricht ab / Server verweigert Chunks.
- **Ursache:** Chunks müssen Vielfache von 256 KB sein (außer letzter); `setChunkSize` verlangt `n * MINIMUM_CHUNK_SIZE`.
- **Versionen:** google-api-java-client.
- **FIX:** `n * MediaHttpUploader.MINIMUM_CHUNK_SIZE` (praktisch 5–10 MB).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-uploads

### U2. Resumable-Session-URI nach 1 Woche abgelaufen
- **Symptom:** Wiederaufnahme → 404.
- **Ursache:** Session-URI läuft nach 1 Woche (Inaktivität) ab.
- **Versionen:** v3.
- **FIX:** Session-URI mit Zeitstempel persistieren; bei > ~6 Tagen oder 404 neu initiieren; Status via leerem `PUT` mit `Content-Range: */<total>` (308 → ab `Range` weiter, 200/201 = fertig).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-uploads

### U3. Retry erzeugt Duplikate (fehlende Idempotenz) ⭐ HAEUFIG
- **Symptom:** Nach Timeout/Abbruch liegt dieselbe Datei mehrfach in Drive.
- **Ursache:** `files.create` ist nicht idempotent; erneuter POST erzeugt neue Datei. Drive dedupliziert API-Uploads nicht.
- **Versionen:** v3.
- **FIX:** Resumable verwenden (Datei entsteht erst beim Abschluss). Pre-Generated File-ID via `files.generateIds` beim `create` → Retry gibt `409 Conflict`, kein Duplikat (nicht für Workspace-Typen). Gürtel+Hosenträger: vor Upload `files.list` → `files.update` statt `create`.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-uploads

### U4. 4xx während resumable Upload wie 5xx behandeln
- **Symptom:** Endlose Retries oder kaputte Session.
- **Ursache:** 5xx/Abbruch = resume; **jeder 4xx** (außer 403-rate) = Session tot → kompletter Neustart.
- **Versionen:** v3.
- **FIX:** Fehlerklassen trennen: 5xx → Session-URI resumen; 4xx → neue Session; 403-rate → mit Backoff am selben Punkt.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-uploads

### U5. OOM bei unbekannter Content-Länge
- **Symptom:** `OutOfMemoryError` bei großen Uploads (v. a. aus Streams).
- **Ursache:** Ohne bekannte Länge puffert der Uploader ganze Chunks im RAM.
- **Versionen:** google-api-java-client.
- **FIX:** Content-Länge immer setzen — `FileContent(mimeType, file)` (Länge bekannt) bevorzugen; bei `InputStreamContent` `setLength()`.
- **Quelle:** https://googleapis.dev/java/google-api-client/1.30.5/com/google/api/client/googleapis/media/MediaHttpUploader.html

### U6. `SocketTimeoutException` bei großen Dateien (Default-Timeouts zu kurz)
- **Symptom:** Großer Upload stirbt mit `SocketTimeoutException`.
- **Ursache:** `NetHttpTransport`-Default-Timeouts (~20 s) zu knapp für große Chunks/langsame Netze.
- **Versionen:** google-api-java-client.
- **FIX:** Über `HttpRequestInitializer` `setConnectTimeout(60_000)`/`setReadTimeout(180_000)`+; beim Wrappen den Original-Initializer mitaufrufen (sonst Auth weg); Chunk-Größe moderat.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/853

### U7. Request-Objekte NICHT thread-safe (5 parallele Coroutinen) ⭐ HAEUFIG
- **Symptom:** Sporadische Fehler/„meist ok, selten Murks" bei parallelen Uploads.
- **Ursache:** `AbstractGoogleClientRequest` (Basis aller `files().create/update`) ist nicht thread-safe; `MediaHttpUploader` ist pro Request.
- **Versionen:** google-api-java-client.
- **FIX:** EIN `Drive`-Service + EIN `NetHttpTransport` app-weit teilen (Connection-Reuse), aber **pro Coroutine ein eigenes Request-/Uploader-Objekt** (`service.files().create(...)` je Task neu). Credentials sind thread-safe.
- **Quelle:** https://docs.cloud.google.com/java/docs/reference/google-api-client/latest/com.google.api.client.googleapis.services.AbstractGoogleClientRequest

### U8. `MediaHttpUploader` resumed nicht über Prozessgrenzen
- **Symptom:** Nach App-Kill startet der nächste Lauf den Upload bei Byte 0.
- **Ursache:** Der High-Level-Uploader resumed nur innerhalb eines laufenden `upload()`-Aufrufs; persistiert den Session-URI nicht.
- **Versionen:** google-api-java-client.
- **FIX:** Delta-Sync auf Datei-Ebene (nur fehlende Dateien je Lauf) — abgebrochener Batch setzt an der halb hochgeladenen Menge an. Für sehr große Einzeldateien Resumable-Protokoll selbst fahren (Session-URI + Offset persistieren).
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/897

### U9. Upload im ViewModel-/Activity-Scope bricht bei App-Wechsel ab ⭐ HAEUFIG
- **Symptom:** Backup hängt/stoppt, sobald die App in den Hintergrund geht.
- **Ursache:** `viewModelScope`-Coroutinen werden bei `onCleared()` (Activity-Zerstörung/Memory-Druck) gecancelt.
- **Versionen:** alle.
- **FIX:** Upload in `CoroutineWorker` (WorkManager) — übersteht App-Exit/Prozess-Tod/Neustart; Constraints WLAN/Akku; `Result.retry()` + Backoff. ViewModel beobachtet nur `WorkInfo`.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent

### U10. Android 14+: Foreground-Service-Type fehlt
- **Symptom:** `MissingForegroundServiceTypeException`/`ForegroundServiceStartNotAllowedException` beim `setForeground()`.
- **Ursache:** Ab Android 14 (targetSdk 34) muss der FGS-Typ deklariert sein.
- **Versionen:** Android 14+.
- **FIX:** `<service android:foregroundServiceType="dataSync">` + Permission `FOREGROUND_SERVICE_DATA_SYNC`.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### U11. Android 14/15: `dataSync`-FGS 6-Stunden-Budget
- **Symptom:** Nach längerer Laufzeit `ForegroundServiceDidNotStopInTimeException` / kein neuer dataSync-FGS startbar.
- **Ursache:** `dataSync`-FGS dürfen max. 6 h/24 h laufen (ab Android 15 teilen sich alle FGS dieses Budget).
- **Versionen:** Android 14+, verschärft 15.
- **FIX:** Für sehr lange/große Backups **User-Initiated Data Transfer (UIDT) Jobs** statt `dataSync`; ohnehin in Chunks/Delta arbeiten.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

---

## R) Restore & Integritätsprüfung

### R1. Metadaten-Scope darf nicht herunterladen
- **Symptom:** `files.get?alt=media` schlägt fehl.
- **Ursache:** Reiner Metadaten-Scope hat keinen Inhalts-Lesezugriff.
- **Versionen:** v3.
- **FIX:** `drive.appdata` reicht für App-Ordner-Inhalt; vor Download `capabilities.canDownload` prüfen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-downloads

### R2. Range-Download wird von High-Level-Downloadern ignoriert
- **Symptom:** Trotz `Range` kommt die ganze Datei.
- **Ursache:** `MediaIoBaseDownload`/`MediaHttpDownloader` setzen den `Range`-Header nicht durch.
- **Versionen:** google-api-java-client.
- **FIX:** Für echte Teil-Downloads HTTP-Request manuell mit `Range` gegen `…?alt=media`; fürs DB-Restore meist unnötig (ganze DB resumable laden).
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-downloads

### R3. Restore ohne Checksum-Prüfung ersetzt intakte DB durch korrupte ⭐ HAEUFIG
- **Symptom:** Nach Restore „database disk image is malformed".
- **Ursache:** Unvollständiger/korrupter Download wurde ungeprüft über die Live-DB gelegt.
- **Versionen:** alle.
- **FIX:** Metadaten mit `fields=md5Checksum,sha256Checksum,size` holen; lokal über die empfangenen Bytes Hash berechnen, vergleichen → Mismatch = Abbruch (in Temp laden, erst nach Prüfung tauschen). SHA-256 bevorzugen; eigenen Hash zusätzlich in `appProperties`.
- **Quelle:** https://docs.cloud.google.com/storage/docs/data-validation

### R4. Zurückgelassene `-wal`/`-shm` korrumpieren die restaurierte DB ⭐ HAEUFIG
- **Symptom:** Nach Restore alte Daten zurück / „malformed".
- **Ursache:** Nur `.db` ersetzt, alte `…-wal`/`…-shm` liegen gelassen → SQLite spielt alte WAL auf neue DB.
- **Versionen:** alle.
- **FIX:** Beim Swap immer alle drei behandeln: neue `.db` einsetzen UND `…-wal` + `…-shm` löschen. (Backup selbst enthält durch `wal_checkpoint(TRUNCATE)` keine WAL.)
- **Quelle:** https://sqlite.org/forum/forumpost/905eb5e564d4df44

### R5. Datei-Tausch bei offener Room-Verbindung → Crash/„malformed" ⭐ HAEUFIG
- **Symptom:** Crash beim/nach Restore, oder App nutzt weiter die alte DB.
- **Ursache:** Room hält Verbindung + WAL/SHM offen; Singleton zeigt auf alte Instanz.
- **Versionen:** alle.
- **FIX:** `RoomDatabase.close()` + Singleton verwerfen, sicherstellen dass keine Verbindung offen ist, dann tauschen; **App-Neustart** (Process restart) für neue Room-Instanz. Nicht tauschen, während Threads schreiben.
- **Quelle:** https://github.com/rafi0101/Android-Room-Database-Backup

### R6. Schema-Version-Mismatch nach Restore
- **Symptom:** „Room cannot verify data integrity … changed schema but forgot to update version" / Migration-Crash.
- **Ursache:** Backup aus anderer App-Version mit anderer `user_version`/`identityHash`.
- **Versionen:** Room, alle.
- **FIX:** Schema-Version VOR dem Restore prüfen (`appProperties.schemaVersion` oder `PRAGMA user_version` auf Temp). Älter → registrierte `Migration`s greifen lassen; neuer → Restore ablehnen (Update nötig). **Kein** `fallbackToDestructiveMigration()` als Default (löscht restaurierte DB).
- **Quelle:** https://proandroiddev.com/why-room-crashes-when-you-change-your-database-and-how-to-fix-it-ca8e3538bf57

### R7. `integrity_check` erkennt nur strukturelle Schäden
- **Symptom:** DB öffnet, aber Inhalt subtil falsch.
- **Ursache:** `PRAGMA integrity_check`/`quick_check` prüft Struktur, nicht inhaltliche Verfälschung.
- **Versionen:** SQLite.
- **FIX:** `quick_check` auf Temp-Datei als Gate VOR dem Swap **plus** kryptografischen Checksum-Vergleich (R3) — letzterer fängt genau die Fälle, die `integrity_check` nicht sieht.
- **Quelle:** https://sqlite.org/pragma.html

### R8. Teilweises Restore → DB zeigt auf fehlende Fotos
- **Symptom:** Einträge mit fehlenden Bildern / verwaiste Fotos.
- **Ursache:** DB und Fotos zu verschiedenen Zeitpunkten gesichert, oder Restore brach zwischen DB und Fotos ab.
- **Versionen:** eigener Code.
- **FIX:** Gemeinsamen Snapshot-Token in DB- und Foto-Manifest, beim Restore auf Übereinstimmung prüfen; Fotos zuerst vollständig laden+verifizieren, DB-Swap als letzten atomaren Schritt; nach Restore Referenzen reconcilen (fehlende markieren statt crashen).
- **Quelle:** https://developer.android.com/identity/data/autobackup

### R9. SQLCipher: falscher/fehlender Key wirkt wie Korruption
- **Symptom:** „file is not a database"/„malformed", obwohl Datei intakt.
- **Ursache:** Restaurierte verschlüsselte DB mit falschem/leerem Key geöffnet.
- **Versionen:** SQLCipher (falls genutzt).
- **FIX:** Denselben Keystore-Key wie beim Backup nutzen; Key NIE ins Backup legen; auf Temp mit Key öffnen + `PRAGMA cipher_integrity_check` (ab 4.2.0) vor dem Swap.
- **Quelle:** https://commonsware.com/Room/pages/chap-sqlciphermgmt-001.html

### R10. Java-SQLite-API + nativer Code gleichzeitig auf dieselbe Datei (ab Android N)
- **Symptom:** Sporadische „malformed"-Fehler nach Restore.
- **Ursache:** Ab Android N korrumpiert gleichzeitiger Zugriff über zwei SQLite-Stacks die DB (WAL-Locking-Konflikt).
- **Versionen:** Android 7+.
- **FIX:** Datei-Operationen (Löschen/Umbenennen von `.db`/`-wal`/`-shm`) nur ohne offene SQLite-Verbindung; nicht parallel über zwei Stacks zugreifen.
- **Quelle:** https://github.com/jeremysheeley/Android_N_SQLite_Corruption

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-google-drive-backup.md`) |
|---------------|-------------------------------------------------------------------|
| A1–A8 (appDataFolder) | §1 (Grundlagen/Layout) |
| O1–O5 (Orphan/Sync) | §2 (Reconciliation), §3 (Change-Detection) |
| M1–M7 (Multi-Device) | §4 (Konfliktauflösung) |
| AU1–AU6 (Auth/Consent) | §5 (Scopes/Auth-Migration) |
| Q1–Q6 (Quota/Rate) | §6 (Quota & Rate-Limits) |
| U1–U11 (Upload/WorkManager) | §7 (Resumable/Reliability), §8 (WorkManager) |
| R1–R10 (Restore) | §9 (Restore & Integrität) |
