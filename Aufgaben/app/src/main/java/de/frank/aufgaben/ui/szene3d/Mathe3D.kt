package de.frank.aufgaben.ui.szene3d

import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal const val PI_F = PI.toFloat()
internal const val TAU_F = (PI * 2.0).toFloat()

/** Leichtgewichtiger 3D-Vektor für Kameras, Licht und Kinematik. */
internal data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else Vec3(0f, 1f, 0f)
    }
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x,
    )
}

internal fun lerpVec3(a: Vec3, b: Vec3, t: Float): Vec3 {
    val c = t.coerceIn(0f, 1f)
    return Vec3(a.x + (b.x - a.x) * c, a.y + (b.y - a.y) * c, a.z + (b.z - a.z) * c)
}

internal fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

internal fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)

/** Linearer Fortschritt 0..1 ab [start] über [dauer] Sekunden. */
internal fun phase(t: Float, start: Float, dauer: Float): Float =
    if (dauer <= 0f) (if (t >= start) 1f else 0f) else ((t - start) / dauer).coerceIn(0f, 1f)

/** Kubisches Smoothstep (weiche Beschleunigung und Abbremsung). */
internal fun smooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** Quintisches Smootherstep (besonders filmisch für Kamera und Übergänge). */
internal fun smoother(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c * (c * (c * 6f - 15f) + 10f)
}

internal fun smoothPhase(t: Float, start: Float, dauer: Float): Float = smooth(phase(t, start, dauer))
internal fun smootherPhase(t: Float, start: Float, dauer: Float): Float = smoother(phase(t, start, dauer))

/** Konvertiert eine sRGB-Komponente (0..1) in lineares Licht für physikalisch korrektes PBR. */
internal fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

/** Wandelt eine Compose-Color (sRGB) in ein lineares RGB-Tripel [r, g, b] um. */
internal fun Color.toLinearRgb(): FloatArray = floatArrayOf(
    srgbToLinear(red),
    srgbToLinear(green),
    srgbToLinear(blue),
)

internal fun rgbLinear(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
    srgbToLinear(r.coerceIn(0f, 1f)),
    srgbToLinear(g.coerceIn(0f, 1f)),
    srgbToLinear(b.coerceIn(0f, 1f)),
)

internal fun hexLinear(hex: Int): FloatArray {
    val r = ((hex ushr 16) and 0xFF) / 255f
    val g = ((hex ushr 8) and 0xFF) / 255f
    val b = (hex and 0xFF) / 255f
    return rgbLinear(r, g, b)
}

internal fun lerpRgb(a: FloatArray, b: FloatArray, t: Float, out: FloatArray = FloatArray(3)): FloatArray {
    val c = t.coerceIn(0f, 1f)
    out[0] = a[0] + (b[0] - a[0]) * c
    out[1] = a[1] + (b[1] - a[1]) * c
    out[2] = a[2] + (b[2] - a[2]) * c
    return out
}

/**
 * Allokationsfreier Matrix-Stack für hierarchische 3D-Skelette (Mensch, Katze, Lampe, Stuhl).
 * Vermeidet GC-Pausen bei 60 FPS auf dem Handy.
 */
internal class MatrixStack(maxDepth: Int = 24) {
    private val stack = Array(maxDepth) { FloatArray(16) }
    private var top = 0

    init {
        Matrix.setIdentityM(stack[0], 0)
    }

    val current: FloatArray get() = stack[top]

    fun reset() {
        top = 0
        Matrix.setIdentityM(stack[0], 0)
    }

    fun push() {
        if (top + 1 >= stack.size) return
        System.arraycopy(stack[top], 0, stack[top + 1], 0, 16)
        top++
    }

    fun pop() {
        if (top > 0) top--
    }

    inline fun scoped(block: () -> Unit) {
        push()
        try {
            block()
        } finally {
            pop()
        }
    }

    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(stack[top], 0, x, y, z)
    }

    fun rotateX(deg: Float) {
        if (deg != 0f) Matrix.rotateM(stack[top], 0, deg, 1f, 0f, 0f)
    }

    fun rotateY(deg: Float) {
        if (deg != 0f) Matrix.rotateM(stack[top], 0, deg, 0f, 1f, 0f)
    }

    fun rotateZ(deg: Float) {
        if (deg != 0f) Matrix.rotateM(stack[top], 0, deg, 0f, 0f, 1f)
    }

    fun scale(x: Float, y: Float, z: Float) {
        Matrix.scaleM(stack[top], 0, x, y, z)
    }

    fun scale(s: Float) = scale(s, s, s)

    /** Transformiert einen lokalen Punkt mit der aktuellen Model-Matrix in Weltkoordinaten. */
    fun transformPoint(lx: Float, ly: Float, lz: Float): Vec3 {
        val m = stack[top]
        return Vec3(
            m[0] * lx + m[4] * ly + m[8] * lz + m[12],
            m[1] * lx + m[5] * ly + m[9] * lz + m[13],
            m[2] * lx + m[6] * ly + m[10] * lz + m[14],
        )
    }
}

/** Berechnet die 3×3 Normal-Matrix (Inverse-Transponierte der oberen 3×3 der Model-Matrix). */
internal fun computeNormalMatrix3(model: FloatArray, out9: FloatArray) {
    val a00 = model[0]; val a01 = model[1]; val a02 = model[2]
    val a10 = model[4]; val a11 = model[5]; val a12 = model[6]
    val a20 = model[8]; val a21 = model[9]; val a22 = model[10]

    val b01 = a22 * a11 - a12 * a21
    val b11 = -a22 * a10 + a12 * a20
    val b21 = a21 * a10 - a11 * a20

    val det = a00 * b01 + a01 * b11 + a02 * b21
    if (kotlin.math.abs(det) < 1e-8f) {
        out9[0] = 1f; out9[1] = 0f; out9[2] = 0f
        out9[3] = 0f; out9[4] = 1f; out9[5] = 0f
        out9[6] = 0f; out9[7] = 0f; out9[8] = 1f
        return
    }
    val invDet = 1f / det
    // Column-major 3x3 of (M^-1)^T
    out9[0] = b01 * invDet
    out9[1] = b11 * invDet
    out9[2] = b21 * invDet
    out9[3] = (-a22 * a01 + a02 * a21) * invDet
    out9[4] = (a22 * a00 - a02 * a20) * invDet
    out9[5] = (-a21 * a00 + a01 * a20) * invDet
    out9[6] = (a12 * a01 - a02 * a11) * invDet
    out9[7] = (-a12 * a00 + a02 * a10) * invDet
    out9[8] = (a11 * a00 - a01 * a10) * invDet
}

/** Hilfsfunktion: Winkel in Bogenmaß. */
internal fun degToRad(deg: Float): Float = deg * (PI_F / 180f)
internal fun sinDeg(deg: Float): Float = sin(degToRad(deg))
internal fun cosDeg(deg: Float): Float = cos(degToRad(deg))

/** Interpoliert zwei Winkel in Grad stets über den kürzesten Drehweg (kein 300°-Rückwärtsspin). */
internal fun lerpAngleDeg(a: Float, b: Float, t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    val diff = ((b - a + 540f) % 360f + 360f) % 360f - 180f
    return a + diff * c
}

/**
 * Vorberechneter kubischer 2D-Bezierpfad (XZ-Ebene) mit exakter Bogenlänge, Tangenten-Ausrichtung
 * und allokationsfreier Auswertung für rutschfreies, natürliches Gehen und Traben.
 */
internal class CubicPath2D(
    private val x0: Float, private val z0: Float,
    private val x1: Float, private val z1: Float,
    private val x2: Float, private val z2: Float,
    private val x3: Float, private val z3: Float,
    samples: Int = 32,
) {
    private val cumLen = FloatArray(samples + 1)
    val totalLength: Float

    init {
        var px = x0
        var pz = z0
        var acc = 0f
        cumLen[0] = 0f
        for (i in 1..samples) {
            val u = i.toFloat() / samples
            val cx = evalX(u)
            val cz = evalZ(u)
            val dx = cx - px
            val dz = cz - pz
            acc += sqrt(dx * dx + dz * dz)
            cumLen[i] = acc
            px = cx
            pz = cz
        }
        totalLength = acc
    }

    fun evalX(uRaw: Float): Float {
        val u = uRaw.coerceIn(0f, 1f)
        val omt = 1f - u
        return omt * omt * omt * x0 + 3f * omt * omt * u * x1 + 3f * omt * u * u * x2 + u * u * u * x3
    }

    fun evalZ(uRaw: Float): Float {
        val u = uRaw.coerceIn(0f, 1f)
        val omt = 1f - u
        return omt * omt * omt * z0 + 3f * omt * omt * u * z1 + 3f * omt * u * u * z2 + u * u * u * z3
    }

    /** Tangenten-Gierwinkel in Grad (OpenGL: lokales +X zeigt nach (cos yaw, -sin yaw)). */
    fun evalYawDeg(uRaw: Float): Float {
        val u = uRaw.coerceIn(0.001f, 0.999f)
        val omt = 1f - u
        val dx = 3f * omt * omt * (x1 - x0) + 6f * omt * u * (x2 - x1) + 3f * u * u * (x3 - x2)
        val dz = 3f * omt * omt * (z1 - z0) + 6f * omt * u * (z2 - z1) + 3f * u * u * (z3 - z2)
        return kotlin.math.atan2(-dz, dx) * (180f / PI_F)
    }

    /** Zurückgelegte Bogenlänge in Metern bis zum Kurvenparameter [uRaw] (für rutschfreie Schritte). */
    fun distanceAt(uRaw: Float): Float {
        val u = uRaw.coerceIn(0f, 1f)
        val n = cumLen.size - 1
        val scaled = u * n
        val idx = scaled.toInt().coerceIn(0, n - 1)
        val frac = scaled - idx
        return cumLen[idx] + (cumLen[idx + 1] - cumLen[idx]) * frac
    }

    /** Momentane Bogenlängen-Ableitung ds/du am Parameter [uRaw]. */
    fun speedAt(uRaw: Float): Float {
        val u = uRaw.coerceIn(0f, 1f)
        val omt = 1f - u
        val dx = 3f * omt * omt * (x1 - x0) + 6f * omt * u * (x2 - x1) + 3f * u * u * (x3 - x2)
        val dz = 3f * omt * omt * (z1 - z0) + 6f * omt * u * (z2 - z1) + 3f * u * u * (z3 - z2)
        return sqrt(dx * dx + dz * dz)
    }
}
