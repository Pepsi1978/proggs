# Heimnetz für Online-Gaming: WLAN vs. LAN, 2,4 vs. 5 GHz, IoT, Kanäle, Alternativen

Stand: 17.11.2026. Methodik: WebSearch + WebFetch, quellentreu. Qualität der Quellen ist durchweg gemischt; belastbare, kontrollierte Messreihen sind selten. Jede Zahl ist mit Quelle und Einordnung versehen. Kein Eigenmessungen-Ersatz: Franks eigene Messung (siehe Abschnitt 9) schlägt jede Literaturzahl.

Ausgangslage: Gaming-PC per WLAN, 2,4 GHz, Kanal 13, 802.11ax, Tenda AX1800 (2 Gigabit-LAN-Ports). Im selben WLAN: Kaffeemaschine, MacBook, Saugroboter, SwitchBot Hub Mini, Drucker, Handy.

---

## 1. Latenz, Jitter, Paketverlust: WLAN gegenüber LAN, 2,4 gegenüber 5 GHz

Harte Fakten (kontrollierter Test, eine Messreihe):
- PC Gamer hat LAN, Powerline, WLAN 5 GHz und 2,4 GHz verglichen: 5 GHz lag zwischen den Powerline-Werten, 2,4 GHz hatte mit großem Abstand die längste mittlere Latenz und schwankte stärker. Nahe am Router waren beide Bänder bei 3 ms zum Router. Quelle (nur über Suchzusammenfassung gelesen, Volltext war per WebFetch nicht abrufbar, Zahlen nicht selbst verifiziert): https://www.pcgamer.com/heres-how-playing-on-wi-fi-hurts-your-game

Typische Größenordnungen (Ratgeber/Anbieter, ohne offengelegte Methodik, nur als Orientierung):
- Ethernet: 0,1 bis 0,5 ms lokal, praktisch kein Jitter, Paketverlust ca. 0 %. WLAN: zusätzlich 5 bis 30 ms Latenz durch Medienzugriff und Konkurrenz. https://speedtesthq.com/guides/wireless/wifi-vs-ethernet
- Tabelle: WLAN 6 auf 5 GHz nah am Router ca. 3 bis 6 ms Latenz und 2 bis 4 ms Jitter; überfülltes 2,4 GHz 10 bis 50 ms Latenz und 20 bis 100 ms Jitter; Ethernet 1 bis 2 ms, Jitter unter 1 ms. https://speedtesthq.com/guides/wifi/wired-vs-wifi-for-gaming
- Weitere Ratgeberangaben: Cat6 1 bis 2 ms, WLAN 6 auf 5 GHz 8 bis 15 ms, überlastetes 2,4 GHz 25 bis 100 ms; WLAN-Paketverlust 0,1 bis 2 % (andere Quelle 1 bis 5 %), kabelgebunden 0 %. https://noping.com/blog/ethernet-vs-wi-fi-gaming
- Anbieter-Fallstudie ohne Methodik: Jitter 2 ms kabelgebunden gegenüber 14 ms auf 5-GHz-WLAN (Quelle über Suchzusammenfassung, Primärseite nicht geprüft).
- Gameswelt: Ping beim Kabel schwankt kaum, WLAN erzeugt je nach Last und Abstand gelegentliche Ausreißer; die Pauschalaussage "WLAN taugt nicht zum Spielen" stimmt nur teilweise. https://www.gameswelt.de/sonstiges-aus-der-welt-der-spiele/kampagne/lan-oder-wlan-fuers-gaming-wie-gross-ist-der-unterschied-in-der-praxis-335078

Einordnung: Für Online-Spiele zählt nicht der Mittelwert, sondern Jitter und Ausreißer (Spitzen durch Wiederholungen, Konkurrenz um die Luft, Stromsparmodus des Adapters, Hintergrund-Scans). Das ist der Kernnachteil von WLAN. 2,4 GHz verschärft ihn, weil das Band nur 3 überlappungsfreie Kanäle hat, von Nachbarn, Bluetooth, Mikrowelle und IoT belegt ist und die Geräte darauf oft langsame/alte Standards sprechen.

Wi-Fi-6-Hinweis (Wissen, ohne Quelle belegt): OFDMA/Target Wake Time helfen vor allem bei vielen gleichzeitigen kleinen Clients. Für einen einzelnen Spiele-Client ändert 802.11ax die Grundlage (Wettbewerb um die Luft auf dem Band) nicht grundlegend. Nicht quellenbelegt, nur als Hypothese kennzeichnen.

## 2. Einfluss vieler IoT-/Altgeräte auf die Latenz anderer Geräte

Mechanismus Airtime:
- Ohne Airtime Fairness (Standard bei den meisten modernen Routern) werden Clients nach "wer zuerst kommt" bedient; das Band läuft effektiv mit der Rate des langsamsten aktiven Clients, ein langsames Gerät verzögert alle dahinter. IoT-Geräte sind nahezu dauernd aktiv. Empfehlung des Autors: höchstens ca. 5 Geräte je Sender, IoT in eigener SSID auf anderem Kanal oder eigener AP, Hub-basierte Geräte (Zigbee/Z-Wave/Thread) bevorzugen. Warnzeichen sind Geräte mit WLAN 4 oder älter bzw. nur 2,4 GHz. https://dongknows.com/airtime-fairness-and-iot-smart-wi-fi-devices/ (keine Latenzmessung, Richtwerte des Autors)
- Airtime Fairness (ATF): TP-Link empfiehlt sie bei Mischbetrieb alter und neuer Clients (https://community.tp-link.com/en/home/stories/detail/137); Kritiker berichten von Verbindungsproblemen bei IoT und instabilen Routern (https://www.mbreviews.com/what-is-airtime-fairness-and-its-impact-on-wifi/). Es gibt keine Messung ATF ein/aus. Beim Tenda AX1800 gibt es die Option wahrscheinlich nicht oder versteckt; nicht verifiziert.

Altstandards und Mischbetrieb (802.11b/g):
- Sind 802.11b-Clients erlaubt, muss ein b-Rate aktiv und einzige Pflicht-(Basis-)Rate sein; das erzwingt CTS-Schutzframes für g/n/ax-Verkehr. Sind keine b-Clients nötig, empfiehlt Cisco, alle Raten unter 12 Mbps abzuschalten, das entfällt den CTS-Schutz. https://community.cisco.com/t5/wireless/2-4ghz-disable-low-data-rates/m-p/4478992 sowie https://community.cisco.com/t5/wireless/why-802-11b-devices-afftecting-all-the-other-devices-operating/td-p/2429263
- Meraki: Mindest-Bitrate auf 2,4 GHz hebt die Basisrate auf 12 Mbps, Beacons gehen dann ebenfalls mit 12 Mbps raus. https://documentation.meraki.com/MR/Radio_Settings/Minimum_Bitrate_Control
- Excentis: b-Clients beeinträchtigen 2,4 GHz selbst ohne Sendeverkehr, Beacons laufen mit 1 Mbps. https://excentis.com/?p=807
- Heim-Router (Tenda) bieten das selten als einzelne Rate; Ersatz ist der WLAN-Modus. Beobachtung aus Tenda-Handbuch AC15: Im Modus "11bg gemischt" ist 2,4-GHz-Bandbreite nur 20 MHz. Es gibt oft einen Modus "11b/g/n/ax" vs. "11n/ax only". https://www.manualslib.com/manual/1827453/Tenda-Ac15.html?page=59 (Hinweis: Seite ist AC15, nicht AX1800).

Broadcast/Multicast:
- Broadcast/Multicast gehen mit niedriger Basisrate (je nach Konfiguration 1 bis 6 Mbps) und belegen die Luft für alle länger; ein einziger alter 2,4-GHz-Client kann Multicast auf 1 Mbps ziehen. Typische Verursacher: mDNS/Bonjour (MacBook, Drucker, Handy), SSDP, Cast-Geräte. UniFi nennt hohe Client-Latenz/Timeouts als Symptom; Gegenmaßnahmen: Multicast-zu-Unicast, Multicast-Rate-Limit, VLAN/mDNS-Reflector. https://help.ui.com/hc/en-us/articles/27384925962647 (Seite per WebFetch 403, Angaben aus Suchzusammenfassung), https://www.ietf.org/archive/id/draft-perkins-intarea-multicast-ieee802-03.txt
- Die "mDNS belegt die Mehrheit der Airtime"-Aussage ist reine Herstellerbehauptung, unabhängig nicht bestätigt.
- DTIM/Stromsparbursts: Frames für schlafende Clients werden gebündelt zum DTIM-Intervall gesendet. https://wyebot.com/blogs/wifi-multicast-benefits-problems-and-mitigation-strategies/

Konkret für Franks Geräte:
- SwitchBot Hub Mini ist ausdrücklich nur 2,4 GHz. https://www.switch-bot.com/products/switchbot-hub-mini . Saugroboter, Kaffeemaschine, oft auch Drucker: typischerweise ebenfalls nur 2,4 GHz (SwitchBot nennt bei eigenen Saugern 2,4 GHz als Absicht wegen Stabilität: https://support.switch-bot.com/hc/en-us/articles/14546624360983 ). Für Franks Saugroboter/Kaffeemaschine/Drucker modellspezifisch nicht verifiziert.
- Folge: 2,4 GHz kann man nicht abschalten, aber man kann den Gaming-PC davon wegholen. Die IoT-Geräte brauchen das Band, der PC nicht.

## 3. Eigene SSID für 5 GHz statt Band Steering?

Ergebnisse, überwiegend Meinungen/Anbieter, kein Konsens:
- Band Steering: bequem für Handy/Laptop; manche Router schieben aggressiv auf 5 GHz auch bei knappem Signal (Retransmits). Clients entscheiden letztlich selbst (802.11v nur Empfehlung). Ohne Steering "kleben" Clients oft an dem Band, mit dem sie sich zuerst verbunden haben. https://www.qualcomm.com/content/dam/qcomm-martech/dm-assets/documents/qc_band_steering_wp.pdf
- Getrennte SSIDs: nach Anleitungen die konsistenteste Lösung für den Spiele-PC, weil man das Band selbst festlegt; Nachteil: Roaming/Sticky-Verbindungen bei mobilen Geräten. Bei Keenetic wird Band Steering automatisch abgeschaltet, wenn SSID/Schlüssel von 2,4 und 5 GHz nicht übereinstimmen. https://support.keenetic.com/titan/kn-1811/en/15221-band-steering.html ; Diskussion: https://forum.openwrt.org/t/band-steering-vs-separate-ssids/198154
- Empfehlung (Synthese): Für einen stationären Gaming-PC ist ein bewusst fest zugewiesenes Band besser als Steering. Konkret: 5-GHz-SSID mit eigenem Namen (z. B. "Heim-5G"), nur den PC (und ggf. MacBook, Handy) dort anmelden; IoT bleibt auf der 2,4-GHz-SSID. Im Windows-Profil für das Netz reicht auch die Adapter-Einstellung "Bevorzugtes Band = 5 GHz" (Gerätemanager, Adapter, Erweitert; Name je nach Treiber). Hinweis: Tenda-AX1800-Menü nicht geprüft; ob die Firmware getrennte SSIDs/Smart Connect anbietet, ist ungeklärt (Suche fand keine Modellquelle).

## 4. Kanalwahl 2,4 GHz: 1/6/11 oder 13 in Deutschland

- In Deutschland sind 13 Kanäle erlaubt, 1/6/11 gelten bei 20 MHz als überlappungsfrei. Kanal 13 ist in Europa zulässig, in den USA nicht; Fritz!Box deaktiviert 12/13 ab Werk. https://www.macwelt.de/article/944722/internet-kanaele.html ; https://www.pcwelt.de/article/1150447/wlan-special-tempo-im-wlan.html ; https://www.channelpartner.de/article/3908107/so-optimieren-sie-ihr-wlan-tempo-3.html
- Kanalabstand 5 MHz, Signal rund 20 bis 22 MHz breit; nur 1/6/11 (bzw. in Europa 1/5/9/13 oder 3/8/13 laut Omada) vermeiden Überlappung. https://www.omadanetworks.com/support/faq/4309/ ; Forendiskussion: 1/5/9/13 als Alternative, Kanal 13 sei frei, mit Einschränkung: manche Geräte (US-Firmware/IoT) unterstützen Kanal 13 nicht und können sich nicht verbinden; Rat, sich dem Schema der Nachbarn anzupassen. https://www.mikrocontroller.net/topic/545417 (Forum, 2022, unbelegte Behauptungen).
- Praktische Folgerung: Kanal 13 kann sinnvoll sein, wenn die Nachbarn zufällig/überwiegend 1/6/11 nutzen und 12/13 wenig belegt ist (typische Router-Voreinstellungen vermeiden 12/13). Aber: (a) Kanal 13 überlappt mit Kanal 11-Nachbarn teilweise, (b) manche 2,4-GHz-IoT-Geräte beherrschen 13 nicht. Da Franks IoT-Geräte aktuell verbunden sind, scheint das bei ihm kein Problem zu sein. Ob er auf 13 sinnvoller sitzt als auf 1/6/11, entscheidet die Nachbarschaftsmessung (Abschnitt 8).
- Wichtig: Gehört der PC auf 5 GHz, ist die 2,4-GHz-Kanalwahl für das Gaming nur noch zweitrangig.

## 5. Kanalbreite 20 vs. 40 MHz (2,4 GHz)

- 40 MHz belegt auf 2,4 GHz mindestens zwei der drei überlappungsfreien Kanäle. Nach 802.11-2012 starten AP in 20 MHz und dürfen nur nach Prüfung auf 40 MHz; ein reiner 40-MHz-Modus ist im 2,4-GHz-Band nicht erlaubt. Apple sperrt 40 MHz in 2,4 GHz komplett, Intel-Adapter liefern standardmäßig 20 MHz. https://www.smallnetbuilder.com/wireless/wireless-features/bye-bye-40-mhz-mode-in-24-ghz-part-1/
- In Tests fielen etliche Router nicht korrekt auf 20 MHz zurück (Cisco EA3500/EA4500/WAP-321 teils, EnGenius EAP-300), und manche blieben nach Wegfall der Störung auf 20 MHz hängen; ein "nur 40 MHz"-Modus ignorierte die Koexistenz. Fazit des Autors: Zertifizierung allein nicht vertrauen. https://www.smallnetbuilder.com/wireless/wireless-features/bye-bye-40-mhz-mode-in-24-ghz-part-2/
- Tenda-Handbücher (andere Modelle) bieten 2,4 GHz: 20 MHz, 40 MHz oder automatisch 20/40 MHz. https://www.manualslib.com/manual/2633780/Tenda-Ax3000.html?page=50
- Empfehlung: 2,4 GHz fest auf 20 MHz; die 40-MHz-Stufe bringt in einer vollen Nachbarschaft nur mehr Kollisionen. 20 MHz ist auch die Omada-/TP-Link-Standardempfehlung. https://www.omadanetworks.com/support/faq/4309/
- Für den Spiele-Traffic ist die Bandbreite ohnehin unwichtig (wenige Mbit/s); es zählt die Störarmut.

## 6. 5 GHz: Kanal, Breite, DFS

- 5-GHz-Kanäle sind 20 MHz auseinander und breit, also bei 20 MHz überlappungsfrei; viel mehr Auswahl als 2,4 GHz. Breitere Kanäle (40/80/160) erhöhen den Durchsatz, brauchen mehr Spektrum und sind störanfälliger. https://www.omadanetworks.com/support/faq/4309/
- DFS: Kanäle in 5250 bis 5350 MHz und 5470 bis 5725 MHz erfordern Radarerkennung (etwa Kanäle 52 bis 64 und 100 bis 140). Die nicht-DFS-Kanäle 36, 40, 44, 48 sind sofort nutzbar. Bei DFS-Kanal läuft zuerst eine Prüfzeit (CAC) von ca. 1 bis 10 Minuten ohne 5 GHz; bei erkanntem Radar wechselt der Router den Kanal, die Verbindung kann kurz abbrechen. ASUS rät bei Abbrüchen/Störungen von DFS-Kanälen ab. https://www.asus.com/de/support/faq/1045936/ ; https://www.schieb.de/probleme-beim-5ghz-frequenzband-der-fritzbox-loesen ; Rechtsgrundlage BNetzA: Allgemeinzuteilung 5 GHz Vfg. 136/2022 (geändert durch 49/2023) für 5150 bis 5250, 5250 bis 5350, 5470 bis 5725 MHz: https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Frequenzen/OeffentlicheNetze/LokaleNetze/_functions/faq_2,4%20bzw.%205%20GHz.html . DFS-Pflichten selbst stehen im ETSI-Standard (Lancom-Handbuch): https://www.lancom-systems.de/docs/LCOS-Refmanual/9.20-RU1/EN/Referenzhandbuch_7.60_EN/WLAN/aa1614946.html . Die Aufteilung 5150-5250 (Kanal 36-48, DFS dort optional) stammt aus der Lancom-Quelle; innen/Außen-Einschränkungen der BNetzA-Verfügung wurden nicht geprüft.
- Empfehlung (Synthese): Für den Gaming-PC Kanal 36/40/44/48 (nicht-DFS) mit 20 oder 40 MHz Breite (80 MHz nur, wenn die Nachbarschaft leer ist), nicht Auto/DFS, sodass keine Radar-Kanalwechsel mit Lag-Spitze auftreten. Tenda-Kanalliste und Voreinstellungen nicht geprüft.
- Wenn die Reichweite durch Wände nicht reicht (5 GHz dämpft stärker), ist das ein Grund für Abschnitt 7.

## 7. Alternativen, falls das Kabel nicht geht

Der Router hat 2 Gigabit-LAN-Ports. Wenn der PC nicht in Kabelreichweite ist:

Powerline/dLAN:
- Netzwelt (2012, 14 Adapter, 500 Mbit-Klasse): Ping LAN 35 ms, Powerline 36 bis 38 ms; Fazit: fürs Online-Gaming durchaus geeignet; Messung alt. https://www.netzwelt.de/news/92347-antwortzeiten-ping-werte-aktueller-powerline-adapter.html
- Guru3D: nah beieinander ca. 4 ms und kein Verlust; weit entfernt in der Küche mit vielen Verbrauchern instabil. https://guru3d.com/review/devolo-dlan-200-aveasy-powerline-adapter-review/page-5
- Igor's Lab: kabelgebunden 6 ms, über 1200-Mbit-Powerline bis 16 ms. https://www.igorslab.de/trendnet-powerline-1200-av-netzwerk-stromkabel-power-lan/2/
- Fehlerkorrektur kann bei Störungen Latenzspitzen erzeugen (Forenmeinung): https://www.computerbase.de/forum/threads/paketverlust-schlechtes-internet.2253647/ ; ein Forennutzer maß 3 bis 7 ms dLAN gegenüber ca. 1 ms WLAN: https://extreme.pcgameshardware.de/threads/wlan-besser-als-dlan-wie-gut-ist-normales-lan.472990/
- Risiko: Störungen durch Netzteile, Mehrfachsteckdosen mit Filter (nicht in Steckerleiste stecken), unterschiedliche Stromkreise/Phasen. Direkt in die Wandsteckdose.
- Fazit: Powerline ist meist besser als 2,4-GHz-WLAN und oft vergleichbar mit gutem 5 GHz, aber sehr abhängig von der Hausinstallation; vorher Test (ping -t 5 Minuten, Jitter prüfen).

Mesh mit LAN-Backhaul:
- Mit LAN-Kabel zwischen Router und Satellit entfällt der Halbierungs-/Hop-Nachteil; kabelgebundener Backhaul gilt als Empfehlung. Drahtlose Backhaul-Hops: etwa 50 % Durchsatz je Hop (Meraki-Angabe aus Cisco-Community), Latenzzuwachs linear je Hop, Zahlen 2 bis 15 ms je Hop stark uneinheitlich (Blogs). https://community.cisco.com/t5/wireless/client-hops-to-gateway-cut-by-50-per-hop-is-this-true/m-p/3867599 ; https://community.tp-link.com/en/home/forum/topic/749740
- Aber: LAN-Backhaul braucht ja gerade das Kabel, das fehlt. Mesh bringt also nur etwas, wenn zwischen Router und Satellit (und nicht bis zum PC) ein Kabel liegt, oder der Satellit per Powerline angebunden wird; dann steht der PC per LAN am Satelliten.

WLAN-Bridge / Media Bridge:
- Zweiter Router/AP im Client-/Bridge-Modus in Nähe des PCs, PC per LAN daran. Funktioniert nur sauber, wenn die Funkstrecke auf 5 GHz (eigener, fester Kanal) liegt; ein Zwischenschritt mit wireless Hop kostet Durchsatz und Latenz (siehe oben). Vorteil gegenüber PC-eigener Antenne: externe, besser ausgerichtete Antenne, stabilere Ausrichtung. Quellen zur Latenz einer Bridge wurden nicht gefunden; Aussage ist Synthese.

Rangfolge (Synthese aus obigen Quellen): 1) Kabel direkt (Router-LAN-Port, 2 vorhanden) 2) Powerline oder LAN-Backhaul zu AP/Bridge, wenn Leitung vorhanden 3) PC auf 5-GHz-SSID, Kanal 36 bis 48 4) WLAN-Bridge 5 GHz 5) 2,4 GHz nur im Notfall.

## 8. Windows: WLAN-Kanäle prüfen

Quelle zum Befehl: https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netsh-wlan (Syntax show networks, show interfaces, show wlanreport); Praxis: https://woshub.com/check-wi-fi-signal-strength-windows/ ; https://mcpmag.com/Articles/2017/06/22/Viewing-Wireless-Networks-with-PowerShell-Legacy-Commands.aspx

1. Eingabeaufforderung/PowerShell öffnen und ausführen:
   `netsh wlan show networks mode=bssid`
   Pro SSID erscheinen je Funkzelle (BSSID) Signal (in Prozent, nicht dBm), Funktyp (802.11n/ax …), Band und Kanal. Zählen: Wie viele Netze/BSSIDs liegen auf Kanal 11, 12, 13, auf 1 und 6?
2. Das eigene Netz: `netsh wlan show interfaces` zeigt SSID, BSSID, Funktyp, Kanal, Empfangs-/Senderate und Signal der aktuellen Verbindung.
3. Detaillierter Bericht: `netsh wlan show wlanreport` erzeugt einen HTML-Bericht der letzten Sitzungen (Abbrüche, Gründe).
4. Gotchas: Auf deutschem Windows sind die Feldnamen übersetzt (Kanal, Signal, Netzwerktyp): Skripte nicht an englischen Bezeichnern festmachen. Ein simples `findstr SSID` trifft auch die BSSID-Zeilen. Scan ist eine Momentaufnahme; mehrfach und zu Spielzeiten (abends) wiederholen.
5. Auswertung: Wenig/keine Nachbarn auf 12/13 und viele auf 1/6/11 spricht für Kanal 13 (mit 20 MHz) auf 2,4 GHz; wenn nicht, 1/6/11 nehmen, den am wenigsten belegten Kanal. Komfortgrafik: WifiInfoView (https://helpdeskgeek.com/free-tools-review/wifi-scanner-channel-scanner-software-windows/ ).
6. Eigene Latenzmessung statt Literaturzahlen: `ping -n 300 <Router-IP>` (Router, Teil der Strecke), danach `ping -n 300 1.1.1.1`; Ausreißer und Verlust (Zeile "Verloren") für 2,4 GHz, 5 GHz und Kabel vergleichen; ideal PingPlotter. Die Methodik schlug bereits der PC-Gamer-Test vor.

## 9. Umsetzbare Reihenfolge für Frank (Synthese, keine Zitate)

1. Kabel vom PC zu einem der 2 LAN-Ports, wenn irgendwie machbar (Flur-Leitung, Flachkabel, notfalls Powerline).
2. Sonst: 5-GHz-SSID getrennt benennen, PC dort anmelden, Kanal fest 36/40/44/48 (kein DFS/Auto), 20 oder 40 MHz.
3. 2,4 GHz nur für IoT: Kanal per `netsh wlan show networks mode=bssid` wählen (1/6/11 oder 13), Breite fest 20 MHz, WLAN-Modus ggf. "11n/ax only" wenn keine b/g-Geräte; wenn der Router Mindestrate bietet, 12 Mbit.
4. Wenige 2,4-GHz-Clients: IoT-Geräte nur dort, die für 5 GHz gehen (MacBook, Handy, ggf. Drucker) auf 5 GHz verschieben; das entlastet 2,4 GHz.
5. Ping-Vorher/Nachher-Messung dokumentieren.

---

BEST-PRACTICES-KANDIDATEN:
- Gaming-PC nicht auf 2,4 GHz: Kabel > Powerline > 5 GHz (Kanal 36 bis 48, kein DFS) > 2,4 GHz; Jitter und Ausreißer zählen, nicht der Mittelwert. https://www.pcgamer.com/heres-how-playing-on-wi-fi-hurts-your-game , https://www.gameswelt.de/sonstiges-aus-der-welt-der-spiele/kampagne/lan-oder-wlan-fuers-gaming-wie-gross-ist-der-unterschied-in-der-praxis-335078
- 2,4 GHz fest auf 20 MHz, Kanal 1/6/11 oder bewusst 13 nach Nachbarschaftsscan. https://www.smallnetbuilder.com/wireless/wireless-features/bye-bye-40-mhz-mode-in-24-ghz-part-1/ , https://www.omadanetworks.com/support/faq/4309/
- IoT getrennt halten (eigene SSID/eigener Kanal oder eigener AP), höchstens ca. 5 Geräte je Sender, Hub-Geräte (Zigbee/Thread) bevorzugen. https://dongknows.com/airtime-fairness-and-iot-smart-wi-fi-devices/
- Niedrige Basisraten (1/2/5,5/11 Mbit) abschalten, wenn keine 802.11b-Geräte nötig sind (Mindestrate 12 Mbit, CTS-Schutz entfällt). https://documentation.meraki.com/MR/Radio_Settings/Minimum_Bitrate_Control , https://community.cisco.com/t5/wireless/2-4ghz-disable-low-data-rates/m-p/4478992
- Multicast/mDNS-Last auf WLAN begrenzen. https://www.ietf.org/archive/id/draft-perkins-intarea-multicast-ieee802-03.txt
- Windows-Kanalcheck: `netsh wlan show networks mode=bssid`, `netsh wlan show interfaces`, `netsh wlan show wlanreport`. https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netsh-wlan
- Powerline direkt in die Wandsteckdose, vorher Ping/Jitter-Test. https://www.netzwelt.de/news/92347-antwortzeiten-ping-werte-aktueller-powerline-adapter.html , https://guru3d.com/review/devolo-dlan-200-aveasy-powerline-adapter-review/page-5
- Drahtlose Mesh-Hops vermeiden: Backhaul per Kabel. https://community.cisco.com/t5/wireless/client-hops-to-gateway-cut-by-50-per-hop-is-this-true/m-p/3867599

BUG-KANDIDATEN:
- 2,4-GHz-Kanal 13 / 12: manche (US-)Geräte und IoT-Firmwares unterstützen die Kanäle nicht und können sich nicht verbinden. Forenquelle, unbelegt: https://www.mikrocontroller.net/topic/545417
- 20/40-MHz-Koexistenz in 2,4 GHz: etliche Router fallen bei Nachbar-APs nicht zurück auf 20 MHz, andere bleiben danach auf 20 MHz hängen; "nur 40 MHz"-Modus ignoriert Koexistenz. https://www.smallnetbuilder.com/wireless/wireless-features/bye-bye-40-mhz-mode-in-24-ghz-part-2/
- DFS: Radar-Kanalwechsel und CAC-Wartezeit (1 bis 10 Minuten) führen zu kurzen Verbindungsabbrüchen auf 5 GHz; deshalb für Gaming Nicht-DFS-Kanäle fest einstellen. https://www.asus.com/de/support/faq/1045936/ , https://www.schieb.de/probleme-beim-5ghz-frequenzband-der-fritzbox-loesen
- Airtime Fairness: Berichte über IoT-Verbindungsprobleme und Router-Instabilität bei eingeschalteter Option. https://dongknows.com/airtime-fairness-and-iot-smart-wi-fi-devices/ , https://www.mbreviews.com/what-is-airtime-fairness-and-its-impact-on-wifi/
- netsh unter deutschem Windows: Feldnamen übersetzt, findstr "SSID" trifft BSSID. https://mcpmag.com/Articles/2017/06/22/Viewing-Wireless-Networks-with-PowerShell-Legacy-Commands.aspx

Offene Lücken/Unsicherheit:
- Tenda AX1800: keine modellspezifische Quelle gefunden (getrennte SSIDs, ATF, Mindestrate, Kanalliste, DFS). Im Router-Menü selbst nachsehen.
- PC-Gamer-Test (Zahlen), UniFi-Seite (403) und die konkreten BNetzA-Kanal/Innenraumregeln wurden nicht im Volltext verifiziert.
- Keine Quelle für Latenz einer WLAN-Bridge im Vergleich zu Powerline; keine kontrollierte Studie ATF ein/aus.
