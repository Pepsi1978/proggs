#!/usr/bin/env bash
# bug-almanac-guard: Erinnert bei Edit/Write an bereichstypischen Dateien an den passenden Bug-Almanach.
# Schicht 2 ("Sicherheitsnetz") des Bug-Almanach-Systems (siehe ~/proggs/bugs/SYSTEM.md).
# Runs as PreToolUse hook (matcher: Edit|Write). NICHT blockierend - injiziert nur additionalContext.
# Output: JSON mit hookSpecificOutput.additionalContext (offizielles Schema) -> AI-Kontext.
# Platform: macOS/Linux

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
trap 'hook_log_warn "bug-almanac-guard: Error at line $LINENO"' ERR

input=$(cat)
[ -n "$input" ] || exit 0
fp=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null || echo "")
[ -n "$fp" ] || exit 0
fpl=$(echo "$fp" | tr '[:upper:]' '[:lower:]' | tr '\\' '/')

# Bereich anhand des Dateipfads erkennen (Pfad-Mapping - bei neuem Almanach hier ergaenzen).
slug=""; file=""; name=""
case "$fpl" in
    *manifest.json|*/overlays/*|*background.js|*service-worker.js|*vorlese-overlay*)
        slug="chrome"; file="chrome-extensions.md"; name="Browser-Erweiterungen (Chrome/Edge MV3)";;
    *.kt|*.kts|*androidmanifest.xml)
        slug="android"; file="android-compose.md"; name="Android (Kotlin/Compose)";;
    *.xaml|*.csproj|*.cs)
        slug="wpf"; file="wpf-csharp.md"; name="Windows-Desktop (C#/WPF/WinUI)";;
    *.swift)
        slug="swift"; file="swift-appkit.md"; name="macOS-Desktop (Swift/AppKit)";;
    *.user.js)
        slug="tampermonkey"; file="tampermonkey.md"; name="Tampermonkey/Userscripts";;
    */hooks/*.ps1|*/hooks/*.sh)
        slug="claudehooks"; file="claude-hooks.md"; name="Claude-Harness Hooks (PowerShell/Bash)";;
esac
[ -n "$slug" ] || exit 0

# Spam-Schutz: nur einmal pro Bereich pro Session (Marker leert der Index-Hook bei SessionStart).
marker="${TMPDIR:-/tmp}/bug-almanac-seen-$slug.flag"
[ -f "$marker" ] && exit 0
touch "$marker" 2>/dev/null || true

if [ -f "$HOME/proggs/bugs/$file" ]; then
    msg="BUG-ALMANACH-HINWEIS: Du arbeitest an $name. Lies ZUERST bugs/$file (falls in dieser Session noch nicht geschehen), bevor du bekannte Bugs einbaust."
else
    msg="BUG-ALMANACH-HINWEIS: Du arbeitest an $name, aber es gibt noch KEINEN Almanach (bugs/$file). Hol Franks kurzes OK und STARTE dann den Skill 'bug-almanach-recherche' - das ist der vorgeschriebene, vollstaendige Weg; NICHT selbst ad hoc recherchieren."
fi

python3 -c "import json,sys; print(json.dumps({'hookSpecificOutput': {'hookEventName': 'PreToolUse', 'additionalContext': sys.argv[1]}}))" "$msg"

exit 0
