#!/data/data/com.termux/files/usr/bin/bash
# Watcher: Auto-starts Terminal Voice Overlay when Termux starts
# Add to ~/.bashrc: source ~/projects/proggs/TerminalVoiceOverlay-Termux/watcher.sh

OVERLAY_DIR="$HOME/projects/proggs/TerminalVoiceOverlay-Termux"
OVERLAY_PID_FILE="$OVERLAY_DIR/.overlay.pid"
OVERLAY_MAIN="$OVERLAY_DIR/dist/main.js"

voice_overlay_start() {
  # Only start if dist/main.js exists
  if [ ! -f "$OVERLAY_MAIN" ]; then
    return
  fi

  # Check if already running
  if [ -f "$OVERLAY_PID_FILE" ]; then
    local pid
    pid=$(cat "$OVERLAY_PID_FILE" 2>/dev/null)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      return  # Already running
    fi
    rm -f "$OVERLAY_PID_FILE"
  fi

  # Start overlay in foreground (it needs TTY)
  echo ""
  echo "  Voice Overlay verfuegbar! Starte mit: voice-overlay"
  echo ""
}

voice_overlay() {
  if [ ! -f "$OVERLAY_MAIN" ]; then
    echo "Voice Overlay nicht gebaut. Bitte erst 'cd $OVERLAY_DIR && bash build.sh' ausfuehren."
    return 1
  fi
  node "$OVERLAY_MAIN"
}

# Register as shell function
alias voice-overlay='voice_overlay'

# Show hint on shell start (only for interactive shells)
if [[ $- == *i* ]]; then
  voice_overlay_start
fi
