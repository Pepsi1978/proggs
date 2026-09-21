plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.frank.novadrehen"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.novadrehen"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"21.09.2026, 20:06 Uhr\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
