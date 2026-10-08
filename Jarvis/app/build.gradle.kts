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
    namespace = "de.frank.jarvis"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.jarvis"
        // 31: Das ngrok-SDK braucht Java-APIs, die es erst ab Android 11 gibt.
        minSdk = 31
        targetSdk = 36
        versionCode = (versionslogAktuell["versionCode"] as Number).toInt()
        versionName = versionslogAktuell["versionName"] as String
        buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")
        ndk { abiFilters += "arm64-v8a" }
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

    packaging.resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/nanohttpd/**")

    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("ngrok/jniLibs"))
}

// Das ngrok-Paket legt seine native Bibliothek lose in die JAR; so landet sie nicht in der APK.
// Deshalb holt dieser Schritt sie heraus und legt sie als normale Android-Bibliothek (arm64) ab.
val ngrokNative by configurations.creating
//
// Zusätzlich meldet die Bibliothek beim Laden die JNI-Version 1.8 (0x10008). Android kennt höchstens 1.6 und
// bricht das Laden dann ab ("Bad JNI version returned from JNI_OnLoad"). Die eine Anweisung, die diese Zahl
// zurückgibt, wird deshalb hier auf 1.6 (0x10006) umgeschrieben. Greift das Muster nicht genau einmal
// (etwa nach einem ngrok-Update), bricht der Bau ab, statt eine nicht ladbare App auszuliefern.
val ngrokBibliothek = tasks.register<Copy>("ngrokBibliothek") {
    from({ zipTree(ngrokNative.singleFile) }) { include("libngrok_java.so") }
    into(layout.buildDirectory.dir("ngrok/jniLibs/arm64-v8a"))
    doLast {
        val datei = destinationDir.resolve("libngrok_java.so")
        val bytes = datei.readBytes()
        // mov w0,#8 ; movk w0,#1,lsl#16 ; ldr x30,[sp],#0x10 ; ret   (Ende von JNI_OnLoad)
        val muster = byteArrayOf(0x00, 0x01, 0x80.toByte(), 0x52, 0x20, 0x00, 0xa0.toByte(), 0x72, 0xfe.toByte(), 0x07, 0x41, 0xf8.toByte(), 0xc0.toByte(), 0x03, 0x5f, 0xd6.toByte())
        val treffer = (0..bytes.size - muster.size step 4).filter { i -> muster.indices.all { bytes[i + it] == muster[it] } }
        check(treffer.size == 1) { "JNI_OnLoad in libngrok_java.so nicht eindeutig gefunden (${treffer.size} Treffer). ngrok-Version geändert? Muster in app/build.gradle.kts prüfen." }
        bytes[treffer.single()] = 0xc0.toByte()   // mov w0,#6
        bytes[treffer.single() + 1] = 0x00
        datei.writeBytes(bytes)
    }
}
tasks.named("preBuild") { dependsOn(ngrokBibliothek) }

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

    // Kleiner HTTP-Server für den MCP-Endpunkt auf dem Handy.
    implementation(libs.nanohttpd)
    // Tunnel direkt aus der App: Das Android-Paket bringt die Klassen und die native Bibliothek (arm64) mit.
    implementation(libs.ngrok)
    implementation("com.ngrok:ngrok-java-native:${libs.versions.ngrok.get()}:linux-android-aarch_64") { isTransitive = false }
    ngrokNative("com.ngrok:ngrok-java-native:${libs.versions.ngrok.get()}:linux-android-aarch_64") { isTransitive = false }
    implementation(libs.slf4j.api)
}
