# 3D mit Godot 4 (cross-platform) — Best Practices (Stand 2026-06-13, Version Godot 4.6)

> Aktuellste Version: **Godot 4.6** (ausgeliefert Ende Januar 2026). Es ist eine der groessten
> Releases seit 4.0 — mit komplett neu geschriebenen Screen-Space Reflections, optimiertem
> Forward+/Clustered-Renderer, AgX-Verbesserungen und (auf Windows) **Direct3D 12 als neuem
> Standard-Backend**. Quellen: [Godot 4.6 Release](https://godotengine.org/releases/4.6/) [offiziell],
> [GamingOnLinux 2026-01](https://www.gamingonlinux.com/2026/01/the-free-and-open-source-godot-engine-4-6-is-out-now-with-major-upgrades/) [extern].

Diese Datei deckt BEIDE Inhaltstypen ab: **echtzeit/interaktiv** (Spiele, Tools) UND
**fotorealistische Szenen** (Archviz, Produktrender, virtuelle Produktion).

---

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

---

## §1 Renderer-Wahl: Forward+ vs Mobile vs Compatibility

Quellen: [Renderers — Godot Docs (stable)](https://docs.godotengine.org/en/stable/tutorials/rendering/renderers.html) [offiziell],
[Renderer-Vergleich studyraid](https://app.studyraid.com/en/read/32761/1441833/selecting-a-renderer-forward-mobile-compatibility) [extern], abgerufen 2026-06-13.

Godot 4 hat drei Render-Methoden, umschaltbar **nur per Projekteinstellung** (kein Szenen-Umbau noetig):

- **Forward+** — fortschrittlichste, feature-reichste Methode. **Nur Desktop/Konsole.** Nutzt ein
  **Clustered Light Grid** (View-Frustum in 3D-Zellen, jede Zelle haelt Indexliste naher Lichter) →
  **hunderte Lichter gleichzeitig, kein Per-Objekt-Limit**. **Exklusiv hier**: VoxelGI, SDFGI, SSR,
  SSIL, volumetrischer Nebel. Hat einen **Depth-Prepass** (reduziert Shading-Kosten). Erste Wahl fuer
  alles Hochwertige auf dem Desktop. In 4.6 zusaetzlich optimiert (dynamische Cluster-Groesse,
  ~15% weniger Per-Frame-Overhead in typischen Szenen, [Godot 4.6 Notes](https://godotengine.org/releases/4.6/) [offiziell]).
- **Mobile** — weniger Features, rendert einfache Szenen schneller. **Default auf Mobile**, laeuft auch
  auf Desktop. Single-Pass-Lighting mit Licht-Limit pro Mesh. Auf Android laeuft Mobile ueber **Vulkan**
  (RenderingDevice-Abstraktion). Kein SSR, kein Echtzeit-GI.
- **Compatibility** (GL Compatibility, OpenGL ES 3 / WebGL 2) — am wenigsten Features, fuer Low-End-Desktop
  und **Pflicht fuer Web-Export**.

**Wichtige Stolpersteine:**
- **Shader sind NICHT portabel** zwischen den RenderingDevice-Renderern (Forward+, Mobile) und
  Compatibility. Wer Web UND Desktop bedient, muss Shader doppelt pflegen.
- **Startzeit**: Compatibility startet spuerbar langsamer (Issue [#106310](https://github.com/godotengine/godot/issues/106310) [offiziell]:
  ~4 s vs <0.5 s bei Forward+/Mobile in 4.4.1).
- **Renderer-Wechsel = Optikwechsel.** Eine Szene fuer Forward+ getuned sieht unter Mobile anders aus —
  bei Cross-Platform IMMER auf beiden Ziel-Renderern visuell prüfen.

**Praxis-Empfehlung fuer cross-platform schoene 3D-App:**
- Desktop (macOS/Windows): Forward+.
- Android: Mobile-Renderer + **gebackene** Beleuchtung (LightmapGI), keine Echtzeit-GI.
- Web (falls noetig): Compatibility, reduziertes Featureset.

---

## §2 PBR & Materialien

Quellen: [Godot 4 PBR Checklist supermatrix](https://supermatrix.studio/blog/how-to-create-realistic-pbr-materials-in-blender-for-godot-4) [extern],
[A23D PBR in Godot](https://www.a23d.co/docs/godot/import-pbr-textures) [extern], abgerufen 2026-06-13.

- Godot nutzt den **metallic/roughness**-PBR-Workflow (glTF-Standard), nicht specular/glossiness.
- **StandardMaterial3D** ist der Default-Shader fuer importierte Szenen. **ORMMaterial3D** verwenden,
  wenn die Quelle eine **ORM-gepackte** Textur liefert (Occlusion=R, Roughness=G, Metalness=B in einer
  Datei) → **3 statt mehr Texture-Lookups**, spuerbar performanter bei grossen Texturen/vielen Objekten
  ([Issue #70979](https://github.com/godotengine/godot/issues/70979) [offiziell]).
- **Data-Maps vs Color-Maps:** Roughness, Metalness, AO, Normal, Height, Opacity sind **Daten** — beim
  Import als **non-sRGB / linear** behandeln, NICHT farb-korrigieren. Nur Albedo/Emission sind sRGB.
- Bei separaten AO/Roughness/Metalness-PNGs ist StandardMaterial3D oft klarer (jede Map einzeln zuweisbar);
  bei einer gepackten ORM-Textur ist ORMMaterial3D die richtige Wahl.
- Fuer Fotorealismus: Albedo nicht zu dunkel/hell setzen (physikalisch plausible Albedo 0.04–0.9),
  Metall = Albedo wird zur Reflexionsfarbe, Nicht-Metall = Albedo ist diffuse Farbe.

---

## §3 Beleuchtung & Global Illumination (LightmapGI, VoxelGI, SDFGI)

Quellen: [Using Lightmap GI — Godot Docs](https://docs.godotengine.org/en/stable/tutorials/3d/global_illumination/using_lightmap_gi.html) [offiziell],
[SDFGI — Godot Docs](https://docs.godotengine.org/en/stable/tutorials/3d/global_illumination/using_sdfgi.html) [offiziell],
[GI-Systeme DeepWiki](https://deepwiki.com/godotengine/godot-docs/3.3-global-illumination-systems) [extern], abgerufen 2026-06-13.

Drei GI-Systeme mit unterschiedlichen Trade-offs (Qualitaet / Performance / Flexibilitaet):

| System | Art | Qualitaet | Performance | Szenen-Typ | Renderer |
|--------|-----|-----------|-------------|-----------|----------|
| **LightmapGI** | vorgebacken (statisch) | hoechste (gebacken) | beste (laufzeit billig) | mostly static | alle Backends |
| **VoxelGI** | echtzeit, dynamisch | mittel (Approximation, weich) | mittel-teuer | klein/mittel, Innenraeume | nur Forward+ |
| **SDFGI** | echtzeit, dynamisch | mittel (Approximation) | sehr teuer | grosse/Open-World | nur Forward+ |

**Best Practices:**
- **Statische Szene → LightmapGI.** Beste Qualitaet UND beste Laufzeit-Performance gleichzeitig.
  Seit Godot 4.0 wird auf der **GPU** gebacken (deutlich schneller mit Mid-/High-End-GPU). Bake dauert
  aber Minuten (VoxelGI nur Sekunden).
- **Wichtiger Stolperstein LightmapGI:** Der Bake **reserviert den UV2-Slot** des Materials — UV2 ist
  danach nicht mehr frei nutzbar. UV2-Lightmap-Unwrap vor dem Bake sicherstellen (Auto-Unwrap beim Import
  aktivierbar).
- **Dynamische Innenraeume (Raeume, Korridore) → VoxelGI.** Voxelisiert einen begrenzten Bereich,
  Bake in Sekunden, reagiert auf bewegte Lichter.
- **Grosse/Open-World mit dynamischem Licht → SDFGI.** Kein Bake noetig, skaliert mit der Welt — aber
  **eine der teuersten** Techniken in Godot. Auf schwacher Hardware/Mobile meiden.
- **Mobile/Android:** Echtzeit-GI (VoxelGI/SDFGI) ist NICHT verfuegbar → **LightmapGI** ist hier
  praktisch alternativlos fuer indirektes Licht.
- ReflectionProbes ergaenzen GI fuer spiegelnde Oberflaechen; in 4.6 nutzen Reflection-/Radiance-Probes
  **oktaedrische Maps statt Cube-Maps** → weniger GPU-/Speicherverbrauch, zuverlaessiger ueber viele
  Hardware ([Godot 4.6 Notes](https://godotengine.org/releases/4.6/) [offiziell]).

---

## §4 Postprocessing & Environment (Glow/Bloom, SSAO, SSR, SSIL, Tonemapping)

Quellen: [Godot 4.6 Release](https://godotengine.org/releases/4.6/) [offiziell],
[Environment & Light for Photorealistic 3D — hexaquo](https://hexaquo.at/pages/environment-and-light-in-godot-setting-up-for-photorealistic-3d-graphics/) [extern],
[5 Features for CG Artists in 4.6 — CG Channel](https://www.cgchannel.com/2026/01/discover-5-key-features-for-cg-artists-in-godot-4-6/) [extern], abgerufen 2026-06-13.

Alles laeuft ueber **WorldEnvironment** (eine Environment-Ressource pro Szene) und ggf. CameraAttributes.

**Tonemapping (entscheidet ueber den "Look"):**
- **AgX** ist der moderne, empfohlene Tonemapper fuer fotorealistische/filmische Bilder. In 4.6 exponiert
  AgX zusaetzlich **`agx_white`** und **`agx_contrast`** → Kontrolle ueber Helligkeit/Kontrast und
  konsistenter Farbton auf hellen Farben selbst bei hohem Kontrast.
- Fuer Fotorealismus: **`tonemap_white` 6.0–8.0** — hoehere Werte = weniger ausgebrannte Highlights,
  aber niedrigerer Kontrast.

**Glow / Bloom:**
- Glow wird **vor** dem Tonemapping geblendet; **Screen** ist der neue Default-Blend-Mode.
- Default-Blend "softlight" ist sehr dezent. Fuer sichtbares Light-Bleeding: **Blend = screen, Bloom ~0.1**
  → Szene wirkt heller/waermer.

**Screen-Space-Effekte (Mid-/Post-Processing):**
- **SSAO** (Ambient Occlusion) und **SSIL** (Indirect Lighting) — fuegen Kontaktschatten / indirektes
  Bounce-Licht hinzu.
- **SSR** (Screen Space Reflections) — **nur Forward+**, nicht Mobile/Compatibility. In **4.6 komplett neu
  geschrieben** mit **Hi-Z-Tracing**: weniger temporale Instabilitaet, weniger Artefakte bei flachen
  (grazing) Winkeln → sauberere Reflexionen fuer High-End-Echtzeit und virtuelle Produktion.
- **Material-Debanding** in 4.6 verbessert → weniger Banding in Verlaufsflaechen (Himmelskuppeln,
  volumetrisches Licht).

**Reihenfolge fuer "schoen out of the box":** WorldEnvironment anlegen → AgX → Glow(screen, bloom~0.1)
→ SSAO an → (Forward+) SSR/SSIL nach Bedarf → DirectionalLight3D mit weichen Schatten.

---

## §5 glTF-Import-Pipeline

Quellen: [3D-Dateiformate fuer Godot — homestyler](https://www.homestyler.com/article/the-ultimate-guide-to-d-file-formats-for-godot) [extern],
[Blender→Godot Material-Fix — gaminei](https://gamineai.com/help/blender-4-2-glb-export-loses-materials-godot-4-metallic-roughness-import-fix) [extern], abgerufen 2026-06-13.

- **GLB/glTF 2.0 ist das beste Format fuer Godot 4** — nativer PBR-Workflow, Animationen, Szenen-Hierarchie
  und Texturen in einer Datei, kompakt. **GLB (binär) bevorzugen** (alles gebündelt).
- **Vor dem Import pruefen:** Skalierung (glTF haelt i.d.R. reale Maße), Pivot/Orientierung, Texturkompatibilitaet.
  OBJ braucht oft manuelle Nacharbeit; FBX ist umstaendlicher.
- **Import-Setup:** Importierte glTF-Szenen nutzen automatisch StandardMaterial3D. Bei ORM-gepackten
  Texturen das Material auf **ORMMaterial3D** umstellen (Performance, §2).
- **Blender→Godot-Falle:** Beim glTF/GLB-Export aus Blender (4.2) koennen Metallic/Roughness-Maps verloren
  gehen, wenn der Material-Output nicht sauber am Principled-BSDF haengt → Materialien erscheinen flach/weiss
  in Godot. Fix: korrektes Principled-BSDF-Setup, Maps direkt verdrahten, beim Export "Export Materials"
  auf "Export" und Image-Format passend setzen.
- **Re-Import-Disziplin:** Modelle als Szene importieren, lokale Aenderungen ueber eine **vererbte Szene**
  (Inherited Scene) statt direkt in der Importdatei machen — sonst gehen Anpassungen beim Re-Import verloren.

---

## §6 Export: macOS, Windows, Android (Signierung & Stolpersteine)

Quellen: [Export Architecture DeepWiki](https://deepwiki.com/godotengine/godot-docs/8.1-export-architecture) [extern],
[Android: Configure projects — Android Developers](https://developer.android.com/games/engines/godot/godot-configure) [offiziell, Google],
[Complete Guide to Signing Godot Games for macOS — oreateai](https://www.oreateai.com/blog/complete-guide-to-signing-godot-games-for-macos/7bc9a68155b01df177139eaad07e9ac0) [extern], abgerufen 2026-06-13.

### macOS
- **Signieren + Notarisieren ist Pflicht.** Apple verlangt seit 2019, dass macOS-Software signiert und
  notarisiert ist — sonst blockiert Gatekeeper den Start beim Endnutzer ("App ist beschaedigt").
- Zwei Signier-Tools: Xcodes **`codesign`** (nur auf macOS) ODER **`rcodesign`** (PyOxidizer, plattformuebergreifend,
  erlaubt Signieren auch von Linux/Windows aus).
- Zertifikat = **Apple Developer ID** im **PKCS#12**-Format, gesetzt ueber Env-Vars
  `GODOT_MACOS_CODESIGN_CERTIFICATE_FILE` / `GODOT_MACOS_CODESIGN_CERTIFICATE_PASSWORD`.
- Notarisierung: entweder Apple-ID-Credentials oder **App Store Connect API Key**.
- Liefert eine `.app` bzw. `.dmg`/`.zip`. Universal-Binary (arm64 + x86_64) fuer breite Mac-Abdeckung.

### Windows
- Export liefert eine `.exe`. **Code-Signing optional**, aber ohne Signatur warnt SmartScreen
  ("Unbekannter Herausgeber"). Fuer Vertrieb empfohlen: Authenticode-Zertifikat (signtool / osslsigncode).
- **4.6-Falle:** Auf Windows ist **D3D12 jetzt Default**-Backend (Vulkan-Treiber auf Windows gelten als
  schlechter gepflegt). Auf manchen Intel-iGPUs (z.B. Jasper Lake / Celeron N4500) verursacht D3D12 in 4.6
  Black-Screens/Tearing ([Issue #116919](https://github.com/godotengine/godot/issues/116919) [offiziell]).
  Fallback: per `--rendering-driver vulkan` bzw. Projekteinstellung auf Vulkan zurueck.

### Android
- Voraussetzungen: **Android-SDK, JDK, Debug-Keystore** einrichten (Editor-Settings → Export → Android).
  Fuer Release einen eigenen **Release-Keystore** erzeugen und ueber `GODOT_ANDROID_KEYSTORE_RELEASE_*`
  konfigurieren. Ohne Keystore kein installierbares Release-APK/AAB.
- **AAB (Android App Bundle)** fuer den Play Store, APK fuer direktes Sideloading.
- Renderer auf Android: **Mobile** (Vulkan). Wechsel von `gl_compatibility` zu `mobile` **reduziert die
  unterstuetzten Geraete** (aeltere Geraete ohne ausreichenden Vulkan-Support fallen raus,
  [Issue #111729](https://github.com/godotengine/godot/issues/111729) [offiziell]) — Zielgeraete-Matrix vorher pruefen.
- **C# auf Android = experimentell** (siehe §7). Benoetigt .NET 7+/NativeAOT, nur arm64/x64.

---

## §7 GDScript vs C#

Quellen: [GDScript vs C# in Godot 2026 — StraySpark](https://www.strayspark.studio/blog/gdscript-vs-csharp-godot-2026-choosing-scripting-language) [extern],
[GDScript vs C# — Chickensoft](https://chickensoft.games/blog/gdscript-vs-csharp) [extern],
[C#-Plattform-Status 4.2 — Godot Blog](https://godotengine.org/article/platform-state-in-csharp-for-godot-4-2/) [offiziell], abgerufen 2026-06-13.

- **Beide sind in 4.6 produktionsreif** und koennen kommerzielle Spiele shippen. Beide koennen im selben
  Projekt **koexistieren** (gegenseitige Methodenaufrufe, Signals).
- **Performance 2026:** GDScript in 4.6 ist schneller als 4.5 (Bytecode-Optimierungen, weniger
  Method-Call-Overhead). Gewinne am groessten bei **typed GDScript** (statische Typannotationen → VM
  ueberspringt Laufzeit-Typchecks via "typed instructions"). Der Abstand zu C# hat sich deutlich verringert.
- **Default-Empfehlung: typed GDScript.** Schnelleres Prototyping, mehr Antworten/Beispiele in der Community,
  schnelleres Shippen. Erste Wahl fuer Solo, Game Jam, 2D, Web.
- **C# waehlen bei:** vorhandenem C#/.NET-Code (Unity-Port), grossem C#-Team (5+), Abhaengigkeit vom
  .NET-Oekosystem.
- **Plattform-Falle C#:**
  - **Web-Export wird von C# NICHT unterstuetzt** (Compatibility-Renderer + WASM-Mono fehlt). Wer Web
    braucht → GDScript.
  - **Android/iOS C#-Export ist seit 4.2 experimentell** (NativeAOT, .NET 7/8, nur arm64/x64) und noch
    nicht voll stabil. Fuer robuste Mobile-Auslieferung ist GDScript der sichere Weg.
- **Tipp fuer "schoenes 3D":** Renderer/Optik sind sprachunabhaengig (alles ueber Nodes/Ressourcen) — die
  Sprachwahl betrifft v.a. Gameplay-Logik, nicht die Bildqualitaet.

---

## §8 Performance (3D)

Quellen: [Optimizing 3D performance — Godot Docs](https://docs.godotengine.org/en/stable/tutorials/performance/optimizing_3d_performance.html) [offiziell],
[Occlusion Culling — Godot Docs](https://docs.godotengine.org/en/stable/tutorials/3d/occlusion_culling.html) [offiziell],
[Optimizing 3D on Arm GPUs — ARM Developer](https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/optimizing-3d-scenes-in-godot-on-arm-gpus) [extern], abgerufen 2026-06-13.

- **Mesh-LOD zuerst** (groesster Hebel): automatisch beim Import generierbar ODER manuell ueber
  **Visibility Ranges**. In den meisten Szenen mehr Performance-Gewinn als Occlusion-Culling.
- **Occlusion-Culling** (gebackener OccluderInstance3D): nuetzlich in **Draw-Call-gebundenen** Szenen,
  v.a. Innenraeume **ohne** schattenwerfendes DirectionalLight3D. Achtung: Forward+ hat schon einen
  **Depth-Prepass**, daher bringt Culling im Freien oft wenig und kann auf der CPU sogar kosten.
- **Draw Calls reduzieren:** Meshes zusammenfassen (Batching, ArrayMesh) und identische Instanzen ueber
  **MultiMesh** (ein Draw Call fuer tausende Instanzen — Gras, Steine, Crowd).
- **Material/Shader-Sortierung:** Godot sortiert nach Material/Shader. **Transparente Objekte** koennen
  nicht so sortiert werden (Back-to-Front) → **so wenig Transparenz wie moeglich**.
- **Mobile/Arm:** Aufloesung der GI-/Schatten-Maps reduzieren, gebackene Beleuchtung bevorzugen,
  Overdraw minimieren, kleine Textur-Atlanten — Tile-Based-GPUs reagieren empfindlich auf Bandbreite.

---

## §9 Haeufige Fallen (Schnellreferenz)

| Falle | Symptom | Ursache | Fix |
|-------|---------|---------|-----|
| Shader nicht portabel | Shader funktioniert auf Desktop, nicht im Web | Forward+/Mobile (RenderingDevice) vs Compatibility nutzen verschiedene Shader-Pfade | Shader fuer Compatibility separat schreiben/anpassen; Optik auf Ziel-Renderer testen |
| D3D12 Black-Screen (4.6) | Schwarzer Bildschirm/Tearing auf Windows-iGPU | D3D12 ist neuer Default, Treiberprobleme auf manchen Intel-GPUs (Jasper Lake) | Rendering-Driver auf **vulkan** zurueck ([#116919](https://github.com/godotengine/godot/issues/116919)) |
| macOS startet nicht beim Nutzer | "App ist beschaedigt"/Gatekeeper blockt | Nicht signiert+notarisiert | codesign/rcodesign + Notarisierung (Developer ID, PKCS#12) |
| LightmapGI belegt UV2 | UV2 nicht mehr nutzbar / Bake schlaegt fehl | Bake reserviert UV2-Slot, Unwrap fehlt | UV2-Unwrap beim Import aktivieren; UV2 nicht anderweitig verplanen |
| Materialien weiss/flach nach Import | Metallic/Roughness verschwunden (Blender→Godot) | glTF-Export-Setup verliert Maps | Principled-BSDF korrekt verdrahten, "Export Materials" aktiv |
| C# laeuft nicht auf Web | Web-Export bricht/leer | C# unterstuetzt keinen Web-Export | GDScript fuer Web-Ziele nutzen |
| C# instabil auf Android | Export-/Laufzeitfehler Mobile | Android-C#-Export seit 4.2 experimentell (NativeAOT) | Fuer robustes Mobile GDScript; sonst .NET 7+/arm64 strikt einhalten |
| Renderer-Wechsel zerstoert Optik | Szene sieht unter Mobile anders aus als Forward+ | Andere Feature-Sets (kein SSR/GI in Mobile) | Pro Ziel-Renderer separat tunen + visuell pruefen |
| Mobile reduziert Geraete | Weniger unterstuetzte Android-Geraete | Wechsel gl_compatibility→mobile braucht Vulkan ([#111729](https://github.com/godotengine/godot/issues/111729)) | Zielgeraete-Matrix pruefen; ggf. Compatibility fuer breite Abdeckung |
| Transparenz frisst FPS | Plötzliche FPS-Drops bei viel Glas/Partikeln | Transparente Objekte umgehen Material-Sortierung, Overdraw | Transparenz minimieren, Alpha-Scissor statt Alpha-Blend wo moeglich |
| Occlusion-Culling langsamer | FPS sinkt nach Aktivierung | Forward+ Depth-Prepass macht Culling teils ueberfluessig; CPU-Last | Outdoor/Forward+ Culling sparsam; erst LOD ausreizen |

---

### Verifikations-Hinweis
Die ausfuehrlichen offiziellen Doku-Seiten (renderers.html, releases/4.6) sind sehr gross; die hier
zitierten Aussagen stammen aus den Such-Zusammenfassungen dieser offiziellen Quellen sowie den verlinkten
GitHub-Issues. Vor kritischen Produktionsentscheidungen die jeweils verlinkten offiziellen Seiten direkt oeffnen.

---

## Bezug ↔ Bug-Almanach

Die konkreten bekannten Bugs/Fallen (mit Symptom, Ursache, Fix-Status und Quellen) stehen in
`bugs/desktop/3d-godot.md`. Querverweise:

| Best-Practice (hier) | Bug-Almanach (→ bugs/desktop/3d-godot.md §N) |
|----------------------|----------------------------------------------|
| §1 Renderer-Wahl | §9 Mobile reduziert Android-Geraete, §11 Compatibility-Startzeit |
| §2 PBR & Materialien | §7 Materialien weiss/flach nach Blender-Import |
| §3 Beleuchtung & GI | §4 LightmapGI UV2 fehlt, §5 Compatibility-Lightmap-Bake kaputt, §6 Sky/GI-Regression, §12 VoxelGI/SDFGI-Flackern |
| §4 Postprocessing (SSR) | §6 Sky/GI-Regression, §12 SSR/GI Frustum-Offset |
| §5 glTF-Import | §7 Materialien weiss, §8 .blend-Import-Endlosschleife |
| §6 Export macOS/Windows/Android | §1 D3D12 Black-Screen, §2 macOS-Signatur, §3 4.6.1-Editor-Binary, §9 Android-Geraete |
| §7 GDScript vs C# | §10 C# kein Web / Android experimentell |
| §8 Performance | §12 GI-Flackern (TAA-Wechselwirkung) |
| §9 Haeufige Fallen | deckt sich mit Almanach-Kurzcheck (Stufe A) |

**Empfohlene Basis fuer alle obigen Best Practices: Godot >= 4.6.2** (1. April 2026, 122 Fixes) —
behebt u.a. die .blend-Import-Endlosschleife (§8) und mehrere Rendering-Regressionen.
