package de.frank.jarvis.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import de.frank.jarvis.MainActivity
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.tts.WiedergabePegel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * Die schwebende Sprachblase: „Hey Jarvis“ aus einer anderen App öffnet Jarvis nicht, das Gespräch läuft im Hintergrund
 * und oben mittig zeigt eine kleine Blase (etwa 2 × 0,7 cm) ein Pegel-Spektrum. Immer in Orange, unabhängig von Tag,
 * Nacht und Design (Franks Wunsch): sprechen Frank oder Jarvis, zittert das Spektrum nach dem Pegel, dazwischen eine
 * ruhige Welle: Jarvis hört noch zu.
 *
 * Sichtbar genau dann, wenn der Sprachmodus läuft und Jarvis nicht vorn ist ([abgleichen] an Start, Ende, onStart und
 * onStop der Activity). Das Fenster nimmt nie den Fokus; nur die Blase selbst ist berührbar: Antippen öffnet Jarvis,
 * langes Drücken oder Wegwischen nach oben beendet den Sprachmodus. Gezeichnet mit dem Canvas statt Compose: leicht,
 * damit Spracherkennung und Ausgabe nichts davon merken.
 */
object SprachBlase {
    private var ansicht: BlasenAnsicht? = null

    fun darf(context: Context): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

    /** Nach „Hey Jarvis“ im Hintergrund. false = nicht gestartet (fehlender Schlüssel …), dann öffnet der Dienst die App wie bisher. */
    fun sprachmodusStarten(context: Context): Boolean {
        val vm = AppViewModel.hole(context)
        vm.weckwortStart(fortsetzen = false) {}
        if (!vm.sprachmodusAktiv) return false
        Logbuch.sprache("Sprachblase", "Hey Jarvis außerhalb der App")
        abgleichen(context)
        return true
    }

    /** Zeigt oder entfernt die Blase nach dem aktuellen Stand. Nur auf dem Hauptfaden. */
    fun abgleichen(context: Context) {
        val app = context.applicationContext
        val vm = AppViewModel.hole(app)
        val zeigen = vm.sprachmodusAktiv && !MainActivity.sichtbar && darf(app)
        val wm = app.getSystemService(WindowManager::class.java) ?: return
        val da = ansicht
        if (zeigen && da == null) {
            val neu = BlasenAnsicht(app, vm)
            val dp = app.resources.displayMetrics.density
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                // Ohne FLAG_LAYOUT_IN_SCREEN legt Android das Fenster unter Statusleiste und Kamera; hier nur etwas Luft.
                y = (6 * dp).toInt()
                title = "Jarvis-Sprachblase"
            }
            runCatching { wm.addView(neu, lp) }
                .onSuccess { ansicht = neu }
                .onFailure { Logbuch.fehler("Sprachblase ließ sich nicht einblenden", it) }
        } else if (!zeigen && da != null) {
            ansicht = null
            runCatching { wm.removeViewImmediate(da) }
        }
        WiedergabePegel.aktiv = ansicht != null
    }
}

@SuppressLint("ViewConstructor")
private class BlasenAnsicht(context: Context, private val vm: AppViewModel) : View(context) {
    private val dp = resources.displayMetrics.density
    // 2 cm × 0,7 cm: 160 dp je Zoll / 2,54 ≈ 63 dp je Zentimeter.
    private val breite = (126 * dp).toInt()
    private val hoehe = (44 * dp).toInt()
    private val balken = 13
    private val werte = FloatArray(balken)
    private var mischFrank = 0f
    private var mischJarvis = 0f
    private var zuletzt = SystemClock.uptimeMillis()

    private val flaeche = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rand = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * dp }
    private val glanz = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strich = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeWidth = 3.2f * dp }
    private val form = RectF()
    private var strichFarbe = 0
    private var strichHoehe = 0f

    private val gesten = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { oeffnen(); return true }
        override fun onLongPress(e: MotionEvent) { beenden() }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (vy < -600 * dp / 3 && abs(vy) > abs(vx)) { beenden(); return true }
            return false
        }
    })

    init {
        contentDescription = "Jarvis hört zu. Antippen öffnet Jarvis, lang drücken beendet den Sprachmodus."
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = setMeasuredDimension(breite, hoehe)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        flaeche.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xF2241A10.toInt(), 0xF20E0A06.toInt(), Shader.TileMode.CLAMP)
        glanz.shader = LinearGradient(0f, 0f, 0f, h * 0.5f, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gesten.onTouchEvent(event)

    private fun oeffnen() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        // Kein Neustart des Bildschirms bei der Rückkehr: Frank will genau dieses Gespräch sehen.
        vm.hintergrundSeit = 0L
        runCatching {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }.onFailure { Logbuch.fehler("Sprachblase: Jarvis ließ sich nicht öffnen", it) }
    }

    private fun beenden() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        vm.sprachmodusBeenden()
    }

    override fun onDraw(canvas: Canvas) {
        val jetzt = SystemClock.uptimeMillis()
        val schritt = ((jetzt - zuletzt).coerceIn(1, 100)) / 1000f
        zuletzt = jetzt
        val t = jetzt / 1000f

        val frank = vm.sprichtFrank
        val jarvis = !frank && vm.sprichtJarvis
        val pegel = when {
            frank -> vm.pegel.value
            // Ohne Messwert (Visualizer fehlt) eine lebendige Ersatzbewegung, damit man trotzdem sieht: Jarvis spricht.
            jarvis -> WiedergabePegel.pegel ?: (0.45f + 0.25f * sin(t * 9f) * sin(t * 3.7f))
            else -> 0f
        }
        mischFrank += ((if (frank) 1f else 0f) - mischFrank) * minOf(1f, schritt * 8f)
        mischJarvis += ((if (jarvis) 1f else 0f) - mischJarvis) * minOf(1f, schritt * 8f)

        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        form.set(rand.strokeWidth / 2, rand.strokeWidth / 2, w - rand.strokeWidth / 2, h - rand.strokeWidth / 2)
        canvas.drawRoundRect(form, r, r, flaeche)
        canvas.drawRoundRect(form, r, r, glanz)

        val farbe = mische(mische(RUHE, FRANK, mischFrank), JARVIS, mischJarvis)
        rand.color = (farbe and 0x00FFFFFF) or (0x70 shl 24)
        canvas.drawRoundRect(form, r, r, rand)

        val abstand = 5.6f * dp
        val start = w / 2 - abstand * (balken - 1) / 2
        val maxHoehe = h * 0.62f
        val mitte = h / 2
        // Neuer Verlauf nur bei anderer Farbe: sonst entstünden 60 bis 120 Shader je Sekunde neben dem Live-Ton.
        if (farbe != strichFarbe || maxHoehe != strichHoehe) {
            strichFarbe = farbe
            strichHoehe = maxHoehe
            strich.shader = LinearGradient(0f, mitte - maxHoehe / 2, 0f, mitte + maxHoehe / 2, aufhellen(farbe), farbe, Shader.TileMode.MIRROR)
        }
        for (i in 0 until balken) {
            val lage = (i - (balken - 1) / 2f) / ((balken - 1) / 2f)
            val huelle = 1f - 0.55f * lage * lage
            // Jeder Balken mit eigener Phase: das Spektrum zittert nach dem Pegel, nicht alle gleich.
            val zittern = 0.55f + 0.45f * sin(t * (7f + i * 1.3f) + i * 2.1f)
            val ziel = if (pegel > 0.02f) (pegel * 1.25f).coerceAtMost(1f) * huelle * zittern
            else 0.10f + 0.07f * sin(t * 2.4f - i * 0.55f) // ruhige Welle: hört noch zu
            val alt = werte[i]
            val tempo = if (ziel > alt) 22f else 7f
            werte[i] = alt + (ziel - alt) * minOf(1f, schritt * tempo)
            val hb = max(3.2f * dp, werte[i] * maxHoehe)
            val x = start + i * abstand
            canvas.drawLine(x, mitte - hb / 2 + strich.strokeWidth / 2, x, mitte + hb / 2 - strich.strokeWidth / 2, strich)
        }
        // Läuft weiter, solange die Blase hängt; endet der Sprachmodus, nimmt [SprachBlase.abgleichen] sie weg.
        if (!vm.sprachmodusAktiv || MainActivity.sichtbar) post { SprachBlase.abgleichen(context) }
        else postInvalidateOnAnimation()
    }

    private fun mische(a: Int, b: Int, f: Float): Int {
        val g = f.coerceIn(0f, 1f)
        fun k(s: Int) = (((a shr s) and 0xFF) * (1 - g) + ((b shr s) and 0xFF) * g).toInt() shl s
        return (0xFF shl 24) or k(16) or k(8) or k(0)
    }

    private fun aufhellen(c: Int) = mische(c, 0xFFFFFFFF.toInt(), 0.15f)

    companion object {
        // Immer das knallige Claude-Code-Orange (#D97757), egal wer spricht (Franks Wunsch).
        private const val FRANK = 0xFFD97757.toInt()
        private const val JARVIS = 0xFFD97757.toInt()
        private const val RUHE = 0xFFD97757.toInt()
    }
}
