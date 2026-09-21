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

## B9 — Profile-Inspector-Override wird bei Nicht-Whitelist-Spiel ignoriert (VERIFIZIERT)
- **Symptom:** Das Profil ist korrekt gesetzt (Export zeigt SR-Override=1, Preset=Recommended), trotzdem gibt es keine DLSS-Anzeige und keine neue Version.
- **Beleg (21.09.2026, Treiber 616.92, CoD MW2 2022, DLSS 2.4.12, DLAA aktiv):** Das NGX-Log zeigt `DRS PROFILE FOUND : "Call of Duty: Modern Warfare 2 (2022)"`, danach `Feature dlss failed to load ... from cache (versions\0 ... 160_8741FBC.bin / 160_E658703.bin)` und dann `feature dlss snippet: ...\nvngx_dlss.dll version: 2.4.12`. Der Override-Snippet `dlss_override app_E658700=310.9.0` liegt bereit, wird aber nicht gezogen.
- **Ursache (Vermutung):** Der Treiber wendet den SR-Override nur bei von NVIDIA freigegebenen Titeln an. Die NVIDIA App schreibt dafür zusätzlich die „Override Reserved Key“-Settings (0x10C7D684/0x10C7D82C), die sich nicht nachbauen lassen. Die Profile-Inspector-Anleitungen im Netz („funktioniert in jedem Spiel“) gelten für diesen Treiberstand nicht.
- **Fix:** Einen funktionserhaltenden Weg ohne Dateitausch gibt es nicht. Die Alternative wäre der DLL-Tausch im Spielordner, der in Anti-Cheat-Spielen ein Ban-Risiko hat (B3). Deshalb die mitgelieferte DLSS/DLAA-Version nutzen.
- **Diagnose-Weg (Poka-Yoke):** Vor jeder Override-Aussage das NGX-Log prüfen: `HKLM\SOFTWARE\NVIDIA Corporation\Global\NGXCore` → `LogLevel`=1, Spiel starten und die Logs im spieleigenen Log-Ordner lesen (MW2: `%LOCALAPPDATA%\Activision\Call of Duty MWII\crash_reports\gpu\nvngx.log`), danach `LogLevel` wieder löschen. Der offizielle Indikatorwert laut NVIDIA/DLSS-Repo ist `ShowDlssIndicator`=1 (nicht 0x400). Keiner der beiden Werte zeigte bei MW2 mit DLSS 2.4.12 eine Anzeige, deshalb ist das Log der verlässliche Beweis.
- **Quelle:** eigene Messung, https://github.com/NVIDIA/DLSS/tree/main/utils

## B8 — „DLSS 5 Mod“ auf YouTube: echtes NVIDIA-Modell, aber inoffiziell eingeschleust (korrigiert 21.09.2026 16:26)
- **Symptom:** Videos zeigen „DLSS 5“ in alten Spielen, z. B. „3x DLSS 5 Call of Duty Modern Warfare 2 DMZ Offline“ (TheGr08).
- **Ursache:** Kein reiner Filter, wie hier zuerst stand: Die Mods (DLSS5-Feeder über ReShade, OptiScaler-DLSSNR, RenoDX) schleusen **NVIDIAs echtes DLSS-5-Modell** `nvngx_dlssnr.dll` ein, das aus dem NBA-2K27-Early-Access geleakt wurde. Offiziell ist das nicht, und per Treiber-Override geht es nicht.
- **Fix:** Nur in Singleplayer-Spielen ohne Anti-Cheat. In CoD (Ricochet) droht ein Bann, auch offline in der Kampagne. Alles Weitere: eigener Bereich `bugs/gaming/dlss5-mod-inoffiziell.md` und Werkzeug `Werkzeuge/dlss5-mod/`.
- **Quellen:** https://www.youtube.com/watch?v=uAHkSDk9LRo · https://www.heise.de/en/background/DLSS-5-mod-tried-out-Why-all-the-fuss-11434544.html

## B10 — Eigenbau-Fallen beim Setzen von Overrides ohne Profile Inspector (21.09.2026, Treiber 616.92)
- **NVAPI lehnt 0x00634291 ab:** `NvAPI_DRS_SetSetting` meldet für „DLSS - Forced Model Preset Profile“ `-160` (SETTING_NOT_FOUND). Der Preset-Buchstabe 0x10E41DF3 = 0x00FFFFFF („Use recommended“) reicht. Fertiger NVAPI-Schreiber mit Rücklesen: `Werkzeuge/dlss-override/spiele-einrichten.ps1`, Anzeige: `werte-lesen.ps1`.
- **Kaputte `.nip` lässt NVPI im Silent-Import hängen:** Bei ungültigen Einträgen (leere `SettingValue`) zeigt `-silent -mergeImport` ein Fenster „Error“ (`Import Error: …`) und blockiert. Läuft NVPI als Admin, kann ein normaler Prozess das Fenster weder lesen noch fotografieren (UIPI). Vorher legte NVPI schon ein neues Profil ohne Exe an. Poka-Yoke: `.nip` vor dem Import per `[xml]` zurücklesen und jede `SettingValue` prüfen.
- **Zwei PowerShell-Fallen beim Erzeugen der `.nip`:** `@(@(a,b,c))` wird zu einem flachen Array (ein Einzel-Setting zerfällt in drei Skalare), und `$s` überschreibt `$S` (Variablennamen ohne Groß-/Kleinschreibung). Mit `[pscustomobject]`-Listen und eindeutigen Namen arbeiten.
- **Quelle:** eigene Messung

## B7 — MW2 2022: „Shader Optimization“ hängt / DLSS schaltet sich ab
- **Symptom:** Nach Grafik- oder Treiberänderung hängt die Shader-Optimierung, oder die DLSS-Option verschwindet bzw. springt zurück.
- **Fix:** Shader-Cache löschen (`shadercache` im Spielordner bzw. `%localappdata%\Activision\Call of Duty\ShaderCache`), Steam-Integritätsprüfung ausführen, Overlays aus.
- **Quellen:** https://dotesports.com/call-of-duty/news/mw2-shader-optimization-stuck-try-this · https://steamcommunity.com/app/1938090/discussions/0/3721693063166606705/
