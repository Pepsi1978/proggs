# Researcher & Internet-Recherche Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | parallele Researcher | max ~5–7 gleichzeitig (RPM/429); Continuous-Spawning statt Wellen | Maximale Anzahl paralleler Researcher |
| 2 | 429 / Rate Limit | Exponential Backoff + `retry-after` respektieren | Exponential Backoff & Retry |
| 3 | Scope je Researcher | enge Prompts, ~15 Web-Fetches als RPM-Schutz | Scope-Begrenzung |
| 4 | Ergebnisse sichern | Checkpointing (lossless in Datei auslagern) | Checkpointing |
| 5 | Findings | NICHT kappen — alle dokumentieren (1M-Kontext) | Scope-Begrenzung |
| 6 | Uebersetzer ≠ Researcher | Uebersetzungs-Agenten NICHT drosseln (kein Web/RPM) | Warum drosseln |
