#!/usr/bin/env bash
# app-roentgen: Master-Scan-Skript fuer Android-Apps
# Aufruf: bash feature-scan.sh <pfad-zur-android-app>
# Schreibt strukturierten Initial-Bericht in <app>/app-roentgen-initial-scan.md

set -uo pipefail

APP_DIR="${1:-}"
if [[ -z "$APP_DIR" ]]; then
    echo "Fehler: Bitte App-Verzeichnis als Parameter angeben." >&2
    echo "Aufruf: bash feature-scan.sh <pfad-zur-android-app>" >&2
    exit 1
fi

if [[ ! -d "$APP_DIR" ]]; then
    echo "Fehler: Verzeichnis '$APP_DIR' existiert nicht." >&2
    exit 1
fi

cd "$APP_DIR"

# Manifest finden (kann an mehreren Stellen liegen)
MANIFEST=""
for candidate in "app/src/main/AndroidManifest.xml" "src/main/AndroidManifest.xml" "AndroidManifest.xml"; do
    if [[ -f "$candidate" ]]; then
        MANIFEST="$candidate"
        break
    fi
done

if [[ -z "$MANIFEST" ]]; then
    # Fallback: erste Manifest-Datei suchen die nicht im build-Verzeichnis liegt
    MANIFEST=$(find . -name AndroidManifest.xml -not -path '*/build/*' 2>/dev/null | head -1)
fi

# Build-Gradle finden
GRADLE_FILE=""
for candidate in "app/build.gradle.kts" "app/build.gradle" "build.gradle.kts" "build.gradle"; do
    if [[ -f "$candidate" ]]; then
        GRADLE_FILE="$candidate"
        break
    fi
done

# Strings finden
STRINGS_XML=""
for candidate in "app/src/main/res/values/strings.xml" "src/main/res/values/strings.xml"; do
    if [[ -f "$candidate" ]]; then
        STRINGS_XML="$candidate"
        break
    fi
done

OUTPUT="app-roentgen-initial-scan.md"
DATE=$(date +%Y-%m-%d)

# Helper: zaehle Treffer fuer Pattern (gibt 0 zurueck bei Fehler)
count_grep() {
    local pattern="$1"
    local count=$(grep -rln --include='*.kt' "$pattern" . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "${count:-0}"
}

count_in_file() {
    local pattern="$1"
    local file="$2"
    if [[ ! -f "$file" ]]; then echo "0"; return; fi
    local count=$(grep -c "$pattern" "$file" 2>/dev/null)
    echo "${count:-0}"
}

# === Bericht schreiben ===
{
    echo "# App-Roentgen Initial-Scan"
    echo ""
    echo "**Datum:** $DATE"
    echo "**App-Verzeichnis:** $APP_DIR"
    echo "**Manifest:** ${MANIFEST:-NICHT GEFUNDEN}"
    echo "**Build-Gradle:** ${GRADLE_FILE:-NICHT GEFUNDEN}"
    echo "**Strings.xml:** ${STRINGS_XML:-NICHT GEFUNDEN}"
    echo ""
    echo "Dieser Initial-Scan ist die maschinelle Vorarbeit fuer die 7-Schichten-Tiefenanalyse."
    echo "Die Detail-Auswertung macht Claude in einem zweiten Schritt."
    echo ""
    echo "---"
    echo ""

    # === GROESSEN-ORDNUNG ===
    echo "## 0. App-Groesse"
    echo ""
    KOTLIN_FILES=$(find . -name '*.kt' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | wc -l)
    JAVA_FILES=$(find . -name '*.java' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | wc -l)
    XML_FILES=$(find . -name '*.xml' -path '*/res/*' -not -path '*/build/*' 2>/dev/null | wc -l)
    GRADLE_FILES=$(find . -name 'build.gradle*' -not -path '*/build/*' 2>/dev/null | wc -l)
    echo "| Datei-Typ | Anzahl |"
    echo "|-----------|--------|"
    echo "| Kotlin (.kt) | $KOTLIN_FILES |"
    echo "| Java (.java) | $JAVA_FILES |"
    echo "| Resource XML | $XML_FILES |"
    echo "| build.gradle* | $GRADLE_FILES |"
    echo ""

    # === SCHICHT 1: MANIFEST ===
    echo "## Schicht 1 — Manifest-Daten"
    echo ""
    if [[ -n "$MANIFEST" ]] && [[ -f "$MANIFEST" ]]; then
        echo "### 1.1 Permissions"
        echo ""
        echo "\`\`\`"
        grep -oE 'android.permission.[A-Z_]+' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        # Custom Permissions (z.B. com.android.vending.BILLING)
        grep -oE 'com\.[a-z\.]+\.permission\.[A-Z_]+' "$MANIFEST" 2>/dev/null | sort -u
        echo "\`\`\`"
        echo ""

        echo "### 1.2 Activities"
        echo ""
        echo "\`\`\`"
        grep -E 'android:name=' "$MANIFEST" 2>/dev/null | grep -i 'activity' | sed 's/.*android:name="\([^"]*\)".*/\1/' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.3 Services"
        echo ""
        echo "\`\`\`"
        grep -B1 '<service' "$MANIFEST" 2>/dev/null | grep -oE 'android:name="[^"]*"' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.4 Receiver"
        echo ""
        echo "\`\`\`"
        grep -B1 '<receiver' "$MANIFEST" 2>/dev/null | grep -oE 'android:name="[^"]*"' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.5 Provider"
        echo ""
        echo "\`\`\`"
        grep -oE 'android:authorities="[^"]*"' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.6 Deep-Links und Intent-Filter-Schemes"
        echo ""
        echo "\`\`\`"
        grep -E 'data android:scheme=|data android:host=' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.7 Intent-Actions in der App"
        echo ""
        echo "\`\`\`"
        grep -oE 'action android:name="[^"]*"' "$MANIFEST" 2>/dev/null | sort -u | head -30 || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.8 Backup-Konfig"
        echo ""
        BACKUP_LINE=$(grep -E 'allowBackup|fullBackupContent|dataExtractionRules' "$MANIFEST" 2>/dev/null | head -3)
        if [[ -n "$BACKUP_LINE" ]]; then
            echo "\`\`\`"
            echo "$BACKUP_LINE"
            echo "\`\`\`"
        else
            echo "(keine Backup-Konfig gefunden)"
        fi
        echo ""
    else
        echo "Kein AndroidManifest.xml gefunden — manueller Check noetig."
        echo ""
    fi

    # === SCHICHT 2: DEPENDENCIES ===
    echo "## Schicht 2 — Dependencies"
    echo ""
    if [[ -n "$GRADLE_FILE" ]] && [[ -f "$GRADLE_FILE" ]]; then
        echo "### 2.1 Plugins"
        echo ""
        echo "\`\`\`"
        grep -E 'apply\(|alias\(|id\("' "$GRADLE_FILE" 2>/dev/null | grep -v '//' | head -20 || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 2.2 Implementations / API"
        echo ""
        echo "\`\`\`"
        find . -name 'build.gradle*' -not -path '*/build/*' 2>/dev/null | xargs grep -hE '^\s+(implementation|api|kapt|ksp)\s' 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""
    fi

    if [[ -f "gradle/libs.versions.toml" ]]; then
        echo "### 2.3 Version-Catalog (libs.versions.toml)"
        echo ""
        echo "\`\`\`"
        grep -E '^[a-zA-Z]' gradle/libs.versions.toml | head -100 || echo "(leer)"
        echo "\`\`\`"
        echo ""
    fi

    echo "### 2.4 Capability-Indikatoren (zaehle Imports im Code)"
    echo ""
    echo "| Capability | Treffer |"
    echo "|-----------|---------|"
    echo "| Firebase Analytics | $(count_grep 'FirebaseAnalytics') |"
    echo "| Firebase Crashlytics | $(count_grep 'Crashlytics') |"
    echo "| Firebase Messaging (FCM Push) | $(count_grep 'FirebaseMessaging') |"
    echo "| Firebase Remote Config | $(count_grep 'RemoteConfig') |"
    echo "| Firebase Auth | $(count_grep 'FirebaseAuth') |"
    echo "| Firebase Firestore | $(count_grep 'FirebaseFirestore\|firestore') |"
    echo "| Firebase Functions | $(count_grep 'FirebaseFunctions\|httpsCallable') |"
    echo "| Google Play Billing | $(count_grep 'BillingClient\|ProductDetails') |"
    echo "| Google Play Review | $(count_grep 'ReviewManager') |"
    echo "| Room Database | $(count_grep '@Entity\|@Dao\|@Database') |"
    echo "| DataStore | $(count_grep 'preferencesDataStore\|stringPreferencesKey') |"
    echo "| WorkManager | $(count_grep 'CoroutineWorker\|class.*Worker') |"
    echo "| Hilt DI | $(count_grep '@HiltAndroidApp\|@HiltViewModel\|@Inject') |"
    echo "| Retrofit | $(count_grep 'Retrofit\.Builder\|@GET\|@POST') |"
    echo "| Ktor | $(count_grep 'HttpClient\|io\.ktor') |"
    echo "| Coil (Bilder) | $(count_grep 'AsyncImage\|coil') |"
    echo "| Compose Navigation | $(count_grep 'NavHost\|composable\(') |"
    echo "| ML Kit | $(count_grep 'mlkit') |"
    echo "| Google Gemini | $(count_grep 'GenerativeModel\|GeminiClient') |"
    echo "| OpenAI | $(count_grep 'OpenAI\|openai') |"
    echo "| Whisper | $(count_grep 'Whisper\|whisper') |"
    echo "| Health Connect | $(count_grep 'HealthConnect') |"
    echo "| Biometric | $(count_grep 'BiometricPrompt') |"
    echo "| AdMob | $(count_grep 'AdMob\|InterstitialAd\|RewardedAd\|BannerAd') |"
    echo ""

    # === SCHICHT 3: ARCHITEKTUR ===
    echo "## Schicht 3 — Architektur-Inventar"
    echo ""

    echo "### 3.1 ViewModels"
    echo ""
    echo "\`\`\`"
    grep -rln '@HiltViewModel\|class.*ViewModel\(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.2 Repositories"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Repository\|interface.*Repository' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.3 UseCases / Interactors"
    echo ""
    echo "\`\`\`"
    find . -name '*UseCase.kt' -o -name '*Interactor.kt' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.4 Hilt-Module"
    echo ""
    echo "\`\`\`"
    grep -rln '@Module' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.5 Room Entities"
    echo ""
    echo "\`\`\`"
    grep -rln '@Entity\b' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.6 Workers (Background)"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    # === SCHICHT 4: SCREENS ===
    echo "## Schicht 4 — Bildschirme und Navigation"
    echo ""

    echo "### 4.1 Compose-Screens (Datei-Namen enden auf Screen.kt)"
    echo ""
    echo "\`\`\`"
    find . -name '*Screen.kt' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort | head -100 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 4.2 Compose-Funktionen die mit Uppercase + 'Screen(' enden"
    echo ""
    echo "\`\`\`"
    grep -rn '@Composable' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | xargs -I {} echo {} 2>/dev/null | head -50
    echo "\`\`\`"
    echo ""

    echo "### 4.3 Navigation-Targets"
    echo ""
    echo "\`\`\`"
    grep -rn 'composable(\|composable<' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 4.4 Navigation-Calls (.navigate)"
    echo ""
    NAVIGATE_COUNT=$(grep -rn '\.navigate(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "Anzahl navigate-Aufrufe: $NAVIGATE_COUNT"
    echo ""

    echo "### 4.5 Click-Handler-Anzahl"
    echo ""
    ONCLICK_COUNT=$(grep -rn 'onClick = {' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLICKABLE_COUNT=$(grep -rn '\.clickable {' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    LONGCLICK_COUNT=$(grep -rn 'onLongClick\|combinedClickable' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Pattern | Treffer |"
    echo "|---------|---------|"
    echo "| onClick = { | $ONCLICK_COUNT |"
    echo "| .clickable { | $CLICKABLE_COUNT |"
    echo "| onLongClick / combinedClickable | $LONGCLICK_COUNT |"
    echo ""

    # === SCHICHT 5: PAYWALL ===
    echo "## Schicht 5 — Paywall-Hinweise"
    echo ""

    echo "### 5.1 Billing-relevante Dateien"
    echo ""
    echo "\`\`\`"
    grep -rln 'BillingClient\|ProductDetails\|SubscriptionOfferDetails' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 5.2 Paywall-/Premium-Bildschirme"
    echo ""
    echo "\`\`\`"
    find . \( -name '*Paywall*.kt' -o -name '*Premium*.kt' -o -name '*Subscription*.kt' -o -name '*Churn*.kt' \) -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    echo "### 5.3 Premium-Strings"
    echo ""
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "\`\`\`"
        grep -E '<string name="(paywall|premium|pro|trial|subscribe|upgrade|churn|cancel|billing)' "$STRINGS_XML" 2>/dev/null | head -50 || echo "(keine gefunden)"
        echo "\`\`\`"
    fi
    echo ""

    # === SCHICHT 6: HIDDEN FEATURES ===
    echo "## Schicht 6 — Hidden Features"
    echo ""

    echo "### 6.1 Worker-Klassen"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Worker\b\|: CoroutineWorker' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort
    echo "\`\`\`"
    echo ""

    echo "### 6.2 Notification Channels"
    echo ""
    echo "\`\`\`"
    grep -rn 'NotificationChannel\|createNotificationChannel' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -20 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.3 Long-Click-Trigger (Debug-Menus?)"
    echo ""
    echo "\`\`\`"
    grep -rn 'setOnLongClickListener\|onLongClick\|combinedClickable' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -20 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.4 Feature-Flags / Remote Config Aufrufe"
    echo ""
    echo "\`\`\`"
    grep -rn 'remoteConfig\.\|FirebaseRemoteConfig\|isFeatureEnabled' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.5 Account-Loeschung (DSGVO)"
    echo ""
    echo "\`\`\`"
    grep -rn 'deleteAccount\|removeUser\|clearAllData\|gdprDelete' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -10 || echo "(KEINE ACCOUNT-DELETION GEFUNDEN — DSGVO/Play-Policy-Risiko!)"
    echo "\`\`\`"
    echo ""

    echo "### 6.6 Widgets / Tile / Shortcuts"
    echo ""
    echo "\`\`\`"
    WIDGETS=$(grep -rln 'AppWidgetProvider' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | head -5)
    TILES=$(grep -rln 'TileService' --include='*.kt' . 2>/dev/null | grep -v '/build/' | head -5)
    SHORTCUTS=$(find . -name 'shortcuts.xml' -path '*/res/xml/*' 2>/dev/null | head -5)
    echo "Widgets:    ${WIDGETS:-(keine)}"
    echo "Tiles:      ${TILES:-(keine)}"
    echo "Shortcuts:  ${SHORTCUTS:-(keine)}"
    echo "\`\`\`"
    echo ""

    # === SCHICHT 7: WERBEAUSSAGEN-VOR-AUDIT ===
    echo "## Schicht 7 — Werbeaussagen-Vor-Audit"
    echo ""

    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        TOTAL_STRINGS=$(grep -c '<string name=' "$STRINGS_XML" 2>/dev/null || echo "0")
        echo "Gesamt-Strings (Hauptsprache): $TOTAL_STRINGS"
        echo ""

        # Sprachen
        echo "### 7.1 Sprach-Varianten"
        echo ""
        echo "\`\`\`"
        ls -d app/src/main/res/values-* 2>/dev/null | sort || echo "(keine uebersetzten Sprachen)"
        echo "\`\`\`"
        echo ""

        echo "### 7.2 KRITISCH-Keywords (in Hauptsprache)"
        echo ""
        echo "\`\`\`"
        grep -iE '(unlimited|unbegrenzt|all features|alle Features|always|immer|24/7|forever|lifetime|lebenslang|niemals|never)' "$STRINGS_XML" 2>/dev/null | head -30 || echo "(keine kritischen Keywords gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 7.3 HOCH-Keywords"
        echo ""
        echo "\`\`\`"
        grep -iE '(\bAI\b|\bKI\b|smart|intelligent|offline|private|privat|secure|sicher|ad-free|werbefrei|encrypted|verschluesselt|kostenlos|gratis)' "$STRINGS_XML" 2>/dev/null | head -50 || echo "(keine HOCH-Keywords gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 7.4 MITTEL-Keywords"
        echo ""
        echo "\`\`\`"
        grep -iE '(premium|\bpro\b|best|fastest|schnellste|complete|exclusive|professional|profi)' "$STRINGS_XML" 2>/dev/null | head -30 || echo "(keine MITTEL-Keywords gefunden)"
        echo "\`\`\`"
        echo ""
    else
        echo "Keine strings.xml gefunden — manueller Check noetig."
        echo ""
    fi

    # === SCHICHT 4b: WORTLAUT-INDEX ===
    echo "## Schicht 4b — Wortlaut-Index pro Bereich"
    echo ""
    echo "Dieser Block ist die Vorarbeit fuer das 1:1-Wortlaut-Mapping. Die Tiefenauswertung pro Bereich folgt durch Claude."
    echo ""

    # 4b.1 String-Resources global
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "### 4b.1 Globale Resource-Zaehlung"
        echo ""
        STR_COUNT=$(grep -c '<string name=' "$STRINGS_XML" 2>/dev/null || echo "0")
        PLR_COUNT=$(grep -c '<plurals name=' "$STRINGS_XML" 2>/dev/null || echo "0")
        ARR_COUNT=$(grep -c '<string-array name=' "$STRINGS_XML" 2>/dev/null || echo "0")
        echo "| Resource-Typ | Anzahl in Hauptsprache |"
        echo "|--------------|------------------------|"
        echo "| <string> | $STR_COUNT |"
        echo "| <plurals> | $PLR_COUNT |"
        echo "| <string-array> | $ARR_COUNT |"
        echo ""

        echo "### 4b.2 String-Anzahl pro Sprache"
        echo ""
        echo "\`\`\`"
        for f in $(ls app/src/main/res/values*/strings.xml 2>/dev/null | sort); do
            COUNT=$(grep -c '<string name=' "$f" 2>/dev/null || echo "0")
            printf "%-60s %s\n" "$f" "$COUNT"
        done
        echo "\`\`\`"
        echo ""
    fi

    # 4b.3 String-Resource-Aufrufe im Code
    echo "### 4b.3 String-Resource-Nutzungen im Code"
    echo ""
    COMPOSE_STR=$(grep -rn 'stringResource(\s*R\.string\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    COMPOSE_PLR=$(grep -rn 'pluralStringResource(\s*R\.plurals\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    COMPOSE_ARR=$(grep -rn 'stringArrayResource(\s*R\.array\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLASSIC_STR=$(grep -rn 'getString(\s*R\.string\.' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLASSIC_PLR=$(grep -rn 'getQuantityString(\s*R\.plurals\.' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    XML_STR=$(grep -rn 'android:text="@string/\|android:hint="@string/\|android:contentDescription="@string/' --include='*.xml' res/ 2>/dev/null | wc -l)
    echo "| Aufruf-Typ | Treffer |"
    echo "|-----------|---------|"
    echo "| Compose stringResource | $COMPOSE_STR |"
    echo "| Compose pluralStringResource | $COMPOSE_PLR |"
    echo "| Compose stringArrayResource | $COMPOSE_ARR |"
    echo "| Klassisch getString | $CLASSIC_STR |"
    echo "| Klassisch getQuantityString | $CLASSIC_PLR |"
    echo "| XML @string-References | $XML_STR |"
    echo ""

    # 4b.4 Bereiche
    echo "### 4b.4 UI-Bereiche fuer Wortlaut-Mapping"
    echo ""

    # Dialoge
    COMPOSE_DLG=$(grep -rln 'AlertDialog(\|BasicAlertDialog(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    BUILDER_DLG=$(grep -rln 'AlertDialog\.Builder\|MaterialAlertDialogBuilder' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    BOTTOM_SHEET=$(grep -rln 'ModalBottomSheet(\|BottomSheetScaffold' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    SNACKBAR=$(grep -rn 'snackbarHostState\.showSnackbar\|Snackbar\.make' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TOAST=$(grep -rn 'Toast\.makeText(' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    NOTIF=$(grep -rn '\.setContentTitle(\|\.setContentText(' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Bereich | Treffer |"
    echo "|---------|---------|"
    echo "| Compose Dialoge | $COMPOSE_DLG |"
    echo "| Klassische Dialog-Builder | $BUILDER_DLG |"
    echo "| Bottom Sheets | $BOTTOM_SHEET |"
    echo "| Snackbar-Aufrufe | $SNACKBAR |"
    echo "| Toast-Aufrufe | $TOAST |"
    echo "| Notification-Inhalte (Title/Text) | $NOTIF |"
    echo ""

    # 4b.5 Menue-Hierarchien
    echo "### 4b.5 Menue- und Settings-Hierarchien"
    echo ""
    MENU_XML_COUNT=$(find . -path '*/res/menu/*.xml' -not -path '*/build/*' 2>/dev/null | wc -l)
    PREF_XML_COUNT=$(find . -path '*/res/xml/*.xml' -not -path '*/build/*' 2>/dev/null | wc -l)
    DD_MENU=$(grep -rln 'DropdownMenu(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    NAV_BAR=$(grep -rln 'NavigationBar(\|NavigationBarItem(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    DRAWER=$(grep -rln 'NavigationDrawer\|ModalDrawerSheet\|NavigationDrawerItem' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Komponente | Dateien |"
    echo "|-----------|---------|"
    echo "| res/menu/*.xml | $MENU_XML_COUNT |"
    echo "| res/xml/*.xml (Preferences) | $PREF_XML_COUNT |"
    echo "| Compose DropdownMenu | $DD_MENU |"
    echo "| NavigationBar/Item | $NAV_BAR |"
    echo "| NavigationDrawer | $DRAWER |"
    echo ""

    echo "#### Menue-Dateien (komplette Liste)"
    echo ""
    echo "\`\`\`"
    find . -path '*/res/menu/*.xml' -not -path '*/build/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    echo "#### Preference-Dateien (komplette Liste)"
    echo ""
    echo "\`\`\`"
    find . -path '*/res/xml/*.xml' -not -path '*/build/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    # 4b.6 Hardcoded Strings (sollten 0 sein!)
    echo "### 4b.6 Hardcoded Strings (Warnung wenn >0)"
    echo ""
    HC_TEXT=$(grep -rn 'Text(\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . 2>/dev/null | grep -v '/test/' | grep -v '/build/' | grep -v 'Text(text =' | wc -l)
    HC_SETTEXT=$(grep -rn '\.setText("[A-Za-zÄÖÜäöü0-9][^"]\{3,\}"' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_TOAST=$(grep -rn 'Toast\.makeText([^,]*,\s*"' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_SNACK=$(grep -rn 'showSnackbar(\s*"' --include='*.kt' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_DLG=$(grep -rn '\.setTitle("\|\.setMessage("' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    echo "| Pattern | Treffer |"
    echo "|---------|---------|"
    echo "| Compose Text(\"literal\") | $HC_TEXT |"
    echo "| View .setText(\"literal\") | $HC_SETTEXT |"
    echo "| Toast mit Literal | $HC_TOAST |"
    echo "| showSnackbar mit Literal | $HC_SNACK |"
    echo "| Dialog .setTitle/.setMessage Literal | $HC_DLG |"
    echo ""
    echo "Hinweis: Jeder Treffer >0 ist eine nicht-internationalisierte Werbeaussage-Quelle und MUSS in 4b zitiert werden."
    echo ""

    # 4b.7 Tote / Fehlende Keys (max je 30 zeigen)
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "### 4b.7 Tote Keys (in strings.xml definiert, im Code nicht verwendet)"
        echo ""
        echo "\`\`\`"
        TMP_KEYS=$(mktemp 2>/dev/null || echo "/tmp/keys.$$")
        grep -oE '<string name="[^"]+"' "$STRINGS_XML" 2>/dev/null | sed 's/<string name="//' | sed 's/"$//' | sort -u > "$TMP_KEYS"
        DEAD_COUNT=0
        while read -r k; do
            if ! grep -rq "R\.string\.$k\b" --include='*.kt' --include='*.java' --include='*.xml' . 2>/dev/null; then
                echo "$k"
                DEAD_COUNT=$((DEAD_COUNT + 1))
                if [[ $DEAD_COUNT -ge 30 ]]; then
                    echo "(... weitere abgeschnitten — siehe Vollscan)"
                    break
                fi
            fi
        done < "$TMP_KEYS"
        rm -f "$TMP_KEYS" 2>/dev/null
        if [[ $DEAD_COUNT -eq 0 ]]; then echo "(keine toten Keys)"; fi
        echo "\`\`\`"
        echo ""

        echo "### 4b.8 Fehlende Keys (im Code referenziert, in strings.xml nicht definiert)"
        echo ""
        echo "\`\`\`"
        TMP_USED=$(mktemp 2>/dev/null || echo "/tmp/used.$$")
        grep -rohE 'R\.string\.[a-zA-Z0-9_]+' --include='*.kt' --include='*.java' . 2>/dev/null | sed 's/R\.string\.//' | sort -u > "$TMP_USED"
        MISS_COUNT=0
        while read -r k; do
            if [[ -n "$k" ]] && ! grep -q "<string name=\"$k\"" "$STRINGS_XML" 2>/dev/null; then
                echo "$k"
                MISS_COUNT=$((MISS_COUNT + 1))
                if [[ $MISS_COUNT -ge 30 ]]; then
                    echo "(... weitere abgeschnitten — siehe Vollscan)"
                    break
                fi
            fi
        done < "$TMP_USED"
        rm -f "$TMP_USED" 2>/dev/null
        if [[ $MISS_COUNT -eq 0 ]]; then echo "(keine fehlenden Keys)"; fi
        echo "\`\`\`"
        echo ""
    fi

    # === ABSCHLUSS ===
    echo "---"
    echo ""
    echo "## Hinweise fuer die Tiefenanalyse"
    echo ""
    echo "Dieser Initial-Scan ist die Vorarbeit. Die Tiefenanalyse muss:"
    echo "1. Jede der 7 Schichten gemaess \`references/layer-N-*.md\` durchlaufen"
    echo "2. **Schicht 4b vollstaendig ausfuehren** — fuer JEDEN Bereich (Screen, Dialog, Bottom-Sheet, Menue, Settings-Item, Snackbar, Toast, Notification, Error/Empty/Loading) eine eigene 1:1-Wortlaut-Tabelle erstellen"
    echo "3. **Menues rekursiv ausrollen** — JEDE Untermenue-Ebene mit Breadcrumb-Pfad als eigene Zeile, beliebige Tiefe, keine Abkuerzung"
    echo "4. Pro Befund Datei + Zeilennummer als Beleg liefern"
    echo "5. Die Don't-Miss-Checkliste (\`references/dont-miss-checklist.md\`) abschliessend pruefen — inkl. Block I (Wortlaut-Erfassung)"
    echo "6. Den finalen Bericht nach \`assets/audit-report-template.md\` strukturieren"
    echo ""
    echo "**Naechster Schritt:** Claude liest diesen Initial-Scan und arbeitet die Schichten 1-7 (inkl. 4b) detailliert durch."
} > "$OUTPUT"

echo "Initial-Scan geschrieben: $APP_DIR/$OUTPUT"
echo "Naechster Schritt: Claude liest die Datei und macht die Tiefenanalyse."
