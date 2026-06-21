# Session Handoff — 2026-06-22, ~00:30

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" bauen: ein selbst gehosteter, ueberall erreichbarer Memory-Server fuer
ALLES (nicht nur Programmieren). Heute wurde das komplette Fundament gebaut und ALLE Architektur-
Entscheidungen final geklaert. Naechste Phase: Mem0 (Bibliothekar) + Direktiven als ersten Inhalt.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Frank hat bewusst fuer heute Schluss
gemacht (Feierabend), morgen weiter. Wiedereinstieg ueber Trigger: **"wir bauen am zweiten Gehirn weiter"**.

## Aktueller Status
- **Server LAEUFT:** Hostinger KVM 2, IP `168.231.83.205`, Ubuntu 24.04.4 LTS (2 vCPU / 8 GB / 96 GB).
- **Zugang:** SSH key-only. Schluessel: `~/.ssh/id_ed25519` (+ Backup `~/SK/second-brain/`). Root-PW
  in Franks Passwortmanager. Login getestet: `ssh root@168.231.83.205` funktioniert (per Key, ohne PW).
- **Gehaertet:** UFW-Firewall (nur SSH/22 offen), Fail2Ban, System-Updates, Passwort-Login aus.
- **Docker + Compose** installiert. Stack-Configs: Repo `second-brain-server/` → Server `/opt/second-brain/`.
- **Qdrant** (Such-Schrank) laeuft, nur 127.0.0.1, API-Key (in `/opt/second-brain/.env` + Backup
  `~/SK/second-brain/qdrant.env`), getestet (ohne Key → 401).
- **Ollama + BGE-M3** laeuft AKTUELL noch — wird aber im naechsten Schritt WIEDER ENTFERNT (s.u.).
- Alles committed bis #47064. Nichts uncommitted.

## Getroffene Entscheidungen (ALLE final, in best-practices/second-brain/UMSETZUNGSPLAN.md dokumentiert)
- **Server:** Hostinger KVM 2, 24-Monats-Laufzeit. **Umzug ~Mitte 2028 geplant** (vor Renewal-Preis,
  vermutlich zu Hetzner). Domain-Trick (eigene Domain statt IP) macht Umzug = nur DNS-Aenderung.
- **Memory-Kern:** Mem0 (Bibliothekar) + Qdrant (Such-Schrank). Bau-Tiefe: selbst bauen auf Bausteinen.
- **WICHTIG — Datenabfluss ist Frank EGAL** (nur Server-Einbruchschutz zaehlt) → **Cloud-KI statt lokal**.
  Daher Ollama/BGE-M3 wird entfernt.
- **FINALE KI-WAHL:** Google Gemini fuer beides (1 Key). LLM = `gemini-3.1-flash-lite` (spaeter frei
  wechselbar, z.B. OpenRouter-Modell). Embeddings = `gemini-embedding-001` (mehrsprachig, ~1536 dim,
  NICHT wechseln). OpenRouter kann KEINE Embeddings.
- **Schluessel-Strategie:** Kosten egal → Bezahl-Tier bevorzugt (hoehere Limits, keine Drosselung),
  EIGENER Key fuers Gehirn (NICHT der TVO-Key `AIzaSy...`). Free-Tier-Key (`AQ.Ab8...`, Googles
  automatisch angelegtes "Default Gemini Projekt") als Fallback. Harte Ausgaben-Caps PFLICHT.
- **Zwei Speicher-Wege noetig:** (1) RAG/Chunking fuer Dokumente (z.B. die 3 Direktiven), (2) Mem0-
  Fakten-Extraktion fuer persoenliche Memories (Praeferenzen/Inventar).
- **Hermes/OpenClaw:** NICHT noetig (nur optionale Zugangskanaele). REST + MCP bauen wir selbst.
- **Zugang spaeter:** Domain/Caddy+TLS ODER VPN (Frank tendiert zu VPN = Server unsichtbar). Plus
  eine schoene Ueberpruef-Oberflaeche fuer Frank (Inhalte sehen/kontrollieren).

## Fehlgeschlagene Ansaetze / Korrekturen (WICHTIG — nicht zurueckrudern)
- KEIN lokales Embedding/LLM mehr (Ollama/BGE-M3 war auf Datenhoheit optimiert — Datenhoheit ist Frank
  egal). Cloud (Gemini) ist schneller/besser + entlastet den kleinen 8GB-Server. → Ollama ENTFERNEN.
- NICHT die kostenlose Hostinger-Domain (.tech, teure Verlaengerung, an Hostinger gebunden) — eigene
  unabhaengige Domain spaeter.
- Mem0 braucht ein LLM (Doku geprueft via context7): `Memory.from_config({vector_store, llm, embedder})`.
  embedding_model_dims muss zur Modell-Dimension passen.

## Naechste Schritte (priorisiert) — Phase: Mem0 + erster Inhalt
1. Mit Frank: eigenen Gemini-API-Key besorgen (Bezahl-Projekt, Google AI Studio), sicher ablegen
   (`/opt/second-brain/.env` 600 + Backup `~/SK/second-brain/`). NIE im Chat/Repo.
2. Ollama + BGE-M3 aus compose.yaml entfernen (`ollama`-Service raus, `ollama-data` weg).
3. Mem0 aufsetzen, konfiguriert mit: vector_store=Qdrant (127.0.0.1:6333 + API-Key),
   embedder=Gemini (`gemini-embedding-001`), llm=Gemini (`gemini-3.1-flash-lite`). dims an Modell anpassen
   (beim Einrichten Live-Doku pruefen — Modellnamen/Dims aendern sich).
4. Zwei Speicher-Wege bauen (RAG-Dokumente vs. Mem0-Memories).
5. Franks 3 Direktiven (~/.claude/rules/superintelligence.md, self-observation.md, resilient-bugfixing.md)
   als ersten echten Inhalt einspeichern (Kategorie "programmieren", als Dokumente/RAG).
6. Harte LLM-Kosten-Caps setzen. Danach: externer Zugang (VPN/Domain) + Ueberpruef-Oberflaeche + Offsite-Backup.

## Offene Fragen
- Welcher konkrete Gemini-Key fuers Gehirn (neuer Bezahl-Key vs. Free `AQ.Ab8`)? Mit Frank beim Einrichten.
- Zugangsweg final: VPN vs. Domain+TLS — entscheiden, wenn der Zugang dran ist.

## Relevante Dateien
- `best-practices/second-brain/UMSETZUNGSPLAN.md` — DER Steuerungsplan (Stand, Entscheidungen, Phasen, finale KI-Wahl).
- `best-practices/second-brain/*.md` — Wissensbasis (memory-backends, orchestrator-und-suche, datenmodell, ...).
- `bugs/server/self-hosted-ai-agent-server.md` + `vps-hosting.md` + `wireguard.md` — Almanache (Sicherheit/Kosten/VPN).
- `second-brain-server/compose.yaml` + `.gitignore` + `README.md` — Deploy-Configs (im Repo + auf Server /opt/second-brain).
- Memory: `project_second_brain_memory_server` (alle Entscheidungen + Praeferenz-Korrekturen).

## Anker
- Branch: main
- Letzte Commits:
ca678c18f #47064 - Second Brain: Schluessel-Strategie (Bezahl-Tier bevorzugt, eigener Key, Caps)
275160af4 #47063 - Second Brain: FINALE KI-Wahl (gemini-3.1-flash-lite + gemini-embedding-001)
(plus #47056-#47062: Plan, Qdrant-Stack, Ollama, Architektur-Korrektur Cloud)
