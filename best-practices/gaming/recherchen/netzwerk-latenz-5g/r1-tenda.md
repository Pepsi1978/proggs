# Recherche r1: Tenda 5G03 - Weboberfläche, Einstellungen, Nutzerberichte (Stand 17.11.2026)

Methode: WebSearch/WebFetch. Primärquelle ist das offizielle englische Benutzerhandbuch (206 Seiten, V1.0), lokal mit pdftotext ausgewertet.
Quelle Handbuch: https://static.tenda.com.cn/tdeweb/download/5G03/5G03V1.0%20User%20Guide.pdf (Spiegel: https://www.tenda.cz/sites/Upload/5G03/5G03_UG.pdf)
Hinweis: Eine Hardware-Revision V1.0 und V1.1 existiert; Firmware ist nicht untereinander tauschbar.

## 1. Zugang
- Weboberfläche: tendawifi.com oder 192.168.0.1; beim ersten Login ohne Passwort, danach selbst vergeben. https://www.tendacn.com/faq/2003693 und Handbuch Kap. 4/13.
- Administration laut SpeedGuide nur per LAN/WLAN (kein Fernzugriff standardmäßig): https://www.speedguide.net/routers/tenda-5g03-ax1800-wi-fi-6-5g-nr-router-5173

## 2. Menüstruktur laut Handbuch (Seitenangaben der Kapitel)
Hauptmenü: Internet Status, Internet Settings, Wi-Fi, Advanced Settings, System Settings (Handbuch Inhaltsverzeichnis, Kap. 4, 5, 12, 13).

### 2.1 Netzmodus (Internet Settings > "Mobile Data Options")
Auswahl laut Handbuch Kap. 5.1 (Tabelle "Mobile Data Options"):
- 5G SA Preferred: SA, NSA, 4G, 3G werden nacheinander je nach Signalstärke genutzt
- 5G NSA: NSA und 4G nacheinander je nach Signalstärke
- 4G Only
- 3G Only
Es gibt KEINE Option "5G SA only" und keine "NR only". Hinweis im Handbuch: Roaming-Schalter (Data Roaming) und Failover (zweiter Modus bei Ausfall) vorhanden.
Quelle: Handbuch Kap. 5.1 / 5.1.1 (Beispiel "4G Only" auswählen und Connect klicken).
Spezifikation: 5G NR SA und NSA, EN-DC, "5G location optimization", Auto-APN: https://www.tendacn.com/product/specification/5G03

### 2.2 Band-Lock / Zell-Lock
- Band-Lock: Internet Settings, ganz unten scrollen; zwei Band-Schalter (standardmäßig aus), Schalter an, Band wählen, Apply/Save. https://www.tendacn.com/faq/2003598 und https://www.tendacn.com/faq/2003693
- Tenda empfiehlt: Das vom Handy für die SIM gemeldete Band (NSA-Band) auch am Router zu sperren; Bandinfo per Android-App "Cellular-Z" oder iPhone-Feldtest (*3001#12345#*) ermitteln. https://www.tendacn.com/faq/2003693
- Die Band-Auswahl existiert also in neueren Firmwareständen nativ. MioNonno nennt Zell-Lock (PCI) beim 5G03 als NICHT unterstützt: https://miononno.it/router/tenda-5g03
- Zell-Lock mit "PCI&Band lock" (Advanced Settings) gibt es laut Tenda-FAQ beim Schwestermodell 5G06, nicht belegt für 5G03: https://www.tendacn.com/faq/711537505906757 und https://www.tendacn.com/material/show/718600051363909
- Drittanbieter-Hack (modifiziertes JavaScript der Weboberfläche, Italien): Live-Dashboard (RSRP, RSRQ, SINR, CQI, eNB/Cell-ID, alle 2 s) und Eingabe von LTE-/NR-Bändern als "3+78" oder "AUTO"; Shell-Zugang. Nicht von Tenda, Sicherheitsrisiko, nicht geprüft: https://miononno.it/router/tenda-5g03 (Video: https://www.youtube.com/watch?v=jH8-A63Twbo)

### 2.3 APN
- Internet Settings: Profile Name, PDP Type, APN, User Name, Password, Authentication Type. Auto-APN normalerweise; per "Create a Profile" manuell anlegbar. Handbuch Kap. 5.1 und 5.1.2.
- Für O2 Deutschland: APN beim Anbieter erfragen (Handbuch sagt: "Contact your ISP"). Ich habe in dieser Recherche keine O2-spezifische APN-Quelle geprüft.

### 2.4 MTU
- Nur in System Settings > WAN Settings und nur im "wireless router mode" (WAN-Port, Kabelinternet), NICHT für den SIM-/Mobilfunkbetrieb. Standard 1500 (PPPoE 1492), empfohlener Bereich 1400-1500. Handbuch Kap. 13.3.1.
- Für Mobilfunk gibt es also keinen MTU-Regler in der Oberfläche. Endgerät-seitig (PC) wäre MTU anpassbar, das ist aber nicht Gegenstand der Quelle.

### 2.5 QoS / Bandbreite / Priorität
- Advanced Settings > Bandwidth Control: pro Gerät Upload- und Download-LIMIT (Begrenzung). Keine Priorisierung, keine Gaming-Regel. Handbuch Kap. 12.4.
- Das Datenblatt wirbt mit "QoS" und einer Prioritätsliste per MAC-Adresse (manua.ls-Zusammenfassung: "give certain devices higher priority by entering their MAC addresses"): https://www.manua.ls/tenda/5g03/manual. Im Handbuch-Text, den ich ausgewertet habe, ist nur Bandwidth Control (Limit) beschrieben, keine Prioritäts-Seite. Unklar/nicht verifiziert, ob die Firmware eine Prioritätsfunktion zeigt.
- Marketing "5G Slicing"/Gaming-Modus und "Latenz unter 10 ms": reine Herstellerangabe, keine Messung gefunden: https://www.speedguide.net/routers/tenda-5g03-ax1800-wi-fi-6-5g-nr-router-5173
- Gast-WLAN mit eigenem Bandbreitenlimit (Kap. 8).

### 2.6 DNS
- LAN-Seite (System Settings > LAN Settings): "DNS Settings" aktivieren, Primary/Secondary DNS, die per DHCP an Clients verteilt werden; Router hat DNS-Proxy. Handbuch Kap. 13.1.
- WAN-DNS bei SIM-Betrieb: wird vom Netz vergeben (Connection Status zeigt Primary/Secondary DNS), manuelle WAN-DNS nur im Kabel-WAN-Modus (Kap. 5.2).
- Praktisch: DNS am PC/Konsole selbst setzen oder die Client-DNS im LAN-Menü des Routers.

### 2.7 IPv6
- Handbuch Kap. 11 "IPv6 (wireless router mode)": Dual-Stack nur im Kabel-WAN-Modus (DHCPv6, PPPoEv6, statisch, Tunnel). Für den SIM-Betrieb gibt es im Handbuch keine IPv6-Einstellung; Datenblatt nennt IPv6 nicht: https://www.tendacn.com/product/specification/5G03
- IPv6 für Mobilfunk daher nicht einstellbar (nach Handbuchlage).

### 2.8 Signalwerte
- Internet Status > Connection Status (4G/5G router mode) zeigt: Signal Strength (Excellent/Good/Fair), ISP, Mobile Network (Netztyp), Access Band, UL/DL Frequency, Cell ID, CQI, Point A ARFCN, SSB ARFCN, SS-RSRP, SS-RSRQ, SS-RSSI, SS-SINR (Handbuch Kap. 13.10.2).
- Die Detailwerte sind die 5G-NR-Werte (SS-...). Eigene LTE-Anker-Werte (RSRP/RSRQ/SINR des LTE-Ankers bei NSA) nicht belegt.
- Signal-Suchtest: Internet Status, "search", "start test" misst Balkenwerte pro Aufstellort; Ergebnisse speicherbar und vergleichbar (Handbuch Kap. 4.2).
- Bewertungsrichtwerte SINR: über 10 dB gut, unter 0 dB Interferenz: https://hicelltek.com/en/blog/rsrp-rsrq-sinr-normal-values/

### 2.9 Firmware
- System Settings > Firmware Upgrade: Online (OTA) oder Local (.bin), danach Werksreset empfohlen (Handbuch 13.7). USB-C 3.0-Port "für Firmware-Upgrade" (Handbuch 1.2.2).
- Offizielle Liste: V1.1 V05.03.01.26 (neueste), V1.0 V05.03.01.19, ISP-Datei V05.03.01.22. Quelle: https://www.tendacn.com/product/help/5G03 (Fassungen der Suchtreffer widersprechen sich teils: .24/.18/.09 an anderen Stellen).
- V1.0 V05.03.01.19, 24.06.2026: Roaming standardmäßig an, Fehler behoben, bei dem sich das Gerät in manchen Situationen nicht mit dem Netz verband, mehr Log-Möglichkeiten, Fehlerbehebungen; danach Werksreset: https://www.tendacn.com/material/show/105784?proPath=5G03
- V1.1 V05.03.01.24, 04.11.2025: SIM-Netzkompatibilität und unerwartete Neustarts behoben: https://www.tendacn.com/material/show/737731861692485 (Inhalt im Abruf nicht ladbar, Angabe aus Suchergebnis). Release-Notes für .26 nicht gefunden.
- Sicherheitslücke CVE-2026-38063 (Command Injection) für Version V05.03.02.04 / HW V1.0, ohne Patch zum Analysezeitpunkt: https://vuln.today/cve/CVE-2026-38063 (nur Fundstelle aus Suchergebnis, nicht selbst geprüft).
- Tenda-FAQ: Update-Anleitung https://www.tendacn.com/faq/2003502
- Tenda-Hilfe-FAQ-Titel u. a.: "I have a full signal, but web pages won't load", "My internet speed is very slow", "What is band lock for on 5G03": https://www.tendacn.com/product/help/5G03

## 3. Antennenanschlüsse
- 2 x TS-9 an der Rückseite für externe Antennen. Handbuch Kap. 1.2.2: "Used to connect self-prepared N77 or N78 external antennas to extend 5G NR signals". Also offiziell nur für 5G NR n77/n78 vorgesehen. Spezifikation: 8 interne Mobilfunk-Antennen plus 2 WLAN-Antennen, extern 2 x TS-9, keine SMA: https://www.tendacn.com/product/specification/5G03
- MioNonno: TS9 "solo per 5g" (nur 5G): https://miononno.it/router/tenda-5g03. Ein Whirlpool-Nutzer bestätigt externe Antennenfähigkeit (anders als Nokia FastMile) und nennt Stabilität schlecht: https://forums.whirlpool.net.au/archive/90x10zkp (Seite per Abruf nicht erreichbar, Angabe aus Suchergebnis).
- Alza.at-Rezension: die Anschlüsse liegen zu eng beieinander (Montage/Kabel): https://www.alza.at/EN/reviews/tenda-5g03-ax1800-5g-nr-wifi-6-7940035.htm
- Nicht belegt: ob die externen Anschlüsse auch LTE-Bänder nutzen. Wirkung einer Außenantenne beim 5G03 nicht gemessen belegt (nur anderes Gerät: O2-Homespot-Thread https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/aussenantenne-fuer-homespot-5g-620905/index6.html).
- Praktikabler Rat aus Quellen: Router an anderem Standort testen (Signaltest-Funktion), da Standort entscheidend ist.

## 4. Nutzerberichte Latenz/Ping/Stabilität
- Italien (WindTre): Latenz um 140 ms gegenüber ca. 30 ms beim früheren Fritz-Router im 4G-Betrieb; Vermutung: 5G-Uplink auf 3,5 GHz; Empfehlung, den Router außen bzw. auf dem Balkon aufzustellen. https://forum.fibra.click/d/47112-prestazioni-tenda-5g03 (per Abruf 403, Angabe aus Suchergebnis-Zusammenfassung)
- Italien, anderes Modem (ZTE MC7010) mit Tenda: Ping ca. 35 ms, Jitter 5-10: https://forum.fibra.click/d/48097-configurazione-tenda-5g03-con-zte-mc7010/14 (Suchergebnis)
- Italien: Probleme nach Firmware-Update bei einer Very-SIM, Frage nach Werksreset (Tenda empfiehlt ihn): https://forum.fibra.click/d/67338-tenda-5g03-con-sim-very
- Amazon.de-Rezension (Suchergebnis): 14 ms Ping, 430 Mbit Download, 70 Upload; ein Nutzer: Abbrüche nach nicht näher beschriebener Änderung verschwunden: https://www.amazon.de/-/en/Tenda-5G03-4-67Gbps-Download-Gigabit/dp/B0BKSY2N4S (Seite per Abruf 503)
- Testsieger.de (970 Bewertungen, 3,8/5): häufig genannt "Instabile Verbindung, die ohne erkennbaren Grund abbricht", "Schwankende Downloadgeschwindigkeiten", Schwierigkeiten bei LAN-Verbindung; "Guter Empfang auch ohne Zusatzantennen": https://www.testsieger.de/mobilfunk-router/3C8HCSV1T09U2-tenda-5g03-5g-nr-lte-wlan-router-fuer-nano-sim-karte-ax1800-cat19-467gbps-download-2-gigabit-port-wi-fi-6-mu-mimo-wpa3-schwarz.html (automatische Zusammenfassung, keine Einzelzitate)
- Testbericht.de 4,3/5 aus 80 Bewertungen: https://www.testbericht.de/produkte/tenda-5g03
- Australien (Optus): ein Nutzer ohne Internet trotz Signal, APN aus Nokia kopiert: https://forums.whirlpool.net.au/archive/90x10zkp ; anderer Thread: Tenda-Support habe Sonderfirmware geliefert: https://forums.whirlpool.net.au/archive/3w8yqvmr (nur Suchergebnis)
- Kein Reddit-Treffer und kein Treffer zu O2 Deutschland + 5G03 + Call of Duty gefunden. Kein veröffentlichter Test, dass LTE-only oder n78-Lock den Ping beim 5G03 verbessert. Das ist eine Hypothese zum Selbsttest.
- O2-Community allgemein (nicht 5G03): 5G-NSA-Ping 10-15 ms bis 40-60 ms mit Schwankungen; Threads älter als drei Jahre: https://hilfe.o2online.de/mobilfunkdienste-telefonie-internet-messaging-mehr-22/ping-zeiten-im-5g-netz-599004

## 5. Konkret umsetzbare Einstellungen für Franks Fall
1. Standort optimieren mit dem Signaltest (Internet Status > search > start test); Fensternähe/Obergeschoss, mehrere Orte speichern und vergleichen. Wichtigster Hebel laut Quellen.
2. Werte ablesen: SS-SINR, SS-RSRP, Access Band, CELL ID im Connection Status notieren (Zielwert SINR über 10 dB).
3. Netzmodus testen: "5G NSA" gegenüber "5G SA Preferred" gegenüber "4G Only" (Mobile Data Options) und jeweils Ping/Jitter über längere Zeit messen. Schlechter 5G-Uplink (n78) kann Latenz erhöhen; LTE-only ist eine Hypothese zum Testen.
4. Band-Lock: unten auf Internet Settings die zwei Band-Schalter; das vom Handy gemeldete Band sperren, um Wechsel zwischen schwachen und starken Bändern/Zellen zu verhindern. Rückgängig durch Schalter aus.
5. Firmware auf V05.03.01.26 (V1.1) bzw. .19 (V1.0) aktualisieren (Hardwareversion am Etikett prüfen), danach Werksreset und neu konfigurieren (Tenda-Vorgabe).
6. Kabel statt WLAN fürs Spielen (LAN-Port); WLAN-Jitter vermeiden. Bandwidth Control: andere Geräte begrenzen, damit sie den Uplink nicht füllen (es gibt nur Limits, keine Priorität).
7. DNS: im LAN-Menü eigene DNS verteilen oder am PC setzen. MTU und IPv6 sind für den SIM-Betrieb in der Oberfläche nicht verfügbar.
8. Externe Antenne: 2 x TS-9, nur für n77/n78 vorgesehen; nur sinnvoll, wenn n78 genutzt wird und eine passende 5G-Antenne vorhanden ist; Wirkung beim 5G03 nicht belegt.

BEST-PRACTICES-KANDIDATEN:
- Tenda 5G03 Weboberfläche: Netzmodus (SA Preferred/NSA/4G/3G), Band-Lock unten in Internet Settings, Signalwerte im Connection Status, MTU/IPv6 nur im Kabel-WAN-Modus, Bandwidth Control nur als Limit, 2 x TS-9 nur n77/n78: https://static.tenda.com.cn/tdeweb/download/5G03/5G03V1.0%20User%20Guide.pdf ; https://www.tendacn.com/faq/2003598 ; https://www.tendacn.com/faq/2003693
- Aufstellort per Signaltest wählen, SINR über 10 dB, Standortwechsel vor Antennenkauf: https://hicelltek.com/en/blog/rsrp-rsrq-sinr-normal-values/ ; Handbuch Kap. 4.2
- Nach Firmware-Update Werksreset (Tenda-Vorgabe): https://www.tendacn.com/material/show/105784?proPath=5G03

BUG-KANDIDATEN:
- Tenda 5G03: Verbindungsabbrüche und schwankende Geschwindigkeit laut Rezensionen (Testsieger.de, 970 Bewertungen): https://www.testsieger.de/mobilfunk-router/3C8HCSV1T09U2-tenda-5g03-5g-nr-lte-wlan-router-fuer-nano-sim-karte-ax1800-cat19-467gbps-download-2-gigabit-port-wi-fi-6-mu-mimo-wpa3-schwarz.html
- Tenda 5G03 V1.1: Neustarts und SIM-Netzkompatibilität (behoben in V05.03.01.24): https://www.tendacn.com/material/show/737731861692485
- Tenda 5G03 V1.0: Netzverbindung schlug in manchen Situationen fehl (behoben in .19): https://www.tendacn.com/material/show/105784?proPath=5G03
- Hohe Latenz (140 ms) im Italien-Bericht, vermutlich 5G-Uplink 3,5 GHz: https://forum.fibra.click/d/47112-prestazioni-tenda-5g03
- CVE-2026-38063 Command Injection (V05.03.02.04, HW V1.0): https://vuln.today/cve/CVE-2026-38063
