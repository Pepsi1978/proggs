# Web-Researcher 1/5 — Unterthema: Beste DLSSNR-Reglerwerte für Cyberpunk 2077 (OptiScaler-DLSSNR v0.2.0)

Stand: 21.09.2026. Recherchiert über WebSearch/WebFetch. KEIN Download/keine Installation vorgenommen.

## Wichtiger Vorbefund zur Quellenlage

Es gibt **keine einzige gefundene Quelle**, die eine vollständige, spielspezifische Cyberpunk-2077-Tabelle mit konkreten Zahlenwerten für alle angefragten Regler (TransferStrength, ColourStrength, Intensity, LocalTone, LocalStructure, SkinStructure, AutoMask, MaxRatio, WorkingScale) veröffentlicht. Weder das Repo-README noch Config.md, noch die Release-Notes v0.1.0/v0.1.1/v0.1.1.5/v0.2.0, noch die Nexus-Modseite (Zugriff mit HTTP 403 verweigert, auch im zweiten Versuch), noch r/nvidia, r/cyberpunkgame oder Guru3D lieferten eine dedizierte Cyberpunk-Tabelle. Die belastbarsten Aussagen stammen aus der offiziellen GitHub-Discussion #17 zum v0.2.0-Release (dort postet der Autor Dagherbou selbst und Nutzer berichten explizit zu Cyberpunk 2077) sowie aus verwandten Forks/Docs. Die Tabelle unten ist daher aus mehreren Teilaussagen zusammengesetzt, nicht aus einer einzigen "Cyberpunk-Preset-Empfehlung". Das wird bei OFFEN/UNSICHER unten nochmal klar benannt.

Quelle für obiges: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 · https://www.nexusmods.com/cyberpunk2077/mods/33380 (403) · https://github.com/Dagherbou/OptiScaler_DLSSNR/blob/dlss-neural-rendering/Config.md · https://github.com/Dagherbou/OptiScaler_DLSSNR/releases

## Befunde mit Quelle

1. **Discussion #17 (offizieller Release-Thread v0.2.0, 03.–10.09.2026)** ist die zentrale Community-Anlaufstelle für DLSSNR-Feedback; Dagherbou selbst antwortet dort aktiv.
   https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

2. **Cyberpunk 2077 wird explizit als einer der Titel mit Flimmer-/Shimmer-Problemen bei niedriger Model Resolution (WorkingScale) genannt.** Nutzer jcuizd (04.09.2026): Model Resolution unter 100 % (besonders um 70 %) verstärkt Flimmern beim Kameraschwenk — namentlich in Cyberpunk 2077, RE9 Requiem und The Last of Us Part 2.
   https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

3. **Konkrete Gegenmaßnahme von Nutzer Tsarri (04.09.2026)** für genau dieses Flimmer-Problem bei 70–75 % Skalierung: "DLSS 4.5 Preset L Balanced oder höher, Detail Strength (TransferStrength) um ca. 15 % unter Default reduzieren, Reversible Proxy nutzen, Highlight Guard (MaxRatio) leicht senken, Intensity unter 1,5 halten." — Umgerechnet auf die Regler des Nutzers: TransferStrength ≈ 0,85 statt Default 1,0; MaxRatio knapp unter Default 2,0 (z. B. 1,7–1,9); Intensity < 1,5.
   https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

4. **ColourStrength-Werte über 1,0 sind laut Autor Dagherbou explizit vorgesehen ("supports values well above 1×"), können aber bei hohen Werten zu Flimmern führen.** Aus einer weiteren Recherche-Passage: ColourStrength=0 hält den originalen Spiele-Hue (deaktiviert Farbverschiebung), Werte über 1× erhöhen die Sättigung über das Modell-Eigenfarbschema hinaus, ohne in flache Clipping-Peaks zu laufen — der Regler "rollt am Gamut-Rand ab statt auszubrennen". Für einen realistischen, artefaktarmen Look empfiehlt sich daher ColourStrength nahe Default 1,0, nicht deutlich darüber, gerade wegen Cyberpunks Neon-Beleuchtung.
   https://github.com/Dagherbou/OptiScaler_DLSSNR (README/Discussion, Herstellerangabe) — Suchbeleg über GitHub-Repo-Inhalte, indirekt über WebSearch-Snippet bestätigt.

5. **Farbstich-/"washed-out"-Problem dokumentiert in einem verwandten Fork-Issue** (Neural-coprocessor, betrifft u. a. Cyberpunk 2077 und Battlefield 6, HDR/Farb-Pipeline): Zitat aus dem Build-Log: "Washed or flat colour? Take tone down – 0.00 fixed it on the dev rig", mit ausdrücklicher Einschränkung, dass dieser Fix nicht generell gilt (rig-abhängig). Bezug ist LocalTone-artiger Regler ("LocalToneStrength"/Panel-Ton-Control).
   https://github.com/maohgad-web/Neural-coprocessor/issues/20

6. **AutoMask/SkinStructure-Verhalten (allgemein, nicht Cyberpunk-exklusiv, aber übertragbar):** AutoMask ist NVIDIAs interne automatische Maskeneingabe für die Haut-/Umgebungstrennung; SkinStructure = -1 folgt standardmäßig LocalStructure. Empfohlener Test zur Beurteilung von Gesichtsartefakten: AutoMask=true, LocalStructure=1, SkinStructure=0 an einer stehenden Nahaufnahme eines Charakters vergleichen (LocalStructure wirkt auf Umgebung/Textur, SkinStructure separat auf Haut — niedrigerer SkinStructure-Wert reduziert das Risiko von überzeichneten/"wächsernen" Gesichtern).
   https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/docs/NR-COMPATIBILITY.md (Skin-Kontrollen-Kapitel) · Websuche bestätigt gleichlautend.

7. **Preset vs. Style — Bedeutung laut Autor/Doku:** "Style" ist der primäre Profil-Selektor der Neural-Rendering-Pipeline (Optionen u. a. Default/Standard, Natural, Cinematic — genaue visuelle Wirkung der einzelnen Style-Stufen wird in keiner gefundenen Quelle im Detail beschrieben). "Preset"-Hinweise sind in der UI unter einem eingeklappten Advanced-Bereich gruppiert, weil ihre visuelle Wirkung laut Autor selbst "unverified" (nicht verifiziert) ist — Preset gilt also als experimentell/nachrangig gegenüber Style.
   Websuche (GitHub-Repo-Snippets zu OptiScaler_DLSSNR / PreSR-Multipass-Fork), Discussion #17.

8. **Multi-Point Exposure Anchoring (neu in v0.2.0)** wird vom Autor als Lösung für instabile Belichtung/Ton in Spielen mit wechselnden Lichtverhältnissen empfohlen (relevant für Cyberpunks Tag/Nacht- und Neon-Wechsel), da der reine "Exposure Scan" bei manchen Spielen (u. a. laut Recherchehinweis auch Cyberpunk) limitiert funktioniert.
   https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

9. **WorkingScale/Model-Resolution und Leistungskosten (allgemein, kein Cyberpunk-spezifischer Benchmarkwert gefunden):** v0.2.0 unterstützt jetzt Supersampling bis 2× (WorkingScale > 1,0 = Modell rendert intern höher aufgelöst als Zielauflösung → deutlich teurer, "Supersampling"), während WorkingScale < 1,0 (z. B. 0,5–0,7) Leistung spart, aber laut Punkt 2/3 das Flimmerrisiko erhöht (u. a. konkret in Cyberpunk 2077 berichtet). RTX-20/30-Karten benötigen laut PreSR-Multipass-Fork einen "much heavier FP16 path", weshalb dort reduzierte Model-Resolution nötiger ist als auf RTX 40/50 — für die RTX 5090 des Nutzers ist folglich mehr Spielraum zu WorkingScale ≥ 1,0 vorhanden, ohne die FP16-Bremse anderer Baureihen. Konkrete FPS-Zahlen für Cyberpunk bei bestimmten WorkingScale-Stufen wurden in keiner Quelle gefunden (siehe OFFEN/UNSICHER).
   https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/INSTALL-DLSSNR.md · https://github.com/Dagherbou/OptiScaler_DLSSNR/releases/tag/v0.2.0-dlssnr

10. **Gesichts-/Detailverbesserung allgemein positiv bewertet, aber ohne Reglerwerte:** Ein Hands-on-Artikel hebt hervor, dass Cyberpunk 2077 "arguably showed the most dramatic jump" durch Neural Rendering zeigte — insbesondere Gesichtsdetail bei NPCs, Hand-/Hauttextur der Spielfigur und Umgebungslicht von Leuchtreklamen/Straßenlaternen. Separate Regler für Skin-Structure-Stärke und Local-Structure-Stärke werden als Kontrollmechanismus genannt, aber ohne empfohlene Zahlenwerte.
    https://www.mindstudio.ai/blog/dlss-5-neural-rendering-hands-on

11. **Reversible Proxy / ReversibleMode=3 ("Hybrid proxy + composed") als vom Autor empfohlener Default-Kompromiss** gegen Licht-Flimmern/fehlende Lichter, die bei reinen Replace-Proxy-Modi auftreten (allgemein, auch bei Cyberpunks Neon-Lichtern relevant, da diese Szenen laut Dagherbous Originalpost bekannt für "light flicker or missing lights" bei bestimmten Proxy-Modi sind).
    https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17

12. **Kein Cyberpunk-spezifischer Beleg für "Wachsgesichter durch zu hohe SkinStructure/Intensity"** gefunden — dieses in der Anfrage genannte Fehlerbild ist aus dem allgemeinen DLSS-Ray-Reconstruction-Kontext bekannt (Steam-Diskussionen berichten "oily"/"plasticine" Gesichter bzw. Ghosting bei RR), aber nicht als DLSSNR/OptiScaler-Cyberpunk-Zitat mit konkretem Reglerwert belegbar.
    (kein direkter Beleg — Negativbefund; verwandte, nicht identische RR-Aussage als Kontext)

## Empfehlungstabelle (aus den obigen Teilbefunden abgeleitet — KEINE offizielle Einzelquelle, siehe OFFEN/UNSICHER)

| Regler | Empfohlener Wert (4K, RTX 5090) | Begründung | Quelle |
|---|---|---|---|
| TransferStrength | ~0,85 (Default 1,0 leicht senken, ca. −15 %) | Reduziert Detail-Übertreibung/Flimmerrisiko bei bewegter Kamera; von Tsarri explizit für Skalierungsprobleme empfohlen | Discussion #17 (Tsarri, 04.09.2026) |
| ColourStrength | 1,0 (Default), nicht deutlich darüber | Werte >1× erhöhen Sättigung ungebremst bis zum Gamut-Rand — bei Cyberpunks Neon-Szenen Risiko für Farbstich/Überzeichnung; 0 = neutral (Original-Hue) als Rückfalloption bei Farbproblemen | OptiScaler_DLSSNR-Repo (README/Discussion-Snippets) |
| Intensity | < 1,5 | Explizite Empfehlung gegen Flimmern/Überzeichnung bei niedrigerer WorkingScale | Discussion #17 (Tsarri) |
| LocalTone | niedrig ansetzen, bei "washed/flat" Farben in Richtung 0 testen | Aus Farb-Pipeline-Issue: LocalTone-artiger Regler auf 0 behob Ausbleichen auf einem Testsystem (nicht allgemeingültig) | Neural-coprocessor Issue #20 |
| LocalStructure | 1,0 (Default) als Ausgangspunkt, mit AutoMask=true testen | Wirkt auf Umgebungs-/Objektdetail; Referenz-Testverfahren aus PreSR-Multipass-Doku | NR-COMPATIBILITY.md (wilsjo2-Fork) |
| SkinStructure | niedriger als LocalStructure ansetzen, z. B. 0 statt -1 (=folgt LocalStructure) | Getrennte Haut-Kontrolle verhindert Übertragung von Umgebungs-Detailschärfe auf Gesichter (Wachsgesicht-Risiko) | NR-COMPATIBILITY.md (wilsjo2-Fork) |
| AutoMask | true | Nutzt NVIDIAs interne Maske zur Trennung Haut/Umgebung statt manueller Maskierung | NR-COMPATIBILITY.md (wilsjo2-Fork) |
| MaxRatio (Highlight Guard) | knapp unter Default 2,0 (z. B. 1,7–1,9) | Reduziert Neon-/Licht-Überstrahlung laut Tsarris Empfehlung | Discussion #17 (Tsarri) |
| WorkingScale | ≥ 1,0 möglich auf RTX 5090 (kein FP16-Flaschenhals wie RTX 20/30); < 1,0 (z. B. 0,7) spart Leistung, erhöht aber laut Nutzerberichten das Flimmerrisiko gerade in Cyberpunk 2077 bei Kameraschwenks | jcuizd-Bericht + PreSR-Multipass-Doku zu FP16-Pfad älterer Karten | Discussion #17 (jcuizd) · INSTALL-DLSSNR.md |
| Preset | nachrangig behandeln (laut Autor "unverified") | Bewusst hinter Advanced-Klapp­menü versteckt, weil Wirkung nicht verifiziert ist | Websuche (Repo-Snippet) |
| Style | Default/Natural bevorzugen für Realismus, Cinematic vermeiden falls überzeichnet wirkt | Style ist primärer Profil-Selektor; keine Detail-Doku zu Einzelwirkung gefunden, aber Namensgebung deutet auf Natural = neutraler | Websuche (Repo-Snippet) |

## BEST-PRACTICES-KANDIDATEN:
- OptiScaler_DLSSNR v0.2.0, Cyberpunk 2077: Bei WorkingScale < 100 % Flimmern durch TransferStrength ≈ −15 % vom Default, MaxRatio leicht unter Default 2,0, Intensity < 1,5 abmildern. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 (Nutzer Tsarri, 04.09.2026, bezogen auf v0.2.0)
- ReversibleMode=3 ("Hybrid proxy + composed") als Default-Kompromiss gegen Licht-Flimmern bei Neon-/Lichtszenen. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 (Dagherbou, v0.2.0)
- SkinStructure getrennt von LocalStructure niedriger einstellen (statt -1/folgt LocalStructure) zur Vermeidung übertriebener Gesichtsdetails. Quelle: https://github.com/wilsjo2/OptiScaler-DLSSNR-PreSR-Multipass/blob/main/docs/NR-COMPATIBILITY.md

## BUG-KANDIDATEN:
- OptiScaler_DLSSNR v0.2.0: Flimmern bei WorkingScale/Model Resolution < 100 % (besonders ~70 %) bei Kamerabewegung, explizit auch in Cyberpunk 2077 berichtet. Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 (jcuizd, 04.09.2026, v0.2.0)
- OptiScaler_DLSSNR v0.2.0 vs. v0.1.2: bis zu 10 FPS Mehrkosten bei gleicher Neural-Skalierung berichtet (nicht Cyberpunk-spezifisch, aber generell für Performance-Erwartung relevant). Quelle: https://github.com/Dagherbou/OptiScaler_DLSSNR/discussions/17 (kilyan82, 04.09.2026)
- Farb-/HDR-Pipeline: "washed/flat" Farben nach Neural-Pass, u. a. bei Cyberpunk 2077 genannt, Ursache ungeklärt (fehlende Farbraum-Konvertierung zwischen Game-Buffer und Neural-Stage vermutet). Quelle: https://github.com/maohgad-web/Neural-coprocessor/issues/20 (offenes Issue, kein Fix bestätigt)

## OFFEN/UNSICHER:
- Es existiert keine einzige Quelle mit einer vollständigen, autoritativen "Cyberpunk-2077-Preset"-Tabelle für alle 9 angefragten Regler. Die obige Tabelle ist eine Zusammenführung mehrerer Teilaussagen aus einem einzigen zentralen Diskussionsthread (Discussion #17) plus verwandten Docs/Issues — sie ist plausibel, aber nicht als "so empfiehlt es der Autor/die Community geschlossen für Cyberpunk" zu verstehen.
- Die Nexus-Modseite "DLSS 5 Neural Rendering Installation Guide for Cyberpunk 2077" (https://www.nexusmods.com/cyberpunk2077/mods/33380) konnte NICHT gelesen werden (HTTP 403 bei zwei Versuchen) — dort könnten spielspezifische Werte stehen, die hier fehlen. Empfehlung: manuell im Browser prüfen.
- Keine konkreten FPS-/Leistungszahlen für Cyberpunk 2077 bei bestimmten WorkingScale-Stufen (z. B. 0,7 / 1,0 / 1,5 / 2,0) auf RTX 5090 gefunden — nur allgemeine Aussagen zu FP16-Pfad älterer Karten und dem generellen "das kostet jeden Frame extra".
- Reddit r/nvidia, r/cyberpunkgame, r/pcgaming lieferten in dieser Recherche keine direkt auffindbaren Threads speziell zu DLSSNR-Reglerwerten für Cyberpunk — möglicherweise, weil die Diskussion primär im GitHub-Discussion-Thread und/oder Discord (RenoDX-Discord, nicht durchsuchbar) stattfindet.
- Genaue Definition/Werteliste der "Style"-Optionen (Default/Natural/Cinematic) und was "Preset" technisch genau steuert, konnte nicht aus Primärquelle (Config.md) bestätigt werden — Config.md dokumentiert laut Abruf nur Upscaler-/Sharpness-/Barrier-Einstellungen, NICHT die [DlssNr]-Sektion, obwohl das README explizit dorthin verweist. Möglich, dass die [DlssNr]-Doku in einer anderen Datei/einem anderen Wiki-Abschnitt liegt, der nicht gefunden wurde.
- Keine direkte, mit Reglerwert verknüpfte Cyberpunk-Aussage zu "Wachs-/KI-Gesichtern" gefunden (Punkt 12) — nur allgemeine, nicht DLSSNR-spezifische Berichte zu Ray-Reconstruction-Gesichtsartefakten als Kontext.
