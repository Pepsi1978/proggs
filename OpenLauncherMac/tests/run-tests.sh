#!/bin/bash
# Baut und startet die Tests des macOS-Launchers.
# Gegenstueck zu OpenLauncher/tests/*.ps1 unter Windows.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$DIR/../OpenLauncher"
OUT="$DIR/.build"
mkdir -p "$OUT"

echo "=== Startskript-Syntaxtest ==="
swiftc -o "$OUT/start-script-syntax-test" \
    -target arm64-apple-macos13.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -framework AppKit -framework Foundation \
    "$SRC/Models/Models.swift" \
    "$SRC/Services/Paths.swift" \
    "$SRC/Services/Logger.swift" \
    "$SRC/Services/Shell.swift" \
    "$SRC/Services/JSONValue.swift" \
    "$SRC/Services/Theme.swift" \
    "$SRC/Services/LayoutSettings.swift" \
    "$SRC/Services/OpenCodeVariantCatalog.swift" \
    "$SRC/Services/LmStudioService.swift" \
    "$SRC/Services/ModelRegistry.swift" \
    "$SRC/Services/ModelDefaultsService.swift" \
    "$SRC/Services/InstructionProfileService.swift" \
    "$SRC/Services/OpenRouterService.swift" \
    "$SRC/Services/OpenCodeUpdateService.swift" \
    "$SRC/Services/TerminalLauncher.swift" \
    "$SRC/Services/OpenLauncherService.swift" \
    "$DIR/main.swift"
"$OUT/start-script-syntax-test"
