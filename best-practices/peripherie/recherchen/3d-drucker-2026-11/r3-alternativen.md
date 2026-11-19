# R3: Alternativen zu Bambu Lab und Prusa (geschlossen, möglichst aktiv beheizt), Stand 19.11.2026

Hinweis zur Methode: WebSearch/WebFetch, Preise sind Momentaufnahmen vom 19.11.2026 (Heise/Geizhals/Smartzone) und "ohne Gewähr". Was nicht belegt werden konnte, ist als "nicht belegt" markiert. Amazon.de-Preise liessen sich über die Suche nicht direkt abrufen (Amazon blockiert Abruf), es wird auf Testseiten/Preisvergleiche verwiesen.

## Kurzfazit

1. Stärkste Alternative: QIDI Plus 5 (neu seit 05.08.2026, ersetzt Plus 4) oder, kleiner und günstiger, QIDI Q2 (nur Einzelfilament, ohne QIDI Box). Einzige Kandidaten in der Liste mit AKTIVER Kammerheizung (65 °C) + HEPA (H12) + Aktivkohle + 370 °C Düse + Direktantrieb + TPU in den Specs.
2. Zweite Wahl: Creality K2 Pro (60 °C aktive Heizung, Düse nur 300 °C, Filter nur Aktivkohle-Luftreiniger, Kammer erreichte im Test nur ~45-57 °C) oder K2 Plus (350 °C, 2 Filter, teuer, Bastler-Ruf).
3. Nicht geeignet für die Anforderungen: Elegoo Centauri Carbon 2 (keine aktive Kammerheizung), Snapmaker U1 (nicht fest geschlossen, nur Zubehör-Abdeckung, 300 °C, kein Filter), Anycubic Kobra S1 Max (Filter nur optional/extra, wenig unabhängige Tests), Flashforge Creator 5 Pro (nur 320 °C, Toolchanger-Spezialmaschine).

## Vergleichstabelle

| Modell | Max. Düse | Aktive Kammerheizung | Filter | TPU | Preis (EUR, Nov 2026) | Amazon.de |
|---|---|---|---|---|---|---|
| QIDI Plus 5 | 370 °C (Bimetall) | Ja, 550 W, bis 65 °C | G3 + H12 HEPA + Kokos-Aktivkohle | Specs: ja, QIDI empfiehlt 95A, unter 85A Qualität kann leiden | 749 (Combo 899), Amazon 637 laut Smartzone 19.11.2026 | Ja, gelistet (Smartzone) |
| QIDI Q2 | 370 °C | Ja, 280 W, bis 65 °C | G3 + H12 HEPA + Kokos-Aktivkohle | Specs: ja (Direktantrieb); Soft TPU <95A Probleme | 499 (Combo 649), Heise ab 449 | Ja, ca. 499 (Notebookcheck, TechRadar-Link) |
| QIDI Q2C | 370 °C | Nein (geschlossen, unbeheizt) | Optional | ja | 399 (Combo 549) | wie Q2 |
| QIDI Max4 | 370 °C | Ja, bis 65 °C (Test: 55 °C nach 12 min) | Partikel + Geruch, HEPA/Aktivkohle laut Hersteller | Nur Einzelspule, nicht über Box | 1.049 (Combo 1.199) | Marketplace 996,55 (Heise), Combo Amazon 1.062 (Smartzone) |
| QIDI Plus 4 | 370 °C | Ja, 65 °C | nur Aktivkohle | ja | eingestellt, nur Restbestand | Restbestand |
| Creality K2 Pro | 300 °C | Ja, bis 60 °C (gemessen 45 °C nur Heizung, 57 °C mit Bett nach 45 min) | 1 Luftreiniger (Aktivkohle), HEPA nicht belegt | Specs nicht belegt; CFS nicht TPU-tauglich | UVP 1.099 Combo, Marktpreis ca. 750-850 | Launch Amazon 1.149 (Notebookcheck), aktuell nicht verifiziert |
| Creality K2 Plus | 350 °C (Messing) | Ja, 60 °C | 2 Filter | nicht belegt | ca. 908-1.100 (Geizhals AT), Combo UVP 1.499 | nicht bei Geizhals-Liste, nicht belegt |
| Creality K2 (Combo) | 300 °C | Nein | Aktivkohle-Granulat | CFS nicht TPU | 359 / 499 Combo | nicht belegt |
| Elegoo Centauri Carbon 2 | 350 °C | Nein (passiv) | HEPA + Kokos-Aktivkohle laut EU-Seite, Filter laut Tests optional/extern | nicht belegt | 309-439, Combo ab ca. 359-399 | nicht belegt |
| Snapmaker U1 | 300 °C | Nein, nicht fest geschlossen (Abdeckung als Zubehör) | keiner genannt | ja ohne Zubehör | 749-899 | nicht belegt |
| Anycubic Kobra S1 Max (Combo) | 350 °C | Ja, bis 65 °C (Herstellerangabe, 10-15 min) | HEPA+Aktivkohle nur als EXTRA | Direktantrieb, weiches TPU langsam | EU-Shop 599 (statt 899) | nicht belegt |
| Flashforge Creator 5 Pro | 320 °C | Ja, 60-65 °C | Aktivkohle + einfacher Filter, HEPA13 laut Hersteller | dried TPU A95 ok, Laden schwierig | EU-Shop 829 (UVP 929/999) | Ja laut Notebookcheck (Basis), Preis nicht verifiziert |

## Pro Modell

### QIDI Plus 5 (empfohlen für größere Teile)
- Verkaufsstart 05.08.2026, Nachfolger des eingestellten Plus 4. Bauraum 320x320x300 mm. Kammerheizung 3. Gen. 550 W bis 65 °C. Düse 370 °C, Direktantrieb mit gehärteten Zahnrädern. Filter: G3-Vorfilter, H12-HEPA, Kokosnuss-Aktivkohle (QIDI: 99,5 % Staub/Partikel). QIDI nennt TPU 95A als empfohlen, unter 85A kann die Qualität leiden. Quelle: https://qidi3d.com/de/blogs/news/qidi-plus-5-vs-plus-4
- Test Smartzone: sehr gute Druckqualität, erste Schicht sauber, Überhänge >80° ohne Stützen (PLA/ABS), PETG nahezu ohne Stringing, ca. 14 Tage / 180-220 Druckstunden / 6 kg ohne technisch bedingte Fehldrucke. ABS: Vorheizen eliminierte Warping. Nachteile: PTFE-Schlauch schlägt gegen Glasdeckel, kleines 4"-Display, 875 W beim Aufheizen, 61 dB(A) bei geschlossener Tür, Tester empfehlen eher separaten Raum als Wohnzimmer. Preis 749 EUR beim Hersteller, bei Amazon 637 EUR (Stand 19.11.2026, 1-2 Tage Lieferung). https://www.smartzone.de/qidi-plus-5-3d-drucker-test/
- Notebookcheck: 749/899 EUR, 65 °C, 370 °C. https://www.notebookcheck.com/Qidi-Plus5-Neuer-geschlossener-3D-Drucker-mit-65-C-Bauraumheizung-und-370-C-Hotend.1360857.0.html
- Tom's Hardware Titel "QIDI Plus5 3D printer review: The best one yet" (Inhalt nicht abrufbar). https://tomshardware.com/3d-printing/qidi-plus5-3d-printer-review
- Lücke: nur wenige Langzeitberichte (Modell erst 3 Monate am Markt).

### QIDI Q2 (empfohlen als Preis-Leistungs-Variante, ohne Box)
- Bauraum 270x270x256, 370 °C, Bett 120 °C, Direktantrieb, PTC-Kammerheizung 2. Gen. bis 65 °C, 3-in-1 Filter (G3, H12 HEPA, Kokos-Aktivkohle). Materialien lt. Hersteller: PLA, ABS, ASA, PETG, TPU, PA, PC, CF/GF. https://qidi3d.com/de/pages/qidi-q2-techspecs
- TechRadar (28.09.2026): 4,5/5, "Recommended", 29/30 im Qualitätsbenchmark, mittlere XY-Maßabweichung 0,128 mm, 6 Monate ohne Probleme, ohne Nachkalibrierung der ersten Schicht; 499 USD bei Amazon. https://www.techradar.com/pro/qidi-q2-3d-printer-review
- Deskmodder: sauberer, detaillierter Druck; Slicer aktiviert Kammerheizung nicht automatisch (manuell setzen); Preise 499/649 EUR (Q2/Q2+Box), Q2C 399/549. Probleme eher bei der Box (Firmware, Fehlermeldungen). https://www.deskmodder.de/blog/2025/12/02/test-qidi-q2-ams-mit-ueberzeugender-ki-kamera/
- Heise Preisvergleich 19.11.2026: ab 449 EUR (3DJake), sonst 531-580 EUR. https://preisvergleich.heise.de/qidi-q2-a3639279.html?va=b
- Notebookcheck: Amazon-Verkauf für 499 EUR (laut Suchergebnis). https://www.notebookcheck.com/Q2-3D-Drucker-unterstuetzt-auch-Mehrfarbendruck-kommt-mit-Luftfilter-und-verspricht-perfekte-erste-Schicht.1129362.0.html
- Der Standard: "Der Drucker mit dem Nudeldefekt", Test mit starker Kritik an der QIDI Box (Volltext hinter Consent-Wand). https://www.derstandard.at/story/3000000308112/qidi-q2-im-test-der-drucker-mit-dem-nudeldefekt
- Bekannte Probleme (printer-hub, Stand 17.06.2026): Heat Creep durch 65 °C Kammer bei PLA, Soft-TPU (<95A) wird von den Zahnrädern plattgedrückt, Messing-/Düsenverschleiß bei CF, Sensorless-Homing, Firmware-Update 06/2026 mit Datenverlustwarnung. https://printer-hub.com/posts/qidi-q2-known-issues

### QIDI Max4 (X-Max 4 Nachfolger, groß)
- 390x390x340, 370 °C, 65 °C, 800 mm/s, 40-46 kg. Test Smartzone: Kammer 55 °C nach 12 min, sehr gute Qualität; Box ungeeignet für TPU (Einzelspulenhalter), 58-63 dB, Farbwechsel >3 min. Preise 1.049 EUR (Heise 19.11.2026: Amazon-Marketplace-Händler FEIDI 996,55 EUR), Combo bei Amazon 1.062 EUR. Für Wohnung zu groß/laut. https://www.smartzone.de/qidi-max4-combo-test/ , https://preisvergleich.heise.de/qidi-max4-744581222559-a3830719.html?va=b

### Creality K2 Pro / K2 Plus
- K2 Pro: aktiv 60 °C, 1 Luftfilter, Düse 300 °C. Messung Igorslab: nur 45 °C (Heizung allein, 30 min), 57 °C mit Bett 100 °C nach 45 min, Ziel 60 °C nicht erreicht; Druckqualität gut, leichtes Ghosting, 54,8 dB(A), 24 kg. UVP 1.099 EUR Combo (Marktpreis später ca. 750). https://www.igorslab.de/creality-k2-pro-combo-3d-drucker-im-test/ , https://www.deskmodder.de/blog/?p=204670
- Creality-Herstellerseite: K2 Plus 350 °C/60 °C/2 Filter; K2 Pro 300 °C/60 °C/1 Filter. https://www.creality.com/de/products/k2-series
- K2 Plus: Geizhals AT 19.11.2026 ab 908 EUR (Amazon nicht gelistet), Messing-Düse 350 °C, Luftfilter in der Kammerbelüftung. https://geizhals.at/creality-k2-plus-a3354996.html . GIGA: musste Gerät zerlegen/reparieren, "wie bei fast allen Creality-Geräten"; China-Gadgets Langzeittest: Extruder-/CFS-Verstopfungen. https://www.giga.de/tech/creality-k2-plus-combo-riesiger-3d-drucker-fuer-bastler-deutlich-guenstiger-zu-haben--01KBZ5S0FX527E17V43GER2D2R
- K2 (ohne Pro): keine aktive Kammerheizung (Notebookcheck). https://www.notebookcheck.com/3D-Drucker-mit-Multicolor-Support-Der-Creality-K2-Combo-druckt-gut-und-einfach.1339548.0.html
- K3 (4 Düsen, Toolchanger) erst als Vorankündigung, kein Test (Stand Sept. 2026, <900 EUR Ziel). https://store.creality.com/de/products/creality-k3-3d-printer
- Creative Bloq warnt vor Kauf des K2 Pro bei Amazon (Titel "don't buy it from Amazon", Grund nicht geprüft). https://www.creativebloq.com/3d/3d-printing/want-the-new-creality-k2-pro-3d-printer-dont-buy-it-from-amazon

### Elegoo Centauri Carbon 2
- 350 °C Düse, geschlossen, aber KEINE aktive Kammerheizung (Tom's Hardware). 256^3 mm, 439 EUR UVP, Combo unter 400 EUR. Filter lt. Elegoo EU HEPA + Aktivkohle, in Tests teils als optionales Zubehör geführt. Qualität sehr gut (GIGA: auf Niveau Bambu P2S), aber Spulenfiepen, offene Spulen, Kammer passiv. https://www.tomshardware.com/3d-printing/elegoo-centauri-carbon-2-review , https://www.giga.de/tech/elegoo-centauri-carbon-2-im-test-so-gut-ist-der-neue-3d-drucker-wirklich--01KFX77M0P3R6FEKX61W1T8QVT , https://all3dp.com/de/4/elegoo-centauri-carbon-2-kommt-fuer-439-e-auf-den-markt/
- Erster Centauri Carbon: Kammer nur ca. 31 °C beim Druck (Tom's Hardware, Vorgängermodell).

### Snapmaker U1
- 4 Werkzeugköpfe, 270^3 mm, 300 °C, Bett 100 °C. Laut Snapmaker KEIN festes Gehäuse, obere Abdeckung nur als Zubehör für ABS/ASA/PA/PC; CF/GF nur zusätzlich mit gehärteter Düse. Kein Filter, keine Kammerheizung. Preis 749-899 EUR (Snapmaker-Shop). https://www.snapmaker.com/de-DE/snapmaker-u1/specs , https://all3dp.com/de/4/snapmaker-u1-ab-10-april-im-handel-preis-steigt-auf-899-e/ . Tests positiv (Tempo, wenig Abfall), aber nicht passend zur Anforderung geschlossen/gefiltert.

### Anycubic Kobra S1 Max Combo
- 350x350x350, 350 °C, 65 °C aktiv (Herstellerangabe, 10-15 min). HEPA+Aktivkohle-Filter wird SEPARAT verkauft, Hersteller-Preis 599 EUR (statt 899). Review 3DTechValley: Erfolgsrate >95 % nach Kalibrierung, weiches TPU (85-90A) nur langsam, gehärtete Düse nicht ideal für weiches TPU. Unabhängige deutsche Tests fehlen. https://www.3dtechvalley.com/anycubic-kobra-s1-max-combo-review/amp/ , https://eu.anycubic.com/products/kobra-s1-max

### Flashforge Creator 5 Pro
- Toolchanger (4 Köpfe), 256^3, Düse 320 °C, Kammer 60 °C (Test) / 65 °C (Amazon-Listing), Filter Aktivkohle + einfach, HEPA13 laut Hersteller. Notebookcheck: "sehr gut", Qualität sehr sauber, aber Geruch trotz Filter, Spülen in die Kammer, getrocknetes TPU A95 nutzbar. EU-Shop 829 EUR (UVP 929), mydealz 689-758 EUR. Nur 320 °C und Spezialmaschine, daher nachrangig. https://www.notebookcheck.net/Four-print-heads-less-waste-Creator-5-Pro-tested.1364964.0.html , https://eu.flashforge.com/products/flashforge-creator-5-pro

## Empfehlung

1. QIDI Plus 5 (für Nutzer, der große/zuverlässige Allrounder-Qualität mit ABS/ASA/PC/PA-CF will und einen separaten, belüfteten Raum hat). Amazon-Preis laut Smartzone 637 EUR (19.11.2026).
2. QIDI Q2 ohne QIDI Box (für Wohnungsdrucker, kompakter, ab 449-499 EUR, bestes Preis-Leistungs-Verhältnis, bestes Zuverlässigkeitszeugnis in TechRadar-Langzeittest).
   - Beide erfüllen als einzige alle Hard-Kriterien: aktive 65 °C, H12 HEPA + Aktivkohle, 370 °C, Direktantrieb, TPU in den Specs, Amazon.de.
   - Einschränkung bei TPU 85A: Direktantrieb kann es laut Hersteller/Community nur eingeschränkt (95A empfohlen); für zuverlässiges 85A-TPU eher Bambu/Prusa prüfen (andere Teilberichte).
   - Einschränkung Marke/Ruf: QIDI ist solide, aber kleinerer Hersteller; Firmware-Updates und Box zeigen Kinderkrankheiten; Garantie 1 Jahr.
   - Auch mit HEPA: Hersteller/Tester betonen, der Filter ersetzt keine Raumbelüftung; Lautstärke 61 dB(A) (Plus 5).

BEST-PRACTICES-KANDIDATEN:
- Kammerheizung und Filter immer in Herstellerangaben UND unabhängigen Messungen prüfen: Creality K2 Pro erreichte statt 60 °C nur 45 °C (nur Heizung) bzw. 57 °C (mit Bett 45 min). https://www.igorslab.de/creality-k2-pro-combo-3d-drucker-im-test/
- Bei QIDI Q2/Plus 5: Kammerheizung wird vom Slicer nicht automatisch gesetzt, manuell einstellen, und für ABS vorheizen (erster Würfel Warping, nach Vorheizen keins). https://www.deskmodder.de/blog/2025/12/02/test-qidi-q2-ams-mit-ueberzeugender-ki-kamera/ , https://www.smartzone.de/qidi-plus-5-3d-drucker-test/
- Weiches TPU (<95A) und Multi-Material-Boxen/CFS vertragen sich nicht: über Einzelspulenhalter direkt in den Extruder füttern (QIDI Max4, Creality K2 CFS). https://www.smartzone.de/qidi-max4-combo-test/ , https://www.notebookcheck.com/3D-Drucker-mit-Multicolor-Support-Der-Creality-K2-Combo-druckt-gut-und-einfach.1339548.0.html
- Filter ersetzen keine Raumlüftung; Filter alle 3-6 Monate wechseln (Anycubic-Review); Drucker besser nicht im Wohnzimmer. https://www.smartzone.de/qidi-plus-5-3d-drucker-test/ , https://www.3dtechvalley.com/anycubic-kobra-s1-max-combo-review/amp/
- Amazon-Preise schwanken stark und Marketplace-Anbieter (z. B. FEIDI 3D TECH) sind kein Amazon-Direktverkauf, Verkäufer und Garantie prüfen. https://preisvergleich.heise.de/qidi-max4-744581222559-a3830719.html?va=b

BUG-KANDIDATEN:
- QIDI Q2 (und Plus 4/5 mit Kammerheizung): Heat Creep bei PLA in beheizter Kammer, Kammerheizung bei PLA/PETG aus bzw. Tür offen. https://printer-hub.com/posts/qidi-q2-known-issues
- QIDI Q2: Soft-TPU <95A klemmt im Extruder (Zahnräder quetschen), Firmware 06/2026 (V01.01.02.01) mit neuer UI, QIDI Link nicht mehr unterstützt, Datenverlustwarnung, Downgrade erzeugt Fehler. https://printer-hub.com/posts/qidi-q2-known-issues
- QIDI Box (Q2/Max4 Combo): Reibung im Filamentpfad, Verstopfungen/Fehlermeldungen, Farbwechsel 2-3,5 min, nicht TPU-tauglich, Tom's Hardware "skip the box". https://printer-hub.com/posts/qidi-q2-known-issues , https://www.smartzone.de/qidi-max4-combo-test/
- QIDI Plus 5: PTFE-Schlauch schlägt gegen Glasdeckel (Abhilfe: Isolierband), 875 W Heizleistung, Display 4". https://www.smartzone.de/qidi-plus-5-3d-drucker-test/
- QIDI Q2: Sensorless-Homing kann verfehlen, Early-Batch-Defekte (Hotend-Kabel, DOA) laut Trustpilot. https://printer-hub.com/posts/qidi-q2-known-issues
- Creality K2 Pro: Kammerheizung erreicht 60 °C nicht; glass door nur 135° öffnend, keine Türsensorik. https://www.igorslab.de/creality-k2-pro-combo-3d-drucker-im-test/
- Creality K2 Plus: Verstopfungen in Extruder/CFS (Langzeittest), Selbstreparatur nötig. https://www.giga.de/tech/creality-k2-plus-combo-riesiger-3d-drucker-fuer-bastler-deutlich-guenstiger-zu-haben--01KBZ5S0FX527E17V43GER2D2R
- Elegoo Centauri Carbon 2: Spulenfiepen, Lüfter lose beim Transport, klappernde PTFE-Schläuche, Software-Fragmentierung. https://www.giga.de/tech/elegoo-centauri-carbon-2-im-test-so-gut-ist-der-neue-3d-drucker-wirklich--01KFX77M0P3R6FEKX61W1T8QVT
- Flashforge Creator 5 Pro: hoher Spülabfall (36 g für 16-g-Benchy), spült in die Kammer, Geruch trotz Filter. https://www.notebookcheck.net/Four-print-heads-less-waste-Creator-5-Pro-tested.1364964.0.html
