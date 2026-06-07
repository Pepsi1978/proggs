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
| `keywords.txt` | vor-tokenisierte Wake-Words (`▁OKAY ▁COMP U TER @OKAY COMPUTER`) | ja |
| `bpe.model` | SentencePiece-BPE-Modell | nur fuer Re-Tokenisierung (Build-Zeit) |

## Neues Wake-Word hinzufuegen

Die `keywords.txt` muss **vor-tokenisiert** sein (BPE). Klartext wird nicht erkannt (Almanach #10).
Neues Wort erzeugen (Python + sentencepiece):

```python
import sentencepiece as spm
sp = spm.SentencePieceProcessor(); sp.load("bpe.model")
toks = " ".join(sp.encode("HEY COMPUTER", out_type=str))
print(toks)   # NUR die BPE-Tokens an keywords.txt anhaengen
```

**WICHTIG (Almanach #32):** KEINEN `@original`-Marker anhaengen — sherpa-onnx 1.13.2 parst
`@...` mit diesem Modell als zusaetzliche Tokens und scheitert ("Cannot find ID for token ...").
Nur die reinen BPE-Tokens. `result.Keyword` enthaelt trotzdem die de-tokenisierte Phrase
(z.B. "HEY COMPUTER"). Optional erlaubt sind pro Zeile `:boost` (z.B. `:2.0`) und
`#threshold` (z.B. `#0.3`) zum Feinjustieren der Empfindlichkeit (Almanach #10).
