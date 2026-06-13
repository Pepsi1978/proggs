# 3D auf macOS (Metal/SceneKit/RealityKit) — Best Practices (Stand 2026-06-13, Version Metal 4 / macOS 26 "Tahoe", Xcode 26)

> Fokus: optisch sehr schoene, native 3D-Anwendungen auf macOS mit Swift — sowohl echtzeit/interaktiv
> als auch fotorealistische Produkt-/Szenenvisualisierung. Quellenpriorisierung: Apple offiziell `[offiziell]`
> schlaegt alles; externe Quellen `[extern]` nur als Ergaenzung, ueberstimmen nie Offizielles.
>
> Stand-Hinweis: macOS 26 ("Tahoe", Versionsschema 26.x) ist die aktuelle Generation. Metal 4 wurde
> auf der WWDC25 vorgestellt und auf der WWDC26 weiter ausgebaut (M5 Pro/Max Neural Accelerators,
> neuronales MetalFX-Upscaling, Game Porting Toolkit 4). SceneKit ist seit WWDC25 **soft-deprecated**.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neues 3D-Projekt, welches Framework? | **Nicht SceneKit** (soft-deprecated). Hohes Niveau/Szenengraph → RealityKit; volle Kontrolle/Custom-Renderer/Game → Metal 4 direkt. | §1 |
| 2 | Bestehende SceneKit-App | Laeuft weiter (nur Critical-Bug-Fixes), aber neue Features → RealityKit migrieren. WWDC25-Migrationspfad nutzen. | §1, §2 |
| 3 | Fotorealistisches Produkt-Rendering | RealityKit mit PBR-Materialien + IBL (HDR-Environment) + EDR-Ausgabe; oder Custom Metal-PBR fuer maximale Bildqualitaet. | §3, §4 |
| 4 | Echtzeit/interaktiv, hohe FPS | Metal 4 + MetalFX (neuronales Temporal-Upscaling, ggf. Frame-Interpolation) + GPU-driven Rendering (Argument Buffers/Heaps). | §1, §5, §6 |
| 5 | Realistische Beleuchtung | Image-Based Lighting mit HDR-Lat/Long-Map; in RealityKit `ImageBasedLightComponent` + Receiver; in Metal vorberechnete Irradiance/Prefilter-Maps. | §4 |
| 6 | HDR/Display P3 auf dem Mac voll ausnutzen | EDR aktivieren: `CAMetalLayer.wantsExtendedDynamicRangeContent = true`, `rgba16Float`, `extendedLinearDisplayP3`, Headroom via `maximumExtendedDynamicRangeColorComponentValue`. | §7 |
| 7 | Asset-Import (glTF/OBJ/FBX) | RealityKit/RCP wollen **USD(Z)** zur Laufzeit, nicht glTF. Vorab via Reality Converter / `usdzconvert` / DCC-USD-Export konvertieren. Model I/O fuer Metal-Pipelines. | §8 |
| 8 | Postprocessing (Bloom, Tonemapping) | RealityKit: `postProcess`-Callback mit Metal-Compute / Metal Performance Shaders. Metal: eigener Compute-Pass nach dem Render. | §5 |
| 9 | Prozedurale/dynamische Geometrie in RealityKit | `LowLevelMesh` + `LowLevelTexture` (macOS 15+), GPU-Update per Metal-Compute. | §2, §6 |
| 10 | Haeufige Fallen vermeiden | USD-Material-Linkage, ARView verschluckt Maus-Events, Tahoe-EDR/Auto-Brightness-Bug, IBL-Component+Receiver noetig. | §9 |

---

## §1 Framework-Wahl: Metal 4 vs. SceneKit vs. RealityKit

**Das Wichtigste zuerst — SceneKit ist soft-deprecated.** Auf der WWDC25 (Session "Bring your SceneKit
project to RealityKit", 288) hat Apple SceneKit ueber alle Plattformen in den Wartungsmodus versetzt:
bestehende Apps laufen weiter, erhalten aber nur noch **kritische Bugfixes**, **keine neuen Features**.
Es gibt aktuell keine harte Deprecation (Entfernung), aber fuer Neuprojekte rechnet Apple klar mit
RealityKit als Nachfolger. Konsequenz: **Kein Neuprojekt mehr auf SceneKit aufbauen.**

Die drei Optionen im Vergleich:

- **RealityKit** — Apples moderner 3D-Szenengraph, eng mit SwiftUI (`RealityView`) verzahnt. Seit WWDC24
  auf iOS/iPadOS/**macOS**/visionOS/tvOS verfuegbar (nicht mehr nur visionOS). Liefert Entity-Component-System
  (ECS), PBR-Materialien, IBL, Schatten, Physik, Animationen, `ShaderGraphMaterial` (via Reality Composer Pro).
  Auf macOS laesst sich RealityKit **rein virtuell** (non-AR) betreiben — `RealityView` nutzt dort
  `RealityViewCameraContent`, Kamera kann auf "virtual" gesetzt werden. Beste Wahl fuer die meisten
  hochwertigen 3D-Apps, Produktvisualisierung und interaktive Szenen ohne eigenen Renderer.
- **Metal 4** — Low-Level-GPU-API, volle Kontrolle ueber Render-Pipeline, Shader, Compute. Wahl fuer
  Game-Engines, Custom-Renderer, maximale Bildqualitaet/Performance und alles, was der Szenengraph
  nicht hergibt. Metal 4 (ab WWDC25) bringt u.a. vereinheitlichtes Command-Encoding, neue Ray-Tracing-
  Features, Tensor-Ressourcen und ML-Inferenz direkt im Shader (Lighting/Materials/Geometry zur Laufzeit
  per neuronalem Netz). `metal-cpp` fuer C++-Anbindung.
- **SceneKit** — nur fuer Bestandscode. Bietet `lightingModel = .physicallyBased`, `lightingEnvironment`
  (HDR-IBL), HDR-Kamera. Solide, aber eingefroren. Migration zu RealityKit empfohlen.

**Faustregel:** Szenengraph + SwiftUI-Integration gewuenscht → RealityKit. Eigener Renderer / Engine /
extreme Kontrolle → Metal 4. SceneKit nur warten, nicht neu beginnen.

> Quelle: Apple WWDC25 "Bring your SceneKit project to RealityKit" (Session 288), Apple "What's new in Metal"
> (developer.apple.com/metal/whats-new), Apple Developer Forums (697680, 795520, 803619). Datum 2026-06-13. `[offiziell]`
> Ergaenzend: dev.to/arshtechpro WWDC25-Migration-Guide, threads.com Diskussion. `[extern]`

## §2 RealityKit auf macOS im Detail (non-AR Rendering, RealityView, Migration)

- **RealityView** ist seit WWDC24 der empfohlene Einstieg auf macOS. In SwiftUI eingebettet, Inhalt vom Typ
  `RealityViewCameraContent`. Fuer Standalone-3D (kein AR): Kamera als "virtual" konfigurieren — damit
  rendert RealityKit eine reine virtuelle Szene ohne Kamerabild.
- **Migration von ARView → RealityView** ist empfohlen. `ARView` war der aeltere Einstieg; auf macOS hatte
  `ARView` zudem das Problem, Maus-Events zu verschlucken, was SwiftUI-Gesten erschwerte (siehe §9).
- **Reality Composer Pro (RCP)** ist das zentrale Authoring-Tool: USDZ importieren → automatisch
  `PhysicallyBasedMaterial` anlegen → Stats pruefen (Dreieckszahlen, Texturen) → Szene bauen → als
  `.reality` exportieren. `ShaderGraphMaterial` (MaterialX-basiert) ist der Weg fuer Custom-Materialien.
- **Prozedurale/dynamische Inhalte:** `LowLevelMesh` und `LowLevelTexture` (eingefuehrt visionOS 2 / iOS 18 /
  **macOS 15 Sequoia**, weiter in macOS 26 verfuegbar) erlauben eigene Vertex-Buffer-Layouts und
  GPU-Updates per Metal-Compute-Shader — Bruecke zwischen RealityKit-Komfort und Metal-Kontrolle.
  `LowLevelTexture` unterstuetzt komprimierte Pixelformate und detaillierte Usage-Kontrolle.

> Quelle: createwithswift.com (RealityView auf iOS/iPadOS/macOS), Apple Forums 788284/788524, rhonabwy.com,
> Apple Doc "Generating interactive geometry with RealityKit", github metal-by-example/metal-spatial-dynamic-mesh.
> Datum 2026-06-13. `[offiziell]` (Apple-Doku) + `[extern]` (Tutorials).

## §3 PBR & Materialien

- **RealityKit:** Materialien als `PhysicallyBasedMaterial` (Basecolor, Metallic, Roughness, Normal, AO,
  Emissive, Clearcoat etc.) oder als `ShaderGraphMaterial` (in Reality Composer Pro als MaterialX-Graph
  zusammengeklickt — der **einzige** Weg fuer komplexe Custom-Materialien). PBR-Materialien werden ueber
  Image-Based Lighting geschattet. Beim USDZ-Import legt RCP automatisch `PhysicallyBasedMaterial` an.
- **Custom Materials in RealityKit:** Ueber `CustomMaterial` + Metal-Surface/Geometry-Modifier laesst sich
  das Shading anpassen ("Modifying RealityKit rendering using custom materials").
- **SceneKit (Bestand):** `material.lightingModel = .physicallyBased`, Texturmaps fuer Diffuse/Roughness/
  Metalness zuweisen.
- **Metal (Custom-Renderer):** Eigene PBR-BRDF im Fragment-Shader (Cook-Torrance/GGX). Texturen ueber
  Model I/O / `MTKTextureLoader` laden, mipmappen, sRGB vs. linear korrekt unterscheiden (Albedo = sRGB,
  Normal/Roughness/Metallic = linear).

**Bildqualitaets-Tipp:** Fuer fotorealistische Produktvisualisierung sind hochaufgeloeste, korrekt
linearisierte Texturen + ein gutes HDR-Environment (IBL) entscheidender als die BRDF-Feinheiten.

> Quelle: Apple Doc "Applying realistic material and lighting effects to entities",
> "Modifying RealityKit rendering using custom materials", zoewave.medium.com (MaterialX in RCP),
> fabrizioduroni.it / medium @avihay (SceneKit PBR, extern). Datum 2026-06-13. `[offiziell]` + `[extern]`.

## §4 Beleuchtung, HDR & Image-Based Lighting (IBL)

- **IBL ist das Herzstueck schoener PBR-Beleuchtung.** Eine HDR-Umgebungs-Map (Lat/Long-Format) liefert
  realistische Reflexionen und diffuse Beleuchtung aus allen Richtungen.
- **RealityKit:** Vier Lichttypen — Point, Spot, Directional, Image-Based (IBL). Fuer IBL braucht man
  **sowohl** eine `ImageBasedLightComponent` (die HDR-Textur) **als auch** einen
  `ImageBasedLightReceiverComponent` an den beleuchteten Entities — **fehlt der Receiver, passiert nichts**
  (haeufige Falle, §9). Eine eigene IBL ersetzt die System-IBL. HDR-Textur in Lat/Long, eher niedrige
  Aufloesung reicht.
- **SceneKit (Bestand):** `scene.lightingEnvironment.contents = <HDR-Bild>` setzt das Environment fuer PBR;
  zusaetzlich HDR-Kamera fuer korrektes High-Dynamic-Range-Shading (statt 8-bit LDR).
- **Metal (Custom):** Environment-Map vorberechnen — Irradiance-Map (diffus) + prefiltered Specular-Map +
  BRDF-LUT. Diese Convolution offline/beim Laden berechnen und cachen, nicht pro Frame (teuer).

> Quelle: Apple Doc "Applying realistic material and lighting effects to entities", Apple Forums 819332,
> medium/macoclock "RealityKit 911 — Lighting and shadows", learnopengl.com PBR/IBL (extern, Konzept).
> Datum 2026-06-13. `[offiziell]` + `[extern]`.

## §5 Postprocessing & MetalFX

**MetalFX** (Stand WWDC25/WWDC26) — Apples Framework fuer Upscaling, Frame-Interpolation und Denoising:

- **Neuronales Temporal-Upscaling:** Auf der WWDC26 wurde ein **neu entworfener Temporal-Upscaler**
  vorgestellt, der Neural Engine + Neural Accelerators (M5 Pro/Max) nutzt und feine Details aus deutlich
  niedrigerer Renderaufloesung rekonstruiert — fluessige Frameraten bei hoher Qualitaet.
- **Frame-Interpolation:** MetalFX kann Zwischenbilder generieren (nach Exposure/Tonemapping) — deutlich
  guenstiger als jedes Frame voll zu rendern.
- **Denoised Upscaler** (`MTLFXTemporalDenoisedScalerDescriptor`): Denoising + Upscaling in einem Schritt,
  ideal fuer Ray-Tracing-Szenen mit wenigen Strahlen → vollwertiges Ergebnis.
- **Porting-Komfort (WWDC26-Framework):** Subrektangle fuer Dynamic Resolution, Motion-Vectors uebergeben
  (spart Preprocessing), Postprocess-Effekte mit Distortion-Fields. Bewegungsvektoren sind fuer gutes
  Temporal-Upscaling Pflicht.

**Postprocessing-Pipeline:**
- **RealityKit:** `renderCallbacks` mit `prepareWithDevice`- und `postProcess`-Closure (seit WWDC21/iOS15).
  Eigene Effekte per Metal-Compute oder Metal Performance Shaders (MPS). Beispiel Bloom: helle Bereiche per
  `MPSImageThresholdToZero` extrahieren → `MPSImageGaussianBlur` → mit `MPSImageAdd` zur Szene addieren.
- **Metal (Custom):** Eigener Compute- oder Fullscreen-Pass nach dem Geometrie-Render fuer Tonemapping,
  Bloom, DOF, Color-Grading. Reihenfolge beachten: Tonemapping/Exposure VOR Frame-Interpolation.

> Quelle: Apple "What's new in Metal", WWDC25 Sessions 205/211/254/262, Apple Doc MetalFX /
> MTLFXTemporalDenoisedScalerDescriptor / "Applying temporal antialiasing and upscaling using MetalFX",
> RealityKit-Postprocessing-Docs, github artyommihailovich/RealityKitPostProcessMetal. Datum 2026-06-13.
> `[offiziell]` + `[extern]`.

## §6 Performance (Metal & RealityKit)

- **Heaps statt Einzelallokationen:** Statische Texturen/Meshes beim Start/Laden in einem `MTLHeap` ablegen,
  Gesamtgroesse vorab bestimmen, mit einem einzigen `useHeap`-Call resident machen. `useResource` im
  Uebermass ist teuer — `useResources` (Plural) oder `useHeap` nutzen.
- **Argument Buffers / Bindless:** Ressourcen aggregieren, ein einzelner Buffer mit Navigation zu allen
  referenzierten Ressourcen → bindless Binding-Modell. Basis fuer GPU-driven Pipelines.
- **GPU-driven Rendering / Indirect:** Draw-Argumente in Buffer, ein `ExecuteIndirect`; ein Compute-Shader
  befuellt den Indirect-Buffer vorab → die GPU bereitet sich selbst die Arbeit vor (GPU-driven Loop).
- **Function Constants:** Shader-Varianten ueber Function Constants statt Branches — Metal faltet sie als
  Konstanten und eliminiert toten Code.
- **MetalFX zur Skalierung:** Niedriger rendern + neuronal hochskalieren ist oft der groesste FPS-Hebel.
- **RealityKit:** Asset-Komplexitaet (Dreieckszahlen, Texturgroessen) in RCP pruefen; IBL-Map klein halten
  (Lat/Long, niedrige Aufloesung reicht); `LowLevelMesh`/`LowLevelTexture` fuer GPU-seitige Updates.
- **Profiling:** Metal System Trace / GPU Frame Capture in Xcode 26 / Instruments.

> Quelle: Apple WWDC23 Sessions 10124/10125/10127, WWDC21 10286, WWDC22 10101,
> Apple Doc "Encoding argument buffers on the GPU", Tech Talk 111373 (Shader best practices). Datum 2026-06-13. `[offiziell]`.

## §7 Color Management — Display P3, EDR/HDR auf dem Mac

Moderne Macs (XDR-Displays, Pro Display XDR) koennen weit ueber SDR hinaus. EDR (Extended Dynamic Range)
ist Apples Mechanismus, HDR auf dem Desktop darzustellen.

**EDR auf einem Metal-View aktivieren (Pflicht-Setup):**
1. `metalLayer.wantsExtendedDynamicRangeContent = true`
2. Pixelformat auf Floating-Point: `MTLPixelFormatRGBA16Float` (`.rgba16Float`)
3. Colorspace auf extended-linear: `kCGColorSpaceExtendedLinearDisplayP3` (oder ExtendedLinearSRGB)

**Headroom abfragen:** Auf macOS liefert `NSScreen.maximumExtendedDynamicRangeColorComponentValue` (bzw.
das Screen-Property) den maximalen EDR-Wert — Werte > 1.0 sind "ueber Weiss". Damit Highlights korrekt
skalieren, statt zu clippen. Headroom ist dynamisch (Helligkeit, Inhalt, andere Fenster) — regelmaessig neu lesen.

- **Display P3** ist der Standard-Wide-Gamut auf Macs; in extended-linear-Form fuer HDR-Rendering verwenden.
- **Pipeline:** Intern in linearem (Floating-Point) Raum rendern, erst am Ende tonemappen/encoden.
- **RealityKit** rendert intern HDR-faehig; fuer echte EDR-Ausgabe muss der zugrundeliegende Layer EDR
  aktiviert haben.

> Quelle: Apple Doc `CAMetalLayer.wantsExtendedDynamicRangeContent`, WWDC21 "Explore HDR rendering with EDR"
> (10161), WWDC22 "Display EDR content with Core Image, Metal, and SwiftUI" (10114) + "Explore EDR on iOS"
> (10113), Apple Forums 724223/118825. Datum 2026-06-13. `[offiziell]`.

## §8 Asset-Pipeline (USDZ/glTF/Model I/O)

- **Laufzeitformat = USD(Z).** RealityKit und Reality Composer Pro erwarten zur Laufzeit **USDZ/USDA/USDC** —
  **kein** OBJ oder glTF direkt. glTF/OBJ/FBX muessen vorher konvertiert werden.
- **Reality Converter (macOS-App):** Drag-and-drop fuer `.obj`, `.gltf`, `.usd` → USDZ; Vorschau, Material-
  Anpassung. Fuer Automatisierung: `usdzconvert` oder die USD Python API in Build-Skripten.
- **DCC-Export:** Blender 3.6+, Maya, 3ds Max, Cinema4D koennen USD/USDC/USDZ exportieren.
- **Model I/O** (`MDLAsset`, `MDLMesh`): Apples Framework zum Laden/Verarbeiten von 3D-Assets (inkl. OBJ,
  USD, Alembic) fuer **Metal-Pipelines** — generiert Vertex-Buffer, Normalen/Tangenten, laedt Texturen,
  Bruecke zu `MTKMesh`. Wahl, wenn man einen eigenen Metal-Renderer fuettert (nicht RealityKit).
- **RCP-Workflow:** USDZ importieren → `PhysicallyBasedMaterial` automatisch → Stats pruefen → `.reality`.

> Quelle: Apple Doc (Reality Converter, Model I/O), Apple Forums 745874/743463/651921, kodeco.com
> "Reality Converter & PBR Materials", gabrieluribe.me, github radcli14/blender-to-realitykit. Datum 2026-06-13.
> `[offiziell]` + `[extern]`.

## §9 Haeufige Fallen & Bugs

1. **USD Material-Linkage bricht beim Import.** Symptom: Texturen/Materialien fehlen nach USDZ-Import in RCP,
   "multiple root level objects"-Warnung. Ursache: DCC legt Materialien ausserhalb des Default-Prim ab; USD
   referenziert nur den Default-Prim. Fix: Beim Export sicherstellen, dass alles unter EINEM Default-Prim
   haengt (single root). Betroffen: Reality Composer Pro (macOS 14+/26), DCC-Exporte (Blender/Maya u.a.).
2. **IBL ohne Wirkung.** Symptom: Image-Based-Light aenderт nichts. Ursache: nur `ImageBasedLightComponent`
   gesetzt, aber `ImageBasedLightReceiverComponent` an den Entities vergessen. Fix: BEIDE setzen (Component
   an die Light-Entity, Receiver an die beleuchteten Entities). Betroffen: RealityKit (alle aktuellen).
3. **ARView verschluckt Maus-Events auf macOS.** Symptom: SwiftUI-Gesten/Maus-Interaktion am 3D-View
   funktionieren nicht. Ursache: `ARView` faengt Maus-Events ab. Fix: Auf `RealityView` migrieren
   (empfohlener Weg ab WWDC24). Betroffen: RealityKit `ARView` auf macOS.
4. **Tahoe EDR / Auto-Brightness-Bug.** Symptom: Helligkeit/Kontrast springt unerwartet, SDR-Bilder falsch
   dargestellt, wenn Display im EDR-Modus + Auto-Brightness. Ursache: macOS 26 filtert die gesamte
   EDR-Range+Gamma durch die Color-Table (Color-Table-Werte als Multiplikatoren). Fix (User-seitig):
   Auto-Brightness im EDR-Modus deaktivieren bzw. Display-Preset wechseln; entwicklerseitig EDR-Headroom
   dynamisch neu lesen. Betroffen: macOS 26.0–26.1+ (Tahoe), v.a. XDR/P3-1600-nits-Presets. `[extern]`
   (Gadget Hacks, DPReview, BetterDisplay-Discussions) — kein offizieller Apple-Fix bekannt.
5. **glTF/OBJ direkt in RealityKit laden.** Symptom: Laedt nicht. Ursache: Laufzeit will USD(Z). Fix: vorher
   konvertieren (§8).
6. **SceneKit fuer Neuprojekt waehlen.** "Falle" im Sinne von technischer Schuld: SceneKit ist
   soft-deprecated, keine neuen Features. Fix: RealityKit/Metal 4 fuer Neues.
7. **EDR vergessen → flaues HDR.** Symptom: HDR-Inhalt sieht auf XDR-Display nicht "leuchtend" aus. Ursache:
   `wantsExtendedDynamicRangeContent`/`rgba16Float`/extended-linear-Colorspace nicht gesetzt. Fix: §7.
8. **IBL-Convolution pro Frame berechnen.** Symptom: niedrige FPS. Ursache: Irradiance/Prefilter-Maps werden
   nicht gecacht. Fix: vorberechnen und cachen (§4).

> Quelle: Apple Forums (745874, 819332, 788284, 724223), apple.gadgethacks.com / dpreview.com /
> github waydabber/BetterDisplay (Tahoe-EDR-Bug, extern), Apple WWDC-Sessions. Datum 2026-06-13.
> Mix `[offiziell]` + `[extern]` (Tahoe-Bug nur extern belegt).

---

### Quellenuebersicht
- Apple "What's new in Metal" — developer.apple.com/metal/whats-new `[offiziell]`
- WWDC25: 205 (Discover Metal 4), 211, 254, 262, 288 (SceneKit→RealityKit) `[offiziell]`
- WWDC24: 10104, 10186 (3D-Assets), RealityView-Erweiterung auf macOS `[offiziell]`
- WWDC23: 10095, 10124/10125/10127 (Metal-Performance), WWDC22: 10101/10113/10114, WWDC21: 10075/10161/10286 `[offiziell]`
- Apple Developer Docs: MetalFX, CAMetalLayer/EDR, RealityKit Material/Lighting/Postprocessing, Model I/O, Reality Converter `[offiziell]`
- Extern: createwithswift.com, rhonabwy.com, kodeco.com, medium (Andy Jazz, avihay, Dennis Ippel),
  zoewave.medium.com, dev.to/arshtechpro, learnopengl.com, github (BetterDisplay, RealityKitPostProcessMetal,
  blender-to-realitykit), gadgethacks/dpreview (Tahoe-EDR-Bug) `[extern]`

---

## Bezug ↔ Bug-Almanach

> Gegenseite (bekannte Fehler & Fallen): `bugs/desktop/3d-metal-scenekit-macos.md`.

| Best-Practice-Abschnitt | Bug-Abschnitt (bugs/desktop/3d-metal-scenekit-macos.md §N) |
|-------------------------|-----------------------------------------------------------|
| §1 Framework-Wahl (SceneKit soft-deprecated) | §14 SceneKit fuer Neuprojekt waehlen |
| §2 RealityKit auf macOS (RealityView, Migration) | §5 Collision/InputTarget, §6 ARView→RealityView |
| §3 PBR & Materialien | §8b Normal-Map faelschlich als sRGB |
| §4 Beleuchtung & IBL | §4 IBL Receiver vergessen, §7 Directional-Light-Schatten |
| §5 Postprocessing & MetalFX | §10 MetalFX Ghosting/Flackern |
| §6 Performance (Metal & RealityKit) | §11 nextDrawable Hang/nil, §12 Multi-Display-Stutter, §15 Speicher/Large-Model |
| §7 Color Management — Display P3, EDR/HDR | §8 sRGB-Doppel-Gamma, §9 Tahoe-EDR/Auto-Brightness, §16 EDR vergessen |
| §8 Asset-Pipeline (USDZ/glTF/Model I/O) | §1 USD Material-Linkage, §2 USDZ schwarz (RCP-Bundle), §3 USDZ-Web-URL schwarz, §13 glTF/OBJ-Import |
| §9 Haeufige Fallen & Bugs | §1, §4, §5, §6, §9 (Sammelbezug) |
