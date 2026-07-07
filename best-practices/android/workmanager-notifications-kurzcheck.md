# WorkManager & Notifications Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Reminder zur Uhrzeit vs. Backup | AlarmManager für exakte Zeit, **WorkManager für Backup** — nie vermischen | §1 |
| 2 | Wochentag-Zeiten (Mo–Fr 20:00, Sa/So 10:00) | **One-shot + Reschedule** (kein `setRepeating`), nächsten Slot via `java.time` | §2 |
| 3 | Exact-Alarm-Permission? | Für ein Journal **inexakt** (`setAndAllowWhileIdle`) — spart Permission + Play-Risiko | §3 |
| 4 | `USE_EXACT_ALARM` | NICHT deklarieren (Play-Policy nur Wecker/Kalender → Reject-Risiko) | §3 |
| 5 | PendingIntent | `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`; eindeutiger Request-Code pro Reminder | §4 |
| 6 | Notification-Channel | Importance VOR erster Anlage final (danach Nutzer-Hoheit); idempotent in `Application.onCreate` | §5 |
| 7 | Notification kommt nicht | `POST_NOTIFICATIONS` runtime anfragen; `areNotificationsEnabled()`+Channel-Importance diagnostizieren | §5 |
| 8 | Nach Reboot/Update weg | Receiver für `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` → `rescheduleAll()` aus Room | §6 |
| 9 | Force-Stop | Ab Android 15 werden PendingIntents gecancelt; Reschedule auch bei JEDEM App-Start | §6 |
| 10 | Auto-Backup | `PeriodicWorkRequest` + `enqueueUniquePeriodicWork(KEEP)`; Constraints UNMETERED + BatteryNotLow | §7 |
| 11 | Constraints | Minimal halten; nie `Charging` + `DeviceIdle` kombinieren (startet nie) | §7 |
| 12 | „Jetzt sichern" lang | UIDT-Job (quota-/6h-exempt) statt `dataSync`-FGS; kurz → expedited Worker | §8 |
| 13 | OEM killt Reminder | Battery-Exemption anfragen + dontkillmyapp-Anleitung; WorkManager-Watchdog | §9 |
| 14 | DST/Zeitzone | Trigger immer frisch via `ZonedDateTime` berechnen, nie altes Millis halten | §2 |
