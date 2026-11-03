# Zum Einbinden (source) in Installationsskripte: bringt eine APK schnell durch einen langsamen Tunnel aufs Handy.
# delta_push <adb> <gerät> <apk> <ziel-am-handy> <paket>
# Liegt die gerade installierte APK des Pakets noch im lokalen Zwischenspeicher, gehen nur die geänderten Einträge
# über die Leitung; das Handy setzt die neue APK aus der installierten zusammen. SHA-256 muss danach exakt
# stimmen, sonst (und ohne passenden Zwischenstand) wird die ganze APK übertragen, mit lz4-Kompression.
# Rückgabe 0 = Ziel liegt byte-genau am Handy.

DELTA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DELTA_CACHE="${XDG_CACHE_HOME:-$HOME/.cache}/apk-delta"
DELTA_TMP=/data/local/tmp/apk-delta

_delta_sha() { if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d' ' -f1; }

_delta_voll() {  # <adb> <gerät> <apk> <ziel>
  "$1" -s "$2" push -z lz4 "$3" "$4" >/dev/null 2>&1 || "$1" -s "$2" push "$3" "$4" >/dev/null 2>&1
}

_delta_merken() {  # <apk> <sha> <paket>: höchstens drei Stände je Paket behalten
  local ordner="$DELTA_CACHE/$3"
  mkdir -p "$ordner" 2>/dev/null || return 0
  if [ -f "$ordner/$2.apk" ]; then
    touch "$ordner/$2.apk"
  else
    cp "$1" "$ordner/$2.apk.teil" 2>/dev/null && mv "$ordner/$2.apk.teil" "$ordner/$2.apk"
  fi
  ls -t "$ordner"/*.apk 2>/dev/null | tail -n +4 | while read -r alt; do rm -f "$alt"; done
}

delta_push() {
  local adb="$1" geraet="$2" apk="$3" ziel="$4" paket="$5"
  local neu_sha basis basis_sha alt arbeit zeile patch_bytes neu_bytes ziel_sha
  neu_sha=$(_delta_sha "$apk")
  basis=$("$adb" -s "$geraet" shell pm path "$paket" 2>/dev/null | tr -d '\r' | sed -n 's/^package:\(.*base\.apk\)$/\1/p' | head -1)
  if [ -n "$basis" ] && command -v python3 >/dev/null 2>&1; then
    basis_sha=$("$adb" -s "$geraet" shell "sha256sum '$basis'" 2>/dev/null | tr -d '\r' | cut -d' ' -f1)
    alt="$DELTA_CACHE/$paket/$basis_sha.apk"
    if [ -n "$basis_sha" ] && [ -f "$alt" ]; then
      arbeit=$(mktemp -d)
      zeile=$(python3 "$DELTA_DIR/apk_delta.py" "$alt" "$apk" "$arbeit" 2>/dev/null | tail -1)
      if [ "${zeile%% *}" = DELTA ]; then
        patch_bytes=$(echo "$zeile" | cut -d' ' -f2); neu_bytes=$(echo "$zeile" | cut -d' ' -f3)
        if _delta_voll "$adb" "$geraet" "$arbeit/patch.bin" "$DELTA_TMP.patch" \
          && "$adb" -s "$geraet" push "$arbeit/plan.sh" "$DELTA_TMP.sh" >/dev/null 2>&1 \
          && "$adb" -s "$geraet" shell "sh $DELTA_TMP.sh '$basis' $DELTA_TMP.patch '$ziel'" >/dev/null 2>&1; then
          ziel_sha=$("$adb" -s "$geraet" shell "sha256sum '$ziel'" 2>/dev/null | tr -d '\r' | cut -d' ' -f1)
        fi
        "$adb" -s "$geraet" shell rm -f "$DELTA_TMP.patch" "$DELTA_TMP.sh" >/dev/null 2>&1
      fi
      rm -rf "$arbeit"
      if [ -n "${ziel_sha:-}" ] && [ "$ziel_sha" = "$neu_sha" ]; then
        echo "Delta übertragen: $(( patch_bytes / 1048576 )) von $(( neu_bytes / 1048576 )) MB, SHA-256 geprüft."
        _delta_merken "$apk" "$neu_sha" "$paket"
        return 0
      fi
      echo "Delta passte nicht, übertrage die ganze APK."
      "$adb" -s "$geraet" shell rm -f "'$ziel'" >/dev/null 2>&1
    fi
  fi
  _delta_voll "$adb" "$geraet" "$apk" "$ziel" || return 1
  _delta_merken "$apk" "$neu_sha" "$paket"
  return 0
}
