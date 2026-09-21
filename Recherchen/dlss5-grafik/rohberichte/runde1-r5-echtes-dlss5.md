# Web-Researcher 5/7 — Echtes DLSS 5, Stand 21.09.2026

## 1. Was DLSS 5 ist und wann es startete

- NVIDIA kündigte DLSS 5 als "3D-Guided Neural Rendering" auf der GTC 2026 an (Pressemitteilung vom 16.03.2026): [NVIDIA DLSS 5 Delivers AI-Powered Breakthrough in Visual Fidelity for Games](https://nvidianews.nvidia.com/news/nvidia-dlss-5-delivers-ai-powered-breakthrough-in-visual-fidelity-for-games) / [Spiegel-Artikel auf nvidia.com](https://www.nvidia.com/en-us/geforce/news/dlss5-breakthrough-in-visual-fidelity-for-games/). DLSS 5 ist **kein** klassisches Upscaling/Frame-Generation, sondern ein generatives Neural-Rendering-Modell, das Beleuchtung, Material, Haut, Haare etc. anhand der 3D-Engine-Daten des Spiels neu erzeugt — es muss pro Spiel vom Entwickler integriert werden.
- Der reale Marktstart erfolgte am **03.09.2026, 21:00 Uhr Pacific**, exklusiv in **NBA 2K27**, zusammen mit dem GeForce Game Ready Treiber **616.64 WHQL**: [GeForce Game Ready Driver 616.64: DLSS 5 in NBA 2K27 (nvidia.com)](https://www.nvidia.com/en-us/geforce/news/nba-2k27-dlss-5-3d-guided-neural-rendering-geforce-game-ready-driver/), [VideoCardz](https://videocardz.com/newz/nvidia-dlss-5-launches-in-nba-2k27-rtx-40-support-is-officially-coming), [Tom's Hardware](https://www.tomshardware.com/pc-components/gpus/dlss-5-officially-launches-inside-nba-2k27-limited-to-rtx-50-series-gpus-for-now-nvidia-promises-to-bring-neutral-rendering-tech-to-rtx-40-series-soon).
- Stand heute (21.09.2026) ist **NBA 2K27 das einzige Spiel mit tatsächlich live spielbarem DLSS 5**. Alle anderen genannten Titel sind angekündigt, aber ohne verifizierten Patch: [DLSS 5 Games Tracker – dlss5.net](https://www.dlss5.net/dlss-5-games), [itechguides – "What NVIDIA has actually announced"](https://www.itechguides.com/dlss-5-supported-gpus-and-confirmed-games-list-what-nvidia-has-actually-announced/).

## 2. Offizielle NVIDIA-Liste angekündigter Spiele (Stand GTC 2026 / Herbst 2026)

Aus der offiziellen NVIDIA-Pressemitteilung (nvidianews.nvidia.com, 16.03.2026) und der GeForce-News-Seite, jeweils ohne konkrete Einzeltermine ("this fall"):
AION 2, **Assassin's Creed Shadows**, Black State, CINDER CITY, Delta Force, EA Sports FC, Hogwarts Legacy, Justice, NARAKA: BLADEPOINT, NTE: Neverness to Everness, Phantom Blade Zero, Resident Evil Requiem, Sea of Remnants, **Starfield**, The Elder Scrolls IV: Oblivion Remastered, Where Winds Meet — "and more".
Quellen: [nvidianews.nvidia.com Pressemitteilung](https://nvidianews.nvidia.com/news/nvidia-dlss-5-delivers-ai-powered-breakthrough-in-visual-fidelity-for-games), [PC Guide Liste (Update 17.03.2026)](https://www.pcguide.com/news/dlss-5-games-list/), [OnMSFT Zusammenfassung](https://onmsft.com/news/nvidia-dlss-5-games-supported-list-all-confirmed-titles/).

Publisher, die DLSS 5 laut NVIDIA unterstützen werden: Bethesda, CAPCOM, Hotta Studio, NetEase, NCSOFT, S-GAME, Tencent, Ubisoft, Warner Bros. Games — Quelle wie oben.

**WICHTIG zur Einordnung:** itechguides betont ausdrücklich: "Ein Ankündigungstitel bedeutet nicht, dass ein Patch bereits vorhanden ist oder Systemanforderungen bekannt sind" — [itechguides](https://www.itechguides.com/dlss-5-supported-gpus-and-confirmed-games-list-what-nvidia-has-actually-announced/). Für keinen der angekündigten Titel (auch nicht Starfield, Assassin's Creed Shadows, Delta Force) gibt es bislang ein bestätigtes Patch-Datum — nur Tech-Demos/Vergleichsvideos von NVIDIA (z. B. GeForce-Mitarbeiter Jacob Freeman auf X zu AC Shadows: [x.com/GeForce_JacobF](https://x.com/GeForce_JacobF/status/2033650480560422985); frühe Starfield-Aufnahmen: [Insider Gaming](https://insider-gaming.com/heres-what-nvidia-dlss-5-looks-like-in-starfield-right-now/), [GameGPU](https://en.gamegpu.com/news/igry/v-seti-poyavilis-novye-kadry-raboty-dlss-5-v-starfield)).

## 3. Abgleich mit den installierten Spielen des Nutzers

| Spiel | Auf offizieller DLSS-5-Liste? | Patch-Termin |
|---|---|---|
| **Starfield** | Ja (angekündigt) | kein Termin, nur Preview-Footage |
| **Assassin's Creed Shadows** | Ja (angekündigt) | kein Termin, nur GeForce-Vergleichsvideos |
| **Delta Force** | Ja (angekündigt) | kein Termin |
| Cyberpunk 2077 | Nein — auf keiner der geprüften Listen (offizielle PM, PC Guide, GameGPU DLSS-4.5/5-Liste, dlss5.net-Tracker) | — |
| Manor Lords | Nein | — |
| Star Trek: Voyager – Across the Unknown | Nein | — |
| Civilization VII | Nein | — |
| DREADZONE | Nein | — |
| Battlefield 6 | Nein | — |
| Call of Duty Black Ops 7 (COD HQ) | Nein | — |
| CoD Modern Warfare III | Nein | — |
| PUBG | Nein für DLSS 5. **Bekommt aber ab 10.09.2026 ein Upgrade von DLSS Super Resolution auf DLSS 4.5** (nicht DLSS 5!) — [GameGPU DLSS-4.5/5-Liste](https://en.gamegpu.com/news/igry/spisok-novykh-igr-s-ofitsialnoj-podderzhkoj-tekhnologij-nvidia-dlss-4-5-i-dlss-5) | — |

Fazit: Von den installierten Spielen stehen nur Starfield, Assassin's Creed Shadows und Delta Force auf der offiziellen Ankündigungsliste — aber ohne Patch-Termin. Keines davon hat aktuell (21.09.2026) DLSS 5 bereits ausgeliefert; das erklärt, warum keines im Spielordner eine DLSS-5-Datei zeigt.

## 4. Voraussetzungen für echtes DLSS 5

- **GPU:** ausschließlich GeForce **RTX 50** Desktop/Laptop (RTX 5090 bis RTX 5050) zum Start. RTX 5090 des Nutzers erfüllt das. — [nvidia.com Treiber-News](https://www.nvidia.com/en-us/geforce/news/nba-2k27-dlss-5-3d-guided-neural-rendering-geforce-game-ready-driver/)
- **RTX 40 (Ada Lovelace):** NVIDIA hat am **04.09.2026** offiziell bestätigt, dass RTX-40-Support kommt — aber ohne Termin; Fokus liegt zunächst auf Modell-Optimierung für RTX 50. Quelle: [TechPowerUp](https://www.techpowerup.com/352330/nvidia-confirms-dlss-5-for-geforce-rtx-40-series-is-coming), [AltChar](https://www.altchar.com/game-news/nvidia-reverse-course-confirm-rtx-4000-gpus-will-officially-get-dlss-5-avunu0l5aLbW), [WCCFTech](https://wccftech.com/nvidia-dlss-5-rtx-40-gpus-support-confirmed/). RTX 30/20 sind laut itechguides bislang komplett unbestätigt.
- **Treiber:** ab **GeForce Game Ready Driver 616.64 WHQL** oder neuer — der Nutzer hat 616.92, das erfüllt die Mindestanforderung. — [nvidia.com](https://www.nvidia.com/en-us/geforce/news/nba-2k27-dlss-5-3d-guided-neural-rendering-geforce-game-ready-driver/)
- **NVIDIA App:** neueste Version erforderlich; **es gibt jedoch KEINEN DLSS-5-Schalter in der NVIDIA App** — das Aktivieren von Neural Rendering ist laut NVIDIA allein Sache des Spieleentwicklers (Einstellung im Spiel selbst, z. B. "Video Settings > DLSS Neural Rendering"). — [WebSearch-Zusammenfassung nvidia.com/GeForce-Forum](https://www.nvidia.com/en-us/geforce/forums/geforce-graphics-cards/5/583738/dlss-5-faq/)
- **Streamline:** **Streamline 2.14 oder neuer** wird vorausgesetzt (der Treiber-Cache des Nutzers hat bereits 2.14 — das ist neu genug; die Spiele-DLLs selbst liegen aber noch bei Streamline 2.12, was erklärt, warum kein Spiel DLSS 5 anbietet). Prüfbar über `C:\ProgramData\NVIDIA\NGX\models\nvngx_config.txt`, Eintrag `sl_dlss_g_0`. — [nvidia.com Treiber-News (WebFetch-Auswertung)](https://www.nvidia.com/en-us/geforce/news/nba-2k27-dlss-5-3d-guided-neural-rendering-geforce-game-ready-driver/)
- **Erkennungsmerkmal im Spielordner:** Die maßgebliche neue Datei heißt **`nvngx_dlssnr.dll`** ("DLSS NR" = Neural Rendering, intern **NGX-Feature 18**). Sie ist zusätzlich zur normalen `nvngx_dlss.dll` (Super Resolution) und den `sl.*.dll`-Streamline-Plugins vorhanden. Die offizielle `nvngx_dlssnr.dll` ist hart an RTX-50-Hardware gebunden (auf Ampere/RTX-30 kann sie NGX-Feature 18 gar nicht erzeugen). — [X/dlss_swapper](https://x.com/dlss_swapper/status/2095246427937218934), [GitHub OptiScaler PR #1116](https://github.com/optiscaler/OptiScaler/pull/1116). Solange in keinem der installierten Spielordner eine `nvngx_dlssnr.dll` liegt, ist DLSS 5 dort schlicht noch nicht integriert — deckt sich mit dem Befund des Nutzers.

## 5. Kann man DLSS 5 per NVIDIA-App-„DLSS-Override" oder Profile Inspector erzwingen?

**Nein, nicht auf offiziellem Weg.** Mehrere unabhängige Quellen bestätigen das:

- NVIDIA selbst: Es gibt **keinen DLSS-5-Schalter** in der NVIDIA App; Aktivierung ist Entwickler-Sache (siehe oben, Abschnitt 4).
- itechguides explizit zur Override-Frage: *"Die aktuelle Dokumentation behandelt bestehende DLSS-4.5-Overrides, nicht universelle DLSS-5-Aktivierung in unsupported Spielen."* Bestehende DLSS-Unterstützung garantiert nicht DLSS-5-Kompatibilität. — [itechguides](https://www.itechguides.com/dlss-5-supported-gpus-and-confirmed-games-list-what-nvidia-has-actually-announced/)
- Der klassische **NVIDIA-App-„DLSS-Override"** bzw. **nvidiaProfileInspector** (Orbmu2k) erlauben nur, die DLSS-**Super-Resolution/Frame-Generation-DLL** (`nvngx_dlss.dll`) und das Preset (z. B. Preset K) zu erzwingen bzw. eine neuere Transformer-DLL unterzuschieben — das betrifft klassisches Upscaling, NICHT das neue Neural-Rendering-Feature 18 von DLSS 5. — [nvidiaProfileInspector GitHub Issue #380](https://github.com/Orbmu2k/nvidiaProfileInspector/issues/380), [XDA Developers: "How to get DLSS 4.5 working in any unsupported title"](https://www.xda-developers.com/how-to-get-dlss-45-working-in-any-unsupported-title/) (bezieht sich klar auf DLSS 4.5, nicht 5).
- Digital-Foundry/Presse-Tests (ComputerBase, siehe unten) behandeln ausschließlich das offizielle NBA-2K27-Beispiel, keine Override-Erzwingung in Drittspielen.

## 6. Presse-/Community-Tests (Digital Foundry, ComputerBase, PCGH)

- **ComputerBase** (deutschsprachig, ausführlicher Test): [„Offiziell: DLSS 5 startet an diesem Freitag mit NBA 2K27"](https://www.computerbase.de/artikel/grafikkarten/nvidia-dlss-5-launch.99166/) und der eigentliche Test [„Nvidia DLSS 5 in NBA 2K27 analysiert"](https://www.computerbase.de/artikel/grafikkarten/dlss-5-nba-2k27-test.99198/). Kernaussagen: DLSS 5 kostet auf allen getesteten Karten/Auflösungen deutlich Leistung; ab RTX 5070 aufwärts gutes Spielerlebnis, RTX 5060 Ti noch spielbar; in UHD lohnt sich DLSS 5 selbst bei moderat fordernden Spielen erst ab RTX 5080 aufwärts; Bildverbesserung am stärksten in Zwischensequenzen/Nahaufnahmen; Stromverbrauch steigt spürbar mit DLSS 5.
- **Digital Foundry / Presse-Sammlung** (über ResetEra/DigitalTrends/Tech-Insider referenziert): RTX 5090 fällt von 128 auf 63 FPS (−51 %), RTX 5070 von 120 auf ~53 FPS, RTX 5060 von ~70 auf ~40 FPS in NBA 2K27. DLSS 5 sei primär für Bildqualität/Fotorealismus gedacht, nicht für FPS-Gewinn, und funktioniere am besten in realistisch angelegten Spielen. — [Tech-Insider Zusammenfassung von 6 Test-Outlets](https://tech-insider.org/nvidia-dlss-5-nba-2k27-frame-rate-hit-analysis-2026/), [ResetEra-Thread zu Digital Foundry](https://www.resetera.com/threads/digital-foundry-dlss-5-tested-nba-2k27-image-quality-benchmarks-mods-and-more.1625434/)
- **PCGH:** keine dedizierte, vom Rest unabhängige Quelle in den Suchergebnissen gefunden — nur ComputerBase deckt den deutschsprachigen Raum ausführlich ab.

## 7. Community-Projekte, die "echtes" DLSS 5 in fremde Spiele injizieren

Ja — es existiert ein ganzes Ökosystem, das die **echte, aber vorab geleakte** NVIDIA-Modell-DLL `nvngx_dlssnr.dll` (aus dem NBA-2K27-Build stammend) per Injection in beliebige Spiele einschleust:

- **OptiScaler** (offizielles Open-Source-Upscaler-Ersatz-Projekt) hat DLSS-5-Neural-Rendering als **optionales Modul** via Pull Request #1116 integriert: [GitHub PR #1116](https://github.com/optiscaler/OptiScaler/pull/1116), berichtet u. a. von [VideoCardz](https://videocardz.com/newz/dlss-5-neural-rendering-can-now-run-through-optiscaler) und [WCCFTech](https://wccftech.com/dlss-5-performance-cost-cut-in-half-with-experimental-optiscaler-mod/). Technisch: `nvngx_dlssnr.dll` wird vom Nutzer selbst bereitgestellt (nicht im Release enthalten), läuft als zusätzlicher Pass nach dem eigentlichen Upscaler über NGX-Feature 18, Standardzustand deaktiviert.
- **Dagherbou/OptiScaler_DLSSNR** — spezialisierter Fork: [GitHub Releases](https://github.com/Dagherbou/OptiScaler_DLSSNR/releases)
- **faisalkindi/DLSS5oneclick** — Ein-Klick-Installer für "the leaked DLSS 5 neural-rendering build" für praktisch jedes DX11/DX12-Spiel (RTX 20–50), kombiniert ReShade+RenoDX oder OptiScaler, inkl. Fallback für Spiele ohne DLSS: [GitHub](https://github.com/faisalkindi/DLSS5oneclick)
- Nexus-Mods-Anleitungen für einzelne Spiele, z. B. Stellar Blade: [Nexus Mods – DLSS 5 Neural Rendering Setup Guide](https://www.nexusmods.com/stellarblade/mods/3706), Skyrim/Starfield-Mods ([SF-DLSS5 bei Nexus Mods](https://www.nexusmods.com/starfield/mods/18165)), Assassin's Creed Shadows ([Nexus Mods Bild-Vergleich](https://www.nexusmods.com/assassinscreedshadows/images/330)).
- Übersichtsartikel zur Einordnung "offiziell vs. Mod": [Held Games – Official DLSS 5 vs. the Leaked Mod](https://heldgames.com/guides/dlss-5-official-vs-mod), [Held Games – DLSS 5 Leaked Early Explained](https://heldgames.com/guides/dlss-5-mod-explained).

**Wichtige Einschränkung/Risiko (siehe Bug-Kandidaten unten):** Diese Community-Injection basiert auf einer **inoffiziell geleakten** NVIDIA-Datei, ist technisch DLL-Injection in die Rendering-Pipeline (mechanisch identisch mit Cheat-Techniken) und wird von Anti-Cheat-Systemen (VAC, Easy Anti-Cheat, BattlEye) potenziell erkannt.

## BEST-PRACTICES-KANDIDATEN

- **Erkennungsregel für "hat ein Spiel bereits echtes DLSS 5?":** Prüfe den Spielordner auf das Vorhandensein von `nvngx_dlssnr.dll` (NGX-Feature 18) zusätzlich zur normalen `nvngx_dlss.dll`. Fehlt sie, hat das Spiel (noch) kein natives DLSS 5, unabhängig davon, ob es auf NVIDIAs Ankündigungsliste steht. Quelle: [GitHub OptiScaler PR #1116](https://github.com/optiscaler/OptiScaler/pull/1116), [X/dlss_swapper](https://x.com/dlss_swapper/status/2095246427937218934). Version: OptiScaler PR #1116 (Stand September 2026).
- **Mindestvoraussetzungen für DLSS 5 vor jeder weiteren Fehlersuche prüfen:** GeForce Game Ready Driver ≥ 616.64 WHQL, RTX-50-GPU, Streamline ≥ 2.14 (prüfbar in `C:\ProgramData\NVIDIA\NGX\models\nvngx_config.txt`, Eintrag `sl_dlss_g_0`). Quelle: [nvidia.com](https://www.nvidia.com/en-us/geforce/news/nba-2k27-dlss-5-3d-guided-neural-rendering-geforce-game-ready-driver/). Version: Treiber 616.64 WHQL / Streamline 2.14 (September 2026).

## BUG-KANDIDATEN

- **Anti-Cheat-Bann-Risiko bei DLSS-5-Neural-Rendering-Injection (OptiScaler/ReShade mit geleakter `nvngx_dlssnr.dll`) in Multiplayer-Spielen mit Anti-Cheat.** Betrifft potenziell die installierten Titel Battlefield 6, Call of Duty Black Ops 7 (COD HQ), Call of Duty Modern Warfare III, PUBG und Delta Force (alle mit EAC/BattlEye/proprietärem Anti-Cheat) — die Injection-Methode ist mechanisch identisch mit Cheat-DLL-Injection und wird von VAC/EAC/BattlEye als solche erkannt. Tools wie DLSS5oneclick verweigern deshalb die Installation, wenn EAC/BattlEye/GameGuard-Dateien gefunden werden, sofern der Nutzer den Schutz nicht explizit übersteuert. Quellen: [GitHub faisalkindi/DLSS5oneclick](https://github.com/faisalkindi/DLSS5oneclick), [Held Games – DLSS 5 Mod Risks](https://heldgames.com/guides/dlss-5-mod-risks), [Star Citizen Spectrum – Anti-Cheat-Frage der Entwickler](https://robertsspaceindustries.com/spectrum/community/SC/forum/50259/thread/any-anti-cheat-flags-for-implementing-dlss5). Version: geleakter DLSS-5-NR-Build (Stand ~August/September 2026), OptiScaler-DLSSNR-Fork.
- **Instabilität des inoffiziellen DLSS-5-Injection-Builds:** unoptimierter Pre-Release-Code, bekannte HDR-Probleme, Abstürze/Artefakte, ~45–50 % Performance-Kosten. Quelle: [Held Games – DLSS 5 Mod Risks](https://heldgames.com/guides/dlss-5-mod-risks). Version: geleakter Build, Stand September 2026.
- **Unsignierte/fehlerhafte `nvngx_dlssnr.dll` in manchen Installer-Paketen:** `streamline.zip` liefert eine unsignierte `nvngx_dlssnr.dll` (Hash-Mismatch), was zu dauerhaftem STANDBY/FAILED-Zustand mit Fehlercode `0xBAD00002` führt. Quelle: [GitHub yumlevi/renodx-dlss-installer Issue #1](https://github.com/yumlevi/renodx-dlss-installer/issues/1). Betroffene Version: streamline.zip-Paket in RenoDX-Installer, Stand September 2026.
- **Rechtliches Risiko / Copyright:** Die kursierende `nvngx_dlssnr.dll` stammt aus einem geleakten NBA-2K27-Build und wird ohne NVIDIA-Autorisierung weiterverbreitet; Präzedenzfall: Nexus Mods entfernte einen KI-gestützten GTA-V-Mod nach einer Take-Two-Urheberrechts-Beschwerde. Quelle: [Held Games – DLSS 5 Mod Risks](https://heldgames.com/guides/dlss-5-mod-risks).

## OFFEN/UNSICHER

- Kein konkretes Patch-Datum für Starfield, Assassin's Creed Shadows oder Delta Force gefunden — nur "Fall 2026" pauschal, plus Tech-Demo-Videos von NVIDIA-Mitarbeitern. Muss laufend neu geprüft werden (z. B. über NVIDIA-Treiber-Notes bei jedem neuen Game-Ready-Treiber).
- Kein exaktes Datum für den offiziellen RTX-40-Rollout von DLSS 5; NVIDIA hat den Support am 04.09.2026 nur grundsätzlich bestätigt (Quelle Frandroid/TechPowerUp/AltChar), ohne Termin.
- Die genaue benötigte NVIDIA-App-Versionsnummer wurde in keiner Quelle explizit genannt (nur "neueste Version").
- Der GeForce-Forum-FAQ-Thread (nvidia.com/.../583738/dlss-5-faq) ließ sich per WebFetch nicht laden (nur Ladeseite) — für tiefergehende offizielle FAQ-Details ggf. manuell im Forum nachsehen: https://www.nvidia.com/en-us/geforce/forums/geforce-graphics-cards/5/583738/dlss-5-faq/
- Keine PCGH-spezifische Quelle gefunden (nur ComputerBase als deutschsprachiger Test).
