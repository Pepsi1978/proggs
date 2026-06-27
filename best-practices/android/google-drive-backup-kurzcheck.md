# Google-Drive-Backup & Cloud-Sync Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
