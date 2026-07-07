# WorkManager & Notifications (Reminder + Hintergrund-Backups) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **A** AlarmManager/Reminder · **N** Notifications · **B** Boot/Reschedule · **W** WorkManager ·
> **F** Foreground-Service/Backup · **O** OEM-Killings.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Reminder driftet / falsche Uhrzeit | `setRepeating` ist seit API 19 inexakt → One-shot + Reschedule | A1 |
| 2 | `SecurityException` bei `setExact*` | `canScheduleExactAlarms()` prüfen; für Journal inexakt bleiben | A2 |
| 3 | Play-Reject `USE_EXACT_ALARM` | Nicht deklarieren (nur Wecker/Kalender) → `setAndAllowWhileIdle` | A3 |
| 4 | Dichte Alarme feuern nicht | Doze: `…AllowWhileIdle` max ~1×/9 Min | A4 |
| 5 | Reminder verschwinden nach Permission-Entzug | Auf `…PERMISSION_STATE_CHANGED` lauschen; besser inexakt | A5 |
| 6 | Reminder nach Zeitumstellung falsch | Trigger frisch via `ZonedDateTime` berechnen, nie altes Millis | A6 |
| 7 | Alarm feuert sofort | Nur Slots echt > now akzeptieren | A7 |
| 8 | Nur 1 von mehreren Remindern feuert | Eindeutiger Request-Code pro Reminder (Extras zählen nicht) | A8 |
| 9 | PendingIntent-Crash ab Android 12 | `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` | N4 |
| 10 | Channel-Settings per Code ignoriert | Importance nach Anlage unveränderlich → neue Channel-ID | N1 |
| 11 | Notification kommt nicht (Android 13+) | `POST_NOTIFICATIONS` runtime; `areNotificationsEnabled()` prüfen | N3, N5 |
| 12 | Heads-up fehlt trotz HIGH | Sound/Vibration am Channel, nicht am Builder | N6 |
| 13 | Tap öffnet keine Activity | Kein Trampolin (ab Android 12); direkt `getActivity` | N7 |
| 14 | Reminder weg nach Reboot/Update | Receiver `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` → `rescheduleAll()` | B1, B3 |
| 15 | Nach Force-Stop tot | Ab Android 15 PendingIntents gecancelt; Reschedule bei App-Start | B2 |
| 16 | Worker startet nie | Constraints minimal; nie Charging+DeviceIdle | W2 |
| 17 | Doppelte Backup-Worker | `enqueueUniquePeriodicWork(KEEP)` | W3 |
| 18 | Worker nach 10 Min/Quota gestoppt | Resumable + `getStopReason()`; Android 16 Job-Quota | W5, W6 |
| 19 | `dataSync`-FGS-ANR nach 6h | `onTimeout`→`stopSelf`; besser UIDT/WorkManager | F1 |
| 20 | `HiltWorker`-Crash | Default-WM-Initializer entfernen + `Configuration.Provider` | W7 |
| 21 | OEM killt Reminder | Battery-Exemption + dontkillmyapp-Anleitung; Watchdog | O1 |
| 22 | `cancel()` räumt nicht auf → "max 500 alarms" | Identischer PendingIntent + Self-Reschedule (1 Alarm) | A10 |
| 23 | Reminder erst nach Entsperren nach Reboot | `LOCKED_BOOT_COMPLETED` + `directBootAware` | B9 |
| 24 | `Cannot initialize WorkManager in direct boot` | WM nicht directBootAware; `isUserUnlocked` prüfen | B11 |
| 25 | `ForegroundServiceStartNotAllowedException` | FGS synchron im Vordergrund-Stack starten | F7 |
| 26 | Periodic-Worker läuft nie wieder | WorkManager ≥ 2.11.2; `doWork` in try/catch | W10 |
| 27 | Mehrere Notifications kollabieren zu einer | Eindeutige ID/Tag pro Reminder | N10 |
| 28 | FCM-Push verspätet/fehlt | High-Prio + sichtbare Notification, nicht data-only | O16 |
