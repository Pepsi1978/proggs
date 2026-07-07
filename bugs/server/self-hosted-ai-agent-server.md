# Bekannte Bugs & Fallen: Selbst gehosteter KI-Agenten- / Memory-Server (Second Brain) auf VPS

> PFLICHT-Lesen vor dem Aufbau eines selbst gehosteten KI-Agenten- und/oder Memory-/Vektor-Servers
> (Second Brain) auf einem VPS (Hostinger o.ae.). Fokus: Sicherheit, Kosten, Ressourcen, Architektur —
> die teuersten/haeufigsten Fehler. Loesungen funktionserhaltend.
>
> **Stand:** recherchiert 2026-06-21 (3-Engine-Schwarm: :online + Firecrawl + 10 Opus-Researcher;
> Quellen je Eintrag). **Anker:** themenbezogen, keine einzelne Software-Version.
> Gegenstueck (richtige Umsetzung): `best-practices/second-brain/hostinger-second-brain.md`.
> Plattform-Limits Hostinger (kein Dedicated/keine GPU/shared CPU/300 MB-s I/O): dort §1/§10.

---

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

---

## 1. Sicherheit (die teuersten Fehler)

### 1.1 Massen-Falle: ungesicherte Agent-Server ⭐
- **Symptom/Befund:** Scan Feb 2026 — **42.665** oeffentlich erreichbare KI-Agent-Instanzen, **93,4 % mit Auth-Bypass** (offene Ports, Auth deaktiviert, API-Keys nicht als Env-Var, keine Firewall).
- **FIX:** Backend (Agent + Vektor-Store) nur an `127.0.0.1` binden, davor Reverse Proxy; nur Port 443 nach aussen; UFW `deny incoming` + Ziel-Whitelist fuer ausgehenden Verkehr (DNS 53, HTTPS 443); Fail2Ban; alle Aktionen loggen (journalctl) + Alarme bei CPU-Spikes/unerwarteten Verbindungen.
- **Quelle:** bluehost.com (Hermes Agent VPS Security Guide), virtua.cloud (Self-Host AI Agents on a VPS).

### 1.2 Cloudflare Tunnel strippt TLS (kritisch fuer persoenliche Daten) ⭐
- **Symptom:** Bequemer Zugang von aussen via Cloudflare Tunnel — aber CF terminiert/entschluesselt TLS in seinem Netz und sieht den **Klartext**.
- **FIX:** Fuer ein privates "Gehirn" mit persoenlichen Daten KEIN CF Tunnel; eigener TLS-Proxy (Caddy mit automatischem Let's-Encrypt-TLS 1.3, oder Nginx) ODER WireGuard/Pangolin.
- **Quelle:** Opus-Researcher T8 (MCP/API-Security), mehrere Web-Quellen.

### 1.3 Container ist KEINE Sicherheitsgrenze ⭐
- **Ursache:** Standard-Docker-Container teilen den Host-Kernel; aus einem permissiven Container kann ausgebrochen werden.
- **FIX (Hierarchie):** MicroVM (Firecracker/Kata) = staerkste Isolation > gVisor (Syscall-Interception) > gehaerteter Container (`--read-only`, `--no-new-privileges`, Capability-Dropping, nur fuer vertrauenswuerdige Agenten). Agent NIE als root; dedizierter User mit minimalen Rechten.
- **Quelle:** virtua.cloud.

### 1.4 Prompt Injection & Lethal Trifecta
- **Ursache:** Das LLM verarbeitet untrusted Eingaben (User, Dateien, API-Antworten, Webseiten) — jede kann eine Injection-Payload sein. **Lethal Trifecta** (Palo Alto Networks): private Daten + untrusted Inhalt + externe Kommunikation, kombiniert mit persistentem Memory = Datenabfluss-Risiko.
- **Supply-Chain:** ~jedes 8. Paket (341/2857) in einem Agent-Skill-Marktplatz boesartig → nie ungeprueafte Skills/Plugins.
- **FIX:** Eingaben grundsaetzlich untrusted behandeln; Netzwerk-Whitelist (kein unbegrenzter Outbound); Secrets via geschuetzte Env-Datei (600-Perms) ueber systemd `EnvironmentFile`, nie im Klartext-Config.
- **Quelle:** virtua.cloud.

### 1.5 MCP-Server-Auth: Spec-Pflicht
- **FIX:** MCP-Spec verlangt **OAuth 2.1 + PKCE (S256)** + audience-gebundene Tokens (Resource Indicators, RFC 8707) + `.well-known`-Discovery. OAuth-Endpoints NIE mit `Access-Control-Allow-Origin: "*"` (CORS-Wildcard). One-Click-Takeover-Pitfall vermeiden (kein blindes Auto-Approve).
- **Quelle:** systemprompt.io (MCP Server Authentication Security), Opus-Researcher T8.

---

## 2. Kosten-Fallen

### 2.1 Runaway-Agent-Loop ohne Hard-Cap (teuerste Falle ueberhaupt) ⭐
- **Realer Vorfall (Nov 2025):** 4-Agenten-LangChain-Loop, 2 Agenten "ping-pongten" Anfragen, **264 h (11 Tage) = 47.000 USD**. Niemand hatte ein Budget-Limit; ein blosser Alert stoppte nichts.
- **Ursache:** Jeder Agenten-Schritt sendet den GESAMTEN akkumulierten Kontext erneut → Kosten steigen superlinear (5K → 20K → 80K+ Token).
- **FIX:** **Enforcement statt Alerts** — der Cap STOPPT den Agenten (keine LLM-Calls mehr bis Mensch/Policy fortsetzt). Empfohlen: 50 USD/Tag Soft-Alert, **100 USD/Tag Hard-Cutoff, 1.000 USD/Monat Hard-Ceiling** (faengt ~95 % der Runaways); Token-Budget 200-500K/Session.
- **Quelle:** dev.to/waxell (Der 47.000-USD-Loop), waxell.ai, relayplane.com.

### 2.2 VPS-Hidden-Costs ⭐
- **Bandbreiten-Overage oft STUENDLICH abgerechnet:** ein einzelner Spike (Traffic/DDoS/fehlkonfigurierter Sync) loest sofort Overage aus — real **175 USD** bei 2 TB. Relevant fuer Offsite-Backup-Egress!
- **Aktionspreis ≠ Renewal:** beworbener Preis gilt nur bei langer Vorauszahlung; Verlaengerung deutlich hoeher (~+100 %; Hostinger KVM 8: Aktion 25,99 → Renewal 49,99-73,99 $). Fuer Dauerkosten immer den Renewal-Preis rechnen.
- **Extra IPv4 / Snapshots / Egress-Caps** kosten separat.
- **Quelle:** petrosky.io, usavps.com, smarthostfinder.com.

---

## 3. Ressourcen-Engpaesse

### 3.1 RAM-Persistenz-Falle beim Vektor-/Memory-Store (haeufigster Stolperstein) ⭐
- **Trugschluss:** "Vektor-DB persistiert auf Disk" → in vielen Default-Setups (HNSW) laeuft die Suche nach Index-Hydratisierung **im RAM**. Bei RAM-Erschoepfung: Swapping (langsam), OOM-Crash, inkonsistente Latenz.
- **FIX:** RAM-Sizing nach Index (HNSW = voller Index im RAM, RAM skaliert linear mit Index-Groesse; Quantisierung -97 % RAM moeglich, mit Praezisionsverlust). RAM-Reserve 30 %. Day-1-Alarme: Latenz > 100 ms/60 s, RAM > 85 %, Snapshot-Fehler. Richtwert ~1,5 GB pro 1 Mio. 384-dim float32.
- **Quelle:** ranksquire.com (Best Self-Hosted Vector Database 2026).

### 3.2 CPU-Steal "Ghost Load" (VPS-Overselling) ⭐
- **Symptom:** Monitoring zeigt nur 20-30 % CPU + genug RAM, dennoch Request-Stau/Timeouts — der physische Kern bedient andere VMs, waehrend die vCPU "leerlaeuft".
- **FIX:** `steal`-Wert (z.B. in `top`) beobachten; bei dauerhaft hohem Steal Provider/Plan wechseln. Shared-CPU-Plaene (auch Hostinger KVM) sind betroffen.
- **Quelle:** blog.linkdata.com, hostadvice.com.

### 3.3 pgvector-HNSW-Skalierungsfalle
- **Symptom:** oberhalb ~5-10 Mio. Vektoren spuerbar langsamer (Index muss in RAM passen); ab ~2 Mio. Index-Build > 20 Min.
- **FIX:** pgvectorscale-Extension (StreamingDiskANN) ODER Qdrant ab dieser Groesse. Benchmark-Zahlen verschiedener Blogs NICHT 1:1 vergleichbar (1M: Qdrant ~850 QPS vs pgvector ~220; 50M: pgvectorscale 471 vs Qdrant 41 — skala-/hardware-/index-abhaengig).
- **Quelle:** dev.to/kencho, tigerdata.com, kalviumlabs.ai.

---

## 4. Architektur- / Plattform-Fallen

### 4.1 OS-Wechsel auf dem VPS loescht alle Daten ⭐
- **Ursache:** Wechsel des OS-Templates ist eine Neuinstallation — loescht alle Daten + Snapshots; nur separate (externe) Backups bleiben.
- **FIX:** OS bewusst einmal waehlen (Ubuntu 24.04 LTS empfohlen, Support bis 2036); das "AI-Assistant"-Template (Ollama+Open WebUI) fuer autonome Agenten MEIDEN (Chat-UI-Stack). Vor jedem Wechsel externes Backup.
- **Quelle:** Hostinger Help-Center, hostaccent.com, openclawlaunch.com.

### 4.2 Memory-Stack-Auswahlfallen
- **Letta:** erlaubt KEINEN direkten externen Such-Zugriff — Retrieval nur ueber Agenten-Tool-Calls. Falle, wenn eine externe App/CLI direkt im Speicher suchen soll. (Sonst technisch solide: Postgres+pgvector, Port 8283.)
- **supermemory:** Lizenz/Selbsthosting-Umfang in den Quellen widerspruechlich (moeglicherweise closed-source Backend / enterprise-only) — vor Festlegung verifizieren; es existiert eine offene Reimplementierung (s11ngh).
- **Mem0-Marketing-Benchmark:** beworbene 94,4 % stehen unabhaengigen ~49,0 % (LongMemEval) gegenueber; Zep/Graphiti real ~63,8 % — Marketing-Zahlen nicht uebernehmen.
- **Quelle:** Opus-/online-Researcher T7, particula, vectorize, supermemory-docs.

### 4.3 Backups: "vorhanden" ≠ "wiederherstellbar"
- **Falle:** Der haeufigste Backup-Fehler ist ein nie getesteter Restore.
- **FIX:** 3-2-1 (3 Kopien, 2 Medientypen, 1 offsite) → besser **3-2-1-1-0** (+1 immutable/air-gapped gegen Ransomware, +0 null Fehler durch monatlichen Restore-Test). Verschluesselt (restic + Backblaze B2/Wasabi), Object-Lock, MFA, getrennte Konten. Echtes Offsite = physisch getrennt + netzwerk-isoliert.
- **Quelle:** acronis.com, zeonedge.com.

---

## 🔗 Bezugs-Tabelle: Bug-Almanach ↔ Best-Practice

| Falle (diese Datei) | Richtige Umsetzung in `best-practices/second-brain/hostinger-second-brain.md` |
|---|---|
| §1 Sicherheit (Auth-Bypass, CF-TLS, Container, Injection, MCP-Auth) | §7 (MCP/API von aussen sicher) + §9 (Sicherheit) |
| §2 Kosten (Runaway-Loop, VPS-Hidden) | §9 + §10 (Fallen) |
| §3 Ressourcen (RAM-Persistenz, CPU-Steal, pgvector) | §1 (Plattform-Limits) + §5/§6 (Vektor-DB) |
| §4 Architektur (OS-Wechsel, Memory-Stack, Backups) | §2 (OS) + §6 (Memory-Stacks) + §9 (Backups) |

---

## Was NIEMALS passieren darf
- ❌ Agent-Server / Vektor-Store direkt ans Internet binden (statt 127.0.0.1 + Reverse Proxy)
- ❌ LLM-Kosten nur per Alert "ueberwachen" statt hart cappen (Enforcement)
- ❌ Annehmen, eine Vektor-DB liege "auf Disk" und RAM nicht dimensionieren → OOM
- ❌ Cloudflare Tunnel fuer persoenliche Daten (TLS-Strip) ohne eigenen TLS-Proxy
- ❌ Standard-Docker-Container als Sicherheitsgrenze fuer untrusted Agent-Code ansehen
- ❌ Ungeprueafte Agent-Skills/Plugins installieren (Supply-Chain)
- ❌ OS-Template wechseln ohne externes Backup (loescht alle Daten)
- ❌ Backup haben, aber den Restore nie testen
