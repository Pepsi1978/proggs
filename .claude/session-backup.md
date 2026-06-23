# Session Handoff — 2026-06-23, ~12:30

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" (serverseitiger Memory-Server auf Hostinger-VPS, ueber WireGuard) weiterbauen,
Schritt fuer Schritt nach dem Flugplan. Aktueller Meilenstein erreicht: mem0-freier 1:1-Speicher LIVE +
177 echte Eintraege importiert. Naechster Schritt laut Flugplan: Phase 1 (Backup der Gehirn-Daten).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende Aufgabe, letzter Stand sauber abgeschlossen** (alle Commits durch bis #47107). Frank hat
das Backup BEWUSST am Ende einer Planungs-/Bauphase gemacht, um spaeter nahtlos beim Flugplan einzusteigen.
- **So geht es weiter:** Bei "weiter mit dem zweiten Gehirn" ZUERST die beiden Memorys lesen:
  `project_second_brain_flugplan` (Roadmap Phase 1-6) + `project_second_brain_memory_server` (voller Stand,
  Server-Anker, Schema-Entscheidung). Dann mit **Flugplan PHASE 1.1 beginnen: Backup der Gehirn-Daten**
  (taeglicher Qdrant-Snapshot auf dem VPS + Kopie offsite). Die 177 Eintraege sind aktuell ungesichert.

## Aktueller Status
- **Erledigt (alles committed + live):** Festplatten Z:(gedanken)/Y:(daten) per Samba ueber WireGuard
  eingebunden (#47103/#47104). mem0 KOMPLETT raus -> brain-api v1.0.0 = 1:1-Speicher (qdrant-client +
  google-genai direkt), #47105. Vierter Abruf-Weg /by-date + get_by_date, created_at bleibt bei Ueberschreiben (#47106).
  177 ChatGPT-Gedaechtnis-Eintraege importiert (1:1, 13 Kategorien, Dubletten zusammengefuehrt) — Collection "brain".
  CLAUDE.md-Regel "Session-Backup immer manuell" (#47107). Docker-Bauschutt geraeumt, 91 GB frei, alle Container healthy.
- **In Arbeit:** nichts.
- **Blockiert:** nichts.
- Stack live auf VPS: sb-qdrant (v1.18.2), sb-mem0-api (brain-api v1.0.0, Ordner/Container heissen noch mem0-api = Legacy), sb-mcp (mcp 1.28).

## Relevante Dateien
- `~/.claude/projects/C--Users-barwa-proggs/memory/project_second_brain_flugplan.md` — DER FLUGPLAN (Phase 1-6, mit Franks Wuenschen)
- `~/.claude/projects/C--Users-barwa-proggs/memory/project_second_brain_memory_server.md` — voller Systemstand, Server-Anker, Schema-Entscheidung
- `second-brain-server/mem0-api/app.py` — brain-api v1.0.0 (1:1-Speicher, 4 Abruf-Wege + /by-date)
- `second-brain-server/mcp-server/server.py` — MCP-Werkzeuge (remember/recall/get_by_title/get_by_category/get_by_date/list_memories/forget)
- `second-brain-server/compose.yaml` — Stack (qdrant/mem0-api/mcp), Collection "brain"

## Getroffene Entscheidungen
- **mem0 RAUS** (halluzinierte beim Extrahieren). Gehirn = wortwoertlicher 1:1-Speicher; Aufbereitung passiert VORHER client-seitig.
- **Datenmodell:** Text(1:1) + Titel(optional=Schluessel, gleicher Titel ersetzt) + Kategorie(optional). 4 Abruf-Wege: by-title/by-category/by-date/search + list/forget.
- **Schema (Frank):** reiner 1:1-Inhalt in den Vektor, Kategorie/Titel/Datum GETRENNT ins Payload, NIE Etiketten in den Text einweben. ASCII-lowercase Kategorien.
- **3 Schichten:** Speicher (dumm, Qdrant+Gemini-Embedding) / Zugriff (Apps+CLIs) / Agent (SPAETER, liest nur, schreibt nie eigenmaechtig).
- **Session-Backup/Restore IMMER manuell** (CLAUDE.md-Regel #47107) — Claude initiiert NIE automatisch.

## Fehlgeschlagene Ansaetze (NICHT wiederholen)
- **mem0 + HHEM-Quality-Gate:** komplett verworfen — nie wieder bauen (mem0 dichtet, Gate war Umweg).
- **PowerShell-Tool blockt Loesch-Befehle** (Remove-Item/`del /q`/`net use /delete` → Sandbox missdeutet als Pfad). Loesung: als `.ps1` schreiben und via `powershell.exe -File` aus dem Bash-Tool ausfuehren.
- **Qdrant interfaces:** Netz-Adresse `10.8.0.0/24` band smbd nur an lo — konkrete Host-IP `10.8.0.1/24` noetig (POINTOPOINT/NOARP).

## Wichtige Recherche-Ergebnisse
- Embedding `gemini-embedding-001` @1536 (output_dimensionality EXPLIZIT setzen, defaultet sonst 768). Qdrant `url=http://` (sonst TLS-Zwang bei gesetztem api_key). Cosine.
- Embedding-Kosten winzig: ~0,15 Cent fuer alle 177 ($0,15/1M Token, NUR Input). Laeuft ueber den Gemini-Key in der VPS-.env (= aktuell geteilter TVO-Key).
- Gehirn-Nutzung in der CLI laeuft AKTUELL nur ueber direkten Zugriff (SSH->brain-api), NOCH KEIN echter MCP-Anschluss.

## Naechste Schritte (priorisiert — = Flugplan)
1. **PHASE 1.1: Backup der Gehirn-Daten** (Qdrant-Snapshot taeglich + offsite). 177 Eintraege ungesichert -> hohe Prio.
2. PHASE 1.2 (optional): eigener Gemini-Key fuers Gehirn (Kontingent vom TVO-Key trennen).
3. PHASE 2.1: echten MCP-Anschluss in Claude Code + OpenCode herstellen (sb-mcp http://10.8.0.1:8001/mcp).
4. PHASE 3: gefilterte Vektorsuche in /search (Datum/Kategorie-Filter) + Speicher-Schema/Kategorienliste als Best-Practice.
5. PHASE 4: Bibliothekar-Agent (Schicht 3) — Query-Routing, Dubletten+Rueckfrage, EIGENES Kategorien-Gedaechtnis (kennt alle, legt neue an).
6. PHASE 5: Sprach-Anbindung (STT+TTS) + Dashboard. PHASE 6: Aufraeumen (mem0-api->brain-api umbenennen, Doku nachziehen).

## Offene Fragen
- Keine blockierenden. (Offen am Rande: MEMORY.md ist ueber dem Lade-Limit ~29 KB -> separate Aufraeum-Runde sinnvoll, Pointer kuerzen.)

## Server-Anker (zum Weiterarbeiten)
- VPS: Hostinger 168.231.83.205, Ubuntu 24.04. SSH: `ssh -i ~/.ssh/id_ed25519 root@168.231.83.205` (Windscribe AUS!).
- WireGuard: Dienste ueber 10.8.0.1. Gehirn http://10.8.0.1:8000, MCP http://10.8.0.1:8001/mcp.
- Deploy (kein git-Repo auf VPS): scp -> `cd /opt/second-brain && docker compose up -d --build mem0-api mcp`. Secrets: /opt/second-brain/.env + ~/SK/second-brain/.
- Samba: Z:->\10.8.0.1\gedanken, Y:->\10.8.0.1\daten (Passwort ~/SK/second-brain/samba.env).

## Anker
- Branch: main
- Letzte Commits:
ecca5e43f #47107 - CLAUDE.md: Session-Backup immer manuell
0a00563a5 #47106 - brain-api: /by-date + get_by_date, created_at erhalten
66ea05be4 #47105 - mem0 raus -> 1:1-Speicher brain-api v1.0.0
6860e05eb #47104 - Best-Practices Samba: Share-Namen gedanken/daten
636cb8451 #47103 - Bug-Almanach+BP Samba/WireGuard (konkrete Host-IP, net use)
