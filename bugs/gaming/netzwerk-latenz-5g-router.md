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

## B8 Schwaches SINR in 5G SA (N28) trotz „Signal Strength: Good“
- Symptom: Tenda zeigt „Good“, aber SS-SINR 2 dB, RSRQ −14 dB, CQI 6 → HARQ-Wiederholungen, Jitter, Verlust.
- Ursache: Störung/Überlast der Zelle (bei Frank: Bauarbeiten am Mast), nicht zu geringe Feldstärke.
- Fix: Standorttest im Router, Ausrichtung, externe n78-Antenne damit N78 öfter per CA dazukommt; Netzbetreiber-Vergleich.
