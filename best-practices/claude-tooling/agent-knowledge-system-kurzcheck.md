# Agenten-Wissens-/Best-Practices-/Lern-System Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Wissen dem Agenten zur richtigen Zeit geben | Progressive Disclosure: Tier-1 description (immer), Tier-2 Volltext (bei Bedarf), Tier-3 Details (on-demand) = euer Digest-Modell | §1 |
| 2 | Wie wird Wissen ausgeloest? | Doppel-Trigger: deterministischer Datei-Trigger + semantischer "Agent-Requested"-Trigger (description-basiert) | §2 |
| 3 | Wissen aktivieren (Hooks) | Nested Schema, beide Speicherorte spiegeln, Defense-in-Depth (mehrere Schichten + Drift-Detektor) | §3 |
| 4 | Wissens-Eintraege governen | Pro Eintrag: `confidence` + `last_verified` + `version_anchor`; veraltete sichtbar markieren | §4 |
| 5 | Aus Erfahrung lernen | Nur ECHTE Signale speichern (nie Platzhalter); Eval-getriebene Pflege; Rueckschreib-Pflicht | §5 |
| 6 | Self-Tests/Wartung | 0 Fehlalarme (sonst wertlos), gebuendelt, im SessionStart/PreCommit | §6 |
| 7 | Lern-Kreislauf | Jeder Bug → Almanach + Fall-DB, AUCH Harness-Bugs; geschlossener Loop | §7 |
| 8 | Langfristig | Graph-Memory (Bug↔RootCause↔Fix↔Version) statt flacher Liste | §8 |
