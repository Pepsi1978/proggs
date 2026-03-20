#!/bin/bash
# Claude Code Setup — macOS
# Kopiert alle Konfigurationsdateien aus dem Repo an die richtigen Stellen nach git pull.
# Ueberschreibt alles ausser MEMORY.md (hat maschinenspezifischen Inhalt).

set -e

# ─── Farben ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
RESET='\033[0m'

ok()   { echo -e "${GREEN}  ✔  $1${RESET}"; }
warn() { echo -e "${YELLOW}  ⚠  $1${RESET}"; }
err()  { echo -e "${RED}  ✘  $1${RESET}"; }
step() { echo -e "\n${BOLD}[$1]${RESET} $2"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║   Claude Code Setup — macOS (vollständig)    ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════════╝${RESET}"
echo ""

# ─── Schritt 1: Verzeichnisse anlegen ─────────────────────────────────────────
step "1/11" "Verzeichnisse anlegen"
mkdir -p \
    ~/.claude/rules \
    ~/.claude/agents \
    ~/.claude/commands/self-improve-ref \
    ~/.claude/commands/tool-check-ref \
    ~/.claude/hooks/prompt-injection-defender/prompt-injection-defender \
    ~/.claude/skills/auto-verify-iterate \
    ~/.claude/skills/cross-platform \
    ~/.claude/skills/tampermonkey-version \
    ~/.claude/skills/undo-changes \
    ~/.claude/agent-memory/shared
ok "Alle Verzeichnisse erstellt"

# ─── Schritt 2: settings.json kopieren ────────────────────────────────────────
step "2/11" "settings.json kopieren"
cp "$SCRIPT_DIR/settings.json" ~/.claude/settings.json
ok "settings.json → ~/.claude/settings.json"

# ─── Schritt 3: macOS-Hooks in settings.json einmergen ────────────────────────
step "3/11" "macOS-Hooks in settings.json einmergen (hooks-macos.json)"

# Bevorzuge python3 (auf macOS immer vorhanden), Fallback: jq
if command -v python3 &>/dev/null; then
    python3 - "$SCRIPT_DIR/hooks-macos.json" ~/.claude/settings.json <<'PYEOF'
import json, sys

hooks_src  = sys.argv[1]
settings_f = sys.argv[2]

with open(hooks_src,  "r", encoding="utf-8") as f:
    macos_hooks = json.load(f).get("hooks", {})

with open(settings_f, "r", encoding="utf-8") as f:
    settings = json.load(f)

settings["hooks"] = macos_hooks

with open(settings_f, "w", encoding="utf-8") as f:
    json.dump(settings, f, indent="\t", ensure_ascii=False)
    f.write("\n")
PYEOF
    ok "Hooks aus hooks-macos.json via python3 eingemergt"
elif command -v jq &>/dev/null; then
    HOOKS=$(jq '.hooks' "$SCRIPT_DIR/hooks-macos.json")
    TMP=$(mktemp)
    jq --argjson h "$HOOKS" '.hooks = $h' ~/.claude/settings.json > "$TMP"
    mv "$TMP" ~/.claude/settings.json
    ok "Hooks aus hooks-macos.json via jq eingemergt"
else
    warn "Weder python3 noch jq gefunden."
    warn "Bitte hooks-macos.json manuell in ~/.claude/settings.json unter 'hooks' eintragen."
    warn "Alternativ: brew install jq"
fi

# ─── Schritt 4: Shell-Hooks (.sh) kopieren und ausführbar machen ──────────────
step "4/11" "Shell-Hooks (.sh) nach ~/.claude/hooks/ kopieren"

SH_HOOKS=(
    admin-setup.sh
    agent-resource-lock.sh
    auto-format.sh
    auto-sync.sh
    config-guard.sh
    disk-guard.sh
    hook-log.sh
    intent-anker.sh
    memory-watchdog.sh
    notify.sh
    pending-admin-updates.sh
    reindex-codebase.sh
    safety-gate.sh
    session-cleanup.sh
    stopfailure-logger.sh
    subagent-context.sh
    whiteboard-insert.sh
    writeback-enforcer.sh
)

for hook in "${SH_HOOKS[@]}"; do
    SRC="$SCRIPT_DIR/hooks/$hook"
    if [ -f "$SRC" ]; then
        cp "$SRC" ~/.claude/hooks/
        chmod +x ~/.claude/hooks/"$hook"
        ok "$hook"
    else
        warn "$hook nicht gefunden — übersprungen"
    fi
done

# ─── Schritt 5: TypeScript-Hooks kopieren und ausführbar machen ───────────────
step "5/11" "TypeScript-Hooks (.ts) nach ~/.claude/hooks/ kopieren"

TS_HOOKS=(
    session-autopsy.ts
    session-scorer.ts
)

for hook in "${TS_HOOKS[@]}"; do
    SRC="$SCRIPT_DIR/hooks/$hook"
    if [ -f "$SRC" ]; then
        cp "$SRC" ~/.claude/hooks/
        chmod +x ~/.claude/hooks/"$hook"
        ok "$hook"
    else
        warn "$hook nicht gefunden — übersprungen"
    fi
done

# ─── Schritt 6: prompt-injection-defender kopieren ────────────────────────────
step "6/11" "prompt-injection-defender nach ~/.claude/hooks/ kopieren"

PID_SRC="$SCRIPT_DIR/hooks/prompt-injection-defender"
PID_DST=~/.claude/hooks/prompt-injection-defender

# Dateien im Wurzelverzeichnis (patterns.yaml, post-tool-defender.py)
for f in "$PID_SRC"/patterns.yaml "$PID_SRC"/post-tool-defender.py; do
    if [ -f "$f" ]; then
        cp "$f" "$PID_DST/"
        ok "$(basename "$f")"
    else
        warn "$(basename "$f") nicht gefunden — übersprungen"
    fi
done

# Inneres Unterverzeichnis prompt-injection-defender/
INNER_SRC="$PID_SRC/prompt-injection-defender"
INNER_DST="$PID_DST/prompt-injection-defender"
if [ -d "$INNER_SRC" ]; then
    cp -r "$INNER_SRC/." "$INNER_DST/"
    ok "prompt-injection-defender/ (inner dir)"
else
    warn "Inneres prompt-injection-defender/ nicht gefunden — übersprungen"
fi

# ─── Schritt 7: Agents kopieren ───────────────────────────────────────────────
step "7/11" "Agents nach ~/.claude/agents/ kopieren"
cp "$SCRIPT_DIR/agents/"*.md ~/.claude/agents/
ok "$(ls "$SCRIPT_DIR/agents/"*.md | wc -l | tr -d ' ') Agent-Dateien kopiert"

# ─── Schritt 8: Commands kopieren (inkl. Unterverzeichnisse) ──────────────────
step "8/11" "Commands nach ~/.claude/commands/ kopieren"
cp "$SCRIPT_DIR/commands/"*.md ~/.claude/commands/

# Unterverzeichnisse self-improve-ref/ und tool-check-ref/
for subdir in self-improve-ref tool-check-ref; do
    SRC_DIR="$SCRIPT_DIR/commands/$subdir"
    DST_DIR=~/.claude/commands/$subdir
    if [ -d "$SRC_DIR" ]; then
        cp -r "$SRC_DIR/." "$DST_DIR/"
        ok "$subdir/"
    else
        warn "$subdir/ nicht gefunden — übersprungen"
    fi
done
ok "Commands kopiert"

# ─── Schritt 9: Skills kopieren (Verzeichnisse + Standalone-Dateien) ──────────
step "9/11" "Skills nach ~/.claude/skills/ kopieren"

# Verzeichnis-basierte Skills
for skill_dir in auto-verify-iterate cross-platform tampermonkey-version undo-changes; do
    SRC_DIR="$SCRIPT_DIR/skills/$skill_dir"
    DST_DIR=~/.claude/skills/$skill_dir
    if [ -d "$SRC_DIR" ]; then
        cp -r "$SRC_DIR/." "$DST_DIR/"
        ok "$skill_dir/"
    else
        warn "$skill_dir/ nicht gefunden — übersprungen"
    fi
done

# Standalone .md Skills
for skill_md in "$SCRIPT_DIR/skills/"*.md; do
    [ -f "$skill_md" ] || continue
    cp "$skill_md" ~/.claude/skills/
    ok "$(basename "$skill_md")"
done

# ─── Schritt 10: Rules kopieren ───────────────────────────────────────────────
step "10/11" "Rules nach ~/.claude/rules/ kopieren"
cp "$SCRIPT_DIR/rules/"*.md ~/.claude/rules/
ok "$(ls "$SCRIPT_DIR/rules/"*.md | wc -l | tr -d ' ') Rule-Dateien kopiert"

# ─── Schritt 11: Weitere Dateien und Git-Konfiguration ────────────────────────
step "11/11" "CLAUDE.md, .gitignore_global, Git-Konfiguration"

cp "$SCRIPT_DIR/CLAUDE.md" ~/CLAUDE.md
ok "CLAUDE.md → ~/CLAUDE.md"

cp "$SCRIPT_DIR/.gitignore_global" ~/.gitignore_global
ok ".gitignore_global → ~/.gitignore_global"

# Git-Konfiguration setzen
git config --global init.defaultBranch     main
git config --global pull.rebase            true
git config --global push.autoSetupRemote   true
git config --global core.excludesFile      ~/.gitignore_global
ok "Git-Konfiguration gesetzt (defaultBranch, pull.rebase, push.autoSetupRemote, core.excludesFile)"

# ─── agent-memory/shared/MEMORY.md — nur anlegen, NIEMALS überschreiben ───────
MEMORY_FILE=~/.claude/agent-memory/shared/MEMORY.md
if [ ! -f "$MEMORY_FILE" ]; then
    # Repo-Vorlage vorhanden? Dann einmalig kopieren.
    MEMORY_SRC="$SCRIPT_DIR/agent-memory/shared/MEMORY.md"
    if [ -f "$MEMORY_SRC" ]; then
        cp "$MEMORY_SRC" "$MEMORY_FILE"
        ok "MEMORY.md erstmalig angelegt (aus Repo-Vorlage)"
    else
        # Leere Datei mit Stub-Inhalt erstellen
        cat > "$MEMORY_FILE" <<'EOF'
# Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Claude Code Systems. JEDE Komponente die hier arbeitet
(Agents, Skills, Hooks, Plugins) MUSS hier lesen und schreiben.
EOF
        ok "MEMORY.md erstmalig mit Stub-Inhalt angelegt"
    fi
else
    warn "MEMORY.md existiert bereits — wird NICHT überschrieben (maschinenspezifischer Inhalt)"
fi

# ─── Optional: apple-platform-build-tools Plugin installieren ─────────────────
echo ""
echo -e "${BOLD}[Optional] apple-platform-build-tools Plugin${RESET}"
if command -v claude &>/dev/null; then
    echo -e "   Installiere apple-platform-build-tools aus dem Anthropic Marketplace..."
    if claude mcp add apple-platform-build-tools 2>/dev/null || \
       claude plugin install kylehughes/apple-platform-build-tools-claude-code-plugin 2>/dev/null; then
        ok "apple-platform-build-tools installiert"
    else
        warn "Plugin konnte nicht automatisch installiert werden."
        warn "Manuell installieren: claude plugin install kylehughes/apple-platform-build-tools-claude-code-plugin"
    fi
else
    warn "claude CLI nicht gefunden — Plugin muss manuell installiert werden."
    warn "Befehl: claude plugin install kylehughes/apple-platform-build-tools-claude-code-plugin"
fi

# ─── Zusammenfassung ──────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║             Setup abgeschlossen!             ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════════╝${RESET}"
echo ""
echo -e "${GREEN}Was wurde kopiert:${RESET}"
echo "  • settings.json          → ~/.claude/settings.json (mit macOS-Hooks)"
echo "  • ${#SH_HOOKS[@]} Shell-Hooks (.sh)    → ~/.claude/hooks/ (chmod +x)"
echo "  • ${#TS_HOOKS[@]} TypeScript-Hooks (.ts)→ ~/.claude/hooks/ (chmod +x)"
echo "  • prompt-injection-defender → ~/.claude/hooks/prompt-injection-defender/"
echo "  • Agents                 → ~/.claude/agents/"
echo "  • Commands (+ Subdirs)   → ~/.claude/commands/"
echo "  • Skills (dirs + .md)    → ~/.claude/skills/"
echo "  • Rules                  → ~/.claude/rules/"
echo "  • CLAUDE.md              → ~/CLAUDE.md"
echo "  • .gitignore_global      → ~/.gitignore_global"
echo "  • Git-Konfiguration      gesetzt"
echo "  • agent-memory/shared/   angelegt (MEMORY.md nur wenn neu)"
echo ""
echo -e "${YELLOW}Nächste Schritte:${RESET}"
echo "  1. Dev-Tools installieren:    brew install swift-format swiftlint golangci-lint"
echo "  2. Shell-Tools installieren:  brew install fzf eza bat fd tmux htop wget jq"
echo "  3. Bun installieren (für TS-Hooks): curl -fsSL https://bun.sh/install | bash"
echo "  4. Claude Code starten und /self-improve ausführen (vollständige Umgebungsprüfung)"
echo "  5. Parry-Daemon starten:      parry serve"
echo ""
