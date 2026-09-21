# Bug-Almanach: NVIDIA DLSS Override (NVIDIA App / Profile Inspector / DLL-Tausch)

> Stand: 21.09.2026 15:28 · Versions-Anker: NVIDIA-Treiber 616.92, NVIDIA App (09/2026), NVIDIA Profile Inspector (Orbmu2k, aktuell), DLSS 4.5 · Gegenstück: `best-practices/gaming/nvidia-dlss-override.md`

## B1 — DLSS-Override-Option fehlt/ausgegraut in der NVIDIA App
- **Symptom:** „DLSS Override – Model Presets“ fehlt, ist ausgegraut oder zeigt „Unsupported“/„Failed to get current values“ (z. B. bei CoD MW2 2022).
- **Ursache:** Der Titel steht nicht auf NVIDIAs Whitelist. Der Global-Schalter umgeht das nicht.
- **Versionen:** alle App-Versionen bis 09/2026.
- **Fix:** Override über den NVIDIA Profile Inspector setzen (siehe Best Practices §3). Das Editieren von `ApplicationStorage.json` funktioniert ab Treiber 572.42 nicht mehr zuverlässig.
- **Quellen:** https://www.nvidia.com/en-us/geforce/news/nvidia-rtx-games-engines-apps/ · https://forums.guru3d.com/threads/how-to-enable-dlss-overrides-in-nvidia-app-on-unsupported-games.455225/

## B2 — Frame-Generation-Override ohne Wirkung
- **Symptom:** FG/MFG-Override gesetzt, aber keine zusätzlichen Frames.
- **Ursache:** Das Spiel hat keine native Frame Generation (Streamline), z. B. MW2 2022, das nur DLSS-SR mitbringt. Das ist eine Funktionsgrenze, kein Fehler.
- **Fix:** Smooth Motion (Treiber-FG) nutzen, nur im Singleplayer.
- **Quelle:** https://www.nvidia.com/en-us/geforce/news/nvidia-app-update-dlss-overrides-and-more/

## B3 — Ban durch DLL-Tausch in Anti-Cheat-Spielen (CoD/Ricochet)
- **Symptom:** Bann oder Shadowban nach dem Tausch von `nvngx_dlss.dll` (manuell, DLSS Swapper, DLSSTweaks, OptiScaler).
- **Ursache:** Ricochet prüft die Dateiintegrität und wertet den DLL-Tausch als Mod.
- **Fix:** Nie Spieldateien tauschen, nur den Treiber-Override nutzen. Vereinzelte, ungeklärte Ban-Meldungen gibt es auch nach einem Profile-Inspector-Override (ein Fall stellte sich später als ReFS-/24H2-Problem heraus), deshalb auf eigenes Risiko.
- **Quellen:** https://www.codforums.com/threads/dlss-override-ban.23058/ · https://forums.guru3d.com/threads/i-get-permaban-in-cod-after-dlss-override.455671/ · https://github.com/Orbmu2k/nvidiaProfileInspector/issues/296 · https://github.com/emoose/DLSSTweaks/issues/80

## B4 — Profile Inspector: Forced Preset greift nur in Performance-Modi
- **Symptom:** Preset M/L wird nur bei Performance/Ultra Performance angewendet, nicht bei Quality/DLAA.
- **Ursache/Status:** offenes Issue. Die Recommended-Zuordnung (K für Quality/Balanced) ist so vorgesehen.
- **Fix:** Für Quality/Balanced Preset K nehmen und mit dem Indikator prüfen.
- **Quelle:** https://github.com/Orbmu2k/nvidiaProfileInspector/issues/371

## B5 — Profile Inspector: DLL-Override ohne Wirkung
- **Symptom:** Der Indikator zeigt weiter das alte Preset bzw. die alte DLL.
- **Ursache:** Das Treiber-Modell-Cache (`C:\ProgramData\NVIDIA\NGX\models`) ist leer oder veraltet, weil keine NVIDIA App installiert ist.
- **Fix:** Treiber-Cache unter `C:\ProgramData\NVIDIA\NGX\models\dlss\versions` prüfen und bei Bedarf mit nvidiaDlssGlom/dlssdl füllen. Die Option „latest available“ gibt es in NVPI 3.0.2.1 nicht.
- **Quellen:** https://github.com/Orbmu2k/nvidiaProfileInspector/issues/380 · https://forums.guru3d.com/threads/dlssdl-download-latest-dlss-ota-files.461660/

## B6 — Profilkonflikt CoD MW2 (2022) / MW3 (2023)
- **Symptom:** „existing profile conflict with the same application name“, oder der Override landet auf dem falschen CoD-Teil.
- **Fix:** Profil gezielt über den absoluten Exe-Pfad (`...\Call of Duty Modern Warfare II\cod22-cod.exe`) binden.
- **Quelle:** https://github.com/Orbmu2k/nvidiaProfileInspector/issues/346

## B8 — „DLSS 5 Mod“ auf YouTube ist kein NVIDIA DLSS 5
- **Symptom:** Videos zeigen „DLSS 5“ in alten Spielen, z. B. „3x DLSS 5 Call of Duty Modern Warfare 2 DMZ Offline“ (TheGr08, aufgenommen auf einer RTX 4090, obwohl echtes DLSS 5 nur auf RTX 50 läuft).
- **Ursache:** Das ist ein ReShade-/Filter-Mod („DLSS 5 MOD“, Download über imod.gg, Tags #ReShade #DLSS5feeder) und kein NVIDIA-Feature. Er läuft dort im **Offline**-Modus ohne Anti-Cheat.
- **Fix:** Nicht als echtes DLSS 5 behandeln. Im Online-Multiplayer injiziert ReShade Code ins Spiel, das hat mit Ricochet ein hohes Ban-Risiko. Nur offline oder mit einem Zweitaccount.
- **Quelle:** https://www.youtube.com/watch?v=uAHkSDk9LRo (Videobeschreibung, abgerufen 21.09.2026)

## B7 — MW2 2022: „Shader Optimization“ hängt / DLSS schaltet sich ab
- **Symptom:** Nach Grafik- oder Treiberänderung hängt die Shader-Optimierung, oder die DLSS-Option verschwindet bzw. springt zurück.
- **Fix:** Shader-Cache löschen (`shadercache` im Spielordner bzw. `%localappdata%\Activision\Call of Duty\ShaderCache`), Steam-Integritätsprüfung ausführen, Overlays aus.
- **Quellen:** https://dotesports.com/call-of-duty/news/mw2-shader-optimization-stuck-try-this · https://steamcommunity.com/app/1938090/discussions/0/3721693063166606705/
