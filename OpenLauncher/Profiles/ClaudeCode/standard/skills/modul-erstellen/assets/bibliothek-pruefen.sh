#!/usr/bin/env bash
# Prüflauf über die ganze Modul-Bibliothek.
#
#   bash bibliothek-pruefen.sh            # alle Module
#   bash bibliothek-pruefen.sh M1.1       # nur eines
#
# Meldet, was still auseinandergelaufen ist. Ändert nichts.

set -u
MODULE="${MODULE_ROOT:-$HOME/proggs/Module}"
FILTER="${1:-}"
befunde=0

melde() { echo "  ⚠ $*"; befunde=$((befunde + 1)); }

for manifest in "$MODULE"/*/*/MODUL.md; do
  ordner=$(dirname "$manifest")
  name=$(basename "$ordner")
  nummer=${name%%-*}
  [ -n "$FILTER" ] && [ "$nummer" != "$FILTER" ] && continue

  echo "── $name"

  # 1) Stand im Manifest gegen die Dateiköpfe
  stand=$(grep -oE '^\- \*\*Stand:\*\* v[0-9]+' "$manifest" | grep -oE 'v[0-9]+')
  if [ -z "$stand" ]; then
    melde "kein Stand im Manifest"
  else
    abweichend=$(grep -rhoE 'Stand v[0-9]+' "$ordner/src" 2>/dev/null \
                 | sort -u | grep -v "Stand $stand" || true)
    [ -n "$abweichend" ] && melde "Dateiköpfe weichen ab: $(echo "$abweichend" | tr '\n' ' ') (Manifest: $stand)"
  fi

  # 2) Indexzeile gegen das Manifest
  idx=$(grep -F "**$nummer**" "$MODULE/INDEX.md" 2>/dev/null || true)
  if [ -z "$idx" ]; then
    melde "steht nicht in INDEX.md"
  else
    echo "$idx" | grep -qF "**$stand**" || melde "INDEX.md nennt einen anderen Stand als das Manifest ($stand)"
    zeilen=$(awk '/^\| App \|/{t=1;next} t&&/^\|---/{next} t&&/^\|/{n++} t&&!/^\|/{exit} END{print n+0}' "$manifest")
    echo "$idx" | grep -qE "Konsumenten: $zeilen\b" || melde "INDEX.md nennt eine andere Konsumentenzahl als die Tabelle ($zeilen)"
  fi

  # 3) Jede Konsumentenkopie gegen die Bibliothek
  # Process Substitution statt Pipe: eine Pipe liefe in einer Subshell, und die
  # dort gezählten Befunde wären beim Schlusssatz wieder verschwunden.
  while IFS='|' read -r _ app appstand pfad _; do
      app=$(echo "$app" | xargs); appstand=$(echo "$appstand" | xargs); pfad=$(echo "$pfad" | xargs | tr -d '`')
      [ -z "$pfad" ] && continue
      ziel="$HOME/proggs/$pfad"
      if [ ! -d "$ziel" ]; then
        melde "$app: Kopie fehlt unter $pfad"
      elif [ "$appstand" = "$stand" ]; then
        diff -r --strip-trailing-cr --exclude="Anbindung.*" "$ordner/src"/*/*/*/* "$ziel" >/dev/null 2>&1 \
          || melde "$app steht auf $stand, weicht aber von der Bibliothek ab — Abweichungsprüfung fällig"
      else
        echo "  · $app hinkt hinterher ($appstand, Bibliothek $stand)"
      fi
  done < <(awk '/^\| App \|/{t=1;next} t&&/^\|---/{next} t&&/^\|/{print} t&&!/^\|/{exit}' "$manifest")

  # 4) Teile ohne Aufrufer — nur melden, wenn wirklich niemand sie nennt
  decls=$(grep -hoE '^(class|data class|interface|object|enum class|fun|val) [A-Za-z][A-Za-z0-9_]*' \
          "$ordner/src"/*/*/*/*/*.kt 2>/dev/null | awk '{print $NF}' | sort -u)
  for d in $decls; do
    n=$(grep -rhoE "\b$d\b" "$ordner/src" 2>/dev/null | wc -l)
    [ "$n" -le 1 ] && melde "im Modul selbst nie benutzt: $d — Aufrufer in den Konsumenten suchen"
  done
done

echo
if [ "$befunde" -eq 0 ]; then
  echo "Nichts zu beanstanden."
else
  echo "$befunde Befund(e). Nichts wurde geändert — was zu tun ist, entscheidet der Benutzer."
fi
