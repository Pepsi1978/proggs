# Kurzcheck: DLSS 5 Mod inoffiziell (Stand 21.09.2026 16:26, Treiber 616.92, OptiScaler-DLSSNR 0.2.0, nvngx_dlssnr 310.8.0)
- Der Mod ist NVIDIAs echtes, geleaktes DLSS-5-Modell `nvngx_dlssnr.dll`, nur RTX 50. Nicht per App/Profile Inspector erzwingbar.
- Spiel mit DLSS/FSR/XeSS (64 Bit) → OptiScaler-DLSSNR (`dxgi.dll` + Forwarder + Modell im Exe-Ordner, `[DlssNr] Enabled=true`). Ohne Upscaler → DLSS5-Feeder + ReShade 6.8 Add-on.
- Nur Original-Releases, SHA-256 gegen GitHub-`digest`, Modell-DLL nur mit gültiger NVIDIA-Authenticode-Signatur. imod.gg meiden. Keine Binärdateien ins Repo.
- Nie in Kernel-Anti-Cheat-Spielen (BF6, CoD inkl. Kampagne, Delta Force, PUBG). Feste Spieleliste statt Ordnersuche.
- Treiber ≥ 616.64: renodx-dlss5 4.6/4.7 kaputt → OptiScaler-DLSSNR oder DFC. Smooth Motion aus. Kosten ca. 45–50 % FPS.
Werkzeug: `Werkzeuge/dlss5-mod/` · Volltext: `dlss5-mod-inoffiziell.md`
