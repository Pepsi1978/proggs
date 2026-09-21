# NVIDIA DLSS Override (Treiber-seitig neuestes DLSS erzwingen) — Best Practices

> Stand: 21.09.2026 15:28 · Versions-Anker: NVIDIA-Treiber 616.92, RTX 5090 (Blackwell), DLSS 4.5 (2nd-Gen-Transformer, Presets K/L/M), DLSS 5 (seit 03.09.2026) · Anlass: CoD Modern Warfare II (2022, Steam, liefert nvngx_dlss.dll 2.4.12)
> Gegenstück: `bugs/gaming/nvidia-dlss-override.md` · Kurzcheck: `nvidia-dlss-override-kurzcheck.md`

## 1. Was lässt sich überhaupt erzwingen?
- **Super Resolution (SR)** lässt sich in jedes Spiel mit DLSS 2.0 oder neuer per Treiber-Override nachrüsten, also aktuelles Transformer-Modell statt Spiel-DLL. `offiziell` https://www.nvidia.com/en-us/geforce/news/gfecnt/20251/nvidia-app-update-dlss-overrides-and-more/
- **(Multi) Frame Generation Override** greift nur, wenn das Spiel selbst schon Frame Generation integriert hat (Streamline). Ohne native FG, wie bei MW2 2022, ist das wirkungslos. `offiziell` https://developer.nvidia.com/blog/how-to-integrate-nvidia-dlss-4-into-your-game-with-nvidia-streamline
- **Smooth Motion** ist Frame Generation im Treiber und funktioniert spielunabhängig (DX11/DX12/Vulkan, RTX 40/50). Sie erhöht aber die Eingabeverzögerung, deshalb nur im Singleplayer und nie im kompetitiven Multiplayer. `offiziell` https://www.nvidia.com/en-us/geforce/news/gfecnt/20258/nvidia-app-global-dlss-overrides-rtx-40-series-smooth-motion/ · `extern` https://www.xda-developers.com/tried-nvidias-smooth-motion-older-games-works-best-when-dont-need-it/
- **DLSS 5** (GTC 2026, Start 03.09.2026 mit NBA 2K27) ist ein eigenes generatives Neural-Rendering-System. Es muss pro Spiel integriert werden und ist **nicht** über das Override-Menü erzwingbar. Das inoffizielle „DLSS5 Swapper“ ist nicht von NVIDIA. `offiziell` https://nvidianews.nvidia.com/news/nvidia-dlss-5-delivers-ai-powered-breakthrough-in-visual-fidelity-for-games

## 2. Weg A: NVIDIA App (nur Whitelist-Titel)
Grafik → Programmeinstellungen → Spiel wählen → Treiber-Einstellungen → „DLSS Override – Model Presets“ → „Recommended“.
Die Option erscheint **nur für Titel auf NVIDIAs Kompatibilitätsliste**, der Global-Schalter umgeht das nicht. CoD MW2 2022 steht nicht darauf (Stand 09/2026, von CoD nur BO7). Liste: https://www.nvidia.com/en-us/geforce/news/nvidia-rtx-games-engines-apps/

## 3. Weg B: NVIDIA Profile Inspector (für Titel ohne Whitelist)
- Werkzeug: https://github.com/Orbmu2k/nvidiaProfileInspector (aktiv gepflegt), **als Administrator** starten.
- Profil des Spiels suchen, bei MW2 2022 per Exe `cod22-cod.exe` bzw. Suchfeld „Call of Duty“. Fehlt es: eigenes Profil anlegen und die Exe über „Add application“ binden.
- Sektion „5 - Common“: `DLSS - Enable DLL Override` = „On - DLSS-SR overridden by latest installed“ (NVPI 3.0.2.1 kennt kein „latest available“), `DLSS - Forced Preset Letter` = „Use recommended preset“ (0x00FFFFFF, ergibt K bei Quality/Balanced, M bei Performance und L bei Ultra Performance; ein festes K würde Gen 1 auch im Performance-Modus erzwingen), `DLSS - Forced Model Preset Profile` = Recommended → Apply.
- **Ohne Klicks:** Setting-IDs `0x10E41E01`=1, `0x00634291`=1, `0x10E41DF3`=0x00FFFFFF, in der `.nip` **dezimal** (283385345 / 6505105 / 283385331). Import mit `nvidiaProfileInspector.exe -silent -mergeImport datei.nip` (nur `-silent` = **Replace** und setzt das Profil zurück!). Danach mit `-exportCustomized` prüfen. Den echten Treiber-Profilnamen per NVAPI `DRS_FindApplicationByName` ermitteln; bei MW2 ist es „Call of Duty: Modern Warfare 2 (2022)“ mit `cod22-cod.exe` und `sp22-cod.exe`. Fertiges Werkzeug: `Werkzeuge/dlss-override/`.
- **Registry-Falle:** `New-Item -Force` auf den Schlüssel `NGXCore` legt ihn neu an und löscht die Treiberwerte `FullPath`/`Installed`. Stattdessen `if (-not (Test-Path $k)) { New-Item $k }`. `extern` https://www.xda-developers.com/how-to-get-dlss-45-working-in-any-unsupported-title/
- Presets A–E gelten als veraltet. Das Treiber-Modell-Cache liegt unter `C:\ProgramData\NVIDIA\NGX\models` und lässt sich ohne NVIDIA App mit nvidiaDlssGlom/dlssdl befüllen. `extern` https://forums.guru3d.com/threads/nvidiadlssglom-manually-update-the-latest-dlss-models-without-nvidia-app.457047/
- Nicht gleichzeitig Overrides in der NVIDIA App **und** im Profile Inspector für denselben Titel pflegen, weil sich beide gegenseitig überschreiben.

## 4. Verifikation (immer!)
`HKLM\SOFTWARE\NVIDIA Corporation\Global\NGXCore` → DWORD `ShowDlssIndicator` = `0x400` (1024; der Wert 1 wirkt nur mit Dev-DLLs). Im Spiel erscheint dann unten links ein Overlay mit Version und Preset. Ausschalten mit 0. `offiziell` https://github.com/NVIDIA/DLSS/blob/main/utils/ngx_driver_onscreenindicator.reg

## 5. Anti-Cheat-Spiele (Ricochet/EAC/BattlEye)
- **Nie** DLLs im Spielordner tauschen (manuell, DLSS Swapper, DLSSTweaks, OptiScaler). Das ist Dateimanipulation und führt in CoD nachweislich zu Bans. `extern` https://www.codforums.com/threads/dlss-override-ban.23058/ · OptiScaler warnt selbst: https://github.com/optiscaler/OptiScaler
- Der Override im Treiber ändert keine Spieldatei und ist deshalb der risikoärmere Weg. Es gibt aber vereinzelte, **ungeklärte** Ban-Meldungen in CoD auch nach einem Override über den Profile Inspector. Auf eigenes Risiko und am besten nicht mit dem Hauptaccount. `extern` https://forums.guru3d.com/threads/i-get-permaban-in-cod-after-dlss-override.455671/

## 6. CoD MW2 (2022) konkret
- Seit 29.07.2025 aus COD HQ gelöst und eigenständig (`cod22-cod.exe`, Kampagne in `sp22\`). https://www.windowscentral.com/gaming/xbox/call-of-dutys-bloated-hq-launcher-now-officially-decoupled-by-activision-heres-what-you-need-to-know
- Im Spiel: Grafik → Qualität → „Upscaling/Sharpening“ = NVIDIA DLSS, Stufe Quality (1440p) bzw. Balanced/Quality (4K). Reflex steht **nicht** im Reiter „Qualität“, sondern unter Grafik → Reiter „Anzeige“ → „NVIDIA Reflex Low Latency“ = Aktiviert + Boost (Nutzer suchte es am 21.09.2026 vergeblich unter „Qualität“). **Kein natives DLAA** (erst ab MW3). https://www.charlieintel.com/call-of-duty-modern-warfare-2/best-modern-warfare-2-pc-settings-199966/
