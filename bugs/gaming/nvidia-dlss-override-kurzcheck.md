# Kurzcheck Bugs: NVIDIA DLSS Override (Stand 21.09.2026, Treiber 616.92)
- B1 Override fehlt in der NVIDIA App → Titel nicht auf der Whitelist → Profile Inspector nutzen.
- B2 FG-Override ohne Wirkung → Spiel hat keine native FG → Smooth Motion (nur Singleplayer).
- B3 CoD-Ban durch DLL-Tausch → nie Spieldateien tauschen. Auch der Treiber-Override hat ein Restrisiko.
- B4 Forced Preset M/L nur in Performance-Modi → K für Quality/Balanced.
- B5 DLL-Override ohne Wirkung → NGX-Cache leer → NGX-Cache prüfen bzw. nvidiaDlssGlom.
- B6 Profilkonflikt MW2/MW3 → Exe per absolutem Pfad binden.
- B7 Shader-Optimierung hängt → Shader-Cache löschen, Dateien prüfen.
- B8 „DLSS 5 Mod“ (YouTube/imod.gg) = ReShade-Filter, kein NVIDIA DLSS 5 → nur offline, online Ban-Risiko.
- B9 Profile-Inspector-Override bei Nicht-Whitelist-Spiel wirkungslos (MW2, Treiber 616.92, per NGX-Log verifiziert). Immer mit `LogLevel`=1 und NGX-Log prüfen, nie nur mit dem Indikator.
Volltext: `nvidia-dlss-override.md`
