# Cowork (Desktop-App) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Voraussetzung | Nur Desktop (macOS/Windows), bezahltes Abo; App offen + Rechner wach, sonst stoppt die Aufgabe | §1 |
| 2 | Einrichtung | `/setup-cowork`; Connectors aktivieren; Arbeitsort wählen (Ordner ODER Projekt) | §1 |
| 3 | Ordner vs. Projekt | Memory nur in **Projekten**, nicht über Standalone-Sessions | §1 |
| 4 | Sicherheitsmodus | "Ask before acting" als Standard; vor endgültigem Löschen fragt Claude immer | §1 |
| 5 | Skill-Beschreibung | Claude.ai-Limit **200 Zeichen** — Trigger knapp & keyword-stark | §2 |
| 6 | Eigene Skills | Customize > Skills > ZIP-Upload (Ordnername = name-Feld), dann Toggle aktivieren | §2 |
| 7 | Plugins | Nur in Cowork/Code, **nicht in Chat**; eigene Git-Repos als Marketplace nutzbar | §2 |
| 8 | Connectors | Claude erbt deine Quellsystem-Rechte; Gmail liest/Entwürfe, **kein Versand** | §3 |
| 9 | MCP in Cowork | Remote-Connectors laufen über Anthropics Cloud → eigene Server: Anthropic-IPs allowlisten | §3 |
| 10 | Datei-Arbeit | Dedizierter Arbeitsordner; sensible Ordner nicht verbinden; Mount `read-write-no-delete` als Schutz | §4 |
| 11 | Scheduled Tasks | Prompt **selbst-enthaltend**; Catch-up-Fallstrick → Zeit-Guardrails in den Prompt | §5 |
| 12 | Live-Artefakte | Nutzen Connectors **ohne Rückfrage**; lokal, (noch) nicht teilbar | §5 |
| 13 | Tool-Hierarchie | Connector → Claude in Chrome → Computer Use (in dieser Reihenfolge) | §6 |
| 14 | Computer Use | Nur Pro/Max, **keine Sandbox**; Links aus Mail/Doku nie per Computer-Use klicken | §6 |
| 15 | Grenzen | Compliance-Blindspot (nicht in Audit-Logs); deutlich höherer Usage-Verbrauch als Chat | §7 |
| 16 | Git push aus Cowork (dauerhaft) | Kein nativer Push-Weg/Secret-Store/Startup-Hook → Token in `.git/credentials` (im Mount, nie committet, relativer Pfad) + `credential.helper store` lokal; Remote NICHT auf SSH/Token-URL ändern (geteilte `.git/config`) | §3a |
