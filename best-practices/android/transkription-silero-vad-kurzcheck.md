# Transkription mit Silero-VAD-Sprachaktivität (Android, Groq Whisper) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Bei Arbeit an Aufnahme, Aufnahmeende oder Halluzinationsfiltern einer Android-App den Volltext lesen: `transkription-silero-vad.md`. Fertiger Baustein: Modul M1.4 Transkriptionsmodul.

## ⚡ Kurzcheck

| # | Situation | Regel | § |
|---|---|---|---|
| 1 | Neue Android-App mit Groq-Diktat | M1.4 einbauen, nicht neu bauen | 1 |
| 2 | Aufnahmeende im freihändigen Modus | Silero-Sprachrahmen statt Pegel; 4,5 s ohne Stimme → Ende | 3 |
| 3 | Lautes Umfeld (Auto, Lüfter) | Pegel taugt nicht: Geräusch liegt über der Pegelschwelle. Stimme zählt, nicht Lautstärke | 2 |
| 4 | Was filtert, was erkennt | Schichten 1–4 filtern Halluzinationen, Silero erkennt nur Sprachaktivität | 2 |
| 5 | Silero fällt aus | nie die Aufnahme opfern: Rückfall auf den Pegel, Filter laufen weiter | 5 |
| 6 | Groq bleibt Anbieter | Silero liefert keinen Text und erkennt nicht, *wer* spricht | 2 |
| 7 | Schwellen ändern | nur mit neuen Testfällen aus `vad_testfaelle.py` | 7 |

---
