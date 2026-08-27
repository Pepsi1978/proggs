# Lokale Modelle über LM Studio in OpenCode — Volltext

> **Stand:** 2026-08-27. Gemessen mit **OpenCode CLI 1.18.23** und der **lms-CLI (LM Studio 2026-08)**
> auf macOS/Apple Silicon (M4, 24 GB Unified Memory). Alle Zahlen stammen aus eigenen Messungen an
> diesem Rechner, nicht aus der Dokumentation.
>
> Gegenstück (was schiefgeht und warum): `bugs/opencode/opencode-cli.md` §15.
> Umsetzung im Code: `OpenLauncherMac/OpenLauncher/Services/LmStudioService.swift` und
> `OpenLauncherService.buildLmStudioPreloadScript`.

---

## 1. Warum lokale Modelle in OpenCode ein Sonderfall sind

OpenCode kennt LM Studio nicht ab Werk. Der Weg führt über den OpenAI-kompatiblen Server
(`http://localhost:1234/v1`) als eigener Provider:

```jsonc
"provider": {
  "lmstudio": {
    "npm": "@ai-sdk/openai-compatible",
    "name": "LM Studio (lokal)",
    "options": { "baseURL": "http://localhost:1234/v1", "apiKey": "lm-studio" },
    "models": {
      "unsloth/qwen3.8-27b": {
        "name": "Qwen3.8 27B - unsloth (lokal)",
        "tool_call": true,
        "limit": { "context": 65536, "output": 8192 }
      }
    }
  }
}
```

`apiKey` muss gesetzt sein, auch wenn LM Studio ihn nicht prüft — das SDK verlangt einen nicht-leeren
Wert. `tool_call: true` ist Pflicht, sonst behandelt OpenCode das Modell als reines Chat-Modell.

Der entscheidende Unterschied zu Cloud-Modellen: **`limit.context` ist bei Cloud-Modellen eine
Eigenschaft des Modells, bei LM Studio eine Eigenschaft des LADEVORGANGS.** Dasselbe Modell hat je
nach `lms load --context-length` 4096 oder 262144 Tokens. Die Konfig muss dem folgen.

---

## 2. Die Mindestgröße: 32768 Tokens

OpenCodes Systemprompt belegt allein rund **22000 Tokens**. Dazu kommen Werkzeugschemata, die
Projekt-`AGENTS.md` und die eigentliche Frage. Unter 32768 Tokens ist keine sinnvolle Sitzung
möglich; darunter bricht schon die erste Anfrage ab:

```
{"message":"The number of tokens to keep from the initial prompt is greater than the context length.
Try to load the model with a larger context length, or provide a shorter input"}
```

LM Studio lädt ein Modell per JIT mit seiner eigenen Vorgabe — häufig **4096 Tokens**. Wer den Wert
aus `lms ps` ungeprüft in die Konfig schreibt, baut sich genau diesen Fehler ein.

**Praktische Wunschgröße: 65536 Tokens**, gedeckelt auf `maxContextLength` des Modells.

### 2.1 Den größten Hebel zuerst ziehen

Die externen Skills aus `~/.claude/skills` werden mit ihrer kompletten Beschreibung in **jede**
Anfrage eingebettet und kosten gemessene **~14000 Token**. Bei einem Cloud-Modell mit 200k Fenster
und Prompt-Cache fällt das kaum auf; bei einem lokalen 32k-Fenster sind das zwei Drittel, bevor die
erste Frage gestellt ist.

```sh
export OPENCODE_DISABLE_EXTERNAL_SKILLS=1   # prozesslokal, nur für diese Sitzung
```

### 2.2 Ausgabe-Obergrenze klein halten

OpenCode reserviert `limit.output` im Kontextbudget und zeigt es als bereits verbraucht an. Bewährt:

```
output = min(8192, max(2048, context / 8))
```

8192 reicht für jede realistische Antwort inklusive Werkzeugaufrufen.

---

## 3. Die Speicher-Schutzschranken von LM Studio

`~/.lmstudio/settings.json` enthält:

```json
"modelLoadingGuardrails": { "mode": "medium", "customThresholdBytes": 4294967296, "alwaysAllowLoadAnyway": true }
```

**Der wichtigste Befund:** Die Schranke bewertet den **Gesamtbedarf des Modells**, nicht die
Kontextlänge. Gemessen an `qwen/qwen3.8-27b`:

| Kontextlänge | Geschätzter Gesamtbedarf | Urteil |
|--------------|--------------------------|--------|
| 16384 | 20,97 GiB | will fail to load |
| 32768 | 20,97 GiB | will fail to load |
| 49152 | 20,97 GiB | will fail to load |

Der Schätzwert ändert sich über den ganzen Bereich **nicht**. Kontext kleiner zu wählen hilft also
nicht, wenn schon die Gewichte nicht passen. Auf 24 GB Gesamtspeicher wird ein Schätzwert von
20,97 GiB (≈ 87 %) bei `mode: medium` abgelehnt.

Die 4-bit-Variante desselben Modells passt dagegen:

| Modell | Größe auf Platte | Schätzwert | Urteil bei 65536 Tokens |
|--------|------------------|------------|--------------------------|
| `qwen/qwen3.8-27b` | 16,1 GB | 20,97 GiB | will fail to load |
| `unsloth/qwen3.8-27b` | 10,9 GB | 10,18 GiB | may be loaded ✅ |
| `openai/gpt-oss-20b` | 12,1 GB | 11,28 GiB | may be loaded ✅ |

**Faustregel Apple Silicon:** Schätzwert unter ~55 % des Gesamtspeichers passt bei `mode: medium`.

### 3.1 Vor dem Entladen IMMER fragen

`--estimate-only` lädt nichts, sondern gibt nur das Urteil zurück:

```sh
lms load <id> --context-length 65536 -y --estimate-only
# ... Estimate: This model will fail to load based on your resource guardrails settings.
# ... Estimate: This model may be loaded based on your resource guardrails settings.
```

Der Exit-Code ist in **beiden** Fällen 0 — nur der Text unterscheidet sich, also auf
`will fail to load` prüfen. Ein echter `lms load` liefert dagegen sauber Exit-Code 1 bei Fehlschlag.

Das ist die entscheidende Vorsichtsmaßnahme: **ein einmal entladenes Modell kommt nicht
zwangsläufig zurück.** Ein Modell, das noch geladen ist, überlebt eine spätere Verschärfung der
Speicherlage; nach `lms unload` gilt die Schranke wieder in voller Härte — auch für den vorher
funktionierenden kleinen Kontext.

**Die Schätzung ist zu pessimistisch.** Mit `modelLoadingGuardrails.mode: off` lud dasselbe Modell
mit 65536 Tokens durch und belegte real **14,98 GiB** statt der geschätzten 20,97 GiB. Die Schranke
blockiert also Ladevorgänge, die funktionieren. Daraus folgt die entscheidende Unterscheidung:

**Nie einen funktionierenden Zustand verspielen, immer einen kaputten riskieren.**

1. Ist ein Modell **geladen und brauchbar** → gar nichts tun.
2. Ist ein Modell **geladen, aber zu klein** → erst `--estimate-only` fragen. Negativ → **nichts
   anfassen**, im Klartext melden. Positiv → entladen, Leiter abwärts laden (Wunschgröße, dann
   Minimum); scheitert die Leiter, alten Ladezustand sofort wiederherstellen.
3. Ist **nichts geladen** → die Schätzung ignorieren und laden **versuchen**. Es gibt nichts zu
   verlieren, und die Schätzung liegt messbar daneben.
4. Unbekannte Antwort → versuchen (fail-open). Eine geänderte Formulierung im CLI-Text darf nie einen
   Ladevorgang blockieren, der funktionieren würde.

---

## 3a. GGUF oder MLX — das Format entscheidet, ob `--context-length` überhaupt wirkt

`lms ls --json` liefert je Modell ein Feld `format`:

| Format | Laufzeit | `--context-length` |
|--------|----------|--------------------|
| `gguf` | llama.cpp | wird übernommen, `lms ps` meldet den angeforderten Wert |
| `safetensors` | MLX (Apple Silicon) | **wird ignoriert**, `lms ps` meldet dauerhaft 4096 |

Gemessen an `qwen/qwen3.8-27b` (`safetensors`, 4bit, `architecture: qwen3_5`): nacheinander mit
`--context-length` 8192, 16384 und 65536 geladen — `lms ps` meldet **jedes Mal 4096**, und ein Prompt
mit ~6500 Tokens wird vom Server abgelehnt. Dieselben Befehle auf GGUF-Modellen (`google/gemma-4-e4b`,
`unsloth/qwen3.8-27b`) übernehmen den Wert korrekt.

In der LM-Studio-**Oberfläche** funktioniert dasselbe MLX-Modell mit weit größerem Kontext. Das täuscht:
für den OpenAI-kompatiblen **Server** — und damit für OpenCode — gilt die 4096.

**Regel: Für Agentenbetrieb über den Server nur GGUF-Modelle.** Und in jedem Fall nach dem Laden
nachprüfen, ob `lms ps` den ANGEFORDERTEN Wert meldet. Tut es das nicht, ignoriert die Laufzeit den
Parameter — dann hilft es auch nicht, die Speicher-Schutzschranken zu lockern. Der brauchbare Hinweis
lautet dann: eine GGUF-Schwester desselben Modells nehmen (gleicher Modellname, anderer Herausgeber).

---

## 4. `parallel` teilt das Kontextfenster NICHT auf

`lms ps` meldet neben `contextLength` ein Feld `parallel` (Vorgabe 4). Die llama.cpp-Protokolle
zeigen Zeilen wie `n_ctx_seq (14336) < n_ctx_train (131072)` bei `4/1 seqs` — das legt nahe, dass
das Fenster auf die Slots aufgeteilt wird und einer Anfrage nur `contextLength / parallel` bleibt.

**Gemessen und widerlegt (2026-08-27):** `google/gemma-4-e4b` mit `--context-length 16384
--parallel 4` geladen, danach ein Prompt mit **9123 Tokens** über `/v1/chat/completions` — geht
sauber durch (`"prompt_tokens": 9123`). Bei einer Aufteilung wären nur 4096 Tokens je Slot
verfügbar gewesen.

**Folge:** Der Wert aus `lms ps` gilt jeder einzelnen Anfrage in voller Höhe und gehört **ungeteilt**
in `limit.context`. Eine Division würde OpenCode viel zu früh komprimieren lassen.

---

## 5. Modellvarianten auseinanderhalten

`unsloth/qwen3.8-27b` und `qwen/qwen3.8-27b` sind verschiedene Quantisierungen desselben Modells und
unterscheiden sich um 5 GB. Ein aus der Modell-ID abgeleiteter Anzeigename verliert den Herausgeber —
beide heißen dann „Qwen3.8 27b (lokal)", und wer die falsche startet, sieht nur „insufficient system
resources".

`lms ls --json` liefert `displayName` und `publisher`. Beides in den Anzeigenamen ziehen:

```
Qwen3.8 27B - unsloth (lokal)
Qwen3.8 27B - qwen (lokal)
```

Zusätzlich hilfreich: bereits geladene Modelle nach oben sortieren und mit ihrem Kontext beschriften
(„geladen, 65k Kontext") — sie sind sofort einsatzbereit, alle anderen kosten erst Ladezeit.

---

## 6. Server-Start ist asynchron

`lms server start` ist idempotent und meldet auch bei laufendem Server Erfolg — kehrt aber zurück,
**bevor** der HTTP-Endpunkt Anfragen annimmt. Ohne Wiederholversuche liefert das direkt folgende
`GET /v1/models` eine leere Liste, und der Modell-Reiter bleibt leer, obwohl LM Studio läuft.

Bewährt: erst abfragen, nur bei leerem Ergebnis den Server starten, danach **4 Versuche im Abstand
von 1,5 Sekunden**.

Aus der Modelliste alle Einträge mit `embed` im Namen aussortieren — Embedding-Modelle taugen nicht
als Agent-Modell.

---

## 7. Realistische Erwartung an die Geschwindigkeit

Ein 27B-Modell in 4-bit auf einem M4 mit 24 GB verarbeitet einen 22000-Token-Systemprompt **nicht**
in Sekunden. Gemessen: über 25 Minuten im Zustand `processingPrompt`, ohne Fehler, aber ohne
Antwort. Für OpenCode, das diesen Prompt bei **jeder** Anfrage schickt, ist das unbrauchbar.

Wer lokal agentisch arbeiten will, wählt auf dieser Klasse Hardware ein kleineres Modell
(≈ 4B–14B) oder nimmt lange Wartezeiten bewusst in Kauf. Die Kontextgröße richtig zu setzen ist
notwendig, aber nicht hinreichend.

---

## Quellen

- Eigene Messungen 2026-08-27 (macOS 15.6, M4, 24 GB; LM Studio 2026-08; OpenCode 1.18.23) —
  `--estimate-only`-Urteile, Parallel-Test, Ladezeiten, Speicherschätzungen.
- `lms load --help`, `lms ps --json`, `lms ls --json` (lms-CLI, offiziell).
- https://opencode.ai/docs/providers — OpenAI-kompatible Provider (`@ai-sdk/openai-compatible`).
