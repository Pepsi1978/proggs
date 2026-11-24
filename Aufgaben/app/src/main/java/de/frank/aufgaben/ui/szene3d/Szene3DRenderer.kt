package de.frank.aufgaben.ui.szene3d

import android.opengl.GLES30
import android.opengl.Matrix
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Gesamtlänge eines vollständigen 3D-Durchlaufs (Tag -> Abend -> Nacht -> Sonnenaufgang -> Rückkehr + Ausklang). */
internal const val SZENE_3D_ZYKLUS = 66.0f

/** Nahtloser Wiedereinstieg nach dem langen Morgen-Ausklang (identische Haltung, Lichtstimmung und Kamera). */
internal const val SZENE_3D_LOOP_START = 8.0f

/** Standbild-Zeitpunkt für reduzierte Bewegung (heller Tag, Arbeiten am PC, Dampf, Katze im Körbchen links oben). */
internal const val SZENE_3D_STANDBILD = 14.2f

/** Phasen-Bezeichnungen unterhalb der 3D-Szene, synchron zur Tages- und Nacht-Choreografie. */
internal val SZENE_3D_PHASEN: List<Pair<Float, String>> = listOf(
    0.0f to "Tag",
    18.0f to "Aufgaben",
    24.2f to "Katzenpause",
    39.2f to "Nacht",
    47.2f to "Morgen",
)

internal fun phaseFuerZeit(t: Float): Int =
    SZENE_3D_PHASEN.indexOfLast { t >= it.first }.coerceAtLeast(0)

/**
 * Vollständiger OpenGL-ES-3.0-Renderer für das animierte 3D-Arbeitszimmer:
 * - Pass 1: 1024×1024 Depth-Shadow-Map vom Haupt-Sonnen-/Mondlicht
 * - Pass 2: PBR-Hauptszene (Raum, schwingende Tür, vertieftes Fenster mit Tag/Abend/Nacht/Sonnenaufgang,
 *           Katzenkörbchen links oben, unterschiedliche Wandbilder, Möbel, PC-Arbeitsplatz,
 *           natürlich artikulierter Mensch mit sauberer Stuhl-Umgehung, animierte Katze)
 * - Pass 3: Transparente Atmosphäre (Sonnen-, Mond- & Morgenstrahlen, Lampenkegel, Kaffeedampf, Staubkörner)
 */
internal class Szene3DRenderer {

    private val shaders = ShaderPrograms()
    private val meshes = MeshLibrary()
    private val ms = MatrixStack(32)

    private var shadowFbo = 0
    private var shadowTex = 0

    private var viewW = 1
    private var viewH = 1

    private val projM = FloatArray(16)
    private val viewM = FloatArray(16)
    private val viewProjM = FloatArray(16)
    private val lightProjM = FloatArray(16)
    private val lightViewM = FloatArray(16)
    private val lightViewProjM = FloatArray(16)
    private val normalM = FloatArray(9)

    // Wiederverwendete Farbpuffer (null GC pro Frame)
    private val tmpCol1 = FloatArray(3)
    private val tmpCol2 = FloatArray(3)
    private val tmpCol3 = FloatArray(3)
    private val tmpEmissive = FloatArray(3)
    private val zeroRgb = floatArrayOf(0f, 0f, 0f)
    private val whiteRgb = hexLinear(0xF6F3EE)
    private val offWhiteRgb = hexLinear(0xECE6DC)
    private val darkMetalRgb = hexLinear(0x23262C)
    private val brushedMetalRgb = hexLinear(0xB8BDC7)
    private val brassRgb = hexLinear(0xD2A95E)
    private val coffeeRgb = hexLinear(0x3B2314)
    private val soilRgb = hexLinear(0x3A2B20)
    private val eyeDarkRgb = hexLinear(0x161414)
    private val scleraRgb = hexLinear(0xF4F2EE)
    private val mouthRgb = hexLinear(0xB97D6A)
    private val pinkNoseRgb = hexLinear(0xE89598)
    private val soleWhiteRgb = hexLinear(0xE6E2DC)
    private val mousePadRgb = hexLinear(0x282B33)
    private val keycapRgb = hexLinear(0x363A44)

    // Konstante Lichtfarben (vermeidet Allokationen in drawFrame)
    private val sunDayRgb = floatArrayOf(1.0f, 0.94f, 0.82f)
    private val sunEveRgb = floatArrayOf(1.0f, 0.46f, 0.17f)
    private val moonNightRgb = floatArrayOf(0.36f, 0.50f, 0.92f)
    private val sunDawnRgb = floatArrayOf(1.0f, 0.58f, 0.28f)
    private val skyDayRgb = floatArrayOf(0.88f, 0.92f, 1.0f)
    private val skyEveRgb = floatArrayOf(0.42f, 0.40f, 0.68f)
    private val skyNightRgb = floatArrayOf(0.18f, 0.26f, 0.56f)
    private val skyDawnRgb = floatArrayOf(0.92f, 0.62f, 0.56f)
    private val gndDayRgb = floatArrayOf(0.76f, 0.59f, 0.44f)
    private val gndNightRgb = floatArrayOf(0.22f, 0.26f, 0.40f)

    private val bookColors = arrayOf(
        hexLinear(0xD65A3A),
        hexLinear(0x3B6EA5),
        hexLinear(0xE2A83B),
        hexLinear(0x4C8C64),
        hexLinear(0x8A569E),
        hexLinear(0xC9764E),
        hexLinear(0x46798A),
        hexLinear(0xD4B895),
    )

    // Vorberechnete glatte Bezier-Laufpfade:
    // 1) Mensch geht zuerst gerade mittig durch die Türöffnung (z = -0.36, voller Abstand zu Wand & Zarge),
    //    biegt erst IM Raum in den weiten Vordergrundbogen ein (Abstand zum Stuhl >= 0.63 m)!
    private val humanEnterPath = CubicPath2D(
        -2.65f, -0.36f,
        -1.35f, -0.36f,
        -0.05f, +0.96f,
        CHAIR_ENTRY_X, CHAIR_ENTRY_Z,
    )
    private val humanExitPath = CubicPath2D(
        CHAIR_ENTRY_X, CHAIR_ENTRY_Z,
        -0.05f, +0.96f,
        -1.35f, -0.36f,
        -2.65f, -0.36f,
    )

    // Katzenpfade: 1) Tür -> Absprungpunkt vor Körbchen links oben, 2) Landepunkt vor Körbchen -> Mensch/Schreibtisch, 3) Rückweg zur Tür
    private val catToBasketApproachPath = CubicPath2D(
        -2.62f, -0.30f,
        -1.45f, -0.04f,
        -0.96f, -0.34f,
        CAT_BASKET_OUT_X, CAT_BASKET_OUT_Z,
    )
    private val catBasketOutToHumanPath = CubicPath2D(
        CAT_BASKET_OUT_X, CAT_BASKET_OUT_Z,
        -0.36f, -0.22f,
        +0.02f, +0.36f,
        CAT_LAUNCH_X, CAT_LAUNCH_Z,
    )
    private val catDeskToWaitPath = CubicPath2D(
        0.56f, +0.46f,
        -0.04f, +0.52f,
        -0.78f, +0.30f,
        -1.10f, +0.08f,
    )
    private val catWaitToExitPath = CubicPath2D(
        -1.10f, +0.08f,
        -1.62f, -0.10f,
        -2.16f, -0.24f,
        -2.70f, -0.32f,
    )

    // Dynamische Licht- und Objektpositionen pro Frame
    private var lampBulbWorld = Vec3(0.95f, 1.18f, -0.78f)
    private var lampAimWorld = Vec3(0.85f, 0.76f, -0.32f)
    private var monitorWorld = Vec3(1.14f, 1.02f, -0.48f)
    private var mugRestWorld = Vec3(0.76f, 0.763f, -0.64f)
    private var rightHandWorld = Vec3(0.65f, 0.85f, -0.25f)

    // Frame-Status für Schatten- und Hauptpass
    private var shadowPass = false
    private var curStoryTime = 0f
    private var curVisualTime = 0f
    private var curDaylight = 1f
    private var curEvening = 0f
    private var curNight = 0f
    private var curDawn = 0f
    private var curSunY = 0.76f
    private var curLampeAn = 0f
    private var curScreenOn = 0f
    private var curTyped = 0f
    private var curChecked = 0f
    private var curDoorAngle = 0f
    private var curChairX = CHAIR_OPEN_X
    private var curChairZ = CHAIR_OPEN_Z
    private var curChairSwivel = -86f
    private var curChairDrop = 0f
    private var curMugInHand = 0f
    private var curMugTilt = 0f

    // Menschen- und Katzen-Weltposition für Kontaktschatten
    private var humanContactX = -2.4f
    private var humanContactZ = -0.24f
    private var humanContactStr = 0f
    private var catContactX = -2.6f
    private var catContactZ = -0.32f
    private var catContactStr = 0f

    init {
        initShadowMap()
    }

    private fun initShadowMap() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)
        shadowFbo = fbos[0]
        shadowTex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            SHADOW_SIZE, SHADOW_SIZE, 0,
            GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null,
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_COMPARE_REF_TO_TEXTURE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_FUNC, GLES30.GL_LEQUAL)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_TEXTURE_2D, shadowTex, 0,
        )
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun resize(w: Int, h: Int) {
        viewW = max(1, w)
        viewH = max(1, h)
        val aspect = viewW.toFloat() / viewH.toFloat()
        Matrix.perspectiveM(projM, 0, 35.0f, aspect, 0.15f, 18f)
    }

    fun release() {
        meshes.release()
        shaders.release()
        if (shadowFbo != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(shadowFbo), 0)
            shadowFbo = 0
        }
        if (shadowTex != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(shadowTex), 0)
            shadowTex = 0
        }
    }

    fun drawFrame(t: Float, visualTime: Float, p: Palette3D) {
        curStoryTime = t
        curVisualTime = visualTime

        // ---- 1. Tageszeit-Phasen: Tag -> langsamer Abend -> eigenständige dunkle Nacht -> Sonnenaufgang -> Morgen ----
        val eveIn = smootherPhase(t, 18.0f, 11.5f)
        val nightIn = smootherPhase(t, 33.2f, 5.2f)
        val nightOut = smootherPhase(t, 47.2f, 3.6f)
        val dawnIn = smootherPhase(t, 47.0f, 3.4f)
        val dayBack = smootherPhase(t, 49.6f, 5.4f)

        curNight = nightIn * (1f - nightOut)
        curEvening = eveIn * (1f - nightIn)
        curDawn = dawnIn * (1f - dayBack)
        curDaylight = clamp01((1f - smootherPhase(t, 18.0f, 15.8f)) + dayBack)

        // Sonnenhöhe im Fenster: hoch am Tag -> sinkt allmählich ab -> unter dem Horizont nachts -> steigt sichtbar auf!
        curSunY = when {
            t < 18.0f -> 0.76f
            t < 35.0f -> mix(0.76f, -0.10f, smootherPhase(t, 18.0f, 16.5f))
            t < 48.0f -> -0.10f
            t < 55.2f -> mix(-0.04f, 0.76f, smootherPhase(t, 48.0f, 7.0f))
            else -> 0.76f
        }

        // Tür schwingt beim Ankommen (0.2..6.5s), beim Feierabend (35.4..40.8s, erst nach vollem Hinausgehen!) und bei Rückkehr (53.8..60.0s)
        val doorEnter1 = smootherPhase(t, 0.20f, 1.25f) * (1f - smootherPhase(t, 5.0f, 1.45f))
        val doorExit = smootherPhase(t, 35.4f, 1.20f) * (1f - smootherPhase(t, 39.60f, 1.20f))
        val doorEnter2 = smootherPhase(t, 53.8f, 1.25f) * (1f - smootherPhase(t, 58.6f, 1.45f))
        curDoorAngle = max(max(doorEnter1, doorExit), doorEnter2) * 74f

        // Drehstuhl: steht offen zum Raum (-86°), schwenkt & rollt exakt nach dem Hinsetzen mit dem Menschen an den Tisch
        val dock1 = smootherPhase(t, 6.45f, 0.90f)
        val undock = smootherPhase(t, 34.10f, 0.85f)
        val dock2 = smootherPhase(t, 60.15f, 0.90f)
        val chairDocked = clamp01(dock1 * (1f - undock) + dock2)

        val petSwivel = smootherPhase(t, 28.0f, 0.85f) * (1f - smootherPhase(t, 31.3f, 0.85f))
        curChairX = mix(CHAIR_OPEN_X, CHAIR_WORLD_X, chairDocked)
        curChairZ = mix(CHAIR_OPEN_Z, CHAIR_WORLD_Z, chairDocked)
        curChairSwivel = mix(-86f, 0f, chairDocked) - petSwivel * 14f + sin(visualTime * 0.85f) * 1.1f * chairDocked

        val seatedNow = clamp01(
            smootherPhase(t, 5.65f, 0.80f) * (1f - smootherPhase(t, 34.95f, 0.70f)) +
                smootherPhase(t, 59.35f, 0.80f),
        )
        val sitBounce1 = sin(phase(t, 6.15f, 0.75f) * PI_F) * 0.018f
        val sitBounce2 = sin(phase(t, 59.85f, 0.75f) * PI_F) * 0.018f
        curChairDrop = seatedNow * 0.015f + sitBounce1 + sitBounce2

        // Monitor & Aufgabenliste
        val screen1 = smoothPhase(t, 7.25f, 0.55f) * (1f - smoothPhase(t, 33.85f, 0.45f))
        val screen2 = smoothPhase(t, 60.75f, 0.55f)
        curScreenOn = clamp01(screen1 + screen2)

        curTyped = when {
            t < 8.4f -> 0f
            t < 22.0f -> ((t - 8.4f) / 3.2f).coerceIn(0f, 4f)
            t < 34.0f -> 4f
            else -> 0f
        }
        curChecked = when {
            t < 11.4f -> 0f
            t < 22.0f -> ((t - 11.4f) / 4.2f).coerceIn(0f, 2f)
            t < 31.8f -> 2f
            t < 33.2f -> 2f + ((t - 31.8f) / 0.65f).coerceIn(0f, 2f)
            t < 34.0f -> 4f
            else -> 0f
        }

        // Schreibtischlampe: bei beginnendem Abend (22.8s) eingeschaltet, bei Feierabend (33.7s) ausgeschaltet
        curLampeAn = smoothPhase(t, 22.85f, 0.40f) * (1f - smoothPhase(t, 33.65f, 0.40f))

        // Kaffeetasse in der Hand während der Kaffeepause am Tag (13.3..15.1s)
        curMugInHand = if (t in 13.25f..15.15f) 1f else 0f
        curMugTilt = sin(phase(t, 13.65f, 1.05f) * PI_F) * 28f

        // ---- 2. Filmische 3D-Kamera mit sanften Übergängen ----
        val camWork = smootherPhase(t, 1.4f, 6.2f) * (1f - smootherPhase(t, 34.2f, 4.2f))
        val camCat = smootherPhase(t, 26.8f, 1.8f) * (1f - smootherPhase(t, 32.0f, 2.0f))
        val camNight = smootherPhase(t, 36.8f, 3.2f) * (1f - smootherPhase(t, 52.8f, 3.0f))
        val camReturnWork = smootherPhase(t, 55.2f, 5.6f)
        val camEnterWeight = clamp01(1f - max(max(camWork, camNight), camReturnWork))

        val enterEye = Vec3(-0.54f, 1.84f, 3.08f)
        val workEye = Vec3(-0.36f, 1.66f, 2.66f)
        val catEye = Vec3(-0.16f, 1.56f, 2.38f)
        val nightEye = Vec3(-0.42f, 1.68f, 2.74f)

        val enterTarget = Vec3(-0.06f, 0.82f, -0.22f)
        val workTarget = Vec3(0.18f, 0.86f, -0.24f)
        val catTarget = Vec3(0.44f, 0.86f, -0.14f)
        val nightTarget = Vec3(0.32f, 1.06f, -0.52f)

        var camEye = lerpVec3(workEye, enterEye, camEnterWeight)
        camEye = lerpVec3(camEye, catEye, camCat)
        camEye = lerpVec3(camEye, nightEye, camNight)
        camEye = lerpVec3(camEye, workEye, camReturnWork)

        var camTarget = lerpVec3(workTarget, enterTarget, camEnterWeight)
        camTarget = lerpVec3(camTarget, catTarget, camCat)
        camTarget = lerpVec3(camTarget, nightTarget, camNight)
        camTarget = lerpVec3(camTarget, workTarget, camReturnWork)

        val breatheX = sin(visualTime * 0.24f) * 0.045f
        val breatheY = cos(visualTime * 0.20f) * 0.020f
        camEye = camEye + Vec3(breatheX, breatheY, 0f)

        Matrix.setLookAtM(
            viewM, 0,
            camEye.x, camEye.y, camEye.z,
            camTarget.x, camTarget.y, camTarget.z,
            0f, 1f, 0f,
        )
        Matrix.multiplyMM(viewProjM, 0, projM, 0, viewM, 0)

        // ---- 3. Sonnen- bzw. Mondrichtung & Schattenkamera ----
        val sunPitchY = mix(0.46f, 0.24f, clamp01(curEvening + curDawn * 0.8f))
        val sunDir = Vec3(
            mix(0.32f, 0.48f, curEvening) - curDawn * 0.10f,
            mix(sunPitchY, 0.42f, curNight),
            -0.82f,
        ).normalized()

        val lightEye = Vec3(0.05f, 0.65f, -0.25f) + sunDir * 5.2f
        Matrix.setLookAtM(
            lightViewM, 0,
            lightEye.x, lightEye.y, lightEye.z,
            0.05f, 0.65f, -0.25f,
            0f, 1f, 0f,
        )
        Matrix.orthoM(lightProjM, 0, -2.9f, 2.9f, -2.2f, 2.4f, 1.0f, 10.5f)
        Matrix.multiplyMM(lightViewProjM, 0, lightProjM, 0, lightViewM, 0)

        // Figuren-Positionen vorab bestimmen (für Kontaktschatten und Lampen/Hand-Referenzen)
        val humanPose = evaluateHumanPose(t, visualTime)
        val catPose = evaluateCatPose(t, visualTime)
        humanContactX = humanPose.x
        humanContactZ = humanPose.z
        humanContactStr = if (humanPose.x > -2.18f) 0.42f else 0f
        catContactX = catPose.x
        catContactZ = catPose.z
        catContactStr = if (catPose.x > -2.18f && catPose.y < 0.25f) (1f - catPose.y / 0.25f) * 0.34f else 0f

        // ---- PASS 1: Schattenkarte rendern ----
        shadowPass = true
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glViewport(0, 0, SHADOW_SIZE, SHADOW_SIZE)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(shaders.shadowProgram)
        GLES30.glUniformMatrix4fv(shaders.shadowLightVP, 1, false, lightViewProjM, 0)
        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glPolygonOffset(2.0f, 3.5f)
        drawWorldGeometry(p, humanPose, catPose, forShadow = true)
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        // ---- PASS 2: Haupt-PBR-Szene ----
        shadowPass = false
        GLES30.glViewport(0, 0, viewW, viewH)
        val clearDark = mix(0.055f, 0.014f, curNight)
        GLES30.glClearColor(clearDark, clearDark, clearDark * 1.25f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(shaders.mainProgram)
        GLES30.glUniformMatrix4fv(shaders.uViewProj, 1, false, viewProjM, 0)
        GLES30.glUniformMatrix4fv(shaders.uLightViewProj, 1, false, lightViewProjM, 0)
        GLES30.glUniform3f(shaders.uCameraPos, camEye.x, camEye.y, camEye.z)
        GLES30.glUniform1f(shaders.uTime, visualTime)
        // Nachts sichtbar reduzierte Belichtung, damit der Raum für die Nachtphase klar dunkel & stimmungsvoll bleibt
        GLES30.glUniform1f(shaders.uExposure, p.belichtung * mix(1.0f, 0.68f, curNight))

        // Richtungslicht: folgt realer Sonnenhöhe (sinkt abends spürbar ab, nachts dezentes Mondlicht, steigt beim Sonnenaufgang!)
        val sunAboveHorizon = smooth(phase(curSunY, 0.04f, 0.42f))
        lerpRgb(sunDayRgb, sunEveRgb, curEvening, tmpCol1)
        lerpRgb(tmpCol1, moonNightRgb, curNight, tmpCol1)
        lerpRgb(tmpCol1, sunDawnRgb, curDawn, tmpCol1)
        val dayEveSun = (curDaylight * 2.55f + curEvening * 0.55f) * mix(0.22f, 1.0f, sunAboveHorizon)
        val dawnSun = curDawn * (0.28f + 1.45f * sunAboveHorizon)
        val sunIntensity = (dayEveSun + dawnSun + curNight * 0.13f).coerceIn(0.12f, 2.60f)
        GLES30.glUniform3f(shaders.uSunDir, sunDir.x, sunDir.y, sunDir.z)
        GLES30.glUniform3f(shaders.uSunColor, tmpCol1[0] * sunIntensity, tmpCol1[1] * sunIntensity, tmpCol1[2] * sunIntensity)

        // Hemisphären-IBL (hell am Tag -> warm gedimmt am Abend -> sichtbar dunkel in der Nacht -> aufhellend bei Sonnenaufgang)
        lerpRgb(skyDayRgb, skyEveRgb, curEvening, tmpCol2)
        lerpRgb(tmpCol2, skyNightRgb, curNight, tmpCol2)
        lerpRgb(tmpCol2, skyDawnRgb, curDawn * 0.80f, tmpCol2)
        val skyI = (curDaylight * 0.76f + curEvening * 0.28f + curNight * 0.068f + curDawn * (0.18f + 0.28f * sunAboveHorizon))
            .coerceIn(0.065f, 0.78f)
        GLES30.glUniform3f(shaders.uSkyColor, tmpCol2[0] * skyI, tmpCol2[1] * skyI, tmpCol2[2] * skyI)

        lerpRgb(gndDayRgb, gndNightRgb, curNight, tmpCol3)
        val gndI = (curDaylight * 0.48f + curEvening * 0.21f + curNight * 0.046f + curDawn * (0.12f + 0.18f * sunAboveHorizon))
            .coerceIn(0.045f, 0.50f)
        GLES30.glUniform3f(shaders.uGroundColor, tmpCol3[0] * gndI, tmpCol3[1] * gndI, tmpCol3[2] * gndI)

        // Schreibtischlampe Spotlight
        val lampDir = (lampAimWorld - lampBulbWorld).normalized()
        val lampStr = curLampeAn * 3.85f
        GLES30.glUniform3f(shaders.uLampPos, lampBulbWorld.x, lampBulbWorld.y, lampBulbWorld.z)
        GLES30.glUniform3f(shaders.uLampDir, lampDir.x, lampDir.y, lampDir.z)
        GLES30.glUniform3f(shaders.uLampColor, 1.0f * lampStr, 0.74f * lampStr, 0.36f * lampStr)

        // Monitor-Glow
        val monStr = curScreenOn * 0.95f
        GLES30.glUniform3f(shaders.uMonitorPos, monitorWorld.x - 0.12f, monitorWorld.y, monitorWorld.z + 0.08f)
        GLES30.glUniform3f(
            shaders.uMonitorColor,
            (0.45f + p.akzentHell[0] * 0.55f) * monStr,
            (0.55f + p.akzentHell[1] * 0.45f) * monStr,
            (0.75f + p.akzentHell[2] * 0.25f) * monStr,
        )

        // Warmes Flurlicht durch geöffnete Tür (bei geschlossener Tür 0 -> kein Lichtleck in der Nacht)
        val doorOpen = (curDoorAngle / 74f).coerceIn(0f, 1f)
        val doorStr = doorOpen * mix(1.45f, 0.85f, curNight)
        GLES30.glUniform3f(shaders.uDoorGlowPos, -2.05f, 1.15f, -0.36f)
        GLES30.glUniform3f(shaders.uDoorGlowColor, p.flurLicht[0] * doorStr, p.flurLicht[1] * doorStr, p.flurLicht[2] * doorStr)

        // Kontaktschatten (Mensch, Stuhl, Katze, Schreibtisch)
        GLES30.glUniform4f(shaders.uContact0, humanContactX, humanContactZ, 0.36f, humanContactStr)
        GLES30.glUniform4f(shaders.uContact1, curChairX, curChairZ, 0.40f, 0.32f)
        GLES30.glUniform4f(shaders.uContact2, catContactX, catContactZ, 0.24f, catContactStr)
        GLES30.glUniform4f(shaders.uContact3, DESK_X + 0.05f, DESK_Z, 0.85f, 0.26f)

        // Schatten-Textur an Slot 0 binden
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex)
        GLES30.glUniform1i(shaders.uShadowMap, 0)

        drawWorldGeometry(p, humanPose, catPose, forShadow = false)

        // ---- PASS 3: Transparente Atmosphäre (Sonnen-/Mond-/Morgenstrahlen, Lampenkegel, Dampf, Staub) ----
        drawAtmosphere(visualTime, camEye)

        GLES30.glBindVertexArray(0)
    }

    private fun setTmpEmissive(rgb: FloatArray, scale: Float): FloatArray {
        tmpEmissive[0] = rgb[0] * scale
        tmpEmissive[1] = rgb[1] * scale
        tmpEmissive[2] = rgb[2] * scale
        return tmpEmissive
    }

    private fun drawMesh(
        mesh: GpuMesh,
        base: FloatArray,
        rough: Float = 0.65f,
        metal: Float = 0f,
        emissive: FloatArray = zeroRgb,
        pattern: Int = 0,
        alt: FloatArray = base,
        p0: Float = 0f,
        p1: Float = 0f,
        p2: Float = 0f,
        p3: Float = 0f,
    ) {
        if (shadowPass) {
            GLES30.glUniformMatrix4fv(shaders.shadowModel, 1, false, ms.current, 0)
            mesh.draw()
            return
        }
        GLES30.glUniformMatrix4fv(shaders.uModel, 1, false, ms.current, 0)
        computeNormalMatrix3(ms.current, normalM)
        GLES30.glUniformMatrix3fv(shaders.uNormalMat, 1, false, normalM, 0)
        GLES30.glUniform3f(shaders.uBaseColor, base[0], base[1], base[2])
        GLES30.glUniform3f(shaders.uAltColor, alt[0], alt[1], alt[2])
        GLES30.glUniform1f(shaders.uRoughness, rough)
        GLES30.glUniform1f(shaders.uMetallic, metal)
        GLES30.glUniform3f(shaders.uEmissive, emissive[0], emissive[1], emissive[2])
        GLES30.glUniform1i(shaders.uPattern, pattern)
        GLES30.glUniform4f(shaders.uParam, p0, p1, p2, p3)
        mesh.draw()
    }

    private fun drawWorldGeometry(p: Palette3D, human: HumanPose, cat: CatPose, forShadow: Boolean) {
        ms.reset()

        // 1. Raum (Boden, Wände, vertieftes Fenster, schwingende Tür, Deko, Katzenkörbchen links oben)
        drawRoom(p, forShadow)

        // 2. Schreibtisch, PC-Setup, Lampe & Tasse
        drawWorkstation(p, forShadow)

        // 3. Ergonomischer 3D-Drehstuhl (rollt & schwenkt weich für natürlichen Einstieg)
        drawChair(p)

        // 4. Natürlich artikulierter 3D-Mensch (merkt sich rechte Hand für Kaffeetasse)
        drawHuman(p, human)

        // 5. Kaffeetasse (auf dem Tisch oder beim Trinken in der Hand)
        drawCoffeeMug(p)

        // 6. Animierte 3D-Katze
        drawCat(p, cat)
    }

    private fun drawRoom(p: Palette3D, forShadow: Boolean) {
        // Holzdielenboden (wirft selbst keinen Schatten, reicht auch bei breiten Tablets/Foldables bis hinter die Kamera)
        if (!forShadow) {
            ms.scoped {
                ms.translate(0.0f, -0.05f, 1.05f)
                ms.scale(4.8f, 0.10f, 6.2f)
                drawMesh(meshes.box, p.bodenHolz, rough = 0.45f, pattern = 1, alt = p.bodenHolzAlt)
            }
            // Helle Decke über dem Raum
            ms.scoped {
                ms.translate(0.0f, 2.81f, 1.05f)
                ms.scale(4.8f, 0.10f, 6.2f)
                drawMesh(meshes.box, p.fussleiste, rough = 0.90f)
            }
        }

        // Gewebter Teppich im linken/mittleren Bereich
        ms.scoped {
            ms.translate(-0.78f, 0.009f, 0.30f)
            ms.scale(1.42f, 0.018f, 1.02f)
            drawMesh(meshes.beveledBox, p.teppichBasis, rough = 0.92f, pattern = 3, alt = p.teppichRand)
        }

        // ---- Rückwand mit echter 3D-Fensteröffnung (x: 0.25..1.85, y: 0.85..2.15) ----
        ms.scoped {
            ms.translate(-1.05f, 1.38f, -1.76f)
            ms.scale(2.60f, 2.76f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(2.25f, 1.38f, -1.76f)
            ms.scale(0.80f, 2.76f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(1.05f, 0.425f, -1.76f)
            ms.scale(1.60f, 0.85f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(1.05f, 2.455f, -1.76f)
            ms.scale(1.60f, 0.61f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }

        // Fußleiste Rückwand
        ms.scoped {
            ms.translate(-0.05f, 0.055f, -1.635f)
            ms.scale(4.20f, 0.11f, 0.03f)
            drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f)
        }

        // ---- Fenster-Details: Himmel draußen, tiefer Holzrahmen, Fensterbank & Faltvorhänge ----
        if (!forShadow) {
            ms.scoped {
                ms.translate(1.05f, 1.50f, -2.06f)
                ms.scale(1.90f, 1.52f, 1f)
                drawMesh(
                    meshes.quadXY, p.himmelTagOben, rough = 1f, pattern = 8, alt = p.himmelTagUnten,
                    p0 = curEvening, p1 = curNight, p2 = curDawn, p3 = curSunY,
                )
            }
        }
        ms.scoped {
            ms.translate(1.05f, 0.845f, -1.59f)
            ms.scale(1.76f, 0.048f, 0.34f)
            drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.42f)
        }
        val frameZ = -1.77f
        ms.scoped { ms.translate(1.05f, 2.13f, frameZ); ms.scale(1.62f, 0.055f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.05f, 0.88f, frameZ); ms.scale(1.62f, 0.055f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(0.27f, 1.50f, frameZ); ms.scale(0.055f, 1.30f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.83f, 1.50f, frameZ); ms.scale(0.055f, 1.30f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.05f, 1.50f, frameZ); ms.scale(0.036f, 1.24f, 0.045f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.05f, 1.62f, frameZ); ms.scale(1.56f, 0.032f, 0.045f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }

        // Gardinenstange + sanft schwingende 3D-Faltvorhänge
        ms.scoped {
            ms.translate(1.03f, 2.24f, -1.55f)
            ms.rotateZ(90f)
            ms.scale(0.026f, 1.98f, 0.026f)
            drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.32f, metal = 1f)
        }
        val sway = sin(curVisualTime * 0.85f) * 1.5f
        ms.scoped {
            ms.translate(0.20f, 1.50f, -1.54f)
            ms.rotateZ(sway)
            ms.scale(0.32f, 1.46f, 0.45f)
            drawMesh(meshes.curtain, p.vorhang, rough = 0.86f)
        }
        ms.scoped {
            ms.translate(1.86f, 1.50f, -1.54f)
            ms.rotateZ(-sway * 0.85f)
            ms.scale(0.28f, 1.46f, 0.45f)
            drawMesh(meshes.curtain, p.vorhang, rough = 0.86f)
        }

        // ---- Rechte Wand (schließt den Raum rechts vollständig, auch auf breiten Foldables & Tablets) ----
        ms.scoped {
            ms.translate(2.16f, 1.38f, 1.05f)
            ms.scale(0.22f, 2.76f, 5.84f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(2.035f, 0.055f, 1.05f)
            ms.scale(0.03f, 0.11f, 5.40f)
            drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f)
        }
        // Rechtes Wandbild (unverändert: Sonnenuntergangs-Berge, pattern = 9)
        ms.scoped {
            ms.translate(2.025f, 1.50f, 0.04f)
            ms.rotateY(-90f)
            ms.scoped { ms.scale(0.62f, 0.48f, 0.036f); drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.44f) }
            if (!forShadow) {
                ms.scoped {
                    ms.translate(0f, 0f, 0.020f)
                    ms.scale(0.54f, 0.40f, 1f)
                    drawMesh(meshes.quadXY, p.akzentHell, rough = 0.52f, pattern = 9, alt = p.akzent)
                }
            }
        }
        // Kleines schwebendes Deko-Regal rechts
        ms.scoped {
            ms.translate(1.94f, 1.08f, 0.78f)
            ms.rotateY(-90f)
            ms.scoped {
                ms.scale(0.58f, 0.036f, 0.20f)
                drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.42f, pattern = 2, alt = p.bodenHolzAlt)
            }
            for (bx in floatArrayOf(-0.20f, 0.20f)) {
                ms.scoped { ms.translate(bx, -0.055f, -0.06f); ms.scale(0.022f, 0.09f, 0.10f); drawMesh(meshes.box, darkMetalRgb, rough = 0.35f, metal = 1f) }
            }
            for (bi in 0 until 3) {
                val bh = 0.17f + bi * 0.022f
                ms.scoped {
                    ms.translate(-0.18f + bi * 0.052f, 0.018f + bh * 0.5f, 0f)
                    ms.scale(0.042f, bh, 0.14f)
                    drawMesh(meshes.beveledBox, bookColors[(bi + 2) % bookColors.size], rough = 0.62f)
                }
            }
            ms.scoped {
                ms.translate(0.14f, 0.018f, 0f)
                drawPottedPlant(p, potScale = 0.10f, leafCount = 5, leafScale = 0.16f, phaseOffset = 2.6f)
            }
        }
        // Große Zimmerpflanze rechts
        ms.scoped {
            ms.translate(1.74f, 0f, 0.72f)
            drawPottedPlant(p, potScale = 0.24f, leafCount = 7, leafScale = 0.38f, phaseOffset = 3.2f)
        }

        // ---- Linke Wand mit echter 3D-Türöffnung (z: -0.82..+0.08, y: 0..2.04) ----
        ms.scoped {
            ms.translate(-2.26f, 1.38f, -1.345f)
            ms.scale(0.22f, 2.76f, 1.05f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(-2.26f, 1.38f, 1.98f)
            ms.scale(0.22f, 2.76f, 3.80f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(-2.26f, 2.40f, -0.37f)
            ms.scale(0.22f, 0.72f, 0.90f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        ms.scoped { ms.translate(-2.135f, 0.055f, -1.24f); ms.scale(0.03f, 0.11f, 0.84f); drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f) }
        ms.scoped { ms.translate(-2.135f, 0.055f, 1.98f); ms.scale(0.03f, 0.11f, 3.80f); drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f) }

        // Geschlossener warmer Flur-Vorraum hinter der Tür (kein dunkler Spalt bei breiten Foldables/Tablets!)
        if (!forShadow) {
            val hallGlow = (0.52f + (curDoorAngle / 74f) * 0.45f) * mix(1.0f, 0.18f, curNight)
            val hallEm = setTmpEmissive(p.flurLicht, hallGlow)
            // Rückwand des Flurs
            ms.scoped {
                ms.translate(-2.92f, 1.12f, -0.37f)
                ms.scale(0.10f, 2.50f, 3.20f)
                drawMesh(meshes.box, p.flurLicht, rough = 0.9f, emissive = hallEm)
            }
            // Seitenwände & Boden des Flurs direkt hinter der Zarge (schließen jede schräge Sichtlinie)
            ms.scoped {
                ms.translate(-2.58f, 1.12f, 0.24f)
                ms.scale(0.84f, 2.50f, 0.08f)
                drawMesh(meshes.box, p.wandLinks, rough = 0.88f, emissive = hallEm)
            }
            ms.scoped {
                ms.translate(-2.58f, 1.12f, -0.96f)
                ms.scale(0.84f, 2.50f, 0.08f)
                drawMesh(meshes.box, p.wandLinks, rough = 0.88f, emissive = hallEm)
            }
            ms.scoped {
                ms.translate(-2.58f, -0.05f, -0.37f)
                ms.scale(0.95f, 0.10f, 1.60f)
                drawMesh(meshes.box, p.bodenHolz, rough = 0.50f, pattern = 1, alt = p.bodenHolzAlt)
            }
        }
        // Türzarge
        ms.scoped { ms.translate(-2.13f, 1.02f, -0.84f); ms.scale(0.08f, 2.08f, 0.07f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }
        ms.scoped { ms.translate(-2.13f, 1.02f, 0.10f); ms.scale(0.08f, 2.08f, 0.07f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }
        ms.scoped { ms.translate(-2.13f, 2.06f, -0.37f); ms.scale(0.08f, 0.07f, 1.01f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }

        // Real schwingendes 3D-Türblatt (Scharnier bei x = -2.14, z = -0.80)
        ms.scoped {
            ms.translate(-2.14f, 0f, -0.80f)
            ms.rotateY(curDoorAngle)
            ms.scoped {
                ms.translate(0.025f, 1.01f, 0.44f)
                ms.scale(0.048f, 2.01f, 0.87f)
                drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.44f, pattern = 2, alt = p.bodenHolzAlt)
            }
            ms.scoped { ms.translate(0.052f, 1.46f, 0.44f); ms.scale(0.015f, 0.68f, 0.56f); drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.48f) }
            ms.scoped { ms.translate(0.052f, 0.58f, 0.44f); ms.scale(0.015f, 0.62f, 0.56f); drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.48f) }
            ms.scoped { ms.translate(0.068f, 0.98f, 0.77f); ms.scale(0.03f, 0.03f, 0.11f); drawMesh(meshes.beveledBox, brassRgb, rough = 0.24f, metal = 1f) }
            ms.scoped { ms.translate(-0.018f, 0.98f, 0.77f); ms.scale(0.03f, 0.03f, 0.11f); drawMesh(meshes.beveledBox, brassRgb, rough = 0.24f, metal = 1f) }
        }

        // ---- Wandregal mit 3D-Büchern & kleinem Blumentopf ----
        val shelfY = 1.54f
        ms.scoped {
            ms.translate(-1.14f, shelfY, -1.52f)
            ms.scale(1.16f, 0.042f, 0.24f)
            drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.42f, pattern = 2, alt = p.bodenHolzAlt)
        }
        for (bx in floatArrayOf(-1.55f, -0.73f)) {
            ms.scoped { ms.translate(bx, shelfY - 0.065f, -1.58f); ms.scale(0.025f, 0.11f, 0.12f); drawMesh(meshes.box, darkMetalRgb, rough = 0.35f, metal = 1f) }
        }
        var bookX = -1.62f
        for (i in 0 until 8) {
            val bh = 0.20f + (i * 37 % 5) * 0.024f
            val bw = 0.042f + (i % 3) * 0.010f
            val bd = 0.16f + (i % 2) * 0.025f
            ms.scoped {
                if (i == 6) {
                    ms.translate(bookX + 0.055f, shelfY + 0.021f + bh * 0.47f, -1.51f)
                    ms.rotateZ(-16f)
                } else {
                    ms.translate(bookX + bw * 0.5f, shelfY + 0.021f + bh * 0.5f, -1.51f)
                }
                ms.scale(bw, bh, bd)
                drawMesh(meshes.beveledBox, bookColors[i % bookColors.size], rough = 0.62f)
            }
            bookX += bw + 0.008f
        }
        ms.scoped {
            ms.translate(-0.70f, shelfY + 0.021f, -1.50f)
            drawPottedPlant(p, potScale = 0.11f, leafCount = 5, leafScale = 0.17f, phaseOffset = 1.4f)
        }

        // ---- Linkes Wandbild: deutlich anderes Motiv (Botanisches Stillleben mit Keramikvase, pattern = 12) ----
        ms.scoped {
            ms.translate(-0.18f, 1.66f, -1.62f)
            ms.scoped { ms.scale(0.46f, 0.56f, 0.036f); drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.46f) }
            if (!forShadow) {
                ms.scoped {
                    ms.translate(0f, 0f, 0.020f)
                    ms.scale(0.38f, 0.48f, 1f)
                    drawMesh(meshes.quadXY, whiteRgb, rough = 0.56f, pattern = 12, alt = p.pflanzeBlatt)
                }
            }
        }

        // ---- 3D-Wanduhr mit echten drehenden 3D-Zeigern ----
        ms.scoped {
            ms.translate(-1.14f, 2.07f, -1.61f)
            ms.scoped { ms.rotateX(90f); ms.scale(0.28f, 0.045f, 0.28f); drawMesh(meshes.cylinder, p.tischHolz, rough = 0.40f) }
            if (!forShadow) {
                ms.scoped {
                    ms.translate(0f, 0f, 0.024f)
                    ms.scale(0.24f, 0.24f, 1f)
                    drawMesh(meshes.quadXY, whiteRgb, rough = 0.45f, pattern = 10, alt = darkMetalRgb)
                }
            }
            val minuteDeg = -(curVisualTime / 24.0f) * 360f
            val hourDeg = -45f - (curVisualTime / 144.0f) * 360f
            ms.scoped {
                ms.translate(0f, 0f, 0.030f)
                ms.rotateZ(minuteDeg)
                ms.translate(0f, 0.042f, 0f)
                ms.scale(0.011f, 0.095f, 0.008f)
                drawMesh(meshes.box, darkMetalRgb, rough = 0.3f, metal = 1f)
            }
            ms.scoped {
                ms.translate(0f, 0f, 0.034f)
                ms.rotateZ(hourDeg)
                ms.translate(0f, 0.030f, 0f)
                ms.scale(0.015f, 0.068f, 0.008f)
                drawMesh(meshes.box, p.akzent, rough = 0.35f)
            }
            ms.scoped { ms.translate(0f, 0f, 0.037f); ms.scale(0.022f); drawMesh(meshes.sphere, brassRgb, rough = 0.25f, metal = 1f) }
        }

        // ---- Große 3D-Zimmerpflanze in der Ecke hinten links ----
        ms.scoped {
            ms.translate(-1.76f, 0f, -1.28f)
            drawPottedPlant(p, potScale = 0.26f, leafCount = 8, leafScale = 0.44f, phaseOffset = 0f)
        }

        // ---- Gemütliches 3D-Katzenkörbchen links oben am Boden ----
        drawCatBasket(p)
    }

    private fun drawCatBasket(p: Palette3D) {
        ms.scoped {
            ms.translate(CAT_BASKET_X, 0f, CAT_BASKET_Z)
            ms.rotateY(18f)
            // Geflochtener Korb-Boden (bündig mit der Korbwand)
            ms.scoped {
                ms.translate(0f, 0.016f, 0f)
                ms.scale(0.45f, 0.032f, 0.39f)
                drawMesh(meshes.cylinder, p.korbWicker, rough = 0.84f, pattern = 13, alt = p.korbWickerAlt)
            }
            // Geflochtene ovale Korbwand (gemütlich niedrig mit Randhöhe ~0.12 m, passend für klaren Katzensprung)
            ms.scoped {
                ms.translate(0f, 0.052f, 0f)
                ms.rotateX(180f)
                ms.scale(0.54f, 0.092f, 0.47f)
                drawMesh(meshes.catBasketWall, p.korbWicker, rough = 0.84f, pattern = 13, alt = p.korbWickerAlt)
            }
            // Weich gerollter Flechtrand oben (Oberkante bei y ≈ 0.121 m)
            ms.scoped {
                ms.translate(0f, 0.096f, 0f)
                ms.rotateX(90f)
                ms.scale(0.68f, 0.60f, 0.23f)
                drawMesh(meshes.torus, p.korbWicker, rough = 0.82f, pattern = 13, alt = p.korbWickerAlt)
            }
            // Kuscheliges, weiches Innenkissen
            ms.scoped {
                ms.translate(0f, 0.040f, 0f)
                ms.scale(0.42f, 0.078f, 0.36f)
                drawMesh(meshes.sphere, p.korbKissen, rough = 0.90f, pattern = 6)
            }
            // Kleiner farblich passender Zier-Kissenrand hinten links
            ms.scoped {
                ms.translate(-0.10f, 0.062f, -0.07f)
                ms.rotateY(28f)
                ms.scale(0.14f, 0.048f, 0.095f)
                drawMesh(meshes.sphere, p.akzentHell, rough = 0.88f, pattern = 6)
            }
        }
    }

    private fun drawPottedPlant(p: Palette3D, potScale: Float, leafCount: Int, leafScale: Float, phaseOffset: Float) {
        ms.scoped {
            ms.translate(0f, potScale * 0.5f, 0f)
            ms.rotateX(180f)
            ms.scale(potScale * 0.92f, potScale, potScale * 0.92f)
            drawMesh(meshes.frustum, p.topf, rough = 0.72f)
        }
        ms.scoped {
            ms.translate(0f, potScale * 0.95f, 0f)
            ms.scale(potScale * 0.98f, potScale * 0.14f, potScale * 0.98f)
            drawMesh(meshes.cylinder, p.topf, rough = 0.68f)
        }
        ms.scoped {
            ms.translate(0f, potScale * 0.96f, 0f)
            ms.scale(potScale * 0.84f, potScale * 0.05f, potScale * 0.84f)
            drawMesh(meshes.cylinder, soilRgb, rough = 0.95f)
        }
        for (i in 0 until leafCount) {
            val yaw = (i.toFloat() / leafCount) * 360f + 18f
            val pitch = -28f - (i % 3) * 14f + sin(curVisualTime * 1.10f + i * 0.9f + phaseOffset) * 2.2f
            val lScale = leafScale * (0.82f + (i % 3) * 0.14f)
            ms.scoped {
                ms.translate(0f, potScale * 0.94f, 0f)
                ms.rotateY(yaw)
                ms.rotateX(pitch)
                ms.scale(lScale, lScale, lScale)
                drawMesh(meshes.leaf, p.pflanzeBlatt, rough = 0.56f)
            }
        }
    }

    private fun drawWorkstation(p: Palette3D, forShadow: Boolean) {
        ms.scoped {
            ms.translate(DESK_X, 0f, DESK_Z)
            ms.rotateY(DESK_YAW)

            // Schreibtischplatte
            ms.scoped {
                ms.translate(0f, 0.74f, 0f)
                ms.scale(0.76f, 0.046f, 1.42f)
                drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.38f, pattern = 2, alt = p.bodenHolzAlt)
            }
            for (lx in floatArrayOf(-0.30f, 0.30f)) {
                ms.scoped {
                    ms.translate(lx, 0.36f, 0.60f)
                    ms.scale(0.042f, 0.72f, 0.042f)
                    drawMesh(meshes.beveledBox, p.tischGestell, rough = 0.36f, metal = 1f)
                }
            }
            ms.scoped { ms.translate(0f, 0.16f, 0.60f); ms.scale(0.60f, 0.032f, 0.032f); drawMesh(meshes.box, p.tischGestell, rough = 0.36f, metal = 1f) }
            ms.scoped { ms.translate(0.28f, 0.60f, 0f); ms.scale(0.03f, 0.06f, 1.24f); drawMesh(meshes.box, p.tischGestell, rough = 0.40f, metal = 1f) }

            // Rollcontainer
            ms.scoped {
                ms.translate(0.02f, 0.37f, -0.44f)
                ms.scale(0.64f, 0.70f, 0.44f)
                drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.44f, pattern = 2, alt = p.bodenHolzAlt)
            }
            for (dIdx in 0 until 3) {
                val dy = 0.15f + dIdx * 0.22f
                ms.scoped {
                    ms.translate(-0.305f, dy, -0.44f)
                    ms.scale(0.022f, 0.19f, 0.39f)
                    drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.40f)
                }
                ms.scoped {
                    ms.translate(-0.326f, dy + 0.02f, -0.44f)
                    ms.scale(0.018f, 0.018f, 0.12f)
                    drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.24f, metal = 1f)
                }
            }

            // PC-Tower
            ms.scoped {
                ms.translate(0.08f, 0.24f, -0.84f)
                ms.scale(0.44f, 0.48f, 0.22f)
                drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.34f, metal = 0.9f)
                val led = curScreenOn * 1.1f + 0.06f * (1f - curNight * 0.7f)
                ms.scoped {
                    ms.translate(-0.505f, 0f, 0.32f)
                    ms.scale(0.02f, 0.72f, 0.06f)
                    drawMesh(meshes.box, p.akzentHell, rough = 0.2f, emissive = setTmpEmissive(p.akzentHell, led))
                }
            }

            // 3D-Monitor
            ms.scoped {
                ms.translate(0.18f, 0.763f, -0.02f)
                ms.scoped { ms.translate(0f, 0.009f, 0f); ms.scale(0.18f, 0.018f, 0.26f); drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.28f, metal = 1f) }
                ms.scoped {
                    ms.translate(0.04f, 0.15f, 0f)
                    ms.rotateZ(-10f)
                    ms.scale(0.032f, 0.30f, 0.075f)
                    drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.28f, metal = 1f)
                }
                ms.scoped {
                    ms.translate(-0.02f, 0.31f, 0f)
                    ms.rotateZ(5f)
                    monitorWorld = ms.transformPoint(-0.03f, 0f, 0f)
                    ms.scoped { ms.scale(0.038f, 0.39f, 0.66f); drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.30f) }
                    if (!forShadow) {
                        ms.scoped {
                            ms.translate(-0.021f, 0.004f, 0f)
                            ms.rotateY(-90f)
                            ms.scale(0.61f, 0.34f, 1f)
                            drawMesh(
                                meshes.quadXY, darkMetalRgb, rough = 0.14f, pattern = 7, alt = p.akzentHell,
                                p0 = curScreenOn, p1 = curTyped, p2 = curChecked,
                            )
                        }
                    }
                }
            }

            // Mechanische 3D-Tastatur
            ms.scoped {
                ms.translate(-0.15f, 0.774f, -0.02f)
                ms.rotateZ(-4f)
                ms.scoped { ms.scale(0.145f, 0.020f, 0.38f); drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.38f) }
                if (!forShadow) {
                    ms.scoped {
                        ms.translate(0f, 0.011f, 0f)
                        ms.rotateY(-90f)
                        ms.rotateX(-90f)
                        ms.scale(0.35f, 0.12f, 1f)
                        drawMesh(meshes.quadXY, keycapRgb, rough = 0.42f, pattern = 11, alt = p.akzentHell, p0 = curScreenOn)
                    }
                }
            }

            // Mauspad & 3D-Maus
            ms.scoped {
                ms.translate(-0.15f, 0.766f, 0.24f)
                ms.scoped { ms.scale(0.18f, 0.006f, 0.16f); drawMesh(meshes.beveledBox, mousePadRgb, rough = 0.88f) }
                val mouseShift = if (curScreenOn > 0.5f && curMugInHand < 0.5f) sin(curVisualTime * 2.1f) * 0.012f else 0f
                ms.scoped {
                    ms.translate(mouseShift, 0.018f, mouseShift * 0.5f)
                    ms.scale(0.090f, 0.030f, 0.055f)
                    drawMesh(meshes.sphere, offWhiteRgb, rough = 0.30f)
                }
            }

            // Notizbuch & Stift
            ms.scoped {
                ms.translate(0.16f, 0.772f, 0.46f)
                ms.rotateY(14f)
                ms.scoped { ms.scale(0.16f, 0.016f, 0.21f); drawMesh(meshes.beveledBox, p.akzent, rough = 0.72f) }
                ms.scoped {
                    ms.translate(-0.02f, 0.014f, 0.01f)
                    ms.rotateX(90f)
                    ms.rotateZ(18f)
                    ms.scale(0.010f, 0.15f, 0.010f)
                    drawMesh(meshes.cylinder, brassRgb, rough = 0.25f, metal = 1f)
                }
            }

            mugRestWorld = ms.transformPoint(-0.11f, 0.763f, -0.34f)
            lampAimWorld = ms.transformPoint(-0.08f, 0.76f, -0.10f)

            // Gelenkige 3D-Architektenlampe
            ms.scoped {
                ms.translate(0.15f, 0.763f, -0.50f)
                ms.scoped { ms.translate(0f, 0.015f, 0f); ms.scale(0.16f, 0.030f, 0.16f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                ms.translate(0f, 0.03f, 0f)
                ms.rotateY(-38f)
                ms.rotateZ(-18f)
                ms.scoped { ms.translate(0f, 0.16f, 0f); ms.scale(0.018f, 0.32f, 0.018f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                ms.translate(0f, 0.32f, 0f)
                ms.scoped { ms.scale(0.036f); drawMesh(meshes.sphere, brassRgb, rough = 0.24f, metal = 1f) }
                ms.rotateZ(64f)
                ms.scoped { ms.translate(0f, 0.145f, 0f); ms.scale(0.016f, 0.29f, 0.016f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                ms.translate(0f, 0.29f, 0f)
                ms.rotateZ(-18f)
                ms.scoped { ms.scale(0.030f); drawMesh(meshes.sphere, brassRgb, rough = 0.24f, metal = 1f) }
                ms.scoped {
                    ms.translate(0f, -0.045f, 0f)
                    ms.scale(0.17f, 0.13f, 0.17f)
                    drawMesh(meshes.lampShade, p.lampeSchirm, rough = 0.36f)
                }
                lampBulbWorld = ms.transformPoint(0f, -0.075f, 0f)
                val bulbGlow = curLampeAn * 3.5f
                tmpEmissive[0] = 1.0f * bulbGlow
                tmpEmissive[1] = 0.78f * bulbGlow
                tmpEmissive[2] = 0.42f * bulbGlow
                ms.scoped {
                    ms.translate(0f, -0.072f, 0f)
                    ms.scale(0.048f)
                    drawMesh(meshes.sphere, whiteRgb, rough = 0.15f, emissive = tmpEmissive)
                }
            }
        }
    }

    private fun drawChair(p: Palette3D) {
        ms.scoped {
            ms.translate(curChairX, 0f, curChairZ)
            ms.rotateY(DESK_YAW)

            // 5-Stern-Fußkreuz mit Rollen
            for (i in 0 until 5) {
                val ang = i * 72f + 18f
                ms.scoped {
                    ms.rotateY(ang)
                    ms.scoped {
                        ms.translate(0.135f, 0.075f, 0f)
                        ms.rotateZ(-10f)
                        ms.scale(0.27f, 0.026f, 0.032f)
                        drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.32f, metal = 1f)
                    }
                    ms.scoped {
                        ms.translate(0.255f, 0.026f, 0f)
                        ms.scale(0.048f)
                        drawMesh(meshes.sphere, darkMetalRgb, rough = 0.42f)
                    }
                }
            }
            // Gasdruckfeder
            ms.scoped {
                ms.translate(0f, 0.25f, 0f)
                ms.scale(0.056f, 0.36f, 0.056f)
                drawMesh(meshes.cylinder, brushedMetalRgb, rough = 0.22f, metal = 1f)
            }

            // Drehbarer Oberteil
            ms.translate(0f, 0.44f - curChairDrop, 0f)
            ms.rotateY(curChairSwivel)

            ms.scoped {
                ms.translate(0f, 0.03f, 0f)
                ms.scale(0.47f, 0.075f, 0.47f)
                drawMesh(meshes.beveledBox, p.stuhlStoff, rough = 0.80f, pattern = 6)
            }
            ms.scoped {
                ms.translate(-0.22f, 0.14f, 0f)
                ms.rotateZ(12f)
                ms.scale(0.04f, 0.24f, 0.08f)
                drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.34f, metal = 1f)
            }
            ms.scoped {
                ms.translate(-0.245f, 0.35f, 0f)
                ms.rotateY(90f)
                ms.rotateX(-7f)
                ms.scale(0.46f, 0.48f, 0.28f)
                drawMesh(meshes.chairBack, p.stuhlStoff, rough = 0.80f, pattern = 6)
            }
            for (side in floatArrayOf(-1f, 1f)) {
                ms.scoped {
                    ms.translate(-0.04f, 0.14f, side * 0.245f)
                    ms.scale(0.036f, 0.20f, 0.036f)
                    drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.36f, metal = 1f)
                }
                ms.scoped {
                    ms.translate(0.01f, 0.24f, side * 0.245f)
                    ms.scale(0.28f, 0.034f, 0.058f)
                    drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.48f)
                }
            }
        }
    }

    private fun drawCoffeeMug(p: Palette3D) {
        ms.scoped {
            if (curMugInHand > 0.5f) {
                ms.translate(rightHandWorld.x, rightHandWorld.y - 0.035f, rightHandWorld.z)
                ms.rotateY(DESK_YAW)
                ms.rotateZ(curMugTilt)
            } else {
                ms.translate(mugRestWorld.x, mugRestWorld.y, mugRestWorld.z)
                ms.rotateY(DESK_YAW + 24f)
            }
            ms.scoped { ms.translate(0f, 0.048f, 0f); ms.scale(0.078f, 0.096f, 0.078f); drawMesh(meshes.cylinder, whiteRgb, rough = 0.24f) }
            ms.scoped { ms.translate(0f, 0.088f, 0f); ms.scale(0.066f, 0.012f, 0.066f); drawMesh(meshes.cylinder, coffeeRgb, rough = 0.14f) }
            ms.scoped {
                ms.translate(0.042f, 0.048f, 0f)
                ms.scale(0.056f, 0.062f, 0.056f)
                drawMesh(meshes.torus, whiteRgb, rough = 0.24f)
            }
            ms.scoped { ms.translate(0f, 0.025f, 0f); ms.scale(0.080f, 0.014f, 0.080f); drawMesh(meshes.cylinder, p.akzent, rough = 0.30f) }
        }
    }

    // ============================================================================================
    // NATÜRLICH ARTIKULIERTER 3D-MENSCH (2-teilige Wirbelsäule, Fersen-Ballen-Gang, Bodenhaftung)
    // ============================================================================================

    private data class HumanPose(
        val x: Float,
        val z: Float,
        val yawDeg: Float,
        val pelvisY: Float,
        val pelvisRoll: Float,
        val pelvisYaw: Float,
        val torsoLean: Float,
        val torsoTwist: Float,
        val headYaw: Float,
        val headPitch: Float,
        val headRoll: Float,
        val thighL: Float,
        val kneeL: Float,
        val footPitchL: Float,
        val thighR: Float,
        val kneeR: Float,
        val footPitchR: Float,
        val shL: Float,
        val shLOut: Float,
        val elbL: Float,
        val shR: Float,
        val shROut: Float,
        val elbR: Float,
        val blink: Float,
        val smileLift: Float,
    )

    /** Berechnet die exakte Beckenhöhe, damit der tiefere Standfuß ohne Schweben am Boden y=0 aufsetzt. */
    private fun computeGroundedPelvisY(
        thighL: Float, kneeL: Float, footPitchL: Float,
        thighR: Float, kneeR: Float, footPitchR: Float,
    ): Float {
        fun legReach(thigh: Float, knee: Float, footPitch: Float): Float {
            val heelToeDrop = max(0f, -sinDeg(footPitch) * 0.055f) + max(0f, sinDeg(footPitch) * 0.022f)
            return 0.020f + 0.375f * cosDeg(thigh) + 0.385f * cosDeg(thigh - knee) + 0.056f + heelToeDrop
        }
        return max(legReach(thighL, kneeL, footPitchL), legReach(thighR, kneeR, footPitchR))
    }

    private fun evaluateHumanPose(t: Float, visualTime: Float): HumanPose {
        val blinkCycle = (visualTime + 0.9f) % 3.9f
        val blink = if (blinkCycle < 0.14f) sin(blinkCycle / 0.14f * PI_F) else 0f

        // Biomechanischer Gehzyklus mit exakter Bodenhaftung, Hüftwiegen & Fersen-Ballen-Abrollen
        fun walkPose(
            wx: Float, wz: Float, yaw: Float,
            phaseRad: Float, strideAmp: Float,
            headYaw: Float = 0f, headPitch: Float = 3f, smileLift: Float = 0f,
        ): HumanPose {
            val amp = strideAmp.coerceIn(0f, 1f)
            val s = sin(phaseRad) * amp
            val c = cos(phaseRad) * amp
            // Oberschenkel: asymmetrisch (etwas weiter vor als zurück), im Stillstand exakt gleich standPose
            val thighL = s * 28f + mix(2f, 2f, amp)
            val thighR = -s * 28f + mix(-2f, 2f, amp)
            // Kniebeugung: beim Durchschwingen deutlich angehoben, beim Aufsetzen leicht federnd
            val swingKneeL = max(0f, -cos(phaseRad)) * 46f * amp
            val stanceKneeL = max(0f, sin(phaseRad)) * 8f * amp
            val kneeL = swingKneeL + stanceKneeL + 5f
            val swingKneeR = max(0f, cos(phaseRad)) * 46f * amp
            val stanceKneeR = max(0f, -sin(phaseRad)) * 8f * amp
            val kneeR = swingKneeR + stanceKneeR + 5f
            // Natürliches Fersen-Ballen-Abrollen (+ = Zehen hoch beim Fersenauftritt, - = Abdrücken hinten)
            val footL = (max(0f, sin(phaseRad - 0.35f)) * 16f - max(0f, -sin(phaseRad + 0.35f)) * 20f) * amp
            val footR = (max(0f, -sin(phaseRad - 0.35f)) * 16f - max(0f, sin(phaseRad + 0.35f)) * 20f) * amp
            // Exakte Bodenhaftung des Standbeins
            val pelvisY = computeGroundedPelvisY(thighL, kneeL, footL, thighR, kneeR, footR)
            val pelvisRoll = -s * 2.8f
            val pelvisYaw = s * 6.5f
            // Gegenläufige Oberkörper-Rotation & natürlicher lockerer Armschwung (gleitet nahtlos in standPose über)
            // Im Bereich der Türöffnung (x < -1.75) liegen die Arme natürlich etwas näher am Körper (kein Wandkontakt)
            val doorZone = smooth(phase(-wx, 1.65f, 0.45f))
            val armSwingScale = mix(1.0f, 0.68f, doorZone)
            val armOutDeg = mix(7.0f, 4.2f, doorZone)
            val torsoTwist = -s * 8.5f * mix(1.0f, 0.72f, doorZone)
            val shL = -s * 24f * armSwingScale + mix(3f, 2f, amp)
            val shR = s * 24f * armSwingScale + mix(3f, 2f, amp)
            val elbL = 15f + max(0f, -s) * 18f * armSwingScale + 3f * amp
            val elbR = 15f + max(0f, s) * 18f * armSwingScale + 3f * amp
            return HumanPose(
                x = wx, z = wz, yawDeg = yaw,
                pelvisY = pelvisY,
                pelvisRoll = pelvisRoll,
                pelvisYaw = pelvisYaw,
                torsoLean = mix(2.0f, 6.0f, amp),
                torsoTwist = torsoTwist,
                headYaw = headYaw - torsoTwist * 0.45f,
                headPitch = headPitch,
                headRoll = -pelvisRoll * 0.6f,
                thighL = thighL, kneeL = kneeL, footPitchL = footL,
                thighR = thighR, kneeR = kneeR, footPitchR = footR,
                shL = shL, shLOut = armOutDeg, elbL = elbL,
                shR = shR, shROut = armOutDeg, elbR = elbR,
                blink = blink,
                smileLift = smileLift,
            )
        }

        // Ruhige, natürlich aufrechte Standhaltung (leicht gebeugte Knie & Ellbogen, kein Stock)
        fun standPose(wx: Float, wz: Float, yaw: Float, headYaw: Float = 0f, headPitch: Float = 3f): HumanPose {
            val sway = sin(visualTime * 1.15f) * 0.9f
            val py = computeGroundedPelvisY(2f, 5f, 0f, -2f, 5f, 0f)
            return HumanPose(
                x = wx, z = wz, yawDeg = yaw,
                pelvisY = py, pelvisRoll = sway * 0.5f, pelvisYaw = 0f,
                torsoLean = 2.0f, torsoTwist = sway * 0.4f,
                headYaw = headYaw, headPitch = headPitch, headRoll = -sway * 0.4f,
                thighL = 2f, kneeL = 5f, footPitchL = 0f,
                thighR = -2f, kneeR = 5f, footPitchR = 0f,
                shL = 3f, shLOut = 7.5f, elbL = 15f,
                shR = 3f, shROut = 7.5f, elbR = 15f,
                blink = blink,
                smileLift = 0.25f,
            )
        }

        // Entspannte Sitzhaltung am Drehstuhl (folgt sauber der Position & Drehung des Stuhls)
        fun sitPose(
            lean: Float = 6.5f, twist: Float = 0f,
            headYaw: Float = 0f, headPitch: Float = 8f, headRoll: Float = 0f,
            shL: Float = 45f, shLOut: Float = 10f, elbL: Float = 66f,
            shR: Float = 45f, shROut: Float = 10f, elbR: Float = 66f,
            smileLift: Float = 0.2f,
        ): HumanPose {
            val totalYaw = DESK_YAW + curChairSwivel
            val yawRad = degToRad(totalYaw)
            val seatX = curChairX - cos(yawRad) * 0.025f
            val seatZ = curChairZ + sin(yawRad) * 0.025f
            return HumanPose(
                x = seatX, z = seatZ,
                yawDeg = totalYaw,
                pelvisY = 0.542f - curChairDrop,
                pelvisRoll = 0f, pelvisYaw = 0f,
                torsoLean = lean, torsoTwist = twist,
                headYaw = headYaw, headPitch = headPitch, headRoll = headRoll,
                thighL = 81f, kneeL = 81f, footPitchL = 0f,
                thighR = 81f, kneeR = 81f, footPitchR = 0f,
                shL = shL, shLOut = shLOut, elbL = elbL,
                shR = shR, shROut = shROut, elbR = elbR,
                blink = blink,
                smileLift = smileLift,
            )
        }

        fun lerpPose(a: HumanPose, b: HumanPose, u: Float): HumanPose {
            val k = smoother(u)
            return HumanPose(
                x = mix(a.x, b.x, k),
                z = mix(a.z, b.z, k),
                yawDeg = lerpAngleDeg(a.yawDeg, b.yawDeg, k),
                pelvisY = mix(a.pelvisY, b.pelvisY, k),
                pelvisRoll = mix(a.pelvisRoll, b.pelvisRoll, k),
                pelvisYaw = mix(a.pelvisYaw, b.pelvisYaw, k),
                torsoLean = mix(a.torsoLean, b.torsoLean, k),
                torsoTwist = mix(a.torsoTwist, b.torsoTwist, k),
                headYaw = mix(a.headYaw, b.headYaw, k),
                headPitch = mix(a.headPitch, b.headPitch, k),
                headRoll = mix(a.headRoll, b.headRoll, k),
                thighL = mix(a.thighL, b.thighL, k),
                kneeL = mix(a.kneeL, b.kneeL, k),
                footPitchL = mix(a.footPitchL, b.footPitchL, k),
                thighR = mix(a.thighR, b.thighR, k),
                kneeR = mix(a.kneeR, b.kneeR, k),
                footPitchR = mix(a.footPitchR, b.footPitchR, k),
                shL = mix(a.shL, b.shL, k),
                shLOut = mix(a.shLOut, b.shLOut, k),
                elbL = mix(a.elbL, b.elbL, k),
                shR = mix(a.shR, b.shR, k),
                shROut = mix(a.shROut, b.shROut, k),
                elbR = mix(a.elbR, b.elbR, k),
                blink = blink,
                smileLift = mix(a.smileLift, b.smileLift, k),
            )
        }

        // Gemeinsamer Weg-Einlauf mit exakter Bogenlängen-Schrittkopplung (Ankommen & Morgen-Rückkehr)
        fun evalEnterWalk(localT: Float, duration: Float, glanceStart: Float, glanceDur: Float): HumanPose {
            val uRaw = (localT / duration).coerceIn(0f, 1f)
            val u = smoother(uRaw)
            val dist = humanEnterPath.distanceAt(u)
            val walkPhaseRad = dist * (PI_F / HUMAN_STEP_LEN)
            val ramp = smoothPhase(uRaw, 0f, 0.14f) * (1f - smoothPhase(uRaw, 0.84f, 0.16f))
            val wx = humanEnterPath.evalX(u)
            val wz = humanEnterPath.evalZ(u)
            val yaw = humanEnterPath.evalYawDeg(u)
            // Freundlicher Blick links zurück Richtung Katze & Körbchen
            val glance = sin(phase(localT, glanceStart, glanceDur) * PI_F)
            return walkPose(
                wx, wz, yaw, walkPhaseRad, ramp,
                headYaw = glance * 36f,
                headPitch = 3f + glance * 9f,
                smileLift = 0.25f + glance * 0.45f,
            )
        }

        // Natürliches Hinsetzen von vor der offenen Stuhlfront (ohne Stuhllehne oder Armlehne zu berühren!)
        fun evalSitSequence(localT: Float): HumanPose {
            val arriveYaw = humanEnterPath.evalYawDeg(0.995f)
            val openFaceYaw = DESK_YAW - 86f // -60°: Blick in den offenen Raum, Rücken exakt zur offenen Sitzfläche
            return when {
                // 0.0..0.45s: Ruhig vor der offenen Sitzfläche umdrehen (kleiner Ausfallschritt beim Drehen)
                localT < 0.45f -> {
                    val p = phase(localT, 0f, 0.45f)
                    val pivotStep = sin(p * PI_F)
                    val a = standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, arriveYaw)
                    val b = standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, openFaceYaw)
                    lerpPose(a, b, p).copy(
                        thighL = 2f + pivotStep * 10f,
                        kneeL = 5f + pivotStep * 18f,
                    )
                }
                // 0.45..1.25s: Sanft rückwärts in die offene Sitzfläche absenken (Füße bleiben exakt am Boden!)
                localT < 1.25f -> {
                    val p = phase(localT, 0.45f, 0.80f)
                    val bend = sin(p * PI_F)
                    val startSt = standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, openFaceYaw)
                    val seatedOpen = sitPose(
                        lean = 5f, headYaw = 8f, headPitch = 6f,
                        shL = 20f, shLOut = 10f, elbL = 40f,
                        shR = 20f, shROut = 10f, elbR = 40f,
                    )
                    val mid = lerpPose(startSt, seatedOpen, p)
                    val groundedY = max(mid.pelvisY, computeGroundedPelvisY(mid.thighL, mid.kneeL, 0f, mid.thighR, mid.kneeR, 0f))
                    mid.copy(
                        pelvisY = groundedY,
                        torsoLean = mix(2f, 5f, smoother(p)) + bend * 11f,
                    )
                }
                // 1.25..2.15s: Gemeinsam mit dem Drehstuhl flüssig an den Schreibtisch schwenken & heranrollen
                else -> {
                    val p = phase(localT, 1.25f, 0.90f)
                    val openSeated = sitPose(
                        lean = 5f, headYaw = 10f, headPitch = 6f,
                        shL = 20f, shLOut = 10f, elbL = 40f,
                        shR = 20f, shROut = 10f, elbR = 40f,
                    )
                    val deskReady = sitPose(
                        lean = 6.5f, headYaw = 0f, headPitch = 8f,
                        shL = 41f, shLOut = 10f, elbL = 57f,
                        shR = 41f, shROut = 10f, elbR = 57f,
                    )
                    lerpPose(openSeated, deskReady, p)
                }
            }
        }

        // Tippen & Arbeiten am PC (Hände ruhen exakt in Tastaturhöhe!)
        fun typingPose(tNow: Float): HumanPose {
            val tapL = sin(tNow * 13.5f) * 3.6f
            val tapR = sin(tNow * 13.5f + 1.9f) * 3.6f
            val mouseCycle = tNow % 4.8f
            val mouseMove = smoothPhase(mouseCycle, 3.0f, 0.5f) * (1f - smoothPhase(mouseCycle, 4.0f, 0.5f))
            return sitPose(
                lean = 6.5f + sin(tNow * 1.0f) * 1.1f,
                headYaw = sin(tNow * 0.65f) * 4.5f,
                headPitch = 9.5f + sin(tNow * 1.4f) * 2.2f,
                headRoll = sin(tNow * 0.5f) * 1.5f,
                shL = 41f + tapL * 0.50f, shLOut = 10f, elbL = 57f - tapL,
                shR = mix(41f + tapR * 0.50f, 45f, mouseMove),
                shROut = mix(10f, 22f, mouseMove),
                elbR = mix(57f - tapR, 52f, mouseMove),
            )
        }

        return when {
            // ---- 0.0..0.9s: Hinter der sich öffnenden Tür warten (mittig vor der Türöffnung) ----
            t < 0.9f -> standPose(-2.65f, -0.36f, 0f)

            // ---- 0.9..5.2s: Durch die Tür mit sichtbar großzügigem Abstand am Stuhl vorbeigehen ----
            t < 5.2f -> evalEnterWalk(t - 0.9f, 4.3f, glanceStart = 1.6f, glanceDur = 1.7f)

            // ---- 5.2..7.35s: Vor offenem Stuhl drehen, natürlich hinsetzen & zum Tisch schwenken ----
            t < 7.35f -> evalSitSequence(t - 5.2f)

            // ---- 7.35..8.0s: Rechte Hand weckt Monitor/PC auf ----
            t < 8.0f -> {
                val wake = sin(phase(t, 7.35f, 0.65f) * PI_F)
                val base = typingPose(visualTime)
                base.copy(
                    torsoLean = mix(base.torsoLean, 13f, wake),
                    shR = mix(base.shR, 60f, wake),
                    shROut = mix(base.shROut, 18f, wake),
                    elbR = mix(base.elbR, 34f, wake),
                )
            }

            // ---- 8.0..12.9s: Konzentriertes Arbeiten am hellen Tag ----
            t < 12.9f -> typingPose(visualTime)

            // ---- 12.9..15.6s: Kaffeetasse greifen, entspannt trinken, abstellen ----
            t < 15.6f -> {
                val tp = typingPose(visualTime)
                val reach = sitPose(lean = 11f, twist = 8f, headYaw = 12f, headPitch = 11f, shL = 42f, elbL = 62f, shR = 55f, shROut = -8f, elbR = 36f)
                val sip = sitPose(lean = 4f, twist = 4f, headYaw = 4f, headPitch = -9f, shL = 36f, elbL = 62f, shR = 48f, shROut = -4f, elbR = 112f, smileLift = 0.45f)
                when {
                    t < 13.4f -> lerpPose(tp, reach, phase(t, 12.9f, 0.5f))
                    t < 14.0f -> lerpPose(reach, sip, phase(t, 13.4f, 0.6f))
                    t < 14.5f -> sip
                    t < 15.1f -> lerpPose(sip, reach, phase(t, 14.5f, 0.6f))
                    else -> lerpPose(reach, tp, phase(t, 15.1f, 0.5f))
                }
            }

            // ---- 15.6..22.2s: Weiterarbeiten, während es ab 18s ganz allmählich Abend wird ----
            t < 22.2f -> {
                val tp = typingPose(visualTime)
                // Kurzer ruhiger Blick hinaus ins Abendrot bei 19.6..21.5s
                val windowGlance = sin(phase(t, 19.6f, 1.9f) * PI_F)
                tp.copy(
                    headYaw = mix(tp.headYaw, 22f, windowGlance),
                    headPitch = mix(tp.headPitch, -4f, windowGlance),
                )
            }

            // ---- 22.2..23.7s: Schreibtischlampe anschalten, als die Dämmerung einsetzt ----
            t < 23.7f -> {
                val tp = typingPose(visualTime)
                val lampReach = sitPose(lean = 17f, twist = 13f, headYaw = 17f, headPitch = 4f, shL = 40f, elbL = 60f, shR = 71f, shROut = -11f, elbR = 24f)
                when {
                    t < 22.95f -> lerpPose(tp, lampReach, phase(t, 22.2f, 0.75f))
                    else -> lerpPose(lampReach, tp, phase(t, 22.95f, 0.75f))
                }
            }

            // ---- 23.7..28.3s: Weiterarbeiten, ab 26.2s freudig zur herankommenden Katze schauen ----
            t < 28.3f -> {
                val tp = typingPose(visualTime)
                val lookCat = smootherPhase(t, 26.2f, 1.6f)
                tp.copy(
                    torsoTwist = mix(tp.torsoTwist, -14f, lookCat),
                    headYaw = mix(tp.headYaw, -36f, lookCat),
                    headPitch = mix(tp.headPitch, 14f, lookCat),
                    headRoll = mix(tp.headRoll, 5f, lookCat),
                    smileLift = mix(tp.smileLift, 0.85f, lookCat),
                )
            }

            // ---- 28.3..32.1s: Katzenpause — direkter Blickkontakt & sanftes Streicheln über Kopf und Rücken ----
            t < 32.1f -> {
                val tp = typingPose(visualTime)
                val stroke01 = 0.5f + 0.5f * sin(visualTime * 4.2f)
                val pet = sitPose(
                    lean = 13.5f,
                    twist = -22f,
                    headYaw = -6f - stroke01 * 3f,
                    headPitch = 14f + stroke01 * 3f,
                    headRoll = 5f,
                    shL = 28f, shLOut = 10f, elbL = 54f,
                    shR = mix(84f, 80f, stroke01),
                    shROut = -11f,
                    elbR = mix(36f, 14f, stroke01),
                    smileLift = 0.95f,
                )
                when {
                    t < 28.85f -> lerpPose(tp, pet, phase(t, 28.3f, 0.55f))
                    t < 31.45f -> pet
                    else -> lerpPose(pet, tp, phase(t, 31.45f, 0.65f))
                }
            }

            // ---- 32.1..33.1s: Letzte Aufgaben abhaken ----
            t < 33.1f -> typingPose(visualTime)

            // ---- 33.1..34.1s: Zufrieden strecken & Lampe/PC ausschalten ----
            t < 34.1f -> {
                val tp = typingPose(visualTime)
                val stretch = sitPose(lean = -7f, headPitch = -11f, shL = 142f, shLOut = 26f, elbL = 24f, shR = 142f, shROut = 26f, elbR = 24f, smileLift = 0.6f)
                val offReach = sitPose(lean = 13f, twist = 10f, headYaw = 12f, shL = 32f, elbL = 54f, shR = 65f, shROut = -10f, elbR = 26f)
                when {
                    t < 33.55f -> lerpPose(tp, stretch, phase(t, 33.1f, 0.45f))
                    else -> lerpPose(stretch, offReach, phase(t, 33.55f, 0.55f))
                }
            }

            // ---- 34.1..35.95s: Stuhl schwenkt auf, Mensch steht natürlich vor dem Stuhl auf & dreht Richtung Tür ----
            t < 35.95f -> {
                val exitStartYaw = humanExitPath.evalYawDeg(0.005f)
                val openFaceYaw = DESK_YAW - 86f
                when {
                    // 34.1..34.95s: Gemeinsam mit dem Drehstuhl in den freien Raum aufdrehen & zurückrollen
                    t < 34.95f -> {
                        val p = phase(t, 34.1f, 0.85f)
                        val fromDesk = sitPose(lean = 10f, shL = 28f, elbL = 50f, shR = 28f, elbR = 50f)
                        val readyStand = sitPose(lean = 14f, headYaw = -12f, shL = 18f, elbL = 36f, shR = 18f, elbR = 36f)
                        lerpPose(fromDesk, readyStand, p)
                    }
                    // 34.95..35.65s: Sauber nach vorne-oben aus dem offenen Stuhl aufstehen (Füße exakt am Boden!)
                    t < 35.65f -> {
                        val p = phase(t, 34.95f, 0.70f)
                        val liftLean = sin(p * PI_F) * 12f
                        val seatedOpen = sitPose(lean = 14f, shL = 18f, elbL = 36f, shR = 18f, elbR = 36f)
                        val stoodUp = standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, openFaceYaw)
                        val mid = lerpPose(seatedOpen, stoodUp, p)
                        val groundedY = max(mid.pelvisY, computeGroundedPelvisY(mid.thighL, mid.kneeL, 0f, mid.thighR, mid.kneeR, 0f))
                        mid.copy(
                            pelvisY = groundedY,
                            torsoLean = mix(14f, 3f, smoother(p)) + liftLean,
                        )
                    }
                    // 35.65..35.95s: Flüssig Richtung Laufbogen zur Tür eindrehen
                    else -> {
                        val p = phase(t, 35.65f, 0.30f)
                        lerpPose(
                            standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, openFaceYaw),
                            standPose(CHAIR_ENTRY_X, CHAIR_ENTRY_Z, exitStartYaw),
                            p,
                        )
                    }
                }
            }

            // ---- 35.95..39.75s: Im weiten Bogen mit klar sichtbarem Abstand am Stuhl vorbei zur Tür hinausgehen ----
            t < 39.75f -> {
                val uRaw = phase(t, 35.95f, 3.80f)
                val u = smoother(uRaw)
                val dist = humanExitPath.distanceAt(u)
                val walkPhaseRad = dist * (PI_F / HUMAN_STEP_LEN)
                val ramp = smoothPhase(uRaw, 0f, 0.14f) * (1f - smoothPhase(uRaw, 0.88f, 0.12f))
                walkPose(
                    humanExitPath.evalX(u),
                    humanExitPath.evalZ(u),
                    humanExitPath.evalYawDeg(u),
                    walkPhaseRad,
                    ramp,
                )
            }

            // ---- 39.75..54.6s: Eigenständige dunkle Nacht & Sonnenaufgang (Mensch außerhalb des Raums) ----
            t < 54.6f -> standPose(-2.65f, -0.36f, 0f)

            // ---- 54.6..58.9s: Rückkehr des Menschen im hellen Morgenlicht (wieder mit weitem Abstand zum Stuhl!) ----
            t < 58.9f -> evalEnterWalk(t - 54.6f, 4.3f, glanceStart = 1.5f, glanceDur = 1.8f)

            // ---- 58.9..61.05s: Natürlich vor dem Stuhl drehen, hinsetzen & an den Schreibtisch schwenken ----
            t < 61.05f -> evalSitSequence(t - 58.9f)

            // ---- 61.05..66.0s: Längerer, ruhiger Morgen-Ausklang im hellen Zimmer (endet exakt wie t = 8.0s!) ----
            else -> {
                val wake = sin(phase(t, 61.05f, 0.65f) * PI_F)
                val sunEnjoy = sin(phase(t, 61.8f, 1.9f) * PI_F)
                val catSmile = sin(phase(t, 63.6f, 1.5f) * PI_F)
                val base = typingPose(visualTime)
                base.copy(
                    torsoLean = mix(base.torsoLean, 12f, wake) - sunEnjoy * 2.5f,
                    headYaw = base.headYaw + sunEnjoy * 24f + catSmile * 28f,
                    headPitch = base.headPitch - sunEnjoy * 9f + catSmile * 4f,
                    headRoll = base.headRoll + catSmile * 4f,
                    shR = mix(base.shR, 58f, wake),
                    shROut = mix(base.shROut, 17f, wake),
                    elbR = mix(base.elbR, 36f, wake),
                    smileLift = clamp01(0.20f + sunEnjoy * 0.40f + catSmile * 0.50f),
                )
            }
        }
    }

    private fun drawHuman(p: Palette3D, h: HumanPose) {
        ms.scoped {
            ms.translate(h.x, h.pelvisY, h.z)
            ms.rotateY(h.yawDeg + h.pelvisYaw)
            ms.rotateX(h.pelvisRoll)

            // Anatomisches Becken / Hüfte mit sauberem Hosenbund-Übergang
            ms.scoped {
                ms.translate(0f, 0.01f, 0f)
                ms.scale(0.185f, 0.135f, 0.255f)
                drawMesh(meshes.sphere, p.hose, rough = 0.80f)
            }
            // Unterer Hoodie-Saum über der Hüfte
            ms.scoped {
                ms.translate(0f, 0.055f, 0f)
                ms.scale(0.21f, 0.075f, 0.27f)
                drawMesh(meshes.sphere, p.hoodie, rough = 0.85f, pattern = 6)
            }

            // ---- Linkes & Rechtes Bein ----
            drawHumanLeg(p, sideZ = -0.074f, thighDeg = h.thighL, kneeDeg = h.kneeL, footPitch = h.footPitchL)
            drawHumanLeg(p, sideZ = +0.074f, thighDeg = h.thighR, kneeDeg = h.kneeR, footPitch = h.footPitchR)

            // ---- 2-teiliger organischer Oberkörper (Taille -> Brustkorb/Schultern) ----
            val breath = 1f + sin(curVisualTime * 2.0f) * 0.016f
            ms.scoped {
                // Untere Wirbelsäule / Taille (45 % Beugung & Drehung)
                ms.translate(0f, 0.05f, 0f)
                ms.rotateZ(-h.torsoLean * 0.45f)
                ms.rotateY(h.torsoTwist * 0.40f)

                ms.scoped {
                    ms.translate(0f, 0.120f, 0f)
                    ms.scale(0.312f, 0.315f, 0.402f)
                    drawMesh(meshes.thickCapsule, p.hoodie, rough = 0.84f, pattern = 6)
                }
                // Dezente Hoodie-Kängurutasche vorne am Bauch
                ms.scoped {
                    ms.translate(0.095f, 0.085f, 0f)
                    ms.scale(0.048f, 0.115f, 0.185f)
                    drawMesh(meshes.beveledBox, p.hoodie, rough = 0.88f, pattern = 6)
                }

                // Obere Wirbelsäule / Brustkorb & breitere Schultern (55 % Beugung & 60 % Drehung)
                ms.translate(0f, 0.19f, 0f)
                ms.rotateZ(-h.torsoLean * 0.55f)
                ms.rotateY(h.torsoTwist * 0.60f)

                // Geformter Brustkorb mit weichem, faltenfreiem Übergang zur Taille
                ms.scoped {
                    ms.translate(0.006f, 0.098f, 0f)
                    ms.scale(0.326f * breath, 0.318f, 0.436f * breath)
                    drawMesh(meshes.thickCapsule, p.hoodie, rough = 0.84f, pattern = 6)
                }
                // Schultergürtel / Schlüsselbein-Rundung
                ms.scoped {
                    ms.translate(0.005f, 0.205f, 0f)
                    ms.rotateX(90f)
                    ms.scale(0.165f, 0.355f, 0.135f)
                    drawMesh(meshes.capsule, p.hoodie, rough = 0.84f, pattern = 6)
                }
                // Locker fallende Hoodie-Kapuze im Nacken
                ms.scoped {
                    ms.translate(-0.048f, 0.22f, 0f)
                    ms.rotateZ(22f)
                    ms.scale(0.165f, 0.095f, 0.225f)
                    drawMesh(meshes.sphere, p.hoodie, rough = 0.86f, pattern = 6)
                }
                // Zwei helle Hoodie-Kordeln sichtbar außen auf der Brust
                for (sz in floatArrayOf(-1f, 1f)) {
                    ms.scoped {
                        ms.translate(0.119f, 0.145f, sz * 0.032f)
                        ms.rotateZ(-5f)
                        ms.scale(0.010f, 0.125f, 0.010f)
                        drawMesh(meshes.cylinder, offWhiteRgb, rough = 0.72f)
                    }
                }

                // ---- Hals & ausdrucksstarker, natürlich proportionierter 3D-Kopf ----
                ms.scoped {
                    ms.translate(0.012f, 0.24f, 0f)
                    // Hals
                    ms.scoped { ms.translate(0f, 0.045f, 0f); ms.scale(0.076f, 0.095f, 0.076f); drawMesh(meshes.cylinder, p.haut, rough = 0.60f) }

                    ms.translate(0f, 0.148f, 0f)
                    ms.rotateY(h.headYaw)
                    ms.rotateZ(-h.headPitch)
                    ms.rotateX(h.headRoll)

                    // Schädel-Oval + weich geformter Kiefer/Kinn-Bereich (realistische Gesichtsform)
                    ms.scoped { ms.translate(-0.004f, 0.012f, 0f); ms.scale(0.196f, 0.212f, 0.182f); drawMesh(meshes.sphere, p.haut, rough = 0.56f) }
                    ms.scoped { ms.translate(0.018f, -0.048f, 0f); ms.scale(0.146f, 0.124f, 0.136f); drawMesh(meshes.sphere, p.haut, rough = 0.58f) }

                    // Geformte Nase (Nasenrücken + feine Spitze)
                    ms.scoped {
                        ms.translate(0.092f, -0.002f, 0f)
                        ms.rotateZ(-14f)
                        ms.scale(0.024f, 0.034f, 0.020f)
                        drawMesh(meshes.sphere, p.haut, rough = 0.55f)
                    }
                    ms.scoped { ms.translate(0.102f, -0.016f, 0f); ms.scale(0.024f, 0.022f, 0.024f); drawMesh(meshes.sphere, p.haut, rough = 0.55f) }

                    // Sanfter freundlicher Mund
                    ms.scoped {
                        ms.translate(0.084f, -0.052f + h.smileLift * 0.004f, 0f)
                        ms.scale(0.018f, 0.011f + h.smileLift * 0.003f, 0.044f + h.smileLift * 0.010f)
                        drawMesh(meshes.sphere, mouthRgb, rough = 0.54f)
                    }

                    // Ohren (±Z)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped { ms.translate(-0.012f, -0.006f, sz * 0.090f); ms.scale(0.028f, 0.048f, 0.020f); drawMesh(meshes.sphere, p.haut, rough = 0.58f) }
                    }

                    // Ausdrucksstarke Augen (weißes Augenweiß + dunkle Iris mit Glanz + Augenbrauen)
                    val eyeScaleY = (1f - h.blink * 0.88f).coerceAtLeast(0.12f)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        // Sclera
                        ms.scoped {
                            ms.translate(0.080f, 0.016f, sz * 0.039f)
                            ms.scale(0.022f, 0.028f * eyeScaleY, 0.030f)
                            drawMesh(meshes.sphere, scleraRgb, rough = 0.14f)
                        }
                        // Iris / Pupille
                        ms.scoped {
                            ms.translate(0.088f, 0.016f, sz * 0.039f)
                            ms.scale(0.016f, 0.021f * eyeScaleY, 0.020f)
                            drawMesh(meshes.sphere, eyeDarkRgb, rough = 0.06f)
                        }
                        // Augenbraue (hebt sich leicht beim Lächeln / Streicheln)
                        ms.scoped {
                            ms.translate(0.083f, 0.042f + h.smileLift * 0.005f, sz * 0.039f)
                            ms.rotateX(sz * 8f)
                            ms.scale(0.016f, 0.010f, 0.036f)
                            drawMesh(meshes.beveledBox, p.haar, rough = 0.68f)
                        }
                    }

                    // Natürlich gestuftes 3D-Haarvolumen (Krone, Hinterkopf, Schläfen, weicher Pony)
                    ms.scoped { ms.translate(-0.018f, 0.046f, 0f); ms.scale(0.214f, 0.172f, 0.200f); drawMesh(meshes.sphere, p.haar, rough = 0.64f) }
                    ms.scoped { ms.translate(-0.056f, -0.008f, 0f); ms.scale(0.148f, 0.168f, 0.184f); drawMesh(meshes.sphere, p.haar, rough = 0.64f) }
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(-0.014f, 0.016f, sz * 0.074f)
                            ms.scale(0.112f, 0.105f, 0.062f)
                            drawMesh(meshes.sphere, p.haar, rough = 0.66f)
                        }
                    }
                    ms.scoped {
                        ms.translate(0.052f, 0.072f, 0.026f)
                        ms.rotateZ(-16f)
                        ms.rotateY(10f)
                        ms.scale(0.112f, 0.064f, 0.132f)
                        drawMesh(meshes.sphere, p.haar, rough = 0.64f)
                    }
                }

                // ---- Linker & Rechter Arm ----
                drawHumanArm(p, sideZ = -0.172f, shFwd = h.shL, shOut = h.shLOut, elbDeg = h.elbL, trackHand = false)
                drawHumanArm(p, sideZ = +0.172f, shFwd = h.shR, shOut = h.shROut, elbDeg = h.elbR, trackHand = true)
            }
        }
    }

    private fun drawHumanLeg(p: Palette3D, sideZ: Float, thighDeg: Float, kneeDeg: Float, footPitch: Float) {
        ms.scoped {
            ms.translate(0f, -0.020f, sideZ)
            ms.rotateZ(thighDeg)
            // Oberschenkel
            ms.scoped {
                ms.translate(0f, -0.188f, 0f)
                ms.scale(0.265f, 0.405f, 0.265f)
                drawMesh(meshes.capsule, p.hose, rough = 0.80f)
            }
            // Kniegelenk + Unterschenkel
            ms.translate(0f, -0.375f, 0f)
            ms.scoped { ms.scale(0.102f); drawMesh(meshes.sphere, p.hose, rough = 0.80f) }
            ms.rotateZ(-kneeDeg)
            ms.scoped {
                ms.translate(0f, -0.192f, 0f)
                ms.scale(0.222f, 0.398f, 0.222f)
                drawMesh(meshes.capsule, p.hose, rough = 0.80f)
            }
            // Sneaker am Knöchel mit natürlichem Fersen-Ballen-Winkel
            ms.translate(0f, -0.385f, 0f)
            ms.rotateZ(kneeDeg - thighDeg + footPitch)
            // Knöchel / Schuhschaft
            ms.scoped { ms.translate(-0.004f, -0.002f, 0f); ms.scale(0.082f, 0.056f, 0.078f); drawMesh(meshes.sphere, p.schuh, rough = 0.58f) }
            // Schuhkörper + abgerundete Zehenkappe
            ms.scoped {
                ms.translate(0.042f, -0.016f, 0f)
                ms.scale(0.155f, 0.054f, 0.086f)
                drawMesh(meshes.beveledBox, p.schuh, rough = 0.56f)
            }
            ms.scoped {
                ms.translate(0.106f, -0.022f, 0f)
                ms.scale(0.062f, 0.044f, 0.080f)
                drawMesh(meshes.sphere, p.schuh, rough = 0.56f)
            }
            // Weiße Sneaker-Sohle
            ms.scoped {
                ms.translate(0.046f, -0.046f, 0f)
                ms.scale(0.175f, 0.020f, 0.092f)
                drawMesh(meshes.beveledBox, soleWhiteRgb, rough = 0.65f)
            }
        }
    }

    private fun drawHumanArm(p: Palette3D, sideZ: Float, shFwd: Float, shOut: Float, elbDeg: Float, trackHand: Boolean) {
        val sign = if (sideZ > 0f) 1f else -1f
        ms.scoped {
            ms.translate(0.008f, 0.192f, sideZ)
            // Abgerundeter Schulteransatz
            ms.scoped { ms.scale(0.112f); drawMesh(meshes.sphere, p.hoodie, rough = 0.84f, pattern = 6) }
            ms.rotateX(-sign * shOut)
            ms.rotateZ(shFwd)
            // Oberarm
            ms.scoped {
                ms.translate(0f, -0.132f, 0f)
                ms.scale(0.212f, 0.276f, 0.212f)
                drawMesh(meshes.capsule, p.hoodie, rough = 0.84f, pattern = 6)
            }
            // Ellbogen + Unterarm
            ms.translate(0f, -0.252f, 0f)
            ms.scoped { ms.scale(0.084f); drawMesh(meshes.sphere, p.hoodie, rough = 0.84f, pattern = 6) }
            ms.rotateZ(elbDeg)
            ms.scoped {
                ms.translate(0f, -0.110f, 0f)
                ms.scale(0.184f, 0.232f, 0.184f)
                drawMesh(meshes.capsule, p.hoodie, rough = 0.84f, pattern = 6)
            }
            // Ärmelbündchen am Handgelenk
            ms.scoped {
                ms.translate(0f, -0.214f, 0f)
                ms.scale(0.074f, 0.030f, 0.074f)
                drawMesh(meshes.cylinder, p.hoodie, rough = 0.88f, pattern = 6)
            }
            // Handgelenk & geformte Hand (Handfläche + Finger + Daumen)
            ms.translate(0f, -0.236f, 0f)
            if (trackHand) {
                rightHandWorld = ms.transformPoint(0f, -0.025f, 0f)
            }
            ms.scoped { ms.scale(0.058f, 0.068f, 0.044f); drawMesh(meshes.sphere, p.haut, rough = 0.56f) }
            ms.scoped {
                ms.translate(0.012f, -0.036f, 0f)
                ms.rotateZ(12f)
                ms.scale(0.046f, 0.048f, 0.042f)
                drawMesh(meshes.beveledBox, p.haut, rough = 0.56f)
            }
            ms.scoped {
                ms.translate(0.024f, -0.010f, -sign * 0.024f)
                ms.rotateZ(32f)
                ms.scale(0.024f, 0.032f, 0.020f)
                drawMesh(meshes.sphere, p.haut, rough = 0.56f)
            }
        }
    }

    // ============================================================================================
    // ARTIKULIERTE 3D-KATZE (erst ins Körbchen links oben, bei Katzenpause zum Menschen & zurück)
    // ============================================================================================

    private data class CatPose(
        val x: Float,
        val y: Float,
        val z: Float,
        val yawDeg: Float,
        val bodyPitch: Float,
        val hipDrop: Float,
        val gaitPhase: Float,
        val gaitAmp: Float,
        val sitAmt: Float,
        val curlAmt: Float,
        val jumpAmt: Float,
        val headYaw: Float,
        val headPitch: Float,
        val tailBase: Float,
        val tailWave: Float,
        val earTwitch: Float,
        val blink: Float,
    )

    private fun evaluateCatPose(t: Float, visualTime: Float): CatPose {
        val blinkC = (visualTime + 2.1f) % 4.3f
        val blink = if (blinkC < 0.16f) sin(blinkC / 0.16f * PI_F) else 0f
        val earT = sin(visualTime * 2.5f) * 4f + sin(visualTime * 6.8f) * 2.4f

        val deskCatX = CAT_DESK_X
        val deskCatY = 0.763f
        val deskCatZ = CAT_DESK_Z

        // Blickwinkel für den sauberen Bogensprung ins Körbchen bzw. aus dem Körbchen heraus
        val hopInYaw = atan2(-(CAT_BASKET_Z - CAT_BASKET_OUT_Z), CAT_BASKET_X - CAT_BASKET_OUT_X) * (180f / PI_F)
        val hopOutYaw = atan2(-(CAT_BASKET_OUT_Z - CAT_BASKET_Z), CAT_BASKET_OUT_X - CAT_BASKET_X) * (180f / PI_F)

        // Gemeinsamer Weg: durch die Tür vor das Körbchen links oben traben, sauber ÜBER den Rand hineinhüpfen & einkuscheln
        fun evalCatEnterToBasket(localT: Float): CatPose {
            val approachDur = 3.75f
            val hopDur = 0.62f
            val hopEnd = approachDur + hopDur
            return when {
                localT < approachDur -> {
                    val uRaw = (localT / approachDur).coerceIn(0f, 1f)
                    val u = smoother(uRaw)
                    val dist = catToBasketApproachPath.distanceAt(u)
                    val cx = catToBasketApproachPath.evalX(u)
                    val cz = catToBasketApproachPath.evalZ(u)
                    val yaw = catToBasketApproachPath.evalYawDeg(u)
                    val amp = smoothPhase(uRaw, 0f, 0.10f) * (1f - smoothPhase(uRaw, 0.85f, 0.15f))
                    CatPose(
                        cx, 0f, cz, yaw, 0f, 0f,
                        dist * (PI_F / CAT_STEP_LEN), amp,
                        0f, 0f, 0f, sin(visualTime * 1.3f) * 12f, -6f,
                        64f, 17f, earT, blink,
                    )
                }
                localT < hopEnd -> {
                    // Sauberer Bogensprung hoch ÜBER die Korbwand (alle 4 Pfoten mit Luft über dem Rand!)
                    val k = phase(localT, approachDur, hopDur)
                    val arc = sin(k * PI_F)
                    val cx = mix(CAT_BASKET_OUT_X, CAT_BASKET_X, k)
                    val cz = mix(CAT_BASKET_OUT_Z, CAT_BASKET_Z, k)
                    val cy = mix(0f, CAT_BASKET_Y, k) + arc * 0.25f
                    val arriveYaw = catToBasketApproachPath.evalYawDeg(0.995f)
                    val yaw = lerpAngleDeg(arriveYaw, hopInYaw, smooth(k))
                    val pitch = (0.45f - k) * 38f
                    CatPose(
                        cx, cy, cz, yaw, pitch, 0f,
                        0f, 0f, 0f, 0f, arc,
                        0f, -8f, 32f, 12f, earT, blink,
                    )
                }
                else -> {
                    // Im Körbchen ruhig Richtung Raum/Mensch drehen und gemütlich einrollen
                    val settle = smootherPhase(localT, hopEnd, 1.38f)
                    val restYaw = lerpAngleDeg(hopInYaw, -28f, settle)
                    CatPose(
                        CAT_BASKET_X, CAT_BASKET_Y, CAT_BASKET_Z,
                        restYaw, 18f * settle, 0.036f * settle,
                        0f, 0f, settle, settle * 0.85f, 0f,
                        mix(0f, 10f, settle), -8f * settle,
                        mix(44f, 14f, settle), mix(14f, 7f, settle), earT,
                        max(blink, settle * 0.35f),
                    )
                }
            }
        }

        // Gemütliches Ruhen & Beobachten im Körbchen links oben
        fun basketRestPose(): CatPose {
            val look = sin(visualTime * 0.55f) * 18f
            val sleepy = 0.35f + 0.30f * (0.5f + 0.5f * sin(visualTime * 0.42f))
            return CatPose(
                CAT_BASKET_X, CAT_BASKET_Y, CAT_BASKET_Z,
                -28f, 18f, 0.036f,
                0f, 0f, 1f, 0.85f, 0f,
                10f + look, -8f + sin(visualTime * 0.45f) * 4f,
                14f, 7f, earT * 0.7f, max(blink, sleepy),
            )
        }

        return when {
            // ---- 0.0..1.8s: Hinter dem Menschen vor der Tür ----
            t < 1.8f -> CatPose(-2.62f, 0f, -0.30f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 45f, 10f, earT, blink)

            // ---- 1.8..7.55s: Durch die Tür links oben ins Körbchen hüpfen & einkuscheln ----
            t < 7.55f -> evalCatEnterToBasket(t - 1.8f)

            // ---- 7.55..24.05s: Gemütlich links oben im Körbchen ruhen & den Raum beobachten ----
            t < 24.05f -> basketRestPose()

            // ---- 24.05..24.65s: Im Körbchen aufstehen & Richtung Ausstieg drehen (alle 4 Pfoten bleiben im Kissen!) ----
            t < 24.65f -> {
                val up = smootherPhase(t, 24.05f, 0.48f)
                val crouch = smootherPhase(t, 24.42f, 0.23f)
                CatPose(
                    CAT_BASKET_X, CAT_BASKET_Y, CAT_BASKET_Z,
                    lerpAngleDeg(-28f, hopOutYaw, up),
                    mix(18f, 0f, up) - crouch * 7f,
                    mix(0.036f, 0f, up) + crouch * 0.018f,
                    0f, 0f, 1f - up, (1f - up) * 0.85f, 0f,
                    mix(10f, 0f, up), -10f - crouch * 8f,
                    mix(14f, 48f, up), mix(7f, 16f, up), earT, blink,
                )
            }

            // ---- 24.65..25.27s: Eleganter Bogensprung ÜBER die Korbwand hinaus auf den freien Boden (null Wandkontakt!) ----
            t < 25.27f -> {
                val k = phase(t, 24.65f, 0.62f)
                val arc = sin(k * PI_F)
                val cx = mix(CAT_BASKET_X, CAT_BASKET_OUT_X, k)
                val cz = mix(CAT_BASKET_Z, CAT_BASKET_OUT_Z, k)
                val cy = mix(CAT_BASKET_Y, 0f, k) + arc * 0.25f
                val exitYaw = catBasketOutToHumanPath.evalYawDeg(0.005f)
                val yaw = lerpAngleDeg(hopOutYaw, exitYaw, smooth(k))
                val pitch = (0.45f - k) * 38f
                CatPose(
                    cx, cy, cz, yaw, pitch, 0f,
                    0f, 0f, 0f, 0f, arc,
                    4f, -10f, 38f, 16f, earT, blink,
                )
            }

            // ---- 25.27..27.15s: Vom freien Boden vor dem Körbchen zügig & natürlich zum Menschen traben ----
            t < 27.15f -> {
                val uRaw = phase(t, 25.27f, 1.88f)
                val u = smoother(uRaw)
                val dist = catBasketOutToHumanPath.distanceAt(u)
                val cx = catBasketOutToHumanPath.evalX(u)
                val cz = catBasketOutToHumanPath.evalZ(u)
                val yaw = catBasketOutToHumanPath.evalYawDeg(u)
                val amp = smoothPhase(uRaw, 0f, 0.14f) * (1f - smoothPhase(uRaw, 0.84f, 0.16f))
                CatPose(
                    cx, 0f, cz, yaw, 0f, 0f,
                    dist * (PI_F / CAT_STEP_LEN), amp,
                    0f, 0f, 0f, 8f, -12f,
                    66f, 20f, earT, blink,
                )
            }

            // ---- 27.15..27.70s: Niedriges Ducken vor dem Schreibtisch (Antizipation vor dem Sprung) ----
            t < 27.70f -> {
                val crouch = smootherPhase(t, 27.15f, 0.50f)
                val arriveYaw = catBasketOutToHumanPath.evalYawDeg(0.995f)
                CatPose(
                    CAT_LAUNCH_X, 0f, CAT_LAUNCH_Z,
                    lerpAngleDeg(arriveYaw, 44f, crouch),
                    -13f * crouch, 0.062f * crouch,
                    0f, 0f, 0f, 0f, 0f,
                    0f, -26f * crouch,
                    28f, 24f, earT, blink,
                )
            }

            // ---- 27.70..28.35s: Eleganter 3D-Bogensprung auf die Schreibtischente ----
            t < 28.35f -> {
                val k = phase(t, 27.70f, 0.65f)
                val arc = sin(k * PI_F)
                val cx = mix(CAT_LAUNCH_X, deskCatX, k)
                val cz = mix(CAT_LAUNCH_Z, deskCatZ, k)
                val cy = mix(0f, deskCatY, k) + arc * 0.36f
                val pitch = (0.5f - k) * 60f
                CatPose(cx, cy, cz, mix(44f, 56f, k), pitch, 0f, k * PI_F, 0f, 0f, 0f, arc, 0f, -10f, 18f, 12f, earT, blink)
            }

            // ---- 28.35..32.05s: Auf dem Schreibtisch sitzen, zum Menschen schauen & gestreichelt werden ----
            t < 32.05f -> {
                val turn = smootherPhase(t, 28.35f, 0.55f)
                val sit = smootherPhase(t, 28.50f, 0.60f) * (1f - smootherPhase(t, 31.65f, 0.40f))
                val petted = smootherPhase(t, 28.65f, 0.45f) * (1f - smootherPhase(t, 31.35f, 0.50f))
                val purrNod = sin(visualTime * 4.2f) * 6.5f * petted
                CatPose(
                    deskCatX, deskCatY, deskCatZ,
                    lerpAngleDeg(56f, 165f, turn),
                    24f * sit - petted * 5f,
                    0.028f * sit,
                    0f, 0f, sit, 0f, 0f,
                    mix(0f, 6f, turn) + purrNod * 0.4f,
                    -14f * petted + purrNod,
                    38f + petted * 24f,
                    16f + petted * 15f,
                    earT,
                    max(blink, petted * 0.65f),
                )
            }

            // ---- 32.05..32.70s: Sanft vom Schreibtisch herunterhüpfen ----
            t < 32.70f -> {
                val k = phase(t, 32.05f, 0.65f)
                val arc = sin(k * PI_F)
                val cx = mix(deskCatX, 0.56f, k)
                val cz = mix(deskCatZ, 0.46f, k)
                val cy = mix(deskCatY, 0f, k) + arc * 0.18f
                CatPose(cx, cy, cz, 168f, (0.35f - k) * 46f, 0f, 0f, 0f, 0f, 0f, arc * 0.7f, 0f, 8f, 50f, 12f, earT, blink)
            }

            // ---- 32.70..35.3s: Im Bogen vor dem Stuhl Richtung Tür traben & auf den Menschen warten ----
            t < 35.3f -> {
                val uRaw = phase(t, 32.70f, 2.00f)
                val u = smoother(uRaw)
                val dist = catDeskToWaitPath.distanceAt(u)
                val amp = smoothPhase(uRaw, 0f, 0.12f) * (1f - smoothPhase(uRaw, 0.85f, 0.15f))
                val lookBack = smoothPhase(t, 34.4f, 0.6f) * 40f
                CatPose(
                    catDeskToWaitPath.evalX(u), 0f, catDeskToWaitPath.evalZ(u),
                    catDeskToWaitPath.evalYawDeg(u),
                    0f, 0f, dist * (PI_F / CAT_STEP_LEN), amp,
                    0f, 0f, 0f, sin(visualTime * 1.5f) * 18f - lookBack, -10f,
                    68f, 16f, earT, blink,
                )
            }

            // ---- 35.3..38.8s: Gemeinsam mit dem Menschen durch die Tür hinauslaufen ----
            t < 38.8f -> {
                val uRaw = phase(t, 35.3f, 3.2f)
                val u = smoother(uRaw)
                val dist = catWaitToExitPath.distanceAt(u)
                val amp = smoothPhase(uRaw, 0f, 0.12f) * (1f - smoothPhase(uRaw, 0.90f, 0.10f))
                CatPose(
                    catWaitToExitPath.evalX(u), 0f, catWaitToExitPath.evalZ(u),
                    catWaitToExitPath.evalYawDeg(u),
                    0f, 0f, dist * (PI_F / CAT_STEP_LEN), amp,
                    0f, 0f, 0f, 0f, 0f, 66f, 18f, earT, blink,
                )
            }

            // ---- 38.8..55.4s: Dunkle Nacht & Sonnenaufgang (draußen) ----
            t < 55.4f -> CatPose(-2.70f, 0f, -0.32f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 45f, 10f, earT, blink)

            // ---- 55.4..61.15s: Rückkehr im Morgenlicht direkt wieder ins Körbchen links oben ----
            t < 61.15f -> evalCatEnterToBasket(t - 55.4f)

            // ---- 61.15..66.0s: Entspannt im Körbchen links oben ruhen (nahtlos identisch zu t = 8.0s!) ----
            else -> basketRestPose()
        }
    }

    private fun drawCat(p: Palette3D, c: CatPose) {
        ms.scoped {
            ms.translate(c.x, c.y + 0.188f - c.hipDrop, c.z)
            ms.rotateY(c.yawDeg)

            val catBreath = 1f + sin(curVisualTime * 2.4f) * 0.022f

            // Körper (horizontal entlang +X)
            ms.scoped {
                ms.rotateZ(c.bodyPitch)

                // Getigerter Katzenrumpf
                ms.scoped {
                    ms.rotateZ(90f)
                    ms.scale(0.24f * catBreath, 0.35f, 0.21f * catBreath)
                    drawMesh(meshes.thickCapsule, p.katzeFell, rough = 0.82f, pattern = 5, alt = p.katzeStreifen)
                }
                // Cremefarbener Brustlatz
                ms.scoped {
                    ms.translate(0.11f, -0.02f, 0f)
                    ms.scale(0.12f, 0.13f, 0.12f)
                    drawMesh(meshes.sphere, p.katzeCreme, rough = 0.84f)
                }

                // ---- Kopf, Ohren, Schnauze & Augen ----
                ms.scoped {
                    ms.translate(0.175f, 0.085f, 0f)
                    ms.rotateZ(-c.bodyPitch * 0.72f - c.headPitch)
                    ms.rotateY(c.headYaw)

                    ms.scoped { ms.scale(0.145f, 0.135f, 0.145f); drawMesh(meshes.sphere, p.katzeFell, rough = 0.80f, pattern = 5, alt = p.katzeStreifen) }
                    ms.scoped { ms.translate(0.068f, -0.018f, 0f); ms.scale(0.062f, 0.048f, 0.068f); drawMesh(meshes.sphere, p.katzeCreme, rough = 0.82f) }
                    ms.scoped { ms.translate(0.095f, -0.008f, 0f); ms.scale(0.022f, 0.017f, 0.024f); drawMesh(meshes.sphere, pinkNoseRgb, rough = 0.40f) }

                    val eyeY = (1f - c.blink * 0.88f).coerceAtLeast(0.12f)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(0.060f, 0.015f, sz * 0.036f)
                            ms.scale(0.022f, 0.026f * eyeY, 0.022f)
                            drawMesh(meshes.sphere, eyeDarkRgb, rough = 0.08f)
                        }
                    }

                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(-0.005f, 0.052f, sz * 0.048f)
                            ms.rotateY(90f)
                            ms.rotateZ(-sz * (16f + c.earTwitch * 0.5f))
                            ms.scale(0.058f, 0.068f, 0.048f)
                            drawMesh(meshes.catEar, p.katzeFell, rough = 0.78f)
                        }
                    }
                }

                // ---- 4-Segment artikulierter geschwungener Schwanz (legt sich im Körbchen sanft herum) ----
                ms.scoped {
                    ms.translate(-0.155f, 0.025f, 0f)
                    ms.rotateZ(-c.bodyPitch * 0.4f)
                    var segLen = 0.082f
                    var segRad = 0.086f
                    for (seg in 0 until 4) {
                        val swayY = sin(curVisualTime * 2.6f - seg * 0.75f) * c.tailWave * (0.4f + seg * 0.25f) + c.curlAmt * 26f
                        val liftZ = if (seg == 0) c.tailBase else (16f - seg * 11f) * (1f - c.curlAmt * 0.65f)
                        ms.rotateY(swayY)
                        ms.rotateZ(-liftZ)
                        ms.scoped {
                            ms.translate(-segLen * 0.5f, 0f, 0f)
                            ms.rotateZ(90f)
                            ms.scale(segRad, segLen * 1.15f, segRad)
                            val col = if (seg == 3) p.katzeCreme else p.katzeFell
                            drawMesh(meshes.capsule, col, rough = 0.82f, pattern = if (seg < 3) 5 else 0, alt = p.katzeStreifen)
                        }
                        ms.translate(-segLen, 0f, 0f)
                        segLen *= 0.92f
                        segRad *= 0.88f
                    }
                }
            }

            // ---- 4 artikulierte Katzenbeine (Diagonalgang, eingekuschelt im Körbchen, sauber angezogen beim Sprung) ----
            val fl = sin(c.gaitPhase) * 32f * c.gaitAmp
            val fr = sin(c.gaitPhase + PI_F) * 32f * c.gaitAmp
            val bl = sin(c.gaitPhase + PI_F) * 32f * c.gaitAmp
            val br = sin(c.gaitPhase) * 32f * c.gaitAmp

            val frontTuck = c.sitAmt * 8f + c.curlAmt * 24f - c.jumpAmt * 26f
            val frontBend = c.sitAmt * 12f + c.curlAmt * 32f + c.jumpAmt * 44f
            val backSwingOffset = c.sitAmt * 56f + c.jumpAmt * 20f
            val backBend = c.sitAmt * 50f + c.jumpAmt * 46f
            drawCatLeg(p, x = 0.11f, z = -0.052f, swing = fl - frontTuck, bend = frontBend)
            drawCatLeg(p, x = 0.11f, z = +0.052f, swing = fr - frontTuck, bend = frontBend)
            drawCatLeg(p, x = -0.11f, z = -0.056f, swing = bl + backSwingOffset, bend = backBend)
            drawCatLeg(p, x = -0.11f, z = +0.056f, swing = br + backSwingOffset, bend = backBend)
        }
    }

    private fun drawCatLeg(p: Palette3D, x: Float, z: Float, swing: Float, bend: Float) {
        ms.scoped {
            ms.translate(x, -0.02f, z)
            ms.rotateZ(swing)
            ms.scoped {
                ms.translate(0f, -0.048f, 0f)
                ms.scale(0.102f, 0.108f, 0.102f)
                drawMesh(meshes.capsule, p.katzeFell, rough = 0.82f)
            }
            ms.translate(0f, -0.088f, 0f)
            ms.rotateZ(-bend)
            ms.scoped {
                ms.translate(0f, -0.036f, 0f)
                ms.scale(0.086f, 0.085f, 0.086f)
                drawMesh(meshes.capsule, p.katzeFell, rough = 0.82f)
            }
            ms.scoped {
                ms.translate(0.012f, -0.072f, 0f)
                ms.scale(0.048f, 0.028f, 0.044f)
                drawMesh(meshes.sphere, p.katzeCreme, rough = 0.84f)
            }
        }
    }

    // ============================================================================================
    // PASS 3: ATMOSPHÄRE (Sonnen-, Mond- & Morgenstrahlen, Lampenkegel, Kaffeedampf, Staubkörner)
    // ============================================================================================

    private fun drawAtmosphere(visualTime: Float, camEye: Vec3) {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        GLES30.glUseProgram(shaders.alphaProgram)
        GLES30.glUniformMatrix4fv(shaders.alphaViewProj, 1, false, viewProjM, 0)
        GLES30.glUniform1f(shaders.alphaTime, visualTime)

        // 1. Weiche schräge Lichtstrahlen durch das Fenster (Tag-Gold, Abendrot, zartes Mondlicht, Morgen-Sonnenaufgang)
        val rayAlpha = curDaylight * 0.105f + curEvening * 0.055f + curNight * 0.032f + curDawn * 0.115f
        if (rayAlpha > 0.005f) {
            var rayR = mix(1.0f, 1.0f, curEvening)
            var rayG = mix(0.95f, 0.68f, curEvening)
            var rayB = mix(0.80f, 0.40f, curEvening)
            rayR = mix(rayR, 0.56f, curNight)
            rayG = mix(rayG, 0.68f, curNight)
            rayB = mix(rayB, 1.00f, curNight)
            rayR = mix(rayR, 1.00f, curDawn)
            rayG = mix(rayG, 0.74f, curDawn)
            rayB = mix(rayB, 0.48f, curDawn)

            GLES30.glUniform1i(shaders.alphaMode, 0)
            for (i in 0 until 2) {
                ms.reset()
                ms.translate(0.55f + i * 0.48f, 1.10f, -0.82f)
                ms.rotateY(-24f)
                ms.rotateZ(30f)
                ms.rotateX(16f)
                ms.scale(0.62f, 2.25f, 1f)
                GLES30.glUniformMatrix4fv(shaders.alphaModel, 1, false, ms.current, 0)
                GLES30.glUniform4f(shaders.alphaColor, rayR, rayG, rayB, rayAlpha)
                meshes.quadXY.draw()
            }
        }

        // 2. Warmer Lampenkegel unter dem Schirm
        if (curLampeAn > 0.02f) {
            GLES30.glUniform1i(shaders.alphaMode, 0)
            val mid = lerpVec3(lampBulbWorld, lampAimWorld, 0.45f)
            val toCam = camEye - mid
            val billYaw = atan2(toCam.x, toCam.z) * (180f / PI_F)
            ms.reset()
            ms.translate(mid.x, mid.y, mid.z)
            ms.rotateY(billYaw)
            ms.rotateZ(-14f)
            ms.scale(0.56f, 0.62f, 1f)
            GLES30.glUniformMatrix4fv(shaders.alphaModel, 1, false, ms.current, 0)
            GLES30.glUniform4f(shaders.alphaColor, 1.0f, 0.82f, 0.48f, curLampeAn * 0.16f)
            meshes.quadXY.draw()
        }

        // 3. Zarter Kaffeedampf über der Tasse (nachts abgeklungen, morgens frisch dampfend)
        val steamVis = (1f - curNight * 0.88f) * if (curMugInHand < 0.5f) 1f else 0f
        if (steamVis > 0.05f) {
            GLES30.glUniform1i(shaders.alphaMode, 1)
            val toCam = camEye - mugRestWorld
            val billYaw = atan2(toCam.x, toCam.z) * (180f / PI_F)
            for (i in 0 until 3) {
                val q = (visualTime * 0.40f + i / 3f) % 1f
                val rise = q * 0.24f
                val drift = sin(q * 6.28f + i * 2.1f) * 0.022f
                val steamA = sin(q * PI_F) * 0.21f * steamVis
                ms.reset()
                ms.translate(mugRestWorld.x + drift, mugRestWorld.y + 0.11f + rise, mugRestWorld.z)
                ms.rotateY(billYaw)
                val sz = 0.045f + q * 0.065f
                ms.scale(sz, sz * 1.45f, 1f)
                GLES30.glUniformMatrix4fv(shaders.alphaModel, 1, false, ms.current, 0)
                GLES30.glUniform4f(shaders.alphaColor, 0.96f, 0.95f, 0.92f, steamA)
                meshes.quadXY.draw()
            }
        }

        // 4. Feine Staubkörnchen im Sonnen- bzw. Morgenlicht
        val moteAlpha = clamp01(curDaylight + curDawn * 0.75f + curEvening * 0.25f) * 0.34f
        if (moteAlpha > 0.02f) {
            GLES30.glUniform1i(shaders.alphaMode, 2)
            for (i in 0 until 10) {
                val mx = -0.25f + (i * 0.27f % 1.85f) + sin(visualTime * 0.45f + i * 1.7f) * 0.08f
                val my = 0.55f + (i * 0.39f % 1.25f) + cos(visualTime * 0.35f + i * 2.3f) * 0.06f
                val mz = -1.05f + (i * 0.31f % 1.15f)
                val toCam = camEye - Vec3(mx, my, mz)
                val billYaw = atan2(toCam.x, toCam.z) * (180f / PI_F)
                val tw = 0.45f + 0.55f * sin(visualTime * 1.9f + i * 3.1f)
                ms.reset()
                ms.translate(mx, my, mz)
                ms.rotateY(billYaw)
                ms.scale(0.020f, 0.020f, 1f)
                GLES30.glUniformMatrix4fv(shaders.alphaModel, 1, false, ms.current, 0)
                GLES30.glUniform4f(shaders.alphaColor, 1.0f, 0.94f, 0.78f, moteAlpha * tw)
                meshes.quadXY.draw()
            }
        }

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    companion object {
        private const val SHADOW_SIZE = 1024

        // Schreibtisch-Weltposition und Ausrichtung (+26°: Bildschirm und Person ideal im 3/4-Blick)
        private const val DESK_X = 0.98f
        private const val DESK_Z = -0.38f
        private const val DESK_YAW = 26f

        // Stuhl-Weltpositionen: angedockt am Schreibtisch vs. leicht zurückgerollt & zum Raum geöffnet
        private const val CHAIR_WORLD_X = 0.468f
        private const val CHAIR_WORLD_Z = -0.108f
        private const val CHAIR_OPEN_X = 0.320f
        private const val CHAIR_OPEN_Z = -0.140f

        // Freier Einstiegspunkt direkt vor der offenen Sitzfläche (0.63 m vor Stuhlmitte, sichtbar großer Abstand)
        private const val CHAIR_ENTRY_X = 0.620f
        private const val CHAIR_ENTRY_Z = 0.420f

        // Gemütliches Katzenkörbchen links oben am Boden (klar getrennt von Pflanze, Tür und Laufweg)
        private const val CAT_BASKET_X = -1.04f
        private const val CAT_BASKET_Z = -1.24f
        private const val CAT_BASKET_Y = 0.068f

        // Freier Absprung- und Landepunkt sauber außerhalb des Korbrands (Abstand 0.55 m zur Korbmitte)
        private const val CAT_BASKET_OUT_X = -0.74f
        private const val CAT_BASKET_OUT_Z = -0.78f

        // Katzen-Absprungpunkt vor dem Schreibtisch & Landeplatz auf der vorderen rechten Tischecke
        private const val CAT_LAUNCH_X = 0.64f
        private const val CAT_LAUNCH_Z = 0.44f
        private const val CAT_DESK_X = 1.030f
        private const val CAT_DESK_Z = 0.041f

        // Natürliche Schrittlängen (Meter pro Halbschritt) für rutschfreie Bodenhaftung
        private const val HUMAN_STEP_LEN = 0.44f
        private const val CAT_STEP_LEN = 0.22f
    }
}
