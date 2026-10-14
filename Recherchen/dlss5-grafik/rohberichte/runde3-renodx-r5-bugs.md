# R5 - Bekannte Bugs, Treiber, Konflikte: ShortFuse RenoDX DLSS (renodx-dlss.addon64)
Stand: 14.10.2026. Alle Aussagen mit Quelle. Unbestätigtes ist als solches markiert.

## 0. Wichtige Einordnung (Namensverwechslung vermeiden)
- Es gibt ZWEI verschiedene Add-ons: `renodx-dlss.addon64` (ShortFuse, "RenoDX DLSS Tool", WIP) und `renodx-dlss5.addon64` (Krish, ältere Community-Linie, v2.5 / v4.x / v6-v8). Laut ShortFuse-Anleitung laufen sie nicht zusammen, eines davon entfernen/umbenennen (.addon64x). Quelle: https://dlss5mod.com/guides/install
- Der "616.64-Crash" betrifft belegt NUR Krishs renodx-dlss5 v4.6/v4.7 (im DLSS5-Feeder-Test). Für ShortFuses renodx-dlss gibt es KEINEN belegten 616.64-Crash-Bericht (siehe Abschnitt 2).
- Nutzer-Build "2026-09-04" = Build-Zeitstempel 2026-09-04T08:48:41 (Add-on meldet 0.0.0.0), identisch mit dem im Issue #651 genannten Build. Quelle: https://github.com/clshortfuse/renodx/issues/651
- Neuere ShortFuse-Version laut Issue #697: RenoDX DLSS v0.2026.1003.2333 (Streamline 2.14.1, DLSS SR/RR/FG 310.9.1, ReShade 6.8.0.2155). Quelle: https://github.com/clshortfuse/renodx/issues/697

## 1. Bekannte Bugs des ShortFuse-Add-ons (GitHub clshortfuse/renodx, alle Issues OFFEN, keine Maintainer-Antwort sichtbar)

### B1 Absturz bei Hook Method Render/Upscaled (Present geht)
- Symptom: Monster Hunter Wilds stürzt ab, sobald Hook Method von Present auf Render oder Upscaled gestellt wird. Letzte Logzeile: "RenoDX DLSS NR rebuilding: recreate_feature=false, passes=1->1", kein RenoDX-Fehler.
- Ursache: unbekannt. Setup: D3D12, RTX 4070 Ti Super, Treiber nicht genannt.
- Version: RenoDX DLSS v0.2026.1003.2333, ReShade 6.8.0.2155, Streamline 2.14.1, DLSS 310.9.1
- Fix: Workaround Hook Method = Present. Kein offizieller Fix.
- Quelle: https://github.com/clshortfuse/renodx/issues/697 (04.10.2026)

### B2 Direct Neural Rendering hängt dauerhaft auf "Waiting"
- Symptom: Mit "Require DLSS" an bleibt NR in allen Hook Methods (Auto/Upscaled/Present) auf "Waiting"; keine NGX-Init/CreateFeature/Evaluate-Logs. Mit Require DLSS aus: source=3, preset=1 (Fallback-Quelle).
- Ursache (Vermutung des Melders, unbestätigt): Source-Match-Prüfung akzeptiert vom DLSS-5-Bridge erzeugte Features nicht; Motion Vectors R16G16_FLOAT statt RG32F. Krishs älteres renodx-dlss5 funktioniert im selben Setup.
- Version: Build 2026-09-04T08:48:41, ReShade 6.8.0.2155, Bridge v1.4.8.0, Euro Truck Simulator 2 (DX11)
- Fix: keiner; Workaround Krishs renodx-dlss5 nutzen.
- Quelle: https://github.com/clshortfuse/renodx/issues/651

### B3 Performance-Regression im Present-Modus (~10 FPS)
- Symptom: Neuere Builds ~30+ FPS statt ~40+ FPS beim Build vom 4. September (gleiche Szene, FG aus, Hook Present, 1 Pass, RTX 4070).
- Ursache/Fix: nicht genannt. Hinweis: Der 04.09.-Build des Nutzers ist laut diesem Issue der SCHNELLERE; nicht blind auf neuere Builds updaten.
- Quelle: https://github.com/clshortfuse/renodx/issues/652

### B4 Flackern (Schatten) bei Neural Rendering im "latest patch"
- Symptom: ständiges Flackern mit NR an, nicht in der Vorversion. Später präzisiert: Cyberpunk 2077, Schattenflackern, skalierter NR-Pfad.
- Ursache (Vermutung eines Kommentators S3Shankel, 29.09.2026): Der skalierte NR-Pfad bricht den Motion-Vector-Vertrag (Motion-Resource, Origin, Dimensions und MVecScale X/Y müssen gemeinsam vor dem skalierten Evaluate wiederhergestellt werden). Behauptet: kein Flackern bei 75%/Quality/Balanced/DLAA, mit und ohne FG. Hohe Pass-Zahlen (ca. P8-P10) können an VRAM-Admission scheitern (separates Problem).
- Fix: Nur ein inoffizieller, gepatchter Binary-Kandidat ("v34W"), NICHT veröffentlicht, Herkunft unklar. Nicht installieren.
- Quelle: https://github.com/clshortfuse/renodx/issues/642
- Ergänzend (anderes Projekt, Stellar-Blade-Anleitung): Flackern-Fixes in Reihenfolge: FG aus, DLAA statt DLSS, Scene Paper White Scale ~0.07, Add-on auf v2.5. Quelle: https://www.nexusmods.com/stellarblade/mods/3706 (nur per WebSearch-Auszug, Seite per WebFetch 403)

### B5 Schwarzer Bildschirm bei NR Render (TLOU2 Remastered)
- Symptom: NR Render an = Bild schwarz, Overlay sichtbar; Proxy-Capture am First Present scheitert (captured=false), Swapchain-Identitäten unterschiedlich. CreateFeature/Evaluate laufen.
- Version: renodx-dlss.addon64 v0.55, nvngx_dlssnr.dll 310.8.SF.0, ReShade 6.8.0, RTX 3090 Ti
- Fix: kein offizieller; ein Kommentator nennt ein Build aus PR #631 (lief angeblich auf RTX 5090) - nicht verifiziert, Drittbinary.
- Quelle: https://github.com/clshortfuse/renodx/issues/666

### B6 Frame Generation: "ReShade before Frame Generation" / nur eine Swapchain
- Symptom: Einstellung "ReShade before FG" unklar funktionsfähig; GTA V Enhanced: Flackern mit nativem DLSS-G + Direct NR im Present-Modus; Log: Present-Evaluation abgelehnt, "requires one live swapchain per device". Im selben Add-on funktioniert FG in Onimusha.
- Version: Add-on-Build 2026-09-07, Streamline 2.14.1, DLSS/DLSS-G 310.9.1, ReShade 6.7.3.4, Treiber 616.56
- Fix: keiner.
- Quelle: https://github.com/clshortfuse/renodx/issues/648
- Ähnlich: Darktide friert nach RenoDX-Update bei aktivierter NVIDIA Frame Generation ein (Issue vom 30.07.2026, kein DLSS-Add-on-spezifischer Beleg). Quelle: https://github.com/clshortfuse/renodx/issues/612
- Weiter: Stellar-Blade-Guide: FG kann zweites D3D12-Device erzeugen, ReShade-Runtime wird zerstört, Add-on läuft weiter, Overlay zeichnet nie. Test: FG aus. Quelle: https://www.nexusmods.com/stellarblade/mods/3706 (nur WebSearch-Auszug)

### B7 HDR / Paper-White: Rauschen in dunklen Szenen
- Symptom: Scene Paper-White Scale ~16000 gut bei Tag, aber Körnung/Rauschen in dunklen Szenen; ~1000 behebt es. Feature-Wunsch: automatische Anpassung.
- Quelle: https://github.com/clshortfuse/renodx/issues/650
- Ergänzend: Falscher Paper-White-Wert -> überbelichtete Lichter / unnatürliche Farben. Quelle: https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/
- Farb-/Stil-Wunsch: Color-Strength-Slider (neurale Beleuchtung behalten, Spielfarben zurück). Quelle: https://github.com/clshortfuse/renodx/issues/693

### B8 Weitere offene DLSS-NR-Issues (Kurzliste)
- #643 DLSS5 Generic scheitert in Dying Light: The Beast ("output format 9 rejected"): https://github.com/clshortfuse/renodx/issues/643
- #668 DLSS-NR bleibt idle (PCSX2, Streamline-Interposer nicht gefunden): https://github.com/clshortfuse/renodx/issues/668
- #702 MSFS 2024 unter Proton stürzt bei NR-Feature-Erzeugung ab (vkd3d-proton): https://github.com/clshortfuse/renodx/issues/702 (nur Linux)
- #669 LEGO Batman stürzt beim Upgrade-Menü (RenoDX/UE-Extended): https://github.com/clshortfuse/renodx/issues/669
- #687 NR Pre-Upscale geht nicht / DLAA fix (32-bit DX9): https://github.com/clshortfuse/renodx/issues/687
- Liste aller 23 DLSS-Issues: https://github.com/clshortfuse/renodx/issues?q=is%3Aissue+DLSS

### B9 Nicht belegt (ehrlich)
- "Wachsgesichter", Ghosting als eigenes Issue, HUD-Verarbeitung des ShortFuse-Add-ons: KEINE spezifische Quelle zum ShortFuse-Add-on gefunden. Reddit/Discord/YouTube waren per Suche nicht auffindbar.
- Verwandt: DLSS5-Feeder (anderes Projekt) schreibt "Das HUD wird zusammen mit der Szene verarbeitet" (https://github.com/jlrouzies-fr/DLSS5-Feeder). Igor's Lab nennt eine Option "UI Correction" (für UI gedacht) und generelles Flackern/Ghosting/zeitliche Instabilität bei Bewegung (https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/).
- VRAM: hohe Pass-Zahlen können an VRAM-Admission scheitern (https://github.com/clshortfuse/renodx/issues/642); allgemein "zusätzliche Rechenleistung und VRAM nötig" (Igor's Lab, s.o.). Leistungsverlust: "kann Framerate deutlich senken" (Igor's Lab); Modell läuft in Ausgabeauflösung, DLSS Quality vs Performance spart nichts (https://paradrew.com/en/blog/kak-ustanovit-dlss-5/).
- ShortFuse-Build: laut Guide WIP, Bug-Reporting zum Prüfzeitpunkt geschlossen; unterstützt 64-bit DX9/11/12, kein Vulkan/OpenGL/32-bit DX9. Quelle: https://dlss5mod.com/guides/install

## 2. Treiber 616.92 / neueste Treiber (Oktober 2026)
- Treiber-Zeitlinie: 616.56 -> 616.64 (03.09., NBA 2K27, offizielles DLSS 5) -> 616.86 (Hotfix) -> 616.92 (ca. 09./10.09.2026) -> 617.14 -> 617.42 (06.10.2026, WHQL). Quellen: https://paradrew.com/en/blog/kak-ustanovit-dlss-5/ , https://www.techpowerup.com/352517/nvidia-releases-geforce-616-92-whql-game-ready-drivers , https://www.ntcompatible.com/story/nvidia-geforce-game-ready-driver-61742-released-fixes-assassins-creed-shadows-crash-adds-call-of-duty
- 616.64-Problem (Ursache): Bei Krishs renodx-dlss5 v4.6/v4.7 faulten ALLE DLSS-Evaluates in NVIDIAs nvngx_dlssnr.dll (0xC0000005 in D3D12Core.dll <- nvngx_dlssnr.dll <- _nvngx.dll), Spiel rendert normal, aber kein Neural-Frame. Gemessen an 616.56/616.64/616.86. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/releases/tag/v0.14.0-beta.1
- FIX/Workarounds (Messung RTX 5090, 300 Testläufe): renodx-dlss5 v6.1.0, v7.0.0-rc8, v8.0.1 = 300/300 auf 617.14; v4.55 (classic engine) und "latest" classic = 300/300 auf 616.64; v4.7 = 0/300 auf 616.64 und 617.14, 300/300 auf 616.56; Alternative Deep Fried Chicken 1.4.8-alpha; oder Treiber 616.56. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder
- Treiber 616.92 steht in keiner Feeder-Tabelle. Ein Singularity-Crash-Bericht (RTX 3060 Laptop, 616.92) behauptet nur, 4.6/4.7 fehlten auf 616.64+; unverifiziert, Issue ohne Treiber-Befund geschlossen (Ursache dort: Firewall, WinError 10013). Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/434
- Blog: "NVIDIA fixte das Flackern in Hotfix 616.86 und in 616.92" (Flackern = 8-bit-Ausgabe; Workaround 10 bit in der NVIDIA-Systemsteuerung); 616.64+: "modded network crashes for a lot of people", manche laufen problemlos, v.a. mit RTSS/Afterburner geschlossen; "Treiber nicht updaten, wenn es schon läuft". Quelle: https://paradrew.com/en/blog/kak-ustanovit-dlss-5/
- DLSS5-Swapper: 616.56 = letzter gemessener Treiber mit abgeschlossenem Neural Pass; 616.64+ fault im NVIDIA-Runtime (616.92/617.x nicht direkt getestet). Quelle: https://github.com/rakanki911/DLSS5-Swapper/releases/tag/v2.2.4
- ShortFuse renodx-dlss auf 616.92/617.x: KEIN direkter Bericht gefunden. Indizien: läuft auf RTX 5090 (TLOU2-Kommentar, Treiber nicht genannt, https://github.com/clshortfuse/renodx/issues/666); Treiber 616.56 (https://github.com/clshortfuse/renodx/issues/648). Das Feeder-Projekt sagt, ShortFuse ersetzt das Feeder-Projekt; Treiberergebnisse sind nicht übertragbar.
- 617.42: laut ntcompatible kein DLSS-5-Eintrag ("DLSS 4.5 Games"), behebt AC-Shadows-Crash (Bug 6685219, seit 616.56). Formale Release Notes zum Zeitpunkt noch nicht komplett. NVIDIA-Notes nicht selbst gelesen.
- Offizieller NVIDIA-Fix für das 616.64-Problem: keiner gefunden (Problem tritt nur beim modifizierten/geleakten Pfad auf; offizielle Spiele wie NBA 2K27 laufen auf 616.64).

## 3. Konflikte
### OptiScaler / OptiScaler-DLSSNR
- Beide laden als `dxgi.dll`: "The two engines cannot share a game". Beim Engine-Wechsel wird der alte zuerst entfernt (Tool-Verhalten). Quelle: https://github.com/faisalkindi/DLSS5oneclick
- DLSS5-Feeder: OptiScaler-DLSS-NR-Fork wird unterstützt, aber Chicken UND RenoDX müssen aus diesem Ordner entfernt werden; "install only one" neural add-on. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder
- Andere neurale Add-ons: nur eines installieren; mehrere Add-ons blockieren sich, ReShade läuft, NR startet nicht. Quelle: https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/ ; ShortFuse-Guide: Feeder/Bridge/zweites Streamline-Paket vermeiden, außer eine Spiele-Anleitung verlangt es (https://dlss5mod.com/guides/install)
- Muss OptiScaler-DLSSNR vorher entfernt werden? Ja, praktisch: gleiche Aufgabe (nvngx_dlssnr-Hook), gleicher dxgi.dll-Slot, "DLSS-NR-Fork + RenoDX im selben Ordner" wird vom Feeder-Projekt ausgeschlossen. Zu entfernen (aus Quellenlage abgeleitet, keine explizite Deinstallationsanleitung gefunden): dxgi.dll (OptiScaler), OptiScaler.ini, die vom Fork mitgebrachte nvngx_dlssnr.dll-Kopie im Spielordner und Begleitdateien (z. B. plugins-Ordner, Fakenvapi). Dagherbou-README verweist nur auf das (upstream-)Wiki. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR
- ReShade neben OptiScaler: Methode 1 ReShade-DLL in `ReShade64.dll` umbenennen und in OptiScaler.ini `LoadReshade=true`; Methode 2 Ultimate ASI Loader (Reshade.asi + OptiScaler.asi, dinput8.dll/winmm.dll); oder OptiScaler als winmm.dll; `plugins`-Ordner-Variante. `d3d12.dll` wird im Wiki NICHT erwähnt. "Not all names will work". Quelle: https://github.com/optiscaler/OptiScaler/wiki/Compatibility-with-other-mods-(Reshade,-SpecialK)
- RenoDX-HDR-Mod auf OptiScaler-Engine: Tool setzt ReShade als `ReShade64.dll` + LoadReshade=true; Autor hat die Kombination in keinem Spiel gelaufen; bei Crash Mod abwählen. Quelle: https://github.com/faisalkindi/DLSS5oneclick
- Historischer Crash RenoDX + OptiScaler + umbenanntes ReShade (The Outer Worlds, 2025): Crash nur mit OptiFG, nur Upscaling ging; Label "external"; Crash blieb auch nach Umbenennung (ReShade dxgi, OptiScaler winmm). Quelle: https://github.com/clshortfuse/renodx/issues/299
- DLSS-NR-OptiScaler-Fork selbst: bis v0.8.91 Flackern bei <100% Auflösung (Fix in v0.8.92 des Forks); "0.2.0-patch1 bricht manche Spiele, die auf 0.1.1.5 liefen". Quellen: https://github.com/jlrouzies-fr/DLSS5-Feeder , https://paradrew.com/en/blog/kak-ustanovit-dlss-5/

### NVIDIA App / DLSS-Overrides
- Kein "DLSS 5 Override" in der NVIDIA App; DLSS 5 bleibt Entwickler-Entscheidung. Quelle: https://paradrew.com/en/blog/kak-ustanovit-dlss-5/
- "NVIDIA App overrides may break the route" (ShortFuse-Installationsanleitung): Overrides für das Spiel ausschalten, wenn das Add-on sich nicht meldet. Quelle: https://dlss5mod.com/guides/install

### Frame Generation / Smooth Motion / Streamline
- FG zunächst aus lassen, auch beim Test des On-Present-Hooks, Dynamic Resolution aus. Quelle: https://dlss5mod.com/guides/install
- Smooth Motion (Treiber-FG) zusammen mit DLSS 5: Crashes (RDR2, SotTR) und Flackern/halbe Frames; Fix: Smooth Motion aus. Quellen: https://github.com/rakanki911/DLSS5-Swapper/issues/189 , https://github.com/jlrouzies-fr/DLSS5-Feeder
- Streamline: Add-on braucht Streamline-Dateien (Interposer); fehlt/falscher Pfad -> idle (https://github.com/clshortfuse/renodx/issues/668). Ein falsch signiertes nvngx_dlssnr.dll in einem Installer-streamline.zip verursacht dauerhaft 0xBAD00002/STANDBY (Hash-Mismatch; Installer eines Dritten): https://github.com/yumlevi/renodx-dlss-installer/issues/1

### Overlays / andere ReShade-Add-ons
- RTSS/MSI Afterburner: als Crash-/Schwarzbild-Ursache genannt; Schließen ist erster Schritt. Quellen: https://paradrew.com/en/blog/kak-ustanovit-dlss-5/ , https://dlss5-swapper.com/troubleshooting/dlss-5-swapper-not-working/ (nur WebSearch-Auszug)
- Steam- und Discord-Overlay: KEINE Quelle gefunden (weder Konflikt noch Entwarnung).
- Andere ReShade-Add-ons/Shader: Luma-Add-on crasht 1-2 s nach Start (Feeder/Smooth Motion); `LoadFromDllMain=renodx-dlss.addon64` in ReShade.ini als Fallback; Anti-Hook-Patches und Motion-Vector-Provider können stören. Quellen: https://github.com/jlrouzies-fr/DLSS5-Feeder , https://dlss5mod.com/guides/install
- ReShade selbst: Öffnen der ReShade-UI kann FPS senken. Quelle: https://dlss5mod.com/guides/install
- BattlEye blockiert dxgi.dll -> ReShade startet nicht. Quelle: https://paradrew.com/en/blog/kak-ustanovit-dlss-5/

## 4. Offizielles DLSS 5 (Stand Oktober 2026)
- Live: nur NBA 2K27 (RTX 50, Treiber 616.64 WHQL) laut Tracker, geprüft 01.10.2026. Quelle: https://www.dlss5.net/dlss-5-games
- Starfield, Assassin's Creed Shadows, Delta Force: angekündigt, KEIN verifizierter Live-Patch (Tracker 01.10.2026; Delta Force zuletzt 05.09.). Quelle: https://www.dlss5.net/dlss-5-games
- NVIDIA: DLSS 5 "will arrive this Fall"; Starfield, AC Shadows, Delta Force in der Liste. Quelle: https://www.nvidia.com/en-us/geforce/news/dlss5-breakthrough-in-visual-fidelity-for-games/
- Keine Hersteller-Patchnotiz für die drei Spiele bis 14.10.2026 gefunden (Suche, kein Beweis für Nichtexistenz).

## Praktische Folgerungen für den Nutzer (RTX 5090, Treiber 616.92)
1. Das 616.64-Problem ist das von Krishs renodx-dlss5 v4.6/v4.7. Für ShortFuses renodx-dlss existiert kein belegter Crash; v6.1+ der Krish-Linie und classic-Engine laufen auf neueren Treibern. Nach Installation im ReShade.log prüfen, ob Evaluates durchlaufen.
2. Vor dem Wechsel auf renodx-dlss: OptiScaler-DLSSNR sauber entfernen (dxgi.dll, OptiScaler.ini, mitgelieferte nvngx_dlssnr.dll/Begleitdateien) und nur EIN neurales Add-on im Spielordner lassen. Backup der Dateien.
3. Build 2026-09-04 behalten, wenn es läuft (neuere Builds laut #652 ~10 FPS langsamer; Gegenbeispiel Hook-Crash #697 bei v0.2026.1003.2333).
4. Hook Method: Present ist die belegt stabilste; Render/Upscaled crashte in MH Wilds.
5. Overlays (RTSS/Afterburner), Smooth Motion und FG beim ersten Test aus; NVIDIA-App-Overrides für das Spiel aus.

BEST-PRACTICES-KANDIDATEN:
- Neurales-Add-on-Regel: pro Spielordner nur EIN DLSS-NR-Quelle (OptiScaler-DLSSNR ODER RenoDX-DLSS ODER Feeder/Chicken), beide wollen dxgi.dll; vor Wechsel alte Dateien entfernen. Quellen: https://github.com/faisalkindi/DLSS5oneclick , https://github.com/jlrouzies-fr/DLSS5-Feeder (RenoDX DLSS Build 2026-09-04 / v0.2026.1003.2333, OptiScaler-DLSSNR 0.2.0-patch1)
- Erster Test: RTSS/Afterburner schließen, Smooth Motion aus, FG aus, Hook Method = Present, NVIDIA-App-Overrides aus, ReShade.log auf Evaluate-Frames prüfen. Quellen: https://dlss5mod.com/guides/install , https://paradrew.com/en/blog/kak-ustanovit-dlss-5/
- Treiber-Faustregel DLSS-5-Mods: nicht blind updaten, wenn es läuft; 616.56 sicherste Referenz; Feeder-Selbsttest `dlss5-feed-host64.exe --test` (ca. 15 s, ohne Spiel) prüft Treiber/Add-on-Kombination. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/releases/tag/v0.14.0-beta.1
- Flackern-Leiter: FG aus -> DLAA -> Scene Paper White senken -> 10-bit-Ausgabe. Quellen: https://www.nexusmods.com/stellarblade/mods/3706 , https://paradrew.com/en/blog/kak-ustanovit-dlss-5/
- ReShade neben OptiScaler: ReShade als ReShade64.dll + LoadReshade=true (offiziell dokumentiert), nicht d3d12.dll. Quelle: https://github.com/optiscaler/OptiScaler/wiki/Compatibility-with-other-mods-(Reshade,-SpecialK)

BUG-KANDIDATEN:
- renodx-dlss5 (Krish) v4.6/v4.7 faulten in nvngx_dlssnr.dll auf Treiber >=616.64 (0xC0000005, D3D12Core.dll); Fix: v6.1+ / classic v4.55 / Deep Fried Chicken / Treiber 616.56. https://github.com/jlrouzies-fr/DLSS5-Feeder/releases/tag/v0.14.0-beta.1
- RenoDX DLSS v0.2026.1003.2333: Absturz bei Hook Method Render/Upscaled (MH Wilds), Present geht. https://github.com/clshortfuse/renodx/issues/697
- RenoDX DLSS (Build 2026-09-04): Direct NR hängt auf "Waiting" bei Bridge-erzeugtem DLAA. https://github.com/clshortfuse/renodx/issues/651
- RenoDX DLSS: Present-Modus ~10 FPS langsamer in Builds nach 04.09.2026. https://github.com/clshortfuse/renodx/issues/652
- RenoDX DLSS: Flackern im skalierten NR-Pfad (Cyberpunk, Motion-Vector-Vertrag), kein offizieller Fix. https://github.com/clshortfuse/renodx/issues/642
- RenoDX DLSS v0.55: Schwarzbild bei NR Render (TLOU2, Proxy-Capture). https://github.com/clshortfuse/renodx/issues/666
- RenoDX DLSS + NVIDIA FG/DLSS-G: "one live swapchain per device" (GTA V Enhanced) / Darktide friert ein. https://github.com/clshortfuse/renodx/issues/648 , https://github.com/clshortfuse/renodx/issues/612
- ReShade+OptiScaler+RenoDX Crash (Outer Worlds), nur mit OptiFG. https://github.com/clshortfuse/renodx/issues/299
