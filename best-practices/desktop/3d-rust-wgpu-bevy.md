# 3D mit Rust (wgpu/Bevy, cross-platform) — Best Practices (Stand 2026-06-13, Version Bevy 0.18 / wgpu 29.x)

> Recherche-Stand 2026-06-13. Aktuelle stabile Versionen: **Bevy 0.18** (veroeffentlicht Maerz 2026, in Folge von 0.17 vom 30.09.2025), **wgpu 29.0.3** (02.05.2026). Bevy 0.19 mit weiteren Solari-Verbesserungen bereits in Entwicklung (Stand April 2026). Quellen siehe je Abschnitt; `[offiziell]` = bevy.org / gfx-rs / docs.rs, `[extern]` = sonstige seriose Quellen.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | "Soll ich wgpu oder Bevy nehmen?" | Standard = **Bevy** (Engine, ECS, fertige PBR-Pipeline). wgpu nur fuer eigenen Low-Level-Renderer / Custom-Pipeline / minimale Abhaengigkeiten. wgpu ist ohnehin die Basis unter Bevy. | §1 |
| 2 | Spiel/App soll auf macOS + Windows + Android laufen | Bevy waehlen — wgpu-Backend deckt Metal/DX12/Vulkan ab. Android-Build noch rau: `cargo-apk` nur fuer Dev, fuer Play Store AAB via `xbuild`-Fork noetig. Template `bevy_game_template` als Startpunkt. | §6 |
| 3 | Materialien sehen flach/unecht aus | `StandardMaterial` (PBR) nutzen, korrekte metallic/roughness-Werte, glTF mit eingebetteten PBR-Texturen importieren. In 0.18 mehrere PBR-Bugs gefixt → Quali-Sprung. | §2, §5 |
| 4 | Helle Materialien / Neon sollen leuchten | `Camera { hdr: true }` + `emissive` am Material + `Bloom`-Komponente + filmisches Tonemapping (`TonyMcMapface`). Bloom braucht HDR. | §3, §4 |
| 5 | Szene wirkt "platt", kein Ambient/Schatten | SSAO aktivieren (seit 0.11), Atmospheric Scattering (0.16+, in 0.18 generalisiert + Occlusion). Fuer echte GI: Solari (experimentell). | §3 |
| 6 | Fotorealistische Echtzeit-Beleuchtung gewuenscht | **Bevy Solari** (Raytracing, seit 0.17, experimentell). Braucht GPU mit inline ray queries; Denoising praktisch NVIDIA-only (DLSS-RR). Nicht production-ready. | §3 |
| 7 | glTF-Modell laedt nicht / nur teilweise | Asset-Label angeben: `model.glb#Scene0` fuer erste Szene. Ohne Label weiss Bevy nicht, welchen Teil es spawnen soll. `Handle<Gltf>` fuer benannte Teile. | §5 |
| 8 | Spiel laeuft extrem langsam | Debug-Build = katastrophale Runtime-Perf. `--release` ODER `[profile.dev] opt-level=1` + `opt-level=3` fuer Dependencies. Sonst sinnlos zu messen. | §7 |
| 9 | FPS bricht bei vielen Objekten ein | Materialien/Texturen konsolidieren — viele unique Materials killen Batching. Fixe Material-Anzahl statt random kann FPS verdreifachen. GPU-driven rendering (0.16+) + occlusion culling nutzen. | §7, §8 |
| 10 | Build dauert ewig | Dynamic-linking-Feature in Dev (`bevy/dynamic_linking`), schnellerer Linker (lld/zld/mold), Cranelift-Backend fuer Dev-Compiles. | §7 |

---

## §1 wgpu vs. Bevy — wann was [offiziell]

Quelle: Bevy Cheat Book "Render Architecture Overview" (bevy-cheatbook.github.io/gpu/intro.html), wgpu.rs — abgerufen 2026-06-13.

- **wgpu** ist eine sichere, portable Grafik-Bibliothek fuer Rust auf Basis des **WebGPU**-Standards. Laeuft nativ auf **Vulkan, Metal, DirectX 12, OpenGL ES** und im Browser via WebAssembly. Geeignet fuer general-purpose Grafik UND Compute auf der GPU.
- **Bevy** ist eine data-driven Game-Engine (ECS) in Rust. **wgpu bildet die unterste Schicht von Bevys Rendering** — man kann wgpu sogar direkt aus dem Bevy-Render-Framework heraus ansprechen, wenn man maximale GPU-Kontrolle braucht.

**Entscheidungsregel:**
- **Bevy** = Default fuer alles, was eine Engine braucht: Szenengraph, ECS, fertige PBR-Pipeline, Asset-System, glTF, Post-Processing, Audio, UI, Input. Schnell schoene Ergebnisse.
- **wgpu pur** = nur wenn du (a) einen eigenen Renderer / Custom-Render-Algorithmus baust (z.B. spezialisierter Path-Tracer, wissenschaftliche Visualisierung, Voxel-Engine), (b) minimale Abhaengigkeiten willst, (c) volle Kontrolle ueber jeden Draw-Call/Pipeline-State brauchst. Preis: du baust Kamera, Materialien, Beleuchtung, glTF-Loading selbst.
- **Hybrid:** Bevy nehmen und punktuell mit eigenen wgpu-Render-Nodes/Custom-Materials erweitern.

**Aktuelle wgpu-Versionen** (docs.rs/crate/wgpu, gfx-rs/wgpu CHANGELOG, abgerufen 2026-06-13): 29.0.3 (02.05.2026), 29.0.0 (19.03.2026), 28.0.0 (18.12.2025), 27.0.x (Okt 2025), 26.0.x (Jul 2025). Sehr aktiver Release-Zyklus; Bevy-Version pinnt jeweils eine bestimmte wgpu-Version — wgpu nicht eigenmaechtig hochziehen, wenn man Bevy nutzt.

---

## §2 PBR & Materialien [offiziell]

Quelle: bevy.org/news/bevy-0-18, docs.rs/bevy (StandardMaterial), DeepWiki "PBR Materials and Shaders" — abgerufen 2026-06-13.

- Bevys Standard-Materialmodell ist **`StandardMaterial`** (physically-based rendering, metallic-roughness-Workflow). Das ist der richtige Weg fuer realistische Oberflaechen — Texturen fuer base color, metallic, roughness, normal, occlusion, emissive.
- **Bevy 0.18 hat mehrere langjaehrige PBR-Bugs gefixt** ("PBR Shading Fixes") → spuerbare Qualitaetsverbesserung ohne eigenes Zutun. Wer auf alter Version sitzt, sollte updaten, wenn die Materialien "leicht falsch" aussehen.
- **0.18 "Fullscreen Material":** neuer High-Level-Materialtyp, mit dem man Fullscreen-Post-Processing-Shader sehr einfach definieren kann (vorher manueller Render-Node noetig).
- Fuer Custom-Looks: eigene Materials via `Material`-Trait (WGSL-Shader). Aber: jede unique Material-Instanz kostet Batching-Performance (siehe §7).

**Praxis:** Realistische Assets immer aus DCC-Tools (Blender) als **glTF mit eingebetteten PBR-Maps** exportieren — Bevy mappt diese direkt auf `StandardMaterial`. Nicht Material-Werte von Hand raten.

---

## §3 Beleuchtung & Global Illumination [offiziell]

Quellen: bevy.org/news/bevy-0-16, -0-17, -0-18; jms55.github.io Solari-Posts (0.17/0.18/0.19); GitHub Issue #23535 — abgerufen 2026-06-13.

**Klassische Beleuchtung (stabil):**
- Directional/Point/Spot Lights, Shadows.
- **SSAO** (Screen Space Ambient Occlusion) seit Bevy 0.11 — simuliert Verdeckung von indirektem Diffuslicht, hebt Render-Qualitaet deutlich. Guenstig zuschaltbar.
- **Procedural Atmospheric Scattering** seit 0.16: physikalisch basierter Himmel fuer beliebige Tageszeiten, minimal Performance-Kosten.
- **Bevy 0.18 — Atmosphere Occlusion + Generalized Scattering Media:** Die prozedurale Atmosphaere beeinflusst jetzt, wie Licht Objekte erreicht (Occlusion). Atmosphaere ist anpassbar fuer beliebige Typen: Wuestenhimmel, neblige Kueste, nicht-erdaehnliche Planeten.
- **Light Textures** (seit 0.17): Lichtintensitaet ueber Texturen modulieren (z.B. Gobos, Fensterschatten).

**Echtzeit-GI / Raytracing — Bevy Solari (`bevy_solari`):**
- Eingefuehrt in 0.17, weiterentwickelt in 0.18 und 0.19. **Experimentell, NICHT production-ready** (Stand 2026-06).
- Bietet raytraced lighting: emissive Meshes casten echtes Licht + Schatten, hunderte schattenwerfende Lichter, voll dynamische Szenen.
- 0.18: Solari unterstuetzt jetzt **specular materials** (multiscattering GGX lobe, additiv zur Diffuse-Lobe).
- **Hardware-Anforderung:** GPU mit Raytracing-Support via **inline ray queries**. Mesh-Anforderungen: unkomprimierte position/normal/uv_0/tangent-Attribute, triangle-list-Topologie, 32-bit Indizes.
- **Denoising in der Praxis NVIDIA-only:** setzt auf **DLSS Ray Reconstruction** (FSR-RR ist derzeit DX12-only, kein Vulkan). Auf AMD/Intel/Apple also stark eingeschraenkt.
- **Konsequenz:** Fuer cross-platform fotorealistisch HEUTE nicht verlassbar. Wer fotorealistisch + portabel will: auf klassischer PBR + SSAO + Atmosphaere + sauberen Lightmaps/Baked-GI aufbauen, Solari als optionales High-End-Feature auf NVIDIA-Desktops.

**DLSS** (seit 0.17): Anti-Aliasing + Upscaling auf NVIDIA RTX. Hilft, Solari/teure Effekte bezahlbar zu machen — aber eben NVIDIA-gebunden.

---

## §4 Postprocessing (Bloom / Tonemapping / HDR) [offiziell]

Quellen: docs.rs/bevy bloom, Bevy Cheat Book "HDR and Tonemapping", bevy.org/news/bevy-0-9 & -0-11 — abgerufen 2026-06-13.

- **HDR ist die Voraussetzung** fuer hochwertiges Post-Processing. Per-Camera-Toggle: `Camera { hdr: true, ..default() }`. Erst dann behaelt Bevy die HDR-Daten intern, damit nachfolgende Passes (Bloom etc.) sie nutzen koennen.
- **Bloom:** `Bloom`-Komponente an die Kamera. Wird typischerweise mit `StandardMaterial::emissive` kombiniert, damit helle Materialien einen Halo bekommen. Implementierung: parametrische Kurve, die zwischen mehreren geblurrten (niederfrequenten) Mip-Bildern der View blendet.
- **Tonemapping ist Pflichtpartner von Bloom:** filmisches Tonemapping wie **`Tonemapping::TonyMcMapface`** entsaettigt helle Farben — so behaelt der Bloom-Halo auf dunklem Hintergrund die Farbsaettigung und wirkt nicht ausgewaschen. Bloom ohne passendes Tonemapping sieht oft "blass" aus.
- Reihenfolge der Effekte: HDR-Render → Bloom (nutzt HDR) → Tonemapping → Display.
- Bevy 0.18 macht eigene Fullscreen-Post-Effekte trivial (siehe §2, "Fullscreen Material").

---

## §5 glTF-Asset-Pipeline [offiziell]

Quellen: docs.rs/bevy/latest/bevy/gltf, bevy.org/examples (load-gltf), DeepWiki "Asset Loaders and Formats", GitHub #13681 — abgerufen 2026-06-13.

- Bevy bringt einen **glTF 2.0**-AssetLoader mit (`.gltf` und `.glb`). glTF ist der empfohlene Austauschstandard fuer 3D-Szenen (aus Blender/Maya etc. exportieren).
- **Wichtigste Falle — Asset-Labels:** Beim Spawnen einer glTF-Szene muss man dem AssetServer sagen, WELCHEN Teil man will. `asset_server.load("model.glb#Scene0")` laedt die erste Szene. **Ohne Label weiss Bevy nicht, welchen Teil der glTF-Datei es laden soll** → nichts erscheint / Fehler. Weitere Labels: `#Mesh0/Primitive0`, `#Material0`, `#Animation0`, benannte Nodes.
- Fuer Zugriff auf die ganze Datei: `Handle<Gltf>` laden; sobald geladen, ueber das `Gltf`-Asset auf benannte Teile (Szenen, Meshes, Materials, Animationen, Nodes) zugreifen.
- **glTF Extras:** Custom-Metadaten aus dem DCC-Tool kommen als `GltfExtras`, `GltfMaterialExtras`, `GltfMeshExtras`, `GltfSceneExtras` an — abfragbar an den gespawnten Entities. Gut fuer Game-Logik-Hints (z.B. "dies ist ein Spawn-Point").
- Roadmap: Die Bevy-Devs entkoppeln glTF-Loading von der Szenenerzeugung (Issue #13681), damit man die Szenen-Konstruktion anpassen kann. Bis dahin ist viel Loading-Logik fest in den Loader gebacken.

---

## §6 Cross-Platform-Build: macOS / Windows / Android [offiziell + extern]

Quellen: bevy.org/learn/quick-start/getting-started/setup, Bevy Cheat Book "Bevy on Different Platforms" + "From macOS to Windows", GitHub bevy/examples/mobile/android_basic/readme.md, Issue #19021, NiklasEi/bevy_game_template, Erik Horton Blog `[extern]` — abgerufen 2026-06-13.

**Desktop (macOS + Windows) — reif:**
- wgpu deckt automatisch ab: macOS → **Metal**, Windows → **DirectX 12** (Fallback Vulkan), Linux → **Vulkan**. Kein plattformspezifischer Renderer-Code noetig.
- Auslieferung: native Binary. macOS ggf. `.app`-Bundle + Code-Signing/Notarization (Gatekeeper). Windows `.exe` (statisch, keine Runtime-Abhaengigkeiten fuer Endnutzer).
- Cross-Compile macOS → Windows ist moeglich, aber unbequem; CI mit nativen Runnern (Template `bevy_game_template` hat fertige CI/CD fuer Web/Windows/Linux/iOS/Android).

**Android — funktioniert, aber rau (KRITISCH fuer Erwartungsmanagement):**
- Dev/Test: **`cargo-apk`** (`cargo apk run`). Targetet per Default API-Level 33 (Play-Store-Minimum).
- **Stolperstein 1 — Store-Publishing:** `cargo-apk` kann KEIN **AAB** (Android App Bundle) erzeugen, das der Play Store inzwischen verlangt. APKs lassen sich nicht mehr hochladen. Loesung im offiziellen Workflow: `package.metadata.android` (fuer cargo-apk) PLUS `manifest.yaml` fuer einen **xbuild-Fork** im `release-android-google-play`-Workflow, der das AAB baut. Zwei-Tool-Ansatz.
- **Stolperstein 2 — Doku:** Lange keine klare README fuer iOS/Android-Builds; `cargo apk run` scheiterte oft mit obskuren Fehlern (Issue #19021, Stand Mai 2025). Inzwischen `examples/mobile/android_basic/readme.md` als Referenz. Erwarte trotzdem Fummelei beim ersten "Hello World" auf dem Geraet.
- **Empfehlung:** Mit dem **`NiklasEi/bevy_game_template`** starten — enthaelt funktionierende CI/CD inkl. Android-AAB-Pipeline. Nicht von Null aufsetzen.
- Solari/Raytracing auf Android: praktisch nicht verfuegbar — fuer Mobile bei klassischer PBR bleiben.

---

## §7 Performance [offiziell + extern]

Quellen: Bevy Cheat Book "Slow Performance", CleanCut/bevy_template, bevy.org/news/bevy-0-16, GitHub Discussions #9146 & #13325 — abgerufen 2026-06-13.

- **Debug-Builds sind unbrauchbar langsam.** Rust ohne Compiler-Optimierungen ist sehr langsam, und Bevys Default-`cargo build`-Debug-Settings fuehren zu "awful runtime performance". IMMER `--release` testen, oder in `Cargo.toml`:
  ```toml
  [profile.dev]
  opt-level = 1            # eigener Code minimal optimiert
  [profile.dev.package."*"]
  opt-level = 3            # Dependencies (inkl. Bevy) voll optimiert
  ```
  Das beschleunigt Runtime massiv, ohne die Recompile-Zeiten des eigenen Codes zu killen (Dependencies werden nur beim Clean-Build langsamer).
- **GPU-driven rendering** (seit 0.16): mehr Arbeit auf die GPU verlagert, optimiert komplexe Szenen.
- **Occlusion culling** (seit 0.16): verdeckte Objekte werden nicht gerendert.
- **Build-Zeiten** (nicht Runtime): Bevy ist gross und kompiliert lang. Hebel: `bevy/dynamic_linking`-Feature in Dev, schnellerer Linker (lld auf Linux/Win, **zld**/lld/mold), Cranelift-Backend fuer schnellere Dev-Compiles (erzeugt aber langsamere Binaries — nur fuer Iteration, nicht fuer Release).

---

## §8 Haeufige Fallen [offiziell + extern]

Quellen: Bevy Cheat Book "Slow Performance", GitHub Discussions #13325 (Material-Batching), docs.rs bloom, examples load-gltf — abgerufen 2026-06-13.

- **Zu viele unique Materials → Batching bricht.** Random generierte Materialien killen die Performance; Umstieg auf eine FIXE, kleine Anzahl Materials kann die FPS verdreifachen. Texturen/Materials konsolidieren (Texture-Atlas, Material-Sharing). Sprites: pro Texture-Handle + z-Level gebatcht; Vertex-Colors kosten extra (130k → 100k Sprites @60fps), gefaerbte und ungefaerbte Sprites batchen NICHT zusammen.
- **Bloom ohne HDR = kein Effekt.** Erst `Camera { hdr: true }`, sonst hat Bloom nichts zum Arbeiten.
- **Bloom ohne filmisches Tonemapping = blass/ausgewaschen.** Immer `TonyMcMapface` o.ae. dazu.
- **glTF spawnt nichts:** fehlendes `#Scene0`-Label (siehe §5).
- **Android `cargo apk run` schlaegt fehl / kein Store-Upload moeglich:** AAB-Limitierung von cargo-apk (siehe §6) — Template + xbuild nutzen.
- **Solari "rendert nichts"** (Issue #23535): meist fehlende Hardware-Voraussetzung (keine inline ray queries) oder kein Denoiser (DLSS-RR fehlt). Solari ist experimentell — nicht als verlassliche Beleuchtung einplanen.
- **wgpu eigenmaechtig hochziehen** bricht Bevy: Bevy pinnt eine bestimmte wgpu-Version; nicht manuell ueberschreiben.
- **Performance vor `--release` messen** ist sinnlos (siehe §7) — fuehrt zu Fehlentscheidungen.

---

## Quellenliste (abgerufen 2026-06-13)

`[offiziell]`
- bevy.org/news/bevy-0-18 — Release Notes 0.18
- bevy.org/news/bevy-0-17 — Release Notes 0.17 (Solari, DLSS, Light Textures)
- alternativeto.net (Mirror) zu bevy.org 0.16 — GPU-driven, Atmospheric Scattering, Occlusion Culling, Decals
- bevy.org/learn/migration-guides/0-16-to-0-17
- bevy.org/learn/quick-start/getting-started/setup
- bevy.org/examples (load-gltf, bloom)
- docs.rs/bevy (StandardMaterial, gltf, post_process/bloom)
- docs.rs/crate/wgpu (29.0.3), github.com/gfx-rs/wgpu CHANGELOG
- wgpu.rs
- github.com/bevyengine/bevy examples/mobile/android_basic/readme.md, Issues #19021, #13681, #23535, Discussions #9146, #13325
- jms55.github.io Solari-Posts (Bevy 0.17 / 0.18 / 0.19)

`[extern]`
- bevy-cheatbook.github.io (Unofficial Bevy Cheat Book): gpu/intro, pitfalls/performance, graphics/hdr-tonemap, platforms, setup/cross/macos-windows
- github.com/NiklasEi/bevy_game_template (CI/CD-Template Web/Win/Linux/iOS/Android)
- github.com/CleanCut/bevy_template (compile-time-optimiertes Profil)
- blog.erikhorton.com — Deploy Bevy to Android and WASM
- gamefromscratch.com / alternativeto.net — Release-Berichterstattung

---

## Bezug ↔ Bug-Almanach

> Gegenseite (bekannte Bugs & Fallen): `bugs/desktop/3d-rust-wgpu-bevy.md`.

| Best Practice (hier §) | Verwandter Bug-Almanach-Eintrag |
|------------------------|----------------------------------|
| §1 wgpu vs. Bevy | → bugs/desktop/3d-rust-wgpu-bevy.md §3 (Bundle-API), §13 (wgpu-Pinning) |
| §2 PBR & Materialien | → bugs/desktop/3d-rust-wgpu-bevy.md §3 (Mesh3d/MeshMaterial3d), §5 (Emissive > 1.0), §7 (Sampler/Texturen) |
| §3 Beleuchtung & GI | → bugs/desktop/3d-rust-wgpu-bevy.md §11 (Solari rendert nichts) |
| §4 Postprocessing | → bugs/desktop/3d-rust-wgpu-bevy.md §4 (Bloom ohne HDR), §5 (Emissive), §6 (Bloom blass) |
| §5 glTF-Asset-Pipeline | → bugs/desktop/3d-rust-wgpu-bevy.md §1 (fehlendes Label), §2 (180°-Koordinaten-Bug) |
| §6 Cross-Platform-Build | → bugs/desktop/3d-rust-wgpu-bevy.md §8 (DX12 Resize-Crash), §12 (Android AAB) |
| §7 Performance | → bugs/desktop/3d-rust-wgpu-bevy.md §9 (Material-Batching), §10 (Debug-Build) |
| §8 Haeufige Fallen | → bugs/desktop/3d-rust-wgpu-bevy.md (Kurzcheck + §1, §4, §9, §10) |
