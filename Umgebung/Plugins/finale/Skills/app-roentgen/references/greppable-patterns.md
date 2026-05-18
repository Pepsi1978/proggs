# Greppable-Patterns: Werkzeugkasten fuer systematische Suchen

## Zweck

Diese Datei sammelt ALLE Greppable-Patterns aus den Schichten 1-7 an einer Stelle. Sie ist der Werkzeugkasten — wenn ein bestimmter Bereich noch tiefer untersucht werden soll, hier nachschauen.

## Plattform-Hinweise (PFLICHT lesen vor Copy-Paste)

> **FIX U5 — Bash-only-Hinweis:** Alle Patterns hier sind fuer **POSIX-Shells** geschrieben (Bash, Zsh, Git Bash auf Windows, macOS Terminal, Linux). Sie nutzen `grep`, `find`, `awk`, `sed`, `xargs` und temporaere Dateien unter `/tmp/`.
>
> **PowerShell / CMD funktioniert NICHT direkt** — wenn du die Patterns auf Windows in PowerShell ausfuehren willst, brauchst du entweder Git Bash (empfohlen) oder PowerShell-Aequivalente (`Select-String` statt `grep`, `Get-ChildItem -Recurse` statt `find`, `$env:TEMP` statt `/tmp/`).
>
> **Temp-Pfade:** `/tmp/` ist Bash-Default. Plattformneutral: `${TMPDIR:-/tmp}` (Bash) oder `$env:TEMP` (PowerShell).
>
> **Multi-Module:** Patterns mit `app/src/main/...` decken nur Single-Module-Apps ab. Bei Multi-Module-Apps siehe die Helper-Funktionen in `scripts/feature-scan.sh` (`find_default_strings_xml`, `find_translated_strings_xml`, `find_locale_dirs`, `find_module_roots`, `find_en_strings_xml`, `find_regional_locales`, `find_locale_strings_xml`).
>
> **FIX Y1 (Audit 8) — Kotlin + Java:** Die Patterns in dieser Datei zeigen aus Lesbarkeitsgruenden nur `--include='*.kt'` bzw. `--type kotlin`. Wenn die App auch **Java-Code** enthaelt (Legacy-Module, alte SDKs wie Firebase-Java, RevenueCat-Java, Apollo-Java), MUSS bei jeder Pattern-Anwendung `--include='*.java'` bzw. `--type java` ergaenzt werden:
>
> ```bash
> # Statt:  grep -rn 'pattern' --include='*.kt' .
> # Java+Kotlin: grep -rn 'pattern' --include='*.kt' --include='*.java' .
>
> # Statt:  rg 'pattern' --type kotlin
> # Java+Kotlin: rg 'pattern' --type kotlin --type java
> ```
>
> Das `feature-scan.sh` Skript hat die Helper-Funktionen `GREP_R` und `count_grep` bereits auf Kotlin+Java umgestellt — bei manuellen Aufrufen aus dieser Datei muss man es selbst ergaenzen. Wenn die App AUSSCHLIESSLICH Kotlin nutzt (kein Java), darf der Java-Filter weggelassen werden.

Alle Patterns sind fuer **ripgrep (rg)** oder **grep** auf einem POSIX-Shell formatiert. Auf Windows Git Bash funktionieren sie identisch.

## Generelle Tipps

```bash
# ripgrep ist schneller und respektiert .gitignore automatisch
rg "pattern" --type kotlin

# grep mit Datei-Typ:
grep -rn "pattern" --include='*.kt' .

# Wichtige Flags:
-l       # nur Dateinamen
-n       # mit Zeilennummern
-A 5     # 5 Zeilen nach Treffer
-B 2     # 2 Zeilen vor Treffer
-r       # rekursiv
-i       # case-insensitive
--include='*.kt'   # Datei-Filter (grep)
--type kotlin       # Datei-Filter (rg)
```

Ausschluss von Test-/Build-Verzeichnissen meistens noetig:
```bash
... | grep -v 'test/' | grep -v 'build/' | grep -v 'androidTest/'
```

## Schicht 1 — Manifest

```bash
# Permissions extrahieren
grep -oE 'android.permission.[A-Z_]+' AndroidManifest.xml | sort -u

# Activities mit Intent-Filtern
grep -B1 -A8 '<activity' AndroidManifest.xml | grep -E 'android:name|intent-filter|action android:name|data android:'

# Services
grep -B1 -A6 '<service' AndroidManifest.xml

# BroadcastReceiver
grep -B1 -A6 '<receiver' AndroidManifest.xml

# ContentProvider
grep -B1 -A6 '<provider' AndroidManifest.xml

# Deep-Links extrahieren
grep -A5 '<intent-filter' AndroidManifest.xml | grep -E 'data android:scheme|data android:host|data android:pathPrefix'

# Meta-Data Tags
grep -B1 -A2 '<meta-data' AndroidManifest.xml

# Mehrere Manifest-Varianten finden
find . -name AndroidManifest.xml -not -path '*/build/*'

# Final-merged Manifest nach Build
ls app/build/intermediates/merged_manifests/release/AndroidManifest.xml 2>/dev/null
```

## Schicht 2 — Dependencies

```bash
# Alle implementations
grep -E '^\s+(implementation|api|kapt|ksp|debugImplementation|releaseImplementation)\s' \
  $(find . -name 'build.gradle.kts' -not -path '*/build/*')

# Version-Catalog
grep -E '^[a-zA-Z]' gradle/libs.versions.toml 2>/dev/null

# Plugins
grep -E 'apply\(|alias\(|id\("' app/build.gradle.kts | head -20

# Build-Variants und Flavors
grep -A30 'productFlavors' app/build.gradle.kts
grep -A10 'buildTypes' app/build.gradle.kts
```

## Schicht 3 — Architektur

```bash
# ViewModels
grep -rln '@HiltViewModel' --include='*.kt' . | sort
grep -rn 'class.*ViewModel\(' --include='*.kt' . | grep -v 'test/'

# Repositories
grep -rln 'class.*Repository\|interface.*Repository' --include='*.kt' .

# UseCases
grep -rln 'class.*UseCase\|class.*Interactor' --include='*.kt' .

# Hilt-Module
grep -rln '@Module' --include='*.kt' .
grep -rn '@Provides\|@Binds' --include='*.kt' . | head -100

# Room
grep -rln '@Entity\b' --include='*.kt' .
grep -rln '@Database\b' --include='*.kt' .
grep -rln '@Dao\b' --include='*.kt' .
grep -rn '@Query\|@Insert\|@Delete\|@Update' --include='*.kt' .

# Workers
grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' .
grep -rn 'OneTimeWorkRequest\|PeriodicWorkRequest' --include='*.kt' .

# Sealed Classes
grep -rn 'sealed class\|sealed interface' --include='*.kt' . | grep -E 'State|Event|Effect|Result|Action'

# Application
grep -rn ': Application()\|@HiltAndroidApp' --include='*.kt' .
```

## Schicht 4 — Bildschirme und Flows

```bash
# Top-Level-Screens
grep -rn '@Composable' --include='*.kt' . | grep -E 'fun [A-Z][a-zA-Z]*Screen\('
find . -name '*Screen.kt' -not -path '*/test/*' -not -path '*/build/*'

# Compose Destinations Library
grep -rn '@Destination\|@RootNavGraph\|@NavGraph' --include='*.kt' .

# Navigation
grep -rn 'NavHost(\|composable("' --include='*.kt' .
grep -rn 'composable<' --include='*.kt' .
grep -rn 'navController.navigate\|\.navigate(' --include='*.kt' .

# Click-Handler
grep -rn 'onClick = {' --include='*.kt' .
grep -rn '\.clickable {' --include='*.kt' .
grep -rn '\.combinedClickable\|onLongClick' --include='*.kt' .
grep -rn '\.selectable\|\.toggleable' --include='*.kt' .
grep -rn 'IconButton\|TextButton\|FilledTonalButton\|OutlinedButton' --include='*.kt' .

# Side-Effects mit Navigation
grep -rn 'LaunchedEffect' --include='*.kt' . -A 5
grep -rn '\.collect {' --include='*.kt' . -A 3

# Dialoge und Bottom-Sheets
grep -rn 'AlertDialog\|Dialog(\|BasicAlertDialog\|DatePickerDialog' --include='*.kt' .
grep -rn 'ModalBottomSheet\|BottomSheetScaffold' --include='*.kt' .
grep -rn 'snackbarHostState\|SnackbarResult' --include='*.kt' .

# UI-Events / UiEffects
grep -rn 'sealed class UiEvent\|sealed class UiEffect' --include='*.kt' .
grep -rn 'Channel<\|MutableSharedFlow' --include='*.kt' .

# Onboarding
grep -rn 'first.*launch\|firstLaunch\|isFirstTime\|onboardingComplete' --include='*.kt' . -i
grep -rn 'startDestination' --include='*.kt' . -A 3
grep -rn 'HorizontalPager\|rememberPagerState' --include='*.kt' .

# BackHandler
grep -rn 'BackHandler' --include='*.kt' .

# ActivityResultLauncher / Externe Intents
grep -rn 'rememberLauncherForActivityResult\|ActivityResultContracts' --include='*.kt' .
grep -rn 'Intent\.ACTION_VIEW\|Intent\.ACTION_SEND\|Intent\.ACTION_PICK' --include='*.kt' .

# Permissions Runtime
grep -rn 'rememberPermissionState\|rememberMultiplePermissionsState' --include='*.kt' .
grep -rn 'requestPermissions\|checkSelfPermission' --include='*.kt' .
```

## Schicht 5 — Paywall

```bash
# Core Billing
grep -rln 'BillingClient\|BillingFlowParams\|ProductDetails' --include='*.kt' .
grep -rn 'startConnection\|endConnection\|onBillingSetupFinished' --include='*.kt' .
grep -rn 'queryProductDetailsAsync\|queryPurchasesAsync' --include='*.kt' .

# Subscription-Felder
grep -rn 'SubscriptionOfferDetails\|offerToken\|basePlanId\|pricingPhases' --include='*.kt' .

# Kauf-Flow
grep -rn 'launchBillingFlow\|setProductDetailsParamsList' --include='*.kt' .
grep -rn 'obfuscatedAccountId\|setObfuscatedAccountId' --include='*.kt' .

# Purchase-Verarbeitung
grep -rn 'onPurchasesUpdated\|PurchaseState\|isAcknowledged' --include='*.kt' .
grep -rn 'acknowledgePurchase\|consumeAsync' --include='*.kt' .

# Subscription-Status
grep -rn 'isSubscribed\|isPremium\|hasSubscription' --include='*.kt' . -i

# Premium-Gates
grep -rn 'requirePremium\|premiumOnly\|gateBehindPremium' --include='*.kt' . -i

# Trial / Promo
grep -rn 'pricingPhaseList\|priceAmountMicros\|billingPeriod' --include='*.kt' .
grep -rn 'RecurrenceMode\|FINITE_RECURRING\|INFINITE_RECURRING\|NON_RECURRING' --include='*.kt' .
grep -rn 'offerTags\|offerTag' --include='*.kt' .
grep -rn 'linkedPurchaseToken' --include='*.kt' .

# Cancel-Flow
grep -rn 'churn\|cancel\|kuendig' --include='*.kt' . -i | grep -v 'test/'
grep -rn 'ChurnReason\|CancelReason\|cancellationReason' --include='*.kt' .

# Server-Side Validation
grep -rn 'callFirebaseFunction\|FirebaseFunctions\|httpsCallable' --include='*.kt' .

# Restore Purchases
grep -rn 'restorePurchases\|RESTORE_PURCHASE' --include='*.kt' . -i
```

## Schicht 6 — Hidden Features

```bash
# Background-Jobs
grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' .
grep -rn 'WorkManager\.getInstance\|enqueue\|enqueueUniqueWork' --include='*.kt' .

# Widgets
grep -rln 'class.*: AppWidgetProvider\|extends AppWidgetProvider' --include='*.kt' --include='*.java' .
grep -rn 'AppWidgetManager\|RemoteViews' --include='*.kt' .
find . -path '*/res/xml/*widget*.xml'

# Quick-Settings-Tile
grep -rln 'class.*: TileService' --include='*.kt' .

# App-Shortcuts
grep -rln 'ShortcutManager\|ShortcutInfo\|ShortcutInfoCompat' --include='*.kt' .
find . -name 'shortcuts.xml' -path '*/res/xml/*'

# Notification-Channels
grep -rn 'NotificationChannel\|createNotificationChannel' --include='*.kt' .

# Accessibility Service
grep -rln 'class.*: AccessibilityService' --include='*.kt' --include='*.java'

# Print
grep -rn 'PrintDocumentAdapter\|PrintManager\|PrintAttributes' --include='*.kt' .

# NFC
grep -rn 'NfcAdapter\|NdefRecord\|NdefMessage\|HostApduService' --include='*.kt' .

# Boot-Receiver
grep -rln 'BroadcastReceiver' --include='*.kt' . | xargs grep -l 'BOOT_COMPLETED' 2>/dev/null

# Feature-Flags / Remote Config
grep -rn 'FirebaseRemoteConfig\|remoteConfig\b\|remoteConfig\.' --include='*.kt' .
grep -rn 'BuildConfig\.\|BUILD_TYPE\|FLAVOR\b' --include='*.kt' .
grep -rn 'isFeatureEnabled\|featureFlag\|FeatureToggle' --include='*.kt' . -i

# Debug-Menus
grep -rn 'setOnLongClickListener\|onLongClick\|combinedClickable' --include='*.kt' . -A 5
grep -rn 'BuildConfig.DEBUG' --include='*.kt' . -A 5
grep -rn 'debugMenu\|DebugScreen\|InternalSettings' --include='*.kt' . -i

# A/B-Tests
grep -rn 'experiment\|variant\|cohort\|abTest\|treatment' --include='*.kt' . -i

# Account-Deletion
grep -rn 'deleteAccount\|removeUser\|clearAllData\|gdprDelete' --include='*.kt' . -i

# Backup-Logik
grep -rln 'BackupAgent\|BackupAgentHelper' --include='*.kt' --include='*.java'
find . -name 'backup_rules.xml' -path '*/res/xml/*'

# Sharing-Empfaenger
grep -rn 'ACTION_SEND\|ACTION_SEND_MULTIPLE\|ACTION_PROCESS_TEXT' --include='*.kt' --include='AndroidManifest.xml' .

# In-App-Review
grep -rn 'ReviewManager\|launchReviewFlow' --include='*.kt' .

# Foreground Services
grep -rn 'startForeground\|startForegroundService\|foregroundServiceType' --include='*.kt' --include='AndroidManifest.xml' .

# Dynamic Feature Modules
grep -rn 'com.android.dynamic-feature\|SplitInstallManager' --include='*.kt' --include='*.gradle*' .

# Health Connect
grep -rn 'HealthConnectClient\|readRecords\|insertRecords\|HealthPermission' --include='*.kt' .

# Credential Manager
grep -rn 'CredentialManager\|GetPasswordOption\|GetPasskeyOption' --include='*.kt' .

# In-App-Updates
grep -rn 'AppUpdateManager\|AppUpdateInfo\|completeUpdate' --include='*.kt' .

# DataStore-Keys / SharedPreferences-Keys
grep -rn 'stringPreferencesKey\|booleanPreferencesKey\|intPreferencesKey\|longPreferencesKey' --include='*.kt' .
```

## Schicht 7 — Marketing Audit

```bash
# Strings extrahieren
cat app/src/main/res/values/strings.xml > /tmp/claims_de.txt

# Alle Sprachen
ls -d app/src/main/res/values-* | sort
for f in app/src/main/res/values-*/strings.xml; do
  echo "===== $f ====="
  cat "$f"
done > /tmp/claims_translated.txt

# KRITISCH-Keywords
grep -E '(unlimited|unbegrenzt|all features|alle Features|always|immer|24/7|forever|lifetime|lebenslang|niemals)' \
  app/src/main/res/values/strings.xml -i

# HOCH-Keywords
grep -E '(\bAI\b|\bKI\b|smart|intelligent|offline|private|privat|secure|sicher|ad-free|werbefrei|encrypted|verschluesselt|kostenlos)' \
  app/src/main/res/values/strings.xml -i

# MITTEL-Keywords
grep -E '(premium|pro|best|fastest|schnellste|complete|exclusive|professional|profi)' \
  app/src/main/res/values/strings.xml -i

# Code-Realitaet: Limits suchen
grep -rn 'maxAnalyses\|dailyLimit\|aiLimit\|MAX_\|FREE_DAILY' --include='*.kt' .
grep -rn 'analysisCount\|aiUsageCount' --include='*.kt' .

# Code-Realitaet: Werbung-SDKs
grep -rn 'AdMob\|InterstitialAd\|RewardedAd\|BannerAd' --include='*.kt' .
grep -E 'AD_ID' app/src/main/AndroidManifest.xml

# Code-Realitaet: KI-Aufrufe
grep -rn 'GenerativeModel\|GeminiClient\|OpenAI\|Anthropic\|Claude' --include='*.kt' .
grep -rn 'generateContent\|chat.completions' --include='*.kt' .

# Code-Realitaet: Cloud-Calls
grep -rn 'firebaseFunctions\|httpsCallable\|retrofit\|ktor' --include='*.kt' .

# Code-Realitaet: Verschluesselung
grep -rn 'AES\|EncryptedSharedPreferences\|MasterKey\|sqlCipher' --include='*.kt' .

# Code-Realitaet: Analytics
grep -rn 'FirebaseAnalytics\|Crashlytics\|FirebasePerformance' --include='*.kt' .
grep -rn 'logEvent\|setUserProperty' --include='*.kt' .
```

## Schicht 4b — Wortlaut-Mapping (String-Resource-Bruecken)

Diese Patterns sind die zentrale Brücke fuer 1:1-Wortlaut-Extraktion. Sie machen sichtbar WELCHE Strings in WELCHER Datei verwendet werden.

### Compose: String-Resources

```bash
# stringResource — der haeufigste Compose-Pattern
grep -rn 'stringResource(\s*R\.string\.' --include='*.kt' . | grep -v '/test/' | grep -v '/build/'

# pluralStringResource — fuer Mengenangaben
grep -rn 'pluralStringResource(\s*R\.plurals\.' --include='*.kt' . | grep -v '/test/'

# stringResource MIT Format-Argumenten
grep -rn 'stringResource(\s*R\.string\.[a-zA-Z_]\+\s*,' --include='*.kt' . | grep -v '/test/'

# stringArrayResource — typisch fuer Settings-Dropdowns
grep -rn 'stringArrayResource(\s*R\.array\.' --include='*.kt' .
grep -rn 'integerArrayResource(\s*R\.array\.' --include='*.kt' .

# Pro Screen-Datei: welche String-Keys werden verwendet?
SCREEN=DashboardScreen.kt
grep -oE 'R\.string\.[a-zA-Z0-9_]+' "$SCREEN" | sort -u
grep -oE 'R\.plurals\.[a-zA-Z0-9_]+' "$SCREEN" | sort -u
grep -oE 'R\.array\.[a-zA-Z0-9_]+' "$SCREEN" | sort -u
```

### Klassisches Android (Activity/Fragment/View)

```bash
# getString in allen Varianten
grep -rn 'getString(\s*R\.string\.' --include='*.kt' --include='*.java' . | grep -v '/test/'
grep -rn 'context\.getString\|requireContext()\.getString\|resources\.getString\|getResources()\.getString' --include='*.kt' --include='*.java' . | grep -v '/test/'

# getQuantityString — Plurals klassisch
grep -rn 'getQuantityString(\s*R\.plurals\.' --include='*.kt' --include='*.java' .

# getStringArray
grep -rn 'getStringArray(\s*R\.array\.' --include='*.kt' --include='*.java' .

# XML-Layouts mit Text-Slots
grep -rn 'android:text=' --include='*.xml' res/ | grep -v '@string/' | head -50
grep -rn 'android:text="@string/' --include='*.xml' res/
grep -rn 'android:hint=' --include='*.xml' res/
grep -rn 'android:contentDescription=' --include='*.xml' res/
grep -rn 'app:helperText=\|app:errorText=\|app:placeholderText=' --include='*.xml' res/
```

### Strings-XML alle definierten Keys

```bash
# Alle String-Keys in der Hauptsprache (zaehlen + listen)
grep -oE '<string name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<string name="//' | sed 's/"$//' | sort -u

# Alle Plurals
grep -oE '<plurals name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<plurals name="//' | sed 's/"$//' | sort -u

# Alle Arrays
grep -oE '<string-array name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<string-array name="//' | sed 's/"$//' | sort -u

# Anzahl pro Sprache
for f in app/src/main/res/values*/strings.xml; do
  printf "%-50s %s\n" "$f" "$(grep -c '<string name=' "$f")"
done

# Tote Keys finden (definiert aber im Code nicht verwendet)
ALL_KEYS=$(grep -oE '<string name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<string name="//' | sed 's/"$//')
for k in $ALL_KEYS; do
  if ! grep -rq "R\.string\.$k\b" --include='*.kt' --include='*.java' --include='*.xml' .; then
    echo "TOT: $k"
  fi
done

# Fehlende Keys (im Code verwendet, in strings.xml nicht definiert)
USED_KEYS=$(grep -roEh 'R\.string\.[a-zA-Z0-9_]+' --include='*.kt' --include='*.java' . | sed 's/R\.string\.//' | sort -u)
for k in $USED_KEYS; do
  if ! grep -q "<string name=\"$k\"" app/src/main/res/values/strings.xml; then
    echo "FEHLT: $k"
  fi
done
```

### Dialoge — alle Slots erfassen

```bash
# Compose AlertDialog (Material 3 Slots: title, text, confirmButton, dismissButton)
grep -rn 'AlertDialog(' --include='*.kt' . -A 30 | grep -E 'title = |text = |confirmButton = |dismissButton = '

# Compose BasicAlertDialog (eigenes Layout)
grep -rn 'BasicAlertDialog(' --include='*.kt' . -A 30

# Klassisches AlertDialog.Builder
grep -rn 'AlertDialog\.Builder\|MaterialAlertDialogBuilder' --include='*.kt' --include='*.java' . -A 20 | grep -E '\.setTitle\(|\.setMessage\(|\.setPositiveButton\(|\.setNegativeButton\(|\.setNeutralButton\('

# Date/Time/Picker-Dialoge
grep -rn 'DatePickerDialog\|TimePickerDialog\|MaterialDatePicker\|MaterialTimePicker' --include='*.kt' --include='*.java' . -A 10
```

### Menues (alle Tiefen)

```bash
# Klassische Menu-XML
find . -path '*/res/menu/*.xml' -not -path '*/build/*'
grep -rn '<item\b' --include='*.xml' res/menu/ -A 3

# Compose DropdownMenu
grep -rn 'DropdownMenu(' --include='*.kt' . -A 5
grep -rn 'DropdownMenuItem(' --include='*.kt' . -A 5

# ExposedDropdownMenuBox (Settings/Pickers)
grep -rn 'ExposedDropdownMenuBox\|ExposedDropdownMenu' --include='*.kt' . -A 5

# Bottom Navigation
grep -rn 'NavigationBar(\|NavigationBarItem(' --include='*.kt' . -A 8

# Navigation Rail
grep -rn 'NavigationRail(\|NavigationRailItem(' --include='*.kt' . -A 5

# Drawer
grep -rn 'NavigationDrawer\|ModalDrawerSheet\|PermanentDrawerSheet\|NavigationDrawerItem' --include='*.kt' . -A 5

# TopAppBar mit Actions/Menu
grep -rn 'TopAppBar(' --include='*.kt' . -A 15
grep -rn 'CenterAlignedTopAppBar\|MediumTopAppBar\|LargeTopAppBar' --include='*.kt' . -A 15

# Tabs
grep -rn 'TabRow(\|ScrollableTabRow(\|Tab(\|LeadingIconTab(' --include='*.kt' . -A 5
```

### Settings/Preferences — komplette Hierarchie

```bash
# Preference-XML (klassisch, AndroidX Preference Library)
find . -path '*/res/xml/*.xml' -not -path '*/build/*'
grep -rn '<PreferenceScreen\|<PreferenceCategory\|<SwitchPreferenceCompat\|<CheckBoxPreference\|<ListPreference\|<EditTextPreference\|<SeekBarPreference\|<MultiSelectListPreference\|<DialogPreference\|<Preference\b' --include='*.xml' res/xml/ -A 5

# Preference-Attribute extrahieren
grep -roEh 'android:title="[^"]*"|android:summary="[^"]*"|android:entries="[^"]*"|android:entryValues="[^"]*"|android:dialogTitle="[^"]*"' --include='*.xml' res/xml/

# Compose-basierte Settings (keine Standard-API, App-spezifisch)
grep -rn 'SettingsScreen\|SettingsScreen(\|SettingsList\|SettingsRow\|SettingsItem\|PreferenceRow' --include='*.kt' .
```

### Snackbars, Toasts, Notifications

```bash
# Snackbar
grep -rn 'snackbarHostState\.showSnackbar\|SnackbarHost\|Snackbar\.make\|SnackbarResult\|withDismissAction' --include='*.kt' --include='*.java' . -A 3

# Toast
grep -rn 'Toast\.makeText(' --include='*.kt' --include='*.java' . -A 1

# Notification Channels (Name + Description)
grep -rn 'NotificationChannel(\|createNotificationChannel\|notificationManager\.createNotificationChannel' --include='*.kt' --include='*.java' . -A 5

# Notification-Inhalte
grep -rn '\.setContentTitle(\|\.setContentText(\|\.setSubText(\|\.setTicker(\|\.setBigContentTitle(\|\.setSummaryText(\|\.addAction(' --include='*.kt' --include='*.java' .

# Notification Action-Buttons
grep -rn 'NotificationCompat\.Action\b\|NotificationCompat\.Action\.Builder' --include='*.kt' --include='*.java' .
```

### Error/Empty/Loading-States

```bash
# Pattern: sealed class UiState
grep -rn 'sealed.*UiState\|sealed class.*State\|sealed interface.*State' --include='*.kt' . -A 8

# Komponenten-Namen
grep -rn 'ErrorScreen\|ErrorState\|ErrorMessage\|ErrorBanner\|EmptyScreen\|EmptyState\|EmptyView\|LoadingScreen\|LoadingState\|LoadingIndicator\|RetryButton\|RetryAction' --include='*.kt' . -A 10

# Compose-Patterns fuer Error-Dialoge
grep -rn 'isError = true\|supportingText.*error' --include='*.kt' .
```

### TextField-Slots (oft uebersehen!)

```bash
# Material 3 OutlinedTextField / TextField — alle Slots
grep -rn 'OutlinedTextField(\|TextField(' --include='*.kt' . -A 20 | grep -E 'label\s*=|placeholder\s*=|supportingText\s*=|prefix\s*=|suffix\s*=|isError\s*=|leadingIcon\s*=|trailingIcon\s*='

# Spezifisch nach Text-Slots in TextFields
grep -rn 'TextField(' --include='*.kt' . -A 30 | grep -E 'Text\("[^"]*"\)|stringResource\(R\.string\.[a-z_]+\)'

# label = {}
grep -rn 'label\s*=\s*{' --include='*.kt' . -A 2

# placeholder = {}
grep -rn 'placeholder\s*=\s*{' --include='*.kt' . -A 2

# supportingText = {}
grep -rn 'supportingText\s*=\s*{' --include='*.kt' . -A 3

# prefix / suffix
grep -rn 'prefix\s*=\s*{\|suffix\s*=\s*{' --include='*.kt' . -A 2

# BasicTextField mit decorationBox
grep -rn 'BasicTextField(' --include='*.kt' . -A 30 | grep 'decorationBox'

# DropdownMenuAnchor / ExposedDropdownMenuBox
grep -rn 'ExposedDropdownMenuBox\|ExposedDropdownMenuAnchor' --include='*.kt' . -A 15
```

### Chips (oft uebersehen — viele Filter-/Tag-Labels)

```bash
# Material 3 Chips
grep -rn 'FilterChip(\|AssistChip(\|InputChip(\|SuggestionChip(\|ElevatedFilterChip(\|ElevatedAssistChip(' --include='*.kt' . -A 8

# label = { Text(...) } in Chips
grep -rn 'FilterChip\|AssistChip\|InputChip\|SuggestionChip' --include='*.kt' . -A 10 | grep -E 'label\s*=\s*{' -A 1

# Chip-Container (FlowRow mit Chips)
grep -rn 'FlowRow.*Chip\|Chip\s*[A-Z]' --include='*.kt' . | head -20
```

### Tooltips Material 3 (PlainTooltip + RichTooltip)

```bash
# Material 3 TooltipBox
grep -rn 'TooltipBox(' --include='*.kt' . -A 15

# PlainTooltip — Single-Text-Slot
grep -rn 'PlainTooltip(' --include='*.kt' . -A 5

# RichTooltip — title/text/action
grep -rn 'RichTooltip(' --include='*.kt' . -A 15
grep -rn 'rememberTooltipState\|TooltipState' --include='*.kt' .
```

### SearchBar / DockedSearchBar (Material 3)

```bash
grep -rn 'SearchBar(\|DockedSearchBar(' --include='*.kt' . -A 15
grep -rn 'placeholder\s*=\s*{' --include='*.kt' . -B 5 | grep -E 'SearchBar|DockedSearchBar' -A 5
grep -rn 'leadingIcon\s*=\s*{\|trailingIcon\s*=\s*{' --include='*.kt' . -B 5 | grep -E 'SearchBar' -A 5
```

### semantics-Block (Accessibility-Text)

```bash
# Modifier.semantics { ... }
grep -rn 'Modifier\.semantics\s*{' --include='*.kt' . -A 5
grep -rn 'semantics\s*{' --include='*.kt' . -A 5

# Spezifische a11y-Slots
grep -rn 'contentDescription\s*=' --include='*.kt' . | grep -v 'test/' | head -30
grep -rn 'stateDescription\s*=\|liveRegion\s*=\|paneTitle\s*=' --include='*.kt' .
grep -rn 'clearAndSetSemantics\|mergeDescendants' --include='*.kt' .

# Material 3 Komponenten mit eingebauter a11y
grep -rn 'Modifier\.semantics(mergeDescendants = true)' --include='*.kt' .
```

### Banner-Komponenten (Custom + Material 3)

```bash
# Custom Banner
grep -rn 'class.*Banner\|fun.*Banner(' --include='*.kt' . | head -20

# Snackbar mit Action (oft Banner-Ersatz)
grep -rn 'snackbarHostState\.showSnackbar' --include='*.kt' . -A 5
```

### Slider mit Labels

```bash
# Material 3 Slider
grep -rn 'Slider(\|RangeSlider(' --include='*.kt' . -A 10

# Slider mit Tooltip / Label
grep -rn 'Slider(' --include='*.kt' . -A 15 | grep -E 'thumb\s*=|track\s*=' -B 2

# valueRange + steps fuer beschriftete Slider
grep -rn 'valueRange\s*=' --include='*.kt' . -B 2 | head -10
```

### Markdown / AnnotatedString (Inline-Hyperlinks)

```bash
# AnnotatedString-Builder (Compose Native fuer reichen Text)
grep -rn 'buildAnnotatedString\|AnnotatedString\.Builder' --include='*.kt' . -A 10

# withStyle / SpanStyle
grep -rn 'withStyle\(' --include='*.kt' . -A 5

# Inline-Hyperlinks (UrlAnnotation, ClickableText)
grep -rn 'UrlAnnotation\|ClickableText\|LinkAnnotation\|StringAnnotation' --include='*.kt' . -A 5

# Markdown-Libraries
grep -rln 'compose-markdown\|markdown\.compose\|dev\.jeziellago\.compose_markdown\|com\.halilibo\.richtext' --include='*.kt' --include='*.gradle*' .
```

### Date/Time-Picker

```bash
# Material 3 DatePicker
grep -rn 'DatePicker(\|DatePickerDialog(\|rememberDatePickerState' --include='*.kt' . -A 10

# Material 3 TimePicker
grep -rn 'TimePicker(\|TimePickerDialog(\|rememberTimePickerState' --include='*.kt' . -A 10

# Custom Picker mit Title/Confirm/Dismiss
grep -rn 'showModeToggle\|dateRangePicker' --include='*.kt' . -A 5
```

### Hardcoded Strings (DARF nicht sein, MUSS aber geprueft werden)

```bash
# Compose: Text("...") mit Literal
grep -rn 'Text(\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . | grep -v '/test/' | grep -v '/build/' | grep -v 'Text(text ='

# Compose: Text(text = "literal")
grep -rn 'Text(\s*text\s*=\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . | grep -v '/test/' | grep -v '/build/'

# View: setText("literal")
grep -rn '\.setText("[A-Za-zÄÖÜäöü0-9][^"]\{3,\}"' --include='*.kt' --include='*.java' . | grep -v '/test/'

# Toast/Snackbar mit String-Literal
grep -rn 'Toast\.makeText([^,]*,\s*"' --include='*.kt' --include='*.java' . | grep -v '/test/'
grep -rn 'showSnackbar(\s*"' --include='*.kt' . | grep -v '/test/'

# Title in AlertDialog mit Literal
grep -rn '\.setTitle("\|\.setMessage("' --include='*.kt' --include='*.java' . | grep -v '/test/'
```

### Accessibility-Texte (contentDescription)

```bash
# Compose contentDescription
grep -rn 'contentDescription\s*=' --include='*.kt' . | grep -v '/test/' | grep -v '/build/'

# semantics-Block
grep -rn 'semantics\s*{' --include='*.kt' . -A 3

# XML
grep -rn 'android:contentDescription=' --include='*.xml' res/
```

### Konkatenierte und Template-Strings

```bash
# String-Konkatenation mit Plus
grep -rn '"\s*+\s*stringResource\|stringResource[^)]*)\s*+\s*"' --include='*.kt' .

# Kotlin String-Templates (Vorsicht: viele False-Positives, nur exemplarisch pruefen)
grep -rn '"[^"]*\$\(\|"[^"]*\${' --include='*.kt' . | grep -v '/test/' | head -30
```

## Schicht 4c — Translation-Context

### translatable="false" und xliff:g

```bash
# Nicht-uebersetzbare Strings
grep -oE '<string name="[^"]+" translatable="false"' app/src/main/res/values/strings.xml | sed 's/<string name="//' | sed 's/" translatable.*//' | sort -u

# Anzahl pro Sprache
for f in app/src/main/res/values*/strings.xml; do
  COUNT=$(grep -c 'translatable="false"' "$f" 2>/dev/null || echo 0)
  printf "%-60s %s\n" "$f" "$COUNT"
done

# xliff:g-Tags (Inline-Schutz)
grep -n '<xliff:g' app/src/main/res/values/strings.xml
grep -oE '<xliff:g id="[^"]*"[^>]*>' app/src/main/res/values/strings.xml | sort -u

# Pruefen ob xmlns:xliff im resources-Tag deklariert ist
grep -n 'xmlns:xliff' app/src/main/res/values/strings.xml

# Format-Strings OHNE xliff:g (Kandidaten zum Wrappen)
grep -E '<string[^>]*>[^<]*%[0-9]\$[sdf]' app/src/main/res/values/strings.xml | grep -v 'xliff:g'
```

### XML-Kommentare als Uebersetzer-Notizen

```bash
# Strings mit vorangestelltem Kommentar
awk '/<!--/{c=$0; next} /<string name=/{if(c) print "COMMENT: " c "\nSTRING:  " $0 "\n"; c=""} /^[^!]/{c=""}' app/src/main/res/values/strings.xml | head -100

# Strings OHNE Kommentar (Quote-Liste)
awk 'BEGIN{prev=""} /<!--/{prev=$0; next} /<string name=/{if(prev=="") print; prev=""} /^[[:space:]]*[^<!]/{prev=""}' app/src/main/res/values/strings.xml | head -50

# Format-Strings ohne Kommentar (Pruefen!)
grep -B 1 '%[0-9]\$[sdf]' app/src/main/res/values/strings.xml | grep -v '<!--' | grep '<string' | head -30
```

### CLDR-Plural-Vollstaendigkeit

```bash
# Alle Plural-Keys
grep -oE '<plurals name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<plurals name="//' | sed 's/"$//' | sort -u

# Pro Plural-Key alle Quantitaeten in einer Sprache
KEY=plural_entries_count
LANG=ru
awk "/<plurals name=\"$KEY\"/,/<\/plurals>/" "app/src/main/res/values-$LANG/strings.xml" | grep -oE 'quantity="[^"]+"' | sort -u

# Liste benoetigter Quantitaeten pro Sprache (CLDR-Referenz)
# de, en, es, it, nl, pt, tr, zh, ja, ko: one, other
# fr, pt-rBR:                              one, many, other
# ru, uk, pl, cs, sk:                      one, few, many, other
# ar:                                      zero, one, two, few, many, other
# he:                                      one, two, many, other
# cy:                                      zero, one, two, few, many, other
# ga:                                      one, two, few, many, other
# sl:                                      one, two, few, other
```

### HTML/CDATA in Strings

```bash
# Strings mit HTML-Tags
grep -E '<string[^>]*>[^<]*<(b|i|u|br|a|font|strong|em|span|p|ol|ul|li|tt)\b' app/src/main/res/values/strings.xml

# CDATA-Bereiche
grep -E '<!\[CDATA\[' app/src/main/res/values/strings.xml

# HTML-Entities (escapt)
grep -E '&(lt|gt|amp|quot|apos|nbsp|#[0-9]+);' app/src/main/res/values/strings.xml | head -30

# Strings mit Newlines (\n) — koennen bei Uebersetzung verloren gehen
grep -E '\\\\n' app/src/main/res/values/strings.xml | head -20
```

### Format-Argumente erkennen

```bash
# Positional-Format (%1$s, %2$d)
grep -oE '%[0-9]+\$[sdf]' app/src/main/res/values/strings.xml | sort -u

# Generic-Format (%s, %d) — sollten POSITIONAL gemacht werden fuer Uebersetzungen
grep -E '<string[^>]*>[^<]*%[sdf][^<]*<' app/src/main/res/values/strings.xml | grep -v '%[0-9]\$' | head -20

# Argument-Anzahl pro String
for key in $(grep -oE '<string name="[^"]+"' app/src/main/res/values/strings.xml | sed 's/<string name="//' | sed 's/"$//'); do
  COUNT=$(grep "<string name=\"$key\"" app/src/main/res/values/strings.xml | grep -oE '%[0-9]\$[sdf]' | sort -u | wc -l)
  if [ "$COUNT" -gt 0 ]; then
    echo "$COUNT args: $key"
  fi
done | sort -rn | head -20
```

### Glossar-Auto-Erkennung

```bash
# Top-50 deutsche Substantive (mit Grossbuchstabe)
grep -oE '<string name="[^"]+">[^<]+</string>' app/src/main/res/values/strings.xml | \
  sed 's/<[^>]*>//g' | grep -oE '\b[A-ZÄÖÜ][a-zäöüß]+\b' | \
  sort | uniq -c | sort -rn | head -50

# Top-50 englische Schlagwoerter (lowercase)
grep -oE '<string name="[^"]+">[^<]+</string>' app/src/main/res/values-en/strings.xml | \
  sed 's/<[^>]*>//g' | tr ' ' '\n' | tr -cd '[:alpha:]\n' | \
  awk '{ print tolower($0) }' | grep -E '^.{4,}$' | sort | uniq -c | sort -rn | head -50

# Inkonsistenz-Pruefung: dasselbe deutsche Wort, verschiedene EN-Uebersetzungen
# (Manuell pro Top-Begriff: in welchen Strings taucht "Eintrag" auf? Wie wird er uebersetzt?)
DE_KEY=$(grep -lE '<string name="[^"]+">[^<]*Eintrag' app/src/main/res/values/strings.xml | head -1)
```

### Region-Differenzen

```bash
# Identische Strings in Regional-Varianten (Verdacht: keine echte Lokalisierung)
diff <(grep -E '<string name=' app/src/main/res/values-pt-rBR/strings.xml | sort) \
     <(grep -E '<string name=' app/src/main/res/values-pt-rPT/strings.xml | sort) | wc -l

# Alle Sprach-Paare die "kandidaten" sind
ls -d app/src/main/res/values-*/ 2>/dev/null | grep -oE 'values-[a-z]{2}-r[A-Z]{2}' | sort -u
```

### Du/Sie-Konsistenz (Deutsch)

```bash
# Du-Form
grep -cE '\b(du|dein|deine|deinem|deinen|deiner|dir|dich)\b' app/src/main/res/values/strings.xml

# Sie-Form
grep -cE '\b(Sie|Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihnen)\b' app/src/main/res/values/strings.xml

# Detail mit Zeilen
grep -nE '\b(du|dein|deine)\b' app/src/main/res/values/strings.xml | head -10
grep -nE '\b(Sie|Ihr|Ihre|Ihnen)\b' app/src/main/res/values/strings.xml | head -10
```

### Slot-Laengen-Audit

```bash
# Alle Buttons mit ihrer Laenge (in Hauptsprache) — Cutoff bei 18 Zeichen
grep -oE '<string name="[^"]*button[^"]*"[^>]*>[^<]+</string>\|<string name="[^"]*_cta[^"]*"[^>]*>[^<]+</string>' app/src/main/res/values/strings.xml | \
  awk -F'</string>|>' '{ key=$0; sub(/.*name="/,"",key); sub(/".*/,"",key); text=$2; print length(text), key, text }' | sort -rn | head -30

# Push-Notification-Titles
grep -E '<string name="notif_[^"]*title[^"]*"' app/src/main/res/values/strings.xml | \
  awk -F'</string>|>' '{ key=$0; sub(/.*name="/,"",key); sub(/".*/,"",key); text=$2; print length(text), key, text }'
```

## Quick-Master-Sweep (das wichtigste in einer Zeile)

```bash
# Schneller Erst-Eindruck — was hat die App alles?
echo "=== SCREENS ===" && rg "@Composable" --type kotlin -l | wc -l
echo "=== VIEWMODELS ===" && rg "@HiltViewModel" --type kotlin | wc -l
echo "=== USECASES ===" && find . -name '*UseCase.kt' -o -name '*Interactor.kt' | wc -l
echo "=== ROOM ENTITIES ===" && rg "@Entity\b" --type kotlin -l | wc -l
echo "=== WORKERS ===" && rg "class.*Worker\b\|: CoroutineWorker" --type kotlin | wc -l
echo "=== PERMISSIONS ===" && grep -oE 'android.permission.[A-Z_]+' AndroidManifest.xml | sort -u | wc -l
echo "=== FEATURE FLAGS ===" && rg "remoteConfig\." --type kotlin | wc -l
echo "=== BILLING REFS ===" && rg "BillingClient\|ProductDetails" --type kotlin -l | wc -l
echo "=== STRING KEYS ===" && grep -c "<string name=" app/src/main/res/values/strings.xml
echo "=== PLURAL KEYS ===" && grep -c "<plurals name=" app/src/main/res/values/strings.xml
echo "=== ARRAY KEYS ===" && grep -c "<string-array name=" app/src/main/res/values/strings.xml
echo "=== STRING USES (Compose) ===" && rg "stringResource\(\s*R\.string\." --type kotlin | wc -l
echo "=== STRING USES (klassisch) ===" && rg "getString\(\s*R\.string\." --type kotlin | wc -l
echo "=== MENU XML ===" && find . -path '*/res/menu/*.xml' -not -path '*/build/*' | wc -l
echo "=== PREFERENCE XML ===" && find . -path '*/res/xml/*.xml' -not -path '*/build/*' | wc -l
echo "=== HARDCODED Text() ===" && rg 'Text\(\s*"[A-Za-zÄÖÜäöü]' --type kotlin | grep -v '/test/' | wc -l
```

Diese Zeilen koennen vor jedem Audit ausgefuehrt werden um eine Groessenordnung der App zu bekommen.
