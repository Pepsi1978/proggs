# 3D auf macOS (Metal/SceneKit/RealityKit) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | USDZ-Import: "Multiple root level objects", Texturen fehlen | DCC-Export mit EINEM Root-/Default-Prim (`/root`); alles unter einen Prim haengen | §1 |
| 2 | USDZ aus RealityKitContentBundle laedt als rein schwarz, ohne Texturen | Nicht `Entity(named:in:)` aus dem RCP-Bundle — `ModelEntity(named:in:)` aus dem Main-Bundle; bzw. Packaging pruefen | §2 |
| 3 | USDZ aus Web-URL zur Laufzeit → schwarze Textur | Asset lokal cachen / mitliefern statt direkt von URL streamen; Material/Textur-Aufl-Pfad pruefen | §3 |
| 4 | Image-Based Light wirkt nicht | `ImageBasedLightComponent` UND `ImageBasedLightReceiverComponent` setzen — beide! | §4 |
| 5 | Tap/Klick auf Entity feuert nie (macOS) | Entity braucht BEIDES: `CollisionComponent` + `InputTargetComponent`. `MagnifyGesture` ist auf macOS kaputt | §5 |
| 6 | Maus-/Gesten gehen am 3D-View verloren | Weg von `ARView` → `RealityView` migrieren (ARView verschluckt Maus-Events) | §6 |
| 7 | Schatten fehlen / flackern (Directional Light) | Default `maximumDistance = 5` zu klein → erhoehen; flackernde dynamische Schatten = bekannter Bug | §7 |
| 8 | Bild flau/ausgewaschen oder zu dunkel im MTKView | sRGB-Doppel-Gamma: NICHT zugleich sRGB-PixelFormat UND manuelles Gamma; linear rendern, einmal encoden | §8 |
| 9 | HDR/EDR springt, Helligkeit zuckt (macOS 26) | Auto-Brightness im EDR-Modus aus; Studio Display: Profil P3-600 → P3-D65; Headroom dynamisch neu lesen | §9 |
| 10 | MetalFX-Upscaler: Ghosting / Flackern / Schmieren | Korrekte Motion-Vectors + Exposure uebergeben; falscher Exposure-Wert = Flicker/Ghosting; Jitter mitliefern | §10 |
| 11 | `nextDrawable()` gibt nil / Crash / Main-Thread-Hang | `setAllowsNextDrawableTimeout` nicht auf NO; Drawable schnell freigeben; nicht mehr als 3 in-flight | §11 |
| 12 | Multi-Display → Frame-Stutter | Bekannter macOS-Bug bei mehreren Displays auf einem CAMetalLayer; ggf. auf ein Display beschraenken | §12 |
| 13 | glTF/OBJ laedt nicht in RealityKit | Laufzeit will USD(Z): vorher konvertieren (Reality Converter / `usdzconvert`) | §13 |
| 14 | Neuprojekt auf SceneKit gestartet | SceneKit ist soft-deprecated (WWDC25), nur Critical-Fixes — RealityKit/Metal 4 waehlen | §14 |
| 15 | RAM laeuft voll / Crash bei grossen Modellen oder dynamischen Meshes | `MeshResource`-Leak + Async-Loading off-main; `[weak self]` in Completion; Textur-Budget pruefen | §15 |
