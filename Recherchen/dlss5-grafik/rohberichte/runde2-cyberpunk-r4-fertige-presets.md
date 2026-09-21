# Web-Researcher 4/5 — Fertige DLSS-5-Konfigurationen/Presets für Cyberpunk 2077

Stand: 21.09.2026. Untersucht: Nexus Mods, GitHub, Reddit, YouTube-Beschreibungen, Blogs/News.

## Befunde

### 1. Es gibt KEINE fertige, community-geteilte OptiScaler.ini mit konkreten Cyberpunk-Werten
- Die offizielle OptiScaler-Wiki-Seite für Cyberpunk 2077 enthält **keine konkreten .ini-Werte**, nur Kompatibilitätshinweise: "Last Tested Version: 0.9.3" (OS W11 24H2, GPU RX 9070 XT); Warnung "Avoid using DLSS inputs on AMD/Intel with PT" (führt zu Rauschen), "Use either FSR or XeSS inputs" bei AMD/Intel, "Avoid FSR-FG inputs!". https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077
- Im GitHub-Issue #137 des DLSS5-Autopilot-Repos (Ersteller „pizzaguylol", 10.09.2026) werden konkrete Fixes diskutiert, aber als Troubleshooting, nicht als fertiges Preset:
  - Fix 1: RTX40MFG-Backend-Dateien direkt in `Cyberpunk 2077\bin\x64\plugins\` statt als Proxy-DLL platzieren.
  - Fix 2 gegen Flimmern: DLSS Frame Generation Override auf „Preset B" (NVIDIA App/Profile Inspector), GPU-Energieverwaltung auf „Prefer maximum performance/maximize performance", Injektionsmodus von Native auf Hook/CET wechseln.
  - Fix 3: In OptiScaler.ini FrameGen-Sektion, DLSSG-Overrides, Spoofing und Fake-NVAPI-Aufrufe deaktivieren, um natives DLSS-G zu behalten (Enabled=false, FGInput=nofg, FGOutput=nofg laut ergänzender Suche).
  - https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137
- Kizzuwatnaa/DLSS5-Autopilot selbst (705 Sterne, MIT-Lizenz, GitHub-Actions-Build, Code-Signing via SignPath Foundation — wirkt seriös als Community-Tool, aber NICHT offiziell von NVIDIA/OptiScaler/RenoDX) bietet nur generische Profile „Quality/Balanced/Performance" für alle Spiele, **keine Cyberpunk-spezifischen Presets**. https://github.com/Kizzuwatnaa/DLSS5-Autopilot

### 2. Nexus Mods — nur Installationsanleitungen, keine Preset-Dateien
- **„DLSS 5 Neural Rendering Installation Guide for Cyberpunk 2077"** (mods/33380): Konnte wegen Nexus-Bot-Schutz (HTTP 403, auch über Proxy) nicht vollständig gelesen werden; laut Suchtreffer „very quick and simple" Tutorial, keine sichtbaren Preset-Downloads. https://www.nexusmods.com/cyberpunk2077/mods/33380
- **„DLSS 5 Cyberpunk Tutorial"** (mods/33457), Autor **Coolbriggs123**, veröffentlicht 03.09.2026 18:37, letztes Update 03.09.2026 18:40: reine Installationsanleitung für RenoDX. Zitat des Autors: „Ich habe RenoDX oder die DLSS-5-Implementierung nicht erstellt. Ich erstelle diesen Leitfaden nur, um Leuten bei der Installation zu helfen." **Keine fertigen Preset-Dateien oder INI-Werte enthalten** — nur Empfehlung „mit Standardeinstellungen beginnen, dann experimentieren". Benötigte Dateien (RenoDX-Installer, DLSS-5-Dateien) müssen separat aus dem RenoDX-Discord bezogen werden. https://www.nexusmods.com/cyberpunk2077/mods/33457
- **„dlss 5 for cyberpunk 2077 (4000 and 5000 series cards)"** (mods/33280): **Aktuell NICHT verfügbar/entfernt.** Ein Abruf zeigte „The mod you were looking for was removed by a member of staff"; ein Suchtreffer beschrieb sie zuvor als „archiviert, vom Autor nicht mehr unterstützt". Status uneindeutig, aber übereinstimmend: nicht mehr zuverlässig erreichbar/zurückgezogen. **Als unseriös/nicht mehr vertrauenswürdig einstufen**, nicht zum Nachbauen empfehlen. https://www.nexusmods.com/cyberpunk2077/mods/33280

### 3. RenoDX-DLSS5-Presets speziell für Cyberpunk — nur allgemeine HDR/Tonemapper-Regler, kein DLSS-5-Support vom Mod-Maintainer
- **hdrmods.com/Cyberpunk** (Betreiber „Creepy", explizit nicht identisch mit „ShortFuse", dem RenoDX-Cyberpunk-Ersteller; Stand Juli 2026, Referenz auf RenoDX-Release „PsychoV-17" vom 05.04.2026) nennt konkrete RenoDX-Tonemapper-Werte für Cyberpunk 2077:
  - Tone Mapper: PsychoV-17
  - Peak Brightness: laut Windows-HDR-Kalibrierung
  - Game Brightness: 100–300 nits (Geschmackssache)
  - SDR EOTF Emulation: „UI/Menu Only" empfohlen
  - Cone Response: 50
  - Blowout: 0
  - Dynamic Exposure: 0
  - LUT Strength: 50–60 (bei Standard-LUTs)
  - Film Grain Strength: nach Bedarf gegen Banding
  - **Wichtig:** Die Seite schreibt ausdrücklich **„DLSS 5 SUPPORT IS NOT PROVIDED!!!"** und verweist stattdessen auf die Ultrapunk-Modliste. Diese Werte sind also HDR/Tonemapping-Presets, **kein DLSS-5-Preset**. https://www.hdrmods.com/Cyberpunk

### 4. Verdächtige/unseriöse Quelle identifiziert
- **github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games**: klare Warnsignale — generischer, aus Suchbegriffen zusammengesetzter Account-Name „rrdlss5renoxdx", nur 3 Commits, 1 Stern, 0 Watcher, „Windows free download"-Formulierung, verlangt Admin-Rechte und schreibt DLLs direkt ins Spielverzeichnis, keine Sicherheits-/Quellcode-Transparenz, listet Cyberpunk 2077 ohne konkrete Einstellungen. **Einschätzung: potenziell Fake/Malware, NICHT nutzen, NICHT downloaden.** https://github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games

### 5. DLSS5-Feeder / DLSS5-Swapper / Deep Fried Chicken (alternative Wege) — generische, nicht Cyberpunk-spezifische Konfigwerte
- **jlrouzies-fr/DLSS5-Feeder** (GitHub): für Spiele ohne natives DLSS. Konfiguration: ReShade-Overlay (Home-Taste) → `DLSS5_Feed.fx` auswählen → `DLSS5_MV_PROVIDER = 3` in Preprocessor-Definitionen setzen → Effekte neu laden. Im Spiel „LUMENITE: Kernel 2.0" aktivieren, danach „DLSS 5 Feed", danach Neural Rendering im Consumer-Panel (Deep Fried Chicken-Tab). MSAA/SSAA im Spiel abschalten. Schreibt automatisch `EnableHooks=2`, `NeuralUplift=1`, `NREnableUpscaling=0`. **Generisch, keine Cyberpunk-2077-spezifischen Werte gefunden.** https://github.com/jlrouzies-fr/DLSS5-Feeder
- **rakanki911/DLSS5-Swapper** (GitHub): Ein-Klick-Installer/Tuner für RenoDX, DLSS5-Feeder, OptiScaler u. a., mit „community page showing what works on your games and your graphics card" — potenziell die beste Quelle für crowdgesourcte Cyberpunk-Werte, aber im Rahmen dieser Recherche keine konkreten dort hinterlegten Cyberpunk-Werte einsehbar (Rate-Limit/Zeitrahmen). https://github.com/rakanki911/DLSS5-Swapper

### 6. Videos, die Cyberpunk 2077 mit DLSS-5-Mod zeigen (Kanal/Datum wo ermittelbar)
Alle folgenden erschienen laut Suchergebnis vor ca. 1–3 Wochen (Publikationsdatum durch YouTube-Bot-Schutz nicht exakt verifizierbar, nur relative Angabe aus der Suche):
- „DLSS 5 Neural Rendering in Cyberpunk 2077 – Graphics Comparison" — https://www.youtube.com/watch?v=s8pk_1i0i4s (Side-by-Side ON/OFF)
- „NVIDIA DLSS 5 Neural Rendering in Cyberpunk 2077 – Graphics/Performance Comparison | RTX 5080" — https://www.youtube.com/watch?v=tTowPWp3cQc (Benchmark 4K/1440p)
- „DLSS 5 Neural Rendering in Cyberpunk 2077 – Path Tracing & Ray Tracing | RTX 5080" — https://www.youtube.com/watch?v=ILuQwdMuF3U — nutzt laut Suchtreffer explizit den **RenoDX-DLSS-5-Mod**
- „DLSS 4.5 vs DLSS 5 in Cyberpunk 2077" — https://www.youtube.com/watch?v=_MAfCP_qHBw (4K, Path Tracing maxed)
- „NVIDIA DLSS 4.5 VS DLSS 5 Neural Rendering Test In Cyberpunk 2077 | RTX 5090" — https://www.youtube.com/watch?v=AWA31K6Z050
- „DLSS 5 Neural Rendering Test In Cyberpunk 2077 | RTX 5090" — https://www.youtube.com/watch?v=fsAf2rzBDOg
- „Is DLSS 5 Neural Rendering the Ultimate Cyberpunk 2077 Experience? (RTX 5090)" — https://www.youtube.com/watch?v=L5O9Y8SNJKs
- „DLSS 5 in Cyberpunk 2077 (5070ti, Max Settings)" — https://www.youtube.com/watch?v=pLzz_THypg0
- „DLSS 4.5 Update, Recommended Override settings | Cyberpunk 2077 | RTX 5090" — https://www.youtube.com/watch?v=CABMTxfi-a8 (Titel deutet auf konkrete Override-Empfehlungen hin, Videobeschreibung wegen YouTube-Bot-Schutz nicht auslesbar — Nutzer sollte selbst reinschauen)
- „How To Install RenoDX DLSS 5 Neural Rendering: Cyberpunk 2077 On RTX 5060" — https://www.youtube.com/watch?v=teS-bgQi9uo
- Genaue Kanalnamen und Einstellungswerte aus den Videobeschreibungen konnten wegen YouTube-Zugriffsbeschränkungen (nur Footer/Navigation abrufbar) NICHT extrahiert werden — hierzu wäre ein direkter Videoaufruf durch den Nutzer nötig.

### 7. Performance- und Qualitätsbewertung (aus Tests/Reviews, nicht aus Presets)
- RTX 5090, 4K, DLSS Quality: Einbruch von 63 auf 42 fps mit DLSS 5 in Cyberpunk 2077 (gamegpu.com, via Suchergebnis). https://en.gamegpu.com/news/igry/cyberpunk-2077-v-4k-dlss-quality-s-dlss-5-pokazal-padenie-do-42-fps-na-rtx-5090
- RTX 5060: Einbruch von 86 auf 34 fps laut MindStudio-Blogartikel (Autor Luis Chavez-Mattos, 29.08.2026), der drei Preset-Modi nennt (Standard, Natural, Cinematic) plus Regler „skin structure strength" und „local structure strength" — aber ohne Cyberpunk-spezifische Werte. Bewertung: Gesichtsdetails, Haut- und Umgebungslicht „much more photorealistic", Cinematic-Preset „am photorealistischsten" mit Farbabstufungen, aber Artefakte in Reflexionen (Beispiel Skyrim) als Hinweis auf unvollständige neuronale Rekonstruktion. https://www.mindstudio.ai/blog/dlss-5-neural-rendering-hands-on
- „AI-Look"-Kritik (Medium, Artikel „DLSS 5 in Cyberpunk 2077 Looks Amazing Until You Move", Volltext wegen 403 nicht abrufbar, nur Titel/Snippet verfügbar): laut Suchsnippet wirkt DLSS 5 stellenweise „over-processed, slightly CGI-like", v. a. bei Gesichtern/Augen/Haut; Night City lebt von „saturated neon, stylized lighting... a color palette that feels designed rather than simply observed" — DLSS 5 drängt das Bild Richtung fotografischen Realismus, was als subjektiv verbessert oder verschlechtert bewertet wird. Performance-Beispiel dort: 111 → 55 fps. https://medium.com/beyond-the-game/cyberpunk-2077-dlss-5-bd44b8933d73
- Tom's Guide (Titel „I tested the real DLSS 5 — forget the leaked mods, it's not an AI filter"): grenzt offizielle NVIDIA-DLSS-5-Technik ausdrücklich von den „geleakten Mods" ab; Volltext wegen Kürzung nicht vollständig auswertbar, Cyberpunk im abgerufenen Auszug nicht erwähnt, aber Titel signalisiert Skepsis gegenüber inoffiziellen Mod-Wegen generell. https://www.tomsguide.com/gaming/pc-gaming/i-tested-the-real-dlss-5-forget-the-leaked-mods-its-not-an-ai-filter-this-is-nvidias-photorealistic-game-changer
- dlss5studio.com/en/game/cyberpunk-2077: transparentes, nicht-kommerzielles Bildungsprojekt („not affiliated with NVIDIA"), bietet Vorher/Nachher-Slider mit Cyberpunk-Beispielen, aber **keine herunterladbaren Presets**, keine Analysetexte zu Gesichtern/Neon/Regen — nur visueller Vergleich; Nutzer kann eigenes Material hochladen (180 s kostenlose GPU-Zeit ohne Login). https://dlss5studio.com/en/game/cyberpunk-2077

## BEST-PRACTICES-KANDIDATEN
- OptiScaler.ini FrameGen-Sektion bei aktivem nativen DLSS-G deaktivieren (Enabled=false, FGInput=nofg, FGOutput=nofg), um Konflikte mit Cyberpunks eigenem Frame Generation zu vermeiden. Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137 (Version: OptiScaler, Bezug Cyberpunk 2077, Stand 10.09.2026)
- DLSS Frame Generation Override auf „Preset B" (NVIDIA App/Profile Inspector) + GPU-Energieverwaltung auf „Prefer maximum performance" gegen temporales Flimmern bei DLSS5+OptiScaler in Cyberpunk. Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137
- RTX40MFG-Backend-Dateien direkt in `Cyberpunk 2077\bin\x64\plugins\` statt als Proxy-DLL platzieren, da das Spiel lokale D3D12-Proxies ignoriert. Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137
- Bei AMD/Intel-GPUs mit Path Tracing keine DLSS-Inputs in OptiScaler verwenden (Rauschen), stattdessen FSR/XeSS; FSR-FG-Inputs generell vermeiden. Quelle: https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077 (Stand: OptiScaler 0.9.3, W11 24H2)

## BUG-KANDIDATEN
- Nexus-Mod „dlss 5 for cyberpunk 2077 (4000 and 5000 series cards)" (mods/33280) aktuell entfernt/nicht erreichbar — nicht als verlässliche Quelle nutzen. https://www.nexusmods.com/cyberpunk2077/mods/33280
- github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games — Verdacht auf Fake/Malware-Repo (Admin-Rechte, DLL-Injektion ins Spielverzeichnis, kaum Aktivität, generischer Name). NICHT verwenden. https://github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games
- Temporales Flimmern bei DLSS5 + OptiScaler + RTX40MFG-Backend in Cyberpunk 2077, gemeldet 10.09.2026, mit Workaround (Preset B + Energieverwaltung + Hook/CET-Injektion). https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137
- Der offizielle RenoDX-HDR-Mod-Maintainer für Cyberpunk 2077 (hdrmods.com, Betreiber „Creepy") distanziert sich ausdrücklich von DLSS-5-Support („DLSS 5 SUPPORT IS NOT PROVIDED!!!") — Hinweis, dass DLSS-5-Integration in Cyberpunk außerhalb des offiziellen RenoDX-HDR-Werkzeugs läuft und dort keinen Support genießt. https://www.hdrmods.com/Cyberpunk

## OFFEN/UNSICHER
- Nexus-Mods-Seiten (mods/33380, teils 33457/33280) waren wegen Bot-/Cloudflare-Schutz (HTTP 403) nur eingeschränkt bzw. gar nicht per Direktzugriff auswertbar; Angaben stammen teils nur aus Suchmaschinen-Snippets, nicht aus Volltext-Verifikation. Empfehlung: bei Bedarf manuell im Browser prüfen.
- YouTube-Videobeschreibungen (Kanalname, exaktes Datum, konkrete Einstellungswerte) waren wegen YouTube-Zugriffsbeschränkungen im Fetch nicht auslesbar — nur Titel aus der Suche bekannt. Für „welche Einstellungen die viral gegangenen Vergleiche nutzten" bräuchte es einen manuellen Blick in die Videobeschreibungen/Pinned Comments.
- Keine Discord-Zusammenfassungen einsehbar (RenoDX-Discord discord.gg/jz6ujVpgFB erwähnt als Quelle für aktuelle DLSS-5-Dateien, aber Inhalt nicht zugänglich ohne Beitritt).
- Status von Nexus-Mod 33280 (entfernt vom Autor vs. von Nexus-Staff) bleibt uneindeutig — unterschiedliche Angaben in Such-Snippet vs. Proxy-Abruf.
- rakanki911/DLSS5-Swapper hat laut Beschreibung eine „community page showing what works on your games" — dort könnten crowdgesourcte Cyberpunk-Werte liegen, wurde aber im Zeitrahmen dieser Recherche nicht inhaltlich geprüft.
- Medium-Artikel und Tom's-Guide-Artikel konnten wegen 403/Kürzung nicht vollständig gelesen werden — Aussagen basieren auf Titel/Snippet, nicht auf Volltext-Verifikation.

## Quellenliste (alle verwendeten URLs)
- https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077
- https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137
- https://github.com/Kizzuwatnaa/DLSS5-Autopilot
- https://www.nexusmods.com/cyberpunk2077/mods/33380
- https://www.nexusmods.com/cyberpunk2077/mods/33457
- https://www.nexusmods.com/cyberpunk2077/mods/33280
- https://github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games
- https://www.hdrmods.com/Cyberpunk
- https://github.com/jlrouzies-fr/DLSS5-Feeder
- https://github.com/rakanki911/DLSS5-Swapper
- https://dlss5studio.com/en/game/cyberpunk-2077
- https://medium.com/beyond-the-game/cyberpunk-2077-dlss-5-bd44b8933d73
- https://www.mindstudio.ai/blog/dlss-5-neural-rendering-hands-on
- https://www.tomsguide.com/gaming/pc-gaming/i-tested-the-real-dlss-5-forget-the-leaked-mods-its-not-an-ai-filter-this-is-nvidias-photorealistic-game-changer
- https://en.gamegpu.com/news/igry/cyberpunk-2077-v-4k-dlss-quality-s-dlss-5-pokazal-padenie-do-42-fps-na-rtx-5090
- https://www.youtube.com/watch?v=s8pk_1i0i4s
- https://www.youtube.com/watch?v=tTowPWp3cQc
- https://www.youtube.com/watch?v=ILuQwdMuF3U
- https://www.youtube.com/watch?v=_MAfCP_qHBw
- https://www.youtube.com/watch?v=AWA31K6Z050
- https://www.youtube.com/watch?v=fsAf2rzBDOg
- https://www.youtube.com/watch?v=L5O9Y8SNJKs
- https://www.youtube.com/watch?v=pLzz_THypg0
- https://www.youtube.com/watch?v=CABMTxfi-a8
- https://www.youtube.com/watch?v=teS-bgQi9uo
