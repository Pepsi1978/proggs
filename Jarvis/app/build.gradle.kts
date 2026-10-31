import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Version kommt aus dem Versionslog (app/src/main/assets/versionslog.json, neuester Eintrag unten).
// Die Datei liegt als Asset in der APK, damit UpdateStation Verlauf und Neuerungen anzeigen kann.
@Suppress("UNCHECKED_CAST")
val versionslogEintraege = ((groovy.json.JsonSlurper().parse(file("src/main/assets/versionslog.json"), "UTF-8") as Map<String, Any>)["eintraege"] as List<Map<String, Any>>)
val versionslogAktuell = versionslogEintraege.last()

// Ohne höheren versionCode erkennt UpdateStation das Update nicht. Geprüft werden nur die letzten zwei Einträge:
// Alte Einträge bleiben unverändert, und in der Historie steht versionCode 60 doppelt.
if (versionslogEintraege.size >= 2) {
    val vorher = (versionslogEintraege[versionslogEintraege.size - 2]["versionCode"] as Number).toInt()
    val jetzt = (versionslogAktuell["versionCode"] as Number).toInt()
    check(jetzt > vorher) {
        "versionslog.json: Der letzte versionCode ($jetzt) muss größer sein als der vorletzte ($vorher), sonst erkennt UpdateStation das Update nicht."
    }
}

// Nur Adressen und öffentliche Kennungen kommen in die APK; Schlüssel und Tokens nie (wer die APK hat, könnte sie
// auslesen). Die bringt werkzeuge/zugang-aufs-handy.ps1 aus ~/SK direkt in die App, oder man trägt sie unter
// Einstellungen ein.

// Adresse des eigenen Servers (Jarvis-Relay) aus ~/SK/Jarvis/relay.properties, nie im Git. Fehlt die Datei (z. B. beim
// Bau in der Cloud), bleibt sie leer und wird in der App unter Einstellungen eingetragen.
val relay = Properties().apply {
    File(System.getProperty("user.home"), "SK/Jarvis/relay.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// Google-Client für „Berechtigung erneuern“ (Nur-Lese-Zugang zum Tagebuch-Ordner): nur die Client-ID, aus ~/SK.
val driveClient = Properties().apply {
    File(System.getProperty("user.home"), "SK/rclone/google-oauth-client.txt").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// Adresse des Auslösers der Claude-Code-Routine für das Werkzeug „programmieren“ (aus dem API-Trigger der Routine auf
// claude.ai/code/routines), aus ~/SK. Fehlt die Datei, trägt man sie in der App ein.
val routine = Properties().apply {
    File(System.getProperty("user.home"), "SK/Jarvis/routine.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

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
        buildConfigField("String", "DRIVE_CLIENT_ID", "\"${driveClient.getProperty("client_id", "")}\"")
        buildConfigField("String", "ROUTINE_URL", "\"${routine.getProperty("url", "")}\"")

        // „Hey Jarvis“ rechnet mit ONNX Runtime (native Bibliotheken); nur die Handy-Architekturen mitnehmen.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
    // sherpa-onnx (eigene Weckwort-Phrase) und „Hey Jarvis“ teilen EINE ONNX Runtime: die neueste von Microsoft
    // (libs.versions.toml). Aus der sherpa-AAR ist ihre eigene libonnxruntime.so entfernt (libs/LIESMICH.md), damit
    // nur diese eine geladen wird und der KleidiAI-Schalter in WeckwortErkenner für beide gilt.
}

// Pflicht-Teil „freigabe“ (siehe AndroidManifest): liegt signiert als Asset in der APK. So bleibt es für apk-update und
// UpdateStation bei einer Datei; nur Werkzeuge/jarvis-install und UpdateStation holen ihn heraus und installieren ihn mit.
abstract class FreigabeTeilBauen @Inject constructor(private val ausfuehren: ExecOperations) : DefaultTask() {
    @get:Input abstract val paket: Property<String>
    @get:Input abstract val versionCode: Property<Int>
    @get:Input abstract val versionName: Property<String>
    @get:Input abstract val schluessel: Property<String>
    @get:Input abstract val sdk: Property<String>
    @get:Input abstract val plattform: Property<String>
    @get:OutputDirectory abstract val ausgabe: DirectoryProperty

    @TaskAction
    fun baue() {
        val windows = System.getProperty("os.name").startsWith("Windows")
        val aapt2Name = if (windows) "aapt2.exe" else "aapt2"
        val apksignerName = if (windows) "apksigner.bat" else "apksigner"
        val werkzeuge = File(sdk.get(), "build-tools").listFiles().orEmpty()
            .filter { File(it, aapt2Name).exists() && File(it, apksignerName).exists() }
            .maxByOrNull { it.name } ?: error("Keine build-tools mit aapt2 und apksigner gefunden")
        val arbeit = temporaryDir
        val manifest = File(arbeit, "AndroidManifest.xml")
        manifest.writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="${paket.get()}" split="freigabe"
                android:versionCode="${versionCode.get()}" android:versionName="${versionName.get()}" android:splitTypes="freigabe">
                <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36" />
                <application android:hasCode="false" />
            </manifest>
            """.trimIndent(),
        )
        val roh = File(arbeit, "freigabe-roh.apk")
        val ziel = ausgabe.get().asFile.apply { mkdirs() }.resolve("freigabe-teil.apk")
        ausfuehren.exec {
            commandLine(File(werkzeuge, aapt2Name).path, "link", "-o", roh.path, "-I", "${sdk.get()}/platforms/${plattform.get()}/android.jar", "--manifest", manifest.path)
        }
        ausfuehren.exec {
            commandLine(
                File(werkzeuge, apksignerName).path, "sign", "--ks", schluessel.get(), "--ks-key-alias", "androiddebugkey",
                "--ks-pass", "pass:android", "--key-pass", "pass:android", "--min-sdk-version", "26", "--v4-signing-enabled", "false", "--out", ziel.path, roh.path,
            )
        }
    }
}

val freigabeTeil = tasks.register<FreigabeTeilBauen>("baueFreigabeTeil") {
    paket = android.defaultConfig.applicationId
    versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
    versionName = versionslogAktuell["versionName"] as String
    schluessel = android.signingConfigs.getByName("debug").storeFile?.path ?: "${System.getProperty("user.home")}/.android/debug.keystore"
    sdk = androidComponents.sdkComponents.sdkDirectory.map { it.asFile.path }
    plattform = "android-${android.compileSdk}"
    ausgabe = layout.buildDirectory.dir("generated/freigabeTeil")
}

androidComponents {
    onVariants { variante ->
        variante.sources.assets?.addGeneratedSourceDirectory(freigabeTeil, FreigabeTeilBauen::ausgabe)
    }
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
    implementation(libs.biometric)
    implementation(libs.fragment)
    // Bedienknöpfe der Vorlese-Benachrichtigung.
    implementation(libs.media)

    // Ablage: Audio und Video abspielen (Media3), große Bilder und PDF-Seiten gekachelt zoomen.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.ssiv)

    // Weckwort „Hey Jarvis“: Modelle von openWakeWord (assets/weckwort/), gerechnet mit ONNX Runtime.
    implementation(libs.onnxruntime.android)
    // Eigene Weckwort-Phrase: sherpa-onnx KeywordSpotter (Apache-2.0), Modell in assets/kws/ (Gigaspeech, englisch).
    implementation(files("libs/sherpa-onnx-1.13.8-ohne-onnxruntime.aar"))

    // Unit-Tests der Ablage ohne Gerät (org.json ersetzt die Android-Attrappe, MockWebServer für Downloads).
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.okhttp.mockwebserver)

    // Abgeschottete Laufzeit der eigenen Helfer (nur Interpreter, siehe helfer/HelferLauf.kt).
    implementation("org.mozilla:rhino:1.7.15")
    // E-Mail über Gmail (SMTP senden, IMAP lesen).
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
