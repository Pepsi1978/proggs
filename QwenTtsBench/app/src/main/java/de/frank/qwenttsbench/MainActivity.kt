package de.frank.qwenttsbench

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.system.measureNanoTime

/**
 * Misst das Tempo des Qwen3-TTS-Talkers auf diesem Geraet.
 *
 * Der Talker ist der einzige autoregressive Teil der TTS-Pipeline und damit der
 * Flaschenhals. Der 12-Hz-Tokenizer bedeutet: 12 Talker-Schritte = 1 Sekunde Sprache.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            textSize = 13f
            setPadding(24, 24, 24, 24)
        }
        setContentView(ScrollView(this).apply { addView(view) })

        val log: (String) -> Unit = { line ->
            Log.i(TAG, line)
            runOnUiThread { view.append(line + "\n") }
        }

        val steps = intent.getIntExtra("steps", STEPS)
        val promptLen = intent.getIntExtra("prompt", PROMPT_LEN)
        val threads = intent.getIntExtra("threads", THREADS)
        val mode = intent.getStringExtra("mode") ?: MODE_TALKER

        log("Qwen TTS Bench ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})")
        log("Geraet: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        log("Kerne: ${Runtime.getRuntime().availableProcessors()}")
        log("Modus: $mode")
        if (mode == MODE_TALKER) log("Lauf: $steps Schritte, Prompt $promptLen, Threads $threads")
        log("")

        Thread {
            try {
                when (mode) {
                    MODE_VOCODER -> runVocoderProbe(log, steps, threads)
                    else -> runBenchmark(log, steps, promptLen, threads)
                }
            } catch (error: Throwable) {
                log("FEHLER: ${error::class.java.simpleName}: ${error.message}")
                Log.e(TAG, "Lauf fehlgeschlagen", error)
            }
        }.start()
    }

    private fun runBenchmark(
        log: (String) -> Unit,
        steps: Int,
        promptLen: Int,
        threads: Int,
    ) {
        val model = File(getExternalFilesDir("models"), MODEL_NAME)
        if (!model.exists()) {
            log("Modell fehlt. Erwartet unter:")
            log(model.absolutePath)
            log("")
            log("Per adb pushen:")
            log("adb push talker_decode_q.onnx ${model.absolutePath}")
            return
        }
        log("Modell: ${model.length() / 1024 / 1024} MB")

        val env = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(threads)
        }

        var session: OrtSession? = null
        val loadMs = measureNanoTime {
            session = env.createSession(model.absolutePath, options)
        } / 1_000_000
        val ort = session ?: return
        log("Geladen in ${loadMs} ms")
        log("")

        // Zwischenspeicher (KV-Cache) mit dem Prompt vorbelegen.
        var cache = Array(LAYERS * 2) { FloatArray(KV_HEADS * promptLen * HEAD_DIM) }
        var cacheLen = promptLen

        val outputNames = ort.outputNames.toList()
        val timings = ArrayList<Long>(steps)

        for (step in 0 until steps) {
            val inputs = HashMap<String, OnnxTensor>()
            inputs["inputs_embeds"] = OnnxTensor.createTensor(
                env,
                FloatBuffer.allocate(HIDDEN),
                longArrayOf(1, 1, HIDDEN.toLong()),
            )
            inputs["attention_mask"] = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(LongArray(cacheLen + 1) { 1L }),
                longArrayOf(1, (cacheLen + 1).toLong()),
            )
            val cacheShape = longArrayOf(1, KV_HEADS.toLong(), cacheLen.toLong(), HEAD_DIM.toLong())
            for (layer in 0 until LAYERS) {
                inputs["past_key_$layer"] =
                    OnnxTensor.createTensor(env, FloatBuffer.wrap(cache[layer * 2]), cacheShape)
                inputs["past_value_$layer"] =
                    OnnxTensor.createTensor(env, FloatBuffer.wrap(cache[layer * 2 + 1]), cacheShape)
            }

            val elapsed: Long
            val nextCache = Array(LAYERS * 2) { FloatArray(0) }
            try {
                val started = System.nanoTime()
                val result = ort.run(inputs)
                elapsed = (System.nanoTime() - started) / 1_000_000
                result.use { outputs ->
                    for (layer in 0 until LAYERS) {
                        nextCache[layer * 2] = readFloats(outputs, outputNames, "present_key_$layer")
                        nextCache[layer * 2 + 1] = readFloats(outputs, outputNames, "present_value_$layer")
                    }
                }
            } finally {
                inputs.values.forEach { it.close() }
            }

            cache = nextCache
            cacheLen += 1
            timings.add(elapsed)

            if (step == 0) log("Erster Schritt: ${elapsed} ms")
            if (step > 0 && step % 20 == 0) log("  ... Schritt $step: ${elapsed} ms")
        }

        ort.close()

        val warm = if (timings.size > 6) timings.drop(3) else timings
        val avgMs = warm.average()
        val perSecond = 1000.0 / avgMs
        val realtime = perSecond / TOKENS_PER_AUDIO_SECOND
        val totalSeconds = timings.sum() / 1000.0
        val audioSeconds = timings.size.toDouble() / TOKENS_PER_AUDIO_SECOND

        log("")
        log("======== ERGEBNIS ========")
        log("Schritte:            ${timings.size}  (= ${"%.1f".format(audioSeconds)} s Sprache)")
        log("Erster Schritt:      ${timings.first()} ms")
        log("Letzter Schritt:     ${timings.last()} ms")
        log("Mittel:              ${"%.0f".format(avgMs)} ms/Schritt")
        log("Tempo:               ${"%.1f".format(perSecond)} Schritte/s")
        log("Echtzeitfaktor:      ${"%.2f".format(realtime)} x")
        log("")
        log("Talker gesamt:       ${"%.1f".format(totalSeconds)} s fuer ${"%.1f".format(audioSeconds)} s Sprache")
        log("")
        log(if (realtime >= 1.0) "Schneller als Echtzeit." else "Langsamer als Echtzeit.")
    }

    /**
     * Prueft, ob der Vocoder (12-Hz-Tokenizer-Decoder) auf diesem Geraet ueberhaupt laeuft.
     *
     * Der Modell-Anbieter meldet, dass dieses Modell ConvInteger-Operationen benutzt, die auf
     * der CPU fehlschlagen. Ohne dieses Modell entsteht kein hoerbares Audio. Die Probe klaert
     * das, bevor die uebrige Pipeline gebaut wird.
     */
    private fun runVocoderProbe(log: (String) -> Unit, frames: Int, threads: Int) {
        val model = File(getExternalFilesDir("models"), VOCODER_NAME)
        if (!model.exists()) {
            log("Modell fehlt. Erwartet unter:")
            log(model.absolutePath)
            log("")
            log("Per adb pushen:")
            log("adb push $VOCODER_NAME ${model.absolutePath}")
            return
        }
        log("Modell: ${model.length() / 1024 / 1024} MB")

        val env = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(threads)
        }

        var session: OrtSession? = null
        val loadMs = measureNanoTime {
            session = env.createSession(model.absolutePath, options)
        } / 1_000_000
        val ort = session ?: return
        log("Geladen in $loadMs ms — ConvInteger wird also geladen.")
        log("")

        log("Eingaben:")
        ort.inputInfo.forEach { (name, info) -> log("  $name: ${describe(info)}") }
        log("Ausgaben:")
        ort.outputInfo.forEach { (name, info) -> log("  $name: ${describe(info)}") }
        log("")

        val inputName = ort.inputNames.first()
        val layouts = listOf(
            longArrayOf(1, frames.toLong(), CODEBOOKS.toLong()) to "1 x T=$frames x C=$CODEBOOKS",
            longArrayOf(1, CODEBOOKS.toLong(), frames.toLong()) to "1 x C=$CODEBOOKS x T=$frames",
        )

        for ((shape, label) in layouts) {
            log("Versuch mit $label ...")
            try {
                val codes = LongArray((shape[1] * shape[2]).toInt()) {
                    (it * 37 % CODE_RANGE).toLong()
                }
                var samples = FloatArray(0)
                val runMs = measureNanoTime {
                    OnnxTensor.createTensor(env, LongBuffer.wrap(codes), shape).use { tensor ->
                        ort.run(mapOf(inputName to tensor)).use { result ->
                            samples = flatten(result.get(0) as OnnxTensor)
                        }
                    }
                } / 1_000_000

                val seconds = samples.size.toDouble() / SAMPLE_RATE
                val expected = frames.toDouble() / TOKENS_PER_AUDIO_SECOND
                var peak = 0f
                for (value in samples) if (kotlin.math.abs(value) > peak) peak = kotlin.math.abs(value)

                log("")
                log("======== VOCODER LAEUFT ========")
                log("Samples:        ${samples.size}  (= ${"%.2f".format(seconds)} s bei $SAMPLE_RATE Hz)")
                log("Erwartet:       ${"%.2f".format(expected)} s aus $frames Frames")
                log("Rechenzeit:     $runMs ms")
                if (runMs > 0) log("Echtzeitfaktor: ${"%.2f".format(seconds * 1000 / runMs)} x")
                log("Spitzenpegel:   ${"%.3f".format(peak)}")
                ort.close()
                return
            } catch (error: Throwable) {
                log("  fehlgeschlagen: ${error.message?.take(300)}")
            }
        }

        ort.close()
        log("")
        log("Kein Eingabe-Layout hat funktioniert.")
    }

    private fun describe(info: ai.onnxruntime.NodeInfo): String {
        val tensor = info.info as? ai.onnxruntime.TensorInfo ?: return info.info.toString()
        return "${tensor.type} [${tensor.shape.joinToString(", ")}]"
    }

    private fun flatten(tensor: OnnxTensor): FloatArray {
        val buffer = tensor.floatBuffer
        val out = FloatArray(buffer.remaining())
        buffer.get(out)
        return out
    }

    private fun readFloats(
        outputs: OrtSession.Result,
        names: List<String>,
        name: String,
    ): FloatArray {
        val tensor = outputs.get(names.indexOf(name)) as OnnxTensor
        val buffer = tensor.floatBuffer
        val out = FloatArray(buffer.remaining())
        buffer.get(out)
        return out
    }

    private companion object {
        const val TAG = "QwenTtsBench"
        const val MODEL_NAME = "talker_decode_q.onnx"
        const val VOCODER_NAME = "tokenizer12hz_decode_q.onnx"

        const val MODE_TALKER = "talker"
        const val MODE_VOCODER = "vocoder"

        // Der 12-Hz-Tokenizer benutzt 16 Codebuecher; Codes liegen unter 1024.
        const val CODEBOOKS = 16
        const val CODE_RANGE = 1000
        const val SAMPLE_RATE = 24000

        // Architektur des Qwen3-TTS-0.6B-Talkers.
        const val LAYERS = 28
        const val KV_HEADS = 8
        const val HEAD_DIM = 128
        const val HIDDEN = 1024

        const val PROMPT_LEN = 100
        const val STEPS = 60
        const val THREADS = 4

        // 12-Hz-Tokenizer: 12 Schritte ergeben 1 Sekunde Audio.
        const val TOKENS_PER_AUDIO_SECOND = 12
    }
}
