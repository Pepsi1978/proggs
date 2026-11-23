package de.frank.aufgaben.ui.szene3d

import android.opengl.GLES30
import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Gesamtlänge eines nahtlosen 3D-Durchlaufs in Sekunden. */
internal const val SZENE_3D_ZYKLUS = 36f

/** Standbild-Zeitpunkt für reduzierte Bewegung (Sonne, Arbeiten am PC, Dampf, Katze im Raum). */
internal const val SZENE_3D_STANDBILD = 14.2f

/** Phasen-Bezeichnungen unterhalb der 3D-Szene, synchron zur Choreografie. */
internal val SZENE_3D_PHASEN: List<Pair<Float, String>> = listOf(
    0f to "Ankommen",
    9.0f to "Arbeiten",
    19.5f to "Katzenpause",
    28.5f to "Feierabend",
)

internal fun phaseFuerZeit(t: Float): Int =
    SZENE_3D_PHASEN.indexOfLast { t >= it.first }.coerceAtLeast(0)

/**
 * Vollständiger OpenGL-ES-3.0-Renderer für das animierte 3D-Arbeitszimmer:
 * - Pass 1: 1024×1024 Depth-Shadow-Map vom Haupt-Sonnenlicht
 * - Pass 2: PBR-Hauptszene (Raum, schwingende Tür, vertieftes Fenster mit Tageszeit-Himmel,
 *           Möbel, PC-Arbeitsplatz, animierter Mensch, animierte Katze)
 * - Pass 3: Transparente Atmosphäre (Sonnenstrahlen, Lampenkegel, Kaffeedampf, Staubkörner)
 */
internal class Szene3DRenderer {

    private val shaders = ShaderPrograms()
    private val meshes = MeshLibrary()
    private val ms = MatrixStack(28)

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
    private val zeroRgb = floatArrayOf(0f, 0f, 0f)
    private val whiteRgb = hexLinear(0xF6F3EE)
    private val offWhiteRgb = hexLinear(0xECE6DC)
    private val darkMetalRgb = hexLinear(0x23262C)
    private val brushedMetalRgb = hexLinear(0xB8BDC7)
    private val brassRgb = hexLinear(0xD2A95E)
    private val coffeeRgb = hexLinear(0x3B2314)
    private val soilRgb = hexLinear(0x3A2B20)
    private val eyeDarkRgb = hexLinear(0x161414)
    private val pinkNoseRgb = hexLinear(0xE89598)
    private val soleWhiteRgb = hexLinear(0xE6E2DC)

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

    // Dynamische Licht- und Objektpositionen pro Frame
    private var lampBulbWorld = Vec3(0.95f, 1.18f, -0.78f)
    private var lampAimWorld = Vec3(0.85f, 0.76f, -0.32f)
    private var monitorWorld = Vec3(1.14f, 1.02f, -0.48f)
    private var mugRestWorld = Vec3(0.76f, 0.763f, -0.64f)
    private var rightHandWorld = Vec3(0.65f, 0.85f, -0.25f)

    // Frame-Status für Schatten- und Hauptpass
    private var shadowPass = false
    private var curTime = 0f
    private var curAbend = 0f
    private var curLampeAn = 0f
    private var curScreenOn = 0f
    private var curTyped = 0f
    private var curChecked = 0f
    private var curDoorAngle = 0f
    private var curChairSwivel = -32f
    private var curChairDrop = 0f
    private var curMugInHand = 0f
    private var curMugTilt = 0f

    // Menschen- und Katzen-Weltposition für Kontaktschatten
    private var humanContactX = -2.4f
    private var humanContactZ = -0.25f
    private var humanContactStr = 0f
    private var catContactX = -2.6f
    private var catContactZ = -0.40f
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
        Matrix.perspectiveM(projM, 0, 33.5f, aspect, 0.15f, 18f)
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

    fun drawFrame(t: Float, p: Palette3D) {
        curTime = t

        // ---- 1. Story-Parameter aus t (0..36s) berechnen ----
        // Tageszeit: heller Morgen -> goldener Nachmittag -> gemütlicher Abend -> sanftes Erwachen vor 36s
        val abendHoch = smootherPhase(t, 14.0f, 15.5f)
        val abendReset = smootherPhase(t, 33.2f, 2.8f)
        curAbend = abendHoch * (1f - abendReset)

        // Tür schwingt beim Hereinkommen (0.3..1.7s -> auf, 4.8..6.4s -> zu) und Hinausgehen (32.2..33.3s -> auf, 34.8..35.8s -> zu)
        val doorEnter = smootherPhase(t, 0.25f, 1.35f) * (1f - smootherPhase(t, 4.9f, 1.5f))
        val doorExit = smootherPhase(t, 32.1f, 1.15f) * (1f - smootherPhase(t, 34.9f, 0.95f))
        curDoorAngle = max(doorEnter, doorExit) * 72f

        // Stuhl schwenkt beim Hinsetzen (7.0..8.3s) zum Tisch und beim Aufstehen (30.5..31.7s) leicht zurück
        val sitIn = smootherPhase(t, 7.0f, 1.25f)
        val standOut = smootherPhase(t, 30.5f, 1.15f)
        val sitFactor = sitIn * (1f - standOut)
        curChairSwivel = mix(-34f, 0f, sitFactor) + sin(t * 0.9f) * 1.4f * sitFactor
        // Sanftes Einfedern beim Hinsetzen
        val bounce = sin(phase(t, 7.2f, 0.9f) * PI_F) * 0.024f
        curChairDrop = sitFactor * 0.014f + bounce

        // Monitor & Aufgabenliste
        curScreenOn = smoothPhase(t, 8.35f, 0.55f) * (1f - smoothPhase(t, 30.25f, 0.45f))
        curTyped = when {
            t < 9.2f -> 0f
            t < 18.8f -> ((t - 9.2f) / 2.2f).coerceIn(0f, 4f)
            t < 30.4f -> 4f
            else -> 0f
        }
        curChecked = when {
            t < 12.2f -> 0f
            t < 19.0f -> ((t - 12.2f) / 3.0f).coerceIn(0f, 2f)
            t < 26.2f -> 2f
            t < 28.4f -> 2f + ((t - 26.2f) / 1.0f).coerceIn(0f, 2f)
            t < 30.4f -> 4f
            else -> 0f
        }

        // Schreibtischlampe: bei 16.9s eingeschaltet, bei 30.2s ausgeschaltet
        curLampeAn = smoothPhase(t, 16.9f, 0.35f) * (1f - smoothPhase(t, 30.15f, 0.35f))

        // Kaffeetasse in der Hand (13.9..15.7s)
        curMugInHand = if (t in 13.85f..15.75f) 1f else 0f
        curMugTilt = sin(phase(t, 14.25f, 1.05f) * PI_F) * 28f

        // ---- 2. Filmische 3D-Kamera mit sanfter Fahrt ----
        val camTrack = smootherPhase(t, 1.2f, 7.2f) * (1f - smootherPhase(t, 30.8f, 4.8f))
        val catFocus = smootherPhase(t, 19.6f, 2.2f) * (1f - smootherPhase(t, 28.2f, 2.2f))
        val breatheX = sin(t * TAU_F / SZENE_3D_ZYKLUS * 2f) * 0.06f
        val breatheY = cos(t * TAU_F / SZENE_3D_ZYKLUS * 2f) * 0.025f

        val baseEye = lerpVec3(Vec3(-0.78f, 1.76f, 2.72f), Vec3(-0.26f, 1.60f, 2.38f), camTrack)
        val camEye = lerpVec3(baseEye, Vec3(-0.08f, 1.54f, 2.24f), catFocus) + Vec3(breatheX, breatheY, 0f)
        val baseTarget = lerpVec3(Vec3(-0.22f, 0.84f, -0.26f), Vec3(0.42f, 0.89f, -0.22f), camTrack)
        val camTarget = lerpVec3(baseTarget, Vec3(0.54f, 0.86f, -0.12f), catFocus)

        Matrix.setLookAtM(
            viewM, 0,
            camEye.x, camEye.y, camEye.z,
            camTarget.x, camTarget.y, camTarget.z,
            0f, 1f, 0f,
        )
        Matrix.multiplyMM(viewProjM, 0, projM, 0, viewM, 0)

        // ---- 3. Sonnenrichtung & Schattenkamera ----
        // Sonne scheint schräg durch das große Fenster direkt über Schreibtisch, Mensch, Teppich und Katze
        val sunDir = Vec3(
            mix(0.32f, 0.46f, curAbend),
            mix(0.46f, 0.28f, curAbend),
            mix(-0.82f, -0.84f, curAbend),
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
        val humanPose = evaluateHumanPose(t)
        val catPose = evaluateCatPose(t)
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
        GLES30.glClearColor(0.06f, 0.05f, 0.06f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(shaders.mainProgram)
        GLES30.glUniformMatrix4fv(shaders.uViewProj, 1, false, viewProjM, 0)
        GLES30.glUniformMatrix4fv(shaders.uLightViewProj, 1, false, lightViewProjM, 0)
        GLES30.glUniform3f(shaders.uCameraPos, camEye.x, camEye.y, camEye.z)
        GLES30.glUniform1f(shaders.uTime, t)
        GLES30.glUniform1f(shaders.uExposure, p.belichtung)

        // Lichtfarben abhängig von Tageszeit
        val sunIntensity = mix(2.55f, 0.45f, curAbend)
        val sunCol = lerpRgb(floatArrayOf(1.0f, 0.94f, 0.82f), floatArrayOf(1.0f, 0.44f, 0.16f), curAbend, tmpCol1)
        GLES30.glUniform3f(shaders.uSunDir, sunDir.x, sunDir.y, sunDir.z)
        GLES30.glUniform3f(shaders.uSunColor, sunCol[0] * sunIntensity, sunCol[1] * sunIntensity, sunCol[2] * sunIntensity)

        // Hemisphären-IBL (Himmel oben + warmer Holz-Bounce unten)
        val skyI = mix(0.76f, 0.34f, curAbend)
        val skyCol = lerpRgb(floatArrayOf(0.88f, 0.92f, 1.0f), floatArrayOf(0.38f, 0.42f, 0.68f), curAbend, tmpCol2)
        GLES30.glUniform3f(shaders.uSkyColor, skyCol[0] * skyI, skyCol[1] * skyI, skyCol[2] * skyI)
        val gndI = mix(0.48f, 0.25f, curAbend)
        GLES30.glUniform3f(shaders.uGroundColor, 0.76f * gndI, 0.59f * gndI, 0.44f * gndI)

        // Schreibtischlampe Spotlight
        val lampDir = (lampAimWorld - lampBulbWorld).normalized()
        val lampStr = curLampeAn * 3.8f
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

        // Warmes Flurlicht durch geöffnete Tür
        val doorOpen = (curDoorAngle / 72f).coerceIn(0f, 1f)
        val doorStr = doorOpen * 1.45f
        GLES30.glUniform3f(shaders.uDoorGlowPos, -2.05f, 1.15f, -0.36f)
        GLES30.glUniform3f(shaders.uDoorGlowColor, p.flurLicht[0] * doorStr, p.flurLicht[1] * doorStr, p.flurLicht[2] * doorStr)

        // Kontaktschatten (Mensch, Stuhl, Katze, Schreibtisch)
        GLES30.glUniform4f(shaders.uContact0, humanContactX, humanContactZ, 0.36f, humanContactStr)
        GLES30.glUniform4f(shaders.uContact1, CHAIR_WORLD_X, CHAIR_WORLD_Z, 0.42f, 0.32f)
        GLES30.glUniform4f(shaders.uContact2, catContactX, catContactZ, 0.24f, catContactStr)
        GLES30.glUniform4f(shaders.uContact3, DESK_X + 0.05f, DESK_Z, 0.85f, 0.26f)

        // Schatten-Textur an Slot 0 binden
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex)
        GLES30.glUniform1i(shaders.uShadowMap, 0)

        drawWorldGeometry(p, humanPose, catPose, forShadow = false)

        // ---- PASS 3: Transparente Atmosphäre (Sonnenstrahlen, Lampenkegel, Dampf, Staub) ----
        drawAtmosphere(p, t, camEye)

        GLES30.glBindVertexArray(0)
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

        // 1. Raum (Boden, Wände, vertieftes Fenster, schwingende Tür, Deko)
        drawRoom(p, forShadow)

        // 2. Schreibtisch, PC-Setup, Lampe & Tasse
        drawWorkstation(p, forShadow)

        // 3. Ergonomischer 3D-Drehstuhl
        drawChair(p)

        // 4. Animierter 3D-Mensch (merkt sich rechte Hand für Kaffeetasse)
        drawHuman(p, human)

        // 5. Kaffeetasse (auf dem Tisch oder beim Trinken in der Hand)
        drawCoffeeMug(p)

        // 6. Animierte 3D-Katze
        drawCat(p, cat)
    }

    private fun drawRoom(p: Palette3D, forShadow: Boolean) {
        // Holzdielenboden (wirft selbst keinen Schatten)
        if (!forShadow) {
            ms.scoped {
                ms.translate(0.0f, -0.05f, 0.55f)
                ms.scale(4.8f, 0.10f, 5.2f)
                drawMesh(meshes.box, p.bodenHolz, rough = 0.45f, pattern = 1, alt = p.bodenHolzAlt)
            }
            // Helle Decke über dem Raum (verhindert dunkle Kanten bei weiten Displays)
            ms.scoped {
                ms.translate(0.0f, 2.81f, 0.55f)
                ms.scale(4.8f, 0.10f, 5.2f)
                drawMesh(meshes.box, p.fussleiste, rough = 0.90f)
            }
        }

        // Gewebter Teppich im linken/mittleren Bereich
        ms.scoped {
            ms.translate(-0.78f, 0.009f, 0.28f)
            ms.scale(1.42f, 0.018f, 1.02f)
            drawMesh(meshes.beveledBox, p.teppichBasis, rough = 0.92f, pattern = 3, alt = p.teppichRand)
        }

        // ---- Rückwand mit echter 3D-Fensteröffnung (x: 0.25..1.85, y: 0.85..2.15) ----
        // Links neben Fenster
        ms.scoped {
            ms.translate(-1.05f, 1.38f, -1.76f)
            ms.scale(2.60f, 2.76f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        // Rechts neben Fenster
        ms.scoped {
            ms.translate(2.25f, 1.38f, -1.76f)
            ms.scale(0.80f, 2.76f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        // Unter dem Fenster
        ms.scoped {
            ms.translate(1.05f, 0.425f, -1.76f)
            ms.scale(1.60f, 0.85f, 0.22f)
            drawMesh(meshes.box, p.wandHinten, rough = 0.86f, pattern = 4)
        }
        // Über dem Fenster
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
            // Dynamischer Himmel & Stadtsilhouette hinter der Fensteröffnung
            ms.scoped {
                ms.translate(1.05f, 1.50f, -2.06f)
                ms.scale(1.90f, 1.52f, 1f)
                drawMesh(
                    meshes.quadXY, p.himmelTagOben, rough = 1f, pattern = 8, alt = p.himmelTagUnten,
                    p0 = curAbend, p1 = 0f, p2 = 0f,
                )
            }
        }
        // Fensterbank (ragt räumlich in den Raum)
        ms.scoped {
            ms.translate(1.05f, 0.845f, -1.59f)
            ms.scale(1.76f, 0.048f, 0.34f)
            drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.42f)
        }
        // Äußerer Fensterrahmen (4 Leisten) & Kreuzsprossen in der Laibung
        val frameZ = -1.77f
        ms.scoped { ms.translate(1.05f, 2.13f, frameZ); ms.scale(1.62f, 0.055f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.05f, 0.88f, frameZ); ms.scale(1.62f, 0.055f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(0.27f, 1.50f, frameZ); ms.scale(0.055f, 1.30f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.83f, 1.50f, frameZ); ms.scale(0.055f, 1.30f, 0.06f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        // Mittelsprosse vertikal & horizontal
        ms.scoped { ms.translate(1.05f, 1.50f, frameZ); ms.scale(0.036f, 1.24f, 0.045f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }
        ms.scoped { ms.translate(1.05f, 1.62f, frameZ); ms.scale(1.56f, 0.032f, 0.045f); drawMesh(meshes.box, whiteRgb, rough = 0.38f) }

        // Gardinenstange + sanft schwingende 3D-Faltvorhänge
        ms.scoped {
            ms.translate(1.03f, 2.24f, -1.55f)
            ms.rotateZ(90f)
            ms.scale(0.026f, 1.98f, 0.026f)
            drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.32f, metal = 1f)
        }
        val sway = sin(curTime * 0.9f) * 1.6f
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

        // ---- Rechte Wand (schließt den Raum rechts vollständig, auch auf breiten Bildschirmen) ----
        ms.scoped {
            ms.translate(2.16f, 1.38f, 0.65f)
            ms.scale(0.22f, 2.76f, 5.04f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        // Fußleiste rechte Wand
        ms.scoped {
            ms.translate(2.035f, 0.055f, 0.65f)
            ms.scale(0.03f, 0.11f, 4.60f)
            drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f)
        }
        // Gerahmtes 3D-Wandbild an der rechten Wand (blickt nach -X in den Raum)
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
        // Kleines schwebendes Deko-Regal rechts mit Büchern und kleiner Grünpflanze
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
            // Drei Deko-Bücher links auf dem kleinen Regal
            for (bi in 0 until 3) {
                val bh = 0.17f + bi * 0.022f
                ms.scoped {
                    ms.translate(-0.18f + bi * 0.052f, 0.018f + bh * 0.5f, 0f)
                    ms.scale(0.042f, bh, 0.14f)
                    drawMesh(meshes.beveledBox, bookColors[(bi + 2) % bookColors.size], rough = 0.62f)
                }
            }
            // Kleine Pflanze rechts auf dem Regal
            ms.scoped {
                ms.translate(0.14f, 0.018f, 0f)
                drawPottedPlant(p, potScale = 0.10f, leafCount = 5, leafScale = 0.16f, phaseOffset = 2.6f)
            }
        }
        // Große 3D-Zimmerpflanze auf dem Boden vor der rechten Wand
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
            ms.translate(-2.26f, 1.38f, 1.56f)
            ms.scale(0.22f, 2.76f, 2.96f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        ms.scoped {
            ms.translate(-2.26f, 2.40f, -0.37f)
            ms.scale(0.22f, 0.72f, 0.90f)
            drawMesh(meshes.box, p.wandLinks, rough = 0.86f, pattern = 4)
        }
        // Fußleiste linke Wand (vor und hinter Tür)
        ms.scoped { ms.translate(-2.135f, 0.055f, -1.24f); ms.scale(0.03f, 0.11f, 0.84f); drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f) }
        ms.scoped { ms.translate(-2.135f, 0.055f, 1.56f); ms.scale(0.03f, 0.11f, 2.96f); drawMesh(meshes.beveledBox, p.fussleiste, rough = 0.50f) }

        // Warmer Flur-Hintergrund hinter der Tür (leuchtet angenehm wenn Tür aufgeht)
        if (!forShadow) {
            val hallGlow = 0.52f + (curDoorAngle / 72f) * 0.46f
            ms.scoped {
                ms.translate(-3.15f, 1.05f, -0.37f)
                ms.scale(0.10f, 2.30f, 1.60f)
                drawMesh(
                    meshes.box, p.flurLicht, rough = 0.9f,
                    emissive = floatArrayOf(p.flurLicht[0] * hallGlow, p.flurLicht[1] * hallGlow, p.flurLicht[2] * hallGlow),
                )
            }
        }
        // Türzarge (3D-Rahmen)
        ms.scoped { ms.translate(-2.13f, 1.02f, -0.84f); ms.scale(0.08f, 2.08f, 0.07f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }
        ms.scoped { ms.translate(-2.13f, 1.02f, 0.10f); ms.scale(0.08f, 2.08f, 0.07f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }
        ms.scoped { ms.translate(-2.13f, 2.06f, -0.37f); ms.scale(0.08f, 0.07f, 1.01f); drawMesh(meshes.beveledBox, p.tuerRahmen, rough = 0.46f) }

        // Real schwingendes 3D-Türblatt (Scharnier bei x = -2.14, z = -0.80)
        ms.scoped {
            ms.translate(-2.14f, 0f, -0.80f)
            ms.rotateY(curDoorAngle)
            // Hauptblatt
            ms.scoped {
                ms.translate(0.025f, 1.01f, 0.44f)
                ms.scale(0.048f, 2.01f, 0.87f)
                drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.44f, pattern = 2, alt = p.bodenHolzAlt)
            }
            // Zwei dekorative 3D-Türfüllungen (Kassetten)
            ms.scoped { ms.translate(0.052f, 1.46f, 0.44f); ms.scale(0.015f, 0.68f, 0.56f); drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.48f) }
            ms.scoped { ms.translate(0.052f, 0.58f, 0.44f); ms.scale(0.015f, 0.62f, 0.56f); drawMesh(meshes.beveledBox, p.tuerHolz, rough = 0.48f) }
            // Messing-Türklinke (innen und außen)
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
        // Zwei Wandhalterungen
        for (bx in floatArrayOf(-1.55f, -0.73f)) {
            ms.scoped { ms.translate(bx, shelfY - 0.065f, -1.58f); ms.scale(0.025f, 0.11f, 0.12f); drawMesh(meshes.box, darkMetalRgb, rough = 0.35f, metal = 1f) }
        }
        // 8 einzelne 3D-Bücher (eines leicht angelehnt)
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
        // Kleine Regal-Pflanze rechts auf dem Brett
        ms.scoped {
            ms.translate(-0.70f, shelfY + 0.021f, -1.50f)
            drawPottedPlant(p, potScale = 0.11f, leafCount = 5, leafScale = 0.17f, phaseOffset = 1.4f)
        }

        // ---- Gerahmtes 3D-Wandbild ----
        ms.scoped {
            ms.translate(-0.18f, 1.66f, -1.62f)
            ms.scoped { ms.scale(0.46f, 0.56f, 0.036f); drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.46f) }
            if (!forShadow) {
                ms.scoped {
                    ms.translate(0f, 0f, 0.020f)
                    ms.scale(0.38f, 0.48f, 1f)
                    drawMesh(meshes.quadXY, p.akzent, rough = 0.55f, pattern = 9, alt = p.akzentHell)
                }
            }
        }

        // ---- 3D-Wanduhr mit echten drehenden 3D-Zeigern ----
        ms.scoped {
            ms.translate(-1.14f, 2.07f, -1.61f)
            // Gehäuse
            ms.scoped { ms.rotateX(90f); ms.scale(0.28f, 0.045f, 0.28f); drawMesh(meshes.cylinder, p.tischHolz, rough = 0.40f) }
            // Zifferblatt
            if (!forShadow) {
                ms.scoped {
                    ms.translate(0f, 0f, 0.024f)
                    ms.scale(0.24f, 0.24f, 1f)
                    drawMesh(meshes.quadXY, whiteRgb, rough = 0.45f, pattern = 10, alt = darkMetalRgb)
                }
            }
            // Minutenzeiger & Stundenzeiger (drehen sich passend zum Tagesablauf)
            val minuteDeg = -(curTime / SZENE_3D_ZYKLUS) * 360f * 2f
            val hourDeg = -45f - (curTime / SZENE_3D_ZYKLUS) * 180f
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
            ms.translate(-1.74f, 0f, -1.24f)
            drawPottedPlant(p, potScale = 0.28f, leafCount = 8, leafScale = 0.46f, phaseOffset = 0f)
        }
    }

    private fun drawPottedPlant(p: Palette3D, potScale: Float, leafCount: Int, leafScale: Float, phaseOffset: Float) {
        // Konischer Terrakotta-Topf (unten schmaler -> umgedrehter Frustum)
        ms.scoped {
            ms.translate(0f, potScale * 0.5f, 0f)
            ms.rotateX(180f)
            ms.scale(potScale * 0.92f, potScale, potScale * 0.92f)
            drawMesh(meshes.frustum, p.topf, rough = 0.72f)
        }
        // Topfrand oben
        ms.scoped {
            ms.translate(0f, potScale * 0.95f, 0f)
            ms.scale(potScale * 0.98f, potScale * 0.14f, potScale * 0.98f)
            drawMesh(meshes.cylinder, p.topf, rough = 0.68f)
        }
        // Erde
        ms.scoped {
            ms.translate(0f, potScale * 0.96f, 0f)
            ms.scale(potScale * 0.84f, potScale * 0.05f, potScale * 0.84f)
            drawMesh(meshes.cylinder, soilRgb, rough = 0.95f)
        }
        // 3D-gebogene Blätter im Fächer
        for (i in 0 until leafCount) {
            val yaw = (i.toFloat() / leafCount) * 360f + 18f
            val pitch = -28f - (i % 3) * 14f + sin(curTime * 1.15f + i * 0.9f + phaseOffset) * 2.5f
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

            // ---- Schreibtischplatte (abgeschrägte Eiche) ----
            ms.scoped {
                ms.translate(0f, 0.74f, 0f)
                ms.scale(0.76f, 0.046f, 1.42f)
                drawMesh(meshes.beveledBox, p.tischHolz, rough = 0.38f, pattern = 2, alt = p.bodenHolzAlt)
            }

            // Vordere Metallbeine (+Z)
            for (lx in floatArrayOf(-0.30f, 0.30f)) {
                ms.scoped {
                    ms.translate(lx, 0.36f, 0.60f)
                    ms.scale(0.042f, 0.72f, 0.042f)
                    drawMesh(meshes.beveledBox, p.tischGestell, rough = 0.36f, metal = 1f)
                }
            }
            // Querstrebe vorne
            ms.scoped { ms.translate(0f, 0.16f, 0.60f); ms.scale(0.60f, 0.032f, 0.032f); drawMesh(meshes.box, p.tischGestell, rough = 0.36f, metal = 1f) }
            // Hintere Querstrebe für Stabilität
            ms.scoped { ms.translate(0.28f, 0.60f, 0f); ms.scale(0.03f, 0.06f, 1.24f); drawMesh(meshes.box, p.tischGestell, rough = 0.40f, metal = 1f) }

            // 3-Schubladen-Rollcontainer unter der hinteren Tischseite (-Z)
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
                // Metall-Griffleiste
                ms.scoped {
                    ms.translate(-0.326f, dy + 0.02f, -0.44f)
                    ms.scale(0.018f, 0.018f, 0.12f)
                    drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.24f, metal = 1f)
                }
            }

            // ---- Moderner PC-Tower neben/hinter dem Schreibtisch ----
            ms.scoped {
                ms.translate(0.08f, 0.24f, -0.84f)
                ms.scale(0.44f, 0.48f, 0.22f)
                drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.34f, metal = 0.9f)
                // Feine LED-Akzentlinie vorne am Gehäuse
                val led = curScreenOn * 1.1f + 0.12f
                ms.scoped {
                    ms.translate(-0.505f, 0f, 0.32f)
                    ms.scale(0.02f, 0.72f, 0.06f)
                    drawMesh(
                        meshes.box, p.akzentHell, rough = 0.2f,
                        emissive = floatArrayOf(p.akzentHell[0] * led, p.akzentHell[1] * led, p.akzentHell[2] * led),
                    )
                }
            }

            // ---- 3D-Monitor auf Aluminium-Standfuß ----
            ms.scoped {
                ms.translate(0.18f, 0.763f, -0.02f)
                // Standplatte
                ms.scoped { ms.translate(0f, 0.009f, 0f); ms.scale(0.18f, 0.018f, 0.26f); drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.28f, metal = 1f) }
                // Leicht geneigter Standhals
                ms.scoped {
                    ms.translate(0.04f, 0.15f, 0f)
                    ms.rotateZ(-10f)
                    ms.scale(0.032f, 0.30f, 0.075f)
                    drawMesh(meshes.beveledBox, brushedMetalRgb, rough = 0.28f, metal = 1f)
                }
                // Monitor-Gehäuse (schlanker moderner 27"-Monitor, leicht nach oben geneigt)
                ms.scoped {
                    ms.translate(-0.02f, 0.31f, 0f)
                    ms.rotateZ(5f)
                    monitorWorld = ms.transformPoint(-0.03f, 0f, 0f)
                    // Rück- und Rahmengehäuse
                    ms.scoped { ms.scale(0.038f, 0.39f, 0.66f); drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.30f) }
                    // Leuchtendes Display auf der -X-Seite (dreht +Z-Quad nach -X via rotateY(-90))
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

            // ---- Mechanische 3D-Tastatur ----
            ms.scoped {
                ms.translate(-0.15f, 0.774f, -0.02f)
                ms.rotateZ(-4f)
                ms.scoped { ms.scale(0.145f, 0.020f, 0.38f); drawMesh(meshes.beveledBox, darkMetalRgb, rough = 0.38f) }
                // Tastenfeld oben (Quad um -90°X -> zeigt nach +Y, rotiert um Y damit Breite entlang Z läuft)
                if (!forShadow) {
                    ms.scoped {
                        ms.translate(0f, 0.011f, 0f)
                        ms.rotateY(-90f)
                        ms.rotateX(-90f)
                        ms.scale(0.35f, 0.12f, 1f)
                        drawMesh(
                            meshes.quadXY, hexLinear(0x363A44), rough = 0.42f, pattern = 11, alt = p.akzentHell,
                            p0 = curScreenOn,
                        )
                    }
                }
            }

            // ---- Mauspad & 3D-Maus ----
            ms.scoped {
                ms.translate(-0.14f, 0.766f, 0.30f)
                ms.scoped { ms.scale(0.22f, 0.006f, 0.20f); drawMesh(meshes.beveledBox, hexLinear(0x282B33), rough = 0.88f) }
                val mouseShift = if (curScreenOn > 0.5f && curMugInHand < 0.5f) sin(curTime * 2.1f) * 0.014f else 0f
                ms.scoped {
                    ms.translate(mouseShift, 0.018f, mouseShift * 0.5f)
                    ms.scale(0.095f, 0.032f, 0.058f)
                    drawMesh(meshes.sphere, offWhiteRgb, rough = 0.30f)
                }
            }

            // ---- Kleines Notizbuch & Stift ----
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

            // ---- Kaffeetassen-Ruheposition merken ----
            mugRestWorld = ms.transformPoint(-0.11f, 0.763f, -0.34f)
            lampAimWorld = ms.transformPoint(-0.08f, 0.76f, -0.10f)

            // ---- Gelenkige 3D-Architektenlampe ----
            ms.scoped {
                ms.translate(0.15f, 0.763f, -0.50f)
                // Schwerer runder Sockel
                ms.scoped { ms.translate(0f, 0.015f, 0f); ms.scale(0.16f, 0.030f, 0.16f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                // Unterer Arm (schräg nach hinten-oben)
                ms.translate(0f, 0.03f, 0f)
                ms.rotateY(-38f)
                ms.rotateZ(-18f)
                ms.scoped { ms.translate(0f, 0.16f, 0f); ms.scale(0.018f, 0.32f, 0.018f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                // Ellbogen-Gelenk
                ms.translate(0f, 0.32f, 0f)
                ms.scoped { ms.scale(0.036f); drawMesh(meshes.sphere, brassRgb, rough = 0.24f, metal = 1f) }
                // Oberer Arm (reicht nach vorne über den Tisch)
                ms.rotateZ(64f)
                ms.scoped { ms.translate(0f, 0.145f, 0f); ms.scale(0.016f, 0.29f, 0.016f); drawMesh(meshes.cylinder, darkMetalRgb, rough = 0.34f, metal = 1f) }
                // Schirm-Gelenk & Kegel-Lampenschirm
                ms.translate(0f, 0.29f, 0f)
                ms.rotateZ(-18f)
                ms.scoped { ms.scale(0.030f); drawMesh(meshes.sphere, brassRgb, rough = 0.24f, metal = 1f) }
                ms.scoped {
                    ms.translate(0f, -0.045f, 0f)
                    ms.scale(0.17f, 0.13f, 0.17f)
                    drawMesh(meshes.lampShade, p.lampeSchirm, rough = 0.36f)
                }
                // Leuchtende Glühbirne im Schirm
                lampBulbWorld = ms.transformPoint(0f, -0.075f, 0f)
                val bulbGlow = curLampeAn * 3.5f
                ms.scoped {
                    ms.translate(0f, -0.072f, 0f)
                    ms.scale(0.048f)
                    drawMesh(
                        meshes.sphere, whiteRgb, rough = 0.15f,
                        emissive = floatArrayOf(1.0f * bulbGlow, 0.78f * bulbGlow, 0.42f * bulbGlow),
                    )
                }
            }
        }
    }

    private fun drawChair(p: Palette3D) {
        ms.scoped {
            ms.translate(CHAIR_WORLD_X, 0f, CHAIR_WORLD_Z)
            ms.rotateY(DESK_YAW)

            // 5-Stern-Fußkreuz mit Rollen (bleibt ruhig auf dem Boden)
            for (i in 0 until 5) {
                val ang = i * 72f + 18f
                ms.scoped {
                    ms.rotateY(ang)
                    // Ausleger
                    ms.scoped {
                        ms.translate(0.135f, 0.075f, 0f)
                        ms.rotateZ(-10f)
                        ms.scale(0.27f, 0.026f, 0.032f)
                        drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.32f, metal = 1f)
                    }
                    // Rolle am Boden
                    ms.scoped {
                        ms.translate(0.255f, 0.026f, 0f)
                        ms.scale(0.048f)
                        drawMesh(meshes.sphere, darkMetalRgb, rough = 0.42f)
                    }
                }
            }
            // Gasdruckfeder-Zylinder
            ms.scoped {
                ms.translate(0f, 0.25f, 0f)
                ms.scale(0.056f, 0.36f, 0.056f)
                drawMesh(meshes.cylinder, brushedMetalRgb, rough = 0.22f, metal = 1f)
            }

            // ---- Drehbarer Oberteil (Sitz, Lehne, Armlehnen) ----
            ms.translate(0f, 0.44f - curChairDrop, 0f)
            ms.rotateY(curChairSwivel)

            // Konturiertes Sitzpolster
            ms.scoped {
                ms.translate(0f, 0.03f, 0f)
                ms.scale(0.47f, 0.075f, 0.47f)
                drawMesh(meshes.beveledBox, p.stuhlStoff, rough = 0.80f, pattern = 6)
            }
            // Rückenlehnen-Träger hinten (-X)
            ms.scoped {
                ms.translate(-0.22f, 0.14f, 0f)
                ms.rotateZ(12f)
                ms.scale(0.04f, 0.24f, 0.08f)
                drawMesh(meshes.beveledBox, p.stuhlGestell, rough = 0.34f, metal = 1f)
            }
            // Ergonomisch geschwungene 3D-Rückenlehne
            ms.scoped {
                ms.translate(-0.245f, 0.35f, 0f)
                ms.rotateY(90f) // Wölbung zeigt nach +X (zum Sitzenden)
                ms.rotateX(-7f) // leichte ergonomische Neigung nach hinten
                ms.scale(0.46f, 0.48f, 0.28f)
                drawMesh(meshes.chairBack, p.stuhlStoff, rough = 0.80f, pattern = 6)
            }
            // Zwei Armlehnen (+Z und -Z)
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
            // Tassenkörper (Keramik weiß)
            ms.scoped { ms.translate(0f, 0.048f, 0f); ms.scale(0.078f, 0.096f, 0.078f); drawMesh(meshes.cylinder, whiteRgb, rough = 0.24f) }
            // Kaffee-Spiegel innen
            ms.scoped { ms.translate(0f, 0.088f, 0f); ms.scale(0.066f, 0.012f, 0.066f); drawMesh(meshes.cylinder, coffeeRgb, rough = 0.14f) }
            // Henkel (Torus an der Seite)
            ms.scoped {
                ms.translate(0.042f, 0.048f, 0f)
                ms.scale(0.056f, 0.062f, 0.056f)
                drawMesh(meshes.torus, whiteRgb, rough = 0.24f)
            }
            // Dezenter Farbstreifen im Design-Akzent
            ms.scoped { ms.translate(0f, 0.025f, 0f); ms.scale(0.080f, 0.014f, 0.080f); drawMesh(meshes.cylinder, p.akzent, rough = 0.30f) }
        }
    }

    // ============================================================================================
    // ARTIKULIERTER 3D-MENSCH (Skelett-Hierarchie + natürliche Bewegungen)
    // ============================================================================================

    private data class HumanPose(
        val x: Float,
        val z: Float,
        val yawDeg: Float,
        val pelvisY: Float,
        val torsoLean: Float,
        val torsoTwist: Float,
        val headYaw: Float,
        val headPitch: Float,
        val thighL: Float,
        val kneeL: Float,
        val thighR: Float,
        val kneeR: Float,
        val shL: Float,
        val shLOut: Float,
        val elbL: Float,
        val shR: Float,
        val shROut: Float,
        val elbR: Float,
        val blink: Float,
    )

    private fun evaluateHumanPose(t: Float): HumanPose {
        // Natürliches Blinzeln alle ~3.8 Sekunden
        val blinkCycle = (t + 0.9f) % 3.8f
        val blink = if (blinkCycle < 0.14f) sin(blinkCycle / 0.14f * PI_F) else 0f

        // Hilfsfunktion: natürlicher Gehzyklus entlang Pfad
        fun walkPose(wx: Float, wz: Float, yaw: Float, phaseRad: Float, strideAmp: Float, headYaw: Float = 0f, headPitch: Float = 4f): HumanPose {
            val s = sin(phaseRad) * strideAmp
            val c = cos(phaseRad)
            val bob = kotlin.math.abs(cos(phaseRad)) * 0.032f * strideAmp
            val thighL = s * 32f
            val thighR = -s * 32f
            val kneeL = max(0f, -c) * 44f * strideAmp + 6f
            val kneeR = max(0f, c) * 44f * strideAmp + 6f
            return HumanPose(
                x = wx, z = wz, yawDeg = yaw,
                pelvisY = 0.84f + bob,
                torsoLean = 5f * strideAmp,
                torsoTwist = -s * 7f,
                headYaw = headYaw,
                headPitch = headPitch,
                thighL = thighL, kneeL = kneeL,
                thighR = thighR, kneeR = kneeR,
                shL = -s * 26f, shLOut = 6f, elbL = 16f + max(0f, -s) * 14f,
                shR = s * 26f, shROut = 6f, elbR = 16f + max(0f, s) * 14f,
                blink = blink,
            )
        }

        // Standard-Sitzhaltung am Schreibtisch (lokale +X = zum Monitor)
        fun sitPose(
            lean: Float = 7f, twist: Float = 0f,
            headYaw: Float = 0f, headPitch: Float = 9f,
            shL: Float = 46f, shLOut: Float = 10f, elbL: Float = 66f,
            shR: Float = 46f, shROut: Float = 10f, elbR: Float = 66f,
        ) = HumanPose(
            x = CHAIR_WORLD_X - 0.02f, z = CHAIR_WORLD_Z + 0.01f,
            yawDeg = DESK_YAW + curChairSwivel * 0.85f,
            pelvisY = 0.555f - curChairDrop,
            torsoLean = lean, torsoTwist = twist,
            headYaw = headYaw, headPitch = headPitch,
            thighL = 86f, kneeL = 84f,
            thighR = 86f, kneeR = 84f,
            shL = shL, shLOut = shLOut, elbL = elbL,
            shR = shR, shROut = shROut, elbR = elbR,
            blink = blink,
        )

        fun lerpPose(a: HumanPose, b: HumanPose, u: Float): HumanPose {
            val k = smoother(u)
            return HumanPose(
                mix(a.x, b.x, k), mix(a.z, b.z, k), mix(a.yawDeg, b.yawDeg, k),
                mix(a.pelvisY, b.pelvisY, k), mix(a.torsoLean, b.torsoLean, k), mix(a.torsoTwist, b.torsoTwist, k),
                mix(a.headYaw, b.headYaw, k), mix(a.headPitch, b.headPitch, k),
                mix(a.thighL, b.thighL, k), mix(a.kneeL, b.kneeL, k),
                mix(a.thighR, b.thighR, k), mix(a.kneeR, b.kneeR, k),
                mix(a.shL, b.shL, k), mix(a.shLOut, b.shLOut, k), mix(a.elbL, b.elbL, k),
                mix(a.shR, b.shR, k), mix(a.shROut, b.shROut, k), mix(a.elbR, b.elbR, k),
                blink,
            )
        }

        // Tippen-Funktion (beide Hände rhythmisch auf der Tastatur, gelegentlich zur Maus)
        fun typingPose(tNow: Float): HumanPose {
            val tapL = sin(tNow * 14.5f) * 4.5f
            val tapR = sin(tNow * 14.5f + 1.9f) * 4.5f
            val mouseMove = smoothPhase((tNow % 4.5f), 2.8f, 0.5f) * (1f - smoothPhase((tNow % 4.5f), 3.8f, 0.5f))
            return sitPose(
                lean = 7f + sin(tNow * 1.1f) * 1.2f,
                headYaw = sin(tNow * 0.7f) * 5f,
                headPitch = 10f + sin(tNow * 1.6f) * 2.5f,
                shL = 46f + tapL * 0.6f, shLOut = 10f, elbL = 66f - tapL,
                shR = mix(46f + tapR * 0.6f, 50f, mouseMove),
                shROut = mix(10f, 22f, mouseMove),
                elbR = mix(66f - tapR, 58f, mouseMove),
            )
        }

        return when {
            // 0.0..1.1s: Hinter der sich öffnenden Tür warten
            t < 1.1f -> walkPose(-2.52f, -0.28f, 0f, 0f, 0f)

            // 1.1..6.7s: Durch die Tür herein über den Holzboden zum Schreibtischstuhl gehen
            t < 6.7f -> {
                val u = phase(t, 1.1f, 5.6f)
                val us = smoother(u)
                val wx = mix(-2.52f, CHAIR_WORLD_X - 0.14f, us)
                // Natürlicher leichter Bogen am Teppich vorbei Richtung Stuhl
                val wz = mix(-0.28f, CHAIR_WORLD_Z + 0.14f, us) + sin(us * PI_F) * 0.24f
                val yaw = mix(-4f, DESK_YAW - 18f, us)
                // Kurzer liebevoller Blick zurück zur Katze bei t = 3.2..4.8s
                val glance = sin(phase(t, 3.0f, 1.8f) * PI_F)
                val fadeStride = (1f - smoothPhase(t, 6.0f, 0.7f)).coerceAtLeast(0.05f)
                walkPose(wx, wz, yaw, t * 5.6f, fadeStride, headYaw = -glance * 38f, headPitch = 4f + glance * 10f)
            }

            // 6.7..8.3s: Hinsetzen in den Drehstuhl
            t < 8.3f -> {
                val startStand = walkPose(CHAIR_WORLD_X - 0.14f, CHAIR_WORLD_Z + 0.14f, DESK_YAW - 18f, 0f, 0f)
                val endSit = sitPose(lean = 4f, shL = 22f, elbL = 42f, shR = 22f, elbR = 42f)
                lerpPose(startStand, endSit, phase(t, 6.7f, 1.6f))
            }

            // 8.3..9.2s: Rechte Hand weckt PC/Monitor auf, dann Hände zur Tastatur
            t < 9.2f -> {
                val wake = sin(phase(t, 8.25f, 0.75f) * PI_F)
                val base = typingPose(t)
                base.copy(
                    torsoLean = mix(base.torsoLean, 14f, wake),
                    shR = mix(base.shR, 62f, wake),
                    shROut = mix(base.shROut, 18f, wake),
                    elbR = mix(base.elbR, 34f, wake),
                )
            }

            // 9.2..13.5s: Konzentriertes Arbeiten & Tippen
            t < 13.5f -> typingPose(t)

            // 13.5..16.1s: Nach Kaffee greifen, Schluck trinken, wieder abstellen
            t < 16.1f -> {
                val tp = typingPose(t)
                val reach = sitPose(lean = 12f, twist = -8f, headYaw = -12f, headPitch = 12f, shL = 42f, elbL = 62f, shR = 56f, shROut = -8f, elbR = 36f)
                val sip = sitPose(lean = 4f, twist = -4f, headYaw = -4f, headPitch = -10f, shL = 36f, elbL = 62f, shR = 48f, shROut = -4f, elbR = 114f)
                when {
                    t < 14.0f -> lerpPose(tp, reach, phase(t, 13.5f, 0.5f))
                    t < 14.6f -> lerpPose(reach, sip, phase(t, 14.0f, 0.6f))
                    t < 15.1f -> sip
                    t < 15.65f -> lerpPose(sip, reach, phase(t, 15.1f, 0.55f))
                    else -> lerpPose(reach, tp, phase(t, 15.65f, 0.45f))
                }
            }

            // 16.1..17.6s: Schreibtischlampe anschalten als es dämmert
            t < 17.6f -> {
                val tp = typingPose(t)
                val lampReach = sitPose(lean = 18f, twist = -14f, headYaw = -18f, headPitch = 4f, shL = 40f, elbL = 60f, shR = 72f, shROut = -12f, elbR = 24f)
                when {
                    t < 16.95f -> lerpPose(tp, lampReach, phase(t, 16.3f, 0.65f))
                    else -> lerpPose(lampReach, tp, phase(t, 16.95f, 0.65f))
                }
            }

            // 17.6..21.9s: Weiter tippen, ab 21.0s neugierig zur springenden Katze schauen
            t < 21.9f -> {
                val tp = typingPose(t)
                val lookCat = smootherPhase(t, 20.8f, 1.0f)
                tp.copy(
                    headYaw = mix(tp.headYaw, 34f, lookCat),
                    headPitch = mix(tp.headPitch, 14f, lookCat),
                )
            }

            // 21.9..26.2s: Katze auf der Tischecke liebevoll streicheln
            t < 26.2f -> {
                val tp = typingPose(t)
                val stroke = sin(t * 4.2f)
                val pet = sitPose(
                    lean = 12f, twist = 16f,
                    headYaw = 32f + stroke * 3f, headPitch = 18f,
                    shL = 34f, shLOut = 10f, elbL = 60f,
                    shR = 52f + stroke * 8f, shROut = 28f, elbR = 46f - stroke * 10f,
                )
                when {
                    t < 22.6f -> lerpPose(tp, pet, phase(t, 21.9f, 0.7f))
                    t < 25.5f -> pet
                    else -> lerpPose(pet, tp, phase(t, 25.5f, 0.7f))
                }
            }

            // 26.2..28.6s: Letzte Aufgaben abhaken
            t < 28.6f -> typingPose(t)

            // 28.6..30.5s: Zufrieden strecken & Lampe/PC ausschalten
            t < 30.5f -> {
                val tp = typingPose(t)
                val stretch = sitPose(lean = -8f, headPitch = -12f, shL = 148f, shLOut = 28f, elbL = 22f, shR = 148f, shROut = 28f, elbR = 22f)
                val offReach = sitPose(lean = 14f, twist = -10f, headYaw = -12f, shL = 32f, elbL = 54f, shR = 66f, shROut = -10f, elbR = 26f)
                when {
                    t < 29.3f -> lerpPose(tp, stretch, phase(t, 28.6f, 0.7f))
                    t < 29.8f -> stretch
                    else -> lerpPose(stretch, offReach, phase(t, 29.8f, 0.7f))
                }
            }

            // 30.5..31.7s: Aufstehen & zur offenen Raumseite Richtung Tür drehen
            t < 31.7f -> {
                val fromSit = sitPose(lean = 10f, shL = 22f, elbL = 36f, shR = 22f, elbR = 36f)
                val toStand = walkPose(CHAIR_WORLD_X - 0.14f, CHAIR_WORLD_Z + 0.14f, -182f, 0f, 0f)
                lerpPose(fromSit, toStand, phase(t, 30.5f, 1.2f))
            }

            // 31.7..36.0s: Zurück zur Tür und hinausgehen
            else -> {
                val u = phase(t, 31.7f, 3.6f)
                val us = smoother(u)
                val wx = mix(CHAIR_WORLD_X - 0.14f, -2.55f, us)
                val wz = mix(CHAIR_WORLD_Z + 0.14f, -0.28f, us) + sin(us * PI_F) * 0.18f
                val stride = if (u < 0.96f) 1f else 0f
                walkPose(wx, wz, -184f, t * 5.8f, stride)
            }
        }
    }

    private fun drawHuman(p: Palette3D, h: HumanPose) {
        ms.scoped {
            ms.translate(h.x, h.pelvisY, h.z)
            ms.rotateY(h.yawDeg)

            // Becken / Hüfte
            ms.scoped {
                ms.scale(0.18f, 0.13f, 0.25f)
                drawMesh(meshes.sphere, p.hose, rough = 0.80f)
            }

            // ---- Linkes & Rechtes Bein ----
            // Konvention: Charakter schaut nach +X. Positive rotateZ schwingt den nach unten (-Y) zeigenden
            // Schenkel nach vorne (+X)! Kniebeugung nach hinten (-X) ist dann negative rotateZ.
            drawHumanLeg(p, sideZ = -0.072f, thighDeg = h.thighL, kneeDeg = h.kneeL)
            drawHumanLeg(p, sideZ = +0.072f, thighDeg = h.thighR, kneeDeg = h.kneeR)

            // ---- Oberkörper / Hoodie (mit ruhiger Atmung) ----
            val breath = 1f + sin(curTime * 2.1f) * 0.015f
            ms.scoped {
                ms.rotateZ(-h.torsoLean) // Vorbeugen Richtung +X ist negative rotateZ!
                ms.rotateY(h.torsoTwist)

                // Hoodie-Rumpf (thickCapsule hat Durchmesser 0.68 -> ergibt ca. 0.24 × 0.30 m)
                ms.scoped {
                    ms.translate(0f, 0.22f, 0f)
                    ms.scale(0.35f * breath, 0.46f, 0.44f * breath)
                    drawMesh(meshes.thickCapsule, p.hoodie, rough = 0.84f, pattern = 6)
                }
                // Lockerer Kapuzen-Kragen im Nacken
                ms.scoped {
                    ms.translate(-0.04f, 0.42f, 0f)
                    ms.rotateZ(18f)
                    ms.scale(0.17f, 0.10f, 0.22f)
                    drawMesh(meshes.sphere, p.hoodie, rough = 0.86f, pattern = 6)
                }

                // ---- Hals & detaillierter 3D-Kopf ----
                ms.scoped {
                    ms.translate(0.01f, 0.45f, 0f)
                    ms.scoped { ms.translate(0f, 0.04f, 0f); ms.scale(0.078f, 0.09f, 0.078f); drawMesh(meshes.cylinder, p.haut, rough = 0.62f) }
                    ms.translate(0f, 0.145f, 0f)
                    ms.rotateY(h.headYaw)
                    ms.rotateZ(-h.headPitch)

                    // Kopf-Oval
                    ms.scoped { ms.scale(0.21f, 0.23f, 0.195f); drawMesh(meshes.sphere, p.haut, rough = 0.58f) }
                    // Kleine Nase (+X)
                    ms.scoped { ms.translate(0.102f, -0.008f, 0f); ms.scale(0.032f, 0.030f, 0.026f); drawMesh(meshes.sphere, p.haut, rough = 0.56f) }
                    // Ohren (±Z)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped { ms.translate(-0.01f, -0.005f, sz * 0.096f); ms.scale(0.032f, 0.052f, 0.022f); drawMesh(meshes.sphere, p.haut, rough = 0.60f) }
                    }
                    // Augen (mit Blinzeln)
                    val eyeScaleY = (1f - h.blink * 0.88f).coerceAtLeast(0.12f)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(0.088f, 0.018f, sz * 0.042f)
                            ms.scale(0.024f, 0.028f * eyeScaleY, 0.024f)
                            drawMesh(meshes.sphere, eyeDarkRgb, rough = 0.12f)
                        }
                    }
                    // Stilisiertes 3D-Haarvolumen (Kopfkrone + Seitenscheitel + Pony)
                    ms.scoped { ms.translate(-0.022f, 0.046f, 0f); ms.scale(0.225f, 0.185f, 0.215f); drawMesh(meshes.sphere, p.haar, rough = 0.66f) }
                    ms.scoped { ms.translate(-0.068f, -0.006f, 0f); ms.scale(0.15f, 0.17f, 0.195f); drawMesh(meshes.sphere, p.haar, rough = 0.66f) }
                    ms.scoped { ms.translate(0.055f, 0.075f, 0.032f); ms.rotateZ(-18f); ms.scale(0.115f, 0.072f, 0.135f); drawMesh(meshes.sphere, p.haar, rough = 0.66f) }
                }

                // ---- Linker & Rechter Arm ----
                drawHumanArm(p, sideZ = -0.165f, shFwd = h.shL, shOut = h.shLOut, elbDeg = h.elbL, trackHand = false)
                drawHumanArm(p, sideZ = +0.165f, shFwd = h.shR, shOut = h.shROut, elbDeg = h.elbR, trackHand = true)
            }
        }
    }

    private fun drawHumanLeg(p: Palette3D, sideZ: Float, thighDeg: Float, kneeDeg: Float) {
        ms.scoped {
            ms.translate(0f, -0.02f, sideZ)
            ms.rotateZ(thighDeg)
            // Oberschenkel (capsule Durchmesser 0.44 -> 0.27 ergibt ~0.12 m)
            ms.scoped {
                ms.translate(0f, -0.19f, 0f)
                ms.scale(0.27f, 0.41f, 0.27f)
                drawMesh(meshes.capsule, p.hose, rough = 0.80f)
            }
            // Knie + Unterschenkel
            ms.translate(0f, -0.38f, 0f)
            ms.rotateZ(-kneeDeg)
            ms.scoped {
                ms.translate(0f, -0.19f, 0f)
                ms.scale(0.23f, 0.40f, 0.23f)
                drawMesh(meshes.capsule, p.hose, rough = 0.80f)
            }
            // Sneaker-Schuh am Knöchel (waagerecht ausgerichtet)
            ms.translate(0f, -0.39f, 0f)
            ms.rotateZ(kneeDeg - thighDeg)
            ms.scoped {
                ms.translate(0.042f, -0.014f, 0f)
                ms.scale(0.16f, 0.058f, 0.090f)
                drawMesh(meshes.beveledBox, p.schuh, rough = 0.56f)
            }
            ms.scoped {
                ms.translate(0.042f, -0.044f, 0f)
                ms.scale(0.165f, 0.020f, 0.094f)
                drawMesh(meshes.beveledBox, soleWhiteRgb, rough = 0.65f)
            }
        }
    }

    private fun drawHumanArm(p: Palette3D, sideZ: Float, shFwd: Float, shOut: Float, elbDeg: Float, trackHand: Boolean) {
        val sign = if (sideZ > 0f) 1f else -1f
        ms.scoped {
            ms.translate(0.01f, 0.38f, sideZ)
            // Schulterkugel
            ms.scoped { ms.scale(0.115f); drawMesh(meshes.sphere, p.hoodie, rough = 0.84f, pattern = 6) }
            ms.rotateX(-sign * shOut)
            ms.rotateZ(shFwd)
            // Oberarm (capsule Durchmesser 0.44 -> 0.22 ergibt ~0.097 m)
            ms.scoped {
                ms.translate(0f, -0.135f, 0f)
                ms.scale(0.22f, 0.28f, 0.22f)
                drawMesh(meshes.capsule, p.hoodie, rough = 0.84f, pattern = 6)
            }
            // Ellbogen + Unterarm (beugt nach vorne/+X -> +rotateZ)
            ms.translate(0f, -0.255f, 0f)
            ms.rotateZ(elbDeg)
            ms.scoped {
                ms.translate(0f, -0.115f, 0f)
                ms.scale(0.19f, 0.24f, 0.19f)
                drawMesh(meshes.capsule, p.hoodie, rough = 0.84f, pattern = 6)
            }
            // Hand
            ms.translate(0f, -0.235f, 0f)
            if (trackHand) {
                rightHandWorld = ms.transformPoint(0f, -0.02f, 0f)
            }
            ms.scoped {
                ms.scale(0.072f, 0.086f, 0.060f)
                drawMesh(meshes.sphere, p.haut, rough = 0.58f)
            }
        }
    }

    // ============================================================================================
    // ARTIKULIERTE 3D-KATZE (Vierbeiner-Gang, Sitzen, Sprung auf den Schreibtisch, 4-Segment-Schwanz)
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
        val headYaw: Float,
        val headPitch: Float,
        val tailBase: Float,
        val tailWave: Float,
        val earTwitch: Float,
        val blink: Float,
    )

    private fun evaluateCatPose(t: Float): CatPose {
        val blinkC = (t + 2.1f) % 4.3f
        val blink = if (blinkC < 0.16f) sin(blinkC / 0.16f * PI_F) else 0f
        val earT = sin(t * 2.6f) * 4f + sin(t * 7.1f) * 2.5f

        val rugX = -0.68f; val rugZ = 0.28f
        val launchX = 0.64f; val launchZ = 0.44f
        val deskCatX = CAT_DESK_X; val deskCatY = 0.763f; val deskCatZ = CAT_DESK_Z

        return when {
            // 0.0..2.1s: Hinter dem Menschen vor der Tür
            t < 2.1f -> CatPose(-2.72f, 0f, -0.44f, 8f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 45f, 10f, earT, blink)

            // 2.1..6.9s: Durch die Tür herein zum warmen Teppich traben
            t < 6.9f -> {
                val u = smootherPhase(t, 2.1f, 4.8f)
                val cx = mix(-2.65f, rugX, u)
                val cz = mix(-0.42f, rugZ, u) + sin(u * PI_F) * 0.16f
                val yaw = mix(-12f, 18f, u)
                val amp = 1f - smoothPhase(t, 6.2f, 0.7f)
                CatPose(cx, 0f, cz, yaw, 0f, 0f, t * 9.5f, amp, 0f, sin(t * 1.4f) * 14f, -8f, 64f, 18f, earT, blink)
            }

            // 6.9..19.0s: Auf dem Teppich im Sonnenlicht sitzen & neugierig zuschauen
            t < 19.0f -> {
                val sit = smootherPhase(t, 6.9f, 0.9f)
                val look = sin(t * 0.65f) * 22f
                CatPose(rugX, 0f, rugZ, 18f, 22f * sit, 0.03f * sit, 0f, 0f, sit, look, -12f + sin(t * 0.5f) * 6f, mix(58f, 22f, sit), 12f, earT, blink)
            }

            // 19.0..20.8s: Aufstehen und vor die Schreibtischente traben
            t < 20.8f -> {
                val standUp = 1f - smootherPhase(t, 19.0f, 0.45f)
                val u = smootherPhase(t, 19.25f, 1.55f)
                val cx = mix(rugX, launchX, u)
                val cz = mix(rugZ, launchZ, u) + sin(u * PI_F) * 0.12f
                val yaw = mix(-8f, 38f, smooth(u))
                val amp = smoothPhase(t, 19.25f, 0.25f) * (1f - smoothPhase(t, 20.5f, 0.3f))
                CatPose(cx, 0f, cz, yaw, 22f * standUp, 0.03f * standUp, t * 10.5f, amp, standUp, 8f, -14f, 65f, 20f, earT, blink)
            }

            // 20.8..21.35s: Niedriges Ducken (Antizipation vor dem Sprung!)
            t < 21.35f -> {
                val crouch = smootherPhase(t, 20.8f, 0.48f)
                CatPose(launchX, 0f, launchZ, 38f, -12f * crouch, 0.062f * crouch, 0f, 0f, 0f, 0f, -26f * crouch, 28f, 24f, earT, blink)
            }

            // 21.35..22.0f: Eleganter 3D-Bogensprung auf die Ecke des Schreibtischs!
            t < 22.0f -> {
                val k = phase(t, 21.35f, 0.65f)
                val cx = mix(launchX, deskCatX, k)
                val cz = mix(launchZ, deskCatZ, k)
                val cy = mix(0f, deskCatY, k) + sin(k * PI_F) * 0.36f
                val pitch = (0.5f - k) * 62f // Aufwärts beim Absprung, abwärts beim Landen
                CatPose(cx, cy, cz, mix(38f, 52f, k), pitch, 0f, k * PI_F, 0f, 0f, 0f, -10f, 15f, 10f, earT, blink)
            }

            // 22.0..28.3s: Auf der Schreibtischente sitzen, gestreichelt werden & Bildschirm beobachten
            t < 28.3f -> {
                val turn = smootherPhase(t, 22.0f, 0.65f)
                val sit = smootherPhase(t, 22.15f, 0.7f) * (1f - smootherPhase(t, 27.9f, 0.4f))
                val petted = smootherPhase(t, 22.4f, 0.5f) * (1f - smootherPhase(t, 25.6f, 0.5f))
                val purrNod = sin(t * 4.2f) * 6f * petted
                CatPose(
                    deskCatX, deskCatY, deskCatZ,
                    mix(52f, 178f, turn),
                    24f * sit - petted * 6f,
                    0.028f * sit,
                    0f, 0f, sit,
                    mix(0f, -18f, turn) + purrNod * 0.5f,
                    -6f + purrNod,
                    32f + petted * 22f,
                    16f + petted * 14f,
                    earT,
                    max(blink, petted * 0.55f),
                )
            }

            // 28.3..28.95s: Sanft vom Schreibtisch herunterhüpfen
            t < 28.95f -> {
                val k = phase(t, 28.3f, 0.65f)
                val cx = mix(deskCatX, 0.54f, k)
                val cz = mix(deskCatZ, 0.46f, k)
                val cy = mix(deskCatY, 0f, k) + sin(k * PI_F) * 0.18f
                CatPose(cx, cy, cz, 168f, (0.35f - k) * 48f, 0f, 0f, 0f, 0f, 0f, 8f, 50f, 12f, earT, blink)
            }

            // 28.95..31.4s: Kurz vor der Tür warten & sich umdrehen
            t < 31.4f -> {
                val u = smootherPhase(t, 28.95f, 1.85f)
                val cx = mix(0.54f, -0.68f, u)
                val cz = mix(0.46f, 0.14f, u)
                val amp = 1f - smoothPhase(t, 30.5f, 0.5f)
                CatPose(cx, 0f, cz, 174f, 0f, 0f, t * 9.5f, amp, 0f, sin(t * 1.5f) * 28f, -10f, 68f, 16f, earT, blink)
            }

            // 31.4..36.0s: Gemeinsam mit dem Menschen hinauslaufen
            else -> {
                val u = smootherPhase(t, 31.4f, 3.4f)
                val cx = mix(-0.68f, -2.72f, u)
                val cz = mix(0.14f, -0.44f, u)
                val amp = if (u < 0.96f) 1f else 0f
                CatPose(cx, 0f, cz, 174f, 0f, 0f, t * 10.2f, amp, 0f, 0f, 0f, 66f, 18f, earT, blink)
            }
        }
    }

    private fun drawCat(p: Palette3D, c: CatPose) {
        ms.scoped {
            ms.translate(c.x, c.y + 0.188f - c.hipDrop, c.z)
            ms.rotateY(c.yawDeg)

            // Körper (horizontal entlang +X)
            ms.scoped {
                ms.rotateZ(c.bodyPitch)

                // Getigerter Katzenrumpf (liegende thickCapsule, Durchmesser 0.68 -> ~0.16 × 0.14 m)
                ms.scoped {
                    ms.rotateZ(90f)
                    ms.scale(0.24f, 0.35f, 0.21f)
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

                    // Kopf
                    ms.scoped { ms.scale(0.145f, 0.135f, 0.145f); drawMesh(meshes.sphere, p.katzeFell, rough = 0.80f, pattern = 5, alt = p.katzeStreifen) }
                    // Schnauze & rosa Nase
                    ms.scoped { ms.translate(0.068f, -0.018f, 0f); ms.scale(0.062f, 0.048f, 0.068f); drawMesh(meshes.sphere, p.katzeCreme, rough = 0.82f) }
                    ms.scoped { ms.translate(0.095f, -0.008f, 0f); ms.scale(0.022f, 0.017f, 0.024f); drawMesh(meshes.sphere, pinkNoseRgb, rough = 0.40f) }

                    // Ausdrucksstarke Augen
                    val eyeY = (1f - c.blink * 0.88f).coerceAtLeast(0.12f)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(0.060f, 0.015f, sz * 0.036f)
                            ms.scale(0.022f, 0.026f * eyeY, 0.022f)
                            drawMesh(meshes.sphere, eyeDarkRgb, rough = 0.08f)
                        }
                    }

                    // Zwei dreidimensionale Ohren (mit sanftem Zucken)
                    for (sz in floatArrayOf(-1f, 1f)) {
                        ms.scoped {
                            ms.translate(-0.005f, 0.052f, sz * 0.048f)
                            ms.rotateY(90f) // Vorderseite des Ohrs schaut nach +X
                            ms.rotateZ(-sz * (16f + c.earTwitch * 0.5f))
                            ms.scale(0.058f, 0.068f, 0.048f)
                            drawMesh(meshes.catEar, p.katzeFell, rough = 0.78f)
                        }
                    }
                }

                // ---- 4-Segment artikulierter geschwungener Schwanz ----
                ms.scoped {
                    ms.translate(-0.155f, 0.025f, 0f)
                    ms.rotateZ(-c.bodyPitch * 0.4f)
                    var segLen = 0.082f
                    var segRad = 0.086f // capsule Durchmesser 0.44 -> ~0.038 m flauschiger Schwanz
                    for (seg in 0 until 4) {
                        val swayY = sin(curTime * 2.8f - seg * 0.75f) * c.tailWave * (0.4f + seg * 0.25f)
                        val liftZ = if (seg == 0) c.tailBase else (18f - seg * 12f)
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

            // ---- 4 artikulierte Katzenbeine (Diagonalgang beim Laufen, angezogen beim Sitzen) ----
            val fl = sin(c.gaitPhase) * 34f * c.gaitAmp
            val fr = sin(c.gaitPhase + PI_F) * 34f * c.gaitAmp
            val bl = sin(c.gaitPhase + PI_F) * 34f * c.gaitAmp
            val br = sin(c.gaitPhase) * 34f * c.gaitAmp

            // Vorderbeine
            drawCatLeg(p, x = 0.11f, z = -0.052f, swing = fl - c.sitAmt * 8f, bend = c.sitAmt * 10f)
            drawCatLeg(p, x = 0.11f, z = +0.052f, swing = fr - c.sitAmt * 8f, bend = c.sitAmt * 10f)
            // Hinterbeine (beim Sitzen angewinkelt)
            drawCatLeg(p, x = -0.11f, z = -0.056f, swing = bl + c.sitAmt * 56f, bend = c.sitAmt * 48f)
            drawCatLeg(p, x = -0.11f, z = +0.056f, swing = br + c.sitAmt * 56f, bend = c.sitAmt * 48f)
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
            // Helle Pfote
            ms.scoped {
                ms.translate(0.012f, -0.072f, 0f)
                ms.scale(0.048f, 0.028f, 0.044f)
                drawMesh(meshes.sphere, p.katzeCreme, rough = 0.84f)
            }
        }
    }

    // ============================================================================================
    // PASS 3: ATMOSPHÄRE (Sonnenstrahlen, Lampenkegel, Kaffeedampf, Staubkörner)
    // ============================================================================================

    private fun drawAtmosphere(p: Palette3D, t: Float, camEye: Vec3) {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        GLES30.glUseProgram(shaders.alphaProgram)
        GLES30.glUniformMatrix4fv(shaders.alphaViewProj, 1, false, viewProjM, 0)
        GLES30.glUniform1f(shaders.alphaTime, t)

        // 1. Weiche schräge Sonnenstrahlen durch das Fenster
        val sunAlpha = (1f - curAbend * 0.85f) * 0.105f
        if (sunAlpha > 0.006f) {
            val rayR = mix(1.0f, 1.0f, curAbend)
            val rayG = mix(0.95f, 0.72f, curAbend)
            val rayB = mix(0.80f, 0.45f, curAbend)
            GLES30.glUniform1i(shaders.alphaMode, 0)
            for (i in 0 until 2) {
                ms.reset()
                ms.translate(0.55f + i * 0.48f, 1.10f, -0.82f)
                ms.rotateY(-24f)
                ms.rotateZ(30f)
                ms.rotateX(16f)
                ms.scale(0.62f, 2.25f, 1f)
                GLES30.glUniformMatrix4fv(shaders.alphaModel, 1, false, ms.current, 0)
                GLES30.glUniform4f(shaders.alphaColor, rayR, rayG, rayB, sunAlpha)
                meshes.quadXY.draw()
            }
        }

        // 2. Warmer Lampenkegel unter dem Schirm (wenn Lampe eingeschaltet)
        if (curLampeAn > 0.02f) {
            GLES30.glUniform1i(shaders.alphaMode, 0)
            val mid = lerpVec3(lampBulbWorld, lampAimWorld, 0.45f)
            val toCam = (camEye - mid)
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

        // 3. Zarter Kaffeedampf über der Tasse
        if (curMugInHand < 0.5f) {
            GLES30.glUniform1i(shaders.alphaMode, 1)
            val toCam = camEye - mugRestWorld
            val billYaw = atan2(toCam.x, toCam.z) * (180f / PI_F)
            for (i in 0 until 3) {
                val q = ((t * 0.42f + i / 3f) % 1f)
                val rise = q * 0.24f
                val drift = sin(q * 6.28f + i * 2.1f) * 0.022f
                val steamA = sin(q * PI_F) * 0.22f
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

        // 4. Feine Staubkörnchen im Sonnenlicht
        val moteAlpha = (1f - curAbend * 0.6f) * 0.36f
        if (moteAlpha > 0.02f) {
            GLES30.glUniform1i(shaders.alphaMode, 2)
            for (i in 0 until 10) {
                val mx = -0.25f + (i * 0.27f % 1.85f) + sin(t * 0.45f + i * 1.7f) * 0.08f
                val my = 0.55f + (i * 0.39f % 1.25f) + cos(t * 0.35f + i * 2.3f) * 0.06f
                val mz = -1.05f + (i * 0.31f % 1.15f)
                val toCam = camEye - Vec3(mx, my, mz)
                val billYaw = atan2(toCam.x, toCam.z) * (180f / PI_F)
                val tw = 0.45f + 0.55f * sin(t * 1.9f + i * 3.1f)
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

        // Stuhl-Weltposition (desk-lokal (-0.58, 0, +0.02) rotiert um +26°)
        private const val CHAIR_WORLD_X = 0.468f
        private const val CHAIR_WORLD_Z = -0.108f

        // Katzen-Landeplatz auf der vorderen linken Tischecke (desk-lokal (-0.08, 0.763, +0.48))
        private const val CAT_DESK_X = 1.118f
        private const val CAT_DESK_Z = 0.086f
    }
}
