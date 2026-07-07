# Schreib-Pfad & Ingestion (Klassifikation, Extraktion, Dedup) — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Behandelt den WRITE-Pfad: was passiert, wenn Frank sagt "speicher das ab".
> Der Agent klassifiziert das Wissen, extrahiert die Fakten, wählt Kategorie/Scope, dedupliziert und speichert
> app-tauglich. Quellen: `extern` (Mem0-Blog, Patronus, ML-Mastery, Fountain City — 2025/2026).
> Schwester-Dateien: [[datenmodell]], [[orchestrator-und-suche]], [[qualitaet-pflege]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Was speichern? | **Extrahieren, nicht roh ablegen.** Rohe Transkripte = verrauschtes Retrieval. Erst zu strukturierten Fakten/Entitäten/Präferenzen destillieren, DANN schreiben. |
| Welcher Typ? | Klassifizieren in: episodisch (Ereignis+Zeit), semantisch (Fakt/Präferenz/Entität), prozedural (Regel/Workflow), kurz-/langfristig |
| Dedup beim Schreiben | Entity-Linking als Primitiv (Mem0: parallele `{collection}_entities`); "habe ich das schon?" vor dem Add |
| Konflikt (neuer widerspricht altem Fakt) | **Alten invalidieren + neuen hinzufügen** (LWW-artig mit expliziter Invalidierung), bei zeitlichen Fakten Gültigkeits-Fenster (bi-temporal, Zep) statt Überschreiben |
| Pflicht-Metadaten pro Eintrag | Timestamp, Quelle/Provenance, Kategorie, Geltungsdauer/Expiry, Confidence — Decay einbauen |
| Voice ("speicher das" im Auto) | Schreib-Call **asynchron** (keine Latenz im Gespräch); User-ID aus der App-Auth ableiten, nicht neu erfinden |
| Vertrauen | **Erst explizit, dann automatisch** — Auto-Speichern erst, wenn das System sich bewährt hat; Mensch bleibt Mitschreiber |
| RAG vs. Memory | RAG = Relevanz als Eigenschaft des INHALTS (Allgemeinwissen); Memory = Relevanz als Eigenschaft des NUTZERS (persönlicher Kontext) — getrennt halten |

---

## 1. Write-Path-Skelett (synthetisiert) (`extern`)
Der "speicher das"-Pfad hat sechs Stufen:
1. **Trigger** — Agent bekommt "speicher das" (oder ein Hook feuert auf einen Gesprächsturn).
2. **Extraktion/Klassifikation** — Rohinhalt zu strukturiertem Memory-Objekt destillieren (§2/§3).
3. **Scope-Zuweisung** — mit Scope-IDs taggen (§1.1).
4. **Dedup- & Konflikt-Check** — §4.
5. **Speicherung** — ins passende Backend (vector/graph/kv/relational).
6. **Bestätigung/Observability** — IDs + Status an die aufrufende App zurück.

### 1.1 Scopes (Mem0-Modell) (`extern`)
Mem0 nutzt vier Scopes: `user_id`, `agent_id`, `run_id`, `app_id` (+ optional `org_id`). Memory-Identität
wird aus der **App-Authentifizierung** abgeleitet, nicht vom Memory-System erzeugt — Isolation hängt an
der App-Auth. Für Frank: pro Kategorie/Domäne (programmieren, persönlich, inventar, journal) ein Scope.

### 1.2 "Der Agent entscheidet"-Muster (`extern`)
Kanonisches Tool-Interface: Der Agent ruft `add_memory` / `delete_memory` und entscheidet selbst, was
gespeichert/aktualisiert/verworfen wird. Beim Lesen holt er nur die relevanten Einträge (semantisch oder
KV-Lookup).

## 2. Klassifikation — welche Art Wissen? (`extern`)

| Memory-Typ | Definition | Speicher-Konsequenz |
|------------|-----------|---------------------|
| Kurzfristig/Working | Rolling Buffer, Kontextfenster, am Session-Ende geleert | nicht persistent |
| **Episodisch** | Zeitgestempelte Ereignisse/Interaktionen/Ergebnisse | mit Timestamp, semantisch durchsuchbar |
| **Semantisch** | strukturierte Fakten, Präferenzen, Entitäts-Beziehungen | strukturierte Records, KV/Graph |
| **Prozedural** | Regeln, Workflows, Verhaltensmuster | oft in System-Prompt/Regeln |
| Langfristig (Dach) | persistenter Speicher über Sessions | Mischung obiger |

**Lücke:** Keine Quelle gibt ein konkretes Prompt-Template, wie der Agent die Kategorie *zuweist* — die
Stufe ist benannt, nicht spezifiziert. Für Frank ableitbar: kleiner Klassifizierer (Regel/Embedding/Mini-LLM),
der Kategorie + Memory-Typ vergibt (siehe [[orchestrator-und-suche]] §1.2).

## 3. Extraktion — wie Mem0 es macht (`extern`)
- **Single-pass, ADD-only:** Der Write-Call mutiert KEINEN bestehenden Fakt, sondern fügt einen neuen hinzu;
  das Retrieval sortiert den aktuellsten nach oben.
- **Entity-Linking eingebaut:** Bei `add()` werden Entitäten extrahiert und in eine parallele Collection
  `{collection}_entities` gelegt (ersetzt einen externen Graph-Store für einfache Fälle).
- **Agenten-Bestätigungen sind first-class:** vom Agenten bestätigte Fakten werden gleichwertig zu
  nutzergesagten gespeichert.
- **Leitlinie (allgemein):** "Interaktionen zu prägnanten, strukturierten Memory-Objekten destillieren —
  Schlüsselfakten, explizite Präferenzen, Ergebnisse — VOR dem Schreiben. Hier passiert die eigentliche
  Design-Arbeit."

## 4. Dedup, Konflikt-Auflösung, Idempotenz (teilweise belegt) (`extern`)
- **Dedup:** Entity-Linking (`{collection}_entities`) ist das Primitiv ("gleiche Entität, evtl. redundante
  Memory"). **Lücke:** Kein exakter Schwellwert (cosine > X / Exact-Match / LLM-Judge) dokumentiert.
- **Konflikt (LWW vs. Merge):** Quellen nennen "LWW" nicht als Begriff. Belegt: Mem0 ADD-only (Retrieval
  wählt das Neueste); Korrektur-Muster "überschreibe/invalidiere veralteten Fakt" ("Ich bin nach Berlin
  gezogen" → alten Ort entfernen, neuen setzen) = Delete-old+Add-new ≈ LWW mit Invalidierung; Zep
  **bi-temporal** (Fakt mit Gültigkeits-Fenster, alter Fakt bleibt invalidiert erhalten). Echtes "Merge"
  definiert keine Quelle.
- **Idempotenz:** In KEINER Quelle behandelt — echte Lücke. Für Frank ableitbar: eigenen Idempotenz-Key
  (Hash aus Inhalt+Scope) beim Add mitführen, um Doppel-Writes bei Retries zu vermeiden.

## 5. Voice-Flow: STT → klassifizieren → speichern → bestätigen (`extern`)
- **Asynchrones Schreiben:** "Memory-Writes sind async, erzeugen also keine Latenz im Sprach-Gespräch"
  (Mem0/ElevenLabs-Muster: zwei async Tool-Funktionen `addMemories`/`retrieveMemories`).
- **User-ID aus App-Auth**, nicht vom Memory-System.
- **Warum Voice anders ist:** Im Gespräch kann man nicht zurückscrollen/Kontext einfügen — vergisst der
  Agent, ist die Reibung sofort spürbar.
- **Lücke:** Die volle Pipeline (STT → klassifizieren → speichern → TTS-Rückbestätigung) beschreibt keine
  Quelle. Für Frank ableitbar: nach dem async-Store eine kurze TTS-Bestätigung ("Gespeichert unter Inventar:
  Paddel = Keller-Regal links") — gibt Sicherheit, dass es richtig klassifiziert wurde (deckt sich mit
  Franks Live-Logik-Sonden-Gedanke: erwartet vs. tatsächlich).

## 6. App-tauglich speichern (Read-Seite) (`extern`)
Mem0 fährt drei Scoring-Pässe parallel (semantische Ähnlichkeit + Keyword + Entity-Match) und fusioniert —
also genau die hybride Suche aus [[orchestrator-und-suche]]. Hybrid-Retrieval (Embedding + Metadaten-Filter)
deckt den "messy middle" ab ("was sagte der Nutzer zu Thema X in den letzten 30 Tagen" = semantisch + Datumsfilter).

## 7. Best Practices & Pitfalls (`extern`)
**Best Practices:** Extrahieren vor Speichern · Retrieval an Memory-Typ anpassen · Provenance+Timestamp+Expiry
taggen + Decay · jüngere Memories höher gewichten oder TTL · RAG von Memory trennen · Memory-Identität aus
App-Auth · async Writes bei Latenz-Pfaden (Voice) · Strategien kombinieren (Sliding-Window + Summarization +
Retrieval) · erst explizit, dann automatisch · Mensch bleibt Mitschreiber.
**Pitfalls:** Rohe Transkripte speichern (verrauscht) · größeres Kontextfenster löst Memory NICHT (Context
Rot) · Memory-Footprint-Explosion (Zep ~600k Token/Konversation vs. Mem0 ~1.800 — Mem0-Eigenangabe) ·
async Ingestion = veraltete Reads ("immediate post-ingestion retrieval" verfehlt oft) · Staleness/
Cross-Session-Identity/temporale Abstraktion sind offene Probleme · kein nativer Human-Review-Workflow ·
Token-Kosten real ("Full-Context kauft <6 Punkte Genauigkeit für ~14x Token").

## Offene Lücken
supermemory in den Quellen NICHT behandelt; Idempotenz nicht behandelt; exakte Dedup-Schwellen/Prompt-Templates
nicht offengelegt; vollständige Voice-Pipeline (STT→…→TTS-Readback) nicht belegt — alles oben als "abgeleitet"
gekennzeichnet, vor Einsatz an Franks Daten prüfen.

## Quellen (`extern`, 2025-2026)
Mem0 Engineering-Blog (ADD-only, Entity-Linking, ElevenLabs-Voice-Integration); Patronus "Agent Memory";
Machine Learning Mastery "Memory for Agents" (extract-before-store, RAG vs Memory, Decay); Fountain City
(Mem0 4-Scope-Modell, Zep-Vergleich, Token-Kosten).
