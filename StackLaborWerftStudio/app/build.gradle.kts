import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

fun File.readEnv(): Map<String, String> = if (!isFile) emptyMap() else readLines()
    .map(String::trim)
    .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
    .associate { line ->
        val (name, value) = line.split('=', limit = 2)
        name.trim() to value.trim().trim('"', '\'')
    }

fun quoted(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val secretRoot = File(System.getProperty("user.home"), "SK/PerfectMoment")
val env = File(secretRoot, ".env").readEnv()
fun envSecret(vararg names: String): String = names.firstNotNullOfOrNull { env[it]?.takeIf(String::isNotBlank) }.orEmpty()
val googleTtsKey = envSecret("GOOGLE_TTS_API_KEY", "GOOGLE_CLOUD_TTS_API_KEY", "GOOGLE_API_KEY")
val qwenTtsKey = envSecret("QWEN_TTS_API_KEY", "DASHSCOPE_API_KEY").ifBlank {
    File(secretRoot, "dashscope.key").takeIf(File::isFile)?.readText()?.trim().orEmpty()
}
val qwenVoiceId = envSecret("QWEN_TTS_VOICE_ID", "QWEN_VOICE_ID").ifBlank {
    File(secretRoot, "qwen-voice-id.txt").takeIf(File::isFile)?.readText()?.trim().orEmpty()
}

android {
    namespace = "de.frank.stacklabor.werftstudio"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.stacklabor.werftstudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
        buildConfigField("String", "VERSION_BUMPED_AT", quoted("14.08.2026, 18:07 Uhr"))
        buildConfigField("String", "GOOGLE_TTS_API_KEY", quoted(googleTtsKey))
        buildConfigField("String", "QWEN_TTS_API_KEY", quoted(qwenTtsKey))
        buildConfigField("String", "QWEN_TTS_VOICE_ID", quoted(qwenVoiceId))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    testOptions.unitTests.isIncludeAndroidResources = true
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.okhttp)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
