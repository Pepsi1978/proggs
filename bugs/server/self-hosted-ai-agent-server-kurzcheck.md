# Selbst gehosteter KI-Agenten- / Memory-Server (Second Brain) auf VPS Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel |
|---|--------------------|--------------|
| 1 | ⭐ Agent-Server exponiert | **93,4 % von 42.665** gescannten KI-Agent-Servern hatten Auth-Bypass (offene Ports, Auth aus). Backend NUR an `127.0.0.1`, nur Port 443 offen, UFW deny-incoming + Fail2Ban |
| 2 | ⭐ LLM-Kosten "ueberwacht" | **Alerts ≠ Enforcement.** Realer Runaway-Loop: 264 h = **47.000 USD**. Harte Caps (50/100 USD-Tag, 1000 USD-Monat) die den Agent STOPPEN, nicht nur mailen; Token-Budget 200-500K/Session |
| 3 | ⭐ "Vektor-DB persistiert auf Disk" | Trugschluss: Suche laeuft nach Index-Hydratisierung **im RAM** → bei RAM-Erschoepfung Swapping/OOM-Crash. RAM-Reserve 30 %, Alarm bei RAM > 85 % |
| 4 | ⭐ Cloudflare Tunnel fuer privates Gehirn | CF Tunnel **strippt TLS im CF-Netz → sieht Klartext.** Fuer persoenliche Daten eigener TLS-Proxy (Caddy Auto-TLS) oder WireGuard/Pangolin, nicht CF Tunnel |
| 5 | ⭐ Container als Sicherheitsgrenze | Standard-Docker-Container ist KEINE Sandbox (geteilter Host-Kernel, Ausbruch moeglich). Untrusted Agent-Code: MicroVM (Firecracker/Kata) > gVisor > gehaerteter Container |
| 6 | MCP-Server-Auth | MCP-Spec verlangt **OAuth 2.1 + PKCE(S256) + Resource Indicators (RFC 8707)**; OAuth-Endpoint NIE `Access-Control-Allow-Origin: *` (Wildcard) |
| 7 | Prompt Injection / Lethal Trifecta | Agent-Eingaben (User/Datei/Web/API) sind UNTRUSTED. Gefaehrlich: private Daten + untrusted Inhalt + externe Kommunikation + persistentes Memory zusammen → Datenabfluss |
| 8 | Agent-Skill/Plugin installieren | Supply-Chain: ~jedes 8. Paket (341/2857) in einem Skill-Marktplatz war boesartig. Nie ungeprueafte Skills/Plugins; Secrets nie im Klartext (600-Env via systemd) |
| 9 | Memory-Stack-Wahl | **Letta erlaubt KEINEN direkten externen Such-Zugriff** (nur via Agent-Tool-Calls) — Auswahl-Falle, wenn die App selbst suchen soll. supermemory-Lizenz/Selbsthosting widerspruechlich (evtl. closed Backend) |
| 10 | VPS-Kosten | Bandbreiten-Overage oft **stuendlich** abgerechnet (ein Spike → sofort Overage, real 175 USD bei 2 TB). Aktionspreis ≠ Renewal (~+100 %). CPU-Steal "Ghost Load" (20-30 % CPU, dennoch Timeouts) |
| 11 | OS-Wechsel auf dem VPS | Wechsel des OS-Templates **LOESCHT alle Daten + Snapshots** (nur separate Backups bleiben) — OS-Wahl ist eine Festlegung |
| 12 | Backup vorhanden | "Backup" ≠ "wiederherstellbar". Restore **monatlich testen** (3-2-1-1-0: +1 immutable/air-gapped, +0 null Fehler) |
