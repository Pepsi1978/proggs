# Bug-Almanach: Gaming über 5G-Router (Ping, Jitter, Paketverlust)

> Stand: 17.11.2026 17:54 · Versions-Anker: Tenda 5G03 V05.03.01.26_multi, O2 5G SA, Windows 11 26200, Intel AX201 23.170.0.1, Gaming Services 39.118.30001.0
> Gegenstück: `best-practices/gaming/netzwerk-latenz-5g-router.md` · Kurzcheck: `netzwerk-latenz-5g-router-kurzcheck.md`

## B1 Ping springt auf 400–1500 ms, sobald der PC lädt (Bufferbloat)
- Symptom: Heimnetz-Ping 1 ms, Internet-Ping unter Last Ø 323 / max 1547 ms, CoD meldet Latenzschwankung.
- Ursache: Download füllt die Warteschlange der schmalen Funkstrecke; bei Frank `gamingservices` (Xbox-App) mit 12 parallelen HTTP-Strömen zu Fastly.
- Betroffen: jede Mobilfunk-/FWA-Leitung ohne SQM.
- Fix: Downloads beim Spielen pausieren bzw. begrenzen; dauerhaft SQM/CAKE (cake-autorate). Quelle: https://github.com/lynxthecat/cake-autorate

## B2 Gaming-Services öffnet beim CoD-Start ein Microsoft-Store-Fenster
- Symptom: Bei jedem Start von CoD (Steam) erscheint ein kleines Store-Verbindungsfenster; Wegklicken lässt es beim nächsten Start wiederkommen.
- Ursache (Hypothese, nicht offiziell): Advanced Shader Delivery — vorkompilierte Shader über Xbox-App/Gaming Services (`C:\XboxGames\Shaders`). Activision empfiehlt die Xbox-PC-App plattformübergreifend.
- Fix: Fenster nicht schließen, sondern den Shader-/Store-Vorgang einmal durchlaufen lassen (nicht während Online-Matches). Hilft das nicht: `wsreset.exe` → Gaming Services Repair Tool (Xbox-App) → als Admin `Get-AppxPackage Microsoft.GamingServices | Remove-AppxPackage -AllUsers`, danach `start ms-windows-store://pdp/?productid=9MWPM2CQNLHN` (zuletzt, kann Xbox-App-Installationen beschädigen). Beleg bei Frank 17.11.2026: `C:\XboxGames\Shaders\Steam\1938090` (Steam-ID von CoD) um 17:51 geändert, während gamingservices lud. Quelle: https://devblogs.microsoft.com/directx/advanced-shader-delivery-is-available-now-across-hardware · https://support.xbox.com/en-US/help/games-apps/troubleshooting/gaming-services-repair-tool
- **Zweite Möglichkeit:** Xbox Game Bar (`Microsoft.XboxGamingOverlay`) fehlt, aber `GameDVR_Enabled=1` → Windows öffnet beim Spielstart ein kleines Fenster „neue App für ms-gamingoverlay-Link“ mit Store-Verweis. Unterscheiden am Fenstertext. Fix: Game Bar aus dem Store installieren **oder** Einstellungen → Spielen → Aufnahmen/Game Bar ausschalten.

## B2a Gaming Services lassen sich nicht sperren, hartes Beenden startet sie sofort neu
- `Set-Service GamingServices -StartupType Disabled` → „Zugriff verweigert“ (geschützter Paket-Dienst), auch als Admin.
- `Stop-Process gamingservices` wertet der Dienststeuerungs-Manager als Absturz → Wiederherstellung startet den Dienst sofort neu.
- Fix: `Stop-Service -NoWait` + `WaitForStatus('Stopped', 15 s)`, bei Wiederanlauf erneut stoppen (umgesetzt in `Werkzeuge/spielmodus/waechter.ps1`, verifiziert 17.11.2026).
- Während CoD läuft, startet CoD die Gaming Services selbst neu (~30 Stopp/Start-Runden in 20 min) → dann nicht bekämpfen, nur vor/nach CoD stoppen.
- Messung im Spiel nach allen Maßnahmen (17.11.2026, 20 min DMZ): Ping Ø 22–25 ms, 95 % < 36 ms, Jitter 6–9 ms. Restspitzen nur bei CoD-Nachladen zwischen Runden (3–7,7 Mbit/s) und Uploads laufender KI-Sitzungen (Claude Code sendet je Antwort den ganzen Verlauf).

## B3 Intel AX201/AX210/AX211: Ping-Spitzen etwa alle 10 Minuten
- Ursache: WLAN-Hintergrundscan von Windows; die Treiberoption „Global BG Scan blocking“ fehlt ab 22.150.0.
- Fix (Workaround, Forenberichte): während des Spielens `netsh wlan set autoconfig enabled=no interface="WLAN"`, danach wieder `enabled=yes`. Besser: LAN-Kabel. Quelle: https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871

## B4 PC hängt im 2,4-GHz-WLAN, obwohl 5 GHz verfügbar ist
- Symptom: Router sendet „Tenda 5 Ghz“ (Kanal 36, 80 MHz), PC verbindet sich mit „Tenda 2.4 Ghz“ Kanal 13 (Nachbar auf Kanal 1 mit 69 % Last); `netsh wlan show networks` zeigt das 5-GHz-Netz erst nach erzwungenem Scan.
- Fix: `netsh wlan set profileorder name="Tenda 5 Ghz" interface="WLAN" priority=1` + `connectionmode=auto`; als Admin „Bevorzugtes Band“ = 5 GHz. Ergebnis 17.11.2026: Link 287 → 1081 Mbit/s, Router-Ping max 9 → 5 ms.

## B5 Windows bildet DSCP 46 auf WMM-Priorität 5 statt 6 ab
- Fix: `New-NetQosPolicy … -DSCPAction 46 -PriorityValue8021Action 6`. Über Mobilfunk wird DSCP ohnehin meist verworfen. Quelle: https://learn.microsoft.com/en-us/powershell/module/netqos/new-netqospolicy

## B6 O2: NAT „strikt“ trotz UPnP/Portfreigabe
- Ursache: CGNAT (WAN-IP 10.x), IPv6 eingehend geblockt.
- Fix: nur APN `netpublic` (öffentliche IPv4, einmalig 49,99 €, nicht in 5G SA). Für Ping wirkungslos. Quelle: https://hilfe.o2online.de/packs-optionen-ausland-18/keine-oeffentliche-ipv4-trotz-buchung-und-nutzung-apn-netpublic-636077

## B7 Tenda 5G03: Band-Lock mit allen Häkchen wirkt nicht
- Der Schalter „4G/5G Lock BAND“ steht auf an, aber alle Bänder sind angehakt → kein Effekt. Ein echter Lock (z. B. nur N28+N78) erst nach A/B-Test; Locks laufen ins Leere, wenn der Betreiber Sender umbaut (Baustelle am Mast).

## B7a Tenda 5G03: „5G SA Preferred“ fällt bei schwachem Signal auf 5G NSA zurück
- Messung 18.11.2026 (CoD, je ~40 min, PC-Verkehr gleich): NSA/wechselnd (0–1 Balken) Median 50 ms, 95 % 204 ms, 18 % über 100 ms · SA (2 Balken) Median 26 ms, 95 % 106 ms, 5 % über 100 ms. Router-Ping in beiden Fällen 1 ms → Ursache ist allein die Funkstrecke.
- NSA braucht einen 4G-Anker; bei schwachem 4G (0–1 Balken) ist NSA schlechter als SA. Jeder SA↔NSA-Wechsel kostet Sekunden ohne Verbindung (Ausfälle 3–73 s im Mitschnitt).
- Aber: Um 17:09 fiel der Router auf NSA mit **3 Balken** (bessere 4G-Ankerzelle) → Ping Ø 21–34 ms, 0 Verluste, bester Abschnitt des Tages. NSA ist also nur mit schwacher Ankerzelle schlecht.
- Funkwerte dabei (17:19, „4G“, Band **B3+B1** CA, eNodeB 21118): SINR **27 dB**, RSRP −75 dBm, RSRQ −11 dB, CQI 14 → hervorragend (zum Vergleich 5G SA N28: SINR 2 dB, B8). Die Balkenanzeige taugt nicht zur Bewertung, nur SINR/CQI.
- Fix: nicht blind „SA only“ erzwingen. Nach gemessenem Ping entscheiden, bei guter Lage Band/PCI/Zell-ID von der Tenda-Statusseite notieren und genau dieses Band per „Lock BAND“ festlegen; Router drehen/Richtantenne zum Mast für mehr Pegel.

## B7b Dieselbe 5G-Zelle schwankt um 30 dB, Bandkombinationen wechseln ständig
- Messung 18.11.2026, 5G SA N28, NR-Zelle 6881771692: 17:23 RSRP −63 dBm / SINR 19 dB / CQI 14 (Ping Median 15–18 ms) → 17:41 RSRP −96 dBm / SINR −1 dB / CQI 8. Gleichzeitig bot das Netz wechselnde Kombinationen an (B7+B1, N28+B7 NSA, B1+B20, B3+B1).
- Ursache unbelegt: Router bewegt, Leistungsregelung/Umbau am Mast (nach O2-Umbauten ändern sich oft eNB-/Zell-IDs), Last.
- Fix: vor dem Spielen Statusseite lesen (SINR/CQI, nicht Balken); SA nur bei SINR > 10 dB, sonst „4G only“.
- Mast: eNodeB 21118 = Berliner Straße, Neuenhagen-Bollensdorf, 52.508093, 13.684266 (CellMapper-API, bestätigt durch OSM-Turm 2 m daneben); Bänder dort B20+B3, Zelle 51 vermutlich B1.

## B8a Restbandbreite trotz Spielmodus (gemessen 18.11.2026)
- CoD lädt trotz „On-Demand-Texturstreaming: Minimal“ und 1-GB-Limit im Menü/Kartenwechsel Inhalte per HTTP (Port 80) mit 1–3,7 MB/s, ~17 min nach Spielstart. Abhilfe nur: alle Inhaltspakete vorab installieren.
- Steam lädt beim Spielstart über `*.steamcontent.com` (Shader Pre-Caching) → in Steam „Shader Pre-Caching“ aus.
- Laufende KI-CLI-Sitzungen senden je Antwort den ganzen Verlauf (Claude Code bis ~85 KB/s hoch) → während Matches nicht chatten.
- Firewall-Block trennt bestehende Verbindungen nicht (GoogleDriveFS behielt 2 aufgebaute Verbindungen).

## B8 Schwaches SINR in 5G SA (N28) trotz „Signal Strength: Good“
- Symptom: Tenda zeigt „Good“, aber SS-SINR 2 dB, RSRQ −14 dB, CQI 6 → HARQ-Wiederholungen, Jitter, Verlust.
- Ursache: Störung/Überlast der Zelle (bei Frank: Bauarbeiten am Mast), nicht zu geringe Feldstärke.
- Fix: Standorttest im Router, Ausrichtung, externe n78-Antenne damit N78 öfter per CA dazukommt; Netzbetreiber-Vergleich.
