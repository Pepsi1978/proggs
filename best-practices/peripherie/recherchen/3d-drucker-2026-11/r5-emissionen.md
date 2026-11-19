# R5: Emissionen von FDM-Druckern und Wirksamkeit eingebauter Filter (Stand 19.11.2026)

Hinweis zur Belegqualität: A = unabhängige Studie / Behörde, B = Herstellerangabe oder Händler, C = Forum/Anekdote. Wo ich nur eine Zusammenfassung (Abstract/Nachrichtenseite) gesehen habe, steht das dabei. Nicht gefunden: eine unabhängige Labormessung der eingebauten Filter von Bambu H2D/X1/P1S, Prusa Core One, Qidi oder Creality K2 (siehe "Lücken").

## 1. Kurzfazit für Laien

- Beim Schmelzen von Kunststoff entstehen zwei Dinge: ultrafeine Partikel (UFP, winzige Teilchen, die tief in die Lunge gelangen) und Gase (VOC). Je heißer die Düse, desto mehr.
- PLA ist in allen Studien am unproblematischsten, PETG meist ähnlich niedrig (nicht in allen Studien, siehe 2.4). ABS/ASA (Styrol), Nylon (Caprolactam) und PC sind die Problemmaterialien.
- Ein HEPA-Filter fängt nur Partikel, kein Gas. Aktivkohle fängt Gase (VOC, Geruch), aber nur bis sie gesättigt ist, und sie fängt keine ultrafeinen Partikel.
- Eingebaute Umluftfilter helfen nach Herstellerangaben und nach den vorhandenen Fremdmessungen an Nachrüst-Systemen deutlich, machen aber ein schlecht belüftetes Zimmer nicht automatisch sicher. Eine unabhängige Messung der Serienfilter fehlt.
- Praktisch für eine Wohnung: PLA/PETG drucken, Drucker geschlossen (Gehäuse), nicht im Schlafzimmer, nach dem Druck lüften, Filter regelmäßig wechseln.

## 2. Emissionen je Material

### 2.1 Grundaussagen (Fraunhofer WKI)
- UFP entstehen beim Schmelzen, höhere Drucktemperatur erhöht meist die Rate. Styrol ist typisch für ABS, Lactid für PLA, Caprolactam für Polyamid. PLA und PETG haben das geringste Emissionspotenzial, danach ABS und Polyamid. Empfehlung: Raum gut lüften, HEPA-/Aktivkohle-Abdeckung kann Emissionen verringern, emissionsarme Filamente nutzen, niedrigste mögliche Drucktemperatur. Schmierstoffe in der Mechanik setzen in der Aufheizphase Organocyclosiloxane frei. (A, Fraunhofer WKI Indoor Air Quality Portal) https://www.iaqip.wki.fraunhofer.de/en/all_topics/3d_printer.html
- WKI-Kammerstudie (Gu, Wensing, Uhde, Salthammer 2019): Partikel-Emissionsraten von 2,0e9 (PETG-basiertes Filament "GLASS") bis 1,7e11 pro Minute (ASA), Verdampfung ab ca. 150 °C; die Einheit der VOC-Raten ist in Sekundärquellen widersprüchlich (mg/min vs. µg/min). Nur über Sekundärquellen gesehen (Publica-Eintrag laut Suchergebnis). https://publica.fraunhofer.de/handle/publica/255593 und https://3dadept.com/how-far-3d-printer-emissions-are-problematic/
- WKI-Teil "Filterabdeckung und Luftreiniger" (Salthammer 2019) existiert; ich konnte den Inhalt nicht abrufen (ResearchGate 403). https://www.researchgate.net/publication/334403178_Emission_Control_of_Desktop_3D_Printing_-_the_Effects_of_Filter_Cover_and_Air_Purifier

### 2.2 Groß angelegte Auswertung (Zhang & Black, Environment International, Dez. 2023)
- 447 Partikelläufe, 58 VOC-Läufe, 14 FFF-Drucker von 6 Herstellern, 45 Filamente. ABS und HIPS sind das größte Problem bei Partikeln und VOC, Nylon bei Partikeln und Caprolactam, ASA mit erheblichen gefährlichen VOC. PLA insgesamt niedrig mit einzelnen Ausreißern. Formaldehyd, Styrol, Acetaldehyd bei 80 % der Filamente nachgewiesen. Bei ABS steigen Partikel mit Druckgeschwindigkeit und Temperatur. Empfehlung der Autoren/Berichts: in großen Räumen drucken, gut belüften, stark emittierende Materialien meiden. (A, Zusammenfassung über Nachrichtenseite und Abstract) https://3dprintingindustry.com/news/ciri-research-raises-health-concerns-over-3d-printing-emissions-226978/ und https://doaj.org/article/da9bb9795f3c48419a0e3987a6d6a366
- Hinweis: Die Zuordnung zu "UL Chemical Insights" konnte ich nicht belegen; die Studie stammt von Zhang/Black (CIRI). UL-Norm 2904 (Kammerprüfung) taucht in der Schulstudie (2.5) auf.

### 2.3 Azimi et al. 2016 (Environmental Science & Technology 50, 1260-1268)
- 5 Desktop-Drucker, bis zu 9 Filamente in Kammer. Wichtigste VOC: Caprolactam aus Nylon-Filamenten, Styrol aus ABS und HIPS, Lactid aus PLA. Vorsicht bei schlecht belüfteten Räumen ohne Filtration. (A, Metadaten/Zusammenfassung via Europe PMC) https://pubmed.ncbi.nlm.nih.gov/26741485/
- Folgemodellierung (Azimi, Fazli, Stephens, J. Industrial Ecology 2017, Volltext gelesen): Caprolactam nahe einem Drucker mit Nylon kann die kalifornischen akuten und chronischen Richtwerte (REL) überschreiten; UFP bis 80.000 Partikel/cm3 in Nähe (ABS, PC); Filamente mit niedrigsten Emissionen (z. B. PLA) etwa unter 100 Partikel/cm3 in Druckernähe. Wirksamste Gegenmaßnahmen: starke Punktabsaugung und Betrieb in dichtem Gehäuse mit hocheffizienter Gas- und Partikelfiltration. In 3 bis 18 m Entfernung kaum Wirkung mit den schwächsten Emittenten. (A) https://stacks.cdc.gov/view/cdc/207601/cdc_207601_DS1.pdf

### 2.4 PETG: uneinheitlich
- Fraunhofer: PETG gering. (A) siehe 2.1
- Toxics 2023 (Fang et al., ein Drucker, 220 °C für alle): Spitzen ABS 2,57 Mio./cm3, PETG 1,88 Mio./cm3, TPU 0,54 Mio./cm3; PETG-Mittelwert/Median ähnlich hoch wie ABS; Zytotoxizität bei ABS am höchsten, PETG nach 45 min signifikant. Einschränkung: einzelner Drucker, feste Temperatur, kein PLA im Test. (A) https://pmc.ncbi.nlm.nih.gov/articles/PMC10536656/
- Alveo3D (Filterhersteller) zählt PETG zu "hochemittierenden" Filamenten. (B) https://www.alveo3d.com/en/product/kit-bambu-lab-activated-carbon-hepa13-air-filter/
- Fazit: PETG ist wahrscheinlich besser als ABS, aber nicht so sicher wie PLA; Aussage "PETG unbedenklich" ist zu pauschal.

### 2.5 ABS vs. PLA in Zahlen
- Meta-Analyse (16 Arbeiten): Mittelwert ca. 300.980 Partikel/cm3 bei ABS gegenüber ca. 65.482 bei PLA; Methoden sehr uneinheitlich. (A, Abstract) https://pmc.ncbi.nlm.nih.gov/articles/PMC8350970
- Büromessungen: ca. 2e10 Partikel/min PLA, ca. 1,9e11 ABS (Stephens et al. 2013), laut Sekundärquelle ca. 10-fach. (A, sekundär) https://bambuhub.net/articles/printer-air-quality
- Wohnzimmer-Messung Creality Ender-3 (Khaki et al., Sensors 2021): ABS deutlich mehr als PLA; weißes PLA ohne messbaren Anstieg; Emissionsrate je Markenfilament bis ca. 150-fach unterschiedlich; Spitzen beim Aufheizen und erster Schicht; ohne Lüften sank die Belastung erst nach 30 min bis 4 h auf Ausgangsniveau; höhere Lüfterstärke (PLA) und höherer Infill senken Emission. (A) https://pmc.ncbi.nlm.nih.gov/articles/PMC8125858/
- UBA/BAM (TEXTE 217/2020): Bei 14 ABS-Filamenten von 3 Herstellern auf zwei FFF-Druckern praktisch nur während der Aufheizphase Emissionen, Median-Partikelgröße ca. 50 nm; Streuung zwischen Filamentmarken signifikant; Prüfverfahren des Blauen Engels für Laserdrucker (DE-UZ 205 Anhang S-M) grundsätzlich auch auf FFF-Drucker anwendbar. Einer der beiden Drucker hatte eine Filterkassette (HEPA mit Aktivkohle) im Deckel, aber nicht gasdichtes Gehäuse (Spalten). Der Bericht gibt hier keine Filterwirkung an. (A, Volltext gelesen) https://www.umweltbundesamt.de/system/files/medien/5750/publikationen/2020_11_19_texte_217_2020_ultrafeinstaubemissionen_drucker.pdf
- Schulstudie (Building and Environment, CIRI/Khaos): Gesamtpartikelzahl stieg beim Drucken um das 1- bis 4-Fache, TVOC verdoppelte sich teils, sank nach Druckende meist rasch; PM2.5/PM10 unter EPA/WHO-Grenzen; Drucker waren etwa ein Drittel bis über die Hälfte der VOC im Raum; Gesundheitsrisiko bei 1 bis 2 zertifizierten emissionsarmen Druckern wahrscheinlich gering; Filterwechsel der Luftreiniger wurden nicht immer geplant. (A, Nachrichtenzusammenfassung) https://3dprintingindustry.com/news/3d-printing-and-indoor-air-quality-what-scientists-discovered-in-schools-245206/

## 3. Behörden, Blauer Engel, Normen

- Blauer Engel RAL-UZ/DE-UZ 205 gilt für Bürogeräte mit Druckfunktion (Drucker/Multifunktionsgeräte). Ich habe keine eigene Kategorie für 3D-Drucker gefunden. UBA/BAM hat nur geprüft, ob das Verfahren auf FFF-Drucker übertragbar ist (ja, grundsätzlich). Konsequenz: Es gibt kein "Blauer Engel"-Siegel für 3D-Drucker, auf das man sich beim Kauf stützen könnte. https://www.umweltbundesamt.de/publikationen/weiterentwicklung-des-pruefwertes-fuer und Vergabegrundlage https://produktinfo.blauer-engel.de/uploads/criteriafile/de/DE-UZ%20205-201701-de%20Kriterien-2020-07-17.pdf
- DGUV/IFA (Beisser et al., Gefahrstoffe 2020, Teil 1): Ein FDM-Tischdrucker, 7 Filamente in der Prüfkammer; je höher die nötige Verarbeitungstemperatur, desto mehr Partikel. IFA: Emissionskonzentrationen der Tischgeräte (2018) sehr deutlich unter Arbeitsplatzgrenzwerten. Hinweis: Arbeitsplatzgrenzwerte gelten für Gesunde am Arbeitsplatz, nicht als Wohnraum-Garantie. https://dguv.de/ifa/fachinfos/arbeiten-4-0/neue-technologien-stoffe/3d-drucker/index.jsp , Studie https://www.sifa.dguv.de/medien/ifa/de/pub/grl/pdf/grl_2020_002.pdf (Inhalt nur über Suchzusammenfassung)
- UBA: Seite nennt Feinstaub, VOC und Nanopartikel in Innenräumen als Belastung bei Desktop-Druckern im Privatgebrauch; keine Verhaltenstipps. https://www.umweltbundesamt.de/themen/umwelt-gesundheitswirkungen-des-3d-drucks
- BfR: Nur ein Tagungsbeitrag einer BfR-Autorin zur UBA-Innenraumkonferenz 2024 (ausdrücklich nicht UBA-Position): VOC und respirable Nanopartikel, offene Plattformen problematisch, mehr Forschung nötig. https://www.umweltbundesamt.de/irl2024-a43 (nur Suchzusammenfassung gesehen)
- NIOSH (USA) "Approaches to Safe 3D Printing" (2024-103): Emissionen nahe der Quelle erfassen, Zeit in Druckernähe minimieren, Hierarchie der Schutzmaßnahmen (Absaugung/Gehäuse mit Abluft nach draußen). Volltext nicht abrufbar, Inhalt über Engadget/NIOSH-Blog. https://cdc.gov/niosh/docs/2024-103/default.html und https://blogs.cdc.gov/niosh-science-blog/?p=11203

## 4. Wirksamkeit von HEPA und Aktivkohle (Messungen, meist nicht Serien-Drucker)

- NIOSH/Dunn et al.: Absaugung direkt am Extruder und vollständiges Gehäuse waren am wirksamsten; MakerBot-Versuch mit Extruder-Haube 98 % weniger UFP; Büroversuch mit 20 Druckern: ohne Maßnahmen bis ca. 20.000 Partikel/cm3, mit Maßnahmen auf Hintergrundniveau (unter 1.000). Teilgehäuse schnitt schlecht ab, weil interne Lüfter Partikel am Filter vorbeileiten. (A) https://stacks.cdc.gov/view/cdc/258855 und https://stacks.cdc.gov/view/cdc/110293/cdc_110293_DS1.pdf
- Rapid Prototyping Journal 2025: Zwei FFF-Drucker, einfache Maßnahmen; beste Maßnahme mindestens 98,35 %; zwei Gehäuse nur 67,3 bis 94,97 % je nach Gehäuse/Drucker. Heißt: Gehäuse allein ist sehr unterschiedlich gut. (A, Abstract) https://www.emerald.com/rpj/article/doi/10.1108/RPJ-05-2025-0182/
- Workplace-Studie (Desktop-Drucker, selbstgebautes belüftetes Gehäuse mit HEPA + 3,6 kg Aktivkohle): Partikel -98,4 % (innen) bzw. -99,7 % (außen), TVOC aber nur -53,2 % (außen) bzw. -69,5 % (innen). Autoren: Aktivkohle weniger wirksam als HEPA bei Partikeln, Aktivkohle nach Gewicht, nicht nach Effizienz gewählt. "Vorläufig", nur ein Aufbau. (A) https://pmc.ncbi.nlm.nih.gov/articles/PMC6889885
- Fazit: HEPA ist sehr wirksam gegen Partikel (>98 %), Aktivkohle senkt VOC in realen Aufbauten nur um etwa 50 bis 70 %. Styrol/Caprolactam/Formaldehyd werden nicht vollständig entfernt, vor allem nicht, wenn die Kohle gesättigt ist.
- Illinois-Institut (zitiert via Händlerseite): geschlossenes Gehäuse -74 % UFP, mit HEPA -91 % (sekundär, nicht geprüft). https://www.tiertime.com/research-shows-up-box-a-safer-3d-printer-for-indoor-use/ (B)
- Alveo3D/Prusa-XL-Enclosure-Test (Herstellerpartner): Gehäuse reduziert bei ASA und PC-Carbonfaser Nanopartikel deutlich; bei PLA kaum Effekt; langes Nachlüften nach dem Druck wichtig; Zertifikat 94 bis 97 % Filtereffizienz für 7 bis 5000 nm. (B) https://www.alveo3d.com/en/prusa-research-alveo3d-partnership/
- Sachlage "Luftreiniger im Raum": Ein Forennutzer weist darauf hin, dass ein normaler Raum-Luftreiniger Partikel erwischt, aber kein Styrol. (C) https://forum.bambulab.com/t/mitigation-for-voc-and-nano-particles-polluting-indoor-air/2285

## 5. Eingebaute Filter konkreter Geräte

### Bambu Lab
Nach BambuHub (Fan-/Informationsseite, Bambu-Tabelle zitiert): X1E, X2D, H2S, H2D, H2C, H2D Pro: G3-Vorfilter + H12-HEPA + Kokosnuss-Aktivkohle; P1S: nur Aktivkohle; X1 Carbon: nur Aktivkohle (HEPA "None"); P2S: nur Aktivkohle (Bambu führt Partikelfilterung als unterstützt auf, HEPA-Stufe unklar); A1/A1 mini/P1P: offen, ohne Filter. X2D ist UL 2904 zertifiziert für PLA Basic und PETG Basic. (B/C, nicht von Bambu selbst abgerufen, da Shop/Wiki blockierten) https://bambuhub.net/articles/printer-air-quality
- H2D Pro Datenblatt (Drittseite): G3 + H12 HEPA + Kokosnuss-Kohle. https://m3.tuc.gr/media/xgxnc4i3/τεχνικο-φυλλαδιο-bambu-lab-h2d-pro.pdf (B). Ob das Standard-H2D gleich ist, ist hier nicht bestätigt; BambuHub und Alveo3D nennen es für die H2-Serie so.
- P2S-Filter: Bericht nennt Aktivkohle ohne UFP-Wirkung. https://all3dp.com/4/alveo3d-launches-hepa14-filter-kit-for-bambu-lab-p2s-adds-extra-layer-of-voc-capture/ (B)
- Filterlebensdauer Original-Kohlefilter: laut MatterHackers-Eintrag 1440 h (60 Tage); Alveo3D empfiehlt 600 h oder 2 Monate für seinen Filter; Forennutzer: ca. 3 Monate bei 8 h/Tag. Quellen uneinig. (B/C) https://www.matterhackers.com/store/l/spare-parts-for-bambu-lab-x1-and-p1-series-3d-printer/sk/MK1NQK8V (laut Suchzusammenfassung)
- Messungen: keine unabhängige Messung gefunden. Forum: ein Nutzer sah mit VOC-Messgerät beim H2D/ABS Anstieg nur beim Spülen, nicht während des Drucks (ohne Zahlen); anderer berichtet starken ABS-Geruch nach einer Stunde bei geschlossenem Fenster. (C) https://forum.bambulab.com/t/how-well-does-the-stock-air-filtration-work/159434
- Aftermarket für P1S/X1: Alveo3D HEPA13+Kohle-Kit, 71,99 EUR inkl. MwSt., "bis zu 4-fach weniger" UFP/VOC laut eigenen Tests, Filterwechsel nach 600 h oder 2 Monaten, warnt: ABS/ASA/Nylon/PETG/PC bleiben auch damit belastend; Drucker nicht bei Wohnbereichen. (B) https://www.alveo3d.com/en/product/kit-bambu-lab-activated-carbon-hepa13-air-filter/

### Prusa Core One / Core One+ / L
- Serie ohne HEPA-Filter; "Advanced Filtration System" als Nachrüstung (HEPA + Aktivkohle + Hochdrucklüfter, Heck, ca. 130x130x80 mm): 79,99 EUR (67,22 netto), "mindestens 600 Betriebsstunden", HEPA 99,9 %. Firmware steuert Filterung, Nachfilterung nach dem Druck einstellbar und empfohlen, mindestens 40 % Lüfterleistung nötig; Filter läuft nur bei als "Erfordert Filtration" markierten Filamenten (ABS, ASA, HIPS, PA laut Drittquellen) oder bei "Alle Materialien filtern". Prusa sagt selbst: es gibt keine Zertifizierung/Norm für Filtration im 3D-Druck. (B, Herstellerseite gelesen) https://www.prusa3d.com/de/produkt/advanced-filtration-for-prusa-core-one/ und https://help.prusa3d.com/de/article/fortschrittliches-filtersystem-fur-prusa-core-one_887189
- Messdaten für Core One + Filter: keine gefunden. Es gibt nur den Alveo3D-Test am Prusa XL (siehe oben).

### Qidi
- Q2: 3-in-1-Filter G3 + H12 HEPA + Kokosnuss-Kohle; HEPA-Schicht laut Qidi 99,5 % >0,3 µm; Lebensdauer in der Wiki ca. 1440 h (60 Tage), laut Produktseite 500 bis 800 h (300 bis 500 h bei Carbon/Nylon): widersprüchlich. (B) https://qidi3d.com/pages/qidi-q2-techspecs und https://wiki.qidi3d.com/en/Q2/Activated-Carbon-Air-Filter
- Plus4: laut Techspecs nur Aktivkohlefilter, kein HEPA in den gesehenen Ausschnitten; HEPA nur per Community-Umbau. (B/C) https://wiki.qidi3d.com/en/Plus4/Maintenance/Activated-Carbon-Air-Filter
- Messungen: keine.

### Creality K2-Serie
- K2/K2 Pro: ein Filter, K2 Plus: zwei; 101x101x10 mm; Material laut Händler "Aktivkohle + Partikelfilter", keine offizielle HEPA-Klasse gefunden; Händlerempfehlung Wechsel nach 100 bis 150 Druckstunden. Separater Creality-Luftreiniger: G4 + H11 HEPA (99,97 % bei 0,3 µm laut Hersteller) + Kohle. (B) https://store.creality.com/eu/products/k2-k2-pro-k2-plus-air-filter und https://store.creality.com/de/products/high-efficiency-composite-hepa-filter
- Messungen: keine.

## 6. Reicht Umluftfilter? Abluft? Luftreiniger?

- Gesicherte Aussagen: Dichte Gehäuse + Umluft mit HEPA und Kohle senken Partikel stark (>90 bis 99 % in Studien) und VOC mittelmäßig (ca. 50 bis 70 %). Gehäuse allein ist unterschiedlich gut (67 bis 95 %). Teilgehäuse oder Gehäuse mit Lüfterspalten sind schlechter. (Quellen oben, Abschnitt 4)
- Abluft ins Freie / abgesaugte Umhausung ist der von NIOSH und IFA genannte bessere Weg (Hierarchie der Schutzmaßnahmen). Für eine Mietwohnung meist unpraktisch; der Kompromiss ist ein Fenster-Abluftschlauch (Eigenbau/Zubehör) mit sicherer Abdichtung. (A/B) https://blogs.cdc.gov/niosh-science-blog/?p=11203 , https://engadget.com/2281211/bedroom-home-office-3d-printing-safety-concerns/
- Ein offenes Fenster allein entfernt Partikel und Gase laut Engadget nicht zuverlässig; kein Filter macht einen schlecht belüfteten Raum automatisch sicher. (B, Zusammenfassung) Engadget-Link wie oben.
- Zusätzlicher Raumluftreiniger: hilft bei Restpartikeln (HEPA), nicht bei Styrol/Caprolactam, wenn er keine ausreichende Aktivkohle hat. (C/B, abgeleitet)

## 7. Praktische Empfehlungen (belegt oder aus Studien abgeleitet)

1. Aufstellort: nicht im Schlafzimmer, besser separater Raum; Abstand zum Drucker verkürzt die Zeit in der Nähe, ersetzt aber keine Lüftung (Engadget, B). Ein Raum, in dem man sich nicht dauernd aufhält, mit Tür zu.
2. Lüften: bei Emissionsspitze (Aufheizen, erste Schicht) und danach gut lüften; ohne Lüften blieben Werte 30 min bis 4 h erhöht (Khaki et al., A). Stoßlüften nach Druckende; Partikelsensor/VOC-Sensor als Warnung (Khaki et al.).
3. Nach Druckende: Gehäuse geschlossen lassen, bis die Nachfilterung durch ist (Prusa empfiehlt Nachfilterung ausdrücklich; Alveo3D: langes Nachlüften nötig). Tür des Zimmers geschlossen halten bis nach dem Lüften (abgeleitet; keine direkte Studie dazu gefunden).
4. Nicht neben dem Drucker sitzen/schlafen; Teile nicht direkt nach dem Druck aus der heißen Kammer holen (Emission beim Öffnen) (abgeleitet).
5. Filterwechsel: Original-Kohlefilter laut Händlerangaben 600 bis 1440 h bzw. 2 bis 3 Monate (uneinig); bei ABS/ASA/Nylon eher kürzer. Kohle gesättigt = lässt VOC durch (CNRS-Hinweis laut Suchzusammenfassung). Wartung in der Schulstudie oft vergessen. Kosten: Prusa-AFS 80 EUR, Alveo3D-Kit ca. 72 EUR; Original-Bambu-Filterpreise nicht belegt.
6. Materialien Wohnung: PLA (am besten); PETG nur mit Gehäuse und Lüften; TPU Mittelfeld (Fang 2023: Zytotoxizität nicht signifikant, aber Partikel vorhanden). Meiden oder nur mit Gehäuse+Filter+getrenntem Raum: ABS, ASA, HIPS, PC, PA/Nylon, Carbonfaser-Filamente. Niedrigste sinnvolle Drucktemperatur, Marke und Farbe zählen (Faktor bis ~150 zwischen Marken).
7. Resin (SLA) ist ein eigenes Thema (Hautreaktionen, Lösungsmittel) und für Wohnung ohne Hobbyraum eher ungeeignet (NIOSH via Engadget, B).

## 8. Kaufrelevante Folgerung für den Nutzer (Wohnung, allein, emissionsarm)

- Nur PLA drucken: fast jedes geschlossene Gerät mit Filter ist ausreichend; ein offener Rahmen (z. B. Bambu A1/P1P) ist mit PLA ebenfalls vertretbar, wenn gelüftet wird.
- Wenn ABS/ASA/PA gewünscht: Gerät mit geschlossenem Gehäuse und HEPA+Kohle (Bambu H2-Serie/X2D laut BambuHub, Qidi Q2, Prusa Core One mit AFS) plus eigener Raum und Abluft/Lüften. Ein reiner Kohlefilter (Bambu P1S/X1C/P2S, Qidi Plus4) filtert keine ultrafeinen Partikel und braucht ggf. ein Nachrüst-HEPA.
- Wichtiger als der Filter: dichtes Gehäuse, Standort, Lüften, Materialwahl.

## Lücken (nicht gefunden)
- Keine unabhängige Messung der Serienfilter von Bambu H2D/X1/P1S, Prusa Core One, Qidi, Creality K2.
- Kein UL-Chemical-Insights-Bericht; Zhang/Black-Studie nur über Abstract/News gelesen.
- Offizielle Bambu-Wiki und Shop-Seiten waren blockiert (HTTP 402); Filterklassen daher über BambuHub/Drittseiten.
- BfR/UBA: keine konkreten Verhaltensempfehlungen für Privatnutzer gefunden.
- Fraunhofer-Salthammer-Studie zu Filterabdeckung/Luftreiniger nicht lesbar (ResearchGate 403).

BEST-PRACTICES-KANDIDATEN:
- HEPA filtert nur Partikel, Aktivkohle nur Gase; Kohle ist begrenzt und muss nach Stunden/Monaten gewechselt werden; reale Systeme senken Partikel >98 %, VOC nur ca. 50 bis 70 %. https://pmc.ncbi.nlm.nih.gov/articles/PMC6889885
- Emissionen entstehen vor allem beim Aufheizen/erste Schicht; danach Nachfilterung/Nachlüften einplanen (Prusa empfiehlt Nachfilterung; ohne Lüften 30 min bis 4 h erhöht). https://pmc.ncbi.nlm.nih.gov/articles/PMC8125858/ , https://help.prusa3d.com/de/article/fortschrittliches-filtersystem-fur-prusa-core-one_887189
- Emissionsarm drucken: PLA, niedrigste sinnvolle Düsentemperatur, ABS/ASA/PA/PC meiden oder nur mit dichtem Gehäuse + Filter + eigenem Raum. https://www.iaqip.wki.fraunhofer.de/en/all_topics/3d_printer.html
- Vor einem Gerätekauf Filterstufen prüfen: HEPA-Stufe vorhanden? (BambuHub-Tabelle) https://bambuhub.net/articles/printer-air-quality
- Wirksamste Schutzmaßnahmen (Studien): Absaugung an der Quelle/Abluft ins Freie und dichtes Gehäuse mit Gas- und Partikelfilter. https://stacks.cdc.gov/view/cdc/207601/cdc_207601_DS1.pdf , https://stacks.cdc.gov/view/cdc/258855

BUG-KANDIDATEN:
- Falle: Reiner Aktivkohlefilter (Bambu P1S, X1 Carbon, P2S, Qidi Plus4) hält keine ultrafeinen Partikel zurück; "Filter eingebaut" heißt nicht HEPA. https://bambuhub.net/articles/printer-air-quality , https://all3dp.com/4/alveo3d-launches-hepa14-filter-kit-for-bambu-lab-p2s-adds-extra-layer-of-voc-capture/
- Falle: Raumluftreiniger mit HEPA entfernt kein Styrol/Caprolactam (Gase). https://forum.bambulab.com/t/mitigation-for-voc-and-nano-particles-polluting-indoor-air/2285
- Falle: Gehäuse ist nicht gleich dicht; Spalten/interne Lüfter führen Emissionen am Filter vorbei (Teilgehäuse schlecht, Gehäuse 67 bis 95 %); UBA-Testdrucker mit Filterkassette hatte undichtes Gehäuse. https://stacks.cdc.gov/view/cdc/258855 , https://www.emerald.com/rpj/article/doi/10.1108/RPJ-05-2025-0182/ , UBA TEXTE 217/2020
- Falle: Gesättigte Aktivkohle lässt VOC einfach durch; Wechselintervalle der Hersteller widersprechen sich (Qidi 500 bis 1440 h; Bambu 1440 h vs. Alveo3D 600 h). https://wiki.qidi3d.com/en/Q2/Activated-Carbon-Air-Filter , https://qidi3d.com/ko/products/q2-3-in-1-air-filter
- Irrtum: "PETG ist unbedenklich": Fang et al. 2023 fanden bei PETG Partikelwerte ähnlich ABS. https://pmc.ncbi.nlm.nih.gov/articles/PMC10536656/
- Irrtum: Filterwirkungsangaben (z. B. "bis zu 4x weniger", 94 bis 97 %) stammen von Filterherstellern, nicht unabhängig geprüft; Prusa selbst sagt, es gebe keine Norm/Zertifizierung. https://www.alveo3d.com/en/product/kit-bambu-lab-activated-carbon-hepa13-air-filter/ , https://help.prusa3d.com/de/article/fortschrittliches-filtersystem-fur-prusa-core-one_887189
- Irrtum: Blauer Engel RAL-UZ 205 gilt für Büro-Drucker, nicht für 3D-Drucker; ein Siegel für FDM-Drucker gibt es nicht. https://www.umweltbundesamt.de/publikationen/weiterentwicklung-des-pruefwertes-fuer
