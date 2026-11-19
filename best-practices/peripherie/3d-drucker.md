# 3D-Drucker (FDM) für die Wohnung: Auswahl, Materialien, Filter (Stand 19.11.2026)

> Versions-Anker: Marktstand 19.11.2026 (Bambu Lab H2D/H2S/H2C/H3C/X2D/P2S, Prusa CORE One+/L+, QIDI Q2/Plus 5,
> Creality K2 Plus/Pro, Elegoo Centauri Carbon 2, Snapmaker U1). Recherche: Sonnet-5.5-Schwarm (7 Researcher).
> Rohberichte mit allen URLs: `best-practices/peripherie/recherchen/3d-drucker-2026-11/`.
> Gegenstück: `bugs/peripherie/3d-drucker.md`. Markierung: [offiziell] = Hersteller, [extern] = Test/Forum/Studie.

## 1. Anforderung → Pflicht-Merkmale
Für „alle Materialien in der Wohnung“ (PLA, PETG, ASA, ABS, PC, PA/PA-CF, TPU 85A–95A):
- Düse ≥ 300 °C (besser 350 °C) für PC/PA, gehärtete Düse + Extruderzahnräder für CF/GF. [extern] https://printpal.io/wiki/engineering-materials-guide
- **Aktiv beheizte** Kammer (~60–65 °C) für PC/PA/PPA-CF und verzugsfreies ASA/ABS.
- Filter **HEPA (H12) + Aktivkohle**. HEPA fängt Partikel (>98 %), Kohle nur Gase (~50–70 %); „Filter eingebaut“ heißt nicht HEPA. [extern] https://pmc.ncbi.nlm.nih.gov/articles/PMC6889885 , https://bambuhub.net/articles/printer-air-quality
- Direktextruder für weiches TPU; Filamenttrockner (PA/PC/TPU/PET-CF 4–8 h trocknen). [offiziell] Polymaker-TDS

## 2. Materialzuordnung (Laienfassung)
| Material | Hält bis ca. | Draußen/UV | Emission | Einsatz |
|---|---|---|---|---|
| PLA | 50–60 °C | schlecht | niedrigste | Deko, Innen |
| PETG | 70–78 °C | mittel (vergilbt) | mittel (Fang 2023: Partikel teils wie ABS) | „bis ~60 °C, weniger Emission“ |
| ASA | 85–100 °C | sehr gut | hoch (Styrol) | „draußen, UV, bis ~100 °C“ |
| ABS | ~95 °C | schlecht | hoch | Innen, technisch |
| PC / PA-CF / PPA-CF | >100 °C | nur UV-stabilisiert | hoch (PA: Caprolactam) | heiß, belastet |
| TPU 85A | – | gut | mittel | weiche, rutschfeste Gummifüße (stehende Möbel) |
| TPU 95A | – | gut | mittel | härtere Füße/Puffer (oft verschobene Stühle) |
| TPU 64D/68D | – | gut | mittel | „Hartgummi“, einzig AMS-tauglich |
Quellen: https://www.3dxtech.com/blogs/featured/asa-filament-vs-abs-outdoor-uv-parts , https://qidi3d.com/blogs/print-lab/tpu-furniture-feet-stop-sliding , https://www.iaqip.wki.fraunhofer.de/en/all_topics/3d_printer.html
HDT-Werte immer mit Prüflast lesen (0,45 vs. 1,8 MPa); Datenblätter widersprechen sich (PA6 70–111 °C).

## 3. Kandidaten (Stand 19.11.2026)
| Gerät | Düse | Kammer aktiv | Filter | Preis ca. | Amazon.de |
|---|---|---|---|---|---|
| **Bambu Lab H2S** | 350 °C | 65 °C | H12-HEPA + Kohle | 999 € (EU-Store, BF bis 01.12.) | nicht offiziell bestätigt |
| Bambu Lab H2D (2 Düsen) | 350 °C | 65 °C | H12-HEPA + Kohle | ab 1.399 € | nicht offiziell bestätigt |
| **Prusa CORE One+** + Advanced Filtration | 290 °C (HT-Hotend 400 °C +260 €) | 55 °C (L: 60 °C) | HEPA + Kohle (Zubehör 79,99 €) | ~1.250–1.470 € | nicht durch Prusa |
| **QIDI Q2** | 370 °C | 65 °C | H12-HEPA + Kohle | 449–499 € | ja (~475–499 €) |
| QIDI Plus 5 | 370 °C | 65 °C | HEPA + Kohle | 637–749 € | ja (laut Smartzone) |
| Bambu P2S / P1S | 300 °C | nein | **nur Kohle** | 599 € | Drittanbieter/110 V |
| Creality K2 Plus | 350 °C | 60 °C (K2 Pro im Test nur 45–57 °C) | 2 Filter, HEPA-Klasse unklar | ~1.250 € | ja |
| Elegoo Centauri Carbon 2 / Snapmaker U1 | 300 °C | nein | – | 340–700 € | teils |
Quellen: Rohberichte r1, r2, r3, r7; https://www.igorslab.de/creality-k2-pro-combo-3d-drucker-im-test/ , https://preisvergleich.heise.de/prusa-core-one-a3678102.html

## 4. Zuverlässigkeit / Service [extern]
- Am belastbarsten: Prusa CORE One (2 J. EU-Garantie, Ersatzteile, offen, Prag) und Bambu P-Serie (robust, aber nur Kohlefilter).
- Bambu H-Serie: komplexer; Lüfter ~1.000–1.600 h, Spindelmutter ~2.000 h getauscht (H2D-Langzeitbericht). Verschleißteile vorrätig halten. https://stack-archive.com/blog/bambu-lab-h2d-reliability-4000-hours-owner-repairs-2026/
- Bambu: Cloud-/Autorisierungskontroverse; „Developer Mode“/LAN für Unabhängigkeit. https://all3dp.com/4/bambu-labs-controversial-authorization-control-hits-budget-3d-printers/
- QIDI Q2: TechRadar 4,5/5 nach 6 Monaten; QIDI Box meiden. Creality schneidet bei Service/Langzeit am schwächsten ab.

## 5. Kauf
- Bambu im offiziellen EU-Store oder bei autorisierten Händlern kaufen (Garantie nur mit Beleg von dort). https://bambulab.com/de/policies/warranty
- Amazon.de: „Verkauf durch“ prüfen, EU-Modellnummer (…-EU) und 230 V; 110-V-/Importlistings existieren. Amazon-Seiten sind per WebFetch/curl nicht abrufbar (HTTP 503) → Preise per heise/Geizhals.
- Prusa-Garantieverlängerung nur im selben Warenkorb.

## 6. Betrieb in der Wohnung
- Nicht im Schlafzimmer; nach Druckende Tür zu, Nachfilterung laufen lassen, dann lüften. https://help.prusa3d.com/de/article/fortschrittliches-filtersystem-fur-prusa-core-one_887189
- Filter rechtzeitig wechseln (Herstellerangaben 500–1.440 h, widersprüchlich → eher früher).
- Emissionsärmste Wahl bleibt PLA; ASA/ABS/PA/PC nur mit dichtem Gehäuse, HEPA + Kohle, eigenem Raum. Abluft nach draußen ist wirksamer als Umluft (NIOSH). https://stacks.cdc.gov/view/cdc/258855
- Kein Blauer Engel für 3D-Drucker (RAL-UZ 205 = Bürodrucker); Filter-Wirkangaben sind Herstellerangaben, keine Norm.
- Weiches TPU nie durch AMS/QIDI Box/CFS, sondern von externer Spule direkt in den Extruder; 15–25 mm/s, Retraktion minimal. https://wiki.bambulab.com/en/h2/h2d-tpu-printing-guide

## 6a. Marken-Einordnung (19.11.2026)
- Klassische Haushaltsmarken (Siemens, Bosch, Philips) bauen keine Consumer-3D-Drucker. Dremel (Bosch-Tochter) hat seine DigiLab-Drucker an 3PI Tech Solutions lizenziert, der 3D45 ist bei Händlern seit 2023 ausgelaufen. https://www.dremel.com/us/en/digilab , https://www.cdw.ca/product/dremel-digilab-3d45-3d-printer/5451670
- Bambu Lab (Shenzhen, gegründet 2020, laut Bericht von Ex-DJI-Ingenieuren) ist Weltmarktführer bei Druckern unter 2.500 US$: 2025 laut CONTEXT 37 % Anteil (2024: 29 %, 1,2 Mio. Geräte). Über 90 % der Einstiegsgeräte kommen aus China. https://www.tomshardware.com/3d-printing/bambu-lab-overtakes-creality-as-the-worlds-top-selling-budget-3d-printer-brand , https://www.contextworld.com/quarterly-industrial-3d-printer-shipments-rebound-amid-continued-record-breaking-entry-level-growth
- US-Marken: Stratasys (Erfinder von FDM) und 3D Systems = Industrie; Formlabs = Harzdruck (SLA), kein FDM; MakerBot ist in UltiMaker (NL) aufgegangen, Profi-Preisklasse. Stratasys gewann 09/2026 ein Patenturteil in Texas gegen Bambu (27,6 Mio. US$, Berufung angekündigt). https://technode.com/2026/10/30/how-bambu-lab-turned-3d-printing-into-a-consumer-hit/

## 6b. Steuerung per Jarvis/CLI (Prusa CORE One, geprüft 19.11.2026)
- Verbindung: WLAN (nur 2,4 GHz) + Ethernet (RJ45), kein Bluetooth. https://help.prusa3d.com/article/network-connection-core-one-l-core-one-mk4s-mk3-9s_736892
- PrusaLink läuft lokal auf dem Drucker (nur LAN, kein Cloud-Zwang); offizielle OpenAPI-Spezifikation: https://github.com/prusa3d/Prusa-Link-Web/blob/master/spec/openapi.yaml
  Endpunkte u. a. `/api/v1/status`, `/api/v1/job` (+ pause/resume), `/api/v1/files/{storage}/{path}` (Upload per PUT), `/api/v1/cameras/snap`, `/api/v1/transfer`.
- Slicen per CLI: PrusaSlicer-Kommandozeile (`prusa-slicer --export-gcode …`), danach Upload per PrusaLink → Druck startet.
- Fernzugriff von unterwegs: über den eigenen WireGuard-Tunnel ins Heimnetz statt Prusa Connect (Cloud optional).
- Gegenbeispiel Bambu: Cloud-/Autorisierungszwang, lokale Steuerung nur im LAN-/Developer-Modus.

## 7. Empfehlung (für Frank, 19.11.2026)
1. Bambu Lab H2S (+ AMS 2 Pro/HT) – beste Gesamtpaket aus 350 °C, 65 °C Kammer, HEPA + Kohle, Lidar, großer Bauraum, TPU 85A direkt. Kauf im EU-Store.
2. QIDI Q2 – günstigster Drucker mit allen harten Kriterien, bei Amazon.de; Schwäche weiches TPU < 95A.
3. Prusa CORE One+ mit Advanced Filtration – zuverlässigster Service, Europa; nicht bei Amazon, teurer.
