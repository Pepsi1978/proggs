# R4: Windows 11 (26200) Netzwerk-Optimierung für Online-Gaming, Intel Wi-Fi 6 AX201

Stand der Recherche: 17.11.2026. Methodik: WebSearch + WebFetch, URL pro Aussage. Ehrlichkeitshinweis: Für die meisten "Tweaks" gibt es KEINE kontrollierten Messungen, nur Einzeltests und Foren. Das ist je Punkt vermerkt. Es wurde nichts am System verändert, nur lesend geprüft (siehe Abschnitt 0).

## 0. Ist-Zustand dieses PCs (lokal gelesen, read-only)

- WLAN-Adapter "WLAN" = Intel(R) Wi-Fi 6 AX201 160MHz, Status Up, LinkSpeed 287 Mbit/s. Ethernet (I225-V) ist vorhanden, aber nicht verbunden.
- Installierter WLAN-Treiber: 23.170.0.1 (Win32_PnPSignedDriver).
- Aktueller Intel-Treiber laut Intel-Download-Seite: 24.80.0 (Treiberversion 24.80.0.2, Datum 20.10.2026), AX201 unterstützt, validiert für Windows 11 25H2. Quelle: http://www.intel.com/content/www/us/en/download/19351/intel-wireless-wi-fi-drivers-for-windows-10-and-windows-11.html
- Erweiterte Eigenschaften (alle Standardwerte): Roaming-Dynamik = 3. Mittel, Bevorzugtes Band = 1. Keine Einstellung, MIMO-Energiesparmodus = Auto-SMPS, Durchsatzbooster = Deaktiviert, U-APSD = Deaktiviert, Paketzusammenfügung = Aktiviert, Kanalbreite 2,4/5 GHz = Auto, Übertragungsleistung = 5. Am höchsten.
- Die Einstellung "Global BG Scan blocking" existiert in dieser Treiberfassung NICHT mehr in der Liste (siehe Abschnitt 3).
- Energieplan "Höchstleistung" aktiv, Drahtlosadapter-Energiesparmodus (AC und DC) = 0 = "Höchstleistung". Lokal verifiziert: Index 0 = Höchstleistung, 1 = Minimal, 2 = Mittel, 3 = Maximaler Energiesparmodus. Achtung: Ein Web-Guide (geekrewind) nennt eine abweichende Zuordnung (0 = Low). Die lokale Ausgabe von `powercfg /q` ist maßgeblich. Quelle des widersprüchlichen Guides: https://geekrewind.com/how-to-change-wireless-adapter-power-saving-mode-in-windows-11/

## 1. Belegt wirksam bzw. begründet sinnvoll

### 1.1 Kabel statt WLAN (stärkster Hebel, aber nicht Frage-Thema)
- Activision empfiehlt in seinem Lag-Leitfaden ausdrücklich Ethernet: Wi-Fi schwanke stark in Zuverlässigkeit und Durchsatz. https://support.activision.com/uk/en/warzone-2/articles/improving-lag-and-in-game-performance-in-warzone-2
- Der PC hat bereits einen I225-V-Ethernet-Port (lokal gesehen). Wenn irgendwie möglich: Kabel/Powerline zum Router. Das ist belegt wirksamer als jeder Treiber-Tweak.

### 1.2 Periodische Hintergrund-Scans des WLAN (häufigste Ursache für regelmäßige Ping-Spitzen bei Intel-Adaptern)
- Befund: Intel AX201 zeigt Latenzspitzen von 50+ ms etwa alle 10 Minuten im Leerlauf; Ursache laut Nutzer: Hintergrund-Scan, während verbunden. Intel hat eine Anfrage "Scan abschalten" intern erfasst, aber keinen öffentlichen Fix geliefert. https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871
- Wirksamer Workaround (Nutzertest, keine Intel-Zusage): `netsh wlan set autoconfig enabled=no interface="WLAN"` beendete die Spitzen vollständig. Nebenwirkung: Windows verbindet/sucht nicht mehr selbständig, nach Neustart oder Netzwechsel ist wieder `enabled=yes` nötig. Gleiche Quelle wie oben.
- Weitere Bestätigungen: ghacks (Bericht, dass WLAN AutoConfig Scans auch im verbundenen Zustand ausführt; Seite lieferte beim Abruf 403, nur über Suchzusammenfassung belegt) https://www.ghacks.net/?p=54335 ; Intel-Community "AX210 Periodic Lag Spike" (GeForce-Now-Ruckler exakt alle 10 Minuten, AX211 ebenso) https://community.intel.com/t5/Wireless/AX210-Periodic-Lag-Spike/m-p/1666486
- Älterer Treiber-Weg: Bis Treiber 22.120.1.9 gab es "Global BG scan blocking = Always" (Standard "Never"; "Always: Will not perform periodic scans for other available APs"). Rollback auf 22.120.1.9 + Always reduzierte die Spitzen stark, beseitigte sie aber nicht vollständig. Quellen: https://community.intel.com/t5/Wireless/Please-bring-back-Global-BG-Scan-blocking-setting/m-p/1630589 und https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871
- Intel-Position: Die Einstellung sei ab Treiber 22.150.0 "nicht mehr nötig" (Intel-Support laut Nutzerzitat). Nutzer widersprechen, Problem bestehe auch auf neueren Treibern. Zitiert aus der Suchzusammenfassung zu https://community.intel.com/t5/Wireless/Global-BG-Scan-blocking-Setting-missing/m-p/1406055
- Befehle (Administrator-PowerShell):
  - Ist-Zustand prüfen: `netsh wlan show settings`
  - Scans stoppen: `netsh wlan set autoconfig enabled=no interface="WLAN"`
  - Wieder erlauben (vor Netzwechsel oder nach Spiel): `netsh wlan set autoconfig enabled=yes interface="WLAN"`
  - Der Adaptername auf diesem PC lautet "WLAN" (lokal gelesen).
  - Praktisch: zwei kleine Skripte "Spielmodus an/aus", oder geplante Aufgabe beim Anmelden. Der Schalter geht beim Reboot verloren (Quelle Intel-Thread oben).
  - Diagnose-Messung vorher: `ping -t <Router-IP>` im Leerlauf 15 Minuten laufen lassen und auf periodische Spitzen (Intervall 10 min) achten. Vorher/Nachher vergleichen.
- Schwäche der Evidenz: Nur Forenberichte und eine Intel-Community-Diskussion, keine kontrollierte Messung. Ein Teil der Nutzer meldet "nur temporär" oder "half nicht" (Suchzusammenfassung zu Tom's Hardware / Intel-Threads). Daher: erst messen, dann entscheiden.

### 1.3 Treiber aktualisieren (mit Vorbehalt)
- Installiert: 23.170.0.1, aktuell: 24.80.0.2 (Intel, 20.10.2026). Quelle: Intel-Download-Seite oben.
- Konkrete latenzbezogene Fixes in 24.80.0 sind NICHT belegt (Release-Notes-PDF war nicht lesbar; Zusammenfassung comss.ru nennt nur "regulatorische Updates, kleine Fixes" https://www.comss.ru/page.php?id=22496). Intel selbst empfiehlt bei OEM-Rechnern den Treiber des Mainboard-/PC-Herstellers (Intel-Support im AX201-Thread, 5.-7.2.2024) https://community.intel.com/t5/Wireless/AX201-ping-spikes/m-p/1570104
- Praxis: Treiber aktualisieren ist risikoarm, aber keine garantierte Latenzverbesserung. Danach erweiterte Eigenschaften neu prüfen, weil Neuinstallation Werte zurücksetzen kann (Intel-Support-Hinweis im Intel-Thread "check or reapply all the recommended advanced settings after clean installation", Zusammenfassung zu https://community.intel.com/t5/Wireless/AX201-slow-to-connect-to-my-Wi-Fi/td-p/1302015).

### 1.4 Roaming-Dynamik (bei fest stehendem PC)
- Intel-Beschreibung: Roaming Aggressiveness setzt die Signalstärke-Schwelle, ab der der Adapter nach einem anderen AP sucht; Standard = Mittel, von Intel empfohlen. https://www.intel.com/content/www/us/en/support/articles/000005585/network-and-i-o/wireless.html
- Dell-KB empfiehlt für stationäre Geräte "Lowest" (oder Medium-Low), damit nicht ständig nach anderen AP gesucht wird; Medium nur in Firmennetzen mit mehreren AP. https://www.dell.com/support/kbdoc/en-ws/000132395/change-intel-advanced-wi-fi-adapter-settings-to-improve-slow-performance
- Wirksamkeit unsicher: Im AX201-Thread brachte Medium-Low/Lowest keine Änderung der Spitzen (Signal ~90 %), Intel schlug es dennoch vor. https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871
- Einordnung: Bei einem Router/AP (kein Mesh) und stabilem Signal sinnvoll und risikoarm zu testen, aber kein belegtes Latenz-Heilmittel. Bei Mesh/Hotspot-Wechsel kann "Lowest" schaden (Sticky Client).
- Befehl: `Set-NetAdapterAdvancedProperty -Name "WLAN" -RegistryKeyword RoamAggressiveness -RegistryValue 1` (Wertezuordnung anhand `(Get-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamAggressiveness).ValidDisplayValues` prüfen; 1 = Lowest nur vermutet, nicht in der Quelle belegt). Die Cmdlet-Syntax ist offiziell, ein Neustart des Adapters erfolgt automatisch, außer `-NoRestart`: https://learn.microsoft.com/en-us/powershell/module/netadapter/set-netadapteradvancedproperty

### 1.5 Energieoptionen / Adapter-Energiesparen
- Intel: "Maximum Performance" = Adapter optimiert Leistung statt Stromsparen. Intel rät allerdings, Standardwerte zu behalten, und nennt, dass neuere Windows-Versionen die Optionen ausblenden können. https://www.intel.com/content/www/us/en/support/articles/000005879/network-and-i-o/wireless-networking.html
- Dieser PC ist bereits auf Höchstleistung/Index 0 (lokal gelesen). Hier gibt es nichts mehr zu tun.
- Messung der Latenzwirkung: Keine Quelle gefunden. Ein Desktop am Netzteil profitiert vermutlich nur wenig; nicht belegt.
- Zusätzlich im Geräte-Manager: "Computer kann das Gerät ausschalten, um Energie zu sparen" deaktivieren. Befehle: `Get-NetAdapterPowerManagement -Name WLAN` und `Disable-NetAdapterPowerManagement -Name WLAN` (Cmdlet-Doku: https://learn.microsoft.com/en-us/powershell/module/netadapter/set-netadapterpowermanagement). Hinweis: Disable-NetAdapterPowerManagement schaltet auch WoWLAN-Funktionen (Wake-on-Magic-Packet etc.) ab. Nutzen für Latenz: nicht belegt.

### 1.6 Hintergrundlast (Delivery Optimization, Updates, Cloud-Sync)
- Delivery Optimization ist bei Windows-Clients standardmäßig Modus 1 (LAN): Peer-Sharing nur mit Geräten im selben Netz/derselben öffentlichen IP; nicht über das Internet (das wäre Modus 3). Quelle (Microsoft Learn, Stand 5.11.2026): https://learn.microsoft.com/en-us/windows/deployment/do/waas-delivery-optimization-reference
- Standardmäßig lädt Windows nicht im Akkubetrieb hoch (Default), und ein VPN-Erkennung deaktiviert Peering. Gleiche Quelle. Monatliche Upload-Kappe für Internet-Peers Default 20 GB.
- Folge: Das "Windows lädt für fremde Rechner hoch"-Szenario trifft Heimanwender nur im LAN. Der Effekt auf Spiel-Latenz ist nicht belegt; die Wirkung ist indirekt (Bandbreite/Bufferbloat während laufender Updates).
- Abschalten (Oberfläche): Einstellungen > Windows Update > Erweiterte Optionen > Übermittlungsoptimierung > "Downloads von anderen Geräten zulassen" aus. Offizielle Beschreibung: https://support.microsoft.com/en-ca/help/4468254/windows-update-delivery-optimization-faq
- Abschalten (Befehl, Modus 0 = HTTP only, nur Peer-Sharing aus, Updates laufen weiter):
  `New-Item -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization" -Force | Out-Null; Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization" -Name DODownloadMode -Value 0 -Type DWord`
  (Wert 0 und die Policy-Struktur laut Microsoft-Referenz oben; Modus 100 "Bypass" ist ab Windows 11 veraltet und kann Downloads brechen, NICHT verwenden.)
- Wichtiger als P2P: Wirklich störend sind Downloads (Steam, Battle.net, Windows Update, OneDrive, Cloud-Backup) während des Spielens. Activision rät, bandbreitenintensive Aktivität auf dem Netz zu vermeiden. https://support.activision.com/uk/en/warzone-2/articles/improving-lag-and-in-game-performance-in-warzone-2

## 2. Mythos / wirkungslos / unbelegt

### 2.1 Nagle-Algorithmus, TcpAckFrequency, TCPNoDelay (für CoD wirkungslos)
- Nagle und Delayed ACK sind TCP-Mechanismen. Echtzeit-Spiele nutzen für Spielzustandsdaten UDP; GameBench: Spieltraffic nutzt UDP, um Latenz zu minimieren, daher greifen TCP-Mechanismen dort nicht. https://gamebench.net/index.html%3Fp=3439.html (Folgerung "Nagle betrifft UDP nicht" ist Fachwissen, nicht wörtlich in der Quelle).
- Die von Activision für PC genannten Ports (Season-6-Liste von charlieintel) enthalten UDP 3074, 3478, 4379-4380, 27000-27031, 27036 und TCP 3074, 27014-27050. Die Liste ist nicht die offizielle Seite, daher mit Vorsicht. https://www.charlieintel.com/call-of-duty-warzone/how-to-fix-packet-loss-and-packet-burst-in-warzone-104829/
- Messungen: MakeUseOf-Test (Marvel Rivals, Elden Ring Nightreign, Monster Hunter Wilds) mit TcpAckFrequency/TCPNoDelay/TcpDelAckTicks: kein Ping-Gewinn, bei einem Spiel evtl. Placebo. Einzeltest, kein Labor. https://www.makeuseof.com/i-tested-the-windows-network-tweaks-gamers-swear-by-and-only-one-lowered-my-ping/
- Offizielle Mechanik: TcpAckFrequency=1 pro Schnittstellen-GUID, offizieller API-Weg SIO_TCP_SET_ACK_FREQUENCY. https://learn.microsoft.com/en-us/archive/technet-wiki/7636.iscsi-and-the-nagle-algorithm ; Nagle pro Socket mit TCP_NODELAY, viele Spielclients setzen es selbst (Hinweis in https://www.revk.uk/2009/12/wow-latency.html, anekdotisch, 2009).
- Fazit: Für CoD (UDP) nichts zu holen, Registry unberührt lassen.

### 2.2 NetworkThrottlingIndex (Standard 10, "0xFFFFFFFF = aus")
- Offizielle Microsoft-MMCSS-Seite beschreibt nur SystemResponsiveness und Tasks, NetworkThrottlingIndex ist dort nicht dokumentiert. https://learn.microsoft.com/en-us/windows/win32/procthread/multimedia-class-scheduler-service
- Technischer Test: djdallmann/xperf-Messung: Abschalten erhöht NDIS.sys-DPC-Latenz "ziemlich deutlich"; "improves latency" sei "not completely true"; Durchsatzgewinn nur bei Datenraten über dem Limit plausibel, nicht gemessen. https://djdallmann.github.io/GamingPCSetup/CONTENT/RESEARCH/NETWORK/ (Diagramme waren nicht abrufbar, keine Zahlen für ein/aus; die Seite enthält außerdem einen Rechenfehler beim Limit.)
- Einzeltest: MakeUseOf: nur dieser Tweak senkte den Ping, aber nur in einem von drei Spielen (Marvel Rivals ca. 110 auf 80 ms), in den anderen keine Änderung. Gleiche Quelle wie 2.1. Das ist Einzelfall, kein Beleg für CoD.
- Weitere Aussagen: Gaming-Tuning-Guides sprechen von gemischten Ergebnissen, teils mehr DPC-Latenz/Audioknacken (Suchzusammenfassung zu windowsforum.com). Deshalb: NICHT empfohlen, höchstens als A/B-Test mit eigener Messung (LatencyMon, Ping unter Last), jederzeit rückgängig machbar.
- Test-Befehl (nur falls A/B gewünscht): `Set-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Multimedia\SystemProfile" -Name NetworkThrottlingIndex -Value 0xffffffff -Type DWord`; Rückweg: Wert 10.

### 2.3 DNS wechseln (Cloudflare/Google/"Gaming-DNS")
- Konsens (überwiegend VPN-/DNS-Anbieter, also interessengeleitet): DNS löst nur den Servernamen vor dem Verbindungsaufbau auf, die laufende Ping-Zeit bleibt gleich; nützlich höchstens für Login, Matchmaking, Launcher oder bei langsamem ISP-Resolver. Unabhängige Messungen: keine gefunden. https://vpncentral.com/changing-dns-gaming/ und https://www.whatismyip.com/best-dns-server-gaming/
- Fazit: Kein Latenzhebel im Spiel. Wenn Login/Matchmaking hakt, kann ein schnellerer Resolver helfen. Befehl: `Set-DnsClientServerAddress -InterfaceAlias "WLAN" -ServerAddresses 1.1.1.1,1.0.0.1` (Cmdlet Standard; Rückweg `-ResetServerAddresses`).

### 2.4 Erweiterte Intel-Eigenschaften, die NICHT für niedrige Latenz taugen oder unbelegt sind
(Beschreibungen jeweils aus Intels Referenzseite https://www.intel.com/content/www/us/en/support/articles/000005585/network-and-i-o/wireless.html, falls nicht anders vermerkt.)
- Durchsatzbooster (Standard: aus): erhöht Upload-Durchsatz, indem der Adapter das Medium länger hält; Intel warnt vor Durchsatzverlust anderer Clients. Es ist eine Durchsatz-, keine Latenzfunktion. Dell: Aus lassen. Empfehlung: aus lassen (Dell: https://www.dell.com/support/kbdoc/en-ws/000132395/change-intel-advanced-wi-fi-adapter-settings-to-improve-slow-performance).
- U-APSD (Standard: aus): spart Strom bei periodischem latenzsensiblem Traffic (VoIP); Intel nennt Interoperabilitätsprobleme mit manchen AP (geringerer Empfangsdurchsatz). Ein Intel-Mitarbeiter schlug es in einem 1x1-AP-Thread dennoch vor (Zusammenfassung zu https://www.intel.com/content/www/us/en/support/articles/000005585/network-and-i-o/wireless.html). Für Gaming-Latenz: nicht belegt, aus lassen.
- MIMO-Energiesparmodus (Standard: Auto-SMPS): spart Strom (nur eine Antenne im Leerlauf). "No SMPS" löst laut Intel Durchsatzprobleme mit manchen älteren AP. Im Intel-Thread zum Lag-Spike brachte das Ausschalten von MIMO Power Save nichts. https://community.intel.com/t5/Wireless/Please-bring-back-Global-BG-Scan-blocking-setting/m-p/1630589 . Test mit "Kein SMPS" ist harmlos auf einem Desktop, aber nicht als Latenz-Fix belegt.
- Paketzusammenfügung (Packet Coalescing, Standard: aktiviert): bündelt zufällige Broadcast/Multicast-Pakete zur Stromersparnis (weniger Interrupts). Betrifft Broadcast/Multicast, nicht UDP-Unicast-Spielverkehr. Ausschalten ist harmlos, aber kein belegter Latenzgewinn. Cmdlet-Beschreibung D0PacketCoalescing: https://learn.microsoft.com/en-us/powershell/module/netadapter/set-netadapterpowermanagement
- Kanalbreite: Auto ist Standard. Dell empfiehlt "Auto". Die 160-MHz-Verbindung (laut Adapternamen) kann im Router-Umfeld anfälliger für Störungen sein als 80 MHz; das ist eine Router-Einstellung, belegt ist hier nichts. Empfehlung: Kanalbreite am Router/Adapter nicht blind verändern, aber DFS-Kanäle meiden (siehe Hinweis unten).
- Bevorzugtes Band: Dell empfiehlt "Prefer 5 GHz", wenn 2,4/5 GHz dieselbe SSID teilen. Sinnvoll, falls der Adapter auf 2,4 GHz hängt: Die gelesene Link-Rate (287 Mbit/s) sagt nicht, welches Band aktiv ist. Befehl zur Prüfung: `netsh wlan show interfaces` (Zeile "Funktyp", "Band", "Kanal", "Signal"). Beim Wechsel: `Set-NetAdapterAdvancedProperty -Name "WLAN" -RegistryKeyword RoamingPreferredBandType -RegistryValue 3` (Wert 3 = Prefer 5 GHz nur vermutet; vorher ValidDisplayValues prüfen).
- Übertragungsleistung: Intel empfiehlt prinzipiell die niedrigste, die noch gute Verbindung hält (Interferenz in dichten Umgebungen). Dell sagt Highest. Widerspruch, kein Latenzbeleg. Standard (5. Am höchsten) belassen.
- Wireless-Modus 802.11ax, Fat Channel Intolerant: Dell-Tabelle, nur bei Verbindungsabbrüchen anfassen (gleiche Dell-Quelle).

### 2.5 Ortungsdienste / "WLAN-Scans durch Windows" als eigener Hebel
- Ein Nutzer im Intel-Forum berichtet: Ortungsdienste und Cortana abzuschalten half nicht. Ein Intel-Moderator nannte Ping-Spitzen bis 500 ms bei Hintergrundscans als "normales Verhalten von WLAN-Geräten" (Suchzusammenfassung zu https://community.intel.com/t5/Wireless/Frequent-ping-spikes/m-p/426780 ; beim direkten Abruf des Threads fand sich dieses Zitat nicht, die Aussage ist daher nur über die Suchzusammenfassung belegt, geringe Sicherheit).
- Konkret belegt ist nur die AutoConfig-/Treiberseite (Abschnitt 1.2). Für die Ortungsdienste: keine Belege für Wirksamkeit.

### 2.6 WLAN-Optimizer-Tools (z. B. "WLAN Optimizer")
- Die Tools ändern im Kern genau diese Scan-Funktion (AutoConfig/Scan-Intervall) und Energiesparen. Nachweis: ghacks zu "Disable WLAN Background Scans" und Forumsberichte, dass dasselbe per netsh geht (Abschnitt 1.2). Eine eigene Quelle zu einem konkreten Tool wurde nicht gefunden. Ein Tool ist nicht nötig.

### 2.7 QoS-Richtlinie (gpedit, DSCP) und Mobilfunk
- Windows: Richtlinienbasiertes QoS (gpedit.msc: Computerkonfiguration > Windows-Einstellungen > Richtlinienbasierte QoS) setzt DSCP; Windows bildet DSCP auf WMM-Zugriffskategorien ab (FedTech/BizTech-Artikel 2010, allgemein). https://fedtechmagazine.com/article/2010/04/boost-network-performance-windows-7-qos
- Praxisproblem: Windows nimmt nur die obersten 3 Bit von DSCP für die 802.11-User-Priority; DSCP 46 landet in UP 5 (Video-Queue) statt UP 6 (Voice). Workaround per PowerShell mit PriorityValue8021Action. Einzeltest eines Bloggers (Windows 10 Pro, Skype), keine Absicherung: https://wifinigel.blogspot.com/2019/01/the-windows-wmm-user-priority-issue-fix.html
- Beispielbefehl (Blog): `New-NetQosPolicy -Name "Skype" -AppPathNameMatchCondition "Skype.exe" -DSCPAction 46 -PriorityValue8021Action 6`. Für CoD wäre das z. B. `-AppPathNameMatchCondition "cod.exe"` (Prozessname nur Annahme, vor Gebrauch mit dem Task-Manager prüfen). Prüfen: `Get-NetQosPolicy`; entfernen: `Remove-NetQosPolicy -Name "..."`.
- Entscheidend ist, ob die Strecke DSCP/WMM beachtet. Consumer-Router lesen DSCP meist nicht direkt, sie stützen sich auf WMM; nur Enterprise-Gerät unterstützt DSCP direkt (FedTech, siehe oben). Router-QoS wirkt oft stärker als Windows-QoS (usekudu.com, Suchzusammenfassung: https://usekudu.com/guides/gaming/prioritize-game-traffic-with-qos-settings-on-windows ).
- Mobilfunk: Eine Messstudie (Univ. Aberdeen, 2017) fand, dass DSCP in Mobilfunknetzen und auf dem Internetpfad häufig zurückgesetzt, umgeschrieben oder herabgestuft wird ("Remarking Pathologies"). https://dl.ifip.org/index.html/db/conf/tma/tma2017/mnm2017_paper13.pdf (Kein Beleg für Tethering/Hotspot-Spielverkehr und keine Carrier-Dokumentation gefunden.) Fazit: Ein DSCP-Tag, der Mobilfunk/Hotspot durchlaufen muss, bringt voraussichtlich nichts, weil der Betreiber ihn am Netzrand zurücksetzen oder ignorieren kann. Das ist eine begründete Schlussfolgerung aus dieser Studie, kein Messbeleg für CoD über Hotspot.
- Nutzen höchstens auf der letzten Meile WLAN (WMM) zu einem Router, der selbst Prioritäten setzt. Nur dort sinnvoll, wenn der Router WMM honoriert; Wirkung bei einem einzelnen Gerät im Netz ohne Konkurrenz: gering (Folgerung, kein Beleg).

### 2.8 Hintergrund-Apps
- Allgemein sinnvoll (Activision: Streaming, Downloads, andere Nutzer belasten die Leitung): https://support.activision.com/uk/en/warzone-2/articles/improving-lag-and-in-game-performance-in-warzone-2
- Prüfbefehl: `Get-NetTCPConnection -State Established | Group-Object OwningProcess | Sort Count -desc | Select -First 10` bzw. Ressourcenmonitor > Netzwerk (Prozesse mit Netzwerkaktivität). Eine Studie zur Latenzwirkung bestimmter Apps gibt es nicht.
- Gaming-Hinweis aus den Quellen: Steady-Ping/Geo-Filter im Spiel aktivieren und Verbindungsanzeige (Server-Latenz, Paketverlust) einschalten, um Jitter von Paketverlust zu unterscheiden (Suchzusammenfassung zu https://netduma.com/blog/warzone-lag-fix ).

## 3. Konkrete Reihenfolge (aus den Belegen abgeleitet)

1. Messen: `ping -t <Router-IP>` 15 Minuten im Leerlauf; Spitzen im 10-Minuten-Raster = Scan-Problem. In-Game: Verbindungsmeter einschalten.
2. Wenn Kabel möglich: Ethernet nutzen (belegter Hauptfaktor).
3. Treiber auf 24.80.0.2 aktualisieren (Intel-Seite oben). Danach erweiterte Eigenschaften prüfen.
4. Bei periodischen Spitzen: `netsh wlan set autoconfig enabled=no interface="WLAN"` für die Spielsitzung, danach wieder `enabled=yes`.
5. Roaming-Dynamik auf "Niedrigste" testen (stationärer PC, ein AP). Rückweg: "3. Mittel".
6. Band prüfen (`netsh wlan show interfaces`), ggf. 5 GHz bevorzugen.
7. Delivery Optimization/Updates/Cloud-Sync während des Spielens pausieren oder Peer-Sharing aus.
8. Registry-Tweaks (Nagle/TcpAckFrequency/NetworkThrottlingIndex), DNS und gpedit-QoS: nicht für CoD notwendig; nur als reversibler A/B-Test.

## 4. Lücken und Vorbehalte
- Keine kontrollierte Messung zu Roaming-Dynamik, MIMO-SMPS, U-APSD, Durchsatzbooster, Paketzusammenfügung gefunden. Aussagen dort sind Intel-/Dell-Beschreibungen, keine Wirkungsbelege.
- Intel-Release-Notes-PDF zu 24.80.0 war nicht lesbar; Fixes unbekannt.
- Zum Verhalten von DSCP bei Handy-Hotspots/Tethering fand sich keine Quelle.
- Die Zuordnung der Registerwerte für RoamAggressiveness/RoamingPreferredBandType wurde nicht in einer Quelle verifiziert. Vor dem Setzen die gültigen Anzeigewerte lesen: `(Get-NetAdapterAdvancedProperty -Name WLAN -RegistryKeyword RoamAggressiveness).ValidDisplayValues` und `.ValidRegistryValues`. Alternativ `Set-NetAdapterAdvancedProperty ... -DisplayName "Roaming-Dynamik" -DisplayValue "1. Niedrigste"` (Anzeigewerte sind deutsch und vom Treiber abhängig).
- Mehrere Seiten waren nicht direkt abrufbar (ghacks 403, Intel-Release-Notes-PDF, redlib, Tom's Hardware). Das ist in den jeweiligen Punkten vermerkt.

## BEST-PRACTICES-KANDIDATEN:
- Windows-Gaming-WLAN: Periodische Hintergrund-Scans prüfen (ping -t Router, 10-Minuten-Raster), Workaround `netsh wlan set autoconfig enabled=no interface="<Name>"`, danach wieder yes. https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871
- Windows-Netzwerk-Tweaks für UDP-Spiele (TcpAckFrequency/TCPNoDelay/Nagle) sind wirkungslos; NetworkThrottlingIndex nur als A/B-Test (DPC-Latenz steigt). https://djdallmann.github.io/GamingPCSetup/CONTENT/RESEARCH/NETWORK/ und https://www.makeuseof.com/i-tested-the-windows-network-tweaks-gamers-swear-by-and-only-one-lowered-my-ping/
- Delivery Optimization: Default LAN-Modus (1), Peer-Sharing aus per DODownloadMode=0; Modus 100 ab Windows 11 nicht mehr verwenden. https://learn.microsoft.com/en-us/windows/deployment/do/waas-delivery-optimization-reference
- Intel-WLAN-Erweiterte-Eigenschaften: Beschreibungen und Standardwerte (Roaming Aggressiveness Medium, Throughput Booster aus, U-APSD aus, Packet Coalescing an, MIMO Auto SMPS). https://www.intel.com/content/www/us/en/support/articles/000005585/network-and-i-o/wireless.html
- Wireless-Adapter-Energiesparmodus per powercfg: Index 0 = Höchstleistung (lokal an Windows 11 26200 verifiziert; Web-Guide widerspricht). Untergruppe 19cbb8fa-5279-450e-9fac-8a3d5fedd0c1, Einstellung 12bbebe6-58d6-4636-95bb-3217ef867c1a.
- DSCP über Mobilfunk: wird häufig zurückgesetzt/umgeschrieben. https://dl.ifip.org/index.html/db/conf/tma/tma2017/mnm2017_paper13.pdf

## BUG-KANDIDATEN:
- Intel AX201/AX210/AX211 (Windows): Periodische Latenzspitzen alle ca. 10 Minuten durch Hintergrundscans; Einstellung "Global BG Scan blocking" ab Treiber 22.150.0 entfernt; Rollback auf 22.120.1.9 + "Always" oder `netsh wlan set autoconfig enabled=no` als Workaround; kein offizieller Intel-Fix. https://community.intel.com/t5/Wireless/AX201-ping-spikes/td-p/1567871 , https://community.intel.com/t5/Wireless/Please-bring-back-Global-BG-Scan-blocking-setting/m-p/1630589 , https://community.intel.com/t5/Wireless/AX210-Periodic-Lag-Spike/m-p/1666486
- Windows WMM/DSCP: DSCP 46 wird auf 802.11 UP 5 statt 6 abgebildet; Workaround New-NetQosPolicy -PriorityValue8021Action 6. https://wifinigel.blogspot.com/2019/01/the-windows-wmm-user-priority-issue-fix.html
- Delivery Optimization Download Mode 100 (Bypass) in Windows 11 veraltet, kann Downloads fehlschlagen lassen. https://learn.microsoft.com/en-us/windows/deployment/do/waas-delivery-optimization-reference
