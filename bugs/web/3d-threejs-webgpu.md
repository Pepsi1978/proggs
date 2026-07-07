# Bekannte Bugs: 3D im Web/TypeScript (Three.js/Babylon/WebGPU)

> **PFLICHT-LESEN VOR DER ARBEIT.** Kuratierter Bug-Almanach fuer optisch schoene 3D-Web-/TS-Apps
> (Three.js, Babylon.js, WebGPU/WebGL2, React Three Fiber) verpackt mit Tauri (Desktop) + Capacitor
> (Android). Stand recherchiert **2026-06-13** fuer **Three.js r842 / Babylon.js 9.2.1 / Tauri v2.10.1 /
> WebGPU (Firefox-Holdout)**. Jeder Eintrag mit Symptom, Ursache, Versionen, FIX und Quelle.
> Gegenseite (Best Practices, das WIE-richtig): `best-practices/web/3d-threejs-webgpu.md`.

---

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

---

## 1. macOS-App ruckelt, max 60fps trotz ProMotion-Display [⭐ HÄUFIG]

- **Symptom:** Three.js/Babylon-App laeuft in der Tauri-Desktop-App auf einem 120Hz-ProMotion-Mac
  sichtbar bei nur 60fps, waehrend dieselbe Web-Seite in Chrome/Safari oder die Windows-Build mit 120fps laeuft.
- **Ursache:** WKWebView (das System-WebView, das Tauri/WRY auf macOS nutzt) cappt `requestAnimationFrame`
  auf macOS 13-15 (Ventura bis Sequoia) hart auf 60fps — unabhaengig vom echten Refresh-Rate des Displays.
  Es gibt KEIN offizielles Apple-API, um die WKWebView-Framerate zu setzen.
- **Versionen:** Tauri v2.10.1, macOS 13-15. **WICHTIG:** Auf **macOS 26 (Tahoe)** hat Apple den Cap
  **komplett entfernt** — WKWebView rendert dort wieder mit nativer Refresh-Rate. Das Problem betrifft
  also primaer aeltere macOS-Versionen.
- **FIX:**
  1. **macOS 26+:** Nichts zu tun — laeuft nativ mit voller Rate.
  2. **macOS 13-15, Sideload/eigene Distribution:** `tauri-plugin-macos-fps` einsetzen (deaktiviert
     den WKWebView-Framerate-Cap via WebKit-Private-`_features`-API). Funktioniert mit 120Hz ProMotion
     und 144Hz+ externen Displays. **App-Store-Risiko:** Nutzt private API → Mac-App-Store-Review lehnt das ab.
  3. **App-Store-Pflicht / 120Hz unverzichtbar:** Reiner WebView-Ansatz ist die falsche Wahl → nativer
     Render-Pfad (wgpu) statt WebView. Sonst 60fps akzeptieren.
- **Quelle:** [tauri-apps/tauri Issue #13978](https://github.com/tauri-apps/tauri/issues/13978) `[offiziell]`,
  [tauri-plugin-macos-fps (GitHub)](https://github.com/userFRM/tauri-plugin-macos-fps) `[extern]`,
  [crates.io tauri-plugin-macos-fps](https://crates.io/crates/tauri-plugin-macos-fps) `[extern]`.

## 2. Aliasing / Treppchen trotz `antialias: true` [⭐ HÄUFIG]

- **Symptom:** Kanten flimmern/treppen, obwohl der WebGLRenderer mit `antialias: true` erstellt wurde —
  sichtbar erst sobald Postprocessing/EffectComposer aktiv ist.
- **Ursache:** Sobald gerendert wird in ein RenderTarget statt direkt in den Canvas (EffectComposer/
  Postprocessing), wird das eingebaute MSAA des WebGLRenderers **umgangen** — es greift nur fuer den
  Default-Framebuffer.
- **Versionen:** Three.js r842, pmndrs/postprocessing.
- **FIX:** Eigenen AA-Pass als letzten Schritt der Composer-Kette anhaengen — `SMAAPass`, `TAARenderPass`
  oder `FXAA`. Alternativ `pmndrs/postprocessing` nutzen (merged Effekte + integriertes AA, performanter).
  In R3F: `<EffectComposer>` mit `<SMAA />` aus `@react-three/postprocessing`. WebGPU-Pipeline macht das
  AA-Handling anders — Pass-Reihenfolge der Node-Pipeline pruefen.
- **Quelle:** [threejs.org EffectComposer](https://threejs.org/docs/#examples/en/postprocessing/EffectComposer) `[offiziell]`,
  [pmndrs/postprocessing](https://github.com/pmndrs/postprocessing) `[offiziell-Repo]`.

## 3. Farben ausgewaschen / Beleuchtung falsch [⭐ HÄUFIG]

- **Symptom:** Szene wirkt flau/grau/zu hell, Materialien sehen "billig" aus, Spitzlichter clippen oder
  fehlen, Albedo-Farben stimmen nicht.
- **Ursache:** Color-Space- und Tone-Mapping-Konfiguration fehlt oder ist falsch. Color-Texturen
  (Albedo, Environment) nicht als sRGB markiert, Daten-Texturen (Normal/Roughness/AO) faelschlich als sRGB,
  oder kein Output-Color-Space/ToneMapping gesetzt.
- **Versionen:** Three.js r842.
- **FIX:** `renderer.outputColorSpace = SRGBColorSpace`, `renderer.toneMapping = ACESFilmicToneMapping`,
  `toneMappingExposure` feinjustieren. Color-Maps (Albedo/Environment) → `SRGBColorSpace`; Daten-Maps
  (Normal/Roughness/Metalness/AO) → `LinearSRGBColorSpace`. Bei WebGL-EffectComposer `OutputPass` ans Ende.
  Bei WebGPU-Pipeline werden Tone Mapping + Color-Konvertierung automatisch erledigt — keinen manuellen
  OutputPass doppeln.
- **Quelle:** [threejs.org Color Management](https://threejs.org/docs/#manual/en/introduction/Color-management) `[offiziell]`,
  [threejsroadmap Postprocessing 2026](https://threejsroadmap.com/blog/the-complete-guide-to-threejs-post-processing-in-2026) `[extern]`.

## 4. GLB laedt nicht — Draco/KTX2-Loader fehlt [⭐ HÄUFIG]

- **Symptom:** Fehler beim Laden komprimierter GLBs, z.B. `setKTX2Loader must be called before loading
  KTX2 textures` oder Draco-Decode-Fehler; Modell bleibt unsichtbar.
- **Ursache:** `KHR_draco_mesh_compression` braucht einen registrierten `DRACOLoader`, KTX2-Texturen
  brauchen einen `KTX2Loader` — beide muessen VOR dem Laden am GLTFLoader haengen. Reihenfolge zaehlt:
  `setKTX2Loader()` muss vor dem ersten Ladevorgang aufgerufen sein.
- **Versionen:** Three.js r842.
- **FIX:** Beide Loader instanziieren und setzen: `dracoLoader.setDecoderPath(...)` →
  `gltfLoader.setDRACOLoader(dracoLoader)`; `ktx2Loader.setTranscoderPath(...).detectSupport(renderer)` →
  `gltfLoader.setKTX2Loader(ktx2Loader)`. Decoder-/Transcoder-Pfade muessen ausgeliefert sein. In R3F/drei:
  `useGLTF` kann via Konfiguration beide Loader bekommen.
- **Quelle:** [threejs.org GLTFLoader](https://threejs.org/docs/pages/GLTFLoader.html) `[offiziell]`,
  [three.js forum: KTX2 setKTX2Loader-Fehler](https://discourse.threejs.org/t/how-to-load-a-gltf-that-uses-ktx2-textures-getting-error-setktx2loader-must-be-called-before-loading-ktx2-textures/48792) `[extern]`,
  [three.js forum: beide Loader fuer komprimierte GLB](https://discourse.threejs.org/t/how-to-add-both-ktx2loader-and-dracoloader-for-compressed-glb/46726) `[extern]`.

## 5. Flache Reflexionen / PMREMGenerator-Crash

- **Symptom:** Reflexionen sehen flach/falsch aus, Roughness wirkt unnatuerlich; ODER PMREMGenerator
  wirft nach erstem Gebrauch Fehler.
- **Ursache:** (a) HDR-Map nicht vorgefiltert oder als `scene.background` statt `scene.environment`
  gesetzt → keine korrekte Mip-Chain fuer Roughness. (b) Mehrere PMREMGenerator-Instanzen: `dispose()`
  auf einer macht alle anderen unbrauchbar (das Objekt ist quasi statisch/geteilt).
- **Versionen:** Three.js r842.
- **FIX:** HDR per `RGBELoader`/`EXRLoader` laden → `pmremGenerator.fromEquirectangular(hdr).texture`
  → `scene.environment` setzen (beleuchtet alle PBR-Materialien). Input ideal 1k (1024x512). Genau EINE
  PMREMGenerator-Instanz halten und wiederverwenden, danach gezielt einmal `dispose()`.
- **Quelle:** [threejs.org PMREMGenerator](https://threejs.org/docs/pages/PMREMGenerator.html) `[offiziell]`.

## 6. Memory-Leak — Speicher waechst, FPS faellt [⭐ HÄUFIG]

- **Symptom:** Ueber die Zeit (Szenenwechsel, Hot-Reload, wiederholtes Laden) steigt der Speicher stetig,
  FPS sinkt, irgendwann Crash/Context-Loss.
- **Ursache:** Three.js gibt GPU-Ressourcen NICHT automatisch frei. Geometrien, Materialien, Texturen und
  RenderTargets bleiben im GPU-Speicher, bis man sie explizit freigibt.
- **Versionen:** Three.js r842.
- **FIX:** Beim Entladen explizit `.dispose()` auf Geometry, Material, Texture und RenderTarget aufrufen
  (Material kann mehrere Texturen halten → alle Maps mit-disposen). In R3F raeumt der Reconciler beim
  Unmount vieles auf, ABER manuell erzeugte Ressourcen (eigene RenderTargets, Loader-Ergebnisse,
  PMREM-Texturen) selbst freigeben. Helper wie `drei`-`dispose` oder eigene Cleanup-Funktion nutzen.
- **Quelle:** [threejs.org: How to dispose of objects](https://threejs.org/docs/#manual/en/introduction/How-to-dispose-of-objects) `[offiziell]`,
  [utsubo 100 Three.js Tips 2026](https://www.utsubo.com/blog/threejs-best-practices-100-tips) `[extern]`.

## 7. WebGPU leer in Firefox / R3F + WebGPU bricht [⭐ HÄUFIG]

- **Symptom:** WebGPU-Szene bleibt in Firefox leer/schwarz; ODER in einer R3F-App bricht der
  WebGPU-Renderer (besonders mit Postprocessing) oder verhaelt sich inkonsistent.
- **Ursache:** (a) Firefox liefert WebGPU mid-2026 **noch NICHT default aktiv** (Holdout — Win 141 und
  macOS-Apple-Silicon 145 vorhanden, aber nicht standardmaessig an). (b) R3F unterstuetzt den
  `WebGPURenderer` Stand H1 2026 **noch nicht vollstaendig** — Poimandres arbeitet aktiv daran; die
  `gl`-Prop kann inzwischen ein Promise (async-Konstruktor) zurueckgeben, aber Postprocessing-Kompatibilitaet
  ist noch luckenhaft.
- **Versionen:** Firefox 141/145 (2026), R3F fiber v9.x, Three.js r842.
- **FIX:** Auf den automatischen WebGPU→WebGL2-Fallback des `WebGPURenderer` setzen UND den WebGL2-Pfad
  real testen (nicht nur annehmen). Fuer WebGPU-Pflicht mit React: vanilla Three.js oder R3F mit manuellem
  Renderer-Setup + Vorsicht bei Postprocessing (z.B. `ektogamat/r3f-webgpu-starter` als Referenz). Im
  Zweifel WebGL2 als Default belassen.
- **Quelle:** [gpuweb Implementation Status](https://github.com/gpuweb/gpuweb/wiki/Implementation-Status) `[offiziell]`,
  [r3f.docs.pmnd.rs](https://r3f.docs.pmnd.rs/) `[offiziell]`,
  [ektogamat/r3f-webgpu-starter](https://github.com/ektogamat/r3f-webgpu-starter) `[extern]`.

## 8. WebGPU-Canvas bleibt schwarz — kein Fehler in der Konsole

- **Symptom:** Mit `WebGPURenderer` bleibt der Canvas leer/schwarz, KEINE Fehlermeldung. Im Init-Code
  wird gerendert, aber nichts erscheint.
- **Ursache:** WebGPU initialisiert **asynchron** (Adapter/Device anfordern). Wenn man `render()` aus
  `requestAnimationFrame` oder im Init-Code aufruft, BEVOR der Renderer initialisiert ist, schlaegt das
  Rendern **still** fehl — kein Fehler, nur kein Bild.
- **Versionen:** Three.js r842 (Verhalten seit r171).
- **FIX:** Entweder `renderer.setAnimationLoop(fn)` nutzen — das stellt sicher, dass der Renderer beim
  ersten Frame initialisiert ist (empfohlen). ODER bei manuellem `requestAnimationFrame`/Init-Routine
  explizit `await renderer.init()` aufrufen, BEVOR gerendert wird.
- **Quelle:** [threejs.org WebGPURenderer](https://threejs.org/docs/pages/WebGPURenderer.html) `[offiziell]`,
  [utsubo WebGPU Migration Guide 2026](https://www.utsubo.com/blog/webgpu-threejs-migration-guide) `[extern]`.

## 9. Eigener Shader / `onBeforeCompile` bricht unter WebGPU

- **Symptom:** Custom-Materialien mit GLSL-Snippets ueber `onBeforeCompile` rendern unter WebGPU falsch
  oder gar nicht; ODER Code, der `material.type` ueberschreibt, wirft Fehler.
- **Ursache:** Der `WebGPURenderer` nutzt WGSL, nicht GLSL. `onBeforeCompile`-GLSL-Hacks sind ein
  WebGL-Konstrukt und laufen nicht auf WebGPU. Zusaetzlich ist `Material.type` jetzt eine statische
  Property und darf vom App-Code nicht mehr ueberschrieben werden (frueher genutzt, um Uniform-Updates
  zu erzwingen).
- **Versionen:** Three.js r842 (TSL/Node-System als renderer-agnostischer Ersatz).
- **FIX:** Custom-Shader auf **TSL (Three.js Shading Language) / NodeMaterial** migrieren — als Graph aus
  Nodes definiert, den Three.js fuer den aktiven Renderer (WebGL2 oder WebGPU) in die passende
  Shader-Sprache kompiliert. `onBeforeCompile` vermeiden. Keine `material.type`-Zuweisung mehr; fuer
  erzwungene Updates die offiziellen Dirty-Flags/Node-Updates nutzen.
- **Quelle:** [threejs.org TSL Specification](https://threejs.org/docs/TSL.html) `[offiziell]`,
  [three.js Migration Guide (Wiki)](https://github.com/mrdoob/three.js/wiki/Migration-Guide) `[offiziell]`,
  [three.js Issue #26719 Custom shader support for WebGPURenderer](https://github.com/mrdoob/three.js/issues/26719) `[offiziell]`.

## 10. `detectSupportAsync` deprecated / KTX2 transcode-Fehler unter WebGPU

- **Symptom:** Deprecation-Warnung zu `KTX2Loader.detectSupportAsync()`; ODER KTX2-Texturen
  transkodieren nicht / `.transcodeImage failed` unter dem WebGPURenderer.
- **Ursache:** `detectSupportAsync()` ist deprecated. Beim WebGPURenderer muss die Support-Erkennung
  NACH der asynchronen Renderer-Initialisierung erfolgen — sonst kennt der KTX2Loader die unterstuetzten
  GPU-Texturformate nicht.
- **Versionen:** Three.js r842.
- **FIX:** `await renderer.init()` zuerst, dann `ktx2Loader.detectSupport(renderer)` (synchrone Variante)
  aufrufen, danach Texturen laden. Reihenfolge: Renderer init → detectSupport → setKTX2Loader → laden.
- **Quelle:** [three.js Migration Guide (Wiki)](https://github.com/mrdoob/three.js/wiki/Migration-Guide) `[offiziell]`,
  [three.js forum: KTX2Loader transcodeImage failed](https://discourse.threejs.org/t/error-ktx2loader-transcodeimage-failed/32452) `[extern]`.

## 11. Mobile/iPad: WebGL context lost — Canvas wird schwarz nach App-Wechsel

- **Symptom:** In der Capacitor-Android- oder iOS/iPad-App wird der 3D-Canvas schwarz, nachdem die App in
  den Hintergrund ging und wieder nach vorn kommt; teils auch auf M-Series-iPads/Macs beim Laden grosser Modelle.
- **Ursache:** Der WebView verliert den WebGL-Kontext (Power-Save, Backgrounding, Speicherdruck,
  Treiber). Alle GPU-Ressourcen sind danach weg — wer nicht reagiert, bleibt schwarz. Bekannt auf
  iPadOS-Backgrounding und Android WebView seit Chromium 106.
- **Versionen:** Capacitor 2026, Android WebView (Chromium 106+), iOS/iPadOS 17+, Three.js r842.
- **FIX:** `canvas.addEventListener('webglcontextlost', e => e.preventDefault())` und
  `'webglcontextrestored'` behandeln → Szene/Texturen/Geometrien neu aufbauen. Speicherbudget niedrig
  halten (KTX2, dispose), Pixel-Ratio deckeln. Auf realen Zielgeraeten testen, nicht nur im Emulator.
- **Quelle:** [three.js forum: How to fix context lost (Android, iPhone, iOS)](https://discourse.threejs.org/t/how-to-fix-context-lost-android-iphone-ios/56829) `[extern]`,
  [Chromium Issue: Android WebView WebGL lost context seit 106](https://issues.chromium.org/issues/40249037) `[extern]`,
  [MDN WEBGL_lose_context](https://developer.mozilla.org/docs/Web/API/WEBGL_lose_context/loseContext) `[offiziell]`.

## 12. Tauri-Desktop: GLB/Draco-WASM 404 oder CORS-Block

- **Symptom:** In der Tauri-Build laden lokale GLBs/HDRs nicht, oder der Draco/KTX2-WASM-Decoder wirft
  404/CORS-Fehler — obwohl es im Dev-Server (Browser) funktioniert.
- **Ursache:** Tauri liefert Dateien ueber ein plattformspezifisches `asset:`-Protokoll aus, nicht ueber
  normale relative HTTP-Pfade. Drei-Loader/Decoder, die auf nackte Dateipfade zeigen, finden die Datei in
  der gepackten App nicht. CORS auf dem asset-Protokoll war historisch ein Problem (inzwischen aktivierbar).
- **Versionen:** Tauri v2.10.1.
- **FIX:** Lokale Pfade mit `convertFileSrc(path)` (aus `@tauri-apps/api/core`) in eine asset-Protokoll-URL
  wandeln und diese an die Loader geben. Draco/KTX2-Decoder-/Transcoder-Pfade ebenfalls als asset-URL
  setzen. asset-Scope in `tauri.conf.json` fuer die Asset-Verzeichnisse freigeben. CORS auf dem
  asset-Protokoll ist seit laengerem aktivierbar.
- **Quelle:** [tauri-apps Discussion #5045 Three JS integration](https://github.com/tauri-apps/tauri/discussions/5045) `[offiziell]`,
  [tauri commit: enable CORS on asset protocol](https://github.com/tauri-apps/tauri/commit/d28ac8aac0d19a70bb658f12e56330ec8ac4dda5) `[offiziell]`,
  [threejs.org DRACOLoader](https://threejs.org/docs/pages/DRACOLoader.html) `[offiziell]`.

## 13. Capacitor Android ruckelt, Desktop/iOS fluessig

- **Symptom:** Dieselbe 3D-App laeuft auf Desktop und iOS fluessig, auf Android (Capacitor WebView)
  deutliche Frame-Drops, besonders bei Animationen/vielen Objekten.
- **Ursache:** Der Android System WebView ist fuer grafisch intensive Inhalte langsamer; Geraetestreuung
  (Low-End-GPUs) gross; ungebremstes High-DPI-Rendering frisst Fillrate. Capacitor ist fuer "graphically
  intensive" Apps nicht erste Wahl.
- **Versionen:** Capacitor 2026, Android WebView.
- **FIX:** `renderer.setPixelRatio(Math.min(devicePixelRatio, 2))` deckeln, On-Demand-Rendering bei
  statischen Viewern (`frameloop="demand"` in R3F), Draw Calls < 100 (Instancing/Batching), KTX2 statt
  PNG/JPG, WebGL2-Fallback auf Low-End erzwingen (WebGPU erst ab Chrome 121+/Android 12+). Auf realen
  Low-End-Geraeten messen. Fuer schwere AAA-Echtzeit eher native Engine.
- **Quelle:** [capacitorjs.com Games Guide](https://capacitorjs.com/docs/guides/games) `[offiziell]`,
  [ionic-team/capacitor Discussion #3899 Android performance](https://github.com/ionic-team/capacitor/discussions/3899) `[extern]`,
  [ionic-team/capacitor Discussion #5562 webgl-gltf-threejs](https://github.com/ionic-team/capacitor/discussions/5562) `[extern]`.

## 14. Babylon.js WebGPU: dunkle Artefakte / Snapshot-Rendering verschluckt dynamische Meshes

- **Symptom:** Unter Babylon.js WebGPU erscheinen bei bestimmten Modellen dunkle Render-Artefakte, die in
  WebGL2 nicht da sind; ODER bei aktiviertem Snapshot-Rendering werden dynamische Aenderungen (bewegte
  Meshes, neue Objekte) nicht angezeigt.
- **Ursache:** (a) WebGPU-spezifische Render-Artefakte fuer einzelne Materialien/Modelle (gemeldet im
  Babylon-Forum). (b) Snapshot-Rendering zeichnet GPU-Commands EINMAL auf und spielt sie ab (~10x
  schneller) — dynamische Szenen-Aenderungen werden dabei nicht erfasst, wenn der falsche Snapshot-Modus
  aktiv ist.
- **Versionen:** Babylon.js 9.2.1 (Snapshot-Rendering im WebGPU-Pfad in 9.0 gefixt + ins Frame Graph integriert).
- **FIX:** Auf Babylon 9.2.1 aktualisieren (Snapshot-Fixes). Fuer dynamische Szenen Snapshot-Rendering
  ausschalten ODER den `FAST`-Modus statt `STANDARD` verwenden (FAST erlaubt bestimmte Uniform-Updates).
  Bei Artefakten das betroffene Material isolieren und gegen WebGL2 gegenpruefen, Forum-Issue verfolgen.
- **Quelle:** [Babylon.js WebGPU Snapshot Rendering](https://doc.babylonjs.com/setup/support/webGPU/webGPUOptimization/webGPUSnapshotRendering) `[offiziell]`,
  [Babylon.js Forum: Rendering artifacts with WebGPU](https://forum.babylonjs.com/t/rendering-artifacts-with-webgpu/62502) `[extern]`,
  [Babylon.js CHANGELOG](https://github.com/BabylonJS/Babylon.js/blob/master/CHANGELOG.md) `[offiziell]`.

---

## Fix-Status

| # | Bug | Status | Beleg |
|---|-----|--------|-------|
| 1 | macOS 60fps-Cap (WKWebView) | **Teilweise gefixt** — auf macOS 26 Tahoe entfernt; auf 13-15 nur via Plugin/native | macOS-26-Release; tauri-plugin-macos-fps |
| 2 | Aliasing trotz antialias | **Per Design / Workaround** — AA-Pass noetig, kein Bug der gefixt wird | EffectComposer-Doku |
| 3 | Farben ausgewaschen | **Konfigurationsfehler** — durch korrektes Color-Management vermeidbar | Color-Management-Doku |
| 4 | GLB Draco/KTX2 laedt nicht | **Konfigurationsfehler** — Loader korrekt registrieren | GLTFLoader-Doku |
| 5 | Flache Reflexion / PMREM-Crash | **Per Design** — PMREMGenerator quasi statisch, EINE Instanz | PMREMGenerator-Doku |
| 6 | Memory-Leak / dispose | **Per Design** — manuelles dispose noetig (kein Auto-GC fuer GPU) | dispose-Doku |
| 7 | Firefox WebGPU / R3F-WebGPU | **OFFEN** — Firefox-Holdout mid-2026; R3F-WebGPU in Arbeit (Poimandres) | gpuweb Status; R3F-Docs |
| 8 | WebGPU schwarz (async init) | **Per Design** — setAnimationLoop oder await init() | WebGPURenderer-Doku |
| 9 | onBeforeCompile/GLSL unter WebGPU | **Migrationspflicht** — TSL/NodeMaterial ersetzt es; material.type read-only | Migration Guide; TSL-Spec |
| 10 | detectSupportAsync deprecated | **Gefixt durch API-Aenderung** — detectSupport nach init nutzen | Migration Guide |
| 11 | WebGL context lost (mobile) | **OFFEN (Plattform)** — Handler ist Pflicht-Workaround, kein Engine-Fix | three.js forum; Chromium-Issue |
| 12 | Tauri asset/CORS/WASM-Pfad | **Konfigurationsfehler** — convertFileSrc + asset-Scope; CORS aktivierbar | tauri Discussion/Commit |
| 13 | Capacitor Android Performance | **OFFEN (Plattform-Limit)** — Workarounds, kein vollstaendiger Fix | capacitor Guide/Discussion |
| 14 | Babylon WebGPU Artefakte/Snapshot | **Teilweise gefixt** — Snapshot in 9.0 gefixt; Material-Artefakte fallweise offen | Babylon CHANGELOG/Forum |

**Noch offen (aktiv beobachten):**
- **#7 Firefox-WebGPU** — mid-2026 noch nicht default; R3F-WebGPU-Vollsupport noch nicht da.
- **#11 WebGL context lost auf Mobile** — Plattform-/Treiberverhalten, nur per Handler abfangbar.
- **#13 Android-Performance unter Capacitor** — strukturelles Limit des WebView, nur mildernde Workarounds.

**Ehrlichkeits-Hinweis:** `gh`-CLI war nicht verfuegbar — GitHub-Issue-Stati (z.B. genauer Merge-Stand
von R3F-WebGPU-PRs, Babylon-Material-Artefakt-Tickets) konnten NICHT 1:1 am Issue-Tracker verifiziert
werden. Diese Eintraege beruhen auf Release-Notes, Doku und Forum-Stand 2026-06-13 und sind als
"offen/teilweise" markiert, wo der genaue Fix-Stand unklar ist. Bei #1 ist der macOS-26-Fix ueber
mehrere Quellen (Issue + Plugin-Beschreibung) bestaetigt, aber nicht an einem offiziellen Apple-Changelog.

---

## Bezug ↔ Best-Practices

| Bug (hier) | Best-Practice-Abschnitt (Gegenseite) |
|------------|--------------------------------------|
| §1 macOS 60fps-Cap | §6 Verpackung Desktop: Tauri 2 (macOS-Falle) |
| §2 Aliasing | §4 Postprocessing (AA-Falle) |
| §3 Farben/Color-Space | §4 Postprocessing (Tone Mapping/Farbe) |
| §4 Draco/KTX2-Loader | §5 glTF/Draco/KTX2 (Loader-Setup-Falle) |
| §5 PMREM/Reflexionen | §3 PBR/HDR/IBL (PMREMGenerator-Falle) |
| §6 Memory-Leak | §9 Haeufige Fallen (dispose); §8 Performance |
| §7 Firefox/R3F-WebGPU | §1 Bibliotheks-Wahl (R3F-Einschraenkung); §2 WebGPU-Strategie |
| §8 WebGPU async init | §2 WebGPU-Strategie; §4 WebGPU-Pipeline |
| §9 TSL/onBeforeCompile | §1/§4 (Custom-Looks, WebGPU-Pipeline) |
| §10 detectSupport | §5 glTF/KTX2 (detectSupport(renderer)) |
| §11 context lost mobile | §7 Verpackung Android: Capacitor; §6 Desktop-WebView |
| §12 Tauri asset/CORS | §6 Verpackung Desktop: Tauri 2 |
| §13 Android-Performance | §7 Capacitor (Performance-Hinweis); §8 Performance |
| §14 Babylon WebGPU/Snapshot | §1 Babylon.js; §2 WebGPU-Strategie; §8 Render Bundles |

---

## Pflicht-Checkliste vor dem Start

- [ ] **Renderer-Init:** WebGPU? → `setAnimationLoop()` ODER `await renderer.init()` vor erstem Render (§8).
- [ ] **Fallback getestet:** WebGL2-Pfad real auf Firefox + Low-End-Android geprueft, nicht nur angenommen (§7, §13).
- [ ] **Color-Management:** `outputColorSpace=SRGBColorSpace` + `ACESFilmicToneMapping`; Color-Maps sRGB, Daten-Maps Linear (§3).
- [ ] **AA bei Postprocessing:** SMAA/TAA-Pass am Ende ODER pmndrs/postprocessing (§2).
- [ ] **Loader registriert:** DRACOLoader + KTX2Loader gesetzt; `detectSupport(renderer)` NACH init; `setKTX2Loader()` vor Laden (§4, §10).
- [ ] **HDR/IBL:** `fromEquirectangular` → `scene.environment`; genau EINE PMREM-Instanz (§5).
- [ ] **Dispose-Plan:** Cleanup fuer Geometry/Material/Texture/RenderTarget beim Entladen (§6).
- [ ] **Custom-Shader:** Auf TSL/NodeMaterial statt `onBeforeCompile`; kein `material.type`-Override (§9).
- [ ] **Context-Loss-Handler:** `webglcontextlost`/`restored` auf Mobile verdrahtet (§11).
- [ ] **Tauri-Assets:** Lokale Pfade per `convertFileSrc()`; asset-Scope + Decoder-Pfade gesetzt (§12).
- [ ] **macOS-fps:** 120Hz-Pflicht? → macOS 26 ODER Plugin (App-Store-Risiko bewusst) ODER 60fps akzeptieren (§1).
- [ ] **Pixel-Ratio gedeckelt:** `Math.min(devicePixelRatio, 2)` (§13).
- [ ] **Babylon:** Auf 9.2.1; Snapshot-Rendering nur fuer statische Szenen / FAST-Modus dynamisch (§14).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-dotnet-directx-windows](../desktop/3d-dotnet-directx-windows.md)
- [3d-godot](../desktop/3d-godot.md)
- [3d-metal-scenekit-macos](../desktop/3d-metal-scenekit-macos.md)
- [3d-rust-wgpu-bevy](../desktop/3d-rust-wgpu-bevy.md)
