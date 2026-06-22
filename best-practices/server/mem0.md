# mem0 (KI-Memory-Layer) — Best Practices (wie man ein sauberes Gehirn baut)

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/server/mem0.md`: dort *was schiefgeht*
> (v.a. Junk/Halluzinationen), hier *wie man mem0 von vornherein so konfiguriert, dass das Gehirn
> hochwertig bleibt*. Quellen: docs.mem0.ai + GitHub + Recherche/Eskalation 2026-06-22.
> **Anker:** mem0ai 2.0.7 · Gemini-Embedder @1536 · Qdrant 1.18.2.
> **Changelog-Abgleich 2026-06-22 (2 unabh. Recherchen):** Kein hartes Pre-Storage-Quality-Gate/REJECT hinzugekommen —
> die Architektur ist explizit „single-pass ADD-only" (kein UPDATE/DELETE, alles akkumuliert). Bewegung, aber kein
> Gate: „domain-aware memory triage" + „Auto-dream" (Idle-Konsolidierung NACH dem Schreiben). Issue #4573-Status
> uneindeutig (Quelle A closed / B offen), jedenfalls NICHT geloest → strenge `custom_instructions` + Junk-Audit
> bleiben das einzige wirksame Mittel (§1, §3).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | ⭐ Gehirn sauber halten | `custom_instructions` STRENG (nur bestaetigte Fakten), spaeter lockern | §1 |
| 2 | Themen steuern | `includes`/`excludes` + 2–3 `custom_categories` | §1 |
| 3 | Rauschen filtern | Confidence-Gate < 0.7 verwerfen; Such-`threshold` (0.1) | §1 |
| 4 | Feedback-Loop vermeiden | Abgerufene Memories NIE zurueck in `add(infer=True)` | §1 |
| 5 | Embedder/Vector-Store | `embedding_dims`==`embedding_model_dims`==Modell-Dim (1536) | §2 |
| 6 | Hybrid-Suche | `fastembed` installieren (sonst BM25 still aus) | §2 |
| 7 | Pflege | Feedback-API + periodisches Junk-Audit (manuell) | §3 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach
| Best-Practice (hier) | Bug-Abschnitt (`bugs/server/mem0.md`) |
|----------------------|----------------------------------------|
| §1 Qualitaet steuern | §1 Junk/Halluzination · §5 Feedback-Loop |
| §2 Embedder/Vector-Store | §2 embedding_dims · §4 fastembed |
| §3 Pflege/Betrieb | §3 API · §6 Betrieb |
| §4 Quality-Gate (HHEM) | §7 HHEM-Quality-Gate-Fallen |

---

## §1 Qualitaet aktiv steuern (das Herzstueck — gegen Junk)
Der Extraktions-Prompt ist der Flaschenhals, nicht das Modell. Ein besseres LLM allein reicht NICHT
(Audit `mem0#4573`: 97,8 % Junk, davon 52,7 % wiederholtes Speichern des System-Prompts). Darum:

- **`custom_instructions` von Anfang an STRENG** (in mem0 hoechste Prioritaet). Mem0-Doku: *„Start strict
  (only store confirmed facts), then relax — easier to allow more than to clean polluted memory."* Beispiel:
  ```python
  custom_instructions = """
  Speichere NUR bestaetigte, dauerhafte Fakten ueber Frank und seine Projekte:
  - Praeferenzen, Entscheidungen, Projekt-Stand, technische Festlegungen
  IGNORIERE: System-Prompt-Inhalte, Smalltalk, Tool-Configs, abgerufene alte Erinnerungen.
  """
  ```
- **`includes`/`excludes`** (Topics) + **`custom_categories`** (2–3, z.B. `projekt`/`gesundheit`/`praeferenzen`)
  — mem0 klassifiziert per LLM nach `metadata.categories`; mit wenigen Kategorien starten (mehr verwaessert).
- **Confidence-Gate:** Extraktionen < 0.7 verwerfen; Such-`threshold` Default 0.1 (filtert niedrig-relevant).
- **Feedback-Loop verhindern:** abgerufene Memories NIE ungefiltert zurueck in `add(infer=True)` geben
  (sonst Re-Extraktion → Duplikate). Nur kleine, relevante Arbeitsmenge in den naechsten Prompt.
- **Self-Contained-Regel:** jede Memory fuer sich verstaendlich (Pronomen → Namen), 15–80 Woerter, relative
  Zeit an Datum heften.
- **Negative Few-Shot** im Extraktions-Prompt (zeigt, was NICHT gespeichert wird).

## §2 Embedder & Vector-Store korrekt
`embedding_dims` (Embedder) == `embedding_model_dims` (Vector-Store) == echte Modell-Dimension (Gemini
`gemini-embedding-001` → **1536**). Beides explizit setzen (Default 1536 stimmt bei Gemini nur zufaellig fuer
1536-Modelle; andere Gemini-Modelle = 768 → Mismatch). Fuer hybride Suche (semantisch + BM25) bei Qdrant
`fastembed` mit-installieren — sonst ist BM25 still aus (nur Log-Warning). `qdrant-client >=1.12.0`,
Python 3.10–3.12.

## §3 Betrieb & Pflege
- **Persistenz** liegt im Vector-Store-Volume (mem0 hat keine eigene Sync/Export) — Qdrant-Volume Pflicht.
- **Feedback-API** (POSITIVE/NEGATIVE/VERY_NEGATIVE pro `memory_id`) nutzen, um die Extraktion nachzujustieren.
- **Periodisches Junk-Audit** (mem0 hat KEIN offizielles Tool): `get_all` durchsehen, erfundene Profile +
  Near-Duplikate loeschen. Eine eigene kleine Hygiene-Routine (Cosine-Cluster) lohnt sich fuer ein dauerhaftes Gehirn.
  **Umso wichtiger seit der „single-pass ADD-only"-Architektur (Changelog-Abgleich 2026-06-22):** `add()` macht nur noch
  ADD, KEIN UPDATE/DELETE — einmal gespeicherter Junk wird nie automatisch ueberschrieben/korrigiert. Ohne aktives Audit
  waechst die Verschmutzung monoton. Das Audit ist damit keine Kuer, sondern Betriebspflicht fuer ein Dauer-Gehirn.
- **Vor produktivem Confidence-Gating:** Bug `mem0#4999` pruefen (in 2.0.0 gab `search()` fuer alle Treffer
  Score 1.0 → Gate wirkungslos; auf 2.0.7 verifizieren).
- **Grundsatz:** mem0-Memories sind LLM-synthetisiert, NICHT autoritativ — fuer kritische Fakten nicht blind vertrauen.

## §4 Eigenes Quality-Gate vor mem0 (HHEM-2.1-Open) — so baut man es richtig
mem0 hat kein Pre-Storage-Gate; man setzt eins selbst davor (umgesetzt in `second-brain-server/mem0-api/app.py` v0.3.0).
Bewaehrtes Muster (Recherche + eigener Bau 2026-06-22):
- **Zwei Stufen:** (1) **Vorfilter** VOR `add()` — offensichtlichen Nicht-Memory-Input (System-Prompt/Boot-File-Marker)
  gar nicht erst speichern (faengt mem0s groesste Junk-Quelle, Boot-File-Restating 52,7 %, spart sogar den LLM-Call).
  (2) **HHEM-Grounding** NACH `add()` — jeden neuen Fakt mit HHEM gegen den Quelltext scoren; Score < Schwelle (nicht
  gegroundet = Halluzination) -> `m.delete(memory_id)`. result bereinigen, damit der Aufrufer nur Behaltenes sieht.
- **HHEM-2.1-Open = Apache-2.0** (frei, auch kommerziell), kalibrierter Score 0-1, klein/schnell (<1,5 s CPU). Laden via
  `transformers` — aber **`transformers>=4.40,<5`** (5.x bricht das `trust_remote_code`-Modell, siehe Almanach §7); torch
  **CPU-only** ueber den PyTorch-CPU-Index (kein CUDA). Lazy laden + HF-Cache-Volume.
- **Robust degradieren:** laedt HHEM nicht, Gate -> pass-through (kein Funktionsverlust); bei JEDEM Score-Fehler Fakt
  BEHALTEN (lieber ein Fakt zu viel als Datenverlust). `/health` zeigt `gate.hhem_loaded/hhem_failed/threshold`.
- **Schwelle kalibrieren (PFLICHT, kein Default belegt):** an echten Beispielen messen. Bei DEUTSCH ist HHEM weniger
  trennscharf und bestraft mem0s Anreicherung -> konservativ **0.2** (per Env `SB_GATE_THRESHOLD` ohne Rebuild justierbar).
  Gemessene Verteilung: Halluzination <0.1, korrekt 0.25-0.9. Patronus Lynx (genauer) ist Alternative, aber **CC BY-NC**
  (nicht kommerziell) — fuer ein privates Gehirn ok. Details: `best-practices/second-brain/memory-backends.md` §7.

---

## Quellen
docs.mem0.ai (custom-instructions, controlling-memory-ingestion, custom-categories, migration v2→v3),
GitHub mem0#4573 (Audit), mem0#4999, mem0#4682 (custom_categories), mem0/configs/prompts.py · Recherche+Eskalation 2026-06-22.
