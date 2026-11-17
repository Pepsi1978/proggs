# Online-Gaming über 5G-Router: Ping, Jitter, Paketverlust — Best Practices

> Stand: 17.11.2026 17:54 · Versions-Anker: Tenda 5G03 Firmware V05.03.01.26_multi, O2 Deutschland 5G SA (N28 + zeitweise N78), Windows 11 Build 26200, Intel AX201 Treiber 23.170.0.1 (aktuell 24.80.0.2), Gaming Services 39.118.30001.0, CoD MW4 (Steam)
> Anlass: Franks Spiel-PC (MSI MS-7D32, RTX 5090) — CoD-Telemetrie zeigt hohen Ping, Paketverlust, Latenzschwankung
> Gegenstück: `bugs/gaming/netzwerk-latenz-5g-router.md` · Kurzcheck: `netzwerk-latenz-5g-router-kurzcheck.md` · Rohrecherchen: `recherchen/netzwerk-latenz-5g/` (8 Sonnet-Researcher)

## 1. Erst messen, dann ändern
- Getrennt messen: `ping -n 40 192.168.0.1` (Heimnetz) und `ping -n 40 1.1.1.1` (Internet), jeweils **im Leerlauf und unter Last**. Leerlauf gut, Last schlecht = Bufferbloat (PC-/Haus-Seite lösbar). Schon Leerlauf schlecht = Funkzelle (nur Router/Antenne/Standort). `extern` https://www.waveform.com/tools/bufferbloat
- Franks Messung 17.11.2026: Heimnetz 1 ms; Internet unter Download-Last (gamingservices, 2,4 GHz) Ø 323 / min 34 / max 1547 ms; echter Leerlauf (Router „Current Speed“ ~50 kbit/s, 5 GHz) Ø 30 / min 13 / max 118 ms → Hauptursache Bufferbloat, Rest Funkzelle (SINR 2 dB). Leerlauf erst messen, wenn „Current Speed“ im Tenda nahe null steht.
- Signalwerte im Tenda: Internet Status → Klick auf das Router-Symbol „5G03“ → System Status (SS-RSRP, SS-RSRQ, SS-SINR, CQI, Access Band). Zielwerte: **SINR ≥ 10 dB**, RSRP besser als −95 dBm, RSRQ besser als −12 dB. SINR/RSRQ zählen mehr als RSRP. `extern` https://wiki.teltonika-networks.com/view/Mobile_Signal_Strength_Recommendations

## 2. Hebel nach Wirkung (größter zuerst)
1. **LAN-Kabel** PC ↔ Router (Tenda hat 2 Gigabit-Ports; Franks PC hat Intel I225-V). Activision empfiehlt Ethernet ausdrücklich. `offiziell` https://support.activision.com/black-ops-7/articles/connecting-to-black-ops-7
2. **Kein WLAN-Kabel möglich → 5 GHz** mit eigener SSID, fester Kanal 36–48 (kein DFS). 2,4 GHz nur für IoT (SwitchBot, Kaffeemaschine, Drucker), 20 MHz. Powerline-Alternative: `extern` https://www.netzwelt.de/news/92347-antwortzeiten-ping-werte-aktueller-powerline-adapter.html
3. **Hintergrund-Downloads beim Spielen stoppen**: Xbox-App/Gaming Services (Shader- und Spiel-Downloads, bis zu 12 parallele HTTP-Ströme), Steam-Downloadlimit, Google Drive Bandbreitenlimit, Windows Update Übermittlungsoptimierung (Einstellungen → Windows Update → Erweitert → Übermittlungsoptimierung → Bandbreite begrenzen). Bei schmaler Mobilfunkleitung füllt **ein** Download die Warteschlange.
4. **Antenne/Standort**: Tenda-Standorttest (Internet Status → search/start test) nutzen; Fenster zur Mastseite, hoch, weg von Metall/Wärmeschutzglas. Mast suchen: https://www.cellmapper.net, BNetzA-EMF-Karte https://emf3.bundesnetzagentur.de/karte/default.aspx
5. **Externe n78-Antenne** an den 2 TS-9-Buchsen des 5G03 (laut Handbuch nur für n77/n78). Sinnvoll, wenn N78 nur zeitweise per Carrier Aggregation dazukommt: Richtantenne 7–14 dBi, beide MIMO-Ports, 90° versetzt, kurzes Low-Loss-Kabel. `offiziell` Tenda 5G03 Handbuch V1.0 · `extern` https://lte-anbieter.info/ratgeber/lte-empfang-verbessern.php · https://docs.gl-inet.com/router/de/4/tutorials/understanding_external_cellular_antennas/
6. **SQM/CAKE** (Dauerlösung gegen Bufferbloat): OpenWrt/GL.iNet-Router (Firmware ≥ 4.11 mit Cake-Autorate-Schalter, z. B. Flint 2, Beryl AX) hinter dem 5G-Router. Mit SQM sank in einem LTE-Bericht der Ping unter Volllast von > 400 auf ~75 ms; kostet 25–35 % Durchsatz. `extern` https://github.com/lynxthecat/cake-autorate · https://docs.gl-inet.com/router/en/4/interface_guide/sqm/
7. **Netzmodus testen** (SA / NSA / 4G Only) per 20–30-min-Ping. Bei Frank: nur 5G SA brauchbar, NSA und 4G schlechter (Nutzer-Erfahrung 17.11.2026).

## 3. Windows (Intel AX201)
- Wirksam/risikoarm: Treiber aktualisieren (Intel 24.80.0.2), „Bevorzugtes Band“ = „3. 5 GHz-Band bevorzugt“, „Roaming-Dynamik“ = „1. Am niedrigsten“ (stationärer PC; braucht Admin). WLAN-Profilpriorität ohne Admin: `netsh wlan set profileorder name="<5GHz-SSID>" interface="WLAN" priority=1`.
- **Wirkungslos für CoD**: Nagle/TcpAckFrequency/TCPNoDelay (CoD = UDP), DNS-Wechsel (ändert Spielping nicht), Durchsatzbooster/U-APSD. NetworkThrottlingIndex nur A/B-Test. `extern` https://www.makeuseof.com/i-tested-the-windows-network-tweaks-gamers-swear-by-and-only-one-lowered-my-ping/
- QoS-DSCP über Mobilfunk wird meist vom Netz verworfen; Windows bildet DSCP 46 auf WMM-Priorität 5 statt 6 ab (Workaround `New-NetQosPolicy … -PriorityValue8021Action 6`).

## 4. Call of Duty
- Telemetrie: Einstellungen → Interface → Telemetry: Server-Latenz, Paketverlust, Verbindungsanzeige an. Activision nennt keine Schwellen; Faustwerte (eigene Einschätzung): Ping < 60 ms gut, Jitter > 30 ms problematisch, Verlust > 1 % schlecht.
- Grafik: „On-Demand Texture Streaming“ auf Minimal (lädt sonst während des Spiels nach). `extern` https://netduma.com/blog/warzone-lag-fix · Ports: `offiziell` https://support.activision.com/articles/ports-used-for-call-of-duty-games
- NAT „strikt“ bei O2 = CGNAT; UPnP/Portweiterleitung wirkungslos. Öffentliche IPv4 nur per APN `netpublic` (einmalig 49,99 €, laut O2-Community, **nicht in 5G SA**). Für Matchmaking-Ping irrelevant. `extern` https://hilfe.o2online.de/packs-optionen-ausland-18/keine-oeffentliche-ipv4-trotz-buchung-und-nutzung-apn-netpublic-636077 · https://support.activision.com/articles/port-forward-and-nat-faq
- Gaming-VPNs (ExitLag, NoPing): bei Mobilfunk kein unabhängiger Beleg, nur A/B-Test; Bann-Risiko-Klausel beachten.

## 5. O2
- APN Vertrag `internet`, Prepaid `pinternet.interkom.de`; kein Low-Latency-APN. IPv6 eingehend geblockt.
- Berlin: seit 05/2026 bekannte O2-Zellprobleme mit Paketverlust (O2-Community, ohne Lösung). Vergleich mit Telekom/Vodafone-Test-SIM im selben Router lohnt.
