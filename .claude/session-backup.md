# Session Handoff — 2026-06-25, spaeter Abend

## Ziel
Second-Brain (Cortex): Komplettes Direktive-#3-Debugging, warum lange Eintraege nur halb
gespeichert/zurueckgegeben wurden, und ROBUSTER Fix, damit SEHR grosse Dateien (Frank: 20-30x
seine ~18,7k-Loop-Datei, also bis ~560k Zeichen) zuverlaessig 1:1 gespeichert UND 1:1 abgerufen
werden. Alles erledigt, deployt und live byte-genau verifiziert.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alles committet (#47239/#47240/#47241),
gepusht, auf den VPS deployt und live getestet. Kein uncommitteter eigener Code im Working Tree
(nur hook-verwaltete agent-memory/shared/*.jsonl + MEMORY.md — NICHT meine, nicht committen).

## Aktueller Status (alles ERLEDIGT + DEPLOYT + VERIFIZIERT)
- ZUERST: Loop-Engineering-Almanach (18.731 Zeichen) per MCP `remember` VOLLSTAENDIG neu eingespeichert
  (ersetzte die alte, bei ~8000 abgeschnittene Haelfte). Per get_by_title byte-genau verifiziert.
- URSACHE 1 bewiesen (die "halbe Datei"): ein STILLER text[:8000]-Slice im dashboard /api/chat (api_chat)
  kuerzte die EINGABE, BEVOR sie brain-api erreichte. Speicherung (full_text 1:1 in JEDEM Chunk, Z.528)
  UND Abruf (by_title/by_category/by_parent/search geben full_text 1:1, Z.560/585/610/899) waren NIE
  kaputt — sie waren treu zur bereits gekuerzten Eingabe. Es war EIN Bug, nicht zwei.
  Fix: dashboard /api/chat lehnt nun LAUT ab (MAX_STORE_CHARS, Default 500000, env DASH_MAX_STORE_CHARS)
  statt still zu slicen (committet im Parallel-Session-Commit dashboard v0.23.0). agent ChatReq.text
  100000->500000 (#47239, agent v0.31.0). brain-api StoreReq.text bleibt UNGECAPPT (chunkt selbst =
  der eigentliche Speicherpfad, auch via MCP); Edit/Trash-Caps 200000->1000000 (#47239, brain-api v1.12.0).
- URSACHE 2 (vom Regressionstest gefunden!): full_text haengt 1:1 in JEDEM Chunk -> 600k-Zeichen-Doc
  (~158 Chunks) = ~102 MB in EINEM qc.upsert -> Qdrant lehnt Requests >32 MB ab (400). Genau Franks
  20-30x-Dateien waeren hart gescheitert. Fix: _upsert_batched() splittet jeden Upsert unter
  UPSERT_MAX_BYTES (Default 24 MB, env SB_UPSERT_MAX_BYTES) ueber ALLE 6 Schreibwege; funktionserhaltend
  (gleiche Punkte/Payloads, mehrere Requests), KEIN Lesepfad veraendert (0 Regressionsrisiko).
  #47240, brain-api v1.13.0.
- GARANTIE: tests/large_doc_roundtrip.py (stdlib-only) speichert ~600k Zeichen und prueft den
  byte-genauen Round-Trip (store -> by_title). LIVE auf dem VPS gelaufen: 600.040 Zeichen, 158 Chunks,
  ~4 Batches -> PASS (1:1 zurueck), Aufraeumung via /purge (isolierter eval*-Nutzer).
- Bug-cases dokumentiert (#47239 Ursache 1, #47241 Ursache 2).
- LIVE-Versionen VPS (alle 3 Container healthy): brain-api 1.13.0, agent 0.31.0, dashboard 0.23.0.

## Relevante Dateien
- second-brain-server/brain-api/app.py (v1.13.0) — _upsert_batched + UPSERT_MAX_BYTES (Z.~74 + ~257),
  alle 6 qc.upsert ersetzt; Edit/Trash-Caps 1_000_000; StoreReq.text ungecappt; full_text 1:1 je Chunk.
- second-brain-server/agent/app.py (v0.31.0) — ChatReq.text max_length 500_000 (Z.1246).
- second-brain-server/dashboard/app.py (v0.23.0) — MAX_STORE_CHARS + lauter Reject im /api/chat
  (committet von Parallel-Session, enthaelt meinen Speicher-Fix).
- second-brain-server/tests/large_doc_roundtrip.py — Regressionstest (dauerhafte Garantie).
- ACHTUNG Parallel-Session: dashboard wurde von einer anderen Session aktiv bearbeitet (VORLESEN/
  index.html, v0.23.0) — sie hat meinen dashboard-app.py-Fix in IHREN Commit eingepackt (geteilter
  Working-Tree). brain-api/agent waren frei.

## Getroffene Entscheidungen
- Klasse beseitigen statt Wand erhoehen: stille text[:N]-Kuerzung -> LAUTE Ablehnung; brain-api
  StoreReq ungecappt (chunkt selbst).
- Fuer Ursache 2 den RISIKOARMEN Batch-Upsert gewaehlt (kein Lesepfad angefasst) statt full_text-once
  (haette ~12 Lese-/Re-Embed-Stellen + limit=1-Scrolls angefasst -> apparenter Datenverlust-Risiko).
- Deploy IMMER nur committeter HEAD-Stand (git show HEAD:...) per scp auf /opt/second-brain, dann
  `docker compose up -d --build <service>`. SSH-Key ~/SK/second-brain/id_ed25519, VPS root@168.231.83.205.
- Test auf dem VPS via: docker compose cp tests/large_doc_roundtrip.py brain-api:/tmp/lt.py &&
  docker compose exec -T brain-api sh -lc "BRAIN_URL=http://localhost:8000 LARGE_DOC_CHARS=600000 python3 /tmp/lt.py"
  (Container-Recreate loescht /tmp -> Test vor jedem Lauf neu reinkopieren).

## Fehlgeschlagene Ansaetze / Stolpersteine
- Erster 600k-Testlauf schlug mit Qdrant 400 (>32MB) fehl — DAS war der echte zweite Bug (gewollt gefunden).
- Container-Recreate (compose up --build) loescht /tmp im Container -> Test-Datei muss vor jedem
  exec neu per `docker compose cp` rein.
- Langer git-commit-Befehl mit Heredoc-Message lief in 2-Min-Timeout (Index-Contention Parallel-Session)
  -> stattdessen kurze -m-Message + atomarer Pfad-Commit `git commit -m "..." -- <pfade>` nutzen.

## Naechste Schritte (priorisiert, OFFEN — Vorschlaege, nichts dringend)
1. Storage-Effizienz: full_text nur 1x in Chunk 0 statt in jedem Chunk (O(len) statt O(N*len)) —
   EIGENE Session, weil ~12 Lese-/Re-Embed-Stellen (mehrere limit=1-Scrolls) angefasst + getestet
   werden muessen. Heute bewusst NICHT gemacht (Risiko in parallel bearbeiteter Datei).
2. Regressionstest automatisch nach jedem Deploy laufen lassen (Deploy-Skript) oder ins Eval-Set.
3. MCP-Anbindung fertig: alle 3 Direktiven als Volltext-Eintraege ablegen + per Titel komplett
   abrufen (Franks eigentliches Ziel; passt locker, bis 600k bewiesen).
4. Router schaerfen: Few-shot, damit vage Wissensfragen auf query statt smalltalk gehen.
5. VPS-Disk/Qdrant-Snapshot-Groesse im Blick (grosse Dokumente vervielfachen aktuell den Speicher,
   siehe Schritt 1).

## Offene Fragen
- Keine offene Rueckfrage. Frank wollte zuletzt das Backup.

## Anker
- Branch: main
- Letzte Commits:
b1a66eab4 #47241 - docs(bug-cases): Second-Brain grosses Dokument sprengte Qdrant-32MB-Upsert
ff41d4230 #47240 - fix(second-brain brain-api): sehr grosse Dokumente speichern ohne Qdrant-32MB-Crash v1.13.0
577c91528 #47239 - fix(second-brain): sehr grosse Dokumente robust speichern+abrufen (Direktive-#3-Debugging)
fdee5eb2c #47231 - perf(second-brain dashboard): Vorlesen Satz-fuer-Satz-Pipelining v0.23.0
20fedcdfe #47238 - session restore: clear handoff backup
