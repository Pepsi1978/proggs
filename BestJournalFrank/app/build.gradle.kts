plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// SK — Secret Keys Zentrale (cross-platform: $HOME/SK/BestJournalFrank/).
// Alle Secrets (debug-shared.keystore) liegen dort und werden beim Build kopiert.
val skBase: File = File(System.getProperty("user.home")).resolve("SK").resolve("BestJournalFrank")

val syncCopies: List<Pair<File, File>> =
    listOf(
        skBase.resolve("debug-shared.keystore") to rootProject.file("debug-shared.keystore"),
    )

val syncSecretsFromSk =
    tasks.register("syncSecretsFromSk") {
        val sk = skBase
        val copies = syncCopies
        doLast {
            if (!sk.isDirectory) {
                throw GradleException(
                    "SK-Ordner fehlt: ${sk.absolutePath}\n" +
                        "Erwartete Inhalte: debug-shared.keystore\n" +
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
    namespace = "com.entropyjournal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.entropyjournal"
        minSdk = 26
        targetSdk = 35
        versionCode = 154
        versionName = "0.23.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug-shared.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

    // Lucide Icons
    implementation(libs.lucide.icons)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
