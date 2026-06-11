plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.frank.voicekey"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.voicekey"
        minSdk = 34
        targetSdk = 36
        versionCode = 6
        versionName = "0.5.0"

        // Vosk liefert native .so — auf die real genutzten ABIs beschraenken (Fold 6 = arm64).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        // Sichtbare Version in der UI kommt aus BuildConfig (version-bump-visible-Regel).
        buildConfig = true
    }

    packaging {
        resources {
            // JNA/Vosk bringen doppelte Meta-Dateien mit.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)

    // Wake-Word-Engine: Vosk (offline, frei definierbare Keywords, EN + DE Modelle).
    implementation(libs.vosk.android)
    implementation(libs.jna) { artifact { type = "aar" } }
}
