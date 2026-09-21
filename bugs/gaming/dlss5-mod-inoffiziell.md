# Bug-Almanach: DLSS 5 Mod (inoffiziell: OptiScaler-DLSSNR, DLSS5-Feeder, DLSS5 Swapper, RenoDX)

> Stand: 21.09.2026 16:26 · Versions-Anker: NVIDIA-Treiber 616.92, `nvngx_dlssnr.dll` 310.8.0, OptiScaler-DLSSNR v0.2.0, DLSS5-Feeder 1.16.0-beta.6, DLSS5 Swapper 2.2.7, renodx-dlss5 4.70 · Gegenstück: `best-practices/gaming/dlss5-mod-inoffiziell.md`

## M1 — Fake-Repos und Malware-Repacks
- **Symptom:** „DLSS 5“-Download bringt Fernwartung (ScreenConnect), Krypto-Miner (srbminer), getarnte geplante Aufgaben („Windows System Health“) oder Cookie-Stealer mit.
- **Ursache:** Klon-Repos mit ersetztem Download-Button (u. a. `efdfdfsdfds`, `gitzec`, `Aryoksini`, `GorgotsRoman` jeweils `/DLSS5-Feeder`, mehrere „DLSS5-Autopilot“-Forks) und Repacks auf Drittseiten. `DLSS5oneclick` holt eine URL aus einem offen editierbaren Wiki ohne Prüfung (#102).
- **Fix:** Nur Original-Releases, SHA-256 gegen den GitHub-API-`digest`, Modell-DLL per Authenticode prüfen (M4), Defender-Scan.
- **Quellen:** https://github.com/rakanki911/DLSS5-Swapper/issues/363 · https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/CAREFUL_FAKE_MALICIOUS_FEEDER.txt · https://github.com/faisalkindi/DLSS5oneclick/issues/102

## M2 — renodx-dlss5 v4.6/v4.7 auf Treiber ≥ 616.64 kaputt
- **Symptom:** Kein Neural Pass, `--test` meldet 0/300 (v4.7) bzw. 1/300 (v4.6) erfolgreiche Evaluates.
- **Ursache:** Die „lazy-adoption engine“ löst auf 616.64+ einen Fault in NVIDIAs eigener `nvngx_dlssnr.dll` aus (Feeder #54).
- **Versionen:** Treiber 616.64 gemessen, 616.92 ungeprüft. Der automatische Feeder-Installer lädt genau renodx-dlss5 **4.70**.
- **Fix:** Deep Fried Chicken 1.4.8-alpha, eine classic-engine-renodx-dlss5 (4.55), Treiber 616.56 oder OptiScaler-DLSSNR.
- **Quelle:** https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md · https://github.com/jlrouzies-fr/DLSS5-Feeder/issues/54

## M3 — NGX-Init `0xBAD00001` im Spielprozess
- **Symptom:** `NVSDK_NGX_D3D12_Init -> 0xBAD00001`, dieselben Dateien laufen im `host64`-Helfer.
- **Versionen:** Treiber 616.56, DLSS5-Feeder bis 1.16.0-beta.6, offen.
- **Quelle:** https://github.com/jlrouzies-fr/DLSS5-Feeder/issues/47

## M4 — Unsignierte `nvngx_dlssnr.dll` → STANDBY/FAILED `0xBAD00002`
- **Symptom:** Neural Pass bleibt in STANDBY oder FAILED.
- **Ursache:** Manche Pakete (`streamline.zip`) liefern eine unsignierte bzw. veränderte DLL (Hash passt nicht).
- **Fix (Poka-Yoke):** Vor dem Kopieren `Get-AuthenticodeSignature` → nur `Valid` + `O=NVIDIA Corporation` zulassen. `Werkzeuge/dlss5-mod/installieren.ps1` bricht sonst ab.
- **Quelle:** https://github.com/yumlevi/renodx-dlss-installer/issues/1

## M5 — Modell-DLL unter falschem Namen zerstört Ray Reconstruction
- **Symptom:** Nach der Installation ist Ray Reconstruction kaputt.
- **Ursache:** Eine ca. 165 MB große Datei namens `nvngx_dlssd.dll` ist das DLSS-5-Modell mit falschem Namen, nicht Ray Reconstruction.
- **Fix:** Dateieigenschaften prüfen („NVIDIA DLSSNR“ = Modell). Nur als `nvngx_dlssnr.dll` ablegen.
- **Quelle:** OptiScaler-DLSSNR v0.2.0, „READ ME - DLSS Neural Rendering.txt“

## M6 — Zwei Neural-Consumer installiert → stilles Nichts
- **Symptom:** Alles installiert, kein Effekt, keine Fehlermeldung.
- **Fix:** Nur genau einen Consumer (Deep Fried Chicken **oder** RenoDX **oder** OptiScaler-DLSSNR) pro Spiel. Normales OptiScaler kollidiert mit dem DLSSNR-Fork.
- **Quelle:** https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md

## M7 — Swapper-Deinstallation lässt Mod-Dateien in den Spielordnern
- **Fix:** Pro Spiel zuerst „Restore originals“, dann das Programm deinstallieren. Seit 2.2.7 weist der Uninstaller darauf hin.
- **Quelle:** https://github.com/rakanki911/DLSS5-Swapper/issues/266

## M8 — Bann durch Hook in Anti-Cheat-Spielen
- **Symptom:** Kontosperre oder Startblockade (PUBG/BattlEye blockiert den Start).
- **Ursache:** DLL-Injection bzw. ReShade-Add-on ist technisch identisch mit Cheat-Hooks. Keiner der Hersteller lässt die Add-on-Edition zu.
- **Fix:** Nie in BF6 (Javelin), CoD inkl. Kampagne (Ricochet), Delta Force (ACE), PUBG. Absicherung über eine feste Spieleliste, weil Javelin und ACE nicht im Spielordner liegen.
- **Quellen:** https://github.com/rakanki911/DLSS5-Swapper · https://help.ea.com/en/articles/battlefield/battlefield-6/play-by-the-rules/

## M9 — Defender entfernt Deep Fried Chicken
- **Ursache:** Detours-Hooking auf die NGX-Runtime, heuristischer Fehlalarm.
- **Fix:** Nur mit Einverständnis eine Ordner-Ausnahme setzen (Sicherheitsabstrich benennen). Sonst OptiScaler-DLSSNR nehmen, das braucht keine Ausnahme (Defender-Scan am 21.09.2026 ohne Befund).
- **Quelle:** https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md

## M10 — Bildfehler mit NVIDIA Smooth Motion
- **Fix:** Smooth Motion für Spiele mit Mod ausschalten.
- **Quelle:** https://github.com/jlrouzies-fr/DLSS5-Feeder/issues/1

## M12 — CoD MW2 (2022): Spiel stürzt direkt nach dem Start ab (VERIFIZIERT 21.09.2026 17:59)
- **Symptom:** MW2 startet nicht. Windows meldet `cod22-cod.exe`, Modul `ntdll.dll`, Ausnahme `0xc0000005`, zweimal hintereinander, ca. 1 s nach dem Start.
- **Beleg:** `OptiScaler.log` im Spielordner endet regulär mit „Init done“ und der GPU-Erkennung (RTX 5090). Direkt danach kommt der Absturz. Der Ricochet-Kerneltreiber `atvi-hrist_sr` war noch nicht einmal gestartet, abgeschossen hat also der Manipulationsschutz im Spielprozess.
- **Fix:** Mod entfernen (`entfernen.ps1 -Spiel 'Modern Warfare II'`), danach startet MW2 wieder. Keine anderen Proxy-Namen probieren: Das wäre gezieltes Umgehen des Anti-Cheats. CoD-Titel bleiben im Installer gesperrt.
- **Quelle:** eigene Messung (Treiber 616.92, OptiScaler-DLSSNR v0.2.0, MW2 Steam)

## M13 — Cyberpunk: CET/RED4ext/RedScript-Mods brechen mit OptiScaler als `dxgi.dll`
- **Symptom:** Script-Mods (Ultra+, Nova City, LUT Switcher, DreamPunk-Listen) laden nicht oder das Spiel „flatlined“, sobald OptiScaler-DLSSNR v0.2.0 als `dxgi.dll` in `bind` liegt. Gemeldet für 2.3.1, ungelöst.
- **Workaround:** OptiScaler als `.asi` über einen ASI-Loader in `bind\plugins\` laden (kostet laut Bericht ca. 10 FPS, bei neueren Builds unzuverlässig), oder auf diese Mods verzichten.
- **Quellen:** https://github.com/optiscaler/OptiScaler/issues/313 · Research 21.09.2026 (Issue zum DLSSNR-Fork mit genau diesem Setup)

## M14 — Cyberpunk: automatischer Belichtungs-Scan greift nicht (HDR)
- **Symptom:** In HDR stimmt der Weißpunkt des Neural Pass nicht, Lichter wirken ausgebrannt oder flach.
- **Ursache:** Cyberpunk berechnet die Belichtung in einer Form, die der Scan nicht sieht. Der Mod fällt auf den manuellen Regler zurück.
- **Fix:** Paper White (`WhitePointScale`) von Hand kalibrieren oder SDR spielen. Der früher dabei mögliche Absturz (use-after-free mit RR + Streamline) ist seit v0.2.0 behoben.
- **Quellen:** https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 · https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

## M15 — Flimmern bei Model Resolution unter 100 %
- **Symptom:** Bei `WorkingScale` um 0,7 flimmert das Bild beim Kameraschwenk (Cyberpunk, RE Requiem, TLoU 2).
- **Fix:** `WorkingScale` 1,0 lassen. Sonst `TransferStrength` ca. 15 % senken, `MaxRatio` knapp unter 2,0, `Intensity` < 1,5.
- **Quelle:** https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 (04.09.2026)

## M11 — AnvilNext (AC-Reihe): Tiefenpuffer geht bei Kamerawechsel verloren
- **Symptom:** Bei der ReShade-/Feeder-Route fällt der Effekt in Dialogen und Zwischensequenzen aus.
- **Einordnung:** Für AC Mirage belegt, auf AC Shadows übertragbar. Betrifft nur die ReShade-Route, nicht OptiScaler (der liest Tiefe und Bewegungsvektoren aus dem DLSS-Aufruf).
- **Quelle:** https://reshade.me/forum/troubleshooting/9101-assassins-creed-mirage-loss-of-depth-buffer
