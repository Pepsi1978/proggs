# UpdateZentrale: Deno-Versionsabfrage läuft ins Zeitlimit, weil ein filternder WLAN-DNS Sperr-Adressen liefert (20.11.2026)

> Behoben in UpdateZentrale 1.3.90 (`Services/Netzdienst.cs`). macOS nicht betroffen (siehe unten).

## Symptom

Die Karte „Deno“ wurde bei jeder Prüfung rot (viermal hintereinander am 20.11.2026, 09:03–09:53):
`https://dl.deno.land/release-latest.txt -> Versionsabfrage fehlgeschlagen: The request was canceled due to the configured HttpClient.Timeout of 15 seconds elapsing.`
Jede Prüfung dauerte genau 30 s (15 s + eine Wiederholung). `curl` (Git Bash und `curl.exe`) lud dieselbe Adresse im selben Moment in 0,1–0,3 s.

## Ursache

- Der Rechner hing an zwei Netzen: LAN mit DNS 192.168.0.1 und WLAN mit DNS **156.154.70.25 / 156.154.71.25** (UltraDNS/Vercara, ein filternder Resolver).
- Der WLAN-Resolver beantwortet `dl.deno.land` mit den Sperr-Adressen **156.154.112.39 / 156.154.113.39**. Sie nehmen keine TCP-Verbindung an. Der LAN-Resolver liefert die richtigen Cloudflare-Adressen (104.21.89.78, 172.67.138.213).
  Gemessen mit `Resolve-DnsName dl.deno.land -Type A -Server 156.154.70.25` gegen `-Server 192.168.0.1`.
- Windows nimmt die Antwort des Resolvers, der zuerst antwortet. Die beiden Abfragearten landeten verlässlich bei verschiedenen Resolvern:
  - `Dns.GetHostAddresses(host)` ohne Adressfamilie (die .NET-Vorgabe) ergab die beiden Sperr-Adressen und zwei IPv6-Adressen. IPv6 wird in diesem Netz nicht geroutet.
  - `Dns.GetHostAddresses(host, InterNetwork)` ergab die richtigen Adressen.
  - curl fragt A und AAAA getrennt ab und bekam deshalb die richtigen.
- `Netzdienst.VerbindenAsync` (eingeführt für den IPv6-Fall, siehe `desktop/dotnet-httpclient-ipv6-haenger.md`) probierte die Adressen **nacheinander** mit je 4 s Frist: 2 tote IPv4- plus 2 tote IPv6-Adressen ergeben 16 s, mehr als das Zeitlimit von 15 s. Keine Adresse konnte gewinnen. Allgemein galt: `Take(6)` × 4 s = 24 s > 15 s. Schon wenige tote Adressen reichten für den Fehler.
- Deshalb ist der Fall weder vorübergehend noch mit längerer Frist oder mehr Wiederholungen lösbar: Der Ablauf traf nie eine gute Adresse.

## Behebung (Vorbeugung für die ganze Fehlerart: tote oder umgebogene DNS-Antworten)

`UpdateZentrale/Services/Netzdienst.cs`:
1. **Adressen aus drei Abfragen:** ohne Familie, nur IPv4, nur IPv6, parallel und dedupliziert, IPv4 zuerst, höchstens 8 Adressen. Sobald eine Abfrage Adressen liefert, bekommen die anderen noch 300 ms. Eine verschluckte AAAA-Abfrage hält den Verbindungsaufbau also nicht auf. Liefert keine Abfrage etwas, endet das Warten nach 8 s.
2. **Gestaffelt-paralleler Verbindungsaufbau** (`WettlaufAsync`, nach dem Muster von Happy Eyeballs): Alle 250 ms startet die nächste Adresse, nach einem Fehlschlag sofort. Die erste Verbindung gewinnt. Bei Verlierern wird ein später Erfolg entsorgt und ein später Fehler beobachtet, damit keine „unbehandelte Ausnahme“ in die Diagnose gerät. Tote Adressen vorn kosten nur noch 250 ms je Adresse statt 4 s.
3. **Ausweichquelle DNS-over-HTTPS:** Antwortet keine Adresse oder löst das System nichts auf (auch bei „Host nicht gefunden“, denn ein filternder Resolver sperrt Namen auch so), fragt die Zentrale `https://1.1.1.1/dns-query` und `1.0.0.1` gleichzeitig im JSON-Format nach A-Einträgen und versucht die neuen Adressen. Der DoH-Client spricht eine IP direkt an und braucht den System-Resolver nicht. Jeder Einsatz wird als Diagnose-Ereignis `netz.doh` mit dem Hostnamen protokolliert. TLS prüft weiterhin das Zertifikat.
4. Das Zeitlimit des HttpClient steigt von 15 auf 30 s, über den schlimmsten Fall eines Verbindungsaufbaus (DNS 8 s + Wettlauf ~6 s + DoH 3 s + Wettlauf ~6 s, dazu TLS und Antwort). Der funktionierende Weg bleibt deutlich unter einer Sekunde.
5. Die Fehlermeldung nennt jetzt alle versuchten Adressen (`Keine Verbindung zu host (versucht: …)`) oder sagt, dass der Name nicht auflösbar war, statt nur das nichtssagende Zeitlimit.

Tests: `UpdateZentrale/Tests/NetzdienstTests.cs`, mit toten Sperr-Adressen vorn und der guten hinten, dazu Fehlschlag-sofort-weiter, alle tot, Verlierer-Entsorgung, Abbruch, Sortierung, DoH-Auswertung und ein Live-Test (`UZ_LIVE_NETZ=1`). Live bestätigt am 20.11.2026: Der System-Resolver lieferte weiter 156.154.112.39/113.39, die reparierte Abfrage ergab trotzdem in 0,15 s `2.9.8`. Der DoH-Weg lieferte live Adressen.

## macOS

`UpdaterZentrale-macOS` fragt per `URLSession` ab. Das Netzwerk-Framework von Apple lässt die Adressen nach Happy Eyeballs selbst gegeneinander laufen und hat keine Frist-Kette pro Adresse. Der Rechenfehler (Adressen × Frist > Zeitlimit) existiert dort nicht. Liefert der Resolver eines Macs **nur** Sperr-Adressen, hilft auch dort nur ein anderer Resolver. Das ist bisher nicht beobachtet und deshalb nicht nachgezogen.

## Erkennen

- Eine Versionsabfrage scheitert reproduzierbar mit genau dem HttpClient-Zeitlimit, `curl` kommt sofort durch, und andere Hosts gehen: Vergleiche `[Net.Dns]::GetHostAddresses(host)` mit `Resolve-DnsName host -Type A`.
- Zeigt .NET Adressen, die curl nicht nutzt, die DNS-Server je Adapter prüfen: `Get-DnsClientServerAddress` und dann `Resolve-DnsName host -Server <dns>`.
- Ab 1.3.90 nennt die Meldung die versuchten Adressen. Stehen dort 156.154.x.x oder andere fremde Adressen, ist ein filternder Resolver im Spiel.

## Nicht tun

- DNS-Server am Adapter umstellen oder einen Adapter abschalten, um die Zentrale zu reparieren. Das ist eine Systemänderung, und das WLAN gehört eventuell zu einem fremden Netz.
- Nur Frist oder Wiederholungen erhöhen: Bei nacheinander probierten toten Adressen gewinnt nie eine gute.
