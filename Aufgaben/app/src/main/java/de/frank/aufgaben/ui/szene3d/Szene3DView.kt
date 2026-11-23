package de.frank.aufgaben.ui.szene3d

import android.content.Context
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import kotlin.math.max

/**
 * Hardwarebeschleunigte OpenGL-ES-3.0-View auf Basis von [TextureView].
 * Vorteile in einer scrollbaren Compose-Liste:
 * - kein SurfaceView-Lochstanzen, volle Unterstützung für abgerundete Ecken, Alpha, Skalierung und Scroll-Parallaxe
 * - fängt keinerlei Touch-Gesten ab, damit Scrollen und Aufgaben-Ziehen völlig ungestört bleiben
 * - pausiert den Render-Thread bei Nicht-Sichtbarkeit auf 0 % CPU/GPU
 * - räumt alle GL- und EGL-Ressourcen sauber auf demselben Thread wieder auf
 */
internal class Szene3DView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private val lock = Object()
    private var renderThread: RenderThread? = null

    @Volatile private var palette: Palette3D = palette3DFuer(de.frank.aufgaben.ui.theme.Design.ORANGE, false)
    @Volatile private var pausiert: Boolean = false
    @Volatile private var bewegung: Boolean = true
    @Volatile private var phaseCallback: ((Int) -> Unit)? = null
    @Volatile private var cornerRadiusPx: Float = 0f

    init {
        isOpaque = true
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        surfaceTextureListener = this
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        clipToOutline = true
    }

    fun setCornerRadiusPx(px: Float) {
        if (cornerRadiusPx != px) {
            cornerRadiusPx = px
            invalidateOutline()
        }
    }

    fun setPalette(p: Palette3D) {
        if (palette != p) {
            palette = p
            requestOneFrame()
        }
    }

    fun setZustand(pausiert: Boolean, bewegung: Boolean) {
        val changed = this.pausiert != pausiert || this.bewegung != bewegung
        this.pausiert = pausiert
        this.bewegung = bewegung
        if (changed) {
            synchronized(lock) {
                renderThread?.dirty = true
                lock.notifyAll()
            }
        }
    }

    fun setOnPhaseChanged(cb: ((Int) -> Unit)?) {
        phaseCallback = cb
    }

    private fun requestOneFrame() {
        synchronized(lock) {
            renderThread?.dirty = true
            lock.notifyAll()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        stopThread()
        val t = RenderThread(surface, width, height)
        renderThread = t
        t.start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        synchronized(lock) {
            renderThread?.let {
                it.pendingWidth = max(1, width)
                it.pendingHeight = max(1, height)
                it.sizeChanged = true
                it.dirty = true
                lock.notifyAll()
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopThread()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onDetachedFromWindow() {
        stopThread()
        super.onDetachedFromWindow()
    }

    private fun stopThread() {
        val t: RenderThread?
        synchronized(lock) {
            t = renderThread
            renderThread = null
            if (t != null) {
                t.running = false
                lock.notifyAll()
            }
        }
        if (t != null && t !== Thread.currentThread()) {
            try {
                t.join(1200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private inner class RenderThread(
        private val surfaceTexture: SurfaceTexture,
        initialWidth: Int,
        initialHeight: Int,
    ) : Thread("Szene3D-GL") {

        @Volatile var running = true
        @Volatile var dirty = true
        @Volatile var sizeChanged = true
        @Volatile var pendingWidth = max(1, initialWidth)
        @Volatile var pendingHeight = max(1, initialHeight)

        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var renderer: Szene3DRenderer? = null

        override fun run() {
            if (!initEgl()) {
                releaseEgl()
                return
            }
            try {
                val r = Szene3DRenderer()
                renderer = r
                var simTime = 0f
                var lastNanos = System.nanoTime()
                var lastPhase = -1

                while (running) {
                    var isPaused = false
                    var isMoving = false
                    var needRender = false
                    var w = 0
                    var h = 0
                    var resized = false

                    synchronized(lock) {
                        while (running && (pausiert || (!bewegung && !dirty))) {
                            lastNanos = 0L
                            try {
                                lock.wait()
                            } catch (_: InterruptedException) {
                                return@synchronized
                            }
                        }
                        if (!running) return@synchronized
                        isPaused = pausiert
                        isMoving = bewegung
                        needRender = isMoving || dirty
                        dirty = false
                        if (sizeChanged) {
                            w = pendingWidth
                            h = pendingHeight
                            sizeChanged = false
                            resized = true
                        }
                    }

                    if (!running) break
                    if (isPaused) continue

                    if (resized) {
                        r.resize(w, h)
                    }

                    val now = System.nanoTime()
                    if (isMoving) {
                        if (lastNanos != 0L) {
                            val dt = ((now - lastNanos) / 1e9f).coerceIn(0f, 0.05f)
                            simTime = (simTime + dt) % SZENE_3D_ZYKLUS
                        }
                        lastNanos = now
                    } else {
                        lastNanos = 0L
                    }

                    val tRender = if (isMoving) simTime else SZENE_3D_STANDBILD
                    if (needRender) {
                        val frameStart = System.nanoTime()
                        r.drawFrame(tRender, palette)
                        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                            val err = EGL14.eglGetError()
                            if (err == EGL14.EGL_CONTEXT_LOST) {
                                break
                            }
                        }
                        val ph = phaseFuerZeit(tRender)
                        if (ph != lastPhase) {
                            lastPhase = ph
                            val cb = phaseCallback
                            if (cb != null) post { cb(ph) }
                        }
                        // Sanfte Begrenzung auf 60 FPS (spart Akku auf 120-Hz-Displays)
                        if (isMoving) {
                            val spentNs = System.nanoTime() - frameStart
                            val sleepMs = (16_500_000L - spentNs) / 1_000_000L
                            if (sleepMs > 1L) {
                                try {
                                    sleep(sleepMs)
                                } catch (_: InterruptedException) {
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Fehler im 3D-Render-Thread", t)
            } finally {
                try {
                    renderer?.release()
                } catch (_: Throwable) {
                }
                renderer = null
                releaseEgl()
            }
        }

        private fun initEgl(): Boolean {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL14.EGL_NO_DISPLAY) return false
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

            val config = chooseConfig(withMsaa = true) ?: chooseConfig(withMsaa = false) ?: return false
            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            if (eglContext === EGL14.EGL_NO_CONTEXT) return false

            val surfAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surfaceTexture, surfAttribs, 0)
            if (eglSurface === EGL14.EGL_NO_SURFACE) return false

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false
            EGL14.eglSwapInterval(eglDisplay, 1)
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthFunc(GLES30.GL_LEQUAL)
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glCullFace(GLES30.GL_BACK)
            return true
        }

        private fun chooseConfig(withMsaa: Boolean): EGLConfig? {
            val attribs = if (withMsaa) {
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT_KHR
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 24,
                    EGL14.EGL_SAMPLE_BUFFERS, 1,
                    EGL14.EGL_SAMPLES, 4,
                    EGL14.EGL_NONE,
                )
            } else {
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, 0x0040,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_NONE,
                )
            }
            val configs = arrayOfNulls<EGLConfig>(1)
            val num = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, num, 0) || num[0] <= 0) {
                return null
            }
            return configs[0]
        }

        private fun releaseEgl() {
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface !== EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }
    }

    companion object {
        private const val TAG = "Szene3DView"
    }
}
