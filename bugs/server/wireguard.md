# Bekannte Bugs & Fallen: WireGuard (VPN für sicheren Zugriff auf selbst-gehostete Dienste)

> **PFLICHT-LESEN vor Arbeit an einem WireGuard-Setup** (`wg0.conf`, `wg`/`wg-quick`,
> `systemd wg-quick@`, VPS-Dienst hinter VPN). Kuratiert aus wireguard.com/quickstart +
> wiki.archlinux.org/title/WireGuard + eigenem A/B-Research (2026-06-19, Opus-Researcher
> korrigierte einen verbreiteten Mythos, den ein günstigeres Modell übernahm). Lösungen sind
> funktionserhaltend (Direktive #3).
>
> **Zweite Seite der Medaille:** [`best-practices/server/wireguard.md`](../../best-practices/server/wireguard.md).
> **Stand:** 2026-06-19. **Anker:** wireguard-tools (Protokoll stabil, versioniert nicht schnell).

---

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

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice
| Bug-Abschnitt | Best-Practice (`best-practices/server/wireguard.md`) |
|---|---|
| §1–§2 (Forwarding-Mythos, Dienst nur über VPN) | §1 Split-Tunnel-Pattern (Dienst-Zugriff) |
| §3–§7 (Keys, Keepalive, AllowedIPs, DNS, Autostart) | §2 Sichere Konfig |

---

## 1. ⭐ IP-Forwarding/NAT ist für Dienst-AUF-dem-Host NICHT nötig (Mythos)
**Symptom/Mythos:** Viele Anleitungen (und schwächere KI-Modelle) behaupten, man müsse `net.ipv4.ip_forward=1`
+ MASQUERADE setzen, um per WireGuard auf einen Dienst auf dem VPS zuzugreifen.
**Realität:** **IP-Forwarding ist nur nötig, wenn der VPS Pakete zwischen ZWEI Interfaces weiterleitet** —
also Full-Tunnel (Client→Internet über den VPS) oder Routing in andere Subnetze. Ein Paket an `10.x.x.1:8080`
(die WireGuard-IP des Servers, an die der Dienst gebunden ist) wird **lokal an den Prozess zugestellt**, nicht
„weitergeleitet". Für reinen Dienst-Zugriff genügt also **Split-Tunnel ohne Forwarding/NAT**.
**FIX:** Dienst an die WireGuard-IP binden (§2), `AllowedIPs` am Client auf das VPN-Subnetz (`10.x/24`) — fertig.
IP-Forwarding/MASQUERADE NUR aktivieren, wenn Clients auch ins Internet getunnelt werden sollen.
**Belegt:** A/B-Research 2026-06-19 (Opus fing diesen Punkt; ein günstigeres Modell übernahm den Mythos).
**Quelle:** wireguard.com/quickstart (NAT-Traversal nur bzgl. Keepalive); wiki.archlinux.org/title/WireGuard.

## 2. Dienst NUR über das VPN erreichbar machen
**Ziel:** Der Dienst-Port (z.B. 8080) darf NICHT öffentlich offen sein.
**FIX (funktionserhaltend):**
1. Dienst an die **WireGuard-IP** binden: `--listen 10.x.x.1:8080` (oder `127.0.0.1` + nur lokal). NICHT `0.0.0.0`.
2. Firewall: `iptables -A INPUT -i wg0 -p tcp --dport 8080 -j ACCEPT` + `iptables -A INPUT -p tcp --dport 8080 -j DROP`
   (auch wenn der Dienst versehentlich auf `0.0.0.0` lauscht, kommt nur wg0-Verkehr durch).
3. **Einziger öffentlicher Port:** UDP `ListenPort` (51820) auf der VPS-/Cloud-Firewall freigeben.
**Kombination mit Reverse-Proxy:** Caddy/nginx + TLS sind dann **optional** — über WireGuard ist der Kanal schon
verschlüsselt; man kann den MCP-/HTTP-Dienst direkt an die VPN-IP binden (einfacher als öffentliche TLS+Token-Lösung).

## 3. Schlüssel-Erzeugung & Dateirechte
`umask 077; wg genkey | tee privatekey | wg pubkey > publickey`. `chmod 600 /etc/wireguard/wg0.conf`.
Optional `PresharedKey` (`wg genpsk`) für zusätzliche symmetrische Sicherheit pro Peer.

## 4. PersistentKeepalive hinter NAT
Steht eine Seite hinter NAT/Stateful-Firewall (typisch: der Client zu Hause), `PersistentKeepalive = 25` setzen,
sonst läuft das NAT-Mapping ab und eingehende Pakete kommen nicht mehr durch. Default 0 = aus.

## 5. `AllowedIPs`-Semantik (Routing, nicht „erlaubt")
- **Server-Peer (Client):** `AllowedIPs = 10.x.x.2/32` (genau die VPN-IP des Clients).
- **Client-Peer (Server):** `AllowedIPs = 10.x.x.0/24` → **Split-Tunnel** (nur VPN-Subnetz/Dienst). `0.0.0.0/0, ::/0`
  → **Full-Tunnel** (aller Verkehr über VPS — DANN braucht der Server Forwarding+MASQUERADE, siehe §1).

## 6. DNS-Leak vermeiden
`DNS =` im Client-`[Interface]` setzen (z.B. die VPN-IP des Servers, wenn dort ein Resolver läuft), sonst laufen
DNS-Abfragen am Tunnel vorbei über das physische Interface.

## 7. Autostart
Datei nach `/etc/wireguard/wg0.conf`, dann `systemctl enable --now wg-quick@wg0`.

---

## Quellen (Stand 2026-06-19)
- wireguard.com/quickstart (Konfig-Grundlagen, NAT-Traversal/Keepalive)
- wiki.archlinux.org/title/WireGuard (Keys, AllowedIPs, Routing, systemd, Forwarding nur für Full-Tunnel)
- Eigenes A/B-Research 2026-06-19 (Opus-Korrektur des IP-Forwarding-Mythos).
