@file:Suppress("KotlinConstantConditions")
package com.entropyjournal.ui.components.energyboard

import org.intellij.lang.annotations.Language

/**
 * AGSL-Shader-Definitionen fuer das Energy Board.
 * AGSL (Android Graphics Shader Language) ist verfuegbar ab API 33 (Android 13).
 * Auf aelteren Geraeten faellt der Overlay auf den klassischen Canvas-Renderer zurueck.
 *
 * Quellen:
 * - Researcher A (2026-05-01): Top-3 AGSL-Techniken — SDF-Glow Multi-Layer-Bloom,
 *   FBM-Lightning, Voronoi-Veins
 * - Shadertoy WscyWB (Lightning), 3s3GDn (Glow Tutorial)
 * - drinkthestars/shady, JumpingKeyCaps/DynamicVisualEffectsAGSL
 */
object EnergyShaders {

    /**
     * Standalone-Variante des SDF-basierten Card-Border-Glows — ohne shader-Input.
     * Liefert reinen Glow als Output (transparent innerhalb der Karte). Wird via
     * ShaderBrush + drawRect im DrawScope additiv ueber die Karte gemalt.
     * HDR-Look durch clamp(value, 0.0, 3.0) statt 1.0.
     */
    @Language("AGSL")
    val CARD_BORDER_GLOW = """
        uniform float2 size;
        uniform float cornerRadius;
        uniform float time;
        uniform float glowRadius;
        layout(color) uniform half4 glowColor;
        layout(color) uniform half4 innerGlowColor;

        float roundedRectSDF(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 coord) {
            float2 uv = (coord - size * 0.5);
            float2 cardSize = size * 0.5 - float2(glowRadius, glowRadius);
            float dist = roundedRectSDF(uv, cardSize, cornerRadius);

            float pulse = sin(time * 2.5) * 0.5 + 0.5;
            float strongPulse = sin(time * 1.7) * 0.3 + 0.7;

            // Innerhalb der Karte: nur dezenter Innenrand-Glow (transparent in der Mitte)
            if (dist <= 0.0) {
                float innerGlow = exp(dist * 0.06) * 0.35 * strongPulse;
                return innerGlowColor * innerGlow;
            }

            // Ausserhalb: drei Bloom-Schichten
            float glow1 = exp(-dist / (glowRadius * 0.15)) * (1.2 + pulse * 0.8);
            float glow2 = exp(-dist / (glowRadius * 0.40)) * (0.7 + pulse * 0.4);
            float glow3 = exp(-dist / (glowRadius * 1.0))  * (0.35 + pulse * 0.2);

            float totalGlow = glow1 + glow2 * 0.85 + glow3 * 0.6;
            totalGlow = clamp(totalGlow, 0.0, 3.0);

            half4 finalGlow = glowColor * totalGlow;
            finalGlow.rgb = finalGlow.rgb * (1.0 + strongPulse * 0.4);
            return finalGlow;
        }
    """.trimIndent()

    /**
     * FBM-Lightning-Shader fuer die Hauptlinie und Lichtenberg-Adern.
     * Erzeugt eine vertikal verlaufende Plasma-Stromader mit fraktal verzweigten
     * Sub-Aesten via Fractal Brownian Motion.
     */
    @Language("AGSL")
    val FBM_LIGHTNING = """
        uniform float2 resolution;
        uniform float time;
        uniform float intensity;
        layout(color) uniform half4 plasmaColor;
        layout(color) uniform half4 hotCore;

        float hash(float2 p) {
            p = fract(p * float2(234.34, 435.345));
            p += dot(p, p + 34.23);
            return fract(p.x * p.y);
        }

        float noise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i), hash(i + float2(1, 0)), u.x),
                mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x),
                u.y
            );
        }

        float fbm(float2 p) {
            float value = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 4; i++) {
                value += amp * noise(p);
                p *= 2.1;
                amp *= 0.5;
            }
            return value;
        }

        float bolt(float2 uv, float2 a, float2 b, float thickness) {
            float2 dir = b - a;
            float len = length(dir);
            float2 dN = dir / len;
            float t = clamp(dot(uv - a, dN) / len, 0.0, 1.0);
            float2 closest = a + dN * t * len;
            float warp = (fbm(float2(t * 5.0 + time * 0.7, t * 2.5)) - 0.5);
            float2 perp = float2(-dN.y, dN.x);
            closest += perp * warp * 0.06;
            float d = length(uv - closest);
            float core = exp(-d / (thickness * 0.25)) * 2.5;
            float glow = exp(-d / thickness) * 0.7;
            return core + glow;
        }

        half4 main(float2 coord) {
            float2 uv = coord / resolution.xy;
            uv = uv * 2.0 - 1.0;
            uv.x *= resolution.x / resolution.y;

            float energy = 0.0;
            // Hauptlinie vertikal mit leichtem Pulsieren
            energy += bolt(uv, float2(0.0, -1.0), float2(0.0, 1.0),
                           0.05 + sin(time * 3.0) * 0.012);

            // Sub-Aeste — zucken in 6er-Steps
            float bt = floor(time * 8.0);
            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float st = hash(float2(fi, bt));
                float2 sa = float2(0.0, -1.0 + st * 2.0);
                float ang = (hash(float2(fi + 10.0, bt)) - 0.5) * 1.6;
                float2 sb = sa + float2(ang * 0.5,
                                          hash(float2(fi + 20.0, bt)) * 0.4);
                energy += bolt(uv, sa, sb, 0.018) * 0.55;
            }

            energy *= intensity;
            half3 col = mix(plasmaColor.rgb, hotCore.rgb,
                             clamp(energy - 1.2, 0.0, 1.0));
            col *= energy;
            // Tone-Mapping fuer echten Bloom
            col = 1.0 - exp(-col * 1.4);
            return half4(col, clamp(energy * 0.8, 0.0, 1.0));
        }
    """.trimIndent()

    /**
     * Voronoi-basiertes Adermuster fuer den Karten-Innenraum.
     * Liefert ein subtiles, pulsierendes Cyan-Netz im Hintergrund der aktiven Karte.
     */
    @Language("AGSL")
    val CARD_VEIN = """
        uniform shader card;
        uniform float2 size;
        uniform float time;
        layout(color) uniform half4 veinColor;

        float2 hashGrid(float2 p) {
            p = float2(dot(p, float2(127.1, 311.7)),
                       dot(p, float2(269.5, 183.3)));
            return -1.0 + 2.0 * fract(sin(p) * 43758.5453);
        }

        float voronoiEdge(float2 uv, float scale) {
            uv *= scale;
            float2 i = floor(uv);
            float2 f = fract(uv);
            float minDist1 = 10.0;
            float minDist2 = 10.0;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    float2 nb = float2(float(x), float(y));
                    float2 pt = hashGrid(i + nb) * 0.5 + 0.5;
                    float d = length(f - nb - pt);
                    if (d < minDist1) { minDist2 = minDist1; minDist1 = d; }
                    else if (d < minDist2) { minDist2 = d; }
                }
            }
            return minDist2 - minDist1;
        }

        half4 main(float2 coord) {
            float2 uv = coord / size;
            half4 cardColor = card.eval(coord);
            float edge = voronoiEdge(uv, 5.0);
            float vein = exp(-edge * 90.0) * 0.55;
            vein *= 0.6 + 0.4 * sin(time * 1.8 + uv.x * 4.0);
            return cardColor + veinColor * vein;
        }
    """.trimIndent()
}
