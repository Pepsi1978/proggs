# M1.4 Transkriptionsmodul — so wird es eingebaut

Aus Jarvis, dem ersten Konsumenten. Vorlage der Anbindung:
`Jarvis/app/src/main/java/de/frank/module/transkriptionsmodul/Anbindung.kt`.

## 1. Anbindung der App

```kotlin
package de.frank.module.transkriptionsmodul

object TranskriptionAnbindung {
    init {
        // Optional: Protokollzeilen des Moduls ins eigene Protokoll.
        Protokoll.aktiv = object : Protokoll {
            override fun sprache(ereignis: String, details: String, ms: Long?) = MeinLog.sprache(ereignis, details, ms)
            override fun netz(ereignis: String, details: String) = MeinLog.netz(ereignis, details)
        }
    }

    fun mikrofon(context: Context) = MicRecorder(context, KLEIDIAI_ABSCHALTEN)

    /** Ohne eigene Korrektur einfach `Diktat(transcriber)`. */
    fun diktat(transcriber: GroqTranscriber) = Diktat(transcriber) { text -> MeineUmlautKorrektur.korrigiere(text) }

    /**
     * ONNX Runtime ab 1.22 rechnet auf ARM mit KleidiAI-Kernen, die SME2 nutzen. Prozessoren mit SME, aber ohne
     * SME2 (Galaxy Fold 8) stürzen daran mit SIGILL ab. Gibt es in der App schon eine solche Prüfung (Jarvis:
     * `WeckwortErkenner.KLEIDIAI_ABSCHALTEN`), dieselbe nehmen.
     */
    val KLEIDIAI_ABSCHALTEN: Boolean by lazy {
        val merkmale = runCatching {
            java.io.File("/proc/cpuinfo").useLines { z -> z.firstOrNull { it.startsWith("Features") }.orEmpty() }
        }.getOrDefault("").substringAfter(':').trim().split(' ').toSet()
        "sme" in merkmale && "sme2" !in merkmale
    }
}
```

## 2. Aufnehmen, Ende erkennen, transkribieren (aus `AppViewModel` von Jarvis)

```kotlin
private val mikro = TranskriptionAnbindung.mikrofon(app)

// Freihändig: piepen, aufnehmen, bis 4,5 s keine Stimme mehr kam.
suspend fun runde(erste: Boolean): SprechEnde.Lage {
    if (!mikro.start(viewModelScope)) return SprechEnde.Lage.NICHTS_GEHOERT
    // Nach einer Antwort darf man etwas länger überlegen, bevor man anfängt.
    val ende = SprechEnde(wartenMs = if (erste) 7_000 else 9_000)
    val start = System.currentTimeMillis()   // Uhr ab Beginn des Pieptons; SprechEnde überspringt 250 ms
    piep()
    var lage = SprechEnde.Lage.WEITER
    while (lage == SprechEnde.Lage.WEITER) {
        delay(50)
        lage = ende.neu(mikro.pegel.value, System.currentTimeMillis() - start, mikro.sprachRahmen)
    }
    if (lage == SprechEnde.Lage.NICHTS_GEHOERT) { mikro.stop(); return lage }
    abschicken()
    return lage
}

suspend fun abschicken() {
    val wav = mikro.stop() ?: return
    val stimmhaft = mikro.letzteStimmhaft          // direkt nach stop() lesen, gehört genau zu dieser Aufnahme
    val groq = GroqTranscriber(apiKey = schluessel) // einmal bauen und behalten, solange sich Schlüssel/Filter nicht ändern
    val text = withContext(Dispatchers.IO) { TranskriptionAnbindung.diktat(groq).transkribiere(wav, stimmhaft).text.trim() }
    if (text.isBlank()) meldung = "Ich habe nichts verstanden." else sende(text)
}
```

Mit Knopf statt freihändig: `mikro.start(...)` beim Drücken, `abschicken()` beim zweiten Druck — `SprechEnde`
entfällt, die Filter greifen trotzdem.

## 3. Gradle und Assets

```kotlin
dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
android { defaultConfig { ndk { abiFilters += listOf("arm64-v8a") } } }
```

`Module/Android/M1.4-Transkriptionsmodul/assets/vad/` → `app/src/main/assets/vad/`,
Tests nach `app/src/test/java/de/frank/module/transkriptionsmodul/`, Testdaten nach `app/src/test/resources/vad/`.
