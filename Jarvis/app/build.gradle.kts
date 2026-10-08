import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Version kommt aus dem Versionslog (app/src/main/assets/versionslog.json, neuester Eintrag unten).
// Die Datei liegt als Asset in der APK, damit UpdateStation Verlauf und Neuerungen anzeigen kann.
@Suppress("UNCHECKED_CAST")
val versionslogAktuell = ((groovy.json.JsonSlurper().parse(file("src/main/assets/versionslog.json"), "UTF-8") as Map<String, Any>)["eintraege"] as List<Map<String, Any>>).last()

// Adresse und Schlüssel des eigenen Servers (Jarvis-Relay) liegen nur in ~/SK/Jarvis/relay.properties, nie im Git.
// Fehlt die Datei (z. B. beim Bau in der Cloud), bleibt beides leer und wird in der App unter Einstellungen eingetragen.
val relay = Properties().apply {
    File(System.getProperty("user.home"), "SK/Jarvis/relay.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// Suchschlüssel für die Internet-Recherche der Agenten (Tavily), ebenfalls nur aus ~/SK.
val tavilyKey = File(System.getProperty("user.home"), "SK/Tavily/tavily-api-key.txt").takeIf { it.exists() }?.readText()?.trim().orEmpty()

android {
    namespace = "de.frank.jarvis"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.jarvis"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
        versionName = versionslogAktuell["versionName"] as String
        buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")
        buildConfigField("String", "RELAY_HOST", "\"${relay.getProperty("host", "")}\"")
        buildConfigField("String", "RELAY_TOKEN", "\"${relay.getProperty("token", "")}\"")
        buildConfigField("String", "TAVILY_KEY", "\"$tavilyKey\"")
    }

    // Gemeinsamer Debug-Key aller Apps (liegt nicht im Git, sondern unter ~/SK/Android/, in der Cloud legt ihn der
    // Bau-Ablauf dort ab). Jarvis und Geniale Aufgaben MÜSSEN gleich signiert sein: Die Brücke zwischen beiden
    // ist durch eine Signatur-Erlaubnis geschützt. Fehlt die Datei, bleibt ~/.android/debug.keystore.
    val geteilterDebugKey = File(System.getProperty("user.home"), "SK/Android/debug-shared.keystore").takeIf { it.exists() }

    signingConfigs {
        getByName("debug") {
            geteilterDebugKey?.let { datei ->
                storeFile = datei
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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

    packaging.resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/NOTICE.md", "META-INF/LICENSE.md")
}

kotlin {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // E-Mail über Gmail (SMTP senden, IMAP lesen).
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
