# QwenTtsBench

Misst, wie schnell **Qwen3-TTS 0.6B** auf einem Android-Gerät und auf dem PC rechnet.

Zweck: klären, ob Sprachausgabe mit einer geklonten Stimme **direkt auf dem Handy** laufen
kann — als Grundlage für die Entscheidung, ob Perfect Moment einen dritten TTS-Anbieter
neben Microsoft Edge und Google Chirp 3 HD bekommt.

## Was gemessen wird

Gemessen wird der **Talker** — der 28-Schichten-Transformer, der die Audio-Token erzeugt.
Er ist der einzige autoregressive Teil der Pipeline und damit der Flaschenhals. Code
Predictor, Vocoder und Speaker Encoder sind **nicht** enthalten.

Der Tokenizer arbeitet mit 12 Hz: **12 Talker-Schritte ergeben 1 Sekunde Sprache.**
Daraus folgt der Echtzeitfaktor — Werte über 1,0 bedeuten schneller als gesprochen wird.

Modell: [`sivasub987/Qwen3-TTS-0.6B-ONNX-INT8`](https://huggingface.co/sivasub987/Qwen3-TTS-0.6B-ONNX-INT8),
Datei `talker_decode_q.onnx` (426 MB).

## Ergebnisse (27.07.2026)

Referenzsatz mit 30 Wörtern, ergibt 10,9 s Sprache = 131 Talker-Schritte, Prompt 300 Token
(Text plus die rund 250 Token der 20-sekündigen Referenzaufnahme).

| Gerät | ms/Schritt | Schritte/s | Echtzeitfaktor | Talker gesamt |
|-------|-----------:|-----------:|---------------:|--------------:|
| Galaxy Z Fold 6 (Snapdragon 8 Gen 3) | 33 | 30,7 | **2,56 x** | 4,3 s |
| PC (Intel Lunar Lake, Arc 140V) | 57 | 17,5 | 1,45 x | 7,5 s |

Kürzerer Lauf (60 Schritte, Prompt 100): Fold 6 22 ms / 3,85 x — PC 42 ms / 1,98 x.
Der Unterschied zeigt, wie stark der wachsende Zwischenspeicher die Schritte verteuert.

**Das Handy ist rund 1,75-mal schneller als der PC.** Vermutliche Ursache: ARM-Kerne
führen die INT8-Rechnungen effizienter aus. Nicht nachgewiesen.

### Vergleich mit Voicebox

Derselbe Satz, erzeugt über die Voicebox-API auf dem PC (PyTorch, volle Pipeline):
**52,3 s für 10,24 s Sprache = 0,20 x**. Also fünfmal langsamer als Echtzeit und
rund siebenmal langsamer als der ONNX-INT8-Talker auf demselben Rechner.

Vermutliche Ursachen: PyTorch rechnet in voller Genauigkeit statt INT8, und die volle
Pipeline umfasst mehr als den Talker. Nicht isoliert nachgewiesen.

## Benutzung

### Auf dem Handy

```bash
# Modell besorgen
huggingface-cli download sivasub987/Qwen3-TTS-0.6B-ONNX-INT8 talker_decode_q.onnx

# App bauen und installieren
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Modell auf das Gerät schieben
adb shell "mkdir -p /sdcard/Android/data/de.frank.qwenttsbench/files/models"
adb push talker_decode_q.onnx /sdcard/Android/data/de.frank.qwenttsbench/files/models/

# Messen — Schritte, Promptlänge und Threads sind frei wählbar
adb shell "am start -n de.frank.qwenttsbench/.MainActivity --ei steps 131 --ei prompt 300"
adb logcat -s QwenTtsBench:I
```

Das Ergebnis steht auch auf dem Bildschirm der App.

### Auf dem PC

```bash
pip install onnxruntime numpy
python tools/bench_talker.py <pfad/talker_decode_q.onnx> <prompt> <schritte>
```

`tools/gen30.py` erzeugt einen 30-Wörter-Satz über die laufende Voicebox-API und misst
die entstandene Audiolänge — daraus ergibt sich die passende Schrittzahl für den Benchmark.
Die Profil-Kennung darin muss auf ein vorhandenes Voicebox-Profil zeigen.

## Grenzen

- Gemessen wird mit Platzhalter-Werten statt echtem Text. Für die Rechenzeit macht das
  keinen Unterschied, es entsteht aber **kein hörbares Audio**.
- Code Predictor, Vocoder und Speaker Encoder fehlen in der Messung. Die vollständige
  Pipeline braucht mehr — grob geschätzt 6 bis 9 s für den Referenzsatz auf dem Fold 6.
- Die vollständige INT8-Pipeline belegt rund 1,6 GB, nicht die hier gemessenen 426 MB.
- Nur die CPU wird genutzt. Ob die Hexagon-NPU zusätzlich etwas bringt, ist offen.
