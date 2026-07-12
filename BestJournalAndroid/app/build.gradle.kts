import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// SK — Secret Keys Zentrale (cross-platform: $HOME/SK/BestJournalAndroid/).
// Alle Secrets (google-services.json, keystores, keystore.properties) liegen dort.
// syncSecretsFromSk-Task kopiert sie beim Build an die erwarteten Pfade.
val skBase: File = File(System.getProperty("user.home")).resolve("SK").resolve("BestJournalAndroid")

// Pfade zur Configuration-Zeit aufloesen (Configuration-Cache-kompatibel).
val syncCopies: List<Pair<File, File>> =
    listOf(
        skBase.resolve("google-services-debug.json") to
            project.file("src/debug/google-services.json"),
        skBase.resolve("google-services-release.json") to project.file("google-services.json"),
        skBase.resolve("debug-shared.keystore") to rootProject.file("debug-shared.keystore"),
        skBase.resolve("release.keystore") to rootProject.file("release.keystore"),
    )

val syncSecretsFromSk =
    tasks.register("syncSecretsFromSk") {
        val sk = skBase
        val copies = syncCopies
        doLast {
            if (!sk.isDirectory) {
                throw GradleException(
                    "SK-Ordner fehlt: ${sk.absolutePath}\n" +
                        "Erwartete Inhalte: google-services-debug.json, google-services-release.json, " +
                        "debug-shared.keystore, release.keystore, keystore.properties\n" +
                        "Siehe ~/SK/README.md fuer Details."
                )
            }
            copies.forEach { (src, dst) ->
                if (!src.exists()) throw GradleException("SK-Datei fehlt: ${src.absolutePath}")
                dst.parentFile.mkdirs()
                src.copyTo(dst, overwrite = true)
            }
        }
    }

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(syncSecretsFromSk) }

android {
    namespace = "com.bestjournal.app"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug-shared.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystoreProps = skBase.resolve("keystore.properties")
            if (keystoreProps.exists()) {
                val props = Properties()
                keystoreProps.inputStream().use { props.load(it) }
                val storeFileValue = props.getProperty("RELEASE_STORE_FILE", "")
                val resolvedStoreFile =
                    File(storeFileValue).let {
                        if (it.isAbsolute) it else skBase.resolve(storeFileValue)
                    }
                storeFile = resolvedStoreFile
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD", "")
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS", "")
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    defaultConfig {
        applicationId = "com.bestjournal.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 298
        versionName = "0.21.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            // R8/ProGuard AKTIV (2026-05-31, PS-001 Release-Hardening):
            // Code-Shrinking + Obfuskation + Resource-Shrinking eingeschaltet.
            // Keep-Regeln fuer Hilt/Room/Moshi/Retrofit/Firebase/Gemini/sherpa-onnx
            // liegen in proguard-rules.pro. Bei Laufzeit-Crashes durch entfernte
            // Reflection-Klassen: dort gezielt -keep ergaenzen.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    testOptions { unitTests.all { it.useJUnitPlatform() } }

    androidResources { noCompress += listOf("onnx", "txt") }
}

// Kotlin 2.x: jvmTarget via compilerOptions (kotlinOptions {} wird in Kotlin 2.2 ein Fehler).
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.google.fonts)
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

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Auth & Drive
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.google.id)
    implementation(libs.google.api.client.android)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation(libs.google.drive.api)

    // Security
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // Sherpa-ONNX (local Whisper speech-to-text)
    implementation(files("libs/sherpa-onnx-1.12.34.aar"))

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Animation
    implementation(libs.lottie.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Firebase AI Logic
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.functions)

    // Google Play Billing
    implementation(libs.play.billing)

    // Google Play In-App Review
    implementation(libs.play.review)

    // Unit Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
