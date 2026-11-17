# Kurzcheck Best Practices: Gaming über 5G-Router (Stand 17.11.2026, Tenda 5G03 V05.03.01.26, O2 5G SA)
- Messen: Ping Router + 1.1.1.1, Leerlauf UND Last. Signal im Tenda: Router-Symbol → System Status, Ziel SINR ≥ 10 dB.
- Reihenfolge: LAN-Kabel > 5 GHz (Kanal 36–48) > Downloads stoppen (Xbox-App/Gaming Services, Steam, Drive, Windows Update) > Standort/Fenster > externe n78-Antenne (TS-9) > SQM/CAKE-Router.
- CoD: Texture Streaming auf Minimal, Telemetrie an; NAT strikt (O2-CGNAT) ist für Ping egal.
- Wirkungslos: Nagle/TCP-Tweaks (CoD = UDP), DNS-Wechsel, Gaming-VPN ohne A/B-Test.
- AX201: Treiber aktuell, 5 GHz bevorzugen, Roaming niedrigste Stufe (Admin).
Volltext: `netzwerk-latenz-5g-router.md`
