# Session Handoff — 2026-07-04, 11:58 Uhr

## Ziel (1-3 Saetze)
Franks Second Brain (Cortex: brain-api/Qdrant + sb-agent + Dashboard + CortexAndroid) auf ein
neues Level heben — wie ein "Unterbewusstsein": immer praesent, Kurzzeit- UND Langzeitgedaechtnis,
lernt in jeder Programmiersession mit. Diese Session hat dafuer recherchiert und einen Plan mit
50 Feature-Vorschlaegen geliefert (Wissensfrage/Inspiration — noch KEINE Umsetzung beauftragt).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Frank hat die 50 Vorschlaege als
Inspiration bekommen und wollte danach nur dieses Backup. Naechster natuerlicher Schritt: Frank
nennt Nummern/Favoriten aus den Plaenen, dann Umsetzung planen (Phasen unten).

## Aktueller Status
- Erledigt: Recherche-Protokoll eingehalten (Empfehlung -> Frage 1 -> Engine A Firecrawl+MiniMax,
  7 Themen + 2 Retries, ~44 Quellen; Eskalations-Frage 2 gestellt, Frank: "Nein, fertig").
- Erledigt: Recherche persistiert: best-practices/second-brain/memory-evolution-2026.md +
  -kurzcheck.md (#47464). Kein Bug-Almanach-Update noetig (keine Bug-Kandidaten).
- Erledigt: Plan mit 50 Vorschlaegen: second-brain-server/LEVEL2-FEATURES-PLAN.md (#47464),
  Querverweis auf Cowork-Schwester-Plan (#47465). Alles gepusht.
- WICHTIG: Eine PARALLELE Cowork-Session hat am selben Tag denselben Auftrag bearbeitet und
  best-practices/second-brain/second-brain-2.0-unterbewusstsein-50-features.md gepusht (#47463).
  Beide Plaene sind gegenseitig verlinkt und als EIN Ideen-Pool zu nutzen.
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- `second-brain-server/LEVEL2-FEATURES-PLAN.md` — der 50-Punkte-Plan dieser Session + Phasen-Reihenfolge
- `best-practices/second-brain/second-brain-2.0-unterbewusstsein-50-features.md` — Schwester-Plan der Cowork-Session (Bonus-Ideen: ntfy-Push, Spaced-Repetition, Heat-Score, ACE-Playbook, Agent-Inbox)
- `best-practices/second-brain/memory-evolution-2026.md` (+ `-kurzcheck.md`) — persistiertes Recherche-Wissen (VOR Umsetzungs-Arbeit am Second Brain lesen!)
- `~/.research-swarm/sb-level2/` — Rohantworten der 7+2 Researcher (lossless, nur bei Bedarf)

## Getroffene Entscheidungen
- Engine A (Firecrawl+MiniMax) fuer die Recherche, keine Eskalation (Frank-Klick) — Cowork-Lauf validierte unabhaengig.
- Kern-Architektur-Empfehlung: Kern-Bloecke (Kurzzeit) + Sleep-Time-Bibliothekar (Konsolidierung) + gedrosselte Proaktivitaet (Notify/Question/Review). KEIN Voll-GraphRAG (belegt Overkill; mem0 hat Graph-Layer entfernt) — leichtes Entity-Linking reicht.
- Empfohlene Umsetz-Reihenfolge: (1) Kern-Bloecke+Injektion [Plan-Nr. 2,3,21] -> (2) Nacht-Bibliothekar [11,13,16,18] -> (3) Session-Mitlernen [27,28,30] -> (4) Proaktivitaet [19,23,24] -> (5) Hybrid-Suche [34,35,38] -> (6) Frontends [42,43,40].

## Fehlgeschlagene Ansaetze
- Researcher 2 (Sleep-time) stuerzte mit Firecrawl 502 ab — Retry-Lauf war erfolgreich; kein offenes Problem.
- `git commit -- <pfade>` scheitert bei NEUEN (untracked) Dateien ("pathspec did not match") — neue Dateien brauchen erst `git add`.
- Achtung research-swarm.py: arbeitet IMMER in ~/.research-swarm (egal welches CWD) und loescht dort beim Start answer-*/log-*/run-*/done.flag — fertige Antworten VOR einem Folgelauf wegkopieren (diesmal nach sb-level2/ gerettet). answer-Dateien werden beim Start LEER angelegt (Existenz != fertig).

## Wichtige Recherche-Ergebnisse
- Letta/MemGPT: Memory-Blocks im Kontext + Sleep-Time-Agent (staerkeres Modell, asynchron) = Blaupause fuers Unterbewusstsein.
- Decay-Modell: Abruf verstaerkt (+Strength, Zeit-Reset), Score faellt NIE auf null (MemoryBank/arXiv 2404.00573).
- Proaktivitaets-Drossel (Temporal Constraints) belegt +38,9 % Zufriedenheit (ProAgent).
- Hybrid-Suche (BM25+dense, RRF) = der eine lohnende Retrieval-Upgrade fuer hunderte Eintraege (Qdrant nativ).
- Session-Mitlernen: Rueckfluss-Loop ist die Luecke ("domain knowledge discovered multiple times, never persisted"); Vorbilder: claude-engram (Schema-Canary!), claude_progress-Muster.
- Vertrauen braucht Provenance + Confidence (+30 % Zeitersparnis NUR damit); Wissens-Gesundheits-Dashboards = Marktluecke.

## Naechste Schritte (priorisiert)
1. Frank fragen/abwarten, welche Feature-Nummern ihn anspringen (aus BEIDEN Plaenen), dann Umsetzungs-Plan fuer die gewaehlten bauen.
2. Falls Frank "fang einfach an" sagt: Phase 1 = Kern-Bloecke + Selbst-Edit + Per-Turn-Injektion im sb-agent (Plan-Nr. 2, 3, 21); vorher memory-evolution-2026-kurzcheck.md lesen.
3. Offene Intelligenz-Vorschlaege aus der Session (Frank hat noch nicht geantwortet): (a) session-presence-Hinweis um letzten User-Prompt der Parallel-Session erweitern (Doppelarbeit frueher erkennen), (b) research-swarm.py atomar schreiben lassen (tmp->rename).

## Offene Fragen
- Welche der 50(+7 Bonus)-Features will Frank zuerst? (Empfehlung: Phase 1 — Kern-Bloecke.)
- Sollen die beiden 50er-Listen zu EINEM Master-Backlog verschmolzen werden (Vorschlag Nr. 5 der Abschluss-Box)?

## Anker
- Branch: main
- Letzte Commits:
2fb2f72fe #47465 - Second Brain Level-2 plan: cross-reference to parallel Cowork plan (both 50-feature lists as combined idea pool)
68834d00b #47464 - Second Brain Level-2: research persisted (memory-evolution-2026 best practices + kurzcheck) + 50-feature plan (LEVEL2-FEATURES-PLAN.md)
9d54236ec #47463 - Second Brain 2.0: Unterbewusstsein-Plan mit 50 Feature-Vorschlaegen (Researcher-Schwarm: Letta/MemGPT, Mem0/Zep/Graphiti, Konsolidierung, Qdrant, Coding-Session-Memory, proaktive Agenten, PKM-Produkte)
7a84451d3 #47462 - Bug-cases: 4 EntropieReductor-Faelle (HC-Trainingsimport aus Roh-Samples, HC-Route consentRequired, Google-Maps Key-Projekt-Mismatch, geloeschte-Trainings-Tombstone)
610a4c2c7 #47461 - EntropieReductor: geloeschte Trainings bleiben geloescht - Tombstone-Mechanismus (startMs) verhindert Re-Import via Health-Connect-Sync UND Drive-Backup-Restore (beide Pfade); version 0.18.4
