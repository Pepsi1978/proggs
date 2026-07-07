#!/usr/bin/env bash
# Admin Setup Script — macOS/Linux equivalent of admin-setup.ps1
# Run this ONCE with sudo privileges for initial setup.

echo "=== Admin Setup for Development Environment ==="
echo ""

if [ "$(uname)" = "Darwin" ]; then
    # macOS-specific setup
    echo "[1/2] Checking Xcode Command Line Tools..."
    if ! xcode-select -p &>/dev/null; then
        xcode-select --install
        echo "  Xcode CLT installation started"
    else
        echo "  Xcode CLT already installed"
    fi

    echo "[2/2] Checking Homebrew..."
    if ! command -v brew &>/dev/null; then
        echo "  Homebrew not installed — install from https://brew.sh"
    else
        echo "  Homebrew OK ($(brew --version | head -1))"
    fi
else
    echo "[1/1] Linux: No admin setup needed beyond package manager."
fi

echo ""
echo "=== Setup Complete ==="
