# 3D mit Rust (wgpu/Bevy) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
