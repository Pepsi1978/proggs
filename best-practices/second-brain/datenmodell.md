# Datenmodell & Kategorien — Best Practices (Stand 2026-06-21)

> Teil des Second-Brain-Wissens. Behandelt, WIE man ein heterogenes persönliches Memory strukturiert:
> Code-Wissen + Personen-Kontext + Wohnungs-/Inventar + Aufgaben/Ideen/Journal in EINEM Schema, auf das
> viele Apps zugreifen. Quellen: `extern` (Cortex "How I Built My Second Brain" 05/2026, Notion-PKM,
> Tiago Forte, mem0/Zep-Charakterisierung). Schwester: [[schreibpfad-ingestion]], [[orchestrator-und-suche]], [[memory-backends]].

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Grund-Architektur | **Drei-Speicher-Muster:** Graph (Entitäten+Beziehungen) + Memories (destillierte Fakten) + Chunks (Rohtext, doppelt indiziert: Keyword + semantisch) — bei jedem Ingest parallel füllen |
| Pflicht-Metadaten pro Eintrag | `timestamp`, `source/provenance`, `category`, `confidence` (0-1), `valid_from/valid_until` (Geltung), `tags` |
| Confidence | Jeder extrahierte Eintrag bekommt Score 0-1 (1.0 = wörtlich im Quelltext, niedriger = inferiert/paraphrasiert) |
| Sich ändernde Fakten (Adresse, Job) | **Bi-temporal** (Zep): alten Fakt invalidieren mit Gültigkeits-Fenster statt löschen → Zeitreise-Abfragen |
| Scopes | Owner-Entity beim Init ("wer bin ich") + Kategorie/Domäne als Scope (programmieren/persönlich/inventar/aufgaben/journal) |
| Tags vs. Hierarchie | Tags ERGÄNZEN die Hierarchie ("Tunnel durch die Wände der Silos"), ersetzen sie nicht |
| Graph vs. Vektor vs. strukturiert | Beziehung→Graph, Konzept→Vektor, exakter Ort/Feld→strukturiert (siehe [[orchestrator-und-suche]] §2.1) |
| Extraktion | Deterministisch (YAML/CSV/JSON/Wikilinks) + LLM (Prosa→Entitäten/Fakten) kombinieren |

---

## 1. Drei-Speicher-Architektur (`extern`, Cortex)
Das einzige in den Quellen voll ausgearbeitete Schema (Projekt "Cortex", Sau Sheong, 05/2026) speichert bei
jedem Ingest **drei Dinge parallel**:
- **Graph:** wer/was und wie verbunden (gerichtete Beziehungen, getypte Entitäten).
- **Memories:** vom LLM destillierte "high-signal facts" als kurze, abrufbare Aussagen.
- **Chunks:** der Rohtext verbatim, **doppelt indiziert** (Volltext + Embedding).

Beispiel "Sumiko works at GovTech as a data scientist": Sumiko=Person, GovTech=Organisation, gerichtete
Kante "works at", Satz→Memory destilliert, Originaltext bleibt indiziert. Inspiriert von Cognee (getypte
Entitäten + Multi-Strategy-Retrieval). **Hinweis:** kein formales JSON-/ER-Schema in der Quelle — die
Feldnamen unten sind daraus abgeleitet.

## 2. Graph vs. Vektor vs. strukturiert — wann was (`extern`)
- **Karpathy "LLM Wiki Pattern":** ein dauerhaftes, vom LLM gepflegtes Wiki ist wertvoller als RAG über
  Rohdokumente bei jeder Anfrage. Cortex automatisiert das (DB + deterministische Extraktion + Volltext +
  Vektor + Graph-Traversierung statt manueller Wiki-Pflege).
- **Multi-Strategy-Retrieval:** Graph-Traversierung + Volltext + Vektor kombiniert.
- Konkrete Auswahl-Heuristik: siehe [[orchestrator-und-suche]] §2.1 (die belegt-abgeleitete Routing-Tabelle).

## 3. Pflicht-Metadaten (`extern`)

| Feld | Belegt? | Detail |
|------|---------|--------|
| `timestamp` | ✅ | Cortex speichert Timestamps; Notion "Last Updated" |
| `source/provenance` | ⚠️ teilweise | Chunks behalten Originaltext (Source-of-truth); kein formales URL-Feld dokumentiert |
| `category/type` | ✅ | Notion "type of media"; Cortex erkennt Format (CSV/JSON/YAML/Markdown) |
| `confidence/importance` | ✅ | "Every item gets a confidence score 0-1" (1.0 wörtlich, niedriger inferiert) |
| `valid_from/valid_until` | ⚠️ nur via Zep | Zep bi-temporal (Fakt invalidierbar ohne Löschung, Zeitpunkt-Abfrage); Cortex hat das NICHT |
| `tags` | ✅ | siehe §5 |

## 4. Heterogene Inhalte (`extern` + abgeleitet)
**Belegt:** Cortex extrahiert generisch aus Prosa (Entitäten/Beziehungen/Fakten) — prinzipiell für beliebige
Inhalte. Notion deckt Bücher/Videos/Artikel/Notizen ab.
**NICHT belegt (Lücke):** Home-Inventar ("was in welcher Schublade"), Tasks, Code-Snippets, Journal als
eigene Memory-Typen — in KEINER Quelle als Schema dokumentiert. Das Extraktions-PRINZIP ist generisch, aber
es gibt keine fertigen Schemata dafür.

### 4.1 Abgeleitetes konkretes Schema für Frank (Synthese, nicht Quellen-Wortlaut)
Ein Memory-Record, der die belegten Muster (Drei-Speicher, Confidence, bi-temporal, Scopes) mit Franks
heterogenen Typen verbindet:
```jsonc
{
  "id": "uuid",
  "content": "Rohtext / Aussage",
  "type": "episodic | semantic | procedural",   // siehe schreibpfad-ingestion §2
  "category": "programmieren | persoenlich | inventar | aufgaben | journal | ...",
  "scope": "user | domaene:<name>",              // Owner + Domäne
  "entities": ["Sumiko", "GovTech"],             // → Graph
  "relations": [{"from":"Sumiko","rel":"works_at","to":"GovTech"}],
  "source": "voice | claude-code | app:auto | ...",
  "created_at": "ISO", "valid_from": "ISO", "valid_until": null,  // bi-temporal
  "confidence": 0.0,                              // 0-1
  "tags": ["..."],
  "embedding": "[vector]",                        // für semantische Suche
  // typ-spezifische Erweiterungen:
  "fields": { "item":"Paddel", "location":"Keller-Regal links" }  // z.B. Inventar
  // oder { "status":"offen", "due":"ISO" } für Aufgaben
}
```
Kern + `fields` als typ-spezifische Erweiterung hält das Schema EINHEITLICH (eine Abfrage-Schicht für alle
Apps) UND erweiterbar (neue Kategorien docken über `fields` an, ohne das Kern-Schema zu brechen). Genau die
"eine Wahrheit, viele Apps"-Anforderung aus [[../../.../]] Franks Zielbild.

## 5. Scopes & Tags (`extern`)
- **Owner-Entity:** `cortex init` registriert "wer bin ich" (Name, Spitzname, E-Mails) → "who am I"-Anfragen
  funktionieren ab Werk.
- **Project/Domain als formale Scope-Ebene:** NICHT belegt (Quellen nutzen Ordner-Hierarchie/separate DBs).
  Für Frank: Kategorie = Scope (siehe §4.1).
- **Tags:** "der Dreh- und Angelpunkt" (Notion); 400 Tags über 865 Notizen als Querverbindung; Forte: Tags
  "tunneln durch die Wände der Silo-Ordner" — ERGÄNZEN Hierarchie, ersetzen sie nicht ("Netzwerk in der
  Hierarchie, Hierarchie im Netzwerk").

## 6. Wie mem0/Zep/Cortex modellieren (`extern`)
- **mem0:** High-Level-API `remember`/`recall`/`forget`; für Engineering-Teams (kein Notizen-Ordner,
  kein Wikilink-Parsing); Schema-Felddetails nicht dokumentiert.
- **Zep:** temporaler Knowledge-Graph, **bi-temporal** (was war wann wahr), Fakten invalidierbar; "mehr
  Architektur als die meisten brauchen" für ein persönliches Second Brain.
- **Cortex:** Drei-Speicher + Confidence + deterministisch+LLM + Owner-Entity + Auto-Linking + periodische
  Konsolidierung (Cron); KEIN bi-temporales Modell. Lokale SQLite-Datei `brain.db`.
- **supermemory:** in den Quellen DIESES Laufs nicht erwähnt — siehe [[memory-backends]] für den dedizierten Vergleich.

## 7. Weitere Design-Prinzipien (`extern`)
Capture-Retrieval-Gap ("Capture funktioniert, Retrieval nicht, nichts verbindet beide" → Automation
zwischen beiden); atomare Notizen ("eine Notiz = eine Idee", Zettelkasten); CODE-Lebenszyklus
(Capture→Organize→Distill→Express); Suche vs. Navigation (Nutzer navigieren gern in Hierarchien →
Hierarchie bleibt sinnvoll, nicht nur Suche).

## Offene Lücken
Kein publiziertes formales JSON/SQL-Schema in den Quellen; Inventar/Tasks/Journal/Code als Typen nicht
belegt (§4.1 ist Synthese); formale user/project/domain-Scope-Hierarchie nicht belegt; mem0/Zep-Feldinterna
nicht offengelegt. §4.1-Schema vor Einsatz an echten Daten erproben.

## Quellen (`extern`, 2025-2026)
Sau Sheong "How I Built My Second Brain (Cortex)" 05/2026; Irfan Bhanji "Notion PKM"; Tiago Forte (Building
a Second Brain, Tags vs Hierarchie); Cognee (typed entities); mem0/Zep-Charakterisierung aus dem Cortex-Artikel.
