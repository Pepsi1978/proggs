# 3D mit Godot 4 Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Windows: schwarzer Bildschirm / Tearing auf Intel-iGPU (Jasper Lake, Celeron N4500) | D3D12 ist 4.6-Default → per `--rendering-driver vulkan` bzw. Projekteinstellung auf Vulkan zurueck | §1 |
| 2 | macOS: App startet beim Nutzer nicht ("beschaedigt"/Gatekeeper) | Nicht signiert+notarisiert. codesign/rcodesign + Notarisierung (Developer ID, PKCS#12) | §2 |
| 3 | macOS: eigene Editor-Binaries 4.6.1, .NET/GDExtension kaputt | Signatur-Bug der ersten 4.6.1-macOS-Uploads → neu hochgeladene 4.6.1 oder gleich **4.6.2** ziehen | §3 |
| 4 | LightmapGI-Bake schlaegt fehl / Mesh bleibt schwarz | UV2 fehlt oder schlecht. UV2-Auto-Unwrap **in Godot** beim Import aktivieren, NICHT in Blender unwrappen | §4 |
| 5 | Lightmap-Bake im Compatibility-Renderer bricht (Shader-Error `uv2_attrib_input`) | 4.6-Regression. Auf **4.6.2** updaten; sonst Forward+/Mobile zum Backen nutzen | §5 |
| 6 | Nach Upgrade 4.5→4.6: Sky/VoxelGI/SDFGI sehen kaputt/ueberbelichtet aus | Major-Rendering-Regression in fruehem 4.6. Auf **4.6.2** updaten und Optik neu pruefen | §6 |
| 7 | Materialien weiss/flach/grau nach Import (Blender→Godot) | glTF-Export verliert Metallic/Roughness-Maps. Principled-BSDF korrekt verdrahten, "Export Materials" aktiv, GLB statt FBX | §7 |
| 8 | Endlosschleife/Haenger beim .blend-Import | 4.6-Bug (Blender-Import-Versuche zaehlten nicht hoch). Fix in **4.6.2** (GH-116589) | §8 |
| 9 | Android: nach Wechsel gl_compatibility→mobile weniger Geraete unterstuetzt | Mobile-Renderer braucht Vulkan; alte Geraete fallen raus. Zielgeraete-Matrix VORHER pruefen | §9 |
| 10 | C#-Projekt soll ins Web | Geht nicht — C# unterstuetzt keinen Web-Export. GDScript fuer Web-Ziele | §10 |
| 11 | C#-Projekt nach Android | Experimentell seit 4.2, NativeAOT nur per Hand-Config. Fuer robustes Mobile GDScript | §10 |
| 12 | Compatibility-Renderer startet spuerbar langsam (~4 s) | Bekannt (#106310). Fuer schnellen Start Forward+/Mobile; Compatibility nur fuer Web/Low-End | §11 |
| 13 | VoxelGI/SDFGI flackern, v.a. mit TAA / in dunklen Bereichen | Bekannte GI-Instabilitaet (#62080, #74597). TAA-Wechselwirkung pruefen, ggf. statt SDFGI LightmapGI | §12 |
| 14 | Reflexionen (SSR/GI) falsch bei Kamera mit Frustum-Offset | VoxelGI/SDFGI/SSR rendern falsch wenn `frustum_offset != (0,0)` (#108508) | §12 |
