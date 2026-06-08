# Session Handoff — 2026-06-08

## Ziel
Bug-Almanach-Recherche fuer API-Integration + Einlog-/Auth-Mechanismen (neue Kategorie),
DANACH Best-Practices-Gegenstuecke (zweite Seite der Medaille). Beides als 1:1-Mirror
pro Anbieter (Franks Wunsch: "genau die gleichen APIs wie beim Bug").

## Aktueller Status
- ERLEDIGT #46647: 13 Bug-Almanache in bugs/apis/ (openai, anthropic, google-gemini, groq,
  openrouter, xai-grok, mistral, deepseek, local-openai-compatible, other-llm-apis,
  oauth-device-code, cli-impersonation-subscription-auth, api-integration-general) + README-Kategorie.
  Recherche: 13 Researcher, 0 Abstuerze. 8 GitHub-Issues per gh hart verifiziert.
- ERLEDIGT #46648: 5 Best-Practices-Dateien in best-practices/projekt-code/apis/
  (api-integration-general, multi-provider, openai-api, anthropic-api, oauth-device-code) + _RESUME.md.
  Research dafuer: 5 Researcher erfolgreich.
- OFFEN: BP-Dateien fuer Gemini, Groq, OpenRouter, xAI Grok, Mistral, DeepSeek, lokale Server,
  weitere APIs, CLI-Impersonation (2 Researcher rate-limited, Rest auf Token-Wunsch gestoppt).

## Relevante Dateien
- bugs/apis/*.md (13) — Bug-Almanache, fertig
- best-practices/projekt-code/apis/*.md (5 BP + _RESUME.md) — _RESUME.md hat den genauen Offen-Stand
- bugs/README.md — apis/-Kategorie eingetragen

## Getroffene Entscheidungen
- bug-almanac-guard NICHT um apis/ erweitert: API-Integration hat kein sauberes Datei-Pattern;
  Enforcement per Endung wuerde Fehlalarme bei jeder .cs/.py erzeugen. Nur via Index/Keywords auffindbar.
- BP fuer fehlende Anbieter: Praevention steht bereits in den FIX-Feldern der bugs/apis/-Almanache.

## Fehlgeschlagene Ansaetze (WICHTIG)
- 7 Researcher gleichzeitig in Welle 2 der BP-Recherche -> 2 rate-limited ("Server is temporarily
  limiting requests", subagent_tokens 0). Bei Fortsetzung: max 5 parallel, 429-Backoff.
- Researcher schrieben mehrfach in dieselbe agent-writeback-researcher.json und ueberschrieben sich
  (offener Harness-Bug; Vorschlag: pro-Agent-eindeutiger Sentinel-Pfad).

## Naechste Schritte (priorisiert)
1. Fehlende BP-Dateien schreiben (best-practices-<x>.md fuer gemini, groq, openrouter, xai-grok,
   mistral, deepseek, local-openai-compatible, other-llm-apis, cli-impersonation) — aus FIX-Feldern
   der jeweiligen bugs/apis/<x>.md ableiten ODER je Anbieter 1 Researcher (max 5 parallel).
2. Bidirektionale Bezugs-Tabellen in die 5 fertigen UND neuen BUG-Dateien (BP<->Bug), dann
   python3 bugs/check-coupling.py.
3. best-practices/projekt-code/README.md um apis/-Sektion erweitern.

## Offene Fragen
- Keine. Frank war bei 99% Token -> Session-Backup, dann /clear + restore.

## Anker
- Branch: main
- Letzte Commits:
2eeb1679a #46648 - Best-Practices apis/: BP-Gegenstuecke + RESUME
a29611132 #46653 - VoiceAgent: freie Weckwort-Eingabe (Parallel-Session)
132576e40 #46652 - VoiceAgent: Computer Use Stufe 2 (Parallel-Session)
9dee73ca2 #46645 - VoiceAgent: Modell-Auswahl Dropdown (Parallel-Session)
