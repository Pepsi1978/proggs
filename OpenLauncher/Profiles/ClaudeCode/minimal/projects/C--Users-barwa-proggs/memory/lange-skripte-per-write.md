---
name: lange-skripte-per-write
description: "Skripte über ~30 Zeilen nie als Bash-Heredoc, sondern per Write in den Scratchpad und dann ausführen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-07T13:33:45.399Z
---

Lange Skripte (Python, Shell, über ~30 Zeilen) nicht als `python - <<'EOF' … EOF` im Bash-Werkzeug schicken, sondern per Write als Datei in den Scratchpad, dann `python -X utf8 <datei>`.

**Why:** Messung des Skills session-flaschenhals am 07.11.2026 über 40 Sitzungen: 6 von 27 langen Heredocs scheiterten mit `unexpected EOF while looking for matching '`, 0 von 76 kurzen. Jeder Fehlschlag kostete 4–41 s (meist ~30 s) plus erneutes Ausgeben des ganzen Skripts; per Write + Aufruf klappte es danach jedes Mal.

**How to apply:** Kurze Einzeiler/Heredocs bleiben erlaubt. Wirkung wird als Maßnahme M1 in `skills/session-flaschenhals/massnahmen.json` nachgemessen; dort steht auch die Rücknahme. Verwandt: [[bash-commit-heredoc-syntax]].
