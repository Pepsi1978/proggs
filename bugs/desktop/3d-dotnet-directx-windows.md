# Bekannte Bugs: 3D auf Windows (C#/.NET — DirectX/Stride/Silk.NET)

> **PFLICHT-LESEN vor jeder 3D-Arbeit auf Windows mit C#/.NET.** Stand recherchiert 2026-06-13 fuer .NET 10 / C# 14 / Stride 4.3 / Silk.NET (Agility SDK 1.618.5) / Vortice 3.8.1 / MonoGame 3.8.4 (D3D11) bzw. 3.8.5-Preview.2 (D3D12) / Helix Toolkit. Die Gegenseite — wie man es von Anfang an richtig macht — steht in `best-practices/desktop/3d-dotnet-directx-windows.md`. Dieser Almanach sammelt die konkreten Fallen, die optisch schoene 3D-Apps reihenweise versenken: falscher Farbraum, Trimming/AOT-Crashs, Plattform-Registrierung, Swapchain-Format, Deployment-Fallstricke.

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

---

## 1. Materialien ausgewaschen oder zu dunkel — Farbraum-Falle [⭐ HÄUFIG]

**Symptom:** PBR-Materialien sehen flau/ausgewaschen oder unnatuerlich dunkel aus; Metalle wirken matt, Beleuchtung "falsch" obwohl Lichter und Geometrie stimmen.

**Ursache:** Falscher Farbraum pro Texturkanal. Base-Color- und Emissive-Maps liegen in **sRGB** vor (vom Maler so gespeichert), Daten-Maps (Normal, Roughness, Metallic, AO) muessen dagegen **linear** interpretiert werden. Wer alles als sRGB (oder alles als linear) laedt, bekommt doppelte oder fehlende Gamma-Korrektur. Zusaetzlich muss die Beleuchtungsrechnung in linearem HDR erfolgen und erst am Ende per Tonemapper auf das Display gemappt werden.

**Versionen:** Alle (Stride 4.3, Helix, eigene Silk.NET/Vortice-Renderer).

**FIX (funktionserhaltend):** Texturformate trennen — Base-Color/Emissive als sRGB-Format laden (z.B. `R8G8B8A8_UNORM_SRGB`), Normal/Roughness/Metallic/AO als lineares `_UNORM`. Komplette Lichtrechnung in linearem HDR (`R16G16B16A16_FLOAT`-Rendertarget), dann Tonemapping (ACES/Reinhard) als finaler Pass. In Stride: Texture-Asset auf "Color"/"Grayscale"-sRGB-Flag korrekt setzen.

**Quelle:** [Khronos glTF PBR](https://www.khronos.org/gltf/pbr/), [Walbourn — Care and Feeding of Modern Swap Chains pt.1](https://walbourn.github.io/care-and-feeding-of-modern-swapchains/)

## 2. Eigener D3D12-Renderer: Bild zu hell/flau — Flip-Model-Swapchain-sRGB [⭐ HÄUFIG]

**Symptom:** Selbst wenn die Texturen korrekt im richtigen Farbraum vorliegen (siehe §1), ist das Endbild zu hell, kontrastarm oder das Tonemapping "stimmt nicht". Tritt bei eigenen D3D12-Renderern auf.

**Ursache:** Flip-Model-Swapchains (`DXGI_SWAP_EFFECT_FLIP_DISCARD`/`FLIP_SEQUENTIAL`) unterstuetzen **kein** `_SRGB`-Swapchain-Format. Erlaubt sind nur `R16G16B16A16_FLOAT`, `B8G8R8A8_UNORM`, `R8G8B8A8_UNORM`, `R10G10B10A2_UNORM`. Wer trotzdem versucht, ein `_SRGB`-Format als Swapchain-Format zu setzen, bekommt einen Fehler — oder, schlimmer, baut die Gamma-Korrektur an der falschen Stelle ein und erhaelt doppelte/fehlende sRGB-Konvertierung.

**Versionen:** D3D12 (alle), Silk.NET, Vortice.Windows 3.8.x.

**FIX (funktionserhaltend):** Swapchain mit `R8G8B8A8_UNORM` (oder `B8G8R8A8_UNORM`) anlegen, aber den **Render-Target-View (RTV) auf dasselbe Backbuffer-Resource als `_SRGB`-Format** erzeugen (z.B. `R8G8B8A8_UNORM_SRGB`). Damit schreibt die Pipeline gamma-korrekt, ohne dass das Swapchain-Format die Flip-Model-Regel verletzt. Color-Space per `IDXGISwapChain3::SetColorSpace1(DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709)` setzen. Bei MSAA auf sRGB-Resolve achten.

**Quelle:** [Walbourn — Care and Feeding of Modern Swap Chains pt.2](https://walbourn.github.io/care-and-feeding-of-modern-swap-chains-2/), [GameDev.net — D3D12 sRGB Swap Chain](https://www.gamedev.net/forums/topic/670546-d3d12srgb-buffer-format-for-swap-chain/5243987/)

## 3. Crash nach PublishTrimmed/PublishAot — Trimmer entfernt Reflection-Code [⭐ HÄUFIG]

**Symptom:** Die App laeuft im Debug/normalen Publish einwandfrei, crasht aber nach `PublishTrimmed` oder `PublishAot` zur Laufzeit mit "Type not found", `MissingMethodException` oder Reflection-Fehlern.

**Ursache:** Der Trimmer entfernt Code, der nur per Reflection (dynamisch) erreicht wird, weil er ihn statisch nicht als erreichbar erkennt. Engines wie Stride laden Komponenten/Serializer per Reflection — genau das fliegt raus.

**Versionen:** .NET 9 / .NET 10.

**FIX (funktionserhaltend):** Trimmer-Warnings als Errors behandeln und ernst nehmen. Benoetigte Typen per `[DynamicDependency]` oder `TrimmerRootAssembly`/`TrimmerRootDescriptor` als Roots markieren. Wo moeglich Source-Generatoren statt Reflection (JSON, Config, YamlDotNet-Generator). **Bei reflection-lastigen Engines (Stride) Trimming UND NativeAOT ganz weglassen** — dort nur Self-Contained Single-File ohne Trim.

**Quelle:** [Microsoft Learn — Trimming options](https://learn.microsoft.com/en-us/dotnet/core/deploying/trimming/trimming-options), [Native AOT overview](https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/)

## 4. Silk.NET startet nicht unter NativeAOT — fehlende Plattform-Registrierung [⭐ HÄUFIG]

**Symptom:** Silk.NET-App laeuft als normaler Build, aber unter NativeAOT oeffnet sich kein Fenster / es kommt eine "platform not found"-Exception.

**Ursache:** Silk.NET ermittelt das Windowing-/Input-Backend (GLFW) normalerweise per Reflection-Autodiscover. Unter NativeAOT gibt es kein dynamisches Assembly-Scanning, also wird kein Backend gefunden.

**Versionen:** Silk.NET 2.x (laut Maintainer-Aussage soll das in 3.0 wegfallen).

**FIX (funktionserhaltend):** Ganz am Programmstart, vor dem Fenster-Erzeugen, explizit registrieren:
```csharp
GlfwWindowing.RegisterPlatform();
GlfwInput.RegisterPlatform();
```
Beide Aufrufe noetig (Windowing UND Input). Ggf. `Microsoft.Extensions.DependencyModel` aktualisieren, wenn native Libs unter AOT nicht gefunden werden.

**Quelle:** [Silk.NET Discussion #2500 (NativeAOT-Support)](https://github.com/dotnet/Silk.NET/discussions/2500), [Issue #960](https://github.com/dotnet/Silk.NET/issues/960)

## 5. Single-.exe (.NET 10) findet native DLL nicht mehr — Breaking Change [⭐ HÄUFIG]

**Symptom:** Eine Single-File-App, die unter .NET 8/9 lief, wirft nach Umstieg auf .NET 10 eine `DllNotFoundException` fuer eine native Bibliothek, die neben der `.exe` liegt.

**Ursache:** **Breaking Change in .NET 10** (eingefuehrt .NET 10). Frueher wurde das Verzeichnis der Single-File-`.exe` automatisch zu `NATIVE_DLL_SEARCH_DIRECTORIES` hinzugefuegt; bei NativeAOT war der `rpath` aufs App-Verzeichnis gesetzt. Beides wurde entfernt. Das App-Verzeichnis wird jetzt nur noch durchsucht, wenn die Such-Flags die Assembly-Directory einschliessen.

**Versionen:** .NET 10 (Migration von .NET 8/9).

**FIX (funktionserhaltend):** Bei P/Invokes, die das App-Verzeichnis brauchen, explizit `[DefaultDllImportSearchPaths(DllImportSearchPath.AssemblyDirectory)]` setzen (fuer Single-File-Apps bedeutet Assembly-Directory = App-Verzeichnis). Alternativ `NativeLibrary.Load` mit explizitem Pfad. Bei NativeAOT den `rpath` per Linker-Argument wiederherstellen. Default ohne Flags sucht weiterhin zuerst neben der App — Probleme treten nur auf, wenn Flags ohne AssemblyDirectory gesetzt sind.

**Quelle:** [Microsoft Learn — Single-file apps no longer look for native libraries in executable directory](https://learn.microsoft.com/en-us/dotnet/core/compatibility/interop/10.0/native-library-search), [dotnet/runtime #114717](https://github.com/dotnet/runtime/issues/114717)

## 6. Single-.exe bricht sporadisch / Antivirus blockt — Temp-Extraktion

**Symptom:** Self-Contained Single-File-App laeuft meist, bricht aber sporadisch ("Datei nicht gefunden") oder wird vom Antivirus/SmartScreen als verdaechtig gemeldet.

**Ursache:** Self-Contained Single-File extrahiert native DLLs beim Start nach `%LOCALAPPDATA%\Temp\.net\<App>`. Diese Dateien koennen jederzeit von Aufraeum-Tools geloescht werden (dann bricht ein Neustart) oder die Laufzeit-Extraktion wird von AV-Software als verdaechtiges Verhalten gewertet.

**Versionen:** .NET 9 / .NET 10.

**FIX (funktionserhaltend):** `<IncludeNativeLibrariesForSelfExtract>true</IncludeNativeLibrariesForSelfExtract>` korrekt setzen bzw. fuer maximale Robustheit native Libs NICHT bundlen, sondern neben die `.exe` legen (kein Temp-Extract). `.exe` mit gueltigem Code-Signing-Zertifikat signieren (gegen AV/SmartScreen). Bei reinen Managed-Apps kann `DOTNET_BUNDLE_EXTRACT_BASE_DIR` auf einen stabilen Pfad gesetzt werden.

**Quelle:** [dotnet/core #3885 — PublishSingleFile exe dependant on Temp Directory](https://github.com/dotnet/core/issues/3885), [dotnet/runtime #42772 — DllImport fails to find native library](https://github.com/dotnet/runtime/issues/42772)

## 7. WPF Helix Viewport3DX startet schwarz / flackert — AirSpace & Init-Timing

**Symptom:** `HelixToolkit.SharpDX.WPF`-`Viewport3DX` zeigt beim App-Start fuer einige Sekunden ein schwarzes Bild, flackert, oder zeigt das Modell mehrfach — besonders in einer `ViewBox` oder mit mehreren Viewports (CAD-Layout).

**Ursache:** Mehrere Effekte: (a) WPF-AirSpace/Interop-Timing — der DirectX-Inhalt ist beim Start noch nicht bereit. (b) `Viewport3DX` in einer `ViewBox` skaliert/dupliziert die Darstellung. (c) Mehrere Viewports, die sich denselben `EffectsManager` teilen, koennen einzeln crashen, wenn Ressourcen falsch verwaltet werden.

**Versionen:** HelixToolkit.Wpf.SharpDX 2.25.x und aelter.

**FIX (funktionserhaltend):** `Viewport3DX` nicht in eine `ViewBox` packen — stattdessen direkt mit `Grid`/Layout positionieren. `EffectsManager` als langlebiges Singleton/Feld halten (nicht pro Frame neu erzeugen) und ggf. erst im `Loaded`-Event initialisieren. Bei mehreren Viewports auf saubere Ressourcen-Trennung achten. Fuer freistehende, optisch anspruchsvolle 3D-Apps lieber Stride nehmen und WPF nur fuer die Drumherum-UI.

**Quelle:** [helix-toolkit #891 — Viewport3DX is black on app startup](https://github.com/helix-toolkit/helix-toolkit/issues/891), [#1438 — Viewport3DX inside ViewBox crash](https://github.com/helix-toolkit/helix-toolkit/issues/1438), [#1330 — Viewport black](https://github.com/helix-toolkit/helix-toolkit/issues/1330)

## 8. Veldrid — kein D3D12, verwaist seit 2023

**Symptom:** Veldrid-Projekt kann kein DirectX 12 nutzen; gemeldete Bugs bleiben offen, keine neuen Releases.

**Ursache:** Original-Veldrid (mellinoe) hat seit Februar 2023 keinen offiziellen Maintainer mehr und besitzt **kein D3D12-Backend** (nur D3D11/Vulkan/Metal/GL).

**Versionen:** Veldrid (alle).

**FIX (funktionserhaltend):** Fuer Neustarts meiden. Wenn ueberhaupt, den Community-Fork (TechPizzaDev) pruefen. Besser: auf **Silk.NET** (echtes D3D12, aktiv) oder **Stride** (volle Engine) wechseln.

**Quelle:** [mellinoe/Veldrid](https://github.com/mellinoe/Veldrid), [TechPizzaDev/veldrid Fork](https://github.com/TechPizzaDev/veldrid)

## 9. MonoGame-D3D12 instabil + Content-Pipeline-Regression im Preview

**Symptom:** (a) D3D12-Backend ("DesktopDX") in MonoGame nur als Preview, instabil. (b) Konkret in 3.8.5-Preview.2: die Content-Pipeline wirft eine Exception, **Custom Content Importer/Processor** funktionieren nicht mehr (z.B. Tiled-Maps laden nicht).

**Ursache:** D3D12 ist erst ab 3.8.5-Preview verfuegbar, nicht produktionsreif. Zusaetzlich ein konkreter Regressions-Bug: der neue Content-Builder ruft eine obsolete Methode statt der uebergebenen Importer/Processor-Instanz auf und wirft dadurch eine Exception.

**Versionen:** MonoGame 3.8.5-Preview.1/.2 (D3D12), stabil ist 3.8.4 (D3D11).

**FIX (funktionserhaltend):** Fuer Produktion **MonoGame 3.8.4 (D3D11)** nutzen, auf 3.9 fuer stabiles D3D12 warten. Wer das Preview testet und Custom Importer braucht: vorerst kein Custom-Content im neuen Content-Builder, oder bis zum Fix bei der alten Pipeline bleiben.

**Quelle:** [MonoGame Discussion #9155 — 3.8.5 Preview known issues](https://github.com/MonoGame/MonoGame/discussions/9155), [MonoGame 3.8.5-preview.2 Release](https://github.com/MonoGame/MonoGame/releases/tag/v3.8.5-preview.1), [Roadmap](https://docs.monogame.net/roadmap/)

## 10. glTF mit KTX2/Basis-Texturen laedt nicht — KHR_texture_basisu

**Symptom:** Ein glTF/GLB mit komprimierten KTX2-Texturen (kleine Dateien) laedt nicht oder wirft "KHR_texture_basisu is not supported".

**Ursache:** Die `KHR_texture_basisu`-Extension speichert Texturen als Basis-Universal/KTX2 (`image/ktx2`), die zur Laufzeit in ein GPU-Blockformat transcodiert werden muessen. SharpGLTF liest nur das Schema/Datenmodell — es transcodiert NICHT. Viele Renderer/Engines, die die Extension nicht implementieren, brechen ab statt auf eine unkomprimierte Textur zurueckzufallen.

**Versionen:** SharpGLTF 1.0.6 (liest Schema, kein Transcoding); betrifft eigene Renderer.

**FIX (funktionserhaltend):** Wenn der Renderer KTX2 nicht unterstuetzt: Assets ohne `KHR_texture_basisu` exportieren (PNG/JPG-Texturen) — robusteste Loesung. Wer KTX2 braucht, muss einen Basis-Transcoder einbinden (native libktx/basisu) und das transcodierte Blockformat an die GPU geben. In Stride pruefen, welche Texturkompression die jeweilige Version nativ unterstuetzt.

**Quelle:** [KhronosGroup/glTF — KHR_texture_basisu](https://github.com/KhronosGroup/glTF/tree/main/extensions/2.0/Khronos/KHR_texture_basisu), [SharpGLTF GitHub](https://github.com/vpenades/SharpGLTF)

## 11. glTF-Material-Extensions fehlen optisch — Clearcoat/Glas/Sheen

**Symptom:** Ein glTF-Modell laedt, aber fortgeschrittene Materialeffekte (Autolack-Clearcoat, Glas-Transmission, Stoff-Sheen) sehen wie stumpfes Standard-PBR aus.

**Ursache:** Der Renderer/die Engine wertet die KHR-Material-Extensions (`KHR_materials_clearcoat`, `_transmission`, `_sheen`, `_specular`) nicht aus. SharpGLTF liefert die Extension-Daten korrekt, aber der Shader muss sie aktiv verarbeiten — sonst Fallback auf Base-Metallic/Roughness.

**Versionen:** SharpGLTF 1.0.6 (liest die Extensions); Auswertung haengt vom Renderer/Stride-Version ab.

**FIX (funktionserhaltend):** Pruefen, welche KHR-Material-Extensions die Ziel-Engine/der eigene Shader unterstuetzt. Fehlende Effekte im eigenen Shader implementieren (Cook-Torrance + Clearcoat-Layer/Transmission-BTDF). In Stride pruefen, welche Extensions die Material-Pipeline der Version kennt; sonst Materialien auf unterstuetzte Features reduzieren.

**Quelle:** [SharpGLTF GitHub (vpenades)](https://github.com/vpenades/SharpGLTF), [Khronos glTF PBR](https://www.khronos.org/gltf/pbr/)

## 12. D3D12 DEVICE_REMOVED / DEVICE_HUNG — Descriptor & TDR [⭐ HÄUFIG]

**Symptom:** Eigener D3D12-Renderer liefert ein schwarzes Bild, friert ein oder bricht mit `DXGI_ERROR_DEVICE_REMOVED` / `DXGI_ERROR_DEVICE_HUNG` ab.

**Ursache:** Haeufige Ausloeser: (a) Indexierung ueber das Ende eines Descriptor-Heaps hinaus im Shader; (b) GPU-Descriptor-Handle vom falschen Typ gesetzt (z.B. SRV-Handle wo CBV erwartet wird); (c) statisch deklarierte Descriptor-Ranges in der Root-Signature, die vor dem Setzen nicht initialisiert wurden; (d) zu lange GPU-Arbeit → TDR (Timeout Detection and Recovery, ~2 s) loest `DEVICE_HUNG` aus.

**Versionen:** D3D12 (alle), Silk.NET, Vortice.

**FIX (funktionserhaltend):** Debug-Layer aktivieren (Windows-11-Optionsfeature "Graphics Tools" installieren) und Meldungen lesen; **DRED** (Device Removed Extended Data) aktivieren fuer Auto-Breadcrumbs beim Removed-Event. Descriptor-Heap-Indizes und -Typen pruefen, alle statischen Descriptors vor Set-on-Command-List initialisieren. Lange Compute-Passes in kleinere Dispatches aufteilen, um TDR zu vermeiden.

**Quelle:** [Microsoft — D3D12 Debug Layer GPU-based Validation](https://learn.microsoft.com/en-us/windows/win32/direct3d12/using-d3d12-debug-layer-gpu-based-validation), [DirectX-Specs — DRED](https://microsoft.github.io/DirectX-Specs/d3d/DeviceRemovedExtendedData.html), [DirectX-Graphics-Samples #475](https://github.com/microsoft/DirectX-Graphics-Samples/issues/475)

## 13. Stride: "Unable to find compiled shaders" / Crash beim ersten Start

**Symptom:** Stride-App/-Editor crasht beim ersten Start oder beim Laden eines Effekts mit "Unable to find compiled shaders"; manche Shader kompilieren, andere nicht.

**Ursache:** (a) Der Shader-Cache in AppData ist korrupt oder unvollstaendig (Stride kompiliert Materialien im Hintergrund und cacht sie auf Disk). (b) Projekte mit niedrigem Graphics-Feature-Level (z.B. 9_1) und dem zugehoerigen Graphics-Compositor crashen beim Start mit `E_INVALIDARG`-SharpDX-Exception beim Texture-Create.

**Versionen:** Stride 4.x (inkl. 4.3); Feature-Level-9-Bug historisch dokumentiert.

**FIX (funktionserhaltend):** Shader-Cache loeschen (Stride-Cache-Verzeichnis in AppData) und neu kompilieren lassen. GraphicsProfile/Feature-Level auf ein modernes Level (10_0+/11_x) setzen — Level 9_1 vermeiden. Auf aktuelle GPU-Treiber achten (alte/kaputte Treiber sehen wie Shader-Fehler aus).

**Quelle:** [Stride Forums — Unable to find compiled shaders](https://forums.stride3d.net/t/error-unable-to-find-compiled-shaders/952), [stride3d/stride #2269 — Level 9_1 crash at startup](https://github.com/stride3d/stride/issues/2269), [Stride Docs — Compile shaders](https://doc.stride3d.net/4.0/en/manual/graphics/effects-and-shaders/compile-shaders.html)

## 14. Vortice/SharpDX: ComObject-Leak / Hang beim Beenden

**Symptom:** Eigener Renderer mit Vortice.Windows (oder SharpDX) haengt beim Beenden, oder der Debug-Layer meldet beim Device-Release Live-COM-Objekte; gelegentlich `0x8007000E` (Out of Memory) nach langer Laufzeit.

**Ursache:** Vortice/SharpDX bilden COM-Objekte als .NET-Wrapper ab. Werden D3D-Objekte (Buffers, Textures, PSOs, Command-Allocators) nicht deterministisch freigegeben (`Dispose`/`using`), bleiben native Referenzen haengen → Leak/Hang. Der GC raeumt sie nicht zuverlaessig rechtzeitig ab.

**Versionen:** Vortice.Windows 3.8.x, SharpGen.Runtime (auch HelixToolkit.SharpDX).

**FIX (funktionserhaltend):** Alle D3D-Ressourcen deterministisch mit `using`/`Dispose` freigeben, in umgekehrter Erzeugungsreihenfolge. Pro-Frame erzeugte Objekte poolen statt neu allozieren. Beim Shutdown Device zuletzt freigeben; Debug-Layer (`D3D12GetDebugInterface` / `ReportLiveObjects`) zur Leak-Diagnose nutzen. Keine D3D-Objekte im Finalizer-Thread sterben lassen.

**Quelle:** [amerkoleci/Vortice.Windows](https://github.com/amerkoleci/Vortice.Windows), [SharpGen.Runtime.COM — ComObject.cs](https://github.com/SharpGenTools/SharpGen.Runtime.COM/blob/master/ComObject.cs)

---

## Fix-Status

| # | Bug | Status | Anmerkung |
|---|-----|--------|-----------|
| 1 | Farbraum ausgewaschen/dunkel | **kein Lib-Bug — Anwendungsfehler** | Dauerhafte Falle, gilt fuer alle Versionen. Disziplin-Fix. |
| 2 | Flip-Model-Swapchain sRGB | **kein Lib-Bug — API-Design** | Flip-Model erzwingt UNORM; RTV-als-SRGB ist die offizielle Loesung. |
| 3 | Trimmer/AOT entfernt Reflection-Code | **bekannt, by-design** | .NET-Tooling warnt; Fix ist Roots/`[DynamicDependency]`. |
| 4 | Silk.NET AOT RegisterPlatform | **bekannt, Workaround stabil** | Soll laut Maintainer in Silk.NET 3.0 entfallen — bis dahin offen. |
| 5 | .NET 10 native-lib-search | **by-design Breaking Change** | Bewusste Verhaltensaenderung in .NET 10, dokumentiert. |
| 6 | Single-file Temp-Extraktion | **bekannt, alt, teils offen** | Mehrere offene dotnet-Issues; Code-Signing + no-extract mildern. |
| 7 | Helix Viewport3DX schwarz/flackert | **teils gefixt, teils offen** | Mehrere Issues; manche in 2.25.x gemildert, ViewBox-Crash dokumentiert. |
| 8 | Veldrid verwaist, kein D3D12 | **wird nicht gefixt** | Original ohne Maintainer seit 2023; nur Community-Fork. |
| 9 | MonoGame D3D12 + Content-Pipeline-Regression | **D3D12 noch Preview; Pipeline-Bug offen** | Regression in 3.8.5-Preview.2; stabil bleibt 3.8.4. |
| 10 | glTF KTX2 nicht geladen | **kein Lib-Bug — Feature fehlt** | SharpGLTF transcodiert per Design nicht; Renderer-Aufgabe. |
| 11 | glTF-Material-Extensions fehlen | **renderer-abhaengig** | SharpGLTF liefert Daten; Auswertung haengt am Shader/der Engine. |
| 12 | D3D12 DEVICE_REMOVED/HUNG | **kein Lib-Bug — App-Fehler** | Diagnose via Debug-Layer/DRED; Heap-/TDR-Disziplin. |
| 13 | Stride Shader-Cache/Feature-Level | **teils alt-gefixt, Cache-Falle bleibt** | Level-9_1-Crash historisch; Cache-Korruption bleibt moegliche Falle. |
| 14 | Vortice/SharpDX ComObject-Leak | **kein Lib-Bug — Lifecycle** | Deterministisches Dispose ist Pflicht. |

**Offen / unklar (ehrlich markiert):**
- **#4 Silk.NET RegisterPlatform**: Workaround stabil, aber ob Silk.NET 3.0 (Status Mitte 2026 noch nicht final) das wirklich entfernt, ist nicht verifiziert — gh-CLI nicht verfuegbar, Release-Datum offen.
- **#6 Single-file Temp**: mehrere dotnet-Issues offen, kein endgueltiger Fix; Verhalten je nach .NET-Version/AV-Setup unterschiedlich.
- **#7 Helix Viewport3DX**: Mischbild aus mehreren Issues unterschiedlichen Alters — welche exakt in 2.25.x gefixt sind, ist nicht im Detail verifiziert.
- **#9 MonoGame Content-Pipeline-Regression**: aus Preview-Discussion #9155; ob in einer spaeteren Preview/3.9 schon behoben, ist nicht abschliessend verifiziert.
- **#13 Stride Level-9_1-Crash**: Issue historisch dokumentiert; ob in 4.3 noch reproduzierbar, wurde nicht gegengeprueft.

**Ehrlichkeits-Hinweis:** gh-CLI war nicht verfuegbar; Issue-Stati wurden ueber Suchergebnisse/Changelogs/Docs ermittelt, nicht ueber die GitHub-API. Die meisten Eintraege sind **keine Library-Bugs im engeren Sinn**, sondern dauerhafte API-/Anwendungsfallen (Farbraum, Swapchain, Descriptor-Disziplin, COM-Lifecycle, Trimming) — die fixt man nicht "weg", man vermeidet sie. Versions-Anker = Stand 2026-06-13.

---

## Bezug ↔ Best-Practices

| Bug § | Thema | Best-Practice-Abschnitt (best-practices-3d-dotnet-directx-windows.md) |
|-------|-------|----------------------------------------------------------------------|
| §1, §2 | Farbraum / HDR / sRGB-Swapchain | §2 PBR & Materialien, §5 Beleuchtung & Postprocessing |
| §3, §5, §6 | Deployment (Trimming/AOT/Single-file/.NET 10) | §6 Single-.exe / Deployment |
| §4 | Silk.NET NativeAOT-Registrierung | §6 Deployment, §1 Stack-Wahl |
| §7 | WPF Helix Viewport3DX | §3 WPF-3D & Helix Toolkit |
| §8 | Veldrid meiden | §1 Stack-Wahl (Entscheidungsmatrix) |
| §9 | MonoGame D3D12-Reife | §1 Stack-Wahl |
| §10, §11 | glTF Texturen & Material-Extensions | §2 PBR & Materialien, §4 Asset-Pipeline (glTF) |
| §12 | D3D12 DEVICE_REMOVED/Descriptor/TDR | §7 Performance (PSO/Descriptor-Heaps) |
| §13 | Stride Shader-Cache/Feature-Level | §1 Stack-Wahl, §5 Beleuchtung & Postprocessing |
| §14 | Vortice/SharpDX COM-Lifecycle | §1 Stack-Wahl, §7 Performance |

---

## Pflicht-Checkliste vor dem Start

- [ ] **Farbraum geklaert?** BaseColor/Emissive=sRGB, Normal/Roughness/Metallic/AO=linear, in HDR rechnen + tonemappen (§1).
- [ ] **Eigener D3D12-Renderer?** Flip-Model-Swapchain als `_UNORM`, RTV als `_SRGB`-View (§2).
- [ ] **Deployment-Plan?** Engine (Stride) → Self-Contained ohne Trim. Eigener Silk.NET-Renderer → AOT moeglich, dann `RegisterPlatform()` (§3, §4).
- [ ] **.NET 10 + native DLL?** `DllImportSearchPath.AssemblyDirectory` gesetzt / Libs neben `.exe` ohne Temp-Extract (§5, §6).
- [ ] **WPF-Viewport?** Helix `SharpDX.WPF`, EffectsManager als Singleton, nicht in ViewBox, NICHT natives Viewport3D (§7).
- [ ] **Stack-Wahl bewusst?** Veldrid gemieden, MonoGame-D3D12 nur wenn Preview-tauglich (§8, §9).
- [ ] **glTF-Assets geprueft?** KTX2 nur wenn Transcoder vorhanden; Material-Extensions vom Renderer unterstuetzt (§10, §11).
- [ ] **D3D12 Debug-Layer + DRED aktiv?** Descriptor-Heap-Indizes/-Typen geprueft, lange Dispatches gesplittet (§12).
- [ ] **Stride: Shader-Cache sauber?** Feature-Level >= 10_0, aktuelle Treiber (§13).
- [ ] **COM-Lifecycle?** Alle D3D-Objekte per `using`/`Dispose`, Device zuletzt (§14).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-godot](3d-godot.md)
- [3d-metal-scenekit-macos](3d-metal-scenekit-macos.md)
- [3d-rust-wgpu-bevy](3d-rust-wgpu-bevy.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
