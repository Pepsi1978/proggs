#!/bin/zsh
# Baut den Fernsteuerungs-Dienst nach ~/Library/Application Support/JarvisFernsteuerung/jarvis-fernsteuerung.
set -e
cd "${0:A:h}"
ziel="$HOME/Library/Application Support/JarvisFernsteuerung"
mkdir -p "$ziel"
swiftc -O -swift-version 5 -import-objc-header VirtuellerBildschirm.h main.swift Befehle.swift -o "$ziel/jarvis-fernsteuerung.neu"
# Stabile Signatur, damit Rechte und Firewall-Freigabe nach jedem Bau gelten.
codesign -f -s "Frank Local Dev" -i de.frank.jarvis.fernsteuerung "$ziel/jarvis-fernsteuerung.neu" 2>/dev/null || codesign -f -s - "$ziel/jarvis-fernsteuerung.neu"
mv -f "$ziel/jarvis-fernsteuerung.neu" "$ziel/jarvis-fernsteuerung"
echo "gebaut: $ziel/jarvis-fernsteuerung"
