# 3D mit Rust (wgpu/Bevy, cross-platform) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
