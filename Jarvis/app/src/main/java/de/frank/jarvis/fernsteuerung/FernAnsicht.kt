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

    private val gesten = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (zweiFinger) return false
            ereignis("klick", e.x, e.y)
            return true
        }
        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            // Zweiter Klick des Doppelklicks erst beim Loslassen, mit Klickzähler 2.
            if (e.actionMasked == MotionEvent.ACTION_UP && !ziehtMaus && !zweiFinger) ereignis("klick", e.x, e.y) { put("doppel", true) }
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            if (!zweiFinger && !ziehtMaus) ereignis("rechtsklick", e.x, e.y)
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
                zweiFinger = false; ziehtMaus = false
                startX = e.x; startY = e.y
                gesten.onTouchEvent(e)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ziehtMaus) { ereignis("hoch", e.getX(0), e.getY(0)); ziehtMaus = false }
                zweiFinger = true
                letzteMitteX = mitteX(e); letzteMitteY = mitteY(e)
                scrollRestX = 0f; scrollRestY = 0f
                // Den GestureDetector die Folge abbrechen lassen (kein Tippen, kein Rechtsklick)
                gesten.onTouchEvent(MotionEvent.obtain(e).apply { action = MotionEvent.ACTION_CANCEL })
            }
            MotionEvent.ACTION_MOVE -> {
                if (zweiFinger) {
                    if (e.pointerCount >= 2 && !skalierung.isInProgress || e.pointerCount >= 2 && abs(skalierung.scaleFactor - 1f) < 0.01f) {
                        val mx = mitteX(e); val my = mitteY(e)
                        val dx = mx - letzteMitteX; val dy = my - letzteMitteY
                        letzteMitteX = mx; letzteMitteY = my
                        if (zoom > 1.02f) { versatzX += dx; versatzY += dy; anordnen() }
                        else {
                            scrollRestX += dx; scrollRestY += dy
                            if (abs(scrollRestX) >= 2 || abs(scrollRestY) >= 2) {
                                senden(JSONObject().put("t", "scrollen").put("dx", (scrollRestX * 1.2f).toDouble()).put("dy", (scrollRestY * 1.2f).toDouble()))
                                scrollRestX = 0f; scrollRestY = 0f
                            }
                        }
                    } else { letzteMitteX = mitteX(e); letzteMitteY = mitteY(e) }
                } else if (ziehtMaus) {
                    ereignis("ziehen", e.x, e.y)
                } else if (hypot(e.x - startX, e.y - startY) > slop * 1.5f) {
                    ziehtMaus = true
                    gesten.onTouchEvent(MotionEvent.obtain(e).apply { action = MotionEvent.ACTION_CANCEL })
                    ereignis("runter", startX, startY)
                    ereignis("ziehen", e.x, e.y)
                } else gesten.onTouchEvent(e)
            }
            MotionEvent.ACTION_UP -> {
                if (ziehtMaus) { ereignis("hoch", e.x, e.y); ziehtMaus = false }
                else if (!zweiFinger) gesten.onTouchEvent(e)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (ziehtMaus) { ereignis("hoch", e.x, e.y); ziehtMaus = false }
                gesten.onTouchEvent(e)
            }
        }
        return true
    }

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
                repeat(maxOf(1, vor)) { taste("zurueck") }; return true
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
