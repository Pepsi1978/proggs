# Bekannte Bugs: Android-Framework / Platform-SDK

> **PFLICHT-LESEN vor Arbeit am Android-Framework/Runtime** (Lifecycle, Permissions,
> Services, WorkManager, Room-Runtime/Migrations, PendingIntent/AlarmManager,
> Notifications, BroadcastReceiver, Scoped Storage, Doze/Background, targetSdk-Verhalten).
>
> **Stand:** zuletzt recherchiert am **2026-06-02**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> fuer **targetSdk = compileSdk = 36 (Android 16 „Baklava")**, minSdk 26 (BestJournalAndroid) bzw. 28 (EntropieReductor),
> Java 17, **Room 2.7.0**, **WorkManager 2.10.0**, hilt-work 1.2.0, Health Connect,
> Glance/RemoteViews-Widget, AGP 8.7.3 / 8.10.0. Beide Apps zielen auf API 36 — die
> Verhaltensaenderungen der Sektion 8 greifen also voll.
> **Neu 2026-07-02:** **Android 17 (API 37)** existiert bereits (compileSdk 37 wird in Projekten benutzt) →
> neuer §8.14 (Ausblick API 37); §4.9 um `STOP_REASON_TIMEOUT_ABANDONED`/`getPendingJobReasonsHistory` ergaenzt.
>
> Recherche: 7-Researcher-Schwarm (offizielle Quellen zuerst), Fix-Status teils per
> `gh` hart geprueft. Quelle des Systems: `~/proggs/bugs/SYSTEM.md`.
>
> **Ergaenzt 2026-07-02:** §1.12 (ProcessLifecycleOwner-Observer-Leak, CortexAndroid-Fund).
>
> **Ergaenzung 2026-06-02 (Best-Practices-Lauf):** Die Best-Practices-Seite
> [`best-practices/android/android-platform.md`](../../best-practices/android/android-platform.md)
> ist die „so macht man es richtig"-Seite zu diesem Almanach. Dabei kamen zusaetzliche Bug-Funde
> dazu: 4.14, 4.15 (WorkManager/Hilt) und 8.10–8.13 (Android-15/16-Plattform-Crashes). Die
> wechselseitige Bezugs-Tabelle steht direkt unter dem TL;DR.

---

## Abgrenzung — was hier steht und was woanders (KRITISCH)

Dieser Almanach deckt **Framework/Runtime-Verhalten** ab. Sprach-, UI- und Build-Bugs
liegen in eigenen Almanachen — NICHT verwechseln:

| Thema | Hier (`android-platform.md`) | Woanders |
|-------|------------------------------|----------|
| Activity/Fragment/Service-Lifecycle, Process Death, Context-Leaks | ✅ | — |
| Runtime-Permissions, Manifest-Permissions, `<queries>` | ✅ | — |
| Foreground Services, ANRs, WorkManager, Doze, AlarmManager | ✅ | — |
| Room **Runtime** (Migrations, WAL, CursorWindow, Threading, Locking) | ✅ | Room-**Build**-Integration (KSP-Plugin) → `gradle.md` §8 |
| PendingIntent, Notifications, BroadcastReceiver, Scoped Storage | ✅ | — |
| Plattform-Verhaltensaenderungen durch targetSdk-Bump (15/16) | ✅ | — |
| Kotlin-Sprache, K2, Coroutines/Flow-Mechanik, `data class` | → `kotlin.md` | Coroutine-Lifecycle-Sammlung steht dort UND hier (#1.4) |
| Compose-UI: Recomposition, `remember`, Side-Effects, Lazy, Modifier, Navigation, Material3 | → `jetpack-compose.md` | — |
| Gradle/AGP, R8/ProGuard (Keep-Regeln fuer Room/Hilt), KSP-Build, Config-Cache, Version-Catalog | → `gradle.md` | R8-Keep-Regeln fuer Room/Hilt/Moshi liegen dort |

**Querverweis-Faustregel:** Geht es um *Verhalten zur Laufzeit auf dem Geraet* → hier.
Geht es um *Kompilieren/Bauen* → `gradle.md`. Geht es um *was auf dem Screen passiert* →
`jetpack-compose.md`. Geht es um *Sprach-Syntax/Coroutine-Semantik* → `kotlin.md`.

---

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
| 16 | `ProcessLifecycleOwner.addObserver` in der Activity | Observer als Feld + in `onDestroy` `removeObserver` — sonst Leak + Mehrfach-Callbacks nach Recreation | §1.12 |
| 17 | compileSdk/targetSdk **37** (Android 17) | Reflection/JNI auf `static final` crasht; `MessageQueue`-Interna nicht reflektieren; Local-Network-Permission wird Pflicht | §8.14 |

---

## 🔗 Bezug zur Best-Practices-Seite (so vermeidet man diese Bugs von vornherein)

Zwei Seiten derselben Medaille: dieser Almanach sagt *was schiefgeht und wie man es umgeht*,
[`best-practices/android/android-platform.md`](../../best-practices/android/android-platform.md)
sagt *wie man es von vornherein richtig macht*. Jede Bug-Sektion hier hat dort ihre Praevention.

| Bug-Sektion (hier) | Praevention in `best-practices/android/android-platform.md` |
|--------------------|------------------------------------------------------------------------|
| §1 Lifecycle/Process-Death/Leaks (1.1–1.11) | §2 Lifecycle-sicheres Arbeiten (Grundlage: §1 Architektur) |
| §2 Runtime-Permissions (2.1–2.10) | §6 Moderner Permission-Flow |
| §3 Foreground Services & ANRs (3.1–3.9) | §5 FGS & Notifications (+ §4 WorkManager als Alternative) |
| §4 WorkManager/Doze/App-Standby (4.1–4.15) | §4 Korrekter WorkManager-Einsatz |
| §5 Room Runtime/Migrations/WAL (5.1–5.15) | §3 Saubere Room-Migrationsstrategie & Runtime-Disziplin |
| §6 PendingIntent/AlarmManager/Notifications/Receiver (6.1–6.9) | §6 PendingIntent/AlarmManager + §5 Notifications |
| §7 Scoped Storage (7.1–7.3) | §7 Scoped Storage (Entscheidungsbaum + FileProvider) |
| §8 Android 15/16 targetSdk-Verhalten (8.1–8.13) | §7 targetSdk-36-Compliance (Edge-to-Edge, Predictive Back, 16-KB, Large-Screen) |

---

## 1. Lifecycle, Activity/Fragment, Process Death & Memory-Leaks

### 1.1 FragmentManager-`commit()` nach `onSaveInstanceState` → IllegalStateException
- **Symptom:** `java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState`.
- **Ursache:** `commit()` einer FragmentTransaction in einem async Callback (Netzwerk/Coroutine), nachdem `onSaveInstanceState()` lief → State-Loss.
- **Versionen:** per Design (alle Versionen).
- **FIX:** Transaktionen nicht in async Callbacks committen; Lifecycle-aware sammeln (`repeatOnLifecycle(STARTED)`) oder vor `commit()` `!isStateSaved` pruefen. `commitNow()` wo synchron moeglich. `commitAllowingStateLoss()` nur als letzter Ausweg (versteckt das Problem). Nie Feature weglassen.
- **Quelle:** https://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html

### 1.2 `registerForActivityResult` zu spaet → IllegalStateException
- **Symptom:** `LifecycleOwner ... is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.`
- **Ursache:** `registerForActivityResult()` in Click-Listener/`onResume`/nach `onCreateView` aufgerufen — zu spaet im Lifecycle.
- **Versionen:** per Design seit AndroidX Activity 1.2 / Fragment 1.3.
- **FIX:** `registerForActivityResult()` IMMER als Feld/Property bzw. in `onCreate` (Activity) / vor `onCreateView` (Fragment) registrieren. Nur `launcher.launch()` darf spaeter.
- **Quelle:** https://developer.android.com/training/basics/intents/result

### 1.3 `TransactionTooLargeException` bei `onSaveInstanceState` (Bundle > ~1 MB)
- **Symptom:** `android.os.TransactionTooLargeException: data parcel size NNN bytes` bei Rotation/Backgrounding (als Crash ab Android 7).
- **Ursache:** Das Bundle geht via Binder ins system_process; der Binder-Buffer ist prozessweit auf ~1 MB begrenzt. Grosse Listen/komplexe Objekte/viele verschachtelte Fragmente sprengen das.
- **Versionen:** Crash-Verhalten ab Android 7 (API 24); Limit per Design.
- **FIX:** Nur IDs/Schluessel ins SavedInstanceState. Grosse Daten in ViewModel (Config-Change) bzw. Room/DataStore (Process Death) halten, ueber ID nachladen. Diagnose: TooLargeTool.
- **Quelle:** https://developer.android.com/reference/android/os/TransactionTooLargeException

### 1.4 Flow-Collection ohne Lifecycle-Awareness leakt / verschwendet Ressourcen
- **Symptom:** Collection laeuft im Hintergrund weiter; Updates auf nicht sichtbarer/zerstoerter View; unnoetige Arbeit; gelegentliche Crashes.
- **Ursache:** `lifecycleScope.launch { flow.collect {…} }` ohne `repeatOnLifecycle`. Die alten `launchWhenStarted/Resumed` SUSPENDIEREN nur (Producer bleibt aktiv) statt zu canceln — deprecated.
- **Versionen:** `repeatOnLifecycle` ab lifecycle-runtime-ktx 2.4.0; `launchWhenX` deprecated ab 2.7.
- **FIX:** `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect {…} } }`. In Fragments `viewLifecycleOwner` nutzen (View-Lifecycle ≠ Fragment-Lifecycle). (Coroutine-Mechanik auch in `kotlin.md`.)
- **Quelle:** https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda

### 1.5 ViewModel ueberlebt Config-Change, aber NICHT Process Death
- **Symptom:** Nach Rotation alles da; nach Hintergrund-Kill (Low-Memory) ViewModel-State weg.
- **Ursache:** ViewModel lebt im `ViewModelStore` (ueberlebt Recreation), aber nicht den Prozesstod. Trugschluss „ViewModel = persistent".
- **Versionen:** per Design.
- **FIX:** Process-Death-resistenten State via `SavedStateHandle` (kleine Daten/IDs) oder Persistenz (Room/DataStore). Testen mit „Don't keep activities" / `adb shell am kill <pkg>`.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/viewmodel

### 1.6 `SavedStateHandle` liefert `null` nach Process Death
- **Symptom:** `savedStateHandle.get<T>(key)` ist nach Process Death `null` statt des gesetzten Werts.
- **Ursache:** Speichert nur Werte, die zwischen onStart/onStop geschrieben wurden; inkonsistente Keys; zu grosse/nicht-parcelbare Objekte scheitern still; falsches ViewModel-Scoping (mehrere Instanzen).
- **Versionen:** per Design / dokumentierte Einschraenkung.
- **FIX:** Konstante Keys; nur kleine, parcelbare Werte; korrektes ViewModelStoreOwner-Scoping (`navGraphViewModels`/Activity-Scope); `getStateFlow(key, default)` fuer beobachtbaren State.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate

### 1.7 Handler-Leak durch nicht-statische Inner/Anonymous-Klasse
- **Symptom:** Activity wird nach `finish()` nicht GC'd (LeakCanary); verzoegerte Messages feuern auf toter Activity.
- **Ursache:** Nicht-statischer Handler haelt implizite Referenz auf die aeussere Activity; gepostete Messages bleiben in der MessageQueue.
- **Versionen:** per Design.
- **FIX:** Statische Handler-Klasse mit `WeakReference<Activity>` ODER in `onDestroy()` `handler.removeCallbacksAndMessages(null)`.
- **Quelle:** https://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html

### 1.8 Nicht abgemeldeter BroadcastReceiver / Listener / Sensor leakt Context
- **Symptom:** Context/Activity-Leak; `IllegalArgumentException: Receiver not registered` (bei Doppel-unregister); Callbacks nach onDestroy.
- **Ursache:** Dynamisch via `registerReceiver()` / Sensor-/Location-Listener registrierte Objekte halten den Context bis zum `unregister`.
- **Versionen:** per Design. Zusatz ab Android 14: dynamische Receiver brauchen `RECEIVER_EXPORTED/NOT_EXPORTED` (siehe 6.7).
- **FIX:** Jedes `registerX` braucht spiegelbildliches `unregisterX` im passenden Lifecycle-Callback (onStart/onStop bzw. onResume/onPause). In Compose: `DisposableEffect { onDispose { … } }`. Doppel-unregister mit Guard absichern (nicht weglassen).
- **Quelle:** https://www.symphony-solutions.eu/fixing-memory-leaks-in-android/

### 1.9 „View not attached to window manager" bei Dialog nach Destroy
- **Symptom:** `WindowManager$BadTokenException: ... View not attached to window manager` beim Dialog `show()`/`dismiss()` aus async Callback.
- **Ursache:** Async-Arbeit zeigt/schliesst Dialog mit Activity-Token, nachdem die Activity zerstoert wurde.
- **Versionen:** per Design (`isDestroyed()` ab API 17).
- **FIX:** Vor UI-Zugriff `if (isFinishing || isDestroyed) return`. Besser `DialogFragment` (lifecycle-managed) statt nacktem `Dialog`; async-Jobs in onDestroy canceln.
- **Quelle:** https://www.symphony-solutions.eu/fixing-memory-leaks-in-android/

### 1.10 `getIntent()` liefert altes Intent bei singleTop/singleTask + `onNewIntent`
- **Symptom:** Bei Re-Launch verarbeitet die App noch das urspruengliche Intent (falsche Deep-Link-/Notification-Daten).
- **Ursache:** `onNewIntent(intent)` bekommt das neue Intent, aber `getIntent()` gibt weiter das ALTE zurueck, bis `setIntent()` aufgerufen wird.
- **Versionen:** per Design (Android 15+ dispatched strikter nach Lifecycle).
- **FIX:** In `onNewIntent` `super.onNewIntent(intent)` + `setIntent(intent)` aufrufen; danach liefert `getIntent()` das neue. Alternativ den Parameter direkt nutzen.
- **Quelle:** https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)

### 1.11 `onConfigurationChanged` feuert auf Android 11–13 nicht / `configChanges` unvollstaendig
- **Symptom:** Trotz `android:configChanges` greift `onConfigurationChanged()` nicht; oder Activity recreated trotzdem bei nicht deklarierten Configs (Locale/Dark-Mode).
- **Ursache:** (a) Bekanntes Verhalten Android 11–13 (API 30–33). (b) `configChanges` muss ALLE relevanten Flags enthalten (`orientation|screenSize|smallestScreenSize|screenLayout|density|uiMode|locale|layoutDirection`) — fehlt eines → Recreation. Dark-Mode = `uiMode`, Sprache = `locale`.
- **Versionen:** Bug Android 11–13; `configChanges` per Design.
- **FIX:** Wo moeglich Config-Changes ueber System-Recreation + ViewModel/SavedState handhaben statt manuell. Wenn manuell: alle noetigen Flags deklarieren UND lesen; State lifecycle-robust halten, nicht auf den Callback verlassen.
- **Quelle:** https://developer.android.com/guide/topics/resources/runtime-changes

### 1.12 `ProcessLifecycleOwner`-Observer in der Activity registriert → Leak + Mehrfach-Callbacks
- **Symptom:** Nach Rotation/System-Theme-Wechsel/Prozess-Restore laufen App-Vordergrund-Callbacks (Auth-Prompt, VPN-Connect/Disconnect) MEHRFACH; Memory-Profiler zeigt zerstoerte Activities, die nicht freigegeben werden.
- **Ursache:** `ProcessLifecycleOwner.get().lifecycle.addObserver(...)` in `Activity.onCreate` mit anonymem Observer: Der Process-Lifecycle lebt fuer immer, die Activity nicht — bei jeder Recreation kommt ein NEUER Observer dazu, keiner wird entfernt, und jeder alte haelt seine tote Activity fest.
- **Versionen:** per Design (lifecycle-process, alle Versionen).
- **FIX (funktionserhaltend):** Observer als Feld halten und in `onDestroy()` per `removeObserver()` abmelden — ODER die Logik in eine Klasse mit Prozess-Lebensdauer (Application/Singleton) verlegen, wenn sie wirklich pro Prozess statt pro Activity gemeint ist. (Gefunden 2026-07-02 in CortexAndroid MainActivity: Biometrie-Auth + VPN-Autoconnect.)
- **Quelle:** developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner

---

## 2. Runtime-Permissions (besonders neue API-Level)

### 2.1 `POST_NOTIFICATIONS` — Notifications still verworfen (Android 13)
- **Symptom:** `notify()` wirft nichts, aber nichts erscheint. Bei Neuinstallation auf API 33+ sind Notifications per Default AUS.
- **Ursache:** Ab Android 13 (API 33) ist `POST_NOTIFICATIONS` eine Runtime-Permission; ohne Grant werden nicht-exempte Notifications **still** verworfen.
- **Versionen:** ab API 33 (per Design). Apps mit targetSdk < 33 bekommen den Dialog beim ersten Channel-Erstellen.
- **FIX:** Permission deklarieren + zur Laufzeit anfragen (`RequestPermission`); vor `notify()` `NotificationManagerCompat.areNotificationsEnabled()` pruefen. (Notification-Seite: 6.2/6.3.)
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/notification-permission

### 2.2 `READ_MEDIA_*` ersetzt `READ_EXTERNAL_STORAGE` (Android 13)
- **Symptom:** `READ_EXTERNAL_STORAGE` wird auf API 33+ ignoriert → kein Medienzugriff / SecurityException bei MediaStore-Query.
- **Ursache:** Ab Android 13 granulare `READ_MEDIA_IMAGES/VIDEO/AUDIO`.
- **Versionen:** ab API 33 (per Design).
- **FIX:** Pro Medientyp passende Permission deklarieren/anfragen (mit `Build.VERSION`-Verzweigung fuer ≤ API 32); `READ_EXTERNAL_STORAGE` mit `android:maxSdkVersion="32"`. Besser: **Photo Picker** (`ACTION_PICK_IMAGES`) — ganz ohne Permission.
- **Quelle:** https://developer.android.com/about/versions/13/behavior-changes-13

### 2.3 `READ_MEDIA_VISUAL_USER_SELECTED` — partieller Fotozugriff (Android 14)
- **Symptom:** Eigener Gallery-Picker zeigt leere/abgeschnittene Liste; Nutzer hat nur „Selected photos" gewaehlt.
- **Ursache:** Ab Android 14 (API 34) kann partieller Zugriff gegeben werden; dann ist nur `READ_MEDIA_VISUAL_USER_SELECTED` gewaehrt, nicht `READ_MEDIA_IMAGES/VIDEO`.
- **Versionen:** ab API 34 (per Design).
- **FIX:** `READ_MEDIA_VISUAL_USER_SELECTED` deklarieren + zusammen mit `READ_MEDIA_IMAGES/VIDEO` in EINEM Request anfragen; drei Zustaende behandeln (full/partial/denied); „Manage"-Button anbieten.
- **Quelle:** https://developer.android.com/about/versions/14/changes/partial-photo-video-access

### 2.4 Package Visibility — `resolveActivity`/`queryIntentActivities` null/leer ohne `<queries>`
- **Symptom:** `resolveActivity()` gibt `null`, `queryIntentActivities()`/`resolveContentProvider()` leer — obwohl Ziel-App/Provider installiert. `startActivity()` „schlaegt fehl".
- **Ursache:** Ab Android 11 (API 30) filtert das System sichtbare Pakete.
- **Versionen:** ab API 30 (per Design).
- **FIX:** `<queries>` ins Manifest — Ziel-Apps per `<package>`, `<intent>` oder `<provider android:authorities>`. NUR im Notfall `QUERY_ALL_PACKAGES` (Play-Policy). (Real genutzt in EntropieReductor fuer den Journal-Provider + Health Connect.)
- **Quelle:** https://developer.android.com/training/package-visibility

### 2.5 Bluetooth-Permissions Runtime + `neverForLocation` (Android 12)
- **Symptom:** BLE-Scan liefert leere Ergebnisse / `SecurityException`; Standort-Permission wird unnoetig verlangt.
- **Ursache:** Ab Android 12 (API 31) sind `BLUETOOTH_SCAN/CONNECT/ADVERTISE` Runtime-Permissions; ohne `neverForLocation` wird weiter `ACCESS_FINE_LOCATION` fuer Scans gefordert.
- **Versionen:** ab API 31 (per Design).
- **FIX:** Die drei Permissions zur Laufzeit anfragen; wenn nie Standort aus Scans abgeleitet wird: `android:usesPermissionFlags="neverForLocation"` an `BLUETOOTH_SCAN` + `ACCESS_FINE_LOCATION` auf `maxSdkVersion="30"`.
- **Quelle:** https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

### 2.6 `ACCESS_BACKGROUND_LOCATION` — separater zweiter Flow Pflicht
- **Symptom:** Werden Foreground- + Background-Location gleichzeitig angefragt, gewaehrt das System KEINE der beiden.
- **Ursache:** Ab Android 11 (API 30) erzwingt das System inkrementelle Anfragen; Background nur ueber Settings-Seite.
- **Versionen:** ab API 30 (per Design).
- **FIX:** Erst `ACCESS_FINE/COARSE_LOCATION`, nach Grant in SEPARATEM Request `ACCESS_BACKGROUND_LOCATION` (leitet auf Settings). Nie buendeln.
- **Quelle:** https://developer.android.com/develop/sensors-and-location/location/permissions/background

### 2.7 Permission Auto-Reset / App-Hibernation (ungenutzte Apps)
- **Symptom:** Nach Monaten Nichtnutzung sind Runtime-Permissions entzogen; App verhaelt sich wie nach Force-Stop.
- **Ursache:** Ab Android 11 (API 30, retroaktiv via Play-Services) resettet das System Permissions ungenutzter Apps automatisch.
- **Versionen:** ab API 30 (per Design).
- **FIX:** Beim Start Permissions immer re-pruefen statt anzunehmen; Status via `PackageManagerCompat.getUnusedAppRestrictionsStatus()`; bei kritischen Hintergrund-Funktionen Nutzer um Deaktivierung bitten (`ACTION_APPLICATION_DETAILS_SETTINGS`).
- **Quelle:** https://developer.android.com/topic/performance/app-hibernation

### 2.8 `shouldShowRequestPermissionRationale` — permanently-denied erkennen
- **Symptom:** Nach `requestPermissions()` erscheint kein Dialog mehr; nichts passiert.
- **Ursache:** Ab Android 11 gilt zweimal „Deny" als „Don't ask again" (`USER_FIXED`). In diesem Zustand gibt `shouldShowRequestPermissionRationale()` **false** zurueck (nicht true).
- **Versionen:** ab API 30 (per Design).
- **FIX:** `!granted && !shouldShowRequestPermissionRationale()` (und nicht beim ersten Mal) ⇒ permanently denied → in App-Settings leiten. `true` ⇒ Rationale zeigen, erneut anfragen.
- **Quelle:** https://developer.android.com/training/permissions/requesting

### 2.9 Permission-Gruppen — nur 1 pro Gruppe anfragen
- **Symptom:** Eine Permission „scheint" gewaehrt obwohl nicht angefragt; geraeteabhaengig inkonsistent.
- **Ursache:** Vor Android 13 wurde mit Grant einer Permission die ganze Gruppe implizit erlaubt; ab API 33 granular. Annahmen ueber Gruppen-Grants schlagen fehl.
- **Versionen:** verschaerft ab API 33.
- **FIX:** Jede benoetigte Permission einzeln per `checkSelfPermission()` pruefen/anfragen — nie auf Gruppen-Implikation verlassen.
- **Quelle:** https://developer.android.com/training/permissions/requesting

### 2.10 Health Connect — Dialog „finished" sofort / 30-Tage-Limit / Background
- **Symptom:** Permission-Dialog schliesst sofort ohne Auswahl; aeltere Daten unlesbar; Background-Read liefert nichts.
- **Ursache:** (a) Nicht alle angefragten Health-Permissions im Manifest deklariert → Dialog bricht sofort ab. (b) Default nur Daten bis 30 Tage vor erstem Grant. (c) Background-Read braucht eigene Permission.
- **Versionen:** Health Connect SDK (per Design). (EntropieReductor deklariert deshalb prophylaktisch alle Health-READ-Permissions im Manifest.)
- **FIX:** Jede genutzte Data-Type-Permission + Privacy-Activity im Manifest deklarieren; `READ_HEALTH_DATA_HISTORY` fuer > 30 Tage; `READ_HEALTH_DATA_IN_BACKGROUND` fuer Hintergrund. (Android 16: BODY_SENSORS → granulare `health.*`, siehe 8.9.)
- **Quelle:** https://developer.android.com/health-and-fitness/health-connect/ui/permissions

### 2.11 Custom-Permission von einer ANDEREN App: `granted=false` durch Installationsreihenfolge ⭐ (Multi-App-Setup)
- **Symptom:** App B liest einen ContentProvider von App A und bekommt zur Laufzeit `SecurityException: Permission Denial … requires <App-A>.permission.X, or grantUriPermission()`. `adb shell dumpsys package <App-B> | grep <X>` zeigt die Permission zwar **deklariert**, aber `granted=false` — obwohl App A (die sie definiert) installiert ist und der Provider existiert.
- **Ursache:** Custom-Permissions mit `protectionLevel="normal"` (oder `signature`) werden der NUTZENDEN App nur bei DEREN Installation gewaehrt — und nur, wenn die DEFINIERENDE App zu dem Zeitpunkt bereits installiert ist (mit der Manifest-Version, die die Permission enthaelt). War App B vor App A (bzw. vor der A-Version mit der Permission) installiert, bleibt die Permission **dauerhaft** `granted=false`; eine spaetere Installation/Aktualisierung von App A gewaehrt sie **NICHT rueckwirkend**. (Verwandt, aber anders: §2.4 Package-Visibility — dort fehlt der Provider mangels `<queries>`; hier existiert der Provider, nur die Permission fehlt.)
- **Versionen:** alle (Android-Custom-Permission-Modell, per Design).
- **FIX (funktionserhaltend, KEIN Datenverlust):** Reihenfolge herstellen — die DEFINIERENDE App A (mit Provider + Permission) **zuerst** installieren/aktualisieren, DANN die NUTZENDE App B **neu installieren** (`adb install -r <apk>` reicht, App-Daten bleiben erhalten) → danach `granted=true`, Sync laeuft. `adb shell pm grant` hilft NICHT (das gilt nur fuer `dangerous`-Runtime-Permissions, nicht fuer `normal`/`signature`). NIEMALS App B deinstallieren, um die Permission zu „erzwingen" (Datenverlust) — `install -r` genuegt.
- **Echter Vorfall (2026-06-20):** EntropieReductor (B) spiegelt das Tagebuch der BestJournal-Frank-App (A) read-only ueber `content://com.entropyjournal[.debug].journalexport/entries`, geschuetzt durch `com.entropyjournal.permission.READ_JOURNAL`. Auf einem frisch eingerichteten Geraet (S23 Ultra) war B installiert, als A noch die alte Version OHNE Provider/Permission war → Journal-Reiter zeigte „0 Eintraege". Fix: A auf >= 0.19.11 (mit Provider), dann B neu installiert → `granted=true`. Siehe Memory `reference_entropie_reductor_journal_mirror` + bug-cases 2026-06-20.
- **Quelle:** https://developer.android.com/guide/topics/permissions/defining ; https://developer.android.com/guide/topics/manifest/permission-element

---

## 3. Foreground Services & ANRs

### 3.1 `MissingForegroundServiceTypeException` — fehlender FGS-Typ (Android 14)
- **Symptom:** `android.app.MissingForegroundServiceTypeException: Starting FGS without a type` beim `startForeground()` → Crash.
- **Ursache:** Ab targetSdk 34 muss jeder FGS einen `foregroundServiceType` deklarieren.
- **Versionen:** ab Android 14 (API 34) per Design.
- **FIX:** `<service android:foregroundServiceType="dataSync" .../>` + Typ-Arg in `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`. Mehrere Typen mit `|`. Gilt auch fuer WorkManager-Foreground-Worker (siehe 3.3).
- **Quelle:** https://developer.android.com/about/versions/14/changes/fgs-types-required

### 3.2 `SecurityException` — fehlende typ-spezifische FGS-Permission (Android 14)
- **Symptom:** `SecurityException` nach `startForeground()` („requires permission …"); FGS startet nicht / Crash.
- **Ursache:** Neben `FOREGROUND_SERVICE` fehlt die typ-spezifische Permission (`FOREGROUND_SERVICE_MICROPHONE`, `_DATA_SYNC`, `_MEDIA_PLAYBACK`, `_CAMERA`, …). Bei `microphone`/`camera`/`location` muss zusaetzlich die while-in-use-Runtime-Permission aktuell gewaehrt sein.
- **Versionen:** `FOREGROUND_SERVICE` ab API 28; typ-Permissions ab API 34.
- **FIX:** Im Manifest BEIDE `uses-permission` deklarieren + Laufzeit-Permission (mic/cam/loc) vor dem Start bestaetigen. (EntropieReductor: `FOREGROUND_SERVICE_MICROPHONE` + `FOREGROUND_SERVICE_DATA_SYNC` + `RECORD_AUDIO`.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/declare

### 3.3 WorkManager: `foregroundServiceType`-Mismatch → IllegalArgumentException
- **Symptom:** `IllegalArgumentException: foregroundServiceType 0x… is not a subset of foregroundServiceType attribute … in service element of manifest file` (zielt auf `SystemForegroundService`).
- **Ursache:** WorkManager nutzt intern `androidx.work.impl.foreground.SystemForegroundService`. Der in `ForegroundInfo` verwendete Typ ist im App-Manifest fuer diesen Service NICHT deklariert (man denkt nur an den eigenen Service).
- **Versionen:** ab API 34.
- **FIX:** Im App-Manifest `<service android:name="androidx.work.impl.foreground.SystemForegroundService" android:foregroundServiceType="dataSync|microphone" tools:node="merge" tools:replace="android:foregroundServiceType" />` + passende Permission + gleicher Typ in `ForegroundInfo`. (Exakt so in EntropieReductor geloest.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/declare

### 3.4 `ForegroundServiceStartNotAllowedException` — FGS-Start aus dem Hintergrund (Android 12)
- **Symptom:** `ForegroundServiceStartNotAllowedException` beim FGS-Start, waehrend die App im Hintergrund ist.
- **Ursache:** Ab Android 12 (API 31) generell verboten, ausser eine dokumentierte Ausnahme greift.
- **Versionen:** ab API 31 (per Design). Android 14: while-in-use-FGS (mic/cam/location) duerfen selbst MIT Ausnahme nicht aus dem Hintergrund ERSTELLT werden.
- **FIX:** FGS aus sichtbarer Activity starten ODER eine Ausnahme nutzen (FCM **high-priority** — vorher `getPriority()==PRIORITY_HIGH` pruefen; Notification/Widget-Interaktion; Exact-Alarm fuer Nutzeraktion; Boot-Broadcasts; CompanionDevice; Batterie-Optimierung deaktiviert; `SYSTEM_ALERT_WINDOW` — ab Android 15 zusaetzlich sichtbares Overlay noetig). Funktion bleibt, nur der Trigger-Pfad aendert sich.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

### 3.5 „startForegroundService() did not then call startForeground()" — ~5 s-ANR
- **Symptom:** `RemoteServiceException$ForegroundServiceDidNotStartInTimeException` (fataler Crash/ANR).
- **Ursache:** Nach `startForegroundService()` muss der Service binnen ~5 s `startForeground()` aufrufen; `onCreate`/`onStartCommand` laufen auf dem Main-Thread — blockiert dieser, kommt der Aufruf zu spaet.
- **Versionen:** ab Android 9 (API 28) per Design.
- **FIX:** `ServiceCompat.startForeground(...)` ganz am Anfang von `onStartCommand`/`onCreate` mit fertiger Notification, BEVOR Arbeit passiert; schwere Arbeit erst danach in Coroutine/Thread. (Bei zwei ueberlappenden Foreground-Workern: WorkManager 2.10.5+.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/troubleshooting

### 3.6 FGS-Timeout: dataSync/mediaProcessing max 6 h pro 24 h (Android 15)
- **Symptom:** Service wird nach Timeout gestoppt; `Service.onTimeout(int,int)` aufgerufen. Ohne sofortiges `stopSelf()`: `RemoteServiceException: "A foreground service of type dataSync did not stop within its timeout"`. Re-Start danach: `ForegroundServiceStartNotAllowedException: "Time limit already exhausted for foreground service type dataSync"`.
- **Ursache:** Ab Android 15 (API 35, targetSdk 35+) duerfen dataSync UND mediaProcessing je max 6 h in 24 h laufen (getrennt getrackt); Timer resettet, wenn die App in den Vordergrund kommt.
- **Versionen:** ab API 35 (per Design). Test: `adb shell am compat enable FGS_INTRODUCE_TIME_LIMITS <pkg>`.
- **FIX:** `onTimeout()` ueberschreiben → sofort `stopSelf()`; Arbeit < 6 h halten; lange Transfers als User-Initiated-Data-Transfer-Job (UIDT) / WorkManager. (Betrifft EntropieReductors dataSync-Worker — Bulk-Importe stueckeln.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### 3.7 `shortService`-FGS ~3 min-Timeout
- **Symptom:** `RemoteServiceException: "A foreground service of type FOREGROUND_SERVICE_TYPE_SHORT_SERVICE did not stop within its timeout"`.
- **Ursache:** `shortService` darf nur ~3 min laufen (strenger als dataSync).
- **Versionen:** ab Android 14 (API 34) per Design.
- **FIX:** Arbeit binnen ~3 min beenden; `onTimeout()` → `stopSelf()`. `shortService` braucht keine typ-Permission, kann aber nicht in andere Typen ueberfuehrt werden.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### 3.8 `FOREGROUND_SERVICE_TYPE_HEALTH` braucht `health.*`-Permission (Android 16)
- **Symptom:** Health-FGS schlaegt auf Android 16 fehl.
- **Ursache:** targetSdk 36 verlangt fuer `FOREGROUND_SERVICE_TYPE_HEALTH` die konkrete `android.permission.health.*`-Permission statt `BODY_SENSORS`.
- **Versionen:** ab Android 16 (API 36) per Design.
- **FIX:** Manifest auf die konkreten health-Permissions umstellen (siehe 8.9).
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 3.9 ANR-Schwellen (alle Versionen, Main-Thread)
- **Symptom:** „Input dispatching timed out" (Touch/Key > 5 s); BroadcastReceiver-ANR (`onReceive` > ~10–20 s); Service-ANR (Lifecycle > ~20 s); ContentProvider-/Binder-/`onStartJob`-ANR.
- **Ursache:** Main-/UI-Thread blockiert. `BroadcastReceiver.onReceive`, `JobScheduler.onStartJob` und ContentProvider-Queries laufen auf dem Main-Thread.
- **Versionen:** per Design (alle).
- **FIX:** Jede nicht-triviale Arbeit off-main (Coroutine/Executor); in Receivern `goAsync()` + Hintergrund-Thread oder Arbeit an WorkManager delegieren; `onStartJob` sofort returnen; StrictMode zum Aufdecken aktivieren. Nie Funktion entfernen, nur verlagern.
- **Quelle:** https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs

---

## 4. WorkManager, Background-Execution & Doze/App-Standby

### 4.1 WorkManager-Doppel-Initialisierung („WorkManager is already initialized")
- **Symptom:** Crash beim App-Start, sobald eine Custom-`Configuration` (z. B. Hilt-Worker) bereitgestellt wird.
- **Ursache:** Default-`WorkManagerInitializer` (via `androidx.startup.InitializationProvider`) UND die Custom-Konfig initialisieren beide.
- **Versionen:** WM 2.6+ (per Design).
- **FIX:** App implementiert `Configuration.Provider` UND im Manifest den Default-Initializer entfernen: `<provider androidx.startup.InitializationProvider … tools:node="merge"><meta-data androidx.work.WorkManagerInitializer tools:node="remove"/></provider>` (`tools:`-Namespace deklarieren). Exakt so in EntropieReductor.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration

### 4.2 Expedited Work — Quota erschoepft (IllegalStateException / stiller Fallback)
- **Symptom:** Expedited Worker startet nicht / `IllegalStateException`.
- **Ursache:** Expedited Jobs haben striktere, an den App-Standby-Bucket gekoppelte Quoten.
- **Versionen:** WM 2.6+ / API 31+ (auf < 31 FGS-basiert → FGS-Start-Restriktionen).
- **FIX:** `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` → faellt funktionserhaltend auf normale Work zurueck (nicht `DROP_WORK_REQUEST`); Exception abfangen.
- **Quelle:** https://developer.android.com/reference/androidx/work/OutOfQuotaPolicy

### 4.3 „Warum stoppt mein Worker?" — `getStopReason()` zur Diagnose
- **Symptom:** Worker bricht scheinbar grundlos ab.
- **Ursache:** Doze/Battery-Saver, Constraint verloren, Quota, App-Standby, force-stop (`STOP_REASON_USER`), Background-Restriction u. a.
- **Versionen:** `WorkInfo.getStopReason()` ab WM 2.9.0 (min API 31; auf < 31 immer `STOP_REASON_UNKNOWN`) — mit WM 2.10.0 verfuegbar.
- **FIX:** `getStopReason()` auslesen und loggen; je nach Code reagieren (z. B. bei `_CONSTRAINT_*` Constraints lockern, bei `_QUOTA` echten FGS nutzen). Nie still schlucken.
- **Quelle:** https://developer.android.com/reference/androidx/work/WorkInfo

### 4.4 `ExistingPeriodicWorkPolicy.KEEP` — Spec-Updates gehen verloren
- **Symptom:** Geaenderte Constraints/Intervall/Worker-Klasse greifen nicht; alter Worker laeuft weiter.
- **Ursache:** Bei `KEEP` wird die neue Spec ignoriert; naives CANCEL+REENQUEUE kann Laeufe verlieren/verdoppeln.
- **Versionen:** `UPDATE` ab WM 2.8.0.
- **FIX:** `ExistingPeriodicWorkPolicy.UPDATE` nutzen (aktualisiert Spec ohne Zaehler-Reset).
- **Quelle:** https://developer.android.com/reference/androidx/work/ExistingPeriodicWorkPolicy

### 4.5 Worker ueberlebt App-Swipe, aber NICHT „Stoppen erzwingen" / force-stop
- **Symptom:** Nach „App-Info → Stoppen erzwingen" (oder Crash-Loop) laeuft kein Worker/Alarm mehr, bis der Nutzer die App oeffnet.
- **Ursache:** force-stop setzt `FLAG_STOPPED` und schiebt die App in den RESTRICTED-Bucket; geplante Jobs werden gestrichen. (Swipe aus Recents ist KEIN force-stop.)
- **Versionen:** App-Standby-Buckets ab API 28; RESTRICTED ab API 30 (per Design).
- **FIX:** Kein Code-Fix moeglich. Worker bei App-Start/`onResume` mit `ExistingPeriodicWorkPolicy.KEEP` re-enqueuen; Stop-Grund loggen.
- **Quelle:** https://issuetracker.google.com/issues/150325687 *(issuetracker, nicht gh-verifiziert)*

### 4.6 PeriodicWork: 15-min-Minimum + `flexInterval` ist keine Garantie
- **Symptom:** Intervall < 15 min laeuft alle 15 min; Periodic-Work „laeuft nur einmal" / wird uebersprungen.
- **Ursache:** WM klemmt jedes Intervall still auf 15 min; `flexInterval` ist nur ein Ausfuehrungs-Fenster, keine Timing-Garantie; Constraints/Doze verschieben Laeufe.
- **Versionen:** alle WM-Versionen (per Design).
- **FIX:** Fuer haeufigere Frequenz `OneTimeWorkRequest`, der sich am Ende selbst neu enqueued (mit Backoff). Erwartung im Code dokumentieren.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### 4.7 Constraints werden nie erfuellt → Worker laeuft nie
- **Symptom:** Worker bleibt ewig `ENQUEUED`.
- **Ursache:** z. B. `NetworkType.UNMETERED` bei nur Mobilfunk, `setRequiresCharging(true)` ohne Ladegeraet, `BatteryNotLow` bei Dauer-Lowbattery.
- **Versionen:** alle (per Design).
- **FIX:** Constraints minimal halten; bei kritischen Tasks Fallback ohne Constraint nach Timeout; State per `getWorkInfoByIdLiveData` beobachten + Nutzer-Feedback.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### 4.8 `setExactAndAllowWhileIdle` — in Doze nur ~1×/9 min, nicht exakt
- **Symptom:** Alarm in Doze feuert verzoegert/gebuendelt.
- **Ursache:** In Doze nur in Maintenance-Windows; weder `setAndAllowWhileIdle` noch `setExactAndAllowWhileIdle` oefter als einmal/9 min pro App; Doku: „as nearly as possible".
- **Versionen:** Doze ab API 23 (per Design).
- **FIX:** Limit akzeptieren; nicht fuer Sub-9-min-Wiederholung nutzen; bei Doze-tolerantem Bedarf WorkManager. (Exact-Alarm-Permission: 6.4.)
- **Quelle:** https://developer.android.com/training/monitoring-device-state/doze-standby

### 4.9 Android 16 / targetSdk 36: JobScheduler-Runtime-Quota auch fuer FGS-Jobs
- **Symptom:** Lang laufende Worker werden auf Android 16 abgebrochen, obwohl ein FGS laeuft (vorher unbegrenzt).
- **Ursache:** Quota wird jetzt auch im Top-State, bei laufendem FGS und im Active-Bucket erzwungen (JobScheduler/WorkManager/DownloadManager). `setImportantWhileForeground` ist jetzt No-op.
- **Versionen:** ab Android 16 (API 36).
- **FIX:** Diagnose via `getStopReason()`; bei Quota-Erschoepfung echten FGS direkt statt Long-running-Worker; fuer Nutzer-Transfers `setUserInitiatedJob(true)` (UIDT, quota-befreit).
- **Diagnose-Neuerungen Android 16 (Re-Recherche 2026-07-02):** Neue API `JobScheduler#getPendingJobReasonsHistory()`
  zeigt, WARUM ein Job nicht lief. Zusaetzlich neuer Stop-Reason **`STOP_REASON_TIMEOUT_ABANDONED`** (statt
  `STOP_REASON_TIMEOUT`), wenn eine App **JobScheduler direkt** nutzt, KEINE starke Referenz auf `JobParameters`
  haelt und in einen Timeout laeuft — bei haeufigem Auftreten drosselt das System die Job-Frequenz.
  **WorkManager/AsyncTask/DownloadManager sind NICHT betroffen** (sie verwalten den Job-Lebenszyklus selbst).
  Test-Overrides: `adb shell am compat enable OVERRIDE_QUOTA_ENFORCEMENT_TO_TOP_STARTED_JOBS <pkg>` /
  `OVERRIDE_QUOTA_ENFORCEMENT_TO_FGS_JOBS <pkg>`.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/changes,
  https://developer.android.com/about/versions/16/behavior-changes-all

### 4.10 BOOT_COMPLETED: Worker/Alarme nach Reboot neu planen
- **Symptom:** Nach Neustart laufen geplante (auch periodische) Worker/Alarme nicht mehr.
- **Ursache:** AlarmManager-Alarme ueberleben Reboot generell nicht; ohne `RECEIVE_BOOT_COMPLETED`-Receiver werden eigene Re-Schedule-Hooks nicht ausgeloest. (Zusatz: bestimmte FGS-Typen duerfen ab Android 14/15 NICHT aus dem Boot-Receiver gestartet werden — siehe 6.9.)
- **Versionen:** per Design; FGS-Boot-Restriktion ab API 34/35.
- **FIX:** `RECEIVE_BOOT_COMPLETED`-Permission + Receiver, der bei `ACTION_BOOT_COMPLETED` Alarme/Work neu plant (mit `KEEP`); keine restriktierten FGS-Typen aus dem Boot-Receiver. (BestJournalAndroid hat `BootReminderReceiver` fuer genau das.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

### 4.11 CoroutineWorker/`doWork`-Exception → still `Result.failure`, kein Retry
- **Symptom:** Worker „scheitert" lautlos; kein Retry.
- **Ursache:** Uncaught Exception in `doWork()` → WM behandelt das als `Result.failure()`.
- **Versionen:** alle (per Design).
- **FIX:** `try/catch` im Worker; bei transienten Fehlern bewusst `Result.retry()`; `setBackoffCriteria(EXPONENTIAL, …)` (Min-Backoff 10 s); im Catch loggen (nicht schlucken).
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

### 4.12 Background-Activity-Start (BAL) standardmaessig blockiert (Android 15)
- **Symptom:** Aus dem Hintergrund (z. B. Worker via PendingIntent) gestartete Activity oeffnet nicht.
- **Ursache:** targetSdk 35+ gewaehrt PendingIntent-Erstellern BAL nicht mehr implizit.
- **Versionen:** ab API 35; Android 16 ergaenzt explizite Modes.
- **FIX:** Statt Activity aus Background → Notification mit `PendingIntent` (Nutzer-Tap startet legal). Wenn unvermeidbar: `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE` explizit setzen.
- **Quelle:** https://developer.android.com/guide/components/activities/background-starts

### 4.13 Battery-Optimization-Whitelist — Play-Policy-Falle + OEM-Killer
- **Symptom:** Worker laufen auf manchen Geraeten (Xiaomi/Huawei/Samsung aggressiv) unzuverlaessig.
- **Ursache:** Doze/App-Standby + OEM-Killer drosseln. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` darf laut Play-Policy nur wenige App-Kategorien nutzen.
- **Versionen:** Doze ab API 23; Policy laufend.
- **FIX:** Nicht pauschal whitelisten. WorkManager + korrekte Constraints + Expedited fuer dringend; echte Dauer-Tasks → FGS. Nutzer ueber Geraete-Einstellungen informieren statt programmatisch erzwingen.
- **Quelle:** https://developer.android.com/training/monitoring-device-state/doze-standby

### 4.14 Hilt-`@HiltWorker` wird nie aufgerufen — stiller Fehler (kein Crash)
- **Symptom:** Ein `@HiltWorker` mit `@AssistedInject`-Konstruktor wird scheinbar nie ausgefuehrt / dependencies sind null; KEINE Exception, nur „passiert nichts".
- **Ursache:** Die `Application` implementiert kein `Configuration.Provider` mit `setWorkerFactory(HiltWorkerFactory)`, ODER der Default-`WorkManagerInitializer` wurde nicht aus dem Manifest entfernt. Dann nutzt WorkManager die Default-Factory, die den `@AssistedInject`-Konstruktor nicht kennt — stiller Fehlschlag.
- **Versionen:** hilt-work (per Design, betrifft EntropieReductor mit `hilt-work 1.2.0`).
- **FIX:** `Application : Configuration.Provider` mit `@Inject HiltWorkerFactory` + `setWorkerFactory(...)`, UND im Manifest den Default-Initializer per `tools:node="remove"` entfernen (siehe 4.1). Best-Practice: §4.9. Nie die Funktion „weglassen" — die DI-Verkabelung reparieren.
- **Quelle:** https://developer.android.com/reference/androidx/hilt/work/HiltWorker

### 4.15 Expedited Worker ohne `getForegroundInfo()` → Crash; `setForeground()` → IllegalStateException
- **Symptom:** (a) `setExpedited(...)`-Worker crasht auf aelteren Plattformversionen (vor API 31) zur Laufzeit. (b) `setForeground(...)`/`setForegroundAsync(...)` wirft `IllegalStateException`.
- **Ursache:** (a) Auf < API 31 laeuft Expedited Work als FGS und verlangt darum eine `getForegroundInfo()`-Implementierung — fehlt sie, Crash. (b) `setForeground` wird aufgerufen, waehrend die App nicht in den Vordergrund treten darf.
- **Versionen:** WM 2.6+ / per Design.
- **FIX:** Bei `setExpedited` IMMER `getForegroundInfo()` implementieren; `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` setzen (statt DROP). `setForeground` in `try/catch (IllegalStateException)` kapseln. Best-Practice: §4.5/§4.6.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

---

## 5. Room (Runtime/Migrations), WAL & SQLite

### 5.1 „A migration from X to Y was required but not found" (IllegalStateException)
- **Symptom:** Crash beim ersten DB-Zugriff nach Update, sobald `@Database`-`version` erhoeht wurde.
- **Ursache:** version-Bump ohne `Migration`/Auto-Migration-Pfad.
- **Versionen:** per Design (alle Room-Versionen).
- **FIX:** Passendes `Migration(N, M)` schreiben + `addMigrations(...)`, ODER `@AutoMigration(from=N, to=M)` wenn auto-faehig. **Niemals** destruktive Migration als „Fix" (= Datenverlust).
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### 5.2 `fallbackToDestructiveMigration()` = stiller Datenverlust + Pflicht-Argument ab 2.7
- **Symptom:** Nach Schema-Aenderung alle Nutzerdaten weg, ohne Fehler. Ab Room 2.7 kompiliert die Methode ohne Argument nicht mehr (verlangt `fallbackToDestructiveMigration(dropAllTables: Boolean)`).
- **Ursache:** Loescht bei fehlendem Migrationspfad die DB und legt sie neu an — by design. 2.7 macht die API explizit.
- **Versionen:** Verhalten per Design alle Versionen; Pflicht-Boolean ab 2.7.0.
- **FIX:** In Produktion NICHT verwenden (nur reine Cache-DBs). Echten Migrationspfad schreiben. Nie als Bequemlichkeits-Fix fuer 5.1.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### 5.3 „Room cannot verify the data integrity … changed schema but forgot to update the version" (identityHash)
- **Symptom:** Crash beim DB-Open, oft NUR bei Update-Installs: `IllegalStateException: Room cannot verify the data integrity …`.
- **Ursache:** Room vergleicht den SHA-256-`identityHash` des Schemas (`room_master_table`) mit dem generierten. Schema geaendert, `version` nicht erhoeht → Mismatch. **Versteckte Variante:** Android Auto-Backup (`allowBackup=true`) stellt eine ALTE DB auf neuem Geraet/Reinstall wieder her, waehrend der Code ein neues Schema erwartet → derselbe Crash.
- **Versionen:** per Design (alle).
- **FIX:** Bei echter Schema-Aenderung `version` erhoehen + Migration. Gegen die Auto-Backup-Falle: DB gezielt aus dem Backup ausschliessen (`dataExtractionRules`/`fullBackupContent` mit `<exclude domain="database" .../>`) statt naiv `allowBackup=false`.
- **Quelle:** https://imunique-zj.medium.com/lesson-learnt-from-allowbackup-and-changing-room-database-schema-d8ed0a0acddd

### 5.4 Auto-Migration scheitert bei Rename/Delete → `AutoMigrationSpec` noetig
- **Symptom:** Build-Fehler beim Annotation-Processing: Room kann Rename vs. Delete nicht entscheiden.
- **Ursache:** Bei `@RenameColumn`/`@RenameTable`/`@DeleteColumn`/`@DeleteTable` ist die Aenderung mehrdeutig.
- **Versionen:** per Design ab 2.4.0 (Auto-Migrations). *(Build-naher Fall — Laufzeit-Folge ist der Migrations-Crash; KSP-Build selbst siehe `gradle.md` §8.)*
- **FIX:** Statische `AutoMigrationSpec`-Klasse mit `@RenameColumn(...)` etc. + `@AutoMigration(spec = ...)`. Bei komplexen Aenderungen manuelle `Migration`.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### 5.5 `exportSchema=true` + `room.schemaLocation` Pflicht fuer Migrationstests
- **Symptom:** Auto-Migrations/`MigrationTestHelper` finden kein Schema-JSON; keine Migrationstests; Build-Warnung „Schema export directory is not provided".
- **Ursache:** Ohne exportiertes Schema kennt Room das alte Schema nicht.
- **Versionen:** per Design; Room-Gradle-Plugin (reproduzierbar) ab 2.6.0.
- **FIX:** `@Database(exportSchema = true)` + `room { schemaDirectory("$projectDir/schemas") }`; Schema-JSONs einchecken. (Beide Apps setzen `room.schemaLocation` via `ksp { arg(...) }`.)
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### 5.6 Destructive Migration droppt jetzt auch Views
- **Symptom:** `@DatabaseView`s verhalten sich nach destruktiver Migration anders / werden neu erstellt.
- **Ursache:** Ab 2.7.0-alpha12 werden Views bei destruktiver Migration gedroppt (Angleichung ans KMP-Verhalten; b/381518941).
- **Versionen:** ab 2.7.0-alpha12 — in 2.7.0 enthalten.
- **FIX:** Kein Eingriff noetig; bei View-Abhaengigkeit deren Neuerstellung pruefen.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### 5.7 WAL: frische Writes liegen in `-wal`/`-shm` → Backup/Drive-Upload verliert Daten
- **Symptom:** Kopiertes `.db`-File (Backup/Drive-Upload) enthaelt die letzten Eintraege nicht; nach Restore fehlen frische Daten — ohne Fehler. Oder die Kopie wirkt korrupt.
- **Ursache:** Room nutzt standardmaessig WAL-Journal-Modus. Noch nicht gecheckpointete Writes liegen in `<db>-wal`/`<db>-shm`; kopiert man nur die Hauptdatei, fehlen sie.
- **Versionen:** per Design (WAL Default).
- **FIX:** VOR dem Kopieren `db.close()` (forciert Checkpoint) ODER `PRAGMA wal_checkpoint(TRUNCATE)`; alternativ alle drei Dateien zusammen sichern; am saubersten SQLite-Online-Backup (`VACUUM INTO`). Nie die Live-`.db` waehrend aktiver Writes kopieren. **(Lokal bestaetigt: Memory `feedback_wal_checkpoint_close_room` — Room vor Backup schliessen.)**
- **Quelle:** https://sqlite.org/forum/info/47107ab818977549

### 5.8 `SQLiteDatabaseCorruptException` / „database disk image is malformed"
- **Symptom:** Crash beim Open/Query.
- **Ursache:** Stromausfall/Kill waehrend Write, defekte Sektoren, mehrere Prozesse ohne Koordination, Datei-Kopie waehrend aktiver Writes (siehe 5.7).
- **Versionen:** per Design, plattformweit.
- **FIX:** `RoomDatabase.Builder` mit `onCorruption`/`DatabaseErrorHandler`; `PRAGMA integrity_check` zur Diagnose; aus letztem gueltigem Backup wiederherstellen; korrupte DB nur als letzten Ausweg loeschen. WAL-Korruption oft via `wal_checkpoint(TRUNCATE)` + Re-Open behebbar.
- **Quelle:** https://www.dbpro.app/learn/sqlite/errors/database-corrupt

### 5.9 „Row too big to fit into CursorWindow" / „Couldn't read row 0, col -1 from CursorWindow"
- **Symptom:** Crash beim Lesen grosser Zeilen (`SQLiteBlobTooBigException`) oder die maskierte „Couldn't read row"-Variante.
- **Ursache:** CursorWindow ist auf ~2 MB pro Window begrenzt; eine Zeile (grosser BLOB/langer Text) ueberschreitet das.
- **Versionen:** per Design, plattformweit.
- **FIX:** Grosse BLOBs NICHT in der DB — Datei/URI ablegen, nur Pfad speichern; bei vielen Zeilen Paging (`PagingSource`/LIMIT). Nie Spalten „weglassen".
- **Quelle:** https://medium.com/bobble-engineering/android-database-sqlite-sqliteblobtoobigexception-in-room-database-7bb70ce17717

### 5.10 „Cannot access database on the main thread" (+ `allowMainThreadQueries`-Falle)
- **Symptom:** `IllegalStateException: Cannot access database on the main thread …`.
- **Ursache:** Synchroner DAO-Aufruf auf dem Main-Thread (Room blockt das bewusst zur ANR-Vermeidung).
- **Versionen:** per Design.
- **FIX:** DAO-Zugriff `suspend`/Flow/Background-Thread. `allowMainThreadQueries()` nur in Tests/Debug — in Produktion verschleiert es ANRs.
- **Quelle:** https://commonsware.com/Room/pages/chap-roomthreads-001.html

### 5.11 „database is locked" / `SQLiteDatabaseLockedException`
- **Symptom:** Sporadische `database is locked`-Fehler, v. a. bei nebenlaeufigen Writes/mehreren Verbindungen.
- **Ursache:** WAL erlaubt viele Reader, aber nur EINEN Writer; mehrere Instanzen/Prozesse ohne Koordination kollidieren. In fruehen 2.7-Alphas fehlte `busy_timeout` auf der initialen Connection (b/380088809).
- **Versionen:** locked generell per Design; busy_timeout-Fix ab 2.7.0-rc01 (in 2.7.0 enthalten).
- **FIX:** DB als **Singleton** (siehe 5.13); keine mehrfachen `RoomDatabase`-Instanzen; auf Room ≥ 2.7.0 bleiben.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### 5.12 Connection-Pool-Timeout / Deadlock (neue Driver-Architektur)
- **Symptom:** Bei `SQLiteDriver`-Nutzung: „Timed out attempting to acquire reader/writer connection" unter Last; teils Deadlock beim Re-Open einer auto-closed DB aus Flow-Emission.
- **Ursache:** Connection-Pool der neuen KMP-/Driver-Architektur.
- **Versionen:** Pool-Fix ab 2.7.0-rc03 (in 2.7.0); auto-close-Deadlock erst ab 2.8.2 (b/446643789).
- **FIX:** Auf Room ≥ 2.7.0 bleiben; wer `setAutoCloseTimeout` + Flow kombiniert → auf 2.8.2+.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### 5.13 Nicht-Singleton DB-Builder → mehrere Instanzen, Inkonsistenz & Locks
- **Symptom:** Daten inkonsistent zwischen Screens; Haeufung von „database is locked".
- **Ursache:** `Room.databaseBuilder(...).build()` mehrfach → mehrere unabhaengige DB-Instanzen mit eigenen Pools auf derselben Datei.
- **Versionen:** per Design.
- **FIX:** DB als prozessweites Singleton (Hilt `@Singleton` / `@Volatile` + `synchronized`). Nie pro ViewModel neu bauen.
- **Quelle:** https://developer.android.com/training/data-storage/room

### 5.14 TypeConverter: „Cannot figure out how to save this field" / NPE zur Laufzeit
- **Symptom:** Build-Fehler bzw. NPE, wenn ein Converter `null` nicht behandelt.
- **Ursache:** Nicht-primitiver Feldtyp ohne `@TypeConverter`; Converter nicht im Scope; Converter wirft bei `null`.
- **Versionen:** per Design.
- **FIX:** Converter-Klasse mit zwei `@TypeConverter`-Methoden (hin/zurueck), per `@TypeConverters` registrieren (richtiger Scope); Null-Faelle explizit behandeln.
- **Quelle:** https://medium.com/bobble-engineering/cannot-figure-out-how-to-save-this-field-into-database-exception-in-room-database-kotlin-24619717acef

### 5.15 KSP2 + Room: Schema-Dir nicht gesetzt / Annotation-Errors
- **Symptom:** Unter KSP2 wird das Schema-Verzeichnis nicht angelegt → Auto-Migrations/Tests scheitern; teils Annotation-Errors.
- **Ursache:** Room-Gradle-Plugin unterstuetzte KSP2-Task anfangs nicht (b/379159770). Generelle KSP2+Room-Annotation-Probleme: **google/ksp#1896 — Status OPEN (gh-geprueft 2026-06-02)**.
- **Versionen:** Schema-Dir-Fix ab 2.7.0-alpha13 (in 2.7.0); KSP2+Room-Annotation-Issue noch offen.
- **FIX:** Room ≥ 2.7.0 (Schema-Dir behoben). Bei KSP1→KSP2-Umstieg #1896 im Blick behalten; KSP2 mit Kotlin 2.0+ empfohlen. (KSP-Build-Integration: `gradle.md` §8.)
- **Quelle:** https://github.com/google/ksp/issues/1896 *(gh-geprueft: OPEN)*

---

## 6. PendingIntent, AlarmManager, Notifications & BroadcastReceiver

### 6.1 PendingIntent ohne Mutability-Flag → IllegalArgumentException (Android 12)
- **Symptom:** `IllegalArgumentException: Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified` (oft via `getBroadcast`/`getActivity`).
- **Ursache:** Ab Android 12 (API 31) muss bei `PendingIntent`-Erzeugung ein Mutability-Flag gesetzt sein.
- **Versionen:** ab API 31 (per Design). `FLAG_IMMUTABLE` existiert ab API 23 → keine Versionsweiche noetig.
- **FIX:** Default `PendingIntent.FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`. Nur `FLAG_MUTABLE`, wenn das System das Intent fuellen muss (z. B. Direct Reply, Bubbles). Beide Apps nutzen PendingIntents (Reminder/Widget-Taps).
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

### 6.2 Notification ohne Channel erscheint nicht (Android 8)
- **Symptom:** `notify()` ohne Effekt; keine Exception.
- **Ursache:** Ab Android 8 (API 26) braucht jede Notification einen Channel; ohne Channel wird sie verworfen.
- **Versionen:** ab API 26 (per Design).
- **FIX:** Channel(s) einmalig erstellen (`NotificationManager.createNotificationChannel`) bevor `notify()`. Channel-Importance ist nach Erstellung nicht mehr aenderbar — sinnvoll waehlen.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/channels

### 6.3 Notification-Trampolines verboten (Android 12)
- **Symptom:** Tippt der Nutzer die Notification, oeffnet die Ziel-Activity nicht; Logcat warnt vor Trampoline.
- **Ursache:** Ab Android 12 darf eine Notification keine Activity ueber einen Umweg via Service/BroadcastReceiver starten.
- **Versionen:** ab API 31 (per Design).
- **FIX:** `PendingIntent.getActivity()` direkt verwenden; Daten via Intent-Extras uebergeben statt im Service vorzubereiten.
- **Quelle:** https://developer.android.com/about/versions/12/behavior-changes-12

### 6.4 `SCHEDULE_EXACT_ALARM` default-denied → SecurityException (Android 14)
- **Symptom:** Exakte Alarme feuern nicht; `setExact()`/`setExactAndAllowWhileIdle()` wirft `SecurityException` auf neu installierten Apps unter Android 14.
- **Ursache:** Ab Android 14 wird `SCHEDULE_EXACT_ALARM` neu installierten Apps (targetSdk 33+) nicht mehr pre-granted (Ausnahmen: Wecker/Kalender). Permission-Entzug cancelt alle kuenftigen Exact-Alarms.
- **Versionen:** API 31 special access; API 33 `USE_EXACT_ALARM`; ab API 34 default-denied.
- **FIX:** Vor dem Setzen `AlarmManager.canScheduleExactAlarms()` pruefen; fehlt sie → `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`-Intent. **Permission-freie Alternative:** `setExact(..., listener, handler)` (OnAlarmListener) braucht `SCHEDULE_EXACT_ALARM` nicht. `USE_EXACT_ALARM` (Normal-Permission) NUR fuer echte Wecker/Kalender-Apps (Play-Policy!). BestJournalAndroid plant Reminder via AlarmManager → betroffen.
- **Quelle:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

### 6.5 `USE_FULL_SCREEN_INTENT` von Google Play widerrufen (Android 14)
- **Symptom:** Full-Screen-Intent-Notifications (z. B. Alarm-/Anruf-Vollbild) zeigen nur noch eine Heads-up-Notification.
- **Ursache:** Ab Android 14 ist `USE_FULL_SCREEN_INTENT` nur noch fuer Alarm-/Anruf-Apps automatisch gewaehrt; anderen Apps von Google Play entzogen.
- **Versionen:** ab API 34 (per Design).
- **FIX:** `NotificationManager.canUseFullScreenIntent()` pruefen; fehlt sie → `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`-Settings; funktionserhaltend auf normale Heads-up-Notification zuruckfallen, wenn nicht gewaehrt.
- **Quelle:** https://developer.android.com/about/versions/14/behavior-changes-14

### 6.6 Implicit-Broadcast-Restriktionen (Android 8)
- **Symptom:** Manifest-deklarierter Receiver fuer einen impliziten Broadcast feuert nicht.
- **Ursache:** Ab Android 8 (API 26) werden die meisten impliziten Broadcasts NICHT mehr an im Manifest deklarierte Receiver zugestellt (Ausnahmenliste, u. a. `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `LOCALE_CHANGED`).
- **Versionen:** ab API 26 (per Design).
- **FIX:** Fuer nicht-exempte Broadcasts Context-registered Receiver (`registerReceiver` zur Laufzeit) nutzen; oder auf WorkManager/JobScheduler umstellen.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions

### 6.7 `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` Pflicht (Android 14)
- **Symptom:** `registerReceiver()` crasht mit `SecurityException` (`One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified …`).
- **Ursache:** Ab Android 14 (API 34) muss bei `registerReceiver` fuer nicht-System-Broadcasts ein Export-Flag angegeben werden.
- **Versionen:** ab API 34 (per Design).
- **FIX:** `ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)` (bzw. `RECEIVER_EXPORTED`, wenn andere Apps senden duerfen sollen).
- **Quelle:** https://developer.android.com/about/versions/14/behavior-changes-14

### 6.8 BOOT_COMPLETED feuert nicht (FLAG_STOPPED / Pre-Unlock / OEM)
- **Symptom:** Nach Reboot laeuft die geplante Arbeit nicht; der Receiver wird nie aufgerufen.
- **Ursache:** Die App muss seit Install/force-stop mindestens einmal manuell gestartet worden sein (`FLAG_STOPPED`); Direct-Boot-Phase vor User-Unlock; manche OEMs (Xiaomi/Huawei) unterdruecken Autostart.
- **Versionen:** per Design (alle).
- **FIX:** `RECEIVE_BOOT_COMPLETED` deklarieren + exported Receiver mit `BOOT_COMPLETED`-Filter; State so bauen, dass die App nach erstem manuellem Start zuverlaessig re-plant; Nutzer ueber OEM-Autostart-Einstellung informieren. (BestJournalAndroid: `BootReminderReceiver` mit `BOOT_COMPLETED|TIMEZONE_CHANGED|TIME_SET`.)
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

### 6.9 Boot-Receiver darf bestimmte FGS-Typen nicht starten (Android 15)
- **Symptom:** FGS-Start aus `ACTION_BOOT_COMPLETED`-Receiver schlaegt fehl.
- **Ursache:** Ab Android 15 (API 35) duerfen aus BOOT_COMPLETED gestartete Receiver bestimmte FGS-Typen (dataSync, camera, mic, mediaPlayback, mediaProjection, phoneCall) nicht mehr starten; Android 16 verschaerft.
- **Versionen:** ab API 35 (per Design).
- **FIX:** Beim Boot stattdessen WorkManager/JobScheduler bzw. AlarmManager nutzen; FGS erst bei zulaessigem Trigger.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/changes

---

## 7. Scoped Storage & Datei-Zugriff

### 7.1 `WRITE_EXTERNAL_STORAGE` ist No-Op ab API 30
- **Symptom:** Schreiben in beliebige externe Pfade schlaegt fehl/wird ignoriert; nur App-Sandbox + MediaStore funktionieren.
- **Ursache:** Scoped Storage (Android 10 opt-in, Android 11/API 30 erzwungen) wertet die Permission nicht mehr.
- **Versionen:** No-Op ab API 30 (per Design).
- **FIX:** App-eigene Dateien → `getExternalFilesDir()`/`filesDir`; geteilte Medien → MediaStore; beliebige Nutzer-Dateien → SAF (`ACTION_CREATE_DOCUMENT`/`ACTION_OPEN_DOCUMENT`). `WRITE_EXTERNAL_STORAGE` mit `android:maxSdkVersion="28"`.
- **Quelle:** https://developer.android.com/about/versions/11/privacy/storage

### 7.2 `MANAGE_EXTERNAL_STORAGE` (All Files Access) — Play-Policy-Falle
- **Symptom:** App-Update von Google Play abgelehnt; „All files access"-Begruendung gefordert.
- **Ursache:** Nur fuer wenige Kategorien erlaubt (Dateimanager, Backup, Antivirus).
- **Versionen:** ab API 30, Policy seit 2021 verschaerft (per Design).
- **FIX:** Nicht verwenden; Use-Case ueber SAF/MediaStore. Wenn unvermeidbar: Kategorie-Nachweis in Play Console.
- **Quelle:** https://developer.android.com/training/data-storage/manage-all-files

### 7.3 `FileUriExposedException` — `file://` in Intent verboten (Android 7)
- **Symptom:** `FileUriExposedException` beim Teilen/Oeffnen einer Datei per Intent.
- **Ursache:** Seit Android 7 (API 24) duerfen keine `file://`-URIs cross-app weitergegeben werden.
- **Versionen:** ab API 24 (per Design).
- **FIX:** `FileProvider.getUriForFile()` → `content://` + `addFlags(FLAG_GRANT_READ_URI_PERMISSION)`; Provider im Manifest (`exported="false"`, `grantUriPermissions="true"`) + `file_paths.xml`. (Beide Apps nutzen `FileProvider`.) Folgefehler: `SecurityException` (Flag vergessen), `FileNotFoundException` (Pfad fehlt in file_paths.xml).
- **Quelle:** https://developer.android.com/reference/androidx/core/content/FileProvider

### 7.4 `EncryptedSharedPreferences` crasht nach Geraetewechsel/Backup-Restore
- **Symptom:** App startet gar nicht mehr, sofortiger Crash in `Activity.onCreate()` (bei Hilt: waehrend `inject`). Stacktrace: `AEADBadTagException` → `KeyStoreException: Signature/MAC verification failed (internal Keystore code: -30)` ueber `AndroidKeystoreAesGcm.decrypt` → `AndroidKeysetManager.build` → `EncryptedSharedPreferences.create`. Android zeigt nur den Systemdialog „App wurde beendet / Cache leeren".
- **Ursache:** Der Tink-Keyset liegt in der SharedPreferences-Datei und wird von Auto-Backup/Smart-Switch aufs neue Geraet mitkopiert. Der zugehoerige Masterkey (`_androidx_security_master_key_`) ist aber **hardwaregebunden im Keystore** und wandert NICHT mit. Der neue Key kann den alten Keyset nicht entschluesseln → MAC-Fehler. Gleicher Effekt bei Keystore-Reset (Bildschirmsperre entfernt, Fingerabdruck geloescht, Werksreset-Teilzustaende).
- **Versionen:** `androidx.security:security-crypto` alle Versionen (1.0.0 – 1.1.0-alpha), API 23+ (per Design; die Bibliothek hat keinerlei eingebautes Recovery).
- **FIX (zwei Schichten, funktionserhaltend):**
  1. **Reaktiv** — `EncryptedSharedPreferences.create()` in `try/catch` kapseln; im Fehlerfall `context.deleteSharedPreferences(<name>)` + `KeyStore.getInstance("AndroidKeyStore").deleteEntry("_androidx_security_master_key_")`, dann neu anlegen. Die alten Werte sind ohnehin unwiederbringlich — verschluesselt bleibt alles, es geht nur der nicht mehr lesbare Inhalt verloren. NIE den Fehler still schlucken: mit `Log.w` samt Stacktrace protokollieren.
  2. **Praeventiv** — die Prefs-Datei aus Backup UND Geraetetransfer ausschliessen: `<exclude domain="sharedpref" path="<name>.xml" />` in `backup_rules.xml` (API ≤30) **und** in beiden Bloecken (`cloud-backup` + `device-transfer`) von `data_extraction_rules.xml` (API 31+). Nur eine der beiden Dateien zu pflegen reicht nicht.
- **Achtung:** Nur `deleteSharedPreferences` ohne Loeschen des Keystore-Alias deckt den Fall „Key selbst beschaedigt" nicht ab. Nach dem Reset ist der Nutzer abgemeldet (Tokens lagen in den Prefs) — das ist der korrekte, ehrliche Zustand, kein Folgebug.
- **Erlebt:** 05.08.2026, BestJournalFrank auf Galaxy Z Fold 8 nach Geraetetransfer.
- **Quelle:** https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences · https://developer.android.com/guide/topics/data/autobackup#include-exclude-android-12

---

## 8. Plattformweite Verhaltensaenderungen Android 15/16 (durch targetSdk-Bump)

> Diese greifen automatisch, sobald targetSdk auf 35/36 steht (beide Apps: 36). Es sind
> **keine Bugs mit Fix-Version**, sondern verpflichtende Migrationen.

### 8.1 Edge-to-Edge erzwungen (Android 15) — Opt-out faellt ab Android 16 ganz weg
- **Symptom:** Inhalt zeichnet unter Status-/Navigationsleiste; ueberlappende UI, abgeschnittene Buttons/oberster Inhalt.
- **Ursache:** targetSdk 35+ → App standardmaessig edge-to-edge, System-Bars transparent. Unter Android 15 noch via `windowOptOutEdgeToEdgeEnforcement=true` abschaltbar — unter **Android 16 deprecated und wirkungslos**.
- **Versionen:** API 35 erzwungen (opt-out moeglich), API 36 opt-out entfaellt.
- **FIX:** WindowInsets behandeln. Compose: `Scaffold` (Material3) bzw. `Modifier.systemBarsPadding()`/`.navigationBarsPadding()`/`.imePadding()`. Views: `ViewCompat.setOnApplyWindowInsetsListener()`. `setStatusBarColor`/`setNavigationBarColor`/`setDecorFitsSystemWindows` sind deprecated/no-op → Faerbung hinter den Insets selbst zeichnen, Icon-Helligkeit via `WindowInsetsControllerCompat`. (Compose-Insets-Details auch in `jetpack-compose.md` §8.)
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.2 Predictive Back — `onBackPressed()` wird ab Android 16 nicht mehr aufgerufen
- **Symptom:** Override von `onBackPressed()` greift nicht; `KEYCODE_BACK` wird nicht dispatched; eigene Back-Logik tot.
- **Ursache:** Fuer targetSdk 36 auf Android-16-Geraeten sind Predictive-Back-Animationen default aktiv. (`onBackPressed` schon ab API 33 deprecated.)
- **Versionen:** opt-in ab API 33, erzwungen API 36. Manifest-Flag `android:enableOnBackInvokedCallback` ab Android 14.
- **FIX:** Auf `OnBackPressedDispatcher` + `OnBackPressedCallback` (AndroidX, rueckwaertskompatibel) migrieren; in Compose `BackHandler`. Temporaeres Opt-out: `android:enableOnBackInvokedCallback="false"`. Back-Logik portieren, nicht entfernen. (Beide Apps haben `enableOnBackInvokedCallback="true"`.)
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.3 16-KB-Page-Size-Pflicht — App startet/crasht nicht auf 16-KB-Geraeten
- **Symptom:** App installiert nicht / crasht beim Start auf 16-KB-Geraeten (Pixel 8/9 mit Dev-Option, kuenftig Standard).
- **Ursache:** Native `.so`-ELF-Segmente nur 4-KB-aligned. Betrifft NUR Apps mit nativem Code (NDK / Drittanbieter-SDKs wie onnx, maps, ML, Bildverarbeitung). Reine Kotlin/Java-Apps nicht. **Relevanz hier: BestJournalAndroid bundelt `sherpa-onnx`-AAR (native libs); EntropieReductor nutzt Maps/Media3.**
- **Versionen:** Play-Pflicht ab 1.11.2025 fuer targetSdk 35+.
- **FIX:** NDK r28+ (default aligned) ODER Linker-Flags `-Wl,-z,max-page-size=16384`; AGP 8.5.1+ (unkomprimierte `.so`); Drittanbieter-SDKs auf 16-KB-Version aktualisieren; `getpagesize()` statt hardcoded 4096. Pruefen: APK Analyzer „Alignment", `zipalign -c -P 16 -v 4 app.apk`, `adb shell getconf PAGE_SIZE` (16384 auf 16-KB-Image).
- **Quelle:** https://developer.android.com/guide/practices/page-sizes

### 8.4 Safer Intents — strengeres Intent-Filter-Matching (Android 16)
- **Symptom:** Explizite Intents an „falsche" Komponente blockiert; Intents ohne Action abgelehnt; Logcat „Intent does not match component's intent filter".
- **Ursache:** Android 16 — explizite Intents muessen dem Intent-Filter der Zielkomponente entsprechen; Intents ohne Action matchen keinen Filter.
- **Versionen:** API 36 (Opt-in-Phase).
- **FIX:** Mit `android:intentMatchingFlags="enforceIntentFilter"` testen (pro Receiver `="none"` opt-out); Intent-Filter an die tatsaechlichen Actions anpassen.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.5 Orientation/Resizability/Aspect-Ratio ignoriert auf grossen Screens (Android 16)
- **Symptom:** Fixe Portrait/Landscape-Layouts werden auf Tablets/Foldables (≥ 600 dp) gedehnt/brechen; unerwartete Rotation, Off-Screen-Komponenten, mehr Recreation → State-Verlust.
- **Ursache:** Android 16 ignoriert auf grossen Screens `screenOrientation`, `resizeableActivity`, `min/maxAspectRatio`, `setRequestedOrientation()`.
- **Versionen:** API 36 auf grossen Screens.
- **FIX:** Adaptive Layouts (`WindowSizeClass`); State sauber retten (ViewModel/SavedStateHandle/`onSaveInstanceState`). Temporaeres Opt-out: `<property android:name="android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" android:value="true"/>` (gilt nicht mehr ab API 37). Ausnahmen: Games (`appCategory="game"`), < 600 dp. **Relevant fuer beide Apps (Fold 6 / Tablet-Nutzung).**
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.6 `Configuration.screenWidthDp/HeightDp` inkludiert jetzt System-Bars (Android 15)
- **Symptom:** Layout-Berechnungen auf Basis von `Configuration`-Groessen werden zu gross.
- **Ursache:** Unter erzwungenem Edge-to-Edge schliesst `Configuration` die System-Bars nicht mehr aus.
- **Versionen:** ab API 35.
- **FIX:** `WindowMetricsCalculator.computeCurrentWindowMetrics().bounds` bzw. WindowInsets statt `Configuration.screenWidthDp/HeightDp`.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.7 `elegantTextHeight`-Default-Wechsel → Text-Clipping (Android 15)
- **Symptom:** Layout-Hoehe aendert sich / Text wird abgeschnitten — besonders Arabisch, Thai, Tamil, Lao, Myanmar, Telugu.
- **Ursache:** API 35 setzt `elegantTextHeight`-Default auf `true` (vorher false); API 36 ignoriert das Attribut ganz.
- **Versionen:** API 35 Default-Wechsel, API 36 Attribut wirkungslos.
- **FIX:** Layouts fuer diese Skripte testen; Clipping bei kursiven Fonts mit `useBoundsForWidth="true"` adressieren. (Beide Apps lokalisiert in viele Sprachen → testen.)
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.8 TLS 1.0/1.1 verboten + Cleartext blockiert (Android 15 / 9)
- **Symptom:** `SSLException`/Connection-Fail bei TLS 1.0/1.1; Cleartext-HTTP blockiert.
- **Ursache:** Android 15 verbietet TLS 1.0/1.1; `usesCleartextTraffic=false` Default ab Android 9; Network Security Config blockiert HTTP.
- **Versionen:** TLS ab API 35; Cleartext-Default ab API 28.
- **FIX:** Backend auf TLS 1.2+; bei noetigem HTTP Network-Security-Config mit `cleartextTrafficPermitted` nur fuer betroffene Domains. (Beide Apps: `usesCleartextTraffic="false"` bzw. eigene `network_security_config`.)
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.9 BODY_SENSORS → granulare `health.*`-Permissions + Local-Network (Android 16)
- **Symptom:** Health-Daten (Heart-Rate) mit altem `BODY_SENSORS` unzugaenglich; LAN/mDNS/Cast-Sockets schlagen fehl (Preview).
- **Ursache:** Android 16 macht `BODY_SENSORS` granular zu `android.permission.health.*`; Local-Network-Zugriff wird `NEARBY_WIFI_DEVICES`-gated (Opt-in-Preview).
- **Versionen:** API 36.
- **FIX:** Granulare Health-Permissions deklarieren + runtime (Privacy-Policy-Activity Pflicht); fuer LAN `NEARBY_WIFI_DEVICES` vorbereiten. (Betrifft EntropieReductors Health-Connect-Pfad.)
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.10 `Arrays.asList(...).toArray()`-Cast → ClassCastException (Android 15 OpenJDK)
- **Symptom:** `ClassCastException` beim Cast von `Arrays.asList(...).toArray()` nach `String[]` (o. ae.).
- **Ursache:** Android 15 aktualisiert OpenJDK-Verhalten: `Arrays.asList(...).toArray()` gibt `Object[]` zurueck, nicht den Element-Typ. Der Cast nach `String[]` crasht.
- **Versionen:** ab API 35 (targetSdk 35+), per Design.
- **FIX:** `toArray(new String[0])` (typisierte Ueberladung) statt `(String[]) ...toArray()`. Best-Practice: §7.8.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.11 `List.removeFirst()/removeLast()` Kotlin-stdlib-Kollision (Android 15 SequencedCollection)
- **Symptom:** Verhaltensaenderung/Build-Warnung oder unerwartetes Ergebnis bei `list.removeFirst()`/`removeLast()` auf Kotlin-`MutableList`.
- **Ursache:** Android 15 fuegt `List` die OpenJDK-`SequencedCollection`-Methoden `removeFirst()`/`removeLast()` hinzu; sie kollidieren mit den gleichnamigen Kotlin-stdlib-Extensions — die Plattform-Methode gewinnt.
- **Versionen:** ab API 35 (targetSdk 35+), per Design.
- **FIX:** `removeAt(0)` / `removeAt(lastIndex)` verwenden. Best-Practice: §7.8.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### 8.12 `scheduleAtFixedRate()` holt nur EINE verpasste Ausfuehrung nach (Android 16)
- **Symptom:** Periodische Logik, die auf das Nachholen ALLER verpassten Ausfuehrungen baut, verhaelt sich nach App-Pause/Doze anders.
- **Ursache:** Ab Android 16 fuehrt `ScheduledThreadPoolExecutor.scheduleAtFixedRate()` hoechstens EINE verpasste Ausfuehrung sofort nach (vorher alle verpassten in schneller Folge).
- **Versionen:** ab API 36, per Design.
- **FIX:** Nicht auf „alle verpassten Laeufe werden nachgeholt" verlassen; fuer garantierte Background-Frequenz WorkManager nutzen. Best-Practice: §5.11.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.13 `MediaStore#getVersion()` ab Android 16 pro App eindeutig
- **Symptom:** Code, der ein bestimmtes Format/einen Vergleich des `MediaStore`-Versions-Strings annimmt (z. B. um Aenderungen zu erkennen), bricht.
- **Ursache:** Ab Android 16 ist `MediaStore.getVersion()` pro App eindeutig — das Format darf nicht mehr appuebergreifend verglichen werden.
- **Versionen:** ab API 36, per Design.
- **FIX:** Den Versions-String nur als opaken Token behandeln (Gleichheits-Check gegen den eigenen zuletzt gespeicherten Wert), kein Format/Parsing annehmen.
- **Quelle:** https://developer.android.com/about/versions/16/behavior-changes-16

### 8.14 Ausblick Android 17 (API 37) — neue Verhaltensaenderungen (Re-Recherche 2026-07-02)
> **Kontext:** compileSdk **37** wird bereits in Projekten benutzt; targetSdk bleibt vorerst 35/36. Diese
> Aenderungen greifen erst bei `targetSdk = 37`, sind aber jetzt zu kennen (einige treffen auch beim
> reinen Kompilieren gegen API 37). Quelle: https://developer.android.com/about/versions/17/behavior-changes-37
- **`static final`-Felder sind jetzt wirklich unveraenderlich** ⭐: Reflection-Schreibzugriff auf `static final`
  wirft `IllegalAccessException`; die JNI-Setter (`SetStaticLongField()` etc.) fuehren zum **App-Crash**. Betrifft
  Libraries/Code, die Konstanten per Reflection/JNI ueberschreiben. → nie `static final` reflektiv setzen.
- **`MessageQueue` lock-frei neu implementiert:** Code, der private `MessageQueue`-Felder/-Methoden **reflektiert**,
  bricht. → keine internen Framework-Felder reflektieren.
- **Local-Network-Permission wird Pflicht** (API 37): LAN/mDNS/Multicast/direkte lokale IP-Zugriffe brauchen die
  neue Local-Network-Runtime-Permission (in Android 16 noch Opt-in-Preview, s. §8.9). → rechtzeitig deklarieren + anfragen.
- **ECH (Encrypted Client Hello) per Default aktiv:** TLS-Handshake nutzt ECH — relevant bei striktem
  Netzwerk-Monitoring/Pinning; normalerweise transparent.
- **App-Memory-Limits (alle Apps auf Android 17):** aggressiveres Speicherbudget; per adb testbar
  (`adb shell am memory-limiter manual|ignore|status`). Speicherlastige Pfade (grosse Bitmaps) pruefen.
- **Grosse Screens:** das temporaere Resizability-Opt-out (`PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`, §8.5)
  **wirkt ab API 37 nicht mehr** — adaptive Layouts sind dann Pflicht.
- **FIX (uebergreifend):** Vor einem targetSdk-37-Bump: Reflection/JNI auf `static final` + interne Framework-Felder
  ausmerzen, Local-Network-Permission-Pfad vorbereiten, adaptive Layouts sicherstellen, Speicherbudget testen.

---

## 9. Fix-Status — was belegt behoben ist und was offen bleibt

> **Methodik-Ehrlichkeit:** `gh`-verifiziert = GitHub-Issue per `gh issue view` am 2026-06-02
> hart geprueft. `issuetracker` = Googles Tracker (`issuetracker.google.com`, login-gated) —
> **nicht** per `gh` pruefbar, daher Status NICHT hart verifiziert (nur aus offiziellen
> Release-Notes/Doku abgeleitet). Im Zweifel gilt ein Eintrag als „offen".

### 9a. Room-Library — gefixt ab Version (Anker: Room 2.7.0)

| Frueherer Bug | gefixt ab | in 2.7.0? | Beleg |
|---------------|-----------|-----------|-------|
| FK-Checks bei destructive migration zu frueh | 2.7.0-alpha06 | ✅ ja | Room Release Notes (b/352085724, issuetracker) |
| KSP2: Schema-Dir nicht gesetzt | 2.7.0-alpha13 | ✅ ja | Room Release Notes (b/379159770, issuetracker) |
| `busy_timeout` fehlte auf Initial-Connection | 2.7.0-rc01 | ✅ ja | Room Release Notes (b/380088809, issuetracker) |
| Flow emittiert nicht neuestes Ergebnis | 2.7.0-rc02 | ✅ ja | Room Release Notes (b/340606803, issuetracker) |
| Connection-Pool-Timeout (Driver) | 2.7.0-rc03 | ✅ ja | Room Release Notes (issuetracker) |
| Fuehrende SQL-Kommentare → Query falsch behandelt | **2.7.2** | ❌ **NEIN** | Room Release Notes (b/413061402, issuetracker) |
| Deadlock Re-Open auto-closed DB aus Flow | **2.8.2** | ❌ **NEIN** | Room Release Notes (b/446643789, issuetracker) |

→ **Konkreter, funktionserhaltender Quick-Win:** Room von **2.7.0 auf ≥ 2.7.2** anheben
(beseitigt den SQL-Kommentar-Bug). Wer `setAutoCloseTimeout` + Flow kombiniert: auf
**≥ 2.8.2**. Alle anderen oben sind in 2.7.0 bereits enthalten.

### 9b. GitHub-Issues — `gh`-hart geprueft (2026-06-02)

| Issue | Status | Bedeutung |
|-------|--------|-----------|
| `google/ksp#1896` „[KSP2] Errors with Room annotations with KSP2" | **OPEN** | KSP2+Room-Annotation-Probleme noch offen → bei KSP1→KSP2-Umstieg vorsichtig (5.15) |
| `android/architecture-components-samples#696` (fallbackToDestructiveMigration-Missverstaendnis) | **CLOSED / COMPLETED** (2019) | historischer Beleg fuer 5.2; kein offener Plattform-Bug |

### 9c. „Noch offen / per Design" (Workaround bleibt aktiv)

Die **gesamten Sektionen 1–4, 6, 7, 8** sind ueberwiegend **per-Design-Verhalten** bzw.
verpflichtende Behavior-Changes (offiziell auf developer.android.com dokumentiert) —
**kein** „gefixt", sondern dauerhaft gueltige Migration. Sie bleiben aktive Eintraege.
Besonders bei targetSdk 36 dauerhaft relevant: FGS-Typen/Permissions (3.1–3.3),
FGS-Timeouts (3.6/3.7), `SCHEDULE_EXACT_ALARM` (6.4), `RECEIVER_EXPORTED` (6.7),
Edge-to-Edge (8.1), Predictive Back (8.2), 16-KB-Page-Size (8.3), Large-Screen-Orientation
(8.5).

### 9d. Ehrlich nicht hart verifiziert

Alle `issuetracker.google.com`-Nummern in diesem Almanach (b/-Nummern + Issue-IDs wie
296460095, 322559122, 306634065, 150325687, 279516894, 195588434, 425569862, 65108694 …)
sind **nicht** per `gh` pruefbar (login-gated). Sie dienen als Beleg-Zeiger; ihr
Open/Closed-Status wurde NICHT hart verifiziert. Die „gefixt ab"-Aussagen in 9a stuetzen
sich auf die offiziellen **Room-Release-Notes** (developer.android.com), nicht auf den
Tracker-Status.

---

## ✅ Pflicht-Checkliste vor Framework-/Runtime-Arbeit

- [ ] **Lifecycle:** Jedes `registerX` hat ein `unregisterX`? Flows mit `repeatOnLifecycle(STARTED)` (in Fragments `viewLifecycleOwner`)? Async-UI mit `isFinishing/isDestroyed`-Guard? Grosse Daten NICHT in `onSaveInstanceState` (1.3), Process-Death via `SavedStateHandle`/Persistenz (1.5/1.6)?
- [ ] **Permissions:** `POST_NOTIFICATIONS` (2.1) + `SCHEDULE_EXACT_ALARM` (6.4) zur Laufzeit/abgefragt? `<queries>` fuer alle externen Provider/Apps (2.4)? permanently-denied-Logik korrekt (2.8)? Health-Permissions vollstaendig im Manifest (2.10)?
- [ ] **FGS:** `foregroundServiceType` (Manifest) + `FOREGROUND_SERVICE_*`-Permission + Typ-Arg in `startForeground()` — alle drei? `startForeground()` sofort in `onStartCommand` (3.5)? dataSync < 6 h + `onTimeout()` (3.6)? `SystemForegroundService`-Typ im Manifest fuer WorkManager (3.3)?
- [ ] **Background:** Worker nach Boot/force-stop re-enqueuen (4.5/4.10)? `getStopReason()` geloggt (4.3)? Kein Verlass auf exaktes Timing (4.6/4.8)? `OutOfQuotaPolicy` gesetzt (4.2)?
- [ ] **Room:** version-Bump ⇒ Migration geschrieben (5.1)? Kein `fallbackToDestructiveMigration` in Prod (5.2)? `exportSchema` + schemaLocation (5.5)? **WAL-Checkpoint vor jedem DB-Backup/Drive-Upload (5.7)**? DB als Singleton (5.13)? DAO off-main (5.10)? Room ≥ 2.7.2 erwogen (9a)?
- [ ] **Intents/Notif:** PendingIntent mit `FLAG_IMMUTABLE/MUTABLE` (6.1)? Notification-Channel erstellt (6.2)? `ContextCompat.registerReceiver` mit Export-Flag (6.7)?
- [ ] **Storage:** Kein direkter externer Pfad-Zugriff — App-Sandbox/MediaStore/SAF (7.1)? `FileProvider` statt `file://` (7.3)?
- [ ] **targetSdk 36:** Edge-to-Edge-Insets behandelt (8.1)? Back auf `OnBackPressedCallback` migriert (8.2)? Native `.so` 16-KB-aligned (8.3)? Adaptive Layout auf grossen Screens (8.5)?
