# DLSS-Override (NVIDIA-Treiber) für CoD Modern Warfare II (2022)

Erzwingt über das Treiberprofil das neueste DLSS-Super-Resolution-Modell (Transformer, Preset „Recommended“) statt der DLSS-2.4.12-Datei aus dem Spiel. **Keine Spieldatei wird verändert.**

| Datei | Zweck |
|-------|-------|
| `einrichten.ps1` | Einmal ausführen (ein UAC-Klick): lädt NVIDIA Profile Inspector nach `%LOCALAPPDATA%\nvidiaProfileInspector`, importiert `mw2-dlss.nip` per Merge, schaltet den DLSS-Indikator an und prüft das Ergebnis per Export. Log: `%TEMP%\dlss-override-einrichten.log`. `-KeinIndikator` lässt die Anzeige aus. |
| `mw2-dlss.nip` | Treiberprofil „Call of Duty: Modern Warfare 2 (2022)“ (`cod22-cod.exe` und `sp22-cod.exe`): DLL-Override an, Model Preset Profile = Recommended, Preset Letter = Use recommended. |
| `indikator-umschalten.ps1` | DLSS-Anzeige unten links im Spiel an/aus. |

Nach einem Treiber-Neuinstall mit „Clean Install“ ist das Profil weg, dann `einrichten.ps1` erneut starten.
Hintergrund und Risiken: `best-practices/gaming/nvidia-dlss-override.md`, `bugs/gaming/nvidia-dlss-override.md`.
