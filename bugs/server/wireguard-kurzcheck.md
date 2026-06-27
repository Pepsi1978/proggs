# WireGuard (VPN für sicheren Zugriff auf selbst-gehostete Dienste) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ „Brauche ich IP-Forwarding/NAT, um auf einen Dienst AUF dem VPS zuzugreifen?" | **NEIN.** Forwarding/NAT gilt nur fürs **Full-Tunnel-Routing zwischen Interfaces** (z.B. Client→Internet über VPS). Ein Dienst, der an die WireGuard-IP gebunden ist, wird **lokal** zugestellt — kein `ip_forward` nötig. (Verbreiteter Mythos, den viele Tutorials/KI-Antworten falsch machen.) | §1 |
| 2 | Dienst soll NUR über VPN erreichbar sein | Dienst an die **WireGuard-IP** (z.B. `10.x.x.1`) binden statt `0.0.0.0`; zusätzlich Firewall `-i wg0 ACCEPT` + sonst DROP. **Einziger öffentlicher Port: UDP `ListenPort` (51820).** | §2 |
| 3 | Schlüssel erzeugen | `umask 077` davor (`wg genkey \| tee priv \| wg pubkey > pub`), sonst sind Private Keys lesbar. `chmod 600 wg0.conf`. | §3 |
| 4 | Tunnel stirbt nach kurzer Zeit (Client hinter NAT) | `PersistentKeepalive = 25` auf der NAT-Seite (hält das NAT-Mapping offen). | §4 |
| 5 | `AllowedIPs` falsch verstanden | Ist **Routing-Filter**, kein „erlaubt"-Flag: Server-Peer = Client-IP `/32`; Client-Peer = was durch den Tunnel soll (`10.x/24` = Split-Tunnel nur Dienst; `0.0.0.0/0, ::/0` = Full-Tunnel). | §5 |
| 6 | DNS-Leak / Abfragen am Tunnel vorbei | `DNS =` im `[Interface]` des Clients setzen (z.B. VPN-IP des Servers, falls dort ein Resolver läuft). | §6 |
| 7 | Interface kommt nach Reboot nicht hoch | `systemctl enable wg-quick@wg0` (Datei in `/etc/wireguard/wg0.conf`). | §7 |
