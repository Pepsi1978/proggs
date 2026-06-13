# Visuelle Qualität für 3D (PBR/Licht/PostFX/Assets) — Best Practices (Stand 2026-06-13)

> Plattform- und Engine-übergreifende Grundlagen, die 3D-Anwendungen optisch **schön** machen — das „Warum hinter dem schönen Bild". Gilt gleichermaßen für three.js, Babylon.js, Filament, Unity, Unreal, Godot, model-viewer, SceneKit/RealityKit u. a. Die physikalischen Gesetze ändern sich nicht mit der Engine.
>
> **Kerngedanke:** Ein „edel" aussehendes Render entsteht nicht durch teure Hardware, sondern durch **korrekte Physik** (linearer Lichtraum, Energieerhaltung), **gutes Licht** (HDR/IBL statt nackter Punktlichter), **sinnvolles Tonemapping** und **disziplinierte Asset-Pipeline**. Ein „billiges" Render verrät sich durch falsche Gamma, flache Beleuchtung, ausgefressene Highlights und Aliasing.

---

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

---

## §1 — PBR-Grundlagen & Materialien

**Quelle:** Google Filament „Physically Based Rendering in Filament" (`google.github.io/filament/Filament.md.html`, `Materials.md.html`), abgerufen 2026-06-13 — **[offiziell]** (herstellerneutrales, weithin referenziertes PBR-Standardwerk). Khronos glTF 2.0 Spec — **[offiziell]**.

**Worum geht es:** PBR (Physically Based Rendering) modelliert Materialien nach physikalischen Gesetzen (Energieerhaltung, Fresnel, Microfacet-BRDF) statt nach Ad-hoc-Reglern. Resultat: ein Material sieht unter *jeder* Beleuchtung plausibel aus, nicht nur unter der, für die es getuned wurde.

### Der metallic/roughness-Workflow (Industriestandard, von glTF vorgeschrieben)
Vier Kernparameter (Filament):

1. **baseColor** (Albedo): die „reine" Farbe.
   - Bei **Dielektrika** (Nichtmetalle): die diffuse Reflexionsfarbe.
   - Bei **Metallen**: die Farbe der spiegelnden Reflexion (Metalle haben keine diffuse Komponente).
   - **Wichtig:** baseColor darf KEINE eingebackene Beleuchtung enthalten (keine gemalten Schatten/Highlights, kein Ambient Occlusion). Das macht die Beleuchtung sonst doppelt → „billig".
2. **metallic**: nahezu binär. Pure Leiter = **1**, pure Dielektrika = **0**. Filament-Regel: *„use values close to or at 0 and 1"*. Zwischenwerte nur für Übergänge (Rost auf Metall). Falsche Mittelwerte sind der häufigste Anfängerfehler und sehen sofort unecht aus.
3. **roughness**: wahrgenommene Glätte (0 = spiegelglatt, scharfe Reflexe) bis Rauheit (1 = matt, breite Reflexe). Der visuell wichtigste „Look"-Regler. Reale Oberflächen haben **nie** perfekt uniforme roughness — leichte Variation (über eine roughness-Map) ist der Unterschied zwischen „CAD" und „echt".
4. **reflectance** (nur Dielektrika): Stärke der Spiegelung bei senkrechtem Blick (F0). Default ~0.5 entspricht ~4 % Reflexion (IOR 1.5) — passt für die meisten Materialien. Nur selten anfassen.

### Warum das „edel" macht
- **Energieerhaltung**: Ein Material reflektiert nie mehr Licht als es empfängt → keine überstrahlten, „glühenden" Flächen.
- **Fresnel-Effekt**: Streifende Winkel reflektieren stärker (Rand-Aufhellung). Fehlt das, wirkt das Material tot.
- **Interoperabilität**: Filaments „lit"-Modell ist bewusst kompatibel zu Unity, Unreal, Substance Designer, Marmoset Toolbag — ein korrekt gebautes Material sieht überall gleich aus.

### Material-Maps (Texturen)
| Map | Zweck | Color-Space |
|-----|-------|-------------|
| baseColor/Albedo | Grundfarbe | **sRGB** (display-referred) |
| metallic | Metall-Maske | **linear/raw** |
| roughness | Rauheits-Variation | **linear/raw** |
| normal | Oberflächendetail | **linear/raw** |
| AO (Ambient Occlusion) | Selbstverschattung in Spalten | **linear/raw** |
| emissive | Selbstleuchten | **sRGB** |

> glTF packt metallic+roughness+AO oft in **einen** Textur-Kanal (ORM: AO=R, Roughness=G, Metallic=B) zur Effizienz.

---

## §2 — HDR & Image-Based Lighting (IBL)

**Quelle:** learnopengl.com „Diffuse irradiance / IBL" — **[extern]**; Bruno Opsenica „Image Based Lighting with Multiple Scattering" (`bruop.github.io/ibl`) — **[extern]**; Filament-Doku — **[offiziell]**. Abgerufen 2026-06-13.

**Worum geht es:** Statt die Szene mit ein paar Punkt-/Spotlichtern zu beleuchten (sieht hart und künstlich aus), nutzt IBL ein **HDR-Bild der Umgebung** (Environment Map / Skybox) als 360°-Lichtquelle. Jeder Punkt der Umgebung strahlt Licht ab — das liefert die weichen, richtungsabhängigen Reflexe und das natürliche Ambient, die ein Render „echt" wirken lassen.

### Die zwei vorberechneten Komponenten
IBL speichert die Umgebung in zwei aufbereiteten Cubemaps:

1. **Irradiance Map (Diffus):** Die Umgebung wird über die Hemisphäre integriert (cosinus-gewichtete Faltung) → niedrigauflösende Cubemap, die für jede Normale das ankommende diffuse Licht liefert. Sorgt für die weiche Grundbeleuchtung.
2. **Prefiltered Environment Map (Spekular):** Die Umgebung wird für mehrere **roughness**-Stufen vor-gefiltert und in einer **Mip-Kette** gespeichert (Level 0 = scharf/glatt, höhere Level = zunehmend verschwommen/rau). Beim Rendern wählt der Shader das Mip-Level passend zur roughness → korrekte unscharfe Reflexe auf rauen Oberflächen.

### Split-Sum-Approximation (Epic/UE4, heute überall)
Das Spekular-Lichtintegral wird in zwei billig vorberechenbare Teile zerlegt:
- **Teil 1:** die Prefiltered Environment Map (oben).
- **Teil 2:** eine **BRDF-LUT** (2D-Lookup-Texture, abhängig von roughness und Blickwinkel) als Polynom-Approximation.
Zur Laufzeit nur zwei Textur-Lookups → echtzeitfähiges, physikalisch plausibles Environment-Lighting.

### Praxis
- **HDR-Quelle nötig**: Echte HDR-Maps (`.hdr`, `.exr`) haben Werte > 1.0 (Sonne, Fenster). Ein normales sRGB-JPG als „Environment" sieht flach aus, weil die Lichtenergie fehlt.
- **Kostenloses Material**: Polyhaven (CC0-HDRIs) ist Industriestandard für Test-/Produktionsumgebungen.
- **Skybox ≠ Lichtquelle**: Die sichtbare Skybox und die Beleuchtungs-Maps können (und sollten oft) verschieden sein, müssen aber visuell zueinander passen.
- **Ein einziges gutes HDRI** wertet eine Szene mehr auf als zehn manuell platzierte Lichter.

---

## §3 — Tonemapping (ACES vs AgX vs Khronos PBR Neutral)

**Quelle:** Emmett Lalish, „PBR Neutral Tone Mapping" auf `modelviewer.dev/examples/tone-mapping` (Khronos 3D Commerce WG) — **[offiziell]**; Khronos-Pressemitteilung „Khronos PBR Neutral Tone Mapper Released" (2024) — **[offiziell]**. Abgerufen 2026-06-13.

**Worum geht es:** Ein PBR-Render erzeugt **HDR**-Werte (unbeschränkter Helligkeitsbereich, Highlights um Größenordnungen heller als der Rest). Ein Bildschirm kann nur 0–1 (8-bit sRGB) darstellen. Tonemapping komprimiert den unendlichen HDR-Bereich auf 0–1 — „auf eine perzeptuell angenehme Weise".

**Kritisch:** Es gibt **kein** „kein Tonemapping". Lässt man es weg, **clamped** der Encoder hart bei 1.0 — das ergibt eine stückweise-lineare Funktion mit scharfen Knicken → ausgefressene Highlights, Hue-Skews bei glänzenden Objekten, verlorenes 3D-Gefühl. Das ist der Hauptgrund, warum naive Renderings „billig" aussehen.

### Die drei wichtigen Operatoren (Stand 2025/2026)

| Operator | Stärke | Schwäche | Wann nutzen |
|----------|--------|----------|-------------|
| **ACES** | Filmischer Look, gute Highlight-Kompression, branchenüblich seit Jahren | **Starker Saturationsverlust** — canary-yellow, kräftige Grün/Blautöne sind unerreichbar; weißt Paper-White ab | Film, Game, stark-HDR-Szenen, breiter Eingangsgamut |
| **AgX** | Sanftere, „nettere" Highlight-Rolloffs als ACES, modern, weniger aggressive Hue-Skews | Ebenfalls deutlicher Saturationsverlust (gleiches Grundproblem wie ACES) | Film/Game/Look-Development, wenn weicher als ACES gewünscht |
| **Khronos PBR Neutral** | **Treue Farbwiedergabe**: baseColor bleibt unter neutralem (grauem) Licht bis sRGB-Wert **231** exakt 1:1; Hue bleibt erhalten, Saturation maximal bewahrt; eliminiert HDR-Artefakte um Highlights | Bewusst „un-opinionated" — kein filmischer Stil/Grading | **E-Commerce, Produktvisualisierung**, jedes PBR-Render das bisher Tonemapping „abgeschaltet" hatte |

### Der 2024–2026-Trend: PBR Neutral
- **Problem das es löst:** Bei ACES/AgX können Künstler oft eine vom Marketing vorgegebene Produktfarbe **physikalisch nicht** auf den Schirm bringen — das Tonemapping ist der limitierende Faktor. PBR Neutral reproduziert die baseColor faithfully unter neutralem Licht.
- **Wie es funktioniert (technisch):** 1:1-Bereich bis 0.8 (linear) ≙ sRGB 231; darüber eine `1/x`-Kompression (255 → 243); Highlights desaturieren über einen „Path to White" (Desaturation-Rate 0.15). Kein Luminanz-Gewicht → Hue/Saturation bleiben, nur Helligkeit wird skaliert. Shader ist winzig (drei Divisionen, nur für Werte > 0.8).
- **Adoption (Stand 2024–2026):** Three.js, Filament, model-viewer (dort wird es in v4 Default, alt-Name „commerce"), OCIO-Profil + `.cube`-LUT für Blender verfügbar. „Better than AgX, Filmic or ACES 1.x" für Produkt-Workflows (Community, April 2025).
- **Komplementär, nicht ersetzend:** PBR Neutral ergänzt ACES/AgX, ersetzt sie nicht. Faustregel: **Produkt/Treue → PBR Neutral, Stimmung/Film → ACES/AgX.**

### Warum baseColor nicht reines Weiß wird
Physikalisch korrekt: Tonemapping muss Headroom für Highlights reservieren, sonst gibt es keinen Spielraum für glänzende Reflexe. Ein Paper-White-Material darf unter diffusem Licht **nicht** 255 erreichen, sonst verliert die Kugel ihr 3D-Gefühl. Das ist Feature, kein Bug.

---

## §4 — Linear/sRGB Color Management (Gamma)

**Quelle:** Unity Manual „Linear or gamma workflow" — **[offiziell, Engine]**; Autodesk/Arnold „Gamma Correction and Linear Workflow" — **[offiziell]**; Pixar RenderMan „Color Management" — **[offiziell]**. Abgerufen 2026-06-13.

**Worum geht es:** Licht addiert sich in der echten Welt **linear** (doppelt so viele Photonen = doppelter Wert). Render-Mathematik (Beleuchtung, Blending, Filterung) MUSS daher in **linear space** passieren. Bildschirme sind aber **nicht** linear (sRGB/Rec.709 haben eine Gamma-Kurve). Lösung: in linear rechnen, beim Anzeigen eine sRGB-View-Transform anwenden.

### Die zwei eisernen Regeln
1. **Eingang:** Color-/Albedo-Maps sind display-referred → müssen **von sRGB nach linear** dekodiert werden, bevor sie in die Mathematik gehen.
2. **Daten-Maps** (roughness, metallic, normal, height, masks) sind **linear/raw** und dürfen **NIEMALS** gamma-korrigiert werden. Eine fälschlich als sRGB behandelte Normal/Roughness-Map ist ein klassischer „warum sieht das falsch aus"-Bug.

### Symptome eines kaputten Workflows
- Beleuchtung wirkt **überstrahlt/verwaschen**, Farben unnatürlich.
- Lichter „leuchten zu schnell aus", Abstufungen wirken falsch.
- Additives Blending (Bloom, Partikel) sieht falsch hell aus.

### Praxis
- Materials, Viewport und finaler Output müssen dasselbe Color-Management nutzen, damit Preview = Render.
- Zwischenformate (Compositing, Lightmaps) als **EXR (linear float)** speichern, nicht als 8-bit sRGB → kein Präzisionsverlust, kein vorzeitiges Clamping.
- Reihenfolge im Frame: **sRGB-Decode (Input) → linear rendern → Tonemapping → sRGB-Encode (Output)**. Tonemapping (§3) passiert in linear, *vor* dem sRGB-Encode.

---

## §5 — Postprocessing-Stack

**Quelle:** Unity HDRP „Execution order reference" — **[offiziell, Engine]**; Catlike Coding „Custom SRP / Post-Processing" — **[extern]**; „Order of post-process effects" renderingevolution.net — **[extern]**. Abgerufen 2026-06-13.

**Worum geht es:** Postprocessing-Effekte werden als **Stack** nacheinander auf das gerenderte Bild angewandt. Die **Reihenfolge ist entscheidend** — falsche Reihenfolge sieht falsch aus.

### Empfohlene Reihenfolge (typische SDR-Pipeline)
```
Opaque-Render → AA-Resolve → Transparenz
  → SSAO → SSR
  → Depth of Field
  → Motion Blur
  → Bloom
  → Tonemapping (§3)
  → Color Grading / Color Correction
```
> Unity HDRP-Reihenfolge: Distortion → Exposure → Anti-Aliasing → Depth of Field → Motion Blur (→ dann Bloom/Tonemapping/Grading). Prinzip: **szenenbezogene Effekte (in linear, HDR) vor dem Tonemapping**, reine Bild-Looks (Grading) danach.

### Die wichtigsten Effekte
- **SSAO (Screen-Space Ambient Occlusion):** Verdunkelt enge Spalten/Kontaktzonen, wo diffuses Licht schlecht hinkommt. Erdet Objekte, gibt Tiefe. Subtil dosieren — übertriebenes SSAO sieht „schmutzig" aus.
- **SSR (Screen-Space Reflections):** Echtzeit-Spiegelungen aus dem Bildschirminhalt. **Limitierung:** was nicht auf dem Schirm ist, wirft keine Reflexion → fehlende Reflexe an Bildrändern (mit IBL als Fallback kombinieren).
- **Bloom:** Helle Bereiche „bluten" in Nachbarpixel (Bright-Pass → Blur → additives Blending). Simuliert Linsen-Glow. **Muss in HDR/linear** vor dem Tonemapping laufen, sonst falsch. Dezent = edel, übertrieben = billig/Amateur.
- **Depth of Field (DoF):** Unschärfe außerhalb der Fokusebene → cinematischer „Großsensor"-Look.
- **Motion Blur:** Subtiles Verwischen zwischen Frames → flüssigere Wahrnehmung, filmisches Gefühl.

---

## §6 — Schatten (Cascaded Shadow Maps & Contact Shadows)

**Quelle:** Microsoft Learn „Cascaded Shadow Maps" — **[offiziell]**; Babylon.js Doku „Cascaded Shadow Maps" — **[offiziell, Engine]**; MJP „A Sampling of Shadow Techniques" — **[extern]**; NVIDIA CSM-Paper — **[offiziell]**. Abgerufen 2026-06-13.

**Worum geht es:** Schatten erden Objekte. Ohne sie „schweben" Modelle. Aber große Außenszenen (Sonne = Directional Light) brauchen riesige Schattenauflösung nah am Betrachter und grobe weit weg.

### Cascaded Shadow Maps (CSM) — für Directional/Sonne
- **Idee:** Das Kamera-Frustum wird in mehrere Distanz-Slices („Cascades", typ. 3–4) zerlegt; jede bekommt ihre eigene Shadow Map. Nah = hohe Auflösung, fern = niedrige. Eine Map für alles wäre entweder verschwendet oder pixelig.
- **Best Practices:**
  - **Statische Cascade-Intervalle** pro Szenario (nicht pro Frame neu berechnen — das verursacht Flimmern an Schattenkanten).
  - **Stabilisierung:** Orthographische Projektions-Grenzen in **Pixel-Schritten** verschieben → eliminiert Shimmering bei Kamerabewegung.
  - **Filterung kombinieren:** **PCF** (Percentage-Closer Filtering) oder **VSM** → weiche Kanten statt harter Treppchen.

### Contact Shadows (Kontaktschatten)
- Ergänzen CSM dort, wo sie zu grob sind: feine, kurze Schatten genau am Berührungspunkt (Screen-Space-Raymarch im Tiefenpuffer).
- Verhindern den „schwebenden"-Eindruck, den CSM allein bei kleinen Details lassen. Erst die Kombination CSM + Contact Shadows wirkt „edel".

---

## §7 — Anti-Aliasing (MSAA / TAA / FXAA)

**Quelle:** box.co.uk „FXAA vs TAA vs MSAA vs DLAA" — **[extern]**; Hardware Times Vergleich — **[extern]**. Konsens-Stand 2024–2026. Abgerufen 2026-06-13.

**Worum geht es:** Geometrie-Kanten und Shader-Details erzeugen ohne AA Treppchen und Flimmern in Bewegung — das absolute Kennzeichen eines „billigen" Renders.

| Verfahren | Funktionsweise | Kosten | Qualität | Verdikt 2026 |
|-----------|----------------|--------|----------|--------------|
| **TAA** (Temporal) | Sammelt/mittelt mehrere Frames über die Zeit, mit Motion-Vektoren | ~2–5 % FPS | Sehr gut, auch in Bewegung; glättet Geometrie **und** Shader/Textur-Aliasing | **Standard für die meisten modernen Pipelines.** Deferred-kompatibel. Bei schlechtem Tuning leicht weichzeichnend/ghosting. |
| **MSAA** (Multisample) | Mehrfach-Sampling nur an Geometriekanten | teuer (steigt mit Sample-Zahl) | Sehr scharfe Kanten, aber **kein** Shader-/Textur-AA | **Veraltet für moderne Titel.** Funktioniert NICHT mit Deferred Rendering. Nur Forward/alte DX9-DX10-Pipelines, VR. |
| **FXAA** (Fast Approximate) | Einfacher Post-Filter auf Kanten im fertigen Bild | minimal | Schwächste; verwischt Feindetail | **Budget-Option**, wenn TAA zu teuer ist (Mobile/Low-End). |
| **DLAA/DLSS u. ä.** | ML-basiert, native Auflösung | GPU-abhängig | Höchste Qualität | Wenn Hardware vorhanden — Qualität über FPS. |

**Faustregel:** Moderne Echtzeit-3D → **TAA** als Default; **FXAA** als billiger Fallback; **MSAA** nur bei Forward-Rendering/VR; offline/Pathtracing nutzt ohnehin Supersampling.

---

## §8 — Asset-Pipeline (glTF / KTX2 / Draco / Normal Maps)

**Quelle:** Khronos „Universal GPU Compressed Textures for glTF using KTX 2.0" (PDF) — **[offiziell]**; glTF-Transform Doku (`gltf-transform.dev`, Don McCurdy) — **[offiziell/Referenz-Tool]**; DeepWiki „Texture Compression with KTX2 and Basis Universal" — **[extern]**; polycount „Normal Map Technical Details" — **[extern]**; glTF 2.0 Spec — **[offiziell]**. Abgerufen 2026-06-13.

### glTF 2.0 — das „JPEG der 3D-Welt"
- **Standard-Austauschformat** für Echtzeit-3D (von Khronos, herstellerneutral). `.gltf` (JSON + externe Dateien) oder `.glb` (alles in einer Binärdatei — bevorzugt für Auslieferung).
- Definiert metallic/roughness-PBR nativ, schreibt **MikkTSpace** + **OpenGL-Konvention** für Tangenten/Normalen vor → konsistente Beleuchtung über alle konformen Renderer.

### Textur-Kompression: KTX2 + Basis Universal (`KHR_texture_basisu`)
- **Problem:** Eine 200 KB PNG belegt im GPU-Speicher **20 MB+** VRAM (dekomprimiert). KTX2/Basis bleibt **GPU-komprimiert im Speicher** → **~10× weniger VRAM**.
- **Container** KTX2 + **Codec** Basis transcodiert zur Laufzeit in das GPU-native Format (BC7, ASTC, ETC2 …).
- **Zwei Modi:**
  - **UASTC** — höhere Qualität, größere Dateien → **Normal Maps & Hero-Texturen**.
  - **ETC1S** — kleiner, akzeptable Qualität → **Diffuse/Environment & Sekundär-Texturen**.
  - Startregel: *UASTC für Normals, ETC1S für Diffuse.*
- **Verbreitung 2025/2026:** KTX2/Basis ist im Web (three.js, Babylon, model-viewer, Cesium) faktischer Standard; `gltf-transform` und `gltfpack` als Pipeline-Tools etabliert.

### Mesh-Kompression: Draco (`KHR_draco_mesh_compression`)
- Komprimiert Geometrie (Positionen, Normalen, UVs, Indizes) verlustbehaftet/-frei → drastisch kleinere Dateien & schnellerer Download. De-facto-Standard für Mesh-Kompression in glTF.
- **Trade-off:** Dekompression kostet CPU-Zeit beim Laden. Für sehr kleine Meshes lohnt es nicht.

### Kombinierte Pipeline (Referenz)
```
gltf-transform optimize model.glb output.glb \
  --texture-compress ktx2 \
  --compress draco
```
> Beispiel FlightHelmet: 43,06 MB → 29,37 MB (−32 %), v. a. durch Color-Textur-Kompression.

### Normal Maps — die häufigste Stolperfalle
- **Konvention:** Nur der **grüne Kanal (Y)** unterscheidet sich.
  - **OpenGL / glTF / Blender / Maya / Unity / Marmoset:** **Y+** (grün hell = Oberfläche zeigt nach oben).
  - **DirectX / Unreal / 3ds Max / CryEngine / Source:** **Y−** (grün invertiert).
- **Fix bei „eingedellt statt gewölbt"/falscher Beleuchtung:** grünen Kanal invertieren.
- **Tangenten:** Renderer **muss** dieselbe Tangentenbasis nutzen wie der Baker — sonst falsche Beleuchtung, besonders an UV-Naht-Kanten. **MikkTSpace** ist der gemeinsame Standard (von glTF vorgeschrieben).
- Normal Maps immer **linear/raw** behandeln, NIE als sRGB dekodieren (§4).

---

## §9 — „Look & Feel"-Checkliste: edel vs billig

| Aspekt | „Billig" (Amateur-Render) | „Edel" (professionell) |
|--------|---------------------------|------------------------|
| **Beleuchtung** | Ein, zwei harte Punktlichter; flach, tot | HDR-IBL/Environment-Map; weiche, richtungsabhängige Reflexe (§2) |
| **Gamma/Color** | Falsch (alles in sRGB gerechnet) → verwaschen/überstrahlt | Korrekter linearer Workflow, sRGB nur am Ausgang (§4) |
| **Tonemapping** | Keins → ausgefressene weiße Highlights, Hue-Skews | PBR Neutral / ACES / AgX; Highlights desaturieren sauber (§3) |
| **Materialien** | metallic-Mittelwerte, uniforme roughness, gemalte Schatten in baseColor | metallic 0/1, roughness-Variation per Map, saubere baseColor (§1) |
| **Schatten** | Keine oder harte, flimmernde; Objekte „schweben" | Stabile CSM + Kontaktschatten; weich gefiltert (PCF) (§6) |
| **Kanten** | Treppchen, Flimmern in Bewegung | TAA, glatte Kanten in Ruhe & Bewegung (§7) |
| **Reflexe** | Spiegelglatt oder gar keine | roughness-abhängige Prefiltered-IBL + dezentes SSR (§2, §5) |
| **Postprocessing** | Übertriebener Bloom, „neon"-SSAO | Subtiler Bloom, dezentes AO/DoF — man merkt es kaum, aber es fehlt sofort wenn weg (§5) |
| **Erdung/Detail** | Objekte wirken isoliert, „freigestellt" | Kontaktschatten, AO in Spalten, konsistentes IBL-Ambient |
| **Grundregel** | „mehr ist mehr" | **Subtilität.** Edel entsteht durch viele kleine, physikalisch korrekte Effekte in Maßen — nicht durch einen auffälligen. |

**Der eine Satz:** Schöne 3D-Renders entstehen aus **korrekter Physik** (linear, energieerhaltend, PBR), **gutem Licht** (HDR/IBL), **sinnvollem Tonemapping** und **disziplinierter Asset-Pipeline** — nicht aus auffälligen Effekten.

---

## Quellenübersicht

- **[offiziell]** Google Filament — *Physically Based Rendering in Filament* & *Materials Guide* — `google.github.io/filament/Filament.md.html`, `Materials.md.html`
- **[offiziell]** Khronos — glTF 2.0 Spec, *KTX 2.0 / Basis Universal* (PDF), *PBR Neutral Tone Mapper* (Pressemitteilung 2024)
- **[offiziell]** Emmett Lalish / Khronos 3D Commerce — *PBR Neutral Tone Mapping* — `modelviewer.dev/examples/tone-mapping`
- **[offiziell]** glTF-Transform — `gltf-transform.dev` (Don McCurdy)
- **[offiziell, Engine]** Unity Manual (Linear Workflow, HDRP Execution Order), Babylon.js (CSM), Microsoft Learn (CSM), Pixar RenderMan (Color Management), Autodesk Arnold (Gamma/Linear)
- **[extern]** learnopengl.com (IBL), bruop.github.io (IBL Multiple Scattering), polycount Wiki (Normal Maps), box.co.uk / Hardware Times (AA-Vergleich), Catlike Coding (Post-Processing)

*Alle Quellen abgerufen am 2026-06-13.*

---

## Bezug ↔ Bug-Almanach

Die typischen Fehler zu diesen Best Practices — „was geht schief und wie sieht es aus" — stehen im Bug-Almanach: `bugs/assets/3d-visual-quality.md`.

| Best-Practices § | Thema | Bug-Almanach § |
|------------------|-------|----------------|
| §1 PBR-Materialien | metallic-Mittelwerte, eingebackene Schatten in baseColor | §12 |
| §2 HDR/IBL | SSR-Randabriss → IBL-Fallback | §9 |
| §3 Tonemapping | Highlights ausgefressen (Tonemapping aus); baseColor wird nicht 255 | §3, §4 |
| §4 Linear/sRGB | verwaschen/überstrahlt (kein linearer Workflow); Daten-Maps als sRGB | §2, §11 |
| §5 Postprocessing | Bloom-Reihenfolge falsch; Emissive ohne HDR-Strength | §13 |
| §6 Schatten/CSM | CSM-Shimmering; Shadow Acne / Peter Panning | §5, §6 |
| §7 Anti-Aliasing | MSAA in Deferred inkompatibel; TAA-Ghosting | §7, §8 |
| §8 Asset-Pipeline | Normal-Map Y-Konvention/Tangenten; gltf-transform WebP-Bug; KTX2 sRGB-Tag | §1, §10, §11 |
| §9 edel vs billig | Sammelt die Symptome aller obigen Fehler | §1–§13 |
