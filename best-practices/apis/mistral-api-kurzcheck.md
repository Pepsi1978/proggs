# Mistral API Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modell waehlen | Datierte Version pinnen (`-2512`), `-latest` nur Dev | §1 |
| 2 | Strukturierte Ausgabe | `json_schema` statt `json_object` (Schema-garantiert) | §2 |
| 3 | Tool-Calling | Pro Call eine `tool`-Antwort, IDs durchreichen; rekursiv | §3 |
| 4 | Code-Completion | FIM am Codestral-Endpunkt + eigener Key | §4 |
| 5 | Token sparen | `prompt_cache_key` bei gemeinsamem Praefix (10 % Kosten) | §5 |
| 6 | Rate Limits | Pro Workspace; bei 429 `Retry-After` + Backoff | §6 |
| 7 | SDK-Setup | `RetryConfig`; Stream-Timeouts hoch (~10 min Inaktivitaet) | §7 |
| 8 | Streaming/OCR | `stream:true` im Body; `include_usage`; OCR mit json_schema | §8 |
