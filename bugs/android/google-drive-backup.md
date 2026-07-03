# Bekannte Bugs: Google-Drive-Backup & Cloud-Sync (Android, appDataFolder)

> PFLICHT-LESEN vor Arbeit am Drive-Backup/Restore (BestJournal: Room-DB + Fotos → appDataFolder).
> Stand: tief recherchiert am 2026-06-14 (zwei Läufe, je 7 Researcher parallel — Doku-Lauf +
> Issue-Tracker-/Vorfall-Lauf; ~145 Einträge inkl. Vertiefung §L/§CH/§GI/§AV/§CM/§RV/§QV/§WM/§INC
> und Fix-Status-Sektion). Versions-Anker:
> `google-api-services-drive:v3-rev20241206-2.0.0` · Drive REST v3 (**Quota-Unit-Modell seit
> 01.05.2026**) · `play-services-auth 21.3.0` (`GoogleAuthUtil`/`GoogleSignIn` deprecated,
> **Entfernung ab Mai 2026**) · `androidx.credentials 1.5.0-alpha06` + `googleid 1.1.1` ·
> compileSdk 35, minSdk 26. Lokale DB ist Source of Truth.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/android/google-drive-backup.md`](../../best-practices/android/google-drive-backup.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **A** appDataFolder · **O** Orphan/Sync · **M** Multi-Device · **AU** Auth · **Q** Quota · **U** Upload · **R** Restore.
> **Vertiefung (Issue-Tracker-Lauf):** **L** HTTP-Client/google-api-java-client · **CH** Changes-API ·
> **GI** generateIds/Revisions · **AV** appDataFolder/Query-Vertiefung · **CM** Credential-Manager/AuthorizationClient ·
> **RV** Room/Restore-Vertiefung · **QV** Quota-Vertiefung · **WM** WorkManager/FGS · **INC** Reale Vorfälle. Fix-Status am Ende.

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
| 21 | Connection-Leak bei 5 parallel | `google-http-client >= 1.35.0` pinnen (gzip+chunked Keep-Alive-Leak) | L5 |
| 22 | Cancel bricht Up/Download nicht ab | Lib-Handler stellt Interrupt-Flag nicht her → eigener Wrapper | L7, L8 |
| 23 | `changes.list` für appData leer | `restrictToMyDrive` weglassen; `spaces=appDataFolder` bei getStartPageToken+list | CH1, CH2 |
| 24 | Paginierung verliert Dateien | Bei leerem `files`-Array NICHT abbrechen — nur wenn `nextPageToken` fehlt | AV1 |
| 25 | `getId()` ≠ E-Mail | ID-Token verifizieren, `email`-Claim lesen; `sub` als Schlüssel | CM6 |
| 26 | `NoCredentialException` | `GetSignInWithGoogleOption` (Button-Flow) statt `GetGoogleIdOption(false)` | CM1 |
| 27 | `authorize()` ok, dann 403 insufficient | Nach authorize `getGrantedScopes()` prüfen (granulare Permissions) | CM10 |
| 28 | `HiltWorker`-Crash nach Prozess-Tod | Default-WM-Initializer entfernen + `Configuration.Provider` (Property!) | WM1 |
| 29 | FGS-Crash Android 14/15 | Service-Typ im Manifest mergen; `onTimeout`/StopReason behandeln | WM4, WM6 |
| 30 | Backup „aktiviert" aber tot | Zeitstempel des letzten ERFOLGREICHEN Backups zeigen + warnen | INC1, INC2 |
| 31 | Sign-In `DEVELOPER_ERROR` (Code 10) nach Keystore-Wechsel | Neuen SHA-1 in Cloud Console (Android-Client, Package **inkl. `.debug`**) hinterlegen; neue „Google Auth Platform"-UI hat nur EIN SHA-Feld → alten Wert **überschreiben** | CM8 |

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

# ── Vertiefung (Issue-Tracker-/Vorfall-Lauf 2026-06-14) ──

## L) HTTP-Client / google-api-java-client (Upload/Download-Schicht)

### L1. `308`-Resume ohne `Range`/`Content-Range` → Upload startet von 0
- **Symptom:** Nach Netzwechsel lädt die Datei jedes Mal komplett neu statt fortzusetzen.
- **Ursache:** `MediaHttpUploader` erwartet im Status-Request immer `Content-Range`; fehlt es (legitim erlaubt), fällt er auf „von vorn" zurück statt zu pollen.
- **Versionen:** 1.18.0-rc und älter/neuer; offen.
- **FIX:** Fehlendes Range als „0 Bytes bestätigt" werten, **Session-URI behalten**, nicht neue Session öffnen. Für echtes Cross-Prozess-Resume Protokoll selbst fahren.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/897

### L2. Status-Probe `Content-Range: bytes */<total>` → 500 statt 308
- **Symptom:** Resume scheitert reproduzierbar mit 500 im `serverErrorCallback()`.
- **Ursache:** Probe-Request sendet Gesamtlänge, Server akzeptiert in der Konstellation nur `*/*`.
- **Versionen:** ab 1.10.3-beta.
- **FIX:** Bei unbekanntem Server-Stand `Content-Range: bytes */*` senden; konkrete Grenzen erst nach `Range`-Header des 308.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/566

### L3. `insufficient data written` beim Streaming-Upload
- **Symptom:** `IOException: insufficient data written` in `StreamingOutputStream.close()`.
- **Ursache:** Angekündigte Content-Length ≠ tatsächlich aus dem Stream gelieferte Bytes (Fixed-Length-Streaming erzwingt exakte Zahl).
- **Versionen:** ab 1.15.0-rc.
- **FIX:** Länge exakt aus der Quelle (`file.length()` unmittelbar vor Upload); für Streams `setRetrySupported(true)` + resettable Stream.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/781

### L4. 4-KB-Copy-Buffer drosselt Upload auf ~256 KB/s (SSL-Record-Overhead)
- **Symptom:** Upload nie schneller als ~256 KB/s; große Dateien laufen in die 1-h-Grenze.
- **Ursache:** `AbstractInputStreamContent.writeTo()` kopiert in 4-KB-Häppchen + flusht je Block → jeder Block ein SSL-Record.
- **Versionen:** Default über viele Versionen; Issue **WON'T FIX**.
- **FIX:** `writeTo(OutputStream)` mit **16-KB-Buffer** überschreiben (SSL-Max-Record) → ~4× Durchsatz.
- **Quelle:** https://github.com/googleapis/google-http-java-client/issues/278

### L5. chunked + gzip → Keep-Alive bricht / Connection-Leak ⭐ HAEUFIG
- **Symptom:** Unter Parallelität (Semaphore 5) sammeln sich tote Verbindungen; gelegentlich `EOFException`.
- **Ursache:** `GZIPInputStream.read()` liefert `-1` vor dem letzten Chunk-Header → Socket wird nicht recycelt.
- **Versionen:** **GEFIXT in google-http-client 1.35.0** (PR #990).
- **FIX:** `google-http-client >= 1.35.0` **explizit pinnen** (transitive Alt-Version via altem Drive-Artefakt vermeiden).
- **Quelle:** https://github.com/googleapis/google-http-java-client/issues/367

### L6. Gechunkter Download gzip-komprimierter Objekte korrumpiert
- **Symptom:** Range-/Chunk-Download liefert korrupten/abgeschnittenen Inhalt.
- **Ursache:** `HttpResponse.getContent()` legt automatisch `GZIPInputStream` über den Stream; ein Range-Fragment ist kein vollständiger gzip-Stream.
- **Versionen:** generisch; Abschalter Feature-Request offen (#568).
- **FIX:** Auto-Dekompression für Chunk-Downloads abschalten / Rohbytes konkatenieren, erst am Ende dekomprimieren.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/1009

### L7. Backoff-Handler stellen Interrupt-Flag nicht wieder her → Cancel hängt ⭐ HAEUFIG
- **Symptom:** Coroutine-Cancel/App-Pause während Retry-Sleep wirkt nicht; hängende Retries trotz Cancel.
- **Ursache:** `catch (InterruptedException)` ohne `Thread.currentThread().interrupt()` → Interrupt geht verloren (kooperatives Cancellation bricht).
- **Versionen:** ≤ 1.34.2.
- **FIX:** Eigenen `HttpUnsuccessfulResponseHandler`/`HttpIOExceptionHandler`-Wrapper, der das Interrupt-Flag restauriert + `false` zurückgibt.
- **Quelle:** https://github.com/googleapis/google-http-java-client/issues/1005

### L8. `MediaHttpDownloader` ist nicht unterbrechbar
- **Symptom:** Laufender Download lässt sich nicht abbrechen.
- **Ursache:** Chunk-Schleife prüft kein `Thread.interrupted()`.
- **Versionen:** offen (adressiert via #1101).
- **FIX:** Download in abbruchbaren Wrapper hüllen, zwischen Chunks `isActive` prüfen.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/1051

### L9. Verschachtelte Retry-Ebenen → bis ~10 Min Retries
- **Symptom:** Upload/Download hängt 10 Min in Retries.
- **Ursache:** `MediaHttpUploader`-Retry × `UnsuccessfulResponseHandler` × `IOExceptionHandler` multiplizieren sich.
- **Versionen:** mehrere.
- **FIX:** Retry auf EINE Ebene konsolidieren; `ExponentialBackOff` mit hartem `setMaxElapsedTimeMillis`; pro Request frische Handler/BackOff-Instanz.
- **Quelle:** https://github.com/googleapis/google-http-java-client/issues/238

### L10. Default-gzip auf leerem Body bricht PUT/PATCH/POST (411/501)
- **Symptom:** Metadaten-Patch / Resumable-Init-POST mit leerem Body → 411/501 an strengen Proxys.
- **Ursache:** Library setzt default `Content-Encoding: gzip` auch bei leerem Body; `Content-Length` inkonsistent.
- **Versionen:** durchgehend.
- **FIX:** Bei leerem Body `request.setDisableGZipContent(true)`.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/1548

### L11. `getMediaHttpDownloader()` liefert null → NPE
- **Symptom:** NPE beim Setzen von Chunk-Größe/Progress-Listener.
- **Ursache:** Downloader nur lazy via `initializeMediaDownload()` initialisiert.
- **Versionen:** gemeldet.
- **FIX:** Download über `executeMediaAndDownloadTo(out)` (initialisiert intern) oder `MediaHttpDownloader` selbst instanziieren.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/906

### L12. ApacheHttpTransport-Pool: infinite Keep-Alive + kein Stale-Check → `NoHttpResponseException`
- **Symptom:** Nach Idle sporadisch `NoHttpResponseException`/Connection reset.
- **Ursache:** Pool nimmt unendliche Keep-Alive an, prüft Verbindungen nicht auf Staleness.
- **Versionen:** Default über viele Versionen.
- **FIX:** Auf Android **`NetHttpTransport`** statt Apache; sonst endliche Keep-Alive-TTL + `validateAfterInactivity`.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/1060

### L13. Auth-Initializer-Chaining bricht Token → 401 ⭐ HAEUFIG
- **Symptom:** `401 Unauthorized` trotz gültiger Credential.
- **Ursache:** Eigener `HttpRequestInitializer` ruft `credential.initialize(request)` nicht (oder in falscher Reihenfolge) → kein `Authorization`-Header; eigener `UnsuccessfulResponseHandler` überschreibt zudem den 401-Refresh-Handler.
- **Versionen:** Pattern-Fehler.
- **FIX:** Im eigenen Initializer **zuerst** `delegate.initialize(request)`, **dann** Timeouts/Header; Token-Refresh-Handler mitketten.
- **Quelle:** https://github.com/googleapis/google-api-java-client/issues/1430

## CH) Changes-API (Multi-Device-Delta)

### CH1. `restrictToMyDrive=true` löscht appDataFolder-Changes aus dem Feed ⭐ HAEUFIG
- **Symptom:** `changes.list` liefert für Backups nie Änderungen, `newStartPageToken` rückt aber vor (stiller Feed-Verlust).
- **Ursache:** `restrictToMyDrive` lässt appDataFolder (außerhalb My Drive) komplett weg.
- **Versionen:** v3, per Design.
- **FIX:** Für appData nie `restrictToMyDrive=true` (Default false weglassen); `spaces=appDataFolder` setzen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-changes

### CH2. `getStartPageToken` braucht denselben `spaces` wie `changes.list`
- **Symptom:** Changes verspätet/doppelt/fehlend.
- **Ursache:** Token für Default-Korpus passt nicht zum appData-Feed.
- **Versionen:** v3.
- **FIX:** `spaces=appDataFolder` bei beiden Calls identisch; Token pro Space getrennt persistieren.
- **Quelle:** https://developers.google.com/workspace/drive/api/reference/rest/v3/changes/getStartPageToken

### CH3. Frischer `startPageToken` zieht ~2 Monate alte Changes mit
- **Symptom:** Erster Sync verarbeitet längst erledigte „Änderungen", ggf. Doppel-Uploads.
- **Ursache:** Token referenziert Change-Liste mit Historie, nicht den Jetzt-Zustand.
- **Versionen:** beobachtet.
- **FIX:** Zuletzt verarbeiteten `newStartPageToken` dauerhaft speichern; Changes idempotent (per `fileId`+`version`) verarbeiten.
- **Quelle:** https://www.emptor.io/blog/demystifying-the-google-drive-changes-api

### CH4. Datei lesen/öffnen erzeugt beim Polling einen Change
- **Symptom:** Backup taucht im Feed auf, obwohl nur gelesen.
- **Ursache:** Öffnen ändert Metadaten (`viewedByMeTime`) → Change-Item beim Polling (bei `watch` anders).
- **Versionen:** beobachtet.
- **FIX:** Auf echten Zustand (`md5Checksum`/`version`/`size`) prüfen, nicht auf bloße Feed-Präsenz.
- **Quelle:** https://www.emptor.io/blog/demystifying-the-google-drive-changes-api

### CH5. `includeRemoved` default true → `change.file` null → NPE
- **Symptom:** NPE beim Lesen von `change.file.*` bei Removal-Changes.
- **Ursache:** Default `includeRemoved=true`; gelöschte/unzugängliche Dateien liefern `file=null`.
- **Versionen:** v3.
- **FIX:** Zuerst `change.getRemoved()` prüfen; Removal als Cache-Invalidierung behandeln; ggf. `includeRemoved=false`.
- **Quelle:** https://googleapis.github.io/google-api-python-client/docs/dyn/drive_v3.changes.html

### CH6. Changes-Feed kann Duplikate enthalten
- **Symptom:** Derselbe `fileId` mehrfach (v. a. bei watch+poll-Überlappung).
- **Ursache:** Keine Dedup-Garantie seitens Google.
- **Versionen:** per Design.
- **FIX:** Idempotent verarbeiten — letzte `version`/`modifiedTime` pro `fileId` halten, Ältere/Gleiche überspringen.
- **Quelle:** https://www.emptor.io/blog/demystifying-the-google-drive-changes-api

## GI) generateIds & Revisions (Vertiefung)

### GI1. Pre-Generated File-ID + Retry → 409 (Idempotenz-Feature) ⭐ wertvoll
- **Symptom:** `create`-Retry mit derselben Vorab-ID gibt 409 Conflict.
- **Ursache:** Idempotenz-Garantie — erster Upload war serverseitig erfolgreich, kein Duplikat.
- **Versionen:** v3.
- **FIX:** Genau nutzen: ID via `generateIds` ziehen, persistieren, immer mitgeben; 409 = „bereits hochgeladen" → per `get` verifizieren, nicht neu hochladen.
- **Quelle:** https://blog.kiprosh.com/google-drive-file-upload-pre-generated-file-id-for-uploads/

### GI2. generateIds braucht `space=appDataFolder` (Default `drive`)
- **Symptom:** Im falschen Space verankerte ID → create schlägt fehl/landet falsch.
- **Ursache:** `space`-Parameter Teil der Reservierung, Default `drive`.
- **Versionen:** v3.
- **FIX:** `space=appDataFolder` setzen, passend zum Ziel.
- **Quelle:** https://developers.google.com/workspace/drive/api/reference/rest/v3/files/generateIds

### GI3. generateIds nicht für Workspace-Typen
- **Symptom:** Vorab-ID für Google-Doc/Sheet schlägt fehl.
- **Ursache:** Nur Blobs + `drive-sdk`/`folder` unterstützt.
- **Versionen:** v3.
- **FIX:** Nur für binäre Backups/Ordner nutzen (euer Fall); für Workspace-MIME ID weglassen.
- **Quelle:** https://developers.google.com/workspace/drive/api/reference/rest/v3/files/generateIds

### GI4. `keepForever`-Limit 200 + nicht per Update auf false rücksetzbar
- **Symptom:** 201. Pin schlägt fehl; `keepForever=false` → `illegalKeepForeverModification`.
- **Ursache:** Max 200 keepForever-Revisionen/Datei; gepinnte Blob-Revision nur download-/löschbar.
- **Versionen:** v3.
- **FIX:** Pin-Budget verwalten; zum „Entpinnen" Revision **permanent löschen** (`revisions.delete`); zählen gegen 15 GB.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/manage-revisions

### GI5. v2 `pinned` → v3 `keepForever`/Upload `keepRevisionForever`
- **Symptom:** v2-Code findet `pinned` nicht; Pin per Upload greift nicht.
- **Ursache:** Feld umbenannt; Upload-Query heißt `keepRevisionForever`.
- **Versionen:** v3.
- **FIX:** Durchgängig `keepForever` (Revision) bzw. `keepRevisionForever` (Upload-Query); v2-`pinned` entfernen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/v3versusv2

## AV) appDataFolder / Query (Vertiefung)

### AV1. Paginierung: leeres `files: []` + nicht-null `nextPageToken` → Dateien fehlen ⭐ HAEUFIG
- **Symptom:** Code bricht bei leerer Seite ab und verliert echte Dateien.
- **Ursache:** Filterung teils nach Token-Generierung; eine Seite kann komplett herausgefiltert sein.
- **Versionen:** gemeldet (Issue 406305173).
- **FIX:** Nur abbrechen, wenn `nextPageToken` fehlt/null — leere Seiten mit Token weiter paginieren.
- **Quelle:** https://issuetracker.google.com/issues/406305173

### AV2. `modifiedTime`-Filter braucht RFC-3339 + Quotes
- **Symptom:** `modifiedTime > 2024-01-01` → 400 invalid query.
- **Ursache:** Datum muss RFC 3339 in Single-Quotes: `modifiedTime > '2026-06-14T00:00:00Z'`.
- **Versionen:** v3.
- **FIX:** Voll quoten + UTC normalisieren.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/search-files

### AV3. `appProperties`-Query nur mit OAuth (kein API-Key) + `has`-Syntax
- **Symptom:** `appProperties has {...}` liefert nichts / scheitert mit API-Key.
- **Ursache:** appProperties privat, nur per OAuth-Client lesbar; strikte `has`-Syntax.
- **Versionen:** v3.
- **FIX:** OAuth-Token (via `drive.appdata`); `appProperties has { key='k' and value='v' }` exakt.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/search-files

### AV4. `storageQuota.limit` fehlt bei Unlimited-Storage → NPE/Fehlrechnung
- **Symptom:** „Freier Platz"-Berechnung wirft NPE oder absurde Werte.
- **Ursache:** `limit` ist bei unbegrenztem Speicher **abwesend** (nicht 0); bei Pooled Storage = Org-Limit.
- **Versionen:** v3.
- **FIX:** Abwesenheit als „unbegrenzt" behandeln; Quota-Check nur bei vorhandenem `limit`.
- **Quelle:** https://googleapis.dev/java/google-api-services-drive/latest/com/google/api/services/drive/model/About.StorageQuota.html

### AV5. Eventual Consistency: frisch erstellte Datei fehlt im sofortigen `files.list`
- **Symptom:** Nach `create` taucht die Datei im direkt folgenden List nicht auf (wirkt verloren); kurzzeitig 404 bei `get`.
- **Ursache:** List-Index propagiert verzögert.
- **Versionen:** beobachtet.
- **FIX:** Nach `create` die **zurückgegebene `fileId`** direkt verwenden/persistieren statt per List zu verifizieren; List-Verify nur mit Retry.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/appdata

### AV6. `supportsAllDrives`/`corpora` bei appData fehl am Platz
- **Symptom:** Unerwartete Ergebnisse, wenn man Shared-Drive-Parameter „zur Sicherheit" mitsetzt.
- **Ursache:** appData ist eigener Space, kein Shared Drive; `corpora` default `user`.
- **Versionen:** v3.
- **FIX:** Für appData nur `spaces=appDataFolder`; `supportsAllDrives`/`corpora`/`driveId` weglassen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/enable-shareddrives

### AV7. appDataFolder hängt an OAuth-Client/Projekt-Identität → Waisen nach Signatur-/Client-Wechsel
- **Symptom:** Nach Reinstall mit anderer Client-Config/Signatur ist der alte appData-Inhalt unzugänglich, belegt aber die 15 GB.
- **Ursache:** „Only the application that created the data can access it" — app-/projektgebunden.
- **Versionen:** per Design.
- **FIX:** Package-Name + Release-Signatur-SHA-1 + GCP-Projekt/Client-ID über Versionen **stabil** halten; Signatur-Key nie wechseln.
- **Quelle:** https://support.google.com/drive/thread/240427694

### AV8. `'`/`\` im `q` müssen escaped werden
- **Symptom:** Name mit Apostroph → 400 invalid query / kein Treffer.
- **Ursache:** Single-Quote-Werte; `'`→`\'`, `\`→`\\`.
- **Versionen:** v3.
- **FIX:** Zentrale Escape-Funktion; besser gar keine Sonderzeichen in Backup-Dateinamen.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/search-files

## CM) Credential Manager / AuthorizationClient (Auth-Vertiefung)

### CM1. `NoCredentialException` trotz `setFilterByAuthorizedAccounts(false)` ⭐ HAEUFIG
- **Symptom:** `NoCredentialException: No credentials available`; kein Account-Picker, kein „Account hinzufügen".
- **Ursache:** Credential-Manager-Designlücke (kein Sign-up-Fallback wie altes GoogleSignIn).
- **Versionen:** ≥ 1.3.0-alpha03, auch 1.5.0-alpha.
- **FIX:** Zweistufig: `GetGoogleIdOption(filterByAuthorizedAccounts=true)` → bei Exception Retry mit **`GetSignInWithGoogleOption`** (zeigt vollen Picker); sonst `Settings.ACTION_ADD_ACCOUNT`.
- **Quelle:** https://github.com/android/identity-samples/issues/53

### CM2. `NoCredentialException` bei deaktiviertem globalem „Sign in with Google"
- **Symptom:** `... 16: [28439] User disabled the feature`; Bottom-Sheet erscheint nie.
- **Ursache:** Globaler Konto-Schalter deaktiviert unterdrückt das Bottom-Sheet für alle Accounts.
- **Versionen:** aktiv.
- **FIX:** Button-Flow (`GetSignInWithGoogleOption`) statt Bottom-Sheet; Nutzer zu Konto-Einstellungen leiten.
- **Quelle:** https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide

### CM3. `TransactionTooLargeException` bei mehreren Accounts (Android 14+)
- **Symptom:** Kein Sign-in-Dialog bei mehreren Google-Accounts (`GetGoogleIdOption`).
- **Ursache:** Binder-IPC-Payload zu groß; Google-Bug 341690734.
- **Versionen:** **GEFIXT ab GMS 24.40.XX**.
- **FIX:** GMS ≥ 24.40 anheben; übergangsweise `GetSignInWithGoogleOption`.
- **Quelle:** https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide

### CM4. `GetCredentialProviderConfigurationException` — „no provider dependencies found"
- **Symptom:** Flow bricht komplett ab.
- **Ursache:** `credentials-play-services-auth` fehlt ODER GMS älter als gefordert.
- **Versionen:** aktiv.
- **FIX:** `credentials` UND `credentials-play-services-auth` in identischer Version (1.5.0-alpha06); aktuelles GMS-Image.
- **Quelle:** https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide

### CM5. `NoCredentialException` nur im `BOTTOM_SHEET`-Modus (Regression)
- **Symptom:** Crash nur bei One-Tap-Bottom-Sheet, Button-Flow ok.
- **Ursache:** Regression in `CredentialProviderFrameworkImpl`.
- **Versionen:** offen/Workaround.
- **FIX:** Dialog-/Button-Flow (`GetSignInWithGoogleOption`).
- **Quelle:** https://github.com/supabase-community/supabase-kt/issues/659

### CM6. `getId()` liefert numerische ID statt E-Mail ⭐ HAEUFIG
- **Symptom:** Beim zweiten Login liefert `getId()` die `sub`-ID (oder leer) statt E-Mail.
- **Ursache:** `getId()` = `sub`-Feld des ID-Tokens, nicht garantiert E-Mail.
- **Versionen:** ab credentials 1.2.2 (Issue 368873078).
- **FIX:** `getId()` nie als E-Mail; ID-Token verifizieren, `email`-Claim lesen; `sub` als Schlüssel.
- **Quelle:** https://github.com/android/identity-samples/issues/98

### CM7. `nonce`-Mismatch beim Token-Verify
- **Symptom:** „Passed nonce and nonce in id_token should either both exist or not."
- **Ursache:** Inkonsistente Nonce-Handhabung Client↔Server.
- **Versionen:** aktiv.
- **FIX:** Nonce konsistent (beide Seiten gleicher Hash) oder gar nicht setzen.
- **Quelle:** https://www.codestudy.net/blog/error-10-developer-console-is-not-set-up-correctly-not-using-firebase-one-tap-sign-up/

### CM8. Error 10 (DEVELOPER_ERROR) — Web- vs. Android-Client-ID / SHA-1 ⭐ HAEUFIG
- **Symptom:** `ApiException 10` „developer console not set up correctly".
- **Ursache:** `setServerClientId()` braucht die **Web**-Client-ID; SHA-1 (Debug+Release) fehlt/falsch.
- **Versionen:** durchgehend.
- **FIX:** Web-Client-ID eintragen; Web- + Android-OAuth-Client anlegen; beide SHA-1 (Debug-Keystore + Play-App-Signing) hinterlegen.
- **Quelle:** https://www.codestudy.net/blog/error-10-developer-console-is-not-set-up-correctly-not-using-firebase-one-tap-sign-up/
- **PRAXIS-FALL (Entropie Reductor, 2026-07-03):** Reiner `GoogleSignIn`-Flow `DEFAULT_SIGN_IN` + `requestScopes(DRIVE_APPDATA)` OHNE Firebase / OHNE `requestIdToken` braucht **keine** Web-Client-ID — er matcht den Android-OAuth-Client allein ueber **Package-Name + SHA-1**. Nach Wechsel auf einen **gemeinsamen Debug-Keystore** aenderte sich der SHA-1; der in der Cloud Console hinterlegte (alte) SHA-1 passte nicht mehr → Code 10. Zwei Fallen dabei: (1) Debug-Build mit `applicationIdSuffix ".debug"` → der Android-OAuth-Client muss auf den ECHTEN Package `…​.debug` lauten (nicht den Basis-Package). (2) Die neue **„Google Auth Platform"-Oberflaeche erlaubt pro Android-Client nur EINEN SHA-1** (kein „SHA-1 hinzufuegen" wie in der alten UI) → den vorhandenen Wert **ueberschreiben**; wer zwei Keys parallel braucht, legt **zwei Android-Clients** an (gleicher Package, je 1 SHA-1). Das appDataFolder-Backup bleibt erhalten (an Package + Cloud-Projekt gebunden, NICHT an den SHA-1 — vgl. AV7). SHA-1 auslesen: `keytool -list -v -keystore <keystore> -alias androiddebugkey -storepass android`.

### CM9. Fehlkonfiguration getarnt als `GetCredentialCancellationException`
- **Symptom:** „Activity is cancelled by the user", obwohl Nutzer nichts tat; hohe „Abbruch"-Rate.
- **Ursache:** Falscher Scope/Client-ID öffnet die UI nicht sauber.
- **Versionen:** dokumentiert.
- **FIX:** Bei hoher Cancel-Rate Config prüfen (Client-ID/Scopes/SHA-1), nicht auto-retryen.
- **Quelle:** https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide

### CM10. Nach `authorize()` werden nicht alle Scopes geprüft (granulare Permissions) ⭐ HAEUFIG
- **Symptom:** `403 insufficient permissions` trotz erfolgreichem `authorize()`.
- **Ursache:** Nutzer kann `drive.appdata` im Consent abwählen; `AuthorizationResult` kommt trotzdem „erfolgreich".
- **Versionen:** aktiv.
- **FIX:** `authorizationResult.getGrantedScopes()` gegen benötigte prüfen, bevor API-Call; sonst inkrementell nachfordern.
- **Quelle:** https://developers.google.com/identity/protocols/oauth2/resources/granular-permissions

### CM11. `getAuthorizationResultFromIntent()` wirft `ApiException`/NPE bei Abbruch
- **Symptom:** ApiException (10/16) oder NPE bei `RESULT_CANCELED`.
- **Ursache:** Kein Result-Extra bei nicht-OK; Methode wirft grundsätzlich `ApiException` ohne Ergebnis.
- **Versionen:** aktiv.
- **FIX:** `StartIntentSenderForResult`-Contract; zuerst `resultCode==RESULT_OK` + `data!=null`, dann in try/catch(`ApiException`) `statusCode` auswerten.
- **Quelle:** https://developer.android.com/identity/authorization

### CM12. `include_granted_scopes=true` (Default) bündelt Scopes / revoke entzieht alles
- **Symptom:** Token deckt mehr Scopes ab als angefragt; Revoke entzieht alle.
- **Ursache:** Default true re-präsentiert alle je gewährten Scopes.
- **Versionen:** API neu ab play-services-auth 21.3.0.
- **FIX:** Für isolierte Grants `setOptOutOfIncludeGrantedScopes(true)`.
- **Quelle:** https://developers.google.com/android/guides/releases

### CM13. Kein Silent Sign-In mehr (Regression ggü. `silentSignIn()`)
- **Symptom:** Bei jedem Start Account-Auswahl; `setAutoSelectEnabled` hilft kaum.
- **Ursache:** Credential Manager ist keine Session-DB.
- **Versionen:** Designänderung.
- **FIX:** Eigenen Login-State (verschlüsselt) persistieren; stilles Re-Auth via `filterByAuthorizedAccounts(true)`+`autoSelect`, Picker nur Fallback.
- **Quelle:** https://github.com/android/identity-samples/issues/53

### CM14. Pre-Migration-Accounts erscheinen nicht in `filterByAuthorizedAccounts(true)`
- **Symptom:** Alte (via GoogleSignIn autorisierte) Accounts fehlen im Picker.
- **Ursache:** „authorized accounts"-Liste deckt alte Autorisierungen nicht ab.
- **Versionen:** aktiv.
- **FIX:** Nach Migration einmalig vollen Picker (`GetSignInWithGoogleOption`) anbieten; Migrations-Hinweis in UI.
- **Quelle:** https://github.com/android/identity-samples/issues/53

### CM15. `androidx.credentials` Alpha-API-Drift (1.5.0-alpha06)
- **Symptom:** `Unresolved reference 'pendingIntent'` u. ä. — Tutorials kompilieren nicht.
- **Ursache:** API-Oberfläche ändert sich zwischen Alphas; keine Stabilitätsgarantie.
- **Versionen:** Alpha-inhärent.
- **FIX:** Gegen exakte Version entwickeln; PendingIntent via `ActivityResultLauncher`-Flow, nicht aus Exception; für Prod stabile credentials-Version erwägen.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/credentials

### CM16. `drive.appdata`-Einstufung (Korrektur/Widerspruch)
- **Symptom:** Unsicherheit, ob `drive.appdata` CASA/Audit braucht.
- **Ursache:** Widersprüchliche Quellen — Drive-Doku „api-specific-auth" listet `drive.appdata` als **non-sensitive** (kein CASA); ein Researcher fand jedoch eine Quelle, die es als restricted bezeichnet.
- **Versionen:** Stand 2026.
- **FIX:** Maßgeblich ist die offizielle Drive-Tabelle „Non-sensitive scopes" → nur Brand-/Basis-Verifizierung. **Vor Release verifizieren** und im Zweifel direkt an der Google-Primärquelle prüfen; Verifizierung früh starten (Privacy-Policy/Support-Mail erreichbar). `drive.file` als Alternative, falls je Zweifel.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/api-specific-auth · https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification

## RV) Room / Restore (Vertiefung)

### RV1. Hilt-`@Singleton` hält alte DB-Instanz → Restore wirkungslos bis Neustart ⭐ HAEUFIG
- **Symptom:** Nach Datei-Tausch zeigt die App weiter alte Daten; erst nach Neustart neu.
- **Ursache:** `@Singleton`-DB einmal gebaut; injizierte DAOs/ViewModels halten die alte Instanz.
- **Versionen:** alle Room/Hilt.
- **FIX:** DB hinter Holder/`Provider` (`@Volatile`) injizieren, beim Restore `close()`→tauschen→neu bauen; pragmatisch echter Prozess-Kill + Relaunch (nicht nur `startActivity`).
- **Quelle:** https://github.com/rafi0101/Android-Room-Database-Backup

### RV2. `no such table: room_table_modification_log` nach Restore
- **Symptom:** Crash beim ersten DAO-Zugriff (oft Nicht-Primär-Connection).
- **Ursache:** Room-TEMP-Tabelle (connection-lokal); mit WAL+Multi-Connection/`enableMultiInstanceInvalidation` inkonsistent nach reopen/Tausch.
- **Versionen:** Issue 67757002 offen; SQLCipher #640.
- **FIX:** Nach Restore DB komplett schließen+neu bauen; `enableMultiInstanceInvalidation` nur bei echtem Mehrprozess.
- **Quelle:** https://issuetracker.google.com/issues/67757002

### RV3. `File.delete()`/`renameTo()` lässt `-wal`/`-shm` zurück → WAL-Replay korrumpiert
- **Symptom:** Alte Daten wieder da / „malformed" nach Restore.
- **Ursache:** Sidecars bleiben; SQLite spielt alte WAL-Frames auf neue DB.
- **Versionen:** geräteübergreifend (expo #43441).
- **FIX:** `context.deleteDatabase(DB_NAME)` (löscht alle drei Sidecars) statt manuell; sicherstellen, dass das Backup keine WAL mitbringt.
- **Quelle:** https://github.com/expo/expo/issues/43441

### RV4. `wal_checkpoint(TRUNCATE)` gibt `SQLITE_BUSY` (Spalte 0 = 1) → Backup unvollständig
- **Symptom:** Checkpoint „lief", aber Backup ist veraltet / `-wal` bleibt.
- **Ursache:** Andere offene Connection hält Lock → kein exklusiver Checkpoint; Busy-Handler wird NICHT aufgerufen; Rückgabewert ignoriert.
- **Versionen:** alle Room.
- **FIX:** Rückgabe-Cursor lesen (Spalte 0: 1=busy → abbrechen/retry); vor Checkpoint alle anderen Connections schließen; sicherster Weg: DB `close()` (checkpointet+entfernt Sidecars) und die saubere Single-File sichern.
- **Quelle:** https://sqlite.org/forum/info/6a66501e4df030ae

### RV5. `identityHash`-Mismatch trotz gleicher Version
- **Symptom:** `Pre-packaged database has an invalid schema`/`cannot verify data integrity` bei identischer `version`.
- **Ursache:** `identity_hash` aus exakter Schema-Struktur (Spaltenreihenfolge/Index/Collation); anderer Build/KSP/Room-Version → anderer Hash; `exportSchema=false`.
- **Versionen:** alle Room 2.x.
- **FIX:** Nur Room-selbst-erzeugte Dateien desselben Builds restaurieren; App-Build ins Manifest; Restore via Kopie nach `getDatabasePath()` (umgeht Prepackaged-Validierung) statt `createFromFile()`.
- **Quelle:** https://issuetracker.google.com/issues/63872392

### RV6. `fallbackToDestructiveMigration` + altes Backup → Daten gelöscht
- **Symptom:** Nach Restore eines älteren Backups DB leer.
- **Ursache:** Migrations-Mismatch ohne Pfad → Destructive-Fallback löscht restaurierte Daten.
- **Versionen:** Room 2.2.0+.
- **FIX:** Aus Prod entfernen, echte `Migration`s; Backup-Version prüfen (neuer→ablehnen, älter→Migrationspfad nötig).
- **Quelle:** https://proandroiddev.com/why-room-crashes-when-you-change-your-database-and-how-to-fix-it-ca8e3538bf57

### RV7. Auto-Migration nach Restore eines älteren Backups schlägt fehl
- **Symptom:** „Migration didn't properly handle…" / Auto-Migration übersprungen.
- **Ursache:** Auto-Migration braucht Schema-JSONs beider Versionen; fehlt das alte → Lücke.
- **Versionen:** Room 2.4.0+.
- **FIX:** Schema-JSON jeder veröffentlichten Version behalten+ausliefern; lückenlose Kette + `@RenameColumn`/`@DeleteColumn`; defensive manuelle `Migration`-Fallbacks.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### RV8. Scoped Storage: `EACCES` beim Lesen des Backups via `file://` (API 30+)
- **Symptom:** `open failed: EACCES` beim Restore aus geteiltem Speicher.
- **Ursache:** targetSdk 30+ sperrt `file://`-Zugriff auf geteilten Speicher; minSdk-26-Code läuft auf neuen Geräten in die Falle.
- **Versionen:** Android 11+.
- **FIX:** Backup als `content://` via SAF/`ContentResolver.openInputStream` → cacheDir → `getDatabasePath()`; `takePersistableUriPermission`.
- **Quelle:** https://developer.android.com/training/data-storage

### RV9. `renameTo()`/Datei-Tausch `EBUSY` bei offenem Handle / über FS-Grenzen
- **Symptom:** `EBUSY: resource busy, rename` oder `renameTo` gibt false (FAT32/Samsung).
- **Ursache:** Offenes Handle (Room nicht ganz geschlossen) / Rename über Mount-Grenzen.
- **Versionen:** geräte-/FS-abhängig.
- **FIX:** Vor Tausch alle Connections schließen; per Stream ins Ziel-FS kopieren, dann innerhalb `databases/` umbenennen; bei EBUSY Retry + `deleteDatabase()`.
- **Quelle:** https://blog.substitute.tech/blog/20141222-Android-EBUSY-Exception.html

### RV10. `InvalidationTracker`/Flow emittiert nach Restore nicht
- **Symptom:** UI/Flows zeigen alten Stand nach Restore ohne Neustart.
- **Ursache:** Tracker-Zustand in In-Memory-TEMP-Tabelle der alten Connection; Datei-Tausch ohne Transaktion triggert nichts.
- **Versionen:** alle Room.
- **FIX:** DB neu bauen + Observer auf neuer Instanz; sonst `refreshVersionsAsync()`/triviale Schreibtransaktion; StateFlow-Caches zurücksetzen.
- **Quelle:** https://developer.android.com/reference/android/arch/persistence/room/InvalidationTracker

### RV11. WAL-Mode erzwingt 3-Datei-Sicht — nur `.db` sichern = stiller Datenverlust
- **Symptom:** Restore liefert veralteten Stand.
- **Ursache:** Committed-Transaktionen stehen in `-wal` bis zum Checkpoint; Room nutzt WAL default.
- **Versionen:** alle Room.
- **FIX:** Vor Backup `close()` (finaler Checkpoint+Sidecar-Entfernung) ODER `wal_checkpoint(TRUNCATE)` mit Rückgabe-Prüfung (RV4); dedizierte Backup-DB `setJournalMode(TRUNCATE)`; sonst alle drei Dateien sichern.
- **Quelle:** https://commonsware.com/Room/pages/chap-backup-003.html

## QV) Quota & Rate-Limits (Vertiefung)

### QV1. Mai-2026-Doku widersprüchlich: „Per day" = 1 TB **oder** 400 Mio Units
- **Symptom:** Tageslimit nicht eindeutig bestimmbar.
- **Ursache:** Übergangs-Inkonsistenz der Unit-Umstellung.
- **Versionen:** seit 01.05.2026.
- **FIX:** In Units budgetieren (Write 50, List 100); konservativ ~3 Writes/s; Listen sparsam.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/limits

### QV2. `userRateLimitExceeded` trotz 1 Thread = oft 750-GB-Tageslimit maskiert ⭐ HAEUFIG
- **Symptom:** 403 selbst bei 1 Thread; Backup bricht reproduzierbar bei 60–94 %.
- **Ursache:** Undokumentiertes 750-GB/Tag-Upload-Limit ohne eigenen Reason-String; Backoff hilft nicht.
- **Versionen:** Backend, unverändert.
- **FIX:** Hochgeladenes Volumen mitzählen, bei ~700 GB/Tag pausieren (Tages-Reset abwarten), nicht endlos retryen.
- **Quelle:** https://forum.duplicacy.com/t/googleapi-error-403-user-rate-limit-exceeded.../2500

### QV3. Backoff-Cap zu niedrig + feste Retry-Obergrenze → Abbruch trotz korrektem Backoff
- **Symptom:** „Maximum number of retries reached (backoff 64, attempts 15)".
- **Ursache:** Max-Backoff 64 s zu kurz für Stunden-Fenster; harte 15-Versuche-Grenze.
- **Versionen:** Praxis (Duplicacy).
- **FIX:** Moderaten Cap (~30 min) ABER viele Retries; fehlgeschlagene Items zurückstellen + nächste Runde; Jitter behalten.
- **Quelle:** https://forum.duplicacy.com/t/googleapi-error-403-user-rate-limit-exceeded.../2500

### QV4. Token-Bucket: 100 TPS zu aggressiv → Durchschnitt ~10 TPS auslegen
- **Symptom:** 403/429 trotz scheinbar eingehaltener Quota.
- **Ursache:** Verwechslung Spitze vs. Durchschnitt; Burst leert Bucket sofort.
- **Versionen:** rclone-Praxis (Default 10 ms→100 ms).
- **FIX:** Pacer-Mindestabstand ~100 ms (~10 TPS); für Foto-Backup mit Semaphore eher ~3–4 Writes/s (je 50 Units).
- **Quelle:** https://github.com/rclone/rclone/issues/4136

### QV5. `storageQuotaExceeded` bei Service-Account / trotz freiem Speicher
- **Symptom:** Plötzlich 403 storageQuotaExceeded, UI-Upload geht weiter.
- **Ursache:** Service Accounts haben kein Storage-Quota / dürfen nicht in My Drive schreiben; irreführender Reason.
- **Versionen:** wiederkehrend 2024–2025.
- **FIX:** Nicht retryen (fatal); OAuth-User-Flow (euer Fall) oder Shared Drive. Lehre: storageQuotaExceeded immer sofort hart abbrechen.
- **Quelle:** https://discuss.google.dev/t/error-403-storagequotaexceeded-when-the-service-accounts-drive-is-completely-empty/194265

### QV6. Per-User-Verbrauch in der Cloud Console unsichtbar
- **Symptom:** Per-User-Limit getroffen, keine Console-Anzeige → Diagnose unmöglich.
- **Ursache:** Console-Quota ≠ Backend-Per-User-Limiter; Mai-2026-Modell verschärft.
- **Versionen:** langjährig.
- **FIX:** Eigenes Unit-Monitoring (mitzählen, bei >70 %/Min warnen); `quotaUser` pro Endnutzer.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/limits

### QV7. `Retry-After` liefert Drive unzuverlässig
- **Symptom:** Code, der auf den Header wartet, hängt/retryt sofort.
- **Ursache:** Drive sendet ihn bei 403/429 oft nicht (teils nur 503).
- **Versionen:** Stand 2026.
- **FIX:** `Retry-After` nutzen falls vorhanden, aber eigenen truncated-exponential-Backoff+Jitter als Fallback.
- **Quelle:** https://developers.google.com/workspace/drive/api/guides/handle-errors

### QV8. `subscriptionRateLimitExceeded` bei `changes.watch` trotz Backoff
- **Symptom:** 403 beim Anlegen von Watch-Channels, Backoff hilft nicht.
- **Ursache:** Separater, strenger Subscription-Limiter.
- **Versionen:** offen (Issue 398065924, 2025).
- **FIX:** Langlebige Channels statt häufigem Churn; bei diesem Reason Minuten statt Sekunden warten + Anlage-Frequenz drosseln.
- **Quelle:** https://issuetracker.google.com/issues/398065924

## WM) WorkManager / Foreground-Service

### WM1. `HiltWorkerFactory` greift nicht nach Prozess-Tod/Update → `NoSuchMethodException` ⭐ HAEUFIG
- **Symptom:** Worker-Crash beim Reanimieren (kein UI-Start).
- **Ursache:** `androidx.startup`-Default-Initializer läuft vor Hilt-Injection → `DefaultWorkerFactory` kennt den `@AssistedInject`-Konstruktor nicht.
- **Versionen:** offen (Dagger #3177).
- **FIX:** Default-WM-Initializer im Manifest entfernen (`tools:node="remove"`); `Application : Configuration.Provider` mit `override val workManagerConfiguration` (Property, nicht Methode!) + `HiltWorkerFactory`; `@HiltWorker`+`@AssistedInject`.
- **Quelle:** https://github.com/google/dagger/issues/3177

### WM2. `ClassNotFoundException` nach Worker-Rename/Move (Update mit pending Work)
- **Symptom:** Crash nach Update bei Nutzern mit Work in der Queue.
- **Ursache:** WorkManager persistiert den FQCN; umbenannt/verschoben → alter Name nicht instanziierbar.
- **Versionen:** unverändert.
- **FIX:** Worker nicht hart umbenennen/verschieben; alten Namen als Stub behalten, der die neue Klasse re-enqueued; `DelegatingWorkerFactory` mappt alte→neue.
- **Quelle:** https://jeroenmols.com/blog/2022/04/27/workmanager-crash/

### WM3. Expedited ohne `getForegroundInfo()` → IllegalStateException (API < 31)
- **Symptom:** Crash beim Enqueue/Start mit `setExpedited(...)`.
- **Ursache:** Android < 12 setzt Expedited via FGS, braucht `ForegroundInfo`.
- **Versionen:** stabil seit WM 2.7; minSdk 26 betroffen.
- **FIX:** `override suspend fun getForegroundInfo()`; Notification-Channel vorab.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### WM4. `MissingForegroundServiceTypeException` (Android 14) ⭐ HAEUFIG
- **Symptom:** `setForeground()` crasht beim `startForeground()`.
- **Ursache:** WM-eigener `SystemForegroundService` ohne deklarierten Typ.
- **Versionen:** targetSdk 34+.
- **FIX:** Manifest-Merge `foregroundServiceType="dataSync"` + Permissions; Typ in `ForegroundInfo(id, notif, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`.
- **Quelle:** https://developer.android.com/about/versions/14/changes/fgs-types-required

### WM5. `ForegroundServiceStartNotAllowedException` — `setForeground()` aus Hintergrund
- **Symptom:** Crash mitten im Upload nach App-Wechsel.
- **Ursache:** Ab Android 12 kein FGS-Start aus dem Hintergrund; `setForeground()` ist „opportunistisch".
- **Versionen:** Android 12+.
- **FIX:** In try/catch wrappen; FGS früh anfordern (App sichtbar); `RUN_AS_NON_EXPEDITED_WORK_REQUEST` oder UIDT.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running

### WM6. `ForegroundServiceDidNotStopInTimeException` nach 6h (Android 15) ⭐ HAEUFIG
- **Symptom:** Crash/Kill nach Stunden; `onTimeout` nicht rechtzeitig gestoppt.
- **Ursache:** `dataSync`-FGS 6h/24h; nach `onTimeout` nur Sekunden zum `stopSelf()`; WM-FGS betroffen.
- **Versionen:** Android 15 / targetSdk 35.
- **FIX:** `getStopReason()` (WM 2.9+) auswerten, bei Timeout `Result.retry()`; lange Backups auf UIDT; Test: `adb ... FGS_INTRODUCE_TIME_LIMITS`.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### WM7. Android 16: Long-Running-Worker verbrennt app-weite Job-Quota
- **Symptom:** Folgejobs starten nicht / Worker bricht vorzeitig ab.
- **Ursache:** FGS-Arbeit zählt ab Android 16 gegen app-weites Job-Runtime-Quota.
- **Versionen:** Android 16.
- **FIX:** Daten-Transfer als UIDT (von Quota ausgenommen); Worker segmentieren.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-all

### WM8. `Result.retry()`-Endlosschleife (Backoff begrenzt Anzahl nicht)
- **Symptom:** Dauerhaft fehlschlagender Upload retryt endlos (Akku/Netz).
- **Ursache:** Backoff regelt nur Abstand, keine Max-Retries.
- **Versionen:** alle.
- **FIX:** `if (runAttemptCount >= MAX) return Result.failure()`; nur transient → `retry()`; permanent → `failure()`.
- **Quelle:** https://github.com/googlecodelabs/android-workmanager/issues/63

### WM9. Über-constrainte Jobs starten nie
- **Symptom:** Backup bleibt auf ENQUEUED.
- **Ursache:** `UNMETERED`+`Charging`+`BatteryNotLow` zugleich selten alle erfüllt.
- **Versionen:** versionsunabhängig.
- **FIX:** Minimal nötige Constraints (`CONNECTED`); `UNMETERED` nur als erklärte Option.
- **Quelle:** https://medium.com/proandroiddev/android-workmanager-a-complete-technical-deep-dive-f037c768d87b

### WM10. `Data` > 10 KB → IllegalStateException
- **Symptom:** Crash vor Worker-Start bei großer Dateiliste in `Data`.
- **Ursache:** WM serialisiert `Data` in Room-DB, Limit 10 KB.
- **Versionen:** alle.
- **FIX:** WM als Orchestrator: nur Batch-/Session-ID übergeben, Liste in Room/Datei; `doWork()` lädt sie.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### WM11. UIDT-Fallen (User-Initiated Data Transfer)
- **Symptom:** UIDT startet nicht/abgelehnt.
- **Ursache:** Muss durch sichtbare User-Aktion im Vordergrund; `RUN_USER_INITIATED_JOBS` nötig; läuft über `JobScheduler.setUserInitiated(true)`, NICHT `setExpedited()`.
- **Versionen:** Android 14+.
- **FIX:** An User-Tap koppeln; Permission deklarieren; korrekten API-Pfad wählen; für Auto-Hintergrund-Backup UIDT nicht nutzen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/uidt

### WM12. Prozess-Tod → `doWork()` startet von vorne (kein State)
- **Symptom:** Bereits hochgeladene Dateien erneut hochgeladen / Fortschritt weg.
- **Ursache:** WM garantiert Ausführung, nicht Zustandserhalt; `setProgress()` überlebt Prozess-Tod nicht.
- **Versionen:** alle.
- **FIX:** Pro Datei Status (PENDING/UPLOADING/DONE) in Room; `doWork()` überspringt DONE; resumable Uploads (Resume-URI persistieren).
- **Quelle:** https://proandroiddev.com/why-has-my-background-worker-stopped-exploring-android-workmangers-stopreason-a0f743e6411c

### WM13. Doppeltes Backup trotz `enqueueUniqueWork` / Builder-Reuse
- **Symptom:** Zwei parallele Backups; oder NPE nach REPLACE-Re-enqueue; oder nur eine Ausführung.
- **Ursache:** Builder-Instanz mehrfach `.build()` → gleiche ID; `REPLACE` cancelt hart.
- **Versionen:** Builder-Reuse historisch gefixt; Policy-Falle bleibt.
- **FIX:** `enqueueUniqueWork(NAME, KEEP, request)`; pro Enqueue frische Builder-Instanz.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work

### WM14. OEM-Battery-Killer (Xiaomi/Samsung/Huawei) killen Worker & FGS
- **Symptom:** Backup läuft auf Pixel, stirbt auf MIUI/OneUI/EMUI ohne Resume.
- **Ursache:** Aggressive OEM-Power-Manager jenseits AOSP; `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` oft wirkungslos.
- **Versionen:** geräteabhängig.
- **FIX:** Defense-in-Depth: FGS + Battery-Exemption-Prompt + Autostart-Hinweis (dontkillmyapp) + persistenter Fortschritt (WM12) + Auto-Resume. Auch `POST_NOTIFICATIONS` (Android 13+) anfragen, sonst unsichtbarer FGS.
- **Quelle:** https://dev.to/stoyan_minchev/what-android-oems-do-to-background-apps-and-the-11-layers-i-built-to-survive-it-28bb

## INC) Reale Vorfall-Berichte (Open-Source-Apps)

### INC1. „Backup aktiviert" ≠ „Backup funktioniert" (Tasks.org/Aegis) ⭐ HAEUFIG
- **Symptom:** Drive-Backup lief jahrelang tot (z. B. „last enabled 2021"); Restore liefert uralten Stand → Datenverlust.
- **Ursache:** Nur Boolean-Flag statt echter Erfolgs-Historie; stiller Token-/Scope-Verlust nie gemeldet.
- **Versionen:** real (Tasks.org #2344).
- **FIX:** Zeitstempel des letzten **erfolgreichen** Backups zeigen; bei Veraltung (> N Tage) sichtbare Warnung; Token-Gültigkeit bei Start prüfen+Reauth.
- **Quelle:** https://github.com/tasks/tasks/issues/2344

### INC2. Android Auto Backup ≠ Drive-API-Backup (Aegis)
- **Symptom:** Nutzer glaubt, Cloud-Backup zu haben; bei App-Neuinstallation auf eingerichtetem Gerät keine Restore-Option.
- **Ursache:** Android Auto Backup wird nur beim Geräte-Ersteinrichten zurückgespielt, nie on-demand/bei App-Reinstall.
- **Versionen:** real (Aegis #1397).
- **FIX:** Für app-gesteuertes Backup aktiv per Drive-REST in appDataFolder schreiben/lesen (euer Setup); in UI klar trennen+beschriften.
- **Quelle:** https://github.com/beemdevelopment/Aegis/issues/1397

### INC3. Restore-Einträge nicht „dirty" → erster Sync löscht sie (Tasks×DAVx5)
- **Symptom:** Wiederhergestellte Einträge werden beim ersten Server-Sync gelöscht.
- **Ursache:** Restore markiert Einträge nicht als lokal-neu; Sync deutet „lokal vorhanden, server nicht" als Server-Delete.
- **Versionen:** real.
- **FIX:** Restaurierte Datensätze als dirty/lokal-neu markieren; nach Restore lokale DB (SoT) Vorrang sichern, bevor Sync läuft.
- **Quelle:** https://github.com/bitfireAT/davx5-ose/discussions/570

### INC4. Force-Sync + Restore in falscher Reihenfolge → IDs zerschossen (AnkiDroid)
- **Symptom:** Nach Force-Sync + Restore landen alle Karten im Default-Deck, Zuordnung weg.
- **Ursache:** Sync und Restore nicht koordiniert; ID-Remapping nur teilweise.
- **Versionen:** real (#4430).
- **FIX:** Restore und Sync exklusiv (Lock/State-Machine); ID-Remapping atomar (Transaktion); nach Restore Sync-State neu initialisieren.
- **Quelle:** https://github.com/ankidroid/Anki-Android/issues/4430

### INC5. Stuck „Backup in progress" → App startet nie mehr (AnkiDroid)
- **Symptom:** App blockiert dauerhaft beim Start.
- **Ursache:** Backup-Lock/Flag wird bei Abbruch (Kill/Doze/Crash) nie zurückgesetzt.
- **Versionen:** real (#19380, #19050).
- **FIX:** Lock mit Timeout + Recovery (verwaist erkennen, freigeben); Backup in try/finally; transaktional in Temp, am Ende atomar umbenennen.
- **Quelle:** https://github.com/ankidroid/Anki-Android/issues/19380

### INC6. „Collection corrupted"/Restore versagt bei DB-Versions-Mismatch (AnkiDroid)
- **Symptom:** Restore tut nichts / „corrupted"; sqlite3-Dump nur Teildaten.
- **Ursache:** Backup mit älterem Schema; WAL/SHM nicht eingecheckpointet (halbe DB).
- **Versionen:** real (#11811, #8112, #4796).
- **FIX:** Schema-Version ins Backup; beim Restore migrieren oder klar fehlschlagen; vor Backup `wal_checkpoint(TRUNCATE)` + close.
- **Quelle:** https://github.com/ankidroid/Anki-Android/issues/11811

### INC7. Drive erzwingt keine eindeutigen Namen → Duplikate/Duplikat-Ordner ⭐ HAEUFIG
- **Symptom:** Mehrere „backup.db" / doppelte Ordner; Restore mehrdeutig.
- **Ursache:** Google-bestätigt: keine Namens-Eindeutigkeit; `create` legt immer neu an.
- **Versionen:** per Design.
- **FIX:** `files.list` (name + parent + trashed=false) → vorhanden? `update` (gleiche ID) sonst `create`; fileId stabil persistieren.
- **Quelle:** https://github.com/googleworkspace/android-samples/issues/97

### INC8. Verschlüsseltes Backup mit Random-Key in EncryptedSharedPreferences → auf neuem Gerät unbrauchbar
- **Symptom:** Cloud-Backup lässt sich auf neuem Gerät nicht entschlüsseln.
- **Ursache:** Zufalls-Key liegt nur lokal (EncryptedSharedPreferences), wird nicht mitgesichert; bzw. nur Biometrie.
- **Versionen:** real (rafi0101-Lib-Warnung; Aegis).
- **FIX:** Vom Nutzer gesetzten/explizit gesicherten Key verwenden; UI-Bestätigung „Schlüssel gesichert"; klar kommunizieren, dass Biometrie nur das Gerät entsperrt.
- **Quelle:** https://github.com/rafi0101/Android-Room-Database-Backup

### INC9. App gekillt während Upload → halbe Datei überschreibt gutes Backup
- **Symptom:** Unvollständige Datei in Drive, vorheriges gutes Backup beschädigt.
- **Ursache:** In-place `update` über das einzige gute Backup; Abbruch mittendrin.
- **Versionen:** real.
- **FIX:** Erst neue Datei hochladen+verifizieren (Größe/MD5) → dann atomar umschalten → altes erst danach löschen; resumable + Integritätsprüfung.
- **Quelle:** https://github.com/googlecodelabs/android-workmanager/issues/154

### INC10. Stiller Backup-Stopp durch widerrufenes Token/entzogenen Scope
- **Symptom:** „Erfolgreiche" Backups, real seit Wochen keins; Restore ohne aktuellen Stand.
- **Ursache:** OAuth-Token automatisch widerrufen (Passwortwechsel/Inaktivität/Policy); App fängt `UserRecoverableAuthException`/401/403 nur ab und loggt weg.
- **Versionen:** real.
- **FIX:** `UserRecoverableAuth*`/401/403 explizit behandeln: Reauth-Intent / sichtbare persistente Notification; echten Erfolgs-Zeitstempel anzeigen.
- **Quelle:** https://developers.google.com/workspace/drive/api/troubleshoot-authentication-authorization

### INC11. Foto-Referenzen nach Restore kaputt (DB zeigt auf fehlende/anders-pfadige Bilder)
- **Symptom:** Leere Thumbnails / verwaiste Fotos nach Restore.
- **Ursache:** DB + Fotos nicht transaktional gekoppelt; absolute Pfade existieren auf neuem Gerät nicht.
- **Versionen:** real (Muster).
- **FIX:** Fotos per stabiler ID/relativem Schlüssel referenzieren (Restore-Remapping); erst Fotos, dann DB; nach Restore Referenzen reconcilen.
- **Quelle:** https://support.google.com/photos/answer/6306652

### INC12. Atomarität ist die rote Linie (Querschnitt)
- **Symptom:** Korruption (WAL), Duplikate, halbe Uploads, Foto-Bruch — alle mit derselben Wurzel.
- **Ursache:** Schreiben ohne „alles-oder-nichts".
- **Versionen:** Querschnitt.
- **FIX:** Überall: in Temp schreiben → verifizieren → atomar umschalten → Altes erst danach entfernen.
- **Quelle:** Synthese der Vorfälle INC1–INC11.

---

## ✅ Fix-Status (was ist in neueren Versionen behoben?)

> Ehrlichkeits-Hinweis: „GEFIXT" nur mit Beleg (Changelog/Release/Issue mit Fix-Commit). Alles
> andere ist „offen / per Design / Status unklar" — Workaround bleibt aktiv. GitHub-CLI war in
> dieser Umgebung nicht verfügbar; Status aus Changelog/Issue-Text, nicht aus Live-`gh`-Abfrage.

| Bug | Status | Beleg / Maßnahme |
|-----|--------|------------------|
| L5 (chunked+gzip Keep-Alive-Leak) | **GEFIXT** | google-http-client **1.35.0** (PR #990) — `>= 1.35.0` pinnen |
| CM3 (TransactionTooLarge, mehrere Accounts) | **GEFIXT** | Google Play services **24.40.XX** — GMS anheben |
| L4 (4-KB-Buffer-Drossel) | **WON'T FIX** | Eigener 16-KB-`writeTo()` |
| L7 (Interrupt-Flag-Verlust) | offen (≤1.34.2) | Eigener Handler-Wrapper |
| L8 (Download nicht abbrechbar) | offen (adressiert via #1101) | Eigener abbruchbarer Wrapper |
| L12 (Apache-Pool stale) | offen | `NetHttpTransport` auf Android |
| WM1 (HiltWorkerFactory nach Prozess-Tod) | offen (Dagger #3177) | Initializer entfernen + `Configuration.Provider` |
| WM6 (dataSync 6h-Timeout) | per Design (Android 15) | UIDT / `getStopReason()`+retry |
| WM7 (Android-16-Job-Quota) | per Design (Android 16) | UIDT |
| AV1 (leere-Seite-Pagination) | offen (Issue 406305173) | Nur bei fehlendem `nextPageToken` abbrechen |
| QV8 (subscriptionRateLimitExceeded) | offen (Issue 398065924) | Langlebige Channels, länger warten |
| RV2 (room_table_modification_log) | offen (Issue 67757002) | DB nach Restore neu bauen |
| CM6 (`getId()` ≠ E-Mail) | teils gefixt (Issue 368873078), weiter Reports | ID-Token-`email`-Claim lesen |
| AU1/§5 (GoogleAuthUtil-Entfernung) | angekündigt **ab Mai 2026** | Migration zu AuthorizationClient |
| Q6/QV1 (Quota-Unit-Modell) | **aktiv seit 01.05.2026** | In Units budgetieren; Projekt-Alter prüfen |
| Alle übrigen appData/Quota/Drive-Quirks | **per Design / Status unklar** | Workaround bleibt aktiv |

---

## 📋 Pflicht-Checkliste (vor Release des Backup-/Restore-Features)

**Auth**
- [ ] Migration `GoogleAuthUtil` → `AuthorizationClient` umgesetzt (Frist Mai 2026); SDK ≥ 21.6.0
- [ ] Nach `authorize()` `getGrantedScopes()` geprüft (CM10); `getAuthorizationResultFromIntent` in try/catch (CM11)
- [ ] `getId()` NICHT als E-Mail (CM6); Web-Client-ID + beide SHA-1 hinterlegt (CM8)
- [ ] Consent-Screen „In Production" + Verifizierung; `drive.appdata`-Einstufung an der Primärquelle bestätigt (CM16)

**HTTP-Client**
- [ ] `google-http-client >= 1.35.0` gepinnt (L5); `NetHttpTransport` statt Apache (L12)
- [ ] Initializer-Chaining „delegate first" (L13); read-Timeout ≥ 3 min (U6)
- [ ] Cancel-sicherer Retry-Wrapper (L7); Retry auf EINE Ebene + Max-Elapsed (L9)
- [ ] Pro Coroutine eigenes Request-Objekt (U7); 16-KB-Buffer falls Durchsatz kritisch (L4)

**Sync / Orphan / Multi-Device**
- [ ] Reconciliation-Sweep mit Max-Delete-Schutz + Dry-Run-Log (O1, O3)
- [ ] Change-Detection über `md5/sha256` statt Name+Size (O2); kanonische File-ID + `update` (M4, INC7)
- [ ] Pre-Generated File-ID-Idempotenz (GI1); appData-`spaces` bei Changes-API (CH1, CH2)
- [ ] Paginierung bricht nur bei fehlendem `nextPageToken` ab (AV1)

**Quota / Reliability**
- [ ] Semaphore 3–4 + ~3 Writes/s (QV4); Backoff+Jitter, moderater Cap + viele Retries (QV3)
- [ ] `storageQuotaExceeded` nicht retryen (Q3); 750-GB/Tag mitzählen (QV2); eigenes Unit-Monitoring (QV6)

**Upload-Job**
- [ ] Backup in `CoroutineWorker` (U9); `HiltWorkerFactory` korrekt verdrahtet (WM1)
- [ ] FGS-Typ `dataSync` im Manifest gemergt (WM4); `setForeground()` in try/catch (WM5); `onTimeout`/StopReason (WM6)
- [ ] UIDT für lange Backups (WM7, WM11); `enqueueUniqueWork(KEEP)` (WM13); persistenter Resume-State in Room (WM12)
- [ ] `POST_NOTIFICATIONS` angefragt (WM14); minimale Constraints (WM9); Daten als ID, nicht > 10 KB (WM10)

**Restore / Integrität**
- [ ] Checksum-Prüfung vor Swap (R3); `-wal`/`-shm` via `deleteDatabase()` entfernt (RV3)
- [ ] `wal_checkpoint(TRUNCATE)`-Rückgabe geprüft (RV4); Room `close()` + neu bauen / Prozess-Neustart (RV1, R5)
- [ ] Schema-/`identityHash`-Version vor Swap geprüft (R6, RV5); kein `fallbackToDestructiveMigration` (RV6)
- [ ] Backup über `content://`/SAF, nicht `file://` (RV8); Foto-Referenzen reconcilen (INC11)

**Hygiene**
- [ ] Letztes **erfolgreiches** Backup mit Zeitstempel angezeigt + Veraltungs-Warnung (INC1, INC10)
- [ ] Atomar: upload → verify → switch → delete-old (INC9, INC12); Backup-Lock mit Timeout/Recovery (INC5)
- [ ] Signatur-Key/Client-ID stabil (AV7); Encrypt-Key vom Nutzer gesichert (INC8)

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
| L1–L13 (HTTP-Client) | §7 (Resumable/Reliability) |
| CH1–CH6 (Changes-API) | §4 (Multi-Device) |
| GI1–GI5 (generateIds/Revisions) | §4 (Konfliktauflösung), §7 (Idempotenz) |
| AV1–AV8 (appData/Query-Vertiefung) | §1 (Grundlagen/Layout) |
| CM1–CM16 (Credential Manager/Auth) | §5 (Scopes/Auth-Migration) |
| RV1–RV11 (Room/Restore-Vertiefung) | §9 (Restore & Integrität) |
| QV1–QV8 (Quota-Vertiefung) | §6 (Quota & Rate-Limits) |
| WM1–WM14 (WorkManager/FGS) | §8 (WorkManager) |
| INC1–INC12 (Reale Vorfälle) | §2/§9/§11 (Orphan/Restore/Architektur) |
