# macOS: zsh zerlegt Variablen nicht in Wörter

> Stand: 13.11.2026 22:38. Selbst erlebt (Claude Code am Mac, Bash-Werkzeug läuft in zsh).

- **Signal:** `ssh $K …` mit `K="-i datei -o BatchMode=yes"` meldet „Identity file datei -o BatchMode=yes not accessible“;
  `git add $F` mit einer Pfadliste in `F` meldet „Pfadspezifikation 'a b c' stimmt mit keinen Dateien überein“.
- **Warum:** zsh macht ohne `SH_WORD_SPLIT` aus `$VAR` genau ein Argument (bash würde zerlegen).
- **Fix:** Arrays: `K=(-i "$HOME/…" -o BatchMode=yes); ssh "${K[@]}" …`, `F=(a b c); git add "${F[@]}"`.
  Achtung bei `&&`-Ketten: Scheitert `git add`, laufen die Befehle der nächsten Zeile trotzdem.

## Unquotierte Globs in Optionen (15.11.2026, selbst erlebt)
- **Signal:** `grep -rn X --include=*.kt .` bricht mit `(eval):1: no matches found: --include=*.kt` ab, ohne zu suchen.
- **Warum:** zsh (`NOMATCH`) wertet `*` auch in `--include=*.kt` als Dateimuster aus und bricht ab, wenn nichts passt.
- **Fix:** Muster immer quoten: `--include='*.kt'`, `find . -name '*.kt'`.
- **Stand 19.11.2026:** `~/.zshrc` setzt `setopt nonomatch`, der Shell-Schnappschuss übernimmt es (geprüft): Das Muster bleibt
  dann wörtlich stehen, grep sucht normal. Quoten bleibt richtig (`*.kt` passt sonst auf Dateien im aktuellen Ordner).
