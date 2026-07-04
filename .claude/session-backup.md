# Session Handoff — 2026-07-04, 23:20 Uhr

## Ziel (1-3 Saetze)
Cortex/Second Brain Level-2-Ausbau. Diese Session hat die komplette Such-Intelligenz (Plan-Nr.
34-39) + Info-Bereich gebaut, getestet (Eval 114/114) und deployt. NAECHSTE Aufgabe (Franks
Ansage): den NACHTSCHICHT-BIBLIOTHEKAR (Plan B, Nr. 11-18) in den Server implementieren.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Franks expliziter Fahrplan fuer die
neue Session: (1) ZUERST Frank ausfuehrlich erklaeren, was der Nachtschicht-Bibliothekar ist —
alle 8 Punkte (11-18) einzeln, in leichtem Deutsch, er kennt den Bereich noch nicht. (2) DANN
gemeinsam die Implementierung in den Server angehen. Vollstaendiger Kontext dafuer liegt in der
Memory project_nachtschicht_bibliothekar.md (wird automatisch geladen — dort: Plan-Quelle,
v1-Empfehlung 11+13+16+18, Server-Stand, wiederverwendbare Code-Muster, Deploy-Weg, Frank-Regeln).

## Aktueller Status
- Erledigt (alles live auf dem VPS + committed + gepusht): Hybrid-Suche BM25+RRF (brain-api),
  Zeit-Parser, Entity-Register (938 Entitaeten, Bestand-Rebuild 602/602 fertig), Multi-Query,
  Confidence, Quellen-Chips, Info-Bereich/Feature-Chronik (30 Eintraege, features.json +
  /api/features), Eval-Check 100->114 Faelle, 3 Bugfixes (Dockerfile-COPY, Browser-Cache
  no-cache, zur/zum + Router-Haertung). Commits #47482-#47489. Eval-Endlauf: 114/114 (100%).
- Versionen live: brain-api 1.21.1 · agent 0.51.2 · dashboard 0.37.3.
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- `second-brain-server/LEVEL2-FEATURES-PLAN.md` — Gruppe B (Nr. 11-18) = der Nachtschicht-Plan
- `best-practices/second-brain/memory-evolution-2026-kurzcheck.md` — VOR der Arbeit lesen (#2 asynchron, #13 nur Vorschlag+Bestaetigung)
- `best-practices/second-brain/second-brain-2.0-unterbewusstsein-50-features.md` — Schwester-Plan (Bonus: ntfy-Push, Heat-Score)
- `second-brain-server/DEPLOY.md` — Deploy-Weg + Feature-Chronik-Pflegepflicht
- `second-brain-server/agent/app.py` — Muster: /entities/rebuild (Hintergrund-Thread + Status-Dict + Lock), _find_duplicates
- `second-brain-server/dashboard/features.json` — bei neuem Feature Eintrag ergaenzen (Pflicht)

## Getroffene Entscheidungen
- BM25 in-Prozess statt Qdrant-Sparse-Migration (null Risiko am Bestand, kein fastembed).
- Entity-Extraktion NUR im Agenten (brain-api bleibt stummer Speicher ohne LLM).
- Modell-Zuordnung: Multi-Query->Router-Modell, Entity-Extraktion->Speicher-Modell (keine neue UI-Auswahl).
- 'Alles ueber X'-Muster erzwingen intent=query DETERMINISTISCH (nie LLM-Ermessen bei eindeutigen Kommandos).
- Eval-Fall #76: also_ok:['save'] (Frank-Urteil: Tagebuch-Deutung legitim).
- Frank: Eval-Laeufe SPARSAM — max 1 je Anlass, er laeuft selbst alle paar Monate (Memory eval-runs-sparingly).

## Fehlgeschlagene Ansaetze
- `git commit -- <pfade>` scheitert bei NEUEN (untracked) Dateien — erst `git add <datei>`, dann committen.
- Neue Laufzeit-Datei eines Dienstes NUR per scp hochladen reicht NICHT — sie muss im Dockerfile
  in die COPY-Liste (features.json-Vorfall; die Stack-Dockerfiles kopieren bewusst einzeln).
- index.html ohne Cache-Header ausliefern -> Browser zeigt nach Deploy die ALTE Oberflaeche
  (jetzt behoben via Cache-Control: no-cache — nicht rueckgaengig machen).
- Log-Auswertung als inline python -c ueber SSH-Pipe bricht leicht (Quoting) — serverseitige
  Datei + python3 - <<PYEOF nutzen.

## Wichtige Recherche-Ergebnisse
- memory-evolution-2026: Konsolidierung ASYNCHRON in eigenem Sleep-Time-Agent (Letta-Muster),
  staerkeres Modell erlaubt; Dubletten-Merge/Umbauten NUR als Vorschlag mit Frank-Bestaetigung;
  Decay nie auf null; Proaktivitaet gedrosselt (Notify/Question/Review).
- Der geplante 'Nachzuegler-Lauf' (Eintraege ohne Entity-Verknuepfung nachziehen — schliesst die
  MCP-Luecke, Frank fand die Idee gut) gehoert sinnvollerweise IN den Bibliothekar.

## Naechste Schritte (priorisiert)
1. Frank den Nachtschicht-Bibliothekar ERKLAEREN: alle 8 Punkte (11-18) einzeln, leichtes
   Deutsch, mit Alltagsbeispielen — erst danach ueber Umfang/v1 entscheiden lassen.
2. Nach Franks Auswahl: Umsetzungs-Plan (eigener Dienst vs. Thread im Agenten abwaegen; Zeitanker
   nach dem 4-Uhr-Backup; Morgen-Report-Karte in Dashboard+App; alles nur Vorschlags-Workflow).
3. Kurzcheck memory-evolution-2026 + Almanache (fastapi/qdrant/ai-agent/docker) VOR dem Coden lesen.
4. Bei neuem Feature: features.json-Eintrag + Dashboard-Versions-Bump (DEPLOY.md-Pflichten).

## Offene Fragen
- Welche Bibliothekar-Punkte will Frank in v1? (Empfehlung aus dem Plan: 11+13+16+18.)
- Eigener Container 'librarian' ODER Nacht-Thread im agent-Dienst? (Mit Frank nach der Erklaerung klaeren.)
- Offener Intelligenz-Vorschlag: Eval-Pflege-Regel in DEPLOY.md verankern (Frank hat noch nicht geantwortet).

## Anker
- Branch: main
- Letzte Commits:
42b9d88b0 #47489 - Second Brain: agent 0.51.2 - deterministic router hardening: 'Alles ueber X' patterns force intent=query when router says smalltalk (live find: 'Zeig mir alles zur WireGuard-Einrichtung' was routed as smalltalk so the retrieval chain never ran; these phrasings are memory commands by definition - code decides, not LLM discretion)
227d33409 #47488 - Second Brain: agent 0.51.1 - 'Alles ueber X' entity detection now matches inflected 'zur'/'zum' (eval case #112 root cause, proven by regex test) + eval case #76 accepts save intent via also_ok (Frank ruling: diary phrasing may legitimately be a store wish, confirm question protects anyway); dashboard 0.37.3 visible bump
ac13443f8 #47494 - OpenCodeLauncher update visible group order
40f63cbaa #47493 - OpenCodeLauncher fix dropdown group reordering
8baba3e24 #47492 - OpenCodeLauncher repair model group persistence
