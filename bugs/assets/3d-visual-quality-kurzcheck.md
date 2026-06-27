# Visuelle Qualität für 3D (PBR/Licht/PostFX/Assets) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Normal Map wirkt „eingedellt statt gewölbt", Beleuchtung kippt | Y-Konvention prüfen: glTF/OpenGL = Y+, DirectX/Unreal = Y−. Falsch → grünen Kanal invertieren | §1 |
| 2 | Bild verwaschen/überstrahlt, Lichter „leuchten zu schnell aus" | Kein linearer Workflow. In linear rendern, sRGB nur am Output. Color-Maps sRGB, Daten-Maps (roughness/metal/normal) NIE als sRGB dekodieren | §2 |
| 3 | Highlights brennen weiß aus, Farben kippen ins Bunte | Tonemapping fehlt → hartes Clamp bei 1.0. PBR Neutral (Produkt) / ACES / AgX aktivieren | §3 |
| 4 | baseColor wird nicht reines Weiß (255) | Kein Bug — Tonemapping reserviert Highlight-Headroom. Mit PBR Neutral bleibt baseColor bis sRGB 231 1:1 | §4 |
| 5 | Schattenkanten flimmern beim Kamerafahren (Shimmering) | CSM-Frustum wird pro Frame neu berechnet. Statische Cascade-Intervalle + Texel-Snapping | §5 |
| 6 | Schatten-Streifen auf Flächen (Shadow Acne) | Depth-Bias fehlt/zu klein. Slope-Scaled Bias + ggf. Front-Face-Culling beim Depth-Pass | §6 |
| 7 | Schatten „löst sich vom Fuß" (Peter Panning) | Bias zu groß. Bias reduzieren, Front-Face-Culling statt riesigem Bias, dünne Geo vermeiden | §6 |
| 8 | Treppchen/Flimmern trotz MSAA in Deferred-Pipeline | MSAA ist inkompatibel mit Deferred. Auf TAA wechseln | §7 |
| 9 | TAA: Geisterbilder/Schlieren hinter bewegten Objekten | Fehlende/falsche Motion-Vektoren oder keine History-Rejection. Variance-Clipping + Depth-Reject | §8 |
| 10 | SSR-Reflexe fehlen an Bildrändern / hinter Objekten | SSR sieht nur Screen-Inhalt. IBL als Fallback kombinieren | §9 |
| 11 | `gltf-transform --texture-compress webp` → „Invalid JPG, marker table corrupted" | Tool-Bug bei WebP-Pfad. KTX2 statt WebP nutzen | §10 |
| 12 | KTX2-Normal-Map sieht falsch beleuchtet/zu dunkel aus | Als sRGB statt linear getaggt. `--assign_oetf linear --assign_primaries none` beim Encoden | §11 |
| 13 | metallic-Mittelwerte, baseColor mit eingebackenen Schatten | Häufigster Anfängerfehler → sofort „billig". metallic 0 oder 1; baseColor ohne Licht/AO | §12 |
| 14 | Emissive Material leuchtet nicht / kein Bloom | emissiveFactor clamped auf [0,1]. KHR_materials_emissive_strength > 1 setzen, Bloom in HDR/linear | §13 |
| 15 | Bloom sieht falsch hell / Partikel/Additiv falsch | Effekt läuft nach Tonemapping statt davor. Bloom muss in HDR/linear VOR Tonemapping | §13 |
| 16 | UV-Naht-Kanten falsch beleuchtet | Renderer-Tangenten ≠ Baker-Tangenten. MikkTSpace beidseitig erzwingen | §1 |
