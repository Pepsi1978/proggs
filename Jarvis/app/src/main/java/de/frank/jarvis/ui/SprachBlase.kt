package de.frank.jarvis.ui

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
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

/** Was Jarvis gerade tut, aus dem echten Stand gelesen ([AppViewModel.blasenZustand]). */
enum class BlasenZustand { MIKROFON, HOERT, FRANK_SPRICHT, WARTET_AUF_ENDE, SCHREIBT_MIT, DENKT, WERKZEUG, STIMME, SPRICHT, ANGEHALTEN }

/**
 * Messlauf über adb, nur für Shell und System (Manifest: Recht DUMP). Startet die Kette wie nach dem Weckwort, mit Text statt Sprache:
 * `adb shell am broadcast -a de.frank.jarvis.SPRACH_MESSLAUF -p de.frank.jarvis --es frage "Wie spät ist es?"`
 * Ergebnis: „Kette Mikrofon“ und „Blase“ im Logbuch. Nicht, solange Jarvis vorn ist oder schon ein Sprachgespräch läuft.
 */
class SprachMesslauf : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val frage = intent.getStringExtra("frage")?.takeIf { it.isNotBlank() } ?: return
        // Jarvis ist vorn: Frank arbeitet gerade darin, der Lauf begänne ihm mitten hinein ein neues Gespräch.
        if (MainActivity.sichtbar) { Logbuch.sprache("Messlauf", "abgelehnt: Jarvis ist vorn"); return }
        AppViewModel.hole(context).sprachMesslauf(context.applicationContext, frage)
    }
}

/**
 * Die schwebende Sprachblase: „Hey Jarvis“ aus einer anderen App öffnet Jarvis nicht, das Gespräch läuft im Hintergrund
 * und oben mittig zeigt eine kleine Blase (etwa 2 × 0,7 cm) ein Pegel-Spektrum. Immer in Orange, unabhängig von Tag,
 * Nacht und Design (Franks Wunsch). Jede Phase hat ihre eigene Bewegung ([BlasenZustand]), und eine kleine Zeile darunter
 * sagt in Worten, was gerade läuft.
 *
 * Sichtbar genau dann, wenn der Sprachmodus läuft und Jarvis nicht vorn ist ([abgleichen] an Start, Ende, onStart und
 * onStop der Activity). Die Fenster nehmen nie den Fokus; nur die Blase selbst ist berührbar: Antippen öffnet Jarvis,
 * langes Drücken oder Wegwischen nach oben beendet den Sprachmodus. Die Statuszeile liegt in einem eigenen Fenster, das
 * Berührungen an die App darunter durchlässt. Gezeichnet mit dem Canvas statt Compose: leicht, damit Spracherkennung und
 * Ausgabe nichts davon merken.
 */
object SprachBlase {
    private var ansicht: BlasenAnsicht? = null
    private var zeile: StatusZeile? = null
    private val haupt = Handler(Looper.getMainLooper())
    private var neuEingehaengt = 0
    private var letztesEinhaengen = 0L
    private var bedienbarSeit = 0L
    private var waechterKontext: Context? = null

    // Hier statt in der Ansicht: Neu eingehängt zählen Statuszeile und Logbuch sonst wieder bei null.
    internal var zustand = BlasenZustand.MIKROFON
    internal var zustandSeit = 0L

    /**
     * Schaut zweimal je Sekunde, ob die Blase noch zeichnet. Android kann das Fenster eines Hintergrund-Prozesses anhalten (etwa bei einem
     * App-Wechsel); dann stünde sie sichtbar still, obwohl Jarvis arbeitet. Erst anstoßen, hilft das nicht, das Fenster neu einhängen.
     * Über dem Sperrbildschirm und kurz nach dem Einschalten zeichnet sie zu Recht nicht: Das zählt nicht als Stillstand.
     */
    private val waechter = object : Runnable {
        override fun run() {
            val da = ansicht ?: return
            val app = waechterKontext ?: return
            val jetzt = SystemClock.uptimeMillis()
            val stand = jetzt - da.zuletzt
            val bedienbar = app.getSystemService(PowerManager::class.java)?.isInteractive != false &&
                app.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != true
            if (!bedienbar) bedienbarSeit = jetzt
            // Länger sauber gezeichnet: Der nächste Halter hat wieder alle Versuche.
            if (stand < 300 && jetzt - letztesEinhaengen > 15_000) neuEingehaengt = 0
            if (bedienbar && jetzt - bedienbarSeit > 3_000 && stand > 1_500 && neuEingehaengt < 5) {
                neuEingehaengt++
                letztesEinhaengen = jetzt
                Logbuch.sprache("Blase neu eingehängt", "stand $stand ms, Versuch $neuEingehaengt")
                val wm = app.getSystemService(WindowManager::class.java)
                entfernen(wm)
                // Plant den Wächter selbst neu ein, wenn die Blase wieder hängt.
                abgleichen(app)
                return
            }
            if (bedienbar && stand > 500) da.invalidate()
            haupt.postDelayed(this, 500)
        }
    }

    fun darf(context: Context): Boolean = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

    /** Nach „Hey Jarvis“ im Hintergrund. false = nicht gestartet (fehlender Schlüssel …), dann öffnet der Dienst die App wie bisher. */
    fun sprachmodusStarten(context: Context): Boolean {
        val vm = AppViewModel.hole(context)
        vm.weckwortStart(fortsetzen = false) {}
        if (!vm.sprachmodusAktiv) return false
        Logbuch.sprache("Sprachblase", "Hey Jarvis außerhalb der App")
        neuEingehaengt = 0
        // Neues Gespräch: Die Dauer im Logbuch und der Sekundenzähler beginnen frisch.
        zustandSeit = 0L
        abgleichen(context)
        return true
    }

    private fun entfernen(wm: WindowManager?) {
        val blase = ansicht
        val text = zeile
        ansicht = null
        zeile = null
        haupt.removeCallbacks(waechter)
        if (wm != null) {
            if (blase != null) runCatching { wm.removeViewImmediate(blase) }
            if (text != null) runCatching { wm.removeViewImmediate(text) }
        }
    }

    /** Zeigt oder entfernt die Blase nach dem aktuellen Stand. Nur auf dem Hauptfaden. */
    fun abgleichen(context: Context) {
        val app = context.applicationContext
        val vm = AppViewModel.hole(app)
        val zeigen = vm.sprachmodusAktiv && !MainActivity.sichtbar && darf(app)
        val wm = app.getSystemService(WindowManager::class.java) ?: return
        if (zeigen && ansicht == null) {
            val dp = app.resources.displayMetrics.density
            fun fenster(flags: Int, name: String, abstand: Float) = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                // Ohne FLAG_LAYOUT_IN_SCREEN legt Android das Fenster unter Statusleiste und Kamera; hier nur etwas Luft.
                y = (abstand * dp).toInt()
                title = name
            }
            val nichtFokus = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            val text = StatusZeile(app)
            val neu = BlasenAnsicht(app, vm, text)
            runCatching { wm.addView(neu, fenster(nichtFokus, "Jarvis-Sprachblase", 6f)) }
                .onSuccess {
                    ansicht = neu
                    // Die Zeile ist nur zum Lesen: Berührungen gehen durch sie hindurch an die App darunter.
                    runCatching { wm.addView(text, fenster(nichtFokus or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, "Jarvis-Statuszeile", 6f + BlasenAnsicht.HOEHE_DP + 4f)) }
                        .onSuccess { zeile = text }
                        .onFailure { Logbuch.fehler("Statuszeile ließ sich nicht einblenden", it) }
                    waechterKontext = app
                    bedienbarSeit = SystemClock.uptimeMillis()
                    haupt.removeCallbacks(waechter)
                    haupt.postDelayed(waechter, 500)
                }
                .onFailure { Logbuch.fehler("Sprachblase ließ sich nicht einblenden", it) }
        } else if (!zeigen && ansicht != null) {
            neuEingehaengt = 0
            entfernen(wm)
        }
        if (!zeigen) neuEingehaengt = 0
        WiedergabePegel.aktiv = ansicht != null
    }
}

/** Die Zeile unter der Blase: zeichnet nur, wenn sich ihr Wortlaut ändert, nicht je Bild. */
@SuppressLint("ViewConstructor")
private class StatusZeile(context: Context) : View(context) {
    private val dp = resources.displayMetrics.density
    private val rand = 11 * dp
    private val flaeche = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2140E08.toInt() }
    private val schrift = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFE9D6.toInt()
        // applyDimension kennt auch die nichtlineare Schriftskalierung ab Android 14.
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12.5f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
    }
    // Aus der Schrift berechnet: Bei großer Systemschrift wächst die Zeile mit, statt den Text abzuschneiden.
    private val zeilenHoehe = max(24 * dp, schrift.fontMetrics.let { it.descent - it.ascent } + 8 * dp)
    private val maxBreite = 300 * dp
    private val form = RectF()
    private var text = ""
    private var breite = 0f

    fun setze(neu: String) {
        val gekuerzt = TextUtils.ellipsize(neu, schrift, maxBreite - 2 * rand, TextUtils.TruncateAt.END).toString()
        if (gekuerzt == text) return
        text = gekuerzt
        val alt = breite
        breite = schrift.measureText(text)
        // Die zählenden Ziffern lassen das Fenster nicht zappeln: neu messen nur bei spürbar anderer Breite.
        if (breite > alt + 0.5f || breite < alt - 8 * dp) requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension((breite + 2 * rand).toInt() + 1, zeilenHoehe.toInt() + 1)

    override fun onDraw(canvas: Canvas) {
        form.set(0f, 0f, width.toFloat(), zeilenHoehe)
        canvas.drawRoundRect(form, zeilenHoehe / 2, zeilenHoehe / 2, flaeche)
        canvas.drawText(text, width / 2f, zeilenHoehe / 2 - (schrift.descent() + schrift.ascent()) / 2, schrift)
    }
}

@SuppressLint("ViewConstructor")
private class BlasenAnsicht(context: Context, private val vm: AppViewModel, private val zeile: StatusZeile) : View(context) {
    private val dp = resources.displayMetrics.density
    // 2 cm × 0,7 cm: 160 dp je Zoll / 2,54 ≈ 63 dp je Zentimeter.
    private val breite = (126 * dp).toInt()
    private val hoehe = (HOEHE_DP * dp).toInt()
    private val balken = 13
    private val werte = FloatArray(balken)
    /** Das letzte gezeichnete Bild; daran erkennt [SprachBlase], ob die Zeichenschleife noch läuft. */
    var zuletzt = SystemClock.uptimeMillis()
        private set
    private var zuletztGezeichnet = false

    private val flaeche = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rand = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * dp; color = (FARBE and 0x00FFFFFF) or (0xD0 shl 24) }
    private val glanz = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strich = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeWidth = 3.2f * dp }
    private val form = RectF()
    private val maxHoehe get() = hoehe * 0.7f

    private var textStand = -1
    private var textSchritt = ""
    private var letzterRoh = ""
    /** Kam in dieser Wiedergabe gerade ein echter Messwert? Dann sind Pausen echte Pausen; sonst läuft die Ersatzbewegung. */
    private var wiedergabeGemessen = false
    private var tonZuletzt = 0L

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = setMeasuredDimension(breite, hoehe)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        flaeche.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xF2241A10.toInt(), 0xF20E0A06.toInt(), Shader.TileMode.CLAMP)
        glanz.shader = LinearGradient(0f, 0f, 0f, h * 0.5f, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        val mitte = h / 2f
        // Nur hier: In onDraw entstünden 60 bis 120 Shader je Sekunde neben dem Live-Ton.
        strich.shader = LinearGradient(0f, mitte - maxHoehe / 2, 0f, mitte + maxHoehe / 2, aufhellen(FARBE), FARBE, Shader.TileMode.MIRROR)
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
        if (SprachBlase.zustandSeit == 0L) SprachBlase.zustandSeit = jetzt
        if (neu != SprachBlase.zustand) {
            Logbuch.sprache("Blase", "${SprachBlase.zustand.name} → ${neu.name}", jetzt - SprachBlase.zustandSeit)
            SprachBlase.zustand = neu
            SprachBlase.zustandSeit = jetzt
            wiedergabeGemessen = false
            textStand = -1
        }
        val zustand = SprachBlase.zustand
        val schritt = if (zustand == BlasenZustand.WERKZEUG) vm.taetigkeit else ""
        val sekunden = if (zustand == BlasenZustand.WARTET_AUF_ENDE) ceil(vm.blasenStilleRestMs() / 1000f).toInt() else ((jetzt - SprachBlase.zustandSeit) / 1000).toInt()
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
            BlasenZustand.WERKZEUG -> "$schritt$dauer"
            BlasenZustand.STIMME -> "Jarvis bereitet die Stimme vor$dauer"
            BlasenZustand.SPRICHT -> "Jarvis spricht"
            BlasenZustand.ANGEHALTEN -> "Jarvis wartet auf dich"
        }
        if (roh == letzterRoh) return
        letzterRoh = roh
        zeile.setze(roh)
        contentDescription = "$roh. Antippen öffnet Jarvis, lang drücken beendet den Sprachmodus."
    }

    override fun onDraw(canvas: Canvas) {
        val jetzt = SystemClock.uptimeMillis()
        val schritt = ((jetzt - zuletzt).coerceIn(1, 100)) / 1000f
        if (jetzt - zuletzt > 1_000 && zuletztGezeichnet) Logbuch.sprache("Blase stand still", "Zustand ${SprachBlase.zustand.name}", jetzt - zuletzt)
        zuletzt = jetzt
        zuletztGezeichnet = true
        val t = jetzt / 1000f
        textAktualisieren(jetzt)
        val zustand = SprachBlase.zustand

        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        form.set(rand.strokeWidth / 2, rand.strokeWidth / 2, w - rand.strokeWidth / 2, h - rand.strokeWidth / 2)
        canvas.drawRoundRect(form, r, r, flaeche)
        canvas.drawRoundRect(form, r, r, glanz)
        canvas.drawRoundRect(form, r, r, rand)

        val abstand = 5.6f * dp
        val start = w / 2 - abstand * (balken - 1) / 2
        val mitte = h / 2

        // Der Mikrofonpegel ist in normaler Handhaltung klein: gespreizt und über dem Grundrauschen, sonst sähe man nur Punkte.
        val mikro = ((vm.pegel.value - 0.03f) / 0.45f).coerceIn(0f, 1f).let { sqrt(it) }
        // Ohne erkannte Stimme bleibt die Welle ruhig: Lüfter und Fahrgeräusch im Auto dürfen nicht nach Zuhören mit Sprache aussehen.
        val nebenbei = if (vm.blasenMitStimmerkennung) 0f else mikro
        // Echter Ton, wenn der Visualizer ihn liefert. Bleibt er stumm (etwa auf manchen Samsung-Tonwegen), die Ersatzbewegung statt Stillstand.
        val ton = if (zustand == BlasenZustand.SPRICHT) WiedergabePegel.pegel else null
        if (ton != null) {
            tonZuletzt = jetzt
            if (ton > 0.05f) wiedergabeGemessen = true
        } else if (jetzt - tonZuletzt > 1_000) {
            // Der Visualizer entsteht je Absatz neu und kann einzeln scheitern.
            wiedergabeGemessen = false
        }
        val tonPegel = if (wiedergabeGemessen) ton ?: 0f else 0.5f + 0.3f * sin(t * 9f) * sin(t * 3.7f)
        val halb = (balken - 1) / 2f
        val lauf = when (zustand) {
            BlasenZustand.SCHREIBT_MIT -> wippe(t * 5.2f)
            BlasenZustand.WERKZEUG -> wippe(t * 3.6f)
            BlasenZustand.STIMME -> wippe(t * 4.2f) / (balken - 1) * halb
            else -> wippe(t * 2.6f)
        }
        for (i in 0 until balken) {
            val lage = (i - halb) / halb
            val huelle = 1f - 0.55f * lage * lage
            // Jeder Balken mit eigener Phase: das Spektrum zittert nach dem Pegel, nicht alle gleich.
            val zittern = 0.6f + 0.4f * sin(t * (7f + i * 1.3f) + i * 2.1f)
            // Verarbeitung: ein wandernder Lichtpunkt statt eines Spektrums; jede Phase in ihrem eigenen Lauf.
            val licht = when (zustand) {
                BlasenZustand.SCHREIBT_MIT -> puls(i, lauf, 1.3f)
                BlasenZustand.DENKT, BlasenZustand.MIKROFON -> puls(i, lauf, 1.8f)
                BlasenZustand.WERKZEUG -> maxOf(puls(i, lauf, 1.2f), puls(i, balken - 1 - lauf, 1.2f))
                BlasenZustand.STIMME -> maxOf(puls(i, halb + lauf, 1.2f), puls(i, halb - lauf, 1.2f))
                else -> -1f
            }
            val ziel: Float
            val deckkraft: Int
            if (licht >= 0f) {
                ziel = 0.12f + 0.74f * licht
                deckkraft = 90 + (165 * licht).toInt()
            } else {
                deckkraft = when (zustand) { BlasenZustand.WARTET_AUF_ENDE -> 170; BlasenZustand.ANGEHALTEN -> 130; else -> 255 }
                ziel = when (zustand) {
                    // Zuhören: große, ruhige Welle; ohne Spracherkennung schwingt der Pegel mit.
                    BlasenZustand.HOERT -> maxOf(0.24f + 0.16f * sin(t * 3f - i * 0.6f), nebenbei * huelle * zittern)
                    // Frank spricht: das Spektrum folgt seiner Stimme und ist immer deutlich höher als beim Warten.
                    BlasenZustand.FRANK_SPRICHT -> maxOf(0.34f, mikro) * huelle * zittern * 1.15f
                    // Nach dem letzten Wort: nur noch ein langsames Atmen, gedimmt.
                    BlasenZustand.WARTET_AUF_ENDE -> maxOf(0.14f + 0.08f * sin(t * 1.8f), nebenbei * huelle * zittern)
                    // Jarvis spricht: vom Ton getriebene Balken, nach der Mitte hin am höchsten.
                    BlasenZustand.SPRICHT -> if (tonPegel > 0.02f) (tonPegel * 1.3f).coerceAtMost(1f) * huelle * zittern else 0.1f
                    else -> 0.14f
                }
            }
            val alt = werte[i]
            val tempo = if (ziel > alt) 22f else 9f
            werte[i] = alt + (ziel - alt) * minOf(1f, schritt * tempo)
            val hb = max(3.2f * dp, werte[i] * maxHoehe)
            val x = start + i * abstand
            strich.alpha = deckkraft
            canvas.drawLine(x, mitte - hb / 2 + strich.strokeWidth / 2, x, mitte + hb / 2 - strich.strokeWidth / 2, strich)
        }

        // Läuft weiter, solange die Blase hängt: Hörte die Schleife hier auf, weil Jarvis kurz als sichtbar galt, und blieb die Blase beim
        // Abgleich doch hängen, stünde sie danach für immer still. Weggenommen wird sie allein über [SprachBlase.abgleichen].
        if (!vm.sprachmodusAktiv || MainActivity.sichtbar) post { SprachBlase.abgleichen(context) }
        if (isAttachedToWindow) postInvalidateOnAnimation()
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
        const val HOEHE_DP = 44f
        // Immer leuchtendes, knalliges Orange (#FF6D00), in jeder Phase (Franks Wunsch).
        private const val FARBE = 0xFFFF6D00.toInt()
    }
}
