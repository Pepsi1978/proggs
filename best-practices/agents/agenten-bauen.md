# KI-Agenten selbst bauen — Best Practices (Stand 04.10.2026, 23:30 Uhr)

> Gegenstück zu `bugs/agents/agenten-bauen.md`. Quelle: Recherche vom 04.10.2026 mit sechs Sonnet-5.5-Researchern (Engine C),
> rund 200 abgerufene Quellen. Die vollständigen Ergebnisse mit allen Kernaussagen, URLs und Prüfstufen liegen verlustfrei in
> `~/proggs/Lernen/ki-agenten/research/01` bis `06`. Diese Datei ist die verdichtete Fassung.
> Eng verwandt: `agents/loop-engineering.md` (Schleife im Dauerbetrieb), `agents/orchestrator-agent.md` (Mehr-Agenten),
> `agents/multi-agent-interop.md` (MCP/A2A), `apis/cli-impersonation-subscription-auth.md` (Abo-OAuth), `server/ai-agent-frameworks.md`.
> Quell-Flag: `offiziell` = Hersteller/Standard/Paper, `extern` = Praktiker/Blog.
>
> **Versions-Anker (04.10.2026):** Claude Agent SDK 0.3.289 (= Claude Code 2.1.289), `@anthropic-ai/sdk` 0.131.0, `openai` (npm) 7.28.0,
> `@openai/agents` 0.18.0, Codex SDK/CLI 0.160.0, Vercel AI SDK 7.0.127, Koog 1.3.0, anthropic-java 2.68.0, openai-java 4.76.0,
> MCP-Spezifikation 2026-07-28, MCP TypeScript SDK 2.3.0 (v1-Linie 1.32.0), MCP Kotlin SDK 0.15.0, LangChain4j 1.21.0.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neue KI-Funktion geplant | Erst einzelner Aufruf, dann Workflow, erst dann Agent. Prüffrage: entscheidet mein Code oder das Modell den nächsten Schritt? | §1 |
| 2 | Agenten-Schleife selbst schreiben | Schleife über den Stop-Grund, IMMER eigenes Rundenlimit (Tutorial und Agent SDK haben keins), alle Werkzeugwünsche einer Antwort beantworten | §2 |
| 3 | Werkzeug entwerfen | Wenige starke Werkzeuge, 3 bis 4 Sätze Beschreibung, `strict`-Schema, Fehler als lehrreiches Ergebnis zurückgeben | §3 |
| 4 | MCP einsetzen | Spezifikation 2026-07-28 ist zustandslos, SDKs sprechen standardmäßig noch das 2025er-Protokoll. Alte Tutorials passen nicht | §4 |
| 5 | Kontext wird voll | Statisches zuerst (Cache), knappe Werkzeugausgaben, Zusammenfassen, Notizen in Dateien, Subagenten nur lesend | §5 |
| 6 | Mehrere Agenten erwogen | Standard ist EIN Agent. Mehrere nur bei zerlegbaren Aufgaben, nur einer schreibt, Ergebnisse in Dateien | §6 |
| 7 | Agent prüfen | 20 bis 50 Aufgaben aus echten Fehlern, deterministische Prüfer zuerst, Transkripte lesen, Richter binär | §7 |
| 8 | Agent absichern | Lethal Trifecta / Rule of Two prüfen, Rechte minimieren, Werkzeugergebnisse sind fremde Daten. Sandbox fehlt unter nativem Windows | §8 |
| 9 | Baukasten für TypeScript wählen | Claude Agent SDK und Codex SDK starten die CLI als Unterprozess: nur PC/Server, nie Android | §9 |
| 10 | Baukasten für Kotlin/Android wählen | Kein Kotlin-SDK von Anthropic/OpenAI (nur Java-SDK, Android ungeprüft). Koog 1.3.0 nennt Android als Ziel | §9 |
| 11 | Modellzugang per Abo statt API-Schlüssel | OpenAI: offizieller Weg „Sign in with ChatGPT“ (seit 29.09.2026). Claude: Grauzone, nie die Claude-Code-Identität nachbauen | §10 |

## 1. Die einfachste Stufe wählen (offiziell)

Anthropic trennt Workflows (Modelle und Werkzeuge über vorgegebene Code-Pfade) von Agenten (das Modell lenkt Ablauf und Werkzeugnutzung selbst). Empfehlung: einfachste Lösung suchen, Komplexität nur bei nachgewiesenem Nutzen erhöhen, erst die Roh-API benutzen und Frameworks erst danach. OpenAI nennt drei Kriterien für einen Agenten: Entscheidungen mit Urteilsvermögen, schwer wartbare Regelwerke, unstrukturierte Daten. Agenten verbrauchen laut Anthropic etwa 4-mal, Mehr-Agenten-Systeme etwa 15-mal so viele Token wie ein Chat.

Quellen: https://www.anthropic.com/engineering/building-effective-agents · https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf · https://www.anthropic.com/engineering/multi-agent-research-system

## 2. Die Schleife (offiziell)

Claude Messages API: Anfrage mit `tools` senden; solange `stop_reason == "tool_use"`, jeden `tool_use`-Block ausführen und alle Ergebnisse als `tool_result`-Blöcke in EINER `user`-Nachricht zurückgeben (Ergebnisblöcke zuerst, am besten kein Text daneben). Das Modell führt nie selbst etwas aus. Der ganze Verlauf wird in jeder Runde neu geschickt. Weitere Stop-Gründe behandeln: `max_tokens` (abgeschnittenen Werkzeugwunsch nie ausführen), `pause_turn`, `refusal`, `model_context_window_exceeded`.

OpenAI Responses API: `function_call` verarbeiten (`arguments` ist ein JSON-String), Antwort als `function_call_output` mit gleichem `call_id`.

Pflicht: eigene harte Obergrenze für Runden und möglichst Kosten. Das Claude-Tutorial zeigt keine, im Claude Agent SDK sind `maxTurns` und `maxBudgetUsd` standardmäßig unbegrenzt.

Quellen: https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works · https://platform.claude.com/docs/en/agents-and-tools/tool-use/handle-tool-calls · https://code.claude.com/docs/en/agent-sdk/agent-loop · https://developers.openai.com/api/docs/guides/function-calling

## 3. Werkzeuge entwerfen (offiziell)

Wenige gebündelte Werkzeuge statt vieler kleiner, Namensräume, Beschreibung wie für einen neuen Kollegen (wann nutzen, wann nicht), eindeutige Parameternamen, Enums, absolute Pfade, `strict: true` mit `additionalProperties: false`. Rückgaben kurz und bedeutungstragend, mit Paginierung. Fehler als Ergebnis mit `is_error: true` und einem Text, der sagt, was das Modell stattdessen versuchen soll. Ab etwa 10 Werkzeugen oder 10k Token Definitionen Tool Search mit `defer_loading` erwägen.

Quellen: https://www.anthropic.com/engineering/writing-tools-for-agents · https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools · https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool

## 4. MCP (offiziell)

Die Spezifikation 2026-07-28 ist zustandslos (kein `initialize`, keine Sessions). TypeScript-SDK v2 (2.3.0) spricht sie nur per Opt-in, das Kotlin-SDK 0.15.0 höchstens 2025-11-25. Pro Anfrage eigene Server-/Transport-Instanz (TS v2). Der MCP-Connector der Claude-API erreicht nur entfernte Server, keinen lokalen Stdio-Server. Eine offizielle Faustregel „MCP oder eigene Werkzeuge“ gibt es nicht.

Quellen: https://modelcontextprotocol.io/specification/latest · https://modelcontextprotocol.io/specification/2026-07-28/changelog · https://github.com/modelcontextprotocol/typescript-sdk · https://github.com/modelcontextprotocol/kotlin-sdk

## 5. Kontext und Gedächtnis (offiziell + extern)

Prompt-Aufbau für den Cache: Werkzeuge, dann System-Prompt, dann Nachrichten; Statisches zuerst, Wechselndes (Zeitstempel) zuletzt; Werkzeuge und Modell mitten in der Sitzung nicht wechseln. Kontext klein halten: knappe Werkzeugausgaben, Zusammenfassen (Compaction, Beta), Notizen und Fortschritt in Dateien, Laden erst bei Bedarf, Subagenten mit frischem Kontext für lesende Teilaufgaben. Lange Aufgaben über mehrere Fenster: Aufgabenliste, Fortschrittsdatei, Git als Zustand. Dem Modell sagen, dass der Kontext automatisch verdichtet wird, sonst hört es zu früh auf.

Offener Streit (extern): Fehler im Kontext lassen (Manus) oder bereinigen (Breunig).

Quellen: https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents · https://platform.claude.com/docs/en/build-with-claude/prompt-caching · https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool · https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents · https://www.trychroma.com/research/context-rot · https://manus.im/blog/Context-Engineering-for-AI-Agents-Lessons-from-Building-Manus

## 6. Mehrere Agenten (offiziell + extern)

Mit einem Agenten anfangen. Mehrere lohnen bei zerlegbaren, breiten Aufgaben (Recherche) und schaden bei sequenziellen. Regeln aus der Praxis: nur ein Agent schreibt, weitere liefern Erkenntnisse; Briefing mit Ziel, Ausgabeformat, Werkzeugen und Grenzen; Ergebnisse in Dateien statt durch die Gesprächskette.

Quellen: https://www.anthropic.com/engineering/multi-agent-research-system · https://cognition.com/blog/dont-build-multi-agents · https://cognition.com/blog/multi-agents-working · https://arxiv.org/abs/2503.13657

## 7. Evals (offiziell + extern)

Start mit 20 bis 50 Aufgaben aus echten Fehlern. Deterministische Prüfer zuerst, LLM-Richter nur binär, je Fehlertyp einer und gegen menschliche Urteile geprüft. Ergebnis prüfen, nicht den Weg. Mehrere Läufe pro Aufgabe, Regression getrennt von Fähigkeit. Transkripte selbst lesen.

Quellen: https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents · https://hamel.dev/blog/posts/evals-faq/ · https://developers.openai.com/api/docs/guides/evaluation-best-practices

## 8. Sicherheit (offiziell + extern)

Lethal Trifecta: private Daten, fremde Inhalte und Außenverbindung nie alle drei ohne menschliche Freigabe (Metas Rule of Two). Werkzeugergebnisse sind fremde Daten; Modellausgaben und Werkzeugparameter validieren; Autorisierung im Zielsystem. Rechte minimieren, harte Garantien über Hooks oder Sandbox statt Textmuster. Geheimnisse nicht in die Agenten-Umgebung legen. Die Claude-Code-Sandbox gilt nur für Shell-Befehle und läuft unter nativem Windows gar nicht (nur WSL2). Prüfraster: OWASP Top 10 for LLM Applications 2025 und for Agentic Applications 2026.

Quellen: https://simonwillison.net/2025/Jun/16/the-lethal-trifecta/ · https://ai.meta.com/blog/practical-ai-agent-security/ · https://code.claude.com/docs/en/sandboxing · https://code.claude.com/docs/en/agent-sdk/secure-deployment · https://genai.owasp.org/resource/owasp-top-10-for-agentic-applications-for-2026/

## 9. Baukästen für TypeScript und Kotlin (offiziell)

- Claude Agent SDK (TS) und Codex SDK (TS) sind Hüllen um die jeweilige CLI (Unterprozess, stdio). Sie erben deren Anmeldung, laufen aber nur auf PC/Server, nicht in einer Android-App.
- `@openai/agents` und Vercel AI SDK (`ToolLoopAgent`) sind reine Bibliotheken mit API-Schlüssel bzw. Bearer-Token.
- Kotlin: kein eigenes SDK von Anthropic oder OpenAI, nur die Java-SDKs (Java 8+, R8-Regeln bei Anthropic, Android nirgends ausdrücklich zugesagt). Koog (JetBrains) nennt Android als Ziel und erlaubt eine eigene `baseUrl`. Das MCP Kotlin SDK nennt Android nicht namentlich.
- Zum Lernen: erst Eigenbau-Schleife über die Roh-API, dann mit einem Framework vergleichen.

Quellen: https://code.claude.com/docs/en/agent-sdk/hosting · https://github.com/openai/codex/blob/main/sdk/typescript/README.md · https://github.com/JetBrains/koog · https://platform.claude.com/docs/en/cli-sdks-libraries/sdks/java

## 10. Modellzugang per Abo (offiziell; Details in `apis/cli-impersonation-subscription-auth.md` §10)

OpenAI: „Sign in with ChatGPT“ mit Abo-Nutzung ist seit 29.09.2026 der offizielle Weg für Open-Source- und lokale Eigenprojekte (PKCE, Loopback-Redirect, Responses API mit Bearer-Token, `store:false` und `stream:true` Pflicht). Claude: Produkte sollen API-Schlüssel nutzen; die Anmeldung mit eigenem Abo im unveränderten Claude-Code-Binary ist erlaubt; Eigenbau mit Agent SDK und eigenem Abo ist nicht ausdrücklich geregelt. Nie die Claude-Code-Identität nachbauen.

Quellen: https://developers.openai.com/siwc/quickstart.md · https://code.claude.com/docs/en/legal-and-compliance

## Noch offen

Android-Tauglichkeit der Java-SDKs und des MCP-Kotlin-SDK; SIWC-Loopback auf Android; Android-spezifische Agenten-Sicherheit; Zahlen aus Blogartikeln (Manus, Chroma, Cognition, Hamel Husain) sind nur über Zusammenfassungen geprüft.

## 🔗 Bezug zum Bug-Almanach

`bugs/agents/agenten-bauen.md`
