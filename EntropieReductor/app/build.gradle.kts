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
val skKeystoreSrc: JFile = skBase.resolve("entropiereductor.debug.keystore")
val rootKeystoreDst: JFile = rootProject.file("debug-shared.keystore")

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
    compileSdk = 35

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
        targetSdk = 35
        versionCode = 9
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OAuth-Redirect-Scheme fuer AppAuth (Whoop + Google Calendar)
        manifestPlaceholders["appAuthRedirectScheme"] = "de.frank.entropyreducer"

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
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
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

    // Pruefe Internet-Konnektivitaet
    implementation(libs.core.ktx)

    // Tests (Schichtcode-Parser + UseCases)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
}
