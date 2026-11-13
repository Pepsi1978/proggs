# macOS: zsh zerlegt Variablen nicht in Wörter

> Stand: 13.11.2026 22:38. Selbst erlebt (Claude Code am Mac, Bash-Werkzeug läuft in zsh).

- **Signal:** `ssh $K …` mit `K="-i datei -o BatchMode=yes"` meldet „Identity file datei -o BatchMode=yes not accessible“;
  `git add $F` mit einer Pfadliste in `F` meldet „Pfadspezifikation 'a b c' stimmt mit keinen Dateien überein“.
- **Warum:** zsh macht ohne `SH_WORD_SPLIT` aus `$VAR` genau ein Argument (bash würde zerlegen).
- **Fix:** Arrays: `K=(-i "$HOME/…" -o BatchMode=yes); ssh "${K[@]}" …`, `F=(a b c); git add "${F[@]}"`.
  Achtung bei `&&`-Ketten: Scheitert `git add`, laufen die Befehle der nächsten Zeile trotzdem.
