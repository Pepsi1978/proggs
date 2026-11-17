# Kurzcheck Bugs: Gaming über 5G-Router (Stand 17.11.2026, Tenda 5G03 V05.03.01.26, O2 5G SA)
- B1 Ping 400–1500 ms bei Download → Bufferbloat → Downloads pausieren, SQM/CAKE.
- B2 Store-Fenster beim CoD-Start → entweder Shader-Download (Gaming Services) oder fehlende Game Bar bei aktivem GameDVR („ms-gamingoverlay“) → Fenstertext prüfen; Game Bar installieren/Aufnahme aus bzw. Repair Tool.
- B3 AX201-Ping-Spitzen alle ~10 min → WLAN-Hintergrundscan → `netsh wlan set autoconfig enabled=no` beim Spielen oder Kabel.
- B4 PC auf 2,4 GHz statt 5 GHz → `netsh wlan set profileorder … priority=1`.
- B5 DSCP 46 → WMM 5 statt 6 → `-PriorityValue8021Action 6`.
- B6 NAT strikt bei O2 → CGNAT → nur APN netpublic (nicht in SA).
- B7 Tenda-Band-Lock mit allen Häkchen = wirkungslos.
- B8 „Signal Good“ aber SINR 2 dB → Zellstörung, nicht Feldstärke.
Volltext: `netzwerk-latenz-5g-router.md`
