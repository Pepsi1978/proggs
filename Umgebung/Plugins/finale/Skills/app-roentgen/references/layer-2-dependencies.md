# Schicht 2 — Dependency-Analyse: Was kann die App technisch?

> **FIX AA1 (Audit 10) — Kotlin + Java:** Die Patterns in dieser Datei (z.B. RemoteConfig-Code-Nutzung auf Zeile 163) zeigen aus Lesbarkeitsgruenden nur `--include='*.kt'`. Bei Apps mit Java-Anteilen MUSS `--include='*.java'` ergaenzt werden — Java-Legacy-Module rufen Firebase RemoteConfig, Crashlytics & andere SDKs haeufig aus `.java`-Dateien auf. Reine Kotlin-Apps koennen den Java-Filter weglassen.

## Zweck

Jede Bibliothek im Build-System impliziert technische Faehigkeiten. Eine App mit `firebase-messaging` MUSS Push-Notifications koennen, eine App mit `play-billing` MUSS In-App-Purchases unterstuetzen. Die Dependency-Liste ist die "Capability-Karte" der App.

**Coverage-Beitrag: ~25 Prozent** — zeigt was MOEGLICH ist (nicht zwingend was AKTIV ist).

## Pfade zu pruefen

```
app/build.gradle.kts          # App-Modul-Dependencies
build.gradle.kts              # Root-Project (Plugin-Versionen)
settings.gradle.kts           # Plugin-Repositories
gradle/libs.versions.toml     # Version-Catalog (modernes Setup)
```

Bei Multi-Module-Setup zusaetzlich pro Modul:
```
core/build.gradle.kts
data/build.gradle.kts
domain/build.gradle.kts
feature-*/build.gradle.kts
```

## Master-Grep fuer Dependencies

```bash
# Alle implementation/api Lines extrahieren
grep -E '^\s+(implementation|api|kapt|ksp|debugImplementation|releaseImplementation)\s' \
  $(find . -name 'build.gradle.kts' -not -path '*/build/*') \
  | sort -u

# Aus libs.versions.toml
grep -E '^[a-zA-Z]' gradle/libs.versions.toml
```

## Capability-Mapping (Pflicht-Tabelle)

### Firebase-Familie

| Dependency | Capability | Audit-Aufmerksamkeit |
|-----------|-----------|---------------------|
| firebase-bom | Versions-Container | Stand pruefen (mind. 33+) |
| firebase-analytics | Tracking, Custom Events | DSGVO-Consent erforderlich |
| firebase-crashlytics | Crash-Reporting | DSGVO-Pflichtinfo |
| firebase-messaging | FCM Push-Notifications | AndroidManifest-Service noetig |
| firebase-remote-config | Feature-Flags / A/B-Tests | Versteckte Features moeglich |
| firebase-auth | Authentifizierung | Anbieter pruefen |
| firebase-firestore / firebase-database | Cloud-DB | DSGVO-Datenfluss |
| firebase-functions | Cloud Functions Aufruf | Server-Side Logik |
| firebase-installations | Installation-IDs | DSGVO-relevante ID |
| firebase-perf | Performance-Tracking | DSGVO-relevant |
| firebase-config-ktx + firebase-ai | Gemini-API ueber Firebase | KI-Capability |
| firebase-appcheck-* | App-Integrity | Sicherheits-Feature |
| firebase-dynamic-links | Deep-Links | Entry-Points pruefen |

### Google Play / Billing

| Dependency | Capability |
|-----------|-----------|
| com.android.billingclient:billing-ktx | Google Play Billing v6/7/v8 — Subscription/IAP. **Bei v8 (06/2025) wurden APIs entfernt — siehe `layer-5-paywall.md` Abschnitt 5.0 fuer Migration-Check.** |
| com.google.android.play:review-ktx | In-App-Review-Dialog |
| com.google.android.play:app-update-ktx | In-App-Updates |
| com.google.android.play:integrity | Play Integrity API |
| com.google.android.play:asset-delivery | Play Asset Delivery (modulare Assets) |

### Persistenz

| Dependency | Capability |
|-----------|-----------|
| androidx.room:room-runtime | Lokale SQLite-DB |
| androidx.room:room-ktx | Coroutines-Support fuer Room |
| androidx.datastore:datastore-preferences | Settings/Preferences |
| androidx.datastore:datastore | Typed Datastore |

### Background-Work

| Dependency | Capability |
|-----------|-----------|
| androidx.work:work-runtime-ktx | WorkManager Background-Jobs |
| androidx.work:work-multiprocess | WorkManager Multiprocess |
| androidx.startup:startup-runtime | App-Startup-Initialisierung |

### Network

| Dependency | Capability |
|-----------|-----------|
| com.squareup.retrofit2:retrofit | REST-API-Client |
| com.squareup.okhttp3:okhttp | HTTP-Client (oft transitiv) |
| io.ktor:ktor-client-* | KMP-faehiger HTTP-Client |
| com.squareup.okhttp3:logging-interceptor | Network-Debug |

### UI / Compose

| Dependency | Capability |
|-----------|-----------|
| androidx.compose.material3 | Material 3 UI |
| androidx.navigation:navigation-compose | Compose Navigation |
| androidx.compose.runtime:runtime | Reaktive UI |
| io.coil-kt:coil-compose / coil3 | Bild-Loading |
| com.google.accompanist:accompanist-permissions | Runtime-Permission-Dialoge |
| androidx.compose.material:material-icons-extended | Material-Icons-Set |

### KI/ML

| Dependency | Capability |
|-----------|-----------|
| com.google.mlkit:* | On-Device-ML (Text-Recognition, Face, Barcode, Translation) |
| com.google.android.gms:play-services-mlkit-* | MLKit ueber GMS |
| Gemini-API direkt (over Retrofit/Ktor) | Cloud-LLM |
| OpenAI-API (firmeneigen) | Cloud-LLM |
| TensorFlow-Lite | On-Device-ML |
| onnxruntime-android | On-Device-ML |
| whisper.cpp / whisper-android | Lokales Whisper |

### Authentifizierung / Security

| Dependency | Capability |
|-----------|-----------|
| androidx.biometric:biometric | Fingerabdruck/Face-Unlock |
| androidx.security:security-crypto | EncryptedSharedPreferences |
| androidx.credentials:credentials | Passkey-Support |
| com.google.android.gms:play-services-auth | Google-Sign-In |

### Health & Wearable

| Dependency | Capability |
|-----------|-----------|
| androidx.health.connect:connect-client | Health Connect |
| com.google.android.gms:play-services-fitness | Google Fit (legacy) |
| com.google.android.gms:play-services-wearable | Wear-OS-Integration |

### Hilt / Dependency Injection

| Dependency | Capability |
|-----------|-----------|
| com.google.dagger:hilt-android | Hilt DI |
| androidx.hilt:hilt-work | WorkManager-DI |
| androidx.hilt:hilt-navigation-compose | Compose-Navigation-DI |

### Coroutines

| Dependency | Capability |
|-----------|-----------|
| org.jetbrains.kotlinx:kotlinx-coroutines-android | Async-Calls |
| org.jetbrains.kotlinx:kotlinx-coroutines-play-services | Tasks <-> Coroutines |

### Werbung / Monetarisierung (nicht-Subscription)

| Dependency | Capability |
|-----------|-----------|
| com.google.android.gms:play-services-ads | AdMob-Werbung |
| com.applovin:applovin-sdk | AppLovin-Mediation |
| com.facebook.android:audience-network-sdk | Meta Audience Network |
| com.unity3d.ads:unity-ads | Unity Ads |

**Audit-Hinweis:** Wenn Werbung drin ist und die App in der Beschreibung "ad-free" oder "werbefrei" verspricht — KRITISCHER Befund.

## Pruef-Pflicht: Dependency genutzt vs. tot?

```bash
# Beispiel fuer Firebase Remote Config
grep -rn "RemoteConfig\|FirebaseRemoteConfig" --include='*.kt' .

# Wenn 0 Treffer: Dependency ist im Build aber wird nicht genutzt
# → "Toter Dependency" — kein Audit-Befund, aber verdaechtig
```

Pflicht-Pruefung fuer JEDE deklarierte Dependency:
1. Wird sie im Code importiert (`import com.google.firebase.remoteconfig...`)?
2. Wird mindestens eine ihrer Hauptklassen instanziiert?
3. Falls Hilt: Gibt es ein `@Provides` das die Klasse zurueckgibt?

Wenn alle drei Antworten "Nein" — Dependency ist tot oder hinter Feature-Flag versteckt.

## Plugin-Analyse

```bash
grep -E 'apply\(|alias\(|id\("' app/build.gradle.kts | head -20
```

Wichtige Plugins:
- `com.google.gms.google-services` → Firebase aktiv
- `com.google.firebase.crashlytics` → Crashlytics aktiv
- `com.google.firebase.firebase-perf` → Performance-Monitoring aktiv
- `kotlin-parcelize` → @Parcelize-Datenklassen
- `kotlin-kapt` / `com.google.devtools.ksp` → Annotation-Processing
- `dagger.hilt.android.plugin` → Hilt aktiv
- `com.android.dynamic-feature` → Modular gelieferte Features

## Build-Variants und Flavors

```bash
grep -A30 'productFlavors' app/build.gradle.kts
grep -A10 'buildTypes' app/build.gradle.kts
```

Flavors zeigen unterschiedliche App-Varianten (free vs paid, dev vs prod, etc.). Jede Flavor kann eigene Features haben — wenn vorhanden, ALLE Flavors auditieren.

## Output-Format fuer Schicht 2

```markdown
## Schicht 2 — Dependency-Analyse

### Build-System
- Gradle: 8.x
- AGP (Android Gradle Plugin): 8.x
- Kotlin: 2.x
- Compose Compiler: x.x
- Min SDK: 24 — Target SDK: 35

### Aktive Plugins
- com.google.gms.google-services (Firebase aktiv)
- dagger.hilt.android.plugin (Hilt DI)
- ...

### Feature-Cluster aus Dependencies

#### Firebase (N Pakete)
- analytics — aktive Nutzung in AnalyticsTracker.kt
- crashlytics — automatische Initialisierung
- messaging — FCM-Service registriert
- remote-config — keine Code-Nutzung gefunden — VERDAECHTIG / TOT
- ...

#### Persistenz
- Room 2.6.x — N Entities, N DAOs (Detail in Schicht 3)
- DataStore — N PreferenceKeys (Detail in Schicht 6)

#### KI/ML
- ...

### Build-Variants und Flavors
- buildTypes: debug, release, staging
- productFlavors: keine ODER ...

### Tote/Verdaechtige Dependencies
| Dependency | Grund |
|-----------|-------|
| com.example:foo | Kein Import gefunden |
| ... | ... |
```

## Typische Fehlerquellen

- **Version-Catalog uebersehen**: Bei modernen Setups stehen Dependencies in `libs.versions.toml`, nicht direkt im build.gradle.
- **Transitive Dependencies vergessen**: `com.google.firebase:firebase-bom` zieht alle Firebase-Versionen. Wichtig fuer Cross-Reference, aber selten Audit-relevant.
- **`dependencyResolutionManagement` verschoben**: Bei Composite-Builds kann die Repos-Konfig in `settings.gradle.kts` liegen.
- **Sub-Module-Dependencies vergessen**: Bei Multi-Module muss jedes Modul geprueft werden.
- **buildSrc oder Convention-Plugins**: Manche Projekte definieren Dependencies in `buildSrc/` oder Plugin-Files unter `build-logic/`.
