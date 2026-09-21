# DLSS 5 & Grafik-Optimierung — Wissensstand (Einstieg)

> Stand: 21.09.2026 18:40 · System: RTX 5090, NVIDIA-Treiber 616.92, Windows 11, Steam-Bibliotheken `D:\SteamLibrary`, `G:\SteamLibrary`
> Zweck: Startpunkt für jede spätere Frage wie „Wie holen wir noch mehr aus der Grafik raus?“. Erst diese Seite lesen, dann die verlinkten Detaildateien.

## 1. Das Wichtigste in vier Sätzen
1. Echtes DLSS 5 (NVIDIA Neural Rendering) muss das Spiel selbst einbauen. Offiziell läuft es bisher nur in NBA 2K27. **Starfield, AC Shadows und Delta Force** sind angekündigt, noch ohne Termin.
2. Der „DLSS 5 Mod“ aus YouTube schleust NVIDIAs echtes, geleaktes Modell `nvngx_dlssnr.dll` über **OptiScaler-DLSSNR** in Spiele, die schon DLSS haben. Das Bild läuft nach dem Upscaler und vor dem HUD durch das Modell.
3. Nur in Singleplayer-Spielen ohne Kernel-Anti-Cheat. **CoD MW2 wurde probiert und stürzt ab** (Manipulationsschutz), BF6, CoD, Delta Force und PUBG sind tabu.
4. Treiber-Overrides (neuestes DLSS 4.5) sind für 10 Spiele gesetzt. Ohne die Freigabeschlüssel der NVIDIA App ignoriert Treiber 616.92 sie aber vermutlich.

## 2. Aufbau auf diesem PC
| Spiel | DLSS-5-Mod | Spiel-DLLs (SR / RR / FG) | Treiber-Override | Offizielles DLSS 5 |
|---|---|---|---|---|
| Cyberpunk 2077 (2.31) | ✅ läuft (verifiziert, 7,7 ms/Bild), Start-Preset gesetzt | 310.1 / 310.1 / 310.1 | gesetzt, Wirkung offen | – |
| Starfield | ✅ installiert, ungetestet | **3.5** / – / 3.5 | gesetzt, Wirkung offen | angekündigt |
| Assassin's Creed Shadows | ✅ installiert, ungetestet | 310.2 / – / 310.2 | gesetzt, Wirkung offen | angekündigt |
| Manor Lords | ✅ installiert, ungetestet | 310.2 / 310.2 / 310.2 | gesetzt, Wirkung offen | – |
| Star Trek Voyager – Across the Unknown | ✅ installiert, ungetestet | 310.4 / 310.4 / 310.4 | gesetzt (eigenes Profil), Wirkung offen | – |
| CoD MW2 (2022) | ❌ stürzt ab (Almanach M12), entfernt | 2.4.12 | andere Sitzung | – |
| Battlefield 6, CoD BO7/MW3, Delta Force, PUBG | ❌ tabu (Anti-Cheat) | – | gesetzt (außer PUBG) | Delta Force angekündigt |
| DREADZONE | nicht installiert (Anti-Cheat unklar) | 310.6 | gesetzt | – |
| Civilization VII | nicht installiert (kein DLSS, nur FSR/XeSS) | – | – | – |

Treiber-Cache (`C:\ProgramData\NVIDIA\NGX\models`): DLSS 310.9, Streamline 2.14, **kein** NR-Modell.

## 3. Beste Einstellungen (Stand heute)
**Im Spiel (Cyberpunk, gilt sinngemäß für andere Path-Tracing-Spiele):** Path Tracing an, Ray Reconstruction an, DLSS Quality (DLAA nur bei FPS-Reserve), DLSS-Preset K statt L, Frame Generation 3x–4x, Bewegungsunschärfe/Filmkörnung/chromatische Aberration aus, SDR.

**Im Mod-Menü (Einfg → „DLSS Neural Rendering“) bzw. `OptiScaler.ini` `[DlssNr]`:**
| Regler | INI-Schlüssel | Start-Wert | Hinweis |
|---|---|---|---|
| Detail strength | `TransferStrength` | 0,85 | 0 = Mod aus, 1 = voll, >1 übertrieben |
| Highlight guard | `MaxRatio` | 1,8 | gegen überstrahlte Neonlichter |
| Model resolution | `WorkingScale` | 1,0 | < 1,0 flimmert in Cyberpunk, > 1,0 = Supersampling (teurer, ruhiger) |
| Colour strength | `ColourStrength` | 1,0 | 0 = Spielfarben, > 1 grell |
| Intensity | `Intensity` | 1,0 | unter 1,5 bleiben |
| Skin structure | `SkinStructure` | -1 (= wie Local structure) | bei Wachsgesichtern niedriger als `LocalStructure` |
| Proxy-Modus | nur im Overlay | „Hybrid proxy + composed“ | „Replace“ lässt Lichter flackern |

## 4. Noch nicht genutzte Hebel (Fahrplan, nach Nutzen sortiert)
1. **NVIDIA App → Grafik → Globale Einstellungen → „DLSS Override – Model Presets“ = Recommended.** Nur so greift DLSS 4.5 in freigegebenen Spielen sicher. Danach mit `Werkzeuge/dlss-override/werte-lesen.ps1` prüfen, ob die „Key“-Spalten gefüllt sind.
2. **Neuere DLSS-Datei in Singleplayer-Spielen**, allen voran Starfield (nur DLSS 3.5, altes CNN-Modell). Der Mod verbessert das Bild nach dem Upscaler, ein besseres Ausgangsbild hilft ihm. Nur NVIDIA-signierte `nvngx_dlss.dll` 310.x nehmen (gleiche Signaturprüfung wie beim Modell), nie in Anti-Cheat-Spielen.
3. **Model resolution über 1,0** (bis 2,0) auf der 5090 testen, wenn mit Frame Generation genug FPS bleiben. Das Modell rechnet dann über nativer Auflösung und mittelt zurück, das Bild wird ruhiger.
4. **Die übrigen 4 Mod-Spiele einzeln testen** (Starfield, AC Shadows, Manor Lords, Star Trek Voyager): Log auf `DLSS-NR cost` prüfen, dann Regler wie in Abschnitt 3 anpassen. AC Shadows: Tiefenpuffer-Probleme betreffen nur die ReShade-Route, nicht OptiScaler.
5. **Textur-Mods für Cyberpunk** ohne CET/RED4ext: HD Reworked Project, Environment Textures Overhaul, Realistic Complexion (Haut). Script-Mods wie Ultra+ oder Nova City brechen mit dem Mod (Almanach M13).
6. **HDR** nur, wenn der Monitor es gut kann: dann Paper White (`WhitePointScale`) von Hand kalibrieren, weil der Auto-Scan in Cyberpunk nicht greift (M14).
7. **Civilization VII**: OptiScaler-DLSSNR kann auch über FSR/XeSS andocken. Das wäre ein Versuch wert, bringt in einem Strategiespiel aber wenig.
8. **Updates beobachten:** neue OptiScaler-DLSSNR-Releases (github.com/Dagherbou/OptiScaler_DLSSNR/releases, Diskussion #17), neuere `nvngx_dlssnr.dll`, offizielle DLSS-5-Patches für Starfield/AC Shadows/Delta Force. **Kommt ein offizieller Patch, dort vorher den Mod entfernen.**
9. **Vergleichen statt raten:** Debug-Ansicht „Difference“ im Overlay, die Vorher/Nachher-Bilder im Ordner `dlssnr-capture` neben der Spiel-Exe, FPS mit und ohne Mod.

## 5. Nie
- Mod in Spiele mit Kernel-Anti-Cheat (BF6, CoD, Delta Force, PUBG). Kein Umbenennen zum Umgehen des Schutzes.
- Downloads von imod.gg, Nexus 33280, GitHub „rrdlss5renoxdx“ oder Klon-Repos. Modell-DLLs ohne gültige NVIDIA-Signatur.
- Zwei DLSS-5-Mods gleichzeitig (OptiScaler-DLSSNR und ReShade/RenoDX), dann passiert gar nichts.
- Binärdateien (DLLs) ins Repo.

## 6. Prüfen und Werkzeuge
| Zweck | Befehl / Ort |
|---|---|
| Mod installieren (alle 5 Spiele oder eines) | `Werkzeuge\dlss5-mod\installieren.ps1 [-Spiel 'Starfield']` |
| Mod entfernen | `Werkzeuge\dlss5-mod\entfernen.ps1 [-Spiel 'Starfield']` |
| Läuft DLSS 5? | `OptiScaler.log` neben der Spiel-Exe, Zeile `DlssNr_Dx12::Dispatch DLSS-NR cost` |
| Treiber-Overrides + App-Keys anzeigen | `Werkzeuge\dlss-override\werte-lesen.ps1` |
| Overrides setzen | `Werkzeuge\dlss-override\spiele-einrichten.ps1` (ein UAC-Klick) |
| Exe → Treiberprofil | `Werkzeuge\dlss-override\profil-finden.ps1 <exe>` |
| Wirkt ein Override wirklich? | NGX-Log: `HKLM\SOFTWARE\NVIDIA Corporation\Global\NGXCore\LogLevel`=1, Spiel starten, `nvngx.log` lesen (Almanach B9) |
| Downloads (lokal, nicht im Repo) | `%LOCALAPPDATA%\DLSS5-Mod\downloads` |

## 7. Wo das Detailwissen liegt
- Best Practices: `best-practices/gaming/dlss5-mod-inoffiziell.md` (Abschnitt 4a = Cyberpunk) · `best-practices/gaming/nvidia-dlss-override.md`
- Bug-Almanach: `bugs/gaming/dlss5-mod-inoffiziell.md` (M1–M15) · `bugs/gaming/nvidia-dlss-override.md` (B1–B11)
- Werkzeuge: `Werkzeuge/dlss5-mod/README.md` · `Werkzeuge/dlss-override/README.md`
- Rohberichte der Recherchen vom 21.09.2026 (13 Researcher, Sonnet-5-Schwarm, alle Quellen-URLs): `rohberichte/`
  - Runde 1 (allgemein): `runde1-r1-identitaet` (was der Mod ist), `-r2-installation`, `-r3-kompatibilitaet` (deine Spiele), `-r4-anticheat`, `-r5-echtes-dlss5` (offizielle Liste), `-r6-sicherheit`, `-r7-alternativen` (RTGI, LumeniteFX, DLAA), `-r8-feeder-swapper`
  - Runde 2 (Cyberpunk): `runde2-cyberpunk-r1-regler`, `-r2-spieleinstellungen`, `-r3-realismus-mods`, `-r4-fertige-presets`, `-r5-probleme`
