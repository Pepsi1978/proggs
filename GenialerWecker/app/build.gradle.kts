plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    // Die übernommenen Design- und Sprachdateien behalten ihre Original-Namensräume.
    namespace = "de.frank.genialeideen"
    compileSdk = 36
    defaultConfig {
        applicationId = "de.frank.genialerwecker"
        minSdk = 26
        targetSdk = 36
        versionCode = 80
        versionName = "1.1.84"
        buildConfigField("String", "VERSION_BUMPED_AT", "\"21.09.2026, 13:01 Uhr\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}
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
