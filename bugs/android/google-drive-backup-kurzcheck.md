# Google-Drive-Backup & Cloud-Sync (Android, appDataFolder) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
