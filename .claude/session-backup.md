# Session Handoff — 2026-06-25, ~14:00 Uhr

## Ziel (1-3 Saetze)
Am **second-brain-server** (Cortex, Franks "zweites Gehirn") weiterbauen. Diese Session: drei grosse Bloecke abgeschlossen — (1) Kategorie-Verwaltung + deutsche Grossschreibung, (2) Agenten-Haertung Paket A/B/C, (3) komplettes Eval-Check-System (90 Test-Saetze). Alles deployed + live verifiziert.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende Aufgabe, letzter Stand sauber abgeschlossen.** Alles committed (bis #47202), gepusht, auf dem VPS deployed und per Live-Lauf verifiziert. Nichts uncommittet im Working Tree (second-brain-server clean).

## Aktueller Status
- **Erledigt (alles live auf dem VPS):**
  - Kategorie-Verwaltung im Dashboard (Umbenennen/Loeschen/Merge/leere anlegen), deutsche Grossschreibung (13 Bestandskategorien migriert: persoenlich->Persoenlich etc.), Gespraech-Dropdown synchron + leere ausgegraut. (#47187-47190)
  - Agenten-Haertung Paket A (Injektions-Schutz in allen 3 geschuetzten Bloecken + Eskalation: neue/unsichere Kategorie -> Rueckfrage statt still speichern, pending-mode store_clarify). Paket B (Quittung mit doc_id "no receipt no claim", typisiertes Routing-Enum, MAX_TOKENS-Erkennung). Paket C1 (Rate-Limit-Retry Full-Jitter-Backoff). (#47191-47195)
  - Anklickbare Antwort-Knoepfe im Chat (save_confirm + store_clarify liefern 'options', Dashboard rendert Ja/Nein-Knoepfe). (#47196)
  - **Eval-Check-System komplett:** brain POST /purge (hard-delete eval-Nutzer, Schutz gegen 'frank'), agent /eval-run mit 90 Test-Saetzen (24 store, 21 query, 45 smalltalk inkl. 10 sinnlose Plauder-Saetze, 3 Injektion), isoliert unter user 'eval-test', hart aufgeraeumt, Markdown-Log auf Z /eval-logs (14 Tage Retention, im Backup). Dashboard: Eval-Knopf + Auswertung + Logs-Ansicht im System-Prompt-Card. (#47197-47202)
- **Live-Eval-Ergebnis:** 89/90 bestanden. Frank-Gehirn unveraendert (174->174), eval-test 0 Reste, Log auf Z.
- **In Arbeit:** nichts.
- **Blockiert:** nichts.

## Versionen auf dem VPS (alle healthy)
brain-api **1.8.0**, agent **0.21.0**, dashboard **0.15.0** (dashboard-Version von paralleler Session gesetzt).

## Relevante Dateien
- `second-brain-server/agent/app.py` — Hauptagent/Speicher/Abfrage, EVAL_CASES (90 Saetze, id 1-90), _run_eval/_eval_one, /eval-run, brain_store/brain_search(+user_id), brain_purge. Haertung A/B/C. _canonical_category (Kategorie-Kanonisierung).
- `second-brain-server/brain-api/app.py` — 1:1-Speicher; POST /purge (eval-Aufraeumung), rename/detach/category-counts.
- `second-brain-server/dashboard/app.py` — Proxys /api/eval/run|logs|log (langer Timeout), /api/categories/*.
- `second-brain-server/dashboard/static/index.html` — Kategorie-Verwaltung, chatOptions-Knoepfe, Eval-UI (evalRunBtn/evalLogsBtn/renderResult/showLog).
- `second-brain-server/compose.yaml` — agent-Mount /srv/samba/gedanken/Eval-Logs:/eval-logs.

## Getroffene Entscheidungen
- Kategorien werden 1:1 als Klartext (deutsche Rechtschreibung) gespeichert; Dubletten-Schutz case-insensitiv (kein lowercase/Slug mehr). Frank pflegt offene Liste, Agent eskaliert bei neuer Kategorie statt still anzulegen.
- Eval laeuft unter eigenem user 'eval-test' (Isolation) + hard-purge (kein Papierkorb). PASS-Kriterium: store = gespeichert+verifiziert ("rein+raus"), query = erwarteter Inhalt in Antwort, smalltalk = intent==smalltalk. Router-Intent wird zusaetzlich geloggt.
- KEINE Internet-Suche im Hauptagent: statische Wissensfragen aus Gemini-Wissen (smalltalk), Live-Fragen ehrlich verneint.

## Fehlgeschlagene Ansaetze / Fallen (WICHTIG)
- `checkpoint(step, intent, ok, **ctx)` hat 'intent' als 2. Positional-Param -> NIE `intent=` als kwarg uebergeben (war ein Chat-Totalausfall, #47194). Andere kwarg-Namen nutzen (z.B. route=).
- agent /categories/rename: reine Gross-/Kleinschreibung ('fitness'->'Fitness') ist eine ECHTE Aenderung -> EXAKTER String-Vergleich `old != new`, NICHT casefold (#47190).
- brain DELETE /entry erwartet doc_id als QUERY-Param, nicht im JSON-Body.
- **Parallele Sessions in DERSELBEN Arbeitskopie**: eine andere Session arbeitete gleichzeitig am gleichen Code (Drawer-Eintragsbearbeitung + agent /categories/move-entry, #47199). Sie packte meine uncommitteten Edits in ihren Commit. Loesung: nach jedem fertigen Teil SOFORT atomar committen (`git commit -- <pfad>`), fetch+rebase vor push.

## Naechste Schritte (priorisiert, falls fortgesetzt — alles OPTIONAL, Frank hat noch nicht zugestimmt)
1. **#61-FAIL fixen:** Hauptagent-Prompt schaerfen, damit reine Wissensfragen ('Erklaer mir Aktien vs ETFs') direkt als smalltalk beantwortet werden statt query (Gedaechtnis-Suche). Danach Eval-Lauf gegenpruefen.
2. Internet-Suche fuer den Hauptagent (Live-Fragen Wetter/Sport).
3. Eval-Check automatisch nach jedem Deploy (Regressionsschutz).
4. Eval-Set um Update/Dubletten-Tests erweitern.
5. Eval-Trend im Dashboard (mehrere Laeufe vergleichen).

## Offene Fragen
- Frank wollte zu den 2 Intelligenz-Vorschlaegen (a) #61-Fix + Gegenpruefung, (b) atomares Committen bei Parallel-Sessions als feste Gewohnheit — noch keine Antwort.
- Pfannkuchen-Rezept-Eintrag liegt noch in Franks Papierkorb (Altrest aus Paket-A-Test, kein Eval-Rest). Frank wollte ihn selbst leeren.

## Server-Zugang (Deploy)
VPS 168.231.83.205, /opt/second-brain (KEIN git! Deploy = scp + `docker compose up -d --build <service>`). SSH-Key ~/SK/second-brain/id_ed25519 (lokal nach Temp kopieren + chmod 600). SB_API_KEY aus /opt/second-brain/.env. Interne Ports: brain 10.8.0.1:8000, agent :8002, dashboard :8003.

## Anker
- Branch: main
- Letzte Commits:
6dc554b8c #47202 - Eval-Set auf 90 Saetze (agent v0.21.0)
f6cb06a9f #47201 - Eval-Check-UI + compose Eval-Logs-Mount
6f07774b9 #47200 - Eval-Set auf 80 Saetze (agent v0.20.0)
ccd5d02eb #47199 - Eintrag im Drawer (parallele Session)
f56086d00 #47198 - Eval-Check Runner + 50 Test-Saetze (agent v0.18.0)
