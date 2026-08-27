#!/usr/bin/env bash
#
# build-install-macos-tuifix.sh — gepatchtes OpenCode-Binary fuer macOS bauen und einhaengen.
#
# Gegenstueck zu build-install-windows-mousefix.ps1. Bewusst NICHT derselbe Patch: der
# Windows-Patch dreht an Maus, Auswahl und Zwischenablage (PowerShell-Aufrufe, Rechtsklick-Einfuegen)
# — das waere unter macOS ein Rueckschritt, dort funktioniert Kopieren-bei-Auswahl nativ. Uebernommen
# werden nur die plattformneutralen Teile:
#
#   1. TuiModel-API (`api.model`)  — die Seitenleiste token-cost-sidebar liest darueber das aktive
#      Modell und schaltet die Effort-Stufen um. Ohne sie fehlt der Effort-Waehler in der TUI.
#   2. Full-Repaint-Recovery       — OpenTUI fordert nach einem fehlgeschlagenen Frame einen
#      vollstaendigen Neuaufbau an, statt das zerfallene Bild stehen zu lassen.
#   3. TUI-Fehler-Handler          — unbehandelte Fehler im TUI-Hauptthread landen in einer
#      Logdatei statt roh auf stderr, also auf demselben TTY, auf dem die TUI zeichnet
#      (bugs/opencode/opencode-cli.md #14a).
#   4. Cache-Telemetrie            — cache_write_tokens werden aus der OpenAI-Antwort uebernommen,
#      sonst zaehlt die Seitenleiste Cache-Schreibkosten als normalen Input.
#
# Ergebnis liegt unter ~/.local/share/opencode-mousefix/ (derselbe Ort wie unter Windows) und wird
# vom macOS-Launcher ueber current.json -> relativeExe gefunden (OpenLauncherService.resolveOpenCodeExecutable).
#
# Aufruf:  bash ~/proggs/opencode-setup/build-install-macos-tuifix.sh [--force] [--version 1.18.23]
#
# Idempotent: ist die Zielversion schon gebaut und eingehaengt, passiert nichts.

set -euo pipefail

PATCH_REVISION="1"
VERSION=""
FORCE=0
INSTALL_ROOT="${OPENCODE_TUIFIX_ROOT:-$HOME/.local/share/opencode-mousefix}"

while [ $# -gt 0 ]; do
  case "$1" in
    --force) FORCE=1; shift ;;
    --version) VERSION="${2:-}"; shift 2 ;;
    *) echo "Unbekannte Option: $1" >&2; exit 2 ;;
  esac
done

SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TUI_MODEL_PATCH="$SRC/patches/opencode-1.18.23-tui-model.patch"
CACHE_PATCH="$SRC/patches/opencode-1.18.3-cache-telemetry.patch"
VERSIONS_DIR="$INSTALL_ROOT/versions"
POINTER="$INSTALL_ROOT/current.json"

green() { printf '\033[32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[33m%s\033[0m\n' "$1"; }
red() { printf '\033[31m%s\033[0m\n' "$1"; }

for command_name in git bun python3; do
  command -v "$command_name" >/dev/null 2>&1 || { red "Voraussetzung fehlt: $command_name"; exit 1; }
done
for patch_file in "$TUI_MODEL_PATCH" "$CACHE_PATCH"; do
  [ -f "$patch_file" ] || { red "Patch nicht gefunden: $patch_file"; exit 1; }
done

if [ -z "$VERSION" ]; then
  VERSION="$(npm view opencode-ai dist-tags.latest 2>/dev/null || true)"
fi
case "$VERSION" in
  [0-9]*.[0-9]*.[0-9]*) : ;;
  *) red "Keine stabile OpenCode-Version ermittelt: '$VERSION'"; exit 1 ;;
esac
CUSTOM_VERSION="$VERSION-macfix.$PATCH_REVISION"
FINAL_DIR="$VERSIONS_DIR/$CUSTOM_VERSION"
FINAL_EXE="$FINAL_DIR/opencode"

mkdir -p "$VERSIONS_DIR"

# Schon gebaut UND eingehaengt? Dann ist nichts zu tun.
if [ "$FORCE" -eq 0 ] && [ -x "$FINAL_EXE" ] && [ -f "$POINTER" ]; then
  active="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('active',{}).get('customVersion',''))" "$POINTER" 2>/dev/null || true)"
  if [ "$active" = "$CUSTOM_VERSION" ]; then
    green "Bereits aktuell: $FINAL_EXE ($CUSTOM_VERSION)"
    exit 0
  fi
fi

WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/opencode-macfix-XXXXXX")"
cleanup() { rm -rf "$WORKDIR" 2>/dev/null || true; }
trap cleanup EXIT

if [ ! -x "$FINAL_EXE" ]; then
  echo "== Baue OpenCode $CUSTOM_VERSION =="
  git clone --depth 1 --branch "v$VERSION" https://github.com/anomalyco/opencode.git "$WORKDIR/oc" >/dev/null 2>&1 \
    || { red "OpenCode v$VERSION konnte nicht geklont werden."; exit 1; }
  cd "$WORKDIR/oc"

  # 1 + 4: die beiden Quelltext-Patches. --check zuerst, damit ein inkompatibler Patch nicht
  # halb angewendet liegen bleibt.
  for patch_file in "$TUI_MODEL_PATCH" "$CACHE_PATCH"; do
    git apply --check --ignore-space-change "$patch_file" \
      || { red "Patch passt nicht zu OpenCode v$VERSION: $(basename "$patch_file")"; exit 1; }
    git apply --ignore-space-change "$patch_file"
  done
  green "OK  Quelltext-Patches angewendet"

  bun install --ignore-scripts >/dev/null 2>&1 || { red "Bun-Abhaengigkeiten konnten nicht installiert werden."; exit 1; }
  green "OK  Abhaengigkeiten installiert"

  # 2: Full-Repaint-Recovery im vorgebauten OpenTUI-Bundle. Kein git-Patch moeglich - die Datei
  # kommt aus node_modules und traegt einen wechselnden Hash im Namen.
  python3 - "$WORKDIR/oc" <<'PYEOF'
import glob, sys
base = sys.argv[1] + "/packages/tui/node_modules/@opentui/core/"
candidates = glob.glob(base + "chunk-bun-*.js") + glob.glob(base + "index-*.js")
hits = [f for f in candidates if "reportNativeRenderFailure()" in open(f, encoding="utf-8").read()]
if len(hits) != 1:
    raise SystemExit("OpenTUI-Renderer-Bundle nicht eindeutig bestimmbar: %d Treffer" % len(hits))
path = hits[0]
text = open(path, encoding="utf-8").read()
needle = ('  reportNativeRenderFailure() {\n    console.error("[CliRenderer] Native frame render '
          'failed; waiting for the next render request to force repaint");')
replacement = ('  reportNativeRenderFailure() {\n    this.forceFullRepaintRequested = true;\n'
               '    console.error("[CliRenderer] Native frame render failed; waiting for the next '
               'render request to force repaint");')
if needle in text:
    open(path, "w", encoding="utf-8", newline="\n").write(text.replace(needle, replacement, 1))
elif replacement not in text:
    raise SystemExit("Full-Repaint-Recovery ist weder vorhanden noch sicher patchbar.")
PYEOF
  green "OK  Full-Repaint-Recovery"

  # 3: Prozessweite Fehler-Handler im TUI-Einstieg.
  python3 - "$WORKDIR/oc" <<'PYEOF'
import sys
path = sys.argv[1] + "/packages/opencode/src/cli/cmd/tui.ts"
text = open(path, encoding="utf-8").read()
marker = "tuifix: unhandled errors in the TUI main thread"
if marker in text:
    raise SystemExit(0)
anchor = "  handler: async (args) => {\n    if (args.replay === true) {"
if anchor not in text:
    raise SystemExit("TUI-Fehler-Handler-Patch ist weder vorhanden noch sicher anwendbar.")
injection = '''  handler: async (args) => {
    // tuifix: unhandled errors in the TUI main thread must never reach stderr -
    // stderr is the TTY the TUI renders on; raw stack traces corrupt the diff-rendered
    // screen until a full repaint (bugs/opencode/opencode-cli.md #14a). Mirror the
    // server worker's process-level handlers, but log to a file (stay observable).
    {
      const fs = await import("node:fs")
      const os = await import("node:os")
      const crashLog = path.join(os.homedir(), ".local", "share", "opencode", "log", "tui-crash.log")
      const logUnhandled = (kind: string, error: unknown) => {
        try {
          fs.mkdirSync(path.dirname(crashLog), { recursive: true })
          const text = error instanceof Error ? (error.stack ?? error.message) : String(error)
          fs.appendFileSync(crashLog, new Date().toISOString() + " " + kind + ": " + text + "\\n")
        } catch {
          // last resort: swallowing here is deliberate - a failing crash logger
          // must never take down or corrupt the TUI it protects
        }
      }
      process.on("unhandledRejection", (error) => logUnhandled("unhandledRejection", error))
      process.on("uncaughtException", (error) => logUnhandled("uncaughtException", error))
    }
    if (args.replay === true) {'''
open(path, "w", encoding="utf-8", newline="\n").write(text.replace(anchor, injection, 1))
PYEOF
  green "OK  TUI-Fehler-Handler"

  for package_name in core plugin tui llm opencode; do
    bun run --cwd "packages/$package_name" typecheck >/dev/null 2>&1 \
      || { red "Typecheck fehlgeschlagen: packages/$package_name"; exit 1; }
  done
  green "OK  Typecheck (core, plugin, tui, llm, opencode)"

  # Die Regressionstests fassen prozessglobale Config-/Temp-Zustaende an und laufen deshalb ohne
  # geerbte OPENCODE_CONFIG*-Variablen und in getrennten Laeufen.
  unset OPENCODE_CONFIG OPENCODE_CONFIG_DIR OPENCODE_CONFIG_CONTENT
  ( cd packages/tui && bun test test/context/local.test.ts test/clipboard.test.ts >/dev/null 2>&1 ) \
    || { red "TUI-Regressionstests fehlgeschlagen."; exit 1; }
  for test_file in test/cli/tui/thread.test.ts test/cli/tui/plugin-toggle.test.ts test/plugin/install.test.ts test/config/config.test.ts; do
    [ -f "packages/opencode/$test_file" ] || continue
    ( cd packages/opencode && bun test --timeout 30000 "$test_file" >/dev/null 2>&1 ) \
      || { red "Regressionstest fehlgeschlagen: $test_file"; exit 1; }
  done
  green "OK  Regressionstests"

  OPENCODE_VERSION="$CUSTOM_VERSION" bun run --cwd packages/opencode script/build.ts \
    --single --skip-install --skip-embed-web-ui >/dev/null 2>&1 \
    || { red "Custom-Binary konnte nicht gebaut werden."; exit 1; }

  arch_dir="opencode-darwin-arm64"
  [ "$(uname -m)" = "x86_64" ] && arch_dir="opencode-darwin-x64"
  built="packages/opencode/dist/$arch_dir/bin/opencode"
  [ -f "$built" ] || { red "Build-Artefakt fehlt: $built"; exit 1; }

  SOURCE_COMMIT="$(git rev-parse HEAD)"
  staging="$INSTALL_ROOT/staging/$CUSTOM_VERSION.$$"
  mkdir -p "$staging"
  cp "$built" "$staging/opencode"
  chmod +x "$staging/opencode"
  staged_version="$("$staging/opencode" --version | tr -d '[:space:]')"
  [ "$staged_version" = "$CUSTOM_VERSION" ] \
    || { red "Versionspruefung fehlgeschlagen: erwartet $CUSTOM_VERSION, erhalten $staged_version"; rm -rf "$staging"; exit 1; }
  rm -rf "$FINAL_DIR"
  mkdir -p "$(dirname "$FINAL_DIR")"
  mv "$staging" "$FINAL_DIR"
  rmdir "$INSTALL_ROOT/staging" 2>/dev/null || true
  green "OK  gebaut: $FINAL_EXE"
else
  SOURCE_COMMIT="unveraendert"
  green "OK  bereits gebaut: $FINAL_EXE"
fi

actual="$("$FINAL_EXE" --version | tr -d '[:space:]')"
[ "$actual" = "$CUSTOM_VERSION" ] || { red "Installiertes Binary meldet unerwartet $actual"; exit 1; }

# Zeiger atomar schreiben. Der Launcher liest ihn (current.json, sonst current.json.bak) und
# startet genau dieses Binary; previous bleibt als Rueckfallweg erhalten.
python3 - "$POINTER" "$INSTALL_ROOT" "$VERSION" "$CUSTOM_VERSION" "$FINAL_EXE" "${SOURCE_COMMIT:-unbekannt}" <<'PYEOF'
import hashlib, json, os, sys
from datetime import datetime, timezone

pointer, root, upstream, custom, exe, commit = sys.argv[1:7]
previous = None
if os.path.exists(pointer):
    try:
        previous = json.load(open(pointer, encoding="utf-8")).get("active")
    except Exception:
        previous = None

digest = hashlib.sha256(open(exe, "rb").read()).hexdigest()
data = {
    "schemaVersion": 1,
    "active": {
        "upstreamVersion": upstream,
        "customVersion": custom,
        "relativeExe": "versions/%s/opencode" % custom,
        "sha256": digest,
        "sourceTag": "v%s" % upstream,
        "sourceCommit": commit,
        "capabilities": {
            "tuiModelApi": "present",
            "fullRepaintRecovery": "present",
            "tuiErrorHandlers": "present",
            "cacheTelemetry": "present",
        },
    },
    "previous": previous,
    "activatedAtUtc": datetime.now(timezone.utc).isoformat(),
}
temp = pointer + ".tmp"
os.makedirs(os.path.dirname(pointer), exist_ok=True)
with open(temp, "w", encoding="utf-8", newline="\n") as handle:
    json.dump(data, handle, indent=2, ensure_ascii=False)
if os.path.exists(pointer):
    os.replace(pointer, pointer + ".bak")
os.replace(temp, pointer)
print("Zeiger geschrieben: %s -> %s" % (pointer, custom))
PYEOF

green "== Fertig: OpenCode $CUSTOM_VERSION ist fuer neue Sitzungen aktiv =="
echo "   Der Launcher startet es automatisch. Direkt testen:  $FINAL_EXE --version"
