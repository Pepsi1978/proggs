# 3D auf macOS (Metal/SceneKit/RealityKit) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
