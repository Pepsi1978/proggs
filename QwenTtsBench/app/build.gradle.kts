plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.frank.qwenttsbench"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.qwenttsbench"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.00.0"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"27.07.2026, 13:12 Uhr\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.27.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
