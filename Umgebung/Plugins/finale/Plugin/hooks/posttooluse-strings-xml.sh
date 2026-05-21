#!/usr/bin/env bash
# posttooluse-strings-xml.sh
#
# Nach jedem Edit/Write auf eine strings.xml: leichter XML-Lint, der die haeufigsten
# Build-Killer-Fehler in Android-Lokalisierungs-Dateien detektiert. Schreibt Warnungen
# nach stderr, blockiert NICHT (die Aenderung ist schon passiert, wir koennen nur warnen).
#
# Geprueft:
# 1. Nackte Apostrophe in <string>-Werten (Pflicht-Escape: \' oder &apos;)
# 2. Nackte doppelte Anfuehrungszeichen in <string>-Werten (Pflicht-Escape: \")
# 3. Unmatched &-Symbole (& muss &amp; sein)
#
# Hinweis: ein vierter Check fuer translatable="false" in values-xx/ war geplant
# aber nicht implementiert — Kommentar entfernt 2026-05-21 (Direktive #3
# Doku-Ehrlichkeit). Wenn der Check noetig wird: hier ergaenzen und im
# PowerShell-Pendant identisch.

# set -eu: Hardening FIN-029 (2026-05-21). Non-blocking Hook — Exit 0 am Ende.
set -eu

# DoS-Limit (W5-A 2026-05-21 Hardening): max 512 KB stdin akzeptieren.
# Bei groesserem Input (theoretisch >500 MB moeglich) wuerde `cat` den Speicher
# fluten. head -c begrenzt den Read hart. Hooks sind non-blocking, exit 0 OK.
stdin_input="$(head -c 524288 2>/dev/null || true)"
if [ -z "$stdin_input" ]; then
  exit 0
fi

# Pfad aus dem Hook-Input extrahieren
file_path="$(printf '%s' "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ti = d.get('tool_input', {}) or {}
    fp = ti.get('file_path') or ti.get('path') or ''
    print(fp)
except Exception:
    print('')
" 2>/dev/null || true)"

if [ -z "$file_path" ] || [ ! -f "$file_path" ]; then
  exit 0
fi

# Nur strings.xml-Dateien interessieren uns.
# Cross-Platform: deckt Forward-Slash (Claude Code Standard) UND Backslash
# (falls Claude Code auf Windows den Pfad mit \ uebergibt) ab. Hardening
# Wave 2.3 2026-05-21 (Direktive #3) — vorher Backslash-Pfade still ueberspruengen.
case "$file_path" in
  *res/values*/strings.xml|*res/values/strings.xml) : ;;
  *res\\values*\\strings.xml|*res\\values\\strings.xml) : ;;
  *) exit 0 ;;
esac

warnings=()

# Wir nutzen Python fuer eine robuste XML-Light-Analyse. Zaehlt Vorkommen.
issues=$(python3 - "$file_path" <<'PY'
import sys, re, os
path = sys.argv[1]
try:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
except Exception as e:
    print(f"read-fail:{e}")
    sys.exit(0)

problems = []

# Finde alle <string ...>...</string> Bloecke
for m in re.finditer(r'<string\b[^>]*>(.*?)</string>', content, flags=re.DOTALL):
    val = m.group(1)
    line = content[:m.start()].count('\n') + 1
    # Nackte Apostrophe (nicht escaped, nicht in CDATA)
    # Ignoriere bereits escapete \' und &apos;
    bare_apo = re.findall(r"(?<!\\)'", val.replace('&apos;', ''))
    if bare_apo and 'CDATA' not in val:
        problems.append((line, f"nackter Apostroph im <string>-Wert (Build-Killer in vielen Locales)"))
    # Nackte doppelte Anfuehrungszeichen
    bare_dq = re.findall(r'(?<!\\)"', val.replace('&quot;', ''))
    if bare_dq and 'CDATA' not in val:
        problems.append((line, f"nacktes Anfuehrungszeichen im <string>-Wert"))
    # & ohne ; (kein gueltiger Entity)
    if re.search(r'&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)', val):
        problems.append((line, f"nacktes '&' ohne Entity-Schreibweise"))

for line, msg in problems[:20]:  # max 20 Warnungen pro Datei
    print(f"line:{line}|{msg}")
PY
)

if [ -n "$issues" ]; then
  echo "[finale] strings.xml-Lint-Warnungen fuer $file_path:" >&2
  while IFS= read -r line; do
    if [ -n "$line" ]; then
      echo "[finale]   $line" >&2
    fi
  done <<< "$issues"
  echo "[finale] Diese Probleme koennen den Android-Build brechen. fix-applier sollte sie eigentlich vermeiden — bitte pruefen." >&2
fi

exit 0
