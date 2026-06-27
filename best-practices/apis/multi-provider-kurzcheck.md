# Provider-agnostische LLM-API-Architektur Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Mehrere Provider anbinden | Unified-Interface/Adapter (Vercel AI SDK, Bedrock Converse, LiteLLM) | §A |
| 2 | Gateway ja/nein | Erst ab >1 Provider ODER >einige 100 $/Monat; darunter direkte SDKs | §B |
| 3 | Fallback bauen | Drei GETRENNTE Typen: general / content_policy / context_window | §C |
| 4 | Resilienz | retries + timeout + allowed_fails + cooldown + geordnete Fallback-Liste | §C |
| 5 | Konfiguration | Modelle/Keys/Endpunkte aus Config/Env; Routing-Strategie explizit wählen | §D |
| 6 | Modellwahl | Capability-Detection (Tools/JSON/Image?) + Kosten-/Latenz-Routing nach Task | §E |
