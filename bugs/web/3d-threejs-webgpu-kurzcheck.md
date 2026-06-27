# 3D im Web/TypeScript (Three.js/Babylon/WebGPU) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | macOS-App ruckelt, max 60fps trotz ProMotion | macOS 26 Tahoe hebt den Cap auf; auf macOS 13-15 `tauri-plugin-macos-fps` ODER 60fps akzeptieren (kein public API, App-Store-Risiko) | §1 |
| 2 | Aliasing trotz `antialias:true` | EffectComposer umgeht eingebautes MSAA → SMAA/TAA-Pass ans Ende ODER pmndrs/postprocessing | §2 |
| 3 | Farben ausgewaschen / falsche Beleuchtung | `outputColorSpace=SRGBColorSpace` + `ACESFilmicToneMapping`; Color-Maps sRGB, Daten-Maps Linear | §3 |
| 4 | GLB laedt nicht (Draco/KTX2) | DRACOLoader UND KTX2Loader registrieren; `setKTX2Loader()` VOR dem Laden; `detectSupport(renderer)` | §4 |
| 5 | Flache Reflexionen / PMREMGenerator-Crash | HDR durch `fromEquirectangular` → `scene.environment`; genau EINE PMREM-Instanz | §5 |
| 6 | Speicher waechst, FPS faellt | `.dispose()` auf Geometry/Material/Texture/RenderTarget beim Entladen | §6 |
| 7 | WebGPU leer in Firefox / R3F+WebGPU bricht | Auto-Fallback auf WebGL2 testen; R3F-WebGPU unreif → WebGL2 oder vanilla Three.js | §7 |
| 8 | WebGPU-Canvas bleibt schwarz, kein Fehler | `await renderer.init()` fehlt ODER nicht `setAnimationLoop()` → still kein erster Frame | §8 |
| 9 | Eigener Shader/`onBeforeCompile` bricht unter WebGPU | GLSL/`onBeforeCompile` laeuft NICHT auf WebGPU → auf TSL/NodeMaterial migrieren; `material.type` ist jetzt read-only | §9 |
| 10 | `detectSupportAsync` deprecated-Warnung / KTX2 transcode-Fehler | Auf `detectSupport(renderer)` NACH `await renderer.init()` umstellen | §10 |
| 11 | Mobile/iPad: Canvas wird schwarz nach App-Wechsel | WebGL context lost beim Backgrounding → `webglcontextlost`/`restored`-Handler + Ressourcen neu aufbauen | §11 |
| 12 | Tauri-Desktop: GLB/Draco-WASM 404 / CORS | Lokale Pfade per `convertFileSrc()` in asset-Protokoll wandeln; Decoder-Pfade ueber asset-URL | §12 |
| 13 | Capacitor Android ruckelt, Desktop/iOS fluessig | Android WebView langsamer; Pixel-Ratio deckeln, On-Demand-Render, WebGL2-Fallback erzwingen | §13 |
| 14 | Babylon WebGPU: dunkle Artefakte / Snapshot zeigt nichts Dynamisches | Snapshot-Rendering friert Commands ein → fuer dynamische Meshes ausschalten/FAST-Modus; auf 9.2.1 aktualisieren | §14 |
