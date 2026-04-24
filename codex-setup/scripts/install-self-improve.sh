#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
source_dir="$workspace/codex-setup/skills/self-improve"
target_dir="$HOME/.codex/skills/self-improve"
rules_source_dir="$workspace/codex-setup/rules"
rules_target_dir="$HOME/.codex/rules"

if [[ ! -d "$source_dir" ]]; then
  echo "Missing repo source: $source_dir" >&2
  exit 1
fi

mkdir -p "$target_dir"
cp -R "$source_dir"/. "$target_dir"/
echo "Deployed self-improve to $target_dir"

if [[ -d "$rules_source_dir" ]]; then
  mkdir -p "$rules_target_dir"
  for rule_path in "$rules_source_dir"/*.md; do
    [[ -f "$rule_path" ]] || continue
    cp "$rule_path" "$rules_target_dir/$(basename "$rule_path")"
    echo "Deployed Codex rule $(basename "$rule_path") to $rules_target_dir"
  done
fi
