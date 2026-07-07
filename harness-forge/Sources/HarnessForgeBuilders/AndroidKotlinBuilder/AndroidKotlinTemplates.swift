// AndroidKotlinTemplates.swift
// Templates fuer ein minimales, aber lauffaehiges Android-Projekt:
// Kotlin + Jetpack Compose + Material 3 + OkHttp fuer den LLM-Call.

import Foundation

public enum AndroidKotlinTemplates {

    /// Android-Package-Name aus Slug: "meine-app" → "com.harnessforge.meine_app"
    public static func androidPackage(from slug: String) -> String {
        let base = slug.replacingOccurrences(of: "-", with: "_")
        return "com.harnessforge.\(base)"
    }

    /// Package-Name in Pfad-Form: "com.harnessforge.meine_app" → "com/harnessforge/meine_app"
    public static func packagePath(from androidPackage: String) -> String {
        androidPackage.replacingOccurrences(of: ".", with: "/")
    }

    // MARK: - Root-Level

    public static func rootBuildGradle() -> String {
        #"""
        // Root-Level build.gradle.kts — keine Plugins hier, alles pro Modul.
        plugins {
            id("com.android.application") version "8.7.0" apply false
            id("org.jetbrains.kotlin.android") version "2.0.21" apply false
            id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
        }
        """#
    }

    public static func settingsGradle(slug: String) -> String {
        #"""
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "\#(slug)"
        include(":app")
        """#
    }

    public static func gradleProperties() -> String {
        #"""
        org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
        android.useAndroidX=true
        kotlin.code.style=official
        android.nonTransitiveRClass=true
        android.nonFinalResIds=false
        """#
    }

    // MARK: - app/

    public static func appBuildGradle(pkg: String) -> String {
        #"""
        plugins {
            id("com.android.application")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose")
        }

        android {
            namespace = "\#(pkg)"
            compileSdk = 36

            defaultConfig {
                applicationId = "\#(pkg)"
                minSdk = 26
                targetSdk = 36
                versionCode = 1
                versionName = "0.1.0"
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            kotlinOptions {
                jvmTarget = "17"
            }

            buildFeatures {
                compose = true
            }
        }

        dependencies {
            implementation("androidx.core:core-ktx:1.15.0")
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation(platform("androidx.compose:compose-bom:2024.12.01"))
            implementation("androidx.compose.ui:ui")
            implementation("androidx.compose.material3:material3")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

            // HTTP-Client
            implementation("com.squareup.okhttp3:okhttp:4.12.0")

            // JSON
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        """#
    }

    public static func androidManifest(slug: String) -> String {
        #"""
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">

            <uses-permission android:name="android.permission.INTERNET" />

            <application
                android:label="\#(slug)"
                android:theme="@style/Theme.HarnessApp"
                android:supportsRtl="true">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>

        </manifest>
        """#
    }

    public static func stringsXml(slug: String) -> String {
        #"""
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <string name="app_name">\#(slug)</string>
        </resources>
        """#
    }

    public static func themesXml() -> String {
        #"""
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <style name="Theme.HarnessApp" parent="android:Theme.Material.Light.NoActionBar" />
        </resources>
        """#
    }

    // MARK: - Kotlin-Quelldateien

    public static func mainActivityKt(pkg: String) -> String {
        #"""
        package \#(pkg)

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.compose.foundation.layout.fillMaxSize
        import androidx.compose.material3.MaterialTheme
        import androidx.compose.material3.Surface
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    HarnessApp()
                }
            }
        }

        @Composable
        fun HarnessApp() {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
        """#
    }

    public static func mainScreenKt(pkg: String, taskDescription: String) -> String {
        #"""
        package \#(pkg)

        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.rememberScrollState
        import androidx.compose.foundation.verticalScroll
        import androidx.compose.material3.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.unit.dp
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.withContext

        @Composable
        fun MainScreen() {
            var input by remember { mutableStateOf("") }
            var output by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "\#(escapeForKotlin(taskDescription))",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Deine Frage") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        loading = true
                        errorMsg = ""
                        output = ""
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    LLMClient.chat(input)
                                }
                                output = result
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Unbekannter Fehler"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading && input.isNotBlank(),
                ) {
                    Text(if (loading) "Lade ..." else "Fragen")
                }

                Spacer(Modifier.height(16.dp))

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = "Fehler: $errorMsg",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (output.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(output, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
        """#
    }

    public static func llmClientKt(pkg: String) -> String {
        #"""
        package \#(pkg)

        import kotlinx.serialization.json.JsonObject
        import kotlinx.serialization.json.JsonPrimitive
        import kotlinx.serialization.json.buildJsonObject
        import kotlinx.serialization.json.Json
        import kotlinx.serialization.json.jsonArray
        import kotlinx.serialization.json.jsonObject
        import kotlinx.serialization.json.jsonPrimitive
        import kotlinx.serialization.json.put
        import kotlinx.serialization.json.putJsonArray
        import kotlinx.serialization.json.putJsonObject
        import okhttp3.MediaType.Companion.toMediaType
        import okhttp3.OkHttpClient
        import okhttp3.Request
        import okhttp3.RequestBody.Companion.toRequestBody
        import java.util.concurrent.TimeUnit

        /**
         * Minimaler Anthropic-Client ueber OkHttp. API-Key wird aus BuildConfig
         * oder einer Umgebungs-Property erwartet. Fuer Release: Secure-Storage
         * oder User-Eingabe bevorzugen.
         */
        object LLMClient {
            private val http = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            private const val SYSTEM_PROMPT = "Du bist ein hilfreicher Assistent."

            fun chat(userMessage: String): String {
                val apiKey = System.getenv("ANTHROPIC_API_KEY")
                    ?: throw IllegalStateException("ANTHROPIC_API_KEY nicht gesetzt")

                val body = buildJsonObject {
                    put("model", "claude-opus-4-7")
                    put("max_tokens", 2048)
                    put("system", SYSTEM_PROMPT)
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    }
                }

                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(request).execute().use { response ->
                    val raw = response.body?.string() ?: throw RuntimeException("Leere Antwort")
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP ${'$'}{response.code}: ${'$'}raw")
                    }
                    val json = Json.parseToJsonElement(raw).jsonObject
                    val content = json["content"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")?.jsonPrimitive?.content
                    return content ?: "<keine Antwort>"
                }
            }

            private inline fun kotlinx.serialization.json.JsonArrayBuilder.add(
                element: kotlinx.serialization.json.JsonElement
            ) { add(element) }
        }
        """#
    }

    // MARK: - Docs

    public static func readmeMd(slug: String, taskDescription: String, decomposition: TaskDecomposition) -> String {
        """
        # \(slug)

        > Android-App, automatisch generiert von Harness Forge.
        > Kotlin + Jetpack Compose + Material 3.

        ## Aufgabe

        \(taskDescription)

        ## Voraussetzungen

        - Android Studio Jellyfish+ (oder neuer)
        - JDK 17
        - Android SDK 36

        ## Setup

        1. Oeffne dieses Verzeichnis in Android Studio.
        2. Syncronisiere Gradle (laeuft beim ersten Oeffnen automatisch).
        3. Setze die Umgebungsvariable `ANTHROPIC_API_KEY` beim Start.

        ## Start

        Entweder via Android Studio (Run-Button) oder:

        ```bash
        ./gradlew :app:installDebug
        ```

        ## Architektur

        - `MainActivity.kt` — setzt Compose-UI auf
        - `MainScreen.kt` — Eingabe + Button + Antwort-Anzeige
        - `LLMClient.kt` — OkHttp-basierter Anthropic-Client

        ## Kontext

        - Zielnutzer: `\(decomposition.targetUsers.rawValue)`
        - Begruendung: \(decomposition.rationale)

        ---

        *Generiert von Harness Forge.*
        """
    }

    public static func systemPromptMd(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition
    ) -> String {
        PromptTemplate.render(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition
        )
    }

    public static func gitignore() -> String {
        #"""
        # Build-Outputs
        build/
        .gradle/

        # Android-Studio
        .idea/
        *.iml
        local.properties

        # OS
        .DS_Store

        # Keystores
        *.jks
        *.keystore

        # Misc
        captures/
        """#
    }

    // MARK: - Escape-Helfer

    private static func escapeForKotlin(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
            .replacingOccurrences(of: "\n", with: " ")
    }
}
