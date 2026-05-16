# Schicht 1 — AndroidManifest.xml Tiefenanalyse

## Zweck

Der AndroidManifest.xml ist die offizielle "Selbstauskunft" der App gegenueber dem Android-System. Hier wird deklariert was die App darf, welche Komponenten existieren, wie externe Apps mit ihr interagieren koennen. Ohne diese Schicht fehlt die System-Perspektive auf die App.

**Coverage-Beitrag fuer das Gesamt-Inventar: ~40 Prozent** — sehr hoch, weil hier alle System-Schnittstellen sichtbar sind.

## Pfad zur Hauptdatei

```
app/src/main/AndroidManifest.xml
```

Achtung: Es kann mehrere Manifests geben — pro Build-Variant (debug, release, staging) und pro Library-Modul. Alle muessen geprueft werden:

```bash
find . -name AndroidManifest.xml -not -path '*/build/*'
```

## Was extrahiert werden muss

### 1.1 Permissions (uses-permission)

```bash
grep -E "uses-permission android:name" AndroidManifest.xml | grep -o 'android.permission.[A-Z_]*' | sort -u
```

Jede Permission impliziert mindestens ein Feature. Volle Mapping-Tabelle in `permission-feature-map.md`.

**Kritische Permissions die im Audit besonders auffallen muessen:**
- `CAMERA` → App kann Fotos/Videos aufnehmen
- `RECORD_AUDIO` → App kann Audio aufnehmen (Voice-Input, Aufnahme-Feature)
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` → Standort-Features
- `READ_CONTACTS` / `WRITE_CONTACTS` → Kontakt-Integration
- `READ_MEDIA_*` (Android 13+) → Galerie-Zugriff
- `POST_NOTIFICATIONS` (Android 13+) → Push-Notification-System
- `USE_BIOMETRIC` → Fingerabdruck/Face-Unlock
- `BILLING` (com.android.vending.BILLING) → In-App-Purchases / Paywall
- `FOREGROUND_SERVICE` → Aufnahme/Sync laeuft im Vordergrund
- `SCHEDULE_EXACT_ALARM` → Praezise Reminder/Timer
- `BIND_NOTIFICATION_LISTENER_SERVICE` → Liest Notifications anderer Apps (DSGVO-relevant!)
- `RECEIVE_BOOT_COMPLETED` → App startet automatisch beim Booten
- `SYSTEM_ALERT_WINDOW` → Overlay-Feature
- `PACKAGE_USAGE_STATS` → Misst Nutzung anderer Apps
- `HEALTH_CONNECT` (READ/WRITE) → Health-Daten-Integration

**Nachweis-Pflicht:** Fuer JEDE deklarierte Permission im Audit-Bericht angeben:
- Welcher Code-Pfad nutzt sie (Grep-Beleg)
- Welches Feature wird damit umgesetzt
- Wenn keine Code-Nutzung gefunden: "Permission deklariert aber im Code nicht verwendet — verdaechtig oder tot"

### 1.2 Activities

```bash
grep -B1 -A8 '<activity' AndroidManifest.xml | grep -E 'android:name|intent-filter|action android:name|category android:name|data android:'
```

Pro Activity dokumentieren:
- `android:name` — voll-qualifizierter Klassenname
- `android:exported` — true bedeutet von aussen aufrufbar
- `intent-filter` — welche Aktionen werden behandelt
- `data` (in intent-filter) — welche Schemes/Hosts/Pfade

**Besonders wichtig — Entry-Points:**
- `android.intent.action.MAIN` + `android.intent.category.LAUNCHER` → der App-Icon-Start
- `android.intent.action.VIEW` mit `data android:scheme="https"` → Deep-Links von Webseiten
- `android.intent.action.SEND` mit `mimeType` → empfaengt geteilte Inhalte
- `android.intent.action.PROCESS_TEXT` → Text-Selection-Sharing-Feature

### 1.3 Services

```bash
grep -B1 -A6 '<service' AndroidManifest.xml
```

Typische Service-Typen die im Audit erkennbar werden:
- `FirebaseMessagingService` → Push-Notification-Empfang
- `FirebaseInstanceIdService` (alt) / `MessagingService` → FCM-Integration
- `MediaSessionService` → Hintergrund-Audio-Wiedergabe
- `MediaBrowserServiceCompat` → Auto/Wear-Integration
- `TileService` → Quick-Settings-Tile
- `AccessibilityService` → Accessibility-Feature (sehr maechtig, DSGVO-kritisch)
- `NotificationListenerService` → Liest Notifications anderer Apps
- `JobIntentService` → Background-Tasks (legacy)
- Custom Foreground Services (z.B. fuer Timer, Aufnahme, GPS-Tracking)

### 1.4 BroadcastReceiver

```bash
grep -B1 -A6 '<receiver' AndroidManifest.xml
```

Reagiert auf System-Events. Typische Patterns:
- `android.intent.action.BOOT_COMPLETED` → App startet beim Boot (Auto-Start-Feature)
- `android.intent.action.PACKAGE_REPLACED` → reagiert auf eigenes Update
- `android.appwidget.action.APPWIDGET_UPDATE` → Widget-Provider
- `android.intent.action.LOCALE_CHANGED` → reagiert auf Sprach-Wechsel
- `android.net.conn.CONNECTIVITY_CHANGE` → Netzwerk-Wechsel-Reaktion
- `android.intent.action.TIMEZONE_CHANGED` → Zeitzonen-Reaktion

### 1.5 ContentProvider

```bash
grep -B1 -A6 '<provider' AndroidManifest.xml
```

ContentProvider sind Daten-Schnittstellen zu anderen Apps:
- `androidx.core.content.FileProvider` → Dateifreigabe an andere Apps (Sharing-Feature)
- Custom-Provider → App teilt eigene Daten mit anderen Apps (z.B. fuer Auto-Backup)
- `WorkManagerInitializer` (`androidx.startup.InitializationProvider`) → WorkManager-Boot

### 1.6 Deep-Links und URL-Schemes

Tiefere Suche im Intent-Filter-Bereich:

```bash
grep -A5 '<intent-filter' AndroidManifest.xml | grep -E 'data android:scheme|data android:host|data android:pathPrefix'
```

Pro Deep-Link dokumentieren:
- Scheme (https, custom-scheme)
- Host (z.B. bestjournal.app)
- Path-Prefix (z.B. /entry, /share)
- Welche Activity es behandelt

### 1.7 Backup-Konfiguration

```xml
<application
    android:allowBackup="true|false"
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules">
```

Wenn `allowBackup="true"`: Pruefe `res/xml/backup_rules.xml` und `res/xml/data_extraction_rules.xml` auf Inhalte.

```bash
cat app/src/main/res/xml/backup_rules.xml 2>/dev/null
cat app/src/main/res/xml/data_extraction_rules.xml 2>/dev/null
```

DSGVO-Aspekt: Werden personenbezogene Daten ins Cloud-Backup geschrieben?

### 1.8 Network Security Config

```bash
cat app/src/main/res/xml/network_security_config.xml 2>/dev/null
```

Wenn vorhanden: Werden custom Domains gepinnt? Cleartext erlaubt?

### 1.9 Meta-Data Tags

```bash
grep -B1 -A2 '<meta-data' AndroidManifest.xml
```

Hier verstecken sich oft API-Keys, Firebase-IDs, Ad-SDK-Konfigurationen, Library-Versionen. Pro Meta-Data dokumentieren wozu der Wert gehoert.

### 1.10 App-Shortcuts

```bash
grep -A2 'meta-data android:name="android.app.shortcuts"' AndroidManifest.xml
cat app/src/main/res/xml/shortcuts.xml 2>/dev/null
```

Long-Press auf App-Icon kann Shortcuts zeigen — diese sind eigene "Mini-Entry-Points" die im Audit oft fehlen.

## Output-Format fuer Schicht 1

```markdown
## Schicht 1 — Manifest-Analyse

### Permissions (N deklariert)

| Permission | Code-Nutzung (Datei:Zeile) | Impliziertes Feature | Status |
|-----------|--------------------------|---------------------|--------|
| CAMERA | CameraScreen.kt:42 | Foto-Capture in Eintrag | aktiv |
| RECORD_AUDIO | VoiceInputManager.kt:18 | Whisper-Voice-Input | aktiv |
| BILLING | BillingManager.kt:22 | Premium-Subscription | aktiv |
| ... | ... | ... | ... |

### Activities (N deklariert)

| Activity | Exported | Intent-Filter | Zweck |
|----------|----------|--------------|-------|
| MainActivity | true | LAUNCHER | App-Hauptstart |
| ShareReceiverActivity | true | SEND text/plain | Empfaengt geteilten Text |
| ... | ... | ... | ... |

### Services (N deklariert)

| Service | Typ | Zweck |
|---------|-----|-------|
| FirebaseMessagingService | Push | FCM-Notifications |
| ... | ... | ... |

### BroadcastReceiver (N deklariert)

| Receiver | Triggers | Zweck |
|----------|---------|-------|
| BootReceiver | BOOT_COMPLETED | Auto-Start fuer Reminder |
| ... | ... | ... |

### Deep-Links

| Scheme | Host | Path | Ziel-Activity |
|--------|------|------|--------------|
| https | bestjournal.app | /share | ShareReceiver |
| ... | ... | ... | ... |

### Backup-Konfiguration

- allowBackup: true/false
- fullBackupContent: ...
- DSGVO-Bewertung: ...

### Meta-Data (relevante Eintraege)

| Name | Wert | Zweck |
|------|------|-------|
| com.google.firebase.messaging.default_notification_channel_id | default | FCM-Default-Channel |
| ... | ... | ... |

### Quervergleich Permissions vs. Code

Permissions die deklariert sind aber nicht im Code verwendet werden:
- (keine — alle aktiv) ODER
- PERMISSION_X — kein Code-Pfad gefunden, verdaechtig

Permissions die im Code requested werden aber nicht im Manifest stehen:
- (sollte 0 sein, sonst Build-Fehler)
```

## Typische Fehlerquellen in dieser Schicht

- **Mehrere Manifests vergessen**: Wenn nur das `main`-Manifest geprueft wird, werden Permission-Unterschiede zwischen `debug` und `release` uebersehen.
- **Manifest-Merger-Ergebnis nicht geprueft**: Bibliotheken bringen eigene Permissions mit. Echtes finales Manifest nach Build:
  ```
  app/build/intermediates/merged_manifests/release/AndroidManifest.xml
  ```
- **Tools-Attribute uebersehen**: `tools:remove` und `tools:replace` aendern was im finalen Manifest landet.
- **Implicit Permissions vergessen**: Bestimmte Bibliotheken fuegen Permissions per Manifest-Merger hinzu (z.B. AdMob fuegt INTERNET, ACCESS_NETWORK_STATE, AD_ID hinzu).
