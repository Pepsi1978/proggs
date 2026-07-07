# Bekannte Bugs: 3D auf Android (Filament/SceneView)

> **PFLICHT-LESEN vor jeder Arbeit an einer 3D-App (Filament/SceneView/Vulkan/Compose/glTF).**
> Stand recherchiert **2026-06-13** fuer **Filament 1.71.x / SceneView 4.18.0 / Android 15+ (targetSdk)**.
> Gegenseite (Best-Practices, was man TUN soll): `best-practices/android/3d-filament-android.md`.
> Diese Datei = was SCHIEFGEHT und wie man es vermeidet/fixt. Quellen-Prioritaet: offiziell zuerst
> (google/filament Issues, sceneview/sceneview, Android Developers), dann Community.
> **Ehrlichkeits-Hinweis:** gh-CLI war nicht verfuegbar — Issue-Status (open/closed) konnte nicht
> live abgefragt werden. Fix-Belege stammen aus Release-Notes/Issue-Texten/Web; unsichere Faelle sind
> mit ❓ markiert. Versionsnummern in FIX-Zeilen sind belegt wo moeglich, sonst als "pruefen" gekennzeichnet.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Play-Upload abgelehnt: "native libraries not 16 KB aligned" | `packaging.jniLibs.pageAlignSharedLibraries = true` + AGP >= 8.5.1. **filamat-android-lite meiden** (kein 16-KB-Support). | §1 |
| 2 | App-Speicher waechst stetig pro Frame | NIEMALS `View.setViewport(...)` jeden Frame mit wechselnden Werten. Nur bei echter Aenderung. | §2 |
| 3 | SIGSEGV sobald 2+ 3D-Ansichten sichtbar sind | EINE `Engine` + mehrere `View`/`Scene`/`Renderer` teilen. NIE mehrere Engines/SwapChains parallel. | §3 |
| 4 | Crash beim Schliessen/Wegwischen/Drehen, "global reference table overflow" | Saubere Teardown-Reihenfolge: erst alle Ressourcen `destroy()`, ganz zuletzt `Engine.destroy()`. Nicht direkt nach Creation zerstoeren. | §4 |
| 5 | Crash "Material version mismatch. Expected N but received M" | `matc` aus EXAKT demselben Release wie `filament-android`/`gltfio`. Alle filament-Deps auf gleiche Version. | §5 |
| 6 | Schwarzer Screen, nur App-Bar sichtbar | Echtgerät statt Emulator; `UiHelper.attachTo`; Logcat auf Shader-Compile-Fehler pruefen. | §6 |
| 7 | Vulkan langsamer/instabiler als erwartet (Adreno/Mali) | Default **OpenGL ES** belassen. Vulkan nur gezielt + GLES-Fallback. Materialien IMMER mit matc optimieren. | §7 |
| 8 | KTX2/Basis-Texturen erscheinen schwarz/dunkel auf dem Geraet | Transcoder-Format/Geraet pruefen; Pipeline (gltf-transform) gegenchecken; auf Echtgeraet testen; ggf. UASTC statt ETC1S. | §8 |
| 9 | Dynamic Resolution → Speicher-Spikes / OOM-Crash | Vorsichtig einsetzen, Speicher monitoren; mit Vulkan zusaetzlich Artefakte (Pixel 4). | §9 |
| 10 | `rotation`/`scale` driften bei wiederholtem Setzen | SceneView >= 4.17.0 (gecachte TRS, keine Re-Dekomposition). | §10 |
| 11 | AR-Crash bei Resume / `CameraNotAvailableException` | In `onResume`/Session-Resume abfangen; SceneView-Compose-Camera-Stream-Fixes nutzen; ARCore-Play-Services-Version pruefen. | §11 |
| 12 | Crash beim schnellen Erstellen+Wegwischen (ViewPager/Liste) | Detach/Destroy nicht direkt nach Create; Erstellung/Zerstoerung nicht gleichzeitig; Quick-create-detach ist bekannt fragil. | §12 |
| 13 | Transparenter Hintergrund + Postprocessing = falsch geblendet | Auf aktuelle Filament-Version (gefixt) + `UiHelper.isOpaque=false` + `ClearOptions`. | §13 |
| 14 | Engine in eigenem Thread (HandlerThread) → SIGSEGV | Engine/Renderer auf konsistentem Thread bedienen; nicht zwischen Threads mischen. | §14 |

---

## 1. Play-Upload abgelehnt: native `.so` nicht 16-KB-aligned [⭐ HÄUFIG]

- **Symptom:** Google Play lehnt AAB/APK ab — "Your app's native libraries are not aligned to 16 KB"
  / App crasht auf 16-KB-Geraeten beim Start.
- **Ursache:** Seit **1. November 2025** muessen alle neuen Apps/Updates, die Android 15 (API 35)+ targeten,
  16-KB-Memory-Pages auf 64-bit-Geraeten unterstuetzen. Filaments/SceneViews native `.so` waren teils nur
  4-KB-aligned. Sonderfall: **`filamat-android-lite`** hat (Issue #9460) `libfilamat-jni.so` mit 4-KB-Alignment —
  bricht selbst dann, wenn der Rest passt.
- **Versionen:** betrifft Android 15+; Filament-Consumer allgemein. SceneView ab **v4.16.8** mit Fix-Support.
- **FIX:**
  - App-`build.gradle`: `packaging { jniLibs { pageAlignSharedLibraries = true } }` (bzw. Library-Modul:
    `experimentalProperties["android.nativeLibraryAlignmentPageSize"] = "16k"`).
  - **AGP >= 8.5.1** verwenden (richtet uncompressed `.so` automatisch 16-KB-aligned aus).
  - SceneView **>= 4.16.8** nehmen; `filamat-android-lite` meiden, bis 16-KB-fix bestaetigt (Issue #9460).
  - Vor Upload mit 16-KB-Checker pruefen.
- **Quelle:** https://github.com/google/filament/issues/9460 ·
  https://developer.android.com/guide/practices/page-sizes ·
  https://android-developers.googleblog.com/2025/07/transition-to-16-kb-page-sizes-android-apps-games-android-studio.html

## 2. Speicher waechst pro Frame durch `View.setViewport` [⭐ HÄUFIG]

- **Symptom:** RAM-Verbrauch steigt kontinuierlich waehrend des Renderns, App wird langsam / OOM.
- **Ursache:** `View::setViewport(...)` wird in jedem Frame mit Werten aufgerufen, die sich von Frame zu Frame
  unterscheiden. Filament cached/alloziert pro neuer Viewport-Konfiguration, gibt aber alte nicht frei.
- **Versionen:** gemeldet auf Android mit Filament **v1.9.9** (Issue #3381).
- **FIX:** Viewport **nur bei echter Aenderung** setzen (z.B. nach Resize/Rotation), niemals unkonditional im
  Frame-Loop. Filament-seitig als "Fix memory leak when calling View::setViewport frequently" in den
  RELEASE_NOTES vermerkt — d.h. neuere Versionen lindern, aber die Best Practice (nicht jeden Frame setzen)
  bleibt richtig. **Fix-Version exakt: pruefen** (RELEASE_NOTES nach dem Eintrag durchsuchen).
- **Quelle:** https://github.com/google/filament/issues/3381

## 3. SIGSEGV bei mehreren 3D-Views / mehreren Engines [⭐ HÄUFIG]

- **Symptom:** Fatal signal 11 (SIGSEGV), sobald zwei oder mehr Filament-Views gleichzeitig gerendert werden;
  geraeteabhaengig (z.B. vivo S15/S17 Pro crashen auf 1.38.0, andere Geraete laufen).
- **Ursache:** Mehrere `Engine`-Instanzen bzw. mehrere SwapChains kollidieren. Auch "gemeinsame Engine, aber je
  View eigener UiHelper/Choreographer/Renderer/SwapChain" fuehrt zu SIGSEGV. Mehrere Views in einem Frame
  funktionieren auf Android laenger nicht (Issue #2364). gltf-viewer-Sample crasht mit mehreren Instanzen (#7303).
- **Versionen:** durchgehend gemeldet (1.38.0 u.a.); device-spezifisch; gilt als bekannte Limitierung.
- **FIX:** **EINE** `Engine` verwenden und mehrere `View`/`Scene`/`Camera` teilen, statt mehrere Engines. Wenn
  mehrere Surfaces noetig sind: extrem vorsichtig, auf vielen Geraeten testen. Im Zweifel auf eine sichtbare 3D-View
  pro Zeitpunkt beschraenken. **Status der einzelnen Issues: ❓ (gh-CLI nicht verfuegbar — open/closed unbestaetigt).**
- **Quelle:** https://github.com/google/filament/issues/1344 ·
  https://github.com/google/filament/issues/2364 · https://github.com/google/filament/issues/7303

## 4. Native Memory Leak / "global reference table overflow" beim Teardown [⭐ HÄUFIG]

- **Symptom:** "JNI ERROR (app bug): global reference table overflow (max=51200)"; nativer Speicher steigt;
  Crash beim Verlassen/Zerstoeren des Screens; "destroying material with instances still alive".
- **Ursache:** Filament-Ressourcen (Renderable/Entities, `MaterialInstance`, Textures, ByteBuffer, SwapChain)
  werden nicht oder in falscher Reihenfolge zerstoert. `TextureHelper.setBitmap` hat einen eigenen Leak (#6936).
  Engine direkt nach Creation zerstoeren crasht ebenfalls (#4881).
- **Versionen:** mehrfach gemeldet (Discussion #7394, Issues #6936, #4881, #3039 Engine.destroy in Fragment).
- **FIX:** Strikte Teardown-Reihenfolge — erst **alle** Entities/Renderables aus der Scene entfernen und
  `destroy()`, dann `MaterialInstance`s, `Material`s, Textures, IndexBuffer/VertexBuffer, `Scene`, `View`,
  `Camera`, `Renderer`, `SwapChain`, **ganz zuletzt** `Engine.destroy()`. Nicht zerstoeren waehrend gleichzeitig
  erstellt wird; nicht direkt nach Creation. Lifecycle sauber an `DisposableEffect`/`onDestroy` koppeln.
- **Quelle:** https://github.com/google/filament/discussions/7394 ·
  https://github.com/google/filament/issues/6936 · https://github.com/google/filament/issues/4881 ·
  https://github.com/google/filament/issues/3039

## 5. matc-Material laedt nicht — "Material version mismatch" [⭐ HÄUFIG]

- **Symptom:** Crash `Panic in createParser() reason: Material version mismatch. Expected 12 but received 11.`
  (oder andere Zahlenpaare); Material rendert kaputt / laedt nicht.
- **Ursache:** Die `.filamat`-Datei wurde mit einer **anderen `matc`-Version** kompiliert als die Runtime-Lib
  (`filament-android`) erwartet. Auch Mismatch zwischen `filament-android`, `gltfio-android` und `filamat-android`
  innerhalb desselben Projekts loest das aus.
- **Versionen:** versionsuebergreifend (4685/4135/4399 dokumentieren versch. Versions-Paare).
- **FIX:** `matc` **immer** aus exakt demselben Filament-Release wie die Runtime nehmen. Alle filament-Maven-Deps
  auf identische Version pinnen. Nach jedem Filament-Upgrade alle `.mat` → `.filamat` neu kompilieren.
  (Material-Version aendert sich nur selten, aber wenn, dann hart.)
- **Quelle:** https://github.com/google/filament/issues/4685 ·
  https://github.com/google/filament/issues/4399 · https://google.github.io/filament/Materials.md.html

## 6. Schwarzer Screen (nur App-Bar sichtbar)

- **Symptom:** Szene bleibt komplett schwarz, App startet aber, keine Geometrie sichtbar.
- **Ursache:** Shader-Compile-Fehler, Emulator-GPU ohne passende GL-Features, oder Surface nicht korrekt an
  Filaments SwapChain attached.
- **Versionen:** Issue #4692 (allgemein, weiterhin gueltiges Pattern).
- **FIX:** Auf **echtem Geraet** testen (nicht Emulator); `UiHelper.attachTo(surfaceView)` korrekt aufrufen;
  Logcat auf Shader-Compile-/Link-Fehler durchsuchen; pruefen ob IBL/Skybox gesetzt und Kamera korrekt
  positioniert ist.
- **Quelle:** https://github.com/google/filament/issues/4692

## 7. Vulkan langsamer/instabiler als OpenGL ES (Adreno/Mali)

- **Symptom:** Vulkan-Backend liefert deutlich weniger FPS als GLES (z.B. 21.6 vs 44.9 fps, oder 43 vs 60 fps);
  Segfaults in `vkCreateGraphicsPipelines`/qcom-Treiber; Crash auf Pixel 4XL mit Material + Vulkan.
- **Ursache:** Adreno/PowerVR/Mali-Treiberprobleme; unoptimierte Materialien triggern Treiber-Segfaults;
  Vulkan-Pfad auf Mobile ist auf vielen Geraeten noch schlechter optimiert als der GLES-Pfad. Auch reiner
  GLES-Pfad kann auf Adreno 750/830 in `libGLESv2_adreno.so` crashen (#8774).
- **Versionen:** #4225, #7091 (Perf), #5294 (Adreno-Segfault unoptimierte Materialien), #6444 (Pixel 4XL),
  #8774 (Adreno GLES-Crash), #8028 (Mali vs Adreno).
- **FIX:** **Default OpenGL ES belassen.** Vulkan nur gezielt aktivieren und IMMER GLES-Fallback bereitstellen.
  Materialien **immer mit matc optimieren** (nicht unoptimiert ausliefern → Treiber-Segfault). Auf realer
  Adreno-/Mali-Hardware messen statt nur Emulator.
- **Quelle:** https://github.com/google/filament/issues/4225 ·
  https://github.com/google/filament/issues/7091 · https://github.com/google/filament/issues/5294 ·
  https://github.com/google/filament/issues/6444 · https://github.com/google/filament/issues/8774

## 8. KTX2/Basis-Texturen erscheinen schwarz oder zu dunkel auf dem Geraet

- **Symptom:** Mit KTX2/Basis (`KHR_texture_basisu`) komprimierte Texturen werden auf Android schwarz/dunkel
  dargestellt; auf Desktop sieht alles korrekt aus.
- **Ursache:** Transcoder-Probleme des Basis-Universal-Pfads auf manchen Android-GPUs; falsches Ziel-GPU-Format
  beim Transcoding; Kombination Draco + KTX2 kann Texturen schwarz machen. Bekannt aus dem groesseren
  glTF-Oekosystem (model-viewer, three.js, Babylon.js) — derselbe Transcoder-Unterbau betrifft Filament.
- **Versionen:** Filament-KTX2-Support via Ktx2Reader/BasisEncoder (#4771). Geraete-/Transcoder-abhaengig.
  **Genauer Filament-Fix-Status: ❓** — primaer Oekosystem-/Pipeline-Problem.
- **FIX:** Auf **Echtgeraet** verifizieren (Emulator taeuscht); Encoding pruefen (ETC1S vs UASTC — bei dunklen
  Ergebnissen UASTC testen); Pipeline (`gltf-transform`/`gltfpack`) gegenchecken; sicherstellen dass das
  Ziel-GPU-Format unterstuetzt ist; bei Verdacht erst unkomprimiert testen, dann KTX2 wieder zuschalten.
- **Quelle:** https://github.com/google/filament/issues/4771 ·
  https://github.com/google/model-viewer/discussions/4207 ·
  https://forum.babylonjs.com/t/ktx2-texture-displayed-black-on-android-phone/34773

## 9. Dynamic Resolution → Speicher-Spikes / OOM-Crash

- **Symptom:** Mit aktivierter Dynamic Resolution schwankt der Speicher stark, App crasht nach einiger Zeit
  durch OOM; mit Vulkan zusaetzlich Artefakte (Pixel 4).
- **Ursache:** Bekannter Bug: Dynamic Resolution alloziert/freed Render-Targets beim Skalenwechsel ungeschickt →
  Spikes. Vulkan-Backend zeigt zusaetzlich Bild-Artefakte.
- **Versionen:** Issue #1898 (gemeldet 2019, Android); #5885 (Vulkan-Artefakte Pixel 4).
  **Aktueller Fix-Status: ❓ (open/closed via gh-CLI nicht pruefbar).**
- **FIX:** Dynamic Resolution vorsichtig einsetzen, Speicher live monitoren, Skalengrenzen testen (z.B.
  minScale 0.1, maxScale 0.8). Nur mit Postprocessing aktiv (DR ist Teil davon). Auf schwachen Geraeten lieber
  feste niedrigere Aufloesung statt aggressiver DR. Mit Vulkan extra auf Artefakte achten.
- **Quelle:** https://github.com/google/filament/issues/1898 ·
  https://github.com/google/filament/issues/5885

## 10. Transform-Drift bei wiederholtem Setzen von rotation/scale (SceneView)

- **Symptom:** Bei wiederholtem Setzen von `rotation`/`scale` einer Node driftet die Skalierung/Rotation langsam
  weg (akkumulierende Fehler).
- **Ursache:** Per-Komponenten-Setter dekomponierte die Transform-Matrix neu und re-komponierte sie → numerische
  Drift ueber viele Updates.
- **Versionen:** **gefixt in SceneView >= 4.17.0** (gecachte TRS-Getter, keine Re-Dekomposition mehr).
- **FIX:** SceneView **>= 4.17.0** (idealerweise 4.18.0) verwenden. Falls aeltere Version unvermeidbar:
  Transform als komplette Matrix setzen statt einzelne Komponenten wiederholt.
- **Quelle:** https://github.com/sceneview/sceneview (Releases v4.17.0)

## 11. AR-Crash bei Resume / `CameraNotAvailableException` (arsceneview)

- **Symptom:** AR-View crasht beim Wiederaufnehmen (App in den Vordergrund), `CameraNotAvailableException`
  → wird in `IllegalStateException` umgewandelt; Scene-Viewer-AR crasht sofort auf manchen Geraeten
  (Pixel 8a / Android 16) nach ARCore-Play-Services-Update.
- **Ursache:** Kamera ist beim Session-Resume noch nicht verfuegbar; in Compose: Camera-Stream wird bei
  Recomposition neu erstellt / stale Camera-Stream im Render-Loop; externe Regression durch Google-Play-Services-
  for-AR-Updates (Muster wie April 2022, Oktober 2025).
- **Versionen:** SceneView-Compose hat Fixes fuer Camera-Stream-Recreation/stale-stream; ARCore-Regression
  #1752 (arcore-android-sdk). **Externe ARCore-Regressionen sind nicht in SceneView fixbar — Play-Services-
  Version pruefen.**
- **FIX:** `sceneView.resume()` in `onResume` in try/catch kapseln (CameraNotAvailable abfangen);
  `sceneView.onException`-Lambda fuer ARCore-unavailable/Permission setzen; aktuelle SceneView (4.18.x) nutzen
  (Camera-Stream-Fixes); bei geraetespezifischen AR-Crashes ARCore/Play-Services-Version als externe Ursache
  pruefen.
- **Quelle:** https://github.com/sceneview/sceneview ·
  https://github.com/google-ar/arcore-android-sdk/issues/1752 ·
  https://github.com/SceneView/sceneview-android/discussions/181

## 12. Crash beim schnellen Erstellen + Wegwischen (ModelViewer / ViewPager / Liste)

- **Symptom:** Crash, wenn eine 3D-View schnell erstellt und gleich wieder detached/zerstoert wird — typisch in
  `ViewPager`, RecyclerView-Listen oder bei schnellem Hin-/Herwischen; auf Low-End-Geraeten bei schneller
  Interaktion.
- **Ursache:** `ModelViewer` crasht, wenn die verbundene View kurz nach Creation detached wird; Erstellung und
  Zerstoerung laufen gleichzeitig/zu schnell hintereinander.
- **Versionen:** Issues #6933 (ModelViewer quick create+detach), #5543 (ViewPager), #6604 (create+destroy
  gleichzeitig). **Fix-Status: ❓.**
- **FIX:** Creation/Destroy serialisieren (nicht gleichzeitig); Detach nicht unmittelbar nach Create; Loading mit
  Lifecycle-Guard absichern; ggf. Debounce beim Wischen; pro Page nur dann eine Engine-View aufbauen, wenn die
  Page wirklich sichtbar wird, und sauber teardownen bevor die naechste startet (siehe §4).
- **Quelle:** https://github.com/google/filament/issues/6933 ·
  https://github.com/google/filament/issues/5543 · https://github.com/google/filament/issues/6604

## 13. Transparenter Hintergrund + Postprocessing falsch geblendet

- **Symptom:** Bei transparentem Render-Hintergrund (3D ueber UI) blendet das Postprocessing den transparenten
  View nicht korrekt — Hintergrund wird opak/falsch ueberlagert.
- **Ursache:** Der Postprocessing-Pass ignorierte Transparenz (alter Bug).
- **Versionen:** Issue #1165 — laut Best-Practices-Notiz **inzwischen gefixt**; **genaue Fix-Version: pruefen.**
- **FIX:** Auf aktuelle Filament-Version aktualisieren; `UiHelper.isOpaque = false` + `Renderer.ClearOptions`
  (clear=true, discard passend) + korrektes Material-Blending setzen. Nach Update verifizieren, dass Transparenz
  mit aktivem Postprocessing stimmt.
- **Quelle:** https://github.com/google/filament/issues/1165

## 14. Engine in eigenem Thread (HandlerThread) → SIGSEGV

- **Symptom:** Fatal signal 11 (SIGSEGV), wenn die Engine in einem `HandlerThread` betrieben und z.B. schnell
  zwischen Modellen gewechselt wird; SIGSEGV im `FrameCallback`.
- **Ursache:** Engine/Renderer werden ueber Threadgrenzen hinweg inkonsistent bedient; Filament erwartet
  konsistente Thread-Nutzung fuer Engine-Aufrufe und den Render-Loop.
- **Versionen:** Issues #6534 (HandlerThread), #4168 (FrameCallback SIGSEGV). **Fix-Status: ❓.**
- **FIX:** Engine-/Renderer-Aufrufe auf einem konsistenten Thread halten (Render-Loop via `Choreographer` auf dem
  zugehoerigen Thread); Modellwechsel nicht race-conditiongefaehrdet aus einem anderen Thread triggern; State
  synchronisieren bevor Render und Destroy interagieren.
- **Quelle:** https://github.com/google/filament/issues/6534 ·
  https://github.com/google/filament/issues/4168

---

## Fix-Status

| # | Bug | Status | Beleg |
|---|-----|--------|-------|
| 1 | 16-KB-Alignment | **Loesbar** (Config + AGP>=8.5.1 + SceneView>=4.16.8); `filamat-android-lite` ❓ offen (#9460) | Issue #9460, Android Devs |
| 2 | setViewport-Leak | **Gefixt** (RELEASE_NOTES-Eintrag); Best Practice bleibt | Issue #3381 |
| 3 | Mehrere Views/Engines SIGSEGV | **Workaround** (eine Engine teilen); Issue-Status ❓ | #1344/#2364/#7303 |
| 4 | Teardown-Leak / GREF-overflow | **Vermeidbar** (Reihenfolge); TextureHelper-Leak ❓ | #7394/#6936/#4881 |
| 5 | matc version mismatch | **Vermeidbar** (Versionen pinnen) | #4685/#4399 |
| 6 | Schwarzer Screen | **Diagnostizierbar** (Echtgeraet/Logcat) | #4692 |
| 7 | Vulkan langsamer/Crash | **Workaround** (GLES default + matc) | #4225/#7091/#5294/#6444/#8774 |
| 8 | KTX2 schwarz | **Pipeline-/Geraete-Problem**; Filament-Fix ❓ | #4771 + Oekosystem |
| 9 | Dynamic Resolution Spikes | **Offen/❓** (vorsichtig einsetzen) | #1898/#5885 |
| 10 | Transform-Drift | **Gefixt in SceneView 4.17.0** | SceneView Releases |
| 11 | AR Resume / CameraNotAvailable | **Teilweise gefixt** (SceneView-Stream-Fixes); externe ARCore-Regression ❓ | #1752, SceneView |
| 12 | Quick create+detach Crash | **Workaround** (serialisieren); Status ❓ | #6933/#5543/#6604 |
| 13 | Transparenz + Postprocessing | **Gefixt** (Version pruefen) | #1165 |
| 14 | Engine in HandlerThread SIGSEGV | **Workaround** (konsistenter Thread); Status ❓ | #6534/#4168 |

**Noch offen / mit Vorsicht (❓ Status nicht live verifiziert):** #9460 (filamat-android-lite 16-KB),
#1898/#5885 (Dynamic Resolution), Mehrere-Views-Familie (#1344/#2364/#7303), Quick-create-detach
(#6933/#5543/#6604), HandlerThread (#6534/#4168), KTX2-schwarz (#4771 + Oekosystem).

**Ehrlichkeits-Hinweis:** gh-CLI war nicht verfuegbar — der echte open/closed-Status der Issues und exakte
Fix-Versionen (RELEASE_NOTES-Zeilen) konnten nicht 1:1 abgeglichen werden. Alle ❓-Eintraege beim naechsten
Durchlauf gegen die aktuellen google/filament Issues + RELEASE_NOTES.md verifizieren.

---

## Bezug ↔ Best-Practices

(Gegenseite: `best-practices/android/3d-filament-android.md`)

| Bug hier | Verhindert man mit Best-Practice |
|----------|----------------------------------|
| §1 16-KB-Alignment | BP §7/§8 (16-KB-Page-Size, AGP, jniLibs) |
| §2 setViewport-Leak | BP §8 (Viewport nicht jeden Frame) |
| §3 Mehrere Views SIGSEGV | BP §8 (eine Engine, Views/Scenes teilen) |
| §4 Teardown-Leak | BP §6/§8 (saubere Teardown-Reihenfolge, DisposableEffect) |
| §5 matc mismatch | BP §2 (matc-Version == Runtime) |
| §6 Schwarzer Screen | BP §8 (Echtgeraet, UiHelper, Logcat) |
| §7 Vulkan/Adreno | BP §1/§9 (GLES default, Vulkan + Fallback, matc-Optimierung) |
| §8 KTX2 schwarz | BP §5 (glTF/KTX2-Pipeline, gltf-transform) |
| §9 Dynamic Resolution | BP §7 (DR vorsichtig, Speicher monitoren) |
| §10 Transform-Drift | BP §8 (SceneView >= 4.17.0) |
| §11 AR Resume | BP §1/§6 (SceneView aktuell, Lifecycle, onException) |
| §12 Quick create+detach | BP §6/§8 (Lifecycle-Guard, Teardown vor Recreate) |
| §13 Transparenz + Postprocessing | BP §6 (isOpaque=false, ClearOptions, Version) |
| §14 HandlerThread SIGSEGV | BP §6 (Choreographer-Loop, konsistenter Thread) |

---

## Pflicht-Checkliste vor dem Start

- [ ] **16-KB-Alignment** aktiv? `pageAlignSharedLibraries=true`, AGP>=8.5.1, SceneView>=4.16.8, `filamat-android-lite` gemieden.
- [ ] **Eine Engine**-Strategie? Keine mehreren Engines/SwapChains; mehrere Views teilen eine Engine.
- [ ] **Teardown-Reihenfolge** definiert? Ressourcen zuerst, `Engine.destroy()` zuletzt; an Lifecycle gekoppelt.
- [ ] **`setViewport`** nur bei Aenderung, nie unkonditional im Frame-Loop.
- [ ] **matc-Version == Runtime-Version**? Alle filament-Deps gepinnt; `.filamat` nach Upgrade neu kompiliert.
- [ ] **OpenGL ES als Default**? Vulkan nur mit GLES-Fallback; Materialien mit matc optimiert.
- [ ] **Auf Echtgeraet getestet** (Adreno + Mali), nicht nur Emulator? Logcat auf Shader-Fehler gecheckt.
- [ ] **KTX2-Texturen** auf realem Geraet verifiziert (nicht schwarz)?
- [ ] **Dynamic Resolution** Speicher-monitort, Grenzen getestet?
- [ ] **SceneView >= 4.17.0/4.18.0** (Transform-Drift gefixt)?
- [ ] **AR:** `resume()` in try/catch, `onException` gesetzt, ARCore-Play-Services-Version geprueft?
- [ ] **Listen/ViewPager:** Create/Destroy serialisiert, Teardown vor Recreate?
- [ ] **Engine-Thread** konsistent (Render-Loop und Modellwechsel nicht ueber Threadgrenzen mischen)?


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-dotnet-directx-windows](../desktop/3d-dotnet-directx-windows.md)
- [3d-godot](../desktop/3d-godot.md)
- [3d-metal-scenekit-macos](../desktop/3d-metal-scenekit-macos.md)
- [3d-rust-wgpu-bevy](../desktop/3d-rust-wgpu-bevy.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
