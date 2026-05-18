# Permission-zu-Feature-Map: Vollstaendige Referenz

## Zweck

Jede deklarierte Android-Permission impliziert mindestens ein Feature. Die App-Roentgen-Schicht 1 nutzt diese Tabelle als Reverse-Map: gegeben eine Permission im Manifest, was ist die wahrscheinlichste Funktionalitaet im Code?

Wenn eine Permission deklariert ist aber das implizierte Feature nicht im Code zu finden ist → "Permission deklariert ohne erkennbares Feature" — entweder tot, oder es gibt ein verstecktes Feature.

## Vollstaendige Permission-zu-Feature-Tabelle

### Standort und Bewegung

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `ACCESS_FINE_LOCATION` | Praezise GPS-Ortung (Fitness-Tracking, Geofencing, Ortsbasierte Suche) | `LocationManager`, `FusedLocationProviderClient`, `requestLocationUpdates` |
| `ACCESS_COARSE_LOCATION` | Netzwerkbasierte Ortung (Stadt-Level, weniger praezise) | gleiche wie oben, mit anderem Provider |
| `ACCESS_BACKGROUND_LOCATION` (Android 10+) | Standort-Tracking im Hintergrund | Foreground-Service oder WorkManager mit Location |
| `FOREGROUND_SERVICE_LOCATION` (Android 14+) | Location-Foreground-Service-Typ | Service mit `foregroundServiceType="location"` |
| `ACTIVITY_RECOGNITION` | Aktivitaets-Erkennung (Gehen, Laufen, Fahren) | `ActivityRecognitionClient` |
| `BODY_SENSORS` | Herzfrequenz, Schritte | `SensorManager.getDefaultSensor(TYPE_HEART_RATE)` |

### Kommunikation und Identitaet

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `READ_CONTACTS` | Kontakt-Import, Sharing zu Kontakten | `ContactsContract`, `ContentResolver.query` |
| `WRITE_CONTACTS` | Kontakt-Anlage/Aenderung | `ContactsContract`, `applyBatch` |
| `READ_PHONE_STATE` | Geraete-ID, Telefonzustand | `TelephonyManager`, `getDeviceId` (deprecated) |
| `READ_PHONE_NUMBERS` (Android 8+) | Eigene Telefonnummer | `TelephonyManager.getLine1Number` |
| `READ_SMS` | SMS-Lesen (z.B. fuer OTP-Auto-Fill) | `Telephony.Sms` ContentProvider |
| `RECEIVE_SMS` | SMS-Empfang ohne lesen aller | `BroadcastReceiver SMS_RECEIVED` |
| `SEND_SMS` | SMS-Versand aus der App | `SmsManager.sendTextMessage` |
| `CALL_PHONE` | Direktes Telefonieren | `Intent.ACTION_CALL` |
| `READ_CALL_LOG` | Anrufprotokoll lesen | `CallLog.Calls` |

### Kamera und Medien

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `CAMERA` | Foto/Video-Aufnahme | `CameraX`, `Camera2 API`, `MediaRecorder`, `Intent.ACTION_IMAGE_CAPTURE` |
| `RECORD_AUDIO` | Voice-Input, Audio-Recording | `AudioRecord`, `MediaRecorder`, `WhisperContext` |
| `READ_EXTERNAL_STORAGE` (Android <13) | Galerie-Zugriff legacy | `MediaStore.Images`, `OpenDocument` |
| `WRITE_EXTERNAL_STORAGE` (Android <11) | Datei-Speichern legacy | `Environment.getExternalStorageDirectory` |
| `READ_MEDIA_IMAGES` (Android 13+) | Galerie-Bilder lesen | `MediaStore.Images` |
| `READ_MEDIA_VIDEO` (Android 13+) | Galerie-Videos lesen | `MediaStore.Video` |
| `READ_MEDIA_AUDIO` (Android 13+) | Audio-Dateien lesen | `MediaStore.Audio` |
| `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+) | Photo Picker mit User-Auswahl | `ActivityResultContracts.PickVisualMedia` |
| `MANAGE_EXTERNAL_STORAGE` | Voller SD-Zugriff (KRITISCH, Play-Policy) | `Environment.isExternalStorageManager` |

### Netzwerk

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `INTERNET` | Alle Cloud-Features (allgemein) | Retrofit, OkHttp, FCM, Firebase, etc. |
| `ACCESS_NETWORK_STATE` | Netzwerk-Status pruefen | `ConnectivityManager` |
| `ACCESS_WIFI_STATE` | WLAN-Info | `WifiManager` |
| `CHANGE_NETWORK_STATE` | Mobile/WLAN-Wechsel | `ConnectivityManager.bindProcessToNetwork` |
| `NEARBY_WIFI_DEVICES` (Android 13+) | WLAN-Scan ohne Standort | `WifiManager.startScan` |

### Bluetooth und NFC

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `BLUETOOTH_CONNECT` (Android 12+) | BT-Geraet verbinden | `BluetoothAdapter`, `BluetoothDevice.connectGatt` |
| `BLUETOOTH_SCAN` (Android 12+) | BT-Geraete-Suche | `BluetoothLeScanner` |
| `BLUETOOTH_ADVERTISE` (Android 12+) | App als BT-Service anbieten | `BluetoothLeAdvertiser` |
| `NFC` | NFC-Tag-Lesen, HCE | `NfcAdapter`, `Tag`, `IsoDep`, `HostApduService` |

### Authentifizierung

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `USE_BIOMETRIC` | Fingerabdruck, Face-Unlock | `BiometricPrompt`, `BiometricManager` |
| `USE_FINGERPRINT` (Android 6-9) | Legacy Fingerabdruck | `FingerprintManager` |
| `MANAGE_ACCOUNTS` | AccountManager-Konten | `AccountManager` |
| `GET_ACCOUNTS` | Konten lesen | `AccountManager.getAccounts` |

### Notifications und Alarme

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `POST_NOTIFICATIONS` (Android 13+) | Benachrichtigungen senden | `NotificationManager.notify` |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notifications anderer Apps lesen | `NotificationListenerService` (DSGVO-KRITISCH) |
| `SCHEDULE_EXACT_ALARM` (Android 12+) | Praezise Alarme | `AlarmManager.setExactAndAllowWhileIdle` |
| `USE_EXACT_ALARM` (Android 13+) | Praezise Alarme ohne User-Approval (Reminder, Calendar, Wecker) | gleich |
| `RECEIVE_BOOT_COMPLETED` | App startet beim Boot | `<receiver>` mit `BOOT_COMPLETED` |
| `WAKE_LOCK` | Geraet wach halten | `PowerManager.WakeLock` |

### Foreground Services und Background

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `FOREGROUND_SERVICE` | Vordergrund-Service erlaubt | `Service.startForeground` |
| `FOREGROUND_SERVICE_*` (Android 14+) | Spezifischer Service-Typ (mediaPlayback, location, dataSync, etc.) | `<service android:foregroundServiceType="...">` |
| `FOREGROUND_SERVICE_MEDIA_PROCESSING` (Android 14, **DEPRECATED in 15**) | Media-Processing-FGS | bei API 35: muss durch `dataSync` oder `specialUse` ersetzt werden |
| `RUN_USER_INITIATED_JOBS` (Android 14+, prominenter ab 15) | User-Initiated Data Transfer Jobs (z.B. grosse Datei-Uploads die der Nutzer aktiv startet) | `JobInfo.Builder.setUserInitiated(true)`, JobScheduler |

### Android 15 (API 35) — neue Permission-Klassen

| Permission | Implizierte Features | Code-Patterns / Notiz |
|-----------|---------------------|----------------------|
| **Photo Picker (kein neues Permission-Pattern, aber API-Wechsel)** | Embedded Photo Picker statt vollem READ_MEDIA_IMAGES | `ActivityResultContracts.PickVisualMedia.PickMultipleVisualMedia`. Apps die READ_MEDIA_IMAGES wegen Galerie-Auswahl deklariert hatten, koennen oft auf Picker umsteigen (Privacy-Improvement). |
| **Partial-Storage statt MANAGE_EXTERNAL_STORAGE** | Apps muessen begruenden warum All-Files-Access noetig ist (verschaerfte Play-Console-Pruefung) | Bei MANAGE_EXTERNAL_STORAGE: Begruendung in Play Console + Datenschutz-Erklaerung Pflicht |
| `MANAGE_OWN_CALLS` (verschaerfter Scope ab API 35) | Nur fuer VoIP-Apps die eigene Audio-Sessions verwalten | `ConnectionService` |
| **Foreground-Service-Typ `specialUse` Restriktion** | Apps die `specialUse` deklarieren MUESSEN seit Android 15 zusaetzlich `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` setzen und Play-Console-Begruendung liefern | im Manifest pruefen + Play-Console-Submission-Form |
| **NFC-Wallet Default-Service** (API 35+) | `BIND_NFC_SERVICE` + Wallet-Role | `HostApduService` mit role="wallet" |

**Audit-Hinweise fuer Android 15:**

- Wenn `compileSdk >= 35`: Pruefen ob `mediaProcessing`-FGS-Typ noch verwendet wird → MIGRATION-Befund
- Wenn `targetSdk >= 35` und MANAGE_EXTERNAL_STORAGE deklariert: Play-Console-Form ausgefuellt? → Befund
- Foreground-Service mit `specialUse`: Property-Tag im Manifest vorhanden? → Befund
- Photo Picker statt READ_MEDIA_IMAGES wo moeglich? → Verbesserungsvorschlag (Privacy-Score)

### Display und Overlays

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `SYSTEM_ALERT_WINDOW` | Overlay-UI ueber andere Apps (Floating-Bubble, Pop-up) | `WindowManager.addView` mit `TYPE_APPLICATION_OVERLAY` |
| `EXPAND_STATUS_BAR` | Status-Bar erweitern | (selten) |

### Sicherheit und Admin

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `BIND_DEVICE_ADMIN` | Geraete-Admin (Wipe, Lock) | `DeviceAdminReceiver` |
| `BIND_ACCESSIBILITY_SERVICE` | Accessibility-Service | `AccessibilityService` (KRITISCH, Play-Policy) |
| `BIND_INPUT_METHOD` | Eigene Tastatur | `InputMethodService` |
| `BIND_VPN_SERVICE` | Eigene VPN-Implementierung | `VpnService` |
| `PACKAGE_USAGE_STATS` | App-Nutzungsdaten | `UsageStatsManager` (Special Access, kein Runtime) |
| `QUERY_ALL_PACKAGES` (Android 11+) | Alle installierten Apps sehen | `PackageManager.getInstalledPackages` (Play-Policy: Begruendung noetig) |
| `REQUEST_INSTALL_PACKAGES` | App-Installation aus eigener App | `Intent.ACTION_INSTALL_PACKAGE` |

### Health Connect

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `android.permission.health.READ_HEART_RATE` | Herzfrequenz lesen | `HealthConnectClient.readRecords<HeartRateRecord>` |
| `android.permission.health.READ_STEPS` | Schritte lesen | `HealthConnectClient.readRecords<StepsRecord>` |
| `android.permission.health.READ_SLEEP` | Schlaf-Daten | `SleepSessionRecord` |
| `android.permission.health.WRITE_*` | Health-Daten schreiben | `HealthConnectClient.insertRecords` |

(Es gibt ueber 50 Health-Connect-Permissions — nur die haeufigsten hier.)

### Werbung

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `com.google.android.gms.permission.AD_ID` | Werbe-ID-Zugriff | AdMob, Audience Network — **KRITISCH wenn App "ad-free" wirbt** |

### In-App-Purchases

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `com.android.vending.BILLING` | Google Play In-App-Purchases | `BillingClient`, alle Subscription-Features |

### Selten oder spezifisch

| Permission | Implizierte Features | Code-Patterns |
|-----------|---------------------|---------------|
| `READ_CALENDAR` | Termine lesen | `CalendarContract.Events` |
| `WRITE_CALENDAR` | Termine schreiben | gleich |
| `READ_LOGS` | Geraete-Logs lesen (NUR Debug, Pre-Install) | `Log.getLogcat` (deprecated) |
| `VIBRATE` | Haptisches Feedback | `Vibrator.vibrate` |
| `FLASHLIGHT` | Taschenlampe | `CameraManager.setTorchMode` |
| `MODIFY_AUDIO_SETTINGS` | Lautstaerke aendern | `AudioManager.setStreamVolume` |
| `CHANGE_WIFI_STATE` | WLAN ein/aus | `WifiManager.setWifiEnabled` |
| `CHANGE_NETWORK_STATE` | Netzwerk-Wechsel | `ConnectivityManager` |
| `INSTANT_APP_FOREGROUND_SERVICE` | Instant-App-Service | (selten) |

## Reverse-Lookup: Feature → Permissions

Wenn der Audit ein Feature im Code findet, welche Permissions sollten deklariert sein?

| Feature | Erwartete Permissions |
|---------|----------------------|
| Voice-Input (Whisper, Speech-to-Text) | `RECORD_AUDIO` |
| Foto-Capture im Eintrag | `CAMERA`, ggf. `WRITE_EXTERNAL_STORAGE` (Android <11) |
| Galerie-Bild auswaehlen | `READ_MEDIA_IMAGES` (Android 13+) ODER `READ_EXTERNAL_STORAGE` (legacy) |
| Push-Notifications | `POST_NOTIFICATIONS` (Android 13+) |
| Daily-Reminder | `POST_NOTIFICATIONS` + `SCHEDULE_EXACT_ALARM` oder `USE_EXACT_ALARM` |
| Cloud-Sync | `INTERNET`, `ACCESS_NETWORK_STATE` |
| Biometrische Sperre | `USE_BIOMETRIC` |
| In-App-Purchases | `com.android.vending.BILLING` |
| Health-Daten-Integration | `android.permission.health.READ_*` (oder WRITE_*) |
| Bluetooth-Geraet (Wearable) | `BLUETOOTH_CONNECT` (Android 12+) |
| App-Widget | (keine Permission noetig, nur AndroidManifest-Receiver) |
| Boot-Auto-Start | `RECEIVE_BOOT_COMPLETED` |
| Foreground-Service (Aufnahme/Sync) | `FOREGROUND_SERVICE` + spezifischer Typ ab Android 14 |

## Audit-Befund-Tabelle

Im Audit fuer jede Permission diese Pruefung:

```markdown
| Permission | Im Code genutzt? | Datei:Zeile | Impliziertes Feature | Status |
|-----------|----------------|-------------|---------------------|--------|
| RECORD_AUDIO | JA | VoiceInputManager.kt:18 | Whisper-Voice | OK |
| AD_ID | JA (transitive) | (AdMob-SDK) | Werbung | KRITISCH wenn App "werbefrei" verspricht |
| READ_PHONE_STATE | NEIN | — | Geraete-ID | Permission tot ODER versteckt |
| ... | ... | ... | ... | ... |
```

## Datenschutz-Pflicht-Mapping (DSGVO)

Bestimmte Permissions zwingen zu Eintraegen in der Datenschutzerklaerung. Im Audit pruefen ob fuer JEDE der folgenden Permissions ein Hinweis in der DS-Erklaerung steht:

```
- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION → Standortdaten
- READ_CONTACTS → Kontakte
- READ_CALENDAR → Kalenderdaten
- CAMERA / RECORD_AUDIO → Kamera/Mikrofon-Aufnahmen
- READ_PHONE_STATE → Geraete-IDs
- BIND_NOTIFICATION_LISTENER_SERVICE → Andere App-Notifications (sehr DSGVO-kritisch)
- BIND_ACCESSIBILITY_SERVICE → Bildschirminhalte (KRITISCH)
- com.google.android.gms.permission.AD_ID → Werbe-IDs
- Health-Connect-Permissions → Gesundheitsdaten (Art. 9 DSGVO besondere Kategorie)
- BODY_SENSORS → Vitalparameter (Art. 9 DSGVO)
```

## Quellen

- [Android Permissions API Reference](https://developer.android.com/reference/android/Manifest.permission)
- [Permissions and APIs that Access Sensitive Information | Play Console Help](https://support.google.com/googleplay/android-developer/answer/9888170)
- [Health Connect Permissions](https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started)
- [Foreground Service Types | Android Developers](https://developer.android.com/about/versions/14/changes/fgs-types-required)
