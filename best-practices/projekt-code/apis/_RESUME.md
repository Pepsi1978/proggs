# RESUME: Best-Practices apis/ — Stand 2026-06-08

Bug-Almanache `bugs/apis/` sind komplett (13 Dateien, #46647).

## BP-Dateien FERTIG (mit dedizierter Research):
- best-practices-api-integration-general.md (Resilienz/Rate-Limit/SSE/Timeout/Pooling/Secrets)
- best-practices-multi-provider.md (Architektur/Gateway/Fallback)
- best-practices-openai-api.md
- best-practices-anthropic-api.md
- best-practices-oauth-device-code.md

## BP-Dateien NOCH OFFEN (Research stoppte bei 99% Token / 2 Researcher rate-limited):
Gemini, Groq, OpenRouter, xAI Grok, Mistral, DeepSeek, lokale Server, weitere APIs (Cohere/Together/
Fireworks/Perplexity/Bedrock/Azure/Cerebras/Vertex/HF), CLI-Impersonation.
→ Prävention steht bereits in den FIX-Feldern der jeweiligen `bugs/apis/<x>.md`. Beim Fortsetzen
entweder daraus ableiten ODER pro Anbieter 1 Researcher (max 5 parallel wegen RPM).

## NOCH ZU TUN beim Fortsetzen:
- Fehlende BP-Dateien schreiben (1:1 Mirror zu bugs/apis/ — Franks Wunsch).
- Bidirektionale Bezugs-Tabellen in die 5 bestehenden BUG-Dateien einfügen (BP↔Bug), dann `python3 bugs/check-coupling.py`.
- best-practices/projekt-code/README.md + apis/-Sektion eintragen.
