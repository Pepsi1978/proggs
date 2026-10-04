# KI-Agenten bauen: Quellen

Stand 04.10.2026. Die vollständigen Rechercheergebnisse mit allen Kernaussagen, Fallen und Prüfstufen liegen in `research/01` bis `research/06`. Hier steht nur die scharfe Auswahl.

## Knowledge

### Grundlagen und Schleife (Details: `research/01-grundlagen-schleife.md`)

- [Artikel: „Building effective agents“, Schluntz und Zhang, Anthropic Engineering, 19.12.2024](https://www.anthropic.com/engineering/building-effective-agents)
  Grundgerüst des Kurses: Workflow gegen Agent, fünf Bauformen, „so einfach wie möglich“. Use for: jede Frage „brauche ich hier überhaupt einen Agenten?“. Modellnamen darin sind veraltet.
- [Leitfaden: „A practical guide to building agents“, OpenAI (PDF)](https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf)
  Zweite Anbietersicht: Definition, Einsatzkriterien, Abbruchbedingungen, Manager- und Handoff-Muster. Use for: Gegenprobe zu Anthropic.
- [Artikel: „How to Build an Agent“, Thorsten Ball, 15.04.2025](https://ampcode.com/how-to-build-an-agent)
  Ganzer Coding-Agent in rund 300 Zeilen Go mit drei Werkzeugen. Use for: Vorlage für die erste eigene Schleife in TypeScript und Kotlin.
- [Doku: „How tool use works“, Claude-Dokumentation](https://platform.claude.com/docs/en/agents-and-tools/tool-use/how-tool-use-works)
  Die Schleife technisch, über `stop_reason`. Use for: genaue Feldnamen und Abbruchgründe.
- [Paper: „ReAct“, Yao u. a., ICLR 2023](https://arxiv.org/abs/2210.03629)
  Wissenschaftliche Wurzel von Denken, Handeln, Beobachten samt Fehlbild der Wiederholungsschleife. Use for: Hintergrund, nicht für Code.

### Werkzeuge und MCP (Details: `research/02-werkzeuge-mcp.md`)

- [Doku: „Define tools“ und „Handle tool calls“, Claude-Dokumentation](https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools)
  Schema, Beschreibungsregeln, `tool_result`, `is_error`, typische 400-Fehler. Use for: jedes eigene Werkzeug.
- [Doku: „Function calling“, OpenAI Responses API](https://developers.openai.com/api/docs/guides/function-calling)
  Dasselbe bei OpenAI: `function_call` und `function_call_output`. Use for: Beispiele, die über die Codex-Anmeldung laufen.
- [Artikel: „Writing effective tools for agents“, Anthropic Engineering, 11.09.2025](https://www.anthropic.com/engineering/writing-tools-for-agents)
  Werkzeugdesign: wenige starke Werkzeuge, klare Namen, sparsame Rückgaben, lehrreiche Fehlertexte. Use for: Entwurf vor dem Programmieren.
- [Spezifikation: Model Context Protocol, Stand 2026-07-28](https://modelcontextprotocol.io/specification/latest)
  Das Protokoll selbst, jetzt zustandslos. Use for: MCP-Lektionen. SDKs: [TypeScript 2.3.0](https://github.com/modelcontextprotocol/typescript-sdk), [Kotlin 0.15.0](https://github.com/modelcontextprotocol/kotlin-sdk); beide sprechen standardmäßig noch das 2025er-Protokoll.

### Kontext und Gedächtnis (Details: `research/03-kontext-gedaechtnis.md`)

- [Artikel: „Effective context engineering for AI agents“, Anthropic Engineering, 29.09.2025](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents)
  Kernbegriffe: Zusammenfassen, Notizen außerhalb des Kontexts, Subagenten, Laden erst bei Bedarf. Use for: alles rund ums volle Kontextfenster.
- [Doku: „Memory tool“ und „Prompt caching“, Claude-Dokumentation](https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool)
  Langzeitgedächtnis in Dateien, Kosten sparen durch Caching. Use for: Gedächtnis-Lektionen.
- [Studie: „Context Rot“, Chroma Research, 14.07.2025](https://www.trychroma.com/research/context-rot)
  Messung, wie die Leistung mit wachsendem Kontext fällt. Use for: Begründung, warum weniger Kontext oft besser ist. Zahlen nur über Zusammenfassung geprüft.
- [Artikel: „Context Engineering for AI Agents: Lessons from Building Manus“, 18.07.2025](https://manus.im/blog/Context-Engineering-for-AI-Agents-Lessons-from-Building-Manus)
  Praxisregeln eines Agenten-Herstellers: Dateisystem als Kontext, Fehler im Verlauf lassen. Use for: Gegenposition zu „Kontext bereinigen“ ([Breunig](https://www.dbreunig.com/2025/06/26/how-to-fix-your-context.html)).

### Mehrere Agenten (Details: `research/04-mehr-agenten.md`)

- [Artikel: „How we built our multi-agent research system“, Anthropic Engineering, 13.06.2025](https://www.anthropic.com/engineering/multi-agent-research-system)
  Referenzfall Orchestrator mit Subagenten, mit Zahlen zu Gewinn und Token-Kosten. Use for: Briefing und Rückgabeformat.
- [Artikel: „Don't Build Multi-Agents“ (12.06.2025)](https://cognition.com/blog/dont-build-multi-agents) und [„Multi-Agents: What's Actually Working“ (22.04.2026)](https://cognition.com/blog/multi-agents-working), Cognition
  Die begründete Gegenposition und ihre spätere Verfeinerung (nur ein Agent schreibt). Use for: Entscheidung, ob sich mehrere Agenten lohnen. Hersteller mit eigenem Produktinteresse.
- [Artikel: „Effective harnesses for long-running agents“, Anthropic Engineering, 26.11.2025](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)
  Fortschrittsdatei, Aufgabenliste und Git als Zustand über Sitzungen. Use for: Agenten, die länger als ein Kontextfenster arbeiten.

### Prüfen und Absichern (Details: `research/05-evals-sicherheit.md`)

- [Artikel: „Demystifying evals for AI agents“, Anthropic Engineering, 09.01.2026](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents)
  Gesamtmodell: Testfälle, Prüfer, mehrere Läufe, Regressionen. Use for: das Eval-Kapitel.
- [FAQ: „Evals FAQ“, Hamel Husain und Shreya Shankar](https://hamel.dev/blog/posts/evals-faq/)
  Fehleranalyse zuerst, Ja/Nein-Urteile, Richter prüfen. Use for: praktisches Vorgehen beim ersten Eval.
- [Artikel: „The lethal trifecta for AI agents“, Simon Willison, 16.06.2025](https://simonwillison.net/2025/Jun/16/the-lethal-trifecta/)
  Merkregel für die gefährliche Kombination aus privaten Daten, fremden Inhalten und Außenverbindung. Use for: Sicherheitsprüfung jedes Agenten vor dem Einbau.
- [Liste: OWASP Top 10 for Agentic Applications 2026](https://genai.owasp.org/resource/owasp-top-10-for-agentic-applications-for-2026/)
  Standardliste der Agenten-Risiken. Use for: Checkliste vor dem Einbau in eine App.

### Baukästen und Modellzugang per OAuth (Details: `research/06-baukaesten-oauth.md`)

- [Doku: „Sign in with ChatGPT“, OpenAI, seit 29.09.2026](https://developers.openai.com/siwc/quickstart.md)
  Der offizielle Weg, eigene Programme auf dem ChatGPT-Abo (Plus/Pro) laufen zu lassen, für Open-Source- und lokale Eigenprojekte. Use for: Anmeldung in allen Kursbeispielen. Dazu: [Anmeldeablauf](https://developers.openai.com/siwc/token-sharing-open-source/sign-in.md), [Aufrufe](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference.md), [Einschränkungen](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations.md), [DevKit für Node](https://github.com/openai/sign-in-with-chatgpt-devkit). Kein Kotlin-Paket, auf Android Eigenbau.
- [Doku: Codex-Anmeldung](https://learn.chatgpt.com/docs/auth.md) und [Codex SDK für TypeScript](https://github.com/openai/codex/blob/main/sdk/typescript/README.md), Version 0.160.0
  Fertiger Agent als Baustein am PC, erbt die Anmeldung der Codex-CLI. Use for: Arbeits-Agenten am PC. Läuft nicht auf Android (startet die CLI als Unterprozess).
- [Doku: Claude Agent SDK](https://code.claude.com/docs/en/agent-sdk/overview), Version 0.3.289, mit [Rechtslage](https://code.claude.com/docs/en/legal-and-compliance)
  Dieselbe Schleife wie Claude Code, als Bibliothek. Use for: Vergleich Eigenbau gegen fertigen Baukasten. Nutzung mit eigenem Abo im Eigenbau ist nicht ausdrücklich geregelt; nie die Claude-Code-Identität nachbauen.
- [Projekt: Koog, JetBrains, Version 1.3.0](https://github.com/JetBrains/koog)
  Agenten-Framework für Kotlin, nennt Android als Ziel, erlaubt eigene Basis-Adresse. Use for: Agent in der Android-App, nachdem die Schleife im Eigenbau verstanden ist.
- Eigene Vorarbeit im Repo: `~/proggs/best-practices/apis/cli-impersonation-subscription-auth.md` (§10 SIWC, §9 Nachbau) und `~/proggs/best-practices/apis/oauth-device-code.md`.
- Anthropic und OpenAI haben kein Kotlin-SDK, nur Java-SDKs ([anthropic-java 2.68.0](https://github.com/anthropics/anthropic-sdk-java), [openai-java 4.76.0](https://github.com/openai/openai-java)). Use for: Kotlin am PC; für Android ungeprüft.

## Wisdom (Communities)

- [Simon Willison, Blog, Schlagwort AI-Agents](https://simonwillison.net/tags/ai-agents/)
  Kurze, quellengestützte Einordnungen neuer Agenten-Funktionen. Use for: auf dem Laufenden bleiben, Sicherheitsfragen.
- [Koog im Kotlin-Slack, Kanal #koog-agentic-framework](https://kotlinlang.slack.com/messages/koog-agentic-framework/)
  Von JetBrains betreut. Use for: Fragen zu Agenten in Kotlin und auf Android.
- [GitHub Issues von Codex](https://github.com/openai/codex/issues) und [Claude Agent SDK](https://github.com/anthropics/claude-agent-sdk-typescript/issues)
  Use for: Fehler mit Anmeldung, `codex exec`, SDK-Verhalten.
- [MCP-Discord](https://discord.com/invite/modelcontextprotocol) und [GitHub Discussions](https://github.com/modelcontextprotocol/modelcontextprotocol/discussions)
  Use for: Fragen zu MCP-Servern und -Clients.
- [Claude Developers Discord](https://www.anthropic.com/discord), [OpenAI Developer Community](https://community.openai.com)
  Offizielle Foren. Inhalt und Signalanteil nicht geprüft.
- Frank hat sich noch nicht geäußert, ob er Communities beitreten will.

## Gaps

- Android-spezifische Agenten-Sicherheit: keine Primärquelle gefunden.
- Android-Tauglichkeit der Java-SDKs von Anthropic und OpenAI sowie des MCP-Kotlin-SDK: ungeprüft.
- Offizielle Faustregel „MCP oder eigene Werkzeuge“: gibt es nicht, nur aus der Architektur abgeleitet.
- Viele Zahlen aus Blogartikeln (Manus, Breunig, Chroma, Cognition, Hamel Husain) sind nur über Zusammenfassungen geprüft. Vor wörtlichem Zitat in einer Lektion im Volltext nachlesen.
