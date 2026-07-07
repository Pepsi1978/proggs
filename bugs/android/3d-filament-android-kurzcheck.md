# 3D auf Android (Filament/SceneView) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
