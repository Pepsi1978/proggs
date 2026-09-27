package de.frank.wecker

import android.os.Build
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Gerätesonde: meldet, ob und für welche Sprachen die Offline-Erkennung verfügbar ist. Behauptet bewusst nichts. */
@RunWith(AndroidJUnit4::class)
class OfflineDiktatGeraeteTest {
    @Test fun meldeOfflineErkennung() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val context = instr.targetContext
        val bericht = StringBuilder("SDK=${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            var verfuegbar = false
            instr.runOnMainSync { verfuegbar = SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
            bericht.append(" onDeviceVerfuegbar=$verfuegbar")
            if (verfuegbar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf("de-DE", "en-US", "fr-FR", "es-ES").forEach { tag ->
                    val fertig = CountDownLatch(1)
                    var zeile = "$tag: keine Antwort"
                    var erkenner: SpeechRecognizer? = null
                    instr.runOnMainSync {
                        erkenner = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                        val absicht = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                        erkenner!!.checkRecognitionSupport(absicht, context.mainExecutor, object : RecognitionSupportCallback {
                            override fun onSupportResult(s: RecognitionSupport) {
                                zeile = "$tag: installiert=${s.installedOnDeviceLanguages} ladend=${s.pendingOnDeviceLanguages} ladbar=${s.supportedOnDeviceLanguages}"
                                fertig.countDown()
                            }
                            override fun onError(fehler: Int) { zeile = "$tag: Fehler $fehler"; fertig.countDown() }
                        })
                    }
                    fertig.await(10, TimeUnit.SECONDS)
                    instr.runOnMainSync { erkenner?.destroy() }
                    bericht.append(" | ").append(zeile)
                }
            }
        }
        android.util.Log.i("OfflineDiktatSonde", bericht.toString())
        println("OfflineDiktatSonde: $bericht")
        instr.sendStatus(0, android.os.Bundle().apply { putString("sonde", bericht.toString()) })
        assertTrue(true)
    }
}
