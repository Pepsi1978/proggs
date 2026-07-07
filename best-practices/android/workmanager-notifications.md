# WorkManager & Notifications — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man in BestJournal (Android, Kotlin) zuverlässige Reminder-Benachrichtigungen
> (smarte Standardzeiten pro Wochentag) und robuste Hintergrund-Backups baut.
> **Versions-Anker:** targetSdk **35** (Android 15), minSdk **26** (Android 8) · WorkManager
> empfohlen **2.11.x** (SDK-35-kompatibel ab 2.10.0; Projekt nutzt aktuell **keine** WorkManager-Dep,
> Reminder laufen über **AlarmManager**) · `java.time` ab API 26 **nativ** · `POST_NOTIFICATIONS` +
> `RECEIVE_BOOT_COMPLETED` deklariert; **kein** Exact-Alarm-Permission.
> **Gegenstück (was schiefgeht):** [`bugs/android/workmanager-notifications.md`](../../bugs/android/workmanager-notifications.md).

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

---

## 1) Architektur-Entscheidung: AlarmManager vs. WorkManager

`offiziell`

- **Reminder zur exakten Uhrzeit → AlarmManager.** WorkManager ist **nicht zeitgenau** (Min-Intervall
  15 Min, durch Doze/Buckets verschoben) — „erscheine um 20:00 wie ein Wecker" gehört NICHT in WorkManager.
- **Backups (verschiebbar, constraint-abhängig, reboot-fest) → WorkManager.** Garantierte Ausführung,
  überlebt Reboot automatisch (eigene SQLite-DB, kein Boot-Receiver nötig), Constraint-System für Netz/Akku.
- **Zwei getrennte Pfade, nie vermischen:** `AlarmManager` (exakte Reminder-Notifications) +
  `WorkManager`/UIDT (Drive-Backup).
- **Quellen:** https://developer.android.com/develop/background-work/services/alarms · https://developer.android.com/topic/libraries/architecture/workmanager

## 2) Reminder-Pattern: One-shot + Reschedule (pro Wochentag)

`offiziell` · `extern` (Pattern)

- **`setRepeating` NICHT verwenden:** Seit API 19 sind alle `setRepeating`-Alarme **inexakt** (gebatcht,
  driften), und ein festes Intervall (`INTERVAL_DAY`) kann **keine** unterschiedlichen Zeiten pro
  Wochentag abbilden. (Das ist exakt der aktuelle BestJournal-Zustand → umstellen.)
- **Pattern:** Persistente Reminder-Definition (Room) → **nächsten Trigger berechnen** → **einen**
  one-shot-Alarm setzen → im `BroadcastReceiver` Notification posten **und sofort den nächsten Slot
  neu planen** → bei Boot/Update alles aus der DB neu registrieren. Kein Drift (jeder Termin frisch berechnet).
- **Nächste-Trigger-Berechnung mit `java.time`** (ab API 26 nativ): `ZonedDateTime.now(zoneId)`, über die
  nächsten **8 Tage** iterieren, pro aktivem `DayOfWeek` die zugehörige `LocalTime` setzen, den frühesten
  Zeitpunkt **echt > now** nehmen, `.toInstant().toEpochMilli()`. Nie nackte `Calendar`-Millis-Arithmetik
  (bricht bei Monats-/DST-Grenzen).
- **Smart Defaults** (editierbar, nicht hart verdrahtet): z. B. Mo–Fr 20:00, Sa/So 10:00. Snooze =
  separater one-shot mit eigenem Request-Code. Quiet-Hours-Check beim Reschedule.
- **Quellen:** https://developer.android.com/develop/background-work/services/alarms · https://developer.android.com/reference/java/time/ZonedDateTime · https://www.baeldung.com/java-daylight-savings

## 3) Exact vs. inexact & Android-14-Policy

`offiziell`

- **Für eine Journal-App reicht inexakt:** `setAndAllowWhileIdle()` (one-shot, dann reschedule) — feuert
  auch in Doze, Drift typisch wenige Minuten, **kein Exact-Permission, kein Play-Risiko**. Google nennt
  genau diesen Fall: „Aktion zu ungefährer Zeit, auch im Idle-Zustand → `setAndAllowWhileIdle()`".
- **`USE_EXACT_ALARM` NICHT deklarieren:** Play-Policy beschränkt es auf Wecker-/Kalender-Apps; eine
  Journal-/Reminder-App qualifiziert sich nicht zuverlässig → Veröffentlichung kann abgelehnt werden.
- **`SCHEDULE_EXACT_ALARM`** ist ab Android 14 (targetSdk 33+) **denied-by-default** (Manifest reicht
  nicht). Nur falls echt sekundengenau nötig: vor jedem `setExact*` `canScheduleExactAlarms()` prüfen,
  bei `false` über `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` führen und auf
  `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` lauschen + neu planen; sonst auf
  `setAndAllowWhileIdle` zurückfallen.
- **Doze-Drossel:** `setAndAllowWhileIdle`/`setExactAndAllowWhileIdle` feuern in Doze max. **~1×/9 Min**
  (Low-Power ~15 Min). Für tägliche Reminder irrelevant; dichte Alarme entsprechend einplanen.
- **Quellen:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms · https://developer.android.com/develop/background-work/services/alarms

## 4) PendingIntent: Flags & Request-Codes

`offiziell`

- **`FLAG_IMMUTABLE` ist ab Android 12 Pflicht** (sonst `IllegalArgumentException`); für Reminder
  `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. `FLAG_MUTABLE` nur bei RemoteInput/Direct-Reply/Bubbles.
- **Eindeutiger, stabiler Request-Code pro Reminder** (z. B. Room-Row-ID). Zwei PendingIntents mit
  gleichem Request-Code + per `filterEquals` gleichem Intent gelten als identisch → der zweite
  überschreibt den ersten (Extras zählen beim Vergleich NICHT). Daten über Request-Code/Intent-`data`-URI
  unterscheiden, nicht über Extras.
- Canceln: gleichen Request-Code + `FLAG_NO_CREATE` holen, dann `alarmManager.cancel(pi)`.
- **Quelle:** https://developer.android.com/reference/android/app/PendingIntent

## 5) Notification-Channels & POST_NOTIFICATIONS

`offiziell`

- **Channels (API 26+ Pflicht):** idempotent in `Application.onCreate()` anlegen (Re-Create mit gleichen
  Werten = No-Op). **Importance/Sound/Vibration sind nach erster Anlage unveränderlich** (Nutzer-Hoheit);
  nur Name/Beschreibung änderbar. Soll sich das Verhalten ändern → **neue Channel-ID** (`reminders_v2`),
  nicht bei jedem Update hochzählen (Settings zeigt Zahl gelöschter Channels als Spam-Schutz).
- Für hörbare Reminder Channel mit **`IMPORTANCE_HIGH`** (sonst kein Heads-up/Sound); Sound/Vibration
  am **Channel** setzen (`setSound`/`enableVibration` am Builder sind ab API 26 wirkungslos);
  `setCategory(CATEGORY_REMINDER)`.
- **POST_NOTIFICATIONS (Android 13+):** runtime anfragen (Manifest reicht nicht). Bei targetSdk 35 volle
  Kontrolle über den Zeitpunkt → im Kontext anfragen (wenn Nutzer den ersten Reminder anlegt), mit
  `shouldShowRequestPermissionRationale`. Nach 2× „Don't allow" = dauerhaft verweigert →
  `Settings.ACTION_APP_NOTIFICATION_SETTINGS` statt vergeblichem Dialog.
- **Pflichtfelder:** `setSmallIcon` (sonst unsichtbar), `setContentTitle`, gültige `channelId`,
  `setAutoCancel(true)`, `setContentIntent`. Tap-Intent **direkt** auf die Ziel-Activity
  (`PendingIntent.getActivity`), **kein Trampolin** über Receiver/Service (ab Android 12 verboten).
- **Diagnose „kommt nicht an":** `areNotificationsEnabled()` (App-Ebene) + `getNotificationChannel(id).importance`
  (Channel-Ebene) zusammen prüfen.
- **Full-Screen-Intent meiden** (ab Android 14 nur Wecker/Anruf-Apps); Heads-up via `IMPORTANCE_HIGH`.
  Android-15-Cooldown bedenken (dichte Notifications werden leiser) → Reminder entzerren.
- **Quellen:** https://developer.android.com/develop/ui/views/notifications/channels · https://developer.android.com/develop/ui/views/notifications/notification-permission

## 6) Reschedule nach Reboot / Update / Permission-Change

`offiziell`

- **AlarmManager-Alarme überleben Reboot NICHT** → Receiver mit `RECEIVE_BOOT_COMPLETED` +
  Intent-Filter `BOOT_COMPLETED`. Eine zentrale **`rescheduleAll()`** (liest aktive Reminder aus Room),
  aufgerufen aus: Boot-Receiver, **jedem App-Start**, und nach Exact-Alarm-Permission-Grant.
- **App-Update:** zusätzlich `MY_PACKAGE_REPLACED` im selben Filter (Alarme werden bei Replace gecancelt).
- **Receiver-Manifest (Android 12+):** `android:exported="true"` (System-Broadcast von außen),
  optional `directBootAware="true"` + `LOCKED_BOOT_COMPLETED` für früheste Wiederherstellung (dann
  Reminder-State in **device-protected Storage**). Plus `QUICKBOOT_POWERON` (+ HTC) für OEM-Fast-Boot.
- **Receiver-Budget:** ~10 s → schwere Arbeit per `goAsync()` (Background-Thread + `finish()`) oder
  WorkManager-Job; **keine** verbotenen FGS-Typen aus `BOOT_COMPLETED` starten (Android 15).
- **Force-Stop (nicht abfangbar):** danach feuern keine Broadcasts (auch nicht `BOOT_COMPLETED`), bis der
  Nutzer die App manuell öffnet; **Android 15 cancelt zusätzlich alle PendingIntents** beim Eintritt in den
  stopped state. → Deshalb `rescheduleAll()` bei jedem App-Start; im Onboarding erklären.
- **WorkManager braucht KEINEN Boot-Receiver** (reschedult sich selbst).
- **Quellen:** https://developer.android.com/develop/background-work/services/alarms · https://developer.android.com/privacy-and-security/direct-boot · https://developer.android.com/about/versions/15/behavior-changes-all

## 7) WorkManager für Backups

`offiziell`

- **Auto-Backup:** `PeriodicWorkRequest` (Intervall 6–24 h, optional `flexInterval`) +
  **`enqueueUniquePeriodicWork("drive_backup", KEEP, request)`** (KEEP beim App-Start, nicht ständig
  replacen — sonst doppelte Worker / Intervall-Reset). Bei Konfigänderung **UPDATE** (erhält Enqueue-Zeit).
- **Constraints minimal:** `UNMETERED` + `BatteryNotLow` reicht. **Nie `Charging` + `DeviceIdle`
  kombinieren** (auf vielen Geräten nie gleichzeitig erfüllt → Job startet nie; WorkManager warnt per Lint).
  Periodische Läufe können bei unerfüllten Constraints nicht nur verzögert, sondern **ausgelassen** werden
  → großzügiges Intervall + beim App-Start prüfen „letztes Backup überfällig?" und einmalig anstoßen.
- **Periodisch ≠ zeitgenau:** Doze-Maintenance-Windows + App-Standby-Buckets (active→restricted)
  verzögern; selten geöffnete App → schlechterer Bucket → seltenere Backups (gewollt, nicht umgehbar).
- **Expedited** (`setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)`) nur für kurzes, user-initiiertes
  „Jetzt sichern"; Quota auf Android 12+ beachten; `getForegroundInfo()` implementieren (FGS-Fallback < A12).
- **Lange Backups:** Worker-Lauf-Deadline ~10 Min → gechunkt + resumable + `Result.retry()` mit
  `setBackoffCriteria(EXPONENTIAL)`; `runAttemptCount` als Abbruchschwelle; **`getStopReason()` loggen**.
- **Android 15/16:** `dataSync`-FGS 6h/24h-Limit; **Android 16** zählt Jobs neben FGS gegen app-weites
  Job-Quota → lange Worker können gestoppt werden → resumable bauen, für lange User-Transfers UIDT (§8).
- **Hilt:** `@HiltWorker` + `Configuration.Provider` + Default-WorkManager-Initializer im Manifest
  entfernen (sonst `NoSuchMethodException` nach Prozess-Tod).
- **Auf WorkManager 2.11.x** aktualisieren (SDK-35-kompatibel ab 2.10.0).
- **Quellen:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work · https://developer.android.com/topic/performance/appstandby · https://developer.android.com/about/versions/16/behavior-changes-all

## 8) Foreground-Service-Typen & UIDT für Backup

`offiziell`

- **Entscheidungsbaum (offiziell):** Auto/periodisch, < 10 Min, deferrable → **WorkManager**.
  User-tippt-„Jetzt sichern", soll Fortschritt sehen, Unterbrechung schädlich → **UIDT**. Kurz & kritisch
  (< 3 Min) → `shortService`. **`dataSync`-FGS möglichst vermeiden** (Google rät aktiv ab).
- **UIDT** (`JobScheduler.setUserInitiated(true)`, Permission `RUN_USER_INITIATED_JOBS`, Notification
  Pflicht): **quota-exempt, kein 6h-Limit** → ideal für potenziell lange Backups. Nur planbar, wenn App
  sichtbar; resumable bauen (bei Low-Memory kein `onStopJob()`). Kein Jetpack-API → ab API 34 gaten,
  darunter WorkManager-FGS-Fallback.
- **Falls `dataSync`-FGS:** `foregroundServiceType` im Manifest + `ForegroundInfo(id, notif,
  FOREGROUND_SERVICE_TYPE_DATA_SYNC)` (ab API 34 Pflicht); `onTimeout()` → `stopSelf()`; Vordergrund-Rückkehr
  resettet das 6h-Budget. **Notification-Pflicht** — `POST_NOTIFICATIONS`-denied macht FGS-Notification
  unsichtbar (FGS läuft weiter), Backup-Logik darf nicht davon abhängen.
- **Cancel-Action** via `WorkManager.createCancelPendingIntent(id)`.
- **Quellen:** https://developer.android.com/develop/background-work/background-tasks/data-transfer-options · https://developer.android.com/develop/background-work/background-tasks/uidt · https://developer.android.com/develop/background-work/services/fgs/timeout

## 9) OEM-Killings & Workarounds

`offiziell` (Doze/Battery-Exemption) · `extern` (dontkillmyapp)

- **Aggressivste OEMs** (dontkillmyapp 5/5): Xiaomi (MIUI/HyperOS PowerKeeper + Autostart), Samsung (One UI
  „Sleeping apps"), OnePlus (Deep Optimization), Huawei/Honor (PowerGenie, HwPFWService). Sie killen Alarme/
  Worker/FGS **über** AOSP-Doze hinaus — `setExactAndAllowWhileIdle` + FGS werden trotzdem gekillt.
- **Defense-in-Depth:** (1) WorkManager-Periodic-**Watchdog** (15 Min, prüft „verpasst?" + holt nach);
  (2) Boot-/Update-/Permission-Receiver gegen stille Tode; (3) **Battery-Exemption** prüfen/anfragen
  (`isIgnoringBatteryOptimizations()` → `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`); (4)
  **`Build.MANUFACTURER`-Erkennung** → device-spezifische In-App-Anleitung (dontkillmyapp-Pattern:
  Xiaomi Autostart, Samsung „Never sleeping apps" **+** „Put unused apps to sleep" AUS).
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** ist Play-policy-beschränkt — nur mit triftiger
  (kritischer-Reminder-)Begründung, nie für Marketing/Analytics.
- **OTA setzt Exemptions zurück** → bei `MY_PACKAGE_REPLACED`/App-Start Exemption-Status neu prüfen.
- **Honor-Eigenheit:** PowerGenie zählt `setAlarmClock()`-Frequenz (> ~3×/Tag → Kill) → AlarmClock-Safety-Net
  auf ~8 h drosseln. **FCM High-Priority** weckt das Gerät robuster als lokaler Alarm (OEMs killen FCM selten).
- **Quellen:** https://dontkillmyapp.com · https://developer.android.com/training/monitoring-device-state/doze-standby · https://firebase.google.com/docs/cloud-messaging/android/message-priority

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/android/workmanager-notifications.md`) |
|-------------------------|---------------------------------------------------------------------|
| §1 (Architektur) | A1, W1 (WorkManager nicht zeitgenau) |
| §2 (Reminder-Pattern) | A1 (setRepeating-Drift), A6 (DST), A7 (vergangener Trigger) |
| §3 (Exact/Policy) | A2 (SecurityException), A3 (USE_EXACT_ALARM-Reject), A4 (Doze-9-Min), A5 (Permission-Widerruf) |
| §4 (PendingIntent) | N4 (FLAG_IMMUTABLE), A8 (Request-Code-Kollision), A10 (cancel-Slot-Leak) |
| §5 (Notifications) | N1–N21 (Channel-Immutability/POST_NOTIFICATIONS/Trampolin/Full-Screen/Heads-up/ID-Kollision/Gruppen/DND/Cooldown/Bitmap-Limit) |
| §6 (Reboot/Reschedule) | B1–B17 (BOOT_COMPLETED/Force-Stop/Update/Direct-Boot/FBE/exported/FGS-aus-Boot/Action-Tippfehler) |
| §7 (WorkManager-Backup) | W1–W25 (Periodic-Timing/Constraints/Expedited/Quota/Hilt-KSP/Init/Testing/Compose/R8) |
| §8 (FGS/UIDT) | F1–F15 (dataSync-Timeout/6h-Budget/shortService/StartNotAllowed/Typ-Mismatch/While-in-use/UIDT) |
| §9 (OEM-Killings) | O1–O18 (OEM-Kills/OTA-Reset/MIUI-Autostart/Samsung-Guardians/PowerGenie/ColorOS/FCM-Prio/dontkillmyapp-API) |

> **Checkpoint:** Vollständig recherchiert in 2 Durchläufen (Best-Practices + dedizierte Bug-Recherche,
> je 7 Researcher; Bug-Lauf mit IssueTracker-/reale-Vorfälle-Fokus, Juni 2026).
> Kern für BestJournal: Reminder von `setRepeating` auf **One-shot + Reschedule (inexakt, java.time)**
> umstellen, Backups in **WorkManager** (UIDT fürs lange „Jetzt sichern"), beides getrennt halten.
