# Vibe-Coding mit OpenCode — Modelle, Thinking & Prompts Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) |
|---|-----------|--------------------------|
| 1 | Modell macht beim Vibe-Coding viele Fehler | **Thinking/Reasoning AKTIVIEREN** — der grosse Sprung ist *aus → an* (Pass-Rate +~14 Punkte) |
| 2 | Welche Thinking-Stufe fuer Code-Editing? | **`medium`** ist der Sweet Spot. `high`/`xhigh` bringen beim Code-Editing fast nichts (+58 % Token, 0 Accuracy) und over-editen. (Mathe ≠ Code: bei Mathe lohnt `high`.) |
| 3 | GLM-5.2 "kann kein Thinking" | **Falsch** — GLM-5.2 hat Default + Max Effort. In `opencode.json` `reasoningEffort` explizit setzen (sonst laeuft es ohne) |
| 4 | Reasoning-Modell aendert zu viel (Over-Editing) | Im Prompt **explizit "minimale Aenderung, bestehenden Code/Signaturen erhalten"** → dann editiert es sogar weniger |
| 5 | Modellwahl per Benchmark | **Nicht verlassen** — Harness/Prompt verschiebt dasselbe Modell um 19 %→73 %; Benchmarks kontaminiert + manipulierbar; "offiziell top" ≠ Praxis |
| 6 | Guenstiger Reasoning-Default fuer Vibe-Coding | **MiMo-V2.5-Pro** (Xiaomi, $1/$3, 1M Kontext, #1 Coding-Volumen) oder **MiniMax M3** |
| 7 | Ultra-Long-Horizon-Coding (Stunden) | **GLM-5.2** (stark, aber token-hungrig ~42k) oder MiMo + "MiMo Code"-Harness |
| 8 | Wie viel Tooling/Harness? | **Wenig generische Tools > viele spezialisierte** (Vercel: 15→2 Tools = 80 %→100 %) — aber nur ueber einem Modell-"Capability Floor" |
| 9 | Prompt fuer wenig Fehler | Persona + praezise Spec (benannte Komponente + Signatur + State-Modell) + Plan/Act-Bestaetigung + schrittweise |
| 10 | Android-KI-Code reviewen | Checkliste: Coroutine-Scope (kein `GlobalScope`), kein `!!`, aktuelle APIs (StateFlow statt LiveData/AsyncTask), Recomposition (stable Lambdas) |
