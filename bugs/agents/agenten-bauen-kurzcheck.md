# KI-Agenten selbst bauen Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT
> (`agenten-bauen.md`), nicht nur diese Kurzfassung. Stand 04.10.2026, 23:30 Uhr.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | 400 „tool_use ids were found without tool_result blocks“ | Jeder Werkzeugwunsch braucht sein Ergebnis, direkt in der nächsten `user`-Nachricht, Ergebnisblöcke zuerst | §1 |
| 2 | Agent läuft endlos, Kosten steigen | Eigenes Rundenlimit und Kostenlimit setzen, Standard ist unbegrenzt | §1 |
| 3 | 400 bei `tool_choice: any` oder `tool` | Bei den aktuellen Claude-Modellen nicht erlaubt, `auto` verwenden | §2 |
| 4 | Halber Werkzeugwunsch bei `max_tokens` | Nie ausführen, Grenze erhöhen und wiederholen | §1 |
| 5 | OpenAI: Werkzeugargumente sind ein String | `arguments` erst als JSON parsen; strict-Schema verlangt alle Felder als `required` | §2 |
| 6 | MCP-Beispiel aus Tutorial läuft nicht | v1-SDK/Sessions passen nicht zu 2026-07-28; TS v2: pro Anfrage eigene Instanz | §3 |
| 7 | Kosten springen plötzlich | Cache gebrochen: Werkzeuge oder Modell mitten in der Sitzung geändert, Zeitstempel im Präfix | §4 |
| 8 | Agent nutzt Bash trotz `allowedTools: ["Read"]` | `allowedTools` begrenzt `bypassPermissions` nicht, `disallowedTools` nehmen | §5 |
| 9 | Annahme „Befehle laufen in der Sandbox“ | Unter nativem Windows gibt es keine Sandbox; sie deckt nie Dateitools, MCP, Hooks | §5 |
| 10 | Agent gibt nach Lesen fremder Inhalte Daten preis | Indirekte Prompt Injection; Lethal Trifecta auflösen | §5 |
| 11 | Eval zeigt 0 % oder bestraft gute Lösungen | Erst Aufgabe und Prüfer prüfen; Ergebnis prüfen, nicht den Weg | §6 |
| 12 | Agent SDK / Codex SDK in Android-App | Geht nicht: beide brauchen die CLI als Unterprozess | §7 |
| 13 | Abo wird ignoriert, API wird abgerechnet | Gesetztes `ANTHROPIC_API_KEY` schlägt das Abo-Login | §7 |
| 14 | „refresh token was already used“ (Codex) | Eine `auth.json` pro Rechner, nie parallel oder mit altem Stand überschreiben | §7 |
| 15 | SIWC-Anfrage abgelehnt | `store:false`, `stream:true`; keine `temperature`, `max_output_tokens`, `previous_response_id` | §7 |
