# RESUME: Best-Practices apis/ — FERTIG (Stand 2026-06-09)

Bug-Almanache `bugs/apis/` (13) und Best-Practices `best-practices/projekt-code/apis/` (14)
sind komplett und wechselseitig verlinkt. `python3 bugs/check-coupling.py` → alle 13 apis/-Paare [OK].

## Erledigt
- Alle 14 BP-Dateien vorhanden (13 als 1:1-Mirror zu bugs/apis/ + multi-provider, ungepaart):
  api-integration-general, multi-provider, openai-api, anthropic-api, google-gemini-api,
  groq-api, openrouter-api, xai-grok-api, mistral-api, deepseek-api, local-openai-compatible,
  other-llm-apis, oauth-device-code, cli-impersonation-subscription-auth. (#46656/#46657)
- Bidirektionale Bezugs-Tabellen: BP-Dateien tragen "🔗 Bezug zum Bug-Almanach" (Mapping-Tabelle),
  Bug-Dateien tragen "🔗 Bezug zu Best Practices" (Verweis auf die BP-Datei, DRY ohne Duplikation).
  Veraltete "noch keine best-practices"-Header in den Bug-Dateien korrigiert. (#46658)
- best-practices/projekt-code/README.md um apis/-Kategorie erweitert (Baum + Kategorien-Liste). (#46659)

## Hinweis (anderer Scope — NICHT apis/)
check-coupling.py meldet noch 3 vorbestehende Drift-Faelle ausserhalb apis/:
- desktop/best-practices-groq-transkription.md (BP ohne 🔗-Tabelle)
- assets/icon-building.md + best-practices-icon-building.md (beide ohne 🔗-Tabelle)
- desktop/wake-word.md + best-practices-wake-word.md (beide ohne 🔗-Tabelle)
Diese gehoeren zu desktop/ bzw. assets/ und waren schon vor der apis/-Arbeit offen.
