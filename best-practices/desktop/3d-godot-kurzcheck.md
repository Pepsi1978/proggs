# 3D mit Godot 4 (cross-platform) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Desktop-Projekt, beste Optik (Archviz/AAA) | **Forward+** Renderer waehlen. Einzige Methode mit VoxelGI, SDFGI, SSR, SSIL, SSAO, Volumetrik. | §1 |
| 2 | Android/Mobile-Ziel | **Mobile** Renderer (Vulkan via RenderingDevice). Kein SSR/SSIL/GI-Echtzeit — auf **LightmapGI** setzen. | §1, §3 |
| 3 | Web-Export oder uralte Hardware | **Compatibility** (GL/OpenGL ES 3) — Pflicht fuer Web. Shader NICHT portabel zu Forward+/Mobile. | §1 |
| 4 | Statische Szene, Top-Optik+Top-Performance | **LightmapGI** backen (GPU-Bake seit 4.0). UV2 wird reserviert. Beste Qualitaet/Leistung fuer Statik. | §3 |
| 5 | Modelle importieren | **glTF 2.0 / GLB** nutzen (nativer PBR-Workflow). Bei ORM-gepackter Textur auf **ORMMaterial3D** umstellen (3 Lookups statt mehr). | §5 |
| 6 | Schoenes Licht "out of the box" | WorldEnvironment + **AgX**-Tonemapping, Glow-Blend = **screen**, dezenter Bloom (~0.1), `tonemap_white` 6.0–8.0. | §4 |
| 7 | Sprache waehlen | Typed **GDScript** als Default (4.6 deutlich schneller, weniger Reibung). **C#** nur bei .NET-Team/Unity-Port. Beide koexistieren. | §7 |
| 8 | macOS ausliefern | App **signieren + notarisieren** (Apple verlangt das seit 2019). Env-Vars `GODOT_MACOS_CODESIGN_*`. Sonst startet die App beim Nutzer nicht. | §6 |
| 9 | 3D-Performance | Erst **Mesh-LOD** (groesster Hebel), dann ggf. Occlusion-Culling. Instanzen via **MultiMesh**, Transparenz minimieren. | §8 |
| 10 | Windows-Render-Glitches in 4.6 | D3D12 ist neuer Default — auf Intel-iGPUs (z.B. Jasper Lake) Black-Screens moeglich. Auf **Vulkan** zurueckschalten als Fallback. | §6, §9 |
