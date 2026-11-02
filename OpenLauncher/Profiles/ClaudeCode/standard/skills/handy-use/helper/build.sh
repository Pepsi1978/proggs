#!/usr/bin/env bash
# Baut scripts/handy-helper.dex neu (macOS, Linux). Nur noetig, wenn HandyHelper.java geaendert wurde; das Ergebnis liegt im Repo.
set -euo pipefail
hier="$(cd "$(dirname "$0")" && pwd)"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[ -d "$sdk" ] || sdk="$HOME/Android/Sdk"
plattform="$(ls -d "$sdk"/platforms/android-* | sort -V | tail -1)"
bt="$(ls -d "$sdk"/build-tools/* | sort -V | tail -1)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 --release 11 -nowarn -cp "$plattform/android.jar" -d "$tmp" "$hier/HandyHelper.java"
"$bt/d8" --min-api 26 --lib "$plattform/android.jar" --output "$tmp" "$tmp"/*.class
cp "$tmp/classes.dex" "$hier/../scripts/handy-helper.dex"
wc -c "$hier/../scripts/handy-helper.dex"
