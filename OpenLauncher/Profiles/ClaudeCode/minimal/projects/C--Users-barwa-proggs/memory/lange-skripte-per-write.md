---
name: lange-skripte-per-write
description: "Skripte über ~30 Zeilen nie als Bash-Heredoc, sondern per Write in den Scratchpad und dann ausführen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-12T13:32:13.695Z
---

Lange Skripte (Python, Shell, über ~30 Zeilen) nicht als `python - <<'EOF' … EOF` im Bash-Werkzeug schicken, sondern per Write als Datei in den Scratchpad, dann `python -X utf8 <datei>`.

**Why:** Messung des Skills session-flaschenhals am 07.11.2026 über 40 Sitzungen: 6 von 27 langen Heredocs scheiterten mit `unexpected EOF while looking for matching '`, 0 von 76 kurzen. Jeder Fehlschlag kostete 4–41 s (meist ~30 s) plus erneutes Ausgeben des ganzen Skripts; per Write + Aufruf klappte es danach jedes Mal.

**How to apply:** Kurze Einzeiler/Heredocs bleiben erlaubt. Wirkung wird als Maßnahme M1 in `skills/session-flaschenhals/massnahmen.json` nachgemessen; dort steht auch die Rücknahme. Verwandt: [[bash-commit-heredoc-syntax]].

**Ersetzskripte:** Kotlin-Dateien im Repo haben oft CRLF. Nach dem Lesen CRLF in LF umwandeln (`chr(13) + chr(10)` → `chr(10)`, nie per sed oder printf einsetzen), beim Schreiben zurückwandeln, sonst scheitert jeder mehrzeilige Treffer (Sitzung 9d83ba1b, ~2 min).

**PowerShell und deutsche Anführungszeichen:** PowerShell behandelt die Zeichen U+201E und U+201C (deutsche Anführungszeichen) wie normale doppelte Anführungszeichen. Steht so ein Zeichen in einem Befehl oder in einem doppelt oder einfach gequoteten String, bricht der Parser ab („Unerwartetes Token“ bzw. „Fehlender Eigenschaftsname nach Verweisoperator“). Code mit deutschen Anführungszeichen deshalb per Edit/Write ändern, in Suchmustern `\u201e` schreiben (Sitzung e52f3b2f, 2 Fehlschläge).

**PowerShell-Fallen (Sitzung 6fc10676, 5 Fehlschläge):** In doppelt gequoteten Strings `"$var:"` als `"${var}:"` schreiben. Hilfsfunktionen nie `R`, `H`, `r` nennen (eingebaute Aliase, `R` = Invoke-History). Text mit `$`-Zeichen oder deutschen Anführungszeichen (Gradle `$projectDir`, Doku-Sätze) immer per Edit/Write, nie im Befehl. Mehrzeilige `.Replace()` scheitern still an CRLF: Treffer prüfen oder Edit nehmen.
Sitzung 3320ad98 (2 Fehlschläge, 19k Fehlerausgabe): `[IO.File]::…` löst relative Pfade gegen das Prozess-Arbeitsverzeichnis auf, nicht gegen `cd`/`Set-Location` → immer absolute Pfade. Variablen sind ohne Groß-/Kleinschreibung: `$B` außen und Parameter `$b` sind dieselbe Variable.
