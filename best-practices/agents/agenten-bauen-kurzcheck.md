# KI-Agenten selbst bauen Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT
> (`agenten-bauen.md`), nicht nur diese Kurzfassung. Stand 04.10.2026, 23:30 Uhr.

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
