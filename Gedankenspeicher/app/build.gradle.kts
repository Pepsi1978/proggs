plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.frank.gedankenspeicher"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.gedankenspeicher"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"

        // Sichtbar in den Einstellungen (B-04, Abschnitt "Über"). Zeit aus der echten Systemuhr.
        buildConfigField("String", "VERSION_BUMPED_AT", "\"18.08.2026, 20:31 Uhr\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.core.ktx)
    implementation(libs.documentfile)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
