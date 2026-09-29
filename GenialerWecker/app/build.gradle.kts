import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Version kommt aus dem Versionslog (app/src/main/assets/versionslog.json, neuester Eintrag unten).
// Die Datei liegt als Asset in der APK, damit UpdateStation Verlauf und Neuerungen anzeigen kann.
@Suppress("UNCHECKED_CAST")
val versionslogAktuell = ((groovy.json.JsonSlurper().parse(file("src/main/assets/versionslog.json"), "UTF-8") as Map<String, Any>)["eintraege"] as List<Map<String, Any>>).last()

android {
    // Die übernommenen Design- und Sprachdateien behalten ihre Original-Namensräume.
    namespace = "de.frank.genialeideen"
    compileSdk = 36
    defaultConfig {
        applicationId = "de.frank.genialerwecker"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
        versionName = versionslogAktuell["versionName"] as String
        buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // sherpa-onnx (Whisper-Diktat und Supertonic-Stimmen) liegt nur für 64-Bit-ARM bei, wie in GenialerWeckerAndroid.
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    // Die installierte Fassung ist der Debug-Build. Als „debuggable“ läuft Compose auf dem Gerät
    // deutlich langsamer (ohne vorkompilierte Profile) — spürbar als Ruckeln beim Tippen und als
    // träge öffnende Fenster. Signiert wird weiterhin mit dem gemeinsamen Debug-Schlüssel.
    buildTypes { getByName("debug") { isDebuggable = false } }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    // Die Stimm-Modelle liegen unkomprimiert in der APK, damit sherpa-onnx sie direkt aus der APK lesen kann.
    androidResources { noCompress += listOf("onnx") }
    sourceSets["main"].assets.srcDir("tts-modelle")
}

// Supertonic 3 (31 Sprachen, OpenRAIL-M) als sherpa-onnx-Export, in höchster Qualität mit den fp32-Originalen von
// Supertone – dieselben Stimmen wie in GenialerWeckerAndroid. Der Build lädt sie einmal nach app/tts-modelle/ (nicht im Git).
// Whisper (Small und Large V3 Turbo) ist nicht in der APK: beide lädt die App bei Bedarf herunter (TurboModell.kt).
val ladeTtsModelle by tasks.registering {
    val ziel = file("tts-modelle/tts")
    outputs.dir(ziel)
    doLast {
        val name = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
        val ordner = ziel.resolve("supertonic")
        val kennung = "$name+fp32"
        if (ordner.resolve("fertig").let { it.isFile && it.readText() == kennung }) return@doLast
        val archiv = file("tts-modelle/$name.tar.bz2")
        if (!archiv.isFile) {
            val verbindung = URI("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$name.tar.bz2").toURL().openConnection()
            val erwartet = verbindung.contentLengthLong
            val teil = file("tts-modelle/$name.part")
            teil.parentFile.mkdirs()
            verbindung.getInputStream().use { ein -> teil.outputStream().use { ein.copyTo(it, 1 shl 20) } }
            if (erwartet > 0 && teil.length() != erwartet) throw GradleException("TTS-Modell $name unvollständig (${teil.length()} von $erwartet Bytes).")
            if (!teil.renameTo(archiv)) throw GradleException("TTS-Modell $name ließ sich nicht speichern.")
        }
        ordner.deleteRecursively()
        copy {
            from(tarTree(resources.bzip2(archiv)))
            into(ordner)
            eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
            includeEmptyDirs = false
        }
        // fp32-Originale statt der int8-Varianten (tts.json, unicode_indexer.bin und voice.bin bleiben aus dem sherpa-Export).
        listOf("duration_predictor", "text_encoder", "vector_estimator", "vocoder").forEach { teil ->
            val datei = ordner.resolve("$teil.onnx")
            val verbindung = URI("https://huggingface.co/Supertone/supertonic-3/resolve/main/onnx/$teil.onnx").toURL().openConnection()
            val erwartet = verbindung.contentLengthLong
            val tmp = ordner.resolve("$teil.part")
            verbindung.getInputStream().use { ein -> tmp.outputStream().use { ein.copyTo(it, 1 shl 20) } }
            if (erwartet > 0 && tmp.length() != erwartet) throw GradleException("Supertonic $teil unvollständig (${tmp.length()} von $erwartet Bytes).")
            if (!tmp.renameTo(datei)) throw GradleException("Supertonic $teil ließ sich nicht speichern.")
            ordner.resolve("$teil.int8.onnx").delete()
        }
        ordner.resolve("fertig").writeText(kennung)
        archiv.delete()
    }
}
tasks.named("preBuild") { dependsOn(ladeTtsModelle) }
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
    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.coroutines.android)
    implementation(libs.graphics.shapes)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
