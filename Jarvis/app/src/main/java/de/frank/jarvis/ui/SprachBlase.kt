package de.frank.jarvis.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.BroadcastReceiver
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
import android.text.TextPaint
import android.text.TextUtils
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
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Die schwebende Sprachblase: „Hey Jarvis“ aus einer anderen App öffnet Jarvis nicht, das Gespräch läuft im Hintergrund
 * und oben mittig zeigt eine kleine Blase (etwa 2 × 0,7 cm) ein Pegel-Spektrum. Immer in Orange, unabhängig von Tag,
 * Nacht und Design (Franks Wunsch): sprechen Frank oder Jarvis, zittert das Spektrum nach dem Pegel, dazwischen eine
 * ruhige Welle: Jarvis hört noch zu. Jede Phase hat ihre eigene Bewegung ([BlasenZustand]), und eine kleine Zeile darunter sagt in Worten, was gerade läuft.
 *
 * Sichtbar genau dann, wenn der Sprachmodus läuft und Jarvis nicht vorn ist ([abgleichen] an Start, Ende, onStart und
 * onStop der Activity). Das Fenster nimmt nie den Fokus; nur die Blase selbst ist berührbar: Antippen öffnet Jarvis,
 * langes Drücken oder Wegwischen nach oben beendet den Sprachmodus. Gezeichnet mit dem Canvas statt Compose: leicht,
 * damit Spracherkennung und Ausgabe nichts davon merken.
 */
/** Was Jarvis gerade tut, aus dem echten Stand gelesen ([AppViewModel.blasenZustand]). */
enum class BlasenZustand { MIKROFON, HOERT, FRANK_SPRICHT, WARTET_AUF_ENDE, SCHREIBT_MIT, DENKT, WERKZEUG, STIMME, SPRICHT, ANGEHALTEN }

/**
 * Messlauf über adb, nur für Shell und System (Manifest: Recht DUMP). Startet die Kette wie nach dem Weckwort, mit Text statt Sprache:
 * `adb shell am broadcast -a de.frank.jarvis.SPRACH_MESSLAUF -p de.frank.jarvis --es frage "Wie spät ist es?"`
 * Mit `--ez ohne_zwischenspeicher true` ohne Zwischenspeicher-Kennung, zum Vergleich. Ergebnis: „Kette Mikrofon“ und „Blase“ im Logbuch.
 */
class SprachMesslauf : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val frage = intent.getStringExtra("frage")?.takeIf { it.isNotBlank() } ?: return
        de.frank.jarvis.auth.CodexZwischenspeicherSchalter.setzen(!intent.getBooleanExtra("ohne_zwischenspeicher", false))
        AppViewModel.hole(context).sprachMesslauf(context.applicationContext, frage)
    }
}

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
    // Die Statuszeile unter der Blase; das Fenster wächst nur so weit wie ihr Text, damit es nichts Fremdes verdeckt.
    private val zeileAbstand = 4 * dp
    private val zeileHoehe = 24 * dp
    private val zeileRand = 11 * dp
    private val zeileMax = 280 * dp
    private val balken = 13
    private val werte = FloatArray(balken)
    private var zuletzt = SystemClock.uptimeMillis()

    private val flaeche = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rand = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * dp; color = (FARBE and 0x00FFFFFF) or (0xD0 shl 24) }
    private val glanz = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strich = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeWidth = 3.2f * dp }
    private val zeileFlaeche = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2140E08.toInt() }
    private val zeileText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFE9D6.toInt()
        textSize = 12.5f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val form = RectF()
    private var strichHoehe = 0f

    private var zustand = vm.blasenZustand()
    private var zustandSeit = SystemClock.uptimeMillis()
    private var textStand = -1
    private var textSchritt = ""
    private var text = ""
    private var textBreite = 0f
    /** Kam in dieser Wiedergabe schon ein echter Messwert? Dann sind Pausen echte Pausen; sonst misst der Visualizer nichts und die Ersatzbewegung läuft. */
    private var wiedergabeGemessen = false

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
        textAktualisieren(SystemClock.uptimeMillis())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(maxOf(breite, (textBreite + 2 * zeileRand).toInt() + 1), hoehe + (zeileAbstand + zeileHoehe).toInt() + 1)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        flaeche.shader = LinearGradient(0f, 0f, 0f, hoehe.toFloat(), 0xF2241A10.toInt(), 0xF20E0A06.toInt(), Shader.TileMode.CLAMP)
        glanz.shader = LinearGradient(0f, 0f, 0f, hoehe * 0.5f, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gesten.onTouchEvent(event)

    private fun oeffnen() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        runCatching {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }.onFailure { Logbuch.fehler("Sprachblase: Jarvis ließ sich nicht öffnen", it) }
    }

    private fun beenden() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        vm.sprachmodusBeenden()
    }

    /** Der Wortlaut ändert sich nur bei einem Wechsel oder einmal je Sekunde; dazwischen entsteht hier kein neuer Text. */
    private fun textAktualisieren(jetzt: Long) {
        val neu = vm.blasenZustand()
        if (neu != zustand) {
            Logbuch.sprache("Blase", "${zustand.name} → ${neu.name}", jetzt - zustandSeit)
            zustand = neu
            zustandSeit = jetzt
            wiedergabeGemessen = false
            textStand = -1
        }
        val schritt = if (zustand == BlasenZustand.WERKZEUG) vm.schritt else ""
        val sekunden = if (zustand == BlasenZustand.WARTET_AUF_ENDE) ceil(vm.blasenStilleRestMs() / 1000f).toInt() else ((jetzt - zustandSeit) / 1000).toInt()
        if (sekunden == textStand && schritt == textSchritt) return
        textStand = sekunden
        textSchritt = schritt
        // Ab 5 Sekunden zählt die Zeile mit: So ist sicher zu sehen, dass die Arbeit läuft und die App nicht hängt.
        val dauer = if (sekunden >= 5) " · $sekunden s" else ""
        val roh = when (zustand) {
            BlasenZustand.MIKROFON -> "Jarvis öffnet das Mikrofon"
            BlasenZustand.HOERT -> "Jarvis hört zu"
            BlasenZustand.FRANK_SPRICHT -> "Jarvis hört dich"
            BlasenZustand.WARTET_AUF_ENDE -> "Jarvis wartet, ob du weitersprichst · $sekunden s"
            BlasenZustand.SCHREIBT_MIT -> "Jarvis schreibt mit$dauer"
            BlasenZustand.DENKT -> "Jarvis denkt nach$dauer"
            BlasenZustand.WERKZEUG -> "Jarvis: $schritt$dauer"
            BlasenZustand.STIMME -> "Jarvis bereitet die Stimme vor$dauer"
            BlasenZustand.SPRICHT -> "Jarvis spricht"
            BlasenZustand.ANGEHALTEN -> "Jarvis wartet auf dich"
        }
        text = TextUtils.ellipsize(roh, zeileText, zeileMax - 2 * zeileRand, TextUtils.TruncateAt.END).toString()
        contentDescription = "$text. Antippen öffnet Jarvis, lang drücken beendet den Sprachmodus."
        val alt = textBreite
        textBreite = zeileText.measureText(text)
        // Das Fenster nur anpassen, wenn es zu klein wird oder die Phase wechselt; die zählenden Ziffern lassen es nicht zappeln.
        if (textBreite > alt + 0.5f || (textBreite < alt - 8 * dp)) requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        val jetzt = SystemClock.uptimeMillis()
        val schritt = ((jetzt - zuletzt).coerceIn(1, 100)) / 1000f
        zuletzt = jetzt
        val t = jetzt / 1000f
        textAktualisieren(jetzt)

        val w = breite.toFloat()
        val h = hoehe.toFloat()
        val r = h / 2f
        val links = (width - breite) / 2f
        canvas.save()
        canvas.translate(links, 0f)
        form.set(rand.strokeWidth / 2, rand.strokeWidth / 2, w - rand.strokeWidth / 2, h - rand.strokeWidth / 2)
        canvas.drawRoundRect(form, r, r, flaeche)
        canvas.drawRoundRect(form, r, r, glanz)
        canvas.drawRoundRect(form, r, r, rand)

        val abstand = 5.6f * dp
        val start = w / 2 - abstand * (balken - 1) / 2
        val maxHoehe = h * 0.7f
        val mitte = h / 2
        // Neuer Verlauf nur bei anderer Höhe: sonst entstünden 60 bis 120 Shader je Sekunde neben dem Live-Ton.
        if (maxHoehe != strichHoehe) {
            strichHoehe = maxHoehe
            strich.shader = LinearGradient(0f, mitte - maxHoehe / 2, 0f, mitte + maxHoehe / 2, aufhellen(FARBE), FARBE, Shader.TileMode.MIRROR)
        }

        // Der Mikrofonpegel ist in normaler Handhaltung klein: gespreizt und über dem Grundrauschen, sonst sähe man nur Punkte.
        val mikro = ((vm.pegel.value - 0.03f) / 0.45f).coerceIn(0f, 1f).let { sqrt(it) }
        // Echter Ton, wenn der Visualizer ihn liefert. Bleibt er stumm (etwa auf manchen Samsung-Tonwegen), die Ersatzbewegung statt Stillstand.
        val ton = if (zustand == BlasenZustand.SPRICHT) WiedergabePegel.pegel else null
        if (ton != null && ton > 0.05f) wiedergabeGemessen = true
        val tonPegel = when {
            wiedergabeGemessen -> ton ?: 0f
            else -> 0.5f + 0.3f * sin(t * 9f) * sin(t * 3.7f)
        }
        for (i in 0 until balken) {
            val lage = (i - (balken - 1) / 2f) / ((balken - 1) / 2f)
            val huelle = 1f - 0.55f * lage * lage
            // Jeder Balken mit eigener Phase: das Spektrum zittert nach dem Pegel, nicht alle gleich.
            val zittern = 0.6f + 0.4f * sin(t * (7f + i * 1.3f) + i * 2.1f)
            var deckkraft = 255
            val ziel = when (zustand) {
                // Zuhören: große, ruhige Welle; das Mikrofon schwingt schon mit, bevor die Spracherkennung anschlägt.
                BlasenZustand.HOERT -> maxOf(0.24f + 0.16f * sin(t * 3f - i * 0.6f), mikro * huelle * zittern)
                // Frank spricht: das Spektrum folgt seiner Stimme und ist immer deutlich höher als beim Warten.
                BlasenZustand.FRANK_SPRICHT -> maxOf(0.34f, mikro) * huelle * zittern * 1.15f
                // Nach dem letzten Wort: nur noch ein langsames Atmen, gedimmt.
                BlasenZustand.WARTET_AUF_ENDE -> { deckkraft = 170; maxOf(0.14f + 0.08f * sin(t * 1.8f), mikro * huelle * zittern) }
                // Verarbeitung: ein wandernder Lichtpunkt statt eines Spektrums; jede Phase in ihrem eigenen Lauf.
                BlasenZustand.SCHREIBT_MIT -> { val g = puls(i, wippe(t * 5.2f), 1.3f); deckkraft = 90 + (165 * g).toInt(); 0.12f + 0.62f * g }
                BlasenZustand.DENKT, BlasenZustand.MIKROFON -> { val g = puls(i, wippe(t * 2.6f), 1.8f); deckkraft = 90 + (165 * g).toInt(); 0.12f + 0.78f * g }
                BlasenZustand.WERKZEUG -> { val p = wippe(t * 3.6f); val g = maxOf(puls(i, p, 1.2f), puls(i, balken - 1 - p, 1.2f)); deckkraft = 90 + (165 * g).toInt(); 0.12f + 0.72f * g }
                BlasenZustand.STIMME -> { val halb = (balken - 1) / 2f; val g = maxOf(puls(i, halb + wippe(t * 4.2f) / (balken - 1) * halb, 1.2f), puls(i, halb - wippe(t * 4.2f) / (balken - 1) * halb, 1.2f)); deckkraft = 90 + (165 * g).toInt(); 0.12f + 0.8f * g }
                // Jarvis spricht: vom Ton getriebene Balken, nach der Mitte hin am höchsten.
                BlasenZustand.SPRICHT -> if (tonPegel > 0.02f) (tonPegel * 1.3f).coerceAtMost(1f) * huelle * zittern else 0.1f
                BlasenZustand.ANGEHALTEN -> { deckkraft = 130; 0.14f }
            }
            val alt = werte[i]
            val tempo = if (ziel > alt) 22f else 9f
            werte[i] = alt + (ziel - alt) * minOf(1f, schritt * tempo)
            val hb = max(3.2f * dp, werte[i] * maxHoehe)
            val x = start + i * abstand
            strich.alpha = deckkraft
            canvas.drawLine(x, mitte - hb / 2 + strich.strokeWidth / 2, x, mitte + hb / 2 - strich.strokeWidth / 2, strich)
        }
        canvas.restore()

        // Die Statuszeile: dieselbe dunkle Fläche wie die Blase, damit sie über jeder App lesbar bleibt.
        val zeileOben = h + zeileAbstand
        val halbe = textBreite / 2 + zeileRand
        val mitteX = width / 2f
        form.set(mitteX - halbe, zeileOben, mitteX + halbe, zeileOben + zeileHoehe)
        canvas.drawRoundRect(form, zeileHoehe / 2, zeileHoehe / 2, zeileFlaeche)
        canvas.drawText(text, mitteX, zeileOben + zeileHoehe / 2 - (zeileText.descent() + zeileText.ascent()) / 2, zeileText)

        // Läuft weiter, solange die Blase hängt; endet der Sprachmodus, nimmt [SprachBlase.abgleichen] sie weg.
        if (!vm.sprachmodusAktiv || MainActivity.sichtbar) post { SprachBlase.abgleichen(context) }
        else postInvalidateOnAnimation()
    }

    /** Hin und her über alle Balken, 0 bis balken - 1. */
    private fun wippe(phase: Float) = (sin(phase) * 0.5f + 0.5f) * (balken - 1)

    /** Wie stark Balken [i] vom Lichtpunkt bei [mitte] getroffen wird, 0 bis 1. */
    private fun puls(i: Int, mitte: Float, breite: Float): Float { val d = i - mitte; return exp(-(d * d) / (2f * breite * breite)) }

    private fun mische(a: Int, b: Int, f: Float): Int {
        val g = f.coerceIn(0f, 1f)
        fun k(s: Int) = (((a shr s) and 0xFF) * (1 - g) + ((b shr s) and 0xFF) * g).toInt() shl s
        return (0xFF shl 24) or k(16) or k(8) or k(0)
    }

    private fun aufhellen(c: Int) = mische(c, 0xFFFFB000.toInt(), 0.45f) // zu Gelb-Orange statt Weiß: bleibt satt

    companion object {
        // Immer leuchtendes, knalliges Orange (#FF6D00), in jeder Phase (Franks Wunsch).
        private const val FARBE = 0xFFFF6D00.toInt()
    }
}
