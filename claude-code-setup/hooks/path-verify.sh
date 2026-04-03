#!/bin/bash
# path-verify.sh — Robuste PATH-Verifikation nach Shell-Updates
# Event: Manuell nach Shell-Updates ausfuehren ODER als Post-Update-Check
# Platform: macOS/Linux
#
# ZWECK: Prueft ob alle kritischen Tools nach einem Shell-Update noch im PATH sind.
# Repariert fehlende Eintraege automatisch wo moeglich.
#
# ANWENDUNG:
#   bash ~/.claude/hooks/path-verify.sh          # Pruefung
#   bash ~/.claude/hooks/path-verify.sh --fix    # Pruefung + Reparatur

set +e
FIX_MODE=false
[ "$1" = "--fix" ] && FIX_MODE=true

missing=()
warnings=()
fixed=()

# --- Hilfsfunktion: Pruefe ob ein Befehl existiert ---
check_cmd() {
    local cmd="$1"
    local desc="$2"
    local install_hint="$3"
    if ! command -v "$cmd" >/dev/null 2>&1; then
        missing+=("$desc ($cmd) — $install_hint")
        return 1
    fi
    return 0
}

# --- Hilfsfunktion: Pruefe ob ein Verzeichnis im PATH ist ---
check_path_dir() {
    local dir="$1"
    local desc="$2"
    if [ -d "$dir" ] && ! echo "$PATH" | tr ':' '\n' | grep -qF "$dir"; then
        warnings+=("$desc: $dir existiert aber ist NICHT im PATH")
        if $FIX_MODE; then
            export PATH="$dir:$PATH"
            fixed+=("$dir zum PATH hinzugefuegt (Session)")
            # Persistent fuer zsh
            local shellrc="$HOME/.zshrc"
            [ -f "$HOME/.bashrc" ] && ! [ -f "$HOME/.zshrc" ] && shellrc="$HOME/.bashrc"
            if [ -f "$shellrc" ] && ! grep -qF "$dir" "$shellrc" 2>/dev/null; then
                echo "export PATH=\"$dir:\$PATH\"" >> "$shellrc"
                fixed+=("$dir in $shellrc eingetragen (persistent)")
            fi
        fi
        return 1
    fi
    return 0
}

echo "=== PATH-Verifikation ($(uname -s)) ==="
echo ""

# ===========================================================
# PLATTFORM-UEBERGREIFENDE CHECKS
# ===========================================================

# --- Git ---
check_cmd "git" "Git" "brew install git / winget install Git.Git"

# --- Node.js / npm ---
check_cmd "node" "Node.js" "brew install node / winget install OpenJS.NodeJS"
check_cmd "npm" "npm" "kommt mit Node.js"

# --- Bun ---
check_cmd "bun" "Bun" "brew install oven-sh/bun/bun"
check_path_dir "$HOME/.bun/bin" "Bun bin"

# --- Python ---
check_cmd "python3" "Python 3" "brew install python3"

# --- Rust / Cargo ---
check_cmd "rustc" "Rust Compiler" "rustup.rs"
check_cmd "cargo" "Cargo" "rustup.rs"
check_path_dir "$HOME/.cargo/bin" "Cargo bin"

# --- Go ---
check_cmd "go" "Go" "brew install go"
check_path_dir "$HOME/go/bin" "Go bin (GOPATH)"

# --- Claude Code CLI ---
check_cmd "claude" "Claude Code CLI" "npm install -g @anthropic-ai/claude-code"

# --- claudewatch ---
check_cmd "claudewatch" "claudewatch" "gh release download --repo blackwell-systems/claudewatch"

# ===========================================================
# macOS-SPEZIFISCHE CHECKS
# ===========================================================
if [ "$(uname -s)" = "Darwin" ]; then
    # Homebrew
    check_cmd "brew" "Homebrew" "https://brew.sh"

    # Swift
    check_cmd "swift" "Swift" "Xcode oder swift.org"
    check_cmd "swiftc" "Swift Compiler" "Xcode oder swift.org"

    # Xcode CLI Tools
    check_cmd "xcodebuild" "Xcode Build" "xcode-select --install"

    # Kotlin/Gradle (optional, nur warnen)
    check_cmd "kotlin" "Kotlin" "brew install kotlin" || true
    check_cmd "gradle" "Gradle" "brew install gradle" || true

    # Ollama
    check_cmd "ollama" "Ollama" "brew install ollama"

    # Deno
    check_cmd "deno" "Deno" "brew install deno"

    # Homebrew Pfade
    check_path_dir "/opt/homebrew/bin" "Homebrew (Apple Silicon)"
    check_path_dir "/usr/local/bin" "Homebrew (Intel)"
fi

# ===========================================================
# ORPHANED PYTHON INSTALLATIONS (Poka-Yoke)
# Root Cause: When switching Python managers (official installer -> uv/pyenv/brew),
# the old installation may be partially removed, leaving pip without python.
# ===========================================================
orphaned_python=()

# Check PATH entries with pip but no python
IFS=':' read -ra path_dirs <<< "$PATH"
for dir in "${path_dirs[@]}"; do
    if [ -f "$dir/pip" ] || [ -f "$dir/pip3" ]; then
        parent_dir=$(dirname "$dir")
        if [ ! -f "$parent_dir/python" ] && [ ! -f "$parent_dir/python3" ] && [ ! -f "$dir/python" ] && [ ! -f "$dir/python3" ]; then
            orphaned_python+=("ORPHANED pip: $dir has pip but no python in $parent_dir")
        fi
    fi
done

# Check PATH entries pointing to non-existent directories (tool installations only)
ghost_dirs=()
for dir in "${path_dirs[@]}"; do
    if [ -n "$dir" ] && [ ! -d "$dir" ]; then
        case "$dir" in
            *Python*|*python*|*node*|*npm*|*cargo*|*rustup*|*Go*|*go*|*gradle*|*kotlin*|*Android*|*bun*)
                ghost_dirs+=("GHOST-PATH: $dir does not exist")
                ;;
        esac
    fi
done

if [ ${#orphaned_python[@]} -gt 0 ] || [ ${#ghost_dirs[@]} -gt 0 ]; then
    warnings+=("${orphaned_python[@]}" "${ghost_dirs[@]}")
fi

# ===========================================================
# ENVIRONMENT-VARIABLEN
# ===========================================================
env_missing=()

# JAVA_HOME (fuer Android/Kotlin)
if [ -z "$JAVA_HOME" ] && command -v java >/dev/null 2>&1; then
    if [ "$(uname -s)" = "Darwin" ]; then
        detected_java=$(/usr/libexec/java_home 2>/dev/null)
        if [ -n "$detected_java" ]; then
            env_missing+=("JAVA_HOME nicht gesetzt — sollte sein: $detected_java")
            if $FIX_MODE; then
                export JAVA_HOME="$detected_java"
                fixed+=("JAVA_HOME=$detected_java gesetzt (Session)")
            fi
        fi
    fi
fi

# ANDROID_HOME / ANDROID_SDK_ROOT
if [ -z "$ANDROID_HOME" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    env_missing+=("ANDROID_HOME nicht gesetzt — sollte sein: $HOME/Library/Android/sdk")
fi

# ===========================================================
# REPORT
# ===========================================================
echo ""
total_issues=$(( ${#missing[@]} + ${#warnings[@]} + ${#env_missing[@]} ))

if [ $total_issues -eq 0 ]; then
    echo "PATH-Verifikation: Alle Checks bestanden."
else
    echo "PATH-Verifikation: $total_issues Problem(e) gefunden:"
    echo ""

    if [ ${#missing[@]} -gt 0 ]; then
        echo "FEHLENDE TOOLS:"
        for m in "${missing[@]}"; do
            echo "  - $m"
        done
        echo ""
    fi

    if [ ${#warnings[@]} -gt 0 ]; then
        echo "PATH-WARNUNGEN:"
        for w in "${warnings[@]}"; do
            echo "  - $w"
        done
        echo ""
    fi

    if [ ${#env_missing[@]} -gt 0 ]; then
        echo "FEHLENDE ENV-VARIABLEN:"
        for e in "${env_missing[@]}"; do
            echo "  - $e"
        done
        echo ""
    fi

    if [ ${#fixed[@]} -gt 0 ]; then
        echo "AUTOMATISCH REPARIERT:"
        for f in "${fixed[@]}"; do
            echo "  - $f"
        done
        echo ""
    fi
fi

exit 0
