#!/data/data/com.termux/files/usr/bin/bash
# Startet tmux mit Claude Code (links) + Voice Overlay (rechts)
# Das Overlay tippt erkannten Text direkt in Claude Code ein.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
export PATH=/data/data/com.termux/files/usr/bin:$PATH
export SHELL=/data/data/com.termux/files/usr/bin/bash
export TMPDIR=/data/data/com.termux/files/usr/tmp
export LANG=en_US.UTF-8

# Falls tmux-Session bereits läuft, dranhaengen
SESSION="voice"

if tmux has-session -t "$SESSION" 2>/dev/null; then
  tmux attach -t "$SESSION"
  exit 0
fi

# Neue tmux-Session starten
# Hauptpane (links): Claude Code
tmux new-session -d -s "$SESSION" -x "$(tput cols)" -y "$(tput lines)"

# Claude Code im Hauptpane starten
tmux send-keys -t "$SESSION" "source ~/.bashrc 2>/dev/null; claude --dangerously-skip-permissions --effort max" Enter

# Rechte Pane fuer Voice Overlay (20 Spalten breit)
tmux split-window -h -t "$SESSION" -l 22

# Overlay starten mit Verweis auf die linke Pane (Claude Code)
tmux send-keys -t "$SESSION" "export TMUX_TARGET_PANE='${SESSION}:0.0' && cd '$SCRIPT_DIR' && node dist/main.js" Enter

# Fokus auf linke Pane (Claude Code)
tmux select-pane -t "$SESSION:0.0"

# Tmux anzeigen
tmux attach -t "$SESSION"
