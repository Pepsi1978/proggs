# STT-Provider (Speech-to-Text, Deutsch) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

Stand: 21.10.2026 · Quelle: Studie `Recherchen/stt-vergleich` (10 Berichte + eigener Messplatz)

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Deutsch-Qualität eines STT-Modells beurteilen | Nur HF Open ASR Leaderboard `multilingual_de.csv` (unabhängig); Artificial Analysis ist englischlastig; Herstellerzahlen nie quer vergleichen | §1 |
| 2 | Groq Turbo vs. large-v3 | large-v3 auf Deutsch belegt besser (HF CV 4,79 vs 8,58), gleiches Freikontingent; ggf. erst in der Groq-Konsole für die Org freischalten | §2 |
| 3 | Großes Gratis-Kontingent gesucht | Dauerhaft groß nur Groq (8 h/Tag) und Cloudflare Turbo (≈ 214 Min./Tag); alle besseren Anbieter nur einmalige Startguthaben | §3 |
| 4 | Gemini 3.5 Transcribe gratis | Free Tier = 3 RPM / 25 RPD (429-Meldung 21.10.2026) → nur als Bezahlweg oder mit Rückfall | §4 |
| 5 | Eigennamen / Fachbegriffe | Vokabelliste mitschicken (Whisper `prompt`, Gemini `custom_vocabulary`, Deepgram je Begriff ein `keyterm`) — halbiert die Eigennamen-Fehler | §5 |
| 6 | Stille / leere Aufnahme | IMMER VAD-Sperre vor dem Upload: Whisper+Prompt gibt den Prompt aus, Gemini-LLM erfindet Sätze | §6 |
| 7 | LLM als STT (generateContent mit Audio) | Nur mit Stille-Sperre und Verbatim-Anweisung; dedizierte Transcribe-Modelle bevorzugen | §6 |
| 8 | Neuen Anbieter einbauen | Datei-Upload mit einem REST-Aufruf, Rückfall auf Groq bei Netz/429/5xx/leerer 200 | §7 |
| 9 | Messung eigener Kandidaten | Messplatz `Recherchen/stt-vergleich/messung/messen.py` + `seite/bauen.py` (WER mit deutscher Zahlen-/Abkürzungs-Normalisierung) | §8 |
| 10 | Datenschutz | Deepgram `mip_opt_out=true`; Gemini Free Tier: Apps für Dritte nur Paid; Mistral Free Mode trainiert | §9 |
| 11 | Lokal / offline | Parakeet v3 (sherpa-onnx/onnx-asr) ist auf Deutsch nur etwa Turbo-Niveau, bei Zahlen und Rauschen schwächer | §10 |
