# Web-Researcher 5/5 — Bekannte Probleme & Lösungen: OptiScaler / OptiScaler-DLSSNR in Cyberpunk 2077 (2.2x/2.3x)

Kontext: RTX 5090, Treiber 616.92, Cyberpunk 2077 v2.31 (Steam, DX12), OptiScaler-DLSSNR v0.2.0 (Dagherbou-Fork) als dxgi.dll, nvngx_dlssnr.dll 310.8.0, Neural Rendering per INI aktiv, Dx12Upscaler=dlss. Zusätzlich NVIDIA-Treiberprofil-Override für DLSS-SR/RR/FG-DLLs.

Stand der Recherche: 21.09.2026.

---

## 1. Abstürze beim Start / Regression zwischen Versionen

**Symptom:** Cyberpunk 2077 stürzt sofort ab mit OptiScaler v0.9.3 und neuer, während v0.9.2a einwandfrei läuft.
**Ursache:** Nicht identifiziert (offenes Issue, Logs beider Versionen vorhanden, keine Root-Cause dokumentiert).
**Lösung:** Downgrade auf v0.9.2a als Workaround genannt.
**Version:** OptiScaler (nicht -DLSSNR-Fork) v0.9.3+ defekt, v0.9.2a funktioniert; Cyberpunk 2.31 (Steam), RTX 3080 Ti.
Quelle: https://github.com/optiscaler/OptiScaler/issues/1063

**Symptom:** OptiScaler-NR v0.8.3 (wilsjo2-Fork) crasht nach automatischer Installation via DLSS5-Autopilot — bei aktiviertem Neural Rendering "GPU Crash for unknown reasons" nach Auflösungswechsel, bei deaktiviertem NR "EXCEPTION_ACCESS_VIOLATION". Bei manueller Installation derselben Version stabil.
**Ursache:** Fehlkonfiguration durch das Autopilot-Tool (evtl. falsche Build-Variante oder falsche Dateiplatzierung), nicht OptiScaler selbst.
**Lösung:** Manuelle Installation statt Autopilot: dxgi.dll + OptiScaler.ini direkt aus dem Repo ins Spielverzeichnis kopieren.
**Version:** OptiScaler-NR v0.8.3, Cyberpunk 2.31, RTX 5070 Ti, Treiber 616.56.
Quellen: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/196 , https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/52

**Symptom:** "Cyberpunk 2077 – Cyberpunk has flatlined" beim gleichzeitigen Einsatz von OptiScaler-Nightly (15.03.2025), Cyber Engine Tweaks (CET), Ultimate ASI Loader und RED4Ext.
**Ursache:** DLL-Injektions-Konflikte — OptiScaler und die ASI-Loader konkurrieren um dieselben Einstiegspunkte.
**Lösung:** OptiScaler als OptiScaler.asi nach bin\x64\plugins\ verschieben (per ASI Loader geladen), inkl. amd_fidelityfx_dx12.dll, amdxcffx64.dll, libxess.dll. Funktionierte laut Bericht nur mit älteren Nightly-Builds; aktuelle Nightly unterstützte den .asi-Weg zum Berichtszeitpunkt nicht mehr.
Quelle: https://github.com/optiscaler/OptiScaler/issues/313

---

## 2. Neural Pass / DLSS-Neural-Rendering initialisiert nicht

**Symptom:** Auf Nicht-Blackwell-GPUs schlägt die offizielle nvngx_dlssnr.dll mit 0xBAD00001 bei "feature 18 create" fehl — das Neural-Rendering-Modell verweigert die Initialisierung.
**Ursache:** Die offizielle nvngx_dlssnr.dll ist hart auf RTX 50 (Blackwell) gesperrt.
**Lösung:** Community-gepatchte DLL (aus dem RenoDX-Discord), die die Blackwell-only-Funktionen zurückportiert. (Für den Nutzer mit RTX 5090 nicht relevant, da Blackwell nativ unterstützt wird.)
Quelle: WebSearch-Zusammenfassung, Kontext Nexus-Mod "DLSS 5 Neural Rendering Installation Guide" (nexusmods.com/cyberpunk2077/mods/33380) — Originalseite selbst lieferte HTTP 403, Aussage stammt aus Suchergebnis-Snippet, daher als unsicher markiert (siehe OFFEN/UNSICHER).

**Symptom:** "OptiScaler mentions neural rendering but never reports it running" — NR wird in Logs erwähnt, aber nie tatsächlich als aktiv gemeldet.
**Ursache:** Im konkreten Fall Fehlkonfiguration durch Auto-Installer (siehe Punkt 1, Issue #196).
**Lösung:** Manuelle Installation.
Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/196

**Symptom (Linux/Proton-spezifisch, aber technisch relevant):** Unter vkd3d-proton bleibt NR bei "build epoch 0" stecken — 0 "DLSS-NR cost"-Log-Zeilen, der Neural Pass läuft nie.
**Ursache:** Der in v0.7.5 eingeführte "Epoch-Gate" greift im vkd3d-Pfad nicht; der Fix aus PR #8 adressierte nur den DXVK-Pfad via LocalPresent(), diese Funktion wird im vkd3d-Pfad laut Log aber nie aufgerufen.
**Lösung:** v0.2.0 verwenden (keine Epoch-Validierung, funktioniert in dieser Konstellation) statt v0.7.5.
**Version:** v0.7.5 defekt (NR läuft nicht), v0.2.0 funktioniert (29 NR-Instanzen pro Testlauf); Cyberpunk 2.31, RTX 5090, Treiber 610.57.04 (Linux/nvidia-open-dkms), CachyOS.
Quelle: https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/24
**Hinweis:** Da der Nutzer exakt v0.2.0 verwendet, ist dieser Bug für ihn nicht relevant — aber bestätigt indirekt, dass v0.2.0 als vergleichsweise stabile NR-Baseline gilt.

**Exposure-Scan-Limitierung (Cyberpunk-spezifisch):** Cyberpunk berechnet die Belichtung "in a form the scan cannot see" — der automatische Exposure-Scan von OptiScaler-DLSSNR funktioniert bei Cyberpunk nicht zuverlässig.
**Lösung:** Manueller Fallback auf den Papierweiß-Schieber (Paper White) im Overlay statt automatischer Erkennung.
**Version:** v0.2.0.
Quellen: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 , https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

---

## 3. Ray Reconstruction (DLSSD) wird nicht abgefangen / ausgegraut

**Symptom:** Ray Reconstruction ist in Cyberpunk nach Installation von OptiScaler_DLSSNR (Dagherbou-Fork) ausgegraut/nicht wählbar, während die offizielle OptiScaler-Version RR normal anbietet.
**Ursache:** Nicht explizit dokumentiert (Issue ohne technische Erklärung geschlossen).
**Lösung:** Keine im Issue genannt.
**Version:** OptiScaler_DLSSNR (Fork), Version im Issue nicht spezifiziert.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/8

**Behoben in v0.2.0:** Ein "Exposure-scan use-after-free", der das Gerät bei Ray-Reconstruction + Streamline-Titeln (explizit Cyberpunk 2077 genannt) entfernen konnte — Scan pinnte eine treibereigene Ressource, deren Speicher beim Feature-Teardown bereits freigegeben war. Fix: Referenzen zuerst freigeben.
**Version:** Gefixt in v0.2.0.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

**Symptom:** "Access violation / GPU crash with RR+FG even when DlssNr is disabled" — Absturz auch wenn Neural Rendering deaktiviert ist, sobald Ray Reconstruction + Frame Generation gemeinsam aktiv sind.
**Ursache:** Laut Folgeklärung nicht OptiScaler selbst, sondern fehlerhafte Autopilot-Konfiguration (siehe Punkt 1).
**Version:** v0.8.3, Cyberpunk 2.31, RTX 5070 Ti, Treiber 616.56.
Quelle: https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/52

**Path Tracing + DLSS/RR auf Nicht-Nvidia:** "Noisy image" bei DLSS-Inputs mit Path Tracing auf AMD/Intel-GPUs, da das Spiel versucht, DLSS Ray Reconstruction zu erzwingen. Lösung: FSR- oder XeSS-Inputs statt DLSS, XeSS wird als besser bewertet. (Für Nvidia/RTX 5090 nicht relevant, da DLSS nativ läuft.)
**Version:** OptiScaler 0.9.3 (Wiki, getestet mit RX 9070 XT).
Quelle: https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077

**Ray Regeneration (AMD FSR) über OptiScaler in Cyberpunk (nur informativ, betrifft AMD/RDNA4):** Path Tracing + DLSS Ray Reconstruction in-game aktivieren, dann in OptiScaler auf FSR Ray Regeneration + FSR 4.1.0 umschalten. Nur auf RDNA4 (FSR 4.1 zusätzlich RDNA3 unter Linux). Extrem experimenteller Fork, für Nvidia-Nutzer ohne Bedeutung.
Quelle: https://en.gamegpu.com/news/zhelezo/optiscaler-pomogla-zapustit-ray-reconstruction-ot-amd-v-cyberpunk-2077 , https://wccftech.com/i-tested-amds-ray-regeneration-in-cyberpunk-thanks-to-optiscaler-and-its-surprisingly-good/

---

## 4. Frame Generation / MFG-Konflikte (native DLSS-FG vs. OptiScaler-FG, Streamline)

**Kernproblem:** Wenn OptiScaler die native Frame Generation durch FSR-FG, XeSS-FG, OptiFG oder ein anderes Backend ersetzt, sieht ein Addon/Mod, der den nativen DLSS-G/Streamline-Pfad erwartet (z.B. "DLSS Frame Gen Unlocked"), diesen Pfad nicht mehr.
**Zusatz (Cyberpunk + DLSS 5 NR):** Wenn OptiScaler zusammen mit DLSS 5 Neural Rendering läuft, verstecken die Standard-Spoofing- und Auto-FG-Flags die native DLSS-G-Option und zwingen das In-Game-Menü in den AMD-FSR-3.1-Fallback-Modus.
**Empfehlung (offizielles Wiki, Cyberpunk-spezifisch):** Priorität DLSSG via SL → XeFG/FSR-FG (oder Nukems) → OptiFG/Upscaler als letzter Ausweg. DLSS-FG bleibt in den Spieleinstellungen ausgegraut, bis "DLSSG via SL" oder Nukems gewählt wird.
**Workflow-Hinweis:** FG-Optionen im Overlay wählen → INI speichern → Spiel neu starten (Einstellungen wirken nicht live).
Quellen: https://github.com/optiscaler/OptiScaler/wiki/Frame-Generation-Options , https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077

**Symptom:** Flimmern (Flickering) bei Nutzung von DLSS5 mit OptiScaler + entsperrtem RTX-MFG; Proxy-Kollisionen beim Laden von RTX40MFG.
**Ursache/Lösung:**
- RTX40MFG.asi + RTX40MFGCore.dll direkt in bin\x64\plugins\ platzieren statt Proxy-DLLs, da Cyberpunk lokale d3d12.dll-Proxys in bin\x64 ignoriert.
- Gegen Flimmern: NVIDIA Control Panel "Prefer maximum performance" aktivieren, DLSS Frame Generation auf "Preset B" setzen, Injektionsmethode auf Hook/CET-Modus umschalten.
- In OptiScaler.ini: FrameGen (Enabled=false), Spoofing-Module und DXGI-Masking deaktivieren, um native DLSS-Optionen freizugeben.
- Alternative: NVIDIA App / Profile Inspector — DLSS-Frame-Generation-Override auf "Preset B" setzen, um temporale Frame-Extrapolation bei Multi-Pass-Rekonstruktion zu stabilisieren. (Deckt sich mit dem beim Nutzer bereits gesetzten Treiberprofil-Override — dessen fehlende Wirkung könnte an genau dieser FrameGen/Spoofing-Konfiguration in der OptiScaler.ini liegen.)
Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137

**Proxy/Loading-Fehler:** "Spiel lehnte OptiScalers Swapchain ab" — Cyberpunk verweigert die Swapchain-Übernahme je nach Setup/Loader-Name.
**Workaround:** Anderen Namen in "loads as" versuchen oder die "Feeder"-Route wechseln.
**Version:** DLSS5-Autopilot 1.8.1, RTX 4070 Laptop, Treiber 616.56, DX12.
Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/166

**Dateiumbenennungs-Workaround (mehrfach berichtet):** nvngx_dlss.dll → nvngx.dll umbenennen in bin\x64, dann DLSS mit Frame Generation im Spiel aktivieren, um native DLSS-FG-Funktionalität sicherzustellen. Zusammenhang: nvngx_dlssnr.dll verweigert laut Recherche Aufrufe von Modulen, deren Pfad nicht "nvngx.dll" enthält.
Quelle: WebSearch-Zusammenfassung (mehrere Treffer, u.a. Kizzuwatnaa/DLSS5-Autopilot-Issues); keine Einzel-URL mit Volltext verifiziert — als OFFEN/UNSICHER markiert.

---

## 5. HDR-Probleme (Weißpunkt, ausgebrannte Lichter)

**Kein OptiScaler-spezifischer HDR-Bug gefunden.** Die Recherche liefert nur allgemeine Cyberpunk-HDR-Kalibrierungshinweise (Paper-White/Peak-Luminance-Fehlkalibrierung führt zu Clipping/Washout), unabhängig von OptiScaler:
- Washed-out-Bild = i.d.R. Paper-White/Peak-Brightness-Fehlkalibrierung.
- Ausgebrannte Lichter = Peak-Luminance höher eingestellt als das Display physisch darstellen kann → Clipping; Peak-Luminance muss dem tatsächlichen 10%-Fenster-Peak des Displays entsprechen.
Quelle: WebSearch-Zusammenfassung (Steam-Community-Diskussionen, allgemeine Guides), nicht OptiScaler-spezifisch.

**OptiScaler-spezifischer Bezug (indirekt):** Die Exposure-Scan-Limitierung bei Cyberpunk (siehe Punkt 2) zwingt zu manueller Paper-White-Einstellung im OptiScaler-Overlay statt automatischer Erkennung — das kann bei falscher manueller Einstellung zu genau den o.g. Washout-/Clipping-Symptomen führen.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

---

## 6. Flimmern / Ghosting

**Symptom:** DLSS-Inputs flackern auf dem Balanced-Preset bei Nutzung von FSR4 (Wiki-Hinweis, generisch, nicht Cyberpunk-exklusiv, aber im Cyberpunk-Wiki-Kontext gelistet).
**Lösung:** "Non-Linear sRGB Input" aktivieren — kann aber Ghosting erhöhen. Bei flackernden Cutscenes: Non-Linear/sRGB-Option probieren.
**Version:** OptiScaler-Wiki, aktueller Stand (FSR4-Compatibility-List).
Quelle: https://github.com/optiscaler/OptiScaler/wiki/FSR4-Compatibility-List

**Symptom:** Replace-Proxy-Modus (reversibler "Replace"-Modus) flackert bei hellen bewegten Lichtern.
**Ursache:** Design-bedingt, kein Bug.
**Lösung:** "Composed"-Modi statt "Replace" verwenden (siehe ReversibleMode 0-4 in [DlssNr]-Sektion der INI, empfohlen Modus 3 = "Hybrid proxy + composed").
**Version:** v0.2.0.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr , https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

**Ghosting allgemein:** Deaktivieren von Motion Blur kann Ghosting reduzieren (generischer Hinweis, nicht OptiScaler-spezifisch).
Quelle: WebSearch-Zusammenfassung.

---

## 7. Overlay (Einfg) öffnet nicht / HUD-Probleme

**Overlay-Bedienung:** Einfg (Insert) öffnet das OptiScaler-Overlay. Falls das nicht funktioniert: Tastaturnavigation über Pfeiltasten, Tab und Leertaste; teils muss erst Esc gedrückt werden (Spiel pausieren), damit die Maus im Overlay entsperrt wird.
**Weitere Ursachen laut Wiki "Known Issues":** Falscher Installationsort, Tastenkombinations-Konflikte, RTSS-Konflikte (RivaTuner Statistics Server) können das Öffnen des Overlays verhindern.
Quelle: https://github.com/optiscaler/OptiScaler/wiki/Known-Issues

**EGS/EOS-Overlay-Absturz:** Epic-Games-/EOS-Overlay wurde als Ursache für Abstürze bei aktivierter Frame Generation über OptiScaler identifiziert.
**Lösung:** EOS/EGS-Overlay deaktivieren.
Quelle: https://github.com/optiscaler/OptiScaler/wiki/Known-Issues (via WebSearch-Zusammenfassung)

**Allgemeine Absturz-Ursache bei Overlays/FPS-Tools:** Andere Overlays oder FPS-Monitoring-Software (z.B. RTSS, Discord-Overlay) können Startabstürze verursachen. Ab OptiScaler "0.9-final" blockiert das Tool automatisch bekannte problematische Overlay-Apps vom Hooking.
Quelle: WebSearch-Zusammenfassung, Kontext optiscaler/OptiScaler Known-Issues-Wiki (nicht per WebFetch einzeln verifiziert für diese konkrete Formulierung — siehe OFFEN/UNSICHER).

**HUD-Probleme spezifisch durch OptiScaler:** Keine direkten Treffer gefunden. Nur allgemeine Cyberpunk-HUD-Bugs (Kiroshi-Scanner öffnen/schließen oder Ein-/Aussteigen aus Fahrzeug hilft bei verschwundenem HUD), nicht OptiScaler-bezogen.
Quelle: WebSearch-Zusammenfassung, allgemeine Cyberpunk-Hinweise, kein OptiScaler-Bezug.

---

## 8. Konflikte mit anderen Mods (ReShade, Ultra+, CET, RED4ext)

**ReShade / RedScript (Issue #18, Dagherbou-Fork):**
**Symptom:** RedScript-Mods (und davon abhängige Mods wie ArchiveXL, Virtual Car Dealer, TweakXL, Codeware) kompilieren nicht mehr, sobald OptiScaler.dll als dxgi.dll installiert ist.
**Workaround:** ReShade 6.6.2 dxgi.dll zu d3d12.dll umbenennen erhält die RedScript-Funktion — sobald OptiScaler zusätzlich als dxgi.dll installiert wird, bricht es dennoch.
**Ursache:** Unklar, ob OptiScaler selbst oder die Interaktion mit ReShade 6.6.2 die Ursache ist (im Issue offen gelassen).
**Version:** OptiScaler_DLSSNR v0.2.0, Cyberpunk 2.3.1, ReShade 6.6.2, RedScript 0.5.3.1.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/18

**REDScript-Compilation-Fehler mit Modpacks (offizielles OptiScaler, nicht Fork):**
**Symptom:** "Redscript compilation failed" beim Start, wenn OptiScaler zusammen mit umfangreichen Modpacks (Beispiel "Night City Reborn 2.21") aktiv ist.
**Lösung:** OptiScaler als OptiScaler.asi nach bin\x64\plugins\ verschieben, per ASI Loader (Option 7) laden, inkl. amd_fidelityfx_dx12.dll, amdxcffx64.dll, libxess.dll. Nachteil laut Bericht: ca. 10 FPS Leistungsverlust.
**Version:** Cyberpunk 2077 3.0.78.57301, RED4ext 1.27.0.
Quelle: https://github.com/optiscaler/OptiScaler/discussions/353

**CET + RED4Ext + Ultimate ASI Loader (Issue #313):**
**Symptom:** "Cyberpunk has flatlined" beim gleichzeitigen Einsatz der OptiScaler-Nightly (15.03.2025) mit CET, Ultimate ASI Loader und RED4Ext. Alternative Konfiguration (DLSS Enabler entfernt, OptiScaler als dxgi.dll, ASI Loader als version.dll) lädt zwar CET und OptiScaler erfolgreich, bricht aber RED4ext, das einen eigenen Wrapper über winmm.dll nutzt.
**Lösung:** Siehe oben — OptiScaler als .asi in bin\x64\plugins\, funktionierte laut Bericht nur mit älteren Nightly-Builds; zum Berichtszeitpunkt unterstützte die aktuelle Nightly diesen Weg nicht mehr.
Quelle: https://github.com/optiscaler/OptiScaler/issues/313

**Ultra+ (Ultra Plus – Physically Accurate Path Tracing):**
**Symptom:** Kompatibilitätsprobleme ab Version 9.3.3 bei Nutzern mit nur Core-Mods + Ultra Plus; deaktivieren von Ultra Plus behob Probleme mit spiegelnden Fenstern/Spiegeln.
Quelle: WebSearch-Zusammenfassung, Kontext nexusmods.com/cyberpunk2077/mods/10490 (Kommentarbereich, nicht einzeln per WebFetch verifiziert — siehe OFFEN/UNSICHER).

---

## 9. Probleme durch NVIDIA-Treiber 616.6x/616.9x

**Treiber 616.92 (exakt die Version des Nutzers):** Laut einem Ergebnisbericht ("worked") funktionierte Cyberpunk 2077 mit OptiScaler-Route und RTX 4090 auf Treiber 616.92 grundsätzlich.
Quelle: WebSearch-Zusammenfassung, Kontext Kizzuwatnaa/DLSS5-Autopilot Issue #305 ("result: Cyberpunk 2077 - worked") — nicht per WebFetch einzeln verifiziert, daher unter OFFEN/UNSICHER als Einzelbeleg zu werten, aber Ergebnis war durchgängig positiv in den gesichteten Snippets.

**Treiber 616.56:** Mindestanforderung für OptiScaler_DLSSNR laut Release Notes v0.2.0 ("minimum driver version: 616.56"); mehrere der oben zitierten Crash-Reports (Issues #196, #52, #166) liefen bereits auf 616.56 — die Abstürze wurden dort jedoch nicht dem Treiber, sondern der Autopilot-Konfiguration zugeschrieben.
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

**Allgemeiner GPU-Auslastungs-Hinweis (nicht klar OptiScaler-spezifisch):** Es gibt Berichte, dass mit neueren Treibern die GPU-Auslastung in Cyberpunk 2077 nicht über 90% bleibt, was zu Stottern führt. Kein direkter Bezug zu OptiScaler in der Quelle gefunden — als allgemeiner Treiber-Kontext aufgeführt.
Quelle: WebSearch-Zusammenfassung, nicht einzeln verifiziert — OFFEN/UNSICHER.

**Kein spezifischer, verifizierter Bug-Report zu 616.9x + OptiScaler_DLSSNR + RTX 5090 gefunden.** Es gibt keine Belege dafür, dass Treiber 616.92 spezifisch mit OptiScaler-DLSSNR v0.2.0 inkompatibel ist.

---

## 10. Leistung (Performance)

Kein dediziertes Performance-Issue speziell für OptiScaler-DLSSNR + Cyberpunk 2.3x mit belastbaren Zahlen gefunden, abgesehen von:
- ~10 FPS Verlust durch den .asi-Workaround gegen RedScript-Konflikte (siehe Punkt 8, Discussion #353).
- DLSS-NR rebuilds das Modell bei jedem Dispatch in einem anderen Spiel (MSFS 2024, Issue #29) — kein Cyberpunk-Bezug, nur als Hinweis auf ein generisches Performance-Muster des Forks erwähnt (nicht als Cyberpunk-Bug bestätigt).
Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues (Issue-Liste, Titel #29)

---

# BEST-PRACTICES-KANDIDATEN

1. **FG-Priorität für Cyberpunk laut OptiScaler-Wiki:** DLSSG via Streamline (SL) zuerst aktivieren (in-game DLSS-FG anschalten, dann im Overlay "(FG) Active"), erst danach XeFG/FSR-FG oder Nukems als Alternative, OptiFG/Upscaler nur als letzter Ausweg. INI-Änderungen erst nach "Save INI" + Neustart wirksam.
   Version: OptiScaler (Wiki-Stand, zuletzt getestet 0.9.3). Quelle: https://github.com/optiscaler/OptiScaler/wiki/Frame-Generation-Options , https://github.com/optiscaler/OptiScaler/wiki/Cyberpunk-2077

2. **ReversibleMode=3 ("Hybrid proxy + composed")** in der [DlssNr]-Sektion der OptiScaler.ini als empfohlene Balance zwischen Qualität und dem bekannten Flacker-Problem des "Replace"-Modus bei hellen bewegten Lichtern.
   Version: OptiScaler_DLSSNR v0.2.0. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

3. **Bei Exposure-Problemen (Washout/zu dunkel) in Cyberpunk:** manuellen Paper-White-Schieber im Overlay nutzen statt sich auf den automatischen Exposure-Scan zu verlassen, da Cyberpunk die Belichtung in einer für den Scan unsichtbaren Form berechnet.
   Version: v0.2.0. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

4. **Bei RedScript/CET/RED4ext-Konflikten:** OptiScaler als OptiScaler.asi in bin\x64\plugins\ (statt dxgi.dll) über Ultimate ASI Loader laden, inkl. amd_fidelityfx_dx12.dll, amdxcffx64.dll, libxess.dll — reduziert Konflikte mit Mod-Loadern, kostet aber laut Anwenderbericht ca. 10 FPS. Funktionierte nicht zuverlässig mit allen Nightly-Ständen.
   Version: Cyberpunk 3.0.78.57301 / RED4ext 1.27.0 (Discussion #353); Nightly 15.03.2025 (Issue #313). Quellen: https://github.com/optiscaler/OptiScaler/discussions/353 , https://github.com/optiscaler/OptiScaler/issues/313

5. **Mindest-Treiberversion 616.56** für OptiScaler_DLSSNR wird vom Projekt selbst genannt — beim Nutzer mit 616.92 erfüllt.
   Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

---

# BUG-KANDIDATEN

1. **OptiScaler (offiziell) v0.9.3+ crasht Cyberpunk 2.31 sofort, v0.9.2a nicht** — offenes, ungeklärtes Issue.
   Version: OptiScaler v0.9.3+, Cyberpunk 2.31, RTX 3080 Ti. Quelle: https://github.com/optiscaler/OptiScaler/issues/1063

2. **OptiScaler_DLSSNR (Dagherbou-Fork) bricht RedScript-Kompilierung**, sobald als dxgi.dll installiert — auch mit umbenanntem ReShade-dxgi.dll. Ursache ungeklärt (OptiScaler vs. ReShade 6.6.2).
   Version: v0.2.0, Cyberpunk 2.3.1, ReShade 6.6.2, RedScript 0.5.3.1. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/18

3. **Ray Reconstruction wird durch OptiScaler_DLSSNR (Fork) ausgegraut**, während die offizielle OptiScaler-Version RR normal anbietet. Ungeklärt, ohne dokumentierte Lösung.
   Version: OptiScaler_DLSSNR (Version im Issue nicht spezifiziert). Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/issues/8

4. **Exposure-scan use-after-free** bei Ray-Reconstruction + Streamline-Titeln (Cyberpunk 2077 namentlich genannt) — konnte das Gerät entfernen ("device removed"). **Bereits gefixt in v0.2.0**, also für den Nutzer nicht mehr aktiv, aber relevant als Hintergrund für evtl. ältere Installationsreste.
   Version: gefixt in v0.2.0. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

5. **vkd3d-proton (Linux) NR "build epoch 0"-Stall in v0.7.5**, v0.2.0 funktioniert. Nur relevant falls der Nutzer je auf Linux/Proton wechselt — auf Windows/Steam (Nutzer-Setup) nicht anwendbar, aber dokumentiert einen Regressionspfad zwischen v0.2.0 und späteren Versionen des Forks.
   Version: v0.7.5 defekt, v0.2.0 funktioniert; Cyberpunk 2.31, RTX 5090, Treiber 610.57.04 (Linux). Quelle: https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/issues/24

6. **OptiScaler + DLSS-5-NR versteckt native DLSS-G-Option** durch Standard-Spoofing/Auto-FG-Flags, zwingt Spielemenü in FSR-3.1-Fallback — erklärt möglicherweise, warum der NVIDIA-Treiberprofil-Override beim Nutzer "evtl. nicht wirkt": OptiScaler überschreibt/verdeckt die Auswahl auf Spiel-Ebene, bevor der Treiber-Override greifen kann.
   Quelle: https://github.com/Kizzuwatnaa/DLSS5-Autopilot/issues/137 (WebSearch-Zusammenfassung)

---

# OFFEN/UNSICHER

- **Nexus-Mod-Seite "DLSS 5 Neural Rendering Installation Guide" (nexusmods.com/cyberpunk2077/mods/33380)** war per WebFetch nicht erreichbar (HTTP 403). Alle daraus zitierten Aussagen (0xBAD00001-Fehler auf Nicht-Blackwell-GPUs, RenoDX-Discord-Patch) stammen nur aus WebSearch-Snippets, nicht aus dem verifizierten Volltext — sollten bei Bedarf über einen Browser direkt gegengeprüft werden.
- **Behauptung "ab OptiScaler 0.9-final blockiert automatisch bekannte problematische Overlays"** — stammt aus einer WebSearch-Zusammenfassung der Known-Issues-Wiki-Seite, wurde aber beim direkten WebFetch dieser Seite NICHT bestätigt (die Fetch-Zusammenfassung dort nannte nur allgemeine Overlay-Probleme, EGS/EOS-Crash und XeSS-Probleme, keine Versionsnummer "0.9-final"). Als unsicher zu behandeln.
- **Treiber 616.92 + Cyberpunk + OptiScaler "worked" (Issue #305)** wurde nicht per WebFetch einzeln geöffnet, nur aus Suchtreffer-Titel/Snippet übernommen. Keine Fehlerdetails, falls doch Probleme vorlägen.
- **Ultra-Plus-Kompatibilitätsproblem ab 9.3.3** stammt aus einem WebSearch-Snippet zu den Nexus-Kommentaren, nicht aus verifiziertem Volltext.
- **Kein direkter, mit Datum/Version belegter Treffer für "Treiber 616.6x/616.9x verursacht OptiScaler-Absturz in Cyberpunk"** gefunden. Es gibt keine Hinweise auf eine bekannte Inkompatibilität speziell zwischen Treiber 616.92 und OptiScaler-DLSSNR v0.2.0 bei RTX 5090 — das exakte Setup des Nutzers (v0.2.0 + nvngx_dlssnr 310.8.0 + Treiber 616.92 + RTX 5090) taucht in keiner gefundenen Quelle als problematisch auf.
- **Warum der NVIDIA-Treiberprofil-Override "evtl. nicht wirkt"** wurde nicht mit einer expliziten, auf das Override-Feature bezogenen Quelle belegt — Punkt 4 in den Bug-Kandidaten ist eine plausible Ableitung aus Issue #137, aber keine bestätigte 1:1-Erklärung für das Override-Verhalten.
- Die Dagherbou-Fork-Issue-Liste (#54, #52, #50, #47, #45, #43, #41, #38, #37, #35, #30, #29) enthält aktuell **keine einzige Cyberpunk-2077-spezifische Meldung** unter den offenen Issues — das deutet darauf hin, dass v0.2.0 in Cyberpunk vergleichsweise stabil läuft, ist aber kein Beweis für Bugfreiheit (ältere/geschlossene Issues wie #18, #8, #17 betreffen Cyberpunk und wurden oben ausgewertet).
