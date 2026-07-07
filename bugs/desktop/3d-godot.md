# Bekannte Bugs: 3D mit Godot 4

> **PFLICHT-LESEN vor jeder 3D-Godot-Arbeit (Desktop/Mobile, schoene 3D-Apps).**
> Stand recherchiert **2026-06-13** fuer **Godot 4.6** (ausgeliefert Ende Januar 2026;
> Maintenance: 4.6.1 vom 16. Feb 2026, 4.6.2 vom 1. April 2026 mit 122 Fixes).
> **Empfehlung: immer auf der neuesten Maintenance (>= 4.6.2) arbeiten** — sie behebt einen Grossteil
> der hier gelisteten Regressionen.
> Gegenseite (was man richtig macht): `best-practices/desktop/3d-godot.md`.
> Jeder Eintrag verweist per `§N` auf die passende Best-Practice-Sektion.

---

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

---

## 1. Windows D3D12 Black-Screen auf Intel-iGPUs [⭐ HÄUFIG]

- **Symptom:** Schwarzer Bildschirm, Screen-Tearing, Blur/Wackeln — selbst bei leerer Szene — auf Windows
  mit Intel-iGPU (z.B. Celeron N4500 / Jasper Lake). Tritt in 4.6 auf, in 4.5 nicht.
- **Ursache:** Godot 4.6 macht **D3D12 zum Default-RD-Treiber auf Windows** (Vulkan-Treiber gelten auf
  Windows als schlechter gepflegt). Auf manchen Intel-iGPUs ist der D3D12-Pfad/Treiber fehlerhaft. D3D12
  ist dort sogar schneller (60+ fps vs 40 fps mit Vulkan), aber visuell kaputt.
- **Versionen:** Eingefuehrt mit 4.6 (Default-Wechsel ab 4.6-dev5, PR #113213). Issue noch offen.
- **FIX / Workaround:** Rendering-Treiber auf **Vulkan** zuruecksetzen — `--rendering-driver vulkan`
  auf der Kommandozeile ODER Projekteinstellung `rendering/rendering_device/driver.windows = vulkan`.
  Jede Vulkan-faehige Windows-GPU unterstuetzt auch D3D12 (und Godot faellt seit 4.4 automatisch auf
  Vulkan zurueck, wenn D3D12 fehlt) — der Glitch ist also kein "fehlt", sondern "D3D12-Pfad defekt".
- **Quelle:** [Issue #116919](https://github.com/godotengine/godot/issues/116919),
  [PR #113213](https://github.com/godotengine/godot/pull/113213),
  [Godot 4.6 Release](https://godotengine.org/releases/4.6/)

## 2. macOS startet nicht ("App ist beschaedigt") [⭐ HÄUFIG]

- **Symptom:** Exportierte `.app`/`.dmg` startet beim Endnutzer nicht; Gatekeeper meldet "App ist
  beschaedigt und kann nicht geoeffnet werden".
- **Ursache:** Die App ist nicht **signiert + notarisiert**. Apple verlangt das seit 2019 fuer alle
  ausserhalb des App Store verteilten macOS-Programme.
- **Versionen:** Plattform-Verhalten, unabhaengig von der Godot-Version.
- **FIX:** Mit Apple **Developer ID** signieren (`codesign` nur auf macOS, ODER `rcodesign` plattform-
  uebergreifend auch von Linux/Windows) und bei Apple **notarisieren** (Apple-ID-Credentials oder
  App-Store-Connect-API-Key). Zertifikat im **PKCS#12**-Format ueber Env-Vars
  `GODOT_MACOS_CODESIGN_CERTIFICATE_FILE` / `_PASSWORD` setzen. Universal-Binary (arm64 + x86_64) bauen.
- **Quelle:** [Complete Guide to Signing Godot Games for macOS — oreateai](https://www.oreateai.com/blog/complete-guide-to-signing-godot-games-for-macos/7bc9a68155b01df177139eaad07e9ac0),
  Godot-Docs Export-Architektur

## 3. macOS 4.6.1-Editor-Binaries kaputt (Signatur-Bug)

- **Symptom:** Mit den ersten macOS-Editor-Downloads von **4.6.1** funktionieren **.NET (C#) und
  GDExtension** nicht.
- **Ursache:** Die erste 4.6.1-macOS-Editor-Binary hatte ein **Signing-Issue**, das .NET/GDExtension-
  Support kaputtmachte.
- **Versionen:** Nur 4.6.1, nur macOS-Editor. Wurde mit korrigierter Signatur **neu hochgeladen**.
- **FIX:** Die neu hochgeladene 4.6.1-macOS-Binary verwenden — oder gleich **4.6.2** (1. April 2026)
  ziehen, dann ist das Thema erledigt.
- **Quelle:** [Maintenance release: Godot 4.6.1](https://godotengine.org/article/maintenance-release-godot-4-6-1/)

## 4. LightmapGI-Bake schlaegt fehl: UV2 fehlt / schlecht [⭐ HÄUFIG]

- **Symptom:** LightmapGI-Bake bricht ab, einzelne Meshes bleiben **komplett schwarz/unbeleuchtet**,
  Lightmap leckt an Box-Meshes (Light-Leaking), oder der Bake gelingt mal und mal nicht.
- **Ursache:** Fehlende oder qualitativ schlechte **UV2-Lightmap-Koordinaten**. Insbesondere: UV2
  **in Blender** erzeugt → Godot kann oft NICHT backen; UV2 **in Godot** unwrappt → Bake klappt.
  Box-PrimitiveMeshes haben schlechte Auto-UV2-Qualitaet (#98685).
- **Versionen:** Langlebiges Thema ueber mehrere 4.x (Tracker #56033). Einzelfaelle weiter offen.
- **FIX:** Beim Import **UV2-Auto-Unwrap in Godot aktivieren** (nicht in Blender unwrappen). Lightmap-
  Texel-Density pro Mesh angemessen waehlen; bei Light-Leaking an Boxen Geometrie/Density anpassen.
  Achtung Crash-Falle: UV2-Werte > 100 koennen Godot haengen lassen + Vulkan-Treiber verlieren (#75726).
- **Quelle:** [Issue #98685](https://github.com/godotengine/godot/issues/98685),
  [Issue #73124](https://github.com/godotengine/godot/issues/73124),
  [Tracker #56033](https://github.com/godotengine/godot/issues/56033),
  [Issue #75726](https://github.com/godotengine/godot/issues/75726)

## 5. Lightmap-Bake im Compatibility-Renderer kaputt (4.6-Regression)

- **Symptom:** Lightmap-Backen schlaegt im **Compatibility-Renderer** mit einem Shader-Compile-Fehler
  fehl: undefinierte Variable **`uv2_attrib_input`** im GLES3-Vertex-Shader.
- **Ursache:** Core-Shader-**Regression** zwischen 4.5/4.6-dev1 und spaeterem 4.6-dev. Betrifft nur den
  Compatibility-Backend-Bake.
- **Versionen:** Reproduzierbar in 4.6.dev, NICHT in 4.5.stable / 4.6.dev1. Stable-Status in 4.6.x:
  **unklar** (siehe Ehrlichkeits-Hinweis).
- **FIX:** Auf **4.6.2** updaten (122 Fixes, "most stable yet"). Falls das Problem bestehen bleibt:
  Lightmap im **Forward+/Mobile** backen (RenderingDevice-Backends) statt Compatibility.
- **Quelle:** [Issue #111465](https://github.com/godotengine/godot/issues/111465),
  [Maintenance release: Godot 4.6.2](https://godotengine.org/article/maintenance-release-godot-4-6-2/)

## 6. Major-Rendering-Regression in fruehem 4.6 (Sky/VoxelGI/SDFGI)

- **Symptom:** Nach Upgrade **4.5 → 4.6** wirkt die ganze Render-Pipeline kaputt: defekte Sky-Shader,
  VoxelGI mit falscher Lichtausbreitung / dreckig-ueberbelichtetem Ergebnis, deutlich verschlechterte
  SDFGI-Qualitaet gegenueber 4.5.
- **Ursache:** Rendering-Regression im (fruehen) 4.6-Pfad. Gemeldet kurz nach dem 4.6-Release (29.01.2026).
- **Versionen:** 4.6 (gemeldet gegen Release/fruehe 4.6). Ob in 4.6.1/4.6.2 vollstaendig behoben:
  **nicht eindeutig belegt** — 4.6.2 listet mehrere Rendering-Fixes, aber dieses Issue nicht namentlich.
- **FIX:** Auf **4.6.2** updaten und die GI-/Sky-Optik visuell neu pruefen. Falls weiterhin defekt:
  Issue gegen die genutzte Version beobachten; Werte/Probe in einer Vergleichsszene 4.5 vs 4.6.2.
- **Quelle:** [Issue #115599](https://github.com/godotengine/godot/issues/115599),
  [Maintenance release: Godot 4.6.2](https://godotengine.org/article/maintenance-release-godot-4-6-2/)

## 7. Materialien weiss/flach/grau nach Import (Blender→Godot) [⭐ HÄUFIG]

- **Symptom:** Modell sieht in Blender korrekt aus, in Godot erscheint es flach **weiss/grau/unshaded**,
  Metallic/Roughness fehlen oder Roughness wird faelschlich als Metallic verwendet.
- **Ursache:** Der glTF/GLB-Export aus Blender verliert Maps, wenn der Material-Output nicht sauber am
  **Principled-BSDF** haengt (custom Node-Setups, Non-Standard-Shader, deaktiviertes Material-Export).
  FBX ist hier noch fragiler (oft gar keine Metallic/Roughness-Werte gesetzt).
- **Versionen:** Langlebiges Pipeline-Thema (4.2–4.6).
- **FIX:** **GLB/glTF 2.0 statt FBX** verwenden; in Blender den **Metallic/Roughness-Workflow** mit
  Principled-BSDF nutzen, Maps direkt verdrahten; beim Export "**Export Materials = Export**" und ein
  passendes Image-Format. Bei ORM-gepackten Texturen in Godot auf **ORMMaterial3D** umstellen (§2/§5
  der Best Practices).
- **Quelle:** [Blender 4.2 GLB Export Loses Materials — gamineai](https://gamineai.com/help/blender-4-2-glb-export-loses-materials-godot-4-metallic-roughness-import-fix),
  [Issue #82455](https://github.com/godotengine/godot/issues/82455)

## 8. .blend-Import haengt in Endlosschleife (4.6-Bug, gefixt in 4.6.2)

- **Symptom:** Beim Importieren von `.blend`-Dateien haengt der Import / laeuft in eine Endlosschleife.
- **Ursache:** Der Zaehler fuer Blender-Import-Versuche wurde nicht hochgezaehlt → keine Abbruchbedingung.
- **Versionen:** 4.6. **Behoben in 4.6.2** (GH-116589).
- **FIX:** Auf **4.6.2** updaten. (Generell: lieber direkt nach GLB exportieren statt `.blend` zu
  importieren, dann ist man von der Blender-Import-Bruecke unabhaengig.)
- **Quelle:** [Maintenance release: Godot 4.6.2 (GH-116589)](https://godotengine.org/article/maintenance-release-godot-4-6-2/)

## 9. Mobile-Renderer reduziert unterstuetzte Android-Geraete [⭐ HÄUFIG]

- **Symptom:** Nach Wechsel des Renderers von `gl_compatibility` auf `mobile` werden im Play Store /
  auf Geraeten **weniger Android-Geraete** unterstuetzt; alte Geraete koennen die App nicht installieren/starten.
- **Ursache:** Der Mobile-Renderer laeuft ueber **Vulkan** (RenderingDevice). Android-Geraete ohne
  ausreichenden Vulkan-Support fallen aus der Geraete-Matrix.
- **Versionen:** Bekannt, Verhalten gilt fuer 4.6 (#111729).
- **FIX:** Vor der Renderer-Wahl die **Zielgeraete-Matrix** pruefen. Wenn maximale Geraeteabdeckung
  noetig ist: Compatibility (GL ES 3) behalten — dann aber kein SSR/Echtzeit-GI, Optik entsprechend
  planen (LightmapGI fuer indirektes Licht).
- **Quelle:** [Issue #111729](https://github.com/godotengine/godot/issues/111729),
  [Android: Configure projects — Android Developers](https://developer.android.com/games/engines/godot/godot-configure)

## 10. C#: kein Web-Export, Android nur experimentell [⭐ HÄUFIG]

- **Symptom:** C#-Projekt laesst sich nicht ins **Web** exportieren (Export bricht/leer); auf
  **Android** Export- oder Laufzeitfehler (z.B. .NET-Assemblies fuer arm64 zur Laufzeit nicht gefunden).
- **Ursache:** **Web-Export wird von C# NICHT unterstuetzt** (kein WASM/Mono-Pfad). **Android/iOS-C#**
  ist seit 4.2 **experimentell** (NativeAOT, .NET 7/8, nur arm64/x64), NativeAOT auf Android nur per
  manueller Konfiguration, nicht out-of-the-box.
- **Versionen:** Stand 2026 weiterhin so (Web unsupported, Mobile experimentell).
- **FIX:** Fuer **Web** und robustes **Mobile** → **GDScript** verwenden. Wenn C# auf Android
  unvermeidlich: .NET 7+/NativeAOT strikt einhalten, nur arm64/x64, mit Hand-Config (kein verlaesslicher
  Default). Renderer/Optik sind sprachunabhaengig — die Sprachwahl betrifft nur Logik, nicht das Bild.
- **Quelle:** [Current state of C# platform support (4.2)](https://godotengine.org/article/platform-state-in-csharp-for-godot-4-2/),
  [Issue #97775](https://github.com/godotengine/godot/issues/97775),
  [Issue #81852](https://github.com/godotengine/godot/issues/81852)

## 11. Compatibility-Renderer: langsame Startzeit (~4 s)

- **Symptom:** App mit Compatibility-Backend braucht spuerbar lange zum Start (~4 s) gegenueber
  Forward+/Mobile (< 0.5 s).
- **Ursache:** Shader-/Init-Overhead des GL-Compatibility-Pfads.
- **Versionen:** Gemessen in 4.4.1 (#106310); fuer 4.6 als fortbestehendes Verhalten anzunehmen
  (nicht explizit als behoben belegt).
- **FIX:** Wo schneller Start zaehlt: Forward+ (Desktop) bzw. Mobile (Android) waehlen. Compatibility
  nur dort einsetzen, wo es Pflicht ist (Web) oder fuer sehr alte Hardware.
- **Quelle:** [Issue #106310](https://github.com/godotengine/godot/issues/106310)

## 12. VoxelGI/SDFGI/SSR: Flackern & Frustum-Offset-Fehler

- **Symptom:** (a) VoxelGI/SDFGI **flackern**, besonders mit **TAA** aktiv und bei Uebergaengen in
  dunkle Bereiche. (b) VoxelGI/SDFGI/SSR rendern **falsch**, sobald der Kamera-`frustum_offset`
  ungleich (0,0) ist.
- **Ursache:** (a) Wechselwirkung der temporalen GI-Akkumulation mit TAA bzw. SDFGI-Probe-Uebergaenge.
  (b) Die Screen-Space-/GI-Pfade gehen von zentriertem Frustum aus.
- **Versionen:** Langlebige GI-Instabilitaeten (#62080, #74597). Frustum-Offset (#108508). 4.6 hat den
  **SSR neu geschrieben** (Hi-Z-Tracing) → SSR-Stabilitaet deutlich besser; die VoxelGI/SDFGI-Flacker-
  und Frustum-Themen sind davon nicht automatisch alle erledigt.
- **FIX:** Bei Flackern TAA-Wechselwirkung pruefen (TAA testweise aus, Vergleich); fuer hoechste
  Stabilitaet statischer Szenen **LightmapGI** statt Echtzeit-GI. Frustum-Offset nur einsetzen, wenn
  die GI/SSR-Korrektheit dort verifiziert ist (sonst vermeiden).
- **Quelle:** [Issue #62080](https://github.com/godotengine/godot/issues/62080),
  [Issue #74597](https://github.com/godotengine/godot/issues/74597),
  [Issue #108508](https://github.com/godotengine/godot/issues/108508),
  [SSR-Rewrite Deep Dive — StraySpark](https://www.strayspark.studio/blog/godot-46-rendering-deep-dive-ssr-lightmapper-performance)

---

## Fix-Status

| # | Bug | Status | Beleg |
|---|-----|--------|-------|
| 1 | D3D12 Black-Screen Intel-iGPU | **offen** (Workaround: Vulkan) | #116919 offen; Default-Verhalten dokumentiert |
| 2 | macOS startet nicht (Signatur) | **kein Bug — Pflicht-Schritt** (signieren+notarisieren) | Apple-Policy seit 2019 |
| 3 | macOS 4.6.1-Editor-Binary kaputt | **GEFIXT** (neu hochgeladen / 4.6.2) | 4.6.1-Release-Notes |
| 4 | LightmapGI UV2 fehlt/schlecht | **teils offen** (Einzelfaelle), Workaround: UV2 in Godot | #98685/#56033 offen |
| 5 | Compatibility-Lightmap `uv2_attrib_input` | **unklar** (vermutlich in 4.6.x adressiert) | #111465 — Stable-Status nicht eindeutig |
| 6 | Major-Rendering-Regression Sky/GI | **unklar** (4.6.2 hat Rendering-Fixes, Issue nicht namentlich) | #115599 / 4.6.2-Notes |
| 7 | Materialien weiss nach Blender-Import | **kein Engine-Bug** — Export-Pipeline; Workaround GLB+BSDF | gamineai / #82455 |
| 8 | .blend-Import Endlosschleife | **GEFIXT in 4.6.2** | GH-116589 |
| 9 | Mobile reduziert Android-Geraete | **by design** (Vulkan-Anforderung) | #111729 |
| 10 | C# kein Web / Android experimentell | **offen / by design** | C#-Platform-State, #97775 |
| 11 | Compatibility ~4 s Startzeit | **unklar** (gegen 4.6 nicht als behoben belegt) | #106310 |
| 12 | VoxelGI/SDFGI Flackern + Frustum-Offset | **teils offen** (SSR in 4.6 neu/besser; GI-Flacker offen) | #62080/#74597/#108508 |

**Offen (aktiv beachten):** #1 (D3D12 iGPU), #4 (LightmapGI UV2 Einzelfaelle), #10 (C# Web/Mobile),
#12 (VoxelGI/SDFGI-Flackern, Frustum-Offset).

**Empfohlene Basis:** Immer auf **>= 4.6.2** (1. April 2026, 122 Fixes, "most stable yet", laut
Release-Notes keine bekannten Inkompatibilitaeten zu 4.6.1). Es existieren bereits weitere
Maintenance-Releases (4.6.3 angekuendigt) — vor Produktionsentscheidungen die jeweils neueste
4.6.x-Maintenance pruefen.

**Ehrlichkeits-Hinweis:** `gh`-CLI ist NICHT verfuegbar → der genaue "closed/fixed"-Status einzelner
GitHub-Issues konnte nicht direkt aus dem Issue-Tracker bestaetigt werden. Status-Angaben stuetzen sich
auf offizielle Release-/Maintenance-Notes und Such-Zusammenfassungen. Als **unklar** markierte Faelle
(#5, #6, #11) sind vor kritischen Entscheidungen am Issue-Tracker bzw. an der jeweils genutzten
4.6.x-Version selbst zu verifizieren.

---

## Bezug ↔ Best-Practices

| Bug (hier) | Best-Practice (Gegenseite) |
|-----------|----------------------------|
| §1 D3D12 Black-Screen | best-practices §6 (Windows-Export) / §9 (Fallen) |
| §2 macOS Signatur | best-practices §6 (macOS-Export) |
| §3 4.6.1-Editor-Binary | best-practices §6 (Versions-Hygiene) |
| §4/§5 LightmapGI/UV2/Compatibility-Bake | best-practices §3 (GI/LightmapGI) |
| §6 Rendering-Regression Sky/GI | best-practices §3 (GI) / §4 (Postprocessing) |
| §7 Materialien weiss (Blender) | best-practices §2 (PBR) / §5 (glTF-Import) |
| §8 .blend-Import-Endlosschleife | best-practices §5 (glTF-Import-Pipeline) |
| §9 Mobile reduziert Geraete | best-practices §1 (Renderer-Wahl) / §6 (Android) |
| §10 C# Web/Android | best-practices §7 (GDScript vs C#) |
| §11 Compatibility-Startzeit | best-practices §1 (Renderer-Wahl) |
| §12 GI-Flackern / Frustum-Offset | best-practices §3 (GI) / §4 (SSR) |

---

## Pflicht-Checkliste vor dem Start

- [ ] **Godot-Version >= 4.6.2** (bzw. neueste 4.6.x-Maintenance) — behebt einen Grossteil der Regressionen.
- [ ] **Windows:** D3D12-Default bedacht? Auf Intel-iGPU vorsorglich Vulkan-Fallback dokumentiert (§1).
- [ ] **macOS:** Signier-/Notarisierungs-Pipeline (Developer ID, PKCS#12, Env-Vars) eingerichtet (§2).
- [ ] **Android:** Zielgeraete-Matrix gegen Vulkan-Anforderung des Mobile-Renderers geprueft (§9).
- [ ] **Sprache:** Web/robustes Mobile → GDScript; C# nur wo passend (§10).
- [ ] **LightmapGI:** UV2-Auto-Unwrap **in Godot** beim Import, NICHT in Blender; UV2 nicht anderweitig verplanen (§4).
- [ ] **Import:** GLB/glTF 2.0 statt FBX; Principled-BSDF + "Export Materials" aktiv (§7). Lieber GLB als `.blend` importieren (§8).
- [ ] **Optik nach Upgrade** 4.5→4.6 visuell gegengeprueft (Sky/VoxelGI/SDFGI) (§6).
- [ ] **GI-Stabilitaet:** Bei Flackern TAA-Wechselwirkung getestet; statische Szenen lieber LightmapGI (§12).
- [ ] **Renderer pro Zielplattform** getrennt visuell geprueft (Optik nicht portabel).


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-dotnet-directx-windows](3d-dotnet-directx-windows.md)
- [3d-metal-scenekit-macos](3d-metal-scenekit-macos.md)
- [3d-rust-wgpu-bevy](3d-rust-wgpu-bevy.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
