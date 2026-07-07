#!/usr/bin/env bash
# Statusline fuer Codex — zeigt Modell + Effort neben der Eingabezeile.
# Wird von Codex mit Session-JSON auf stdin aufgerufen.
# Ausgabe: Eine einzige Zeile Text, die unter dem Eingabefeld erscheint.

set +e

# JSON-Input einlesen (Codex uebergibt Session-Infos per stdin).
input="$(cat 2>/dev/null)"

# Modell-Anzeigename aus dem JSON extrahieren (mit Fallbacks).
model=""
if [ -n "$input" ]; then
  model=$(printf '%s' "$input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    m = d.get('model') or {}
    name = m.get('display_name') or m.get('id') or ''
    print(name)
except Exception:
    print('')
" 2>/dev/null)
fi

# Effort-Level aus settings.json lesen (hat keine Session-JSON-Repraesentation).
effort=$(python3 -c "
import json, os
try:
    with open(os.path.expanduser('~/.codex/settings.json'), 'r', encoding='utf-8') as f:
        d = json.load(f)
    print(d.get('effortLevel', 'high'))
except Exception:
    print('high')
" 2>/dev/null)

# Fallback falls Model leer
[ -z "$model" ] && model="codex"

# Kompakte Ausgabe — eine Zeile, keine Umbrueche.
printf '%s · effort: %s' "$model" "$effort"
