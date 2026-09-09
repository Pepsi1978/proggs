# Multi-Agent-Interop Kurzcheck (Bugs)

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT
> (`bugs/agents/multi-agent-interop.md`), nicht nur diese Kurzfassung.
> Stand: 2026-09-09.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Symptom | Ursache in einem Satz | Fix in einem Satz |
|---|---------|----------------------|-------------------|
| 1 | MCP-Server fuehrt versteckte Anweisungen aus | Tool-Beschreibungen landen ungeprueft im LLM-Kontext (indirekte Prompt-Injection); ~5,5 % von 1.899 Servern betroffen | Fremde Server strikt whitelisten, Tool-Beschreibungen sanitizen — es gibt keinen universellen Fix |
| 2 | A2A-Token nach Leak weiter gueltig, Agent kann zu viel | Die Spec definiert KEIN Autorisierungs-Framework — Scopes und Ablaufzeiten bleiben der Implementierung ueberlassen | Tokens auf einzelne Skills scopen, kurze Ablaufzeiten selbst erzwingen, Agent Cards validieren |
| 3 | Kosten explodieren erst nach dem Rollout | Orchestrator-LLM-Calls kommen ZUSAETZLICH zu jedem Worker-Call; Tool-Schemas machen 60-80 % der Tokens aus | Toolsets je Agent minimieren/dynamisch laden, Overhead im Lasttest messen statt im Funktionstest |
| 4 | Handoff zu einem Agenten anderer Firma geht nicht | OpenAI-Handoffs sind SDK-intern, bleiben "within a single run" — kein Cross-Vendor-Protokoll | Dateisystem/Queue mit atomaren Claims statt Handoff; A2A nur mit Vorbehalt (Anthropic fehlt in der Traegerliste) |
| 5 | Zwei Agenten bearbeiten dieselbe Aufgabe | Ledger allein ist nicht atomar — zwischen Lesen und Eintragen liegt ein Fenster | `O_CREAT\|O_EXCL`-Claim + Output-Existenzcheck; Ledger nur als Nachweis, nie als alleinige Koordination |
| 6 | `git add` korrumpiert den Index, Agenten sehen halbfertige Dateien | Mehrere Agenten im selben Working Tree mit gemeinsamem HEAD/Index | Git-Worktrees (`isolation: worktree`), praktische Grenze 8-10 parallel |
| 7 | Schwarm liefert Ergebnisse zu einem fremden Thema | Ausgabeordner mit `exist_ok=True`; bei Agenten-Ausfall bleibt die alte Datei stehen und wird als aktuell gelesen | Vor jedem Lauf eigene Ausgabemuster hart loeschen; Ausgabeverzeichnis je Lauf konfigurierbar machen |
| 8 | Architekturentscheidung auf Basis von "ACP" ist falsch | ZWEI Protokolle mit dem Kuerzel: Zed = Agent↔Editor, IBM/BeeAI = Agent↔Agent | Bei jeder ACP-Erwaehnung zuerst den Traeger pruefen |
| 9 | Recherche belegt alles mit Marketing-Blogs statt Primaerquellen | Retrieval-Bias zu SEO-optimierten Seiten; faellt in automatisierten Evals NICHT auf | Agenten mit eigener Suche statt Snippet-Pipeline; Prompt auf Primaerquellen lenken; menschliche Stichprobe |
| 10 | Antwort zitiert "(Quelle 3)" statt einer URL | Prompt uebergab Quellen als nummerierte Liste und verlangte nur "die Quelle" | Im Prompt ausdruecklich die vollstaendige URL verlangen — sonst ist die Antwort ohne Quellendatei nicht pruefbar |

**Faustregel:** Die Verbindung zwischen Agenten ist 2026 der unsicherste und teuerste Teil des
Systems — nicht die Agenten selbst. Vor jeder Cross-Vendor-Architektur pruefen, ob ein gemeinsames
Dateisystem mit atomaren Claims nicht reicht.
