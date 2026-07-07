# Server-Infrastruktur, Sicherheit & Backup — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. VPS-Wahl + Dimensionierung + Härtung + Backup für den selbstgehosteten
> Memory-Server. Quellen: `extern` (VPS-Vergleiche, Vektor-DB-Hosting, DSGVO/Backup-Guides 2025-2026).
> Koppelt zurück auf [[../server/vps-hosting]], [[../server/wireguard]] und [[../opencode/self-hosted-memory-server]]
> (der bereits Caddy/TLS/Firewall/systemd für supermemory dokumentiert — hier füllen wir die A1-Lücken damit).

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| VPS-Empfehlung | **Hetzner Cloud CX32** (4 vCPU / 8 GB / 80 GB NVMe, ~6,80 €/Mo, EU/ISO-27001/DSGVO) — bestes Preis/Leistung |
| Budget-Alternative | Hetzner CX22 (~3,79 €) ohne lokales LLM; Contabo VPS 10 (mehr RAM/€, aber CPU-oversubscribed + langsamere I/O); Oracle Always Free (24 GB ARM, 0 €, aber Setup-Schmerz) |
| Hostinger? | geht (1-Click, 8 GB), aber 24-Mo-Bindung + Renewal-Verdopplung auf 14,99 $ + Template zielt auf Agent, nicht Qdrant → für DIY-Memory eher Hetzner |
| Vektor-DB | **Qdrant** (Rust, Single-Binary, Docker, Sub-30ms, 3 ms/99,2 % Recall) für <1 Mio. Vektoren |
| Deployment | **Docker + Compose**, `restart: unless-stopped`, Volume auf NVMe (nicht Container-Overlay) |
| DB-Port | **NIE öffentlich.** An 127.0.0.1 binden + Reverse-Proxy (Caddy, Auto-TLS) davor; Cloud-Firewall nur 80/443; DB-Port nur localhost/App-IP. API-Key an der DB selbst |
| Verschlüsselung | at-rest LUKS (Default), in-transit TLS 1.2+; AES-256; Schlüssel in der EU |
| Backup | **3-2-1** (3 Kopien, 2 Medien, 1 Offsite-EU), AES-256, immutable gegen Ransomware, PITR. **Embeddings sind nicht trivial neu generierbar → Offsite PFLICHT** |
| Restore | **regelmäßig automatisiert testen — Art. 32 DSGVO-Pflicht**, in isolierte Umgebung |
| "Recht auf Vergessen" | **per-User/per-Record-Encryption-Key** → gezielte Löschung möglich (Key wegwerfen = Daten weg, auch in Backups) |
| DSGVO-Realität | "alles über mich" steht im Spannungsfeld zur Datenminimierung; Self-Host = du bist Verantwortlicher; "privat" ≠ "ausgenommen" |

---

## 1. VPS-Vergleich (`extern`)
Empfehlung **Hetzner CX32** (4 vCPU/8 GB/80 GB NVMe, ~6,80 €): bestes Preis/Leistung, vCPUs nicht oversold,
NVMe spürbar beim Container-Start, 20 TB Traffic + Cloud-Firewall + DDoS inklusive, EU/ISO-27001/DSGVO.
8 GB erlauben Qdrant + Embedding-Service + Reverse-Proxy + kleines 7B-LLM (4-6 GB quantisiert).

| Plan | Specs | Preis | Rolle |
|------|-------|-------|-------|
| Hetzner CX22 | 2 vCPU / 4 GB / 40 GB | ~3,79 € | Minimum (Embedding-API extern, kein lokales LLM) |
| **Hetzner CX32** | **4 vCPU / 8 GB / 80 GB** | **~6,80 €** | **Empfehlung** |
| Contabo VPS 10 | 4 vCPU / 8 GB / 75 GB | ~3,60 € (annual) | RAM/€-König, ABER CPU-oversubscribed + langsamere I/O |
| Hostinger 1-Click | 2 vCPU / 8 GB / 100 GB | 6,99→14,99 $ | nur wenn DIY-frei gewünscht; 24-Mo-Bindung |
| Hetzner AX52 | 12 vCPU / 64 GB / 1,92 TB | ~60 € | erst bei sehr großem Index (10 Mio. Vektoren) |
| Oracle Always Free | 4 ARM / 24 GB / 200 GB | 0 € | 7B-LLM + Memory gratis, aber Setup-Schmerz/Reclaim-Risiko |

**Sizing-Faustregel:** Persönliches PKM (1-100k Notizen) = 2 vCPU/4 GB/40 GB; größerer Index (100k-1 Mio.) =
4 vCPU/8 GB/80 GB; **plus lokales 7B-LLM** = 16 GB RAM Minimum. Kostenhinweis: Bei externer LLM-API ist die
API meist teurer als der VPS ("3-5× VPS-Kosten für Tokens").

## 2. Vektor-DB & Deployment (`extern`)
**Qdrant** ist die konsistente Empfehlung für <1 Mio. Vektoren (Rust, Single-Binary, Docker, Sub-30ms).
Alternativen: Weaviate (Hybrid+Multi-Tenancy), Milvus (Billion-Scale/K8s), pgvector (wenn Postgres eh da, <50M),
Chroma (Prototyp). Deployment ausschließlich containerisiert: Docker + Compose, `restart: unless-stopped`,
Volume auf NVMe (`/mnt/nvme0/qdrant-data:/qdrant/storage`), API-Key via ENV (`QDRANT__SERVICE__API_KEY`).

## 3. Reverse-Proxy, TLS, systemd (A1-Lücke → aus dem bestehenden Bauplan gefüllt)
A1 belegte das nicht (1-Click-Pfade nutzen Nginx). Der bestehende [[../opencode/self-hosted-memory-server]] §3.6
hat es aber bereits konkret: **Caddy** als Reverse-Proxy mit **Auto-HTTPS (Let's Encrypt, Auto-Renewal)** +
Bearer-Token-Auth; Dienst bleibt an **127.0.0.1**; selbst gebautes **systemd-Unit** (`Restart=always`,
`WantedBy=multi-user.target`, `EnvironmentFile`). Optional noch sicherer: Dienst NUR über **WireGuard**
erreichbar statt öffentlich (siehe [[../server/wireguard]], Split-Tunnel).

## 4. Cloud-Firewall vs. Docker-UFW-Falle (`extern`, teils indirekt)
- **Hetzner-Cloud-Firewall** (kostenlos): default-deny, nur 80/443 öffentlich — greift VOR Docker/iptables und
  umgeht damit die Docker-UFW-Falle (Docker öffnet Ports via iptables direkt, an UFW vorbei).
- DB-Ports (6333/6334) NUR von localhost/App-IP (UFW-Regel `ufw allow from APP_IP to any port 6333`).
- Minimalstandard: API-Key an der DB + IP-Allowlist + TLS-Reverse-Proxy davor + Cloud-Firewall als zusätzliche Schicht.

## 5. Sicherheit & DSGVO (`extern`)
**Verschlüsselung:** at-rest LUKS (Default) oder pro-DB + Schlüsselrotation; in-transit TLS 1.2+ erzwingen;
AES-256; Schlüssel ausschließlich EU. **Auth-Härtung:** MFA am Provider-Konto, zertifikatsbasierter DB-Zugriff,
IP-Allowlisting, RBAC/Least-Privilege (`app_readonly`/`app_writer`), MFA-Genehmigung für Backups.
**EU-Hosting:** eliminiert CLOUD-Act/FISA-Bedenken nach Schrems II; EU-Datenresidenz, einfachere Compliance.
**Secrets:** zentrales Key-Lifecycle, automatische Rotation; **per-User-Encryption-Key** als Best Practice
(jeder Datensatz eigener Schlüssel → gezielte Löschung fürs "Recht auf Vergessen", auch in Backups).
**Logging:** Audit-Logging (pgaudit), Anomalie-Erkennung, Logs anonymisieren (IPs trunkieren).

**DSGVO-Implikationen für "alles über mich" (wichtig):** steht im **Spannungsfeld zur Datenminimierung**
(Art. 5) und Speicherbegrenzung; Self-Hosting macht dich zu **Controller + Processor** mit voller Verantwortung,
sobald personenbezogene Daten Dritter (Kontakte, Notizen über andere) drin sind — "privat" heißt NICHT
"ausgenommen". Praktisch: klare Retention-Policy, Daten-Inventar, Export (JSON/CSV) für Portabilität,
definierter Lösch-Prozess inkl. Backups.

## 6. Backup-Strategie (`extern`)
- **3-2-1:** 3 Kopien, 2 Storage-Typen, 1 Offsite (EU-S3-kompatibel).
- **AES-256** für Backup-Dateien, Schlüssel sicher verwaltet.
- **Immutable Backups** (gegen Ransomware-Verschlüsselung/Löschung).
- **Granular** (nur nötige Datensätze, nicht ganze Disks) + Lifecycle-Auto-Expire (30-180 Tage).
- **PITR** (PostgreSQL WAL-Archivierung / MySQL Binlog) für RDBMS-Anteile.
- **Embeddings:** nicht trivial neu generierbar → **Offsite-Backup zwingend** (sonst tagelanges Re-Embedding
  bei Verlust; Rohtext-Chunks zusätzlich behalten, siehe [[qualitaet-pflege]] §6).
- **Restore TESTEN:** automatisierte Verifikation in isolierte Umgebung — Art. 32 DSGVO-Pflicht, nicht optional.

## 7. Sicherheits-Checkliste (komprimiert, `extern`)
☐ EU-Rechenzentrum + DPA ☐ at-rest LUKS ☐ TLS 1.2+ ☐ Security-Patches ☐ Zero-Trust-Segmentierung
☐ MFA ☐ RBAC/Least-Privilege ☐ IP-Allowlist ☐ per-User-Key ☐ Audit-Logging + Anomalie-Erkennung
☐ 3-2-1 + Offsite-EU ☐ AES-256-Backup ☐ Immutability ☐ PITR ☐ automatisierte Restore-Tests
☐ Lösch-/Auskunfts-/Portabilitäts-Prozess (Art. 15/17) ☐ Daten-Inventar ☐ Retention-Policy ☐ DPIA bei High-Risk.

## Offene Lücken (selbst nachziehen)
A1-Quellen ließen offen: konkrete Caddy/Traefik-vs-Nginx-Entscheidung (→ Caddy aus dem Bauplan übernommen),
systemd-Unit-Detail (→ Bauplan), Uptime-Monitoring (UptimeRobot/Healthchecks — keine Quelle), Telemetrie-Abschalten
(keine Quelle, aber bei self-hosted Tools `*_DISABLE_TELEMETRY=1` setzen), WAF/IDS auf App-Layer.

## Quellen (`extern`, 2025-2026)
VPS-Vergleiche (Hetzner/Contabo/Hostinger/Oracle Preis-Leistung, 2026-Stand); Qdrant/Weaviate/Milvus-Self-Hosting-Guides;
DSGVO/Backup-Praxis (LUKS, TLS 1.2+, 3-2-1, per-User-Key, Art. 32 Restore-Test, Schrems II). Ergänzend bestehend:
`best-practices/server/vps-hosting.md`, `server/wireguard.md`, `opencode/self-hosted-memory-server.md`.
