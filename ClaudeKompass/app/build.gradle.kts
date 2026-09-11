plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Der Debug-Schluessel liegt in der Schluesselzentrale ~/SK/Android/ und ist fuer alle Apps und
// Rechner derselbe. Ohne ihn signiert jeder Rechner mit seinem eigenen Debug-Schluessel — dann
// verweigert das Geraet die Aktualisierung („signatures do not match") und die App muesste vor
// jeder Installation vom anderen Rechner geloescht werden, samt aller eigenen Fragen.
val skOrdner: File = File(System.getProperty("user.home")).resolve("SK").resolve("Android")
val debugSchluessel: File = rootProject.file("debug-shared.keystore")

val holeSchluessel = tasks.register("holeSchluesselAusSk") {
    val quelle = skOrdner.resolve("debug-shared.keystore")
    val ziel = debugSchluessel
    doLast {
        if (!quelle.exists()) {
            throw GradleException(
                "Debug-Schluessel fehlt: ${quelle.absolutePath}. " +
                    "Er wird nicht mitversioniert. Von Y:\\Keystores\\Android oder einem anderen " +
                    "Rechner kopieren — es ist ueberall derselbe.",
            )
        }
        quelle.copyTo(ziel, overwrite = true)
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(holeSchluessel) }

android {
    namespace = "de.frank.claudekompass"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.frank.claudekompass"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.4.12"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"11.09.2026, 14:46 Uhr\"")
        // Stand der mitgelieferten Wissensbasis. Der Aktualisieren-Knopf hebt den in der
        // Datenbank gespeicherten Stand an; dieser Wert bleibt der Auslieferungsstand.
        buildConfigField("String", "SEEDED_CLI_VERSION", "\"2.1.261\"")
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = debugSchluessel
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

    // Gemeinsamer Code beider Kompass-Apps. Nur update/ und AppProfil liegen pro App.
    sourceSets {
        getByName("main").java.srcDir(rootProject.file("../KompassKern/src/main/java"))
        getByName("test").java.srcDir(rootProject.file("../KompassKern/src/test/java"))
        getByName("androidTest").java.srcDir(rootProject.file("../KompassKern/src/androidTest/java"))
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

    testOptions {
        unitTests {
            // Die Logik-Klassen protokollieren ueber android.util.Log. Im reinen JVM-Test gibt
            // es die Klasse nur als Huelle; ohne diesen Schalter wirft jeder Aufruf. Getestet
            // wird die Logik, nicht das Protokollieren.
            isReturnDefaultValues = true
        }
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
    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.compose.material3.window.size)
    implementation(libs.lifecycle.process)

    implementation(libs.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.biometric)
    implementation(libs.fragment)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20240303")
}
