# Session Handoff — 2026-06-25, ~16:00

## Ziel
Second-Brain (Cortex) "Unterkategorien + intelligente Suche" umsetzen: 2-Ebenen-Kategorien
(`Haupt/Unter`) ueberall im Dashboard nutzbar + intelligentere Suche. Plan in 6 Phasen, Frank will
ALLE 6 am Stueck (pro Phase committen+pushen, am ENDE einmal auf den VPS deployen).
Voller Dauer-Stand: Memory `project_second_brain_subcategories.md` (im Zweifel ZUERST lesen).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende/unterbrochene Aufgabe — Phase 1 ist sauber abgeschlossen, committet UND gepusht.
Kein uncommitteter Code-Stand im Working Tree (nur Hook-generierte agent-memory/*.jsonl + MEMORY.md,
nicht meine Aufgabe). Naechster Schritt ist der ERSTE Punkt unter "Naechste Schritte".

## Aktueller Status
- Erledigt HEUTE (alle gepusht): #47199 Drawer Titel-bearbeiten + Kategorie-verschieben (DEPLOYT, live),
  #47208 Favicon Cortex-Gehirn (DEPLOYT, live), #47209 Recherche-Persistenz (bugs/best-practices),
  #47210 Phase 1 Backend-Fundament (brain-api v1.9.0, gepusht — ABER NICHT DEPLOYT).
- Offen: Phasen 2-6 (siehe Naechste Schritte). Phase 1 noch nicht auf VPS.

## Getroffene Entscheidungen (Frank, verbindlich)
- Hierarchie: GENAU 2 Ebenen (`Haupt/Unter`), nicht tiefer.
- Kategorie ins Embedding: JA (gleich mitgebaut) -> Verschieben/Aendern loest Re-Embed aus.
- Routing: WEICH + Fallback (nie hart wegfiltern). Reranking: NICHT (DB zu klein). Hybrid: optional spaeter.
- Qdrant hat KEINEN Praefix-Filter -> separates `parent`-Feld + Index (Recherche #47209).

## Fehlgeschlagene Ansaetze / Stolpersteine
- Praefix-Filter "alles unter Haupt" geht in Qdrant NICHT nativ -> deshalb `parent`-Feld (schon gebaut).
- `set_payload` fuer Kategorie-Wechsel reicht NICHT mehr, seit die Kategorie im Embedding steckt
  -> `/entry/category` macht jetzt Re-Embed (schon umgestellt in Phase 1).

## Wichtige Recherche-Ergebnisse (persistiert #47209)
bugs/server/qdrant.md §7 (kein startsWith #5300, nested-Probleme #2256, Index VOR Ingest),
best-practices/server/qdrant.md §6 (parent-Feld-Muster), NEU best-practices/server/rag-retrieval.md
(Metadaten=Scope; weiches Routing+Fallback NVIDIA-Blueprint; Embedding-Anreicherung-Tradeoff;
Reranking bei kleiner DB unnoetig).

## KRITISCH — parallele Session am Agent
Eine parallele Session hat agent/app.py stark umgebaut (v0.19 -> v0.23.1: Internet-Werkzeug Tavily +
Leseagent-Architektur = Leseagent filtert per JSON-Nummern, Hauptagent formuliert). Phase 5+6 (Agent)
ERST bauen, wenn diese Session fertig ist (sonst Konflikt + bewegliche Architektur). Phasen 2-4
(reines index.html) sind davon unberuehrt.

## Naechste Schritte (priorisiert)
1. ENTSCHEIDUNG mit Frank (offene Frage, s.u.): Erst Phase 1 deployen + backfill-parent, ODER direkt
   Phasen 2-4 bauen? Frank hatte sich noch nicht entschieden, als er laufen ging.
2. Phase 2: Einstellungen->Kategorien als BAUM (Haupt > Unter eingerueckt) + "Unterkategorie
   hinzufuegen" (Frontend dashboard/static/index.html, Funktionen loadCatManager/enhanceSelect/syncCS).
3. Phase 3: Gespraech- + Drawer-Kategorie-Dropdown als Baum + neue Unterkategorie (index.html,
   loadChatCategories + loadDrawerCategories).
4. Phase 4: Uebersicht (renderOverview) — erst Hauptkategorien, Klick klappt Unterkategorien auf
   (anklickbar/filterbar; GET /api/by-parent als Proxy noch anlegen in dashboard/app.py).
5. Phase 5 (NUR nach paralleler Agent-Session): Lese-/Abfrageagent weiches Routing auf parent/category
   + Fallback bei 0/duenn; agent `_cat_key` um 2-Ebenen-Normalisierung erweitern; SearchReq.parent nutzen.
6. Phase 6: Speicheragent Unterkategorie-Vorschlag + Rueckfrage. DANACH Gesamt-Deploy + backfill-parent
   + Verifikation aller Phasen.

## Deploy-Erinnerung (PFLICHT vor "fertig")
Phase 1 (und spaeter alle) auf VPS: scp der geaenderten Dateien nach /opt/second-brain + 
`ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 'cd /opt/second-brain && docker compose up --build -d <service>'`.
NACH dem Deploy EINMAL `POST /backfill-parent` aufrufen (Altbestand mit parent versorgen).

## Offene Fragen
- Frank: erst Phase 1 deployen+backfill (Phase 1 schon live pruefen), oder direkt UI-Phasen 2-4 bauen?

## Anker
- Branch: main
- Letzte Commits:
78e6ef250 #47210 - feat(second-brain brain-api): Unterkategorien-Fundament v1.9.0 (Phase 1)
74b7c31c3 #47212 - feat(second-brain agent): Eval-Set an Internet-Suche angepasst v0.23.1 (parallele Session)
d0eec5cf1 #47211 - fix(harness): Task-Ledger-Hook redaktiert Secrets (parallele Session)
2b16a988c #47210 - feat(second-brain agent): Internet-Suche als 3. Werkzeug v0.23.0 (parallele Session)
213845114 #47209 - docs(bugs+best-practices): Qdrant-Hierarchie/Filter + RAG-Retrieval (Recherche)
