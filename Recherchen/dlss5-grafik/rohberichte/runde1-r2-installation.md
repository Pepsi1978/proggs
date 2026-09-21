# Web-Researcher 2/7 — Unterthema: Installation Schritt für Schritt („DLSS 5 Mod" / „DLSS5-Feeder" / „DLSS5 Swapper")

Kontext: RTX 5090, Treiber 616.92, Windows 11, Steam. Quelle im Auftrag: TheGr08-Video, „DLSS 5 MOD" via imod.gg, Tags #ReShade #DLSS5feeder, dazu ein inoffizieller „DLSS5 Swapper".

## Wichtiger Einordnungsfund zuerst

Offizielles NVIDIA-DLSS 5 ist seit dem 3. September 2026 real und wird von NVIDIA selbst in Spiele integriert (zunächst nur RTX-50/Blackwell, dann auch RTX-40 nachgezogen) — Quelle: [dlssmod.com/is-it-safe](https://dlssmod.com/is-it-safe/) und Performance-Berichte unten. Das, was in TheGr08-artigen Videos und auf imod.gg als „DLSS 5 MOD"/„DLSS5feeder" beworben wird, ist dagegen **kein** offizielles NVIDIA-Werkzeug, sondern eine inoffizielle, community-gebaute ReShade-Erweiterungskette, die per „geleaktem"/inoffiziellem Binary (`nvngx_dlssnr.dll`, `nvngx_dlss.dll`) versucht, Neural-Rendering/DLAA-Effekte auch in Spiele ohne native DLSS-5-Unterstützung zu „feeden". Das ist technisch etwas anderes als natives DLSS 5 und laut mehreren Quellen mit echtem Malware-Risiko behaftet (siehe Bug-Kandidaten unten). Diese Einordnung ist Grundlage für alle folgenden Installationsschritte — die Schritte werden rein deskriptiv dokumentiert, nicht als Empfehlung zur Nutzung.

## 1. Die Werkzeug-Landschaft

- **DLSS5-Feeder** (GitHub `jlrouzies-fr/DLSS5-Feeder`): „feeds a synthetic DLAA contract (ReShade depth + motion vectors) to the DLSS 5 add-on via a private D3D12 device" — für Spiele ohne jede DLSS-Anbindung, D3D11/D3D12/Vulkan/OpenGL, auch 32-Bit via Helper-Prozess. Quelle: [github.com/jlrouzies-fr/DLSS5-Feeder](https://github.com/jlrouzies-fr/DLSS5-Feeder), README: [raw…/README.md](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **DLSS 5 Swapper** (GitHub `rakanki911/DLSS5-Swapper`): grafische Oberfläche, scannt Steam/Epic/GOG-Bibliotheken, wählt automatisch zwischen „Native DLSS", DLSS5-Feeder, RenoDX, OptiScaler; In-App-Overlay per **F8**. Quelle: [github.com/rakanki911/DLSS5-Swapper](https://github.com/rakanki911/DLSS5-Swapper), Guide-Seite [dlss5swapper.org/guides](https://dlss5swapper.org/guides)
- **RenoDX** — der „Neural Consumer", der die eigentliche Neural-Rendering-Berechnung über ein ReShade-Add-on ausführt (`renodx-dlss5.addon64`). Distribution laut Skript-Quelltabelle über einen Drittanbieter-Mirror `RankFTW/rhi-repo` (s. unten), nicht offiziell NVIDIA. Quelle: [dlss5feeder.com/paths](https://dlss5feeder.com/paths), [github.com/topics/renodx-dlss5](https://github.com/topics/renodx-dlss5)
- **Deep Fried Chicken** — Alternativer „Neural Consumer"; laut Skript-Analyse **ohne öffentliche Download-URL**, nur manuell über Discord beziehbar. Quelle: WebFetch-Analyse des Install-Skripts, siehe unten
- **LumeniteFX Kernel** — Motion-Vector-Provider (ReShade-Shader), empfohlen als `DLSS5_MV_PROVIDER=3`. Quelle: [igorslab.de/en/install-dlss-5-reshade-compatible-games](https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/)
- **imod.gg** selbst: laut Selbstbeschreibung eine generische „Game Modding & Asset Platform" mit „built-in security scanning" — keine belastbaren eigenständigen Installationsdetails zu DLSS5 dort auffindbar. Quelle: [imod.gg](https://imod.gg/)

## 2. ReShade-Version und Add-on-Unterstützung

- Benötigt wird **ReShade 6.8 oder neuer, explizit die Edition „mit vollständiger Add-on-Unterstützung"** („Full Add-on Support"), nicht die Standard-Edition ohne Add-ons. Der Standard-Installer reicht nicht, weil RenoDX/DLSS5-Feeder über die ReShade-Add-on-API andocken. Quellen: [igorslab.de](https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/) („The standard ReShade version is not sufficient. The edition with Full Add-On Support is explicitly required"), [dlss5feeder.com/paths](https://dlss5feeder.com/paths)
- Im offiziellen Setup wird beim Installieren die Ziel-API ausgewählt (DirectX 10/11/12, Vulkan/OpenGL je nach Spiel) und **alle Add-ons außer den Standard-Effekten sollten abgewählt werden**, bevor die DLSS5-spezifischen Add-ons manuell ergänzt werden. Quelle: erste WebSearch-Zusammenfassung zu [github.com/jlrouzies-fr/DLSS5-Feeder](https://github.com/jlrouzies-fr/DLSS5-Feeder)
- Das Install-Skript (`Install-DLSS5Feeder.ps1`) prüft laut Code-Analyse die ReShade-Version aktiv per Export-Namen `ReShadeRegisterAddon`/`ReShadeUnregisterAddon` und warnt, falls eine „Plain"-ReShade-Version (ohne Add-on-Support) erkannt wird. Quelle: [raw…/tools/Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1)

## 3. Dateien, Zielorte, Preset-Ablage (laut Skript-/Doku-Analyse)

| Datei/Komponente | Zweck | Zielort |
|---|---|---|
| `dxgi.dll` (D3D11/12) bzw. `opengl32.dll` (OpenGL) | ReShade-Loader | Spielordner (neben der .exe) |
| `ReShade64.dll`/`ReShade32.dll` (Vulkan) | ReShade als Vulkan-Layer | `C:\ProgramData\ReShade` (maschinenweit) |
| `ReShade.ini` | ReShade-Konfiguration/Preset-Verweis | Spielordner neben .exe |
| `dlss5-feed.addon64` / `.addon32` | DLSS5-Feeder-Kernmodul | Spielordner (64-Bit) bzw. `host64\` (32-Bit) |
| `DLSS5_Feed.fx` | Shader-Effektdatei | `reshade-shaders\Shaders\` |
| `renodx-dlss5.addon64` / Deep-Fried-Chicken-Addon | „Neural Consumer" (nur eines von beiden aktiv!) | Spielordner bzw. `host64\` |
| `nvngx_dlssnr.dll`, `nvngx_dlss.dll` | NVIDIA-Runtime-DLLs | Spielordner (64-Bit) bzw. `host64\` (32-Bit) |
| `dlss5-feed.cfg` / `.log` | Feeder-eigene Konfig/Log | Spielordner |
| `host64\dlss5-feed-host64.exe` | 64-Bit-Hilfsprozess für 32-Bit-Spiele | eigener `host64\`-Unterordner (NICHT neben der 32-Bit-exe) |

Quelle für die Tabelle: konsolidiert aus [github.com/jlrouzies-fr/DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md), [dlss5feeder.com/paths](https://dlss5feeder.com/paths), [igorslab.de](https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/) und Skript-Analyse von [Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1).

**Wichtig, laut README wörtlich zitiert:** „Never install two neural add-ons. If Deep Fried Chicken finds RenoDX's add-on or Alex's Toolkit loaded beside it, it does nothing at all" — d. h. bei gleichzeitiger Installation zweier „Neural Consumer" bricht die Funktion **kommentarlos** ab. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)

### Sonderwege je API
- **DirectX 9/8**: Zusätzlich **dgVoodoo2** als Wrapper nötig (`D3D9.dll`/`D3D8.dll` + `dgVoodoo.conf` neben der .exe, `DisableAndPassThru=false`, `VRAM=1GB`), ReShade wird dabei als `dxgi.dll` installiert statt als `d3d9.dll`. Quelle: [dlss5feeder.com/paths](https://dlss5feeder.com/paths), [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **DirectX 10**: läuft laut Doku über eine interne D3D11-Relay-Komponente. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Vulkan**: ReShade muss als **Vulkan-Layer** (nicht als dxgi.dll) registriert werden, `AddonPath=.\` in `ReShade.ini`; die Registrierung ist maschinenweit für alle Vulkan-Spiele, nicht nur das Zielspiel. Quelle: [dlss5feeder.com/paths](https://dlss5feeder.com/paths)
- **32-Bit über DXVK/Vulkan**: `dxvk.allowFse=False` in `dxvk.conf`, zusätzlich der komplette `host64\`-Zweig nötig. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)

## 4. Tastenkürzel

- **Pos1/Home**: öffnet das ReShade-Overlay/-Menü (Standard-ReShade-Taste). Quelle: [igorslab.de](https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/), [dlss5feeder.com/paths](https://dlss5feeder.com/paths)
- **F8**: öffnet ab DLSS5-Swapper v2.2.1 das eigene In-App-Overlay auf unterstützten 64-Bit-DX11/DX12-Titeln. Quelle: [dlss5swapper.org/guides](https://dlss5swapper.org/guides)
- **Einf/Insert**: OptiScaler-Menü, falls die OptiScaler-DLSSNR-Variante statt DLSS5-Feeder genutzt wird. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- Ein/Aus des Neural-Rendering-Effekts selbst erfolgt **im ReShade-Overlay** über einen eigenen „Enabled"-Schalter unter Add-ons → DLSS 5 Feed/RenoDX, kein separates globales Tastenkürzel dafür dokumentiert. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)

## 5. Nötige Spiel-Einstellungen

- **Depth Buffer**: Das „Generic Depth"-Add-on von ReShade muss aktiviert und der korrekte Tiefenpuffer im ReShade-Overlay ausgewählt werden — das ist Voraussetzung, damit Motion Vectors/Neural Rendering funktionieren. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **MSAA/SSAA**: im Spiel deaktivieren (Kompatibilitätsproblem mit ReShade-Tiefenpuffer-Zugriff). Quelle: [dlss5feeder.com/paths](https://dlss5feeder.com/paths)
- **DLSS/DLAA im Spiel selbst**: Bei Spielen ganz ohne native DLSS-Option liefert das Add-on stattdessen ein synthetisches DLAA („keine echte DLSS-Hochskalierung möglich" laut Doku, nur Kantenglättung/Temporal-Stabilisierung); `work_resolution` kann in `dlss5-feed.cfg` auf 50–100 % gesetzt werden, um doch einen Upscaling-Effekt zu erzwingen. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Motion Vectors**: LumeniteFX-Kernel-Shader muss in der ReShade-Technik-Liste **vor** DLSS5_Feed stehen und aktiv sein, sonst bleibt der Provider auf „DISABLED" und es gibt keinen Effekt. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **HDR**: `hdr_bridge` in `dlss5-feed.cfg` auf „auto" (-1) stellen, `hdr_paper_white=203` als Helligkeits-Referenz anpassbar; bei HDR10/PQ-BT.2020-Swapchains wird laut Doku automatisch eine lineare HDR-Konvertierung aktiviert. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)

## 6. Deinstallation

Laut Doku sauber zu entfernen:
- `dlss5-feed.addon64`/`.addon32`, `DLSS5_Feed.fx` und zugehörige LumeniteFX-Shader aus `reshade-shaders\Shaders\`
- kompletter `host64\`-Ordner (bei 32-Bit-Spielen)
- `dlss5-feed.cfg`, `dlss5-feed.log`
- `renodx-dlss5.addon64` bzw. Deep-Fried-Chicken-Addon
- ReShade selbst (`dxgi.dll`/`opengl32.dll`/`ReShade.ini`) optional zusätzlich entfernen, falls ReShade komplett wieder raus soll
- Bei Vulkan zusätzlich den maschinenweiten Vulkan-Layer-Eintrag prüfen/entfernen (betrifft sonst alle Vulkan-Spiele)

Der DLSS5-Swapper bietet dafür laut Guide-Seite eine eingebaute Funktion „**Restore originals**", die die Original-Dateien aus einem automatischen Backup zurückschreibt. Quellen: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md), [dlss5swapper.org/guides](https://dlss5swapper.org/guides)

## 7. Automatisierte/Kommandozeilen-Installation

`Install-DLSS5Feeder.ps1` (PowerShell, neben die Spiel-.exe legen und ausführen):

```
powershell.exe -ExecutionPolicy Bypass -File .\Install-DLSS5Feeder.ps1
```

Dokumentierte Parameter laut Skript-Analyse:
- `GameExe` — Pfad zur .exe/zum Spielordner (Positionsparameter)
- `-Api Auto|D3D|Vulkan|OpenGL|D3D9|D3D8` (Standard: Auto)
- `-Consumer Ask|RenoDX|DFC|OptiScaler` (Standard: Ask)
- `-MvProvider 3|4`
- `-Downloads <Ordner>` (Cache, Standard `%LOCALAPPDATA%\DLSS5-Feeder\downloads`)
- `-LocalFiles <Ordner>` (Offline-Installation aus lokalen Dateien statt Download)
- `-Prerelease`, `-Force`, `-Yes`, `-NoElevate`, `-NoVerify`, `-NoPause`
- explizite Dateipfad-Overrides: `-FeederZip`, `-DfcZip`, `-DlssNrDll`, `-DlssDll`, `-RenoDxAddon`, `-OptiScalerZip`, `-ReShadeSetup`, `-LumeniteZip`, `-DgVoodooZip`

Quelle: WebFetch-Analyse von [Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1)

**Download-Quellen laut Skript** (`$Sources`-Tabelle im Skript):
- DLSS5-Feeder-Releases: `api.github.com/repos/jlrouzies-fr/DLSS5-Feeder/releases`
- ReShade-Setup: `reshade.me`, Fallback fest verdrahtet auf `reshade.me/downloads/ReShade_Setup_6.8.0_Addon.exe`
- LumeniteFX: `codeload.github.com/umar-afzaal/LumeniteFX/zip/refs/heads/mainline`
- dgVoodoo2: `api.github.com/repos/dege-diosg/dgVoodoo2/releases/latest`
- **NVIDIA-„DLSS-NR"/DLSS/RenoDX-Binärdateien: `github.com/RankFTW/rhi-repo/releases/...`** — ein Drittanbieter-Fork/Mirror, nicht der offizielle NVIDIA-Kanal
- Deep Fried Chicken: laut Skript **keine öffentliche URL hinterlegt**, muss manuell über Discord beschafft werden

Quelle: [Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1)

## 8. Bekannte Installationsprobleme

- **Schwarzer Bildschirm / kein Effekt**: meist deaktivierter oder falsch konfigurierter Motion-Vector-Provider (Overlay zeigt „Motion vectors: DISABLED"); `DLSS5_MV_PROVIDER=3` prüfen; Log auf „feature ready … DLAA" kontrollieren. Quelle: [igorslab.de](https://www.igorslab.de/en/install-dlss-5-reshade-compatible-games/), [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Absturz beim Start**: Windows Defender blockiert häufig „Deep Fried Chicken" als Fehlalarm (Detours-Hooking-Technik) → Ordner-Ausnahme nötig; D3D12-Fehler `0x887E0003` deutet auf beschädigtes lokales D3D12-Agility-SDK des Spiels hin; parallel installiertes OptiScaler (Stock-Version) kollidiert mit dem DLSSNR-Fork und muss entfernt werden. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Depth Buffer nicht/falsch erkannt**: Generic-Depth-Auswahl im ReShade-Overlay prüfen; für einzelne Spiele (z. B. Subnautica) existieren laut Doku spezielle Profile. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **HDR-Probleme**: zu dunkles/helles Bild → `hdr_bridge=-1` (auto) und `hdr_paper_white` anpassen. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Vulkan-Flimmern**: Kollision mit NVIDIA Smooth Motion („the two cannot work together", ca. die Hälfte der Frames bleibt unverarbeitet) → Smooth Motion für Vulkan im NVIDIA Profile Inspector separat ausschalten. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Treiberkonflikt**: Für Treiber **616.64** in Kombination mit RenoDX v4.6/v4.7 werden Faults direkt im Treiber gemeldet; Workarounds laut Doku: Deep Fried Chicken statt RenoDX nutzen, auf Treiber 616.56 downgraden, oder klassische RenoDX-Engine statt neuer Version. Nutzer hat Treiber 616.92 — ob dieser konkrete Konflikt dort noch besteht, ist in den Quellen nicht erwähnt (offen). Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
- **Stumme Fehlfunktion**: zwei gleichzeitig geladene „Neural Consumer"-Add-ons (z. B. Deep Fried Chicken + RenoDX) führen zu komplett wirkungslosem Zustand ohne Fehlermeldung. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)

## 9. Leistungskosten auf RTX 40/50

Diese Zahlen stammen aus Tests von **echtem, offiziellem** DLSS 5 (nicht dem inoffiziellen Feeder/Mod — für den Mod-Pfad selbst wurden in den Quellen keine belastbaren RTX-40/50-Benchmarks gefunden, siehe OFFEN/UNSICHER):
- RTX 5090, 4K/Ultra in NBA 2K27: 128,4 → 62,7 FPS, **−51,2 %**; Frame-Time-Kosten ca. 8,3 ms bei 4K. Quelle: [notebookcheck.net](https://www.notebookcheck.net/First-DLSS-5-game-tested-DLSS-5-humbles-RTX-5090-with-over-50-performance-loss.1391978.0.html)
- RTX 5090 in Control: 87,5 → 50,9 FPS, **−42 %**. Quelle: [techspot.com/article/3170-real-dlss-5-performance](https://www.techspot.com/article/3170-real-dlss-5-performance/)
- RTX 4090 (nachträglich per Patch mit DLSS 5 versorgt): ca. 135 → 82 FPS, **−39 %**. Quelle: [tech-insider.org/dlss-5-patched-rtx-4000-ada-lovelace-2026](https://tech-insider.org/dlss-5-patched-rtx-4000-ada-lovelace-2026/)
- RTX 4090: −33 % FPS, Frame-Time +50 % bei Ultrawide-Auflösung. Quelle: [en.gamegpu.com](https://en.gamegpu.com/news/zhelezo/geforce-rtx-4090-s-dlss-5-teryaet-33-fps-i-uvelichivaet-vremya-kadra-na-50-v-razreshenii-ultrawide)
- Kostenskalierung ca. proportional zur Pixelzahl: 4K kostet ca. 2,2× so viel Rechenzeit wie 1440p (bei 2,25× mehr Pixeln). Quelle: [tech-insider.org/nvidia-dlss-5-performance-power-benchmarks-2026](https://tech-insider.org/nvidia-dlss-5-performance-power-benchmarks-2026/)

Einordnung für den inoffiziellen Feeder-Pfad: Da dieser laut Doku dieselben Neural-Rendering-Binärdateien nutzt (nur mit synthetischem statt nativem DLAA-Kontrakt gefüttert), ist von vergleichbarer oder tendenziell höherer Kostenordnung (30–50 % FPS-Verlust) auszugehen — das ist aber eine Ableitung, keine direkt belegte Messung für den Mod-Pfad selbst.

## BEST-PRACTICES-KANDIDATEN:

KEINE — es handelt sich um ein inoffizielles, sicherheitskritisches Drittanbieter-Werkzeug ohne verifizierten Hersteller; eine Aufnahme als „Best Practice" in `best-practices/` wäre unangemessen. Rein technische Fakten (z. B. „ReShade mit Add-on-Support ist Voraussetzung für Add-on-basierte Mods", Version ReShade 6.8) könnten allenfalls neutral in einen bestehenden ReShade-Best-Practices-Eintrag einfließen, falls einer existiert — das aber nur als generische ReShade-Information, nicht als Empfehlung des DLSS5-Mod-Ökosystems.

## BUG-KANDIDATEN:

1. **Malware-Risiko bei Drittanbieter-DLLs im DLSS-Swapper-Ökosystem** — Tom's Hardware berichtet, der App-Ersteller selbst warnt davor, zufällige nutzer-eingereichte DLLs zu verwenden, da eine Infektion damit möglich ist. Quelle: [tomshardware.com/…modding-tool-dlss-swapper-might-infect-your-pc-with-malware…](https://www.tomshardware.com/video-games/pc-gaming/modding-tool-dlss-swapper-might-infect-your-pc-with-malware-if-you-download-the-wrong-files-app-creator-warns-against-using-random-user-submitted-dlls) — betrifft direkt den hier beschriebenen Installationsweg (DLL-Ersatz `nvngx_dlssnr.dll`/`nvngx_dlss.dll`/Neural-Consumer-Addons).
2. **„Deep Fried Chicken" ohne verifizierbare öffentliche Quelle** — laut Skript-Analyse des offiziellen Install-Skripts gibt es dafür keine öffentliche Download-URL, Bezug nur manuell über Discord; damit keine reproduzierbare Herkunfts-/Integritätsprüfung möglich. Quelle: [Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1)
3. **Neural-Rendering-Binärdateien über Drittanbieter-Mirror statt NVIDIA** — Das Install-Skript bezieht `nvngx_dlssnr.dll`, `nvngx_dlss.dll` und `renodx-dlss5.addon64` nicht von nvidia.com, sondern von `github.com/RankFTW/rhi-repo`, einem privaten GitHub-Konto. Quelle: [Install-DLSS5Feeder.ps1](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/tools/Install-DLSS5Feeder.ps1)
4. **Anti-Cheat-/Bann-Risiko**: Mehrere Quellen warnen unabhängig, DLL-Ersetzung/-Injection könne von VAC/EAC/BattlEye als Manipulation erkannt werden und zu Sperren führen; Nutzung nur in Singleplayer/ohne Anti-Cheat empfohlen. Quellen: [dlssmod.com/is-it-safe](https://dlssmod.com/is-it-safe/) („do not use this mod with online games"), Zusammenfassung zu [github.com/rakanki911/DLSS5-Swapper](https://github.com/rakanki911/DLSS5-Swapper)
5. **Stumme Fehlfunktion bei doppelten Neural-Consumer-Add-ons** — siehe Abschnitt 8, kein Fehler-Feedback, nur wirkungslose Installation. Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md)
6. **Treiberkonflikt 616.64 + RenoDX v4.6/v4.7** (Faults im Treiber selbst). Quelle: [DLSS5-Feeder README](https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md) — Version des Nutzer-Treibers (616.92) ungeprüft in Bezug auf diesen konkreten Bug.

Empfehlung an den Nutzer (nicht Teil der reinen Faktensammlung, aber sicherheitsrelevant genug für den Hinweis): Bevor dieser Mod-Pfad installiert wird, sollte der/die für Sicherheits-/Legitimitätsbewertung zuständige Researcher-Befund dieser Recherche-Runde konsultiert werden — die hier gesammelten Installationsschritte setzen mehrere Dateien voraus, deren Herkunft nicht über offizielle NVIDIA-Kanäle verifizierbar ist.

## OFFEN/UNSICHER:

- Keine belastbare, direkt am Mod-Pfad (DLSS5-Feeder/Swapper) gemessene FPS-Kostenzahl für RTX 40/50 gefunden — nur Zahlen zum offiziellen, nativ integrierten DLSS 5. Die im Text genannte Ableitung „vergleichbar/tendenziell höher" ist nicht durch eine explizite Quelle belegt.
- Nicht verifiziert, ob der konkrete Treiberkonflikt „616.64 + RenoDX v4.6/v4.7" auch bei der vom Nutzer gemeldeten Treiberversion 616.92 auftritt.
- `imod.gg` als konkrete Bezugsquelle für den „DLSS 5 MOD" aus dem TheGr08-Video konnte inhaltlich nicht direkt geprüft werden (Suche fand nur die generische Plattform-Startseite, keine spezifische DLSS5-Mod-Downloadseite dort).
- WebFetch-Zusammenfassungen (nicht Volltext-Zitate) wurden für mehrere Seiten verwendet, da Originalseiten teils nicht vollständig zugänglich waren (z. B. Tom's Hardware teilweise, GitHub-Repo rakanki911/DLSS5-Swapper nur eingeschränkt lesbar, reshade.me lieferte HTTP 429). Einzelangaben (Dateinamen, Parameter, Pfade) stammen aus KI-generierten Seiten-Zusammenfassungen Dritter, nicht aus direkt gegengelesenem Rohtext — Restfehlerrisiko bei Detailgenauigkeit einzelner Parameter-/Pfadnamen ist nicht auszuschließen.
- Die extrem hohe Zahl an praktisch identischen, aber unabhängig benannten GitHub-Forks/-Repos rund um „DLSS5-Feeder"/„dlss5-neural-amd" mit unterschiedlichen, wenig etablierten Account-Namen (z. B. GorgotsRoman, zmodelerlover, billy948787, faisalkindi, zhubaohi, mrcgibb9876-hash) ist auffällig und könnte auf ein Ökosystem aus Forks/Klonen mit unklarer Vertrauenswürdigkeit hindeuten — dies wurde nicht tiefer geprüft, da außerhalb des Unterthemas „Installation".
