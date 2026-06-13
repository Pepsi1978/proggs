# 3D im Web/TypeScript (Three.js/Babylon/WebGPU) — Best Practices (Stand 2026-06-13, Version Three.js r842 / Babylon.js 9.2.1)

> Zielbild: optisch sehr schoene 3D-Apps mit Web-/TypeScript-Stack, verpackt fuer Desktop (Tauri)
> und Android (Capacitor). Deckt BEIDE Inhaltstypen ab: echtzeit/interaktiv UND fotorealistische
> Produktvisualisierung. Quellen pro Abschnitt mit Datum und `[offiziell]`/`[extern]` markiert.

---

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

---

## §1 — Bibliotheks-Wahl: Three.js vs Babylon.js vs React Three Fiber

**Three.js (r842, Stand 2026-06-09)** — die schlanke, maximal flexible Low-Level-Lib. Groesstes
Oekosystem, volle Kontrolle ueber den Render-Loop, kleinste Bundles bei Tree-Shaking. Ideal fuer
Custom-Looks, kreative/interaktive Installationen und produktnahe Visualisierung, wenn man die
Render-Pipeline selbst steuern will. Quelle: [threejs.org/docs WebGPURenderer](https://threejs.org/docs/pages/WebGPURenderer.html), [GitHub Releases](https://github.com/mrdoob/three.js/releases) — `[offiziell]` 2026-06.

**Babylon.js (9.2.1, Release 2026-04-13)** — Full-Engine mit Batterien inklusive: integrierter
Inspector, Physik, Node Material Editor, Animation-Retargeting, Geospatial-Rendering und seit 9.0
das **Frame Graph**-System (v1) fuer feingranulare Pipeline-Kontrolle. Alle Core-Shader sind seit
2024 nativ in WGSL. Babylon meldet ~10x schnelleres Scene-Rendering ueber WebGPU **Render Bundles**
(vorrecorded Commands) und deutlich bessere Akkulaufzeit (3h statt 2h ggue. WebGL bei gleicher Last).
Waehlen, wenn man Editor/Tooling, Physik und ein konsistentes Engine-Erlebnis statt Bastelkasten will.
Quelle: [doc.babylonjs.com WebGPU](https://doc.babylonjs.com/setup/support/webGPU), [Windows Dev Blog: Babylon.js 9.0](https://blogs.windows.com/windowsdeveloper/2026/03/26/announcing-babylon-js-9-0/) — `[offiziell]` 2026-03/04.

**React Three Fiber (R3F v9.5+, Stand H1 2026)** — deklarativer React-Renderer fuer Three.js.
Empfohlene Versionsmatrix 2026: `@react-three/fiber@9` + **React 19+**, `@react-three/drei@9.116+`,
`@react-three/postprocessing@3+`, `@react-three/rapier@2+`. "Alles was in Three.js geht, geht in R3F."
**WICHTIGE EINSCHRAENKUNG:** R3F unterstuetzt den WebGPURenderer Stand Anfang 2026 noch NICHT
vollstaendig — das Poimandres-Team arbeitet aktiv daran. Wenn WebGPU zwingend ist und du React nutzt,
musst du den Renderer ggf. manuell setzen und postprocessing-Kompatibilitaet pruefen.
Quelle: [r3f.docs.pmnd.rs](https://r3f.docs.pmnd.rs/), [pmndrs/react-three-fiber](https://github.com/pmndrs/react-three-fiber) — `[offiziell]`; Versionsmatrix [creativedevjobs.com R3F vs Three.js 2026](https://www.creativedevjobs.com/blog/react-three-fiber-vs-threejs) — `[extern]` 2026.

**Entscheidungs-Faustregel:** React-Frontend + schneller Aufbau → R3F (WebGL2-Pfad sicher, WebGPU
abwarten). Maximale visuelle Kontrolle/Custom-Shader → vanilla Three.js. Editor/Physik/komplette
Engine → Babylon.js.

---

## §2 — WebGPU vs WebGL: aktueller Browser-Stand und Strategie

**Browser-Support (Stand 2026, ~82% global; teils mit "70%" zitiert):** Quelle: [web.dev WebGPU supported in major browsers](https://web.dev/blog/webgpu-supported-major-browsers) `[offiziell]`, [gpuweb Implementation Status](https://github.com/gpuweb/gpuweb/wiki/Implementation-Status) `[offiziell]`, [byteiota WebGPU 2026](https://byteiota.com/webgpu-2026-70-browser-support-15x-performance-gains/) `[extern]` 2026.

- **Chrome/Edge:** default an. Windows via Direct3D 12, macOS, ChromeOS. **Android: ab Chrome 121**
  (Android 12+, Qualcomm/ARM GPUs). Linux rollt noch aus (Chrome 144 Beta: Intel Gen12+).
- **Safari 26:** WebGPU default an auf macOS Tahoe 26, iOS 26, iPadOS 26, visionOS 26. Inkl. HDR-Bilder
  im WebGPU-Canvas; ab Safari 26.2 WebXR + WebGPU auf Vision Pro.
- **Firefox:** Win (141) und macOS Apple Silicon (145) vorhanden, **aber mid-2026 noch NICHT default
  aktiv** — der Holdout. Linux/Android noch in Arbeit. → **Fallback ist Pflicht.**

**Strategie:** Mit Three.js `WebGPURenderer` arbeiten — er nutzt WebGPU wenn verfuegbar und faellt
**automatisch auf WebGL2** zurueck. Damit ein Codepfad statt zwei. WebGL2 bleibt sinnvoll fuer
einfache Produkt-Viewer und Low-End-Mobile (breitere Alt-Geraete-Abdeckung). WebGPU lohnt fuer
schwere Echtzeit-Szenen, Compute, viele Draw Calls und Data-Viz (Quellen nennen bis 15x Speedup
in Spezialfaellen). Quelle: [threejs.org WebGPURenderer](https://threejs.org/docs/pages/WebGPURenderer.html) `[offiziell]`, [utsubo What's New in Three.js 2026](https://www.utsubo.com/blog/threejs-2026-what-changed) `[extern]`.

---

## §3 — PBR / HDR-Environment / IBL (fotorealistische Optik)

Quelle: [threejs.org PMREMGenerator](https://threejs.org/docs/pages/PMREMGenerator.html) `[offiziell]`, three.js forum HDR/IBL Threads `[extern]`, [Medium PBR+IBL](https://medium.com/@althafkhanbecse/elevating-realism-mastering-physically-based-rendering-pbr-and-image-based-lighting-ibl-in-e17c287aa9e1) `[extern]` 2026.

- **Material:** `MeshStandardMaterial` (Standard-PBR) oder `MeshPhysicalMaterial` (Clearcoat,
  Transmission/Glas, Sheen, Iridescence) fuer Produktvisualisierung. Albedo/Metalness/Roughness/Normal/AO-Maps.
- **IBL / Environment:** HDR-Map (`.hdr` via `RGBELoader` oder `.exr` via `EXRLoader`) durch den
  **PMREMGenerator** vorfiltern → liefert die Mip-Chain fuer korrekte Roughness-Reflexionen. Ergebnis
  als `scene.environment` setzen (beleuchtet alle PBR-Materialien), optional als `scene.background`.
- **Input-Groessen:** Equirect ideal **1k (1024x512)** — passt am besten zum 256x256-Cubemap-Output;
  Cubemap-Input ideal 256x256. Groesser bringt selten sichtbaren Gewinn, kostet aber Speicher/Zeit.
- **HDR statt LDR:** HDR-Environments machen PBR-Fehler sichtbar und liefern echte Spitzlichter/Reflexe
  — fuer Realismus klar HDR bevorzugen.
- **PMREMGenerator-Falle:** ist quasi statisch — nur EIN Objekt halten; `dispose()` auf einem macht
  alle anderen unbrauchbar. Quelle: [threejs.org PMREMGenerator](https://threejs.org/docs/pages/PMREMGenerator.html) `[offiziell]`.

---

## §4 — Postprocessing (Bloom, SSAO, TAA, Tone Mapping, Farbe)

Quelle: [pmndrs/postprocessing](https://github.com/pmndrs/postprocessing) `[offiziell-Repo]`, [threejs.org EffectComposer](https://threejs.org/docs/#examples/en/postprocessing/EffectComposer) `[offiziell]`, [threejsroadmap Postprocessing 2026](https://threejsroadmap.com/blog/the-complete-guide-to-threejs-post-processing-in-2026) `[extern]` 2026.

- **Bibliothek:** Fuer ernsthafte Looks `pmndrs/postprocessing` statt vieler einzelner Three.js-Passes.
  Es **merged** kompatible Effekte in moeglichst wenige Fullscreen-Passes → deutlich weniger
  Bandbreite/Overhead. In R3F via `@react-three/postprocessing@3`.
- **EffectComposer (WebGL):** RenderPass zuerst (clears + rendert Szene). Tone Mapping + sRGB-Konvertierung
  am Ende ueber `OutputPass`. **AA-Falle:** EffectComposer umgeht das eingebaute MSAA des WebGLRenderers
  → eigenen AA-Pass (z.B. SMAA, TAARenderPass, FXAA) ans Ende haengen, sonst sichtbares Aliasing.
- **WebGPU-Pipeline:** Tone Mapping, Color-Space-Konvertierung und Resize werden automatisch erledigt
  — KEIN manueller OutputPass/`setSize()` noetig (Architektur-Unterschied beachten).
- **Tone Mapping fuer Realismus:** `renderer.toneMapping = ACESFilmicToneMapping`,
  `renderer.toneMappingExposure` feinjustieren, `renderer.outputColorSpace = SRGBColorSpace`. Texturen,
  die Farbe sind (Albedo/Environment), als `SRGBColorSpace` markieren; Daten-Maps (Normal/Roughness/AO)
  als `LinearSRGBColorSpace` lassen — sonst falsche Beleuchtung.
- **Typische Effekte fuer "teuren" Look:** Bloom (HDR-Spitzlichter), SSAO (Kontakt-Schatten,
  nur sichtbare Pixel → guenstig), DoF, GTAO, Color-Grading/LUT, leichtes Film-Grain/Vignette.

---

## §5 — glTF / Draco / KTX2 / Basis — Asset-Pipeline

Quelle: [threejs.org GLTFLoader](https://threejs.org/docs/pages/GLTFLoader.html) `[offiziell]`, [gltf-transform.dev](https://gltf-transform.dev/) `[offiziell-Tool]`, [utsubo 100 Tips](https://www.utsubo.com/blog/threejs-best-practices-100-tips) `[extern]` 2026.

- **Format:** glTF 2.0 / **GLB** (binaer, ein File). Standard fuer Web-3D.
- **Mesh-Kompression — Draco:** Extension `KHR_draco_mesh_compression`. **Decode im Web Worker** →
  blockiert den Main-Thread nicht. `DRACOLoader` an `GLTFLoader` haengen, Decoder-Pfad setzen.
- **Textur-Kompression — KTX2 + Basis Universal:** bleibt **komprimiert auf der GPU** → ca. **10x**
  weniger GPU-Speicher als entpackte Texturen. `KTX2Loader` setzen (braucht Transcoder + Renderer-Support-Detect).
  - **UASTC:** hoehere Qualitaet, groessere Files → Normal-Maps und Hero-Texturen.
  - **ETC1S:** kleiner, ok-Qualitaet → Environment/Diffuse/Sekundaer-Texturen.
  - Startregel: UASTC fuer Normals, ETC1S fuer Diffuse.
- **One-Liner-Pipeline (gltf-transform):**
  `gltf-transform optimize model.glb output.glb --texture-compress ktx2 --compress draco`
  Fuer Feinkontrolle KTX2-Codecs einzeln (UASTC/ETC1S mit Quality/Compression-Flags). Optional
  `meshopt` als Alternative/Ergaenzung zu Draco.
- **Loader-Setup-Falle:** DRACOLoader UND KTX2Loader BEIDE am GLTFLoader registrieren, sonst Fehler
  beim Laden komprimierter Assets. KTX2Loader braucht `detectSupport(renderer)`.

---

## §6 — Verpackung Desktop: Tauri 2 (macOS / Windows)

Quelle: [v2.tauri.app Tauri 2.0 Stable](https://v2.tauri.app/blog/tauri-20/) `[offiziell]`, [tauri-apps/tauri Discussions/Issues](https://github.com/tauri-apps/tauri) `[offiziell]`, [dev.to System WebViews in Tauri](https://dev.to/shrsv/exploring-system-webviews-in-tauri-native-rendering-for-efficient-cross-platform-apps-9hl) `[extern]`.

- **Stand:** Tauri **v2.10.1** (2026-03-04). Stabile Desktop- (und Mobile-)Unterstuetzung. HMR auch
  auf Geraeten/Emulatoren.
- **Architektur:** Tauri nutzt **WRY** → System-WebView: **WebView2** (Windows), **WKWebView**
  (macOS/iOS), **WebKitGTK** (Linux), Android System WebView. Kleine Bundles, kein gebuendelter Chromium.
- **WebGPU im WebView:**
  - **Windows/WebView2:** WebGPU ist aktuell und unterstuetzt; ggf. via Experimental-Flags freischalten.
  - **macOS/WKWebView:** WebGPU vorhanden ab Safari/WebKit 26-Generation.
- **macOS-Falle (KRITISCH fuer fluessige 3D):** WKWebView ist auf **60fps gedeckelt** — **kein
  ProMotion 120Hz** ueber den WebView erreichbar. Nicht durch App-Settings loesbar (WKWebView-intern).
  Fuer 120Hz-Pflicht-Looks ist der reine WebView-Ansatz die falsche Wahl.
- **Native-GPU-Overlay (Fortgeschritten):** wgpu-Frames als WebView-Overlay zu rendern ist noch
  unausgereift (Issues #11944, #8246) — Performance unklar (Beispiel: 300ms auf Low-End). Fuer
  reine WebGPU/WebGL-3D im Canvas NICHT noetig; nur relevant wenn man natives Rendering mit WebView mischt.

---

## §7 — Verpackung Android: Capacitor

Quelle: [capacitorjs.com Games Guide](https://capacitorjs.com/docs/guides/games) `[offiziell]`, [developer.android.com WebGPU for Android](https://developer.android.com/develop/ui/views/graphics/webgpu) `[offiziell]`, [capgo Animation Performance](https://capgo.app/blog/ultimate-guide-to-animation-performance-in-capacitor-apps/) `[extern]`.

- **WebGL:** Capacitor unterstuetzt WebGL/Canvas solide → glTF + Three.js laeuft (vgl. ionic-team
  Discussion #5562). Fuer 3D-Produkt-Viewer und moderate Echtzeit gut nutzbar.
- **WebGPU:** haengt am **Android System WebView** (auf Chromium basierend). WebGPU erst ab Chrome
  **121+** / Android 12+ mit passenden GPUs → auf vielen Geraeten noch nicht garantiert. **WebGL2-Fallback
  erzwingen** und auf Low-End testen. (Jetpack WebGPU-Bindings sind fuer native Kotlin-Apps, nicht den WebView.)
- **Performance-Hinweis (extern):** Capacitor ist fuer "graphically-intensive" Apps nicht erste Wahl
  — fuer schwere AAA-Echtzeit eher native Engine. Fuer Produktvisualisierung/UI-3D ist es ok.
- **Animationen:** GPU-beschleunigte Properties (`transform`, `opacity`) fuer DOM/UI-Overlays;
  3D bleibt im Canvas. GSAP/Web Animations API fuer komplexe UI-Animationen.

---

## §8 — Performance (beide Inhaltstypen)

Quelle: [utsubo 100 Three.js Tips 2026](https://www.utsubo.com/blog/threejs-best-practices-100-tips) `[extern]` 2026, [threejs.org docs] `[offiziell]`.

- **Draw Calls:** Ziel **< 100/Frame**. **Instancing** (`InstancedMesh`) und Batching/Merging
  reduzieren Draw Calls um **90%+**. Gleiche Materialien teilen, Geometrien mergen wo statisch.
- **GPU-Speicher:** KTX2/Basis statt PNG/JPG → ~10x weniger VRAM, bleibt komprimiert auf der GPU.
- **WebGPU-Vorteile:** Render Bundles (Babylon ~10x), Compute-Shader, weniger CPU-Overhead bei vielen
  Objekten, bessere Akkulaufzeit auf Mobile.
- **Render-Loop:** On-Demand-Rendering bei statischen Produkt-Viewern (nur rendern wenn sich was aendert,
  in R3F `frameloop="demand"`) → spart massiv Strom/CPU.
- **Texturen/Geometrie:** Mip-Maps an, Texturgroessen als Zweierpotenz, unnoetige UV-Sets/Attribute
  per gltf-transform entfernen (`prune`/`dedup`). Frustum-Culling/LOD fuer grosse Szenen.
- **Pixel Ratio:** `renderer.setPixelRatio(Math.min(devicePixelRatio, 2))` — auf High-DPI-Mobile
  nicht ungebremst rendern.

---

## §9 — Haeufige Fallen (Quick-Reference)

| Symptom | Ursache | Fix | Versionen/Kontext |
|---------|---------|-----|-------------------|
| Speicher waechst stetig, FPS faellt | Geometrie/Material/Textur/RenderTarget nicht freigegeben | Beim Entladen explizit `.dispose()` auf Geometry, Material, Texture, RenderTarget; In R3F raeumt der Reconciler vieles auf, aber manuell erzeugte Ressourcen selbst freigeben | Three.js r842 |
| Aliasing trotz `antialias:true` | EffectComposer/Postprocessing umgeht das eingebaute MSAA | AA-Pass (SMAA/TAA/FXAA) ans Ende der Composer-Kette; oder pmndrs/postprocessing nutzen | Three.js r842, postprocessing |
| Farben/Beleuchtung "ausgewaschen" oder falsch | Color Space falsch: Albedo nicht sRGB, oder Output-Color-Space/ToneMapping fehlt | `outputColorSpace=SRGBColorSpace`, `ACESFilmicToneMapping`, Color-Texturen sRGB / Daten-Maps Linear; bei WebGL OutputPass am Ende | Three.js r842 |
| WebGPU-Szene crasht/leer in Firefox | Firefox liefert WebGPU mid-2026 noch nicht default | Auf WebGPURenderer-Auto-Fallback vertrauen UND WebGL2-Pfad real testen | Firefox 145, 2026 |
| R3F + WebGPU bricht (postprocessing) | R3F unterstuetzt WebGPURenderer Anfang 2026 noch nicht voll | WebGL2 bleiben oder vanilla Three.js fuer WebGPU; R3F-Updates abwarten | R3F v9.x |
| GLB laedt nicht (KHR_draco/KTX2 Fehler) | DRACOLoader/KTX2Loader nicht am GLTFLoader registriert | Beide Loader setzen; KTX2Loader `detectSupport(renderer)`; Decoder-Pfade korrekt | Three.js r842 |
| Reflexionen "flach"/falsche Roughness | HDR nicht durch PMREMGenerator gefiltert oder als `background` statt `environment` gesetzt | `PMREMGenerator.fromEquirectangular(...)` → `scene.environment`; Input ~1k | Three.js r842 |
| PMREMGenerator wirft Fehler nach erstem Gebrauch | mehrere Instanzen, eine disposed → andere unbrauchbar (quasi statisch) | Genau EINE Instanz halten/wiederverwenden | Three.js r842 |
| macOS-App ruckelt/max 60fps trotz ProMotion | WKWebView in Tauri ist auf 60fps gedeckelt | Akzeptieren, oder fuer 120Hz nativen Render-Pfad statt WebView | Tauri v2.10.1 macOS |
| WebGPU fehlt auf Android-Geraet | System WebView/Chrome < 121 oder Android < 12 / GPU nicht unterstuetzt | WebGL2-Fallback erzwingen, auf Zielgeraeten testen | Capacitor 2026 |

---

## Versions-Schnellreferenz (Stand 2026-06-13)

- Three.js: **r842** (2026-06-09), WebGPURenderer produktionsreif seit r171 (Sep 2025), Auto-Fallback WebGL2.
- Babylon.js: **9.2.1** (2026-04-13); Frame Graph v1 seit 9.0; WebGPU seit 5.0, native WGSL seit 2024.
- WebGPU Browser: Chrome/Edge/Safari 26 default an; Firefox noch nicht default (mid-2026); Android ab Chrome 121.
- R3F: fiber **v9.5+** + React 19; drei **9.116+**; postprocessing **3+**; rapier **2+**. WebGPU noch nicht voll.
- Tauri: **v2.10.1** (2026-03-04), Desktop + Mobile stable.
- Pipeline-Tool: gltf-transform (KTX2/Basis + Draco/meshopt).

---

## Bezug ↔ Bug-Almanach

Gegenseite (bekannte Bugs/Fallen, das WAS-schiefgeht): `bugs/web/3d-threejs-webgpu.md`.
Jeder Best-Practice-Abschnitt hat dort einen oder mehrere konkrete Bug-Eintraege.

| Best-Practice-Abschnitt (hier) | Bug-Almanach (→ `bugs/web/3d-threejs-webgpu.md`) |
|--------------------------------|--------------------------------------------------|
| §1 Bibliotheks-Wahl (R3F-Einschraenkung, Babylon) | §7 Firefox/R3F-WebGPU; §9 TSL; §14 Babylon WebGPU/Snapshot |
| §2 WebGPU vs WebGL Strategie | §7 Firefox/R3F-WebGPU; §8 WebGPU async init |
| §3 PBR/HDR/IBL (PMREMGenerator-Falle) | §5 Flache Reflexion / PMREM-Crash |
| §4 Postprocessing (AA-Falle, Tone Mapping/Farbe, WebGPU-Pipeline) | §2 Aliasing; §3 Farben/Color-Space; §8 async init; §9 TSL/onBeforeCompile |
| §5 glTF/Draco/KTX2 (Loader-Setup-Falle, detectSupport) | §4 Draco/KTX2-Loader; §10 detectSupport deprecated |
| §6 Verpackung Desktop: Tauri 2 (macOS-Falle) | §1 macOS 60fps-Cap; §12 Tauri asset/CORS/WASM-Pfad |
| §7 Verpackung Android: Capacitor | §11 WebGL context lost mobile; §13 Android-Performance |
| §8 Performance (Draw Calls, dispose, Pixel-Ratio) | §6 Memory-Leak/dispose; §13 Android-Performance |
| §9 Haeufige Fallen (Quick-Reference) | deckt §2-§6, §8, §10 ab (dort detailliert mit Quellen) |
