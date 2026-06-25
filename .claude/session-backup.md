# Session Handoff — 2026-06-25, abends

## Ziel
Second-Brain (Cortex) Unterkategorien-Feature + Speicher-/Ausgabe-Bugs. Erledigt: gesamtes
Unterkategorien-Vorhaben (beliebig tief) im ganzen Dashboard + Agent, plus eine Bug-Serie ums
Speichern/Abrufen grosser Eintraege. Alles deployt auf VPS (WireGuard, /opt/second-brain).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe — alles committet, gepusht UND auf den VPS deployt, end-to-end verifiziert.
Kein uncommitteter eigener Code im Working Tree.

## Aktueller Status (alles ERLEDIGT + DEPLOYT)
- Unterkategorien Phase 2-6 + Drilldown + BELIEBIG TIEFE Hierarchie (A/B/C/...): Einstellungen-Baum
  rekursiv, beide Kategorie-Dropdowns (Gespraech+Drawer) rekursiver Drilldown (Knoten waehlen /
  tiefer / + Unterkategorie), Uebersicht-Legende rekursiv. Commits #47216-#47229.
- brain-api 1.10.0 deployt + POST /backfill-parent gelaufen (174 Chunks/17 Gruppen). /by-parent live.
- BUG-SERIE "grosser Eintrag" behoben (Frank-Bugs):
  1. Router scheiterte an grossem Paste -> "nicht verstanden" + falscher Titel + nichts gespeichert.
     Fix: explicit_save (Titel/Kategorie gesetzt -> Router ueberspringen, direkt save). Agent 0.28.0 (#47230).
     Dashboard: Titel-Feld nur leeren wenn Save erkannt (#47231).
  2. Langer Eintrag wurde bei 8000 Zeichen ABGESCHNITTEN — drei 8000er-Caps: dashboard /api/chat
     text[:8000] (#47233, dashboard 0.21.0) + agent ChatReq.text max_length=8000 (#47234, agent 0.29.0).
     Beide auf 100000; Kategorie-Caps 60->120. brain-api StoreReq hat KEINEN Cap (chunkt selbst).
     Verifiziert: 15413-Zeichen-Text komplett gespeichert.
  3. AUSGABE-Haertung (#47236, agent 0.30.0): Leseagent bekam Volltext aller Treffer -> jetzt nur
     Relevanz-Schnipsel (LESE_SNIPPET_CHARS=1200); Hauptagent-Antwort gedeckelt pro Treffer
     (ANSWER_HIT_CHARS=8000) + gesamt (ANSWER_TOTAL_CHARS=24000) mit "gekuerzt"-Hinweis -> Drawer.
     Verifiziert: 28551-Zeichen-Eintrag abgefragt -> gefunden, gefiltert, korrekt beantwortet (BANANE-7).
- Bug-cases dokumentiert: #47232, #47235.
- LIVE-Versionen VPS: dashboard 0.21.0, brain-api 1.10.0 (Achtung: parallele Session hatte brain-api
  zwischenzeitlich auf 1.11.0 Multi-Category committet — Repo-HEAD kann hoeher sein als deployt),
  agent 0.30.0. Alle healthy.

## Relevante Dateien
- second-brain-server/agent/app.py (0.30.0) — _process_turn explicit_save, _cat_key beliebig tief,
  ChatReq/Caps, leseagent_select/hauptagent_answer Ausgabe-Haertung, Speicheragent-Hierarchie-Prompts.
- second-brain-server/dashboard/static/index.html — rekursive Baeume (buildPathTree, renderCatTree,
  renderCSTree, Uebersicht-Legende), Drilldown-Dropdowns.
- second-brain-server/dashboard/app.py (0.21.0) — /api/chat text-Cap 100000, category 120, /api/by-parent.
- second-brain-server/brain-api/app.py — /by-parent, parent-Feld, embed_input (Titel+Kategorie im Vektor).
  ACHTUNG: brain-api wird von einer PARALLELEN Session aktiv bearbeitet (Multi-Category 1.11.0) — nicht blind anfassen.

## Getroffene Entscheidungen (Frank)
- Hierarchie BELIEBIG tief (nicht nur 2 Ebenen). Pfad als String "A/B/C" (im Embedding als "A > B > C").
- Deploy IMMER nur committeter HEAD-Stand (git show HEAD:...) per scp, NIE Working-Tree (Fremdcode).
- Confirm-before-store bleibt (Ja/Nein-Knoepfe).

## Fehlgeschlagene Ansaetze / Stolpersteine
- Router den grossen Text wortwoertlich in quote+reply echoen lassen -> max_tokens-JSON-Truncation. NICHT so.
- Beim Test direkt nach dem Speichern abfragen -> Qdrant-Index-Delay, ~5-6s warten.
- Router stuft vage Fragen ("Wie lautet das Codewort?") manchmal als smalltalk ein (sucht nicht) —
  klare Such-Formulierung ("Was habe ich zu X gespeichert?") routet zuverlaessig auf query.

## Naechste Schritte (priorisiert, OFFEN)
1. NOETIG (Frank): Loop-Engineering-Almanach NEU einfuegen — der aktuell gespeicherte Eintrag
   "Loop Engineering" ist nur die erste Haelfte (~8000 Zeichen, vom alten Cap). Gleicher Titel
   "Loop Engineering" -> ersetzt den unvollstaendigen automatisch. Diesmal kommt der volle Text an.
2. Router schaerfen: Few-shot-Beispiele im Router-Prompt, damit vage Wissensfragen auf query statt
   smalltalk gehen (kein Datenrisiko, nur "suchte diesmal nicht").
3. brain-api: Umbenennen/Verschieben sehr tiefer Kategorien (Name >60 Zeichen) — dort haengt noch ein
   60-Cap (Edge-Case). NUR wenn brain-api frei (parallele Session beachten).
4. Optional: Langtext-Tests (Eingabe+Ausgabe) ins Eval-Set; ancestors-Feld fuer echtes Tief-Prefix-Filtern.

## Offene Fragen
- Keine offene Rueckfrage. Frank wollte zuletzt nur das Backup.

## Anker
- Branch: main
- Letzte Commits:
1d651f4b8 #47236 - feat(second-brain agent): Ausgabe-Haertung fuer grosse Eintraege v0.30.0
9851b4ad9 #47235 - docs(bug-cases): Second-Brain langer Eintrag nur halb gespeichert
b3797ba07 #47234 - fix(second-brain agent): 2. Cap-Schicht ChatReq.text 8000->100000 v0.29.0
d4b8c7783 #47233 - fix(second-brain dashboard): langer Eintrag bei 8000 abgeschnitten v0.21.0
fa7ad82e0 #47232 - docs(bug-cases): Second-Brain Agent grosser Text nicht gespeichert
