package de.frank.jarvis.fernsteuerung

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.ParakeetLokal
import de.frank.jarvis.weckwort.Weckwort
import de.frank.module.transkriptionsmodul.GroqTranscriber
import de.frank.module.transkriptionsmodul.TranskriptionAnbindung
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.Intent
import android.widget.Toast

/**
 * Die Konsole des Hostinger-Servers (OpenLauncherServer/web): Sitzungen, laufende CLI, OpenLauncher, System. Nur im WireGuard-Tunnel erreichbar.
 * Anmeldung mit demselben Zugang wie die Fernsteuerung; HTTP ist nur für 10.8.0.1 erlaubt (res/xml/netz_regeln.xml), der Tunnel verschlüsselt.
 * Adressen außerhalb des Tunnels (Anmeldeseiten der Anbieter) öffnet der Browser des Handys; ohne Mehrfenster-Unterstützung kommt auch
 * target=_blank bei shouldOverrideUrlLoading an.
 */
class ServerKonsoleActivity : ComponentActivity() {
    private lateinit var ansicht: WebView
    private val adresse = "http://${Fernziel.SERVER.adresse}:${Fernziel.SERVER.port}"
    private var angemeldet = false

    private fun anmelden() {
        angemeldet = true
        ansicht.postUrl("$adresse/anmelden", ("token=" + Uri.encode(MacSteuerung.token(this))).toByteArray())
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ansicht = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#10131A"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(Stimme(), "JarvisSprache")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == Fernziel.SERVER.adresse) return false
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }

                // Ein Cookie, das nicht mehr gilt (Zugang gewechselt), führt zum Formular: dann einmal selbst anmelden.
                override fun onPageFinished(view: WebView, url: String) {
                    if (angemeldet) return
                    view.evaluateJavascript("!!document.querySelector('form[action=\"/anmelden\"]')") { formular ->
                        if (formular == "true" && !angemeldet) { angemeldet = true; anmelden() }
                    }
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) Toast.makeText(this@ServerKonsoleActivity, "Server-Konsole nicht erreichbar: ${error.description}", Toast.LENGTH_LONG).show()
                }
            }
        }
        // Vollbild ohne Statusleiste: Sie lag sonst über den Reitern und schluckte jeden Tipper dort. Der Rahmen hält die Seite frei von
        // Kamera-Aussparung, App-Griff (Fold, oben Mitte) und Tastatur; wegen des Vollbilds übernimmt das hier nicht adjustResize.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rahmen = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#10131A"))
            addView(ansicht)
        }
        ViewCompat.setOnApplyWindowInsetsListener(rahmen) { v, insets ->
            val r = insets.getInsets(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.captionBar() or WindowInsetsCompat.Type.ime())
            v.setPadding(r.left, r.top, r.right, r.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(rahmen)
        leistenAusblenden()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        // POST statt Adresse: So steht der Zugang in keinem Verlauf; danach gilt ein davon abgeleitetes Cookie.
        // Mit gültigem Cookie direkt zur Seite: spart einen Weg über den Tunnel.
        if (CookieManager.getInstance().getCookie(adresse)?.contains("konsole=") == true) ansicht.loadUrl("$adresse/")
        else anmelden()
    }

    /**
     * Sprache für die Konsole, das Gegenstück zum TVO an den Rechnern: Das Handy nimmt auf und wandelt mit Franks Diktat-Einstellung um (Parakeet auf dem
     * Handy oder Groq), der Text geht über die Seite ins Terminal. Auf dem Server liegt dafür weder ein Mikrofon noch ein Schlüssel. Die Seite ist immer
     * 10.8.0.1 (alles andere öffnet der Browser des Handys), nur sie sieht diese Schnittstelle.
     */
    private inner class Stimme {
        @JavascriptInterface fun start() { runOnUiThread { aufnahmeStarten() } }
        @JavascriptInterface fun stop() { runOnUiThread { aufnahmeBeenden() } }
    }

    private var aufnahme: AudioRecord? = null
    private var aufnahmeFaden: Job? = null
    private val puffer = ByteArrayOutputStream()

    private fun melden(text: String, fehler: String = "") {
        ansicht.evaluateJavascript("stimmeFertig(${JSONObject.quote(text)}, ${JSONObject.quote(fehler)})", null)
    }

    private fun aufnahmeStarten() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            melden("", "Bitte das Mikrofon erlauben und noch einmal tippen.")
            return
        }
        aufnahmeFaden = lifecycleScope.launch {
            Weckwort.mikrofonHolen()
            piep()
            val groesse = maxOf(AudioRecord.getMinBufferSize(HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), HZ)
            @SuppressLint("MissingPermission")
            val mikro = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, groesse)
            aufnahme = mikro
            synchronized(puffer) { puffer.reset() }
            withContext(Dispatchers.IO) {
                mikro.startRecording()
                val stueck = ByteArray(4096)
                while (isActive) {
                    val n = mikro.read(stueck, 0, stueck.size)
                    if (n <= 0) break
                    synchronized(puffer) { puffer.write(stueck, 0, n) }
                }
            }
        }
    }

    private fun aufnahmeFreigeben() {
        aufnahmeFaden?.cancel()
        aufnahmeFaden = null
        aufnahme?.let { m -> runCatching { m.stop() }; m.release() }
        aufnahme = null
        Weckwort.mikrofonFreigeben()
    }

    private fun aufnahmeBeenden() {
        aufnahmeFreigeben()
        val pcm = synchronized(puffer) { puffer.toByteArray() }
        lifecycleScope.launch {
            piep()
            try {
                if (pcm.size < HZ) { melden("", "Die Aufnahme war zu kurz."); return@launch }
                val wav = withContext(Dispatchers.Default) { alsWav(pcm) }
                val text = SecureSettings(this@ServerKonsoleActivity).use { s ->
                    if (s.transkriptionLokal) ParakeetLokal.transkribiere(this@ServerKonsoleActivity, wav, null, s.filterStilleVorabAn, "")
                    else withContext(Dispatchers.IO) {
                        val groq = GroqTranscriber(apiKey = s.groqApiKey, filterStille = s.filterStilleVorabAn, filterMetriken = s.filterSegmentmetrikenAn,
                            filterZeitstempel = s.filterZeitstempelAn, filterFloskeln = s.filterFloskelnAn, model = s.whisperModell)
                        try { TranskriptionAnbindung.diktat(groq).transkribiere(wav, null).text } finally { groq.shutdown() }
                    }
                }.trim()
                melden(text, if (text.isEmpty()) "Ich habe nichts verstanden." else "")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logbuch.fehler("Konsole: Mitschrift", e)
                melden("", e.message ?: "Die Aufnahme konnte nicht in Text umgewandelt werden.")
            }
        }
    }

    private suspend fun piep() {
        val ton = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull() ?: return
        try { ton.startTone(ToneGenerator.TONE_PROP_BEEP, 120); delay(160) } finally { ton.release() }
    }

    private fun alsWav(pcm: ByteArray): ByteArray {
        val kopf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVEfmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(HZ); putInt(HZ * 2); putShort(2); putShort(16); put("data".toByteArray()); putInt(pcm.size)
        }.array()
        return kopf + pcm
    }

    override fun onPause() {
        // Im Hintergrund nie weiter aufnehmen.
        if (aufnahme != null) { aufnahmeFreigeben(); melden("", "Die Aufnahme wurde beendet, weil die Konsole nicht mehr vorn war.") }
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Nach dem Herunterwischen oder der Tastatur wieder ausblenden.
        if (hasFocus) leistenAusblenden()
    }

    private fun leistenAusblenden() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    private companion object { const val HZ = 16_000 }

    override fun onDestroy() {
        ansicht.destroy()
        super.onDestroy()
    }
}
