# Session Handoff — 2026-06-22, ~16:10

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" bauen — serverseitiger Memory-Server fuer ALLES (nicht nur Programmieren),
von ueberall erreichbar, mit Sprach-Bedienung + Oberflaeche als Ziel. Fundament + Backend + sicherer
Zugang sind FERTIG; als Naechstes folgen Anbindung (MCP, Sprache, Dashboard, Dirigent) + Inhalte.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Frank ist laufen gegangen. Er will beim
Zurueckkommen GEMEINSAM brainstormen/recherchieren, wie die naechsten Anbindungen aussehen
(v.a. Sprach-App STT+TTS + Dashboard), dann Reihenfolge festlegen und bauen.
Wiedereinstieg-Trigger: "wir bauen am zweiten Gehirn weiter".

## Aktueller Status — ALLES LIVE & getestet (Commits #47069-#47074)
- Server: Hostinger KVM2, 168.231.83.205, Ubuntu 24.04, gehaertet (UFW: nur 22/tcp + 51820/udp), SSH key-only.
- Qdrant (Container sb-qdrant) + Mem0+Gemini REST-Wrapper (Container sb-mem0-api). LLM gemini-3.1-flash-lite,
  Embeddings gemini-embedding-001 @1536. Qdrant-Collection second_brain (Vektorgroesse 1536). Container healthy.
- WireGuard: wg0 = 10.8.0.1; Handy 10.8.0.2 + PC 10.8.0.3 verbunden+getestet. mem0-api an 10.8.0.1:8000
  (oeffentlich UNSICHTBAR, getestet). Gehirn-Endpunkte: http://10.8.0.1:8000 (/health /store /recall /memories).
- Auth: Bearer SB_API_KEY. Secrets in /opt/second-brain/.env (chmod 600) + Backup ~/SK/second-brain/
  (inkl. wireguard/ mit phone.conf, pc.conf, phone-qr.png, server-wg0.conf). NIE im Repo.
- Repo-Code unter second-brain-server/ (compose.yaml, mem0-api/{app.py,Dockerfile,requirements.txt}, README).
- Nichts uncommittet von mir. Lokal == origin/main.

## Naechste Schritte — Details in best-practices/second-brain/UMSETZUNGSPLAN.md ("Roadmap & Franks Wunschliste")
Franks Wuensche (Prioritaet seine Worte): 1) Sprach-Anbindung STT+TTS (sich natuerlich MIT dem Server
unterhalten) (*); 2) Dashboard/Oberflaeche zum Reinschauen (*); 3) Dirigent (lokaler Router-Agent);
4) MCP-Anbindung (CLIs nutzen Gehirn automatisch); 5) Inhalte einspeichern (3 Direktiven, RAG); 6) Backup.
Claude-Reihenfolge-Vorschlag (mit Frank abstimmen): MCP zuerst (Alltagsnutzen) -> Sprache -> Dashboard ->
Dirigent -> Inhalte -> Backup. Kostenbremse: laut Frank ueber Gemini-Key gesetzt -> nur verifizieren.

## Getroffene Entscheidungen
- Mem0 + Qdrant + Gemini (Cloud-KI; Datenabfluss ist Frank egal). NICHT supermemory/lokal. Ollama entfernt.
- Gemini-Key AKTUELL = derselbe wie TVO-Key (geteilt). Eigener Bezahl-Key optional spaeter.
- WireGuard Split-Tunnel (nur 10.8.0.0/24) -> darf dauerhaft anbleiben, stoert TVO/Groq/Internet NICHT.

## Fehlgeschlagene Ansaetze / Fallen (NICHT wiederholen)
- qdrant-client mit gesetztem api_key -> https=True -> [SSL: WRONG_VERSION_NUMBER]. FIX: url="http://host:6333" statt host/port.
- Mem0 2.x: search()/get_all() brauchen filters={"user_id":...} + top_k (NICHT user_id=/limit=). add() nimmt weiter user_id=.
- Mem0 Gemini-Embedder default 768 dims -> explizit embedding_dims=1536 UND qdrant embedding_model_dims=1536 (gleicher Wert!).
- Bind-Mount + nicht-root-Container -> Host-Logverzeichnis chown auf uid 10001.
- WINDSCRIBE (Full-Tunnel) blockiert SSH zum Server UND Groq/TVO -> vor Server-Arbeit AUS lassen (oder 168.231.83.205+Groq ausschliessen).
- bug-almanac-guard verlangt python-windows-Kurzcheck auch fuer Linux-Container-.py (Fehlalarm; einmal lesen pro Session genuegt).

## Offene Fragen (fuer den Brainstorm)
- Sprach-Anbindung: eigene App (Auto/Handy) ODER an TVO/VoiceAgent andocken? STT/TTS lokal vs. Cloud? Wake-Word?
- Dashboard: erst read-only Inspektion, Editieren spaeter? MCP: eigener Adapter vs. fertiger mem0-MCP?

## Anker
- Branch: main
- Letzte Commits:
e4ee491c5 #47074 - Second Brain: Roadmap + Franks Wunschliste (Sprach-Anbindung STT/TTS, Dashboard, Dirigent, MCP, Inhalte, Backup) + Design-Skizze fuer Brainstorm; Kostenbremse laut Frank ueber Gemini-Key
7e4c7b200 #47073 - Second Brain: WireGuard-Zugang — mem0-api an VPN-IP 10.8.0.1 gebunden (oeffentlich unsichtbar, kein 0.0.0.0/Docker-UFW-Falle), README WireGuard-Abschnitt, Plan-Status externer Zugang LIVE
ba4456dae #47072 - Second Brain: Mem0+Gemini LIVE im Plan dokumentiert + 4 Integrations-Fallen (Qdrant-TLS, Mem0-2.x-API, Embed-Dim, Bind-Mount-Perms) in memory-backends
0d197c591 #47071 - Second Brain fix: mem0 2.x API — search/get_all nutzen filters={user_id} + top_k statt user_id=/limit=
cf570fbdb #47070 - Second Brain fix: mem0-api spricht Qdrant ueber explizite http-URL an (api_key erzwang sonst https=True -> SSL WRONG_VERSION_NUMBER)
c21fa379c #47069 - Second Brain: mem0-api REST-Wrapper (Gemini gemini-3.1-flash-lite + gemini-embedding-001 @1536 + Qdrant), Ollama aus compose entfernt, Observability (JSON-Log/Sonden/Checkpoints), README
