# 3D auf Windows (C#/.NET — DirectX/Stride/Silk.NET) — Best Practices (Stand 2026-06-13, Version .NET 10 / Stride 4.3 / C# 14)

> Researcher-Wissensbasis fuer Frank. Fokus: optisch sehr schoene 3D-Apps auf Windows mit C#/.NET — sowohl echtzeit/interaktiv als auch fotorealistische Produkt-/Szenenvisualisierung. Randbedingung: Auslieferung idealerweise als einzelne `.exe` ohne Endnutzer-Abhaengigkeiten.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Du willst eine komplette 3D-App/Spiel mit Editor, PBR, Postprocessing — schnell und "schoen" | **Stride 4.3** nehmen (Open-Source C#-Engine, .NET 10, DirectX 12 + Vulkan, PBR + Post-Effects eingebaut). Pragmatischster Weg zu "schoen". | §1, §2, §5 |
| 2 | Eingebettetes 3D-Viewport in einer WPF-Business-App (CAD/Viewer, MVVM) | **Helix Toolkit** — aber die SharpDX-Variante (`HelixToolkit.SharpDX.WPF`, DirectX 11), NICHT das langsame native `Viewport3D`. | §3 |
| 3 | Du willst die GPU roh ansprechen (DirectX 12), volle Kontrolle, eigener Renderer | **Silk.NET** (aktiv gepflegt, D3D12-Agility-SDK + Vulkan-Bindings, NativeAOT-faehig). Bevorzugt vor rohem C++-Interop. | §1, §4 |
| 4 | Du brauchst nur Windows-spezifische D3D12/D3D11/D2D-Bindings ohne Vulkan-Ballast | **Vortice.Windows** 3.8.x (.NET 9/10, schlanke Win32-DirectX-Bindings). | §1 |
| 5 | Veldrid als API-agnostische Abstraktion in Erwaegung | **Vermeiden fuer Neustarts** — Original seit 2023 ohne offiziellen Maintainer, kein D3D12-Backend. Nur via Community-Fork (TechPizzaDev). | §1 |
| 6 | 2D-lastiges Spiel mit etwas 3D, viel Kontrolle, kein Editor | **MonoGame 3.8.4** (stabil) — D3D12 erst ab 3.8.5-Preview (DesktopDX), noch nicht produktionsreif. | §1 |
| 7 | 3D-Modelle laden (Artist-Assets) | **glTF 2.0** als Austauschformat + **SharpGLTF** (1.0.6) zum Lesen/Schreiben. PBR-Materialien out of the box. | §4 |
| 8 | Eine einzelne `.exe` ohne .NET-Runtime ausliefern | `PublishSingleFile` + `SelfContained` + ggf. `PublishTrimmed`. **NativeAOT nur, wenn alle Libs AOT-kompatibel sind** — Engines mit Reflection (Stride) sind es oft nicht. | §6 |
| 9 | NativeAOT bricht zur Laufzeit (fehlende Typen, Reflection-Fehler) | Ursache: Trimming entfernt per-Reflection genutzten Code. Fix: Trimmer-Roots / `[DynamicDependency]` / Source-Generatoren. Silk.NET braucht `RegisterPlatform()`-Aufrufe. | §6, §9 |
| 10 | Fotorealismus (Produkt-/Szenen-Render, nicht zwingend 60 fps) | Stride-PBR + Post-Effects (Tonemapping/Bloom/DoF/AO) ODER eigener D3D12-Pathtracer via Silk.NET. Schoene Beleuchtung > Polygonzahl. | §2, §5 |

---

## §1 Stack-Wahl: Welche Technologie wann? [offiziell + extern]

**Quellen:** stride3d.net Features/Download (offiziell), Silk.NET NuGet 2.23.0 + GitHub Discussion #2500 (offiziell), Vortice.Windows CHANGELOG (offiziell), Veldrid GitHub mellinoe/TechPizzaDev (offiziell), MonoGame Roadmap + Blog (offiziell). Abruf: 2026-06-13.

Entscheidungsmatrix von "fertige Engine" bis "rohe GPU":

| Option | Abstraktionsgrad | DirectX 12? | Pflege-Status 2026 | "Schoen" out-of-box | Wann waehlen |
|--------|------------------|-------------|--------------------|--------------------|--------------|
| **Stride 4.3** | Volle Engine + Editor | Ja (D3D11/12 + Vulkan) | Aktiv (4.3 Nov 2025) | **Ja** — PBR, Post-FX, Light Probes | Komplette App/Spiel, schnell schoen werden |
| **Helix Toolkit (SharpDX)** | Scene-Graph fuer WPF/WinUI/Avalonia | Nein (D3D11 via SharpDX) | Aktiv (develop2, Vulkan-Engine experimentell) | Mittel (eigene Shader noetig) | 3D-Viewport in WPF-Business-App |
| **Silk.NET** | Niedrig (Bindings) | **Ja** (Agility SDK 1.618.5) | Sehr aktiv (Winter 2025) | Nein (alles selbst) | Eigener Renderer, max. Kontrolle, AOT |
| **Vortice.Windows** | Niedrig (Win32-Bindings) | **Ja** (3.8.x) | Aktiv (3.8.1, Dez 2025) | Nein | Nur-Windows-DirectX ohne Vulkan |
| **Veldrid** | Mittel (API-agnostisch) | **Nein** (D3D11/Vulkan/Metal/GL) | **Verwaist** seit 2023 (nur Fork) | Nein | Eher meiden bei Neustart |
| **MonoGame 3.8.4** | Framework (XNA-Stil) | Preview ab 3.8.5 | Aktiv (3.8.4 Juni 2025) | Eher 2D/Stilisiert | 2D + leichtes 3D, kein Editor |

**Kernaussagen:**
- **Stride** ist die einzige vollwertige, aktiv gepflegte Open-Source-**C#-Engine** mit Editor. Version 4.3 (14.11.2025) bringt **.NET 10, C# 14, Bepu-Physik, Vulkan-Compute-Shader, Rider/VSCode-Support** und ~40% weniger CPU-Frame-Prep bei draw-call-lastigen Szenen (4.2.1). Fuer "ich will schnell etwas Schoenes" ist Stride der pragmatischste Weg.
- **Silk.NET** dominiert zusammen mit Vortice die rohen DirectX-12/Vulkan-Bindings; beide werden gepflegt. Silk.NET deckt zusaetzlich OpenGL/CL/AL/XR/GLFW/SDL/Assimp ab und ist NativeAOT-faehig.
- **Veldrid** war beliebt als API-agnostische Mittelschicht, hat aber **seit Februar 2023 keinen offiziellen Maintainer** mehr und **kein D3D12-Backend** (nur D3D11/Vulkan/Metal/GL). Fuer neue Projekte daher riskant — wenn ueberhaupt, dann den TechPizzaDev-Fork pruefen.
- **MonoGame**: D3D12 ("DesktopDX") existiert erst im **3.8.5-Preview** (Dez 2025), produktionsreif voraussichtlich erst mit 3.9. Stabil ist 3.8.4 (D3D11). Eher fuer 2D/stilisierte 3D.

---

## §2 PBR & Materialien [offiziell + extern]

**Quellen:** stride3d.net Features (offiziell), khronos.org/gltf/pbr (offiziell), SharpGLTF NuGet/DeepWiki (offiziell). Abruf: 2026-06-13.

- **Standard-Workflow ist Metallic/Roughness-PBR** (glTF-2.0-Modell). Stride unterstuetzt das nativ: Material-System mit Tessellation, Displacement, Normal, Diffuse, Specular/Metalness, Transparent, Occlusion/Cavity, plus **multi-layered Materials**.
- **Texturkanaele** (Konvention): Base Color (sRGB), Metallic + Roughness (oft in einer Map gepackt: G=Roughness, B=Metallic), Normal (linear), Ambient Occlusion, Emissive. glTF packt Metallic/Roughness genau so.
- **glTF-Material-Extensions** (von SharpGLTF unterstuetzt, fuer "schoenere" Materialien): `KHR_materials_clearcoat` (Autolack), `KHR_materials_transmission` (Glas), `KHR_materials_sheen` (Stoff), `KHR_materials_specular`, `KHR_materials_unlit`. Diese Extensions sind der Schluessel zu fotorealistischen Produktrenders.
- **Eigener Renderer (Silk.NET/Vortice)**: PBR muss selbst implementiert werden — Cook-Torrance-BRDF, Image-Based-Lighting (IBL) mit vorberechneter Environment-Map (Irradiance + Pre-filtered Specular + BRDF-LUT). Aufwaendig, aber volle Kontrolle.
- **Farbraum-Falle:** Base-Color/Emissive in **sRGB**, alle anderen Maps (Normal/Roughness/Metallic/AO) **linear**. Falscher Farbraum = "ausgewaschene" oder "zu dunkle" Materialien (siehe §9).

---

## §3 WPF-3D & Helix Toolkit — Status [offiziell + extern]

**Quellen:** helix-toolkit GitHub CHANGELOG/Releases (offiziell), Microsoft Learn Media3D (offiziell), docs.helix-toolkit.org (offiziell). Abruf: 2026-06-13.

- **Natives WPF `Viewport3D` (System.Windows.Media.Media3D)** ist **nicht offiziell deprecated**, aber technisch veraltet: software-/D3D9-basiert, langsam, keine modernen Shader, kein echtes PBR. Fuer "schoen" ungeeignet — nur fuer simple Drahtgitter/Diagramme.
- **Helix Toolkit ist aktiv gepflegt** (MIT-Lizenz) und kommt in zwei Geschmacksrichtungen:
  - `HelixToolkit.WPF` — Komfort-Layer ueber dem nativen WPF-Media3D (erbt dessen Limits).
  - **`HelixToolkit.SharpDX.WPF`** — eigener 3D-Renderer auf **SharpDX (DirectX 11)**, XAML/MVVM-kompatible Scene-Graphs (`Viewport3DX`). **Das ist die Variante fuer Performance und bessere Optik.**
- Helix unterstuetzt zusaetzlich **Avalonia, WinUI und WinForms**. Im `develop2`-Branch gibt es eine **experimentelle Next-Gen-Engine** mit Multi-Backend (Fokus Vulkan).
- **Empfehlung:** Fuer ein 3D-Viewport in einer WPF-Business-App `HelixToolkit.SharpDX.WPF` nehmen. Fuer freistehende, optisch anspruchsvolle 3D-Apps lieber Stride und WPF/WinUI nur fuer die Drumherum-UI.

---

## §4 Asset-Pipeline (glTF) & rohe DirectX-12-Bindings [offiziell]

**Quellen:** khronos.org/gltf (offiziell), SharpGLTF NuGet 1.0.6 + GitHub vpenades (offiziell), Silk.NET.Direct3D12 NuGet 2.23.0 (offiziell), Vortice.Windows README/CHANGELOG (offiziell), Microsoft DirectX Developer Blog Agility SDK (offiziell). Abruf: 2026-06-13.

**Asset-Format:**
- **glTF 2.0 / GLB ist das Standard-Austauschformat** ("das JPEG der 3D-Welt"). Binaer (`.glb`) fuer Auslieferung, Text (`.gltf`) fuer Debugging.
- **SharpGLTF** (1.0.6, 100% .NET Standard): `SharpGLTF.Core` = vollstaendige Schema2-Repraesentation (Lesen/Schreiben, Low-Level-Zugriff), `SharpGLTF.Toolkit` = Komfort-Utilities zum Erzeugen/Manipulieren. Beispiel: `var m = SharpGLTF.Schema2.ModelRoot.Load("model.gltf"); m.SaveGLB("model.glb");`
- Stride hat einen eigenen Asset-Importer (FBX/glTF/OBJ) im Game Studio. Fuer eigene Renderer: SharpGLTF + ggf. Assimp (via Silk.NET.Assimp) fuer Exoten-Formate.

**DirectX-12-Stand 2025/2026 (Agility SDK):**
- Aktuell ist die **Agility SDK** der Weg, neue D3D12-Features unabhaengig vom Windows-Update zu bekommen (NuGet `Microsoft.Direct3D.D3D12`). Silk.NET ist im Winter-2025-Update auf **Agility SDK 1.618.5** und **Vulkan 1.4.336** gehoben.
- Neuere D3D12-Features: **Work Graphs 1.0** (GPU generiert/plant eigene Arbeit ohne CPU — Culling, Binning, Compute-Ketten), **DirectSR** (Super Resolution, integriert AMD FSR 3.1), **DirectStorage 1.4** (schnelles Asset-Streaming von NVMe direkt zur GPU), **DirectML** (GPU-ML). Day-One-Treibersupport von AMD und NVIDIA.
- Fuer "schoen" relevant: DirectSR/Upscaling spart Renderbudget fuer mehr Effekte; DirectStorage fuer grosse Texturen ohne Ruckler.

---

## §5 Beleuchtung & Postprocessing [offiziell]

**Quellen:** stride3d.net Features/Blog PBR-Scene-Editor (offiziell). Abruf: 2026-06-13.

- **Stride-Compositor** definiert praezise, wie Szenen gerendert werden, mit einer **Post-Effects-API** und vielen eingebauten Effekten: **Depth of Field, Bloom, Lens Flare, Glare, Tone Mapping, Vignetting, Film Grain, Antialiasing**. Das deckt 90% dessen ab, was eine Szene "teuer" aussehen laesst.
- **HDR + Tonemapping ist Pflicht fuer Realismus**: intern in linearem HDR rechnen, dann via Tonemapper (z.B. ACES/Reinhard) auf SDR/Display mappen. Bloom greift **vor** dem Tonemapping (auf HDR-Werten).
- **Beleuchtung:** Stride bietet **Light Probes** (vorberechnete indirekte Beleuchtung) — wichtig fuer realistisches GI ohne Echtzeit-Pathtracing. Plus Direktionallichter mit Schatten-Maps, Punkt-/Spotlichter.
- **Faustregel fuer "schoen":** Gute **HDR-Environment-Map (IBL)** + korrektes Tonemapping + dezenter Bloom + Ambient Occlusion + Anti-Aliasing schlagen jede Polygon-Schlacht. Beleuchtung und Postprocessing machen den optischen Unterschied, nicht die Geometrie-Dichte.
- **Eigener Renderer:** Diese Effekte als Fullscreen-Compute/Pixel-Shader-Passes selbst bauen (Bloom = Downsample-Kette + Blur + Composite; Tonemapping als finaler Pass). Reihenfolge: Geometrie (HDR) → AO → Bloom → Tonemapping → AA → UI.

---

## §6 Single-`.exe` / Deployment (AOT, Trimming, Self-Contained) [offiziell + extern]

**Quellen:** Microsoft Learn — Single-file overview, Native AOT overview + optimizing (offiziell), andrewlock.net .NET-10-Preview-Serie (extern), thedotnetblog.com / inedo.com (extern). Abruf: 2026-06-13.

Drei Stufen, von robust bis schlank:

1. **Self-Contained Single-File (empfohlener Default fuer Engines):**
   - `<SelfContained>true</SelfContained>`, `<PublishSingleFile>true</PublishSingleFile>`, `<RuntimeIdentifier>win-x64</RuntimeIdentifier>`.
   - Buendelt App + .NET-Runtime + Abhaengigkeiten in **eine `.exe`**, laeuft ohne installiertes .NET. Groesste Kompatibilitaet (volle Reflection/JIT). Groesse: ~60–90 MB.
   - **Funktioniert mit Stride/Helix/Veldrid** — diese nutzen Reflection und vertragen AOT oft nicht.

2. **+ Trimming (mittlere Groesse):**
   - Zusaetzlich `<PublishTrimmed>true</PublishTrimmed>`. Entfernt unerreichbaren Code, oft **60–80% kleiner**.
   - **Risiko:** per-Reflection genutzter Code wird faelschlich entfernt → Laufzeitfehler. Bei Engines mit dynamischem Laden gefaehrlich (Trimmer-Warnings ernst nehmen, Roots angeben).

3. **NativeAOT (schlank + schnellster Start, aber restriktiv):**
   - `<PublishAot>true</PublishAot>`. Kompiliert zu nativem Maschinencode, **kein JIT**, near-instant Start, kleiner Memory-Footprint. Einfache Apps: **5–15 MB**.
   - **NUR wenn der ganze Stack AOT-kompatibel ist.** Reflection-lastige Engines (Stride) sind es meist NICHT. **Silk.NET ist AOT-faehig**, braucht aber explizite `GlfwWindowing.RegisterPlatform();` / `GlfwInput.RegisterPlatform();` am Programmstart (in 3.0 soll das wegfallen).
   - Empfehlung: NativeAOT nur fuer **eigene Silk.NET/Vortice-Renderer**, nicht fuer Stride-Spiele.

**Praxis-Entscheid fuer Frank:** Stride-App → Self-Contained Single-File (ohne Trimming, sonst Tests noetig). Eigener Silk.NET-Renderer → NativeAOT moeglich fuer eine kleine, schnelle `.exe`.

---

## §7 Performance [offiziell + extern]

**Quellen:** Microsoft DirectX Developer Blog (offiziell), stride3d.net Blog 4.2.1 (offiziell). Abruf: 2026-06-13.

- **Draw-Call-Reduktion** ist der groesste Hebel: Instancing, Batching, Material-Sortierung. Stride 4.2.1 senkte CPU-Frame-Prep um ~40% bei draw-call-lastigen Szenen — zeigt, wie teuer Draw-Calls sind.
- **D3D12-spezifisch:** PSO-Caching (Pipeline-State-Objects vorab erzeugen, nicht pro Frame), Command-Lists parallel auf mehreren Threads aufzeichnen, Descriptor-Heaps wiederverwenden, Ringpuffer fuer Upload-Heaps.
- **DirectStorage 1.4** fuer Asset-Streaming (NVMe → GPU, entlastet CPU). **DirectSR/Upscaling** rendert intern in niedrigerer Aufloesung und skaliert hoch — spart Budget fuer Effekte.
- **.NET-Seite:** GC-Druck minimieren (Structs/`Span<T>`/Pooling im Hot-Path, kein LINQ/Allokationen pro Frame). NativeAOT eliminiert JIT-Warmup-Ruckler.
- **Multithreading:** Stride ist multithreaded; bei eigenem Renderer Render-Thread vom Logik-Thread trennen.

---

## §8 Echtzeit vs. fotorealistische Visualisierung [offiziell + extern]

**Quellen:** stride3d.net Features (offiziell), khronos.org/gltf/pbr (offiziell). Abruf: 2026-06-13.

- **Echtzeit/interaktiv (60+ fps):** Stride (mit Editor/Physik) oder eigener Silk.NET-D3D12-Renderer. Rasterisierung + Light Probes + Screen-Space-Effekte (SSAO, SSR). Optik via gutem PBR + Postprocessing.
- **Fotorealistische Produkt-/Szenen-Render (Qualitaet > fps):**
  - **Pragmatisch:** Stride mit hoher Qualitaetsstufe — IBL aus HDR-Environment, viele Lichter, alle Post-Effects an, hohe Aufloesung, ggf. mehrere Frames akkumulieren. Reicht fuer die meisten Produktbilder.
  - **Maximal:** eigener **Pathtracer** via D3D12 DXR (Raytracing) ueber Silk.NET/Vortice — physikalisch korrekte Reflexionen/GI/Schatten, dafuer Aufwand hoch.
  - **Schluessel** in beiden Faellen: korrektes lineares Lighting + HDR + Tonemapping + hochwertige Materialien (glTF-Extensions: Clearcoat, Transmission, Sheen). Siehe §2/§5.
- **Hybrid:** Echtzeit-Viewport zum Arrangieren, dann hochwertiger "Final Render"-Modus (mehr Samples/Aufloesung) fuer das Ausgabebild.

---

## §9 Haeufige Fallen (BUGS/Pitfalls) [offiziell + extern]

**Quellen:** Silk.NET GitHub Discussion #2500 (offiziell), Microsoft Learn Native AOT/Trimming (offiziell), Veldrid GitHub (offiziell), MonoGame Roadmap (offiziell), khronos.org/gltf (offiziell). Abruf: 2026-06-13.

| # | Symptom | Ursache | Fix | Versionen |
|---|---------|---------|-----|-----------|
| F1 | Materialien "ausgewaschen" oder zu dunkel | Falscher Farbraum: Maps als sRGB statt linear (oder umgekehrt) interpretiert | Base-Color/Emissive = sRGB-Texturformat; Normal/Roughness/Metallic/AO = linear. In linearem HDR rechnen, am Ende tonemappen | Alle (Stride, eigene Renderer) |
| F2 | App crasht zur Laufzeit nach `PublishTrimmed`/`PublishAot` ("Type not found", Reflection-Fehler) | Trimmer entfernt per-Reflection genutzten Code | Trimmer-Roots / `[DynamicDependency]` / `TrimmerRootAssembly`; Trimmer-Warnings ernst nehmen; bei Engines Trimming ganz weglassen | .NET 9/10 |
| F3 | Silk.NET-App startet nicht / kein Fenster unter NativeAOT | Plattform-Registrierung fehlt (kein Reflection-Autodiscover unter AOT) | `GlfwWindowing.RegisterPlatform();` + `GlfwInput.RegisterPlatform();` am Programmstart aufrufen | Silk.NET 2.x (entfaellt evtl. in 3.0) |
| F4 | Veldrid-Projekt: kein D3D12, Bugfixes versanden | Original-Veldrid seit Feb 2023 ohne offiziellen Maintainer; nur D3D11/Vulkan/Metal/GL | Fuer Neustart meiden; sonst TechPizzaDev-Fork pruefen oder auf Silk.NET/Stride wechseln | Veldrid (alle) |
| F5 | MonoGame-D3D12-Projekt instabil/unfertig | D3D12 ("DesktopDX") ist nur 3.8.5-**Preview**, nicht produktionsreif | Fuer Produktion 3.8.4 (D3D11) nutzen; auf 3.9 warten fuer stabiles D3D12 | MonoGame 3.8.4 / 3.8.5-preview |
| F6 | WPF-`Viewport3D` ruckelt / sieht flach aus | Natives Media3D ist software-/D3D9-basiert, kein PBR | Auf `HelixToolkit.SharpDX.WPF` (D3D11) wechseln oder Stride nutzen | WPF (alle) |
| F7 | Single-`.exe` riesig (80+ MB) | Self-Contained buendelt die ganze .NET-Runtime | Akzeptieren (robust), oder Trimming testen, oder NativeAOT (nur wenn Stack AOT-faehig) | .NET 9/10 |
| F8 | glTF-Modell laedt, aber Extensions (Clearcoat/Glas) fehlen optisch | Renderer/Engine unterstuetzt die KHR-Material-Extension nicht | SharpGLTF liest sie; Renderer muss sie auswerten — sonst Fallback auf Base-PBR; in Stride pruefen, welche Extensions die Version unterstuetzt | SharpGLTF 1.0.6 |
| F9 | NativeAOT bricht beim Build mit Trimmer-Warnings, die als Errors zaehlen | Reflection-/dynamische APIs nicht AOT-analysierbar | Source-Generatoren statt Reflection (z.B. JSON/Config/YamlDotNet-Generator); betroffene Libs ggf. ersetzen | .NET 8/9/10 |

---

## Quellen (Gesamtliste)

- Stride: [stride3d.net](https://www.stride3d.net/), [Features](https://www.stride3d.net/features/), [Download](https://www.stride3d.net/download/), [4.2.1 Phoronix](https://www.phoronix.com/news/Stride-4.2.1.2485) — `offiziell`/`extern`
- Silk.NET: [NuGet 2.23.0](https://www.nuget.org/packages/Silk.NET/), [Direct3D12 2.23.0](https://www.nuget.org/packages/Silk.NET.Direct3D12/), [AOT-Discussion #2500](https://github.com/dotnet/Silk.NET/discussions/2500) — `offiziell`
- Vortice.Windows: [GitHub](https://github.com/amerkoleci/Vortice.Windows), [CHANGELOG](https://github.com/amerkoleci/Vortice.Windows/blob/main/CHANGELOG.md) — `offiziell`
- Veldrid: [mellinoe/Veldrid](https://github.com/mellinoe/Veldrid), [TechPizzaDev-Fork](https://github.com/TechPizzaDev/veldrid) — `offiziell`
- MonoGame: [Roadmap](https://docs.monogame.net/roadmap/), [3.8.5-Preview Blog](https://monogame.net/blog/2025-12-19-385-preview/) — `offiziell`
- Helix Toolkit: [GitHub](https://github.com/helix-toolkit/helix-toolkit), [Docs](https://docs.helix-toolkit.org/en/latest/wpf/getting-started.html) — `offiziell`
- DirectX 12 / Agility SDK: [DirectX Developer Blog](https://devblogs.microsoft.com/directx/agility-sdk-1-613-0/), [Work Graphs GPUOpen](https://gpuopen.com/news/microsoft-work-graphs-1-0-now-available/) — `offiziell`
- glTF / SharpGLTF: [Khronos glTF PBR](https://www.khronos.org/gltf/pbr/), [SharpGLTF NuGet](https://www.nuget.org/packages/SharpGLTF.Core), [GitHub vpenades](https://github.com/vpenades/SharpGLTF) — `offiziell`
- Deployment: [Single-file overview](https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview), [Native AOT overview](https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/), [Optimizing AOT](https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/optimizing) — `offiziell`; [andrewlock.net .NET 10 Serie](https://andrewlock.net/exploring-dotnet-10-preview-features-7-packaging-self-contained-and-native-aot-dotnet-tools-for-nuget/) — `extern`

---

## Bezug ↔ Bug-Almanach

Die konkreten Fallen zu diesen Best Practices stehen im Bug-Almanach: `bugs/desktop/3d-dotnet-directx-windows.md`. Wer hier "wie macht man es richtig" liest, sollte dort "was geht schief" gegenlesen.

| Best-Practice-Abschnitt | Verwandte Bugs (→ bugs/desktop/3d-dotnet-directx-windows.md) |
|-------------------------|--------------------------------------------------------------|
| §1 Stack-Wahl | §4 Silk.NET AOT, §8 Veldrid verwaist, §9 MonoGame D3D12-Reife, §13 Stride Shader-Cache, §14 Vortice COM-Lifecycle |
| §2 PBR & Materialien | §1 Farbraum ausgewaschen/dunkel, §10 glTF KTX2, §11 glTF Material-Extensions |
| §3 WPF-3D & Helix Toolkit | §7 Helix Viewport3DX schwarz/flackert |
| §4 Asset-Pipeline (glTF) | §10 glTF KTX2 nicht geladen, §11 Material-Extensions fehlen |
| §5 Beleuchtung & Postprocessing | §1 Farbraum/HDR, §2 Flip-Model-Swapchain sRGB |
| §6 Single-.exe / Deployment | §3 Trimmer/AOT-Crash, §4 Silk.NET RegisterPlatform, §5 .NET-10 native-lib-search, §6 Single-file Temp-Extraktion |
| §7 Performance | §12 D3D12 DEVICE_REMOVED/Descriptor/TDR, §14 COM-Leak |
| §9 Haeufige Fallen (Tabelle) | Volltext + erweitert in den Bug-Almanach-Eintraegen §1-§14 |
