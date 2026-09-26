plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Version kommt aus dem Versionslog (app/src/main/assets/versionslog.json, neuester Eintrag unten).
// Die Datei liegt als Asset in der APK, damit UpdateStation Verlauf und Neuerungen anzeigen kann.
@Suppress("UNCHECKED_CAST")
val versionslogAktuell = ((groovy.json.JsonSlurper().parse(file("src/main/assets/versionslog.json"), "UTF-8") as Map<String, Any>)["eintraege"] as List<Map<String, Any>>).last()

android {
    namespace = "de.frank.karteikartenlernen"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.frank.karteikartenlernen"
        minSdk = 26
        targetSdk = 35
        versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
        versionName = versionslogAktuell["versionName"] as String
        buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")
        buildConfigField("String", "GROQ_TRANSCRIPTION_MODEL", "\"whisper-large-v3-turbo\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.android)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
