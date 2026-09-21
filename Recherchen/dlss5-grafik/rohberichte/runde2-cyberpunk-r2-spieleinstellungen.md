# Web-Researcher 2/5 — Beste Spiel-Grundeinstellung für DLSS-5-Pass (OptiScaler-DLSSNR v0.2.0) in Cyberpunk 2077 v2.31

Kontext: RTX 5090, Treiber 616.92, Cyberpunk 2077 v2.31 (Steam), OptiScaler-DLSSNR v0.2.0 (Fork von Dagherbou/OptiScaler_DLSSNR, fängt DLSS-Aufrufe ab und hängt NVIDIAs DLSS-5-Neural-Rendering-Pass danach an, vor dem HUD).

---

## 1. Was der Mod technisch abfängt (Grundlage für alle Empfehlungen unten)

- Der Mod ist ein **Fork von OptiScaler** ("OptiScaler + DLSS Neural Rendering", Autor Dagherbou), der `nvngx_dlssnr.dll` als zusätzlichen Verarbeitungsschritt NACH dem im Spiel gewählten Upscaler einhängt. Nicht der offizielle OptiScaler-Release. — [Nexus-Modseite "DLSS 5 Neural Rendering Installation Guide"](https://www.nexusmods.com/cyberpunk2077/mods/33380), [GitHub Dagherbou/OptiScaler_DLSSNR](https://github.com/Dagherbou/OptiScaler_DLSSNR)
- v0.2.0-Release: "Neural Rendering läuft jetzt auf allen Upscalern — DLSS, FSR und XeSS, nicht nur DLSS", plus Vulkan-Nativsupport und DX11-Bridge mit direktem DLSS-Support. — [Release v0.2.0-dlssnr](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr)
- **Ray Reconstruction/DLSSD wird eigenständig mit abgefangen, nicht nur DLSS-SR:** Die Installationsanleitung nennt explizit, dass man "RR in den Spieleinstellungen aktivieren und Neural Rendering in OptiScaler" gleichzeitig nutzen kann, rät aber, DLSSD (RR) und DLSSNR (NR) in der Konfiguration getrennt zu halten. Cyberpunk 2077 wird als Beispieltitel für einen bekannten "Exposure-Scan-Use-after-free"-Bug genannt, der spezifisch bei "Ray-Reconstruction + Streamline"-Titeln auftrat und in v0.2.0 gefixt wurde — das bestätigt, dass der NR-Pass in CP2077 auch hinter aktivierter Ray Reconstruction hängt, nicht nur hinter DLSS-SR. — [INSTALL-DLSSNR.md](https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/INSTALL-DLSSNR.md), [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17)
- **Bekannter CP2077-spezifischer Bug:** Wenn RR-Option in OptiScaler ausgegraut ist, liegt es am Proxy-Modus — `d3d12.dll`-Proxy vermeiden, `dxgi.dll` verwenden. — [INSTALL-DLSSNR.md](https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/INSTALL-DLSSNR.md)
- **DLSS-5-NR-Pass-Kosten hängen an der Ausgabeauflösung, nicht an der Renderauflösung:** "DLSS 5's performance is tied directly to the output resolution, not the render resolution, because DLSS 5 processes the final rendered frame after upscaling" — d.h. der NR-Pass selbst kostet bei 4K-Ausgabe praktisch gleich viel, egal ob DLAA oder DLSS Performance als SR-Eingang läuft; nur der Upscaling-Schritt darunter wird billiger. — [NVIDIA Research ADLR: DLSS 5 Generative Neural Rendering](https://research.nvidia.com/labs/adlr/DLSS5/)

## 2. DLAA vs. DLSS Quality/Balanced (Eingabeauflösung fürs Neural-Rendering-Modell)

- Da der NR-Pass kostenmäßig an der Ausgabe- statt Renderauflösung hängt (siehe oben), bringt eine niedrigere SR-Eingangsstufe (Quality/Balanced) FPS-Gewinn beim Upscaling-Schritt, OHNE dass der NR-Pass selbst günstiger wird.
- Für die Bildqualität, die der NR-Pass als Grundlage bekommt, gilt laut Modding-Community: "Bei niedrigerem Modell-Resolution (z.B. 70-75%) entstehen mehr Artefakte in der Bewegung." Der Entwickler empfiehlt **mindestens DLSS Balanced oder höher** als SR-Basis für saubere NR-Ergebnisse. — [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17)
- Praktische Konsequenz: DLAA liefert dem NR-Modell das sauberste Ausgangsbild (kein Upscaling-Rauschen), kostet aber die volle native Renderlast obendrauf zur NR-Pass-Last — bei 4K mit Path Tracing auf der RTX 5090 kaum praktikabel. DLSS Quality ist der sinnvolle Kompromiss aus Bildqualität für den NR-Pass und Spielbarkeit.

## 3. Ray Reconstruction an/aus

- Der Mod fängt RR (DLSSD) ausdrücklich mit ab (siehe Abschnitt 1) — RR bleibt also aktivierbar und wird vom NR-Pass mitverarbeitet.
- Community-Konsens (bezogen auf das Zusammenspiel von NVIDIAs offiziellem DLSS-4.5-Transformer-Preset L und Ray Reconstruction, gilt analog für den Trade-off): **Preset L deaktiviert bei Aktivierung die klassische Raytracing-Denoising-Pipeline**, was bei Path-Tracing-Spielen zu deutlich instabilerem Bild führt. Ohne Ray Reconstruction entsteht "Boiling" (sichtbares Flächenrauschen), da "DLSS 4.5 itself does not have built-in noise reduction for ray tracing". Fazit der Quelle: "most gamers agree that Ray Reconstruction remains the preferred choice. Image stability and noise reduction are more important than the slight improvement in texture detail." — [GameGPU: DLSS 4.5 vs. Ray Reconstruction in Cyberpunk 2077](https://en.gamegpu.com/news/zhelezo/dlss-4-5-ili-ray-reconstruction-v-cyberpunk-2077)
- **Empfehlung: Ray Reconstruction EINSCHALTEN**, insbesondere bei Path Tracing.

## 4. Path Tracing vs. Raytracing Psycho

- Für "maximal realistische Grafik" ist Path Tracing (Overdrive) gegenüber klassischem Raytracing Psycho die durchgängig empfohlene Stufe, da nur Path Tracing die komplette Beleuchtungspipeline durch Pfadverfolgung ersetzt. — [NVIDIA Blog: Cyberpunk 2077 Path-Traced Visuals](https://blogs.nvidia.com/?p=63236) (allgemeine Einordnung)
- Getestete Kombination auf RTX 5090: Path Tracing "Overdrive (Maximum)" + Ray Reconstruction aktiv + DLSS Quality + Dynamic MFG bis 6x, 4K: 55,25 fps nativ ohne Frame Gen → 172,21 fps mit Dynamic MFG (Ziel 165 Hz). — [iLLGaming: DLSS 4.5 Dynamic MFG RTX 5090 Test](https://illgaming.net/dlss-45-cyberpunk-2077-dynamic-multi-frame-generation-rtx-5090/) (Hinweis: dieser Test nutzt NVIDIAs offizielles DLSS 4.5, NICHT den inoffiziellen DLSS-5-NR-Mod — als Baseline-Referenz brauchbar, nicht 1:1 auf den Mod übertragbar)
- Reines Path Tracing ohne DLSS/FG: RTX 5090 bei 4K ca. 55–70 fps (je nach Quelle/Szene). — [iLLGaming](https://illgaming.net/dlss-45-cyberpunk-2077-dynamic-multi-frame-generation-rtx-5090/), [TechPowerUp-Zusammenfassung via Suche](https://www.techpowerup.com/331242/nvidia-geforce-rtx-5090-performance-in-cyberpunk-2077-with-and-without-dlss-4-detailed)

## 5. DLSS-4.5-Preset (K/M/L, Transformer)

- NVIDIA-Empfehlungslogik: "Recommended" setzt **Preset M** für DLSS-Performance-Modus, **Preset L** für 4K-Ultra-Performance-Modus, **Preset K** für alle übrigen Modi (also auch DLAA/Quality/Balanced). — [Suchsynthese aus mehreren Quellen inkl. NVIDIA-Ankündigung](https://www.nvidia.com/en-my/geforce/news/dlss-4-5-dynamic-multi-frame-gen-6x-2nd-gen-transformer-super-res)
- In Cyberpunk 2077 sind die L/M-Presets in den niedrigen Modi (Performance/Ultra Performance) laut Quelle "radically superior" zu den alten K-Presets in niedrigen Modi und nahe an bzw. besser als mittlere/hohe DLSS-4-Modi. — [en.gamegpu.com: DLSS 4 vs DLSS 4.5 Settings Comparison](https://en.gamegpu.com/test-gpu/action-fps-tps/cyberpunk-2077-sravnenie-nastroek-dlss-4-protiv-dlss-4-5)
- **Wichtiger Zielkonflikt:** Preset L deaktiviert bei aktiver Nutzung die klassische Raytracing-Denoise-Pipeline und führt bei Path Tracing zu Bildinstabilität ("Boiling") — siehe Abschnitt 3. — [GameGPU: DLSS 4.5 vs. Ray Reconstruction](https://en.gamegpu.com/news/zhelezo/dlss-4-5-ili-ray-reconstruction-v-cyberpunk-2077)
- **Empfehlung:** Bei DLAA/Quality/Balanced + Path Tracing + Ray Reconstruction beim **Standard-Preset K** bleiben (nicht manuell auf L forcen), da L in dieser Kombination die RT-Denoise-Kette stört.

## 6. Multi Frame Generation 3x/4x + Reflex

- Cyberpunk 2077 zwingt Reflex automatisch auf "On", sobald Frame Generation aktiv ist. — [Suchsynthese, CDPR Support / Community-Guides](https://support.cdprojektred.com/en/cyberpunk%20/pc/sp-technical/issue/2369/how-to-enable-dlss-frame-generation)
- OptiScaler unterstützt MFG x3/x4 mit dem DLSS4-Transformer. — [Suchsynthese, u.a. NVIDIA GeForce News zu MFG in Cyberpunk 2077](https://www.nvidia.com/en-us/geforce/news/steel-seed-dlss-4-multi-frame-gen/)
- Konkrete (offizielle DLSS-4/4.5-)FPS-Referenzwerte RTX 5090, 4K, Path Tracing:
  - DLSS Performance + 4x MFG: ~280–290 fps, ~40 ms Latenz
  - DLSS Quality + 4x MFG: ~200 fps
  - Ohne Frame Generation (nur DLSS Quality): ~65–70 fps
  — [Suchsynthese aus TechPowerUp/Tom's Hardware/WindowsCentral-Berichten](https://www.techpowerup.com/331242/nvidia-geforce-rtx-5090-performance-in-cyberpunk-2077-with-and-without-dlss-4-detailed)
- 1440p, DLSS Quality + 4x MFG + Path Tracing: ~230 fps bei ~50 ms Latenz. — [Suchsynthese, siehe gleiche Quellenlage]
- Hinweis zum NR-Pass und MFG: In einem Community-Bugreport zur Kombination "DLSS5 via OptiScaler + Unlocked RTX MFG" wird angegeben, dass der "DLSS 5 neural pass" vor der Frame-Extrapolation abgeschlossen sein sollte (Anpassung der Injektionsmethode empfohlen) — das stützt indirekt die im Auftrag genannte Eigenschaft, dass der NR-Pass nur auf echten Frames läuft, nicht auf den von MFG interpolierten. Dokumentierte Probleme: temporales Flackern zwischen DLSS 5 und MFG, Micro-Downclocks mit Strobing-Effekt, Menü-Konflikte (OptiScaler versteckt natives DLSS-G und erzwingt FSR-3.1-Fallback). — [Kizzuwatnaa/DLSS5-Autopilot Issue #137](https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137)
- **Empfehlung:** MFG 3x oder 4x je nach Zielbildwiederholrate des Monitors, Reflex bleibt automatisch an; bei Flacker-/Strobing-Problemen zwischen NR-Pass und MFG auf Hook/CET-Injection statt der Standard-Injektionsmethode umstellen (siehe Issue #137).

## 7. Schärfe, Filmkörnung, chromatische Aberration, Bewegungsunschärfe, Linsenreflexe

- Allgemeine Optimierungs-Guides (nicht Mod-spezifisch, aber durchgängig bestätigt) stufen Bewegungsunschärfe, Filmkörnung, chromatische Aberration und Tiefenschärfe als "cosmetic, negligible fps impact" ein — d.h. sie kosten praktisch keine Leistung, sind reine Geschmacksfrage. — [games-genie.com: Best Graphics Settings Cyberpunk 2077 (Patch 2.31)](https://www.games-genie.com/articles/best-graphics-settings-cyberpunk-2077)
- Für maximal realistische, klare Optik wird in den allgemeinen Cyberpunk-Guides üblicherweise empfohlen, Bewegungsunschärfe, chromatische Aberration und Filmkörnung zu deaktivieren, da sie das Bild künstlich weichzeichnen/verfärben — dies ist Standardpraxis aus generischen PC-Optimierungsguides, **nicht spezifisch für den DLSS-5-NR-Mod verifiziert**.
- Zu einer expliziten Wechselwirkung zwischen Schärfe-Regler/Filmkörnung und dem DLSS-5-NR-Pass wurde in dieser Recherche **keine belastbare Quelle** gefunden — siehe OFFEN/UNSICHER unten.

## 8. HDR vs. SDR (Mod arbeitet intern SDR-referenziert, braucht WhitePoint/Proxy)

- v0.2.0 fügt gezielt einen **"HDR-safe colour path"** für das SDR-trainierte NR-Modell hinzu, statt rohe lineare/scRGB-Werte direkt einzuspeisen: Das Original-HDR-Bild bleibt unangetastet, die Modell-Eingabe wird über Paper-White oder Belichtung normalisiert und als begrenzter, display-referenzierter Proxy präsentiert. — [xenmods/DLSSNR-Cost-Scaler README](https://github.com/xenmods/DLSSNR-Cost-Scaler), [Release v0.2.0-dlssnr](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr)
- **Reversible Proxy**: fünf Modi, empfohlen wird **"Hybrid proxy + composed"** (Standard-Empfehlung des Entwicklers), da er verhindert, dass das Modell in hellen Bereichen nur "flache Weißflächen" ohne Detail sieht (das alte Verhalten). Der reine "Replace"-Modus flackert bewusst bei hellen Lichtern — dafür "Composed" verwenden. — [Release v0.2.0-dlssnr](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr)
- **Multi-Point Exposure Anchoring**: Kalibriert den Weißpunkt über mehrere Lichtsituationen mit Interpolation; White-Point-Quelle wahlweise manuell, Spiel-Exposure oder automatisches HDR-Metering (GPU-seitig unter D3D12/Vulkan, mit Highlight-Schutz).
- **Cyberpunk-2077-spezifisches Problem:** Das Spiel berechnet Belichtung "in einer Form, die der [Exposure-]Scan nicht sehen kann" — der automatische Weißabgleich fällt daher in CP2077 auf den **manuellen Schieber** zurück. Wer HDR nutzt, muss den White-Point manuell kalibrieren, statt sich auf Auto-Erkennung zu verlassen. — [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17)
- Community-Empfehlung zur Feinjustierung gegen Flackern/Rauschen: Detailstärke ca. 15 % unter Standard, Highlight Guard leicht reduzieren, Farbintensität unter 1,5x halten. — [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17)
- Laut Installationsdoku des Multipass-Forks unterstützt DX12 nativ SDR, HDR10 und scRGB, HDR wird automatisch erkannt — die Umsetzung ist also grundsätzlich vorhanden, aber CP2077 braucht wegen des Exposure-Scan-Problems manuelle Nacharbeit. — [INSTALL-DLSSNR.md](https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/INSTALL-DLSSNR.md)
- **Empfehlung:** Wer maximale Verlässlichkeit ohne Kalibrieraufwand will, bleibt in SDR (Mod arbeitet ohnehin intern SDR-referenziert). Wer HDR am Monitor nutzen will, muss den White-Point-Schieber in OptiScaler manuell einstellen, da die Auto-Erkennung in CP2077 nicht funktioniert.

## 9. Crowd Density / Texturqualität / Screen-Space-Reflections

- **Crowd Density:** High→Medium bringt 8–15 fps in dichten Distrikten zurück, ohne die Stadt spürbar leerer wirken zu lassen; zudem wird Crowd Density bei aktivem Path Tracing auf High-End-GPUs (wie der 5090) ohnehin automatisch reduziert, um CPU-Ressourcen für Path Tracing freizuhalten. — [Suchsynthese aus mehreren Optimierungsguides](https://deltiasgaming.com/best-settings-for-cyberpunk-2077-rtx-5090/)
- **Texturqualität:** Kostet primär VRAM, kaum GPU-Rechenzeit — auf der 5090 mit 32 GB VRAM kann High/Ultra bedenkenlos gefahren werden.
- **Screen-Space-Reflections:** Gilt als eine der teuersten Einzeleinstellungen in Cyberpunk 2077, sollte bei NICHT aktiviertem Raytracing auf Low/Medium reduziert werden. Bei aktivem Path Tracing/Ray Reconstruction übernimmt die Pfadverfolgung ohnehin sämtliche Reflexionen — SSR spielt dann keine sichtbare Rolle mehr. — [Suchsynthese, u.a. eXputer/SageTweaks-Guides](https://exputer.com/guides/settings/best-cyberpunk-2077-pc-settings-for-high-fps/)

## 10. Konkrete Einstellliste — RTX 5090, 4K (3840×2160)

| Einstellung | Empfehlung |
|---|---|
| Auflösung | 4K nativ |
| Upscaling (SR-Eingang für NR-Pass) | DLSS **Quality** (DLAA nur für Screenshots/ruhige Szenen, kostet zusätzlich zur NR-Pass-Last die volle native Renderlast) |
| DLSS-Preset | **K** (Standard) — L nicht forcen wegen RT-Denoise-Konflikt |
| Ray Reconstruction | **An** |
| Path Tracing | **Overdrive** (statt Raytracing Psycho) |
| Frame Generation | Multi Frame Generation **3x–4x**, Reflex automatisch an |
| Schärfe | reduziert/niedrig (allgemeine Empfehlung, nicht Mod-spezifisch belegt) |
| Filmkörnung/chromat. Aberration/Bewegungsunschärfe/Linsenreflexe | Aus (kostenlos laut games-genie.com) |
| HDR/SDR | SDR empfohlen, außer bei manueller White-Point-Kalibrierung in OptiScaler |
| Crowd Density | Medium–High (bei Path Tracing ohnehin CPU-limitiert reduziert) |
| Texturqualität | High/Ultra (VRAM-Kosten, kein Problem bei 32 GB) |
| Screen-Space-Reflections | irrelevant/aus (Path Tracing übernimmt Reflexionen) |
| **Erwartete FPS OHNE NR-Mod** (Referenzwert) | ca. 200 fps (DLSS Quality + Path Tracing + 4x MFG) laut Quellensynthese |
| **Erwartete FPS MIT NR-Mod** | grobe Schätzung ca. **100–110 fps** (Referenzwert × 45–50 % Kosten laut Auftragsangabe) — **keine Quelle testet diese exakte Kombination direkt, siehe OFFEN/UNSICHER** |

## 11. Konkrete Einstellliste — RTX 5090, 1440p (2560×1440)

| Einstellung | Empfehlung |
|---|---|
| Auflösung | 1440p nativ |
| Upscaling (SR-Eingang für NR-Pass) | **DLAA** ist bei 1440p wegen geringerer Pixelzahl eher machbar als bei 4K; alternativ DLSS Quality für mehr FPS-Puffer |
| DLSS-Preset | K (Standard) |
| Ray Reconstruction | An |
| Path Tracing | Overdrive |
| Frame Generation | MFG 3x–4x, Reflex automatisch an |
| Schärfe/Filmkörnung/Aberration/Bewegungsunschärfe/Linsenreflexe | wie 4K-Liste |
| HDR/SDR | wie 4K-Liste |
| Crowd Density / Texturqualität / SSR | wie 4K-Liste |
| **Erwartete FPS OHNE NR-Mod** (Referenzwert) | ca. 230 fps (DLSS Quality + Path Tracing + 4x MFG) laut Quellensynthese |
| **Erwartete FPS MIT NR-Mod** | grobe Schätzung ca. **115–125 fps** — ebenfalls nicht direkt quellenbelegt, reine Übertragung der 45–50 %-Kostenangabe |

---

## BEST-PRACTICES-KANDIDATEN:

- Ray Reconstruction bei Path Tracing + DLSS-4.5-Transformer aktiviert lassen, Preset L nicht forcen (RT-Denoise-Konflikt/"Boiling") — [GameGPU: DLSS 4.5 vs. Ray Reconstruction in Cyberpunk 2077](https://en.gamegpu.com/news/zhelezo/dlss-4-5-ili-ray-reconstruction-v-cyberpunk-2077), Stand Cyberpunk 2077 mit DLSS 4.5
- OptiScaler-DLSSNR: "Hybrid proxy + composed"-Modus statt reinem "Replace" nutzen, um Flackern in Highlights zu vermeiden; HDR-White-Point in CP2077 manuell kalibrieren, da Auto-Exposure-Scan im Spiel nicht funktioniert — [Release v0.2.0-dlssnr](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr), [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17), Stand v0.2.0
- Bei ausgegrautem Ray-Reconstruction-Schalter in OptiScaler für CP2077: `d3d12.dll`-Proxy meiden, `dxgi.dll` nutzen — [INSTALL-DLSSNR.md](https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/INSTALL-DLSSNR.md)
- Mindestens DLSS Balanced als SR-Eingangsqualität für den NR-Pass verwenden, darunter (z.B. 70–75 % Renderskalierung) steigen Bewegungsartefakte spürbar — [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17)

## BUG-KANDIDATEN:

- Temporales Flackern zwischen DLSS-5-NR-Pass und Multi Frame Generation, Micro-Downclocks mit Strobing-Effekt; OptiScaler versteckt natives DLSS-G und erzwingt FSR-3.1-Fallback im Menü — Workaround: Injection-Methode auf Hook/CET umstellen — [Kizzuwatnaa/DLSS5-Autopilot Issue #137](https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137), betrifft OptiScaler-DLSSNR in Cyberpunk 2077, kein Versionsstand im Issue genannt
- Exposure-Scan-Use-after-free-Crash bei Ray-Reconstruction+Streamline-Titeln (CP2077 explizit genannt) — laut Herstellerangabe in v0.2.0 gefixt — [Release v0.2.0-dlssnr](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr), Stand v0.2.0 (behoben)
- Cyberpunk-2077-Exposure-Scan liefert keine verwertbaren Werte für automatische HDR-Weißpunkt-Erkennung → Fallback auf manuellen Schieber nötig — [Discussion #17](https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17), Stand v0.2.0, ungelöst (Workaround: manuell)

## OFFEN/UNSICHER:

- Keine Quelle testet die **exakte Kombination** aus OptiScaler-DLSSNR v0.2.0 + Path Tracing + Ray Reconstruction + MFG bei 4K/1440p mit konkreten FPS-Messungen auf RTX 5090 — die FPS-Schätzungen in Abschnitt 10/11 sind eine rechnerische Übertragung (Referenz-FPS aus offiziellem DLSS-4/4.5-Betrieb ohne den Mod × 45–50 % Kostenangabe aus dem Auftrag), keine direkte Quellenmessung.
- Keine belastbare Quelle zur konkreten Wechselwirkung zwischen In-Game-Schärferegler/Filmkörnung und dem DLSS-5-NR-Pass gefunden (ob Über-Schärfung mit der NR-eigenen Detailanreicherung kollidiert). Empfehlung "Schärfe reduzieren" stammt aus allgemeinen Optimierungsguides, nicht Mod-spezifisch verifiziert.
- DLSS-4.5-Presets K/L/M: Zuordnung "K = restliche Modi, M = Performance, L = Ultra-Performance/4K" stammt aus einer Ankündigungs-/Community-Quelle zu DLSS 4.5 allgemein, nicht CP2077-2.31-spezifisch mit Versionsnummer belegt.
- Ob der NR-Pass bei aktivierter MFG tatsächlich ausschließlich auf realen (nicht interpolierten) Frames läuft, wird durch die Recherche nur indirekt gestützt (ein Bugreport erwähnt, dass der NR-Pass "vor der Frame-Extrapolation abgeschlossen" sein sollte) — keine explizite technische Bestätigung durch die Moddokumentation selbst gefunden.
- Die vollständige GitHub-Wiki-Seite "Cyberpunk 2077" von optiscaler/OptiScaler sowie die Nexus-Modseite 33380 konnten nicht vollständig geladen werden (Ladefehler bzw. HTTP 403) — dort könnten weitere CP2077-spezifische Einstellungshinweise stehen, die dieser Recherche entgangen sind.
