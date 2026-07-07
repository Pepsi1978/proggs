# Bekannte Bugs: Visuelle Qualität für 3D (PBR/Licht/PostFX/Assets)

> **PFLICHT-LESEN vor jeder 3D-Render-/Asset-Arbeit.** Stand recherchiert 2026-06-13, engine-übergreifend (three.js, Babylon.js, Filament, Unity, Unreal, Godot, model-viewer, SceneKit/RealityKit). Die meisten Einträge sind **„per Design" / zeitlos** (Physik & Color-Science ändern sich nicht mit der Version) — sie sind dann so markiert. Tool-/spec-abhängige Fälle sind getrennt im Abschnitt **Fix-Status** belegt.
>
> Dies ist die Bug-/Fallen-Seite. Die Gegenseite (das „Wie macht man es richtig") liegt in `best-practices/assets/3d-visual-quality.md`. Querverweise unten unter **Bezug ↔ Best-Practices**.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Normal Map wirkt „eingedellt statt gewölbt", Beleuchtung kippt | Y-Konvention prüfen: glTF/OpenGL = Y+, DirectX/Unreal = Y−. Falsch → grünen Kanal invertieren | §1 |
| 2 | Bild verwaschen/überstrahlt, Lichter „leuchten zu schnell aus" | Kein linearer Workflow. In linear rendern, sRGB nur am Output. Color-Maps sRGB, Daten-Maps (roughness/metal/normal) NIE als sRGB dekodieren | §2 |
| 3 | Highlights brennen weiß aus, Farben kippen ins Bunte | Tonemapping fehlt → hartes Clamp bei 1.0. PBR Neutral (Produkt) / ACES / AgX aktivieren | §3 |
| 4 | baseColor wird nicht reines Weiß (255) | Kein Bug — Tonemapping reserviert Highlight-Headroom. Mit PBR Neutral bleibt baseColor bis sRGB 231 1:1 | §4 |
| 5 | Schattenkanten flimmern beim Kamerafahren (Shimmering) | CSM-Frustum wird pro Frame neu berechnet. Statische Cascade-Intervalle + Texel-Snapping | §5 |
| 6 | Schatten-Streifen auf Flächen (Shadow Acne) | Depth-Bias fehlt/zu klein. Slope-Scaled Bias + ggf. Front-Face-Culling beim Depth-Pass | §6 |
| 7 | Schatten „löst sich vom Fuß" (Peter Panning) | Bias zu groß. Bias reduzieren, Front-Face-Culling statt riesigem Bias, dünne Geo vermeiden | §6 |
| 8 | Treppchen/Flimmern trotz MSAA in Deferred-Pipeline | MSAA ist inkompatibel mit Deferred. Auf TAA wechseln | §7 |
| 9 | TAA: Geisterbilder/Schlieren hinter bewegten Objekten | Fehlende/falsche Motion-Vektoren oder keine History-Rejection. Variance-Clipping + Depth-Reject | §8 |
| 10 | SSR-Reflexe fehlen an Bildrändern / hinter Objekten | SSR sieht nur Screen-Inhalt. IBL als Fallback kombinieren | §9 |
| 11 | `gltf-transform --texture-compress webp` → „Invalid JPG, marker table corrupted" | Tool-Bug bei WebP-Pfad. KTX2 statt WebP nutzen | §10 |
| 12 | KTX2-Normal-Map sieht falsch beleuchtet/zu dunkel aus | Als sRGB statt linear getaggt. `--assign_oetf linear --assign_primaries none` beim Encoden | §11 |
| 13 | metallic-Mittelwerte, baseColor mit eingebackenen Schatten | Häufigster Anfängerfehler → sofort „billig". metallic 0 oder 1; baseColor ohne Licht/AO | §12 |
| 14 | Emissive Material leuchtet nicht / kein Bloom | emissiveFactor clamped auf [0,1]. KHR_materials_emissive_strength > 1 setzen, Bloom in HDR/linear | §13 |
| 15 | Bloom sieht falsch hell / Partikel/Additiv falsch | Effekt läuft nach Tonemapping statt davor. Bloom muss in HDR/linear VOR Tonemapping | §13 |
| 16 | UV-Naht-Kanten falsch beleuchtet | Renderer-Tangenten ≠ Baker-Tangenten. MikkTSpace beidseitig erzwingen | §1 |

---

## 1. Normal Map „eingedellt statt gewölbt" — falsche Y-Konvention [⭐ HÄUFIG]

- **Symptom:** Oberflächendetails wirken umgekehrt (Vertiefungen erscheinen als Erhebungen), Beleuchtung kippt, besonders auffällig bei Schräglicht. Zusätzlich: an UV-Naht-Kanten falsche Beleuchtung.
- **Ursache:** Tangent-Space-Normal-Maps kodieren die Y-Achse (grüner Kanal) je nach Konvention gegensätzlich. **OpenGL/glTF/Blender/Maya/Unity/Marmoset = Y+** (grün hell = nach oben), **DirectX/Unreal/3ds Max/CryEngine/Source = Y−** (invertiert). Eine für die falsche Engine gebackene Map dreht das Relief um. Separat: Wenn der Renderer eine andere Tangentenbasis nutzt als der Baker, stimmt die Beleuchtung an UV-Nähten nicht.
- **Versionen:** Zeitlos / per Design — Eigenschaft der jeweiligen Engine-Konvention, kein Bug, der „gefixt" wird.
- **FIX:** Grünen Kanal der Normal-Map invertieren (oder beim Bake die Ziel-Konvention wählen). Tangenten beidseitig über **MikkTSpace** erzeugen (von glTF vorgeschrieben). Normal-Map IMMER linear/raw, nie als sRGB dekodieren (siehe §2).
- **Quelle:** [polycount Wiki – Normal Map Technical Details](http://wiki.polycount.com/wiki/Normal_Map_Technical_Details); [Maya – Troubleshoot Normal Mapping (Autodesk)](https://help.autodesk.com/view/MAYAUL/2022/ENU/?guid=GUID-CEFA09DB-8D7A-4059-8434-551387A88E8C); [Khronos glTF 2.0 Spec](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html)

---

## 2. Beleuchtung verwaschen/überstrahlt — kein linearer Workflow [⭐ HÄUFIG]

- **Symptom:** Beleuchtung wirkt flach/überstrahlt, Lichter „leuchten zu schnell aus", Farbverläufe falsch, additives Blending (Bloom/Partikel) zu hell. Oft begleitet von doppelt-dunklen oder doppelt-hellen Bereichen.
- **Ursache:** Licht addiert sich physikalisch **linear**, Bildschirme sind aber **sRGB-gamma-kodiert**. Wird die Beleuchtungsmathematik fälschlich im sRGB-Raum gerechnet (oder Color-Maps nicht von sRGB nach linear dekodiert), stimmt die ganze Licht-Energie nicht. Der klassische Zwilling: **Daten-Maps (roughness/metallic/normal/height/AO) werden fälschlich als sRGB dekodiert** — die dürfen NIEMALS gamma-korrigiert werden.
- **Versionen:** Zeitlos / per Design. **Tool-Hinweis:** three.js hat ab **r152** Color-Management standardmäßig aktiviert (`renderer.outputColorSpace = SRGBColorSpace`, früher `outputEncoding`). Migrationsfalle: alte Projekte, die vor r152 manuell „gefixt" hatten, sind nach Upgrade doppelt korrigiert → zu dunkel/zu hell.
- **FIX:** In linear rendern, sRGB-View-Transform nur am Output. Reihenfolge: sRGB-Decode (Input) → linear rendern → Tonemapping → sRGB-Encode (Output). Color/Albedo/Emissive = sRGB taggen; roughness/metallic/normal/AO/height = linear/raw. In three.js ≥ r152 Color-Management nicht doppelt anwenden.
- **Quelle:** [three.js – Color Management](https://threejs.org/manual/en/color-management.html); [three.js r152 Color-Management-Roadmap (Issue #23614)](https://github.com/mrdoob/three.js/issues/23614); [Unity Manual – Linear or gamma workflow](https://docs.unity3d.com/Manual/LinearRendering-LinearOrGammaWorkflow.html)

---

## 3. Highlights ausgefressen, Farben kippen — Tonemapping aus [⭐ HÄUFIG]

- **Symptom:** Glänzende Objekte haben weiße, detaillose Highlight-Flächen; bei kräftigen Farben (Gelb, Grün) entstehen Hue-Skews; das Bild verliert sein 3D-Gefühl und wirkt „billig".
- **Ursache:** Es gibt **kein** „kein Tonemapping". Ein PBR-Render erzeugt HDR-Werte > 1.0; ohne Tonemapping **clamped** der Encoder hart bei 1.0 → stückweise-lineare Funktion mit scharfen Knicken. Das ist DER Hauptgrund, warum naive Renderings billig aussehen.
- **Versionen:** Zeitlos / per Design (HDR→SDR-Mapping ist physikalisch nötig). **Tool-Hinweis:** model-viewer hat ab **v4.0** PBR Neutral als Default-Tonemapper (vorher ACES).
- **FIX:** Tonemapping aktivieren. **Produkt/E-Commerce/Farbtreue → Khronos PBR Neutral**; **Film/Game/Stimmung → ACES oder AgX**. Tonemapping passiert in linear, VOR dem sRGB-Encode. Achtung: ACES/AgX verlieren Saturation (kräftige Gelb/Grün-Töne unerreichbar) — wenn Markenfarbe wichtig ist, PBR Neutral.
- **Quelle:** [Khronos PBR Neutral Tone Mapping (model-viewer)](https://modelviewer.dev/examples/tone-mapping); [google/model-viewer Releases (v4.0 Default-Wechsel)](https://github.com/google/model-viewer/releases)

---

## 4. baseColor wird nicht reines Weiß (255) [⭐ HÄUFIG — KEIN Bug]

- **Symptom:** Ein als reines Weiß (oder gesättigte Markenfarbe) definiertes Material erreicht im Render nicht den vollen sRGB-Wert; Künstler/Kunde meldet „die Farbe stimmt nicht".
- **Ursache:** **Kein Bug, sondern korrekte Physik.** Tonemapping muss Headroom für Highlights reservieren — ein Paper-White-Material darf unter diffusem Licht nicht 255 erreichen, sonst gibt es keinen Spielraum für glänzende Reflexe und die Kugel verliert ihr 3D-Gefühl.
- **Versionen:** Zeitlos / per Design.
- **FIX:** Wenn echte Farbtreue gefordert ist: **Khronos PBR Neutral** + Markenfarbe als baseColor + neutrale (graue) Lichtumgebung. PBR Neutral lässt baseColor bis linear 0.8 ≙ **sRGB 231** exakt 1:1 durch; darüber `1/x`-Kompression (255 → 243) mit „Path to White"-Desaturierung. Dem Kunden erklären, dass volles 255 unter Beleuchtung physikalisch nicht stimmig wäre.
- **Quelle:** [model-viewer – Color Accuracy](https://modelviewer.dev/examples/color); [Khronos PBR Neutral Tone Mapping](https://modelviewer.dev/examples/tone-mapping)

---

## 5. Schattenkanten flimmern beim Kamerafahren (Shimmering) [⭐ HÄUFIG]

- **Symptom:** Schattenkanten „kribbeln"/flackern, sobald die Kamera sich bewegt — besonders bei Cascaded Shadow Maps in Außenszenen.
- **Ursache:** Das CSM-Frustum (orthographische Projektionsgrenzen je Cascade) wird pro Frame neu an die Kamera angepasst. Dadurch springen die Shadow-Map-Texel von Frame zu Frame → flimmernde Kanten.
- **Versionen:** Zeitlos / per Design (Eigenschaft der Shadow-Map-Diskretisierung).
- **FIX:** **Statische Cascade-Intervalle** pro Szenario (nicht pro Frame neu berechnen). **Texel-Snapping:** orthographische Projektionsgrenzen in ganzen Pixel-/Texel-Schritten verschieben → die Schattenprojektion „rastet" und flimmert nicht. Zusätzlich PCF/VSM für weiche Kanten.
- **Quelle:** [Microsoft Learn – Cascaded Shadow Maps](https://learn.microsoft.com/en-us/windows/win32/dxtecharts/cascaded-shadow-maps); [Babylon.js – Cascaded Shadow Maps](https://doc.babylonjs.com/features/featuresDeepDive/lights/shadows_csm)

---

## 6. Shadow Acne & Peter Panning — der Bias-Catch-22 [⭐ HÄUFIG]

- **Symptom:** **Shadow Acne** = streifige/moiréartige Selbstverschattung auf eigentlich beleuchteten Flächen. **Peter Panning** = der Schatten „löst sich" sichtbar vom Fuß des Objekts und schwebt versetzt.
- **Ursache:** Klassischer Zielkonflikt. Acne entsteht, wenn Tiefe in Light-Space und View-Space so nah beieinander liegen, dass Float-Fehler den Tiefentest falsch bestehen lassen. Der übliche Fix ist ein **Depth-Bias** — ist der aber zu groß, detachiert der Schatten → Peter Panning.
- **Versionen:** Zeitlos / per Design (Float-Präzision der Shadow Map).
- **FIX:** **Slope-Scaled Bias** (Bias abhängig vom Winkel zwischen Oberfläche und Licht) statt konstantem großen Bias. **Front-Face-Culling beim Depth-Pass** löst Peter Panning für solide Objekte, ohne großen Bias zu brauchen. Sehr dünne Geometrie vermeiden (Wände mit Dicke modellieren). Normal-Offset-Bias als Ergänzung.
- **Quelle:** [LearnOpenGL – Shadow Mapping](https://learnopengl.com/Advanced-Lighting/Shadows/Shadow-Mapping); [WillP GFX – Dealing with Shadow Map Artifacts](https://willpgfx.com/2015/05/dealing-with-shadow-map-artifacts/)

---

## 7. Aliasing trotz MSAA in Deferred-Rendering [⭐ HÄUFIG]

- **Symptom:** Treppchen/Flimmern an Kanten bleiben, obwohl MSAA „aktiviert" ist. Performance-Einbruch ohne Qualitätsgewinn.
- **Ursache:** **MSAA ist mit Deferred Rendering inkompatibel.** MSAA arbeitet an Geometriekanten zum Zeitpunkt des Rasterns; Deferred verschiebt die Beleuchtung in einen Screen-Space-Pass über den G-Buffer, wo die Kanteninformation für MSAA nicht mehr im nötigen Format vorliegt (und MSAA-G-Buffer wären extrem teuer). Zudem deckt MSAA nie Shader-/Textur-Aliasing ab.
- **Versionen:** Zeitlos / per Design (architektonische Eigenschaft von Deferred).
- **FIX:** In Deferred-Pipelines **TAA** verwenden (glättet Geometrie UND Shader/Textur-Aliasing, deferred-kompatibel). MSAA nur in Forward-Pipelines/VR sinnvoll. FXAA als Budget-Fallback (verwischt aber Feindetail).
- **Quelle:** [Wikipedia – Temporal anti-aliasing](https://en.wikipedia.org/wiki/Temporal_anti-aliasing); [box.co.uk – FXAA vs TAA vs MSAA vs DLAA](https://www.box.co.uk/blog/fxaa-vs-taa-vs-msaa-vs-dlaa)

---

## 8. TAA-Geisterbilder / Schlieren (Ghosting) [⭐ HÄUFIG]

- **Symptom:** Hinter bewegten Objekten bleiben verschmierte „Geister"-Spuren; bei Disocclusion (vorher verdeckte Bereiche werden frei) entstehen Schlieren; insgesamt leicht weichgezeichnetes Bild.
- **Ursache:** TAA mittelt über mehrere Frames per Reprojektion. Stimmen die **Motion-Vektoren** nicht (fehlen für bestimmte Objekte, transparente Geo, Vertex-Animationen), oder wird die History bei Sichtbarkeitswechsel nicht verworfen, wird altes Pixel-Material auf die neue Position geklebt → Ghosting.
- **Versionen:** Zeitlos / per Design (Trade-off der temporalen Akkumulation), aber durch Algorithmen stark reduzierbar.
- **FIX:** Korrekte Motion-Vektoren für ALLE bewegten Quellen erzeugen (auch animierte/transparente Geo). **Variance-Clipping** (AABB der Nachbarfarben in YCoCg, History dahin clampen) als Haupt-Anti-Ghosting. **Depth-based History-Rejection** bei Disocclusion. Bei statischen Kameras hilft Jitter-Reduktion gegen Weichzeichnung.
- **Quelle:** [Wikipedia – Temporal anti-aliasing (Ghosting/Disocclusion)](https://en.wikipedia.org/wiki/Temporal_anti-aliasing); [Intel GameTechDev – TAA](https://github.com/GameTechDev/TAA)

---

## 9. SSR-Reflexe fehlen an Bildrändern / hinter Objekten [⭐ HÄUFIG]

- **Symptom:** Spiegelnde Böden/Flächen zeigen Reflexionen, die am Bildrand abrupt abreißen, oder Objekte außerhalb des Sichtfelds werfen keine Reflexion.
- **Ursache:** **Screen-Space Reflections sehen nur, was auf dem Bildschirm ist.** Was außerhalb des Viewports oder hinter anderer Geometrie liegt, existiert für SSR nicht → keine Reflexion. Per Definition.
- **Versionen:** Zeitlos / per Design (Screen-Space-Limitierung).
- **FIX:** **IBL (Prefiltered Environment Map) als Fallback** dort, wo SSR keine Daten hat. Üblich: SSR für nahe, sichtbare Reflexe, sanft in IBL überblenden, wo SSR ausläuft. So fehlen Reflexe nie ganz.
- **Quelle:** [Unity HDRP – Screen Space Reflection](https://docs.unity3d.com/Packages/com.unity.render-pipelines.high-definition@latest/index.html?subfolder=/manual/Override-Screen-Space-Reflection.html); best-practices §2/§5 (IBL-Fallback)

---

## 10. `gltf-transform --texture-compress webp` → „Invalid JPG, marker table corrupted"

- **Symptom:** Nach `gltf-transform optimize ... --texture-compress webp` meldet `gltf-transform inspect` (oder ein Loader) „Invalid JPG, marker table corrupted", obwohl die GLB klein ist und z. B. in Blender importierbar bleibt.
- **Ursache:** Tool-Bug im WebP-Konvertierungspfad: Es bleibt offenbar eine Referenz erhalten, die die Textur weiterhin als JPEG ansieht, obwohl sie nach WebP konvertiert wurde → inkonsistente/ungültige glTF wird erzeugt. Der Maintainer (Don McCurdy) konnte den Fehler ohne die Originaldatei nicht reproduzieren — also kein bestätigter, universeller Fix.
- **Versionen:** Tool-abhängig (glTF-Transform). Status **unklar/nicht bestätigt** — siehe Fix-Status.
- **FIX:** **KTX2 statt WebP** verwenden (`--texture-compress ktx2`). KTX2/Basis ist ohnehin der robustere Web-Standard (GPU-komprimiert, ~10× weniger VRAM). Wenn WebP zwingend ist: aktuelle glTF-Transform-Version testen und mit der Originaldatei in der Discussion melden.
- **Quelle:** [glTF-Transform Discussion #1305](https://github.com/donmccurdy/glTF-Transform/discussions/1305)

---

## 11. KTX2-Normal/Daten-Map falsch als sRGB getaggt

- **Symptom:** Eine als KTX2 komprimierte Normal-/Roughness-/Metallic-Map sieht falsch beleuchtet, zu dunkel oder „verwaschen" aus — selbst wenn dieselbe Map als PNG korrekt war.
- **Ursache:** Beim Encoden wurde die Daten-Map mit sRGB-Transfer-Funktion (OETF) getaggt statt linear. Manche Encoder (z. B. ältere NVIDIA Texture Tools) taggen ALLES als sRGB. Daten-Maps enthalten Skalar-Daten, keine Farbe — sie dürfen NIE color-managed werden.
- **Versionen:** Tool-abhängig (toktx/KTX-Software, NVIDIA Texture Tools). Verhalten teils tool-spezifisch.
- **FIX:** Beim Encoden für Nicht-Farb-Texturen explizit linear setzen: aktuell **`--assign_oetf linear --assign_primaries none`** (ersetzt das ältere `--linear`). UASTC für Normal-Maps (höhere Qualität), ETC1S für Diffuse. Im Loader sicherstellen, dass die Normal-Map als linear interpretiert wird.
- **Quelle:** [KTX-Software Issue #98 – Color space handling](https://github.com/KhronosGroup/KTX-Software/issues/98); [KTX-Software Discussion #503 – Is the texture a normal map?](https://github.com/KhronosGroup/KTX-Software/discussions/503); [Don McCurdy – Choosing texture formats](https://www.donmccurdy.com/2024/02/11/web-texture-formats/)

---

## 12. metallic-Mittelwerte & eingebackene Schatten in baseColor [⭐ HÄUFIG]

- **Symptom:** Material wirkt „plastik"/unecht; bei Lichtwechsel passen Schatten nicht zur Szene; Oberfläche sieht „CAD-haft" flach aus.
- **Ursache:** Der häufigste Anfängerfehler — zweiteilig: (1) **metallic auf Mittelwerten** (z. B. 0.5). PBR ist nahezu binär: pure Leiter = 1, Dielektrika = 0. Zwischenwerte gibt es real nur in Übergangszonen (Rost). (2) **baseColor mit eingebackener Beleuchtung** (gemalte Schatten/Highlights/AO) → die Beleuchtung wird beim Rendern doppelt aufgetragen.
- **Versionen:** Zeitlos / per Design (PBR-Materialmodell).
- **FIX:** metallic strikt 0 oder 1 (Zwischenwerte nur für echte Übergänge per Map). baseColor = reine Albedo ohne Licht, Schatten, AO. roughness-Variation über eine Map statt uniformem Wert (Unterschied zwischen „CAD" und „echt"). reflectance/F0 der Dielektrika meist auf Default ~0.5 lassen.
- **Quelle:** [Google Filament – Materials Guide](https://google.github.io/filament/Materials.html); [Khronos – PBR in glTF](https://www.khronos.org/gltf/pbr/)

---

## 13. Emissive leuchtet nicht / Bloom in falscher Reihenfolge [⭐ HÄUFIG]

- **Symptom:** (a) Ein als emissiv gedachtes Material leuchtet nicht stark genug und löst keinen Bloom aus. (b) Bloom/Partikel/Additiv-Effekte sehen falsch hell oder „milchig" aus.
- **Ursache:** (a) Der glTF-Core-`emissiveFactor` ist hart auf **[0.0, 1.0] geclamped** — ohne Extension kann nichts „über 1" leuchten, also auch keine HDR-Schwelle für Bloom überschreiten. (b) Bloom (und additives Blending) muss in **HDR/linear VOR dem Tonemapping** laufen; wird er danach (auf bereits getonemapptes, geclamptes Bild) angewandt, stimmt die Helligkeit nicht.
- **Versionen:** (a) per Design des glTF-Core; behoben durch Extension **KHR_materials_emissive_strength**. (b) zeitlos / per Design der Postprocessing-Reihenfolge.
- **FIX:** (a) **KHR_materials_emissive_strength** nutzen und `emissiveStrength > 1` setzen (unitless Multiplikator, Werte z. B. bis 256 für kräftiges Glühen). Das ist zugleich der Hint für Bloom. (b) Postprocessing-Reihenfolge einhalten: szenenbezogene Effekte (SSAO/SSR/DoF/Motion Blur/**Bloom**) in linear/HDR → dann **Tonemapping** → dann Color Grading. Bloom dezent dosieren (übertrieben = billig).
- **Quelle:** [KHR_materials_emissive_strength Spec](https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_materials_emissive_strength/README.md); [Unity HDRP – Custom Post Process Execution Order](https://docs.unity3d.com/Packages/com.unity.render-pipelines.high-definition@latest)

---

## Fix-Status

**Per Design / zeitlos (kein „Fix" möglich — Physik/Color-Science/Architektur; Disziplin ist die Lösung):**

| # | Eintrag | Warum zeitlos |
|---|---------|---------------|
| 1 | Normal-Map Y-Konvention | Engine-Konvention (Y+ vs Y−), nicht behebbar — nur korrekt wählen |
| 2 | Linearer Workflow | Licht addiert sich physikalisch linear; Disziplin nötig |
| 3 | Tonemapping nötig | HDR→SDR-Mapping ist physikalisch zwingend |
| 4 | baseColor nicht 255 | Headroom-Reservierung ist gewollt — kein Bug |
| 5 | CSM-Shimmering | Diskretisierung der Shadow Map; Texel-Snapping ist die Gegenmaßnahme |
| 6 | Acne / Peter Panning | Float-Präzision der Shadow Map; Bias-Tuning |
| 7 | MSAA in Deferred | Architektonisch inkompatibel → TAA |
| 8 | TAA-Ghosting | Trade-off temporaler Akkumulation; reduzierbar, nicht eliminierbar |
| 9 | SSR-Randabriss | Screen-Space-Limitierung per Definition → IBL-Fallback |
| 12 | metallic/baseColor | PBR-Materialmodell; Anwenderdisziplin |
| 13b | Bloom-Reihenfolge | Postprocessing-Pipeline-Logik |

**Tool-/Spec-abhängig (Status separat):**

| # | Eintrag | Status |
|---|---------|--------|
| 2 | three.js Color-Management | Standardmäßig aktiv seit **r152** (`outputColorSpace`). Migrationsfalle bei Upgrade aus < r152. |
| 3 | model-viewer Default-Tonemapper | **v4.0**: PBR Neutral ist Default (vorher ACES). Behoben/verbessert. |
| 10 | gltf-transform WebP „Invalid JPG" | **Unklar / nicht bestätigt** — vom Maintainer ohne Repro-Datei nicht nachvollziehbar. Workaround: KTX2. |
| 11 | KTX2 sRGB-Tag bei Daten-Maps | Tool-spezifisch (toktx vs NVIDIA Texture Tools). Korrektes Flag: `--assign_oetf linear`. |
| 13a | emissive > 1 | Per Spec erst mit Extension **KHR_materials_emissive_strength** möglich (Core clamped [0,1]). |

**Ehrlichkeits-Hinweis:** `gh`-CLI war nicht verfügbar, daher konnten exakte Merge-/Release-Status einzelner GitHub-Issues nicht 1:1 verifiziert werden. #10 ist explizit als **unbestätigt** markiert. Die meisten Einträge sind ohnehin „per Design" und brauchen keinen Versions-Fix, sondern korrekte Anwendung — das ist oben ehrlich so gekennzeichnet. Versions-/Default-Angaben (three.js r152, model-viewer v4.0) stammen aus offiziellen Release-/Doku-Quellen, abgerufen 2026-06-13.

---

## Bezug ↔ Best-Practices

Jeder Bug hat sein „Wie macht man es richtig"-Gegenstück in `best-practices/assets/3d-visual-quality.md`:

| Bug-Almanach § | Thema | Best-Practices § |
|----------------|-------|------------------|
| §1 Normal-Map-Konvention / Tangenten | Asset-Pipeline / Normal Maps | §8 |
| §2 Linearer Workflow | Linear/sRGB Color Management | §4 |
| §3 Tonemapping aus | Tonemapping (ACES/AgX/PBR Neutral) | §3 |
| §4 baseColor nicht weiß | Tonemapping / Headroom | §3 (Kurzcheck #11) |
| §5 CSM-Shimmering | Schatten / CSM | §6 |
| §6 Acne / Peter Panning | Schatten / Bias | §6 |
| §7 MSAA in Deferred | Anti-Aliasing | §7 |
| §8 TAA-Ghosting | Anti-Aliasing (TAA) | §7 |
| §9 SSR-Randabriss | Postprocessing / IBL-Fallback | §5, §2 |
| §10 gltf-transform WebP | Asset-Pipeline (KTX2 statt WebP) | §8 |
| §11 KTX2 sRGB-Tag | Asset-Pipeline / Color-Space | §8, §4 |
| §12 metallic / baseColor | PBR-Materialien | §1 |
| §13 Emissive / Bloom-Reihenfolge | Postprocessing-Stack | §5 |

---

## Pflicht-Checkliste vor dem Start

- [ ] **Color-Management aktiv?** In linear rendern, sRGB nur am Output. Color-Maps = sRGB, Daten-Maps (roughness/metal/normal/AO) = linear. (Bei three.js ≥ r152 nicht doppelt korrigieren.)
- [ ] **Tonemapping gesetzt?** PBR Neutral (Produkt) oder ACES/AgX (Film/Game) — niemals ungemappt clampen.
- [ ] **Normal-Map-Konvention geprüft?** glTF/OpenGL = Y+; bei Unreal/3ds-Max-Quellen grünen Kanal invertieren. Tangenten via MikkTSpace.
- [ ] **metallic strikt 0 oder 1?** Keine Mittelwerte. baseColor frei von eingebackenem Licht/Schatten/AO.
- [ ] **Schatten stabilisiert?** CSM mit statischen Intervallen + Texel-Snapping; Slope-Scaled Bias gegen Acne, Front-Face-Culling gegen Peter Panning.
- [ ] **AA passend zur Pipeline?** Deferred → TAA (mit korrekten Motion-Vektoren + Variance-Clipping); Forward/VR → MSAA möglich; Budget → FXAA.
- [ ] **Postprocessing-Reihenfolge?** SSAO → SSR → DoF → Motion Blur → Bloom → **Tonemapping** → Color Grading. Bloom/Additiv VOR Tonemapping in HDR.
- [ ] **SSR mit IBL-Fallback** kombiniert, damit Reflexe an Rändern nicht abreißen.
- [ ] **Texturen GPU-komprimiert?** KTX2/Basis (UASTC für Normals, ETC1S für Diffuse); Daten-Maps mit `--assign_oetf linear`. WebP-Pfad meiden (gltf-transform #1305).
- [ ] **Emissive HDR?** Für Glühen/Bloom KHR_materials_emissive_strength > 1, nicht nur emissiveFactor (clamped [0,1]).

*Alle Quellen abgerufen am 2026-06-13. Engine-übergreifend; Versions-/Default-Angaben stammen aus offiziellen Release-/Doku-Quellen.*


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [3d-filament-android](../android/3d-filament-android.md)
- [3d-dotnet-directx-windows](../desktop/3d-dotnet-directx-windows.md)
- [3d-godot](../desktop/3d-godot.md)
- [3d-metal-scenekit-macos](../desktop/3d-metal-scenekit-macos.md)
- [3d-rust-wgpu-bevy](../desktop/3d-rust-wgpu-bevy.md)
- [3d-threejs-webgpu](../web/3d-threejs-webgpu.md)
