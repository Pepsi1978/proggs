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
        versionCode = 41
        versionName = "0.5.28"

        // Sichtbar in den Einstellungen (B-04, Abschnitt "Über"). Zeit aus der echten Systemuhr.
        buildConfigField("String", "VERSION_BUMPED_AT", "\"27.08.2026, 12:17 Uhr\"")
    }

    // Die App auf dem Handy stammt vom Windows-Rechner und trägt dessen Debug-Signatur
    // (Zertifikat-SHA-256 `171034c5…`). Der Mac signiert von Haus aus mit einer anderen —
    // eine Installation darüber lehnt Android mit INSTALL_FAILED_UPDATE_INCOMPATIBLE ab
    // und alle Notizen wären nur über Deinstallation zu retten.
    //
    // Liegt der Windows-Debug-Keystore unter `~/SK/Gedankenspeicher/` (Dateiname
    // `debug-shared.keystore` oder `debug.keystore`), signiert Gradle damit und die
    // Installation geht über die bestehende drüber. Fehlt er, bleibt alles beim Standard —
    // dann baut der Mac wie bisher, nur eben nicht auf dieses Handy installierbar.
    val eigenerDebugKeystore = listOf("debug-shared.keystore", "debug.keystore")
        .map { File(System.getProperty("user.home"), "SK/Gedankenspeicher/$it") }
        .firstOrNull { it.exists() }

    signingConfigs {
        getByName("debug") {
            eigenerDebugKeystore?.let { datei ->
                storeFile = datei
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
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
    implementation(libs.dokumentenscanner)
    implementation(libs.texterkennung)
    implementation(libs.exifinterface)
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
    implementation(libs.biometric)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
