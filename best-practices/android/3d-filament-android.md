# 3D auf Android (Filament/SceneView) — Best Practices (Stand 2026-06-13, Version Filament 1.71.x)

> Fokus: Optisch sehr schoene 3D-Apps NATIV auf Android mit Kotlin — sowohl echtzeit/interaktiv
> als auch fotorealistische Produkt-/Szenenvisualisierung. Quellen Prioritaet 1 = offiziell
> (google/filament, sceneview/sceneview, Android Developers). Externe Quellen sind als `[extern]`
> gekennzeichnet. Stand der Versionen: **Filament 1.71.5/1.71.6** (Mai 2026, offiziell),
> **SceneView 4.18.0** (2026-06-06, offiziell; Repo umbenannt zu `sceneview/sceneview`).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Du willst schnell eine schoene 3D/AR-App in Kotlin + Compose | **SceneView 4.18.x** nehmen (`io.github.sceneview:sceneview` bzw. `arsceneview`). Kapselt Filament+ARCore, glTF/glb ohne Plugin, `Scene { }`-Composable. | §1, §6 |
| 2 | Du brauchst volle Kontrolle / Custom-Renderer / kein AR | **Filament direkt** (`com.google.android.filament:filament-android` + `gltfio-android` + `filament-utils-android`). | §1, §6 |
| 3 | Backend-Wahl OpenGL ES vs Vulkan | **Default = OpenGL ES** belassen (breiteste Kompatibilitaet, oft schneller auf Mobile). Vulkan nur gezielt + immer GLES-Fallback. | §1, §9 |
| 4 | Materialien definieren | `.mat`-Dateien mit **`matc`** offline kompilieren. matc-Version MUSS exakt zur Runtime-Lib passen. Modell `lit` (Standard) fuer PBR, `unlit` fuer Video/UI/Cubemap. | §2 |
| 5 | Realistische Beleuchtung / Spiegelungen | **IBL** aus HDR/EXR per **`cmgen`** vorbacken (`*_ibl.ktx` + `*_sh.txt` + Skybox). EXR statt PNG. Belichtung ueber Tonemapping, nicht ueber uebertriebene Env-Strength. | §3 |
| 6 | "Wie gekaufte Software" aussehen lassen | Postprocessing AN: **ACES-Tonemapping**, Bloom, SSAO, MSAA/FXAA, Color Grading. Fuer Produktvisualisierung lohnt sich der GPU-Aufwand. | §4 |
| 7 | glb/glTF laden, klein halten | **Draco** (Mesh) + **KTX2/Basis** (Texturen) verwenden. KTX2 bleibt im Speicher komprimiert → schneller Ladezeit + weniger RAM. | §5 |
| 8 | Compose-Einbindung bei rohem Filament | `AndroidView { SurfaceView }` + `UiHelper` + `Choreographer`-Frame-Loop; Lifecycle sauber via `DisposableEffect` aufraeumen. | §6 |
| 9 | Akku/Hitze/FPS auf Mittelklasse-Geraeten | Auf 30/60 FPS clampen, **dynamische Aufloesung**, MSAA reduzieren, ggf. Postprocessing droppen, **ADPF** (Thermal-API) nutzen. | §7 |
| 10 | Google-Play-Upload schlaegt fehl | **16 KB Page-Size-Alignment** fuer native `.so` ist seit Jan 2026 Pflicht (Android 15+). In app-`build.gradle` setzen. | §7, §8 |
| 11 | Schwarzer Screen / Crash beim Schliessen/Drehen | SwapChain/Engine-Ressourcen in der RICHTIGEN Reihenfolge zerstoeren; `setViewport` NICHT bei jedem Frame mit wechselnden Werten. | §8 |
| 12 | Mehrere 3D-Views gleichzeitig | Sehr fehleranfaellig (SIGSEGV). Wenn moeglich EINE Engine + mehrere `View`/`Scene` teilen statt mehrere Engines. | §8 |

---

## §1 Framework-Wahl: SceneView vs. Filament direkt vs. roh Vulkan/GLES

**Quellen:** [sceneview/sceneview README + Releases](https://github.com/sceneview/sceneview) (2026-06-06, offiziell); [google/filament](https://github.com/google/filament) (2026-05, offiziell)

### Die drei Ebenen

1. **SceneView (Kotlin, Compose-nativ) — Standardempfehlung fuer die meisten Apps.**
   - Der offizielle Nachfolger des archivierten Google **Sceneform 1.16.0**. Es gibt zwei Linien:
     **Sceneform (Java-Fortfuehrung)** unter `SceneView/sceneform-android` und **SceneView (Kotlin-Successor)**
     — letzteres ist die Zukunft.
   - **Repo 2026 umbenannt**: frueher `sceneview/sceneview-android`, jetzt `sceneview/sceneview` — es ist
     inzwischen ein Multiplattform-Projekt (Android Compose+Filament, iOS SwiftUI+RealityKit, Web).
   - **Aktuelle Version: 4.18.0** (2026-06-06). "The only Compose-native 3D library" (Eigenbeschreibung).
   - Kapselt Filament **und** ARCore, bringt glTF/glb direkt aus assets, `res/raw`, lokalen Dateien oder
     HTTP(S)-URLs — **ohne Plugin** (anders als altes Sceneform).
   - Bringt fertige Nodes: `ModelNode`, `LightNode`, `TextNode`, `ImageNode`, `VideoNode`, `BillboardNode`,
     `ReflectionProbeNode`, `DynamicSkyNode`, `PhysicsNode`, `HitResultNode` etc.
   - Compose: reines `ComponentActivity + setContent { Scene(...) }` — die alten Fragment-Layouts wurden
     entfernt, die Compose-Module in die Hauptmodule gemerged.
   - Gradle (Android): `io.github.sceneview:sceneview:<ver>` (3D-only) bzw. `io.github.sceneview:arsceneview:<ver>` (3D+AR).

2. **Filament direkt — wenn du volle Kontrolle willst, kein AR, oder einen eigenen Renderer/Loop brauchst.**
   - Maven: `com.google.android.filament:filament-android`, `:gltfio-android` (glTF-Loader),
     `:filament-utils-android` (`ModelViewer`, KTX-Loader, Mathe). Version **1.71.6** (Mai 2026).
   - Mehr Boilerplate (Engine, Renderer, View, Scene, Camera, SwapChain, UiHelper selbst verdrahten),
     aber maximale Freiheit fuer fotorealistische Produktszenen.

3. **Roh Vulkan / OpenGL ES (NDK) — nur in Ausnahmefaellen.**
   - Du baust den kompletten PBR-Stack selbst. Praktisch nie noetig, wenn Filament passt — Filament
     ist genau dafuer da und bereits hochoptimiert. Roh-Vulkan nur bei Spezial-Pipelines
     (eigene Compute, exotische Effekte) sinnvoll.

**Faustregel:** Produkt-/Szenenviewer, Konfiguratoren, AR-Platzierung, schnelle schoene Ergebnisse → **SceneView**.
Eigene Engine-Architektur, Spiel-aehnliche Render-Loop, kein AR, Custom-Postprocessing → **Filament direkt**.

---

## §2 PBR / Materialien (Filament Material System, matc)

**Quellen:** [Filament Materials Guide](https://google.github.io/filament/Materials.md.html) (offiziell);
[Materials System Overview (mintlify mirror)](https://www.mintlify.com/google/filament/concepts/materials-overview) (offiziell-Mirror); Discussion #3353 (offiziell)

- **Offline-Kompilierung:** Filament kompiliert Materialien NICHT zur Laufzeit. Du schreibst `.mat`-Dateien
  und uebersetzt sie mit dem CLI-Tool **`matc`** in `.filamat`-Pakete (enthalten Metadaten + vorgenerierte
  Shader fuer die Zielplattformen). Zur Laufzeit wird nur das Paket geladen.
- **KRITISCH:** `matc` MUSS aus exakt demselben Filament-Release stammen wie die Runtime-Lib. Versionsdrift
  zwischen Compiler und Runtime ist eine haeufige stille Fehlerquelle.
- **Material-Modelle:** `lit` (Standard, voll-PBR), `subsurface`, `cloth`, `unlit`, `specularGlossiness` (legacy).
  - **`lit`** fuer alles Physikalische (Metalle, Kunststoffe, Lacke) — der Default fuer schoene Produktvisualisierung.
  - **`unlit`** schaltet alle Lichtberechnung ab — fuer vorbeleuchtete Inhalte: Cubemaps, Video/Kamerastream,
    UI, Visualisierung/Debug. Beim Kompilieren werden alle Licht-Varianten herausgefiltert (kleiner/schneller).
- **Sampler-Budget:** `lit`-Materialien koennen standardmaessig bis zu **9 Sampler** nutzen; wird
  `refractionMode`/`reflectionMode` auf `screenspace` gesetzt, sinkt das auf 8. Sampler-Budget beim
  Texturreichtum im Auge behalten.
- **Material-Instances:** Ein kompiliertes Material kann viele `MaterialInstance` mit unterschiedlichen
  Parametern (baseColor, metallic, roughness, Texturen) speisen — so teilst du Shader und sparst Compile/Memory.

---

## §3 IBL / Beleuchtung (Image-Based Lighting)

**Quellen:** [Filament IBL (mintlify mirror)](https://www.mintlify.com/google/filament/rendering/ibl) (offiziell-Mirror);
[cmgen docs](https://google.github.io/filament/dup/cmgen.html) (offiziell); Issue #1387 (offiziell)

- **IBL ist der Schluessel zu Fotorealismus.** Es liefert sowohl diffuse Irradiance (weiches Umgebungslicht)
  als auch spekulare Reflexionen (Spiegelungen) aus einer aufgenommenen Umgebung — echte globale Beleuchtung.
- **Vorbacken mit `cmgen`** (CLI, Teil von Filament): Aus einer HDR/EXR-Lat-Long- oder Cubemap erzeugt cmgen
  - `environment_ibl.ktx` — gemippte Reflexions-Cubemap,
  - `environment_sh.txt` — Spherical-Harmonics-Koeffizienten (diffus),
  - `environment_skybox.ktx` — optionaler Skybox-Cubemap.
- **EXR/HDR statt PNG:** EXR ist empfohlen, weil es den hohen Dynamikumfang traegt, der fuer hochwertige
  IBL kritisch ist. PNG fuehrt zu ungenauer Beleuchtung.
- **Dynamikumfang:** Auf >= ~12 EV achten, mit echten, ungeclippten Lichtquellen. Die Szene ueber
  **Belichtung/Tonemapping** richtig hell machen — NICHT ueber kuenstlich hochgedrehte Environment-Strength
  (das frisst Energie/Realismus, siehe Issue #1387 "HDR IBL loses energy").
- **Mischung:** IBL fuer Umgebungslicht/Reflexionen + 1 gerichtete Sonne (`DirectionalLight`) mit Schatten
  ist das klassische, glaubwuerdige Setup. Punkt-/Spotlights nur gezielt ergaenzen.
- **SceneView:** bietet das ueber HDR-Environment-Switching + `ReflectionProbeNode` (lokale IBL-Zonen) +
  `DynamicSkyNode` (Tageszeit-Sonne) als fertige Bausteine an (Lighting-Lab-Demo, v4.17.0).

---

## §4 Postprocessing (Bloom / Tonemapping / Color Grading in Filament)

**Quellen:** [Filament IBL/Color mgmt (mintlify)](https://www.mintlify.com/google/filament/rendering/ibl) (offiziell-Mirror); SceneView v4.17.0 Lighting-Lab/Post-FX (offiziell)

- Damit eine App "wie gekaufte Software aus dem Laden" aussieht, ist die Postprocessing-Kette entscheidend:
  - **Tonemapping:** Filament unterstuetzt mehrere Tonemapper inkl. **ACES**. ACES (oder die Filament-eigenen
    Varianten) geben den filmischen, sauberen Look. Standardmaessig aktiv lassen.
  - **Color Grading:** Belichtung (`exposure`), Weissabgleich, Kontrast, Saettigung, Gamut-Mapping,
    Luminanz-Skalierung. Ueber das `ColorGrading`-Objekt am `View` setzen.
  - **Bloom:** weiches Ueberstrahlen heller Stellen — gibt Glanz/High-End-Anmutung.
  - **SSAO:** Ambient Occlusion fuer Kontaktschatten/Tiefe.
  - **Antialiasing:** **MSAA** (Hardware, teuer) und/oder **FXAA**/TAA (Postprocess, guenstiger).
- **Trade-off:** Postprocessing kostet GPU/Akku. Fuer **fotorealistische Standszenen / Produktviewer**
  lohnt sich der volle Stack. Fuer **echtzeit/interaktiv auf Mittelklasse** selektiv reduzieren (siehe §7).
- SceneView 4.17.0 liefert eine Post-FX-Demo (SSAO / MSAA / FXAA / Dithering) als Referenz.

---

## §5 glTF-Pipeline (Import, Draco, KTX2)

**Quellen:** [Khronos glTF / KTX2 + Basis (DeepWiki)](https://deepwiki.com/KhronosGroup/glTF-Sample-Models/5.1-texture-compression-with-ktx2-and-basis-universal) `[extern, Khronos]`;
[compress-glb.com](https://compress-glb.com/docs/support/advanced-texture-compression/) `[extern]`; SceneView README (offiziell)

- **Format:** glTF 2.0 / `.glb` ist der Standard ("JPEG fuer 3D", Khronos). Filament `gltfio` und SceneView
  laden beides direkt.
- **Mesh-Kompression: Draco** — der De-facto-Standard fuer glTF-Mesh-Kompression (`KHR_draco_mesh_compression`).
  Stark kleinere Dateien.
- **Textur-Kompression: KTX2 / Basis Universal** (`KHR_texture_basisu`):
  - KTX2 bleibt **auch im Speicher komprimiert** (einzigartig unter glTF-Texturformaten) und wird beim Laden
    in das fuer das jeweilige Geraet optimale GPU-Format transcodiert. → schnellere Ladezeit + weniger VRAM/RAM.
  - Besonders wertvoll bei Multi-Asset-Szenen: Raumplaner, Konfiguratoren, Galerien.
- **Kombinierbar:** Draco (Geometrie) + KTX2 (Texturen) zusammen → minimale Gesamtgroesse.
- **SceneView-Vorteil:** glTF/glb ohne Zusatz-Plugin direkt aus assets/raw/Datei/URL — inklusive aktueller
  Filament- und ARCore-Versionen out of the box.
- **Praxis:** Modelle vorab durch eine Optimierungspipeline schicken (z.B. `gltf-transform` / `gltfpack`)
  fuer Draco + KTX2 + Mesh-Aufraeumen, statt rohe Exporte auszuliefern. `[extern]`

---

## §6 Compose-Einbindung

**Quellen:** [romainguy/sample-wake-me-up](https://github.com/romainguy/sample-wake-me-up) (offiziell, Filament-Autor);
[A Guide to Filament for Android — Victor Brandalise](https://victorbrandalise.com/a-guide-to-filament-for-android/) `[extern]`; Discussion #7701 (offiziell)

### Variante A — SceneView (einfach, empfohlen)
- Direkt das `Scene(...)`-Composable (3D) bzw. `ARScene(...)` verwenden. Nodes als State halten,
  Modelle ueber `rememberNodes` / `ModelNode` laden. Kein manueller SurfaceView/Choreographer-Code noetig.
- Sample-Apps sind reine `ComponentActivity + setContent { }`.

### Variante B — Filament direkt in Compose
- **`AndroidView`** hostet einen klassischen **`SurfaceView`** (oder `TextureView`) in der Compose-Hierarchie.
- **`UiHelper`** verbindet die Surface mit Filaments SwapChain:
  `UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)`, dann `attachTo(surfaceView)`.
- Render-Loop ueber **`Choreographer`** (per-Frame-Callback) → `Renderer.render(view)`.
- `filament-utils`' **`ModelViewer(surfaceView, uiHelper)`** nimmt dir viel ab (Kamera-Manipulator,
  glTF laden, IBL setzen, Touch-Handling).
- **Lifecycle (KRITISCH):** Start/Stop des Frame-Loops via `LaunchedEffect`/`DisposableEffect` an den
  Compose-Lifecycle koppeln; in `onDispose` Choreographer-Callback entfernen und Engine-Ressourcen
  zerstoeren (siehe §8). Referenz-Sample fuer Compose<->SurfaceView-Interop: `romainguy/sample-wake-me-up`
  (vom Filament-Hauptautor, I/O-2021-Demo).
- **Transparenter Hintergrund:** `Renderer.ClearOptions` + `UiHelper.isOpaque = false` + Material-Blending;
  historisch Konflikt mit Postprocessing (Issue #1165, inzwischen gefixt) — Versionsstand pruefen.

---

## §7 Performance / Akku / Thermal

**Quellen:** [Android Performance Analyzer — Filament glTF Viewer Case Study](https://developer.android.com/android-performance-analyzer/case-study/filament) (offiziell);
[ADPF — Android Dynamic Performance Framework](https://developer.android.com/games/optimize/adpf) (offiziell);
[ARM — Save battery with ADPF](https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/save-battery-modern-graphics-mobile-adpf) `[extern, ARM]`; Discussion #7803, Issue #1898 (offiziell)

- **FPS clampen:** Nicht ungebremst rendern. Auf 30 oder 60 FPS begrenzen (Choreographer-Throttling),
  spart deutlich Akku/Hitze bei statischen/langsamen Szenen (Discussion #7803).
- **On-demand-Rendering:** Bei Standszenen nur rendern, wenn sich etwas aendert (Kamera bewegt, Animation
  laeuft, Laden im Gange). SceneView Web macht genau das per Dirty-Flag (`RenderGate`, v4.18.0) — dasselbe
  Prinzip auf Android anwenden: keine 60 Renders/s fuer ein unbewegtes Bild.
- **Dynamische Aufloesung:** Szene niedriger rendern und hochskalieren — optimiert die Fragment-Stage
  (Geometrie wird trotzdem voll verarbeitet). Auf Mobile lohnend (z.B. minScale 0.1, maxScale 0.8).
  **ACHTUNG:** Dynamic Resolution ist Teil des Postprocessings → funktioniert NICHT, wenn Postprocessing
  aus ist. Und: bekannter Bug mit Speicher-Spikes (Issue #1898) — Speicher monitoren.
- **Quality-Hebel fuer schwache GPUs:** Postprocessing aus, Render-Quality LOW, niedrigerer Color-Buffer,
  MSAA reduzieren/aus (FXAA statt MSAA). (Discussion #4965/#8026, offiziell.)
- **ADPF (Pflicht-Tool fuer ernsthafte Apps):** Das Android Dynamic Performance Framework liefert
  Echtzeit-Thermal-Status. Vor dem System-Throttling proaktiv Qualitaet senken (Aufloesung/Effekte),
  um Hitze-Akkumulation zu bremsen. Spart Akku und haelt FPS stabil.
- **16 KB Page-Size (Pflicht seit Jan 2026):** Google Play verlangt fuer Apps, die Android 15+ targeten,
  16-KB-aligned native Libraries. Im app-`build.gradle`:
  `packaging.jniLibs.pageAlignSharedLibraries = true` bzw. (Library-Module)
  `experimentalProperties["android.nativeLibraryAlignmentPageSize"] = "16k"`. Sonst Upload-Rejection.
  (SceneView v4.16.8, offiziell — gilt fuer alle Filament-Consumer, nicht nur SceneView.)

---

## §8 Haeufige Fallen (Bugs / Pitfalls)

**Quellen:** google/filament Issues #1344, #1165, #3381, #4692, #6936, Discussion #7394 (alle offiziell); SceneView Releases (offiziell)

| Symptom | Ursache | Fix | Versionen/Quelle |
|---------|---------|-----|------------------|
| Schwarzer Screen (nur App-Bar sichtbar) | Shader-Compile-Fehler / Emulator-GPU / Surface nicht korrekt attached | Auf echtem Geraet testen; UiHelper korrekt `attachTo`; Logcat auf Shader-Fehler pruefen | Issue #4692 |
| Crash/SIGSEGV bei mehreren 3D-Views gleichzeitig | Mehrere Engines/SwapChains kollidieren | Wenn moeglich EINE `Engine` + mehrere `View`/`Scene`/`Renderer` teilen statt mehrere Engines | Issue #1344 |
| Speicher waechst rapide pro Frame | `View.setViewport` jeden Frame mit wechselnden Werten aufgerufen | Viewport nur bei echter Aenderung setzen, nicht im Frame-Loop | Issue #3381 |
| Native Memory Leak | Filament-Ressourcen (Renderable, ByteBuffer, Texturen) nicht zerstoert; "global reference table overflow" | Saubere Teardown-Reihenfolge: alle Entities/Materials/Textures/SwapChain `destroy()`, dann `Engine.destroy()`. `TextureHelper.setBitmap`-Leak beachten | Discussion #7394, Issue #6936 |
| Transparente Views werden vom Postprocessing nicht geblendet | Postprocessing-Pass ignorierte Transparenz (alter Bug) | Auf aktuelle Filament-Version aktualisieren (gefixt); `UiHelper.isOpaque=false` + ClearOptions | Issue #1165 |
| Vulkan langsamer als OpenGL / Crash | Adreno/PowerVR-Treiberprobleme, unoptimierte Materialien | Bei OpenGL ES bleiben (Default); Materialien immer mit matc optimieren; Vulkan nur mit GLES-Fallback | Issue #5294, #5201, #4225 |
| matc-Material laedt nicht / Render kaputt | matc-Version != Runtime-Lib-Version | matc IMMER aus demselben Release wie die Runtime nutzen | Materials Guide |
| Play-Store-Upload abgelehnt | Native `.so` nicht 16-KB-aligned (Android 15+) | `pageAlignSharedLibraries=true` setzen (§7) | SceneView v4.16.8 |
| Dynamic-Resolution-Crash / Speicher-Spikes | Bekannter Bug, nur Android | Vorsichtig einsetzen, Speicher monitoren, Grenzen testen | Issue #1898 |
| Transform-Drift bei wiederholtem Setzen von rotation/scale | Per-Komponenten-Setter dekomponierte Matrix neu → Scale driftet | SceneView >= 4.17.0 (gefixt; gecachte TRS-Getter, keine Re-Dekomposition) | SceneView v4.17.0 |

---

## Quellenliste (kompakt)

**Offiziell:**
- google/filament — Repo + RELEASE_NOTES + Issues: https://github.com/google/filament
- Filament Materials Guide: https://google.github.io/filament/Materials.md.html
- Filament cmgen: https://google.github.io/filament/dup/cmgen.html
- sceneview/sceneview — Repo + Releases (4.18.0): https://github.com/sceneview/sceneview
- romainguy/sample-wake-me-up (Compose<->SurfaceView): https://github.com/romainguy/sample-wake-me-up
- Android Performance Analyzer — Filament Case Study: https://developer.android.com/android-performance-analyzer/case-study/filament
- ADPF: https://developer.android.com/games/optimize/adpf

**Extern:**
- Khronos KTX2/Basis (DeepWiki): https://deepwiki.com/KhronosGroup/glTF-Sample-Models/5.1-texture-compression-with-ktx2-and-basis-universal
- A Guide to Filament for Android (V. Brandalise): https://victorbrandalise.com/a-guide-to-filament-for-android/
- ARM — Save battery with ADPF: https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/save-battery-modern-graphics-mobile-adpf

---

## Bezug ↔ Bug-Almanach

(Gegenseite: `bugs/android/3d-filament-android.md` — was schiefgeht und wie man es fixt. Stand 2026-06-13.)

| Best-Practice hier | Verhindert Bug-Almanach-Eintrag |
|--------------------|----------------------------------|
| §7/§8 16-KB-Page-Size, AGP, jniLibs | → §1 (Play-Upload abgelehnt; `filamat-android-lite` meiden, #9460) |
| §8 Viewport nicht jeden Frame setzen | → §2 (setViewport Memory-Leak, #3381) |
| §8 Eine Engine, Views/Scenes teilen | → §3 (SIGSEGV mehrere Views/Engines, #1344/#2364/#7303) |
| §6/§8 Teardown-Reihenfolge, DisposableEffect | → §4 (GREF-overflow / Native Leak, #7394/#6936/#4881) |
| §2 matc-Version == Runtime | → §5 (Material version mismatch, #4685/#4399) |
| §8 Echtgeraet, UiHelper, Logcat | → §6 (Schwarzer Screen, #4692) |
| §1/§9 GLES default, Vulkan+Fallback, matc-Optimierung | → §7 (Vulkan langsamer/Adreno-Crash, #4225/#7091/#5294/#6444/#8774) |
| §5 glTF/KTX2-Pipeline, gltf-transform | → §8 (KTX2-Texturen schwarz, #4771 + Oekosystem) |
| §7 Dynamic Resolution vorsichtig, Speicher monitoren | → §9 (DR Memory-Spikes, #1898/#5885) |
| §8 SceneView >= 4.17.0 | → §10 (Transform-Drift rotation/scale) |
| §1/§6 SceneView aktuell, Lifecycle, onException | → §11 (AR Resume / CameraNotAvailableException, #1752) |
| §6/§8 Lifecycle-Guard, Teardown vor Recreate | → §12 (Quick create+detach Crash, #6933/#5543/#6604) |
| §6 isOpaque=false, ClearOptions, Version | → §13 (Transparenz + Postprocessing, #1165) |
| §6 Choreographer-Loop, konsistenter Thread | → §14 (Engine in HandlerThread SIGSEGV, #6534/#4168) |
