# R6 – Sicherheit, Echtheit, Anti-Cheat, Rechtliches (RenoDX-DLSS / DLSS-5-Neural-Rendering)
Stand: 14.10.2026. Eine Quelle (URL) pro Aussage. "(API)" = von mir am 14.10.2026 per GitHub-API selbst abgefragt.

## 0. Wichtigste Erkenntnisse
- ShortFuses generisches Add-on heißt `renodx-dlss.addon64` (nicht `renodx-dlss5.addon64`; das ist die Variante von "Krish"): https://github.com/clshortfuse/renodx/issues/651
- Es steht NICHT in den offiziellen GitHub-Releases von clshortfuse/renodx: Im Release "snapshot" (13.10.2026) gibt es nur `renodx-dlssfix.*`, kein `renodx-dlss.addon64` (API: https://api.github.com/repos/clshortfuse/renodx/releases/tags/snapshot). Verteilung laut Guides über den RenoDX-Discord (Kanal #DLSS5 bzw. ShortFuse-Thread): https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/ und https://dlss5mod.com/guides/install
- Build 2026-09-04: eingebetteter Zeitstempel 2026-09-04T08:48:41, Version meldet 0.0.0.0: https://github.com/clshortfuse/renodx/issues/651
- clshortfuse kommentiert in den DLSS-Issues #651 / #668 nicht: https://github.com/clshortfuse/renodx/issues/668

## 1. Downloadquellen: echt vs. Fälschung
### Offiziell
- clshortfuse/renodx (GitHub-Releases snapshot/nightly mit SHA-256-Digests in der API): https://github.com/clshortfuse/renodx
- Offizieller Discord laut RenoDX-Wiki: https://discord.com/invite/renodx ("where the mods are being developed"): https://github.com/clshortfuse/renodx/wiki/Mods (Name "HDR Den" in keiner Quelle bestätigt)
- clshortfuse/openNR (MIT): verteilt KEINE NVIDIA-DLLs, Gewichte oder generierte DLLs; Nutzer muss nvngx_dlssnr.dll 310.8.0.0 selbst bereitstellen: https://github.com/clshortfuse/openNR

### Community-Mirror/Tools (nicht offiziell, weit verbreitet)
- RankFTW/rhi-repo ("RHI"): hostet `renodx-dlss-SF-*` (Spiegel der ShortFuse-Builds), `renodx-dlss5-*`, `dlssnr-*`, `dlss-*`-Zips jeweils mit SHA-256-Digest (API), online und aktiv (Push 13.10.2026): https://github.com/RankFTW/rhi-repo/releases . Wan2GP-Doku: Links "not NVIDIA or RenoDX official releases": https://github.com/deepbeepmeep/Wan2GP/blob/main/docs/DLSS5.md
- RHI auf Nexus (Modding Tools 1710): https://www.nexusmods.com/site/mods/1710
- yumlevi/renodx-dlss-installer (RenoDX-DLSS-Setup.exe, rolling release, lädt Add-on + streamline.zip zur Installationszeit nach, keine Hashes): https://dlss5swapper.org/renodx-dlss
- faisalkindi/DLSS5oneclick (Rust-EXE, nicht code-signiert, lädt alles aus rhi-repo; "cannot vouch" für Add-on/Modell; warnt vor anderen "DLSS 5 manager/one-click"-Programmen): https://github.com/faisalkindi/DLSS5oneclick
- rakanki911/DLSS5-Swapper als angeblich einzige echte Swapper-Quelle (Drittseite, nicht verifiziert): https://dlss5-swapper.com/safety/is-dlss-5-swapper-safe/

### Fälschungen / Warnsignale
- rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games: 1 Stern, 1 Fork, 3 Commits, "Run the loader once as admin", Loader schreibt `nvngx_dlss.dll` + Add-on, ZIP `RR-DLSS5-RenoDX.zip` v5.0.2, keine Hashes, keine Signatur, Loader-Quellcode nicht angegeben. Nicht als Malware bewiesen, aber hochriskant. Keine Verbindung zu clshortfuse/RankFTW erkennbar: https://github.com/rrdlss5renoxdx/RR-and-DLSS-5-RenoDX-for-the-Games
- imod.gg: in keiner Quelle gefunden, weder Echtheits- noch Malwarebericht (unverifiziert).
- Nexus "33280": nicht auffindbar. Ähnlich: Cyberpunk-Mod 33380 "DLSS 5 Neural Rendering Installation Guide", Autor "RenoDX community" (nur Suchsnippet, Nexus liefert 403): https://www.nexusmods.com/cyberpunk2077/mods/33380
- Typische Fake-Merkmale: verlangt Geld, "Manager-App"/Installer, DLL auf eigenem Server, Antivirus ausschalten, .exe statt .zip: https://dlssmod.com/is-it-safe/
- Held Games (Meinung): kein legitimer öffentlicher Download; "Easy Install"-Pakete verdächtig; Leak ohne legitimen Download sei "exactly the setup malware distributors look for" (Aussage "RenoDX hat kein Release" gilt nur für GitHub-Releases, nicht für Discord): https://heldgames.com/guides/dlss-5-mod-risks

### Malware-Berichte 2026
- DLSS-5-Bezug, unverifiziert: Issue "Malware/Infostealer disguised as Forge Mod (novahelper)" im Repo dlss5-minecraft-ultra-graphics (RAT/Infostealer-Behauptung; WebFetch 404, nur Suchsnippet): https://github.com/talonconquerorsee/dlss5-minecraft-ultra-graphics/issues/1
- DLSS Swapper: nutzer-hochgeladene DLL mit Malware (Crypto-Miner) im GitHub-Repo; Entwickler warnt; kein DLSS-5-Bezug im abgerufenen Text: https://www.tomshardware.com/video-games/pc-gaming/modding-tool-dlss-swapper-might-infect-your-pc-with-malware-if-you-download-the-wrong-files-app-creator-warns-against-using-random-user-submitted-dlls , https://videocardz.com/newz/dlss-swapper-creator-warns-that-user-submitted-dll-files-may-contain-malware
- Defender-Fehlalarme bei unsignierten DLSS5-Tools (Cloud-ML-Verdikt, kein Signaturtreffer; Seite beim Abruf 404, nur Snippet): https://github.com/mrcgibb9876-hash/OptiDLSS5-UI/issues/141
- Kein Malwarefund in offiziellen clshortfuse- oder RankFTW-Dateien gefunden. "Die meisten Meldungen betreffen gefälschte Kopien": https://dlssmod.com/is-it-safe/ (Drittseite)

## 2. Echtheit prüfen
- Original-NVIDIA-DLL: `nvngx_dlssnr.dll` 310.8.0.0, 165.840.496 Bytes (~158 MB), SHA-256 `E16BCF15E16E13F527491CDF7845B2FE6521A738D8F7C9C721866A8496E1FC8E`: https://github.com/clshortfuse/openNR
- Authenticode gültig (NVIDIA Corporation) bei genau diesem Hash bestätigt von einem Issue-Reporter: https://github.com/clshortfuse/renodx/issues/668 . Signiert 11. August, Version 310.8.0 laut Linus Tech Tips (nur Snippet, Seite 403): https://linustechtips.com/topic/1642227-dlss-5-dll-leaked-from-early-access-build-of-nba2k27-feature-control-already-exposed-for-control/
- ACHTUNG gepatchte Community-Varianten (RTX-40-Patch/FP16) sind absichtlich UNSIGNIERT: `310.8.SF`/`310.8.SF-v2` (DLL-SHA-256 `6EB209E764F39872625DEBD6ABAF45E2BB6322F6F270F781F70C059AE30B3927`, "modified, unsigned NVIDIA-derived runtime"): https://github.com/deepbeepmeep/Wan2GP/blob/main/docs/DLSS5.md . RHI-Zip-Digests (API): 310.8.0 = 388c0a79...fb3ac, 310.8.0-RTX40 = 46124cfa...9e3b, 310.8.SF = 4de991bf...c449, 310.8.SF-v2 = 1da35941...955c, 310.8.Lecram = ddb64e55...ca54 (vollständig in https://github.com/RankFTW/rhi-repo/releases). Unsigniert heißt nicht Malware, ist aber nicht per Signatur prüfbar.
- Fehlerfall: `streamline.zip` im yumlevi-Installer enthielt eine nvngx_dlssnr.dll 310.8.0.0 mit Authenticode HashMismatch (SHA-256 `CEB6432F6FBDF44D886014BCD47241932BF8B67439FEEF9BBDD0961436662650`); NGX lehnt sie ab, Add-on bleibt STANDBY/FAILED 0xBAD00002: https://github.com/yumlevi/renodx-dlss-installer/issues/1
- Prüfbefehle (Methode): `Get-AuthenticodeSignature .\nvngx_dlssnr.dll` (Status Valid, Signer NVIDIA Corporation) und `Get-FileHash -Algorithm SHA256`.
- Add-on-Datei: unsigniert. clshortfuse/renodx-Snapshot-Assets haben Digests in der API (z. B. renodx-dlssfix.addon64 = `8a23cea200c8b62bc33fb7e0971c859f2abb281d1c434bd9ab568f16f899d034`, API snapshot 13.10.2026). Für `renodx-dlss.addon64` (Discord-Verteilung) ist kein offizieller Hash publiziert; der RHI-Spiegel hat Digests, z. B. renodx-dlss-SF-26.1003.2350 zip = `34ef92d6742bf6d05df42ff81c04b70f5138b0a0a970a93f1a04295ac83f7f0f` (https://github.com/RankFTW/rhi-repo/releases). Dass das Add-on aus einem GitHub-Actions-Build stammt, ist für `renodx-dlss.addon64` NICHT belegt.

### Woher kommt die DLL (legal/sicher)?
- RenoDX/ShortFuse liefert sie nicht mit: https://github.com/clshortfuse/openNR
- Praxis: RHI/Swapper/Autopilot/OneClick laden sie aus rhi-repo; Discord-Guide nennt für RTX 20/30/40 eine gepatchte Version im angepinnten Post: https://dlss5mod.com/guides/install
- Legal sauber nur über offizielle Spiele mit DLSS 5 (z. B. NBA 2K27); Community-Kopien stammen aus Pre-Release/Leak: https://dlss5-swapper.com/learn/nvngx-dlssnr-dll/ . Wan2GP empfiehlt, eine NVIDIA-signierte DLL aus lizenzierter Software zu kopieren: https://github.com/deepbeepmeep/Wan2GP/blob/main/docs/DLSS5.md
- Offizieller Start von DLSS 5: 3.9.2026 in NBA 2K27 (RTX 50, GeForce NOW): https://www.videogameschronicle.com/news/nvidias-controversial-dlss-5-officially-launches-this-week-via-nba-2k27/
- Leak: Fund 26.08.2026 im NBA-2K27-Early-Access-Build (sekundäre Quelle): https://overclock3d.net/news/software/nvidia-dlss-5-neural-rendering-dll-files-leak-launch-incoming/

## 3. Anti-Cheat
- Mechanik: ReShade-Add-on = in den Spielprozess injizierte DLL, Anti-Cheat kann blocken oder als Modifikation werten. Empfehlung nur Singleplayer/Offline; GTA Online: vorher ReShade/RenoDX/modifizierte DLSS-Dateien entfernen; Star Citizen derzeit nicht empfohlen; Vorsicht bei CoD, Fortnite, Battlefield, Destiny 2: https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/
- DLSS5oneclick verweigert die Installation bei Dateien von EAC, BattlEye, GameGuard, EA Javelin, Tencent ACE; Overwatch/Valorant/League/Roblox per EXE-Name; Overwatch blockt unsignierte DLLs (Fehler 0x80090006); "ReShade add-on injection is exactly what they flag": https://github.com/faisalkindi/DLSS5oneclick
- "Do not install injection add-ons or replacement libraries into online or anti-cheat protected games" (Warnung der Installationsanleitung): https://dlss5mod.com/guides/install
- Bann-BERICHTE: keine belegten, verifizierten Einzelfälle für das RenoDX-DLSS-Add-on gefunden. Sportskeeda behauptet ohne Quelle, EAC erkenne den DLSS5-Feeder und Spieler seien hardware-gebannt (Artikel belegt es nicht): https://tech.sportskeeda.com/gaming-news/modders-built-dlss-5-mod-override-tool-use-it . Fallout-76-Thread: kein dokumentierter Bann: https://steamcommunity.com/app/1151340/discussions/0/572674734038206596/
- Spezifisch Ricochet/CoD, ACE/Delta Force, BattlEye/PUBG, Vanguard: keine Quelle gefunden. BF6/Javelin: nur allgemeine, anekdotische ReShade-Diskussionen: https://steamcommunity.com/app/2807960/discussions/0/690871777622153886/ , https://forums.ea.com/discussions/battlefield-franchise-discussion-en/cant-use-reshade-with-the-new-anti-cheat-update/6826706
- Bans kämen oft verzögert in Wellen, "keine Bans" ist kein Beweis (Drittseite): https://dlss5-swapper.com/safety/dlss-5-mods-online-ban-risk/
- Fazit: Risiko real, aber überwiegend vorsorglich dokumentiert; einhellige Empfehlung: nur Singleplayer ohne Kernel-Anti-Cheat.

## 4. Rechtliches / NVIDIA-Reaktion
- Keine NVIDIA-Stellungnahme zum Leak und keine DMCA-Notice gefunden. github/dmca per API (14.10.2026): Suche "nvngx", "renodx", "neural rendering" = 0 Treffer; "dlss" nur der DLSS-Updater-Fall (Streit zwischen Entwicklern, nicht NVIDIA): https://github.com/github/dmca (2026/05/2026-05-07-dlss-updater.md, 2026/10/2026-10-05-dlss-updater-counternotice.md)
- RankFTW/rhi-repo online, nicht archiviert/disabled, alle `dlssnr-*`-Releases (310.8.0 vom 27.08. bis 310.8.Lecram vom 24.09.) vorhanden (API): https://github.com/RankFTW/rhi-repo/releases . clshortfuse/renodx online, Push 14.10.2026 (API): https://github.com/clshortfuse/renodx
- Keine NVIDIA-Reaktion, Verbreitung "faster than NVIDIA could react": https://vgtimes.com/tech-and-hardware/165721-modders-test-nvidia-dlss-5before-its-official-release.html
- Meinung: Weitergabe fremden proprietären Codes; Präzedenz Nexus/Take-Two-Strike (GTA-V-KI-Mod); Risiko für einzelne Downloader "admittedly low", Inhalte könnten verschwinden: https://heldgames.com/guides/dlss-5-mod-risks
- Lizenz: für das DLSS5-Add-on wird keine Lizenz veröffentlicht, NVIDIA-Laufzeiten unterliegen NVIDIA-Bedingungen: https://github.com/faisalkindi/DLSS5oneclick

## 5. Lücken
- "HDR Den" nicht bestätigt; imod.gg und Nexus 33280 nicht auffindbar; Nexus/LTT/Tom's-Hardware per WebFetch 403 bzw. gekürzt; kein Bann mit Datum/Spiel/Anti-Cheat belegt; kein belegter Malwarefund in offiziellen RenoDX-/RHI-Dateien.

## BEST-PRACTICES-KANDIDATEN:
1. nvngx_dlssnr.dll prüfen: `Get-AuthenticodeSignature` (Valid, NVIDIA Corporation) plus SHA-256 E16BCF15E16E13F527491CDF7845B2FE6521A738D8F7C9C721866A8496E1FC8E für 310.8.0.0 (https://github.com/clshortfuse/openNR , https://github.com/clshortfuse/renodx/issues/668). Gepatchte Varianten (310.8.SF, -RTX40, -Lecram) sind unsigniert: nur per RHI-Release-Digest prüfen (https://github.com/RankFTW/rhi-repo/releases).
2. Add-on/Tools nur aus clshortfuse/renodx-Releases, RenoDX-Discord (discord.com/invite/renodx) oder RankFTW/rhi-repo; Digest per `gh api repos/<o>/<r>/releases/tags/<tag> --jq '.assets[]|[.name,.digest]'` abgleichen.
3. Nur Singleplayer/Offline, nie mit EAC/BattlEye/Javelin/Ricochet/ACE/Vanguard; vor Online-Start ReShade/RenoDX/modifizierte DLSS-DLLs entfernen (https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/ , https://github.com/faisalkindi/DLSS5oneclick).
4. Keine Loader als Admin aus unbekannten Repos (z. B. rrdlss5renoxdx), keine DLL-Downloadseiten, Archive vorher bei VirusTotal prüfen (https://dlssmod.com/is-it-safe/).

## BUG-KANDIDATEN:
1. yumlevi/renodx-dlss-installer: streamline.zip (Release "latest") enthält nvngx_dlssnr.dll 310.8.0.0 mit Authenticode HashMismatch (SHA-256 CEB6432F...2650): NGX erzeugt Feature 18 nicht, Add-on STANDBY/FAILED 0xBAD00002 (renodx-dlss5-v2.5 v0.2026.827.2036, ReShade 6.8.0.2155, Crimson Desert): https://github.com/yumlevi/renodx-dlss-installer/issues/1
2. Gleiches Issue: Manifest StartDisabled=1, Neural Rendering nach Installation aus (`DLSSNR.Enabled` setzen); bei "creates 0" Upscaling-Einstellung im Spiel umschalten: https://github.com/yumlevi/renodx-dlss-installer/issues/1
3. clshortfuse/renodx #651: Direct Neural Rendering bleibt auf "Waiting" bei "Require DLSS" mit DLSS-5-Bridge-DLAA (ETS2; Add-on-Build 2026-09-04T08:48:41, ReShade 6.8.0.2155, Bridge v1.4.8.0): https://github.com/clshortfuse/renodx/issues/651
4. clshortfuse/renodx #668: DLSS-NR idle (evaluate_c=0, hooks 6/8) auf D3D12-Host ohne natives DLSS (PCSX2); nvngx_dlssnr 310.8.0 signiert OK, Add-on-Build 2026-09-17; zuvor 310.8.SF.0 aus RHI-Cache unsigniert: https://github.com/clshortfuse/renodx/issues/668
5. Overwatch blockt unsignierte DLLs (0x80090006): https://github.com/faisalkindi/DLSS5oneclick ; Defender löscht unsignierte Tool-Dateien (Cloud-ML): https://github.com/mrcgibb9876-hash/OptiDLSS5-UI/issues/141
