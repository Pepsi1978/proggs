# 3D auf Android (Filament/SceneView) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
