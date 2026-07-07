# Bekannte Bugs: WorkManager & Notifications (Reminder + Hintergrund-Backups)

> PFLICHT-LESEN vor Arbeit an Reminder/Benachrichtigungen oder Hintergrund-Backups in BestJournal.
> Stand: tief recherchiert am 2026-06-14 in **zwei Durchläufen** — (1) Best-Practices-Lauf, (2) dedizierte
> Bug-Recherche (je 7 Researcher parallel; Fokus Lauf 2: Google IssueTracker, reale Crash-/OEM-Vorfälle,
> Fix-Versionen). ~110 Einträge in 6 Sektionen.
> Versions-Anker: targetSdk **35** (Android 15), minSdk **26** · WorkManager 2.11.x — neuester belegter Fix-Stand
> **2.11.2** (Projekt aktuell AlarmManager-basiert, keine WorkManager-Dep) · `java.time` ab API 26 nativ ·
> `POST_NOTIFICATIONS` + `RECEIVE_BOOT_COMPLETED` deklariert, kein Exact-Alarm-Permission.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/android/workmanager-notifications.md`](../../best-practices/android/workmanager-notifications.md).

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

### A10. `cancel()` gibt OS-Alarm-Slots nicht frei → "max 500 alarms" trotz "0 aktiver" ⭐ HAEUFIG
- **Symptom:** `IllegalStateException: Maximum limit of concurrent alarms 500 reached for uid` (ab API 31), obwohl die App scheinbar 0 Alarme hält.
- **Ursache:** Beim Massen-Schedule/Cancel bleibt der OS-Slot belegt, wenn `cancel()` einen NICHT-identischen PendingIntent bekommt; "Zombie"-Alarme summieren sich (v.a. Samsung).
- **Versionen:** API 31+ (Android 12–15).
- **FIX:** `cancel(pi)` mit IDENTISCHEM PendingIntent (gleiche Action/Data/Component/Request-Code/Flags) wie beim `set()`; danach zusätzlich `pi.cancel()`. Besser: statt 1 Alarm pro Reminder die Self-Rescheduling-Kette (genau 1 aktiver Alarm) — deckt sich mit A1.
- **Quelle:** https://github.com/invertase/notifee/issues/349 (extern)

### A11. `setAlarmClock()` zeigt App-Termin als "nächsten Wecker" in Statusleiste/Lockscreen
- **Symptom:** Wecker-Icon in der Statusleiste + App-Reminder als "nächster Wecker" auf dem Lockscreen; Nutzer verwirrt.
- **Ursache:** Das Wecker-Icon ist nicht der Uhr-App vorbehalten — jede `setAlarmClock()`-App triggert es; der app-übergreifend früheste AlarmClock gewinnt, die anderen sind unsichtbar.
- **Versionen:** API 21+ (bis Android 15).
- **FIX:** Für stille Hintergrund-Reminder `setExactAndAllowWhileIdle()` statt `setAlarmClock()` (kein Icon, fast gleiche Zuverlässigkeit). Wenn AlarmClock nötig: `showIntent` auf App-Deep-Link + Onboarding-Hinweis.
- **Quelle:** https://developer.android.com/reference/androidx/core/app/AlarmManagerCompat

### A12. `OnAlarmListener`-Überladung umgeht SCHEDULE_EXACT_ALARM (nur In-Process)
- **Symptom:** Verwirrung um die Permission — entweder unnötiger Flow oder der Alarm feuert nie.
- **Ursache:** `setExact(type, time, tag, listener, handler)` braucht KEINE Exact-Alarm-Permission, feuert aber nur solange der Prozess lebt; für persistente Alarme ungeeignet (feuert nie, ohne Fehler).
- **Versionen:** API 24+ (bis Android 15).
- **FIX:** Listener-Pfad nur für kurze In-Process-Timer (App sichtbar). Persistente Reminder weiter über PendingIntent (+ inexakt). Im Code klar dokumentieren, welcher Pfad welche Lebensdauer hat (Poka-Yoke).
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### A13. Inexakter `set()`/`setWindow()` klemmt das Fenster auf ≥10 Min (Android 14: bis ~1 h)
- **Symptom:** "in 2 Min" feuert 10+ Min später; Android 14: Reminder "1 h vorher" feuert mehrere Minuten zu spät.
- **Ursache:** targetSdk ≥ 31 klemmt jedes `windowLengthMillis` < 600 000 ms auf 10 Min; der erste inexakte Alarm nach Start/Boot ist zusätzlich ≥ 10 Min verzögert; Android 14 batcht stärker.
- **Versionen:** API 31+ (verschärft Android 14).
- **FIX:** Genauigkeit nötig → exakter Alarm mit passender Permission (Wecker-Kategorie) bzw. `setAlarmClock`; sonst das 10-Min-Clipping einplanen, keine engeren Fenster versprechen. (Anders als A4: hier Fenster-Clamp, nicht Doze-Drossel.)
- **Quelle:** https://developer.android.com/develop/background-work/services/alarms

### A14. `…PERMISSION_STATE_CHANGED` nicht empfangen / `canScheduleExactAlarms()` beim Kaltstart stale
- **Symptom:** Nutzer erteilt Exact-Alarm-Permission in den Settings → App plant nichts neu; oder die erste Abfrage liefert fälschlich `false`.
- **Ursache:** Erteilung außerhalb der App sendet `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`; ohne Receiver bleibt alles leer. `canScheduleExactAlarms()` kann direkt nach Kaltstart veraltet sein.
- **Versionen:** Android 12+ (verschärft 14).
- **FIX:** Receiver auf `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` → `rescheduleAll()` (eine Funktion, geteilt mit Boot); nicht auf eine einzige frühe `canScheduleExactAlarms()`-Abfrage vertrauen. Für ein Journal umgeht inexakt alles.
- **Quelle:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

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

### N10. Notification-ID-Kollision: gleiche ID überschreibt still ⭐ HAEUFIG
- **Symptom:** Statt mehrerer Notifications nur eine; jede neue ersetzt die vorige.
- **Ursache:** `notify(id)` identifiziert über `(tag, id)`; gleiche ID = Update, nicht neuer Eintrag. Fix-kodiertes `notify(0, …)` kollabiert alles.
- **Versionen:** alle (by design).
- **FIX:** Pro Entität eindeutige ID (Zähler / `System.currentTimeMillis().toInt()`) ODER stabiler String-Tag: `notify("reminder_$id", BASE_ID, n)` (Tag = stabile Identität pro Reminder).
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/build-notification

### N11. Custom-Sound spielt nicht — Channel-Sound nur bei Erstanlage + raw-Fallen
- **Symptom:** Eigener Ton wird nie abgespielt, Standard/Stille stattdessen.
- **Ursache:** Ab API 26 ist Builder-`setSound()` wirkungslos (nur Channel zählt) UND der Channel-Sound nach Anlage unveränderlich; raw-Resource muss lowercase, ≤128 kbps, <30 s sein; `content://` braucht URI-Permission.
- **Versionen:** API 26+.
- **FIX:** Sound am Channel via `setSound(uri, AudioAttributes)`; bei Änderung neue Channel-ID (`alerts_v2`); raw-Resource statt `content://`.
- **Quelle:** https://github.com/FirebaseExtended/flutterfire/issues/523 (extern)

### N12. Heads-up/Ton nur beim ersten Mal bei Update derselben ID
- **Symptom:** Re-`notify()` mit gleicher ID aktualisiert still, kein erneutes Pop-up/Ton.
- **Ursache:** `setOnlyAlertOnce(true)` alarmiert nur einmal; auch ohne das drosselt das System Re-Alerts bei bereits sichtbaren `(tag,id)`.
- **Versionen:** alle (verschärft durch Android-15-Cooldown, N18).
- **FIX:** Jedes Ereignis voll alarmieren → `setOnlyAlertOnce(false)` + neue eindeutige ID; Progress leise → `setOnlyAlertOnce(true)`, Abschluss mit neuer ID.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/build-notification

### N13. Auto-Gruppierung ab 4 Notifications stiehlt eigene Intents
- **Symptom:** Ab 4 Notifications ohne Group-Key gruppiert das System selbst; Tap auf die Summary öffnet nur den Launcher (Deep-Link/Tracking weg).
- **Ursache:** Android 7+ Auto-Group erzeugt eine System-Summary mit eigenem Intent.
- **Versionen:** Android 7+.
- **FIX:** Sobald ≥4 möglich: explizit `setGroup(KEY)` auf allen Kindern + eigene Summary mit `setGroupSummary(true)` und eigenem `contentIntent`.
- **Quelle:** https://github.com/OneSignal/OneSignal-Android-SDK/issues/666 (extern)

### N14. "Silent group summary" dämpft den Alarm der ganzen Gruppe
- **Symptom:** Gruppierte Notification erzeugt keinen Ton/kein Heads-up trotz lautem Channel.
- **Ursache:** Die Summary bestimmt das Alert-Verhalten der Gruppe; eine stille/zuerst gepostete Summary dämpft die Kinder.
- **Versionen:** API 26+.
- **FIX:** `setGroupAlertBehavior(GROUP_ALERT_CHILDREN)`; Summary auf demselben (oder lauteren) Channel posten.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/group

### N15. `setOngoing` + `setAutoCancel` = nicht entfernbare Zombie-Notification
- **Symptom:** Notification weder wischbar noch per Tap entfernbar.
- **Ursache:** `setOngoing(true)` gewinnt gegen `setAutoCancel(true)` → kein Auto-Cancel, kein Wischen.
- **Versionen:** alle (Android 14 lockert die Wischbarkeit teils).
- **FIX:** Genau eines wählen — laufende Tasks `setOngoing` + manuelles `cancel(id)`; tippbare `setAutoCancel` ohne `setOngoing`.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/build-notification

### N16. DND unterdrückt Reminder — `setCategory` allein reicht nicht
- **Symptom:** Kritischer Reminder erscheint bei "Bitte nicht stören" nicht.
- **Ursache:** DND-Durchbruch braucht `channel.setBypassDnd(true)` + Permission `ACCESS_NOTIFICATION_POLICY` + Nutzerfreigabe; `setCategory` allein durchbricht nicht zuverlässig.
- **Versionen:** API 26+.
- **FIX:** Nur für echte kritische Reminder: `ACCESS_NOTIFICATION_POLICY` deklarieren, Nutzer über `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` führen, dann `setBypassDnd(true)`.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### N17. `setVibrate`/`setLights` am Builder ab API 26 ignoriert
- **Symptom:** Vibration/LED bleiben aus trotz Builder-Aufruf.
- **Ursache:** Ab API 26 nur Channel (`enableVibration`/`setVibrationPattern`, `enableLights`/`setLightColor`); Channel nach Anlage unveränderlich.
- **Versionen:** API 26+.
- **FIX:** Verhalten am Channel setzen; bei Änderung neue Channel-ID; NotificationCompat regelt < 26 automatisch.
- **Quelle:** https://developer.android.com/develop/ui/compose/notifications/channels

### N18. Android 15 Notification-Cooldown dämpft schnelle Wiederholungen
- **Symptom:** Mehrere Notifications derselben App in kurzer Folge werden zunehmend leiser.
- **Ursache:** Android 15 senkt bei wiederholten Notifications (≈ ≤ 1 Min) Lautstärke/Vibration schrittweise; default an, app-seitig nicht abschaltbar (Ausnahme Call/Alarm/priorisierte Konversation).
- **Versionen:** Android 15+.
- **FIX:** Konsolidieren statt spammen — eine aktualisierende Notification (gleiche ID, `setOnlyAlertOnce`) oder Gruppe; kritische Alerts als Conversation/Call kategorisieren.
- **Quelle:** https://www.androidcentral.com/apps-software/android-15-notification-cooldown (extern)

### N19. Android 16 `Notification.ProgressStyle`/Live-Updates nur teils verfügbar
- **Symptom:** ProgressStyle rendert nicht als Status-Bar-Chip/erhöhte Lockscreen-Position.
- **Ursache:** Basis-API in Android 16 vorhanden, volle Live-Updates-UI rollt zuerst nur auf Pixel (QPR) aus; braucht `setOngoing` + passende Kategorie.
- **Versionen:** Android 16 (API 36).
- **FIX:** ProgressStyle als progressives Enhancement, Fallback auf `setProgress(...)` für < 16 / Non-Pixel.
- **Quelle:** https://developer.android.com/about/versions/16/features/progress-centric-notifications

### N20. BigPicture/Large-Icon rendert nicht — Binder-IPC-Bitmap-Limit (~1 MB)
- **Symptom:** Bild fehlt/abgeschnitten oder Notification erscheint nicht (`TransactionTooLargeException`).
- **Ursache:** Notification geht per Binder an System-UI; die Gesamttransaktion inkl. Bitmap muss < ~1 MB bleiben.
- **Versionen:** alle.
- **FIX:** Bitmaps vor dem Setzen herunterskalieren (BigPicture ≤ ~450 dp, Large-Icon ~64 dp); Netzbilder mit `inSampleSize` dekodieren.
- **Quelle:** https://developer.android.com/reference/android/app/Notification.BigPictureStyle

### N21. Voll-custom RemoteViews ab Android 12 anders gerendert
- **Symptom:** Eigenes Layout wird kleiner/in Standard-Rahmen eingebettet, eigene Headerzeile verschwindet.
- **Ursache:** Android 12 bettet custom RemoteViews in den Standard-Template-Rahmen ein (System-Header + Expand).
- **Versionen:** Android 12+.
- **FIX:** `DecoratedCustomViewStyle` statt voll-custom; redundante eigene Header entfernen; besser Standard-Styles (BigText/Messaging).
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

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

### B9. BOOT_COMPLETED kommt erst nach dem ersten Entsperren (FBE) ⭐ HAEUFIG
- **Symptom:** Receiver feuert nach Reboot nicht, erst nach dem ersten Unlock (bei Lockscreen-Nutzern ggf. lange gar nicht).
- **Ursache:** Seit FBE (Pflicht ab Android 10) ist der Credential-Storage bis zum Unlock verschlüsselt; `BOOT_COMPLETED` kommt erst danach. Davor nur `LOCKED_BOOT_COMPLETED` (directBootAware). Die häufigste "Reminder nach Reboot weg"-Ursache, die in Standard-Tutorials fehlt.
- **Versionen:** Android 10+ (OEM-verschärft).
- **FIX:** Zusätzlich auf `LOCKED_BOOT_COMPLETED` lauschen + `android:directBootAware="true"`, über `createDeviceProtectedStorageContext()` arbeiten; im LOCKED-Fenster nur Alarme setzen (kein FGS/Room). (Vertieft B6.)
- **Quelle:** https://github.com/hossain-khan/android-keep-alive/issues/70 (extern)

### B10. directBootAware-Receiver kann KEINEN Foreground-Service starten
- **Symptom:** `startForegroundService()` aus `LOCKED_BOOT_COMPLETED` tut nichts / Exception.
- **Ursache:** Im Direct-Boot-Fenster (vor Unlock) erlaubt das System keinen FGS-Start aus dem Receiver — nur minimale Arbeit (device-protected Storage lesen, Alarm setzen).
- **Versionen:** Android 7+.
- **FIX:** Im LOCKED-Pfad nur leichtgewichtig reschedulen (AlarmManager feuert auch gesperrt); Service-Start erst im echten `BOOT_COMPLETED`. Beide Aktionen idempotent (LOCKED + BOOT doppelt).
- **Quelle:** https://github.com/hossain-khan/android-keep-alive/issues/70 (extern)

### B11. WorkManager crasht in Direct-Boot ("Cannot initialize WorkManager in direct boot mode")
- **Symptom:** `IllegalStateException: Cannot initialize WorkManager in direct boot mode` direkt nach Reboot (vor Unlock).
- **Ursache:** WorkManagers SQLite-DB liegt in Credential-Storage; ein directBootAware-Receiver/Widget zieht WM zu früh hoch.
- **Versionen:** WorkManager generell (offen).
- **FIX:** WM nicht directBootAware machen; vor Boot-getriggertem WM-Zugriff `userManager.isUserUnlocked` prüfen; Direct-Boot-Arbeit über AlarmManager statt WM.
- **Quelle:** https://github.com/jellyfin/jellyfin-androidtv/issues/3143 (extern)

### B12. SharedPreferences/Room im Direct Boot nicht lesbar → Crash
- **Symptom:** LOCKED-Receiver liest Reminder aus Room/Prefs → leer/Crash; Firebase/Crashlytics werfen.
- **Ursache:** Default-Context zeigt auf verschlüsselten Credential-Storage; Drittlibs unterstützen device-protected Storage nicht.
- **Versionen:** Android 7+.
- **FIX:** Reschedule-Minimaldaten in device-protected Storage spiegeln (`createDeviceProtectedStorageContext()`, `moveSharedPreferencesFrom`/`moveDatabaseFrom`); Drittlib-Init hinter `isUserUnlocked`.
- **Quelle:** https://developer.android.com/privacy-and-security/direct-boot

### B13. `setComponentEnabledSetting` überschreibt das Manifest dauerhaft
- **Symptom:** Boot-Receiver feuert nie trotz `enabled="true"` im Manifest — oder bleibt nach einmaligem Deaktivieren für immer tot (auch nach Update).
- **Ursache:** Der programmatische Enabled-Zustand wird persistent in `packages.xml` geschrieben, hat Vorrang vor dem Manifest und überlebt Reboot/Force-Stop/Update.
- **Versionen:** alle.
- **FIX:** Kritische Boot-Receiver `enabled="true"` lassen und intern entscheiden; falls geschaltet wird, beim App-Start IMMER `COMPONENT_ENABLED_STATE_ENABLED` (mit `DONT_KILL_APP`) re-setzen (Selbstheilung).
- **Quelle:** https://code.luasoftware.com/tutorials/android/android-manually-enable-or-disable-broadcastreceiver/ (extern)

### B14. `goAsync()` ohne `PendingResult.finish()` → ANR + Prozess-Leak
- **Symptom:** ANR "did not call finish()" / Prozess bleibt am Leben (v.a. wenn `finish()` nur im Happy-Path steht).
- **Ursache:** Das System hält Receiver/Prozess bis `finish()`; fehlt es (Exception vor `finish()`, > 10 s), ANR.
- **Versionen:** alle.
- **FIX:** `finish()` IMMER im `finally`; Async-Arbeit auf Sekunden deckeln, sonst in WorkManager auslagern und sofort finishen.
- **Quelle:** https://github.com/Appboy/appboy-android-sdk/issues/119 (extern)

### B15. Background-Activity-Launch aus dem Boot-Receiver blockiert (Android 14/15)
- **Symptom:** `startActivity()` aus dem Receiver wird verschluckt/wirft, selbst mit `FLAG_ACTIVITY_NEW_TASK`.
- **Ursache:** BAL-Restriktionen; ab Android 15 / targetSdk 35 gewährt ein erstellter PendingIntent dem Sender per Default KEINE BAL-Rechte mehr.
- **Versionen:** Android 14 verschärft, 15 / targetSdk 35 Default-Opt-out.
- **FIX:** Full-Screen-Intent via Notification (`setFullScreenIntent`, HIGH, `USE_FULL_SCREEN_INTENT`) statt direkter Activity; wenn ein PendingIntent eine Activity starten muss: `ActivityOptions.setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)`.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### B16. Falscher Action-String (`BOOT_COMPLETE` statt `BOOT_COMPLETED`) — stiller Totalausfall
- **Symptom:** Receiver feuert nie, Build grün, kein Log.
- **Ursache:** Tippfehler `…BOOT_COMPLETE` (ohne "D") → keine Action → kein Match, kein Crash. Verwandt: `RECEIVE_BOOT_COMPLETED`-Permission vergessen → Receiver wird stumm übersprungen.
- **Versionen:** alle.
- **FIX:** Im Code die Konstante `Intent.ACTION_BOOT_COMPLETED` statt String; Manifest exakt + Permission; Test: `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -n <pkg>/<receiver>`.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/broadcasts

### B17. Implicit-Broadcast-Sperre (Android 8+) — BOOT_COMPLETED ist exempt
- **Symptom:** Andere manifest-registrierte implizite Broadcasts feuern nicht mehr; Fehlschluss, BOOT_COMPLETED sei auch betroffen.
- **Ursache:** Ab targetSdk 26 dürfen die meisten impliziten Broadcasts nicht mehr im Manifest registriert werden; `BOOT_COMPLETED`/`LOCKED_BOOT_COMPLETED` stehen aber explizit auf der Ausnahmeliste.
- **Versionen:** Android 8+ (targetSdk 35 betroffen).
- **FIX:** BOOT_COMPLETED ruhig im Manifest; nicht-exempte Broadcasts context-registrieren oder per WorkManager-Constraints lösen — vorher gegen die offizielle Exception-Liste prüfen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions

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
- **FIX:** Worker nicht hart umbenennen/verschieben; alten Namen als Stub behalten / `DelegatingWorkerFactory` mappen. Custom-Factory fängt `ClassNotFoundException` (Mapping alt→neu oder kontrolliert `null`, sonst geht der persistierte Job + Daten verloren).
- **Quelle:** https://jeroenmols.com/blog/2022/04/27/workmanager-crash/ · https://dev.to/rojanthomas (extern)

### W10. PeriodicWork stirbt still nach ungefangener Exception → gefixt 2.11.2 ⭐ HAEUFIG
- **Symptom:** Periodischer Worker läuft einmal, crasht intern mit uncaught Exception und wird NIE wieder geplant — kein Retry, kein Log, im `workdb` einfach weg.
- **Ursache:** Bei einer uncaught Exception (statt sauberem `Result.retry/failure`) versäumte WorkManager das Einplanen des nächsten Periodenlaufs (`b/443879071`).
- **Versionen:** bis 2.11.1; **gefixt 2.11.2**.
- **FIX:** Auf 2.11.2+; bis dahin `doWork()`-Body komplett in try/catch, im catch `Result.retry()`/`failure()`.
- **Quelle:** https://issuetracker.google.com/issues/443879071

### W11. Netzwerk-Constraint-Regressions-Cluster 2.10.x ⭐ HAEUFIG
- **Symptom:** Der 2.+ Worker mit Netz-Constraint bleibt fälschlich `ConstraintsNotMet`; Android 15: Constraint gilt fälschlich als erfüllt (false positive).
- **Ursache:** Constraint-Tracking-Bugs ab 2.10.0 (`NetworkCapabilities` nicht gecached, re-added Capabilities, Android-15-blocked-Zustand, `TooManyRequestsException`).
- **Versionen:** eingeführt 2.10.0; nachgebessert 2.10.1 → **2.11.1**.
- **FIX:** Auf 2.11.1+ (mindestens 2.10.3); nie auf 2.10.0–2.10.2 bleiben.
- **Quelle:** https://issuetracker.google.com/issues/427115602 · https://issuetracker.google.com/issues/465016918

### W12. Reboot-Reschedule-ANR im `RescheduleReceiver` bei zwei Broadcasts → gefixt 2.9.0
- **Symptom:** ANR "androidx.work…RescheduleReceiver" nach Reboot, wenn `BOOT_COMPLETED` + `TIME_SET`/`MY_PACKAGE_REPLACED` quasi gleichzeitig eintreffen.
- **Ursache:** Zwei simultane Broadcasts blockierten den Main-Thread (`b/236906724`).
- **Versionen:** vor 2.9.0; **gefixt 2.9.0**.
- **FIX:** Auf 2.9.0+ (kein App-Code nötig).
- **Quelle:** https://issuetracker.google.com/issues/236906724

### W13. `workdb`-Migration "duplicate column" / DB-Corruption bei Multi-Version
- **Symptom:** Start-Crash `SQLiteException: duplicate column name: run_in_foreground` / `SQLiteDatabaseCorruptException` auf `androidx.work.workdb`.
- **Ursache:** Zwei verschiedene `androidx.work`-Versionen im Dependency-Graph → inkonsistente Schema-Migration.
- **Versionen:** bei gemischten Versionen.
- **FIX:** Alle `work-*`-Artefakte auf EINE Version pinnen (BOM/`resolutionStrategy`); auf aktuelle 2.11.x.
- **Quelle:** https://github.com/android/codelab-android-workmanager/issues/292 (extern)

### W14. Worker bleibt dauerhaft `RUNNING` (Executor schon zu)
- **Symptom:** Nicht-kooperativer Worker hängt für immer in `RUNNING`; blockiert Unique-Ketten.
- **Ursache:** Worker ignoriert Stop/`isStopped`; der zweite WM-Shutdown nach 10 Min trifft einen bereits geschlossenen Executor.
- **Versionen:** mehrere; App-Kooperation bleibt nötig.
- **FIX:** `isStopped` konsequent prüfen / Cancellation respektieren (kein `runBlocking` ohne Cancellation); aktuelle WM-Version.
- **Quelle:** https://issuetracker.google.com/issues/155370056

### W15. `getInstance()` "not initialized" durch ContentProvider-Reihenfolge / Multiprocess
- **Symptom:** `IllegalStateException: WorkManager is not initialized properly` trotz Default-Initializer.
- **Ursache:** `InitializationProvider` läuft zu spät / `WorkManagerInitializer` ohne `Configuration.Provider` entfernt / in Multiprocess im falschen Prozess instanziiert.
- **Versionen:** konfigabhängig (alle 2.x).
- **FIX:** On-Demand korrekt — Initializer entfernen UND `Configuration.Provider` implementieren; in Multiprocess `RemoteWorkManager.getInstance()`.
- **Quelle:** https://issuetracker.google.com/issues/112665532

### W16. `flexInterval` ignoriert / erster Periodenlauf sofort (alt-API + Doppellauf)
- **Symptom:** < API 24 läuft Flex-Periodic sofort statt im Flex-Fenster; API 23 doppelt. Logcat "Flex duration … Ignoring".
- **Ursache:** Flex vor API 24 nicht unterstützt → Fallback erzeugt Sofort-/Doppellauf (`b/124274584`).
- **Versionen:** API < 24; Periodic-Reconciliation-Fix in 2.5.0.
- **FIX:** Worker idempotent (Zeitstempel-Check); `enqueueUniquePeriodicWork` mit eindeutigem Namen.
- **Quelle:** https://issuetracker.google.com/issues/124274584

### W17. `RemoteCoroutineWorker` unbindet `:remote`-Prozess nicht → Leak → gefixt 2.10.4
- **Symptom:** Der `:remote`-Prozess bleibt nach Worker-Ende gebunden, hängt im Speicher.
- **Ursache:** fehlendes `unbindService()` für den Remote-`WorkerService` (`b/247113322`).
- **Versionen:** bis 2.10.3; **gefixt 2.10.4**.
- **FIX:** Auf 2.10.4+ (nur Multiprocess-Apps mit `work-multiprocess`).
- **Quelle:** https://issuetracker.google.com/issues/247113322

### W18. KSP-Migration: `@HiltWorker` braucht ZWEI Compiler ⭐ HAEUFIG
- **Symptom:** Nach kapt→KSP `NoSuchMethodException` zur Laufzeit; `HiltWorkerFactory`-Map leer.
- **Ursache:** `com.google.dagger:hilt-compiler` UND `androidx.hilt:hilt-compiler` sind getrennte Prozessoren; nur letzterer generiert die Worker-Factory — bei KSP still fehlend, kein Build-Fehler.
- **Versionen:** work 2.10 / hilt 2.53 / KSP (by design, schlecht dokumentiert).
- **FIX:** Beide als `ksp(...)` in JEDEM Worker-Modul + `androidx.hilt:hilt-work`.
- **Quelle:** https://github.com/google/dagger/issues/4058 (extern)

### W19. `Configuration.Provider`: Property (2.9+) statt Methode (≤ 2.8)
- **Symptom:** Nach Update auf 2.9/2.10 wird die eigene Config (HiltWorkerFactory) ignoriert; `NoSuchMethodException`.
- **Ursache:** API-Signaturwechsel — ab 2.9.0 `val workManagerConfiguration` statt `getWorkManagerConfiguration()`; ein stehengelassener Methoden-Override greift nicht.
- **Versionen:** Bruch 2.8 → 2.9.
- **FIX:** `override val workManagerConfiguration: Configuration` nutzen; NICHT auf 2.8 downgraden (verliert Bugfixes).
- **Quelle:** https://github.com/google/dagger/issues/4058 (extern)

### W20. `InitializationProvider` falsch entfernt → Reboot-Reschedule kaputt
- **Symptom:** Eigene Config läuft, aber Worker laufen nach Reboot nicht an (kein Crash).
- **Ursache:** Der ganze `androidx.startup.InitializationProvider` entfernt statt nur des `WorkManagerInitializer`-`meta-data` → der `RescheduleReceiver` wird nicht aktiviert.
- **Versionen:** WM 2.6+.
- **FIX:** Provider mit `tools:node="merge"` behalten, nur das eine `meta-data` `tools:node="remove"`; App implementiert `Configuration.Provider`. Nach dem Umbau echten Reboot testen, nicht nur App-Restart.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration

### W21. Doppelte Initialisierung → `IllegalStateException: already initialized`
- **Symptom:** Start-Crash "WorkManager is already initialized. Did you try to initialize it manually…".
- **Ursache:** Auto-`WorkManagerInitializer` (Provider noch da) UND manuelles `WorkManager.initialize()` gleichzeitig.
- **Versionen:** alle.
- **FIX:** Genau EIN Pfad — bevorzugt `Configuration.Provider` + Default-Provider entfernen (kein manuelles `initialize()`).
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration

### W22. Testing: Constraint-/Delay-Methoden VOR `enqueue()` → Crash
- **Symptom:** `TestDriver`-Call wirft `IllegalArgumentException`/NPE, Test bricht ab.
- **Ursache:** `setAllConstraintsMet`/`setInitialDelayMet`/`setPeriodDelayMet` brauchen die bereits enqueuete WorkSpec-ID.
- **Versionen:** alle mit `WorkManagerTestInitHelper`.
- **FIX:** Reihenfolge — erst `enqueue(...).result.get()`, dann TestDriver; Init mit `SynchronousExecutor()` + `HiltWorkerFactory`.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing

### W23. Testing: Periodic-Worker nur mit TestDriver testbar, nicht `TestListenableWorkerBuilder`
- **Symptom:** Periodic landet wieder in ENQUEUED / Test hängt; `TestListenableWorkerBuilder` umgeht Periodic/Constraints/Factory.
- **Ursache:** Zwei verschiedene Test-APIs verwechselt — `TestListenableWorkerBuilder` ruft nur `doWork()` direkt (kein Scheduling, keine injizierte Factory).
- **Versionen:** seit 2.1.0.
- **FIX:** Periodic/Constraints/Hilt → `WorkManagerTestInitHelper` + `getTestDriver()` + `SynchronousExecutor`; reine doWork-Logik → `TestListenableWorkerBuilder`. Nie vermengen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing

### W24. Compose: doppelte WorkInfo-Emissions / Recomposition-Sturm
- **Symptom:** `getWorkInfos…Flow().collectAsState()` recomposed exzessiv; `LaunchedEffect` feuert doppelt; Progress flackert.
- **Ursache:** Der Flow emittiert bei jedem WorkSpec-Update (auch UI-identisch); ohne Dedup löst jede Emission Recomposition aus.
- **Versionen:** Flow-Varianten seit 2.9.0.
- **FIX:** `.map { it.firstOrNull()?.state }.distinctUntilChanged().collectAsStateWithLifecycle(...)`; Effekte an einen stabilen Key binden.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/work

### W25. R8/Minify: Worker-Klasse obfuskiert → `ClassNotFoundException`
- **Symptom:** Release-Build crasht beim Worker-Start (Debug ok).
- **Ursache:** Die Consumer-Regeln keepen nur `androidx.work.**`, nicht die eigenen Worker; der persistierte FQCN passt nach Obfuskierung nicht mehr.
- **Versionen:** dauerhaft (R8 Default-Shrinker).
- **FIX:** `-keep class * extends androidx.work.ListenableWorker { <init>(...); }`; den Worker-Konstruktor `(Context, WorkerParameters)` explizit halten.
- **Quelle:** https://www.codestudy.net/blog/android-work-manager-could-not-instantiate-worker/ (extern)

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

### F7. `ForegroundServiceStartNotAllowedException` — Background-Start nach User-Interaktion ⭐ HAEUFIG
- **Symptom:** Crash `ForegroundServiceStartNotAllowedException` / "Service.startForeground() not allowed due to mAllowStartForeground false" (Crashlytics-Wellen Pixel/Samsung S25).
- **Ursache:** Ab Android 12 kein FGS-Start aus dem Hintergrund ohne Exemption; Race "Button → App in den Hintergrund"; die FCM-High-Prio-Exemption hat nur ein kurzes Fenster.
- **Versionen:** Android 12+ (verschärft).
- **FIX:** `startForegroundService()` synchron im selben User-Interaction-Stack (Vordergrund); bei FCM den Service zuerst starten; legitime Background-Starts nur mit gültiger Exemption (z.B. Exact-Alarm, F10).
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/troubleshooting

### F8. `ForegroundServiceDidNotStartInTimeException` (5 s) + WorkManager-Doppel-Worker-Race → gefixt 2.10.5
- **Symptom:** Crash "Context.startForegroundService() did not then call Service.startForeground()"; bei WM begleitet vom Log-Marker "Re-initializing SystemForegroundService after a request to shut-down".
- **Ursache:** Nach `startForegroundService()` bleiben de facto ~5 s bis `startForeground()`; bei WM ein Race zwischen zwei Foreground-Workern (B startet FGS, während A herunterfährt).
- **Versionen:** allgemein Android 8+; WM-Race **gefixt 2.10.5**.
- **FIX:** `startForeground()` als erste Zeile, Main-Thread nie blockieren; WM auf 2.10.5+; Foreground-Worker serialisieren (`KEEP`/`APPEND`).
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/troubleshooting

### F9. `InvalidForegroundServiceTypeException` — Typ Code ≠ Manifest
- **Symptom:** `IllegalArgumentException: foregroundServiceType 0x… is not a subset of … in manifest file`.
- **Ursache:** Ab Android 14 muss der Laufzeit-Typ Teilmenge des Manifest-`foregroundServiceType` sein; bei WM oft `getForegroundInfo()`-Typ ≠ Manifest (`0x0`).
- **Versionen:** Android 14+.
- **FIX:** Typ im Manifest deklarieren UND identisch an `ForegroundInfo`/`startForeground()` reichen; mehrere Typen per `|`; passende `FOREGROUND_SERVICE_*`-Permission. (Erweitert F6.)
- **Quelle:** https://developer.android.com/about/versions/14/changes/fgs-types-required

### F10. FGS aus Exact-Alarm-Receiver / `SYSTEM_EXEMPTED`-Falle (Android 14)
- **Symptom:** FGS-Start aus Alarm-Receiver crasht trotz "Exact-Alarm" — ODER `SecurityException` bei `SYSTEM_EXEMPTED` ohne Permission.
- **Ursache:** Exact Alarms SIND BAL-exempt (FGS-Start aus `onReceive` erlaubt) — aber unter Android 14 ist `SCHEDULE_EXACT_ALARM` denied-by-default; ohne Permission entfällt die Exemption. `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` setzt Exact-Alarm-Permission o.ä. voraus.
- **Versionen:** Android 14+.
- **FIX:** `canScheduleExactAlarms()` prüfen, sonst inexakt; FGS aus dem Receiver synchron starten; `SYSTEM_EXEMPTED` nur bei erfüllter Voraussetzung.
- **Quelle:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

### F11. 6h-FGS-Budget ist app-weit pro Typ kumuliert
- **Symptom:** Ein zweiter `dataSync`-FGS crasht "zu früh" mit "Time limit already exhausted for foreground service type dataSync".
- **Ursache:** Das 6h-Limit gilt pro Typ app-weit kumuliert, nicht pro Instanz; Service B erbt nur das Restbudget von A. (`dataSync`/`mediaProcessing` haben getrennte Töpfe.)
- **Versionen:** Android 15+.
- **FIX:** Budget app-weit denken; mehrere kurzlebige FGS konsolidieren; auf UIDT/WorkManager-Constraints ausweichen; Reset nur durch echte Vordergrund-Interaktion.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15#datasync-timeout

### F12. `shortService`: 3-Min-ANR, nicht-sticky, kann keine weiteren FGS starten
- **Symptom:** `RemoteServiceException: … FOREGROUND_SERVICE_TYPE_SHORT_SERVICE did not stop within its timeout` → ANR.
- **Ursache:** `shortService` (Android 14) darf nur ~3 Min; kein Sticky-Restart; darf keine weiteren FGS starten — wird oft fälschlich für lange Arbeit genutzt (braucht keine Permission).
- **Versionen:** Android 14+.
- **FIX:** Nur < 3-Min-Tasks; `onTimeout()` → `stopSelf()`; lange Arbeit korrekt typisieren (`dataSync`/UIDT); keine Ketten-FGS.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/service-types

### F13. While-In-Use-`SecurityException` (FGS-Typ-Permission nur im Vordergrund)
- **Symptom:** `SecurityException` direkt nach `startForeground()`, nur bei Background-Start, obwohl die Permission gewährt ist.
- **Ursache:** `camera`/`microphone`/`location`/`connectedDevice` brauchen while-in-use-Permissions; im Hintergrund sind sie faktisch inaktiv → `SecurityException` (anderer Fehler als die normale BG-Start-Restriktion).
- **Versionen:** Android 14+.
- **FIX:** Solche FGS nur im Vordergrund starten; vor `startForeground()` `checkSelfPermission` prüfen; `connectedDevice` braucht zusätzlich eine Sub-Permission.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/troubleshooting

### F14. Android 16: 6h-FGS-Timeout greift unabhängig vom targetSdk
- **Symptom:** Apps mit niedrigem targetSdk zeigen auf Android-16-Geräten plötzlich FGS-Timeout-Crashes/ANRs ("ohne Codeänderung").
- **Ursache:** Android 16 wendet bestimmte FGS-Änderungen auf ALLE Apps an, die auf 16 laufen — unabhängig vom targetSdk.
- **Versionen:** Android 16.
- **FIX:** `onTimeout()` auch bei niedrigem targetSdk implementieren; FGS-getriggerte Background-Jobs auf WorkManager-Constraints umstellen.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-all

### F15. Test-Tooling — 6h-Timeout deterministisch provozieren
- **Symptom:** 6h-Timeout-Crashes treten erst nach Stunden in Produktion auf, im Test nie reproduzierbar.
- **Ursache:** Das Limit greift erst nach 6h kumuliert im Hintergrund — manuell nicht abwartbar.
- **Versionen:** Android 15/16 Test-Tooling.
- **FIX:** `adb shell am compat enable FGS_INTRODUCE_TIME_LIMITS <pkg>` + `adb shell device_config put activity_manager data_sync_fgs_timeout_duration <ms>` → Crash sofort reproduzierbar; in CI als Regressionstest verdrahten.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

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

### O7. Xiaomi/MIUI 14: separate Per-App "Background autostart"-Permission
- **Symptom:** App ist in der alten Autostart-Liste freigegeben, startet trotzdem nicht aus dem Hintergrund.
- **Ursache:** MIUI 14 hat einen zweiten, neueren Autostart-Schalter pro App (unabhängig von der Security-App-Liste).
- **Versionen:** MIUI 14 / HyperOS.
- **FIX:** Nutzer zu `Settings > Apps > [App] > App permissions > Background autostart` UND `Security > Permissions > Auto-start` (beide nötig).
- **Quelle:** https://dontkillmyapp.com/xiaomi (extern)

### O8. Xiaomi-Autostart-Status programmatisch prüfbar
- **Symptom:** App weiß nicht, ob Autostart aus ist → kann nicht gezielt warnen.
- **Ursache:** AOSP kennt die Permission nicht (kein Standard-API).
- **Versionen:** MIUI 10–14.
- **FIX:** Lib `XomaDev/MIUI-autostart` (`getAutoStartState()`); nur bei `DISABLED` in die Settings führen (kein unnötiger Dialog).
- **Quelle:** https://github.com/XomaDev/MIUI-autostart (extern)

### O9. MIUI Optimization-Toggle (Developer Options) bricht Background-Tasks
- **Symptom:** Trotz korrekter Battery/Autostart-Settings brechen Hintergrund-Tasks ab.
- **Ursache:** Verstecktes "MIUI optimization" (default an, in den Entwickleroptionen) ändert das Process-Management.
- **Versionen:** MIUI 12+.
- **FIX:** Developer Mode → "MIUI optimization" ausschalten.
- **Quelle:** https://dontkillmyapp.com/xiaomi (extern)

### O10. Samsung One UI 7 (Android 15): Alarm-/Lockscreen-Alarm-Regression
- **Symptom:** Nach One-UI-7-Update verstellen sich Alarme / der Lockscreen zeigt keine Alarme mehr.
- **Ursache:** One-UI-7-Regression im Zusammenspiel mit Sleeping-Apps/Adaptive-Battery.
- **Versionen:** Galaxy S21+/S23, One UI 7.
- **FIX:** "Never sleeping apps" + "Put unused apps to sleep" aus + Battery "Don't optimize"; reine Wecker über AOSP-Clock (`setAlarmClock` hat die höchste Priorität).
- **Quelle:** https://us.community.samsung.com (extern)

### O11. Samsung "Good Guardians"/Good Lock-Layer (Android 14)
- **Symptom:** Der System-Toggle allein reicht nicht; die App wird trotzdem aus dem RAM geworfen.
- **Ursache:** Background-Retention in separate Module ausgelagert (Memory/Battery Guardian, Good Lock "The long live app").
- **Versionen:** One UI 6+.
- **FIX:** Memory Guardian "Keep more apps", Battery Guardian "App power saving" aus, Good Lock "long live app" an, App in Recents locken.
- **Quelle:** https://dontkillmyapp.com/samsung (extern)

### O12. Huawei/Honor: Wakelock-Tag-Whitelist-Trick gegen PowerGenie
- **Symptom:** App mit Wakelock > 60 Min gekillt ("force stop abnormal wakelock app").
- **Ursache:** `HwPFWService` killt nicht-whitelisted Wakelock-Tags.
- **Versionen:** EMUI 4+ / 9+.
- **FIX:** Auf Huawei/Honor den Wakelock-Tag auf einen whitelisted Tag mappen (`LocationManagerService`, `AudioMix` …) via `Build.MANUFACTURER`-Branch.
- **Quelle:** https://dontkillmyapp.com/huawei (extern)

### O13. PowerGenie nur per ADB entfernbar (keine User-Whitelist)
- **Symptom:** Egal welche Settings, eine nicht-whitelisted App wird gekillt.
- **Ursache:** PowerGenie ist eine System-App mit fester Whitelist.
- **Versionen:** EMUI 9+.
- **FIX (Power-User):** `adb shell pm uninstall -k --user 0 com.huawei.powergenie`; vorher "App launch → Manage manually".
- **Quelle:** https://dontkillmyapp.com/huawei (extern)

### O14. Oppo/ColorOS killt bei Screen-Off, wenn nicht alle vier Hürden genommen sind
- **Symptom:** Der Service stirbt bei jedem Bildschirm-Aus.
- **Ursache:** Aggressive ColorOS-Background-Verwaltung (`com.coloros.safecenter`).
- **Versionen:** ColorOS 5/6.
- **FIX:** App in Recents pinnen + "Startup manager"/"floating app list" freigeben + Battery-Optimization aus + persistente FGS-Notification + "Allow Auto Start-up".
- **Quelle:** https://dontkillmyapp.com/oppo (extern)

### O15. Oppo/Vivo "Sleep standby optimization" / "AI sleep mode" friert nachts ein
- **Symptom:** App läuft tagsüber, versagt aber nachts.
- **Ursache:** Der OEM friert Apps in erkannten Schlafphasen ein.
- **Versionen:** ColorOS/Funtouch OS, Android 13+.
- **FIX:** Feature pro App deaktivieren; Vivo "High background power consumption" an + Battery "Not optimized"; App in Recents locken.
- **Quelle:** dev.to/stoyan_minchev (extern)

### O16. FCM High-Priority: nur sichtbare Notification-Messages werden proxied ⭐ HAEUFIG
- **Symptom:** Trotz `priority:high` kommen Pushes verspätet/gar nicht — besonders data-only.
- **Ursache:** Google Play Services proxied nur High-Prio-Messages, die eine sichtbare Notification erzeugen; data-only/keine-Notification wird gedrosselt; OEM-Battery-Saver ignorieren "high" teils.
- **Versionen:** Android-weit (Doze) + OEM.
- **FIX:** `priority:high` + Channel `IMPORTANCE_HIGH` + JEDE High-Prio-Message erzeugt eine sichtbare Notification; schwere Arbeit in einen Expedited-WorkManager-Job; zusätzlich Nutzer-Whitelisting.
- **Quelle:** https://firebase.blog/posts/2025/04/fcm-on-android/ (offiziell)

### O17. `Build.MANUFACTURER` + dontkillmyapp-JSON-API + Deep-Link-Libs
- **Symptom:** Der pauschale Battery-Dialog passt nicht zum OEM; OEM-Settings-Intents werfen `ActivityNotFoundException`.
- **Ursache:** Jeder OEM hat andere Pfade/Activities; manche existieren nicht auf jedem Build.
- **Versionen:** geräteabhängig.
- **FIX:** JSON-API `dontkillmyapp.com/api/v2/{manufacturer}.json` → `user_solution` anzeigen; Libs `judemanutd/AutoStarter`, `DoubleDotLabs/doki`; OEM-Intents IMMER in try/catch mit Fallback `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.
- **Quelle:** https://dontkillmyapp.com/apidoc (extern)

### O18. SyncAdapter + batched Accelerometer als zusätzliche Recovery-Kanäle
- **Symptom:** AlarmManager suppressed, WorkManager deferred, FGS gekillt — punktuell hilft nichts davon.
- **Ursache:** OEMs filtern gezielt die schedulebaren Standard-APIs.
- **Versionen:** geräteabhängig.
- **FIX (additiv, optional):** `ContentResolver.addPeriodicSync()` (~1 h; Sync-Prozesse werden ungern gekillt); batched Accelerometer (`maxReportLatencyUs`, Hardware-FIFO, für Battery-Manager unsichtbar) — erste Readings stale, `flush()`/`onFlushCompleted()` abwarten.
- **Quelle:** dev.to/stoyan_minchev (extern)

---

## ✅ Fix-Status (was ist in neueren Versionen schon behoben?)

> Belege aus offiziellen androidx-WorkManager-Release-Notes (Stand 2.11.2) + Google IssueTracker.
> Ehrlichkeit: streng getrennt nach *belegt gefixt* vs. *Workaround bleibt aktiv*.

**Belegt gefixt (Versions-Anker):**

| Früherer Bug | Gefixt ab | Beleg |
|--------------|-----------|-------|
| W10 PeriodicWork stirbt still nach Exception | WorkManager **2.11.2** | issuetracker 443879071 |
| W11 Netzwerk-Constraint-Cluster | **2.11.1** (Teilfixes 2.10.1–2.11.1) | issuetracker 427115602 / 465016918 |
| W12 RescheduleReceiver-ANR (2 Broadcasts) | **2.9.0** | issuetracker 236906724 |
| W17 `RemoteCoroutineWorker`-Leak (`:remote`) | **2.10.4** | issuetracker 247113322 |
| F8 FGS-Overlap "Re-initializing SystemForegroundService" | **2.10.5** | issuetracker 432069314 |
| F1 `dataSync`/`shortService`-ANR API 34/35 (WM) | **2.10.0** | issuetracker 364508145 |
| W (Long-Running-FGS-Restart nach FGS-Permission-Entzug, A14) | **2.9.1 / 2.10.0** | issuetracker 333957914 |
| W16 flexInterval/Periodic-Reconciliation + Greedy-Scheduler | **2.5.0 / 2.8.0** | issuetracker 124274584 / 248111307 |

**Noch NICHT gefixt — Workaround bleibt aktiv:**

- **Plattform-Vertrag / by design** (kein Code-Bug): A11 (`setAlarmClock`-Lockscreen-Icon), A12 (Listener-Pfad), A13 (10-Min-Fenster-Clamp), N1/N11/N17 (Channel-Settings unveränderlich), B9–B12 (Direct-Boot-Constraints), F11 (6h-Budget app-weit), F14 (Android-16-Geltung), W18/W19 (Hilt-Compiler-Set / Property-Signatur).
- **OS-Verhalten ohne Google-Fix:** A10 (`cancel()`-Slot-Leak, OS-Buchhaltung), O1–O18 (alle OEM-Killings sind ROM-Verhalten — kein API-Fix, nur Nutzer-Anleitung + Defense-in-Depth).
- **Offen im Issue-Tracker:** B11 (WorkManager-Direct-Boot-Crash, jellyfin #3143).
- **Dependency-Disziplin statt Versions-Fix:** W13 (`workdb` Multi-Version → eine Version pinnen).

> Methodik-Hinweis: Fix-Versionen aus den androidx-Release-Notes verifiziert. Wo nur ein Snippet
> vorlag, bleibt der Status bewusst "offen/unklar" statt "gefixt".

---

## 📋 Pflicht-Checkliste (vor Reminder-/Backup-Arbeit abhaken)

- [ ] Reminder per **One-shot + Reschedule** (`java.time`), kein `setRepeating` (A1)
- [ ] Inexakt (`setAndAllowWhileIdle`), kein `USE_EXACT_ALARM` → sonst Play-Reject (A2/A3)
- [ ] Genau **1 aktiver Alarm** (Self-Reschedule-Kette); `cancel()` mit IDENTISCHEM PendingIntent (A10)
- [ ] Eine zentrale `rescheduleAll()` — aufgerufen aus `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` + `…PERMISSION_STATE_CHANGED` + jedem App-Start (B1/B2/B3/A14)
- [ ] Action-Konstante `Intent.ACTION_BOOT_COMPLETED`; Receiver `exported="true"`; `RECEIVE_BOOT_COMPLETED` (B5/B16)
- [ ] Reboot vor Unlock bedacht: `LOCKED_BOOT_COMPLETED` + `directBootAware` nur wenn nötig, kein Room/FGS im LOCKED-Fenster (B9–B12)
- [ ] Jeder PendingIntent mit `FLAG_IMMUTABLE` (N4)
- [ ] Eindeutige Notification-ID/Tag pro Reminder (N10); Channel-Settings final, neue ID bei Änderung (N1)
- [ ] `POST_NOTIFICATIONS` runtime + `areNotificationsEnabled()` prüfen (N5/N9)
- [ ] Sound/Vibration **am Channel**, nicht am Builder (N11/N17)
- [ ] Backups in WorkManager (`enqueueUniquePeriodicWork(KEEP)`), nicht als `dataSync`-FGS (W3/F2)
- [ ] Falls WorkManager eingeführt: **≥ 2.11.2**, alle `work-*` auf EINE Version gepinnt (W10/W11/W13)
- [ ] FGS-Typ Manifest = `ForegroundInfo` (F6/F9); `onTimeout()` → `stopSelf()` (F1/F11/F14)
- [ ] Hilt: beide Compiler als `ksp`, `Configuration.Provider` als `val`, nur das `meta-data` entfernen (W18/W19/W20)
- [ ] R8: keep-Regel für Worker-Konstruktor; Worker-Klassen nie umbenennen (W25/W9)
- [ ] OEM: Battery-Exemption-Prompt + `Build.MANUFACTURER` → dontkillmyapp-Anleitung; Watchdog (O1/O17)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-workmanager-notifications.md`) |
|---------------|------------------------------------------------------------------------|
| A1–A14 (AlarmManager/Reminder) | §2 (Reminder-Pattern), §3 (Exact/Policy), §4 (PendingIntent) |
| N1–N21 (Notifications) | §5 (Channels & POST_NOTIFICATIONS), §4 (PendingIntent) |
| B1–B17 (Boot/Reschedule, inkl. Direct Boot) | §6 (Reschedule nach Reboot/Update) |
| W1–W25 (WorkManager, inkl. Hilt/Testing/Compose) | §7 (WorkManager für Backups), §1 (Architektur) |
| F1–F15 (FGS/Backup) | §8 (FGS-Typen & UIDT) |
| O1–O18 (OEM-Killings) | §9 (OEM-Killings & Workarounds) |
