plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.frank.cortex"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.frank.cortex"
        minSdk = 26
        targetSdk = 35
        versionCode = 77
        versionName = "0.5.5"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"06.07.2026, 17:55 Uhr\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // Gemeinsamer Debug-Keystore (cross-platform: liegt in ~/SK und auf Laufwerk Y).
            // Faellt auf den lokalen Standard-Keystore zurueck, falls die Datei fehlt.
            val sharedDebugKeystore = file("${System.getProperty("user.home")}/SK/CortexAndroid/debug-shared.keystore")
            if (sharedDebugKeystore.exists()) {
                storeFile = sharedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
}

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
    implementation(libs.compose.google.fonts)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Security — DataStore + Tink ist der aktive Schluessel-Speicher; security-crypto
    // (deprecated) bleibt nur fuer die einmalige Migration der Alt-Secrets.
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.biometric)
    implementation(libs.fragment)

    // WireGuard
    implementation(libs.wireguard.tunnel)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
