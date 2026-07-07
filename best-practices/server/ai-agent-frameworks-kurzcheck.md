# Serverseitige autonome KI-Agenten Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Thema | Best Practice (Kurzform) | Almanach |
|---|-------|--------------------------|----------|
| 1 | Framework-Wahl | Klein+robust+lesend → **eigene schlanke Tool-Loop** ODER **Pydantic-AI** (typisiert, leicht); **LangGraph** erst bei echter Graph-/Checkpoint-Komplexitaet | §7/§8 |
| 2 | Single vs. Multi | Mit EINEM Agenten + guten Tools starten; Multi erst wenn Kontext/Capabilities es erzwingen | §1 (Loop) |
| 3 | Tool-Trennung | Lesende vs. schreibende Tools strikt trennen, Scope serverseitig validieren | §5 |
| 4 | Schreib-Tools | Idempotent (Idempotency-Key) — Agent ruft sie im BG mit Retry | §5.2 |
| 5 | Tool-Schema | Striktes JSON/Zod-Schema (`strict`), wenige klar benannte Tools, gute `description` | §4.1 (Tool-Protokoll) |
| 6 | Tool-Fehler | Als `tool_result`-Fehlertext ans LLM zurueck (es kann korrigieren), nie Crash; Timeout pro Tool | §3.2/§3.3 |
| 7 | Hard-Stop | Deterministischer Abbruch (max Turns/Zeit) UNABHAENGIG vom LLM — Notbremse | §1.1 |
| 8 | Kosten | Token-Budget das STOPPT (nicht nur warnt); usage.total_tokens abziehen | §2.1 |
| 9 | Kontext | Trimming (billig) vor Summarization; Kontext „informativ aber knapp"; Pointer statt Voll-Output | §2.2 |
| 10 | Memory-Zugriff | Read/Write-Scopes getrennt, Least-Privilege, kein Secret im Kontext, Timeouts | §5 |
| 11 | Architektur | Schlanker Intent-Router (klein/lokal) ↔ staerkerer lesender Server-Agent; separate Kontextfenster; Modell-pro-Rolle | §5 (Concurrency) |
| 12 | Observability | Strukturierte JSON-Logs (Eingaben/Entscheidungen/Tool-Calls) + OTel-Tracing + Evals + Token/Kosten-Tracking | (Direktive #2) |
| 13 | Intent-Checkpoints | erwartet-vs-tatsaechlich loggen (eigener Kanal) — bestaetigen, ob Logik wie gemeint ankommt | (Live-Logik-Sonden) |
| 14 | Prompt-Injection | Gespeicherte Memory-Inhalte als UNTRUSTED; beim Recall als DATEN markieren; Lethal Trifecta brechen | §6.1 |
| 15 | async | Blockierende Calls NIE im async-Loop (`to_thread`/`run_in_threadpool`) | §3.1 |
