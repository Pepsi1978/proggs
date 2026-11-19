# R4 Materialkunde FDM (Recherche vom 19.11.2026)

Hinweis zur Methode: Zahlen stammen überwiegend aus Hersteller-Datenblättern bzw. Hersteller-Blogs. Sie schwanken je Produkt, Prüfbedingung (HDT bei 0,45 MPa vs. 1,8 MPa) und Hersteller. Wo nur Sekundärquellen vorlagen, steht "ca." oder "Herstellerangabe". Die Emissionsstufen (niedrig/mittel/hoch) sind meine Einordnung aus den unten genannten Studien/Quellen, keine genormten Werte.

## 1. Zuordnung der drei Erinnerungen des Nutzers

| Erinnerung | Passendes Material | Begründung / Quelle |
|---|---|---|
| Gummi weich + hart für Tischfüße | TPU: 85A/90A (weich, viel Grip), 95A (härter, haltbarer), 64D/68D (hart, fast Kunststoff). TPE/TPU generell. | QIDI-Furnitureguide: 95A für oft gezogene Stühle, 85A für schwere, stehende Möbel; Reibwert ca. 0,70-0,80 (95A) und 0,80-0,90 (85A) auf Hartholz, Gleiter ca. 0,30, Filz ca. 0,35 (Herstellerangabe): https://qidi3d.com/blogs/print-lab/tpu-furniture-feet-stop-sliding |
| Außen, UV-beständig, hitzebeständig bis ca. 100 °C | ASA (HDT ca. 85-100 °C), für sicher >100 °C: PC (114 °C) bzw. ASA-CF/PPA-CF/PET-CF. ABS ähnlich heiß, aber nicht UV-fest. | 3DXTech: ASA UV "excellent", ABS "poor", HDT ASA 85-96 °C, ABS 80-98 °C: https://www.3dxtech.com/blogs/featured/asa-filament-vs-abs-outdoor-uv-parts ; 3DJake: ASA "5-10 Jahre Außeneinsatz", PC nur UV-stabilisiert: https://www.3djake.com/info/guide/outdoor-ready-filaments-for-weather-resistant-3d-prints |
| Bis ca. 60 °C, weniger Emissionen | PETG (HDT ca. 70-78 °C, Emissionen niedriger als ABS/ASA/Nylon). Alternative: PLA nur bis ca. 50-55 °C (reicht NICHT sicher für 60 °C), getempertes PLA Tough/HT-PLA bis ca. 100 °C. | Harper-Vergleich: PLA 60, PETG 78 °C HDT: https://jon-harper.github.io/abs/materials ; Bambu/QIDI-Tabelle PETG Basic 72,9 °C, PLA Basic 55,3 °C: https://qidi3d.com/en-br/pages/choose-the-right-filament-3d-filaments-guide-for-qidi-printers ; Emissionen: siehe Abschnitt 4 |

Fazit der Zuordnung: "100 °C + UV" = ASA (bei Dauerlast eher knapp, dann PC/ASA-CF/PPA-CF), "60 °C + wenig Emissionen" = PETG (oder PLA nur wenn wirklich <50 °C), "Gummi" = TPU 85A-95A (hart: 64D-68D).

## 2. Haupttabelle (je Material)

Legende Emissionen: niedrig / mittel / hoch (eigene Einordnung aus Studien, siehe Abschnitt 4).

| Material | Wärmeform (HDT 0,45 MPa; ca.) | UV / Witterung | Härte / Zähigkeit | Typische Anwendung | Emissionen | Druckanforderungen |
|---|---|---|---|---|---|---|
| PLA | ca. 50-60 °C (Bambu PLA Basic 55,3 °C; Harper 60 °C; Polymaker 50 °C; JLC ca. 53 °C) | schlecht: dunkelt in Wochen nach, wird in Monaten spröde (3DJake) | hart, spröde | Prototypen, Deko, Innenraum-Halter | niedrig (aber nicht null; UFP + VOC) | Düse ca. 190-230 °C, Bett 45-60 °C, keine Kammer nötig (offen sogar besser), keine gehärtete Düse, Trocknen optional (Bambu: 50 °C 4-8 h), mäßig hygroskopisch |
| PLA+ / PLA Tough | ungetempert ca. 55-60 °C; getempert (HT-PLA Pro: 30 min bei 100 °C) von 56,3 auf 107,6 °C (Herstellerangabe) | schlecht wie PLA | zäher als PLA, weniger spröde | Halter, Clips, Gehäuse Innen | niedrig | wie PLA; Tempern kann Teile verziehen (Polymaker) |
| PETG | ca. 70-78 °C (Bambu PETG Basic 72,9 °C; Filamentive 70 °C, Tg 77 °C; Harper 78 °C) | mittel: klare Typen vergilben nach 3-6 Monaten starker Sonne, trüb nach 6-18 Monaten (3DJake); weicht bei Sonne eher | zäh, leicht flexibel, schlagfest | Halter, Gehäuse, Teile bis ca. 60 °C, feuchte Umgebung | niedrig bis mittel (Studie fand Styrol-Spuren in PETG; Gesamt-VOC deutlich unter ABS) | Düse ca. 230-260 °C, Bett 70-80 °C, keine Kammer nötig, keine gehärtete Düse, Trocknen empfohlen (60-65 °C 4-6 h), hygroskopisch (Fäden/Blasen) |
| ASA | ca. 85-100 °C (3DXTech 85-96 °C; Harper 102 °C; Bambu/QIDI ASA 93 °C) | sehr gut: kaum Farb-/Strukturänderung nach Jahren (3DJake, 3DXTech) | zäh, mittel-hart, bruchfest | Außenteile, Halterungen, Autoteile, Gehäuse im Freien | mittel bis hoch (ABS-ähnliche VOC, Styrol aber unter 1/4 von ABS laut Molecules-Studie; CIRI-Bericht: ASA setzt viele schädliche VOC frei) | Düse 240-260 °C, Bett 80-110 °C, Kammer empfohlen (nicht zwingend; 3DXTech), keine gehärtete Düse, Trocknen 70-80 °C 4-6 h, schwach hygroskopisch, Warping/Zugluft-empfindlich |
| ABS | ca. 80-100 °C (3DXTech 80-98 °C; Harper 100 °C; Bambu ABS 85-92 °C) | schlecht: Butadien-Phase baut ab, Kreiden/Vergilben (3DXTech, 3DJake) | zäh, schlagfest, bearbeitbar (Acetonglätten) | Innenraum-Gehäuse, Technikteile | hoch (Styrol, hohe UFP; ABS/HIPS/Nylon höchste Partikel in CIRI-Studie, schwarz etwas niedriger) | Düse 240-260 °C, Bett 90-110 °C, geschlossene Kammer nötig (Warping), keine gehärtete Düse, Trocknen 70-80 °C |
| PC (Polycarbonat) | ca. 110-114 °C (Polymaker PolyMax PC: 114 °C bei 0,45 MPa, 99 °C bei 1,8 MPa). Blends wie PC-ABS-FR (Bambu) nur 88 °C | unmodifiziert: vergilbt nach 2-4 Monaten starker Sonne; UV-stabilisiert 12-18 Monate intensiv / 4-5 Jahre moderat (3DJake) | sehr zäh und steif | Technische Teile, hitzebelastete Halter, Gehäuse | mittel bis hoch (sehr hohe Drucktemperatur; kaum Studiendaten zu PC, daher Schätzung) | Düse 250-270 °C (Polymaker), Bett 90-105 °C, Kammer Pflicht (große Teile: beheizte Kammer), Trocknen Pflicht (75 °C 6 h, ältere Fassung 90 °C 8 h), stark hygroskopisch, Tempern empfohlen; Klebestift auf Bett |
| PA6 / PA12 (Nylon) | PA6 ca. 70-111 °C je Quelle (Datenlage uneinheitlich: Harper 111 °C, andere ca. 70 °C, UltraPA bei Bambu/QIDI 79,6 °C); PA12 etwas niedriger als PA6 (3DJake) | unstabilisiert schlecht (vergilbt, versprödet); PA12 feuchtestabiler als PA6; UV-stabilisierte Typen nötig (3DJake) | zäh, verschleißfest, reibungsarm, nimmt Feuchte auf und wird weicher (bei PA6 bis auf ca. 1/3 der Trockensteifigkeit) | Zahnräder, Scharniere, Lager, Clips | hoch (Caprolactam; CIRI: Nylon hohe Partikel + Caprolactam) | Düse ca. 250-290 °C, Bett 70-80 °C (+ PVP-Kleber), Kammer empfohlen, keine gehärtete Düse (ungefüllt), Trocknen 80-100 °C 4-6 h und trocken drucken (Pflicht), stark hygroskopisch (PA6 ca. 3-9 % bei CF-Typ) |
| PA-CF (PA12-CF/PA6-CF) | PA12-CF Bambu 138 °C; Hersteller: PA6-CF ca. 203 °C, PA12-CF ca. 175 °C (Lastbedingung nicht genannt, nicht direkt vergleichbar) | wie PA; CF verbessert UV-Stabilität etwas, aber Nylonmatrix hygroskopisch | sehr steif, hart, wenig zäh, Oberfläche matt | Konstruktionsteile, Werkzeuge, Halter | hoch (Nylon + Faserstaub beim Schleifen) | Hartstahldüse Pflicht (Messing verschleißt), Düse ca. 270-300 °C, Bett 70 °C (+Kleber), Kammer empfohlen/warm, Trocknen 80-100 °C 4-6 h Pflicht, stark hygroskopisch |
| PPA-CF / PAHT-CF | 190-197 °C (Bambu PAHT-CF 190,7 °C; Ultra PA-CF25 196,9 °C) | gut (hitzestabile Matrix), Feuchteaufnahme geringer als PA6 | sehr steif, hart | Heiße Motorraum-/Industrieteile, Halter | hoch (Schätzung) | Hartstahldüse, Hotend muss 320 °C halten (QIDI), Düse ca. 285-320 °C, Bett 70-90 °C, Kammer stark empfohlen, Trocknen 80-100 °C (Bambu PPA-CF 100-130 °C 8-12 h) |
| PET-CF | ca. 155 °C (Bambu PET-CF17: 155,3 °C) | gut, besser als PETG | steif, hart | Halter, Vorrichtungen, formstabile Teile | niedrig bis mittel (PETG-Basis; Faserstaub) | Hartstahldüse, Düse ca. 260-290 °C, Bett 70-80 °C, aktive Kammerheizung nicht nötig (QIDI, Flashforge), Trocknen 100 °C 4-10 h Pflicht |
| TPU 95A | niedrig (Herstellerangabe teils ca. 47-60 °C Erweichung; unsicher) | mittel; Polyester-/Polyether-TPU unterschiedlich, kaum Daten | flexibel, abriebfest, ca. Gummi | Dichtungen, Stoßdämpfer, Tischfüße (haltbar) | mittel (TVOC ähnlich PETG/ASA, unter ABS/Nylon; Partikel mittel; Herstellerseite Alveo3D, im Original prüfen) | Düse ca. 220-240 °C, Bett 30-60 °C, Direktextruder ideal (Bowden mit Vorsicht), 20-40 mm/s, keine Kammer nötig, keine gehärtete Düse, Trocknen Pflicht (60-80 °C 4-6 h), hygroskopisch |
| TPU 85A / 90A | niedrig | wie TPU | sehr weich bis weich, hoher Grip | Tischfüße, Griffe, Dämpfer | mittel | wie 95A, aber nur Direktextruder, 15-25 mm/s, kurzer Filamentweg (sonst Knicken/Stau), Retraktion minimal oder aus bei <=85A; nicht in AMS/Mehrfarbsystem (Bambu: 95A/85A/83A/80A "not compatible" mit AMS) |
| TPU 64D / 68D | höher als A-Typen | wie TPU | halbhart, fast Kunststoff | Zahnräder, Schutzkappen, harte Gummifüße | mittel | leichter zu drucken als 85A (QIDI-Tabelle); Bambu "TPU for AMS" ist 68D und AMS-tauglich (Max-Volumenfluss 18 mm³/s), 95A HF 12 mm³/s |
| TPE | niedrig | unterschiedlich | sehr weich, gummiartig | weiche Dichtungen | mittel | wie weiches TPU, oft schwieriger (klebrig, weicher), Direktextruder zwingend |
| PP | ca. 90-100 °C (Schmelz ca. 160 °C; Bambu/Prusa: keine HDT in Quellen, daher Schätzung) | mittel, ohne Stabilisator abbaubar | zäh, ermüdungsfest, Filmscharniere (All3DP) | Behälter, chemikalienfeste Teile, Scharniere | niedrig bis mittel (keine Studiendaten) | Düse 220-275 °C, Bett 85-100 °C, Kammer ca. 45-60 °C empfohlen, PP-Platte/Packband für Haftung, Brim 5-10 mm (Prusa); Warping + schlechte Haftung Hauptproblem |

Nicht in Quellen belegt (daher Schätzung/offen): PP-HDT, TPU-HDT, Emissionsdaten für PC/PP/PET-CF/PPA-CF direkt gemessen.

## 3. TPU im Detail (Gummifüße)

- Shore-A (85A/90A/95A) und Shore-D (64D/68D) sind getrennte Skalen, nicht direkt vergleichbar (Fabbaloo/Flashforge): https://www.flashforge.com/blogs/news/tpu-filament-guide
- Für rutschfeste Tischfüße: 85A = maximaler Grip, aber schneller abgenutzt und schwerer zu drucken; 95A = Kompromiss für oft verschobene Stühle (QIDI): https://qidi3d.com/blogs/print-lab/tpu-furniture-feet-stop-sliding. Die Reibwerte sind Herstellerangaben, auf dem realen Boden testen.
- Gegenmeinung (Bambu-Forum): TPU greift nicht so gut wie echte Gummifüße, 90A sei brauchbar, 85A und weicher besser aber mühsam: https://forum.bambulab.com/t/rubber-feet-filament/254316
- TPU ist laut QIDI bodenschonend (hinterlässt keine schwarzen Striche wie Naturkautschuk), nur Herstellerangabe.
- Druckparameter für Füße (QIDI): 0,2 mm Schicht, 5-8 Bodenschichten, Infill 30-50 %, 30-50 mm/s (95A) bzw. 15-25 mm/s (85A), Düse 220-240 °C, Bett 50-60 °C, Retraktion 0,5-1,5 mm.
- Direktextruder: 85A/90A scheitern fast immer an Bowden-Systemen (Knicken); kurzer Weg verhindert Ausknicken (Siddament/Siraya/Flashforge): https://siddament.com.au/blogs/filament-guide-au/how-to-print-tpu und https://siraya.tech/blogs/news/how-to-print-with-tpu
- Mehrfarbsystem: Bambu-Wiki: weiche TPU (95A, 85A, 83A, 80A) nicht für AMS/AMS Lite, sie werden vom Rad plattgedrückt und verstopfen; nur das 68D-"TPU for AMS" ist AMS-tauglich: https://wiki.bambulab.com/en/knowledge-sharing/tpu-printing-guide (Inhalt per Suchergebnis, Seite selbst nicht abrufbar, daher sekundär). Ergänzend: https://forum.bambulab.com/t/tpu-95a-on-ams-ht/236373
- Trocknen: nasses TPU ist extrem schwer zu drucken (Flashforge/Siddament).

## 4. Emissionen (Quellenlage)

- Azimi 2016: Styrol aus ABS und HIPS, Caprolactam aus Nylon (ca. 2-180 µg/min); ABS emittiert mehr Partikel als PLA oder Nylon: https://pubmed.ncbi.nlm.nih.gov/26741485/ (Seite nicht abrufbar, Inhalt per Suchzusammenfassung).
- Stephens 2013: UFP ca. 2,0e10/min (PLA-Drucker) bis ca. 1,9e11/min (ABS-Drucker): https://ui.adsabs.harvard.edu/abs/2013AtmEn..79..334S/abstract
- Molecules 2022 (ABS, ASA, Nylon, PETG): VOC-Raten von Nylon und PETG über eine Größenordnung unter ABS; ASA ähnlich zusammengesetzt wie ABS, Styrol unter 1/4 von ABS: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC9229569/ (nur Suchzusammenfassung).
- Zhang/Black 2023 (45 Filamente, 13 Hersteller, 14 Drucker): höchste Partikel bei ABS, HIPS, Nylon; PLA am niedrigsten; Nylon viel Caprolactam; ASA setzt hohe VOC-Mengen frei; Formaldehyd, Styrol, Acetaldehyd bei 80 % der Filamente nachgewiesen: https://3dprintingindustry.com/news/ciri-research-raises-health-concerns-over-3d-printing-emissions-226978/
- TPU: Mittel (Partikel), TVOC ähnlich PETG/ASA, unter ABS/Nylon laut Alveo3D (Hersteller-Zusammenfassung der obigen Studie): https://www.alveo3d.com/en/tpu-filament-have-toxic-fumes-like-abs/
- PETG enthält nachweislich Styrol-Spuren (IJERPH 2021): https://www.mdpi.com/1660-4601/18/21/11670/html
- Byrley-Metaanalyse: ABS höhere Gesamtemission als PLA, niedrigere Düsentemperatur senkt Emission (zitiert in CDC-Paper).
- Filter: HEPA fängt Partikel, Aktivkohle gasförmige VOC; ein am Gehäuse angebrachter Filter garantiert nicht, dass die Kammerluft wirklich hindurchströmt; keine belastbaren Messdaten zur Filtereffizienz in Drucker-Kammern gefunden: https://3dbite.com/3d-printing-fumes-ventilation-home-safety-guide/ (Blog, schwache Quelle). Fazit: Geschlossene Kammer mit Filter reduziert, ersetzt aber bei ABS/ASA/Nylon/PC keine Lüftung.
- Die Studien gelten für verschiedene Drucker und Bedingungen; Partikel reagieren empfindlicher auf Druckbedingungen als VOC.

## 5. Daraus folgende Pflicht-Druckermerkmale

| Wunsch | Pflicht am Drucker | Begründung / Quelle |
|---|---|---|
| ASA/ABS | Geschlossene Kammer, Bett bis 100-110 °C, Düse bis 260 °C | 3DXTech, Bambu/QIDI-Tabelle (Bett 80-110 °C) |
| PC | Düse 270-300 °C (Polymaker: 250-270 °C, Bett 90-105 °C), Kammer; große Teile beheizte Kammer, Filamenttrockner | Polymaker TDS: https://polymaker.com/wp-content/uploads/TDS_Polymaker_PolyMax-PC_V5.5_2026-01-05_EN.pdf |
| PA/Nylon, PA-CF | Aktive Kammerheizung oder zumindest warme Kammer (ca. 50-60 °C, PrintPal: warmer Raum), Düse ca. 290-300 °C, Hartstahldüse für CF, Trocknung 80-100 °C | https://printpal.io/wiki/engineering-materials-guide ; https://wiki.bambulab.com/knowledge-sharing/pa-cf-printing-tips |
| PPA-CF / PAHT-CF | Hotend 320 °C, Hartstahldüse, Kammerheizung | https://qidi3d.com/en-br/blogs/guides/how-to-print-paht-cf-filament |
| PET-CF | Hartstahldüse, Trockner (100 °C); aktive Kammer nicht nötig | QIDI/Flashforge (Quellen widersprechen sich teilweise) |
| TPU weich | Direktextruder, möglichst kein Mehrfarbsystem für weiche Typen, Trockner | Bambu-Wiki, Siddament |
| PP | Beheiztes Bett ca. 85-100 °C, warme Kammer 45-60 °C, PP-Haftfläche | Prusa: https://help.prusa3d.com/en/article/polypropylene-pp_167126 ; Simplify3D: https://Simplify3D.com/resources/materials-guide/polypropylene/ |
| Allgemein | Filamenttrockner (PA, PC, TPU, PET-CF Pflicht; PETG empfohlen), Hartstahl-/Hotend mit Wolframkarbid-Option, abnehmbare HEPA+Aktivkohle-Filterung im Kammerkreislauf, Bett >= 100 °C | siehe oben |

Kurz: Für "möglichst viele Materialien": Düse >= 300 °C, Bett >= 100-110 °C, aktive Kammerheizung (Ziel 60 °C+), Hartstahldüse (gehärtet) für CF, Direktextruder (für 85A TPU), Filament-Trockner, HEPA + Aktivkohle.

## 6. Materialvergleich Kurzüberblick (Hitze, UV)

- Harper-HDT-Tabelle (0,45 MPa): PLA 60, PETG 78, ABS 100, ASA 102, PC-ABS 106, PA6 111, PC 114 °C: https://jon-harper.github.io/abs/materials
- Bambu/QIDI Filamentführer: PLA Basic 55,3; PETG 72,9; ASA 93; PC-ABS-FR 88; UltraPA 79,6; PA12-CF 138,2; PET-CF17 155,3; PAHT-CF 190,7; Ultra PA-CF25 196,9: https://qidi3d.com/en-br/pages/choose-the-right-filament-3d-filaments-guide-for-qidi-printers
- Outdoor: ASA > PETG (Wasser) > PC (stabilisiert); ABS nicht: https://www.3djake.com/info/guide/outdoor-ready-filaments-for-weather-resistant-3d-prints ; ASA-Vergleich: https://www.matterhackers.com/articles/the-best-3d-printing-filament-for-outdoor-use
- PLA im heißen Auto verformt sich; Tempern kann bei PLA Tough/HT-PLA HDT auf ca. 100 °C anheben: https://polymaker.com/ht-pla-pro-engineering-toughness-into-heat-stable-pla/

BEST-PRACTICES-KANDIDATEN:
- Materialwahl nach Anforderung: 60 °C + wenig Emission = PETG (nicht PLA, HDT 50-60 °C); Außen + bis ca. 100 °C = ASA (UV "excellent", HDT 85-100 °C), ABS draußen vermeiden. https://www.3dxtech.com/blogs/featured/asa-filament-vs-abs-outdoor-uv-parts ; https://www.3djake.com/info/guide/outdoor-ready-filaments-for-weather-resistant-3d-prints
- Tischfüße/Gummi: TPU 95A für oft verschobene Stühle, 85A für stehende Möbel; auf realem Boden testen; Bodenkontaktfläche auf das Bett legen, 5-8 Bodenschichten. https://qidi3d.com/blogs/print-lab/tpu-furniture-feet-stop-sliding
- Weiches TPU (<=90A): Direktextruder, 15-25 mm/s, minimale/keine Retraktion, nicht über AMS/Mehrfarbsystem (nur 68D "TPU for AMS" ist dafür gemacht). https://siddament.com.au/blogs/filament-guide-au/how-to-print-tpu ; https://wiki.bambulab.com/en/knowledge-sharing/tpu-printing-guide
- Hartstahl-Düse bei jedem CF-/GF-Filament; Messing verschleißt. https://qidi3d.com/blogs/news/3d-printing-carbon-fiber-filament-guide ; https://printpal.io/wiki/engineering-materials-guide
- Filamentqualität: Nylon, PC, TPU, PET-CF vor dem Druck trocknen (PA/PC 75-100 °C, 4-8 h) und aus trockener Box drucken. https://polymaker.com/wp-content/uploads/TDS_Polymaker_PolyMax-PC_V5.5_2026-01-05_EN.pdf
- PLA Tough/HT-PLA bei Bedarf tempern (z. B. 100 °C, 30 min) für höhere HDT, mit Verzugsrisiko. https://polymaker.com/ht-pla-pro-engineering-toughness-into-heat-stable-pla/
- Emissionen: ABS/Nylon/ASA höchste Partikel/VOC, PLA am niedrigsten; Filter (HEPA + Aktivkohle) mindern, ersetzen aber keine Lüftung. https://3dprintingindustry.com/news/ciri-research-raises-health-concerns-over-3d-printing-emissions-226978/
- Datenblatt-Abgleich: HDT-Werte immer mit Prüflast (0,45 vs. 1,8 MPa) lesen; Quellen widersprechen sich (PA6 70 vs. 111 °C).

BUG-KANDIDATEN:
- Weiches TPU (85A/90A) knickt/staut im Extruder bei langem Filamentweg (Bowden) oder zu hoher Retraktion; Abhilfe Direktextruder, langsam, Retraktion minimal oder aus. https://siddament.com.au/blogs/filament-guide-au/how-to-print-tpu
- Weiches TPU im AMS/AMS Lite: Rad drückt es platt, Verstopfung ("Not compatible: 95A, 85A, 83A, 80A"). https://wiki.bambulab.com/en/knowledge-sharing/tpu-printing-guide
- Nasses Filament (TPU, Nylon, PC, PET-CF): Blasen, Fäden, Stau, Festigkeitsverlust; Trocknung ist Pflicht. https://qidi3d.com/en-br/pages/choose-the-right-filament-3d-filaments-guide-for-qidi-printers
- PP: starkes Warping und schlechte Bettaaftung; Abhilfe PP-Platte/Packband, Brim 5-10 mm, Kammer 45-60 °C. https://help.prusa3d.com/en/article/polypropylene-pp_167126 ; https://Simplify3D.com/resources/materials-guide/polypropylene/
- PETG klebt zu stark an glatten PEI-Platten; Klebestift/Fensterreiniger als Trennschicht. https://blog.prusa3d.com/advanced-filament-guide_39718/ (Kommentar von Prusa-Mitarbeiter)
- PC und Nylon brauchen Klebestift/PVP-Kleber auf der Platte (Haftung/Warping). https://blog.prusa3d.com/advanced-filament-guide_39718/
- PLA im heißen Auto (>50-60 °C) verformt sich; PETG draußen vergilbt/trübt nach Monaten. https://polymaker.com/ht-pla-pro-engineering-toughness-into-heat-stable-pla/ ; https://www.3djake.com/info/guide/outdoor-ready-filaments-for-weather-resistant-3d-prints
- Unmodifiziertes PC und Nylon draußen: vergilben/verspröden schnell, nur UV-stabilisierte Typen. https://www.3djake.com/info/guide/outdoor-ready-filaments-for-weather-resistant-3d-prints
- Messingdüse + CF-Filament: Düse verschleißt schnell (Durchmesser wächst). https://printpal.io/wiki/engineering-materials-guide
- Webabruf: PubMed/PMC (Cloudflare/Cookie-Seite) und Bambu-Wiki (HTTP 402) ließen sich mit WebFetch nicht lesen; nur Suchzusammenfassungen verfügbar (Recherche-Einschränkung, kein Druckfehler).
