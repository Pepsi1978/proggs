# Android-Framework / Platform-SDK Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | targetSdk 35/36 — Edge-to-Edge, Insets ueberlappen | WindowInsets behandeln; nie Funktion entfernen | §8.1 |
| 2 | targetSdk 36 — eigene `onBackPressed()`-Logik tot | Auf `OnBackPressedCallback`/`BackHandler` migrieren | §8.2 |
| 3 | Foreground Service startet/crasht | Manifest-Typ + `FOREGROUND_SERVICE_*`-Permission + Typ-Arg, alle drei | §3.1, §3.2 |
| 4 | `startForegroundService()` ohne sofortiges `startForeground()` | Binnen ~5 s `startForeground()` ganz am Anfang | §3.5 |
| 5 | `@Database`-`version` erhoeht | Echte `Migration`/`@AutoMigration`, nie destruktiv | §5.1, §5.2 |
| 6 | DB-Datei-Backup / Drive-Upload | Vorher WAL-Checkpoint (`close()` / `wal_checkpoint(TRUNCATE)`) | §5.7 |
| 7 | `PendingIntent` ab Android 12 | Immer `FLAG_IMMUTABLE` (oder bewusst `FLAG_MUTABLE`) | §6.1 |
| 8 | Notification erscheint nicht | Channel (ab 8) + `POST_NOTIFICATIONS` runtime (ab 13) | §6.2, §2.1 |
| 9 | Exakter Alarm feuert nicht (Android 14) | `canScheduleExactAlarms()` pruefen, sonst Request | §6.4 |
| 10 | `registerReceiver()` crasht (Android 14) | Export-Flag via `ContextCompat.registerReceiver` | §6.7 |
| 11 | Background-Worker stoppt/laeuft nie | Kein Timing-Verlass; `getStopReason()` loggen; nach Boot re-enqueue | §4.3, §4.5, §4.10 |
| 12 | Flow-Collection / `registerX` ohne Cleanup | `repeatOnLifecycle(STARTED)`; jedes `registerX` braucht `unregisterX` | §1.4, §1.8 |
| 13 | State nach Hintergrund-Kill weg | ViewModel ueberlebt nicht Process Death → `SavedStateHandle`/Persistenz | §1.5, §1.6 |
| 14 | Native `.so` (NDK/SDK), targetSdk 35+ | 16-KB-Page-Size: NDK r28+ / `max-page-size=16384` | §8.3 |
| 15 | Custom-Permission einer ANDEREN App `granted=false` (ContentProvider-`SecurityException`) | Definierende App ZUERST, nutzende App DANACH neu installieren (`pm grant` hilft nicht) | §2.11 |
| 16 | `EncryptedSharedPreferences` — Crash in `onCreate` nach Geraetewechsel (`AEADBadTagException`, Keystore -30) | `create()` in try/catch + Prefs & Masterkey-Alias loeschen und neu anlegen; Prefs-Datei aus BEIDEN Backup-XMLs ausschliessen | §7.4 |
