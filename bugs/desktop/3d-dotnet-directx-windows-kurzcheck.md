# 3D auf Windows (C#/.NET — DirectX/Stride/Silk.NET) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|---------------------|--------------|----------|
| 1 | Materialien wirken ausgewaschen oder zu dunkel | Texturen-Farbraum pruefen: BaseColor/Emissive = sRGB, Normal/Roughness/Metallic/AO = linear. In HDR rechnen, am Ende tonemappen. | §1 |
| 2 | Bild zu hell/flau, obwohl Texturen korrekt sind (eigener D3D12-Renderer) | **Flip-Model-Swapchain**: Swapchain-Format MUSS `R8G8B8A8_UNORM` sein (nicht `_SRGB`), nur das **RTV** als `_SRGB`-View anlegen. Sonst doppelte/fehlende Gamma. | §2 |
| 3 | App crasht nach `PublishTrimmed`/`PublishAot` ("Type not found", Reflection-Fehler) | Trimmer entfernt per-Reflection genutzten Code. Trimmer-Roots/`[DynamicDependency]`; bei Engines (Stride) Trimming/AOT ganz weglassen. | §3 |
| 4 | Silk.NET startet nicht / kein Fenster unter NativeAOT | `GlfwWindowing.RegisterPlatform();` + `GlfwInput.RegisterPlatform();` ganz am Programmstart aufrufen (kein Reflection-Autodiscover unter AOT). | §4 |
| 5 | Single-`.exe` findet native DLL nicht mehr (.NET 10), `DllNotFoundException` | **.NET-10-Breaking-Change**: App-Verzeichnis wird nicht mehr automatisch durchsucht. `DllImportSearchPath.AssemblyDirectory` setzen oder `NativeLibrary.Load` mit Pfad. | §5 |
| 6 | Single-`.exe` bricht sporadisch / Antivirus meldet sie | Self-Contained extrahiert native DLLs nach `%TEMP%\.net` — werden geloescht oder geblockt. Code-Signing + ggf. `IncludeNativeLibrariesForSelfExtract`. | §6 |
| 7 | WPF-`Viewport3DX` (Helix SharpDX) startet schwarz / flackert in ViewBox | EffectsManager-Init-Timing + WPF-AirSpace. Viewport NICHT in ViewBox; EffectsManager als Singleton; Init nach Loaded. | §7 |
| 8 | Veldrid-Projekt: kein D3D12, Bugfixes versanden | Original-Veldrid seit Feb 2023 ohne Maintainer, nur D3D11/Vulkan/Metal/GL. Fuer Neustart meiden → Silk.NET/Stride. | §8 |
| 9 | MonoGame 3.8.5-Preview: Content-Pipeline wirft Exception, Custom Importer kaputt | Regression in Preview.2: Content-Builder ruft obsolete Methode statt der uebergebenen Instanz. Fuer Produktion 3.8.4 (D3D11). | §9 |
| 10 | glTF mit KTX2/Basis-Texturen laedt nicht ("KHR_texture_basisu not supported") | Renderer/Engine muss `KHR_texture_basisu` transcodieren; SharpGLTF liest nur das Schema. Fallback: PNG/normale Texturen oder Transcoder einbauen. | §10 |
| 11 | glTF-Extensions (Clearcoat/Glas/Sheen) fehlen optisch trotz korrekter Datei | Renderer wertet die KHR-Material-Extension nicht aus. SharpGLTF liefert die Daten — Shader muss sie nutzen, sonst Fallback auf Base-PBR. | §11 |
| 12 | D3D12: `DEVICE_REMOVED` / `DXGI_ERROR_DEVICE_HUNG`, schwarzes Bild, Stillstand | Descriptor-Heap-Overflow, falscher Descriptor-Typ, oder Shader-Timeout (TDR). Debug-Layer + DRED aktivieren, Heap-Indizes pruefen. | §12 |
| 13 | Stride: "Unable to find compiled shaders" / Crash beim ersten Start | Shader-Cache in AppData korrupt oder GraphicsProfile/Feature-Level inkompatibel (z.B. Level 9_1). Cache loeschen, Profil hochsetzen. | §13 |
| 14 | Vortice/SharpDX: ComObject-Leak, `0x8007000E`/Hang beim Beenden | Nicht freigegebene COM-Objekte (kein `Dispose`/`using`); Debug-Layer meldet Live-Objekte beim Device-Release. Alle D3D-Objekte deterministisch disposen. | §14 |
