# 3D im Web/TypeScript (Three.js/Babylon/WebGPU) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Bibliothek waehlen | Three.js + React Three Fiber (R3F) fuer React-Apps/Custom Looks; Babylon.js wenn du Engine-Features (Inspector, Physik, Frame Graph, Editor) out-of-the-box willst | §1 |
| 2 | Renderer waehlen | `WebGPURenderer` als Default verwenden (seit r171 produktionsreif), faellt automatisch auf WebGL2 zurueck. Nicht zwei Codepfade pflegen | §2 |
| 3 | R3F + WebGPU heute | R3F unterstuetzt WebGPU noch NICHT vollstaendig (Stand H1 2026). Fuer WebGPU-Pflicht entweder vanilla Three.js oder R3F mit manuellem Renderer-Setup + Vorsicht bei postprocessing | §1, §4 |
| 4 | Realistische Optik | PBR-Materialien (`MeshPhysicalMaterial`) + HDR-Environment via IBL. `.hdr`/`.exr` durch `PMREMGenerator` jagen, Input ideal 1k (1024x512) | §3 |
| 5 | Tone Mapping / Farbe | `ACESFilmicToneMapping` + `outputColorSpace = SRGBColorSpace`. Bei EffectComposer (WebGL) `OutputPass` ans Ende; bei WebGPU-Pipeline wird Tone Mapping/Color automatisch gemacht | §4 |
| 6 | Assets ausliefern | glTF/GLB + Draco (Mesh) + KTX2/Basis (Texturen). Pipeline: `gltf-transform optimize in.glb out.glb --texture-compress ktx2 --compress draco` | §5 |
| 7 | Postprocessing | `pmndrs/postprocessing` (gemergete Effekte, performanter) statt einzelner EffectComposer-Passes. AA nicht vergessen (EffectComposer umgeht WebGL-MSAA) | §4 |
| 8 | Desktop-Verpackung | Tauri 2 (stable v2.10.1). WebGPU laeuft via System-WebView (WebView2/WKWebView/WebKitGTK). macOS WKWebView ist auf 60fps gedeckelt (kein ProMotion 120Hz) | §6 |
| 9 | Android-Verpackung | Capacitor: WebGL solide, WebGPU nur ab Android System WebView mit WebGPU-Support (Chrome 121+, Android 12+). Fuer Low-End-Geraete WebGL2-Fallback erzwingen | §7 |
| 10 | Performance-Budget | Draw Calls < 100/Frame anstreben; Instancing/Batching reduziert Draw Calls um 90%+. KTX2 spart ~10x GPU-Speicher. Draco-Decode im Web Worker | §8 |
| 11 | Haeufigste Falle | `dispose()` vergessen → Memory-Leak. Geometrie, Material, Textur, RenderTarget explizit freigeben beim Entladen | §9 |
| 12 | Firefox-Falle | Firefox liefert WebGPU mid-2026 noch NICHT default aktiv → Fallback auf WebGL2 muss IMMER getestet werden | §2 |
