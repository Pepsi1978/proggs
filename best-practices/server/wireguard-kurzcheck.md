# WireGuard (sicherer VPN-Zugriff auf selbst-gehostete Dienste) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck

| # | Situation | Best Practice |
|---|-----------|---------------|
| 1 | Zugriff auf einen Dienst AUF dem VPS | **Split-Tunnel** (`AllowedIPs = 10.x/24` am Client), Dienst an die **VPN-IP** binden — **kein IP-Forwarding/NAT nötig**. |
| 2 | Öffentliche Angriffsfläche | Nur **UDP 51820** (WireGuard) öffentlich; Dienst-Port nie auf `0.0.0.0`, nur `wg0`/VPN-IP. |
| 3 | Sicherheit | `umask 077` für Keys, `chmod 600 wg0.conf`, optional `PresharedKey`. |
| 4 | Stabilität hinter NAT | `PersistentKeepalive = 25` auf der Heim-/Client-Seite. |
| 5 | WireGuard vs. öffentliches TLS+Token | Für den **eigenen** Zugriff ist WireGuard oft die einfachere + sicherere Wahl: der Kanal ist schon verschlüsselt, der Dienst muss gar nicht öffentlich exponiert werden. |
