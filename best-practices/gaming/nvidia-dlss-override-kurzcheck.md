# Kurzcheck: NVIDIA DLSS Override (Stand 21.09.2026, Treiber 616.92, DLSS 4.5 / DLSS 5)
- Nachrüstbar ist nur Super Resolution (Transformer, Presets K/M/L). FG/MFG braucht native Spielintegration, DLSS 5 ebenfalls.
- NVIDIA App → nur Whitelist-Titel. Sonst Profile Inspector (Admin): Enable DLL Override = latest installed, Forced Preset Letter = „Use recommended preset“. Ohne Klicks: `-silent -mergeImport` (nie nur `-silent`, das bedeutet Replace).
- Immer prüfen: `HKLM\SOFTWARE\NVIDIA Corporation\Global\NGXCore\ShowDlssIndicator` = 0x400.
- Anti-Cheat-Spiele: nie DLLs tauschen. Auch der Treiber-Override hat in CoD ein Restrisiko.
- Smooth Motion nur Singleplayer (Latenz).
Volltext: `nvidia-dlss-override.md`
