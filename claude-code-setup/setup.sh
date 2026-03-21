#!/bin/bash
# =============================================================================
# Claude Code Setup - macOS / Linux
# Installiert alle Plugins, Skills, Dev-Tools, MCP-Server und Settings
# Version 2.0.0
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MANIFEST="$SCRIPT_DIR/manifest.json"
SKILLS_DIR="$SCRIPT_DIR/skills"
CLAUDE_SKILLS_DIR="$HOME/.claude/skills"
CLAUDE_SETTINGS="$HOME/.claude/settings.json"

# Farben
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}  Claude Code Setup v2.0 - macOS / Linux     ${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# ---------------------------------------------------
# 1. Voraussetzungen pruefen
# ---------------------------------------------------
echo -e "${YELLOW}[1/8] Voraussetzungen pruefen...${NC}"

MISSING=""

if ! command -v claude &> /dev/null; then
    MISSING="$MISSING claude"
    echo -e "  ${RED}✗ Claude Code CLI nicht gefunden${NC}"
else
    echo -e "  ${GREEN}✓ Claude Code CLI $(claude --version 2>&1 | head -1)${NC}"
fi

if ! command -v git &> /dev/null; then
    MISSING="$MISSING git"
    echo -e "  ${RED}✗ Git nicht gefunden${NC}"
else
    echo -e "  ${GREEN}✓ Git $(git --version)${NC}"
fi

if ! command -v gh &> /dev/null; then
    echo -e "  ${YELLOW}⚠ GitHub CLI (gh) nicht gefunden - optional${NC}"
else
    echo -e "  ${GREEN}✓ GitHub CLI $(gh --version | head -1)${NC}"
fi

if ! command -v node &> /dev/null; then
    echo -e "  ${YELLOW}⚠ Node.js nicht gefunden - wird fuer einige Plugins benoetigt${NC}"
else
    echo -e "  ${GREEN}✓ Node.js $(node --version)${NC}"
fi

if [ -n "$MISSING" ]; then
    echo ""
    echo -e "${RED}Fehlende Voraussetzungen:$MISSING${NC}"
    echo "Bitte installiere diese zuerst und starte das Skript erneut."
    exit 1
fi

# ---------------------------------------------------
# 2. Rechner erkennen
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[2/8] Rechner erkennen...${NC}"

IS_KNOWN_MACHINE=false
GITHUB_USER=""

if command -v gh &> /dev/null; then
    GITHUB_USER=$(gh auth status 2>&1 | grep -o 'Logged in to github.com account [^ ]*' | awk '{print $NF}' || true)
fi

if [ "$GITHUB_USER" = "Pepsi1978" ]; then
    IS_KNOWN_MACHINE=true
    echo -e "  ${GREEN}✓ Erkannt: Frank's Rechner (GitHub: Pepsi1978)${NC}"
elif [ -d "$HOME/proggs" ]; then
    IS_KNOWN_MACHINE=true
    echo -e "  ${GREEN}✓ Erkannt: proggs-Verzeichnis vorhanden${NC}"
else
    echo -e "  ${BLUE}ℹ Neuer/unbekannter Rechner${NC}"
    echo ""
    read -p "  GitHub-Benutzername (Enter fuer Pepsi1978): " INPUT_USER
    GITHUB_USER="${INPUT_USER:-Pepsi1978}"
    echo ""
fi

# ---------------------------------------------------
# 3. Dev-Tools installieren
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[3/8] Dev-Tools installieren...${NC}"

# Homebrew pruefen (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    if ! command -v brew &> /dev/null; then
        echo -e "  ${YELLOW}⚠ Homebrew nicht gefunden - installiere...${NC}"
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
fi

# Bun
if ! command -v bun &> /dev/null; then
    echo -ne "  Bun installieren ... "
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install oven-sh/bun/bun &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    else
        curl -fsSL https://bun.sh/install | bash &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    fi
else
    echo -e "  ${GREEN}✓ Bun $(bun --version)${NC}"
fi

# Deno
if ! command -v deno &> /dev/null; then
    echo -ne "  Deno installieren ... "
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install deno &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    else
        curl -fsSL https://deno.land/install.sh | sh &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    fi
else
    echo -e "  ${GREEN}✓ Deno $(deno -V)${NC}"
fi

# Rust
if ! command -v rustc &> /dev/null && [ ! -f "$HOME/.cargo/bin/rustc" ]; then
    echo -ne "  Rust installieren ... "
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    # PATH fuer aktuelle Session setzen
    export PATH="$HOME/.cargo/bin:$PATH"
    # In .zshrc eintragen falls noch nicht vorhanden
    if ! grep -q "cargo/bin" "$HOME/.zshrc" 2>/dev/null; then
        echo 'export PATH="$HOME/.cargo/bin:$PATH"' >> "$HOME/.zshrc"
    fi
else
    RUSTC="${HOME}/.cargo/bin/rustc"
    [ -x "$RUSTC" ] || RUSTC="rustc"
    echo -e "  ${GREEN}✓ Rust $($RUSTC --version)${NC}"
fi

# Docker
if ! command -v docker &> /dev/null; then
    if [ -d "/Applications/Docker.app" ]; then
        echo -e "  ${GREEN}✓ Docker Desktop installiert (bitte App starten)${NC}"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo -ne "  Docker Desktop installieren ... "
        if brew install --cask docker &> /dev/null; then
            echo -e "${GREEN}✓ (bitte App einmal starten)${NC}"
        else
            echo -e "${YELLOW}⚠ Braucht sudo - bitte manuell: sudo mkdir -p /usr/local/cli-plugins && brew install --cask docker${NC}"
        fi
    fi
else
    echo -e "  ${GREEN}✓ Docker $(docker --version)${NC}"
fi

# .NET SDK
if ! command -v dotnet &> /dev/null; then
    echo -ne "  .NET SDK installieren ... "
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install dotnet-sdk &> /dev/null && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}"
    fi
else
    echo -e "  ${GREEN}✓ .NET $(dotnet --version)${NC}"
fi

# ---------------------------------------------------
# 4. Extra Marketplaces einrichten
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[4/8] Marketplaces einrichten...${NC}"

mkdir -p "$HOME/.claude"

if [ -f "$CLAUDE_SETTINGS" ]; then
    if grep -q "superpowers-marketplace" "$CLAUDE_SETTINGS" 2>/dev/null; then
        echo -e "  ${GREEN}✓ superpowers-marketplace bereits konfiguriert${NC}"
    else
        echo -e "  ${BLUE}  superpowers-marketplace wird durch Plugin-Installation hinzugefuegt...${NC}"
    fi
else
    echo -e "  ${BLUE}  Settings-Datei wird erstellt...${NC}"
fi

# ---------------------------------------------------
# 5. Plugins installieren
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[5/8] Plugins installieren...${NC}"

INSTALLED=0
SKIPPED=0

# Official Marketplace Plugins
for PLUGIN in swift-lsp typescript-lsp csharp-lsp clangd-lsp superpowers frontend-design context7 feature-dev code-simplifier commit-commands security-guidance claude-md-management pr-review-toolkit claude-code-setup hookify playground ralph-loop code-review playwright coderabbit explanatory-output-style skill-creator plugin-dev agent-sdk-dev; do
    echo -ne "  $PLUGIN@claude-plugins-official ... "
    if claude plugins install "$PLUGIN@claude-plugins-official" 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
        INSTALLED=$((INSTALLED + 1))
    else
        echo -e "${YELLOW}(bereits installiert oder Fehler)${NC}"
        SKIPPED=$((SKIPPED + 1))
    fi
done

# Superpowers Marketplace Plugins
for PLUGIN in episodic-memory double-shot-latte superpowers-chrome claude-session-driver superpowers-lab superpowers-developing-for-claude-code elements-of-style; do
    echo -ne "  $PLUGIN@superpowers-marketplace ... "
    if claude plugins install "$PLUGIN@superpowers-marketplace" 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
        INSTALLED=$((INSTALLED + 1))
    else
        echo -e "${YELLOW}(bereits installiert oder Fehler)${NC}"
        SKIPPED=$((SKIPPED + 1))
    fi
done

# Platform-specific Plugins (macOS only)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo ""
    echo -e "  ${BLUE}macOS-spezifische Plugins:${NC}"
    for PLUGIN_SPEC in "apple-platform-build-tools@apple-platform-build-tools"; do
        echo -ne "  $PLUGIN_SPEC ... "
        if claude plugins install "$PLUGIN_SPEC" 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
            INSTALLED=$((INSTALLED + 1))
        else
            echo -e "${YELLOW}(bereits installiert oder Fehler)${NC}"
            SKIPPED=$((SKIPPED + 1))
        fi
    done
fi

echo ""
echo -e "  ${GREEN}Installiert: $INSTALLED${NC} | Uebersprungen: $SKIPPED"

# ---------------------------------------------------
# 6. MCP Server einrichten
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[6/8] MCP Server einrichten...${NC}"

# Sequential Thinking
if claude mcp list 2>/dev/null | grep -q "sequential-thinking"; then
    echo -e "  ${GREEN}✓ sequential-thinking bereits konfiguriert${NC}"
else
    echo -ne "  sequential-thinking installieren ... "
    if claude mcp add --scope user sequential-thinking -- npx -y @modelcontextprotocol/server-sequential-thinking 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗${NC}"
    fi
fi

# ---------------------------------------------------
# 7. Security Tools (Parry)
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[7/8] Security Tools installieren...${NC}"

CARGO="$HOME/.cargo/bin/cargo"
PARRY="$HOME/.cargo/bin/parry"

if [ -x "$PARRY" ]; then
    echo -e "  ${GREEN}✓ Parry $($PARRY --version 2>&1)${NC}"
else
    if [ -x "$CARGO" ]; then
        echo -ne "  Parry installieren (dauert ~2 Min) ... "
        if $CARGO install --git https://github.com/vaporif/parry parry-ai 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}✗ (manuell: cargo install --git https://github.com/vaporif/parry parry-ai)${NC}"
        fi
    else
        echo -e "  ${YELLOW}⚠ Rust/Cargo nicht gefunden - Parry kann nicht installiert werden${NC}"
    fi
fi

# ---------------------------------------------------
# 8. Custom Skills kopieren
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[8/8] Custom Skills installieren...${NC}"

if [ -d "$SKILLS_DIR" ]; then
    # Install directory-based skills (contain SKILL.md)
    for SKILL_DIR in "$SKILLS_DIR"/*/; do
        [ -d "$SKILL_DIR" ] || continue
        SKILL_NAME=$(basename "$SKILL_DIR")
        TARGET_DIR="$CLAUDE_SKILLS_DIR/$SKILL_NAME"

        if [ -d "$TARGET_DIR" ]; then
            cp -r "$SKILL_DIR"* "$TARGET_DIR/"
            echo -e "  ${GREEN}✓ $SKILL_NAME (aktualisiert)${NC}"
        else
            mkdir -p "$TARGET_DIR"
            cp -r "$SKILL_DIR"* "$TARGET_DIR/"
            echo -e "  ${GREEN}✓ $SKILL_NAME (neu installiert)${NC}"
        fi
    done

    # Install flat .md skills (single-file skills like android-clean-architecture.md)
    for SKILL_FILE in "$SKILLS_DIR"/*.md; do
        [ -f "$SKILL_FILE" ] || continue
        SKILL_NAME=$(basename "$SKILL_FILE" .md)
        TARGET_DIR="$CLAUDE_SKILLS_DIR/$SKILL_NAME"
        mkdir -p "$TARGET_DIR"
        cp "$SKILL_FILE" "$TARGET_DIR/SKILL.md"
        echo -e "  ${GREEN}✓ $SKILL_NAME (flat .md)${NC}"
    done
else
    echo -e "  ${RED}✗ Skills-Verzeichnis nicht gefunden: $SKILLS_DIR${NC}"
fi

# ---------------------------------------------------
# Deploy Hooks
# ---------------------------------------------------
echo -e "\n=== Deploying Hooks ==="
HOOKS_SRC="$SCRIPT_DIR/hooks"
HOOKS_DST="$HOME/.claude/hooks"
mkdir -p "$HOOKS_DST"
hook_count=0
while IFS= read -r -d '' f; do
    rel="${f#$HOOKS_SRC/}"
    dst="$HOOKS_DST/$rel"
    mkdir -p "$(dirname "$dst")"
    cp "$f" "$dst"
    hook_count=$((hook_count + 1))
done < <(find "$HOOKS_SRC" -type f -print0)
echo "  $hook_count hook files deployed"

# ---------------------------------------------------
# Deploy Settings
# ---------------------------------------------------
echo -e "\n=== Deploying Settings ==="
SETTINGS_DST="$HOME/.claude/settings.json"
SETTINGS_SRC="$SCRIPT_DIR/settings.json"
if [ -f "$SETTINGS_DST" ]; then
    backup="$SETTINGS_DST.backup.$(date +%Y%m%d-%H%M%S)"
    cp "$SETTINGS_DST" "$backup"
    echo "  Existing settings backed up to: $backup"
fi
cp "$SETTINGS_SRC" "$SETTINGS_DST"
echo "  Settings deployed"

# ---------------------------------------------------
# Zusammenfassung
# ---------------------------------------------------
echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${GREEN}  Setup abgeschlossen!${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
echo "  Plugins:        32 (24 official + 7 superpowers-marketplace + 1 macOS)"
echo "  Skills:          6 (auto-verify-iterate, cross-platform, tampermonkey-version, undo-changes, android-clean-architecture, android-ninja)"
echo "  Hooks:          $hook_count Dateien deployed nach ~/.claude/hooks/"
echo "  MCP Server:      sequential-thinking"
echo "  Security:        Parry (Prompt-Injection-Scanner)"
echo "  Dev-Tools:       Bun, Deno, Rust, Docker, .NET SDK"
echo "  Agent Teams:     Aktiviert (experimentell)"
echo "  Marketplaces:    2 (claude-plugins-official, superpowers-marketplace)"
echo ""
echo -e "  ${YELLOW}Hinweis: Starte Claude Code neu, damit alle Plugins aktiv werden:${NC}"
echo "  claude"
echo ""
echo -e "  ${YELLOW}Optional: Parry benoetigt einen HuggingFace-Token fuer volle Funktionalitaet:${NC}"
echo "  export HF_TOKEN=\"hf_dein_token\""
echo ""
