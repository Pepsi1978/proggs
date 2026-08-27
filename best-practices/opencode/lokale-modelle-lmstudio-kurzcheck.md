# Lokale Modelle über LM Studio in OpenCode — Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.
>
> **Stand:** 2026-08-27, gemessen mit OpenCode CLI 1.18.23 und der lms-CLI (LM Studio 2026-08) auf
> macOS/Apple Silicon (24 GB Unified Memory). Gegenstück: `bugs/opencode/opencode-cli.md` §15.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Frage / Situation | Sofort-Regel |
|---|-------------------|--------------|
| 1 | ⭐ Wie viel Kontext braucht OpenCode mindestens? | **≥ 32768 Tokens.** Der Systemprompt allein belegt rund **22000**. LM Studios JIT-Vorgabe (oft **4096**) reicht nicht — die erste Anfrage bricht ab. |
| 2 | ⭐ Welche Zahl gehört in `provider.lmstudio.models.<id>.limit.context`? | **Genau der Wert aus `lms ps`** (`contextLength`). Größer → LM Studio lehnt den Prompt ab; kleiner → OpenCode hält den Kontext für fast voll und komprimiert endlos im Kreis. |
| 3 | ⭐ Darf man ein geladenes Modell zum Vergrößern entladen? | **Nur nach `lms load … --estimate-only`.** Meldet es „will fail to load", bleibt alles unangetastet — entladen wäre nicht umkehrbar. Nur bei „may be loaded" entladen + neu laden, bei Fehlschlag sofort den alten Zustand wiederherstellen. Ist **nichts** geladen: trotz negativer Schätzung versuchen — sie ist messbar zu pessimistisch, und verlieren kann man nichts. |
| 4 | ⭐ Passt das Modell überhaupt in den Speicher? | Die Schutzschranken bewerten den **Gesamtbedarf**, nicht die Kontextlänge — der Schätzwert ist für 4096 und 65536 identisch, und er ist zu hoch (geschätzt 20,97 GiB, real belegt 14,98 GiB). Faustregel Apple Silicon: Schätzwert < ~55 % des Gesamtspeichers passt bei `mode: medium`. |
| 4a | ⭐ **Format prüfen, bevor man ein Modell für Agentenbetrieb wählt** | `lms ls --json` → `format`. **`safetensors` (MLX) ignoriert `--context-length`** und bleibt bei 4096 — gemessen mit 8192/16384/65536. In der LM-Studio-Oberfläche geht es trotzdem, über den Server nicht. **Für OpenCode nur GGUF.** |
| 5 | ⭐ Zwei gleich benannte Modelle im Picker | Verschiedene Quantisierungen desselben Modells (`unsloth/…` 10,9 GB vs. `qwen/…` 16,1 GB). **Herausgeber in den Anzeigenamen ziehen** (`publisher` aus `lms ls --json`), sonst startet man blind die zu große Variante. |
| 6 | Teilt `parallel` das Kontextfenster auf? | **Nein** — gemessen: `--context-length 16384 --parallel 4`, Prompt mit 9123 Tokens geht durch. Den Wert aus `lms ps` **nicht** durch `parallel` teilen. |
| 7 | ⭐ Kontext ist knapp — was zuerst wegnehmen? | `OPENCODE_DISABLE_EXTERNAL_SKILLS=1` setzen: die externen `~/.claude/skills` kosten gemessene **~14000 Token pro Anfrage**. Bei Cloud-Modellen egal (Cache + großes Fenster), lokal zwei Drittel des Fensters. |
| 8 | Wie viel Ausgabe reservieren? | `limit.output` klein halten: `min(8192, max(2048, context/8))`. OpenCode rechnet die Obergrenze als bereits verbraucht — eine großzügige Reservierung frisst sichtbar Kontext, bevor ein Token geschrieben ist. |
| 9 | Server läuft, Reiter bleibt leer | `lms server start` kehrt zurück, **bevor** der HTTP-Endpunkt Anfragen annimmt. Nach dem Start `GET /v1/models` mit **Wiederholversuchen** (4× im Abstand von 1,5 s) abfragen. |
| 10 | Modell taucht nicht als Chat-Modell auf | Einträge mit `embed` im Namen aussortieren — Embedding-Modelle taugen nicht als Agent-Modell. |

**Empfohlener Ablauf beim Start:** `lms server start` → `lms ps` lesen → Kontext < 32768? → ist etwas
geladen, erst `--estimate-only` fragen und nur bei „may be loaded" entladen; ist nichts geladen,
direkt laden (Wunschgröße, dann Minimum) → **nachprüfen, ob `lms ps` den angeforderten Wert meldet**
(sonst MLX-Laufzeit, siehe Zeile 4a) → tatsächlichen Wert aus `lms ps` in `limit.context` schreiben →
bleibt es zu klein, im **Klartext** melden und anhalten, statt OpenCode in den Serverfehler laufen zu
lassen.
