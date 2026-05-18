# App-Roentgen Initial-Scan

**Datum:** 2026-05-18
**App-Verzeichnis:** C:/Users/barwa/proggs/BestJournalAndroid
**Manifest:** app/src/main/AndroidManifest.xml
**Build-Gradle:** app/build.gradle.kts
**Strings.xml:** app/src/main/res/values/strings.xml

Dieser Initial-Scan ist die maschinelle Vorarbeit fuer die 7-Schichten-Tiefenanalyse.
Die Detail-Auswertung macht Claude in einem zweiten Schritt.

---

## 0. App-Groesse

| Datei-Typ | Anzahl |
|-----------|--------|
| Kotlin (.kt) | 142 |
| Java (.java) | 0 |
| Resource XML | 37 |
| build.gradle* | 2 |
| Gradle-Module (mit src/main/) | 0
0 |

## Schicht 1 — Manifest-Daten

### 1.1 Permissions

```
android.permission.ACCESS_COARSE_LOCATION
android.permission.ACCESS_NETWORK_STATE
android.permission.CAMERA
android.permission.INTERNET
android.permission.POST_NOTIFICATIONS
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.RECORD_AUDIO
```

### 1.2 Activities

```
.MainActivity
```

### 1.3 Services

```
(keine gefunden)
```

### 1.4 Receiver

```
android:name=".util.MonthlyReviewReceiver"
android:name=".util.YearlyReviewReceiver"
```

### 1.5 Provider

```
android:authorities="${applicationId}.fileprovider"
```

### 1.6 Deep-Links und Intent-Filter-Schemes

```
(keine gefunden)
```

### 1.7 Intent-Actions in der App

```
action android:name="android.intent.action.BOOT_COMPLETED"
action android:name="android.intent.action.MAIN"
action android:name="android.intent.action.TIMEZONE_CHANGED"
action android:name="android.intent.action.TIME_SET"
```

### 1.8 Backup-Konfig

```
        android:allowBackup="true"
        android:fullBackupContent="@xml/backup_rules"
        android:dataExtractionRules="@xml/data_extraction_rules"
```

## Schicht 2 — Dependencies

### 2.1 Plugins

```
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
```

### 2.2 Implementations / API

```
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

### 2.3 Version-Catalog (libs.versions.toml)

```
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
composeBom = "2025.01.01"
room = "2.7.0"
hilt = "2.55"
hiltNavigationCompose = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.1"
credentials = "1.5.0-alpha06"
googleId = "1.1.1"
googleApiClient = "2.7.0"
googleDrive = "v3-rev20241206-2.0.0"
securityCrypto = "1.1.0-alpha06"
biometric = "1.2.0-alpha05"
navigationCompose = "2.8.7"
lifecycleRuntime = "2.8.7"
activityCompose = "1.9.3"
coreKtx = "1.15.0"
coroutines = "1.9.0"
coil = "3.0.4"
lottie = "6.6.4"
firebaseBom = "34.11.0"
playBilling = "7.1.1"
playReview = "2.0.2"
googleServices = "4.4.2"
junit5 = "5.10.2"
mockk = "1.13.13"
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material3-window = { group = "androidx.compose.material3", name = "material3-window-size-class" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntime" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntime" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
retrofit-scalars = { group = "com.squareup.retrofit2", name = "converter-scalars", version.ref = "retrofit" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
moshi-codegen = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
credentials-play = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
google-id = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleId" }
google-api-client-android = { group = "com.google.api-client", name = "google-api-client-android", version.ref = "googleApiClient" }
google-drive-api = { group = "com.google.apis", name = "google-api-services-drive", version.ref = "googleDrive" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-video = { group = "io.coil-kt.coil3", name = "coil-video", version.ref = "coil" }
lottie-compose = { group = "com.airbnb.android", name = "lottie-compose", version.ref = "lottie" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity" }
firebase-appcheck-debug = { group = "com.google.firebase", name = "firebase-appcheck-debug" }
firebase-config = { group = "com.google.firebase", name = "firebase-config" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }
firebase-functions = { group = "com.google.firebase", name = "firebase-functions" }
play-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "playBilling" }
play-review = { group = "com.google.android.play", name = "review-ktx", version.ref = "playReview" }
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

### 2.4 Capability-Indikatoren (zaehle Imports im Code)

| Capability | Treffer |
|-----------|---------|
| Firebase Analytics | 8 |
| Firebase Crashlytics | 0 |
| Firebase Messaging (FCM Push) | 0 |
| Firebase Remote Config | 1 |
| Firebase Auth | 0 |
| Firebase Firestore | 0 |
| Firebase Functions | 2 |
| Google Play Billing | 3 |
| Google Play Review | 1 |
| Room Database | 13 |
| DataStore | 0 |
| WorkManager | 0 |
| Hilt DI | 43 |
| Retrofit | 2 |
| Ktor | 2 |
| Coil (Bilder) | 3 |
| Compose Navigation | 0 |
| ML Kit | 0 |
| Google Gemini | 1 |
| OpenAI | 1 |
| Whisper | 8 |
| Health Connect | 0 |
| Biometric | 2 |
| AdMob | 0 |

## Schicht 3 — Architektur-Inventar

### 3.1 ViewModels

```
(keine gefunden)
```

### 3.2 Repositories

```
./app/src/main/java/com/bestjournal/app/data/repository/AdviceRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/AuthRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/EntryFollowUpRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/JournalRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/PhotoRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/RetrospectiveRepository.kt
./app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt
```

### 3.3 UseCases / Interactors

```
./app/src/main/java/com/bestjournal/app/domain/usecase/AnalyzeEntropyUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/GenerateAdviceUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/GenerateRetrospectiveUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/ImproveTextUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/RecordAudioUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/SaveJournalEntryUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/SignInWithGoogleUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/SummarizeEntryUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/SyncWithDriveUseCase.kt
./app/src/main/java/com/bestjournal/app/domain/usecase/TranscribeAudioUseCase.kt
```

### 3.4 Hilt-Module

```
./app/src/main/java/com/bestjournal/app/di/AppModule.kt
./app/src/main/java/com/bestjournal/app/di/AuthModule.kt
./app/src/main/java/com/bestjournal/app/di/DatabaseModule.kt
./app/src/main/java/com/bestjournal/app/di/FirebaseModule.kt
./app/src/main/java/com/bestjournal/app/di/NetworkModule.kt
```

### 3.5 Room Entities

```
./app/src/main/java/com/bestjournal/app/data/local/entity/AdviceBlockEntity.kt
./app/src/main/java/com/bestjournal/app/data/local/entity/EntryFollowUpEntity.kt
./app/src/main/java/com/bestjournal/app/data/local/entity/EntryPhotoEntity.kt
./app/src/main/java/com/bestjournal/app/data/local/entity/JournalEntryEntity.kt
./app/src/main/java/com/bestjournal/app/data/local/entity/RetrospectiveSummaryEntity.kt
```

### 3.6 Workers (Background)

```
(keine gefunden)
```

## Schicht 4 — Bildschirme und Navigation

### 4.1 Compose-Screens (Datei-Namen enden auf Screen.kt)

```
./app/src/main/java/com/bestjournal/app/ui/screens/consent/ConsentScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/consent/LegalDocumentScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/entrydetail/EntryDetailScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/journal/JournalScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/onboarding/OnboardingScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/paywall/PaywallScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/retrospective/RetrospectiveScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/splash/SplashScreen.kt
```

### 4.2 Compose-Funktionen mit Uppercase-Namen (Screen/Dialog/Sheet/...)

```
AchievementIcon
AchievementRow
AchievementsSection
AddFollowUpCard
AdviceCard
AdviceCategoryCard
AdviceDerivationDialog
AdviceDetailSheet
AiGeneratedBadge
AiGeneratedBadgeInline
AiImprovedFooter
AiInfoBanner
AiLimitReachedSheet
AiLimitsDialog
AiLimitsDisclaimerRow
AiLimitsFreeNote
AiLimitsInfoIcon
AnalysisTtsShareRow
AnimatedMicButton
AppNavGraph
BackgroundGlow
BenefitRow
BenefitSection
BestJournalTheme
BottomNavBar
BulletLine
CategoryButton
CategoryDetailDialog
ChurnFlowDialog
ConsentFilledButton
ConsentScreen
ContinuousTimelineSection
CrisisActionRow
CrisisHelpDialog
CustomAnalysesBenefitPoint
CustomAnalysesPremiumSheet
CustomDetailDialog
CustomInsightsBlock
CustomLegendItem
CustomRelevanceLegend
CustomResultCard
DashboardScreen
DocLinkChip
EmptyHint
EntropyLevelIndicator
EntryDetailScreen
EvolvingStreakIcon
FeedbackDialog
FloatingActionButtonAnimated
FollowUpDeleteConfirmDialog
(keine gefunden — XML-only-App ohne Compose? Layer 4 nutzt klassische View-Patterns)
```

### 4.3 Navigation-Targets

```
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:60:        composable("splash", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:75:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:99:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:110:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:121:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:132:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:145:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:262:        composable(
./app/src/main/java/com/bestjournal/app/ui/navigation/AppNavGraph.kt:286:        composable(
```

### 4.4 Navigation-Calls (.navigate)

Anzahl navigate-Aufrufe: 11

### 4.5 Click-Handler-Anzahl

| Pattern | Treffer |
|---------|---------|
| onClick = { | 148 |
| .clickable { | 36 |
| onLongClick / combinedClickable | 0 |

## Schicht 5 — Paywall-Hinweise

### 5.1 Billing-relevante Dateien

```
./app/src/main/java/com/bestjournal/app/billing/BillingManager.kt
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt
./app/src/main/java/com/bestjournal/app/util/Constants.kt
```

### 5.2 Paywall-/Premium-Bildschirme

```
./app/src/main/java/com/bestjournal/app/billing/SubscriptionState.kt
./app/src/main/java/com/bestjournal/app/data/remote/SubscriptionStatusService.kt
./app/src/main/java/com/bestjournal/app/ui/screens/paywall/PaywallScreen.kt
./app/src/main/java/com/bestjournal/app/ui/screens/paywall/PaywallViewModel.kt
./app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt
```

### 5.3 Premium-Strings

```
    <string name="paywall_from_per_day">Jahresabo, %1$s pro Jahr</string>
    <string name="paywall_instead_per_month">Spare %1$d%% gegenüber dem Monatsabo</string>
    <string name="paywall_or">oder</string>
    <string name="paywall_with_limited">mit eingeschränkten Limits.</string>
    <string name="churn_instead_of">statt </string>
    <string name="prompt_recording_with_model">Aufnahme läuft — Modell: %1$s</string>
    <string name="prompt_transcribing_with_model">%1$s transkribiert…</string>
    <string name="prompt_transcription_model_hint">Transkribiert mit %1$s</string>
    <string name="prompt_clear_content_desc">Text löschen</string>
    <string name="prompt_clear_confirm_title">Text löschen?</string>
    <string name="prompt_clear_confirm_text">Die Individuelle Analyse wird unwiderruflich gelöscht. Möchtest du wirklich fortfahren?</string>
    <string name="profile_summary">Zusammenfassung</string>
    <string name="profile_entropy">Räume dein Leben auf</string>
    <string name="profile_insight">Selbsterkenntnis</string>
    <string name="profile_goals">Persönliche Ziele</string>
    <string name="profile_custom">Individuelle Analyse</string>
    <string name="profile_summary_desc">Fasst Themen, Muster und Erlebnisse zusammen</string>
    <string name="profile_entropy_desc">Erkennt Stress, Unordnung und Belastung</string>
    <string name="profile_insight_desc">Deckt verborgene Denk- und Gefühlsmuster auf</string>
    <string name="profile_goals_desc">Erkennt Ziele, Wünsche und Fortschritte</string>
    <string name="profile_custom_desc">Eigenen Analyse-Fokus festlegen</string>
    <string name="profile_summary_long">Deine Einträge werden neutral zusammengefasst, ohne Bewertung oder Ratschläge.\n\nDu siehst auf einen Blick:\n\n• Welche Themen dich gerade beschäftigen\n• Welche Muster sich wiederholen\n• Wie sich dein Leben entwickelt\n\nPerfekt als täglicher Überblick über alles, was in deinem Leben passiert.</string>
    <string name="profile_entropy_long">Die KI sucht gezielt nach Stress, Belastung und Unordnung in deinen Einträgen.\n\nDu bekommst:\n\n• Eine Analyse deiner größten Belastungsquellen\n• 5 konkrete Maßnahmen zum Aufräumen\n• Tipps, die dir sofort helfen können\n\nIdeal wenn du das Gefühl hast, dass gerade alles zu viel wird.</string>
    <string name="profile_insight_long">Die KI schaut tiefer als nur auf Ereignisse. Sie erkennt in deinen Einträgen:\n\n• Verborgene Denkmuster und Überzeugungen\n• Wiederkehrende Gefühle und Reaktionen\n• Persönliche Stärken, die dir nicht bewusst sind\n• Werte, die dein Handeln antreiben\n\nFür alle, die sich selbst besser verstehen und innerlich wachsen wollen.</string>
    <string name="profile_goals_long">Die KI findet alle Ziele, Wünsche und Vorhaben in deinen Einträgen, auch beiläufig erwähnte.\n\nDu siehst:\n\n• Welche Ziele du hast (auch versteckte)\n• Wie weit du bei jedem Ziel bist\n• Was dein nächster Schritt sein könnte\n\nDein persönlicher Ziel-Tracker, der aus deinen eigenen Worten liest.</string>
    <string name="profile_custom_long">Du bestimmst selbst, worauf die KI achten soll.\n\nSchreibe deinen eigenen Analyse-Fokus, zum Beispiel:\n\n• „Finde alle Erwähnungen von Sport"\n• „Analysiere meine Stimmungsschwankungen"\n• „Zeige mir, wann ich am produktivsten bin"\n\nVolle Kontrolle für alle, die genau wissen, was sie suchen.</string>
    <string name="profile_custom_prompt">Was ist dir besonders wichtig? Worauf soll sich die KI bei der Analyse deiner Tagebucheinträge konzentrieren?</string>
    <string name="profile_select_hint">Wähle ein Profil aus!</string>
    <string name="profile_custom_add">Individuelle Analyse hinzufügen</string>
    <string name="profile_custom_remove">Individuelle Analyse entfernen</string>
    <string name="profile_custom_rename">Umbenennen</string>
    <string name="profile_custom_rename_save">Name übernehmen</string>
    <string name="profiles_premium_title">Mehrere individuelle Profile</string>
    <string name="profiles_premium_desc">Mit Premium legst du so viele individuelle Analyse-Profile an, wie du willst. Zum Beispiel eins für Sport, eins für deine Arbeit, eins für deine Familie. Jedes Profil analysiert deine Tagebucheinträge mit seinem eigenen Fokus.</string>
    <string name="profiles_benefit_many">Beliebig viele individuelle Profile anlegen</string>
    <string name="profiles_benefit_switch">Jederzeit zwischen deinen Fokussen wechseln</string>
    <string name="profiles_benefit_named">Jedem Profil einen eigenen Namen geben</string>
    <string name="churn_google_play_redirect">Du wirst zu Google Play weitergeleitet, um dein Abo zu verwalten. Deine Tagebucheinträge bleiben natürlich erhalten.</string>
    <string name="paywall_exit_bonus_days">+ %1$d Tage extra Testzeit</string>
    <string name="churn_discount_badge"><xliff:g id="percent" example="50">%1$d</xliff:g>%% SPAREN</string>
    <string name="paywall_prices_loading">Preise werden geladen…</string>
    <string name="paywall_sub_loading_toast">Abo wird geladen, bitte versuche es gleich nochmal.</string>
    <string name="paywall_consent_dialog_title">Widerrufsbelehrung</string>
    <string name="paywall_consent_dialog_body">Du hast nach Abschluss eines Abos oder Kaufs grundsätzlich ein 14-tägiges Widerrufsrecht (§ 355 BGB).\n\nDamit du die Premium-Funktionen sofort nutzen kannst, beginnen wir mit der Bereitstellung unmittelbar nach deinem Kauf. Mit deiner Zustimmung unten verzichtest du in Kenntnis der Folgen auf dein 14-tägiges Widerrufsrecht (§ 356 Abs. 5 BGB).\n\nOhne diese Zustimmung können wir die Premium-Funktionen nicht sofort freischalten.</string>
    <string name="paywall_consent_dialog_checkbox">Ich stimme zu, dass die Bereitstellung der Premium-Funktionen sofort nach Bestellung beginnt. Mir ist bekannt, dass ich dadurch mein 14-tägiges Widerrufsrecht verliere.</string>
    <string name="paywall_consent_dialog_confirm">Jetzt zahlungspflichtig abonnieren</string>
    <string name="paywall_consent_dialog_cancel">Abbrechen</string>
    <string name="paywall_purchase_loading_toast">Kaufvorgang wird geladen, bitte versuche es gleich nochmal.</string>
    <string name="paywall_today">Heute</string>
    <string name="paywall_full_access">Voller Zugang</string>
```

## Schicht 6 — Hidden Features

### 6.1 Worker-Klassen

```
```

### 6.2 Notification Channels

```
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:5:import android.app.NotificationChannel
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:81:        createNotificationChannels()
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:84:    private fun createNotificationChannels() {
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:89:                NotificationChannel(
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:95:            manager.createNotificationChannel(dailyChannel)
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:98:                NotificationChannel(
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:104:            manager.createNotificationChannel(weeklyChannel)
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:106:            NotificationChannel(
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:113:                    manager.createNotificationChannel(it)
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:115:            NotificationChannel(
./app/src/main/java/com/bestjournal/app/BestJournalApp.kt:122:                    manager.createNotificationChannel(it)
```

### 6.3 Long-Click-Trigger (Debug-Menus?)

```
(keine gefunden)
```

### 6.4 Feature-Flags / Remote Config Aufrufe

```
./app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt:15:import com.google.firebase.remoteconfig.FirebaseRemoteConfig
./app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt:52:                val remoteConfig = FirebaseRemoteConfig.getInstance()
./app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt:53:                remoteConfig.fetchAndActivate().await()
./app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt:54:                val groqKey = remoteConfig.getString(Constants.REMOTE_CONFIG_GROQ_KEY)
```

### 6.5 Account-Loeschung (DSGVO)

```
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt:3885:                                    viewModel.deleteAccount(context)
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt:3906:                if (uiState.deleteAccountInProgress) {
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt:3926:                uiState.deleteAccountDriveError?.let { reason ->
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt:3942:                                onClick = { viewModel.deleteAccount(context) }
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsScreen.kt:3951:                                        viewModel.deleteAccount(context, forceLocalDelete = true)
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt:76:    val deleteAccountInProgress: Boolean = false,
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt:78:    val deleteAccountDriveError: String? = null,
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt:905:     *   mismatch), we STOP and expose the error via [SettingsUiState.deleteAccountDriveError] so
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt:911:    fun deleteAccount(context: android.content.Context, forceLocalDelete: Boolean = false) {
./app/src/main/java/com/bestjournal/app/ui/screens/settings/SettingsViewModel.kt:914:                _uiState.value.copy(deleteAccountInProgress = true, deleteAccountDriveError = null)
```

### 6.6 Widgets / Tile / Shortcuts

```
Widgets:    (keine)
Tiles:      (keine)
Shortcuts:  (keine)
```

## Schicht 7 — Werbeaussagen-Vor-Audit

Gesamt-Strings (Hauptsprache): 1094

### 7.1 Sprach-Varianten

```
./app/src/main/res/values-ar
./app/src/main/res/values-bn
./app/src/main/res/values-en
./app/src/main/res/values-es
./app/src/main/res/values-fr
./app/src/main/res/values-gu
./app/src/main/res/values-hi
./app/src/main/res/values-in
./app/src/main/res/values-it
./app/src/main/res/values-ja
./app/src/main/res/values-kn
./app/src/main/res/values-ko
./app/src/main/res/values-ml
./app/src/main/res/values-mr
./app/src/main/res/values-nl
./app/src/main/res/values-pl
./app/src/main/res/values-pt-rBR
./app/src/main/res/values-pt-rPT
./app/src/main/res/values-ru
./app/src/main/res/values-ta
./app/src/main/res/values-te
./app/src/main/res/values-th
./app/src/main/res/values-tr
./app/src/main/res/values-uk
./app/src/main/res/values-ur
./app/src/main/res/values-zh-rCN
./app/src/main/res/values-zh-rTW
```

### 7.2 KRITISCH-Keywords (in Hauptsprache)

```
    <string name="follow_up_premium_title">Unbegrenzte Nachträge</string>
    <string name="follow_up_premium_subtext">Jeder Nachtrag fließt automatisch ins Dashboard ein, damit deine Analyse immer vollständig bleibt.</string>
    <string name="settings_premium_lifetime">Lifetime-Zugang aktiv</string>
    <string name="settings_premium_lifetime_desc">Einmalkauf, alle Features dauerhaft freigeschaltet.</string>
    <string name="settings_premium_desc">Alle Features freigeschaltet, KI jeden Tag, PDF-Export und mehr.</string>
    <string name="settings_premium_feature_profiles">Unbegrenzte individuelle KI-Profile</string>
    <string name="settings_crisis_hotline_de_hours">Kostenlos, 24/7, anonym</string>
    <string name="ai_prompt_insight_attitude">DEINE HALTUNG:\nDu bist ein wohlwollender Spiegel. Du zeigst dem Nutzer ehrlich, was du in seinen Einträgen erkennst — aber immer mit dem Ziel, dass er daraus wachsen kann. Jede Erkenntnis soll ihm helfen, sich selbst besser zu verstehen. Auch schwierige Muster benennst du klar, aber konstruktiv und ohne Vorwurf. Fokus: Was kann der Nutzer aus seinen eigenen Worten über sich lernen?\n\nWAS DU SUCHST:\n- Wiederkehrende Gefühle: Welche Emotionen tauchen immer wieder auf?\n- Denkmuster: Wie denkt der Nutzer über sich, andere, die Welt?\n- Vermeidungsmuster: Was umgeht der Nutzer? Worüber schreibt er nie?\n- Stärken: Was macht der Nutzer gut, auch wenn er es selbst nicht sieht?\n- Werte: Was ist dem Nutzer wirklich wichtig (zeigt sich durch Handeln, nicht Worte)?\n- Auslöser: Was löst starke Reaktionen aus — positiv wie negativ?\n- Widersprüche: Sagt der Nutzer etwas, handelt aber anders?\n- Bedürfnisse: Was braucht der Nutzer, das zwischen den Zeilen durchscheint?\n- Wachstum: Wo hat sich die Sichtweise des Nutzers verändert?</string>
    <string name="ai_prompt_insight_rules">SPRACHREGELN (gelten für ALLE Textfelder im JSON):\n- Einfache, klare Sprache. Kurze Sätze.\n- Keine Fremdwörter, keine Fachbegriffe, keine Floskeln.\n- Jeder soll den Text sofort verstehen, ohne nachzudenken.\n- Einfühlsam, ehrlich und konstruktiv, kein Vorwurf, kein Belehren.\n- Immer mit Blick auf das Positive: Was kann der Nutzer daraus lernen?\n- Keine langen Gedankenstriche (—). Nutze Kommas oder Punkte.\n\nMENGEN-REGEL, VOLLSTÄNDIGKEIT VOR KÜRZE:\nDie Gesamtzahl aller Erkenntnisse über alle Bereiche hinweg soll mindestens 15 betragen. Weniger als 10 ist ein Fehler. Jedes erkannte Muster, jeder Hinweis auf eine Überzeugung, jede wiederkehrende Emotion verdient eine eigene Erkenntnis. Fasse NICHT zusammen. Wenn ein Eintrag Angst, Stolz und Vermeidung zeigt, entstehen daraus 3 separate Erkenntnisse — nicht eine die alles zusammenfasst. Das JSON darf lang werden — Vollständigkeit ist wichtiger als Kürze.</string>
    <string name="ai_prompt_insight_schema_header">JSON-AUSGABE-SCHEMA:\n{\n  \"gesamt_entropie\": 0.0,\n  \"trend\": \"wachsend|stabil|sinkend|unbekannt\",\n  \"gesamtanalyse\": \"...\",\n  \"fortschritte\": [...],\n  \"top_massnahmen\": [...],\n  \"kategorien\": [...]\n}\n\nFELD-DEFINITIONEN:\n\n1) \"gesamt_entropie\" (Zahl, 0.0 bis 1.0)\n   Wie stark reflektiert der Nutzer über sich selbst in seinen Einträgen?\n   - 0.0–0.33 = Wenig Selbstreflexion (hauptsächlich Ereignisse beschrieben)\n   - 0.34–0.66 = Teilweise Selbstreflexion (Gefühle und Gedanken erwähnt)\n   - 0.67–1.0 = Starke Selbstreflexion (tiefe Auseinandersetzung mit sich selbst)\n\n2) \"trend\" (Text)\n   Nur wenn mindestens 3 Einträge über mehrere Tage vorliegen.\n   Vergleiche ältere mit neueren Einträgen:\n   - \"wachsend\" = Der Nutzer reflektiert immer tiefer über sich\n   - \"stabil\" = Gleichbleibendes Reflexionsniveau\n   - \"sinkend\" = Weniger Selbstreflexion in neueren Einträgen\n   - \"unbekannt\" = Zu wenig Daten für eine Aussage\n\n3) \"gesamtanalyse\" (Text, 15–25 Sätze)\n   - Gehe Eintrag für Eintrag durch und finde das tiefere Thema dahinter.\n   - Was verraten die Einträge über den Nutzer als Person?\n   - Welche Muster im Denken, Fühlen und Handeln werden sichtbar?\n   - Welche Stärken zeigt der Nutzer, ohne es vielleicht selbst zu merken?\n   - Welche unbewussten Überzeugungen steuern sein Verhalten?\n   - Wo zeigt sich persönliches Wachstum?\n   - Sei einfühlsam und persönlich — sprich den Nutzer direkt an.\n   - Immer konstruktiv: Auch schwierige Erkenntnisse mit Lernpotenzial verbinden.</string>
    <string name="ai_prompt_insight_output">WORTANZAHL-REGEL FÜR BESCHREIBUNGEN (STRENG EINHALTEN):\nDie \"beschreibung\" in \"top_massnahmen\" und in \"ratschlaege\" muss IMMER zwischen 13 und 21 Wörter lang sein.\n- Weniger als 13 Wörter = zu kurz = FEHLER\n- Mehr als 21 Wörter = zu lang = FEHLER\nZähle die Wörter bevor du sie schreibst. Jede Beschreibung ist EIN kompakter, vollständiger Satz. Nicht mehr, nicht weniger.\n\nAUSGABEFORMAT — STRENGE REGELN:\n- Antworte NUR mit dem JSON-Objekt.\n- Kein Text davor oder danach.\n- Keine Markdown-Backticks.\n- Beginne direkt mit { und ende mit }.\n- Valides JSON — keine fehlenden Kommas, keine doppelten Schlüssel.</string>
    <string name="ai_prompt_summary_schema_header">JSON-AUSGABE-SCHEMA:\n{\n  \"gesamt_entropie\": 0.0,\n  \"trend\": \"steigend|stabil|sinkend|unbekannt\",\n  \"gesamtanalyse\": \"...\",\n  \"fortschritte\": [...],\n  \"top_massnahmen\": [...],\n  \"kategorien\": [...]\n}\n\nFELD-DEFINITIONEN:\n\n1) \"gesamt_entropie\" (Zahl, 0.0 bis 1.0)\n   Wie viel passiert gerade im Leben des Nutzers? Gewichteter Durchschnitt über alle Themenbereiche.\n   - 0.0–0.33 = Ruhige Phase (wenig Aktivität, wenig Veränderung)\n   - 0.34–0.66 = Normale Phase (durchschnittlich viel los)\n   - 0.67–1.0 = Intensive Phase (viel los, viele Themen gleichzeitig)\n\n2) \"trend\" (Text)\n   Nur wenn mindestens 3 Einträge über mehrere Tage vorliegen.\n   Vergleiche ältere mit neueren Einträgen:\n   - \"steigend\" = Es passiert immer mehr, Aktivität nimmt zu\n   - \"stabil\" = Ähnliches Aktivitätsniveau\n   - \"sinkend\" = Es wird ruhiger, weniger Themen\n   - \"unbekannt\" = Zu wenig Daten für eine Aussage\n\n3) \"gesamtanalyse\" (Text, 15–25 Sätze)\n   - Gehe Eintrag für Eintrag durch und extrahiere das Hauptthema.\n   - Benenne JEDES Thema aus JEDEM Eintrag namentlich.\n   - Erkenne Zusammenhänge zwischen den Themen.\n   - Was beschäftigt den Nutzer gerade am meisten?\n   - Was hat sich über die Einträge hinweg verändert?\n   - Sei sachlich und persönlich — sprich den Nutzer direkt an.\n   - Keine Bewertungen, keine Ratschläge — nur zusammenfassen und ordnen.</string>
    <string name="ai_prompt_summary_output">WORTANZAHL-REGEL FÜR BESCHREIBUNGEN (STRENG EINHALTEN):\nDie \"beschreibung\" in \"top_massnahmen\" und in \"ratschlaege\" muss IMMER zwischen 13 und 21 Wörter lang sein.\n- Weniger als 13 Wörter = zu kurz = FEHLER\n- Mehr als 21 Wörter = zu lang = FEHLER\nZähle die Wörter bevor du sie schreibst. Jede Beschreibung ist EIN kompakter, vollständiger Satz. Nicht mehr, nicht weniger.\n\nAUSGABEFORMAT — STRENGE REGELN:\n- Antworte NUR mit dem JSON-Objekt.\n- Kein Text davor oder danach.\n- Keine Markdown-Backticks.\n- Beginne direkt mit { und ende mit }.\n- Valides JSON — keine fehlenden Kommas, keine doppelten Schlüssel.</string>
    <string name="ai_prompt_custom_schema">JSON-AUSGABE-SCHEMA:\n{\n  \"ueberschrift_top5\": \"Kreative Überschrift, passend zum Auftrags-Ergebnis, max 3 Wörter\",\n  \"ueberschrift_analyse\": \"Kreative Überschrift, passend zum Auftrags-Ergebnis, max 3 Wörter\",\n  \"ueberschrift_ergebnisse\": \"Kreative Überschrift, passend zum Auftrags-Ergebnis, max 3 Wörter\",\n  \"fokus_kern\": \"Ein Satz, der den Kern des Auftrags in eigenen Worten wiedergibt — als sichtbarer Anker für das Dashboard.\",\n  \"fokus_zitate\": [\"...\", \"...\"],\n  \"gesamt_entropie\": 0.0,\n  \"trend\": \"steigend|stabil|sinkend|unbekannt\",\n  \"gesamtanalyse\": \"...\",\n  \"fortschritte\": [...],\n  \"top_massnahmen\": [...],\n  \"kategorien\": [...]\n}\n\n1) \"ueberschrift_top5/analyse/ergebnisse\": PFLICHT. Kreativ, spezifisch, max 3 Wörter. MUSS das ERGEBNIS des Auftrags widerspiegeln, nicht nur das Thema. KEINE generischen Titel.\n\n2) \"fokus_kern\": PFLICHT. Ein einzelner Satz (15–30 Wörter), der dem Nutzer zeigt: \"Ich habe deinen Auftrag verstanden — DAS ist der rote Faden.\" Kein Zitat des Auftrags, sondern eine eigene Formulierung in deinen Worten. Das wird oben im Dashboard angezeigt und ist der Beweis, dass das Profil greift.\n\n3) \"fokus_zitate\": PFLICHT, 3 bis 5 Stück. Kurze, wörtlich oder fast-wörtlich entnommene Stellen aus den Tagebucheinträgen, die direkt zum Auftrag passen. Format jeweils: \"[Datum] kurzes Zitat oder paraphrasierte Stelle\". Wenn es bei kreativ-recherchierenden Aufträgen keine wörtlichen Treffer gibt, nimm die 3–5 Einträge mit der größten thematischen Nähe und beschreibe in einer Zeile den Bezug. Wenn weniger als 3 Einträge thematisch passen, gib trotzdem mindestens diese aus und ergänze ggf. einen Hinweis \"(thematische Nähe)\" — niemals leer lassen.\n\n4) \"gesamt_entropie\" (0.0 bis 1.0): Wie stark ist das Auftrags-Thema in den Einträgen vertreten?\n\n5) \"trend\": Nur bei 3+ Einträgen. Wie entwickelt sich das Auftrags-Thema in den Einträgen?\n\n6) \"gesamtanalyse\" (15–25 Sätze): Zwei Teile klar erkennbar. MINDESTENS 50 Prozent des Texts behandeln direkt oder umschreibend den Auftrag.\n   Teil A (Kontext aus den Einträgen): Was steht in den Einträgen zum Thema? Benenne relevante Details.\n   Teil B (Ergebnis des Auftrags): Was ist deine Antwort auf den Auftrag? Was lieferst du neu, zusätzlich oder als Empfehlung?\n   Verknüpfe beide Teile, damit der Nutzer den roten Faden sieht.\n\n7) \"fortschritte\" (0–5): Muster oder Entwicklungen aus den Einträgen, die für den Auftrag wichtig sind. Mindestens die Hälfte der Fortschritts-Einträge muss klar zum Auftrag gehören.\n   { \"titel\": \"max 5 Wörter\", \"beschreibung\": \"2–3 Sätze\", \"bezug\": \"1 Satz\" }\n\n8) \"top_massnahmen\" (genau 5): Die wichtigsten ERGEBNISSE des Auftrags. MINDESTENS 3 von 5 müssen unmissverständlich zum Auftrag gehören. Das können neue Vorschläge, Alternativen, Empfehlungen oder Erkenntnisse sein, die der Nutzer in den Einträgen NICHT erwähnt hat, wenn der Auftrag das verlangt. Bei reinen Analyse-Aufträgen stammen sie aus den Einträgen.\n   {\n     \"titel\": \"max 6 Wörter\",\n     \"beschreibung\": \"13–21 Wörter, kompakt auf den Punkt.\",\n     \"erklaerung\": \"5–8 Sätze ausführlich. Wenn der Inhalt neu ist, begründe, warum er zum Kontext des Nutzers passt. Wenn der Inhalt aus den Einträgen stammt, nenne den konkreten Bezug.\"\n   }\n\n9) \"kategorien\": Themengruppen, die den Auftrag strukturieren (dynamisch). MINDESTENS die Hälfte der Kategorien muss inhaltlich zum Auftrag gehören. Verzichte auf \"Standard-Lebensthemen\" wie Arbeit, Schlaf, Freizeit, wenn sie nicht zum Auftrag passen.\n   {\n     \"name\": \"max 12 Zeichen\", \"icon\": \"material_icon_name\", \"farbe\": \"#HEX\",\n     \"entropie_level\": 0.0,\n     \"zusammenfassung\": \"3–5 Sätze\",\n     \"ratschlaege\": [{\n       \"titel\": \"max 6 Wörter\",\n       \"beschreibung\": \"13–21 Wörter\",\n       \"prioritaet\": \"hoch|mittel|niedrig\",\n       \"verknuepfung\": \"Verbindung zu anderem Thema oder null\",\n       \"herleitung\": [{\"datum\":\"...\",\"zusammenfassung\":\"1–2 Sätze\"}]\n     }]\n   }\n\nHERLEITUNG — HERKUNFT DES INHALTS:\n- Stammt der Inhalt aus einem Eintrag: \"datum\" auf das Eintragsdatum setzen, \"zusammenfassung\" gibt den konkreten Bezug.\n- Ist der Inhalt neu (z.B. eine recherchierte Alternative, ein eigener Vorschlag): \"datum\" auf \"neu\" setzen, \"zusammenfassung\" erklärt in 1–2 Sätzen, warum dieser Vorschlag zum Nutzer-Kontext passt.</string>
    <string name="ai_prompt_custom_output">WORTANZAHL-REGEL: \"beschreibung\" in top_massnahmen und ratschlaege IMMER 13–21 Wörter.\n\nAUSGABEFORMAT — STRENGE REGELN:\n- Antworte NUR mit dem JSON-Objekt.\n- Kein Text davor oder danach. Keine Backticks.\n- Beginne direkt mit { und ende mit }.</string>
    <string name="retro_benefit_weekly">Unbegrenzte Wochenrückblicke, nicht nur die ersten 2 Wochen</string>
    <string name="paywall_lifetime_title">Einmalkauf</string>
    <string name="paywall_lifetime_desc">Einmal zahlen, alles nutzen</string>
    <string name="paywall_lifetime_note">Kein Abo, keine Verlängerung</string>
    <!-- Paywall features -->
    <string name="paywall_feature_profiles">Unbegrenzte individuelle KI-Profile</string>
    <string name="ai_limits_dialog_body" formatted="false">MIT PREMIUM-ABO\n\nPro Tag stehen dir zur Verfügung:\n• Bis zu 150 Dashboard-Analysen pro KI-Profil (bei 4 Profilen also bis zu 600 pro Tag)\n• Bis zu 150 Text-Verbesserungen\n\nWie das im Detail abläuft:\n• Anfragen 1 bis 30: in höchster KI-Qualität\n• Anfragen 31 bis 100: in schneller Standard-Qualität\n• Bei der 101. Anfrage: einmalige 30-Min-Pause\n• Anfragen 101 bis 150: schnelle Standard-Qualität\n• Ab Anfrage 151: keine weiteren Anfragen bis morgen\n\nAußerdem gilt für alle Nutzer:\n• Höchstens 50 KI-Anfragen pro Stunde — danach 5-Min-Pause\n\nWann werden die Kontingente zurückgesetzt?\nJeden Tag um 0:00 Uhr (deine Lokalzeit) startet ein neues volles Kontingent.\n\n──────────\n\nOHNE ABO\n\n• 5 Dashboard-Analysen pro Woche pro Profil\n• 5 Text-Verbesserungen pro Woche\n• Immer in schneller Standard-Qualität\n• Zurückgesetzt jeden Montag um 0:00 Uhr (deine Lokalzeit)\n\n──────────\n\nÜber 99% aller Nutzer treffen diese Beschränkungen wahrscheinlich nie.</string>
    <!-- Premium feature: unlimited Nachträge per journal entry -->
    <string name="paywall_feature_followups">Unbegrenzte Nachträge zu jedem Tagebucheintrag</string>
    <string name="settings_premium_feature_followups">Unbegrenzte Nachträge</string>
    <string name="paywall_lifetime_label">Einmalig</string>
```

### 7.3 HOCH-Keywords

```
    <string name="journal_ai_improving">KI verbessert Text — bitte warten</string>
    <string name="label_improved">✨ KI verbessert</string>
    <string name="journal_drive_backup_current">Backup aktuell, alle Einträge gesichert</string>
    <string name="entry_improve_with_ai">Mit KI nachträglich verbessern</string>
    <!-- AI-improved suffix marker (per EU AI Act Art. 50(4)). The marker is what
    <string name="ai_improved_suffix_marker">✨ KI-verbessert</string>
    <string name="ai_improved_suffix">\n\n✨ KI-verbessert</string>
    <string name="follow_up_tab_improved">✨ KI verbessert</string>
    <string name="follow_up_improve_with_ai">Mit KI nachträglich verbessern</string>
    <string name="follow_up_improving">KI verbessert den Nachtrag…</string>
    <string name="dashboard_create_entries_custom">Erstelle Tagebucheinträge,\ndann analysiert die KI deinen Fokus.</string>
    <string name="dashboard_create_entries_default">Erstelle Tagebucheinträge,\ndann analysiert die KI deine Muster.</string>
    <string name="dashboard_loading_profile_switch">KI-Dashboard wird nach jedem Profilwechsel automatisch aktualisiert</string>
    <string name="dashboard_loading_delete_update">KI-Dashboard wird nach jedem gelöschten Tagebucheintrag automatisch aktualisiert</string>
    <string name="dashboard_loading_auto_update">KI-Dashboard wird nach jedem neuen Tagebucheintrag automatisch aktualisiert</string>
    <string name="dashboard_loading_default">KI-Dashboard wird aktualisiert</string>
    <string name="legend_categories_desc">Kategorien werden dynamisch erstellt — die KI erkennt Themen in deinen Einträgen und erstellt passende Kategorien. Neue Themen führen automatisch zu neuen Symbolen.</string>
    <string name="profile_entropy_long">Die KI sucht gezielt nach Stress, Belastung und Unordnung in deinen Einträgen.\n\nDu bekommst:\n\n• Eine Analyse deiner größten Belastungsquellen\n• 5 konkrete Maßnahmen zum Aufräumen\n• Tipps, die dir sofort helfen können\n\nIdeal wenn du das Gefühl hast, dass gerade alles zu viel wird.</string>
    <string name="profile_insight_long">Die KI schaut tiefer als nur auf Ereignisse. Sie erkennt in deinen Einträgen:\n\n• Verborgene Denkmuster und Überzeugungen\n• Wiederkehrende Gefühle und Reaktionen\n• Persönliche Stärken, die dir nicht bewusst sind\n• Werte, die dein Handeln antreiben\n\nFür alle, die sich selbst besser verstehen und innerlich wachsen wollen.</string>
    <string name="profile_goals_long">Die KI findet alle Ziele, Wünsche und Vorhaben in deinen Einträgen, auch beiläufig erwähnte.\n\nDu siehst:\n\n• Welche Ziele du hast (auch versteckte)\n• Wie weit du bei jedem Ziel bist\n• Was dein nächster Schritt sein könnte\n\nDein persönlicher Ziel-Tracker, der aus deinen eigenen Worten liest.</string>
    <string name="profile_custom_long">Du bestimmst selbst, worauf die KI achten soll.\n\nSchreibe deinen eigenen Analyse-Fokus, zum Beispiel:\n\n• „Finde alle Erwähnungen von Sport"\n• „Analysiere meine Stimmungsschwankungen"\n• „Zeige mir, wann ich am produktivsten bin"\n\nVolle Kontrolle für alle, die genau wissen, was sie suchen.</string>
    <string name="profile_custom_prompt">Was ist dir besonders wichtig? Worauf soll sich die KI bei der Analyse deiner Tagebucheinträge konzentrieren?</string>
    <string name="settings_backup_photos">Fotos sichern</string>
    <string name="settings_backup_videos">Videos sichern</string>
    <string name="settings_syncing">Wird gesichert…</string>
    <string name="settings_sync_success">Erfolgreich gesichert</string>
    <string name="settings_backup_entries">Tagebucheinträge sichern</string>
    <string name="settings_entries_loaded_on_sign_in">Gesicherte Einträge werden beim Anmelden geladen</string>
    <string name="settings_security">Sicherheit</string>
    <string name="settings_ai_profiles_title">KI-Dashboard Profile:</string>
    <string name="settings_ai_automations">KI-Automatisierungen</string>
    <string name="splash_subtitle">DEIN INTELLIGENTES TAGEBUCH</string>
    <string name="settings_premium_desc">Alle Features freigeschaltet, KI jeden Tag, PDF-Export und mehr.</string>
    <string name="settings_premium_feature_improve">Tägliche KI-Textverbesserung</string>
    <string name="settings_premium_feature_5_perspectives">4 besondere KI-Profile</string>
    <string name="settings_premium_feature_reviews">Alle Rückblicke ausführlich mit KI erstellt</string>
    <string name="settings_premium_feature_reviews_desc">Die KI erzählt deine Geschichte</string>
    <string name="settings_premium_feature_patterns_desc">Die KI findet Zusammenhänge die dir nicht auffallen</string>
    <string name="settings_premium_feature_profiles">Unbegrenzte individuelle KI-Profile</string>
    <string name="settings_about_desc">Dein persönliches KI-Tagebuch</string>
    <string name="consent_card2_title">KI-Funktionen (USA)</string>
    <string name="privacy_gate_groq_body">Diese Funktion schickt deine Sprachaufnahme verschlüsselt an Groq (USA) und wandelt sie dort in Text um — schneller und genauer als die lokale Erkennung.\n\n✓ Aufnahme wird nach der Umwandlung sofort gelöscht\n✓ Kein KI-Training mit deinen Aufnahmen\n✓ Rechtsgrundlage: Standardvertragsklauseln (EU SCCs)\n\nAlternative: Lokale Offline-Erkennung in den Einstellungen.</string>
    <string name="privacy_gate_gemini_title">KI-Funktion aktivieren</string>
    <string name="privacy_gate_gemini_body">Schön, dass du die intelligenten Funktionen von Best Journal nutzen möchtest! Für Zusammenfassungen, Rückblicke, Dashboard-Einblicke und Textverbesserungen werden deine Texte verschlüsselt an Google Gemini gesendet.\n\n✓ Kein KI-Training mit deinen Daten\n✓ Anfragen werden sofort nach Verarbeitung gelöscht\n✓ Verschlüsselt nach EU-US Data Privacy Framework\n\nWichtiger Hinweis: Da deine Tagebucheinträge besonders sensible Daten enthalten können (Gesundheit, Religion, persönliche Beziehungen — Art. 9 DSGVO), bitten wir hier um deine ausdrückliche Zustimmung zur KI-Verarbeitung. Die KI-Ausgaben sind Vorschläge, keine professionelle Beratung und ersetzen keine Fachperson.\n\nDu kannst das jederzeit in den Einstellungen ändern.</string>
    <string name="privacy_gate_tts_body">Mit den Premium-Stimmen klingt das Vorlesen natürlicher und angenehmer. Dein Text wird verschlüsselt an Microsoft gesendet und als Audio zurückgegeben.\n\n✓ Kein KI-Training mit deinen Texten\n✓ Verschlüsselt nach EU-US Data Privacy Framework</string>
    <!-- Report AI Content (Google Play AI Policy 04/2024, in Settings → Datenschutz) -->
    <string name="settings_report_ai_title">KI-Antwort melden</string>
    <string name="settings_report_ai_subtitle">Unangemessene oder fehlerhafte KI-Ausgabe</string>
    <string name="settings_report_ai_confirm_body">Es öffnet sich dein E-Mail-Programm mit einer vorbereiteten Nachricht an dev.app.support@gmail.com. Du kannst die Beschreibung noch ergänzen, bevor du absendest. Wir antworten innerhalb von 24 Stunden an Werktagen.\n\nBitte melde damit: unangemessene, anstößige, falsche oder irreführende KI-Ausgaben aus Dashboard, Zusammenfassungen, Rückblicken oder der Textverbesserung.</string>
    <string name="settings_report_ai_subject">Best Journal: Unangemessene KI-Antwort</string>
```

### 7.4 MITTEL-Keywords

```
    <string name="app_name" translatable="false">Best Journal</string>
    <string name="paywall_from_per_day">Jahresabo, %1$s pro Jahr</string>
    <string name="label_premium">Premium</string>
    <string name="journal_text_upsell_body">Mit Premium kannst du jeden Eintrag verbessern.</string>
    <string name="journal_discover_premium">Premium entdecken →</string>
    <string name="journal_streak_premium">Streak-Schutz: Erhältlich mit Premium</string>
    <string name="follow_up_premium_hint">ab dem zweiten Nachtrag</string>
    <string name="follow_up_premium_title">Unbegrenzte Nachträge</string>
    <string name="follow_up_premium_body">Mit Premium kannst du zu jedem Tagebucheintrag so viele Nachträge hinzufügen wie du möchtest.</string>
    <string name="follow_up_premium_subtext">Jeder Nachtrag fließt automatisch ins Dashboard ein, damit deine Analyse immer vollständig bleibt.</string>
    <string name="follow_up_premium_subscribe">Abo starten</string>
    <string name="follow_up_premium_later">Später entscheiden</string>
    <string name="share_footer">Tagebucheintrag von der BestJournal App</string>
    <!-- Body text of the weekly-review premium upsell card on the dashboard. -->
    <string name="dashboard_weekly_review_upsell_body">Du hattest eine bewegte Woche. Mit Premium siehst du die volle Analyse — erkenne Muster, entdecke Einsichten und verstehe, was dich wirklich bewegt.</string>
    <string name="dashboard_discover_premium">Premium entdecken</string>
    <string name="dashboard_custom_focus_hint">Gib in den Einstellungen unter\n\u201eIndividuelle Analyse\u201c einen Fokus ein,\noder wähle ein anderes Profil.</string>
    <string name="dashboard_premium_upsell_body">Mit Premium bekommst du großzügige Analysen aus beliebig vielen Profilen.</string>
    <string name="dashboard_limit_weekly_profile">Wochenlimit für dieses Profil erreicht. Wechsle das Profil oder warte bis Montag.</string>
    <string name="dashboard_loading_profile_switch">KI-Dashboard wird nach jedem Profilwechsel automatisch aktualisiert</string>
    <!-- Profile names -->
    <string name="profile_summary">Zusammenfassung</string>
    <string name="profile_entropy">Räume dein Leben auf</string>
    <string name="profile_insight">Selbsterkenntnis</string>
    <string name="profile_goals">Persönliche Ziele</string>
    <string name="profile_custom">Individuelle Analyse</string>
    <string name="profile_summary_desc">Fasst Themen, Muster und Erlebnisse zusammen</string>
    <string name="profile_entropy_desc">Erkennt Stress, Unordnung und Belastung</string>
    <string name="profile_insight_desc">Deckt verborgene Denk- und Gefühlsmuster auf</string>
    <string name="profile_goals_desc">Erkennt Ziele, Wünsche und Fortschritte</string>
```

## Schicht 4b — Wortlaut-Index pro Bereich

Dieser Block ist die Vorarbeit fuer das 1:1-Wortlaut-Mapping. Die Tiefenauswertung pro Bereich folgt durch Claude.

### 4b.1 Globale Resource-Zaehlung

| Resource-Typ | Anzahl in Hauptsprache |
|--------------|------------------------|
| <string> | 1094 |
| <plurals> | 6 |
| <string-array> | 5 |

### 4b.2 String-Anzahl pro Sprache

```
./app/src/main/res/values-ar/strings.xml                     1082
./app/src/main/res/values-bn/strings.xml                     1082
./app/src/main/res/values-en/strings.xml                     1089
./app/src/main/res/values-es/strings.xml                     1082
./app/src/main/res/values-fr/strings.xml                     1082
./app/src/main/res/values-gu/strings.xml                     1082
./app/src/main/res/values-hi/strings.xml                     1082
./app/src/main/res/values-in/strings.xml                     1082
./app/src/main/res/values-it/strings.xml                     1082
./app/src/main/res/values-ja/strings.xml                     1082
./app/src/main/res/values-kn/strings.xml                     1082
./app/src/main/res/values-ko/strings.xml                     1082
./app/src/main/res/values-ml/strings.xml                     1082
./app/src/main/res/values-mr/strings.xml                     1082
./app/src/main/res/values-nl/strings.xml                     1082
./app/src/main/res/values-pl/strings.xml                     1081
./app/src/main/res/values-pt-rBR/strings.xml                 1082
./app/src/main/res/values-pt-rPT/strings.xml                 1082
./app/src/main/res/values-ru/strings.xml                     1082
./app/src/main/res/values-ta/strings.xml                     1082
./app/src/main/res/values-te/strings.xml                     1082
./app/src/main/res/values-th/strings.xml                     1082
./app/src/main/res/values-tr/strings.xml                     1082
./app/src/main/res/values-uk/strings.xml                     1082
./app/src/main/res/values-ur/strings.xml                     1082
./app/src/main/res/values-zh-rCN/strings.xml                 1082
./app/src/main/res/values-zh-rTW/strings.xml                 1082
./app/src/main/res/values/strings.xml                        1094
```

### 4b.3 String-Resource-Nutzungen im Code

| Aufruf-Typ | Treffer |
|-----------|---------|
| Compose stringResource | 768 |
| Compose pluralStringResource | 0 |
| Compose stringArrayResource | 1 |
| Klassisch getString | 240 |
| Klassisch getQuantityString | 1 |
| XML @string-References | 0 |

### 4b.4 UI-Bereiche fuer Wortlaut-Mapping

| Bereich | Treffer |
|---------|---------|
| Compose Dialoge | 9 |
| Klassische Dialog-Builder | 0 |
| Bottom Sheets | 5 |
| Snackbar-Aufrufe | 2 |
| Toast-Aufrufe | 10 |
| Notification-Inhalte (Title/Text) | 8 |

### 4b.5 Menue- und Settings-Hierarchien

| Komponente | Dateien |
|-----------|---------|
| res/menu/*.xml | 0 |
| res/xml/*.xml (Preferences) | 4 |
| Compose DropdownMenu | 1 |
| NavigationBar/Item | 1 |
| NavigationDrawer | 0 |

#### Menue-Dateien (komplette Liste)

```
```

#### Preference-Dateien (komplette Liste)

```
./app/src/main/res/xml/backup_rules.xml
./app/src/main/res/xml/data_extraction_rules.xml
./app/src/main/res/xml/file_paths.xml
./app/src/main/res/xml/network_security_config.xml
```

### 4b.6 Hardcoded Strings (Warnung wenn >0)

| Pattern | Treffer |
|---------|---------|
| Compose Text("literal") | 0 |
| View .setText("literal") | 0 |
| Toast mit Literal | 0 |
| showSnackbar mit Literal | 0 |
| Dialog .setTitle/.setMessage Literal | 0 |

Hinweis: Jeder Treffer >0 ist eine nicht-internationalisierte Werbeaussage-Quelle und MUSS in 4b zitiert werden.

### 4b.7 Tote Keys (in strings.xml definiert, im Code nicht verwendet)

```
ai_prompt_language_override
ai_retro_copied_message
ai_retro_copy_button
ai_retro_empty_state
ai_retro_error_state
ai_retro_generate_button
ai_retro_generating_state
ai_retro_month_label
ai_retro_share_button
ai_retro_title
ai_retro_week_label
ai_retro_year_label
churn_promo_months_remaining
churn_promo_one_month_remaining
consent_accept_all
consent_btn_save_selection
consent_card1_body
consent_card1_title
consent_card2_body
consent_card2_title
consent_card3_body
consent_card3_title
consent_confirmation
consent_disable_stats
consent_intro
consent_links_header
consent_optout_confirmation
consent_revoke_snackbar
consent_saved_snackbar
consent_title
(... weitere abgeschnitten — siehe Vollscan)
```

### 4b.8 Fehlende Keys (im Code referenziert, in strings.xml nicht definiert)

```
(keine fehlenden Keys)
```

## Schicht 4c — Translation-Context

Vorarbeit fuer den Uebersetzungs-Skill. Die Tiefenauswertung folgt durch Claude.

### 4c.1 Nicht-uebersetzbare Strings

**Anzahl:** 4

```
<string name="app_name" translatable="false">Best Journal</string>
<string name="transcription_model_groq" translatable="false">whisper-large-v3-turbo</string>
<string name="settings_about_copyright" translatable="false">© Barwandt Digital Labs</string>
<string name="settings_legal_online_url" translatable="false">https://pepsi1978.github.io/proggs/bestjournal/</string>
```

### 4c.2 xliff:g-Tags (Inline-Schutz)

| Metrik | Wert |
|--------|------|
| xmlns:xliff deklariert | JA |
| xliff:g-Tags verwendet | 21 |
| Format-Strings OHNE xliff:g (Kandidaten) | 68 |

**Beispiele:**
```
    <string name="follow_up_inline_title">Nachtrag <xliff:g id="ordinal" example="1">%1$s</xliff:g></string>
    <string name="follow_up_delete_confirm_title">Nachtrag <xliff:g id="ordinal" example="1">%1$s</xliff:g> löschen?</string>
    <string name="follow_up_delete_confirm_text">Möchtest du Nachtrag <xliff:g id="ordinal" example="1">%1$s</xliff:g> wirklich unwiderruflich löschen?</string>
    <string name="share_followup_numbered">Nachtrag <xliff:g id="ordinal" example="1">%1$s</xliff:g></string>
    <string name="error_recording_failed">Aufnahme fehlgeschlagen: <xliff:g id="reason" example="Mikrofon nicht verfügbar">%1$s</xliff:g></string>
    <string name="error_transcription_failed">Transkription fehlgeschlagen: <xliff:g id="reason" example="Zeitüberschreitung">%1$s</xliff:g></string>
    <string name="settings_signin_failed">Anmeldung fehlgeschlagen: <xliff:g id="error_reason" example="Network error">%1$s</xliff:g></string>
    <string name="settings_premium_cancelled_until">Premium-Abo gekündigt — läuft noch bis <xliff:g id="date" example="30.04.2026, 21:01">%1$s</xliff:g></string>
    <string name="limit_remaining"><xliff:g id="count" example="3">%1$d</xliff:g> verbleibend</string>
    <string name="limit_used_of_max"><xliff:g id="used" example="3">%1$d</xliff:g> von <xliff:g id="max" example="5">%2$d</xliff:g> genutzt</string>
```

### 4c.3 Uebersetzer-Notizen (XML-Kommentare)

**Strings mit vorangestelltem Kommentar:** 111 / 1094 (10.1%)

### 4c.4 Plural-Resources und CLDR-Vollstaendigkeit

**Anzahl Plural-Keys:** 6

**Quantitaeten pro Sprache und Plural-Key:**

```
default         | datetime_days_ago                        | one,other
default         | datetime_hours_ago                       | one,other
default         | datetime_minutes_ago                     | one,other
default         | datetime_months_relative                 | one,other
default         | datetime_years_relative                  | one,other
default         | settings_export_success                  | one,other
ar              | datetime_days_ago                        | few,many,one,other,two,zero
ar              | datetime_hours_ago                       | few,many,one,other,two,zero
ar              | datetime_minutes_ago                     | few,many,one,other,two,zero
ar              | datetime_months_relative                 | one,other
ar              | datetime_years_relative                  | one,other
ar              | settings_export_success                  | few,many,one,other,two,zero
bn              | datetime_days_ago                        | one,other
bn              | datetime_hours_ago                       | one,other
bn              | datetime_minutes_ago                     | one,other
bn              | datetime_months_relative                 | one,other
bn              | datetime_years_relative                  | one,other
bn              | settings_export_success                  | one,other
en              | datetime_days_ago                        | one,other
en              | datetime_hours_ago                       | one,other
en              | datetime_minutes_ago                     | one,other
en              | datetime_months_relative                 | one,other
en              | datetime_years_relative                  | one,other
en              | settings_export_success                  | one,other
es              | datetime_days_ago                        | one,other
es              | datetime_hours_ago                       | one,other
es              | datetime_minutes_ago                     | one,other
es              | datetime_months_relative                 | one,other
es              | datetime_years_relative                  | one,other
es              | settings_export_success                  | one,other
fr              | datetime_days_ago                        | one,other
fr              | datetime_hours_ago                       | one,other
fr              | datetime_minutes_ago                     | one,other
fr              | datetime_months_relative                 | one,other
fr              | datetime_years_relative                  | one,other
fr              | settings_export_success                  | one,other
gu              | datetime_days_ago                        | one,other
gu              | datetime_hours_ago                       | one,other
gu              | datetime_minutes_ago                     | one,other
gu              | datetime_months_relative                 | one,other
gu              | datetime_years_relative                  | one,other
gu              | settings_export_success                  | one,other
hi              | datetime_days_ago                        | one,other
hi              | datetime_hours_ago                       | one,other
hi              | datetime_minutes_ago                     | one,other
hi              | datetime_months_relative                 | one,other
hi              | datetime_years_relative                  | one,other
hi              | settings_export_success                  | one,other
in              | datetime_days_ago                        | other
in              | datetime_hours_ago                       | other
in              | datetime_minutes_ago                     | other
in              | datetime_months_relative                 | one,other
in              | datetime_years_relative                  | one,other
in              | settings_export_success                  | other
it              | datetime_days_ago                        | one,other
it              | datetime_hours_ago                       | one,other
it              | datetime_minutes_ago                     | one,other
it              | datetime_months_relative                 | one,other
it              | datetime_years_relative                  | one,other
it              | settings_export_success                  | one,other
ja              | datetime_days_ago                        | other
ja              | datetime_hours_ago                       | other
ja              | datetime_minutes_ago                     | other
ja              | datetime_months_relative                 | one,other
ja              | datetime_years_relative                  | one,other
ja              | settings_export_success                  | other
kn              | datetime_days_ago                        | one,other
kn              | datetime_hours_ago                       | one,other
kn              | datetime_minutes_ago                     | one,other
kn              | datetime_months_relative                 | one,other
kn              | datetime_years_relative                  | one,other
kn              | settings_export_success                  | one,other
ko              | datetime_days_ago                        | other
ko              | datetime_hours_ago                       | other
ko              | datetime_minutes_ago                     | other
ko              | datetime_months_relative                 | one,other
ko              | datetime_years_relative                  | one,other
ko              | settings_export_success                  | other
ml              | datetime_days_ago                        | one,other
ml              | datetime_hours_ago                       | one,other
ml              | datetime_minutes_ago                     | one,other
ml              | datetime_months_relative                 | one,other
ml              | datetime_years_relative                  | one,other
ml              | settings_export_success                  | one,other
mr              | datetime_days_ago                        | one,other
mr              | datetime_hours_ago                       | one,other
mr              | datetime_minutes_ago                     | one,other
mr              | datetime_months_relative                 | one,other
mr              | datetime_years_relative                  | one,other
mr              | settings_export_success                  | one,other
nl              | datetime_days_ago                        | one,other
nl              | datetime_hours_ago                       | one,other
nl              | datetime_minutes_ago                     | one,other
nl              | datetime_months_relative                 | one,other
nl              | datetime_years_relative                  | one,other
nl              | settings_export_success                  | one,other
pl              | datetime_days_ago                        | few,many,one,other
pl              | datetime_hours_ago                       | few,many,one,other
pl              | datetime_minutes_ago                     | few,many,one,other
pl              | datetime_months_relative                 | one,other
pl              | datetime_years_relative                  | one,other
pl              | settings_export_success                  | few,many,one,other
pt-rBR          | datetime_days_ago                        | one,other
pt-rBR          | datetime_hours_ago                       | one,other
pt-rBR          | datetime_minutes_ago                     | one,other
pt-rBR          | datetime_months_relative                 | one,other
pt-rBR          | datetime_years_relative                  | one,other
pt-rBR          | settings_export_success                  | one,other
pt-rPT          | datetime_days_ago                        | one,other
pt-rPT          | datetime_hours_ago                       | one,other
pt-rPT          | datetime_minutes_ago                     | one,other
pt-rPT          | datetime_months_relative                 | one,other
pt-rPT          | datetime_years_relative                  | one,other
pt-rPT          | settings_export_success                  | one,other
ru              | datetime_days_ago                        | few,many,one,other
ru              | datetime_hours_ago                       | few,many,one,other
ru              | datetime_minutes_ago                     | few,many,one,other
ru              | datetime_months_relative                 | one,other
ru              | datetime_years_relative                  | one,other
ru              | settings_export_success                  | few,many,one,other
ta              | datetime_days_ago                        | one,other
ta              | datetime_hours_ago                       | one,other
ta              | datetime_minutes_ago                     | one,other
ta              | datetime_months_relative                 | one,other
ta              | datetime_years_relative                  | one,other
ta              | settings_export_success                  | one,other
te              | datetime_days_ago                        | one,other
te              | datetime_hours_ago                       | one,other
te              | datetime_minutes_ago                     | one,other
te              | datetime_months_relative                 | one,other
te              | datetime_years_relative                  | one,other
te              | settings_export_success                  | one,other
th              | datetime_days_ago                        | other
th              | datetime_hours_ago                       | other
th              | datetime_minutes_ago                     | other
th              | datetime_months_relative                 | one,other
th              | datetime_years_relative                  | one,other
th              | settings_export_success                  | other
tr              | datetime_days_ago                        | one,other
tr              | datetime_hours_ago                       | one,other
tr              | datetime_minutes_ago                     | one,other
tr              | datetime_months_relative                 | one,other
tr              | datetime_years_relative                  | one,other
tr              | settings_export_success                  | one,other
uk              | datetime_days_ago                        | few,many,one,other
uk              | datetime_hours_ago                       | few,many,one,other
uk              | datetime_minutes_ago                     | few,many,one,other
uk              | datetime_months_relative                 | one,other
uk              | datetime_years_relative                  | one,other
uk              | settings_export_success                  | few,many,one,other
ur              | datetime_days_ago                        | one,other
ur              | datetime_hours_ago                       | one,other
ur              | datetime_minutes_ago                     | one,other
ur              | datetime_months_relative                 | one,other
ur              | datetime_years_relative                  | one,other
ur              | settings_export_success                  | one,other
zh-rCN          | datetime_days_ago                        | other
zh-rCN          | datetime_hours_ago                       | other
zh-rCN          | datetime_minutes_ago                     | other
zh-rCN          | datetime_months_relative                 | one,other
zh-rCN          | datetime_years_relative                  | one,other
zh-rCN          | settings_export_success                  | other
zh-rTW          | datetime_days_ago                        | other
zh-rTW          | datetime_hours_ago                       | other
zh-rTW          | datetime_minutes_ago                     | other
zh-rTW          | datetime_months_relative                 | one,other
zh-rTW          | datetime_years_relative                  | one,other
zh-rTW          | settings_export_success                  | other
ar              | datetime_days_ago                        | few,many,one,other,two,zero
ar              | datetime_hours_ago                       | few,many,one,other,two,zero
ar              | datetime_minutes_ago                     | few,many,one,other,two,zero
ar              | datetime_months_relative                 | one,other
ar              | datetime_years_relative                  | one,other
ar              | settings_export_success                  | few,many,one,other,two,zero
bn              | datetime_days_ago                        | one,other
bn              | datetime_hours_ago                       | one,other
bn              | datetime_minutes_ago                     | one,other
bn              | datetime_months_relative                 | one,other
bn              | datetime_years_relative                  | one,other
bn              | settings_export_success                  | one,other
en              | datetime_days_ago                        | one,other
en              | datetime_hours_ago                       | one,other
en              | datetime_minutes_ago                     | one,other
en              | datetime_months_relative                 | one,other
en              | datetime_years_relative                  | one,other
en              | settings_export_success                  | one,other
es              | datetime_days_ago                        | one,other
es              | datetime_hours_ago                       | one,other
es              | datetime_minutes_ago                     | one,other
es              | datetime_months_relative                 | one,other
es              | datetime_years_relative                  | one,other
es              | settings_export_success                  | one,other
fr              | datetime_days_ago                        | one,other
fr              | datetime_hours_ago                       | one,other
fr              | datetime_minutes_ago                     | one,other
fr              | datetime_months_relative                 | one,other
fr              | datetime_years_relative                  | one,other
fr              | settings_export_success                  | one,other
gu              | datetime_days_ago                        | one,other
gu              | datetime_hours_ago                       | one,other
```

**CLDR-Referenz:**
- de/en/es/it/nl/pt/tr/zh/ja/ko: `one, other`
- fr/pt-rBR: `one, many, other`
- ru/uk/pl/cs/sk: `one, few, many, other`
- ar: `zero, one, two, few, many, other`

### 4c.5 HTML / CDATA in Strings

| Metrik | Wert |
|--------|------|
| Strings mit HTML-Tags | 0 |
| Strings mit CDATA | 0 |
| Strings mit HTML-Entities | 4 |

### 4c.6 Format-Argumente

| Format-Typ | Anzahl |
|-----------|--------|
| Positional (%1$s, %2$d) | 117 |
| Generic (%s, %d) | 0 |

### 4c.7 Glossar-Kandidaten (Top-30 Begriffe)

**Top-30 deutsche Substantive (in Strings, Vorkommen):**

```
     77 Einträge
     51 Einträgen
     50 ür
     48 Wörter
     48 Eintrag
     42 Text
     40 Nutzer
     40 Die
     38 Du
     36 Was
     35 Muster
     34 Thema
     34 Analyse
     32 Google
     32 Dein
     31 Premium
     31 Dashboard
     29 Sätze
     29 Auftrag
     26 Profil
     26 Keine
     26 Fokus
     24 Tagebucheinträge
     21 Themen
     21 Alle
     20 Jeder
     19 Erkenntnisse
     18 Ziele
     18 Fotos
     18 Erkenntnis
```

**Top-30 englische Schlagwoerter (lowercase, >3 Zeichen):**

```
    186 your
    134 entries
     94 with
     71 journal
     70 from
     69 entry
     66 what
     56 text
     55 this
     51 icon
     50 farbe
     46 every
     42 user
     42 review
     41 analysis
     40 assignment
     37 personal
     37 patterns
     35 short
     33 that
     33 reviews
     33 profile
     33 only
     32 google
     32 focus
     30 premium
     30 dashboard
     28 write
     28 words
     28 life
```

### 4c.8 Region-Differenzen (Verdacht fehlende Lokalisierung)

| Sprach-Paar | Strings im 1. | Strings im 2. | Diff-Zeilen | Status |
|-------------|---------------|---------------|-------------|--------|
| pt-rBR vs pt-rPT | 1082 | 1082 | 1270 | OK (1270 Diff-Zeilen) |
| zh-rCN vs zh-rTW | 1082 | 1082 | 2000 | OK (2000 Diff-Zeilen) |

### 4c.9 Du/Sie-Konsistenz (Deutsch)

| Anrede-Form | Treffer |
|------------|---------|
| Du-Form (du/dein/dir/dich) | 206 |
| Sie-Form (Sie/Ihr/Ihnen) | 2 |

**⚠ MISCHANREDE erkannt** — beide Anredeformen kommen vor. Beispiel-Zeilen:

```
[Du-Beispiele]
88:    <string name="journal_placeholder_prompt">Schreibe hier deine Gedanken…</string>
90:    <string name="journal_freeflow_hint">Lass deine Gedanken frei fließen. Es gibt kein richtig oder falsch.</string>
97:    <string name="journal_text_upsell_body">Mit Premium kannst du jeden Eintrag verbessern.</string>
123:    <string name="prompt_clear_confirm_text">Die Individuelle Analyse wird unwiderruflich gelöscht. Möchtest du wirklich fortfahren?</string>
140:    <string name="streak_desc_1">Jeder Eintrag ist ein kleines Geschenk an dein zukünftiges Ich. Schreib morgen wieder!</string>

[Sie-Beispiele]
391:    <string name="profile_insight_long">Die KI schaut tiefer als nur auf Ereignisse. Sie erkennt in deinen Einträgen:\n\n• Verborgene Denkmuster und Überzeugungen\n• Wiederkehrende Gefühle und Reaktionen\n• Persönliche Stärken, die dir nicht bewusst sind\n• Werte, die dein Handeln antreiben\n\nFür alle, die sich selbst besser verstehen und innerlich wachsen wollen.</string>
920:    <string name="ai_prompt_custom_intro">Du bist ein intelligenter, aufmerksamer Tagebuch-Analyst UND Aufgaben-Bearbeiter.\n\nARBEITSWEISE IN ZWEI SCHRITTEN:\n\nSCHRITT 1 — KONTEXT AUFNEHMEN:\nLies zuerst ALLE Tagebucheinträge des Nutzers vollständig durch. Verstehe die Situation, die Muster, die Themen, die genannten Dinge, Probleme, Wünsche. Die Einträge sind dein KONTEXT, dein Startpunkt, deine Grundlage. Sie begrenzen dich aber nicht.\n\nSCHRITT 2 — AUFGABE AUSFÜHREN:\n%1$s\n\nDas oben ist dein eigentlicher AUFTRAG. Führe diesen Auftrag auf Basis des Kontexts aus Schritt 1 aus. Wenn der Auftrag Recherche, Ideen, Alternativen, Vorschläge, Empfehlungen oder neue Informationen verlangt, dann liefere diese AKTIV, auch wenn sie in den Einträgen nicht vorkommen. Die Einträge informieren deinen Output, sie begrenzen ihn nicht.\n\nKERN-REGEL — DAS PROFIL TRÄGT MINDESTENS 50 PROZENT:\nDer gesamte Output (Gesamtanalyse, Top-Maßnahmen, Kategorien, Ratschläge, Fortschritte) MUSS zu mindestens 50 Prozent klar erkennbar mit dem Auftrag des Nutzers verbunden sein. Erkenntnisse, Punkte oder Themen, die KEINEN inhaltlichen Bezug zum Auftrag haben, lass weg — fülle den Output nicht mit allgemeinen Lebensthemen auf, nur damit das JSON voll ist. Lieber weniger Punkte mit klarem Bezug als viele generische.\n\nWichtig zur Sprache: Drücke den Bezug VARIANTENREICH aus. Nutze Synonyme, verwandte Begriffe, thematische Nachbarschaften und Umschreibungen. Wiederhole NICHT mechanisch die exakten Wörter aus dem Auftrag — das wirkt aufdringlich. Beispiel Auftrag \"Ziele\": Sprich auch von Vorhaben, Wünschen, Ambitionen, dem was du erreichen willst, deinem Kurs, Entwicklungs-Schritten. Beispiel Auftrag \"Sport\": Sprich auch von Bewegung, Training, körperlicher Aktivität, Ausdauer, Fitness, Gesundheit. Der Profil-Bezug muss SPÜRBAR sein, nicht buchstabengetreu.\n\nENTSCHEIDEND:\n- Steht im Auftrag \"analysiere\", \"fasse zusammen\", \"was fällt auf\" — bleib dicht an den Einträgen.\n- Steht im Auftrag \"recherchiere\", \"finde Alternativen\", \"schlage vor\", \"empfehle\", \"ergänze\", \"was wäre wenn\", \"neue Ideen\" — gehe AKTIV über die Einträge hinaus und bring eigene, neue Inhalte ein.\n- Der Auftrag hat Vorrang. Die Einträge sind das Fundament, nicht die Wand.</string>
```

### 4c.10 Slot-Laengen-Stichprobe

Format pro Zeile: `<Laenge> | <Key> | "<Wortlaut>"` — sortiert nach Laenge

**Laengste Buttons / CTAs in Hauptsprache (Top 15, Soft-Limit ~18 Zeichen DE):**
```
 51 | ai_prompt_rerank_actions_header                    | "=== AKTUELLES TOP-MASSNAHMEN-ARRAY (5 Eintr�ge) ==="
 47 | retro_intro_cta                                    | "Schau zur�ck und entdecke, was dich bewegt hat."
 22 | paywall_restore_button                             | "K�ufe wiederherstellen"
 20 | follow_up_add_button                               | "Erg�nzung hinzuf�gen"
 19 | ai_retro_generate_button                           | "R�ckblick erstellen"
 17 | action_retry                                       | "Nochmal versuchen"
 16 | dashboard_top_5_actions                            | "Top 5 Massnahmen"
 14 | action_go_google_play                              | "Zu Google Play"
 14 | json_key_top_actions                               | "top_massnahmen"
 11 | settings_export_action                             | "Exportieren"
 10 | action_understood                                  | "Verstanden"
 10 | action_no_thanks                                   | "Nein danke"
 10 | action_undo                                        | "R�ckg�ngig"
 10 | biometric_unlock_button                            | "Entsperren"
 10 | follow_up_improve_button                           | "Verbessern"
```

**Laengste Push-Notification-Titles in Hauptsprache (Top 10, Soft-Limit ~65 Zeichen DE):**
```
 22 | notif_reminder_title                               | "Zeit f�r dein Tagebuch"
 20 | notif_weekly_title                                 | "Dein Wochenr�ckblick"
 20 | notif_monthly_title                                | "Dein Monatsr�ckblick"
 20 | notif_yearly_title                                 | "Dein Jahresr�ckblick"
```

**Laengste TopBar-Titles in Hauptsprache (Top 10, Soft-Limit ~25 Zeichen DE):**
```
210 | ai_retro_week_title_prompt                         | "Basierend auf diesem Wochenr�ckblick, gib einen kurzen, emotionalen Titel (max 6 W�rter) in %1$s.\nNur den Titel ausgeben, nichts anderes. Keine Anf�hrungszeichen. Keine Gedankenstriche (�).\n\nR�ckblick:\n%2$s"
210 | ai_retro_month_title_prompt                        | "Basierend auf diesem Monatsr�ckblick, gib einen kurzen, emotionalen Titel (max 6 W�rter) in %1$s.\nNur den Titel ausgeben, nichts anderes. Keine Anf�hrungszeichen. Keine Gedankenstriche (�).\n\nR�ckblick:\n%2$s"
210 | ai_retro_year_title_prompt                         | "Basierend auf diesem Jahresr�ckblick, gib einen kurzen, emotionalen Titel (max 6 W�rter) in %1$s.\nNur den Titel ausgeben, nichts anderes. Keine Anf�hrungszeichen. Keine Gedankenstriche (�).\n\nR�ckblick:\n%2$s"
164 | settings_ccpa_do_not_sell_subtitle                 | "Nur relevant f�r Einwohner des US-Bundesstaats Kalifornien. Bei Aktivierung werden alle optionalen Cloud-Funktionen deaktiviert � nur lokale Features bleiben aktiv."
106 | settings_delete_account_subtitle                   | "Entfernt alle lokalen Daten und das Drive-Backup unwiderruflich. Dein Google-Konto selbst bleibt bestehen."
 94 | privacy_sheet_subtitle                             | "Aktiviere einzelne Funktionen so, wie es f�r dich passt. Alles kann jederzeit ge�ndert werden."
 60 | settings_analytics_subtitle                        | "Firebase Analytics f�r Fehleranalyse und Produktverbesserung"
 59 | onboarding_subtitle                                | "Dein pers�nliches KI-Tagebuch\nf�r Klarheit und Ver�nderung"
 52 | settings_ai_groq_subtitle                          | "Sprachaufnahmen zur Transkription an Groq USA senden"
 51 | settings_ai_gemini_subtitle                        | "Texte f�r KI-Zusammenfassungen an Google USA senden"
```

## Schicht 4d — Legal-Text-Inventar

Vorarbeit fuer den Rechtssicherheits-Skill.

### 4d.1 Permission-Rationale-Indikatoren

| Indikator | Anzahl |
|-----------|--------|
| Dateien mit Runtime-Permission-Aufrufen | 0 |
| Permission-/Rationale-Strings in strings.xml | 0 |

### 4d.2 Consent-Banner-Indikatoren

| Indikator | Anzahl |
|-----------|--------|
| Google UMP (User Messaging Platform) | 0 |
| Firebase Consent-API | 6 |
| Custom Consent-Komponenten | 8 |
| Consent-Strings in strings.xml | 44 |

### 4d.3 Rechtstexte (Links + Strings)

| Rechtstext | String-Treffer |
|-----------|----------------|
| AGB / Terms / Nutzungsbedingungen | 1 |
| Datenschutz / Privacy | 21 |
| Impressum / Imprint | 1 |
| Widerruf / Withdrawal | 0 |
| URL-Strings (http/https) | 1 |

**URLs in strings.xml (Stichprobe):**
```
    <string name="settings_legal_online_url" translatable="false">https://pepsi1978.github.io/proggs/bestjournal/</string>
```

### 4d.4 Health-Indikatoren

| Indikator | Anzahl |
|-----------|--------|
| Health Connect / GoogleFit SDK | 0 |
| Health-Begriffe in Strings | 7 |
| Disclaimer-Strings | 1 |

### 4d.5 AI-Indikatoren

| Indikator | Anzahl |
|-----------|--------|
| AI-SDK-Aufrufe | 6 |
| AI-Disclaimer-Strings | 0 |

**⚠ AI-SDK verwendet aber KEINE Disclaimer-Strings gefunden** — EU AI Act Pflicht!

### 4d.6 Werbe-Indikatoren

| Indikator | Anzahl |
|-----------|--------|
| Ad-SDK-Aufrufe | 0 |
| AD_ID Permission im Manifest | 0 |
| Werbe-Markierungs-Strings | 0 |

### 4d.7 Account-Deletion (DSGVO Art. 17)

| Indikator | Anzahl |
|-----------|--------|
| Loeschungs-Code-Dateien | 2 |
| Loeschungs-Strings | 12 |

### 4d.8 Standort-Verwendung

| Indikator | Anzahl |
|-----------|--------|
| Location-Permissions im Manifest | 1 |
| LocationManager/FusedLocation-Code | 0 |

**⚠ Location-Permission deklariert aber NIE verwendet** — aus Manifest entfernen!

## Schicht 4e — Externe Inhalte

Vorarbeit fuer den Audit. Frank muss zusaetzlich Inhalte ausserhalb des Repos beitragen.

### 4e.1 Fastlane Play-Store-Metadaten (falls vorhanden)

Fastlane-Sprachen-Verzeichnisse: 0

### 4e.2 Firebase Remote Config

| Indikator | Anzahl |
|-----------|--------|
| RemoteConfig-Code-Dateien | 1 |
| Default-Wert-XML-Dateien | 0 |

**Remote-Config-Keys im Code:**
```
```

### 4e.3 Cloud Functions

Functions-Verzeichnis: `./functions`

Notification-Send-Aufrufe: 58

### 4e.4 WebView-Inhalte

| Indikator | Anzahl |
|-----------|--------|
| WebView-Komponenten im Code | 1 |
| HTML-Dateien in assets/ | 81 |
| Markdown-Dateien in assets/ | 0 |

**Asset-HTML-Dateien:**
```
./app/src/main/assets/legal/ar/IMPRINT.html
./app/src/main/assets/legal/ar/PRIVACY.html
./app/src/main/assets/legal/ar/TERMS.html
./app/src/main/assets/legal/bn/IMPRINT.html
./app/src/main/assets/legal/bn/PRIVACY.html
./app/src/main/assets/legal/bn/TERMS.html
./app/src/main/assets/legal/de/DATENSCHUTZ.html
./app/src/main/assets/legal/de/IMPRESSUM.html
./app/src/main/assets/legal/de/NUTZUNGSBEDINGUNGEN.html
./app/src/main/assets/legal/en/IMPRINT.html
./app/src/main/assets/legal/en/PRIVACY.html
./app/src/main/assets/legal/en/TERMS.html
./app/src/main/assets/legal/es/IMPRINT.html
./app/src/main/assets/legal/es/PRIVACY.html
./app/src/main/assets/legal/es/TERMS.html
./app/src/main/assets/legal/fr/IMPRINT.html
./app/src/main/assets/legal/fr/PRIVACY.html
./app/src/main/assets/legal/fr/TERMS.html
./app/src/main/assets/legal/gu/IMPRINT.html
./app/src/main/assets/legal/gu/PRIVACY.html
./app/src/main/assets/legal/gu/TERMS.html
./app/src/main/assets/legal/hi/IMPRINT.html
./app/src/main/assets/legal/hi/PRIVACY.html
./app/src/main/assets/legal/hi/TERMS.html
./app/src/main/assets/legal/id/IMPRINT.html
./app/src/main/assets/legal/id/PRIVACY.html
./app/src/main/assets/legal/id/TERMS.html
./app/src/main/assets/legal/it/IMPRINT.html
./app/src/main/assets/legal/it/PRIVACY.html
./app/src/main/assets/legal/it/TERMS.html
./app/src/main/assets/legal/ja/IMPRINT.html
./app/src/main/assets/legal/ja/PRIVACY.html
./app/src/main/assets/legal/ja/TERMS.html
./app/src/main/assets/legal/kn/IMPRINT.html
./app/src/main/assets/legal/kn/PRIVACY.html
./app/src/main/assets/legal/kn/TERMS.html
./app/src/main/assets/legal/ko/IMPRINT.html
./app/src/main/assets/legal/ko/PRIVACY.html
./app/src/main/assets/legal/ko/TERMS.html
./app/src/main/assets/legal/ml/IMPRINT.html
./app/src/main/assets/legal/ml/PRIVACY.html
./app/src/main/assets/legal/ml/TERMS.html
./app/src/main/assets/legal/mr/IMPRINT.html
./app/src/main/assets/legal/mr/PRIVACY.html
./app/src/main/assets/legal/mr/TERMS.html
./app/src/main/assets/legal/nl/IMPRINT.html
./app/src/main/assets/legal/nl/PRIVACY.html
./app/src/main/assets/legal/nl/TERMS.html
./app/src/main/assets/legal/pl/IMPRINT.html
./app/src/main/assets/legal/pl/PRIVACY.html
./app/src/main/assets/legal/pl/TERMS.html
./app/src/main/assets/legal/pt-BR/IMPRINT.html
./app/src/main/assets/legal/pt-BR/PRIVACY.html
./app/src/main/assets/legal/pt-BR/TERMS.html
./app/src/main/assets/legal/pt-PT/IMPRINT.html
./app/src/main/assets/legal/pt-PT/PRIVACY.html
./app/src/main/assets/legal/pt-PT/TERMS.html
./app/src/main/assets/legal/ta/IMPRINT.html
./app/src/main/assets/legal/ta/PRIVACY.html
./app/src/main/assets/legal/ta/TERMS.html
./app/src/main/assets/legal/te/IMPRINT.html
./app/src/main/assets/legal/te/PRIVACY.html
./app/src/main/assets/legal/te/TERMS.html
./app/src/main/assets/legal/th/IMPRINT.html
./app/src/main/assets/legal/th/PRIVACY.html
./app/src/main/assets/legal/th/TERMS.html
./app/src/main/assets/legal/tr/IMPRINT.html
./app/src/main/assets/legal/tr/PRIVACY.html
./app/src/main/assets/legal/tr/TERMS.html
./app/src/main/assets/legal/uk/IMPRINT.html
./app/src/main/assets/legal/uk/PRIVACY.html
./app/src/main/assets/legal/uk/TERMS.html
./app/src/main/assets/legal/ur/IMPRINT.html
./app/src/main/assets/legal/ur/PRIVACY.html
./app/src/main/assets/legal/ur/TERMS.html
./app/src/main/assets/legal/zh-CN/IMPRINT.html
./app/src/main/assets/legal/zh-CN/PRIVACY.html
./app/src/main/assets/legal/zh-CN/TERMS.html
./app/src/main/assets/legal/zh-TW/IMPRINT.html
./app/src/main/assets/legal/zh-TW/PRIVACY.html
./app/src/main/assets/legal/zh-TW/TERMS.html
```

### 4e.5 PDF-Export

| Indikator | Anzahl |
|-----------|--------|
| PDF-Generierungs-Code | 1 |

### 4e.6 Customer-Support-System

Kein externes Customer-Support-System erkannt (vermutlich Email-only oder kein Support).

## Schicht 4b — Erweiterte UI-Komponenten (Bereiche 13-20)

| UI-Komponente | Treffer |
|---------------|---------|
| TextField (Dateien mit TextField/OutlinedTextField) | 5 |
|   davon label-Slots | 3 |
|   davon placeholder-Slots | 6 |
|   davon supportingText-Slots | 0 |
| Chips (Filter/Assist/Input/Suggestion) | 1 |
| Material 3 Tooltips | 0 |
| SearchBar / DockedSearchBar | 0 |
| semantics-Block (a11y) | 0 |
| Slider / RangeSlider | 0 |
| DatePicker | 0 |
| TimePicker | 1 |
| AnnotatedString-Builder | 2 |

---

## Hinweise fuer die Tiefenanalyse

Dieser Initial-Scan ist die Vorarbeit. Die Tiefenanalyse muss:
1. Jede der 7 Schichten gemaess `references/layer-N-*.md` durchlaufen
2. **Schicht 4b vollstaendig ausfuehren** — fuer JEDEN Bereich (20 Bereichstypen inkl. TextField-Slots, Chips, Tooltips, SearchBar, semantics, Slider, Picker, AnnotatedString) eine eigene 1:1-Wortlaut-Tabelle erstellen
3. **Menues rekursiv ausrollen** — JEDE Untermenue-Ebene mit Breadcrumb-Pfad als eigene Zeile, beliebige Tiefe, keine Abkuerzung
4. **Schicht 4c vollstaendig ausfuehren** — Slot-Laengen, translatable=false, xliff:g, Uebersetzer-Notes, CLDR-Plurals, HTML/CDATA, Format-Args, Glossar, Region-Differenzen, Du/Sie-Konsistenz
5. **Schicht 4d vollstaendig ausfuehren** — Permission-Rationale, Consent, Rechtstexte, Health/AI/Werbe-Disclaimer, Account-Deletion, Newsletter, Widerruf, Standort, Altersfreigabe
6. **Schicht 4e vollstaendig ausfuehren** — Play-Store-Listing, Remote-Config-Live-Werte, Cloud-Functions-Templates, Email-Templates, WebView-Inhalte, PDF-Vorlagen, Marketing-Material (Frank-Aufgaben dokumentieren)
7. Pro Befund Datei + Zeilennummer als Beleg liefern
8. Die Don't-Miss-Checkliste (`references/dont-miss-checklist.md`) abschliessend pruefen — inkl. Block I, J, K, L
9. Den finalen Bericht nach `assets/audit-report-template.md` strukturieren

**Naechster Schritt:** Claude liest diesen Initial-Scan und arbeitet die Schichten 1-7 (inkl. 4b, 4c, 4d, 4e) detailliert durch.
