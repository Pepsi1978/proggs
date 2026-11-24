package de.frank.aufgaben.ui.szene3d

import android.opengl.GLES30
import android.util.Log

/**
 * OpenGL ES 3.0 Shader-Programme für die 3D-Szene:
 * 1. Shadow-Depth-Pass (1024×1024 PCF-Schattenkarte)
 * 2. Haupt-PBR-Pass (Cook-Torrance GGX, 3×3 PCF-Schatten, Schreibtisch-Spot, Monitor-Glow,
 *    prozedurale Holz-/Teppich-/Fell-/Monitor-UI-/Himmel-Shader, ACES-Tonemapping, sRGB-Ausgabe)
 * 3. Atmosphärischer Transparenz-Pass (Sonnenstrahlen, Lampenkegel, Kaffeedampf, Staubpartikel)
 */
internal class ShaderPrograms {
    val shadowProgram: Int
    val shadowLightVP: Int
    val shadowModel: Int

    val mainProgram: Int
    val uModel: Int
    val uViewProj: Int
    val uNormalMat: Int
    val uLightViewProj: Int
    val uCameraPos: Int
    val uTime: Int
    val uExposure: Int
    val uBaseColor: Int
    val uAltColor: Int
    val uRoughness: Int
    val uMetallic: Int
    val uEmissive: Int
    val uPattern: Int
    val uParam: Int
    val uSunDir: Int
    val uSunColor: Int
    val uSkyColor: Int
    val uGroundColor: Int
    val uLampPos: Int
    val uLampDir: Int
    val uLampColor: Int
    val uMonitorPos: Int
    val uMonitorColor: Int
    val uDoorGlowPos: Int
    val uDoorGlowColor: Int
    val uShadowMap: Int
    val uContact0: Int
    val uContact1: Int
    val uContact2: Int
    val uContact3: Int

    val alphaProgram: Int
    val alphaModel: Int
    val alphaViewProj: Int
    val alphaColor: Int
    val alphaMode: Int
    val alphaTime: Int

    init {
        shadowProgram = linkProgram(SHADOW_VS, SHADOW_FS)
        shadowLightVP = GLES30.glGetUniformLocation(shadowProgram, "uLightViewProj")
        shadowModel = GLES30.glGetUniformLocation(shadowProgram, "uModel")

        mainProgram = linkProgram(MAIN_VS, MAIN_FS)
        uModel = GLES30.glGetUniformLocation(mainProgram, "uModel")
        uViewProj = GLES30.glGetUniformLocation(mainProgram, "uViewProj")
        uNormalMat = GLES30.glGetUniformLocation(mainProgram, "uNormalMat")
        uLightViewProj = GLES30.glGetUniformLocation(mainProgram, "uLightViewProj")
        uCameraPos = GLES30.glGetUniformLocation(mainProgram, "uCameraPos")
        uTime = GLES30.glGetUniformLocation(mainProgram, "uTime")
        uExposure = GLES30.glGetUniformLocation(mainProgram, "uExposure")
        uBaseColor = GLES30.glGetUniformLocation(mainProgram, "uBaseColor")
        uAltColor = GLES30.glGetUniformLocation(mainProgram, "uAltColor")
        uRoughness = GLES30.glGetUniformLocation(mainProgram, "uRoughness")
        uMetallic = GLES30.glGetUniformLocation(mainProgram, "uMetallic")
        uEmissive = GLES30.glGetUniformLocation(mainProgram, "uEmissive")
        uPattern = GLES30.glGetUniformLocation(mainProgram, "uPattern")
        uParam = GLES30.glGetUniformLocation(mainProgram, "uParam")
        uSunDir = GLES30.glGetUniformLocation(mainProgram, "uSunDir")
        uSunColor = GLES30.glGetUniformLocation(mainProgram, "uSunColor")
        uSkyColor = GLES30.glGetUniformLocation(mainProgram, "uSkyColor")
        uGroundColor = GLES30.glGetUniformLocation(mainProgram, "uGroundColor")
        uLampPos = GLES30.glGetUniformLocation(mainProgram, "uLampPos")
        uLampDir = GLES30.glGetUniformLocation(mainProgram, "uLampDir")
        uLampColor = GLES30.glGetUniformLocation(mainProgram, "uLampColor")
        uMonitorPos = GLES30.glGetUniformLocation(mainProgram, "uMonitorPos")
        uMonitorColor = GLES30.glGetUniformLocation(mainProgram, "uMonitorColor")
        uDoorGlowPos = GLES30.glGetUniformLocation(mainProgram, "uDoorGlowPos")
        uDoorGlowColor = GLES30.glGetUniformLocation(mainProgram, "uDoorGlowColor")
        uShadowMap = GLES30.glGetUniformLocation(mainProgram, "uShadowMap")
        uContact0 = GLES30.glGetUniformLocation(mainProgram, "uContact0")
        uContact1 = GLES30.glGetUniformLocation(mainProgram, "uContact1")
        uContact2 = GLES30.glGetUniformLocation(mainProgram, "uContact2")
        uContact3 = GLES30.glGetUniformLocation(mainProgram, "uContact3")

        alphaProgram = linkProgram(ALPHA_VS, ALPHA_FS)
        alphaModel = GLES30.glGetUniformLocation(alphaProgram, "uModel")
        alphaViewProj = GLES30.glGetUniformLocation(alphaProgram, "uViewProj")
        alphaColor = GLES30.glGetUniformLocation(alphaProgram, "uColor")
        alphaMode = GLES30.glGetUniformLocation(alphaProgram, "uMode")
        alphaTime = GLES30.glGetUniformLocation(alphaProgram, "uTime")
    }

    fun release() {
        GLES30.glDeleteProgram(shadowProgram)
        GLES30.glDeleteProgram(mainProgram)
        GLES30.glDeleteProgram(alphaProgram)
    }

    companion object {
        private const val TAG = "Szene3DShader"

        private fun compileShader(type: Int, src: String): Int {
            val id = GLES30.glCreateShader(type)
            GLES30.glShaderSource(id, src)
            GLES30.glCompileShader(id)
            val status = IntArray(1)
            GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(id)
                GLES30.glDeleteShader(id)
                Log.e(TAG, "Shader compile error: $log")
                throw IllegalStateException("Shader compile failed: $log")
            }
            return id
        }

        private fun linkProgram(vsSrc: String, fsSrc: String): Int {
            val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSrc)
            val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSrc)
            val prog = GLES30.glCreateProgram()
            GLES30.glAttachShader(prog, vs)
            GLES30.glAttachShader(prog, fs)
            GLES30.glLinkProgram(prog)
            val status = IntArray(1)
            GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            if (status[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(prog)
                GLES30.glDeleteProgram(prog)
                Log.e(TAG, "Program link error: $log")
                throw IllegalStateException("Program link failed: $log")
            }
            return prog
        }

        private const val SHADOW_VS = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
uniform mat4 uLightViewProj;
uniform mat4 uModel;
void main() {
    gl_Position = uLightViewProj * uModel * vec4(aPos, 1.0);
}
"""

        private const val SHADOW_FS = """#version 300 es
precision mediump float;
void main() {
}
"""

        private const val MAIN_VS = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;

uniform mat4 uModel;
uniform mat4 uViewProj;
uniform mat3 uNormalMat;
uniform mat4 uLightViewProj;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;
out vec4 vLightSpacePos;

void main() {
    vec4 wp = uModel * vec4(aPos, 1.0);
    vWorldPos = wp.xyz;
    vNormal = normalize(uNormalMat * aNormal);
    vUv = aUv;
    vLightSpacePos = uLightViewProj * wp;
    gl_Position = uViewProj * wp;
}
"""

        private const val MAIN_FS = """#version 300 es
precision highp float;
precision highp sampler2DShadow;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;
in vec4 vLightSpacePos;

uniform vec3 uCameraPos;
uniform float uTime;
uniform float uExposure;

uniform vec3 uBaseColor;
uniform vec3 uAltColor;
uniform float uRoughness;
uniform float uMetallic;
uniform vec3 uEmissive;
uniform int uPattern;
uniform vec4 uParam;

uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyColor;
uniform vec3 uGroundColor;

uniform vec3 uLampPos;
uniform vec3 uLampDir;
uniform vec3 uLampColor;

uniform vec3 uMonitorPos;
uniform vec3 uMonitorColor;

uniform vec3 uDoorGlowPos;
uniform vec3 uDoorGlowColor;

uniform sampler2DShadow uShadowMap;
uniform vec4 uContact0;
uniform vec4 uContact1;
uniform vec4 uContact2;
uniform vec4 uContact3;

out vec4 fragColor;

const float PI = 3.14159265;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// 3x3 PCF weicher Schatten über Hardware-Depth-Comparison
float sampleShadow(vec4 lightSpacePos, vec3 N, vec3 L) {
    vec3 proj = lightSpacePos.xyz / lightSpacePos.w;
    proj = proj * 0.5 + 0.5;
    if (proj.x < 0.02 || proj.x > 0.98 || proj.y < 0.02 || proj.y > 0.98 || proj.z > 0.995) {
        return 1.0;
    }
    float ndl = max(dot(N, L), 0.0);
    float bias = max(0.0022 * (1.0 - ndl), 0.0008);
    vec2 texel = vec2(1.0 / 1024.0);
    float sum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec3 coord = vec3(proj.xy + vec2(float(x), float(y)) * texel * 1.45, proj.z - bias);
            sum += texture(uShadowMap, coord);
        }
    }
    return sum / 9.0;
}

// Cook-Torrance GGX PBR-Beleuchtung für eine Lichtquelle
vec3 evalPbrLight(vec3 N, vec3 V, vec3 L, vec3 radiance, vec3 albedo, float rough, float metal) {
    float NdotL = max(dot(N, L), 0.0);
    if (NdotL <= 0.0) return vec3(0.0);
    vec3 H = normalize(V + L);
    float NdotV = max(dot(N, V), 0.001);
    float NdotH = max(dot(N, H), 0.0);
    float HdotV = max(dot(H, V), 0.0);

    float a = rough * rough;
    float a2 = a * a;
    float denom = NdotH * NdotH * (a2 - 1.0) + 1.0;
    float D = a2 / max(PI * denom * denom, 0.0001);

    float k = (rough + 1.0);
    k = (k * k) / 8.0;
    float gv = NdotV / (NdotV * (1.0 - k) + k);
    float gl = NdotL / (NdotL * (1.0 - k) + k);
    float G = gv * gl;

    vec3 F0 = mix(vec3(0.04), albedo, metal);
    vec3 F = F0 + (1.0 - F0) * pow(clamp(1.0 - HdotV, 0.0, 1.0), 5.0);

    vec3 spec = (D * G * F) / max(4.0 * NdotV * NdotL, 0.001);
    vec3 kD = (vec3(1.0) - F) * (1.0 - metal);
    return (kD * albedo / PI + spec) * radiance * NdotL;
}

float contactShadow(vec2 xz, vec4 c) {
    if (c.w <= 0.001) return 1.0;
    float d = length(xz - c.xy) / max(c.z, 0.05);
    float f = smoothstep(0.15, 1.15, d);
    return mix(1.0 - c.w, 1.0, f);
}

// ACES Filmic Tonemapping (nach Best-Practice: kein hartes Clipping, filmische Highlights)
vec3 acesToneMap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCameraPos - vWorldPos);

    vec3 albedo = uBaseColor;
    float rough = clamp(uRoughness, 0.05, 1.0);
    float metal = clamp(uMetallic, 0.0, 1.0);
    vec3 emissive = uEmissive;
    float localAo = 1.0;

    // ---- Prozedurale 3D-Oberflächenmuster ----
    if (uPattern == 1) {
        // Warme Holzdielen auf dem Boden (Weltkoordinaten für echte Maßstäblichkeit)
        float plankZ = vWorldPos.z * 3.1;
        float row = floor(plankZ);
        float rowFrac = fract(plankZ);
        float plankX = vWorldPos.x * 0.72 + mod(row, 2.0) * 0.53;
        float col = floor(plankX);
        float colFrac = fract(plankX);
        float tone = hash21(vec2(col * 3.7, row * 5.1));
        float grain = valueNoise(vec2(vWorldPos.x * 5.0 + tone * 11.0, vWorldPos.z * 26.0));
        albedo = mix(uBaseColor, uAltColor, tone * 0.55 + grain * 0.35);
        float seamZ = smoothstep(0.0, 0.035, rowFrac) * smoothstep(0.0, 0.035, 1.0 - rowFrac);
        float seamX = smoothstep(0.0, 0.014, colFrac) * smoothstep(0.0, 0.014, 1.0 - colFrac);
        float seam = mix(0.68, 1.0, seamZ * seamX);
        albedo *= seam;
        rough = mix(0.36, 0.56, grain);
        // Wand-Boden-Kontaktschatten
        float aoBack = smoothstep(-1.68, -1.15, vWorldPos.z);
        float aoLeft = smoothstep(-2.18, -1.65, vWorldPos.x);
        localAo *= mix(0.66, 1.0, aoBack) * mix(0.72, 1.0, aoLeft);
    } else if (uPattern == 2) {
        // Feine Eichenholzmaserung (Schreibtischplatte, Regal, Tür)
        float g1 = valueNoise(vec2(vUv.x * 16.0, vUv.y * 3.5));
        float g2 = sin(vUv.y * 38.0 + g1 * 6.5) * 0.5 + 0.5;
        albedo = mix(uBaseColor, uAltColor, g2 * 0.28 + g1 * 0.16);
        rough = mix(0.30, 0.46, g2);
    } else if (uPattern == 3) {
        // Gewebter Teppich mit hellem Schmuckrand
        vec2 p = abs(vUv - 0.5) * 2.0;
        float edge = max(p.x, p.y);
        float border = smoothstep(0.76, 0.79, edge) * (1.0 - smoothstep(0.87, 0.90, edge));
        float weave = sin(vUv.x * 120.0) * sin(vUv.y * 90.0) * 0.5 + 0.5;
        albedo = mix(uBaseColor, uAltColor, border * 0.85);
        albedo *= mix(0.90, 1.06, weave);
        rough = 0.92;
    } else if (uPattern == 4) {
        // Feiner Wandputz mit sanfter Raum-Eck-AO (links und rechts)
        float plaster = valueNoise(vWorldPos.xy * 9.0 + vWorldPos.zy * 9.0);
        albedo *= mix(0.96, 1.03, plaster);
        float floorAo = smoothstep(0.0, 0.45, vWorldPos.y);
        float cornerL = smoothstep(0.0, 0.55, length(vec2(vWorldPos.x + 2.15, vWorldPos.z + 1.65)));
        float cornerR = smoothstep(0.0, 0.55, length(vec2(vWorldPos.x - 2.05, vWorldPos.z + 1.65)));
        localAo *= mix(0.74, 1.0, floorAo) * mix(0.74, 1.0, cornerL) * mix(0.74, 1.0, cornerR);
    } else if (uPattern == 5) {
        // Getigertes Katzenfell mit weichen Streifen
        float stripe = sin(vUv.y * 28.0 + sin(vUv.x * 10.0) * 1.4) * 0.5 + 0.5;
        float band = smoothstep(0.58, 0.84, stripe);
        albedo = mix(uBaseColor, uAltColor, band * 0.72);
        // Samtiger Fell-Schimmer am Rand
        float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0);
        albedo += uBaseColor * rim * 0.25;
        rough = 0.82;
    } else if (uPattern == 6) {
        // Weicher Strick-Hoodie
        float knit = sin(vUv.x * 80.0) * sin(vUv.y * 80.0) * 0.5 + 0.5;
        albedo *= mix(0.92, 1.06, knit);
        rough = 0.84;
    } else if (uPattern == 7) {
        // Lebendiger 3D-Monitor-Bildschirm mit Aufgabenliste & Abhaken
        float on = clamp(uParam.x, 0.0, 1.0);
        float typed = uParam.y;   // 0..4 Aufgaben erscheinen
        float checked = uParam.z; // 0..4 Aufgaben werden abgehakt
        // Screen-UV: vUv.x links->rechts, vUv.y unten->oben
        vec2 uv = vUv;
        vec3 screenBg = mix(vec3(0.012, 0.013, 0.018), vec3(0.055, 0.062, 0.088), on);
        vec3 ui = screenBg;
        if (on > 0.02) {
            // Kopfzeile oben
            if (uv.y > 0.82) {
                ui = mix(ui, uAltColor * 0.85, 0.9);
                // 3 kleine Fensterpunkte links
                for (int i = 0; i < 3; i++) {
                    vec2 dotC = vec2(0.08 + float(i) * 0.055, 0.91);
                    if (length((uv - dotC) * vec2(1.75, 1.0)) < 0.03) {
                        ui = vec3(0.95);
                    }
                }
            }
            // 4 Aufgaben-Zeilen mit Checkbox, Textbalken und Haken
            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float rowY = 0.68 - fi * 0.165;
                float appear = clamp(typed - fi, 0.0, 1.0);
                float done = clamp(checked - fi, 0.0, 1.0);
                if (appear > 0.01) {
                    // Kartenhintergrund der Aufgabe
                    if (uv.x > 0.06 && uv.x < 0.94 && abs(uv.y - rowY) < 0.058) {
                        ui = mix(ui, vec3(0.11, 0.125, 0.17), 0.72 * appear);
                    }
                    // Checkbox links
                    vec2 boxC = vec2(0.14, rowY);
                    vec2 bd = abs(uv - boxC) * vec2(1.75, 1.0);
                    float inBox = step(max(bd.x, bd.y), 0.036);
                    float borderBox = inBox * (1.0 - step(max(bd.x, bd.y), 0.024));
                    if (done > 0.5) {
                        ui = mix(ui, uAltColor * 1.35, inBox);
                        // Helle Mitte als Haken-Akzent
                        if (length((uv - boxC) * vec2(1.75, 1.0)) < 0.017) ui = vec3(1.0);
                    } else {
                        ui = mix(ui, uAltColor * 1.1, borderBox * appear);
                    }
                    // Textbalken (wächst beim Tippen)
                    float barLen = (0.44 + mod(fi * 0.17, 0.24)) * appear;
                    if (uv.x > 0.22 && uv.x < 0.22 + barLen && abs(uv.y - rowY) < 0.016) {
                        vec3 barCol = mix(vec3(0.78, 0.83, 0.92), vec3(0.35, 0.40, 0.48), done * 0.7);
                        ui = barCol;
                    }
                }
            }
            // Blinkender Cursor an der aktiven Zeile
            float blink = step(0.5, fract(uTime * 1.8));
            int activeRow = clamp(int(floor(typed)), 0, 3);
            float curY = 0.68 - float(activeRow) * 0.165;
            float curX = 0.22 + (0.44 + mod(float(activeRow) * 0.17, 0.24)) * fract(typed);
            if (typed < 3.98 && abs(uv.x - curX) < 0.008 && abs(uv.y - curY) < 0.028) {
                ui = mix(ui, uAltColor * 1.6, blink);
            }
        }
        // Zarte Glas-Diagonalspiegelung
        float glass = smoothstep(0.35, 0.55, uv.x * 0.6 + uv.y * 0.4) * (1.0 - smoothstep(0.55, 0.68, uv.x * 0.6 + uv.y * 0.4));
        albedo = vec3(0.02);
        rough = 0.14;
        emissive = ui * (1.18 * on + 0.04) + vec3(0.06) * glass;
    } else if (uPattern == 8) {
        // Dynamischer Fenster-Himmel: Tag -> allmählicher Abend mit funkelnden Sternen -> dunkle Nacht -> sichtbarer Sonnenaufgang
        float evening = clamp(uParam.x, 0.0, 1.0);
        float night = clamp(uParam.y, 0.0, 1.0);
        float dawn = clamp(uParam.z, 0.0, 1.0);
        float sunY = uParam.w;

        // Basis-Himmel (Tag -> Abend -> tiefe Nacht -> Morgendämmerung)
        vec3 skyTop = uBaseColor;
        vec3 skyBot = uAltColor;

        // Abendliche Färbung (warmes Gold/Orange am Horizont, tiefes Indigo oben für deutliches Sternenfunkeln)
        vec3 eveTop = vec3(0.032, 0.036, 0.145);
        vec3 eveBot = vec3(1.18, 0.40, 0.13);
        skyTop = mix(skyTop, eveTop, evening * 0.95);
        skyBot = mix(skyBot, eveBot, evening * 0.88);

        // Eigenständige dunkle Nacht (samtiges Mitternachtsblau)
        vec3 nightTop = vec3(0.008, 0.013, 0.044);
        vec3 nightBot = vec3(0.026, 0.032, 0.078);
        skyTop = mix(skyTop, nightTop, night);
        skyBot = mix(skyBot, nightBot, night);

        // Morgendämmerung & Sonnenaufgang (zartes Rosé-Gold steigt vom Horizont auf)
        vec3 dawnTop = vec3(0.15, 0.23, 0.52);
        vec3 dawnBot = vec3(1.32, 0.50, 0.22);
        skyTop = mix(skyTop, dawnTop, dawn * 0.88);
        skyBot = mix(skyBot, dawnBot, dawn * 0.95);

        vec3 sky = mix(skyBot, skyTop, clamp(pow(vUv.y, 0.88) * 1.06, 0.0, 1.0));

        // Feiner warmer Horizontsaum bei Sonnenuntergang & Sonnenaufgang
        float horizBand = exp(-abs(vUv.y - 0.22) * 5.2);
        sky += vec3(1.15, 0.44, 0.16) * horizBand * evening * 0.48 * (1.0 - night);
        sky += vec3(1.35, 0.54, 0.22) * horizBand * dawn * 0.72;

        // Sichtbare Sonne (senkt sich abends langsam ab, verschwindet nachts, geht morgens sichtbar auf!)
        float sunX = mix(0.35 + evening * 0.24, 0.30, dawn);
        vec2 sunPos = vec2(sunX, sunY);
        float dSun = length((vUv - sunPos) * vec2(1.28, 1.0));
        float sunVis = smoothstep(0.02, 0.16, sunY) * (1.0 - night * 0.98);
        if (sunVis > 0.005) {
            vec3 sunHaloCol = mix(vec3(1.45, 0.96, 0.52), vec3(1.75, 0.60, 0.18), max(evening, dawn));
            vec3 sunCoreCol = mix(vec3(2.35, 1.95, 1.25), vec3(2.50, 1.24, 0.52), max(evening, dawn) * 0.80);
            sky += sunHaloCol * exp(-dSun * 4.8) * (0.90 + dawn * 0.65) * sunVis;
            sky += sunCoreCol * smoothstep(0.092, 0.056, dSun) * sunVis;
        }

        // Deutlich funkelnde Sterne am Abend und in der Nacht (zwei Schichten + weich auslaufendes 4-Strahl-Glitzern)
        float starVis = clamp(smoothstep(0.20, 0.72, evening) * 1.05 + night * 1.15 - dawn * 0.85, 0.0, 1.0);
        if (starVis > 0.01 && vUv.y > 0.24) {
            float skyFade = smoothstep(0.24, 0.40, vUv.y);
            // Schicht 1: Kräftige Hauptsterne mit deutlichem Funkeln und sauber auslaufendem Sternkreuz
            vec2 g1 = vUv * vec2(15.0, 10.0);
            vec2 id1 = floor(g1);
            vec2 f1 = fract(g1);
            float s1 = hash21(id1 + vec2(3.7, 1.9));
            if (s1 > 0.65) {
                vec2 center = vec2(
                    0.28 + 0.44 * hash21(id1 + vec2(11.3, 4.1)),
                    0.28 + 0.44 * hash21(id1 + vec2(7.9, 19.3))
                );
                vec2 diff = (f1 - center) * vec2(1.18, 1.0);
                float dist = length(diff);
                float freq = 2.8 + fract(s1 * 17.3) * 4.0;
                float phase = s1 * 83.0;
                float tw = 0.20 + 0.80 * pow(0.5 + 0.5 * sin(uTime * freq + phase), 1.65);
                float core = exp(-dist * dist * 145.0);
                float radialMask = smoothstep(0.28, 0.04, dist);
                float crossRay = (exp(-abs(diff.x) * 26.0 - abs(diff.y) * 5.8)
                                + exp(-abs(diff.y) * 26.0 - abs(diff.x) * 5.8)) * radialMask;
                float sparkle = core * 1.65 + crossRay * 0.60 * tw;
                vec3 starCol = mix(vec3(0.86, 0.93, 1.0), vec3(1.0, 0.95, 0.82), fract(s1 * 9.1));
                sky += starCol * sparkle * tw * starVis * skyFade * 1.85;
            }
            // Schicht 2: Feine funkelnde Hintergrundsterne für tiefen Sternenhimmel
            vec2 g2 = vUv * vec2(29.0, 19.0);
            vec2 id2 = floor(g2);
            vec2 f2 = fract(g2);
            float s2 = hash21(id2 + vec2(23.1, 7.4));
            if (s2 > 0.76) {
                vec2 center2 = vec2(
                    0.26 + 0.48 * hash21(id2 + vec2(2.3, 15.7)),
                    0.26 + 0.48 * hash21(id2 + vec2(13.1, 6.9))
                );
                float dist2 = length((f2 - center2) * vec2(1.2, 1.0));
                float tw2 = 0.24 + 0.76 * pow(0.5 + 0.5 * sin(uTime * (3.4 + fract(s2 * 13.0) * 3.2) + s2 * 91.0), 1.55);
                float pt2 = exp(-dist2 * dist2 * 170.0);
                sky += vec3(0.92, 0.95, 1.0) * pt2 * tw2 * starVis * skyFade * 1.38;
            }
        }

        // Mondsichel (steigt am späten Abend auf, leuchtet ruhig in der Nacht, verblasst beim Sonnenaufgang)
        float moonVis = clamp(smoothstep(0.45, 0.95, evening) * 0.85 + night * 1.05 - dawn * 0.95, 0.0, 1.0);
        if (moonVis > 0.01) {
            vec2 moonPos = vec2(0.74 - night * 0.06, 0.73 + night * 0.05);
            float dm1 = length((vUv - moonPos) * vec2(1.28, 1.0));
            float dm2 = length((vUv - (moonPos + vec2(0.028, 0.016))) * vec2(1.28, 1.0));
            float crescent = smoothstep(0.074, 0.062, dm1) * (1.0 - smoothstep(0.068, 0.056, dm2));
            sky += vec3(0.96, 0.94, 0.84) * crescent * moonVis * 1.65;
            sky += vec3(0.40, 0.48, 0.78) * exp(-dm1 * 8.0) * moonVis * 0.42;
        }

        // Ferne Stadtsilhouette unten am Fenster
        float bx = floor(vUv.x * 7.0);
        float bh = 0.13 + hash21(vec2(bx, 3.1)) * 0.16;
        if (vUv.y < bh) {
            float darkSky = clamp(evening * 0.75 + night, 0.0, 1.0);
            vec3 bCol = mix(vec3(0.11, 0.10, 0.14), vec3(0.014, 0.017, 0.036), darkSky);
            bCol = mix(bCol, vec3(0.08, 0.06, 0.09), dawn * 0.55);
            // Beleuchtete Hausfenster abends und nachts (gehen morgens nach und nach aus)
            vec2 winUv = fract(vec2(vUv.x * 28.0, vUv.y * 26.0));
            float winSeed = hash21(floor(vec2(vUv.x * 28.0, vUv.y * 26.0)));
            float cityLight = clamp(smoothstep(0.32, 0.85, evening) + night * 0.95 - dawn * 0.95, 0.0, 1.0);
            if (winUv.x > 0.25 && winUv.x < 0.75 && winUv.y > 0.25 && winUv.y < 0.75 && winSeed > 0.48 && vUv.y < bh - 0.024) {
                bCol += vec3(1.10, 0.72, 0.25) * cityLight;
            }
            sky = bCol;
        }
        albedo = vec3(0.0);
        emissive = sky * 1.08;
    } else if (uPattern == 9) {
        // Gerahmtes Wandbild rechts (unverändert): minimalistische Sonnenuntergangs-Berge
        vec3 bg = mix(uBaseColor, uAltColor, vUv.y);
        float sunD = length(vUv - vec2(0.68, 0.68));
        bg = mix(bg, vec3(1.1, 0.88, 0.58), smoothstep(0.15, 0.12, sunD));
        float m1 = 0.36 + 0.18 * sin(vUv.x * 5.2 + 0.4) + 0.08 * cos(vUv.x * 11.0);
        float m2 = 0.24 + 0.14 * sin(vUv.x * 4.5 + 2.3);
        if (vUv.y < m1) bg = uBaseColor * 0.42;
        if (vUv.y < m2) bg = uBaseColor * 0.22;
        albedo = bg;
        rough = 0.52;
    } else if (uPattern == 10) {
        // Rundes Zifferblatt der 3D-Wanduhr mit 12 Stundenmarkierungen
        vec2 p = (vUv - 0.5) * 2.0;
        float r = length(p);
        if (r > 0.98) discard;
        float ang = atan(p.y, p.x);
        float tick = abs(sin(ang * 6.0));
        if (r > 0.72 && r < 0.89 && tick < 0.09) {
            albedo = uAltColor;
        } else if (r > 0.91) {
            albedo = mix(albedo, uAltColor, 0.25);
        }
        rough = 0.45;
    } else if (uPattern == 11) {
        // Mechanische Tastatur mit Tastenraster und dezentem Leuchten
        vec2 g = fract(vUv * vec2(15.0, 5.0));
        float gap = smoothstep(0.08, 0.15, g.x) * smoothstep(0.08, 0.15, 1.0 - g.x)
                  * smoothstep(0.10, 0.18, g.y) * smoothstep(0.10, 0.18, 1.0 - g.y);
        albedo = mix(uBaseColor * 0.45, uBaseColor, gap);
        emissive = uAltColor * (1.0 - gap) * uParam.x * 0.55;
        rough = 0.42;
    } else if (uPattern == 12) {
        // Linkes Wandbild: Botanisches Stillleben (Keramikvase & Zweige auf hellem Leinen-Passepartout)
        vec2 uv = vUv;
        // Helles warmes Galerieleinen mit sanfter Struktur
        float linen = valueNoise(uv * 38.0);
        vec3 col = mix(vec3(0.84, 0.80, 0.72), vec3(0.90, 0.86, 0.78), linen);
        // Zarter Innenrand (Passepartout)
        vec2 edge = min(uv, 1.0 - uv);
        float matLine = step(0.055, min(edge.x, edge.y)) * (1.0 - step(0.072, min(edge.x, edge.y)));
        col = mix(col, vec3(0.62, 0.58, 0.51), matLine * 0.55);

        // Sanfter hellgrün-salbeifarbener Dekobogen im Hintergrund
        vec2 archP = (uv - vec2(0.50, 0.44)) * vec2(1.35, 0.85);
        float inArch = step(length(archP), 0.34) * step(0.14, uv.y)
                     + step(abs(uv.x - 0.50), 0.25) * step(0.14, uv.y) * (1.0 - step(0.44, uv.y));
        col = mix(col, vec3(0.64, 0.73, 0.66), clamp(inArch, 0.0, 1.0) * 0.55);

        // Tischlinie unter der Vase
        if (uv.y < 0.15 && uv.x > 0.08 && uv.x < 0.92) {
            col = mix(col, vec3(0.56, 0.46, 0.36), 0.65);
        }

        // Drei geschwungene botanische Stiele aus dem Vasenhals
        float sMid = abs(uv.x - (0.50 + sin((uv.y - 0.36) * 5.0) * 0.035));
        float sLeft = abs(uv.x - (0.50 - (uv.y - 0.36) * 0.58));
        float sRight = abs(uv.x - (0.50 + (uv.y - 0.36) * 0.52));
        vec3 stemCol = vec3(0.08, 0.22, 0.13);
        if (uv.y > 0.35 && uv.y < 0.86 && sMid < 0.011) col = stemCol;
        if (uv.y > 0.36 && uv.y < 0.76 && sLeft < 0.010) col = stemCol;
        if (uv.y > 0.36 && uv.y < 0.78 && sRight < 0.010) col = stemCol;

        // Kräftige botanische Blätter (6 oval-spitze Blätter + 2 warme Blütenknospen)
        vec3 leafDark = vec3(0.07, 0.28, 0.14);
        vec3 leafLight = vec3(0.14, 0.40, 0.21);
        for (int li = 0; li < 6; li++) {
            vec2 lc;
            float ang;
            vec2 sc;
            if (li == 0) { lc = vec2(0.32, 0.62); ang = 0.75; sc = vec2(0.10, 0.052); }
            else if (li == 1) { lc = vec2(0.27, 0.73); ang = 0.42; sc = vec2(0.095, 0.048); }
            else if (li == 2) { lc = vec2(0.49, 0.81); ang = 1.48; sc = vec2(0.105, 0.055); }
            else if (li == 3) { lc = vec2(0.54, 0.67); ang = -0.55; sc = vec2(0.095, 0.048); }
            else if (li == 4) { lc = vec2(0.68, 0.64); ang = -0.68; sc = vec2(0.10, 0.052); }
            else { lc = vec2(0.73, 0.75); ang = -0.38; sc = vec2(0.09, 0.046); }
            vec2 lp = uv - lc;
            float ca = cos(ang); float sa = sin(ang);
            vec2 rot = vec2(lp.x * ca - lp.y * sa, lp.x * sa + lp.y * ca) / sc;
            float dLeaf = length(rot);
            float leafMask = smoothstep(1.02, 0.82, dLeaf);
            if (leafMask > 0.01) {
                vec3 lCol = mix(leafDark, leafLight, smoothstep(-0.8, 0.8, rot.y));
                col = mix(col, lCol, leafMask);
            }
        }
        // Zwei kleine gold-korallenfarbene Blütenpunkte als lebendiger Kontrast
        float b1 = smoothstep(0.044, 0.032, length((uv - vec2(0.38, 0.76)) * vec2(1.0, 1.25)));
        float b2 = smoothstep(0.040, 0.028, length((uv - vec2(0.64, 0.78)) * vec2(1.0, 1.25)));
        col = mix(col, vec3(0.88, 0.46, 0.24), max(b1, b2));

        // Tiefblaue Keramikvase mit schmalem Hals & weichem Glanzlicht
        vec2 vp = (uv - vec2(0.50, 0.25)) * vec2(1.0, 1.12);
        float vaseBody = smoothstep(0.150, 0.138, length(vp * vec2(1.18, 0.95))) * smoothstep(0.140, 0.152, uv.y);
        float vaseNeck = smoothstep(0.046, 0.038, abs(uv.x - 0.50)) * smoothstep(0.315, 0.325, uv.y) * smoothstep(0.415, 0.405, uv.y);
        float vaseLip = smoothstep(0.058, 0.050, abs(uv.x - 0.50)) * smoothstep(0.392, 0.400, uv.y) * smoothstep(0.422, 0.414, uv.y);
        float vaseMask = clamp(max(max(vaseBody, vaseNeck), vaseLip), 0.0, 1.0);
        if (vaseMask > 0.01) {
            float shade = smoothstep(0.64, 0.38, uv.x);
            vec3 vCol = mix(vec3(0.05, 0.15, 0.28), vec3(0.16, 0.36, 0.52), shade);
            float spec = exp(-length((uv - vec2(0.44, 0.28)) * vec2(8.0, 4.5)) * 2.2);
            vCol += vec3(0.28, 0.36, 0.42) * spec;
            col = mix(col, vCol, vaseMask);
        }
        albedo = col;
        rough = 0.56;
    } else if (uPattern == 13) {
        // Geflochtenes Weidenkörbchen (warmes Korbmuster für das Katzenkörbchen)
        float band = sin(vUv.y * 44.0) * 0.5 + 0.5;
        float crossWeave = sin(vUv.x * 78.0 + floor(vUv.y * 14.0) * 1.57) * 0.5 + 0.5;
        float braid = band * 0.55 + crossWeave * 0.45;
        albedo = mix(uAltColor, uBaseColor, braid);
        rough = 0.84;
    }

    // ---- Beleuchtung im linearen Farbraum ----
    vec3 Lo = vec3(0.0);

    // 1. Haupt-Sonnenlicht durch das Fenster mit 3x3 PCF-Schattenkarte
    vec3 Lsun = normalize(uSunDir);
    float shadow = sampleShadow(vLightSpacePos, N, Lsun);
    Lo += evalPbrLight(N, V, Lsun, uSunColor * shadow, albedo, rough, metal);

    // 1b. Sanftes indirektes Raum-Gegenlicht vom hellen Fenster/Boden, damit auch die rechte Wand warm erstrahlt
    vec3 Lbounce = normalize(vec3(-0.68, 0.32, 0.66));
    float bounceNdl = max(dot(N, Lbounce), 0.0);
    Lo += (albedo / PI) * (uSunColor * 0.16 + uSkyColor * 0.24) * bounceNdl * (1.0 - metal * 0.8);

    // 2. Schreibtischlampe (warmer Kegel-Spot mit sanfter Distanzabnahme)
    if (dot(uLampColor, uLampColor) > 0.0001) {
        vec3 toLamp = uLampPos - vWorldPos;
        float dist = length(toLamp);
        vec3 Llamp = toLamp / max(dist, 0.001);
        float cosAng = dot(-Llamp, normalize(uLampDir));
        float cone = smoothstep(0.42, 0.84, cosAng);
        float atten = 1.0 / (0.35 + dist * dist * 1.35);
        Lo += evalPbrLight(N, V, Llamp, uLampColor * cone * atten, albedo, rough, metal);
        // Sanftes warmes Streulicht rund um den Schirm
        Lo += albedo * uLampColor * 0.045 * atten;
    }

    // 3. Dynamischer Monitor-Schein auf Gesicht, Hände und Tisch
    if (dot(uMonitorColor, uMonitorColor) > 0.0001) {
        vec3 toMon = uMonitorPos - vWorldPos;
        float dist = length(toMon);
        vec3 Lmon = toMon / max(dist, 0.001);
        float atten = 1.0 / (0.45 + dist * dist * 1.9);
        Lo += evalPbrLight(N, V, Lmon, uMonitorColor * atten, albedo, rough, metal);
    }

    // 4. Warmes Flurlicht durch die sich öffnende Tür
    if (dot(uDoorGlowColor, uDoorGlowColor) > 0.0001) {
        vec3 toDoor = uDoorGlowPos - vWorldPos;
        float dist = length(toDoor);
        vec3 Ldoor = toDoor / max(dist, 0.001);
        float atten = 1.0 / (0.55 + dist * dist * 1.1);
        Lo += evalPbrLight(N, V, Ldoor, uDoorGlowColor * atten, albedo, rough, metal);
    }

    // 5. Hemisphären-IBL (Himmel oben + warmer Holz-Bounce von unten + Fresnel-Reflex)
    float hemi = N.y * 0.5 + 0.5;
    vec3 ambientDiff = mix(uGroundColor, uSkyColor, hemi) * albedo * (1.0 - metal * 0.85);
    vec3 R = reflect(-V, N);
    float envHemi = R.y * 0.5 + 0.5;
    vec3 envSpecColor = mix(uGroundColor * 0.8, uSkyColor * 1.35, envHemi);
    vec3 F0 = mix(vec3(0.04), albedo, metal);
    float fresnel = pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 5.0);
    vec3 envSpec = envSpecColor * (F0 + (max(vec3(1.0 - rough), F0) - F0) * fresnel) * (1.0 - rough * 0.72) * 0.45;

    // Weiche Boden-Kontaktschatten unter Figuren und Möbeln
    if (vWorldPos.y < 0.14) {
        float floorFade = 1.0 - smoothstep(0.0, 0.14, vWorldPos.y);
        float cAo = contactShadow(vWorldPos.xz, uContact0)
                  * contactShadow(vWorldPos.xz, uContact1)
                  * contactShadow(vWorldPos.xz, uContact2)
                  * contactShadow(vWorldPos.xz, uContact3);
        localAo *= mix(1.0, cAo, floorFade);
    }

    vec3 color = (ambientDiff + envSpec) * localAo + Lo + emissive;
    color *= uExposure;

    // ACES-Tonemapping + sRGB-Gamma
    color = acesToneMap(color);
    color = pow(color, vec3(1.0 / 2.2));
    fragColor = vec4(color, 1.0);
}
"""

        private const val ALPHA_VS = """#version 300 es
precision highp float;
layout(location = 0) in vec3 aPos;
layout(location = 2) in vec2 aUv;
uniform mat4 uModel;
uniform mat4 uViewProj;
out vec2 vUv;
void main() {
    vUv = aUv;
    gl_Position = uViewProj * uModel * vec4(aPos, 1.0);
}
"""

        private const val ALPHA_FS = """#version 300 es
precision highp float;
in vec2 vUv;
uniform vec4 uColor;
uniform int uMode;
uniform float uTime;
out vec4 fragColor;

void main() {
    float a = uColor.a;
    if (uMode == 0) {
        // Weicher Sonnenstrahl / Lichtkegel mit sanftem Randabfall
        float xFade = sin(clamp(vUv.x, 0.0, 1.0) * 3.14159);
        float yFade = smoothstep(0.0, 0.25, vUv.y) * (1.0 - smoothstep(0.55, 1.0, vUv.y));
        a *= xFade * xFade * yFade;
    } else if (uMode == 1) {
        // Zarter Kaffeedampf-Wisp
        vec2 p = (vUv - 0.5) * 2.0;
        p.x += sin(vUv.y * 6.0 + uTime * 2.4) * 0.25;
        float d = length(p);
        a *= smoothstep(1.0, 0.15, d);
    } else {
        // Feines leuchtendes Staubkorn im Sonnenstrahl
        float d = length((vUv - 0.5) * 2.0);
        a *= smoothstep(1.0, 0.1, d);
    }
    if (a < 0.002) discard;
    fragColor = vec4(uColor.rgb, a);
}
"""
    }
}
