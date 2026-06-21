# Umsetzungsplan: Franks zweites Gehirn (Hostinger + Mem0 + Qdrant)

> DER verbindliche Aktions-/Steuerungsplan fuer den Bau von Franks selbst gehostetem "zweitem Gehirn".
> Entscheidungen getroffen am 2026-06-21 (nach vollstaendigem Durcharbeiten des `second-brain/`-Wissens).
> Dies ist das **lebende Steuerungsdokument**: Bei Fortschritt hier den Status aktualisieren.
> Wissensbasis (Begruendungen, Details): die anderen Dateien in `best-practices/second-brain/` + die
> 3 Server-Almanache `bugs/server/`. Projekt-Memory: `project_second_brain_memory_server`.
> Trigger kuenftig: **"wir bauen am zweiten Gehirn weiter"**.

---

## ⚡ Die getroffenen Entscheidungen (Stand 2026-06-21)

| Frage | Entscheidung | Begruendung (Kurz) |
|-------|--------------|--------------------|
| **Server** | **Hostinger KVM VPS** (Franks Wunsch) | Root + Docker; machbar fuer Memory-System. Hetzner waere DSGVO/Performance-staerker, aber Hostinger ist bewusst gewaehlt |
| **Memory-Kern** | **Mem0 Open-Source (self-hosted) + Qdrant** | Mem0 = "Bibliothekar" (Apache-2.0, laeuft AUF Franks Server, kein externer Dienst); Qdrant = "Such-Schrank" (semantische + andere Suche) |
| **Hermes / OpenClaw** | **Spaeter, optional** | Nur als Zugangskanal/Frontend, falls je gewuenscht. Das Gehirn braucht sie nicht |
| **LLM (Denken/Formulieren)** | **Cloud-API** (kleines lokales Modell als Option) | Hostinger hat keine GPU. Nur der zu verarbeitende Text-Schnipsel geht raus, das Archiv bleibt im Server |
| **Bau-Tiefe** | **Selbst bauen auf Bausteinen** | Qdrant als erprobter Such-Motor (wie SQLite in Franks Apps); das Gehirn drumherum (Datenmodell, Dirigent, Agenten, REST) bauen wir selbst |

---

## Das mentale Modell (3 Schichten)

```
┌─ KERN (lokal, klein, immer dabei) ──────── = "RAM"
│   Verhaltensregeln (3 Direktiven etc.) — haengen NIE vom Server ab
│
├─ DIRIGENT (lokaler Agent) ──────────────── versteht Anfrage, waehlt Such-Art,
│   ruft den Server, schreibt Wissen zurueck   (+ Sprach-Veredelung via Cloud-KI)
│
└─ SERVER-MEMORY (Hostinger, waechst) ────── = "Festplatte des Gehirns"
    ALLES Wissen: Programmieren · Personen · Inventar · Aufgaben · Journal
    Mem0 (MCP + REST) → Qdrant (Vektor/Suche)
```

**Grenze:** Wissen = Server, Verhalten = lokaler Kern. Bei Server-Ausfall greift der Dirigent auf lokale Dateien zurueck (Graceful Degradation).

---

## Was alles auf EINEM Server laeuft (keine externe Bruecke)

```
DEIN HOSTINGER-SERVER (eine Maschine, alles lokal)
┌───────────────────────────────────────────────┐
│  Caddy        (Tuersteher + TLS-Verschluesselung) │
│  Mem0         (Bibliothekar, Apache-2.0)          │
│  Qdrant       (Such-Schrank = deine Daten)        │
│  Deine Agenten (Dirigent, Speicher-Logik)         │
└────────────────────┬──────────────────────────┘
                     │  einzige Linie nach draussen:
                     ▼
           Cloud-KI (nur Denken/Formulieren des aktuellen Schnipsels)
```

---

## Empfohlener Stack (konkret)

| Schicht | Wahl | Rolle |
|---------|------|-------|
| Server | Hostinger **KVM 2** (2 vCPU / 8 GB) zum Start | Root + Docker; Upgrade auf KVM 4 spaeter moeglich |
| OS | **Ubuntu 24.04** (sauberes Image) | NICHT das AI-Assistant/Ollama-Template (nur Chat-UI) |
| Basis | **Docker + Compose** | Alle Dienste als Container, je mit RAM-Limits + `reservations` |
| Such-Motor | **Qdrant** (1 Container) | semantische + gefilterte Suche, Daten-Volume auf NVMe |
| Memory-Schicht | **Mem0** (self-hosted, Apache-2.0) | MCP fuer CLIs + REST fuer eigene Apps; auf "still ablegen" stellbar |
| Embeddings | **BGE-M3** lokal (DE/EN) | ~98 % OpenAI-Qualitaet, ~1/40 Kosten, null Datenabfluss; vor Einsatz an echten Daten testen |
| LLM | **Cloud-API** (klein-lokal als Option) | Denken/Formulieren/Veredeln; Hostinger hat keine GPU |
| Dirigent | eigener schlanker Router (Regel/Semantic, LLM nur im Zweifel) | versteht Anfrage, waehlt Suche, schreibt |
| Zugang | **Caddy** (Auto-TLS + Bearer-Token) ODER **WireGuard** | DB-Port nie oeffentlich; nur Port 443; KEIN Cloudflare-Tunnel |

---

## Wer macht was

| Schritt | Frank (manuell) | Claude (uebernimmt) |
|---------|-----------------|---------------------|
| VPS bestellen | ✅ Hostinger-Konto, KVM 2, Ubuntu 24.04, bezahlen | Auswahl/Optionen vorab erklaeren |
| Server-Zugang | ✅ initiale Zugangsdaten / SSH bereitstellen | Einrichtung dann per SSH von Franks Rechner aus |
| Haertung, Docker, Mem0, Qdrant, Caddy | — | ✅ komplett (sichtbar, Schritt fuer Schritt) |
| Datenmodell + Dirigent + Agenten | — | ✅ programmieren |
| Eigene Apps / Auto-Sprach-App | ✅ Idee/Wuensche | ✅ REST-Anbindung bauen |
| Entscheidungen (Preis, Plan, Wechsel) | ✅ | Empfehlung + Verifikation |

---

## Phasen-Fahrplan (was zuerst)

**Phase 0 — Server-Fundament**
- [ ] (Frank) VPS bestellen: Hostinger **KVM 2**, **Ubuntu 24.04**, Live-Preis/NVMe pruefen
- [ ] Haertung: SSH key-only, Root-Login aus, Cloud-Firewall default-deny (nur 443 + SSH auf Franks IP), Fail2Ban
- [ ] **Kosten-Caps setzen** (harte Stopps, nicht nur Alerts) — siehe Leitplanken

**Phase 1 — Das Gehirn**
- [ ] Docker + `compose.yaml`: Qdrant + Mem0, je an `127.0.0.1`, mit RAM-Limits + `reservations`
- [ ] Caddy davor (Auto-TLS + Bearer-Token) ODER WireGuard (Dienst nur ueber VPN)
- [ ] BGE-M3-Embeddings lokal an Franks echten DE/EN-Daten testen

**Phase 2 — Datenmodell + erste Kategorie**
- [ ] Einheitliches Record-Schema (Pflicht-Metadaten + erweiterbares `fields`) — siehe `datenmodell.md` §4.1
- [ ] Kategorie **Programmieren** zuerst befuellen (taeglich genutzt = sofortiges Feedback)
- [ ] Claude Code + OpenCode als erste Clients via MCP anbinden

**Phase 3 — Der Dirigent**
- [ ] Hybride Suche: Vektor + BM25 → RRF (k=60) → optional Reranking — siehe `orchestrator-und-suche.md`
- [ ] Router billig (Regel/Semantic), LLM nur im Zweifel
- [ ] Schreib-Pfad: extrahieren statt roh → klassifizieren → Dedup → speichern → Bestaetigung

**Phase 4 — Eigene Apps + Sprach-Veredelung**
- [ ] REST-Schicht (`/store`, `/recall`, `/context`) fuer eigene Apps (Auto-Sprach-App)
- [ ] Veredelungs-Funktion: Rohtext → Cloud-KI "verbessere" → verbesserter Text → speichern

**Phase 5 — Zugangskanaele (optional, spaeter)**
- [ ] Hermes (Frontend) / OpenClaw (Multi-Channel: WhatsApp/Telegram/Auto) andocken — nur falls gewuenscht

**Phase 6 — Qualitaet ueber Jahre (Dauerbetrieb)**
- [ ] Konsolidierungs-Job (Synthese), Entity-Resolution ≠ Dedup, Recency-Weighting, monatlicher Restore-Test

---

## Nicht-verhandelbare Leitplanken (ab Tag 1, aus `bugs/server/self-hosted-ai-agent-server.md`)

| # | Leitplanke | Realer Schaden ohne sie |
|---|------------|--------------------------|
| 1 | **Harte Kosten-Caps** (Stopp, nicht nur Alert): ~100 USD/Tag, ~1000 USD/Monat | Runaway-Loop kostete real **47.000 USD** |
| 2 | **Backend nur an `127.0.0.1`**, nur Port 443 offen, Auth an | 93,4 % von 42.665 Agent-Servern hatten Auth-Bypass |
| 3 | **RAM fuer Vektor-DB einplanen** (Suche laeuft im RAM) | OOM-Crash, DB stirbt im Betrieb |
| 4 | **Kein Cloudflare-Tunnel** fuer persoenliche Daten → eigenes TLS/WireGuard | "alles ueber mich" laege im Klartext bei Dritten |
| 5 | **Eigenes taegliches Backup + Offsite** (Hostinger nur woechentlich gratis) | Datenverlust bei oft schreibender Memory-DB |

---

## Offene Verifikationen (waehrend der Umsetzung, KEIN Web-Research noetig vorab)

1. **Hostinger Live-Preise + NVMe-Specs** zum Kaufzeitpunkt (schwanken je Region; vor Kauf live pruefen).
2. **Mem0 "still ablegen"-Modus** (Rohtext ohne interne LLM-Extraktion) an aktueller Mem0-Doku verifizieren.
3. **Mem0 lokale Ollama-Extraktion** + OSS-Graph-Faehigkeit (vs. Pro-Tier) pruefen.
4. **BGE-M3** an Franks echten Daten testen (praktischer Test, kein Web-Research).
5. **Renewal-Preis** (nicht Aktionspreis) fuer die Dauerkosten ansetzen.

---

## Verweise auf die Wissensbasis

| Thema | Datei |
|-------|-------|
| VPS-Wahl, Dimensionierung, Sicherheit, Backup | `second-brain/server-infrastruktur.md` |
| Mem0/Qdrant/Alternativen-Vergleich | `second-brain/memory-backends.md` |
| Datenmodell + Record-Schema | `second-brain/datenmodell.md` |
| Dirigent + hybride Suche (welche Suche wann) | `second-brain/orchestrator-und-suche.md` |
| Schreib-Pfad / "speicher das" | `second-brain/schreibpfad-ingestion.md` |
| Qualitaet ueber Jahre | `second-brain/qualitaet-pflege.md` |
| Multi-Client (MCP + REST) | `second-brain/multi-client-zugriff.md` |
| Architektur-Muster / Anti-Patterns | `second-brain/referenz-architekturen.md` |
| Hostinger-Spezifika + Fallen | `second-brain/hostinger-second-brain.md` |
| Konkreter Deploy-Bauplan (Caddy/systemd/MCP) | `best-practices/opencode/self-hosted-memory-server.md` |
| Bug-Almanache | `bugs/server/self-hosted-ai-agent-server.md`, `bugs/server/vps-hosting.md`, `bugs/server/wireguard.md` |

---

## Naechster konkreter Schritt

**Frank:** Hostinger-VPS bestellen — **KVM 2** (2 vCPU / 8 GB), OS **Ubuntu 24.04 (sauber)**.
Danach Zugangsdaten bereitstellen; ab dann uebernimmt Claude die komplette Einrichtung per SSH (sichtbar, Schritt fuer Schritt).
