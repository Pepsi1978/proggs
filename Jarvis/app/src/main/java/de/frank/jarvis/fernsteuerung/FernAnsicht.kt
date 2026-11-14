package de.frank.jarvis.fernsteuerung

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.text.InputType
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.HapticFeedbackConstants
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Zeigt den Mac-Bildschirm und übersetzt Fingergesten wie auf einem Tablet:
 * Tippen = Klick, Doppeltippen = Doppelklick (öffnet Apps), lange drücken = Rechtsklick, ein Finger ziehen = Maus ziehen,
 * zwei Finger schieben = scrollen (vergrößert: Ausschnitt verschieben), zwei Finger spreizen = vergrößern.
 * Auf einem Terminalfenster des Macs: ein Finger wischen = scrollen (mit Schwung), lange drücken = Text markieren
 * (Maustaste bleibt gedrückt, weiterziehen markiert, Loslassen beendet); lange drücken ohne Ziehen = Rechtsklick.
 * Die Titel- und Tableiste eines Terminals zählt nicht dazu (dort zieht ein Finger weiter das Fenster).
 * Ist sie fokussiert, nimmt sie die Bildschirmtastatur des Handys entgegen und tippt auf dem Mac.
 */
@SuppressLint("ViewConstructor")
class FernAnsicht(context: Context, private val senden: (JSONObject) -> Unit) : FrameLayout(context) {
    val textur = TextureView(context)
    var surfaceBereit: ((Surface?) -> Unit)? = null
    private var macBreite = 0
    private var macHoehe = 0
    var geheim = false
        set(v) { if (field != v) { field = v; imm.restartInput(this) } }

    // Zoom/Verschiebung des Bildes (zusätzlich zur Einpassung)
    private var zoom = 1f
    private var versatzX = 0f
    private var versatzY = 0f
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    init {
        addView(textur, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isFocusable = true
        isFocusableInTouchMode = true
        textur.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { surfaceBereit?.invoke(Surface(st)); anordnen() }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = anordnen()
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { surfaceBereit?.invoke(null); return true }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    fun macGroesse(w: Int, h: Int) { macBreite = w; macHoehe = h; post { anordnen() } }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); anordnen() }

    /** Rechteck, in dem das Mac-Bild auf der Ansicht liegt (eingepasst, dann vergrößert und verschoben). */
    private fun bildRahmen(): RectF {
        val vw = width.toFloat(); val vh = height.toFloat()
        if (macBreite == 0 || vw == 0f) return RectF(0f, 0f, vw, vh)
        val s = minOf(vw / macBreite, vh / macHoehe)
        val bw = macBreite * s * zoom; val bh = macHoehe * s * zoom
        // Verschiebung so begrenzen, dass das Bild die Ansicht nicht verlässt
        val maxX = maxOf(0f, (bw - vw) / 2); val maxY = maxOf(0f, (bh - vh) / 2)
        versatzX = versatzX.coerceIn(-maxX, maxX); versatzY = versatzY.coerceIn(-maxY, maxY)
        val l = (vw - bw) / 2 + versatzX; val t = (vh - bh) / 2 + versatzY
        return RectF(l, t, l + bw, t + bh)
    }

    private fun anordnen() {
        val r = bildRahmen()
        val m = Matrix()
        // TextureView füllt die ganze Ansicht; die Matrix bildet sie auf den Bildrahmen ab.
        m.setRectToRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), r, Matrix.ScaleToFit.FILL)
        textur.setTransform(m)
        textur.invalidate()
    }

    private fun macPunkt(x: Float, y: Float): Pair<Double, Double> {
        val r = bildRahmen()
        return ((x - r.left) / r.width()).toDouble().coerceIn(0.0, 1.0) to ((y - r.top) / r.height()).toDouble().coerceIn(0.0, 1.0)
    }

    private fun imBild(x: Float, y: Float) = bildRahmen().contains(x, y)

    /** Sichtbare Mac-Fenster von vorn nach hinten: [x, y, b, h, terminal] bezogen auf den Bildschirm (vom Mac im Zustand). */
    private var fenster: List<DoubleArray> = emptyList()

    fun fensterSetzen(a: org.json.JSONArray?) {
        fenster = if (a == null) emptyList() else (0 until a.length()).mapNotNull { i ->
            a.optJSONArray(i)?.takeIf { it.length() >= 5 }?.let { f -> DoubleArray(5) { f.optDouble(it) } }
        }
    }

    /** Liegt der Punkt auf einem Terminalfenster (das vorderste Fenster unter dem Finger zählt)? */
    private fun aufTerminal(x: Float, y: Float): Boolean {
        if (!imBild(x, y)) return false
        val (mx, my) = macPunkt(x, y)
        val f = fenster.firstOrNull { mx >= it[0] && mx <= it[0] + it[2] && my >= it[1] && my <= it[1] + it[3] } ?: return false
        return f[4] >= 0.5
    }

    private fun ereignis(t: String, x: Float, y: Float, extra: JSONObject.() -> Unit = {}) {
        val (mx, my) = macPunkt(x, y)
        senden(JSONObject().put("t", t).put("x", mx).put("y", my).apply(extra))
    }

    // --- Gesten ---

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var ziehtMaus = false
    private var zweiFinger = false
    private var startX = 0f
    private var startY = 0f
    private var letzteMitteX = 0f
    private var letzteMitteY = 0f
    private var scrollRestX = 0f
    private var scrollRestY = 0f
    // Terminal-Modus: Wischen scrollt, langes Drücken markiert
    private var terminal = false
    private var wischScroll = false
    private var markiert = false
    private var letzteX = 0f
    private var letzteY = 0f
    private var tempo: VelocityTracker? = null
    private var schwungY = 0f
    private var schwungX = 0f
    private var schwungLetzt = 0L
    private var markiertBewegt = false
    private var langX = 0f
    private var langY = 0f
    /** Ein Tippen, das einen laufenden Schwung anhält, klickt nicht am Mac. */
    private var tippenStopptSchwung = false

    private val gesten = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Tippen auf die schwarzen Ränder neben dem Bild löst am Mac nichts aus.
            if (zweiFinger || tippenStopptSchwung || !imBild(e.x, e.y)) return false
            eingabeNeu()
            ereignis("klick", e.x, e.y)
            return true
        }
        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            // Zweiter Klick des Doppelklicks erst beim Loslassen, mit Klickzähler 2.
            if (e.actionMasked == MotionEvent.ACTION_UP && !ziehtMaus && !zweiFinger && imBild(e.x, e.y)) ereignis("klick", e.x, e.y) { put("doppel", true) }
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            if (zweiFinger || ziehtMaus || wischScroll || !imBild(e.x, e.y)) return
            if (terminal) {
                // Markieren: Maustaste drücken und halten, weiterziehen zieht die Markierung auf.
                markiert = true; markiertBewegt = false; langX = e.x; langY = e.y
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                ereignis("runter", e.x, e.y)
                return
            }
            ereignis("rechtsklick", e.x, e.y)
        }
    })

    private val skalierung = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val alt = zoom
            zoom = (zoom * d.scaleFactor).coerceIn(1f, 5f)
            // Um den Mittelpunkt der Finger vergrößern
            val cx = d.focusX - width / 2f; val cy = d.focusY - height / 2f
            val f = zoom / alt
            versatzX = cx - (cx - versatzX) * f; versatzY = cy - (cy - versatzY) * f
            anordnen()
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) requestFocus()
        skalierung.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                zweiFinger = false; ziehtMaus = false; wischScroll = false; markiert = false
                tippenStopptSchwung = schwungLaeuft()
                schwungStopp()
                startX = e.x; startY = e.y; letzteX = e.x; letzteY = e.y
                terminal = aufTerminal(e.x, e.y)
                tempo?.recycle(); tempo = VelocityTracker.obtain().also { it.addMovement(e) }
                gesten.onTouchEvent(e)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ziehtMaus || markiert) { ereignis("hoch", e.getX(0), e.getY(0)); ziehtMaus = false; markiert = false }
                wischScroll = false
                zweiFinger = true
                letzteMitteX = mitteX(e); letzteMitteY = mitteY(e)
                scrollRestX = 0f; scrollRestY = 0f
                // Den GestureDetector die Folge abbrechen lassen (kein Tippen, kein Rechtsklick)
                abbrechen(e)
            }
            MotionEvent.ACTION_MOVE -> {
                tempo?.addMovement(e)
                if (zweiFinger) {
                    if (e.pointerCount >= 2 && !skalierung.isInProgress || e.pointerCount >= 2 && abs(skalierung.scaleFactor - 1f) < 0.01f) {
                        val mx = mitteX(e); val my = mitteY(e)
                        val dx = mx - letzteMitteX; val dy = my - letzteMitteY
                        letzteMitteX = mx; letzteMitteY = my
                        if (zoom > 1.02f) { versatzX += dx; versatzY += dy; anordnen() }
                        else {
                            scrollSenden(dx * 1.2f, dy * 1.2f, nurSenkrecht = false)
                        }
                    } else { letzteMitteX = mitteX(e); letzteMitteY = mitteY(e) }
                } else if (ziehtMaus || markiert) {
                    if (markiert && hypot(e.x - langX, e.y - langY) > slop) markiertBewegt = true
                    ereignis("ziehen", e.x, e.y)
                } else if (wischScroll) {
                    wischen(e.x - letzteX, e.y - letzteY)
                    letzteX = e.x; letzteY = e.y
                } else if (terminal && hypot(e.x - startX, e.y - startY) > slop) {
                    // Terminal: ein Finger wischen scrollt, natürliche Richtung (Inhalt folgt dem Finger).
                    wischScroll = true
                    abbrechen(e)
                    scrollRestX = 0f; scrollRestY = 0f
                    ereignis("bewegen", startX, startY)
                    wischen(e.x - startX, e.y - startY)
                    letzteX = e.x; letzteY = e.y
                } else if (hypot(e.x - startX, e.y - startY) > slop * 1.5f && imBild(startX, startY)) {
                    ziehtMaus = true
                    abbrechen(e)
                    ereignis("runter", startX, startY)
                    ereignis("ziehen", e.x, e.y)
                } else gesten.onTouchEvent(e)
            }
            MotionEvent.ACTION_UP -> {
                if (ziehtMaus || markiert) {
                    ereignis("hoch", e.x, e.y)
                    // Lange gedrückt, aber nicht gezogen: Kontextmenü des Terminals (Kopieren, Einsetzen …).
                    if (markiert && !markiertBewegt) ereignis("rechtsklick", langX, langY)
                    ziehtMaus = false; markiert = false; abbrechen(e)
                }
                else if (wischScroll) {
                    wischScroll = false
                    tempo?.let { it.addMovement(e); it.computeCurrentVelocity(1000); schwungStart(it.xVelocity, it.yVelocity) }
                }
                else if (!zweiFinger) gesten.onTouchEvent(e)
                tempo?.recycle(); tempo = null
            }
            MotionEvent.ACTION_CANCEL -> {
                if (ziehtMaus || markiert) { ereignis("hoch", e.x, e.y); ziehtMaus = false; markiert = false }
                wischScroll = false
                tempo?.recycle(); tempo = null
                gesten.onTouchEvent(e)
            }
        }
        return true
    }

    /** Fingerweg in Bildpunkten als Scrollen an den Mac (gleiche Richtung und Stärke wie beim Zwei-Finger-Scrollen). */
    private fun wischen(dx: Float, dy: Float) {
        // Vergrößert ist ein Bildpunkt auf dem Handy weniger Weg auf dem Mac.
        val f = 1.2f / zoom
        scrollSenden(dx * f, dy * f, nurSenkrecht = true)
    }

    /**
     * Sammelt Scrollweg und schickt ganze Bildpunkte (der Mac rechnet in ganzen Zahlen); der Rest bleibt für das nächste Mal,
     * damit langsames Wischen und der auslaufende Schwung keinen Weg verlieren.
     */
    private fun scrollSenden(dx: Float, dy: Float, nurSenkrecht: Boolean) {
        scrollRestX += dx; scrollRestY += dy
        if (abs(scrollRestX) < 2 && abs(scrollRestY) < 2) return
        // Terminal: waagerechtes Zittern weglassen, damit der Inhalt nicht seitlich wackelt.
        if (nurSenkrecht && abs(scrollRestX) <= abs(scrollRestY) * 2) scrollRestX = 0f
        val sx = scrollRestX.toInt(); val sy = scrollRestY.toInt()
        scrollRestX -= sx; scrollRestY -= sy
        senden(JSONObject().put("t", "scrollen").put("dx", sx).put("dy", sy))
    }

    /** Dem GestureDetector die laufende Folge abbrechen (kein Tippen, kein langes Drücken). */
    private fun abbrechen(e: MotionEvent) {
        val c = MotionEvent.obtain(e).apply { action = MotionEvent.ACTION_CANCEL }
        gesten.onTouchEvent(c); c.recycle()
    }

    // Schwung nach dem Loslassen: Geschwindigkeit klingt pro Bild ab.
    private val schwungSchritt = object : Runnable {
        override fun run() {
            val jetzt = System.nanoTime()
            val dt = ((jetzt - schwungLetzt) / 1e9f).coerceIn(0.001f, 0.05f)
            schwungLetzt = jetzt
            wischen(schwungX * dt, schwungY * dt)
            val abfall = Math.pow(0.04, dt.toDouble()).toFloat() // nach 1 s noch 4 %
            schwungX *= abfall; schwungY *= abfall
            if (abs(schwungY) > 40f || abs(schwungX) > 40f) postOnAnimation(this) else schwungStopp()
        }
    }

    private fun schwungStart(vx: Float, vy: Float) {
        if (abs(vy) < 300f && abs(vx) < 300f) return
        schwungX = vx.coerceIn(-12000f, 12000f); schwungY = vy.coerceIn(-12000f, 12000f)
        schwungLetzt = System.nanoTime()
        postOnAnimation(schwungSchritt)
    }

    private fun schwungLaeuft() = abs(schwungX) > 0f || abs(schwungY) > 0f

    override fun onDetachedFromWindow() { schwungStopp(); super.onDetachedFromWindow() }

    fun schwungAus() = schwungStopp()

    private fun schwungStopp() { removeCallbacks(schwungSchritt); schwungX = 0f; schwungY = 0f }

    private fun mitteX(e: MotionEvent) = (0 until minOf(2, e.pointerCount)).map { e.getX(it) }.average().toFloat()
    private fun mitteY(e: MotionEvent) = (0 until minOf(2, e.pointerCount)).map { e.getY(it) }.average().toFloat()

    // --- Tastatur ---

    fun tastaturZeigen(an: Boolean) {
        if (an) {
            requestFocus()
            imm.restartInput(this)
            // Ab Android 11 zuverlässiger über die Fenster-Insets; showSoftInput zusätzlich für ältere Geräte.
            if (android.os.Build.VERSION.SDK_INT >= 30) windowInsetsController?.show(android.view.WindowInsets.Type.ime())
            imm.showSoftInput(this, 0)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= 30) windowInsetsController?.hide(android.view.WindowInsets.Type.ime())
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    /** Neue Eingabesitzung ohne alten Entwurf, etwa nach einem Feldwechsel; öffnet die Tastatur nicht. */
    fun eingabeNeu() = imm.restartInput(this)

    private fun taste(name: String) = senden(JSONObject().put("t", "taste").put("k", name))
    private fun text(s: String) { if (s.isNotEmpty()) senden(JSONObject().put("t", "text").put("s", s)) }

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(out: EditorInfo): InputConnection {
        out.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            if (geheim) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        out.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        return object : BaseInputConnection(this, false) {
            private var entwurf = ""

            override fun commitText(t: CharSequence, pos: Int): Boolean {
                ersetzeEntwurf(t.toString()); entwurf = ""; return true
            }
            override fun setComposingText(t: CharSequence, pos: Int): Boolean { ersetzeEntwurf(t.toString()); return true }
            override fun finishComposingText(): Boolean { entwurf = ""; return true }
            override fun deleteSurroundingText(vor: Int, nach: Int): Boolean {
                // Gelöschtes aus dem Entwurf nehmen, damit der nächste Entwurf richtig verglichen wird.
                entwurf = entwurf.dropLast(minOf(vor, entwurf.length))
                repeat(vor) { taste("zurueck") }
                repeat(nach) { taste("entf") }
                if (vor == 0 && nach == 0) taste("zurueck")
                return true
            }
            override fun sendKeyEvent(e: KeyEvent): Boolean {
                if (e.action != KeyEvent.ACTION_DOWN) return true
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DEL -> taste("zurueck")
                    KeyEvent.KEYCODE_FORWARD_DEL -> taste("entf")
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> taste("enter")
                    KeyEvent.KEYCODE_TAB -> taste("tab")
                    KeyEvent.KEYCODE_DPAD_LEFT -> taste("links")
                    KeyEvent.KEYCODE_DPAD_RIGHT -> taste("rechts")
                    KeyEvent.KEYCODE_DPAD_UP -> taste("hoch")
                    KeyEvent.KEYCODE_DPAD_DOWN -> taste("runter")
                    else -> e.unicodeChar.takeIf { it != 0 }?.let { text(String(Character.toChars(it))) }
                }
                return true
            }
            override fun performEditorAction(aktion: Int): Boolean { taste("enter"); return true }

            /** Tastaturen mit Wortvorschlag schicken Entwürfe: nur die Änderung gegenüber dem letzten Entwurf tippen. */
            private fun ersetzeEntwurf(neu: String) {
                val gleich = entwurf.commonPrefixWith(neu).length
                repeat(entwurf.length - gleich) { taste("zurueck") }
                text(neu.substring(gleich))
                entwurf = neu
            }
        }
    }
}
