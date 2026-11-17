# Bufferbloat über 5G/LTE: Test, Gegenmaßnahmen, Nachweis (Stand 17.11.2026)

Ausgangslage: Ping zum Router 1 ms, Ping ins Internet 400–1100 ms am Gaming-PC. Ziel: stabiler Ping für Call of Duty.
Quellentreue: Jede Aussage hat eine URL. Was ich nicht belegen konnte, steht unter "Lücken / nicht belegt".

## 1. Erst einordnen: Ist es Bufferbloat?

- Bufferbloat heißt: Die Latenz steigt nur unter Last stark an (Down- oder Upload sättigt die Leitung). Im Leerlauf ist der Ping normal. Die 400-1100 ms müssen daher im Leerlauf (nichts lädt) UND unter Last gemessen werden. Ist schon der Leerlauf-Ping 400+ ms, ist es kein Bufferbloat, sondern Funkproblem, schlechte Signallage oder Routing.
  - Prinzip "Latenz im Leerlauf vs. unter Last, Differenz = Bloat": https://sthq.pages.dev/guides/diagnostics/test-for-bufferbloat und https://evezone.evetech.co.za/build-lab/test-bufferbloat-before-blaming-server
- Vergleichsfall: LTE-Backhaul, Round-Trip über 400 ms bei vollem Uplink; nach SQM-Shaping rund 70–80 ms (Autor nennt es nutzbar, aber noch hoch; ein WireGuard-Tunnel über 1000 km war mit im Pfad): https://blog.wirelessmoves.com/?p=10666
- Weiterer Mobilfunk-Fall: Pingspitzen bis ca. 900 ms bei schwachem Signal; ein Upload-Limit von 80 % der erreichbaren Rate verringerte die Spitzen spürbar (Teltonika-Forum, Anwenderbericht): https://community.teltonika.lt/t/advice-needed-how-to-set-sqm-parameters-in-the-scenario-the-rutm30-is-constantly-moving/18903

## 2. Bufferbloat testen

Werkzeuge (Übersicht aus dem OpenWrt-Forum, https://forum.openwrt.org/t/2025-current-recommendations-for-bufferbloat-testing/241303):
- Waveform Bufferbloat Test (https://www.waveform.com/tools/bufferbloat): misst Latenz im Leerlauf, bei Download und bei Upload mit vielen Messpunkten, Verteilungen, CSV-Export. Kurze Tests, keine gleichzeitige Down+Up-Sättigung. Vergleicht Mittelwerte.
- Cloudflare (speed.cloudflare.com): ähnliche Phasen, zusätzlich Durchsatz je Objektgröße und Paketverlust, aber weniger Latenzmesspunkte (gleiche Quelle). Einzelner Anwenderbericht: Cloudflare zeigte höhere Latenz/Jitter als Waveform (25-30 ms vs. 11 ms Leerlauf) bei einer 1-Gbit-Leitung: https://forum.openwrt.org/t/high-ping-and-jitter-results-from-cloudflare-speed-test/250808
- LibreQoS-Test: zusätzlich gleichzeitiger Down+Up-Test und "virtueller Haushalt"; vergleicht 95. Perzentil gegen Leerlauf-Median, daher empfindlicher/strenger als Waveform (gleiche Quelle OpenWrt-Forum; https://libreqos.com/news/bloat-test).
- Stärkste Selbsttests: flent (braucht Gegenstelle), crusader (lokal, Down/Up/gleichzeitig getrennt), iperf2; networkQuality (Apple) auf Mac.
- Einfachster Praxis-Test: Dauerlast starten (Download oder Upload) und parallel `ping -t` auf einen nahen Server laufen lassen, mit Leerlauf vergleichen (https://forum.netgate.com/post/781874 und Zusammenfassungen).

Fallstricke:
- Am Kabel testen, Haushalt ruhig, im Hintergrund nichts laufen lassen; Browser-Artefakte (andere Browser-Engine gegentesten); Privacy-Einstellungen können Cloudflare-Tests stören (OpenWrt-Forum-Quelle oben).
- Auf Median/Mittel/75. Perzentil schauen, nicht auf das Maximum (OpenWrt-Forum-Quelle oben).
- Notenskala Waveform: Quellen widersprechen sich. Eine Drittseite nennt A+ <5 ms, A <30, B <60, C <200, D <400, F ab 400 ms zusätzliche Latenz (https://sthq.pages.dev/guides/diagnostics/test-for-bufferbloat), ob das Waveforms offizielle Skala ist, konnte ich nicht bestätigen (Waveform-Seite lieferte 403). Nach Änderungen mit demselben Test und Browser vergleichen.
- Wichtig für CoD: Ein A+ in Waveform ist keine Garantie fürs Spielgefühl. Fall: Flint 2, A+ (+0 ms), trotzdem schlechte Trefferregistrierung in CoD; GL.iNet-Support sah die Ursache nicht im Netzwerk, kein bestätigter Fix: https://forum.gl-inet.com/t/flint-2-owrt24-a-bufferbloat-0ms-but-bad-hitreg-in-cod-is-single-core-sqm-the-bottleneck/65965

## 3. Upload- vs. Download-Bufferbloat

- Upload: Der eigene Router kann ausgehende Pakete direkt verzögern/verwerfen, er kontrolliert die Warteschlange. Upload-Bloat ist meist das größere Problem und trifft auch den Download (ACKs): https://blog.apnic.net/2024/02/12/unbloating-the-buffers/ und https://www.gl-inet.com/blogs/blog/how-to-reduce-bufferbloat-with-sqm-on-glinet-routers
- Download: Pakete sind schon beim Router angekommen; er kann Sender nur indirekt per Drop/ECN bremsen. Lösung ist "Ingress-Shaping": Download-Limit etwas unter die Leitung setzen (üblich ca. 85–95 %), damit die Warteschlange im eigenen Router statt im Modem/Provider liegt (APNIC-Quelle; Zusammenfassung https://forum.netgate.com/post/781874).
- Messwerte APNIC-Test (Kabel/Ethernet, kein Mobilfunk): vor CAKE stieg der Ping bei paralleler Last von wenigen ms auf fast 300 ms, nach CAKE kein sichtbarer Einfluss der Last; Preis rund 10 % Durchsatz (305/48 -> 263/41 Mbit): https://blog.apnic.net/2024/02/12/unbloating-the-buffers/
- Bei Mobilfunk ist Ingress-Shaping schwieriger, weil die Rate schwankt (Tag/Nacht, Zellenlast): https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049 und https://forum.netgate.com/post/781874.

## 4. SQM/CAKE bei schwankender Bandbreite: cake-autorate

- cake-autorate stellt die CAKE-Rate (Up und Down) laufend nach Last und Laufzeit/Delay-Messung ein. Gedacht für LTE, 5G, Starlink und andere variable Leitungen; für feste Leitungen nicht nötig. Läuft auf OpenWrt, Asus Merlin und Debian-Familie. Master-Branch ist Entwicklungsstand, für den Alltag wird v3.2.1 (Branch v3.2) empfohlen: https://github.com/lynxthecat/cake-autorate
- Legt sich auf eine bestehende CAKE-Einrichtung (OpenWrt-SQM oder cake-qos-simple), ersetzt sie nicht: https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049
- Konfiguration: je Richtung min/base/max (min_dl/ul_shaper_rate_kbps = niedrigste Rate ohne Bloat, base = Normalwert, max = Maximum oder leicht darunter); Reflektoren = Ping-Ziele (Beispiel 1.1.1.1, 1.0.0.1, 8.8.8.8); Standard 20 Hz Pings (6 Pinger à 0,3 s), 3 Pinger = 10 Hz und etwa halbe CPU-Last: https://github.com/lynxthecat/cake-autorate
- Beispiel-Config für LTE-Strecke: base/min/max Download 100000/10000/200000 kbit/s, Upload 10000/2000/30000 kbit/s, OWD-Schwellen 40/80 ms; Autor nennt als Ergebnis auf LTE: ohne CAKE/autorate Note C, mit Defaults B, mit lockeren Schwellen (80 ms) wieder C ("etwas Bloat tolerieren, um Bandbreite nicht zu verschenken"); CPU rund 10-12 % aktiv auf einem RT3200: https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049
- Voraussetzungen/Fallen: bash und fping >= 5.3 (für One-Way-Delay, zeigt ob Up oder Down verursacht); Reflektoren möglichst nah am anderen Ende der Funkstrecke wählen; Bei Multi-WAN sicherstellen, dass Pings über das richtige Interface gehen: https://community.teltonika.lt/t/advice-needed-how-to-set-sqm-parameters-in-the-scenario-the-rutm30-is-constantly-moving/18903 und https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049
- Datenverbrauch: Dauerhafte Ping-Last erzeugt Hintergrundtraffic (relevant bei Datenvolumen-Tarif): https://docs.gl-inet.com/router/en/4/interface_guide/sqm/
- Erfahrung/Negativ: Manche Anwender sehen niedrigere Geschwindigkeiten oder haben mit autorate-ingress keine guten Erfahrungen (Einzelberichte): https://forum.mikrotik.com/t/cake-autorate-ingress-turns-speeds-into-molasses/151372
- Reiner CAKE ohne autorate bei 5G (T-Mobile-Fall): rund 25-35 % Durchsatzverlust (770/170 Shaper vs. ca. 680/130 ohne SQM -> ca. 600/80); keine Vorher/Nachher-Latenz berichtet; Experte moeller0 empfiehlt für variable Links cake-autorate oder sqm-autorate; Raspberry Pi 4 CPU-Spitzen 74-83 % bei ca. 700 Mbit: https://forum.openwrt.org/t/sqm-settings-for-5g-cellular-wan-connection/154585
- Einfache Alternative ohne Skript: fixes Limit auf "Worst-Case-Geschwindigkeit" (Upload ca. 80 % der erreichbaren Rate, Teltonika-Anwender oben) oder ein Skript, das per Speedtest nachregelt (Teltonika-Support-Vorschlag, kostet Datenvolumen): https://community.teltonika.lt/t/advice-needed-how-to-set-sqm-parameters-in-the-scenario-the-rutm30-is-constantly-moving/18903

## 5. OpenWrt-Router hinter dem 5G-Router

- Das 5G-Gerät sollte in Bridge-/IP-Passthrough-Modus laufen; ein GL.iNet-Forenbeitrag nennt: Modem im Router-Modus kann die Latenz erhöhen. Mehrere Anwender betreiben einen OpenWrt-Router (auch Raspberry Pi) zwischen Zyxel/5G-Gerät im Passthrough und Haupt-Router mit CAKE. Ein Anwender nutzt LAN-Port des LTE-Routers an WAN von OpenWrt und berichtet, es laufe sehr gut; Doppel-NAT lasse sich vermeiden: https://forum.openwrt.org/t/using-openwrt-inbetween/247512 (Zusammenfassung aus Suche; Seite selbst nicht geladen).
- Wenn ein Gateway vor dem Router sitzt, shaped CAKE nur die Strecke Router-Gateway; die Warteschlange im Gateway bleibt. Abhilfe: Bridge-Mode: https://forum.netgate.com/post/781874
- Beachte: Shaping hilft nur, wenn der Engpass (Funkstrecke) durch das Limit in den eigenen Router verlegt wird; deshalb Rate etwas unter der echten Funkrate (ohne autorate geraten -> Verschwendung oder Bloat).
- Geräte (GL.iNet, laut offizieller SQM-Seite mit SQM + Cake Autorate-Schalter ab Firmware 4.11 unterstützt): GL-MT6000 Flint 2, GL-BE9300 Flint 3, GL-BE14000 Flint 4, GL-MT3000 Beryl AX, GL-MT5000 Brume 3, GL-BE6500 Flint 3e, GL-BE3600 Slate 7, GL-BE10000 Slate 7 Pro, GL-MT3600BE Beryl 7, GL-E5800 Mudi 7. Nicht unterstützt (21 Modelle), z. B. GL-MG1300 Mango 2. Einschränkungen: SQM schaltet Network Acceleration (Hardware-Offload) ab, funktioniert nicht im Drop-in-Gateway-Modus, nicht zusammen mit QoS; Idle-Schwelle von Cake Autorate max. 25 % des Limits; Concurrent probes <= Anzahl Probe-Server: https://docs.gl-inet.com/router/en/4/interface_guide/sqm/
- Leistungsgrenze: Flint 2 mit CAKE hielt A+ nur bis ca. 300 Mbit (Single-Core); für einen 5G-Anschluss (meist < 300 Mbit) ausreichend; Packet Steering kann mehr Durchsatz bringen: https://forum.gl-inet.com/t/flint-2-owrt24-a-bufferbloat-0ms-but-bad-hitreg-in-cod-is-single-core-sqm-the-bottleneck/65965
- Kosten-Nutzen: Für Sub-100-Mbit-Mobilfunk reicht laut cake-autorate-Autor schon ein RT3200 (Verbindungen >350 Mbit brauchen deutlich stärkere Hardware): https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049. Ich habe keine Preise geprüft.
- Laut OpenWrt-Doku gibt es ab 25.12 zusätzlich cake-mq (Mehrkern-CAKE) (aus Suchergebnis zusammengefasst, nicht selbst verifiziert): https://openwrt.org/docs/guide-user/network/traffic-shaping/sqm

## 6. Bandbreitenbegrenzung pro Gerät / Windows-seitig

Grundsatz: Bei schwankender Mobilfunkrate ist ein fixes PC-Limit nur eine Notlösung; es ersetzt kein SQM, weil es keine Warteschlange verwaltet (https://noping.com/blog/sqm-and-qos-for-gaming-control-bufferbloat-under-load-2, Aussage "nicht so wirksam wie SQM" laut Suchzusammenfassung).

### Windows QoS-Richtlinie (Drosselrate)
- Nur ausgehender Verkehr (Upload): "Specify Throttle Rate" begrenzt den ausgehenden Traffic; Wert größer 1, Einheiten KBps/MBps (Achtung Bytes, nicht Bits): https://learn.microsoft.com/en-us/windows-server/networking/technologies/qos/qos-policy-manage
- Eingehend (Download) lässt sich nur indirekt über "Inbound TCP Traffic" (TCP-Empfangsfenster 64 KB/256 KB/1 MB/16 MB) beeinflussen, ist kein präzises Rate-Limit: gleiche Microsoft-Seite.
- PowerShell: `New-NetQosPolicy -Name "X" -AppPathNameMatchCondition "app.exe" -ThrottleRateActionBitsPerSecond <Bit/s> -PolicyStore ActiveStore` (ActiveStore = nach Neustart weg; Parameter in Bit/s): https://learn.microsoft.com/en-us/powershell/module/netqos/new-netqospolicy
- Pro Verkehr gilt nur eine Richtlinie, Drosselraten addieren sich nicht (Microsoft-Seite "Manage QoS Policy" oben).
- Zuverlässigkeit: Berichte widersprechen sich (ein Server-2012-Anwender sah keine Drosselung): https://serverfault.com/questions/731002 (Link aus Suchergebnis, nicht geladen). Selbst verifizieren.
- Hinweis: Das "QoS-Paketplaner reserviert 20 %"-Thema und DSCP helfen nicht gegen Bufferbloat; DSCP wirkt nur, wenn der Router es auswertet (https://learn.microsoft.com/en-us/windows-server/networking/technologies/qos/qos-policy-manage).

### NetLimiter
- Pro App Download-/Upload-Limit in der Activity-Ansicht (DL Limit/UL Limit): https://netlimiter.com/docs/basic-concepts/limits
- Empfehlungen aus Anleitungen: Upload/Download auf ca. 85-95 % der gemessenen Rate (Community/Vendor, keine Messung): https://noping.com/blog/what-is-bufferbloat und https://www.exitlag.com/blog/how-fix-bufferbloat
- Berichtet: NetLimiter-Limits zeigten zu Beginn eine Spitze deutlich über dem Limit, vermutlich Lag-Spitzen in Spielen (Einzelbericht, Suchzusammenfassung).
- Tuning-Tipp aus Forum: Download unbegrenzt lassen und nur Upload tunen, dabei Upload-aktive Latenz beobachten; Ziel ca. 60-100 ms für die meisten Spiele (Netduma-Forum, Suchzusammenfassung): https://forum.netduma.com/topic/54940-r3-how-to-get-a-better-latency-score-on-waveform
- Es gibt keine Quelle, die NetLimiter-Priorität gegen Bufferbloat misst (Lücke).

### Typische Verursacher auf dem PC begrenzen
- Steam: Einstellungen > Downloads > "Bandbreite begrenzen auf" (Einheit hängt von der Anzeige-Option kbps/Bit ab; einzelne Berichte, dass das Limit nicht eingehalten wurde): https://help.steampowered.com/faqs/view/163C-7C89-406E-2F63 und https://windowsreport.com/steam-download-bandwidth-limit/
- Windows Update / Zustellungsoptimierung: Einstellungen > Windows Update > Erweiterte Optionen > Zustellungsoptimierung > Erweiterte Optionen; Download-Limit (absolut in Mbit/s oder % der gemessenen Bandbreite) und Upload-Limit; Peer-Upload ausschalten ist hier die wirksamste Maßnahme: https://support.microsoft.com/de-DE/Windows/Deployment/Updates-Lifecycle/delivery-optimization-in-windows und https://geekrewind.com/how-to-set-bandwidth-limits-for-uploads-with-delivery-optimization-in-windows-11/
- OneDrive: Einstellungen > Synchronisieren und Sichern > Erweiterte Einstellungen; Upload "automatisch anpassen" oder fest (Minimum 50 KB/s, Maximum 100.000 KB/s); Download nur fest oder unbegrenzt: https://support.microsoft.com/en-us/article/71cc69da-2371-4981-8cc8-b4558bdda56e
- Google Drive für Desktop: Einstellungen > Einstellungen > Erweiterte Einstellungen, Download-/Upload-Rate in KB/s: https://support.google.com/drive/answer/16631477?hl=en
- Hinweis: Bei Mobilfunk mit ca. 2–20 Mbit/s Upload reicht schon ein einzelnes Hintergrund-Upload (Cloud, Telemetrie), um den Upload zu sättigen (siehe Teltonika-Fall oben).

## 7. Was bringt es nachweislich an Latenz? (Belegte Zahlen)

| Fall | Vorher | Nachher | Quelle |
|---|---|---|---|
| LTE, OpenWrt SQM (WRT-1200ac), Upload voll | RTT > 400 ms | ca. 70-80 ms (inkl. VPN-Strecke) | https://blog.wirelessmoves.com/?p=10666 |
| Kabel/Ethernet, CAKE, Flent | bis fast 300 ms | keine sichtbare Last-Abhängigkeit, ca. -10 % Durchsatz | https://blog.apnic.net/2024/02/12/unbloating-the-buffers/ |
| Mobilfunk Teltonika, Upload-Cap 80 % | Spitzen bis ca. 900 ms | Spitzen "spürbar" kleiner (keine Zahl) | https://community.teltonika.lt/t/advice-needed-how-to-set-sqm-parameters-in-the-scenario-the-rutm30-is-constantly-moving/18903 |
| LTE, cake-autorate (RT3200) | Waveform Note C | B mit Defaults, wieder C mit lockeren Schwellen | https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049 |
| T-Mobile 5G, reines CAKE | - | Durchsatz -25..35 %, keine Latenzzahl | https://forum.openwrt.org/t/sqm-settings-for-5g-cellular-wan-connection/154585 |

## 8. Lücken / nicht belegt

- Keine harte Vorher/Nachher-Latenzmessung für cake-autorate speziell auf 5G gefunden (nur Waveform-Noten und Anekdoten).
- Waveforms eigene Notenskala/Messmethode nicht bestätigt (403).
- Das 5G-Gerät des Nutzers (Modell) ist unbekannt: ob es Bridge/IP-Passthrough kann, ist die Voraussetzung für den OpenWrt-Plan.
- Windows-QoS-Drosselung zuverlässig? Berichte widersprüchlich; NetLimiter-Priorität nicht gemessen.
- Preise der GL.iNet-Geräte nicht geprüft. Seite openwrt.org/docs/.../sqm und Reddit-Thread waren per WebFetch nicht abrufbar (Zusammenfassungen nur aus Suchergebnissen).
- Eine eigene Messung der 400-1100 ms im Leerlauf vs. unter Last fehlt noch: Das entscheidet, ob es überhaupt Bufferbloat ist.

## 9. Konkrete Reihenfolge für Frank (umsetzbar)

1. Messen: Im Leerlauf `ping -t 1.1.1.1` (Router aus dem Weg, nichts lädt); dann Waveform + Cloudflare am Kabel; Download- UND Upload-aktive Latenz notieren, zusätzlich Durchsatz.
2. Wenn Leerlauf schon hoch: Signal/Band/Antenne/anderer Standort klären (kein Bufferbloat-Problem).
3. Wenn nur unter Last hoch: Hintergrund-Uploads/-Downloads begrenzen (Steam, Windows-Update-Zustellungsoptimierung inkl. Peer-Upload aus, OneDrive, Google Drive), neu messen.
4. Windows-seitig Upload-Limit per QoS-Richtlinie oder NetLimiter auf ca. 80-90 % der Mindestrate testen; Download unbegrenzt lassen und Upload-aktive Latenz beobachten.
5. Dauerlösung: 5G-Gerät auf Bridge/IP-Passthrough, dahinter OpenWrt-/GL.iNet-Router (z. B. Flint 2/Beryl AX) mit SQM+CAKE, bei schwankender Rate cake-autorate (min/base/max aus eigenen Messungen), Ping-Ziele nah am Mobilfunknetz, Datenverbrauch der Probes beachten.
6. Erwartung realistisch: SQM kann Bloat von mehreren hundert ms auf zweistellige Zusatz-ms senken (Belege bei LTE: 400 -> 70-80 ms), die Grundlatenz der Funkstrecke bleibt; Durchsatzverlust 10-35 %.

BEST-PRACTICES-KANDIDATEN:
- Bufferbloat testen: Waveform + Cloudflare + LibreQoS, Leerlauf vs. Last, Median/75. Perzentil, am Kabel: https://forum.openwrt.org/t/2025-current-recommendations-for-bufferbloat-testing/241303
- Variable Mobilfunk-Leitung: CAKE + cake-autorate statt fester SQM-Rate; min/base/max aus bloat-freien Messungen, Bridge/Passthrough am Modem: https://github.com/lynxthecat/cake-autorate und https://forum.openwrt.org/t/cake-w-adaptive-bandwidth/191049
- GL.iNet SQM: Cake Autorate ab FW 4.11, schaltet Network Acceleration ab, nicht mit QoS/Drop-in-Gateway: https://docs.gl-inet.com/router/en/4/interface_guide/sqm/
- Windows: New-NetQosPolicy drosselt nur ausgehend, Bit/s, ActiveStore nicht persistent: https://learn.microsoft.com/en-us/powershell/module/netqos/new-netqospolicy und https://learn.microsoft.com/en-us/windows-server/networking/technologies/qos/qos-policy-manage
- Hintergrund-Verkehr auf dem PC begrenzen (Steam, Zustellungsoptimierung inkl. Peer-Upload, OneDrive, Google Drive): https://support.microsoft.com/en-us/article/71cc69da-2371-4981-8cc8-b4558bdda56e und https://support.google.com/drive/answer/16631477?hl=en

BUG-KANDIDATEN:
- A+ im Bufferbloat-Test, aber trotzdem schlechte CoD-Trefferregistrierung (Flint 2, Single-Core-CAKE, A+ nur bis 300 Mbit, kein bestätigter Fix): https://forum.gl-inet.com/t/flint-2-owrt24-a-bufferbloat-0ms-but-bad-hitreg-in-cod-is-single-core-sqm-the-bottleneck/65965
- CAKE bei 5G kostet 25-35 % Durchsatz bei geratener fixer Rate: https://forum.openwrt.org/t/sqm-settings-for-5g-cellular-wan-connection/154585
- cake-autorate-ingress macht Leitung langsam (Einzelbericht, MikroTik): https://forum.mikrotik.com/t/cake-autorate-ingress-turns-speeds-into-molasses/151372
- Windows-QoS-Throttle drosselt laut Bericht nicht (Server 2012, widersprüchlich): https://serverfault.com/questions/731002
- NetLimiter-Limit mit Anfangsspitze über Limit (Einzelbericht, Suchzusammenfassung, keine Original-URL geprüft): KEINE belastbare URL
