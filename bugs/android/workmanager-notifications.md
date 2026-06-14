# Bekannte Bugs: WorkManager & Notifications (Reminder + Hintergrund-Backups)

> PFLICHT-LESEN vor Arbeit an Reminder/Benachrichtigungen oder Hintergrund-Backups in BestJournal.
> Stand: tief recherchiert am 2026-06-14 (7 Researcher parallel, offizielle Android-Quellen).
> Versions-Anker: targetSdk **35** (Android 15), minSdk **26** · WorkManager 2.11.x (Projekt aktuell
> AlarmManager-basiert, keine WorkManager-Dep) · `java.time` ab API 26 nativ · `POST_NOTIFICATIONS` +
> `RECEIVE_BOOT_COMPLETED` deklariert, kein Exact-Alarm-Permission.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/android/best-practices-workmanager-notifications.md`](../../best-practices/projekt-code/android/best-practices-workmanager-notifications.md).

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

---

## A) AlarmManager / Reminder

### A1. `setRepeating` driftet / kann keine Wochentag-Zeiten ⭐ HAEUFIG (BestJournal aktuell)
- **Symptom:** Täglicher Reminder kommt mal pünktlich, mal Minuten zu spät; über Tage wandert die Zeit. Unterschiedliche Zeiten pro Wochentag nicht abbildbar.
- **Ursache:** Seit API 19 sind alle `setRepeating`-Alarme **inexakt** (gebatcht, Anker driftet); ein festes Intervall (`INTERVAL_DAY`) kann nur einen Zeitpunkt.
- **Versionen:** API 19+ (alle).
- **FIX:** `setRepeating` ersetzen durch **One-shot + Reschedule**: nächsten Trigger via `java.time` (8-Tage-Schleife, pro `DayOfWeek` eigene `LocalTime`) berechnen, einen `setAndAllowWhileIdle`-Alarm setzen, im Receiver Notification posten + nächsten Slot neu planen.
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### A2. `SecurityException` bei `setExact*` ohne Permission
- **Symptom:** Crash beim Setzen eines exakten Alarms auf Android 12+.
- **Ursache:** targetSdk ≥ 31 ohne `USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM`; ab Android 14 `SCHEDULE_EXACT_ALARM` denied-by-default (Manifest reicht nicht).
- **Versionen:** Android 12+ (verschärft 14).
- **FIX:** Für ein Journal exakte Alarme vermeiden → `setAndAllowWhileIdle`. Falls nötig: `canScheduleExactAlarms()` vor jedem `setExact*`, bei false `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` + graceful Fallback.
- **Quelle:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

### A3. Play-Reject wegen `USE_EXACT_ALARM` ⭐ HAEUFIG
- **Symptom:** App-Release abgelehnt, weil `USE_EXACT_ALARM` im Manifest.
- **Ursache:** Play-Policy beschränkt die Permission strikt auf Wecker-/Kalender-Apps; eine Journal-/Reminder-App qualifiziert sich nicht zuverlässig.
- **Versionen:** targetSdk 33+, Play-Policy.
- **FIX:** `USE_EXACT_ALARM` nicht deklarieren; inexakt (`setAndAllowWhileIdle`) bleiben oder `SCHEDULE_EXACT_ALARM` mit Nutzer-Flow.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/12253906

### A4. Doze drosselt `…AllowWhileIdle` auf ~1×/9 Min
- **Symptom:** Mehrere kurz aufeinanderfolgende Reminder feuern nicht; nur ~1 alle 9–15 Min.
- **Ursache:** `setAndAllowWhileIdle`/`setExactAndAllowWhileIdle` dürfen pro App max. 1×/9 Min (Low-Power ~15 Min) feuern.
- **Versionen:** Android 6+ (Doze).
- **FIX:** Reminder-Abstände ≥ ~15 Min; für sekundengenaue dichte Alarme nur `setAlarmClock()` (Wecker-Icon).
- **Quelle:** https://developer.android.com/training/monitoring-device-state/doze-standby

### A5. Permission-Widerruf killt App + alle exakten Alarme
- **Symptom:** Nach Entzug „Wecker & Erinnerungen" verschwinden alle Reminder.
- **Ursache:** Widerruf von `SCHEDULE_EXACT_ALARM` stoppt die App und cancelt alle zukünftigen exakten Alarme.
- **Versionen:** Android 12+.
- **FIX:** Auf `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` reagieren + neu planen; in `onResume` re-prüfen. Inexakt umgeht das ganz (nicht widerrufbar).
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### A6. DST/Zeitzonenwechsel berechnet falschen Trigger
- **Symptom:** Reminder feuert nach Sommer-/Winterzeit 1 h zu früh/spät, doppelt oder gar nicht.
- **Ursache:** Wall-Clock-RTC-Alarme; einmal berechnetes Millis stimmt nach Zeitzonen-/DST-/Uhr-Änderung nicht mehr.
- **Versionen:** alle.
- **FIX:** Nie ein berechnetes Millis langfristig halten; bei jeder Iteration/im Receiver frisch via `ZonedDateTime` (lokale Zeit → `.toInstant()`) neu berechnen; `ZoneId` neu lesen. One-shot-Pattern (A1) erledigt das.
- **Quelle:** https://www.baeldung.com/java-daylight-savings · https://developer.android.com/reference/java/time/ZonedDateTime

### A7. „Vergangener" Trigger feuert sofort
- **Symptom:** Beim (Neu-)Planen feuert der Alarm unmittelbar.
- **Ursache:** Trigger-Zeit in der Vergangenheit → AlarmManager feuert sofort; passiert bei „heute, gleiche Uhrzeit", obwohl vorbei.
- **Versionen:** alle.
- **FIX:** In der Slot-Berechnung nur Zeitpunkte **echt > now** (strikt) akzeptieren.
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### A8. Gleicher Request-Code → Alarme überschreiben sich
- **Symptom:** Mehrere Reminder definiert, nur einer feuert; neuer „löscht" alten.
- **Ursache:** PendingIntents mit gleichem Request-Code + per `filterEquals` gleichem Intent gelten als identisch; Extras zählen beim Vergleich nicht.
- **Versionen:** alle.
- **FIX:** Stabiler eindeutiger Request-Code pro Reminder (Room-ID); Unterscheidung nicht über Extras, sondern Request-Code/Intent-`data`.
- **Quelle:** https://developer.android.com/reference/android/app/PendingIntent

### A9. `Handler.postDelayed` als Alarm-Ersatz feuert im Deep-Sleep nie
- **Symptom:** Timer funktioniert bei aktivem Display, schläft das Gerät → kein Trigger.
- **Ursache:** Handler laufen nicht im CPU-Deep-Sleep; nur AlarmManager weckt die CPU.
- **Versionen:** alle.
- **FIX:** Für zeitkritische Trigger immer AlarmManager; Handler nur für aktive-Display-Pulse.
- **Quelle:** dev.to/stoyan_minchev (extern)

---

## N) Notifications

### N1. Channel-Settings per Code nach Anlage NICHT änderbar ⭐ HAEUFIG
- **Symptom:** Importance/Sound/Vibration im Code geändert, System ignoriert es.
- **Ursache:** Nach `createNotificationChannel()` hat der Nutzer die Hoheit; nur Name/Beschreibung änderbar.
- **Versionen:** API 26+.
- **FIX:** Importance/Sound/Vibration VOR erster Registrierung final; für neues Verhalten **neue Channel-ID** (`reminders_v2`).
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### N2. Channel mit gleicher ID neu anlegen behält alte Settings
- **Symptom:** Channel gelöscht + mit gleicher ID/neuer Importance neu erstellt → alte Importance bleibt.
- **Ursache:** Android cached gelöschte Channels; Recreate stellt alte Konfig wieder her.
- **Versionen:** API 26+.
- **FIX:** Neue Channel-ID vergeben; nicht bei jedem Update hochzählen (Settings zeigt Zahl gelöschter Channels).
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### N3. Notification ohne Channel = unsichtbar + Log-Error
- **Symptom:** Notification erscheint nie, Logcat-Fehler.
- **Ursache:** targetSdk ≥ 26 + Post ohne gültige `channelId`.
- **Versionen:** API 26+.
- **FIX:** ChannelId setzen + Channel vorher registrieren; Dev-Option „Show notification channel warnings".
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### N4. PendingIntent ohne Mutability-Flag crasht (Android 12)
- **Symptom:** `IllegalArgumentException` „requires FLAG_IMMUTABLE or FLAG_MUTABLE".
- **Ursache:** Ab targetSdk 31 muss jeder PendingIntent explizit immutable/mutable sein.
- **Versionen:** Android 12+.
- **FIX:** `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`; `FLAG_MUTABLE` nur für RemoteInput/Bubbles.
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

### N5. POST_NOTIFICATIONS: 2× „Don't allow" → Dialog nie wieder ⭐ HAEUFIG
- **Symptom:** `requestPermissions` tut nichts, sofort denied; Reminder feuert, aber nichts sichtbar.
- **Ursache:** Nach 2× Ablehnung dauerhaft verweigert (USER_FIXED); ab Android 13 Notifications default aus.
- **Versionen:** Android 13+.
- **FIX:** Im Kontext anfragen (mit Rationale); dauerhafte Ablehnung erkennen (`shouldShowRequestPermissionRationale==false` + denied) → `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/notification-permission

### N6. Heads-up fehlt trotz `IMPORTANCE_HIGH`
- **Symptom:** Notification nur lautlos im Drawer, kein Pop-up.
- **Ursache:** Heads-up braucht HIGH **und** Sound/Vibration; ab API 26 nur über den **Channel** (Builder-`setSound`/`setVibrate` wirkungslos); DND/Cooldown unterdrücken zusätzlich.
- **Versionen:** API 26+.
- **FIX:** Channel mit HIGH + Sound/Vibration am Channel (vor erster Anlage); Cooldown (Android 15) bedenken.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### N7. Notification-Trampoline verboten (Android 12)
- **Symptom:** Tap öffnet keine Activity; Logcat „trampoline blocked".
- **Ursache:** Ab targetSdk 31 kein `startActivity()` aus Service/Receiver nach Notification-Tap.
- **Versionen:** Android 12+.
- **FIX:** `PendingIntent.getActivity(...)` direkt auf Ziel-Activity, kein Umweg über Receiver/Service.
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

### N8. Full-Screen-Intent nur noch Wecker/Anruf-Apps (Android 14)
- **Symptom:** `setFullScreenIntent` degradiert zur normalen Heads-up.
- **Ursache:** Ab targetSdk 34 `USE_FULL_SCREEN_INTENT` nur automatisch für Anruf/Wecker; sonst Nutzer-Permission/Console-Deklaration.
- **Versionen:** Android 14+.
- **FIX:** Für Journal-Reminder nicht nutzen (HIGH-Heads-up reicht); falls nötig `canUseFullScreenIntent()` + `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.
- **Quelle:** https://developer.android.com/about/versions/14/behavior-changes-14

### N9. Notification kommt nicht — Diagnose
- **Symptom:** Gepostet, aber nichts erscheint.
- **Ursache:** Permission fehlt / App-weit aus / Channel `IMPORTANCE_NONE` / kein Channel / `smallIcon` fehlt / OEM-Power.
- **Versionen:** alle.
- **FIX:** `areNotificationsEnabled()` (App) + `getNotificationChannel(id).importance` (Channel) zusammen prüfen; `smallIcon` + `contentTitle` + `channelId` sicherstellen.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/notification-permission

---

## B) Boot / Reschedule

### B1. Alarme weg nach Reboot ⭐ HAEUFIG
- **Symptom:** Nach Neustart feuern keine Reminder.
- **Ursache:** AlarmManager persistiert Alarme nicht über Reboot.
- **Versionen:** alle.
- **FIX:** Receiver `RECEIVE_BOOT_COMPLETED` + `BOOT_COMPLETED` → zentrale `rescheduleAll()` aus Room.
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### B2. Nach Force-Stop feuert nichts mehr (auch kein BOOT_COMPLETED) ⭐ HAEUFIG
- **Symptom:** Nach „Stopp erzwingen" alle Reminder/Worker tot, selbst nach Reboot.
- **Ursache:** Stopped state → keine Manifest-Broadcasts bis manuelles App-Öffnen; **Android 15 cancelt zusätzlich alle PendingIntents** beim Eintritt.
- **Versionen:** alle; PendingIntent-Cancel ab Android 15.
- **FIX:** Nicht abfangbar — `rescheduleAll()` bei jedem App-Start; im Onboarding erklären, dass Force-Stop Reminder deaktiviert.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-all

### B3. Alarme weg nach App-Update
- **Symptom:** Nach Play-Update/Sideload feuern Reminder nicht mehr (kein Reboot).
- **Ursache:** Package-Replace cancelt Alarme; reiner BOOT_COMPLETED-Receiver greift nicht.
- **Versionen:** alle.
- **FIX:** `android.intent.action.MY_PACKAGE_REPLACED` im selben Filter → `rescheduleAll()`.
- **Quelle:** https://github.com/MaikuB/flutter_local_notifications/issues/689

### B4. BOOT_COMPLETED feuert nicht vor erstem App-Start
- **Symptom:** Frisch installierte App bekommt nach Reboot kein BOOT_COMPLETED.
- **Ursache:** Apps stehen nach Install im stopped state; Manifest-Receiver feuern erst nach ≥1× manuellem Öffnen.
- **Versionen:** Android 3.1+.
- **FIX:** Schedule schon beim ersten App-Start; Boot-Receiver ist nur Wiederherstellungsnetz.
- **Quelle:** Android stopped-state-Verhalten (extern/offiziell)

### B5. `android:exported` fehlt am Receiver → Install-Block
- **Symptom:** App auf Android 12+ nicht installierbar.
- **Ursache:** Ab targetSdk 31 muss jede Komponente mit Intent-Filter `android:exported` explizit setzen.
- **Versionen:** Android 12+.
- **FIX:** `android:exported="true"` am Boot-Receiver (System-Broadcast von außen).
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

### B6. Reminder vor Entsperren verpasst (Direct Boot)
- **Symptom:** Nach Reboot ohne Entsperren feuern Reminder bis zum ersten Unlock nicht.
- **Ursache:** Direct Boot: nur `LOCKED_BOOT_COMPLETED` feuert vor Unlock; normales `BOOT_COMPLETED` erst danach.
- **Versionen:** Android 7+.
- **FIX:** Receiver `directBootAware="true"` + `LOCKED_BOOT_COMPLETED`; Reminder-State in device-protected Storage. Optional, wenn Reminder vor Unlock nötig.
- **Quelle:** https://developer.android.com/privacy-and-security/direct-boot

### B7. Verbotener FGS-Typ aus BOOT_COMPLETED (Android 15)
- **Symptom:** Crash beim Start eines `dataSync`/`mediaPlayback`/`camera`-FGS aus dem Boot-Receiver.
- **Ursache:** Android 15 verbietet diese FGS-Typen aus `BOOT_COMPLETED`.
- **Versionen:** Android 15 / targetSdk 35.
- **FIX:** Im Boot-Receiver keinen FGS starten — `goAsync()` oder WorkManager (überlebt Reboot ohnehin).
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### B8. Schwere Arbeit im Receiver → ANR
- **Symptom:** Boot-Receiver hängt/ANR bei vielen Remindern.
- **Ursache:** Receiver hat ~10 s (WM rechnet ~8 s); beim Boot viele Receiver gleichzeitig.
- **Versionen:** alle.
- **FIX:** `goAsync()` + Background-Thread + `finish()` für I/O; viele Reminder per OneTimeWorkRequest; kein Networking im Receiver.
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

---

## W) WorkManager (Backups)

### W1. Periodische Arbeit ist nicht zeitgenau ⭐ HAEUFIG
- **Symptom:** Worker läuft Stunden später als erwartet.
- **Ursache:** Min-Intervall 15 Min; Doze-Maintenance-Windows + App-Standby-Buckets verschieben.
- **Versionen:** alle.
- **FIX:** „Periodisch" = „ungefähr" akzeptieren (Backups vertragen das); für exakte Zeit AlarmManager. Beim App-Start „überfälliges Backup?" prüfen + einmalig anstoßen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### W2. Job startet nie — Constraint-Kombination ⭐ HAEUFIG
- **Symptom:** Worker bleibt auf vielen Geräten ENQUEUED.
- **Ursache:** `setRequiresCharging(true)` + `setRequiresDeviceIdle(true)` selten gleichzeitig erfüllt; jeder weitere Constraint verkleinert das Fenster; unerfüllte Constraints können den Lauf **auslassen**.
- **Versionen:** alle.
- **FIX:** Minimal: `UNMETERED` + `BatteryNotLow`. Nie Charging+Idle koppeln; großzügiges Intervall.
- **Quelle:** https://developer.android.com/reference/androidx/work/Constraints.Builder

### W3. Doppelte Backup-Worker / Akku-Drain
- **Symptom:** Mehrere identische Periodic-Worker laufen.
- **Ursache:** `enqueue()` statt unique, oder `REPLACE` bei jedem App-Start → Intervall-Reset, Job läuft nie zu Ende.
- **Versionen:** alle.
- **FIX:** `enqueueUniquePeriodicWork("drive_backup", KEEP, request)`; nur bei Konfigänderung UPDATE.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/workmanager/how-to/unique-work

### W4. App im rare/restricted Bucket → Backup läuft tagelang nicht
- **Symptom:** Bei selten geöffneter App läuft das Backup tagelang nicht.
- **Ursache:** App-Standby-Bucket (selten geöffnet) → strenge Job-Drosselung + Netz-Limit (OS-Energiemanagement, kein Bug).
- **Versionen:** Android 9+.
- **FIX:** Constraints lockern, 1×/Tag-Intervall; beim App-Öffnen überfälliges Backup anstoßen (App-Öffnen hebt Bucket auf active).
- **Quelle:** https://developer.android.com/topic/performance/appstandby

### W5. Worker nach ~10 Min hart gestoppt
- **Symptom:** Großes Backup wird mittendrin abgebrochen.
- **Ursache:** Standard-Lauf-Deadline ~10 Min pro Worker-Lauf.
- **Versionen:** alle.
- **FIX:** Long-Running-Worker (`setForeground`) oder gechunkt + resumable + `Result.retry()`; `getStopReason()` loggen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running

### W6. Android 16: Long-Running-Worker verbrennt Job-Quota
- **Symptom:** Backup gestoppt / Folgejobs starten nicht; `getStopReason()` zeigt Quota.
- **Ursache:** Ab Android 16 zählen Jobs neben einem FGS gegen app-weites Job-Runtime-Quota.
- **Versionen:** Android 16.
- **FIX:** FGS direkt starten ODER (user-initiiert) **UIDT-Job** (quota-exempt); Worker resumable.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-all

### W7. Hilt + WorkManager `NoSuchMethodException` ⭐ HAEUFIG
- **Symptom:** Crash `NoSuchMethodException: <init> [Context, WorkerParameters]` beim Worker-Start.
- **Ursache:** Default-WorkManager-Initializer im Manifest nicht entfernt → `HiltWorkerFactory` nicht genutzt.
- **Versionen:** Hilt + WorkManager.
- **FIX:** Default-Initializer per `tools:node="remove"` entfernen; `Application : Configuration.Provider` + `HiltWorkerFactory`; `@HiltWorker` + `@AssistedInject`.
- **Quelle:** https://medium.com/@santimattius/workmanager-with-hilt-and-app-startup-80b34062e144

### W8. Expedited-Worker ohne `getForegroundInfo()` (< Android 12)
- **Symptom:** Expedited Work scheitert/keine Notification auf alten Geräten.
- **Ursache:** < Android 12 nutzt Expedited einen FGS-Fallback; ohne `getForegroundInfo()` keine Notification.
- **Versionen:** < Android 12.
- **FIX:** `getForegroundInfo()` im Worker implementieren; `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` als Fallback.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### W9. Worker-Rename/-Move → `ClassNotFoundException` nach Update
- **Symptom:** Crash nach Update bei Nutzern mit pending Work.
- **Ursache:** WorkManager persistiert den FQCN; Umbenennen/Verschieben → alter Name nicht instanziierbar.
- **Versionen:** alle.
- **FIX:** Worker nicht hart umbenennen/verschieben; alten Namen als Stub behalten / `DelegatingWorkerFactory` mappen.
- **Quelle:** https://jeroenmols.com/blog/2022/04/27/workmanager-crash/

---

## F) Foreground-Service / Backup

### F1. `dataSync`-FGS-ANR nach 6h (Android 15)
- **Symptom:** „A foreground service of type dataSync did not stop within its timeout" / `ForegroundServiceStartNotAllowedException: Time limit already exhausted`.
- **Ursache:** 6h/24h-Limit unter targetSdk 35; `onTimeout()` nicht behandelt / erneuter Start nach Erschöpfung.
- **Versionen:** Android 15 / targetSdk 35.
- **FIX:** `onTimeout()` → `stopSelf()`; besser UIDT/WorkManager; Vordergrund-Rückkehr resettet das Budget.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### F2. `dataSync`-FGS fürs Backup ist der falsche Mechanismus
- **Symptom:** Reject-/Quota-/Timeout-Probleme beim Drive-Upload als `dataSync`-FGS.
- **Ursache:** Google rät für Netzwerk-Transfer aktiv zu WorkManager (auto) / UIDT (user-initiiert) statt `dataSync`.
- **Versionen:** Android 14+.
- **FIX:** Auto-Backup → WorkManager; „Jetzt sichern" kurz → expedited Worker, lang → UIDT.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/data-transfer-options

### F3. UIDT-Job startet nicht
- **Symptom:** UIDT abgelehnt/`RESULT_FAILURE`.
- **Ursache:** Muss durch sichtbare User-Aktion im Vordergrund; `RUN_USER_INITIATED_JOBS` nötig; läuft über `JobScheduler.setUserInitiated(true)`, nicht `setExpedited`; Notification Pflicht.
- **Versionen:** Android 14+.
- **FIX:** An User-Tap koppeln; Permission deklarieren; `setNotification()` in `onStartJob`; ab API 34 gaten, darunter WorkManager-FGS-Fallback.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/uidt

### F4. UIDT verliert State bei Low-Memory-Kill
- **Symptom:** Transfer startet bei Resume von vorne / Fortschritt weg.
- **Ursache:** Bei Low-Memory wird `onStopJob()` NICHT gerufen.
- **Versionen:** Android 14+.
- **FIX:** Transfer-State persistent halten, in `onStartJob()` resumen (resumable).
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/uidt

### F5. Unsichtbarer FGS bei verweigertem POST_NOTIFICATIONS
- **Symptom:** Backup läuft, aber keine Notification (nur Task-Manager).
- **Ursache:** Ab Android 13 ist `POST_NOTIFICATIONS` runtime; verweigert → FGS-Notification nicht im Drawer (FGS läuft weiter).
- **Versionen:** Android 13+.
- **FIX:** `POST_NOTIFICATIONS` runtime anfragen; Backup-Logik nicht von sichtbarer Notification abhängig machen.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/notification-permission

### F6. FGS-Typ im Manifest fehlt → `MissingForegroundServiceTypeException`
- **Symptom:** Crash beim `setForeground()`/FGS-Start.
- **Ursache:** Ab Android 14 muss der FGS-Typ deklariert sein; bei WorkManager am `SystemForegroundService` mergen; `ForegroundInfo`-Typ muss Manifest decken.
- **Versionen:** Android 14+.
- **FIX:** `<service android:name="androidx.work.impl.foreground.SystemForegroundService" android:foregroundServiceType="dataSync" tools:node="merge"/>` + Permission; `ForegroundInfo(id, notif, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running

---

## O) OEM-Killings

### O1. OEM-Power-Manager killt Alarme/Worker/FGS ⭐ HAEUFIG
- **Symptom:** Code korrekt, aber auf Xiaomi/Samsung/Huawei/Oppo feuern Reminder/Backups nicht, wenn App im Hintergrund.
- **Ursache:** Aggressive OEM-Battery-Manager (PowerKeeper/PowerGenie/„Sleeping apps"/Deep Optimization) jenseits AOSP-Doze; `setExactAndAllowWhileIdle` + FGS werden trotzdem gekillt.
- **Versionen:** geräteabhängig.
- **FIX:** Defense-in-Depth: WorkManager-Watchdog (15 Min, holt Verpasstes nach) + Boot/Update/Permission-Receiver + Battery-Exemption-Prompt + `Build.MANUFACTURER`-Erkennung → dontkillmyapp-Anleitung; FCM High-Priority als robusterer Trigger.
- **Quelle:** https://dontkillmyapp.com/problem

### O2. OTA-Update setzt Battery-Exemption/Autostart still zurück
- **Symptom:** Nach System-Update feuert nichts mehr (kein Crash/Log).
- **Ursache:** OTA resettet Exemption/Autostart-Whitelist (Samsung/Xiaomi/OnePlus).
- **Versionen:** geräteabhängig.
- **FIX:** Bei `MY_PACKAGE_REPLACED`/App-Start `isIgnoringBatteryOptimizations()` neu prüfen → Nutzer erneut zum Exemption-Dialog; Ketten neu setzen.
- **Quelle:** dev.to/stoyan_minchev (extern)

### O3. Samsung legt manuell aufgeweckte App nach ~3 Tagen wieder schlafen
- **Symptom:** App zu „Never sleeping apps" hinzugefügt, nach 3 Tagen feuert nichts.
- **Ursache:** „Put unused apps to sleep" ist separat aktiv und überschreibt die Wahl.
- **Versionen:** Samsung One UI.
- **FIX:** Nutzer muss ZUSÄTZLICH „Put unused apps to sleep" deaktivieren (beides in die In-App-Anleitung).
- **Quelle:** https://dontkillmyapp.com/samsung

### O4. Honor/Huawei PowerGenie flaggt häufige `setAlarmClock()`
- **Symptom:** App auf Honor binnen Stunden gekillt.
- **Ursache:** PowerGenie zählt `setAlarmClock()`-Frequenz; > ~3×/Tag → „weckt System häufig" → Kill.
- **Versionen:** Honor/Huawei EMUI.
- **FIX:** AlarmClock-Safety-Net auf ~8-h-Intervall drosseln; häufige Wakes über WorkManager statt AlarmClock.
- **Quelle:** dev.to/stoyan_minchev (extern)

### O5. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → Play-Ablehnung
- **Symptom:** Release abgelehnt/Bann.
- **Ursache:** Direkter Permission-Request ist policy-restricted; ohne triftige Begründung nicht erlaubt.
- **Versionen:** Play-Policy.
- **FIX:** Für Reminder-Kern gerechtfertigt — im Declaration-Form begründen, nicht für Marketing/Analytics; alternativ `USE_EXACT_ALARM` (nur wenn Reminder-Kategorie zutrifft).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9888170

### O6. Persistente FGS-Notification nervt
- **Symptom:** Dauer-Notification der FGS sichtbar trotz „silent".
- **Ursache:** OEMs auto-granten POST_NOTIFICATIONS bei Channels mit IMPORTANCE_DEFAULT+.
- **Versionen:** geräteabhängig.
- **FIX:** FGS-Channel auf `IMPORTANCE_MIN` (bleibt still, FGS behält Prozess-Priorität).
- **Quelle:** dev.to/stoyan_minchev (extern)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-workmanager-notifications.md`) |
|---------------|------------------------------------------------------------------------|
| A1–A9 (AlarmManager/Reminder) | §2 (Reminder-Pattern), §3 (Exact/Policy), §4 (PendingIntent) |
| N1–N9 (Notifications) | §5 (Channels & POST_NOTIFICATIONS), §4 (PendingIntent) |
| B1–B8 (Boot/Reschedule) | §6 (Reschedule nach Reboot/Update) |
| W1–W9 (WorkManager) | §7 (WorkManager für Backups), §1 (Architektur) |
| F1–F6 (FGS/Backup) | §8 (FGS-Typen & UIDT) |
| O1–O6 (OEM-Killings) | §9 (OEM-Killings & Workarounds) |
