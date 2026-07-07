# Server-Infrastruktur, Sicherheit & Backup Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
