# 3D auf Windows (C#/.NET — DirectX/Stride/Silk.NET) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
