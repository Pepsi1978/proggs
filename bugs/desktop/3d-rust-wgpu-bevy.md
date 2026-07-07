# Bekannte Bugs: 3D mit Rust (wgpu/Bevy)

> **PFLICHT-LESEN vor jeder 3D-Arbeit mit Rust.** Stand recherchiert **2026-06-13** fuer **Bevy 0.18 / wgpu 29.x**.
> Bevy bricht oft pro Version (API-Umbau, gepinnte wgpu-Version) — Versionsangaben unten beachten.
> Gegenseite (Best Practices, was man von Anfang an richtig macht): `best-practices/desktop/3d-rust-wgpu-bevy.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | glTF/.glb spawnt nichts, kein Fehler | Asset-Label angeben: `load("model.glb#Scene0")`. Ohne Label weiss Bevy nicht, was es laden soll. | §1 |
| 2 | Modell aus Blender/glTF um 180° verdreht, `looking_at` zeigt falsch | glTF-Koordinaten-Bug. In 0.18 opt-in: `convert_coordinates` am glTF-Loader aktivieren ODER Plugin `bevy_fix_gltf_coordinate_system`. | §2 |
| 3 | `cargo build`-Compile-Fehler `Camera3dBundle`/`PbrBundle` existiert nicht | Bundles sind seit 0.15 deprecated/entfernt. `Camera3d`, `Mesh3d`, `MeshMaterial3d` als Required Components nutzen. | §3 |
| 4 | Bloom hat keinen Effekt | HDR ist Voraussetzung: `Camera { hdr: true, .. }`. | §4 |
| 5 | Emissive-Material leuchtet nicht / bloomt nicht | `emissive`-Werte muessen **> 1.0** sein (z.B. `LinearRgba::rgb(0.,0.,150.)`). Werte ≤ 1.0 bloomen nicht. | §5 |
| 6 | Bloom blass/ausgewaschen | Filmisches Tonemapping dazu: `Tonemapping::TonyMcMapface`. | §6 |
| 7 | Texturen/Pixel-Art unscharf | Sampler auf Nearest: `ImagePlugin::default_nearest()` bzw. `mag_filter: Nearest`. | §7 |
| 8 | App auf Windows crasht beim Fenster-Resize/Minimieren (DX12) | 0.18.0-rc.1-Regression — gefixt in 0.18.0 (PR #22254). Auf finales 0.18.x updaten. | §8 |
| 9 | FPS bricht bei vielen Objekten ein | Zu viele unique Materials → Batching bricht. Wenige Materials/Atlas. Fixe Anzahl kann FPS verdreifachen. | §9 |
| 10 | Spiel extrem langsam | Debug-Build. `--release` ODER `opt-level` anheben (Deps auf 3). Vor `--release` nie Perf messen. | §10 |
| 11 | Solari rendert nur Schwarz/nichts | Fehlende inline ray queries ODER kein Denoiser (DLSS-RR, NVIDIA-only). Experimentell — nicht als verlaessliche Beleuchtung einplanen. | §11 |
| 12 | Android: kein Store-Upload / `cargo apk run` scheitert | cargo-apk kann kein AAB. xbuild-Fork + `bevy_game_template`-Pipeline nutzen. | §12 |
| 13 | wgpu manuell hochgezogen → Bevy bricht | Bevy pinnt wgpu. Version NICHT eigenmaechtig ueberschreiben. | §13 |

---

## 1. glTF spawnt nichts — fehlendes Asset-Label [⭐ HÄUFIG]

- **Symptom:** `asset_server.load("model.glb")` laedt, aber es erscheint nichts in der Szene; oft kein lauter Fehler.
- **Ursache:** Eine glTF-Datei enthaelt mehrere Teile (Szenen, Meshes, Materials, Animationen). Ohne Label weiss Bevy nicht, welchen Teil es spawnen soll.
- **Versionen:** alle Bevy-Versionen (Dauerbrenner, kein Versions-Bug — Bedien-Falle).
- **FIX:** Label angeben: `asset_server.load("model.glb#Scene0")` fuer die erste Szene. Weitere Labels: `#Mesh0/Primitive0`, `#Material0`, `#Animation0`, benannte Nodes. Fuer Zugriff auf die ganze Datei `Handle<Gltf>` laden.
- **Quelle:** https://bevy.org/examples/ (load-gltf), https://docs.rs/bevy/latest/bevy/gltf/

## 2. glTF-Modell um 180° verdreht / falsche Forward-Richtung [⭐ HÄUFIG]

- **Symptom:** Aus Blender/TrenchBroom exportierte oder aus dem Netz geladene glTFs erscheinen in Bevy gespiegelt/um 180° (um Y) gedreht. `Transform::default().looking_at(p, Vec3::Y)` schaut in die **entgegengesetzte** Richtung. 1st-Person-Animationen werden dadurch unsichtbar.
- **Ursache:** glTF definiert forward = +Z, right = -X; Bevy `Transform` definiert forward = -Z, right = +X (beide Y-up, right-handed). Der glTF-Importer ignorierte diesen Unterschied historisch → alles 180° verdreht. Langjaehriger bekannter Bug (#5670).
- **Versionen:** Bug seit Anbeginn. PR #19633 fuehrte ein **opt-in** `convert_coordinates`-Flag am glTF-Loader ein; in 0.18 weiter ausgebaut + dokumentiert (Doku-Update #22355), aber **noch nicht Default** (#19686 diskutiert den Default-Wechsel). Light-Orientation separat gefixt (#20122).
- **FIX:** (a) Koordinaten-Konvertierung am glTF-Loader aktivieren (`convert_coordinates`, 0.18 — experimentell, beachtet die Doku-Warnung), ODER (b) Community-Plugin `bevy_fix_gltf_coordinate_system` (janhohenheim), ODER (c) bewusst alle Modelle 180° um Y vorab rotieren (Workaround). Beim Default-Wechsel in spaeteren Versionen Migrations-Aufwand einplanen.
- **Quelle:** https://github.com/bevyengine/bevy/issues/5670 , https://github.com/bevyengine/bevy/issues/19686 , https://github.com/janhohenheim/bevy_fix_gltf_coordinate_system

## 3. Compile-Fehler: `Camera3dBundle` / `PbrBundle` / `MaterialMeshBundle` nicht gefunden [⭐ HÄUFIG]

- **Symptom:** Code aus aelteren Tutorials kompiliert nicht: `Camera3dBundle`, `Camera2dBundle`, `PbrBundle`, `MaterialMeshBundle` unbekannt; oder Mesh rendert nicht mehr, obwohl der Handle gespawnt wurde.
- **Ursache:** Bevy hat seit 0.15 die Bundle-API durch **Required Components** ersetzt. Bundles sind deprecated/entfernt. Rohe `Handle<Mesh>`/`Handle<Material>` als Komponente rendern nicht mehr — sie muessen in `Mesh3d` / `MeshMaterial3d` gewrappt werden.
- **Versionen:** Umbau ab 0.15, fortgesetzt bis 0.18. Klassischer Migrations-Bruch.
- **FIX:** `Camera3d` (statt `Camera3dBundle`), `Mesh3d(handle)` + `MeshMaterial3d(handle)` (statt `PbrBundle`). Diese Komponenten ziehen `Transform`/`Visibility` automatisch als Required Components. In 0.18 zusaetzlich: `RenderTarget` direkt als Komponente spawnen statt `Camera { target: .. }`.
- **Quelle:** https://bevy.org/news/bevy-0-15/ , https://bevy.org/learn/migration-guides/0-17-to-0-18/

## 4. Bloom ohne Effekt — HDR nicht aktiv [⭐ HÄUFIG]

- **Symptom:** `Bloom`-Komponente an der Kamera, aber gar kein Glow.
- **Ursache:** Bloom arbeitet auf HDR-Daten. Ohne HDR clampt Bevy die Helligkeit, bevor Bloom etwas zu tun hat.
- **Versionen:** alle (Bedien-Falle).
- **FIX:** Per-Camera-Toggle `Camera { hdr: true, ..default() }`. Reihenfolge: HDR-Render → Bloom → Tonemapping → Display.
- **Quelle:** https://bevy-cheatbook.github.io/graphics/bloom.html , https://docs.rs/bevy/latest/bevy/ (bloom)

## 5. Emissive-Material leuchtet/bloomt nicht — Werte zu niedrig

- **Symptom:** `emissive` gesetzt, HDR + Bloom aktiv, aber das Material glueht nicht oder strahlt nicht ab.
- **Ursache:** Damit ein Material bloomt, muessen die emissive-Channel-Werte deutlich **ueber 1.0** liegen. Werte ≤ 1.0 erzeugen keinen Halo. Zusatz-Falle: emissive ist `LinearRgba` (nicht sRGB) — Werte direkt im Linearraum denken.
- **Versionen:** Bevy 0.13+ (emissive ist seit #13212 `LinearRgba`).
- **FIX:** Hohe emissive-Werte: z.B. `emissive: LinearRgba::rgb(0.0, 0.0, 150.0)` oder bis `1000.0`. HDR + Bloom muessen aktiv sein. Hinweis: emissive faerbt nur das Material hell, es **beleuchtet nicht** die Umgebung (dafuer echte Lights oder Solari).
- **Quelle:** https://github.com/bevyengine/bevy/issues/4095 , https://github.com/bevyengine/bevy/blob/main/examples/3d/bloom_3d.rs

## 6. Bloom blass/ausgewaschen — kein filmisches Tonemapping [⭐ HÄUFIG]

- **Symptom:** Bloom funktioniert, sieht aber milchig/entsaettigt aus.
- **Ursache:** Ohne filmisches Tonemapping behaelt der Halo auf dunklem Hintergrund keine Saettigung.
- **Versionen:** alle.
- **FIX:** `Tonemapping::TonyMcMapface` (o.ae.) an der Kamera setzen.
- **Quelle:** https://bevy-cheatbook.github.io/graphics/hdr-tonemap.html

## 7. Texturen / Pixel-Art unscharf — falscher Sampler

- **Symptom:** Pixel-Art oder scharfe Texturen erscheinen verwaschen beim Vergroessern; Texturen in der Distanz uebermaessig blurry.
- **Ursache:** Default-Sampler ist Linear. Fuer Pixel-Art braucht man Nearest. Fuer schraege Oberflaechen fehlt anisotropes Filtering.
- **Versionen:** alle; Anisotropie-Regression bei Basis-Universal-Texturen in 0.16 (#18646).
- **FIX:** Pixel-Art: `ImagePlugin::default_nearest()` beim App-Setup, oder pro Bild `mag_filter: ImageFilterMode::Nearest`. Schraege 3D-Flaechen: `anisotropy_clamp: 16` am Sampler (`default_sampler` des `ImagePlugin`).
- **Quelle:** https://github.com/bevyengine/bevy/discussions/9118 , https://github.com/bevyengine/bevy/issues/18646

## 8. Windows-DX12 Crash bei Fenster-Resize / Minimieren

- **Symptom:** App crasht beim Verkleinern/Vergroessern oder Minimieren des Fensters auf Windows (DX12). Fehler `ResizeBuffers ... window is in use` bzw. Access Violation.
- **Ursache:** Swapchain-Texture-Views wurden nicht sauber gedroppt — Regression in 0.18.0-rc.1 gegenueber 0.17.3 (#22225). Verwandt: Minimieren-Crash #23399.
- **Versionen:** 0.18.0-rc.1 betroffen. **Gefixt** in 0.18.0 final via PR #22254 ("Make sure swapchain texture views are dropped").
- **FIX:** Auf finales **0.18.0+** updaten (nicht auf rc-Builds sitzen bleiben). Aeltere DX12-Resize-Crashes (#15077, Treiber #12199) ggf. Vulkan-Backend als Fallback testen.
- **Quelle:** https://github.com/bevyengine/bevy/issues/22225 , https://github.com/bevyengine/bevy/pull/22254

## 9. FPS bricht ein — zu viele unique Materials (Batching bricht) [⭐ HÄUFIG]

- **Symptom:** Bei vielen Objekten faellt die Framerate stark ab.
- **Ursache:** Jede unique Material-Instanz unterbricht das Batching. Random generierte Materialien killen die Performance.
- **Versionen:** alle.
- **FIX:** Fixe, kleine Anzahl Materials / Texture-Atlas / Material-Sharing — kann die FPS verdreifachen. GPU-driven rendering (0.16+) + Occlusion Culling nutzen.
- **Quelle:** https://github.com/bevyengine/bevy/discussions/13325 , https://bevy-cheatbook.github.io/pitfalls/performance.html

## 10. Spiel extrem langsam — Debug-Build [⭐ HÄUFIG]

- **Symptom:** Selbst einfache Szenen ruckeln, Runtime-Performance "awful".
- **Ursache:** Rust ohne Optimierungen ist sehr langsam; Bevys Default-Debug-Settings fuehren zu katastrophaler Runtime-Perf.
- **Versionen:** alle.
- **FIX:** `--release` testen, ODER in `Cargo.toml`: `[profile.dev] opt-level=1` + `[profile.dev.package."*"] opt-level=3`. Perf NIE vor `--release` messen.
- **Quelle:** https://bevy-cheatbook.github.io/pitfalls/performance.html

## 11. Solari rendert nur Schwarz / nichts (#23535)

- **Symptom:** Mit `SolariPlugins` nur schwarzer Hintergrund, obwohl GPU 7-16% Last zeigt; ohne Solari rendert alles korrekt. Keine wgpu-Validation-Errors, keine bevy_solari-Fehler im Trace.
- **Ursache:** Fehlende Hardware-Voraussetzung (GPU ohne inline ray queries) und/oder fehlender Denoiser. Denoising in der Praxis NVIDIA-only (DLSS Ray Reconstruction; FSR-RR ist DX12-only, kein Vulkan).
- **Versionen:** Solari seit 0.17 (experimentell), 0.18/0.19 weiterentwickelt — weiterhin **NICHT production-ready**.
- **FIX:** Solari nicht als verlaessliche Beleuchtung einplanen. Hardware-Voraussetzungen pruefen: unkomprimierte position/normal/uv_0/tangent-Attribute, triangle-list, 32-bit Indizes. Fuer cross-platform fotorealistisch: klassische PBR + SSAO + Atmosphaere + Baked-GI; Solari nur als optionales High-End-Feature auf NVIDIA-Desktops.
- **Quelle:** https://github.com/bevyengine/bevy/issues/23535 , https://github.com/bevyengine/bevy/issues/20203 (Tracking)

## 12. Android — cargo-apk kann kein AAB / kein Store-Upload [⭐ HÄUFIG]

- **Symptom:** `cargo apk run` scheitert mit obskuren Fehlern; fertiges APK laesst sich nicht in den Play Store hochladen.
- **Ursache:** Play Store verlangt **AAB** (Android App Bundle); `cargo-apk` kann nur APK. Lange fehlte zudem klare Doku (#19021).
- **Versionen:** Dauerthema; android_basic-Readme inzwischen vorhanden.
- **FIX:** Zwei-Tool-Ansatz: `package.metadata.android` (cargo-apk fuer Dev) PLUS `manifest.yaml` fuer einen **xbuild-Fork** (`cargo install --git https://github.com/NiklasEi/xbuild`) im `release-android-google-play`-Workflow fuers AAB. Am besten mit `NiklasEi/bevy_game_template` starten (fertige CI/CD inkl. AAB-Pipeline). Solari/Raytracing auf Android praktisch nicht verfuegbar — bei klassischer PBR bleiben.
- **Quelle:** https://github.com/bevyengine/bevy/issues/19021 , https://github.com/NiklasEi/bevy_game_template , https://www.nikl.me/blog/2023/github_workflow_to_publish_android_app/

## 13. wgpu eigenmaechtig hochgezogen → Bevy bricht

- **Symptom:** Nach manuellem Anheben der wgpu-Version (Cargo-Patch/Override) bricht der Bevy-Build oder Renderer.
- **Ursache:** Bevy pinnt eine bestimmte wgpu-Version; API-Breaks zwischen wgpu-Versionen. wgpu hat sehr schnellen Release-Zyklus (29.0.3 am 02.05.2026, 29.0.0 19.03.2026, 28.0.0 18.12.2025).
- **Versionen:** alle Bevy/wgpu-Kombinationen.
- **FIX:** Die von Bevy gepinnte wgpu-Version NICHT ueberschreiben. wgpu-Updates ueber ein Bevy-Update beziehen, nicht direkt.
- **Quelle:** https://github.com/gfx-rs/wgpu (CHANGELOG) , https://docs.rs/crate/wgpu/

---

## Fix-Status

| # | Bug | Status | Beleg |
|---|-----|--------|-------|
| 1 | glTF kein Label | Bedien-Falle (kein Fix noetig) | Doku/Examples |
| 2 | glTF 180°-Rotation | Teil-Fix: opt-in `convert_coordinates` ab 0.18, **noch nicht Default** | #19686 (Default offen), PR #19633 |
| 3 | Bundle deprecated | Behoben durch API-Umbau (Required Components ab 0.15) | Migration Guide 0.17→0.18 |
| 4 | Bloom ohne HDR | Bedien-Falle | Cheat Book |
| 5 | Emissive zu niedrig | Bedien-Falle | #4095, bloom_3d.rs |
| 6 | Bloom blass | Bedien-Falle | Cheat Book |
| 7 | Texturen unscharf | Bedien-Falle; Anisotropie-Regression #18646 (Basis-Universal) ggf. **offen** | #9118, #18646 |
| 8 | DX12 Resize-Crash | **Gefixt** in 0.18.0 (PR #22254) | #22225 → #22254 |
| 9 | Material-Batching | Bedien-Falle (Best Practice) | #13325 |
| 10 | Debug-Build langsam | Bedien-Falle | Cheat Book |
| 11 | Solari rendert nichts | **Offen/experimentell** — Hardware/Denoiser-bedingt | #23535, #20203 |
| 12 | Android AAB | Workaround (xbuild-Fork); kein offizieller cargo-apk-Fix | #19021 |
| 13 | wgpu-Override | Bedien-Falle | wgpu CHANGELOG |

**Offen / unsicher (ehrlich markiert):**
- **#2 glTF-Koordinaten:** Fix existiert nur als opt-in; der Default-Wechsel ist noch in Diskussion (#19686). Bei einem spaeteren Default-Wechsel BRICHT bestehender Code/Asset-Verhalten — Migration Guide der jeweiligen Version als Beleg pruefen.
- **#7 Anisotropie-Regression (#18646):** Status auf 0.18 nicht eindeutig verifizierbar (gh-CLI nicht verfuegbar). Bei Basis-Universal-Texturen testen.
- **#11 Solari:** experimentell, kein verlaesslicher Fix-Status — Hardware-/Plattform-abhaengig.

**Ehrlichkeits-Hinweis:** Bevy bricht haeufig pro Version (API-Umbau wie Bundles→Required Components, gepinnte wgpu-Version). Der jeweilige **Migration Guide** (`bevy.org/learn/migration-guides/<X>-to-<Y>`) ist der autoritative Beleg fuer "war es ein Bug oder eine bewusste Aenderung". gh-CLI war nicht verfuegbar — Issue-Detail-Status (offen/geschlossen) konnte nicht 1:1 geprueft werden; oben markierte Faelle entsprechend mit Vorbehalt.

---

## Bezug ↔ Best-Practices

| Bug (hier) | Verwandte Best Practice (Gegenseite §) |
|------------|----------------------------------------|
| §1 glTF-Label | §5 glTF-Asset-Pipeline |
| §2 glTF-Rotation | §5 glTF-Asset-Pipeline (Koordinaten) |
| §3 Bundle-API | §1 wgpu vs. Bevy / §2 PBR-Materialien |
| §4–6 Bloom/HDR/Tonemapping/Emissive | §4 Postprocessing |
| §7 Sampler/Texturen | §2 PBR & Materialien |
| §8 DX12-Crash | §6 Cross-Platform-Build (Desktop) |
| §9 Material-Batching | §7 Performance / §8 Haeufige Fallen |
| §10 Debug-Build | §7 Performance |
| §11 Solari | §3 Beleuchtung & GI |
| §12 Android AAB | §6 Cross-Platform-Build (Android) |
| §13 wgpu-Pinning | §1 wgpu vs. Bevy |

---

## Pflicht-Checkliste vor dem Start

- [ ] glTF immer mit Label laden (`#Scene0`) — sonst spawnt nichts (§1).
- [ ] glTF-Orientierung pruefen: `convert_coordinates` oder Fix-Plugin gegen den 180°-Bug (§2).
- [ ] Aktuelle Spawn-API verwenden: `Camera3d`, `Mesh3d`, `MeshMaterial3d` — keine `*Bundle` aus alten Tutorials (§3).
- [ ] Fuer Glow: `hdr: true` + `emissive > 1.0` + `Bloom` + `TonyMcMapface` zusammen (§4–6).
- [ ] Pixel-Art? → `ImagePlugin::default_nearest()`. 3D-Flaechen? → `anisotropy_clamp: 16` (§7).
- [ ] Windows: finales **0.18.0+** (nicht rc) wegen DX12-Resize-Crash; im Zweifel Resize/Minimieren testen (§8).
- [ ] Wenige, geteilte Materials / Atlas — keine random Materials (§9).
- [ ] Immer `--release` bzw. `opt-level` setzen, BEVOR Performance gemessen wird (§10).
- [ ] Solari nur als optionales NVIDIA-Feature, nicht als verlaessliche Beleuchtung (§11).
- [ ] Android-Store-Build von Anfang an ueber `bevy_game_template` + xbuild-Fork planen (§12).
- [ ] wgpu-Version NICHT manuell ueberschreiben — Bevy pinnt sie (§13).
- [ ] Bei Versions-Update: Migration Guide lesen, BEVOR man Bugs vermutet (Ehrlichkeits-Hinweis).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-dotnet-directx-windows](3d-dotnet-directx-windows.md)
- [3d-godot](3d-godot.md)
- [3d-metal-scenekit-macos](3d-metal-scenekit-macos.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
