# R6: CoD / Modern Warfare 4 (PC, DMZ) - Netzwerk, Telemetrie, NAT, Routing-Tools

Stand der Recherche: 17.11.2026. Quellentreu; Aussagen mit URL. Vermerk "Hersteller" = Anbieter verkauft ein Routing-Produkt (Eigeninteresse). Eigene Einschätzungen sind als solche markiert.

## 0. Wichtigste Erkenntnis in Kürze
- Es gibt KEINE offizielle Activision-Seite mit Schwellenwerten für Ping, Jitter oder Paketverlust. Alle numerischen "kritisch ab"-Werte sind Faustwerte (eigene Einschätzung, nicht belegt).
- Der offizielle MW4-Lag-Artikel nennt nur Latenz, Paketverlust, Bandbreite, ISP-Routing, Ethernet, QoS: https://support.activision.com/modern-warfare-4/articles/improving-lag-and-performance-in-modern-warfare-4 (Abruf: erwähnt weder Jitter, Telemetrie, Texture Streaming, Crossplay noch Zahlenwerte).
- Mobilfunk mit CGNAT: Router-/Spiel-Einstellungen können den NAT-Typ nicht öffnen. Strict/Moderate ist dort der Normalfall. Eine NAT-Öffnung ist laut Guide "nicht erforderlich", betrifft vor allem Einladen/Beitreten bei Freunden, nicht den Ping.
- Routing-Tools (ExitLag, NoPing, GearUP): keine unabhängigen Messungen bei Mobilfunk gefunden, nur Herstellerbehauptungen. Nur ein eigener A/B-Test ist aussagekräftig.

## 1. Spiel-Einstellungen, die Bandbreite/Latenz beeinflussen
### On-Demand Texture Streaming (ODTS)
- Netduma (Warzone-Guide): Graphics-Menü, "On-Demand Texture Streaming" auf "Minimal" setzen, dann "Show More" und "Texture Cache Size" auf "Large" (braucht mehr Speicherplatz). Warzone habe die Funktion verpflichtend gemacht, Abschalten nicht mehr möglich. https://netduma.com/blog/warzone-lag-fix (Hersteller von Routern, aber Setting-Beschreibung konkret.)
- Dexerto-Titel: "Warzone is increasing lag with forced setting that can't be turned off". https://dexerto.com/call-of-duty/warzone-is-increasing-lag-with-forced-setting-that-cant-be-turned-off-2872342 (nur Titel/Treffer gesehen, Inhalt nicht abgerufen).
- MW4-spezifisch: GearUP (Hersteller) sagt, Streaming erzeuge zusätzlichen Netzverkehr, der auf überlasteten Leitungen mit dem Spiel konkurrieren kann; es gebe keine verlässliche MW4-spezifische Verbrauchszahl; Option "Minimal" testen. Exakte Menü-Pfade/Optionen für MW4 nannte die Seite NICHT (unbestätigt). https://www.gearupbooster.com/en/blog/fix-on-demand-texture-streaming-issues-in-modern-warfare-4.html
- Ein DMZ-Guide (Okt. 2026) rät laut Suchergebnis, ODTS zu reduzieren, falls vorhanden, und Hintergrundanwendungen/Downloads zu schließen: https://www.dtgre.com/2026/10/mw4-dmz-stuttering-fps-drops-pc-fixes.html (nur Suchsnippet). Die DMZ-Lag-Seite selbst erwähnt ODTS nicht: https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html
- Praktischer Schluss (eigene Einschätzung, plausibel aus obigen Quellen): ODTS auf Minimal + große Cache-Größe verlagert Last von der Leitung auf die Platte. Bei dünner Mobilfunk-Leitung relevant, bei voller Leitung kann Streaming Bursts auslösen.

### Hintergrund-Downloads / Updates / Shader
- Offiziell (BO7-Verbindungsartikel): bandbreitenintensive Aktivitäten wie Streaming oder große Downloads pausieren, Router neu starten, Server-Status prüfen, Ethernet statt WLAN. https://support.activision.com/black-ops-7/articles/connecting-to-black-ops-7
- Offiziell (MW4-Lag-Artikel): geteilte Netznutzung, Musik-/Videostreaming und Gameplay-Streaming (Upload) verbrauchen Bandbreite. https://support.activision.com/modern-warfare-4/articles/improving-lag-and-performance-in-modern-warfare-4
- DMZ-Guide: Spiel-Downloads, Windows-Updates, Cloud-Sync, große Uploads, Streaming auf anderen Geräten pausieren; Router normal neustarten, NIE Werksreset; QoS nur testen, wenn Router es kann, eine Änderung auf einmal. https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html
- Zu Battle.net-/Steam-spezifischen Hintergrund-Download-Einstellungen wurde keine MW4-Quelle gefunden. (Eigene Einschätzung: Steam "Downloads während Spielen zulassen" aus, Battle.net Update-Bandbreite begrenzen/pausieren, Windows Update Zustellungsoptimierung drosseln - nicht durch Quelle belegt.)
- Shader-Vorabkompilierung betrifft FPS/Stottern, nicht den Ping (laut DMZ-Guide: Grafik-Fehlersuche nur, wenn FPS fällt, Ping aber stabil bleibt). https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html

### Crossplay / Region / Ping-Limit
- Offiziell: Matchmaking versucht, nahe Spieler zusammenzubringen; Ping hängt vor allem von ISP und Entfernung ab, wenig direkt änderbar. https://support.activision.com/modern-warfare-4/articles/improving-lag-and-performance-in-modern-warfare-4
- Netduma: Kein In-Game-Region-Setting diskutiert; Serverauswahl nur über Router-Geo-Filter. https://netduma.com/blog/warzone-lag-fix
- Zu Crossplay-Einfluss auf Latenz und zu einer MW4-Ping-Obergrenze im Matchmaking wurde KEINE belastbare Quelle gefunden. Offen, im Spiel selbst prüfen.

## 2. Telemetrie-Anzeige und Bedeutung der Werte
### Wo die Option liegt (Quellen widersprechen sich)
- Netduma: Settings > Interface > Telemetry; "Server Latency", "Packet Loss", "Connection Meter" sind standardmäßig aus. https://netduma.com/blog/warzone-lag-fix
- Anderes Netduma-Material nennt "Essentials" für Ping/Paketverlust/FPS (Suchsnippet): https://netduma.com/blog/fix-black-ops-6-lag
- Ein DMZ-Guide nennt keinen Pfad: https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html
- Für MW4 im Spiel nachsehen (Settings > Interface bzw. Account/Telemetry).

### Bedeutung
- Latenz (Ping): Zeit in ms, die Daten zwischen Orten brauchen. https://support.activision.com/modern-warfare-4/articles/improving-lag-and-performance-in-modern-warfare-4
- Paketverlust: Daten kommen nicht an; nach Verlassen des eigenen Netzes nicht verhinderbar, nur das Risiko senken (Ethernet etc.); steigt bei Spitzenlast beim ISP. ebenda.
- Jitter / Latenzschwankung: Schwankung der Latenz über die Zeit. Beispiel NoPing: 30 ms mit 15 ms Jitter fühlt sich meist schlechter an als stabile 45 ms. https://noping.com/blog/good-call-of-duty-the-united-states (Hersteller)
- Packet Burst: Netduma beschreibt Burst als unregelmäßig ankommende Pakete (Rucken/Freezes), Paketverlust als nie angekommene Pakete (übersprungene/erratische Aktionen). https://netduma.com/blog/warzone-lag-fix (Hersteller). Die Anzeige "Latenzschwankungen" im Interface entspricht vermutlich der Jitter-Anzeige; eine offizielle Definition fehlt.
- Eingehend/ausgehend: Für die getrennte Anzeige des Paketverlusts (down/up) wurde KEINE offizielle Erklärung gefunden. (Eigene Einschätzung: Ausgehend-Verlust = Treffer/Eingaben kommen nicht beim Server an; eingehend = Servergeschehen kommt nicht bei dir an. Bei Mobilfunk meist Uplink-Verlust/Funkzelle.)

### Schwellen
- Offiziell: keine. Netduma: keine Zahlen. DMZ-Guide: keine Zahlen, nur Hinweis, einen eigenen Baseline-Wert zu bestimmen (Sprung 20 auf 400 ms = erheblich). https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html
- Faustwerte (eigene Einschätzung, NICHT quellenbelegt): Ping <60 ms gut, 60-100 spielbar, >100 spürbar; Jitter <10 ms gut, >30 ms problematisch; Paketverlust 0 % Ziel, >1 % dauerhaft oder einzelne Bursts >2-3 % sichtbar schlecht.
- Selbstmessung: `ping -n 30 1.1.1.1` testet nur einen öffentlichen Endpunkt, nicht den Spielserver; ICMP kann geblockt sein. https://www.dtgre.com/2026/10/mw4-dmz-packet-burst-rubberbanding-lag-fix.html

## 3. NAT-Typ, Ports, UPnP, IPv6
### Offiziell (Activision)
- NAT FAQ: Open / Moderate / Strict (Sony Typ 1/2/3). Moderate verbindet nur mit Moderate/Open, Strict nur mit Open. "Ultimately, an Open/Type 1 NAT will provide the best connection quality." Portweiterleitung kann Verbindungsgeschwindigkeit und Lobby-Wartezeit verbessern, besonders für Hosts. Kein Wort zu CGNAT/UPnP/Ping. https://support.activision.com/articles/port-forward-and-nat-faq (Seite ist laut Suchmetadaten ca. 7 Jahre alt.)
- Verbindungs-Artikel (BO7): Tests nacheinander: (1) Portweiterleitung aus, UPnP an; (2) UPnP aus, Portweiterleitung an, feste interne IP; (3) DMZ (nur Konsole). Keine Portliste dort. https://support.activision.com/black-ops-7/articles/connecting-to-black-ops-7
- Offizielle Portliste, neueste Titel (BO7, BO6, Warzone, MW3, MW2), PC: TCP 3074, 4000, 6112-6119, 20500, 20510, 27014-27050, 28960; UDP 3074, 3478, 4379-4380, 6112-6119, 20500, 20510, 27000-27031, 27036, 28960. https://support.activision.com/articles/ports-used-for-call-of-duty-games
- Eine MW4-Zeile fehlt auf dieser Seite (Abruf 17.11.2026: MW4 nur im Dropdown). Es gibt also keine offizielle MW4-Portliste; Annahme "wie neueste Titel" ist unbestätigt.
- Drittanbieter-Liste TroubleChute für MW4 (nicht offiziell): TCP 3074, 3075, 27015-27030, 27036-27037; UDP 3074, 4380, 27000-27036. Weicht von der offiziellen Liste der Vorgänger ab. https://hub.tcno.co/games/mw4/open-nat/ Dort: NAT-Öffnung nicht erforderlich, hilft v.a. beim Beitreten zu Freunden; mit UPnP Portweiterleitung meist überflüssig; Windows-Firewall ggf. nötig; NAT-Anzeige im Spiel: Settings > Accounts and Network > Network Info (nicht beim Streamen öffnen, zeigt öffentliche IP). CGNAT und IPv6 werden dort nicht behandelt.
- GearUP warnt, MW4-Portanforderungen könnten sich nach Launch ändern; alte Listen nicht blind übernehmen: https://www.gearupbooster.com/en/blog/optimize-call-of-duty-modern-warfare-4-nat-type.html (Hersteller)

### Mobilfunk + CGNAT (O2 Deutschland)
- O2-Community: Nutzer bekommen nur private Adresse (CGNAT), Portweiterleitung/Online-Gaming (PS5 NAT 2) funktioniert nicht richtig. https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/port-freigabe-bei-mobilfunktarif-auf-lte-5g-router-nicht-moeglich-625857 (Nutzerberichte, per Suche gesehen)
- O2-Moderator (Community, 2026): bei dem Vertrag sei "die öffentliche IPv4-Adresse aktiv"/aktiviert; Gerät muss den APN von "internet" auf "netpublic" ändern; Portfreigaben auf einem Handy-Hotspot seien "schwer bis nicht machbar". https://hilfe.o2online.de/packs-optionen-ausland-18/freischaltung-oeffentliche-ipv4-nat-typ-2-667318
- Gebühr: Suchergebnis nennt 49,99 EUR einmalig (Nutzerangabe); in dem abgerufenen Thread keine Gebührenaussage des Moderators; in einem anderen Thread Zustimmung wegen Gebühr nötig. Unverifiziert. https://hilfe.o2online.de/packs-optionen-ausland-18/oeffentliche-ipv4-adresse-beantragen-5g-router-cgnat-672045 (nicht abgerufen)
- Einzelfall: APN-Wechsel auf Netpublic reichte allein nicht für Portweiterleitung (2021). https://hilfe.o2online.de/archiv-o2-mobilfunk-61/public-ip-avoiding-cgnat-oeffentliche-ipv4-adresse-642290 (Suchsnippet)
- Kabel (nicht Mobilfunk): O2 Kabel nur DS-Lite, keine öffentliche IPv4. https://hilfe.o2online.de/packs-optionen-ausland-18/freischaltung-oeffentliche-ipv4-nat-typ-2-667318
- Andere Mobilfunk-Netze: EE-Mitarbeiter bestätigt CGNAT, NAT-Typ nicht änderbar. https://community.ee.co.uk/t5/Mobile-Broadband/5GEE-strict-NAT-on-warzone/m-p/1146806/highlight/true Allgemein: Hotspot liefert meist Strict; Test: NAT-Test am Hotspot und an Heim-WLAN mit UPnP vergleichen; Phone-Hotspots bieten kein UPnP/Portweiterleitung. https://natchecker.com/blog/mobile-hotspot-nat-type
- UPnP: hilft nur, wenn der Router eine öffentliche WAN-Adresse hat. Bei CGNAT (private 10.x/100.64.x WAN-Adresse) wirkt UPnP/Portweiterleitung nicht (aus O2-Berichten abgeleitet). Prüfen: WAN-IP des Routers/Hotspots vs. öffentliche IP (Network Info im Spiel).
- IPv6: Zu CoD und IPv6 wurde keine Quelle gefunden. (Eigene Einschätzung, unbelegt: CoD-Matchmaking läuft vermutlich über IPv4; IPv6 bringt NAT-Typ nicht auf Open.)
- Praktischer Schluss: Mit Strict/Moderate ist Spielen möglich, aber Beitreten/Hosten bei Freunden und Lobby-Zeiten können leiden (Activision NAT FAQ). Ping/Jitter/Verlust bei Mobilfunk kommen primär von Funkzelle/Signal/Auslastung (NoPing nennt Basisstationsauslastung, Signal, Innenempfang, Tageszeit: https://noping.com/blog/call-of-duty-exitlag-vs-noping-which-routing-technology-helps-reduce-lag-in-the-us, Hersteller).

## 4. Gaming-VPN/Routing-Tools bei Mobilfunk
- Unabhängige Messungen für ExitLag/NoPing/WTFast bei Mobilfunk/CGNAT: KEINE gefunden. Alles sind Herstellerblogs (NoPing/ExitLag/GearUP), die keine Verbesserung garantieren (Zitat sinngemäß: Ergebnisse hängen von ISP, Standort, Zielserver, Netzqualität, Tageszeit ab). https://noping.com/blog/call-of-duty-exitlag-vs-noping-which-routing-technology-helps-reduce-lag-in-the-us
- Empfohlene Teststruktur laut NoPing: ohne Tool spielen, dann mit Tool, gleiche Bedingungen, Ping-Schnitt, Spitzen, Jitter, Paketverlust vergleichen. ebenda. NoPing bietet 1 Tag kostenlosen Test, ExitLag Testphasen (laut Suchzusammenfassung).
- Unabhängige Desktop-VPN-Tests (nicht CoD, nicht Mobilfunk): VPN fügt meist +5 bis +15 ms hinzu; Paketverlust meist ~0; Ergebnisse widersprüchlich, viele Affiliate-Seiten. https://pcgamer.com/software/security/the-best-valorant-vpn (PC Gamer CS2-Test: 12,58 ms ohne VPN, 12,73 mit NordVPN). Fazit: Ein normales VPN verbessert den Ping nur, wenn der Standard-Weg des ISP schlecht geroutet ist.
- Logische Einordnung (eigene Einschätzung): Routing-Tools helfen bei schlechtem ISP-Peering/Routing zum Spielserver und können Pakete über mehrere Pfade duplizieren (NoPing "Multi Internet" bündelt Hotspot + Festnetz, Herstellerangabe). Gegen Funkzellen-Jitter und Uplink-Verlust auf der letzten Meile helfen sie nur begrenzt; der Tunnel kann zusätzlich einen stabilen Moderate NAT liefern (nur Herstellerbehauptung, nicht verifiziert).
- Regelrisiko: Activision hat keine eindeutige VPN-Bannregel; Sicherheits- und Durchsetzungsrichtlinie enthält Spoofing-Klausel, die Blogs auf VPN beziehen (Interpretation). Berichte über VPN-Nutzer in Bot-/Cheater-Lobbys. https://support.activision.com/fi/articles/call-of-duty-security-and-enforcement-policy ; https://insider-gaming.com/activision-cracking-down-on-call-of-duty-vpn-users/ ; https://stealthoptional.com/gaming/warzone-vpn-ban/ (Suchzusammenfassungen, niedrige Autorität). Vorsicht: VPN zur Region-Manipulation kann bannbar sein; kein VPN ist belegt "sicher".

## 5. Umsetzbare Checkliste (aus belegten Punkten)
1. Telemetrie im Spiel einschalten (Settings > Interface > Telemetry: Server Latency, Packet Loss, Connection Meter), Werte beim Auftreten notieren (Baseline ermitteln).
2. Server-Status prüfen (Activision Online Services).
3. ODTS auf "Minimal", Texture Cache Size "Large" (Netduma; MW4-Pfad im Spiel prüfen).
4. Hintergrund: Steam/Battle.net/Windows-Update/Cloud-Sync pausieren; Streaming auf anderen Geräten aus.
5. Ethernet/USB-Tethering statt WLAN-Hotspot (Activision empfiehlt Ethernet; USB-Tethering ist eigene Einschätzung); Router neu starten, nie Werksreset.
6. NAT-Typ unter Settings > Accounts and Network > Network Info prüfen. Bei O2-Mobilfunk: APN "netpublic" für öffentliche IPv4 versuchen (Moderator-Angabe); Gebühr unverifiziert.
7. Routing-Tool nur per eigenem A/B-Test (gleiche Zeit/Zelle/Server) mit Testphase; VPN-Regelrisiko beachten.

## 6. Lücken
- Keine offizielle MW4-Portliste, keine offizielle Telemetrie-Definition, keine Schwellenwerte, keine MW4-Angabe zu Crossplay/Ping-Limit, keine IPv6-Aussage, kein unabhängiger Mobilfunk-Test für Routing-Tools. Mehrere Quellen sind Hersteller (Netduma, NoPing, ExitLag, GearUP). exitlag.com/blog/modern-warfare-4-packet-burst gab HTTP 403 (nicht gelesen).

BEST-PRACTICES-KANDIDATEN:
- Telemetrie + ODTS-Minimal + Hintergrund-Traffic pausieren: https://netduma.com/blog/warzone-lag-fix ; https://support.activision.com/black-ops-7/articles/connecting-to-black-ops-7
- Offizielle CoD-Portliste (neueste Titel, ohne MW4-Zeile) und NAT-FAQ: https://support.activision.com/articles/ports-used-for-call-of-duty-games ; https://support.activision.com/articles/port-forward-and-nat-faq
- O2-Mobilfunk: öffentliche IPv4 über APN "netpublic" (Moderator-Aussage, Gebühr unklar): https://hilfe.o2online.de/packs-optionen-ausland-18/freischaltung-oeffentliche-ipv4-nat-typ-2-667318
- Routing-Tool nur per A/B-Test bewerten, Herstellerangaben nicht als Beleg: https://noping.com/blog/call-of-duty-exitlag-vs-noping-which-routing-technology-helps-reduce-lag-in-the-us

BUG-KANDIDATEN:
- Mobilfunk-CGNAT = Strict/Moderate NAT, Portweiterleitung/UPnP wirkungslos: https://community.ee.co.uk/t5/Mobile-Broadband/5GEE-strict-NAT-on-warzone/m-p/1146806/highlight/true ; https://hilfe.o2online.de/mobilfunk-zu-hause-homespot-router-software-internet-telefonie-21/port-freigabe-bei-mobilfunktarif-auf-lte-5g-router-nicht-moeglich-625857
- Warzone/CoD: erzwungenes On-Demand Texture Streaming erhöht Lag (nicht abschaltbar): https://dexerto.com/call-of-duty/warzone-is-increasing-lag-with-forced-setting-that-cant-be-turned-off-2872342
