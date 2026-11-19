# R11: Prusa CORE One / CORE One+ - Langzeit-Haltbarkeit (Stand 19.11.2026)

Hinweis zur Quellenlage: Reddit (r/prusa3d) war per WebSearch/WebFetch nicht erreichbar; es kamen keine Reddit-Treffer. Die Owner-Erfahrungen stammen aus dem Prusa-Forum, Reviews und Herstellerseiten. Eine echte, dokumentierte Langzeitstudie (Verschleißprotokoll über Tausende Stunden) wurde nicht gefunden. Das CORE One ist erst seit rund 1,5 Jahren am Markt, Daten jenseits ~2.500 Std sind nur Einzelfälle.

## 1. Erfahrungen nach hunderten bis tausenden Druckstunden

- Prusa-Forum, Thread "changing belts on coreone" (Belt-Verschleiß): ein Nutzer mit ältestem CORE One bei ca. 600 Std berichtet Riemenverschleiß und kleine First-Layer-Abweichungen; ein anderer 1.450 Std ohne merklichen Riemenverschleiß; ein dritter ältestes Gerät ca. 2.500 Std, weitere über 1.500 Std, ohne Probleme.
  https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/changing-belts-on-coreone/
- Tom's Hardware (CORE One L+ Review): eigenes Gerät über 1.000 Std harter Nutzung, inkl. abrasiver Filamente (Carbon/Glasfaser), ohne Probleme (Einzelerfahrung eines Testers, kein Verschleißprotokoll).
  https://www.tomshardware.com/3d-printing/prusa-core-one-l-plus-review
- Prusa-Forum, Forenanalyse vom 21.04.2026 (ca. 1.094 Support-Threads, 11.381 Beiträge): größte Problemkategorie Motion/Gantry/Riemen mit 22,6 %, dann Firmware/Konnektivität 13,5 %, Extruder/Hotend 9,8 %. Wichtig: Anteil an Forenaktivität, keine Ausfallrate (kein Verkaufs-Nenner); überwiegend Aufbau-/Frühphasenprobleme, kein Verschleiß-über-Zeit analysiert. Genannte Einzelthemen: festsitzende Riemenspanner-Schrauben, lose Gummifüße, lockere Türgriffe, Abstürze bei langen Drucken (Aufladung auf Riemen vermutet).
  https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/a-summary-of-core-one-support-questions-on-these-forums/
- Prusa-Forum, "Core One owners regretting their purchase" (Einzelberichte): Thermistor-Ausfälle (Prusa schickte schnell Ersatz), Riemenspannung driftet mit Temperatur (Layer-Shifts bei langen ASA-Drucken bei 55 °C Kammer), IO-/Mainboard-Kommunikationsverlust nach ca. 12 Std (Mainboard-Tausch löste es), Standfüße ungeeignet. Support reagierte bei Elektronik schnell.
  https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/core-one-owners-regretting-their-purchase/paged/5
- Die Gen-2-Überarbeitung (GT1.5-Riemen, neue Heizbett-Aufhängung/Expansion Joints, Düsenwischer, robustere Top-Vent-Mechanik) adressiert genau die Komponenten, die im Forum am häufigsten auffielen (Riemen/Motion, Heizbett-Mount, Top-Vent).
  https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/

## 2. Typische Verschleißteile

- Düse: Prusa nennt für Messingdüsen "2400 Stunden Druck" (laut iFixit-Guide, Nutzerbeitrag); iFixit-Praxisempfehlung: Messing alle 3-6 Monate, gehärteter Stahl bis 12 Monate, Rubin mehrere Jahre. Wechsel "Easy", dauert Sekunden bis Minuten. Prusa-Forum-Schätzung: Messing 7-9 Monate bei 50 % Auslastung. Abrasive Filamente verschleißen die Standarddüse schnell.
  https://www.ifixit.com/Guide/Prusa+CORE+One+Nozzle+Replacement/182464
- Druckblech (PEI-Sheet) und Bowden-/PTFE-Schläuche gelten als Verbrauchsmaterial; Prusa nennt für Düse und Sheet Lebensdauer von "hunderten Druckstunden", Sheet ist doppelseitig.
  https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/core-one-periodic-maintenance
  https://www.creativebloq.com/reviews/prusa-core-one-review-built-to-last-and-grow-with-you
- Riemen: ab ~600 Std erste Abnutzung gemeldet, andere >1.450-2.500 Std ohne Auffälligkeit (siehe oben). Riemenspannung per Kreistest prüfen.
- Lager: Y-Achse Linearlager (LM8UU), CoreXY-Lager (LM10LUU) und X-Linearschiene sind wartbar; Schiene braucht eigenes Schmierset, kein Prusa-Lubricant.
- Lüfter (Hotend-/Blower-Lüfter), Heizbett-Thermistor, Hotend-Heatsink: im offiziellen Maintenance Kit enthalten, d.h. als erwartbare Wartungs-/Verschleißteile eingestuft.
  https://www.prusa3d.com/product/prusa-core-one-maintenance-kit
- Elektronik: Steckverbinder alle 600-800 Std prüfen/nachstecken (xBuddy, Loveboard, Heizbett, PSU, xLCD).

## 3. Wartungsintervalle laut Prusa (offizielle Hilfe)

Quelle: https://help.prusa3d.com/article/regular-printer-maintenance-core-one_829710
- Vor jedem Druck: Schmutz entfernen, Düse/Heizblock bei 250 °C reinigen (Messingbürste).
- Alle ca. 200 Druckstunden: X-Linearschiene prüfen/reinigen (trocken); Schiene alle 3-6 Monate mit Linearschienen-Set schmieren; Y-Glattstangen und Linearlager reinigen/schmieren (Prusa Lubricant); Riemenspannung (Kreistest); Lüfter reinigen; Extruder-Zahnrad/Idler säubern; Filamentsensor.
- Alle 600-800 Std: Elektronik-Steckverbinder prüfen.
- Z-Gewindespindeln: ca. 500 Std nachfetten (laut Wartungsüberblick/Forum-Hinweis; im Artikel selbst fehlt ein Abschnitt, mehrere Kommentatoren monieren die Lücke).
- Nozzle-Cold-Pull (INDX-Seite): alle 200 Std oder bei Bedarf.
  https://help.prusa3d.com/en/article/regular-printer-maintenance-core-one-indx_1116911 (laut Suchtreffer)
- Kein offizielles Intervall für Düsen-, Heizbett- oder Riemenwechsel im Artikel.

## 4. Ersatzteilpreise und -verfügbarkeit

- CORE One Maintenance Kit: 199 EUR inkl. MwSt. (167,23 EUR netto), Lieferzeit 4-7 Werktage; enthält u.a. LM8UU-/LM10LUU-Lager, Schmiermittel, Heizbett-Thermistor-Set, Hotend- und Blower-Lüfter, kompletten Hotend inkl. 0,4-mm-Düse, Heatsink mit Loadcell/Filamentsensor, Schrauben/Nieten, PTFE-Schlauch. Nicht kompatibel mit CORE One+ (Gen 2) und CORE One L+.
  https://www.prusa3d.com/product/prusa-core-one-maintenance-kit
- Einzelteile (Drittanbieter, US): Heizbett-Thermistor-Set 6,99 USD, CoreXY-Linearschiene 93,99 USD (Preisangabe aus Suchzusammenfassung, nicht verifiziert). Offizielle Einzelpreise für Heizbett-Tile, Nextruder, Riemen: nicht gefunden.
  https://www.printedsolid.com/collections/all-spare-parts?page=2
- Hinweis aus Suchzusammenfassung: Lubrication-Kit-Preise laut Forennutzer "considerably" gestiegen.
  https://forum.prusa3d.com/forum/prusa-core-one-general-discussion-announcements-and-releases/core-one-periodic-maintenance
- Prusa verspricht für XL, CORE One L und CORE One weiterhin Firmware, Slicer-Verbesserungen, Profile, Dokumentation, Ersatzteile und Support.
  https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/

## 5. Reparierbarkeit

- Tom's Hardware (CORE One Review): auf Wartung ausgelegt, nichts verschweißt oder verklebt, Deckel leicht abnehmbar, Prusa legt Ersatz-Nieten bei.
  https://www.tomshardware.com/3d-printing/prusa-core-one-review
- Reparaturanleitungen offiziell auf help.prusa3d.com; iFixit-Guide für Düsenwechsel existiert.
  https://www.ifixit.com/Guide/Prusa+CORE+One+Nozzle+Replacement/182464
- 3DPrint.com: erschwingliche, zugängliche Teile beheben seltene Probleme (z. B. ASA-Druckfehler).
  https://3dprint.com/316927/prusa-core-one-innovating-or-catching-up/
- Einschränkung: Getriebe des Nextruders nicht öffnen ohne PG-Assembly-Adapter (liegt dem Bausatz bei).

## 6. Prusas Upgrade-Politik

- CORE One (1. Release) -> CORE One+ -> CORE One+ (Gen 2): Upgrade-Kits, nacheinander installierbar. Offizielle Pfadübersicht: https://help.prusa3d.com/article/core-one-upgrade-paths_1253002
- CORE One -> CORE One+ Kit: 9,99 EUR inkl. MwSt. (gedruckte Teile, Aufkleber, Befestigungen; automatische Top-Vent, Flex-Einfädelung, Bajonett-Spulenhalter). https://www.prusa3d.com/product/prusa-core-one-to-core-one-upgrade/ (Lieferzeit dort 3-4 Wochen)
- CORE One+ -> Gen 2 Kit: ca. 54,53 EUR inkl. MwSt. bei 3DJake (Händler, keine offizielle Prusa-Preisangabe gefunden); Inhalt: GT1.5-Riemen/Pulley, Expansion Joints Gen 2, Düsenwischer, Nylon-Nieten; kompatibel mit CORE One und CORE One+. https://www.3djake.com/prusa/core-one-to-core-one-gen-2-upgrade-kit-1
- Gen 2 kam im August 2026 ohne Preiserhöhung, Bestandsbesteller wurden automatisch auf Gen 2 hochgezogen; Prusa: Besitzer müssen ein funktionierendes Gerät nicht ersetzen. https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/ und https://www.tomshardware.com/3d-printing/prusa-research-xl-core-one-and-core-one-l-all-to-receive-second-generation-upgrades-all-new-orders-get-updated-model-for-free
- INDX-Toolchanger als eigenes Modul auf jede CORE-One-Generation nachrüstbar (kein Gen 2 nötig). https://help.prusa3d.com/article/core-one-upgrade-paths_1253002
- MK4S -> CORE One+: Konversionskit (Motoren und Elektronik werden weiterverwendet, schwarzes Netzteil nötig; Restteile reichen nicht für einen zweiten Drucker). Preise je Quelle: ca. 449 USD (Hackaday-Kommentar, unbestätigt), 539 USD (US-Händler), 461,98 EUR (EU-Händler); Aufbau ca. 8 Std. https://hackaday.com/2025/08/30/does-it-make-sense-to-upgrade-a-prusa-mk4s-to-a-core-one , https://top3dshop.com/product/original-prusa-mk4s-to-prusa-core-one-conversion-3d-printer-kit
- MK3 -> MK4: Upgrade-Kits ("bis auf Weiteres" produziert, laut Chat-Auskunft, Forum, Stand 2024). Direkter MK3S+ -> CORE One Pfad: nicht gefunden, nur zweistufig (MK3S+ -> MK4S -> CORE One+); Preise der MK3S->MK4(S)-Kits laut Shop-Snippets 249-579 USD, nicht eindeutig zugeordnet. https://forum.prusa3d.com/forum/english-forum-general-discussion-announcements-and-releases/how-long-will-mk3-s-mk4-upgrade-kits-be-available
- Prusa beschreibt ältere Open-Frame-Drucker, die Schritt für Schritt (Elektronik, Extruder, bis zur CORE-One+-Konversion) aufgerüstet werden. https://blog.prusa3d.com/better-prints-easier-use-prusa-xl-core-one-l-and-core-one-gen-2-our-big-product-update_137539/

## 7. Prusa-Druckfarm als Haltbarkeitsbeleg

- Prusa betreibt eigene Farm mit mehr als 700 Druckern, 24/7, über 1 Mio. Produktionsteile pro Monat und über 1 Mio. Druckstunden pro Quartal; nutzt dieselben Drucker, Materialien, Software und Profile, die Kunden bekommen. Die Seite nennt keine Uptime-/Ausfallzahlen und keine Modelle der internen Farm. https://www.prusa3d.com/cs/p/prusa-farm/index.md
- Aktuelle Prusa-Angabe 700+ (Preisanfrage-Seite). Ältere Angaben: 500 (früher Blog), 600 (2021 und Pro-Blog "From six, we grew to 600"). https://pro.prusa3d.com/?p=15448 , https://blog.prusa3d.com/a-quick-look-to-our-printing-farm_7474/
- Berichte nennen als Mehrheit der Farm-Drucker den MK3S (Alt-Modell). Ein MK4/CORE-One-Anteil ist nicht belegt. https://www.open-electronics.org/hundreds-of-3d-printers-in-one-room-its-the-new-prusa-printing-farm
- Aussagekraft: Beleg, dass die Plattform im Dauerbetrieb tragbar ist (MK3S war weit über Jahre im 24/7-Einsatz), aber nicht direkt für das CORE One; Prusa wartet die Farm intensiv (Prusa Connect, Bettwechsel, Wartung), das ist kein Heimnutzer-Szenario.

## 8. Wie lange wurden alte Prusa-Modelle unterstützt (MK3-Präzedenzfall)

- Josef Prusa (Sept. 2024, laut Fabbaloo): MK3 und MK2.5 werden sieben Jahre nach Erstveröffentlichung weiter mit Firmware versorgt. https://www.fabbaloo.com/news/prusa-research-releases-firmware-updates-for-7-year-old-3d-printers
- Firmware 3.14.1 für MK3/MK3S/MK3S+/MK2.5(S) veröffentlicht 28.11.2024 (GitHub). https://github.com/prusa3d/Prusa-Firmware/releases/tag/v3.14.1
- Produktion MK3S+ (Komplettgeräte und Bausätze) endete Ende Februar 2024, ca. 4 Jahre nach Marktstart; Upgrade-Kits auf MK3.5/3.9/4 liefen weiter. https://forum.prusa3d.com/forum/english-forum-general-discussion-announcements-and-releases/how-long-will-mk3-s-mk4-upgrade-kits-be-available
- Einschränkung: Nov. 2024 meldete ein Forennutzer (nur sekundär, Weitergabe von Support-Aussagen), die komplette MK3S+-Hotend-Baugruppe sei ausverkauft und komme "most likely" nicht zurück, der Support für das Modell "lasse nach". Das ist eine inoffizielle Aussage zur Teileverfügbarkeit. https://forum.prusa3d.com/forum/original-prusa-i3-mk3s-mk3-hardware-firmware-and-software-help/mk3s-life-cycle
- Ein offizielles Support-Ende (EOL) für MK3S+ wurde nicht gefunden. Aktuelle (2026) Aussagen zu MK3S+-Ersatzteilen: nicht gefunden.

## 9. Fazit: Wie langlebig ist das Gerät realistisch?

(Eigene Einschätzung auf Basis der oben belegten Quellen; Belege dünn, da Gerät erst seit ~2025 verbreitet.)
- Mechanisch/strukturell: stabiler Rahmen, Standardteile und Wartbarkeit sprechen für 5+ Jahre Nutzbarkeit bei Hobby-Nutzung. Einzelne Geräte laufen belegt bei 1.500-2.500 Std ohne Auffälligkeit.
- Verschleißteile (Düse, Druckblech, Riemen, Lüfter, Thermistor, Linearlager) sind günstig bzw. im 199-EUR-Maintenance-Kit gebündelt und gehören eingeplant; typische Wechsel: Düse 3-12 Monate je nach Material, Riemen/Lager eher nach 600+ Std prüfen.
- Schwachpunkte der frühen Charge (Riemenspannung, Heizbett-Mount, Top-Vent, Aufladung der Riemen) wurden mit CORE One+/Gen 2 adressiert; Upgrade-Kits sind günstig (ca. 10 EUR bzw. ~55 EUR), ältere Geräte werden also gezielt nachgezogen, nicht abgehängt.
- Support-Präzedenzfall MK3: ca. 7 Jahre Firmware, Upgrade-Kits für neuere Modelle, aber Teilengpässe (Hotend) gegen Ende des Lebenszyklus. Realistischer Horizont: Firmware/Support-Erwartung 5-7 Jahre, Teileversorgung wahrscheinlich ähnlich, aber nicht garantiert.
- Unsicherheiten: keine echten Langzeitdaten über 3.000 Std; Forenanalyse ist verzerrt (nur Problemfälle); Reddit nicht ausgewertet; Gen-2-Kit-Preis nur von Händler 3DJake; offizielle Preise einzelner Ersatzteile nicht gefunden.

BEST-PRACTICES-KANDIDATEN:
- Wartungsplan CORE One: alle ~200 Std Riemenspannung (Kreistest), Y-Stangen/Lager schmieren, X-Linearschiene alle 3-6 Monate nur mit Linearschienen-Set (nie Prusa Lubricant), Lüfter reinigen, Steckverbinder alle 600-800 Std nachstecken, Z-Spindeln ca. 500 Std nachfetten.
- Verbrauchsteile einplanen/bevorraten: Düse (Messing 3-6 Monate, abrasiv nur gehärtet/Rubin), Druckblech, Thermistor, Lüfter, Bowden-/PTFE-Schlauch; Maintenance Kit 199 EUR (nicht kompatibel mit CORE One+ Gen 2 und L+, Kompatibilität vor Kauf prüfen).
- Beim Kauf direkt CORE One+ (Gen 2) wählen; Bestandsgeräte per Upgrade-Kit (CORE One -> One+ ca. 10 EUR, One+ -> Gen 2 ca. 55 EUR, in dieser Reihenfolge) nachziehen.

BUG-KANDIDATEN:
- CORE One (1. Release): Riemenspannung driftet bei hoher Kammertemperatur (55 °C, ASA) -> Layer-Shifts; festsitzende Riemenspanner-Schrauben beschädigen Idler-Halter (Forum).
- CORE One: Abstürze/Neustarts bei langen Drucken, vermutet durch statische Aufladung der Riemen (Erdung mit Kabel als Workaround im Forum); IO-/Mainboard-Kommunikationsverlust (Mainboard-Tausch löste es); Thermistor-Ausfälle.
- Kit-Aufbau: Riemen wirken zu kurz/ziehen sich beim Spannen heraus, Gantry-Pulley-Orientierung falsch (Montagefehler).
