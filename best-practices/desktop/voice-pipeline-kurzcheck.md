# Voice-Agent-Sprachpipeline (Spracheingabe → Verstehen → Sprachausgabe) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Session-Lebenszyklus bauen | FSM Idle→Listening→Thinking→Speaking, Timer nur im Idle | §1 |
| 2 | Follow-up-Fenster setzen | Nach JEDER Antwort oeffnen, ab Antwort-Ende zaehlen | §1 |
| 3 | Im Fenster begonnene Rede | IMMER verarbeiten, nie still verwerfen | §1 |
| 4 | Stille-Pause dimensionieren | 300–550 ms Dialog, 1000–2000 ms Diktat + semantisches Netz | §2 |
| 5 | Endpointing robust machen | Drei Schichten: Energie + Transkript + LLM-Check (FERTIG/WEITER) | §2 |
| 6 | Endlos-Aufnahme verhindern | Max-Utterance-Deckel 15–30 s, finalisieren UND verarbeiten | §2 |
| 7 | Daueraufnahme stabil halten | EINE WaveInEvent-Instanz, Watchdog, Stop/Dispose serialisieren | §3 |
| 8 | Windows-Audio einrichten | AGC/Boost/Ducking/Exclusive aus (mmsys.cpl, einmalig) | §3 |
| 9 | Latenz minimieren | Budget < 1 s, Zwischenschritte aufs kleinste Modell/Effort | §4 |
| 10 | Pipeline schnell machen | Parallelisieren statt verketten, Streaming + Ueberlappung | §4 |
| 11 | Gapless-Audio sichern | EIN offener Output, BufferedWaveProvider, LINEAR16/PCM | §4 |
| 12 | Barge-in ermoeglichen | Stufe 1 Mute-Trade-off, Stufe 3 echtes Barge-in mit AEC | §5 |
| 13 | STT-Requests fuer Voice | `language=de` explizit, Timeout 5–10 s, EIN HttpClient | §6 |
| 14 | Frueherkennung sichern | Jeden FSM-Uebergang + Stufen-Latenz als CHECKPOINT loggen | §7 |
| 15 | Aufnahme nicht von aussen abwuergen | Busy-Status (Aufnahme/Transkription) ueber lokalen Endpoint exponieren; Deploy/Rebuild/Kill wartet auf Ruhe | §8 |
| 16 | Hybrid-Diktat (Live-Vorschau + finale Engine) | Vorschau getrennt vom Zielfeld; `previewActive`-Riegel: nach Stopp schreibt nur die finale Engine; Fallback mit Hinweis | §9 |
