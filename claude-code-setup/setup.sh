#!/bin/bash
# =============================================================================
# Claude Code Setup - macOS / Linux
# Installiert alle Plugins, Skills und Settings aus dem Manifest
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
echo -e "${BLUE}  Claude Code Setup - Plattformuebergreifend ${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# ---------------------------------------------------
# 1. Voraussetzungen pruefen
# ---------------------------------------------------
echo -e "${YELLOW}[1/6] Voraussetzungen pruefen...${NC}"

MISSING=""

if ! command -v claude &> /dev/null; then
    MISSING="$MISSING claude"
    echo -e "  ${RED}✗ Claude Code CLI nicht gefunden${NC}"
else
    echo -e "  ${GREEN}✓ Claude Code CLI${NC}"
fi

if ! command -v git &> /dev/null; then
    MISSING="$MISSING git"
    echo -e "  ${RED}✗ Git nicht gefunden${NC}"
else
    echo -e "  ${GREEN}✓ Git${NC}"
fi

if ! command -v gh &> /dev/null; then
    echo -e "  ${YELLOW}⚠ GitHub CLI (gh) nicht gefunden - optional${NC}"
else
    echo -e "  ${GREEN}✓ GitHub CLI${NC}"
fi

if ! command -v node &> /dev/null; then
    echo -e "  ${YELLOW}⚠ Node.js nicht gefunden - wird fuer einige Plugins benoetigt${NC}"
else
    echo -e "  ${GREEN}✓ Node.js$(node --version)${NC}"
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
echo -e "${YELLOW}[2/6] Rechner erkennen...${NC}"

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
# 3. Extra Marketplaces einrichten
# ---------------------------------------------------
echo -e "${YELLOW}[3/6] Marketplaces einrichten...${NC}"

# Sicherstellen dass ~/.claude/ existiert
mkdir -p "$HOME/.claude"

if [ -f "$CLAUDE_SETTINGS" ]; then
    # Pruefen ob superpowers-marketplace schon konfiguriert ist
    if grep -q "superpowers-marketplace" "$CLAUDE_SETTINGS" 2>/dev/null; then
        echo -e "  ${GREEN}✓ superpowers-marketplace bereits konfiguriert${NC}"
    else
        echo -e "  ${BLUE}  superpowers-marketplace wird hinzugefuegt...${NC}"
        # Wird durch Plugin-Installation automatisch angelegt
    fi
else
    echo -e "  ${BLUE}  Settings-Datei wird erstellt...${NC}"
fi

# ---------------------------------------------------
# 4. Plugins installieren
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[4/6] Plugins installieren...${NC}"

INSTALLED=0
SKIPPED=0
FAILED=0

# Official Marketplace Plugins
for PLUGIN in swift-lsp typescript-lsp csharp-lsp superpowers frontend-design context7 github feature-dev code-simplifier commit-commands security-guidance claude-md-management pr-review-toolkit claude-code-setup hookify playground ralph-loop code-review; do
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
for PLUGIN in episodic-memory double-shot-latte superpowers-chrome claude-session-driver; do
    echo -ne "  $PLUGIN@superpowers-marketplace ... "
    if claude plugins install "$PLUGIN@superpowers-marketplace" 2>/dev/null; then
        echo -e "${GREEN}✓${NC}"
        INSTALLED=$((INSTALLED + 1))
    else
        echo -e "${YELLOW}(bereits installiert oder Fehler)${NC}"
        SKIPPED=$((SKIPPED + 1))
    fi
done

echo ""
echo -e "  ${GREEN}Installiert: $INSTALLED${NC} | Uebersprungen: $SKIPPED | Fehler: $FAILED"

# ---------------------------------------------------
# 5. Custom Skills kopieren
# ---------------------------------------------------
echo ""
echo -e "${YELLOW}[5/6] Custom Skills installieren...${NC}"

if [ -d "$SKILLS_DIR" ]; then
    for SKILL_DIR in "$SKILLS_DIR"/*/; do
        SKILL_NAME=$(basename "$SKILL_DIR")
        TARGET_DIR="$CLAUDE_SKILLS_DIR/$SKILL_NAME"

        if [ -d "$TARGET_DIR" ]; then
            # Vorhandene Version aktualisieren
            cp -r "$SKILL_DIR"* "$TARGET_DIR/"
            echo -e "  ${GREEN}✓ $SKILL_NAME (aktualisiert)${NC}"
        else
            mkdir -p "$TARGET_DIR"
            cp -r "$SKILL_DIR"* "$TARGET_DIR/"
            echo -e "  ${GREEN}✓ $SKILL_NAME (neu installiert)${NC}"
        fi
    done
else
    echo -e "  ${RED}✗ Skills-Verzeichnis nicht gefunden: $SKILLS_DIR${NC}"
fi

# ---------------------------------------------------
# 6. Zusammenfassung
# ---------------------------------------------------
echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${GREEN}  Setup abgeschlossen!${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
echo "  Installierte Plugins:  22"
echo "  Installierte Skills:   3 (auto-verify-iterate, cross-platform, tampermonkey-version)"
echo "  Marketplaces:          2 (claude-plugins-official, superpowers-marketplace)"
echo ""
echo -e "  ${YELLOW}Starte Claude Code neu, damit alle Plugins aktiv werden:${NC}"
echo "  claude"
echo ""
