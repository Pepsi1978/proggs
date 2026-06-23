# Umsetzungsplan: Franks zweites Gehirn (Hostinger + Mem0 + Qdrant)

> DER verbindliche Aktions-/Steuerungsplan fuer den Bau von Franks selbst gehostetem "zweitem Gehirn".
> Entscheidungen getroffen am 2026-06-21 (nach vollstaendigem Durcharbeiten des `second-brain/`-Wissens).
> Dies ist das **lebende Steuerungsdokument**: Bei Fortschritt hier den Status aktualisieren.
> Wissensbasis (Begruendungen, Details): die anderen Dateien in `best-practices/second-brain/` + die
> 3 Server-Almanache `bugs/server/`. Projekt-Memory: `project_second_brain_memory_server`.
> Trigger kuenftig: **"wir bauen am zweiten Gehirn weiter"**.

> **⚠️ GROSSE ARCHITEKTUR-WENDE (2026-06-23): mem0 ist RAUS.** Die Entscheidungen unten (Mem0 als
> Memory-Kern + LLM-Extraktion) sind ÜBERHOLT. Das Gehirn ist jetzt ein **wortwoertlicher 1:1-Speicher**
> (`brain-api` v1.1.0: qdrant-client + Gemini-Embedding direkt, kein mem0/LLM im Speicher). Titel/Kategorie/
> Datum getrennt im Payload, 4 Abruf-Wege + gefilterte Suche. As-built-Schema: [[speicher-schema-1zu1]];
> Flugplan der naechsten Schritte: Projekt-Memory `project_second_brain_flugplan`. Die Mem0-Abschnitte
> unten bleiben als Historie stehen.

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

## ✅ Aufbau-Stand (2026-06-21, Abend) — Server LAEUFT

**Server:** Hostinger KVM 2, IP `168.231.83.205`, Ubuntu 24.04.4 LTS. SSH key-only (Schluessel in
`~/.ssh/` + Backup `~/SK/second-brain/`), Root-Passwort in Franks Passwortmanager. Deploy-Configs:
`second-brain-server/` (Repo) → `/opt/second-brain/` (Server).

| Schritt | Status |
|---------|--------|
| VPS bestellt (KVM 2, 24-Mon-Laufzeit, Ubuntu 24.04) | ✅ |
| Haertung: SSH key-only, UFW (nur SSH offen), Fail2Ban, System-Updates | ✅ |
| Docker + Compose | ✅ |
| Qdrant (Such-Schrank) — nur 127.0.0.1, API-Key, getestet (ohne Key → 401) | ✅ |
| Ollama + BGE-M3 (lokale Embeddings) | ❌ am 2026-06-22 ENTFERNT (Cloud-KI statt lokal) |
| **Mem0 (Bibliothekar) + Gemini** | ✅ **LIVE** (2026-06-22) — REST `mem0-api`, end-to-end getestet |
| Direktiven einspeichern · externer Zugang · Backup · Apps · Kosten-Caps | offen |

### Offene Entscheidungen vor Mem0 (aus Doku-Pruefung 2026-06-21)
1. **Mem0 braucht ein LLM** fuer die Fakten-Extraktion. Optionen: kleines lokales LLM in Ollama
   (z.B. `llama3.2:3b` — Datenhoheit, +~2 GB, langsamer auf CPU) · ODER `infer=False` (kein LLM,
   speichert Rohtext direkt) · ODER Cloud-LLM (schnell, aber Datenabfluss + Kosten-Caps noetig).
2. **`embedding_model_dims = 1024`** fuer BGE-M3 setzen (NICHT 768 wie die nomic-Beispiele der Doku).
3. **Architektur-Weiche — Referenz-Dokumente vs. persoenliche Memories:** Mem0s Fakten-Extraktion ist
   fuer "Memories ueber Frank" gedacht (Praeferenzen/Fakten). Lange Regel-Dokumente (wie die 3 Direktiven)
   sind evtl. besser als reines RAG (Chunking + Embedding direkt in Qdrant) aufgehoben. Moeglich: ZWEI
   Modi — RAG fuer Wissen/Dokumente + Mem0 fuer persoenliche Memories. VOR dem Einspeichern klaeren.
4. **Mem0 als REST-Server** (Multi-Client) ist weniger klar dokumentiert als die Python-Lib —
   genauer pruefen, wenn der Multi-Client-Zugang dran ist.

### ⚠️ ARCHITEKTUR-KORREKTUR (2026-06-21, Abend) — Datenabfluss egal → Cloud-KI
**Frank-Klarstellung:** Datenhoheit/Datenabfluss ist Frank EGAL ("habe nichts zu verbergen"). Wichtig ist
ihm NUR die Server-Sicherheit (kein Einbruch). Das aendert die KI-Wahl:
- **Embeddings + LLM ueber Cloud-API** statt lokal. Grund: Cloud hat GPUs → schneller + bessere Qualitaet;
  entlastet den kleinen 8GB/2vCPU-Server massiv (RAM/CPU bleibt fuer Qdrant + Daten); einfacheres
  Mem0-Setup (Standard = OpenAI). Datenabfluss ist kein Hindernis mehr.
- **Server-Sicherheit ≠ Datenabfluss** (getrennt): Cloud-APIs = AUSGEHENDE Anfragen, oeffnen KEINE Tuer
  fuer Einbrecher. Die Haertung (Firewall/key-only/Fail2Ban) bleibt voll erhalten.
- **Konsequenz:** Ollama + BGE-M3 werden wieder ENTFERNT (waren auf Datenhoheit optimiert). Stattdessen
  Cloud-Embedder + Cloud-LLM in Mem0. Braucht einen Cloud-API-Key (OpenAI fuer Embeddings+LLM am
  einfachsten; Anthropic hat keine Embeddings). Kosten: Embeddings winzig, LLM mit harten Caps.
- **Die Stack-Tabelle oben (BGE-M3 lokal) ist damit UEBERHOLT** — Cloud ist die neue Wahl. `embedding_model_dims`
  richtet sich dann nach dem Cloud-Modell.

### ✅ FINALE KI-WAHL (2026-06-22, von Frank bestaetigt)
**Anbieter: Google Gemini** (EIN Konto, EIN API-Schluessel fuer beide Bausteine; Cloud, da Datenabfluss egal).
- **LLM (Denken / Fakten-Extraktion / Formulierungs-Veredelung): `gemini-3.1-flash-lite`** — leichte, schnelle,
  guenstige Flash-Variante; ideal, weil die Fakten-Aufgabe simpel ist. **Spaeter frei wechselbar** (z.B.
  OpenRouter-Modell wie nemotron/gemma/mimo), ohne die Embeddings anzufassen.
- **Embeddings (semantische Suche): `gemini-embedding-001`** — mehrsprachig (DE/EN), MTEB-Spitze, flexible
  Dimensionen (768/1536/3072; Vorschlag 1536). **NICHT spaeter wechseln** (Wechsel = gesamter Bestand neu einbetten).
- **Schluessel-Strategie (Frank 2026-06-22): Kosten egal → Bezahl-Tier bevorzugt** (hoehere Limits, keine
  Drosselung, volle Prioritaet; bei Franks Nutzung trotzdem nur Cent/Monat). EIGENER Key fuers Gehirn (NICHT
  der TVO-Key `AIzaSy...`; TVO bleibt getrennt). Kostenloser Default-Projekt-Key (`AQ.Ab8...`, automatisch von
  Google AI Studio/Gemini-CLI angelegt) als Fallback. **Harte Ausgaben-Caps PFLICHT** als Sicherheitsnetz.
  Ablage: `.env` auf dem Server (600) + Backup `~/SK/second-brain/`, NIE ins Repo.
  Free-Tier-Limits grob ~15 Anfragen/Min, ~1000+/Tag (reicht im Alltag; nur Erst-Import koennte bremsen → dann Bezahl).
- **Beim Einrichten verifizieren:** exakte Modellnamen + Dimensionen an der Live-Doku (Google benennt Modelle
  oft um; Wissensstand aelter). Mem0-Konfig: `embedder.provider=gemini`, `llm.provider=gemini`,
  `embedding_model_dims` passend zum Embedding-Modell. Server-Sicherheit ≠ Datenabfluss (Key auf Server ist ok).

---

## ✅✅ MEM0 + GEMINI LIVE (2026-06-22) — Gehirn-Backend funktioniert end-to-end

**Was laeuft (auf `168.231.83.205`, `/opt/second-brain/`):**
- **`sb-mem0-api`** (neuer Container, `second-brain-server/mem0-api/`): schlanker FastAPI-Wrapper um die
  Mem0-Bibliothek. Nur `127.0.0.1:8000`, Bearer-Token-Auth (`SB_API_KEY`), nicht-root-User, RAM-Limit 1G.
  Endpunkte: `GET /health`, `POST /store`, `POST /recall`, `GET /memories`.
- **Mem0 2.0.7** mit **Gemini** (LLM `gemini-3.1-flash-lite`, Embedder `models/gemini-embedding-001` @ **1536**)
  + **Qdrant** (Collection `second_brain`, Vektorgroesse 1536 verifiziert).
- **Ollama/BGE-M3 entfernt** (Orphan-Container weg, `ollama-data` bleibt vorerst auf der Platte).
- **Observability-First:** strukturiertes JSON-Log (`mem0-logs/mem0-api.jsonl`, rotierend + stdout),
  globaler Fehler-Faenger, Logik-Sonden (DIM-Invariante embedder==qdrant), Intent-Checkpoints (store/recall).
- **Kosten:** clientseitiger Tages-Cap (`SB_MAX_LLM_CALLS_PER_DAY`, Default 5000) als Defense-in-Depth;
  der HARTE Cap fehlt noch (Google AI Studio / Cloud-Console Budget — siehe offene TODOs).
- **Verifiziert (end-to-end):** Auth 401 ohne Token; `store infer=false` (Embeddings+Qdrant);
  `store infer=true` (Gemini-Faktenextraktion: "gruener Tee statt Kaffee" sauber extrahiert);
  `recall` rankt korrekt (Tee-Frage→Tee 0.84, Sprach-Frage→Kotlin 0.76). Commits #47069–#47071.

**Schluessel-Hinweis:** Der eingebaute Gemini-Key ist AKTUELL derselbe wie der TVO-Key (`AIzaSy…`) —
der Plan sah einen GETRENNTEN Key vor. Geteiltes Quota/Limit Gehirn↔TVO; bei Bedarf auf eigenen
Bezahl-Key umstellen (nur `.env` GEMINI_API_KEY/GOOGLE_API_KEY + `~/SK/second-brain/brain.env` aendern).

**Zwei geloeste Integrations-Fallen (Direktive #3, dokumentiert in `second-brain/memory-backends.md`):**
1. `qdrant-client` mit gesetztem `api_key` nimmt automatisch `https=True` an → TLS gegen Klartext-HTTP-Qdrant
   → `[SSL: WRONG_VERSION_NUMBER]`. **Fix:** explizite `url="http://host:6333"` statt `host`/`port`.
2. **Mem0 2.x API-Wechsel:** `search()`/`get_all()` brauchen `filters={"user_id": …}` + `top_k=`
   (NICHT mehr `user_id=`/`limit=`). `add()` nimmt weiterhin `user_id=`.

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

## Portabilitaet & geplanter Umzug (Anbieter-Wechsel ab ~Mitte 2028)

> Frank-Entscheidung 2026-06-21: Start auf **Hostinger** (KVM 2, **24-Monats-Laufzeit** = niedrigster
> Preis fuer genau die geplante Nutzungsdauer). **Umzug nach Ablauf der Bindung (~Mitte 2028) ist FEST
> eingeplant** — voraussichtlich zu Hetzner (konstanter Preis, bessere Leistung, EU-Datenschutz), BEVOR
> der Renewal-Preis (~14 EUR) greift. Kein Lock-in, weil alles auf Docker + Standard-Bausteinen laeuft.

**Damit der Umzug kinderleicht wird — von Anfang an (ab Phase 1) einbauen:**
- **Eigene Domain fuer das Gehirn** (z.B. `gehirn.<deinname>.de`) statt der nackten Server-IP. Alle Clients
  (Apps, CLIs) sprechen NUR den Domainnamen an. Ein Umzug ist dann fuer sie nur eine unsichtbare
  DNS-Aenderung (Domain auf die neue Server-IP zeigen) — in den Apps muss NICHTS geaendert werden.
- **Alles als Code/Config in Git** (compose.yaml, Caddyfile, Env-Vorlagen, Agenten) → Stack auf jedem
  Linux-Server 1:1 reproduzierbar.
- **Daten-Volumes auf festem Pfad** + regelmaessiges Offsite-Backup (Leitplanke 5). Ein Umzug = ein
  Backup-Restore auf dem neuen Server.

**Umzug-Vorgang (wenn es soweit ist — ~4 Schritte, Claude macht es per SSH):**
1. Neuen Server (z.B. Hetzner) mieten, Ubuntu sauber.
2. Gleichen Docker-Stack hochfahren (aus Git).
3. Daten-Volume vom alten auf den neuen Server kopieren.
4. Domain auf neue IP umstellen, alten Server abschalten. Fast keine Ausfallzeit (Parallelbetrieb bis Umschaltung).

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

## 🧭 Roadmap & Franks Wunschliste (Stand 2026-06-22, vor Franks Laufrunde)

✅ **FERTIG & getestet:** Server (gehaertet) · Qdrant · **Mem0 + Gemini** (LLM `gemini-3.1-flash-lite`,
Embeddings `gemini-embedding-001` @1536) · REST-API (`mem0-api`, Auth + Observability) ·
**WireGuard-Zugang** (Handy 10.8.0.2 + PC 10.8.0.3, beide verbunden+getestet; Gehirn unter
`http://10.8.0.1:8000`, oeffentlich unsichtbar).

**Franks ausdrueckliche Wuensche (2026-06-22, beim Weggehen genannt) — das wollen wir noch bauen:**
1. ⭐ **Sprach-Anbindung (STT + TTS): sich NATUERLICH mit dem Server unterhalten.** Reinsprechen
   (Speech-to-Text) + das Gehirn antwortet per Stimme (Text-to-Speech). **Hohe Prioritaet fuer Frank.**
2. ⭐ **Oberflaeche zum Reinschauen (Dashboard):** Inhalte sehen / kontrollieren / verwalten. Will Frank fix.
3. **Dirigent** (lokaler Router-Agent): versteht die Anfrage, waehlt die Such-Art, schreibt/liest.
4. **MCP-Anbindung:** Claude Code / OpenCode nutzen das Gehirn automatisch als Gedaechtnis.
5. **Inhalte einspeichern:** 3 Direktiven (RAG-Pfad fuer Dokumente) als erster echter Inhalt.
6. **Backup** (Offsite, taeglich) + Docker-Images auf feste Versionen pinnen.

**Kostenbremse:** Frank sagt, die ist ueber den **Gemini-Key gesetzt** (Google-Seite). → Beim
Wiederaufnehmen nur kurz **verifizieren** (harter Budget-Stopp in der Google-Console); zusaetzlich laeuft
der clientseitige Tages-Cap im `mem0-api` (`SB_MAX_LLM_CALLS_PER_DAY`). Kein dringender Bau-Schritt mehr.

**Design-Skizze fuer den gemeinsamen Brainstorm/Recherche, wenn Frank zurueck ist (NICHT vorab umgesetzt):**
- *Sprach-Anbindung:* nutzt Franks bestehende Bausteine (Groq-STT, Gemini-TTS aus TVO/VoiceAgent).
  Flow: sprechen → STT → Text an `/recall`+`/store` → Antwort → TTS → vorlesen. **Offene Fragen:** eigene
  kleine App (Auto/Handy) ODER an TVO/VoiceAgent andocken? STT/TTS lokal vs. Cloud? Wake-Word noetig?
- *Dashboard:* schlanke Web-Oberflaeche, liest das Gehirn ueber denselben REST-Weg (`10.8.0.1:8000`), nur
  ueber WireGuard erreichbar. **Offene Frage:** erst read-only Inspektion, Editieren spaeter?
- *MCP:* duenner MCP-Server vor `mem0-api` (HTTP-Transport gegen `http://10.8.0.1:8000`). **Offene Frage:**
  eigener MCP-Adapter vs. fertiger mem0-MCP; Scoping pro Client (user/projekt).
- *Dirigent:* Regel/Semantik-Router (LLM nur im Zweifel) — entscheidet store vs. recall vs. Suchart.
- *Reihenfolge-Vorschlag (Claude, mit Frank final abstimmen):* erst **MCP** (sofortiger Alltagsnutzen in
  den CLIs) → dann **Sprach-Anbindung** (Franks Top-Wunsch) → **Dashboard** → **Dirigent** → **Inhalte** →
  **Backup**. (Frank kann anders priorisieren — Sprach-Anbindung + Dashboard sind ihm besonders wichtig.)

**WICHTIG vor jeder Server-Arbeit:** **Windscribe AUS lassen** ODER `168.231.83.205` + Groq vom
Windscribe-VPN ausschliessen — Windscribe ist Full-Tunnel und blockierte sonst SSH UND Groq/TVO
(erlebt 2026-06-22). Unser eigener WireGuard-Tunnel ist Split-Tunnel und stoert nichts (darf anbleiben).
