# R1: Bambu Lab, geschlossene Drucker, Stand 19.11.2026

Quellenlage: Bambu-Seiten (bambulab.com, eu.store, wiki) sind per WebFetch gesperrt (403/402); wiki.bambulab.com und die EU-Store-HTML waren per curl lesbar. Datenblatt-PDFs (H2S, H2D, H2D Pro) von Händlerseiten per pdftotext gelesen. Nicht jede Zahl ist von Bambu selbst bestätigt, Kennzeichnung unten: [W]=offizielles Bambu-Wiki, [DB]=Bambu-Datenblatt (PDF bei Händler), [S]=Bambu-EU-Store am 19.11.2026, [P]=Presse/Händler.

## 1. Aktuelles Lineup (geschlossene Drucker)

| Modell | Status |
|---|---|
| X2D | NEU, seit 14.04.2026, Nachfolger des X1 Carbon, aktuell. [P] https://mizumakes.com/posts/bambu-lab-new-printers-2026/ |
| H3C | NEU, seit 03.11.2026, 9-Werkzeug-Wechsler, ca. 2.499 EUR. [P] https://3dprinting.com/news/bambu-lab-launches-h3c-nine-tool-changer-3d-printer-priced-from-2499/ , https://ap3ddesign.com/en/news/bambu-lab-h3c/ |
| H2D | aktuell (seit 03/2025) |
| H2D Pro | Enterprise-Variante, über autorisierte Händler/Firmenkanal, nicht der normale Privatweg. [W] https://wiki.bambulab.com/en/h2/manual/h2d-pro-faq |
| H2S | aktuell (seit 08/2025) |
| H2C | aktuell (seit 11/2025), Vortek-Düsenwechsel |
| P2S | aktuell (seit 10/2025) |
| P1S | läuft aus, wird mit Rabatt abverkauft, durch P2S ersetzt. [P] https://3dprinting.com/3dprinters/bambu-lab-3d-printer-comparison/ |
| X1 Carbon / X1 / X1E | Produktion X1-Serie Ende 03/2026 beendet; X1C offiziell "retired"; Firmware-Support bis 05/2027, Ersatzteile bis 03/2031. X1E nur noch Geschäftskanal. [P] https://mizumakes.com/posts/bambu-lab-new-printers-2026/ , https://spoolhound.com/which-bambu-lab-printer |

## 2. Technische Daten je Modell

### X2D (14.04.2026)
- Düse max 300 °C, Bett 120 °C. Wiki: https://wiki.bambulab.com/en/x2d/manual/x2d-faq
- Kammer: aktiv geheizt (PTC + Umluft), max 65 °C. [W] gleiche URL
- Filter: ja. G3-Vorfilter, H12-HEPA, Aktivkohle (3dprinting.com); Notebookcheck: "HEPA + Aktivkohle", 3 Stufen (Makers101). Im Gerät, Abluft-Kit mit Filter. https://3dprinting.com/3dprinters/bambu-lab-3d-printer-comparison/ , https://www.notebookcheck.com/Mit-zwei-Duesen-starken-Features-und-Abluftfilter-3D-Drucker-X2D-startet.1275467.0.html , https://makers101.com/de/bambu-lab-x2d-specs-release-date/
- Gehärtet: Düse und Extrudergetriebe gehärteter Stahl. [W]
- Bauraum: 256x256x260 mm (eine Düse), 235,5x256x256 mm (beide Düsen). Makers101 s.o.
- Zwei Düsen auf einem Kopf (Haupt-Düse links Direct Drive, Hilfsdüse rechts Bowden, nur 200 mm/s; für Support/zweites Material). Makers101 s.o.
- AMS: AMS 2 Pro und AMS HT, bis 25 Farben (4 AMS 2 Pro + 8 AMS HT). [W]
- TPU: nur "TPU for AMS" (Shore 68D, über AMS), weichere TPU (85A/90A/95A) manuell per externer Spule/TPU-Feed-Assist-Modul über die Haupt-Düse. [W] https://wiki.bambulab.com/en/x2d/manual/x2d-faq ; Datenblatt 68D: https://cdn.shopify.com/s/files/1/1339/4265/files/Bambu_TPU_for_AMS_Technical_Data_Sheet.pdf
- Materialliste Wiki: PLA, PETG, ABS, ASA, PET, PA, PC, PVA, CF/GF-PLA, PETG*, ABS, ASA*, PA6*, PAHT; "mit Vorsicht": PETG-CF, ASA-CF, PA6-CF, TPU for AMS. [W]
- Präzision: KEIN Micro-Lidar (Makers101). Vision Encoder (50 µm Bewegungsgenauigkeit) nur als separates Zubehör (Wiki: "purchased separately"; EU-Store listet "Vision Encoder H2-Serie / H3C" 83,97 EUR mit 30 % Rabatt, derzeit ausverkauft [S]).
- Preis DE: Start-UVP 629 EUR (ohne AMS) / 849 EUR (Combo, AMS 2 Pro). https://www.notebookcheck.com/Mit-zwei-Duesen-starken-Features-und-Abluftfilter-3D-Drucker-X2D-startet.1275467.0.html . Bambu-EU-Store 19.11.2026: 749 EUR (Variante nicht eindeutig, vermutlich Combo), "30-day lowest" 849 EUR [S] https://eu.store.bambulab.com/de/products/x2d ; Black-Friday laut All3DP-Titel 549/749 EUR https://all3dp.com/de/4/black-friday-bei-bambu-lab-gestartet-x2d-fuer-549-e-filament-bis-zu-47-guenstiger/ (Inhalt nicht abrufbar, nur Titel).
- Verlässlichkeit: viele Forenberichte über Probleme (siehe BUG-KANDIDATEN).

### H2D (2025)
- Düse max 350 °C, Bett 120 °C. [DB] https://materialpro3d.cz/user/related_files/h2d_spec.pdf
- Kammer: aktiv, max 65 °C. [DB] (ein Händlervergleich nennt 60 °C, widersprüchlich: https://ultimate3dprintingstore.com/a/s/products/bambu-lab-h2d-pro-3d-printer)
- Filter: G3-Vorfilter, H12-HEPA, Kokosnuss-Aktivkohle; im Gerät. [DB] s.o.
- Gehärtet: Düse und Extrudergetriebe gehärteter Stahl. [DB]
- Bauraum: 325x320x325 (eine Düse), 300x320x325 (Dual), 350x320x325 Gesamt. [DB]
- Zwei Düsen, Wechsel per Düsenwechsel-Kopf; AMS 2 Pro + AMS HT. Optional Laser 10/40 W.
- TPU: H2-Serie, TPU 85A/90A/95A HF nur manuell (kein AMS), 85A direkt in den Extruder, nur über rechte Düse (H2D/H2C); AMS HT als Trocknung/Spulenhalter nutzbar; TPU for AMS geht in AMS/AMS 2 Pro/AMS HT. [W] https://wiki.bambulab.com/en/h2/h2d-tpu-printing-guide
- Präzision: Micro-Lidar (Z-Höhe/Flow), Nozzle-Kamera. [DB]; Vision Encoder optional.
- Preis DE: EU-Store 19.11.2026 "ab 1.399 EUR" (30-Tage-Tief 1.549 EUR) [S] https://eu.store.bambulab.com/de/products/h2d ; EU-Startpreis 1.899 EUR (https://eu.store.bambulab.com/de/collections/h2d via Suchtreffer); Juni-Sale-Combo 1.699 EUR https://www.mydealz.de/deals/bambu-lab-4-anniversary-sale-versch-drucker-stark-reduziert-2793782

### H2D Pro (Enterprise)
- Wie H2D (350 °C, 65 °C, H12-HEPA + Kohle) plus: Wolframkarbid-Düse, Toolhead-Enhanced-Cooling-Fan, Ethernet/WPA2-Enterprise, Vision Encoder serienmäßig (5 µm Auflösung, 50 µm Genauigkeit). Vertrieb über autorisierte Händler. [W] https://wiki.bambulab.com/en/h2/manual/h2d-pro-faq ; [DB] https://m3.tuc.gr/media/xgxnc4i3/τεχνικο-φυλλαδιο-bambu-lab-h2d-pro.pdf
- Preis: US 3.799 USD (https://printer-hub.ru/en/compare/bambu-lab-h2d-vs-bambu-lab-h2d-pro). Euro-Preis nicht gefunden. Für Privatnutzer unnötig: Wolframdüse und Cooling-Fan sind laut Bambu auch für den H2D nachrüstbar (Wiki-Aussage Q4/2025).

### H2S (08/2025)
- Düse max 350 °C, Bett 120 °C. [W] https://wiki.bambulab.com/en/h2/manual/h2s-faq ; [DB] https://www.print3dd.com/wp-content/uploads/2025/08/h2s_specs.pdf.pdf
- Kammer: aktiv, 65 °C (Bambu-Seite laut Suchtreffer https://bambulab.com/en-gb/h2s ; Datenblatt: Active Chamber Heating).
- Filter: G3-Vorfilter, H12-HEPA, Kokosnuss-Aktivkohle, Standard im Gerät (Suchtreffer-Zusammenfassung des Datenblatts; Tom's Hardware bestätigt HEPA + Kohle) https://www.tomshardware.com/3d-printing/bambu-lab-h2s-review-the-one-we-wanted
- Gehärtet: Düse und Extrudergetriebe gehärteter Stahl. [DB]
- Bauraum: 340x320x340 mm (größter aller Bambu-Drucker bei einer Düse). [DB]
- Eine Düse, Direct Drive (kein Düsenwechsler), AMS 2 Pro + AMS HT, Laser-Option 10 W.
- Materialien laut Datenblatt: PLA, PETG, TPU, PVA, BVOH, ABS, ASA, PC, PA, PET, PPS; CF/GF-PLA, -PETG, -PA, -PET, -PC, -ABS, -ASA, PPA, PPS. [DB]
- TPU: Datenblatt TPU unterstützt; Praxis siehe TPU-Guide H2-Serie (85A/90A/95A manuell zuführen, kein AMS; hier nur eine Düse, daher kein Sonderproblem "rechte Düse"). https://wiki.bambulab.com/en/h2/manual/h2s-faq
- Präzision: Micro-Lidar. [DB]; Vision Encoder optional.
- Preis DE: EU-Store 19.11.2026 999 EUR (30-Tage-Tief 1.099 EUR; Variante nicht eindeutig, vermutlich ohne AMS) [S] https://eu.store.bambulab.com/de/products/h2s ; Suchtreffer EU-Store: H2S AMS Combo 1.149 / Laser Full Combo 1.499 (unbestätigt); Juni-Sale 1.249 EUR Combo (reguläre UVP Combo 1.399 EUR). https://www.mydealz.de/deals/bambu-lab-4-anniversary-sale-versch-drucker-stark-reduziert-2793782
- Amazon.com-Listings existieren (B0HP5M44DR u. a.), teils "unavailable". https://www.amazon.com/BAMBULAB-Bambu-Lab-H2S-Combo/dp/B0HP5M44DR

### H2C (11/2025)
- 350 °C, aktive Kammer 65 °C, G3 + H12-HEPA + Kohle. Bauraum 330x320x325 (Dual 300x320x320). Vortek-Düsenwechsel (induktive Hotend-Wechsel, mehrfarbig ohne viel Ausschuss). Nur als Combo. https://3dprinting.com/3dprinters/bambu-lab-3d-printer-comparison/
- Preis DE: EU-Store 1.799 EUR (30-Tage-Tief 2.149 EUR) [S] https://eu.store.bambulab.com/de/products/h2c
- Aufwändiger/teurer, Mehrwert nur bei vielen Farben.

### H3C (03.11.2026)
- 9 Werkzeuge, 330x320x325, 350 °C, Kammer 65 °C (2 Zonen), H11-HEPA + Kohle (3dprinting.com), AMS 3H im Gerät, EU-Combo 2.499 EUR (Quellen teils unzuverlässig, 3DBite genauer). https://3dbite.com/bambu-lab-h3c-launched-confirmed-specs-vs-rumours/ , https://ap3ddesign.com/en/news/bambu-lab-h3c/
- Zu neu/teuer für den Zweck, keine Langzeitdaten.

### P2S (10/2025)
- Düse max 300 °C, Bett 110 °C. Gehärteter Stahl (Düse + Getriebe). [W] https://wiki.bambulab.com/en/p2s/manual/p2s-faq
- Kammer: KEINE aktive Heizung, nur passive Wärme durch geschlossenes Gehäuse (Wiki: "does not have an active chamber temperature function"). [W]
- Filter: nur Aktivkohle (Umluft/Rückführung gegen VOC und Partikel), KEIN HEPA. [W]; 3dprinting.com gleiche Aussage.
- Bauraum 256x256x256 mm. Eddy-Current-Sensor (Düsen-Taster, kein Lidar), AMS 2 Pro + AMS HT, bis 20 Farben. [W]
- Materialien laut Wiki: PLA, PETG, ABS, ASA, TPU, PET, PA, PC, PVA, PLA-CF, PETG-CF, ABS-GF, ASA-CF, PA6-CF/GF, PAHT-CF, PPA-CF, PET-CF. TPU über externe Spule (Guide für 90A & 85A bei P-Serie). [W]
- Preis DE: EU-Store 19.11.2026 599 EUR (30-Tage-Tief 699 EUR; Variante nicht eindeutig, vermutlich Combo) [S] https://eu.store.bambulab.com/de/products/p2s ; Juni-Sale Combo 649 EUR (UVP alt 749 EUR).
- Amazon.de: Listing "Bambu Lab P2S Combo" B0FT818HCH gefunden, aber 110-V-Modell und "derzeit nicht verfügbar"; ein 220-V-Listing von Drittanbieter "3D MARKET" (B0G87M7V8Z). https://www.amazon.de/3D-Drucker-Geschwindigkeit-Unterst%C3%BCtzung-schl%C3%BCsselfertiger-FDM-3D-Drucker/dp/B0FT818HCH , https://www.amazon.de/Lab-P2SC-Bambus-3D-Drucker-Combo/dp/B0G87M7V8Z

### P1S (2023, Auslauf)
- Geschlossen, passive Kammer, Düse 300 °C, ohne HEPA (Aktivkohle-Filter), Düse/Getriebe in Standardausführung nicht gehärtet (braucht gehärtete Düse für CF). EU-Store ab 349 EUR (Combo 379+ ... Variantenpreise) [S] https://eu.store.bambulab.com/de/products/p1s ; Hinweis "Carbon-ready" nur mit gehärteter Düse https://spoolhound.com/which-bambu-lab-printer . Nicht empfohlen (alt, keine aktive Heizung).

### X1 Carbon / X1E
- Eingestellt (X1-Serie Ende 03/2026). X1E: aktive Kammer, nur Geschäftskanal. Nicht kaufen/neu verfügbar nur als Restposten. https://mizumakes.com/posts/bambu-lab-new-printers-2026/ , https://3dprinting.com/3dprinters/bambu-lab-3d-printer-comparison/

## 3. Amazon.de
- Offizieller "Bambu Lab Store" auf Amazon.de: NICHT bestätigt. Bambu-Forum erwähnt einen Amazon-Bambu-Store ohne Region https://forum.bambulab.com/t/parts-on-amazon/169122/9 . Auf Amazon.de wurden nur P2S-Combo-Listings gefunden (110 V, nicht verfügbar; Drittanbieter-220-V "3D MARKET"). Bambu verkauft sonst primär über eu.store.bambulab.com (Versand aus DE-Lager, 1-3 Werktage, https://eu.store.bambulab.com/products/h2d ), dazu Händler (Galaxus.de, welectron.com, heise-Preisvergleich).
- Folge: Beim Kauf auf Amazon.de auf Verkäufer, 220-V-Variante (EU-Modellnummer ...-EU) und Garantie achten; sonst Bambu EU-Store oder deutschen Fachhändler nehmen.

## 4. Empfehlung (Anforderungen: Wohnung, HEPA+Kohle, Präzision, viele Materialien inkl. PC/PA/CF/ASA/TPU, Zuverlässigkeit)

Beste Wahl: **Bambu Lab H2S** (mit AMS 2 Pro bzw. AMS HT für Trocknung).
- Erfüllt als einziges bezahlbares Modell alles: 350 °C Düse (PC, PA-CF, PPA/PPS-tauglich), aktiv geheizte Kammer 65 °C, H12-HEPA + Aktivkohle standardmäßig, gehärtete Düse + Getriebe, Micro-Lidar, größter Bauraum 340x320x340, Direct Drive für TPU, AMS 2 Pro / AMS HT.
- Preis ca. 1.000-1.250 EUR (EU-Store aktuell 999 EUR, zuvor 1.099 EUR; mit AMS Combo mehr).
- Gegenüber H2D: gleiche Filter-/Kammer-/Temperaturstufe, billiger; H2D bietet zusätzlich zweite Düse (Support/zweites Material) und 350 mm Breite. TPU 85A geht beim H2D nur über die rechte Düse.
- Alternativen: H2D (wenn zwei Düsen gewünscht, ab 1.399 EUR), H2C (viele Farben, 1.799 EUR).
- Nicht empfohlen für diesen Fall: P2S (kein HEPA, kein aktives Kammerheizen, 300 °C), P1S/X1C/X1E (Auslauf/eingestellt), X2D (nur 300 °C, kein Lidar, Vision Encoder extra, deutlich mehr Problemberichte bei Zuverlässigkeit trotz guter Ausstattung und HEPA), H2D Pro (Enterprise), H3C (neu, teuer).
- TPU: weiches TPU (85A/95A) bei allen Bambu-Druckern nur manuell/extern zuführen, nicht durch AMS; "TPU for AMS" ist 68D (hart). Vor Kauf klären: Zusatzzubehör TPU-High-Flow-Kit (H2) und TPU Feed Assist.
- Wichtig: Eine HEPA-Filterung ist auch beim H2S kein Ersatz für Belüftung bei ABS/ASA/PC: Raum lüften.

Offene Punkte (nicht verifiziert): exakte H2S-Variantenpreise im EU-Store (Seite lieferte nur den vorausgewählten Preis), Amazon.de-Verfügbarkeit offizieller Bambu-Ware, H2S-Chamber 65 °C nur über Suchtreffer/Datenblatt-Zusammenfassung.

BEST-PRACTICES-KANDIDATEN:
- Bambu H2/X2D: TPU 85A/90A/95A HF nie durch AMS/AMS 2 Pro; nur "TPU for AMS" (68D) geht in AMS; weiche TPU manuell bzw. über AMS HT als Spulenhalter, vorher trocknen, 0.4/0.6/0.8 mm, keine High-Flow-Düse für 85A, Hotend nach CF-Filament vorher möglichst nicht mit TPU nutzen. https://wiki.bambulab.com/en/h2/h2d-tpu-printing-guide
- Bambu-Wiki ist per curl (User-Agent Mozilla) lesbar, WebFetch bekommt 403/402; EU-Store-HTML per curl lesbar, Preise stehen im HTML. (Recherche-Trick)
- Bambu-Datenblätter: Filter-Klassen im Datenblatt (G3 / H12 / Kokosnuss-Kohle), H2D/H2S/H2C/X2D jeweils 65 °C aktive Kammer. https://materialpro3d.cz/user/related_files/h2d_spec.pdf
- X2D: bei PC/PA mit Vorsicht, "Print with caution" für PETG-CF, ASA-CF, PA6-CF laut Wiki. https://wiki.bambulab.com/en/x2d/manual/x2d-faq
- Amazon.de-Kauf von Bambu-Druckern: auf 220-V-EU-Modell achten (Modellnummer mit "-EU"), Verkäufer prüfen. https://www.amazon.de/3D-Drucker-Geschwindigkeit-Unterst%C3%BCtzung-schl%C3%BCsselfertiger-FDM-3D-Drucker/dp/B0FT818HCH

BUG-KANDIDATEN:
- X2D (Firmware/Hardware 2026): Foren-Berichte zu Filament-Zuführung (enge PTFE-Biegungen, fehlende Zugentlastung), AMS-HT-Unzuverlässigkeit, Wi-Fi-Abbrüche, Düsenwechsel-/Hilfsdüsen-Verstopfung in heißer Kammer, Bett schleift beim Homing; Puffer-Federspannung für eine Düse (Firmware, laut Nutzer). Nur anekdotisch, kein offizieller Rückruf. https://forum.bambulab.com/t/x2d-is-the-least-reliable-bambu-printer-in-our-fleet-list-of-issues/261709 , https://forum.bambulab.com/t/there-is-something-seriously-wrong-with-the-x2d/251144 , https://forum.bambulab.com/t/bambu-x2d-concerns/251703 , https://forum.bambulab.com/t/x2d-wifi-connection-problems-solutions/252663 , https://printer-hub.com/posts/bambu-lab-x2d-known-issues
- X2D: Hilfsdüse (rechts) Bowden, nur 200 mm/s, leichte Wandwelligkeit nur unter Vergrößerung (Tom's Hardware laut Makers101); Bambu-Studio-Filamentzuweisung für Dual-Nozzle gewöhnungsbedürftig. https://makers101.com/de/bambu-lab-x2d-specs-release-date/
- H2D (2025): Beta-Firmware 01.01.02.00 musste von Nutzern zurückgerollt werden (Einzelfall). https://forum.bambulab.com/t/01-01-02-00-beta-firmware-issues-had-to-roll-back/164402/11
- H2D/H2C: weiches TPU (85A) nur über rechte Düse zuführbar. https://wiki.bambulab.com/en/h2/h2d-tpu-printing-guide
- H2D-Datenwidersprüche in Quellen: Kammer 60 vs 65 °C, Düse 300 vs 350 °C bei Händlertabellen; Bambu-Datenblatt sagt 350 °C / 65 °C. https://ultimate3dprinting-store (s. o.)
- P2S: Amazon.de-Listing B0FT818HCH ist 110-V-Modell (PF004-U+SA007-AU) und nicht verfügbar; 220-V-Listing von Drittanbieter. https://www.amazon.de/3D-Drucker-Geschwindigkeit-Unterst%C3%BCtzung-schl%C3%BCsselfertiger-FDM-3D-Drucker/dp/B0FT818HCH
