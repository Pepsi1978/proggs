# R2: Signal, Band, Platzierung, Antennen bei 5G/LTE-Heimrouter (O2 Deutschland) für niedrigen, stabilen Ping

Stand der Recherche: 17.11.2026. Methode: WebSearch + WebFetch, URL pro Aussage. Quellenqualität steht dabei: (P) = Primär/Hersteller/Betreiber, (R) = Ratgeber, (F) = Forum/Einzelerfahrung, (S) = Sekundär.

## 1. Signalwerte: gut / schlecht

Es gibt keine einheitliche Norm, jede Quelle zieht die Grenzen anders. Zwei Skalen im Vergleich:

| Größe | Teltonika (Hersteller, P) | lte-anbieter.info (R, Notenskala) |
|---|---|---|
| RSRP | >= -80 excellent; -80..-90 good; -90..-100 fair/poor; < -100 poor | -50..-65 sehr gut; -65..-80 gut; -80..-95 befriedigend (stabil); -95..-105 ausreichend (Einbrüche möglich); ab ca. -105 keine stabile schnelle Verbindung erwarten; -110..-125 mangelhaft |
| RSRQ | >= -10 excellent; -10..-15 good; -15..-20 fair/poor; <= -20 poor | -3 sehr gut; -4..-5 gut; -6..-8 befriedigend; -16..-20 ungenügend |
| SINR | >= 20 excellent; 13..20 good; 0..13 fair/poor; <= 0 poor | +20..+40 sehr gut; +10..+19 gut/stabil; +5..+9 brauchbar; 0..+4 knapp, Verbindungsprobleme möglich; -1..-5 extrem instabil |

Quellen:
- Teltonika Wiki, Mobile Signal Strength Recommendations: https://wiki.teltonika-networks.com/view/Mobile_Signal_Strength_Recommendations
  Wörtlich: Nahe RSRP -100, RSRQ -20, SINR 0 "performance will drop drastically"; "A minimum of SINR should be >= 0 dB for basic operation"; RSSI allein unzuverlässig.
- lte-anbieter.info SINR: https://www.lte-anbieter.info/technik/sinr.php (unter ca. 10 dB externe Antenne erwägen; 0..4 dB Verbindungsprobleme, unter 0 oft instabil/unbrauchbar)
- lte-anbieter.info RSRP: https://www.lte-anbieter.info/technik/rsrp.php (typisch real -70..-95 dBm; ab ca. -105 keine stabile schnelle Verbindung)
- lte-anbieter.info RSRQ: https://www.lte-anbieter.info/technik/rsrq.php (Skala laut Suchergebnis; Seite nicht einzeln abgerufen)
- Zyxel (Hersteller): Beispiel schwaches 5G RSRP -107 dBm vs. 4G -69 dBm; Ziel für 5G "-80 dBm or better"; Router "often appears to go completely offline" bei schwachem 5G: https://community.zyxel.com/en/discussion/31637/how-to-improve-fwa710-connection-stability-with-weak-5g-signal
- Zyxel Support: bei RSRP <= -120 dBm instabil/nicht installierbar: https://support.zyxel.eu/hc/en-us/articles/360005188999 (nur aus Suchergebnis)

Praktische Einordnung für Gaming (Synthese aus obigen Quellen, kein belegter Messwert): RSRP besser als ca. -95 dBm UND SINR >= 10 dB UND RSRQ besser als ca. -12 dB anpeilen. SINR und RSRQ sind für Stabilität wichtiger als RSRP: RSRP sagt nur "wie stark", nicht "wie sauber". Beispiel Teltonika: RSSI -68/RSRP -56, aber SINR -1,8 und RSRQ -16 = schlechte Verbindung (https://www.teltonika-networks.com/de/blog/beyond-signal-bars-understanding-rssi-rsrp-sinr-other-key-metrics, nur aus Suchergebnis). Telecomhall: SINR sei "best predictor of real user experience", starkes RSRP mit schlechtem SINR liefert trotzdem schlechte Performance: https://www.telecomhall.net/t/rsrp-vs-rsrq-vs-sinr-the-signal-trio-that-powers-5g-decisions/33958
RSRQ sinkt auch mit Zellauslastung, nicht nur mit Interferenz (ebd.).

## 2. Ab wann steigen Latenz und Paketverlust? Einfluss von SINR auf Jitter

LÜCKE (ehrlich): Ich habe KEINE Messstudie gefunden, die Ping-RTT/Jitter/Paketverlust als Kurve gegen SINR/RSRP an einem realen LTE/5G-Link zeigt. Alle Schwellen oben sind Hersteller-/Ratgeberangaben zu "Performance/Abbrüchen", nicht zu Millisekunden.

Was belegt ist (Mechanismus):
- Bei niedrigem SINR fallen mehr Übertragungen durch und HARQ/RLC-ARQ müssen wiederholen; jede Wiederholung kostet Zeit. In einer Simulation (UAV-Link, nicht Heimrouter) war die HARQ-Latenz im niedrigen SINR-Bereich höher und sank von -5 dB bis +5 dB mit weniger Wiederholungen: https://arxiv.org/pdf/2507.20392
- VoLTE-Studie: IR-HARQ und RLC-ARQ-Wiederholungen erzeugen "sudden delay ramping" = abrupte Jitter-Sprünge; HARQ-RTT ca. 8 ms, höchstens ca. 2 Wiederholungen pro Paket akzeptabel: https://jwcn-eurasipjournals.springeropen.com/articles/10.1186/s13638-021-01970-y
  => Plausible Folge: schlechtes SINR zeigt sich vor allem als Jitter/Ping-Spikes (Vielfache von ca. 8 ms) und bei zu viel Verlust als Paketverlust, weniger als Anhebung der Grundlatenz.
- Patentschrift (nur Behauptung der Erfinder, nicht unabhängig): RSRP hat die höchste negative Korrelation mit RAN-Latenz: https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11044185
- SINR steuert Modulation/Coding (MCS) und damit Durchsatz (Telecomhall, lte-anbieter oben). Bei schlechtem SINR wählt das Netz robustere, langsamere MCS; Pakete brauchen länger auf der Luftschnittstelle, bei Last staut es.
- Einordnung "Ping stabil": Heimrouter-Foren berichten typische 9-15 ms (5G NSA, guter Empfang) bis 22-50 ms (anderes Routing/Last): https://hilfe.o2online.de/mobilfunkdienste-telefonie-internet-messaging-mehr-22/massiver-ping-anstieg-von-9ms-auf-24ms-nach-rueckgabe-des-o2-homespots-routing-fehler-664969 (F). Dort wird auch gesagt, dass das Core-Routing (in welchem Rechenzentrum die Verbindung endet) ein großer Latenzfaktor ist, den nur O2 steuert.

## 3. 5G NSA mit schwachem Signal vs. stabiles LTE

Befund: Mehrere unabhängige Quellen sagen: bei schwachem 5G ist erzwungenes LTE (nur LTE, ggf. Band-Lock auf einen starken LTE-Layer) oft stabiler.
- Zyxel (Hersteller): "4G-only mode ... has proven effective in stabilizing connections when 5G is unreliable"; empfiehlt erst NR5G-NSA/4G Auto Switch, dann 4G-only; Router höher/besser Sichtlinie: https://community.zyxel.com/en/discussion/31637/how-to-improve-fwa710-connection-stability-with-weak-5g-signal
- MikroTik-Forum 06/2026 (F, Quectel RG520F, Latvia): Mit 5G NSA war LTE-Anker-SINR merklich schlechter und Upload instabil, mit Paketverlust in Videocalls; nach LTE-only Upload stabil ca. 100 Mbit/s. LTE-Anker B3 -75 dBm, B7/B1/B20 per CA; dutzende schwache n78/n77-Zellen bei -100..-115 dBm, n78 wurde per CA ständig hinzugefügt/entfernt (kurze Aussetzer). Erklärung eines Antwortenden: das NETZ wählt Technologie/Zelle/CA; mit 5G NSA kann das Modem auf eine weiter entfernte oder höhere Band-Zelle geschoben werden; nicht jede LTE-Zelle kann 5G-NSA verankern. Warnung: Zell-/Band-Locks können je nach Tageszeit inkonsistent sein (Betreiber schalten Sender zum Energiesparen ab): https://forum.mikrotik.com/t/chateau-5g-r16-poor-performance-in-5g-vs-lte-only-mode/270883
- O2-Community 04/2025 (F+Staff), FRITZ!Box 6850 5G: 5G NSA fällt ständig ab, LTE-only stark. O2-Mitarbeiter/Community: Bänder 20 und n28 im Router blockieren ("definitiv"), alternativ auf 900/1800/2100 festlegen; Router 6850 5G unterstützt LTE-800 + 5G-n28 nicht; externe Antennenanschlüsse nur Mid/High-Bänder, Low-Bands laufen über interne Antennen: https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/5g-nsa-laeuft-nicht-stabil-651561
- Gegenbeispiel Einzelfall (S, Suchergebnis): Abschalten der Low-Bands habe 5G-NSA stabiler gemacht. Ergebnis ist standortabhängig.
- 5G kann auch bei gutem Standort langsamer sein als 4G (SA auf n28: 16 Mbit/s vs. 85-90 Mbit/s LTE, O2-Staff vermutet Überlast/schwache Abdeckung), Thread 10/2026: https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/warum-ist-5g-langsamer-als-4g-676112
- NSA braucht immer einen LTE-Anker: fällt der Anker weg oder ist er überlastet, bricht auch 5G ab (Foren, Suchergebnis O2-Community).

Fazit: Kein Beleg, dass 5G generell mehr Ping macht (5G SA/NSA bei gutem Signal liefert eher 10-20 ms laut O2, s. u.). Aber bei schwachem Signal (n78 bei -100..-115 dBm oder SINR < ca. 5 dB) ist ein starker LTE-Layer (B3/B1/B20) fast immer stabiler, vor allem wegen CA-Flattern, instabilem Anker und Upload-Verlust. Methode: A/B-Test auf demselben Router (Auto vs. LTE-only), 20-30 Min Ping mit Spielverkehr, Jitter/Max-Ping/Verlust vergleichen.

5G SA bei O2 ("5G Plus"): seit Oktober 2023 netzweit (inside-digital, Stand 09.07.2026: https://www.inside-digital.de/ratgeber/o2-netz-abdeckung-lte-5g). Latenz bei SA laut O2 ca. 10-20 ms (VR-Gaming-Premiere, https://uk.marketscreener.com/quote/stock/TELEF-NICA-DEUTSCHLAND-HO-11814173/news/Telef-nica-Deutschland-Telef-nica-O2-starts-the-future-of-mobile-gaming-via-5G-35727544/ (S)). KEIN verifizierter Millisekunden-Vergleich SA vs. NSA gefunden. Nutzer berichten bei SA hohen Ping wegen fehlerhafter Server-Zuweisung (Core-Routing): https://hilfe.o2online.de/mobilfunkdienste-telefonie-internet-messaging-mehr-22/o2-5g-plus-sa-hoher-ping-durch-fehlerhafte-server-zuweisung-analyse-ursache-und-aufruf-zur-behebung-658453 (F, Seite nicht abgerufen). Ob der eigene Router SA kann, ist modellabhängig.

## 4. O2-Bänder in Deutschland und welches für Reichweite/Stabilität

ACHTUNG Quellenalter: Die Bandliste stammt aus Artikeln 2019/2022/2026. Prüfen, ob aktuelles Bild abweicht.

| Frequenz | Band | Nutzung bei O2 (Quelle, Stand) | Reichweite/Rolle |
|---|---|---|---|
| 700 MHz | n28 (5G) | 5G Flächenband, "ausschließlich für 5G" (inside-digital 2026); in NSA mit LTE 800 als Anker (teltarif 11/2022); 5G SA auch auf n28 (O2-Forum 10/2026) | Größte Reichweite, gute Gebäudedurchdringung, aber schmale Kanäle (Kapazität klein) |
| 800 MHz | LTE B20 | LTE, NSA-Anker für n28 (teltarif 2022) | Flächen-/Landband, sehr gute Reichweite |
| 900 MHz | LTE B8 | Laut Suchergebnissen 2G und teils LTE; O2-Mitarbeiter nennen 900/1800/2100 als alternative Lock-Bänder (O2-Forum 2025). 2G-Abschaltung geplant Mitte-Ende 2028 | Gute Reichweite |
| 1800 MHz | LTE B3 / n3 | LTE, 5G per DSS "vereinzelt" (teltarif 2022, inside-digital 2026) | Guter Kompromiss: Reichweite + Kapazität, in Städten oft LTE-Rückgrat |
| 2100 MHz | LTE B1 / n1 | LTE + 3G; ab 01.01.2026 hat 1&1 einen 2x10-MHz-Block übernommen, den bisher O2 hielt; 3G wird zu 4G/5G umgewidmet (simdealz/winfuture, nur Suchergebnis) | Mittlere Reichweite, weniger Kapazität seit Block-Abgabe |
| 2600 MHz | LTE B7 | LTE (mobiflip 12/2019) | Kurze Reichweite, viel Kapazität |
| 3,6 GHz | n78 | 5G, Kapazitätsband (70 MHz laut mobiflip 2019), Zellradius nur "wenige hundert Meter" (teltarif 2022), "sehr hohe Datenraten, geringe Reichweite" (inside-digital 2026) | Kürzeste Reichweite, größte Gebäudedämpfung |

Quellen: https://www.mobiflip.de/telefonica-o2-frequenzspektrum-in-der-uebersicht/amp/ (Stand 12/2019), https://www.teltarif.de/arch/2022/kw45/s89941.html (Stand 12.11.2022), https://www.inside-digital.de/ratgeber/o2-netz-abdeckung-lte-5g (06.01.2026/09.07.2026), https://www.lte-anbieter.info/lte-news/lte-netz-in-gefahr-versteigert-bnetza-die-frequenzbaender, Nutzungsrechte 800/1800/2600 MHz bis Ende 2025 verlängert: https://telefonica.de/news/press-releases-telefonica-germany/2024/05/mobile-radio-frequencies-an-eight-year-extension-creates-the-greatest-possible-benefit-for-germany.html (nur Titel/Suchergebnis).

Physik (relative Orientierung, Quellen Hersteller-Glossare/Ratgeber, Zahlen streuen stark):
- Freiraum-Pfadverlust 700 MHz vs. 3,5 GHz: ca. 14 dB Unterschied (20*log10(3500/700), eigene Rechnung; ähnlich https://rfessentials.com/resources/rf-glossary/band-n28/).
- Gebäudedurchdringung: ca. 8-12 dB bei 700/800 MHz vs. 15-25 dB bei 3,5 GHz (Quellen streuen, 15-30 dB): https://www.kaeltripton.com/telecoms/indoor-outdoor-5g-coverage/ (S), https://rfessentials.com/resources/rf-glossary/cellular-700-mhz/ (S)
- 3,5 GHz Zellen 1-3 km, 700 MHz 5-10 km oder mehr (Quellen uneinig); n78-5G verschwindet oft nach ca. 200 m, weil das LTE-Signal beim Handover stärker wird, nicht wegen harter n78-Grenze; Standortabstand städtisch 250-500 m: https://blog.wirelessmoves.com/2019/10/the-range-of-5g-band-n78.html

Antwort "welches Band für Reichweite/Stabilität": Für Reichweite und Wand-Durchdringung ranken B20/n28 (700/800) vorn, dann B8, B3 (1800), B1; n78 und B7 sind Kapazitäts-Bänder mit kürzerer Reichweite. Für STABILEN PING bei schwachem Signal: ein starker LTE-Layer (B3/B20, ggf. B1) schlägt schwaches n78 (siehe 3). ABER: Low-Band-Antennen sind bei vielen Routern (z. B. FRITZ!Box 6850 5G) nur intern, externe Antenne wirkt dann nur auf Mid/High-Band (O2-Forum oben). Und ein O2-Mitarbeiter-Hinweis (Einzelquelle, 2025): Cell-ID eines O2-Kunden zeigte LTE-800 (B20), das über MOCN von Vodafone geteilt sei und nicht zum 5G-NSA-Setup des Routers passte; deshalb B20/n28 sperren. Einzelaussage, nicht verallgemeinern.
Ein Band-Lock ist riskant: Nutzer berichtet, nach Sperren von B28 Signal verloren, Router hing (Suchergebnis O2-Forum); Betreiber können Sender zeitweise abschalten, Lock kann dann ins Leere laufen (MikroTik-Thread oben). Vorher Werkseinstellung/Notzugang klären.

## 5. Router-Platzierung und Mastsuche

Platzierung (lte-anbieter.info Ratgeber, https://lte-anbieter.info/ratgeber/lte-empfang-verbessern.php):
- Router an einem Fenster in Richtung Station testen, den Punkt mit höchstem Pegel suchen (z. B. Fensterbrett); "oft entscheiden wenige Zentimeter".
- Mit RSRP und RSRQ (und SINR) fein einstellen; nicht nur Balkenanzeige/RSSI.
- Wärmeschutzverglasung und isolierte Gebäudehülle dämpfen zusätzlich; Außenantenne umgeht das.
- Höher ist besser ("so hoch wie möglich" bei Außenantenne); Hügel, Wald, hohe Gebäude aus der Hauptkeule halten.
- Mindestabstand 30 cm zu Metallflächen (Herstellerhinweis laut Suchergebnis desselben Anbieters).
- Test: Router probeweise draußen vor das Fenster stellen; wird es deutlich besser, hilft eine Antenne (Forum: https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/homespot-empfang-verbessern-560459). O2-Moderation: Ausrichtung auf eine andere Station ist möglich; Netzabdeckungskarte nutzen.
- Zyxel: Router erhöhen/Sichtlinie zum 5G-Mast: siehe oben. GL.iNet: Vor dem Antennenkauf erst Router näher ans Fenster/höher: https://docs.gl-inet.com/router/de/4/tutorials/understanding_external_cellular_antennas/
- Beste Richtung ist nicht zwingend die zum Mast (Reflexion, Beugung): https://www.lte-anbieter.info/daheim/tools-besserer-empfang.php
- Unter 5 km Sichtlinie und gutem Signal im Haus oft keine Außenantenne nötig (Ratgeber oben).
- Methode: Pro Position 1-2 Minuten warten, RSRP/RSRQ/SINR UND verbundenes Band/Zelle notieren (Band kann beim Umstellen wechseln!). Mit Ping (Gaming-Ziel) gegenprüfen.

Mastsuche / Tools:
- lte-anbieter.info Tools: Höhenprofil-Tool (Profimodus mit Haus-/Antennen-/Masthöhe und Frequenz, Fresnel-Zone), Pingtest, LTE-Speedtest zu verschiedenen Tageszeiten, FRITZ!Box-Ausrichthilfe: https://www.lte-anbieter.info/daheim/tools-besserer-empfang.php. LTEWatch-Tool empfohlen vom O2-Forum (https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/5g-nsa-laeuft-nicht-stabil-651561).
- CellMapper (https://www.cellmapper.net/FAQ): Standorte "based purely on calculations", LTE am genauesten kartiert, Zelle kann 5-15 km "stretchen"; roter vs. grüner (bestätigt) Turm. eNB-ID-Rechner im Tools-Menü; Tower Search per eNB-ID + MCC 262 + MNC (O2 = 07/08/11 laut allgemeinem Wissen, nicht aus Quelle) (Anleitung nach Peplink-Forum/CTF-Writeup: https://forum.peplink.com/t/identify-the-cell-tower-show-on-map/31664, https://ctftime.org/writeup/23892). Android-App vorhanden, iOS gibt keine Zelldaten. Band-Filter kann Türme ausschließen. Hinweis: Dass man auf der Karte durch Band-Filter eingrenzt, stammt aus Forenbeitrag (F).
- Bundesnetzagentur EMF-Standortdatenbank (Standorte mit Standortbescheinigung, Positionsungenauigkeit 0-80 m): https://emf3.bundesnetzagentur.de/karte/default.aspx (Zitat aus DARC-Vortrag, https://www.darc.de/fileadmin/filemounts/distrikte/g/ortsverbaende/21/2020_03_07_DD3JI_vortrag.pdf); eine BNetzA-Seite meldete die Datenbank zeitweise nicht verfügbar, live prüfen. Liste enthält Antennenhöhen/Richtungen typischerweise, hier aber nicht belegt.
- O2-Netzkarte/Netzcheck auf o2online.de (Adresse/PLZ, 2G/4G/5G einzeln, innen/außen, "In Kürze"): https://www.o2online.de/netz/netzcheck/ ; Überblick der Karten: https://www.pcwelt.de/tipps/Netzkarten-von-Deutscher-Telekom-Vodafone-und-O2-So-pruefen-Sie-die-Netzabdeckung-10727534.html . Karten sind Schätzungen, Nutzer berichten Abweichungen. O2 Live Check für Störungen.
- Cell-ID aus dem Router in Mast ableiten: lte-anbieter.info beschreibt das auf der Tools-Seite nicht; Weg ist über CellMapper eNB-Rechner (s. o.).

## 6. Externe Antennen (MIMO, Richtantenne)

- Nutzen: verbessern Verbindungsqualität bei geschwächtem Signal (Beton, Keller, ländlich), garantieren aber nicht mehr Speed: "Eine externe Antenne kann nicht jedes Problem ... lösen" (GL.iNet): https://docs.gl-inet.com/router/de/4/tutorials/understanding_external_cellular_antennas/. Grenzen: starkes Signal schon vorhanden, überlasteter Mast, unterstützte Bänder, lange Kabel, Antenne direkt neben internen Antennen. Angeschlossene externe Antennen schalten die internen ab.
- Richtantenne: für festen Standort und bekannten Mast, braucht genaue Ausrichtung; Omni nur bei mehreren Masten/mobil (GL.iNet). Außenantenne ist laut lte-anbieter.info "eindeutig bessere Wahl" (Ratgeber, https://lte-anbieter.info/ratgeber/lte-empfang-verbessern.php).
- Gewinn: Richtantennen typisch 7-14 dBi laut Teltonika; WiMo-Doppel-Richtantenne 9,5-11 dBi je nach Band (Suchergebnisse): https://www.wimo.com/de/60072
- MIMO: Alle Antennenanschlüsse bestücken (nur ein Anschluss = weniger Leistung); 2x2 bevorzugt als zwei um 90° versetzte Polarisationen (lte-anbieter.info). Der Ratgeber behauptet "fast verdoppelte" Datenrate, das betrifft Durchsatz, nicht Ping.
- Kabel: Kurz, wenig Adapter, Low-Loss; Kabel/Stecker beeinflussen EIRP (GL.iNet, lte-anbieter.info). Sendeleistungsrechner (EIRP <= +25 dBm) auf lte-anbieter.info.
- Erfahrungsberichte gemischt (F): Antennentausch ohne Effekt (FRITZ!Box 6840), bei großer Mastentfernung teils negativ; bei guter RSRP aber schlechtem RSRQ/SINR deutet das auf Störung durch Nachbarzellen hin, die Antenne nicht heilt (lte-anbieter.info Forum, Suchergebnis). Interferenz: Nachbarmasten auf gleichen Frequenzen senken SINR/RSRQ bei gleichem RSRP (https://www.lte-anbieter.info/ratgeber/faktoren-fuer-empfang.php). Richtantenne kann hier helfen, weil sie Nachbarzellen ausblendet.
- Aussage "SINR > 20 dB für MIMO" stammt aus einem unbelegten Forenpost, nicht verwenden.
- Wichtig für O2: Prüfen, ob die Antenne alle genutzten Bänder abdeckt (inkl. 700/800 und 3,6 GHz) und ob die externen Router-Ports Low-Bands bedienen (FRITZ!Box 6850: nur Mid/High extern, O2-Forum oben).

## 7. Tageszeit und Zellauslastung

- Shared Medium: Alle Nutzer teilen die Zellkapazität; Engpässe z. B. gegen 20 Uhr trotz gutem Empfang: https://www.lte-anbieter.info/ratgeber/faktoren-fuer-empfang.php . Eigener Test: Speedtest/Pingtest an verschiedenen Tagen und Uhrzeiten (https://www.lte-anbieter.info/daheim/tools-besserer-empfang.php).
- O2-Staff bestätigt 11/2026 Überlast an zwei Stationen für eine Straße (Datenrate abends ca. 1-3 Mbit/s), keine neue Station geplant: https://hilfe.o2online.de/mobilfunkdienste-telefonie-internet-messaging-mehr-22/titel-des-beitrags-chronische-ueberlastung-einbruch-der-datenrate-jeden-abend-an-meiner-adresse-seit-2-jahren-677026 (Ping wird dort nicht genannt). Alternativhypothese im Thread: Tagesvolumen-Drossel des Tarifs (10 GB/Tag, danach 384 kbit/s); unbestätigt.
- RSRQ fällt bei Last: abends RSRQ/SINR mitloggen. Fällt RSRQ abends bei gleichem RSRP, ist es Zelllast bzw. Interferenz, nicht Antennenproblem (Telecomhall oben).
- Peak-Stunden-Verschlechterung bleibt auch bei 5G erheblich, 5G senkt vor allem Latenz (Ookla-Studie Mai 2026 laut Sekundärbericht, nicht verifiziert): https://mobileecosystemforum.com/2026/06/19/europes-5g-networks-are-already-under-strain-at-peak-is-investment-falling-behind-demand/ (S).
- Bandsperren-Warnung: Locks können je nach Tageszeit unterschiedlich wirken (Sender-Abschaltung, MikroTik-Thread).
- KEIN FWA-spezifisches Latenz-Tageszeitprofil gefunden. Der O2-Forum-Thread zu Ping-Spikes bis 3000 ms am Abend betraf einen Festnetz-Anschluss (DSL/Kabel), nicht Mobilfunk, und ist nicht übertragbar: https://hilfe.o2online.de/dsl-kabel-glasfaser-router-software-internet-telefonie-34/betreff-massive-ping-spikes-bis-zu-3000-ms-und-paketverlust-in-den-abendstunden-ueberlasteter-hop-2-674564

## 8. Konkrete Maßnahmenliste (Synthese)

1. Messen statt raten: RSRP, RSRQ, SINR, Band, Zelle/PCI am Router-Menü loggen, dazu 20-30 Min Ping zum Spielserver (Mittel, Max, Jitter, Verlust), morgens und abends.
2. Platz suchen: Fenster zum Mast, möglichst hoch, weg von Metall/Wärmeschutzglas, Zentimeterschritte; erst messen, dann kaufen. Test draußen vor dem Fenster als Obergrenze des Möglichen.
3. A/B: Auto/5G-NSA vs. LTE-only (Zyxel: erst "NR5G-NSA/4G Auto Switch", dann "4G only"). Bei SINR < ca. 5 dB oder n78 schwächer als ca. -100 dBm: LTE bevorzugen.
4. Erst danach Band-Lock (z. B. LTE B3 allein oder B3+B1, ggf. B20/n28 sperren, wenn der Router die Kombination nicht beherrscht); Notweg sichern; mehrere Tageszeiten testen.
5. Außen-Richtantenne (MIMO, 2 Leitungen, kurze Low-Loss-Kabel, 90° versetzt) wenn SINR < ca. 10 dB trotz bestem Platz, Mast bekannt (CellMapper/BNetzA-EMF/O2-Netzkarte) und Router externe Ports für das genutzte Band hat.
6. Bei abends fallendem RSRQ/Speed: Zelllast; Lösung nur anderer Mast (Antenne auf andere Station ausrichten, O2-Moderation) oder Betreiberwechsel/Stationsmeldung beim Anbieter.
7. SA/"5G Plus": Wenn Router SA kann und Signal gut, testen; Core-Routing beeinflusst Ping stark, nur O2 kann das ändern.

## OFFENE LÜCKEN
- Keine Messkurve Ping/Jitter/Verlust vs. SINR für LTE/5G-FWA gefunden.
- Aktuelle, belastbare O2-Bandliste (Bandbreiten, B8-LTE, B7, 2100-Block nach 01.2026) nur teilweise belegt; O2-MNC nicht aus Quelle.
- Kein verifizierter NSA-vs.-SA-Latenzvergleich; kein FWA-Tageszeitprofil.
- Mehrere Zahlen (Reichweiten, dB-Durchdringung) stammen aus Hersteller-Glossaren und streuen stark.

BEST-PRACTICES-KANDIDATEN:
- RSRP/RSRQ/SINR-Schwellen für FWA-Heimrouter (Teltonika + lte-anbieter.info): https://wiki.teltonika-networks.com/view/Mobile_Signal_Strength_Recommendations ; https://www.lte-anbieter.info/technik/sinr.php ; https://www.lte-anbieter.info/technik/rsrp.php
- LTE-only/4G-only bei schwachem 5G zur Stabilisierung, A/B-Messung: https://community.zyxel.com/en/discussion/31637/how-to-improve-fwa710-connection-stability-with-weak-5g-signal ; https://forum.mikrotik.com/t/chateau-5g-r16-poor-performance-in-5g-vs-lte-only-mode/270883
- Router-Platzierung/Antennen-Ausrichtung (Fenster, Höhe, 90°-MIMO, kurze Kabel, erst messen): https://lte-anbieter.info/ratgeber/lte-empfang-verbessern.php ; https://docs.gl-inet.com/router/de/4/tutorials/understanding_external_cellular_antennas/ ; https://www.lte-anbieter.info/daheim/tools-besserer-empfang.php
- Mastsuche: CellMapper + BNetzA-EMF + O2-Netzcheck: https://www.cellmapper.net/FAQ ; https://emf3.bundesnetzagentur.de/karte/default.aspx ; https://www.o2online.de/netz/netzcheck/
- O2-Bänder und Rollen (700/800 Fläche, 1800 DSS, 3,6 GHz Kapazität): https://www.teltarif.de/arch/2022/kw45/s89941.html ; https://www.inside-digital.de/ratgeber/o2-netz-abdeckung-lte-5g

BUG-KANDIDATEN:
- FRITZ!Box 6850 5G im O2-Netz: 5G NSA instabil, wenn der Router LTE B20 (800) mit n28 nicht unterstützt; externe Antennenanschlüsse bedienen nur Mid/High-Bänder; Workaround B20/n28 sperren: https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/5g-nsa-laeuft-nicht-stabil-651561
- 5G NSA verschlechtert LTE-Anker-SINR / CA-Flattern bei schwachen n78-Zellen (Quectel RG520F, MikroTik, 06/2026), Workaround LTE-only: https://forum.mikrotik.com/t/chateau-5g-r16-poor-performance-in-5g-vs-lte-only-mode/270883
- Zyxel FWA710: bei schwachem 5G (RSRP -107) wirkt Router offline, wechselt nicht auf stärkeres 4G; Workaround 4G-only: https://community.zyxel.com/en/discussion/31637/how-to-improve-fwa710-connection-stability-with-weak-5g-signal
- O2 5G Plus SA: hoher Ping durch fehlerhafte Server-Zuweisung/Core-Routing (nur Forenbericht, Seite nicht abgerufen): https://hilfe.o2online.de/mobilfunkdienste-telefonie-internet-messaging-mehr-22/o2-5g-plus-sa-hoher-ping-durch-fehlerhafte-server-zuweisung-analyse-ursache-und-aufruf-zur-behebung-658453
- Band-Lock-Risiko: Sperren von B28/Lock auf Zelle kann Signalverlust/Router-Hänger auslösen, tageszeitabhängig (O2-Forum/MikroTik-Thread oben).
