# Session Handoff — 2026-06-22, ~21:00

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" (serverseitiger Memory-Server, NICHT lokal) nutzbar machen UND das Wissen
darum dauerhaft sichern. Heute erreicht: MCP-Anbindung beider CLIs, Bug-Almanache + Best-Practices
recherchiert (Samba/Qdrant/mem0/FastMCP), Gehirn gegen Muell abgesichert. Als Naechstes: BP-Skill
vollstaendig, proaktive Memory-Nutzung (Bereich 5), Festplatte (Task 3).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alle Commits durch (zuletzt #47085 von
mir). Frank macht jetzt /clear + "session restore" und danach die offenen Punkte (siehe Naechste
Schritte). Nichts von mir uncommittet; lokal == origin/main.

## Aktueller Status
- Erledigt:
  - MCP-Server (Container sb-mcp) LIVE an 10.8.0.1:8001/mcp (FastMCP Streamable-HTTP, mcp==1.12.4).
    Claude Code (claude mcp add --transport http --scope user) UND OpenCode (~/.config/opencode/
    opencode.jsonc mcp-Block) beide "connected" verifiziert. Werkzeuge: remember/recall/list_memories/
    brain_health. Commits #47077 (+ Fix #47078: KEIN from-future-import-annotations).
  - Recherche-Protokoll als Regel GEHAERTET (#47080, #47082; research-strategy.md + Spiegelung +
    Memory feedback_research_always_via_protocol): JEDE Recherche IMMER (1) Empfehlung (2) 4-Fragen-
    AskUserQuestion A/B/C/D (3) research-Skill; UND Frage 2 (Eskalation) IMMER nach Firecrawl.
  - Bug-Almanache NEU: bugs/server/{samba-wireguard,qdrant,mem0}.md + bugs/claude-tooling/mcp-server.md
    erweitert (3.11/3.12/3.13). Best-Practices NEU: best-practices/server/{samba-wireguard,qdrant,mem0}.md.
    Ins System eingehaengt (README, guard-coverage Querschnitt, health.py gruen ausser firecrawl-Drift
    [fremd]). Commits #47081, #47083.
  - Eskalation (OpenRouter :online, Runde1=7 + Runde2=6 Researcher) vertiefte alle Bereiche, Fokus mem0-Junk.
  - mem0 GEGEN MUELL ABGESICHERT + live verifiziert: strenge custom_instructions in app.py
    (DEFAULT_CUSTOM_INSTRUCTIONS, per Env SB_CUSTOM_INSTRUCTIONS ueberschreibbar), VERSION 0.2.0 (#47084).
    fastembed nachgeruestet -> hybride Suche aktiv, VERSION 0.2.1 (#47085). Bug mem0#4999 geprueft ->
    auf 2.0.7 NICHT aktiv (Scores variieren 0.76/0.69). Test-Stores gemacht + wieder geloescht; RAM ok
    (mem0-api 151MB/1GB, qdrant 36MB/2GB).
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- second-brain-server/mem0-api/app.py — custom_instructions (DEFAULT_CUSTOM_INSTRUCTIONS oben + Key in MEM0_CONFIG), VERSION 0.2.1
- second-brain-server/mem0-api/requirements.txt — fastembed>=0.4 ergaenzt
- second-brain-server/mcp-server/{server.py,Dockerfile,requirements.txt} — der MCP-Server (mcp==1.12.4)
- second-brain-server/compose.yaml — Service sb-mcp an 10.8.0.1:8001
- bugs/server/{samba-wireguard,qdrant,mem0}.md + best-practices/server/{...}.md — neue Almanache+BP
- best-practices/second-brain/UMSETZUNGSPLAN.md — Roadmap/Wunschliste

## Getroffene Entscheidungen
- mem0-Junk-Absicherung ueber STRENGE custom_instructions (mem0 hat KEIN Quality-Gate; Audit #4573 = 97,8% Junk).
- MCP an WireGuard-IP; host=0.0.0.0 im Container deaktiviert DNS-Rebinding-Schutz (saubere Alt: TransportSecuritySettings/allowed_hosts, dokumentiert mcp-server.md 3.13 — spaeter umstellbar).
- Engine A (Firecrawl) fuer Bug-Recherche, B (OpenRouter :online) fuer Eskalation. Samba/mem0-Rohdaten behalten statt neu.

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- FastMCP Python: KEIN "from __future__ import annotations" (issubclass-TypeError beim Tool-Registrieren, Issue #1129/#1336). In server.py bereits raus.
- mem0 2.0.7: search/get_all brauchen filters={"user_id":...} + top_k (NICHT top-level user_id=/limit=).
- Qdrant: gesetzter api_key erzwingt https=True -> WRONG_VERSION_NUMBER; explizite http://-URL nutzen (QDRANT_URL=http://sb-qdrant:6333).
- rsync FEHLT lokal (Windows Git Bash) -> fuer Server-Deploy scp nutzen.
- 2 PROTOKOLL-VERSTOESSE heute bei Recherche: (1) Frage1+research-Skill uebersprungen, (2) Frage2/Eskalation vergessen. BEIDE korrigiert + Regel gehaertet. NIE wieder: jede Recherche IMMER volles Protokoll inkl. Frage 2.

## Wichtige Recherche-Ergebnisse (mem0-Junk = Franks Hauptanliegen)
- mem0 Audit #4573: 97,8% Junk, davon 52,7% Boot-File-Restating (System-Prompt wird gespeichert). "Extraktions-Prompt ist Flaschenhals, NICHT das Modell."
- Gegenmittel (jetzt teils umgesetzt): custom_instructions STRENG (hoechste Prioritaet) [DONE], includes/excludes, custom_categories (2-3), Confidence-Gate <0.7, threshold (0.1), Feedback-Loop-Disziplin (recalled NIE zurueck in add(infer=True)), Self-Contained (Pronomen->Namen, 15-80 Woerter).
- mem0 hat KEINE REJECT-Action, kein hartes Quality-Gate -> Sauberkeit kommt aus Prompt + Disziplin + Audit.
- Proaktive Memory (Bereich 5): offizielle Quelle mem0.ai/blog/proactive-memory-in-ai-agents (3 Patterns) bereits gefunden.

## Naechste Schritte (priorisiert)
1. (Frank-Wunsch) Best-Practices-Skill VOLLSTAENDIG fuer die neuen Server-Almanache durchlaufen (Changelog-Abgleich; Gegenstuecke best-practices/server/* stehen schon als Kuratierung, der volle Skill-Lauf fehlt). -> Skill "best-practices".
2. Bereich 5: proaktive Gedaechtnis-Nutzung recherchieren als EIGENER Lauf mit VOLLEM Protokoll (Empfehlung + 4 Fragen + research-Skill + Frage 2!). Ziel best-practices/second-brain/proaktive-memory-nutzung.md. Beantwortet Franks Kernfrage "wie wird Memory automatisch genutzt" (Stufe 1 = Instruktion in CLAUDE.md/AGENTS.md; Stufe 2 = "Dirigent"-Agent).
3. Festplatte (Task 3): Samba auf dem Server aufsetzen (Almanach bugs/server/samba-wireguard.md FERTIG: interfaces=lo eth0 10.8.0.0/24, smb encrypt=required, ufw in on wg0 445, protocol=SMB3) + als Netzlaufwerk im Windows-Explorer (\\10.8.0.1\share). WireGuard-Tunnel muss AN sein.
4. (optional) Qdrant-Image auf 1.18.2 pinnen statt :latest; MCP allowed_hosts statt 0.0.0.0.

## Offene Fragen
- Bereich 5: Sprach-App eigene App ODER an TVO/VoiceAgent andocken? STT=Groq lokal (1:1 vom TVO), TTS=Gemini lokal pro Geraet (Frank-Entscheidung: Server gibt NUR Text aus). Dashboard read-only zuerst?

## Anker
- Branch: main
- Letzte Commits:
  cada77702 #47084 - Voice Overlays Etappe 2d (Windows) [FREMDE Session]
  7473c9a4d #47085 - Second Brain mem0: fastembed -> hybride Suche, VERSION 0.2.1
  eec6c0b02 #47084 - Second Brain mem0: strenge custom_instructions, VERSION 0.2.0
  74c499317 #47083 - Voice Overlays Etappe 2d (Mac) [FREMDE Session]
  cd6737a7a #47083 - Best-Practices Second-Brain-Stack + Eskalation
  (Hinweis: doppelte #-Nummern, weil parallele Voice-Overlays-Session gleichzeitig pusht — normal.)

## Technische Anker (Server-Stack)
- Server: Hostinger 168.231.83.205, Ubuntu 24.04. SSH: ssh -i ~/.ssh/id_ed25519 root@168.231.83.205 (Windscribe stoert NICHT). WireGuard-Tunnel: WireGuardTunnel$pc.
- Container: sb-qdrant (1.18.2), sb-mem0-api (v0.2.1, mem0ai 2.0.7, Python 3.12), sb-mcp (FastMCP).
- Endpunkte ueber WireGuard 10.8.0.1: Gehirn http://10.8.0.1:8000 (/health /store /recall /memories, Bearer SB_API_KEY), MCP http://10.8.0.1:8001/mcp.
- Secrets: ~/SK/second-brain/brain.env (SB_API_KEY), /opt/second-brain/.env (QDRANT_API_KEY, GEMINI_API_KEY). NIE im Repo.
- Deploy: scp <datei> root@168.231.83.205:/opt/second-brain/mem0-api/ ; dann ssh ... cd /opt/second-brain && docker compose up -d --build mem0-api.
