# Anti-Halluzinations-Regeln fuer KI-Modelle Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektuere (`Read` mit
> `limit=80`). Volltext bei tieferem Bedarf (unten).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modell halluziniert Dateien/Funktionen/APIs | **Tool-first, nicht Memory-first** — erst Read/Grep/Bash, dann behaupten. „Die Datei hat immer recht, dein Gedaechtnis oft nicht." | §1 |
| 2 | Regel-Grundgeruest fuer AGENTS.md/CLAUDE.md | Die **5 Kern-Regeln** (I-don't-know · tool-first · kein Ketten-Raten · sofort zurueckziehen · Quelle nennen) als Block einsetzen | §1 |
| 3 | Modell raet statt zuzugeben | „Ich weiss es nicht" **explizit erlauben** (Abstention) — Raten erzeugt Halluzination | §2, §5 |
| 4 | Modell soll Fakten belegen | **Zitatpflicht** („According to…") — +~20 % Genauigkeit; „kein Beleg = keine Behauptung" | §2 |
| 5 | RAG/Kontext-Antwort | „Antworte **NUR** aus dem bereitgestellten Kontext; fehlt es, sag ‚insufficient data'" | §2, §3 |
| 6 | Staerkster Einzelhebel | **Grounding** (Kontext bereitstellen / Web-Suche / RAG) — Web-Suche allein −73–86 % | §3 |
| 7 | Strukturierte JSON-Ausgabe erzwingen | **Nur MIT Grounding** — Schema-Zwang OHNE Grounding macht es schlimmer (+10–15 pp) | §3 |
| 8 | Reasoning-Felder im Schema | Pflicht-Reasoning-Feld **zuerst** im Schema (Vonage: 23,7 % → 1,0 % Fehler) | §3 |
| 9 | Faktentreue Aufgabe | **Niedrige Temperatur** (0.0–0.3) — Praxis-Empfehlung (kein harter empirischer Beweis) | §4 |
| 10 | Wichtige Fakten absichern | **Chain-of-Verification** — Verifikationsfragen UNABHAENGIG beantworten (sonst Wiederholung) | §5 |
| 11 | Stark halluzinierendes/billiges Modell (GLM-Flash &Co.) | **Thinking AN + enger Scope + Tool-Zwang + Verifikation durch staerkeres Modell** | §6 |
| 12 | Regel MUSS sicher greifen | Prompt-Regeln sind **Bitten**, keine Gesetze — kritisches per **Hook/Code** erzwingen | §7 |
| 13 | „Confidence" des Modells | Confidence ≠ Accuracy (51,4 % selbstsicherer Antworten widerlegt) — nicht darauf verlassen | §5, §8 |
