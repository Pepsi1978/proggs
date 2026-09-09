# Übergabedokument: KI-Agenten — Vollständige Materialsammlung für ein illustriertes PDF

**An die empfangende KI:** Dieses Dokument enthält den kompletten Inhalt für ein hochwertiges,
reich illustriertes PDF zum Thema KI-Agenten. Du bekommst hier alles: Fließtext, alle Zahlen mit
Quellen, konkrete Illustrationsbriefings und die ausdrücklich markierten Wissenslücken.

**Bitte beachte drei Dinge:**

1. **Erfinde keine Zahlen dazu.** Jede Zahl unten hat eine Quelle. Wo etwas nicht belegt ist, steht
   das ausdrücklich dabei — bitte übernimm diese Kennzeichnung ins PDF, statt sie wegzuglätten.
2. **Die Illustrationsbriefings sind Mechanik-Beschreibungen, keine Dekowünsche.** Jede Abbildung
   soll einen Vorgang zeigen, den man aus dem Text sonst mühsam zusammensetzen müsste. Ein Kasten
   mit der Aufschrift „Cache" sagt weniger als der Weg, den eine Anfrage durch ihn nimmt.
3. **Sprache:** Deutsch, mit echten Umlauten. Fachbegriffe (Agent, Loop, Handoff, Orchestrator)
   bleiben in ihrer üblichen Form.

**Herkunft des Materials:** Recherche vom 09.09.2026 mit 21 parallel laufenden Researchern über
drei verschiedene Such-Engines, die alle wortgleich dieselben sieben Unterthemen bearbeitet haben.
Kapitel 8 wertet die Engines gegeneinander aus — das ist ein Nebenbefund, der aber in das PDF
gehört, weil er zeigt, wie unterschiedlich KI-Rechercheergebnisse ausfallen.

**Zielgruppe:** technisch versierte Leser, die Agentensysteme bauen oder bewerten. Kein
Einsteiger-Erklärstück, aber auch kein Paper — ein Handbuch.

**Umfang:** großzügig. Das Material trägt 40 bis 60 PDF-Seiten mit Abbildungen.

---

# TEIL 1 — Was ein KI-Agent ist

## Die gemeinsame Definition

Anthropic, OpenAI und Google definieren einen KI-Agenten strukturell gleich: **ein Modell, das
selbst entscheidet, welche Werkzeuge es einsetzt, und dabei in einer Schleife läuft, bis ein Ziel
oder eine Abbruchbedingung erreicht ist.**

Der entscheidende Punkt ist nicht die Schleife, sondern wer den Pfad bestimmt: nicht der Code,
sondern das Modell.

**Wörtliches Zitat (Anthropic, "Building Effective Agents"):**
> "Workflows are systems where LLMs and tools are orchestrated through predefined code paths.
> Agents, on the other hand, are systems where LLMs dynamically direct their own processes and
> tool usage, maintaining control over how they accomplish tasks."

**Wörtliches Zitat (Anthropic, "Trustworthy agents in practice"):**
> Agent = "AI model that directs its own processes and tool use when accomplishing a task".
> Chatbots seien "simple question-and-answer machines"; Agenten dagegen "plan, act, observe the
> result, adjust, and repeat", bis die Aufgabe fertig ist oder menschliche Eingabe nötig wird.

**Wörtliches Zitat (OpenAI Agents SDK Doku):**
> "An agent is a large language model (LLM) configured with instructions, tools, and optional
> runtime behavior such as handoffs, guardrails, and structured outputs."

**Wörtliches Zitat (Google, Whitepaper "Agents"):**
> Ein Agent sei "a program that extends beyond the standalone capabilities of a Generative AI
> model" — er verfolgt ein Ziel durch Beobachtung und Handlung mittels verfügbarer Werkzeuge,
> mit Autonomie und Proaktivität als Schlüsselmerkmalen.

## Die Dreiteilung Chatbot / Workflow / Agent

| | Wer bestimmt den Pfad | Handelt es nach außen? |
|---|---|---|
| **Chatbot** | der Mensch, Zug um Zug | nein — reines Frage-Antwort |
| **Workflow** | fest verdrahteter Code | ja, aber auf vorgegebener Strecke |
| **Agent** | das Modell selbst | ja, Weg und Werkzeugwahl frei |

Anthropic empfiehlt ausdrücklich, wo möglich Workflows statt Agenten zu bauen. Eine
Zusammenfassung des Artikels formuliert es als "most production LLM systems are workflows, not
agents" — diese Formulierung stammt allerdings aus einer Suchmaschinen-Zusammenfassung und wurde
nicht wörtlich im Volltext verifiziert.

## Die vier Bausteine in drei Vokabularen

| Baustein | Anthropic | OpenAI | Google |
|---|---|---|---|
| Denkeinheit | augmented LLM | Model | Model — "Brain" |
| Werkzeuge | Tools | Tools | Tools — "Hands" (Extensions, Functions, Data Stores) |
| Steuerung | Retrieval + Memory | Instructions, Guardrails, Handoffs | Orchestration Layer — "Nervous System" |
| Abbruch | stopping conditions | finale Antwort, Termination-Tool-Call, Timeout | "Final Answer" / stopping point |

**Wichtige Einschränkung:** Die Analogie "Brain / Hands / Nervous System" erscheint übereinstimmend
in mehreren unabhängigen Zusammenfassungen des Google-Whitepapers, konnte aber im direkt
abgerufenen OCR-Text der Primärquelle nicht wortwörtlich gefunden werden. Bitte im PDF als
"sinngemäß" oder "mehrfach sekundärbestätigt" kennzeichnen.

**Wörtliches Zitat zur "augmented LLM" (Anthropic):**
> "The basic building block of agentic systems is an LLM enhanced with augmentations such as
> retrieval, tools, and memory. Our current models can actively use these capabilities—generating
> their own search queries, selecting appropriate tools, and determining what information to retain."

## Der Merksatz

> **Eine Schleife ohne Werkzeuge ist nur ein Chatbot in einem `while`.**

Belegt durch: "Tools are the primary building blocks of execution for your agent"
(Claude-Agent-SDK-Blogpost).

---

## 🎨 ILLUSTRATION 1 — "Wer bestimmt den Pfad"

**Was gezeigt werden soll:** Drei horizontale Bahnen untereinander, jede zeigt denselben
Vorgang in einer anderen Systemart.

- **Bahn 1 „Chatbot":** Mensch → Modell → Antwort zurück zum Menschen. Keine Werkzeuge
  angeschlossen. Beschriftung unter der Bahn: *„Pfad: der Mensch, Zug um Zug"*.
- **Bahn 2 „Workflow":** Drei Kästen „Schritt 1 → Schritt 2 → Schritt 3" mit festen Pfeilen.
  Darunter eine gestrichelte Klammer mit „Reihenfolge steht im Code". Das Modell arbeitet *in*
  den Kästen, nicht am Pfad. Beschriftung: *„Pfad: der Code"*.
- **Bahn 3 „Agent":** Ein hervorgehobener Modell-Knoten in der Mitte, von dem Pfeile zu drei
  Werkzeugkästen gehen (Suche, Datei, Shell) — und Rückpfeile zurück. Beschriftung an den Pfeilen:
  „wählt" hin, „Ergebnis" zurück. Beschriftung: *„Pfad: das Modell"*.

**Kernaussage der Abbildung:** Die Trennlinie zwischen den drei Systemarten ist die Kontrolle
über den Pfad — nicht die Intelligenz des Modells.

---

## 🎨 ILLUSTRATION 2 — "Der Agent-Loop"

**Was gezeigt werden soll:** Ein Ring aus fünf Stationen mit Pfeilen im Uhrzeigersinn, plus einem
Ausgang, der aus dem Ring herausführt.

**Die fünf Stationen im Ring:**
1. Kontext holen
2. Denken
3. Handeln (hervorheben — dies ist die einzige Station, die nach außen wirkt)
4. Beobachten
5. Prüfen

**Der Ausgang:** Von „Handeln" führt ein andersfarbiger Pfeil mit der Beschriftung „Abbruch?" aus
dem Ring heraus zu einem Kasten: **„Abbruchbedingung — liegt im CODE, nicht im Modell."**

**In der Ringmitte:** Ein blasser Text „Das Modell steuert diese Runde".

**Seitliche Anmerkungen zu je einer Station:**
- Kontext holen → „sparsam laden, grep statt Volltext"
- Handeln → „Werkzeug ist der einzige Weg nach außen"
- Beobachten → „echtes Ergebnis zurück — ground truth"
- Prüfen → „Regel, Screenshot oder ein zweites Modell"

**Kernaussage:** Der Ring gehört dem Modell, der Ausgang nicht.

---

## Der Loop bei allen drei Anbietern

| Phase | Anthropic | OpenAI | Google |
|---|---|---|---|
| Kontext | "gather context" — Dateisystem, Suche, Subagenten, Kompaktierung | Nutzer-Input + Tool-Ergebnisse | "Question" / Observation |
| Denken | implizit im Modell | Model-Reasoning zwischen Tool-Aufrufen | "Thought" (ReAct, Chain-of-Thought, Tree-of-Thought) |
| Handeln | "take action" | Tool-Calls, ggf. Guardrail-Prüfung vorher | "Action" |
| Beobachten | Ergebnis fließt zurück als "ground truth" | Tool-Ergebnis zurück ans Modell | "Observation" |
| Verifikation | "verify work" — Regeln, visuelles Feedback, LLM-als-Richter | Guardrails (Relevance-/Safety-Classifier) | Teil der Orchestrierung |
| Abbruch | "stopping conditions (such as a maximum number of iterations)" | "model returns a final answer, a termination tool call is triggered, or an error or timeout ends the cycle" | "Final Answer" / "goal or stopping point" |

**Wörtliches Zitat zur Notwendigkeit echter Rückmeldung (Anthropic):**
> "During execution, it's crucial for the agents to gain 'ground truth' from the environment at each
> step (such as tool call results or code execution) to assess its progress."

Fehlt diese Rückmeldung, bewertet der Agent seinen eigenen Plan statt der Wirklichkeit — der
häufigste Grund für Agenten, die zufrieden Unsinn melden.

---

# TEIL 2 — Einen einzelnen Agenten bauen

## Werkzeug-Design ist Prompt-Design

Das ist der größte Einzelhebel bei Einzelagenten. Tool-Namen, Beschreibungen und Parameter sind
**Teil des Prompts**, nicht bloß API-Spezifikation.

**Belegte Wirkung:** Anthropic berichtet, dass bereits kleine Verfeinerungen der
Tool-Beschreibungen dazu führten, dass Claude Sonnet 3.5 den damaligen SWE-bench-Verified-Bestwert
erreichte — bei laut Quelle "dramatisch" reduzierter Fehlerrate.

### Die fünf Regeln

**1. Namensräume bei vielen Werkzeugen.**
Verwandte Werkzeuge über Präfix oder Suffix gruppieren: `asana_search`, `jira_search` bzw.
`asana_projects_search`, `asana_users_search`. Anthropic hat Präfix- gegen Suffix-Schema getestet
und "non-trivial effects" auf die Evaluierungsergebnisse gefunden.

**2. Semantisch eindeutige Parameter.**
`user_id` statt `user`. Semantische Bezeichner statt beliebiger UUIDs verbessern laut Anthropic
"significantly" die Trefferquote bei Retrieval-Aufgaben, weil sie Halluzinationen reduzieren.

**3. Beschreibungen wie Onboarding-Dokumentation.**
Wörtlich: *"Think of how you would describe your tool to a new hire on your team."* Implizites
Wissen muss explizit gemacht werden.

**4. Fehlermeldungen als Lernsignal.**
Fehlermeldungen sollen handlungsleitend sein statt opake Codes zu liefern — sie sollen den Agenten
zu effizienteren Strategien lenken. Beispiel: statt eines generischen Fehlercodes der Hinweis auf
das korrekte Eingabeformat, oder die Ermutigung zu gezielter statt breiter Suche.

**5. Sparsamkeit erzwingen.**
Paginierung, Kürzung und Filterung einbauen, damit der Agent nicht mit Rohdaten überflutet wird.
Werkzeuge sollen "token efficient" antworten.

### Der Test für überladene Werkzeugsätze

**Wörtliches Zitat (Anthropic):**
> "If a human engineer can't definitively say which tool should be used in a given situation, an AI
> agent can't be expected to do better."

Überlappende Werkzeugsätze sind laut Anthropic der **häufigste Konstruktionsfehler überhaupt**.

### Das Grundparadigma

Werkzeuge sind Verträge zwischen deterministischen Systemen und nicht-deterministischen Agenten.
Der Agent kann halluzinieren, den Zweck missverstehen oder falsch aufrufen — das Werkzeug-Design
muss das einkalkulieren. Anthropic nennt in diesem Zusammenhang das **Poka-Yoke-Prinzip**: Fehler
durch Design unmöglich machen, etwa indem man absolute statt relativer Dateipfade erzwingt.

Werkzeuge sollen außerdem "high leverage" sein — keine dünnen Wrapper um bestehende APIs, sondern
Werkzeuge, die die Fähigkeiten des Agenten sinnvoll erweitern.

---

## Systemprompt-Aufbau

**Strukturierung.** Prompts in klar abgegrenzte Abschnitte gliedern, per XML-Tags oder
Markdown-Überschriften: `<background_information>`, `<instructions>`, `## Tool guidance`,
`## Output description`.

**Die richtige Flughöhe.** Weder zu starre, brüchige Logik hart in den Prompt kodieren, noch zu
vage High-Level-Anweisungen ohne konkrete Signale geben. Ziel laut Anthropic: *"specific enough to
guide behavior effectively, yet flexible enough to provide strong heuristics."*

**Minimalismus.** Die minimale Informationsmenge finden, die das gewünschte Verhalten vollständig
umreißt. Mit minimalem Prompt starten, dann gezielt anhand beobachteter Fehler erweitern.

**Few-Shot-Beispiele.** Wenige, diverse, kanonische Beispiele statt einer Liste von Sonderfällen.
Anthropic: *"examples are the 'pictures' worth a thousand words."*

**Ein Warnsignal aus der Praxis:** Anthropic stellte im eigenen Team fest, dass sie ihr eigenes
Werkzeug über Systemprompts und Dokumentationsdateien **überreguliert** hatten — widersprüchliche
Anweisungen wie "leave documentation as appropriate" gegen "DO NOT add comments" zwangen das Modell,
Aufwand in Intentionsdeutung statt in die Aufgabe zu stecken.
*Belegstatus: Diese Aussage stammt aus einem Beitrag einer Anthropic-Person auf X, nicht aus einem
offiziellen Blogpost. Bitte im PDF entsprechend einordnen.*

---

## Kontextverwaltung

### "Context Rot"

Mit steigender Tokenzahl sinkt die Fähigkeit des Modells, Informationen korrekt aus dem Kontext
abzurufen. Technische Ursache: die quadratischen Paarbeziehungen der Transformer-Architektur bei
n Tokens. Modelle, die auf kürzeren Sequenzen trainiert wurden, zeigen bei langen Kontexten
reduzierte Präzision.

### Die vier Eingriffe, von sanft nach hart

**1. Tool-Ergebnisse löschen** — von Anthropic beschrieben als "one of the safest lightest touch
forms of compaction". Inhaltlich geht nichts verloren.

**2. Kompaktieren** — die Konversation nahe am Limit zusammenfassen und mit der Zusammenfassung ein
neues Kontextfenster starten. Wichtig: architektonische Entscheidungen, ungelöste Bugs und
Implementierungsdetails behalten, redundante Tool-Ausgaben verwerfen. Vorgehen laut Anthropic: den
Kompaktierungs-Prompt zunächst auf maximalen Recall trimmen (nichts Wichtiges verlieren), danach
auf Präzision iterieren.

**3. Harter Reset** — bei einer Modellgeneration (Claude Sonnet 4.5) reichte Kompaktierung nicht
aus. Das Modell zeigte "context anxiety": es brach Arbeit vorzeitig ab, weil es glaubte, das
Tokenlimit sei nah. Anthropic musste deshalb harte Context-Resets einbauen. **Bei der nächsten
Generation (Opus 4.6) verschwand das Verhalten weitgehend von selbst, und der Reset-Mechanismus
konnte wieder entfernt werden.**

**4. An Subagenten auslagern** — statt dass ein Agent den gesamten Projektkontext trägt, übernehmen
spezialisierte Subagenten fokussierte Teilaufgaben mit eigenem, sauberem Kontextfenster und liefern
nur eine kondensierte Zusammenfassung zurück, typischerweise **1.000 bis 2.000 Token**. Laut
Anthropic zeigte das "substantial improvement over single-agent systems" bei komplexen
Recherche-Aufgaben.

### Just-in-Time-Retrieval

Leichte Referenzen im Kontext halten (Dateipfade, gespeicherte Queries, Links) und Daten erst zur
Laufzeit per Werkzeug nachladen, statt alles vorab hineinzupressen. Anthropic zieht die Analogie
zur menschlichen Kognition: wir merken uns keine ganzen Textkorpora, sondern Indizes.
Abwägung: langsamer als vorab geladene Daten, und es braucht durchdachtes Engineering, damit der
Agent nicht durch Fehlnutzung der Werkzeuge Kontext verschwendet.

### Die Meta-Regel zur Harness-Komplexität

**Wörtliches Zitat (Anthropic):**
> "every component in a harness encodes an assumption about what the model can't do on its own"

Konsequenz: Komplexität nur so weit, wie das Modell sie tatsächlich braucht — und mit steigender
Modellfähigkeit Komponenten **wieder entfernen**.

---

## 🎨 ILLUSTRATION 3 — "Vier Eingriffe gegen volle Kontextfenster"

**Was gezeigt werden soll:** Eine waagerechte Achse von „sanft" nach „hart". Darüber vier Kästen
in aufsteigender Höhe (je härter der Eingriff, desto höher der Kasten — die Höhe visualisiert den
Eingriffsgrad).

1. **Tool-Ergebnisse löschen** (niedrigster Kasten) — „nichts geht inhaltlich verloren"
2. **Kompaktieren** — „Architektur und offene Bugs behalten, Rohdaten weg"
3. **Harter Reset** (in Warnfarbe) — „nötig bei context anxiety: Modell bricht ab, weil es das
   Limit für nah hält"
4. **An Subagenten auslagern** (höchster Kasten, in Signalfarbe) — „eigenes sauberes Fenster,
   zurück kommen nur 1.000–2.000 Token" + Zusatzzeile „löst das Problem, statt es zu verwalten"

**Kernaussage:** Die ersten drei verwalten das volle Fenster. Der vierte umgeht es.

---

## Wiederaufnahme nach Abbruch

Wenn ein Lauf abbricht, ist der Kontext weg. Die Lösung sind **externe, dateibasierte Artefakte**,
nicht ein größeres Kontextfenster.

**Anthropics konkretes Muster:**
- eine `claude-progress.txt` mit protokollierten Agentenaktionen
- eine JSON-Feature-Liste mit über 200 Einträgen, je markiert als "passing" oder "failing"
- Git-Commit-Nachrichten mit beschreibenden Zusammenfassungen

**Der ausdrückliche Zweck:** der Agent soll beim Wiedereinstieg nicht raten müssen — wörtlich
"eliminate agents needing to 'guess at what had happened'".

**Die drei Schritte beim Wiedereinstieg:**
1. Git-Logs und Fortschrittsdateien lesen, um den letzten Arbeitsstand zu rekonstruieren
2. auf die strukturierte Feature-Liste zugreifen, um die nächste Priorität zu bestimmen
3. ein `init.sh` ausführen, um die Entwicklungsumgebung neu zu starten

**Zusätzlich:** Jede neue Sitzung beginnt mit grundlegenden Funktionstests über den Dev-Server, um
Fehler aus der Vorsitzung zu erkennen, bevor neue Features gebaut werden. Und: Agenten nutzen Git,
um fehlerhafte Änderungen zurückzurollen.

**Beispiel für gute Fehler-Rückmeldung** (aus einem Generator-Evaluator-Aufbau): Der Evaluator
liefert konkrete, umsetzbare Fehlerbeschreibungen ohne zusätzlichen Recherchebedarf — etwa
*"Rectangle fill tool only places tiles at drag start/end points instead of filling the region"*.
Der Generator behebt das in der nächsten Iteration.

---

## Evaluation von Agenten

### Grundbegriffe
Task, Trial, Grader, Transcript, Outcome, Evaluation Harness, Agent Harness, Evaluation Suite.

### Die Prinzipien

**Früh und klein anfangen.** 20 bis 50 Aufgaben aus echten Fehlschlägen reichen zu Beginn — bei
kleinen Stichproben hat "each change to the system often has a clear, noticeable impact".

**Eindeutige Aufgabenstellungen.** So klar formulieren, dass unabhängige Fachleute zum gleichen
Pass/Fail-Urteil kommen. Referenzlösungen mitliefern, um Lösbarkeit zu belegen. Wörtlich:
*"Everything the grader checks should be clear from the task description."*

**Ausgewogene Aufgabensets.** Positive **und** negative Fälle testen — ob der Agent sucht, wenn er
soll, und ob er *nicht* sucht, wenn er nicht soll. Sonst optimiert man einseitig.

**Aus echter Nutzung schöpfen.** Manuelle Prüfungen und Fehlerberichte in Testfälle umwandeln,
priorisiert nach Nutzerauswirkung.

**Das Kernprinzip:** *"Grade what the agent produced, not the path it took."* Den Weg nicht
vorschreiben, sondern das Ergebnis bewerten, damit kreative gültige Lösungen nicht bestraft werden.
Partial Credit bei mehrteiligen Aufgaben einbauen.

**Rubric-Design:** klar, je Dimension getrennt bewerten, und dem Modell einen Ausweg geben — es
darf "Unknown" zurückgeben, statt zu halluzinieren.

**Isolierte Testumgebungen:** jeder Trial startet aus einer sauberen Umgebung. Gemeinsamer Zustand
zwischen Läufen (Restdateien, Caches, erschöpfte Ressourcen) erzeugt korrelierte Fehlschläge durch
Infrastruktur-Flakiness.

### Die drei Grader-Typen

| Typ | Was er kann | Was er nicht kann |
|---|---|---|
| **Code-basiert** (String-Match, Regex, Unit-Tests, statische Analyse, Tool-Call-Verifikation) | schnell, billig, objektiv, reproduzierbar | brüchig bei gültigen Varianten, wenig Nuance |
| **Modell-basiert** (Rubric-Scoring, Pairwise-Vergleich, referenzbasiert) | flexibel, skalierbar, erfasst Nuancen | nicht-deterministisch, teurer, braucht menschliche Kalibrierung |
| **Menschlich** (Fachprüfung, Crowd, Stichproben) | Goldstandard | teuer, langsam, begrenzt skalierbar |

**Empfehlung:** Menschen primär zur **Kalibrierung** von Modell-Gradern einsetzen, nicht für die
Breite. Und: ein *anderes* Modell zur Bewertung nutzen als das, das die Ausgabe erzeugt hat.

### pass@k gegen pass^k — die Metrik, die verwechselt wird

- `pass@k` = Wahrscheinlichkeit, dass **mindestens einer** von k Versuchen gelingt
- `pass^k` = Wahrscheinlichkeit, dass **alle** k Versuche gelingen

**Rechenbeispiel:** Bei 75 % Erfolg je Versuch liegt `pass^3` nur noch bei **rund 42 %**.

`pass@k` nutzen, wenn ein Erfolg reicht. `pass^k`, wenn Konsistenz zählt.

### Die drei Fallstricke

**1. Zu strenge Grader.** Ein Modell erreichte auf CORE-Bench zunächst nur **42 %**, weil "96.12"
gegen erwartete "96.124991…" geprüft wurde. Nach Korrektur der Prüfung: **95 %**.

**2. Eval-Sättigung.** Bei 100 % Pass-Rate zeigen Evals nur noch Regressionen, keine Verbesserungen
mehr. SWE-Bench Verified stieg von 30 % auf über 80 % und nähert sich der Sättigung.

**3. Grader-Bypass.** Ein Modell löste eine Flugbuchungsaufgabe über eine Regellücke der
Fluggesellschaft, "scheiterte" formal am geschriebenen Test — und lieferte dabei besseren
Nutzerwert. Grader müssen gegen Schummeln resistent sein.

### SMART-Kriterien für Erfolgsmetriken
Specific, Measurable, Achievable, Relevant. Beispiel: "accurate sentiment classification" statt
"good performance". Multidimensional bewerten, etwa: Task Fidelity F1 ≥ 0,85; Sicherheit 99,5 %
non-toxic; Latenz 95 % unter 200 ms.

Grundsatz: *"More questions with slightly lower signal automated grading is better than fewer
questions with high-quality human hand-graded evals."*

---

## Die Kostenmessung: dieselbe Aufgabe, drei Aufbauten

Anthropic hat eine Tetris-artige App mit drei verschiedenen Aufbauten bauen lassen:

| Aufbau | Zeit | Kosten | Ergebnis |
|---|---|---|---|
| Solo-Agent ohne Harness | 20 Minuten | 9 $ | Kernfunktion kaputt |
| Voller Harness (3 Agenten: Planner, Generator, Evaluator) | 6 Stunden | 200 $ | vollständig funktionierend |
| Vereinfachter Harness mit neuerem Modell | 3 Std. 50 Min. | 124,70 $ | funktionierend |

**Die Lehre aus Zeile 3:** Ein besseres Modell bei *weniger* Gerüst schlägt das aufwendigere Gerüst.

---

## 🎨 ILLUSTRATION 4 — "Die Kosten des Gerüsts"

**Was gezeigt werden soll:** Ein Diagramm mit zwei Achsen — waagerecht die Zeit (0 bis 6 Stunden),
senkrecht die Kosten (0 bis 200 $). Drei Punkte eintragen, jeweils mit Beschriftung und einem
kleinen Symbol für „funktioniert" oder „kaputt":

- Punkt bei (20 Min, 9 $) — mit rotem Kreuz: „Solo-Agent, Kernfunktion kaputt"
- Punkt bei (6 Std, 200 $) — mit Haken: „Voller Harness, 3 Agenten"
- Punkt bei (3:50 Std, 124,70 $) — mit Haken, hervorgehoben: „Vereinfachter Harness, neueres Modell"

Ein Pfeil vom zweiten zum dritten Punkt mit der Beschriftung: **„Besseres Modell, weniger Gerüst"**.

**Kernaussage:** Der billigste funktionierende Aufbau ist nicht der mit dem meisten Gerüst.

---

# TEIL 3 — Mehrere Agenten

## Die sechs Topologien

**Wichtige Vorbemerkung fürs PDF:** Es gibt **keine einheitliche, allgemein anerkannte Taxonomie**.
Die Benennungen schwanken je nach Quelle. Die folgende Liste ist die Schnittmenge der am häufigsten
genannten Muster.

### 1. Orchestrator-Worker / Supervisor (zentralisierter Stern)
Ein Lead-Agent plant, startet spezialisierte Subagenten parallel und sammelt Ergebnisse ein. Worker
melden an den Supervisor zurück, kommunizieren aber nicht direkt untereinander.
**Vorteil:** klare Verantwortlichkeit, einfaches Debugging.
**Nachteil:** Supervisor als Flaschenhals und Single Point of Failure.
**Verbreitung:** Produktionsstandard (AWS Bedrock Supervisor+Collaborator, LangGraph
`create_supervisor()`, CrewAI `Process.hierarchical`).

### 2. Hierarchie
Mehrere Supervisor-Ebenen. Ein Top-Koordinator zerlegt Ziele in Teilziele für mittlere Supervisoren,
die eigene Worker-Pools verwalten.
**Vorteil:** skaliert durch strukturierte Delegation.
**Nachteil:** Verzögerung durch mehrschichtige Kommunikation, kritische Ausfallpunkte an den
Kontrollknoten.

### 3. Peer-to-Peer / Swarm
Agenten reichen sich die Kontrolle direkt über Handoffs weiter, ohne zentrale Instanz.
**Vorteil:** laut Praxisvergleich schneller und mit weniger Modellaufrufen als das Supervisor-Muster,
weil der Routing-Umweg entfällt.
**Nachteil:** schwer nachvollziehbar, Deadlock-Risiko.

### 4. Blackboard
Ein gemeinsamer Speicher, den unabhängige Agenten lesen und beschreiben. Ein Agent postet eine
Anfrage, andere entscheiden selbst, ob sie beitragen können.
**Vorteil:** toleriert ausfallende Agenten, da Zwischenergebnisse persistent bleiben.
**Beleglage:** dünn — überwiegend akademische Papers, kaum Produktions-Fallstudien mit Zahlen.
Der genaue Prozentvorteil aus der Hauptquelle (arXiv 2510.01285) ließ sich aus dem PDF nicht
vollständig extrahieren.

### 5. Pipeline
Sequenzielle Verarbeitungskette (etwa ChatDevs siebenstufiges Wasserfall-Rollenmodell).
**Geeignet für:** ETL-artige, klar geordnete Teilaufgaben.
**Nachteil:** keine Parallelität.

### 6. Generator-Verifier / Debate
Ein bauender Agent, ein separater Prüf-Agent **ohne den vollen Kontextverlauf des ersten** — der
saubere Kontext ist Absicht, er ermöglicht bessere Kritik.
**Der am besten belegte Gewinn unter allen sechs Mustern.**

---

## 🎨 ILLUSTRATION 5 — "Sechs Topologien"

**Was gezeigt werden soll:** Ein 2×3-Raster kleiner Schaltbilder, jedes mit Titel und einer Zeile
Bewertung darunter.

1. **Orchestrator-Worker:** ein gefüllter Knoten oben, drei Pfeile nach unten zu drei leeren Knoten.
   Untertitel: „Produktionsstandard · Lead ist Flaschenhals"
2. **Hierarchie:** ein Knoten oben, zwei darunter, vier ganz unten — Baumstruktur.
   Untertitel: „skaliert · aber Verzögerung je Ebene"
3. **Peer-to-Peer:** fünf Knoten im Kreis, Pfeile zwischen Nachbarn, keine Zentrale.
   Untertitel: „weniger Umwege · schwer zu debuggen"
4. **Blackboard:** ein breiter Kasten „gemeinsamer Speicher" oben, drei Knoten darunter mit
   Doppelpfeilen (lesen und schreiben) zum Kasten.
   Untertitel: „überlebt Ausfälle · dünne Beleglage"
5. **Pipeline:** drei Knoten nebeneinander, einfache Pfeile in eine Richtung.
   Untertitel: „geordnet · keine Parallelität"
6. **Generator-Verifier:** zwei Kästen „baut" und „prüft", ein Pfeil hin, ein hervorgehobener
   Rückpfeil mit Beschriftung „konkreter Fehler zurück".
   Untertitel: „Prüfer OHNE den Kontext des Bauers"

**Unter dem Raster eine Zeile:** „Es gibt keine anerkannte Standard-Taxonomie — die Benennungen
schwanken je Quelle."

**Kernaussage:** Der Generator-Verifier ist der einzige, der ohne Parallelität auskommt — und der
am besten belegte Gewinn.

---

## Der berühmte Streit — und seine Auflösung

### Position A: Anthropic
Das eigene Orchestrator-Worker-System schlägt einen Einzelagenten um **+90,2 %** auf einem internen
Recherche-Eval. Kosten: **etwa 15× so viele Tokens wie ein normaler Chat** (ein Einzelagent liegt
bereits bei ~4×).

Anthropic sagt selbst: Multi-Agent lohnt sich **nur** bei "breadth-first"-Aufgaben mit hohem
wirtschaftlichem Wert, die die Mehrkosten rechtfertigen — genannt werden juristische Due Diligence,
Wettbewerbsanalyse und biomedizinische Literaturreviews. Alltägliches Frage-Antwort trägt den
15×-Multiplikator nicht.

**Ein wichtiger Nebensatz:** Der Tokenverbrauch allein erklärt **80 % der Ergebnisvarianz**. Mehr
Tokens *ist* der Wirkmechanismus, nicht die Architektur an sich.

**Und ein zweiter:** *"upgrading to Claude Sonnet 4 is a larger performance gain than doubling the
token budget on Claude Sonnet 3.7."* Modellwahl schlägt Tokenbudget.

### Position B: Cognition (das Devin-Team)
Der Blogpost "Don't Build Multi-Agents" warnt vor genau dieser Architektur: parallele Subagenten
führen zu Kontextverlust und widersprüchlichen Annahmen.

**Das Flappy-Bird-Beispiel:** Ein Subagent baut versehentlich Mario-Stil-Ästhetik, ein zweiter
erzeugt inkompatible Assets. Der finale Agent muss die widersprüchlichen Ergebnisse zusammenführen —
was die Fehler weiterträgt, statt sie zu beheben. Wörtlich beschrieben als "bird and background with
completely different visual styles".

**Cognitions zwei Prinzipien:**
1. Vollständige Agent-Traces teilen, nicht nur einzelne Nachrichten — sonst fehlt Subagenten der
   Kontext, was zu Fehlinterpretationen führt.
2. *"Actions carry implicit decisions, and conflicting decisions carry bad results."*

**Cognitions Kernthese:** *"Context engineering … is effectively the #1 job of engineers building
AI agents."*

### Die Auflösung
**Beide haben recht, für verschiedene Aufgabentypen.** Anthropic spricht von **breadth-first-
Recherche** (read-heavy, parallelisierbar). Cognition spricht von **Coding** (write-heavy, hohes
Konfliktrisiko bei parallelen Schreibzugriffen). Beide Firmen benennen selbst dieselbe
Unterscheidung als Erklärung.

**Wichtig für das PDF:** Cognition hat die Position seit Anfang 2026 **präzisiert, nicht
widerrufen**. Die aktuelle Formulierung lautet: **"writes stay single-threaded"** — mehrere Agenten
liefern Intelligenz (Review, Konsultation), aber nur einer schreibt. Ihr Code-Review-Loop findet im
Schnitt "2 bugs per PR, of which roughly 58% are severe".

Ein Cognition-Mitgründer öffentlich: *"A year ago, I'd tell people to not build multi-agents…
Today… we've found some setups that actually work."* **Wer nur den alten Blogpost zitiert, gibt eine
überholte Position wieder.**

---

## 🎨 ILLUSTRATION 6 — "Lesen parallel, schreiben einspurig" ⭐ WICHTIGSTE ABBILDUNG

**Was gezeigt werden soll:** Drei Szenarien, links eines, rechts zwei übereinander.

**Links — „Lesen, parallel":** Ein Kasten „Frage" links, davon vier Pfeile in Signalfarbe zu vier
Kästen „Quelle A" bis „Quelle D". Untertitel: *„Vier Agenten stören einander nicht — sie verändern
nichts."*

**Rechts oben — „Schreiben, gleichzeitig":** Drei Agenten-Kreise, alle mit Pfeilen in Warnfarbe auf
**ein** gemeinsames Artefakt. Über dem Artefakt ein Kollisionssymbol (ein X). Danebenstehender Text:
*„Jede Aktion trägt eine stille Entscheidung. Drei Agenten treffen drei davon — und sie
widersprechen sich."*

**Rechts unten — „Schreiben, einspurig":** Drei Agenten-Kreise, Pfeile auf **einen** hervorgehobenen
Kasten „ein Schreiber", von dort ein einzelner Pfeil auf das Artefakt. Danebenstehender Text:
*„Mehrere Agenten liefern Intelligenz. Nur einer verändert etwas."*

**Kernaussage:** Die eine Regel, aus der sich fast alle Multi-Agent-Entscheidungen ableiten lassen.

---

## 🎨 ILLUSTRATION 7 — "Der Tokenmultiplikator"

**Was gezeigt werden soll:** Ein horizontales Balkendiagramm mit drei Balken, korrekt zur Skala:

- „normaler Chat" — 1× (kurzer Balken)
- „ein Agent" — 4× (vierfache Länge)
- „Multi-Agent-System" — 15× (volle Breite, in Warnfarbe hervorgehoben)

Im längsten Balken die Beschriftung: *„für +90,2 % Ergebnisqualität auf Recherche-Aufgaben"*.

Darunter eine Zeile: *„Der Tokenverbrauch allein erklärt 80 % der Ergebnisvarianz — mehr Tokens
**ist** der Wirkmechanismus."*

**Wichtig:** Die Balkenlängen müssen dem Verhältnis 1 : 4 : 15 entsprechen. Bitte nicht optisch
„ausgleichen".

---

## Wann Multi-Agent schadet

### Die harten Zahlen

**MAST-Studie** (Cemri et al., UC Berkeley und Databricks, mit Matei Zaharia, Joseph Gonzalez,
Ion Stoica): über **1.600 annotierte Traces aus 7 Multi-Agent-Frameworks**.
- Fehlerraten: **41 bis 86,7 %**
- davon rund **37 % Koordinationsfehler** (inter-agent misalignment)
- 14 Fehlermodi in drei Kategorien: System-Design, Inter-Agent-Misalignment, Task-Verification
- Inter-Annotator-Übereinstimmung κ = 0,88 auf einer 150-Traces-Teilmenge

**Eine zweite Arbeit** (arXiv 2604.02460) findet, dass Single-Agent-LLMs Multi-Agent-Systeme bei
Multi-Hop-Reasoning **unter identischem Tokenbudget übertreffen**.

**Gartner** (Pressemitteilung Juni 2025, Umfrage unter 3.412 Organisationen): über **40 % der
Agentic-AI-Projekte werden bis Ende 2027 abgebrochen** — wegen steigender Kosten, unklarem
Geschäftswert oder unzureichenden Risikokontrollen. *Nicht spezifisch auf Multi-Agent-Topologien
bezogen, wird aber häufig in diesem Zusammenhang zitiert.*

### Anthropics eigene frühe Fehlermodi (Primärquelle, sehr konkret)
- "Spawning 50 subagents for simple queries"
- "Scouring the web endlessly for nonexistent sources"
- "Distracting each other with excessive updates"
- Doppelarbeit bei vagen Aufgabenbeschreibungen
- ein subtiler Bias, bei dem Subagenten **SEO-optimierte Seiten akademischen PDFs vorzogen** —
  **nur durch menschliches Testen gefunden**, nicht durch automatisierte Evals

### Was NICHT belastbar ist — bitte im PDF nicht als Fakt darstellen
Mehrere kursierende Einzelzahlen konnten **nicht** unabhängig verifiziert werden:
- „3,5× Kostenmultiplikator bei 4 Agenten"
- „18.000 bis 90.000 $/Monat bei Skalierung"
- „17,2-fache Fehlerverstärkung"
- „89 % der Firmen null Produktivitätsgewinn"
- „Klarna sparte 60 Mio. $ durch Multi-Agent"

Diese Zahlen stammen aus Marketing- und Sekundärartikeln, teils selbst als „hypothetisches Beispiel
mit Platzhaltern" gekennzeichnet. **Bitte weglassen oder ausdrücklich als unbelegt markieren.**

Ebenso eine populäre Aufzählung von „fünf Arten von Kontextdegradation bei Handoffs" (kausale
Begründung, implizite Constraints, Unsicherheitssignale, zeitliche Ordnung, Negativraum) — sie
stammt aus einem Marketing-Blog ohne erkennbare eigene Studie. Plausibel, aber nicht zitierfähig.

---

# TEIL 4 — Schleifen und Selbstkritik

## Selbstkorrektur funktioniert nicht zuverlässig

**Wörtliches Zitat (Huang et al., arXiv:2310.01798, ICLR 2024):**
> "LLMs struggle to self-correct their responses without external feedback, and at times, their
> performance even degrades after self-correction."

## Warum — das Problem liegt im Finden, nicht im Beheben

**Wörtliches Zitat (arXiv:2311.08516, ACL Findings 2024):**
> "LLMs cannot find reasoning errors, but can correct them given the error location"

Schwache Modelle finden eigene Fehler kaum. Ein separat trainierter kleiner Klassifikator findet
Fehler **besser** als das große Modell beim reinen Prompting.

**Die strukturelle Begründung:** *"when generator and evaluator share failure modes, self-evaluation
provides weak evidence of correctness."* Sind Erzeuger und Prüfer dasselbe Modell, teilen sie
dieselben systematischen Schwächen — die Selbstbewertung ist dann kein Korrektheitssignal.

## Self-Preference-Bias

**arXiv:2410.21819:** Bewerter-LLMs bevorzugen **systematisch eigene Outputs**. Der Effekt
korreliert linear mit der Selbst-Erkennungsgenauigkeit und verstärkt sich durch Fine-Tuning auf
Selbst-Diskriminierung.
**Gegenmittel:** langes Chain-of-Thought vor dem Urteil reduziert den Bias.

## Ein dedizierter Kritiker schlägt Selbstkritik — meistens

**Beleg dafür** (arXiv:2503.16024, "The Lighthouse of Language"): ein spezialisiertes
Critic-Modell erreicht **+46,03 %** (Llama-3-70B) bzw. **+42,89 %** (Llama-3-8B) gegenüber
Diskriminator- und Selbstkritik-Baselines.

**Gegenbeleg, ehrlich zu nennen** (arXiv:2509.00676, "LLaVA-Critic-R1"): dort erzielt
selbstkritik-basiertes Test-Time-Scaling die höchste Leistung. Beide sind aktuelle Preprints mit
unterschiedlichen Domänen (Text-Agenten gegen multimodale Policy-Modelle). **Die Diskrepanz ist
nicht aufgelöst und vermutlich stark aufgaben- und architekturabhängig.**

## Reflexion — wie es richtig gebaut ist

Reflexion (arXiv:2303.11366) formalisiert den Loop: Actor erzeugt eine Trajektorie → Evaluator
bewertet → ein Self-Reflection-Modell fasst die Trajektorie verbal zusammen und legt sie in einen
episodischen Speicher → der nächste Versuch nutzt diesen Kontext.

**Entscheidend:** Der Loop läuft, *"until the Evaluator deems the trajectory to be correct"* — das
Abbruchkriterium ist ausdrücklich ein **externer Evaluator**, nicht das Modell, das sich selbst für
fertig erklärt.

**Ergebnisse:** +22 Prozentpunkte absolut auf AlfWorld über 12 iterative Lernschritte; 91 % pass@1
auf HumanEval.
**Lücke:** Die Suchergebnisse enthalten keine expliziten Angaben zu einem Leistungsplateau nach
einer bestimmten Rundenzahl.

---

## 🎨 ILLUSTRATION 8 — "Selbstkritik gegen getrennte Prüfung"

**Was gezeigt werden soll:** Zwei Hälften, durch eine senkrechte Linie getrennt.

**Links „Selbstkritik":** Zwei stark überlappende Kreise (Venn-Diagramm), beide in Warnfarbe
schraffiert. Der linke Kreis beschriftet „erzeugt", der rechte „prüft". In der Überlappung, groß:
**„gleiche blinde Flecken"**. Untertitel: *„Dasselbe Modell in beiden Rollen. Was es nicht sieht,
sieht es auch beim zweiten Hinsehen nicht."*

**Rechts „Getrennte Prüfung":** Zwei **getrennte**, nicht überlappende Kreise. Links „Generator —
baut" (neutral), rechts „Verifier — Test · Kritiker · Klassifikator" (in Signalfarbe gefüllt).
Zwischen ihnen zwei Pfeile: oben „Ergebnis" nach rechts, unten „Fehlerort" nach links.
Untertitel: *„Andere Fehlermodi, sauberer Kontext. Das Modell kann Fehler gut beheben — es findet
sie nur selbst nicht."*

**Kernaussage:** Die Trennung ist der Hebel, nicht mehr Runden desselben Modells.

---

## Endlosschleifen sind häufig, nicht selten

**Studie:** "When Agents Do Not Stop: Uncovering Infinite Agentic Loops in LLM Agents"
(arXiv 2607.01641). **68 bestätigte Endlosschleifen-Fehler in 47 Projekten** aus 6.549 durchsuchten
Repositories, bei 91,9 % Precision der Analyse.

### Ursachenverteilung (Mehrfachnennung möglich)
| Ursache | Anteil |
|---|---|
| Tool-gesteuerte Retries ohne Grenze | 41,2 % |
| Modellgesteuerte Terminierung ohne harten Fallback | 38,2 % |
| Fehlende Exits | 33,8 % |
| Ungeprüfte Workflow-Zyklen | 30,9 % |

### Verteilung nach Framework
LangGraph 33,8 % · AutoGen 32,4 % · LlamaIndex 8,8 % · LangChain 7,4 %
(die beiden ersten machen zusammen rund zwei Drittel aller Fälle aus)

### Folgen
API-Kosten-Exzess: **95,6 %** der Fälle · faktischer Denial-of-Service: **95,6 %** ·
Context-Window-Erschöpfung: **27,9 %**

---

## 🎨 ILLUSTRATION 9 — "Ursachen von Endlosschleifen"

**Was gezeigt werden soll:** Ein horizontales Balkendiagramm mit vier Balken, korrekt zur Skala
(0 bis 45 %), Achsenbeschriftung mit Teilstrichen bei 0, 20 % und 40 %.

- Tool-Retries ohne Grenze — 41,2 % (in Warnfarbe)
- Modell entscheidet den Abbruch — 38,2 % (in Warnfarbe)
- Fehlende Exits — 33,8 % (neutral)
- Ungeprüfte Workflow-Zyklen — 30,9 % (neutral)

**Unter dem Diagramm:** *„Folgen: Kostenexzess und faktischer Denial-of-Service in je 95,6 % der
Fälle · Kontextfenster erschöpft in 27,9 %."*

**Bildunterschrift-Vorschlag:** „Die beiden oberen Ursachen sind dieselbe Wurzel: In beiden Fällen
entscheidet etwas über den Abbruch, das nicht deterministisch ist."

---

## Die Regel für Abbruchkriterien

> **Das Abbruchkriterium gehört deterministisch in den Code, nie ins Modell.**

**Die konkreten Anforderungen:**
- Stop-Bedingungen **binär und messbar** formulieren: „Test-Coverage > 80 %", „JSON validiert".
  Nie subjektiv: „gut genug", „umfassend genug".
- **Immer zusätzlich** eine harte `max_iterations`-Grenze als Netz. Praxis-Default 10 bis 15;
  LangChain-Default liegt bei 15.
- Die Grenze **nicht reflexhaft erhöhen**, sondern erst prüfen, ob in den vorherigen Iterationen
  echter Fortschritt stattfand.
- Tool-Call-Limiter: etwa maximal 10 gleiche Aufrufe in Folge, 50 gesamt. Plus Timeout je Ausführung.
- Das Limit muss unabhängig vom Modell-Output greifen — wörtlich: *"force return to END no matter
  what the LLM wants"*.
- Bewertung und Ausführung trennen (kein Self-Assessment).

**Die Begründung, warum Modelle schlechte Abbrecher sind:** LLMs wollen tendenziell hilfsbereit
klingen, und „lass uns hier aufhören" wirkt nicht wie eine hilfreiche Antwort.

**Konvergenz-Heuristik aus der Praxis:** Embedding-Ähnlichkeit über etwa 0,92 zwischen
aufeinanderfolgenden Ausgaben, kombiniert mit der Prüfung „die Kritik enthält keine neuen Punkte
mehr". Zusätzlich: Budget-Deckel mit Kill-Switch, Deadlock-Breaker über ein gleitendes Fenster
wiederkehrender Feedback-Muster, und Überwachung der Verbesserungsrate pro Runde.
**Der finale Kill-Switch muss deterministischer Code sein, nie ein Modellurteil.**

**Bounded-Recovery-Protokoll** bei Plan-Execute-Replan (arXiv 2603.11445, "VMAO"): fünf Phasen
(Plan, Execute, Verify, Replan, Synthesize) mit der Regel: erst Retry, dann lokaler Patch, erst
danach vollständiges Replanning. Das verhindert das Muster, dass ein Modell wiederholt neu plant,
ohne Fortschritt zu machen.

## Wie viele Runden lohnen sich?

| Verfahren | Konvergenz | Abbruchkriterium |
|---|---|---|
| **Self-Refine** | Plateau bei 2–4 Runden ⚠️ | Embedding-Ähnlichkeit ~0,92 **und** keine neuen Kritikpunkte |
| **Multi-Agent-Debate** | 4–8 Runden | Kolmogorov-Smirnov-Test zwischen Antwortverteilungen, Schwelle < 0,05 über zwei Runden |
| **Plan-Execute-Replan** | — | Bounded Recovery (Retry → Patch → Replan) |
| **Reflexion** | — | ein **externer** Evaluator erklärt die Trajektorie für korrekt |

⚠️ **Wichtige Einschränkung zur Zeile Self-Refine:** Die Angabe „2–4 Runden" stammt aus
Sekundärquellen (Medium, EmergentMind). Das arXiv-Original (2303.17651) ließ sich nur als Abstract
abrufen — die Tabellen mit Leistung pro Iteration waren nicht extrahierbar. Die Zahl ist
aufgabenabhängig und sollte **nicht als feste Konstante** dargestellt werden.

**Zum Debate-Kriterium:** Das adaptive Verfahren spart Rechenzeit bei einem Genauigkeitsverlust von
nur **−0,13 % bis 0,00 %** gegenüber fixen zehn Runden.

**Keine Quelle liefert eine einheitliche Konvergenzformel** über alle vier Verfahren hinweg.

## Debate läuft in die Echo-Kammer

**Wörtliches Zitat:**
> "Multi-LLM debates are susceptible to 'echo chamber' effects, where a flawed majority opinion
> arising from shared misconceptions can suppress correct minority viewpoints."

Bereits ein moderater Anfangs-Bias kann sich durch die Interaktion verstärken (arXiv 2608.02827,
"Emergence of Biased Consensus in Multi-Agent LLM Debates").

**Der wirksame Hebel:**
> "Intrinsic reasoning strength and group diversity are the dominant drivers of debate success,
> while structural parameters such as order or confidence visibility offer limited gains."

Praktisch: **verschiedene Modelle oder verschiedene Blickwinkel einsetzen**, nicht dasselbe Modell
mehrfach befragen. Diversität schlägt Rundenzahl.

---

# TEIL 5 — Schwärme und Parallelität

## Das umgekehrte U

Die Leistung steigt zunächst mit der Agentenzahl (Synergie, gegenseitige Fehlerkorrektur), sinkt
aber ab einem Optimum wieder, weil der Koordinations-Overhead dominiert. **Tokenkosten skalieren
dabei quadratisch mit der Agentenzahl.**

| Aufgabentyp | Sinnvolle Parallelität | Beleg |
|---|---|---|
| Reasoning-lastig (z. B. abstrakte Algebra) | **2–4 Agenten**, praktisch bis 6 | arXiv 2606.00655 |
| Breit zerlegbare Recherche | 3–5 pro Welle, insgesamt >10 | Anthropic |
| Massiv-paralleler Fan-out | 100–300 möglich ⚠️ | nur Sekundärquelle |

**Konkrete Messung:** Bei reasoning-lastigen Aufgaben fällt die Genauigkeit bei 8 Agenten gegenüber
1 Agent um bis zu **27,45 %**.

**Andere Ensemble-Studien** finden je nach Setup unterschiedliche Sweet Spots: manche berichten ein
brauchbares Optimum bei 10 bis 40 Ensemble-Mitgliedern, andere sehen bereits ab n ≥ 3 abnehmenden
Grenznutzen und negative Effekte jenseits von n = 5. **Die Studien widersprechen sich in der
konkreten Zahl, stimmen aber im Muster überein: abnehmende oder negative Erträge oberhalb eines
kleinen n.**

**Der Kernsatz aus der Literatur:**
> Kollektive Intelligenz ist "an emergent property of interaction architecture, not an automatic
> outcome of agent plurality".

Mehr Agenten helfen **nur**, wenn Aufgabenzerlegung, Aggregation und Fehlerbehandlung ausdrücklich
dafür gebaut sind.

⚠️ **Zur Zeile „100–300 Agenten":** Alle Zahlen zum massiv-parallelen Fall (300 Agenten, 4.000
Steps, 2,4 Mio. Tokens, ~7,20 $ pro Lauf, 75–83 % Caching-Ersparnis) stammen aus einem
Drittanbieter-Guide, nicht aus offizieller Herstellerdokumentation. Bitte als unverifiziert
kennzeichnen oder weglassen.

---

## 🎨 ILLUSTRATION 10 — "Das umgekehrte U" ⭐ SCHLÜSSELABBILDUNG

**Was gezeigt werden soll:** Ein Liniendiagramm mit zwei Kurven.

**Achsen:** waagerecht „Zahl der Agenten" (Teilstriche bei 1, 2, 4, 6, 8, 10), senkrecht
„Leistung / Kosten".

**Kurve 1 — Leistung** (durchgezogen, Signalfarbe): steigt von links an, erreicht ihren Höhepunkt
zwischen 2 und 4 Agenten, fällt danach spürbar ab. Am Höhepunkt ein markierter Punkt mit der
Beschriftung „Höhepunkt".

**Kurve 2 — Kosten** (gestrichelt, Warnfarbe): steigt durchgehend und beschleunigt (quadratisch).
Beschriftung am rechten Ende: „Kosten — quadratisch".

**Farbig hinterlegtes Band** über den Bereich 2 bis 6 Agenten, darüber die Beschriftung
„OPTIMUM 2–6".

**Annotation bei x = 8:** eine Hilfslinie mit dem Text „bis −27 % Genauigkeit bei 8 Agenten
(reasoning-lastige Aufgaben)".

**Unter dem Diagramm:** der Kernsatz zur kollektiven Intelligenz.

**Kernaussage:** Der Schnittpunkt der beiden Kurven ist die eigentliche Entscheidung.

---

## Wellen gegen kontinuierliches Nachziehen

**Ein überraschender Befund:** Anthropic fährt **synchrone Wellen von 3 bis 5 Subagenten** und
wartet, bis eine Welle fertig ist, bevor es weitergeht.

**Wörtliches Zitat:**
> "Currently, our lead agents execute subagents synchronously, waiting for each set of subagents to
> complete before proceeding"

Anthropic benennt das **selbst als Limitation** des eigenen Systems.

**Innerhalb einer Welle** nutzt ein einzelner Subagent bis zu 3+ Werkzeuge parallel — das brachte in
Summe bis zu **90 % Zeitersparnis** bei komplexen Recherchen gegenüber sequenzieller Abarbeitung.

**Wichtiger Begriffshinweis fürs PDF:** Der Ausdruck „Continuous-Spawning" ist **kein etablierter
Fachbegriff**. Die Literatur spricht von synchronen Wellen gegenüber asynchronem oder
kontinuierlichem Fan-out. Das Verfahren selbst ist zeitlich überlegen und leicht im Code zu
erzwingen — nur die Bezeichnung ist Hauswortschatz.

---

## 🎨 ILLUSTRATION 11 — "Wellen gegen kontinuierliches Nachziehen"

**Was gezeigt werden soll:** Zwei Zeitleisten untereinander, beide mit derselben waagerechten
Zeitachse, damit man die Gesamtdauer direkt vergleichen kann.

**Oben „Wellen — mit Barriere":** Drei Balkenzeilen (drei Agenten). In der ersten Welle haben die
drei Balken **unterschiedliche Längen**. Nach dem längsten steht eine senkrechte Linie „Barriere".
Die Lücken zwischen den kürzeren Balken und der Barriere sind **schraffiert** als Leerlauf
markiert. Danach die zweite Welle, wieder mit unterschiedlichen Längen und wieder Leerlauf.
Untertitel: *„Schraffiert = Leerlauf. Alle warten auf den langsamsten der Welle."*

**Unten „Kontinuierlich nachziehen":** Dieselben drei Zeilen, aber die Balken schließen **nahtlos
aneinander** an, keine Schraffur. Die Gesamtdauer endet **sichtbar früher** als oben — die
Endlinie ist markiert mit „früher fertig".
Untertitel: *„Einer fertig → sofort der nächste. Konstant so viele wie erlaubt, keine Lücken."*

**Kernaussage:** Der Zeitgewinn entsteht durch die weggefallenen Wartezeiten, nicht durch schnellere
Agenten.

---

## Aufgabenzerlegung

Anthropic bettet **Skalierungsregeln direkt in den Prompt des Lead-Agenten** ein:

| Aufgabenart | Agenten | Tool-Calls je Agent |
|---|---|---|
| Einfache Faktensuche | 1 | 3–10 |
| Vergleiche | 2–4 | 10–15 |
| Komplexe Recherche | >10 | klar getrennte Zuständigkeiten |

**Warum das nötig ist:** Frühe Versionen mit vagen Anweisungen wie „research the semiconductor
shortage" führten zu **Duplikation** — mehrere Agenten untersuchten dieselben Zeiträume, statt die
Arbeit sauber zu teilen.

**Allgemeines Muster:** Ohne zentrale Aufgaben-Registry greifen mehrere Agenten unabhängig dieselbe
Aufgabe auf und produzieren widersprüchliche Ergebnisse — eine klassische Race Condition. Eine
Planner-Worker-Zerlegung mit expliziter Zuweisung beseitigt das an der Quelle.

## Kontext-Isolation

In Claude Code bekommt jeder Subagent ein **komplett isoliertes Kontextfenster**: keine
Chat-Historie, kein Skill-Memory, kein Datei-Cache der Hauptsitzung. Er startet nur mit
System-Prompt, Aufgabenbeschreibung, der CLAUDE.md-Hierarchie, einem Git-Status-Schnappschuss und
vorgeladenen Skills. **Ausnahme:** ein „Fork"-Subagent erbt die gesamte Elternkonversation.

Anthropics Begründung für getrennte Fenster: sie schaffen "mehr Kapazität für paralleles Reasoning"
— die Grundidee ist, genug Tokens zu verbrauchen, um das Problem zu lösen, verteilt über mehrere
Fenster statt eines einzigen begrenzten.

## Aggregation und Deduplizierung

Der Lead-Agent empfängt alle Befunde und entscheidet, ob weitere Recherche nötig ist. Ein
spezialisierter **CitationAgent** verarbeitet die Ergebnisse danach separat, um Quellenverweise
korrekt zuzuordnen.

**Eine ausdrückliche Lücke:** Der Originalartikel nennt **keinen konkreten technischen
Deduplizierungs-Mechanismus** — nur, dass unpräzise Aufgabenbeschreibungen zu Duplikation, Lücken
oder fehlender Information führen.

**Eine Warnung aus der Dokumentation:** Das Aggregieren vieler Subagent-Ergebnisse kann erheblich
Kontext der Hauptkonversation verbrauchen. Empfehlung: Zusammenfassungen knapp halten.

**Aggregationsmuster aus der allgemeinen Literatur:** Redundanz-Abgleich (mehrere Prompts prüfen
dasselbe, Flag nur bei Übereinstimmung), Mehrheitsentscheid, gewichtetes Merging bei bewerteten
Ausgaben.

## Umgang mit fehlgeschlagenen Agenten

**Anthropics Ansatz:** Retry-Logik und regelmäßige Checkpoints statt Neustart von vorn — Agenten
setzen "from where the errors occurred" fort.

Zusätzlich hat sich bewährt, dem Agenten schlicht mitzuteilen, dass ein Werkzeug fehlschlägt, und
ihn selbst adaptieren zu lassen. Wörtlich: *"letting the agent know when a tool is failing and
letting it adapt works surprisingly well."*

**Eine klare Lücke:** Keine der gefundenen Primärquellen nennt eine konkrete Fehlerrate für einzelne
ausgefallene Agenten in einem Schwarm. Nur qualitative Aussagen zu Retry, Checkpoint und
Reassignment.

## Konkrete Leitplanken in Claude Code

| Grenze | Wert | Umgebungsvariable |
|---|---|---|
| Gleichzeitige Subagenten | 20 | `CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS` |
| Verschachtelungstiefe | 3 | `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` |
| Gesamtzahl je Sitzung | kein Limit (ein früherer 200er-Deckel wurde wieder entfernt) | — |

Zur Kostenkontrolle gibt es `--max-budget-usd`. Kostensenkung über billigere Modelle je Subagent
(`model: haiku` im Frontmatter) oder pauschal über `CLAUDE_CODE_SUBAGENT_MODEL`.

Transkripte persistieren unter `~/.claude/projects/{project}/{sessionId}/subagents/` und dienen
de facto als Audit-Trail.

---

# TEIL 6 — Über Modellgrenzen hinweg

## Die Ausgangsfrage

„Kann ein Agent bei Anthropic mit einem Agenten bei OpenAI arbeiten — habe ich die dann angestellt?"

**Die kurze Antwort:** Nein. Man stellt niemanden an. **Man legt einen gemeinsamen Arbeitsplatz an.**

## Zwei Achsen, die ständig verwechselt werden

```
        MCP  (vertikal)                A2A  (horizontal)
    Agent ──→ Werkzeuge/Daten      Agent ←──→ Agent
```

- **MCP (Model Context Protocol)** verbindet einen Agenten **nach unten** mit Werkzeugen, Daten und
  APIs.
- **A2A (Agent2Agent)** verbindet ihn **zur Seite** mit anderen, unabhängig gehosteten Agenten.

Sie sind **komplementär, nicht konkurrierend.**

## MCP — Reifegrad und Trägerschaft

**Reifegrad: etabliert.** Aktuelle Spezifikation **2026-07-28**.

**Wesentliche Änderungen gegenüber der Version 2025-11-25:**
- zustandsloser Protokollkern — der `initialize` / `notifications/initialized`-Handshake entfiel
- protokollseitige Sessions und der `Mcp-Session-Id`-Header aus Streamable HTTP entfernt
- Multi-Round-Trip-Requests, header-basiertes Routing, cachebare List-Ergebnisse
- verhärtete Autorisierung, näher an OAuth/OIDC
- formales Extensions-Framework, u. a. „MCP Apps" für server-gerenderte UIs und eine
  „Tasks"-Extension für langlaufende Jobs
- formale Deprecation-Policy

**Trägerschaft:** Anthropic hat MCP am **09.12.2025** an die neu gegründete **Agentic AI Foundation
(AAIF)** unter dem Dach der Linux Foundation gespendet.
- Gründungsmitglieder (Platinum): **Anthropic, Block, OpenAI**
- Unterstützt von: Google, Microsoft, AWS, Cloudflare, Bloomberg
- Stand April 2026: 170+ Mitglieder
- Eingebracht wurde: MCP von Anthropic, „goose" von Block, **AGENTS.md von OpenAI**

**Verbreitung:** über 10.000 aktive öffentliche MCP-Server. Genutzt von ChatGPT, Cursor, Gemini,
Microsoft Copilot, VS Code. 97+ Mio. monatliche SDK-Downloads zum Zeitpunkt der Spende, 110+ Mio.
laut Stand April 2026.

**Governance nach der Spende:** laut Anthropic unverändert („Community-Input und transparente
Entscheidungsfindung"). Anthropic bleibt aktiver Contributor, gibt aber die Alleinkontrolle ab.

**Hinweis zu AGENTS.md:** Das ist ein **Konventions- und Kontextstandard, kein
Laufzeit-Kommunikationsprotokoll**. Es gehört nicht in dieselbe Kategorie wie MCP/A2A, wird aber
oft in einem Atemzug genannt, weil es Teil derselben AAIF-Gründung ist. Verbreitung seit August
2025: über 60.000 Open-Source-Projekte, u. a. GitHub Copilot, VS Code, Cursor, Gemini CLI.

## A2A — Reifegrad und die entscheidende Lücke

**Reifegrad: im Übergang von Entwurf zu Etablierung.** Von Google im April 2025 gestartet, bereits
im **Juni 2025** an die Linux Foundation übergeben — aber als **eigenes A2A-Projekt, nicht als Teil
der AAIF**.

- Version 1.0 als erste stabile Spezifikation
- Eine Sekundärquelle nennt Version 1.0.1 ab Mai 2026 mit einem Extension-Mechanismus für neue
  Daten, Anforderungen, RPC-Methoden und Zustandsautomaten ⚠️ *nicht primärquellenverifiziert*
- 150+ Organisationen, 22.000+ GitHub-Sterne
- SDKs in Python, JavaScript, Java, Go und .NET

**⚠️ DER ENTSCHEIDENDE VORBEHALT:** In der Trägerliste der Linux-Foundation-Pressemeldung vom
09.04.2026 werden genannt: **AWS, Cisco, Google, IBM, Microsoft, Salesforce, SAP, ServiceNow.**
**Anthropic wird in dieser Liste nicht genannt** — obwohl Anthropic bei der AAIF (MCP)
Gründungsmitglied ist.

**Konsequenz:** A2A als Draht zwischen einem Anthropic-Agenten und einem OpenAI- oder
Google-Agenten ist **nicht durch Anthropic-Beteiligung abgesichert**. Es müsste über allgemeine
Agent-Card- und HTTP-Kompatibilität funktionieren. Ob Anthropic A2A informell unterstützt, ließ sich
mit den Quellen vom 09.09.2026 **nicht klären** — das ist eine echte offene Frage und sollte im PDF
so stehen.

**Eine kritische Einordnung** (Sekundärquelle, Meinungsäußerung): Ein unabhängiger Autor stuft
Sicherheit als „die größte ungelöste Frage" ein und warnt davor, Logo-Unterstützung mit echter
Produktionsnutzung zu verwechseln — das eigentliche Erfolgskriterium sei „production retention after
the first real operational incident".

## ACP — die Namensfalle

**Es gibt zwei verschiedene Protokolle mit demselben Kürzel:**

| Kürzel | Voller Name | Träger | Zweck |
|---|---|---|---|
| ACP | Agent **Client** Protocol | Zed Industries | Coding-Agent ↔ **Editor** |
| ACP | Agent **Communication** Protocol | IBM / BeeAI | Agent ↔ **Agent** |

**Zeds ACP:** JSON-RPC 2.0 über stdin/stdout, veröffentlicht August 2025. Adoption durch JetBrains,
Google, GitHub, 25+ Agenten (Stand März 2026), gemeinsame Registry mit JetBrains seit Januar 2026.

**Die Falle in freier Wildbahn:** Ein arXiv-Governance-Paper (2606.31498) nennt „ACP" neben MCP und
A2A als Agent-Interoperabilitätsprotokoll — **ohne zu spezifizieren, welches gemeint ist**. Inhaltlich
passt es eher zum IBM/BeeAI-ACP, aber das ließ sich nicht sicher auflösen.

## Der Governance-Befund über alle Protokolle

Das Paper "Governance Gaps in Agent Interoperability Protocols" (Kang & Diponegoro, 30.06.2026,
arXiv 2606.31498) untersucht **MCP, A2A, ACP, ANP und ERC-8004** entlang einer sechsdimensionalen
Taxonomie: Mitgliedschaft, Deliberation, Abstimmung, Dissens-Erhalt, menschliche Eskalation,
Audit/Replay.

**Zentrales Ergebnis:** **Abstimmung und Dissens-Erhaltung fehlen bei ALLEN fünf untersuchten
Protokollen vollständig.** Deliberation ist abwesend oder bestenfalls teilweise vorhanden.

**In einem Satz:** Agenten können sich Aufgaben zuwerfen — sich einigen können sie nicht.

## OpenAI-Handoffs sind kein Cross-Vendor-Weg

Handoffs im OpenAI Agents SDK delegieren zwischen spezialisierten Agenten **innerhalb desselben
Runs und desselben Frameworks**. Beispiel aus der Doku: eine Kundenservice-App mit getrennten
Agenten für Bestellstatus, Rückerstattung und FAQ. Technisch werden sie dem Modell als Werkzeug
präsentiert, etwa `transfer_to_refund_agent`.

**Die dokumentierten Grenzen:**
- bleiben "within a single run"
- linear (A→B→C), keine nativen Parallel- oder Rückführungsmuster ohne manuelles Modellieren
- Input-Guardrails gelten nur für den ersten Agenten, Output-Guardrails nur für den letzten
- „Nested handoff history" ist Opt-in-Beta, standardmäßig deaktiviert
- **keine dokumentierte Aussage zur Interoperabilität mit Claude- oder Gemini-Agenten**

**Anthropics OpenAI-SDK-Kompatibilitätsschicht** für die Claude API existiert, ist laut
Sekundärquelle aber primär zum Testen und Vergleichen von Modellfähigkeiten gedacht und **nicht als
produktionsreife Langzeitlösung** empfohlen.

---

## 🎨 ILLUSTRATION 12 — "Zwei Achsen" ⭐ SCHLÜSSELABBILDUNG

**Was gezeigt werden soll:** Ein Kreuz-Diagramm mit einem Agenten in der Mitte.

**Mitte:** Ein Kasten „dein Agent", darunter kleiner „Claude Code".

**Nach unten (MCP, Signalfarbe):** Ein kräftiger Pfeil nach unten zu einem Kasten
„Werkzeuge · Daten · APIs". Am Pfeil die Beschriftung „MCP — vertikal". Im Zielkasten klein:
„Spec 2026-07-28 · etabliert".

**Nach rechts (A2A, Warnfarbe):** Ein Pfeil nach rechts zu einem **gestrichelt umrandeten** Kasten
„fremder Agent (GPT · Gemini)". Am Pfeil „A2A — horizontal". Die gestrichelte Umrandung
signalisiert: nicht abgesichert.

**Links oben — ein Infokasten in Signalfarbe:**
„Agentic AI Foundation — Anthropic · OpenAI · Block, seit 09.12.2025. 110+ Mio. SDK-Downloads/Monat"
Mit einer feinen Verbindungslinie zum MCP-Pfeil.

**Rechts oben — ein Infokasten in Warnfarbe:**
„Linux Foundation, eigenes Projekt — AWS · Cisco · Google · IBM · Microsoft · SAP …
**Anthropic fehlt in dieser Liste**"
Mit einer feinen Verbindungslinie zum A2A-Pfeil.

**Unter allem eine Zeile:** *„MCP ist reif, löst aber die falsche Achse. A2A ist die richtige Achse —
ohne erkennbare Anthropic-Beteiligung."*

**Kernaussage:** Die reife Schiene führt nicht dorthin, wo man hin will.

---

## Der Weg, der 2026 wirklich trägt

Kein Protokoll, sondern **ein gemeinsames Dateisystem mit klaren Regeln.**

### Die vier Bausteine

**1. Atomic File Claiming.**
`os.open(pfad, os.O_CREAT | os.O_EXCL | os.O_WRONLY)` — nur **ein** Prozess kann die Datei exklusiv
anlegen. Das garantiert Exactly-once-Verarbeitung ohne Lock-Datei-Gebastel.

**2. Deduplizierung über Output-Existenzcheck.**
Vor der Verarbeitung prüfen, ob die Ausgabedatei schon existiert. Macht den Schritt idempotent und
überlebt Doppelaufrufe.

**3. Persistentes State-Ledger — nur als Nachweis.**
Eine Liste bereits verarbeiteter Einheiten auf gemeinsamem Speicher. **Die Autoren des Musters
warnen ausdrücklich, dass dies bei gleichzeitiger Skalierung ohne atomare Claims bricht.**

**4. Verzeichnisstruktur als Vertrag.**
Inbox/Outbox oder Phasen-Ordner, dazu ein fortlaufendes Statusdokument. Flach und offensichtlich
halten. Pfade wie eine API behandeln und dokumentieren.

### ⚠️ Begriffswarnung „Ledger"
Es gibt **keinen Industriestandard**. Microsoft/AutoGen meint damit Task-Ledger und Progress-Ledger,
ein Nischenprojekt eine `.uai/intake-outcome-ledger.uai`, andere schlicht ein `STATE.md`. Ein
prominenter Artikel mit dem Titel „File-Based AI Coordination Pattern" behandelt Locking und
Konflikte **gar nicht**. Wer „Ledger-Datei" sagt, muss dazusagen, was er meint.

---

## 🎨 ILLUSTRATION 13 — "Wettlauf gegen atomaren Claim" ⭐ SCHLÜSSELABBILDUNG

**Was gezeigt werden soll:** Zwei Hälften nebeneinander, die denselben Ablauf mit unterschiedlichem
Ausgang zeigen.

**Links „Nur Ledger — Wettlauf":**
- Agent 1 oben links, Agent 2 unten links
- Beide Pfeile zeigen auf einen Kasten „Ledger — Aufgabe 7: frei" in der Mitte
- An beiden Pfeilen steht „liest: frei"
- Von der Ledger-Position gehen **zwei** Pfeile nach unten in einen Kasten in Warnfarbe:
  **„beide bearbeiten sie"**
- Seitlich: *„Zwischen Lesen und Eintragen liegt ein Fenster."*

**Rechts „Atomarer Claim":**
- Agent 1 oben links, Agent 2 unten links
- Beide Pfeile zeigen auf einen Kasten `O_CREAT | O_EXCL` — „nur einer darf anlegen",
  darunter klein `task-7.claim`
- Der Pfeil von Agent 1 ist **hervorgehoben** und beschriftet „gewinnt"
- Der Pfeil von Agent 2 ist **neutral** und beschriftet „EEXIST → überspringt"
- Von dort **ein** Pfeil nach unten in einen gefüllten Kasten:
  **„genau einer bearbeitet sie"**

**Kernaussage:** Das Ledger ist der Nachweis, nicht die Koordination.

---

## Heterogene CLI-Agenten zusammenschalten

**Das dokumentierte Muster ist der Headless-Subprocess.** Das Claude Agent SDK spawnt selbst einen
`claude`-CLI-Prozess und spricht per stdin/stdout im JSON-Lines-Protokoll mit ihm. Für fremde
CLI-Agenten gilt dasselbe: im Headless-Modus aufrufen (`claude -p`, `codex exec`) und strukturierte
Ausgabe parsen (`--output-format json`).

**Wichtig:** Es gibt **keinen offiziellen Anthropic-Adapter**, der fremde CLI-Agenten nativ einbindet.

**Das konkreteste Vorbild** ist ein Community-Orchestrator namens NEEDLE: Er verteilt Aufgaben aus
einer **SQLite-Queue mit atomaren Claims** an headless CLI-Backends — ausdrücklich genannt: Claude
Code, Codex, OpenCode und Aider. Fortschritt läuft über eine explizite State Machine, und
**bewusst gibt es keinen Laufzeit-Kommunikationskanal zwischen den Agenten**: die Koordination
passiert vorher, bei der Aufgabenzerlegung.
⚠️ *Die Details zu NEEDLE stammen aus der Zusammenfassung eines kuratierten Meta-Repos, nicht aus
dem Projekt-Repository selbst.*

**Umgekehrt:** `claude mcp serve` exponiert die Werkzeuge von Claude Code über MCP, sodass andere
MCP-Clients (Claude Desktop, Cursor, Windsurf) Claude Code beauftragen können — „Agent im Agent".

## Git-Worktrees gegen Schreibkonflikte

**Der native Mechanismus** in Claude Code: `isolation: worktree` im Subagent-Frontmatter bzw.
`claude --worktree <name>`.

**Warum es funktioniert:** Worktrees teilen die `.git`-Objektdatenbank, haben aber eigenes HEAD,
eigenen Index und eigenen Working Tree. Das verhindert Dateikollisionen, Index-Korruption bei
gleichzeitigem `git add` und „Context Contamination" durch halbfertige Dateien anderer Agenten.

**Praxisregeln:**
- flache Geschwister-Verzeichnisse (`projekt-feature-a/`, `projekt-bugfix-422/`), nicht verschachteln
- Namens- und Ressourcenkonflikte (Ports, Datenbanken, Build-Caches) über Worktree-Index-Offsets,
  eigene SQLite je Worktree und branch-geprefixte Container-Namen lösen
- Merge-Strategien: sequenziell mergen, Rebase vor PR (meist empfohlen), Pre-Merge-Konflikterkennung
  über `git merge-tree`; Cherry-Pick bei Ensemble-Ansätzen, wo mehrere Agenten dasselbe Problem
  parallel lösen und die beste Lösung gewinnt
- ein `PreToolUse`-Hook kann Schreibzugriffe außerhalb der eigenen Worktree hart blocken
- **praktische Grenze: 8 bis 10 parallele Worktrees**, darüber übersteigt der Verwaltungsaufwand den
  Nutzen ⚠️ *Beispielmessung „9,82 GB bei 20 Agenten auf 2-GB-Codebase" ist eine unbestätigte
  Einzelbeobachtung*

## Die OpenCode-Seite

- Zwei Agent-Typen: **Primary Agents** (Build als Default mit allen Werkzeugen, Plan als
  schreibgeschützter Analysemodus) und **Subagents** (General, Explore, Scout — jeweils read-only)
- Konfiguration per `opencode.json` oder Markdown-Dateien unter `~/.config/opencode/agents/`
  (global) bzw. `.opencode/agents/` (Projekt)
- Modell-IDs im Format `provider/model-id`
- **Subagents erben standardmäßig das Modell des aufrufenden Agenten**, sofern nicht überschrieben —
  genau das macht einen Schwarm auf dem jeweiligen Session-Modell möglich
- Aufgabenübergabe: automatische Delegation nach Beschreibung, manuelle `@`-Erwähnungen, und ein
  Task-Werkzeug mit rechtebasierter Filterung, das steuert, welche Subagents ein Agent aufrufen darf

---

## Was die Verbindung kostet

- Eine **Drei-Agenten-Pipeline** verbraucht etwa **29.000 Tokens** gegenüber ~10.000 bei einem
  gleichwertigen Ein-Agenten-Ansatz — knapp das Dreifache.
- **Tool-Schema-Overhead** liegt bei **10.000 bis 60.000 Tokens** in typischen
  Multi-Server-Deployments und macht **60 bis 80 % des Tokenverbrauchs** bei statischen Werkzeugsätzen
  aus.
- Ein Workflow, der im Test **0,50 $** kostet, kann bei 100.000 Ausführungen im Monat auf
  **50.000 $/Monat** eskalieren — weil der Orchestrator **zusätzlich** zu jedem Worker-Aufruf eigene
  Modellaufrufe für Zerlegung und Aggregation macht.

**Gegenmaßnahmen:** Werkzeugsätze je Agent minimieren oder dynamisch laden statt alle Schemas immer
mitzuschicken. Orchestrierungs-Overhead im **Lasttest** messen, nicht nur im Funktionstest.

## Was die Verbindung an Sicherheit kostet

### MCP
| Schwachstelle | Verbreitung in der Stichprobe |
|---|---|
| Tool Poisoning | ~5,5 % von 1.899 untersuchten Servern |
| Command Injection | 43 % der getesteten Server |
| SSRF-Anfälligkeit | 36,7 % von 7.000+ Servern |

**Tool Poisoning erklärt:** Ein Server liefert scheinbar normale Werkzeugbeschreibungen; der Agent
führt darin versteckte Anweisungen aus. Ursache: Werkzeug-Metadaten werden ungeprüft in den Kontext
übernommen und als vertrauenswürdig behandelt — indirekte Prompt-Injection über einen Kanal, den man
nicht als Eingabe wahrnimmt.
**Es gibt keinen universellen Fix.** Die Guardrail-Qualität unterscheidet sich je Client. Praktisch
hilft nur striktes Whitelisting und Sanitizing der Beschreibungen.

### A2A
- Agent-Impersonation
- Context Poisoning über manipulierte Agent Cards
- Privilegien-Eskalation durch zu grob geschnittene OAuth-Scopes
- **keine erzwungenen Token-Ablaufzeiten in der Spezifikation**

**Ursache:** Die A2A-Spezifikation definiert **kein eigenes Autorisierungs-Framework**. Scope-Design
und Token-Lebensdauer bleiben jeder Implementierung selbst überlassen.
**Gegenmaßnahmen:** Tokens granular auf einzelne Agent-Skills scopen (Least Privilege), kurze
Ablaufzeiten selbst erzwingen, bei höherem Schutzbedarf mTLS-gebundene Tokens oder DPoP.

---

# TEIL 7 — Die Messung: drei Engines im Vergleich

**Kontext für das PDF:** Dieses Kapitel ist ein Nebenbefund der Recherche selbst und gehört ins PDF,
weil es zeigt, wie stark KI-Rechercheergebnisse je nach Verfahren auseinandergehen.

**Der Aufbau:** Alle drei Engines bekamen **wortgleich dieselben sieben Unterthemen**.

- **Engine A** — Firecrawl und Tavily holen volle Seiten, ein günstiges Modell wertet aus.
- **Engine B** — dasselbe Modell mit eigener Snippet-Websuche.
- **Engine C** — ein Schwarm aus sieben Agenten, die selbst suchen und mehrfach nachfassen.

## Die Ergebnisse

| Kennzahl | A — Vollseiten | B — Snippets | C — Agenten-Schwarm |
|---|---|---|---|
| Zeichen je Antwort | 10.823 | 10.564 | **19.145** |
| URLs je Antwort | **0** | **39,0** | 36,9 |
| verschiedene Domains | — | **15,1** | 11,9 |
| Unsicherheits-Marker | 3,1 | 3,3 | **11,3** |
| Primärquellen-Anteil | **5 %** | 11 % | **44 %** |
| Zeit je Researcher | 400 s+ | **~90 s** | 3–4 min |
| Kosten je Researcher | **~0,3 ct** | ~1,5 ct | Claude-Token |

## Die drei harten Befunde

**1. Engine A nannte in keiner Antwort eine URL.** Sie zitierte stattdessen „(Quelle 1: Openlayer)"
— einen Verweis in eine nummerierte Liste, die nur im Prompt stand. Ohne die Quellendatei war die
Antwort **nicht überprüfbar**. Die Ursache war der Auswerte-Prompt, nicht das Modell: er verlangte
„nenne die Quelle" und übergab die Quellen nummeriert. Das Modell befolgte das wörtlich.

**2. B und C teilen nur 8 von 141 Domains.** Praktisch keine Überschneidung — die beiden Engines
haben verschiedene Webs gelesen. Unter den Domains, die **nur** die Snippet-Suche fand, war ein
Übersetzungswörterbuch, und zwar in vier von sieben Unterthemen.

Das ist genau der Retrieval-Bias, den Anthropic im eigenen System beschreibt: SEO-optimierte Seiten
schlagen akademische PDFs, und **automatisierte Prüfungen sehen das nicht**.

**3. Engine C ist am ehrlichsten.** Dreimal so viele Stellen mit „das konnte ich nicht belegen",
„hier widersprechen sich die Quellen", „diese PDF war nicht lesbar". Engine A hatte beim Thema
Protokolle **null** Unsicherheitsmarker — bei einem Feld, das sich vierteljährlich ändert, ist das
ein Warnsignal und kein Gütesiegel.

## Der strukturelle Grund

A und B setzen **eine** Suchanfrage ab und verarbeiten, was zurückkommt. C liest an, erkennt „diese
Seite zitiert das Original", und holt das Original. Iteratives Nachfassen gegen einmalige Suche.

## Methodische Einschränkung — bitte ins PDF übernehmen

„Primärquelle" war eine Stichwortliste (Hersteller-Domains, offizielle Dokumentationen, arXiv,
Spezifikationen). Ein anderer Zuschnitt verschiebt die Prozentzahlen. Die Rangfolge zwischen den
Engines dürfte er nicht umdrehen — dafür sind die Abstände zu groß.

---

## 🎨 ILLUSTRATION 14 — "Primärquellen-Anteil je Engine"

**Was gezeigt werden soll:** Horizontales Balkendiagramm, korrekt zur Skala (0 bis 50 %):

- Engine A — 5 % (kurzer Balken, Warnfarbe) — Beschriftung daneben: „Vollseiten + günstiges Modell"
- Engine B — 11 % (neutral) — „Snippet-Suche"
- Engine C — 44 % (langer Balken, Signalfarbe) — „Agenten-Schwarm, sucht mehrfach und folgt Spuren"

**Darunter, als zweiter Block:** Die Zahl der zitierten URLs je Antwort als große Ziffern:
A = **0** (in Warnfarbe, mit dem Zusatz „zitierte ‚(Quelle 3)' statt der Adresse"),
B = 39,0 · C = 36,9

**Kernaussage:** Engine C zitierte fast neunmal so oft Primärquellen wie Engine A.

---

## 🎨 ILLUSTRATION 15 — "Zwei Engines, zwei verschiedene Webs"

**Was gezeigt werden soll:** Ein Venn-Diagramm mit zwei Kreisen, die sich **nur minimal**
überlappen — die Überlappung muss optisch winzig sein, sie entspricht 8 von 141.

- Linker Kreis: „Engine B — 77 Domains"
- Rechter Kreis: „Engine C — 64 Domains"
- In der winzigen Überlappung: „**8**"

**Daneben ein Hinweiskasten:** „Unter den Domains, die nur die Snippet-Suche fand, war ein
Übersetzungswörterbuch — in vier von sieben Unterthemen."

**Kernaussage:** Zwei Recherchesysteme mit derselben Frage können praktisch disjunkte Quellenmengen
liefern.

---

# TEIL 8 — Die neun Merksätze

Falls jemand alles andere vergisst, das hier trägt:

1. **Baue einen Workflow, wenn du kannst.** Ein Agent kostet Latenz, Geld und Fehlerfortpflanzung.
   Autonomie muss sich lohnen, nicht beeindrucken.
2. **Werkzeugbeschreibungen sind Prompt.** Der größte Qualitätshebel bei Einzelagenten sitzt in
   Namen, Beschreibungen und Fehlermeldungen — nicht im Systemprompt.
3. **Gib nach jeder Aktion echtes Ergebnis zurück.** Sonst bewertet der Agent seinen Plan statt der
   Wirklichkeit.
4. **Der Abbruch gehört in den Code.** Binär, messbar, plus hartes Iterationslimit als Netz.
   68 dokumentierte Endlosschleifen sind Argument genug.
5. **Lass nichts sich selbst prüfen.** Generator und Prüfer teilen sonst dieselben blinden Flecken.
6. **Lesen parallel, schreiben einspurig.** Die eine Regel, aus der sich fast alle
   Multi-Agent-Entscheidungen ableiten.
7. **Bleib bei 2–6 Agenten**, außer die Teilaufgaben sind wirklich unabhängig. Kosten wachsen
   quadratisch, Qualität nicht.
8. **Für Modellgrenzen: Dateisystem statt Protokoll.** Atomarer Claim, Existenzcheck, Ledger nur
   als Nachweis.
9. **Miss den Overhead im Lasttest.** Der Orchestrator macht eigene Aufrufe zusätzlich zu jedem
   Worker-Aufruf. Im Funktionstest sieht man das nie.

**Die Meta-Regel über allem** — sie wird in fast jeder Primärquelle wiederholt:
> **Erst einfach. Komplexität nur bei belegtem Bedarf.**
> *"Start with simple prompts, optimize them with comprehensive evaluation, and add multi-step
> agentic systems only when simpler solutions fall short."*

Und, als Ergänzung dazu: *"every component in a harness encodes an assumption about what the model
can't do on its own"* — mit besseren Modellen darf man Teile wieder **abbauen**.

---

# ANHANG A — Was ausdrücklich NICHT belegt ist

**Bitte diese Liste im PDF berücksichtigen — entweder weglassen oder klar als unbelegt markieren.**

| Aussage | Status |
|---|---|
| „Brain / Hands / Nervous System" als Google-Wortlaut | mehrfach sekundärbestätigt, im OCR der Primärquelle nicht wörtlich gefunden |
| Self-Refine plateaut bei 2–4 Runden | nur Sekundärquellen; arXiv-Original nur als Abstract abrufbar |
| „3,5× Kosten bei 4 Agenten", „18.000–90.000 $/Monat" | Marketing-Blogs, teils selbst als hypothetisch gekennzeichnet |
| „17,2-fache Fehlerverstärkung", „89 % null Produktivitätsgewinn", „Klarna sparte 60 Mio. $" | einzelner Sekundärartikel ohne Primärverlinkung |
| Fünf Arten von Kontextdegradation bei Handoffs | Marketing-Blog ohne erkennbare eigene Studie |
| Massiv-paralleler Schwarm: 300 Agenten, 7,20 $/Lauf, 75–83 % Caching-Ersparnis | Drittanbieter-Guide, keine Herstellerdokumentation |
| „9,82 GB bei 20 Agenten" (Git-Worktrees) | unbestätigte Einzelbeobachtung |
| A2A-Versionshistorie zwischen 1.0 und 1.0.1 | nur Sekundärquelle |
| Claude-Agent-SDK-Versionsnummern | aus Sekundärartikel, nicht an PyPI/npm verifiziert |
| NEEDLE-Details | aus der Zusammenfassung eines Meta-Repos, nicht aus dem Projekt selbst |
| Blackboard-Prozentvorteil | PDF nicht vollständig extrahierbar |
| Ob Anthropic A2A informell unterstützt | **offene Frage** — mit den Quellen nicht klärbar |
| Ob das Governance-Paper Zed-ACP oder IBM/BeeAI-ACP meint | **offene Frage** |
| Fehlerraten einzelner ausgefallener Agenten im Schwarm | **Lücke in der Literatur** — keine Primärquelle gefunden |
| Deduplizierungs-Mechanismus bei Anthropic | im Originalartikel nicht spezifiziert |
| Offizielle Anthropic-Doku zu „SDK ruft fremde CLI-Agenten" | existiert nicht; nur Community-Beispiele |

---

# ANHANG B — Vollständige Quellenliste

## Hersteller und Spezifikationen
- Anthropic — Building Effective Agents: https://www.anthropic.com/engineering/building-effective-agents
- Anthropic — Writing Tools for Agents: https://www.anthropic.com/engineering/writing-tools-for-agents
- Anthropic — Effective Context Engineering: https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
- Anthropic — Harnesses for Long-Running Agents: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
- Anthropic — Harness Design for Long-Running Apps: https://www.anthropic.com/engineering/harness-design-long-running-apps
- Anthropic — Demystifying Evals for AI Agents: https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents
- Anthropic — How we built our multi-agent research system: https://www.anthropic.com/engineering/multi-agent-research-system
- Anthropic — Trustworthy agents in practice: https://www.anthropic.com/research/trustworthy-agents
- Anthropic — MCP-Spende / Agentic AI Foundation: https://anthropic.com/news/donating-the-model-context-protocol-and-establishing-of-the-agentic-ai-foundation
- Claude Agent SDK: https://claude.com/blog/building-agents-with-the-claude-agent-sdk
- Claude Code — Subagents: https://code.claude.com/docs/en/sub-agents
- Claude Code — Hooks: https://code.claude.com/docs/en/hooks
- Claude Code — MCP: https://code.claude.com/docs/en/mcp
- Claude Platform — Managed Agents, Multiagent Orchestration: https://platform.claude.com/docs/en/managed-agents/multiagent-orchestration
- Claude Platform — Tests entwickeln: https://platform.claude.com/docs/en/test-and-evaluate/develop-tests
- MCP-Spezifikation 2026-07-28: https://modelcontextprotocol.io/specification/2026-07-28/changelog
- OpenAI Agents SDK — Agents: https://openai.github.io/openai-agents-python/agents/
- OpenAI Agents SDK — Handoffs: https://openai.github.io/openai-agents-python/handoffs/
- OpenAI — A Practical Guide to Building Agents (PDF, nicht textuell extrahierbar): https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf
- Google — Whitepaper „Agents" (OCR-Textderivat): https://ia800601.us.archive.org/15/items/google-ai-agents-whitepaper/Newwhitepaper_Agents_djvu.txt
- OpenCode — Agents: https://opencode.ai/docs/agents/
- Linux Foundation — Agentic AI Foundation: https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation
- Linux Foundation — A2A, Stand April 2026: https://www.linuxfoundation.org/press/a2a-protocol-surpasses-150-organizations-lands-in-major-cloud-platforms-and-sees-enterprise-production-use-in-first-year
- Google Developers Blog — A2A an die Linux Foundation: https://developers.googleblog.com/en/google-cloud-donates-a2a-to-linux-foundation/
- Zed — Agent Client Protocol Registry: https://zed.dev/blog/acp-registry

## Forschung
- arXiv 2503.13657 — Why Do Multi-Agent LLM Systems Fail? (MAST): https://arxiv.org/pdf/2503.13657
- arXiv 2310.01798 — LLMs Cannot Self-Correct Reasoning Yet: https://arxiv.org/abs/2310.01798
- arXiv 2311.08516 — Fehler finden gegen Fehler beheben: https://arxiv.org/pdf/2311.08516
- arXiv 2410.21819 — Self-Preference Bias in LLM-as-a-Judge: https://arxiv.org/abs/2410.21819
- arXiv 2503.16024 — The Lighthouse of Language (Critique-Guided Improvement): https://arxiv.org/html/2503.16024v2
- arXiv 2509.00676 — LLaVA-Critic-R1 (Gegenbefund): https://arxiv.org/html/2509.00676v1
- arXiv 2607.01641 — When Agents Do Not Stop (Infinite Agentic Loops): https://arxiv.org/html/2607.01641v1
- arXiv 2510.12697 — Multi-Agent Debate mit adaptivem Stopp-Kriterium: https://arxiv.org/html/2510.12697v1
- arXiv 2606.00655 — Scaling Behavior of Multi-Agent Systems: https://arxiv.org/html/2606.00655v1
- arXiv 2604.02460 — Single-Agent gegen Multi-Agent bei gleichem Budget: https://arxiv.org/pdf/2604.02460
- arXiv 2303.11366 — Reflexion: https://arxiv.org/abs/2303.11366
- arXiv 2603.11445 — Verified Multi-Agent Orchestration (VMAO): https://arxiv.org/html/2603.11445v2
- arXiv 2503.16814 — Echo-Chamber-Effekte in Debatten: https://arxiv.org/pdf/2503.16814
- arXiv 2608.02827 — Emergence of Biased Consensus: https://arxiv.org/html/2608.02827v1
- arXiv 2606.31498 — Governance Gaps in Agent Interoperability Protocols: https://arxiv.org/abs/2606.31498
- arXiv 2508.12683 — Taxonomy of Hierarchical Multi-Agent Systems: https://arxiv.org/pdf/2508.12683
- arXiv 2510.01285 — LLM-Based Multi-Agent Blackboard System: https://arxiv.org/pdf/2510.01285
- TACL — When Can LLMs Actually Correct Their Own Mistakes: https://direct.mit.edu/tacl/article/doi/10.1162/tacl_a_00713/125177/When-Can-LLMs-Actually-Correct-Their-Own-Mistakes

## Praxis und Analyse
- Cognition — Don't Build Multi-Agents: https://cognition.com/blog/dont-build-multi-agents
- Cognition — Multi-Agents: What's Actually Working: https://cognition.com/blog/multi-agents-working
- AWS — Multi-Agent-Orchestrierung über S3-Dateien: https://aws.amazon.com/blogs/storage/orchestrating-multi-agent-ai-architectures-with-amazon-s3-files/
- Augment Code — Multi-Agent Cost Compounding: https://www.augmentcode.com/guides/multi-agent-cost-compounding
- Practical DevSecOps — MCP-Schwachstellen: https://www.practical-devsecops.com/mcp-security-vulnerabilities/
- OWASP — MCP Tool Poisoning: https://owasp.org/www-community/attacks/MCP_Tool_Poisoning
- Tyk — A2A Security Guide: https://tyk.io/learning-center/a2a-security-the-developers-complete-guide/
- Cloud Security Alliance — MAESTRO-Bedrohungsmodell für A2A: https://cloudsecurityalliance.org/blog/2025/04/30/threat-modeling-google-s-a2a-protocol-with-the-maestro-framework
- Gartner — Prognose zu Agentic-AI-Projekten: https://www.gartner.com/en/newsroom/press-releases/2025-06-25-gartner-predicts-over-40-percent-of-agentic-ai-projects-will-be-canceled-by-end-of-2027
- MindStudio — Verifiable Stop Conditions: https://www.mindstudio.ai/blog/agent-loops-verifiable-stop-conditions
- LangGraph — Recursion Limit: https://docs.langchain.com/oss/python/langgraph/errors/GRAPH_RECURSION_LIMIT
- Awesome Agent Orchestrators (Meta-Repo, Quelle für NEEDLE): https://github.com/andyrewlee/awesome-agent-orchestrators
- Openlayer — Multi-Agent System Architecture Guide: https://www.openlayer.com/blog/post/multi-agent-system-architecture-guide

---

# ANHANG C — Vorschläge für die PDF-Gestaltung

Das ist ein Angebot, keine Vorgabe. Wenn du eine bessere Idee hast, nimm sie.

**Charakter:** technisches Handbuch, kein Marketingmaterial. Der Inhalt ist präzise und stellenweise
unbequem — die Gestaltung sollte das tragen, nicht überdecken.

**Struktur:** Die acht Teile sind eine sinnvolle Kapitelfolge und bauen aufeinander auf. Eine
Nummerierung ist hier inhaltlich gerechtfertigt, weil man Teil 4 ohne Teil 1 nicht versteht.

**Farbe:** Zwei Akzente reichen. Einer für „so funktioniert der Mechanismus" (in den Abbildungen die
hervorgehobenen Pfade), einer für „hier wird es teuer oder geht schief" (Kosten, Fehlerraten,
Sicherheitslücken). Der zweite Akzent sollte in allen Abbildungen konsistent dieselbe Bedeutung
tragen.

**Abbildungen:** Fünfzehn Briefings stehen oben, jeweils mit 🎨 markiert. Vier davon sind mit ⭐
als Schlüsselabbildungen gekennzeichnet — wenn du kürzen musst, kürze bei den anderen.

**Zwei Dinge, die das PDF unbedingt haben sollte:**
1. **Die Unsicherheitsmarkierungen aus Anhang A**, sichtbar an der jeweiligen Stelle im Text — nicht
   nur versteckt im Anhang. Ein kleines Symbol oder ein Randvermerk reicht.
2. **Die wörtlichen Zitate** als solche erkennbar. Sie tragen die Beweislast.

**Ein Ton-Hinweis:** Der Text sagt an mehreren Stellen ausdrücklich, was **nicht** bekannt ist. Das
ist keine Schwäche des Materials, sondern sein Wert. Bitte nicht wegglätten.
