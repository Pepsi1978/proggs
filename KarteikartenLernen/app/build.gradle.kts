plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun localSecret(name: String): String {
    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val secretFile = file("${System.getProperty("user.home")}/SK/VoiceOverlays/.env")
    if (!secretFile.isFile) return ""
    return secretFile.useLines { lines ->
        lines.firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("$name=")) trimmed.substringAfter('=').trim().takeIf { it.isNotEmpty() } else null
        }.orEmpty()
    }
}

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val groqApiKey = localSecret("GROQ_API_KEY")
val geminiApiKey = localSecret("GEMINI_API_KEY")
val geminiModel = localSecret("GEMINI_MODEL").ifBlank { "gemini-3.1-flash-lite" }

android {
    namespace = "de.frank.karteikartenlernen"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.frank.karteikartenlernen"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "0.1.15"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"17.07.2026, 23:39 Uhr\"")
        buildConfigField("String", "GROQ_API_KEY", groqApiKey.asBuildConfigString())
        buildConfigField("String", "GROQ_TRANSCRIPTION_MODEL", "\"whisper-large-v3-turbo\"")
        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.asBuildConfigString())
        buildConfigField("String", "GEMINI_MODEL", geminiModel.asBuildConfigString())
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
