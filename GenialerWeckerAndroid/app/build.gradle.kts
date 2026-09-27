import java.net.URI
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

// Eigener Signierschlüssel dieser Verkaufs-App (Ausnahme von der gemeinsamen Debug-Signierung, siehe
// best-practices/android/debug-signing.md). Er liegt nur unter ~/SK/GenialerWeckerAndroid, nie im Repo.
// Fehlt er, bricht der Build ab — ein stiller Rückfall auf den gemeinsamen Debug-Key würde die
// Signatur wechseln und Updates auf dem Handy unmöglich machen.
val skOrdner: File = File(System.getProperty("user.home")).resolve("SK").resolve("GenialerWeckerAndroid")
val signierDaten = Properties().apply {
    val datei = skOrdner.resolve("keystore.properties")
    if (!datei.isFile) throw GradleException("Signierschlüssel fehlt: ${datei.absolutePath}. Siehe ~/SK/README.md, Abschnitt GenialerWeckerAndroid.")
    datei.inputStream().use { load(it) }
}

android {
    namespace = "de.frank.genialerwecker.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "de.frank.genialerwecker.app"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
        versionName = versionslogAktuell["versionName"] as String
        buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Whisper (sherpa-onnx) liegt nur für 64-Bit-ARM bei; das deckt alle aktuellen Play-Geräte ab.
        ndk { abiFilters += "arm64-v8a" }
    }
    signingConfigs {
        create("eigen") {
            storeFile = skOrdner.resolve(signierDaten.getProperty("RELEASE_STORE_FILE"))
            storePassword = signierDaten.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = signierDaten.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = signierDaten.getProperty("RELEASE_KEY_PASSWORD")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        // Als „debuggable“ läuft Compose auf dem Gerät deutlich langsamer; darum auch der Debug-Build ohne.
        getByName("debug") { isDebuggable = false; signingConfig = signingConfigs.getByName("eigen") }
        getByName("release") { signingConfig = signingConfigs.getByName("eigen") }
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    // Das Whisper-Modell liegt unkomprimiert in der APK, damit es direkt aus der APK gelesen werden kann.
    androidResources { noCompress += listOf("onnx") }
    sourceSets["main"].assets.srcDir("whisper-modell")
}
// Whisper small (int8, sherpa-onnx-Export, Modell-Lizenz MIT/Apache-2.0) fest in der App: Der Build lädt die Dateien
// einmal von Hugging Face nach app/whisper-modell/whisper/ (nicht im Git) und prüft die Länge gegen Content-Length.
val ladeWhisperModell by tasks.registering {
    val ziel = file("whisper-modell/whisper")
    outputs.dir(ziel)
    doLast {
        ziel.mkdirs()
        listOf("small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt").forEach { name ->
            val datei = ziel.resolve(name)
            if (datei.isFile && datei.length() > 0) return@forEach
            val verbindung = URI("https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main/$name").toURL().openConnection()
            val erwartet = verbindung.contentLengthLong
            val teil = ziel.resolve("$name.part")
            verbindung.getInputStream().use { ein -> teil.outputStream().use { ein.copyTo(it, 1 shl 20) } }
            if (erwartet > 0 && teil.length() != erwartet) throw GradleException("Whisper-Modell $name unvollständig (${teil.length()} von $erwartet Bytes).")
            if (!teil.renameTo(datei)) throw GradleException("Whisper-Modell $name ließ sich nicht speichern.")
        }
    }
}
tasks.named("preBuild") { dependsOn(ladeWhisperModell) }
kotlin { compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.icons)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.coroutines.android)
    implementation(libs.graphics.shapes)
    implementation(libs.play.billing)
    implementation(libs.okhttp)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
