# Visuelle Qualität für 3D (PBR/Licht/PostFX/Assets) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Material wirkt „plastik"/unecht | Echtes **PBR metallic/roughness**: metallic nahe 0 **oder** 1 (nichts dazwischen), realistische roughness, baseColor ohne eingebackene Schatten/Highlights | §1 |
| 2 | Szene wirkt flach, tot, „CAD-haft" | **HDR-Environment-Map (IBL)** als Lichtquelle statt einzelner Punktlichter — liefert weiche, richtungsabhängige Reflexe und Ambient | §2 |
| 3 | Highlights brennen weiß aus, Farben kippen | **Tonemapping** zwingend (nie ungemappt clampen). Produkt/E-Commerce → **Khronos PBR Neutral**; Film/Game → **ACES** oder **AgX** | §3 |
| 4 | Beleuchtung wirkt „verwaschen" oder überstrahlt | **Linearer Workflow**: Rendern in linear, Anzeige in sRGB. Color-Maps = sRGB, Daten-Maps (roughness/metal/normal) = linear/raw | §4 |
| 5 | Bild wirkt „roh" / nicht filmisch | **Postprocessing-Stack** in richtiger Reihenfolge: SSAO → SSR → DoF → Motion Blur → Bloom → Tonemapping → Color Grading | §5 |
| 6 | Objekte „schweben", keine Erdung | **Kontaktschatten** + saubere **Cascaded Shadow Maps** (CSM) für Sonne/Directional; stabilisiert gegen Flimmern | §6 |
| 7 | Treppchen an Kanten, Flimmern in Bewegung | **TAA** ist 2026 der Standard (deferred-kompatibel). FXAA = Budget, MSAA nur Forward/alte Pipelines | §7 |
| 8 | Modelle zu groß, VRAM/Ladezeit explodiert | **glTF 2.0** + **KTX2/Basis** (GPU-komprimiert, ~10× weniger VRAM) + **Draco** (Mesh) | §8 |
| 9 | Normal Map „invertiert"/Beleuchtung falsch | Konvention beachten: glTF/OpenGL = **Y+** (grün oben). Falsch → grünen Kanal invertieren. Tangenten via **MikkTSpace** | §8 |
| 10 | „Billig" vs „edel" — was unterscheidet es? | Subtilität: weiches IBL, leichte roughness-Variation, dezenter Bloom, korrekte Gamma, Kontaktschatten, keine ausgefressenen Weißtöne | §9 |
| 11 | baseColor erscheint nicht als reines Weiß | Physikalisch korrekt: Tonemapping reserviert Headroom für Highlights. Mit PBR Neutral bleibt baseColor bis ~231 (sRGB) 1:1 | §3 |
| 12 | Reflexe wirken billig/spiegelglatt | Specular-IBL über **Prefiltered Environment Map** + roughness-abhängige Mip-Kette (Split-Sum-Approximation) | §2 |
