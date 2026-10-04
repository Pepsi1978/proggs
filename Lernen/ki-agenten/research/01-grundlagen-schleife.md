# Research 01: Grundlagen und Agenten-Schleife

Stand der Recherche: 04.10.2026. Researcher 1. Thema: Was ist ein Agent, wie sieht die Schleife technisch aus, welche Bauformen gibt es, wann keinen Agenten bauen.

Prüfstufen der Quellen:
- **Volltext**: die Seite oder das PDF wurde im Rohtext geladen und gelesen, Zitate sind wörtlich.
- **Zusammenfassung**: der Abruf lieferte nur eine Zusammenfassung des Abrufwerkzeugs. Zitate daraus sind wörtlich so übernommen, aber nicht gegen den Volltext geprüft.

## Kernaussagen

1. **Workflow und Agent sind laut Anthropic ein architektonischer Unterschied.** Workflows sind "systems where LLMs and tools are orchestrated through predefined code paths". Agenten sind "systems where LLMs dynamically direct their own processes and tool usage, maintaining control over how they accomplish tasks". Beides zusammen nennt Anthropic "agentic systems". Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents

2. **Abgrenzung zum einzelnen Modellaufruf nach OpenAI.** Anwendungen, die ein LLM einbinden, aber nicht zur Steuerung des Ablaufs nutzen, sind keine Agenten: "simple chatbots, single-turn LLMs, or sentiment classifiers". Ein Agent nutzt das LLM, um den Ablauf zu steuern und Entscheidungen zu treffen, erkennt, wann der Ablauf fertig ist, und kann bei Fehlern anhalten und die Kontrolle an den Nutzer zurückgeben. Er hat Werkzeuge und wählt sie dynamisch je nach Zustand. Quelle (Volltext, PDF, Seiten 4 und 5): https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf

3. **Kurzdefinition der Praktiker: Werkzeuge in einer Schleife.** Simon Willison (18.09.2025): "An LLM agent runs tools in a loop to achieve a goal." Das "goal" ist die Abbruchbedingung, die Schleife ist begrenzt, nicht endlos. Er hat dafür 211 Definitionen gesammelt. Quelle (Zusammenfassung): https://simonwillison.net/2025/Sep/18/agents/ . Anthropic sagt dasselbe mit anderen Worten: Agenten sind "typically just LLMs using tools based on environmental feedback in a loop" (Volltext: https://www.anthropic.com/engineering/building-effective-agents).

4. **Die Schleife ist technisch eine einfache while-Schleife über `stop_reason`.** Kanonische Form laut Claude-Dokumentation: (1) Anfrage mit `tools` und Nutzernachricht senden, (2) Claude antwortet mit `stop_reason: "tool_use"` und einem oder mehreren `tool_use`-Blöcken, (3) jedes Werkzeug ausführen und als `tool_result`-Blöcke formatieren, (4) neue Anfrage mit den bisherigen Nachrichten, der Assistenten-Antwort und einer User-Nachricht mit den `tool_result`-Blöcken, (5) wiederholen, solange `stop_reason` `"tool_use"` ist. Die Schleife endet bei jedem anderen Stop-Grund (`end_turn`, `max_tokens`, `stop_sequence`, `refusal`). Quelle (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works

5. **Das Modell führt nie selbst etwas aus.** "The model never executes anything on its own. It emits a structured request, your code (or Anthropic's servers) runs the operation, and the result flows back into the conversation." Client-Werkzeuge laufen in der eigenen Anwendung, Server-Werkzeuge (`web_search`, `web_fetch`, `code_execution`, `tool_search`) bei Anthropic. Quelle (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works und https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

6. **Aufbau der Nachrichten für die Schleife (Claude Messages API).** Ein `tool_use`-Block hat `id`, `name`, `input`. Die Antwort darauf ist eine `user`-Nachricht mit einem `tool_result`-Block mit `tool_use_id`, optional `content` und optional `is_error`. Die Claude-API hat keine eigene Rolle `tool`: Werkzeugaufrufe stehen in `assistant`-Nachrichten, Ergebnisse in `user`-Nachrichten. Die komplette Gesprächshistorie wird bei jedem Aufruf neu mitgeschickt. Quelle (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

7. **Das Minimalbeispiel einer Schleife in TypeScript** steht in der Claude-Doku im Tutorial: `while (response.stop_reason === "tool_use") { ... messages.push({role:"assistant", content: response.content}); messages.push({role:"user", content:[{type:"tool_result", tool_use_id: toolUse.id, content: ...}]}); response = await client.messages.create({...}) }`. Das Tutorial setzt dort `disable_parallel_tool_use: true`, weil der Code nur den ersten `tool_use`-Block bearbeitet. Es nennt keine Obergrenze für Iterationen. Quelle (Zusammenfassung): https://platform.claude.com/docs/en/agents-and-tools/tool-use/build-a-tool-using-agent ; das Flag `disable_parallel_tool_use` selbst ist im Volltext belegt: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

8. **Der gesamte Agent in einem Satz von Thorsten Ball:** "It's an LLM, a loop, and enough tokens." Sein Agent hat etwa 300 Zeilen Go-Code und drei Werkzeuge (`read_file`, `list_files`, `edit_file`). Die Schleife: Nutzereingabe lesen, an die Historie anhängen, Modell aufrufen, Antwort auf `tool_use` prüfen, Werkzeuge ausführen und Ergebnisse als nächste Nachricht senden, wenn keine Werkzeugergebnisse da sind, wieder Nutzereingabe lesen. Datum: 15.04.2025. Quelle (Volltext): https://ampcode.com/how-to-build-an-agent

9. **Abbruchbedingungen laut den Anbietern.**
   - Anthropic: "The task often terminates upon completion, but it's also common to include stopping conditions (such as a maximum number of iterations) to maintain control." (Volltext: https://www.anthropic.com/engineering/building-effective-agents)
   - OpenAI: "Common exit conditions include tool calls, a certain structured output, errors, or reaching a maximum number of turns." Der Agents-SDK-Runner läuft, bis ein Final-Output-Werkzeug aufgerufen wird oder das Modell ohne Werkzeugaufruf antwortet. (Volltext PDF, Seite 14 und 15: https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf)
   - OpenAI Agents SDK: nach `max_turns` wird `MaxTurnsExceeded` geworfen, `max_turns=None` schaltet die Grenze ab, ein Fehler-Handler kann stattdessen eine kontrollierte Ausgabe liefern. Quelle (Zusammenfassung): https://openai.github.io/openai-agents-python/running_agents/
   - Claude Agent SDK: `max_turns`/`maxTurns` (zählt nur Tool-Use-Runden) und `max_budget_usd`/`maxBudgetUsd`, beide Standard: kein Limit. Ergebnis-Untertypen: `success`, `error_max_turns`, `error_max_budget_usd`, `error_during_execution`, `error_max_structured_output_retries`. Quelle (Volltext): https://code.claude.com/docs/en/agent-sdk/agent-loop

10. **Alle `stop_reason`-Werte der Claude-API:** `end_turn`, `max_tokens`, `stop_sequence`, `tool_use`, `pause_turn` (Server-Werkzeug-Schleife hat ihr Iterationslimit, Standard 10, erreicht, Antwort zurückschicken, um fortzusetzen), `refusal`, `model_context_window_exceeded` (als abgeschnitten behandeln). Quelle (Zusammenfassung): https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons ; `pause_turn` und die Aufzählung `end_turn`/`max_tokens`/`stop_sequence`/`refusal` zusätzlich im Volltext: https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works

11. **Fehler eines Werkzeugs gehören in die Schleife zurück, nicht in eine Exception.** Bei einem Fehler wird ein `tool_result` mit `is_error: true` und einer lehrreichen Meldung zurückgeschickt (nicht "failed", sondern was schiefging und was Claude versuchen soll). Bei ungültigen Aufrufen versucht Claude laut Doku 2 bis 3 Korrekturen, bevor es sich entschuldigt. `strict: true` an der Werkzeugdefinition garantiert schemakonforme Eingaben. Quelle (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls und https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

12. **Der Agent braucht Rückmeldung aus der Umgebung ("ground truth") in jedem Schritt.** "During execution, it's crucial for the agents to gain 'ground truth' from the environment at each step (such as tool call results or code execution) to assess its progress." Claude Code beschreibt dieselbe Schleife als drei verschwimmende Phasen: Kontext sammeln, handeln, Ergebnis prüfen. Quellen (Volltext): https://www.anthropic.com/engineering/building-effective-agents und https://code.claude.com/docs/en/how-claude-code-works

13. **Die fünf Workflow-Bauformen von Anthropic (mit Einsatzkriterium):**
    - Prompt-Chaining: Aufgabe lässt sich sauber in feste Teilschritte zerlegen; tauscht Latenz gegen Genauigkeit.
    - Routing: klar getrennte Kategorien, die Klassifikation ist zuverlässig möglich (Beispiel: leichte Fragen an kleines Modell, schwere an großes).
    - Parallelisierung in zwei Varianten: Sectioning (unabhängige Teilaufgaben) und Voting (dieselbe Aufgabe mehrfach).
    - Orchestrator-Workers: Teilaufgaben sind nicht vorhersagbar, ein zentrales LLM zerlegt dynamisch und delegiert. Unterschied zur Parallelisierung: Teilaufgaben sind nicht vorab festgelegt.
    - Evaluator-Optimizer: klare Bewertungskriterien, und Iteration bringt messbaren Nutzen (ein LLM erzeugt, ein anderes bewertet, in einer Schleife).
    Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents . Referenzimplementierungen (Python-Notebooks): https://github.com/anthropics/claude-cookbooks/tree/main/patterns/agents (Zusammenfassung).

14. **Anthropic baut den Bausteine-Stapel von unten auf:** erst das "augmented LLM" (LLM plus Retrieval, Werkzeuge, Gedächtnis), dann Workflows, dann Agenten. Agenten sind für offene Probleme, bei denen sich die Schrittzahl nicht vorhersagen lässt und kein fester Pfad hartcodierbar ist, in vertrauenswürdigen Umgebungen. Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents

15. **ReAct (Originalarbeit):** Yao u. a., "ReAct: Synergizing Reasoning and Acting in Language Models", arXiv 06.10.2022, v3 vom 10.03.2023, ICLR 2023. Das Modell erzeugt abwechselnd Gedanken (Thought) und Aktionen (Action), die Umgebung liefert Beobachtungen (Observation); die Trajektorie besteht aus mehreren Thought-Action-Observation-Schritten. Aktionsraum bei HotpotQA: `search[entity]`, `lookup[string]`, `finish[answer]`. Ergebnis: auf ALFWorld +34 Prozentpunkte, auf WebShop +10 Prozentpunkte absolute Erfolgsrate gegenüber Imitations- und Verstärkungslernen, mit ein bis zwei Beispielen im Prompt. Quelle (Volltext PDF): https://arxiv.org/pdf/2210.03629 ; Abstract-Seite: https://arxiv.org/abs/2210.03629

16. **ReAct nennt selbst die typische Schleifenfalle:** "one frequent error pattern specific to ReAct, in which the model repetitively generates the previous thoughts and actions ... the model fails to reason about what the proper next action to take and jump out of the loop". Nicht-informative Suchergebnisse machten 23 Prozent der Fehlerfälle aus. Halluzination ist bei reinem Chain-of-Thought der Hauptfehler (56 Prozent der Fehlerfälle), bei ReAct deutlich seltener, weil die Werkzeugergebnisse erden. Quelle (Volltext PDF, Abschnitt 3.3): https://arxiv.org/pdf/2210.03629

17. **Moderne Tool-Use-APIs sind die eingebaute Form von ReAct (Einordnung, keine wörtliche Quellenaussage).** Der Gedanke steht als Text oder Thinking-Block neben dem `tool_use`-Block, die Beobachtung ist der `tool_result`-Block. Belegt ist nur der Aufbau der Antwort: Claude kann laut Doku "respond with text, request one or more tool calls, or both" (Volltext: https://code.claude.com/docs/en/agent-sdk/agent-loop , Beispielantwort mit `text`- plus `tool_use`-Block in https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls ). Die Gleichsetzung mit ReAct ist eine Deutung des Researchers.

18. **Wann KEINEN Agenten bauen (Anthropic):** "we recommend finding the simplest solution possible, and only increasing complexity when needed. This might mean not building agentic systems at all. Agentic systems often trade latency and cost for better task performance". Für viele Anwendungen reicht ein einzelner LLM-Aufruf mit Retrieval und Beispielen. Erst einfache Prompts, dann Evaluation, dann mehrstufige Agentensysteme, "only when simpler solutions fall short". Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents

19. **Wann einen Agenten bauen (OpenAI):** drei Kriterien: komplexe Entscheidungen mit Ausnahmen und Urteilsvermögen (Beispiel Erstattungsfreigabe), schwer wartbare Regelwerke (Beispiel Lieferantenprüfung), starke Abhängigkeit von unstrukturierten Daten (Beispiel Schadensmeldung). "Before committing to building an agent, validate that your use case can meet these criteria clearly. Otherwise, a deterministic solution may suffice." Quelle (Volltext PDF, Seite 6): https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf

20. **Kosten und Fehlerverstärkung sprechen gegen Agenten bei einfachen Aufgaben.** Anthropic: "The autonomous nature of agents means higher costs, and the potential for compounding errors." Empfohlen sind ausgiebige Tests in Sandbox-Umgebungen und Leitplanken. Anthropic misst im eigenen Multi-Agenten-Recherchesystem: Agenten verbrauchen etwa 4-mal, Multi-Agenten-Systeme etwa 15-mal so viele Token wie ein Chat. Quellen: https://www.anthropic.com/engineering/building-effective-agents (Volltext) und https://www.anthropic.com/engineering/multi-agent-research-system (Zusammenfassung, Datum 13.06.2025)

21. **Gute Einsatzgebiete haben klare Erfolgskriterien und prüfbare Ergebnisse.** Coding-Agenten funktionieren, weil Lösungen per automatisierten Tests prüfbar sind und der Agent Testergebnisse als Rückmeldung nutzt; menschliche Prüfung bleibt nötig. Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents (Appendix 1). Willison ergänzt: gut für Probleme mit klaren Erfolgskriterien und Versuch-und-Irrtum (fehlschlagende Tests, Leistungsoptimierung, Abhängigkeits-Updates), ein solider automatisierter Testbestand verstärkt den Agenten. Quelle (Zusammenfassung): https://simonwillison.net/2025/Sep/30/designing-agentic-loops/

22. **Mit einem Agenten anfangen, nicht mit mehreren.** OpenAI: "starting with a single agent and evolving to multi-agent systems only when needed". Zwei Muster: Manager-Muster (ein zentrales LLM ruft spezialisierte Agenten als Werkzeuge auf und behält den Nutzerkontakt) und dezentrales Muster (Agenten übergeben sich per Handoff die Kontrolle). Quelle (Volltext PDF, Seiten 13 bis 20 und Fazit): https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf

23. **Gegenposition zu Multi-Agenten (Cognition, 12.06.2025):** Multi-Agenten-Architekturen seien in der Praxis fragil. Zwei Prinzipien: "Share context, and share full agent traces, not just individual messages" und "Actions carry implicit decisions, and conflicting decisions carry bad results". Empfehlung für die meisten Fälle: einfädige, lineare Agenten mit durchgehendem Kontext, bei langen Aufgaben eine LLM-Kompressionsschicht. Quelle (Zusammenfassung): https://cognition.com/blog/dont-build-multi-agents . Anthropic nennt selbst Grenzen: Multi-Agenten passen schlecht, wenn alle Agenten denselben Kontext brauchen oder viele Abhängigkeiten bestehen, Coding habe weniger parallelisierbare Teilaufgaben als Recherche (https://www.anthropic.com/engineering/multi-agent-research-system, Zusammenfassung).

24. **Frameworks: erst direkt die API benutzen.** "We suggest that developers start by using LLM APIs directly: many patterns can be implemented in a few lines of code. If you do use a framework, ensure you understand the underlying code." Frameworks erzeugen Abstraktionsschichten, die Prompts und Antworten verdecken und das Debuggen erschweren. Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents . Das passt zum Kurskonzept "von Grund auf".

25. **Werkzeugentwurf ist halbe Arbeit der Schleife (Agent-Computer-Interface, ACI).** Anthropic hat beim SWE-bench-Agenten mehr Zeit in die Werkzeuge als in den Gesamtprompt gesteckt. Beispiel: Das Modell machte Fehler mit relativen Dateipfaden, nachdem der Agent aus dem Wurzelverzeichnis gewechselt war; nach der Umstellung auf zwingend absolute Pfade benutzte es die Methode "flawlessly". Weitere Regeln: Format nah an dem, was das Modell im Netz gesehen hat; kein Formatierungs-Overhead (zum Beispiel Zeilen zählen, Code in JSON escapen); Werkzeuge wie eine Docstring für einen Junior-Entwickler beschreiben; Fehler "poka-yoke"-sicher machen. Quelle (Volltext): https://www.anthropic.com/engineering/building-effective-agents (Appendix 2). Vertiefung (Zusammenfassung, 11.09.2025): https://www.anthropic.com/engineering/writing-tools-for-agents (verwandte Werkzeuge zusammenlegen, Namensräume, aussagekräftige Rückgabefelder, Token-sparsame Antworten mit Paginierung, eindeutige Parameternamen wie `user_id`, evaluationsgetrieben iterieren).

26. **Kontext wächst in jeder Runde und muss verwaltet werden.** "Everything accumulates: the system prompt, tool definitions, conversation history, tool inputs, and tool outputs." Große Werkzeugausgaben verbrauchen viel Kontext. Gegenmaßnahmen: automatische Kompaktierung (ältere Historie zusammenfassen), Subagenten mit frischem Kontext, die nur eine Zusammenfassung zurückgeben, wenige und knapp definierte Werkzeuge. Quelle (Volltext): https://code.claude.com/docs/en/agent-sdk/agent-loop . Konzeptionell (Zusammenfassung, 29.09.2025): https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents (Context Rot, Compaction, Notizen außerhalb des Kontexts, Subagenten geben 1.000 bis 2.000 Token Zusammenfassung zurück). Huntley: "The more you allocate to a context window, the worse the performance will be." (Zusammenfassung: https://ghuntley.com/agent/ , 24.08.2025)

27. **Sicherheit der Schleife.** Werkzeugergebnisse stammen oft aus Quellen außerhalb der eigenen Kontrolle (Webseiten, E-Mails, Uploads) und sind unvertrauenswürdig (indirekte Prompt Injection); untrusted Inhalte gehören in `tool_result`-Blöcke, nicht in `system`-Prompts. Quelle (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls . Willison nennt drei Gefahren bei automatischer Freigabe ("YOLO mode"): zerstörerische Shell-Befehle, Abfluss von Quellcode oder Geheimnissen, Missbrauch der Maschine als Angriffsproxy; Abhilfe: Container, fremde Rechner (Codespaces), Zugangsdaten nur für Test-Umgebungen und mit Ausgabenlimit. Quelle (Zusammenfassung): https://simonwillison.net/2025/Sep/30/designing-agentic-loops/

28. **Berechtigungen und Hooks sind Teil der Schleife in Produktionsagenten.** Das Claude Agent SDK fängt Werkzeugaufrufe vor der Ausführung mit `PreToolUse`-Hooks ab; ein abgelehnter Aufruf liefert Claude die Ablehnung als Werkzeugergebnis, und Claude versucht typischerweise einen anderen Weg. Schreibende Werkzeuge laufen nacheinander, nur lesende dürfen parallel laufen. Quelle (Volltext): https://code.claude.com/docs/en/agent-sdk/agent-loop

29. **Harness-Annahmen veralten mit den Modellen.** Anthropic: "Harnesses encode assumptions about what Claude can't do on its own. However, those assumptions need to be frequently questioned because they can go stale as models improve." Beispiel: Kontext-Resets gegen "context anxiety" bei Claude Sonnet 4.5 waren bei Opus 4.5 "dead weight". Quelle (Zusammenfassung, 08.04.2026, Lance Martin u. a.): https://www.anthropic.com/engineering/managed-agents . Der Originalartikel "Building effective agents" trägt selbst den Hinweis, dass sich die Werkzeuglandschaft seit Dezember 2024 geändert hat, mit Verweis auf Managed Agents (Volltext).

30. **Es gibt fertige Schleifen als SDK-Abstraktion, die man für den Kurs bewusst erst danach zeigt.** Der Tool Runner der Anthropic-SDKs "handles the agentic loop, error wrapping, and type safety"; er ist Beta und für Python, TypeScript, C#, Go, Java, PHP und Ruby aufgelistet. Für Human-in-the-loop, eigenes Logging oder bedingte Ausführung nimmt man die manuelle Schleife. Das Claude Agent SDK liefert dieselbe Schleife wie Claude Code (TypeScript und Python). Quellen (Volltext): https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-runner (Kopf der Seite geprüft, restliche Seite nicht ausgewertet) und https://code.claude.com/docs/en/agent-sdk/agent-loop

31. **Kotlin hat kein offizielles Anthropic-SDK.** Die offizielle Liste der Client-SDKs: Python, TypeScript, C#, Go, Java, PHP, Ruby, dazu die `ant`-CLI. Für Kotlin/Android bleibt das Java-SDK oder eine eigene HTTP-Implementierung der Messages API. Quelle (Volltext): https://platform.claude.com/docs/en/cli-sdks-libraries/overview . Inoffizielle Kotlin-Bibliotheken tauchten nur in einer Websuche auf und wurden nicht geprüft (xemantic/anthropic-sdk-kotlin, rimdoo/anthropic-kotlin).

32. **Evaluation gehört zur Methode.** Hamel Husain (29.03.2024): erfolglose Produkte teilen fast immer die Ursache fehlender robuster Evaluationssysteme; drei Stufen (Unit-Tests, Mensch- und Modellbewertung anhand echter Traces, A/B-Tests) und "remove all friction from the process of looking at data". Quelle (Zusammenfassung): https://hamel.dev/blog/posts/evals/ . Passt zu Anthropic: Leistung messen und iterieren, Komplexität nur hinzufügen, wenn sie das Ergebnis nachweislich verbessert (Volltext: https://www.anthropic.com/engineering/building-effective-agents).

33. **Geoffrey Huntley bestätigt das Minimalmodell als Lernpfad.** Workshop "how to build a coding agent" in Go, sechs Stufen, jeweils ein neues Werkzeug: Basic Chat, File Reader, File Explorer, Command Runner, File Editor, Code Search. Er nennt das Gerüst "300 lines of code running in a loop with LLM tokens". Quellen (Zusammenfassung): https://ghuntley.com/agent/ und https://github.com/ghuntley/how-to-build-a-coding-agent/blob/trunk/README.md

## Beste Quellen

1. **Building effective agents** — Erik Schluntz und Barry Zhang, Anthropic Engineering. https://www.anthropic.com/engineering/building-effective-agents — veröffentlicht 19.12.2024 (trägt Hinweis, dass sich die Tool-Landschaft geändert hat). Taugt als Grundgerüst des Kurses: Definition Workflow vs. Agent, alle fünf Bauformen, Einsatzkriterien, "wann nicht". Vertrauensstufe: offiziell.

2. **A practical guide to building agents** — OpenAI (PDF). https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf — kein Datum im PDF-Text, Datum nicht verifiziert. Taugt für Agentendefinition, Einsatzkriterien, Run-Loop mit Ausstiegsbedingungen, Manager- und dezentrales Muster, Leitplanken. Vertrauensstufe: offiziell. Die HTML-Seite https://openai.com/business/guides-and-resources/a-practical-guide-to-building-ai-agents/ lieferte 403.

3. **How to Build an Agent** (or: The Emperor Has No Clothes) — Thorsten Ball, Amp. https://ampcode.com/how-to-build-an-agent — 15.04.2025. Taugt als Vorlage für den praktischen Kursteil: komplette Schleife mit drei Werkzeugen in Go, direkt übertragbar nach TypeScript und Kotlin. Vertrauensstufe: extern, hohe Reputation.

4. **ReAct: Synergizing Reasoning and Acting in Language Models** — Yao, Zhao, Yu, Du, Shafran, Narasimhan, Cao. https://arxiv.org/abs/2210.03629 (PDF: https://arxiv.org/pdf/2210.03629) — v1 06.10.2022, v3 10.03.2023, ICLR 2023. Taugt als wissenschaftliche Wurzel der Schleife Denken-Handeln-Beobachten samt Fehlermuster der Endlosschleife. Vertrauensstufe: extern (peer-reviewte Originalarbeit).

5. **How tool use works** — Claude Docs. https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works — Stand 04.10.2026 abgerufen. Taugt für die technische Beschreibung der Schleife über `stop_reason`, Wo-Werkzeuge-laufen, Wann-Werkzeuge-nicht. Vertrauensstufe: offiziell.

6. **Handle tool calls** — Claude Docs. https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls — Stand 04.10.2026. Taugt für exakte Feldnamen, Reihenfolgeregeln und `is_error`. Vertrauensstufe: offiziell.

7. **Handle stop reasons** — Claude Docs. https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons — Stand 04.10.2026 (nur als Zusammenfassung geprüft). Taugt für alle Stop-Gründe und die Fallen mit `max_tokens` und leeren Antworten. Vertrauensstufe: offiziell.

8. **Tutorial: Build a tool-using agent** — Claude Docs. https://platform.claude.com/docs/en/agents-and-tools/tool-use/build-a-tool-using-agent — Stand 04.10.2026 (Zusammenfassung). Taugt als TypeScript-Vorlage der while-Schleife in fünf Ringen. Vertrauensstufe: offiziell.

9. **How the agent loop works** — Claude Agent SDK Docs. https://code.claude.com/docs/en/agent-sdk/agent-loop — Stand 04.10.2026 (Volltext). Taugt für Turns, `max_turns`, Budget, Ergebnis-Untertypen, Kontextwachstum, Kompaktierung, Hooks. Vertrauensstufe: offiziell.

10. **How Claude Code works** — Claude Code Docs. https://code.claude.com/docs/en/how-claude-code-works — Stand 04.10.2026 (Volltext). Taugt, um dem Lerner die vertraute Claude-Code-Erfahrung als Agentenschleife und "agentic harness" zu erklären. Vertrauensstufe: offiziell.

11. **How we built our multi-agent research system** — Anthropic Engineering. https://www.anthropic.com/engineering/multi-agent-research-system — 13.06.2025 (Zusammenfassung). Taugt für Orchestrator-Worker in der Praxis samt Fehlerbildern (Überspawnen, vage Delegation) und Token-Faktoren 4 und 15. Vertrauensstufe: offiziell.

12. **Writing effective tools for agents** — Anthropic Engineering. https://www.anthropic.com/engineering/writing-tools-for-agents — 11.09.2025 (Zusammenfassung). Taugt für das Kapitel Werkzeugdesign. Vertrauensstufe: offiziell.

13. **Effective context engineering for AI agents** — Anthropic Engineering. https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents — 29.09.2025 (Zusammenfassung). Taugt für das Kapitel Kontext und Gedächtnis. Vertrauensstufe: offiziell.

14. **Scaling Managed Agents** — Lance Martin, Gabe Cemaj, Michael Cohen, Anthropic Engineering. https://www.anthropic.com/engineering/managed-agents — 08.04.2026 (Zusammenfassung). Taugt als aktueller Nachfolger: Harness als austauschbare Schleife, Sitzungslog außerhalb des Harness, veraltende Annahmen. Vertrauensstufe: offiziell.

15. **Running agents** — OpenAI Agents SDK. https://openai.github.io/openai-agents-python/running_agents/ — Stand 04.10.2026 (Zusammenfassung). Taugt als zweites Anbieterbeispiel für Schleife, `max_turns`, Final-Output. Vertrauensstufe: offiziell.

16. **I think "agent" may finally have a widely enough agreed upon definition** — Simon Willison. https://simonwillison.net/2025/Sep/18/agents/ — 18.09.2025 (Zusammenfassung). Taugt für die Ein-Satz-Definition. Vertrauensstufe: extern, hohe Reputation.

17. **Designing agentic loops** — Simon Willison. https://simonwillison.net/2025/Sep/30/designing-agentic-loops/ — 30.09.2025 (Zusammenfassung). Taugt für Sandboxing, Zugangsdaten, geeignete Aufgaben. Vertrauensstufe: extern, hohe Reputation.

18. **how to build a coding agent: free workshop** — Geoffrey Huntley. https://ghuntley.com/agent/ (24.08.2025) und https://github.com/ghuntley/how-to-build-a-coding-agent (Zusammenfassungen). Taugt als stufenweise Übungsfolge (Chat, Lesen, Listen, Shell, Editieren, Suchen). Vertrauensstufe: extern.

19. **Don't Build Multi-Agents** — Walden Yan, Cognition. https://cognition.com/blog/dont-build-multi-agents — 12.06.2025 (Zusammenfassung). Taugt als begründete Gegenposition zu Multi-Agenten. Vertrauensstufe: extern, Anbieter mit eigenem Produktinteresse (Devin).

20. **Evals-Beitrag (Titel im Abruf nicht bestätigt)** — Hamel Husain. https://hamel.dev/blog/posts/evals/ — 29.03.2024 (Zusammenfassung). Taugt für das Schlusskapitel Evaluation. Vertrauensstufe: extern, hohe Reputation. Bestätigt wurden nur Inhalt und Datum, nicht der Titel.

21. **Tool runner (SDK)** — Claude Docs. https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-runner — Stand 04.10.2026 (nur Seitenkopf geprüft). Taugt für die Abstraktion über die manuelle Schleife. Vertrauensstufe: offiziell.

22. **SDKs, CLI, and libraries** — Claude Docs. https://platform.claude.com/docs/en/cli-sdks-libraries/overview — Stand 04.10.2026 (Volltext). Taugt für die offizielle SDK-Sprachenliste (kein Kotlin). Vertrauensstufe: offiziell.

23. **claude-cookbooks / patterns/agents** — Anthropic. https://github.com/anthropics/claude-cookbooks/tree/main/patterns/agents — Stand 04.10.2026 (Zusammenfassung). Taugt für Python-Referenzcode zu Chaining, Routing, Parallelisierung, Evaluator-Optimizer, Orchestrator-Workers. Vertrauensstufe: offiziell.

## Offen oder widersprüchlich

- **Definition von "Agent" ist uneinheitlich.** Anthropic: das LLM lenkt seinen Prozess dynamisch selbst. OpenAI: LLM steuert die Workflow-Ausführung, erkennt das Ende und gibt bei Fehlern zurück. Willison: Werkzeuge in einer Schleife bis zum Ziel. Der Kurs sollte eine Arbeitsdefinition festlegen, am besten die von Anthropic (Workflow vs. Agent) plus Willisons Merksatz.
- **Eine Schleife mit festen Schritten kann beides sein.** Evaluator-Optimizer ist laut Anthropic ein Workflow, enthält aber eine Schleife; die Grenze zum Agenten (wer entscheidet über die Wiederholung, Code oder Modell) ist im Kurs ausdrücklich zu ziehen.
- **ReAct taucht bei Anthropic nicht als eigene Bauform auf.** Die Kursliste (Prompt-Chaining, Routing, Parallelisierung, Orchestrator-Worker, Evaluator-Optimizer, ReAct) mischt Anthropics fünf Workflow-Muster mit einem Agenten-Muster aus der Forschung. Kernaussage 17 (ReAct gleich nativer Tool-Use) ist eine Deutung, keine Quellenaussage. Eine Quelle, die genau diese Gleichsetzung belegt, wurde nicht gefunden.
- **Multi-Agenten: Anthropic und Cognition widersprechen sich.** Anthropic setzt Orchestrator-Worker produktiv für Recherche ein (Faktor 15 bei Token), Cognition rät grundsätzlich ab. Beide Begründungen decken sich teilweise (Coding hat wenig parallelisierbare Arbeit). Bewertung für den Kurs: Standard ist ein Agent; Multi-Agenten nur als Ausblick.
- **Standardlimit der Schleife unterscheidet sich.** Claude Agent SDK: kein Limit für `max_turns` und `max_budget_usd`. OpenAI Agents SDK: ein `max_turns`-Limit gilt standardmäßig (der Standardwert wurde nicht abgerufen). Das Claude-Tutorial zeigt eine while-Schleife ganz ohne Obergrenze. Eigener Code braucht eine harte Iterationsgrenze, auch wenn das Tutorial sie weglässt.
- **Die Quelle "Building effective agents" ist selbst teilweise veraltet.** Der Artikel nennt Claude Haiku 4.5 und Sonnet 4.5 und trägt den Hinweis zur geänderten Werkzeuglandschaft. Die Muster gelten weiter, aber Beispielmodelle und Framework-Liste sind nicht aktuell.
- **Modellnamen in der Doku (Stand 04.10.2026).** Die abgerufenen Seiten benutzen `claude-opus-5-5` in den Beispielen und erwähnen Claude Sonnet 5.5 in der Preistabelle. Für Kursbeispiele muss die Modell-ID vor Veröffentlichung gegen https://platform.claude.com/docs/en/models/overview geprüft werden; das wurde nicht gemacht.
- **Datum des OpenAI-Leitfadens nicht verifiziert.** Weder PDF-Text noch Suchtreffer lieferten ein Datum. Die HTML-Seite war nicht abrufbar (403). Der OpenAI-Beitrag "Unrolling the Codex agent loop" (https://openai.com/index/unrolling-the-codex-agent-loop/) war ebenfalls nicht abrufbar (403) und ging nicht in diese Liste ein.
- **Tool-Runner-Details nicht ausgewertet.** Parameter wie eine maximale Iterationszahl, Kotlin/Java-Syntax und Beta-Stand pro SDK stehen in der Seite, die nur im Kopf geprüft wurde (Volltext liegt in der Abrufdatei, 65 KB). Kotlin-spezifischer Beispielcode für die Messages-API mit dem Java-SDK wurde nicht recherchiert (gehört vermutlich zu einem anderen Unterthema).
- **Zusammenfassungs-Quellen.** Alle Quellen mit Vermerk "Zusammenfassung" wurden nicht im Volltext gelesen. Für wörtliche Zitate im Kurs sollten Artikel 11 bis 20 vorher einzeln im Volltext nachgeprüft werden.
- **Quellenlücken.** Nicht gefunden beziehungsweise nicht abgerufen: Lilian Weng "LLM Powered Autonomous Agents", Chip Huyen zu Agenten, Hamel Husain speziell zu Agenten. Anthropics "Effective harnesses for long-running agents" wurde nicht gesucht.

## BEST-PRACTICES-KANDIDATEN:

- **Schleife über `stop_reason` mit hartem Limit.** `while (stop_reason === "tool_use")`, zusätzlich eigene Obergrenze für Runden (und optional Kostenbudget), weil weder Tutorial noch Claude Agent SDK ein Standardlimit setzen. Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works ; https://code.claude.com/docs/en/agent-sdk/agent-loop ; https://www.anthropic.com/engineering/building-effective-agents . Version/Stand: Claude Messages API, Header `anthropic-version: 2023-06-01`, abgerufen am 04.10.2026.
- **Ergebnisse immer als `tool_result` direkt in der nächsten `user`-Nachricht, `tool_result`-Blöcke zuerst, dann erst Text, bei der Schleife am besten gar kein Text daneben.** Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls ; https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons
- **Alle `tool_use`-Blöcke einer Antwort bearbeiten und alle Ergebnisse in einer einzigen User-Nachricht zurückgeben**, oder bewusst `tool_choice: {type:"auto", disable_parallel_tool_use:true}` setzen. Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/build-a-tool-using-agent ; https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview
- **Werkzeugfehler als `is_error: true` mit lehrreichem Text zurückgeben; `strict: true` für schemagenaue Eingaben.** Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls ; https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview
- **Werkzeuge mit absoluten Pfaden, eindeutigen Parameternamen, Beispielen im Beschreibungstext und Poka-yoke-Design.** Quellen: https://www.anthropic.com/engineering/building-effective-agents ; https://www.anthropic.com/engineering/writing-tools-for-agents
- **Erst einfach: einzelner Aufruf, dann Workflow, dann Agent; Komplexität nur bei nachgewiesenem Nutzen; Frameworks erst nach Verständnis der Roh-API.** Quelle: https://www.anthropic.com/engineering/building-effective-agents
- **Einen Agenten statt mehrerer; Multi-Agenten nur bei klarem Bedarf.** Quellen: https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf ; https://cognition.com/blog/dont-build-multi-agents ; https://www.anthropic.com/engineering/multi-agent-research-system
- **Agenten in Sandbox oder Container betreiben, Zugangsdaten nur für Test-Umgebungen mit Ausgabenlimit; Tool-Ergebnisse als unvertrauenswürdig behandeln.** Quellen: https://simonwillison.net/2025/Sep/30/designing-agentic-loops/ ; https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls
- **Kontext-Disziplin: knappe Werkzeugausgaben, Kompaktierung, Subagenten mit frischem Kontext.** Quellen: https://code.claude.com/docs/en/agent-sdk/agent-loop ; https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents
- **Evaluation von Anfang an (Unit-Tests, echte Traces ansehen).** Quelle: https://hamel.dev/blog/posts/evals/
- **Versions-/Stand-Hinweise für Kursbeispiele:** Claude Messages API mit `anthropic-version: 2023-06-01`; in der Doku verwendete Modell-ID `claude-opus-5-5` und Server-Werkzeug `web_search_20260209` (04.10.2026); Claude Agent SDK `@anthropic-ai/claude-agent-sdk` (TypeScript) mit Cap-Durchsetzung für Budgets ab Claude Code v2.1.217 (https://code.claude.com/docs/en/agent-sdk/agent-loop); Tool Runner in der Doku als Beta gekennzeichnet (https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-runner). Paketversionen der npm- und Maven-Pakete wurden nicht geprüft. Offizielle Kotlin-SDKs gibt es nicht (Java-SDK nutzen): https://platform.claude.com/docs/en/cli-sdks-libraries/overview

## BUG-KANDIDATEN:

1. **400-Fehler "tool_use ids were found without tool_result blocks immediately after".**
   - Symptom: API-Fehler 400 beim zweiten Aufruf.
   - Ursache: Zwischen der Assistenten-Nachricht mit `tool_use` und der User-Nachricht mit `tool_result` liegt eine weitere Nachricht, oder in der User-Nachricht steht Text vor den `tool_result`-Blöcken, oder ein `tool_use` bekam kein Ergebnis.
   - Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

2. **Leere Antwort oder vorzeitiges `end_turn` nach einem Werkzeugergebnis.**
   - Symptom: Das Modell antwortet nach dem `tool_result` mit leerem Inhalt und `stop_reason: "end_turn"`.
   - Ursache: Zusätzlicher Text direkt hinter dem `tool_result` im selben User-Block bringt dem Modell bei, Nutzereingabe zu erwarten.
   - Abhilfe laut Doku: nur den `tool_result` senden; bei leerer Antwort eine neue User-Nachricht mit "Please continue" anhängen, leere Antworten nie unverändert wiederholen.
   - Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons (Zusammenfassung)

3. **Abgeschnittener Werkzeugaufruf bei `stop_reason: "max_tokens"`.**
   - Symptom: Der letzte Block ist ein unvollständiger `tool_use`-Block.
   - Ursache: `max_tokens` zu niedrig für Eingabeparameter (zum Beispiel ganze Dateiinhalte als Argument).
   - Abhilfe: `max_tokens` erhöhen und die Anfrage wiederholen, den unvollständigen Aufruf nicht ausführen.
   - Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons (Zusammenfassung)

4. **Nur der erste `tool_use`-Block wird bearbeitet.**
   - Symptom: Fehler 400 wegen fehlender `tool_result`s oder stillschweigend verlorene Aufrufe, sobald das Modell mehrere Werkzeuge parallel aufruft.
   - Ursache: Schleifencode mit `find()` auf den ersten `tool_use`-Block (so im Tutorial, dort mit `disable_parallel_tool_use: true`), aber ohne das Flag übernommen.
   - Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/build-a-tool-using-agent (Zusammenfassung) und https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview (Flag)

5. **Endlosschleife und Kostenexplosion ohne Limit.**
   - Symptom: Agent läuft viele Runden, Kosten wachsen, kein Ende.
   - Ursache: Keine Obergrenze für Runden oder Budget (Claude Agent SDK Standard: kein Limit; das Tutorial zeigt keine Obergrenze).
   - Abhilfe: `max_turns` und `max_budget_usd` beziehungsweise eigener Zähler.
   - Quellen: https://code.claude.com/docs/en/agent-sdk/agent-loop ; https://www.anthropic.com/engineering/building-effective-agents

6. **Wiederholungsschleife des Modells (ReAct-Fehlermuster).**
   - Symptom: Das Modell erzeugt immer wieder dieselben Gedanken und Aktionen.
   - Ursache: Es erkennt nicht, welche Aktion als nächste sinnvoll ist; häufig nach nicht-informativen Suchergebnissen (23 Prozent der ReAct-Fehlerfälle).
   - Quelle: https://arxiv.org/pdf/2210.03629 (Abschnitt 3.3). Abhilfe (Deutung): Wiederholungserkennung und Rundenlimit.

7. **Schlechte Werkzeugparameter führen zu falschen Aufrufen.**
   - Symptom: Falsche Dateien oder Fehler nach Verzeichniswechsel.
   - Ursache: Relative Pfade, nachdem der Agent aus dem Wurzelverzeichnis gewechselt war.
   - Abhilfe: Absolute Pfade zwingend verlangen.
   - Quelle: https://www.anthropic.com/engineering/building-effective-agents (Appendix 2)

8. **Modell erfindet fehlende Pflichtparameter.**
   - Symptom: `tool_use` mit plausibel klingenden, aber nicht vom Nutzer genannten Werten (Beispiel: Ort "New York, NY" ohne Angabe).
   - Ursache: Besonders Sonnet rät eher, Opus fragt eher nach; nicht garantiert.
   - Abhilfe: Beschreibung präzisieren, im System-Prompt zum Nachfragen anweisen, `strict`-Schema nutzen.
   - Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

9. **Prompt Injection über Werkzeugergebnisse.**
   - Symptom: Der Agent befolgt Anweisungen, die in einer Webseite, E-Mail oder Datei stehen.
   - Ursache: Unvertrauenswürdiger Inhalt landet in System-Prompt oder Nutzertext statt im `tool_result`, oder wird ohne Schutz ausgeführt.
   - Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls

10. **Kontext läuft voll, Qualität sinkt (Context Rot).**
    - Symptom: Der Agent vergisst frühere Vorgaben, wird ungenau oder bricht ab.
    - Ursache: Große Werkzeugausgaben und wachsende Historie; Kompaktierung kann frühe Anweisungen verlieren; Claude Code stoppt nach wenigen Versuchen mit einem "thrashing"-Fehler, wenn eine einzelne Ausgabe den Kontext sofort wieder füllt.
    - Quellen: https://code.claude.com/docs/en/agent-sdk/agent-loop ; https://code.claude.com/docs/en/how-claude-code-works ; https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents (Zusammenfassung)

11. **Multi-Agenten: Überspawnen, doppelte Arbeit, widersprüchliche Annahmen.**
    - Symptom: 50 Subagenten für einfache Fragen, Lücken, Doppelarbeit, nicht zusammenpassende Teilergebnisse.
    - Ursache: Vage Delegation, kein geteilter Kontext, keine Anleitung zur Aufwandsschätzung.
    - Quellen: https://www.anthropic.com/engineering/multi-agent-research-system (Zusammenfassung) ; https://cognition.com/blog/dont-build-multi-agents (Zusammenfassung)

12. **`pause_turn` und `refusal` werden nicht behandelt.**
    - Symptom: Agent bricht scheinbar mitten in der Arbeit ab (Server-Werkzeuge) oder liefert nichts (Ablehnung).
    - Ursache: Schleife kennt nur `tool_use` und `end_turn`. `pause_turn` verlangt, die Antwort unverändert zurückzuschicken, bei `refusal` sind `stop_details` auszuwerten.
    - Quelle: https://platform.claude.com/docs/en/build-with-claude/handling-stop-reasons (Zusammenfassung) ; https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works (Volltext)

13. **Veraltete Harness-Annahmen.**
    - Symptom: Workarounds (zum Beispiel Kontext-Resets) verschlechtern neuere Modelle.
    - Ursache: Annahmen über Modellschwächen werden nicht überprüft, wenn das Modell wechselt.
    - Quelle: https://www.anthropic.com/engineering/managed-agents (Zusammenfassung)
