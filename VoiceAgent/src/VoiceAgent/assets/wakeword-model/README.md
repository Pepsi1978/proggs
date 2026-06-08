# Wake-Word-Modell (sherpa-onnx KWS)

Open-vocabulary Keyword-Spotting-Modell, 100 % offline, Apache-2.0.

- **Quelle:** `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01`
  (https://github.com/k2-fsa/sherpa-onnx/releases/tag/kws-models)
- **Sprache:** Englisch (gigaspeech) → Wake-Words in englischer Phonetik (z.B. `OKAY COMPUTER`).
- **Variante:** int8 (kleiner, schneller; ~5 MB statt ~14 MB).

## Dateien

| Datei | Zweck | Laufzeit noetig |
|-------|-------|-----------------|
| `encoder.onnx` / `decoder.onnx` / `joiner.onnx` | Zipformer-Transducer (int8) | ja |
| `tokens.txt` | Token → ID-Map | ja |
| `keywords.txt` | gebundelte Default-Keywords (nur Tokens, KEIN `@original`) | ja (Fallback) |
| `bpe.model` | SentencePiece-Modell (model_type=UNIGRAM, 500 Pieces) | nur zur Vokabular-Erzeugung |
| `unigram-vocab.txt` | aus `bpe.model` extrahiertes Vokabular (Piece + Log-Score + Typ) | **ja** — Laufzeit-Tokenisierung |

## Weckwort-Tokenisierung (kuratiert + FREIE Eingabe)

Das KWS-Modell erkennt **Tokens**, nicht Klartext (Almanach #10). Es gibt zwei Wege, beide liefern
garantiert dieselben Tokens:

1. **Kuratierte Vorschlaege** — in `Core/WakeWords.cs` einmal vorab tokenisiert (schnell, ohne Datei).
2. **Freie Eingabe** — der Nutzer tippt ein eigenes Wort; `Core/SentencePieceUnigram.cs` tokenisiert es
   zur Laufzeit ueber `unigram-vocab.txt` (eigener Unigram-Viterbi, **keine** schwergewichtige Lib).
   Verifiziert: identisch zu Python-sentencepiece (`tests/.../SentencePieceUnigramTests.cs`).
   Das Modell ist auf GROSSSCHREIBUNG trainiert → der Encoder normalisiert zu Uppercase
   ("Computer" → `▁COMP U TER`). Umlaute/Sonderzeichen sind nicht erkennbar und werden in der UI abgelehnt.

**`unigram-vocab.txt` regenerieren** (nur falls `bpe.model` je getauscht wird):

```python
import sentencepiece.sentencepiece_model_pb2 as m
mp = m.ModelProto(); mp.ParseFromString(open("bpe.model","rb").read())
with open("unigram-vocab.txt","w",encoding="utf-8",newline="\n") as f:
    f.write(f"# sentencepiece unigram vocab | pieces={len(mp.pieces)}\n")
    for p in mp.pieces:
        f.write(f"{p.piece}\t{p.score:.6f}\t{int(p.type)}\n")   # Typ 1=NORMAL, 2=UNKNOWN, 4=USER_DEFINED
```

**WICHTIG (Almanach #32):** In `keywords.txt`/Tokens KEINEN `@original`-Marker — sherpa-onnx 1.13.2
parst `@...` als zusaetzliche Tokens und scheitert. Nur die reinen Tokens. `result.Keyword` liefert
die de-tokenisierte Phrase trotzdem. Pro Zeile optional `:boost` und `#threshold` (Almanach #10).
