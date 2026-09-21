# Web-Researcher 3/5 — Realismus-Grafikmods für Cyberpunk 2.31 + Kompatibilität mit OptiScaler-DLSSNR

Kontext: RTX 5090, Cyberpunk 2077 v2.31 (Steam), bereits installiert: OptiScaler-DLSSNR v0.2.0 als `dxgi.dll` in `bin\x64`.

## Wichtigster Befund zuerst (KRITISCH für dieses Setup)

Es gibt einen dokumentierten GitHub-Issue, der **exakt** die Kombination des Nutzers betrifft:
Ein Nutzer meldet mit **Cyberpunk 2077 (Steam) v2.3.1, OptiScaler + DLSSNR v0.2.0, RedScript 0.5.3.1, ReShade 6.6.2**, dass RedScript-Mods (also alles, was auf CET/RED4ext/RedScript aufsetzt — inkl. Ultra+, Nova City, viele Gameplay-/Lighting-Mods) **sofort kaputtgeht, sobald OptiScaler.dll als `dxgi.dll` installiert wird**, während sie mit reinem ReShade 6.6.2 (auf `d3d12.dll` umbenannt) funktionieren. Der Issue wurde ohne dokumentierte Lösung geschlossen, es ist unklar ob OptiScaler oder ReShade schuld ist.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/18

Ein weiterer, verwandter Workaround-Thread im Haupt-OptiScaler-Repo (anderes Spiel/Setup, aber gleiches Funktionsprinzip) nennt als Lösung: **OptiScaler als `OptiScaler.asi` über den ASI-Loader in `bin/x64/plugins/` laden statt als `dxgi.dll`**, dazu die nötigen Companion-DLLs (`amd_fidelityfx_dx12.dll`, `amdxcffx64.dll`, `libxess.dll`) — kostet aber ca. 10 FPS gegenüber der `dxgi.dll`-Variante.
Quelle: https://github.com/optiscaler/OptiScaler/discussions/353 (Issue #313 referenziert)

Fazit: **Der DLSS-5-Mod des Nutzers (dxgi.dll-Methode) ist mit CET/RedScript-basierten Realismus-Mods potenziell inkompatibel.** Das betrifft direkt Ultra+ (nutzt CET-Overlay + RED4ext) und alle Nova-City-artigen Weather/Lighting-Mods, die auf Codeware/RedScript aufbauen.

## ReShade + OptiScaler: dxgi.dll-Kollision (allgemein bestätigt)

Beide Tools wollen `dxgi.dll` sein → Konflikt beim Start. Community-Standardlösung, in mehreren Threads bestätigt:
- ReShades `dxgi.dll` umbenennen zu **`d3d12.dll`** (für Cyberpunk 2077 explizit bestätigt, da DX12-Titel) — dann funktioniert das ReShade-Overlay parallel zu OptiScaler.
- Alternative: OptiScaler.ini `LoadReshade=true` setzen, damit OptiScaler ReShade64.dll selbst nachlädt — laut einem Nutzerbericht hat das bei ihm NICHT funktioniert.
Quellen: https://github.com/optiscaler/OptiScaler/discussions/473 , https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077

Offizielle Installationsanleitung des DLSS-5-Nexus-Mods (mods/33380) warnt zudem ausdrücklich vor Doppelinstallation: „Do not use both methods at the same time; if you want to use OptiScaler but already had the ReShade method installed, simply delete `dlss5.addon64` from `bin/x64` and follow the OptiScaler instructions.” → Das bestätigt: ReShade-basierte DLSS5-Injektion (`dlss5.addon64`) und die OptiScaler-DLSSNR-Route sind zwei sich ausschließende Installationswege, kein Doppel-Betrieb.
Quelle: https://www.nexusmods.com/cyberpunk2077/mods/33380

OptiScaler-Wiki-Seite zu Cyberpunk 2077 (getestet mit v0.9.3, RX 9070 XT) geht nicht auf CET/RedScript/ReShade-Konflikte ein, warnt aber: DLSS-Input auf AMD/Intel mit Path Tracing meiden (Bildfehler → FSR/XeSS nutzen), FSR-Frame-Generation meiden.
Quelle: https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077

## Tabelle: Mod | Zweck | Version/2.31 | Abhängigkeiten | Konflikt mit OptiScaler | Link

| Mod | Zweck | Version / Stand 2.31 | Abhängigkeiten | Konflikt mit OptiScaler-DLSSNR | Link |
|---|---|---|---|---|---|
| Nova LUT 4.0 (AgX – New HDR) | Farbgrading/Tonemapping (AgX statt Standard-Filmic) | v4.0, zuletzt aktualisiert 10.05.2026; „Nova Enhanced Night City 2.31“-Collection existiert explizit für 2.31 | LUT Switcher 3 (Realtime/Weather/Menu), reines LUT-Ersetzungssystem, KEIN ReShade nötig | Kein direkter Konflikt bekannt — arbeitet über eigenes In-Game-LUT-System, nicht über dxgi.dll. Kann sich aber mit dem Farbgrading eines gleichzeitig aktiven fotorealistischen ReShade-Presets doppeln/überlagern | https://www.nexusmods.com/cyberpunk2077/mods/11622 |
| LUT Switcher 3 | Trägersystem für Nova LUT (Echtzeit-Wechsel je Wetter/Menü) | v1.4.0n Nova-LUT-Pack, 10.05.2026 | Codeware/CET vermutlich (LUT-Switching via CET-Overlay) | Betroffen vom o.g. CET/RedScript-Problem, falls es über CET/RED4ext läuft | https://www.nexusmods.com/cyberpunk2077/mods/16310 |
| Ultra Plus (Ultra+) — Physically Accurate Path Tracing | Path-Tracing-Overhaul: eigenes Lighting-System (v9 „V4 Lighting“), RR-Denoiser-Presets „Clean“/„Sharp“, PT21/PTNextV2-Modi, bis zu 40 % mehr Performance bei besserer Qualität | Bis mind. v8.1.0 (März 2026, AMD-PT-Smearing-Fixes), v9 als größtes Update genannt (Lighting-Rewrite in C++); explizite 2.31-Versionsnummer nicht auffindbar | **Cyber Engine Tweaks (CET) + RED4ext zwingend** (In-Game-CET-Overlay), DLSS 310.6+ für RR-Presets | **HOHES Risiko**: läuft über CET-Overlay/RED4ext — genau die Kette, die laut Issue #18 durch OptiScaler-DLSSNR v0.2.0 bricht | https://www.nexusmods.com/cyberpunk2077/mods/10490 , offizielle Doku: https://theultraplace.com/games/cyberpunk2077/ |
| Nova City 2 (Custom Weather and Lighting – Exposure Overhaul) | Wetter-/Beleuchtungssystem, neue Wetterzustände, Belichtungs-Overhaul | v2.3.2, zuletzt aktualisiert 22.08.2026 (Autor CyanideX) | Vermutlich CET/Codeware-basiert (wie die meisten Weather-Mods) | Gleiches CET/RED4ext-Risiko wie Ultra+ (nicht explizit im Issue erwähnt, aber technisch gleiche Mod-Klasse) | https://www.nexusmods.com/cyberpunk2077/mods/12490 |
| Cyberpunk 2077 HD Reworked Project (HDRP) 2.0 | Textur-Rework (Umgebung, Architektur, Graffiti etc.) in „balanced“ und „ultra“-Variante | v2.0, Release Okt. 2023, laut Modseite „kompatibel ab 2.1+“; Community berichtet Juni 2026 von Installationsproblemen unter 2.31 (Workaround: erst auf 2.21 installieren, dann auf 2.31 updaten) | Reine Archive-Datei (`.archive`), keine CET/RED4ext-Abhängigkeit dokumentiert | Kein Konflikt — reine Textur-Assets, keine DLL/Renderer-Interaktion | https://www.nexusmods.com/cyberpunk2077/mods/7652 |
| Environment Textures Overhaul (ETO) | Ergänzt/ersetzt HDRP für Umgebungstexturen | aktiv 2026, im selben Installations-Workaround-Kontext wie HDRP genannt | Archive-Datei | Kein Konflikt | https://www.nexusmods.com/cyberpunk2077/mods/13372 |
| Realistic Complexion II/III, Faces of Night City, 8K-4K Skin Texture | Fotorealistische Haut-/Gesichtstexturen (4K–8K), 72 Hauttöne, Poren/Adern/Male | aktiv, mehrere Varianten je Hautfarbe/Auflösung | teils „Universal Skin Tone“-Basismod nötig | Kein Konflikt — reine Texturen | https://www.nexusmods.com/cyberpunk2077/mods/15634 , https://www.nexusmods.com/cyberpunk2077/mods/19314 , https://www.nexusmods.com/cyberpunk2077/mods/4301 , https://www.nexusmods.com/cyberpunk2077/mods/13537 |
| Photorealistic Cyberpunk 2077 (ReShade-Preset) | ReShade-Preset für fotorealistischen Look | zuletzt aktualisiert 07.02.2026 | ReShade (verwendet `dxgi.dll` standardmäßig!) | **Direkter dxgi.dll-Konflikt** mit OptiScaler → ReShade muss auf `d3d12.dll` umbenannt werden | https://www.nexusmods.com/cyberpunk2077/mods/27398 |
| CyberDreams Reality ReShade 8K RTX 5090 Visual Overhaul | ReShade-Preset, explizit für RTX-5090-Look ausgelegt, „echtes“ Farbverhalten statt Übersättigung, content-creator-tauglich | v1.0 | ReShade | **Direkter dxgi.dll-Konflikt**, gleiche Umbenennungspflicht wie oben | https://www.nexusmods.com/cyberpunk2077/mods/28729 |
| Cyberpunk 2077 (RTGI) Reshade by Sublime / Ultimate CP2077 Raytracing Reshade | RTGI-artige Zusatz-GI via ReShade (imitiertes Raytracing on top von echtem RT/PT) | mehrere aktive Varianten (Standard + „High Performance“) | ReShade | **Direkter dxgi.dll-Konflikt**, zusätzlich: bei bereits aktivem Path Tracing (RT Overdrive) ist ein zusätzliches RTGI-Preset meist gegenläufig/überflüssig (doppelte GI) | https://www.nexusmods.com/cyberpunk2077/mods/26 , https://www.nexusmods.com/cyberpunk2077/mods/119 , https://www.nexusmods.com/cyberpunk2077/mods/10576 |
| Cyberpunk Render Tweaks | ReShade-Addon, das gezielt einzelne Shader „live“ ersetzt (Himmel/Reflexionen, Wolken, Nässe, RT-Bäume) statt komplettes Preset | aktiver Build, Stand 16.09.2026 oder neuer | ReShade | **Direkter dxgi.dll-Konflikt** wie alle ReShade-Tools | https://www.nexusmods.com/cyberpunk2077/mods/33943 |
| DreamPunk 2.1 / Dreampunk-Collections (3.3, 3.4.1.1) | Kuratierte Gesamt-Modlisten: Path Tracing + Lighting + Nova-LUT-artiges Farbgrading/Diffusion/Sättigung, 8K-Auflösung als Ziel | DreamPunk 2.1 (Basisartikel), neuere Collections 3.3/3.4.1.1 auf Nexus | Ganzes Ökosystem: CET, RED4ext, ArchiveXL, Codeware, TweakXL + o.g. Einzelmods | Trägt dasselbe CET/RED4ext-Risiko wie Ultra+/Nova City, da es diese Mods bündelt | https://www.nexusmods.com/games/cyberpunk2077/collections/eidgvp , https://www.nexusmods.com/games/cyberpunk2077/collections/j8ehj8 |

## Installationsweg (allgemein, aus den Quellen)

- **ReShade-Presets** (Photorealistic, CyberDreams Reality, RTGI by Sublime, Render Tweaks): manuelle Installation über reshade.me-Installer, `.exe` wählt Cyberpunk2077.exe als Ziel, API DirectX 12. **Wegen OptiScaler-Kollision: ReShades `dxgi.dll` danach manuell in `d3d12.dll` umbenennen.**
- **Nova LUT / LUT Switcher / Ultra+ / Nova City**: i. d. R. manuell über Vortex oder Datei-Kopie in `archive\pc\mod` bzw. `bin\x64\plugins\cyber_engine_tweaks\mods`, da sie CET-Mods sind — **benötigen CET + RED4ext als Grundgerüst**, viele davon zusätzlich ArchiveXL/Codeware/TweakXL.
- **HDRP/ETO/Skin-Texturen**: reine `.archive`-Dateien nach `archive\pc\mod`, keine Frameworks nötig.

## Community-Empfehlung „realistischster Look“

Wiederkehrende Kombination in mehreren Quellen: **Ultra+ (Path Tracing/Lighting-Basis) + Nova LUT/Nova City (Farbgrading + Wetter) + HDRP/ETO (Texturen) + optional ein fotorealistisches ReShade-Preset „on top“** (z. B. CyberDreams Reality oder Photorealistic Cyberpunk 2077). Ein Showcase-Video kombiniert explizit „Nova City 2 – Ultra Plus PTNext – Nova 3 LUT“.
Quellen: https://www.nexusmods.com/cyberpunk2077/videos/3504 , https://www.notebookcheck.net/Cyberpunk-2077-sees-up-to-a-40-performance-boost-with-new-optimized-path-tracing-mod.874098.0.html

## Doppelung / Überflüssigkeit ggü. dem DLSS-5-Pass

- Der DLSS-5-Mod (OptiScaler-DLSSNR) ersetzt/verbessert **Upscaling + Ray Reconstruction + Frame Generation**, macht aber KEIN Farbgrading. Nova LUT und ReShade-Farbgrading-Presets (Photorealistic, CyberDreams Reality) doppeln sich **untereinander** (zwei LUTs/Grading-Layer übereinander = unrealistische Übersättigung), nicht mit dem DLSS-Pass selbst.
- Ultra+ bietet eigene RR-Denoiser-Presets „RR Clean“/„RR Sharp“, die **direkt mit DLSS Ray Reconstruction interagieren** (Ultra+ dokumentiert Tuning für „DLSS Ray Reconstruction v4.5“) — hier ist eine echte Funktionsüberschneidung mit dem, was DLSS-5/DLSSNR ebenfalls steuert (Denoising-Qualität). Beide gleichzeitig aktiv zu justieren kann zu widersprüchlichen Einstellungen führen.
- RTGI-ReShade-Presets (Sublime, Ultimate Raytracing Reshade) fügen zusätzliche Pseudo-GI hinzu — bei bereits aktivem Path Tracing (RT Overdrive, das der Nutzer vermutlich nutzt) ist das meist **kontraproduktiv/überflüssig**, da echtes PT bereits globale Beleuchtung physikalisch korrekt berechnet.

## BEST-PRACTICES-KANDIDATEN:
- ReShade + OptiScaler unter Cyberpunk 2077: ReShades `dxgi.dll` zu `d3d12.dll` umbenennen, damit beide parallel laufen (DX12-Titel). Quelle: https://github.com/optiscaler/OptiScaler/discussions/473 , https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077 (Stand: OptiScaler getestet mit v0.9.3)
- Bei DLSS-5/OptiScaler-Installation via Nexus-Guide: nicht gleichzeitig die ReShade-Methode (`dlss5.addon64`) UND die OptiScaler-Methode installieren — sich ausschließende Wege. Quelle: https://www.nexusmods.com/cyberpunk2077/mods/33380

## BUG-KANDIDATEN:
- **OptiScaler + DLSSNR v0.2.0 (als `dxgi.dll`) bricht RedScript-Mods** in Cyberpunk 2077 v2.3.1 (getestet mit RedScript 0.5.3.1, ReShade 6.6.2) — betrifft praktisch alle CET/RED4ext-basierten Realismus-Mods (Ultra+, Nova City, DreamPunk-Collections). Kein dokumentierter Fix im Issue. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/18 (Version: OptiScaler_DLSSNR v0.2.0, Spiel 2.3.1)
- Verwandtes Muster (anderes Setup/Modpack, gleiches Prinzip): OptiScaler als `dxgi.dll` kollidiert mit RED4ext/CET-Plugin-Hooks; Workaround = OptiScaler als `.asi` über ASI-Loader statt `dxgi.dll`, kostet ca. 10 FPS. Quelle: https://github.com/optiscaler/OptiScaler/discussions/353 (Spielversion 3.0.78.57301, RED4ext 1.27.0)
- HD Reworked Project: Community meldet Juni 2026 Installationsprobleme unter 2.31 (Texturen greifen nicht), Workaround = Downgrade auf 2.21, HDRP/ETO installieren, dann wieder auf 2.31 updaten. Quelle: (aus WebSearch-Snippet zu nexusmods.com/cyberpunk2077/mods/7652, genauer Post nicht einzeln verifizierbar wegen 403-Sperre)

## OFFEN/UNSICHER:
- Nexus-Mod-Seiten selbst waren per WebFetch durchgehend mit HTTP 403 gesperrt (Nexus blockt automatisierte Fetches) — alle Nexus-Angaben stammen aus WebSearch-Snippets, nicht aus verifizierten Volltext-Seiten. Genaue Kompatibilitätsvermerke „getestet mit 2.31“ auf den einzelnen Mod-Seiten konnten dadurch NICHT direkt eingesehen werden.
- Ob Issue #18 (OptiScaler_DLSSNR bricht RedScript) inzwischen (nach dem 21.09.2026 sichtbaren Stand) gefixt wurde, ist unklar — der Issue-Thread endete ohne dokumentierte Lösung.
- Explizite 2.31-Versionsnummer für Ultra+ (aktuellste Version, ob v9 bereits final oder noch Beta) konnte nicht zweifelsfrei ermittelt werden.
- Ob Nova City 2 und LUT Switcher 3 tatsächlich über CET/RED4ext laufen (und damit vom Issue-#18-Problem betroffen wären) ist eine plausible Annahme aus der Mod-Klasse heraus, aber nicht durch eine explizite Quelle bestätigt.
- Keine Quelle bestätigt oder verneint explizit, ob CyberDreams Reality / Photorealistic-ReShade-Presets speziell mit RTX 5090 + DLSS5-Pass getestet wurden (CyberDreams Reality wirbt nur mit „RTX-5090-Look“ als Bildästhetik, nicht als getestete Hardware-Kompatibilität).
