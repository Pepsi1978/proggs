#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/TerminalVoiceOverlay"
BUILD_DIR="$PROJECT_DIR/build"
APP_NAME="TerminalVoiceOverlay"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"
source "$PROJECT_DIR/../voice-overlay-deploy-guard.sh"
DEPLOYMENT_RESERVED=0
cleanup_deployment_guard() {
    release_voice_overlay_deployment "$APP_NAME" 5723 "$DEPLOYMENT_RESERVED"
}
trap cleanup_deployment_guard EXIT
if reserve_voice_overlay_deployment "$APP_NAME" 5723; then
    DEPLOYMENT_RESERVED=1
else
    rc=$?
    if [ "$rc" -eq 2 ]; then exit 1; fi
fi

# Voice-Overlay-Config-Datei in $HOME/SK/VoiceOverlays/ installieren falls
# noch nicht vorhanden. Idempotent — vorhandene User-Anpassungen werden NIE
# ueberschrieben. Das macht "git pull && bash build.sh" zu einem one-shot
# Setup auf einer frischen Mac-Installation.
# Hinweis: voice-prompt.txt wird nicht mehr installiert. Whisper bekommt das
# Audio ohne Prompt-Hint; themenspezifische Profile passieren in Gemini.
SK_VOICE_DIR="$HOME/SK/VoiceOverlays"
TEMPLATE_DIR="$PROJECT_DIR/../voice-overlay-templates"
mkdir -p "$SK_VOICE_DIR"
for f in gemini-correction-prompt.txt; do
    if [ ! -f "$SK_VOICE_DIR/$f" ] && [ -f "$TEMPLATE_DIR/$f" ]; then
        cp "$TEMPLATE_DIR/$f" "$SK_VOICE_DIR/$f"
        echo "Installed: $SK_VOICE_DIR/$f"
    fi
done

echo "=== Building $APP_NAME ==="

# Clean
rm -rf "$BUILD_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# Copy Info.plist
cp "$SRC_DIR/Info.plist" "$APP_BUNDLE/Contents/Info.plist"

# Compile
SWIFT_FILES=(
    "$SRC_DIR/Config.swift"
    "$SRC_DIR/AppWatcher.swift"
    "$SRC_DIR/AudioRecorder.swift"
    "$SRC_DIR/GroqWhisperClient.swift"
    "$SRC_DIR/TranscriptionEngineSetting.swift"
    "$SRC_DIR/TranscriptionModeSetting.swift"
    "$SRC_DIR/PersonalVocabulary.swift"
    "$SRC_DIR/GeminiBatchTranscribeClient.swift"
    "$SRC_DIR/SpeechToTextRouter.swift"
    "$SRC_DIR/RecordingArchive.swift"
    "$SRC_DIR/GeminiClient.swift"
    "$SRC_DIR/TerminalController.swift"
    "$SRC_DIR/OverlayPanel.swift"
    "$SRC_DIR/OverlayOrientation.swift"
    "$SRC_DIR/OverlayHorizontalLayout.swift"
    "$SRC_DIR/OverlayGlideAnimation.swift"
    "$SRC_DIR/OverlayCollapsedMic.swift"
    "$SRC_DIR/OverlayExtraButtons.swift"
    "$SRC_DIR/AutoEnterStatusServer.swift"
    "$SRC_DIR/SettingsDialog.swift"
    "$SRC_DIR/CommonDialogs.swift"
    "$SRC_DIR/AutoHideController.swift"
    "$SRC_DIR/DiagLog.swift"
    "$SRC_DIR/PromptHotkeyRegistry.swift"
    "$SRC_DIR/WaveformView.swift"
    "$SRC_DIR/RecordingCuePlayer.swift"
    "$SRC_DIR/IconPaths.swift"
    "$SRC_DIR/PushToTalkController.swift"
    "$SRC_DIR/ErrorDescriptions.swift"
    "$SRC_DIR/PromptBoardModels.swift"
    "$SRC_DIR/PromptBoardStore.swift"
    "$SRC_DIR/AlwaysOnPrefixService.swift"
    "$SRC_DIR/VoiceServiceProvider.swift"
    "$SRC_DIR/PromptBoardDialogs.swift"
    "$SRC_DIR/GoogleDriveBackupService.swift"
    "$SRC_DIR/PromptBoardPanel.swift"
    "$SRC_DIR/PromptInputPanel.swift"
    "$SRC_DIR/PromptHistoryStore.swift"
    "$SRC_DIR/PromptHistoryPanel.swift"
    "$SRC_DIR/PromptSlotStore.swift"
    "$SRC_DIR/HotkeyRegistry.swift"
    "$SRC_DIR/AppDelegate.swift"
    "$SRC_DIR/main.swift"
)

swiftc \
    -o "$APP_BUNDLE/Contents/MacOS/$APP_NAME" \
    -target arm64-apple-macos13.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -framework AppKit \
    -framework AVFoundation \
    -framework CoreGraphics \
    -framework Carbon \
    -framework Network \
    -lsqlite3 \
    "${SWIFT_FILES[@]}"

# Copy entitlements and sign with persistent certificate (not ad-hoc)
# This ensures TCC permissions survive rebuilds, because macOS identifies
# the app by certificate identity instead of binary hash.
cp "$SRC_DIR/$APP_NAME.entitlements" "$APP_BUNDLE/Contents/Resources/"

SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" --entitlements "$SRC_DIR/$APP_NAME.entitlements" --deep "$APP_BUNDLE"
    echo "=== Signiert mit Zertifikat: $SIGNING_IDENTITY ==="
else
    echo "⚠ Zertifikat '$SIGNING_IDENTITY' nicht gefunden, fallback auf ad-hoc Signierung"
    echo "  TCC-Berechtigungen gehen bei Rebuild verloren!"
    codesign --force --sign - --entitlements "$SRC_DIR/$APP_NAME.entitlements" --deep "$APP_BUNDLE"
fi

echo "=== Build erfolgreich: $APP_BUNDLE ==="
echo ""
echo "App starten:"
echo "  open $APP_BUNDLE"
echo ""
echo "Oder direkt:"
echo "  $APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Auf den ersten Build mit dem Hotkey-Update SETUP.md im Standardprogramm
# (typisch eine Markdown-Vorschau wie Marked oder QuickLook) oeffnen, damit
# Frank die Hotkey-Liste und Permission-Schritte direkt sieht. Der Marker
# verhindert dass die Datei bei jedem spaeteren Build wieder geoeffnet wird.
SETUP_MARKER="$HOME/.terminalvoiceoverlay-setup-seen"
SETUP_DOC="$PROJECT_DIR/SETUP.md"
if [ ! -f "$SETUP_MARKER" ] && [ -f "$SETUP_DOC" ]; then
    echo ""
    echo "=== ERSTER BUILD MIT HOTKEY-UPDATE — SETUP.md wird geoeffnet ==="
    open "$SETUP_DOC"
    touch "$SETUP_MARKER"
fi
