# Bekannte Bugs: 3D auf macOS (Metal/SceneKit/RealityKit)

> **PFLICHT-LESEN vor echter Arbeit.**
> Stand: recherchiert am 2026-06-13 für Metal 4 / macOS 26 / Xcode 26.
> Gegenseite (wie man es richtig macht): best-practices/desktop/3d-metal-scenekit-macos.md (Bezugstabelle unten).

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

---

## USD/USDZ-Import & Assets

## 1. USD Material-Linkage bricht beim Import ("Multiple root level objects") [⭐ HÄUFIG]

**Symptom:** Nach USDZ-Import in Reality Composer Pro fehlen Materialien/Texturen; Warnung "Multiple root level objects exist".
**Ursache:** Das DCC-Tool (Blender/Maya u.a.) legt Materialien/Prims AUSSERHALB des Default-Prim ab. USD referenziert beim Import nur den Default-Prim, dadurch bricht die Material-Linkage und es entstehen mehrere Top-Level-Prims.
**Versionen:** Reality Composer Pro (macOS 14+ bis macOS 26), alle DCC-USD-Exporte ohne Root-Prim.
**FIX:** Beim Export alles unter EINEN Default-/Root-Prim haengen. In Blender den Export-Parameter "Root Prim" auf `/root` setzen — dann liegen alle Prims unter einem gemeinsamen Wurzel-Prim und die Linkage haelt. Funktion bleibt voll erhalten, nur die USD-Hierarchie wird korrekt verschachtelt.
**Quelle:** https://developer.apple.com/forums/thread/745874 , https://developer.apple.com/forums/thread/743515

## 2. USDZ aus RealityKitContentBundle wird komplett schwarz (Texturen verloren) [⭐ HÄUFIG]

**Symptom:** Mesh laedt korrekt, erscheint aber komplett schwarz/ohne Textur, wenn es per `Entity(named:in:)` aus einem RealityKitContentBundle (RCP-Package) geladen wird. Dasselbe USDZ direkt aus dem Main-Bundle per `ModelEntity(named:in:)` sieht korrekt aus.
**Ursache:** Inkonsistenz in der Kette Reality Composer Pro → Xcode → RealityView beim Packaging der Texturen. Tritt sofort beim Laden auf (kein Laufzeit-Speicherproblem), deutet also auf einen Packaging-/Bundle-Bug, nicht auf fehlenden Texturspeicher.
**Versionen:** RealityKit auf macOS/visionOS, RCP-Bundles (aktuelle Xcode-26-Toolchain), Status unklar — kein offizieller Fix belegt.
**FIX:** Workaround — dasselbe Modell per `ModelEntity(named:in:)` aus dem Main-Bundle laden statt `Entity(named:in:)` aus dem RCP-Package. Alternativ: Textur-Budget pruefen und Bug-Report (FB) einreichen. Funktion erhalten (gleiches Modell, anderer Lade-Pfad).
**Quelle:** https://developer.apple.com/forums/thread/764822

## 3. USDZ von Web-URL zur Laufzeit → schwarze Textur

**Symptom:** Ein zur Laufzeit von einer Web-URL geladenes USDZ erscheint mit schwarzer Textur.
**Ursache:** Texturen/Material-Referenzen werden beim Direkt-Streaming nicht zuverlaessig aufgeloest (Aufloesungs-/Lade-Timing-Problem).
**Versionen:** RealityKit (iOS/macOS), Status unklar.
**FIX:** USDZ erst vollstaendig lokal herunterladen/cachen, dann aus dem lokalen Pfad laden — nicht direkt von der Remote-URL in die Szene streamen. Funktion erhalten, nur Lade-Reihenfolge geaendert.
**Quelle:** https://developer.apple.com/forums/thread/751082

## 13. glTF/OBJ direkt in RealityKit laden schlägt fehl

**Symptom:** glTF-/OBJ-Datei laedt nicht in RealityKit/RCP.
**Ursache:** RealityKit-Laufzeit erwartet USD(Z) (USDZ/USDA/USDC); glTF/OBJ/FBX werden zur Laufzeit nicht direkt unterstuetzt.
**Versionen:** RealityKit, alle Versionen.
**FIX:** Vorab konvertieren — Reality Converter (Drag-and-Drop) oder `usdzconvert` / USD Python API im Build-Skript. Fuer eigene Metal-Pipelines stattdessen Model I/O (`MDLAsset`) nutzen, das OBJ/USD/Alembic direkt laedt. Funktion erhalten (gleiches Modell, anderes Format).
**Quelle:** https://developer.apple.com/forums/thread/745874 , https://developer.apple.com/documentation/realitykit

## Beleuchtung, Schatten & Interaktion (RealityKit)

## 4. IBL ohne Wirkung — Receiver vergessen [⭐ HÄUFIG]

**Symptom:** Image-Based Light gesetzt, aendert aber an der Szene nichts.
**Ursache:** Nur die `ImageBasedLightComponent` (HDR-Textur) an die Light-Entity gesetzt, aber der `ImageBasedLightReceiverComponent` an den beleuchteten Entities vergessen. Fehlt der Receiver, passiert nichts.
**Versionen:** RealityKit, alle aktuellen.
**FIX:** BEIDE Komponenten setzen — `ImageBasedLightComponent` an die Light-Entity, `ImageBasedLightReceiverComponent` an jede beleuchtete Entity (der Receiver verweist auf die Light-Entity). Funktion erhalten.
**Quelle:** https://developer.apple.com/forums/thread/819332 , https://developer.apple.com/documentation/realitykit

## 5. Tap/Gesten auf Entity feuern nie — fehlende CollisionComponent (macOS) [⭐ HÄUFIG]

**Symptom:** Tap-/Drag-Gesten auf eine Entity loesen nichts aus. Auf macOS funktioniert `MagnifyGesture` selbst mit korrekten Komponenten gar nicht.
**Ursache:** Hat eine Entity `InputTargetComponent` (oder `ManipulationComponent`) aber KEINE `CollisionComponent`, feuern Gesten nie — die Collision-Shape ist die Hit-Test-Grenze. `MagnifyGesture` hat zusaetzlich ein eigenes macOS-spezifisches Limit.
**Versionen:** RealityKit auf macOS (RealityView). `MagnifyGesture`-Problem macOS-spezifisch, Status unklar.
**FIX:** Beide Komponenten setzen — `CollisionComponent` (passende Shape, in RCP generierbar) UND `InputTargetComponent`. Auf macOS fuer Zoom statt `MagnifyGesture` ein eigenes Scroll-/Trackpad-Handling verdrahten. Funktion erhalten.
**Quelle:** https://developer.apple.com/forums/thread/756414 , https://developer.apple.com/forums/thread/778679

## 6. ARView verschluckt Maus-Events auf macOS [⭐ HÄUFIG]

**Symptom:** SwiftUI-Gesten / Maus-Interaktion am 3D-View funktionieren nicht.
**Ursache:** `ARView` faengt auf macOS Maus-Events ab, bevor SwiftUI sie sieht.
**Versionen:** RealityKit `ARView` auf macOS.
**FIX:** Auf `RealityView` migrieren (empfohlener Einstieg ab WWDC24; non-AR: Kamera auf "virtual"). Funktion erhalten — gleiche Szene, moderner View-Typ ohne Event-Klau.
**Quelle:** Apple WWDC24 Session 10103, https://developer.apple.com/videos/play/wwdc2024/10103/

## 7. Directional-Light-Schatten fehlen oder flackern

**Symptom:** Schatten erscheinen gar nicht, oder dynamische Schatten flackern stark; Schattenkanten rasiermesserscharf, kaum justierbar.
**Ursache:** `DirectionalLightComponent.Shadow` hat per Default `maximumDistance = 5` und `depthBias = 1`. Schatten werden NUR gezeigt, wenn der Abstand Caster→Receiver <= `maximumDistance` ist — bei groesseren Szenen fehlen sie deshalb. Zusaetzlich bekannter Flacker-Bug bei dynamischen Schatten.
**Versionen:** RealityKit (iOS/macOS/visionOS), Flacker-Bug Status unklar.
**FIX:** `maximumDistance` an die Szenengroesse anpassen (erhoehen), `depthBias` tunen. Gegen Flackern: Caster-Bewegung/Update glaetten, stabilere Shadow-Distance setzen. Funktion erhalten.
**Quelle:** https://developer.apple.com/forums/thread/748121 , https://www.hackingwithswift.com/forums/swiftui/realitykit-incorrect-shadows-with-directional-light/25811

## Farbe, Gamma, EDR/HDR

## 8. sRGB-Doppel-Gamma / ausgewaschenes Bild im MTKView [⭐ HÄUFIG]

**Symptom:** Bild wirkt flau/ausgewaschen (zu hell) oder, je nach Pipeline, zu dunkel.
**Ursache:** Gamma wird doppelt angewandt — z. B. sRGB-`colorPixelFormat` der View setzt automatisch die sRGB-Encode-Stufe, gleichzeitig wird im Shader noch manuell gamma-korrigiert. Bei `rgba16Float`/extended-linear gilt das umgekehrt: extended-linear-sRGB als Colorspace ist nicht dasselbe wie linearSRGB, was zu falsch angewandtem Gamma fuehrt.
**Versionen:** Metal/MTKView, plattformuebergreifend, alle Versionen.
**FIX:** EINE klare Konvention — intern in linearem (Float) Raum rendern, Gamma/Encode genau EINMAL am Ende. Entweder sRGB-PixelFormat nutzen (Hardware encodet) ODER manuell encoden, nie beides. Albedo-Texturen als sRGB samplen, Normal/Roughness/Metallic linear. Funktion erhalten, nur Color-Pipeline begradigt.
**Quelle:** https://developer.apple.com/forums/thread/724223 , https://developer.apple.com/forums/thread/746101

## 8b. Normal-Map faelschlich als sRGB interpretiert

**Symptom:** Oberflaechendetails / Beleuchtung sehen falsch aus, Normal-Map wirkt verwaschen.
**Ursache:** Normal-Maps sind bereits linear (Gamma 1.0); werden sie als sRGB geladen, wird faelschlich Gamma korrigiert. In USD sind Normal-Maps linear zu uebergeben.
**Versionen:** RealityKit/Metal Asset-Pipelines, alle Versionen.
**FIX:** Normal/Roughness/Metallic/AO als LINEAR laden, nur Albedo/Emissive als sRGB. In USD die Color-Space-Hints korrekt setzen. Funktion erhalten.
**Quelle:** https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects/chapter-24-importance-being-linear

## 9. Tahoe EDR / Auto-Brightness-Bug (Helligkeit springt, Studio-Display-Flicker) [⭐ HÄUFIG]

**Symptom:** Helligkeit/Kontrast springt unerwartet, wenn das Display im EDR-Modus ist und Auto-Brightness an ist; manuelles Helligkeitsaendern bei aktivem Auto-Brightness triggert Spruenge. Separat: Studio Display flackert im P3-600-Nits-Profil.
**Ursache:** Bei aktivem Auto-Brightness + Color-Table-Adjustment filtert macOS 26 die GESAMTE EDR-Range+Gamma (SDR+HDR) durch die modifizierte Color-Table, statt nur die SDR-Luminanz — daher die Spruenge "ueber Weiss". Der Studio-Display-Flicker haengt an der Behandlung des hochhelligen P3-600-Nits-Farbraums (HDR-/Gamut-Management).
**Versionen:** macOS 26.0–26.x (Tahoe), v. a. XDR/P3-1600- und Studio-Display-P3-600-Presets.
**FIX (funktionserhaltend):** Auto-Brightness im EDR-Modus deaktivieren (System Settings > Displays). Studio Display: Profil von P3-600 auf P3-D65 umstellen (kostet etwas Helligkeit/Color-Range, behebt aber das Flackern). Entwicklerseitig: EDR-Headroom (`maximumExtendedDynamicRangeColorComponentValue`) dynamisch regelmaessig neu lesen, statt einmal zu cachen, damit Highlights korrekt skalieren statt zu clippen.
**Quelle:** https://github.com/waydabber/BetterDisplay/discussions/5146 , https://github.com/waydabber/BetterDisplay/issues/5234 , https://apple.gadgethacks.com/news/macos-tahoe-studio-display-flickering-bug-plagues-users/ , https://www.macrumors.com/2025/12/18/macos-tahoe-studio-display-flickering/

## 16. EDR vergessen → flaues HDR

**Symptom:** HDR-Inhalt sieht auf XDR-/Pro-Display nicht "leuchtend" aus, Highlights clippen bei Weiss.
**Ursache:** EDR-Setup auf dem Layer fehlt — `wantsExtendedDynamicRangeContent`, `rgba16Float`-PixelFormat und extended-linear-Colorspace nicht gesetzt.
**Versionen:** Metal/CAMetalLayer auf macOS, alle Versionen.
**FIX:** EDR aktivieren: `metalLayer.wantsExtendedDynamicRangeContent = true`, PixelFormat `.rgba16Float`, Colorspace `kCGColorSpaceExtendedLinearDisplayP3`; Headroom abfragen und Highlights ueber 1.0 skalieren. Funktion erhalten, nur HDR-Ausgabe aktiviert.
**Quelle:** https://developer.apple.com/forums/thread/724223

## MetalFX & Metal-Laufzeit

## 10. MetalFX Temporal-Upscaler: Ghosting / Flackern / Schmieren [⭐ HÄUFIG]

**Symptom:** Bewegte Objektkanten schmieren (Ghosting), Bild flackert; bei hohem Upscaling-Faktor blenden Partikel/Transparenzen mit dem Hintergrund.
**Ursache:** (a) Falsche/fehlende Motion-Vectors → History-Samples passen nicht zum aktuellen Pixel. (b) Falscher Exposure-Wert am Upscaler → Flicker und Ghosting. (c) Fehlender Jitter / TAA-Jitter falsch verdrahtet. (d) Niedrige Input-Aufloesung + hoher Faktor bei Partikeln.
**Versionen:** MetalFX Temporal/Denoised Upscaler, alle Versionen.
**FIX:** Korrekte per-Pixel Motion-Vectors uebergeben (auch fuer prozedurale Verschiebungen), den korrekten Exposure-Wert pro Frame setzen, Sub-Pixel-Jitter wie gefordert verdrahten, und Tonemapping/Exposure VOR der Frame-Interpolation anwenden. Funktion erhalten, nur korrekte Inputs.
**Quelle:** https://developer.apple.com/documentation/metalfx/applying-temporal-antialiasing-and-upscaling-using-metalfx , https://developer.apple.com/videos/play/wwdc2022/10103/

## 11. `nextDrawable()` gibt nil / Crash / Main-Thread-Hang [⭐ HÄUFIG]

**Symptom:** `[CAMetalLayer nextDrawable]` liefert nil oder crasht (bad access); App haengt auf dem Main-Thread.
**Ursache:** (a) `setAllowsNextDrawableTimeout = NO` + begrenzter Drawable-Pool → `nextDrawable()` blockiert unbegrenzt, GPU-Pipeline-Stall/Deadlock. (b) Alle Drawables in Benutzung → nach ~1 s Timeout kommt nil (Drawable-Erschoepfung). (c) Device nil → nil-Drawable.
**Versionen:** CAMetalLayer/Metal auf macOS, alle Versionen.
**FIX:** `setAllowsNextDrawableTimeout` NICHT auf NO setzen; Drawable so frueh wie moeglich praesentieren/freigeben; nicht mehr als ~3 Frames in-flight halten (Semaphore); nil-Drawable defensiv abfangen (Frame ueberspringen statt force-unwrap). Funktion erhalten, nur Drawable-Lifecycle diszipliniert.
**Quelle:** https://github.com/zed-industries/zed/issues/53390 , https://developer.apple.com/forums/thread/100055 , https://developer.apple.com/forums/thread/791878

## 12. Multi-Display → Frame-Stutter

**Symptom:** Frame-Stuttering, sobald mehrere Displays am Mac haengen; mit nur einem Display verschwindet es.
**Ursache:** `CAMetalLayer.nextDrawable` synchronisiert bei mehreren angeschlossenen Displays ungünstig — bekannter macOS-Bug.
**Versionen:** macOS Multi-Display-Setups, Status unklar (langjaehriges Forum-Thema).
**FIX (Workaround):** Wenn moeglich Rendering an EIN Display binden; Display-Sync/VSync-Verhalten pruefen; Drawable-Pool und Frame-Pacing tunen. Kein voll funktionserhaltender OS-Fix bekannt — Workaround mildert.
**Quelle:** https://developer.apple.com/forums/thread/112468

## Framework-Wahl & Speicher

## 14. SceneKit für Neuprojekt waehlen (soft-deprecated)

**Symptom:** "Falle" im Sinne technischer Schuld — Neues auf SceneKit gebaut, keine neuen Features, nur Critical-Fixes.
**Ursache:** SceneKit ist seit WWDC25 soft-deprecated (Wartungsmodus). Zusaetzlich bekanntes Altproblem: erster PBR-Render mit `lightingEnvironment` ist spuerbar langsam (Shader-Kompilierung/IBL-Vorberechnung beim ersten Frame).
**Versionen:** SceneKit, alle; Soft-Deprecation ab macOS 26.
**FIX:** Neuprojekte auf RealityKit (Szenengraph + SwiftUI) oder Metal 4 (eigener Renderer). Bestands-SceneKit laeuft weiter, aber Migrationspfad (WWDC25 Session 288) einplanen. Bei SceneKit-Bestand: ersten Render "aufwaermen" (IBL/Material vorab laden), um den langsamen First-Render zu verstecken. Funktion erhalten.
**Quelle:** Apple WWDC25 Session 288, https://developer.apple.com/forums/thread/74258

## 15. Speicher laeuft voll / Crash bei grossen Modellen & dynamischen Meshes

**Symptom:** App crasht beim Laden grosser USDZ-Modelle; bei dynamischen Mesh-Updates steigt der Speicher kontinuierlich (>5 GB) bis zum Crash.
**Ursache:** (a) Grosse Modelle: ~40 MB Datei kann ~800 MB Texturspeicher bedeuten. (b) `MeshResource`-Memory-Leak bei wiederholten dynamischen Updates. (c) Completion-Closure haelt starke Referenz (Leak). (d) Loader auf Main-Thread blockiert/crasht.
**Versionen:** RealityKit (iOS/macOS), Leak Status unklar.
**FIX:** Async laden (`loadModelAsync` / Combine bzw. async-await), schwere Arbeit off-main, Ergebnis dann auf Main-Thread anwenden; in Completion-Handlern `[weak self]`; Textur-Budget reduzieren (Aufloesung/Kompression); dynamische `MeshResource` wiederverwenden statt pro Frame neu allokieren. Funktion erhalten.
**Quelle:** https://developer.apple.com/forums/thread/701924 , https://developer.apple.com/forums/thread/710657 , https://developer.apple.com/documentation/realitykit/entity/loadmodelasync(named:in:)

---

## Fix-Status

| Bug | gefixt ab Version | Beleg |
|-----|-------------------|-------|
| §9 Image-Adjustment bei Auto-Brightness (Beta-Variante) | macOS 26 dev beta 5 (OS-Level, vor Release) | BetterDisplay Issue #4466 (Titel/Verlauf: "issue was resolved at OS level") |
| §6 ARView Maus-Events | RealityView seit WWDC24 verfuegbar (Migration = Fix) | Apple WWDC24 Session 10103 |
| §13 glTF/OBJ Import | kein OS-Fix — by design (USD-Konvertierung) | Apple RealityKit-Doku |

**Noch NICHT gefixt (Workaround aktiv):**
- §9 EDR-Brightness-Shift bei manueller Helligkeit + Auto-Brightness (BetterDisplay #5146/#5234) — offen, Workaround: Auto-Brightness aus.
- §9 Studio-Display-Flicker P3-600-Nits — offen, Apple hat es nicht anerkannt; Workaround: Profil P3-D65 (macOS 26.1/26.2 fixen es nicht).
- §1 USD Material-Linkage — kein Framework-Fix; DCC-seitiger Export-Workaround (Root-Prim).
- §2 USDZ schwarz aus RCP-Bundle — Status unklar, Workaround: Main-Bundle-Ladepfad.
- §3 USDZ-Web-URL schwarz — Status unklar, Workaround: lokal cachen.
- §5 MagnifyGesture auf macOS — Status unklar, Workaround: eigenes Handling.
- §7 dynamische Schatten flackern — Status unklar, Workaround: Distance/Bias tunen.
- §8/§8b sRGB-Doppel-Gamma / Normal-Map-sRGB — kein Bug, sondern Anwendungsfehler; Fix = korrekte Pipeline.
- §10 MetalFX-Ghosting — kein Bug, sondern fehlerhafte Inputs; Fix = korrekte Motion-Vectors/Exposure.
- §11 nextDrawable-Hang/nil — Verhalten by design; Fix = Drawable-Lifecycle-Disziplin.
- §12 Multi-Display-Stutter — offen (langjaehrig), Workaround: ein Display / Frame-Pacing.
- §15 MeshResource-Leak / Large-Model-Crash — Leak-Status unklar, Workaround: Async + [weak self] + Budget.

**Ehrlichkeits-Hinweis:** `gh`-CLI war nicht verfuegbar; Fix-Status stuetzt sich auf oeffentliche Changelogs, Apple-Forum-/Doku-Stand und Issue-Tracker-Verlaeufe. Wo kein offizieller Changelog-Beleg existiert, ist der Status bewusst als "unklar" markiert statt ungesichert "gefixt" zu behaupten.

---

## Bezug ↔ Best-Practices

| Bug-Abschnitt §N | Best-Practice-Abschnitt |
|------------------|-------------------------|
| §1, §2, §3, §13 USD/USDZ-Import & Assets | §8 Asset-Pipeline (USDZ/glTF/Model I/O) |
| §4 IBL Receiver | §4 Beleuchtung & IBL |
| §5 Collision/InputTarget, §6 ARView→RealityView | §2 RealityKit auf macOS (RealityView, Migration), §9 Fallen |
| §7 Directional-Light-Schatten | §4 Beleuchtung & IBL |
| §8, §8b, §16 Gamma/EDR/Color | §7 Color Management — Display P3, EDR/HDR |
| §9 Tahoe-EDR/Auto-Brightness/Studio-Display | §7 Color Management + §9 Fallen |
| §10 MetalFX | §5 Postprocessing & MetalFX |
| §11, §12 Drawable/Multi-Display, §15 Performance/Speicher | §6 Performance (Metal & RealityKit) |
| §3 PBR Materialien (Normal-Map) | §3 PBR & Materialien |
| §14 SceneKit soft-deprecated | §1 Framework-Wahl |

---

## Pflicht-Checkliste vor dem Start

- [ ] USD/USDZ aus EINEM Root-/Default-Prim exportiert (Material-Linkage haelt)? (§1)
- [ ] USDZ-Ladepfad gewaehlt: Main-Bundle `ModelEntity` statt RCP-Bundle `Entity`, falls schwarz? (§2)
- [ ] glTF/OBJ vorab in USDZ konvertiert? (§13)
- [ ] IBL: `ImageBasedLightComponent` UND `ImageBasedLightReceiverComponent` gesetzt? (§4)
- [ ] Interaktive Entities haben `CollisionComponent` + `InputTargetComponent`? (§5)
- [ ] `RealityView` statt `ARView` (keine geklauten Maus-Events)? (§6)
- [ ] Directional-Light `maximumDistance` an Szenengroesse angepasst? (§7)
- [ ] Color-Pipeline: linear rendern, Gamma genau einmal encoden, Normal-Maps linear? (§8/§8b)
- [ ] EDR aktiviert (wantsEDR + rgba16Float + extended-linear), Headroom dynamisch gelesen? (§16/§9)
- [ ] MetalFX: korrekte Motion-Vectors, Exposure, Jitter uebergeben? (§10)
- [ ] Drawable-Lifecycle diszipliniert (kein Timeout-NO, <=3 in-flight, nil abfangen)? (§11)
- [ ] Grosse Modelle async off-main geladen, `[weak self]`, Textur-Budget geprueft? (§15)
- [ ] Neuprojekt NICHT auf SceneKit (soft-deprecated)? (§14)


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-visual-quality](../assets/3d-visual-quality.md)
- [3d-dotnet-directx-windows](3d-dotnet-directx-windows.md)
- [3d-godot](3d-godot.md)
- [3d-rust-wgpu-bevy](3d-rust-wgpu-bevy.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
