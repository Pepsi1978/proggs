# Session Handoff — 2026-06-24, ~13:50

## Ziel
Franks "zweites Gehirn" (serverseitiger 1:1-Memory-Server auf Hostinger-VPS, ueber WireGuard)
Schritt fuer Schritt weiterbauen. Vollstaendiger Systemstand + Flugplan im Memory
[[project_second_brain_flugplan]] und [[project_second_brain_memory_server]] — DIESE ZUERST LESEN.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende Aufgabe — alle 4 Aufgaben dieser Session sauber abgeschlossen + committed + gepusht
+ deployed + verifiziert.** Frank hat Schluss gemacht (Token aus). Alles Offene steht im Memory
[[project_second_brain_flugplan]] (heute ausfuehrlich ergaenzt, Block "STAND 2026-06-24 Mittag").

## Aktueller Status — HEUTE ERLEDIGT (alles live auf dem VPS, alles committed+gepusht)
- **Phase 4b (recall) LIVE** — sb-agent v0.3.0 (#47131): vierter Modus 'recall'. Wissensfrage ->
  read-only Vektorsuche (brain-api /search) -> ZWEITER LLM-Aufruf llm_answer, antwortet NUR aus den
  Treffern (nichts erfinden). EIN Eingang /chat, intern zwei Koepfe, EIN editierbarer Prompt (ohne
  Schema beim Antworten). 3 Faelle live getestet. agent/app.py.
- **Dashboard-Chat (Cortex v0.3.0) LIVE** (#47135): Tab "Gespraech" (Sidebar+Mobile), Bubbles im
  kosmischen Theme. dashboard/app.py: /api/chat proxied an Agent via asyncio.to_thread (kein
  Event-Loop-Block, fastapi §1); PUT-Handler auch nicht-blockierend. Browser-verifiziert.
- **Gehirn-Backup (Phase 1.1) LIVE** (#47144): scripts/brain-backup.sh — taeglicher Qdrant-Snapshot
  -> /srv/samba/gedanken/qdrant-snapshot (Z:), Rotation 14, Cron 0 4 * * *. Server-TZ auf Europe/Berlin
  gesetzt (war UTC). Erster Snapshot liegt schon auf Z:. ~3,2 MB/Snapshot.
- **Laufwerks-Bug Z:/Y: gefixt** (#47132/#47134): wg-drive-reconnect.ps1 testete Port 8000 (brain-api)
  statt SMB-445 -> bei brain-api-Neustart blockiert. Fix: Gate auf 445 + Mapping-Fehler ins Log.
  Almanach bugs/server/samba-wireguard.md §5 + bug-case ergaenzt.
- Eintraege im Gehirn unveraendert: 177 (alle Tests read-only).

## Relevante Dateien
- `second-brain-server/agent/app.py` — Bibliothekar-Agent (store + recall). v0.3.0.
- `second-brain-server/dashboard/app.py` + `static/index.html` — Cortex-Dashboard mit Chat-Tab. v0.3.0.
- `second-brain-server/scripts/brain-backup.sh` — taegliches Qdrant-Backup (Cron auf VPS).
- `second-brain-server/compose.yaml` — Stack (qdrant/brain-api/mcp/agent/dashboard).
- Memory `project_second_brain_flugplan` — Flugplan + Block "STAND 2026-06-24 Mittag" mit ALLEN offenen Punkten.

## Getroffene Entscheidungen
- recall: EIN Gespraechs-Eingang, intern zwei Koepfe, EIN editierbarer Prompt (Frank-Wunsch: ein Fenster).
- Backup: 14 Snapshots, Cron 4 Uhr; Server-TZ Berlin (statt CRON_TZ, das Ubuntu-cron nicht zuverlaessig kann).
- Z: liegt PHYSISCH auf dem VPS -> Snapshot dorthin schuetzt vor Datenfehler, NICHT vor VPS-Totalverlust.

## Fehlgeschlagene Ansaetze / Fallen (NICHT wiederholen)
- `git commit -- <pfad>` erfasst NEUE (untracked) Dateien NICHT -> erst `git add`, dann commit (#47144).
- Qdrant-Snapshot landet im Container unter /qdrant/snapshots (NICHT gemountet) -> per API downloaden, nicht im Volume suchen.
- Browser-Mikrofon (Web Speech API/getUserMedia) braucht secure context -> ueber http://10.8.0.1 BLOCKIERT (HTTPS noetig).
- bug-almanac-guard flackerte beim Edit (state-race) -> Notaus-Flag bug-almanac-disable.flag im TEMP (danach wieder entfernt).
- async def + synchroner httpx blockiert den GANZEN Event-Loop (fastapi §1) -> asyncio.to_thread.

## Naechste Schritte (priorisiert) — OFFEN, Frank muss waehlen
1. **Sprach-Anbindung (Phase 5.1)** — Weg noch offen: (a) Web/Handy-App mit HTTPS+Mikro (HTTPS noetig wg.
   secure-context-Huerde), (b) ueber Voice-Overlays TVO/CVO (Desktop, haben Whisper-STT), (c) Server-seitiges
   STT+TTS. Frank entscheiden lassen.
2. **Server-Disaster-Recovery / echtes Offsite (NEU)** — kompletter Server-Backup auf Franks PC: Snapshots +
   Config (compose.yaml, .env, WireGuard-Config, smb.conf, agent-data), damit der ganze Server bei Totalverlust
   neu aufsetzbar ist. Eigenes groesseres Vorhaben.
3. Klein: bei recall die doppelte Suche sparen (Dedup-Vorsuche ueberspringen, wenn action=recall).
4. Weitere offene (aus Flugplan): geschuetzten Prompt-Teil read-only im Dashboard anzeigen; Logbuch-Viewer; Favicon-404.

## Offene Fragen
- Sprach-Weg (a/b/c)? — siehe Schritt 1.
- Server-Disaster-Recovery: wann/wie (Schritt 2)?

## Anker
- Branch: main
- Letzte Commits:
0bb00284e #47147 - best-practices/server/remote-mcp.md (parallele Session)
b58f1fc66 #47144 - feat(second-brain): taegliches Gehirn-Backup (Qdrant-Snapshot) [DIESE SESSION]
228448e9b #47135 - feat(dashboard v0.3.0): Gespraech-Tab (Chat) [DIESE SESSION]
1fae94787 #47131 - sb-agent v0.3.0: Phase 4b Abruf-Seite (recall) [DIESE SESSION]
(parallele Sessions: #47143/#47145/#47146/#47147 almanach/nav/best-practices)
