# Best Practices: WireGuard (sicherer VPN-Zugriff auf selbst-gehostete Dienste)

> **Zweite Seite der Medaille** zum Almanach [`bugs/server/wireguard.md`](../../bugs/server/wireguard.md):
> wie man einen Dienst (z.B. den self-hosted Memory-Server) sicher per WireGuard erreichbar macht,
> ohne ihn öffentlich ins Netz zu stellen. **Stand 2026-06-19. Anker: wireguard-tools (stabil).**

---

## ⚡ Kurzcheck

| # | Situation | Best Practice |
|---|-----------|---------------|
| 1 | Zugriff auf einen Dienst AUF dem VPS | **Split-Tunnel** (`AllowedIPs = 10.x/24` am Client), Dienst an die **VPN-IP** binden — **kein IP-Forwarding/NAT nötig**. |
| 2 | Öffentliche Angriffsfläche | Nur **UDP 51820** (WireGuard) öffentlich; Dienst-Port nie auf `0.0.0.0`, nur `wg0`/VPN-IP. |
| 3 | Sicherheit | `umask 077` für Keys, `chmod 600 wg0.conf`, optional `PresharedKey`. |
| 4 | Stabilität hinter NAT | `PersistentKeepalive = 25` auf der Heim-/Client-Seite. |
| 5 | WireGuard vs. öffentliches TLS+Token | Für den **eigenen** Zugriff ist WireGuard oft die einfachere + sicherere Wahl: der Kanal ist schon verschlüsselt, der Dienst muss gar nicht öffentlich exponiert werden. |

---

## 1. Das Split-Tunnel-Pattern (Dienst-Zugriff, empfohlen)

Ziel: von zu Hause sicher auf einen Dienst (z.B. Memory-MCP auf Port 8080) auf dem VPS zugreifen, **ohne** den
Port öffentlich zu öffnen.

**Server `/etc/wireguard/wg0.conf`:**
```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = <SERVER_PRIV>
# KEIN PostUp-MASQUERADE/ip_forward nötig — Dienst läuft AUF diesem Host
[Peer]
PublicKey = <CLIENT_PUB>
AllowedIPs = 10.8.0.2/32
```
**Client `wg0.conf`:**
```ini
[Interface]
Address = 10.8.0.2/24
PrivateKey = <CLIENT_PRIV>
DNS = 10.8.0.1            # optional, gegen DNS-Leak
[Peer]
PublicKey = <SERVER_PUB>
Endpoint = <VPS_PUBLIC_IP>:51820
AllowedIPs = 10.8.0.0/24  # Split-Tunnel: NUR das VPN-Subnetz (nicht 0.0.0.0/0)
PersistentKeepalive = 25
```
**Dienst absichern:** Dienst an `10.8.0.1:8080` binden (nicht `0.0.0.0`), Firewall `-i wg0 ACCEPT` + sonst DROP,
auf der Cloud-Firewall nur UDP 51820 öffnen. → Der Dienst ist aus dem öffentlichen Internet **unsichtbar**.

## 2. Sichere Konfig (Pflicht-Punkte)
- **Keys:** `umask 077; wg genkey | tee priv | wg pubkey > pub`; `chmod 600 wg0.conf`; optional `PresharedKey` pro Peer.
- **Keepalive:** `PersistentKeepalive = 25` auf der Seite hinter NAT (i.d.R. der Client).
- **Autostart:** `systemctl enable --now wg-quick@wg0`.
- **Nur wenn Full-Tunnel gewünscht** (Client→Internet über VPS): erst DANN `net.ipv4.ip_forward=1` + MASQUERADE
  (`PostUp = iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE`) und `AllowedIPs = 0.0.0.0/0, ::/0` am Client.

## 3. WireGuard statt öffentlichem TLS+Token (für den Memory-Server)
Statt den Memory-MCP öffentlich mit Caddy-TLS + Bearer-Token zu exponieren, ist WireGuard für den **eigenen**
Zugriff meist die bessere Wahl: nur deine Geräte (mit gültigem Key) erreichen den Dienst überhaupt, der Kanal ist
verschlüsselt, und es ist **kein** öffentlicher Dienst-Port nötig. (Verweis: `best-practices/server/vps-hosting.md`,
`best-practices/opencode/self-hosted-memory-server.md`.)

## 🔗 Bezugs-Tabelle
| Best-Practice | Bug-Abschnitt (`bugs/server/wireguard.md`) |
|---|---|
| §1 Split-Tunnel | §1 (Forwarding-Mythos), §2 (Dienst nur über VPN), §5 (AllowedIPs) |
| §2 Sichere Konfig | §3, §4, §6, §7 |

## Quellen
- wireguard.com/quickstart · wiki.archlinux.org/title/WireGuard · A/B-Research 2026-06-19.
