import java.io.File as JFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Secrets liegen ausserhalb des Repos in $HOME/SK/EntropieReductor/.
// Der syncSecretsFromSk-Task kopiert die Debug-Keystore vor jedem Build in den
// Projekt-Root (rootProject) als debug-shared.keystore — der dortige Pfad ist in
// .gitignore ausgeschlossen, kommt also nie ins Git-Repo.
val skBase: JFile = JFile(System.getProperty("user.home"))
    .resolve("SK").resolve("EntropieReductor")
val skKeystoreSrc: JFile = skBase.resolve("debug-shared.keystore")
val rootKeystoreDst: JFile = rootProject.file("debug-shared.keystore")

// Frank-Wunsch 2026-05-17: Google-Maps-API-Key fuer Satelliten-Karte im
// Trainings-Detail. Liegt als Klartext in ~/SK/EntropieReductor/maps-api-key.txt
// (eine Zeile, nur der Key). Bei Build wird er hier gelesen und als
// manifestPlaceholder gesetzt — landet damit nie im Repo.
val skMapsKeyFile: JFile = skBase.resolve("maps-api-key.txt")
val mapsApiKey: String = if (skMapsKeyFile.exists()) {
    skMapsKeyFile.readText().trim()
} else {
    // Fallback: leerer Key. Karte zeigt grauen Hintergrund + "For development
    // purposes only"-Wasserzeichen, Build laeuft aber durch. Sobald die
    // Datei existiert (auch leer + neu = irgendein String), wird die Karte
    // korrekt gerendert.
    ""
}

val syncSecretsFromSk = tasks.register("syncSecretsFromSk") {
    val src = skKeystoreSrc
    val dst = rootKeystoreDst
    val sk = skBase
    doLast {
        if (!sk.isDirectory) {
            throw GradleException(
                "SK-Ordner fehlt: ${sk.absolutePath}\n" +
                    "Erwartet: entropiereductor.debug.keystore\n" +
                    "Siehe ~/SK/EntropieReductor/README.md."
            )
        }
        if (!src.exists()) {
            throw GradleException("SK-Datei fehlt: ${src.absolutePath}")
        }
        dst.parentFile.mkdirs()
        src.copyTo(dst, overwrite = true)
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(syncSecretsFromSk) }

android {
    namespace = "de.frank.entropyreducer"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            storeFile = rootKeystoreDst
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "de.frank.entropyreducer"
        minSdk = 28
        targetSdk = 36
        versionCode = 315
        versionName = "0.28.16"
        buildConfigField("String", "VERSION_UPDATED_AT", "\"14.07.2026, 13:14 Uhr\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OAuth-Redirect-Scheme fuer AppAuth (Whoop + Google Calendar)
        manifestPlaceholders["appAuthRedirectScheme"] = "de.frank.entropyreducer"
        // Google-Maps-API-Key (Frank-Wunsch 2026-05-17) — Wert wird oben aus
        // ~/SK/EntropieReductor/maps-api-key.txt gelesen, nie ins Repo committed.
        manifestPlaceholders["mapsApiKey"] = mapsApiKey

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "VARIANT_LABEL", "\"Release Version\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            buildConfigField("String", "VARIANT_LABEL", "\"Debugversion\"")
        }
        // Frank-Wunsch 2026-05-09 (FINAL): Es gibt immer nur EINE App auf dem
        // Geraet. Benchmark- und Debug-Variante haben den GLEICHEN
        // applicationId (.debug Suffix), sodass eine die andere beim Install
        // ueberschreibt und alle Daten/Keys/Backups erhalten bleiben (gleicher
        // applicationId = Android haelt die Daten). Frank wechselt je nach
        // Bedarf: Standard ist Debug (schneller Workflow), Performance wird
        // bei Bedarf drueber installiert wenn fluessige Performance gewollt.
        // KEINE separaten Apps (.slow, .bench) wegen Google-Cloud-Console-
        // Konfiguration (OAuth-Code-10-Developer-Error fuer unregistrierte IDs).
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            isDebuggable = false
            matchingFallbacks += "release"
            buildConfigField("String", "VARIANT_LABEL", "\"Performance Version\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // PERFORMANCE 2026-05-09: Compose Compiler Reports aktivieren — schreibt
    // beim Build pro Modul eine Datei mit allen nicht-skippable Composables
    // und allen unstable Parametern in build/compose_compiler/. Goldquelle
    // der Wahrheit fuer Compose-Performance-Probleme.
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Lint-Baseline (E2-Audit 2026-05-10): unterdrueckt 2 False-Positives aus
    // google-http-client-1.45.0.jar (MockHttpTransport-Klasse, in unserem Code
    // nirgendwo verkabelt). Siehe lint-baseline.xml fuer Audit-Details.
    lint {
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

// Kotlin 2.x: jvmTarget + opt-ins via compilerOptions (kotlinOptions {} wird in Kotlin 2.2 ein Fehler).
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    // ProcessLifecycleOwner — feuert bei jedem App-Foreground-Wechsel,
    // damit Whoop-Sync auch nach App-im-Hintergrund-zurueckholen laeuft.
    implementation(libs.lifecycle.process)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // EncryptedSharedPreferences (laut Spec — fuer API-Keys + OAuth-Tokens)
    implementation(libs.security.crypto)

    // OAuth (AppAuth) — Whoop + Google Calendar
    implementation(libs.appauth)

    // Media3 ExoPlayer fuer TTS-Wiedergabe
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)

    // WorkManager (Hintergrund-Sync)
    implementation(libs.work.runtime)

    // Stufe 4: Home-Screen-Widget (Glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Remote Compose (Frank-Wunsch 2026-05-31): WIEDER ENTFERNT — alpha11 erzwingt
    // AGP 9.1.0 (Projekt: 8.10.0), minSdk 29 (Projekt: 28 → wuerde Android-9-Geraete
    // ausschliessen) und compileSdk 37 (Projekt: 36). Eine ungenutzte Vorbereitung ist
    // diese Anforderungen + den Geraete-Ausschluss nicht wert. Sobald Remote Compose
    // reif ist (stabile Glance-Widget-Anbindung), hier wieder aktivieren — die
    // Catalog-Eintraege stehen auskommentiert in gradle/libs.versions.toml bereit.

    // Vico Charts (Stufe 2-4)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Google Drive Backup/Restore (Stufe 1)
    implementation(libs.google.api.client.android)
    implementation(libs.google.drive.api)
    implementation(libs.play.services.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.google.id)

    // Reorderable LazyColumn (Drag & Drop fuer Biomarker-Karten — Frank-Wunsch 2026-05-10)
    implementation(libs.reorderable)

    // Health Connect — liest Gewicht aus der Zepp-App-Bruecke (Frank-Wunsch 2026-05-10)
    implementation(libs.health.connect.client)

    // Google Maps Compose (Frank-Wunsch 2026-05-17) — Satelliten-Karte mit
    // eingezeichneter Strecke im Trainings-Detail-Screen.
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    // RFC-5545 RRULE-Parser (Sprint 2, Frank-Wunsch 2026-05-22) — wiederkehrende Aufgaben.
    // Apache 2.0, ~120kb. Bietet RecurrenceRule + RecurrenceRuleIterator zum Berechnen
    // naechster Vorkommen. Wird nur von RecurringTemplateRepository/UseCase importiert.
    implementation(libs.lib.recur)

    // WireGuard-Tunnel fuer private Second-Brain-Anbindung wie in CortexAndroid.
    implementation(libs.wireguard.tunnel)

    // Pruefe Internet-Konnektivitaet
    implementation(libs.core.ktx)

    // Tests (Schichtcode-Parser + UseCases)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
}
