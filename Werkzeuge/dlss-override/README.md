# DLSS-Override (NVIDIA-Treiber) für CoD Modern Warfare II (2022)

Erzwingt über das Treiberprofil das neueste DLSS-Super-Resolution-Modell (Transformer, Preset „Recommended“) statt der DLSS-2.4.12-Datei aus dem Spiel. **Keine Spieldatei wird verändert.**

| Datei | Zweck |
|-------|-------|
| `einrichten.ps1` | Einmal ausführen (ein UAC-Klick): lädt NVIDIA Profile Inspector nach `%LOCALAPPDATA%\nvidiaProfileInspector`, importiert `mw2-dlss.nip` per Merge, schaltet den DLSS-Indikator an und prüft das Ergebnis per Export. Log: `%TEMP%\dlss-override-einrichten.log`. `-KeinIndikator` lässt die Anzeige aus. |
| `mw2-dlss.nip` | Treiberprofil „Call of Duty: Modern Warfare 2 (2022)“ (`cod22-cod.exe` und `sp22-cod.exe`): DLL-Override an, Model Preset Profile = Recommended, Preset Letter = Use recommended. |
| `indikator-umschalten.ps1` | DLSS-Anzeige unten links im Spiel an/aus. |

Nach einem Treiber-Neuinstall mit „Clean Install“ ist das Profil weg, dann `einrichten.ps1` erneut starten.
Hintergrund und Risiken: `best-practices/gaming/nvidia-dlss-override.md`, `bugs/gaming/nvidia-dlss-override.md`.

## Alle anderen DLSS-Spiele (ohne MW2)

| Datei | Zweck |
|-------|-------|
| `spiele-einrichten.ps1` | Einmal ausführen (ein UAC-Klick): schreibt die Werte aus `spiele-dlss.nip` **direkt per NVAPI** in die Treiberprofile (kein Profile Inspector nötig), legt fehlende Profile an, bindet die Exe und liest jeden Wert zurück. Log: `%TEMP%\dlss-spiele-einrichten.log`. |
| `spiele-dlss.nip` | 10 Profile: AC Shadows, Battlefield 6, CoD Black Ops 7 (= `cod.exe` aus COD HQ), CoD MW3 (2023), Cyberpunk 2077, Delta Force, DREADZONE, Manor Lords, Star Trek Voyager (eigenes Profil, `stvoyagersteam-win64-shipping.exe`), Starfield. SR-Override + Preset „Use recommended“ überall, RR-Override (+ Preset recommended) wo das Spiel `nvngx_dlssd.dll` mitbringt, FG-Override wo `nvngx_dlssg.dll` liegt. |
| `profil-finden.ps1` | Zeigt per NVAPI, in welchem Treiberprofil eine Exe steckt (ohne Admin, nur lesend). |

„DLSS - Forced Model Preset Profile“ (0x00634291) fehlt absichtlich: `NvAPI_DRS_SetSetting` lehnt die ID mit -160 (Setting not found) ab, der Preset-Buchstabe „Use recommended“ reicht.
Neues Spiel dazu: Exe mit `profil-finden.ps1` prüfen, Profil in `spiele-dlss.nip` ergänzen, `spiele-einrichten.ps1` erneut starten.
