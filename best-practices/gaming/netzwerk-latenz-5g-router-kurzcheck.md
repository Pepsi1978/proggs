# Kurzcheck Best Practices: Gaming über 5G-Router (Stand 17.11.2026, Tenda 5G03 V05.03.01.26, O2 5G SA)
- Messen: Ping Router + 1.1.1.1, Leerlauf UND Last. Signal im Tenda: Router-Symbol → System Status, Ziel SINR ≥ 10 dB.
- Reihenfolge: LAN-Kabel > 5 GHz (Kanal 36–48) > Downloads stoppen (Xbox-App/Gaming Services, Steam, Drive, Windows Update) > Standort/Fenster > externe n78-Antenne (TS-9) > SQM/CAKE-Router.
- CoD: Texture Streaming auf Minimal, Telemetrie an; NAT strikt (O2-CGNAT) ist für Ping egal.
- Wirkungslos: Nagle/TCP-Tweaks (CoD = UDP), DNS-Wechsel, Gaming-VPN ohne A/B-Test.
- AX201: Treiber aktuell, 5 GHz bevorzugen, Roaming niedrigste Stufe (Admin).
- Netzart nach SINR wählen, nicht nach Balken (18.11.2026): SA N28 bei SINR 19 dB = bester Ping (15–18 ms); 4G B3+B1 bei SINR 27 dB = 21–34 ms; SA/NSA bei SINR ≤ 2 dB = 50 ms + Verluste.
- Mast finden: `https://api.cellmapper.net/v6/getTowerInformation?MCC=262&MNC=3&Region=0&Site=<eNodeB>&RAT=LTE` (eNodeB = ECI / 256), Gegenprobe OSM/Nominatim. Franks Mast: eNB 21118, Berliner Straße Neuenhagen-Bollensdorf.
- Steam „Shader Pre-Caching“ aus; Downloads nur ohne Match (sonst Ping ~160 ms); N28 schafft nur ~3 MB/s, schnell nur N78 oder 4G-CA.
Volltext: `netzwerk-latenz-5g-router.md`
