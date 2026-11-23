package de.frank.aufgaben.ui.szene3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ein auf der GPU gespeichertes 3D-Mesh (Interleaved VBO: pos3 + norm3 + uv2 = 32 Byte/Vertex + IBO + VAO).
 */
internal class GpuMesh(
    val vao: Int,
    val vbo: Int,
    val ibo: Int,
    val indexCount: Int,
) {
    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    }

    fun release() {
        val vaos = intArrayOf(vao)
        val bufs = intArrayOf(vbo, ibo)
        GLES30.glDeleteVertexArrays(1, vaos, 0)
        GLES30.glDeleteBuffers(2, bufs, 0)
    }
}

private class MeshBuilder {
    private val verts = ArrayList<Float>(1024)
    private val indices = ArrayList<Short>(1024)

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
        val idx = verts.size / 8
        verts.add(x); verts.add(y); verts.add(z)
        verts.add(nx); verts.add(ny); verts.add(nz)
        verts.add(u); verts.add(v)
        return idx
    }

    fun addTri(a: Int, b: Int, c: Int) {
        indices.add(a.toShort())
        indices.add(b.toShort())
        indices.add(c.toShort())
    }

    fun addQuad(a: Int, b: Int, c: Int, d: Int) {
        addTri(a, b, c)
        addTri(a, c, d)
    }

    fun upload(): GpuMesh {
        val vArr = FloatArray(verts.size) { verts[it] }
        val iArr = ShortArray(indices.size) { indices[it] }

        val vBuf = ByteBuffer.allocateDirect(vArr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vBuf.put(vArr).position(0)
        val iBuf = ByteBuffer.allocateDirect(iArr.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        iBuf.put(iArr).position(0)

        val vaoArr = IntArray(1)
        val bufArr = IntArray(2)
        GLES30.glGenVertexArrays(1, vaoArr, 0)
        GLES30.glGenBuffers(2, bufArr, 0)

        GLES30.glBindVertexArray(vaoArr[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufArr[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vArr.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bufArr[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, iArr.size * 2, iBuf, GLES30.GL_STATIC_DRAW)

        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindVertexArray(0)
        return GpuMesh(vaoArr[0], bufArr[0], bufArr[1], iArr.size)
    }
}

/**
 * Sammlung aller prozeduralen 3D-Meshes für Raum, Möbel, PC-Arbeitsplatz, Mensch und Katze.
 * Wird genau einmal pro GL-Kontext erzeugt und bei Teardown sauber freigegeben.
 */
internal class MeshLibrary {
    /** Einheitswürfel [-0.5..+0.5]^3 mit sauberen Flächennormalen. */
    val box: GpuMesh = buildUnitBox()
    /** Abgeschrägter Einheitsquader [-0.5..+0.5]^3 — fängt realistische Glanzlichter an Möbelkanten ein. */
    val beveledBox: GpuMesh = buildBeveledBox(bevel = 0.08f)
    /** Weiche UV-Kugel (Radius 0.5, Durchmesser 1.0). */
    val sphere: GpuMesh = buildSphere(16, 22)
    /** Glatter Zylinder (Radius 0.5, Höhe 1.0 entlang Y von -0.5 bis +0.5) mit Deckeln. */
    val cylinder: GpuMesh = buildCylinder(20, 0.5f, 0.5f)
    /** Verjüngter Kegelstumpf (unten r=0.5, oben r=0.22, Höhe 1.0 entlang Y) für Lampenschirm/Topf. */
    val frustum: GpuMesh = buildCylinder(22, 0.5f, 0.24f)
    /** Offener Lampenschirm-Kegelstumpf (doppelseitig mit Wandstärke, unten r=0.5, oben r=0.20). */
    val lampShade: GpuMesh = buildHollowShade(22, 0.5f, 0.20f)
    /** Abgerundete Kapsel (Gesamthöhe 1.0 von -0.5..+0.5, Radius 0.22) für organische Gliedmaßen & Körper. */
    val capsule: GpuMesh = buildCapsule(16, 6, radius = 0.22f, totalHeight = 1.0f)
    /** Dickere Kapsel (Radius 0.34, Höhe 1.0) für Rumpf, Katzenkörper und Kissen. */
    val thickCapsule: GpuMesh = buildCapsule(18, 6, radius = 0.34f, totalHeight = 1.0f)
    /** Torus (Hauptradius 0.38, Rohrradius 0.12 in XY-Ebene) für Tassenhenkel, Uhrenring, Rollen. */
    val torus: GpuMesh = buildTorus(22, 10, 0.38f, 0.11f)
    /** 3D-Faltvorhang (Breite 1, Höhe 1, Tiefe ~0.12) mit weichen sinusförmigen Stoffwellen. */
    val curtain: GpuMesh = buildCurtain(18, 8)
    /** Gebogene 3D-Pflanzenblätter (Länge 1 entlang +Z/+Y, sanft gewölbt). */
    val leaf: GpuMesh = buildCurvedLeaf()
    /** Ergonomisch gewölbte Bürostuhl-Rückenlehne (Breite 1, Höhe 1, leichte Rundung um Y). */
    val chairBack: GpuMesh = buildChairBack()
    /** Dreidimensionales Katzenohr (Breite 1, Höhe 1, leicht ausgehöhlte Vorderseite). */
    val catEar: GpuMesh = buildCatEar()
    /** Flaches Quad [-0.5..+0.5] in XY-Ebene (+Z Normale) für Lichtstrahlen, Dampf und Bildschirmfläche. */
    val quadXY: GpuMesh = buildQuadXY()

    fun release() {
        box.release()
        beveledBox.release()
        sphere.release()
        cylinder.release()
        frustum.release()
        lampShade.release()
        capsule.release()
        thickCapsule.release()
        torus.release()
        curtain.release()
        leaf.release()
        chairBack.release()
        catEar.release()
        quadXY.release()
    }
}

private fun buildQuadXY(): GpuMesh {
    val b = MeshBuilder()
    val v0 = b.addVertex(-0.5f, -0.5f, 0f, 0f, 0f, 1f, 0f, 0f)
    val v1 = b.addVertex(0.5f, -0.5f, 0f, 0f, 0f, 1f, 1f, 0f)
    val v2 = b.addVertex(0.5f, 0.5f, 0f, 0f, 0f, 1f, 1f, 1f)
    val v3 = b.addVertex(-0.5f, 0.5f, 0f, 0f, 0f, 1f, 0f, 1f)
    b.addQuad(v0, v1, v2, v3)
    return b.upload()
}

private fun buildUnitBox(): GpuMesh {
    val b = MeshBuilder()
    fun face(
        p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3,
        nx: Float, ny: Float, nz: Float,
    ) {
        val i0 = b.addVertex(p0.x, p0.y, p0.z, nx, ny, nz, 0f, 0f)
        val i1 = b.addVertex(p1.x, p1.y, p1.z, nx, ny, nz, 1f, 0f)
        val i2 = b.addVertex(p2.x, p2.y, p2.z, nx, ny, nz, 1f, 1f)
        val i3 = b.addVertex(p3.x, p3.y, p3.z, nx, ny, nz, 0f, 1f)
        b.addQuad(i0, i1, i2, i3)
    }
    val h = 0.5f
    // +Z (front)
    face(Vec3(-h, -h, h), Vec3(h, -h, h), Vec3(h, h, h), Vec3(-h, h, h), 0f, 0f, 1f)
    // -Z (back)
    face(Vec3(h, -h, -h), Vec3(-h, -h, -h), Vec3(-h, h, -h), Vec3(h, h, -h), 0f, 0f, -1f)
    // +X (right)
    face(Vec3(h, -h, h), Vec3(h, -h, -h), Vec3(h, h, -h), Vec3(h, h, h), 1f, 0f, 0f)
    // -X (left)
    face(Vec3(-h, -h, -h), Vec3(-h, -h, h), Vec3(-h, h, h), Vec3(-h, h, -h), -1f, 0f, 0f)
    // +Y (top)
    face(Vec3(-h, h, h), Vec3(h, h, h), Vec3(h, h, -h), Vec3(-h, h, -h), 0f, 1f, 0f)
    // -Y (bottom)
    face(Vec3(-h, -h, -h), Vec3(h, -h, -h), Vec3(h, -h, h), Vec3(-h, -h, h), 0f, -1f, 0f)
    return b.upload()
}

/**
 * Quader mit 45°-Fasen an allen 12 Kanten und 8 Ecken: reflektiert Lichtkanten realistisch.
 * Erzeugt als sanft abgerundeter Superquadric/Rounded-Box-Gitterkörper auf einem Würfelnetz.
 */
private fun buildBeveledBox(bevel: Float): GpuMesh {
    val b = MeshBuilder()
    val h = 0.5f
    val inner = h - bevel
    val steps = 4 // Pro Seite 4 Segmente (0, bevel, 1-bevel, 1) -> glatte Fasen!
    val coords = floatArrayOf(-h, -inner, 0f, inner, h)

    fun projectToRoundedCube(px: Float, py: Float, pz: Float): Pair<Vec3, Vec3> {
        val cx = px.coerceIn(-inner, inner)
        val cy = py.coerceIn(-inner, inner)
        val cz = pz.coerceIn(-inner, inner)
        val dx = px - cx
        val dy = py - cy
        val dz = pz - cz
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        return if (len > 1e-6f) {
            val nx = dx / len; val ny = dy / len; val nz = dz / len
            Pair(Vec3(cx + nx * bevel, cy + ny * bevel, cz + nz * bevel), Vec3(nx, ny, nz))
        } else {
            Pair(Vec3(px, py, pz), Vec3(0f, 1f, 0f))
        }
    }

    fun addSide(mapper: (Float, Float) -> Vec3) {
        val base = Array(steps + 1) { IntArray(steps + 1) }
        for (j in 0..steps) {
            for (i in 0..steps) {
                val raw = mapper(coords[i], coords[j])
                val (pos, n) = projectToRoundedCube(raw.x, raw.y, raw.z)
                val u = (coords[i] + h)
                val v = (coords[j] + h)
                base[j][i] = b.addVertex(pos.x, pos.y, pos.z, n.x, n.y, n.z, u, v)
            }
        }
        for (j in 0 until steps) {
            for (i in 0 until steps) {
                b.addQuad(base[j][i], base[j][i + 1], base[j + 1][i + 1], base[j + 1][i])
            }
        }
    }

    addSide { u, v -> Vec3(u, v, h) }       // +Z
    addSide { u, v -> Vec3(-u, v, -h) }     // -Z
    addSide { u, v -> Vec3(h, v, -u) }      // +X
    addSide { u, v -> Vec3(-h, v, u) }      // -X
    addSide { u, v -> Vec3(u, h, -v) }      // +Y
    addSide { u, v -> Vec3(u, -h, v) }      // -Y

    return b.upload()
}

private fun buildSphere(stacks: Int, slices: Int): GpuMesh {
    val b = MeshBuilder()
    val r = 0.5f
    val ringVerts = Array(stacks + 1) { IntArray(slices + 1) }
    for (st in 0..stacks) {
        val v = st.toFloat() / stacks
        val phi = v * PI_F
        val sinP = sin(phi)
        val cosP = cos(phi)
        for (sl in 0..slices) {
            val u = sl.toFloat() / slices
            val theta = u * TAU_F
            val nx = sinP * cos(theta)
            val ny = cosP
            val nz = sinP * sin(theta)
            ringVerts[st][sl] = b.addVertex(nx * r, ny * r, nz * r, nx, ny, nz, u, 1f - v)
        }
    }
    for (st in 0 until stacks) {
        for (sl in 0 until slices) {
            val a = ringVerts[st][sl]
            val bIdx = ringVerts[st + 1][sl]
            val c = ringVerts[st + 1][sl + 1]
            val d = ringVerts[st][sl + 1]
            b.addQuad(a, d, c, bIdx)
        }
    }
    return b.upload()
}

private fun buildCylinder(segments: Int, bottomR: Float, topR: Float): GpuMesh {
    val b = MeshBuilder()
    val h = 0.5f
    val slopeY = (bottomR - topR) / 1.0f
    val nLen = sqrt(1f + slopeY * slopeY)
    val nySide = slopeY / nLen
    val nrSide = 1f / nLen

    val bot = IntArray(segments + 1)
    val top = IntArray(segments + 1)
    for (i in 0..segments) {
        val u = i.toFloat() / segments
        val a = u * TAU_F
        val ca = cos(a); val sa = sin(a)
        val nx = ca * nrSide; val nz = sa * nrSide
        bot[i] = b.addVertex(ca * bottomR, -h, sa * bottomR, nx, nySide, nz, u, 0f)
        top[i] = b.addVertex(ca * topR, h, sa * topR, nx, nySide, nz, u, 1f)
    }
    for (i in 0 until segments) {
        b.addQuad(bot[i], top[i], top[i + 1], bot[i + 1])
    }

    // Top cap (+Y)
    val topCenter = b.addVertex(0f, h, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
    val topRing = IntArray(segments + 1)
    for (i in 0..segments) {
        val a = (i.toFloat() / segments) * TAU_F
        val ca = cos(a); val sa = sin(a)
        topRing[i] = b.addVertex(ca * topR, h, sa * topR, 0f, 1f, 0f, 0.5f + ca * 0.5f, 0.5f + sa * 0.5f)
    }
    for (i in 0 until segments) {
        b.addTri(topCenter, topRing[i + 1], topRing[i])
    }

    // Bottom cap (-Y)
    val botCenter = b.addVertex(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
    val botRing = IntArray(segments + 1)
    for (i in 0..segments) {
        val a = (i.toFloat() / segments) * TAU_F
        val ca = cos(a); val sa = sin(a)
        botRing[i] = b.addVertex(ca * bottomR, -h, sa * bottomR, 0f, -1f, 0f, 0.5f + ca * 0.5f, 0.5f + sa * 0.5f)
    }
    for (i in 0 until segments) {
        b.addTri(botCenter, botRing[i], botRing[i + 1])
    }
    return b.upload()
}

private fun buildHollowShade(segments: Int, bottomR: Float, topR: Float): GpuMesh {
    val b = MeshBuilder()
    val h = 0.5f
    val slopeY = (bottomR - topR) / 1.0f
    val nLen = sqrt(1f + slopeY * slopeY)
    val nySide = slopeY / nLen
    val nrSide = 1f / nLen

    // Outer shell
    val oBot = IntArray(segments + 1)
    val oTop = IntArray(segments + 1)
    // Inner shell (for visible bright inside)
    val iBot = IntArray(segments + 1)
    val iTop = IntArray(segments + 1)
    val thick = 0.04f
    for (i in 0..segments) {
        val u = i.toFloat() / segments
        val a = u * TAU_F
        val ca = cos(a); val sa = sin(a)
        val nx = ca * nrSide; val nz = sa * nrSide
        oBot[i] = b.addVertex(ca * bottomR, -h, sa * bottomR, nx, nySide, nz, u, 0f)
        oTop[i] = b.addVertex(ca * topR, h, sa * topR, nx, nySide, nz, u, 1f)
        iBot[i] = b.addVertex(ca * (bottomR - thick), -h, sa * (bottomR - thick), -nx, -nySide, -nz, u, 0f)
        iTop[i] = b.addVertex(ca * (topR - thick), h, sa * (topR - thick), -nx, -nySide, -nz, u, 1f)
    }
    for (i in 0 until segments) {
        b.addQuad(oBot[i], oTop[i], oTop[i + 1], oBot[i + 1])
        b.addQuad(iBot[i], iBot[i + 1], iTop[i + 1], iTop[i])
        // Bottom rim
        b.addQuad(oBot[i], oBot[i + 1], iBot[i + 1], iBot[i])
    }
    // Top cap
    val tc = b.addVertex(0f, h, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
    for (i in 0 until segments) {
        b.addTri(tc, oTop[i + 1], oTop[i])
    }
    return b.upload()
}

private fun buildCapsule(slices: Int, hemiRings: Int, radius: Float, totalHeight: Float): GpuMesh {
    val b = MeshBuilder()
    val halfCyl = (totalHeight * 0.5f - radius).coerceAtLeast(0.001f)
    val ringCount = hemiRings * 2 + 1
    val rings = Array(ringCount + 1) { IntArray(slices + 1) }

    for (r in 0..ringCount) {
        val yCenter: Float
        val phi: Float
        when {
            r <= hemiRings -> {
                // Top hemisphere: phi 0 (top pole) -> PI/2 (equator)
                phi = (r.toFloat() / hemiRings) * (PI_F * 0.5f)
                yCenter = halfCyl
            }
            else -> {
                // Bottom hemisphere: phi PI/2 -> PI
                val br = r - hemiRings - 1
                phi = (PI_F * 0.5f) + (br.toFloat() / hemiRings) * (PI_F * 0.5f)
                yCenter = -halfCyl
            }
        }
        val sinP = sin(phi)
        val cosP = cos(phi)
        val y = yCenter + cosP * radius
        val vCoord = 1f - (y + totalHeight * 0.5f) / totalHeight
        for (s in 0..slices) {
            val u = s.toFloat() / slices
            val theta = u * TAU_F
            val nx = sinP * cos(theta)
            val ny = cosP
            val nz = sinP * sin(theta)
            rings[r][s] = b.addVertex(nx * radius, y, nz * radius, nx, ny, nz, u, vCoord)
        }
    }
    for (r in 0 until ringCount) {
        for (s in 0 until slices) {
            b.addQuad(rings[r][s], rings[r][s + 1], rings[r + 1][s + 1], rings[r + 1][s])
        }
    }
    return b.upload()
}

private fun buildTorus(majorSegs: Int, minorSegs: Int, majorR: Float, minorR: Float): GpuMesh {
    val b = MeshBuilder()
    val grid = Array(majorSegs + 1) { IntArray(minorSegs + 1) }
    for (i in 0..majorSegs) {
        val u = i.toFloat() / majorSegs
        val theta = u * TAU_F
        val ct = cos(theta); val st = sin(theta)
        for (j in 0..minorSegs) {
            val v = j.toFloat() / minorSegs
            val phi = v * TAU_F
            val cp = cos(phi); val sp = sin(phi)
            val x = (majorR + minorR * cp) * ct
            val y = (majorR + minorR * cp) * st
            val z = minorR * sp
            val nx = cp * ct; val ny = cp * st; val nz = sp
            grid[i][j] = b.addVertex(x, y, z, nx, ny, nz, u, v)
        }
    }
    for (i in 0 until majorSegs) {
        for (j in 0 until minorSegs) {
            b.addQuad(grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1])
        }
    }
    return b.upload()
}

private fun buildCurtain(cols: Int, rows: Int): GpuMesh {
    val b = MeshBuilder()
    val grid = Array(rows + 1) { IntArray(cols + 1) }
    val waves = 4.5f
    val amp = 0.09f
    for (r in 0..rows) {
        val v = r.toFloat() / rows // 0 = bottom, 1 = top
        val y = v - 0.5f
        val flare = 1f + (1f - v) * 0.14f // Unten leicht weiter fallend
        for (c in 0..cols) {
            val u = c.toFloat() / cols
            val x = (u - 0.5f) * flare
            val phase = u * waves * TAU_F
            val z = sin(phase) * amp
            val dzdx = cos(phase) * amp * waves * TAU_F / flare
            val nLen = sqrt(dzdx * dzdx + 1f)
            grid[r][c] = b.addVertex(x, y, z, -dzdx / nLen, 0f, 1f / nLen, u, v)
        }
    }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            b.addQuad(grid[r][c], grid[r][c + 1], grid[r + 1][c + 1], grid[r + 1][c])
            // Rückseite für sauberen Blick aus schrägen Winkeln
            b.addQuad(grid[r][c], grid[r + 1][c], grid[r + 1][c + 1], grid[r][c + 1])
        }
    }
    return b.upload()
}

private fun buildCurvedLeaf(): GpuMesh {
    val b = MeshBuilder()
    val segs = 7
    val left = IntArray(segs + 1)
    val mid = IntArray(segs + 1)
    val right = IntArray(segs + 1)
    for (i in 0..segs) {
        val t = i.toFloat() / segs // 0 = Stiel, 1 = Spitze
        val width = sin(t * PI_F).coerceAtLeast(0.02f) * 0.28f
        val z = t * 0.95f
        val arch = sin(t * PI_F * 0.82f) * 0.32f - t * t * 0.14f
        val cup = width * 0.18f
        left[i] = b.addVertex(-width, arch + cup, z, -0.35f, 0.92f, 0.15f, 0f, t)
        mid[i] = b.addVertex(0f, arch, z, 0f, 0.98f, 0.18f, 0.5f, t)
        right[i] = b.addVertex(width, arch + cup, z, 0.35f, 0.92f, 0.15f, 1f, t)
    }
    for (i in 0 until segs) {
        b.addQuad(left[i], mid[i], mid[i + 1], left[i + 1])
        b.addQuad(mid[i], right[i], right[i + 1], mid[i + 1])
        // Unterseite
        b.addQuad(left[i], left[i + 1], mid[i + 1], mid[i])
        b.addQuad(mid[i], mid[i + 1], right[i + 1], right[i])
    }
    return b.upload()
}

private fun buildChairBack(): GpuMesh {
    val b = MeshBuilder()
    val cols = 10
    val rows = 6
    val thick = 0.12f
    val front = Array(rows + 1) { IntArray(cols + 1) }
    val back = Array(rows + 1) { IntArray(cols + 1) }
    for (r in 0..rows) {
        val v = r.toFloat() / rows
        val y = v - 0.5f
        val lumbar = sin(v * PI_F) * 0.06f // leichte Lordosenwölbung nach +Z
        for (c in 0..cols) {
            val u = c.toFloat() / cols
            val ang = (u - 0.5f) * 0.95f
            val x = sin(ang) * 0.56f
            val curveZ = (1f - cos(ang)) * 0.42f + lumbar
            val nx = -sin(ang)
            val nz = cos(ang)
            front[r][c] = b.addVertex(x, y, curveZ + thick * 0.5f, nx, 0f, nz, u, v)
            back[r][c] = b.addVertex(x, y, curveZ - thick * 0.5f, -nx, 0f, -nz, u, v)
        }
    }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            b.addQuad(front[r][c], front[r][c + 1], front[r + 1][c + 1], front[r + 1][c])
            b.addQuad(back[r][c], back[r + 1][c], back[r + 1][c + 1], back[r][c + 1])
        }
    }
    // Kanten schließen
    for (c in 0 until cols) {
        b.addQuad(front[rows][c], front[rows][c + 1], back[rows][c + 1], back[rows][c])
        b.addQuad(back[0][c], back[0][c + 1], front[0][c + 1], front[0][c])
    }
    for (r in 0 until rows) {
        b.addQuad(back[r][0], front[r][0], front[r + 1][0], back[r + 1][0])
        b.addQuad(front[r][cols], back[r][cols], back[r + 1][cols], front[r + 1][cols])
    }
    return b.upload()
}

private fun buildCatEar(): GpuMesh {
    val b = MeshBuilder()
    // 3D-Pyramiden-Ohr mit leicht abgerundeter Spitze: Basis y=0, Spitze y=1
    val bl = b.addVertex(-0.48f, 0f, 0.25f, -0.5f, 0.2f, 0.8f, 0f, 0f)
    val br = b.addVertex(0.48f, 0f, 0.25f, 0.5f, 0.2f, 0.8f, 1f, 0f)
    val bb = b.addVertex(0f, 0f, -0.42f, 0f, 0.2f, -0.95f, 0.5f, 0f)
    val tip = b.addVertex(0f, 1f, 0.04f, 0f, 0.95f, 0.25f, 0.5f, 1f)
    // Vorderseite (leicht nach innen gewölbt über Mittelpunkt)
    val inner = b.addVertex(0f, 0.36f, 0.14f, 0f, 0.25f, 0.96f, 0.5f, 0.36f)
    b.addTri(bl, br, inner)
    b.addTri(bl, inner, tip)
    b.addTri(inner, br, tip)
    // Linke Rückflanke
    b.addTri(bb, bl, tip)
    // Rechte Rückflanke
    b.addTri(br, bb, tip)
    // Boden
    b.addTri(bl, bb, br)
    return b.upload()
}
