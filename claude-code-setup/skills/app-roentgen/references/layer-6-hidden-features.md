# Schicht 6 — Hidden Features aufdecken

> **FIX AA6 (Audit 10) — Kotlin + Java:** Patterns mit `--include='*.kt'` muessen bei Java-Hybrid-Apps um `--include='*.java'` ergaenzt werden (Java-WorkManager-Worker, Java-AppWidgetProvider, Java-AccessibilityService, Java-NotificationListenerService). Reine Kotlin-Apps koennen den Java-Filter weglassen.

## Zweck

Hidden Features sind Funktionen die nicht offensichtlich aus der Top-Level-UI zu erkennen sind. Sie sind oft die Quelle von Audit-Luecken: Werbeaussagen sagen "wir haben Feature X nicht" — aber ein Feature-Flag, ein Debug-Menue oder ein Background-Job tut genau das. Im Audit duerfen sie nicht uebersehen werden.

**Coverage-Beitrag fuer Vollstaendigkeit: 100% — diese Schicht macht den Unterschied zwischen 95% und 100% Coverage.**

## 6.1 Background-Jobs (WorkManager)

```bash
grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' . | grep -v 'test/'
grep -rn 'OneTimeWorkRequest\|PeriodicWorkRequest\|enqueue\|enqueueUniqueWork' --include='*.kt' .
grep -rn 'WorkManager\.getInstance\|WorkRequest\.Builder' --include='*.kt' .
```

Pro Worker im Audit:

| Worker | Trigger | Periodic? | Constraints | Was er tut | Network-Verbindungen |
|--------|---------|-----------|------------|-----------|---------------------|
| DailyReminderWorker | OneTime nach onCreate | Periodic 24h | None | Notification senden | Nein |
| CloudBackupWorker | Manuell + Periodic 6h | Periodic | Network UNMETERED, Charging | Eintraege ins Cloud-Backup hochladen | Firestore |
| AnalyticsFlushWorker | Periodic | Periodic 4h | Network ANY | Events an Firebase senden | Firebase Analytics |
| ... | ... | ... | ... | ... | ... |

**Audit-Hinweis:** Workers laufen oft im Hintergrund OHNE dass der Nutzer es weiss. Wenn ein Worker Daten ins Internet sendet, MUSS das in der Datenschutzerklaerung stehen.

## 6.2 Widgets (App-Widget)

```bash
grep -rln 'class.*: AppWidgetProvider\|extends AppWidgetProvider' --include='*.kt' --include='*.java' .
grep -rn 'AppWidgetManager\|RemoteViews' --include='*.kt' .
find . -path '*/res/xml/*widget*.xml' -o -name '*widget_info.xml'
```

Widgets sind eigene Mini-Bildschirme auf dem Home-Screen. Sie sind oft separate Entry-Points die im Audit fehlen.

Pro Widget dokumentieren:
- Widget-Name (in widget_info.xml)
- Layout (welches XML-File)
- Update-Frequenz (`updatePeriodMillis`)
- Klick-Aktionen (PendingIntent → wohin)
- Konfigurierbar? (`configure`-Activity)

## 6.3 Quick-Settings-Tile

```bash
grep -rln 'class.*: TileService' --include='*.kt' .
grep -rn 'qsTile\|onTileAdded\|onClick' --include='*.kt' . | grep -i tile
```

Nutzer kann eine eigene App als Tile in den Quick-Settings hinzufuegen. Wenn vorhanden — wichtig fuer Audit, weil das ein nicht-UI-Pfad ist.

## 6.4 App-Shortcuts (Long-Press auf Icon)

```bash
grep -rln 'ShortcutManager\|ShortcutInfo\|ShortcutInfoCompat' --include='*.kt' .
grep -rn 'pushDynamicShortcut\|setDynamicShortcuts\|reportShortcutUsed' --include='*.kt' .
find . -name 'shortcuts.xml' -path '*/res/xml/*'
```

Pro Shortcut: Was ist die Aktion, welche Activity wird gestartet, welcher Pfad?

## 6.5 Notification-Channels

```bash
grep -rn 'NotificationChannel\|NotificationChannelCompat\|createNotificationChannel\|createNotificationChannels' --include='*.kt' .
```

Jeder Channel ist eine Feature-Gruppe. Im Audit auflisten:
- Channel-ID
- Channel-Name (User-sichtbar)
- Channel-Description (User-sichtbar)
- Importance-Level (HIGH erlaubt Heads-Up, LOW = silent)
- Beispiel-Notifications die in diesem Channel gesendet werden

## 6.6 Accessibility Service

```bash
grep -rln 'class.*: AccessibilityService' --include='*.kt' --include='*.java'
find . -name '*accessibility_config*' -path '*/res/xml/*'
```

**SEHR KRITISCH** — Accessibility Services koennen jede UI lesen und Aktionen ausfuehren. Wenn die App einen hat, MUSS:
- In der Datenschutzerklaerung erwaehnt werden
- Google Play Policy: Spezielle Genehmigung erforderlich
- Im Audit als hochrangiger Befund

## 6.7 Print-Adapter

```bash
grep -rn 'PrintDocumentAdapter\|PrintManager\|PrintAttributes' --include='*.kt' .
```

App kann drucken? Im Audit dokumentieren wo der Druck-Button ist und was gedruckt wird.

## 6.8 NFC

```bash
grep -rn 'NfcAdapter\|NdefRecord\|NdefMessage\|IsoDep\|Tag\b' --include='*.kt' .
grep -rn 'HostApduService' --include='*.kt' .
```

NFC-Lesen oder NFC-Card-Emulation. Im Audit als eigene Feature-Kategorie.

## 6.9 Boot-Receiver / Auto-Start

```bash
grep -rln 'BroadcastReceiver' --include='*.kt' . | xargs grep -l 'BOOT_COMPLETED' 2>/dev/null
grep -rn 'BOOT_COMPLETED\|RECEIVE_BOOT_COMPLETED' --include='*.kt' --include='AndroidManifest.xml' .
```

Wenn die App automatisch beim Boot startet — kritisch fuer "App laeuft nur wenn ich sie nutze"-Werbeaussagen.

## 6.10 Feature-Flags und Remote Config

```bash
grep -rn 'FirebaseRemoteConfig\|remoteConfig\b\|remoteConfig\.' --include='*.kt' .
grep -rn 'getBoolean\|getString\|getDouble\|getLong' --include='*.kt' . | head -50
grep -rn 'BuildConfig\.\|BUILD_TYPE\|FLAVOR\b' --include='*.kt' .
grep -rn 'isFeatureEnabled\|featureFlag\|FeatureToggle' --include='*.kt' . -i
```

Feature-Flags zeigen Features die:
- Geplant sind aber noch nicht aktiv
- A/B-Tests sind (nur fuer N% der Nutzer aktiv)
- Per Server an/abgeschaltet werden koennen

Im Audit fuer JEDEN Flag dokumentieren:
- Default-Wert
- Wo wird er abgefragt
- Welche Funktionalitaet schaltet er an/ab

## 6.11 Debug-Menus / Long-Click-Trigger

```bash
grep -rn 'setOnLongClickListener\|onLongClick\|combinedClickable' --include='*.kt' . -A 5
grep -rn 'BuildConfig.DEBUG' --include='*.kt' . -A 5
grep -rn 'debugMenu\|DebugScreen\|InternalSettings\|HiddenSettings' --include='*.kt' . -i
```

Klassische Verstecke:
- 7x auf das Logo tippen → Debug-Menu
- Long-Press auf Versionsnummer → Internal-Tools
- Geheimer Code im Settings-Suchfeld

Wenn vorhanden — im Audit als "Hidden Feature" dokumentieren. Falls Werbung "schlanke App ohne Debug-Sachen" verspricht, ist das ein Befund.

## 6.12 A/B-Tests

```bash
grep -rn 'experiment\|variant\|cohort\|abTest\|A_B\|treatment' --include='*.kt' . -i
grep -rn 'getExperimentVariant\|isInExperiment' --include='*.kt' .
```

Wenn die App A/B-Tests fuer Nutzer macht (nicht alle sehen das Gleiche) — im Audit als "User-Subgruppen sehen unterschiedliche Inhalte" dokumentieren. Werbung muss den moeglichen Variationen entsprechen.

## 6.13 Account-Deletion (DSGVO-Pflicht)

```bash
grep -rn 'deleteAccount\|removeUser\|clearAllData\|gdprDelete\|userDeletion' --include='*.kt' . -i
grep -rn 'DELETE_ACCOUNT\|account_delete' --include='*.kt' .
```

Google Play Policy + DSGVO Art. 17: Pflicht fuer Apps mit User-Konten. Wenn fehlt — KRITISCHER Befund.

Audit pruefen:
- Gibt es einen Account-Loeschen-Button in der App?
- Gibt es eine separate Web-URL fuer Account-Loeschung (Pflicht laut Google Play seit 2024)?
- Werden alle Daten geloescht oder nur deaktiviert?
- Gibt es eine Bestaetigung mit Hinweis auf Unwiderruflichkeit?

## 6.14 Backup-Logik (Custom)

```bash
grep -rln 'BackupAgent\|BackupAgentHelper' --include='*.kt' --include='*.java'
grep -rn 'onBackup\|onRestore' --include='*.kt' --include='*.java'
find . -name 'backup_rules.xml' -path '*/res/xml/*'
find . -name 'data_extraction_rules.xml' -path '*/res/xml/*'
```

Wenn die App ein eigenes Backup-System hat (z.B. Google Drive, Dropbox), im Audit dokumentieren:
- Wohin wird gesichert
- Was wird gesichert (Eintraege? Mediendateien? Settings?)
- Verschluesselt? (Datenschutz!)
- User-initiated oder automatisch?

## 6.15 Sharing-Empfaenger (intern)

```bash
grep -rn 'ACTION_SEND\|ACTION_SEND_MULTIPLE\|ACTION_PROCESS_TEXT' --include='*.kt' --include='AndroidManifest.xml' .
```

App empfaengt geteilte Inhalte? Welche MIME-Types? Was passiert mit den Daten? Im Audit als externer Entry-Point.

## 6.16 In-App-Review-Trigger

```bash
grep -rn 'ReviewManager\|ReviewManagerFactory\|requestReviewFlow\|launchReviewFlow' --include='*.kt' .
```

Wo wird der In-App-Review-Dialog ausgeloest? (Nicht zu oft, nicht in Fehler-Situationen, sonst Play-Policy-Verstoss)

## 6.17 Foreground Services (was laeuft im Vordergrund)

```bash
grep -rln 'class.*: Service\b\|extends Service' --include='*.kt' --include='*.java'
grep -rn 'startForeground\|startForegroundService' --include='*.kt' .
grep -rn 'foregroundServiceType' --include='AndroidManifest.xml' .
```

Foreground Services laufen mit fester Notification (User sieht "App ist aktiv"). Typen ab Android 14:
- mediaPlayback
- location
- dataSync
- camera, microphone, mediaProjection
- specialUse

Pro Service: Wann wird er gestartet, wann gestoppt, was sieht der User?

## 6.18 Dynamic Feature Modules

```bash
grep -rn 'com.android.dynamic-feature\|DynamicInstallManager\|SplitInstallManager' --include='*.kt' --include='*.gradle*' .
```

Wenn die App Module on-demand nachlaedt (Play Asset Delivery, Dynamic Feature Modules) — im Audit als "diese Features werden erst spaeter geladen" dokumentieren.

## 6.19 Health Connect Integration

```bash
grep -rn 'HealthConnectClient\|readRecords\|insertRecords\|HealthPermission' --include='*.kt' .
```

Welche Health-Daten werden gelesen/geschrieben? Im Audit + DSGVO Art. 9 (besondere Kategorie personenbezogener Daten) auffuehren.

## 6.20 Credential Manager (Passkeys)

```bash
grep -rn 'CredentialManager\|GetPasswordOption\|CreatePasswordRequest\|GetPasskeyOption' --include='*.kt' .
```

Wenn vorhanden — moderne Authentifizierung. Im Audit dokumentieren.

## 6.21 In-App-Updates

```bash
grep -rn 'AppUpdateManager\|AppUpdateInfo\|completeUpdate' --include='*.kt' .
```

App kann sich selbst aktualisieren? Welcher Modus (FLEXIBLE oder IMMEDIATE)?

## 6.22 Strings die nur in Premium / Debug erscheinen

```bash
grep -E 'name="(premium|pro|unlock|upgrade|debug|internal|test|hidden)' app/src/main/res/values/strings.xml -i
```

Diese Strings deuten auf Premium- oder Debug-Features hin die in normalen Builds nicht sichtbar sind. Im Audit pruefen ob sie im Release-Build erreichbar sind.

## 6.23 Dynamische Strings (in Code generiert)

```bash
grep -rn 'String\.format\|stringResource.*formatArgs\|"%s.*%d\|context\.getString.*formatArgs' --include='*.kt' .
```

Wenn Werbeaussagen mit String.format zusammengesetzt werden, sind sie schwer zu auditieren. Im Bericht alle dynamischen Werbestrings auflisten.

## Output-Format fuer Schicht 6

```markdown
## Schicht 6 — Hidden Features

### Background-Jobs (WorkManager)
[Tabelle aus 6.1]

### Widgets (App-Widget)
[Tabelle pro Widget]

### Quick-Settings-Tile
[Tabelle pro Tile]

### App-Shortcuts (Long-Press auf Icon)
[Liste der Shortcuts]

### Notification-Channels
[Tabelle aller Channels]

### Accessibility Service
- Vorhanden: ja/nein
- Falls ja: Detail + KRITISCHER Befund-Block

### Print / NFC / Boot-Auto-Start
[Status pro Feature]

### Feature-Flags / Remote Config
[Tabelle aller Flags mit Defaults]

### Debug-Menus / Long-Click-Trigger
[Liste aller Triggers]

### A/B-Tests
[Liste aller laufenden Experimente]

### Account-Deletion (DSGVO-Pflicht)
- In-App-Loeschung: gefunden in DeleteAccountScreen.kt:25 ODER NICHT IMPLEMENTIERT
- Web-URL: erforderlich seit 2024 — VORHANDEN/FEHLT
- Loescht alle Daten: ja/nein
- Bestaetigung mit Unwiderruflichkeitshinweis: ja/nein

### Backup-Logik
[Detail wie in 6.14]

### Sharing-Empfaenger
[Liste der MIME-Types und Behandlung]

### Foreground Services
[Tabelle pro Service]

### Dynamic Feature Modules
[Liste der nachladbaren Module]

### Health Connect / Credential Manager / In-App-Updates
[Status pro Bereich]

### Premium-/Debug-Only-Strings
[Liste der verdaechtigen Strings]

### Audit-Befunde Schicht 6

| # | Befund | Risiko | Datei |
|---|--------|--------|-------|
| 1 | Account-Deletion-URL fehlt (Google Play Policy 2024) | KRITISCH | — |
| 2 | Boot-Auto-Start aktiv aber nicht in Privacy-Policy erwaehnt | HOCH | AndroidManifest.xml |
| ... | ... | ... | ... |
```

## Typische Fehlerquellen

- **Worker-Klassen in Library-Modulen vergessen**: Multi-Module-Apps haben Worker oft in `data/` oder `core/` Modulen.
- **Receiver via `<receiver>`-Tag versus dynamisch via `registerReceiver`**: Beide muessen gefunden werden.
- **Implicit Intents als Hidden-Entry-Points**: Wenn die App ein `<intent-filter>` mit ACTION_SEND hat ist sie ein Sharing-Ziel — oft nicht in der UI sichtbar.
- **Compose-spezifische Side-Effects**: `LaunchedEffect` mit `WorkManager.getInstance().enqueue` ist eine versteckte Aktion die beim Render passiert.
- **Library-Worker**: Crashlytics, Firebase Performance bringen ihre eigenen Worker mit — die laufen still im Hintergrund.
